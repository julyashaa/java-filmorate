package ru.yandex.practicum.filmorate.dal.storage;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FilmStorage {
    Collection<Film> findAll();

    Optional<Film> findById(Long id);

    Film add(Film film);

    Film update(Film film);

    void deleteById(Long id);

    boolean existsById(Long id);

    void addLike(long filmId, long userId);

    void removeLike(long filmId, long userId);

    List<Film> getPopular(int count);

    List<Genre> getAllGenres();

    Optional<Genre> getGenreById(int id);

    Set<Genre> getGenres(long filmId);

    List<MpaRating> getAllMpa();

    Optional<MpaRating> getMpaById(int id);
}
