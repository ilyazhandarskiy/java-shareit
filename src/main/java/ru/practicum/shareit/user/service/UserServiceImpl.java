package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.CreateUserRequest;
import ru.practicum.shareit.user.dto.PatchUserRequest;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserDto create(CreateUserRequest createUserRequest) {
        checkEmailUnique(createUserRequest.getEmail());
        User createdUser = UserMapper.toUser(createUserRequest);
        return UserMapper.toUserDto(userRepository.save(createdUser));
    }

    @Override
    public UserDto patch(long userId, PatchUserRequest patchUserRequest) {
        User user = getUserOrThrow(userId);

        if (!user.getEmail().equalsIgnoreCase(patchUserRequest.getEmail())) {
            checkEmailUnique(patchUserRequest.getEmail());
        }

        UserMapper.patchUser(user, patchUserRequest);

        return UserMapper.toUserDto(userRepository.save(user));
    }

    @Override
    public UserDto getById(long userId) {
        return UserMapper.toUserDto(getUserOrThrow(userId));
    }

    @Override
    public List<UserDto> getAll() {
        return userRepository.findAll().stream().map(UserMapper::toUserDto).toList();
    }

    @Override
    public void deleteById(long userId) {
        getUserOrThrow(userId);
        userRepository.deleteById(userId);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + userId));
    }

    private void checkEmailUnique(String email) {
        userRepository.findByEmail(email).ifPresent(u -> {
            throw new ConflictException("Email already in use: " + email);
        });
    }
}
