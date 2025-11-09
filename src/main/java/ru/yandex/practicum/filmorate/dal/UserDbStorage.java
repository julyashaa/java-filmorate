package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.UserRowMapper;
import ru.yandex.practicum.filmorate.dal.sql.UserSqlQuery;
import ru.yandex.practicum.filmorate.dal.storage.UserStorage;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository("userDbStorage")
@RequiredArgsConstructor
public class UserDbStorage implements UserStorage {
    private final JdbcTemplate jdbcTemplate;
    private final UserRowMapper userRowMapper = new UserRowMapper();

    @Override
    public Collection<User> findAll() {
        return jdbcTemplate.query(UserSqlQuery.FIND_ALL.getSql(), userRowMapper);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jdbcTemplate.query(UserSqlQuery.FIND_BY_ID.getSql(), userRowMapper, id)
                .stream()
                .findFirst();
    }

    @Override
    public User add(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(UserSqlQuery.INSERT.getSql(), Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setObject(3, user.getName());
            ps.setObject(4, user.getBirthday());
            return ps;
        }, keyHolder);
        Optional.ofNullable(keyHolder.getKey())
                .ifPresent(k -> user.setId(k.longValue()));
        return user;
    }

    @Override
    public User update(User user) {
        jdbcTemplate.update(UserSqlQuery.UPDATE.getSql(),
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday(),
                user.getId());
        return user;
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update(UserSqlQuery.DELETE_BY_ID.getSql(), id);
    }

    @Override
    public boolean existsById(Long id) {
        try {
            Integer count = jdbcTemplate.queryForObject(UserSqlQuery.EXISTS_BY_ID.getSql(), Integer.class, id);
            return count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void addFriend(long userId, long friendId) {
        if (userId == friendId) return;
        if (!existsById(userId) || !existsById(friendId)) return;

        jdbcTemplate.update(UserSqlQuery.ADD_FRIEND.getSql(), userId, friendId);
    }

    @Override
    public void removeFriend(long userId, long friendId) {
        jdbcTemplate.update(UserSqlQuery.REMOVE_FRIEND.getSql(), userId, friendId);
    }

    @Override
    public List<User> findFriends(long userId) {
        return jdbcTemplate.query(UserSqlQuery.FIND_FRIENDS.getSql(), userRowMapper, userId);
    }

    @Override
    public List<User> findCommonFriends(long userId, long otherUserId) {
        return jdbcTemplate.query(UserSqlQuery.FIND_COMMON_FRIENDS.getSql(), userRowMapper, userId, otherUserId);
    }
}
