package info.clockworksapple.android.barsearch.common;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class BarDetail {

    private String url = null;

    public BarDetail(String url) {
        this.url = url;
    }

    /**
     * 
     * @return
     */
    public String getPhoneNo() {
        HttpClient httpclient = null;
        String phoneNo = "";
        try {
            httpclient = new DefaultHttpClient();
            URI uri = getURI(url);
            HttpGet httpget = new HttpGet(uri);
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docbuilder = dbfactory.newDocumentBuilder();
            Document doc = docbuilder.parse(entity.getContent());
            NodeList nodeList = doc.getElementsByTagName("a");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                NamedNodeMap nnm = node.getAttributes();
                if (nnm.getNamedItem("href") == null) {
                    continue;
                }
                String value = nnm.getNamedItem("href").getNodeValue();
                if (value.indexOf("TEL:") != -1) {
                    phoneNo = value.split(":")[1];
                    break;
                }
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return phoneNo;
    }

    /**
     * 
     * @param url
     * @return
     */
    private URI getURI(String url) {
        List<String> list = new ArrayList<String>();
        String[] schema = url.split("/");
        for (int i = 0; i < schema.length; i++) {
            if (!schema[i].equals("")) {
                list.add(schema[i]);
            }
        }
        URI uri = null;
        try {
            uri = URIUtils.createURI("http", list.get(1), -1, createPath(list), null, null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return uri;
    }

    private String createPath(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < list.size(); i++) {
            sb.append("/");
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}
