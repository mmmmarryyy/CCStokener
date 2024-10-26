package org.lnicholls.galleon.apps.hdphotos;

import com.tivo.hme.interfaces.IApplication;
import com.tivo.hme.interfaces.IArgumentList;
import com.tivo.hme.interfaces.IContext;
import com.tivo.hme.interfaces.IFactory;
import com.tivo.hme.sdk.IHmeProtocol;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.util.NameValue;

public class HDPhotos {

    private static final Log log = LogFactory.getLog(HDPhotos.class);

    private static final File installDir;

    static {
        String dir = "../apps/hdphotos";
        if (System.getProperty("apps") != null) {
            dir = System.getProperty("apps") + "/hdphotos";
        }
        installDir = new File(dir);
    }

    public static IFactory getAppFactory(String appClassName, ClassLoader loader, IArgumentList args) {
        IFactory factory = new HDPhotosFactory();
        factory.initFactory(appClassName, loader, args);
        return factory;
    }

    public static void installApp() {
        if (isInstalled()) {
            return;
        }
        log.info("Installing hdphotos application...");
        try {
            if (!installDir.exists()) {
                log.info("Creating directory: " + installDir.getAbsolutePath());
                installDir.mkdirs();
            }
            File file = new File(installDir, "hdphotos.jar.orig");
            URL url = new URL("http://cds.tivo.com.edgesuite.net/TiVoDesktop/hdphotos/hdphotos.jar");
            downloadFile(url, file);
            unsignJar(file, new File(installDir, "hdphotos.jar"));
            file = new File(installDir, "hme-host-sample.jar");
            url = new URL("http://cds.tivo.com.edgesuite.net/TiVoDesktop/hdphotos/hme-host-sample.jar");
            downloadFile(url, file);
            log.info("hdphotos application is now installed!");
        } catch (MalformedURLException e) {
            log.fatal("Cannot install application.", e);
        } catch (IOException e) {
            log.error("Cannot install application.", e);
        }
    }

