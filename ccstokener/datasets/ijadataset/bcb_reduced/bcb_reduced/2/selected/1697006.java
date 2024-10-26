package com.rapid_i.deployment.update.client;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import com.rapid_i.Launcher;
import com.rapidminer.RapidMiner;
import com.rapidminer.RapidMiner.ExecutionMode;
import com.rapidminer.deployment.client.wsimport.AccountService;
import com.rapidminer.deployment.client.wsimport.AccountServiceService;
import com.rapidminer.deployment.client.wsimport.PackageDescriptor;
import com.rapidminer.deployment.client.wsimport.UpdateService;
import com.rapidminer.deployment.client.wsimport.UpdateServiceException_Exception;
import com.rapidminer.deployment.client.wsimport.UpdateServiceService;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;

/**
 * This class manages the updates of the core and installation and updates of extensions.
 * 
 * @author Simon Fischer
 */
public class UpdateManager {

    public static final String PARAMETER_UPDATE_INCREMENTALLY = "rapidminer.update.incremental";

    public static final String PARAMETER_UPDATE_URL = "rapidminer.update.url";

    public static final String PARAMETER_INSTALL_TO_HOME = "rapidminer.update.to_home";

    public static final String UPDATESERVICE_URL = "http://rapidupdate.de:80/UpdateServer";

    private final UpdateService service;

    static final String COMMERCIAL_LICENSE_NAME = "RIC";

    public UpdateManager(UpdateService service) {
        super();
        this.service = service;
    }

    public void performUpdates(List<PackageDescriptor> downloadList, ProgressListener progressListener) throws IOException, UpdateServiceException_Exception {
        int i = 0;
        try {
            for (PackageDescriptor desc : downloadList) {
                String urlString = service.getDownloadURL(desc.getPackageId(), desc.getVersion(), desc.getPlatformName());
                int minProgress = 20 + 80 * i / downloadList.size();
                int maxProgress = 20 + 80 * (i + 1) / downloadList.size();
                boolean incremental = UpdateManager.isIncrementalUpdate();
                if (desc.getPackageTypeName().equals("RAPIDMINER_PLUGIN")) {
                    ManagedExtension extension = ManagedExtension.getOrCreate(desc.getPackageId(), desc.getName(), desc.getLicenseName());
                    String baseVersion = extension.getLatestInstalledVersionBefore(desc.getVersion());
                    incremental &= baseVersion != null;
                    URL url = UpdateManager.getUpdateServerURI(urlString + (incremental ? "?baseVersion=" + URLEncoder.encode(baseVersion, "UTF-8") : "")).toURL();
                    if (incremental) {
                        LogService.getRoot().info("Updating " + desc.getPackageId() + " incrementally.");
                        try {
                            updatePluginIncrementally(extension, openStream(url, progressListener, minProgress, maxProgress), baseVersion, desc.getVersion());
                        } catch (IOException e) {
                            LogService.getRoot().warning("Incremental Update failed. Trying to fall back on non incremental Update...");
                            incremental = false;
                        }
                    }
                    if (!incremental) {
                        LogService.getRoot().info("Updating " + desc.getPackageId() + ".");
                        updatePlugin(extension, openStream(url, progressListener, minProgress, maxProgress), desc.getVersion());
                    }
                    extension.addAndSelectVersion(desc.getVersion());
                } else {
                    URL url = UpdateManager.getUpdateServerURI(urlString + (incremental ? "?baseVersion=" + URLEncoder.encode(RapidMiner.getLongVersion(), "UTF-8") : "")).toURL();
                    LogService.getRoot().info("Updating RapidMiner core.");
                    updateRapidMiner(openStream(url, progressListener, minProgress, maxProgress), desc.getVersion());
                }
                i++;
                progressListener.setCompleted(20 + 80 * i / downloadList.size());
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        } finally {
            progressListener.complete();
        }
    }

    /**
     * @throws IOException  */
    private InputStream openStream(URL url, ProgressListener listener, int minProgress, int maxProgress) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(false);
        String lengthStr = con.getHeaderField("Content-Length");
        InputStream urlIn;
        try {
            urlIn = con.getInputStream();
        } catch (IOException e) {
            throw new IOException(con.getResponseCode() + ": " + con.getResponseMessage(), e);
        }
        if (lengthStr == null || lengthStr.isEmpty()) {
            LogService.getRoot().warning("Server did not send content length.");
            return urlIn;
        } else {
            try {
                long length = Long.parseLong(lengthStr);
                return new ProgressReportingInputStream(urlIn, listener, minProgress, maxProgress, length);
            } catch (NumberFormatException e) {
                LogService.getRoot().log(Level.WARNING, "Server sent illegal content length: " + lengthStr, e);
                return urlIn;
            }
        }
    }

