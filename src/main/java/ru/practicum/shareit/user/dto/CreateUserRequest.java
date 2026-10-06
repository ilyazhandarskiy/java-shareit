package ru.practicum.shareit.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Имя пользователя не указано или пусто")
    private String name;

    @NotBlank(message = "Электронная почта не указана или пуста")
    @Email(message = "Электронная почта указана некорректно")
    private String email;
}

