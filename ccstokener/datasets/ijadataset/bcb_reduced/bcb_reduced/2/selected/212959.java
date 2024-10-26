package jircbot.plugins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 * Handles the responses from the aiml bot site
 *
 * @author matt
 */
public class AimlHandler {

    /**
     * Retrieves a response from the aiml bot site using the provided message
     * and identification
     *
     * @param message the input message to respond to
     * @param id the unique id used to recognise the individuals
     * @return the response to the input message
     */
    public String getResponse(String message, String id) {
        String data, xmlResponse = null;
        try {
            data = URLEncoder.encode("botid", "UTF-8") + "=" + URLEncoder.encode("a0a2b58fce3752f8", "UTF-8");
            data += "&" + URLEncoder.encode("input", "UTF-8") + "=" + URLEncoder.encode(message, "UTF-8");
            data += "&" + URLEncoder.encode("custid", "UTF-8") + "=" + URLEncoder.encode(id, "UTF-8");
            URL url = new URL("http://www.pandorabots.com/pandora/talk-xml");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            xmlResponse = rd.readLine();
            wr.close();
            rd.close();
        } catch (IOException e) {
            System.out.println("I/O Error: " + e);
        }
        return getMessage(xmlResponse);
    }

    /**
     * Returns a message from pandora bots.
     *
     * @param botId The identification string for the bot
     * @param userId The unique user id
     * @param message The message to respond to
     * @return The response message.
     */
    public String getResponse(String botId, String userId, String message) {
        String data, xmlResponse = "";
        try {
            data = URLEncoder.encode("botid", "UTF-8") + "=" + URLEncoder.encode(botId, "UTF-8");
            data += "&" + URLEncoder.encode("input", "UTF-8") + "=" + URLEncoder.encode(message, "UTF-8");
            data += "&" + URLEncoder.encode("custid", "UTF-8") + "=" + URLEncoder.encode(userId, "UTF-8");
            URL url = new URL("http://www.pandorabots.com/pandora/talk-xml");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            xmlResponse = rd.readLine();
            wr.close();
            rd.close();
        } catch (IOException e) {
            System.out.println("I/O Error: " + e);
        }
        return getMessage(xmlResponse);
    }

    private String getMessage(String xmlResponse) {
        return xmlResponse.substring(xmlResponse.indexOf("<that>"), xmlResponse.indexOf("</that>") - 7);
    }
}
