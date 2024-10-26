package tufts.vue;

import java.awt.*;
import java.applet.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import javax.swing.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Multiset;
import com.jgoodies.looks.LookUtils;
import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;
import com.sun.org.apache.xerces.internal.impl.XMLScanner;
import edu.tufts.vue.mbs.AnalyzerResult;
import tufts.vue.ds.DataAction;
import tufts.vue.ds.DataTree;
import tufts.vue.ds.Schema;
import tufts.vue.ds.XMLIngest;
import tufts.vue.ds.XmlDataSource;
import tufts.vue.gui.GUI;
import tufts.vue.gui.VueMenuBar;

/**
 * Experimental VUE applet.
 * 
 * @version $Revision: 1.25 $ / $Date: 2010-02-03 19:17:40 $ / $Author: mike $
 */
public class VueApplet extends JApplet {

    private static boolean isInited = false;

    private static boolean firstInit = true;

    private static MapViewer viewer = null;

    private static JPanel toolbarPanel = null;

    private static JComponent toolbar = null;

    private static boolean fullyStopped = false;

    private static boolean isZotero = false;

    private static MapTabbedPane mMapTabbedPane = null;

    private static final String zoteroPlugin = "zoteroPlugin";

    private static JApplet instance;

    public VueApplet() {
        instance = this;
        this.getRootPane().setDoubleBuffered(true);
    }

    public void init() {
        super.init();
        this.setBackground(new Color(225, 225, 225));
        try {
            UIManager.put("ClassLoader", LookUtils.class.getClassLoader());
            UIManager.setLookAndFeel(new Plastic3DLookAndFeel());
        } catch (Exception e) {
            System.out.println("Couldn't load jlooks look and feel");
        }
    }

    public static String getActiveMapPath() {
        return VUE.getActiveMap().getFile().getAbsolutePath();
    }

    public static String getActiveMapDisplayTitle() {
        return VUE.getActiveMap().getDisplayLabel();
    }

    public static JApplet getInstance() {
        return instance;
    }

    public void start() {
        super.start();
        instance = this;
        VUE.setAppletContext(this.getAppletContext());
        if (!firstInit) while (!fullyStopped) {
            try {
                Thread.sleep(3000);
                System.out.println("WAIT");
            } catch (InterruptedException e) {
            }
        }
        fullyStopped = false;
        super.init();
        processAppletParameters();
        getContentPane().setLayout(new BorderLayout());
        msg("init\n\tapplet=" + Integer.toHexString(hashCode()) + "\n\tcontext=" + getAppletContext());
        VUE.initUI();
        VUE.initApplication();
        loadViewer();
        if (!VUE.getLeftTabbedPane().isEnabled()) VUE.getLeftTabbedPane().setEnabled(true);
        VUE.initDataSources();
        GUI.invokeAfterAWT(new Runnable() {

            public void run() {
                final DRBrowser DR_BROWSER = VUE.getDRBrowser();
                if (DR_BROWSER != null) DR_BROWSER.loadDataSourceViewer();
            }
        });
        isInited = true;
    }

