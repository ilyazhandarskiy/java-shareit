package ru.practicum.shareit.item.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.shareit.item.dto.CreateItemRequest;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.PatchItemRequest;
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

    public Item toItem(CreateItemRequest createItemRequest) {
        if (createItemRequest == null) {
            return null;
        }
        return Item.builder()
                .name(createItemRequest.getName())
                .description(createItemRequest.getDescription())
                .available(createItemRequest.getAvailable())
                .build();
    }

    public void patchItem(Item item, PatchItemRequest patchItemRequest) {
        if (item == null || patchItemRequest == null) {
            return;
        }
        if (patchItemRequest.getName() != null) {
            item.setName(patchItemRequest.getName());
        }
        if (patchItemRequest.getDescription() != null) {
            item.setDescription(patchItemRequest.getDescription());
        }
        if (patchItemRequest.getAvailable() != null) {
            item.setAvailable(patchItemRequest.getAvailable());
        }
    }
}
