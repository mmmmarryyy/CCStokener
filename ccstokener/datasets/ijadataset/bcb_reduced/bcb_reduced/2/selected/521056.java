package ru.adv.util.mail;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import ru.adv.util.Arguments;
import ru.adv.util.InputOutput;
import ru.adv.util.XmlUtils;
import ru.adv.xml.parser.Parser;
import ru.adv.xml.parser.XmlDoc;
import ru.adv.xml.verifier.Verifier;

/**
 * Рассылка e-mail по указанному файлу xml
 * @version $Revision: 1.5 $
 */
public class SendMail {

    private static final String[] ARGS_OPTIONS = { "u", "s", "d" };

    private static final String USAGE = "" + "Usage: sendmail -u url [-s 1000] [-d]\n\n" + "\tOptions are:\n\n" + "\t-u <url> URL of config file\n" + "\t-s <millisec> delay between send messages\n" + "\t-d <1|0> set debug mode\n" + "\t-e print example of config file\n";

    private static final String TESTCONFIG = "<?xml version=\"1.0\" encoding=\"windows-1251\"?>\n" + "<sendmail>\n" + "	<params>\n" + "		<from>testunit@example.org</from>\n" + "		<return-path>support@example.org</return-path>\n" + "		<content-type>text/plain</content-type>\n" + "		<charset>koi8-r</charset>\n" + "		<subject>TestUnit: SendMailTest Засада</subject>\n" + "		<message-separator>==================\n</message-separator>\n" + "	</params>\n" + "	<!-- Messages for send -->\n" + "	<messages>\n" + "		<!-- Message 1 -->\n" + "		<message id=\"1\" ><![CDATA[Hello1]]></message>\n" + "		<!-- Message 2 -->\n" + "    <message id=\"2\">Привет  &amp;Засада&amp;</message>\n" + "    <!-- Message 3 -->\n" + "    <message id=\"3\">Hello3</message>\n" + "	</messages>\n" + "	<addresses>\n" + "		<address email=\"vic@example.org\">\n" + "			<add-message id=\"1\"/>\n" + "			<add-message id=\"2\"/>\n" + "		</address>\n" + "		<address email=\"igor@example.org\">\n" + "			<add-message id=\"1\"/>\n" + "			<add-message id=\"3\"/>\n" + "		</address>\n" + "		<address email=\"vic@example.org\">\n" + "			<add-message id=\"1\"/>\n" + "			<add-message id=\"2\"/>\n" + "			<add-message id=\"3\"/>\n" + "		</address>\n" + "	</addresses>\n" + "</sendmail>\n";

    public static String SCHEMA = "resource://ru/adv/util/mail/sendmail.xsd";

    private Document config;

    private String from = null;

    private String returnPath = null;

    private String contentType = null;

    private String subject = null;

    private String messageSep = null;

    private String charset = "windows-1251";

    private int delay = 1000;

    private boolean debug = false;

    private Map<String, String> messages = new HashMap<String, String>();

    /**
	 * Конструктор
	 */
    public SendMail() {
        config = null;
    }

    /**
	 * Установить паузу в милисекундах между посылками писем,
	 * по дефолту 1000 мсек = 1 сек
	 * 
	 */
    public void setDelay(int delay) {
        this.delay = delay;
    }

    /**
	 * пауза в милисекундах между посылками писем
	 */
    public int getDelay() {
        return this.delay;
    }

    /**
	 * Зачитавет конфигфайл из по URI
	 *
	 */
    public void readConfig(String urlString) throws Exception {
        try {
            URL url = new URL(urlString);
            InputStream in = url.openStream();
            XmlDoc xml = new Parser().parse(new InputSource(in), true, true);
            Verifier v = new Verifier(InputOutput.create(SCHEMA), null);
            v.verify(xml.getDocument());
            this.config = xml.getDocument();
        } catch (Exception e) {
            log("Can't read " + urlString + ": " + e.toString());
            throw e;
        }
        initParms();
        log("Got parameters: \n" + paramsInfo());
        initMessages();
        log("Got messages: \n" + messagesInfo());
        checkMessageId();
    }

