package eu.somatik.moviebrowser.cache;

import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Set;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import eu.somatik.moviebrowser.config.Settings;
import eu.somatik.moviebrowser.domain.MovieInfo;
import eu.somatik.moviebrowser.domain.MovieLocation;
import eu.somatik.moviebrowser.service.ui.ContentProvider;
import eu.somatik.moviebrowser.tools.FileTools;

/**
 *
 * @author francisdb
 */
@Singleton
public class FileSystemImageCache implements ImageCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemImageCache.class);

    private final Settings settings;

    @Inject
    public FileSystemImageCache(final Settings settings) {
        this.settings = settings;
    }

    /**
     *
     * @param info
     */
    @Override
    public Image loadImg(MovieInfo info, ContentProvider provider) {
        Image image = null;
        if (info != null) {
            String imgUrl = provider.getImageUrl(info);
            if (imgUrl != null) {
                File file = getCacheFile(imgUrl);
                try {
                    if (!file.exists()) {
                        saveImgToCache(info, provider);
                    }
                    if (file.exists()) {
                        image = ImageIO.read(file);
                    } else {
                        LOGGER.debug("Image not available in local cache: " + imgUrl);
                    }
                } catch (IOException ex) {
                    LOGGER.error("Could not load image " + imgUrl + " -> " + file, ex);
                }
            }
        }
        return image;
    }

    /**
     * @param imgUrl
     * @return the cached image file
     */
    private File getCacheFile(String imgUrl) {
        File cached = null;
        final String startAfter = "imdb.com/";
        String cacheName = imgUrl;
        if (imgUrl.indexOf(startAfter) != -1) {
            int startIndex = imgUrl.indexOf(startAfter) + startAfter.length();
            cacheName = imgUrl.substring(startIndex);
        } else {
            if (imgUrl.startsWith("http://")) {
                cacheName = imgUrl.substring("http://".length());
            }
        }
        cacheName = cacheName.replace('/', '_').replace(':', '_');
        cached = new File(settings.getImageCacheDir(), cacheName);
        return cached;
    }

    @Override
    public void removeImgFromCache(MovieInfo info, ContentProvider provider) {
        String url = provider.getImageUrl(info);
        if (url != null) {
            File file = new File(url);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    /**
     * @param info 
     * @return the saved file
     */
    @Override
    public File saveImgToCache(MovieInfo info, ContentProvider provider) {
        File cached = null;
        final String url = provider.getImageUrl(info);
        if (url != null) {
            InputStream is = null;
            try {
                URL imgUrl = new URL(url);
                URLConnection urlC = imgUrl.openConnection();
                is = new BufferedInputStream(imgUrl.openStream());
                Date date = new Date(urlC.getLastModified());
                LOGGER.trace("Saving resource type: {}, modified on: {}", urlC.getContentType(), date);
                cached = getCacheFile(url);
                FileTools.writeToFile(is, cached);
                is.close();
                cached.setLastModified(date.getTime());
                if (settings.getSaveAlbumArt()) {
                    LOGGER.info("COVER URL: " + url);
                    File cover = null;
                    cover = getCacheFile(url);
                    Set<MovieLocation> locations = info.getMovie().getLocations();
                    for (MovieLocation l : locations) {
                        File save = new File(new File(l.getPath()), info.getMovie().getTitle() + "-cover-art.jpg");
                        try {
                            FileTools.copy(cover, save);
                        } catch (FileNotFoundException ex) {
                            LOGGER.warn(String.format("Could not save cover from %s to %s (%s)", cover.getAbsolutePath(), save.getAbsolutePath(), ex.getMessage()));
                        }
                    }
                }
            } catch (MalformedURLException ex) {
                LOGGER.error("Could not save image '" + url + "'", ex);
            } catch (IOException ex) {
                LOGGER.error("Could not save image '" + url + "'", ex);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ex) {
                        LOGGER.error("Could not close input stream", ex);
                    }
                }
            }
        }
        return cached;
    }
}
