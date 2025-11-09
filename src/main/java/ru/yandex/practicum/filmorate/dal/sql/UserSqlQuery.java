package ru.yandex.practicum.filmorate.dal.sql;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserSqlQuery {
    FIND_ALL("""
            SELECT id, email, login, name, birthday
            FROM users
            """),

    FIND_BY_ID("""
            SELECT id, email, login, name, birthday
            FROM users
            WHERE id = ?
            """),

    INSERT("""
            INSERT INTO users (email, login, name, birthday)
            VALUES (?, ?, ?, ?)
            """),

    UPDATE("""
            UPDATE users
            SET email = ?, login = ?, name = ?, birthday = ?
            WHERE id = ?
            """),

    DELETE_BY_ID("""
            DELETE FROM users WHERE id = ?
            """),

    EXISTS_BY_ID("""
            SELECT COUNT(*) FROM users WHERE id = ?
            """),

    ADD_FRIEND("""
            MERGE INTO friendships (user_id, friend_id)
            KEY(user_id, friend_id)
            VALUES (?, ?)
            """),

    REMOVE_FRIEND("""
            DELETE FROM friendships
            WHERE user_id = ? AND friend_id = ?
            """),

    FIND_FRIENDS("""
            SELECT u.id, u.email, u.login, u.name, u.birthday
            FROM friendships f
            JOIN users u ON u.id = f.friend_id
            WHERE f.user_id = ?
            """),

    FIND_COMMON_FRIENDS("""
            SELECT u.id, u.email, u.login, u.name, u.birthday
            FROM friendships f1
            JOIN friendships f2 ON f1.friend_id = f2.friend_id
            JOIN users u ON u.id = f1.friend_id
            WHERE f1.user_id = ? AND f2.user_id = ?
            """);

    private final String sql;
}
