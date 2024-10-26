package eu.mpower.framework.interoperability.externalnotification;

import eu.mpower.framework.interoperability.externalnotification.soap.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.AddressException;
import java.security.Security;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.net.URLEncoder;
import java.util.InvalidPropertiesFormatException;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author fuxreiter
 */
public class NotificationManager {

    private static Log logger = LogFactory.getLog(NotificationManager.class);

    private EmailGateway emailGateway;

    private HandyweltHTTP2SMSGateway smsGW;

    private final String emailGwConfigFileName = "emailgateway.properties";

    private final String smsGwConfigFileName = "smsgateway.properties";

    public NotificationManager() {
        logger.trace("Instanciate NotificationManager class");
        this.emailGateway = new EmailGateway();
        initEmailGateway();
        this.smsGW = new HandyweltHTTP2SMSGateway();
        initSMSGateway();
    }

    private void initEmailGateway() {
        Properties emailGwProperties = new Properties();
        int res = readPropertiesFile(this.emailGwConfigFileName, emailGwProperties);
        if (res < 0) {
            logger.warn("Error reading emailGatewayConfigFile " + this.emailGwConfigFileName + "; Now using default values for ARCSMED mail relay server!");
            this.emailGateway.setDEFAULTSENDER("foex@arcsmed.at");
            this.emailGateway.setDOMAINNAME("arcsmed.at");
            this.emailGateway.setPORT("25");
            this.emailGateway.setSERVERHOSTNAME("83.64.124.177");
            this.emailGateway.setSERVERLOGIN("");
            this.emailGateway.setSERVERPASSWORD("");
        } else {
            this.emailGateway.setDEFAULTSENDER((String) emailGwProperties.getProperty("DEFAULTSENDER"));
            this.emailGateway.setDOMAINNAME((String) emailGwProperties.getProperty("DOMAINNAME"));
            this.emailGateway.setPORT((String) emailGwProperties.getProperty("PORT"));
            this.emailGateway.setSERVERHOSTNAME((String) emailGwProperties.getProperty("SERVERHOSTNAME"));
            this.emailGateway.setSERVERLOGIN((String) emailGwProperties.getProperty("SERVERLOGIN"));
            this.emailGateway.setSERVERPASSWORD((String) emailGwProperties.getProperty("SERVERPASSWORD"));
        }
        logger.info("Used SMTP gateway: " + this.emailGateway.getSERVERHOSTNAME());
    }

