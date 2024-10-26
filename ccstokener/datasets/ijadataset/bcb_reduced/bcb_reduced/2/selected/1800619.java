package hu.scytha.update;

import hu.scytha.common.*;
import hu.scytha.main.Scytha;
import hu.scytha.main.Settings;
import hu.scytha.preference.PreferenceManager;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;

/**
 * Scytha updater class. This class creates connection to update site, downloads and installs updates.
 * 
 * @author Bertalan Lacza
 */
public abstract class ScythaUpdater {

    private static HashMap fMirrorMap;

    private static String fSwtLocation;

    private static String fCLibLocation;

    private static String fUpdaterLocation;

    public static String checkUpdates(String connection) {
        AvailableUpdatesRunnable availableUpdates = new AvailableUpdatesRunnable(connection);
        ProgressMonitorDialog progressDlg = new ProgressMonitorDialog(Scytha.getWindow().getShell());
        try {
            progressDlg.run(true, true, availableUpdates);
            if (availableUpdates.getUpdates() != null) {
                List<IUpdateable> updates = availableUpdates.getUpdates();
                if (updates.get(0).getType() == IUpdateable.SCYTHA) {
                    return updates.get(0).getVersion();
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static boolean handleUpdateProcess(String connection) {
        return true;
    }

    /**
    * Checks permissions.
    * @return TRUE: ok
    */
    private static boolean existsPermissionsToUpdate() {
        final LocalFile installDir = new LocalFile(Settings.getScythaHomeDirectory());
        final LocalFile modules = new LocalFile(Settings.getScythaHomeDirectory() + File.separator + "modules");
        final LocalFile swt = new LocalFile(Settings.getScythaHomeDirectory() + File.separator + "swt");
        final LocalFile help = new LocalFile(Settings.getScythaHomeDirectory() + File.separator + "help");
        if (!installDir.canWrite() || !modules.canWrite() || !swt.canWrite() || !help.canWrite()) {
            return false;
        }
        return true;
    }

    /**
    * 
    * @param pMmirrors mirror sites
    * @param pUpdates available updates
    * @param pMirrorMap mapping of mirror sites (name=url)
    * @return
    */
    public static void handleUpdateProcessAtStart(final String[] pMmirrors, final List pUpdates, final HashMap pMirrorMap) {
        Display.getDefault().asyncExec(new Runnable() {

            public void run() {
            }
        });
    }

    /**
    * Removes old versions.
    *
    */
    private static void deleteVersions(List modules) {
        for (Iterator iter = modules.iterator(); iter.hasNext(); ) {
            String jar = (String) iter.next();
            new File(Settings.getScythaHomeDirectory() + File.separator + jar).delete();
        }
    }

    /**
    * 
    * @author yetirc
    *
    */
    private static class PrepareSwtRunnable implements IRunnableWithProgress {

        private String fVersion;

        PrepareSwtRunnable(String version) {
            fVersion = version;
        }

        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            try {
                monitor.beginTask(Messages.getString("ScythaUpdater.unzip.swt.files"), IProgressMonitor.UNKNOWN);
                ZipFile swtZip = new ZipFile(fSwtLocation);
                Enumeration enumeration = swtZip.entries();
                String scythaSwtDir = Settings.getUpdatesTempDirectory() + File.separator + "swt";
                while (enumeration.hasMoreElements()) {
                    unZipFile(scythaSwtDir, (ZipEntry) enumeration.nextElement(), swtZip);
                }
                if (!new File(fSwtLocation).delete()) {
                    if (Settings.debugOn) {
                        MessageSystem.log("Can't delete: " + fSwtLocation, getClass().getName(), "run", null);
                    }
                }
            } catch (IOException e) {
                throw new InvocationTargetException(e, "Can't unzip swt.");
            } finally {
                PreferenceManager.getDefaultPreferenceStore().setValue(PreferenceManager.SWT_VERSION, fVersion);
                monitor.done();
            }
        }
    }

    /**
    * Process one file from the zip, given its name. Either print the name, or
    * create the file on disk.
    */
    private static void unZipFile(String destinationDir, ZipEntry zipEntry, ZipFile myZippy) throws IOException {
        if (Settings.traceOn) {
            MessageSystem.trace(ScythaUpdater.class.getName(), "unZipFile(destinationDir, zipEntry, zipFileName");
        }
        String zipName = zipEntry.getName();
        HashSet<String> dirsMade = new HashSet<String>();
        byte[] bytes = new byte[10240];
        if (zipName.startsWith("/")) {
            zipName = zipName.substring(1);
        }
        if (zipName.endsWith("/")) {
            return;
        }
        zipName = destinationDir + File.separator + zipName;
        int ix = zipName.lastIndexOf('/');
        if (ix > 0) {
            String dirName = zipName.substring(0, ix);
            if (!dirsMade.contains(dirName)) {
                File d = new File(dirName);
                if (!(d.exists() && d.isDirectory())) {
                    if (!d.mkdirs()) {
                        MessageSystem.logException("Can't make dir.", "hu.scytha.frw.fs.zip.Zipper", "unZipFile", null, null);
                    }
                    dirsMade.add(dirName);
                }
            }
        }
        FileOutputStream os = new FileOutputStream(zipName);
        InputStream is = myZippy.getInputStream(zipEntry);
        int n = 0;
        while ((n = is.read(bytes)) > 0) {
            os.write(bytes, 0, n);
        }
        is.close();
        os.close();
    }

    /**
    * Downloads all available updates.
    * 
    */
    private static class DownloadUpdatesRunnable implements IRunnableWithProgress {

        private String fMirror;

        private List fList;

        private boolean downloadWasOk = true;

        private ArrayList<String> downloadedFiles = new ArrayList<String>();

        private static final String HELP_DIR = "help";

        private static final String MODULES_DIR = "modules";

        private static final String SWT_DIR = "swt";

        private static final String UPDATER_DIR = "updater";

        private boolean isRestartRequired = false;

        /**
       * Constructor.
       * @param installDir
       * @param mirror
       * @param list
       */
        public DownloadUpdatesRunnable(String mirror, List list) {
            this.fMirror = mirror;
            this.fList = list;
        }

        boolean downloadWasOk() {
            return downloadWasOk;
        }

        /**
       * Downloads updates.
       */
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            try {
                int bytesToDownload = 0;
                for (Iterator iter = fList.iterator(); iter.hasNext(); ) {
                    Updateable updateableModule = (Updateable) iter.next();
                    try {
                        bytesToDownload += Integer.valueOf(updateableModule.getSize()).intValue();
                    } catch (NumberFormatException e) {
                        MessageSystem.logException("", getClass().getName(), "run", null, e);
                        bytesToDownload = IProgressMonitor.UNKNOWN;
                        break;
                    }
                }
                monitor.beginTask(Messages.getString("ScythaUpdater.download.updates"), bytesToDownload);
                String mirrorSite = fMirror;
                for (int i = 0; i < fList.size(); i++) {
                    Updateable updateableModule = (Updateable) fList.get(i);
                    String destinationSubDir = "";
                    switch(updateableModule.getType()) {
                        case IUpdateable.HELP:
                            destinationSubDir = HELP_DIR;
                            break;
                        case IUpdateable.MODULE:
                            destinationSubDir = MODULES_DIR;
                            break;
                        case IUpdateable.SWT:
                            destinationSubDir = SWT_DIR;
                            break;
                        case IUpdateable.SCYTHA:
                            break;
                        case IUpdateable.UPDATER:
                            destinationSubDir = UPDATER_DIR;
                            break;
                        case IUpdateable.C_LIBRARY:
                            destinationSubDir = MODULES_DIR;
                            break;
                    }
                    try {
                        downloadWasOk = downloadFile(mirrorSite, updateableModule, destinationSubDir, monitor);
                        if (!isRestartRequired) {
                            isRestartRequired = updateableModule.isRestartRequired();
                        }
                    } catch (MalformedURLException e) {
                        MessageSystem.logException("Bad URL.", getClass().getName(), "handleUpdateProcess", null, e);
                        MessageSystem.showErrorMessage(e.getLocalizedMessage());
                        downloadWasOk = false;
                        return;
                    } catch (FileNotFoundException e) {
                        MessageSystem.logException("File was not found on the server.", getClass().getName(), "run", null, e);
                        MessageSystem.showErrorMessage(Messages.getString("ScythaUpdater.no.file.was.found"));
                        downloadWasOk = false;
                        return;
                    } catch (IOException e) {
                        MessageSystem.logException("", getClass().getName(), "handleUpdateProcess", null, e);
                        MessageSystem.showErrorMessage(e.getLocalizedMessage());
                        downloadWasOk = false;
                        return;
                    }
                }
            } finally {
                monitor.done();
            }
        }

        /**
       * Retrives the list of downloaded files.
       * @return list of downloaded files
       */
        List getDownLoadedFiles() {
            return this.downloadedFiles;
        }

        /**
       * Downloads files.
       * @param pMirrorSite mirror site url
       * @param pFileName file name to download
       * @param pDestinationDir destination directory
       * @param pInstallName the new name of the downloaded file
       * @param pUpdateableType type of the file
       * @param monitor progress monitor
       * @return TRUE: update was ok, FALSE: otherwise
       * @throws InterruptedException
       * @throws MalformedURLException
       * @throws FileNotFoundException
       * @throws IOException
       */
        private boolean downloadFile(String pMirrorSite, Updateable pUpdateable, String pDestinationDir, IProgressMonitor monitor) throws InterruptedException, MalformedURLException, FileNotFoundException, IOException {
            pMirrorSite = pMirrorSite.endsWith("/") ? pMirrorSite : pMirrorSite + "/";
            URL url = new URL(pMirrorSite + pUpdateable.getJarLocation());
            URLConnection urlConnection = url.openConnection();
            InputStream istream = urlConnection.getInputStream();
            String destinationFile = Settings.getUpdatesTempDirectory() + File.separator + pDestinationDir + File.separator + pUpdateable.getInstallName();
            BufferedOutputStream fos;
            try {
                fos = new BufferedOutputStream(new FileOutputStream(destinationFile));
            } catch (FileNotFoundException e) {
                MessageSystem.logException("", getClass().getName(), "run", null, e);
                MessageSystem.showErrorMessage(Messages.getString("CopyAction.file.is.not.found") + "\n" + e.getMessage());
                return false;
            }
            monitor.subTask(Messages.getString("ScythaUpdater.downloading", new Object[] { pUpdateable.getJarLocation(), "" + (Integer.valueOf(pUpdateable.getSize()).intValue() / 1024) }));
            byte[] buf = new byte[1024];
            int j = 0;
            while ((j = istream.read(buf)) != -1) {
                fos.write(buf, 0, j);
                if (monitor.isCanceled()) {
                    fos.flush();
                    fos.close();
                    istream.close();
                    downloadWasOk = false;
                    throw new InterruptedException();
                }
                monitor.worked(j);
            }
            fos.flush();
            fos.close();
            istream.close();
            switch(pUpdateable.getType()) {
                case IUpdateable.HELP:
                    break;
                case IUpdateable.SWT:
                    fSwtLocation = destinationFile;
                    break;
                case IUpdateable.UPDATER:
                    fUpdaterLocation = destinationFile;
                    break;
                case IUpdateable.C_LIBRARY:
                    fUpdaterLocation = destinationFile;
                    break;
            }
            downloadedFiles.add(destinationFile);
            return true;
        }
    }

    /**
    * Checking MD5 sum.
    * @author Bertalan Lacza
    *
    */
    private static class MD5CheckSum implements IRunnableWithProgress {

        private List fDownloadedFiles;

        private List fUpdateModules;

        private boolean checkOk = false;

        /**
       * Constructor.
       * @param downloadedFiles
       * @param updateModules
       */
        MD5CheckSum(List downloadedFiles, List updateModules) {
            this.fDownloadedFiles = downloadedFiles;
            this.fUpdateModules = updateModules;
        }

        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            monitor.beginTask(Messages.getString("ScythaUpdater.check.md5.sum"), this.fDownloadedFiles.size());
            try {
                Iterator updModules = this.fUpdateModules.iterator();
                for (Iterator iter = this.fDownloadedFiles.iterator(); iter.hasNext(); ) {
                    String destinationFile = (String) iter.next();
                    Updateable updateableModule = (Updateable) updModules.next();
                    if (monitor.isCanceled() || !Util.generateMD5Sum(destinationFile).equals(updateableModule.getMD5Sum())) {
                        checkOk = false;
                        monitor.done();
                        break;
                    }
                    monitor.worked(1);
                    checkOk = true;
                }
            } catch (FileNotFoundException e) {
                MessageSystem.logException("", getClass().getName(), "run", null, e);
                MessageSystem.showErrorMessage(Messages.getString("ScythaUpdater.md5.failed") + e.getLocalizedMessage());
            } catch (NoSuchAlgorithmException e) {
                MessageSystem.logException("", getClass().getName(), "run", null, e);
                MessageSystem.showErrorMessage(Messages.getString("ScythaUpdater.md5.failed") + e.getLocalizedMessage());
            } catch (IOException e) {
                MessageSystem.logException("", getClass().getName(), "run", null, e);
                MessageSystem.showErrorMessage(Messages.getString("ScythaUpdater.md5.failed") + e.getLocalizedMessage());
            }
        }

        boolean isCheckOk() {
            return checkOk;
        }
    }

    /**
    * Check for permissions to update scytha.
    * @return
    */
    public static boolean checkPreRequisitesForUpdate() {
        final LocalFile installDir = new LocalFile(Settings.getScythaHomeDirectory());
        final LocalFile scythaJar = new LocalFile(Settings.getScythaHomeDirectory() + "/" + "scytha.jar");
        if (!installDir.canWrite() || !scythaJar.canWrite()) {
            return false;
        }
        return true;
    }
}
