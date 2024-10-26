package hm.core.utils;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ExceptionHelper {

    private static String STANDART_ERROR_MESSAGE = "Es ist ein Fehler aufgetreten";

    public static void showErrorDialog(String message, Exception e) {
        String[] s = { "OK", "Details" };
        JOptionPane jo = new JOptionPane();
        if (message != null && !message.equals("")) {
            jo.setMessage(message);
        } else {
            jo.setMessage("Error/Fehler");
        }
        jo.setOptions(s);
        jo.setMessageType(JOptionPane.ERROR_MESSAGE);
        JDialog d = jo.createDialog(null, STANDART_ERROR_MESSAGE);
        d.setVisible(true);
        if (jo.getValue() == s[1]) {
            showStackTraceDialog(e);
        }
    }

    public static void showErrorDialog(Exception e) {
        showErrorDialog(e.getMessage(), e);
    }

    public static void showErrorDialog(Throwable e) {
        showErrorDialog(new Exception(e));
    }

    private static void showStackTraceDialog(Exception e) {
        e.printStackTrace();
        JScrollPane sp = new JScrollPane(getStackTraceJTextArea(e));
        sp.setPreferredSize(new Dimension(640, 300));
        JOptionPane.showMessageDialog(null, sp, "Error - " + e.getClass().getName(), JOptionPane.ERROR_MESSAGE);
    }

    private static JTextArea getStackTraceJTextArea(Exception e) {
        StackTraceElement[] st = e.getStackTrace();
        String message = e.getMessage();
        StringBuffer buff = new StringBuffer();
        buff.append(message);
        buff.append("\n");
        if (message != null) {
            for (int i = 0; i < message.length(); i++) {
                buff.append("-");
            }
            buff.append("\n");
        }
        for (int j = 0; j < st.length; j++) {
            buff.append(st[j]);
            buff.append("\n");
        }
        JTextArea ta = new JTextArea(buff.toString());
        ta.setEditable(false);
        return ta;
    }

    /**
     * Sendet eine email an eine angegebene WebAdresse.
     * Der Programmname, die Programmversion und der Stacktrace werden an diese Adressen
     * per POST Variable �bergeben.
     * 
     * 
     * @param urlString
     * @param programName
     * @param programVersion
     * @param ex
     */
    public static void sendErrorEmail(String urlString, String programName, String programVersion, Exception ex) {
        class Versenden extends Thread {

            private String urlString;

            private String programName;

            private String programVersion;

            private Exception ex;

            public Versenden(String urlString, String programName, String programVersion, Exception ex) {
                this.ex = ex;
                this.programName = programName;
                this.programVersion = programVersion;
                this.urlString = urlString;
            }

            public void run() {
                try {
                    String stacktrace;
                    {
                        StringBuffer buff = new StringBuffer();
                        buff.append(ex.toString());
                        buff.append("\n\n");
                        StackTraceElement[] elem = ex.getStackTrace();
                        for (int i = 0; i < elem.length; i++) {
                            buff.append(elem[i]);
                            buff.append("\n");
                        }
                        stacktrace = buff.toString();
                    }
                    String data = URLEncoder.encode("version", "UTF-8") + "=" + URLEncoder.encode(programVersion, "UTF-8");
                    data += "&" + URLEncoder.encode("program", "UTF-8") + "=" + URLEncoder.encode(programName, "UTF-8");
                    data += "&" + URLEncoder.encode("stacktrace", "UTF-8") + "=" + URLEncoder.encode(stacktrace, "UTF-8");
                    if (ex.getMessage() != null) {
                        data += "&" + URLEncoder.encode("message", "UTF-8") + "=" + URLEncoder.encode(ex.getMessage(), "UTF-8");
                    }
                    URL url = new URL(urlString);
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
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        new Versenden(urlString, programName, programVersion, ex).start();
    }
}
