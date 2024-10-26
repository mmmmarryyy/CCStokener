package com.webstersmalley.jtv.radiotimes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.webstersmalley.jtv.Program;
import com.webstersmalley.jtv.XMLWriter;

/**
 * @author Matthew Smalley
 */
public class ListingsReader implements Runnable {

    private static String urlPrefix = "http://xmltv.radiotimes.com/xmltv/";

    private String channelId;

    private Document document;

    public ListingsReader(String channelId, Document document) {
        this.channelId = channelId;
        this.document = document;
    }

    private Set read() throws IOException {
        URL url = new URL(urlPrefix + channelId + ".dat");
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String line = in.readLine();
        Set programs = new HashSet();
        while (line != null) {
            String[] values = line.split("~");
            if (values.length != 23) {
                throw new RuntimeException("error: incorrect format for radiotimes information");
            }
            Program program = new RadioTimesProgram(values, channelId);
            programs.add(program);
            line = in.readLine();
        }
        return programs;
    }

    public void run() {
        Set programs;
        try {
            programs = read();
        } catch (Exception e) {
            return;
        }
        NodeList list = document.getElementsByTagName("tv");
        Element root = (Element) list.item(0);
        if (root == null) {
            root = document.createElement("tv");
            document.appendChild(root);
        }
        Iterator it = programs.iterator();
        while (it.hasNext()) {
            Program program = (Program) it.next();
            root.appendChild(program.createElement(document));
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("usage: ListingsReader channelId filename [stylesheet]");
            System.exit(1);
        }
        try {
            String channelId = args[0];
            String filename = args[1];
            Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            ListingsReader reader = new ListingsReader(channelId, d);
            reader.run();
            if (args.length > 2) {
                XMLWriter.writeXMLToFile(d, filename, args[2]);
            } else {
                XMLWriter.writeXMLToFile(d, filename);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
