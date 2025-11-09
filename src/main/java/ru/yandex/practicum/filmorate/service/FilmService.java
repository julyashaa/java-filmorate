package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationFilmException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.dal.storage.FilmStorage;
import ru.yandex.practicum.filmorate.dal.storage.UserStorage;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    public Collection<Film> findAll() {
        log.info("Запрошен список всех фильмов.");
        return filmStorage.findAll();
    }

    public Film create(Film film) {
        validateFilm(film);
        Film saved = filmStorage.add(film);
        log.info("Добавлен новый фильм id={}", saved.getId());
        return saved;
    }

    public Film update(Film film) {
        if (film.getId() == null || !filmStorage.existsById(film.getId())) {
            throw new NotFoundException("Фильм с таким id не найден или id не указан");
        }
        validateFilm(film);
        Film updated = filmStorage.update(film);
        log.info("Фильм обновлён id={}", updated.getId());
        return updated;
    }

    public Film getById(long id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id = " + id + " не найден"));
    }

    public void addLike(long filmId, long userId) {
        requireFilm(filmId);
        requireUserExists(userId);
        filmStorage.addLike(filmId, userId);
        log.info("Пользователь {} лайкнул фильм {}", userId, filmId);
    }

    public void removeLike(long filmId, long userId) {
        requireFilm(filmId);
        requireUserExists(userId);
        filmStorage.removeLike(filmId, userId);
        log.info("Пользователь {} убрал лайк с фильма {}", userId, filmId);
    }

    public List<Film> getPopular(int count) {
        return filmStorage.getPopular(count);
    }

    public List<Genre> getAllGenres() {
        return filmStorage.getAllGenres();
    }

    public Genre getGenreById(int id) {
        return filmStorage.getGenreById(id)
                .orElseThrow(() -> new NotFoundException("Жанр с id=" + id + " не найден"));
    }

    public List<MpaRating> getAllMpa() {
        return filmStorage.getAllMpa();
    }

    public MpaRating getMpaById(int id) {
        return filmStorage.getMpaById(id)
                .orElseThrow(() -> new NotFoundException("Рейтинг с id=" + id + " не найден"));
    }

    private void requireFilm(long id) {
        filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id = " + id + " не найден."));
    }

    private void requireUserExists(long id) {
        if (!userStorage.existsById(id)) {
            throw new NotFoundException("Пользователь с id = " + id + " не найден.");
        }
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            log.error("Ошибка валидации фильма: пустое название.");
            throw new ValidationFilmException("Название не может быть пустым.");
        }
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.error("Ошибка валидации фильма: слишком длинное описание ({} символов).",
                    film.getDescription().length());
            throw new ValidationFilmException("Описание должно содержать не более 200 символов.");
        }
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
            log.error("Ошибка валидации фильма: слишком ранняя дата релиза: {}.", film.getReleaseDate());
            throw new ValidationFilmException("Дата релиза фильма должна быть не раньше 28 декабря 1895 года.");
        }
        if (film.getDuration() != null && film.getDuration() <= 0) {
            log.error("Ошибка валидации фильма: некорректная продолжительность {}.", film.getDuration());
            throw new ValidationFilmException("Продолжительность фильма должна быть положительным числом.");
        }

        if (film.getMpa() == null || film.getMpa().getId() == null) {
            throw new ValidationFilmException("Не указан рейтинг (mpa.id).");
        }

        if (filmStorage.getMpaById(film.getMpa().getId()).isEmpty()) {
            throw new NotFoundException("Рейтинг с id=" + film.getMpa().getId() + " не найден.");
        }


        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            for (Genre g : film.getGenres()) {
                if (g == null || g.getId() == null) {
                    throw new ValidationFilmException("Каждый жанр должен содержать корректный id.");
                }
                if (filmStorage.getGenreById(g.getId()).isEmpty()) {
                    throw new NotFoundException("Жанр с id=" + g.getId() + " не найден");
                }
            }
        }
    }
}