package org.jtools.antutils.antlib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import org.jpattern.condition.Condition;
import org.jpattern.condition.True;
import org.jtools.config.ConfigUtils;
import org.jtools.elements.Child;
import org.jtools.elements.Element;
import org.jtools.elements.ElementImpl;
import org.jtools.meta.meta_inf.antlib.DefType;
import org.jtools.util.net.NetUtils;
import org.jtools.util.xml.QNameImpl;
import org.jtools.util.xml.XMLUtils;

public class AntLibEntries {

    public static interface AddedEntryListener {

        void onAdded(AntLibEntries entries, AntLibEntry entry);
    }

    public static final String antLibsResource = "META-INF/antlib/antlibs";

    public static final String antLibFragment = "META-INF/antlib/antlib.xml";

    public static final String[][] commonTaskdefs = { { "org/apache/tools/ant/taskdefs/defaults.properties", "antlib:org.apache.tools.ant" } };

    public static final String[][] commonTypedefs = { { "org/apache/tools/ant/types/defaults.properties", "antlib:org.apache.tools.ant" } };

    public static final String[][] commonAntlibs = { { "org.apache.tools.ant" }, { "org.apache.tools.ant.types.conditions" }, { "net.sf.antcontrib" } };

    private static final String antLibsResource2root = "../../";

    private List<AntLibEntry> entries = new ArrayList<AntLibEntry>();

    private Condition<AntLibEntry> filter = True.getInstance();

    private List<AddedEntryListener> addedEntryListeners = new ArrayList<AntLibEntries.AddedEntryListener>();

    public AntLibEntries() {
    }

    public AntLibEntries(AntLibEntries src) {
        for (AntLibEntry entry : src.entries) add(entry.copy());
    }

    public AntLibEntries copy() {
        return new AntLibEntries(this);
    }

    public void loadDefinitions(ClassLoader classLoader) {
        loadDefinitionProperties(classLoader);
        loadAntLibFragments(classLoader);
    }

    public void loadAntlibs(ClassLoader classLoader) {
        loadExistingAntlibs(classLoader);
        loadCommonDefs(classLoader);
        loadCommonLibs(classLoader);
    }

