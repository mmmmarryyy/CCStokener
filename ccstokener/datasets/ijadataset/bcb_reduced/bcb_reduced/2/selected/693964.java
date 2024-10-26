package bw.soap;

import java.io.*;
import java.net.*;

public class SOAPClient {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage:  java SOAPClient4XG " + "http://soapURL soapEnvelopefile.xml" + " [SOAPAction]");
            System.err.println("SOAPAction is optional.");
            System.exit(1);
        }
        String SOAPUrl = args[0];
        String xmlFile2Send = args[1];
        String SOAPAction = "";
        if (args.length > 2) SOAPAction = args[2];
        URL url = new URL(SOAPUrl);
        URLConnection connection = url.openConnection();
        HttpURLConnection httpConn = (HttpURLConnection) connection;
        FileInputStream fin = new FileInputStream(xmlFile2Send);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        copy(fin, bout);
        fin.close();
        byte[] b = bout.toByteArray();
        httpConn.setRequestProperty("Content-Length", String.valueOf(b.length));
        httpConn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        httpConn.setRequestProperty("SOAPAction", SOAPAction);
        httpConn.setRequestMethod("POST");
        httpConn.setDoOutput(true);
        httpConn.setDoInput(true);
        OutputStream out = httpConn.getOutputStream();
        out.write(b);
        out.close();
        InputStreamReader isr = new InputStreamReader(httpConn.getInputStream());
        BufferedReader in = new BufferedReader(isr);
        String inputLine;
        while ((inputLine = in.readLine()) != null) System.out.println(inputLine);
        in.close();
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        synchronized (in) {
            synchronized (out) {
                byte[] buffer = new byte[256];
                while (true) {
                    int bytesRead = in.read(buffer);
                    if (bytesRead == -1) break;
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
    }
}
