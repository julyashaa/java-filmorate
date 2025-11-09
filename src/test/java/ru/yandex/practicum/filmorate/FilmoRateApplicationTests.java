package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.dal.FilmDbStorage;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({UserDbStorage.class, FilmDbStorage.class})
class FilmoRateApplicationTests {
	private final UserDbStorage userStorage;
	private final FilmDbStorage filmStorage;
	private final JdbcTemplate jdbc;

	@BeforeEach
	void setupDatabase() {
		jdbc.update("DELETE FROM film_likes");
		jdbc.update("DELETE FROM friendships");
		jdbc.update("DELETE FROM film_genres");
		jdbc.update("DELETE FROM films");
		jdbc.update("DELETE FROM users");
		jdbc.update("DELETE FROM mpa_ratings");

		jdbc.update("ALTER TABLE mpa_ratings ALTER COLUMN id RESTART WITH 1");
		jdbc.update("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");
		jdbc.update("ALTER TABLE films ALTER COLUMN id RESTART WITH 1");

		jdbc.update("INSERT INTO mpa_ratings (id, name) VALUES (1, 'G'), (2, 'PG')");
		jdbc.update("""
				INSERT INTO users (email, login, name, birthday) VALUES
				('u1@mail.com','u1','User One','1990-01-01'),
				('u2@mail.com','u2','User Two','1991-02-02')
				""");
		jdbc.update("""
				INSERT INTO films (name, description, release_date, duration, mpa_id)
				VALUES ('Matrix','Sci-fi','1999-03-31',136, 1)
				""");
	}

	@Test
	void testFindUserById() {
		Optional<User> userOptional = userStorage.findById(1L);

		assertThat(userOptional)
				.isPresent()
				.hasValueSatisfying(user ->
						assertThat(user).hasFieldOrPropertyWithValue("id", 1L)
								.hasFieldOrPropertyWithValue("email", "u1@mail.com"));
	}

	@Test
	void testAddUser() {
		User newUser = new User();
		newUser.setEmail("new@mail.com");
		newUser.setLogin("newbie");
		newUser.setName("New User");
		newUser.setBirthday(LocalDate.of(1999, 9, 9));

		User saved = userStorage.add(newUser);

		assertThat(saved.getId()).isNotNull();
		Optional<User> loaded = userStorage.findById(saved.getId());
		assertThat(loaded).isPresent();
		assertThat(loaded.get().getLogin()).isEqualTo("newbie");
	}

	@Test
	void testUpdateUser() {
		User user = userStorage.findById(1L).orElseThrow();
		user.setName("Updated Name");
		userStorage.update(user);

		User updated = userStorage.findById(1L).orElseThrow();
		assertThat(updated.getName()).isEqualTo("Updated Name");
	}

	@Test
	void testAddAndFindFilm() {
		Film film = new Film();
		film.setName("Inception");
		film.setDescription("Dream heist");
		film.setReleaseDate(LocalDate.of(2010, 7, 16));
		film.setDuration(148);
		MpaRating mpa = new MpaRating();
		mpa.setId(1);
		mpa.setName("G");
		film.setMpa(mpa);

		Film saved = filmStorage.add(film);
		assertThat(saved.getId()).isNotNull();

		Film loaded = filmStorage.findById(saved.getId()).orElseThrow();
		assertThat(loaded.getName()).isEqualTo("Inception");
		assertThat(loaded.getMpa().getName()).isEqualTo("G");
	}

	@Test
	void testUpdateFilm() {
		Film film = filmStorage.findById(1L).orElseThrow();
		film.setDescription("Neo saves humanity");
		filmStorage.update(film);

		Film updated = filmStorage.findById(1L).orElseThrow();
		assertThat(updated.getDescription()).isEqualTo("Neo saves humanity");
	}

	@Test
	void testGetAllFilms() {
		List<Film> films = List.copyOf(filmStorage.findAll());
		assertThat(films).isNotEmpty();
		assertThat(films.get(0).getName()).isEqualTo("Matrix");
	}
}