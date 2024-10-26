package org.slasoi.infrastructure.servicemanager.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.log4j.Logger;
import org.slasoi.infrastructure.servicemanager.occi.OcciClient;
import org.slasoi.infrastructure.servicemanager.occi.StatusCode;
import org.slasoi.infrastructure.servicemanager.occi.impl.Multipart;
import org.slasoi.infrastructure.servicemanager.occi.types.Response;
import org.slasoi.infrastructure.servicemanager.occi.types.Terms;

public class OcciClientImpl implements OcciClient {

    Logger logger = Logger.getLogger(OcciClientImpl.class.getName());

    private String hostname;

    private int port;

    private String resource;

    private String serviceResource;

    public String getServiceResource() {
        return serviceResource;
    }

    public void setServiceResource(String serviceResource) {
        this.serviceResource = serviceResource;
    }

    private static String lineSeparator = System.getProperty("line.separator");

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String delete(String request) {
        HttpClient httpClient = new DefaultHttpClient();
        HttpDelete httpDelete = new HttpDelete(request);
        logger.info("executing request - " + httpDelete.getRequestLine());
        HttpResponse response = null;
        try {
            response = httpClient.execute(httpDelete);
        } catch (ClientProtocolException e) {
            logger.error(e);
            e.printStackTrace();
        } catch (IOException e) {
            logger.error(e);
            e.printStackTrace();
        }
        if (response != null) {
            int statusCode = response.getStatusLine().getStatusCode();
            logger.info("----------------------------------------");
            logger.info("StatusLine - (full) - " + response.getStatusLine());
            logger.info("	StatusCode - " + statusCode);
            logger.info("	Reason - " + response.getStatusLine().getReasonPhrase());
            logger.info("	Protocol - " + response.getStatusLine().getProtocolVersion().toString());
            logger.info("----------------------------------------");
            if (StatusCode.validate(statusCode)) {
                logger.info("Response Validated");
                return response.getStatusLine().toString();
            } else {
                logger.info("Response NOT Validated");
                return null;
            }
        }
        return null;
    }

    /**
	 * 
	 */
    public String get(String request) {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet("http://" + hostname + ":" + port + request);
        logger.info("executing request - " + httpGet.getRequestLine());
        HttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(httpGet);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            logger.error(e);
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e);
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (httpResponse != null) {
            HttpEntity entity = httpResponse.getEntity();
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            logger.info("----------------------------------------");
            logger.info("StatusLine - (full) - " + httpResponse.getStatusLine());
            logger.info("	StatusCode - " + statusCode);
            logger.info("	Reason - " + httpResponse.getStatusLine().getReasonPhrase());
            logger.info("	Protocol - " + httpResponse.getStatusLine().getProtocolVersion().toString());
            logger.info("----------------------------------------");
            if (StatusCode.validate(statusCode)) {
                logger.info("Response Validated");
            } else {
                logger.error("Response NOT Validated");
                return null;
            }
            Header[] headers = httpResponse.getAllHeaders();
            for (int i = 0; i < headers.length; i++) {
                logger.info("Header - " + headers[i].getName() + "/" + headers[i].getValue());
            }
            InputStream is = null;
            try {
                is = entity.getContent();
            } catch (IllegalStateException e) {
                logger.error(e);
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                logger.error(e);
                e.printStackTrace();
                return null;
            }
            if (is != null) {
                String line;
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    while ((line = reader.readLine()) != null) {
                        logger.info("line " + line);
                        sb.append(line + lineSeparator);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
                httpClient.getConnectionManager().shutdown();
                return sb.toString();
            } else {
                httpClient.getConnectionManager().shutdown();
                return null;
            }
        } else {
            return null;
        }
    }