    private void updatePlugin(ManagedExtension extension, InputStream updateIn, String newVersion) throws IOException {
        File outFile = extension.getDestinationFile(newVersion);
        OutputStream out = new FileOutputStream(outFile);
        try {
            Tools.copyStreamSynchronously(updateIn, out, true);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
            }
        }
    }

    private void updateRapidMiner(InputStream openStream, String version) throws IOException {
        File updateDir = new File(FileSystemService.getRapidMinerHome(), "update");
        if (!updateDir.exists()) {
            if (!updateDir.mkdir()) {
                throw new IOException("Cannot create update directory. Please ensure you have administrator permissions.");
            }
        }
        if (!updateDir.canWrite()) {
            throw new IOException("Cannot write to update directory. Please ensure you have administrator permissions.");
        }
        File updateFile = new File(updateDir, "rmupdate-" + version + ".jar");
        Tools.copyStreamSynchronously(openStream, new FileOutputStream(updateFile), true);
        File ruInstall = new File(FileSystemService.getRapidMinerHome(), "RUinstall");
        ZipFile zip = new ZipFile(updateFile);
        Enumeration<? extends ZipEntry> en = zip.entries();
        while (en.hasMoreElements()) {
            ZipEntry entry = en.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            String name = entry.getName();
            if ("META-INF/UPDATE".equals(name)) {
                Tools.copyStreamSynchronously(zip.getInputStream(entry), new FileOutputStream(new File(updateDir, "UPDATE")), true);
                continue;
            }
            if (name.startsWith("rapidminer/")) {
                name = name.substring("rapidminer/".length());
            }
            File dest = new File(ruInstall, name);
            File parent = dest.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            Tools.copyStreamSynchronously(zip.getInputStream(entry), new FileOutputStream(dest), true);
        }
        zip.close();
        updateFile.delete();
        LogService.getRoot().info("Prepared RapidMiner for update. Restart required.");
    }

    /** This method takes the entries contained in the plugin archive and in the
     *  jar read from the given input stream and merges the entries.
     *  The new jar is scanned for a file META-INF/UPDATE that contains
     *  instructions about files to delete. Files found in this list
     *  are removed from the destination jar. */
    private void updatePluginIncrementally(ManagedExtension extension, InputStream diffJarIn, String fromVersion, String newVersion) throws IOException {
        ByteArrayOutputStream diffJarBuffer = new ByteArrayOutputStream();
        Tools.copyStreamSynchronously(diffJarIn, diffJarBuffer, true);
        LogService.getRoot().fine("Downloaded incremental zip.");
        InMemoryZipFile diffJar = new InMemoryZipFile(diffJarBuffer.toByteArray());
        Set<String> toDelete = new HashSet<String>();
        byte[] updateEntry = diffJar.getContents("META-INF/UPDATE");
        if (updateEntry == null) {
            throw new IOException("META-INFO/UPDATE entry missing");
        }
        BufferedReader updateReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(updateEntry), "UTF-8"));
        String line;
        while ((line = updateReader.readLine()) != null) {
            String[] split = line.split(" ", 2);
            if (split.length != 2) {
                throw new IOException("Illegal entry in update script: " + line);
            }
            if ("DELETE".equals(split[0])) {
                toDelete.add(split[1].trim());
            } else {
                throw new IOException("Illegal entry in update script: " + line);
            }
        }
        LogService.getRoot().fine("Extracted update script, " + toDelete.size() + " items to delete.");
        Set<String> allNames = new HashSet<String>();
        allNames.addAll(diffJar.entryNames());
        JarFile fromJar = extension.findArchive(fromVersion);
        Enumeration<? extends ZipEntry> e = fromJar.entries();
        while (e.hasMoreElements()) {
            ZipEntry entry = e.nextElement();
            allNames.add(entry.getName());
        }
        LogService.getRoot().info("Extracted entry names, " + allNames.size() + " entries in total.");
        File newFile = extension.getDestinationFile(newVersion);
        ZipOutputStream newJar = new ZipOutputStream(new FileOutputStream(newFile));
        ZipFile oldArchive = extension.findArchive();
        for (String name : allNames) {
            if (toDelete.contains(name)) {
                LogService.getRoot().finest("DELETE " + name);
                continue;
            }
            newJar.putNextEntry(new ZipEntry(name));
            if (diffJar.containsEntry(name)) {
                newJar.write(diffJar.getContents(name));
                LogService.getRoot().finest("UPDATE " + name);
            } else {
                ZipEntry oldEntry = oldArchive.getEntry(name);
                Tools.copyStreamSynchronously(oldArchive.getInputStream(oldEntry), newJar, false);
                LogService.getRoot().finest("STORE " + name);
            }
            newJar.closeEntry();
        }
        newJar.finish();
        newJar.close();
    }

    public static String getBaseUrl() {
        String property = ParameterService.getParameterValue(PARAMETER_UPDATE_URL);
        if (property == null) {
            return UPDATESERVICE_URL;
        } else {
            return property;
        }
    }

    public static URI getUpdateServerURI(String suffix) throws URISyntaxException {
        String property = ParameterService.getParameterValue(PARAMETER_UPDATE_URL);
        if (property == null) {
            return new URI(UPDATESERVICE_URL + suffix);
        } else {
            return new URI(property + suffix);
        }
    }

    public static boolean isIncrementalUpdate() {
        return !"false".equals(ParameterService.getParameterValue(PARAMETER_UPDATE_INCREMENTALLY));
    }

    private static UpdateService theService = null;

    private static URI lastUsedUri = null;

    public static synchronized UpdateService getService() throws MalformedURLException, URISyntaxException {
        URI uri = getUpdateServerURI("/UpdateServiceService?wsdl");
        if (theService == null || lastUsedUri != null && !lastUsedUri.equals(uri)) {
            UpdateServiceService uss = new UpdateServiceService(uri.toURL(), new QName("http://ws.update.deployment.rapid_i.com/", "UpdateServiceService"));
            theService = uss.getUpdateServicePort();
        }
        lastUsedUri = uri;
        return theService;
    }

    public static synchronized AccountService getAccountService() throws MalformedURLException, URISyntaxException {
        URI uri = getUpdateServerURI("/AccountService?wsdl");
        AccountServiceService ass = new AccountServiceService(uri.toURL(), new QName("http://ws.update.deployment.rapid_i.com/", "AccountServiceService"));
        return ass.getAccountServicePort();
    }

    public static void saveLastUpdateCheckDate() {
        File file = FileSystemService.getUserConfigFile("updatecheck.date");
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(file));
            out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private static Date loadLastUpdateCheckDate() {
        File file = FileSystemService.getUserConfigFile("updatecheck.date");
        if (!file.exists()) return null;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));
            String date = in.readLine();
            if (date == null) {
                return null;
            } else {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date);
            }
        } catch (Exception e) {
            LogService.getRoot().log(Level.WARNING, "Cannot read last date of update check.", e);
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /** Checks whether the last update is at least 7 days ago, then checks whether there
     *  are any updates, and opens a dialog if desired by the user. */
    public static void checkForUpdates() {
        String updateProperty = ParameterService.getParameterValue(RapidMinerGUI.PROPERTY_RAPIDMINER_GUI_UPDATE_CHECK);
        if (Tools.booleanValue(updateProperty, true)) {
            if (Launcher.isDevelopmentBuild()) {
                LogService.getRoot().config("This is a development build. Ignoring update check.");
                return;
            }
            if (RapidMiner.getExecutionMode() == ExecutionMode.WEBSTART) {
                LogService.getRoot().config("Ignoring update check in Webstart mode.");
                return;
            }
            boolean check = true;
            final Date lastCheckDate = loadLastUpdateCheckDate();
            if (lastCheckDate != null) {
                Calendar lastCheck = Calendar.getInstance();
                lastCheck.setTime(lastCheckDate);
                Calendar currentDate = Calendar.getInstance();
                currentDate.add(Calendar.DAY_OF_YEAR, -2);
                if (!lastCheck.before(currentDate)) {
                    check = false;
                    LogService.getRoot().config("Ignoring update check. Last update check was on " + lastCheckDate);
                }
            }
            if (check) {
                new ProgressThread("check_for_updates") {

                    @Override
                    public void run() {
                        LogService.getRoot().info("Checking for updates.");
                        XMLGregorianCalendar xmlGregorianCalendar;
                        if (lastCheckDate != null) {
                            try {
                                xmlGregorianCalendar = XMLTools.getXMLGregorianCalendar(lastCheckDate);
                            } catch (Exception e) {
                                LogService.getRoot().log(Level.WARNING, "Error checking for updates: " + e, e);
                                return;
                            }
                        } else {
                            xmlGregorianCalendar = null;
                        }
                        boolean updatesExist;
                        try {
                            updatesExist = getService().anyUpdatesSince(xmlGregorianCalendar);
                        } catch (Exception e) {
                            LogService.getRoot().log(Level.WARNING, "Error checking for updates: " + e, e);
                            return;
                        }
                        if (updatesExist) {
                            if (SwingTools.showConfirmDialog("updates_exist", ConfirmDialog.YES_NO_OPTION) == ConfirmDialog.YES_OPTION) {
                                UpdateDialog.showUpdateDialog();
                            } else {
                                saveLastUpdateCheckDate();
                            }
                        } else {
                            LogService.getRoot().info("No updates since " + lastCheckDate + ".");
                            saveLastUpdateCheckDate();
                        }
                    }
                }.start();
            }
        }
    }
}
