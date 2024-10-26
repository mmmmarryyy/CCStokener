package ro.wpcs.traser.controllers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import net.sf.traser.configtool.BackgroundProgress;
import net.sf.traser.databinding.base.Item;
import org.apache.log4j.Logger;
import org.gui4j.Gui4j;
import org.gui4j.Gui4jController;
import org.gui4j.Gui4jDialog;
import org.gui4j.Gui4jView;
import org.gui4j.event.SimpleEvent;
import org.gui4j.exception.Gui4jExceptionHandler;
import ro.wpcs.traser.client.Startup;
import ro.wpcs.traser.client.storage.impl.FTPAdapter;
import ro.wpcs.traser.client.ws.impl.DTAdapter;
import ro.wpcs.traser.client.ws.impl.WSInit;
import ro.wpcs.traser.logging.LoggingAdapter;
import ro.wpcs.traser.mail.MailAdapter;
import ro.wpcs.traser.model.File;
import ro.wpcs.traser.model.FileConstants;
import ro.wpcs.traser.model.MsgTemplateConstants;
import ro.wpcs.traser.model.Node;
import ro.wpcs.traser.model.Project;
import ro.wpcs.traser.model.UserConstants;
import ro.wpcs.traser.model.Version;

/**
 * This is a singleton controller for the menu in the main application window
 * 
 * @author Alina, Tomita Militaru
 * @date Oct 2, 2008
 */
public final class MenuController implements Gui4jController {

    /** Class logger */
    public static final Logger logger = Logger.getLogger(MenuController.class);

    /** Tells if the current user is the admin **/
    public SimpleEvent adminLogged = new SimpleEvent();

    /** Event used to enable or disable menu items */
    public SimpleEvent eAllowed = new SimpleEvent();

    /** The window variable which reacts to the menu's commands */
    private static Gui4jView gui4jView;

    /** The gui4j variable needed in MenuController class */
    private static Gui4j gui4j;

    /** Single instance of this class */
    private static MenuController singleton;

    /** Single instance of TreeController class */
    private static TreeController treeController = TreeController.getInstance();

    /** The properties for the menu */
    private static Properties prop = new Properties();

    /** The URL for the menu properties file */
    private static final URL url = ClassLoader.getSystemResource("menu.properties");

    /** */
    private static final String URL_SITE = "help.url";

    /** */
    private static final String LICENSE_FILE = "LICENCE.txt";

    /** The node copied */
    private static Node itemCopied = null;

    /** The cut node */
    private static Node itemCut = null;

    /** The XML file for the new Project dialog layout */
    private static final String newPrjXml = "xml/NewProject.xml";

    /** The URL for the properties file for the new project dialog */
    private static final URL urlNewPrj = ClassLoader.getSystemResource("newProject.properties");

    /** The XML file for the new File dialog layout */
    private static final String newFileXml = "xml/NewFile.xml";

    /** The URL for the properties file for the new file dialog */
    private static final URL urlNewFile = ClassLoader.getSystemResource("newFile.properties");

    /** Variable used for the FTP operations */
    private static FTPAdapter ftpAdapter = new FTPAdapter();

    /** Background process indicator */
    private BackgroundProgress progress;

    /** The DTAdapter to communicate with */
    private DTAdapter dtAdapter = new DTAdapter();

    /** Account controller **/
    private static AccountController accountController = new AccountController();

    /** History controller **/
    private HistoryController historyController = new HistoryController();

    /** User id to mark which user is logged */
    private int loggedUserFlag;

    /** Flag for enable delete project */
    private boolean prjDelAllowed = false;

    /** Flag for enable delete file */
    private boolean fileDelAllowed = false;

    /** Flag for enable delete version */
    private boolean versDelAllowed = false;

    /** Flag for enable check in */
    private boolean checkinAllowed = false;

    /** Flag for enable checkout parallel */
    private boolean checkoutParallelAllowed = true;

    /** Flag for enable checkout serial */
    private boolean checkoutSerialAllowed = false;

    /** Flag for enable unde checkout */
    private boolean undoCheckoutAllowed = false;

    /** Flag for enable refresh local file */
    private boolean refreshLocalAllowed = false;

    /** The current user name */
    private String currentUserName = LoginController.getLoggedUser().getName();

    private AboutUsController about = new AboutUsController();

