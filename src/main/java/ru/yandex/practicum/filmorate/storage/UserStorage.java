package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.Optional;

public interface UserStorage {
    Collection<User> findAll();

    Optional<User> findById(Long id);

    User add(User user);

    User update(User user);

    void deleteById(Long id);

    boolean existsById(Long id);
}
