package org.bitdrive.file.movie.themoviedb.impl;

import org.bitdrive.file.movies.api.Movie;
import org.bitdrive.file.movies.api.MovieMetaDataService;
import org.bitdrive.file.movies.api.Movies;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class MovieDbFetcher implements MovieMetaDataService {

    private static final String API_BASE_URL = "http://api.themoviedb.org";

    private static final String API_SEARCH = "Movie.search";

    private static final String API_GET_INFO = "Movie.getInfo";

    private static final String API_GET_INFO_HASH = "Hash.getInfo";

    private static final String API_KEY = "1cb517ba4ae926edbae9cfe4c1ebe88d";

    private String getData(String method, String arg) {
        try {
            URL url;
            String str;
            StringBuilder strBuilder;
            BufferedReader stream;
            url = new URL(API_BASE_URL + "/2.1/" + method + "/en/xml/" + API_KEY + "/" + URLEncoder.encode(arg, "UTF-8"));
            stream = new BufferedReader(new InputStreamReader(url.openStream()));
            strBuilder = new StringBuilder();
            while ((str = stream.readLine()) != null) {
                strBuilder.append(str);
            }
            stream.close();
            return strBuilder.toString();
        } catch (MalformedURLException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    private Movie fetchMovie(String method, String arg) {
        String xmlStr;
        Movies movies;
        xmlStr = getData(method, arg);
        if (xmlStr == null) return null;
        movies = Movies.fromXML(xmlStr);
        if (movies.movies.size() < 1) return null;
        return movies.movies.getFirst();
    }

    public Movie get(int id) {
        return fetchMovie(API_GET_INFO, Integer.toString(id));
    }

    public Movie get(File file) {
        try {
            return fetchMovie(API_GET_INFO_HASH, OpenSubtitlesHasher.computeHash(file));
        } catch (IOException e) {
            return null;
        }
    }

    public Movie search(String title) {
        return fetchMovie(API_SEARCH, title);
    }
}