    /**
	 * зачитывает параметры писем
	 */
    private void initParms() throws Exception {
        Element elem = null;
        elem = (Element) XmlUtils.selectSingleNode(config, "/sendmail/params/from");
        if (elem != null) {
            from = getElementData(elem);
        }
        elem = (Element) XmlUtils.selectSingleNode(config, "/sendmail/params/return-path");
        if (elem != null) {
            returnPath = getElementData(elem);
        }
        elem = (Element) XmlUtils.selectSingleNode(config, "/sendmail/params/content-type");
        if (elem != null) {
            contentType = getElementData(elem);
        }
        elem = (Element) XmlUtils.selectSingleNode(config, "/sendmail/params/charset");
        if (elem != null) {
            charset = getElementData(elem);
        }
        elem = (Element) XmlUtils.selectSingleNode(config, "/sendmail/params/subject");
        if (elem != null) {
            subject = getElementData(elem);
        }
        elem = (Element) XmlUtils.selectSingleNode(config, "/sendmail/params/message-separator");
        if (elem != null) {
            messageSep = getElementData(elem);
        }
        checkMailAddress(from);
        checkMailAddress(returnPath);
        if (subject == null || subject.length() == 0) {
            throw new Exception("subject is empty");
        }
        if (messageSep == null || messageSep.length() == 0) {
            throw new Exception("message-separator is empty");
        }
        if (!contentType.equals("text/plain") && !contentType.equals("text/html")) {
            throw new Exception("Unknown content-type '" + contentType + "'");
        }
        try {
            new String(new byte[] { (byte) 1, (byte) 20, (byte) 200 }, charset);
        } catch (Exception e) {
            throw new Exception("Bad charset name ='" + charset + "'");
        }
    }

    /**
	 * провекрка правильности email
	 */
    private void checkMailAddress(String email) throws Exception {
        try {
            new InternetAddress(email);
            if (email.indexOf("@") == -1) {
                throw new Exception("must contain '@' symbol");
            }
        } catch (Exception e) {
            throw new Exception("Bad email address '" + email + "': error=" + e.getMessage());
        }
    }

    /**
	 * Выбирает текстовые данные из первого чилдрена елемента
	 */
    private String getElementData(Element elem) throws Exception {
        String content = "";
        Node[] textNodes = XmlUtils.childrenArray(elem);
        for (int i = 0; i < textNodes.length; i++) {
            if (textNodes[i] instanceof CharacterData) {
                content += ((CharacterData) textNodes[i]).getData();
            } else {
                throw new Exception("Element contains non CharacterData Node :" + XmlUtils.elementToString(elem));
            }
        }
        return content;
    }

    /**
	 * Выдает строку с информацией о плученных глобальных параметрах
	 *
	 */
    public String paramsInfo() {
        String info = "";
        info += "from: " + from;
        info += "\nreturn-path: " + returnPath;
        info += "\ncontent-type: " + contentType;
        info += "\ncharset: " + charset;
        info += "\nsubject: " + subject;
        info += "\nmessage-separator: " + messageSep;
        return info;
    }

    /**
	 * инитит messages в приватные переменные
	 */
    private void initMessages() throws Exception {
        NodeList list = XmlUtils.selectNodeList(config, "/sendmail/messages/message");
        for (int i = 0; i < list.getLength(); i++) {
            Element message = (Element) list.item(i);
            String idMessage = message.getAttribute("id");
            if (idMessage == null || idMessage.length() == 0) {
                throw new Exception("Bad id attribute in message element");
            }
            if (this.messages.containsKey(idMessage)) {
                throw new Exception("Messages has non unique id=" + idMessage);
            }
            this.messages.put(idMessage, getElementData(message));
        }
    }

    /**
	 * Выдает строку с информацией о messages
	 *
	 */
    public String messagesInfo() {
        String info = "";
        for (Map.Entry<String, String> entry : this.messages.entrySet()) {
            info += entry.getKey();
            info += ":\n";
            info += entry.getValue();
            info += "\n";
        }
        return info;
    }

