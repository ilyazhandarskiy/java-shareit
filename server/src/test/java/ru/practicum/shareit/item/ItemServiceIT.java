package ru.practicum.shareit.item;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.exception.BookingUnavailableException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.CreateCommentDto;
import ru.practicum.shareit.item.dto.CreateItemRequestDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.PatchItemRequestDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.dto.CreateUserRequestDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties =
        "spring.datasource.url=jdbc:h2:mem:item-service-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@ActiveProfiles("test")
@Transactional
class ItemServiceIT {
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
    void addCommentAfterBookingEnds() {
        UserDto otherUser = userService.create(new CreateUserRequestDto("Other", "other@example.com"));
        ItemDto item = itemService.create(owner.getId(), new CreateItemRequestDto("Drill", "Cordless", true, null));
        LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
        BookingRequestDto bookingRequest = new BookingRequestDto(item.getId(), start, start.plusDays(1));
        BookingDto booking = bookingService.create(booker.getId(), bookingRequest);

        assertEquals(BookingStatus.WAITING, booking.getStatus());
        assertThrows(NotFoundException.class, () -> bookingService.getById(otherUser.getId(), booking.getId()));
        assertEquals(BookingStatus.APPROVED,
                bookingService.approve(owner.getId(), booking.getId(), true).getStatus());
        assertThrows(BookingUnavailableException.class, () -> bookingService.create(otherUser.getId(), bookingRequest));
        flushAndClear();

        List<ItemDto> ownerItems = itemService.getAllByOwner(owner.getId());
        assertEquals(1, ownerItems.size());
        assertEquals(booking.getId(), ownerItems.getFirst().getNextBooking().getId());
        assertNull(ownerItems.getFirst().getLastBooking());
        assertThrows(IllegalArgumentException.class,
                () -> itemService.addComment(booker.getId(), item.getId(), new CreateCommentDto("Works well")));

        Booking savedBooking = entityManager.find(Booking.class, booking.getId());
        savedBooking.setStart(LocalDateTime.now().minusDays(2));
        savedBooking.setEnd(LocalDateTime.now().minusDays(1));
        flushAndClear();

        CommentDto comment = itemService.addComment(booker.getId(), item.getId(), new CreateCommentDto("Works well"));
        flushAndClear();

        assertNotNull(comment.getId());
        assertTrue(comment.getId() > 0);
        assertEquals("Booker", comment.getAuthorName());
        List<CommentDto> comments = itemService.getById(otherUser.getId(), item.getId()).getComments();
        assertEquals(1, comments.size());
        assertEquals("Works well", comments.getFirst().getText());

        ownerItems = itemService.getAllByOwner(owner.getId());
        assertEquals(1, ownerItems.size());
        ItemDto actual = ownerItems.getFirst();
        assertEquals(booking.getId(), actual.getLastBooking().getId());
        assertNull(actual.getNextBooking());
        assertEquals(1, actual.getComments().size());
        assertEquals(comment.getId(), actual.getComments().getFirst().getId());
    }

    @Test
    void searchWhenItemBecameUnavailable() {
        ItemDto item = itemService.create(owner.getId(), new CreateItemRequestDto("Drill", "Cordless", true, null));
        itemService.patch(owner.getId(), item.getId(), PatchItemRequestDto.builder().available(false).build());
        flushAndClear();

        ItemDto actual = itemService.getById(owner.getId(), item.getId());

        assertEquals("Drill", actual.getName());
        assertEquals("Cordless", actual.getDescription());
        assertFalse(actual.getAvailable());
        assertTrue(itemService.searchAvailableByText(owner.getId(), "drill").isEmpty());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
