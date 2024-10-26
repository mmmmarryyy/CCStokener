package de.nomule.common;

import java.net.*;
import java.io.*;
import javax.swing.JLabel;
import de.nomule.applogic.NoMuleRuntime;
import de.nomule.applogic.Settings;

public class HTTP {

    public static String[] cookies = new String[1];

    public static String post(String strUrl, String strPostString) {
        NoMuleRuntime.showDebug("POST : " + strUrl + "(" + strPostString + ")");
        try {
            URL url = new URL(strUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(true);
            conn.setAllowUserInteraction(true);
            HttpURLConnection.setFollowRedirects(true);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            out.writeBytes(strPostString);
            out.flush();
            out.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String s = "";
            StringBuffer sRet = new StringBuffer();
            while ((s = in.readLine()) != null) {
                sRet.append(s);
            }
            in.close();
            return sRet.toString();
        } catch (MalformedURLException e) {
            NoMuleRuntime.showError("Internal Error. Malformed URL.");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Internal I/O Error.");
            e.printStackTrace();
        }
        return "";
    }

    public static boolean urlExists(String strUrl) {
        NoMuleRuntime.showDebug("UrlExists : " + strUrl);
        try {
            URL url = new URL(strUrl);
            URLConnection conn = url.openConnection();
            InputStream in = conn.getInputStream();
            in.close();
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
        }
        return false;
    }

    public static void download(String strUrl, String strFileDest, JLabel lStatus) {
        NoMuleRuntime.showDebug("Download : " + strUrl + "(" + strFileDest + ")");
        try {
            URL url = new URL(strUrl);
            OutputStream out = new BufferedOutputStream(new FileOutputStream(strFileDest));
            URLConnection conn = url.openConnection();
            NoMuleRuntime.showDebug("[" + strUrl + "]");
            InputStream in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            long lastNumWritten = 0;
            while (((numRead = in.read(buffer)) != -1) && (!Thread.currentThread().isInterrupted())) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
                if ((lastNumWritten + (100 * 1024)) < numWritten) {
                    lastNumWritten = numWritten;
                    if (lStatus != null) {
                        lStatus.setText((numWritten / 1024) + "kb");
                        lStatus.repaint();
                    } else {
                        System.out.println((numWritten / 1024) + "kb");
                    }
                }
            }
            in.close();
            out.close();
        } catch (MalformedURLException e) {
            NoMuleRuntime.showError("Internal Error. Malformed URL.");
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            NoMuleRuntime.showError("Internal Error. Could not find file.");
            e.printStackTrace();
        } catch (IOException e) {
            NoMuleRuntime.showError("Internal I/O Error.");
            e.printStackTrace();
        }
    }

    public static String get(String strUrl) {
        if (NoMuleRuntime.DEBUG) System.out.println("GET : " + strUrl);
        try {
            URL url = new URL(strUrl);
            URLConnection conn = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String s = "";
            String sRet = "";
            while ((s = in.readLine()) != null) {
                sRet += s;
            }
            NoMuleRuntime.showDebug("ANSWER: " + sRet);
            return sRet;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
