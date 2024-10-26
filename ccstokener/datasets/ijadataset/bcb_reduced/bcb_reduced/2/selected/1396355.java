package com.tilab.wsig.soap;

import java.io.*;
import java.net.*;

public class SoapClient {

    public static String sendFileMessage(String SOAPUrl, String fileName) {
        ByteArrayOutputStream bout = null;
        try {
            FileInputStream fin = new FileInputStream(fileName);
            bout = new ByteArrayOutputStream();
            copy(fin, bout);
            fin.close();
        } catch (Exception e) {
            String resp = "Error reading file";
            System.out.println(resp);
            e.printStackTrace();
            return resp;
        }
        byte[] byteMessage = bout.toByteArray();
        System.out.println("SOAP request:");
        System.out.println(new String(byteMessage));
        return sendMessage(SOAPUrl, byteMessage);
    }

    public static String sendStringMessage(String SOAPUrl, String SOAPmessage) {
        byte[] byteMessage = SOAPmessage.getBytes();
        return sendMessage(SOAPUrl, byteMessage);
    }

    private static String sendMessage(String SOAPUrl, byte[] byteMessage) {
        String resp = null;
        boolean requestSent = false;
        HttpURLConnection httpConn = null;
        try {
            URL url = new URL(SOAPUrl);
            URLConnection connection = url.openConnection();
            httpConn = (HttpURLConnection) connection;
            httpConn.setRequestProperty("Content-Length", String.valueOf(byteMessage.length));
            httpConn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            httpConn.setRequestProperty("SOAPAction", "");
            httpConn.setRequestMethod("POST");
            httpConn.setDoOutput(true);
            httpConn.setDoInput(true);
            OutputStream out = httpConn.getOutputStream();
            out.write(byteMessage);
            out.close();
            requestSent = true;
            if (httpConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStreamReader isr = new InputStreamReader(httpConn.getInputStream());
                BufferedReader in = new BufferedReader(isr);
                String inputLine;
                StringBuffer sb = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    sb.append(inputLine);
                }
                in.close();
                resp = sb.toString();
            } else {
                resp = httpConn.getResponseMessage();
            }
        } catch (Exception e) {
            resp = (requestSent ? "Error response received" : "Error sending soap message") + " - " + e.getMessage();
            System.out.println(resp);
            e.printStackTrace();
        } finally {
            try {
                httpConn.disconnect();
            } catch (Exception e) {
            }
        }
        return resp;
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
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

    public static void main(String[] args) throws Exception {
        System.out.println("SOAP Client to inwoke a webservice");
        System.out.println("");
        if (args == null || args.length < 2) {
            System.out.println("usage: SoapClient URL FILE");
            System.out.println("where:");
            System.out.println("URL: soap web-server url");
            System.out.println("FILE: xml file with soap message request");
            return;
        }
        String SOAPUrl = args[0];
        String SOAPFile = args[1];
        System.out.println("Web-service: " + SOAPUrl);
        String resp = SoapClient.sendFileMessage(SOAPUrl, SOAPFile);
        System.out.println("");
        System.out.println("SAOP response:");
        System.out.println(resp);
    }
}
