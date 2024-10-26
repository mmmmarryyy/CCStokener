package org.xaware.ide.xadev;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ISavedState;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.internal.editors.text.JavaFileEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.jdom.Element;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.w3c.dom.Document;
import org.xaware.api.XABizView;
import org.xaware.ide.runtime.XAClassLoader;
import org.xaware.ide.shared.BuildXAwareHomeDesigner;
import org.xaware.ide.shared.UserPrefs;
import org.xaware.ide.xadev.common.ControlFactory;
import org.xaware.ide.xadev.common.GlobalConstants;
import org.xaware.ide.xadev.common.ResourceUtils;
import org.xaware.ide.xadev.datamodel.JDOMContentFactory;
import org.xaware.ide.xadev.datamodel.NamespaceTable;
import org.xaware.ide.xadev.datamodel.XMLTreeNode;
import org.xaware.ide.xadev.gui.actions.ActionController;
import org.xaware.ide.xadev.gui.actions.UndoableInfoEdit;
import org.xaware.ide.xadev.gui.actions.XAUndoContext;
import org.xaware.ide.xadev.gui.dialog.ClassBrowserDlg;
import org.xaware.ide.xadev.gui.dialog.XAwareInstallationCallbackDlg;
import org.xaware.ide.xadev.gui.editor.XAInternalFrame;
import org.xaware.ide.xadev.gui.view.LogView;
import org.xaware.ide.xadev.gui.view.NavigatorView;
import org.xaware.ide.xadev.gui.view.palette.model.PaletteManager;
import org.xaware.ide.xadev.gui.xmleditor.IXMLEditor;
import org.xaware.ide.xadev.gui.xmleditor.xaware.parser.UserConfigReader;
import org.xaware.ide.xadev.gui.xmleditor.xaware.parser.XMLStyledDocument;
import org.xaware.ide.xadev.runtime.RuntimeStatePrefs;
import org.xaware.ide.xadev.security.XASecurityManager;
import org.xaware.ide.xadev.tools.gui.updatewartool.UpdateXAwareWarEarWin;
import org.xaware.ide.xadev.wizard.WizardException;
import org.xaware.ide.xadev.wizard.WizardFactory;
import org.xaware.shared.i18n.Translator;
import org.xaware.shared.util.XASystemProps;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * Main plugin class for XA-Designer
 * 
 * @author T Vasu
 * @author Y Srihari Prasanth
 * @version 1.0
 */
public class XA_Designer_Plugin extends AbstractUIPlugin implements IWindowListener, IPageListener, IPartListener, IPartListener2, IPerspectiveListener, IOperationHistoryListener {

    /** Logger for XAware. */
    private static XAwareLogger logger = null;

    private static final String className = "XA_Designer_Plugin";

    /** Static instance of XA-Designer plugin. */
    private static XA_Designer_Plugin plugin;

    /** Eclipse install directory. */
    private static String eclipseInstallDir = null;

    /** Plugin ID. */
    private static final String PLUGIN_ID = "org.xaware.designer";

    private static final String DESIGNER_JAR_NAME = "xa-designer.jar";

    /** Instance of Translator. */
    private static Translator translator;

    /** Holds Reference to the WizardFactory */
    public static WizardFactory wizardFactory = null;

    /** Clipboard instance. */
    private static Clipboard clipboard;

    /** Busy shell Stack. */
    private static Stack<ShellStatus> busyShellStack = new Stack<ShellStatus>();

    /** Context ClassLoader for this thread prior to starting plugin */
    private static ClassLoader previousClassLoader = null;

    /** BundleContext instance. */
    private static BundleContext bundleContext;

    /** XMLStyledDocument instance. */
    private static XMLStyledDocument xmlStyledDocument = null;

    /** Holds Reference to DefaultUndoManager UndoContext. */
    private static XAUndoContext undoContext = null;

    /** Holds the name of the Palette Restore Node */
    private static final String PALETTE_NODE = "PaletteState";

    /** This is the file name of the the initial Designer restore state */
    private File lastSaveFile = null;

    /** Boolean instance representing Import operation. */
    private boolean isImport = false;

    /** Search string in Log view using for persistency */
    private String logSearchStr;

    /** ClassLoader able to access jars from static jar directories */
    private XAClassLoader staticClassLoader = null;

    /** xaware.home string */
    private static final String xahomeStr = "xaware.home";

    /** xaware.home.version string */
    private final String xahomeVersionStr = "xaware.home.version";

