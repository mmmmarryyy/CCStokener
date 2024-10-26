package cconverter;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import javax.xml.parsers.*;
import org.xml.sax.InputSource;
import org.w3c.dom.*;
import java.io.*;
import org.xml.sax.SAXException;

/**
 *
 * @author SapunBoj
 */
public class Pxml {

    public String xmlpath = "http://www.ecb.int/stats/eurofxref/eurofxref-daily.xml";

    public String xmlfile = "list.xml";

    public void Pxml() {
    }

    public ArrayList<String> readURL() throws MalformedURLException, IOException {
        ArrayList<String> ret = new ArrayList<String>();
        String str;
        URL url = new URL(xmlpath);
        URLConnection urlc = null;
        DataInputStream dis;
        urlc = url.openConnection();
        urlc.setDoInput(true);
        urlc.setUseCaches(false);
        dis = new DataInputStream(urlc.getInputStream());
        while ((str = dis.readLine()) != null) {
            ret.add(str.trim());
        }
        dis.close();
        return ret;
    }

    public void makeFile() throws IOException {
        String xml = "";
        ArrayList arl = readURL();
        for (int i = 6; i < arl.size() - 1; i++) {
            xml += arl.get(i) + "\n";
            if (i == 7) {
                xml += "<Cube currency='EUR' rate='1.00'/>\n";
                xml += "<Cube currency='BAM' rate='1.95583'/>\n";
            }
        }
        xml.substring(0, xml.length() - 1);
        if (xml.length() > 0) {
            BufferedWriter bw = new BufferedWriter(new FileWriter(xmlfile));
            bw.write(xml);
            bw.close();
        }
    }

    public String readFile() throws FileNotFoundException, IOException {
        String ret = "";
        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(new File(xmlfile))));
        while (dis.available() != 0) {
            ret += dis.readLine();
        }
        dis.close();
        return ret;
    }

    public boolean checkFile() {
        boolean ret = false;
        if (new File(xmlfile).exists()) {
            ret = true;
        }
        return ret;
    }

    public String getLastUpdate() throws IOException, ParserConfigurationException, SAXException {
        String ret = "";
        if (checkFile()) {
            String xml = this.readFile();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml));
            Document doc = db.parse(is);
            NodeList nodes = doc.getElementsByTagName("Cube");
            Element element = (Element) nodes.item(1);
            ret = element.getAttribute("time");
        } else {
            ret = "Never ";
        }
        return ret;
    }
}
