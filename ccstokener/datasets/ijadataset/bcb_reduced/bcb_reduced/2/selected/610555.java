package org.isqlviewer.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Window;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.TooManyListenersException;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.undo.UndoManager;
import org.apache.log4j.Logger;
import org.isqlviewer.ServiceReference;
import org.isqlviewer.bookmarks.Bookmark;
import org.isqlviewer.bookmarks.BookmarkFolder;
import org.isqlviewer.bookmarks.BookmarkReference;
import org.isqlviewer.history.HistoricalCommand;
import org.isqlviewer.model.BookmarkTreeModel;
import org.isqlviewer.model.HistoryTreeModel;
import org.isqlviewer.model.JdbcSchemaTreeModel;
import org.isqlviewer.sql.ConnectionProfile;
import org.isqlviewer.sql.JdbcService;
import org.isqlviewer.sql.JdbcUtilities;
import org.isqlviewer.sql.embedded.CompatabilityKit;
import org.isqlviewer.sql.embedded.EmbeddedDatabase;
import org.isqlviewer.swing.DocumentAppender;
import org.isqlviewer.swing.EnhancedTabbedPane;
import org.isqlviewer.swing.LoginDialog;
import org.isqlviewer.swing.Refreshable;
import org.isqlviewer.swing.SwingUtilities;
import org.isqlviewer.swing.TabbedPaneLister;
import org.isqlviewer.swing.WizardPanel;
import org.isqlviewer.swing.action.CustomAction;
import org.isqlviewer.swing.action.RefreshViewAction;
import org.isqlviewer.swing.action.SharedActions;
import org.isqlviewer.swing.action.SwingEventManager;
import org.isqlviewer.swing.outline.JOutline;
import org.isqlviewer.swing.table.EnhancedTableModel;
import org.isqlviewer.ui.dnd.BookmarkTreeDropTarget;
import org.isqlviewer.ui.dnd.ResultSetRendererDropTarget;
import org.isqlviewer.ui.listeners.BookmarkMouseInputListener;
import org.isqlviewer.ui.listeners.BookmarkTreeDragListener;
import org.isqlviewer.ui.listeners.HistoryTreeDragListener;
import org.isqlviewer.ui.listeners.SchemaTreeDragListener;
import org.isqlviewer.ui.listeners.SchemaTreeListener;
import org.isqlviewer.ui.renderer.BookmarkTreeCellRenderer;
import org.isqlviewer.ui.renderer.HistoryTableCellRenderer;
import org.isqlviewer.ui.renderer.HistoryTreeCellRenderer;
import org.isqlviewer.ui.renderer.JDBCTreeCellRenderer;
import org.isqlviewer.ui.wizards.Wizard;
import org.isqlviewer.ui.wizards.service.ServiceWizard;
import org.isqlviewer.util.IsqlToolkit;
import org.isqlviewer.util.LocalMessages;
import org.isqlviewer.util.QueryExecutor;
import org.isqlviewer.util.StringUtilities;
import org.isqlviewer.xml.ServiceDigester;
import org.xml.sax.InputSource;

/**
 * @author Mark A. Kobold &lt;mkobold at isqlviewer dot com&gt;
 * @version 1.0
 */
public class JdbcWorkbench extends AbstractApplicationView implements ActionListener, TabbedPaneLister {

    private static final String SESSION_TEXT = "session.text";

    private static final String BOOKMARK_VISIBLE = "bookmark.visible";

    private static final String HISTORY_VISIBLE = "history.visible";

    private static final String SCHEMA_VISIBLE = "schema.visible";

    private static final String SOUTH_DIVIDER = "south.divider.location";

    private static final String WEST_DIVIDER = "west.divider.location";

    private static final String EAST_DIVIDER = "east.divider.location";

    private static final String CENTER_DIVIDER = "center.divider.location";

    private static final String DIVIDER_LOCATION = "org.isqlviewer.divider.location";

    private Preferences preferences = null;

    private JdbcSchemaTreeModel schemaModel = new JdbcSchemaTreeModel();

    private SqlCommandEditor commandEditor = new SqlCommandEditor(true);

    private JEditorPane console = new JEditorPane("text/rtf", "");

    private HistoryTreeModel commandHistory = new HistoryTreeModel();

    private JOutline historyView = new JOutline(commandHistory);

    private JTree schemaView = new JTree(schemaModel);

    private JTree bookmarkView = new JTree();

    private LocalMessages messages = new LocalMessages("org.isqlviewer.ui.ResourceBundle");

    private SwingEventManager eventManager;

    private UndoManager undoManager = new UndoManager();

    private JMenu catalogMenu = null;

    private JMenu schemaMenu = null;

    private JMenu serviceMenu = null;

    private ButtonGroup catalogSelection = new ButtonGroup();

    private ButtonGroup schemaSelection = new ButtonGroup();

    private JdbcService currentService = null;

    private TabbedResultsetRenderer tabRenderer;

    private EnhancedTabbedPane rhsTabbedPane = new EnhancedTabbedPane(EnhancedTabbedPane.TOP);

    private EnhancedTabbedPane lhsTabbedPane = new EnhancedTabbedPane(EnhancedTabbedPane.TOP);

    private EnhancedTabbedPane centerTabbedPane = new EnhancedTabbedPane(EnhancedTabbedPane.TOP);

    private JSplitPane lhsSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

    private JSplitPane rhsSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

    private JSplitPane centerSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    private JSplitPane rootSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    private DocumentAppender documentAppender;

    public JdbcWorkbench() {
        Logger rootLogger = IsqlToolkit.getApplicationLogger();
        documentAppender = new DocumentAppender(console.getDocument(), console);
        rootLogger.addAppender(documentAppender);
    }

    public void actionPerformed(ActionEvent event) {
        int id = event.getID();
        switch(id) {
            case SharedActions.REFRESH_SELECTED_RESULTS:
                break;
            case SharedActions.NAVIGATE_LEFT_TAB:
                navigateLeftTab();
                break;
            case SharedActions.NAVIGATE_RIGHT_TAB:
                navigateRightTab();
                break;
            case SharedActions.CLOSE_SELECTED_RESULTS:
                closeSelectedResult();
                break;
            case SharedActions.CLOSE_ALL_RESULTS:
                centerTabbedPane.removeAllTabs();
                break;
            case SharedActions.CLEAR_ALL_HISTORY:
                clearAllHistory();
                break;
            case SharedActions.SERVICE_REMOVED:
                initializeServiceMenu();
                reloadQueryHistory();
                break;
            case SharedActions.SERVICE_UPDATED:
            case SharedActions.SERVICE_ADDED:
                initializeServiceMenu();
                break;
            case SharedActions.EXIT_APPLICATION:
                Window windowOwner = javax.swing.SwingUtilities.getWindowAncestor(console);
                windowOwner.dispatchEvent(new WindowEvent(windowOwner, WindowEvent.WINDOW_CLOSING));
                break;
            case SharedActions.EDIT_BOOKMARK:
            case SharedActions.DELETE_BOOKMARK_SELECTION:
            case SharedActions.ADD_BOOKMARK_FOLDER:
            case SharedActions.CREATE_NEW_BOOKMARK:
                TreePath tp = bookmarkView.getSelectionPath();
                Object selected = tp == null ? null : tp.getLastPathComponent();
                handleBookmarkAction(id, selected);
                break;
            case SharedActions.CONVERT_TEXT_TO_BOOKMARK:
            case SharedActions.CONVERT_HISTORY_TO_BOOKMARK:
                convertToBookmark(event);
                break;
            case SharedActions.SHOW_SERVICE_WIZARD:
                showWizard(ServiceWizard.class);
                break;
            case SharedActions.SHOW_CONNECTION_INFO:
                if (checkConnection()) {
                    queryDatabaseMetadata();
                }
                break;
            case SharedActions.REFRESH_SELECTED_VIEW:
                Refreshable source = (Refreshable) event.getSource();
                if (source != null) {
                    source.refreshView();
                }
                break;
            case SharedActions.SCHEMA_CHANGED:
                String selectedSchema = event.getActionCommand();
                String currentSchema = schemaModel.getCurrentSchema();
                if (!StringUtilities.compare(currentSchema, selectedSchema)) {
                    schemaModel.setSchema(selectedSchema, true);
                    info(messages.format("jdbcworkbench.schema_change", selectedSchema, currentSchema));
                    updateSyntaxHighlighting();
                } else {
                    info(messages.format("jdbcworkbench.schema_change_skip", currentSchema));
                }
                break;
            case SharedActions.CATALOG_CHANGED:
                String selectedCatalog = catalogSelection.getSelection().getActionCommand();
                String currentCatalog = schemaModel.getCurrentCatlog();
                if (!StringUtilities.compare(selectedCatalog, currentCatalog)) {
                    info(messages.format("jdbcworkbench.catalog_change", selectedCatalog, currentCatalog));
                    schemaModel.setCatalog(selectedCatalog, true);
                    try {
                        currentService.getConnection().setCatalog(selectedCatalog);
                        updateSyntaxHighlighting();
                    } catch (SQLException ignored) {
                    }
                } else {
                    info(messages.format("jdbcworkbench.catalog_change_skip", currentCatalog));
                }
                break;
            case SharedActions.EXECUTE_SQL_COMMAND:
                String statement = event.getActionCommand();
                boolean connected = checkConnection();
                if (connected) {
                    QueryExecutor executor = new QueryExecutor(currentService, tabRenderer, statement);
                    executor.run();
                }
                break;
            case SharedActions.SERVICE_CONNECT:
                String service = event.getActionCommand();
                changeConnection(service);
                updateSyntaxHighlighting();
                break;
            case SharedActions.SERVICE_DISCONNECT:
                handleServiceDisconnect();
                break;
            case EnhancedTabbedPane.ACTION_TAB_DBL_CLICKED:
                Component componentSource = (Component) event.getSource();
                if (zoomComponent(componentSource) instanceof JTabbedPane) {
                    restoreZoom(componentSource);
                }
                break;
            default:
                break;
        }
    }

