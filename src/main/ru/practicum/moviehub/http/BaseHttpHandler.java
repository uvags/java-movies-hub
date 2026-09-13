package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8"; // !!! Укажите содержимое заголовка Content-Type
    protected final Gson gson = new Gson();

    protected void sendJson(HttpExchange ex, int status, String json) throws IOException {
        // !!! Реализуйте общий для всех хендлеров метод
        // для отправки ответа с телом в формате JSON
        byte[] body = json.getBytes(StandardCharsets.UTF_8);

        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(status, body.length);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    protected void sendNoContent(HttpExchange ex) throws java.io.IOException {
        // !!! Реализуйте общий для всех хендлеров метод
        // для отправки ответа без тела и кодом 204
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(204, -1);
        ex.close();
    }

    protected void sendError(HttpExchange ex, int status, String message) throws IOException {
        ErrorResponse error = new ErrorResponse(message);
        sendJson(ex, status, gson.toJson(error));
    }

    // для ошибок валидации, где несколько полей могут быть некорректными
    protected void sendValidationError(HttpExchange ex, List<String> details) throws IOException {
        ErrorResponse error = new ErrorResponse("Ошибка валидации", details);
        sendJson(ex, 422, gson.toJson(error));
    }


}