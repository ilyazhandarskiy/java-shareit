package ru.practicum.shareit.booking.model;

public enum BookingState {
    ALL, //все
    CURRENT, // текущие
    PAST, // завершенные
    FUTURE, // запланированные
    WAITING, // ожидающие подтверждения
    REJECTED; // отклоненные

    public static BookingState from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Unknown state: " + value);
        }
        for (BookingState state : values()) {
            if (state.name().equals(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown state: " + value);
    }
}
