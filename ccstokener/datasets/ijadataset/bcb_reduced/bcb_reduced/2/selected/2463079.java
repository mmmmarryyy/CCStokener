package edu.indiana.extreme.xbaya.component.registry;

import edu.indiana.extreme.xbaya.component.ComponentException;
import edu.indiana.extreme.xbaya.component.gui.ComponentTreeNode;
import edu.indiana.extreme.xbaya.component.ws.WSComponent;
import edu.indiana.extreme.xbaya.component.ws.WSComponentFactory;
import edu.indiana.extreme.xbaya.util.IOUtil;
import xsul5.MLogger;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Satoshi Shirasuna
 */
public class WebComponentRegistry extends ComponentRegistry {

    private static final MLogger logger = MLogger.getLogger();

    private URL url;

    private ComponentTreeNode tree;

    private Map<String, List<WSComponent>> componentsMap;

    /**
     * Creates a WebComponentRegistryClient
     * 
     * @param urlString
     * @throws MalformedURLException
     * @throws IOException
     */
    public WebComponentRegistry(String urlString) throws MalformedURLException, IOException {
        this(new URL(urlString));
    }

    /**
     * Creates a WebComponentRegistryClient
     * 
     * @param url
     *            The URL of the web page.
     */
    public WebComponentRegistry(URL url) {
        this.url = url;
        this.componentsMap = new HashMap<String, List<WSComponent>>();
    }

    /**
     * @see edu.indiana.extreme.xbaya.component.registry.ComponentRegistry#getName()
     */
    @Override
    public String getName() {
        return this.url.toString();
    }

    /**
     * @see edu.indiana.extreme.xbaya.component.registry.ComponentRegistry#getComponentTree()
     */
    @Override
    public ComponentTreeNode getComponentTree() throws ComponentRegistryException {
        this.tree = new ComponentTreeNode(this);
        parse();
        return this.tree;
    }

    /**
     * Returns a list of component of a specified name.
     * 
     * @param name
     *            The name of the component. The name here is a relative URL
     *            specified in <a href="name"> tag, and is same as the name of a
     *            corresponding ComponentTree.
     * @return The list of components of the specified name
     */
    public List<WSComponent> getComponents(String name) {
        List<WSComponent> components = this.componentsMap.get(name);
        return components;
    }

    private void parse() throws ComponentRegistryException {
        try {
            HttpURLConnection connection = (HttpURLConnection) this.url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.connect();
            int count = 0;
            while (String.valueOf(connection.getResponseCode()).startsWith("3")) {
                String location = connection.getHeaderField("Location");
                logger.finest("Redirecting to " + location);
                connection.disconnect();
                this.url = new URL(location);
                connection = (HttpURLConnection) this.url.openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.connect();
                count++;
                if (count > 10) {
                    throw new ComponentRegistryException("Too many redirect");
                }
            }
            InputStream inputStream = connection.getInputStream();
            InputStreamReader reader = new InputStreamReader(inputStream);
            HtmlRegistryParserCallback callback = new HtmlRegistryParserCallback();
            ParserDelegator parser = new ParserDelegator();
            parser.parse(reader, callback, false);
        } catch (IOException e) {
            throw new ComponentRegistryException(e);
        }
    }

    private void addComponents(String name) {
        try {
            URL wsdlUrl = new URL(this.url, name);
            logger.finest("WSDL URL: " + wsdlUrl);
            String wsdlString = IOUtil.readToString(wsdlUrl.openStream());
            logger.finest("WSDL: " + wsdlString);
            List<WSComponent> components = WSComponentFactory.createComponents(wsdlString);
            addComponents(name, components);
        } catch (MalformedURLException e) {
            logger.caught(e);
        } catch (IOException e) {
            logger.caught(e);
        } catch (ComponentException e) {
            logger.caught(e);
        } catch (RuntimeException e) {
            logger.caught(e);
        }
    }

    private void addComponents(String name, List<WSComponent> components) {
        this.componentsMap.put(name, components);
        WebComponentReference componentReference = new WebComponentReference(name, components);
        ComponentTreeNode treeLeaf = new ComponentTreeNode(componentReference);
        this.tree.add(treeLeaf);
    }

    private class HtmlRegistryParserCallback extends HTMLEditorKit.ParserCallback {

        /**
         * @see javax.swing.text.html.HTMLEditorKit.ParserCallback#handleStartTag(javax.swing.text.html.HTML.Tag,
         *      javax.swing.text.MutableAttributeSet, int)
         */
        @Override
        public void handleStartTag(Tag tag, MutableAttributeSet attrSet, int pos) {
            logger.entering(new Object[] { tag, attrSet, new Integer(pos) });
            if (tag == HTML.Tag.A) {
                String name = (String) attrSet.getAttribute(HTML.Attribute.HREF);
                addComponents(name);
            }
        }
    }
}