    private static void unsignJar(File origFile, File destFile) throws ZipException, IOException {
        log.info("removing signing info from " + origFile);
        ZipFile zip = new ZipFile(origFile);
        byte[] buff = new byte[1024];
        File tmpFile = new File(destFile.getPath() + ".tmp");
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tmpFile));
        InputStream in = null;
        try {
            Enumeration e = zip.entries();
            while (e.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                String name = entry.getName().toLowerCase();
                if (name.endsWith(".sf") || name.endsWith(".rsa")) {
                    continue;
                }
                out.putNextEntry(entry);
                in = zip.getInputStream(entry);
                try {
                    for (int len = in.read(buff); len != -1; len = in.read(buff)) {
                        out.write(buff, 0, len);
                    }
                } finally {
                    if (in != null) {
                        in.close();
                        in = null;
                    }
                    out.flush();
                }
            }
        } finally {
            zip.close();
            out.flush();
            out.close();
        }
        tmpFile.renameTo(destFile);
        if (!origFile.delete()) {
            log.warn("Cannot delete original file: " + origFile);
        }
        log.info("files saved as " + destFile);
    }

    private static void downloadFile(URL url, File destFile) throws IOException {
        log.info("downloading file: " + url);
        OutputStream out = null;
        InputStream in = null;
        try {
            in = url.openStream();
            out = new FileOutputStream(destFile);
            byte[] buff = new byte[1024];
            for (int len = in.read(buff); len != -1; len = in.read(buff)) {
                out.write(buff, 0, len);
            }
            log.info("saved file: " + destFile);
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    public static boolean isInstalled() {
        if (!new File(installDir, "hdphotos.jar").isFile()) {
            return false;
        }
        if (!new File(installDir, "hme-host-sample.jar").isFile()) {
            return false;
        }
        return true;
    }

    public static class HDPhotosFactory extends AppFactory {

        private static final Log log = LogFactory.getLog(HDPhotosFactory.class);

        private ProcessReader err;

        private ProcessReader in;

        private Process process;

        private CalypsoServer calypso;

        @SuppressWarnings("unchecked")
        @Override
        public void initFactory(String appClassName, ClassLoader loader, IArgumentList args) {
            installApp();
            this.init(args);
        }

        @Override
        public void updateAppContext(AppContext appContext) {
            super.updateAppContext(appContext);
            if (isActive()) {
                log.info("Restarting hdphotos application...");
                setActive(false);
                setActive(true);
            }
        }

        @Override
        public IApplication createApplication(IContext context) throws IOException {
            throw new IOException("Not allowed to create instance of HDPhotos application.");
        }

        @Override
        public String getAppName() {
            return null;
        }

        public void setActive(boolean active) {
            this.isActive = active;
            if (active) {
                if (process == null) {
                    if (!HDPhotos.isInstalled()) {
                        log.fatal("hdphotos external app is not installed to run hdphotos.");
                        return;
                    }
                    HDPhotosConfiguration config = (HDPhotosConfiguration) getAppContext().getConfiguration();
                    StringBuilder paths = new StringBuilder();
                    for (NameValue path : config.getPaths()) {
                        if (paths.length() != 0) {
                            paths.append(';');
                        }
                        if (path.getName() != null) {
                            paths.append(path.getName()).append('|');
                        }
                        paths.append(path.getValue());
                    }
                    int calypsoPort = -1;
                    try {
                        calypso = new CalypsoServer();
                        calypso.start();
                        calypsoPort = calypso.getLocalPort();
                    } catch (IOException e) {
                        log.error("Cannot startup calypso server", e);
                        return;
                    }
                    String os = System.getProperty("os.name").toLowerCase();
                    String sep = os.indexOf("windows") != -1 ? ";" : ":";
                    String port = "7111";
                    List<String> cmd = new ArrayList<String>();
                    cmd.add("java");
                    cmd.add("-Xmx128M");
                    cmd.add("-classpath");
                    cmd.add("hdphotos-ext.jar" + sep + "hdphotos.jar" + sep + "hme-host-sample.jar");
                    cmd.add("-Dhdphotos.path=" + paths.toString());
                    cmd.add("-Dcom.tivo.calypso.host=localhost:" + calypsoPort);
                    if (config.getName() != null) {
                        cmd.add("-Dcom.tivo.hdphotos.title=" + config.getName());
                    }
                    if (config.getFlickrUsername() != null) {
                        cmd.add("-Dcom.tivo.hdphotos.flickrusername=" + config.getFlickrUsername());
                    }
                    if (config.getFlickrFavoriteUsers() != null) {
                        cmd.add("-Dcom.tivo.hdphotos.flickrusers=" + config.getFlickrFavoriteUsers().replace(',', ':'));
                    }
                    cmd.add("com.tivo.hme.host.sample.Main");
                    cmd.add("--exitwithcalypso");
                    cmd.add("--port");
                    cmd.add(port);
                    cmd.add("--class");
                    cmd.add("com.tivo.hme.hdphotos.HDPhotos");
                    if (log.isInfoEnabled()) {
                        StringBuilder sb = new StringBuilder();
                        for (String arg : cmd) {
                            sb.append(' ');
                            boolean quote = arg.indexOf(' ') != -1;
                            if (quote) {
                                sb.append('"');
                            }
                            sb.append(arg);
                            if (quote) {
                                sb.append('"');
                            }
                        }
                        log.info("Starting hdphotos process:" + sb);
                    }
                    try {
                        process = Runtime.getRuntime().exec(cmd.toArray(new String[cmd.size()]), null, installDir);
                        in = new ProcessReader("IN: ", process.getInputStream());
                        err = new ProcessReader("ERR: ", process.getErrorStream());
                        in.start();
                        err.start();
                        if (log.isInfoEnabled()) {
                            log.info("HDPhotos process started on port " + port);
                        }
                    } catch (IOException e) {
                        log.error("Cannot startup hdphotos process", e);
                    }
                }
            } else {
                if (calypso != null) {
                    synchronized (calypso) {
                        calypso.close();
                    }
                    calypso = null;
                }
                if (process != null) {
                    try {
                        in.close();
                        in = null;
                        err.close();
                        err = null;
                    } catch (IOException e) {
                        log.warn("Could not close process reader streams.", e);
                    }
                    process.destroy();
                    process = null;
                }
            }
        }
    }
}
