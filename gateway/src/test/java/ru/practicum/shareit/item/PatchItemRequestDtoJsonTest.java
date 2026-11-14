package ru.practicum.shareit.item;

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
import ru.practicum.shareit.item.dto.PatchItemRequestDto;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@JsonTest
class PatchItemRequestDtoJsonTest {

    @Autowired
    private JacksonTester<PatchItemRequestDto> json;

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

    @Test
    void serialize_allFields_writesAllProperties() throws Exception {
        PatchItemRequestDto dto = new PatchItemRequestDto("Drill", "Cordless", false);

        JsonContent<PatchItemRequestDto> result = json.write(dto);

        assertThat(result).extractingJsonPathStringValue("$.name").isEqualTo("Drill");
        assertThat(result).extractingJsonPathStringValue("$.description").isEqualTo("Cordless");
        assertThat(result).extractingJsonPathBooleanValue("$.available").isFalse();
    }

    @Test
    void deserialize_allFields_createsEqualDto() throws Exception {
        String content = "{\"name\":\"Drill\",\"description\":\"Cordless\",\"available\":false}";

        PatchItemRequestDto result = json.parseObject(content);

        assertThat(result).isEqualTo(new PatchItemRequestDto("Drill", "Cordless", false));
    }

    @Test
    void deserialize_onlyAvailable_leavesOtherFieldsNull() throws Exception {
        PatchItemRequestDto result = json.parseObject("{\"available\":false}");

        assertThat(result.getName()).isNull();
        assertThat(result.getDescription()).isNull();
        assertThat(result.getAvailable()).isFalse();
    }

    @Test
    void deserialize_emptyObject_leavesAllFieldsNull() throws Exception {
        PatchItemRequestDto result = json.parseObject("{}");

        assertThat(result).isEqualTo(new PatchItemRequestDto(null, null, null));
    }

    @Test
    void deserialize_onlyName_leavesOtherFieldsNull() throws Exception {
        PatchItemRequestDto result = json.parseObject("{\"name\":\"Drill\"}");

        assertThat(result).isEqualTo(new PatchItemRequestDto("Drill", null, null));
    }

    @Test
    void deserialize_onlyDescription_leavesOtherFieldsNull() throws Exception {
        PatchItemRequestDto result = json.parseObject("{\"description\":\"Cordless\"}");

        assertThat(result).isEqualTo(new PatchItemRequestDto(null, "Cordless", null));
    }

    @Test
    void deserialize_explicitNulls_leavesAllFieldsNull() throws Exception {
        String content = "{\"name\":null,\"description\":null,\"available\":null}";

        PatchItemRequestDto result = json.parseObject(content);

        assertThat(result).isEqualTo(new PatchItemRequestDto(null, null, null));
    }

    @Test
    void roundTrip_allFields_preservesAllFields() throws Exception {
        PatchItemRequestDto original = new PatchItemRequestDto("Drill", "Cordless", true);

        PatchItemRequestDto restored = json.parseObject(json.write(original).getJson());

        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void validate_allFields_hasNoViolations() {
        PatchItemRequestDto dto = new PatchItemRequestDto("Drill", "Cordless", false);

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void validate_onlyAvailable_hasNoViolations() {
        PatchItemRequestDto dto = new PatchItemRequestDto(null, null, false);

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void validate_allFieldsNull_hasNoViolations() {
        PatchItemRequestDto dto = new PatchItemRequestDto(null, null, null);

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void validate_onlyName_hasNoViolations() {
        PatchItemRequestDto dto = new PatchItemRequestDto("Drill", null, null);

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void validate_onlyDescription_hasNoViolations() {
        PatchItemRequestDto dto = new PatchItemRequestDto(null, "Cordless", null);

        assertThat(validator.validate(dto)).isEmpty();
    }

    @ParameterizedTest(name = "name = {1}")
    @MethodSource("blankValues")
    void validate_blankName_reportsNameViolation(String name, String label) {
        PatchItemRequestDto dto = new PatchItemRequestDto(name, "Cordless", true);

        assertThat(validator.validate(dto))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("name");
    }

    @ParameterizedTest(name = "description = {1}")
    @MethodSource("blankValues")
    void validate_blankDescription_reportsDescriptionViolation(String description, String label) {
        PatchItemRequestDto dto = new PatchItemRequestDto("Drill", description, true);

        assertThat(validator.validate(dto))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("description");
    }
}