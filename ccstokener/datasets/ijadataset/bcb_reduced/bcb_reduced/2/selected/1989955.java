package jomm.utils;

import sun.misc.BASE64Encoder;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: Nelson Graca
 * Date: Nov 15, 2010
 * Time: 4:28:21 PM
 */
public class HttpUtils {

    private static String Boundary = "---------------------------7d021a37605f0";

    public static String upload(URL url, Map<String, File> files) throws Exception {
        return upload(url, files, "", "");
    }

    public static String upload(URL url, Map<String, File> files, long timeout, long inactiveTimeout) throws Exception {
        return upload(url, files, "", "", timeout, inactiveTimeout);
    }

    public static String upload(URL url, Map<String, File> files, String username, String password) throws Exception {
        return upload(url, files, username, password, 360000, 360000);
    }

    static class UploadThread implements Runnable {

        boolean finish = false;

        long lastIteraction = System.currentTimeMillis();

        URL url;

        Map<String, File> files;

        String username;

        String password;

        String result = null;

        Exception e = null;

        public boolean isFinish() {
            return finish;
        }

        public long getLastIteraction() {
            return lastIteraction;
        }

        UploadThread(URL url, Map<String, File> files, String username, String password) {
            this.password = password;
            this.username = username;
            this.files = files;
            this.url = url;
        }

        public String getResult() {
            return result;
        }

        public Exception getE() {
            return e;
        }