    /**
	 * A private Constructor prevents any other class from instantiating. It
	 * also associates menu.properties file
	 */
    private MenuController() {
        super();
        try {
            prop.load(url.openStream());
        } catch (FileNotFoundException e) {
            logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        }
        loggedUserFlag = Integer.valueOf(LoginController.getLoggedUser().getFlag());
        if (loggedUserFlag == UserConstants.ADMIN_FLAG) {
            prjDelAllowed = true;
            fileDelAllowed = true;
            versDelAllowed = true;
        }
    }

    /**
	 * @return true if the current logged user is admin, or false otherwise
	 */
    public boolean isAdminLogged() {
        if (LoginController.isAdminLogged()) {
            adminLogged.fireEvent();
            logger.info("Have fired 'adminLogged' event");
            return true;
        } else return false;
    }

    /**
	 * @return true if current logged user is regular,or false otherwise
	 */
    public boolean isRegularLogged() {
        return LoginController.isRegularLogged();
    }

    /** Static 'instance' method */
    public static synchronized MenuController getInstance() {
        if (singleton == null) {
            singleton = new MenuController();
        }
        return singleton;
    }

    /**
	 * Sets if the check in operation is enabled or disabled
	 * 
	 * @param value
	 *            true for enabled or false for disabled
	 */
    public void setCheckinAllowed(boolean value) {
        checkinAllowed = value;
        eAllowed.fireEvent();
    }

    /**
	 * Sets if the checkout operation paralel is enabled or disabled
	 * 
	 * @param value
	 *            true for enabled or false for disabled
	 */
    public void setCheckoutParallelAllowed(boolean value) {
        checkoutParallelAllowed = value;
        eAllowed.fireEvent();
    }

    /**
	 * Sets if the checkout serial operation is enabled or disabled
	 * 
	 * @param value
	 *            true for enabled or false for disabled
	 */
    public void setCheckoutSerialAllowed(boolean value) {
        checkoutSerialAllowed = value;
        eAllowed.fireEvent();
    }

    /**
	 * Sets if the undo checkout operation is enabled or disabled
	 * 
	 * @param value
	 *            true for enabled or false for disabled
	 */
    public void setUndoCheckoutAllowed(boolean value) {
        undoCheckoutAllowed = value;
        eAllowed.fireEvent();
    }

    /**
	 * Sets if the refresh local file operation is enabled or disabled
	 * 
	 * @param value
	 *            true for enabled or false for disabled
	 */
    public void setRefreshLocalAllowed(boolean value) {
        refreshLocalAllowed = value;
        eAllowed.fireEvent();
    }

    /**
	 * Sets if project deletion is allowed or not
	 * 
	 * @param value
	 *            true or false
	 */
    public void setPrjDelAllowed(boolean value) {
        prjDelAllowed = value;
        eAllowed.fireEvent();
    }

    /**
	 * Sets if file deletion is allowed or not
	 * 
	 * @param value
	 *            true or false
	 */
    public void setFileDelAllowed(boolean value) {
        fileDelAllowed = value;
        eAllowed.fireEvent();
    }

    /**
	 * Sets if version deletion is allowed or not
	 * 
	 * @param value
	 *            true or false
	 */
    public void setVersDelAllowed(boolean value) {
        versDelAllowed = value;
        eAllowed.fireEvent();
    }

    public void showSettings() {
        Startup.createSettingsDialog();
    }

    public String getSettingsText() {
        return prop.getProperty("view.settings");
    }

    /**
	 * @param The
	 *            Gui4j parameter to work on
	 */
    public void setGui4j(final Gui4j gui4j) {
        this.gui4j = gui4j;
    }

    /**
	 * Closes the main application window
	 */
    public void close() {
        GUIMainController.getInstance().onWindowClosing();
    }

    public boolean getSettingsEnabled() {
        return false;
    }

    /**
	 * @return The text on File menu
	 */
    public String getFileText() {
        return (String) prop.get("file");
    }

    /**
	 * @return The text on File->New menu
	 */
    public String getFileNewText() {
        return (String) prop.get("file.new");
    }

    /**
	 * @return the text written on File->New->Project menu item
	 */
    public String getSubPrjText() {
        return (String) prop.get("file.new.project");
    }

    /**
	 * @return the text written on File->New->Version menu item
	 */
    public String getSubVersText() {
        return (String) prop.get("file.new.version");
    }

    /**
	 * @return the text written on File->New->File menu item
	 */
    public String getSubFileText() {
        return (String) prop.get("file.new.file");
    }

