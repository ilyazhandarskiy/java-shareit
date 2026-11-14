package ru.practicum.shareit.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.domain.Sort;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingState;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:repository-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class BookingRepositoryTest {
    private static final LocalDateTime TIME = LocalDateTime.of(2030, 1, 15, 12, 0);

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private BookingRepository bookingRepository;

    private User owner;
    private User booker;
    private User stranger;
    private Item drill;
    private Item otherItem;
    private final Sort sort = Sort.by(Sort.Direction.DESC, "start");

    @BeforeEach
    void setUp() {
        owner = persistUser(1);
        booker = persistUser(2);
        stranger = persistUser(3);
        drill = persistItem(10, owner);
        otherItem = persistItem(11, stranger);
    }

    @ParameterizedTest
    @EnumSource(BookingState.class)
    void findBookingsByBookerAndState(BookingState state) {
        List<Booking> expected = seedBookings(state);
        flushAndClear();

        List<Booking> actual = switch (state) {
            case ALL -> bookingRepository.findByBookerId(booker.getId(), sort);
            case CURRENT -> bookingRepository.findByBookerIdAndStartLessThanEqualAndEndGreaterThanEqual(
                    booker.getId(), TIME, TIME, sort);
            case PAST -> bookingRepository.findByBookerIdAndEndIsBefore(booker.getId(), TIME, sort);
            case FUTURE -> bookingRepository.findByBookerIdAndStartIsAfter(booker.getId(), TIME, sort);
            case WAITING -> bookingRepository.findByBookerIdAndStatus(booker.getId(), BookingStatus.WAITING, sort);
            case REJECTED -> bookingRepository.findByBookerIdAndStatus(booker.getId(), BookingStatus.REJECTED, sort);
        };

        assertThat(actual).extracting(Booking::getId).containsExactlyElementsOf(expected.stream().map(Booking::getId).toList());
    }

    @ParameterizedTest
    @EnumSource(BookingState.class)
    void findBookingsByOwnerAndState(BookingState state) {
        List<Booking> expected = seedBookings(state);
        flushAndClear();

        List<Booking> actual = switch (state) {
            case ALL -> bookingRepository.findByItem_OwnerId(owner.getId(), sort);
            case CURRENT -> bookingRepository.findByItem_OwnerIdAndStartLessThanEqualAndEndGreaterThanEqual(
                    owner.getId(), TIME, TIME, sort);
            case PAST -> bookingRepository.findByItem_OwnerIdAndEndIsBefore(owner.getId(), TIME, sort);
            case FUTURE -> bookingRepository.findByItem_OwnerIdAndStartIsAfter(owner.getId(), TIME, sort);
            case WAITING -> bookingRepository.findByItem_OwnerIdAndStatus(owner.getId(), BookingStatus.WAITING, sort);
            case REJECTED -> bookingRepository.findByItem_OwnerIdAndStatus(owner.getId(), BookingStatus.REJECTED, sort);
        };

        assertThat(actual).extracting(Booking::getId).containsExactlyElementsOf(expected.stream().map(Booking::getId).toList());
    }

    @Test
    void itemBookingQueriesFilterBothItemIdsAndStatus() {
        Booking approved = persistBooking(drill, booker, BookingStatus.APPROVED, -2, -1);
        persistBooking(drill, booker, BookingStatus.REJECTED, -4, -3);
        persistBooking(otherItem, booker, BookingStatus.APPROVED, 1, 2);
        flushAndClear();

        assertThat(bookingRepository.findByItemIdAndStatus(drill.getId(), BookingStatus.APPROVED))
                .extracting(Booking::getId).containsExactly(approved.getId());
        assertThat(bookingRepository.findByItemIdInAndStatus(List.of(drill.getId()), BookingStatus.APPROVED))
                .extracting(Booking::getId).containsExactly(approved.getId());
    }

    @Test
    void existsCompletedApprovedBooking() {
        Booking rental = persistBooking(drill, booker, BookingStatus.WAITING, -2, -1);
        assertThat(canReview(booker, drill)).isFalse();
        rental.setStatus(BookingStatus.REJECTED);
        assertThat(canReview(booker, drill)).isFalse();
        rental.setStatus(BookingStatus.APPROVED);
        rental.setEnd(TIME);
        assertThat(canReview(booker, drill)).isFalse();
        rental.setEnd(TIME.plusDays(1));
        assertThat(canReview(booker, drill)).isFalse();
        rental.setEnd(TIME.minusDays(1));
        assertThat(canReview(booker, drill)).isTrue();
        assertThat(canReview(stranger, drill)).isFalse();
        assertThat(canReview(booker, otherItem)).isFalse();
    }

    private List<Booking> seedBookings(BookingState state) {
        Booking past = persistBooking(drill, booker, BookingStatus.APPROVED, -4, -3);
        Booking current = persistBooking(drill, booker, BookingStatus.APPROVED, -1, 1);
        Booking future = persistBooking(drill, booker, BookingStatus.APPROVED, 3, 4);
        Booking waiting = persistBooking(drill, booker, BookingStatus.WAITING, 5, 6);
        Booking rejected = persistBooking(drill, booker, BookingStatus.REJECTED, 7, 8);
        Booking startsNow = persistBooking(drill, booker, BookingStatus.APPROVED, 0, 2);
        Booking endsNow = persistBooking(drill, booker, BookingStatus.APPROVED, -2, 0);
        persistBooking(otherItem, stranger, BookingStatus.WAITING, 10, 11);
        return switch (state) {
            case ALL -> List.of(rejected, waiting, future, startsNow, current, endsNow, past);
            case CURRENT -> List.of(startsNow, current, endsNow);
            case PAST -> List.of(past);
            case FUTURE -> List.of(rejected, waiting, future);
            case WAITING -> List.of(waiting);
            case REJECTED -> List.of(rejected);
        };
    }

    private Booking persistBooking(Item item, User user, BookingStatus status, int startDays, int endDays) {
        return entityManager.persist(Booking.builder().item(item).booker(user).status(status)
                .start(TIME.plusDays(startDays)).end(TIME.plusDays(endDays)).build());
    }

    private boolean canReview(User user, Item item) {
        return bookingRepository.existsByBookerIdAndItemIdAndStatusAndEndIsBefore(user.getId(), item.getId(), BookingStatus.APPROVED, TIME);
    }

    private User persistUser(long number) {
        return entityManager.persist(User.builder()
                .name("User " + number).email("user" + number + "@example.com").build());
    }

    private Item persistItem(long number, User itemOwner) {
        return entityManager.persist(Item.builder()
                .name("Drill " + number).description("Cordless tool").available(true).owner(itemOwner).build());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
