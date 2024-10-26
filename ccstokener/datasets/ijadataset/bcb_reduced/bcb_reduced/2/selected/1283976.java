package gate.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import gate.*;
import gate.creole.ResourceData;
import gate.creole.ResourceInstantiationException;
import gate.event.CreoleEvent;
import gate.persist.PersistenceException;
import gate.swing.XJMenuItem;
import gate.util.*;

/**
 * The main Shell SLAC Gate GUI frame.
 */
public class ShellSlacFrame extends MainFrame {

    /** Debug flag */
    private static final boolean DEBUG = false;

    /** Shell GUI application */
    private CorpusController application = null;

    /** Shell GUI corpus */
    private Corpus corpus = null;

    private Corpus oneDocCorpus = null;

    /** Shell GUI documents DataStore */
    private DataStore dataStore = null;

    /** Keep this action for enable/disable the menu item */
    private Action saveAction = null;

    /** Keep this action for enable/disable the menu item */
    private Action runOneAction = null;

    private Action runAction = null;

    /** Default corpus resource name */
    public static final String DEFAULT_SLUG_CORPUS_NAME = "SLUG Corpus";

    public static final String ONE_DOC_SLUG_CORPUS_NAME = "SLUG One Doc Corpus";

    /** New frame */
    public ShellSlacFrame() {
        super(true, null);
        initShellSlacLocalData();
        initShellSlacGuiComponents();
    }

    protected void initShellSlacLocalData() {
        createCorpus();
        String applicationURL = System.getProperty(GateConstants.APPLICATION_JAVA_PROPERTY_NAME);
        if (applicationURL != null) {
            createDefaultApplication(applicationURL);
        } else {
            createDefaultApplication();
        }
        dataStore = null;
    }

    protected void initShellSlacGuiComponents() {
        super.setJMenuBar(createMenuBar());
    }

