package org.azrul.epice.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Logger;
import javax.servlet.http.*;
import java.util.*;
import java.util.logging.Level;

public class SendMailUtil extends HttpServlet {

    public static void send(String from, String to, String message, String subject) {
        try {
            ResourceBundle appProps = ResourceBundle.getBundle("epice");
            URL url = new URL(appProps.getString("ASYNC_SERVICES") + "Controller?action=sendEmail&from=" + URLEncoder.encode(from, "UTF8") + "&to=" + URLEncoder.encode(to, "UTF8") + "&message=" + URLEncoder.encode(message, "UTF8") + "&subject=" + URLEncoder.encode(subject, "UTF8") + "&smtpUser=" + appProps.getString("SMTP_USER") + "&smtpPassword=" + appProps.getString("SMTP_PASSWORD") + "&smtpHost=" + appProps.getString("SMTP_HOST") + "&smtpPort=" + appProps.getString("SMTP_PORT") + "&smtpStartTlsEnable=" + appProps.getString("SMTP_STARTTLS_ENABLE") + "&smtpAuth=" + appProps.getString("SMTP_AUTH") + "&smtpSocketFactoryPort=" + appProps.getString("SMTP_SOCKETFACTORY_PORT") + "&smtpSocketFactoryClass=" + appProps.getString("SMTP_SOCKETFACTORY_CLASS") + "&smtpSocketFactoryFallback=" + appProps.getString("SMTP_SOCKETFACTORY_FALLBACK"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            String msg = conn.getResponseMessage();
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(SendMailUtil.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedURLException ex) {
            Logger.getLogger(SendMailUtil.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SendMailUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) {
        SendMailUtil.send("azrulhasni@gmail.com", "azrulhasni@gmail.com", "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\" /><title>EPICE Invitation</title></head><body><p>To whom it may concern, </p><p>A task was sent to you through EPICE. Please click <a href=\"%%Link%%/faces/PgEditProfile.jsp?email=%%email%%&key=%%key%%\">here</a> to start</p><p>Your sincerely </p><p>%%Sender%%</p></body></html>", "subject");
    }
}
