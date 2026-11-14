package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.request.dto.CreateItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDtoFull;
import ru.practicum.shareit.request.dto.ItemRequestDtoShort;
import ru.practicum.shareit.request.service.ItemRequestService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ItemRequestController.class)
class ItemRequestControllerTest {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";
    private static final LocalDateTime TIME = LocalDateTime.of(2030, 1, 15, 12, 0);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private ItemRequestService requestService;

    @Test
    void createRequestBindsDescriptionAndUser() throws Exception {
        CreateItemRequestDto input = new CreateItemRequestDto("Need a drill");
        when(requestService.create(input, 2L)).thenReturn(shortRequest());

        mockMvc.perform(post("/requests").header(USER_ID_HEADER, 2).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(30))
                .andExpect(jsonPath("$.description").value("Need a drill"))
                .andExpect(jsonPath("$.requestorId").value(2))
                .andExpect(jsonPath("$.created").value("2030-01-15T12:00:00"));
        verify(requestService).create(input, 2L);
    }

    @Test
    void getOwnRequestsReturnsFullDtos() throws Exception {
        when(requestService.getUserItems(2L)).thenReturn(List.of(fullRequest()));

        mockMvc.perform(get("/requests").header(USER_ID_HEADER, 2)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(30)).andExpect(jsonPath("$[0].items").isArray());
        verify(requestService).getUserItems(2L);
    }

    @Test
    void getOtherRequestsUsesAllRoute() throws Exception {
        when(requestService.getOtherUsersItems(1L)).thenReturn(List.of(shortRequest()));

        mockMvc.perform(get("/requests/all").header(USER_ID_HEADER, 1)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestorId").value(2));
        verify(requestService).getOtherUsersItems(1L);
    }

    @Test
    void getRequestById() throws Exception {
        when(requestService.findById(30L, 1L)).thenReturn(fullRequest());

        mockMvc.perform(get("/requests/30").header(USER_ID_HEADER, 1)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(30)).andExpect(jsonPath("$.items").isEmpty());
        verify(requestService).findById(30L, 1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/requests", "/requests/all", "/requests/30"})
    void missingUserHeaderReturnsBadRequest(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isBadRequest());
        verifyNoInteractions(requestService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/requests"})
    void createWhenJsonIsInvalid(String path) throws Exception {
        String wrongJson = "{\n";
        mockMvc.perform(post(path).header(USER_ID_HEADER, 2).contentType(MediaType.APPLICATION_JSON).content(wrongJson))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(requestService);
    }

    private ItemRequestDtoShort shortRequest() {
        return ItemRequestDtoShort.builder().id(30L).description("Need a drill").requestorId(2L).created(TIME).build();
    }

    private ItemRequestDtoFull fullRequest() {
        return ItemRequestDtoFull.builder().id(30L).description("Need a drill").requestorId(2L).created(TIME).build();
    }
}
