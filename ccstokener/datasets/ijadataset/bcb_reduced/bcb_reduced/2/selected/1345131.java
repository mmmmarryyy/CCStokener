package edu.harvard.iq.safe.citest.prep.locksspeerlist;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.util.*;
import java.util.logging.*;
import java.net.*;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.auth.*;
import java.util.regex.*;
import java.text.*;

/**
 *
 * @author asone
 */
@WebServlet(name = "PLNpeersFinder", urlPatterns = { "/PLNpeersFinder" })
public class PLNpeersFinder extends HttpServlet {

    private static String[] testTables = { "ArchivalUnitStatusTable", "PlatformStatus", "RepositorySpace", "V3PollerTable" };

    private static Logger logger = Logger.getLogger(LOCKSSDaemonStatusTableXmlParser.class.getPackage().getName());

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @SuppressWarnings("LoggerStringConcat")
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            String peerList = null;
            String lockss_xml_url = "http://verity.irss.unc.edu/lockss.xml";
            URL lockss_xml = new URL(lockss_xml_url);
            List<String> peers = new ArrayList<String>(0);
            readLockssConfigFile(lockss_xml, peers);
            List<String> peerIpList = new ArrayList<String>();
            String IpAdRegex = "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" + "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
            Pattern p = Pattern.compile(IpAdRegex);
            for (String raw : peers) {
                Matcher m = p.matcher(raw);
                if (m.find()) {
                    peerIpList.add(m.group(0));
                }
            }
            StringBuilder sb = new StringBuilder();
            if (peerIpList.size() > 0) {
                sb.append("<ul>\n");
                for (String el : peerIpList) {
                    sb.append("<li>" + el + "</li>");
                }
                sb.append("</ul>");
            } else {
                sb.append("<p>no peer Ip address was found</p>");
            }
            peerList = sb.toString();
            logger.info("peerIpList=" + peerIpList);
            String portNumber = "8081";
            String qsRoot = "/DaemonStatus?table=";
            String[] tableName = { "ArchivalUnitStatusTable" };
            String[] formatType = { "csv", "text", "xml" };
            DefaultHttpClient httpclient = new DefaultHttpClient();
            List<String> exIps = new ArrayList<String>();
            StringBuilder sbPre = new StringBuilder();
            IP: for (String ip : peerIpList) {
                logger.info("working on ip address=" + ip);
                if (ip.startsWith("141")) {
                    logger.info("icpsr sites are currently not accessible: skip " + ip);
                    exIps.add(ip);
                    continue IP;
                }
                httpclient.getCredentialsProvider().setCredentials(new AuthScope(ip, 8081), new UsernamePasswordCredentials("debug", "CNS-debug"));
                for (String tableId : testTables) {
                    String dataUrl = "http://" + ip + ":" + portNumber + qsRoot + tableId + "&output=" + formatType[2];
                    logger.info("dataUrl=" + dataUrl);
                    URL dsDataUrl = new URL(dataUrl);
                    HttpGet httpget = new HttpGet(dataUrl);
                    logger.info("executing request " + httpget.getURI());
                    logger.info("----------------------------------------");
                    Reader sr = null;
                    try {
                        ResponseHandler<String> responseHandler = new BasicResponseHandler();
                        String responseBody = httpclient.execute(httpget, responseHandler);
                        if (responseBody != null) {
                            sr = new StringReader(responseBody);
                            logger.info("----------------------------------------");
                            LOCKSSDaemonStatusTableXmlParser ldstxp = new LOCKSSDaemonStatusTableXmlParser();
                            ldstxp.readLOCKSSDaemonStatusTable(sr);
                            String temp = ldstxp.getColumndescriptorList().toString();
                            if (tableId.equals(testTables[1])) {
                                sbPre.append("<code>" + ip + ": table=" + tableId + ": available variables=" + ldstxp.getSummaryInfoList().toString() + "</code><br />");
                            } else {
                                sbPre.append("<code>" + ip + ": table=" + tableId + ": available variables=" + temp + "</code><br />");
                            }
                            logger.log(Level.INFO, "column list", temp);
                        } else {
                            logger.info("+++++++ responseBody is null");
                        }
                        sr.close();
                    } catch (HttpResponseException hre) {
                        logger.log(Level.SEVERE, "an error in the HTTP protocol", hre);
                        hre.printStackTrace();
                        break IP;
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, "IO exception occurs", ex);
                        ex.printStackTrace();
                    } finally {
                    }
                    sbPre.append("<br />");
                }
                sbPre.append("<br /><br />");
            }
            httpclient.getConnectionManager().shutdown();
            sb = new StringBuilder("<ul>\n");
            for (String el : peers) {
                sb.append("<li>" + el + "</li>");
            }
            sb.append("</ul>");
            peerList = sb.toString();
            String exPeerList = null;
            if (exIps != null) {
                sb = new StringBuilder("<ul>\n");
                for (String el : exIps) {
                    sb.append("<li>" + el + "</li>");
                }
                sb.append("</ul>");
                exPeerList = sb.toString();
            }
            Calendar cal = new GregorianCalendar();
            Format sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String s = sdf.format(cal.getTime());
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet PLNpeersFinder</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>PLN peers found in " + lockss_xml_url + "</h1>");
            out.println(peerList);
            out.println("<h1>sample daemon-status data from accessible PLN peers</h1>");
            out.println("<blockquote>" + sbPre.toString() + "</blockquote>");
            if (exPeerList != null) {
                out.println("<h1>Unaccessible PLN peers: excluded</h1>");
                out.println(exPeerList);
            }
            out.println("<br /><p>As of:" + s + "</p>");
            out.println("<a href='" + lockss_xml_url + "'>lockss.xml at verity (see the attribute id.initialV3PeerList)</>");
            out.println("</body>");
            out.println("</html>");
        } finally {
            out.close();
        }
    }

    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }

    protected void readLockssConfigFile(URL url, List<String> peers) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new OutputStreamWriter(System.out, "utf8"), true);
            out.println("unicode-output-ready");
        } catch (UnsupportedEncodingException ex) {
            System.out.println(ex.toString());
            return;
        }
        XMLInputFactory xmlif = XMLInputFactory.newInstance();
        xmlif.setProperty("javax.xml.stream.isCoalescing", java.lang.Boolean.TRUE);
        xmlif.setProperty("javax.xml.stream.isNamespaceAware", java.lang.Boolean.TRUE);
        XMLStreamReader xmlr = null;
        BufferedInputStream stream = null;
        long starttime = System.currentTimeMillis();
        out.println("Starting to parse the remote config xml[" + url + "]");
        int elementCount = 0;
        int topPropertyCounter = 0;
        int propertyTagLevel = 0;
        try {
            stream = new BufferedInputStream(url.openStream());
            xmlr = xmlif.createXMLStreamReader(stream, "utf8");
            int eventType = xmlr.getEventType();
            String curElement = "";
            String targetTagName = "property";
            String peerListAttrName = "id.initialV3PeerList";
            boolean sentinel = false;
            boolean valueline = false;
            while (xmlr.hasNext()) {
                eventType = xmlr.next();
                switch(eventType) {
                    case XMLEvent.START_ELEMENT:
                        curElement = xmlr.getLocalName();
                        if (curElement.equals("property")) {
                            topPropertyCounter++;
                            propertyTagLevel++;
                            int count = xmlr.getAttributeCount();
                            if (count > 0) {
                                for (int i = 0; i < count; i++) {
                                    if (xmlr.getAttributeValue(i).equals(peerListAttrName)) {
                                        sentinel = true;
                                        out.println("!!!!!! hit the" + peerListAttrName);
                                        out.println("attr=" + xmlr.getAttributeName(i));
                                        out.println("vl=" + xmlr.getAttributeValue(i));
                                        out.println(">>>>>>>>>>>>>> start :property tag (" + topPropertyCounter + ") >>>>>>>>>>>>>>");
                                        out.println(">>>>>>>>>>>>>> property tag level (" + propertyTagLevel + ") >>>>>>>>>>>>>>");
                                        out.print(xmlr.getAttributeName(i).toString());
                                        out.print("=");
                                        out.print("\"");
                                        out.print(xmlr.getAttributeValue(i));
                                        out.println("");
                                    }
                                }
                            }
                        }
                        if (sentinel && curElement.equals("value")) {
                            valueline = true;
                            String ipAd = xmlr.getElementText();
                            peers.add(ipAd);
                        }
                        break;
                    case XMLEvent.CHARACTERS:
                        break;
                    case XMLEvent.ATTRIBUTE:
                        if (curElement.equals(targetTagName)) {
                        }
                        break;
                    case XMLEvent.END_ELEMENT:
                        if (xmlr.getLocalName().equals("property")) {
                            if (sentinel) {
                                out.println("========= end of the target property element");
                                sentinel = false;
                                valueline = false;
                            }
                            elementCount++;
                            propertyTagLevel--;
                        } else {
                        }
                        break;
                    case XMLEvent.END_DOCUMENT:
                }
            }
        } catch (MalformedURLException ue) {
        } catch (IOException ex) {
        } catch (XMLStreamException ex) {
        } finally {
            if (xmlr != null) {
                try {
                    xmlr.close();
                } catch (XMLStreamException ex) {
                }
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ex) {
                }
            }
        }
    }
}