    public void loadDefinitionProperties(ClassLoader classLoader) {
        try {
            for (DefType defType : DefType.values()) {
                LibEntryType entryType = LibEntryType.valueOf(defType);
                Enumeration<URL> resources = classLoader == null ? ClassLoader.getSystemResources(defType.getFileName()) : classLoader.getResources(defType.getFileName());
                while (resources.hasMoreElements()) {
                    loadProperties(entryType, resources.nextElement(), null, "UTF-8");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadProperties(LibEntryType entryType, URL url, URI uri, String charSet) {
        try {
            Properties properties = new Properties();
            InputStream stream = url.openStream();
            InputStreamReader reader = new InputStreamReader(stream, charSet);
            properties.load(reader);
            load(entryType, properties, url, uri);
            reader.close();
            stream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadAntLibFragments(ClassLoader classLoader) {
        try {
            Enumeration<URL> resources = classLoader == null ? ClassLoader.getSystemResources(antLibFragment) : classLoader.getResources(antLibFragment);
            while (resources.hasMoreElements()) loadAntLib(resources.nextElement(), null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadCommonDefs(ClassLoader classLoader) {
        try {
            Enumeration<URL> resources;
            for (String[] def : commonTaskdefs) {
                URI uri = URI.create(def[1]);
                resources = classLoader == null ? ClassLoader.getSystemResources(def[0]) : classLoader.getResources(def[0]);
                while (resources.hasMoreElements()) loadProperties(LibEntryType.taskdef, resources.nextElement(), uri, "UTF-8");
            }
            for (String[] def : commonTypedefs) {
                URI uri = URI.create(def[1]);
                resources = classLoader == null ? ClassLoader.getSystemResources(def[0]) : classLoader.getResources(def[0]);
                while (resources.hasMoreElements()) loadProperties(LibEntryType.typedef, resources.nextElement(), uri, "UTF-8");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadCommonLibs(ClassLoader classLoader) {
        try {
            Enumeration<URL> resources;
            String resourceName;
            for (String[] def : commonAntlibs) {
                resourceName = def[0].replace('.', '/') + "/antlib.xml";
                URI uri = URI.create("antlib:" + def[0]);
                resources = classLoader == null ? ClassLoader.getSystemResources(resourceName) : classLoader.getResources(resourceName);
                while (resources.hasMoreElements()) loadAntLib(resources.nextElement(), uri);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadExistingAntlibs(ClassLoader classLoader) {
        URL antlibUrl;
        URI antlibUri;
        try {
            Enumeration<URL> resources = classLoader == null ? ClassLoader.getSystemResources(antLibsResource) : classLoader.getResources(antLibsResource);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                InputStream stream = url.openStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    String pkg = line.trim();
                    URI uri = URI.create("antlib:" + pkg);
                    URI resource2antlib = URI.create(antLibsResource2root + pkg.replace('.', '/') + (pkg.isEmpty() ? "" : "/") + "antlib.xml");
                    antlibUri = NetUtils.resolve(url.toURI(), resource2antlib);
                    try {
                        antlibUrl = antlibUri.toURL();
                    } catch (IllegalArgumentException e) {
                        System.err.println("base uri: " + url);
                        System.err.println("relativepath: " + resource2antlib);
                        System.err.println("target uri: " + antlibUri);
                        throw new RuntimeException(antlibUri.toString(), e);
                    }
                    loadAntLib(antlibUrl, uri);
                }
                reader.close();
                stream.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadAntLib(URL url, URI uri) {
        Element root = new ElementImpl(null, new QNameImpl("antlib"));
        try {
            ConfigUtils.load(root, url);
        } catch (Exception e) {
            throw new RuntimeException("failed to load : " + url, e);
        }
        try {
            load(root, url, uri);
        } catch (Exception e) {
            System.err.println(root.toRenderedString());
            throw new RuntimeException("failed to load : " + url, e);
        }
    }

    public void load(Element antlib, URL url, URI uri) {
        for (Child element : antlib.getChildElements()) {
            if (element.getName() != null) {
                LibEntryType entryType = LibEntryType.valueOf(element.getName());
                add(entryType.isInternal() ? new AntLibInternalEntry((Element) element, url, uri) : new AntLibExternalEntry((Element) element, url, uri));
            }
        }
    }

    public void load(LibEntryType entryType, Properties properties, URL url, URI uri) {
        if (entryType.isInternal()) throw new RuntimeException("can not load internal entryType " + entryType + " from properties.");
        for (String key : properties.stringPropertyNames()) {
            for (StringTokenizer st = new StringTokenizer(properties.getProperty(key), ",; "); st.hasMoreTokens(); ) add(new AntLibExternalEntry(entryType, key, st.nextToken(), url, uri));
        }
    }

    public void add(AntLibEntry entry) {
        if (filter.match(entry)) {
            entries.add(entry);
            for (AddedEntryListener listener : addedEntryListeners) listener.onAdded(this, entry);
        }
    }

    public Collection<AntLibEntry> getEntries() {
        return entries;
    }

    public Condition<AntLibEntry> getFilter() {
        return filter;
    }

    public void setFilter(Condition<AntLibEntry> filter) {
        if (filter == null) this.filter = True.getInstance(); else this.filter = filter;
    }

    public void writeAntLib(PrintWriter writer, String encoding) {
        writer.println(XMLUtils.header(encoding));
        writer.println("<antlib>");
        for (AntLibEntry entry : entries) {
            writer.println(entry.toRenderedString("  ", "  "));
        }
        writer.println("</antlib>");
        writer.flush();
    }

    public void writeAntLib(OutputStream outputStream, String charSet) throws UnsupportedEncodingException {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, charSet));
        writeAntLib(writer, charSet);
        writer.close();
    }

    public void addEntryListener(AddedEntryListener listener) {
        addedEntryListeners.add(listener);
    }

    public void removeEntryListener(AddedEntryListener listener) {
        addedEntryListeners.remove(listener);
    }
}
