package ro.gateway.aida.usr.messaging;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import ro.gateway.aida.db.DBPersistenceManager;
import ro.gateway.aida.db.PersistenceToken;
import ro.gateway.aida.utils.Utils;

/**
 * @author Mihai Postelnicu
 * Relay
 * Implements LPD data transport methods across the net
 *
 *  *
 *  *
 */
public class LPDRelay extends DBPersistenceManager {

    private LPDRelay(PersistenceToken token) {
        super(token);
    }

    public static LPDRelay getManager(PersistenceToken token) {
        return new LPDRelay(token);
    }

    /**
       * Returns the text obtained by making a http connection to the source URL
       * @param surl The full URL from where the file should be retrieved
       * @return The retrieved HTTP GET response
       * @see java.net
       */
    public static String getHTTP(String surl) {
        try {
            URL url = new URL(surl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            BufferedInputStream in = new BufferedInputStream(con.getInputStream());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            BufferedOutputStream out = new BufferedOutputStream(bos);
            int BUF_SIZE = 1024;
            byte[] znak = new byte[BUF_SIZE];
            int bytesRead = 0;
            while ((bytesRead = in.read(znak)) > -1) out.write(znak, 0, bytesRead);
            in.close();
            out.close();
            return bos.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
       * send an email (using javax.mail) to the specified recipient.
       * @param subject The email subject
       * @param msg The message body (text/html)
       * @param to The destination recipient
       * @see javax.mail
       */
    public void sendMail(String subject, String msg, String to) {
        try {
            String from = Utils.getProperty(token, "replyToUser");
            String server = Utils.getProperty(token, "mailServer");
            Properties props = new Properties();
            props.setProperty("mail.transport.protocol", "smtp");
            props.setProperty("mail.host", server);
            props.setProperty("mail.user", "");
            props.setProperty("mail.password", "");
            Session mailSession = Session.getDefaultInstance(props, null);
            Transport transport = mailSession.getTransport();
            MimeMessage message = new MimeMessage(mailSession);
            message.setContent(msg, "text/html");
            message.setSubject(subject);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            transport.connect();
            transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
            transport.close();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (MessagingException e2) {
            e2.printStackTrace();
        }
    }
}
