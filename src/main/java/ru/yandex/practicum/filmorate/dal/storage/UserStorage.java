package ru.yandex.practicum.filmorate.dal.storage;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserStorage {
    Collection<User> findAll();

    Optional<User> findById(Long id);

    User add(User user);

    User update(User user);

    void deleteById(Long id);

    boolean existsById(Long id);

    void addFriend(long userId, long friendId);

    void removeFriend(long userId, long friendId);

    List<User> findFriends(long userId);

    List<User> findCommonFriends(long userId, long otherUserId);
}
