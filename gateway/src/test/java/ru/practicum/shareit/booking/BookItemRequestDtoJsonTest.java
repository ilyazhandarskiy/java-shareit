package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.booking.dto.BookItemRequestDto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JsonTest
class BookItemRequestDtoJsonTest {

    private static final String VALID_START = "2030-05-20T10:15:30";
    private static final String VALID_END = "2030-05-21T11:45:00";

    @Autowired
    private JacksonTester<BookItemRequestDto> json;

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    private static String bookingJson(String start, String end) {
        return "{\"itemId\":1,\"start\":\"" + start + "\",\"end\":\"" + end + "\"}";
    }

    private static Set<String> violatedFields(Set<ConstraintViolation<BookItemRequestDto>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void serialize_allFields_usesConfiguredDateFormat() throws Exception {
        BookItemRequestDto dto = new BookItemRequestDto(
                1L,
                LocalDateTime.of(2030, 5, 20, 10, 15, 30),
                LocalDateTime.of(2030, 5, 21, 11, 45, 0));

        JsonContent<BookItemRequestDto> result = json.write(dto);

        assertThat(result).extractingJsonPathNumberValue("$.itemId").isEqualTo(1);
        assertThat(result).extractingJsonPathStringValue("$.start").isEqualTo(VALID_START);
        assertThat(result).extractingJsonPathStringValue("$.end").isEqualTo(VALID_END);
    }

    @Test
    void serialize_midnightDates_keepsZeroSeconds() throws Exception {
        BookItemRequestDto dto = new BookItemRequestDto(
                2L,
                LocalDateTime.of(2030, 1, 1, 0, 0),
                LocalDateTime.of(2030, 1, 2, 0, 0));

        JsonContent<BookItemRequestDto> result = json.write(dto);

        assertThat(result).extractingJsonPathStringValue("$.start").isEqualTo("2030-01-01T00:00:00");
        assertThat(result).extractingJsonPathStringValue("$.end").isEqualTo("2030-01-02T00:00:00");
    }

    @Test
    void deserialize_validJson_createsDto() throws Exception {
        BookItemRequestDto dto = json.parseObject(
                "{\"itemId\":5,\"start\":\"" + VALID_START + "\",\"end\":\"" + VALID_END + "\"}");

        assertThat(dto.getItemId()).isEqualTo(5L);
        assertThat(dto.getStart()).isEqualTo(LocalDateTime
                .of(2030, 5, 20, 10, 15, 30));
        assertThat(dto.getEnd()).isEqualTo(LocalDateTime
                .of(2030, 5, 21, 11, 45, 0));
    }

    @Test
    void deserialize_emptyObject_leavesAllFieldsNull() throws Exception {
        BookItemRequestDto dto = json.parseObject("{}");

        assertThat(dto.getItemId()).isNull();
        assertThat(dto.getStart()).isNull();
        assertThat(dto.getEnd()).isNull();
    }

    @Test
    void roundTrip_allFields_preservesAllFields() throws Exception {
        BookItemRequestDto original = new BookItemRequestDto(
                7L,
                LocalDateTime.of(2031, 3, 4, 5, 6, 7),
                LocalDateTime.of(2031, 3, 5, 6, 7, 8));

        BookItemRequestDto restored = json.parseObject(json.write(original).getJson());

        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource({
            "start,2030-05-20 10:15:30",
            "start,2030.05.20 10:15:30",
            "start,2030-05-20T10:15",
            "start,not-a-date",
            "end,2030-05-21 11:45:00",
            "end,2030.05.21 11:45:00",
            "end,2030-05-21T11:45",
            "end,not-a-date"
    })
    void deserialize_invalidDateFormat_failsOnThatField(String field, String value) {
        String start = field.equals("start") ? value : VALID_START;
        String end = field.equals("end") ? value : VALID_END;

        assertThatThrownBy(() -> json.parseObject(bookingJson(start, end)))
                .isInstanceOfSatisfying(InvalidFormatException.class, exception ->
                        assertThat(exception.getPath())
                                .extracting(JsonMappingException.Reference::getFieldName)
                                .containsExactly(field));
    }

    @Test
    void deserialize_nonexistentDate_isClampedBySmartResolver() throws Exception {
        BookItemRequestDto dto = json.parseObject(bookingJson("2030-02-30T10:00:00", VALID_END));

        assertThat(dto.getStart()).isEqualTo(LocalDateTime
                .of(2030, 2, 28, 10, 0, 0));
    }

    @Test
    void validate_futureDates_hasNoViolations() {
        BookItemRequestDto dto = new BookItemRequestDto(
                1L,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(1));

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void validate_startInPast_reportsStartViolation() {
        BookItemRequestDto dto = new BookItemRequestDto(
                1L,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1));

        assertThat(violatedFields(validator.validate(dto))).containsExactly("start");
    }

    @Test
    void validate_startSlightlyInFuture_hasNoViolations() {
        BookItemRequestDto dto = new BookItemRequestDto(
                1L,
                LocalDateTime.now().plusSeconds(2),
                LocalDateTime.now().plusDays(1));

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void validate_endInPast_reportsEndViolation() {
        BookItemRequestDto dto = new BookItemRequestDto(
                1L,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().minusMinutes(1));

        assertThat(violatedFields(validator.validate(dto))).containsExactly("end");
    }

    @Test
    void validate_nullFields_reportsAllFieldsViolations() {
        BookItemRequestDto dto = new BookItemRequestDto(null, null, null);

        assertThat(violatedFields(validator.validate(dto)))
                .containsExactlyInAnyOrder("itemId", "start", "end");
    }

    @Test
    void validate_deserializedPastDates_reportsStartAndEndViolations() throws Exception {
        BookItemRequestDto dto = json.parseObject(bookingJson("2000-01-01T10:00:00", "2000-01-02T10:00:00"));

        assertThat(violatedFields(validator.validate(dto)))
                .containsExactlyInAnyOrder("start", "end");
    }
}