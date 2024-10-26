package net.flysource.server;

import org.apache.commons.lang.StringUtils;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

public class RegistrationRunnable implements Runnable {

    private DataSource dataSource;

    private RegistrationMonitor statusMonitor;

    private ArrayList doneQueue = new ArrayList();

    private static final Logger LOG = Logger.getLogger(RegistrationRunnable.class.getName());

    public RegistrationRunnable(RegistrationMonitor statusMonitor, DataSource dataSource) {
        this.dataSource = dataSource;
        this.statusMonitor = statusMonitor;
    }

    public void run() {
        while (true) {
            doUserRegTask();
            doInactiveUserTask();
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
            }
        }
    }

    private void doUserRegTask() {
        LOG.info("registration task invoked.");
        ArrayList users = getNewUsers();
        if (users.size() == 0) {
            processDoneQueue();
            return;
        }
        boolean ok;
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            Iterator iter = users.iterator();
            while (iter.hasNext()) {
                String rec = (String) iter.next();
                if (StringUtils.trimToEmpty(rec).length() == 0) continue;
                LOG.info("Data [" + rec + "]");
                String fields[] = rec.split("~", 4);
                LOG.info("Adding user " + fields[0] + " " + fields[2]);
                addLogin(conn, fields[0], fields[1], fields[2], fields[3]);
                ok = sendDoneMsg(fields[0]);
                if (!ok) {
                    doneQueue.add(fields[0]);
                    LOG.info("Notify queued for " + fields[0] + "(size=" + doneQueue.size() + ")");
                }
            }
        } catch (SQLException e) {
            LOG.severe("Database error registering user. " + e.getMessage());
        } finally {
            try {
                conn.close();
            } catch (Exception e) {
            }
        }
    }

    private ArrayList getNewUsers() {
        ArrayList users = new ArrayList();
        BufferedReader input = null;
        try {
            URL url = new URL(WebServicesImpl.BASE_WEBSITE_URL + "/admin/regtask.php?pw=kujukudu&action=new");
            input = new BufferedReader(new InputStreamReader(url.openStream()));
            String rec = input.readLine();
            if (rec.indexOf("NONE") == -1) {
                while (rec != null) {
                    users.add(rec);
                    rec = input.readLine();
                }
            }
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
        return users;
    }

    private void addLogin(Connection conn, String userId, String password, String name, String email) throws SQLException {
        PreparedStatement insert = conn.prepareStatement("insert into logins(user_id, password, name, email) values(?,?,?,?)");
        insert.setString(1, userId);
        insert.setString(2, password);
        insert.setString(3, name);
        insert.setString(4, email);
        insert.executeUpdate();
    }

    private boolean sendDoneMsg(String userId) {
        BufferedReader input = null;
        try {
            URL url = new URL(WebServicesImpl.BASE_WEBSITE_URL + "/admin/regtask.php?pw=kujukudu&action=done&userid=" + userId);
            input = new BufferedReader(new InputStreamReader(url.openStream()));
            String result = input.readLine();
            if (result != null && result.indexOf("OK") != -1) return true;
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
        return false;
    }

    private void processDoneQueue() {
        if (doneQueue.size() == 0) return;
        LOG.info("Processing done queue (size=" + doneQueue.size() + ")");
        ArrayList queue = (ArrayList) doneQueue.clone();
        boolean ok;
        Iterator iter = queue.iterator();
        while (iter.hasNext()) {
            String userId = (String) iter.next();
            ok = sendDoneMsg(userId);
            if (ok) {
                LOG.info("Notify OK for " + userId);
                doneQueue.remove(userId);
            } else {
                LOG.info("Notify FAILED for " + userId);
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        }
    }

    private void doInactiveUserTask() {
        LOG.info("inactive user task invoked.");
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("select session_id, user_id, timediff(now(), last_seen) from users where minute(timediff(now(), last_seen)) > 10");
            while (rs.next()) {
                String sessionId = rs.getString(1);
                String userId = rs.getString(2);
                String diff = rs.getString(3);
                LOG.info("Inactive user: " + userId + " session " + sessionId + " time " + diff);
                Toolbox.getWebServicesImpl().doLogout(sessionId);
            }
        } catch (SQLException e) {
            LOG.severe("Database error inactive users. " + e.getMessage());
        } finally {
            try {
                rs.close();
            } catch (Exception e) {
            }
            try {
                conn.close();
            } catch (Exception e) {
            }
        }
    }
}
