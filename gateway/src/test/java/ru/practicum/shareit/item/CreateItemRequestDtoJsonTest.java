package ru.practicum.shareit.item;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.item.dto.CreateItemRequestDto;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void serialize_allFields_writesAllProperties() throws Exception {
        CreateItemRequestDto dto = new CreateItemRequestDto("Drill", "Cordless", false, 9L);

        JsonContent<CreateItemRequestDto> result = json.write(dto);

        assertThat(result).extractingJsonPathStringValue("$.name").isEqualTo("Drill");
        assertThat(result).extractingJsonPathStringValue("$.description").isEqualTo("Cordless");
        assertThat(result).extractingJsonPathBooleanValue("$.available").isFalse();
        assertThat(result).extractingJsonPathNumberValue("$.requestId").isEqualTo(9);
    }

    @Test
    void deserialize_allFields_createsEqualDto() throws Exception {
        String content = "{\"name\":\"Drill\",\"description\":\"Cordless\",\"available\":false,\"requestId\":9}";

        CreateItemRequestDto result = json.parseObject(content);

        assertThat(result).isEqualTo(new CreateItemRequestDto("Drill", "Cordless", false, 9L));
    }

    @Test
    void deserialize_withoutRequestId_leavesRequestIdNull() throws Exception {
        String content = "{\"name\":\"Drill\",\"description\":\"Cordless\",\"available\":true}";

        CreateItemRequestDto result = json.parseObject(content);

        assertThat(result).isEqualTo(new CreateItemRequestDto("Drill", "Cordless", true, null));
    }

    @Test
    void deserialize_withoutAvailable_leavesAvailableNull() throws Exception {
        String content = "{\"name\":\"Drill\",\"description\":\"Cordless\"}";

        CreateItemRequestDto result = json.parseObject(content);

        assertThat(result).isEqualTo(new CreateItemRequestDto("Drill", "Cordless", null, null));
    }

    @Test
    void roundTrip_validDto_preservesAllFields() throws Exception {
        CreateItemRequestDto original = new CreateItemRequestDto("Drill", "Cordless", true, 9L);

        CreateItemRequestDto restored = json.parseObject(json.write(original).getJson());

        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void validate_nullRequestId_hasNoViolations() {
        CreateItemRequestDto dto = new CreateItemRequestDto("Drill", "Cordless", true, null);

        assertThat(validator.validate(dto)).isEmpty();
    }

    @ParameterizedTest(name = "available = {0}")
    @ValueSource(booleans = {true, false})
    void validate_availableSpecified_hasNoViolations(boolean available) {
        CreateItemRequestDto dto = new CreateItemRequestDto("Drill", "Cordless", available, 9L);

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void validate_nullAvailable_reportsAvailableViolation() {
        CreateItemRequestDto dto = new CreateItemRequestDto("Drill", "Cordless", null, null);

        assertThat(validator.validate(dto))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("available");
    }

    @ParameterizedTest(name = "name = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void validate_blankName_reportsNameViolation(String name) {
        CreateItemRequestDto dto = new CreateItemRequestDto(name, "Cordless", true, null);

        assertThat(validator.validate(dto))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("name");
    }

    @ParameterizedTest(name = "description = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void validate_blankDescription_reportsDescriptionViolation(String description) {
        CreateItemRequestDto dto = new CreateItemRequestDto("Drill", description, true, null);

        assertThat(validator.validate(dto))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("description");
    }
}