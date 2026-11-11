package ru.practicum.shareit.request;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import ru.practicum.shareit.request.dto.CreateItemRequestDto;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ItemRequestClientTest {
    private ItemRequestClient itemRequestClient;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestTemplateBuilder builder = new RestTemplateBuilder()
                .additionalCustomizers(rest -> server = MockRestServiceServer.createServer(rest));
        itemRequestClient = new ItemRequestClient("http://localhost:9090", builder);
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void createSendsDescriptionAndUserHeader() {
        server.expect(requestTo("http://localhost:9090/requests"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Sharer-User-Id", "2"))
                .andExpect(content().json("{\"description\":\"Need a drill\"}"))
                .andRespond(withSuccess("{\"id\":5,\"description\":\"Need a drill\"}", MediaType.APPLICATION_JSON));

        assertEquals(Map.of("id", 5, "description", "Need a drill"),
                itemRequestClient.create(2, new CreateItemRequestDto("Need a drill")).getBody());
    }

    @Test
    void getOwnSendsUserHeader() {
        server.expect(requestTo("http://localhost:9090/requests"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Sharer-User-Id", "2"))
                .andRespond(withSuccess("[{\"id\":5}]", MediaType.APPLICATION_JSON));

        assertEquals(List.of(Map.of("id", 5)), itemRequestClient.getOwn(2).getBody());
    }

    @Test
    void getAllDoesNotAddPaginationParameters() {
        server.expect(requestTo("http://localhost:9090/requests/all"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertEquals(List.of(), itemRequestClient.getAll(1).getBody());
    }

    @Test
    void getByIdUsesRequestIdInPath() {
        server.expect(requestTo("http://localhost:9090/requests/5"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andRespond(withSuccess("{\"id\":5}", MediaType.APPLICATION_JSON));

        assertEquals(Map.of("id", 5), itemRequestClient.getById(1, 5).getBody());
    }
}
