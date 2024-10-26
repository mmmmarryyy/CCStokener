package nl.utwente.ewi.stream.servlets;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.jdom.*;
import org.jdom.input.*;
import nl.utwente.ewi.stream.network.QueryNetworkManager;
import nl.utwente.ewi.stream.network.dbcp.SQLConnectionManager;
import nl.utwente.ewi.stream.network.dbcp.SimplePostgresDBManager;

public class ProvenanceReasoningServlet extends HttpServlet {

    public static DataSource ds = SQLConnectionManager.ds;

    private static final Logger logger = Logger.getLogger(ProvenanceReasoningServlet.class.getName());

    public static final String provenanceType = QueryNetworkManager.config.getProvenanceType();

    public static final String wikiUrl = QueryNetworkManager.config.getWikiUrl();

    public static final String wikiNamespace = QueryNetworkManager.config.getWikiNamespace();

    public static final String wikiNamespaceNumber = QueryNetworkManager.config.getWikiNamespaceNumber();

    public static final String username = QueryNetworkManager.config.getUsername();

    public static final String password = QueryNetworkManager.config.getPassword();

    private String pe;

    private Integer no_of_sources;

    private String[] sources;

    private String[] windowTypes;

    private Integer[] windowPredicates;

    private String triggerType;

    private Integer triggerPredicate;

    private Integer mws;

    private Timestamp validTime;

    public static String pageTitle;

    public static Integer position;

    @SuppressWarnings("deprecation")
    public static Boolean hasParent(String source) throws SQLException {
        Connection conn = ds.getConnection();
        Statement st = conn.createStatement();
        System.out.println("SELECT column_name FROM information_schema.columns WHERE table_name = \'" + source.toString() + "_view\'");
        ResultSet rs = st.executeQuery("SELECT column_name FROM information_schema.columns WHERE table_name = \'" + source.toString() + "_view\'");
        while (rs.next()) {
            if (rs.getString(1).equals(source + "_parent") == true) return true;
        }
        return false;
    }

    public static String transformTimestamp(Timestamp ts) {
        String year = Integer.toString(ts.getYear() + 1900);
        String month = Integer.toString(ts.getMonth() + 1);
        if (month.length() == 1) month = "0" + month;
        String day = Integer.toString(ts.getDate());
        if (day.length() == 1) day = "0" + day;
        String hour = Integer.toString(ts.getHours());
        if (hour.length() == 1) hour = "0" + hour;
        String min = Integer.toString(ts.getMinutes());
        if (min.length() == 1) min = "0" + min;
        String sec = Integer.toString(ts.getSeconds());
        if (sec.length() == 1) sec = "0" + sec;
        return year + month + day + hour + min + sec;
    }

