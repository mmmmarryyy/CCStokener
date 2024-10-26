package com.geodar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;
import javax.swing.DefaultListModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.swing.table.DefaultTableModel;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
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
import redstone.xmlrpc.XmlRpcArray;

/**
 *
 * @author Georgi Todorov
 */
public class SysConfig {

    private String settingsdir = System.getProperty("user.home");

    private File file = new File(settingsdir + "/.blogcho.conf");

    private File file2 = new File(settingsdir + "/.blogcho.posts");

    private File file3 = new File(settingsdir + "/.blogcho.categories");

    private String blogurl = null;

    private String bID = "1";

    private String username = null;

    private String password = null;

    private String blogrpcurl = null;

    private String API = "Movable Type";

    private String rsdurl = null;

    private FileOutputStream fout = null;

    private Document document = null;

    private static DefaultTableModel tm;

    private XmlRpcArray catarray = new XmlRpcArray();

    /** Creates a new instance of SysConfig */
    public SysConfig() {
        try {
            boolean succ = file2.createNewFile();
            boolean succ2 = file3.createNewFile();
            boolean success = file.createNewFile();
            if (success) {
                System.out.println("createSettings() called");
                createSettings();
            } else {
                if (file.length() >= 291) {
                    loadSettings();
                } else {
                    file.delete();
                    createSettings();
                }
            }
        } catch (IOException e) {
        }
    }

    private void createSettings() {
        try {
            String[] bup = { "", "", "" };
            ConfigWizard conf = new ConfigWizard();
            System.out.println("new ConfigWizard();");
            bup = conf.autoConfig();
            if (bup[0].equalsIgnoreCase("")) {
                file.delete();
            }
            blogurl = bup[0];
            username = bup[1];
            password = bup[2];
            blogrpcurl = blogurl + "xmlrpc.php";
            rsdurl = blogrpcurl + "?rsd";
            System.out.println("blogurl is " + blogurl);
            try {
                rsdurl = getRSD(blogurl);
                System.out.println("RSD is " + rsdurl);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            try {
                API = getAPI(rsdurl);
                System.out.println("API is " + API);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            fout = new FileOutputStream(settingsdir + "/.blogcho.conf");
            DocumentBuilderFactory docfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docbuil = docfac.newDocumentBuilder();
            document = docbuil.newDocument();
            org.w3c.dom.Element root = document.createElement("configuration");
            org.w3c.dom.Element settings_dir = document.createElement("settings_dir");
            org.w3c.dom.Element blog_url = document.createElement("blog_url");
            org.w3c.dom.Element blog_ID = document.createElement("blog_ID");
            org.w3c.dom.Element uname = document.createElement("username");
            org.w3c.dom.Element pword = document.createElement("password");
            org.w3c.dom.Element blog_control_url = document.createElement("blog_control_url");
            org.w3c.dom.Element blog_api = document.createElement("blog_api");
            org.w3c.dom.Element blg_rsd_url = document.createElement("blg_rsd_url");
            document.appendChild(root);
            root.appendChild(settings_dir);
            root.appendChild(blog_url);
            root.appendChild(blog_ID);
            root.appendChild(uname);
            root.appendChild(pword);
            root.appendChild(blog_control_url);
            root.appendChild(blog_api);
            root.appendChild(blg_rsd_url);
            settings_dir.appendChild(document.createTextNode(settingsdir));
            blog_url.appendChild(document.createTextNode(blogurl));
            blog_ID.appendChild(document.createTextNode(bID));
            uname.appendChild(document.createTextNode(username));
            pword.appendChild(document.createTextNode(password));
            blog_control_url.appendChild(document.createTextNode(blogrpcurl));
            blog_api.appendChild(document.createTextNode(API));
            blg_rsd_url.appendChild(document.createTextNode(rsdurl));
            storeConfig();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadSettings() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            document = factory.newDocumentBuilder().parse(file);
            settingsdir = document.getDocumentElement().getElementsByTagName("settings_dir").item(0).getChildNodes().item(0).getTextContent();
            blogurl = document.getDocumentElement().getElementsByTagName("blog_url").item(0).getChildNodes().item(0).getTextContent();
            bID = document.getDocumentElement().getElementsByTagName("blog_ID").item(0).getChildNodes().item(0).getTextContent();
            username = document.getDocumentElement().getElementsByTagName("username").item(0).getChildNodes().item(0).getTextContent();
            password = document.getDocumentElement().getElementsByTagName("password").item(0).getChildNodes().item(0).getTextContent();
            blogrpcurl = document.getDocumentElement().getElementsByTagName("blog_control_url").item(0).getChildNodes().item(0).getTextContent();
            API = document.getDocumentElement().getElementsByTagName("blog_api").item(0).getChildNodes().item(0).getTextContent();
            rsdurl = document.getDocumentElement().getElementsByTagName("blg_rsd_url").item(0).getChildNodes().item(0).getTextContent();
        } catch (Exception e) {
        }
    }

    public void editSetting(String elname, String elvalue) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            document = factory.newDocumentBuilder().parse(file);
            document.getDocumentElement().getElementsByTagName(elname).item(0).getChildNodes().item(0).setTextContent(elvalue);
            storeConfig();
        } catch (Exception e) {
        }
    }