    public void emailNotify(EmailMessage email, Status status) {
        List<String> recipients = email.getReceipient();
        String emailSubjectTxt = email.getSubject();
        String emailMsgTxt = null;
        if (email.getText() == null) {
            emailMsgTxt = "";
        } else emailMsgTxt = email.getText();
        String emailFromAddress = "";
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        Properties props = new Properties();
        logger.debug("EMail server: " + this.emailGateway.getSERVERHOSTNAME());
        props.put("mail.smtp.host", this.emailGateway.getSERVERHOSTNAME());
        props.put("mail.smtp.auth", "false");
        props.put("mail.debug", "false");
        props.put("mail.smtp.port", this.emailGateway.getPORT());
        Session session = Session.getDefaultInstance(props, null);
        javax.mail.Message msg = new MimeMessage(session);
        if (email.getSender() == null || email.getSender().isEmpty()) {
            emailFromAddress = this.emailGateway.getDEFAULTSENDER();
        } else {
            emailFromAddress = email.getSender();
            try {
                new InternetAddress(emailFromAddress, true);
            } catch (AddressException adrex) {
                logger.debug(adrex.toString());
                emailFromAddress = this.emailGateway.getDEFAULTSENDER();
                logger.info("Internet address 'sender' is not valid! > Using the default sender address: " + emailFromAddress);
            }
        }
        logger.debug("emailFromAddress: " + emailFromAddress);
        InternetAddress addressFrom;
        try {
            addressFrom = new InternetAddress(emailFromAddress, true);
        } catch (AddressException addressException) {
            logger.error("Internet address 'from' is not valid! " + emailFromAddress);
            logger.error("AddressException: " + addressException.toString());
            status.setErrorCause("Internet address 'from' is not valid! " + emailFromAddress);
            status.setResult(ErrorCodes.EXTERNALNOTIFICATION_ERROR_EMAIL_ADDRESS_NOT_VALID.ordinal());
            return;
        }
        InternetAddress[] addressTo = new InternetAddress[recipients.size()];
        int i = 0;
        String address;
        InternetAddress iAddr;
        for (Iterator itr = recipients.iterator(); itr.hasNext(); ) {
            address = (String) itr.next();
            logger.debug("Email To address:" + address);
            try {
                iAddr = new InternetAddress(address, true);
            } catch (AddressException addressException) {
                logger.error("Internet address 'to' is not valid! " + address);
                logger.error("AddressException: " + addressException.toString());
                status.setErrorCause("Internet address 'to' is not valid! " + address);
                status.setResult(ErrorCodes.EXTERNALNOTIFICATION_ERROR_EMAIL_ADDRESS_NOT_VALID.ordinal());
                return;
            }
            addressTo[i++] = iAddr;
        }
        try {
            msg.setFrom(addressFrom);
            msg.setRecipients(javax.mail.Message.RecipientType.TO, addressTo);
            msg.setSubject(emailSubjectTxt);
            msg.setContent(emailMsgTxt, "text/plain");
            logger.debug("now sending email");
            Transport.send(msg);
        } catch (MessagingException ex) {
            logger.error("MessagingException: " + ex.toString());
            status.setErrorCause("MessagingException: " + ex.toString());
            status.setResult(ErrorCodes.EXTERNALNOTIFICATION_ERROR_SENDING_EMAIL.ordinal());
            return;
        }
        logger.info("Email successfully sent to " + recipients + " from " + emailFromAddress);
        status.setErrorCause("Email successfully sent to " + recipients + " from " + emailFromAddress);
        status.setResult(ErrorCodes.EXTERNALNOTIFICATION_OK.ordinal());
    }

    private void initSMSGateway() {
        Properties smsGwProperties = new Properties();
        int res = readPropertiesFile(this.smsGwConfigFileName, smsGwProperties);
        if (res < 0) {
            logger.warn("Error reading smsGatewayConfigFile " + this.smsGwConfigFileName + "; Now using default values for SMS gateway!");
            this.smsGW.setFrom("436648251044");
            this.smsGW.setUrl("http://sms.edev.at/send/?");
            this.smsGW.setId("ARCSMED");
            this.smsGW.setPwd("ARCSMED");
            this.smsGW.setRoute("15");
            this.smsGW.setTyp("1");
            this.smsGW.setUser("tom");
        } else {
            this.smsGW.setFrom((String) smsGwProperties.getProperty("From"));
            this.smsGW.setUrl((String) smsGwProperties.getProperty("Url"));
            this.smsGW.setId((String) smsGwProperties.getProperty("Id"));
            this.smsGW.setPwd((String) smsGwProperties.getProperty("Pwd"));
            this.smsGW.setFlash((String) smsGwProperties.getProperty("Flash"));
            this.smsGW.setRoute((String) smsGwProperties.getProperty("Route"));
            this.smsGW.setAutoroute((String) smsGwProperties.getProperty("Autoroute"));
            this.smsGW.setStatus((String) smsGwProperties.getProperty("Status"));
            this.smsGW.setSim((String) smsGwProperties.getProperty("Sim"));
            this.smsGW.setTyp((String) smsGwProperties.getProperty("Typ"));
            this.smsGW.setUser((String) smsGwProperties.getProperty("User"));
        }
    }