    public Map<String, String> getHeaders(String request) {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet("http://" + hostname + ":" + port + request);
        logger.info("executing request - " + httpGet.getRequestLine());
        HttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(httpGet);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            logger.error(e);
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e);
            return null;
        }
        if (httpResponse != null) {
            HttpEntity entity = httpResponse.getEntity();
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            logger.info("----------------------------------------");
            logger.info("StatusLine - (full) - " + httpResponse.getStatusLine());
            logger.info("	StatusCode - " + statusCode);
            logger.info("	Reason - " + httpResponse.getStatusLine().getReasonPhrase());
            logger.info("	Protocol - " + httpResponse.getStatusLine().getProtocolVersion().toString());
            logger.info("----------------------------------------");
            if (StatusCode.validate(statusCode)) {
                logger.info("Response Validated");
            } else {
                logger.error("Response NOT Validated");
                return null;
            }
            Map<String, String> headers = new HashMap<String, String>();
            Header[] httpHeaders = httpResponse.getAllHeaders();
            for (int i = 0; i < httpHeaders.length; i++) {
                logger.info("Header - " + httpHeaders[i].getName() + "/" + httpHeaders[i].getValue());
                headers.put(httpHeaders[i].getName(), httpHeaders[i].getValue());
            }
            httpClient.getConnectionManager().shutdown();
            return headers;
        }
        return null;
    }

    public String put(String resourceID, Map<String, String> headersMap) {
        HttpClient httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter("http.useragent", "sla@soi OCCI Client v0.2");
        HttpPut httpPut = new HttpPut(resourceID);
        List<Header> headersList = this.convert2Headers(headersMap);
        for (Iterator<Header> iterator = headersList.iterator(); iterator.hasNext(); ) {
            httpPut.addHeader(iterator.next());
        }
        logger.info(httpPut.getRequestLine());
        logger.info(httpPut.getAllHeaders());
        Header[] headersArray = httpPut.getAllHeaders();
        String[] fields = { Response.Location };
        HashMap<String, String> occiHeaders = new HashMap<String, String>();
        for (int H = 0; H < headersArray.length; H++) {
            Header header = headersArray[H];
            logger.info("header - request  -" + header.toString());
            logger.info("	headerName - " + header.getName());
            logger.info("	headerValue - " + header.getValue());
        }
        String statusLine = null;
        try {
            HttpResponse httpResponse = httpClient.execute(httpPut);
            statusLine = httpResponse.getStatusLine().toString();
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            logger.info("----------------------------------------");
            logger.info("StatusLine - (full) - " + httpResponse.getStatusLine());
            logger.info("	StatusCode - " + statusCode);
            logger.info("	Reason - " + httpResponse.getStatusLine().getReasonPhrase());
            logger.info("	Protocol - " + httpResponse.getStatusLine().getProtocolVersion().toString());
            logger.info("----------------------------------------");
            if (StatusCode.validate(statusCode)) {
                logger.info("Response Validated");
            } else {
                logger.error("Response NOT Validated");
                return null;
            }
            Header[] headers = httpResponse.getAllHeaders();
            for (int i = 0; i < headers.length; i++) {
                Header header = headers[i];
                logger.info("header - response - " + header.toString());
                logger.info("	headerName - " + header.getName());
                logger.info("	headerValue - " + header.getValue());
                for (int h = 0; h < fields.length; h++) {
                    logger.info("	Looking for  - " + fields[h]);
                    if (fields[h].equals(header.getName().toString())) {
                        logger.info("	Found an OCCI Header - " + header.getName());
                        occiHeaders.put(header.getName(), header.getValue());
                    }
                }
            }
        } catch (org.apache.http.conn.HttpHostConnectException e) {
            e.printStackTrace();
            logger.error(e);
            return null;
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            logger.error(e);
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e);
            return null;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        logger.info("occiHeaders - " + occiHeaders);
        if (occiHeaders.containsKey(Response.Location)) {
            logger.info("Valid Provision" + statusLine);
            return occiHeaders.get(Response.Location).toString().replaceAll(Response.jobs, "");
        }
        logger.info("NOT a Valid Provision - " + statusLine);
        return statusLine;
    }

    public String post(Map<String, String> headersMap, String monitoringRequest) {
        HttpClient httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter("http.useragent", "sla@soi OCCI Client v0.2");
        HttpPost httpPost = new HttpPost("http://" + hostname + ":" + port + resource);
        List<Header> headersList = this.convert2Headers(headersMap);
        for (Iterator<Header> iterator = headersList.iterator(); iterator.hasNext(); ) {
            httpPost.addHeader(iterator.next());
        }
        if (monitoringRequest == null) {
            logger.info("Monitoring Request has not been specified - ");
            monitoringRequest = Terms.MONITORING_NOT_CONFIGURED;
            logger.info("Monitoring Request has not been specified - " + monitoringRequest);
        } else {
            logger.info("Monitoring Request is - " + monitoringRequest);
        }
        logger.info(httpPost.getRequestLine());
        logger.info(httpPost.getAllHeaders());
        Header[] headersArray = httpPost.getAllHeaders();
        String[] fields = { Response.Location };
        HashMap<String, String> occiHeaders = new HashMap<String, String>();
        for (int H = 0; H < headersArray.length; H++) {
            Header header = headersArray[H];
            logger.info("header - request  -" + header.toString());
            logger.info("	headerName - " + header.getName());
            logger.info("	headerValue - " + header.getValue());
        }
        String statusLine = null;
        try {
            HttpResponse httpResponse = httpClient.execute(httpPost);
            statusLine = httpResponse.getStatusLine().toString();
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            logger.info("----------------------------------------");
            logger.info("StatusLine - (full) - " + httpResponse.getStatusLine());
            logger.info("	StatusCode - " + statusCode);
            logger.info("	Reason - " + httpResponse.getStatusLine().getReasonPhrase());
            logger.info("	Protocol - " + httpResponse.getStatusLine().getProtocolVersion().toString());
            logger.info("----------------------------------------");
            if (StatusCode.validate(statusCode)) {
                logger.info("Response Validated");
            } else {
                logger.error("Response NOT Validated");
            }
            Header[] headers = httpResponse.getAllHeaders();
            for (int i = 0; i < headers.length; i++) {
                Header header = headers[i];
                logger.info("header - response - " + header.toString());
                logger.info("	headerName - " + header.getName());
                logger.info("	headerValue - " + header.getValue());
                for (int h = 0; h < fields.length; h++) {
                    logger.info("	Looking for  - " + fields[h]);
                    if (fields[h].equals(header.getName().toString())) {
                        logger.info("	Found an OCCI Header - " + header.getName());
                        occiHeaders.put(header.getName(), header.getValue());
                    }
                }
            }
        } catch (org.apache.http.conn.HttpHostConnectException e) {
            e.printStackTrace();
            logger.error(e);
            return null;
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            logger.error(e);
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e);
            return null;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        logger.info("occiHeaders - " + occiHeaders);
        if (occiHeaders.containsKey(Response.Location)) {
            logger.info("Valid Provision");
            return occiHeaders.get(Response.Location).toString().replaceAll(Response.jobs, "");
        }
        logger.info("NOT a Valid Provision" + statusLine);
        return null;
    }

    public String post(ArrayList<Map<String, String>> multiHeaders, String serviceName, String serviceDescription, String serviceSlaType, String notificationURI) {
        HttpClient httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter("http.useragent", "sla@soi OCCI Client v0.3 Multipart");
        HttpPost httpPost = new HttpPost("http://" + hostname + ":" + port + serviceResource);
        Header categoryHeader = new BasicHeader("Category", "service; scheme='http://sla-at-soi.eu/ism/service#'; title='" + serviceName + "'");
        Header attributeHeaderNotification = new BasicHeader("Attribute", "eu.slasoi.task.notificationUri='" + notificationURI + "'");
        Header attributeHeaderServiceName = new BasicHeader("Attribute", "eu.slasoi.infrastructure.service.name='" + serviceName + "'");
        Header attributeHeaderServiceDescription = new BasicHeader("Attribute", "eu.slasoi.infrastructure.service.description='" + serviceDescription + "'");
        Header attributeHeaderServiceSlaType = new BasicHeader("Attribute", "eu.slasoi.infrastructure.service.sla='" + serviceSlaType + "'");
        httpPost.addHeader(categoryHeader);
        httpPost.addHeader(attributeHeaderNotification);
        httpPost.addHeader(attributeHeaderServiceName);
        httpPost.addHeader(attributeHeaderServiceDescription);
        httpPost.addHeader(attributeHeaderServiceSlaType);
        httpPost.setEntity(Multipart.createMultipartEntity(multiHeaders));
        String statusLine = null;
        Header[] headersArray = httpPost.getAllHeaders();
        String[] fields = { Response.Location };
        HashMap<String, String> occiHeaders = new HashMap<String, String>();
        try {
            HttpResponse httpResponse = httpClient.execute(httpPost);
            statusLine = httpResponse.getStatusLine().toString();
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            logger.info("----------------------------------------");
            logger.info("StatusLine - (full) - " + httpResponse.getStatusLine());
            logger.info("   StatusCode - " + statusCode);
            logger.info("   Reason - " + httpResponse.getStatusLine().getReasonPhrase());
            logger.info("   Protocol - " + httpResponse.getStatusLine().getProtocolVersion().toString());
            logger.info("----------------------------------------");
            if (StatusCode.validate(statusCode)) {
                logger.info("Response Validated");
            } else {
                logger.error("Response NOT Validated");
            }
            Header[] headers = httpResponse.getAllHeaders();
            for (int i = 0; i < headers.length; i++) {
                Header header = headers[i];
                logger.info("header - response - " + header.toString());
                logger.info("   headerName - " + header.getName());
                logger.info("   headerValue - " + header.getValue());
                for (int h = 0; h < fields.length; h++) {
                    logger.info("   Looking for  - " + fields[h]);
                    if (fields[h].equals(header.getName().toString())) {
                        logger.info("   Found an OCCI Header - " + header.getName());
                        occiHeaders.put(header.getName(), header.getValue());
                    }
                }
            }
        } catch (org.apache.http.conn.HttpHostConnectException e) {
            e.printStackTrace();
            logger.error(e);
            return null;
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            logger.error(e);
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e);
            return null;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        logger.info("occiHeaders - " + occiHeaders);
        if (occiHeaders.containsKey(Response.Location)) {
            logger.info("Valid Provision");
            return occiHeaders.get(Response.Location).toString().replaceAll(Response.jobs, "");
        }
        logger.info("NOT a Valid Provision" + statusLine);
        return null;
    }

    public String post2(Map<String, String> headersMap, String monitoringRequest) {
        HttpClient httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter("http.useragent", "sla@soi OCCI Client v0.2");
        HttpPost httpPost = new HttpPost("http://" + hostname + ":" + port + resource);
        List<Header> headersList = this.convert2Headers(headersMap);
        for (Iterator<Header> iterator = headersList.iterator(); iterator.hasNext(); ) {
            httpPost.addHeader(iterator.next());
        }
        if (monitoringRequest == null) {
            logger.info("Monitoring Request has not been specified - ");
            monitoringRequest = Terms.MONITORING_NOT_CONFIGURED;
            logger.info("Monitoring Request has not been specified - " + monitoringRequest);
        } else {
            logger.info("Monitoring Request is - " + monitoringRequest);
        }
        try {
            StringEntity se = new StringEntity(monitoringRequest);
            httpPost.setEntity(se);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            logger.error(e);
        }
        logger.info(httpPost.getRequestLine());
        logger.info(httpPost.getAllHeaders());
        Header[] headersArray = httpPost.getAllHeaders();
        String[] fields = { Response.Location };
        HashMap<String, String> occiHeaders = new HashMap<String, String>();
        for (int H = 0; H < headersArray.length; H++) {
            Header header = headersArray[H];
            logger.info("header - request  -" + header.toString());
            logger.info("	headerName - " + header.getName());
            logger.info("	headerValue - " + header.getValue());
        }
        return null;
    }

    public String update(String request) {
        return null;
    }

    public Object authenticate(Object o) {
        return null;
    }

    public List<Header> convert2Headers(Map<String, String> headersMap) {
        List<Header> headersArray = new ArrayList<Header>();
        for (Iterator<String> iterator = headersMap.keySet().iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            Header header = new BasicHeader(key, headersMap.get(key));
            headersArray.add(header);
        }
        return headersArray;
    }

    public String createHeader(String term, String schema, String title) {
        String category = term + ";";
        category = category + " scheme=" + schema + ";";
        category = category + " title=" + title;
        logger.info("Creating Category - " + category);
        return category;
    }
}
