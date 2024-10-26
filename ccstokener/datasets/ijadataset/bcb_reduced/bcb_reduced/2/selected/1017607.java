package ru.adv.test.xml.newt;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;
import org.junit.Assert;
import org.springframework.util.FileCopyUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ru.adv.db.Database;
import ru.adv.db.SingleDBConnection;
import ru.adv.db.app.packet.Import;
import ru.adv.db.app.packet.PacketConfig;
import ru.adv.db.handler.HandlerI;
import ru.adv.http.InvalidatedSessionException;
import ru.adv.test.AbstractOldDBTestCase;
import ru.adv.test.TestDatabase;
import ru.adv.util.DOMBeanSerializer;
import ru.adv.util.ErrorCodeException;
import ru.adv.util.Strings;
import ru.adv.util.XmlUtils;
import ru.adv.util.matrix.Matrix;
import ru.adv.util.matrix.Row;
import ru.adv.xml.newt.TManagerLogger;
import ru.adv.xml.parser.Parser;

public class TManagerLoggerTest extends AbstractOldDBTestCase {

    static final String DBNAME_DEFAULT = "DBNAME_DEFAULT";

    public void doTest(TestDatabase database) throws Exception {
        checkUniqueGroups(database);
        checkRequired(database);
        checkForeign(database);
        checkUniqueness(database);
        checkColumnIndex(database);
        checkSearch(database);
        checkNoAllSaving(database);
    }

    private static final String PC1;

    private static final String LOG1;

    private static final String PC2;

    private static final String LOG2;

    private static final String PC3;

    private static final String LOG3;

    private static final String PC4;

    private static final String LOG4;

    private static final String PC5;

    private static final String LOG5;

    private static final String PC6;

    private static final String LOG6;

