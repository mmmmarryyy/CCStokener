package uk.co.whisperingwind.vienna;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.text.Keymap;
import uk.co.whisperingwind.framework.Controller;
import uk.co.whisperingwind.framework.Dialogs;
import uk.co.whisperingwind.framework.ExceptionDialog;
import uk.co.whisperingwind.framework.ModelEvent;
import uk.co.whisperingwind.framework.SqlFileFilter;
import uk.co.whisperingwind.framework.ViewEvent;

class MainController extends Controller {

    private MainView mainView = null;

    private int untitledCount = 1;

    private ConfigModel configModel = null;

    private ConnectionModel connectionModel = new ConnectionModel();

    private JFileChooser fileChooser = new JFileChooser();

    private String selectedConnection = null;

    private OpenList openList = new OpenList();

    public static void main(String args[]) {
        ViennaSplash splash = new ViennaSplash();
        boolean openedFile = false;
        MainController controller = new MainController();
        for (int i = 0; i < args.length; i++) {
            File theFile = new File(args[i]);
            if (theFile.exists()) {
                controller.openTab(theFile);
                openedFile = true;
            }
        }
        if (!openedFile) controller.newUntitled();
        controller.pack();
        splash.dispose();
    }

    private MainController() {
        try {
            configModel = new ConfigModel();
            configModel.addObserver(this);
            mainView = new MainView(configModel);
            mainView.addObserver(this);
            connectionModel.addObserver(this);
            enableGUI();
            fileChooser.addChoosableFileFilter(new SqlFileFilter());
        } catch (FileNotFoundException ex) {
            Dialogs.showError("vienna.xml not found", "Please install vienna.xml in your home directory.");
            System.exit(0);
        } catch (ConfigException ex) {
            Dialogs.showError("Error in vienna.xml", ex.getMessage());
            System.exit(0);
        }
    }

    private void pack() {
        mainView.pack();
    }

    public void modelEvent(ModelEvent event) {
        Object initiator = event.getInitiator();
        if (initiator == connectionModel) handleConnectionEvent(event); else if (initiator == configModel) handleConfigEvent(event);
    }

    private void handleConnectionEvent(ModelEvent event) {
        String action = event.getField();
        if (action.equals("connect")) {
            if (connectionModel.isConnected()) {
                openList.setStatus("Connected to " + connectionModel.getUrl());
            } else {
                openList.setStatus("Connection failed");
                mainView.setSelectedConnection(null);
            }
        }
        enableGUI();
    }

    private void handleConfigEvent(ModelEvent event) {
        String field = event.getField();
        if (field.equals("update")) {
            String value = (String) event.getValue();
            if (value.equals("begin")) {
                selectedConnection = mainView.getSelectedConnection();
                mainView.deleteObserver(this);
            } else if (value.equals("end")) {
                if (!mainView.setSelectedConnection(selectedConnection)) connectionModel.disconnect();
                selectedConnection = null;
                mainView.addObserver(this);
            }
        }
    }

    public void viewEvent(ViewEvent event) {
        Object initiator = event.getInitiator();
        try {
            if (initiator instanceof MainView) handleMainEvent(event); else if (initiator instanceof QueryController) handleQueryEvent(event); else if (initiator instanceof LoginView) handleLoginEvent(event);
        } catch (Exception ex) {
            new ExceptionDialog(ex);
        }
        enableGUI();
    }

    private void handleMainEvent(ViewEvent event) {
        String action = event.getArg1();
        String arg = (String) event.getArg2();
        if (action.equals("connect")) connectDatabase(arg); else if (action.equals("changeTab")) changeTab(); else if (action.equals("new")) newUntitled(); else if (action.equals("open")) openFile(); else if (action.equals("save")) saveFile(); else if (action.equals("saveAs")) saveFileAs(); else if (action.equals("saveAll")) saveFileAll(); else if (action.equals("close")) closeFile(); else if (action.equals("closeAll")) closeFileAll(); else if (action.equals("config")) configure(); else if (action.equals("execute")) execute(); else if (action.equals("cancel")) cancel(); else if (action.equals("commit")) connectionModel.commit(); else if (action.equals("rollback")) connectionModel.rollback(); else if (action.equals("schema")) selectSchema(); else if (action.equals("export")) exportResult(); else if (action.equals("about")) showAbout(); else if (action.equals("cut")) editAction(action); else if (action.equals("copy")) editAction(action); else if (action.equals("paste")) editAction(action); else if (action.equals("clear")) editAction(action); else if (action.equals("popup")) enableGUI(); else if (action.equals("quit")) if (confirmAll()) System.exit(0);
    }

    private void handleQueryEvent(ViewEvent event) {
        String action = event.getArg1();
        if (action.equals("updated")) connectionModel.setUpdated(); else if (action.equals("edited")) setTitle();
    }

