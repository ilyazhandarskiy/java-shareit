package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JsonTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class BookingDtoJsonTest {
    private final JacksonTester<BookingDto> json;

    @Test
    void serialize_allFields_writesAllPropertiesWithoutNanos() throws Exception {
        UserDto booker = new UserDto(2L, "Booker", "booker@example.com");
        ItemDto item = ItemDto.builder().id(10L).name("Drill").available(true).build();
        LocalDateTime start = LocalDateTime.of(2030, 1, 15, 12, 0, 0, 123_456_789);
        LocalDateTime end = LocalDateTime.of(2030, 1, 16, 12, 0, 0, 987_654_321);
        BookingDto dto = new BookingDto(20L, start, end, BookingStatus.APPROVED, booker, item);

        JsonContent<BookingDto> result = json.write(dto);

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(20);
        assertThat(result).extractingJsonPathStringValue("$.start").isEqualTo("2030-01-15T12:00:00");
        assertThat(result).extractingJsonPathStringValue("$.end").isEqualTo("2030-01-16T12:00:00");
        assertThat(result).extractingJsonPathStringValue("$.status").isEqualTo("APPROVED");
        assertThat(result).extractingJsonPathNumberValue("$.booker.id").isEqualTo(2);
        assertThat(result).extractingJsonPathStringValue("$.booker.email").isEqualTo("booker@example.com");
        assertThat(result).extractingJsonPathNumberValue("$.item.id").isEqualTo(10);
        assertThat(result).extractingJsonPathStringValue("$.item.name").isEqualTo("Drill");
    }

    @Test
    void serialize_midnightDates_keepsZeroSeconds() throws Exception {
        BookingDto dto = new BookingDto(1L, LocalDateTime.of(2030, 1, 15, 0, 0),
                LocalDateTime.of(2030, 1, 16, 0, 0),
                BookingStatus.WAITING, null, null);

        JsonContent<BookingDto> result = json.write(dto);

        assertThat(result).extractingJsonPathStringValue("$.start").isEqualTo("2030-01-15T00:00:00");
        assertThat(result).extractingJsonPathStringValue("$.end").isEqualTo("2030-01-16T00:00:00");
    }

    @Test
    void deserialize_allFields_createsDtoWithNestedObjects() throws Exception {
        String content = "{\"id\":20,\"start\":\"2030-01-15T12:00:00\",\"end\":\"2030-01-16T12:00:00\","
                + "\"status\":\"APPROVED\",\"booker\":{\"id\":2,\"name\":\"Booker\",\"email\":\"booker@example.com\"},"
                + "\"item\":{\"id\":10,\"name\":\"Drill\",\"available\":true}}";

        BookingDto result = json.parseObject(content);

        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getStart()).isEqualTo(LocalDateTime.of(2030, 1, 15, 12, 0));
        assertThat(result.getEnd()).isEqualTo(LocalDateTime.of(2030, 1, 16, 12, 0));
        assertThat(result.getStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThat(result.getBooker()).isEqualTo(new UserDto(2L, "Booker", "booker@example.com"));
        assertThat(result.getItem().getId()).isEqualTo(10L);
        assertThat(result.getItem().getName()).isEqualTo("Drill");
        assertThat(result.getItem().getAvailable()).isTrue();
    }

    @Test
    void deserialize_withoutNestedObjects_leavesThemNull() throws Exception {
        String content = "{\"id\":20,\"start\":\"2030-01-15T12:00:00\",\"end\":\"2030-01-16T12:00:00\","
                + "\"status\":\"WAITING\"}";

        BookingDto result = json.parseObject(content);

        assertThat(result.getBooker()).isNull();
        assertThat(result.getItem()).isNull();
    }

    @Test
    void deserialize_unknownStatus_failsOnStatusField() {
        String content = "{\"id\":20,\"status\":\"UNKNOWN\"}";

        assertThatThrownBy(() -> json.parseObject(content))
                .isInstanceOfSatisfying(InvalidFormatException.class, exception ->
                        assertThat(exception.getPath())
                                .extracting(JsonMappingException.Reference::getFieldName)
                                .containsExactly("status"));
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource({
            "start,15.01.2030 12:00:00",
            "start,2030-01-15",
            "start,2030-01-15T12:00",
            "end,2030-01-16 12:00:00",
            "end,2030-01-16T12:00",
            "end,not-a-date"
    })
    void deserialize_invalidDateFormat_failsOnThatField(String field, String value) {
        String start = field.equals("start") ? value : "2030-01-15T12:00:00";
        String end = field.equals("end") ? value : "2030-01-16T12:00:00";
        String content = "{\"id\":20,\"start\":\"" + start + "\",\"end\":\"" + end + "\"}";

        assertThatThrownBy(() -> json.parseObject(content))
                .isInstanceOfSatisfying(InvalidFormatException.class, exception ->
                        assertThat(exception.getPath())
                                .extracting(JsonMappingException.Reference::getFieldName)
                                .containsExactly(field));
    }
}