import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.Stack;
import java.util.Random;
import java.awt.Toolkit;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 * @author achim
 * 
 * This class is the main class for starting the game Dice-Field. 
 * It contains the AWT-frame, menu and all actions.
 *  
 */
public class DiceFieldGame extends AbstractGame implements ActionListener, WindowListener {

    Frame frame = new Frame("Dice-Field");

    /** grid that contains the dice field */
    Panel grid;

    /** Size of the dice field */
    int sizex = 3;

    /** Size of the dice field */
    int sizey = 3;

    Map<DiceField, Integer> eroeffnungen = new HashMap<DiceField, Integer>();

    /** Standard size in pixel of one dice */
    int cubeSize = 64;

    /** The difficulty to play against the computer: 
	 *  The depth of calculation (NegMax-Algorithm) is calculated on basis of this value.  
	 */
    int difficultyLevel = 8;

    /** the images of the dices */
    Image[][] imageCache = new Image[3][7];

    /** the dice field, to show all the dices */
    Dice[][] canvases;

    JProgressBar bar;

    Label playerLabel;

    Label modeLabel;

    Label countLabel;

    Stack<GameNode> undoSpeicher = new Stack<GameNode>();

    List<GameStep> moeglichkeiten = new ArrayList<GameStep>();

    int max = 0;

    ResourceBundle i18;

    static Random r = new Random();

    DiceFieldHelp help = new DiceFieldHelp();

    HourglassCursor waitCursor;

    public int[] player = new int[3];

    public static final int COMPUTER = 1;

    public static final int HUMAN = 2;

    public static final int NETWORK = 3;

    public static final int GELB = 0;

    public static final int ROT = 1;

    public static final int GRUEN = 2;

