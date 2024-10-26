package com.jvito.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import javax.swing.DefaultDesktopManager;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.xml.parsers.DocumentBuilderFactory;
import org.freehep.util.export.ExportDialog;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import com.jvito.JViTo;
import com.jvito.OperatorsReader;
import com.jvito.Project;
import com.jvito.compile.Compileable;
import com.jvito.compile.JViToCompiler;
import com.jvito.data.FileSource;
import com.jvito.data.GivenExampleSet;
import com.jvito.data.GivenJMySVMModel;
import com.jvito.data.Source;
import com.jvito.exception.KeyException;
import com.jvito.exception.XMLException;
import com.jvito.gui.attributeeditor.AttributeEditorWindow;
import com.jvito.plot.Plot;
import com.jvito.rapidminer.operator.visualization.JViToPlotable;
import com.jvito.util.Logger;
import com.jvito.util.Preferences;
import com.jvito.util.Resources;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.learner.functions.kernel.JMySVMModel;

/**
 * Main-Frame for JViTo.
 * 
 * @author Daniel Hakenjos
 * @version $Id: Application.java,v 1.5 2008/04/16 19:54:23 djhacker Exp $
 */
public class Application extends JFrame {

    private static final long serialVersionUID = 8019636583851422852L;

    private StartWindow startwindow;

    public JViToMenu menu;

    private JViToToolBar toolbar;

    public Navigator navigator;

    public Inspector inspector;

    public LogViewer logviewer;

    public DescriptionPanel desc_panel;

    private Preferences pref;

    private JDesktopPane desktop;

    private NavigatorWindow navi_window;

    private InspectorWindow insp_window;

    private JViToCompiler compiler;

    private File projectfile;

    private boolean opened = false;

    private boolean open = false;

    private boolean dirty = false;

    private LinkedList<Object> input;

    private int jpeg_quality = 75;

    /**
	 * When exiting the application <code>System.exit(0);</code> is called.
	 * 
	 * @see #promptExit()
	 */
    public static final int SYSTEM_EXIT_ON_EXIT = 0;

    /**
	 * When exiting the application <code>JFrame.dispose()</code> is called.
	 * 
	 * @see #promptExit()
	 */
    public static final int DISPOSE_ON_EXIT = 1;

    private int exit_type = 0;

    /**
	 * Instantiate this <code>Application</code>.
	 * 
	 */
    public Application() {
        super(Resources.getString("APPLICATION_TITLE"));
        input = new LinkedList<Object>();
        startwindow = new StartWindow();
        startwindow.setVisible(true);
        open = true;
    }

    /**
	 * Sets the type how to exit the application.
	 * 
	 * @param exit_type
	 *            one of the types: <code>SYSTEM_EXIT_ON_EXIT</code> or <code>DISPOSE_ON_EXIT</code>.
	 * @see #SYSTEM_EXIT_ON_EXIT
	 * @see #DISPOSE_ON_EXIT
	 */
    public void setTypeHowToExit(int exit_type) {
        this.exit_type = exit_type;
    }

    /**
	 * Gets the type how to exit the application
	 * 
	 * @see #SYSTEM_EXIT_ON_EXIT
	 * @see #DISPOSE_ON_EXIT
	 */
    public int getTypeHowToExit() {
        return exit_type;
    }

    /**
	 * Inits this <code>Application</code>.
	 * 
	 */
    public void init() {
        loadOperatorsXML();
        initPreferences();
        initComponents();
        projectfile = null;
        testJava3D();
    }

    private void loadOperatorsXML() {
        startwindow.setMessage("Loading Operators...");
        try {
            URL url = Application.class.getClassLoader().getResource(Resources.getString("OPERATORS_XML"));
            InputStream input = url.openStream();
            OperatorsReader.registerOperators(Resources.getString("OPERATORS_XML"), input);
        } catch (FileNotFoundException e) {
            Logger.logException("File '" + Resources.getString("OPERATORS_XML") + "' not found.", e);
        } catch (IOException error) {
            Logger.logException(error.getMessage(), error);
        }
    }

