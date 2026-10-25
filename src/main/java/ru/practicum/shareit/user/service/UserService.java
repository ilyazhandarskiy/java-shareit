package ru.practicum.shareit.user.service;

import ru.practicum.shareit.user.dto.CreateUserRequest;
import ru.practicum.shareit.user.dto.PatchUserRequest;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

public interface UserService {

    UserDto create(CreateUserRequest createUserRequest);

    UserDto patch(long userId, PatchUserRequest patchUserRequest);

    UserDto getById(long userId);

    List<UserDto> getAll();

    void deleteById(long userId);
}
