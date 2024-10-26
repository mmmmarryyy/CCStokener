package com.zorgly.nabaztag.utils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.zorgly.commons.utils.XmlUtils;

/**
 * Utilitary class to play with HTTP protocol.
 * 
 * @see com.zorgly.commons.utils.StreamUtils
 * @see com.zorgly.commons.utils.StringUtils
 * @see com.zorgly.commons.utils.XmlUtils
 * @author Vassyly Lygeros
 */
public class HttpUtils {

    /**
	 * This method sends the commands to the server.
	 * 
	 * @param urlAddress The full URL
	 * @return The root of the document tree.
	 */
    public static Document send(final String urlAddress) {
        Document responseMessage = null;
        try {
            URL url = new URL(urlAddress);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setAllowUserInteraction(false);
            int response = connection.getResponseCode();
            if (response == HttpURLConnection.HTTP_OK) {
                String contentType = connection.getContentType();
                if (contentType != null && contentType.startsWith("text/html")) {
                    InputStream inputStream = connection.getInputStream();
                    responseMessage = XmlUtils.fromStream(inputStream);
                } else {
                    responseMessage = XmlUtils.newDocument();
                    Element responseElement = XmlUtils.createElement(responseMessage, "rsp");
                    Element messageElement = XmlUtils.createElement(responseElement, "message");
                    messageElement.setTextContent(String.valueOf(connection.getResponseCode()));
                    Element commentElement = XmlUtils.createElement(responseElement, "comment");
                    commentElement.setTextContent(contentType);
                }
            } else {
                responseMessage = XmlUtils.newDocument();
                Element responseElement = XmlUtils.createElement(responseMessage, "rsp");
                Element messageElement = XmlUtils.createElement(responseElement, "message");
                messageElement.setTextContent(String.valueOf(connection.getResponseCode()));
                Element commentElement = XmlUtils.createElement(responseElement, "comment");
                commentElement.setTextContent(connection.getResponseMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return responseMessage;
    }
}
