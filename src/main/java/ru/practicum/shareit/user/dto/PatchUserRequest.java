package ru.practicum.shareit.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatchUserRequest {

    @Pattern(regexp = "(?s).*\\S.*", message = "Имя не должно быть пустым")
    private String name;

    @Pattern(regexp = "(?s).*\\S.*", message = "Почта не должна быть пустой")
    @Email(message = "Электронная почта указана некорректно")
    private String email;
}