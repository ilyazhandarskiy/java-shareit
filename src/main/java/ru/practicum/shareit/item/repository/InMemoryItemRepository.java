package ru.practicum.shareit.item.repository;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.model.Item;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryItemRepository implements ItemRepository {
    private final Map<Long, Item> items = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public Item save(Item item) {
        if (item.getId() == null) {
            item.setId(idGenerator.incrementAndGet());
        }

        items.put(item.getId(), item);
        return item;
    }

    @Override
    public Optional<Item> findById(long id) {
        return Optional.ofNullable(items.get(id));
    }

    @Override
    public List<Item> findByOwnerId(long id) {
        return items.values().stream()
                .filter(item -> item.getOwner().getId() == id)
                .collect(Collectors.toList());
    }

    @Override
    public List<Item> searchAvailable(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        String query = text.strip().toLowerCase();

        return items.values().stream()
                .filter(item -> item.getAvailable() == true)
                .filter(item -> containsIgnoreCase(item.getName(), query)
                        || containsIgnoreCase(item.getDescription(), query))
                .collect(Collectors.toList());
    }

    private static boolean containsIgnoreCase(String source, String query) {
        if (source == null) {
            return false;
        }
        return source.toLowerCase().contains(query);
    }

}
