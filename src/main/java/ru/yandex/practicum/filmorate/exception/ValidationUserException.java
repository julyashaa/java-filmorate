package ru.yandex.practicum.filmorate.exception;

public class ValidationUserException extends RuntimeException {
    public ValidationUserException(String message) {
        super(message);
    }
}