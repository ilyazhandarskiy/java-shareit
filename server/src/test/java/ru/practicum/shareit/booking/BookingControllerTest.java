package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.params.provider.CsvSource;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BookingController.class)
class BookingControllerTest {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";
    private static final LocalDateTime TIME = LocalDateTime.of(2030, 1, 15, 12, 0);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private BookingService bookingService;

    @Test
    void createBookingWhenRequestIsValid() throws Exception {
        BookingRequestDto input = new BookingRequestDto(10L, TIME, TIME.plusDays(1));
        BookingDto output = bookingDto();
        when(bookingService.create(2L, input)).thenReturn(output);

        mockMvc.perform(post("/bookings").header(USER_ID_HEADER, 2).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.start").value("2030-01-15T12:00:00"))
                .andExpect(jsonPath("$.end").value("2030-01-16T12:00:00"))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.item.id").value(10))
                .andExpect(jsonPath("$.booker.id").value(2));
        verify(bookingService).create(2L, input);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void approveBooking(boolean approved) throws Exception {
        BookingDto output = bookingDto();
        output.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        when(bookingService.approve(1L, 20L, approved)).thenReturn(output);

        mockMvc.perform(patch("/bookings/20").header(USER_ID_HEADER, 1).param("approved", String.valueOf(approved)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(output.getStatus().name()));
        verify(bookingService).approve(1L, 20L, approved);
    }

    @Test
    void getBookingReturnsBody() throws Exception {
        when(bookingService.getById(2L, 20L)).thenReturn(bookingDto());

        mockMvc.perform(get("/bookings/20").header(USER_ID_HEADER, 2))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(20));
        verify(bookingService).getById(2L, 20L);
    }

    @ParameterizedTest
    @CsvSource({"/bookings,ALL", "/bookings,FUTURE", "/bookings/owner,ALL", "/bookings/owner,PAST"})
    void getBookingsWithState(String path, String state) throws Exception {
        boolean owner = path.endsWith("owner");
        if (owner) {
            when(bookingService.getByOwner(1L, state)).thenReturn(List.of(bookingDto()));
        } else {
            when(bookingService.getByBooker(1L, state)).thenReturn(List.of(bookingDto()));
        }
        var request = get(path).header(USER_ID_HEADER, 1);
        if (!state.equals("ALL")) {
            request.param("state", state);
        }

        mockMvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(20));
        if (owner) {
            verify(bookingService).getByOwner(1L, state);
        } else {
            verify(bookingService).getByBooker(1L, state);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/bookings", "/bookings/20", "/bookings/owner"})
    void missingUserHeaderReturnsBadRequest(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isBadRequest());
        verifyNoInteractions(bookingService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/bookings"})
    void createWhenJsonIsInvalid(String path) throws Exception {
        String wrongJson = "{\n";
        mockMvc.perform(post(path).header(USER_ID_HEADER, 2).contentType(MediaType.APPLICATION_JSON).content(wrongJson))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(bookingService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/bookings/20", "/bookings/20?approved=wrong"})
    void approvalRequiresValidBoolean(String path) throws Exception {
        mockMvc.perform(patch(path).header(USER_ID_HEADER, 1)).andExpect(status().isBadRequest());
        verifyNoInteractions(bookingService);
    }

    private ItemDto itemDto() {
        return ItemDto.builder().id(10L).name("Drill").description("Cordless").available(true).build();
    }

    private BookingDto bookingDto() {
        return BookingDto.builder().id(20L).start(TIME).end(TIME.plusDays(1)).status(BookingStatus.WAITING)
                .item(itemDto()).booker(UserDto.builder().id(2L).name("Booker").build()).build();
    }
}
