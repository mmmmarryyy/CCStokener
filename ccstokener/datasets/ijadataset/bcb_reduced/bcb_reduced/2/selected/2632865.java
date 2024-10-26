package edu.tufts.osidimpl.repository.fedora_2_2;

import java.net.*;
import java.io.*;
import java.util.*;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.XMLConstants;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import fedora.server.types.gen.*;

public class FedoraRESTSearchAdapter {

    public static final String PID = "pid";

    public static final String TITLE = "title";

    public static final String CMODEL = "cModel";

    public static final String SEARCH_STRING = "/fedora/search?pid=true&title=true&xml=true&cModel=true&description=true&maxResults=100&terms=";

    public static final String SEARCH_ADVANCED = "/fedora/search?pid=true&title=true&xml=true&cModel=true&description=true&maxResults=100&query=";

    public static final String SEARCH_RESUME = "/fedora/search?xml=true&sessionToken=";

    public static final String DS_ID_ATTRIBUTE = "dsid";

    public static final String WILDCARD = "*";

    /** Creates a new instance of FedoraRESTSearchAdapter */
    public FedoraRESTSearchAdapter() {
    }

    public static org.osid.repository.AssetIterator search(Repository repository, SearchCriteria lSearchCriteria) throws org.osid.repository.RepositoryException {
        try {
            NodeList fieldNode = null;
            if (lSearchCriteria.getSearchOperation() == SearchCriteria.FIND_OBJECTS) {
                URL url = new URL("http", repository.getAddress(), repository.getPort(), SEARCH_STRING + URLEncoder.encode(lSearchCriteria.getKeywords() + WILDCARD, "ISO-8859-1"));
                XPathFactory factory = XPathFactory.newInstance();
                XPath xPath = factory.newXPath();
                xPath.setNamespaceContext(new FedoraNamespaceContext());
                InputSource inputSource = new InputSource(url.openStream());
                fieldNode = (NodeList) xPath.evaluate("/pre:result/pre:resultList/pre:objectFields", inputSource, XPathConstants.NODESET);
                if (fieldNode.getLength() > 0) {
                    inputSource = new InputSource(url.openStream());
                    XPathExpression xSession = xPath.compile("//pre:token/text()");
                    String token = xSession.evaluate(inputSource);
                    lSearchCriteria.setToken(token);
                }
            }
            return getAssetIterator(repository, fieldNode);
        } catch (Throwable t) {
            throw wrappedException("search", t);
        }
    }

    public static org.osid.repository.AssetIterator advancedSearch(Repository repository, SearchCriteria lSearchCriteria) throws org.osid.repository.RepositoryException {
        try {
            String query = getQueryFromConditions(lSearchCriteria.getConditions());
            NodeList fieldNode = null;
            URL url = new URL("http", repository.getAddress(), repository.getPort(), SEARCH_ADVANCED + query);
            System.out.println("Advanced search url: " + url);
            XPathFactory factory = XPathFactory.newInstance();
            XPath xPath = factory.newXPath();
            xPath.setNamespaceContext(new FedoraNamespaceContext());
            InputSource inputSource = new InputSource(url.openStream());
            fieldNode = (NodeList) xPath.evaluate("/pre:result/pre:resultList/pre:objectFields", inputSource, XPathConstants.NODESET);
            if (fieldNode.getLength() > 0) {
                inputSource = new InputSource(url.openStream());
                XPathExpression xSession = xPath.compile("//pre:token/text()");
                String token = xSession.evaluate(inputSource);
                lSearchCriteria.setToken(token);
            }
            return getAssetIterator(repository, fieldNode);
        } catch (Throwable t) {
            throw wrappedException("search", t);
        }
    }

    public static String getQueryFromConditions(Condition[] conds) throws org.osid.repository.RepositoryException {
        try {
            String query = new String();
            for (int i = 0; i < conds.length; i++) {
                query += conds[i].getProperty();
                query += "%7E";
                query += URLEncoder.encode(conds[i].getValue(), "ISO-8859-1");
                query += "%20";
            }
            query = query.substring(0, query.length() - 3);
            return query;
        } catch (Throwable t) {
            throw wrappedException("FedoraRESTSearchAdapter.getQueryFromConditions", t);
        }
    }

