package org.jmage.resource;

import com.sun.media.jai.codec.ByteArraySeekableStream;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.jmage.ApplicationContext;
import org.jmage.mapper.InterceptorMapper;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.servlet.ServletContext;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * DefaultImageFactory loads images as resources from system environment.
 */
public class DefaultImageFactory implements ResourceFactory {

    protected static DefaultImageFactory defaultImageFactory;

    private static final String PNG = "png";

    private static final String GIF = "gif";

    private static final String JPG = "jpg";

    private static final String JPEG = "jpeg";

    private static final String TIF = "tif";

    private static final String TIFF = "tiff";

    private static final String BMP = "bmp";

    private static final String HTTP = "http";

    private static final String FILE = "file";

    protected static Logger log = Logger.getLogger(DefaultImageFactory.class.getName());

    protected List imageTypes;

    protected List schemeTypes;

    protected ApplicationContext applicationContext;

    protected ServletContext servletContext;

    private static final String resourcedir = "resourcedir";

    private static final String IMAGES = "images";

    private static final String SERVLET_CONTEXT = "SERVLET_CONTEXT";

    private static final String URI_HANDLINGERROR = "unable to handle URI resource, cause: ";

    private static final String FILE_RESOURCE_RETRIEVED = " retrieved file resource: ";

    private static final String URL_RESOURCE_RETRIEVED = " retrieved URL resource: ";

    private static final String SCHEME_ERROR = "unable to retrieve resource, could not handle scheme: ";

    private static final String FILELOAD = "fileload";

    private static final String FILE_LOADED = " loaded image from file: ";

    private static final String FILE_LOADERROR = "unable to load image from file: ";

    private static final String STREAM = "stream";

    private static final String SERVLET_LOADERROR = "unable to load image from servlet container: ";

    private static final String SERVLET_LOADED = " loaded image from servlet container: ";

    private static final String URL_LOADERROR = "unable to load image from URL: ";

    private static final String URL_LOADED = " loaded image from url: ";

    private static final String CAUSE = ", cause: ";

    private static final String TRUE = "TRUE";

    private static final String HTTP_HEADER_ERROR = "error while retrieving http status header from server";

    private static final String HTTP_400 = "40";

    private static final String SLASH = "/";

    private static final char SUFFIX_SEPARATOR = '.';

    private static final String REGEX_BACKSLASH = "\\\\";

    private static final String CLASSPATH_LOADERROR = "unable to retrieve resource from classpath, cause: ";

    /**
     * Create a DefaultimageFactory
     */
    public DefaultImageFactory() {
        imageTypes = new ArrayList();
        imageTypes.add(PNG);
        imageTypes.add(GIF);
        imageTypes.add(JPG);
        imageTypes.add(JPEG);
        imageTypes.add(TIF);
        imageTypes.add(TIFF);
        imageTypes.add(BMP);
        schemeTypes = new ArrayList();
        schemeTypes.add(HTTP);
        schemeTypes.add(FILE);
    }

    /**
     * Configures the ImageFactory with ApplicationContext
     *
     * @param context the ApplicationContext
     */
    public void configure(ApplicationContext context) {
        this.applicationContext = context;
        this.servletContext = (ServletContext) this.applicationContext.get(SERVLET_CONTEXT);
    }

    public void configureRequestProperties(Properties properties) {
    }

    public void removeRequestProperties(Properties properties) {
    }

    /**
     * Tests whether the ImageFactory can handle a particular resource URI.
     *
     * @param resource
     * @return true | false
     */
    public boolean canHandle(URI resource) {
        try {
            String suffix = resource.getPath().substring(resource.getPath().lastIndexOf(SUFFIX_SEPARATOR) + 1).toLowerCase();
            String scheme = resource.getScheme();
            return imageTypes.contains(suffix) && schemeTypes.contains(scheme);
        } catch (Exception e) {
            if (log.isInfoEnabled()) log.info(URI_HANDLINGERROR + e.getMessage());
            return false;
        }
    }

    /**
     * Create an object resource from a resource URI
     *
     * @param resource the resource URI
     * @return the object
     * @throws ResourceException
     */
    public Object createFrom(URI resource) throws ResourceException {
        String scheme = resource.getScheme().toLowerCase();
        if (FILE.equals(scheme)) {
            File file = new File(resource);
            PlanarImage image = this.getFile(file);
            if (log.isInfoEnabled()) log.info(FILE_RESOURCE_RETRIEVED + file.getName());
            return image;
        }
        if (HTTP.equals(scheme)) {
            URL url = null;
            try {
                url = resource.toURL();
            } catch (MalformedURLException e) {
                throw new ResourceException(e.getMessage());
            }
            PlanarImage image = this.getURL(url);
            if (log.isInfoEnabled()) log.info(URL_RESOURCE_RETRIEVED + url.toString());
            return image;
        }
        throw new ResourceException(SCHEME_ERROR + scheme);
    }

