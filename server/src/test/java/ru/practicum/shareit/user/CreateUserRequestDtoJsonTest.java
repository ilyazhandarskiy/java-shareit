package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.user.dto.CreateUserRequestDto;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class CreateUserRequestDtoJsonTest {
    private final JacksonTester<CreateUserRequestDto> json;

    @Test
    void serialize_allFields_writesAllProperties() throws Exception {
        CreateUserRequestDto dto = new CreateUserRequestDto("John", "john@example.com");

        JsonContent<CreateUserRequestDto> result = json.write(dto);

        assertThat(result).extractingJsonPathStringValue("$.name").isEqualTo("John");
        assertThat(result).extractingJsonPathStringValue("$.email").isEqualTo("john@example.com");
    }

    @Test
    void deserialize_allFields_createsEqualDto() throws Exception {
        String content = "{\"name\":\"John\",\"email\":\"john@example.com\"}";

        CreateUserRequestDto result = json.parseObject(content);

        assertThat(result).isEqualTo(new CreateUserRequestDto("John", "john@example.com"));
    }

    @Test
    void deserialize_emptyObject_leavesAllFieldsNull() throws Exception {
        CreateUserRequestDto result = json.parseObject("{}");

        assertThat(result).isEqualTo(new CreateUserRequestDto(null, null));
    }

    @Test
    void deserialize_unknownProperty_isIgnored() throws Exception {
        String content = "{\"name\":\"John\",\"email\":\"john@example.com\",\"extra\":\"value\"}";

        CreateUserRequestDto result = json.parseObject(content);

        assertThat(result).isEqualTo(new CreateUserRequestDto("John", "john@example.com"));
    }

    @Test
    void roundTrip_allFields_preservesAllFields() throws Exception {
        CreateUserRequestDto original = new CreateUserRequestDto("John", "john@example.com");

        CreateUserRequestDto restored = json.parseObject(json.write(original).getJson());

        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
    }
}