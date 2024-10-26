package instead.launcher.download;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.concurrent.Callable;
import ru.nxt.rvacheva.Helper;

/**
 * @author 7ectant
 */
final class DownloaderImpl implements Callable<File> {

    private static final int BLOCK_SIZE = 512;

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private final URL url;

    private final File file;

    private long size = -1;

    private int progress;

    DownloaderImpl(URL url, File file) {
        this.url = url;
        this.file = file;
    }

    void setSize(long size) {
        this.size = size;
    }

    void addListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public File call() throws IOException {
        HttpURLConnection conn = null;
        ReadableByteChannel fileDownloading = null;
        FileChannel fileWriting = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            if (size == -1) {
                size = conn.getContentLength();
            }
            fileDownloading = Channels.newChannel(conn.getInputStream());
            fileWriting = new FileOutputStream(file).getChannel();
            long left = size;
            long chunkSize = BLOCK_SIZE;
            for (long downloaded = 0; downloaded < size; left = size - downloaded) {
                if (left < BLOCK_SIZE) {
                    chunkSize = left;
                }
                fileWriting.transferFrom(fileDownloading, downloaded, chunkSize);
                downloaded += chunkSize;
                setProgress(downloaded);
            }
        } finally {
            if (file != null) {
                file.deleteOnExit();
            }
            if (conn != null) {
                conn.disconnect();
            }
            if (fileDownloading != null) {
                try {
                    fileDownloading.close();
                } catch (IOException ioe) {
                    Helper.logger.log(Level.SEVERE, "Не удалось закрыть поток скачивания", ioe);
                }
            }
            if (fileWriting != null) {
                try {
                    fileWriting.close();
                } catch (IOException ioe) {
                    Helper.logger.log(Level.SEVERE, "Не удалось закрыть поток записи в файл", ioe);
                }
            }
        }
        return file;
    }

    private void setProgress(long downloaded) {
        int newProgress = (int) (downloaded * 100 / size);
        pcs.firePropertyChange("progress", this.progress, this.progress = newProgress);
    }
}
