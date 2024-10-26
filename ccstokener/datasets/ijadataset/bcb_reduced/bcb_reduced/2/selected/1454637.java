package scamsoft.squadleader.client;

import scamsoft.squadleader.classloaders.CoreMediaClassLoader;
import scamsoft.squadleader.rules.Game;
import scamsoft.squadleader.rules.Player;
import scamsoft.squadleader.rules.SquadleaderException;
import scamsoft.squadleader.rules.modules.basic.Persistence;
import scamsoft.squadleader.rules.persistence.GameBuilder;
import scamsoft.squadleader.rules.persistence.PersistenceRegistry;
import scamsoft.squadleader.server.*;
import scamsoft.squadleader.setup.Scenario;
import scamsoft.util.Toolkit;
import javax.help.CSH;
import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.ExportException;
import java.util.*;
import java.util.List;
import nu.xom.*;

/**
 * Provides System level services
 * i.e. Saving and Loading games, network links
 *
 * @author Andreas Mross
 *         created    February 10, 2001
 */
public class GameManager extends JFrame {

    private Collection gamesMasters;

    private JButton newGameButton;

    private JButton loadGameButton;

    private JButton joinGameButton;

    private JTextField ipaddress = new JTextField();

    private SLConfig config;

    private ComponentListener componentlistener;

    private JButton exitProgramButton;

    private HelpBroker helpBroker;

    private PersistenceRegistry registry;