    private void initPreferences() {
        startwindow.setMessage("Loading Preferences...");
        String preffile = System.getProperty("JVITO_PREFERENCES");
        if (preffile != null) {
            File file = new File(preffile);
            if ((file.exists()) && (!file.isDirectory())) {
                pref = new Preferences(preffile);
                try {
                    pref.load();
                } catch (XMLException error) {
                    Logger.logException("Can't load the preferences-file!", error);
                }
                try {
                    jpeg_quality = pref.getInt("jpeg_quality");
                } catch (KeyException error) {
                    jpeg_quality = 75;
                    pref.setInt("jpeg_quality", jpeg_quality);
                }
                Logger.logMessage("Preferences loaded!", Logger.TASK);
            } else if ((!file.exists()) && (!file.isDirectory())) {
                pref = new Preferences(preffile);
                Logger.logMessage("Preferences-file is not existing!", Logger.WARNING);
                try {
                    pref.save();
                } catch (IOException error) {
                    Logger.logException("Can't save the preferences-file.", error);
                } finally {
                    Logger.logMessage("Preferences-file was created.!", Logger.TASK);
                }
            } else {
                pref = new Preferences(System.getProperty("user.home") + "/jvito_preferences.xml");
            }
        } else {
            pref = new Preferences(System.getProperty("user.home") + "/jvito_preferences.xml");
            System.out.println(pref.getFileName());
        }
        Logger.init(pref);
        String wdir = System.getProperty("user.home");
        try {
            wdir = pref.getString("workingdir");
        } catch (KeyException error) {
            wdir = System.getProperty("user.home");
        }
        File file = new File(wdir);
        if ((file.isDirectory()) && (file.exists())) {
            JViTo.setWorkingDir(wdir);
        } else {
            Logger.logMessage("No working directory: " + wdir.toString(), Logger.ERROR);
        }
    }

    private void initComponents() {
        startwindow.setMessage(Resources.getString("STARTWINDOW_MAINFRAME"));
        setWindowTitle();
        this.setIconImage(Resources.getImage("JVITO_EGGS_SMALL"));
        menu = new JViToMenu(this);
        toolbar = new JViToToolBar(this);
        this.setJMenuBar(menu);
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        desktop = new JDesktopPane();
        desktop.setDesktopManager(new DefaultDesktopManager());
        JPanel toolpanel = new JPanel();
        toolpanel.setLayout(new BorderLayout());
        toolpanel.add(toolbar, BorderLayout.WEST);
        JPanel toppanel = new JPanel();
        toppanel.setLayout(new BorderLayout());
        toppanel.add(desktop, BorderLayout.CENTER);
        toppanel.add(toolpanel, BorderLayout.NORTH);
        getContentPane().add(toppanel);
        this.addWindowListener(new WindowClosingListener());
        this.logviewer = new LogViewer(this);
        Logger.initGUI();
        try {
            logviewer.setSize(pref.getInt("logviewer_width"), pref.getInt("logviewer_height"));
            logviewer.setLocation(pref.getInt("logviewer_x"), pref.getInt("logviewer_y"));
        } catch (KeyException error) {
            logviewer.setSize(900, 400);
            logviewer.setLocation(0, 0);
            pref.setInt("logviewer_x", logviewer.getLocation().x);
            pref.setInt("logviewer_y", logviewer.getLocation().y);
            pref.setInt("logviewer_width", logviewer.getSize().width);
            pref.setInt("logviewer_height", logviewer.getSize().height);
        }
        logviewer.setClosable(false);
        logviewer.setResizable(true);
        logviewer.setVisible(true);
        desktop.add(logviewer);
        this.menu.updateWindowMenu();
        pack();
        try {
            setSize(pref.getInt("jvito_window_width"), pref.getInt("jvito_window_height"));
            setLocation(pref.getInt("jvito_window_x"), pref.getInt("jvito_window_y"));
        } catch (KeyException error) {
            if (getToolkit().isFrameStateSupported(Frame.MAXIMIZED_BOTH)) this.setExtendedState(Frame.MAXIMIZED_BOTH); else setSize(500, 500);
        }
        startwindow.dispose();
        setVisible(true);
    }

