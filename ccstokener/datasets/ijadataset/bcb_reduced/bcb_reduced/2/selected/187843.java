package de.anormalmedia.sbstutorial.gui.selector;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import de.anormalmedia.sbstutorial.SBSTutorial;
import de.anormalmedia.sbstutorial.Tutorial;
import de.anormalmedia.sbstutorial.gui.ComponentFinder;
import de.anormalmedia.sbstutorial.store.FileStore;
import de.anormalmedia.sbstutorial.store.URLStore;

/**
 * A dialog that can list tutorials, display a preview and start the selected entry.
 * @author anormal
 */
public class TutorialSelector extends JDialog implements ListSelectionListener, MouseListener {

    private static final long serialVersionUID = 1L;

    private ResourceBundle languageResource;

    private Window parent;

    private String filepath;

    private URL jarpath;

    private JButton btStart;

    private JButton btCancel;

    private JButton btHelp;

    private JTextField tfPath;

    private JList tuts;

    private final JTextPane textpane;

    private ArrayList<ResourceBundle> externalResourceBundles = new ArrayList<ResourceBundle>();

    private final HashMap<String, URL> additionalEntries;

    /**
     * Creates the tutorial selector for tutorials at the given file path
     * @param parent the parent dialog
     * @param filepath the path to search the tutorials in
     * @param textpane an optional textpane to use
     */
    public TutorialSelector(JDialog parent, String filepath, JTextPane textpane) {
        super(parent);
        this.parent = parent;
        this.filepath = filepath;
        this.additionalEntries = null;
        this.jarpath = null;
        if (textpane == null) {
            textpane = new JTextPane();
            textpane.setEditable(false);
        }
        this.textpane = textpane;
        initGui();
    }

    /**
     * Creates the tutorial selector for tutorials at the given file path
     * @param parent the parent frame
     * @param filepath the path to search the tutorials in
     * @param textpane an optional textpane to use
     */
    public TutorialSelector(JFrame parent, String filepath, JTextPane textpane) {
        super(parent);
        this.parent = parent;
        this.filepath = filepath;
        this.jarpath = null;
        this.additionalEntries = null;
        if (textpane == null) {
            textpane = new JTextPane();
            textpane.setEditable(false);
        }
        this.textpane = textpane;
        initGui();
    }

    /**
     * Creates the tutorial selector for tutorials at the given file path
     * @param parent the parent frame
     * @param jarpath the jar url to search the tutorials in
     * @param textpane an optional textpane to use
     */
    public TutorialSelector(JFrame parent, URL jarpath, JTextPane textpane) {
        super(parent);
        this.parent = parent;
        this.filepath = null;
        this.additionalEntries = null;
        this.jarpath = jarpath;
        if (textpane == null) {
            textpane = new JTextPane();
        }
        this.textpane = textpane;
        initGui();
    }

    /**
     * Creates the tutorial selector for tutorials at the given file path
     * @param parent the parent dialog
     * @param jarpath the jar url to search the tutorials in
     * @param textpane an optional textpane to use
     */
    public TutorialSelector(JDialog parent, URL jarpath, JTextPane textpane) {
        super(parent);
        this.parent = parent;
        this.filepath = null;
        this.additionalEntries = null;
        this.jarpath = jarpath;
        if (textpane == null) {
            textpane = new JTextPane();
        }
        this.textpane = textpane;
        initGui();
    }

    /**
     * Creates the tutorial selector for tutorials at the given file path
     * @param parent the parent dialog
     * @param filepath the path to search the tutorials in
     * @param textpane an optional textpane to use
     * @param additionalEntries a map of additional urls to tutorials
     */
    public TutorialSelector(JDialog parent, String filepath, JTextPane textpane, LinkedHashMap<String, URL> additionalEntries) {
        super(parent);
        this.parent = parent;
        this.filepath = filepath;
        this.additionalEntries = additionalEntries;
        this.jarpath = null;
        if (textpane == null) {
            textpane = new JTextPane();
            textpane.setEditable(false);
        }
        this.textpane = textpane;
        initGui();
    }

