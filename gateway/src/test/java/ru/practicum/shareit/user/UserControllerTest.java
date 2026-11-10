package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.user.dto.CreateUserRequestDto;
import ru.practicum.shareit.user.dto.PatchUserRequestDto;


import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserClient userClient;

    @Test
    void validUserIsProxied() throws Exception {
        when(userClient.create(any())).thenReturn(ResponseEntity.ok(Map.of("id", 1, "name", "Ann")));
        when(userClient.update(eq(1L), any())).thenReturn(ResponseEntity.ok(Map.of("id", 1)));
        when(userClient.getById(1L)).thenReturn(ResponseEntity.ok(Map.of("id", 1)));
        when(userClient.getAll()).thenReturn(ResponseEntity.ok(List.of()));
        when(userClient.deleteById(1L)).thenReturn(ResponseEntity.ok().build());

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ann\",\"email\":\"ann@mail.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Anna\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/users/1")).andExpect(status().isOk());
        mockMvc.perform(get("/users")).andExpect(status().isOk());
        mockMvc.perform(delete("/users/1")).andExpect(status().isOk());

        verify(userClient).create(any(CreateUserRequestDto.class));
        verify(userClient).update(eq(1L), any(PatchUserRequestDto.class));
        verify(userClient).getById(1L);
        verify(userClient).getAll();
        verify(userClient).deleteById(1L);
    }

    @Test
    void invalidUserDoesNotCallClient() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        mockMvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verifyNoInteractions(userClient);
    }
}
