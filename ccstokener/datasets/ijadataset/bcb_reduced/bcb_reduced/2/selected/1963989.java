package geomss.app;

import java.util.List;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.Frame;
import java.awt.FileDialog;
import java.net.URL;
import java.awt.event.*;
import java.io.*;
import com.centerkey.utils.BareBonesBrowserLaunch;
import net.roydesign.app.QuitJMenuItem;
import net.roydesign.app.AboutJMenuItem;
import net.roydesign.app.PreferencesJMenuItem;
import net.roydesign.ui.StandardMacAboutFrame;
import jahuwaldt.io.ExtFilenameFilter;
import jahuwaldt.swing.MDIApplication;
import jahuwaldt.swing.AppUtilities;
import geomss.geom.reader.GeomReader;
import geomss.geom.reader.GeomReaderFactory;

/**
*  This class represents a multi-document interface GUI application
*  for use in this program.
*
*  <p>  Modified by:  Joseph A. Huwaldt   </p>
*
*  @author  Joseph A. Huwaldt   Date: May 2, 2009
*  @version August 8, 2011
**/
public class GeomSSGUI extends MDIApplication {

    private ExtFilenameFilter fnFilter = new ExtFilenameFilter();

    private final GeomSS app;

    /**
	*  Constructor for our application that displays the GUI.
	**/
    public GeomSSGUI(ResourceBundle resBundle, GeomSS application) throws Exception {
        this.app = application;
        AppUtilities.setSystemLAF();
        setResourceBundle(resBundle);
        setName(getResourceBundle().getString("appName"));
        GeomReader[] allReaders = GeomReaderFactory.getAllReaders();
        if (allReaders != null) {
            int numReaders = allReaders.length;
            for (int i = 0; i < numReaders; ++i) fnFilter.addExtension(allReaders[i].getExtension());
            fnFilter.addExtension("geo");
        }
        JMenuBar menuBar = createMenuBar();
        setFramelessJMenuBar(menuBar);
    }

    /**
	*  Handle the user choosing the "New..." from the File
	*  menu.  Creates and returns a reference to a new document window.
	**/
    public Frame handleNew(ActionEvent event) {
        Frame window = null;
        try {
            window = MainWindow.newAppWindow(null, null, app);
            window.setVisible(true);
            this.addWindow(window);
        } catch (Exception e) {
            ResourceBundle resBundle = getResourceBundle();
            AppUtilities.showException(null, resBundle.getString("unexpectedTitle"), resBundle.getString("unexpectedMsg"), e);
            e.printStackTrace();
        }
        return window;
    }

    /**
	*  Handles the user choosing "Open" from the File menu.
	*  Allows the user to choose a geometry file to open.
	**/
    public void handleOpen(ActionEvent event) {
        ResourceBundle resBundle = getResourceBundle();
        try {
            String dir = app.getPreferences().getLastPath();
            File theFile = AppUtilities.selectFile(null, FileDialog.LOAD, resBundle.getString("fileDialogLoad"), dir, null, getFilenameFilter());
            if (theFile == null) return;
            MainWindow.newWindowFromDataFile(app, null, theFile);
        } catch (Exception e) {
            AppUtilities.showException(null, resBundle.getString("unexpectedTitle"), resBundle.getString("unexpectedMsg"), e);
            e.printStackTrace();
        }
    }

    /**
	*  Return a reference to this application's file name filter.
	**/
    public FilenameFilter getFilenameFilter() {
        return fnFilter;
    }

