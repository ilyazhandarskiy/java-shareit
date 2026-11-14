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
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.repository.CommentRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:repository-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CommentRepositoryTest {
    private static final LocalDateTime TIME = LocalDateTime.of(2030, 1, 15, 12, 0);

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private CommentRepository commentRepository;

    private User booker;
    private Item drill;
    private Item otherItem;

    @BeforeEach
    void setUp() {
        User owner = persistUser(1);
        booker = persistUser(2);
        User stranger = persistUser(3);
        drill = persistItem(10, owner);
        otherItem = persistItem(11, stranger);
    }

    @Test
    void commentsAreFilteredByItemAndOrderedByCreationTime() {
        Comment newer = persistComment(drill, 1);
        Comment older = persistComment(drill, -1);
        persistComment(otherItem, 0);
        flushAndClear();

        assertThat(commentRepository.findByItemIdOrderByCreatedAsc(drill.getId()))
                .extracting(Comment::getId).containsExactly(older.getId(), newer.getId());
        assertThat(commentRepository.findByItemIdInOrderByCreatedAsc(List.of(drill.getId())))
                .extracting(Comment::getId).containsExactly(older.getId(), newer.getId());
        assertThat(commentRepository.findByItemIdInOrderByCreatedAsc(List.of(drill.getId(), otherItem.getId())))
                .extracting(Comment::getCreated).containsExactly(TIME.minusDays(1), TIME, TIME.plusDays(1));
    }

    private Comment persistComment(Item item, int days) {
        return entityManager.persist(Comment.builder().item(item).author(booker).text("Works")
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
