package ru.dragon.bugzilla.api.v3;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcClientRequestImpl;
import ru.dragon.bugzilla.BugzillaException;
import ru.dragon.bugzilla.http.xmlrpc.XmlRpcWithCookieTransportFactoryImpl;
import ru.dragon.bugzilla.util.Utils;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class used to execute concrete service
 *
 * @author <a href="mailto: NIzhikov@gmail.com">Izhikov Nikolay</a>
 */
public class ServiceExecutor {

    protected Logger log = Logger.getLogger(getClass());

    private static final String REQUEST_ENCODING = "utf-8";

    private String serverUrl;

    private XmlRpcClient xmlRpcClient;

    private List<String> cookies;

    public static class Parameter {

        private String name;

        private Object value;

        public Parameter(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }

    public ServiceExecutor(String serverUrl) {
        this.serverUrl = serverUrl;
        this.cookies = new ArrayList<String>();
        xmlRpcClient = new XmlRpcClient();
        xmlRpcClient.setTransportFactory(new XmlRpcWithCookieTransportFactoryImpl(xmlRpcClient, cookies));
    }

    public InputStream executeQuery(String urlString, Parameter... parameters) throws BugzillaException {
        if (parameters.length > 0) {
            StringBuilder stringBuilder = new StringBuilder(urlString);
            if (urlString.contains("?")) {
                if (!urlString.endsWith("?") && !urlString.endsWith("&")) {
                    stringBuilder.append("&");
                }
            } else {
                stringBuilder.append("?");
            }
            boolean isFirst = true;
            for (Parameter parameter : parameters) {
                if (!isFirst) {
                    stringBuilder.append("&");
                }
                isFirst = false;
                stringBuilder.append(parameter.getName()).append("=").append(parameter.getValue());
            }
            urlString = stringBuilder.toString();
        }
        try {
            URLConnection connection = new URL(urlString).openConnection();
            connection.setRequestProperty("Cookie", Utils.makeCookieString(cookies));
            connection.connect();
            return connection.getInputStream();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map executeService(String methodName, Parameter... parameters) throws BugzillaException {
        try {
            XmlRpcClientConfigImpl xmlRpcClientConfig = new XmlRpcClientConfigImpl();
            xmlRpcClientConfig.setServerURL(new URL(serverUrl));
            xmlRpcClientConfig.setEncoding(REQUEST_ENCODING);
            Map<String, Object> parameterMap = new HashMap<String, Object>();
            for (Parameter parameter : parameters) {
                parameterMap.put(parameter.getName(), parameter.getValue());
            }
            XmlRpcRequest xmlRpcRequest = new XmlRpcClientRequestImpl(xmlRpcClientConfig, methodName, new Object[] { parameterMap });
            return (Map) xmlRpcClient.execute(xmlRpcRequest);
        } catch (MalformedURLException e) {
            throw new BugzillaException(e);
        } catch (XmlRpcException e) {
            throw new BugzillaException(e);
        }
    }
}
