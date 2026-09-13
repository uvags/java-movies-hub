package ru.practicum.moviehub.http;

import com.google.gson.reflect.TypeToken;
import ru.practicum.moviehub.model.Movie;
import java.util.List;

// это для дессериализации списка, используем на стороне клиента тут (т.е. в тестах по сути)
public class ListOfMoviesTypeToken extends TypeToken<List<Movie>> {
}