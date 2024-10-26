package org.jul.dcl.classpath;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jul.common.ExceptionHandler;
import org.jul.dcl.fetcher.ClassFetcher;

class HttpFinder extends ClassFinder {

    @Override
    protected List<ClassFetcher> find0(ExceptionHandler handler) {
        List<ClassFetcher> classes = new ArrayList<ClassFetcher>();
        try {
            URLConnection connection = url.openConnection();
            BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder buffer = new StringBuilder();
            List<String> lines = new LinkedList<String>();
            String inputline;
            try {
                while ((inputline = input.readLine()) != null) {
                    lines.add(inputline);
                    buffer.append(inputline);
                }
            } finally {
                input.close();
            }
            if (buffer.indexOf("<a ") != -1) {
                Pattern pattern = Pattern.compile("href=\"([^\"]*)\"");
                Matcher matcher = pattern.matcher(buffer);
                while (matcher.find()) {
                    String name = matcher.group(1);
                    if (name.indexOf(EXTENSION_SEPARATOR) != -1) {
                        try {
                            classes.addAll(getFinder(new URL(url, matcher.group(1))).find(handler));
                        } catch (MalformedURLException e) {
                            handler.handle(e);
                        }
                    }
                }
            } else {
                for (String line : lines) {
                    try {
                        classes.addAll(getFinder(new URL(url, line)).find(handler));
                    } catch (MalformedURLException e) {
                        handler.handle(e);
                    }
                }
            }
        } catch (IOException e) {
            handler.handle(e);
        }
        return classes;
    }

    HttpFinder(URL url) {
        super(url);
    }
}