    public void doLayout(JComponent parentComponent, Preferences userPreferences, SwingEventManager swingEventManager) {
        preferences = userPreferences.node("views/jdbc-workbench");
        tabRenderer = new TabbedResultsetRenderer(centerTabbedPane);
        this.eventManager = swingEventManager;
        this.eventManager.addActionListener(this);
        rhsSplitPane.setContinuousLayout(true);
        centerSplitPane.setContinuousLayout(true);
        lhsSplitPane.setContinuousLayout(true);
        rootSplitPane.setContinuousLayout(true);
        JPanel container = new JPanel();
        commandEditor.doLayout(container, userPreferences, eventManager);
        centerSplitPane.setTopComponent(container);
        centerSplitPane.setBottomComponent(centerTabbedPane);
        centerSplitPane.setBorder(BorderFactory.createEmptyBorder());
        rhsTabbedPane.setName("rhs");
        rhsTabbedPane.addActionListener(this);
        rhsTabbedPane.addTabbedPaneListener(this);
        lhsTabbedPane.setName("lhs");
        lhsTabbedPane.addActionListener(this);
        lhsTabbedPane.addTabbedPaneListener(this);
        TabbedResultsetRenderer renderer = new TabbedResultsetRenderer(centerTabbedPane);
        ResultSetRendererDropTarget rsrdt = new ResultSetRendererDropTarget();
        rsrdt.setEventManager(eventManager);
        rsrdt.setNodeRenderer(renderer);
        rsrdt.setTreeModel(schemaModel);
        try {
            DropTarget dt = new DropTarget();
            dt.addDropTargetListener(rsrdt);
            centerTabbedPane.setDropTarget(dt);
        } catch (TooManyListenersException e) {
        }
        centerTabbedPane.setName("query-results");
        centerTabbedPane.addActionListener(this);
        centerTabbedPane.addTabbedPaneListener(this);
        rhsSplitPane.setLeftComponent(centerSplitPane);
        rhsSplitPane.setBorder(BorderFactory.createEmptyBorder());
        lhsSplitPane.setRightComponent(rhsSplitPane);
        lhsSplitPane.setBorder(BorderFactory.createEmptyBorder());
        rootSplitPane.setTopComponent(lhsSplitPane);
        rootSplitPane.setBottomComponent(new JScrollPane(console));
        rootSplitPane.setBorder(BorderFactory.createEmptyBorder());
        parentComponent.setLayout(new BorderLayout());
        parentComponent.add(rootSplitPane, BorderLayout.CENTER);
        bookmarkView.setCellRenderer(new BookmarkTreeCellRenderer());
        bookmarkView.setRootVisible(false);
        bookmarkView.setScrollsOnExpand(true);
    }

    public void configureMenubar(JMenuBar menuBar) {
        menuBar.add(createFileMenu(eventManager, messages.getMessage("menu.file.title")));
        menuBar.add(createServiceMenu(messages.getMessage("menu.service.title")));
        menuBar.add(createCatalogMenu(messages.getMessage("menu.catalog.title")));
        menuBar.add(createSchemaMenu(messages.getMessage("menu.schema.title")));
        menuBar.add(createWindowMenu(eventManager, messages.getMessage("menu.window.title")));
        menuBar.add(createToolMenu(eventManager, messages.getMessage("menu.tools.title")));
        if (!SwingUtilities.isMacOS() || !Boolean.getBoolean(IsqlToolkit.PROPERTY_MRJ_ENABLED)) {
            JMenu t = createMenu(messages.format("menu.help.title"));
            menuBar.add(t);
        }
    }

    public void initializeView() {
        String isqlVersion = System.getProperty(IsqlToolkit.PROPERTY_VERSION);
        info(messages.format("JdbcWorkbench.initializing_isql", isqlVersion));
        String logicalVersion = System.getProperty("java.specification.version");
        String actualVersion = System.getProperty("java.version");
        String vmProvider = System.getProperty("java.vm.vendor");
        info(messages.format("JdbcWorkbench.jre_information", actualVersion, logicalVersion, vmProvider));
        configureSplitPanel(rhsSplitPane, EAST_DIVIDER);
        configureSplitPanel(centerSplitPane, CENTER_DIVIDER);
        configureSplitPanel(lhsSplitPane, WEST_DIVIDER);
        configureSplitPanel(rootSplitPane, SOUTH_DIVIDER);
        localizeTextComponent(console, undoManager);
        console.setEditable(false);
        console.setDragEnabled(true);
        commandEditor.initializeView();
        schemaMenu.setEnabled(false);
        catalogMenu.setEnabled(false);
        String sessionText = preferences.get(SESSION_TEXT, "");
        commandEditor.setText(sessionText);
        EmbeddedDatabase edb = EmbeddedDatabase.getSharedInstance();
        try {
            if (edb.initialize()) {
                info(messages.format("JdbcWorkbench.importing_service"));
                Collection<ServiceReference> services = CompatabilityKit.get2xxServices();
                for (ServiceReference serviceReference : services) {
                    info(messages.format("JdbcWorkbench.importing_service_x", serviceReference.getName()));
                    edb.addService(serviceReference);
                }
                info(messages.format("JdbcWorkbench.importing_service_complete"));
                BookmarkFolder rootFolder = CompatabilityKit.get2xxBookmarks();
                if (rootFolder != null) {
                    info(messages.format("JdbcWorkbench.importing_bookmarks"));
                    edb.addBookmarkFolder(rootFolder);
                } else {
                    info(messages.format("JdbcWorkbench.skipping_importing_bookmarks"));
                }
                info(messages.format("JdbcWorkbench.importing_history"));
                Collection<HistoricalCommand> history = CompatabilityKit.get2xxHistory();
                for (HistoricalCommand item : history) {
                    edb.addHistoricalCommand(item);
                }
                info(messages.format("JdbcWorkbench.import_history_success", Integer.toString(history.size())));
            }
        } catch (SQLException error) {
            error(messages.format("JdbcWorkbench.failed_to_initialize_internal_database"), error);
        }
        try {
            BookmarkFolder rootFolder = edb.getBookmarks();
            bookmarkView.setModel(new BookmarkTreeModel(rootFolder));
            initializeServiceMenu();
            reloadQueryHistory();
        } catch (SQLException e) {
            error(messages.format("JdbcWorkbench.failed_to_load_bookmarks"), e);
            return;
        }
        info(messages.format("JdbcWorkbench.initialization_complete"));
    }

