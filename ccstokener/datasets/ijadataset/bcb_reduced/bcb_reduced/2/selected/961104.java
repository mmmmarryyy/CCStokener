package com.chimshaw.jblogeditor.metaweblog;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.xmlrpc.DefaultXmlRpcTransport;
import org.apache.xmlrpc.XmlRpc;
import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.XmlRpcTransport;
import org.eclipse.core.runtime.Plugin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.chimshaw.jblogeditor.blogs.AbstractBlog;
import com.chimshaw.jblogeditor.blogs.BlogException;
import com.chimshaw.jblogeditor.blogs.IBlogCategory;
import com.chimshaw.jblogeditor.blogs.IBlogEntry;
import com.chimshaw.jblogeditor.blogs.IBlogProvider;
import com.chimshaw.jblogeditor.blogs.IPasswordProvider;
import com.chimshaw.jblogeditor.blogs.IBlogEntry.Status;
import com.chimshaw.jblogeditor.util.TmplUtil;
import com.chimshaw.jblogeditor.util.UIUtil;
import com.chimshaw.jblogeditor.util.XMLUtil;

/**
 * @author lokeshshah@gmail.com
 *
 */
public abstract class MetaWeblogBlog extends AbstractBlog {

    private static final String CAT_ATTR_HTMLURL = "htmlurl";

    private static final String CAT_ATTR_NAME = "name";

    private static final String CAT_ATTR_ID = "id";

    protected static final String DATA_ATTR_USER = "user";

    protected static final String DATA_ATTR_HOST = "host";

    protected static final String DATA_ATTR_DIR = "dir";

    public static final String DATA_ATTR_TEMPLATE = "template";

    protected String host;

    protected String dir;

    protected String userName;

    private XmlRpcClient client = null;

    private boolean entriesLoaded = false;

    private boolean template;

    private String templateStr = null;

    /**
   * @param name
   * @param data
   */
    public MetaWeblogBlog(String name, Element data, IBlogProvider blogProvider) throws BlogException {
        super(name, blogProvider);
        dir = data.getAttribute(DATA_ATTR_DIR);
        userName = data.getAttribute(DATA_ATTR_USER);
        host = data.getAttribute(DATA_ATTR_HOST);
        String temp = data.getAttribute(DATA_ATTR_TEMPLATE);
        template = "true".equalsIgnoreCase(temp);
        if ((dir == null) || (userName == null) || (host == null)) {
            throw new BlogException("Invalid Blog Data.");
        }
        loadCategories();
    }

    public static Element makeDataNode(String dir, String host, String userName, Boolean template) throws ParserConfigurationException, IOException, SAXException {
        Element data = (Element) XMLUtil.createNode(null, "<data/>");
        data.setAttribute(DATA_ATTR_DIR, dir);
        data.setAttribute(DATA_ATTR_HOST, host);
        data.setAttribute(DATA_ATTR_USER, userName);
        if (template != null) {
            data.setAttribute(DATA_ATTR_TEMPLATE, template.toString());
        }
        return data;
    }

    public String getUser() throws UnsupportedOperationException {
        return userName;
    }

    public IBlogEntry[] getRecentEntries(int maxEntries, IPasswordProvider pwdProvider) throws BlogException {
        String password = getPassword(pwdProvider);
        if (password == null) {
            return null;
        }
        try {
            Vector<Object> params = new Vector<Object>();
            params.addElement(getBlogId());
            params.addElement(userName);
            params.addElement(password);
            params.addElement(new Integer(maxEntries));
            List<IBlogEntry> list = new ArrayList<IBlogEntry>();
            Object o = execute("metaWeblog.getRecentPosts", params);
            if (o == null) {
                throw new BlogException("Null response from server.");
            }
            Vector result = (Vector) o;
            for (int i = 0; i < result.size(); i++) {
                Hashtable entryHash = (Hashtable) result.get(i);
                IBlogEntry entry = createEntry(entryHash);
                list.add(entry);
            }
            removePublishedEntries();
            for (IBlogEntry entry : entries) {
                for (Iterator iter = list.iterator(); iter.hasNext(); ) {
                    IBlogEntry newEntry = (IBlogEntry) iter.next();
                    if (newEntry.getId().equals(entry.getId())) {
                        iter.remove();
                    }
                }
            }
            entries.addAll(list);
            IBlogEntry[] added = list.toArray(new IBlogEntry[list.size()]);
            fireEntriesAdded(added);
            return added;
        } catch (Exception ecp) {
            throw new BlogException("Error Loading Entries.", ecp);
        }
    }

