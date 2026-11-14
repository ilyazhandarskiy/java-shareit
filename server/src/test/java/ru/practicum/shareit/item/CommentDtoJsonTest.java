package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.item.dto.CommentDto;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@JsonTest
class CommentDtoJsonTest {

    @Autowired
    private JacksonTester<CommentDto> json;

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    static Stream<Arguments> blankValues() {
        return Stream.of(
                arguments(null, "null"),
                arguments("", "empty string"),
                arguments(" ", "single space"),
                arguments("\t\n", "tab and newline")
        );
    }

    @Test
    void serialize_allFields_writesAllPropertiesWithoutNanos() throws Exception {
        CommentDto dto = new CommentDto(40L, "Works well", "Booker",
                LocalDateTime.of(2030, 1, 15, 12, 0, 0, 123_456_789));

        JsonContent<CommentDto> result = json.write(dto);

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(40);
        assertThat(result).extractingJsonPathStringValue("$.text").isEqualTo("Works well");
        assertThat(result).extractingJsonPathStringValue("$.authorName").isEqualTo("Booker");
        assertThat(result).extractingJsonPathStringValue("$.created").isEqualTo("2030-01-15T12:00:00");
    }

    @Test
    void serialize_midnightCreated_keepsZeroSeconds() throws Exception {
        LocalDateTime dateTime = LocalDateTime.of(2030, 1, 15, 0, 0);
        CommentDto dto = new CommentDto(40L, "Works well", "Booker", dateTime);

        JsonContent<CommentDto> result = json.write(dto);

        assertThat(result).extractingJsonPathStringValue("$.created").isEqualTo("2030-01-15T00:00:00");
    }

    @Test
    void deserialize_allFields_createsDto() throws Exception {
        String content = "{\"id\":40,\"text\":\"Works well\",\"authorName\":\"Booker\",\"created\":\"2030-01-15T12:00:00\"}";

        CommentDto result = json.parseObject(content);

        assertThat(result.getId()).isEqualTo(40L);
        assertThat(result.getText()).isEqualTo("Works well");
        assertThat(result.getAuthorName()).isEqualTo("Booker");
        assertThat(result.getCreated()).isEqualTo(LocalDateTime.of(2030, 1, 15, 12, 0));
    }

    @Test
    void deserialize_emptyObject_leavesAllFieldsNull() throws Exception {
        CommentDto result = json.parseObject("{}");

        assertThat(result.getId()).isNull();
        assertThat(result.getText()).isNull();
        assertThat(result.getAuthorName()).isNull();
        assertThat(result.getCreated()).isNull();
    }

    @Test
    void roundTrip_allFields_preservesAllFields() throws Exception {
        LocalDateTime dateTime = LocalDateTime.of(2030, 1, 15, 12, 0, 0);
        CommentDto original = new CommentDto(40L, "Works well", "Booker", dateTime);

        CommentDto restored = json.parseObject(json.write(original).getJson());

        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
    }

    @ParameterizedTest(name = "created = {0}")
    @ValueSource(strings = {
            "15.01.2030 12:00:00",
            "2030-01-15 12:00:00",
            "2030-01-15",
            "2030-01-15T12:00",
            "not-a-date"
    })
    void deserialize_invalidDateFormat_failsOnCreatedField(String created) {
        String content = "{\"id\":40,\"text\":\"Works well\",\"created\":\"" + created + "\"}";

        assertThatThrownBy(() -> json.parseObject(content))
                .isInstanceOfSatisfying(InvalidFormatException.class, exception ->
                        assertThat(exception.getPath())
                                .extracting(JsonMappingException.Reference::getFieldName)
                                .containsExactly("created"));
    }

    @Test
    void validate_textPresent_hasNoViolations() {
        LocalDateTime dateTime = LocalDateTime.of(2030, 1, 15, 12, 0);
        CommentDto dto = new CommentDto(40L, "Works well", "Booker", dateTime);

        assertThat(validator.validate(dto)).isEmpty();
    }

    @ParameterizedTest(name = "text = {1}")
    @MethodSource("blankValues")
    void validate_blankText_reportsTextViolation(String text, String label) {
        LocalDateTime dateTime = LocalDateTime.of(2030, 1, 15, 12, 0);
        CommentDto dto = new CommentDto(40L, text, "Booker", dateTime);

        assertThat(validator.validate(dto))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("text");
    }
}