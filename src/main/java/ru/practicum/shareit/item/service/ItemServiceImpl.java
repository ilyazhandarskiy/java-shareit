package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.*;
import ru.practicum.shareit.item.mapper.CommentMapper;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    @Transactional
    @Override
    public ItemDto create(long userId, CreateItemRequest createItemRequest) {
        User user = getUserOrThrow(userId);
        Item item = ItemMapper.toItem(createItemRequest);
        item.setOwner(user);
        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Transactional
    @Override
    public ItemDto patch(long userId, long itemId, PatchItemRequest patchItemRequest) {
        Item item = getItemOrThrow(itemId);
        if (item.getOwner().getId() != userId) {
            throw new NotFoundException("Пользователь c id: %d не имеет прав для изменения Item с id: %d"
                    .formatted(userId, itemId));
        }
        ItemMapper.patchItem(item, patchItemRequest);
        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    public ItemDto getById(long userId, long itemId) {
        getUserOrThrow(userId);
        ItemDto itemDto = ItemMapper.toItemDto(getItemOrThrow(itemId));
        List<CommentDto> comments = commentRepository.findByItemIdOrderByCreatedAsc(itemId).stream()
                .map(CommentMapper::toCommentDto)
                .toList();
        itemDto.setComments(comments);
        return itemDto;
    }

    @Override
    public List<ItemDto> getAllByOwner(long userId) {
        getUserOrThrow(userId);
        return enrichItemDto(itemRepository.findByOwnerIdOrderByIdAsc(userId));
    }

    @Override
    public List<ItemDto> searchAvailableByText(long userId, String text) {
        getUserOrThrow(userId);

        if (text == null || text.isBlank()) {
            return List.of();
        }

        return itemRepository.searchAvailable(text).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    @Transactional
    @Override
    public CommentDto addComment(Long userId, Long itemId, CreateCommentDto createCommentDto) {
        User author = getUserOrThrow(userId);
        Item item = getItemOrThrow(itemId);

        boolean isRented = bookingRepository.existsByBookerIdAndItemIdAndStatusAndEndBefore(
                userId, itemId, BookingStatus.APPROVED, LocalDateTime.now());
        if (!isRented) {
            throw new IllegalArgumentException("Отзыв можно оставить только после завершённой аренды");
        }

        Comment comment = Comment.builder()
                .text(createCommentDto.getText())
                .item(item)
                .author(author)
                .created(LocalDateTime.now())
                .build();

        return CommentMapper.toCommentDto(commentRepository.save(comment));
    }


    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь c id: " + userId + " не найден"));
    }

    private Item getItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь c id: " + itemId + " не найдена"));
    }

    private void applyBookings(ItemDto dto, List<Booking> bookings) {
        LocalDateTime now = LocalDateTime.now();
        bookings.stream()
                .filter(booking -> !booking.getStart().isAfter(now))
                .max(Comparator.comparing(Booking::getStart))
                .map(BookingMapper::toBookingShortDto)
                .ifPresent(dto::setLastBooking);
        bookings.stream()
                .filter(booking -> booking.getStart().isAfter(now))
                .min(Comparator.comparing(Booking::getStart))
                .map(BookingMapper::toBookingShortDto)
                .ifPresent(dto::setNextBooking);
    }

    private List<ItemDto> enrichItemDto(List<Item> items) {
        if (items.isEmpty()) {
            return List.of();
        }

        List<Long> itemIds = items.stream()
                .map(Item::getId)
                .toList();

        Map<Long, List<CommentDto>> commentsByItemId = loadCommentsByItemId(itemIds);
        Map<Long, List<Booking>> bookingsByItemId = loadApprovedBookingsByItemId(itemIds);

        return items.stream()
                .map(item -> toOwnerDto(
                        item,
                        commentsByItemId.getOrDefault(item.getId(), List.of()),
                        bookingsByItemId.getOrDefault(item.getId(), List.of())
                ))
                .toList();
    }

    private Map<Long, List<CommentDto>> loadCommentsByItemId(List<Long> itemIds) {
        return commentRepository.findByItemIdInOrderByCreatedAsc(itemIds).stream()
                .collect(Collectors.groupingBy(
                        comment -> comment.getItem().getId(),
                        Collectors.mapping(CommentMapper::toCommentDto, Collectors.toList())
                ));
    }

    private Map<Long, List<Booking>> loadApprovedBookingsByItemId(List<Long> itemIds) {
        return bookingRepository.findByItemIdInAndStatus(itemIds, BookingStatus.APPROVED).stream()
                .collect(Collectors.groupingBy(
                        booking -> booking.getItem().getId()
                ));
    }

    private ItemDto toOwnerDto(Item item, List<CommentDto> comments, List<Booking> bookings) {
        ItemDto dto = ItemMapper.toItemDto(item);
        dto.setComments(comments);
        applyBookings(dto, bookings);
        return dto;
    }

}
