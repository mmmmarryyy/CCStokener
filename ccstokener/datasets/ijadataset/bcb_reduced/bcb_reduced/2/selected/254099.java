package com.webstersmalley.jtv.radiotimes;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.webstersmalley.jtv.Channel;
import com.webstersmalley.jtv.XMLWriter;

/**
 * @author Matthew Smalley
 */
public class ChannelsReader {

    private static String urlPrefix = "http://xmltv.radiotimes.com/xmltv";

    private static String channelsUrl = urlPrefix + "/channels.dat";

    public void getChannelsList(Document d) throws IOException, ParserConfigurationException {
        NodeList list = d.getElementsByTagName("tv");
        Element root = (Element) list.item(0);
        if (root == null) {
            root = d.createElement("tv");
            d.appendChild(root);
        }
        Set channels = read();
        Iterator it = channels.iterator();
        while (it.hasNext()) {
            Channel channel = (Channel) it.next();
            root.appendChild(channel.createElement(d));
        }
    }

    public Set read() throws IOException, ParserConfigurationException {
        URL url = new URL(channelsUrl);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String line = in.readLine();
        Set channels = new HashSet();
        while (line != null) {
            Channel channel = new Channel();
            String[] values = line.split("\\|");
            channel.setId(values[0]);
            channel.setDisplayName(values[1]);
            channels.add(channel);
            line = in.readLine();
        }
        return channels;
    }

    public static void main(String[] args) {
        ChannelsReader reader = new ChannelsReader();
        try {
            Set channels = reader.read();
            Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            reader.getChannelsList(d);
            String filename = "xmltv.xml";
            if (args.length > 0) {
                filename = args[0];
            }
            XMLWriter.writeXMLToFile(d, filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
