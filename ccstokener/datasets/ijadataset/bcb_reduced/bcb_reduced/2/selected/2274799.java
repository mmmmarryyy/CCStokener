package org.sapient_platypus.utils.bindml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import com.jgoodies.binding.value.ValueModel;

/**
 * @author Nicholas Daley
 *
 */
public final class BindEngine {

    /**
     * The class loader used for obtaining resources.
     */
    private ClassLoader cl = BindEngine.class.getClassLoader();

    /**
     * Used to fetch component to be bound.
     */
    private final ComponentSource componentSource;

    /**
     * Used to fetch model to be bound to.
     */
    private final ModelSource modelSource;

    /**
     * Library of tag handlers.
     */
    private final TagLibrary tagLibrary = new TagLibrary();

    /**
     * Library of binding handlers.
     */
    private final BindingLibrary bindingLibrary = new BindingLibrary();

    /**
     * @param componentSource {@link #componentSource}
     * @param modelSource {@link #modelSource}
     */
    public BindEngine(final ComponentSource componentSource, final ModelSource modelSource) {
        this.componentSource = componentSource;
        this.modelSource = modelSource;
    }

    /**
     * Binds components from {@link #componentSource} to models from {@link #modelSource}.
     * @param jdoc JDOM Document containing the binding XML file.
     * @throws BindException If there was a problem reading the file.
     */
    public void bind(final Document jdoc) throws BindException {
        final Element root = jdoc.getRootElement();
        if (!root.getName().equals("bindings")) {
            throw new BindException("Root element of binding file should be <bindings>.");
        }
        final List bindingElements = root.getChildren();
        for (final Iterator it = bindingElements.iterator(); it.hasNext(); ) {
            final Element bindingElement = (Element) it.next();
            if (!bindingElement.getName().equals("binding")) {
                throw new BindException("Expected a <binding> element, but got a <" + bindingElement.getName() + "> element.");
            }
            final Iterator it2 = bindingElement.getChildren().iterator();
            if (it2.hasNext()) {
                final Element modelElement = (Element) it2.next();
                final ValueModel model = tagLibrary.getModel(modelElement);
                for (; it2.hasNext(); ) {
                    final Element componentElement = (Element) it2.next();
                    final Object component = tagLibrary.getComponent(componentElement);
                    getBindingLibrary().bind(model, component);
                }
            }
        }
    }

    /**
     * Binds components from {@link #componentSource} to models from {@link #modelSource}.
     * @param xmlFile The binding XML file.
     * @throws BindException If there was a problem reading the file.
     */
    public void bind(final File xmlFile) throws BindException {
        if (xmlFile == null) {
            throw new BindException();
        }
        try {
            bind(new FileReader(xmlFile));
        } catch (final FileNotFoundException e) {
            System.err.println(e);
            throw new BindException(e);
        }
    }

    /**
     * Binds components from {@link #componentSource} to models from {@link #modelSource}.
     * @param xmlReader Reader on the open binding XML file.
     * @throws BindException If there was a problem reading the file.
     */
    public void bind(final Reader xmlReader) throws BindException {
        if (xmlReader == null) {
            throw new BindException();
        }
        try {
            bind(new SAXBuilder().build(xmlReader));
        } catch (final JDOMException e) {
            System.err.println(e);
            throw new BindException("Invalid BindML file", e);
        } catch (final IOException e) {
            System.err.println(e);
            throw new BindException(e);
        }
    }

    /**
     * Binds components from {@link #componentSource} to models from {@link #modelSource}.
     * @param resource The location of the binding XML file.
     * @throws BindException If there was a problem reading the file.
     */
    public void bind(final String resource) throws BindException {
        Reader reader = null;
        try {
            final InputStream in = cl.getResourceAsStream(resource);
            if (in == null) {
                throw new BindException("Resource could not be found " + resource);
            }
            reader = new InputStreamReader(in);
            bind(reader);
        } finally {
            try {
                reader.close();
            } catch (final IOException ex) {
            }
        }
    }

    /**
     * Binds components from {@link #componentSource} to models from {@link #modelSource}.
     * @param url The location of the binding XML file.
     * @throws BindException If there was a problem reading the file.
     */
    public void bind(final URL url) throws BindException {
        Reader reader = null;
        try {
            final InputStream in = url.openStream();
            if (in == null) {
                throw new BindException("Resource could not be found " + url.toString());
            }
            reader = new InputStreamReader(in);
            bind(reader);
        } catch (final IOException e) {
            System.err.println(e);
            throw new BindException(e);
        } finally {
            try {
                reader.close();
            } catch (final IOException ex) {
            }
        }
    }

    /**
     * @return The tag library for this <code>BindEngine</code>.
     */
    public TagLibrary getTagLibrary() {
        return tagLibrary;
    }

    /**
     * @return The binding library for this <code>BindEngine</code>.
     */
    public BindingLibrary getBindingLibrary() {
        return bindingLibrary;
    }
}
