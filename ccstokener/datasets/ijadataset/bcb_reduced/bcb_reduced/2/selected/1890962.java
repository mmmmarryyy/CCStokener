package ogv.util;

import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import ogv.data.Planet;
import ogv.data.controls.Game;
import org.w3c.dom.*;

public class GVNGTransport {

    private static final Logger logger = Logger.getLogger("ogv.GVNGTransport");

    private static final int TIMEOUT = 60000;

    private final String url;

    private final Game game;

    private static final String charset = "windows-1251";

    private static final String BROADCAST = "broadcast";

    private static final String MESSAGE = "message";

    private static final String HYPERCAST = "hypercast";

    private static final String INFO = "info";

    private static final String ATTRIBUTE_FILE = "file";

    private static final String ATTRIBUTE_NUM = "num";

    private static final String ATTRIBUTE_RACE = "race";

    private static final String ATTRIBUTE_TURN = "turn";

    private static final String DEFAULT_ATTRIBUTE_VALUE = "";

    public GVNGTransport(String url, Game game) {
        this.url = url;
        this.game = game;
    }

    public Document sendOrder(List<String> text) throws Exception {
        return sendData("order", game.getOrderHeader(), text);
    }

    public void sendMail(String to, List<String> text) throws Exception {
        String type = "mail";
        String header = "#MAIL " + game.getName() + ' ' + to + ' ' + game.getAuth();
        sendData(type, header, text);
    }

    public void sendMailAnonymously(String to, List<String> text) throws Exception {
        String type = "anonymously";
        String header = "#ANONYMOUSLY " + game.getName() + ' ' + to + ' ' + game.getAuth();
        sendData(type, header, text);
    }

    public void sendBroadcast(List<String> text) throws Exception {
        sendData("mail", "#WALL " + game.getAuth(), text);
    }

    public void sendHypercast(Planet from, Planet to, List<String> text) throws Exception {
        sendData("mail", "#HYPERCAST " + game.getAuth() + ' ' + from.getSafeName() + ' ' + to.getSafeName(), text);
    }

    private Document sendData(String type, String header, List<String> text) throws Exception {
        StringBuilder builder = new StringBuilder(header);
        builder.append("\r\n");
        for (String line : text) builder.append(line).append("\r\n");
        return sendData(type, builder.toString());
    }

