package ru.practicum.shareit.booking.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByItemIdAndStatus(long itemId, BookingStatus bookingStatus);

    List<Booking> findByItemIdInAndStatus(Collection<Long> itemIds, BookingStatus status);

    List<Booking> findByBookerId(Long bookerId, Sort sort);

    List<Booking> findByBookerIdAndStartLessThanEqualAndEndGreaterThanEqual(Long bookerId,
                                                                            LocalDateTime start,
                                                                            LocalDateTime end,
                                                                            Sort sort);

    List<Booking> findByBookerIdAndEndIsBefore(Long bookerId, LocalDateTime end, Sort sort);

    List<Booking> findByBookerIdAndStartIsAfter(Long bookerId, LocalDateTime start, Sort sort);

    List<Booking> findByBookerIdAndStatus(Long bookerId, BookingStatus status, Sort sort);

    List<Booking> findByItem_OwnerId(Long ownerId, Sort sort);

    List<Booking> findByItem_OwnerIdAndStartLessThanEqualAndEndGreaterThanEqual(Long ownerId,
                                                                                LocalDateTime start,
                                                                                LocalDateTime end,
                                                                                Sort sort);

    List<Booking> findByItem_OwnerIdAndEndIsBefore(Long ownerId, LocalDateTime end, Sort sort);

    List<Booking> findByItem_OwnerIdAndStartIsAfter(Long ownerId, LocalDateTime start, Sort sort);

    List<Booking> findByItem_OwnerIdAndStatus(Long ownerId, BookingStatus status, Sort sort);

    boolean existsByBookerIdAndItemIdAndStatusAndEndBefore(Long bookerId,
                                                           Long itemId,
                                                           BookingStatus status,
                                                           LocalDateTime end);
}