    public void ToggleAllVisible() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                tufts.vue.gui.DockWindow.ToggleAllVisible();
            }
        });
    }

    public boolean AllWindowsHidden() {
        return tufts.vue.gui.DockWindow.AllWindowsHidden();
    }

    public void ShowPreviouslyHiddenWindows() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                tufts.vue.gui.DockWindow.ShowPreviouslyHiddenWindows();
            }
        });
    }

    public void HideAllDockWindows() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                tufts.vue.gui.DockWindow.HideAllWindows();
            }
        });
    }

    public void stop() {
        super.stop();
    }

    public void destroy() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                tufts.vue.gui.DockWindow.HideAllWindows();
                EditorManager.destroy();
                VueToolbarController.destroyController();
                VUE.getActiveViewer().destroyContextMenus();
            }
        });
        fullyStopped = true;
        isInited = false;
        msg("stop");
    }

    public static boolean isInited() {
        return isInited;
    }

    public static boolean isZoteroApplet() {
        return isZotero;
    }

    private final void processAppletParameters() {
        String zoteroPlugin = this.getParameter(this.zoteroPlugin);
        if (zoteroPlugin != null) zoteroPlugin = zoteroPlugin.toLowerCase();
        if (zoteroPlugin != null && zoteroPlugin.equals("true")) {
            isZotero = true;
        }
    }

    public static String getActiveMapItems() {
        LWMap active = VUE.getActiveMap();
        java.util.Iterator it = active.getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
        String items = "";
        do {
            if (!it.hasNext()) break;
            LWComponent n = (LWComponent) it.next();
            if (n instanceof LWNode) {
                items = (new StringBuilder()).append(items).append(n.getLabel()).append(",").toString();
                String id = null;
                try {
                    id = n.getResource().getProperty("Zotero.id");
                } catch (Exception e) {
                    System.out.println("Exception in zotero import from vue: no zotero id");
                }
                if (id == null) items = (new StringBuilder()).append(items).append("none,").toString(); else items = (new StringBuilder()).append(items).append(id).append(",").toString();
                String url = null;
                try {
                    url = n.getResource().getSpec();
                } catch (Exception e) {
                    System.out.println("Exception in zotero import from vue: no zotero url");
                }
                if (url == null) items = (new StringBuilder()).append(items).append("none").toString(); else items = (new StringBuilder()).append(items).append(url).toString();
                items = (new StringBuilder()).append(items).append("\n").toString();
            }
        } while (true);
        return items;
    }

    public synchronized void loadViewer() {
        msg("got viewer");
        msg("is applet ? " + VUE.isApplet());
        msg("setting menu bar...");
        VueMenuBar vmb = new VueMenuBar();
        vmb.setBorderPainted(false);
        setJMenuBar(vmb);
        final VueToolbarController tbc = VueToolbarController.getController();
        toolbar = tbc.getToolbar().getMainToolbar();
        toolbarPanel = VUE.constructToolPanel(toolbar);
        getContentPane().setBackground(toolbarPanel.getBackground());
        getContentPane().add(toolbarPanel, BorderLayout.NORTH);
        getContentPane().add(VUE.getLeftTabbedPane(), BorderLayout.CENTER);
        validate();
        VUE.displayMap(new LWMap("New Map"));
        msg("validating...");
        msg("loading complete");
        firstInit = false;
    }

    public void setSize(int width, int height) {
        super.setSize(width, height);
        this.getContentPane().setSize(width, height);
        Container c = this.getParent();
        while (c != null) {
            c.setSize(width, height);
            c.validate();
            c = c.getParent();
        }
    }

    private void msg(String s) {
        System.out.println("VueApplet: " + s);
    }

    @SuppressWarnings("unchecked")
    public static void displayZoteroExport(final String urlString) {
        AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                try {
                    File tempFile = null;
                    tempFile = File.createTempFile(new Long(System.currentTimeMillis()).toString(), null);
                    InputStream io = null;
                    io = getInputStream(urlString);
                    byte[] buf = new byte[256];
                    int read = 0;
                    java.io.FileOutputStream fos = null;
                    fos = new java.io.FileOutputStream(tempFile);
                    while ((read = io.read(buf)) > 0) {
                        fos.write(buf, 0, read);
                    }
                    tufts.vue.action.TextOpenAction.displayMap(tempFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
    }

    public static boolean isGuiInited() {
        return GUI.isGUIInited();
    }

    public static String getActiveResourceSpec() {
        LWSelection selection = VUE.getActiveViewer().getSelection();
        if (!selection.isEmpty()) {
            return selection.get(0).getResource().getSpec();
        } else return null;
    }

    @SuppressWarnings("unchecked")
    public static void addLinksToMap(final String content) {
        AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                javax.xml.parsers.DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setIgnoringElementContentWhitespace(true);
                factory.setIgnoringComments(true);
                factory.setValidating(false);
                InputStream is;
                try {
                    is = new java.io.ByteArrayInputStream(content.getBytes("UTF-8"));
                    final org.w3c.dom.Document doc = factory.newDocumentBuilder().parse((InputStream) is);
                    NodeList nodeLst = doc.getElementsByTagName("link");
                    Multimap<String, String> map = Multimaps.newArrayListMultimap();
                    for (int s = 0; s < nodeLst.getLength(); s++) {
                        Node fstNode = nodeLst.item(s);
                        if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
                            NamedNodeMap atts = fstNode.getAttributes();
                            Node n = atts.item(0);
                            Node val = atts.item(1);
                            map.put(n.getNodeValue(), val.getNodeValue());
                        }
                    }
                    java.util.Collection<LWComponent> comps = VUE.getActiveMap().getAllDescendents();
                    java.util.Iterator<LWComponent> iter = comps.iterator();
                    HashMap<String, LWNode> dataRowNodes = new HashMap<String, LWNode>();
                    while (iter.hasNext()) {
                        LWComponent comp = iter.next();
                        if (comp.isDataRowNode()) {
                            String fromId = comp.getDataValue("id");
                            dataRowNodes.put(fromId, (LWNode) comp);
                        }
                    }
                    Multiset<String> keys = map.keys();
                    java.util.Iterator<String> linkIterator = keys.iterator();
                    while (linkIterator.hasNext()) {
                        String fromId = linkIterator.next();
                        Collection<String> toIds = map.get(fromId);
                        java.util.Iterator<String> toIdIterator = toIds.iterator();
                        while (toIdIterator.hasNext()) {
                            String tid = toIdIterator.next();
                            LWNode fromNode = dataRowNodes.get(fromId);
                            LWNode toNode = dataRowNodes.get(tid);
                            if (fromNode != null && toNode != null) {
                                LWLink link = new LWLink(fromNode, toNode);
                                VUE.getActiveMap().add(link);
                            }
                        }
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                }
                return null;
            }

            ;
        });
    }

    @SuppressWarnings("unchecked")
    public static void addNotesToMap(final String content) {
        AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                javax.xml.parsers.DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setIgnoringElementContentWhitespace(true);
                factory.setIgnoringComments(true);
                factory.setValidating(false);
                InputStream is;
                try {
                    is = new java.io.ByteArrayInputStream(content.getBytes("UTF-8"));
                    final org.w3c.dom.Document doc = factory.newDocumentBuilder().parse((InputStream) is);
                    NodeList nodeLst = doc.getElementsByTagName("note");
                    HashMap<String, String> map = new HashMap<String, String>();
                    for (int s = 0; s < nodeLst.getLength(); s++) {
                        Node fstNode = nodeLst.item(s);
                        if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
                            NamedNodeMap atts = fstNode.getAttributes();
                            Node n = atts.item(0);
                            NodeList noteList = fstNode.getChildNodes();
                            map.put(n.getNodeValue(), ((Node) noteList.item(0)).getNodeValue().trim());
                        }
                    }
                    java.util.Collection<LWComponent> comps = VUE.getActiveMap().getAllDescendents();
                    java.util.Iterator<LWComponent> iter = comps.iterator();
                    HashMap<String, LWNode> dataRowNodes = new HashMap<String, LWNode>();
                    while (iter.hasNext()) {
                        LWComponent comp = iter.next();
                        if (comp.isDataRowNode()) {
                            String fromId = comp.getDataValue("id");
                            dataRowNodes.put(fromId, (LWNode) comp);
                        }
                    }
                    java.util.Set<String> keys = map.keySet();
                    java.util.Iterator<String> notesIterator = keys.iterator();
                    while (notesIterator.hasNext()) {
                        String id = notesIterator.next();
                        LWNode fromNode = dataRowNodes.get(id);
                        if (fromNode != null) {
                            String note = map.get(id);
                            fromNode.setNotes(note);
                        }
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                }
                return null;
            }

            ;
        });
    }

    public static String getActiveResourceTitle() {
        LWSelection selection = VUE.getActiveViewer().getSelection();
        if (!selection.isEmpty()) {
            return selection.get(0).getResource().getTitle();
        } else return null;
    }

    private static boolean sourceAdded = false;

    @SuppressWarnings("unchecked")
    public static void addZoteroDatasource(final String collectionName, final String fileString, final boolean addToMap) {
        sourceAdded = false;
        try {
            DataSourceList l = DataSetViewer.getDataSetList();
            int length = l.getModel().getSize();
            for (int i = 0; i < length; i++) {
                Object o = l.getModel().getElementAt(i);
                if (o instanceof XmlDataSource) {
                    final XmlDataSource xmlds = (XmlDataSource) o;
                    if (xmlds.getDisplayName().equals(collectionName) && xmlds.getAddress().equals(fileString) && !sourceAdded) {
                        sourceAdded = true;
                        VUE.getContentDock().setVisible(true);
                        VUE.getContentPanel().showDatasetsTab();
                        SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                VUE.getContentPanel().getDSBrowser().getDataSetViewer().setActiveDataSource(xmlds);
                                VUE.getContentPanel().getDSBrowser().getDataSetViewer().refreshBrowser();
                            }
                        });
                        if (addToMap) {
                            boolean added = false;
                            int tries = 0;
                            while (!added && tries < 10) {
                                try {
                                    added = true;
                                    List<LWComponent> nodes = DataAction.makeRowNodes(xmlds.getSchema());
                                    LWMap map = VUE.getActiveMap();
                                    for (LWComponent component : nodes) {
                                        map.add(component);
                                    }
                                    LayoutAction.random.act(new LWSelection(nodes));
                                } catch (NullPointerException npe) {
                                    System.out.println(npe.toString());
                                    added = false;
                                    tries++;
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (sourceAdded) return;
            String xml;
            final tufts.vue.DataSource ds = new XmlDataSource(collectionName, fileString);
            Properties props = new Properties();
            props.put("displayName", collectionName);
            props.put("name", collectionName);
            props.put("address", fileString);
            props.put("item_path", "zoteroCollection.zoteroItem");
            props.put("key_field", "zoteroCollection.zoteroItem.id");
            ds.setConfiguration(props);
            final BrowseDataSource bds = (BrowseDataSource) ds;
            VUE.getContentDock().setVisible(true);
            VUE.getContentPanel().showDatasetsTab();
            DataSetViewer.getDataSetList().addOrdered(ds);
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    VUE.getContentPanel().getDSBrowser().getDataSetViewer().setActiveDataSource(ds);
                }
            });
            DataSourceViewer.saveDataSourceViewer();
            if (addToMap) {
                XmlDataSource xmlds = (XmlDataSource) ds;
                boolean added = false;
                int tries = 0;
                while (!added && tries < 10) {
                    try {
                        added = true;
                        List<LWComponent> nodes = DataAction.makeRowNodes(xmlds.getSchema());
                        LWMap map = VUE.getActiveMap();
                        for (LWComponent component : nodes) {
                            map.add(component);
                        }
                        LayoutAction.random.act(new LWSelection(nodes));
                    } catch (NullPointerException npe) {
                        added = false;
                        tries++;
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void displayMap(String urlString) {
        innerDisplayMap(urlString);
    }

    public static void displayLocalMap(String fileString) {
        innerDisplayLocalMap(fileString);
    }

    @SuppressWarnings("unchecked")
    private static void innerDisplayMap(final String urlString) {
        AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                URL url;
                try {
                    url = new URL(urlString);
                    tufts.vue.action.OpenURLAction.displayMap(url);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static void innerDisplayLocalMap(final String fileString) {
        AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                tufts.vue.action.OpenAction.displayMap(new File(fileString));
                return null;
            }
        });
    }

    public static InputStream getInputStream(String fileName) throws IOException {
        InputStream input;
        if (fileName.startsWith("http:")) {
            URL url = new URL(fileName);
            URLConnection connection = url.openConnection();
            input = connection.getInputStream();
        } else {
            input = new FileInputStream(fileName);
        }
        return input;
    }

    protected static MapViewer getMapViewer() {
        return viewer;
    }

    protected static MapTabbedPane getMapTabbedPane() {
        return mMapTabbedPane;
    }
}