    public void smsNotify(SMSMessage sms, Status status) {
        String encodedTxt = null;
        int result = 0;
        if (!sms.getSenderPhoneNumber().isEmpty()) {
            if (!hasOnlyDigits(sms.getSenderPhoneNumber())) {
                logger.error("sms.Sender has wrong format: " + sms.getSenderPhoneNumber());
                status.setErrorCause("sms.Sender has wrong format: " + sms.getSenderPhoneNumber());
                status.setResult(ErrorCodes.EXTERNALNOTIFICATION_ERROR_SENDER_PHONENUMBER_INVALID_FORMAT.ordinal());
                return;
            }
            this.smsGW.setFrom(sms.getSenderPhoneNumber());
        }
        if (sms.getText().isEmpty() || !hasLetters(sms.getText())) {
            logger.error("Text parameter is empty or has no letters!");
            status.setErrorCause("Text parameter is empty or has no letters!");
            status.setResult(ErrorCodes.EXTERNALNOTIFICATION_ERROR_CONTENT_EMPTY.ordinal());
            return;
        }
        try {
            encodedTxt = URLEncoder.encode(sms.getText(), "UTF-8");
        } catch (UnsupportedEncodingException ex6) {
            logger.error("sms.Txt conversion (to URL) failed: " + sms.getText());
            logger.error("Exception Message: " + ex6.toString());
            status.setErrorCause("sms.Txt conversion (to URL) failed: " + sms.getText() + " Exception Message: " + ex6.toString());
            status.setResult(ErrorCodes.EXTERNALNOTIFICATION_ERROR_CONTENT_TO_URL_TRANSFORMATION_FAILED.ordinal());
            return;
        }
        this.smsGW.setTxt(encodedTxt);
        int i = 0;
        String tempReceipient = null;
        Iterator iter = sms.getReceipientPhoneNumber().listIterator();
        while (iter.hasNext() && result == 0) {
            tempReceipient = (String) (iter.next());
            if (!hasOnlyDigits(tempReceipient)) {
                logger.error("sms.Receipient has wrong format: " + tempReceipient);
                status.setErrorCause("sms.Receipient has wrong format: " + tempReceipient);
                status.setResult(ErrorCodes.EXTERNALNOTIFICATION_ERROR_RECEIVER_PHONENUMBER_INVALID_FORMAT.ordinal());
                return;
            }
            this.smsGW.setTo(tempReceipient);
            processHTTPRequest(status);
            this.smsGW.setTo(null);
        }
    }

