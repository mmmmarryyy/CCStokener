package com.myopa.logic.core;

import com.myopa.dao.SchemaVersionDAO;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;

/**
 * This class connects to the SF.net webservers to check for updates.
 *
 * @author Paul Campbell <myopa@users.sourceforge.net>
 */
public class UpdateCheck {

    public static final double APPLICATION_VERSION = 0.1;

    private boolean m_updateAvailable = false;

    private double currentSchemaVersion = 0;

    public UpdateCheck(boolean admin) {
        m_updateAvailable = checkForUpdates(admin);
    }

    public boolean updateAvailable() {
        return m_updateAvailable;
    }

    public double getSchemaVersion() {
        return currentSchemaVersion;
    }

    public double getApplicationVersion() {
        return APPLICATION_VERSION;
    }

    private boolean checkForUpdates(boolean admin) {
        try {
            URL url = new URL("http://myopa.sourceforge.net/version.txt");
            InputStream in = url.openStream();
            BufferedInputStream buffIn = new BufferedInputStream(in);
            String tmp = "";
            int data = buffIn.read();
            while (data != -1) {
                tmp = tmp.concat(Character.toString((char) data));
                data = buffIn.read();
            }
            String[] rows = tmp.split("\n");
            if (rows.length > 0) {
                String[] rowComponents = rows[0].split(":");
                if (rowComponents.length > 0) {
                    double current_version = Double.parseDouble(rowComponents[0]);
                    if (current_version > APPLICATION_VERSION) {
                        if (admin || Double.parseDouble(rowComponents[4]) == loadSchemaVersion()) {
                            currentSchemaVersion = loadSchemaVersion();
                            return true;
                        } else {
                            currentSchemaVersion = loadSchemaVersion();
                            for (int i = 1; i < rows.length; i++) {
                                rowComponents = rows[i].split(":");
                                if (Double.parseDouble(rowComponents[4]) == currentSchemaVersion) {
                                    return true;
                                }
                            }
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            } else {
                return false;
            }
        } catch (java.io.IOException e) {
            return false;
        } catch (NumberFormatException numF) {
            return false;
        }
        return false;
    }

    private double loadSchemaVersion() {
        try {
            return SchemaVersionDAO.getInstance().getVersion();
        } catch (Exception e) {
        }
        return 0;
    }
}
