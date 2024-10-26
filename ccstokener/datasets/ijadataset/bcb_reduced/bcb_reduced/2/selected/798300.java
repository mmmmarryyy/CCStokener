package online;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URLEncoder;
import javax.swing.JOptionPane;
import storage.Session;
import storage.XML;

public class BuddiesBans {

    public URLConnection connect(Session s) {
        URL bbURL = null;
        URLConnection conn = null;
        XML xmlData = new XML();
        String data[] = xmlData.getLoginData(s);
        try {
            bbURL = new URL("http://www.banlist.nl/banlist_login.php?login=" + data[0] + "&pass=" + data[1]);
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(null, "Banlist.nl is not working!");
            e.printStackTrace();
        }
        try {
            conn = bbURL.openConnection();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Could not connect to Banlist.nl");
            e.printStackTrace();
        }
        return conn;
    }

    public String sessionKey(URLConnection conn) {
        String info = "";
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(conn.getURL().openStream()));
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Could not open the stream");
        }
        try {
            String tmp;
            while ((tmp = in.readLine()) != null) {
                info = info + tmp;
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "There was an error reading login data!");
        }
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "There was an error finishing the login process");
        }
        String sessionKey = "";
        if (info == "Login Error") {
            JOptionPane.showMessageDialog(null, "Your login information is incorrect");
        } else {
            sessionKey = info;
        }
        return sessionKey;
    }

    public void uploadBans(String sessionKey) {
        String bans = "TEST\tTEST REASON\t0";
        StringBuffer sb = new StringBuffer();
        URL url = null;
        String body = null;
        try {
            url = new URL("http://www.banlist.nl/banlist_upload.php");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Something's messed up...");
        }
        try {
            body = URLEncoder.encode("PHPSESSID", "UTF-8") + "=" + URLEncoder.encode(sessionKey, "UTF-8");
            body += "&" + URLEncoder.encode("bans", "UTF-8") + "=" + URLEncoder.encode(bans, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Uhh, UTF-8 isn't recognized?  Issues");
        }
        String newline = null;
        try {
            newline = System.getProperty("line.separator");
        } catch (Exception e) {
            newline = "\n";
        }
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setAllowUserInteraction(false);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-length", Integer.toString(body.length()));
            OutputStream rawOutStream = conn.getOutputStream();
            PrintWriter pw = new PrintWriter(rawOutStream);
            pw.print(body);
            pw.flush();
            pw.close();
            InputStream rawInStream = conn.getInputStream();
            BufferedReader rdr = new BufferedReader(new InputStreamReader(rawInStream));
            String line;
            while ((line = rdr.readLine()) != null) {
                sb.append(line);
                sb.append(newline);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String downloadBans(String list, String sessionKey) {
        StringBuffer sb = new StringBuffer();
        URL url = null;
        String body = null;
        if (list != "others" && list != "own") {
            JOptionPane.showMessageDialog(null, "There's some bad programming going on here, the download URL is mistyped");
        }
        try {
            url = new URL("http://www.banlist.nl/banlist_download.php?list=" + list);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Something's messed up...");
        }
        try {
            body = URLEncoder.encode("PHPSESSID", "UTF-8") + "=" + URLEncoder.encode(sessionKey, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Uhh, UTF-8 isn't recognized?  Issues");
        }
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setAllowUserInteraction(false);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-length", Integer.toString(body.length()));
            OutputStream rawOutStream = conn.getOutputStream();
            PrintWriter pw = new PrintWriter(rawOutStream);
            pw.print(body);
            pw.flush();
            pw.close();
            InputStream rawInStream = conn.getInputStream();
            BufferedReader rdr = new BufferedReader(new InputStreamReader(rawInStream));
            String line;
            while ((line = rdr.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error downloading bans!");
            e.printStackTrace();
        }
        return sb.toString();
    }
}
