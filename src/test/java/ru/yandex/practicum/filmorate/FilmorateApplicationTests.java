package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationFilmException;
import ru.yandex.practicum.filmorate.exception.ValidationUserException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.InMemoryUserStorage;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FilmorateApplicationTests {
	private FilmService films;
	private UserService users;

	@BeforeEach
	void setUp() {
		InMemoryUserStorage userStorage = new InMemoryUserStorage();
		InMemoryFilmStorage filmStorage = new InMemoryFilmStorage();
		films = new FilmService(filmStorage, userStorage);
		users = new UserService(userStorage);
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
	void userCreateFailOnEmailMissingOrNoAt() {
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
		assertThrows(NotFoundException.class, () -> users.update(noId));

		User unknown = validUser();
		unknown.setId(created.getId() + 999);
		assertThrows(NotFoundException.class, () -> users.update(unknown));
	}

	@Test
	void addRemoveAndCommonFriends() {
		User u1 = users.create(validUser());
		User u2 = users.create(validUser());
		User u3 = users.create(validUser());

		users.addFriend(u1.getId(), u2.getId());
		users.addFriend(u2.getId(), u1.getId());

		users.addFriend(u1.getId(), u3.getId());
		users.addFriend(u3.getId(), u1.getId());

		users.addFriend(u2.getId(), u3.getId());
		users.addFriend(u3.getId(), u2.getId());

		List<User> friendsU1 = users.listFriends(u1.getId());
		assertEquals(2, friendsU1.size());

		List<User> common = users.commonFriends(u1.getId(), u2.getId());
		assertEquals(1, common.size());
		assertEquals(u3.getId(), common.getFirst().getId());

		users.removeFriend(u1.getId(), u2.getId());
		List<User> friendsU1After = users.listFriends(u1.getId());
		assertEquals(1, friendsU1After.size());
		assertEquals(u3.getId(), friendsU1After.getFirst().getId());
	}

	@Test
	void addRemoveLikeAndPopularFilms() {
		User u1 = users.create(validUser());
		User u2 = users.create(validUser());
		User u3 = users.create(validUser());

		Film f1 = films.create(validFilm());
		Film f2 = films.create(validFilm());
		Film f3 = films.create(validFilm());

		films.addLike(f1.getId(), u1.getId());
		films.addLike(f1.getId(), u2.getId());
		films.addLike(f2.getId(), u3.getId());

		List<Film> topFilms = films.getPopular(2);
		assertEquals(2, topFilms.size());
		assertEquals(f1.getId(), topFilms.getFirst().getId());
		assertEquals(f2.getId(), topFilms.get(1).getId());

		films.removeLike(f1.getId(), u2.getId());
		topFilms = films.getPopular(2);
		assertEquals(f1.getId(), topFilms.getFirst().getId());
		assertEquals(f2.getId(), topFilms.get(1).getId());
	}

	@Test
	void popularDefaultLimitIs10() {
		for (int i = 0; i < 12; i++) {
			users.create(validUser());
		}
		for (int i = 0; i < 15; i++) {
			films.create(validFilm());
		}
		for (long i = 1; i <= 12; i++) {
			films.addLike(i, i);
		}
		List<Film> top = films.getPopular(10);
		assertEquals(10, top.size());
	}
}