    private Document sendData(String type, String body) throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("GAME", game.getName());
        params.put("RACE", game.getYourName());
        params.put("PASSWORD", game.getPassword());
        params.put("TURN", String.valueOf(game.getTurn()));
        params.put("TYPE", type);
        params.put("ID", "120");
        params.put("BODY", body);
        try {
            return doPost(new URL(url + "?send=data"), params);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected static Document doPost(URL url, Map<String, String> params) throws Exception {
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT);
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
            conn.setAllowUserInteraction(false);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);
            OutputStream out = conn.getOutputStream();
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) out.write('&'); else first = false;
                out.write(URLEncoder.encode(entry.getKey(), charset).getBytes("US-ASCII"));
                out.write('=');
                out.write(URLEncoder.encode(entry.getValue(), charset).getBytes("US-ASCII"));
            }
            out.flush();
            out.close();
            conn.connect();
            conn.getResponseCode();
            logger.fine("Request result: " + conn.getResponseMessage());
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                logger.log(Level.WARNING, "Can't send POST request to " + url + ". Response: " + conn.getResponseCode() + ' ' + conn.getResponseMessage());
            }
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(conn.getInputStream());
            conn.disconnect();
            NodeList list = doc.getElementsByTagName("error");
            if (list.getLength() != 0) throw new Exception(list.item(0).getTextContent());
            return doc;
        } catch (Exception err) {
            logger.log(Level.WARNING, "Can't send POST request to " + url, err);
            throw err;
        }
    }

    /**
	 * Load report from server by selected turn.
	 *
	 * @param turn turn.
	 * @return report reader.
	 */
    public Reader loadReport(int turn) throws Exception {
        Document doc = loadReportSendRequest(turn);
        NodeList list = doc.getElementsByTagName("file");
        if (list.getLength() == 0) return new StringReader("");
        Element file = (Element) list.item(0);
        file.getAttribute("encoding");
        String encoding = file.getAttribute("encoding");
        if (!"text".equals(encoding)) throw new Exception("Unsupported encoding: " + encoding);
        return new StringReader(file.getTextContent());
    }

    /**
	 * Create and send request to server for loading report by selected turn.
	 *
	 * @param turn selected turn.
	 * @return xml document.
	 * @throws Exception exception.
	 */
    protected Document loadReportSendRequest(int turn) throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("GAME", game.getName());
        params.put("RACE", game.getYourName());
        params.put("PASSWORD", game.getPassword());
        params.put("TURN", String.valueOf(turn));
        return doPost(new URL(url + "?get=report"), params);
    }

    /**
	 * Load context (all messages) from server.
	 *
	 * @return conext object.
	 * @throws Exception exception.
	 */
    public Context loadContext() throws Exception {
        Document document = loadContextSendRequest();
        int turn = parseTurn(document);
        List<Message> broadcasts = parseHeaders(document, BROADCAST);
        List<Message> messages = parseHeaders(document, MESSAGE);
        List<Message> hypercasts = parseHeaders(document, HYPERCAST);
        return new Context(turn, broadcasts, messages, hypercasts);
    }

    protected int parseTurn(Document document) {
        NodeList nodeList = document.getElementsByTagName(INFO);
        if (nodeList == null || nodeList.getLength() == 0) {
            return 0;
        }
        Node node = nodeList.item(0);
        NamedNodeMap attributes = node.getAttributes();
        String turn = getAttribute(attributes, ATTRIBUTE_TURN);
        return Integer.parseInt(turn);
    }

    /**
	 * Parse 'headers' (all information expept body) for all messages from Document.
	 *
	 * @param document document.
	 * @param tag	  ATTRIBUTE_FILE | ATTRIBUTE_NUM | ATTRIBUTE_RACE | ATTRIBUTE_TURN.
	 * @return List of messages.
	 */
    protected List<Message> parseHeaders(Document document, String tag) {
        List<Message> list = new ArrayList<Message>();
        NodeList nodeList = document.getElementsByTagName(tag);
        if (nodeList == null || nodeList.getLength() == 0) {
            return list;
        }
        for (int index = 0; index < nodeList.getLength(); index++) {
            Node node = nodeList.item(index);
            NamedNodeMap attributes = node.getAttributes();
            String fileId = getAttribute(attributes, ATTRIBUTE_FILE);
            String num = getAttribute(attributes, ATTRIBUTE_NUM);
            String race = getAttribute(attributes, ATTRIBUTE_RACE);
            String turn = getAttribute(attributes, ATTRIBUTE_TURN);
            Message message = new Message();
            message.setFileId(fileId);
            message.setNum(num);
            message.setTurn(turn);
            if ("".equals(race)) {
                message.setFrom("<anonymous>");
            } else {
                message.setFrom(race);
            }
            list.add(message);
        }
        return list;
    }

    /**
	 * Get attribute from attributes map.
	 *
	 * @param attributes map of attributes.
	 * @param attribute  attribute.
	 * @return value of attribute.
	 */
    private static String getAttribute(NamedNodeMap attributes, String attribute) {
        if (attributes == null) {
            return DEFAULT_ATTRIBUTE_VALUE;
        }
        Node node = attributes.getNamedItem(attribute);
        if (node == null) {
            return DEFAULT_ATTRIBUTE_VALUE;
        }
        return node.getNodeValue();
    }

    /**
	 * Create and send request for loading context from server.
	 *
	 * @return document.
	 * @throws Exception exception.
	 */
    protected Document loadContextSendRequest() throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("GAME", game.getName());
        params.put("RACE", game.getYourName());
        params.put("PASSWORD", game.getPassword());
        return doPost(new URL(url + "?get=context"), params);
    }

    /**
	 * Load messages (bodies) for selected file ids.
	 *
	 * @param fileIds file ids.
	 * @return map of bodies.
	 * @throws Exception exception.
	 */
    public Map<String, String> loadMessage(String fileIds) throws Exception {
        Document document = loadMessageSendRequest(fileIds);
        Map<String, String> result = new HashMap<String, String>();
        NodeList nodeList = document.getElementsByTagName("file");
        if (nodeList == null || nodeList.getLength() == 0) {
            return result;
        }
        for (int index = 0; index < nodeList.getLength(); index++) {
            Node node = nodeList.item(index);
            NamedNodeMap attributes = node.getAttributes();
            String fileId = getAttribute(attributes, "name");
            String messageBody = node.getFirstChild().getNodeValue();
            result.put(fileId, messageBody);
        }
        return result;
    }

    /**
	 * Create and send request for loading message bodies from server by selected file ids.
	 *
	 * @param fileId selected file ids.
	 * @return document.
	 * @throws Exception exception.
	 */
    protected Document loadMessageSendRequest(String fileId) throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("GAME", game.getName());
        params.put("RACE", game.getYourName());
        params.put("PASSWORD", game.getPassword());
        params.put("ID", fileId);
        return doPost(new URL(url + "?get=message"), params);
    }
}
