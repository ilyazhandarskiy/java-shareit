package ru.practicum.shareit.booking;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.item.dto.CreateItemRequestDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.dto.CreateUserRequestDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties =
        "spring.datasource.url=jdbc:h2:mem:booking-service-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@ActiveProfiles("test")
@Transactional
class BookingServiceIT {
    @Autowired
    private UserService userService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private BookingService bookingService;
    @Autowired
    private EntityManager entityManager;

    private UserDto owner;
    private UserDto booker;

    @BeforeEach
    void setUp() {
        owner = userService.create(new CreateUserRequestDto("Owner", "owner@example.com"));
        booker = userService.create(new CreateUserRequestDto("Booker", "booker@example.com"));
    }

    @Test
    void createBookingWhenPreviousWasRejected() {
        ItemDto item = itemService.create(owner.getId(), new CreateItemRequestDto("Drill", "Cordless", true, null));
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        BookingRequestDto bookingRequest = new BookingRequestDto(item.getId(), start, start.plusDays(1));
        BookingDto rejected = bookingService.create(booker.getId(), bookingRequest);
        bookingService.approve(owner.getId(), rejected.getId(), false);
        flushAndClear();

        BookingDto booking = bookingService.create(booker.getId(), bookingRequest);
        bookingService.approve(owner.getId(), booking.getId(), true);
        flushAndClear();

        List<BookingDto> rejectedBookings = bookingService.getByBooker(booker.getId(), "REJECTED");
        assertEquals(1, rejectedBookings.size());
        assertEquals(rejected.getId(), rejectedBookings.getFirst().getId());
        assertTrue(bookingService.getByOwner(owner.getId(), "WAITING").isEmpty());
        assertEquals(BookingStatus.APPROVED, bookingService.getById(owner.getId(), booking.getId()).getStatus());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
