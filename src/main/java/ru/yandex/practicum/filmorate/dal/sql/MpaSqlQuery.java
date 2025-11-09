package ru.yandex.practicum.filmorate.dal.sql;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MpaSqlQuery {
    FIND_ALL_MPA("SELECT id, name FROM mpa_ratings ORDER BY id"),
    FIND_MPA_BY_ID("SELECT id, name FROM mpa_ratings WHERE id = :id");

    private final String sql;
}
