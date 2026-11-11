package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingState;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.booking.service.BookingServiceImpl;
import ru.practicum.shareit.exception.BookingUnavailableException;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemRepository itemRepository;
    @InjectMocks
    private BookingServiceImpl bookingService;

    private final LocalDateTime start = LocalDateTime.of(2030, 1, 15, 12, 0);
    private final User owner = User.builder()
            .id(1L).name("Owner").email("owner@example.com").build();
    private final User booker = User.builder()
            .id(2L).name("Booker").email("booker@example.com").build();
    private final Item item = Item.builder()
            .id(10L).name("Drill").description("Cordless tool").available(true).owner(owner).build();
    private final BookingRequestDto bookingRequestDto = BookingRequestDto.builder()
            .itemId(10L).start(start).end(start.plusDays(1)).build();
    private final Sort sort = Sort.by(Sort.Direction.DESC, "start");

    @Test
    void createReturnsWaitingBooking() {
        stubCreate();
        when(bookingRepository.save(any())).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        BookingDto actual = bookingService.create(2, bookingRequestDto);

        assertThat(actual.getId()).isEqualTo(100L);
        assertThat(actual.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(actual.getStart()).isEqualTo(start);
        assertThat(actual.getEnd()).isEqualTo(start.plusDays(1));
        assertThat(actual.getItem().getId()).isEqualTo(10L);
        assertThat(actual.getBooker().getId()).isEqualTo(2L);
        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue().getItem()).isSameAs(item);
        assertThat(captor.getValue().getBooker()).isSameAs(booker);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void createThrowsWhenEndIsNotAfterStart(int days) {
        bookingRequestDto.setEnd(start.plusDays(days));

        assertThatThrownBy(() -> bookingService.create(2, bookingRequestDto)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(itemRepository, userRepository, bookingRepository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = false)
    void createRejectsUnavailableItem(Boolean available) {
        item.setAvailable(available);
        stubCreate();

        assertThatThrownBy(() -> bookingService.create(2, bookingRequestDto)).isInstanceOf(BookingUnavailableException.class);
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void createRejectsOwnerBookingTheirOwnItem() {
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> bookingService.create(1, bookingRequestDto)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void createRejectsMissingItem() {
        assertThatThrownBy(() -> bookingService.create(2, bookingRequestDto)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(userRepository, bookingRepository);
    }

    @Test
    void createRejectsMissingBooker() {
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> bookingService.create(2, bookingRequestDto)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(bookingRepository);
    }

    @ParameterizedTest
    @CsvSource({"-1,1", "0,24", "1,2", "-1,25", "23,25"})
    void createRejectsOverlappingApprovedBooking(int startHours, int endHours) {
        stubCreate();
        Booking existing = approvedBooking(start.plusHours(startHours), start.plusHours(endHours));
        when(bookingRepository.findByItemIdAndStatus(10, BookingStatus.APPROVED)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> bookingService.create(2, bookingRequestDto)).isInstanceOf(BookingUnavailableException.class);
        verify(bookingRepository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({"-24,-1", "-24,0", "24,48", "25,48"})
    void createWhenDatesDoNotOverlap(int startHours, int endHours) {
        stubCreate();
        Booking existing = approvedBooking(start.plusHours(startHours), start.plusHours(endHours));
        when(bookingRepository.findByItemIdAndStatus(10, BookingStatus.APPROVED)).thenReturn(List.of(existing));
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(bookingService.create(2, bookingRequestDto).getStatus()).isEqualTo(BookingStatus.WAITING);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void approveUpdatesStatus(boolean approved) {
        Booking waiting = stubApproval();
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BookingDto actual = bookingService.approve(1L, 20L, approved);

        BookingStatus expected = approved ? BookingStatus.APPROVED : BookingStatus.REJECTED;
        assertThat(actual.getStatus()).isEqualTo(expected);
        assertThat(waiting.getStatus()).isEqualTo(expected);
        verify(bookingRepository).save(waiting);
        if (approved) {
            verify(bookingRepository).findByItemIdAndStatus(10, BookingStatus.APPROVED);
        } else {
            verify(bookingRepository, never()).findByItemIdAndStatus(anyLong(), any());
        }
    }

    @Test
    void approveRejectsMissingBooking() {
        assertThatThrownBy(() -> bookingService.approve(1L, 20L, true)).isInstanceOf(NotFoundException.class);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void approveRejectsMissingItem() {
        when(bookingRepository.findById(20L)).thenReturn(Optional.of(waitingBooking()));

        assertThatThrownBy(() -> bookingService.approve(1L, 20L, true)).isInstanceOf(NotFoundException.class);
        verify(bookingRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void approveRejectsNonOwnerAndMissingUser(boolean userExists) {
        when(bookingRepository.findById(20L)).thenReturn(Optional.of(waitingBooking()));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(userRepository.existsById(3L)).thenReturn(userExists);

        assertThatThrownBy(() -> bookingService.approve(3L, 20L, true)).isInstanceOf(ForbiddenException.class);
        verify(bookingRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = "WAITING", mode = EnumSource.Mode.EXCLUDE)
    void approveRejectsAlreadyReviewedBooking(BookingStatus status) {
        Booking existing = stubApproval();
        existing.setStatus(status);

        assertThatThrownBy(() -> bookingService.approve(1L, 20L, true)).isInstanceOf(BookingUnavailableException.class);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void approveThrowsWhenDatesOverlap() {
        Booking waiting = stubApproval();
        when(bookingRepository.findByItemIdAndStatus(10, BookingStatus.APPROVED))
                .thenReturn(List.of(approvedBooking(start, start.plusDays(1))));

        assertThatThrownBy(() -> bookingService.approve(1L, 20L, true)).isInstanceOf(BookingUnavailableException.class);
        assertThat(waiting.getStatus()).isEqualTo(BookingStatus.WAITING);
        verify(bookingRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2})
    void getByIdAllowsOwnerAndBooker(long userId) {
        when(bookingRepository.findById(20L)).thenReturn(Optional.of(waitingBooking()));

        BookingDto actual = bookingService.getById(userId, 20L);

        assertThat(actual.getId()).isEqualTo(20L);
        assertThat(actual.getBooker().getEmail()).isEqualTo("booker@example.com");
        assertThat(actual.getItem().getName()).isEqualTo("Drill");
    }

    @Test
    void getByIdRejectsStranger() {
        when(bookingRepository.findById(20L)).thenReturn(Optional.of(waitingBooking()));

        assertThatThrownBy(() -> bookingService.getById(3L, 20L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getByIdRejectsMissingBooking() {
        assertThatThrownBy(() -> bookingService.getById(2L, 20L)).isInstanceOf(NotFoundException.class);
    }

    @ParameterizedTest
    @EnumSource(BookingState.class)
    void getByBookerWithState(BookingState state) {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        List<Booking> expected = List.of(waitingBooking());
        switch (state) {
            case ALL -> when(bookingRepository.findByBookerId(2L, sort)).thenReturn(expected);
            case CURRENT -> when(bookingRepository.findByBookerIdAndStartLessThanEqualAndEndGreaterThanEqual(
                    eq(2L), any(), any(), eq(sort))).thenReturn(expected);
            case PAST -> when(bookingRepository.findByBookerIdAndEndIsBefore(eq(2L), any(), eq(sort))).thenReturn(expected);
            case FUTURE -> when(bookingRepository.findByBookerIdAndStartIsAfter(eq(2L), any(), eq(sort))).thenReturn(expected);
            case WAITING -> when(bookingRepository.findByBookerIdAndStatus(2L, BookingStatus.WAITING, sort)).thenReturn(expected);
            case REJECTED -> when(bookingRepository.findByBookerIdAndStatus(2L, BookingStatus.REJECTED, sort)).thenReturn(expected);
        }

        assertThat(bookingService.getByBooker(2L, state.name())).extracting(BookingDto::getId).containsExactly(20L);
    }

    @ParameterizedTest
    @EnumSource(BookingState.class)
    void getByOwnerWithState(BookingState state) {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        List<Booking> expected = List.of(waitingBooking());
        switch (state) {
            case ALL -> when(bookingRepository.findByItem_OwnerId(1L, sort)).thenReturn(expected);
            case CURRENT -> when(bookingRepository.findByItem_OwnerIdAndStartLessThanEqualAndEndGreaterThanEqual(
                    eq(1L), any(), any(), eq(sort))).thenReturn(expected);
            case PAST -> when(bookingRepository.findByItem_OwnerIdAndEndIsBefore(eq(1L), any(), eq(sort))).thenReturn(expected);
            case FUTURE -> when(bookingRepository.findByItem_OwnerIdAndStartIsAfter(eq(1L), any(), eq(sort))).thenReturn(expected);
            case WAITING -> when(bookingRepository.findByItem_OwnerIdAndStatus(1L, BookingStatus.WAITING, sort)).thenReturn(expected);
            case REJECTED -> when(bookingRepository.findByItem_OwnerIdAndStatus(1L, BookingStatus.REJECTED, sort)).thenReturn(expected);
        }

        assertThat(bookingService.getByOwner(1L, state.name())).extracting(BookingDto::getId).containsExactly(20L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void listsRejectMissingUser(boolean byOwner) {
        assertThatThrownBy(() -> list(byOwner, "ALL")).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(bookingRepository);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void listsRejectUnknownState(boolean byOwner) {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> list(byOwner, "UNKNOWN")).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(bookingRepository);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void listsReturnEmptyWhenNoBookings(boolean byOwner) {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertThat(list(byOwner, "ALL")).isEmpty();
    }

    private List<BookingDto> list(boolean byOwner, String state) {
        return byOwner ? bookingService.getByOwner(1L, state) : bookingService.getByBooker(1L, state);
    }

    private void stubCreate() {
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
    }

    private Booking waitingBooking() {
        return Booking.builder()
                .id(20L).item(item).booker(booker).status(BookingStatus.WAITING)
                .start(start).end(start.plusDays(1)).build();
    }

    private Booking approvedBooking(LocalDateTime bookingStart, LocalDateTime bookingEnd) {
        return Booking.builder()
                .id(21L).item(item).booker(booker).status(BookingStatus.APPROVED)
                .start(bookingStart).end(bookingEnd).build();
    }

    private Booking stubApproval() {
        Booking waiting = waitingBooking();
        when(bookingRepository.findById(20L)).thenReturn(Optional.of(waiting));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(userRepository.existsById(1L)).thenReturn(true);
        return waiting;
    }
}