    /**
	 * @return the text written on File->Open menu item
	 */
    public String getOpenText() {
        return (String) prop.get("file.open");
    }

    /**
	 * @return the text written on File->Refresh local copy menu item
	 */
    public String getRefreshLocalText() {
        return (String) prop.get("file.refreshLocal");
    }

    /**
	 * @return the text written on File->Check out serial menu item
	 */
    public String getCheckoutParallelText() {
        return (String) prop.get("file.checkoutParallel");
    }

    /**
	 * @return the text written on File->Check out serial menu item
	 */
    public String getCheckoutSerialText() {
        return (String) prop.get("file.checkoutSerial");
    }

    /**
	 * @return the text written on File->Undo check out menu item
	 */
    public String getUndoCheckText() {
        return (String) prop.get("file.undo");
    }

    /**
	 * @return the text written on File->Check in menu item
	 */
    public String getCheckInText() {
        return (String) prop.get("file.checkin");
    }

    /**
	 * @return the text written on File->Remove menu
	 */
    public String getRemoveText() {
        return (String) prop.get("file.remove");
    }

    /**
	 * @return the text written on File->Histry menu
	 */
    public String getHistoryText() {
        return (String) prop.get("file.history");
    }

    /**
	 * @return the text written on File->Exit menu item
	 */
    public String getExitText() {
        return (String) prop.get("file.exit");
    }

    /**
	 * @return the text on Security menu
	 */
    public String getSecurityText() {
        return (String) prop.getProperty("file.security");
    }

    /**
	 * @return the text written on Edit menu
	 */
    public String getEditText() {
        return (String) prop.get("edit");
    }

    /**
	 * @return the text written on Edit->Copy menu item
	 */
    public String getEditCopyText() {
        return (String) prop.get("edit.copy");
    }

    /**
	 * @return the text written on Edit->Cut menu item
	 */
    public String getEditCutText() {
        return (String) prop.get("edit.cut");
    }

    /**
	 * @return the text written on Edit->Paste menu item
	 */
    public String getEditPasteText() {
        return (String) prop.get("edit.paste");
    }

    /**
	 * @return the text written on Edit->Select all menu item
	 */
    public String getEditSelAllText() {
        return (String) prop.get("edit.selectAll");
    }

    /**
	 * @return the text written on View menu
	 */
    public String getViewText() {
        return (String) prop.get("view");
    }

    /**
	 * @return the text written on View->Refresh menu item
	 */
    public String getViewRefreshText() {
        return (String) prop.get("view.refresh");
    }

    /**
	 * @return the text written on Admin menu
	 */
    public String getAdminText() {
        return (String) prop.getProperty("admin");
    }

    /**
	 * @return the text written on Help menu
	 */
    public String getHelpText() {
        return (String) prop.get("help");
    }

    /**
	 * @return the text written on Help->About US menu item
	 */
    public String getAboutUsText() {
        return (String) prop.get("help.aboutUs");
    }

    /**
	 * 
	 * @return
	 */
    public String getHelpContentsText() {
        return (String) prop.get("help.contents");
    }

    /**
	 * 
	 * @return
	 */
    public String getLicenseText() {
        return (String) prop.get("help.license");
    }

    /**
	 * opens a new dialog window for creating a new project and assigns a
	 * controller to it
	 */
    public void openNewPrjDialog() {
        Properties propNewPrj = setNewPrjProperties();
        final String title = (String) propNewPrj.get("title");
        final int width = Integer.valueOf((String) propNewPrj.get("width"));
        final int height = Integer.valueOf((String) propNewPrj.get("height"));
        final boolean center = Boolean.valueOf((String) propNewPrj.get("center"));
        if (treeController.isRootSelected()) {
            Gui4jDialog dialog = gui4j.createDialog(gui4jView, newPrjXml, getNewProjectController(), title, false);
            dialog.setWindowSize(width, height);
            dialog.prepare();
            dialog.center(center);
            getNewProjectController().setDialog(dialog);
            dialog.show();
        } else {
            if (treeController.getSelectedPrj() != null) {
                JOptionPane.showMessageDialog(null, "You have selected a project, nested projects aren't allowed.\nPlease select a server");
            } else if (treeController.getSelectedFile() != null) {
                JOptionPane.showMessageDialog(null, "You have selected a file.\nIn order to create a new project, please first select a server.");
            } else if (treeController.getSelectedVers() != null) {
                JOptionPane.showMessageDialog(null, "You have selected a version.\nIn order to create a new project, please first select a server.");
            }
        }
    }

