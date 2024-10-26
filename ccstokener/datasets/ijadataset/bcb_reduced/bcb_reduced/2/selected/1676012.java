package org.gdi3d.xnavi.xml.wms;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.gdi3d.xnavi.navigator.Navigator;

public class WebMapServer {

    private WMT_MS_Capabilities wMT_MS_Capabilities;

    public WMT_MS_Capabilities getWMT_MS_Capabilities() {
        return wMT_MS_Capabilities;
    }

    public void setWMT_MS_Capabilities(WMT_MS_Capabilities capabilities) {
        wMT_MS_Capabilities = capabilities;
    }

    public WebMapServer(URL url) {
        URLConnection urlc;
        try {
            urlc = url.openConnection();
            urlc.setReadTimeout(Navigator.TIME_OUT);
            InputStream urlIn = urlc.getInputStream();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(urlIn);
            NodeList WMT_MS_Capabilities_NodeList = document.getElementsByTagName("WMT_MS_Capabilities");
            int NodeList_length = WMT_MS_Capabilities_NodeList.getLength();
            if (NodeList_length == 1) {
                org.w3c.dom.Node WMT_MS_Capabilities_Node = WMT_MS_Capabilities_NodeList.item(0);
                wMT_MS_Capabilities = WMT_MS_Capabilities.parse(WMT_MS_Capabilities_Node);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public WMT_MS_Capabilities getCapabilities() {
        return wMT_MS_Capabilities;
    }
}
