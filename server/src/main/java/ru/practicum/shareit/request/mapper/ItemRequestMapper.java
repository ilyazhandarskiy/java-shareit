package ru.practicum.shareit.request.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.CreateItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDtoFull;
import ru.practicum.shareit.request.dto.ItemRequestDtoShort;
import ru.practicum.shareit.request.dto.LinkedItemDto;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

@UtilityClass
public class ItemRequestMapper {
    public ItemRequestDtoShort toItemRequestDtoShort(ItemRequest itemRequest) {
        return ItemRequestDtoShort.builder()
                .id(itemRequest.getId())
                .description(itemRequest.getDescription())
                .requestorId(itemRequest.getRequestor().getId())
                .created(itemRequest.getCreated())
                .build();
    }

    public ItemRequestDtoFull toItemRequestDtoFull(ItemRequest itemRequest) {
        return ItemRequestDtoFull.builder()
                .id(itemRequest.getId())
                .description(itemRequest.getDescription())
                .requestorId(itemRequest.getRequestor().getId())
                .created(itemRequest.getCreated())
                .items(toLinkedItemDto(itemRequest.getItems()))
                .build();
    }

    public ItemRequest toItemRequest(CreateItemRequestDto createItemRequestDto, User requestor, LocalDateTime created) {
        return ItemRequest.builder()
                .description(createItemRequestDto.getDescription())
                .requestor(requestor)
                .created(created)
                .build();
    }

    private List<LinkedItemDto> toLinkedItemDto(List<Item> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .map(item -> LinkedItemDto.builder()
                        .id(item.getId())
                        .name(item.getName())
                        .ownerId(item.getOwner().getId())
                        .build())
                .toList();
    }
}
