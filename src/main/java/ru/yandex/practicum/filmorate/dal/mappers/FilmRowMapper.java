package ru.yandex.practicum.filmorate.dal.mappers;

import org.springframework.jdbc.core.RowMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;

public class FilmRowMapper implements RowMapper<Film> {
    @Override
    public Film mapRow(ResultSet rs, int rowNum) throws SQLException {
        Film film = new Film();
        film.setId(rs.getLong("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setDuration((Integer) rs.getObject("duration"));
        if (rs.getDate("release_date") != null) {
            film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        }
        Integer ratingId = (Integer) rs.getObject("mpa_id");
        if (ratingId != null) {
            MpaRating mpa = new MpaRating();
            mpa.setId(ratingId);
            mpa.setName(rs.getString("mpa_name"));
            film.setMpa(mpa);
        }

        film.setLikes(new HashSet<>());
        film.setGenres(new HashSet<>());
        return film;
    }
}
