package ru.practicum.shareit.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.request.dto.CreateItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDtoFull;
import ru.practicum.shareit.request.dto.ItemRequestDtoShort;
import ru.practicum.shareit.request.mapper.ItemRequestMapper;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestServiceImpl implements ItemRequestService {
    private final UserRepository userRepository;
    private final ItemRequestRepository itemRequestRepository;

    @Transactional
    @Override
    public ItemRequestDtoShort create(CreateItemRequestDto createItemRequestDto, Long userId) {
        User requestor = getUserOrThrow(userId);

        ItemRequest itemRequest = ItemRequestMapper
                .toItemRequest(createItemRequestDto, requestor, LocalDateTime.now());

        return ItemRequestMapper.toItemRequestDtoShort(itemRequestRepository.save(itemRequest));
    }

    @Override
    public List<ItemRequestDtoFull> getUserItems(Long userId) {
        getUserOrThrow(userId);
        List<ItemRequest> itemRequests = itemRequestRepository.findByRequestorIdOrderByCreatedDesc(userId);

        return itemRequests.stream()
                .map(ItemRequestMapper::toItemRequestDtoFull)
                .toList();
    }

    @Override
    public List<ItemRequestDtoShort> getOtherUsersItems(Long userId) {
        getUserOrThrow(userId);
        return itemRequestRepository.findByRequestorIdNotOrderByCreatedDesc(userId).stream()
                .map(ItemRequestMapper::toItemRequestDtoShort)
                .toList();
    }

    @Override
    public ItemRequestDtoFull findById(Long id, Long userId) {
        getUserOrThrow(userId);
        return ItemRequestMapper.toItemRequestDtoFull(itemRequestRepository.findById(id).orElseThrow(() -> new NotFoundException("Запрос пользователя не найден:" + id)));
    }


    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + userId));
    }
}
