package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.dto.CreateItemRequest;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.PatchItemRequest;
import ru.practicum.shareit.item.service.ItemService;

import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @PostMapping
    public ItemDto createItem(@RequestHeader(USER_ID_HEADER) Long userId,
                              @Valid @RequestBody CreateItemRequest createItemRequest) {
        return itemService.create(userId, createItemRequest);
    }

    @PatchMapping("/{itemId}")
    public ItemDto patchItem(@RequestHeader(USER_ID_HEADER) Long userId, 
                         @Valid @RequestBody PatchItemRequest patchItemRequest,
                         @PathVariable Long itemId) {
        return itemService.patch(userId, itemId, patchItemRequest);
    }

    @GetMapping("/{itemId}")
    public ItemDto getById(@RequestHeader(USER_ID_HEADER) Long userId,
                           @PathVariable Long itemId) {
        return itemService.getById(userId, itemId);
    }

    @GetMapping
    public List<ItemDto> getAllByOwner(@RequestHeader(USER_ID_HEADER) Long userId) {
        return itemService.getAllByOwner(userId);
    }

    @GetMapping("/search")
    public List<ItemDto> search(@RequestHeader(USER_ID_HEADER) Long userId,
                                @RequestParam String text) {
        return itemService.searchAvailableByText(userId, text);
    }

}
