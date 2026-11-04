package ru.practicum.shareit.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.CreateBookingRequest;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingState;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.BookingUnavailableException;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    private static final Sort START_DESC = Sort.by(Sort.Direction.DESC, "start");


    @Transactional
    @Override
    public BookingDto create(long userId, CreateBookingRequest createBookingRequest) {
        validateEndAfterStart(createBookingRequest.getStart(), createBookingRequest.getEnd());

        Item item = getItemOrThrow(createBookingRequest.getItemId());
        User booker = getUserOrThrow(userId);

        if (!Boolean.TRUE.equals(item.getAvailable())) {
            throw new BookingUnavailableException("Вещь недоступна для бронирования");
        }

        if (item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Владелец не может забронировать свою вещь");
        }

        checkBookingOverlap(item.getId(), createBookingRequest.getStart(), createBookingRequest.getEnd());

        Booking booking = bookingRepository.save(BookingMapper.toBooking(createBookingRequest, item, booker, BookingStatus.WAITING));

        return BookingMapper.toBookingDto(booking);
    }

    @Transactional
    @Override
    public BookingDto approve(Long userId, Long bookingId, boolean approved) {
        Booking booking = getBookingOrThrow(bookingId);
        Item item = getItemOrThrow(booking.getItem().getId());

        if (!userRepository.existsById(userId) || !userId.equals(item.getOwner().getId())) {
            throw new ForbiddenException("Подтверждение доступно только владельцу вещи");
        }

        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new BookingUnavailableException("Бронирование уже рассмотрено");
        }

        if (approved) {
            checkBookingOverlap(booking.getItem().getId(), booking.getStart(), booking.getEnd());
            booking.setStatus(BookingStatus.APPROVED);
        } else {
            booking.setStatus(BookingStatus.REJECTED);
        }

        return BookingMapper.toBookingDto(bookingRepository.save(booking));
    }

    @Override
    public BookingDto getById(Long userId, Long bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        boolean isBooker = booking.getBooker().getId().equals(userId);
        boolean isOwner = booking.getItem().getOwner().getId().equals(userId);
        if (!isBooker && !isOwner) {
            throw new NotFoundException("Бронирование не найдено: " + bookingId);
        }
        return BookingMapper.toBookingDto(booking);
    }

    @Override
    public List<BookingDto> getByBooker(Long userId, String state) {
        getUserOrThrow(userId);
        BookingState bookingState = BookingState.from(state);
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = switch (bookingState) {
            case ALL -> bookingRepository.findByBookerId(userId, START_DESC);
            case CURRENT -> bookingRepository
                    .findByBookerIdAndStartLessThanEqualAndEndGreaterThanEqual(userId, now, now, START_DESC);
            case PAST -> bookingRepository.findByBookerIdAndEndIsBefore(userId, now, START_DESC);
            case FUTURE -> bookingRepository.findByBookerIdAndStartIsAfter(userId, now, START_DESC);
            case WAITING -> bookingRepository.findByBookerIdAndStatus(userId, BookingStatus.WAITING, START_DESC);
            case REJECTED -> bookingRepository.findByBookerIdAndStatus(userId, BookingStatus.REJECTED, START_DESC);
        };

        return bookings.stream()
                .map(BookingMapper::toBookingDto)
                .toList();
    }

    @Override
    public List<BookingDto> getByOwner(Long userId, String state) {
        getUserOrThrow(userId);
        BookingState bookingState = BookingState.from(state);
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = switch (bookingState) {
            case ALL -> bookingRepository.findByItem_OwnerId(userId, START_DESC);
            case CURRENT -> bookingRepository
                    .findByItem_OwnerIdAndStartLessThanEqualAndEndGreaterThanEqual(userId, now, now, START_DESC);
            case PAST -> bookingRepository.findByItem_OwnerIdAndEndIsBefore(userId, now, START_DESC);
            case FUTURE -> bookingRepository.findByItem_OwnerIdAndStartIsAfter(userId, now, START_DESC);
            case WAITING -> bookingRepository.findByItem_OwnerIdAndStatus(userId, BookingStatus.WAITING, START_DESC);
            case REJECTED -> bookingRepository
                    .findByItem_OwnerIdAndStatus(userId, BookingStatus.REJECTED, START_DESC);
        };

        return bookings.stream()
                .map(BookingMapper::toBookingDto)
                .toList();
    }

    private void validateEndAfterStart(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Дата окончания должна быть позже даты начала");
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + userId));
    }

    private Item getItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь не найдена: " + itemId));
    }

    private Booking getBookingOrThrow(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование не найдено: " + bookingId));
    }

    private void checkBookingOverlap(Long itemId, LocalDateTime start, LocalDateTime end) {
        boolean overlaps = bookingRepository.findByItemIdAndStatus(itemId, BookingStatus.APPROVED).stream()
                .anyMatch(booking -> booking.getStart().isBefore(end) && booking.getEnd().isAfter(start));
        if (overlaps) {
            throw new BookingUnavailableException("Вещь уже забронирована на выбранные даты");
        }
    }

}

