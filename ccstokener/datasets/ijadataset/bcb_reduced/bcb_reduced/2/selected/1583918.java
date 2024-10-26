package org.vizzini.example.kenken.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import org.vizzini.example.kenken.Environment;
import org.vizzini.example.kenken.Injector;
import org.vizzini.example.kenken.PuzzleDescriptor;
import org.vizzini.example.kenken.PuzzleDescriptorReader;
import org.vizzini.example.kenken.PuzzleDescriptorWriter;
import org.vizzini.game.IAgent;
import org.vizzini.game.IAgentCollection;
import org.vizzini.game.IEngine;
import org.vizzini.game.IEnvironment;
import org.vizzini.game.IGame;
import org.vizzini.game.ITeam;
import org.vizzini.game.IntegerPosition;
import org.vizzini.game.action.RedoAction;
import org.vizzini.game.action.UndoAction;
import org.vizzini.game.boardgame.action.BoardGameActionFactory;
import org.vizzini.game.event.GameEvent;
import org.vizzini.ui.ActionFactory;
import org.vizzini.ui.ActionManager;
import org.vizzini.ui.Alert;
import org.vizzini.ui.ApplicationSupport;
import org.vizzini.ui.StatusBar;
import org.vizzini.ui.ToggleMenuItem;
import org.vizzini.ui.game.IEnvironmentUI;
import org.vizzini.ui.game.boardgame.AbstractBoardgameUISwing;
import org.vizzini.util.DateUtilities;
import org.vizzini.util.IProvider;
import org.vizzini.util.event.StatusEvent;
import org.vizzini.util.event.StatusManager;

/**
 * Provides a KenKen assistant application.
 *
 * @author   Jeffrey M. Thompson
 * @version  v0.4
 * @since    v0.4
 */
public final class KenKenAssistant extends AbstractBoardgameUISwing {

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /** Logger. */
    private static final Logger LOGGER = Logger.getLogger(KenKenAssistant.class.getName());

    /** Status bar position for date time. */
    private static final int DATE_TIME_POSITION = 1;

    /** Date utilities singleton. */
    static final DateUtilities DATE_UTILS = new DateUtilities();

    /** Action factory. */
    private BoardGameActionFactory _actionFactory;

    /** Paused start time. */
    private long _pausedStartTime;

    /** Accumulated time the puzzle was paused. */
    private long _pausedTime;

    /** Puzzle descriptor. */
    private PuzzleDescriptor _puzzleDescriptor;

    /** Timer. */
    private Timer _timer;

    /**
     * Construct this object with the given parameter.
     *
     * @param  game              Game.
     * @param  environmentUI     Environment user interface.
     * @param  agentProviders    List of agent providers.
     * @param  initialDimension  Initial dimension of the GUI.
     *
     * @since  v0.4
     */
    public KenKenAssistant(IGame game, IEnvironmentUI environmentUI, List<IProvider<IAgent>> agentProviders, Dimension initialDimension) {
        super(game, environmentUI, agentProviders, initialDimension);
        setCenteredOnScreen(true);
        setConcedeAvailable(false);
        setFileExtension("kka");
        IntegerPosition.setDimensions(9, 9, 1);
    }

    /**
     * Application method.
     *
     * @param  args  Application arguments.
     *
     * @since  v0.4
     */
    public static void main(String[] args) {
        Injector injector = new Injector();
        KenKenAssistant applet = new KenKenAssistant(injector.injectGame(), null, null, null);
        applet.doMain(args);
    }

    /**
     * Perform the clear notes action.
     *
     * @param  event  Event.
     *
     * @since  v0.4
     */
    public void clearNotesActionPerformed(ActionEvent event) {
        setCursorBusy(true);
        EnvironmentUISwing environmentUI = (EnvironmentUISwing) getEnvironmentUI();
        environmentUI.clearAllNotes();
        setCursorBusy(false);
    }

    /**
     * Perform the clear values action.
     *
     * @param  event  Event.
     *
     * @since  v0.4
     */
    public void clearValuesActionPerformed(ActionEvent event) {
        setCursorBusy(true);
        IGame game = getGame();
        Environment environment = (Environment) game.getEnvironment();
        environment.clearAllValues();
        setCursorBusy(false);
    }

    /**
     * Start this applet as an application.
     *
     * @param  args  Application arguments.
     *
     * @since  v0.4
     */
    @Override
    public void doMain(String[] args) {
        super.doMain(args);
        if ((args != null) && (args.length > 0)) {
            String filename = args[0];
            File file = new File(filename);
            openFile(file);
        }
    }