    private void initComponentsDesktop(Project project) {
        navigator = new Navigator(new ProjectTreeNode(project));
        inspector = new Inspector(this, ((Project) ((ProjectTreeNode) navigator.getModel().getRoot()).getUserObject()));
        desc_panel = new DescriptionPanel(((Project) ((ProjectTreeNode) navigator.getModel().getRoot()).getUserObject()));
        navigator.setInspector(inspector);
        navigator.setDescriptionPanel(desc_panel);
        navi_window = new NavigatorWindow(navigator);
        insp_window = new InspectorWindow(inspector, desc_panel);
        try {
            navi_window.setLocation(pref.getInt("navi_window_x"), pref.getInt("navi_window_y"));
            navi_window.setSize(pref.getInt("navi_window_width"), pref.getInt("navi_window_height"));
        } catch (KeyException error) {
            navi_window.setLocation(0, 0);
            navi_window.setSize(300, 450);
        }
        navi_window.setClosable(false);
        navi_window.setResizable(true);
        navi_window.setVisible(true);
        try {
            insp_window.setLocation(pref.getInt("insp_window_x"), pref.getInt("insp_window_y"));
            insp_window.setSize(pref.getInt("insp_window_width"), pref.getInt("insp_window_height"));
        } catch (KeyException error) {
            insp_window.setLocation(0, 450);
            insp_window.setSize(300, desktop.getSize().height - 550);
        }
        insp_window.setClosable(false);
        insp_window.setResizable(true);
        insp_window.setVisible(true);
    }

