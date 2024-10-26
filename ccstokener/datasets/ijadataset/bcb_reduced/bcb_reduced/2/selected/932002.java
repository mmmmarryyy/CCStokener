package net.sf.cclearly.updater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Scanner;
import java.util.StringTokenizer;
import net.sf.cclearly.logic.UserDAO;
import net.sf.cclearly.md5.MD5;
import net.sf.cclearly.prefs.Prefs;
import za.dats.util.events.EventManager;
import za.dats.util.injection.Dependant;
import za.dats.util.injection.Inject;
import za.dats.util.injection.Injector;

@Dependant
public class UpdateSession {

    private File downloadFolder;

    private File applicationFolder;

    private String updateServer;

    private long totalSize;

    private long downloadCount;

    private long downloadSize;

    private boolean hasUpdate;

    private boolean isDownloading = false;

    @Inject
    Prefs prefs;

    @Inject
    UserDAO userDao;

    private LinkedList<FileDescriptor> descriptors = new LinkedList<FileDescriptor>();

    private LinkedList<FileDescriptor> newFiles = new LinkedList<FileDescriptor>();

    private LinkedList<FileDescriptor> updatedFiles = new LinkedList<FileDescriptor>();

    DownloadProgressEvent progressEvent = EventManager.newEvent(DownloadProgressEvent.class);

    private int downloadCompleted;

    private boolean cancelDownload = false;

    public void addDownloadProgressListener(DownloadProgressEvent listener) {
        EventManager.addListener(progressEvent, listener);
    }

    public void removeDownloadProgressListener(DownloadProgressEvent listener) {
        EventManager.removeListener(progressEvent, listener);
    }

    public UpdateSession() {
        Injector.inject(this);
    }

    /**
     * This opens the update session
     * 
     * @return true if everything is ok, false on error.
     */
    public boolean open() {
        downloadFolder = prefs.getUpdatePrefs().getDownloadFolder();
        applicationFolder = new File(".");
        updateServer = prefs.getUpdatePrefs().getUpdaterURL();
        if ((updateServer == null) || updateServer.equals("")) {
            return false;
        }
        if (!downloadFolder.exists()) {
            downloadFolder.mkdir();
        }
        return true;
    }

    public synchronized void startDownload() {
        cancelDownload = false;
        downloadCompleted = 0;
        try {
            isDownloading = true;
            if (cancelDownload) {
                progressEvent.cancelled();
                return;
            }
            int fileCount = 1;
            if (newFiles.size() > 0) {
                progressEvent.startNewFiles();
                for (FileDescriptor file : newFiles) {
                    downloadFile(file.name, fileCount);
                    fileCount++;
                }
            }
            if (cancelDownload) {
                progressEvent.cancelled();
                return;
            }
            if (updatedFiles.size() > 0) {
                progressEvent.startUpdatedFiles();
                for (FileDescriptor file : updatedFiles) {
                    downloadFile(file.name, fileCount);
                    fileCount++;
                }
            }
            if (cancelDownload) {
                progressEvent.cancelled();
                return;
            }
            if (compare(true)) {
                progressEvent.invalidDownload();
            } else {
                progressEvent.downloadCompleted();
            }
        } finally {
            isDownloading = false;
        }
    }

    public boolean compare() {
        return compare(false);
    }

    /**
     * Runs a comparison between the update location and the application folder
     * 
     * @return true if there is a change, false if not
     */
    private synchronized boolean compare(boolean finalCheck) {
        if (!initializeHashList(finalCheck)) {
            return false;
        }
        boolean result = false;
        downloadSize = 0;
        downloadCount = 0;
        newFiles.clear();
        updatedFiles.clear();
        for (FileDescriptor file : descriptors) {
            File localFile = new File(applicationFolder, file.name);
            String localHash = getFileHash(localFile);
            if (file.hash.equals(localHash)) {
                continue;
            }
            String tempHash = getFileHash(new File(downloadFolder, file.name));
            if (file.hash.equals(tempHash)) {
                continue;
            }
            if (localFile.exists()) {
                updatedFiles.add(file);
            } else {
                newFiles.add(file);
            }
            downloadSize += file.size;
            downloadCount++;
            result = true;
        }
        hasUpdate = result;
        return result;
    }

