package org.eclipse.help.internal.search;

import java.io.*;
import java.net.*;
import java.util.Hashtable;
import javax.xml.parsers.*;
import org.eclipse.core.runtime.*;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.internal.base.*;
import org.eclipse.help.internal.entityresolver.LocalEntityResolver;
import org.eclipse.help.search.ISearchEngine;
import org.eclipse.help.search.ISearchEngineResult;
import org.eclipse.help.search.ISearchEngineResultCollector;
import org.eclipse.help.search.ISearchScope;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * This implementation of <code>ISearchEngine</code> interface performs search
 * by running a query on the remote InfoCenter and presenting the results
 * locally. Instances of this engine type are required to supply the URL to the
 * InfoCenter.
 * 
 * <p>
 * This class is made public in order to be instantiated and parametrized
 * directly in the extentsions. Clients are required to supply the required URL
 * as a parameter <code>url</code>.
 * 
 * <p>
 * This class is not expected to be subclassed or otherwise accessed
 * programmatically.
 * 
 * @since 3.1
 */
public final class InfoCenter implements ISearchEngine {

    private Hashtable tocs;

    public static class Scope implements ISearchScope {

        String url;

        boolean searchSelected;

        String[] tocs;

        public Scope(String url, boolean searchSelected, String[] tocs) {
            this.url = url;
            this.searchSelected = searchSelected;
            this.tocs = tocs;
        }
    }

    private class InfoCenterResult implements ISearchEngineResult {

        private IHelpResource category;

        private Element node;

        private String baseURL;

        public InfoCenterResult(String baseURL, Element node) {
            this.baseURL = baseURL;
            this.node = node;
            createCategory(node);
        }

        private void createCategory(Element node) {
            final String href = node.getAttribute("toc");
            final String label = node.getAttribute("toclabel");
            if (href != null && label != null) {
                category = (IHelpResource) tocs.get(href);
                if (category == null) {
                    category = new IHelpResource() {

                        public String getLabel() {
                            return label;
                        }

                        public String getHref() {
                            return href;
                        }
                    };
                    tocs.put(href, category);
                }
            }
        }

        public String getLabel() {
            return node.getAttribute("label");
        }

        public String getDescription() {
            return null;
        }

        public IHelpResource getCategory() {
            return category;
        }

        public String getHref() {
            return node.getAttribute("href");
        }

        public float getScore() {
            String value = node.getAttribute("score");
            if (value != null) return Float.parseFloat(value);
            return (float) 0.0;
        }

        public boolean getForceExternalWindow() {
            return false;
        }

        public String toAbsoluteHref(String href, boolean frames) {
            String url = baseURL;
            if (!url.endsWith("/")) url = url + "/";
            if (frames) {
                return url + "topic" + href;
            }
            return url + "topic" + href + "&noframes=true";
        }
    }

    /**
	 * The default constructor.
	 */
    public InfoCenter() {
        tocs = new Hashtable();
    }

    public void run(String query, ISearchScope scope, ISearchEngineResultCollector collector, IProgressMonitor monitor) throws CoreException {
        URL url = createURL(query, (Scope) scope);
        if (url == null) return;
        InputStream is = null;
        tocs.clear();
        try {
            URLConnection connection = url.openConnection();
            monitor.beginTask(HelpBaseResources.InfoCenter_connecting, 5);
            is = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
            monitor.worked(1);
            load(((Scope) scope).url, reader, collector, new SubProgressMonitor(monitor, 4));
            reader.close();
        } catch (FileNotFoundException e) {
            reportError(HelpBaseResources.InfoCenter_fileNotFound, e, collector);
        } catch (IOException e) {
            reportError(HelpBaseResources.InfoCenter_io, e, collector);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void reportError(String message, IOException e, ISearchEngineResultCollector collector) {
        Status status = new Status(IStatus.ERROR, HelpBasePlugin.PLUGIN_ID, IStatus.OK, message, e);
        collector.error(status);
    }

    private void load(String baseURL, Reader r, ISearchEngineResultCollector collector, IProgressMonitor monitor) {
        Document document = null;
        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            parser.setEntityResolver(new LocalEntityResolver());
            if (monitor.isCanceled()) return;
            monitor.beginTask("", 5);
            monitor.subTask(HelpBaseResources.InfoCenter_searching);
            document = parser.parse(new InputSource(r));
            if (monitor.isCanceled()) return;
            Node root = document.getFirstChild();
            while (root.getNodeType() == Node.COMMENT_NODE) {
                document.removeChild(root);
                root = document.getFirstChild();
                if (monitor.isCanceled()) return;
            }
            monitor.worked(1);
            load(baseURL, document, (Element) root, collector, new SubProgressMonitor(monitor, 4));
        } catch (ParserConfigurationException e) {
        } catch (IOException e) {
        } catch (SAXException e) {
        }
    }

    private void load(String baseURL, Document doc, Element root, ISearchEngineResultCollector collector, IProgressMonitor monitor) {
        NodeList topics = root.getElementsByTagName("topic");
        ISearchEngineResult[] results = new ISearchEngineResult[topics.getLength()];
        monitor.subTask(HelpBaseResources.InfoCenter_processing);
        monitor.beginTask("", results.length);
        for (int i = 0; i < topics.getLength(); i++) {
            Element el = (Element) topics.item(i);
            if (monitor.isCanceled()) break;
            results[i] = new InfoCenterResult(baseURL, el);
            monitor.worked(1);
        }
        collector.accept(results);
    }

    private URL createURL(String query, Scope scope) {
        StringBuffer buf = new StringBuffer();
        buf.append(scope.url);
        if (!scope.url.endsWith("/")) buf.append("/search?searchWord="); else buf.append("search?searchWord=");
        try {
            buf.append(URLEncoder.encode(query, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            buf.append(query);
        }
        buf.append("&locale=");
        buf.append(Platform.getNL());
        if (scope.searchSelected && scope.tocs != null) {
            buf.append("&scopedSearch=true");
            for (int i = 0; i < scope.tocs.length; i++) {
                String toc;
                try {
                    toc = URLEncoder.encode(scope.tocs[i], "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    toc = scope.tocs[i];
                }
                buf.append("&scope=");
                buf.append(toc);
            }
        }
        try {
            return new URL(buf.toString());
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
