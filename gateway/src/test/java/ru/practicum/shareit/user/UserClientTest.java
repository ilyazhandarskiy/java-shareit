package ru.practicum.shareit.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import ru.practicum.shareit.user.dto.CreateUserRequestDto;
import ru.practicum.shareit.user.dto.PatchUserRequestDto;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class UserClientTest {
    private UserClient userClient;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestTemplateBuilder builder = new RestTemplateBuilder()
                .additionalCustomizers(rest -> server = MockRestServiceServer.createServer(rest));
        userClient = new UserClient("http://localhost:9090", builder);
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void createSendsUserWithoutSharerHeader() {
        CreateUserRequestDto dto = CreateUserRequestDto.builder().name("Ann").email("ann@example.com").build();
        server.expect(requestTo("http://localhost:9090/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(headerDoesNotExist("X-Sharer-User-Id"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"name\":\"Ann\",\"email\":\"ann@example.com\"}"))
                .andRespond(withStatus(HttpStatus.CREATED).body("{\"id\":1}").contentType(MediaType.APPLICATION_JSON));

        ResponseEntity<Object> actual = userClient.create(dto);

        assertEquals(HttpStatus.CREATED, actual.getStatusCode());
        assertEquals(Map.of("id", 1), actual.getBody());
    }

    @Test
    void updateSendsPatchToUserPath() {
        PatchUserRequestDto dto = PatchUserRequestDto.builder().email("updated@example.com").build();
        server.expect(requestTo("http://localhost:9090/users/1"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(headerDoesNotExist("X-Sharer-User-Id"))
                .andExpect(content().json("{\"email\":\"updated@example.com\"}"))
                .andRespond(withSuccess("{\"id\":1,\"email\":\"updated@example.com\"}", MediaType.APPLICATION_JSON));

        assertEquals(Map.of("id", 1, "email", "updated@example.com"), userClient.update(1, dto).getBody());
    }

    @Test
    void getByIdSendsGetWithoutSharerHeader() {
        server.expect(requestTo("http://localhost:9090/users/1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(headerDoesNotExist("X-Sharer-User-Id"))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));

        assertEquals(Map.of("id", 1), userClient.getById(1).getBody());
    }

    @Test
    void getAllReadsResponseList() {
        server.expect(requestTo("http://localhost:9090/users"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(headerDoesNotExist("X-Sharer-User-Id"))
                .andRespond(withSuccess("[{\"id\":1}]", MediaType.APPLICATION_JSON));

        assertEquals(List.of(Map.of("id", 1)), userClient.getAll().getBody());
    }

    @Test
    void deleteByIdPreservesNoContentStatus() {
        server.expect(requestTo("http://localhost:9090/users/1"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(headerDoesNotExist("X-Sharer-User-Id"))
                .andRespond(withNoContent());

        ResponseEntity<Object> actual = userClient.deleteById(1);

        assertEquals(HttpStatus.NO_CONTENT, actual.getStatusCode());
        assertNull(actual.getBody());
    }
}