    protected String getBlogId() {
        return "";
    }

    protected abstract IBlogEntry createEntry(Hashtable entryHash);

    public IBlogEntry[] getEntries() throws BlogException {
        if (!entriesLoaded) {
            loadEntries();
            entriesLoaded = true;
        }
        return super.getEntries();
    }

    private void loadEntries() throws BlogException {
        File[] dirs = (new File(dir)).listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                if (name.endsWith(".entry")) {
                    return true;
                }
                return false;
            }
        });
        for (File entryFile : dirs) {
            try {
                IBlogEntry entry = loadEntry(entryFile);
                entries.add(entry);
            } catch (BlogException ecp) {
                UIUtil.log("Error Reading BlogEntry from:" + entryFile.getAbsolutePath(), ecp);
                throw new BlogException("Error Reading BlogEntry from:" + entryFile.getAbsolutePath(), ecp);
            }
        }
    }

    protected abstract IBlogEntry loadEntry(File entryFile) throws BlogException;

    public IBlogCategory[] getCategories(boolean reload, IPasswordProvider pwdProvider) throws BlogException {
        try {
            if (!reload) {
                File catFile = new File(dir, ".categories");
                if (!catFile.exists()) {
                    reload = true;
                }
            }
            if (reload) {
                String password = getPassword(pwdProvider);
                if (password != null) {
                    reloadCategories(password);
                } else {
                    return null;
                }
            }
        } catch (Exception ecp) {
            throw new BlogException("Error Loading Categories.", ecp);
        }
        return super.getCategories(reload, pwdProvider);
    }

    protected String getPassword(IPasswordProvider pwdProvider) throws BlogException {
        if (pwdProvider == null) {
            return null;
        }
        String password = pwdProvider.getPassword(this);
        return password;
    }

    /**
   * @param password
   * @throws IOException
   * @throws XmlRpcException
   *
   */
    private void reloadCategories(String password) throws XmlRpcException, IOException {
        Vector<Object> params = new Vector<Object>();
        params.addElement(getBlogId());
        params.addElement(userName);
        params.addElement(password);
        Object output = execute("metaWeblog.getCategories", params);
        List<IBlogCategory> cat = handleFetchCategoryOutput(output, params);
        setCategories(cat);
        saveCategories();
    }

    protected List<IBlogCategory> handleFetchCategoryOutput(Object object, Vector<Object> params) {
        List<IBlogCategory> cat = new ArrayList<IBlogCategory>();
        if (object == null) {
            return cat;
        }
        if (object instanceof Hashtable) {
            Hashtable result = (Hashtable) object;
            Enumeration keys = result.keys();
            while (keys.hasMoreElements()) {
                String name = (String) keys.nextElement();
                Map obj = (Hashtable) result.get(name);
                String htmlurl = (String) obj.get("htmlUrl");
                cat.add(createCategory(name, name, htmlurl));
            }
        } else {
            UIUtil.log("MetaWeblogBlog only handles category output of type Hashtable. " + "Override this function to do custom handling");
        }
        return cat;
    }

    protected abstract IBlogCategory createCategory(String id, String name, String htmlurl);

    /**
   *
   */
    private void saveCategories() {
        String cat = "<category name=\"%1$s\" id=\"%2$s\" htmlurl=\"%3$s\"/>";
        StringBuffer sb = new StringBuffer();
        Formatter formatter = new Formatter(sb);
        try {
            File catFile = new File(dir, ".categories");
            Node cNode = XMLUtil.createNode(null, "<categories/>");
            for (IBlogCategory c : categories) {
                formatter.format(cat, c.getName(), c.getId(), c.getUrl());
                cNode.appendChild(XMLUtil.createNode(cNode.getOwnerDocument(), sb.toString()));
                sb.delete(0, sb.length());
            }
            XMLUtil.writeDocument(catFile, cNode);
        } catch (Exception ecp) {
            UIUtil.log("Error Saving Categories.", ecp);
        }
    }

    protected void loadCategories() {
        List<IBlogCategory> cat = new ArrayList<IBlogCategory>();
        File catFile = new File(dir, ".categories");
        if (!catFile.exists()) {
            return;
        }
        try {
            Document doc = XMLUtil.loadDocument(catFile);
            NodeList nodes = (NodeList) XMLUtil.evaluatePath(doc, "/categories/category", XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                if (nodes.item(i) instanceof Element) {
                    Element catElem = (Element) nodes.item(i);
                    String name = catElem.getAttribute(CAT_ATTR_NAME);
                    String id = catElem.getAttribute(CAT_ATTR_ID);
                    String htmlurl = catElem.getAttribute(CAT_ATTR_HTMLURL);
                    if (name != null) {
                        cat.add(createCategory(id, name, htmlurl));
                    }
                }
            }
            setCategories(cat);
        } catch (Exception ecp) {
            UIUtil.log("Error Loading Categories.", ecp);
        }
    }

    public void saveDraft(IBlogEntry blogEntry) throws BlogException {
        if (blogEntry instanceof MetaWeblogEntry) {
            MetaWeblogEntry entry = (MetaWeblogEntry) blogEntry;
            if (entry.getStatus() != Status.PUBLISHED) {
                saveEntryToDisk(blogEntry);
            }
            blogEntry.setDirty(false);
        }
    }

    protected abstract void saveEntryToDisk(IBlogEntry entry) throws BlogException;

    public String postEntry(IBlogEntry entry, boolean publish, IPasswordProvider pwdProvider) throws BlogException {
        if ((entry == null) || !(entry instanceof MetaWeblogEntry)) {
            throw new BlogException(new IllegalArgumentException("Invalid Entry."));
        }
        String password = getPassword(pwdProvider);
        if (password == null) {
            return null;
        }
        Vector<Object> params = new Vector<Object>();
        params.addElement(getBlogId());
        params.addElement(userName);
        params.addElement(password);
        Map<String, Object> struct = new Hashtable<String, Object>();
        String title = entry.getTitle();
        if (title != null) {
            struct.put("title", title);
        }
        String content = entry.getContent();
        if (content == null) {
            content = "";
        }
        struct.put("description", content);
        IBlogCategory[] categories = entry.getCategories();
        List<String> catNames = new Vector<String>();
        for (IBlogCategory category : categories) {
            catNames.add(category.getName());
        }
        struct.put("categories", catNames);
        Date pubDate = entry.getPublicationDate();
        if (pubDate != null) {
            struct.put("pubDate", pubDate);
        }
        params.addElement(struct);
        params.addElement(new Boolean(publish));
        String id;
        try {
            id = (String) execute("metaWeblog.newPost", params);
        } catch (Exception ecp) {
            throw new BlogException("Error making new post.", ecp);
        }
        String oldId = entry.getId();
        entry.setId(id);
        entry.setStatus(Status.PUBLISHED);
        deleteEntryFromDisk(oldId);
        entry.setDirty(false);
        return id;
    }

    public boolean editEntry(IBlogEntry entry, boolean publish, IPasswordProvider pwdProvider) throws BlogException {
        if ((entry == null) || !(entry instanceof MetaWeblogEntry)) {
            throw new BlogException(new IllegalArgumentException("Invalid Entry."));
        }
        if (entry.getStatus() == Status.DRAFT) {
            throw new BlogException("Draft Entries cannot be edited.");
        }
        String password = getPassword(pwdProvider);
        if (password == null) {
            return false;
        }
        Vector<Object> params = new Vector<Object>();
        params.addElement(entry.getId());
        params.addElement(userName);
        params.addElement(password);
        Map<String, Object> struct = new Hashtable<String, Object>();
        String title = entry.getTitle();
        if (title != null) {
            struct.put("title", title);
        }
        String content = entry.getContent();
        if (content == null) {
            content = "";
        }
        struct.put("description", content);
        IBlogCategory[] categories = entry.getCategories();
        List<String> catNames = new Vector<String>();
        for (IBlogCategory category : categories) {
            catNames.add(category.getName());
        }
        struct.put("categories", catNames);
        Date pubDate = entry.getPublicationDate();
        if (pubDate != null) {
            struct.put("pubDate", pubDate);
        }
        params.addElement(struct);
        params.addElement(new Boolean(publish));
        boolean success;
        try {
            success = (Boolean) execute("metaWeblog.editPost", params);
        } catch (Exception ecp) {
            throw new BlogException("Error posting edited entry.", ecp);
        }
        if (success) {
            String oldId = entry.getId();
            entry.setStatus(Status.PUBLISHED);
            deleteEntryFromDisk(oldId);
            entry.setDirty(false);
            return true;
        }
        throw new BlogException("Edit Entry returned false.");
    }

    /**
   * @param id
   */
    protected void deleteEntryFromDisk(String id) {
        File file = new File(dir, id + ".entry");
        file.delete();
    }

    public boolean deleteEntry(String id, IPasswordProvider pwdProvider) throws BlogException {
        IBlogEntry entryById = getEntryById(id);
        if (entryById.getStatus() == IBlogEntry.Status.DRAFT) {
            deleteEntryFromDisk(id);
            removeEntry(id);
            return true;
        }
        String password = getPassword(pwdProvider);
        if (password == null) {
            return false;
        }
        Vector<Object> params = new Vector<Object>();
        params.add(getBlogId());
        params.add(id);
        params.add(userName);
        params.add(password);
        params.add(Boolean.TRUE);
        try {
            execute("blogger.deletePost", params);
            removeEntry(id);
            return true;
        } catch (Exception ecp) {
            throw new BlogException("Error deleting post.", ecp);
        }
    }

    public IBlogEntry getEntry(String id, IPasswordProvider pwdProvider) throws BlogException {
        String password = getPassword(pwdProvider);
        if (password == null) {
            return null;
        }
        Vector<Object> params = new Vector<Object>();
        params.addElement(id);
        params.addElement(userName);
        params.addElement(password);
        Hashtable result;
        try {
            result = (Hashtable) execute("metaWeblog.getPost", params);
        } catch (Exception ecp) {
            throw new BlogException("Error getting post.", ecp);
        }
        return createEntry(result, this);
    }

    protected abstract IBlogEntry createEntry(Hashtable result, MetaWeblogBlog blog);

    public Object execute(String method, Vector<Object> params) throws XmlRpcException, IOException {
        if (client == null) {
            client = new XmlRpcClient(host);
            XmlRpc.setDefaultInputEncoding("UTF8");
        }
        XmlRpcRequest xmlRpcRequest = new XmlRpcRequest(method, params);
        XmlRpcTransport transport = new DefaultXmlRpcTransport(new URL(host)) {

            public java.io.InputStream sendXmlRpc(byte[] request) throws IOException {
                con = url.openConnection();
                con.setDoInput(true);
                con.setDoOutput(true);
                con.setUseCaches(false);
                con.setAllowUserInteraction(false);
                con.setRequestProperty("Content-Length", Integer.toString(request.length));
                con.setRequestProperty("Content-Type", "text/xml");
                if (auth != null) {
                    con.setRequestProperty("Authorization", "Basic " + auth);
                }
                con.setConnectTimeout(1000);
                OutputStream out = con.getOutputStream();
                out.write(request);
                out.flush();
                out.close();
                return con.getInputStream();
            }

            ;
        };
        Object o = client.execute(xmlRpcRequest, transport);
        if (o instanceof XmlRpcException) {
            throw (XmlRpcException) o;
        }
        return o;
    }

    public void setUser(String name) throws UnsupportedOperationException {
        this.userName = name;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public boolean isUseTemplate() {
        return template;
    }

    public void setUseTemplate(boolean use) {
        template = use;
    }

    @Override
    public String getDisplayHtml(String entryTitle, String editorContents, IBlogEntry entry) {
        if (!template) {
            return super.getDisplayHtml(entryTitle, editorContents, entry);
        }
        try {
            if (templateStr == null) {
                templateStr = TmplUtil.loadTemplate(getPlugin(), getDir());
            }
            Velocity.init();
            VelocityContext context = new VelocityContext();
            context.put("title", getName());
            context.put("date", new DateTool());
            context.put("entryTitle", entryTitle);
            Date date = (entry.getPublicationDate() != null) ? entry.getPublicationDate() : new Date();
            context.put("entryDate", date);
            context.put("entryBody", editorContents);
            context.put("user", getUser());
            IBlogCategory[] entryCats = entry.getCategories();
            String[] cats = new String[entryCats.length];
            for (int i = 0; i < entryCats.length; i++) {
                cats[i] = entryCats[i].getName();
            }
            context.put("entryCategory", cats);
            StringWriter writer = new StringWriter();
            Velocity.evaluate(context, writer, "blogger", templateStr);
            writer.flush();
            return writer.toString();
        } catch (Exception ecp) {
            UIUtil.log("Error Converting Template", ecp);
        }
        return super.getDisplayHtml(entryTitle, editorContents, entry);
    }

    public abstract Plugin getPlugin();
}
