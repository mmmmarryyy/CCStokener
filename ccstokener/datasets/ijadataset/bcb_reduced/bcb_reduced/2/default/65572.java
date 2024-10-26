import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import com.sun.mail.smtp.*;

public class TestWebConnect {

    private Vector<String> failLinkList = new Vector<String>();

    private Properties props = new Properties();

    private Authenticator auth;

    private Transport transport;

    private Exception errHandler;

    private String[] webList;

    private String websites = "";

    private String smtpHost = "";

    private String smtpPort = "";

    private String smtpUserAuth = "";

    private String smtpUser = "";

    private String smtpPwd = "";

    private String mailSender = "";

    private String receipients = "";

    private String subject = "";

    private String message = "";

    private int timeout = 0;

    private int index = 0;

    private int linkListLen;

    /**
	 * read parameters from a property file
	 * @param propertyFile the path pf the property file
	 */
    private void init(String propertyFile) {
        try {
            props.load(new FileInputStream(propertyFile));
            websites = props.getProperty("websites");
            webList = websites.split(",");
            if (webList == null || webList.length == 0) {
                return;
            }
            smtpHost = props.getProperty("mail.smtp.host");
            smtpPort = props.getProperty("mail.smtp.port");
            smtpUserAuth = props.getProperty("mail.smtp.userauth");
            smtpUser = props.getProperty("mail.smtp.user");
            smtpPwd = props.getProperty("mail.smtp.pwd");
            mailSender = props.getProperty("mailsender");
            receipients = props.getProperty("receipients");
            subject = props.getProperty("subject");
            message = props.getProperty("message");
            timeout = props.getProperty("timeout") == null ? 6 * 1000 : Integer.parseInt(props.getProperty("timeout"));
            auth = new SMTPAuthenticator();
            linkListLen = webList.length;
            failLinkList.setSize(0);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * check the web connection one by one
	 *
	 */
    private void checkWebLink() {
        URI uri;
        URL url;
        int responseCode = -1;
        HttpURLConnection httpConnect = null;
        try {
            while (index < linkListLen) {
                uri = new URI("");
                uri = uri.create(webList[index]);
                if (uri != null) {
                    url = uri.toURL();
                    if (url != null) {
                        httpConnect = (HttpURLConnection) url.openConnection();
                        if (httpConnect != null) {
                            httpConnect.setConnectTimeout(timeout);
                            httpConnect.setReadTimeout(timeout);
                            httpConnect.connect();
                            responseCode = httpConnect.getResponseCode();
                            if (!isHttpRespOK(responseCode)) {
                                failLinkList.addElement(webList[index]);
                            }
                            httpConnect.disconnect();
                            index++;
                        }
                    }
                }
            }
        } catch (SocketTimeoutException e) {
            errHandler = e;
        } catch (URISyntaxException e) {
            errHandler = e;
        } catch (IOException e) {
            errHandler = e;
        } finally {
            if (errHandler != null) {
                handleExpt(errHandler, httpConnect);
            }
        }
    }

    /**
	 * when one connection triggers exceptions, continue trying others.
	 * @param e expection object
	 * @param httpConnect the java object for the failed connection
	 */
    private void handleExpt(Exception e, HttpURLConnection httpConnect) {
        System.err.println(e.getMessage());
        failLinkList.addElement(webList[index]);
        if (httpConnect != null) {
            httpConnect.disconnect();
        }
        index++;
        errHandler = null;
        checkWebLink();
    }

    /**
	 * 
	 * @param respCode http response code
	 * @return true -- connetion is ok
	 * 		   false -- connection is problematic
	 */
    private boolean isHttpRespOK(int respCode) {
        if (respCode != HttpURLConnection.HTTP_ACCEPTED && respCode != HttpURLConnection.HTTP_OK) {
            return false;
        }
        return true;
    }

    /**
	 * email the failure
	 *
	 */
    private void emailFailMsg() {
        if (failLinkList == null || failLinkList.size() == 0) {
            return;
        }
        try {
            Session session = Session.getInstance(props, auth);
            session.setDebug(true);
            transport = new SMTPSSLTransport(session, new URLName("smtp", smtpHost, Integer.parseInt(smtpPort), "", smtpUser, smtpPwd));
            transport.connect(smtpHost, smtpUser, smtpPwd);
            if (!transport.isConnected()) {
                return;
            }
            message += "\n";
            for (int index = 0; index < failLinkList.size(); index++) {
                message += "\n" + failLinkList.get(index) + "\n";
            }
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(mailSender));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(receipients, false));
            msg.setSubject(subject);
            msg.setSentDate(new Date());
            msg.setText(message);
            msg.setSentDate(new Date());
            msg.saveChanges();
            transport.sendMessage(msg, msg.getAllRecipients());
            transport.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
	 * @param args
	 * @throws URISyntaxException 
	 */
    public static void main(String[] args) {
        if (args != null && args.length != 0) {
            TestWebConnect testEbeWeb = new TestWebConnect();
            testEbeWeb.init(args[0]);
            testEbeWeb.checkWebLink();
            testEbeWeb.emailFailMsg();
        }
    }

    /**
	 * SMTPAuthenticator is used to perform an authentication when the SMTP
	 * server requires it.
	 */
    private class SMTPAuthenticator extends javax.mail.Authenticator {

        public PasswordAuthentication getPasswordAuthentication() {
            String username = smtpUserAuth;
            String password = smtpPwd;
            return new PasswordAuthentication(username, password);
        }
    }
}