    /**
	 * Проверяет соответствие message id для всех адресов
	 */
    private void checkMessageId() throws Exception {
        NodeList list = XmlUtils.selectNodeList(config, "//add-message/@id");
        for (int i = 0; i < list.getLength(); i++) {
            Attr idAttr = (Attr) list.item(i);
            String id = idAttr.getValue();
            if (!this.messages.containsKey(id)) {
                throw new Exception("There are add-massage element with bad id='" + id + "'");
            }
        }
    }

    /**
	 * Message log
	 */
    private static void log(String mess) {
        System.out.println(mess);
        System.out.flush();
    }

    /**
	 * set debug mode 
	 */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
	 * get debug mode 
	 */
    public boolean getDebug() {
        return this.debug;
    }

    /**
	 * Send e-mail
	 */
    public void send() throws Exception {
        Properties mailProps = new Properties();
        mailProps.put("mail.transport.protocol", "smtp");
        mailProps.put("mail.smtp.host", "localhost");
        mailProps.put("mail.smtp.from", this.returnPath);
        javax.mail.Session session = javax.mail.Session.getDefaultInstance(mailProps);
        session.setDebug(getDebug());
        NodeList list = XmlUtils.selectNodeList(config, "//addresses/address");
        log("Found " + list.getLength() + " addresses");
        for (int i = 0; i < list.getLength(); i++) {
            Element addressElement = (Element) list.item(i);
            log("Prepare mail for " + addressElement.getAttribute("email"));
            try {
                checkMailAddress(addressElement.getAttribute("email"));
                MimeMessage message = createMailMessage(session, addressElement);
                try {
                    Thread.sleep(getDelay());
                } catch (Exception e) {
                }
                log("Seng (" + (i + 1) + "/" + list.getLength() + ") mail to " + addressElement.getAttribute("email"));
                Transport.send(message);
            } catch (Exception e) {
                log("Error mail to " + addressElement.getAttribute("email") + ": " + e.getMessage());
            }
        }
    }

    /**
	 * Формирование e-mail
	 */
    private MimeMessage createMailMessage(Session session, Element addressElement) throws Exception {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(this.from));
        message.setHeader("Return-Path", this.returnPath);
        message.setSubject(this.subject, this.charset);
        message.setSentDate(new Date());
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(addressElement.getAttribute("email")));
        MimeBodyPart part1 = new MimeBodyPart();
        part1.setContent(makeBody(addressElement), this.contentType + "; charset=" + this.charset);
        MimeMultipart mp = new MimeMultipart();
        mp.addBodyPart(part1);
        message.setContent(mp);
        return message;
    }

    /**
	 * Создает тело сообщения
	 */
    private String makeBody(Element addressElement) throws Exception {
        String body = "";
        NodeList ids = XmlUtils.selectNodeList(addressElement, "./add-message/@id");
        for (int i = 0; i < ids.getLength(); i++) {
            String attr = ((Attr) ids.item(i)).getValue();
            body += messages.get(attr);
            if (i < ids.getLength() - 1) {
                body += messageSep;
            }
        }
        return body;
    }

    /**
	 * Запуск с коммандной строки
	 */
    public static void main(String params[]) {
        SendMail sm = new SendMail();
        Arguments args = new Arguments(USAGE);
        String url = null;
        try {
            args.parse(params, ARGS_OPTIONS);
            if (args.getNamedOption("e") != null) {
                SendMail.log(SendMail.TESTCONFIG);
                System.exit(0);
            }
            url = args.getNamedOption("u");
            sm.setDebug(args.getNamedOption("d") != null);
            final String strDelay = args.getNamedOption("s");
            if (strDelay != null) {
                sm.setDelay(Integer.parseInt(strDelay));
            }
            if (url == null) {
                args.printHelpMessage();
                System.exit(-1);
            }
        } catch (Exception e) {
            SendMail.log("" + e.getMessage());
            args.printHelpMessage();
            System.exit(-1);
        }
        try {
            sm.readConfig(url);
            sm.send();
        } catch (Exception e) {
            SendMail.log("Error: " + e.getMessage());
        }
    }
}
