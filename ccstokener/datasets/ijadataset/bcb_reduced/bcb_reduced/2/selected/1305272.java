package se.kb.fedora.oreprovider.datasource;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataSourceFactory {

    private static final String INFO_FEDORA_PREFIX = "info:fedora/";

    private static final Pattern SIMPLE_FIRSTLINE = Pattern.compile(".*<objectDatastreams.*pid=\"(\\S*)\".*>");

    private static final Pattern SIMPLE_LINE = Pattern.compile(".*<datastream.*dsid=\"(\\S*)\".*mimeType=\"(\\S*)\".*/>");

    private static final Pattern ANNOTATE_FIRSTLINE = Pattern.compile("\"id\",\"res\"");

    private static final Pattern ANNOTATE_LINE = Pattern.compile(INFO_FEDORA_PREFIX + "(.*),(.*)");

    private String baseurl;

    private Matcher matcher;

    public DataSourceFactory(String baseurl) {
        this.baseurl = baseurl;
    }

    public List<DataSource> createDataSources(List<String> lines) {
        List<DataSource> list = new LinkedList<DataSource>();
        String firstline = lines.get(0);
        matcher = SIMPLE_FIRSTLINE.matcher(firstline);
        if (matcher.matches()) {
            String id = matcher.group(1);
            for (String line : lines) {
                matcher = SIMPLE_LINE.matcher(line);
                if (matcher.matches()) {
                    DataSource source = new DataSource(baseurl, id, matcher.group(1));
                    source.setMimeType(matcher.group(2));
                    list.add(source);
                }
            }
        }
        matcher = ANNOTATE_FIRSTLINE.matcher(firstline);
        if (matcher.matches()) {
            for (String line : lines) {
                matcher = ANNOTATE_LINE.matcher(line);
                if (matcher.matches()) {
                    DataSource source = new DataSource(baseurl, matcher.group(1), matcher.group(2));
                    source.setMimeType(getMimeType(source.getUrl()));
                    list.add(source);
                }
            }
        }
        return list;
    }

    private String getMimeType(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            return connection.getContentType();
        } catch (IOException e) {
            return null;
        }
    }
}
