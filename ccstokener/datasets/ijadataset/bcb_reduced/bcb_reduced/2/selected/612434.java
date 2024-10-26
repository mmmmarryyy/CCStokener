package org.hfbk.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hfbk.vis.Prefs;

/**
 * HTTP protocol related utils. 
 */
public class HTTPUtils {

    /**
	 * connect to an URL, opening the web page.
	 * 
	 * @return a BufferedReader to read the page from 
	 */
    public static BufferedReader connect(URL url, boolean silent) throws IOException {
        if (!silent) System.out.print("Connecting " + url);
        URLConnection con = url.openConnection();
        con.setConnectTimeout(Prefs.current.timeout);
        con.setRequestProperty("Accept-Language", Prefs.current.language);
        con.setReadTimeout(Prefs.current.timeout);
        BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream(), Charset.forName("utf8")));
        if (!silent) System.out.print(", reading ");
        return r;
    }

    /**
	 * connect to an URL predicting an older mozilla version
	 * sad we need this. some pages doesn't like non-browser services. 
	 */
    public static BufferedReader connectAsMozilla(URL url, boolean silent) throws IOException {
        if (!silent) System.out.print("Connecting " + url);
        URLConnection con = url.openConnection();
        con.setConnectTimeout(Prefs.current.timeout);
        con.setReadTimeout(Prefs.current.timeout);
        con.setRequestProperty("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
        con.setRequestProperty("Accept-Language", Prefs.current.language);
        con.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; de; rv:1.8.0.5) Gecko/20060719 Firefox/1.5.0.5");
        BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream(), Charset.forName("utf8")));
        if (!silent) System.out.print(", reading ");
        return r;
    }

    static void download(URL url, File target) throws IOException {
        System.out.print("Downloading " + url + "...");
        URLConnection con = url.openConnection();
        con.setConnectTimeout(Prefs.current.timeout);
        con.setReadTimeout(Prefs.current.timeout);
        InputStream is = con.getInputStream();
        OutputStream os = new FileOutputStream(target);
        byte[] buffer = new byte[4096];
        int bytesRead = 0;
        while ((bytesRead = is.read(buffer)) > 0) os.write(buffer, 0, bytesRead);
        os.close();
        is.close();
        System.out.println("OK.");
    }

    /**
	 *  fetches whole content from an URL (predicting mozilla)
	 *  
	 *  @param silent don't log operation to stdout
	 */
    public static String fetch(String url, boolean silent) {
        try {
            BufferedReader r = HTTPUtils.connectAsMozilla(new URL(url), silent);
            String line;
            StringBuilder s = new StringBuilder(1024);
            while ((line = r.readLine()) != null) s.append(line);
            r.close();
            if (!silent) System.out.println(s.length() + " chars finished.");
            return s.toString();
        } catch (Exception e) {
            if (!silent) throw new RuntimeException(e);
            return "";
        }
    }

    /**
	 * fetches content from an URL until the given pattern matches to the 
	 * last fetched line. 
	 * 
	 * @param url 
	 * @param pattern Pattern to stop fetch. 
	 * @param silent don't log operation to stdout
	 * @return content till pattern matches, inclusive.
	 */
    public static String fetchUntil(String url, Pattern pattern, boolean silent) {
        try {
            BufferedReader r = HTTPUtils.connectAsMozilla(new URL(url), silent);
            StringBuilder s = new StringBuilder(1024);
            Matcher endMatcher = pattern.matcher("");
            String line;
            while ((line = r.readLine()) != null) {
                s.append(line);
                endMatcher.reset(line);
                if (endMatcher.find()) break;
            }
            r.close();
            if (!silent) System.out.println(s.length() + " chars finished.");
            return s.toString();
        } catch (Exception e) {
            if (!silent) throw new RuntimeException(e);
            return "";
        }
    }

    /**
	 * convenient replacement for URLDecoder.decode
	 */
    public static String decode(String encoded) {
        try {
            return URLDecoder.decode(encoded, "utf8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * convenient replacement for URLEncoder.encode
	 */
    public static String encode(String encode) {
        try {
            return URLEncoder.encode(encode, "utf8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void SubmitPicture(String exsistingFileName, String ScriptSource) {
        if (Prefs.current.verbose) System.out.println("uploading :" + exsistingFileName);
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        DataInputStream inStream = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        String urlString = Prefs.current.baseURL + "upload.php";
        try {
            File file = new File(exsistingFileName);
            FileInputStream fileInputStream = new FileInputStream(file);
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            dos = new DataOutputStream(conn.getOutputStream());
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"bild\";" + " filename=\"" + exsistingFileName + "\"" + lineEnd);
            dos.writeBytes(lineEnd);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            while (bytesRead > 0) {
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            fileInputStream.close();
            dos.flush();
            dos.close();
        } catch (MalformedURLException ex) {
            System.out.println("Fehler beim Verbindungsaufbau mit Script-Dateien.");
        } catch (IOException ioe) {
            System.out.println("Fehler beim Laden des Bildes.");
        }
        try {
            inStream = new DataInputStream(conn.getInputStream());
            String str;
            String output = "";
            while ((str = inStream.readUTF()) != null) {
                output = output + str;
            }
            if (output != "") {
            }
            inStream.close();
            new File(exsistingFileName).delete();
        } catch (IOException ioex) {
            System.out.println("Fehler beim Empfangen der Serverantwort.");
        }
    }
}