    public DiceFieldGame() {
        i18 = ResourceBundle.getBundle("DiceField");
        MenuBar mbar = new MenuBar();
        Menu menu1 = new Menu(i18.getString("Game"));
        MenuItem menuitem11 = new MenuItem(i18.getString("New"), new MenuShortcut((int) 'N'));
        menuitem11.setActionCommand("NEW");
        menuitem11.addActionListener(this);
        menu1.add(menuitem11);
        MenuItem menuitem12 = new MenuItem(i18.getString("Hint"), new MenuShortcut((int) 'T'));
        menuitem12.setActionCommand("HINT");
        menuitem12.addActionListener(this);
        menu1.add(menuitem12);
        MenuItem menuitem13 = new MenuItem(i18.getString("Undo"), new MenuShortcut((int) 'U'));
        menuitem13.setActionCommand("UNDO");
        menuitem13.addActionListener(this);
        menu1.add(menuitem13);
        MenuItem menuitem15 = new MenuItem(i18.getString("Human_Computer"), new MenuShortcut((int) 'M'));
        menuitem15.setActionCommand("HUMAN_COMPUTER");
        menuitem15.addActionListener(this);
        menu1.add(menuitem15);
        MenuItem menuitem16 = new MenuItem(i18.getString("Computer_Human"), new MenuShortcut((int) 'C'));
        menuitem16.setActionCommand("COMPUTER_HUMAN");
        menuitem16.addActionListener(this);
        menu1.add(menuitem16);
        MenuItem menuitem17 = new MenuItem(i18.getString("Human_Human"), new MenuShortcut((int) '2'));
        menuitem17.setActionCommand("HUMAN_HUMAN");
        menuitem17.addActionListener(this);
        menu1.add(menuitem17);
        MenuItem menuitem14 = new MenuItem(i18.getString("Computer_Computer"), new MenuShortcut((int) 'S'));
        menuitem14.setActionCommand("COMPUTER_COMPUTER");
        menuitem14.addActionListener(this);
        menu1.add(menuitem14);
        MenuItem menuitem19 = new MenuItem(i18.getString("Quit"), new MenuShortcut((int) 'E'));
        menuitem19.setActionCommand("QUIT");
        menuitem19.addActionListener(this);
        menu1.add(menuitem19);
        mbar.add(menu1);
        Menu menu3 = new Menu(i18.getString("Fieldsize"));
        MenuItem menuitem31 = new MenuItem(i18.getString("3x3"), new MenuShortcut((int) '3'));
        menuitem31.setActionCommand("NEW=3x3");
        menuitem31.addActionListener(this);
        menu3.add(menuitem31);
        MenuItem menuitem32 = new MenuItem(i18.getString("4x3"), new MenuShortcut((int) 'X'));
        menuitem32.setActionCommand("NEW=4x3");
        menuitem32.addActionListener(this);
        menu3.add(menuitem32);
        MenuItem menuitem33 = new MenuItem(i18.getString("4x4"), new MenuShortcut((int) '4'));
        menuitem33.setActionCommand("NEW=4x4");
        menuitem33.addActionListener(this);
        menu3.add(menuitem33);
        MenuItem menuitem34 = new MenuItem(i18.getString("5x5"), new MenuShortcut((int) '5'));
        menuitem34.setActionCommand("NEW=5x5");
        menuitem34.addActionListener(this);
        menu3.add(menuitem34);
        MenuItem menuitem35 = new MenuItem(i18.getString("8x8"), new MenuShortcut((int) '8'));
        menuitem35.setActionCommand("NEW=8x8");
        menuitem35.addActionListener(this);
        menu3.add(menuitem35);
        mbar.add(menu3);
        Menu menu2 = new Menu(i18.getString("Level"));
        MenuItem menuitem21 = new MenuItem(i18.getString("Easy"), new MenuShortcut((int) 'L'));
        menuitem21.setActionCommand("EASY");
        menuitem21.addActionListener(this);
        menu2.add(menuitem21);
        MenuItem menuitem22 = new MenuItem(i18.getString("Medium"), new MenuShortcut((int) 'M'));
        menuitem22.setActionCommand("MEDIUM");
        menuitem22.addActionListener(this);
        menu2.add(menuitem22);
        MenuItem menuitem23 = new MenuItem(i18.getString("Hard"), new MenuShortcut((int) 'S'));
        menuitem23.setActionCommand("HARD");
        menuitem23.addActionListener(this);
        menu2.add(menuitem23);
        MenuItem menuitem25 = new MenuItem(i18.getString("Very_hard"), new MenuShortcut((int) 'V'));
        menuitem25.setActionCommand("VERY");
        menuitem25.addActionListener(this);
        menu2.add(menuitem25);
        MenuItem menuitem24 = new MenuItem(i18.getString("Extrem_hard"), new MenuShortcut((int) 'X'));
        menuitem24.setActionCommand("EXTREM");
        menuitem24.addActionListener(this);
        menu2.add(menuitem24);
        mbar.add(menu2);
        Menu menu5 = new Menu(i18.getString("Network"));
        MenuItem menuitem51 = new MenuItem(i18.getString("Server"), new MenuShortcut((int) 'S'));
        menuitem51.setActionCommand("SERVER");
        menuitem51.addActionListener(this);
        menu5.add(menuitem51);
        MenuItem menuitem54 = new MenuItem(i18.getString("Create_server"), new MenuShortcut((int) 'C'));
        menuitem54.setActionCommand("CREATE_SERVER");
        menuitem54.addActionListener(this);
        menu5.add(menuitem54);
        MenuItem menuitem52 = new MenuItem(i18.getString("Start_game"), new MenuShortcut((int) 'S'));
        menuitem52.setActionCommand("HUMAN_NETWORK");
        menuitem52.addActionListener(this);
        menu5.add(menuitem52);
        MenuItem menuitem53 = new MenuItem(i18.getString("Join_game"), new MenuShortcut((int) 'S'));
        menuitem53.setActionCommand("NETWORK_HUMAN");
        menuitem53.addActionListener(this);
        menu5.add(menuitem53);
        mbar.add(menu5);
        Menu menu4 = new Menu(i18.getString("Help"));
        MenuItem menuitem41 = new MenuItem(i18.getString("Help"), new MenuShortcut((int) 'H'));
        menuitem41.setActionCommand("HELP");
        menuitem41.addActionListener(this);
        menu4.add(menuitem41);
        mbar.add(menu4);
        frame.setMenuBar(mbar);
        frame.addWindowListener(this);
        for (int c = 0; c < 3; c++) {
            String farbe;
            switch(c) {
                case 1:
                    farbe = "rot";
                    break;
                case 2:
                    farbe = "gruen";
                    break;
                default:
                    farbe = "gelb";
            }
            imageCache[c][0] = Toolkit.getDefaultToolkit().getImage("images/wexplode.gif");
            for (int i = 1; i <= 6; i++) {
                imageCache[c][i] = Toolkit.getDefaultToolkit().getImage("images/w" + farbe + i + ".gif");
            }
        }
        waitCursor = new HourglassCursor(frame);
        frame.setSize(sizex * cubeSize + 8, sizey * cubeSize + 75);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((d.width - frame.getSize().width) / 2, (d.height - frame.getSize().height) / 2);
        init();
    }

