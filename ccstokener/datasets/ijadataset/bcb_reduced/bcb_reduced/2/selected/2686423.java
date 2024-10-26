package jmemorize.core;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;
import java.util.Observable;
import java.util.Properties;
import java.util.prefs.Preferences;
import javax.swing.Timer;
import jmemorize.gui.swing.MainFrame;
import jmemorize.strategy.Strategy;
import jmemorize.util.RecentItems;

/**
 * The main class of the application.
 * 
 * @author djemili
 */
public class Main extends Observable {

    public static final Properties PROPERTIES = new Properties();

    public static final Preferences USER_PREFS = Preferences.userRoot().node("de/riad/jmemorize");

    private MainFrame m_frame;

    private Lesson m_lesson;

    private Strategy m_strategy;

    private static Main m_instance;

    private RecentItems m_recentFiles = new RecentItems(5, USER_PREFS.node("recent.files"));

    private Card m_nextCard;

    private Date m_lastDate = new Date();

    private class CardTimer implements ActionListener {

        private Card m_card;

        public CardTimer(Card card) {
            m_card = card;
            int delay = (int) (card.getDateExpired().getTime() - System.currentTimeMillis());
            new Timer(delay, this);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    public static Main getInstance() {
        if (m_instance == null) {
            m_instance = new Main();
        }
        return m_instance;
    }

    public static Date getNow() {
        return new Date();
    }

    public static Date getTomorrow() {
        return new Date(new Date().getTime() + Card.ONE_DAY);
    }

    public void createNewLesson() {
        m_lesson = new Lesson();
        if (m_frame != null) {
            m_frame.setLesson(m_lesson);
        }
    }

    public void loadLesson(File file) throws IOException {
        try {
            m_lesson = Lesson.fromXML(file);
            m_recentFiles.push(file.getAbsolutePath());
        } catch (Exception e) {
            m_recentFiles.remove(file.getAbsolutePath());
            throw new IOException(e.getMessage());
        }
        if (m_frame != null) {
            m_frame.setLesson(m_lesson);
        }
    }

    /**
     * @return Name of newest version if there is a version that is newer then the 
     * current version. null otherwise.
     */
    public String getNewestVersion() {
        String version = Main.PROPERTIES.getProperty("project.version");
        int buildId = Integer.parseInt(Main.PROPERTIES.getProperty("buildId"));
        try {
            URL url = new URL("http://riad.de/jmemorize/vc/" + buildId);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String[] s = in.readLine().split(" ");
            int newestBuildID = Integer.parseInt(s[0]);
            String newestVersion = s[1];
            in.close();
            return newestBuildID > buildId ? newestVersion : null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void saveLesson(File file) throws IOException {
        try {
            m_lesson.toXML(file);
            m_recentFiles.push(file.getAbsolutePath());
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        m_lesson.setFile(file);
    }

    /**
     * @return Currently loaded lesson.
     */
    public Lesson getLesson() {
        return m_lesson;
    }

    /**
     * @return Currently loaded learn strategy.
     */
    public Strategy getStrategy() {
        return m_strategy;
    }

    public RecentItems getRecentFiles() {
        return m_recentFiles;
    }

    private Main() {
        try {
            PROPERTIES.load(getClass().getResource("/resource/jMemorize.properties").openStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void run(File file) {
        createNewLesson();
        m_frame = new MainFrame();
        m_strategy = Settings.loadStrategy(m_frame);
        m_frame.setVisible(true);
        if (file != null) {
            m_frame.loadLesson(file);
        }
    }

    private void startExpirationTimer() {
        m_nextCard = getLesson().getNextExpirationCard(m_lastDate);
        m_lastDate = m_nextCard.getDateExpired();
        new CardTimer(m_nextCard);
    }

    /**
	 * @return the main frame.
	 */
    public MainFrame getFrame() {
        return m_frame;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        File file = args.length >= 1 ? new File(args[0]) : null;
        Main.getInstance().run(file);
    }
}
