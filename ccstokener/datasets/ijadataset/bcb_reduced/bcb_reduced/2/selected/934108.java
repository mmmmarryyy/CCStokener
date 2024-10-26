package org.formaria.scripts.groovy;

import groovy.lang.GroovyObject;
import groovy.lang.MetaProperty;
import groovy.lang.MissingMethodException;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import org.formaria.debug.DebugLogger;
import org.formaria.swing.Page;
import org.formaria.aria.events.AriaEventHandler;
import org.formaria.aria.validation.ValidationHandler;

/**
 *
 * <p>Copyright (c) Formaria Ltd., 2008</p>
 * <p>License: see license.txt</p>
 * @author luano
 */
public class GroovyPage extends Page {

    private GroovyEventHandler eventHandler;

    private GroovyObject groovyObject;

    private static Object[] emptyArgs = new Object[0];

    private URL url;

    private GroovyScriptEngine engine;

    private boolean wasLoaded;

    private long fileTimeStamp;

    public GroovyPage(GroovyScriptEngine eng, URL url) {
        engine = eng;
        this.url = url;
        reload();
    }

    /**
   * Reload the groovy class and create a new object. If the script is loaded
   * from a file resource then the file modification date is checked and the
   * file is only reloaded if modified.
   */
    public void reload() {
        wasLoaded = false;
        InputStream is = null;
        try {
            if (url.getProtocol().equals("file")) {
                File f = new File(url.toURI());
                long timeStamp = f.lastModified();
                if (timeStamp == fileTimeStamp) return;
                fileTimeStamp = timeStamp;
            }
            is = url.openStream();
            GroovyObject oldObject = groovyObject;
            groovyObject = (GroovyObject) engine.loadClass(is);
            groovyObject.setProperty("page", this);
            if (eventHandler == null) eventHandler = new GroovyEventHandler(project, this, new ValidationHandler(this));
            if (oldObject != null) {
                List properties = oldObject.getMetaClass().getProperties();
                for (Object property : properties) {
                    MetaProperty metaProperty = (MetaProperty) property;
                    try {
                        String key = metaProperty.getName();
                        if (!key.equals("class") && !key.equals("metaClass")) groovyObject.setProperty(key, oldObject.getProperty(key));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            wasLoaded = true;
        } catch (Exception ioe) {
            DebugLogger.logError("BUILDER", "Unable to load the Groovy object: " + url.getFile());
            ioe.printStackTrace();
        } finally {
            closeStream(is);
        }
    }

    /**
   * Reload the script if it has been loaded from a file
   */
    public void reloadScript() {
        if (fileTimeStamp > 0) reload();
    }

    /**
   * A method called once the page has been created and initialized but just
   * prior to display. Override this 
   * method if you need to add custom behavior when the page is activated or 
   * displayed.
   */
    public void pageActivated() {
        try {
            if (fileTimeStamp > 0) reloadScript();
            groovyObject.invokeMethod("pageActivated", emptyArgs);
        } catch (MissingMethodException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
   * A method called once the page has been created but not yet initialized.
   * Override this method if you need to implement custom creation when the 
   * page is created. The method is only called when the page is instantiated 
   * and not when the page is redisplayed. The pageActivated method is invoked
   * whenever a page is displayed.
   */
    public void pageCreated() {
        try {
            groovyObject.invokeMethod("pageCreated", emptyArgs);
        } catch (MissingMethodException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
   * Called whenver the page is about to loose scope and be hidden. 
   */
    public void pageDeactivated() {
        try {
            groovyObject.invokeMethod("pageDeactivated", emptyArgs);
        } catch (MissingMethodException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
   * Get the event handler used by this object
   * @return
   */
    public AriaEventHandler getEventHandler() {
        return eventHandler;
    }

    /**
   * Attempt to invoke a method of the page
   * @param methodName the method name
   * @return the return value
   */
    public Object invokeMethod(String methodName) {
        try {
            return groovyObject.invokeMethod(methodName, emptyArgs);
        } catch (MissingMethodException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
   * Is the page valid and hass the script loaded properly
   * @return true if everything load ok
   */
    public boolean wasLoaded() {
        return wasLoaded;
    }

    /**
   * Close the script input stream
   * @param is the input streams
   */
    private void closeStream(InputStream is) {
        try {
            if (is != null) is.close();
        } catch (Exception e) {
        }
    }
}
