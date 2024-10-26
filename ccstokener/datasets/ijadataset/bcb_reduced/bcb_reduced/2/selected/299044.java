package org.eclipse.emf.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import org.eclipse.emf.common.CommonPlugin;
import org.eclipse.emf.common.util.ResourceLocator;
import org.eclipse.emf.common.util.WrappedException;

/**
 * An abstract resource locator implementation
 * comprising a {@link #getPrimaryResourceLocator() primary locator}
 * and a series {@link #getDelegateResourceLocators() delegate locators}.
 */
public abstract class DelegatingResourceLocator implements ResourceLocator {

    /**
   * The cached base URL.
   */
    protected URL baseURL;

    /**
   * The resource bundle containing untranslated strings.
   */
    protected ResourceBundle untranslatedResourceBundle;

    /**
   * The resource bundle containing translated strings.
   */
    protected ResourceBundle resourceBundle;

    /**
   * A cache of the translated strings.
   */
    protected Map<String, String> strings = new HashMap<String, String>();

    /**
   * A cache of the untranslated strings.
   */
    protected Map<String, String> untranslatedStrings = new HashMap<String, String>();

    /**
   * A cache of the image descriptions.
   */
    protected Map<String, Object> images = new HashMap<String, Object>();

    /**
   * Whether to translate strings by default.
   */
    protected boolean shouldTranslate = true;

    /**
   * Creates an instance.
   */
    public DelegatingResourceLocator() {
        super();
    }

    /**
   * Returns the primary resource locator.
   * @return the primary resource locator.
   */
    protected abstract ResourceLocator getPrimaryResourceLocator();

    /**
   * Returns the delegate resource locators.
   * @return the delegate resource locators.
   */
    protected abstract ResourceLocator[] getDelegateResourceLocators();

    private static final URI DOT = URI.createURI(".");

    public URL getBaseURL() {
        if (baseURL == null) {
            if (getPrimaryResourceLocator() == null) {
                try {
                    Class<? extends DelegatingResourceLocator> theClass = getClass();
                    URL pluginPropertiesURL = theClass.getResource("plugin.properties");
                    if (pluginPropertiesURL == null) {
                        String className = theClass.getName();
                        int index = className.lastIndexOf(".");
                        URL classURL = theClass.getResource((index == -1 ? className : className.substring(index + 1)) + ".class");
                        URI uri = URI.createURI(classURL.toString());
                        int count = 1;
                        for (int i = 0; (i = className.indexOf('.', i)) != -1; ++i) {
                            ++count;
                        }
                        uri = uri.trimSegments(count);
                        if (URI.isArchiveScheme(uri.scheme())) {
                            try {
                                InputStream inputStream = new URL(uri.appendSegment("plugin.properties").toString()).openStream();
                                inputStream.close();
                                baseURL = new URL(uri.toString());
                            } catch (IOException exception) {
                                uri = URI.createURI(uri.authority()).trimSegments(1);
                            }
                        }
                        if (baseURL == null) {
                            String lastSegment = uri.lastSegment();
                            if ("bin".equals(lastSegment) || "runtime".equals(lastSegment)) {
                                uri = uri.trimSegments(1);
                            }
                            uri = uri.appendSegment("plugin.properties");
                            try {
                                InputStream inputStream = new URL(uri.toString()).openStream();
                                inputStream.close();
                                baseURL = new URL(DOT.resolve(uri).toString());
                            } catch (IOException exception) {
                            }
                        }
                        if (baseURL == null) {
                            String resourceName = index == -1 ? "plugin.properties" : className.substring(0, index + 1).replace('.', '/') + "plugin.properties";
                            throw new MissingResourceException("Missing properties: " + resourceName, theClass.getName(), "plugin.properties");
                        }
                    } else {
                        baseURL = new URL(DOT.resolve(URI.createURI(pluginPropertiesURL.toString())).toString());
                    }
                } catch (IOException exception) {
                    throw new WrappedException(exception);
                }
            } else {
                baseURL = getPrimaryResourceLocator().getBaseURL();
            }
        }
        return baseURL;
    }

    public Object getImage(String key) {
        Object result = images.get(key);
        if (result == null) {
            ResourceLocator pluginResourceLocator = getPrimaryResourceLocator();
            if (pluginResourceLocator == null) {
                try {
                    result = doGetImage(key);
                } catch (MalformedURLException exception) {
                    throw new WrappedException(exception);
                } catch (IOException exception) {
                    result = delegatedGetImage(key);
                }
            } else {
                try {
                    result = pluginResourceLocator.getImage(key);
                } catch (MissingResourceException exception) {
                    result = delegatedGetImage(key);
                }
            }
            images.put(key, result);
        }
        return result;
    }

    /**
   * Does the work of fetching the image associated with the key.
   * It ensures that the image exists.
   * @param key the key of the image to fetch.
   * @exception IOException if an image doesn't exist.
   * @return the description of the image associated with the key.
   */
    protected Object doGetImage(String key) throws IOException {
        URL url = new URL(getBaseURL() + "icons/" + key + extensionFor(key));
        InputStream inputStream = url.openStream();
        inputStream.close();
        return url;
    }

