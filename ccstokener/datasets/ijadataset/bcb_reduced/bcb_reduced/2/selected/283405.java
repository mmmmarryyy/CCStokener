package org.lokoen.comicviewer.fetchers;

import org.lokoen.comicviewer.*;
import java.util.regex.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.ImageIcon;
import java.net.*;
import java.util.logging.Logger;

/**
 *
 * @author  Dag Viggo Lok??en
 */
public class RegExFetcher implements ComicFetcher, Runnable {

    ImageIcon image;

    boolean isDone = false;

    URL baseURL;

    Pattern pattern;

    URL imageBaseURL;

    Logger logger = Logger.global;

    public void run() {
        logger.info("Fetcher starting");
        try {
            URLConnection con = baseURL.openConnection();
            if (con instanceof HttpURLConnection) {
                con.setRequestProperty("Referer", baseURL.toString());
                con.setRequestProperty("User-Agent", "ComicViewer/1");
                System.out.println(con.getRequestProperties());
                con.connect();
                Object content = con.getContent();
                if (content instanceof InputStream) {
                    InputStream is = (InputStream) content;
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    StringBuffer page = new StringBuffer();
                    String line;
                    line = br.readLine();
                    while (line != null) {
                        page.append(line);
                        line = br.readLine();
                    }
                    logger.info("Trying to match with pattern: \"" + pattern.pattern());
                    Matcher m = pattern.matcher(page);
                    if (m.matches()) {
                        String imgloc = m.group(1);
                        String imgurl = imageBaseURL + imgloc;
                        logger.info("Image at: " + imgurl);
                        URLConnection imgcon = new URL(imgurl).openConnection();
                        imgcon.setRequestProperty("Referer", baseURL.toString());
                        imgcon.setRequestProperty("User-Agent", "ComicViewer/1");
                        imgcon.connect();
                        Toolkit tool = Toolkit.getDefaultToolkit();
                        Image img = tool.createImage((ImageProducer) imgcon.getContent());
                        logger.info(img.getWidth(null) + ", " + img.getHeight(null));
                        image = new ImageIcon(img);
                        isDone = true;
                    } else {
                        logger.warning("No match, pattern \"" + pattern.pattern() + "\"");
                    }
                } else {
                    logger.warning("Wrong IS type");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("Fetcher done");
    }

    public ImageIcon getImage() {
        return image;
    }

    public void setup(Properties _properties) throws ComicException {
        String burl = _properties.getProperty("Base-URL");
        String pat = _properties.getProperty("Image-RegExp");
        String iurl = _properties.getProperty("Image-Base-URL");
        try {
            baseURL = new URL(burl);
            imageBaseURL = new URL(iurl);
            pattern = Pattern.compile(pat);
        } catch (PatternSyntaxException e) {
            e.printStackTrace();
            throw new ComicException("Error in pattern");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new ComicException("Error in URL " + burl);
        }
        Toolkit tool = Toolkit.getDefaultToolkit();
        ClassLoader classloader = this.getClass().getClassLoader();
        URL pic = classloader.getResource("org/lokoen/comicviewer/gui/downloading.png");
        image = new ImageIcon(tool.getImage(pic));
    }

    public boolean isDone() {
        return isDone;
    }
}
