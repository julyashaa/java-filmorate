package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationUserException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.dal.storage.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class UserService {

    private final UserStorage userStorage;

    public UserService(@Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public Collection<User> findAll() {
        log.info("Запрошен список всех пользователей. Количество: {}", userStorage.findAll().size());
        return userStorage.findAll();
    }

    public User create(User user) {
        validateUser(user);
        User created = userStorage.add(user);
        log.info("Создан новый пользователь {}", user);
        return created;
    }

    public User update(User user) {
        if (user.getId() == null || !userStorage.existsById(user.getId())) {
            log.error("Ошибка обновления пользователя: id {} не найден", user.getId());
            throw new NotFoundException("Пользователь с таким id не найден или id не указан");
        }
        validateUser(user);
        User updated = userStorage.update(user);
        log.info("Обновлены данные пользователя с id {}", user.getId());
        return updated;
    }

    public void addFriend(long userId, long friendId) {
        if (userId == friendId) {
            throw new ValidationUserException("Нельзя добавить в друзья самого себя");
        }
        requiredUser(userId);
        requiredUser(friendId);

        userStorage.addFriend(userId, friendId);
        log.info("Пользователь {} добавил в друзья {}", userId, friendId);
    }

    public void removeFriend(long userId, long friendId) {
        requiredUser(userId);
        requiredUser(friendId);

        userStorage.removeFriend(userId, friendId);
        log.info("Пользователь {} удалил из друзей {}", userId, friendId);
    }

    public List<User> listFriends(long userId) {
        requiredUser(userId);
        return userStorage.findFriends(userId);
    }

    public List<User> commonFriends(long userId, long friendId) {
        requiredUser(userId);
        requiredUser(friendId);
        return userStorage.findCommonFriends(userId, friendId);
    }

    private void requiredUser(long id) {
        userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + id + " не найден."));
    }

    private void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            log.error("Ошибка валидации пользователя: некорректный email {}", user.getEmail());
            throw new ValidationUserException("Электронная почта не может быть пустой и должна содержать символ '@'");
        }

        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            log.error("Ошибка валидации пользователя: некорректный логин {}", user.getLogin());
            throw new ValidationUserException("Логин не может быть пустой и не может содержать пробелы");
        }

        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            log.error("Ошибка валидации пользователя: некорректная дата рождения {}", user.getBirthday());
            throw new ValidationUserException("Дата рождения не может быть в будущем");
        }
    }
}