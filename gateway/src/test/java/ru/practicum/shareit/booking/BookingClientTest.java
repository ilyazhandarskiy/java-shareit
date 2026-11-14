package ru.practicum.shareit.booking;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import ru.practicum.shareit.booking.dto.BookItemRequestDto;
import ru.practicum.shareit.booking.dto.BookingState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BookingClientTest {
    private BookingClient bookingClient;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestTemplateBuilder builder = new RestTemplateBuilder()
                .additionalCustomizers(rest -> server = MockRestServiceServer.createServer(rest));
        bookingClient = new BookingClient("http://localhost:9090", builder);
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void bookItemSendsDatesAndUserId() {
        LocalDateTime start = LocalDateTime.of(2030, 1, 15, 12, 0);
        BookItemRequestDto dto = new BookItemRequestDto(4L, start, start.plusDays(1));
        server.expect(requestTo("http://localhost:9090/bookings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Sharer-User-Id", "2"))
                .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"itemId\":4,\"start\":\"2030-01-15T12:00:00\",\"end\":\"2030-01-16T12:00:00\"}"))
                .andRespond(withSuccess("{\"id\":8,\"status\":\"WAITING\"}", MediaType.APPLICATION_JSON));

        ResponseEntity<Object> actual = bookingClient.bookItem(2, dto);

        assertEquals(200, actual.getStatusCode().value());
        assertEquals(Map.of("id", 8, "status", "WAITING"), actual.getBody());
    }

    @Test
    void getBookingUsesBookingIdInPath() {
        server.expect(requestTo("http://localhost:9090/bookings/8"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Sharer-User-Id", "2"))
                .andRespond(withSuccess("{\"id\":8}", MediaType.APPLICATION_JSON));

        assertEquals(Map.of("id", 8), bookingClient.getBooking(2, 8L).getBody());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void approveSendsBooleanInQuery(boolean approved) {
        server.expect(requestTo("http://localhost:9090/bookings/8?approved=" + approved))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andExpect(content().string(""))
                .andRespond(withSuccess("{\"id\":8}", MediaType.APPLICATION_JSON));

        assertEquals(Map.of("id", 8), bookingClient.approve(1, 8L, approved).getBody());
    }

    @Test
    void getBookingsSendsStateAndPagination() {
        server.expect(requestTo("http://localhost:9090/bookings?state=FUTURE&from=20&size=5"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Sharer-User-Id", "2"))
                .andRespond(withSuccess("[{\"id\":8}]", MediaType.APPLICATION_JSON));

        assertEquals(List.of(Map.of("id", 8)), bookingClient.getBookings(2, BookingState.FUTURE, 20, 5).getBody());
    }

    @Test
    void getOwnerBookingsUsesOwnerPathAndPagination() {
        server.expect(requestTo("http://localhost:9090/bookings/owner?state=WAITING&from=10&size=3"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertEquals(List.of(), bookingClient.getOwnerBookings(1, BookingState.WAITING, 10, 3).getBody());
    }
}
