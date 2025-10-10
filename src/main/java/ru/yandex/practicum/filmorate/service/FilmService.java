package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationFilmException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    public Collection<Film> findAll() {
        log.info("Запрошен список всех фильмов. Количество: {}", filmStorage.findAll().size());
        return filmStorage.findAll();
    }

    public Film create(Film film) {
        validateFilm(film);
        Film saved = filmStorage.add(film);
        log.info("Добавлен новый фильм. {}", saved);
        return saved;
    }

    public Film update(Film film) {
        if (film.getId() == null || !filmStorage.existsById(film.getId())) {
            log.error("Ошибка обновления фильма. Неверный id: {}", film.getId());
            throw new NotFoundException("Фильм с таким id не найден или id не указан");
        }
        validateFilm(film);
        Film updated = filmStorage.update(film);
        log.info("Фильм c id {} был обновлен.", film.getId());
        return updated;
    }

    public Film getById(long id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id = " + id + " не найден"));
    }

    public void addLike(long filmId, long userId) {
        Film film = requireFilm(filmId);
        requireUserExists(userId);
        film.getLikes().add(userId);
        filmStorage.update(film);
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void removeLike(long filmId, long userId) {
        Film film = requireFilm(filmId);
        requireUserExists(userId);
        film.getLikes().remove(userId);
        filmStorage.update(film);
        log.info("Пользователь {} убрал лайк с фильма {}", userId, filmId);
    }

    public List<Film> getPopular(int count) {
        return filmStorage.findAll()
                .stream()
                .sorted(Comparator
                        .comparingInt((Film film) -> film.getLikes().size())
                        .reversed()
                        .thenComparing(Film::getId))
                .limit(count)
                .toList();
    }

    private Film requireFilm(long id) {
        return filmStorage.findById(id)
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
    }
}