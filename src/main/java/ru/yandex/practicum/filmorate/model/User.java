package ru.yandex.practicum.filmorate.model;

import lombok.Data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
public class User {
    Long id;
    String email;
    String login;
    String name;
    LocalDate birthday;
    Set<Long> friends = new HashSet<>();
}