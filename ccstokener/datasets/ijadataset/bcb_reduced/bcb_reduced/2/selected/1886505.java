package EVEOverWatch;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author alexanagnos
 */
public class EVEXMLFetch {

    private static final String skillTree = "http://api.eve-online.com/eve/SkillTree.xml.aspx";

    private static final String refTypes = "http://api.eve-online.com/eve/RefTypes.xml.aspx";

    private static final String characterSheet = "http://api.eve-online.com/char/CharacterSheet.xml.aspx";

    private static final String sovereignty = "http://api.eve-online.com/map/Sovereignty.xml.aspx";

    private static final String skillInTraining = "http://api.eve-online.com/char/SkillInTraining.xml.aspx";

    private static final String charWalletJournal = "http://api.eve-online.com/char/WalletJournal.xml.aspx";

    private static final String corpWalletJournal = "http://api.eve-online.com/chorp/WalletJournal.xml.aspx";

    private static final String corpWalletTransactions = "http://api.eve-online.com/corp/WalletTransactions.xml.aspx";

    private static final String charWalletTransactions = "http://api.eve-online.com/corp/WalletTransactions.xml.aspx";

    private static final String charAccountBalance = "http://api.eve-online.com/char/AccountBalance.xml.aspx";

    private static final String corpAccountBalance = "http://api.eve-online.com/corp/AccountBalance.xml.aspx";

    private static final String corpMemberTracking = "http://api.eve-online.com/corp/MemberTracking.xml.aspx";

    private EVEXMLFetch() {
    }

    private static Document getXML(String methodParms, String whichXML) throws Exception {
        System.out.println("Receaved request to fetch " + whichXML);
        DocumentBuilder builder = null;
        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        URL url = new URL(whichXML);
        URLConnection connection = url.openConnection();
        ((HttpURLConnection) connection).setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        System.out.println("Got connection to " + whichXML);
        OutputStreamWriter outStream = new OutputStreamWriter(connection.getOutputStream());
        PrintWriter output = new PrintWriter(outStream);
        output.write(methodParms);
        output.flush();
        output.close();
        outStream.close();
        System.out.println("Wrote request to " + whichXML);
        StringBuilder sb = new StringBuilder();
        String buf = "";
        BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        System.out.println("Reading XML from site");
        while ((buf = input.readLine()) != null) sb.append(buf).append("\n");
        buf = sb.toString();
        if (buf.contains("<error code=\"")) throw new Exception("EVE XML returned an error.\n" + buf);
        return builder.parse(new ByteArrayInputStream(buf.getBytes("UTF-8")));
    }

    public static Document getSkillTree() throws Exception {
        return getXML("", skillTree);
    }

    public static Document getRefTypes() throws Exception {
        return getXML("", refTypes);
    }

    public static Document getSovereignty() throws Exception {
        return getXML("", sovereignty);
    }

    private static String ripSingleElementFromXML(Document d, String name) {
        NodeList list = d.getElementsByTagName(name);
        if (list.getLength() == 0) {
            Prefs.Error("Failed to find: <" + name + ">");
            return "";
        }
        return list.item(0).getFirstChild().getNodeValue();
    }

