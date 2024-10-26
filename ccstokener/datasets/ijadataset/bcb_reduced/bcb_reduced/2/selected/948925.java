package de.shandschuh.jaolt.tools.download.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import de.shandschuh.jaolt.core.Language;
import de.shandschuh.jaolt.tools.log.Logger;

public class Download extends Thread {

    protected URL url;

    protected int size = -2;

    protected int fetchedSize;

    protected File file;

    protected Vector<DownloadListener> downloadListener;

    protected boolean failed;

    protected boolean finished;

    protected long date;

    protected boolean optional;

    protected boolean download;

    public Download(URL url, File file) {
        this.url = url;
        setFile(file);
        init();
    }

    private void init() {
        downloadListener = new Vector<DownloadListener>();
        size = -2;
        date = -2;
    }

    public void run() {
        try {
            download();
        } catch (Exception exception) {
            failed = true;
            for (int n = 0, i = downloadListener.size(); n < i; n++) {
                synchronized (downloadListener.get(n)) {
                    ((DownloadListener) downloadListener.get(n)).exceptionWasThrown(this, exception);
                }
            }
            Logger.log(exception);
        }
    }

    public void download() throws IOException {
        new File(file.getPath().substring(0, file.getPath().lastIndexOf(File.separator))).mkdirs();
        URLConnection urlConnection = url.openConnection();
        size = urlConnection.getContentLength();
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
        InputStream inputStream = urlConnection.getInputStream();
        byte[] buffer = new byte[1024];
        int numRead;
        fetchedSize = 0;
        date = urlConnection.getLastModified();
        while (!failed && (numRead = inputStream.read(buffer)) != -1) {
            if (failed) {
                throw new IOException("Download manually stopped");
            }
            bufferedOutputStream.write(buffer, 0, numRead);
            fetchedSize += numRead;
            for (int n = 0, i = downloadListener.size(); n < i; n++) {
                synchronized (downloadListener.get(n)) {
                    ((DownloadListener) downloadListener.get(n)).downloadProgress(this);
                }
            }
        }
        inputStream.close();
        bufferedOutputStream.close();
        if (file.toString().endsWith(".gz") || file.toString().endsWith(".gzip")) {
            for (int n = 0, i = downloadListener.size(); n < i; n++) {
                synchronized (downloadListener.get(n)) {
                    ((DownloadListener) downloadListener.get(n)).uncompressingProgress(this);
                }
            }
            try {
                GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(file));
                String fileName = file.toString().substring(0, file.toString().lastIndexOf("."));
                OutputStream outputStream = new FileOutputStream(fileName);
                byte[] unpackBuffer = new byte[1024];
                int length;
                while ((length = gzipInputStream.read(unpackBuffer)) > 0) {
                    outputStream.write(unpackBuffer, 0, length);
                }
                gzipInputStream.close();
                outputStream.close();
                file.delete();
                file = new File(fileName);
                file.setLastModified(date);
                failed = false;
                finished = true;
                for (int n = 0, i = downloadListener.size(); n < i; n++) {
                    synchronized (downloadListener.get(n)) {
                        ((DownloadListener) downloadListener.get(n)).uncompressingFinished(this);
                    }
                }
                for (int n = 0, i = downloadListener.size(); n < i; n++) {
                    synchronized (downloadListener.get(n)) {
                        ((DownloadListener) downloadListener.get(n)).downloadFinished(this);
                    }
                }
            } catch (IOException ioException) {
                file.delete();
                failed = true;
                for (int n = 0, i = downloadListener.size(); n < i; n++) {
                    synchronized (downloadListener.get(n)) {
                        ((DownloadListener) downloadListener.get(n)).exceptionWasThrown(this, ioException);
                    }
                }
            }
            try {
                Runtime.getRuntime().exec("chmod 777 " + file.getCanonicalPath());
            } catch (Exception exception) {
            }
        } else {
            failed = false;
            finished = true;
            file.setLastModified(date);
            for (int n = 0, i = downloadListener.size(); n < i; n++) {
                synchronized (downloadListener.get(n)) {
                    ((DownloadListener) downloadListener.get(n)).downloadFinished(this);
                }
            }
        }
    }

    public int getFetchedSize() {
        return fetchedSize;
    }

    public void setFetchedSize(int fetchedSize) {
        this.fetchedSize = fetchedSize;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        if (url != null && url.toString().endsWith(".gz") && !file.toString().endsWith(".gz")) {
            if (file.toString().endsWith(".")) {
                this.file = new File(file.toString() + "gz");
            } else {
                this.file = new File(file.toString() + ".gz");
            }
        } else {
            this.file = file;
        }
    }

    public synchronized int getSize() {
        return size;
    }

    public boolean exists() {
        try {
            url.openConnection().connect();
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public long getDate() {
        if (date < 0) {
            try {
                date = url.openConnection().getLastModified();
            } catch (Exception exception) {
                date = -1;
            }
        }
        return date;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public void addDownloadListener(DownloadListener downloadListener) {
        this.downloadListener.add(downloadListener);
    }

    public boolean removeDownloadListener(DownloadListener downloadListener) {
        return this.downloadListener.remove(downloadListener);
    }

    public String getFormatedSize() {
        return getFormatedSize(getSize());
    }

    public static String getFormatedSize(int size) {
        if (size < 0) {
            return Language.translateStatic("UNKNOWN");
        } else {
            long divisor = 1;
            char[] prefixChar = new char[] { ' ', 'k', 'm', 'g', 't' };
            int n = 0;
            for (int i = prefixChar.length; (double) size / (double) (divisor * 1024) >= 1 && n < i - 1; n++) {
                divisor *= 1024;
            }
            return (((int) (100 * (double) size / (double) divisor)) / 100d) + " " + ("" + prefixChar[n]).trim() + "Byte";
        }
    }

    public String getFormatedFetchedSize() {
        return getFormatedSize(getFetchedSize());
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public boolean isDownload() {
        return download || !isOptional();
    }

    public void setDownload(boolean download) {
        this.download = download;
    }

    public void stopDownload() {
        failed = true;
    }

    public boolean isFailed() {
        return failed;
    }

    public boolean isFinished() {
        return finished;
    }

    public String toString() {
        return file.toString();
    }

    public void setSize(int size) {
        this.size = size;
    }
}
