package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

public class MoviesStore {

    private int movieId = 1;
    private final Map<Integer, Movie> movies = new HashMap<>();

    public Movie add(Movie movie) {
        movie.setId(movieId);
        ++movieId;
        movies.put(movie.getId(), movie);

        return movie;
    }

    public Optional<Movie> findById(int id) {
        return Optional.ofNullable(movies.get(id));
    }


    public List<Movie> findByYear(int year) {
        return movies
                .values()
                .stream()
                .filter(movie -> movie.getYear() == year)
                .toList();
    }

    public boolean deleteById(int id) {
        return movies.remove(id) != null;
    }

    public List<Movie> getAll() {
        return new ArrayList<>(movies.values());
    }

    public void clear() {
        movies.clear();
        movieId = 1;
    }
}