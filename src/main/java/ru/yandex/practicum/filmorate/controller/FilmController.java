package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.Collection;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class FilmController {

    private final FilmService filmService;

    @GetMapping("/films")
    public Collection<Film> findAll() {
        return filmService.findAll();
    }

    @GetMapping("/films/{id}")
    public Film getById(@PathVariable long id) {
        return filmService.getById(id);
    }

    @GetMapping("/films/popular")
    public Collection<Film> getPopular(@RequestParam(name = "count", defaultValue = "10") int count) {
        if (count <= 0) {
            throw new ValidationException("Параметр count должен быть положительным");
        }
        return filmService.getPopular(count);
    }

    @PostMapping("/films")
    public Film create(@RequestBody Film film) {
        return filmService.create(film);
    }

    @PutMapping("/films")
    public Film update(@RequestBody Film film) {
        return filmService.update(film);
    }

    @PutMapping("/films/{id}/like/{userId}")
    public void addLike(@PathVariable long id, @PathVariable long userId) {
        filmService.addLike(id, userId);
    }

    @DeleteMapping("/films/{id}/like/{userId}")
    public void removeLike(@PathVariable long id, @PathVariable long userId) {
        filmService.removeLike(id, userId);
    }

    @GetMapping("/genres")
    public List<Genre> allGenres() {
        return filmService.getAllGenres();
    }

    @GetMapping("/genres/{id}")
    public Genre genreById(@PathVariable int id) {
        return filmService.getGenreById(id);
    }

    @GetMapping("/mpa")
    public List<MpaRating> allMpa() {
        return filmService.getAllMpa();
    }

    @GetMapping("/mpa/{id}")
    public MpaRating mpaById(@PathVariable int id) {
        return filmService.getMpaById(id);
    }
}