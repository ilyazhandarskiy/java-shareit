package ru.practicum.shareit.request;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.request.dto.CreateItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDtoFull;
import ru.practicum.shareit.request.dto.ItemRequestDtoShort;
import ru.practicum.shareit.request.service.ItemRequestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/requests")
public class ItemRequestController {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    private final ItemRequestService itemRequestService;

    @PostMapping
    public ItemRequestDtoShort requestItem(@RequestHeader(USER_ID_HEADER) Long userId,
                                           @Valid @RequestBody CreateItemRequestDto dto) {
        return itemRequestService.create(dto, userId);
    }

    @GetMapping
    public List<ItemRequestDtoFull> getUserItems(@RequestHeader(USER_ID_HEADER) Long userId) {
        return itemRequestService.getUserItems(userId);
    }

    @GetMapping("/all")
    public List<ItemRequestDtoShort> getOtherUsersItems(@RequestHeader(USER_ID_HEADER) Long userId) {
        return itemRequestService.getOtherUsersItems(userId);
    }

    @GetMapping("/{requestId}")
    public ItemRequestDtoFull getItem(@RequestHeader(USER_ID_HEADER) Long userId,
                                      @PathVariable Long requestId) {
        return itemRequestService.findById(requestId, userId);
    }
}
