package com.m4f.utils.link.impl;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.http.json.JsonHttpParser;
import com.google.api.client.util.GenericData;
import com.google.api.client.util.Key;
import com.m4f.utils.link.ifc.URLShortener;
import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.json.jackson.JacksonFactory;

public class GoogleURLShortener implements URLShortener {

    public static final String GOOGL_URL = "https://www.googleapis.com/urlshortener/v1/url?key=AIzaSyD9h-WfnUUEecGjB6ovQLySg_zmquYUHJA";

    private static final Logger LOGGER = Logger.getLogger(GoogleURLShortener.class.getName());

    @Override
    public String shortURL(String url) throws NoSuchAlgorithmException, IOException {
        HttpTransport transport = new NetHttpTransport();
        GoogleHeaders headers = new GoogleHeaders();
        headers.setApplicationName("Hirubila/1.0");
        headers.put("Content-Type", "application/json");
        transport.defaultHeaders = headers;
        JsonHttpParser parser = new JsonHttpParser();
        parser.jsonFactory = new JacksonFactory();
        transport.addParser(parser);
        HttpRequest request = transport.buildPostRequest();
        request.setUrl(GOOGL_URL);
        GenericData data = new GenericData();
        data.put("longUrl", url);
        JsonHttpContent content = new JsonHttpContent();
        content.data = data;
        content.jsonFactory = parser.jsonFactory;
        request.content = content;
        HttpResponse response = request.execute();
        Result result = response.parseAs(Result.class);
        return result.id;
    }

    public static class Result extends GenericJson {

        @Key
        public String id;
    }
}
