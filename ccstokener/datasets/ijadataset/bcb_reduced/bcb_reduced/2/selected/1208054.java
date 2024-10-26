package http;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

public class TestRemoteSysSun {

    public TestRemoteSysSun() {
    }

    public static void main(String args[]) {
        TestRemoteSysSun testremotesyssun = new TestRemoteSysSun();
        if (args.length < 2) {
            System.out.println("Usage : java TestRemoteSysSun <systemURL> <requestFileName> <responseFileName>");
            return;
        }
        String s = args[0] == null ? "" : args[0];
        String s1 = args[1] == null ? "" : args[1];
        String s2 = args[2] == null ? "" : args[2];
        String s3 = "";
        StringBuffer stringbuffer = new StringBuffer("");
        try {
            FileReader filereader = new FileReader(s1);
            BufferedReader bufferedreader;
            for (bufferedreader = new BufferedReader(filereader); bufferedreader.ready(); stringbuffer.append('\n')) stringbuffer.append(bufferedreader.readLine());
            bufferedreader.close();
            filereader.close();
        } catch (IOException ioexception) {
            System.out.println("Error while reading " + s1 + " " + ioexception.getMessage());
            ioexception.printStackTrace();
            System.exit(1);
        }
        s3 = stringbuffer.toString();
        InputStream inputstream = null;
        try {
            inputstream = testremotesyssun.send(s3, s);
        } catch (IOException ioexception1) {
            System.out.println("Error while sending HTTP request " + ioexception1.getMessage());
            ioexception1.printStackTrace();
            System.exit(1);
        }
        try {
            StringBuffer stringbuffer1 = new StringBuffer("");
            int i;
            if (inputstream != null) while ((i = inputstream.read()) != -1) stringbuffer1.append((char) i); else System.exit(1);
            BufferedWriter bufferedwriter = new BufferedWriter(new FileWriter(s2, true));
            bufferedwriter.write(stringbuffer1.toString());
            bufferedwriter.close();
        } catch (IOException ioexception2) {
            System.out.println("Error while writing to file " + s2 + "\n" + ioexception2.getMessage());
            ioexception2.printStackTrace();
        }
    }

    public InputStream send(String s, String s1) throws IOException {
        HttpURLConnection httpurlconnection = null;
        DataInputStream datainputstream = null;
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        URL url = new URL(s1);
        httpurlconnection = (HttpURLConnection) url.openConnection();
        configureURLConnection(httpurlconnection);
        DataOutputStream dataoutputstream = new DataOutputStream(httpurlconnection.getOutputStream());
        dataoutputstream.write(s.getBytes());
        httpurlconnection.connect();
        datainputstream = new DataInputStream(httpurlconnection.getInputStream());
        if (httpurlconnection.getResponseCode() != 200) {
            System.out.println("Invalid Response Code! Code Returned = " + Integer.toString(httpurlconnection.getResponseCode()));
            return null;
        }
        if (!httpurlconnection.getContentType().equalsIgnoreCase("Text/xml")) {
            System.out.println("Invalid Content-Type! Content type of response received = " + httpurlconnection.getContentType());
            return null;
        } else {
            return datainputstream;
        }
    }

    private void configureURLConnection(HttpURLConnection httpurlconnection) throws ProtocolException {
        httpurlconnection.setDoInput(true);
        httpurlconnection.setDoOutput(true);
        HttpURLConnection _tmp = httpurlconnection;
        HttpURLConnection.setFollowRedirects(true);
        httpurlconnection.setRequestMethod("POST");
        httpurlconnection.setRequestProperty("Content-Type", "Text/xml");
    }
}