    /**
	 * Sets the properties for the new project dialog
	 * 
	 * @return the properties variable associated with the new project dialog
	 */
    public Properties setNewPrjProperties() {
        Properties propNewPrj = new Properties();
        try {
            propNewPrj.load(urlNewPrj.openStream());
        } catch (IOException e) {
            logger.error(e.getStackTrace());
        }
        return propNewPrj;
    }

    /**
	 * opens a new dialog window for creating a new file and assigns a
	 * controller to it
	 */
    public void openNewFileDialog() {
        Properties propNewFile = setNewFileProperties();
        final String title = (String) propNewFile.get("title");
        final int width = Integer.valueOf((String) propNewFile.get("width"));
        final int height = Integer.valueOf((String) propNewFile.get("height"));
        final boolean center = Boolean.valueOf((String) propNewFile.get("center"));
        if (treeController.getSelectedPrj() != null) {
            Gui4jDialog dialog = gui4j.createDialog(gui4jView, newFileXml, getNewFileController(), title, false);
            dialog.setWindowSize(width, height);
            dialog.prepare();
            dialog.center(center);
            getNewFileController().setDialog(dialog);
            dialog.show();
        } else {
            JOptionPane.showMessageDialog(null, "Please select a project");
        }
    }

    /**
	 * Sets the properties for the new file dialog
	 * 
	 * @return the properties variable associated with the new file dialog
	 */
    public Properties setNewFileProperties() {
        Properties propNewFile = new Properties();
        try {
            propNewFile.load(urlNewFile.openStream());
        } catch (IOException e) {
            logger.error(e.getStackTrace());
        }
        return propNewFile;
    }

    /**
	 * opens a dialog when creating a new version for a file
	 */
    public static void openNewVersionDialog() {
        if (treeController.getSelectedFile() != null) {
            Gui4jDialog dialog = gui4j.createDialog(gui4jView, "xml/NewVersion.xml", getNewVersionController(), "Insert comments for the new version", false);
            dialog.setWindowSize(500, 200);
            dialog.prepare();
            dialog.center(true);
            getNewVersionController().setDialog(dialog);
            dialog.show();
        } else JOptionPane.showMessageDialog(null, "Please select a file");
    }

    public boolean isPrjDelAllowed() {
        return prjDelAllowed;
    }

    /**
	 * Prompts the user if it's sure about the project deletion Calls removePrj
	 * in TreeController
	 */
    public void deletePrj() {
        boolean deleteIt;
        deleteIt = areYouSure();
        if (deleteIt) {
            Project selectedProject = treeController.getSelectedPrj();
            boolean allowed = selectedProject.getProjectBO().deleteProject();
            if (allowed) {
                treeController.removePrj(selectedProject);
                MailAdapter.notifyUser(MsgTemplateConstants.PRJ_DEL_TMPL, selectedProject.getNodeText(), new Date(), LoginController.getLoggedUserSignature());
            }
        }
    }

    /**
	 * Verifies if a project was selected and displays 'are you sure' message
	 * box
	 */
    public boolean areYouSure() {
        boolean valid = false;
        if (treeController.getSelectedPrj() == null) {
            JOptionPane.showMessageDialog(null, "Please select a project");
        } else {
            final int response = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete the selected project?", "Delete project", JOptionPane.YES_NO_OPTION);
            if (response == 0) {
                valid = true;
            }
        }
        return valid;
    }

    public boolean isVersDelAllowed() {
        return versDelAllowed;
    }

    /**
	 * Calls deleteVers in treeController and deletes the version from FS
	 */
    public void deleteVersion() {
        boolean deleteIt;
        deleteIt = checkDeleteVers();
        if (deleteIt) {
            File parent = (File) treeController.getSelectedVers().getParent();
            Item parentItem = parent.getFileBO().getWSItem();
            Object[] values = new Object[] { currentUserName, treeController.getSelectedVers().getNodeText() };
            LoggingAdapter.logAction(parentItem, "msg.remove.version.attempt", values);
            Version selectedVersion = treeController.getSelectedVers();
            boolean wsSucces = selectedVersion.getVersionBO().deleteVersion();
            boolean ftpSucces = ftpAdapter.removeVersion(selectedVersion);
            if (!wsSucces || !ftpSucces) {
                LoggingAdapter.logAction(parentItem, "msg.remove.version.failed", values);
            }
            File fileParent = (File) selectedVersion.getParent();
            Project prjParent = (Project) fileParent.getParent();
            Object[] placeholders = new Object[] { selectedVersion.getNodeText(), fileParent.getNodeText(), prjParent.getNodeText(), new Date(), LoginController.getLoggedUserSignature() };
            MailAdapter.notifyUser(MsgTemplateConstants.VERS_DEL_TMPL, placeholders);
            treeController.removeVers(selectedVersion);
            LoggingAdapter.logAction(parentItem, "msg.remove.version", values);
            LoggingAdapter.logAction(WSInit.getHistoryItem(), "msg.remove.version", values);
        }
    }

