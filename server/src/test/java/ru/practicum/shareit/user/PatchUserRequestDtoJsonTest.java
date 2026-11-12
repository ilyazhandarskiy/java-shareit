package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.user.dto.PatchUserRequestDto;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PatchUserRequestDtoJsonTest {
    private final JacksonTester<PatchUserRequestDto> json;

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
    void deserialize_unknownProperty_isIgnored() throws Exception {
        String content = "{\"name\":\"John\",\"email\":\"john@example.com\",\"extra\":\"value\"}";

        PatchUserRequestDto result = json.parseObject(content);

        assertThat(result).isEqualTo(new PatchUserRequestDto("John", "john@example.com"));
    }

    @Test
    void roundTrip_allFields_preservesAllFields() throws Exception {
        PatchUserRequestDto original = new PatchUserRequestDto("John", "john@example.com");

        PatchUserRequestDto restored = json.parseObject(json.write(original).getJson());

        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
    }
}