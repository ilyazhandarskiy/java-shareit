package ru.practicum.shareit.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:repository-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ItemRequestRepositoryTest {
    private static final LocalDateTime TIME = LocalDateTime.of(2030, 1, 15, 12, 0);

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private ItemRequestRepository requestRepository;

    private User owner;
    private User stranger;
    private Item drill;

    @BeforeEach
    void setUp() {
        owner = persistUser(1);
        stranger = persistUser(3);
        drill = persistItem(10, owner);
    }

    @Test
    void findRequestsByRequestor() {
        ItemRequest older = persistRequest(owner, -1);
        ItemRequest newer = persistRequest(owner, 1);
        ItemRequest other = persistRequest(stranger, 0);
        drill.setRequest(newer);
        Item secondAnswer = persistItem(12, owner);
        secondAnswer.setRequest(newer);
        flushAndClear();

        assertThat(requestRepository.findByRequestorIdOrderByCreatedDesc(owner.getId()))
                .extracting(ItemRequest::getId).containsExactly(newer.getId(), older.getId());
        assertThat(requestRepository.findByRequestorIdNotOrderByCreatedDesc(stranger.getId()))
                .extracting(ItemRequest::getId).containsExactly(newer.getId(), older.getId());
        assertThat(requestRepository.findByRequestorIdNotOrderByCreatedDesc(owner.getId()))
                .extracting(ItemRequest::getId).containsExactly(other.getId());
        assertThat(requestRepository.findById(newer.getId()).orElseThrow().getItems())
                .extracting(Item::getId).containsExactly(drill.getId(), secondAnswer.getId());
    }

    private ItemRequest persistRequest(User user, int days) {
        return entityManager.persist(ItemRequest.builder().requestor(user).description("Need tools")
                .created(TIME.plusDays(days)).build());
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
