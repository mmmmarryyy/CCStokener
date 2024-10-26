package net.flysource.server;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class StatusRunnable implements Runnable {

    private DataSource dataSource;

    private StatusMonitor statusMonitor;

    private static final Logger LOG = Logger.getLogger(StatusRunnable.class.getName());

    private int prevNumUsers = 0;

    private int prevNumFiles = 0;

    public StatusRunnable(StatusMonitor statusMonitor, DataSource dataSource) {
        this.dataSource = dataSource;
        this.statusMonitor = statusMonitor;
    }

    public void run() {
        while (true) {
            statusMonitor.setStatusMessage(getStatusMsg());
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
            }
        }
    }

    private String getStatusMsg() {
        int numFiles = 0;
        int numUsers = 0;
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs;
        try {
            conn = dataSource.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("select count(*) from users");
            if (rs.next()) {
                numUsers = rs.getInt(1);
            }
            try {
                rs.close();
            } catch (Exception e) {
            }
            rs = stmt.executeQuery("select count(distinct crc) from directory");
            if (rs.next()) {
                numFiles = rs.getInt(1);
            }
            try {
                rs.close();
            } catch (Exception e) {
            }
        } catch (SQLException e) {
            LOG.severe(e.getMessage());
            return "Server problem, unable to determine network status.";
        } finally {
            try {
                stmt.close();
            } catch (Exception e) {
            }
            try {
                conn.close();
            } catch (Exception e) {
            }
        }
        if (prevNumUsers != numUsers || prevNumFiles != numFiles) {
            LOG.info("Status Users: " + numUsers + "/" + WebServicesImpl.MAX_USERS + " Files: " + numFiles);
            prevNumUsers = numUsers;
            prevNumFiles = numFiles;
        }
        updateWebsiteStatus(numUsers, numFiles);
        return numFiles + " flys are being shared by " + numUsers + " users on the FlySource network.";
    }

    private void updateWebsiteStatus(int numUsers, int numFiles) {
        BufferedReader input = null;
        try {
            URL url = new URL(WebServicesImpl.BASE_WEBSITE_URL + "/admin/status.php?pw=kujukudu&users=" + numUsers + "&files=" + numFiles);
            input = new BufferedReader(new InputStreamReader(url.openStream()));
        } catch (MalformedURLException e) {
            LOG.severe(e.getMessage());
        } catch (IOException e) {
            LOG.severe(e.getMessage());
        } finally {
            try {
                input.close();
            } catch (Exception e) {
            }
        }
    }
}
