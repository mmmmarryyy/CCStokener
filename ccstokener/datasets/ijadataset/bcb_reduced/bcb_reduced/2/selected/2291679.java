package gov.sns.apps.launcher;

import gov.sns.application.*;
import java.net.*;

/**
 * Main is the ApplicationAdaptor for the Launcher application.
 *
 * @author t6p
 */
public class Main extends ApplicationAdaptor {

    /**
     * Returns the text file suffixes of files this application can open.
     * @return Suffixes of readable files
     */
    @Override
    public String[] readableDocumentTypes() {
        return new String[] { "launch" };
    }

    /**
     * Returns the text file suffixes of files this application can write.
     * @return Suffixes of writable files
     */
    @Override
    public String[] writableDocumentTypes() {
        return new String[] { "launch" };
    }

    /**
     * Implement this method to return an instance of my custom document.
     * @return An instance of my custom document.
     */
    @Override
    public XalDocument newEmptyDocument() {
        return new LaunchDocument();
    }

    /**
     * Implement this method to return an instance of my custom document 
     * corresponding to the specified URL.
     * @param url The URL of the file to open.
     * @return An instance of my custom document.
     */
    @Override
    public XalDocument newDocument(java.net.URL url) {
        return new LaunchDocument(url);
    }

    /**
     * Specifies the name of my application.
     * @return Name of my application.
     */
    @Override
    public String applicationName() {
        return "Launcher";
    }

    /** 
     * Capture the application launched event and print it.  This is an optional
     * hook that can be used to do something useful at the end of the application launch.
     */
    @Override
    public void applicationFinishedLaunching() {
        System.out.println("Launcher running...");
    }

    /**
     * Constructor
     */
    public Main() {
    }

    /**
     * Edit application preferences.
     * @param document The document where to show the preference panel.
     */
    @Override
    public void editPreferences(XalDocument document) {
        PreferenceController.displayPathPreferenceSelector(document.getMainWindow());
    }

    /** The main method of the application. */
    public static void main(String[] args) {
        try {
            System.out.println("Starting Launcher...");
            boolean openDefaultDocument = !Boolean.getBoolean("SAFE_MODE");
            URL url = null;
            if (openDefaultDocument) {
                try {
                    url = PreferenceController.getDefaultDocumentURL();
                    openDefaultDocument = url != null;
                    if (openDefaultDocument) {
                        int contentLength = url.openConnection().getContentLength();
                        openDefaultDocument = contentLength > 0;
                        if (!openDefaultDocument) {
                            throw new RuntimeException("Contents of \"" + url + "\" is missing.");
                        }
                    }
                } catch (MalformedURLException exception) {
                    openDefaultDocument = false;
                    System.err.println(exception);
                } catch (Exception exception) {
                    openDefaultDocument = false;
                    System.err.println(exception);
                    System.err.println("Default document \"" + url + "\" cannot be openned.");
                    Application.displayApplicationError("Launch Exception", "Default document \"" + url + "\" cannot be openned.", exception);
                }
            }
            URL[] urls = (openDefaultDocument) ? new URL[] { url } : new URL[] {};
            Application.launch(new Main(), urls);
        } catch (Exception exception) {
            System.err.println(exception.getMessage());
            exception.printStackTrace();
            Application.displayApplicationError("Launch Exception", "Launch Exception", exception);
            System.exit(-1);
        }
    }
}