    public static Character getCharacterSheet(String APIKey, String UserID, String CharacterID) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(URLEncoder.encode("userID", "UTF-8")).append("=").append(URLEncoder.encode(UserID, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("apiKey", "UTF-8")).append("=").append(URLEncoder.encode(APIKey, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("characterID", "UTF-8")).append("=").append(URLEncoder.encode(CharacterID, "UTF-8"));
        Document xml = getXML(sb.toString(), characterSheet);
        String name = ripSingleElementFromXML(xml, "name");
        String race = ripSingleElementFromXML(xml, "race");
        String bloodLine = ripSingleElementFromXML(xml, "bloodLine");
        String corporationName = ripSingleElementFromXML(xml, "corporationName");
        String gender = ripSingleElementFromXML(xml, "gender");
        String corporationID = ripSingleElementFromXML(xml, "corporationID");
        String balance = ripSingleElementFromXML(xml, "balance");
        ArrayList<AttributeEnhancer> attrEnhancers = new ArrayList<AttributeEnhancer>();
        Node node = xml.getElementsByTagName("attributeEnhancers").item(0).getFirstChild();
        while (node != null) {
            String nodeName = node.getNodeName();
            if (nodeName.equals("#text")) {
                node = node.getNextSibling();
                continue;
            }
            String attribute = nodeName.substring(0, nodeName.indexOf("Bonus"));
            Node node2 = node.getFirstChild();
            String argName = null;
            int argValue = -1;
            while (node2 != null) {
                if (node2.getNodeName().equals("augmentatorName")) {
                    argName = node2.getFirstChild().getNodeValue();
                } else if (node2.getNodeName().equals("augmentatorValue")) {
                    argValue = Integer.parseInt(node2.getFirstChild().getNodeValue());
                }
                node2 = node2.getNextSibling();
            }
            attrEnhancers.add(new AttributeEnhancer(argName, argValue, attribute));
            node = node.getNextSibling();
        }
        System.out.println("Got past first.");
        ArrayList<Attribute> attributes = new ArrayList<Attribute>();
        node = xml.getElementsByTagName("attributes").item(0).getFirstChild();
        while (node != null) {
            String nodeName = node.getNodeName();
            if (nodeName.equals("#text")) {
                node = node.getNextSibling();
                continue;
            }
            int nodeValue = Integer.parseInt(node.getFirstChild().getNodeValue());
            attributes.add(new Attribute(nodeName, nodeValue));
            node = node.getNextSibling();
        }
        System.out.println("Got past second.");
        ArrayList<Skill> skills = new ArrayList<Skill>();
        node = xml.getElementsByTagName("rowset").item(0).getFirstChild();
        Tables table = Tables.getInstance();
        while (node != null) {
            NamedNodeMap m = node.getAttributes();
            if (m == null) {
                node = node.getNextSibling();
                continue;
            }
            int typeID = Integer.parseInt(m.getNamedItem("typeID").getNodeValue());
            int level = Integer.parseInt(m.getNamedItem("level").getNodeValue());
            int sp = Integer.parseInt(m.getNamedItem("skillpoints").getNodeValue());
            String skillName = table.lookUpInTable(typeID);
            skills.add(new Skill(typeID, level, skillName, sp));
            node = node.getNextSibling();
        }
        System.out.println("Got past third.");
        return new Character(name, UserID, APIKey, CharacterID, race, bloodLine, gender, corporationName, corporationID, balance, skills, attrEnhancers, attributes);
    }

    public static void writeDocument(Document d, PrintStream p) {
        TransformerFactory tf = TransformerFactory.newInstance();
        tf.setAttribute("indent-number", new Integer(4));
        Transformer t = null;
        try {
            t = tf.newTransformer();
        } catch (TransformerConfigurationException ex) {
            ex.printStackTrace();
        }
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.setOutputProperty(OutputKeys.MEDIA_TYPE, "text/xml");
        try {
            t.transform(new DOMSource(d), new StreamResult(new OutputStreamWriter(p)));
        } catch (TransformerException ex) {
            ex.printStackTrace();
        }
    }

    public static TrainingSkill getSkillInTraining(String APIKey, String UserID, String CharacterID) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(URLEncoder.encode("userID", "UTF-8")).append("=").append(URLEncoder.encode(UserID, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("apiKey", "UTF-8")).append("=").append(URLEncoder.encode(APIKey, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("characterID", "UTF-8")).append("=").append(URLEncoder.encode(CharacterID, "UTF-8"));
        Document xml = getXML(sb.toString(), skillInTraining);
        if (xml == null) {
            Prefs.Error("Failed to get XML, or Document otherwise null");
            throw new Exception("Failed to get XML, or Document otherwise null");
        }
        NodeList tmpList = xml.getElementsByTagName("skillInTraining");
        if (tmpList == null || tmpList.getLength() == 0) {
            xml = null;
            throw new Exception("No Skill Training");
        }
        int numSkills = Short.parseShort(tmpList.item(0).getFirstChild().getNodeValue());
        if (numSkills == 0) {
            throw new Exception("No Skill Training");
        }
        int skillID = Integer.parseInt(ripSingleElementFromXML(xml, "trainingTypeID"));
        int destLevel = Short.parseShort(ripSingleElementFromXML(xml, "trainingToLevel"));
        int currentLevel = destLevel - 1;
        String skillName = Tables.getInstance().lookUpInTable(skillID);
        int destSkillPoints = Integer.parseInt(ripSingleElementFromXML(xml, "trainingDestinationSP"));
        int startSkillPoints = Integer.parseInt(ripSingleElementFromXML(xml, "trainingStartSP"));
        String tmpD;
        tmpD = ripSingleElementFromXML(xml, "cachedUntil");
        GregorianCalendar goodTill = getCalandarObject(tmpD);
        tmpD = ripSingleElementFromXML(xml, "trainingEndTime");
        GregorianCalendar endTime = getCalandarObject(tmpD);
        tmpD = ripSingleElementFromXML(xml, "trainingStartTime");
        GregorianCalendar startTime = getCalandarObject(tmpD);
        return new TrainingSkill(endTime, destLevel, startTime, startSkillPoints, destSkillPoints, skillID, currentLevel, skillName, goodTill);
    }

    public static Document getCharWalletJournal(String APIKey, String UserID, String CharacterID, String beforeRefID) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(URLEncoder.encode("userID", "UTF-8")).append("=").append(URLEncoder.encode(UserID, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("apiKey", "UTF-8")).append("=").append(URLEncoder.encode(APIKey, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("characterID", "UTF-8")).append("=").append(URLEncoder.encode(CharacterID, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("beforeRefID", "UTF-8")).append("=").append(URLEncoder.encode(beforeRefID, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("accountKey", "UTF-8")).append("=").append(URLEncoder.encode("1000", "UTF-8"));
        return getXML(sb.toString(), charWalletJournal);
    }

    public static Document getCorpWalletJournal(String APIKey, String UserID, String CharacterID, String beforeRefID, String accountKey) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(URLEncoder.encode("userID", "UTF-8")).append("=").append(URLEncoder.encode(UserID, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("apiKey", "UTF-8")).append("=").append(URLEncoder.encode(APIKey, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("characterID", "UTF-8")).append("=").append(URLEncoder.encode(CharacterID, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("beforeRefID", "UTF-8")).append("=").append(URLEncoder.encode(beforeRefID, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("accountKey", "UTF-8")).append("=").append(URLEncoder.encode(accountKey, "UTF-8"));
        return getXML(sb.toString(), corpWalletJournal);
    }

    public static Document getCharWalletTransactions(String APIKey, String UserID, String CharacterID, String beforeTransID) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(URLEncoder.encode("userID", "UTF-8")).append("=").append(URLEncoder.encode(UserID, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("apiKey", "UTF-8")).append("=").append(URLEncoder.encode(APIKey, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("characterID", "UTF-8")).append("=").append(URLEncoder.encode(CharacterID, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("beforeRefID", "UTF-8")).append("=").append(URLEncoder.encode(beforeTransID, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("accountKey", "UTF-8")).append("=").append(URLEncoder.encode("1000", "UTF-8"));
        return getXML(sb.toString(), charWalletTransactions);
    }

    public static Document getCorpWalletTransactions(String APIKey, String UserID, String CharacterID, String beforeTransID, String accountKey) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(URLEncoder.encode("userID", "UTF-8")).append("=").append(URLEncoder.encode(UserID, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("apiKey", "UTF-8")).append("=").append(URLEncoder.encode(APIKey, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("characterID", "UTF-8")).append("=").append(URLEncoder.encode(CharacterID, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("beforeRefID", "UTF-8")).append("=").append(URLEncoder.encode(beforeTransID, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("accountKey", "UTF-8")).append("=").append(URLEncoder.encode(accountKey, "UTF-8"));
        return getXML(sb.toString(), corpWalletTransactions);
    }

    public static Document getCorpAccountBalance(String APIKey, String UserID, String CharacterID) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(URLEncoder.encode("userID", "UTF-8")).append("=").append(URLEncoder.encode(UserID, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("apiKey", "UTF-8")).append("=").append(URLEncoder.encode(APIKey, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("characterID", "UTF-8")).append("=").append(URLEncoder.encode(CharacterID, "UTF-8"));
        return getXML(sb.toString(), corpAccountBalance);
    }

    public static Document getcharAccountBalance(String APIKey, String UserID, String CharacterID) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(URLEncoder.encode("userID", "UTF-8")).append("=").append(URLEncoder.encode(UserID, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("apiKey", "UTF-8")).append("=").append(URLEncoder.encode(APIKey, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("characterID", "UTF-8")).append("=").append(URLEncoder.encode(CharacterID, "UTF-8"));
        return getXML(sb.toString(), charAccountBalance);
    }

    public static Document getCorpMemberTracking(String APIKey, String UserID, String CharacterID) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(URLEncoder.encode("userID", "UTF-8")).append("=").append(URLEncoder.encode(UserID, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("apiKey", "UTF-8")).append("=").append(URLEncoder.encode(APIKey, "UTF-8"));
        sb.append("&").append(URLEncoder.encode("characterID", "UTF-8")).append("=").append(URLEncoder.encode(CharacterID, "UTF-8"));
        return getXML(sb.toString(), corpMemberTracking);
    }

    private static GregorianCalendar getCalandarObject(String dateTime) {
        String[] tmp = dateTime.split(" ");
        String[] date = tmp[0].split("-");
        String[] time = tmp[1].split(":");
        String amPM = null;
        if (tmp.length == 3) amPM = tmp[2];
        int year = Integer.parseInt(date[0]);
        int month = Integer.parseInt(date[1]);
        month--;
        int day = Integer.parseInt(date[2]);
        int hour = Integer.parseInt(time[0]);
        if (amPM != null && amPM.equalsIgnoreCase("PM")) hour += 12;
        int min = Integer.parseInt(time[1]);
        int sec = Integer.parseInt(time[2]);
        if (Prefs.DEBUG) System.out.println(year + " " + month + " " + day + " " + hour + ":" + min + ":" + sec);
        GregorianCalendar tmpTime = new GregorianCalendar(year, month, day, hour, min, sec);
        tmpTime.setTimeZone(TimeZone.getTimeZone("GMT"));
        tmpTime.getTimeInMillis();
        tmp = null;
        date = null;
        time = null;
        return tmpTime;
    }
}
