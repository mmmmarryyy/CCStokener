package net.sf.opendarkroom;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.GroupLayout.Alignment;
import library.StringUtils;
import org.apache.log4j.Logger;
import external.ScrollGestureRecognizer;
import external.SelectionPreservingCaret;

public class OpenDarkRoom extends JFrame {

    static Logger log = Logger.getLogger(OpenDarkRoom.class);

    private static OpenDarkRoom instance = null;

    static final String HELP = "Help";

    static final String MAIN = "Main";

    public static String userDir;

    public static Properties localization;

    public static Properties preferences;

    public static ArrayList<String> mruList;

    public TextDocumentContainer textPane;

    private JPanel cards;

    static {
        String[] pathComponents = { System.getProperty("user.home"), ".opendarkroom" };
        userDir = StringUtils.buildPath(pathComponents);
        (new File(userDir)).mkdirs();
        localization = new Properties();
        preferences = new Properties();
        loadProperties();
        loadMruList();
    }

    public OpenDarkRoom() {
        super();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(WindowEvent winEvt) {
                log.debug("received windowClosing event");
                if (textPane.discardDocument()) {
                    System.exit(0);
                }
            }
        });
        setTitle("OpenDarkRoom");
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        cards = new JPanel(new CardLayout());
        textPane = new TextDocumentContainer();
        textPane.setCaret(new SelectionPreservingCaret());
        setVisualProperties();
        JScrollPane jScrollPane = new JScrollPane(textPane);
        jScrollPane.setBorder(null);
        jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        cards.add(jScrollPane, MAIN);
        setResizable(false);
        if (!isDisplayable()) {
            setUndecorated(true);
        }
    }

    protected void setVisualProperties() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        GroupLayout layout = (GroupLayout) getContentPane().getLayout();
        int w = Toolkit.getDefaultToolkit().getScreenSize().width * (new Integer(OpenDarkRoom.preferences.getProperty("editor.widthPercent"))) / 100;
        layout.setVerticalGroup(layout.createParallelGroup(Alignment.CENTER).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(cards, GroupLayout.PREFERRED_SIZE, 200, Short.MAX_VALUE).addContainerGap()));
        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER).addGroup(layout.createSequentialGroup().addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(cards, w, w, w).addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        getContentPane().setBackground(new Color(Long.valueOf(OpenDarkRoom.preferences.getProperty("text.backgroundColor"), 16).intValue()));
        textPane.setCaretColor(new Color(Long.valueOf(OpenDarkRoom.preferences.getProperty("text.color"), 16).intValue()));
        textPane.setBackground(new Color(Long.valueOf(OpenDarkRoom.preferences.getProperty("text.backgroundColor"), 16).intValue()));
        textPane.setForeground(new Color(Long.valueOf(OpenDarkRoom.preferences.getProperty("text.color"), 16).intValue()));
        Font editorFont = new Font(OpenDarkRoom.preferences.getProperty("text.fontFamily"), Font.PLAIN, Integer.parseInt(OpenDarkRoom.preferences.getProperty("text.fontSize")));
        textPane.setFont(editorFont);
    }

    public static OpenDarkRoom getInstance() {
        if (instance == null) {
            log.debug("creating new instance");
            instance = new OpenDarkRoom();
        }
        return instance;
    }

    public static void loadProperties() {
        try {
            URL url = ClassLoader.getSystemResource("OpenDarkRoom.lang.en.properties");
            localization.load(url.openStream());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileInputStream is = new FileInputStream(userDir + "OpenDarkRoom.properties");
            preferences.load(is);
        } catch (FileNotFoundException e) {
            log.warn("Preferences file " + userDir + "OpenDarkRoom.properties not found, loading defaults");
            loadDefaultPreferences();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadDefaultPreferences() {
        try {
            URL url = ClassLoader.getSystemResource("OpenDarkRoom.defaults.properties");
            preferences.load(url.openStream());
        } catch (FileNotFoundException e) {
            log.error("Default preferences file not found");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void clearMruList() {
        log.debug("clearing mru...");
        mruList = new ArrayList<String>();
        saveMruList();
        getInstance().textPane.refreshContextMenu();
    }

    public static void loadMruList() {
        mruList = new ArrayList<String>();
        Properties mru = new Properties();
        try {
            FileInputStream is = new FileInputStream(userDir + "MRU.properties");
            mru.load(is);
            for (int i = 0; i < 10; i++) {
                String filePath = (String) mru.get("file" + i);
                if (filePath != null) {
                    mruList.add(filePath);
                }
            }
        } catch (FileNotFoundException e) {
            log.warn("MRU file not found");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveMruList() {
        FileOutputStream out = null;
        Properties mru = new Properties();
        for (int i = 0; i < mruList.size(); i++) {
            mru.setProperty("file" + i, mruList.get(i));
        }
        try {
            log.debug("saving mru...");
            out = new FileOutputStream(userDir + "MRU.properties");
            mru.store(out, "OpenDarkRoom MRU file");
            log.debug("mru saved");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addEntryToMru(String path) {
        log.debug("adding entry to MRU list: " + path);
        while (mruList.contains(path)) {
            log.debug("mruList entry exists, removing it...");
            mruList.remove(path);
        }
        mruList.add(0, path);
        saveMruList();
    }

    public static void savePreferences() {
        FileOutputStream out = null;
        try {
            log.debug("saving preferences...");
            out = new FileOutputStream(userDir + "OpenDarkRoom.properties");
            OpenDarkRoom.preferences.store(out, "OpenDarkRoom preferences file");
            log.debug("preferences saved");
        } catch (Exception e) {
            JOptionPane.showInternalMessageDialog(getInstance(), OpenDarkRoom.localization.getProperty("error.unableToSaveSettings") + "\n" + e.getLocalizedMessage(), OpenDarkRoom.localization.getProperty("error"), JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Decide whether this is the first run or not
	 * 
	 * @return
	 */
    public static boolean showStartupHelp() {
        if ((new File(userDir + ".nocherry")).exists()) {
            return false;
        }
        try {
            (new File(userDir + ".nocherry")).createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private static void createAndShowGui() {
        log.debug("creating gui");
        loadProperties();
        ScrollGestureRecognizer.getInstance();
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        gd.setFullScreenWindow(getInstance());
        getInstance().setVisible(true);
        if (preferences.getProperty("editor.loadMru", "false").equals("true") && mruList.size() > 0) {
            getInstance().textPane.loadDocument(mruList.get(0), true);
        }
        log.debug("displayed gui");
        if (showStartupHelp()) {
            new HelpDialog(getInstance());
        }
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                createAndShowGui();
            }
        });
    }
}
