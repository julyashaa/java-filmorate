package ru.yandex.practicum.filmorate.model;

import lombok.Data;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Data
public class User {
    Long id;
    String email;
    String login;
    String name;
    LocalDate birthday;
    private Map<Long, FriendshipStatus> friends = new HashMap<>();
}