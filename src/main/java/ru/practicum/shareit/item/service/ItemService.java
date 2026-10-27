package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.*;

import java.util.List;

public interface ItemService {
    ItemDto create(long userId, CreateItemRequest createItemRequest);

    ItemDto patch(long userId, long itemId, PatchItemRequest patchItemRequest);

    ItemDto getById(long userId, long itemId);

    List<ItemDto> getAllByOwner(long userId);

    List<ItemDto> searchAvailableByText(long userId, String text);

    CommentDto addComment(Long userId, Long itemId, CreateCommentDto createCommentDto);
}
