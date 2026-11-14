package ru.practicum.shareit.user.service;

import ru.practicum.shareit.user.dto.CreateUserRequestDto;
import ru.practicum.shareit.user.dto.PatchUserRequestDto;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

public interface UserService {

    UserDto create(CreateUserRequestDto createUserRequestDto);

    UserDto patch(long userId, PatchUserRequestDto patchUserRequestDto);

    UserDto getById(long userId);

    List<UserDto> getAll();

    void deleteById(long userId);
}