    private void processHTTPRequest(Status status) {
        String httpRequest = null;
        Document xmlDoc = null;
        httpRequest = this.smsGW.getUrl();
        if (this.smsGW.getFrom() != null) httpRequest += "from=" + this.smsGW.getFrom();
        if (this.smsGW.getTo() != null) httpRequest += "&to=" + this.smsGW.getTo();
        if (this.smsGW.getTxt() != null) httpRequest += "&txt=" + this.smsGW.getTxt();
        httpRequest += "&id=" + this.smsGW.getId() + "&pwd=" + this.smsGW.getPwd();
        if (this.smsGW.getFlash() != null) httpRequest += "&flash=" + this.smsGW.getFlash();
        if (this.smsGW.getRoute() != null) httpRequest += "&route=" + this.smsGW.getRoute();
        if (this.smsGW.getAutoroute() != null) httpRequest += "&autoroute=" + this.smsGW.getAutoroute();
        if (this.smsGW.getStatus() != null) httpRequest += "&status=" + this.smsGW.getStatus();
        if (this.smsGW.getSim() != null) httpRequest += "&sim=" + this.smsGW.getSim();
        if (this.smsGW.getTyp() != null) httpRequest += "&typ=" + this.smsGW.getTyp();
        if (this.smsGW.getUser() != null) httpRequest += "&user=" + this.smsGW.getUser();
        logger.debug("HTTP2SMS request: " + httpRequest);
        InputStream is = null;
        try {
            URL url = new URL(httpRequest);
            is = url.openStream();
            logger.debug("HTTP request sent!");
            xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
        } catch (Exception ex2) {
            logger.error("Exception Message: " + ex2.toString());
            status.setErrorCause("Exception Message: " + ex2.toString());
            status.setResult(ErrorCodes.EXTERNALNOTIFICATION_ERROR_RESPONSE_FROM_SMS_GATEWAY.ordinal());
        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException ex3) {
                logger.error("Exception Message: " + ex3.toString());
            }
        }
        NodeList nl = xmlDoc.getElementsByTagName("response");
        Node nd = nl.item(0);
        NodeList nl2 = nd.getChildNodes();
        String responseResult = nl2.item(1).getTextContent();
        String responseDesc = nl2.item(3).getTextContent();
        String responseId = nl2.item(5).getTextContent();
        int responseRes = Integer.parseInt(responseResult);
        if (responseRes == 0) {
            logger.debug("HTTP2SMS response: result: " + responseResult + "; desc: " + responseDesc + "; ID: " + responseId);
        } else {
            logger.error("HTTP2SMS response: result: " + responseResult + "; desc: " + responseDesc + "; ID: " + responseId);
        }
        if (responseRes == 0) {
            logger.info("SMS with id " + responseId + " successfully sent to number " + this.smsGW.getTo());
            status.setErrorCause("SMS with id " + responseId + " successfully sent to number " + this.smsGW.getTo());
            status.setResult(ErrorCodes.EXTERNALNOTIFICATION_OK.ordinal());
        } else if (responseRes == 1) {
            logger.error("System error in external SMS gateway! HTTP request: " + httpRequest);
            status.setErrorCause("System error in external SMS gateway! HTTP request: " + httpRequest);
            status.setResult(ErrorCodes.EXTERNALNOTIFICATION_ERROR_SYSTEM_ERROR_IN_SMS_GATEWAY.ordinal());
        } else if (responseRes == 2) {
            logger.error("Sending error in external SMS gateway! HTTP request: " + httpRequest);
            logger.error("SMS2HTTP Gateway Response: ResultCode:" + responseResult + "; ErrorDescription:" + responseDesc + "; TransactionID:" + responseId);
            status.setErrorCause("Sending error in external SMS gateway! ErrorDescription:" + responseDesc);
            status.setResult(ErrorCodes.EXTERNALNOTIFICATION_ERROR_SENDING_ERROR_IN_SMS_GATEWAY.ordinal());
        } else if (responseRes >= 10 && responseRes <= 19) {
            logger.error("SMS gateway says: Parameter error in HTTP request: " + httpRequest);
            logger.error("SMS2HTTP Gateway Response: ResultCode:" + responseResult + "; ErrorDescription:" + responseDesc + "; TransactionID:" + responseId);
            status.setErrorCause("SMS gateway says: Parameter error in HTTP request! ErrorDescription:" + responseDesc);
            status.setResult(ErrorCodes.EXTERNALNOTIFICATION_ERROR_PARAMETER_ERROR_IN_SMS_GATEWAY.ordinal());
        } else if (responseRes >= 20 && responseRes <= 29) {
            logger.error("Limit reached at external SMS gateway!");
            logger.error("SMS2HTTP Gateway Response: ResultCode:" + responseResult + "; ErrorDescription:" + responseDesc + "; TransactionID:" + responseId);
            status.setErrorCause("Limit reached at external SMS gateway!");
            status.setResult(ErrorCodes.EXTERNALNOTIFICATION_ERROR_LIMIT_REACHED_IN_SMS_GATEWAY.ordinal());
        } else {
            logger.error("Undefined error from external SMS gateway!");
            logger.error("SMS2HTTP Gateway Response: ResultCode:" + responseResult + "; ErrorDescription:" + responseDesc + "; TransactionID:" + responseId);
            status.setErrorCause("Undefined error from external SMS gateway! ErrorDescription:" + responseDesc);
            status.setResult(ErrorCodes.EXTERNALNOTIFICATION_ERROR_UNDEFINED_ERROR_IN_SMS_GATEWAY.ordinal());
        }
    }

    int readPropertiesFile(String propertiesFileName, Properties prop) {
        try {
            InputStream is = this.getClass().getResourceAsStream(propertiesFileName);
            prop.load(is);
            is.close();
            logger.debug("Successfully loaded Properties from " + propertiesFileName);
        } catch (FileNotFoundException ex1) {
            logger.error("FileNotFound: " + propertiesFileName);
            logger.error(ex1.toString());
            return -1;
        } catch (IOException ex2) {
            logger.error("IOException: " + propertiesFileName);
            logger.error(ex2.toString());
            return -2;
        }
        return 0;
    }

    private boolean hasOnlyDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }

    private boolean hasLetters(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLetter(s.charAt(i))) return true;
        }
        return false;
    }

    private int email2SMS(SMSMessage sms) {
        String emailFromAddress = null;
        List<String> recipient = null;
        String emailSubjectTxt = null;
        String emailMsgTxt = null;
        List<String> to = null;
        String txt = null;
        String id = null;
        String pwd = null;
        String split = null;
        String route = null;
        String from = null;
        String flash = null;
        String status = null;
        recipient.add("smsgate@edev.at");
        if (sms.getText() == null) {
            emailMsgTxt = "";
        } else emailMsgTxt = sms.getText();
        to = sms.getReceipientPhoneNumber();
        id = "ARCSMED";
        pwd = "ARCSMED";
        route = "2";
        from = "436648251044";
        status = "0";
        emailSubjectTxt = to.get(0);
        if (route != null) emailSubjectTxt += ":" + route;
        if (from != null) emailSubjectTxt += ":" + from;
        if (flash != null) emailSubjectTxt += ":" + flash;
        if (status != null) emailSubjectTxt += ":" + status;
        emailSubjectTxt += "@";
        emailSubjectTxt += id + ":" + pwd;
        if (split != null) emailSubjectTxt += "/" + split;
        logger.info("Handywelt subject: " + emailSubjectTxt);
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        Properties props = new Properties();
        props.put("mail.smtp.host", emailGateway.getSERVERHOSTNAME());
        props.put("mail.smtp.auth", "false");
        props.put("mail.debug", "false");
        props.put("mail.smtp.port", emailGateway.getPORT());
        Session session = Session.getDefaultInstance(props, null);
        try {
            javax.mail.Message msg = new MimeMessage(session);
            if (sms.getSenderPhoneNumber() == null) {
                emailFromAddress = emailGateway.getDEFAULTSENDER();
            } else {
                emailFromAddress = sms.getSenderPhoneNumber();
                try {
                    new InternetAddress(emailFromAddress);
                } catch (AddressException adrex) {
                    logger.error("Internet address 'sender' is not valid!");
                    adrex.printStackTrace();
                    emailFromAddress = emailGateway.getDEFAULTSENDER();
                }
            }
            logger.debug("emailFromAddress: " + emailFromAddress);
            InternetAddress addressFrom = new InternetAddress(emailFromAddress);
            msg.setFrom(addressFrom);
            InternetAddress[] addressTo = new InternetAddress[1];
            addressTo[0] = new InternetAddress(recipient.get(0));
            msg.setRecipients(javax.mail.Message.RecipientType.TO, addressTo);
            msg.setSubject(emailSubjectTxt);
            msg.setContent(emailMsgTxt, "text/plain");
            logger.debug("now sending email2SMS");
            Transport.send(msg);
        } catch (AddressException ex) {
            logger.error("Exception Message: " + ex.toString());
            return -1;
        } catch (MessagingException ex) {
            logger.error("Exception Message: " + ex.toString());
            return -2;
        }
        logger.info("Email2SMS successfully sent to " + recipient);
        return 0;
    }
}
