package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.item.dto.CreateCommentDto;
import ru.practicum.shareit.item.dto.CreateItemRequestDto;
import ru.practicum.shareit.item.dto.PatchItemRequestDto;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
class ItemControllerTest {
    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemClient itemClient;

    @Test
    void createWithoutRequestIdCallsClient() throws Exception {
        when(itemClient.create(eq(1L), any())).thenReturn(ResponseEntity.ok(Map.of("id", 3, "name", "Drill")));

        mockMvc.perform(post("/items")
                        .header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drill\",\"description\":\"Power\",\"available\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Drill"));
        verify(itemClient).create(eq(1L), any(CreateItemRequestDto.class));
    }

    @Test
    void createWithRequestIdCallsClient() throws Exception {
        when(itemClient.create(eq(1L), any())).thenReturn(ResponseEntity.ok(Map.of("id", 4, "requestId", 9)));

        mockMvc.perform(post("/items")
                        .header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drill\",\"description\":\"Power\",\"available\":true,\"requestId\":9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(9));
        verify(itemClient).create(eq(1L), any(CreateItemRequestDto.class));
    }

    @Test
    void createWithoutNameDoesNotCallClient() throws Exception {
        mockMvc.perform(post("/items")
                        .header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Power\",\"available\":true,\"requestId\":9}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verifyNoInteractions(itemClient);
    }

    @Test
    void createWithoutDescriptionOrAvailableDoesNotCallClient() throws Exception {
        mockMvc.perform(post("/items")
                        .header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drill\",\"available\":true,\"requestId\":9}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        mockMvc.perform(post("/items")
                        .header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drill\",\"description\":\"Power\",\"requestId\":9}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verifyNoInteractions(itemClient);
    }

    @Test
    void updateGetSearchAndComment() throws Exception {
        when(itemClient.update(eq(1L), eq(3L), any())).thenReturn(ResponseEntity.ok(Map.of("id", 3)));
        when(itemClient.getById(1L, 3L)).thenReturn(ResponseEntity.ok(Map.of("id", 3)));
        when(itemClient.getAll(1L)).thenReturn(ResponseEntity.ok(List.of()));
        when(itemClient.search(1L, "drill")).thenReturn(ResponseEntity.ok(List.of()));
        when(itemClient.addComment(eq(1L), eq(3L), any())).thenReturn(ResponseEntity.ok(Map.of("text", "Nice")));

        mockMvc.perform(patch("/items/3")
                        .header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drill 2\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/items/3").header(USER_HEADER, 1))
                .andExpect(status().isOk());
        mockMvc.perform(get("/items").header(USER_HEADER, 1))
                .andExpect(status().isOk());
        mockMvc.perform(get("/items/search").header(USER_HEADER, 1).param("text", "drill"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/items/3/comment")
                        .header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Nice\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Nice"));

        verify(itemClient).update(eq(1L), eq(3L), any(PatchItemRequestDto.class));
        verify(itemClient).getById(1L, 3L);
        verify(itemClient).getAll(1L);
        verify(itemClient).search(1L, "drill");
        verify(itemClient).addComment(eq(1L), eq(3L), any(CreateCommentDto.class));
    }

    @Test
    void blankCommentDoesNotCallClient() throws Exception {
        mockMvc.perform(post("/items/3/comment")
                        .header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verifyNoInteractions(itemClient);
    }
}
