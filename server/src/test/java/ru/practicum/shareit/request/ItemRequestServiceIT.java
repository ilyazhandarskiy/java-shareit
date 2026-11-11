package ru.practicum.shareit.request;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.item.dto.CreateItemRequestDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.request.dto.ItemRequestDtoFull;
import ru.practicum.shareit.request.dto.ItemRequestDtoShort;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.user.dto.CreateUserRequestDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties =
        "spring.datasource.url=jdbc:h2:mem:request-service-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@ActiveProfiles("test")
@Transactional
class ItemRequestServiceIT {
    @Autowired
    private UserService userService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private ItemRequestService requestService;
    @Autowired
    private EntityManager entityManager;

    private UserDto owner;
    private UserDto booker;

    @BeforeEach
    void setUp() {
        owner = userService.create(new CreateUserRequestDto("Owner", "owner@example.com"));
        booker = userService.create(new CreateUserRequestDto("Booker", "booker@example.com"));
    }

    @Test
    void getRequestWhenItemWasAdded() {
        ItemRequestDtoShort request = requestService.create(
                new ru.practicum.shareit.request.dto.CreateItemRequestDto("Need a drill"), booker.getId());
        ItemDto item = itemService.create(owner.getId(),
                new CreateItemRequestDto("Drill", "Cordless", true, request.getId()));
        flushAndClear();

        ItemRequestDtoFull actual = requestService.findById(request.getId(), booker.getId());

        assertEquals(1, actual.getItems().size());
        assertEquals(item.getId(), actual.getItems().getFirst().getId());
        assertEquals(owner.getId(), actual.getItems().getFirst().getOwnerId());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
