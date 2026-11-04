package ru.practicum.shareit.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    LocalDateTime timestamp;
    int status;
    String error;
    String message;
    String path;
}