    public void init() {
        super.init();
        node = new DiceField(sizex, sizey);
        canvases = new Dice[sizex][sizey];
        undoSpeicher = new Stack<GameNode>();
        frame.setBackground(Color.white);
        frame.setSize(sizex * cubeSize + 8, sizey * cubeSize + 75);
        frame.setVisible(true);
        frame.setSize(sizex * cubeSize + frame.getInsets().left + 4, sizey * cubeSize + frame.getInsets().top + 41);
        frame.removeAll();
        grid = new Panel(new GridLayout(sizey, sizex));
        for (int j = 0; j < sizey; j++) for (int i = 0; i < sizex; i++) {
            canvases[i][j] = new Dice(i, j, this);
            canvases[i][j].setBounds(i * cubeSize + frame.getInsets().left, j * cubeSize + frame.getInsets().top, cubeSize, cubeSize);
            grid.add(canvases[i][j]);
            canvases[i][j].repaint();
        }
        frame.add(grid, BorderLayout.CENTER);
        Panel p = new Panel(new BorderLayout());
        frame.add(p, BorderLayout.PAGE_END);
        p.setBounds(frame.getInsets().left, sizey * cubeSize + frame.getInsets().top, sizex * cubeSize, 20);
        bar = new JProgressBar(0, 100);
        p.add(bar, BorderLayout.PAGE_START);
        bar.setBounds(frame.getInsets().left, sizey * cubeSize + frame.getInsets().top, sizex * cubeSize, 10);
        modeLabel = new Label();
        modeLabel.setAlignment(Label.CENTER);
        p.add(modeLabel, BorderLayout.CENTER);
        countLabel = new Label();
        countLabel.setAlignment(Label.CENTER);
        p.add(countLabel, BorderLayout.EAST);
        countLabel.setSize(30, 10);
        playerLabel = new Label();
        playerLabel.setAlignment(Label.CENTER);
        playerLabel.setSize(30, 10);
        p.add(playerLabel, BorderLayout.WEST);
        eroeffnungen = new HashMap<DiceField, Integer>();
        lernSpeicherLaden("lernspeicher" + sizex + "x" + sizey + ".dat");
        player[ROT] = HUMAN;
        player[GRUEN] = COMPUTER;
        modeLabel.setText("Mensch vs Computer");
        setActivePlayer(1);
        setCount(0);
        setMaxDepth();
        p.setVisible(true);
    }

