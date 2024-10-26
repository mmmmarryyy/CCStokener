package net.trieloff.xmlwebgui.util;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.regexp.RE;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *  This class contains methods for managing the read and writing rights of the
 *  user. This includes validating a given sessionid and retrieving the username
 *  ofthe user that belongs to this session.
 *
 *@author     Lars Trieloff
 *@created    4. M�rz 2002
 */
public class UserRights {

    /**
   *  Returns wheter the user is allowed to read or write a file or not.
   *
   *@param  userNode  information about UserRights a DOM Element
   *@param  filename  name of the file to test
   *@param  mode      &quot;read&quot; for reading access, &quot;write&quot; for
   *      writing access
   *@return           true, if the user can read/write, else false
   *@since
   */
    public static boolean getUserRights(Element userNode, String filename, String mode) {
        boolean reading = false;
        try {
            NodeList readNodes = userNode.getElementsByTagName(mode).item(0).getChildNodes();
            RE regexp;
            Element allowNode;
            for (int i = 0; i < readNodes.getLength(); i++) {
                if (readNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    allowNode = (Element) readNodes.item(i);
                    if (!reading && allowNode.getTagName().equals("allow")) {
                        regexp = new RE(allowNode.getAttribute("pattern"));
                        if (regexp.match(filename)) {
                            reading = true;
                        }
                    } else if (reading && allowNode.getTagName().equals("disallow")) {
                        regexp = new RE(allowNode.getAttribute("pattern"));
                        if (regexp.match(filename)) {
                            reading = false;
                        }
                    }
                }
            }
        } catch (Exception rese) {
            rese.printStackTrace();
        }
        return reading;
    }

    /**
   *  Gets the customSessionId attribute of the UserRights object
   *
   *@param  request      Description of Parameter
   *@param  sessionName  Description of Parameter
   *@return              The customSessionId value
   *@since
   */
    public static String getCustomSessionId(HttpServletRequest request, String sessionName) {
        boolean fromCookie = false;
        String sessionIdValue = new String();
        Cookie[] cookieArray = request.getCookies();
        if (cookieArray != null) {
            for (int i = 0; i < cookieArray.length; i++) {
                if (cookieArray[i].getName().equals(sessionName)) {
                    fromCookie = true;
                    sessionIdValue = cookieArray[i].getValue();
                }
            }
        }
        if (!fromCookie) {
            sessionIdValue = request.getParameter(sessionName);
        }
        return sessionIdValue;
    }

    /**
   *  Gets the customUsername attribute of the UserRights object
   *
   *@param  sessionValidatorURL  Description of Parameter
   *@param  sessionName          Description of Parameter
   *@param  sessionIdValue       Description of Parameter
   *@return                      The customUsername value
   *@since
   */
    public static String getCustomUsername(String sessionValidatorURL, String sessionName, String sessionIdValue) {
        try {
            URL url;
            URLConnection urlConn;
            DataOutputStream printout;
            DataInputStream input;
            url = new URL(sessionValidatorURL);
            urlConn = url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setUseCaches(false);
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            printout = new DataOutputStream(urlConn.getOutputStream());
            String content = new String();
            content = sessionName + "=" + sessionIdValue;
            printout.writeBytes(content);
            printout.flush();
            printout.close();
            input = new DataInputStream(urlConn.getInputStream());
            String str;
            String username = new String();
            while (null != ((str = input.readLine()))) {
                username = username + str;
            }
            input.close();
            return username;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
   *  Gets the userNode attribute of the UserRights object
   *
   *@param  apppath   Description of Parameter
   *@param  username  Description of Parameter
   *@return           The userNode value
   *@since
   */
    public static Element getUserNode(String apppath, String username) {
        Element userNode = null;
        try {
            File userRights = new File(apppath + "WEB-INF/users.xml");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(userRights);
            NodeList userList = document.getElementsByTagName("user");
            for (int i = 0; i < userList.getLength(); i++) {
                if (((Element) userList.item(i)).getAttribute("name").equals(username)) {
                    userNode = (Element) userList.item(i);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userNode;
    }
}
