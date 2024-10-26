package com.metasolutions.jfcml;

import java.applet.Applet;
import java.awt.Container;
import java.awt.Window;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.net.URL;
import javax.xml.parsers.SAXParser;
import javax.swing.*;
import org.xml.sax.InputSource;
import com.metasolutions.jfcml.helpers.WindowContext;
import com.metasolutions.util.JFCMLConfiguration;
import com.metasolutions.util.PackageRegistry;

/**
 * Transforms a JFCML document into the final window.
 * 
 * @author  Shawn Curry
 * @version 0.9, 8-20-2004
 * 
 * @see com.metasolutions.util.PackageRegistry
 * @see com.metasolutions.jfcml.TagHandler
 * @see javax.xml.parsers.SAXParser
 */
public class JFCMLWindowFactory implements Serializable {

    private static final boolean STACK_TRACE;

    static {
        STACK_TRACE = ((Boolean) PackageRegistry.getService("STACK_TRACE")).booleanValue();
    }

    protected TagHandler tagHandler;

    protected SAXParser saxParser;

    /**
	 * Create a JFCMLWindowFactory that uses the default configuration. 
	 */
    public JFCMLWindowFactory() {
        this("com.metasolutions.util.JFCMLConfiguration");
    }

    /**
	 * Create a JFCMLWindowFactory that uses the default configuration and the
	 * specified WindowContext.  This constructor is used primarily by the 'Include'
	 * element.
	 * 
	 * @param context the WindowContext to use
	 */
    public JFCMLWindowFactory(WindowContext context) {
        this("com.metasolutions.util.JFCMLConfiguration");
        if (tagHandler != null) tagHandler.setWindowContext(context);
    }

    /**
	 * Create a JFCMLWindowFactory that uses the specified key to locate
	 * a Configuration in the PackageRegistry.
	 * 
	 * @param configName the JFCMLConfiguration's PackageRegistry key
	 */
    public JFCMLWindowFactory(String configName) {
        this((JFCMLConfiguration) PackageRegistry.getService(configName));
    }

    /**
	 * Create a JFCMLWindowFactory the uses the specified configuration.
	 * 
	 * @param config the JFCMLConfiguration to use.
	 */
    public JFCMLWindowFactory(JFCMLConfiguration config) {
        if (config == null) throw new RuntimeException("Failed Configuration lookup");
        tagHandler = config.getTagHandler();
        saxParser = config.getSAXParser();
        if (tagHandler == null) throw new RuntimeException("Failed TagHandler lookup");
        if (saxParser == null) throw new RuntimeException("Failed SAXParser lookup");
    }

    /**
	 * Create a JFCMLWindowFactory that uses the specified TagHandler and SAXParser.
	 * 
	 * @param handler the TagHandler to use.
	 * @param parser the SAXParser to use.
	 */
    public JFCMLWindowFactory(TagHandler handler, SAXParser parser) {
        if (tagHandler == null) throw new RuntimeException("TagHandler must not be null");
        if (saxParser == null) throw new RuntimeException("SAXParser must not be null");
        tagHandler = handler;
        saxParser = parser;
    }

    public void build(String id, InputSource is, Container target) {
        if (!(target instanceof Window || target instanceof Applet || target instanceof JRootPane)) throw new IllegalArgumentException("Target must be a top-level container");
        synchronized (this) {
            tagHandler.startWindow(id, target);
            try {
                saxParser.parse(is, tagHandler);
            } catch (Exception e) {
                System.out.println("FATAL: " + e.getMessage() + "; " + e.getClass());
                if (STACK_TRACE) e.printStackTrace();
                return;
            }
            tagHandler.endWindow();
            if (target instanceof JFCMLWindow) ((JFCMLWindow) target).init();
        }
    }

    public void build(InputSource is, Container target) {
        build(null, is, target);
    }

    public void build(URL url, Container target) throws IOException {
        build(url.toExternalForm(), new InputSource(url.openStream()), target);
    }

    public void build(File f, Container target) throws FileNotFoundException {
        build(f.getAbsolutePath(), new InputSource(new FileInputStream(f)), target);
    }

    public void build(InputStream in, Container target) {
        build(null, new InputSource(in), target);
    }

    public void build(Reader r, Container target) {
        build(null, new InputSource(r), target);
    }
}
