package org.jmage.resource;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.jmage.ApplicationContext;
import javax.servlet.ServletContext;
import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * FontFactory loads and creates fonts as resources from system environment.
 */
public class FontFactory implements ResourceFactory {

    protected static FontFactory fontFactory;

    private static final String TTF = "ttf";

    private static final String HTTP = "http";

    private static final String FILE = "file";

    private static final char SUFFIX_SEPARATOR = '.';

    protected static Logger log = Logger.getLogger(FontFactory.class.getName());

    protected List fontTypes;

    protected List schemeTypes;

    protected ApplicationContext applicationContext;

    protected ServletContext servletContext;

    private static final String resourcedir = "resourcedir";

    private static final String FONTS = "fonts";

    private static final String SERVLET_CONTEXT = "SERVLET_CONTEXT";

    private static final String URI_HANDLINGERROR = "unable to handle URI resource, cause: ";

    private static final String FILE_RESOURCE_RETRIEVED = " retrieved file resource: ";

    private static final String URL_RESOURCE_RETRIEVED = " retrieved URL resource: ";

    private static final String SCHEME_ERROR = "unable to retrieve resource, could not handle scheme: ";

    private static final String FILE_LOADED = " loaded font from file: ";

    private static final String FILE_LOADERROR = "unable to load font from file: ";

    private static final String SERVLET_LOAD = " loaded font from servlet container: ";

    private static final String SERVLET_LOADERROR = "unable to load font from servlet container: ";

    private static final String URL_LOADERROR = "unable to load font from URL: ";

    private static final String URL_LOADED = " loaded font from url: ";

    private static final String CAUSE = ", cause: ";

    private static final String SLASH = "/";

    private static final String REGEX_BACKSLASH = "\\\\";

    private static final String CLASSPATH_LOADERROR = "unable to retrieve font from classpath, cause: ";

    public FontFactory() {
        fontTypes = new ArrayList();
        fontTypes.add(TTF);
        schemeTypes = new ArrayList();
        schemeTypes.add(HTTP);
        schemeTypes.add(FILE);
    }

    public void configureRequestProperties(Properties properties) {
    }

    public void removeRequestProperties(Properties properties) {
    }

    public void configure(ApplicationContext context) {
        this.applicationContext = context;
        this.servletContext = (ServletContext) this.applicationContext.get(SERVLET_CONTEXT);
    }

    public boolean canHandle(URI resource) {
        try {
            String suffix = resource.getPath().substring(resource.getPath().lastIndexOf(SUFFIX_SEPARATOR) + 1).toLowerCase();
            String scheme = resource.getScheme();
            return fontTypes.contains(suffix) && schemeTypes.contains(scheme);
        } catch (Exception e) {
            if (log.isInfoEnabled()) log.info(URI_HANDLINGERROR + e.getMessage());
            return false;
        }
    }

    public Object createFrom(URI resource) throws ResourceException {
        String scheme = resource.getScheme().toLowerCase();
        if (FILE.equals(scheme)) {
            File file = new File(resource);
            Font font = this.getFile(file);
            if (log.isInfoEnabled()) log.info(FILE_RESOURCE_RETRIEVED + file.getPath());
            return font;
        }
        if (HTTP.equals(scheme)) {
            URL url = null;
            try {
                url = resource.toURL();
            } catch (MalformedURLException e) {
                throw new ResourceException(e.getMessage());
            }
            Font font = this.getURL(url);
            if (log.isInfoEnabled()) log.info(URL_RESOURCE_RETRIEVED + url.toString());
            return font;
        }
        throw new ResourceException(SCHEME_ERROR + scheme);
    }

    protected Font getFile(File file) throws ResourceException {
        Font font = null;
        font = font == null ? this.getAbsoluteFile(file) : font;
        font = font == null ? this.getServletContainerResource(file) : font;
        font = font == null ? this.getJMAGEResourceDirFile(file) : font;
        font = font == null ? this.getClassPathResource(file) : font;
        font = font == null ? this.getCurrentDirFile(file) : font;
        if (font == null) {
            if (log.isEnabledFor(Priority.ERROR)) log.error(FILE_LOADERROR + file);
            throw new ResourceException(FILE_LOADERROR + file);
        }
        return font;
    }

    protected Font getClassPathResource(File file) throws ResourceException {
        Font font = null;
        String fontPath = file.getPath();
        if (fontPath.indexOf(File.separator) == 0) {
            fontPath = fontPath.substring(1);
        }
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL fontURL = classLoader.getResource(fontPath);
        if (fontURL == null) {
            fontURL = classLoader.getResource(SLASH + fontPath);
            if (fontURL != null) {
                fontPath = (SLASH + fontPath);
            }
        }
        if (fontURL == null) {
            fontURL = classLoader.getResource(FONTS + SLASH + fontPath);
            if (fontURL != null) {
                fontPath = (FONTS + SLASH + fontPath);
            }
        }
        if (fontURL == null) {
            fontURL = classLoader.getResource(SLASH + FONTS + SLASH + fontPath);
            if (fontURL != null) {
                fontPath = (SLASH + FONTS + SLASH + fontPath);
            }
        }
        if (fontURL == null) {
            classLoader = this.getClass().getClassLoader();
            fontURL = classLoader.getResource(SLASH + fontPath);
            if (fontURL != null) {
                fontPath = (SLASH + fontPath);
            }
        }
        if (fontURL == null) {
            classLoader = this.getClass().getClassLoader();
            fontURL = classLoader.getResource(FONTS + SLASH + fontPath);
            if (fontURL != null) {
                fontPath = (FONTS + SLASH + fontPath);
            }
        }
        if (fontURL == null) {
            classLoader = this.getClass().getClassLoader();
            fontURL = classLoader.getResource(SLASH + FONTS + SLASH + fontPath);
            if (fontURL != null) {
                fontPath = (SLASH + FONTS + SLASH + fontPath);
            }
        }
        if (fontURL != null) {
            try {
                font = Font.createFont(Font.TRUETYPE_FONT, classLoader.getResourceAsStream(fontPath));
            } catch (Exception e) {
                if (log.isEnabledFor(Priority.INFO)) log.info(CLASSPATH_LOADERROR + e.getMessage());
            }
        }
        return font;
    }

