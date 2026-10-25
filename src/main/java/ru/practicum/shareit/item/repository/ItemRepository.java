package ru.practicum.shareit.item.repository;

import ru.practicum.shareit.item.model.Item;

import java.util.List;
import java.util.Optional;

public interface ItemRepository {
    Item save(Item item);

    Optional<Item> findById(long id);

    List<Item> findByOwnerId(long id);

    List<Item> searchAvailable(String text);
}