    /**
	*  Create an about menu item for use in this application.
	**/
    public AboutJMenuItem createAboutMenuItem() {
        AboutJMenuItem about = this.getAboutJMenuItem();
        about.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ResourceBundle resBundle = getResourceBundle();
                Icon appIcon = null;
                try {
                    URL imgURL = ClassLoader.getSystemResource(resBundle.getString("applicationIconURL"));
                    appIcon = new ImageIcon(imgURL);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if (appIcon == null) appIcon = UIManager.getIcon("OptionPane.informationIcon");
                String credits = readAboutText(resBundle.getString("aboutTextURLStr"));
                String aName = resBundle.getString("appName");
                String aVersion = resBundle.getString("appVersion");
                String aModDate = resBundle.getString("appModDate");
                String copyright = resBundle.getString("copyright");
                StandardMacAboutFrame f = new StandardMacAboutFrame(aName, aVersion + " - " + aModDate);
                f.setApplicationIcon(appIcon);
                f.setCopyright(copyright);
                f.setCredits(credits, "text/html");
                f.setHyperlinkListener(new HyperlinkListener() {

                    public void hyperlinkUpdate(HyperlinkEvent e) {
                        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                            BareBonesBrowserLaunch.openURL(e.getURL().toString());
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
            URL url = ClassLoader.getSystemResource(urlStr);
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
            text = getResourceBundle().getString("standardCreditsMsg");
        }
        return text;
    }

    /**
	*  Initializes and displays the menus associated with this application.
	**/
    private JMenuBar createMenuBar() throws NoSuchMethodException {
        ResourceBundle resBundle = getResourceBundle();
        JMenuBar menuBar = new JMenuBar();
        List<String[]> menuStrings = new ArrayList();
        String[] row = new String[3];
        row[0] = resBundle.getString("newItemText");
        row[1] = resBundle.getString("newItemKey");
        row[2] = "handleNew";
        menuStrings.add(row);
        row = new String[3];
        row[0] = resBundle.getString("openItemText");
        row[1] = resBundle.getString("openItemKey");
        row[2] = "handleOpen";
        menuStrings.add(row);
        menuStrings.add(new String[3]);
        row = new String[3];
        row[0] = resBundle.getString("closeItemText");
        row[1] = resBundle.getString("closeItemKey");
        row[2] = "handleClose";
        menuStrings.add(row);
        row = new String[3];
        row[0] = resBundle.getString("saveItemText");
        row[1] = resBundle.getString("saveItemKey");
        row[2] = null;
        menuStrings.add(row);
        row = new String[3];
        row[0] = resBundle.getString("saveAsItemText");
        row[1] = null;
        row[2] = null;
        menuStrings.add(row);
        JMenu menu = AppUtilities.buildMenu(this, resBundle.getString("fileMenuText"), menuStrings);
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
        menuStrings.clear();
        row = new String[3];
        row[0] = resBundle.getString("undoItemText");
        row[1] = resBundle.getString("undoItemKey");
        row[2] = null;
        menuStrings.add(row);
        row = new String[3];
        row[0] = resBundle.getString("redoItemText");
        row[1] = null;
        row[2] = null;
        menuStrings.add(row);
        menuStrings.add(new String[3]);
        row = new String[3];
        row[0] = resBundle.getString("cutItemText");
        row[1] = resBundle.getString("cutItemKey");
        row[2] = null;
        menuStrings.add(row);
        row = new String[3];
        row[0] = resBundle.getString("copyItemText");
        row[1] = resBundle.getString("copyItemKey");
        row[2] = null;
        menuStrings.add(row);
        row = new String[3];
        row[0] = resBundle.getString("pasteItemText");
        row[1] = resBundle.getString("pasteItemKey");
        row[2] = null;
        menuStrings.add(row);
        menu = AppUtilities.buildMenu(this, resBundle.getString("editMenuText"), menuStrings);
        menuBar.add(menu);
        PreferencesJMenuItem preferences = this.getPreferencesJMenuItem();
        preferences.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                app.getPreferences().showPreferenceDialog();
            }
        });
        if (!preferences.isAutomaticallyPresent()) {
            menu.addSeparator();
            menu.add(preferences);
        }
        AboutJMenuItem about = createAboutMenuItem();
        if (!about.isAutomaticallyPresent()) {
            menu = new JMenu(resBundle.getString("helpMenuText"));
            menuBar.add(menu);
            menu.add(about);
        }
        return menuBar;
    }
}
