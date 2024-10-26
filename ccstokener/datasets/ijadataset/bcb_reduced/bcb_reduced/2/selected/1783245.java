package net.xan.taskstack;

import java.awt.Desktop;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.EventObject;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import net.xan.jutils.preferences.GlobalPreferencesManager;
import net.xan.jutils.properties.GlobalPropertiesManager;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 */
public class TaskStackApp extends SingleFrameApplication {

    TaskStackView window = null;

    /**
	 * At startup create and show the main frame of the application.
	 */
    @Override
    protected void startup() {
        checkForNewVersion();
        loadUserPreferences();
        loadApplicationProperties();
        try {
            this.window = new TaskStackView(this);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        show(this.window);
    }

    private void loadApplicationProperties() {
        try {
            org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(net.xan.taskstack.TaskStackApp.class).getContext().getResourceMap(NewTaskDialog.class);
            String preferencesFile = resourceMap.getString("Application.userPropertiesFilePath");
            URL resource = getClass().getResource(preferencesFile);
            GlobalPropertiesManager.init(resource.getPath(), resourceMap);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private void loadUserPreferences() {
        try {
            GlobalPreferencesManager.init(TaskStackApp.class);
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }
    }

    private void checkForNewVersion() {
        try {
            org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(net.xan.taskstack.TaskStackApp.class).getContext().getResourceMap(NewTaskDialog.class);
            String versionUrl = resourceMap.getString("Application.versionFileUrl");
            long startTime = System.currentTimeMillis();
            System.out.println("Retrieving version file from\n" + versionUrl);
            URL url = new URL(versionUrl);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            while ((str = in.readLine()) != null) {
                if (str.startsWith("LastVersion")) {
                    String remoteVersion = str.substring(str.indexOf("=") + 1);
                    String localVersion = resourceMap.getString("Application.version");
                    System.out.println("Version file found");
                    System.out.println("Local version: " + localVersion);
                    System.out.println("Remote version: " + remoteVersion);
                    if (remoteVersion.compareTo(localVersion) > 0) {
                        askDownloadNewVersion(remoteVersion, localVersion);
                    }
                    break;
                }
            }
            long endTime = System.currentTimeMillis();
            System.out.println("Elapsed time " + (endTime - startTime) + "ms");
            in.close();
        } catch (MalformedURLException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * @param version
	 * @param currentVersion
	 */
    private boolean askDownloadNewVersion(String lastVersion, String userVersion) {
        boolean answer = 0 == JOptionPane.showConfirmDialog(null, "A new version has been found. Your current version is " + userVersion + "\n" + "but the last available version is " + lastVersion + ".\n" + "\nDo you want go to the project home to download the newest version?.\n", "New version available", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer) {
            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                System.err.println("Desktop doesn't support the browse action (fatal)");
                System.exit(1);
            }
            try {
                String taskStackHomeUrl = GlobalPropertiesManager.getProperty("taskstackhomeurl");
                java.net.URI uri = new java.net.URI(taskStackHomeUrl);
                desktop.browse(uri);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
        return answer;
    }

    /**
	 * This method is to initialize the specified window by injecting resources.
	 * Windows shown in our application come fully initialized from the GUI
	 * builder, so this additional configuration is not needed.
	 */
    @Override
    protected void configureWindow(final java.awt.Window root) {
        root.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent we) {
                TSSystemTrayIcon.getInstance().show();
                TSSystemTrayIcon.getInstance().setMainWindow(TaskStackApp.this.window);
                TaskStackApp.this.getMainFrame().setVisible(false);
            }

            @Override
            public void windowOpened(WindowEvent arg0) {
            }

            @Override
            public void windowClosed(WindowEvent arg0) {
            }

            @Override
            public void windowIconified(WindowEvent arg0) {
            }

            @Override
            public void windowDeiconified(WindowEvent arg0) {
            }

            @Override
            public void windowActivated(WindowEvent arg0) {
            }

            @Override
            public void windowDeactivated(WindowEvent arg0) {
            }
        });
    }

    /**
	 * called after mainframelisteners
	 */
    @Override
    protected void shutdown() {
        super.shutdown();
    }

    @Override
    protected void end() {
        this.window.deleteTemporaryFiles();
        super.end();
    }

    @Override
    public void exit(EventObject arg0) {
        if (arg0 instanceof WindowEvent && ((WindowEvent) arg0).getID() == WindowEvent.WINDOW_CLOSING && ((WindowEvent) arg0).getSource() instanceof JFrame) {
            return;
        }
        super.exit(arg0);
    }

    /**
	 * A convenient static getter for the application instance.
	 * 
	 * @return the instance of TaskStackApp
	 */
    public static TaskStackApp getApplication() {
        return Application.getInstance(TaskStackApp.class);
    }

    /**
	 * Main method launching the application.
	 */
    public static void main(String[] args) {
        launch(TaskStackApp.class, args);
    }
}
