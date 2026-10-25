package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.CreateItemRequest;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.PatchItemRequest;

import java.util.List;

public interface ItemService {
    ItemDto create(long userId, CreateItemRequest createItemRequest);

    ItemDto patch(long userId, long itemId, PatchItemRequest patchItemRequest);

    ItemDto getById(long userId, long itemId);

    List<ItemDto> getAllByOwner(long userId);

    List<ItemDto> searchAvailableByText(long userId, String text);
}