    /**
     * Get the image resource from a file.
     *
     * @param file the file
     * @return the image
     * @throws ResourceException
     */
    protected PlanarImage getFile(File file) throws ResourceException {
        PlanarImage image = null;
        image = getAbsoluteFile(file);
        image = image == null ? this.getServletContainerResource(file) : image;
        image = image == null ? this.getJMAGEResourceDirFile(file) : image;
        image = image == null ? this.getClassPathResource(file) : image;
        image = image == null ? this.getCurrentDirFile(file) : image;
        if (image == null) {
            if (log.isEnabledFor(Priority.ERROR)) log.error(FILE_LOADERROR + file);
            throw new ResourceException(FILE_LOADERROR + file);
        }
        return image;
    }

    /**
     * Get the file from the classpath as a system resource
     *
     * @param file
     * @return true | false
     * @throws ResourceException
     */
    protected PlanarImage getClassPathResource(File file) throws ResourceException {
        PlanarImage image = null;
        String imagePath = file.getPath();
        if (imagePath.indexOf(File.separator) == 0) {
            imagePath = imagePath.substring(1);
        }
        imagePath = imagePath.replaceAll(REGEX_BACKSLASH, SLASH);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL imageURL = this.locateOnClassPath(imagePath, classLoader);
        if (imageURL == null) {
            classLoader = this.getClass().getClassLoader();
            imageURL = this.locateOnClassPath(imagePath, classLoader);
        }
        if (imageURL != null) {
            try {
                image = JAI.create(STREAM, new ByteArraySeekableStream(this.streamConvert(classLoader.getResourceAsStream(imagePath)).toByteArray()));
            } catch (IOException e) {
                if (log.isEnabledFor(Priority.INFO)) log.info(CLASSPATH_LOADERROR + e.getMessage());
            }
        }
        return image;
    }

    protected URL locateOnClassPath(String path, ClassLoader classLoader) {
        URL uRL = classLoader.getResource(path);
        if (uRL == null) {
            uRL = classLoader.getResource(SLASH + path);
            if (uRL != null) {
                path = (SLASH + path);
            }
        }
        if (uRL == null) {
            uRL = classLoader.getResource(IMAGES + SLASH + path);
            if (uRL != null) {
                path = (IMAGES + SLASH + path);
            }
        }
        if (uRL == null) {
            uRL = classLoader.getResource(SLASH + IMAGES + SLASH + path);
            if (uRL != null) {
                path = (SLASH + IMAGES + SLASH + path);
            }
        }
        return uRL;
    }

    /**
     * Get the file from the current application directory.
     *
     * @param file the file
     * @return the image
     */
    protected PlanarImage getCurrentDirFile(File file) {
        PlanarImage image = null;
        if (!file.isAbsolute()) {
            File current = new File(".");
            File imageFile = new File(current, file.getPath());
            if (imageFile.isFile() && imageFile.exists()) {
                try {
                    image = JAI.create(FILELOAD, imageFile.getAbsolutePath());
                    if (log.isDebugEnabled()) log.debug(FILE_LOADED + imageFile.getAbsolutePath());
                } catch (Exception e) {
                    if (log.isEnabledFor(Priority.ERROR)) log.error(FILE_LOADERROR, e);
                }
            }
        }
        return image;
    }

    /**
     * Get the file from the dir specified trough the resourcedir property
     *
     * @param file the file
     * @return the image or null
     */
    protected PlanarImage getJMAGEResourceDirFile(File file) {
        PlanarImage image = null;
        String resourceDirName = this.applicationContext.getProperty(resourcedir);
        if (resourceDirName == null) {
            return null;
        }
        File imageResourceDir = new File(resourceDirName, IMAGES);
        if (imageResourceDir != null && imageResourceDir.isDirectory() && imageResourceDir.exists()) {
            File imageFile = new File(imageResourceDir, file.getPath());
            if (imageFile.isFile() && imageFile.exists()) {
                try {
                    image = JAI.create(FILELOAD, imageFile.getAbsolutePath());
                    if (log.isDebugEnabled()) log.debug(FILE_LOADED + imageFile.getAbsolutePath());
                } catch (Exception e) {
                    if (log.isEnabledFor(Priority.ERROR)) log.error(FILE_LOADERROR, e);
                }
            }
        }
        if (image != null) return image;
        imageResourceDir = new File(resourceDirName);
        if (imageResourceDir != null && imageResourceDir.isDirectory() && imageResourceDir.exists()) {
            File imageFile = new File(imageResourceDir, file.getPath());
            if (imageFile.isFile() && imageFile.exists()) {
                try {
                    image = JAI.create(FILELOAD, imageFile.getAbsolutePath());
                    if (log.isDebugEnabled()) log.debug(FILE_LOADED + imageFile.getAbsolutePath());
                } catch (Exception e) {
                    if (log.isEnabledFor(Priority.ERROR)) log.error(FILE_LOADERROR, e);
                }
            }
        }
        return image;
    }

