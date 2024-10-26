package de.guidoludwig.jtrade.install;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import de.guidoludwig.jtrade.ErrorMessage;
import de.guidoludwig.jtrade.JTrade;
import de.guidoludwig.jtrade.Splash;
import de.guidoludwig.jtrade.db4o.JTDB;
import de.guidoludwig.jtrade.domain.AllArchivesModel;
import de.guidoludwig.jtrade.domain.Archive;
import de.guidoludwig.jtrade.domain.Artist;
import de.guidoludwig.jtrade.domain.Show;
import de.guidoludwig.jtrade.ui.AbstractDialog;
import de.guidoludwig.jtrade.util.JTradeUtil;
import de.guidoludwig.jtrade.xml.IO;

/**
 * Utility to check and perform installation steps
 * 
 * @author <a href="mailto:jtrade@gigabss.de">Guido Ludwig</a>
 * @version $Revision: 1.14 $
 */
public class Installer {

    private static Installer INSTANCE;

    public static Installer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Installer();
        }
        return INSTANCE;
    }

    private Installer() {
    }

    /**
     * Ensure that JTrade is completely installed.
     * Directories are created, 
     * Missing resources are copied into the corresponding folders
     *
     */
    public void completeInstall() {
        Preferences prefs = Preferences.userNodeForPackage(JTrade.class);
        String accepted = prefs.get(Version.VERSION, Boolean.FALSE.toString());
        if (Boolean.valueOf(accepted) != Boolean.TRUE) {
            LicenceDialog dialog = new LicenceDialog();
            dialog.open();
            if (!dialog.hasBeenCanceled()) {
                prefs.put(Version.VERSION, Boolean.TRUE.toString());
            } else {
                System.exit(1);
            }
        }
        ensureDirectory(JTradeProperties.INSTANCE.getProperty(JTradeProperties.DATA_DIRECTORY));
        ensureDirectory(JTradeProperties.INSTANCE.getProperty(JTradeProperties.HTML_OUTPUT_DIRECTORY));
        installDTD();
        installCSS();
        ensureDB4O();
    }

    private void ensureDB4O() {
        Splash.setMessage("Checking db4o");
        List<Artist> bydb = JTDB.INSTANCE.getAllArtists();
        if (bydb.size() != 0) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Update from XML skipped as db4o already contains " + bydb.size() + " artists");
            return;
        }
        JTDB.INSTANCE.setRunInEDT(true);
        Splash.setMessage("Update db4o from previous xml");
        IO.read();
        Splash.setMessage("Update from XML : " + IO.readArtists.size() + " Artists");
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Found  " + IO.readArtists.size() + " Artists in xml");
        for (Artist artist : IO.readArtists) {
            JTDB.INSTANCE.store(artist);
            for (Show show : artist.getShows()) {
                JTDB.INSTANCE.store(show);
            }
        }
        Splash.setMessage("Update from XML : " + AllArchivesModel.getInstance().getList().size() + " Archives");
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Found  " + AllArchivesModel.getInstance().getList().size() + " Archives in xml");
        for (Archive archive : AllArchivesModel.getInstance().getList()) {
            JTDB.INSTANCE.store(archive);
        }
        AllArchivesModel.getInstance().removeAll();
        JTDB.INSTANCE.setRunInEDT(false);
    }

    /**
     * Ensures the given directory exists.
     * The directory is created if not
     * @param directoryName
     */
    private void ensureDirectory(String directoryName) {
        File directory = new File(directoryName);
        if (!directory.exists()) {
            directory.mkdir();
        }
    }

    /**
     * Install the current DTD
     */
    private void installDTD() {
        SingleFileInstaller inst = new SingleFileInstaller(Version.DTD, "resources/xml", JTradeProperties.INSTANCE.getProperty(JTradeProperties.DATA_DIRECTORY));
        try {
            inst.installResource(false);
        } catch (IOException e) {
            ErrorMessage.handle(e);
        }
    }

    /**
     * Install the current DTD
     * FIXME : reflect user Settings !
     */
    private void installCSS() {
        SingleFileInstaller inst = new SingleFileInstaller("tradelist.css", "resources/xml", JTradeProperties.INSTANCE.getProperty(JTradeProperties.HTML_OUTPUT_DIRECTORY));
        try {
            inst.installResource(false);
        } catch (IOException e) {
            ErrorMessage.handle(e);
        }
    }

    public static void main(String[] args) {
        try {
            Preferences.userNodeForPackage(JTrade.class).clear();
        } catch (BackingStoreException e) {
            ErrorMessage.handle(e);
        }
    }

    private class LicenceDialog extends AbstractDialog {

        /**
		 * 
		 */
        private static final long serialVersionUID = 1L;

        private static final String TITLE = "licence.dialog.title";

        private LicenceDialog() {
            super(TITLE);
        }

        @Override
        protected JComponent buildContent() {
            JPanel panel = new JPanel(new BorderLayout());
            JTextArea ta = new JTextArea(JTradeUtil.loadResource("licence/LICENCE.txt"));
            ta.setEditable(false);
            panel.add(new JScrollPane(ta), BorderLayout.CENTER);
            panel.add(buildButtonBarWithOKCancel(), BorderLayout.SOUTH);
            return panel;
        }
    }

    private static class SingleFileInstaller {

        private String resource;

        private String resourcePrefix;

        private String outputDirectory;

        /**
         * Create an Installer that checks for teh existance of a file.
         * @param resource the file's name
         * @param prefix the location in the resources where the file can be found
         * @param outDir the directory where to install the file
         */
        public SingleFileInstaller(String resource, String prefix, String outDir) {
            this.resource = resource;
            resourcePrefix = (prefix == null ? "" : prefix);
            if (!this.resourcePrefix.endsWith(File.separator) && prefix.length() > 0) {
                this.resourcePrefix = this.resourcePrefix + "/";
            }
            outputDirectory = outDir;
        }

        private void createOutputDirectory() {
            File dir = new File(outputDirectory);
            if (!dir.exists()) {
                dir.mkdir();
            }
        }

        /**
         * Install the resource.
         * If the source is not found, or the overwrite flag is set,
         * it is copied from the corresponding
         * resource in the resource part of the jar.
         * @param overwrite
         * @throws IOException if the resource could not be opened
         */
        public void installResource(boolean overwrite) throws IOException {
            createOutputDirectory();
            File file = new File(outputDirectory + "/" + resource);
            if (!file.exists() || overwrite) {
                String fullResource = resourcePrefix + resource;
                URL url = ClassLoader.getSystemResource(fullResource);
                if (url == null) {
                    throw new IOException("Resource '" + fullResource + "' not found !");
                }
                InputStream reader = url.openStream();
                if (file.createNewFile()) {
                    FileOutputStream writer = new FileOutputStream(file);
                    int b;
                    while ((b = reader.read()) >= 0) {
                        writer.write(b);
                    }
                    writer.close();
                } else {
                    throw new IOException("file to be created already exists or can not be created ");
                }
            } else {
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).finer("File exists, not overwritten");
            }
        }
    }
}
