package ru.yandex.practicum.filmorate.model;

import lombok.Data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Film.
 */
@Data
public class Film {
    Long id;
    String name;
    String description;
    LocalDate releaseDate;
    Integer duration;
    Set<Long> likes = new HashSet<>();
    private Set<Genre> genres = new HashSet<>();
    private MpaRating mpa;
}