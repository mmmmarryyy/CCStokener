package uk.ac.cam.ch.wwmm;

import org.openscience.cdk.*;
import org.openscience.cdk.io.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.xpath.XPathAPI;
import org.xml.sax.InputSource;
import org.w3c.dom.Document;
import org.dom4j.Node;
import org.w3c.dom.traversal.NodeIterator;

/**
 *  The class is used to retrieve molecules from xindice database
 *
 *@author     Yong Zhangy <yz237@cam.ac.uk>
 *@created    2003-09-17
 */
public class XindiceReader {

    URLConnection connection = null;

    String collection = null;

    String[] childCollectionNames = null;

    int resultCount = -1;

    String server = null;

    String query = null;

    String colname = null;

    int type = 0;

    org.w3c.dom.Element resultList = null;

    String resultString = null;

    BufferedReader br = null;

    public Vector vNames = new Vector();

    /**
     *  Constructor for the XindiceReader object
     */
    public XindiceReader() {
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    /**
     *  Get the sub collection names
     *
     *@param  server           Tomcat server
     *@return                  Description of the Return Value
     *@exception  IOException  Description of the Exception
     */
    public String[] getChildrenCollectionNames(String collection) {
        return childCollectionNames;
    }

    /**
     *  Description of the Method
     *
     *@return                  Description of the Return Value
     *@exception  IOException  Description of the Exception
     */
    public URLConnection makeURLConnection(String server) throws IOException {
        if (server == null) {
            connection = null;
        } else {
            URL url = new URL("http://" + server + "/Bob/QueryXindice");
            connection = url.openConnection();
            connection.setDoOutput(true);
        }
        return connection;
    }

    /**
     *  Gets the connection attribute of the XindiceReader object
     *
     *@return    The connection value
     */
    public URLConnection getConnection() {
        return connection;
    }

    /**
     *  Gets the server attribute of the XindiceReader object
     *
     *@return    The server value
     */
    public String getServer() {
        return server;
    }

    /**
     *  Sets the query attribute of the XindiceReader object
     *
     *@param  q  The new query value
     */
    public void setQuery(String q) throws IOException {
        this.query = URLEncoder.encode("/" + q, "UTF-8");
    }

    /**
     *  Gets the query attribute of the XindiceReader object
     *
     *@return    The query value
     */
    public String getQuery() {
        return query;
    }

    /**
     *  Sets the server attribute of the XindiceReader object
     *
     *@param  s  The new server value
     */
    public void setServer(String s) {
        server = s;
    }

    /**
     *  Gets the collection attribute of the XindiceReader object
     *
     *@return    The collection value
     */
    public String getCollection() {
        return collection;
    }

    /**
     *  Sets the collection attribute of the XindiceReader object
     *
     *@param  c                The new collection value
     *@exception  IOException  Description of the Exception
     */
    public void setCollection(String c) throws IOException {
        collection = c;
        colname = URLEncoder.encode("/" + this.collection, "UTF-8");
    }

    /**
     *  Description of the Method
     *
     *@exception  IOException  Description of the Exception
     */
    public void sendQuery() throws IOException {
        PrintWriter out = new PrintWriter(connection.getOutputStream());
        resultCount = -1;
        out.print("detailed=on");
        out.print("&");
        out.print("xmlOnly=on");
        out.print("&");
        out.print("colName=" + colname);
        out.print("&");
        out.print("xpathString=" + query);
        out.print("&");
        if (type == 2) {
            out.print("type=1");
        } else {
            out.print("type=" + Integer.toString(type));
        }
        out.print("&");
        out.println("query=Query");
        out.close();
    }

    /**
     *  Description of the Method
     *
     *@return                  Description of the Return Value
     *@exception  IOException  Description of the Exception
     */
    public BufferedReader createBufferedReader() throws IOException {
        br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        return br;
    }

    /**
     *  Gets the bufferedReader attribute of the XindiceReader object
     *
     *@return                  The bufferedReader value
     *@exception  IOException  Description of the Exception
     */
    public BufferedReader getBufferedReader() throws IOException {
        if (br == null) {
            this.createBufferedReader();
        }
        return br;
    }

    /**
     *  reades a series of emtries as a result of Xindice Query. They are always
     *  wrapped in list to create a well-formed XML document
     *
     *@return    Element the list
     */
    public org.w3c.dom.Element createResultList() {
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder domBuilder = domFactory.newDocumentBuilder();
            Document doc = domBuilder.parse(new InputSource(br));
            NodeIterator ni = XPathAPI.selectNodeIterator(doc, "/comment()");
            resultList = doc.getDocumentElement();
            String comment = ni.nextNode().getNodeValue();
            Pattern p = Pattern.compile("There are (\\d{1,5}) results!");
            String resultNum = "";
            Matcher match = p.matcher(comment);
            if (match.find()) {
                resultNum = match.group(1);
            } else {
                resultNum = "0";
            }
            try {
                resultCount = Integer.parseInt(resultNum);
            } catch (NumberFormatException nfe) {
                resultCount = -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultList;
    }

    /**
     *  Gets the resultCount attribute of the XindiceReader object
     *
     *@return    The resultCount value
     */
    public int getResultCount() {
        return resultCount;
    }

    /**
     *  Description of the Method
     *
     *@return                String for molecules
     */
    public String getResult() {
        return Utils.node2String((Node) resultList);
    }

    /**
     *  Description of the Method
     *
     *@param  in             Description of the Parameter
     *@return                Description of the Return Value
     *@exception  Exception  Description of the Exception
     */
    public SetOfMolecules readMolecules() throws Exception {
        SetOfMolecules setOfMolecules = null;
        CMLReader reader = null;
        resultString = Utils.node2String((Node) resultList);
        try {
            reader = new CMLReader(new StringReader(resultString));
        } catch (Exception e) {
            e.printStackTrace();
        }
        ChemFile cf = (ChemFile) reader.read((ChemObject) new ChemFile());
        if (cf.getChemSequenceCount() > 0) {
            ChemSequence chemSequence = cf.getChemSequence(0);
            if (chemSequence.getChemModelCount() > 0) {
                ChemModel chemModel = chemSequence.getChemModel(0);
                setOfMolecules = chemModel.getSetOfMolecules();
                if (setOfMolecules.getMoleculeCount() > 0) {
                } else {
                }
            } else {
            }
        } else {
        }
        return setOfMolecules;
    }

    /**
     *  Gets the resultList attribute of the XindiceReader object
     *
     *@return    The resultList value
     */
    public org.w3c.dom.Element getResultList() {
        return resultList;
    }

    /**
     *  Description of the Method
     *
     *@param  collection         Description of the Parameter
     *@param  server             Description of the Parameter
     *@param  query              Description of the Parameter
     *@return                    Description of the Return Value
     *@exception  WWMMException  Description of the Exception
     *@exception  IOException    Description of the Exception
     */
    public org.w3c.dom.Element sendAndReceiveQuery(String server, String collection, String query) throws WWMMException, IOException {
        this.makeURLConnection(server);
        this.setCollection(collection);
        this.setQuery(query);
        this.sendQuery();
        this.createBufferedReader();
        this.createResultList();
        return resultList;
    }

    public void getCollectionName(String server, String collection, String prefix) throws WWMMException, IOException {
        String line = null;
        String name = null;
        BufferedReader br = null;
        switch(type) {
            case 1:
                this.makeURLConnection(server);
                this.setCollection(collection);
                this.setQuery(query);
                this.sendQuery();
                br = this.createBufferedReader();
                line = br.readLine();
                while (line != null) {
                    vNames.addElement(line);
                    line = br.readLine();
                }
                break;
            case 2:
                this.makeURLConnection(server);
                this.setCollection(collection);
                this.setQuery(query);
                this.sendQuery();
                br = this.createBufferedReader();
                line = br.readLine();
                while (line != null) {
                    if (prefix.equals("")) {
                        name = line;
                    } else {
                        name = prefix + "/" + line;
                    }
                    vNames.addElement(name);
                    getCollectionName(server, line, name);
                    line = br.readLine();
                }
                break;
            default:
        }
    }

    /**
     *  The main program for the XindiceReader class
     *
     *@param  args  The command line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("This class is used to query xindice database through tomcat server");
            System.out.println("Usage: uk.ac.cam.ch.wwmm.XindiceReader -SERVER server -COLLECTION collection -QUERY query -TYPE type");
            System.out.println("     Query xindice database through tomcat server");
            System.out.println("     -SERVER");
            System.out.println("       the address of tomcat server(default wwmm.ch.cam.ac.uk)");
            System.out.println("     -COLLECTION");
            System.out.println("       the collection needed to be queried");
            System.out.println("     -QUERY");
            System.out.println("       xpath query string");
            System.out.println("     -TYPE");
            System.out.println("       the type of reading from xindice database");
            System.out.println("      0: default, query using server, collection and query");
            System.out.println("      1: get the names of the direct subcollections of the");
            System.out.println("         collections under /db/wwmm, requring server, collection");
            System.out.println("      2: get the names of all the subcollections of the");
            System.out.println("         collections under /db/wwmm, requring server, collection");
            System.exit(0);
        }
        String server = "wwmm.ch.cam.ac.uk";
        String collection = "nci/entry";
        String query = "//entry[//basic[.='C7H6O2,1H3-5-4H-6(8)2H-3H-7(5)9']]";
        int type = 0;
        int i = 0;
        while (i < args.length) {
            if (args[i].equalsIgnoreCase("-SERVER")) {
                server = args[++i];
                i++;
            } else if (args[i].equalsIgnoreCase("-COLLECTION")) {
                collection = args[++i];
                i++;
            } else if (args[i].equalsIgnoreCase("-QUERY")) {
                query = args[++i];
                i++;
            } else if (args[i].equalsIgnoreCase("-TYPE")) {
                type = Integer.parseInt(args[++i]);
                i++;
            } else {
                System.out.println("Unknown arg: " + args[i++]);
            }
        }
        XindiceReader xr = new XindiceReader();
        switch(type) {
            case 0:
                try {
                    xr.sendAndReceiveQuery(server, collection, query);
                    int count = xr.getResultCount();
                    System.out.println("There are " + count + " results");
                    org.w3c.dom.Element resultList = xr.getResultList();
                    System.out.println("" + resultList.getNodeName());
                    for (i = 0; i < resultList.getChildNodes().getLength(); i++) {
                        org.w3c.dom.Node entry = resultList.getChildNodes().item(i);
                        if (entry instanceof org.w3c.dom.Element) {
                            org.w3c.dom.Element molecule = (org.w3c.dom.Element) entry;
                            System.out.println("Entry " + i + ": " + molecule.getAttribute("name"));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case 1:
                try {
                    xr.setType(type);
                    xr.getCollectionName(server, collection, "");
                    System.out.println("\nSubcollection of /db/wwmm/" + collection + ":");
                    for (i = 0; i < xr.vNames.size(); i++) {
                        String s = (String) xr.vNames.elementAt(i);
                        System.out.println(s);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case 2:
                try {
                    xr.setType(type);
                    xr.getCollectionName(server, collection, "");
                    System.out.println("\nSubcollection of /db/wwmm/" + collection + ":");
                    for (i = 0; i < xr.vNames.size(); i++) {
                        String s = (String) xr.vNames.elementAt(i);
                        System.out.println(s);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
        }
    }
}
