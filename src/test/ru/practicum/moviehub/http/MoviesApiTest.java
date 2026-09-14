package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.store.MoviesStore;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Year;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore store;
    private static final Gson gson = new Gson();

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, 8080);
        server.start();
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void status405WhenMethodNotImplemented() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8));

        assertEquals(405, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("405 Method Not Allowed", error.getError());
    }

    @Test
    void deleteStatus400WhenIdNotNumber() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/notnumber"))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8));

        assertEquals(400, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Некорректный ID", error.getError());
    }

    @Test
    void postStatus422WhenMovieYearLessThan1888() throws Exception {
        String json = "{\"title\":\"Shrek\",\"year\":1887}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8));

        assertEquals(422, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", error.getError());
        assertNotNull(error.getDetails());
        assertTrue(error.getDetails().contains(
                "год должен быть между 1888 и " + (Year.now().getValue() + 1)));
    }

    @Test
    void emptyArrayStatus200WhenEmptyStore() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode());
        assertEquals("application/json; charset=UTF-8", response.headers()
                .firstValue("Content-Type")
                .orElse(""));
        List<Movie> movies = gson.fromJson(
                response.body(), new ListOfMoviesTypeToken().getType());
        assertNotNull(movies);
        assertTrue(movies.isEmpty());
    }

    @Test
    void getMoviesStatus200WhenStoreIsNotEmpty() throws Exception {
        store.add(new Movie("Derevanniy Bolvan", 1994));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode());
        List<Movie> movies = gson.fromJson(
                response.body(), new ListOfMoviesTypeToken().getType());
        assertNotNull(movies);
        assertEquals(1, movies.size());
        assertEquals("Derevanniy Bolvan", movies.getFirst().getTitle());
        assertEquals(1994, movies.getFirst().getYear());
    }

    @Test
    void postStatus201WhenValidMovie() throws Exception {
        String json = "{\"title\":\"Shrek\",\"year\":2001}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8));

        assertEquals(201, response.statusCode());
        Movie posted = gson.fromJson(response.body(), Movie.class);
        assertNotNull(posted);
        assertTrue(posted.getId() > 0);
        assertEquals("Shrek", posted.getTitle());
        assertEquals(2001, posted.getYear());
    }

    @Test
    void postStatus422WhenTitleMoreThan100Letters() throws Exception {
        String title = "s".repeat(101);
        String json = String.format("{\"title\":\"%s\",\"year\":2001}", title);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8));
        assertEquals(422, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", error.getError());
        assertNotNull(error.getDetails());
        assertTrue(error.getDetails().contains(
                "название не может быть больше 100 знаков"));
    }

    @Test
    void postStatus400WhenBadJsonRequestBody() throws Exception {
        String json = "{\"title\",\"year\":2001}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8));

        assertEquals(400, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Некорректно составленный JSON", error.getError());
    }

    @Test
    void postStatus422WhenInvalidMovieData() throws Exception {
        String json = "{\"title\":\"\",\"year\":2001}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8));

        assertEquals(422, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", error.getError());
        assertNotNull(error.getDetails());
        assertTrue(error.getDetails().contains(
                "название не должно быть пустым"));
    }

    @Test
    void postStatus422WhenInvalidYearOfMovie() throws Exception {
        int invalidYear = Year.now().getValue() + 2;
        String json = String.format("{\"title\":\"Shrek\",\"year\":%d}", invalidYear);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8));

        assertEquals(422, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", error.getError());
        assertNotNull(error.getDetails());
        assertTrue(error.getDetails().contains(
                "год должен быть между 1888 и " + (Year.now().getValue() + 1)));
    }

    @Test
    void postStatus415WhenBadContentTypeHeader() throws Exception {
        String json = "{\"title\":\"Shrek\",\"year\":2001}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8));

        assertEquals(415, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Неподдерживаемый Content-Type", error.getError());
    }

    @Test
    void status400WhenInvalidId() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/neparsitsavchislo"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8));

        assertEquals(400, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Некорректный ID", error.getError());
    }

    @Test
    void getMovieStatus200WhenMovieExists() throws Exception {
        Movie movie = store.add(new Movie("Shrek", 2001));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + movie.getId()))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode());
        Movie received = gson.fromJson(response.body(), Movie.class);
        assertNotNull(received);
        assertEquals(movie.getId(), received.getId());
        assertEquals("Shrek", received.getTitle());
        assertEquals(2001, received.getYear());
    }

    @Test
    void getMovieStatus404WhenMovieIdNotExists() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/5"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8));

        assertEquals(404, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Фильм не найден", error.getError());
    }

    @Test
    void deleteStatus204WhenValidMovieId() throws Exception {
        Movie movie = store.add(new Movie("Shrek", 2001));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + movie.getId()))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8));

        assertEquals(204, response.statusCode());
        assertTrue(store.findById(movie.getId()).isEmpty());
    }

    @Test
    void deleteStatus404WhenIdDoesNotExist() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/10"))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8));

        assertEquals(404, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Фильм не найден", error.getError());
    }

    @Test
    void getMoviesStatus200WhenYearMatchesMovies() throws Exception {
        store.add(new Movie("Cinema", 2000));
        store.add(new Movie("Movie", 2017));
        store.add(new Movie("Masterpiece", 2017));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2017"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode());
        List<Movie> movies = gson.fromJson(
                response.body(), new ListOfMoviesTypeToken().getType());
        assertNotNull(movies);
        assertEquals(2, movies.size());
        assertTrue(movies.stream().anyMatch(movie -> movie.getTitle().equals("Movie")));
        assertTrue(movies.stream().anyMatch(movie -> movie.getTitle().equals("Masterpiece")));
        assertTrue(movies.stream().allMatch(movie -> movie.getYear() == 2017));
    }

    @Test
    void emptyMovieListStatus200WhenYearDoesNotMatchMovies() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2005"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode());
        List<Movie> movies = gson.fromJson(
                response.body(), new ListOfMoviesTypeToken().getType());
        assertNotNull(movies);
        assertTrue(movies.isEmpty());
    }

    @Test
    void status400InvalidMovieYear() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=tryparsemetonumber"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8));

        assertEquals(400, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Некорректный параметр запроса — 'year'", error.getError());
    }
}