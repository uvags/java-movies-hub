package ru.practicum.moviehub.http;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;
import ru.practicum.moviehub.validator.MovieValidator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {

    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod(); // метод
        String path = ex.getRequestURI().getPath(); // путь

        // для /movies
        if ("/movies".equals(path)) {
            handleMovies(ex, method);
            return;
        }

        // для /movies/{id}
        if (path.startsWith("/movies/")) {
            handleMovieById(ex, method, path);
            return;
        }

        sendError(ex, 404, "404 Not Found"); // message упаковывается при отправке в json obj
    }

    // get, post /movies[?]
    private void handleMovies(HttpExchange ex, String method) throws IOException {
        switch (method) {
            case "GET":
                handleGetMovies(ex);
                break;
            case "POST":
                handlePostMovie(ex);
                break;
            default:
                sendError(ex, 405, "405 Method Not Allowed");
        }
    }

    private void handleGetMovies(HttpExchange ex) throws IOException {
        String query = ex.getRequestURI().getQuery();

        if (query == null) {
            sendJson(ex, 200, gson.toJson(store.getAll()));
            return;
        }

        // get /movies?year=int
        if (!query.startsWith("year=")) {
            sendError(ex, 400, "Некорректный параметр запроса — 'year'");
            return;
        }

        String value = query.substring("year=".length());
        int year;

        try {
            year = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            sendError(ex, 400, "Некорректный параметр запроса — 'year'");
            return;
        }

        List<Movie> movies = store.findByYear(year);
        sendJson(ex, 200, gson.toJson(movies));
    }


    private void handleMovieById(HttpExchange ex, String method, String path) throws IOException {
        // прочитали id фильма /movies/{id}, если пустая строка - исключение
        String temp = path.substring("/movies/".length());
        int movieId;

        try {
            movieId = Integer.parseInt(temp);
        } catch (NumberFormatException e) {
            sendError(ex, 400, "Некорректный ID");
            return;
        }

        switch (method) {
            case "GET":
                Optional<Movie> movie = store.findById(movieId);
                if (movie.isEmpty()) {
                    sendError(ex, 404, "Фильм не найден");
                    return;
                }
                sendJson(ex, 200, gson.toJson(movie.get()));
                break;
            case "DELETE":
                boolean removed = store.deleteById(movieId);
                if (!removed) {
                    sendError(ex, 404, "Фильм не найден");
                    return;
                }
                sendNoContent(ex);
                break;
            default:
                sendError(ex, 405, "Метод не поддерживается в настоящий момент");
        }
    }

    private void handlePostMovie(HttpExchange ex) throws IOException {
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");

        if (contentType == null) {
            sendError(ex, 415, "Неподдерживаемый Content-Type");
            return;
        }

        String normalized = contentType.toLowerCase();

        if (!normalized.startsWith("application/json")) {
            sendError(ex, 415, "Неподдерживаемый Content-Type");
            return;
        }

        if (normalized.contains("charset=") && !normalized.contains("charset=utf-8")) {

            sendError(ex, 415, "Поддерживается только UTF-8");
            return;
        }

        String body;

        try (InputStream input = ex.getRequestBody()) {
            body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        Movie movie;

        try {
            movie = gson.fromJson(body, Movie.class);
        } catch (JsonSyntaxException e) {
            sendError(ex, 400, "Некорректно составленный JSON");
            return;
        }

        // буквально null передали в теле запроса
        if (movie == null) {
            sendError(ex, 400, "Некорректно составленный JSON");
            return;
        }

        // исключаем случаи, когда не до конца заполнены данные или невалидные данные
        List<String> errList = MovieValidator.validate(movie);

        if (!errList.isEmpty()) {
            sendValidationError(ex, errList); // статус 422 Unprocessable Entity
            return;
        }

        sendJson(ex, 201, gson.toJson(store.add(movie)));
    }
}