    public void disposeView(Preferences userPreferences) {
        preferences = userPreferences.node("views/jdbc-workbench");
        preferences.putInt(CENTER_DIVIDER, centerSplitPane.getDividerLocation());
        preferences.putInt(EAST_DIVIDER, rhsSplitPane.getDividerLocation());
        preferences.putInt(WEST_DIVIDER, lhsSplitPane.getDividerLocation());
        preferences.putInt(SOUTH_DIVIDER, rootSplitPane.getDividerLocation());
        Boolean clientProperty = null;
        clientProperty = (Boolean) schemaView.getClientProperty(SCHEMA_VISIBLE);
        preferences.putBoolean(SCHEMA_VISIBLE, clientProperty.booleanValue());
        clientProperty = (Boolean) historyView.getClientProperty(HISTORY_VISIBLE);
        preferences.putBoolean(HISTORY_VISIBLE, clientProperty.booleanValue());
        clientProperty = (Boolean) bookmarkView.getClientProperty(BOOKMARK_VISIBLE);
        preferences.putBoolean(BOOKMARK_VISIBLE, clientProperty.booleanValue());
        preferences.put(SESSION_TEXT, commandEditor.getText());
        unlocalizeTextComponent(console, undoManager);
    }

    public void tabAdded(JTabbedPane tabPane, int index, Component component) {
    }

    public void tabRemoved(JTabbedPane tabPane, int index, Component component) {
        WorkbenchView view = (WorkbenchView) ((JComponent) component).getClientProperty("type");
        WizardPanel panel = (WizardPanel) javax.swing.SwingUtilities.getAncestorOfClass(WizardPanel.class, tabPane);
        Component rootView = panel.getCard("root-view");
        boolean isEmptyTabbedPane = tabPane.getTabCount() == 0;
        if (isEmptyTabbedPane && rootView == tabPane) {
            zoomComponent(tabPane);
            if (tabPane == centerTabbedPane) {
                centerSplitPane.setBottomComponent(centerTabbedPane);
                restoreZoom(tabPane);
                centerSplitPane.invalidate();
            }
        } else if (isEmptyTabbedPane && tabPane != centerTabbedPane) {
            JSplitPane parent = (JSplitPane) javax.swing.SwingUtilities.getAncestorOfClass(JSplitPane.class, tabPane);
            parent.putClientProperty(DIVIDER_LOCATION, new Integer(parent.getDividerLocation()));
            parent.remove(tabPane);
        }
        if (view != null) {
            switch(view) {
                case BOOKMARK:
                    bookmarkView.putClientProperty(BOOKMARK_VISIBLE, Boolean.FALSE);
                    break;
                case HISTORY:
                    historyView.putClientProperty(HISTORY_VISIBLE, Boolean.FALSE);
                    break;
                case SCHEMA:
                    schemaView.putClientProperty(SCHEMA_VISIBLE, Boolean.FALSE);
                    break;
            }
        }
    }

    public Component zoomComponent(Component view) {
        JSplitPane splitParent = (JSplitPane) javax.swing.SwingUtilities.getAncestorOfClass(JSplitPane.class, view);
        if (splitParent != null) {
            splitParent.putClientProperty(DIVIDER_LOCATION, new Integer(splitParent.getDividerLocation()));
        }
        WizardPanel panel = (WizardPanel) javax.swing.SwingUtilities.getAncestorOfClass(WizardPanel.class, view);
        Component old = panel.removeCard("root-view");
        if (view != null && view != old) {
            panel.add(view, "root-view");
            panel.showCard("root-view");
            panel.add(old, "previous");
            panel.invalidate();
        } else {
            Component previous = panel.removeCard("previous");
            panel.add(previous, "root-view");
        }
        return old;
    }

    private void convertToBookmark(ActionEvent event) {
        Object eventSource = event.getSource();
        Bookmark bookmark = new Bookmark();
        if (eventSource instanceof HistoricalCommand) {
            HistoricalCommand historicalCommand = (HistoricalCommand) event.getSource();
            bookmark.setCreationTime(historicalCommand.getQueryTime());
            bookmark.setCommandText(historicalCommand.getCommandText());
        } else if (eventSource instanceof String) {
            bookmark.setCreationTime(new Date());
            bookmark.setCommandText((String) eventSource);
        } else {
            return;
        }
        String targetPath = event.getActionCommand();
        BookmarkTreeModel model = (BookmarkTreeModel) bookmarkView.getModel();
        BookmarkFolder rootFolder = (BookmarkFolder) model.getRoot();
        BookmarkFolder targetFolder = rootFolder.findChildPath(targetPath);
        bookmark.setLastAccess(new Date());
        bookmark.setFolder(targetFolder);
        EmbeddedDatabase database = EmbeddedDatabase.getSharedInstance();
        OperationStatus status = showModalEditor(BookmarkEditor.class, bookmark);
        if (status == OperationStatus.MODIFIED) {
            try {
                BookmarkReference reference = database.addBookmark(bookmark);
                model.addBookmark(reference, targetFolder);
            } catch (SQLException e) {
                error("", e);
            }
        }
    }

    private void reloadQueryHistory() {
        EmbeddedDatabase edb = EmbeddedDatabase.getSharedInstance();
        commandHistory.clear();
        try {
            Collection<HistoricalCommand> history = edb.getHistory();
            for (HistoricalCommand reference : history) {
                commandHistory.addReference(reference);
            }
        } catch (SQLException error) {
            error("", error);
        } finally {
            commandHistory.reload();
        }
    }

    private void initializeServiceMenu() {
        EmbeddedDatabase edb = EmbeddedDatabase.getSharedInstance();
        serviceMenu.removeAll();
        try {
            Collection<ServiceReference> services = edb.getRegisteredServices();
            for (ServiceReference reference : services) {
                String serviceName = reference.getName();
                addDynamicAction(serviceMenu, eventManager, "service", SharedActions.SERVICE_CONNECT, serviceName);
            }
        } catch (SQLException error) {
            error(messages.format("JdbcWorkbench.error_loading_services"), error);
        }
        serviceMenu.addSeparator();
        CustomAction ca = null;
        String acclerator = null;
        ca = new CustomAction("disconnect-service", SharedActions.SERVICE_DISCONNECT, eventManager);
        acclerator = messages.format("menu.disconnect_service.accelerator");
        ca.putValue(CustomAction.ICON_NAME, "disconnect");
        ca.putValue(Action.ACCELERATOR_KEY, SwingUtilities.getKeyStroke(acclerator));
        ca.putValue(Action.DEFAULT, messages.getMessage("menu.disconnect_service.title"));
        addMenuItem(serviceMenu, ca);
    }

    private void handleServiceDisconnect() {
        try {
            currentService.getConnection().close();
        } catch (SQLException sqle) {
        } finally {
            schemaModel.clear();
        }
        info(messages.format("jdbcworkbench.serivcedisconnect", currentService.getName()));
    }

    private boolean promptForService() {
        Object message = messages.getMessage("jdbcworkbench.please_select_your_service");
        EmbeddedDatabase edb = EmbeddedDatabase.getSharedInstance();
        Object[] selectionValues;
        try {
            Collection<ServiceReference> references = edb.getRegisteredServices();
            selectionValues = new Object[references.size()];
            int i = 0;
            for (ServiceReference reference : references) {
                selectionValues[i++] = reference.getName();
            }
        } catch (SQLException e) {
            error("", e);
            return false;
        }
        int type = JOptionPane.INFORMATION_MESSAGE;
        Frame frameOwner = (Frame) javax.swing.SwingUtilities.getAncestorOfClass(Frame.class, centerTabbedPane);
        Object selection = JOptionPane.showInputDialog(frameOwner, message, "", type, null, selectionValues, null);
        if (selection != null) {
            return changeConnection((String) selection);
        }
        return false;
    }

