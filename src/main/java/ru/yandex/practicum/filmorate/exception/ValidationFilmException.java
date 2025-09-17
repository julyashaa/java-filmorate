package ru.yandex.practicum.filmorate.exception;

public class ValidationFilmException extends RuntimeException {
    public ValidationFilmException(String message) {
        super(message);
    }
}