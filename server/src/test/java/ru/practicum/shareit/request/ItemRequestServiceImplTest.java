package ru.practicum.shareit.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.CreateItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDtoFull;
import ru.practicum.shareit.request.dto.ItemRequestDtoShort;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.request.service.ItemRequestServiceImpl;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemRequestServiceImplTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemRequestRepository requestRepository;
    @InjectMocks
    private ItemRequestServiceImpl requestService;

    private final LocalDateTime created = LocalDateTime.of(2030, 1, 15, 12, 0);
    private final User requestor = User.builder()
            .id(1L).name("Requestor").email("requestor@example.com").build();
    private final User otherUser = User.builder()
            .id(2L).name("Other").email("other@example.com").build();

    @Test
    void createReturnsRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requestor));
        when(requestRepository.save(any())).thenAnswer(invocation -> {
            ItemRequest saved = invocation.getArgument(0);
            saved.setId(30L);
            return saved;
        });
        LocalDateTime before = LocalDateTime.now();

        ItemRequestDtoShort actual = requestService.create(new CreateItemRequestDto("Need a saw"), 1L);

        assertThat(actual.getId()).isEqualTo(30L);
        assertThat(actual.getDescription()).isEqualTo("Need a saw");
        assertThat(actual.getRequestorId()).isEqualTo(1L);
        assertThat(actual.getCreated()).isBetween(before, LocalDateTime.now());
        ArgumentCaptor<ItemRequest> captor = ArgumentCaptor.forClass(ItemRequest.class);
        verify(requestRepository).save(captor.capture());
        assertThat(captor.getValue().getRequestor()).isSameAs(requestor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"create", "own", "others", "get"})
    void everyOperationRequiresExistingUser(String operation) {
        assertThatThrownBy(() -> {
            switch (operation) {
                case "create" -> requestService.create(new CreateItemRequestDto("Need a saw"), 1L);
                case "own" -> requestService.getUserItems(1L);
                case "others" -> requestService.getOtherUsersItems(1L);
                case "get" -> requestService.findById(30L, 1L);
                default -> throw new AssertionError(operation);
            }
        }).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(requestRepository);
    }

    @Test
    void getUserItemsWithLinkedItems() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requestor));
        ItemRequest newer = createRequest(31, requestor);
        newer.setCreated(created.plusDays(1));
        Item linked = Item.builder().id(10L).name("Drill").owner(otherUser).build();
        newer.setItems(List.of(linked));
        when(requestRepository.findByRequestorIdOrderByCreatedDesc(1L))
                .thenReturn(List.of(newer, createRequest(30, requestor)));

        List<ItemRequestDtoFull> actual = requestService.getUserItems(1L);

        assertThat(actual).extracting(ItemRequestDtoFull::getId).containsExactly(31L, 30L);
        assertThat(actual.getFirst().getItems()).singleElement().satisfies(item -> {
            assertThat(item.getId()).isEqualTo(10L);
            assertThat(item.getName()).isEqualTo("Drill");
            assertThat(item.getOwnerId()).isEqualTo(2L);
        });
        assertThat(actual.get(1).getItems()).isEmpty();
    }

    @Test
    void getOtherUsersItems() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requestor));
        when(requestRepository.findByRequestorIdNotOrderByCreatedDesc(1L))
                .thenReturn(List.of(createRequest(30, otherUser)));

        assertThat(requestService.getOtherUsersItems(1L)).singleElement().satisfies(actual -> {
            assertThat(actual.getId()).isEqualTo(30L);
            assertThat(actual.getDescription()).isEqualTo("Need a drill");
            assertThat(actual.getRequestorId()).isEqualTo(2L);
            assertThat(actual.getCreated()).isEqualTo(created);
        });
    }

    @Test
    void emptyRequestListsAreEmpty() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requestor));

        assertThat(requestService.getUserItems(1L)).isEmpty();
        assertThat(requestService.getOtherUsersItems(1L)).isEmpty();
    }

    @Test
    void findByIdWhenRequestExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requestor));
        when(requestRepository.findById(30L)).thenReturn(Optional.of(createRequest(30, otherUser)));

        ItemRequestDtoFull actual = requestService.findById(30L, 1L);

        assertThat(actual.getId()).isEqualTo(30L);
        assertThat(actual.getRequestorId()).isEqualTo(2L);
        assertThat(actual.getDescription()).isEqualTo("Need a drill");
        assertThat(actual.getCreated()).isEqualTo(created);
        assertThat(actual.getItems()).isEmpty();
    }

    @Test
    void findByIdRejectsMissingRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requestor));

        assertThatThrownBy(() -> requestService.findById(30L, 1L)).isInstanceOf(NotFoundException.class);
    }

    private ItemRequest createRequest(long id, User author) {
        return ItemRequest.builder()
                .id(id).description("Need a drill").requestor(author).created(created).build();
    }
}
