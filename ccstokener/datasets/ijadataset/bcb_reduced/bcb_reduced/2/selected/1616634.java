package org.lindenb.wikipedia.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Iterator;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * connect to the mediawiki API, login, and stores the information
 * about the session.
 * @author lindenb
 *
 */
public class MWAuthentication {

    private String userid = null;

    private String username = null;

    private String token = null;

    private String cookieprefix = null;

    private String sessionid = null;

    private MWAuthentication() {
    }

    public String getUserId() {
        return userid;
    }

    public String getUserName() {
        return username;
    }

    public String getToken() {
        return token;
    }

    public String getCookiePrefix() {
        return cookieprefix;
    }

    public String getSessionId() {
        return sessionid;
    }

    @Override
    public int hashCode() {
        return getSessionId() == null ? -1 : getSessionId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public String toString() {
        return getClass().getName() + " : " + getUserName();
    }

    /**
	 * creates a new MWAuthentication
	 * @param api_url the url of the api.php
	 * @param authentication the pair(login/password)
	 * @return
	 * @throws IOException
	 * @throws XMLStreamException
	 */
    public static MWAuthentication login(String api_url, PasswordAuthentication authentication) throws IOException, XMLStreamException {
        MWAuthentication log = new MWAuthentication();
        URL url = new URL(api_url);
        URLConnection con = url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        PrintWriter wr = new PrintWriter(con.getOutputStream());
        wr.print("format=xml&action=login&lgname=");
        wr.print(URLEncoder.encode(authentication.getUserName(), "UTF-8"));
        wr.print("&lgpassword=");
        wr.print(URLEncoder.encode(new String(authentication.getPassword()), "UTF-8"));
        wr.flush();
        String result = null;
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        factory.setProperty(XMLInputFactory.IS_VALIDATING, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        XMLEventReader reader = factory.createXMLEventReader(con.getInputStream());
        while (reader.hasNext()) {
            XMLEvent evt = reader.nextEvent();
            if (!evt.isStartElement()) continue;
            StartElement start = evt.asStartElement();
            if (!start.getName().getLocalPart().equals("login")) continue;
            for (Iterator<?> it = start.getAttributes(); it.hasNext(); ) {
                Attribute att = Attribute.class.cast(it.next());
                String name = att.getName().getLocalPart();
                if (name.equals("lguserid")) {
                    log.userid = att.getValue();
                } else if (name.equals("result")) {
                    result = att.getValue();
                } else if (name.equals("lgusername")) {
                    log.username = att.getValue();
                } else if (name.equals("lgtoken")) {
                    log.token = att.getValue();
                } else if (name.equals("cookieprefix")) {
                    log.cookieprefix = att.getValue();
                } else if (name.equals("sessionid")) {
                    log.sessionid = att.getValue();
                }
            }
        }
        wr.close();
        reader.close();
        if (result == null || !result.equals("Success")) throw new IOException("Authentication failed");
        return log;
    }
}
