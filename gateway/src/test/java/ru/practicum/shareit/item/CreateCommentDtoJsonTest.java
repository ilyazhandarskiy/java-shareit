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
import ru.practicum.shareit.item.dto.CreateCommentDto;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class CreateCommentDtoJsonTest {

    @Autowired
    private JacksonTester<CreateCommentDto> json;

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
    void serialize() throws Exception {
        JsonContent<CreateCommentDto> result = json.write(new CreateCommentDto("Works well"));

        assertThat(result).extractingJsonPathStringValue("$.text").isEqualTo("Works well");
    }

    @Test
    void deserialize() throws Exception {
        CreateCommentDto result = json.parseObject("{\"text\":\"Works well\"}");

        assertThat(result.getText()).isEqualTo("Works well");
    }

    @Test
    void deserializeWhenTextIsNotSpecified() throws Exception {
        assertThat(json.parseObject("{}").getText()).isNull();
    }

    @Test
    void roundTrip() throws Exception {
        CreateCommentDto original = new CreateCommentDto("Works well");

        CreateCommentDto restored = json.parseObject(json.write(original).getJson());

        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void validateCreateCommentDto() {
        assertThat(validator.validate(new CreateCommentDto("Works well"))).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void validateWhenTextIsBlank(String text) {
        assertThat(validator.validate(new CreateCommentDto(text)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("text");
    }
}