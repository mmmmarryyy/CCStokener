package com.sqltablet;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import javax.swing.JOptionPane;
import com.sqltablet.db.Database;
import com.sqltablet.gui.ConfirmCustomerFeedback;

public class RemoteLogger extends Thread {

    private static RemoteLogger INSTANCE = null;

    private static String threadName = "RemoteLogger";

    private static boolean DEBUG = true;

    private ArrayList<String> errors;

    public static synchronized RemoteLogger getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RemoteLogger();
            INSTANCE.start();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return INSTANCE;
    }

    private RemoteLogger() {
        super(threadName);
        errors = new ArrayList<String>();
    }

    private void queue(String str) {
        synchronized (this) {
            errors.add(str);
            notify();
        }
    }

    private String getHeader() {
        String header = "";
        Configuration conf = Configuration.getInstance();
        header += conf.getApplicationName() + "\n";
        header += new Date().toString() + "\n";
        header += "Build number " + conf.getBuildNumber() + "\n";
        header += "Build date " + conf.getBuildDate() + "\n";
        Database db = SqlTablet.getInstance().getDatabase();
        String driver;
        if (db != null) {
            driver = db.getDriver();
            if (driver == null) driver = "(not connected)";
        } else driver = "(not connected)";
        header += "Database Driver: " + driver + "\n";
        header += "\n";
        return header;
    }

    private String getProperties() {
        String log = new String();
        log += "PROPERTIES:\n";
        Properties props = System.getProperties();
        String keys[] = new String[props.size()];
        int i = 0;
        for (Enumeration iter = props.keys(); iter.hasMoreElements(); ) {
            String key = (String) iter.nextElement();
            keys[i] = key;
            i++;
        }
        Arrays.sort(keys);
        for (i = 0; i < keys.length; i++) {
            String key = keys[i];
            String value = (String) props.getProperty(key);
            log += key + "=" + value + "\n";
        }
        log += "\n";
        return log;
    }

    private String getMessage(String msg) {
        String log = new String();
        log += "MESSAGE:\n";
        if (msg != null) {
            msg = msg.replace("\r", "\\r");
            msg = msg.replace("\n", "\\n");
            msg = msg.replace("\t", "\\t");
            log += msg + "\n";
        } else log += "None.\n";
        return log;
    }

    public void logProperties(String msg) {
        String log = new String();
        log += getHeader();
        log += getProperties();
        log += getMessage(msg);
        queue(log);
    }

    public void logMessage(String msg) {
        String log = new String();
        log += getHeader();
        log += getMessage(msg);
        queue(log);
    }

    public void logError(Throwable e) {
        String command = CommandHelper.getInstance().getCommandString();
        logError(e, command);
    }

    public void logError(Throwable e, String msg) {
        String log = new String();
        log += getHeader();
        log += "EXCEPTION:\n";
        log += e.toString() + "\n";
        StackTraceElement[] stackTrace = e.getStackTrace();
        for (int i = 0; i < stackTrace.length; i++) log += "\tat " + stackTrace[i].toString() + "\n";
        log += "\n";
        log += getMessage(msg);
        String user_dir = System.getProperty("user.dir");
        if (user_dir != null && user_dir.contains("eclipse")) e.printStackTrace(); else queue(log);
    }

    private void sendMessages() {
        Configuration conf = Configuration.getInstance();
        for (int i = 0; i < errors.size(); i++) {
            String msg = null;
            synchronized (this) {
                msg = errors.get(i);
                if (DEBUG) System.out.println(msg);
                errors.remove(i);
            }
            if (!conf.getCustomerFeedback()) continue;
            if (conf.getApproveCustomerFeedback()) {
                ConfirmCustomerFeedback dialog = new ConfirmCustomerFeedback(JOptionPane.getFrameForComponent(SqlTablet.getInstance()), msg);
                if (dialog.getResult() == ConfirmCustomerFeedback.Result.NO) continue;
            }
            try {
                URL url = new URL("http://www.sqltablet.com/beta/bug.php");
                URLConnection urlc = url.openConnection();
                urlc.setDoOutput(true);
                urlc.setDoOutput(true);
                urlc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                DataOutputStream out = new DataOutputStream(urlc.getOutputStream());
                String lines[] = msg.split("\n");
                for (int l = 0; l < lines.length; l++) {
                    String line = (l > 0 ? "&line" : "line") + l + "=";
                    line += URLEncoder.encode(lines[l], "UTF-8");
                    out.write(line.getBytes());
                }
                out.flush();
                out.close();
                BufferedReader in = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    if (DEBUG) System.out.println("RemoteLogger : " + line + "\n");
                }
                in.close();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void run() {
        while (true) {
            try {
                synchronized (this) {
                    this.wait();
                }
            } catch (InterruptedException e) {
                RemoteLogger.getInstance().logError(e);
            }
            sendMessages();
        }
    }
}
