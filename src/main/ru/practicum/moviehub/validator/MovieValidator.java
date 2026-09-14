package ru.practicum.moviehub.validator;

import ru.practicum.moviehub.model.Movie;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class MovieValidator {
    public static List<String> validate(Movie movie) {
        String title = movie.getTitle();
        List<String> errList = new ArrayList<>();

        if (title == null || title.trim().isEmpty()) {
            errList.add("название не должно быть пустым");
        } else if (title.length() > 100) {
            errList.add("название не может быть больше 100 знаков");
        }

        int maxYear = Year.now().getValue() + 1;

        if (movie.getYear() < 1888 || movie.getYear() > maxYear) {
            errList.add("год должен быть между 1888 и " + maxYear);
        }

        return errList;
    }
}
