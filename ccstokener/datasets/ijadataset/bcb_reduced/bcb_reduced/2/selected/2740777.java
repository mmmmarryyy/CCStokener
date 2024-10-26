package jeliot;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.util.Properties;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import jeliot.gui.LoadJeliot;
import jeliot.tracker.Tracker;
import jeliot.tracker.TrackerClock;
import jeliot.util.DebugUtil;
import com.incors.plaf.kunststoff.KunststoffLookAndFeel;

/**
 * This is an extension of the application class of Jeliot 3 that
 * adds features for JavaWS and Url Loading
 *
 * @author Roland K�stermann
 * @author amoreno
 */
public class JeliotDebugWS extends Jeliot {

    public JeliotDebugWS() {
        super("jeliot.io.*");
        Policy.setPolicy(new Policy() {

            public PermissionCollection getPermissions(CodeSource codesource) {
                Permissions perms = new Permissions();
                perms.add(new AllPermission());
                return (perms);
            }

            public void refresh() {
            }
        });
    }

    /**
	 * get Program from url
	 */
    public void setProgram(final URL u) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                try {
                    System.out.println("Reading from u = " + u);
                    URLConnection urlConn = u.openConnection();
                    urlConn.connect();
                    InputStream is = urlConn.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader bin = new BufferedReader(isr);
                    String line = null;
                    StringBuffer content = new StringBuffer();
                    while ((line = bin.readLine()) != null) content.append(line).append("\n");
                    bin.close();
                    if (content.length() > 0) gui.setProgram(content.toString());
                } catch (IOException e) {
                    if (DebugUtil.DEBUGGING) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void handleArgs(String args[]) {
        try {
            URL u = null;
            if (args.length >= 1) {
                try {
                    u = new URL(URLDecoder.decode(args[0], "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    if (DebugUtil.DEBUGGING) e.printStackTrace();
                }
                setProgram(u);
            }
            if (args.length >= 2) {
                Properties prop = System.getProperties();
                String osName = prop.getProperty("os.name");
                String userDirectory = prop.getProperty("user.dir");
                if (osName.toLowerCase().indexOf("windows") > -1) {
                    userDirectory = "C:\\temp\\jeliot\\";
                }
                if (args[1] != null) {
                    Tracker.setTrack(Boolean.valueOf(args[1]).booleanValue());
                    File f = new File(userDirectory);
                    Tracker.openFile(f);
                    Tracker.trackEvent(TrackerClock.currentTimeMillis(), Tracker.OTHER, -1, -1, "Open url " + u);
                }
            }
        } catch (MalformedURLException e) {
            if (DebugUtil.DEBUGGING) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        final JeliotDebugWS jeliot = new JeliotDebugWS();
        LoadJeliot.start(jeliot);
        jeliot.handleArgs(args);
    }

    public boolean hasIOImport(String src) {
        Pattern p = Pattern.compile("import\\s+Prog1Tools\\.(\\*|IOTools);");
        return p.matcher(src).find();
    }
}