    /**
	 * Verifies if a version was selected and displays 'are you sure' message
	 * box
	 */
    public boolean checkDeleteVers() {
        boolean valid = false;
        if (treeController.getSelectedVers() == null) {
            JOptionPane.showMessageDialog(null, "Please select a file version");
        } else {
            final int response = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete the selected version?", "Delete version", JOptionPane.YES_NO_OPTION);
            if (response == 0) {
                valid = true;
            }
        }
        return valid;
    }

    /**
	 * Check is file deletion is allowed
	 * 
	 * @return
	 */
    public boolean isFileDelAllowed() {
        return fileDelAllowed;
    }

    /**
	 * Calls removeFile in TreeController and deletes the file from the FTP
	 */
    public void deleteFile() {
        boolean deleteIt;
        File selectedFile = treeController.getSelectedFile();
        if (selectedFile == null) {
            JOptionPane.showMessageDialog(null, "Please select a file.");
            return;
        }
        deleteIt = checkDeleteFile();
        if (deleteIt) {
            Project parent = (Project) selectedFile.getParent();
            Item parentItem = parent.getProjectBO().getWSItem();
            Object[] values = new Object[] { currentUserName, selectedFile.getNodeText() };
            LoggingAdapter.logAction(parentItem, "msg.remove.file.attempt", values);
            boolean wsSucces = selectedFile.getFileBO().deleteFile();
            boolean ftpSucces = ftpAdapter.removeFile(selectedFile);
            if (!wsSucces || !ftpSucces) {
                values = new Object[] { selectedFile.getNodeText() };
                LoggingAdapter.logAction(parentItem, "msg.remove.file.failed", values);
            }
            Object[] placeholders = new Object[] { selectedFile.getNodeText(), selectedFile.getParent().getNodeText(), new Date(), LoginController.getLoggedUserSignature() };
            MailAdapter.notifyUser(MsgTemplateConstants.FILE_DEL_TMPL, placeholders);
            treeController.removeFile(selectedFile);
            LoggingAdapter.logAction(parentItem, "msg.remove.file", values);
            LoggingAdapter.logAction(WSInit.getHistoryItem(), "msg.remove.file", values);
        }
    }

    /**
	 * Verifies if a file was selected and displays 'are you sure' message box
	 */
    public boolean checkDeleteFile() {
        boolean valid = false;
        if (treeController.getSelectedFile() == null) {
            JOptionPane.showMessageDialog(null, "Please select a file");
        } else {
            final int response = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete the selected file?", "Delete file", JOptionPane.YES_NO_OPTION);
            if (response == 0) {
                valid = true;
            }
        }
        return valid;
    }

    /**
	 * Check is project deletion is allowed
	 * 
	 * @return
	 */
    public boolean isDelAllowed() {
        return prjDelAllowed;
    }

    /**
	 * This function clones a copied entity
	 */
    public void copy() {
        List<File> childClones = new ArrayList<File>();
        List<Version> versionClones = new ArrayList<Version>();
        if ((treeController.getSelectedFile() == null) && (treeController.getSelectedPrj()) == null && (treeController.getSelectedVers() == null)) {
            JOptionPane.showMessageDialog(null, "Only projects, files and versions can be copied");
        } else if ((treeController.getSelectedFile() != null)) {
            itemCopied = treeController.getSelectedFile().duplicate();
        } else if ((treeController.getSelectedPrj() != null)) {
            itemCopied = treeController.getSelectedPrj().duplicate();
            if (itemCopied.hasChildren()) {
                for (int i = 0; i < itemCopied.getChildren().size(); i++) {
                    if (itemCopied.getChildAt(i).hasChildren()) {
                        File fileChild = (File) itemCopied.getChildAt(i).duplicate();
                        for (int j = 0; j < itemCopied.getChildAt(i).getChildren().size(); j++) {
                            Version version = (Version) fileChild.getChildAt(j).duplicate();
                            version.setParent(fileChild);
                            versionClones.add(version);
                        }
                        fileChild.setChildren(versionClones);
                        fileChild.setParent(itemCopied);
                        childClones.add(fileChild);
                    } else {
                        File fileChild = (File) itemCopied.getChildAt(i).duplicate();
                        fileChild.setParent(itemCopied);
                        childClones.add(fileChild);
                    }
                }
                itemCopied.setChildren(childClones);
            }
        }
    }

