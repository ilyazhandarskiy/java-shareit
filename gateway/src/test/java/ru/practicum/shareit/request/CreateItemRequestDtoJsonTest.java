package ru.practicum.shareit.request;

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
import ru.practicum.shareit.request.dto.CreateItemRequestDto;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@JsonTest
class CreateItemRequestDtoJsonTest {

    @Autowired
    private JacksonTester<CreateItemRequestDto> json;

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
    void serialize_description_writesProperty() throws Exception {
        JsonContent<CreateItemRequestDto> result = json.write(new CreateItemRequestDto("Need a drill"));

        assertThat(result).extractingJsonPathStringValue("$.description").isEqualTo("Need a drill");
    }

    @Test
    void deserialize_description_setsField() throws Exception {
        CreateItemRequestDto result = json.parseObject("{\"description\":\"Need a drill\"}");

        assertThat(result.getDescription()).isEqualTo("Need a drill");
    }

    @Test
    void deserialize_withoutDescription_leavesFieldNull() throws Exception {
        assertThat(json.parseObject("{}").getDescription()).isNull();
    }

    @Test
    void roundTrip_validDto_preservesDescription() throws Exception {
        CreateItemRequestDto original = new CreateItemRequestDto("Need a drill");

        CreateItemRequestDto restored = json.parseObject(json.write(original).getJson());

        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void validate_validDescription_hasNoViolations() {
        assertThat(validator.validate(new CreateItemRequestDto("Need a drill"))).isEmpty();
    }

    @ParameterizedTest(name = "description = {1}")
    @MethodSource("blankValues")
    void validate_blankDescription_reportsDescriptionViolation(String description, String label) {
        assertThat(validator.validate(new CreateItemRequestDto(description)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("description");
    }
}