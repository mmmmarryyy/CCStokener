package ch.oblivion.comix.model;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

/**
 * ComixPageBuilder builds a ComixPage for a given URL.
 * @author mima
 */
public class ComixPageBuilder {

    private static final Logger logger = Logger.getLogger(ComixPageBuilder.class);

    private final URL url;

    private Pattern imagePattern;

    private Pattern nextPagePattern;

    private Pattern titlePattern;

    private Pattern descriptionPattern;

    private URL cacheLocation;

    public ComixPageBuilder(URL url) {
        this.url = url;
    }

    public ComixPage buildPage() {
        ComixPage page = new ComixPage(url);
        try {
            InputStream pageStream = (InputStream) url.getContent();
            String pageContent = streamToString(pageStream);
            pageStream.close();
            if (imagePattern != null) {
                String value = find(imagePattern, pageContent);
                page.setImageUrl(new URL(url, value));
            }
            if (nextPagePattern != null) {
                String value = find(nextPagePattern, pageContent);
                page.setNextPageUrl(new URL(url, value));
            }
            if (titlePattern != null) {
                String value = find(titlePattern, pageContent);
                page.setTitle(value);
            }
            if (descriptionPattern != null) {
                String value = find(descriptionPattern, pageContent);
                page.setDescription(value);
            }
            if (cacheLocation != null) {
                InputStream input = null;
                OutputStream output = null;
                try {
                    URL cacheUrl = new URL(cacheLocation.toExternalForm() + "/" + page.getFileName());
                    URLConnection urlConnection = page.getImageUrl().openConnection();
                    File file = new File(cacheUrl.toURI());
                    urlConnection.connect();
                    input = new DataInputStream(urlConnection.getInputStream());
                    output = new FileOutputStream(file);
                    byte buf[] = new byte[1024];
                    int len;
                    while ((len = input.read(buf)) > 0) {
                        output.write(buf, 0, len);
                    }
                    page.setCachedImageUrl(cacheUrl);
                } catch (IOException e) {
                    logger.error("Could not cache image for page " + url, e);
                } catch (URISyntaxException e) {
                    logger.error("Could not cache image for page " + url, e);
                } finally {
                    closeStream(input);
                    closeStream(output);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return page;
    }

    private void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
                stream = null;
            } catch (IOException e) {
                logger.error("Could not close stream ", e);
            }
        }
    }

    private String find(Pattern pattern, String content) {
        if (pattern != null) {
            Matcher matcher = pattern.matcher(content);
            if (matcher.find() && matcher.groupCount() >= 1) {
                return matcher.group(1);
            }
        }
        return null;
    }

    /**
	 * Convert the InputStream to String
	 * @param is input stream to convert.
	 * @return String representation of input stream
	 */
    public String streamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public void setImagePattern(Pattern pattern) {
        imagePattern = pattern;
    }

    public void setNextPagePattern(Pattern pattern) {
        nextPagePattern = pattern;
    }

    public void setTitlePattern(Pattern pattern) {
        titlePattern = pattern;
    }

    public void setDescriptionPattern(Pattern pattern) {
        descriptionPattern = pattern;
    }

    public void setCacheLocation(URL url) {
        try {
            File dir = new File(url.toURI());
            if (!dir.exists()) {
                dir.mkdirs();
            }
            cacheLocation = url;
        } catch (URISyntaxException e) {
            logger.error("Could not create cache location.", e);
        }
    }
}