    static {
        try {
            PC1 = readResource(TManagerLoggerTest.class.getResource("test/tmanager/pc1.xml"));
            LOG1 = readResource(TManagerLoggerTest.class.getResource("test/tmanager/log1.xml"));
            PC2 = readResource(TManagerLoggerTest.class.getResource("test/tmanager/pc2.xml"));
            LOG2 = readResource(TManagerLoggerTest.class.getResource("test/tmanager/log2.xml"));
            PC3 = readResource(TManagerLoggerTest.class.getResource("test/tmanager/pc3.xml"));
            LOG3 = readResource(TManagerLoggerTest.class.getResource("test/tmanager/log3.xml"));
            PC4 = readResource(TManagerLoggerTest.class.getResource("test/tmanager/pc4.xml"));
            LOG4 = readResource(TManagerLoggerTest.class.getResource("test/tmanager/log4.xml"));
            PC5 = readResource(TManagerLoggerTest.class.getResource("test/tmanager/pc5.xml"));
            LOG5 = readResource(TManagerLoggerTest.class.getResource("test/tmanager/log5.xml"));
            PC6 = readResource(TManagerLoggerTest.class.getResource("test/tmanager/pc6.xml"));
            LOG6 = readResource(TManagerLoggerTest.class.getResource("test/tmanager/log6.xml"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void checkUniqueGroups(Database database) throws Exception {
        PacketConfig packetConfig = PacketConfig.create(database.getDBConfig(), parseElement(PC1));
        Matrix matrix = new Matrix();
        matrix.addRow(new Row("1", "2", "3", "4", "5"));
        TManagerLogger logger = new TManagerLogger("tmp", false, packetConfig.getCs(), packetConfig.getRs(), packetConfig.getQuote(), "windows-1251");
        Import imp = new Import(database.getHandler(), getSecurityOptions(), packetConfig, null, logger);
        imp.setSaveAll(false);
        Assert.assertTrue("save() must return false", !imp.save(matrix, Arrays.asList(new String[] { "q" })));
        assertEquals(parseElement(LOG1), getLogElement(logger, false));
    }

    private void checkRequired(Database database) throws Exception {
        PacketConfig packetConfig = PacketConfig.create(database.getDBConfig(), parseElement(PC2));
        Matrix matrix = new Matrix();
        matrix.addRow(new Row("1", "2", "2", "4", "4", " ", "1"));
        TManagerLogger logger = new TManagerLogger("tmp", false, packetConfig.getCs(), packetConfig.getRs(), packetConfig.getQuote(), "windows-1251");
        Import imp = new Import(database.getHandler(), getSecurityOptions(), packetConfig, null, logger);
        imp.setSaveAll(true);
        this.logger.fatal("database.getDBConfig().getDBAdapterName()=" + database.getDBConfig().getDBAdapterName());
        this.logger.fatal("database.getDBConfig().getId()=" + database.getDBConfig().getId());
        this.logger.fatal("imp=" + imp);
        this.logger.fatal("matrix=" + matrix);
        Assert.assertTrue("[" + database.getDBConfig().getDBAdapterName() + "]: Must save", imp.save(matrix, Arrays.asList(new String[] { "q" })));
        matrix = new Matrix();
        matrix.addRow(new Row("1", "2", "2", "4", "4", " ", "0"));
        logger = new TManagerLogger("tmp", false, packetConfig.getCs(), packetConfig.getRs(), packetConfig.getQuote(), "windows-1251");
        imp = new Import(database.getHandler(), getSecurityOptions(), packetConfig, null, logger);
        imp.setSaveAll(false);
        Assert.assertTrue("[" + database.getDBConfig().getDBAdapterName() + "]: save() must return false", !imp.save(matrix, Arrays.asList(new String[] { "q" })));
        assertEquals("[" + database.getDBConfig().getDBAdapterName() + "]", parseElement(LOG2), getLogElement(logger, false));
    }

    private void checkForeign(Database database) throws ErrorCodeException, XPathExpressionException {
        if (!database.getDBConfig().getDBAdapter().supportsAdditionalInfoExtraction()) {
            return;
        }
        PacketConfig packetConfig = PacketConfig.create(database.getDBConfig(), parseElement(PC3));
        Matrix matrix = new Matrix();
        Row row = new Row();
        row.add("r1");
        row.add("15");
        matrix.addRow(row);
        TManagerLogger logger = new TManagerLogger("tmp", false, packetConfig.getCs(), packetConfig.getRs(), packetConfig.getQuote(), "windows-1251");
        Import imp = new Import(database.getHandler(), getSecurityOptions(), packetConfig, null, logger);
        imp.setSaveAll(false);
        imp.save(matrix, Arrays.asList(new String[] { "q", "r" }));
        assertEquals(parseElement(LOG3), getLogElement(logger, false));
    }

    private void checkUniqueness(Database database) throws ErrorCodeException, XPathExpressionException {
        if (!database.getDBConfig().getDBAdapter().supportsAdditionalInfoExtraction()) {
            return;
        }
        PacketConfig packetConfig = PacketConfig.create(database.getDBConfig(), parseElement(PC4));
        Matrix matrix = new Matrix();
        Row row = new Row("1", "2", "3", "4", "5", "6", "1", "2", "3", "4", "5", "6", "s", "1");
        matrix.addRow(row);
        Import imp = new Import(database.getHandler(), getSecurityOptions(), packetConfig, null, null);
        imp.setSaveAll(true);
        Assert.assertTrue(imp.save(matrix, Arrays.asList(new String[] { "q" })));
        matrix = new Matrix();
        row = new Row("11", "21", "31", "4", "5", "6", "1", "2", "3", "4", "5", "6", "s2", "1");
        matrix.addRow(row);
        TManagerLogger logger = new TManagerLogger("tmp", false, packetConfig.getCs(), packetConfig.getRs(), packetConfig.getQuote(), "windows-1251");
        imp = new Import(database.getHandler(), getSecurityOptions(), packetConfig, null, logger);
        imp.setSaveAll(false);
        Assert.assertTrue(!imp.save(matrix, Arrays.asList(new String[] { "q" })));
        assertEquals(parseElement(LOG4), getLogElement(logger, true));
    }

    private Element getLogElement(TManagerLogger tManagerLogger, boolean removeZeroCodes) throws InvalidatedSessionException, XPathExpressionException {
        Element result = tManagerLogger.createDomElement(Parser.createEmptyDocument(), 1);
        if (removeZeroCodes) {
            removeZeroErrorCodes(result);
        }
        cleanStartFinishElement(result);
        removeRowsFileAttribute(result);
        removeStackTrace(result);
        removeOracleInstanceName(result);
        return result;
    }

    private void cleanStartFinishElement(Element result) {
        try {
            Node node = XmlUtils.selectSingleNode(result, "log/start");
            XmlUtils.removeAllChildren(node);
            node = XmlUtils.selectSingleNode(result, "log/finish");
            XmlUtils.removeAllChildren(node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void removeZeroErrorCodes(Element result) throws XPathExpressionException {
        NodeList list = XmlUtils.selectNodeList(result, "//error[@code='0']");
        for (int i = 0; i < list.getLength(); ++i) {
            Node node = list.item(i);
            node.getParentNode().removeChild(node);
        }
    }

    private void removeOracleInstanceName(Element result) throws XPathExpressionException {
        NodeList list = XmlUtils.selectNodeList(result, "//param[@name='database']");
        for (int i = 0; i < list.getLength(); ++i) {
            Element node = (Element) list.item(i);
            String value = node.getAttribute("value");
            value = (String) Strings.split(value, "@").get(0);
            node.setAttribute("value", value);
        }
    }

    private void removeStackTrace(Element result) throws XPathExpressionException {
        NodeList list = XmlUtils.selectNodeList(result, "//stack-trace");
        for (int i = 0; i < list.getLength(); ++i) {
            Node node = list.item(i);
            node.getParentNode().removeChild(node);
        }
    }

    public Element parseElement(String xml) {
        String xmlCurrent = xml.replaceAll(DBNAME_DEFAULT, getDBName());
        Element result = super.parseElement(xmlCurrent);
        removeRowsFileAttribute(result);
        cleanStartFinishElement(result);
        return result;
    }

    private Element removeRowsFileAttribute(Element element) {
        NodeList list = element.getChildNodes();
        for (int i = 0; i < list.getLength(); ++i) {
            Node node = list.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && ((Element) node).getTagName().equals("log")) {
                ((Element) node).removeAttribute("rows-file");
            }
        }
        return element;
    }

    private void checkColumnIndex(Database database) throws ErrorCodeException, XPathExpressionException {
        PacketConfig packetConfig = PacketConfig.create(database.getDBConfig(), parseElement(PC5));
        Matrix matrix = new Matrix();
        Row row = new Row();
        row.add("r1");
        matrix.addRow(row);
        TManagerLogger logger = new TManagerLogger("tmp", false, packetConfig.getCs(), packetConfig.getRs(), packetConfig.getQuote(), "windows-1251");
        Import imp = new Import(database.getHandler(), getSecurityOptions(), packetConfig, null, logger);
        imp.setSaveAll(false);
        imp.save(matrix, Arrays.asList(new String[] { "q", "r" }));
        assertEquals(parseElement(LOG5), getLogElement(logger, false));
    }

    private void checkSearch(Database database) throws ErrorCodeException, XPathExpressionException {
        PacketConfig packetConfig = PacketConfig.create(database.getDBConfig(), parseElement(PC6));
        Matrix matrix = new Matrix();
        matrix.addRow(new Row("11", "22", "22", "44", "44", "  ", "1"));
        TManagerLogger logger = new TManagerLogger("tmp", false, packetConfig.getCs(), packetConfig.getRs(), packetConfig.getQuote(), "windows-1251");
        Import imp = new Import(database.getHandler(), getSecurityOptions(), packetConfig, null, logger);
        imp.setSaveAll(false);
        imp.save(matrix, Arrays.asList(new String[] { "q" }));
        assertEquals(parseElement(LOG6), getLogElement(logger, false));
    }

    private void checkNoAllSaving(Database database) throws Exception {
        executeUpdate("delete from q");
        PacketConfig packetConfig = PacketConfig.create(database.getDBConfig(), parseElement(PC2));
        Matrix matrix = new Matrix();
        matrix.addRow(new Row("1", "2", "2", "3", "4", " ", "1"));
        matrix.addRow(new Row("1", "2", "2", "4", "4", " ", "0"));
        matrix.addRow(new Row("1", "2", "2", "5", "4", " ", "1"));
        matrix.addRow(new Row("1", "2", "2", "6", "4", " ", "1"));
        matrix.addRow(new Row("1", "2", "2", "7", "4", " ", "1"));
        matrix.addRow(new Row("1", "2", "2", "8", "4", " ", "1"));
        matrix.addRow(new Row("1", "2", "2", "9", "4", " ", "1"));
        TManagerLogger tmLogger = new TManagerLogger("tmp", false, packetConfig.getCs(), packetConfig.getRs(), packetConfig.getQuote(), "windows-1251");
        Import imp = new Import(database.getHandler(), getSecurityOptions(), packetConfig, null, tmLogger);
        imp.setSaveAll(false);
        logger.fatal("database.getDBConfig().getDBAdapterName()=" + database.getDBConfig().getDBAdapterName());
        logger.fatal("database.getDBConfig().getId()=" + database.getDBConfig().getId());
        logger.fatal("imp=" + imp);
        logger.fatal("matrix=" + matrix);
        boolean allRowsSuccess = imp.save(matrix, Arrays.asList(new String[] { "q" }), 3);
        Assert.assertFalse(allRowsSuccess);
        logger.info(XmlUtils.toString(getLogElement(tmLogger, false)));
        assertEquals(new Parser().parse(readExpected("test/tmanager/checkNoAllSaving-log.xml")).getDocument().getDocumentElement(), getLogElement(tmLogger, false));
        assertEquals(new Parser().parse(readExpected("test/tmanager/checkNoAllSaving-result.xml")).getDocument().getDocumentElement(), readActualXml("select f1,f2,f3 from q order by f3"));
        executeUpdate("delete from q");
    }

    /**
	 * execute sql query, resulat as Xml
	 */
    private Element readActualXml(String sql) throws Exception {
        final SingleDBConnection connect = getTestDatabase().getConnect();
        try {
            List<Object[]> savedList = connect.executeQuery(sql, null);
            Document doc = Parser.createEmptyDocument();
            new DOMBeanSerializer().serializeTo((Element) doc.appendChild(doc.createElement("result")), savedList);
            return doc.getDocumentElement();
        } finally {
            connect.destroy();
        }
    }

    /**
	 * execute sql
	 * @return rows affected
	 */
    private int executeUpdate(String sql) throws Exception {
        HandlerI h = getTestDatabase().getHandler();
        h.startTransaction();
        try {
            return h.getConnection().executeUpdate(sql);
        } finally {
            h.commitTransaction();
            h.destroy();
        }
    }

    private String readExpected(String xmlResourse) throws Exception {
        return XmlUtils.toString(parseElement(readResource(this.getClass().getResource(xmlResourse))));
    }

    private static String readResource(URL url) throws IOException {
        return FileCopyUtils.copyToString(new InputStreamReader(url.openStream()));
    }
}
