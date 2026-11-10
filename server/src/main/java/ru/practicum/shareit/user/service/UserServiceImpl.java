package ru.practicum.shareit.user.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.CreateUserRequestDto;
import ru.practicum.shareit.user.dto.PatchUserRequestDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Transactional
    @Override
    public UserDto create(CreateUserRequestDto createUserRequestDto) {
        checkEmailUnique(createUserRequestDto.getEmail());
        User createdUser = UserMapper.toUser(createUserRequestDto);
        return UserMapper.toUserDto(userRepository.save(createdUser));
    }

    @Transactional
    @Override
    public UserDto patch(long userId, PatchUserRequestDto patchUserRequestDto) {
        User user = getUserOrThrow(userId);

        if (patchUserRequestDto.getEmail() != null) {
            if (!user.getEmail().equalsIgnoreCase(patchUserRequestDto.getEmail())) {
                checkEmailUnique(patchUserRequestDto.getEmail());
            }
        }

        UserMapper.patchUser(user, patchUserRequestDto);

        return UserMapper.toUserDto(userRepository.save(user));
    }

    @Override
    public UserDto getById(long userId) {
        return UserMapper.toUserDto(getUserOrThrow(userId));
    }

    @Override
    public List<UserDto> getAll() {
        return userRepository.findAll().stream().map(UserMapper::toUserDto).toList();
    }

    @Override
    @Transactional
    public void deleteById(long userId) {
        getUserOrThrow(userId);
        userRepository.deleteById(userId);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + userId));
    }

    private void checkEmailUnique(String email) {
        userRepository.findByEmail(email).ifPresent(u -> {
            throw new ConflictException("Email already in use: " + email);
        });
    }
}
