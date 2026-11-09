package ru.practicum.shareit.request.service;

import ru.practicum.shareit.request.dto.CreateItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDtoFull;
import ru.practicum.shareit.request.dto.ItemRequestDtoShort;

import java.util.List;

public interface ItemRequestService {
    ItemRequestDtoShort create(CreateItemRequestDto createItemRequestDto, Long userId);

    List<ItemRequestDtoFull> getUserItems(Long userId);

    List<ItemRequestDtoShort> getOtherUsersItems(Long userId);

    ItemRequestDtoFull findById(Long id, Long userId);
}