    /**
     * Creates the tutorial selector for tutorials at the given file path
     * @param parent the parent frame
     * @param filepath the path to search the tutorials in
     * @param textpane an optional textpane to use
     * @param additionalEntries a map of additional urls to tutorials
     */
    public TutorialSelector(JFrame parent, String filepath, JTextPane textpane, LinkedHashMap<String, URL> additionalEntries) {
        super(parent);
        this.parent = parent;
        this.filepath = filepath;
        this.additionalEntries = additionalEntries;
        this.jarpath = null;
        if (textpane == null) {
            textpane = new JTextPane();
            textpane.setEditable(false);
        }
        this.textpane = textpane;
        initGui();
    }

    /**
     * Creates the tutorial selector for tutorials at the given file path
     * @param parent the parent frame
     * @param jarpath the jar url to search the tutorials in
     * @param textpane an optional textpane to use
     * @param additionalEntries a map of additional urls to tutorials
     */
    public TutorialSelector(JFrame parent, URL jarpath, JTextPane textpane, LinkedHashMap<String, URL> additionalEntries) {
        super(parent);
        this.parent = parent;
        this.additionalEntries = additionalEntries;
        this.filepath = null;
        this.jarpath = jarpath;
        if (textpane == null) {
            textpane = new JTextPane();
        }
        this.textpane = textpane;
        initGui();
    }

    /**
     * Creates the tutorial selector for tutorials at the given file path
     * @param parent the parent dialog
     * @param jarpath the jar url to search the tutorials in
     * @param textpane an optional textpane to use
     * @param additionalEntries a map of additional urls to tutorials
     */
    public TutorialSelector(JDialog parent, URL jarpath, JTextPane textpane, LinkedHashMap<String, URL> additionalEntries) {
        super(parent);
        this.parent = parent;
        this.additionalEntries = additionalEntries;
        this.filepath = null;
        this.jarpath = jarpath;
        if (textpane == null) {
            textpane = new JTextPane();
        }
        this.textpane = textpane;
        initGui();
    }