    /**
	 * This function cuts a node from the tree
	 */
    public void cut() {
        if ((treeController.getSelectedFile() == null) && (treeController.getSelectedPrj()) == null) {
            JOptionPane.showMessageDialog(null, "Only projects or files can be cut");
        } else if ((treeController.getSelectedFile() != null)) {
            itemCut = treeController.getSelectedFile();
            treeController.moveFile((File) itemCut);
        } else if ((treeController.getSelectedPrj() != null)) {
            itemCut = treeController.getSelectedPrj();
            treeController.movePrj(treeController.getSelectedPrj());
            itemCut.setParent(treeController.getRoot());
        }
    }

    /**
	 * This function verifies if paste location is valid and calls paste in
	 * treeController for a visual representation
	 */
    public void paste() {
        if (itemCut != null) {
            itemCopied = itemCut;
        }
        if (itemCopied == null) {
            JOptionPane.showMessageDialog(null, "Nothing was copied");
        } else if ((itemCopied instanceof ro.wpcs.traser.model.File) && (treeController.getSelectedPrj() != null)) {
            if (treeController.getSelectedPrj() == itemCopied.getParent()) {
                logger.info("The file already exists here");
                itemCopied = null;
                itemCut = null;
                return;
            }
            treeController.paste(itemCopied);
            itemCopied = null;
        } else if ((itemCopied instanceof ro.wpcs.traser.model.Project) && (treeController.isRootSelected())) {
            if (treeController.getRoot() == itemCopied.getParent()) {
                logger.info("The project already exists here");
                itemCopied = null;
                itemCut = null;
                return;
            }
            treeController.paste(itemCopied);
            itemCopied = null;
            itemCut = null;
        } else {
            JOptionPane.showMessageDialog(null, "You are not allowed to paste this item here");
        }
    }

    /** Opens a selected file */
    public void open() {
        if (treeController.getSelectedPrj() != null) {
            treeController.expandPrj(treeController.getSelectedPrj());
        } else if (treeController.getSelectedFile() != null) {
            treeController.getSelectedFile().getFileBO().open();
        } else if (treeController.getSelectedVers() != null) {
            treeController.getSelectedVers().getVersionBO().open();
        } else {
            JOptionPane.showMessageDialog(null, "You have selected a server.\n Only projects, files and versions can be opened.");
        }
    }

    /**
	 * Verifies the conditions for uploading a file on FTP
	 */
    public void checkin() {
        File file = treeController.getSelectedFile();
        if (file == null) {
            JOptionPane.showMessageDialog(null, "You can only check in files!");
        } else {
            file.getFileBO().checkIn();
        }
    }

    public boolean isCheckinAllowed() {
        return checkinAllowed;
    }

    /**
	 * verifies if a file was selected for checkout and if so, calls the
	 * checkout logic for specific logged user
	 */
    public void checkOutParallel() {
        File file = treeController.getSelectedFile();
        if (file == null) {
            JOptionPane.showMessageDialog(null, "You can only check out a file. Please select a file.");
            return;
        }
        file.getFileBO().checkOutParallel();
    }

    /**
	 * 
	 */
    public void checkOutSerial() {
        File file = treeController.getSelectedFile();
        if (file == null) {
            JOptionPane.showMessageDialog(null, "You can only check out a file. Please select a file.");
            return;
        }
        file.getFileBO().checkOutSerial();
    }

    /**
	 * @return
	 */
    public boolean isCheckoutParallelAllowed() {
        return checkoutParallelAllowed;
    }

    /**
	 * @return
	 */
    public boolean isCheckoutSerialAllowed() {
        return checkoutSerialAllowed;
    }

    public boolean isUndoCheckoutAllowed() {
        return undoCheckoutAllowed;
    }