    /**
   * Computes the file extension to be used with the key to specify an image resource.
   * @param key the key for the imagine.
   * @return the file extension to be used with the key to specify an image resource.
   */
    protected static String extensionFor(String key) {
        String result = ".gif";
        int index = key.lastIndexOf('.');
        if (index != -1) {
            String extension = key.substring(index + 1);
            if ("png".equalsIgnoreCase(extension) || "gif".equalsIgnoreCase(extension) || "bmp".equalsIgnoreCase(extension) || "ico".equalsIgnoreCase(extension) || "jpg".equalsIgnoreCase(extension) || "jpeg".equalsIgnoreCase(extension) || "tif".equalsIgnoreCase(extension) || "tiff".equalsIgnoreCase(extension)) {
                result = "";
            }
        }
        return result;
    }

    /**
   * Does the work of fetching the image associated with the key,
   * when the image resource is not available locally.
   * @param key the key of the image to fetch.
   * @exception MissingResourceException if the image resource doesn't exist anywhere.
   * @see #getDelegateResourceLocators()
   */
    protected Object delegatedGetImage(String key) throws MissingResourceException {
        ResourceLocator[] delegateResourceLocators = getDelegateResourceLocators();
        for (int i = 0; i < delegateResourceLocators.length; ++i) {
            try {
                return delegateResourceLocators[i].getImage(key);
            } catch (MissingResourceException exception) {
            }
        }
        throw new MissingResourceException(CommonPlugin.INSTANCE.getString("_UI_ImageResourceNotFound_exception", new Object[] { key }), getClass().getName(), key);
    }

    /**
   * Indicates whether strings should be translated by default.
   *
   * @return <code>true</code> if strings should be translated by default; <code>false</code> otherwise.
   */
    public boolean shouldTranslate() {
        return shouldTranslate;
    }

    /**
   * Sets whether strings should be translated by default.
   *
   * @param shouldTranslate whether strings should be translated by default.
   */
    public void setShouldTranslate(boolean shouldTranslate) {
        this.shouldTranslate = shouldTranslate;
    }

    public String getString(String key) {
        return getString(key, shouldTranslate());
    }

    public String getString(String key, boolean translate) {
        Map<String, String> stringMap = translate ? strings : untranslatedStrings;
        String result = stringMap.get(key);
        if (result == null) {
            try {
                ResourceLocator pluginResourceLocator = getPrimaryResourceLocator();
                if (pluginResourceLocator == null) {
                    result = doGetString(key, translate);
                } else {
                    result = pluginResourceLocator.getString(key, translate);
                }
            } catch (MissingResourceException exception) {
                result = delegatedGetString(key, translate);
            }
            stringMap.put(key, result);
        }
        return result;
    }

    /**
   * Does the work of fetching the string associated with the key.
   * It ensures that the string exists.
   * @param key the key of the string to fetch.
   * @exception MissingResourceException if a string doesn't exist.
   * @return the string associated with the key.
   */
    protected String doGetString(String key, boolean translate) throws MissingResourceException {
        ResourceBundle bundle = translate ? resourceBundle : untranslatedResourceBundle;
        if (bundle == null) {
            String packageName = getClass().getName();
            int index = packageName.lastIndexOf(".");
            if (index != -1) {
                packageName = packageName.substring(0, index);
            }
            if (translate) {
                try {
                    bundle = resourceBundle = ResourceBundle.getBundle(packageName + ".plugin");
                } catch (MissingResourceException exception) {
                    try {
                        InputStream inputStream = new URL(getBaseURL().toString() + "plugin.properties").openStream();
                        bundle = untranslatedResourceBundle = resourceBundle = new PropertyResourceBundle(inputStream);
                        inputStream.close();
                    } catch (IOException ioException) {
                    }
                    if (bundle == null) {
                        throw exception;
                    }
                }
            } else {
                String resourceName = getBaseURL().toString() + "plugin.properties";
                try {
                    InputStream inputStream = new URL(resourceName).openStream();
                    bundle = untranslatedResourceBundle = new PropertyResourceBundle(inputStream);
                    inputStream.close();
                } catch (IOException ioException) {
                    throw new MissingResourceException("Missing properties: " + resourceName, getClass().getName(), "plugin.properties");
                }
            }
        }
        return bundle.getString(key);
    }

    /**
   * Does the work of fetching the string associated with the key,
   * when the string resource is not available locally.
   * @param key the key of the string to fetch.
   * @exception MissingResourceException if the string resource doesn't exist anywhere.
   * @see #getDelegateResourceLocators()
   */
    protected String delegatedGetString(String key, boolean translate) {
        ResourceLocator[] delegateResourceLocators = getDelegateResourceLocators();
        for (int i = 0; i < delegateResourceLocators.length; ++i) {
            try {
                return delegateResourceLocators[i].getString(key, translate);
            } catch (MissingResourceException exception) {
            }
        }
        throw new MissingResourceException(MessageFormat.format("The string resource ''{0}'' could not be located", new Object[] { key }), getClass().getName(), key);
    }

    public String getString(String key, Object[] substitutions) {
        return getString(key, substitutions, shouldTranslate());
    }

    public String getString(String key, Object[] substitutions, boolean translate) {
        return MessageFormat.format(getString(key, translate), substitutions);
    }
}
