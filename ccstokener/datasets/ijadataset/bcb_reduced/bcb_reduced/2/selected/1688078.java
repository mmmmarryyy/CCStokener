package jeliot;

import java.io.BufferedReader;
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
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import jeliot.gui.LoadJeliot;
import jeliot.util.DebugUtil;
import com.incors.plaf.kunststoff.KunststoffLookAndFeel;

/**
 * This is an extension of the application class of Jeliot 3 that
 * adds features for JavaWS and Url Loading
 *
 * @author Roland K�stermann
 * @author amoreno
 */
public class MoodleJeliot extends Jeliot {

    public MoodleJeliot() {
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

    {
        try {
            UIManager.setLookAndFeel(new KunststoffLookAndFeel());
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException e1) {
            } catch (InstantiationException e1) {
            } catch (IllegalAccessException e1) {
            } catch (UnsupportedLookAndFeelException e1) {
            }
        }
    }

    /**
     * get Program from url
     */
    public void setProgram(final URL u, final String cookie) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                try {
                    System.out.println("Reading with cookies from u = " + u);
                    URLConnection urlConn = u.openConnection();
                    urlConn.setRequestProperty("Cookie", cookie);
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
            if (args.length >= 2) {
                URL u = null;
                String cookie = "";
                try {
                    u = new URL(URLDecoder.decode(args[0], "UTF-8"));
                    cookie = URLDecoder.decode(args[1], "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    if (DebugUtil.DEBUGGING) e.printStackTrace();
                }
                setProgram(u, cookie);
            }
            if (args.length > 2) {
                super.userName = args[2];
            }
            if (args.length > 3) {
                if (args[3].equals("1")) {
                    super.setAskingQuestions(true);
                } else {
                    super.setAskingQuestions(false);
                }
            }
        } catch (MalformedURLException e) {
            if (DebugUtil.DEBUGGING) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        final MoodleJeliot jeliot = new MoodleJeliot();
        LoadJeliot.start(jeliot);
        jeliot.handleArgs(args);
    }

    public boolean hasIOImport(String src) {
        Pattern p = Pattern.compile("import\\s+jeliot\\.(\\*|io\\.\\*);");
        return p.matcher(src).find();
    }
}