    /**
     * The constructor.
     */
    public XA_Designer_Plugin() {
        super();
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation
     * 
     * @param context
     *            bundle context
     * 
     * @throws Exception
     *             if unable to start.
     */
    public void start(BundleContext context) throws Exception {
        try {
            super.start(context);
            XA_Designer_Plugin.bundleContext = context;
            if (eclipseInstallDir == null) {
                eclipseInstallDir = Platform.getInstallLocation().getURL().getPath();
                eclipseInstallDir = getOSDir(eclipseInstallDir);
                System.out.println("Eclipse install directory: " + eclipseInstallDir);
            }
            getDefault().getWorkbench().getHelpSystem().resolve("", true);
            callBackToXAware();
            setupXAwareHome();
            org.xaware.shared.util.logging.LogConfigUtil.getInstance().setupGlobalLog();
            XABizView.initialize(XABizView.MODE_DESIGNER);
            UserPrefs.initSystemProps();
            translator = Translator.getInstance();
            undoContext = new XAUndoContext();
            previousClassLoader = Thread.currentThread().getContextClassLoader();
            staticClassLoader = XAClassLoader.getStaticClassLoader(XA_Designer_Plugin.class.getClassLoader());
            RuntimeStatePrefs.getInstance();
            XASystemProps.getInstance();
            ActionController.getInstance();
            getWorkbench().addWindowListener(this);
            if (getWorkbench().getActiveWorkbenchWindow() != null) {
                getWorkbench().getActiveWorkbenchWindow().addPageListener(this);
                getWorkbench().getActiveWorkbenchWindow().addPerspectiveListener(this);
            }
            logger = XAwareLogger.getXAwareLogger(XA_Designer_Plugin.class.getName());
            System.setSecurityManager(new XASecurityManager());
            try {
                wizardFactory = WizardFactory.getDefaultInstance();
            } catch (Exception exception) {
                logger.severe("Unable to initialize the WizardFactory " + exception.getMessage());
                logger.printStackTrace(exception);
            }
            clipboard = new Clipboard(Display.getCurrent());
            windowActivated(getWorkbench().getActiveWorkbenchWindow());
            XA_Designer_Plugin.getDefault().getWorkbench().getOperationSupport().getOperationHistory().addOperationHistoryListener(this);
            Display.getDefault().asyncExec(new Runnable() {

                public void run() {
                    new ClassBrowserDlg.LoadClassesThread().start();
                }
            });
            XAWorkspaceSaveParticipant saveParticipant = new XAWorkspaceSaveParticipant();
            ISavedState lastState = ResourcesPlugin.getWorkspace().addSaveParticipant(this, saveParticipant);
            if (lastState != null) {
                IPath location = lastState.lookup(new Path(saveParticipant.getSAVE_FILE_NAME()));
                if (location != null) {
                    readStateFrom(getStateLocation().append(location).toFile());
                }
            }
        } catch (Exception e) {
            String errMsg = "Exception encountered in XA_Designer_Plugin ctor: " + e;
            XADesignerLogger.logError(errMsg, e);
            System.out.println(errMsg);
            e.printStackTrace();
            if (logger != null) {
                logger.printStackTrace(e);
                logger.severe(errMsg);
            }
            throw e;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

    /**
     * Perfomrs call back activity which occurs only for the first time when 
     * designer opened after the installation.
     */
    private void callBackToXAware() {
        final String INSTALLED = "Installed";
        ScopedPreferenceStore preferenceStore = new ScopedPreferenceStore(new ConfigurationScope(), getBundle().getSymbolicName());
        if (!preferenceStore.getBoolean(INSTALLED)) {
            new XAwareInstallationCallbackDlg();
            preferenceStore.setValue(INSTALLED, true);
            try {
                preferenceStore.save();
            } catch (IOException e) {
                ControlFactory.showErrorDialog("Unable to store Call back information to the eclipse preferences.", "Error");
            }
        }
    }

    /**
     * check preference store for xaware.home if not found allow the user to set it. then set the system property
     * xaware.home
     * 
     * @throws Exception
     */
    private void setupXAwareHome() throws Exception {
        ScopedPreferenceStore prefStore = (ScopedPreferenceStore) XA_Designer_Plugin.getDefault().getPreferenceStore();
        String systemXawareHomeDir = System.getProperty(xahomeStr);
        String prefXawareHomeDir = prefStore.getString(xahomeStr);
        String bundleVersion = (String) this.getBundle().getHeaders().get("Bundle-Version");
        if (systemXawareHomeDir == null || systemXawareHomeDir.length() <= 0) {
            if (prefXawareHomeDir == null || prefXawareHomeDir.length() <= 0) {
                prefXawareHomeDir = initializeXAwareHome(bundleVersion, prefStore, null);
            } else {
                String xawareHomeVersion = getXAwareHomeVersion(prefXawareHomeDir);
                if (!xawareHomeVersion.equals(bundleVersion)) {
                    prefXawareHomeDir = initializeXAwareHome(bundleVersion, prefStore, prefXawareHomeDir);
                }
            }
            System.setProperty(xahomeStr, prefXawareHomeDir);
        } else {
            String xawareHomeVersion = getXAwareHomeVersion(systemXawareHomeDir);
            if (!xawareHomeVersion.equals(bundleVersion)) {
                prefXawareHomeDir = initializeXAwareHome(bundleVersion, prefStore, systemXawareHomeDir);
                System.setProperty(xahomeStr, prefXawareHomeDir);
            } else {
                System.setProperty(xahomeStr, systemXawareHomeDir);
            }
        }
        System.out.println(xahomeStr + " = " + getPluginRootPath());
        System.out.println("xaware root directory = " + getXAwareRootPath());
    }

    private String getXAwareHomeVersion(String path) {
        String xawareHomeVersion = "";
        File versionFile = new File(path + File.separator + "version.txt");
        if (versionFile.exists()) {
            BufferedReader in = null;
            try {
                in = new BufferedReader(new FileReader(versionFile));
                xawareHomeVersion = in.readLine();
            } catch (IOException e) {
                xawareHomeVersion = "";
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException e) {
                }
            }
        }
        return xawareHomeVersion;
    }

    /**
     * Call installDesignerHome dialog getting xaware.home and set it in the system preferences.
     * 
     * @param pluginMod
     * @param prefStore
     * @param xawareInitHome 
     * @param xaHomeS 
     * @param xawareHomeDir 
     * @throws Exception 
     */
    private String initializeXAwareHome(String bundleVersion, ScopedPreferenceStore prefStore, String xawareInitHome) throws Exception {
        String xawareHomeDir = getXAwareHome(xawareInitHome);
        if (xawareHomeDir == null) {
            throw new Exception("No xaware.home set!");
        }
        String xawareHomeVersion = this.getXAwareHomeVersion(xawareHomeDir);
        if (!xawareHomeVersion.equals(bundleVersion)) {
            boolean overwrite;
            try {
                overwrite = shouldOverwrite(xawareHomeDir);
            } catch (Exception e) {
                xawareHomeDir = initializeXAwareHome(bundleVersion, prefStore, xawareHomeDir);
                return xawareHomeDir;
            }
            installDesignerHome(xawareHomeDir, overwrite);
        }
        prefStore.setValue(xahomeStr, xawareHomeDir);
        System.out.println("set " + xahomeStr + " = " + xawareHomeDir);
        prefStore.save();
        return xawareHomeDir;
    }

    /**
	 * check directory for any other files.  If other files are found prompt
	 * the user to determine if they want to overwrite or not.
	 * @param xawareHomeDir2
	 * @return
	 * @throws Exception throw exception if cancel pressed
	 */
    private boolean shouldOverwrite(String homedir) throws Exception {
        File homeDirFile = new File(homedir);
        String[] files = homeDirFile.list();
        if (files != null && files.length > 0) {
            String[] tab = new String[3];
            tab[0] = IDialogConstants.YES_LABEL;
            tab[1] = IDialogConstants.NO_LABEL;
            tab[2] = IDialogConstants.CANCEL_LABEL;
            MessageDialog dialog = new MessageDialog(XA_Designer_Plugin.getShell(), "Overwrite Directory", null, "Your selected directory " + homedir + " \ncontains files.  Do you wish to overwrite them?", MessageDialog.QUESTION, tab, 0);
            int retVal = dialog.open();
            retVal = dialog.getReturnCode();
            if (retVal == 0) {
                return true;
            } else if (retVal == 2) {
                throw new Exception("Cancel selected!");
            }
        }
        return false;
    }

    /**
     * Opens the dialog for finding a directory for XAwareHome
     */
    protected String getXAwareHome(String xawareInitHome) {
        String detailMsg = "XAware Designer home not found. Select a directory to assign the Designer home now. ";
        if (xawareInitHome != null) {
            detailMsg = "Select a directory to assign the Designer home now.";
        }
        DirectoryDialog dialog = new DirectoryDialog(XA_Designer_Plugin.getShell());
        dialog.setText("Browse to select XAware Designer home");
        dialog.setMessage(detailMsg);
        String dirName = getXAwareRootPath();
        if (xawareInitHome != null) {
            dirName = xawareInitHome;
        }
        dialog.setFilterPath(new Path(dirName).toOSString());
        return dialog.open();
    }

    /**
     * Use designerhome.jar to build the directories required by designer and extract the jars from the plugin required
     * for external scripts to run.
     */
    private void installDesignerHome(String xawareHome, boolean overwrite) {
        try {
            String symbolicName = "org.xaware.designer";
            Bundle bundle = Platform.getBundle(symbolicName);
            URL url = bundle.getEntry("/home/designerhome.jar");
            BuildXAwareHomeDesigner xaHomeBuilder = new BuildXAwareHomeDesigner();
            xaHomeBuilder.buildHome(xawareHome, url.openStream(), overwrite);
            xaHomeBuilder.extractJarsFromResources(xawareHome);
        } catch (Exception e) {
            System.out.println("FAILED TO INSTALL DESIGNER HOME: " + e);
        }
    }

    public void savePluginState(boolean fullSave) {
        try {
            ResourcesPlugin.getWorkspace().save(fullSave, new NullProgressMonitor());
        } catch (CoreException e) {
            XADesignerLogger.logError("Failed to save Designer state", e);
        }
    }

    public void restorePluginState() {
        if (lastSaveFile != null) {
            readStateFrom(lastSaveFile);
        }
    }

    protected void readStateFrom(File restoreFile) {
        Document doc = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(restoreFile);
        } catch (java.io.IOException e) {
            String errMsg = "Can't find the restore file: " + restoreFile.getAbsolutePath();
            System.out.println(errMsg);
            XADesignerLogger.logError(errMsg, e);
            return;
        } catch (Exception e) {
            String errMsg = "Problem parsing the last Designer state save file:" + restoreFile.getAbsolutePath();
            System.out.print(errMsg);
            XADesignerLogger.logError(errMsg, e);
            return;
        }
        org.w3c.dom.Element root = doc.getDocumentElement();
        UpdateXAwareWarEarWin.restorePersistentState(doc);
        org.w3c.dom.NodeList paletteNodes = root.getElementsByTagName(PALETTE_NODE);
        if (paletteNodes.getLength() > 0) {
            if (paletteNodes.getLength() > 1) {
                XADesignerLogger.log(IStatus.WARNING, IStatus.OK, "There are " + paletteNodes.getLength() + " Palette restore nodes in the Designer " + "save state file, only the first will be " + "used to restore the palette", null);
            }
            org.w3c.dom.Element palette = (org.w3c.dom.Element) paletteNodes.item(0);
            XMLMemento m = new XMLMemento(doc, palette);
            PaletteManager.getManager().init(m);
        }
    }

    public void writeImportantState(File f) {
        org.w3c.dom.Document doc = null;
        org.w3c.dom.Element root = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            doc = parser.newDocument();
            root = doc.createElement("DesignerState");
            doc.appendChild(root);
        } catch (Exception e) {
            XADesignerLogger.logError("Failed to establish initial save Document container", e);
            return;
        }
        org.w3c.dom.Element palette = doc.createElement(PALETTE_NODE);
        root.appendChild(palette);
        XMLMemento m = new XMLMemento(doc, palette);
        PaletteManager.getManager().saveState(m);
        UpdateXAwareWarEarWin.storePersistentState(doc);
        String errMsg = "Failed to create the Designer State save in workspace";
        try {
            OutputStream os = new FileOutputStream(f);
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(os);
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = transFactory.newTransformer();
            transformer.transform(source, result);
            errMsg = "Successfully persisted Designer's state";
        } catch (FileNotFoundException e) {
            XADesignerLogger.logError(errMsg, e);
        } catch (TransformerConfigurationException e) {
            XADesignerLogger.logError(errMsg, e);
        } catch (TransformerException e) {
            XADesignerLogger.logError(errMsg, e);
        } catch (Throwable t) {
            XADesignerLogger.logError(t);
        }
    }