    public void lernSpeicherLaden(String filename) {
        FileInputStream file = null;
        try {
            file = new FileInputStream(filename);
            ObjectInputStream o = null;
            try {
                o = new ObjectInputStream(file);
            } catch (IOException e) {
                System.err.println(e);
            }
            try {
                while (true) {
                    try {
                        DiceField node = (DiceField) o.readObject();
                        Integer bew = (Integer) o.readObject();
                        eroeffnungen.put(node, bew);
                    } catch (ClassNotFoundException e) {
                        System.err.println(e);
                    }
                }
            } catch (IOException e) {
            }
            try {
                o.close();
            } catch (IOException e) {
                System.err.println(e);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Eroeffnungsbibliothek nicht gefunden.");
        }
        System.out.println("Speichergroesse: " + eroeffnungen.size());
    }

    public Image getImage(int x, int y) {
        Image image;
        if (((DiceField) node).getFieldValue(x, y) > 6) image = imageCache[((DiceField) node).getFieldColor(x, y)][0]; else image = imageCache[((DiceField) node).getFieldColor(x, y)][((DiceField) node).getFieldValue(x, y)];
        return image;
    }

    public void paint(Graphics g) {
        paint();
    }

    public void paint() {
        for (int i = 0; i < sizex; i++) for (int j = 0; j < sizey; j++) {
            if (canvases[i][j] != null) canvases[i][j].repaint();
        }
    }

    public void setActivePlayer(int player) {
        ((DiceField) node).activePlayer = player;
        switch(((DiceField) node).activePlayer) {
            case 1:
                playerLabel.setText(i18.getString("red"));
                playerLabel.setBackground(new Color(190, 30, 30));
                break;
            case 2:
                playerLabel.setText(i18.getString("green"));
                playerLabel.setBackground(new Color(50, 190, 80));
                break;
            default:
                playerLabel.setText(i18.getString("---"));
                playerLabel.setBackground(new Color(250, 250, 150));
                break;
        }
    }

    public void setCount(int aCount) {
        ((DiceField) node).count = aCount;
        countLabel.setText(String.valueOf(((DiceField) node).count));
        countLabel.setBackground(new Color(200, 200, 200));
    }

    public void setMaxDepth() {
        int maxSteps = 3 * sizex * sizey - 2 * sizex - 2 * sizey;
        double percent = ((DiceField) node).count / (double) maxSteps;
        maxDepth = (int) ((difficultyLevel / Math.log(sizex * sizey)) * Math.exp(percent * percent));
        if (maxDepth < 1) maxDepth = 1;
        System.out.println("Gerechnet wird bis in Tiefe " + maxDepth + ".");
    }

    public boolean tipp = false;

    public void actionPerformed(String command, boolean fromNetwork) {
        if (command == null || command.length() < 1) return;
        final String fcommand = command;
        if (!fromNetwork && (player[ROT] == NETWORK)) {
            new Thread(new Runnable() {

                public void run() {
                    setNetwork(GRUEN, fcommand);
                }
            }).start();
        }
        if (!fromNetwork && (player[GRUEN] == NETWORK)) {
            new Thread(new Runnable() {

                public void run() {
                    setNetwork(ROT, fcommand);
                }
            }).start();
        }
        System.out.println(command);
        if (command.startsWith("SET=")) {
            String[] pos = command.substring(4).split("x");
            if (pos.length == 2) {
                final int x = Integer.parseInt(pos[0]);
                final int y = Integer.parseInt(pos[1]);
                if (node.allowed(new DiceFieldStep(x, y, ((DiceField) node).activePlayer))) {
                    new Thread(new Runnable() {

                        public void run() {
                            if (tipp) {
                                tipp = false;
                                paint();
                            }
                            pushUndoSpeicher();
                            int winner = action(new DiceFieldStep(x, y, ((DiceField) node).activePlayer), 300);
                            if (winner != 0) {
                                declareWinner(winner);
                                return;
                            }
                            if (player[((DiceField) node).activePlayer] == DiceFieldGame.COMPUTER) {
                                rechne(((DiceField) node).activePlayer);
                            }
                        }
                    }).start();
                } else Toolkit.getDefaultToolkit().beep();
            }
        }
        if (command.equals("HINT")) {
            if (player[((DiceField) node).activePlayer] == HUMAN) {
                new Thread(new Runnable() {

                    public void run() {
                        int maxTiefe_save = maxDepth;
                        maxDepth = maxDepth + 2;
                        if (maxDepth < (int) (20.0 / Math.log(sizex * sizey))) maxDepth = (int) (20.0 / Math.log(sizex * sizey));
                        System.out.println("Gerechnet wird bis in Tiefe " + maxDepth + ".");
                        int bewertung = suche(((DiceField) node).activePlayer);
                        System.out.println(moeglichkeiten);
                        tipp = true;
                        paint();
                        if (bewertung < -1000 / maxDepth) new PopUpWindow();
                        maxDepth = maxTiefe_save;
                    }
                }).start();
            }
        } else tipp = false;
        if (command.equals("NEW")) {
            node = new DiceField(sizex, sizey);
            player[ROT] = HUMAN;
            player[GRUEN] = COMPUTER;
            modeLabel.setText("Mensch vs Computer");
            setActivePlayer(1);
            setCount(0);
            setMaxDepth();
            paint();
        }
        if (command.equals("UNDO")) {
            if (undoSpeicher.size() > 0) node = (DiceField) undoSpeicher.pop();
            setActivePlayer(((DiceField) node).activePlayer);
            setCount(((DiceField) node).count);
            setMaxDepth();
            paint();
        }
        boolean menschAmZug = false;
        if (player[((DiceField) node).activePlayer] == HUMAN) {
            menschAmZug = true;
        }
        if (command.equals("HUMAN_COMPUTER")) {
            player[ROT] = HUMAN;
            player[GRUEN] = COMPUTER;
            modeLabel.setText("Mensch vs Computer");
        }
        if (command.equals("COMPUTER_HUMAN")) {
            player[ROT] = COMPUTER;
            player[GRUEN] = HUMAN;
            modeLabel.setText("Computer vs Mensch");
        }
        if (command.equals("HUMAN_HUMAN")) {
            player[ROT] = HUMAN;
            player[GRUEN] = HUMAN;
            modeLabel.setText("Mensch vs Mensch");
        }
        if (command.equals("COMPUTER_COMPUTER")) {
            player[ROT] = COMPUTER;
            player[GRUEN] = COMPUTER;
            modeLabel.setText("Computer vs Computer");
        }
        if (menschAmZug == true && player[((DiceField) node).activePlayer] == COMPUTER) {
            new Thread(new Runnable() {

                public void run() {
                    rechne(((DiceField) node).activePlayer);
                    paint(frame.getGraphics());
                }
            }).start();
            return;
        }
        if (command.equals("EASY")) {
            difficultyLevel = 2;
            setMaxDepth();
        }
        if (command.equals("MEDIUM")) {
            difficultyLevel = 7;
            setMaxDepth();
        }
        if (command.equals("HARD")) {
            difficultyLevel = 14;
            setMaxDepth();
        }
        if (command.equals("VERY")) {
            difficultyLevel = 22;
            setMaxDepth();
        }
        if (command.equals("EXTREM")) {
            difficultyLevel = 30;
            setMaxDepth();
        }
        if (command.equals("NEW=3x3")) {
            if (sizex != 3 || sizey != 3) {
                sizex = 3;
                sizey = 3;
                System.out.println("Spielfeld 3x3");
                init();
            }
        }
        if (command.equals("NEW=4x3")) {
            if (sizex != 4 || sizey != 3) {
                sizex = 4;
                sizey = 3;
                System.out.println("Spielfeld 4x3");
                init();
            }
        }
        if (command.equals("NEW=4x4")) {
            if (sizex != 4 || sizey != 4) {
                sizex = 4;
                sizey = 4;
                System.out.println("Spielfeld 4x4");
                init();
            }
        }
        if (command.equals("NEW=5x5")) {
            if (sizex != 5 || sizey != 5) {
                sizex = 5;
                sizey = 5;
                System.out.println("Spielfeld 5x5");
                init();
            }
        }
        if (command.equals("NEW=8x8")) {
            if (sizex != 8 || sizey != 8) {
                sizex = 8;
                sizey = 8;
                System.out.println("Spielfeld 8x8");
                init();
            }
        }
        if (command.equals("SERVER")) {
            new GetServer();
        }
        if (command.equals("CREATE_SERVER")) {
            new GetServer(true);
        }
        if (command.equals("HUMAN_NETWORK")) {
            new GetNetworkGame();
            if (GetNetworkGame.gameName != null) {
                player[ROT] = HUMAN;
                player[GRUEN] = NETWORK;
                modeLabel.setText("Mensch vs Netzwerk");
                new Thread(new Runnable() {

                    public void run() {
                        String c = "NEW=" + sizex + "x" + sizey;
                        setNetwork(((DiceField) node).activePlayer, c);
                    }
                }).start();
            }
        }
        if (command.equals("NETWORK_HUMAN")) {
            new GetNetworkGame();
            if (GetNetworkGame.gameName != null) {
                player[ROT] = NETWORK;
                player[GRUEN] = HUMAN;
                modeLabel.setText("Netzwerk vs. Mensch");
                new Thread(new Runnable() {

                    public void run() {
                        actionPerformed(getNetwork(ROT), true);
                    }
                }).start();
            }
        }
        if (command.equals("HELP")) {
            help.setVisible(true);
        }
        if (command.equals("QUIT")) {
            System.exit(0);
        }
        if ((player[ROT] == NETWORK)) {
            new Thread(new Runnable() {

                public void run() {
                    actionPerformed(getNetwork(ROT), true);
                }
            }).start();
        }
        if ((player[GRUEN] == NETWORK)) {
            new Thread(new Runnable() {

                public void run() {
                    actionPerformed(getNetwork(GRUEN), true);
                }
            }).start();
        }
    }

    public void actionPerformed(ActionEvent e) {
        actionPerformed(e.getActionCommand(), false);
    }

    public int suche(int farbe) {
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        waitCursor.next();
        moeglichkeiten = new ArrayList<GameStep>();
        if (node.won(1) || node.won(2)) return node.getRating(farbe);
        Collection<GameNode> c = node.getChildren(farbe);
        Iterator<GameNode> it = c.iterator();
        List<GameThread> l = new ArrayList<GameThread>();
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                bar.setValue(0);
            }
        });
        int size = c.size();
        int count = 0;
        int max = -1000000;
        while (it.hasNext()) {
            count++;
            DiceField next = (DiceField) (it.next());
            int temp = 0;
            Integer lernSpeicherValue = null;
            if ((lernSpeicherValue = eroeffnungen.get(next)) != null) {
                temp = lernSpeicherValue.intValue();
                System.out.print(".");
                if (Math.abs(temp) >= 1000 / maxDepth) System.out.print("*");
                System.out.println("Moeglichkeit fuer " + farbe + " (" + ((DiceFieldStep) next.lastStep).getX() + "," + ((DiceFieldStep) next.lastStep).getY() + ") ist " + temp);
                if (temp > max) {
                    moeglichkeiten = new ArrayList<GameStep>();
                    moeglichkeiten.add(next.lastStep());
                    max = temp;
                } else if (temp == max) {
                    moeglichkeiten.add(next.lastStep());
                }
            } else {
                GameThread thread = new GameThread(this, next, farbe % 2 + 1);
                l.add(thread);
            }
            final int barValue = 10 * count / size;
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    bar.setValue(barValue);
                }
            });
        }
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                bar.setValue(10);
            }
        });
        waitCursor.next();
        size = l.size();
        count = 0;
        Iterator<GameThread> it2 = l.iterator();
        while (it2.hasNext()) {
            GameThread thread = it2.next();
            DiceField next = (DiceField) (thread.getNode());
            int temp = 0;
            while (!thread.fertig()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                waitCursor.next();
            }
            temp = -thread.ergebnis();
            if (Math.abs(temp) >= 1000 / maxDepth) System.out.print("*");
            System.out.println("Moeglichkeit fuer " + farbe + " (" + ((DiceFieldStep) next.lastStep).getX() + "," + ((DiceFieldStep) next.lastStep).getY() + ") ist " + temp);
            if (temp > max) {
                moeglichkeiten = new ArrayList<GameStep>();
                moeglichkeiten.add(next.lastStep());
                max = temp;
            } else if (temp == max) {
                moeglichkeiten.add(next.lastStep());
            }
            final int barValue = 10 + 90 * count / size;
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    bar.setValue(barValue);
                }
            });
            waitCursor.next();
        }
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                bar.setValue(100);
            }
        });
        waitCursor.stop();
        frame.setVisible(true);
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        return max;
    }

    private void addNumber(int x, int y, int farbe, int sleeptime) {
        if (x < 0 || x >= sizex || y < 0 || y >= sizey) {
            return;
        }
        if (((DiceField) node).getFieldColor(x, y) != farbe) {
            ((DiceField) node).points[((DiceField) node).getFieldColor(x, y)]--;
            ((DiceField) node).points[farbe]++;
        }
        ((DiceField) node).setField(x, y, farbe, ((DiceField) node).getFieldValue(x, y) + 1);
        canvases[x][y].paint();
        try {
            Thread.sleep(sleeptime);
        } catch (InterruptedException e) {
        }
    }

    private void explode(int x, int y, int farbe, int sleeptime) {
        if (x < 0 || x >= sizex || y < 0 || y >= sizey || ((DiceField) node).points[farbe] >= sizex * sizey) {
            return;
        }
        if (((DiceField) node).getFieldValue(x, y) > ((DiceField) node).neighbors(x, y)) {
            try {
                Thread.sleep(sleeptime);
            } catch (InterruptedException e) {
            }
            int temp = ((DiceField) node).getFieldValue(x, y);
            ((DiceField) node).setFieldValue(x, y, 0);
            canvases[x][y].paint();
            addNumber(x - 1, y, farbe, sleeptime / 2);
            addNumber(x + 1, y, farbe, sleeptime / 2);
            addNumber(x, y - 1, farbe, sleeptime / 2);
            addNumber(x, y + 1, farbe, sleeptime / 2);
            try {
                Thread.sleep(sleeptime);
            } catch (InterruptedException e) {
            }
            ((DiceField) node).setField(x, y, farbe, temp - ((DiceField) node).neighbors(x, y));
            canvases[x][y].paint();
            explode(x - 1, y, farbe, 4 * sleeptime / 5);
            explode(x + 1, y, farbe, 4 * sleeptime / 5);
            explode(x, y - 1, farbe, 4 * sleeptime / 5);
            explode(x, y + 1, farbe, 4 * sleeptime / 5);
        }
    }

    public int action(GameStep step, int sleeptime) {
        setActivePlayer(0);
        setCount(((DiceField) node).count + 1);
        setMaxDepth();
        int x = ((DiceFieldStep) step).getX();
        int y = ((DiceFieldStep) step).getY();
        int farbe = ((DiceFieldStep) step).getSpieler();
        canvases[x][y].paint();
        addNumber(x, y, farbe, sleeptime);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        explode(x, y, farbe, sleeptime);
        computerstep = null;
        canvases[x][y].paint();
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        setActivePlayer(farbe % 2 + 1);
        if (((DiceField) node).points[farbe] >= sizex * sizey) return farbe; else return 0;
    }

    public GameStep computerstep;

    public void rechne(int spieler) {
        int b = suche(spieler);
        computerstep = (DiceFieldStep) moeglichkeiten.get(r.nextInt(moeglichkeiten.size()));
        System.out.println("Spieler " + spieler + " nahm " + computerstep + " mit " + b);
        if (!node.allowed(computerstep)) System.err.println("Versuch auf ein fehlerhaftes Feld zuzugreifen.");
        int winner = action(computerstep, 200);
        if (winner != 0) {
            declareWinner(winner);
            return;
        }
        if (player[((DiceField) node).activePlayer] == DiceFieldGame.COMPUTER) {
            rechne(((DiceField) node).activePlayer);
        }
    }

    public void pushUndoSpeicher() {
        undoSpeicher.push(node.clone());
    }

    public void computerComputerSpiel_alt() {
        while (!node.won(1) && !node.won(2)) {
            for (int spieler = 1; spieler < 3; spieler++) {
                rechne(spieler);
                paint(frame.getGraphics());
            }
        }
    }

    public void declareWinner(int winner) {
        setActivePlayer(0);
        System.out.println("**** Gewinner ist " + winner + " ****");
        new PopUpWindow(winner);
    }

    public static void main(String[] args) {
        DiceFieldGame cubegame = new DiceFieldGame();
        cubegame.frame.setVisible(true);
    }

    public void windowActivated(WindowEvent arg0) {
    }

    public void windowClosed(WindowEvent arg0) {
    }

    public void windowClosing(WindowEvent arg0) {
        actionPerformed("QUIT", false);
    }

    public void windowDeactivated(WindowEvent arg0) {
    }

    public void windowDeiconified(WindowEvent arg0) {
    }

    public void windowIconified(WindowEvent arg0) {
    }

    public void windowOpened(WindowEvent arg0) {
    }

    public Dimension getCubeSize() {
        Dimension d = grid.getSize();
        return new Dimension(d.width / sizex, d.height / sizey);
    }

    public String getNetwork(int spieler) {
        InputStream is = null;
        String command = "";
        try {
            URL server = new URL("http://" + GetServer.serverName + ":" + String.valueOf(GetServer.serverPort));
            String path;
            if (spieler == DiceField.RED) path = GetNetworkGame.gameName + "/red"; else path = GetNetworkGame.gameName + "/green";
            URL url = new URL(server, path);
            System.out.println("Anfrage: http://" + GetServer.serverName + ":" + String.valueOf(GetServer.serverPort) + "/" + path);
            is = url.openStream();
            command = new Scanner(is).useDelimiter(" ").next();
            System.out.println("Antwort Netzwerk: " + command);
        } catch (MalformedURLException e) {
            new PopUpWindow("Serverfehler", "Server nicht erreichbar.");
        } catch (IOException e) {
            new PopUpWindow("Clientfehler", "Kein Mitspieler gefunden.");
        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException e) {
            }
        }
        return command;
    }

    public void setNetwork(int spieler, String command) {
        InputStream is = null;
        try {
            URL server = new URL("http://" + GetServer.serverName + ":" + String.valueOf(GetServer.serverPort));
            String path;
            if (spieler == DiceField.RED) path = GetNetworkGame.gameName + "/red"; else path = GetNetworkGame.gameName + "/green";
            URL url = new URL(server, path + "?" + command);
            System.out.println("Anfrage: http://" + GetServer.serverName + ":" + String.valueOf(GetServer.serverPort) + "/" + path + "?" + command);
            is = url.openStream();
            System.out.println("Antwort Netzwerk: " + new Scanner(is).useDelimiter("\n").next());
        } catch (MalformedURLException e) {
            new PopUpWindow("Serverfehler", "Server nicht erreichbar.");
        } catch (IOException e) {
            new PopUpWindow("Clientfehler", "Kein Mitspieler gefunden.");
        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException e) {
            }
        }
    }
}
