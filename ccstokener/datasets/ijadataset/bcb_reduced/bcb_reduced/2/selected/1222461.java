package bman.tools.web;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Web Service Client
 * @author MrJacky
 *
 */
public class WebServiceClient {

    /**
     * Sends the xml to the url
     * @param purl url where the request is sent
     * @param xml xml request sent
     * @return string response
     * @throws Exception when an error is encountered
     */
    public static String send(String purl, String xml) throws Exception {
        URL url = new URL(purl);
        HttpURLConnection httpUrlCon = (HttpURLConnection) url.openConnection();
        httpUrlCon.setRequestMethod("POST");
        httpUrlCon.setDoOutput(true);
        PrintWriter writer = new PrintWriter(httpUrlCon.getOutputStream());
        writer.println(xml);
        writer.flush();
        writer.close();
        System.out.println("Sending: " + xml);
        StringBuffer response = new StringBuffer();
        InputStreamReader is = null;
        try {
            is = new InputStreamReader(httpUrlCon.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
            is = new InputStreamReader(httpUrlCon.getErrorStream());
        }
        BufferedReader br = new BufferedReader(is);
        String line = null;
        while ((line = br.readLine()) != null) {
            response.append(line);
            response.append("\n");
        }
        String r = response.toString();
        return r;
    }
}
