package ru.practicum.shareit.request;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemRequestController.class)
class ItemRequestControllerTest {
    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemRequestClient itemRequestClient;

    @Test
    void createProxiesRequest() throws Exception {
        when(itemRequestClient.create(eq(1L), any()))
                .thenReturn(ResponseEntity.ok(Map.of("id", 5, "description", "need drill")));

        mockMvc.perform(post("/requests")
                        .header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"need drill\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.description").value("need drill"));
        verify(itemRequestClient).create(eq(1L), any());
    }

    @Test
    void blankDescriptionDoesNotCallClient() throws Exception {
        mockMvc.perform(post("/requests")
                        .header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verifyNoInteractions(itemRequestClient);
    }

    @Test
    void getOwnProxiesList() throws Exception {
        when(itemRequestClient.getOwn(1L)).thenReturn(ResponseEntity.ok(java.util.List.of()));

        mockMvc.perform(get("/requests").header(USER_HEADER, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        verify(itemRequestClient).getOwn(1L);
    }

    @Test
    void getAllDoesNotUsePagination() throws Exception {
        when(itemRequestClient.getAll(1L)).thenReturn(ResponseEntity.ok(java.util.List.of()));

        mockMvc.perform(get("/requests/all").header(USER_HEADER, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        verify(itemRequestClient).getAll(1L);
    }

    @Test
    void getByIdProxiesRequest() throws Exception {
        when(itemRequestClient.getById(2L, 5L))
                .thenReturn(ResponseEntity.ok(Map.of("id", 5, "description", "need drill")));

        mockMvc.perform(get("/requests/5").header(USER_HEADER, 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
        verify(itemRequestClient).getById(2L, 5L);
    }
}
