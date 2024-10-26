package com.softwaresmithy.lib;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class ScrapeLib {

    /**
	 * @param args
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
    public static void main(String[] args) throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        HttpHost proxy = new HttpHost("proxy.houston.hp.com", 8080);
        httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        String token = "";
        if (token != null) {
            InputStream is = ScrapeLib.class.getResourceAsStream("/patronInfo.html");
            DOMParser parser = new DOMParser();
            parser.parse(new InputSource(is));
            print(parser.getDocument().getDocumentElement(), "");
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile("//TR[@class=\"patFuncEntry\"]");
            NodeList results = (NodeList) expr.evaluate(parser.getDocument().getDocumentElement(), XPathConstants.NODESET);
            System.out.println("PRINTING MATCHES:");
            for (int i = 0; i < results.getLength(); i++) {
                System.out.println("'" + results.item(i).getNodeName() + "'");
            }
        }
    }

    private static void getPatronInfo(HttpClient client) throws Exception {
        HttpGet httpget = new HttpGet("http://libsys.arlingtonva.us/patroninfo~S1/1079675/items");
        HttpResponse response = client.execute(httpget);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            System.out.println(EntityUtils.toString(entity));
        }
        EntityUtils.consume(entity);
    }

    private static void login(HttpClient client, String token) throws Exception {
        HttpPost login = new HttpPost("https://libsys.arlingtonva.us/iii/cas/login?service=http&amp;scope=1");
        List<NameValuePair> formParams = new ArrayList<NameValuePair>();
        formParams.add(new BasicNameValuePair("code", "202002686286"));
        formParams.add(new BasicNameValuePair("pin", "3128"));
        formParams.add(new BasicNameValuePair("_eventId", "submit"));
        formParams.add(new BasicNameValuePair("lt", token));
        UrlEncodedFormEntity form = new UrlEncodedFormEntity(formParams, "UTF-8");
        login.setEntity(form);
        HttpResponse response = client.execute(login);
        EntityUtils.consume(response.getEntity());
        Header[] headers = response.getAllHeaders();
        System.out.println("HEADERS: ");
        for (Header hdr : headers) {
            System.out.println(hdr.getName() + ": " + hdr.getValue());
        }
    }

    private static String getToken(HttpClient client) throws Exception {
        String token = null;
        HttpGet httpget = new HttpGet("https://libsys.arlingtonva.us/iii/cas/login?service=http&scope=1");
        HttpResponse response = client.execute(httpget);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            entity = new BufferedHttpEntity(entity);
        }
        if (entity != null) {
            String resp = EntityUtils.toString(entity);
            EntityUtils.consume(entity);
            Pattern pattern = Pattern.compile("<input .*name=\"lt\".*value=\"(.*)\".*/>");
            Matcher matcher = pattern.matcher(resp);
            if (matcher.find()) {
                token = matcher.group(1);
            }
        }
        return token;
    }

    public static void print(Node node, String indent) {
        System.out.println(indent + "'" + node.getNodeName() + "'");
        Node child = node.getFirstChild();
        while (child != null) {
            print(child, indent + " ");
            child = child.getNextSibling();
        }
    }

    public static void printChildren(Node node) {
        System.out.println(node.getNodeName());
        Node child = node.getFirstChild();
        while (child != null) {
            System.out.println(" " + node.getNodeName());
            child = child.getNextSibling();
        }
    }
}