    /**
     * Perform the fill values action.
     *
     * @param  event  Event.
     *
     * @since  v0.4
     */
    public void fillValuesActionPerformed(ActionEvent event) {
        setCursorBusy(true);
        IGame game = getGame();
        Environment environment = (Environment) game.getEnvironment();
        environment.fillValues();
        setCursorBusy(false);
    }

    /**
     * Respond to game events.
     *
     * @param  event  Game event.
     *
     * @since  v0.4
     */
    @Override
    public void gameChange(GameEvent event) {
        setAllTokensLocked(true);
        EnvironmentUISwing environmentUI = (EnvironmentUISwing) getEnvironmentUI();
        environmentUI.setAllNotesVisible(false);
        environmentUI.setAllLocksVisible(false);
        environmentUI.setAllClearsVisible(false);
        stopTimer();
        long elapsedTime = getElapsedTime();
        String elapsedTimeString = DATE_UTILS.getElapsedTimeString(elapsedTime);
        String message = ApplicationSupport.getResource("STRING_agentWon", new String[] { elapsedTimeString });
        if (isAudioOn()) {
            getGameWinnerAudioClip().play();
        }
        JOptionPane.showMessageDialog(this, message);
        _pausedStartTime = 0L;
        _pausedTime = 0L;
        checkActions();
    }

    /**
     * Perform the hide notes action.
     *
     * @param  event  Event.
     *
     * @since  v0.4
     */
    public void hideNotesActionPerformed(ActionEvent event) {
        setCursorBusy(true);
        EnvironmentUISwing environmentUI = (EnvironmentUISwing) getEnvironmentUI();
        environmentUI.setAllNotesVisible(false);
        setCursorBusy(false);
    }

    /**
     * Perform the lock all cells action.
     *
     * @param  event  Event.
     *
     * @since  v0.4
     */
    public void lockAllCellsActionPerformed(ActionEvent event) {
        setCursorBusy(true);
        setAllTokensLocked(true);
        setCursorBusy(false);
    }

