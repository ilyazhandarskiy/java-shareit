package ru.practicum.shareit.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.CreateBookingRequest;
import ru.practicum.shareit.booking.service.BookingService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/bookings")
public class BookingController {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";
    private final BookingService bookingService;

    @PostMapping
    public BookingDto create(@RequestHeader(USER_ID_HEADER) Long userId,
                             @Valid @RequestBody CreateBookingRequest createBookingRequest) {
        return bookingService.create(userId, createBookingRequest);
    }

    @PatchMapping("/{bookingId}")
    public BookingDto approve(@RequestHeader(USER_ID_HEADER) Long userId,
                              @PathVariable Long bookingId,
                              @RequestParam boolean approved) {
        return bookingService.approve(userId, bookingId, approved);
    }

    @GetMapping("/{bookingId}")
    public BookingDto getById(@RequestHeader(USER_ID_HEADER) Long userId,
                              @PathVariable Long bookingId) {
        return bookingService.getById(userId, bookingId);
    }

    @GetMapping
    public List<BookingDto> getByBooker(@RequestHeader(USER_ID_HEADER) Long userId,
                                        @RequestParam(defaultValue = "ALL") String state) {
        return bookingService.getByBooker(userId, state);
    }

    @GetMapping("/owner")
    public List<BookingDto> getByOwner(@RequestHeader(USER_ID_HEADER) Long userId,
                                       @RequestParam(defaultValue = "ALL") String state) {
        return bookingService.getByOwner(userId, state);
    }
}
