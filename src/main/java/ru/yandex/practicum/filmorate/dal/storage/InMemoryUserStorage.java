package ru.yandex.practicum.filmorate.dal.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

@Component("inMemoryUserStorage")
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();
    private final Map<Long, Set<Long>> friendships = new HashMap<>();

    @Override
    public Collection<User> findAll() {
        return users.values();
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public User add(User user) {
        user.setId(getNextId());
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User update(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public void deleteById(Long id) {
        users.remove(id);
    }

    @Override
    public boolean existsById(Long id) {
        return users.containsKey(id);
    }

    @Override
    public void addFriend(long userId, long friendId) {
        if (users.containsKey(userId) && users.containsKey(friendId)) {
            if (!friendships.containsKey(userId)) {
                friendships.put(userId, new HashSet<>());
            }
            friendships.get(userId).add(friendId);
        }
    }

    @Override
    public void removeFriend(long userId, long friendId) {
        // Удаляем друга, если пользователь и список друзей существуют
        if (friendships.containsKey(userId)) {
            friendships.get(userId).remove(friendId);
        }
    }

    @Override
    public List<User> findFriends(long userId) {
        List<User> result = new ArrayList<>();

        if (friendships.containsKey(userId)) {
            Set<Long> friendIds = friendships.get(userId);
            for (Long friendId : friendIds) {
                User friend = users.get(friendId);
                if (friend != null) {
                    result.add(friend);
                }
            }
        }
        return result;
    }

    @Override
    public List<User> findCommonFriends(long userId, long otherUserId) {
        List<User> common = new ArrayList<>();

        if (friendships.containsKey(userId) && friendships.containsKey(otherUserId)) {
            Set<Long> userFriends = friendships.get(userId);
            Set<Long> otherFriends = friendships.get(otherUserId);

            for (Long id : userFriends) {
                if (otherFriends.contains(id)) {
                    User friend = users.get(id);
                    if (friend != null) {
                        common.add(friend);
                    }
                }
            }
        }
        return common;
    }

    private void checkUserExists(long id) {
        if (!users.containsKey(id)) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
    }

    private long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}
