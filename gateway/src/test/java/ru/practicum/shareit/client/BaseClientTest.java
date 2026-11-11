package ru.practicum.shareit.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class BaseClientTest {
    private BaseClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestTemplate rest = new RestTemplate();
        server = MockRestServiceServer.createServer(rest);
        client = new BaseClient(rest);
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void getExpandsParametersAndSendsJsonHeaders() {
        server.expect(requestTo("http://localhost/items?text=drill"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Sharer-User-Id", "2"))
                .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess("{\"id\":3}", MediaType.APPLICATION_JSON));

        assertEquals(Map.of("id", 3), client.get("http://localhost/items?text={text}", 2L, Map.of("text", "drill")).getBody());
    }

    @Test
    void putSendsBodyAndUserId() {
        server.expect(requestTo("http://localhost/items/3"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andExpect(content().json("{\"name\":\"Drill\"}"))
                .andRespond(withSuccess("{\"id\":3}", MediaType.APPLICATION_JSON));

        assertEquals(Map.of("id", 3), client.put("http://localhost/items/3", 1, Map.of("name", "Drill")).getBody());
    }

    @Test
    void putExpandsPathParameters() {
        server.expect(requestTo("http://localhost/items/3"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andExpect(content().json("{\"available\":false}"))
                .andRespond(withNoContent());

        ResponseEntity<Object> actual = client.put("http://localhost/items/{id}", 1,
                Map.of("id", 3), Map.of("available", false));

        assertEquals(HttpStatus.NO_CONTENT, actual.getStatusCode());
        assertNull(actual.getBody());
    }

    @Test
    void patchCanSendRequestWithoutBody() {
        server.expect(requestTo("http://localhost/bookings/8"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andExpect(content().string(""))
                .andRespond(withSuccess("{\"id\":8}", MediaType.APPLICATION_JSON));

        assertEquals(Map.of("id", 8), client.patch("http://localhost/bookings/8", 1L).getBody());
    }

    @Test
    void deleteSendsUserIdWithoutBody() {
        server.expect(requestTo("http://localhost/items/3"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andExpect(content().string(""))
                .andRespond(withNoContent());

        assertEquals(HttpStatus.NO_CONTENT, client.delete("http://localhost/items/3", 1).getStatusCode());
    }

    @Test
    void deleteExpandsQueryParameters() {
        server.expect(requestTo("http://localhost/items/3?force=true"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andRespond(withNoContent());

        assertEquals(HttpStatus.NO_CONTENT,
                client.delete("http://localhost/items/3?force={force}", 1L, Map.of("force", true)).getStatusCode());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 404, 409, 500, 503})
    void serverErrorPreservesStatusAndResponseBytes(int status) {
        byte[] body = "{\"error\":\"Ошибка сервера\"}".getBytes(StandardCharsets.UTF_8);
        server.expect(requestTo("http://localhost/items/3"))
                .andRespond(withStatus(HttpStatus.valueOf(status)).body(body).contentType(MediaType.APPLICATION_JSON));

        ResponseEntity<Object> actual = client.get("http://localhost/items/3");

        assertEquals(status, actual.getStatusCode().value());
        assertArrayEquals(body, (byte[]) actual.getBody());
    }

    @Test
    void serverErrorWithoutBodyPreservesStatus() {
        server.expect(requestTo("http://localhost/items/3"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        ResponseEntity<Object> actual = client.get("http://localhost/items/3");

        assertEquals(HttpStatus.NOT_FOUND, actual.getStatusCode());
        assertArrayEquals(new byte[0], (byte[]) actual.getBody());
    }

    @Test
    void redirectResponsePreservesStatusAndBody() {
        server.expect(requestTo("http://localhost/items/3"))
                .andRespond(withStatus(HttpStatus.TEMPORARY_REDIRECT)
                        .body("{\"location\":\"/items/4\"}").contentType(MediaType.APPLICATION_JSON));

        ResponseEntity<Object> actual = client.get("http://localhost/items/3");

        assertEquals(HttpStatus.TEMPORARY_REDIRECT, actual.getStatusCode());
        assertEquals(Map.of("location", "/items/4"), actual.getBody());
    }

    @Test
    void redirectResponseWithoutBodyPreservesStatus() {
        server.expect(requestTo("http://localhost/items/3"))
                .andRespond(withStatus(HttpStatus.NOT_MODIFIED));

        ResponseEntity<Object> actual = client.get("http://localhost/items/3");

        assertEquals(HttpStatus.NOT_MODIFIED, actual.getStatusCode());
        assertNull(actual.getBody());
    }
}
