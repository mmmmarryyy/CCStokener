package us.k5n.k5ncal.data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

/**
 * Define a simple class for handling HTTP downloads. We may move to the more
 * robust Apache HttpClient class down the road. For now, this much leaner class
 * will serve our purposes. (The HttpClient jar is about 300k plus we would need
 * to also add jars for JUnit and Apache logging).
 * 
 * @author Craig Knudsen, craig@k5n.us
 * @version $Id: HttpClient.java,v 1.6 2008/01/16 13:42:31 cknudsen Exp $
 * 
 */
public class HttpClient {

    public static HttpClientStatus getRemoteCalendar(URL url, final String username, final String password, File outputFile) {
        if (username != null && password != null) {
            Authenticator.setDefault(new Authenticator() {

                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password.toCharArray());
                }
            });
        } else {
            Authenticator.setDefault(new Authenticator() {

                protected PasswordAuthentication getPasswordAuthentication() {
                    return null;
                }
            });
        }
        HttpURLConnection urlC = null;
        int totalRead = 0;
        try {
            urlC = (HttpURLConnection) url.openConnection();
            InputStream is = urlC.getInputStream();
            OutputStream os = new FileOutputStream(outputFile);
            DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
            byte[] buf = new byte[4 * 1024];
            int bytesRead;
            while ((bytesRead = dis.read(buf)) != -1) {
                os.write(buf, 0, bytesRead);
                totalRead += bytesRead;
            }
            os.close();
            dis.close();
            urlC.disconnect();
            if (urlC.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                return new HttpClientStatus(HttpClientStatus.HTTP_STATUS_NOT_FOUND, "File not found on server");
            } else if (urlC.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                return new HttpClientStatus(HttpClientStatus.HTTP_STATUS_AUTH_REQUIRED, "Authorizaton required");
            } else if (urlC.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return new HttpClientStatus(HttpClientStatus.HTTP_STATUS_OTHER_ERROR, "HTTP Error" + " " + urlC.getResponseCode() + ": " + urlC.getResponseMessage());
            }
        } catch (IOException e1) {
            try {
                if (urlC.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                    return new HttpClientStatus(HttpClientStatus.HTTP_STATUS_NOT_FOUND, "File not found on server");
                } else if (urlC.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    return new HttpClientStatus(HttpClientStatus.HTTP_STATUS_AUTH_REQUIRED, "Authorizaton required");
                } else if (urlC.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return new HttpClientStatus(HttpClientStatus.HTTP_STATUS_OTHER_ERROR, "HTTP Error" + " " + +urlC.getResponseCode() + ": " + urlC.getResponseMessage());
                } else {
                    return new HttpClientStatus(HttpClientStatus.HTTP_STATUS_OTHER_ERROR, "HTTP I/O Exception" + ":", e1);
                }
            } catch (IOException e2) {
                e2.printStackTrace();
                return new HttpClientStatus(HttpClientStatus.HTTP_STATUS_OTHER_ERROR, "HTTP I/O Error" + ": " + e1.getMessage(), e1);
            }
        }
        return new HttpClientStatus(HttpClientStatus.HTTP_STATUS_SUCCESS, outputFile);
    }

    public static HttpClientStatus putRemoteCalendar(URL url, final String username, final String password, File inputFile) {
        if (!inputFile.exists() || inputFile.length() <= 0) {
            return new HttpClientStatus(HttpClientStatus.HTTP_STATUS_NOT_FOUND, "No such file" + ": " + inputFile);
        }
        if (username != null && password != null) {
            Authenticator.setDefault(new Authenticator() {

                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password.toCharArray());
                }
            });
        } else {
            Authenticator.setDefault(new Authenticator() {

                protected PasswordAuthentication getPasswordAuthentication() {
                    return null;
                }
            });
        }
        HttpURLConnection urlC = null;
        int totalRead = 0;
        try {
            urlC = (HttpURLConnection) url.openConnection();
            urlC.setDoInput(true);
            urlC.setDoOutput(true);
            urlC.setUseCaches(false);
            urlC.setDefaultUseCaches(false);
            urlC.setAllowUserInteraction(true);
            urlC.setRequestMethod("PUT");
            urlC.setRequestProperty("Content-type", "text/calendar");
            urlC.setRequestProperty("Content-Length", "" + inputFile.length());
            OutputStream os = urlC.getOutputStream();
            System.out.println("Put file: " + inputFile);
            FileInputStream fis = new FileInputStream(inputFile);
            DataInputStream dis = new DataInputStream(new BufferedInputStream(fis));
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));
            byte[] buf = new byte[4 * 1024];
            int bytesRead;
            while ((bytesRead = dis.read(buf)) != -1) {
                dos.write(buf, 0, bytesRead);
                totalRead += bytesRead;
            }
            dos.flush();
            int code = urlC.getResponseCode();
            System.out.println("PUT response code: " + code);
            if (code < 200 || code >= 300) {
                os.close();
                return new HttpClientStatus(HttpClientStatus.HTTP_STATUS_OTHER_ERROR, "Server does not accept PUT.  Response Code=" + code);
            }
            InputStream is = urlC.getInputStream();
            DataInputStream respIs = new DataInputStream(new BufferedInputStream(is));
            buf = new byte[4 * 1024];
            StringBuffer response = new StringBuffer();
            while ((bytesRead = respIs.read(buf)) != -1) {
                response.append(new String(buf));
                totalRead += bytesRead;
            }
            System.out.println("Response: " + response.toString());
            respIs.close();
            os.close();
            dos.close();
            dis.close();
            urlC.disconnect();
            if (urlC.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                return new HttpClientStatus(HttpClientStatus.HTTP_STATUS_NOT_FOUND, "File not found on server");
            } else if (urlC.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                return new HttpClientStatus(HttpClientStatus.HTTP_STATUS_AUTH_REQUIRED, "Authorizaton required");
            } else if (urlC.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return new HttpClientStatus(HttpClientStatus.HTTP_STATUS_OTHER_ERROR, "HTTP Error" + ": " + urlC.getResponseCode() + ": " + urlC.getResponseMessage());
            }
        } catch (IOException e1) {
            try {
                if (urlC.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                    return new HttpClientStatus(HttpClientStatus.HTTP_STATUS_NOT_FOUND, "File not found on server");
                } else if (urlC.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    return new HttpClientStatus(HttpClientStatus.HTTP_STATUS_AUTH_REQUIRED, "Authorizaton required");
                } else if (urlC.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return new HttpClientStatus(HttpClientStatus.HTTP_STATUS_OTHER_ERROR, "HTTP Error" + " " + urlC.getResponseCode() + ": " + urlC.getResponseMessage());
                } else {
                    return new HttpClientStatus(HttpClientStatus.HTTP_STATUS_OTHER_ERROR, "HTTP I/O Exception" + ":", e1);
                }
            } catch (IOException e2) {
                e2.printStackTrace();
                return new HttpClientStatus(HttpClientStatus.HTTP_STATUS_OTHER_ERROR, "HTTP I/O Exception" + ":", e1);
            }
        }
        return new HttpClientStatus(HttpClientStatus.HTTP_STATUS_SUCCESS, "File successfully uploaded");
    }
}