    private void handleLoginEvent(ViewEvent event) {
        String action = event.getArg1();
        LoginView loginView = (LoginView) event.getInitiator();
        if (action.equals("ok")) {
            String driverClass = loginView.getDriverClass();
            String url = loginView.getUrl();
            String userName = loginView.getUserName();
            String password = loginView.getPassword();
            loginView.closeDialog();
            openConnection(driverClass, url, userName, password);
        } else if (action.equals("cancel")) {
            loginView.closeDialog();
        }
    }

    private void connectDatabase(String configName) {
        if (configModel.getSavePasswords()) {
            connectionModel.disconnect();
            openList.setStatus("Disconnected");
            openConnection(configName);
        } else {
            connectionModel.disconnect();
            openList.setStatus("Disconnected");
            String url = configModel.getConnectionUrl(configName);
            if (url != null) {
                LoginView loginView = new LoginView(configModel, configName);
                loginView.addObserver(this);
            }
        }
    }

    private void openConnection(String configName) {
        String url = configModel.getConnectionUrl(configName);
        if (url != null) {
            String userName = configModel.getConnectionUserName(configName);
            String password = configModel.getConnectionPassword(configName);
            String driverClass = configModel.getConnectionDriverClass(configName);
            connectionModel.setDriverClass(driverClass);
            connectionModel.setUrl(url);
            connectionModel.setUserName(userName);
            connectionModel.setPassword(password);
            openList.setStatus("Connecting to " + url);
            connectionModel.connect();
        }
    }

    private void openConnection(String driverClass, String url, String userName, String password) {
        connectionModel.setDriverClass(driverClass);
        connectionModel.setUrl(url);
        connectionModel.setUserName(userName);
        connectionModel.setPassword(password);
        openList.setStatus("Connecting to " + url);
        connectionModel.connect();
    }

    private void changeTab() {
        setTitle();
        QueryController controller = openList.getActiveController();
        if (controller != null) controller.focusText();
        enableGUI();
    }

    private void newUntitled() {
        Integer sequence = new Integer(untitledCount++);
        String title = "Untitled-" + sequence.toString();
        newTitled(title);
    }

    private void newTitled(String title) {
        QueryController queryController = new QueryController(configModel);
        queryController.addObserver(this);
        openList.addController(queryController);
        mainView.addTab(title, (JPanel) queryController.getWidget());
        setTitle();
    }

    private void execute() {
        QueryController controller = openList.getActiveController();
        Connection connection = connectionModel.getConnection();
        controller.execute(connection);
    }

    private void cancel() {
        QueryController controller = openList.getActiveController();
        controller.cancelExecute();
    }

    private void selectSchema() {
        Connection connection = connectionModel.getConnection();
        if (connection != null) {
            SchemaListController schemaListController = SchemaListController.singleton(connection, configModel);
        }
    }

    private void exportResult() {
        QueryController controller = openList.getActiveController();
        controller.exportResult();
    }

    private void showAbout() {
        new AboutDialog(mainView.getContent());
    }

    private void editAction(String action) {
        QueryController controller = openList.getActiveController();
        controller.editAction(action);
    }

    private void openFile() {
        fileChooser.rescanCurrentDirectory();
        int result = fileChooser.showOpenDialog(mainView.getContent());
        if (result == JFileChooser.APPROVE_OPTION) {
            File theFile = fileChooser.getSelectedFile();
            if (theFile != null) {
                String path = theFile.getAbsolutePath();
                if (openList.containsFile(path)) {
                    Dialogs.showInformation("File already open", theFile.getName() + " is already open");
                } else {
                    openTab(theFile);
                }
            }
        }
    }

    private void openTab(File theFile) {
        if (theFile.isFile()) {
            if (theFile.canRead()) {
                newTitled(theFile.getName());
                QueryController controller = openList.getActiveController();
                controller.openFile(theFile.getAbsolutePath());
                setTitle();
                openList.addFile(theFile.getPath());
            }
        }
    }

    private boolean saveFile() {
        boolean saved = false;
        QueryController controller = openList.getActiveController();
        if (controller.hasFileName()) saved = controller.saveFile(); else saved = saveFileAs();
        setTitle();
        return saved;
    }

    private boolean saveFileAs() {
        boolean saved = false;
        fileChooser.setSelectedFile(new File(openList.getActivePath()));
        int result = fileChooser.showSaveDialog(mainView.getContent());
        if (result == JFileChooser.APPROVE_OPTION) {
            File theFile = fileChooser.getSelectedFile();
            if (theFile != null) {
                QueryController controller = openList.getActiveController();
                String oldName = controller.getFileName();
                saved = controller.saveFileAs(theFile.getAbsolutePath());
                if (saved) {
                    mainView.setSelectedTitle(theFile.getName());
                    openList.renameFile(oldName, controller.getFileName());
                    setTitle();
                }
            }
        }
        setTitle();
        return saved;
    }

