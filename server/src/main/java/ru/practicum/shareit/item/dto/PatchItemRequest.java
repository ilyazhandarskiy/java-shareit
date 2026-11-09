package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PatchItemRequest {
    @Pattern(regexp = "(?s).*\\S.*", message = "Наименование не может быть пустым")
    private String name;

    @Pattern(regexp = "(?s).*\\S.*", message = "Описание не может быть пустым")
    private String description;

    private Boolean available;
}