    /**
     * @see  org.vizzini.ui.game.AbstractGameUISwing#newActionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void newActionPerformed(ActionEvent event) {
        NewDialog dialog = new NewDialog(getFrame(), getAppName());
        dialog.getDialog().setVisible(true);
        if (dialog.getResult() == JOptionPane.OK_OPTION) {
            _puzzleDescriptor = dialog.getPuzzleDescriptor();
            EnvironmentUISwing environmentUI = (EnvironmentUISwing) getEnvironmentUI();
            try {
                environmentUI.configure(_puzzleDescriptor);
            } catch (InstantiationException e) {
                handleException(e);
            } catch (IllegalAccessException e) {
                handleException(e);
            }
            JFrame frame = getFrame();
            frame.validate();
            restartGame();
            getStatusManager().fireStatusChange(ApplicationSupport.getResource("STRING_newGame"));
        }
        checkActions();
    }

    /**
     * @see  org.vizzini.ui.game.AbstractGameUISwing#openActionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void openActionPerformed(ActionEvent event) {
        JFileChooser fileChooser = getFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            setCursorBusy(true);
            File file = fileChooser.getSelectedFile();
            openFile(file);
            setCursorBusy(false);
        }
        checkActions();
    }

    /**
     * Callback for the open URL action.
     *
     * @param  event  Action event.
     *
     * @since  v0.4
     */
    public void openUrlActionPerformed(ActionEvent event) {
        RemoteFileChooser fileChooser = new RemoteFileChooser(this, getAppName());
        fileChooser.getDialog().setVisible(true);
        if (fileChooser.getResult() == JOptionPane.OK_OPTION) {
            setCursorBusy(true);
            URL url = fileChooser.getSelectedUrl();
            String filename = fileChooser.getSelectedFilename();
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                openFile(filename, reader);
            } catch (IOException e) {
                handleException(e);
            }
            setCursorBusy(false);
        }
        checkActions();
    }

    /**
     * Perform the pause action.
     *
     * @param  event  Action event.
     *
     * @since  v0.1
     */
    public void pauseActionPerformed(ActionEvent event) {
        Action action = getPauseAction();
        EnvironmentUISwing environmentUI = (EnvironmentUISwing) getEnvironmentUI();
        if (getGame().getEngine().isPaused()) {
            getGame().getEngine().resume();
            action.putValue(Action.NAME, ApplicationSupport.getResource("STRING_pauseAction"));
            action.putValue(Action.SHORT_DESCRIPTION, ApplicationSupport.getResource("STRING_pauseAction"));
            action.putValue(Action.SMALL_ICON, ApplicationSupport.getIcon("ICON_pause"));
            long now = System.currentTimeMillis();
            _pausedTime += now - _pausedStartTime;
            startTimer();
            environmentUI.setVisible(true);
        } else {
            getGame().getEngine().pause();
            action.putValue(Action.NAME, ApplicationSupport.getResource("STRING_resumeAction"));
            action.putValue(Action.SHORT_DESCRIPTION, ApplicationSupport.getResource("STRING_resumeAction"));
            action.putValue(Action.SMALL_ICON, ApplicationSupport.getIcon("ICON_resume"));
            _pausedStartTime = System.currentTimeMillis();
            stopTimer();
            environmentUI.setVisible(false);
        }
        checkActions();
    }

    /**
     * @see  org.vizzini.ui.game.boardgame.AbstractBoardgameUISwing#redoActionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void redoActionPerformed(ActionEvent event) {
        setCursorBusy(true);
        IGame game = getGame();
        IEnvironment environment = game.getEnvironment();
        RedoAction redoAction = new RedoAction();
        environment.performAction(redoAction);
        setCursorBusy(false);
    }

    /**
     * @see  org.vizzini.ui.game.AbstractGameUISwing#saveAsActionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void saveAsActionPerformed(ActionEvent event) {
        JFileChooser fileChooser = getFileChooser();
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            String suffix = "." + getFileExtension();
            File file = fileChooser.getSelectedFile();
            String filename = file.getAbsolutePath();
            if (!filename.endsWith(suffix)) {
                filename += suffix;
                file = new File(filename);
            }
            if (checkForFileOverwrite(file)) {
                setCursorBusy(true);
                try {
                    FileWriter fileWriter = new FileWriter(file);
                    PuzzleDescriptorWriter writer = new PuzzleDescriptorWriter(fileWriter);
                    writer.write(_puzzleDescriptor);
                    getStatusManager().fireStatusChange(ApplicationSupport.getResource("STRING_gameSaved"));
                } catch (Exception e) {
                    handleException(e);
                }
                setCursorBusy(false);
            }
        }
    }

    /**
     * Perform the show notes action.
     *
     * @param  event  Event.
     *
     * @since  v0.4
     */
    public void showNotesActionPerformed(ActionEvent event) {
        setCursorBusy(true);
        EnvironmentUISwing environmentUI = (EnvironmentUISwing) getEnvironmentUI();
        environmentUI.setAllNotesVisible(true);
        setCursorBusy(false);
    }

    /**
     * @see  org.vizzini.ui.game.boardgame.AbstractBoardgameUISwing#undoActionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void undoActionPerformed(ActionEvent event) {
        setCursorBusy(true);
        IGame game = getGame();
        IEnvironment environment = game.getEnvironment();
        IAgentCollection agentCollection = environment.getAgentCollection();
        IAgent agent = agentCollection.iterator().next();
        BoardGameActionFactory factory = getActionFactory();
        UndoAction undoAction = factory.getUndoAction(agent);
        environment.performAction(undoAction);
        setCursorBusy(false);
    }

    /**
     * Perform the unlock all cells action.
     *
     * @param  event  Event.
     *
     * @since  v0.4
     */
    public void unlockAllCellsActionPerformed(ActionEvent event) {
        setCursorBusy(true);
        setAllTokensLocked(false);
        setCursorBusy(false);
    }

    /**
     * @see  org.vizzini.ui.AbstractApp#checkActions()
     */
    @Override
    protected void checkActions() {
        super.checkActions();
        boolean isEnabled = (_puzzleDescriptor != null);
        ActionFactory.getSaveAction(this).setEnabled(false);
        ActionFactory.getSaveAsAction(this).setEnabled(isEnabled);
        isEnabled = !getGame().getEngine().isGameOver();
        getPauseAction().setEnabled(isEnabled);
    }

    /**
     * @see  org.vizzini.ui.game.AbstractGameUISwing#createStatusBar()
     */
    @Override
    protected StatusBar createStatusBar() {
        return ApplicationSupport.createStatusBar(new int[] { 100, 10 });
    }

    /**
     * @see  org.vizzini.ui.game.AbstractGameUISwing#fillAdditionalEditMenuItems(javax.swing.JMenu)
     */
    @Override
    protected void fillAdditionalEditMenuItems(JMenu editMenu) {
        super.fillAdditionalEditMenuItems(editMenu);
        JMenuItem clearValuesUI = getClearValuesUI();
        JMenuItem clearNotesUI = getClearNotesUI();
        ToggleMenuItem lockAllCellsUI = getLockAllCellsUI();
        lockAllCellsUI.setSelected(false);
        JMenuItem fillValuesUI = getFillValuesUI();
        editMenu.add(clearValuesUI);
        editMenu.add(clearNotesUI);
        editMenu.add(lockAllCellsUI);
        editMenu.add(fillValuesUI);
    }

    /**
     * Fill in additional file menu items. These menu items are placed between
     * the New and Quit menu items.
     *
     * @param  fileMenu  File menu.
     *
     * @since  v0.4
     */
    @Override
    protected void fillAdditionalFileMenuItems(JMenu fileMenu) {
        super.fillAdditionalFileMenuItems(fileMenu);
        fileMenu.add(getOpenUrlAction());
    }

    /**
     * @see  org.vizzini.ui.game.AbstractGameUISwing#fillAdditionalOptionsMenuItems(javax.swing.JMenu)
     */
    @Override
    protected void fillAdditionalOptionsMenuItems(JMenu optionsMenu) {
        optionsMenu.addSeparator();
        optionsMenu.add(getPauseAction());
    }

    /**
     * @see  org.vizzini.ui.game.AbstractGameUISwing#fillAdditionalViewMenuItems(javax.swing.JMenu)
     */
    @Override
    protected void fillAdditionalViewMenuItems(JMenu viewMenu) {
        super.fillAdditionalViewMenuItems(viewMenu);
        ToggleMenuItem showNotesUI = getShowNotesUI();
        showNotesUI.setSelected(true);
        viewMenu.add(showNotesUI);
    }

    /**
     * @see  org.vizzini.ui.game.AbstractGameUISwing#fillToolBar()
     */
    @Override
    protected void fillToolBar() {
        super.fillToolBar();
        JToolBar toolBar = ApplicationSupport.getToolBar();
        toolBar.add(ActionFactory.getOpenAction(this));
        toolBar.add(getOpenUrlAction());
        toolBar.add(ActionFactory.getSaveAsAction(this));
        toolBar.addSeparator();
        toolBar.add(ActionFactory.getUndoAction(this));
        toolBar.add(ActionFactory.getRedoAction(this));
        toolBar.addSeparator();
        toolBar.add(getPauseAction());
    }

    /**
     * @return  the elapsed time for the current puzzle.
     *
     * @since   v0.4
     */
    long getElapsedTime() {
        long answer = 0L;
        IGame game = getGame();
        IEngine engine = game.getEngine();
        Date startTime = engine.getStartTime();
        if (startTime != null) {
            long now = System.currentTimeMillis();
            answer = now - startTime.getTime() - _pausedTime;
        }
        return answer;
    }

    /**
     * @return  the actionFactory
     */
    private BoardGameActionFactory getActionFactory() {
        if (_actionFactory == null) {
            _actionFactory = new BoardGameActionFactory();
        }
        return _actionFactory;
    }

    /**
     * @return  the lock all cells action.
     *
     * @since   v0.4
     */
    private Action getClearNotesAction() {
        return ActionManager.getAction(this, "clearNotes", false, true, true, true);
    }

    /**
     * @return  the lock all cells widget.
     *
     * @since   v0.4
     */
    private JMenuItem getClearNotesUI() {
        Action action = getClearNotesAction();
        return ActionManager.getMenuItem(this, action);
    }

    /**
     * @return  the clear values action.
     *
     * @since   v0.4
     */
    private Action getClearValuesAction() {
        return ActionManager.getAction(this, "clearValues", false, true, true, true);
    }

    /**
     * @return  the clear values widget.
     *
     * @since   v0.4
     */
    private JMenuItem getClearValuesUI() {
        Action action = getClearValuesAction();
        return ActionManager.getMenuItem(this, action);
    }

    /**
     * @return  the fill values action.
     *
     * @since   v0.4
     */
    private Action getFillValuesAction() {
        return ActionManager.getAction(this, "fillValues", false, true, true, true);
    }

    /**
     * @return  the fill values widget.
     *
     * @since   v0.4
     */
    private JMenuItem getFillValuesUI() {
        Action action = getFillValuesAction();
        return ActionManager.getMenuItem(this, action);
    }

    /**
     * @return  the hide notes action.
     *
     * @since   v0.4
     */
    private Action getHideNotesAction() {
        return ActionManager.getAction(this, "hideNotes", false, true, true, true);
    }

    /**
     * @return  the lock all cells action.
     *
     * @since   v0.4
     */
    private Action getLockAllCellsAction() {
        return ActionManager.getAction(this, "lockAllCells", false, true, true, true);
    }

    /**
     * @return  the lock all cells widget.
     *
     * @since   v0.4
     */
    private ToggleMenuItem getLockAllCellsUI() {
        Action actionUnselected = getLockAllCellsAction();
        Action actionSelected = getUnlockAllCellsAction();
        return ActionManager.getToggleMenuItem(this, actionUnselected, actionSelected);
    }

    /**
     * @return  the open URL action.
     *
     * @since   v0.4
     */
    private Action getOpenUrlAction() {
        return ActionManager.getAction(this, "openUrl");
    }

    /**
     * @return  the pause action.
     *
     * @since   v0.4
     */
    private Action getPauseAction() {
        return ActionManager.getAction(this, "pause");
    }

    /**
     * @return  the show notes action.
     *
     * @since   v0.4
     */
    private Action getShowNotesAction() {
        return ActionManager.getAction(this, "showNotes", false, true, true, true);
    }

    /**
     * @return  the show notes widget.
     *
     * @since   v0.4
     */
    private ToggleMenuItem getShowNotesUI() {
        Action actionUnselected = getShowNotesAction();
        Action actionSelected = getHideNotesAction();
        return ActionManager.getToggleMenuItem(this, actionUnselected, actionSelected);
    }

    /**
     * @return  the lock all cells action.
     *
     * @since   v0.4
     */
    private Action getUnlockAllCellsAction() {
        return ActionManager.getAction(this, "unlockAllCells", false, true, true, true);
    }

    /**
     * Handle the given exception.
     *
     * @param  e  Exception.
     *
     * @since  v0.4
     */
    private void handleException(Exception e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
        String prefixMessage = "Exception thrown:";
        Alert.showException(getFrame(), prefixMessage, e);
    }

    /**
     * Open the given file.
     *
     * @param  file  File.
     *
     * @since  v0.4
     */
    private void openFile(File file) {
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(file);
            openFile(file.getName(), fileReader);
        } catch (Exception e) {
            handleException(e);
        } finally {
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    handleException(e);
                }
            }
        }
    }

    /**
     * Open the given file.
     *
     * @param  filename  Filename to display in the frame title bar.
     * @param  reader    Reader.
     *
     * @since  v0.4
     */
    private void openFile(String filename, Reader reader) {
        try {
            PuzzleDescriptorReader puzzleReader = new PuzzleDescriptorReader(reader);
            _puzzleDescriptor = puzzleReader.read();
            ((EnvironmentUISwing) getEnvironmentUI()).configure(_puzzleDescriptor);
            JFrame frame = getFrame();
            frame.validate();
            frame.setTitle(getAppName() + ": " + filename);
            restartGame();
        } catch (Exception e) {
            handleException(e);
        }
    }

    /**
     * Restart the game.
     *
     * @since  v0.4
     */
    private void restartGame() {
        IEnvironment environment = getGame().getEnvironment();
        IEnvironmentUI environmentUI = getEnvironmentUI();
        IAgentCollection agentCollection = environment.getAgentCollection();
        removeAgentListeners(agentCollection.iterator());
        environmentUI.removeAgentListeners(agentCollection.iterator());
        agentCollection.clear();
        String name = "agent";
        ITeam team = environment.getTeamCollection().iterator().next();
        IAgent agent = new Agent();
        agent.setName(name);
        agent.setTeam(team);
        agentCollection.add(agent);
        assignAgentListeners(agentCollection.iterator());
        environmentUI.assignAgentListeners(agentCollection.iterator());
        ApplicationSupport.getStatusBar().clearAll();
        getEnvironmentUI().reset();
        getGame().reset();
        getGame().start();
        startTimer();
        getLockAllCellsUI().setSelected(false);
        getShowNotesUI().setSelected(true);
        checkActions();
    }

    /**
     * Set all tokens locked state.
     *
     * @param  isLocked  Flag indicating whether the tokens are locked.
     *
     * @since  v0.4
     */
    private void setAllTokensLocked(boolean isLocked) {
        IEnvironmentUI environmentUI = getEnvironmentUI();
        Environment environment = (Environment) environmentUI.getEnvironment();
        environment.setAllCellsLocked(isLocked);
    }

    /**
     * Start the timer display.
     *
     * @since  v0.4
     */
    private void startTimer() {
        stopTimer();
        _timer = new Timer();
        final StatusManager statusManager = getStatusManager();
        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                long elapsedTime = getElapsedTime();
                String message = DATE_UTILS.getElapsedTimeString(elapsedTime);
                statusManager.fireStatusChange(DATE_TIME_POSITION, message, StatusEvent.FOREVER_DISPLAY_TIME);
            }
        };
        _timer.scheduleAtFixedRate(task, 0, 250);
    }

    /**
     * Stop the timer display.
     *
     * @since  v0.4
     */
    private void stopTimer() {
        if (_timer != null) {
            _timer.cancel();
            _timer = null;
        }
    }
}
