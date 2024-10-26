package org.jarp.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import javax.swing.JOptionPane;

/**
 * Implementation of exception handler.
 * This exception handler will try to send some system information 
 * (such as JVM version, system version, memory available, etc...),
 * the exception throwed and some user feedback.
 * This is done through SourceForge form email system.
 * 
 * @version $Revision: 1.7 $
 * @author <a href="mailto:ricardo_padilha@users.sourceforge.net">Ricardo 
 * Sangoi Padilha</a>
 */
public class URLPostExceptionHandler {

    public static String DEFAULT_PROTOCOL = "http";

    public static String DEFAULT_HOST = "sourceforge.net";

    public static String DEFAULT_FORM = "/sendmessage.php";

    private static String FORM_DATA1 = "touser=137238";

    private static String FORM_DATA2 = "toaddress=";

    private static String USER_EMAIL = "email=";

    private static String USER_NAME = "name=";

    private static String USER_SUBJECT = "subject=";

    private static String USER_BODY = "body=";

    private static String USER_SEND_MAIL = "send_mail=Send_Message";

    /**
	 * No description available.
	 * @param args
	 */
    public static void main(String[] args) {
        URLPostExceptionHandler.handleException(new NullPointerException("Null pointer somewhere!"));
    }

    /**
	 * Handles an <code>Exception</code> with test data 
	 * by posting it to Sourceforge.
	 * @param e
	 */
    public static void handleException(Exception e) {
        StringBuffer sb = new StringBuffer();
        sb.append(FORM_DATA1);
        sb.append("&");
        sb.append(FORM_DATA2);
        sb.append("&");
        sb.append(USER_EMAIL + "a@test.com");
        sb.append("&");
        sb.append(USER_NAME + "A. Test");
        sb.append("&");
        sb.append(USER_SUBJECT + "Test subject");
        sb.append("&");
        sb.append(USER_BODY + "Test body.\n\n" + e.getMessage());
        sb.append("&");
        sb.append(USER_SEND_MAIL);
        URLPostExceptionHandler h = new URLPostExceptionHandler();
        try {
            h.postData(null, null, null, sb.toString());
        } catch (Exception ex) {
            h.saveData(ex);
        }
    }

    /**
	 * Post a string data to a form, using POST.
	 * @param protocol Usually "http"
	 * @param host
	 * @param form
	 * @param data
	 * @throws Exception Any exception that may be throwed
	 */
    public void postData(String protocol, String host, String form, String data) throws Exception {
        if ((protocol == null) || (protocol.equals(""))) {
            protocol = DEFAULT_PROTOCOL;
        }
        if ((host == null) || (host.equals(""))) {
            host = DEFAULT_HOST;
        }
        if (form == null) {
            form = DEFAULT_FORM;
        }
        if (data == null) {
            throw new IllegalArgumentException("Invalid data");
        }
        URL url = new URL(protocol, host, form);
        URLConnection con = url.openConnection();
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setUseCaches(false);
        con.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
        con.setRequestProperty("Content-length", String.valueOf(data.length()));
        PrintStream out = new PrintStream(con.getOutputStream(), true);
        out.print(data);
        out.close();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        while (in.readLine() != null) {
        }
        in.close();
    }

    /**
	 * Shows a dialog to report an Exception,
	 * try to save the information to a file.
	 * @param ex java.lang.Exception
	 */
    protected void saveData(Exception ex) {
        String logFilename = "jarp.log";
        Object[] message = { ex.toString(), "Save exception to " + logFilename + "?" };
        int ret = JOptionPane.showConfirmDialog(null, message, "Tool Exception", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
        if (ret == 0) {
            try {
                OutputStream fos = new FileOutputStream(logFilename, true);
                OutputStream bos = new BufferedOutputStream(fos);
                PrintStream out = new PrintStream(bos);
                out.println("Tool exception " + new Date());
                ex.printStackTrace(out);
                out.close();
                bos.close();
                fos.close();
            } catch (Exception ignore) {
            }
        }
    }
}