    /**
     * Get the file from the ServletContainer as a system resource.
     *
     * @param file the file
     * @return the image
     */
    protected PlanarImage getServletContainerResource(File file) {
        PlanarImage image = null;
        if (servletContext == null) {
            servletContext = (ServletContext) this.applicationContext.get(SERVLET_CONTEXT);
            if (log.isEnabledFor(Priority.DEBUG)) log.debug("servletContext is " + servletContext);
        }
        if (servletContext != null) {
            try {
                InputStream is = servletContext.getResourceAsStream(file.getPath().replaceAll(REGEX_BACKSLASH, SLASH));
                if (is == null) {
                    File derived = new File(SLASH + IMAGES, file.getPath());
                    is = servletContext.getResourceAsStream(derived.getPath().replaceAll(REGEX_BACKSLASH, SLASH));
                }
                if (is == null) {
                    File derived = new File(IMAGES, file.getPath());
                    is = servletContext.getResourceAsStream(derived.getPath().replaceAll(REGEX_BACKSLASH, SLASH));
                }
                if (is == null) {
                    File derived = new File(SLASH, file.getPath());
                    is = servletContext.getResourceAsStream(derived.getPath().replaceAll(REGEX_BACKSLASH, SLASH));
                }
                if (is == null) {
                    String derivedPath = file.getPath().replaceAll(REGEX_BACKSLASH, SLASH);
                    File derived = new File(derivedPath.startsWith(SLASH) ? derivedPath.substring(1) : derivedPath);
                    is = servletContext.getResourceAsStream(derived.getPath().replaceAll(REGEX_BACKSLASH, SLASH));
                }
                if (is == null) {
                    if (log.isDebugEnabled()) log.debug(SERVLET_LOADERROR + file.getPath().replaceAll(REGEX_BACKSLASH, SLASH));
                    return null;
                }
                image = JAI.create(STREAM, new ByteArraySeekableStream((this.streamConvert(is).toByteArray())));
                if (log.isDebugEnabled()) log.debug(SERVLET_LOADED + file.getPath().replaceAll(REGEX_BACKSLASH, SLASH));
            } catch (Throwable e) {
                if (log.isDebugEnabled()) log.debug(SERVLET_LOADERROR + file.getPath().replaceAll(REGEX_BACKSLASH, SLASH));
            }
        }
        return image;
    }

    /**
     * Get the absolute file from the file system
     *
     * @param file the file
     * @return the image
     */
    protected PlanarImage getAbsoluteFile(File file) {
        PlanarImage image = null;
        if (file.isAbsolute() && file.exists()) {
            try {
                image = JAI.create(FILELOAD, file.getAbsolutePath());
                if (log.isDebugEnabled()) log.debug(FILE_LOADED + file.getAbsolutePath());
            } catch (Exception e) {
                if (log.isEnabledFor(Priority.ERROR)) log.error(FILE_LOADERROR, e);
            }
        }
        return image;
    }

    /**
     * Get the image from an URL
     *
     * @param url the url
     * @return the image
     * @throws ResourceException
     */
    protected PlanarImage getURL(URL url) throws ResourceException {
        PlanarImage image = null;
        final String errorMessage = URL_LOADERROR + url.toString();
        try {
            byte[] urlBytes = this.readFromUrl(url).toByteArray();
            image = JAI.create(STREAM, new ByteArraySeekableStream(urlBytes));
            if (log.isDebugEnabled()) log.debug(URL_LOADED + url.toString());
        } catch (Exception e) {
            if (log.isEnabledFor(Priority.ERROR)) log.error(errorMessage + CAUSE + e.getMessage());
            throw new ResourceException(errorMessage);
        }
        if (image == null) {
            if (log.isEnabledFor(Priority.ERROR)) log.error(errorMessage);
            throw new ResourceException(errorMessage);
        }
        return image;
    }

    /**
     * Read an image from a URL using http
     *
     * @param url the url
     * @return the image stream
     * @throws IOException
     */
    protected ByteArrayOutputStream readFromUrl(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty(InterceptorMapper.JMAGE_INTERNAL, TRUE);
        connection.connect();
        String responseHeader = connection.getHeaderField(null);
        assert (responseHeader != null) : HTTP_HEADER_ERROR;
        if (responseHeader.indexOf(HTTP_400) > -1) {
            throw new IOException(URL_LOADERROR + url + CAUSE + responseHeader);
        }
        InputStream is = connection.getInputStream();
        return streamConvert(is);
    }

    private ByteArrayOutputStream streamConvert(InputStream inputStream) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int c = 0;
        while ((c = inputStream.read()) > -1) {
            bos.write(c);
        }
        return bos;
    }

    public String toString() {
        return "[" + this.getClass().getName() + "#" + this.hashCode() + "]";
    }
}