    public static void extractInfo(String data, String fileName) {
        try {
            URL url;
            url = new URL(data);
            URLConnection urlconn = url.openConnection();
            urlconn.setDoInput(true);
            System.out.println("\nSuccessful");
            BufferedReader in = new BufferedReader(new InputStreamReader(urlconn.getInputStream()));
            StringBuffer output = new StringBuffer();
            String temp;
            while ((temp = in.readLine()) != null) {
                System.out.println(temp);
                output.append(temp);
            }
            in.close();
            FileWriter fstream = new FileWriter(fileName);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(output.toString());
            out.close();
        } catch (MalformedURLException e2) {
            System.out.println("MalformedURlException: " + e2);
        } catch (IOException e3) {
            System.out.println("IOException: " + e3);
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        sources = new String[10];
        windowTypes = new String[10];
        windowPredicates = new Integer[10];
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        String id = request.getParameter("id");
        String source = request.getParameter("source");
        source = source.split("_view", 2)[0];
        long startTime = 0;
        out.println("<html>");
        out.println("<head>");
        out.println("<title> Reasoning Provenance Information</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("Reasoning Provenance Information<br>");
        out.println("--------------------------------<br>");
        startTime = System.nanoTime();
        try {
            Connection conn = ds.getConnection();
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            Statement st = conn.createStatement();
            StringBuffer firstQuery = new StringBuffer("SELECT transactiontime from ");
            firstQuery.append(source + "_view");
            firstQuery.append(" WHERE id = " + id.toString());
            logger.log(Level.INFO, "Step 1-First Query:" + firstQuery.toString());
            ResultSet rs = st.executeQuery(firstQuery.toString());
            Timestamp tt_time = null;
            while (rs.next()) tt_time = rs.getTimestamp(1);
            String tt_time_transformed = transformTimestamp(tt_time);
            logger.log(Level.INFO, "Transaction Time of tuple: " + tt_time_transformed);
            String endpoint = wikiUrl.split("index.php/", 2)[0] + "libs/rdfapi-php/netapi/testmodel?query=";
            String subject, attributes, conditions;
            subject = "?page";
            attributes = " ?page ";
            conditions = "{" + subject + " <" + wikiUrl + "Special:URIResolver/Property-3APE_name> ?x . " + subject + " <" + wikiUrl + "Special:URIResolver/Property-3AAdd> ?y . " + subject + " <" + wikiUrl + "Special:URIResolver/Property-3ADelete> ?z . " + "FILTER " + "regex " + "(?x, \"" + source + "\") . " + "FILTER " + "(?y < \"" + tt_time_transformed + "\") .  " + "FILTER " + "(?z >= \"" + tt_time_transformed + "\") .  " + "}";
            String sparqlQuery1 = endpoint + URLEncoder.encode("SELECT distinct" + attributes + "WHERE " + conditions);
            logger.log(Level.INFO, "sparqlQuery1: " + sparqlQuery1);
            extractInfo(sparqlQuery1, "page.xml");
            SAXBuilder builder = new SAXBuilder();
            try {
                Locale.setDefault(Locale.US);
                Document doc = builder.build("page.xml");
                Element root = doc.getRootElement();
                List nodes = root.getChildren();
                Element results = (Element) nodes.get(1);
                List nodes1 = results.getChildren();
                Element result = (Element) nodes1.get(0);
                List nodes2 = result.getChildren();
                Element binding = (Element) nodes2.get(0);
                List nodes3 = binding.getChildren();
                Element uri = (Element) nodes3.get(0);
                pageTitle = uri.getTextTrim();
                logger.log(Level.INFO, pageTitle);
            } catch (Exception ex) {
                logger.log(Level.INFO, "File not Found!" + ex);
                System.exit(1);
            }
            subject = "<" + pageTitle + ">";
            attributes = " ?pe ?no_of_sources ?t_type ?t_predicate ?valid_time ?mws ?add ";
            conditions = "{" + subject + " <" + wikiUrl + "Special:URIResolver/Property-3APE_name> ?pe . " + subject + " <" + wikiUrl + "Special:URIResolver/Property-3APE_noOfSources> ?no_of_sources . " + subject + " <" + wikiUrl + "Special:URIResolver/Property-3APE_triggerType> ?t_type " + subject + " <" + wikiUrl + "Special:URIResolver/Property-3APE_triggerPredicate> ?t_predicate . " + subject + " <" + wikiUrl + "Special:URIResolver/Property-3AValid_time> ?valid_time . " + subject + " <" + wikiUrl + "Special:URIResolver/Property-3APE_minimalWindowSize> ?mws . " + subject + " <" + wikiUrl + "Special:URIResolver/Property-3AAdd> ?add " + "}";
            StringBuffer sb = new StringBuffer("SELECT distinct");
            sb.append(attributes);
            sb.append("WHERE ");
            sb.append(conditions);
            String data = endpoint + URLEncoder.encode(sb.toString());
            logger.log(Level.INFO, "Query: " + data);
            extractInfo(data, "temp.xml");
            logger.log(Level.INFO, "New block");
            String[] literals = new String[100];
            try {
                Locale.setDefault(Locale.US);
                Document doc = builder.build("temp.xml");
                Element root = doc.getRootElement();
                List nodes = root.getChildren();
                Element results = (Element) nodes.get(1);
                List nodes1 = results.getChildren();
                Element result = (Element) nodes1.get(0);
                List nodes2 = result.getChildren();
                logger.log(Level.INFO, "Size of result: " + nodes2.size());
                Element binding, literal;
                List nodes3;
                for (int i = 0; i < nodes2.size(); i++) {
                    binding = (Element) nodes2.get(i);
                    nodes3 = binding.getChildren();
                    literal = (Element) nodes3.get(0);
                    literals[i] = literal.getTextTrim();
                    logger.log(Level.INFO, "Literal[" + i + "]" + literals[i]);
                }
            } catch (Exception ex) {
                logger.log(Level.INFO, "File not Found!" + ex);
                System.exit(1);
            }
            pe = literals[0];
            no_of_sources = Integer.parseInt(literals[1]);
            triggerType = literals[2];
            triggerPredicate = Integer.parseInt(literals[3]);
            ;
            mws = Integer.parseInt(literals[5]);
            ;
            validTime = Timestamp.valueOf(literals[4]);
            logger.log(Level.INFO, "Final: " + pe + " " + no_of_sources + " " + triggerType + " " + triggerPredicate + " " + mws + " " + validTime);
            subject = "<" + pageTitle + ">";
            String att = "";
            String cond = "";
            for (int i = 1; i <= no_of_sources; i++) {
                att += " ?a" + Integer.toString(i) + " ?b" + Integer.toString(i) + " ?c" + Integer.toString(i);
                if (i > 1) {
                    cond += " . ";
                }
                cond += subject + " <" + wikiUrl + "Special:URIResolver/Property-3APE_source" + Integer.toString(i) + "> ?a" + Integer.toString(i) + " . " + subject + " <" + wikiUrl + "Special:URIResolver/Property-3APE_windowType" + Integer.toString(i) + "> ?b" + Integer.toString(i) + " . " + subject + " <" + wikiUrl + "Special:URIResolver/Property-3APE_windowPredicate" + Integer.toString(i) + "> ?c" + Integer.toString(i);
            }
            attributes = att + " ";
            conditions = "{" + cond + "}";
            StringBuffer sb1 = new StringBuffer("SELECT distinct");
            sb1.append(attributes);
            sb1.append(" WHERE ");
            sb1.append(conditions);
            logger.log(Level.INFO, "Uncoded: " + sb1.toString());
            String data1 = endpoint + URLEncoder.encode(sb1.toString());
            logger.log(Level.INFO, "Query: " + data1);
            extractInfo(data1, "source.xml");
            try {
                Locale.setDefault(Locale.US);
                Document doc = builder.build("source.xml");
                Element root = doc.getRootElement();
                List nodes = root.getChildren();
                Element results = (Element) nodes.get(1);
                List nodes1 = results.getChildren();
                Element result = (Element) nodes1.get(0);
                List nodes2 = result.getChildren();
                logger.log(Level.INFO, "Size of result: " + nodes2.size());
                Element binding, literal;
                List nodes3;
                for (int i = 0; i < nodes2.size(); i++) {
                    binding = (Element) nodes2.get(i);
                    nodes3 = binding.getChildren();
                    literal = (Element) nodes3.get(0);
                    literals[i] = literal.getTextTrim();
                    logger.log(Level.INFO, "Literal[" + i + "]" + literals[i]);
                }
            } catch (Exception ex) {
                logger.log(Level.INFO, "File not Found!" + ex);
                System.exit(1);
            }
            for (int i = 0; i < no_of_sources; i++) {
                sources[i] = literals[3 * i];
                windowTypes[i] = literals[3 * i + 1];
                windowPredicates[i] = Integer.parseInt(literals[3 * i + 2]);
            }
            logger.log(Level.INFO, "Source Info: ");
            for (int i = 0; i < no_of_sources; i++) {
                logger.log(Level.INFO, sources[i] + " " + windowTypes[i] + " " + windowPredicates[i]);
            }
            StringBuffer secondQuery = new StringBuffer("WITH temp AS " + "(SELECT id, ROW_NUMBER() OVER(ORDER BY id) AS position ");
            secondQuery.append("FROM ");
            secondQuery.append(source + "_view");
            secondQuery.append(" WHERE");
            secondQuery.append(" transactiontime IN (SELECT transactiontime" + " FROM " + source + "_view " + "WHERE id=\'" + id + "\'))");
            secondQuery.append(" SELECT id, position FROM temp where id='" + id + "\'");
            logger.log(Level.INFO, "Second Query: " + secondQuery.toString());
            rs = st.executeQuery(secondQuery.toString());
            while (rs.next()) {
                logger.log(Level.INFO, rs.getInt(1) + " " + rs.getInt(2));
                position = rs.getInt(2);
            }
            int no_of_columns = 0;
            StringBuffer query = new StringBuffer();
            StringBuffer query1 = new StringBuffer();
            if (mws == 1) {
                if (no_of_sources > 1) {
                    if (hasParent(source)) {
                        String parent = "";
                        logger.log(Level.INFO, "Union:Parent-" + "SELECT " + source + "_parent FROM " + source + "_view " + "WHERE id = " + id.toString());
                        rs = st.executeQuery("SELECT " + source + "_parent FROM " + source + "_view " + "WHERE id = " + id.toString());
                        while (rs.next()) {
                            parent = rs.getString(source + "_parent");
                            logger.log(Level.INFO, "Result:" + parent);
                        }
                        StringBuffer secondQuery1 = new StringBuffer("WITH temp AS " + "(SELECT id, ROW_NUMBER() OVER(ORDER BY id) AS position ");
                        secondQuery1.append("FROM ");
                        secondQuery1.append(source + "_view");
                        secondQuery1.append(" WHERE");
                        secondQuery1.append(" transactiontime IN (SELECT transactiontime" + " FROM " + source + "_view " + "WHERE id=\'" + id + "\') AND union1_parent=\'" + parent + "\')");
                        secondQuery1.append(" SELECT id, position FROM temp where id='" + id + "\'");
                        logger.log(Level.INFO, "Second Query1: " + secondQuery1.toString());
                        rs = st.executeQuery(secondQuery1.toString());
                        while (rs.next()) {
                            logger.log(Level.INFO, rs.getInt(1) + " " + rs.getInt(2));
                            position = rs.getInt(2);
                        }
                        parent = parent.split("_view", 2)[0];
                        for (int i = 0; i < no_of_sources; i++) {
                            logger.log(Level.INFO, "Result:" + sources[i].toString());
                            if (sources[i].equals(parent)) {
                                query.append("WITH temp AS (SELECT * FROM ");
                                query.append(sources[i] + "_view ");
                                if (windowTypes[i].equals("SlidingTuple")) {
                                    query.append("WHERE transactiontime < \'" + tt_time + "\'");
                                    query.append(" AND transactiontime > \'" + validTime + "\' ORDER BY transactiontime DESC LIMIT " + windowPredicates[i] + ") SELECT *, ROW_NUMBER() OVER (ORDER BY id) AS position from temp");
                                    logger.log(Level.INFO, "Third-Query: " + query.toString());
                                    rs = st.executeQuery(query.toString());
                                    while (rs.next()) {
                                        logger.log(Level.INFO, "Result: " + rs.getInt("id"));
                                        if (rs.getInt("position") == position) {
                                            logger.log(Level.INFO, rs.getInt("id") + " ");
                                            no_of_columns = rs.getMetaData().getColumnCount();
                                            out.println(source + "   ------>>>   " + sources[0] + "<br>");
                                            out.println("<br> <br>");
                                            out.println("<table>");
                                            out.println("<tr>");
                                            for (int j = 1; j <= no_of_columns - 1; j++) {
                                                out.println("<th style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                out.println(rs.getMetaData().getColumnName(j));
                                                out.println("</th>");
                                            }
                                            out.println("</tr>");
                                            int column_type;
                                            out.println("<tr>");
                                            for (int k = 1; k <= no_of_columns - 1; k++) {
                                                column_type = rs.getMetaData().getColumnType(k);
                                                if (column_type == 4) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getInt(k));
                                                    out.println("</td>");
                                                } else if (column_type == -5) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getLong(k));
                                                    out.println("</td>");
                                                } else if (column_type == 8) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getDouble(k));
                                                    out.println("</td>");
                                                } else if (column_type == 12) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getString(k));
                                                    out.println("</td>");
                                                } else if (column_type == 93) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getTimestamp(k));
                                                    out.println("</td>");
                                                } else {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getObject(k));
                                                    out.println("</td>");
                                                }
                                            }
                                            out.println("</tr>");
                                        }
                                    }
                                } else {
                                    Long t1 = tt_time.getTime();
                                    Long t2 = (long) windowPredicates[i];
                                    Long lowerBound = t1 - t2;
                                    Long valid = validTime.getTime();
                                    if (lowerBound < valid) lowerBound = valid;
                                    Timestamp ts = new Timestamp(lowerBound);
                                    query.append("WHERE transactiontime < \'" + tt_time + "\'");
                                    query.append(" AND transactiontime > \'" + ts + "\' ORDER BY transactiontime DESC) SELECT *, ROW_NUMBER() OVER (ORDER BY id) AS position from temp");
                                    rs = st.executeQuery(query.toString());
                                    while (rs.next()) {
                                        if (rs.getInt("position") == position) {
                                            logger.log(Level.INFO, rs.getInt("id") + " ");
                                            logger.log(Level.INFO, rs.getInt("id") + " ");
                                            no_of_columns = rs.getMetaData().getColumnCount();
                                            out.println(source + "   ------>>>   " + sources[0] + "<br>");
                                            out.println("<br> <br>");
                                            out.println("<table>");
                                            out.println("<tr>");
                                            for (int j = 1; j <= no_of_columns - 1; j++) {
                                                out.println("<th style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                out.println(rs.getMetaData().getColumnName(j));
                                                out.println("</th>");
                                            }
                                            out.println("</tr>");
                                            int column_type;
                                            out.println("<tr>");
                                            for (int k = 1; k <= no_of_columns - 1; k++) {
                                                column_type = rs.getMetaData().getColumnType(k);
                                                if (column_type == 4) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getInt(k));
                                                    out.println("</td>");
                                                } else if (column_type == -5) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getLong(k));
                                                    out.println("</td>");
                                                } else if (column_type == 8) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getDouble(k));
                                                    out.println("</td>");
                                                } else if (column_type == 12) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getString(k));
                                                    out.println("</td>");
                                                } else if (column_type == 93) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getTimestamp(k));
                                                    out.println("</td>");
                                                } else {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getObject(k));
                                                    out.println("</td>");
                                                }
                                            }
                                            out.println("</tr>");
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        int sum = 1;
                        int div = 0;
                        int mod = 0;
                        for (int k = 0; k < no_of_sources; k++) {
                            sum *= windowPredicates[k];
                        }
                        if (sum > 1) {
                            Integer[] tuplePositions = new Integer[no_of_sources];
                            Integer[] windows = new Integer[no_of_sources];
                            for (int j = 0; j < no_of_sources; j++) {
                                if (windowTypes[j].equals("SlidingTuple") == true) windows[j] = windowPredicates[j]; else {
                                    Long t1 = tt_time.getTime();
                                    Long t2 = (long) windowPredicates[j];
                                    Long lowerBound = t1 - t2;
                                    Long valid = validTime.getTime();
                                    if (lowerBound < valid) lowerBound = valid;
                                    Timestamp ts = new Timestamp(lowerBound);
                                    rs = st.executeQuery("SELECT count (*) as total_row FROM " + sources[j] + "_view" + "WHERE transactiontime < \'" + tt_time + "\'" + " AND transactiontime > \'" + ts + "\'");
                                    while (rs.next()) windows[j] = rs.getInt("total_row");
                                }
                            }
                            for (int j = 0; j < no_of_sources - 1; j++) {
                                sum = 1;
                                for (int k = 1; k < no_of_sources; k++) {
                                    sum *= windowPredicates[k];
                                }
                                div = position / sum;
                                mod = position % sum;
                                if (mod > 0) div = div + 1;
                                tuplePositions[j] = div;
                            }
                            tuplePositions[no_of_sources - 1] = position % windowPredicates[no_of_sources - 1];
                            if (tuplePositions[no_of_sources - 1] == 0) tuplePositions[no_of_sources - 1] = windowPredicates[no_of_sources - 1];
                            for (int i = 0; i < no_of_sources; i++) {
                                logger.log(Level.INFO, tuplePositions[i] + " " + windows[i]);
                                query.setLength(0);
                                query.append("WITH temp AS (SELECT * FROM ");
                                query.append(sources[i] + "_view ");
                                if (windowTypes[i].equals("SlidingTuple")) {
                                    query.append("WHERE transactiontime < \'" + tt_time + "\'");
                                    query.append(" AND transactiontime > \'" + validTime + "\' ORDER BY transactiontime DESC LIMIT " + windowPredicates[i] + ") SELECT *, ROW_NUMBER() OVER (ORDER BY id) AS position from temp");
                                    logger.log(Level.INFO, "Query3-1:" + query.toString());
                                    rs = st.executeQuery(query.toString());
                                    while (rs.next()) {
                                        if (rs.getInt("position") == tuplePositions[i]) {
                                            logger.log(Level.INFO, rs.getInt("id") + " ");
                                            no_of_columns = rs.getMetaData().getColumnCount();
                                            out.println(source + "   ------>>>   " + sources[0] + "<br>");
                                            out.println("<br> <br>");
                                            out.println("<table>");
                                            out.println("<tr>");
                                            for (int j = 1; j <= no_of_columns - 1; j++) {
                                                out.println("<th style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                out.println(rs.getMetaData().getColumnName(j));
                                                out.println("</th>");
                                            }
                                            out.println("</tr>");
                                            int column_type;
                                            out.println("<tr>");
                                            for (int k = 1; k <= no_of_columns - 1; k++) {
                                                column_type = rs.getMetaData().getColumnType(k);
                                                if (column_type == 4) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getInt(k));
                                                    out.println("</td>");
                                                } else if (column_type == -5) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getLong(k));
                                                    out.println("</td>");
                                                } else if (column_type == 8) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getDouble(k));
                                                    out.println("</td>");
                                                } else if (column_type == 12) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getString(k));
                                                    out.println("</td>");
                                                } else if (column_type == 93) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getTimestamp(k));
                                                    out.println("</td>");
                                                } else {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getObject(k));
                                                    out.println("</td>");
                                                }
                                            }
                                            out.println("</tr>");
                                        }
                                    }
                                } else {
                                    Long t1 = tt_time.getTime();
                                    Long t2 = (long) windowPredicates[i];
                                    Long lowerBound = t1 - t2;
                                    Long valid = validTime.getTime();
                                    if (lowerBound < valid) lowerBound = valid;
                                    Timestamp ts = new Timestamp(lowerBound);
                                    query.append("WHERE transactiontime < \'" + tt_time + "\'");
                                    query.append(" AND transactiontime > \'" + ts + "\' ORDER BY transactiontime DESC) SELECT *, ROW_NUMBER() OVER (ORDER BY id) AS position from temp");
                                    rs = st.executeQuery(query.toString());
                                    while (rs.next()) {
                                        if (rs.getInt("position") == tuplePositions[i]) {
                                            logger.log(Level.INFO, rs.getInt("id") + " ");
                                            no_of_columns = rs.getMetaData().getColumnCount();
                                            out.println(source + "   ------>>>   " + sources[0] + "<br>");
                                            out.println("<br> <br>");
                                            out.println("<table>");
                                            out.println("<tr>");
                                            for (int j = 1; j <= no_of_columns - 1; j++) {
                                                out.println("<th style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                out.println(rs.getMetaData().getColumnName(j));
                                                out.println("</th>");
                                            }
                                            out.println("</tr>");
                                            int column_type;
                                            out.println("<tr>");
                                            for (int k = 1; k <= no_of_columns - 1; k++) {
                                                column_type = rs.getMetaData().getColumnType(k);
                                                if (column_type == 4) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getInt(k));
                                                    out.println("</td>");
                                                } else if (column_type == -5) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getLong(k));
                                                    out.println("</td>");
                                                } else if (column_type == 8) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getDouble(k));
                                                    out.println("</td>");
                                                } else if (column_type == 12) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getString(k));
                                                    out.println("</td>");
                                                } else if (column_type == 93) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getTimestamp(k));
                                                    out.println("</td>");
                                                } else {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getObject(k));
                                                    out.println("</td>");
                                                }
                                            }
                                            out.println("</tr>");
                                        }
                                    }
                                }
                            }
                        } else {
                            for (int i = 0; i < no_of_sources; i++) {
                                query.setLength(0);
                                query.append("WITH temp AS (SELECT * FROM ");
                                query.append(sources[i] + "_view ");
                                if (windowTypes[i].equals("SlidingTuple")) {
                                    query.append("WHERE transactiontime < \'" + tt_time + "\'");
                                    query.append(" AND transactiontime > \'" + validTime + "\' ORDER BY transactiontime DESC LIMIT " + windowPredicates[i] + ") SELECT *, ROW_NUMBER() OVER (ORDER BY id) AS position from temp");
                                    rs = st.executeQuery(query.toString());
                                    while (rs.next()) {
                                        if (rs.getInt("position") == position) {
                                            logger.log(Level.INFO, rs.getInt("id") + " ");
                                            no_of_columns = rs.getMetaData().getColumnCount();
                                            out.println(source + "   ------>>>   " + sources[0] + "<br>");
                                            out.println("<br> <br>");
                                            out.println("<table>");
                                            out.println("<tr>");
                                            for (int j = 1; j <= no_of_columns - 1; j++) {
                                                out.println("<th style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                out.println(rs.getMetaData().getColumnName(j));
                                                out.println("</th>");
                                            }
                                            out.println("</tr>");
                                            int column_type;
                                            out.println("<tr>");
                                            for (int k = 1; k <= no_of_columns - 1; k++) {
                                                column_type = rs.getMetaData().getColumnType(k);
                                                if (column_type == 4) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getInt(k));
                                                    out.println("</td>");
                                                } else if (column_type == -5) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getLong(k));
                                                    out.println("</td>");
                                                } else if (column_type == 8) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getDouble(k));
                                                    out.println("</td>");
                                                } else if (column_type == 12) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getString(k));
                                                    out.println("</td>");
                                                } else if (column_type == 93) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getTimestamp(k));
                                                    out.println("</td>");
                                                } else {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getObject(k));
                                                    out.println("</td>");
                                                }
                                            }
                                            out.println("</tr>");
                                        }
                                    }
                                } else {
                                    Long t1 = tt_time.getTime();
                                    Long t2 = (long) windowPredicates[i];
                                    Long lowerBound = t1 - t2;
                                    Long valid = validTime.getTime();
                                    if (lowerBound < valid) lowerBound = valid;
                                    Timestamp ts = new Timestamp(lowerBound);
                                    query.append("WHERE transactiontime < \'" + tt_time + "\'");
                                    query.append(" AND transactiontime > \'" + ts + "\' ORDER BY transactiontime DESC) SELECT *, ROW_NUMBER() OVER (ORDER BY id) AS position from temp");
                                    rs = st.executeQuery(query.toString());
                                    while (rs.next()) {
                                        if (rs.getInt("position") == position) {
                                            logger.log(Level.INFO, rs.getInt("id") + " ");
                                            no_of_columns = rs.getMetaData().getColumnCount();
                                            out.println(source + "   ------>>>   " + sources[0] + "<br>");
                                            out.println("<br> <br>");
                                            out.println("<table>");
                                            out.println("<tr>");
                                            for (int j = 1; j <= no_of_columns - 1; j++) {
                                                out.println("<th style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                out.println(rs.getMetaData().getColumnName(j));
                                                out.println("</th>");
                                            }
                                            out.println("</tr>");
                                            int column_type;
                                            out.println("<tr>");
                                            for (int k = 1; k <= no_of_columns - 1; k++) {
                                                column_type = rs.getMetaData().getColumnType(k);
                                                if (column_type == 4) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getInt(k));
                                                    out.println("</td>");
                                                } else if (column_type == -5) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getLong(k));
                                                    out.println("</td>");
                                                } else if (column_type == 8) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getDouble(k));
                                                    out.println("</td>");
                                                } else if (column_type == 12) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getString(k));
                                                    out.println("</td>");
                                                } else if (column_type == 93) {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getTimestamp(k));
                                                    out.println("</td>");
                                                } else {
                                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                                    out.println(rs.getObject(k));
                                                    out.println("</td>");
                                                }
                                            }
                                            out.println("</tr>");
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    query.append("WITH temp AS (SELECT * FROM ");
                    query.append(sources[0] + "_view ");
                    if (windowTypes[0].equals("SlidingTuple")) {
                        query.append("WHERE transactiontime < \'" + tt_time + "\'");
                        query.append(" AND transactiontime > \'" + validTime + "\' ORDER BY transactiontime DESC LIMIT " + windowPredicates[0] + ") SELECT *, ROW_NUMBER() OVER (ORDER BY id) AS position from temp");
                        rs = st.executeQuery(query.toString());
                        while (rs.next()) {
                            if (rs.getInt("position") == position) {
                                logger.log(Level.INFO, rs.getInt("id") + " ");
                                no_of_columns = rs.getMetaData().getColumnCount();
                                out.println(source + "   ------>>>   " + sources[0] + "<br>");
                                out.println("<br> <br>");
                                out.println("<table>");
                                out.println("<tr>");
                                for (int j = 1; j <= no_of_columns - 1; j++) {
                                    out.println("<th style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                    out.println(rs.getMetaData().getColumnName(j));
                                    out.println("</th>");
                                }
                                out.println("</tr>");
                                int column_type;
                                out.println("<tr>");
                                for (int k = 1; k <= no_of_columns - 1; k++) {
                                    column_type = rs.getMetaData().getColumnType(k);
                                    if (column_type == 4) {
                                        out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                        out.println(rs.getInt(k));
                                        out.println("</td>");
                                    } else if (column_type == -5) {
                                        out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                        out.println(rs.getLong(k));
                                        out.println("</td>");
                                    } else if (column_type == 8) {
                                        out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                        out.println(rs.getDouble(k));
                                        out.println("</td>");
                                    } else if (column_type == 12) {
                                        out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                        out.println(rs.getString(k));
                                        out.println("</td>");
                                    } else if (column_type == 93) {
                                        out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                        out.println(rs.getTimestamp(k));
                                        out.println("</td>");
                                    } else {
                                        out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                        out.println(rs.getObject(k));
                                        out.println("</td>");
                                    }
                                }
                                out.println("</tr>");
                            }
                        }
                    } else {
                        Long t1 = tt_time.getTime();
                        Long t2 = (long) windowPredicates[0];
                        Long lowerBound = t1 - t2;
                        Long valid = validTime.getTime();
                        if (lowerBound < valid) lowerBound = valid;
                        Timestamp ts = new Timestamp(lowerBound);
                        query.append("WHERE transactiontime < \'" + tt_time + "\'");
                        query.append(" AND transactiontime > \'" + ts + "\' ORDER BY transactiontime DESC) SELECT *, ROW_NUMBER() OVER (ORDER BY id) AS position from temp");
                        rs = st.executeQuery(query.toString());
                        while (rs.next()) {
                            if (rs.getInt("position") == position) {
                                logger.log(Level.INFO, rs.getInt("id") + " ");
                                no_of_columns = rs.getMetaData().getColumnCount();
                                out.println(source + "   ------>>>   " + sources[0] + "<br>");
                                out.println("<br> <br>");
                                out.println("<table>");
                                out.println("<tr>");
                                for (int j = 1; j <= no_of_columns - 1; j++) {
                                    out.println("<th style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                    out.println(rs.getMetaData().getColumnName(j));
                                    out.println("</th>");
                                }
                                out.println("</tr>");
                                int column_type;
                                out.println("<tr>");
                                for (int k = 1; k <= no_of_columns - 1; k++) {
                                    column_type = rs.getMetaData().getColumnType(k);
                                    if (column_type == 4) {
                                        out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                        out.println(rs.getInt(k));
                                        out.println("</td>");
                                    } else if (column_type == -5) {
                                        out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                        out.println(rs.getLong(k));
                                        out.println("</td>");
                                    } else if (column_type == 8) {
                                        out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                        out.println(rs.getDouble(k));
                                        out.println("</td>");
                                    } else if (column_type == 12) {
                                        out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                        out.println(rs.getString(k));
                                        out.println("</td>");
                                    } else if (column_type == 93) {
                                        out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                        out.println(rs.getTimestamp(k));
                                        out.println("</td>");
                                    } else {
                                        out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                        out.println(rs.getObject(k));
                                        out.println("</td>");
                                    }
                                }
                                out.println("</tr>");
                            }
                        }
                    }
                }
            } else {
                for (int i = 0; i < no_of_sources; i++) {
                    query.setLength(0);
                    query.append("WITH temp AS (SELECT * FROM ");
                    query.append(sources[i] + "_view ");
                    if (windowTypes[i].equals("SlidingTuple")) {
                        query.append("WHERE transactiontime < \'" + tt_time + "\'");
                        query.append(" AND transactiontime > \'" + validTime + "\' ORDER BY transactiontime DESC LIMIT " + windowPredicates[i] + ") SELECT * from temp");
                        rs = st.executeQuery(query.toString());
                        while (rs.next()) {
                            logger.log(Level.INFO, rs.getInt("id") + " ");
                            no_of_columns = rs.getMetaData().getColumnCount();
                            out.println(source + "   ------>>>   " + sources[0] + "<br>");
                            out.println("<br> <br>");
                            out.println("<table>");
                            out.println("<tr>");
                            for (int j = 1; j <= no_of_columns - 1; j++) {
                                out.println("<th style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                out.println(rs.getMetaData().getColumnName(j));
                                out.println("</th>");
                            }
                            out.println("</tr>");
                            int column_type;
                            out.println("<tr>");
                            for (int k = 1; k <= no_of_columns - 1; k++) {
                                column_type = rs.getMetaData().getColumnType(k);
                                if (column_type == 4) {
                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                    out.println(rs.getInt(k));
                                    out.println("</td>");
                                } else if (column_type == -5) {
                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                    out.println(rs.getLong(k));
                                    out.println("</td>");
                                } else if (column_type == 8) {
                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                    out.println(rs.getDouble(k));
                                    out.println("</td>");
                                } else if (column_type == 12) {
                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                    out.println(rs.getString(k));
                                    out.println("</td>");
                                } else if (column_type == 93) {
                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                    out.println(rs.getTimestamp(k));
                                    out.println("</td>");
                                } else {
                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                    out.println(rs.getObject(k));
                                    out.println("</td>");
                                }
                            }
                            out.println("</tr>");
                        }
                    } else {
                        Long t1 = tt_time.getTime();
                        Long t2 = (long) windowPredicates[i];
                        Long lowerBound = t1 - t2;
                        Long valid = validTime.getTime();
                        if (lowerBound < valid) lowerBound = valid;
                        Timestamp ts = new Timestamp(lowerBound);
                        query.append("WHERE transactiontime < \'" + tt_time + "\'");
                        query.append(" AND transactiontime > \'" + ts + "\' ORDER BY transactiontime DESC) SELECT * from temp");
                        rs = st.executeQuery(query.toString());
                        while (rs.next()) {
                            logger.log(Level.INFO, rs.getInt("id") + " ");
                            no_of_columns = rs.getMetaData().getColumnCount();
                            out.println(source + "   ------>>>   " + sources[0] + "<br>");
                            out.println("<br> <br>");
                            out.println("<table>");
                            out.println("<tr>");
                            for (int j = 1; j <= no_of_columns - 1; j++) {
                                out.println("<th style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                out.println(rs.getMetaData().getColumnName(j));
                                out.println("</th>");
                            }
                            out.println("</tr>");
                            int column_type;
                            out.println("<tr>");
                            for (int k = 1; k <= no_of_columns - 1; k++) {
                                column_type = rs.getMetaData().getColumnType(k);
                                if (column_type == 4) {
                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                    out.println(rs.getInt(k));
                                    out.println("</td>");
                                } else if (column_type == -5) {
                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                    out.println(rs.getLong(k));
                                    out.println("</td>");
                                } else if (column_type == 8) {
                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                    out.println(rs.getDouble(k));
                                    out.println("</td>");
                                } else if (column_type == 12) {
                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                    out.println(rs.getString(k));
                                    out.println("</td>");
                                } else if (column_type == 93) {
                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                    out.println(rs.getTimestamp(k));
                                    out.println("</td>");
                                } else {
                                    out.println("<td style=\"padding: 2px 5px; border-bottom: 1px solid black; border-right: 1px solid black;\">");
                                    out.println(rs.getObject(k));
                                    out.println("</td>");
                                }
                            }
                            out.println("</tr>");
                        }
                    }
                }
            }
            st.close();
            conn.close();
            out.println("</table><br><br><br>");
        } catch (SQLException e) {
            logger.log(Level.INFO, "SQLException: " + e);
            e.printStackTrace();
        }
        double elapsedTime = (System.nanoTime() - startTime) / 1000000;
        out.println("<br> Response Time : <b>" + elapsedTime + "</b> milliseconds");
        out.println("</body></html>");
    }
}
