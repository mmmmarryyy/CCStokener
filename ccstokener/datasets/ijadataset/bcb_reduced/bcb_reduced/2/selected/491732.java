package com.wwwc.util.web;

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.Security;
import java.security.Provider;
import javax.net.ssl.*;

public class MyHttpsConnection {

    private StringBuffer message;

    private StringBuffer fullStringBuffer;

    private Vector fullLineVector;

    private static Vector vInput = new Vector(10);

    private static Vector vOut = new Vector(10);

    public static void main(String[] args) {
        String url = "https://www.bigcharts.com/custom/datek-com/screener/screener.asp" + "?sh=1%2C1%2C1%2C1%2C1%2C1%2C1&pg=vop&cols=0&cols=14&sort=0&sortd=1&numresults=8815" + "&s1=1&s1p1=0.01&s1p2=500&s2p1=0&s2p3=0&exchange=0&result.x=72&result.y=5";
    }

    public StringBuffer getMessage() {
        return message;
    }

    public StringBuffer getFullReturnStringBuffer() {
        return fullStringBuffer;
    }

    public Vector getReturnLineVector() {
        fullLineVector = new Vector();
        String temp = fullStringBuffer.toString();
        temp = temp.replaceAll("\n", "");
        temp = temp.replaceAll("\t", "");
        temp = temp.replaceAll("&nbsp;", " ");
        temp = temp.replaceAll(">", ">\n");
        temp = temp.replaceAll("</", "\n</");
        String element = null;
        StringTokenizer tokens = new StringTokenizer(temp, "\n");
        while (tokens.hasMoreTokens()) {
            element = (tokens.nextToken()).trim();
            if (element != null && element.length() > 0) {
                fullLineVector.addElement(element);
            }
        }
        return fullLineVector;
    }

    public Vector getReturnBodyLineVector() {
        Vector v = getReturnLineVector();
        Enumeration enums = v.elements();
        String temp = null;
        while (enums.hasMoreElements()) {
            temp = (String) enums.nextElement();
            temp = temp.trim();
            if (temp != null && temp.length() > 0) {
                if (temp.toUpperCase().startsWith("<BODY")) {
                    break;
                }
            }
        }
        v = new Vector();
        while (enums.hasMoreElements()) {
            temp = (String) enums.nextElement();
            temp = temp.trim();
            if (temp != null && temp.length() > 0) {
                if (temp.toUpperCase().startsWith("</BODY")) {
                    break;
                }
                v.add(temp);
            }
        }
        return v;
    }

    public Vector getReturnRangeTextLineVector(String from, String to, String[] filter) {
        Vector v = getReturnLineVector();
        Enumeration enums = v.elements();
        String temp = null;
        while (enums.hasMoreElements()) {
            temp = (String) enums.nextElement();
            temp = temp.trim();
            if (temp != null && temp.length() > 0) {
                if (temp.startsWith(from)) {
                    break;
                }
            }
        }
        v = new Vector();
        if (!temp.startsWith("<")) {
            v.add(temp);
        }
        boolean add = false;
        while (enums.hasMoreElements()) {
            add = true;
            temp = (String) enums.nextElement();
            temp = temp.trim();
            if (temp != null && temp.length() > 0) {
                if (temp.startsWith(to)) {
                    break;
                }
                if (!temp.startsWith("<")) {
                    for (int n = 0; n < filter.length; n++) {
                        if (temp.startsWith(filter[n])) {
                            add = false;
                        }
                    }
                    if (add) {
                        v.add(temp);
                    }
                }
            }
        }
        return v;
    }

    public void connectToUrl(String url_address) {
        message = new StringBuffer("");
        try {
            URL url = new URL(url_address);
            try {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) url.openConnection();
                httpsConnection.setDoOutput(false);
                httpsConnection.connect();
                message.append("<BR>\n Connection Code:[" + httpsConnection.getResponseCode() + "]");
                message.append("<BR>\n Response Message:[" + httpsConnection.getResponseMessage() + "]");
                InputStreamReader insr = new InputStreamReader(httpsConnection.getInputStream());
                BufferedReader in = new BufferedReader(insr);
                fullStringBuffer = new StringBuffer("");
                String temp = in.readLine();
                while (temp != null) {
                    fullStringBuffer.append(temp);
                    temp = in.readLine();
                }
                in.close();
            } catch (IOException e) {
                message.append("<BR>\n [Error][IOException][" + e.getMessage() + "]");
            }
        } catch (MalformedURLException e) {
            message.append("<BR>\n [Error][MalformedURLException][" + e.getMessage() + "]");
        } catch (Exception e) {
            message.append("<BR>\n [Error][Exception][" + e.getMessage() + "]");
        }
    }
}