    /**
	 * Check to see if the version of Java 3D being used is compatible with the desired specification version 1.2.
	 * 
	 * @return true if the Java 3D version being used is greater than or equal to the desired version number
	 */
    public static boolean testJava3D() {
        Class testClass = testClass("javax.vecmath.Point3d");
        boolean b = (testClass != null) ? (testClass("javax.media.j3d.SceneGraphObject") != null) : false;
        if (b) {
            Package p = testClass.getPackage();
            if (p != null) {
                try {
                    b = p.isCompatibleWith("1.2");
                } catch (NumberFormatException nfe) {
                    b = false;
                }
            }
        }
        if (!b) {
            int confirm = JOptionPane.showConfirmDialog(JViTo.getApplication(), Resources.getString("JAVA_3D_TEXT"), Resources.getString("JAVA_3D_TITLE"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (confirm == JOptionPane.OK_OPTION) {
                Browser.showDialog(Resources.getURL("JAVA_3D_URL"));
            }
            return false;
        }
        return b;
    }

    /**
	 * General classloader tester.
	 * 
	 * @param classname
	 *            name of class to test
	 * @return the class or null if class can't be loaded.
	 */
    private static Class testClass(String classname) {
        Class c = null;
        try {
            c = Class.forName(classname);
        } catch (ClassNotFoundException exc) {
        }
        return c;
    }

    /**
	 * Sets the title of the Window.
	 * 
	 */
    public void setWindowTitle() {
        String title = Resources.getString("APPLICATION_TITLE");
        if (projectfile != null) {
            title += " [" + projectfile.getName() + "]";
        } else {
            title += " [" + Resources.getString("FILE_UNNAMED") + "]";
        }
        this.setTitle(title);
    }

    /**
	 * Prompt a new Project.
	 * 
	 */
    public void promptNew() {
        setDirty();
        Project proj = new Project(Resources.getString("PROJECT"));
        proj.setCreationParameter(null);
        proj.setChangesParameter(null);
        promptNew(proj);
        Logger.logMessage("Created new project.", Logger.PROJECT);
    }

    /**
	 * Adds an operator for the given <code>JGivenPlotable</code>.
	 * 
	 * @param plotable
	 */
    public void addOperator(JViToPlotable plotable) {
        Source source = null;
        if (plotable instanceof ExampleSet) {
            source = new GivenExampleSet((ExampleSet) plotable);
        }
        if (plotable instanceof JMySVMModel) {
            source = new GivenJMySVMModel((JMySVMModel) plotable);
        }
        if (source != null) {
            JViTo.getApplication().navigator.addSource(source);
        }
    }

    /**
	 * Prompt this Project as new.
	 * 
	 * @param project
	 */
    public void promptNew(Project project) {
        if (opened) promptClose();
        initComponentsDesktop(project);
        desktop.add(navi_window);
        desktop.add(insp_window);
        menu.enableNew();
        menu.updateWindowMenu();
        toolbar.enableNew();
        opened = true;
    }

    /**
	 * Prompt open a Project from a file.
	 * 
	 */
    public void promptOpen() {
        File file = null;
        JFileChooser chooser = new JFileChooser(JViTo.getWorkingDir());
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = chooser.getSelectedFile();
        } else {
            return;
        }
        promptOpen(file);
    }

    /**
	 * Prompt opem a Project from this file.
	 * 
	 * @param file
	 */
    public void promptOpen(File file) {
        if (!file.exists()) {
            JOptionPane.showMessageDialog(this, "The file " + file.getName() + " don't exists!", "File not exists...", JOptionPane.YES_NO_OPTION);
            return;
        }
        JViTo.setWorkingDir(file.toString());
        Project project = new Project();
        try {
            try {
                Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
                project.loadXMLDocument(document.getDocumentElement());
            } catch (IOException e) {
                throw new XMLException(e.getMessage(), e);
            } catch (javax.xml.parsers.ParserConfigurationException e) {
                throw new XMLException(e.toString(), e);
            } catch (SAXException e) {
                throw new XMLException("Cannot parse document: " + e, e);
            }
        } catch (XMLException e) {
            JOptionPane.showMessageDialog(this, "Error while loading the XML-Document: " + file.getName() + "\n" + e.getMessage(), "Error...", JOptionPane.ERROR_MESSAGE);
            return;
        }
        promptNew(project);
        this.projectfile = file;
        this.dirty = false;
        JViTo.setWorkingDir(file.getParent().toString());
        setWindowTitle();
        this.menu.setMenuItemEnabled("MENU_FILE_SAVE", false);
        this.toolbar.setButtonEnabled("TOOLBAR_SAVE", false);
        Logger.logMessage("Opened file: " + file.toString(), Logger.PROJECT);
    }

    /**
	 * Promopt save the current project.
	 * 
	 */
    public void promptSave() {
        if ((projectfile != null) && (projectfile.exists())) {
            try {
                getProject().setChangesParameter(null);
                FileOutputStream out = new FileOutputStream(projectfile);
                out.write(this.getProject().getXML("").getBytes());
                out.close();
                Logger.logMessage("Project saved: " + projectfile.toString(), Logger.TASK);
                dirty = false;
                this.menu.setMenuItemEnabled("MENU_FILE_SAVE", false);
                this.toolbar.setButtonEnabled("TOOLBAR_SAVE", false);
            } catch (IOException e) {
                Logger.logException("Error while saving the Project", e);
            }
        } else {
            this.promptSaveAs();
        }
    }

    /**
	 * Prompt save the current project as another file.
	 * 
	 */
    public void promptSaveAs() {
        File file = null;
        JFileChooser chooser = new JFileChooser(JViTo.getWorkingDir());
        XMLFileFilter filter = new XMLFileFilter();
        chooser.setFileFilter(filter);
        int returnVal = chooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = chooser.getSelectedFile();
        } else {
            return;
        }
        if (file.exists()) {
            int confirm = JOptionPane.showConfirmDialog(this, Resources.getString("DIALOG_OVERWRITE_QUESTION") + " " + file.getName() + "?", Resources.getString("DIALOG_OVERWRITE_TITLE"), JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }
        if (file != null) {
            try {
                getProject().setChangesParameter(null);
                FileOutputStream out = new FileOutputStream(file);
                out.write(this.getProject().getXML("").getBytes());
                out.close();
                Logger.logMessage("Project saved as: " + file.toString(), Logger.TASK);
            } catch (IOException e) {
                Logger.logException("Error while saving the Project", e);
            }
        }
        projectfile = new File(file.toString());
        JViTo.setWorkingDir(file.getParent().toString());
        setWindowTitle();
        dirty = false;
        this.menu.setMenuItemEnabled("MENU_FILE_SAVE", false);
        this.toolbar.setButtonEnabled("TOOLBAR_SAVE", false);
    }

    /**
	 * Prompt close current project.
	 * 
	 */
    public void promptClose() {
        if (isDirty()) {
            int ret = JOptionPane.showConfirmDialog(this, Resources.getString("DIALOG_SAVE_QUESTION"), Resources.getString("DIALOG_SAVE_TITLE"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (ret == JOptionPane.CANCEL_OPTION) {
                return;
            }
            if (ret == JOptionPane.YES_OPTION) {
                promptSave();
            }
        }
        setPreferences();
        desktop.removeAll();
        desktop.add(logviewer);
        desktop.repaint();
        projectfile = null;
        setWindowTitle();
        menu.enableClose();
        menu.updateWindowMenu();
        toolbar.enableClose();
        opened = false;
        dirty = false;
        Logger.logMessage("Closed the project.", Logger.PROJECT);
    }

    /**
	 * Gets the information wether the application is open or not. It does not indicate that a project is open!
	 */
    public boolean isOpen() {
        return open;
    }

    private void setPreferences() {
        if (opened) {
            pref.setInt("navi_window_x", navi_window.getLocation().x);
            pref.setInt("navi_window_y", navi_window.getLocation().y);
            pref.setInt("navi_window_width", navi_window.getSize().width);
            pref.setInt("navi_window_height", navi_window.getSize().height);
            pref.setInt("insp_window_x", insp_window.getLocation().x);
            pref.setInt("insp_window_y", insp_window.getLocation().y);
            pref.setInt("insp_window_width", insp_window.getSize().width);
            pref.setInt("insp_window_height", insp_window.getSize().height);
        }
        pref.setInt("logviewer_x", logviewer.getLocation().x);
        pref.setInt("logviewer_y", logviewer.getLocation().y);
        pref.setInt("logviewer_width", logviewer.getSize().width);
        pref.setInt("logviewer_height", logviewer.getSize().height);
        pref.setInt("jvito_window_x", this.getLocation().x);
        pref.setInt("jvito_window_y", this.getLocation().y);
        pref.setInt("jvito_window_width", this.getSize().width);
        pref.setInt("jvito_window_height", this.getSize().height);
        pref.setString("workingdir", JViTo.getWorkingDir());
        File f = Logger.getLogFile();
        String file = "";
        if (f != null) file = f.toString();
        pref.setString("logfile", file);
        pref.setInt("logverbosity", Logger.getVerbosityLevel());
        String recent = menu.getRecentFiles().toString();
        pref.setString("recent_files", recent);
    }

    /**
	 * Gets the <code>Preferences</code>.
	 */
    public Preferences getPreferences() {
        return this.pref;
    }

    /**
	 * Prompt exit this <code>Application</code>.
	 * 
	 */
    public void promptExit() {
        if (opened) promptClose();
        pref.setInt("logviewer_x", logviewer.getLocation().x);
        pref.setInt("logviewer_y", logviewer.getLocation().y);
        pref.setInt("logviewer_width", logviewer.getSize().width);
        pref.setInt("logviewer_height", logviewer.getSize().height);
        pref.setInt("jpeg_quality", jpeg_quality);
        try {
            pref.save();
            Logger.logMessage("Preferences saved.", Logger.TASK);
        } catch (IOException error) {
            Logger.logException("Can't save the preferences!", error);
        }
        Logger.logMessage("Exiting JViTo.", Logger.TASK);
        open = false;
        if (exit_type == DISPOSE_ON_EXIT) {
            dispose();
        } else {
            System.exit(0);
        }
    }

    /**
	 * Prompt compile with this Compilertask.
	 * 
	 * @param task
	 * @see com.jvito.compile.JViToCompiler#COMPILE
	 * @see com.jvito.compile.JViToCompiler#COMPILE_ALL
	 * @see com.jvito.compile.JViToCompiler#COMPILE_ALL_CHILDS
	 * @see com.jvito.compile.JViToCompiler#COMPILE_ALL_PARENTS
	 */
    public void promptCompile(int task) {
        MutableTreeNode node = navigator.getSelectedTreeNode();
        Compileable object;
        if (navigator.isProjectNodeSelected()) {
            object = (((ProjectTreeNode) node).getProject());
        } else if (navigator.isSourceNodeSelected()) {
            object = (((SourceTreeNode) node).getSource());
        } else if (navigator.isPlotNodeSelected()) {
            object = (((PlotTreeNode) node).getPlot());
        } else {
            return;
        }
        compiler = new JViToCompiler(object, task);
        Logger.logMessage("Start compiling: " + compiler.getTaskName(), Logger.TASK);
        Thread t = new Thread(compiler);
        CompilingDialog dialog = new CompilingDialog(this, compiler, t);
        t.start();
        dialog.setVisible(true);
        if (compiler.getException() != null) {
            Logger.logException("Error while compiling", compiler.getException());
        } else {
            Logger.logMessage("Done.", Logger.TASK);
        }
        navigator.refresh();
    }

    /**
	 * Prompt show the data of the selected <code>SourceTreeNode</code>.
	 * 
	 */
    public void promptShowData() {
        if (!navigator.isSourceNodeSelected()) return;
        Source source = ((SourceTreeNode) navigator.getSelectedTreeNode()).getSource();
        if (!(source instanceof FileSource)) {
            if (!source.isCompiled()) return;
            ExampleSet set = source.getExampleSet();
            if (set == null) return;
            AttributeEditorWindow window = new AttributeEditorWindow(this, set);
            try {
                window.setSize(pref.getInt("attributeeditor_width"), pref.getInt("attributeeditor_height"));
            } catch (KeyException error) {
                window.setSize(500, 500);
            }
            try {
                window.setLocation(pref.getInt("attributeeditor_x"), pref.getInt("attributeeditor_y"));
            } catch (KeyException error) {
                window.setLocation(0, 0);
            }
            window.setVisible(true);
            return;
        } else {
            String file = source.getParameterAsString("attributes");
            if (file.equals("")) {
                file = null;
            }
            File f = null;
            if (file != null) {
                f = new File(file);
            }
            AttributeEditorWindow window = new AttributeEditorWindow(this, source, f);
            window.setVisible(true);
            return;
        }
    }

    public void updateDataTableWindow(Source source) {
        JInternalFrame[] frames = desktop.getAllFrames();
        for (int f = 0; f < frames.length; f++) {
            if (frames[f] instanceof DataTableWindow) {
                if (frames[f].getTitle().equals(Resources.getString("DATATABLE") + " " + source.getOldName())) {
                    frames[f].setTitle(Resources.getString("DATATABLE") + " " + source.getName());
                }
            }
        }
    }

    public void updatePlotWindow(Plot plot) {
        JInternalFrame[] frames = desktop.getAllFrames();
        for (int f = 0; f < frames.length; f++) {
            if (frames[f] instanceof PlotWindow) {
                if (frames[f].getTitle().equals(plot.getOldName())) {
                    frames[f].setTitle(plot.getName());
                }
            }
        }
    }

    /**
	 * Prompt show the visualization component of the selected <code>SourceTreeNode</code>.
	 * 
	 * @see com.rapidminer.operator.ResultObject#getVisualizationComponent(com.rapidminer.operator.IOContainer)
	 */
    public void promptShowVisualizationComponent() {
        if (!navigator.isSourceNodeSelected()) return;
        Source source = ((SourceTreeNode) navigator.getSelectedTreeNode()).getSource();
        if (!source.isCompiled()) return;
        JInternalFrame[] frames = this.desktop.getAllFrames();
        for (int f = 0; f < frames.length; f++) {
            if (frames[f] instanceof PlotWindow) {
                if (((PlotWindow) frames[f]).getTitle().equals(source.getName())) {
                    ((PlotWindow) frames[f]).toFront();
                    try {
                        ((PlotWindow) frames[f]).setSelected(true);
                    } catch (Exception error) {
                        Logger.logException(error.getMessage(), error);
                    }
                    menu.updateWindowMenu();
                    return;
                }
            }
        }
        PlotWindow window = new PlotWindow(source);
        try {
            window.setSize(pref.getInt("plotwindow_width"), pref.getInt("plotwindow_height"));
        } catch (KeyException error) {
            window.setSize(700, 400);
        }
        window.setLocation(300, 0);
        this.desktop.add(window);
        source.setJInternalFrame(window);
        window.show();
        menu.updateWindowMenu();
        Logger.logMessage("Showing Visualization Component: " + source.getName(), Logger.TASK);
    }

    /**
	 * Prompt show the plot of the selected <code>PlotTreeNode</code>.
	 * 
	 */
    public void promptShowPlot() {
        if (!navigator.isPlotNodeSelected()) return;
        Plot plot = ((PlotTreeNode) navigator.getSelectedTreeNode()).getPlot();
        if (!plot.isCompiled()) return;
        JInternalFrame[] frames = this.desktop.getAllFrames();
        for (int f = 0; f < frames.length; f++) {
            if (frames[f] instanceof PlotWindow) {
                if (((PlotWindow) frames[f]).getTitle().equals(plot.getName())) {
                    ((PlotWindow) frames[f]).toFront();
                    try {
                        ((PlotWindow) frames[f]).setSelected(true);
                    } catch (Exception error) {
                        Logger.logException(error.getMessage(), error);
                    }
                    menu.updateWindowMenu();
                    return;
                }
            }
        }
        PlotWindow window = new PlotWindow(plot);
        try {
            window.setSize(pref.getInt("plotwindow_width"), pref.getInt("plotwindow_height"));
        } catch (KeyException error) {
            window.setSize(700, 400);
        }
        window.setLocation(300, 0);
        this.desktop.add(window);
        plot.setJInternalFrame(window);
        window.show();
        menu.updateWindowMenu();
        Logger.logMessage("Showing plot: " + plot.getName(), Logger.TASK);
    }

    /**
	 * Prompt save the plot-image of the current selected <code>PlotTreeNode</code>.
	 * 
	 */
    public void promptSaveAllPlots() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) navigator.getModel().getRoot();
        LinkedList<DefaultMutableTreeNode> nodes = new LinkedList<DefaultMutableTreeNode>();
        nodes.add(node);
        DefaultMutableTreeNode childnode;
        Plot plot;
        while (nodes.size() > 0) {
            node = nodes.removeFirst();
            for (int i = 0; i < node.getChildCount(); i++) {
                childnode = (DefaultMutableTreeNode) node.getChildAt(i);
                nodes.addLast(childnode);
                if (childnode instanceof PlotTreeNode) {
                    plot = ((PlotTreeNode) childnode).getPlot();
                    saveComponent(this, "Save Plot...", plot.getSaveableComponent(), plot.getName());
                }
            }
        }
        Logger.logMessage("Saved all images of all plots.", Logger.TASK);
    }

    /**
	 * Prompt save the plot-image of the current selected <code>PlotTreeNode</code>.
	 * 
	 */
    public void promptSavePlot() {
        if (!navigator.isPlotNodeSelected()) return;
        Plot plot = ((PlotTreeNode) navigator.getSelectedTreeNode()).getPlot();
        if (!plot.isCompiled()) return;
        saveComponent(this, "Save Plot...", plot.getSaveableComponent(), plot.getName());
        Logger.logMessage("Saved the image of plot: " + plot.getName(), Logger.TASK);
    }

    /**
	 * Saves a component as an image in file (eps,ps,pdf,png,jpg).
	 * 
	 * @param parent
	 *            the parent frame
	 * @param title
	 *            the title of the dialog
	 * @param tosave
	 *            the component ro save
	 * @param filename
	 *            the default filename;
	 */
    public void saveComponent(Component parent, String title, Component tosave, String filename) {
        if (tosave == null) {
            return;
        }
        ExportDialog exportDialog = new ExportDialog("JViTo");
        if (parent == null) parent = this;
        exportDialog.showExportDialog(parent, title, tosave, filename);
    }

    /**
	 * Gets current project.
	 */
    public Project getProject() {
        return ((ProjectTreeNode) this.navigator.getModel().getRoot()).getProject();
    }

    /**
	 * Gets the file of the current project.
	 */
    public File getProjectFile() {
        return projectfile;
    }

    /**
	 * Is the <code>Application</code> dirty. The <code>Application</code> should be dirty iff the
	 * <code>Navigator</code> and/or the <code>Inspector</code> changed. This means iff you are adding or removeing
	 * some nodes from the <code>Navigator</code> and/or ou edited some Parameters in the <code>Inspector</code>.
	 */
    public boolean isDirty() {
        return dirty;
    }

    /**
	 * Sets the <code>Application</code> dirty. This means that the current has to be saved.
	 */
    public void setDirty() {
        this.menu.setMenuItemEnabled("MENU_FILE_SAVE", true);
        this.toolbar.setButtonEnabled("TOOLBAR_SAVE", true);
        dirty = true;
    }

    private class WindowClosingListener extends WindowAdapter {

        /**
		 * Erzeugt einen WindowClosingAdapter zum Schliessen des Fensters. Ist exitSystem true, wird das komplette
		 * Programm beendet.
		 */
        public WindowClosingListener(boolean exitSystem) {
        }

        /**
		 * Erzeugt einen WindowClosingAdapter zum Schliessen des Fensters. Das Programm wird nicht beendet.
		 */
        public WindowClosingListener() {
            this(true);
        }

        @Override
        public void windowClosing(WindowEvent event) {
            promptExit();
        }
    }

    /**
	 * Gets the <code>JDesktopPane</code>.
	 */
    public JDesktopPane getJDesktopPane() {
        return desktop;
    }

    /**
	 * Adds input-objects of type JViToPlotable to visualize them
	 * 
	 * @param input
	 *            Only JViToPlotable-Objects are added from the collection.
	 */
    public void addInput(Collection<?> input) {
        Iterator iter = input.iterator();
        int objects = 0;
        while (iter.hasNext()) {
            Object obj = iter.next();
            if (obj instanceof JViToPlotable) {
                this.input.add(obj);
                objects++;
            }
        }
        if (objects > 0) {
            if (!opened) {
                int confirm = JOptionPane.showConfirmDialog(this, Resources.getString("DIALOG_INPUT_AVAILABLE") + "\n" + Resources.getString("DIALOG_INPUT_NEW_PROJECT"), Resources.getString("DIALOG_INPUT_AVAILABLE_TITLE"), JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    this.promptNew();
                    showInputDialog();
                }
            } else {
                int confirm = JOptionPane.showConfirmDialog(this, Resources.getString("DIALOG_INPUT_AVAILABLE") + "\n" + Resources.getString("DIALOG_INPUT_SHOW"), Resources.getString("DIALOG_INPUT_AVAILABLE_TITLE"), JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    showInputDialog();
                }
            }
        }
    }

    /**
	 * Gets the input. E.g. JViToPlotable-Objects from RapidMiner.
	 */
    public Collection getInput() {
        return input;
    }

    /**
	 * Shows the InputDialog to add, remove or relace input-objects.
	 */
    public void showInputDialog() {
    }

    /**
	 * Sets the quality for saving a jpeg-image. Take care that the argument is between 0 and 100.
	 */
    public void setJPEGQuality(int quality) {
        if ((quality >= 0) && (quality <= 100)) {
            this.jpeg_quality = quality;
        }
    }

    /**
	 * Gets the quality for saving a jpeg-image .
	 */
    public int getJPEGQuality() {
        return jpeg_quality;
    }

    /**
	 * Shows the <code>ExampleColoring</code> of the current selected Source-node.
	 */
    public void promptShowExampleColoring() {
        if (!navigator.isSourceNodeSelected()) return;
        Source source = ((SourceTreeNode) navigator.getSelectedTreeNode()).getSource();
        if (!source.isCompiled()) return;
        new ExampleColoringDialog(this, source.getExampleSet(), source.getExampleColoring());
    }
}