    private boolean initializeHashList(boolean finalCheck) {
        File file = new File(downloadFolder, "files.md5");
        String url = "files.md5?id=" + userDao.getCurrentUser().getMachineID();
        if (finalCheck) {
            url += "&final=true";
        }
        if (downloadRemoteFile(file, url, 0) == 0) {
            return false;
        }
        try {
            descriptors.clear();
            totalSize = 0;
            Scanner scanner = new Scanner(new FileInputStream(file));
            scanner.useDelimiter(System.getProperty("line.separator"));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                StringTokenizer tokenizer = new StringTokenizer(line, ",");
                int elmCount = 0;
                FileDescriptor descriptor = new FileDescriptor();
                while (tokenizer.hasMoreTokens()) {
                    String element = tokenizer.nextToken();
                    switch(elmCount) {
                        case 0:
                            descriptor.name = element;
                            break;
                        case 1:
                            descriptor.hash = element;
                            break;
                        case 2:
                            descriptor.size = Long.valueOf(element);
                            break;
                    }
                    elmCount++;
                }
                if ((descriptor.name.length() > 0) && (descriptor.hash.length() > 0)) {
                    descriptors.add(descriptor);
                    totalSize += descriptor.size;
                }
            }
            scanner.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private int downloadRemoteFile(File destFile, String filePath, long fileCount) {
        int ret = 0;
        if (cancelDownload) {
            return ret;
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL(updateServer + "/" + filePath);
            setupProxy();
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("GET");
            HttpURLConnection.setFollowRedirects(true);
            connection.connect();
            int statusCode = connection.getResponseCode();
            if (statusCode != HttpURLConnection.HTTP_OK) {
                return 0;
            }
            InputStream in = connection.getInputStream();
            FileOutputStream out = new FileOutputStream(destFile);
            byte[] data = new byte[4096];
            int read = -1;
            while ((read = in.read(data, 0, 4096)) != -1) {
                out.write(data, 0, read);
                downloadCompleted += read;
                progressEvent.progress(fileCount, downloadCompleted, downloadCount, downloadSize);
                if (cancelDownload) {
                    break;
                }
            }
            out.flush();
            out.close();
            in.close();
        } catch (IOException e) {
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        ret = (int) destFile.length();
        return ret;
    }

    private void setupProxy() {
        if ((prefs.getProxyHost() != null) && (prefs.getProxyHost().length() > 0)) {
            Properties systemSettings = System.getProperties();
            systemSettings.put("http.proxyHost", prefs.getProxyHost());
            systemSettings.put("http.proxyPort", prefs.getProxyPort());
            System.setProperties(systemSettings);
        }
    }

    private String getFileHash(File file) {
        String hash = "";
        if (file.exists()) {
            try {
                hash = MD5.asHex(MD5.getHash(new File(file.getAbsolutePath())));
            } catch (IOException e) {
                System.out.println(e.toString());
            }
        }
        return hash;
    }

    private long downloadFile(String path, long fileCount) {
        String[] filePath = path.split("/");
        if (filePath.length == 0) {
            return 0;
        }
        for (int j = 0; j < filePath.length - 1; j++) {
            String subPath = "";
            for (int k = 0; k < j; k++) {
                subPath = filePath[k] + "/" + subPath;
            }
            File dir = new File(downloadFolder, subPath + filePath[j]);
            if (!dir.exists()) {
                dir.mkdir();
            }
        }
        String subPath = "";
        for (int j = 0; j < filePath.length - 1; j++) {
            subPath = filePath[j] + "/" + subPath;
        }
        File file = new File(downloadFolder, path);
        long length = downloadRemoteFile(file, "updates/" + path + "?id=" + userDao.getCurrentUser().getMachineID(), fileCount);
        return length;
    }

    private class FileDescriptor {

        String name = "";

        String hash = "";

        long size;
    }

    public File getApplicationFolder() {
        return applicationFolder;
    }

    void setApplicationFolder(File applicationFolder) {
        this.applicationFolder = applicationFolder;
    }

    public boolean hasUpdate() {
        return hasUpdate;
    }

    public int getNewFileCount() {
        return newFiles.size();
    }

    public int getUpdatedFileCount() {
        return updatedFiles.size();
    }

    public boolean isDownloading() {
        return isDownloading;
    }

    public String getFriendlyDownloadSize() {
        if (downloadSize > 1048576) {
            return "" + downloadSize / 1048576 + "MB";
        } else if (downloadSize > 1024) {
            return "" + downloadSize / 1024 + "KB";
        } else {
            return "" + downloadSize + " Bytes";
        }
    }

    public long getDownloadSize() {
        return downloadSize;
    }

    public void cancelDownload() {
        cancelDownload = true;
    }
}