    private static org.osid.repository.AssetIterator getAssetIterator(Repository repository, NodeList fieldNode) throws org.osid.repository.RepositoryException {
        List<Asset> resultList = new ArrayList<Asset>();
        try {
            if (fieldNode.getLength() == 0) {
                System.out.println("search return no results");
            }
            for (int i = 0; i < fieldNode.getLength(); i++) {
                Node n = fieldNode.item(i);
                String pid = "Not Defined";
                String title = "No Title";
                String cModel = "None";
                for (int j = 0; j < n.getChildNodes().getLength(); j++) {
                    org.w3c.dom.Node e = n.getChildNodes().item(j);
                    if (e.getNodeType() == Node.ELEMENT_NODE) {
                        if (e.getNodeName().toString().equals(PID)) {
                            pid = e.getFirstChild().getNodeValue();
                        }
                        if (e.getNodeName().toString().equals(TITLE)) {
                            title = e.getFirstChild().getNodeValue();
                        }
                        if (e.getNodeName().toString().equals(CMODEL)) {
                            cModel = e.getFirstChild().getNodeValue();
                        }
                    }
                }
                resultList.add(new Asset(repository, pid, title, repository.getAssetType(cModel)));
            }
            return new AssetIterator(resultList);
        } catch (Throwable t) {
            throw wrappedException("getAssetIterator", t);
        }
    }

    public static List<String> getDataStreams(String dSUrl) throws org.osid.repository.RepositoryException {
        List<String> dataStreams = new ArrayList<String>();
        try {
            URL url = new URL(dSUrl);
            XPathFactory factory = XPathFactory.newInstance();
            XPath xPath = factory.newXPath();
            InputSource inputSource = new InputSource(url.openStream());
            NodeList dSNodes = (NodeList) xPath.evaluate("/objectDatastreams/datastream", inputSource, XPathConstants.NODESET);
            for (int i = 0; i < dSNodes.getLength(); i++) {
                Node n = dSNodes.item(i);
                dataStreams.add(n.getAttributes().getNamedItem(DS_ID_ATTRIBUTE).getNodeValue());
            }
        } catch (Throwable t) {
            throw wrappedException("getDataStreams", t);
        }
        return dataStreams;
    }

    private static org.osid.repository.RepositoryException wrappedException(String method, Throwable cause) {
        cause.printStackTrace();
        org.osid.repository.RepositoryException re = new org.osid.repository.RepositoryException("FedoraRESTSearchAdapter." + method + "; cause is " + cause);
        re.initCause(cause);
        return re;
    }

    public static void testSearch() throws Exception {
        URL url = new URL("http://dl.tufts.edu:8080/" + SEARCH_STRING + URLEncoder.encode("street", "ISO-8859-1"));
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        xPath.setNamespaceContext(new FedoraNamespaceContext());
        InputSource inputSource = new InputSource(url.openStream());
        XPathExpression xSession = xPath.compile("//pre:expirationDate/text()");
        String date = xSession.evaluate(inputSource);
        System.out.println("Expiration Date:" + date);
        inputSource = new InputSource(url.openStream());
        NodeList fieldNode = (NodeList) xPath.evaluate("/pre:result/pre:resultList/pre:objectFields", inputSource, XPathConstants.NODESET);
        for (int i = 0; i < fieldNode.getLength(); i++) {
            Node n = fieldNode.item(i);
            System.out.println(i + "name:" + n.getNodeName() + "value: " + n.getNodeValue() + " Type: " + n.getNodeType());
            for (int j = 0; j < n.getChildNodes().getLength(); j++) {
                org.w3c.dom.Node e = n.getChildNodes().item(j);
                if (e.getNodeType() == Node.ELEMENT_NODE) {
                    System.out.println("Name: " + e.getNodeName() + " value: " + e.getFirstChild().getNodeValue() + " Type: " + e.getNodeType());
                }
            }
        }
    }

    public static void main(String args[]) throws Exception {
        FedoraRESTSearchAdapter.testSearch();
    }
}

class FedoraNamespaceContext implements NamespaceContext {

    public String getNamespaceURI(String prefix) {
        if (prefix == null) throw new NullPointerException("Null prefix"); else if ("pre".equals(prefix)) return "http://www.fedora.info/definitions/1/0/types/"; else if ("xml".equals(prefix)) return XMLConstants.XML_NS_URI;
        return XMLConstants.NULL_NS_URI;
    }

    public String getPrefix(String uri) {
        throw new UnsupportedOperationException();
    }

    public Iterator getPrefixes(String uri) {
        throw new UnsupportedOperationException();
    }
}
