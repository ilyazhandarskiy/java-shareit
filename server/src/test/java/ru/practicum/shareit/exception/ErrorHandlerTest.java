package ru.practicum.shareit.exception;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.BookingController;
import ru.practicum.shareit.booking.service.BookingService;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BookingController.class)
class ErrorHandlerTest {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private BookingService bookingService;

    @ParameterizedTest
    @MethodSource("errors")
    void handleServiceException(RuntimeException exception, int status, String reason) throws Exception {
        when(bookingService.getById(2L, 20L)).thenThrow(exception);

        mockMvc.perform(get("/bookings/20").header(USER_ID_HEADER, 2))
                .andExpect(status().is(status)).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(status)).andExpect(jsonPath("$.error").value(reason))
                .andExpect(jsonPath("$.message").value("Test error"))
                .andExpect(jsonPath("$.path").value("/bookings/20"))
                .andExpect(jsonPath("$.timestamp", not(emptyOrNullString())));
    }

    private static Stream<Arguments> errors() {
        return Stream.of(
                Arguments.of(new NotFoundException("Test error"), 404, "Not Found"),
                Arguments.of(new BookingUnavailableException("Test error"), 400, "Bad Request"),
                Arguments.of(new AuthorizationException("Test error"), 401, "Unauthorized"),
                Arguments.of(new ConflictException("Test error"), 409, "Conflict"),
                Arguments.of(new ForbiddenException("Test error"), 403, "Forbidden"),
                Arguments.of(new IllegalArgumentException("Test error"), 400, "Bad Request"));
    }
}
