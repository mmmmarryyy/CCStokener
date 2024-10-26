package au.edu.qut.yawl.miscellaneousPrograms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 /**
 * 
 * @author Lachlan Aldred
 * Date: 10/03/2004
 * Time: 13:56:23
 * 
 */
public class TestSMS {

    public static void main(String[] args) {
        String uri = "http://ws.cdyne.com/ip2geo/ip2geo.asmx/ResolveIP?" + "IPaddress=131.181.105.71" + "&LicenseKey=0";
        System.out.println(executeGet(uri));
    }

    public static String executePost(String urlStr, String content) {
        StringBuffer result = new StringBuffer();
        try {
            Authentication.doIt();
            URL url = new URL(urlStr);
            System.out.println("Host: " + url.getHost());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            System.out.println("got connection ");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            connection.setRequestProperty("Content-Length", "" + content.length());
            connection.setRequestProperty("SOAPAction", "\"http://niki-bt.act.cmis.csiro.au/SMSService/SendText\"");
            connection.setRequestMethod("POST");
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            out.print(content);
            out.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                result.append(inputLine);
            }
            in.close();
            out.close();
            connection.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String msg = result.toString();
        if (msg != null) {
            int beginCut = msg.indexOf('>');
            int endCut = msg.lastIndexOf('<');
            if (beginCut != -1 && endCut != -1) {
                return msg.substring(beginCut + 1, endCut);
            }
        }
        return null;
    }

    private static String executeGet(String urlStr) {
        StringBuffer result = new StringBuffer();
        try {
            Authentication.doIt();
            URL url = new URL(urlStr);
            System.out.println("Host: " + url.getHost());
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                result.append(inputLine);
            }
            in.close();
            connection.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString();
    }
}