    /** Deletes the local copy and sets status back to unlocked */
    public void undoCheckOut() {
        if (treeController.getSelectedFile() != null) {
            treeController.getSelectedFile().getFileBO().undoCheckOut();
        } else {
            JOptionPane.showMessageDialog(null, "Please select a file that was checked out");
        }
    }

    /**
	 * Refreshes the local copy for a chosen file
	 * 
	 * @param f
	 *            the file to be refreshed on local machine
	 */
    public void refreshLocalCopy() {
        if (treeController.getSelectedFile() != null) {
            treeController.getSelectedFile().getFileBO().refreshLocalCopy();
        } else {
            JOptionPane.showMessageDialog(null, "Please select a file to be refreshed");
        }
    }

    public boolean isRefreshLocalAllowed() {
        return refreshLocalAllowed;
    }

    /**
	 * Refreshes the tree
	 */
    public void refreshTree() {
        progress = new BackgroundProgress(new JDialog(), "Please wait...", "Please wait while populating the tree...");
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

            private RuntimeException fault;

            @Override
            protected Void doInBackground() throws Exception {
                treeController.populateTree();
                return null;
            }

            @Override
            protected void done() {
                if (fault != null) {
                    JOptionPane.showMessageDialog(null, fault.getMessage());
                } else {
                }
                progress.closeDialog();
            }
        };
        worker.execute();
        progress.openDialog();
    }

    /**
	 * New Account dialog
	 */
    public void showCreateAccountView() {
        accountController.showCreateAccountView();
    }

    /**
	 * Modify Account dialog
	 */
    public void showModifyAccountView() {
        accountController.showModifyCurrentAccountView();
    }

    /**
	 * Manage Account dialog
	 */
    public void showManageAccountsView() {
        accountController.showManageAccountsView();
    }

    /**
	 * Shows the add/remove editors/collaborators view if the current user is
	 * allowed to
	 */
    public void showManagePartners() {
        File selectedFile = treeController.getSelectedFile();
        if (selectedFile == null) {
            JOptionPane.showMessageDialog(null, "Partners can be added only to files.\nPlease select a file first.");
            return;
        }
        Long ownerId = Long.valueOf(dtAdapter.getPropertyValue(selectedFile.getFileBO().getWSItem(), FileConstants.OWNER));
        boolean adminLogged = dtAdapter.getPropertyValue(LoginController.getLoggedWSUserItem(), UserConstants.FLAG).matches(String.valueOf(UserConstants.ADMIN_FLAG));
        if ((ownerId == LoginController.getLoggedUser().getId()) || adminLogged) {
            accountController.showManagePartners();
        } else {
            JOptionPane.showMessageDialog(null, "You do not have sufficient rights for this file");
        }
    }

    /**
	 * Shows the history view for an item in the tree
	 */
    public void showHistoryView() {
        if (treeController.getInstance().getSelectedVers() != null) {
            JOptionPane.showMessageDialog(null, "Only projects or files have history");
        } else {
            historyController.createHistoryView();
        }
    }

    public void showAboutUsView() {
        about.showAboutUs();
    }

    public void showHelpContentsView() {
        launchWebsite();
    }

    public void showLicenseView() {
        String urlPath = System.getProperty("user.dir") + java.io.File.separator + LICENSE_FILE;
        java.io.File temp = new java.io.File(urlPath);
        FTPAdapter.openWithSpecificApp(temp);
    }

    /**
	 * @return an instance of NewProjectController
	 */
    public NewProjectController getNewProjectController() {
        return NewProjectController.getInstance();
    }

    /**
	 * @return an instance of NewFileController
	 */
    public NewFileController getNewFileController() {
        return NewFileController.getInstance();
    }

    /**
	 * @return an instance of NewVersionController
	 */
    public static NewVersionController getNewVersionController() {
        return NewVersionController.getInstance();
    }

    public static AccountController getAccountController() {
        return accountController;
    }

    @Override
    public boolean onWindowClosing() {
        return false;
    }

    @Override
    public void windowClosed() {
    }

    @Override
    public Gui4jExceptionHandler getExceptionHandler() {
        return null;
    }

    @Override
    public Gui4j getGui4j() {
        return null;
    }

    /**
	 * 
	 */
    public void launchWebsite() {
        String urlPath = System.getProperty("user.dir") + java.io.File.separator + URL_SITE;
        java.io.File temp = new java.io.File(urlPath);
        FTPAdapter.openWithSpecificApp(temp);
    }
}
