package ru.practicum.shareit.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.item.repository.ItemRepository;


import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:repository-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ItemRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private ItemRepository itemRepository;

    private User owner;
    private Item drill;
    private Item otherItem;

    @BeforeEach
    void setUp() {
        owner = persistUser(1);
        User stranger = persistUser(3);
        drill = persistItem(10, owner);
        otherItem = persistItem(11, stranger);
    }

    @Test
    void searchAvailableByNameAndDescription() {
        drill.setName("Electric DRILL");
        otherItem.setDescription("A drill for wood");
        Item unavailable = persistItem(12, owner);
        unavailable.setName("Drill unavailable");
        unavailable.setAvailable(false);
        Item unrelated = persistItem(13, owner);
        unrelated.setName("Saw");
        flushAndClear();

        assertThat(itemRepository.searchAvailable("dRiLl")).extracting(Item::getId)
                .containsExactlyInAnyOrder(drill.getId(), otherItem.getId());
        assertThat(itemRepository.searchAvailable("nonexistent")).isEmpty();
    }

    @Test
    void ownerItemsExcludeOtherOwnersAndAreOrderedById() {
        Item second = persistItem(12, owner);
        flushAndClear();

        assertThat(itemRepository.findByOwnerIdOrderByIdAsc(owner.getId())).extracting(Item::getId)
                .containsExactly(drill.getId(), second.getId());
    }

    private User persistUser(long number) {
        return entityManager.persist(User.builder()
                .name("User " + number).email("user" + number + "@example.com").build());
    }

    private Item persistItem(long number, User itemOwner) {
        return entityManager.persist(Item.builder()
                .name("Drill " + number).description("Cordless tool").available(true).owner(itemOwner).build());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
