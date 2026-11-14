package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.user.dto.UserDto;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class UserDtoJsonTest {
    private final JacksonTester<UserDto> json;

    @Test
    void serialize_allFields_writesAllProperties() throws Exception {
        UserDto dto = new UserDto(1L, "John", "john@example.com");

        JsonContent<UserDto> result = json.write(dto);

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(1);
        assertThat(result).extractingJsonPathStringValue("$.name").isEqualTo("John");
        assertThat(result).extractingJsonPathStringValue("$.email").isEqualTo("john@example.com");
    }

    @Test
    void deserialize_allFields_createsDto() throws Exception {
        String content = "{\"id\":1,\"name\":\"John\",\"email\":\"john@example.com\"}";

        UserDto result = json.parseObject(content);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("John");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void deserialize_emptyObject_leavesAllFieldsNull() throws Exception {
        UserDto result = json.parseObject("{}");

        assertThat(result.getId()).isNull();
        assertThat(result.getName()).isNull();
        assertThat(result.getEmail()).isNull();
    }

    @Test
    void deserialize_unknownProperty_isIgnored() throws Exception {
        String content = "{\"id\":1,\"name\":\"John\",\"email\":\"john@example.com\",\"extra\":\"value\"}";

        UserDto result = json.parseObject(content);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("John");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void roundTrip_allFields_preservesAllFields() throws Exception {
        UserDto original = new UserDto(1L, "John", "john@example.com");

        UserDto restored = json.parseObject(json.write(original).getJson());

        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
    }
}