    /**
     * Gets a File reference to a directory where temp files may be stored.
     * Creates the directory if it does not already exist.
     * 
     * @return a File reference to a directory where temp files may be stored.
     */
    public static File getTempDirectory() {
        final String TEMP_DIR = "temp";
        File tempDir = plugin.getStateLocation().append(TEMP_DIR).toFile();
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        return tempDir;
    }

    /**
     * Alignes the shell to center of screen.
     * 
     * @param dialog
     *            dialog instance.
     */
    public static void alignDialogToCenter(Shell dialog) {
        Rectangle screenSize = getShell().getBounds();
        Point dialogSize = dialog.getSize();
        int x = (screenSize.width - dialogSize.x) / 2;
        int y = (screenSize.height - dialogSize.y) / 2;
        dialog.setLocation(x, y);
    }

    /**
     * Gets Plugin Root Directory.
     * 
     * @return Absolute path of the plugin as string.
     */
    public static String getPluginRootPath() {
        return System.getProperty(xahomeStr);
    }

    /**
     * Gets the name of the Eclipse installation Directory.
     * 
     * @return Absolute path of the Eclipse installation Directory as a string.
     */
    public static String getXAwareRootPath() {
        return eclipseInstallDir;
    }

    /**
     * This method is called when the plug-in is stopped
     * 
     * @param context
     *            bundle context.
     * 
     * @throws Exception
     *             if unable to stop plugin.
     */
    public void stop(BundleContext context) throws Exception {
        final String methodName = "stop";
        try {
            if (previousClassLoader != null) {
                Thread.currentThread().setContextClassLoader(previousClassLoader);
            }
        } catch (Exception e) {
            String errMsg = "Error attempting to reset ContextClassLoader in stop method";
            XADesignerLogger.logError(errMsg, e);
            logger.severe(errMsg, className, methodName, e);
        } finally {
            previousClassLoader = null;
        }
        try {
            NamespaceTable.disposePlugin();
        } catch (Exception e) {
            String errMsg = "Error attempting to dispose schema models in stop method";
            XADesignerLogger.logError(errMsg, e);
            logger.severe(errMsg, className, methodName, e);
        }
        try {
            Level level = LogView.getLogLevel();
            if (level != null) {
                RuntimeStatePrefs.setDefaultGuiLogLevel(level.getName());
            }
            RuntimeStatePrefs.saveState();
        } catch (Throwable t) {
            String errMsg = "Error attempting to save runtime state preferences in stop method";
            XADesignerLogger.logError(errMsg, t);
            logger.severe(errMsg, className, methodName, t);
        }
        super.stop(context);
    }