    public void storePosts(DefaultTableModel tableModel) {
        int rows = tableModel.getRowCount();
        int cols = tableModel.getColumnCount();
        Vector dv = tableModel.getDataVector();
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(settingsdir + "/.blogcho.posts"));
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    out.write(((Vector) dv.elementAt(i)).elementAt(j).toString());
                    if (j < cols - 1) {
                        out.write(",");
                    }
                }
                out.write("\n");
            }
            out.close();
        } catch (IOException e) {
        }
    }

    public void editPost(Integer pID, String title, String description) {
        try {
            BufferedReader out = new BufferedReader(new FileReader(settingsdir + "/.blogcho.posts"));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    public void storeCategories(DefaultListModel myListModel, Vector catids) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(settingsdir + "/.blogcho.categories"));
            for (int i = 0; i < myListModel.getSize(); i++) {
                out.write(myListModel.get(i).toString());
                out.write(",");
                out.write((String) catids.get(i));
                out.write("\n");
            }
            out.close();
        } catch (IOException e) {
        }
    }

    public DefaultListModel loadCategories() {
        try {
            catarray.clear();
            DefaultListModel myListModel = new DefaultListModel();
            BufferedReader in = new BufferedReader(new FileReader(settingsdir + "/.blogcho.categories"));
            String str;
            while ((str = in.readLine()) != null) {
                redstone.xmlrpc.XmlRpcStruct catstruct = new redstone.xmlrpc.XmlRpcStruct();
                String[] catinfo = str.split(",");
                myListModel.addElement(catinfo[0]);
                catstruct.put("categoryId", catinfo[1]);
                catstruct.put("categoryName", catinfo[0]);
                catarray.add(catstruct);
            }
            return myListModel;
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public DefaultTableModel loadPosts() {
        try {
            Vector<Object> dv = new Vector<Object>();
            String[] temp;
            Vector<String> cn = new Vector<String>();
            cn.add("ID");
            cn.add("Title");
            cn.add("Date");
            cn.add("Category");
            BufferedReader in = new BufferedReader(new FileReader(settingsdir + "/.blogcho.posts"));
            String str;
            while ((str = in.readLine()) != null) {
                temp = str.split(",");
                Vector<Object> tv = new Vector<Object>();
                for (int i = 0; i < temp.length; i++) {
                    tv.add(temp[i]);
                }
                dv.add(tv);
            }
            in.close();
            if (dv.size() < 1) {
                return null;
            }
            DefaultTableModel tm = new DefaultTableModel(dv, cn);
            return tm;
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public void storeConfig() {
        System.out.println("Storing Config");
        DataOutputStream outData = new DataOutputStream(fout);
        if (document != null) {
            try {
                Source source = new DOMSource(document);
                Result result = new StreamResult(file);
                Transformer xformer = TransformerFactory.newInstance().newTransformer();
                xformer.setOutputProperty(javax.xml.transform.OutputKeys.METHOD, "xml");
                xformer.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "utf-8");
                xformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                xformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
                xformer.transform(source, result);
            } catch (TransformerConfigurationException e) {
            } catch (TransformerException e) {
            }
        } else {
            System.out.println("Fout is null");
        }
    }

    public static String getAPI(String url) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = getDocument(db, url);
        String API = null;
        if (document != null) {
            NodeList matchNodes = document.getElementsByTagName("api");
            Node firstElement = matchNodes.item(0);
            NamedNodeMap nnm = firstElement.getAttributes();
            if (nnm != null) {
                int nnmLenght = nnm.getLength();
                for (int j = 0; j < nnmLenght; j++) {
                    String pref = nnm.getNamedItem("preferred").getNodeValue().toString();
                    if (0 == pref.compareToIgnoreCase("true")) {
                        API = nnm.getNamedItem("name").getNodeValue().toString();
                    }
                }
            } else {
                System.out.println("OOPS. NamedNodeMap is NULL!");
            }
        } else {
            System.out.println("Document is NULL");
        }
        return API;
    }

    public String getRSD(String url) throws Exception {
        String RSD = null;
        URL yahoo = new URL(url);
        URLConnection yc = yahoo.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
        String inputLine;
        String linkline = null;
        while ((inputLine = in.readLine()) != null) {
            if (inputLine.trim().length() > 5 && inputLine.trim().substring(0, 5).compareTo("<link") == 0) {
                String[] tokens = inputLine.split(" ");
                for (int i = 0; i < tokens.length; i++) {
                    if (tokens[i].compareToIgnoreCase("rel=\"EditURI\"") == 0) {
                        linkline = inputLine.trim();
                    }
                }
            }
        }
        in.close();
        String[] tokens = linkline.split(" ");
        for (int i = 0; i < tokens.length; i++) {
            String[] tok = tokens[i].split("=\"");
            if (tok[0].compareToIgnoreCase("href") == 0) {
                RSD = tok[1].substring(0, tok[1].length() - 1);
            }
            try {
                bID = RSD.split("=")[1];
            } catch (Exception e) {
            }
        }
        return RSD;
    }

    public String getCategoryID(String catname) {
        for (int i = 0; i < catarray.size(); i++) {
            if (catarray.getStruct(i).getString("categoryName").compareToIgnoreCase(catname) == 0) {
                return catarray.getStruct(i).getString("categoryId");
            }
        }
        return null;
    }

    public static Document getDocument(DocumentBuilder db, String urlString) {
        try {
            URL url = new URL(urlString);
            try {
                URLConnection URLconnection = url.openConnection();
                HttpURLConnection httpConnection = (HttpURLConnection) URLconnection;
                int responseCode = httpConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream in = httpConnection.getInputStream();
                    try {
                        Document doc = db.parse(in);
                        return doc;
                    } catch (org.xml.sax.SAXException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("HTTP connection response != HTTP_OK");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getAPI() {
        return API;
    }

    public String getbID() {
        return bID;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getURL() {
        return blogrpcurl;
    }
}