    public GameManager() {
        gamesMasters = new ArrayList();
        initialiseHelp();
        initialiseRegistry();
        try {
            LocateRegistry.createRegistry(1099);
        } catch (ExportException e) {
            try {
                LocateRegistry.getRegistry();
            } catch (RemoteException f) {
                f.printStackTrace();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        jbInit();
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        componentlistener = new ComponentAdapter() {

            public void componentResized(ComponentEvent e) {
                config.saveWindowPosition(e.getComponent());
            }

            public void componentMoved(ComponentEvent e) {
                config.saveWindowPosition(e.getComponent());
            }
        };
        this.setTitle("Squad Leader");
        this.setResizable(false);
        try {
            Image icon = scamsoft.util.Toolkit.getImageFromURL(CoreMediaClassLoader.class.getClassLoader().getResource("iconcolour1.gif"));
            this.setIconImage(icon);
        } catch (IOException e) {
            e.printStackTrace();
        }
        config = new SLConfig();
        this.addWindowListener(new WindowAdapter() {

            public void windowClosed(WindowEvent e) {
                if (gamesMasters.isEmpty()) {
                    exitProgram();
                }
            }
        });
    }

    private void initialiseRegistry() {
        new Runnable() {

            public void run() {
                registry = new PersistenceRegistry();
                Persistence.registerObjects(registry);
            }
        }.run();
    }

    private void exitProgram() {
        if (!Toolkit.isDebuggingEnabled()) {
            int sure = JOptionPane.showConfirmDialog(this, "Are you sure?", "Exit Game", JOptionPane.OK_CANCEL_OPTION);
            if (sure != JOptionPane.OK_OPTION) return;
        }
        Collection temp = new ArrayList(gamesMasters);
        for (Iterator e = temp.iterator(); e.hasNext(); ) {
            GamesMasterRemote gm = (GamesMasterRemote) e.next();
            gm.exitGame();
        }
        System.exit(0);
    }

    public SLConfig getConfiguration() {
        return config;
    }

    /**
     * Starts a server with the given scenario
     *
     * @param game
     */
    public GameServerRemote setupServer(Game game) throws RemoteException {
        GameServerRemote result = new GameServer(game);
        GameBroker gameBroker = GameBrokerImplementation.getGameBroker();
        gameBroker.addGame(result);
        return result;
    }

    /**
     * Gets the free player from the local server and gets the player to choose one.
     *
     * @return The chosen player. Returns Observer if there are no free slots available. Returns null if the player cancelled.
     * @throws RemoteException If an error occured trying to get the list of free Players from
     *                         the server.
     */
    private Player choosePlayer(GameServerRemote server) throws RemoteException {
        List freeplayers = server.getFreePlayers();
        freeplayers.add(Player.observer);
        Player result = null;
        if (freeplayers.size() > 1) {
            int chosenplayernumber = JOptionPane.showOptionDialog(this, "Choose a player", "Join Game", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, freeplayers.toArray(), freeplayers.get(0));
            if (chosenplayernumber != JOptionPane.CLOSED_OPTION) {
                result = (Player) freeplayers.get(chosenplayernumber);
            }
        } else {
            result = Player.observer;
        }
        return result;
    }

    /**
     * Setup a Client from a remote server
     * @param server
     * @param chosenplayer
     * @return
     */
    public Mediator setupClient(final GameServerRemote server, final Player chosenplayer) {
        if (chosenplayer == null) throw new NullPointerException("player");
        if (server == null) throw new NullPointerException("game");
        final GameManager thisGameManager = this;
        final GameServerRemote remoteserver = server;
        Mediator result = null;
        try {
            final Mediator mediator = new Mediator(thisGameManager, remoteserver, chosenplayer);
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    try {
                        RemoteClient client = mediator.getGamesMaster();
                        Key key = remoteserver.joinGame(chosenplayer, client);
                        mediator.getGamesMaster().setKey(key);
                    } catch (RemoteException e) {
                        handleNetworkException(e);
                        return;
                    } catch (SquadleaderException e) {
                        handleNetworkException(e);
                        return;
                    }
                    gamesMasters.add(mediator.getGamesMaster());
                    JFrame newGui = mediator.getFrame();
                    newGui.addComponentListener(componentlistener);
                    config.restoreWindowPosition(newGui);
                    newGui.setVisible(true);
                    mediator.getZoomHandler().setFullView();
                    System.out.println("New client created; free memory=" + Runtime.getRuntime().freeMemory() / 1000 + "kb");
                }
            });
            result = mediator;
        } catch (RemoteException e) {
            handleNetworkException(e);
        }
        return result;
    }

    private void newGameNew() {
        List scenarios = getScenarioChoices();
        ScenarioChoice scenarioChoice = (ScenarioChoice) JOptionPane.showInputDialog(this, "Choose a Scenario", "Choose a Scenario", JOptionPane.PLAIN_MESSAGE, null, scenarios.toArray(), scenarios.get(0));
        if (scenarioChoice == null) {
            return;
        }
        Object obj;
        this.getContentPane().setCursor(new Cursor(Cursor.WAIT_CURSOR));
        try {
            obj = scamsoft.util.Toolkit.loadClass(scenarioChoice.className, null);
        } catch (ClassNotFoundException e) {
            handleNetworkException(e);
            return;
        } catch (InstantiationException e) {
            handleNetworkException(e);
            return;
        } catch (InvocationTargetException e) {
            handleNetworkException(e);
            return;
        } catch (IllegalArgumentException e) {
            handleNetworkException(e);
            return;
        } catch (IllegalAccessException e) {
            handleNetworkException(e);
            return;
        } finally {
            this.getContentPane().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
        if (obj == null || !(obj instanceof Scenario)) {
            return;
        }
        Scenario scenario = (Scenario) obj;
        String[] variations = scenario.getVariations();
        int chosenvariation = 0;
        if (variations.length > 1) {
            chosenvariation = JOptionPane.showOptionDialog(this, "Choose a Variation", "Choose Variation", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, variations, variations[0]);
            if (chosenvariation == JOptionPane.CLOSED_OPTION) {
                return;
            }
        }
        scenario.setVariation(chosenvariation);
        Game game = new Game(scenario);
        GameServerRemote remoteGame;
        try {
            remoteGame = setupServer(game);
        } catch (RemoteException e) {
            handleNetworkException(e);
            return;
        }
        Player chosenPlayer;
        try {
            chosenPlayer = choosePlayer(remoteGame);
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        }
        setupClient(remoteGame, chosenPlayer);
    }

    private void newGame() {
        List scenarios = getScenarioChoices();
        ScenarioChoice scenarioChoice = (ScenarioChoice) JOptionPane.showInputDialog(this, "Choose a Scenario", "Choose a Scenario", JOptionPane.PLAIN_MESSAGE, null, scenarios.toArray(), scenarios.get(0));
        if (scenarioChoice == null) {
            return;
        }
        Object obj;
        try {
            obj = scamsoft.util.Toolkit.loadClass(scenarioChoice.className, null);
        } catch (ClassNotFoundException e) {
            handleNetworkException(e);
            return;
        } catch (InstantiationException e) {
            handleNetworkException(e);
            return;
        } catch (InvocationTargetException e) {
            handleNetworkException(e);
            return;
        } catch (IllegalArgumentException e) {
            handleNetworkException(e);
            return;
        } catch (IllegalAccessException e) {
            handleNetworkException(e);
            return;
        }
        if (obj == null || !(obj instanceof Scenario)) {
            return;
        }
        Scenario scenario = (Scenario) obj;
        String[] variations = scenario.getVariations();
        int chosenvariation = 0;
        if (variations.length > 1) {
            String[] variationsChoice = new String[variations.length + 1];
            System.arraycopy(variations, 0, variationsChoice, 0, variations.length);
            variationsChoice[variationsChoice.length - 1] = "Cancel";
            chosenvariation = JOptionPane.showOptionDialog(this, "Choose a Variation", "Choose Variation", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, variationsChoice, variationsChoice[0]);
            if (chosenvariation == JOptionPane.CLOSED_OPTION || chosenvariation == variationsChoice.length - 1) {
                return;
            }
        }
        String path = scenarioChoice.filePrefix + chosenvariation + ".sav";
        URL url = this.getClass().getClassLoader().getResource(path);
        try {
            loadGame(url.openStream());
        } catch (IOException e) {
            handleNetworkException(e);
        }
    }

    private List getScenarioChoices() {
        List result = new ArrayList();
        result.add(new ScenarioChoice("Training 1. Forced March", "scamsoft.squadleader.setup.ScenarioAlpha", "setups/ScenarioAlpha"));
        result.add(new ScenarioChoice("Training 2. Hasty Assault", "scamsoft.squadleader.setup.ScenarioBeta", "setups/ScenarioBeta"));
        result.add(new ScenarioChoice("Training 3. Through the Gauntlet", "scamsoft.squadleader.setup.ScenarioGamma", "setups/ScenarioGamma"));
        result.add(new ScenarioChoice("Training 4. Back to the Sea", "scamsoft.squadleader.setup.ScenarioDelta", "setups/ScenarioDelta"));
        result.add(new ScenarioChoice("Mission 1. The Guards Counterattack", "scamsoft.squadleader.setup.Scenario1", "setups/guards"));
        result.add(new ScenarioChoice("Mission 2. The Tractor Works", "scamsoft.squadleader.setup.Scenario2", "setups/tractorworks"));
        result.add(new ScenarioChoice("Mission 3. Hedgehog of Piepske", "scamsoft.squadleader.setup.Scenario4", "setups/hedgehog"));
        return result;
    }

    private void loadGame() {
        JFileChooser chooser = new JFileChooser(config.getSaveGameDirectory());
        chooser.setFileFilter(new FileFilter() {

            public boolean accept(File f) {
                return f.getName().endsWith(".sav") || f.isDirectory();
            }

            public String getDescription() {
                return "Squad Leader Saved Games";
            }
        });
        JFrame topwindow = getTopLevelWindow();
        int returnVal = chooser.showOpenDialog(topwindow);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        config.setSaveGameDirectory(chooser.getCurrentDirectory().getPath());
        topwindow.getContentPane().setCursor(new Cursor(Cursor.WAIT_CURSOR));
        topwindow.repaint();
        try {
            loadGame(new FileInputStream(chooser.getSelectedFile()));
        } catch (FileNotFoundException e) {
            handleNetworkException(e);
        } finally {
            this.getContentPane().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Setup a Client from an existing Game.
     * Creates a server and asks which Player they want to be
     * @param game
     */
    public void setupClient(Game game) {
        if (game == null) throw new NullPointerException("game");
        GameServerRemote remoteGame;
        try {
            remoteGame = setupServer(game);
        } catch (RemoteException e) {
            handleNetworkException(e);
            return;
        }
        Player chosenPlayer;
        try {
            chosenPlayer = choosePlayer(remoteGame);
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        }
        if (chosenPlayer != null) setupClient(remoteGame, chosenPlayer);
    }

    /**
     * Load a previously saved game
     */
    private void loadGame(InputStream inputStream) {
        Game game;
        try {
            GameBuilder gameBuilder = new GameBuilder(registry);
            Builder builder = new Builder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            Document xmldoc = builder.build(reader);
            reader.close();
            Element root = xmldoc.getRootElement();
            game = gameBuilder.fromXML(root);
        } catch (IOException e) {
            handleNetworkException(e);
            return;
        } catch (ValidityException e) {
            handleNetworkException(e);
            return;
        } catch (ParsingException e) {
            handleNetworkException(e);
            return;
        }
        setupClient(game);
    }

    /**
     * Join a current game
     */
    private void joinGame() throws RemoteException {
        JFrame topwindow = this;
        topwindow.getContentPane().setCursor(new Cursor(Cursor.WAIT_CURSOR));
        String serveraddress = ipaddress.getText();
        GameBroker gameBroker;
        try {
            Object test = Naming.lookup("rmi://" + serveraddress.trim() + "/" + GameBroker.RMI_NAME);
            gameBroker = (GameBroker) test;
        } catch (NotBoundException e) {
            handleNetworkException(e);
            return;
        } catch (MalformedURLException e) {
            handleNetworkException(e);
            return;
        } finally {
            this.getContentPane().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
        List games = gameBroker.getAvailableGames();
        if (games.isEmpty()) {
            JOptionPane.showConfirmDialog(this, "There are no games currently running on that machine");
            return;
        }
        int chosenGame = 0;
        if (games.size() > 1) {
            String[] gameChoices = new String[games.size()];
            int i = 0;
            for (Iterator e = games.iterator(); e.hasNext(); i++) {
                GameServerRemote nextGame = (GameServerRemote) e.next();
                int freePlayers = nextGame.getFreePlayers().size();
                String startDate = nextGame.getStartDate();
                GameRemote gameStub = nextGame.getGameStub();
                String scenarioName = gameStub.getScenarioName();
                gameChoices[i] = scenarioName + " (" + freePlayers + " free slots) " + startDate;
            }
            chosenGame = JOptionPane.showOptionDialog(this, "Choose a game to join", "Join Game", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, gameChoices, gameChoices[0]);
            if (chosenGame == JOptionPane.CLOSED_OPTION) {
                this.getContentPane().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                return;
            }
        }
        this.getContentPane().setCursor(new Cursor(Cursor.WAIT_CURSOR));
        GameServerRemote remoteserver = (GameServerRemote) games.get(chosenGame);
        Player chosenplayer = choosePlayer(remoteserver);
        if (chosenplayer != null) setupClient(remoteserver, chosenplayer);
        this.getContentPane().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    public final Action JOIN_GAME = new AbstractAction("Join Game") {

        public void actionPerformed(ActionEvent e) {
            try {
                joinGame();
            } catch (RemoteException e1) {
                handleNetworkException(e1);
            }
        }
    };

    private ActionListener VIEW_HELP;

    public ActionListener getViewHelpAction() {
        return VIEW_HELP;
    }

    public final Action NEW_GAME = new AbstractAction("New Game") {

        public void actionPerformed(ActionEvent e) {
            newGame();
        }
    };

    public final Action OPEN_GAME = new AbstractAction("Open Game") {

        public void actionPerformed(ActionEvent e) {
            loadGame();
        }
    };

    public final Action EXIT_PROGRAM = new AbstractAction("Exit Program") {

        public void actionPerformed(ActionEvent e) {
            exitProgram();
        }
    };

    private void jbInit() {
        newGameButton = new JButton(NEW_GAME);
        loadGameButton = new JButton(OPEN_GAME);
        joinGameButton = new JButton(JOIN_GAME);
        exitProgramButton = new JButton(EXIT_PROGRAM);
        newGameButton.setOpaque(false);
        newGameButton.setBackground(Color.magenta);
        newGameButton.setBorderPainted(false);
        loadGameButton.setOpaque(false);
        loadGameButton.setBorderPainted(false);
        joinGameButton.setText("Join Multi-Player Game");
        joinGameButton.setOpaque(false);
        joinGameButton.setBorderPainted(false);
        exitProgramButton.setOpaque(false);
        exitProgramButton.setBorderPainted(false);
        ipaddress.setText("127.0.0.1");
        ipaddress.setOpaque(false);
        ipaddress.setColumns(30);
        ipaddress.requestFocus();
        JPanel ipaddresstext = new JPanel();
        ipaddresstext.setLayout(new GridLayout(0, 1));
        ipaddresstext.add(new JLabel("        Your IP address: (ctrl-c to copy) "));
        ipaddresstext.add(new JLabel("        Your opponent's IP address: "));
        ipaddresstext.setOpaque(false);
        JPanel ipaddressfields = new JPanel();
        ipaddressfields.setLayout(new GridLayout(0, 1));
        ipaddressfields.setOpaque(false);
        String localAddress;
        try {
            localAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            localAddress = "127.0.0.1";
        }
        JTextField ip = new JTextField(localAddress);
        ip.setEditable(false);
        ip.setOpaque(false);
        ip.setColumns(30);
        ipaddressfields.add(ip);
        ipaddressfields.add(ipaddress);
        JPanel iptable = new JPanel(new BorderLayout());
        iptable.add(ipaddresstext, BorderLayout.WEST);
        iptable.add(ipaddressfields, BorderLayout.CENTER);
        JPanel buttonpanel1 = new JPanel(new BorderLayout());
        buttonpanel1.add(newGameButton, BorderLayout.WEST);
        buttonpanel1.setOpaque(false);
        JPanel buttonpanel2 = new JPanel(new BorderLayout());
        buttonpanel2.add(joinGameButton, BorderLayout.WEST);
        buttonpanel2.setOpaque(false);
        JPanel buttonpanel3 = new JPanel(new BorderLayout());
        buttonpanel3.add(loadGameButton, BorderLayout.WEST);
        buttonpanel3.setOpaque(false);
        JPanel buttonpanel4 = new JPanel(new BorderLayout());
        buttonpanel4.add(exitProgramButton, BorderLayout.WEST);
        buttonpanel4.setOpaque(false);
        JPanel gridcontainer = new JPanel();
        gridcontainer.setLayout(new GridLayout(0, 1));
        gridcontainer.add(buttonpanel1);
        gridcontainer.add(buttonpanel2);
        gridcontainer.add(iptable);
        iptable.setOpaque(false);
        gridcontainer.add(buttonpanel3);
        gridcontainer.add(buttonpanel4);
        gridcontainer.setOpaque(false);
        ClassLoader classLoader = CoreMediaClassLoader.class.getClassLoader();
        ImageIcon icon = new ImageIcon(classLoader.getResource("stalingrad75.jpg"));
        JLabel background = new JLabel(icon);
        JPanel backgroundcontainer = new JPanel(new BorderLayout());
        backgroundcontainer.add(background, BorderLayout.NORTH);
        JPanel foreground = new JPanel(new BorderLayout());
        foreground.setOpaque(false);
        foreground.setBackground(Color.blue);
        foreground.add(gridcontainer, BorderLayout.NORTH);
        Container content = this.getContentPane();
        OverlayLayout overlay = new OverlayLayout(content);
        content.setLayout(overlay);
        content.add(foreground);
        content.add(backgroundcontainer);
        this.pack();
    }

    public void handleNetworkException(Exception e) {
        e.printStackTrace();
        this.getContentPane().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        String errormessage;
        if (e instanceof MalformedURLException) {
            errormessage = "Oops! The IP address you have entered is not actually an IP address! An IP address looks like 232.45.22.136";
        } else if (e instanceof java.rmi.UnknownHostException) {
            errormessage = "We could not connect to the other player. Check the IP address";
        } else if (e instanceof NotBoundException) {
            errormessage = "The other player has not started a game yet. Wait, then try again.";
        } else if (e instanceof ValidityException || e instanceof ParsingException) {
            errormessage = "The game you have attempted to load appears to be corrupt";
        } else {
            errormessage = e.getMessage();
        }
        JOptionPane.showMessageDialog(this, errormessage, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void initialiseHelp() {
        String helpHS = "help/IdeHelp.hs";
        ClassLoader classLoader = GameManager.class.getClassLoader();
        HelpSet helpSet;
        try {
            URL hsURL = HelpSet.findHelpSet(classLoader, helpHS);
            helpSet = new HelpSet(null, hsURL);
        } catch (Exception ee) {
            ee.printStackTrace();
            System.out.println("HelpSet " + ee.getMessage());
            System.out.println("HelpSet " + helpHS + " not found");
            return;
        }
        helpBroker = helpSet.createHelpBroker("main");
        VIEW_HELP = new CSH.DisplayHelpFromSource(helpBroker);
    }

    private JFrame getTopLevelWindow() {
        return this;
    }

    public void gameClosed(GamesMasterRemote gamesMaster) {
        gamesMasters.remove(gamesMaster);
        if (gamesMasters.isEmpty() && !this.isVisible()) {
            exitProgram();
        }
    }

    private static class ScenarioChoice {

        public ScenarioChoice(String scenarioName, String className, String filePrefix) {
            this.scenarioName = scenarioName;
            this.className = className;
            this.filePrefix = filePrefix;
        }

        public String toString() {
            return scenarioName;
        }

        String scenarioName;

        String className;

        String filePrefix;
    }
}
