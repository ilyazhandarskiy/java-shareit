package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MethodArgumentNotValidException;
import ru.practicum.shareit.booking.dto.BookItemRequestDto;
import ru.practicum.shareit.booking.dto.BookingState;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {
    private static final String USER_HEADER = "X-Sharer-User-Id";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingClient bookingClient;

    @Test
    void validBookingRoutesCallClient() throws Exception {
        when(bookingClient.bookItem(eq(1L), any())).thenReturn(ResponseEntity.ok(Map.of("id", 8)));
        when(bookingClient.approve(1L, 8L, true)).thenReturn(ResponseEntity.ok(Map.of("id", 8)));
        when(bookingClient.getBooking(1L, 8L)).thenReturn(ResponseEntity.ok(Map.of("id", 8)));
        when(bookingClient.getBookings(1L, BookingState.ALL, 0, 10)).thenReturn(ResponseEntity.ok(List.of()));
        when(bookingClient.getOwnerBookings(1L, BookingState.ALL, 0, 10)).thenReturn(ResponseEntity.ok(List.of()));

        String start = LocalDateTime.now().plusDays(1).format(FORMATTER);
        String end = LocalDateTime.now().plusDays(2).format(FORMATTER);
        mockMvc.perform(post("/bookings")
                        .header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":4,\"start\":\"" + start + "\",\"end\":\"" + end + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8));
        mockMvc.perform(patch("/bookings/8").header(USER_HEADER, 1).param("approved", "true"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/bookings/8").header(USER_HEADER, 1))
                .andExpect(status().isOk());
        mockMvc.perform(get("/bookings").header(USER_HEADER, 1).param("state", "all"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/bookings/owner").header(USER_HEADER, 1))
                .andExpect(status().isOk());

        verify(bookingClient).bookItem(eq(1L), any(BookItemRequestDto.class));
        verify(bookingClient).approve(1L, 8L, true);
        verify(bookingClient).getBooking(1L, 8L);
        verify(bookingClient).getBookings(1L, BookingState.ALL, 0, 10);
        verify(bookingClient).getOwnerBookings(1L, BookingState.ALL, 0, 10);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void createWhenEndNotAfterStartDoesNotCallClient(int endOffsetDays) throws Exception {
        LocalDateTime startDate = LocalDateTime.now().plusDays(2).withNano(0);
        String start = startDate.format(FORMATTER);
        String end = startDate.plusDays(endOffsetDays).format(FORMATTER);
        mockMvc.perform(post("/bookings")
                        .header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":4,\"start\":\"" + start + "\",\"end\":\"" + end + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(IllegalArgumentException.class, result.getResolvedException()))
                .andExpect(jsonPath("$.error").value("Дата окончания должна быть позже даты начала"));
        verifyNoInteractions(bookingClient);
    }

    @Test
    void getWhenStateUnknownDoesNotCallClient() throws Exception {
        mockMvc.perform(get("/bookings").header(USER_HEADER, 1).param("state", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown state: unknown"));
        verifyNoInteractions(bookingClient);
    }

    @Test
    void getWhenFromNegativeDoesNotCallClient() throws Exception {
        mockMvc.perform(get("/bookings").header(USER_HEADER, 1).param("from", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verifyNoInteractions(bookingClient);
    }

    @Test
    void createWithoutUserHeaderDoesNotCallClient() throws Exception {
        String start = LocalDateTime.now().plusDays(1).format(FORMATTER);
        String end = LocalDateTime.now().plusDays(2).format(FORMATTER);

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":4,\"start\":\"" + start + "\",\"end\":\"" + end + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(bookingClient);
    }

    @Test
    void createWithoutBodyDoesNotCallClient() throws Exception {
        mockMvc.perform(post("/bookings")
                        .header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verifyNoInteractions(bookingClient);
    }

    @Test
    void createWithoutStartDoesNotCallClient() throws Exception {
        String end = LocalDateTime.now().plusDays(2).format(FORMATTER);

        mockMvc.perform(post("/bookings")
                        .header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":4,\"end\":\"" + end + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException()))
                .andExpect(jsonPath("$.error", containsString("start")));
        verifyNoInteractions(bookingClient);
    }
}
