package ru.practicum.shareit.item.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.shareit.item.dto.CreateItemRequestDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.PatchItemRequestDto;
import ru.practicum.shareit.item.model.Item;

@UtilityClass
public class ItemMapper {

    public ItemDto toItemDto(Item item) {
        if (item == null) {
            return null;
        }

        Long requestId = item.getRequest() == null
                ? null
                : item.getRequest().getId();

        return ItemDto.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .available(item.getAvailable())
                .requestId(requestId)
                .build();
    }

    public Item toItem(CreateItemRequestDto createItemRequestDto) {
        if (createItemRequestDto == null) {
            return null;
        }
        return Item.builder()
                .name(createItemRequestDto.getName())
                .description(createItemRequestDto.getDescription())
                .available(createItemRequestDto.getAvailable())
                .build();
    }

    public void patchItem(Item item, PatchItemRequestDto patchItemRequestDto) {
        if (item == null || patchItemRequestDto == null) {
            return;
        }
        if (patchItemRequestDto.getName() != null) {
            item.setName(patchItemRequestDto.getName());
        }
        if (patchItemRequestDto.getDescription() != null) {
            item.setDescription(patchItemRequestDto.getDescription());
        }
        if (patchItemRequestDto.getAvailable() != null) {
            item.setAvailable(patchItemRequestDto.getAvailable());
        }
    }
}
