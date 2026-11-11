package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.CreateCommentDto;
import ru.practicum.shareit.item.dto.CreateItemRequestDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.PatchItemRequestDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.item.service.ItemServiceImpl;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ItemRequestRepository requestRepository;
    @InjectMocks
    private ItemServiceImpl itemService;

    private final LocalDateTime created = LocalDateTime.of(2030, 1, 15, 12, 0);
    private final User owner = User.builder()
            .id(1L).name("Owner").email("owner@example.com").build();
    private final User booker = User.builder()
            .id(2L).name("Booker").email("booker@example.com").build();
    private final User otherBooker = User.builder()
            .id(3L).name("Other booker").email("other@example.com").build();
    private final Item item = Item.builder()
            .id(10L).name("Drill").description("Cordless tool").available(true).owner(owner).build();
    private final CreateItemRequestDto createDto = CreateItemRequestDto.builder()
            .name("Drill").description("Cordless").available(true).build();

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createWithAndWithoutRequest(boolean withRequest) {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        ItemRequest request = ItemRequest.builder()
                .id(30L).description("Need a drill").requestor(booker).created(created).build();
        if (withRequest) {
            createDto.setRequestId(30L);
            when(requestRepository.findById(30L)).thenReturn(Optional.of(request));
        }
        when(itemRepository.save(any())).thenAnswer(invocation -> {
            Item saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        ItemDto actual = itemService.create(1, createDto);

        assertThat(actual.getId()).isEqualTo(10L);
        assertThat(actual.getName()).isEqualTo("Drill");
        assertThat(actual.getDescription()).isEqualTo("Cordless");
        assertThat(actual.getAvailable()).isTrue();
        assertThat(actual.getRequestId()).isEqualTo(withRequest ? 30L : null);
        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemRepository).save(captor.capture());
        assertThat(captor.getValue().getOwner()).isSameAs(owner);
        assertThat(captor.getValue().getRequest()).isSameAs(withRequest ? request : null);
        if (!withRequest) {
            verifyNoInteractions(requestRepository);
        }
    }

    @Test
    void createRejectsMissingOwner() {
        assertThatThrownBy(() -> itemService.create(1, createDto)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(itemRepository, requestRepository);
    }

    @Test
    void createRejectsMissingRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        createDto.setRequestId(30L);

        assertThatThrownBy(() -> itemService.create(1, createDto)).isInstanceOf(NotFoundException.class);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void patchUpdatesItem() {
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ItemDto actual = itemService.patch(1, 10, PatchItemRequestDto.builder()
                .name("Updated").description("New description").available(false).build());

        assertThat(actual.getName()).isEqualTo("Updated");
        assertThat(actual.getDescription()).isEqualTo("New description");
        assertThat(actual.getAvailable()).isFalse();
        verify(itemRepository).save(item);
    }

    @Test
    void patchPreservesOmittedFields() {
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ItemDto actual = itemService.patch(1, 10, PatchItemRequestDto.builder().name("Updated").build());

        assertThat(actual.getName()).isEqualTo("Updated");
        assertThat(actual.getDescription()).isEqualTo("Cordless tool");
        assertThat(actual.getAvailable()).isTrue();
    }

    @Test
    void emptyPatchPreservesAllFields() {
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ItemDto actual = itemService.patch(1, 10, new PatchItemRequestDto());

        assertThat(actual.getName()).isEqualTo("Drill");
        assertThat(actual.getDescription()).isEqualTo("Cordless tool");
        assertThat(actual.getAvailable()).isTrue();
    }

    @Test
    void patchRejectsNonOwner() {
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> itemService.patch(2, 10, new PatchItemRequestDto())).isInstanceOf(NotFoundException.class);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void patchRejectsMissingItem() {
        assertThatThrownBy(() -> itemService.patch(1, 10, new PatchItemRequestDto())).isInstanceOf(NotFoundException.class);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void getByIdIncludesMappedComments() {
        stubUserAndItem();
        when(commentRepository.findByItemIdOrderByCreatedAsc(10L)).thenReturn(List.of(createComment(40, item, owner)));

        ItemDto actual = itemService.getById(1, 10);

        assertThat(actual.getId()).isEqualTo(10L);
        assertThat(actual.getComments()).singleElement().satisfies(comment -> {
            assertThat(comment.getId()).isEqualTo(40L);
            assertThat(comment.getText()).isEqualTo("Works well");
            assertThat(comment.getAuthorName()).isEqualTo("Owner");
            assertThat(comment.getCreated()).isEqualTo(created);
        });
    }

    @Test
    void getByIdReturnsEmptyCommentsWhenThereAreNone() {
        stubUserAndItem();

        assertThat(itemService.getById(1, 10).getComments()).isEmpty();
    }

    @Test
    void getByIdRejectsMissingItem() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> itemService.getById(1, 10)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(commentRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"get", "list", "search", "comment"})
    void readAndCommentOperationsRejectMissingUser(String operation) {
        assertThatThrownBy(() -> {
            switch (operation) {
                case "get" -> itemService.getById(1, 10);
                case "list" -> itemService.getAllByOwner(1);
                case "search" -> itemService.searchAvailableByText(1, "drill");
                case "comment" -> itemService.addComment(1L, 10L, new CreateCommentDto("Great"));
                default -> throw new AssertionError(operation);
            }
        }).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(itemRepository, bookingRepository, commentRepository);
    }

    @Test
    void getAllByOwnerWithCommentsAndBookings() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        Item second = Item.builder()
                .id(11L).name("Saw").description("Hand saw").available(true).owner(owner).build();
        when(itemRepository.findByOwnerIdOrderByIdAsc(1L)).thenReturn(List.of(item, second));
        when(commentRepository.findByItemIdInOrderByCreatedAsc(List.of(10L, 11L)))
                .thenReturn(List.of(createComment(40, item, owner), createComment(41, second, booker)));
        LocalDateTime now = LocalDateTime.now();
        when(bookingRepository.findByItemIdInAndStatus(List.of(10L, 11L), BookingStatus.APPROVED)).thenReturn(List.of(
                createBooking(20, booker, now.minusDays(10), now.minusDays(9)),
                createBooking(21, booker, now.plusDays(10), now.plusDays(11)),
                createBooking(22, otherBooker, now.minusDays(2), now.minusDays(1)),
                createBooking(23, otherBooker, now.plusDays(2), now.plusDays(3))));

        List<ItemDto> actual = itemService.getAllByOwner(1);

        assertThat(actual).extracting(ItemDto::getId).containsExactly(10L, 11L);
        assertThat(actual.getFirst().getLastBooking().getId()).isEqualTo(22L);
        assertThat(actual.getFirst().getLastBooking().getBookerId()).isEqualTo(3L);
        assertThat(actual.getFirst().getLastBooking().getStart()).isEqualTo(now.minusDays(2));
        assertThat(actual.getFirst().getLastBooking().getEnd()).isEqualTo(now.minusDays(1));
        assertThat(actual.getFirst().getNextBooking().getId()).isEqualTo(23L);
        assertThat(actual.getFirst().getComments()).extracting(CommentDto::getId).containsExactly(40L);
        assertThat(actual.get(1).getComments()).extracting(CommentDto::getId).containsExactly(41L);
        assertThat(actual.get(1).getLastBooking()).isNull();
        assertThat(actual.get(1).getNextBooking()).isNull();
    }

    @Test
    void getAllByOwnerWithoutCommentsAndBookings() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findByOwnerIdOrderByIdAsc(1L)).thenReturn(List.of(item));

        assertThat(itemService.getAllByOwner(1)).singleElement().satisfies(actual -> {
            assertThat(actual.getComments()).isEmpty();
            assertThat(actual.getLastBooking()).isNull();
            assertThat(actual.getNextBooking()).isNull();
        });
    }

    @Test
    void emptyOwnerItemsDoNotLoadRelatedData() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertThat(itemService.getAllByOwner(1)).isEmpty();
        verifyNoInteractions(commentRepository, bookingRepository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void blankSearchReturnsEmptyWithoutQuery(String text) {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertThat(itemService.searchAvailableByText(1, text)).isEmpty();
        verifyNoInteractions(itemRepository);
    }

    @Test
    void searchMapsRepositoryResults() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.searchAvailable("DrIlL")).thenReturn(List.of(item));

        assertThat(itemService.searchAvailableByText(1, "DrIlL")).singleElement().satisfies(actual -> {
            assertThat(actual.getId()).isEqualTo(10L);
            assertThat(actual.getName()).isEqualTo("Drill");
        });
    }

    @Test
    void addCommentAfterRental() {
        stubUserAndItem();
        when(bookingRepository.existsByBookerIdAndItemIdAndStatusAndEndIsBefore(
                eq(1L), eq(10L), eq(BookingStatus.APPROVED), any())).thenReturn(true);
        when(commentRepository.save(any())).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setId(40L);
            return saved;
        });
        LocalDateTime before = LocalDateTime.now();

        CommentDto actual = itemService.addComment(1L, 10L, new CreateCommentDto("Excellent"));

        assertThat(actual.getId()).isEqualTo(40L);
        assertThat(actual.getText()).isEqualTo("Excellent");
        assertThat(actual.getAuthorName()).isEqualTo("Owner");
        assertThat(actual.getCreated()).isBetween(before, LocalDateTime.now());
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getAuthor()).isSameAs(owner);
        assertThat(captor.getValue().getItem()).isSameAs(item);
    }

    @Test
    void commentRequiresCompletedApprovedRental() {
        stubUserAndItem();

        assertThatThrownBy(() -> itemService.addComment(1L, 10L, new CreateCommentDto("Excellent")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void commentRejectsMissingItem() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> itemService.addComment(1L, 10L, new CreateCommentDto("Excellent")))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(bookingRepository, commentRepository);
    }

    private void stubUserAndItem() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
    }

    private Comment createComment(long id, Item targetItem, User author) {
        return Comment.builder()
                .id(id).text("Works well").item(targetItem).author(author).created(created).build();
    }

    private Booking createBooking(long id, User renter, LocalDateTime start, LocalDateTime end) {
        return Booking.builder()
                .id(id).item(item).booker(renter).status(BookingStatus.APPROVED).start(start).end(end).build();
    }
}