    /** Create the new Shell SLAC menu */
    private JMenuBar createMenuBar() {
        JMenuBar retMenuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        Action action;
        ResourceData rDataDocument = getDocumentResourceData();
        if (rDataDocument != null) {
            action = new NewResourceAction(rDataDocument);
            action.putValue(Action.NAME, "New Document");
            action.putValue(Action.SHORT_DESCRIPTION, "Create a new document");
            fileMenu.add(new XJMenuItem(action, this));
        }
        corpusFiller = new CorpusFillerComponent();
        action = new PopulateCorpusAction();
        action.putValue(Action.NAME, "New Documents...");
        action.putValue(Action.SHORT_DESCRIPTION, "Create multiple documents");
        fileMenu.add(new XJMenuItem(action, this));
        fileMenu.add(new XJMenuItem(new CloseSelectedDocumentAction(), this));
        fileMenu.add(new XJMenuItem(new CloseAllDocumentAction(), this));
        fileMenu.addSeparator();
        action = new ImportDocumentAction();
        fileMenu.add(new XJMenuItem(action, this));
        JMenu exportMenu = new JMenu("Export");
        action = new ExportDocumentAction();
        exportMenu.add(new XJMenuItem(action, this));
        action = new ExportDocumentInlineAction();
        exportMenu.add(new XJMenuItem(action, this));
        fileMenu.add(exportMenu);
        JMenu exportAllMenu = new JMenu("Export All");
        action = new ExportAllDocumentAction();
        exportAllMenu.add(new XJMenuItem(action, this));
        action = new ExportAllDocumentInlineAction();
        exportAllMenu.add(new XJMenuItem(action, this));
        fileMenu.add(exportAllMenu);
        fileMenu.addSeparator();
        action = new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
                System.exit(0);
            }
        };
        action.putValue(Action.NAME, "Exit");
        fileMenu.add(new XJMenuItem(action, this));
        retMenuBar.add(fileMenu);
        JMenu analyseMenu = new JMenu("Analyse");
        action = new RunApplicationOneDocumentAction();
        if (application == null) {
            action.setEnabled(false);
        }
        runOneAction = action;
        analyseMenu.add(new XJMenuItem(action, this));
        retMenuBar.add(analyseMenu);
        action = new RunApplicationAction();
        if (application == null) {
            action.setEnabled(false);
        }
        runAction = action;
        analyseMenu.add(new XJMenuItem(action, this));
        retMenuBar.add(analyseMenu);
        JMenu toolsMenu = new JMenu("Tools");
        createToolsMenuItems(toolsMenu);
        retMenuBar.add(toolsMenu);
        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(new HelpAboutSlugAction());
        retMenuBar.add(helpMenu);
        return retMenuBar;
    }

    /** Should check for registered Creole components and populate menu.
   *  <BR> In first version is hardcoded. */
    private void createToolsMenuItems(JMenu toolsMenu) {
        toolsMenu.add(new NewAnnotDiffAction());
        toolsMenu.add(new AbstractAction("Unicode editor", getIcon("unicode")) {

            public void actionPerformed(ActionEvent evt) {
                new guk.Editor();
            }
        });
        if (System.getProperty("gate.slug.gazetteer") != null) toolsMenu.add(new NewGazetteerEditorAction());
    }

    /** Find ResourceData for "Create Document" menu item. */
    private ResourceData getDocumentResourceData() {
        ResourceData result = null;
        CreoleRegister reg = Gate.getCreoleRegister();
        List lrTypes = reg.getPublicLrTypes();
        if (lrTypes != null && !lrTypes.isEmpty()) {
            Iterator lrIter = lrTypes.iterator();
            while (lrIter.hasNext()) {
                ResourceData rData = (ResourceData) reg.get(lrIter.next());
                if ("gate.corpora.DocumentImpl".equalsIgnoreCase(rData.getClassName())) {
                    result = rData;
                    break;
                }
            }
        }
        return result;
    }

    /** Here default ANNIE is created. Could be changed. */
    private void createDefaultApplication() {
        Runnable loadAction = new ANNIERunnable(ShellSlacFrame.this);
        Thread thread = new Thread(loadAction, "");
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    /** Load serialized application from file. */
    private void createDefaultApplication(String url) {
        ApplicationLoadRun run = new ApplicationLoadRun(url, this);
        Thread thread = new Thread(run, "");
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    /** Create corpus for application */
    private void createCorpus() {
        try {
            Factory.newCorpus(DEFAULT_SLUG_CORPUS_NAME);
            Factory.newCorpus(ONE_DOC_SLUG_CORPUS_NAME);
        } catch (ResourceInstantiationException ex) {
            ex.printStackTrace();
            throw new GateRuntimeException("Error in creating build in corpus.");
        }
    }

    /** Override base class method */
    public void resourceLoaded(CreoleEvent e) {
        super.resourceLoaded(e);
        Resource res = e.getResource();
        if (res instanceof CorpusController) {
            if (application != null) {
                Factory.deleteResource(application);
            }
            application = (CorpusController) res;
            runOneAction.setEnabled(true);
            runAction.setEnabled(true);
            if (corpus != null) application.setCorpus(corpus);
        }
        if (res instanceof Corpus) {
            Corpus resCorpus = (Corpus) res;
            if (DEFAULT_SLUG_CORPUS_NAME.equals(resCorpus.getName())) {
                corpus = resCorpus;
                if (application != null) application.setCorpus(corpus);
            }
            if (ONE_DOC_SLUG_CORPUS_NAME.equals(resCorpus.getName())) {
                oneDocCorpus = resCorpus;
            }
        }
        if (res instanceof Document) {
            final Document doc = (Document) res;
            corpus.add(doc);
            if (DEBUG) Out.println("Document loaded, showing...");
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    showDocument(doc);
                }
            });
        }
    }

    /** Find in resource tree and show the document */
    protected void showDocument(Document doc) {
        Handle handle = null;
        Enumeration nodesEnum = resourcesTreeRoot.preorderEnumeration();
        boolean done = false;
        DefaultMutableTreeNode node = resourcesTreeRoot;
        Object obj;
        while (!done && nodesEnum.hasMoreElements()) {
            node = (DefaultMutableTreeNode) nodesEnum.nextElement();
            obj = node.getUserObject();
            if (obj instanceof Handle) {
                handle = (Handle) obj;
                obj = handle.getTarget();
                done = obj instanceof Document && doc == (Document) obj;
            }
        }
        if (done) {
            select(handle);
        } else {
            if (DEBUG) Out.println("Failed to find handle for document");
        }
    }

    /** Called when a {@link gate.DataStore} has been opened.
   *  Save corpus on datastore open. */
    public void datastoreOpened(CreoleEvent e) {
        super.datastoreOpened(e);
        if (corpus == null) return;
        DataStore ds = e.getDatastore();
        try {
            if (dataStore != null) {
                dataStore.close();
            }
            saveAction.setEnabled(false);
            LanguageResource persCorpus = ds.adopt(corpus, null);
            ds.sync(persCorpus);
            Factory.deleteResource((LanguageResource) corpus);
            corpus = (Corpus) persCorpus;
            if (application != null) application.setCorpus(corpus);
            dataStore = ds;
            saveAction.setEnabled(true);
        } catch (PersistenceException pex) {
            pex.printStackTrace();
        } catch (gate.security.SecurityException sex) {
            sex.printStackTrace();
        }
    }

    /** Return handle to selected tab resource */
    private Handle getSelectedResource() {
        JComponent largeView = (JComponent) mainTabbedPane.getSelectedComponent();
        Handle result = null;
        Enumeration nodesEnum = resourcesTreeRoot.preorderEnumeration();
        boolean done = false;
        DefaultMutableTreeNode node = resourcesTreeRoot;
        while (!done && nodesEnum.hasMoreElements()) {
            node = (DefaultMutableTreeNode) nodesEnum.nextElement();
            done = node.getUserObject() instanceof Handle && ((Handle) node.getUserObject()).getLargeView() == largeView;
        }
        if (done) result = (Handle) node.getUserObject();
        return result;
    }

    /** Export All store of documents from SLUG corpus */
    private void saveDocuments(File targetDir) {
        if (corpus == null || corpus.size() == 0) return;
        Document doc;
        String target = targetDir.getPath();
        URL fileURL;
        String fileName = null;
        int index;
        MainFrame.lockGUI("Export all documents...");
        target = target + File.separatorChar;
        for (int i = 0; i < corpus.size(); ++i) {
            doc = (Document) corpus.get(i);
            fileURL = doc.getSourceUrl();
            if (fileURL != null) fileName = fileURL.toString();
            index = fileName.lastIndexOf('/');
            if (index != -1) {
                fileName = fileName.substring(index + 1, fileName.length());
            } else fileName = "content_txt";
            fileName = target + fileName + ".xml";
            try {
                OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(new File(fileName)), "UTF-8");
                writer.write(doc.toXml());
                writer.flush();
                writer.close();
            } catch (Exception ex) {
                ex.printStackTrace(Out.getPrintWriter());
            } finally {
                MainFrame.unlockGUI();
            }
        }
        MainFrame.unlockGUI();
    }

    /** Run the current application SLAC */
    class RunApplicationAction extends AbstractAction {

        public RunApplicationAction() {
            super("Analyse All", getIcon("menu_controller"));
            putValue(SHORT_DESCRIPTION, "Run the application to process documents");
        }

        public void actionPerformed(ActionEvent e) {
            if (application != null && corpus != null && corpus.size() > 0) {
                application.setCorpus(corpus);
                SerialControllerEditor editor = new SerialControllerEditor();
                editor.setTarget(application);
                editor.runAction.actionPerformed(null);
            }
        }
    }

    /** Run the current application SLAC on current document */
    class RunApplicationOneDocumentAction extends AbstractAction {

        public RunApplicationOneDocumentAction() {
            super("Analyse", getIcon("menu_controller"));
            putValue(SHORT_DESCRIPTION, "Run the application to process current document");
        }

        public void actionPerformed(ActionEvent e) {
            if (application != null) {
                Handle handle = getSelectedResource();
                if (handle == null) return;
                Object target = handle.getTarget();
                if (target == null) return;
                if (target instanceof Document) {
                    Document doc = (Document) target;
                    oneDocCorpus.clear();
                    oneDocCorpus.add(doc);
                    application.setCorpus(oneDocCorpus);
                    SerialControllerEditor editor = new SerialControllerEditor();
                    editor.setTarget(application);
                    editor.runAction.actionPerformed(null);
                }
            }
        }
    }

    class RestoreDefaultApplicationAction extends AbstractAction {

        public RestoreDefaultApplicationAction() {
            super("Create ANNIE application");
            putValue(SHORT_DESCRIPTION, "Create default ANNIE application");
        }

        public void actionPerformed(ActionEvent e) {
            createDefaultApplication();
        }
    }

    class CloseSelectedDocumentAction extends AbstractAction {

        public CloseSelectedDocumentAction() {
            super("Close Document");
            putValue(SHORT_DESCRIPTION, "Closes the selected document");
        }

        public void actionPerformed(ActionEvent e) {
            JComponent resource = (JComponent) mainTabbedPane.getSelectedComponent();
            if (resource != null) {
                Action act = resource.getActionMap().get("Close resource");
                if (act != null) act.actionPerformed(null);
            }
        }
    }

    class CloseAllDocumentAction extends AbstractAction {

        public CloseAllDocumentAction() {
            super("Close All");
            putValue(SHORT_DESCRIPTION, "Closes all documents");
        }

        public void actionPerformed(ActionEvent e) {
            JComponent resource;
            for (int i = mainTabbedPane.getTabCount() - 1; i > 0; --i) {
                resource = (JComponent) mainTabbedPane.getComponentAt(i);
                if (resource != null) {
                    Action act = resource.getActionMap().get("Close resource");
                    if (act != null) act.actionPerformed(null);
                }
            }
        }
    }

    class StoreAllDocumentAsAction extends AbstractAction {

        public StoreAllDocumentAsAction() {
            super("Store all Documents As...");
            putValue(SHORT_DESCRIPTION, "Store all opened in the application documents in new directory");
        }

        public void actionPerformed(ActionEvent e) {
            createSerialDataStore();
        }
    }

    class StoreAllDocumentAction extends AbstractAction {

        public StoreAllDocumentAction() {
            super("Store all Documents");
            putValue(SHORT_DESCRIPTION, "Store all opened in the application documents");
        }

        public void actionPerformed(ActionEvent e) {
            if (dataStore != null) {
                try {
                    dataStore.sync(corpus);
                } catch (PersistenceException pex) {
                    pex.printStackTrace();
                } catch (gate.security.SecurityException sex) {
                    sex.printStackTrace();
                }
            }
        }
    }

    class LoadAllDocumentAction extends AbstractAction {

        public LoadAllDocumentAction() {
            super("Load all Documents");
            putValue(SHORT_DESCRIPTION, "Load documents from storage");
        }

        public void actionPerformed(ActionEvent e) {
            if (dataStore != null) {
                try {
                    dataStore.close();
                } catch (PersistenceException pex) {
                    pex.printStackTrace();
                }
                dataStore = null;
            }
            dataStore = openSerialDataStore();
            if (dataStore != null) {
                List corporaIDList = null;
                List docIDList = null;
                String docID = "";
                FeatureMap features;
                Document doc;
                try {
                    corporaIDList = dataStore.getLrIds("gate.corpora.CorpusImpl");
                    docIDList = dataStore.getLrIds("gate.corpora.DocumentImpl");
                } catch (PersistenceException pex) {
                    pex.printStackTrace();
                }
                features = Factory.newFeatureMap();
                features.put(DataStore.LR_ID_FEATURE_NAME, docID);
                features.put(DataStore.DATASTORE_FEATURE_NAME, dataStore);
                for (int i = 0; i < docIDList.size(); ++i) {
                    docID = (String) docIDList.get(i);
                    features.put(DataStore.LR_ID_FEATURE_NAME, docID);
                    doc = null;
                    try {
                        doc = (Document) Factory.createResource("gate.corpora.DocumentImpl", features);
                    } catch (gate.creole.ResourceInstantiationException rex) {
                        rex.printStackTrace();
                    }
                    if (doc != null) corpus.add(doc);
                }
            }
        }
    }

    class TestStoreAction extends AbstractAction {

        public TestStoreAction() {
            super("Test Store application");
            putValue(SHORT_DESCRIPTION, "Store the application");
        }

        public void actionPerformed(ActionEvent e) {
            if (application != null) {
                try {
                    File file = new File("D:/temp/tempapplication.tmp");
                    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
                    long startTime = System.currentTimeMillis();
                    oos.writeObject(application);
                    long endTime = System.currentTimeMillis();
                    System.out.println("Storing completed in " + NumberFormat.getInstance().format((double) (endTime - startTime) / 1000) + " seconds");
                    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                    Object object;
                    startTime = System.currentTimeMillis();
                    object = ois.readObject();
                    endTime = System.currentTimeMillis();
                    application = (CorpusController) object;
                    System.out.println("Loading completed in " + NumberFormat.getInstance().format((double) (endTime - startTime) / 1000) + " seconds");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /** Import document action */
    class ImportDocumentAction extends AbstractAction {

        public ImportDocumentAction() {
            super("Import");
            putValue(SHORT_DESCRIPTION, "Open a document in XML format");
        }

        public void actionPerformed(ActionEvent e) {
            fileChooser.setDialogTitle("Select file to Import from");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int res = fileChooser.showOpenDialog(ShellSlacFrame.this);
            if (res == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                String str = "";
                char chArr[] = new char[1024];
                try {
                    FileReader reader = new FileReader(file);
                    int readedChars = reader.read(chArr);
                    reader.close();
                    str = new String(chArr, 0, readedChars);
                } catch (Exception ex) {
                }
                boolean isGateXmlDocument = false;
                if (str.indexOf("<GateDocument") != -1 || str.indexOf(" GateDocument") != -1) isGateXmlDocument = true;
                if (isGateXmlDocument) {
                    Runnable run = new ImportRunnable(file);
                    Thread thread = new Thread(run, "");
                    thread.setPriority(Thread.MIN_PRIORITY);
                    thread.start();
                } else {
                    JOptionPane.showMessageDialog(ShellSlacFrame.this, "The import file '" + file.getAbsolutePath() + "'\n" + "is not a SLUG document.", "Import error", JOptionPane.WARNING_MESSAGE);
                }
            }
        }
    }

    /** Object to run ExportAll in a new Thread */
    private class ImportRunnable implements Runnable {

        File file;

        ImportRunnable(File targetFile) {
            file = targetFile;
        }

        public void run() {
            if (file != null) {
                MainFrame.lockGUI("Import file...");
                try {
                    Factory.newDocument(file.toURI().toURL());
                } catch (MalformedURLException mex) {
                    mex.printStackTrace();
                } catch (ResourceInstantiationException rex) {
                    rex.printStackTrace();
                } finally {
                    MainFrame.unlockGUI();
                }
            }
        }
    }

    /** Export current document action */
    class ExportDocumentInlineAction extends AbstractAction {

        public ExportDocumentInlineAction() {
            super("with inline markup");
            putValue(SHORT_DESCRIPTION, "Save the selected document in XML format" + " with inline markup");
        }

        public void actionPerformed(ActionEvent e) {
            JComponent resource = (JComponent) mainTabbedPane.getSelectedComponent();
            if (resource == null) return;
            Component c;
            Document doc = null;
            for (int i = 0; i < resource.getComponentCount(); ++i) {
                c = resource.getComponent(i);
                if (c instanceof DocumentEditor) {
                    doc = ((DocumentEditor) c).getDocument();
                }
            }
            if (doc != null) {
                JFileChooser fileChooser = MainFrame.getFileChooser();
                File selectedFile = null;
                fileChooser.setMultiSelectionEnabled(false);
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fileChooser.setDialogTitle("Select document to save ...");
                fileChooser.setSelectedFiles(null);
                int res = fileChooser.showDialog(ShellSlacFrame.this, "Save");
                if (res == JFileChooser.APPROVE_OPTION) {
                    selectedFile = fileChooser.getSelectedFile();
                    fileChooser.setCurrentDirectory(fileChooser.getCurrentDirectory());
                    Runnable run = new ExportInline(doc, selectedFile);
                    Thread thread = new Thread(run, "");
                    thread.setPriority(Thread.MIN_PRIORITY);
                    thread.start();
                }
            }
        }
    }

    /** New thread object for export inline */
    private class ExportInline implements Runnable {

        File targetFile;

        Document document;

        ExportInline(Document doc, File target) {
            targetFile = target;
            document = doc;
        }

        protected Set getTypes(String types) {
            Set set = new HashSet();
            StringTokenizer tokenizer = new StringTokenizer(types, ";");
            while (tokenizer.hasMoreTokens()) {
                set.add(tokenizer.nextToken());
            }
            return set;
        }

        public void run() {
            if (document == null || targetFile == null) return;
            MainFrame.lockGUI("Store document with inline markup...");
            try {
                AnnotationSet annotationsToDump = null;
                annotationsToDump = document.getAnnotations();
                String enumaratedAnnTypes = System.getProperty(GateConstants.ANNOT_TYPE_TO_EXPORT);
                if (enumaratedAnnTypes != null) {
                    Set typesSet = getTypes(enumaratedAnnTypes);
                    annotationsToDump = annotationsToDump.get(typesSet);
                }
                String encoding = ((gate.TextualDocument) document).getEncoding();
                if (encoding == null || encoding.length() == 0) encoding = System.getProperty("file.encoding");
                if (encoding == null || encoding.length() == 0) encoding = "UTF-8";
                OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(targetFile), encoding);
                Boolean featuresSaved = Gate.getUserConfig().getBoolean(GateConstants.SAVE_FEATURES_WHEN_PRESERVING_FORMAT);
                boolean saveFeatures = true;
                if (featuresSaved != null) saveFeatures = featuresSaved.booleanValue();
                String toXml = document.toXml(annotationsToDump, saveFeatures);
                String mimeType = (String) document.getFeatures().get("MimeType");
                if ("text/plain".equalsIgnoreCase(mimeType)) {
                    toXml = "<GATE>\n" + toXml + "\n</GATE>";
                }
                writer.write(toXml);
                writer.flush();
                writer.close();
            } catch (Exception ex) {
                ex.printStackTrace(Out.getPrintWriter());
            }
            MainFrame.unlockGUI();
        }
    }

    /** Export current document action */
    class ExportDocumentAction extends AbstractAction {

        public ExportDocumentAction() {
            super("in GATE format");
            putValue(SHORT_DESCRIPTION, "Save the selected document in XML format");
        }

        public void actionPerformed(ActionEvent e) {
            JComponent resource = (JComponent) mainTabbedPane.getSelectedComponent();
            if (resource != null) {
                Action act = resource.getActionMap().get("Save As XML");
                if (act != null) act.actionPerformed(null);
            }
        }
    }

    /** Export All menu action */
    class ExportAllDocumentAction extends AbstractAction {

        public ExportAllDocumentAction() {
            super("in GATE format");
            putValue(SHORT_DESCRIPTION, "Save all documents in XML format");
        }

        public void actionPerformed(ActionEvent e) {
            fileChooser.setDialogTitle("Select Export directory");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int res = fileChooser.showOpenDialog(ShellSlacFrame.this);
            if (res == JFileChooser.APPROVE_OPTION) {
                File directory = fileChooser.getSelectedFile();
                if (directory != null && directory.isDirectory()) {
                    Runnable run = new ExportAllRunnable(directory);
                    Thread thread = new Thread(run, "");
                    thread.setPriority(Thread.MIN_PRIORITY);
                    thread.start();
                }
            }
        }
    }

    /** Export All Inline menu action */
    class ExportAllDocumentInlineAction extends AbstractAction {

        public ExportAllDocumentInlineAction() {
            super("with inline markup");
            putValue(SHORT_DESCRIPTION, "Save all documents in XML format" + " with inline markup");
        }

        public void actionPerformed(ActionEvent e) {
            fileChooser.setDialogTitle("Select Export directory");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int res = fileChooser.showOpenDialog(ShellSlacFrame.this);
            if (res == JFileChooser.APPROVE_OPTION) {
                File directory = fileChooser.getSelectedFile();
                if (directory != null && directory.isDirectory()) {
                    Document currentDoc;
                    String fileName;
                    URL fileURL;
                    Runnable run;
                    Thread thread;
                    for (int i = 0; i < corpus.size(); ++i) {
                        currentDoc = (Document) corpus.get(i);
                        fileURL = currentDoc.getSourceUrl();
                        fileName = null;
                        if (fileURL != null) {
                            fileName = fileURL.getFile();
                            fileName = Files.getLastPathComponent(fileName);
                        }
                        if (fileName == null || fileName.length() == 0) {
                            fileName = currentDoc.getName();
                        }
                        if (fileName.length() == 0) {
                            fileName = "gate_result" + i;
                        }
                        fileName = fileName + ".gate";
                        run = new ExportInline(currentDoc, new File(directory, fileName));
                        thread = new Thread(run, "");
                        thread.setPriority(Thread.MIN_PRIORITY);
                        thread.start();
                    }
                }
            }
        }
    }

    /** Object to run ExportAll in a new Thread */
    private class ExportAllRunnable implements Runnable {

        File directory;

        ExportAllRunnable(File targetDirectory) {
            directory = targetDirectory;
        }

        public void run() {
            saveDocuments(directory);
        }
    }

    /** Load application from file */
    private class ApplicationLoadRun implements Runnable {

        private String appURL;

        private MainFrame appFrame;

        public ApplicationLoadRun(String url, MainFrame frame) {
            appURL = url;
            appFrame = frame;
        }

        public void run() {
            File file = new File(appURL);
            boolean appLoaded = false;
            MainFrame.lockGUI("Application from '" + appURL + "' is being loaded...");
            if (file.exists()) {
                try {
                    gate.util.persistence.PersistenceManager.loadObjectFromFile(file);
                    appLoaded = true;
                } catch (PersistenceException pex) {
                    pex.printStackTrace();
                } catch (ResourceInstantiationException riex) {
                    riex.printStackTrace();
                } catch (IOException ioex) {
                    ioex.printStackTrace();
                }
            }
            MainFrame.unlockGUI();
            if (!appLoaded) {
                JOptionPane.showMessageDialog(ShellSlacFrame.this, "The application file '" + appURL + "'\n" + "from parameter -Dgate.slug.app\n" + "is missing or corrupted." + "Create default application.", "Load application error", JOptionPane.WARNING_MESSAGE);
                createDefaultApplication();
            }
        }
    }

    /** Create default ANNIE */
    public class ANNIERunnable implements Runnable {

        MainFrame parentFrame;

        ANNIERunnable(MainFrame parent) {
            parentFrame = parent;
        }

        public void run() {
            AbstractAction action = new LoadANNIEWithDefaultsAction();
            action.actionPerformed(new ActionEvent(parentFrame, 1, "Load ANNIE"));
        }
    }

    class AboutPaneDialog extends JDialog {

        public AboutPaneDialog(Frame frame, String title, boolean modal) {
            super(frame, title, modal);
        }

        public boolean setURL(URL url) {
            boolean success = false;
            try {
                Container pane = getContentPane();
                JScrollPane scroll = new JScrollPane();
                JEditorPane editor = new JEditorPane(url);
                editor.setEditable(false);
                scroll.getViewport().add(editor);
                pane.add(scroll, BorderLayout.CENTER);
                JButton ok = new JButton("Close");
                ok.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        AboutPaneDialog.this.setVisible(false);
                    }
                });
                pane.add(ok, BorderLayout.SOUTH);
                success = true;
            } catch (Exception ex) {
                if (DEBUG) {
                    ex.printStackTrace();
                }
            }
            return success;
        }
    }

    /** Dummy Help About dialog */
    class HelpAboutSlugAction extends AbstractAction {

        public HelpAboutSlugAction() {
            super("About");
        }

        public void actionPerformed(ActionEvent e) {
            String aboutText = "Slug application.";
            String aboutURL = System.getProperty(GateConstants.ABOUT_URL_JAVA_PROPERTY_NAME);
            boolean canShowInPane = false;
            if (aboutURL != null) {
                try {
                    URL url = new URL(aboutURL);
                    AboutPaneDialog dlg = new AboutPaneDialog(ShellSlacFrame.this, "Slug application about", true);
                    canShowInPane = dlg.setURL(url);
                    if (canShowInPane) {
                        dlg.setSize(300, 200);
                        dlg.setLocationRelativeTo(ShellSlacFrame.this);
                        dlg.setVisible(true);
                    } else {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                        String line = "";
                        StringBuffer content = new StringBuffer();
                        do {
                            content.append(line);
                            line = reader.readLine();
                        } while (line != null);
                        if (content.length() != 0) {
                            aboutText = content.toString();
                        }
                    }
                } catch (Exception ex) {
                    if (DEBUG) {
                        ex.printStackTrace();
                    }
                }
            }
            if (!canShowInPane) JOptionPane.showMessageDialog(ShellSlacFrame.this, aboutText, "Slug application about", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
   * Component used to select the options for corpus populating
   */
    CorpusFillerComponent corpusFiller;

    class PopulateCorpusAction extends AbstractAction {

        PopulateCorpusAction() {
            super("New Documents...");
        }

        public void actionPerformed(ActionEvent e) {
            Runnable runnable = new Runnable() {

                public void run() {
                    if (corpus == null || corpusFiller == null) return;
                    corpusFiller.setExtensions(new ArrayList());
                    corpusFiller.setEncoding("");
                    boolean answer = OkCancelDialog.showDialog(ShellSlacFrame.this, corpusFiller, "Select a directory and allowed extensions");
                    if (answer) {
                        URL url = null;
                        try {
                            url = new URL(corpusFiller.getUrlString());
                            java.util.List extensions = corpusFiller.getExtensions();
                            ExtensionFileFilter filter = null;
                            if (extensions == null || extensions.isEmpty()) filter = null; else {
                                filter = new ExtensionFileFilter();
                                Iterator extIter = corpusFiller.getExtensions().iterator();
                                while (extIter.hasNext()) {
                                    filter.addExtension((String) extIter.next());
                                }
                            }
                            corpus.populate(url, filter, corpusFiller.getEncoding(), corpusFiller.isRecurseDirectories());
                        } catch (MalformedURLException mue) {
                            JOptionPane.showMessageDialog(ShellSlacFrame.this, "Invalid URL!\n " + "See \"Messages\" tab for details!", "GATE", JOptionPane.ERROR_MESSAGE);
                            mue.printStackTrace(Err.getPrintWriter());
                        } catch (IOException ioe) {
                            JOptionPane.showMessageDialog(ShellSlacFrame.this, "I/O error!\n " + "See \"Messages\" tab for details!", "GATE", JOptionPane.ERROR_MESSAGE);
                            ioe.printStackTrace(Err.getPrintWriter());
                        } catch (ResourceInstantiationException rie) {
                            JOptionPane.showMessageDialog(ShellSlacFrame.this, "Could not create document!\n " + "See \"Messages\" tab for details!", "GATE", JOptionPane.ERROR_MESSAGE);
                            rie.printStackTrace(Err.getPrintWriter());
                        }
                    }
                }
            };
            Thread thread = new Thread(Thread.currentThread().getThreadGroup(), runnable);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }
    }
}
