package ru.practicum.shareit.booking.service;

import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.CreateBookingRequest;

import java.util.List;

public interface BookingService {
    BookingDto create(long userId, CreateBookingRequest createBookingRequest);

    BookingDto approve(Long userId, Long bookingId, boolean approved);

    BookingDto getById(Long userId, Long bookingId);

    List<BookingDto> getByBooker(Long userId, String state);

    List<BookingDto> getByOwner(Long userId, String state);
}

