package de.dirkdittmar.flickr.group.comment.dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import de.dirkdittmar.flickr.group.comment.exception.FlickrException;
import de.dirkdittmar.flickr.group.comment.exception.NetworkException;
import de.dirkdittmar.flickr.group.comment.exception.RemoteException;

public class FlickrDaoImpl implements FlickrDao {

    private static Logger log = Logger.getLogger(FlickrDaoImpl.class);

    private static final String REST_ADR = "http://api.flickr.com/services/rest/";

    private HttpClient httpClient;

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    private void checkFlickrError(Document doc) throws FlickrException {
        Element rootElem = doc.getDocumentElement();
        String stat = rootElem.getAttribute("stat");
        if (stat.equals("fail")) {
            NodeList nodeList = rootElem.getChildNodes();
            String code = "-1";
            String message = "unknown";
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeName().equals("err")) {
                    Element errElem = (Element) node;
                    code = errElem.getAttribute("code");
                    message = errElem.getAttribute("msg");
                }
            }
            throw new FlickrException(Integer.valueOf(code), message);
        }
    }

    @Override
    public Document sendGetRequest(String methodName, Map<String, String> params) throws FlickrException, RemoteException, NetworkException {
        String uri = buildGetUri(methodName, params);
        HttpGet httpGet = new HttpGet(uri);
        return sendRequest(httpGet);
    }

    private Document sendRequest(HttpUriRequest httpUriRequest) throws FlickrException, RemoteException, NetworkException {
        InputStream responseBodyAsStream = null;
        try {
            log.info(httpUriRequest.getMethod() + ": " + httpUriRequest.getURI());
            HttpResponse httpResponse = httpClient.execute(httpUriRequest);
            HttpEntity httpEntity = httpResponse.getEntity();
            responseBodyAsStream = httpEntity.getContent();
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(responseBodyAsStream);
            checkFlickrError(doc);
            return doc;
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException(ex);
        } catch (IOException e) {
            throw new NetworkException("Network communication error!", e);
        } catch (SAXException e) {
            throw new RemoteException("XML-Error in Flickr-Response! ", e);
        } finally {
            if (responseBodyAsStream != null) {
                try {
                    responseBodyAsStream.close();
                } catch (IOException e) {
                    log.warn("could not close InputStream... ", e);
                }
            }
            httpUriRequest.abort();
        }
    }

    private String buildGetUri(String methodName, Map<String, String> params) {
        StringBuilder uri = new StringBuilder(REST_ADR + "?method=" + methodName);
        for (Entry<String, String> entry : params.entrySet()) {
            uri.append("&");
            uri.append(entry.getKey());
            uri.append("=");
            uri.append(entry.getValue());
        }
        return uri.toString();
    }

    @Override
    public byte[] download(URI uri) throws NetworkException {
        log.info("download: " + uri);
        HttpGet httpGet = new HttpGet(uri.toString());
        try {
            HttpResponse httpResponse = httpClient.execute(httpGet);
            return EntityUtils.toByteArray(httpResponse.getEntity());
        } catch (IOException e) {
            throw new NetworkException(e);
        } finally {
            httpGet.abort();
        }
    }

    @Override
    public Document sendPostRequest(String methodName, Map<String, String> params) throws FlickrException, RemoteException, NetworkException {
        HttpPost httpPost = new HttpPost(REST_ADR);
        final List<NameValuePair> paramList = new ArrayList<NameValuePair>(params.size());
        paramList.add(new BasicNameValuePair("method", methodName));
        for (Map.Entry<String, String> entry : params.entrySet()) {
            final NameValuePair pair = new BasicNameValuePair(entry.getKey(), entry.getValue());
            paramList.add(pair);
        }
        try {
            HttpEntity httpEntity = new UrlEncodedFormEntity(paramList, "UTF-8");
            httpPost.setEntity(httpEntity);
        } catch (UnsupportedEncodingException e) {
            log.fatal("caught a ", e);
            throw new RuntimeException(e);
        }
        return sendRequest(httpPost);
    }
}
