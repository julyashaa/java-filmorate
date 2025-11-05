package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.dal.storage.FilmStorage;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

@Repository("filmDbStorage")
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbc;
    private final FilmRowMapper filmRowMapper = new FilmRowMapper();

    private static final String FILM_BASE_SELECT = """
                SELECT f.id, f.name, f.description, f.release_date, f.duration,
                       f.mpa_id, mr.name AS mpa_name
                FROM films f
                JOIN mpa_ratings mr ON mr.id = f.mpa_id
            """;

    @Override
    public Collection<Film> findAll() {
        String sql = """
                    SELECT f.id, f.name, f.description, f.release_date, f.duration,
                           f.mpa_id, mr.name AS mpa_name
                    FROM films f
                    LEFT JOIN mpa_ratings mr ON f.mpa_id = mr.id
                    ORDER BY f.id
                """;
        List<Film> films = jdbc.query(sql, filmRowMapper);
        setGenresToFilms(films);
        return films;
    }

    @Override
    public Optional<Film> findById(Long id) {
        String sql = FILM_BASE_SELECT + " WHERE f.id = ?";
        List<Film> list = jdbc.query(sql, filmRowMapper, id);
        if (list.isEmpty()) return Optional.empty();
        Film film = list.get(0);
        film.setGenres(getGenres(id));
        film.setLikes(loadLikeUserIds(id));
        return Optional.of(film);
    }

    @Override
    public Film add(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setObject(3, film.getReleaseDate());
            ps.setObject(4, film.getDuration());
            ps.setObject(5, film.getMpa() != null ? film.getMpa().getId() : null);
            return ps;
        }, keyHolder);
        Optional.ofNullable(keyHolder.getKey()).ifPresent(k -> film.setId(k.longValue()));

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            var ids = film.getGenres().stream()
                    .map(Genre::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            jdbc.batchUpdate(
                    "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)",
                    ids.stream().map(gid -> new Object[]{film.getId(), gid}).toList()
            );
        }

        film.setGenres(getGenres(film.getId()));
        return film;
    }

    @Override
    public Film update(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE id = ?";
        jdbc.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa() != null ? film.getMpa().getId() : null,
                film.getId());

        if (film.getGenres() != null) {
            jdbc.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());
            if (!film.getGenres().isEmpty()) {
                var ids = film.getGenres().stream()
                        .map(Genre::getId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
                jdbc.batchUpdate(
                        "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)",
                        ids.stream().map(gid -> new Object[]{film.getId(), gid}).toList()
                );
            }
        }

        film.setGenres(getGenres(film.getId()));
        return film;
    }

    @Override
    public void deleteById(Long id) {
        jdbc.update("DELETE FROM films WHERE id = ?", id);
    }

    @Override
    public boolean existsById(Long id) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM films WHERE id = ?",
                Integer.class,
                id
        );
        return count > 0;
    }

    @Override
    public void addLike(long filmId, long userId) {
        jdbc.update("MERGE INTO film_likes (film_id, user_id) KEY(film_id, user_id) VALUES (?, ?)", filmId, userId);
    }

    @Override
    public void removeLike(long filmId, long userId) {
        jdbc.update("DELETE FROM film_likes WHERE film_id = ? AND user_id = ?", filmId, userId);
    }

    private Set<Long> loadLikeUserIds(long filmId) {
        String sql = "SELECT user_id FROM film_likes WHERE film_id = ?";
        return new HashSet<>(jdbc.query(sql, (rs, rn) -> rs.getLong("user_id"), filmId));
    }

    @Override
    public List<Film> getPopular(int count) {
        String sql = """
                    SELECT f.id, f.name, f.description, f.release_date, f.duration,
                           f.mpa_id, mr.name AS mpa_name
                    FROM films f
                    LEFT JOIN mpa_ratings mr ON f.mpa_id = mr.id
                    LEFT JOIN film_likes fl ON fl.film_id = f.id
                    GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id, mr.name
                    ORDER BY COUNT(fl.user_id) DESC, f.id
                    LIMIT ?
                """;
        List<Film> films = jdbc.query(sql, filmRowMapper, count);
        setGenresToFilms(films);
        return films;
    }

    @Override
    public List<Genre> getAllGenres() {
        String sql = "SELECT id, name FROM genres ORDER BY id";
        return jdbc.query(sql, this::genreMapper);
    }

    @Override
    public Optional<Genre> getGenreById(int id) {
        String sql = "SELECT id, name FROM genres WHERE id = ?";
        List<Genre> list = jdbc.query(sql, this::genreMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Set<Genre> getGenres(long filmId) {
        String sql = """
                    SELECT g.id, g.name
                    FROM film_genres fg
                    JOIN genres g ON g.id = fg.genre_id
                    WHERE fg.film_id = ?
                    ORDER BY g.id
                """;
        List<Genre> list = jdbc.query(sql, this::genreMapper, filmId);
        return new LinkedHashSet<>(list);
    }


    private void setGenresToFilms(List<Film> films) {
        if (films == null || films.isEmpty()) return;

        films.forEach(f -> f.setGenres(new HashSet<>()));

        List<Long> filmIds = films.stream().map(Film::getId).toList();
        String inSql = String.join(", ", Collections.nCopies(filmIds.size(), "?"));

        String sql = """
                SELECT fg.film_id, g.id AS genre_id, g.name
                FROM film_genres fg
                JOIN genres g ON g.id = fg.genre_id
                WHERE fg.film_id IN (""" + inSql + ") " +
                "ORDER BY fg.film_id, g.id";

        Map<Long, Set<Genre>> map = new HashMap<>();
        jdbc.query(sql, rs -> {
            long filmId = rs.getLong("film_id");
            int genreId = rs.getInt("genre_id");
            String name = rs.getString("name");
            Genre genre = new Genre();
            genre.setId(genreId);
            genre.setName(name);
            map.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(genre);
        }, filmIds.toArray());

        for (Film f : films) {
            Set<Genre> set = map.get(f.getId());
            if (set != null) f.setGenres(set);
        }
    }

    @Override
    public List<MpaRating> getAllMpa() {
        String sql = "SELECT id, name FROM mpa_ratings ORDER BY id";
        return jdbc.query(sql, this::mpaMapper);
    }

    @Override
    public Optional<MpaRating> getMpaById(int id) {
        String sql = "SELECT id, name FROM mpa_ratings WHERE id = ?";
        List<MpaRating> list = jdbc.query(sql, this::mpaMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    private Genre genreMapper(ResultSet rs, int rn) throws SQLException {
        Genre g = new Genre();
        g.setId(rs.getInt("id"));
        g.setName(rs.getString("name"));
        return g;
    }

    private MpaRating mpaMapper(ResultSet rs, int rn) throws SQLException {
        MpaRating m = new MpaRating();
        m.setId(rs.getInt("id"));
        m.setName(rs.getString("name"));
        return m;
    }
}