    private boolean changeConnection(String service) {
        if (currentService != null) {
            handleServiceDisconnect();
        }
        EmbeddedDatabase database = EmbeddedDatabase.getSharedInstance();
        try {
            ServiceReference reference = database.getServiceForName(service);
            URL url = reference.getResourceURL();
            InputStream inputStream = null;
            try {
                inputStream = url.openStream();
                InputSource inputSource = new InputSource(inputStream);
                currentService = ServiceDigester.parseService(inputSource, IsqlToolkit.getSharedEntityResolver());
            } catch (IOException error) {
                error("", error);
            } catch (Exception error) {
                error("", error);
            }
        } catch (SQLException error) {
            error("", error);
        }
        currentService.setCommandLogger(commandHistory);
        ConnectionProfile profile = currentService.getProfile();
        Connection connection = null;
        if (requiresPrompt(currentService)) {
            Frame frameOwner = (Frame) javax.swing.SwingUtilities.getAncestorOfClass(Frame.class, centerTabbedPane);
            LoginDialog dialog = new LoginDialog(frameOwner);
            dialog.setVisible(true);
            String[] tokens = dialog.getAuthTokens();
            if (tokens == null) {
                info(messages.format("jdbcworkbench.connectioncancelled", service));
                return false;
            }
            try {
                connection = currentService.getConnection(tokens[0], tokens[1]);
            } catch (SQLException ignored) {
                return false;
            }
        } else {
            try {
                connection = currentService.getConnection();
            } catch (SQLException ignored) {
                return false;
            }
        }
        Logger newLogger = Logger.getLogger(currentService.getDriverClass());
        newLogger.addAppender(documentAppender);
        try {
            String preferredSchema = buildSchemaMenu(connection, profile);
            String preferredCatalog = buildCatalogMenu(connection, profile);
            DatabaseMetaData metaData = currentService.getConnection().getMetaData();
            info(messages.format("jdbcworkbench.connectionsucessfull", service, preferredSchema, preferredCatalog));
            String name = metaData.getDriverName();
            String version = metaData.getDriverVersion();
            info(messages.format("jdbcworkbench.driver_information", name, version));
            name = metaData.getDatabaseProductName();
            version = metaData.getDatabaseProductVersion();
            info(messages.format("jdbcworkbench.database_information", name, version));
            schemaMenu.setEnabled(true);
            catalogMenu.setEnabled(true);
            schemaModel.setSchema(preferredSchema, false);
            schemaModel.setCatalog(preferredCatalog, false);
            schemaModel.updateConnection(connection, false);
            schemaModel.reload();
        } catch (SQLException e) {
            error("xxxxxx", e);
        }
        return true;
    }

    private static boolean requiresPrompt(JdbcService currentService) {
        String principal = currentService.getPrincipal();
        String credentials = currentService.getCredentials();
        return principal == null && credentials == null;
    }

