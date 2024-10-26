package edu.ucsd.ncmir.jinx;

import edu.ucsd.ncmir.jinx.core.JxAutosaveHandler;
import edu.sdsc.grid.io.GeneralFile;
import edu.ucsd.ncmir.asynchronous_event.AbstractAsynchronousEventListener;
import edu.ucsd.ncmir.asynchronous_event.AsynchronousEvent;
import edu.ucsd.ncmir.asynchronous_event.AsynchronousEventManager;
import edu.ucsd.ncmir.jinx.contour_edit.listeners.JxIntersectionAbutAgainstOthersEventListener;
import edu.ucsd.ncmir.jinx.contour_edit.listeners.JxIntersectionAbutThatEventListener;
import edu.ucsd.ncmir.jinx.contour_edit.listeners.JxIntersectionAbutThisEventListener;
import edu.ucsd.ncmir.jinx.contour_edit.listeners.JxIntersectionClipAgainstOthersEventListener;
import edu.ucsd.ncmir.jinx.contour_edit.listeners.JxIntersectionClipThatEventListener;
import edu.ucsd.ncmir.jinx.contour_edit.listeners.JxIntersectionClipThisEventListener;
import edu.ucsd.ncmir.jinx.contour_edit.listeners.JxIntersectionCorrectInnerEventListener;
import edu.ucsd.ncmir.jinx.contour_edit.listeners.JxIntersectionCorrectOuterEventListener;
import edu.ucsd.ncmir.jinx.contour_edit.listeners.JxIntersectionDyadicNoopEventListener;
import edu.ucsd.ncmir.jinx.contour_edit.listeners.JxIntersectionMergeInnerEventListener;
import edu.ucsd.ncmir.jinx.contour_edit.listeners.JxIntersectionMergeOuterEventListener;
import edu.ucsd.ncmir.jinx.contour_edit.listeners.JxIntersectionMultipleNoopEventListener;
import edu.ucsd.ncmir.jinx.events.JxAutosaveUpdateEvent;
import edu.ucsd.ncmir.jinx.events.JxErrorEvent;
import edu.ucsd.ncmir.jinx.events.JxGetLogDirectoryEvent;
import edu.ucsd.ncmir.jinx.events.JxLoaderEvent;
import edu.ucsd.ncmir.jinx.events.JxLogEvent;
import edu.ucsd.ncmir.jinx.events.JxSessionLoaderEvent;
import edu.ucsd.ncmir.jinx.events.JxVolumeLoaderEvent;
import edu.ucsd.ncmir.jinx.gui.JxEventFileCleanupDialog;
import edu.ucsd.ncmir.jinx.gui.JxMainFrame;
import edu.ucsd.ncmir.jinx.listeners.JxDeleteContourEventListener;
import edu.ucsd.ncmir.jinx.listeners.JxFileChooserEventListener;
import edu.ucsd.ncmir.jinx.listeners.JxFileSaverEventListener;
import edu.ucsd.ncmir.jinx.listeners.JxLogEventListener;
import edu.ucsd.ncmir.jinx.listeners.JxPrintEventListener;
import edu.ucsd.ncmir.jinx.listeners.JxQuitEventListener;
import edu.ucsd.ncmir.jinx.listeners.JxSaveTextEventListener;
import edu.ucsd.ncmir.jinx.listeners.JxScheduleEventListener;
import edu.ucsd.ncmir.jinx.loader.JxSessionLoaderEventListener;
import edu.ucsd.ncmir.jinx.listeners.JxTraceAreaZeroEventListener;
import edu.ucsd.ncmir.jinx.listeners.gui.JxConfirmCancelEventListener;
import edu.ucsd.ncmir.jinx.listeners.gui.JxConfirmEventListener;
import edu.ucsd.ncmir.jinx.loader.JxVolumeLoaderEventListener;
import edu.ucsd.ncmir.jinx.listeners.gui.object_info.JxObjectInfoPrintEventListener;
import edu.ucsd.ncmir.jinx.listeners.gui.object_info.JxObjectInfoSaveEventListener;
import edu.ucsd.ncmir.jinx.segmentation.contours.listeners.JxContourExportRequestEventListener;
import edu.ucsd.ncmir.jinx.segmentation.contours.listeners.JxExportContoursEventListener;
import edu.ucsd.ncmir.jinx.segmentation.surfacers.listeners.JxContoursSurfacerEventListener;
import edu.ucsd.ncmir.jinx.segmentation.surfacers.listeners.JxSurfaceExportRequestEventListener;
import edu.ucsd.ncmir.jinx.segmentation.surfacers.listeners.JxMarchingCubesSurfacerEventListener;
import edu.ucsd.ncmir.jinx.segmentation.surfacers.listeners.JxNuagesSurfacerEventListener;
import edu.ucsd.ncmir.jinx.segmentation.surfacers.listeners.JxSphereSurfacerEventListener;
import edu.ucsd.ncmir.jinx.segmentation.surfacers.listeners.JxThreadSurfacerEventListener;
import edu.ucsd.ncmir.spl.core.NamedThread;
import edu.ucsd.ncmir.spl.filesystem.GeneralFileFactory;
import edu.ucsd.ncmir.spl.minixml.Document;
import edu.ucsd.ncmir.spl.minixml.Element;
import edu.ucsd.ncmir.spl.minixml.Format;
import edu.ucsd.ncmir.spl.minixml.SAXBuilder;
import edu.ucsd.ncmir.spl.minixml.XMLOutputter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

