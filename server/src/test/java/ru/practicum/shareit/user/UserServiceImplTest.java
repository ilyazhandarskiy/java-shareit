package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.CreateUserRequestDto;
import ru.practicum.shareit.user.dto.PatchUserRequestDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.user.service.UserServiceImpl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private final CreateUserRequestDto createDto = CreateUserRequestDto.builder()
            .name("name")
            .email("my@email.com")
            .build();

    private User existingUser() {
        return User.builder().id(1L).name("name").email("my@email.com").build();
    }

    @Test
    void createReturnsUserDto() {
        when(userRepository.findByEmail("my@email.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(existingUser());

        UserDto actual = userService.create(createDto);

        assertEquals(UserDto.builder().id(1L).name("name").email("my@email.com").build(), actual);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("name", captor.getValue().getName());
        assertEquals("my@email.com", captor.getValue().getEmail());
    }

    @Test
    void createThrowsConflictWhenEmailExists() {
        when(userRepository.findByEmail("my@email.com")).thenReturn(Optional.of(existingUser()));

        assertThrows(ConflictException.class, () -> userService.create(createDto));

        verify(userRepository, never()).save(any());
    }

    @Test
    void patchUpdatesNameAndEmail() {
        PatchUserRequestDto patchDto = PatchUserRequestDto.builder()
                .name("Updated User")
                .email("updated@example.com")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser()));
        when(userRepository.findByEmail("updated@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto actual = userService.patch(1L, patchDto);

        assertEquals(1L, actual.getId());
        assertEquals("Updated User", actual.getName());
        assertEquals("updated@example.com", actual.getEmail());
    }

    @Test
    void patchThrowsConflictWhenEmailTaken() {
        PatchUserRequestDto patchDto = PatchUserRequestDto.builder()
                .email("other@example.com")
                .build();
        User other = User.builder().id(2L).name("other").email("other@example.com").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser()));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(other));

        assertThrows(ConflictException.class, () -> userService.patch(1L, patchDto));

        verify(userRepository, never()).save(any());
    }

    @Test
    void patchThrowsNotFoundWhenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userService.patch(99L, PatchUserRequestDto.builder().name("x").build()));
    }

    @Test
    void getByIdWhenUserFound() {
        User user = existingUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertEquals(UserMapper.toUserDto(user), userService.getById(1L));
    }

    @Test
    void getByIdWhenUserNotFound() {
        when(userRepository.findById(0L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> userService.getById(0L));

        assertEquals("Пользователь не найден: 0", ex.getMessage());
    }

    @Test
    void getAllReturnsMappedUsers() {
        List<User> users = List.of(existingUser());
        when(userRepository.findAll()).thenReturn(users);

        List<UserDto> actual = userService.getAll();

        assertEquals(List.of(UserMapper.toUserDto(users.getFirst())), actual);
    }

    @Test
    void deleteByIdDeletesExistingUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser()));

        userService.deleteById(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteByIdThrowsWhenUserMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.deleteById(1L));

        verify(userRepository, never()).deleteById(anyLong());
    }

    @ParameterizedTest
    @MethodSource("patches")
    void partialUpdatesPreserveOmittedFieldsAndAllowOwnEmail(PatchUserRequestDto patch, String name, String email) {
        User existing = User.builder().id(1L).name("Original").email("user@example.com").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        if (patch.getEmail() != null && !patch.getEmail().equalsIgnoreCase(existing.getEmail())) {
            when(userRepository.findByEmail(patch.getEmail())).thenReturn(Optional.empty());
        }
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserDto actual = userService.patch(1, patch);

        assertEquals(1L, actual.getId());
        assertEquals(name, actual.getName());
        assertEquals(email, actual.getEmail());
        verify(userRepository).save(existing);
        if (email.equalsIgnoreCase("user@example.com")) {
            verify(userRepository, never()).findByEmail(anyString());
        } else {
            verify(userRepository).findByEmail(email);
        }
    }

    @Test
    void emptyUserListIsEmpty() {
        assertTrue(userService.getAll().isEmpty());
    }

    private static Stream<Arguments> patches() {
        return Stream.of(
                Arguments.of(new PatchUserRequestDto(), "Original", "user@example.com"),
                Arguments.of(PatchUserRequestDto.builder().name("Updated").build(), "Updated", "user@example.com"),
                Arguments.of(PatchUserRequestDto.builder().email("new@example.com").build(), "Original", "new@example.com"),
                Arguments.of(PatchUserRequestDto.builder().email("user@example.com").build(), "Original", "user@example.com"),
                Arguments.of(PatchUserRequestDto.builder().email("USER@example.com").build(), "Original", "USER@example.com"));
    }
}
