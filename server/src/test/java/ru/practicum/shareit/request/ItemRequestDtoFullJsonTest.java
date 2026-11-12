package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.request.dto.ItemRequestDtoFull;
import ru.practicum.shareit.request.dto.LinkedItemDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JsonTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ItemRequestDtoFullJsonTest {
    private final JacksonTester<ItemRequestDtoFull> json;

    @Test
    void serialize_allFields_writesAllPropertiesWithoutNanos() throws Exception {
        ItemRequestDtoFull dto = new ItemRequestDtoFull(30L, "Need a drill", 2L,
                LocalDateTime.of(2030, 1, 15, 12, 0, 0, 123_456_789),
                List.of(new LinkedItemDto(10L, "Drill", 1L)));

        JsonContent<ItemRequestDtoFull> result = json.write(dto);

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(30);
        assertThat(result).extractingJsonPathStringValue("$.description").isEqualTo("Need a drill");
        assertThat(result).extractingJsonPathNumberValue("$.requestorId").isEqualTo(2);
        assertThat(result).extractingJsonPathStringValue("$.created").isEqualTo("2030-01-15T12:00:00");
        assertThat(result).extractingJsonPathArrayValue("$.items").hasSize(1);
        assertThat(result).extractingJsonPathNumberValue("$.items[0].id").isEqualTo(10);
        assertThat(result).extractingJsonPathStringValue("$.items[0].name").isEqualTo("Drill");
        assertThat(result).extractingJsonPathNumberValue("$.items[0].ownerId").isEqualTo(1);
    }

    @Test
    void serialize_emptyItems_writesEmptyArray() throws Exception {
        ItemRequestDtoFull dto = new ItemRequestDtoFull(1L, "x", 2L,
                LocalDateTime.of(2030, 1, 15, 12, 0), List.of());

        assertThat(json.write(dto)).extractingJsonPathArrayValue("$.items").isEmpty();
    }

    @Test
    void serialize_midnightCreated_keepsZeroSeconds() throws Exception {
        ItemRequestDtoFull dto = new ItemRequestDtoFull(1L, "x", 2L,
                LocalDateTime.of(2030, 1, 15, 0, 0), List.of());

        assertThat(json.write(dto)).extractingJsonPathStringValue("$.created")
                .isEqualTo("2030-01-15T00:00:00");
    }

    @Test
    void deserialize_allFields_createsDtoWithNestedItems() throws Exception {
        String content = "{\"id\":30,\"description\":\"Need a drill\",\"requestorId\":2,"
                + "\"created\":\"2030-01-15T12:00:00\","
                + "\"items\":[{\"id\":10,\"name\":\"Drill\",\"ownerId\":1}]}";

        ItemRequestDtoFull result = json.parseObject(content);

        assertThat(result.getId()).isEqualTo(30L);
        assertThat(result.getDescription()).isEqualTo("Need a drill");
        assertThat(result.getRequestorId()).isEqualTo(2L);
        assertThat(result.getCreated()).isEqualTo(LocalDateTime.of(2030, 1, 15, 12, 0));
        assertThat(result.getItems())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(new LinkedItemDto(10L, "Drill", 1L));
    }

    @Test
    void deserialize_withoutItems_leavesItemsEmpty() throws Exception {
        String content = "{\"id\":30,\"description\":\"Need a drill\",\"requestorId\":2,"
                + "\"created\":\"2030-01-15T12:00:00\"}";

        ItemRequestDtoFull result = json.parseObject(content);

        assertThat(result.getItems()).isEmpty();
    }

    @Test
    void deserialize_emptyItemsArray_leavesItemsEmpty() throws Exception {
        String content = "{\"id\":30,\"created\":\"2030-01-15T12:00:00\",\"items\":[]}";

        ItemRequestDtoFull result = json.parseObject(content);

        assertThat(result.getItems()).isEmpty();
    }

    @Test
    void roundTrip_allFields_preservesAllFields() throws Exception {
        ItemRequestDtoFull original = new ItemRequestDtoFull(30L, "Need a drill", 2L,
                LocalDateTime.of(2030, 1, 15, 12, 0, 0),
                List.of(new LinkedItemDto(10L, "Drill", 1L)));

        ItemRequestDtoFull restored = json.parseObject(json.write(original).getJson());

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
        String content = "{\"id\":30,\"created\":\"" + created + "\"}";

        assertThatThrownBy(() -> json.parseObject(content))
                .isInstanceOfSatisfying(InvalidFormatException.class, exception ->
                        assertThat(exception.getPath())
                                .extracting(JsonMappingException.Reference::getFieldName)
                                .containsExactly("created"));
    }
}