    private boolean saveFileAll() {
        boolean saved = true;
        for (int i = 0; i < mainView.getQueryCount() && saved; i++) {
            mainView.selectQuery(i);
            saved = saveFile();
        }
        return saved;
    }

    private void closeFile() {
        boolean canClose = true;
        QueryController controller = openList.getActiveController();
        if (controller.isDirty()) canClose = confirmClose(mainView.getSelectedTitle());
        if (canClose) {
            controller.cancelExecute();
            openList.removeFile(controller.getFileName());
            controller.cleanUp();
            controller.deleteObserver(this);
            setTitle();
        }
    }

    private void closeFileAll() {
        if (confirmAll()) {
            QueryController controller = openList.getActiveController();
            while (controller != null) {
                controller.cancelExecute();
                controller.cleanUp();
                controller.deleteObserver(this);
                openList.removeFile(controller.getFileName());
                controller = openList.getActiveController();
            }
            setTitle();
        }
    }

    private void configure() {
        ConfigController configController = ConfigController.singleton(mainView.getContent(), configModel);
    }

    private boolean confirmClose(String title) {
        boolean confirmed = false;
        int reply = Dialogs.getConfirm("File modified", title + " has been modified. Save before closing ?");
        switch(reply) {
            case JOptionPane.YES_OPTION:
                confirmed = saveFile();
                break;
            case JOptionPane.NO_OPTION:
                confirmed = true;
                break;
            case JOptionPane.CANCEL_OPTION:
                break;
            default:
                break;
        }
        return confirmed;
    }

    private boolean confirmAll() {
        boolean canClose = true;
        if (connectionModel.isUpdated()) canClose = confirmDisconnect();
        for (int i = 0; i < mainView.getQueryCount() && canClose; i++) {
            mainView.selectQuery(i);
            QueryController controller = openList.getActiveController();
            if (controller.isDirty()) canClose = confirmClose(mainView.getSelectedTitle());
        }
        return canClose;
    }

    private boolean confirmDisconnect() {
        boolean confirmed = true;
        Object[] options = { "Commit", "Rollback", "Cancel" };
        int reply = Dialogs.getOption("Warning", "Connection has uncommited changes", options, options[0]);
        if (reply == 0) connectionModel.commit(); else if (reply == 1) connectionModel.rollback(); else if (reply == 2) confirmed = false;
        return confirmed;
    }

    private void setTitle() {
        String title = "";
        QueryController controller = openList.getActiveController();
        if (controller != null) {
            title = controller.getFileName();
            if (title.length() == 0) title = mainView.getSelectedTitle();
            if (controller.isDirty()) title = title + "*";
        }
        mainView.setFileTitle(title);
    }

    private void enableGUI() {
        QueryController controller = openList.getActiveController();
        boolean connected = connectionModel.isConnected();
        boolean updated = connectionModel.isUpdated();
        boolean haveFile = false;
        boolean executing = false;
        boolean haveSelection = false;
        boolean haveClip = true;
        boolean canExport = false;
        if (controller != null) {
            haveFile = true;
            executing = controller.isExecuting();
            haveSelection = controller.hasSelection();
            canExport = controller.canExport();
        }
        mainView.enableGUI(connected, haveFile, executing, updated, haveSelection, haveClip, canExport);
    }

    private class OpenList {

        private HashMap controllerMap = new HashMap();

        private HashMap pathMap = new HashMap();

        public void addController(QueryController controller) {
            controllerMap.put(controller.getWidget(), controller);
        }

        public void addFile(String path) {
            pathMap.put(path, null);
        }

        public void removeFile(String path) {
            removeActiveController();
            pathMap.remove(path);
        }

        public void renameFile(String oldPath, String newPath) {
            pathMap.remove(oldPath);
            addFile(newPath);
        }

        public QueryController getActiveController() {
            Object active = mainView.getSelected();
            return getController(active);
        }

        public QueryController getController(Object selected) {
            return (QueryController) controllerMap.get(selected);
        }

        public String getActivePath() {
            String path = "";
            QueryController controller = getActiveController();
            if (controller != null) {
                path = controller.getFileName();
                if (path.length() == 0) path = mainView.getSelectedTitle();
            }
            return path;
        }

        private void removeActiveController() {
            Object removed = mainView.removeSelected();
            controllerMap.remove(removed);
        }

        public boolean containsFile(String path) {
            return pathMap.containsKey(path);
        }

        public void setStatus(String status) {
            Collection controllers = controllerMap.values();
            Iterator i = controllers.iterator();
            while (i.hasNext()) {
                QueryController controller = (QueryController) i.next();
                controller.setStatus(status);
            }
        }
    }

    static {
        JTextField textField = new JTextField();
        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        Keymap map = textField.getKeymap();
        map.removeKeyStrokeBinding(enter);
    }
}
