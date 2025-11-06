package ru.yandex.practicum.filmorate.dal.sql;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FilmSqlQuery {
    FIND_ALL("""
                SELECT f.id, f.name, f.description, f.release_date, f.duration,
                       f.mpa_id, mr.name AS mpa_name
                FROM films f
                LEFT JOIN mpa_ratings mr ON f.mpa_id = mr.id
                ORDER BY f.id
            """),

    FIND_BY_ID("""
                SELECT f.id, f.name, f.description, f.release_date, f.duration,
                       f.mpa_id, mr.name AS mpa_name
                FROM films f
                JOIN mpa_ratings mr ON mr.id = f.mpa_id
                WHERE f.id = :id
            """),

    INSERT("""
                INSERT INTO films (name, description, release_date, duration, mpa_id)
                VALUES (:name, :description, :releaseDate, :duration, :mpaId)
            """),

    UPDATE("""
                UPDATE films SET
                    name = :name,
                    description = :description,
                    release_date = :releaseDate,
                    duration = :duration,
                    mpa_id = :mpaId
                WHERE id = :id
            """),

    DELETE_BY_ID("DELETE FROM films WHERE id = :id"),

    EXISTS_BY_ID("SELECT COUNT(*) FROM films WHERE id = :id"),

    POPULAR("""
                SELECT f.id, f.name, f.description, f.release_date, f.duration,
                       f.mpa_id, mr.name AS mpa_name
                FROM films f
                LEFT JOIN mpa_ratings mr ON f.mpa_id = mr.id
                LEFT JOIN film_likes fl ON fl.film_id = f.id
                GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id, mr.name
                ORDER BY COUNT(fl.user_id) DESC, f.id
                LIMIT :count
            """),

    ADD_LIKE("""
                MERGE INTO film_likes (film_id, user_id) KEY(film_id, user_id)
                VALUES (:filmId, :userId)
            """),

    REMOVE_LIKE("""
                DELETE FROM film_likes WHERE film_id = :filmId AND user_id = :userId
            """),

    LOAD_LIKE_USER_IDS("""
                SELECT user_id FROM film_likes WHERE film_id = :filmId
            """),

    GENRES_BY_FILM_ID("""
                SELECT g.id, g.name
                FROM film_genres fg
                JOIN genres g ON g.id = fg.genre_id
                WHERE fg.film_id = :filmId
                ORDER BY g.id
            """),

    DELETE_FILM_GENRES("DELETE FROM film_genres WHERE film_id = :id"),

    INSERT_FILM_GENRE("""
                INSERT INTO film_genres (film_id, genre_id) VALUES (:filmId, :genreId)
            """),

    GENRES_FOR_FILM_LIST("""
                SELECT fg.film_id, g.id AS genre_id, g.name
                FROM film_genres fg
                JOIN genres g ON g.id = fg.genre_id
                WHERE fg.film_id IN (:ids)
                ORDER BY fg.film_id, g.id
            """);

    private final String sql;
}
