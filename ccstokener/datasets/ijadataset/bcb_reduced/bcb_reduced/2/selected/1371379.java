package org.rickmurphy.monitor;

import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import javax.ejb.MessageDrivenContext;
import javax.ejb.MessageDrivenBean;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.JMSException;
import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * scan the fr and tag privacy notices for routine use
 */
public class FederalRegisterMonitorBean implements MessageDrivenBean {

    /**
   *
   */
    private static Context initialContext;

    /**
   */
    public void onMessage() throws JMSException, Exception {
        System.out.println("monitoring");
        URL url = new URL("http://frwebgate3.access.gpo.gov/cgi-bin/waisgate.cgi?WAISdocID=28468916704+0+0+0&WAISaction=retrieve");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        Map headers = connection.getHeaderFields();
        Set entries = headers.entrySet();
        Set keys = headers.keySet();
        Iterator it = keys.iterator();
        while (it.hasNext()) {
            System.out.println("*** header " + headers.get(it.next()));
        }
        SORNLexer lexer = new SORNLexer(new BufferedInputStream(connection.getInputStream()));
        SORNParser parser = new SORNParser(lexer);
        parser.run();
        connection.disconnect();
    }

    /**
   * Gets the context from the container
   * @param context Container info for the message driven bean
   */
    public void setMessageDrivenContext(MessageDrivenContext context) {
    }

    /**
   * Lookup environment variables, create connection factories, and configure document builder
   */
    public void ejbCreate() {
        try {
            initialContext = (Context) new InitialContext().lookup("java:comp/env");
        } catch (Exception e) {
            System.out.println("exception");
            e.printStackTrace();
        }
    }

    /**
   * Closes the connection to the registry
   */
    public void ejbRemove() {
    }
}