/**
 *
 * @author spl
 */
public class Jinx {

    public static void main(String[] args) {
        LogManager lm = LogManager.getLogManager();
        Enumeration<String> names = lm.getLoggerNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            Logger l = lm.getLogger(name);
            l.setLevel(Level.OFF);
        }
        new Jinx(args);
    }

    private String _log_prefix = null;

    private Element _jinx_properties = null;

    private File _jinx_properties_file = null;

    /** Creates a new instance of Jinx
     * @param args Currently ignored.
     */
    public Jinx(String[] args) {
        String separator = System.getProperty("file.separator");
        String home = System.getProperty("user.home");
        String landf = UIManager.getSystemLookAndFeelClassName();
        Runtime.getRuntime().addShutdownHook(new ShutdownHandler(this));
        Timer timer = new Timer("Save Timer");
        timer.scheduleAtFixedRate(new JxAutosaveHandler(), 30000, 30000);
        this._log_prefix = AsynchronousEventManager.createLogPrefix("Jinx");
        AsynchronousEventManager.setDebugVerbose(true);
        AsynchronousEventManager.setDebugDepth(5);
        String prefs_log_dir = null;
        try {
            this._jinx_properties_file = new File(home + separator + ".jinx.properties");
            Document document = new SAXBuilder().build(this._jinx_properties_file);
            this._jinx_properties = document.getRootElement();
            prefs_log_dir = this.getLogDirectory();
            Element autosave = this._jinx_properties.getChild("autosave");
            JxAutosaveUpdateEvent aue = new JxAutosaveUpdateEvent();
            aue.setAutosaveOn(autosave.getAttribute("on").getBooleanValue());
            aue.setAutosaveFrequency(autosave.getAttribute("frequency").getIntValue());
            aue.setAutosaveDirectory(autosave.getAttributeValue("path"));
            aue.send();
        } catch (Exception e) {
            this._jinx_properties = new Element("Properties");
            Element autosave = new Element("autosave");
            this._jinx_properties.addContent(autosave);
            this.autosaveUpdateXML(true, 300, System.getProperty("user.dir"));
        }
        String[] directories = { prefs_log_dir, System.getProperty("jinx.logdir"), home, System.getProperty("user.dir") };
        String log_dir = null;
        for (int i = 0; i < directories.length; i++) if (directories[i] != null) try {
            log_dir = directories[i];
            String path = log_dir + separator + this._log_prefix + ".event";
            AsynchronousEventManager.setDebugFile(path);
            File[] list = new File(log_dir).listFiles(new ListFilter());
            if (list.length > 5) {
                Arrays.sort(list);
                ArrayList<File> temp = new ArrayList<File>();
                for (int l = 0; l < list.length - 1; l++) temp.add(list[l]);
                list = temp.toArray(new File[temp.size()]);
                new JxEventFileCleanupDialog(list);
            }
            break;
        } catch (FileNotFoundException fnfe) {
        }
        if (log_dir == null) {
            System.err.println("Unable to open log file.  Quitting.");
            System.exit(1);
        } else this.editLogDirectory(log_dir);
        AsynchronousEventManager.setDebug(true);
        new JxLogEvent().send("Jinx Version " + Jinx.getVersion() + " Deployed: " + JxDeployed.deployed());
        String hostname = "Unknown.";
        try {
            hostname = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException ex) {
            new JxLogEvent().send(ex);
        }
        new JxLogEvent().send("Host Name: " + hostname + " OS Name: " + System.getProperty("os.name") + " Version: " + System.getProperty("os.version") + " System Architecture: " + System.getProperty("os.arch"));
        new JxLogEvent().send("Look and Feel: " + landf);
        Runtime runtime = Runtime.getRuntime();
        long maxmem = runtime.maxMemory();
        long totmem = runtime.totalMemory();
        new JxLogEvent().send("Maximum Memory: " + maxmem + " " + (float) (maxmem / (1024.0 * 1024.0)) + "MB" + ", Total Memory:  " + totmem + " " + (float) (totmem / (1024.0 * 1024.0)) + "MB");
        new JxLogEvent().send("Processors: " + runtime.availableProcessors());
        Map<String, String> env = System.getenv();
        Set<String> keys = env.keySet();
        String environment = "";
        for (String key : keys) environment += "\n\t" + key + "\t'" + env.get(key) + "'";
        new JxLogEvent().send("environment:" + environment);
        Properties properties = System.getProperties();
        String props = "";
        for (Enumeration pn = properties.propertyNames(); pn.hasMoreElements(); ) {
            String name = pn.nextElement().toString();
            props += "\n\t" + name + "\t'" + properties.getProperty(name) + "'";
        }
        new JxLogEvent().send("properties:" + props);
        try {
            UIManager.setLookAndFeel(landf);
            ClassLoader cl = this.getClass().getClassLoader();
            String resources = "";
            for (Enumeration e = cl.getResources("META-INF/"); e.hasMoreElements(); ) {
                URL url = (URL) e.nextElement();
                JarURLConnection jc = (JarURLConnection) url.openConnection();
                JarFile jf = jc.getJarFile();
                for (Enumeration<JarEntry> ej = jf.entries(); ej.hasMoreElements(); ) {
                    JarEntry je = ej.nextElement();
                    if (je.toString().equals("META-INF/MANIFEST.MF")) {
                        String[] path = url.getFile().split("/");
                        String[] file = path[path.length - 2].split("!");
                        String date = new Date(je.getTime()).toString();
                        resources += "\n\t" + date + "\t\t" + file[0];
                        break;
                    }
                }
            }
            new JxLogEvent().send("resources:" + resources);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        this.launchListeners();
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        JxMainFrame main_window = new JxMainFrame();
        String host = "Unknown???";
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            new JxErrorEvent().send(ex);
        }
        new JxLogEvent().send("Startup on host " + host);
        String filename = home + separator + ".jinx";
        new JxFileChooserEventListener(main_window, filename + ".chooser").enable();
        if (System.getProperty("jinx.demo") != null) new LaunchDemo(); else {
            String session = System.getProperty("jinx.session");
            if (session != null) try {
                URI uri = new URI(session);
                GeneralFile file = GeneralFileFactory.createFile(uri);
                if ((file != null) && (file.exists())) {
                    JxLoaderEvent le;
                    if (session.endsWith(".jnx")) le = new JxSessionLoaderEvent(); else le = new JxVolumeLoaderEvent();
                    le.parseLocation(System.getProperty("jinx.location"));
                    le.parseSize(System.getProperty("jinx.size"));
                    le.parseObjectName(System.getProperty("jinx.object"));
                    le.parseQuaternion(System.getProperty("jinx.quaternion"));
                    le.parseView(System.getProperty("jinx.view"));
                    le.send(file);
                } else if (file == null) new JxErrorEvent().send("No file: bad uri? " + uri); else new JxErrorEvent().send("File " + uri + " doesn't exist.");
            } catch (Exception ex) {
                new JxErrorEvent().send(ex);
            }
        }
    }

    private void editLogDirectory(String log_dir) {
        this._jinx_properties.removeChild("log");
        Element log = new Element("log");
        log.setAttribute("dir", log_dir);
        this._jinx_properties.addContent(log);
    }

    private String getLogDirectory() {
        return this._jinx_properties.getChild("log").getAttributeValue("dir");
    }

    private class LaunchDemo extends NamedThread {

        LaunchDemo() {
            super("Launch Demo");
            this.start();
        }

        @Override
        public void run() {
            try {
                new JxLogEvent().send("Demo mode: " + "Loading data, please stand by.");
                File home_dir = new File(System.getProperty("user.home"));
                ClassLoader cl = this.getClass().getClassLoader();
                File rec_file = Jinx.copyFromJarToTempFile(cl, home_dir, "demo.rec");
                File jnx_file = Jinx.copyFromJarToTempFile(cl, home_dir, "demo.jnx");
                URI uri = jnx_file.toURI();
                GeneralFile file = GeneralFileFactory.createFile(uri);
                new JxSessionLoaderEvent().send(file);
            } catch (Exception ex) {
                new JxErrorEvent().send(ex);
            }
        }
    }

    private static File copyFromJarToTempFile(ClassLoader cl, File home_dir, String resource) throws IOException {
        InputStream input = cl.getResource(resource).openStream();
        String[] resource_parts = resource.split("\\.");
        File temp = File.createTempFile(resource_parts[0], "." + resource_parts[1], home_dir);
        new JxLogEvent().send("Uncompressing " + temp);
        temp.deleteOnExit();
        FileOutputStream output = new FileOutputStream(temp);
        byte[] scratch = new byte[10 * 1024 * 1024];
        int size;
        while ((size = input.read(scratch)) > 0) output.write(scratch, 0, size);
        input.close();
        output.close();
        new JxLogEvent().send("Uncompressing " + temp + "  complete.");
        return temp;
    }

    private class ListFilter implements FilenameFilter {

        private static final String KEY = "Jinx\\.\\d\\d\\d\\d\\." + "\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d\\d\\d\\.\\d\\d\\d\\.event";

        public boolean accept(File dir, String name) {
            return name.matches(KEY);
        }
    }

    private class JxLogDirectoryChangeEventListener extends AbstractAsynchronousEventListener {

        private Jinx _jinx;

        JxLogDirectoryChangeEventListener(Jinx jinx) {
            this._jinx = jinx;
        }

        public void handler(AsynchronousEvent event, Object object) {
            this._jinx.editLogDirectory((String) object);
        }
    }

    private class JxGetLogDirectoryEventListener extends AbstractAsynchronousEventListener {

        private Jinx _jinx;

        JxGetLogDirectoryEventListener(Jinx jinx) {
            this._jinx = jinx;
        }

        public void handler(AsynchronousEvent event, Object object) {
            JxGetLogDirectoryEvent glde = (JxGetLogDirectoryEvent) event;
            glde.setLogDirectory(this._jinx.getLogDirectory());
        }
    }

    private void launchListeners() {
        new JxQuitEventListener().enable();
        new JxLogEventListener().enable();
        new JxGetLogDirectoryEventListener(this).enable();
        new JxLogDirectoryChangeEventListener(this).enable();
        new JxAutosaveUpdateEventListener(this).enable();
        new JxPrintEventListener().enable();
        new JxSaveTextEventListener().enable();
        new JxConfirmEventListener().enable();
        new JxConfirmCancelEventListener().enable();
        new JxSessionLoaderEventListener().enable();
        new JxFileSaverEventListener().enable();
        new JxDeleteContourEventListener().enable();
        new JxObjectInfoSaveEventListener().enable();
        new JxObjectInfoPrintEventListener().enable();
        new JxNuagesSurfacerEventListener().enable();
        new JxMarchingCubesSurfacerEventListener().enable();
        new JxContoursSurfacerEventListener().enable();
        new JxSphereSurfacerEventListener().enable();
        new JxThreadSurfacerEventListener().enable();
        new JxContourExportRequestEventListener().enable();
        new JxExportContoursEventListener().enable();
        new JxTraceAreaZeroEventListener().enable();
        new JxScheduleEventListener().enable();
        new JxVolumeLoaderEventListener().enable();
        new JxSurfaceExportRequestEventListener().enable();
        new JxIntersectionCorrectInnerEventListener().enable();
        new JxIntersectionCorrectOuterEventListener().enable();
        new JxIntersectionMergeInnerEventListener().enable();
        new JxIntersectionMergeOuterEventListener().enable();
        new JxIntersectionClipThisEventListener().enable();
        new JxIntersectionClipThatEventListener().enable();
        new JxIntersectionAbutThisEventListener().enable();
        new JxIntersectionAbutThatEventListener().enable();
        new JxIntersectionClipAgainstOthersEventListener().enable();
        new JxIntersectionAbutAgainstOthersEventListener().enable();
        new JxIntersectionDyadicNoopEventListener().enable();
        new JxIntersectionMultipleNoopEventListener().enable();
    }

    private class JxAutosaveUpdateEventListener extends AbstractAsynchronousEventListener {

        private Jinx _jinx;

        JxAutosaveUpdateEventListener(Jinx jinx) {
            this._jinx = jinx;
        }

        public void handler(AsynchronousEvent event, Object object) {
            JxAutosaveUpdateEvent aue = (JxAutosaveUpdateEvent) event;
            this._jinx.autosaveUpdateXML(aue.autosaveOn(), aue.getAutosaveFrequency(), aue.getAutosaveDirectory());
        }
    }

    private void autosaveUpdateXML(boolean on, int frequency, String path) {
        Element autosave = this._jinx_properties.getChild("autosave");
        autosave.setAttribute("on", Boolean.toString(on));
        autosave.setAttribute("frequency", Integer.toString(frequency));
        autosave.setAttribute("path", path);
    }

    public static final String getVersion() {
        return Jinx.getMajorVersion() + "." + Jinx.getMinorVersion();
    }

    public static final String getMajorVersion() {
        return "1";
    }

    public static final String getMinorVersion() {
        return "8";
    }

    private class ShutdownHandler extends NamedThread {

        private Jinx _jinx;

        ShutdownHandler(Jinx jinx) {
            super("Shutdown Handler");
            this._jinx = jinx;
        }

        @Override
        public void run() {
            try {
                XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
                Document document = this._jinx._jinx_properties.getDocument();
                if (document == null) document = new Document(this._jinx._jinx_properties);
                FileOutputStream fos = new FileOutputStream(this._jinx._jinx_properties_file);
                outputter.output(document, fos);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
