package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationUserException;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserStorage userStorage;

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
        User user = requiredUser(userId);
        User friend = requiredUser(friendId);
        FriendshipStatus status = friend.getFriends().get(userId);
        if (status == FriendshipStatus.UNCONFIRMED) {
            user.getFriends().put(friendId, FriendshipStatus.CONFIRMED);
            friend.getFriends().put(userId, FriendshipStatus.CONFIRMED);
            log.info("Дружба подтверждена: {} ⇄ {}", userId, friendId);
        } else {
            user.getFriends().put(friendId, FriendshipStatus.UNCONFIRMED);
            log.info("Отправлена заявка в друзья: {} → {}", userId, friendId);
        }
        userStorage.update(user);
        userStorage.update(friend);
    }

    public void removeFriend(long userId, long friendId) {
        User user = requiredUser(userId);
        User friend = requiredUser(friendId);
        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);
        userStorage.update(user);
        userStorage.update(friend);
        log.info("Пользователь {} и {} больше не друзья.", userId, friendId);
    }

    public List<User> listFriends(long userId) {
        User user = requiredUser(userId);
        return user.getFriends()
                .entrySet()
                .stream()
                .filter(e -> e.getValue() == FriendshipStatus.CONFIRMED)
                .map(e -> requiredUser(e.getKey()))
                .collect(Collectors.toList());
    }

    public List<User> commonFriends(long userId, long friendId) {
        User user = requiredUser(userId);
        User friend = requiredUser(friendId);
        return user.getFriends()
                .entrySet()
                .stream()
                .filter(e -> e.getValue() == FriendshipStatus.CONFIRMED)     // подтвержден у u1
                .map(e -> e.getKey())
                .filter(fid -> friend.getFriends().getOrDefault(fid, null) == FriendshipStatus.CONFIRMED) // и у u2
                .map(this::requiredUser)
                .collect(Collectors.toList());
    }

    private User requiredUser(long id) {
        return userStorage.findById(id)
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