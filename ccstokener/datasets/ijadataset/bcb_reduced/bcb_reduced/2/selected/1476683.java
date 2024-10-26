package com.appengine.news.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.appengine.news.utils.StringUtils;

/**
 * Send item 2 given email
 * 
 * @author Aliaksandr_Spichakou
 * 
 */
public class Rss2MailTask extends AbstractService {

    /**
	 * 
	 */
    private static final long serialVersionUID = 5704480272088752437L;

    private static final Logger log = Logger.getLogger(Rss2MailTask.class.getName());

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
        log.entering(Rss2MailTask.class.getName(), "service");
        final String ids = request.getParameter(VALUE_IDS);
        if (StringUtils.isEmpty(ids)) {
            return;
        }
        final String to = request.getParameter(VALUE_MAILTO);
        final String createRendererUrl = createRendererUrl(ids);
        String renderedBody = "";
        try {
            renderedBody = getRenderedBody(createRendererUrl);
            if (!StringUtils.isEmpty(renderedBody)) {
                final int indexOf = renderedBody.indexOf("The server encountered an error and could not complete your request.");
                if (indexOf >= 0) {
                    log.log(Level.SEVERE, "Cannot get rendererd items: Server error");
                    log.exiting(Rss2MailTask.class.getName(), "service");
                    resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    return;
                }
            } else {
                log.log(Level.SEVERE, "Cannot get rendererd items: contend empty");
                log.exiting(Rss2MailTask.class.getName(), "service");
                resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Cannot get rendererd items", e);
            log.exiting(Rss2MailTask.class.getName(), "service");
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        try {
            sendMessage(to, "Rss Updates", renderedBody);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Cannot send mail", e);
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
        log.exiting(Rss2MailTask.class.getName(), "service");
    }

    /**
	 * Create renderer url
	 * @param ids
	 * @return
	 */
    private String createRendererUrl(String ids) {
        log.entering(Rss2MailTask.class.getName(), "createRendererUrl");
        final String url = cfg.getAppUrl() + RENDERER_TASK + "?" + VALUE_IDS + "=" + ids;
        log.exiting(Rss2MailTask.class.getName(), "createRendererUrl");
        return url;
    }

    /**
	 * Send message
	 * @param to
	 * @param subject
	 * @param msgBody
	 * @throws Exception
	 */
    private void sendMessage(String to, String subject, String msgBody) throws Exception {
        log.entering(Rss2MailTask.class.getName(), "sendMessage");
        final Properties props = new Properties();
        final Session session = Session.getDefaultInstance(props, null);
        final Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(cfg.getAdminEmail(), cfg.getAppUrl()));
        msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to, ""));
        msg.setSubject(subject);
        final Multipart mp = new MimeMultipart();
        final MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(msgBody, "text/html");
        mp.addBodyPart(htmlPart);
        msg.setContent(mp);
        Transport.send(msg);
        log.exiting(Rss2MailTask.class.getName(), "sendMessage");
    }

    /**
	 * Read rendered items
	 * @param spec
	 * @return
	 * @throws Exception
	 */
    private String getRenderedBody(String spec) throws Exception {
        log.entering(Rss2MailTask.class.getName(), "getRenderedBody");
        final URL url = new URL(spec);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();
        final InputStream inputStream = connection.getInputStream();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        final StringBuffer bf = new StringBuffer();
        while (line != null) {
            line = reader.readLine();
            if (line != null) {
                bf.append(line);
            }
        }
        log.exiting(Rss2MailTask.class.getName(), "getRenderedBody");
        return bf.toString();
    }
}
