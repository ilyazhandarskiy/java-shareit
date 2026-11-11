package ru.practicum.shareit.booking;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.practicum.shareit.booking.model.BookingState;

import static org.assertj.core.api.Assertions.*;

class BookingStateTest {
    @ParameterizedTest
    @EnumSource(BookingState.class)
    void parsesSupportedStates(BookingState state) {
        assertThat(BookingState.from(state.name())).isEqualTo(state);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "UNKNOWN", "all", " ALL "})
    void rejectsUnsupportedStates(String value) {
        assertThatThrownBy(() -> BookingState.from(value))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Unknown state: " + value);
    }
}
