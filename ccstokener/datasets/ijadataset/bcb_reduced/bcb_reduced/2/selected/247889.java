package org.allcolor.ywt.i18n;

import org.allcolor.xml.parser.CDocumentBuilderFactory;
import org.allcolor.ywt.filter.CContext;
import org.w3c.dom.Document;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;

/**
 * 
DOCUMENT ME!
 *
 * @author Quentin Anciaux
 * @version 0.1.0
 */
@SuppressWarnings("unchecked")
public class CXmlMessageResourceBundle {

    /** DOCUMENT ME! */
    public static final String CONFIG = "i18n/i18n";

    /** DOCUMENT ME! */
    private static final HashMap FileMap = new HashMap();

    /** DOCUMENT ME! */
    protected Locale loc = null;

    /** DOCUMENT ME! */
    protected String baseName = CXmlMessageResourceBundle.CONFIG;

    /** DOCUMENT ME! */
    protected long lastModified = 0;

    /** DOCUMENT ME! */
    private Ci18nList keyMap = null;

    /**
   * Creates a new CXmlMessageResourceBundle object.
   * 
   * @param loc
   *            DOCUMENT ME!
   * @param keyMap
   *            DOCUMENT ME!
   * @param baseName
   *            DOCUMENT ME!
   * @param lastModified
   *            DOCUMENT ME!
   */
    private CXmlMessageResourceBundle(final Locale loc, final Ci18nList keyMap, final String baseName, final long lastModified) {
        this.loc = (loc != null) ? loc : Locale.getDefault();
        this.keyMap = (keyMap != null) ? keyMap : new Ci18nList(this.loc.getLanguage());
        this.baseName = (baseName != null) ? baseName : CXmlMessageResourceBundle.CONFIG;
        this.lastModified = lastModified;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param loc DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws MissingResourceException DOCUMENT ME!
	 */
    public static final CXmlMessageResourceBundle getBundle(final Locale loc) throws MissingResourceException {
        return CXmlMessageResourceBundle.loadFile(null, loc);
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param baseName DOCUMENT ME!
	 * @param loc DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws MissingResourceException DOCUMENT ME!
	 */
    public static final CXmlMessageResourceBundle getBundle(final String baseName, final Locale loc) throws MissingResourceException {
        return CXmlMessageResourceBundle.loadFile(baseName, loc);
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public final String getBaseName() {
        return this.baseName;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public final Iterator getKeys() {
        return this.keyMap.getMessages();
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public final Locale getLocale() {
        return this.loc;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param key DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public final String getMessage(final String key) {
        return this.getMessage(key, (Object[]) null);
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param key DOCUMENT ME!
	 * @param args DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public final String getMessage(final String key, final Object args[]) {
        try {
            final CXmlMessageResourceBundle resource = this;
            final Ci18n result = resource.keyMap.getMessage(key);
            if ((result != null) && (args != null)) {
                String strResult = result.getMessage();
                for (int j = 0; j < args.length; j++) {
                    final StringBuffer tmp = new StringBuffer();
                    final Pattern p = Pattern.compile("\\{" + j + "\\}");
                    final Matcher match = p.matcher(strResult);
                    int offset = 0;
                    while (match.find(offset)) {
                        final int start = match.start();
                        final int end = match.end();
                        tmp.append(strResult.substring(offset, start));
                        if (args[j] != null) {
                            tmp.append(args[j]);
                        }
                        offset = end;
                    }
                    if (offset < strResult.length()) {
                        tmp.append(strResult.substring(offset));
                    }
                    strResult = tmp.toString();
                }
                return strResult;
            } else if (result != null) {
                return result.getMessage();
            } else {
                return null;
            }
        } catch (final Exception ignore) {
            return null;
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param fbaseName DOCUMENT ME!
	 * @param loc DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws MissingResourceException DOCUMENT ME!
	 */
    private static synchronized CXmlMessageResourceBundle loadFile(final String fbaseName, final Locale loc) throws MissingResourceException {
        String baseName = fbaseName;
        if (baseName == null) {
            baseName = CXmlMessageResourceBundle.CONFIG;
        }
        final String language = (loc == null) ? "" : ((("".equals(loc.getLanguage())) || (loc.getLanguage() == null)) ? "" : loc.getLanguage());
        final String country = (loc == null) ? "" : ((("".equals(loc.getCountry())) || (loc.getCountry() == null)) ? "" : ("_" + loc.getCountry().toUpperCase()));
        final String variant = (loc == null) ? "" : ((("".equals(loc.getVariant())) || (loc.getVariant() == null)) ? "" : ("_" + loc.getVariant()));
        final String fileToLoad = "".equals(language) ? ((baseName).replace('.', '/') + ".xml") : ("".equals(country) ? ((baseName + "_" + language).replace('.', '/') + ".xml") : ("".equals(variant) ? ((baseName + "_" + language + country).replace('.', '/') + ".xml") : ((baseName + "_" + language + country + variant).replace('.', '/') + ".xml")));
        final ServletContext context = CContext.getInstance().getContext();
        long lastModified = 0;
        URL urlToLoad = null;
        try {
            urlToLoad = context.getResource(fileToLoad);
        } catch (final Exception ignore) {
            urlToLoad = null;
        }
        if (urlToLoad != null) {
            try {
                final URLConnection conn = urlToLoad.openConnection();
                lastModified = conn.getLastModified();
                conn.getInputStream().close();
            } catch (final Exception ignore) {
                ;
            }
        } else {
            urlToLoad = Thread.currentThread().getContextClassLoader().getResource(fileToLoad);
            if (urlToLoad != null) {
                try {
                    final URLConnection uc = urlToLoad.openConnection();
                    lastModified = uc.getLastModified();
                    uc.getInputStream().close();
                } catch (final Exception ignore) {
                    ;
                }
            }
        }
        if (CXmlMessageResourceBundle.FileMap.containsKey(fileToLoad)) {
            final CXmlMessageResourceBundle resource = (CXmlMessageResourceBundle) CXmlMessageResourceBundle.FileMap.get(fileToLoad);
            if (resource.lastModified == lastModified) {
                return resource;
            }
        }
        if ((urlToLoad == null) && (loc != null) && !fileToLoad.equals(baseName + ".xml")) {
            if ((loc.getVariant() != null) && !"".equals(loc.getVariant().trim())) {
                return CXmlMessageResourceBundle.loadFile(fbaseName, new Locale(loc.getLanguage(), loc.getCountry()));
            }
            if ((loc.getCountry() != null) && !"".equals(loc.getCountry().trim())) {
                return CXmlMessageResourceBundle.loadFile(fbaseName, new Locale(loc.getLanguage()));
            }
            return CXmlMessageResourceBundle.loadFile(fbaseName, null);
        }
        if (urlToLoad == null) {
            final CXmlMessageResourceBundle resource = CXmlMessageResourceBundle.loadPropertyFile(baseName, loc);
            if (resource != null) {
                return resource;
            }
            throw new MissingResourceException("Resource file '" + baseName + "' not found for : " + loc, CXmlMessageResourceBundle.class.getName(), "");
        }
        Document doc = null;
        InputStream in = null;
        try {
            in = urlToLoad.openStream();
            doc = CDocumentBuilderFactory.newParser().newDocumentBuilder().parse(in);
        } catch (final Exception ignore) {
            throw new MissingResourceException("Resource file '" + baseName + "' not found for : " + loc, CXmlMessageResourceBundle.class.getName(), "");
        } finally {
            try {
                in.close();
            } catch (final Exception ignore) {
                ;
            }
        }
        final Ci18nList keyMap = Ci18nList.unmarshall(doc, language + country + variant);
        final CXmlMessageResourceBundle resource = new CXmlMessageResourceBundle(loc, keyMap, baseName, lastModified);
        CXmlMessageResourceBundle.FileMap.put(fileToLoad, resource);
        return resource;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param fbaseName DOCUMENT ME!
	 * @param loc DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws MissingResourceException DOCUMENT ME!
	 */
    private static synchronized CXmlMessageResourceBundle loadPropertyFile(final String fbaseName, final Locale loc) throws MissingResourceException {
        String baseName = fbaseName;
        if (baseName == null) {
            baseName = CXmlMessageResourceBundle.CONFIG;
        }
        final String language = (loc == null) ? "" : ((("".equals(loc.getLanguage())) || (loc.getLanguage() == null)) ? "" : loc.getLanguage());
        final String country = (loc == null) ? "" : ((("".equals(loc.getCountry())) || (loc.getCountry() == null)) ? "" : ("_" + loc.getCountry().toUpperCase()));
        final String variant = (loc == null) ? "" : ((("".equals(loc.getVariant())) || (loc.getVariant() == null)) ? "" : ("_" + loc.getVariant()));
        final String fileToLoad = "".equals(language) ? ((baseName).replace('.', '/') + ".properties") : ("".equals(country) ? ((baseName + "_" + language).replace('.', '/') + ".properties") : ("".equals(variant) ? ((baseName + "_" + language + country).replace('.', '/') + ".properties") : ((baseName + "_" + language + country + variant).replace('.', '/') + ".properties")));
        final ServletContext context = CContext.getInstance().getContext();
        long lastModified = 0;
        URL urlToLoad = null;
        try {
            urlToLoad = context.getResource(fileToLoad);
        } catch (final Exception ignore) {
            urlToLoad = null;
        }
        if (urlToLoad != null) {
            try {
                final URLConnection conn = urlToLoad.openConnection();
                lastModified = conn.getLastModified();
                conn.getInputStream().close();
            } catch (final Exception ignore) {
                ;
            }
        } else {
            urlToLoad = Thread.currentThread().getContextClassLoader().getResource(fileToLoad);
            if (urlToLoad != null) {
                try {
                    final URLConnection uc = urlToLoad.openConnection();
                    lastModified = uc.getLastModified();
                    uc.getInputStream().close();
                } catch (final Exception ignore) {
                    ;
                }
            }
        }
        if (CXmlMessageResourceBundle.FileMap.containsKey(fileToLoad)) {
            final CXmlMessageResourceBundle resource = (CXmlMessageResourceBundle) CXmlMessageResourceBundle.FileMap.get(fileToLoad);
            if ((resource.lastModified == lastModified) && (lastModified != 0)) {
                return resource;
            }
        }
        if (urlToLoad == null) {
            final CXmlMessageResourceBundle resource = new CXmlMessageResourceBundle(loc, new Ci18nList("en"), baseName, lastModified);
            return resource;
        }
        final Properties prop = new Properties();
        InputStream in = null;
        try {
            in = urlToLoad.openStream();
            prop.load(in);
        } catch (final Exception ignore) {
            ignore.printStackTrace();
            final CXmlMessageResourceBundle resource = new CXmlMessageResourceBundle(loc, new Ci18nList(loc.toString()), baseName, lastModified);
            return resource;
        } finally {
            try {
                in.close();
            } catch (final Exception e) {
                ;
            }
        }
        final Ci18nList keyMap = Ci18nList.unmarshall(prop, language + country + variant);
        final CXmlMessageResourceBundle resource = new CXmlMessageResourceBundle(loc, keyMap, baseName, lastModified);
        CXmlMessageResourceBundle.FileMap.put(fileToLoad, resource);
        return resource;
    }
}
