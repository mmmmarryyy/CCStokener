package com.persistent.appfabric.polling;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.ResourceBundle;
import com.persistent.appfabric.acs.Credentials;
import com.persistent.appfabric.common.AppFabricException;
import com.persistent.appfabric.common.StringUtil;
import com.persistent.appfabric.servicebus.MessageBuffer;
import com.persistent.appfabric.servicebus.MessageBufferPolicy;

public class PollingService {

    private String httpProxy;

    private int httpPort;

    private String solutionName;

    private String encoding;

    private String Content_Type;

    private String Content_Language;

    private String Accept;

    public PollingService(String httpProxy, int httpPort) {
        this.httpPort = httpPort;
        this.httpProxy = httpProxy;
        initFromConfig();
    }

    public PollingService() {
        this.httpPort = 0;
        this.httpProxy = null;
        initFromConfig();
    }

    private void initFromConfig() {
        ResourceBundle rbundal = ResourceBundle.getBundle("com.persistent.appfabric.polling.config.pollingConfig");
        this.solutionName = rbundal.getString("SERVICE_NAME");
        this.encoding = rbundal.getString("ENCODING");
        Content_Type = rbundal.getString("CONTENT_TYPE");
        Content_Language = rbundal.getString("CONTENT_LANGUAGE");
        Accept = rbundal.getString("ACCEPT");
    }

    public void stopPolling(Credentials credentials, String serviceMessageBuffer) throws AppFabricException {
        MessageBuffer msgBuffer;
        if (httpProxy != null) msgBuffer = new MessageBuffer(httpProxy, httpPort, credentials, solutionName); else msgBuffer = new MessageBuffer(credentials, solutionName);
        try {
            msgBuffer.sendMessage(serviceMessageBuffer, "<request><url>shutdown</url></request>");
        } catch (AppFabricException e) {
            throw e;
        }
    }