    protected Font getCurrentDirFile(File file) {
        Font font = null;
        if (font == null) {
            File current = new File(".");
            File fontFile = new File(current, file.getPath());
            if (fontFile.isFile() && fontFile.exists()) {
                try {
                    font = Font.createFont(Font.TRUETYPE_FONT, this.loadFile(fontFile));
                    if (log.isDebugEnabled()) log.debug(FILE_LOADED + file.getPath());
                } catch (Exception e) {
                    if (log.isEnabledFor(Priority.ERROR)) log.error(FILE_LOADERROR, e);
                }
            }
        }
        return font;
    }

    protected Font getJMAGEResourceDirFile(File file) {
        Font font = null;
        String resourceDirName = this.applicationContext.getProperty(resourcedir);
        if (resourceDirName == null) {
            return null;
        }
        File fontResourceDir = new File(resourceDirName, FONTS);
        if (fontResourceDir != null && fontResourceDir.isDirectory() && fontResourceDir.exists()) {
            File fontFile = new File(fontResourceDir, file.getPath());
            if (fontFile.isFile() && fontFile.exists()) {
                try {
                    font = Font.createFont(Font.TRUETYPE_FONT, this.loadFile(fontFile));
                    if (log.isDebugEnabled()) log.debug(FILE_LOADED + file.getPath());
                } catch (Exception e) {
                    if (log.isEnabledFor(Priority.ERROR)) log.error(FILE_LOADERROR + file.getAbsolutePath(), e);
                }
            }
        }
        return font;
    }

    protected Font getAbsoluteFile(File file) {
        Font font = null;
        if (font == null && file.isAbsolute() && file.exists()) {
            try {
                font = Font.createFont(Font.TRUETYPE_FONT, this.loadFile(file));
                if (log.isDebugEnabled()) log.debug(FILE_LOADED + file.getAbsolutePath());
            } catch (Exception e) {
                if (log.isEnabledFor(Priority.ERROR)) log.error(FILE_LOADERROR, e);
            }
        }
        return font;
    }

    protected Font getURL(URL url) throws ResourceException {
        Font font = null;
        String errorMessage = URL_LOADERROR + url.toString();
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, this.loadUrl(url));
            if (log.isDebugEnabled()) log.debug(URL_LOADED + url.toString());
        } catch (Exception e) {
            if (log.isEnabledFor(Priority.ERROR)) log.error(errorMessage + CAUSE + e.getMessage());
            throw new ResourceException(errorMessage + CAUSE + e.getMessage());
        }
        if (font == null) {
            if (log.isEnabledFor(Priority.ERROR)) log.error(errorMessage);
            throw new ResourceException(errorMessage);
        }
        return font;
    }

    /**
     * Get the file from the ServletContainer as a system resource.
     *
     * @param file the file
     * @return the image
     */
    protected Font getServletContainerResource(File file) {
        Font font = null;
        if (servletContext == null) {
            servletContext = (ServletContext) this.applicationContext.get(SERVLET_CONTEXT);
        }
        if (servletContext != null) {
            try {
                InputStream is = servletContext.getResourceAsStream(file.getPath().replaceAll(REGEX_BACKSLASH, SLASH));
                if (is == null) {
                    File derived = new File(SLASH + FONTS, file.getPath());
                    is = servletContext.getResourceAsStream(derived.getPath().replaceAll(REGEX_BACKSLASH, SLASH));
                }
                if (is == null) {
                    File derived = new File(FONTS, file.getPath());
                    is = servletContext.getResourceAsStream(derived.getPath().replaceAll(REGEX_BACKSLASH, SLASH));
                }
                if (is == null) {
                    File derived = new File(SLASH, file.getPath());
                    is = servletContext.getResourceAsStream(derived.getPath().replaceAll(REGEX_BACKSLASH, SLASH));
                }
                font = Font.createFont(Font.TRUETYPE_FONT, is);
                if (log.isInfoEnabled()) log.info(SERVLET_LOAD + file.getPath().replaceAll(REGEX_BACKSLASH, SLASH));
            } catch (Exception e) {
                if (log.isEnabledFor(Priority.INFO)) log.info(SERVLET_LOADERROR + file.getPath().replaceAll(REGEX_BACKSLASH, SLASH));
            }
        }
        return font;
    }

    protected InputStream loadFile(File file) throws IOException {
        if (!file.isFile() && !file.exists()) {
            throw new IOException(FILE_LOADERROR);
        }
        FileInputStream fis = new FileInputStream(file);
        return fis;
    }

    protected InputStream loadUrl(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        connection.connect();
        DataInputStream remoteObj = new DataInputStream(connection.getInputStream());
        return remoteObj;
    }

    public String toString() {
        return "[" + this.getClass().getName() + "#" + this.hashCode() + "]";
    }
}