    /**
     * Returns the shared instance.
     * 
     * @return Static instance of XA_Designer_Plugin
     */
    public static XA_Designer_Plugin getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in relative path.
     * 
     * @param path
     *            the path
     * 
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    /**
     * gets active workbench window shell.
     * 
     * @return shell instance.
     */
    public static Shell getShell() {
        if (getDefault().getWorkbench().getActiveWorkbenchWindow() != null) return getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(); else return null;
    }

    /**
     * Translator instance.
     * 
     * @return Static instance of Translator.
     */
    public static Translator getTranslator() {
        return translator;
    }

    /**
     * Gets the Plugin ID.
     * 
     * @return String.
     */
    public static String getPLUGIN_ID() {
        return PLUGIN_ID;
    }

    /**
     * To return the WizardFactory
     * 
     * @return WizardFactory instance
     */
    public static WizardFactory getWizardFactory() {
        return wizardFactory;
    }

    /**
     * To return xmlstyleddocument
     * 
     * @return xmlstyleddocument instance
     */
    public static XMLStyledDocument getXMLStyledDocument() {
        if (xmlStyledDocument == null) {
            xmlStyledDocument = UserConfigReader.parseGUIStyledDocumentElement();
        }
        return xmlStyledDocument;
    }

    public static IWorkbenchPage getActivePage() {
        return getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();
    }

    public static IPerspectiveDescriptor getPerspective() {
        return getActivePage().getPerspective();
    }

    /**
     * Gets Active editor directory.
     * 
     * @return directory of active editor, empty if active editor not found.
     */
    public static String getActiveEditedFileDirectory() {
        Object obj = XA_Designer_Plugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        if (obj != null) {
            if (obj instanceof XAInternalFrame) {
                String activeEdFileDir = ((XAInternalFrame) obj).getEditorFileDirectory();
                return activeEdFileDir;
            }
        }
        String xaRootPath = getXAwareRootPath();
        return xaRootPath;
    }

    /**
     * Gets Active editor .
     * 
     * @return active editor instance.
     */
    public static String getActiveEditedInternalFrameFileDirectory() {
        XAInternalFrame frame = getActiveEditedInternalFrame();
        if (frame != null) {
            String editorFileDir = frame.getEditorFileDirectory();
            return editorFileDir;
        }
        String xaRootPath = getXAwareRootPath();
        return xaRootPath;
    }

    /**
     * Gets UndoContext of the activeEditedInternalFrame adds it to the UndoableInfoEdit, if activeEditedInternalFrame
     * not exists then cteats the new UndoContext.
     * 
     * @param uie
     *            DOCUMENT ME!
     */
    public static void addUndoContext(UndoableInfoEdit uie) {
        if (getActiveEditedInternalFrame() != null) {
            uie.addContext(XA_Designer_Plugin.getActiveEditedInternalFrame().getUndoContext());
        } else {
            uie.addContext(getUndoContext());
        }
        XA_Designer_Plugin.getDefault().getWorkbench().getOperationSupport().getOperationHistory().add(uie);
    }

    /**
     * To Get the DefaultundoManager Undocontext.
     * 
     * @return the IUndoContext.
     */
    private static XAUndoContext getUndoContext() {
        return undoContext;
    }

    /**
     * Gets Active editor absolute path.
     * 
     * @return active editor instance.
     */
    public static String getActiveEditedInternalFrameAbsoluteFilePath() {
        XAInternalFrame frame = getActiveEditedInternalFrame();
        if (frame != null) {
            String absoluteFilepath = frame.getAbsoluteFilePath();
            return absoluteFilepath;
        }
        String xaRootPath = getXAwareRootPath();
        return xaRootPath;
    }

    /**
     * Gets Active editor File Path.
     * 
     * @return directory of active editor, empty if active editor not found.
     */
    public static File getCurrentFilePath() {
        File retVal = null;
        String start = ".";
        XAInternalFrame internalFrame = getActiveEditedInternalFrame();
        if (internalFrame != null) {
            start = internalFrame.getEditorFileDirectory();
        }
        retVal = (new File(start)).getParentFile();
        return retVal;
    }

    /**
     * Gets Active editor .
     * 
     * @return active editor instance.
     */
    public static XAInternalFrame getActiveEditedInternalFrame() {
        try {
            IEditorPart editor = getActiveEditor();
            if ((editor != null) && editor instanceof XAInternalFrame) {
                return ((XAInternalFrame) editor);
            }
        } catch (NullPointerException e) {
        }
        return null;
    }

    /**
     * Gets Active editor .
     * 
     * @return active editor instance.
     */
    public static IEditorPart getActiveEditor() {
        IEditorPart part = null;
        IWorkbenchPage obj = XA_Designer_Plugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (obj != null) {
            part = obj.getActiveEditor();
        }
        return part;
    }

    /**
     * Closes currently active editor.
     */
    public static void closeActiveEditor() {
        IEditorPart editor = getActiveEditor();
        if (editor != null) {
            XA_Designer_Plugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().closeEditor(editor, true);
        }
    }

    /**
     * Gets the List of BizReference editors
     * 
     * @return array of bizreference editors.
     */
    public static Object[] getInternalFrames() {
        try {
            IEditorReference[] workBenchPages = XA_Designer_Plugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
            List<XAInternalFrame> listOfEditors = new ArrayList<XAInternalFrame>();
            for (int i = 0; i < workBenchPages.length; i++) {
                Object obj = workBenchPages[i].getPart(false);
                if (obj instanceof XAInternalFrame) {
                    listOfEditors.add((XAInternalFrame) obj);
                }
            }
            return listOfEditors.toArray();
        } catch (Exception exception) {
            logger.severe("Error getting internal frames list:" + exception.getMessage());
        }
        return new Object[0];
    }

