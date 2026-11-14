package ru.practicum.shareit.item;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import ru.practicum.shareit.item.dto.CreateCommentDto;
import ru.practicum.shareit.item.dto.CreateItemRequestDto;
import ru.practicum.shareit.item.dto.PatchItemRequestDto;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ItemClientTest {
    private ItemClient itemClient;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestTemplateBuilder builder = new RestTemplateBuilder()
                .additionalCustomizers(rest -> server = MockRestServiceServer.createServer(rest));
        itemClient = new ItemClient("http://localhost:9090", builder);
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void createSendsItemAndRequestId() {
        CreateItemRequestDto dto = new CreateItemRequestDto("Drill", "Cordless", true, 9L);
        server.expect(requestTo("http://localhost:9090/items"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andExpect(content().json("{\"name\":\"Drill\",\"description\":\"Cordless\",\"available\":true,\"requestId\":9}"))
                .andRespond(withSuccess("{\"id\":3,\"requestId\":9}", MediaType.APPLICATION_JSON));

        assertEquals(Map.of("id", 3, "requestId", 9), itemClient.create(1, dto).getBody());
    }

    @Test
    void updateSendsPatchWithFalseAvailability() {
        PatchItemRequestDto dto = PatchItemRequestDto.builder().available(false).build();
        server.expect(requestTo("http://localhost:9090/items/3"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andExpect(content().json("{\"available\":false}"))
                .andRespond(withSuccess("{\"id\":3,\"available\":false}", MediaType.APPLICATION_JSON));

        assertEquals(Map.of("id", 3, "available", false), itemClient.update(1, 3, dto).getBody());
    }

    @Test
    void getByIdUsesItemIdInPath() {
        server.expect(requestTo("http://localhost:9090/items/3"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Sharer-User-Id", "2"))
                .andRespond(withSuccess("{\"id\":3}", MediaType.APPLICATION_JSON));

        assertEquals(Map.of("id", 3), itemClient.getById(2, 3).getBody());
    }

    @Test
    void getAllSendsOwnerHeader() {
        server.expect(requestTo("http://localhost:9090/items"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andRespond(withSuccess("[{\"id\":3}]", MediaType.APPLICATION_JSON));

        assertEquals(List.of(Map.of("id", 3)), itemClient.getAll(1).getBody());
    }

    @Test
    void searchEncodesTextInQuery() {
        server.expect(requestTo("http://localhost:9090/items/search?text=cordless%20drill%20%26%20saw"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Sharer-User-Id", "2"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertEquals(List.of(), itemClient.search(2, "cordless drill & saw").getBody());
    }

    @Test
    void addCommentSendsTextAndAuthorHeader() {
        server.expect(requestTo("http://localhost:9090/items/3/comment"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Sharer-User-Id", "2"))
                .andExpect(content().json("{\"text\":\"Works well\"}"))
                .andRespond(withSuccess("{\"id\":7,\"text\":\"Works well\"}", MediaType.APPLICATION_JSON));

        assertEquals(Map.of("id", 7, "text", "Works well"),
                itemClient.addComment(2, 3, new CreateCommentDto("Works well")).getBody());
    }
}
