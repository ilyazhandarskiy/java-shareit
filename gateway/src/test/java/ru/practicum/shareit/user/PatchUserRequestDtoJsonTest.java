package ru.practicum.shareit.user;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.user.dto.PatchUserRequestDto;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@JsonTest
class PatchUserRequestDtoJsonTest {

    @Autowired
    private JacksonTester<PatchUserRequestDto> json;

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
                arguments("", "empty string"),
                arguments(" ", "single space"),
                arguments("\t\n", "tab and newline")
        );
    }

    static Stream<Arguments> invalidEmails() {
        return Stream.of(
                arguments("", "empty string"),
                arguments(" ", "single space"),
                arguments("\t\n", "tab and newline"),
                arguments("invalid", "no at sign"),
                arguments("john@", "no domain"),
                arguments("@example.com", "no local part")
        );
    }

    @Test
    void serialize_allFields_writesAllProperties() throws Exception {
        PatchUserRequestDto dto = new PatchUserRequestDto("John", "john@example.com");

        JsonContent<PatchUserRequestDto> result = json.write(dto);

        assertThat(result).extractingJsonPathStringValue("$.name").isEqualTo("John");
        assertThat(result).extractingJsonPathStringValue("$.email").isEqualTo("john@example.com");
    }

    @Test
    void deserialize_allFields_createsEqualDto() throws Exception {
        String content = "{\"name\":\"John\",\"email\":\"john@example.com\"}";

        PatchUserRequestDto result = json.parseObject(content);

        assertThat(result).isEqualTo(new PatchUserRequestDto("John", "john@example.com"));
    }

    @Test
    void deserialize_onlyName_leavesEmailNull() throws Exception {
        PatchUserRequestDto result = json.parseObject("{\"name\":\"John\"}");

        assertThat(result).isEqualTo(new PatchUserRequestDto("John", null));
    }

    @Test
    void deserialize_onlyEmail_leavesNameNull() throws Exception {
        PatchUserRequestDto result = json.parseObject("{\"email\":\"john@example.com\"}");

        assertThat(result).isEqualTo(new PatchUserRequestDto(null, "john@example.com"));
    }

    @Test
    void deserialize_emptyObject_leavesAllFieldsNull() throws Exception {
        PatchUserRequestDto result = json.parseObject("{}");

        assertThat(result).isEqualTo(new PatchUserRequestDto(null, null));
    }

    @Test
    void deserialize_explicitNulls_leavesAllFieldsNull() throws Exception {
        PatchUserRequestDto result = json.parseObject("{\"name\":null,\"email\":null}");

        assertThat(result).isEqualTo(new PatchUserRequestDto(null, null));
    }

    @Test
    void roundTrip_allFields_preservesAllFields() throws Exception {
        PatchUserRequestDto original = new PatchUserRequestDto("John", "john@example.com");

        PatchUserRequestDto restored = json.parseObject(json.write(original).getJson());

        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void validate_allFields_hasNoViolations() {
        PatchUserRequestDto dto = new PatchUserRequestDto("John", "john@example.com");

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void validate_onlyName_hasNoViolations() {
        PatchUserRequestDto dto = new PatchUserRequestDto("John", null);

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void validate_onlyEmail_hasNoViolations() {
        PatchUserRequestDto dto = new PatchUserRequestDto(null, "john@example.com");

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void validate_allFieldsNull_hasNoViolations() {
        PatchUserRequestDto dto = new PatchUserRequestDto(null, null);

        assertThat(validator.validate(dto)).isEmpty();
    }

    @ParameterizedTest(name = "name = {1}")
    @MethodSource("blankValues")
    void validate_blankName_reportsNameViolation(String name, String label) {
        PatchUserRequestDto dto = new PatchUserRequestDto(name, "john@example.com");

        assertThat(validator.validate(dto))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("name");
    }

    @ParameterizedTest(name = "email = {1}")
    @MethodSource("invalidEmails")
    void validate_invalidEmail_reportsEmailViolation(String email, String label) {
        PatchUserRequestDto dto = new PatchUserRequestDto("John", email);

        assertThat(validator.validate(dto))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsOnly("email");
    }
}