    /**
     * Gets Navigator View Reference.
     * 
     * @return active editor instance.
     */
    public static NavigatorView getNavigatorView() {
        IWorkbench workbench = XA_Designer_Plugin.getDefault().getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                IViewReference[] viewRefs = page.getViewReferences();
                for (int index = 0; index < viewRefs.length; index++) {
                    if ((viewRefs[index] != null) && viewRefs[index].getId().equalsIgnoreCase(GlobalConstants.ID_NAVIGATOR_VIEW)) {
                        return ((NavigatorView) viewRefs[index].getView(false));
                    }
                }
            }
        }
        return null;
    }

    /**
     * Refreshs the navigator view.
     */
    public static void refreshNavigatorView() {
        NavigatorView navigatorView = getNavigatorView();
        if (navigatorView != null) {
            navigatorView.refreshTree();
        }
    }

    /**
     * Gets the clipboard instance.
     * 
     * @return Clipboard instance.
     */
    public static Clipboard getClipboard() {
        return clipboard;
    }

    /**
     * Makes given shell to busy.
     * 
     * @param parent
     *            shell instance.
     * @param taskName
     *            task name.
     */
    public static void makeBusy(final Shell parent, String taskName) {
        Control currentFocusControl = parent.getDisplay().getFocusControl();
        parent.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_WAIT));
        busyShellStack.push(new ShellStatus(parent, currentFocusControl));
    }

    /**
     * Makes given shell to un busy.
     */
    public static void makeUnBusy() {
        Shell busyShell = null;
        Control currentFocusControl = null;
        if (!busyShellStack.isEmpty()) {
            ShellStatus status = (ShellStatus) busyShellStack.pop();
            busyShell = status.getBusyShell();
            currentFocusControl = status.getLastFocusedComponent();
        }
        if (busyShell != null) {
            if (!busyShell.isDisposed()) {
                busyShell.setCursor(null);
                busyShell.forceFocus();
                if ((currentFocusControl != null) && !currentFocusControl.isDisposed()) {
                    currentFocusControl.forceFocus();
                }
            }
        }
    }

    /**
     * Opens the given file.
     * 
     * @param fileObject
     *            absolute path of file to open.
     * 
     * @return true if successfully opened file.
     * @throws Exception
     *             If there is a problem opening or parsing the file.
     */
    public static boolean openFile(File fileObject) {
        return openFile(fileObject, true);
    }

    /**
     * Opens the given file.
     * 
     * @param fileObject
     *            absolute path of file to open.
     * @param reload
     *            true if file to be reloaded.
     * 
     * @return true if successfully opened file, false otherwise.
     * @throws Exception
     *             If there is a problem loading or parsing the file.
     */
    @SuppressWarnings("restriction")
    public static boolean openFile(File fileObject, boolean reload) {
        String fileName = fileObject.getAbsolutePath();
        if (!fileObject.isAbsolute()) {
            fileName = ResourceUtils.getAbsolutePath(fileObject.toString());
            fileObject = new File(fileName);
        }
        try {
            refreshNavigatorView();
        } catch (Exception e) {
            ControlFactory.showStackTrace("Unable to refresh Navigator:" + fileObject.toString(), e);
        }
        Object[] alreadyOpened = XA_Designer_Plugin.getInternalFrames();
        for (int i = 0; i < alreadyOpened.length; i++) {
            if (alreadyOpened[i] instanceof XAInternalFrame) {
                String absPath = ((XAInternalFrame) alreadyOpened[i]).getAbsoluteFilePath();
                if (absPath.equalsIgnoreCase(fileName)) {
                    XAInternalFrame frame = ((XAInternalFrame) alreadyOpened[i]);
                    focusInternalFrame(frame);
                    try {
                        frame.reloadFile();
                    } catch (Exception e) {
                        return false;
                    }
                    frame.getTreeHandler().expandTreeRows(UserPrefs.getTreeExpanionLevel());
                    return true;
                }
            }
        }
        IWorkbenchPage page = getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
            IEditorInput input = null;
            IFile file = getIFile(fileObject);
            if (file != null) {
                file.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
                input = new FileEditorInput(file);
            } else {
                IFileSystem fileSystem = EFS.getLocalFileSystem();
                IFileStore fileStore = fileSystem.fromLocalFile(fileObject);
                input = new JavaFileEditorInput(fileStore);
            }
            page.openEditor(input, getEditorId(fileObject), true);
            return true;
        } catch (Exception e) {
            ControlFactory.showStackTrace("Unable to open file:" + fileObject.toString(), e);
        }
        return false;
    }

    /**
     * Checks whether the given path represents a resource within the workspace. If yes, returns true otherwise returns
     * false.
     * 
     * @param fileAbsolutePath
     *            String value representing file absolute path.
     * 
     * @return Boolean value.
     */
    public static boolean isFileInWorkspace(String fileAbsolutePath) {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for (int i = 0; i < projects.length; i++) {
            String workspaceAbsolutePath = projects[i].getLocation().toOSString().toLowerCase();
            if (fileAbsolutePath.toLowerCase().replace('\\', '/').startsWith(workspaceAbsolutePath.replace('\\', '/'))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns IFile instance of give java.io.File instance.
     * 
     * @param file
     *            java.io.File instance.
     * 
     * @return IFile instance.
     */
    public static IFile getIFile(File file) {
        IFile retFile = null;
        if (isFileInWorkspace(file.getAbsolutePath())) {
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            Path path = new Path(file.getAbsolutePath());
            return root.getFileForLocation(path);
        }
        return retFile;
    }

    /**
     * Refresh the Eclipse Resource manager to register a new file created by ordinary Java I/O. This method will
     * attempt to first refresh from the folder. If that is successful then it will return immediately. If not, this
     * method will attempt to rerfresh from the project level. It will yield ten times to the VM attemtping to let the
     * refresh process to get completed
     * 
     * @param file -
     *            The Java.io.File representation of the file that was recently created.
     */
    public static void refreshResource(File file) {
        final String methodName = "refreshResource";
        IProgressMonitor monitor = new NullProgressMonitor();
        IFile workSpaceFile = getIFile(file);
        int trys = 0;
        if (workSpaceFile == null) {
            return;
        }
        IResource res = workSpaceFile.getParent();
        while (!res.exists() && res != null) {
            res = res.getParent();
        }
        if (res != null) {
            try {
                res.refreshLocal(IResource.DEPTH_INFINITE, monitor);
                Thread.yield();
            } catch (CoreException e) {
                e.printStackTrace();
                String errMsg = "Failed to refresh resource folder with local WSDL " + workSpaceFile.getFullPath();
                XADesignerLogger.logError(errMsg, e);
                logger.warning(errMsg, className, methodName);
            }
            if (workSpaceFile.exists()) {
                return;
            }
        }
        IProject proj = workSpaceFile.getProject();
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(proj.getName());
        try {
            project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
            Thread.yield();
            workSpaceFile = project.getFile(workSpaceFile.getName());
        } catch (CoreException e) {
            String errMsg = "Failed to refresh project with local WSDL " + workSpaceFile.getFullPath();
            XADesignerLogger.logError(errMsg, e);
            logger.warning(errMsg, className, methodName);
            logger.exiting(className, methodName, "WSDL " + workSpaceFile.getName() + " exists: " + workSpaceFile.exists());
            return;
        }
        while (!workSpaceFile.exists() && trys < 11) {
            trys++;
            Thread.yield();
            logger.finest("WSDL " + workSpaceFile.getName() + " exists: " + workSpaceFile.exists() + " try #" + trys, className, methodName);
        }
        logger.exiting(className, methodName, "WSDL " + workSpaceFile.getName() + " exists: " + workSpaceFile.exists());
    }

    /**
     * Focus the given internal frame.
     * 
     * @param frame
     *            internal frame to focus.
     */
    public static void focusInternalFrame(XAInternalFrame frame) {
        getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().activate(frame);
    }

    /**
     * Gets the editor id for given file object.
     * 
     * @param file
     *            file instance.
     * 
     * @return editor id in string form.
     */
    public static String getEditorId(File file) {
        IWorkbench workbench = getDefault().getWorkbench();
        IEditorRegistry editorRegistry = workbench.getEditorRegistry();
        IEditorDescriptor descriptor = editorRegistry.getDefaultEditor(file.getName());
        if (descriptor != null) {
            return descriptor.getId();
        }
        return EditorsUI.DEFAULT_TEXT_EDITOR_ID;
    }

    /**
     * Sets the status message to eclipse status bar.
     * 
     * @param message
     *            message
     */
    public static void setStatus(String message) {
        ((ApplicationWindow) getDefault().getWorkbench().getActiveWorkbenchWindow()).setStatus(message);
    }

    /**
     * Activates the given frame instance.
     * 
     * @param frameInstance
     *            XAInternalFrame instance.
     */
    public static void activeFrame(XAInternalFrame frameInstance) {
        getDefault().getWorkbench().getActiveWorkbenchWindow().setActivePage(frameInstance.getEditorSite().getPage());
    }

    /**
     * Gets the IDocument for given IEditorPart instance.
     * 
     * @param editor
     *            editor instance.
     * 
     * @return IDocument instance.
     */
    public static IDocument getIDocumentForEditor(IEditorPart editor) {
        IDocumentProvider provider = null;
        IDocument iDocument = null;
        if (editor instanceof ITextEditor) {
            provider = ((ITextEditor) editor).getDocumentProvider();
        } else if (editor instanceof IXMLEditor) {
            provider = ((ITextEditor) ((IXMLEditor) editor).getSourceEditor()).getDocumentProvider();
        }
        if (provider != null) {
            iDocument = provider.getDocument(editor.getEditorInput());
        }
        return iDocument;
    }

    /**
     * Sets boolean value indicating Import Swing XA-Designer Project operation is performed or not.
     * 
     * @param isImport
     *            boolean value.
     */
    public void setIsImport(boolean isImport) {
        this.isImport = isImport;
    }

    /**
     * Returns boolean value indicating Import Swing XA-Designer Project operation is performed or not.
     * 
     * @return boolean value.
     */
    public boolean isImport() {
        return isImport;
    }

    /**
     * Method from IWindowListener. Called when window is activated.
     * 
     * @param window
     *            Instance of IWorkbenchWindow.
     */
    @SuppressWarnings("restriction")
    public void windowActivated(IWorkbenchWindow window) {
        if (window == null) {
            return;
        }
        if (window instanceof WorkbenchWindow) {
            ControlFactory.setMenuBarManager(((WorkbenchWindow) window).getMenuManager());
            ControlFactory.setCoolBarManager(((WorkbenchWindow) window).getCoolBarManager());
            if (ControlFactory.createMenuItems()) {
                ControlFactory.createToolbarButtons();
            }
        }
        window.addPageListener(this);
        window.addPerspectiveListener(this);
        IWorkbenchPage[] pages = window.getPages();
        if (pages.length > 0) {
            for (int index = 0; index < pages.length; index++) {
                if (pages[index] != null) {
                    pages[index].addPartListener((IPartListener2) this);
                }
            }
        }
    }

    /**
     * Method from IWindowListener. Called when window is deactivated.
     * 
     * @param window
     *            Instance of IWorkbenchWindow.
     */
    public void windowDeactivated(IWorkbenchWindow window) {
    }

    /**
     * Method from IWindowListener. Called when window is closed.
     * 
     * @param window
     *            Instance of IWorkbenchWindow.
     */
    public void windowClosed(IWorkbenchWindow window) {
        if (window != null) {
            window.removePerspectiveListener(this);
            window.removePageListener(this);
            IWorkbenchPage[] pages = window.getPages();
            for (int index = 0; index < pages.length; index++) {
                pages[index].removePartListener((IPartListener2) this);
            }
            ActionController.removeInstance(window);
        }
    }

    /**
     * Method from IWindowListener. Called when window is opened.
     * 
     * @param window
     *            Instance of IWorkbenchWindow.
     */
    @SuppressWarnings("restriction")
    public void windowOpened(IWorkbenchWindow window) {
        ActionController.getInstance(window);
        if (window instanceof WorkbenchWindow) {
            ControlFactory.setMenuBarManager(((WorkbenchWindow) window).getMenuManager());
            ControlFactory.setCoolBarManager(((WorkbenchWindow) window).getCoolBarManager());
            if (ControlFactory.createMenuItems()) {
                ControlFactory.createToolbarButtons();
            }
        }
        window.addPageListener(this);
        window.addPerspectiveListener(this);
        IWorkbenchPage[] pages = window.getPages();
        if (pages.length > 0) {
            for (int index = 0; index < pages.length; index++) {
                if (pages[index] != null) {
                    pages[index].addPartListener((IPartListener2) this);
                }
            }
        }
    }

    /**
     * Method from IPageListener.
     * 
     * @param page
     *            Instance of IWorkbenchPage.
     */
    public void pageActivated(IWorkbenchPage page) {
        if (page != null) {
            page.addPartListener((IPartListener2) this);
        }
    }

    /**
     * Method from IPageListener.
     * 
     * @param page
     *            Instance of IWorkbenchPage.
     */
    public void pageClosed(IWorkbenchPage page) {
    }

    /**
     * Method from IPageListener.
     * 
     * @param page
     *            Instance of IWorkbenchPage.
     */
    public void pageOpened(IWorkbenchPage page) {
        if (page != null) {
            page.addPartListener((IPartListener2) this);
        }
    }

    /**
     * Method from IPartListener
     * 
     * @param part
     *            Instance of IWorkbenchPart.
     */
    public void partActivated(IWorkbenchPart part) {
    }

    /**
     * Method from IPartListener
     * 
     * @param part
     *            Instance of IWorkbenchPart.
     */
    public void partBroughtToTop(IWorkbenchPart part) {
        XAInternalFrame editor = null;
        if (part instanceof XAInternalFrame) {
            editor = (XAInternalFrame) part;
        }
        ActionController.getInstance().resetActions(editor);
    }

    /**
     * Method from IPartListener
     * 
     * @param part
     *            Instance of IWorkbenchPart.
     */
    public void partClosed(IWorkbenchPart part) {
        if (part instanceof LogView) {
            logSearchStr = ((LogView) part).getLogSearchStr();
        }
    }

    /**
     * Method from IPartListener
     * 
     * @param part
     *            Instance of IWorkbenchPart.
     */
    public void partDeactivated(IWorkbenchPart part) {
    }

    /**
     * Method from IPartListener
     * 
     * @param part
     *            Instance of IWorkbenchPart.
     */
    public void partOpened(IWorkbenchPart part) {
        if (part instanceof LogView) {
            ((LogView) part).setLogSearchStr(logSearchStr);
        }
    }

    /**
     * Method from IOperationHistory Listener. Called whenever object is added to history.
     * 
     * @param event
     *            Instance of OperationHistoryEvent.
     */
    public void historyNotification(OperationHistoryEvent event) {
        XAInternalFrame frame = getActiveEditedInternalFrame();
        if (frame != null) {
            if (event.getEventType() == OperationHistoryEvent.OPERATION_ADDED) {
                event.getHistory().setLimit(frame.getUndoContext(), event.getHistory().getLimit(frame.getUndoContext()) + 1);
            }
            if (frame == getActiveEditor()) {
                ActionController.getInstance().resetActions(frame);
            }
        }
    }

    /**
     * Method from IPerspectiveListener. Called when Perspective is activated.
     * 
     * @param page
     *            Instance of IWorkbenchPage.
     * @param perspective
     *            Instance of IPerspectiveDescriptor.
     */
    @SuppressWarnings("restriction")
    public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
        if (perspective.getId().equalsIgnoreCase(GlobalConstants.ID_XAWARE_PERSPECTIVE)) {
            ControlFactory.setMenuBarManager(((WorkbenchWindow) page.getWorkbenchWindow()).getMenuManager());
            ControlFactory.setCoolBarManager(((WorkbenchWindow) page.getWorkbenchWindow()).getCoolBarManager());
            ActionController.getInstance(page.getWorkbenchWindow()).showAllMenuItems();
            ActionController.getInstance().resetActions(getActiveEditedInternalFrame());
        } else {
            if (page != null) {
                IWorkbenchPart part = page.getActivePart();
                if (part != null) {
                    if (!part.getSite().getId().equals(GlobalConstants.ID_NAVIGATOR_VIEW) && !part.getSite().getId().equals(GlobalConstants.ID_LOG_VIEW) && !(part instanceof XAInternalFrame)) {
                        ControlFactory.setMenuBarManager(((WorkbenchWindow) page.getWorkbenchWindow()).getMenuManager());
                        ControlFactory.setCoolBarManager(((WorkbenchWindow) page.getWorkbenchWindow()).getCoolBarManager());
                        ActionController.getInstance(page.getWorkbenchWindow()).hideAllMenuItems();
                    }
                }
            }
        }
        ControlFactory.updateMenuAndToolBarManager();
    }

    /**
     * Method from IPerspectiveListener. Called when Perspective is changed.
     * 
     * @param page
     *            Instance of IWorkbenchPage.
     * @param perspective
     *            Instance of IPerspectiveDescriptor.
     * @param changeId
     *            String value representing perspective id.
     */
    @SuppressWarnings("restriction")
    public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
        if (page != null) {
            if (ControlFactory.getMenuBarManager() == null) {
                ControlFactory.setMenuBarManager(((WorkbenchWindow) page.getWorkbenchWindow()).getMenuManager());
            }
            if (ControlFactory.getCoolBarManager() == null) {
                ControlFactory.setCoolBarManager(((WorkbenchWindow) page.getWorkbenchWindow()).getCoolBarManager());
            }
            ActionController.getInstance().resetActions(getActiveEditedInternalFrame());
        }
    }

    /**
     * Getter method that returns Directory for specific Operating System.
     * 
     * @param dir
     *            Directory name.
     * 
     * @return directory name.
     */
    private String getOSDir(String dir) {
        String lDir = null;
        if (Platform.getOS().equals(Platform.OS_WIN32)) {
            System.out.println("OS:Windows");
            lDir = dir.substring(1);
        } else if (Platform.getOS().equals(Platform.OS_LINUX)) {
            System.out.println("OS:Linux");
            lDir = dir.substring(0);
        }
        return lDir;
    }

    /**
     * Getter method that returns BundleContext.
     * 
     * @return Instance of BundleContext.
     */
    public static BundleContext getBundleContext() {
        return bundleContext;
    }

    /**
     * Loads a new class using the plugin's static classLoader
     * 
     * @param className
     *            the name of the class to be loaded
     * 
     * @return the Class instance
     * 
     * @throws ClassNotFoundException
     */
    public static Class getClassForName(String className) throws ClassNotFoundException {
        return Class.forName(className.trim(), true, getNewDynamicClassLoader());
    }

    /**
     * Creates and returns a new ClassLoader which searches directories where dynamic classes are found.
     * 
     * @return a new XAClassLoader instance
     */
    public static XAClassLoader getNewDynamicClassLoader() {
        return XAClassLoader.getDynamicClassLoader(plugin.staticClassLoader);
    }

    /**
     * Gets active editor file object.
     * 
     * @return return the IPath to file object else null.
     */
    public static IPath getActiveEditorPath() {
        IEditorPart part = getActiveEditor();
        if (part != null) {
            IEditorInput input = part.getEditorInput();
            if (input instanceof FileEditorInput) {
                return ((FileEditorInput) input).getFile().getRawLocation();
            } else if (input instanceof IPathEditorInput) {
                return ((IPathEditorInput) input).getPath();
            }
        }
        return null;
    }

    /**
     * Returns full absolute path of given IEditorpart instance.
     * 
     * @param iEditorPart
     *            editor instance.
     * 
     * @return String absolute path.
     */
    @SuppressWarnings("restriction")
    public static String getAbsoultePathForIEditorPart(IEditorPart iEditorPart) {
        IEditorInput xaEditorInput = iEditorPart.getEditorInput();
        try {
            IPath path = null;
            String strPath = null;
            if (xaEditorInput instanceof FileEditorInput) {
                path = ((FileEditorInput) xaEditorInput).getPath();
            } else if (xaEditorInput instanceof JavaFileEditorInput) {
                path = ((JavaFileEditorInput) xaEditorInput).getPath();
            }
            if (path != null) {
                strPath = path.toOSString();
            }
            return strPath;
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * To get designer jar location.
     * 
     * @return jar location.
     * 
     * @throws IOException
     */
    @SuppressWarnings("deprecation")
    public String getDesignerLocation() throws IOException {
        URL localURL = Platform.asLocalURL(Platform.find(bundleContext.getBundle(), new Path(DESIGNER_JAR_NAME)));
        String designerLocation = localURL.getPath();
        return designerLocation;
    }

    /**
     * Determines whether atleast one project exists in the projectNavigator
     * 
     * @return true if atleast one project exists in the projectNavigator, false otherwise
     */
    public static boolean projectsExistsInNavigator() {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        if (projects.length == 0) {
            return false;
        }
        return true;
    }

    /**
     * Returns the Biz view type int biz view type as int.
     * 
     * @return element type as integer.
     * 
     * @throws WizardException
     */
    public static int getBizViewType() throws WizardException {
        return getElementType((Element) getBizViewRootElement().getJDOMContent().getContent(), null);
    }

    /**
     * Return the type of the Document.
     * 
     * @param rootElem
     *            Root element.
     * @param absolutePath
     *            absolute path.
     * 
     * @return type
     */
    public static int getElementType(Element rootElem, String absolutePath) {
        int myType;
        if (rootElem.getAttributeValue("bizcomptype", XAInternalFrame.xaNS) != null) {
            myType = XAInternalFrame.BIZ_COMP;
        } else if (rootElem.getAttributeValue("bizdrivertype", XAInternalFrame.xaNS) != null) {
            myType = XAInternalFrame.BIZ_DRIVER;
        } else if (isWSDL(absolutePath)) {
            myType = XAInternalFrame.WSDL;
        } else if (isSchema(absolutePath)) {
            myType = XAInternalFrame.SCHEMA;
        } else {
            myType = XAInternalFrame.BIZ_DOC;
        }
        return myType;
    }

    /**
     * Determines whether file is schema or not.
     * 
     * @param fileName
     *            absolute file path.
     * 
     * @return true if schema.
     */
    public static boolean isSchema(String fileName) {
        if (fileName == null) {
            return false;
        }
        if (fileName.toLowerCase().endsWith(".xsd")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Determines whether file is WSDL or not.
     * 
     * @param fileName
     *            absolute path name.
     * 
     * @return true if WSDL.
     */
    public static boolean isWSDL(String fileName) {
        if (fileName == null) {
            return false;
        }
        try {
            WSDLFactory fact = WSDLFactory.newInstance();
            WSDLReader reader = fact.newWSDLReader();
            reader.readWSDL(fileName);
        } catch (Throwable ex) {
            if (fileName.toLowerCase().indexOf(".wsdl") > -1) {
                return true;
            }
            return false;
        }
        return true;
    }

    /**
     * Returns the Biz view Root Element
     * 
     * @return XMLTreeNode root element as tree node.
     * 
     * @throws WizardException
     */
    public static XMLTreeNode getBizViewRootElement() throws WizardException {
        XAInternalFrame selFrame = XA_Designer_Plugin.getActiveEditedInternalFrame();
        if (WizardFactory.getDefaultInstance().isBizComponentStandAlone()) {
            return new XMLTreeNode((JDOMContentFactory.createJDOMContent(UserPrefs.getDefaultBizCompElement().clone())));
        } else {
            if (selFrame != null) {
                return (XMLTreeNode) selFrame.getTreeHandler().getRoot();
            } else {
                return null;
            }
        }
    }

    /**
     * Gets the selection object from the navigator view.
     * 
     * @return selection object
     */
    public static Object getNavigatorViewSelection() {
        if (XA_Designer_Plugin.getNavigatorView() != null) {
            try {
                IStructuredSelection selection = XA_Designer_Plugin.getNavigatorView().getXawareResNav().getSelection();
                if ((selection != null) && !selection.isEmpty()) {
                    return selection.getFirstElement();
                }
            } catch (Exception ex) {
                logger.finest("Exception caught while retrieving selection from navigator view :", ex);
            }
        }
        return null;
    }

    /**
     * Syntactic sugar to get the Active Workbench window.
     * @return
     */
    public static IWorkbenchWindow getActiveWorkbenchWindow() {
        return getDefault().getWorkbench().getActiveWorkbenchWindow();
    }

    public void partActivated(IWorkbenchPartReference p_partRef) {
    }

    public void partBroughtToTop(IWorkbenchPartReference p_partRef) {
        XAInternalFrame editor = null;
        String id = p_partRef.getId();
        if (id.startsWith("org.xaware.ide.plugin.editor.")) {
            editor = (XAInternalFrame) p_partRef.getPart(false);
        }
        ActionController.getInstance().resetActions(editor);
    }

    public void partClosed(IWorkbenchPartReference p_partRef) {
        IWorkbenchPart part = p_partRef.getPart(false);
        if (part instanceof LogView) {
            logSearchStr = ((LogView) part).getLogSearchStr();
        }
    }

    public void partDeactivated(IWorkbenchPartReference p_partRef) {
    }

    public void partHidden(IWorkbenchPartReference p_partRef) {
    }

    public void partInputChanged(IWorkbenchPartReference p_partRef) {
    }

    public void partOpened(IWorkbenchPartReference p_partRef) {
        IWorkbenchPart part = p_partRef.getPart(false);
        if (part instanceof LogView) {
            ((LogView) part).setLogSearchStr(logSearchStr);
        }
    }

    public void partVisible(IWorkbenchPartReference p_partRef) {
    }
}

/**
 * VO for Shell information.
 * 
 * @author T Vasu
 * @version 1.0
 */
class ShellStatus {

    /** Busy Shell instance. */
    private Shell busyShell;

    /** Last focused Component. */
    private Control lastFocusedComponent;

    /**
     * Creates a new ShellStatus object.
     * 
     * @param _busyShell
     *            busy shell instance.
     * @param _lastFocusedComponent
     *            last focused component.
     */
    public ShellStatus(Shell _busyShell, Control _lastFocusedComponent) {
        busyShell = _busyShell;
        lastFocusedComponent = _lastFocusedComponent;
    }

    /**
     * Gets the busy shell instance.
     * 
     * @return Returns the busyShell.
     */
    public Shell getBusyShell() {
        return busyShell;
    }

    /**
     * Sets the busy shell instance.
     * 
     * @param busyShell
     *            The busyShell to set.
     */
    public void setBusyShell(Shell busyShell) {
        this.busyShell = busyShell;
    }

    /**
     * Gets the last focused component.
     * 
     * @return Returns the lastFocusedComponent.
     */
    public Control getLastFocusedComponent() {
        return lastFocusedComponent;
    }

    /**
     * Sets the last focus component.
     * 
     * @param lastFocusedComponent
     *            The lastFocusedComponent to set.
     */
    public void setLastFocusedComponent(Control lastFocusedComponent) {
        this.lastFocusedComponent = lastFocusedComponent;
    }
}