    private void showWizard(Class<? extends Wizard> wizard) {
        Wizard instance = null;
        try {
            instance = wizard.newInstance();
            Frame frameOwner = (Frame) javax.swing.SwingUtilities.getAncestorOfClass(Frame.class, centerTabbedPane);
            WizardRunner wizardRunner = new WizardRunner(frameOwner, instance, eventManager);
            wizardRunner.pack();
            wizardRunner.setLocationRelativeTo(frameOwner);
            wizardRunner.setModal(true);
            wizardRunner.setVisible(true);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private OperationStatus showModalEditor(Class<? extends AbstractObjectEditor> name, Object editableObject) {
        Preferences prefs = Preferences.userRoot().node(IsqlToolkit.getRootPreferencesNode());
        AbstractObjectEditor instance = null;
        try {
            instance = name.newInstance();
            Object frameOwner = javax.swing.SwingUtilities.getAncestorOfClass(Frame.class, centerTabbedPane);
            JDialog dialog = new JDialog((Frame) frameOwner);
            JComponent contentPanel = (JComponent) dialog.getContentPane();
            instance.doLayout(contentPanel, prefs, eventManager);
            instance.initializeView(editableObject);
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialog.pack();
            dialog.setLocationRelativeTo((Component) frameOwner);
            dialog.setModal(true);
            dialog.setVisible(true);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } finally {
            if (instance != null) {
                try {
                    instance.disposeView(prefs);
                } catch (Exception ignored) {
                    debug("", ignored);
                }
            }
        }
        if (instance != null) {
            return instance.getStatus();
        }
        return OperationStatus.UNKNOWN;
    }

    private JComponent layoutHistoryContainer(SwingEventManager manager) {
        JPanel view = new JPanel(new BorderLayout());
        view.putClientProperty("type", WorkbenchView.HISTORY);
        JToolBar toolbar = new JToolBar("history-tools", JToolBar.HORIZONTAL);
        toolbar.setFloatable(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder());
        historyView.setTreeCellRenderer(new HistoryTreeCellRenderer(historyView));
        TableColumnModel columnModel = historyView.getColumnModel();
        TableColumn column = columnModel.getColumn(1);
        column.setCellRenderer(new HistoryTableCellRenderer(commandHistory));
        historyView.setRootVisible(false);
        ListSelectionModel tableSelectionModel = historyView.getSelectionModel();
        tableSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        HistoryTreeDragListener dragListener = new HistoryTreeDragListener(historyView);
        DragSource dndDragSource = DragSource.getDefaultDragSource();
        dndDragSource.createDefaultDragGestureRecognizer(historyView, DnDConstants.ACTION_COPY_OR_MOVE, dragListener);
        Action action = null;
        action = new CustomAction("clear-all-history", SharedActions.CLEAR_ALL_HISTORY, manager);
        action.putValue(Action.SHORT_DESCRIPTION, messages.getMessage("JdbcWorkbench.clear-all-history"));
        action.putValue(CustomAction.ICON_NAME, "history_clear");
        addButton(toolbar, manager, action);
        view.add(toolbar, BorderLayout.NORTH);
        view.add(new JScrollPane(historyView), BorderLayout.CENTER);
        return view;
    }

    private JComponent layoutBookmarkContainer(SwingEventManager manager) {
        JPanel view = new JPanel(new BorderLayout());
        view.putClientProperty("type", WorkbenchView.BOOKMARK);
        JToolBar toolbar = new JToolBar("bookmark-tools", JToolBar.HORIZONTAL);
        toolbar.setFloatable(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder());
        view.add(toolbar, BorderLayout.NORTH);
        view.add(new JScrollPane(bookmarkView), BorderLayout.CENTER);
        bookmarkView.setDragEnabled(true);
        bookmarkView.setEditable(true);
        bookmarkView.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        bookmarkView.addMouseListener(new BookmarkMouseInputListener(bookmarkView, manager));
        BookmarkTreeDropTarget bookmarkDropListener = new BookmarkTreeDropTarget(manager);
        int supportedActions = DnDConstants.ACTION_COPY_OR_MOVE;
        bookmarkView.setDropTarget(new DropTarget(bookmarkView, supportedActions, bookmarkDropListener));
        Action action = null;
        InputMap im = bookmarkView.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = bookmarkView.getActionMap();
        KeyStroke ks = null;
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_I, SwingUtilities.MENU_SHORTCUT_MASK, true);
        im.put(ks, "edit-bookmark");
        action = new CustomAction("edit-bookmark", SharedActions.EDIT_BOOKMARK, manager);
        action.putValue(Action.ACCELERATOR_KEY, ks);
        action.putValue(Action.SHORT_DESCRIPTION, messages.getMessage("JdbcWorkbench.edit_bookmark"));
        action.putValue(CustomAction.ICON_NAME, "edit");
        am.put("edit-bookmark", action);
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0, true);
        im.put(ks, "delete-bookmark-selection");
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, true);
        action = new CustomAction("delete-bookmark-selection", SharedActions.DELETE_BOOKMARK_SELECTION, manager);
        action.putValue(Action.ACCELERATOR_KEY, ks);
        action.putValue(Action.SHORT_DESCRIPTION, messages.getMessage("JdbcWorkbench.delete_bookmark"));
        action.putValue(CustomAction.ICON_NAME, "delete");
        am.put("delete-bookmark-selection", action);
        im.put(ks, "delete-bookmark-selection");
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_N, SwingUtilities.MENU_SHORTCUT_MASK, true);
        im.put(ks, "add-bookmark");
        action = new CustomAction("add-bookmark", SharedActions.CREATE_NEW_BOOKMARK, manager);
        action.putValue(Action.ACCELERATOR_KEY, ks);
        action.putValue(Action.SHORT_DESCRIPTION, messages.getMessage("JdbcWorkbench.create_new_bookmark"));
        action.putValue(CustomAction.ICON_NAME, "bookmark_add");
        am.put("add-bookmark", action);
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_N, SwingUtilities.MENU_SHORTCUT_MASK | KeyEvent.SHIFT_DOWN_MASK, true);
        action = new CustomAction("add-bookmark-folder", SharedActions.ADD_BOOKMARK_FOLDER, manager);
        action.putValue(Action.ACCELERATOR_KEY, ks);
        action.putValue(Action.SHORT_DESCRIPTION, messages.getMessage("JdbcWorkbench.create_new_bookmark_folder"));
        action.putValue(CustomAction.ICON_NAME, "folder_add");
        am.put("add-bookmark-folder", action);
        im.put(ks, "add-bookmark-folder");
        BookmarkTreeDragListener dragListener = new BookmarkTreeDragListener(bookmarkView);
        DragSource dndDragSource = DragSource.getDefaultDragSource();
        dndDragSource.createDefaultDragGestureRecognizer(bookmarkView, supportedActions, dragListener);
        addButton(toolbar, manager, manager.getAction("add-bookmark"));
        addButton(toolbar, manager, manager.getAction("delete-bookmark-selection"));
        addButton(toolbar, manager, manager.getAction("edit-bookmark"));
        addButton(toolbar, manager, manager.getAction("add-bookmark-folder"));
        return view;
    }

    private JComponent layoutSchemaContainer(SwingEventManager manager) {
        JPanel view = new JPanel(new BorderLayout());
        view.putClientProperty("type", WorkbenchView.SCHEMA);
        JToolBar toolbar = new JToolBar("schema-tools", JToolBar.HORIZONTAL);
        toolbar.setFloatable(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder());
        JdbcSchemaTreeRefreshable refresher = new JdbcSchemaTreeRefreshable(schemaModel, schemaView);
        Action action = new RefreshViewAction(manager, refresher);
        action.putValue(Action.SHORT_DESCRIPTION, messages.format("jdbcworkbench.refresh_current_schema_view"));
        action.putValue(CustomAction.ICON_NAME, "reload");
        addButton(toolbar, manager, action);
        action = new CustomAction("show-meta-data", SharedActions.SHOW_CONNECTION_INFO, manager);
        action.putValue(Action.SHORT_DESCRIPTION, messages.format("jdbcworkbench.show_meta_data"));
        action.putValue(CustomAction.ICON_NAME, "information");
        addButton(toolbar, manager, action);
        SchemaTreeListener listener = new SchemaTreeListener(schemaModel, tabRenderer, manager);
        schemaModel.setTablesEnabled(true);
        schemaView.setCellRenderer(new JDBCTreeCellRenderer());
        schemaView.setScrollsOnExpand(true);
        schemaView.setRootVisible(false);
        schemaView.addMouseListener(listener);
        schemaView.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        schemaView.addTreeExpansionListener(listener);
        InputMap im = schemaView.getInputMap();
        ActionMap am = schemaView.getActionMap();
        KeyStroke ks = null;
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_I, SwingUtilities.MENU_SHORTCUT_MASK, true);
        im.put(ks, "show-metadata-info");
        action = new CustomAction("show-metadata-info", SharedActions.NAVIGATE_RIGHT_TAB, manager);
        action.putValue(Action.ACCELERATOR_KEY, ks);
        action.putValue(Action.SHORT_DESCRIPTION, messages.getMessage("JdbcWorkbench.show_connection_info"));
        action.putValue(CustomAction.ICON_NAME, "information");
        am.put("show-metadata-info", action);
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_R, SwingUtilities.MENU_SHORTCUT_MASK, true);
        im.put(ks, "refresh-metadata-info");
        am.put("refresh-metadata-info", action);
        view.add(toolbar, BorderLayout.NORTH);
        view.add(new JScrollPane(schemaView), BorderLayout.CENTER);
        schemaView.setDragEnabled(true);
        SchemaTreeDragListener dragListener = new SchemaTreeDragListener(schemaView);
        DragSource dndDragSource = DragSource.getDefaultDragSource();
        int dndActions = DnDConstants.ACTION_COPY_OR_MOVE;
        dndDragSource.createDefaultDragGestureRecognizer(schemaView, dndActions, dragListener);
        return view;
    }

    private JMenu createFileMenu(SwingEventManager manager, String name) {
        JMenu menu = createMenu(name);
        CustomAction ca = null;
        String acclerator = null;
        JMenu newMenu = createMenu(messages.format("menu.new_submenu"));
        newMenu.setIcon(SwingUtilities.loadIconResource("spacer", 16));
        menu.add(newMenu);
        ca = new CustomAction("new-bookmark", SharedActions.CREATE_NEW_BOOKMARK, manager);
        ca.putValue(CustomAction.ICON_NAME, "bookmark");
        ca.putValue(Action.DEFAULT, messages.getMessage("menu.new_bookmark.title"));
        addMenuItem(newMenu, ca);
        ca = new CustomAction("new-service", SharedActions.SHOW_SERVICE_WIZARD, manager);
        ca.putValue(CustomAction.ICON_NAME, "service");
        ca.putValue(Action.DEFAULT, messages.getMessage("menu.new_service.title"));
        addMenuItem(newMenu, ca);
        menu.addSeparator();
        ca = new CustomAction("import-wizard", SharedActions.SHOW_IMPORT_WIZARD, manager);
        ca.putValue(Action.DEFAULT, messages.getMessage("menu.import_wizard.title"));
        ca.putValue(CustomAction.ICON_NAME, "import");
        ca.setEnabled(false);
        addMenuItem(menu, ca);
        ca = new CustomAction("export-wizard", SharedActions.SHOW_EXPORT_WIZARD, manager);
        ca.putValue(CustomAction.ICON_NAME, "export");
        ca.putValue(Action.DEFAULT, messages.getMessage("menu.export_wizard.title"));
        ca.setEnabled(false);
        addMenuItem(menu, ca);
        menu.addSeparator();
        ca = new CustomAction("close-tab", SharedActions.CLOSE_SELECTED_RESULTS, manager);
        acclerator = messages.format("menu.close_selected_view.accelerator");
        ca.putValue(Action.ACCELERATOR_KEY, SwingUtilities.getKeyStroke(acclerator));
        ca.putValue(Action.DEFAULT, messages.getMessage("menu.close_selected_view.title"));
        addMenuItem(menu, ca);
        ca = new CustomAction("close-all", SharedActions.CLOSE_ALL_RESULTS, manager);
        acclerator = messages.format("menu.close_all_views.accelerator");
        ca.putValue(Action.ACCELERATOR_KEY, SwingUtilities.getKeyStroke(acclerator));
        ca.putValue(Action.DEFAULT, messages.getMessage("menu.close_all_views.title"));
        addMenuItem(menu, ca);
        if (!(SwingUtilities.isMacOS() && Boolean.getBoolean(IsqlToolkit.PROPERTY_MRJ_ENABLED))) {
            menu.addSeparator();
            ca = new CustomAction("close-all", SharedActions.EXIT_APPLICATION, manager);
            acclerator = messages.format("menu.exit_application.accelerator");
            ca.putValue(Action.ACCELERATOR_KEY, SwingUtilities.getKeyStroke(acclerator));
            ca.putValue(CustomAction.ICON_NAME, "exit");
            ca.putValue(Action.DEFAULT, messages.getMessage("menu.exit_application.title"));
            addMenuItem(menu, ca);
        }
        return menu;
    }

    private JMenu createWindowMenu(SwingEventManager manager, String name) {
        JMenu menu = createMenu(name);
        JMenu viewMenu = createMenu(messages.getMessage("menu.show_view.title"));
        viewMenu.setIcon(SwingUtilities.loadIconResource("spacer", 16));
        ToggleViewAction action = null;
        CustomAction ca = null;
        String acclerator = null;
        action = new ToggleViewAction(WorkbenchView.BOOKMARK, this);
        acclerator = messages.format("menu.view_bookmarks.accelerator");
        action.putValue(CustomAction.ICON_NAME, "bookmark");
        action.putValue(Action.ACCELERATOR_KEY, SwingUtilities.getKeyStroke(acclerator));
        action.putValue(Action.DEFAULT, messages.format("menu.view_bookmarks.title"));
        if (preferences.getBoolean(BOOKMARK_VISIBLE, false)) {
            action.actionPerformed(new ActionEvent(this, 0, ""));
        } else {
            bookmarkView.putClientProperty(BOOKMARK_VISIBLE, Boolean.FALSE);
        }
        addMenuItem(viewMenu, action);
        action = new ToggleViewAction(WorkbenchView.HISTORY, this);
        action.putValue(CustomAction.ICON_NAME, "history");
        acclerator = messages.format("menu.view_history.accelerator");
        action.putValue(Action.ACCELERATOR_KEY, SwingUtilities.getKeyStroke(acclerator));
        action.putValue(Action.DEFAULT, messages.getMessage("menu.view_history.title"));
        if (preferences.getBoolean(HISTORY_VISIBLE, false)) {
            action.actionPerformed(new ActionEvent(this, 0, ""));
        } else {
            historyView.putClientProperty(HISTORY_VISIBLE, Boolean.FALSE);
        }
        addMenuItem(viewMenu, action);
        action = new ToggleViewAction(WorkbenchView.SCHEMA, this);
        action.putValue(CustomAction.ICON_NAME, "database");
        acclerator = messages.format("menu.view_schema.accelerator");
        action.putValue(Action.ACCELERATOR_KEY, SwingUtilities.getKeyStroke(acclerator));
        action.putValue(Action.DEFAULT, messages.getMessage("menu.view_schema.title"));
        if (preferences.getBoolean(SCHEMA_VISIBLE, false)) {
            action.actionPerformed(new ActionEvent(this, 0, ""));
        } else {
            schemaView.putClientProperty(SCHEMA_VISIBLE, Boolean.TRUE);
        }
        addMenuItem(viewMenu, action);
        menu.add(viewMenu);
        menu.addSeparator();
        ca = new CustomAction("refresh-current-view", SharedActions.REFRESH_SELECTED_RESULTS, manager);
        acclerator = messages.format("menu.refresh_selected_view.accelerator");
        ca.setEnabled(false);
        ca.putValue(CustomAction.ICON_NAME, "reload");
        ca.putValue(Action.ACCELERATOR_KEY, SwingUtilities.getKeyStroke(acclerator));
        ca.putValue(Action.DEFAULT, messages.getMessage("menu.refresh_selected_view.title"));
        addMenuItem(menu, ca);
        ca = new CustomAction("next-tab", SharedActions.NAVIGATE_RIGHT_TAB, manager);
        acclerator = messages.format("menu.navigate_right_tab.accelerator");
        ca.putValue(Action.ACCELERATOR_KEY, SwingUtilities.getKeyStroke(acclerator));
        ca.putValue(Action.DEFAULT, messages.getMessage("menu.navigate_right_tab.title"));
        addMenuItem(menu, ca);
        ca = new CustomAction("prev-tab", SharedActions.NAVIGATE_LEFT_TAB, manager);
        acclerator = messages.format("menu.navigate_left_tab.accelerator");
        ca.putValue(Action.ACCELERATOR_KEY, SwingUtilities.getKeyStroke(acclerator));
        ca.putValue(Action.DEFAULT, messages.getMessage("menu.navigate_left_tab.title"));
        addMenuItem(menu, ca);
        return menu;
    }

    private JMenu createToolMenu(SwingEventManager manager, String name) {
        JMenu menu = createMenu(name);
        CustomAction ca = null;
        ca = new CustomAction("show-service-wizard", SharedActions.SHOW_SERVICE_WIZARD, manager);
        ca.putValue(Action.DEFAULT, messages.getMessage("menu.show_service_wizard.title"));
        ca.putValue(CustomAction.ICON_NAME, "service_manager");
        addMenuItem(menu, ca);
        return menu;
    }

    private JMenu createServiceMenu(String name) {
        serviceMenu = createMenu(name);
        return serviceMenu;
    }

    private JMenu createSchemaMenu(String name) {
        schemaMenu = createMenu(name);
        return schemaMenu;
    }

    private String buildSchemaMenu(Connection connection, ConnectionProfile profile) throws SQLException {
        schemaMenu.removeAll();
        Enumeration<AbstractButton> elements = schemaSelection.getElements();
        while (elements.hasMoreElements()) {
            catalogSelection.remove(elements.nextElement());
        }
        ResultSet set = connection.getMetaData().getSchemas();
        String preferredSchema = profile.getPreferredSchema();
        if (preferredSchema == null || preferredSchema.trim().length() == 0) {
            preferredSchema = connection.getMetaData().getUserName();
        }
        boolean schemaFound = false;
        String firstAvailableSchema = null;
        try {
            while (set.next()) {
                String schema = set.getString("TABLE_SCHEM");
                if (!schemaFound && preferredSchema.equalsIgnoreCase(schema)) {
                    schemaFound = true;
                }
                if (firstAvailableSchema == null) {
                    firstAvailableSchema = schema;
                }
                CustomAction action = new CustomAction(schema, SharedActions.SCHEMA_CHANGED, eventManager);
                action.putValue(Action.DEFAULT, schema);
                action.putValue(CustomAction.ICON_NAME, "user");
                JCheckBoxMenuItem button = new JCheckBoxMenuItem(action);
                button.setName(schema);
                configureMenuItem(button, action);
                schemaSelection.add(button);
                if (schema.equals(preferredSchema)) {
                    schemaSelection.setSelected(button.getModel(), true);
                }
                schemaMenu.add(button);
            }
        } finally {
            try {
                set.close();
            } catch (Throwable ignored) {
            }
        }
        if (!schemaFound) {
            warn(messages.format("jdbcworkbench.schemanotfoundwarning", preferredSchema));
            preferredSchema = firstAvailableSchema;
            Enumeration<AbstractButton> selections = schemaSelection.getElements();
            while (selections.hasMoreElements()) {
                AbstractButton button = selections.nextElement();
                String otherSchema = button.getName();
                if (otherSchema.equalsIgnoreCase(preferredSchema)) {
                    schemaSelection.setSelected(button.getModel(), true);
                    break;
                }
            }
        }
        if (schemaSelection.getButtonCount() == 0) {
            JMenuItem disabledOption = new JMenuItem();
            disabledOption.setEnabled(false);
            disabledOption.setText(messages.format("JdbcWorkbench.no_schemas"));
            disabledOption.setIcon(SwingUtilities.loadIconResource("user", 16));
            schemaMenu.add(disabledOption);
        }
        return preferredSchema;
    }

    private JMenu createCatalogMenu(String name) {
        catalogMenu = createMenu(name);
        return catalogMenu;
    }

    private Component toggleView(WorkbenchView view, Component component) {
        String txt = null;
        String tip = null;
        Icon ico = null;
        boolean inTab = javax.swing.SwingUtilities.getAncestorOfClass(EnhancedTabbedPane.class, component) != null;
        Component componentView = null;
        boolean setDivider = false;
        switch(view) {
            case HISTORY:
                historyView.putClientProperty(HISTORY_VISIBLE, Boolean.TRUE);
                if (rhsSplitPane.getRightComponent() == null) {
                    rhsSplitPane.setRightComponent(rhsTabbedPane);
                    Integer dividerLocation = (Integer) rhsSplitPane.getClientProperty(DIVIDER_LOCATION);
                    if (dividerLocation != null) {
                        rhsSplitPane.setDividerLocation(dividerLocation.intValue());
                    } else {
                        setDivider = true;
                    }
                }
                try {
                    if (inTab) {
                        rhsTabbedPane.setSelectedComponent(component);
                    } else {
                        ico = SwingUtilities.loadIconResource("history", 16);
                        tip = messages.format("jdbcworkbench.history_tab.tip");
                        componentView = layoutHistoryContainer(eventManager);
                        rhsTabbedPane.addTab(txt, ico, componentView, tip);
                        int index = rhsTabbedPane.indexOfComponent(componentView);
                        rhsTabbedPane.setClosableTab(index, true);
                        rhsTabbedPane.setSelectedIndex(index);
                        return componentView;
                    }
                } finally {
                    if (setDivider) {
                        int rootWidth = rhsSplitPane.getWidth();
                        int tabWidth = rhsTabbedPane.getPreferredSize().width;
                        rhsSplitPane.setDividerLocation(rootWidth - tabWidth);
                    }
                }
                break;
            case BOOKMARK:
                bookmarkView.putClientProperty(BOOKMARK_VISIBLE, Boolean.TRUE);
                if (rhsSplitPane.getRightComponent() == null) {
                    rhsSplitPane.setRightComponent(rhsTabbedPane);
                    Integer dividerLocation = (Integer) rhsSplitPane.getClientProperty(DIVIDER_LOCATION);
                    if (dividerLocation != null) {
                        rhsSplitPane.setDividerLocation(dividerLocation.intValue());
                    } else {
                        setDivider = true;
                    }
                }
                try {
                    if (inTab) {
                        rhsTabbedPane.setSelectedComponent(component);
                    } else {
                        ico = SwingUtilities.loadIconResource("bookmark", 16);
                        tip = messages.format("jdbcworkbench.bookmark_tab.tip");
                        componentView = layoutBookmarkContainer(eventManager);
                        rhsTabbedPane.addTab(txt, ico, componentView, tip);
                        int index = rhsTabbedPane.indexOfComponent(componentView);
                        rhsTabbedPane.setClosableTab(index, true);
                        rhsTabbedPane.setSelectedIndex(index);
                        return componentView;
                    }
                } finally {
                    if (setDivider) {
                        int rootWidth = rhsSplitPane.getWidth();
                        int tabWidth = rhsTabbedPane.getPreferredSize().width;
                        rhsSplitPane.setDividerLocation(rootWidth - tabWidth);
                    }
                }
                break;
            case SCHEMA:
                schemaView.putClientProperty(SCHEMA_VISIBLE, Boolean.TRUE);
                if (lhsSplitPane.getLeftComponent() == null) {
                    lhsSplitPane.setLeftComponent(lhsTabbedPane);
                    setDivider = true;
                }
                try {
                    if (inTab) {
                        lhsTabbedPane.setSelectedComponent(component);
                    } else {
                        ico = SwingUtilities.loadIconResource("database", 16);
                        tip = messages.format("jdbcworkbench.schema_tab.tip");
                        componentView = layoutSchemaContainer(eventManager);
                        lhsTabbedPane.addTab(txt, ico, componentView, tip);
                        int index = lhsTabbedPane.indexOfComponent(componentView);
                        lhsTabbedPane.setClosableTab(index, true);
                        lhsTabbedPane.setSelectedIndex(index);
                        return componentView;
                    }
                } finally {
                    if (setDivider) {
                        int tabWidth = lhsTabbedPane.getPreferredSize().width;
                        lhsSplitPane.setDividerLocation(tabWidth);
                    }
                }
                break;
        }
        return component;
    }

    private void handleBookmarkAction(int action, Object selected) {
        BookmarkTreeModel model = (BookmarkTreeModel) bookmarkView.getModel();
        BookmarkFolder rootFolder = (BookmarkFolder) model.getRoot();
        BookmarkFolder parentFolder = null;
        OperationStatus status = OperationStatus.UNKNOWN;
        EmbeddedDatabase database = EmbeddedDatabase.getSharedInstance();
        switch(action) {
            case SharedActions.DELETE_BOOKMARK_SELECTION:
                Window owner = javax.swing.SwingUtilities.getWindowAncestor(bookmarkView);
                if (selected instanceof BookmarkReference) {
                    BookmarkReference bookmark = (BookmarkReference) selected;
                    parentFolder = rootFolder.findChildPath(bookmark.getPath());
                    parentFolder = (parentFolder == null) ? rootFolder : parentFolder;
                    String msg = messages.format("jdbcworkbench.deletebookmark_confirm", bookmark.getName());
                    int result = JOptionPane.NO_OPTION;
                    int opt = JOptionPane.YES_NO_OPTION;
                    int type = JOptionPane.QUESTION_MESSAGE;
                    result = JOptionPane.showConfirmDialog(owner, msg, "", opt, type);
                    if (result == JOptionPane.YES_OPTION) {
                        try {
                            database.removeBookmark(bookmark);
                            model.removeBookmark(bookmark);
                        } catch (SQLException e) {
                            error("", e);
                        }
                    }
                } else if (selected instanceof BookmarkFolder) {
                    BookmarkFolder folder = (BookmarkFolder) selected;
                    String msg = messages.format("jdbcworkbench.deletebookmarkfolder_confirm", folder.getName());
                    int result = JOptionPane.NO_OPTION;
                    int opt = JOptionPane.YES_NO_OPTION;
                    int type = JOptionPane.QUESTION_MESSAGE;
                    result = JOptionPane.showConfirmDialog(owner, msg, "", opt, type);
                    if (result == JOptionPane.YES_OPTION) {
                        try {
                            database.removeBookmarkFolder(folder);
                            model.removeBookmarkFolder(folder);
                        } catch (SQLException e) {
                            error("", e);
                        }
                    }
                }
                break;
            case SharedActions.CREATE_NEW_BOOKMARK:
                if (selected instanceof BookmarkReference) {
                    BookmarkReference bookmark = (BookmarkReference) selected;
                    parentFolder = rootFolder.findChildPath(bookmark.getPath());
                    parentFolder = (parentFolder == null) ? rootFolder : parentFolder;
                } else {
                    parentFolder = selected == null ? rootFolder : (BookmarkFolder) selected;
                }
                Bookmark bookmark = new Bookmark();
                bookmark.setCreationTime(new Date());
                bookmark.setLastAccess(new Date());
                bookmark.setFolder(parentFolder);
                status = showModalEditor(BookmarkEditor.class, bookmark);
                if (status == OperationStatus.MODIFIED) {
                    try {
                        BookmarkReference reference = database.addBookmark(bookmark);
                        model.addBookmark(reference, parentFolder);
                    } catch (SQLException e) {
                        error("", e);
                    }
                }
                break;
            case SharedActions.EDIT_BOOKMARK:
                if (selected instanceof BookmarkReference) {
                    BookmarkReference reference = (BookmarkReference) selected;
                    try {
                        bookmark = database.getBookmark(reference);
                        status = showModalEditor(BookmarkEditor.class, bookmark);
                        if (status == OperationStatus.MODIFIED) {
                            database.updateBookmark(bookmark);
                            parentFolder = rootFolder.findChildPath(bookmark.getPath());
                            parentFolder = (parentFolder == null) ? rootFolder : parentFolder;
                            reference.setFavorite(bookmark.isFavorite());
                            reference.setName(bookmark.getName());
                            reference.setColorLabel(bookmark.getColorLabel());
                            model.reload(parentFolder, reference);
                        }
                    } catch (SQLException e) {
                        error("", e);
                    }
                }
                break;
            case SharedActions.ADD_BOOKMARK_FOLDER:
                String promptText = messages.getMessage("jdbcworkbench.newbookmarkfolder-prompt");
                String defaultValue = messages.getMessage("jdbcworkbench.newbookmarkfolder-default");
                String folderName = JOptionPane.showInputDialog(bookmarkView, promptText, defaultValue);
                if (folderName == null || folderName.trim().length() == 0) {
                    return;
                }
                if (selected instanceof BookmarkFolder) {
                    parentFolder = (BookmarkFolder) selected;
                } else {
                    parentFolder = rootFolder;
                }
                model.addBookmarkFolder(parentFolder, folderName);
                break;
            default:
                break;
        }
    }

    private void restoreZoom(Component componentSource) {
        JSplitPane splitPane = null;
        Integer dividerLocation = null;
        if (componentSource == rhsTabbedPane) {
            dividerLocation = (Integer) rhsSplitPane.getClientProperty(DIVIDER_LOCATION);
            rhsSplitPane.setRightComponent(rhsTabbedPane);
            if (dividerLocation == null) {
                int rootWidth = rhsSplitPane.getWidth();
                int tabWidth = rhsTabbedPane.getPreferredSize().width;
                dividerLocation = new Integer(rootWidth - tabWidth);
            }
            splitPane = rhsSplitPane;
        } else if (componentSource == centerTabbedPane) {
            dividerLocation = (Integer) centerSplitPane.getClientProperty(DIVIDER_LOCATION);
            centerSplitPane.setBottomComponent(centerTabbedPane);
            splitPane = centerSplitPane;
        } else if (componentSource == lhsTabbedPane) {
            dividerLocation = (Integer) lhsSplitPane.getClientProperty(DIVIDER_LOCATION);
            lhsSplitPane.setLeftComponent(lhsTabbedPane);
            if (dividerLocation == null) {
                dividerLocation = new Integer(lhsTabbedPane.getPreferredSize().width);
            }
            splitPane = rhsSplitPane;
        }
        if (splitPane != null) {
            if (dividerLocation != null) {
                splitPane.setDividerLocation(dividerLocation.intValue());
            }
            splitPane.invalidate();
        }
    }

    private String buildCatalogMenu(Connection connection, ConnectionProfile profile) throws SQLException {
        catalogMenu.removeAll();
        Enumeration<AbstractButton> elements = catalogSelection.getElements();
        while (elements.hasMoreElements()) {
            catalogSelection.remove(elements.nextElement());
        }
        ResultSet set = connection.getMetaData().getCatalogs();
        String preferredCatalog = profile.getPreferredCatalog();
        if (preferredCatalog == null || preferredCatalog.trim().length() == 0) {
            preferredCatalog = connection.getCatalog();
        }
        boolean catalogFound = false;
        String firstAvailableCatalog = null;
        try {
            while (set.next()) {
                String catalog = set.getString("TABLE_CAT");
                if (!catalogFound && preferredCatalog.equalsIgnoreCase(catalog)) {
                    catalogFound = true;
                }
                if (firstAvailableCatalog == null) {
                    firstAvailableCatalog = catalog;
                }
                CustomAction action = new CustomAction(catalog, SharedActions.CATALOG_CHANGED, eventManager);
                action.putValue(Action.DEFAULT, catalog);
                action.putValue(CustomAction.ICON_NAME, "catalog");
                JCheckBoxMenuItem button = new JCheckBoxMenuItem(action);
                configureMenuItem(button, action);
                catalogSelection.add(button);
                if (catalog.equals(preferredCatalog)) {
                    catalogSelection.setSelected(button.getModel(), true);
                }
                catalogMenu.add(button);
            }
        } finally {
            try {
                set.close();
            } catch (Throwable ignored) {
            }
        }
        if (!catalogFound) {
            warn(messages.format("jdbcworkbench.catalognotfoundwarning", preferredCatalog));
            preferredCatalog = firstAvailableCatalog;
            Enumeration<AbstractButton> selections = catalogSelection.getElements();
            while (selections.hasMoreElements()) {
                AbstractButton button = selections.nextElement();
                String otherCatalog = button.getName();
                if (otherCatalog.equalsIgnoreCase(preferredCatalog)) {
                    catalogSelection.setSelected(button.getModel(), true);
                    break;
                }
            }
        }
        if (catalogSelection.getButtonCount() == 0) {
            JMenuItem disabledOption = new JMenuItem();
            disabledOption.setEnabled(false);
            disabledOption.setText(messages.format("JdbcWorkbench.no_catalogs"));
            disabledOption.setIcon(SwingUtilities.loadIconResource("catalog", 16));
            catalogMenu.add(disabledOption);
        }
        return preferredCatalog;
    }

    private void closeSelectedResult() {
        EnhancedTabbedPane tabbedPane = tabRenderer.getTabbedPane();
        int index = centerTabbedPane.getSelectedIndex();
        if (index >= 0) {
            tabbedPane.remove(index);
        }
    }

    private void navigateRightTab() {
        int index = centerTabbedPane.getSelectedIndex();
        if (index >= 0) {
            if ((index + 1) < centerTabbedPane.getTabCount()) {
                centerTabbedPane.setSelectedIndex(index + 1);
            } else {
                centerTabbedPane.setSelectedIndex(0);
            }
        }
    }

    private void navigateLeftTab() {
        int index = centerTabbedPane.getSelectedIndex();
        if (index >= 0) {
            if ((index - 1) >= 0) {
                centerTabbedPane.setSelectedIndex(index - 1);
            } else {
                index = centerTabbedPane.getTabCount();
                centerTabbedPane.setSelectedIndex(index - 1);
            }
        }
    }

    private void clearAllHistory() {
        EmbeddedDatabase edb = EmbeddedDatabase.getSharedInstance();
        try {
            int removedCount = edb.removeAllHistory();
            debug(messages.format("JdbcWorkbench.removed_history_items", Integer.toString(removedCount)));
        } catch (SQLException error) {
            error(messages.format("JdbcWorkbench.clear_history_failed"), error);
        }
        reloadQueryHistory();
    }

    private void queryDatabaseMetadata() {
        int index;
        DataGrid dataGrid = new DataGrid();
        DatabaseMetaData metaData;
        try {
            metaData = currentService.getConnection().getMetaData();
        } catch (SQLException error) {
            error("", error);
            return;
        }
        JPanel view = new JPanel();
        dataGrid.doLayout(view, null, null);
        Map<String, Object> metaDataMap = JdbcUtilities.extractMetadata(metaData);
        EnhancedTableModel model = new EnhancedTableModel(new String[] { "Property", "Value" });
        Set<Map.Entry<String, Object>> entries = metaDataMap.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            ArrayList<Object> row = new ArrayList<Object>();
            row.add(entry.getKey());
            row.add(entry.getValue());
            model.addRow(row);
        }
        dataGrid.setModel(model);
        EnhancedTabbedPane tabbedPane = tabRenderer.getTabbedPane();
        tabbedPane.addTab(messages.format("JdbcWorkbench.database_metadata"), view);
        index = tabbedPane.indexOfComponent(view);
        tabbedPane.setClosableTab(index, true);
        tabbedPane.setIconAt(index, SwingUtilities.loadIconResource("information", 16));
    }

    private boolean checkConnection() {
        try {
            if (currentService == null || currentService.getConnection().isClosed()) {
                return promptForService();
            }
            return true;
        } catch (SQLException ignored) {
            debug("", ignored);
        }
        return false;
    }

    private void updateSyntaxHighlighting() {
        try {
            String catalog = schemaModel.getCurrentCatlog();
            String schema = schemaModel.getCurrentSchema();
            commandEditor.updateServiceKeywords(currentService, catalog, schema);
        } catch (SQLException ignored) {
            debug("", ignored);
        }
    }

    private void configureSplitPanel(JSplitPane splitPanel, String preference) {
        int location = preferences.getInt(preference, 0);
        if (location == 0) {
            splitPanel.setDividerLocation(0.50d);
        } else {
            splitPanel.setDividerLocation(location);
            splitPanel.putClientProperty(DIVIDER_LOCATION, new Integer(splitPanel.getDividerLocation()));
        }
    }

    private static class ToggleViewAction extends AbstractAction {

        private static final long serialVersionUID = -1587118235512663612L;

        private WorkbenchView view = null;

        private JdbcWorkbench workbench = null;

        private Component viewComponent = null;

        public ToggleViewAction(WorkbenchView view, JdbcWorkbench workbench) {
            super();
            this.view = view;
            this.workbench = workbench;
        }

        public void actionPerformed(ActionEvent e) {
            viewComponent = workbench.toggleView(view, viewComponent);
        }
    }
}
