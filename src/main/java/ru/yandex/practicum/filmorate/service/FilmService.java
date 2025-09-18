package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationFilmException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class FilmService {
    private final Map<Long, Film> films = new HashMap<>();
    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    public Collection<Film> findAll() {
        log.info("Запрошен список всех фильмов. Количество: {}", films.size());
        return films.values();
    }

    public Film create(Film film) {
        validateFilm(film);
        film.setId(getNextId());
        films.put(film.getId(), film);
        log.info("Добавлен новый фильм. {}", film);
        return film;
    }

    public Film update(Film film) {
        if (film.getId() == null || !films.containsKey(film.getId())) {
            log.error("Ошибка обновления фильма. Неверный id: {}", film.getId());
            throw new ValidationFilmException("Фильм с таким id не найден или id не указан");
        }
        validateFilm(film);
        films.put(film.getId(), film);
        log.info("Фильм c id {} был обновлен.", film.getId());
        return film;
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

    private long getNextId() {
        long currentMaxId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}