    public void startPolling(Credentials credentials, String serviceMessageBuffer) throws Exception {
        String solutionName = this.solutionName;
        ResourceBundle rbundal = ResourceBundle.getBundle("com.persistent.appfabric.polling.config.pollingConfig");
        String authorization = rbundal.getString("AUTHORIZATION");
        String transportProtection = rbundal.getString("TRANSPORT_PROTECTION");
        String expiresAfter = rbundal.getString("EXPIRES_AFTER");
        int maxMessageCount = Integer.parseInt(rbundal.getString("MAX_MESSAGE_COUNT"));
        String verb = null;
        String url = null;
        String messageBufferName = null;
        String response = "";
        String body = null;
        try {
            MessageBuffer msgBuffer;
            if (httpProxy != null) msgBuffer = new MessageBuffer(httpProxy, httpPort, credentials, solutionName); else msgBuffer = new MessageBuffer(credentials, solutionName);
            MessageBufferPolicy msgBufferPolicyObj = new MessageBufferPolicy(authorization, transportProtection, expiresAfter, maxMessageCount);
            msgBuffer.createMessageBuffer(serviceMessageBuffer, msgBufferPolicyObj);
            while (true) {
                try {
                    String str = msgBuffer.retrieveMessage(serviceMessageBuffer);
                    if (!StringUtil.IsNullOrEmpty(str)) {
                        List<ServiceMessage> records = parseMessage(str);
                        if (records != null) {
                            ServiceMessage record = records.get(0);
                            if (record.getmessageBufferName() != null) messageBufferName = URLDecoder.decode(record.getmessageBufferName(), encoding);
                            if (record.geturl() != null) {
                                url = URLDecoder.decode(record.geturl(), encoding);
                                if (url.toString().equalsIgnoreCase("shutdown")) {
                                    return;
                                }
                            }
                            if (record.getverb() != null) verb = URLDecoder.decode(record.getverb(), encoding);
                            if (record.getBody() != null) body = URLDecoder.decode(record.getBody(), encoding);
                            if (verb == null || url == null || messageBufferName == null) {
                                continue;
                            }
                            if (verb.equalsIgnoreCase("GET")) {
                                response = fireGetRequest(url, record.getHeaders());
                            }
                            if (verb.equalsIgnoreCase("PUT")) {
                                response = firePUTRequest(url, record.getHeaders(), body);
                            }
                            if (verb.equalsIgnoreCase("POST")) {
                                response = firePOSTRequest(url, record.getHeaders(), body);
                            }
                            msgBuffer.sendMessage(messageBufferName, response);
                        }
                    }
                } catch (Exception e) {
                    String responseCode = responseCode(e.getMessage());
                    if (responseCode.equalsIgnoreCase("404")) {
                        Calendar cal = Calendar.getInstance();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String date = sdf.format(cal.getTime());
                        System.out.println(date + " " + e.getMessage());
                        try {
                            msgBuffer.createMessageBuffer(serviceMessageBuffer, msgBufferPolicyObj);
                        } catch (Exception e1) {
                            System.out.println(date + " " + e1.getMessage());
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw e;
        }
    }

    private static String responseCode(String message) {
        String splitstr[] = message.split("Response code: ");
        if (splitstr.length == 2) return splitstr[1]; else return "";
    }

    public static List<ServiceMessage> parseMessage(String response) {
        List<ServiceMessage> records = new ArrayList<ServiceMessage>();
        MessageReader reader = new MessageReader();
        String responseString = response;
        if (responseString != "") {
            records = reader.parseString(responseString);
        }
        return records;
    }

    private String fireGetRequest(String urlForSalesData, Hashtable<String, String> headers) throws IOException {
        String PROXY_SERVER = this.httpProxy;
        String PROXY_PORT = Integer.toString(this.httpPort);
        InputStream inputStream = null;
        BufferedReader bufferedReader = null;
        DataInputStream input = null;
        StringBuffer sBuf = new StringBuffer();
        Proxy proxy;
        if (PROXY_SERVER != null && PROXY_PORT != null) {
            SocketAddress address = new InetSocketAddress(PROXY_SERVER, Integer.parseInt(PROXY_PORT));
            proxy = new Proxy(Proxy.Type.HTTP, address);
        } else {
            proxy = null;
        }
        proxy = null;
        URL url;
        try {
            url = new URL(urlForSalesData);
            HttpURLConnection httpUrlConnection;
            if (proxy != null) httpUrlConnection = (HttpURLConnection) url.openConnection(proxy); else httpUrlConnection = (HttpURLConnection) url.openConnection();
            httpUrlConnection.setDoInput(true);
            httpUrlConnection.setRequestMethod("GET");
            if (headers != null) {
                Enumeration<String> e = headers.keys();
                while (e.hasMoreElements()) {
                    String key = (String) e.nextElement();
                    httpUrlConnection.addRequestProperty(key, URLDecoder.decode(headers.get(key), "UTF-8"));
                }
            }
            httpUrlConnection.connect();
            sBuf.append("<responseCode>" + httpUrlConnection.getResponseCode() + "</responseCode>");
            System.out.println(httpUrlConnection.getResponseMessage());
            inputStream = httpUrlConnection.getInputStream();
            input = new DataInputStream(inputStream);
            bufferedReader = new BufferedReader(new InputStreamReader(input));
            String str;
            while (null != ((str = bufferedReader.readLine()))) {
                sBuf.append(str);
            }
            return sBuf.toString();
        } catch (MalformedURLException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }
    }

    private String firePOSTRequest(String urlStr, Hashtable<String, String> headers, String body) throws IOException {
        InputStream inputStream = null;
        DataOutputStream outputStream = null;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection httpUrlConnection;
            httpUrlConnection = (HttpURLConnection) url.openConnection();
            httpUrlConnection.setRequestMethod("POST");
            httpUrlConnection.setUseCaches(false);
            httpUrlConnection.setDoInput(true);
            httpUrlConnection.setDoOutput(true);
            httpUrlConnection.setRequestProperty("Content-Type", Content_Type);
            httpUrlConnection.setRequestProperty("Content-Language", Content_Language);
            httpUrlConnection.setRequestProperty("Accept", Accept);
            if (headers != null) {
                Enumeration<String> e = headers.keys();
                while (e.hasMoreElements()) {
                    String key = (String) e.nextElement();
                    httpUrlConnection.addRequestProperty(key, URLDecoder.decode(headers.get(key), "UTF-8"));
                }
            }
            if (body != null) {
                outputStream = new DataOutputStream(httpUrlConnection.getOutputStream());
                outputStream.writeBytes(body);
                outputStream.flush();
            }
            inputStream = httpUrlConnection.getInputStream();
            String response = "<responseCode>" + httpUrlConnection.getResponseCode() + "</responseCode>";
            return response;
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return "";
    }

    private String firePUTRequest(String urlStr, Hashtable<String, String> headers, String body) throws IOException {
        InputStream inputStream = null;
        DataOutputStream outputStream = null;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection httpUrlConnection;
            httpUrlConnection = (HttpURLConnection) url.openConnection();
            httpUrlConnection.setRequestMethod("PUT");
            httpUrlConnection.setUseCaches(false);
            httpUrlConnection.setDoInput(true);
            httpUrlConnection.setDoOutput(true);
            httpUrlConnection.setRequestProperty("Content-Type", Content_Type);
            httpUrlConnection.setRequestProperty("Content-Language", Content_Language);
            httpUrlConnection.setRequestProperty("Accept", Accept);
            if (headers != null) {
                Enumeration<String> e = headers.keys();
                while (e.hasMoreElements()) {
                    String key = (String) e.nextElement();
                    httpUrlConnection.addRequestProperty(key, URLDecoder.decode(headers.get(key), "UTF-8"));
                }
            }
            if (body != null) {
                outputStream = new DataOutputStream(httpUrlConnection.getOutputStream());
                outputStream.writeBytes(body);
                outputStream.flush();
            }
            inputStream = httpUrlConnection.getInputStream();
            String response = "<responseCode>" + httpUrlConnection.getResponseCode() + "</responseCode>";
            return response;
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return "";
    }
}
