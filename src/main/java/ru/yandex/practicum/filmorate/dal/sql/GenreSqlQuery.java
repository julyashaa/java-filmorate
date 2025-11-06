package ru.yandex.practicum.filmorate.dal.sql;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GenreSqlQuery {
    FIND_ALL_GENRE("SELECT id, name FROM genres ORDER BY id"),
    FIND_GENRE_BY_ID("SELECT id, name FROM genres WHERE id = :id");

    private final String sql;
}
