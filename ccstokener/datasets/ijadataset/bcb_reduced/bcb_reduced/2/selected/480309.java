package eveskillwatch;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.prefs.*;
import javax.imageio.stream.FileImageInputStream;
import javax.swing.*;
import javax.xml.parsers.*;
import javax.xml.ws.ProtocolException;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 *
 * @author alexanagnos
 */
public class EveSkillWatch implements Runnable, ActionListener {

    public static final String USER_ID_KEY = "USER_ID";

    public static final String CHARACTER_ID_KEY = "CHARACTER_ID";

    public static final String API_KEY_KEY = "API_KEY";

    public static final String DISPLAY_TRAINING_END_NOTIFICATION_KEY = "DISPLAY_TRAINING_END_NOTIFICATION";

    public static final String ADVANCED_TRAINING_END_NOTIFICATION_KEY = "ADVANCED_TRAINING_END_NOTIFICATION";

    public static final String ADVANCED_TRAINING_END_NOTIFICATION_MIN_KEY = "ADVANCED_TRAINING_END_NOTIFICATION_MIN";

    public static final String PLAY_SOUND_ON_TRAINING_END_KEY = "PLAY_SOUND_ON_TRAINING_END";

    public static final String DEFAULT_SOUND_SELECTED_STATE_KEY = "DEFAULT_SOUND_SELECTED_STATE";

    public static final String CUSTOM_SOUND_SELECTED_STATE_KEY = "CUSTOM_SOUND_SELECTED_STATE";

    public static final String CUSTOM_SOUND_FILE_PATH_KEY = "CUSTOM_SOUND_FILE_PATH";

    public static final String USER_ID_DEFUALT = "USER_ID";

    public static final String CHARACTER_ID_DEFUALT = "CHARACTER_ID";

    public static final String API_KEY_DEFUALT = "API_KEY";

    public static final Boolean DISPLAY_TRAINING_END_NOTIFICATION_DEFAULT = true;

    public static final Boolean ADVANCED_TRAINING_END_NOTIFICATION_DEFAULT = false;

    public static final String ADVANCED_TRAINING_END_NOTIFICATION_MIN_DEFAULT = "5";

    public static final boolean PLAY_SOUND_ON_TRAINING_END_DEFAULT = false;

    public static final boolean DEFAULT_SOUND_SELECTED_STATE_DEFAULT = true;

    public static final boolean CUSTOM_SOUND_SELECTED_STATE_DEFAULT = false;

    public static final String CUSTOM_SOUND_FILE_PATH_DEFAULT = "defaultSound.wav";

    private static final File OPTION_FILE = new File("Options.xml");

    private static final File RELATIONS_TABLE_FILE = new File("table.obj");

    private static final File ERROR_FILE = new File("error.log");

    private static final boolean DEBUG = false;

    private Document eveStatus;

    private SystemTray tray;

    private Image goodStatus, badStatus;

    private TrayIcon trayIcon;

    private int trainingID;

    private String trainingName;

    private Calendar trainingEndDate;

    private Calendar trainingStartDate;

    private int destSkillPoints;

    private int currentSkillPoints;

    private short destLevel;

    private int startSkillPoints;

    private short numSkills;

    private Calendar catchedTill;

    private String xmlCurrentTime;

    private boolean informedUser;

    private MenuItem exitItem;

    private MenuItem infoItem;

    private MenuItem refreshItem;

    private MenuItem skill;

    private MenuItem percent;

    private MenuItem time;

