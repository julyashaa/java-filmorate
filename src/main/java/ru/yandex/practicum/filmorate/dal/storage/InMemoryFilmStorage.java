package ru.yandex.practicum.filmorate.dal.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.*;
import java.util.stream.Collectors;

@Component("inMemoryFilmStorage")
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> films = new HashMap<>();

    @Override
    public Collection<Film> findAll() {
        return new ArrayList<>(films.values());
    }

    @Override
    public Optional<Film> findById(Long id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public Film add(Film film) {
        film.setId(getNextId());
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Film update(Film film) {
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public void deleteById(Long id) {
        films.remove(id);
    }

    @Override
    public boolean existsById(Long id) {
        return films.containsKey(id);
    }

    @Override
    public void addLike(long filmId, long userId) {
        Film film = films.get(filmId);
        if (film != null) {
            film.getLikes().add(userId);
        }
    }

    @Override
    public void removeLike(long filmId, long userId) {
        Film film = films.get(filmId);
        if (film != null) {
            film.getLikes().remove(userId);
        }
    }

    @Override
    public List<Film> getPopular(int count) {
        return films.values().stream()
                .sorted(Comparator
                        .comparingInt((Film film) -> film.getLikes().size())
                        .reversed()
                        .thenComparing(Film::getId))
                .limit(count)
                .collect(Collectors.toList());
    }

    @Override
    public List<Genre> getAllGenres() {
        return films.values().stream()
                .filter(Objects::nonNull)
                .flatMap(f -> Optional.ofNullable(f.getGenres()).orElseGet(Set::of).stream())
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                Genre::getId,
                                g -> g,
                                (a, b) -> a,
                                LinkedHashMap::new
                        ),
                        m -> new ArrayList<>(m.values())
                ));
    }

    @Override
    public Optional<Genre> getGenreById(int id) {
        return getAllGenres().stream()
                .filter(g -> Objects.equals(g.getId(), id))
                .findFirst();
    }

    @Override
    public Set<Genre> getGenres(long filmId) {
        Film film = films.get(filmId);
        return film != null && film.getGenres() != null
                ? new LinkedHashSet<>(film.getGenres())
                : new LinkedHashSet<>();
    }

    @Override
    public List<MpaRating> getAllMpa() {
        // агрегируем уникальные рейтинги из всех фильмов
        return films.values().stream()
                .map(Film::getMpa)
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                MpaRating::getId,
                                m -> m,
                                (a, b) -> a,
                                LinkedHashMap::new
                        ),
                        m -> new ArrayList<>(m.values())
                ));
    }

    @Override
    public Optional<MpaRating> getMpaById(int id) {
        return getAllMpa().stream()
                .filter(m -> Objects.equals(m.getId(), id))
                .findFirst();
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
