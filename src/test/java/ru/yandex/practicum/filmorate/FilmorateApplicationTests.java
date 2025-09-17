package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.ValidationFilmException;
import ru.yandex.practicum.filmorate.exception.ValidationUserException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FilmorateApplicationTests {
	private FilmController films;
	private UserController users;

	@BeforeEach
	void setUp() {
		films = new FilmController();
		users = new UserController();
	}

	@Test
	void contextLoads() {
	}

	private Film validFilm() {
		Film film = new Film();
		film.setName("Название фильма");
		film.setDescription("Описание фильма");
		film.setReleaseDate(LocalDate.of(2016, 10, 10));
		film.setDuration(160);
		return film;
	}

	@Test
	void filmCreateOkOnBoundaryDescription200() {
		Film film = validFilm();
		film.setDescription("a".repeat(200));
		assertDoesNotThrow(() -> films.create(film));
	}

	@Test
	void filmCreateFailOnDescription201() {
		Film film = validFilm();
		film.setDescription("a".repeat(201));
		assertThrows(ValidationFilmException.class, () -> films.create(film));
	}

	@Test
	void filmCreateFailOnEmptyName() {
		Film film = validFilm();
		film.setName("   ");
		assertThrows(ValidationFilmException.class, () -> films.create(film));
	}

	@Test
	void filmCreateValidatesReleaseDateBoundary() {
		Film ok = validFilm();
		ok.setReleaseDate(LocalDate.of(1895, 12, 28));
		assertDoesNotThrow(() -> films.create(ok));

		Film bad = validFilm();
		bad.setReleaseDate(LocalDate.of(1895, 12, 27));
		assertThrows(ValidationFilmException.class, () -> films.create(bad));
	}

	@Test
	void filmCreateFailOnZeroOrNegativeDuration() {
		Film zero = validFilm();
		zero.setDuration(0);
		assertThrows(ValidationFilmException.class, () -> films.create(zero));

		Film negative = validFilm();
		negative.setDuration(-1);
		assertThrows(ValidationFilmException.class, () -> films.create(negative));
	}

	private User validUser() {
		User user = new User();
		user.setEmail("user@example.com");
		user.setLogin("user111");
		user.setName("Имя пользователя");
		user.setBirthday(LocalDate.of(1990, 10, 10));
		return user;
	}

	@Test
	void userCreateFailOnEmailMissing_orNoAt() {
		User blank = validUser();
		blank.setEmail("   ");
		assertThrows(ValidationUserException.class, () -> users.create(blank));

		User noAt = validUser();
		noAt.setEmail("not-an-email");
		assertThrows(ValidationUserException.class, () -> users.create(noAt));
	}

	@Test
	void userCreateFailedOnLoginBlankOrWithSpace() {
		User blank = validUser();
		blank.setLogin("");
		assertThrows(ValidationUserException.class, () -> users.create(blank));

		User spaced = validUser();
		spaced.setEmail("with space");
		assertThrows(ValidationUserException.class, () -> users.create(spaced));
	}

	@Test
	void userCreateSetsNameToLoginWithNameBlank() {
		User blankName = validUser();
		blankName.setName("");
		User saved = users.create(blankName);
		assertEquals(blankName.getLogin(), saved.getName());
	}

	@Test
	void userUpdateFailWithMissingOrUnknownId() {
		User created = users.create(validUser());

		User noId = validUser();
		assertThrows(ValidationUserException.class, () -> users.update(noId));

		User unknown = validUser();
		unknown.setId(created.getId() + 999);
		assertThrows(ValidationUserException.class, () -> users.update(unknown));
	}
}