    /**
     * Initializes the gui of the selector
     */
    private void initGui() {
        languageResource = ResourceBundle.getBundle("de.anormalmedia.sbstutorial.i18n.TutorialSelector");
        setTitle(getLabelText("gui.frame.title"));
        setModal(true);
        setName("de.anormalmedia.sbstutorial.TutorialSelector");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel content = (JPanel) getContentPane();
        content.setLayout(new BorderLayout());
        content.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        content.getActionMap().put("cancel", new AbstractAction() {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                TutorialSelector.this.dispose();
            }
        });
        JPanel roopathpane = new JPanel(new BorderLayout(5, 5));
        roopathpane.setBorder(new EmptyBorder(5, 5, 5, 5));
        content.add(roopathpane, BorderLayout.NORTH);
        JLabel lblPath = new JLabel(getLabelText("gui.path.title"));
        roopathpane.add(lblPath, BorderLayout.WEST);
        tfPath = new JTextField(filepath != null ? filepath : getLabelText("gui.path.jarfile"), 30);
        tfPath.setEditable(false);
        roopathpane.add(tfPath, BorderLayout.CENTER);
        JButton btPath = new JButton(getLabelText("gui.path.browse"));
        btPath.addActionListener(new AbstractAction() {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                File currentPath = new File(TutorialSelector.this.filepath != null ? TutorialSelector.this.filepath : ".");
                JFileChooser fc = new JFileChooser(currentPath);
                fc.setCurrentDirectory(currentPath);
                fc.setAcceptAllFileFilterUsed(false);
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int result = fc.showOpenDialog(TutorialSelector.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fc.getSelectedFile();
                    if (selectedFile != null && selectedFile.exists() && selectedFile.isDirectory()) {
                        textpane.setText("");
                        TutorialSelector.this.filepath = selectedFile.getAbsolutePath();
                        tfPath.setText(TutorialSelector.this.filepath);
                        tuts.setModel(new TutorialListModel(filepath));
                        markFirstIndex();
                    }
                }
            }
        });
        roopathpane.add(btPath, BorderLayout.EAST);
        JPanel selection = new JPanel();
        selection.setBorder(new EmptyBorder(10, 10, 10, 10));
        selection.setLayout(new BorderLayout());
        selection.add(new JLabel(getLabelText("gui.selection.title")), BorderLayout.NORTH);
        TutorialListModel tutorialListModel = null;
        if (filepath != null) {
            tutorialListModel = new TutorialListModel(filepath);
        } else if (jarpath != null) {
            tutorialListModel = new TutorialListModel(jarpath);
        } else {
            tutorialListModel = new TutorialListModel(".");
        }
        if (additionalEntries != null) {
            addEntries(additionalEntries, tutorialListModel);
        }
        tuts = new JList(tutorialListModel);
        tuts.setCellRenderer(new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component listCellRendererComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (listCellRendererComponent instanceof JLabel) {
                    JLabel lbl = (JLabel) listCellRendererComponent;
                    if (value instanceof String) {
                        String strVal = (String) value;
                        if (strVal.matches("^\\d{3,3}\\s.*")) {
                            lbl.setText(strVal.substring(4));
                        }
                    }
                }
                return listCellRendererComponent;
            }
        });
        tuts.addListSelectionListener(this);
        tuts.addMouseListener(this);
        selection.add(new JScrollPane(tuts), BorderLayout.CENTER);
        JPanel preview = new JPanel();
        preview.setBorder(new EmptyBorder(10, 10, 10, 10));
        preview.setLayout(new BorderLayout());
        preview.add(new JLabel(getLabelText("gui.preview.title")), BorderLayout.NORTH);
        textpane.setContentType("text/html");
        textpane.setPreferredSize(new Dimension(300, 300));
        preview.add(new JScrollPane(textpane), BorderLayout.CENTER);
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        sp.setLeftComponent(selection);
        sp.setRightComponent(preview);
        content.add(sp, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        content.add(buttons, BorderLayout.SOUTH);
        btStart = new JButton(getLabelText("gui.button.start"));
        btStart.addActionListener(new AbstractAction() {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                Object selectedValue = ((TutorialListModel) tuts.getModel()).getTutorialAt(tuts.getSelectedIndex());
                if (selectedValue != null && selectedValue instanceof Tutorial) {
                    try {
                        SBSTutorial sbsTutorial = new SBSTutorial();
                        sbsTutorial.setStore(((Tutorial) selectedValue).getStore());
                        sbsTutorial.setParentWindow(parent);
                        sbsTutorial.setSuppressIntro(true);
                        sbsTutorial.setTextpane(textpane);
                        sbsTutorial.start();
                        TutorialSelector.this.dispose();
                    } catch (FileNotFoundException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        btStart.setEnabled(false);
        btCancel = new JButton(getLabelText("gui.button.cancel"));
        btCancel.addActionListener(new AbstractAction() {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                TutorialSelector.this.dispose();
            }
        });
        btHelp = new JButton(getLabelText("gui.button.help"));
        btHelp.addActionListener(new AbstractAction() {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                final JDialog helpdialog = new JDialog(TutorialSelector.this, getLabelText("gui.button.help"));
                helpdialog.setModal(true);
                JPanel helpcontent = (JPanel) helpdialog.getContentPane();
                helpcontent.setLayout(new BorderLayout());
                helpcontent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelhelp");
                helpcontent.getActionMap().put("cancelhelp", new AbstractAction() {

                    private static final long serialVersionUID = 1L;

                    public void actionPerformed(ActionEvent e) {
                        helpdialog.dispose();
                    }
                });
                JTextPane tp = new JTextPane();
                tp.setContentType("text/html");
                tp.setText(getLabelText("gui.help"));
                tp.setEditable(false);
                tp.setCaretPosition(0);
                helpcontent.add(new JScrollPane(tp), BorderLayout.CENTER);
                helpdialog.setSize(400, 300);
                helpdialog.setLocationRelativeTo(TutorialSelector.this);
                helpdialog.setVisible(true);
            }
        });
        if (System.getProperty("os.name").toLowerCase().indexOf("windows") == -1) {
            buttons.add(btHelp);
            buttons.add(btCancel);
            buttons.add(btStart);
        } else {
            buttons.add(btStart);
            buttons.add(btCancel);
            buttons.add(btHelp);
        }
        pack();
        if (parent != null) {
            setLocationRelativeTo(parent);
        }
        markFirstIndex();
        setVisible(true);
    }

    /**
     * Adds an external resource bundle to search in alternatively
     * @param externalResourceBundle an external resource bundle to search in alternatively
     */
    public void addExternalResourceBundle(ResourceBundle externalResourceBundle) {
        this.externalResourceBundles.add(externalResourceBundle);
    }

    /**
     * Selects the first tutorial in the list
     */
    private void markFirstIndex() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                if (tuts.getModel().getSize() > 0) {
                    tuts.setSelectedIndex(0);
                }
            }
        });
    }

    /**
     * Returns the text for labeling components
     * @param key the key in the language resource
     * @return the text to be displayed
     */
    private String getLabelText(String key) {
        if (languageResource == null) {
            return key;
        }
        String value = languageResource.getString(key);
        if (value == null) {
            return key;
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }
        JList list = (JList) e.getSource();
        final TutorialListModel model = (TutorialListModel) tuts.getModel();
        final int index = list.getSelectedIndex();
        Object selectedValue = null;
        URL localTutUrl = null;
        if (index >= 0) {
            selectedValue = model.getTutorialAt(index);
            localTutUrl = model.getUrlAt(index);
        }
        final URL tuturl = localTutUrl;
        btStart.setEnabled(selectedValue != null);
        if (selectedValue instanceof Tutorial) {
            Tutorial tutorial = (Tutorial) selectedValue;
            String introDescription = tutorial.getIntroDescription();
            if (introDescription != null) {
                String baseURL = tutorial.getStore().getUrl().toString().substring(0, tutorial.getStore().getUrl().toString().lastIndexOf('/') + 1);
                introDescription = introDescription.replace("<head>", "<head><base href=\"" + baseURL + "\" />");
                textpane.setText(introDescription);
            } else {
                textpane.setText(getLabelText("gui.preview.nointro"));
            }
        } else {
            TutorialSelector.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            new Thread(new Runnable() {

                public void run() {
                    try {
                        Tutorial tutorial = new Tutorial(new URLStore(tuturl), false);
                        model.setTutorialAt(index, tutorial);
                        String introDescription = tutorial.getIntroDescription();
                        if (!textpane.isShowing()) {
                            return;
                        }
                        if (introDescription != null) {
                            String baseURL = tutorial.getStore().getUrl().toString().substring(0, tutorial.getStore().getUrl().toString().lastIndexOf('/') + 1);
                            introDescription = introDescription.replace("<head>", "<head><base href=\"" + baseURL + "\" />");
                            textpane.setText(introDescription);
                        } else {
                            textpane.setText(getLabelText("gui.preview.nointro"));
                        }
                    } catch (Exception e1) {
                        textpane.setText(getLabelText("gui.preview.nointro"));
                    } finally {
                        SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                tuts.setEnabled(true);
                                Object selectedValue = model.getTutorialAt(index);
                                btStart.setEnabled(selectedValue != null);
                                TutorialSelector.this.setCursor(Cursor.getDefaultCursor());
                            }
                        });
                    }
                }
            }).start();
        }
    }

    /**
     * Adds the entries from the map to the given model
     * @param additionalEntries the map of additional urls to tutorials
     * @param model the current model to add the tutorials at
     */
    private void addEntries(HashMap<String, URL> additionalEntries, TutorialListModel model) {
        for (Entry<String, URL> entry : additionalEntries.entrySet()) {
            model.insertAdditionalUrl(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Searches the list of open windows and closes each {@link TutorialSelector} dialog.
     */
    public static void closeAllSelectors() {
        ArrayList<Window> allWindows = ComponentFinder.getAllWindows();
        if (allWindows != null) {
            for (Window wind : allWindows) {
                if (wind instanceof TutorialSelector) {
                    TutorialSelector ts = (TutorialSelector) wind;
                    ts.dispose();
                }
            }
        }
    }

    /**
     * FOR INTERNAL USE ONLY
     * @param args ignored
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setSize(800, 600);
        frame.setLocation(100, 100);
        frame.setVisible(true);
        LinkedHashMap<String, URL> additionalEntries = new LinkedHashMap<String, URL>();
        try {
            additionalEntries.put("anormal media", new URL("http://anormal-media.de"));
            additionalEntries.put("anormal tracker", new URL("http://anormal-tracker.de"));
            additionalEntries.put("bla", new URL("file:D:\\Work\\jnlp\\How_to_create_a_Datasource.tut"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            new TutorialSelector(frame, new URL("file:D:/Work/jnlp/jnlpDesigner/jnlp/Designer.jar"), null, additionalEntries);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        new TutorialSelector(frame, "D:\\Work\\jnlp\\", null, additionalEntries);
    }

    /**
     * A list model that displays tutorials
     * @author anormal
     */
    private class TutorialListModel extends DefaultListModel {

        private static final long serialVersionUID = 1L;

        private Map<String, Tutorial> data = new HashMap<String, Tutorial>();

        private List<String> titles = new ArrayList<String>();

        private Map<String, URL> urls = new HashMap<String, URL>();

        /**
         * Creates the model with tutorials from the given path
         * @param filepath the path to load the tutorials from
         */
        public TutorialListModel(String filepath) {
            File path = new File(filepath);
            if (path.isDirectory() && path.exists()) {
                File[] files = path.listFiles(new FilenameFilter() {

                    public boolean accept(File dir, String name) {
                        return name.toLowerCase().endsWith(".tut");
                    }
                });
                if (files != null) {
                    for (File f : files) {
                        try {
                            Tutorial tutorial = new Tutorial(new FileStore(f), false);
                            for (ResourceBundle extRes : externalResourceBundles) {
                                tutorial.addExternalResourceBundle(extRes);
                            }
                            data.put(tutorial.toString(), tutorial);
                            titles.add(tutorial.toString());
                            urls.put(tutorial.toString(), null);
                        } catch (FileNotFoundException e) {
                        } catch (IllegalArgumentException e) {
                        } catch (IOException e) {
                        }
                    }
                }
            }
            Collections.sort(titles);
        }

        /**
         * Creates the model with tutorials from the given jar url
         * @param jarpath the jar url to load the tutorials from
         */
        public TutorialListModel(URL jarpath) {
            JarFile jar;
            JarURLConnection jarUrlCon = null;
            try {
                if (jarpath.toString().toLowerCase().startsWith("jar:")) {
                    URLConnection uc = jarpath.openConnection();
                    jarUrlCon = ((JarURLConnection) uc);
                    jar = jarUrlCon.getJarFile();
                } else {
                    try {
                        URL jarUrl = new URL("jar", "", -1, jarpath.toString() + "!/");
                        URLConnection uc = jarUrl.openConnection();
                        jarUrlCon = ((JarURLConnection) uc);
                        jar = jarUrlCon.getJarFile();
                    } catch (IOException e1) {
                        URL jarUrl = new URL("jar", "", -1, URLDecoder.decode(jarpath.toString(), "UTF-8") + "!/");
                        URLConnection uc = jarUrl.openConnection();
                        jarUrlCon = ((JarURLConnection) uc);
                        jar = jarUrlCon.getJarFile();
                    }
                }
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry next = entries.nextElement();
                    if (next.getName().endsWith(".tut")) {
                        try {
                            URL tutJarUrl = null;
                            try {
                                tutJarUrl = new URL("jar", "", -1, jarUrlCon.getJarFileURL() + "!/" + next.getName());
                            } catch (IOException e1) {
                                tutJarUrl = new URL("jar", "", -1, URLDecoder.decode(jarUrlCon.getJarFileURL().toString(), "UTF-8") + "!/" + next.getName());
                            }
                            Tutorial tutorial = new Tutorial(new URLStore(tutJarUrl), false);
                            for (ResourceBundle extRes : externalResourceBundles) {
                                tutorial.addExternalResourceBundle(extRes);
                            }
                            data.put(tutorial.toString(), tutorial);
                            titles.add(tutorial.toString());
                            urls.put(tutorial.toString(), null);
                        } catch (FileNotFoundException e) {
                        } catch (IllegalArgumentException e) {
                        }
                    }
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            Collections.sort(titles);
        }

        /**
         * Returns the tutorial at the given index
         * @param index the index in the list
         * @return the tutorial at the index
         */
        public Object getTutorialAt(int index) {
            if (index < 0 || index > titles.size() - 1) {
                return null;
            }
            String titleKey = titles.get(index);
            if (titleKey == null) {
                return null;
            }
            return data.get(titleKey);
        }

        /**
         * Returns the URL at the given index
         * @param index the index in the list
         * @return the URL at the index
         */
        public URL getUrlAt(int index) {
            return urls.get(titles.get(index));
        }

        @Override
        public Object getElementAt(int index) {
            return titles.get(index);
        }

        @Override
        public int getSize() {
            return titles.size();
        }

        /**
         * Inserts an additional ad the end of the list
         * @param tit the title to be displayed
         * @param url the url to the tutorial
         */
        public void insertAdditionalUrl(String tit, URL url) {
            for (Tutorial tut : data.values()) {
                if (tut == null) {
                    continue;
                }
                URL val = tut.getStore().getUrl();
                String path = url.getPath();
                int lastIndexOf = path.lastIndexOf('/');
                if (lastIndexOf != -1) {
                    path = path.substring(lastIndexOf + 1);
                }
                String valPath = val.getPath();
                if (valPath.endsWith("!/")) {
                    valPath = valPath.substring(0, valPath.length() - 2);
                }
                if (valPath.endsWith(path)) {
                    return;
                }
            }
            for (URL val : urls.values()) {
                if (val == null) {
                    continue;
                }
                String path = url.getPath();
                int lastIndexOf = path.lastIndexOf('/');
                if (lastIndexOf != -1) {
                    path = path.substring(lastIndexOf + 1);
                }
                if (val.getPath().endsWith(path)) {
                    return;
                }
            }
            boolean canInstantiate = true;
            URLConnection conn = null;
            try {
                conn = url.openConnection();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            if (conn instanceof JarURLConnection) {
                JarURLConnection jarCon = (JarURLConnection) conn;
                URL jarFileURL = jarCon.getJarFileURL();
                if (jarFileURL.getProtocol().startsWith("http")) {
                    canInstantiate = false;
                }
            } else {
                if (url.getProtocol().startsWith("http")) {
                    canInstantiate = false;
                }
            }
            Tutorial tutorial = null;
            if (canInstantiate) {
                try {
                    tutorial = new Tutorial(new URLStore(url), false);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            if (tutorial != null) {
                for (ResourceBundle extRes : externalResourceBundles) {
                    tutorial.addExternalResourceBundle(extRes);
                }
                data.put(tutorial.toString(), tutorial);
                titles.add(tutorial.toString());
                urls.put(tutorial.toString(), null);
            } else {
                String tutTitle = tit;
                tutTitle = tutTitle.replace('_', ' ');
                if (tutTitle.toLowerCase().endsWith(".tut")) {
                    tutTitle = tutTitle.substring(0, tutTitle.length() - 4);
                }
                StringTokenizer tokenizer = new StringTokenizer(tutTitle, " ");
                StringBuilder b = new StringBuilder();
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken();
                    if (b.length() > 0) {
                        b.append(' ');
                    }
                    if (token.length() > 0) {
                        b.append(Character.toTitleCase(token.charAt(0)));
                    }
                    if (token.length() > 1) {
                        b.append(token.substring(1));
                    }
                }
                tutTitle = b.toString();
                titles.add(tutTitle);
                data.put(tutTitle, null);
                urls.put(tutTitle, url);
            }
            Collections.sort(titles);
        }

        /**
         * Sets the tutorial the the given index
         * @param index the index to set the tutorial at
         * @param tutorial the tutorial to be inserted
         */
        public void setTutorialAt(int index, Tutorial tutorial) {
            urls.put(titles.get(index), null);
            data.put(titles.get(index), tutorial);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            btStart.doClick();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    public void mouseExited(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    public void mousePressed(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    public void mouseReleased(MouseEvent e) {
    }
}
