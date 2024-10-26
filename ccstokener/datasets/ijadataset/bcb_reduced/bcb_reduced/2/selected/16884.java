package org.opencdspowered.opencds.ui.error;

import org.opencdspowered.opencds.ui.main.*;
import org.opencdspowered.opencds.core.config.*;
import org.opencdspowered.opencds.core.logging.*;
import org.opencdspowered.opencds.core.util.Constants;
import javax.swing.*;
import java.awt.Frame;
import java.util.*;
import java.net.*;
import java.io.*;

/**
 * The class that has exceptions saved.
 *
 * @author  Lars 'Levia' Wesselius
*/
public class ExceptionList implements SettingChangedListener, ExceptionListener {

    private ArrayList<Error> m_ExceptionList = new ArrayList<Error>();

    private MenuBar m_MenuBar;

    private boolean m_ShouldShowIcon = false;

    public ExceptionList() {
        Logger.getInstance().addExceptionListener(this);
        ConfigurationManager.getInstance().addSettingChangedListener(this);
    }

    /**
     * Initializes the ExceptionList.
    */
    public void initialize(MenuBar bar) {
        m_MenuBar = bar;
    }

    /**
     * Add an exception.
     * 
     * @param   e           The exception to add.
     * @param   reportable  Whether this exception can be reported.
    */
    public void addException(Exception e, boolean reportable) {
        ConfigurationManager cfgMgr = ConfigurationManager.getInstance();
        String cfg = cfgMgr.getValue("Logging.AutoSubmit");
        Error err = new Error(e, reportable);
        if (cfg == null || cfg.equals("true")) {
            submitException(err);
        } else {
            if (m_ExceptionList.isEmpty()) {
                if (m_MenuBar != null) {
                    m_MenuBar.exceptionOccured();
                    m_ShouldShowIcon = false;
                } else {
                    m_ShouldShowIcon = true;
                }
            } else {
                if (m_ShouldShowIcon) {
                    m_MenuBar.exceptionOccured();
                }
            }
            m_ExceptionList.add(err);
        }
    }

    /**
     * Clear the exception list.
    */
    public void clear() {
        m_ExceptionList.clear();
        m_MenuBar.exceptionsCleared();
    }

    /**
     * Get the exceptions.
     * 
     * @return  An Iterator with the exceptions.
    */
    public Iterator<Error> getExceptions() {
        return m_ExceptionList.iterator();
    }

    /**
     * Get the amount of errors.
     * 
     * @return  An integer with the amount of errors.
    */
    public int getErrorCount() {
        return m_ExceptionList.size();
    }

    /**
     * Submit all exceptions.
    */
    public void submitAllExceptions() {
        for (Iterator<Error> it = m_ExceptionList.iterator(); it.hasNext(); ) {
            Error ex = it.next();
            submitException(ex);
        }
        clear();
    }

    /**
     * Submit an exception.
     * 
     * @param   err   The exception to submit.
    */
    public void submitException(Error err) {
        if (!err.isReportable()) {
            return;
        }
        try {
            String data = URLEncoder.encode("mode", "UTF-8") + "=" + URLEncoder.encode("auto", "UTF-8");
            data += "&" + URLEncoder.encode("stack", "UTF-8") + "=" + URLEncoder.encode(err.toString(), "UTF-8");
            data += "&" + URLEncoder.encode("jvm", "UTF-8") + "=" + URLEncoder.encode(System.getProperty("java.version"), "UTF-8");
            data += "&" + URLEncoder.encode("ocdsver", "UTF-8") + "=" + URLEncoder.encode(Constants.OPENCDS_VERSION, "UTF-8");
            data += "&" + URLEncoder.encode("os", "UTF-8") + "=" + URLEncoder.encode(Constants.OS_NAME + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"), "UTF-8");
            URL url = new URL(Constants.BUGREPORT_SCRIPT);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                System.out.println(line);
            }
            wr.close();
            rd.close();
        } catch (Exception ex) {
            org.opencdspowered.opencds.core.logging.Logger.getInstance().logException(ex, false);
        }
    }

    /**
     * Overriden, fired when an exception was thrown.
     *
     * @param   exception   The exception.
     * @param   reportable  Whether this exception can be reported.
     * @param   date        The date on which the event occured.
    */
    public void logException(Exception exception, boolean reportable, Date date) {
        String cfg = ConfigurationManager.getInstance().getValue("Logging.IgnoreExceptions");
        if (cfg == null || cfg.equals("false")) {
            addException(exception, reportable);
        }
    }

    public void settingChanged(String key, String oldSetting, String newSetting) {
        if (key.equals("Logging.IgnoreErrors")) {
            if (newSetting.equals("true")) {
                clear();
            }
        } else if (key.equals("Logging.AutoSubmit")) {
            if (newSetting.equals("true")) {
                submitAllExceptions();
            }
        }
    }
}