        public void run() {
            try {
                HttpURLConnection theUrlConnection = (HttpURLConnection) url.openConnection();
                theUrlConnection.setConnectTimeout(1000000);
                theUrlConnection.setReadTimeout(1000000);
                theUrlConnection.setDoOutput(true);
                theUrlConnection.setDoInput(true);
                theUrlConnection.setUseCaches(false);
                theUrlConnection.setChunkedStreamingMode(1024);
                byte[] encodedPassword = (username + ":" + password).getBytes();
                BASE64Encoder encoder = new BASE64Encoder();
                theUrlConnection.setRequestProperty("Authorization", "Basic " + encoder.encode(encodedPassword));
                theUrlConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + Boundary);
                DataOutputStream httpOut = new DataOutputStream(theUrlConnection.getOutputStream());
                for (Map.Entry<String, File> entry : files.entrySet()) {
                    File f = entry.getValue();
                    String str = "--" + Boundary + "\r\n" + "Content-Disposition: form-data;name=\"" + entry.getKey() + "\"; filename=\"" + f.getName() + "\"\r\n" + "Content-Type:" + MimeTypeGuesser.getInstance().guessMimeType(f) + "\r\n" + "\r\n";
                    httpOut.write(str.getBytes());
                    FileInputStream uploadFileReader = new FileInputStream(f);
                    int numBytesToRead = 1024;
                    int availableBytesToRead;
                    while ((availableBytesToRead = uploadFileReader.available()) > 0) {
                        byte[] bufferBytesRead;
                        bufferBytesRead = availableBytesToRead >= numBytesToRead ? new byte[numBytesToRead] : new byte[availableBytesToRead];
                        uploadFileReader.read(bufferBytesRead);
                        lastIteraction = System.currentTimeMillis();
                        httpOut.write(bufferBytesRead);
                        httpOut.flush();
                    }
                    uploadFileReader.close();
                }
                httpOut.write(("\r\n--" + Boundary + "--\r\n").getBytes());
                httpOut.flush();
                httpOut.close();
                InputStream is = theUrlConnection.getInputStream();
                StringBuilder response = new StringBuilder();
                byte[] respBuffer = new byte[4096];
                while (is.read(respBuffer) >= 0) {
                    response.append(new String(respBuffer).trim());
                }
                is.close();
                theUrlConnection.disconnect();
                result = response.toString();
                finish = true;
            } catch (Exception e) {
                this.e = e;
            }
        }
    }

    public static String upload(URL url, Map<String, File> files, String username, String password, long timeout, long inactiveTimeout) throws Exception {
        UploadThread uploadThread = new UploadThread(url, files, username, password);
        Thread t = new Thread(uploadThread);
        t.start();
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            t.join(1000);
            Thread.sleep(500);
            if (uploadThread.isFinish() || !t.isAlive() || t.isInterrupted()) {
                System.out.println("Finish");
                break;
            }
            Thread.sleep(2500);
            if (System.currentTimeMillis() - uploadThread.getLastIteraction() > inactiveTimeout) {
                System.out.println("Inactive for more than " + inactiveTimeout);
                break;
            }
        }
        if (t.isAlive() && !t.isInterrupted()) {
            System.out.println("Thread is alive will kill it");
            try {
                t.interrupt();
            } catch (Throwable tt) {
                System.out.println(tt.toString());
                tt.printStackTrace();
            }
        }
        if (System.currentTimeMillis() - start >= timeout) {
            System.out.println("General Timeout of " + timeout + " passed");
        }
        if (uploadThread.getE() != null) throw uploadThread.getE();
        if (uploadThread.getResult() == null) {
            System.out.println("Thread was not finnished yet will throw a TimeoutException");
            throw new IOException("Timeout passed");
        }
        return uploadThread.getResult();
    }

    public static void download(URL url, File dest) throws IOException {
        download(url, dest, "", "");
    }

    public static void download(URL url, File dest, String username, String password) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setDoInput(true);
        byte[] encodedPassword = (username + ":" + password).getBytes();
        BASE64Encoder encoder = new BASE64Encoder();
        con.setRequestProperty("Authorization", "Basic " + encoder.encode(encodedPassword));
        con.setConnectTimeout(2000);
        BufferedInputStream in = new BufferedInputStream(con.getInputStream());
        FileOutputStream fos = new FileOutputStream(dest);
        BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
        byte[] data = new byte[1024];
        int x = 0;
        while ((x = in.read(data, 0, 1024)) >= 0) {
            bout.write(data, 0, x);
        }
        bout.close();
        in.close();
        con.disconnect();
    }

    public static String generateUrlParameters(Map<String, String> map) {
        String params = "";
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getValue() != null && entry.getKey() != null) {
                if (first) first = false; else params += "&";
                params += entry.getKey() + "=" + entry.getValue().replaceAll(" ", "%20");
            }
        }
        return params;
    }

    public static String urlEncodeParameters(String[][] parameters) throws Throwable {
        return urlEncodeParameters(parameters, "UTF-8");
    }

    public static String urlEncodeParameters(String[][] parameters, String encoding) throws Throwable {
        String ret = "";
        boolean first = true;
        for (String[] parameter : parameters) {
            if (first) {
                first = false;
                ret += parameter[0] + "=" + URLEncoder.encode(parameter[1], encoding);
            } else {
                ret += "&" + parameter[0] + "=" + URLEncoder.encode(parameter[1], encoding);
            }
        }
        return ret;
    }

    public static String urlEncodeParameters(Map<Object, Object> parameters, String encoding) throws Throwable {
        String ret = "";
        boolean first = true;
        for (Map.Entry<Object, Object> entry : parameters.entrySet()) {
            if (first) {
                first = false;
                ret += entry.getKey() + "=" + URLEncoder.encode(String.valueOf(entry.getValue()), encoding);
            } else {
                ret += "&" + entry.getKey() + "=" + URLEncoder.encode(String.valueOf(entry.getValue()), encoding);
            }
        }
        return ret;
    }

    public static String doPostWithBasicAuthentication(URL url, String username, String password, String[][] parameters) throws Throwable {
        return doPostWithBasicAuthentication(url, username, password, parameters, "UTF-8");
    }

    public static String doPostWithBasicAuthentication(URL url, String username, String password, String[][] parameters, String parameterEncoding) throws Throwable {
        return doPostWithBasicAuthentication(url, username, password, urlEncodeParameters(parameters, parameterEncoding));
    }

    private static String doPostWithBasicAuthentication(URL url, String username, String password, String parameters) throws IOException {
        return doPostWithBasicAuthentication(url, username, password, parameters, null);
    }

    public static String doPostWithBasicAuthentication(URL url, String username, String password, String parameters, Map<String, String> headers) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setDoInput(true);
        con.setDoOutput(true);
        byte[] encodedPassword = (username + ":" + password).getBytes();
        BASE64Encoder encoder = new BASE64Encoder();
        con.setRequestProperty("Authorization", "Basic " + encoder.encode(encodedPassword));
        con.setConnectTimeout(2000);
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        if (parameters != null) con.setRequestProperty("Content-Length", "" + Integer.toString(parameters.getBytes().length));
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                con.setRequestProperty(header.getKey(), header.getValue());
            }
        }
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(parameters);
        wr.flush();
        wr.close();
        InputStream is = con.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuffer response = new StringBuffer();
        while ((line = rd.readLine()) != null) {
            response.append(line);
            response.append('\n');
        }
        rd.close();
        is.close();
        con.disconnect();
        return response.toString();
    }

    static class DownloadThread implements Runnable {

        boolean finish = false;

        long lastIteraction = System.currentTimeMillis();

        String result = null;

        Throwable e = null;

        String username;

        String password;

        URL url;

        public boolean isFinish() {
            return finish;
        }

        DownloadThread(URL url, String username, String password) {
            this.username = username;
            this.password = password;
            this.url = url;
        }

        public String getResult() {
            return result;
        }

        public Throwable getE() {
            return e;
        }

        public long getLastIteraction() {
            return lastIteraction;
        }

        public void run() {
            try {
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setDoInput(true);
                byte[] encodedPassword = (username + ":" + password).getBytes();
                BASE64Encoder encoder = new BASE64Encoder();
                con.setRequestProperty("Authorization", "Basic " + encoder.encode(encodedPassword));
                InputStream is = con.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line;
                StringBuffer response = new StringBuffer();
                while ((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\n');
                    lastIteraction = System.currentTimeMillis();
                }
                rd.close();
                is.close();
                con.disconnect();
                result = response.toString();
                finish = true;
            } catch (Throwable e) {
                this.e = e;
            }
        }
    }

    public static String doGetWithBasicAuthentication(URL url, String username, String password, long timeout, long inactiveTimeout) throws Throwable {
        DownloadThread downloadThread = new DownloadThread(url, username, password);
        Thread t = new Thread(downloadThread);
        t.start();
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            t.join(1000);
            Thread.sleep(500);
            if (downloadThread.isFinish() || !t.isAlive() || t.isInterrupted()) {
                System.out.println("Finish");
                break;
            }
            Thread.sleep(2500);
            if (System.currentTimeMillis() - downloadThread.getLastIteraction() > inactiveTimeout) {
                System.out.println("Inactive Timeout for more than " + inactiveTimeout);
                break;
            }
        }
        if (t.isAlive() && !t.isInterrupted()) {
            System.out.println("Thread is alive will kill it");
            try {
                t.interrupt();
            } catch (Throwable tt) {
                System.out.println(tt.toString());
                tt.printStackTrace();
            }
        }
        if (System.currentTimeMillis() - start >= timeout) {
            System.out.println("General Timeout of " + timeout + " passed");
        }
        if (downloadThread.getE() != null) throw downloadThread.getE();
        if (downloadThread.getResult() == null) {
            System.out.println("Thread was not finished yet will throw a TimeoutException");
            throw new IOException("Timeout passed");
        }
        return downloadThread.getResult();
    }

    public static boolean download(URL url, File dest, String username, String password, long timeout, long inactiveTimeout) throws Exception {
        DownloadFileThread downloadThread = new DownloadFileThread(url, username, password, dest);
        Thread t = new Thread(downloadThread);
        t.start();
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            t.join(1000);
            Thread.sleep(500);
            if (downloadThread.isFinish() || !t.isAlive() || t.isInterrupted()) {
                System.out.println("Finish");
                break;
            }
            Thread.sleep(2500);
            if (System.currentTimeMillis() - downloadThread.getLastIteraction() > inactiveTimeout) {
                System.out.println("Inactive Timeout for more than " + inactiveTimeout);
                break;
            }
        }
        if (t.isAlive() && !t.isInterrupted()) {
            System.out.println("Thread is alive will kill it");
            try {
                t.interrupt();
            } catch (Throwable tt) {
                System.out.println(tt.toString());
                tt.printStackTrace();
            }
        }
        if (System.currentTimeMillis() - start >= timeout) {
            System.out.println("General Timeout of " + timeout + " passed");
        }
        if (downloadThread.getE() != null) throw downloadThread.getE();
        if (!downloadThread.isFinish()) {
            System.out.println("Thread was not finished yet will throw a TimeoutException");
            throw new IOException("Timeout passed");
        }
        return true;
    }

    static int i = 0;

    static class DownloadFileThread implements Runnable {

        boolean finish = false;

        long lastIteraction = System.currentTimeMillis();

        Exception e = null;

        File toFile;

        String username;

        String password;

        URL url;

        public boolean isFinish() {
            return finish;
        }

        DownloadFileThread(URL url, String username, String password, File toFile) {
            this.username = username;
            this.password = password;
            this.url = url;
            this.toFile = toFile;
        }

        public File getToFile() {
            return toFile;
        }

        public Exception getE() {
            return e;
        }

        public long getLastIteraction() {
            return lastIteraction;
        }

        public void run() {
            try {
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setDoInput(true);
                byte[] encodedPassword = (username + ":" + password).getBytes();
                BASE64Encoder encoder = new BASE64Encoder();
                con.setRequestProperty("Authorization", "Basic " + encoder.encode(encodedPassword));
                BufferedInputStream in = new BufferedInputStream(con.getInputStream());
                FileOutputStream fos = new FileOutputStream(toFile);
                BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
                byte[] data = new byte[1024];
                int x = 0;
                while ((x = in.read(data, 0, 1024)) >= 0) {
                    bout.write(data, 0, x);
                    lastIteraction = System.currentTimeMillis();
                }
                bout.flush();
                bout.close();
                fos.flush();
                fos.close();
                in.close();
                con.disconnect();
                finish = true;
            } catch (Exception e) {
                this.e = e;
            }
        }
    }

    public static String doGetWithBasicAuthentication(URL url, String username, String password, int timeout) throws Throwable {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setDoInput(true);
        byte[] encodedPassword = (username + ":" + password).getBytes();
        BASE64Encoder encoder = new BASE64Encoder();
        con.setRequestProperty("Authorization", "Basic " + encoder.encode(encodedPassword));
        con.setConnectTimeout(timeout);
        InputStream is = con.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuffer response = new StringBuffer();
        while ((line = rd.readLine()) != null) {
            response.append(line);
            response.append('\n');
        }
        rd.close();
        is.close();
        con.disconnect();
        return response.toString();
    }

    public static String doGetWithBasicAuthentication(URL url, String username, String password, int timeout, Map<String, String> headers) throws Throwable {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setDoInput(true);
        if (username != null || password != null) {
            byte[] encodedPassword = (username + ":" + password).getBytes();
            BASE64Encoder encoder = new BASE64Encoder();
            con.setRequestProperty("Authorization", "Basic " + encoder.encode(encodedPassword));
        }
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                con.setRequestProperty(header.getKey(), header.getValue());
            }
        }
        con.setConnectTimeout(timeout);
        InputStream is = con.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuffer response = new StringBuffer();
        while ((line = rd.readLine()) != null) {
            response.append(line);
            response.append('\n');
        }
        rd.close();
        is.close();
        con.disconnect();
        return response.toString();
    }

    public static String doGetWithBasicAuthentication(URL url, String username, String password) throws Throwable {
        return doGetWithBasicAuthentication(url, username, password, 2000);
    }

    /**
     * Tries to return the address/ip from a given url
     *
     * @param url
     * @return the guessed ip
     */
    public static String guessAddressFromUrl(String url) {
        if (!url.endsWith("/")) url += "/";
        Matcher matcher = Pattern.compile("(?<=[a-zA-z]+://).*?(?=(:[0-9]+|/))").matcher(url);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    /**
     * Tries to swap the address in a given url
     *
     * @param url
     * @param address
     */
    public static String swapAddressInUrl(String url, String address) {
        boolean added = false;
        if (!url.endsWith("/")) {
            url += "/";
            added = true;
        }
        String u = url.replaceFirst("(?<=[a-zA-z]+://).*?(?=(:[0-9]+|/))", address);
        if (added) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }

    public static void main(String[] args) {
        System.out.println(HttpUtils.swapAddressInUrl("http://localhost", "XPTO"));
    }

    public static String doPost(URL url, String[][] params) throws Throwable {
        return doPostWithBasicAuthentication(url, "", "", params);
    }

    public static String doPost(URL url, Map<Object, Object> params) throws Throwable {
        return doPostWithBasicAuthentication(url, "", "", urlEncodeParameters(params, "UTF-8"));
    }

    public static String doGet(URL url) throws Throwable {
        return doGetWithBasicAuthentication(url, "", "");
    }
}
