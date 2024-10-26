package jhelpgenerator.utils;

import java.io.*;
import java.net.*;
import java.util.logging.Level;

/**
 *
 * @author v3r5_u5
 */
public class UpdateManager extends Thread {

    private boolean autoUpdate;

    private String updateUrl;

    private int updateChekInterval;

    private Global global;

    public UpdateManager() {
        setAutoUpdate(false);
        setUpdateChekInterval(5000);
    }

    public UpdateManager(boolean autoUpdate, int updateChekInterval) {
        setAutoUpdate(autoUpdate);
        setUpdateChekInterval(updateChekInterval);
    }

    public UpdateManager(boolean autoUpdate) {
        super();
        setAutoUpdate(autoUpdate);
    }

    public UpdateManager(boolean autoUpdate, String updateUrl, int updateChekInterval) {
        setAutoUpdate(autoUpdate);
        setUpdateUrl(updateUrl);
        setUpdateChekInterval(updateChekInterval);
    }

    public boolean isAutoUpdate() {
        return autoUpdate;
    }

    public void setAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
    }

    public String getUpdateUrl() {
        return updateUrl;
    }

    public void setUpdateUrl(String updateUrl) {
        this.updateUrl = updateUrl;
    }

    public void checkUpdate() {
        try {
            URL url = new URL(getUpdateUrl());
            URLConnection connection = url.openConnection();
            InputStream stream = connection.getInputStream();
            int contentLength = stream.available();
            byte[] buffer = new byte[contentLength];
            int bytesread = 0;
            int offset = 0;
            bytesread = stream.read(buffer, offset, contentLength);
            offset += bytesread;
            String newVersion = new String(buffer);
            if (!newVersion.equalsIgnoreCase("version:" + Global.version)) {
                global.getLogger().log(Level.INFO, "New version available!!!");
            }
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    @Override
    public void run() {
        for (; ; ) {
            if (isAutoUpdate()) {
                checkUpdate();
            }
            try {
                Thread.sleep(getUpdateChekInterval());
            } catch (InterruptedException ex) {
                System.out.println(ex.getLocalizedMessage());
            }
        }
    }

    public int getUpdateChekInterval() {
        return updateChekInterval;
    }

    public void setUpdateChekInterval(int updateChekInterval) {
        this.updateChekInterval = updateChekInterval;
    }

    public Global getGlobal() {
        return global;
    }

    public void setGlobal(Global global) {
        this.global = global;
    }
}
