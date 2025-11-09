package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.dal.sql.FilmSqlQuery;
import ru.yandex.practicum.filmorate.dal.sql.GenreSqlQuery;
import ru.yandex.practicum.filmorate.dal.sql.MpaSqlQuery;
import ru.yandex.practicum.filmorate.dal.storage.FilmStorage;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository("filmDbStorage")
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {
    private final NamedParameterJdbcTemplate jdbc;
    private final FilmRowMapper filmRowMapper = new FilmRowMapper();

    @Override
    public Collection<Film> findAll() {
        List<Film> films = jdbc.getJdbcTemplate().query(FilmSqlQuery.FIND_ALL.getSql(), filmRowMapper);
        setGenresToFilms(films);
        return films;
    }

    @Override
    public Optional<Film> findById(Long id) {
        List<Film> list = jdbc.query(FilmSqlQuery.FIND_BY_ID.getSql(), Map.of("id", id), filmRowMapper);
        if (list.isEmpty()) return Optional.empty();
        Film film = list.get(0);
        film.setGenres(getGenres(id));
        film.setLikes(loadLikeUserIds(id));
        return Optional.of(film);
    }

    @Override
    public Film add(Film film) {
        MapSqlParameterSource ps = new MapSqlParameterSource()
                .addValue("name", film.getName())
                .addValue("description", film.getDescription())
                .addValue("releaseDate", film.getReleaseDate())
                .addValue("duration", film.getDuration())
                .addValue("mpaId", film.getMpa() != null ? film.getMpa().getId() : null);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(FilmSqlQuery.INSERT.getSql(), ps, keyHolder, new String[]{"id"});
        film.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            List<Integer> ids = film.getGenres().stream()
                    .map(Genre::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            MapSqlParameterSource[] batch = ids.stream()
                    .map(gid -> new MapSqlParameterSource()
                            .addValue("filmId", film.getId())
                            .addValue("genreId", gid))
                    .toArray(MapSqlParameterSource[]::new);

            jdbc.batchUpdate(
                    FilmSqlQuery.INSERT_FILM_GENRE.getSql(),
                    batch
            );
        }
        film.setGenres(getGenres(film.getId()));
        return film;
    }

    @Override
    public Film update(Film film) {
        MapSqlParameterSource ps = new MapSqlParameterSource()
                .addValue("name", film.getName())
                .addValue("description", film.getDescription())
                .addValue("releaseDate", film.getReleaseDate())
                .addValue("duration", film.getDuration())
                .addValue("mpaId", film.getMpa() != null ? film.getMpa().getId() : null)
                .addValue("id", film.getId());

        jdbc.update(FilmSqlQuery.UPDATE.getSql(), ps);

        if (film.getGenres() != null) {
            jdbc.update(FilmSqlQuery.DELETE_FILM_GENRES.getSql(), Map.of("id", film.getId()));
            if (!film.getGenres().isEmpty()) {
                List<Integer> ids = film.getGenres().stream()
                        .map(Genre::getId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

                MapSqlParameterSource[] batch = ids.stream()
                        .map(gid -> new MapSqlParameterSource()
                                .addValue("filmId", film.getId())
                                .addValue("genreId", gid))
                        .toArray(MapSqlParameterSource[]::new);

                jdbc.batchUpdate(
                        FilmSqlQuery.INSERT_FILM_GENRE.getSql(),
                        batch
                );
            }
        }

        film.setGenres(getGenres(film.getId()));
        return film;
    }

    @Override
    public void deleteById(Long id) {
        jdbc.update(FilmSqlQuery.DELETE_BY_ID.getSql(), Map.of("id", id));
    }

    @Override
    public boolean existsById(Long id) {
        Long count = jdbc.queryForObject(
                FilmSqlQuery.EXISTS_BY_ID.getSql(),
                Map.of("id", id),
                Long.class
        );
        return count != null && count > 0;
    }

    @Override
    public void addLike(long filmId, long userId) {
        jdbc.update(FilmSqlQuery.ADD_LIKE.getSql(),
                Map.of("filmId", filmId, "userId", userId));
    }

    @Override
    public void removeLike(long filmId, long userId) {
        jdbc.update(FilmSqlQuery.REMOVE_LIKE.getSql(),
                Map.of("filmId", filmId, "userId", userId));
    }

    private Set<Long> loadLikeUserIds(long filmId) {
        return new HashSet<>(jdbc.query(FilmSqlQuery.LOAD_LIKE_USER_IDS.getSql(),
                Map.of("filmId", filmId),
                (rs, rn) -> rs.getLong("user_id")));
    }

    @Override
    public List<Film> getPopular(int count) {
        List<Film> films = jdbc.query(FilmSqlQuery.POPULAR.getSql(), Map.of("count", count), filmRowMapper);
        setGenresToFilms(films);
        return films;
    }

    @Override
    public List<Genre> getAllGenres() {
        return jdbc.getJdbcTemplate().query(GenreSqlQuery.FIND_ALL_GENRE.getSql(), this::genreMapper);
    }

    @Override
    public Optional<Genre> getGenreById(int id) {
        List<Genre> list = jdbc.query(GenreSqlQuery.FIND_GENRE_BY_ID.getSql(), Map.of("id", id), this::genreMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Set<Genre> getGenres(long filmId) {
        List<Genre> list = jdbc.query(FilmSqlQuery.GENRES_BY_FILM_ID.getSql(), Map.of("filmId", filmId), this::genreMapper);
        return new LinkedHashSet<>(list);
    }


    private void setGenresToFilms(List<Film> films) {
        if (films == null || films.isEmpty()) return;

        films.forEach(f -> f.setGenres(new HashSet<>()));

        List<Long> filmIds = films.stream().map(Film::getId).toList();


        Map<Long, Set<Genre>> map = new HashMap<>();
        jdbc.query(FilmSqlQuery.GENRES_FOR_FILM_LIST.getSql(), Map.of("ids", filmIds), rs -> {
            long filmId = rs.getLong("film_id");
            int genreId = rs.getInt("genre_id");
            String name = rs.getString("name");
            Genre genre = new Genre();
            genre.setId(genreId);
            genre.setName(name);
            map.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(genre);
        });

        for (Film f : films) {
            f.setGenres(map.getOrDefault(f.getId(), Collections.emptySet()));
        }
    }

    @Override
    public List<MpaRating> getAllMpa() {
        return jdbc.getJdbcTemplate().query(MpaSqlQuery.FIND_ALL_MPA.getSql(), this::mpaMapper);
    }

    @Override
    public Optional<MpaRating> getMpaById(int id) {
        List<MpaRating> list = jdbc.query(MpaSqlQuery.FIND_MPA_BY_ID.getSql(), Map.of("id", id), this::mpaMapper);
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