package fr.cohen.image.ftp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import fr.cohen.image.ImageRepository;

public class ImageRepositoryFtpProxy extends AbstractImageRepository implements ImageRepository {

    private static ImageRepository instance = null;

    private ImageRepositoryFtpProxy() {
        this.pattern = Pattern.compile("<A HREF=\"[^\"]+\">([^.]+\\.(?:jpg|gif|png|bmp|JPG|GIF|PNG|BMP))</A>$");
    }

    public static ImageRepository getInstance() {
        if (instance == null) {
            instance = new ImageRepositoryFtpProxy();
        }
        return instance;
    }

    protected List<String> refresh(URL url) throws IOException {
        List<String> images = new ArrayList<String>(500);
        URLConnection connection = url.openConnection(proxy);
        InputStream stream = connection.getInputStream();
        Reader reader = new InputStreamReader(stream);
        BufferedReader buffer = new BufferedReader(reader);
        String line = null;
        while ((line = buffer.readLine()) != null) {
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                images.add(matcher.group(1));
            }
            matcher = null;
        }
        buffer.close();
        reader.close();
        stream.close();
        line = null;
        buffer = null;
        reader = null;
        stream = null;
        connection = null;
        return images;
    }
}
