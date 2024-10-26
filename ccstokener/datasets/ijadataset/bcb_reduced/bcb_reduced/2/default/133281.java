import java.net.URL;
import java.net.MalformedURLException;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JMenuBar;
import javax.swing.Icon;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.JOptionPane;
import com.Ostermiller.util.Browser;
import net.roydesign.mac.MRJAdapter;
import net.roydesign.ui.StandardMacAboutFrame;
import net.roydesign.app.QuitJMenuItem;
import net.roydesign.app.AboutJMenuItem;
import net.roydesign.app.PreferencesJMenuItem;
import jahuwaldt.io.ExtFilenameFilter;
import jahuwaldt.swing.MDIApplication;
import jahuwaldt.swing.AppUtilities;
import jahuwaldt.swing.SplashScreen;
import jahuwaldt.maptools.LayerReaderFactory;
import jahuwaldt.maptools.LayerReader;

/**
*  An application for reading and working with USGS UTM map data.
*
*  <p>  Modified by:  Joseph A. Huwaldt    </p>
*
*  @author    Joseph A. Huwaldt    Date:  January 26, 2000
*  @version   April 3, 2010
**/
public class VHEditor extends VHApplication {

    public static final String aName = "VH Editor";

    public static final String aVersion = "0.0.4 b1";

    public static final String aAuthor = "Joseph A. Huwaldt";

    public static final String aModDate = "April 3, 2010";

    public static final String cwYear = "2000-2010";

    private static final String kAboutTextURLStr = "AboutTextVHEditor.html";

    private static final String kSplashImg = "images/Editor-Splash.jpg";

    /**
	*  Definitions for our window's menus.
	**/
    private static final String[][] fileMStrings = { { "New", "N", "handleNew" }, { "Open...", "O", "handleOpen" }, { null, null, null }, { "Close", "W", "handleClose" }, { "Save", "S", null }, { "Save As...", null, null } };

    private static final String[][] editMStrings = { { "Undo", "Z", null }, { null, null, null }, { "Cut", "X", null }, { "Copy", "C", null }, { "Paste", "V", null } };

    /**
	*  Main method for this program.  This is where the program starts.
	**/
    public static void main(String[] args) {
        try {
            URL resource = VHEditor.class.getResource(kSplashImg);
            SplashScreen splash = new SplashScreen(resource, null, SplashScreen.ONE_SECOND * 4);
            new VHEditor();
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Could not find the following file:", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            System.exit(0);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            AppUtilities.showException(null, "No Such Method Error", "Copy the following message and e-mail it to the author:", e);
            System.exit(0);
        } catch (Throwable e) {
            e.printStackTrace();
            AppUtilities.showException(null, "Unexpected Error", "Copy the following message and e-mail it to the author:", e);
            System.exit(0);
        }
    }

    /**
	*  Constructs an application instance and opens an empty window.
	**/
    public VHEditor() throws MalformedURLException, NoSuchMethodException, FileNotFoundException {
        super(aName);
        JMenuBar menuBar = createMenuBar();
        setFramelessJMenuBar(menuBar);
        Frame window = new EditorWindow("Untitled", this);
        window.setVisible(true);
        this.addWindow(window);
    }

    /**
	*  Handle the user choosing "New..." from the File
	*  menu.  Creates an empty map window, shows it, and returns a
	*  reference to it.
	**/
    public Frame handleNew(ActionEvent event) {
        Frame window = null;
        try {
            window = new EditorWindow("Untitled", this);
            window.setVisible(true);
            this.addWindow(window);
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Could not find the following file:", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            AppUtilities.showException(null, "No Such Method Error", "Copy the following message and e-mail it to the author:", e);
        } catch (Throwable e) {
            AppUtilities.showException(null, "Initialization Error", "Copy the following message and e-mail it to the author:", e);
            e.printStackTrace();
        }
        return window;
    }

    /**
	*  Handle the user choosing "Open..." from the File menu.
	*  Lets the user choose a map file and open it.
	**/
    public void handleOpen(ActionEvent event) {
        MapWindow window = (MapWindow) this.handleNew(event);
        if (window == null) return;
        window.handleOpen(event);
    }

    /**
	*  Create an about menu item for use in this application.
	**/
    public AboutJMenuItem createAboutMenuItem() {
        AboutJMenuItem about = this.getAboutJMenuItem();
        about.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Icon appIcon = UIManager.getIcon("OptionPane.informationIcon");
                String credits = readAboutText(kAboutTextURLStr);
                StandardMacAboutFrame f = new StandardMacAboutFrame(aName, aVersion);
                f.setApplicationIcon(appIcon);
                f.setCopyright("Copyright " + cwYear + ", " + aAuthor);
                f.setCredits(credits, "text/html");
                f.setHyperlinkListener(new HyperlinkListener() {

                    public void hyperlinkUpdate(HyperlinkEvent e) {
                        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                            try {
                                Browser.displayURL(e.getURL().toString());
                            } catch (Exception ex) {
                                AppUtilities.showException(null, "Unexpected Error", "Copy the following message and e-mail it to the author:", ex);
                                ex.printStackTrace();
                            }
                        }
                    }
                });
                f.setVisible(true);
            }
        });
        return about;
    }

    /**
	*  Method that handles reading in the contents of the text region of the about box from a text file.
	**/
    private String readAboutText(String urlStr) {
        String text = null;
        try {
            URL url = this.getClass().getResource(urlStr);
            InputStreamReader reader = new InputStreamReader(url.openStream());
            StringWriter writer = new StringWriter();
            int character = reader.read();
            while (character != -1) {
                writer.write(character);
                character = reader.read();
            }
            text = writer.toString();
        } catch (IOException e) {
            e.printStackTrace();
            text = "<html><body><b>Author:</b><br>" + aAuthor + "<br>";
            text += "<a href=\"mailto:jhuwaldt@mac.com\">jhuwaldt@mac.com</a><br>";
            text += "<P ALIGN=CENTER><BR>" + aName + " comes with ABSOLUTELY NO WARRANTY;";
            text += "<BR>This is free software, and you are welcome to redistribute ";
            text += "it and it's source code under certain conditions.";
            text += "<BR>Source code is available at:";
            text += "<BR><a href=\"http://virtualhiker.sf.net/\">";
            text += "http://virtualhiker.sf.net/</a></P>";
            text += "</body></html>";
        }
        return text;
    }

    /**
	*  Initializes the menus associated with this application.
	**/
    public JMenuBar createMenuBar() throws NoSuchMethodException {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = menu = AppUtilities.buildMenu(this, "File", fileMStrings);
        menuBar.add(menu);
        QuitJMenuItem quit = this.getQuitJMenuItem();
        quit.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                handleQuit(e);
            }
        });
        if (!quit.isAutomaticallyPresent()) {
            menu.addSeparator();
            menu.add(quit);
        }
        menu = AppUtilities.buildMenu(this, "Edit", editMStrings);
        menuBar.add(menu);
        menu = this.newWindowsMenu("Windows");
        menuBar.add(menu);
        AboutJMenuItem about = createAboutMenuItem();
        if (!about.isAutomaticallyPresent()) {
            menu = new JMenu("Help");
            menuBar.add(menu);
            menu.add(about);
        }
        return menuBar;
    }
}
