package rsdowloader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Random;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

/**
 *
 * @author mb
 */
public class Downloader implements Runnable {

    private JButton stopButton;

    private JTextArea logArea;

    private Vector<RSLink> tasks;

    public Downloader(Vector<RSLink> tasks, JButton stopButton, JTextArea logArea) {
        this.tasks = tasks;
        this.stopButton = stopButton;
        this.logArea = logArea;
    }

    @Override
    public void run() {
        RSLink link = null;
        while ((link = selectLink()) != null) {
            if (!Main.start.get()) {
                return;
            }
            download(link);
        }
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                stopButton.doClick();
            }
        });
    }

    private RSLink selectLink() {
        Vector<RSLink> highLinks = new Vector<RSLink>();
        Vector<RSLink> normalLinks = new Vector<RSLink>();
        Vector<RSLink> lowLinks = new Vector<RSLink>();
        RSLink link = null;
        for (int i = 0; i < tasks.size(); i++) {
            link = tasks.get(i);
            if (link.getStatus() == RSLink.STATUS_NOTHING) {
                switch(link.getPriority()) {
                    case RSLink.PRIORITY_HIGH:
                        highLinks.add(link);
                        break;
                    case RSLink.PRIORITY_NORMAL:
                        normalLinks.add(link);
                        break;
                    case RSLink.PRIORITY_LOW:
                        lowLinks.add(link);
                        break;
                }
            }
        }
        if (!highLinks.isEmpty()) {
            return highLinks.get(new Random(new Date().getTime()).nextInt(highLinks.size()));
        }
        if (!normalLinks.isEmpty()) {
            return normalLinks.get(new Random(new Date().getTime()).nextInt(normalLinks.size()));
        }
        if (!lowLinks.isEmpty()) {
            return lowLinks.get(new Random(new Date().getTime()).nextInt(lowLinks.size()));
        }
        return null;
    }

    private void download(RSLink link) {
        URL url1 = link.getUrl();
        URL url2 = null;
        URL url3 = null;
        int delay = 0;
        Object[] res;
        if (!Main.start.get()) {
            link.setStatus(RSLink.STATUS_NOTHING);
            return;
        }
        url2 = retrieveFirstURL(url1, link);
        if (url2 == null) return;
        if (!Main.start.get()) {
            link.setStatus(RSLink.STATUS_NOTHING);
            return;
        }
        res = retrieveSecondURL(url2, link);
        if (res[0] == null || res[1] == null) return;
        url3 = (URL) res[0];
        delay = (Integer) res[1];
        if (!Main.start.get()) {
            link.setStatus(RSLink.STATUS_NOTHING);
            return;
        }
        waitForDownload(delay, link);
        if (!Main.start.get()) {
            link.setStatus(RSLink.STATUS_NOTHING);
            return;
        }
        retrieveFile(url3, link);
    }

    private void log(final String msg) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                logArea.setText(logArea.getText() + new Date() + " - " + msg + "\n");
            }
        });
    }

    private URL retrieveFirstURL(URL url, RSLink link) {
        link.setStatus(RSLink.STATUS_WAITING);
        URL result = null;
        HttpURLConnection httpConn = null;
        BufferedReader inr = null;
        Pattern formStartPattern = Pattern.compile("<form.+action=\"");
        Pattern freeUserPattern = Pattern.compile("input type=\"submit\" value=\"Free user\"");
        Pattern formEndPattern = Pattern.compile("</form>");
        Pattern urlString = Pattern.compile("http://[a-zA-Z0-9\\.\\-/_]+");
        try {
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setDoOutput(false);
            httpConn.setDoInput(true);
            inr = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            String line = null;
            String urlLine = null;
            boolean freeUser = false;
            Matcher matcher = null;
            while ((line = inr.readLine()) != null) {
                if (urlLine == null) {
                    matcher = formStartPattern.matcher(line);
                    if (matcher.find()) {
                        urlLine = line;
                    }
                } else {
                    matcher = formEndPattern.matcher(line);
                    if (matcher.find()) {
                        urlLine = null;
                    } else {
                        matcher = freeUserPattern.matcher(line);
                        if (matcher.find()) {
                            freeUser = true;
                            break;
                        }
                    }
                }
            }
            if (freeUser) {
                matcher = urlString.matcher(urlLine);
                if (matcher.find()) {
                    result = new URL(matcher.group());
                }
            }
        } catch (MalformedURLException ex) {
            log("Malformed URL Exception!");
        } catch (IOException ex) {
            log("I/O Exception!");
        } finally {
            try {
                if (inr != null) inr.close();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Can not close some connections:\n" + ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
            }
            if (httpConn != null) httpConn.disconnect();
            link.setStatus(RSLink.STATUS_NOTHING);
            return result;
        }
    }

    private Object[] retrieveSecondURL(URL url, RSLink link) {
        link.setStatus(RSLink.STATUS_WAITING);
        Object[] result = new Object[2];
        HttpURLConnection httpConn = null;
        BufferedReader inr = null;
        DataOutputStream outs = null;
        Pattern mirrorLinePattern = Pattern.compile("'<input.+checked.+type=\"radio\".+name=\"mirror\".+\\\\'.+\\\\'");
        Pattern mirrorUrlPattern = Pattern.compile("\\\\'.+\\\\'");
        Pattern counterPattern = Pattern.compile("var c=[0-9]+;");
        Pattern counterIntPattern = Pattern.compile("[0-9]+");
        try {
            String line = null;
            String urlLine = null;
            Integer counter = null;
            String postData = URLEncoder.encode("dl.start", "UTF-8") + "=" + URLEncoder.encode("Free", "UTF-8");
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpConn.setRequestProperty("Content-Length", "" + Integer.toString(postData.getBytes().length));
            httpConn.setRequestProperty("Content-Language", "en-US");
            httpConn.setDoOutput(true);
            httpConn.setDoInput(true);
            outs = new DataOutputStream(httpConn.getOutputStream());
            outs.writeBytes(postData);
            outs.flush();
            inr = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            Matcher matcher = null;
            while ((line = inr.readLine()) != null) {
                matcher = mirrorLinePattern.matcher(line);
                if (matcher.find()) {
                    matcher = mirrorUrlPattern.matcher(line);
                    if (matcher.find()) {
                        urlLine = matcher.group().substring(2, matcher.group().length() - 2);
                        result[0] = new URL(urlLine);
                    }
                }
                matcher = counterPattern.matcher(line);
                if (matcher.find()) {
                    matcher = counterIntPattern.matcher(line);
                    if (matcher.find()) {
                        counter = new Integer(matcher.group());
                        result[1] = counter;
                    }
                }
            }
        } catch (IOException ex) {
            log("I/O Exception!");
        } finally {
            try {
                if (outs != null) outs.close();
                if (inr != null) inr.close();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Can not close some connections:\n" + ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
            }
            if (httpConn != null) httpConn.disconnect();
            link.setStatus(RSLink.STATUS_NOTHING);
            return result;
        }
    }

    private void retrieveFile(URL url, RSLink link) {
        link.setStatus(RSLink.STATUS_DOWNLOADING);
        HttpURLConnection httpConn = null;
        DataOutputStream outs = null;
        BufferedInputStream bins = null;
        BufferedOutputStream bouts = null;
        try {
            String postData = URLEncoder.encode("mirror", "UTF-8") + "=" + URLEncoder.encode(url.toString(), "UTF-8");
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpConn.setRequestProperty("Content-Length", "" + Integer.toString(postData.getBytes().length));
            httpConn.setRequestProperty("Content-Language", "en-US");
            httpConn.setDoOutput(true);
            httpConn.setDoInput(true);
            outs = new DataOutputStream(httpConn.getOutputStream());
            outs.writeBytes(postData);
            outs.flush();
            Pattern mimePattern = Pattern.compile(".+/html.+");
            Matcher matcher = mimePattern.matcher(httpConn.getContentType());
            if (matcher.find()) {
                log("Can not download, maybe all RS slots are busy!");
                return;
            }
            link.setSize(httpConn.getContentLength());
            bins = new BufferedInputStream(httpConn.getInputStream(), 4096);
            bouts = new BufferedOutputStream(new FileOutputStream(link.getFile()), 4096);
            link.setStatus(RSLink.STATUS_DOWNLOADING);
            link.setDown(0);
            byte[] byteBuffer = new byte[4096];
            int count;
            while ((count = bins.read(byteBuffer)) != -1) {
                bouts.write(byteBuffer, 0, count);
                link.setDown(link.getDown() + count);
                if (!Main.start.get()) {
                    link.setStatus(RSLink.STATUS_NOTHING);
                    return;
                }
            }
            link.setStatus(RSLink.STATUS_DONE);
        } catch (IOException ex) {
            log("I/O Exception!");
            link.setStatus(RSLink.STATUS_NOTHING);
        } finally {
            try {
                if (outs != null) outs.close();
                if (bouts != null) bouts.close();
                if (bins != null) bins.close();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Can not close some connections:\n" + ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
            }
            if (httpConn != null) httpConn.disconnect();
        }
    }

    private void waitForDownload(int delay, RSLink link) {
        link.setStatus(RSLink.STATUS_WAITING);
        try {
            link.setWait(delay);
            while (link.getWait() > 0) {
                Thread.sleep(1000);
                link.decWait(1);
                if (!Main.start.get()) {
                    link.setStatus(RSLink.STATUS_NOTHING);
                    return;
                }
            }
        } catch (InterruptedException ex) {
        } finally {
            link.setStatus(RSLink.STATUS_NOTHING);
        }
    }
}