    public EveSkillWatch() {
        Preferences pref = Preferences.userNodeForPackage(this.getClass());
        try {
            pref.importPreferences(new FileInputStream(OPTION_FILE));
        } catch (FileNotFoundException ex) {
            Error(ex.toString());
        } catch (IOException ex) {
            Error(ex.toString());
        } catch (InvalidPreferencesFormatException ex) {
            Error(ex.toString());
        }
        if (!SystemTray.isSupported()) {
            Error("System Tray unsuported");
            throw new RuntimeException("System Tray unsuported.");
        }
        eveStatus = null;
        tray = SystemTray.getSystemTray();
        goodStatus = Toolkit.getDefaultToolkit().getImage("Training.png");
        badStatus = Toolkit.getDefaultToolkit().getImage("noTraining.png");
        PopupMenu popup = new PopupMenu();
        exitItem = new MenuItem("Exit");
        exitItem.addActionListener(this);
        popup.add(exitItem);
        infoItem = new MenuItem("Options");
        infoItem.addActionListener(this);
        popup.add(infoItem);
        refreshItem = new MenuItem("Refresh");
        refreshItem.addActionListener(this);
        popup.add(refreshItem);
        skill = new MenuItem();
        skill.setEnabled(false);
        popup.add(skill);
        percent = new MenuItem();
        percent.setEnabled(false);
        popup.add(percent);
        time = new MenuItem();
        time.setEnabled(false);
        popup.add(time);
        trayIcon = new TrayIcon(goodStatus, "Loading", popup);
        trayIcon.setImageAutoSize(true);
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.err.println("TrayIcon could not be added.");
        }
        catchedTill = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        trainingEndDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        trainingStartDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        new Thread(this).start();
    }

    private void refreshCycle() {
        refreshAPIXML();
        refresh();
        save();
        System.gc();
        updateMenu();
    }

    private void showUserInfo() {
        new OptionDialog(new JFrame(), true).showGetInfo();
        refreshCycle();
        save();
    }

    private void refreshAPIXML() {
        if (DEBUG) System.out.println("Refreshing XML");
        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        String UserID = prefs.get(USER_ID_KEY, USER_ID_DEFUALT);
        String APIKey = prefs.get(API_KEY_KEY, API_KEY_DEFUALT);
        String CharacterID = prefs.get(CHARACTER_ID_KEY, CHARACTER_ID_DEFUALT);
        if (DEBUG) System.out.println("UID: " + UserID + " API: " + APIKey + " CHAR: " + CharacterID);
        eveStatus = null;
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            String data = URLEncoder.encode("userID", "UTF-8") + "=" + URLEncoder.encode(UserID, "UTF-8");
            data += "&" + URLEncoder.encode("apiKey", "UTF-8") + "=" + URLEncoder.encode(APIKey, "UTF-8");
            data += "&" + URLEncoder.encode("characterID", "UTF-8") + "=" + URLEncoder.encode(CharacterID, "UTF-8");
            String link = "/char/SkillInTraining.xml.aspx";
            URL url = new URL("http://api.eve-online.com" + link);
            URLConnection connection = url.openConnection();
            ((HttpURLConnection) connection).setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            PrintWriter output = new PrintWriter(new OutputStreamWriter(connection.getOutputStream()));
            output.write(data);
            output.flush();
            output.close();
            eveStatus = builder.parse(connection.getInputStream());
        } catch (UnsupportedEncodingException ex) {
            eveStatus = null;
        } catch (ProtocolException ex) {
            eveStatus = null;
        } catch (MalformedURLException ex) {
            eveStatus = null;
        } catch (ParserConfigurationException ex) {
            eveStatus = null;
        } catch (SAXException ex) {
            eveStatus = null;
        } catch (IOException ex) {
            eveStatus = null;
        }
    }

    public static synchronized void Error(String s) {
        try {
            File f = new File("error.log");
            PrintWriter out = new PrintWriter(new FileWriter(f, true));
            out.println(DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(Calendar.getInstance().getTime()) + " " + s);
            out.close();
        } catch (IOException ex) {
        }
    }

    private String lookUpInTable(int i) {
        Hashtable<Integer, Item> table = null;
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(RELATIONS_TABLE_FILE));
            Object ob = in.readObject();
            if (ob.getClass().equals(Hashtable.class)) table = (Hashtable<Integer, Item>) (ob);
            in.close();
        } catch (FileNotFoundException ex) {
            Error("Failed to load relations table");
            Error(ex.toString());
        } catch (IOException ex) {
            Error("Failed to load relations table");
            Error(ex.toString());
        } catch (ClassNotFoundException ex) {
            Error("Failed to load relations table");
            Error(ex.toString());
        }
        if (table == null) {
            Error("Failed to load relations table");
            return "Unknowen";
        }
        Item item = table.get(i);
        if (item == null) return "UnKnowen";
        return item.getName();
    }

    private void updateMenu() {
        if (DEBUG) System.out.println("Updating menu");
        long startEndDiff = trainingEndDate.getTimeInMillis() - trainingStartDate.getTimeInMillis();
        if (DEBUG) System.out.println("startEndDiff: " + startEndDiff);
        long nowEndDiff = trainingEndDate.getTimeInMillis() - Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTimeInMillis();
        if (DEBUG) System.out.println("nowEndDiff: " + nowEndDiff);
        int pointsToGo = destSkillPoints - startSkillPoints;
        if (DEBUG) System.out.println("pointsToGo: " + pointsToGo);
        double pointsPerSec = (startEndDiff / 1000.0) / (pointsToGo);
        if (DEBUG) System.out.println("pointsPerSec: " + pointsPerSec);
        currentSkillPoints = (int) (destSkillPoints - ((nowEndDiff / 1000.0) / pointsPerSec));
        if (DEBUG) System.out.println("currentSkillPoints: " + currentSkillPoints);
        double percentage = currentSkillPoints / (destSkillPoints * 1.0);
        if (currentSkillPoints >= destSkillPoints) {
            if (DEBUG) System.out.println("not Training anything");
            skill.setLabel("Skill: ???");
            percent.setLabel("Training Till: ");
            time.setLabel("??? left");
            trayIcon.setImage(badStatus);
            trayIcon.setToolTip("Not training any Skill!!");
            if (!informedUser) {
                JOptionPane.showMessageDialog(new Component() {
                }, "Not training any skill!", "Not Training", JOptionPane.INFORMATION_MESSAGE);
                informedUser = true;
            }
            return;
        } else {
            if (DEBUG) System.out.println("Training :)");
            informedUser = false;
            skill.setLabel("Skill: " + trainingName + " to Level " + destLevel);
            String s = new String(NumberFormat.getPercentInstance().format(percentage));
            s += " " + NumberFormat.getInstance().format(currentSkillPoints);
            s += " of " + NumberFormat.getInstance().format(destSkillPoints);
            percent.setLabel(s);
            time.setLabel(getTimeLeftFromMilli(nowEndDiff) + " left");
            trayIcon.setImage(goodStatus);
            trayIcon.setToolTip("Training " + trainingName + " to Level " + destLevel + " " + NumberFormat.getPercentInstance().format(percentage) + " complete.");
        }
    }

    private void refresh() {
        if (DEBUG) System.out.println("refreshing variables");
        if (eveStatus == null) {
            Error("Failed to get XML, or Document otherwise null");
            return;
        }
        NodeList tmpList = eveStatus.getElementsByTagName("skillInTraining");
        if (tmpList == null || tmpList.getLength() == 0) {
            eveStatus = null;
            return;
        }
        numSkills = Short.parseShort(tmpList.item(0).getFirstChild().getNodeValue());
        if (DEBUG) System.out.println("numSkills: " + numSkills);
        if (numSkills == 0) {
            return;
        }
        trainingID = Integer.parseInt(eveStatus.getElementsByTagName("trainingTypeID").item(0).getFirstChild().getNodeValue());
        if (DEBUG) System.out.println("trainingID: " + trainingID);
        destLevel = Short.parseShort(eveStatus.getElementsByTagName("trainingToLevel").item(0).getFirstChild().getNodeValue());
        trainingName = lookUpInTable(trainingID);
        destSkillPoints = Integer.parseInt(eveStatus.getElementsByTagName("trainingDestinationSP").item(0).getFirstChild().getNodeValue());
        if (DEBUG) System.out.println("destSkillPoints: " + destSkillPoints);
        startSkillPoints = Integer.parseInt(eveStatus.getElementsByTagName("trainingStartSP").item(0).getFirstChild().getNodeValue());
        if (DEBUG) System.out.println("startSkillPoints: " + startSkillPoints);
        String tmpD;
        tmpD = eveStatus.getElementsByTagName("cachedUntil").item(0).getFirstChild().getNodeValue();
        if (DEBUG) System.out.println("cachedUntil: " + tmpD);
        setCalandarObject(catchedTill, tmpD);
        catchedTill.setTimeZone(TimeZone.getTimeZone("GMT"));
        if (DEBUG) System.out.println("cachedUntil: " + catchedTill.toString());
        tmpD = eveStatus.getElementsByTagName("trainingEndTime").item(0).getFirstChild().getNodeValue();
        if (DEBUG) System.out.println("trainingEndTime: " + tmpD);
        setCalandarObject(trainingEndDate, tmpD);
        trainingEndDate.setTimeZone(TimeZone.getTimeZone("GMT"));
        if (DEBUG) System.out.println("trainingEndTime: " + trainingEndDate.toString());
        tmpD = eveStatus.getElementsByTagName("trainingStartTime").item(0).getFirstChild().getNodeValue();
        if (DEBUG) System.out.println("trainingStartTime: " + tmpD);
        setCalandarObject(trainingStartDate, tmpD);
        trainingStartDate.setTimeZone(TimeZone.getTimeZone("GMT"));
        if (DEBUG) System.out.println("trainingStartTime: " + trainingStartDate.toString());
        xmlCurrentTime = eveStatus.getElementsByTagName("currentTime").item(0).getFirstChild().getNodeValue();
    }

    private String getTimeLeftFromMilli(long left) {
        String s = new String("");
        int days = 1000 * 60 * 60 * 24;
        int hours = 1000 * 60 * 60;
        int min = 1000 * 60;
        int sec = 1000;
        int tmp = 0;
        tmp = (int) left / days;
        left = left - tmp * days;
        if (tmp > 0) s += tmp + "d ";
        tmp = (int) left / hours;
        left = left - tmp * hours;
        if (tmp > 0 || s.length() > 0) s += tmp + "h ";
        tmp = (int) left / min;
        left = left - tmp * min;
        if (tmp > 0 || s.length() > 0) s += tmp + "m ";
        tmp = (int) left / sec;
        s += tmp + "s";
        return s;
    }

    private void setCalandarObject(Calendar cal, String dateTime) {
        String[] tmp = dateTime.split(" ");
        String[] date = tmp[0].split("-");
        String[] time = tmp[1].split(":");
        String amPM = null;
        if (tmp.length == 3) amPM = tmp[2];
        int year = Integer.parseInt(date[0]);
        int month = Integer.parseInt(date[1]);
        month--;
        int day = Integer.parseInt(date[2]);
        int hour = Integer.parseInt(time[0]);
        if (amPM != null && amPM.equalsIgnoreCase("PM")) hour += 12;
        int min = Integer.parseInt(time[1]);
        int sec = Integer.parseInt(time[2]);
        if (DEBUG) System.out.println(year + " " + month + " " + day + " " + hour + ":" + min + ":" + sec);
        cal.set(year, month, day, hour, min, sec);
        cal.getTimeInMillis();
        tmp = null;
        date = null;
        time = null;
    }

    private void save() {
        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        try {
            prefs.exportSubtree(new FileOutputStream(OPTION_FILE));
        } catch (FileNotFoundException ex) {
            Error("Failed to save Option file");
            Error(ex.toString());
        } catch (IOException ex) {
            Error("Failed to save Option file");
            Error(ex.toString());
        } catch (BackingStoreException ex) {
            Error("Failed to save Option file");
            Error(ex.toString());
        }
    }

    public void run() {
        if (DEBUG) System.out.println("Start master loop");
        while (true) {
            if (catchedTill.compareTo(Calendar.getInstance(TimeZone.getTimeZone("GMT"))) < 0) {
                refreshCycle();
            } else updateMenu();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Error("Thread Sleep failed or interupted");
                throw new RuntimeException("Thread Sleep failed or interupted");
            }
        }
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (UnsupportedLookAndFeelException ex) {
                    ex.printStackTrace();
                } catch (InstantiationException ex) {
                    ex.printStackTrace();
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace();
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                }
                new EveSkillWatch();
            }
        });
    }

    public void actionPerformed(ActionEvent actionEvent) {
        Object source = actionEvent.getSource();
        if (source == exitItem) {
            System.out.println("Exiting...");
            System.exit(0);
        } else if (source == infoItem) {
            showUserInfo();
        } else if (source == refreshItem) {
            refreshCycle();
        }
    }
}
