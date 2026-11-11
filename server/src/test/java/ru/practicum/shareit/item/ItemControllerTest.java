package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.CreateCommentDto;
import ru.practicum.shareit.item.dto.CreateItemRequestDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.PatchItemRequestDto;
import ru.practicum.shareit.item.service.ItemService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ItemController.class)
class ItemControllerTest {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";
    private static final LocalDateTime TIME = LocalDateTime.of(2030, 1, 15, 12, 0);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private ItemService itemService;

    @Test
    void createItemWhenRequestIsValid() throws Exception {
        CreateItemRequestDto input = new CreateItemRequestDto("Drill", "Cordless", true, 30L);
        ItemDto output = itemDto();
        output.setRequestId(30L);
        when(itemService.create(1L, input)).thenReturn(output);

        mockMvc.perform(post("/items").header(USER_ID_HEADER, 1).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.available").value(true)).andExpect(jsonPath("$.requestId").value(30));
        verify(itemService).create(1L, input);
    }

    @Test
    void patchItemBindsPartialBody() throws Exception {
        PatchItemRequestDto input = PatchItemRequestDto.builder().available(false).build();
        ItemDto output = itemDto();
        output.setAvailable(false);
        when(itemService.patch(1L, 10L, input)).thenReturn(output);

        mockMvc.perform(patch("/items/10").header(USER_ID_HEADER, 1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(false));
        verify(itemService).patch(1L, 10L, input);
    }

    @Test
    void getItemReturnsComments() throws Exception {
        ItemDto output = itemDto();
        output.setComments(List.of(CommentDto.builder().id(40L).text("Great").authorName("Booker").created(TIME).build()));
        when(itemService.getById(2L, 10L)).thenReturn(output);

        mockMvc.perform(get("/items/10").header(USER_ID_HEADER, 2)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Drill"))
                .andExpect(jsonPath("$.comments[0].authorName").value("Booker"));
        verify(itemService).getById(2L, 10L);
    }

    @Test
    void listOwnerItemsReturnsArray() throws Exception {
        when(itemService.getAllByOwner(1L)).thenReturn(List.of(itemDto()));

        mockMvc.perform(get("/items").header(USER_ID_HEADER, 1)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
        verify(itemService).getAllByOwner(1L);
    }

    @Test
    void searchItemsBindsTextAndRoutesToSearch() throws Exception {
        when(itemService.searchAvailableByText(2L, "cordless drill")).thenReturn(List.of(itemDto()));

        mockMvc.perform(get("/items/search").header(USER_ID_HEADER, 2).param("text", "cordless drill"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(10));
        verify(itemService).searchAvailableByText(2L, "cordless drill");
    }

    @Test
    void addCommentReturnsAuthorAndFormattedTime() throws Exception {
        CreateCommentDto input = new CreateCommentDto("Great");
        when(itemService.addComment(2L, 10L, input)).thenReturn(
                CommentDto.builder().id(40L).text("Great").authorName("Booker").created(TIME).build());

        mockMvc.perform(post("/items/10/comment").header(USER_ID_HEADER, 2).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(40))
                .andExpect(jsonPath("$.text").value("Great"))
                .andExpect(jsonPath("$.authorName").value("Booker"))
                .andExpect(jsonPath("$.created").value("2030-01-15T12:00:00"));
        verify(itemService).addComment(2L, 10L, input);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/items", "/items/10", "/items/search?text=drill"})
    void missingUserHeaderReturnsBadRequest(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isBadRequest());
        verifyNoInteractions(itemService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/items", "/items/10/comment"})
    void createWhenJsonIsInvalid(String path) throws Exception {
        String wrongJson = "{\n";
        mockMvc.perform(post(path).header(USER_ID_HEADER, 2).contentType(MediaType.APPLICATION_JSON).content(wrongJson))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(itemService);
    }

    @Test
    void searchRequiresText() throws Exception {
        mockMvc.perform(get("/items/search").header(USER_ID_HEADER, 1)).andExpect(status().isBadRequest());
        verifyNoInteractions(itemService);
    }

    @Test
    void nonNumericUserHeaderReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/items").header(USER_ID_HEADER, "invalid")).andExpect(status().isBadRequest());
        verifyNoInteractions(itemService);
    }

    private ItemDto itemDto() {
        return ItemDto.builder().id(10L).name("Drill").description("Cordless").available(true).build();
    }
}
