package de.shandschuh.jaolt.gui.error;

import java.awt.Container;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import de.shandschuh.jaolt.core.Language;
import de.shandschuh.jaolt.core.UpdateChannel;
import de.shandschuh.jaolt.gui.ErrorJDialog;
import de.shandschuh.jaolt.gui.Lister;
import de.shandschuh.jaolt.tools.log.Logger;

public class SimpleErrorReporter extends Thread {

    private JDialog dialog;

    private String sender;

    private String summary;

    private String description;

    private Throwable exception;

    private boolean attachErrorLog;

    public SimpleErrorReporter(JDialog dialog) {
        this.dialog = dialog;
    }

    public SimpleErrorReporter() {
        this(null);
    }

    public void reportError(String summary, String description, Throwable exception, boolean attachErrorLog) {
        this.summary = summary;
        this.description = description;
        this.exception = exception;
        this.attachErrorLog = attachErrorLog;
        start();
    }

    public void run() {
        StringBuffer messageStringBuffer = new StringBuffer();
        messageStringBuffer.append("Program: \t" + UpdateChannel.getCurrentChannel().getApplicationTitle() + "\n");
        messageStringBuffer.append("Version: \t" + Lister.version + "\n");
        messageStringBuffer.append("Revision: \t" + Lister.revision + "\n");
        messageStringBuffer.append("Channel: \t" + UpdateChannel.getCurrentChannel().getName() + "\n");
        messageStringBuffer.append("Date: \t\t" + Lister.date + "\n\n");
        messageStringBuffer.append("OS: \t\t" + System.getProperty("os.name") + " (" + System.getProperty("os.version") + ")\n");
        messageStringBuffer.append("JAVA: \t\t" + System.getProperty("java.version") + " (" + System.getProperty("java.specification.vendor") + ")\n");
        messageStringBuffer.append("Desktop: \t" + System.getProperty("sun.desktop") + "\n");
        messageStringBuffer.append("Language: \t" + Language.getCurrentInstance() + "\n\n");
        messageStringBuffer.append("------------------------------------------\n");
        if (summary != null) {
            messageStringBuffer.append(summary + "\n\n");
        }
        messageStringBuffer.append("Details:\n");
        if (description != null) {
            messageStringBuffer.append(description);
        }
        if (exception != null) {
            messageStringBuffer.append("\n\nStacktrace:\n");
            printStackTrace(exception, messageStringBuffer);
        }
        try {
            if (dialog != null) {
                setComponentsEnabled(dialog.getContentPane(), false);
            }
            URL url = UpdateChannel.getCurrentChannel().getErrorReportURL();
            URLConnection urlConnection = url.openConnection();
            urlConnection.setDoOutput(true);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(urlConnection.getOutputStream());
            if (sender != null) {
                outputStreamWriter.write(URLEncoder.encode("sender", "UTF-8") + "=" + URLEncoder.encode(sender, "UTF-8"));
                outputStreamWriter.write("&");
            }
            outputStreamWriter.write(URLEncoder.encode("report", "UTF-8") + "=" + URLEncoder.encode(messageStringBuffer.toString(), "UTF-8"));
            if (attachErrorLog) {
                outputStreamWriter.write("&");
                outputStreamWriter.write(URLEncoder.encode("error.log", "UTF-8") + "=" + URLEncoder.encode(Logger.getErrorLogContent(), "UTF-8"));
            }
            outputStreamWriter.flush();
            urlConnection.getInputStream().close();
            outputStreamWriter.close();
            if (dialog != null) {
                dialog.dispose();
            }
            JOptionPane.showMessageDialog(Lister.getCurrentInstance(), Language.translateStatic("MESSAGE_ERRORREPORTSENT"));
        } catch (Exception exception) {
            ErrorJDialog.showErrorDialog(dialog, exception);
            if (dialog != null) {
                setComponentsEnabled(dialog.getContentPane(), true);
            }
        }
    }

    public static void printStackTrace(Throwable throwable, StringBuffer stringBuffer) {
        if (throwable != null) {
            stringBuffer.append("\n" + throwable);
            StackTraceElement[] stackTraceElements = throwable.getStackTrace();
            for (int n = 0, i = stackTraceElements.length; n < i; n++) {
                stringBuffer.append("\n     " + stackTraceElements[n]);
            }
            printStackTrace(throwable.getCause(), stringBuffer);
        }
    }

    public void setSender(String email, String name) {
        if (name.length() > 0) {
            sender = name + " <" + email + ">";
        } else {
            sender = email;
        }
    }

    private static void setComponentsEnabled(Container component, boolean enabled) {
        if (component != null) {
            component.setEnabled(enabled);
            for (int n = 0, i = component.getComponentCount(); n < i; n++) {
                setComponentsEnabled((Container) component.getComponent(n), enabled);
            }
        }
    }
}
