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
import ru.practicum.shareit.booking.dto.BookingShortDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JsonTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class BookingShortDtoJsonTest {
    private final JacksonTester<BookingShortDto> json;

    @Test
    void serialize_allFields_writesAllPropertiesWithoutNanos() throws Exception {
        BookingShortDto dto = new BookingShortDto(20L, 2L,
                LocalDateTime.of(2030, 1, 15, 12, 0, 0, 123_456_789),
                LocalDateTime.of(2030, 1, 16, 12, 0, 0, 987_654_321));

        JsonContent<BookingShortDto> result = json.write(dto);

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(20);
        assertThat(result).extractingJsonPathNumberValue("$.bookerId").isEqualTo(2);
        assertThat(result).extractingJsonPathStringValue("$.start").isEqualTo("2030-01-15T12:00:00");
        assertThat(result).extractingJsonPathStringValue("$.end").isEqualTo("2030-01-16T12:00:00");
    }

    @Test
    void serialize_midnightDates_keepsZeroSeconds() throws Exception {
        BookingShortDto dto = new BookingShortDto(20L, 2L,
                LocalDateTime.of(2030, 1, 15, 0, 0),
                LocalDateTime.of(2030, 1, 16, 0, 0));

        JsonContent<BookingShortDto> result = json.write(dto);

        assertThat(result).extractingJsonPathStringValue("$.start").isEqualTo("2030-01-15T00:00:00");
        assertThat(result).extractingJsonPathStringValue("$.end").isEqualTo("2030-01-16T00:00:00");
    }

    @Test
    void deserialize_allFields_createsDto() throws Exception {
        String content = "{\"id\":20,\"bookerId\":2,\"start\":\"2030-01-15T12:00:00\",\"end\":\"2030-01-16T12:00:00\"}";

        BookingShortDto result = json.parseObject(content);

        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getBookerId()).isEqualTo(2L);
        assertThat(result.getStart()).isEqualTo(LocalDateTime.of(2030, 1, 15, 12, 0));
        assertThat(result.getEnd()).isEqualTo(LocalDateTime.of(2030, 1, 16, 12, 0));
    }

    @Test
    void deserialize_emptyObject_leavesAllFieldsNull() throws Exception {
        BookingShortDto result = json.parseObject("{}");

        assertThat(result.getId()).isNull();
        assertThat(result.getBookerId()).isNull();
        assertThat(result.getStart()).isNull();
        assertThat(result.getEnd()).isNull();
    }

    @Test
    void roundTrip_allFields_preservesAllFields() throws Exception {
        BookingShortDto original = new BookingShortDto(20L, 2L,
                LocalDateTime.of(2030, 1, 15, 12, 0, 0),
                LocalDateTime.of(2030, 1, 16, 12, 0, 0));

        BookingShortDto restored = json.parseObject(json.write(original).getJson());

        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
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
        String content = "{\"id\":20,\"bookerId\":2,\"start\":\"" + start + "\",\"end\":\"" + end + "\"}";

        assertThatThrownBy(() -> json.parseObject(content))
                .isInstanceOfSatisfying(InvalidFormatException.class, exception ->
                        assertThat(exception.getPath())
                                .extracting(JsonMappingException.Reference::getFieldName)
                                .containsExactly(field));
    }
}