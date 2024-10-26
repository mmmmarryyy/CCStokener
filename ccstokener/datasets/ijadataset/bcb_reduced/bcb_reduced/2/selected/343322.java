package com.persistent.appfabric.sample.salesdataclient;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.persistent.appfabric.sample.salesdataclient.beans.SalesOrderServiceBean;
import com.persistent.appfabric.sample.salesdataclient.logger.LoggerHelper;

/**
* Class that provides methods for firing HTTP requests
*/
public class HttpRequests {

    private static final long serialVersionUID = 2343294639690782339L;

    private static ResourceBundle msgBundle = ResourceBundle.getBundle("com.persistent.appfabric.sample.salesdataclient.config.proxySettings");

    private static String PROXY_SERVER = null;

    private static String PROXY_PORT = null;

    static {
        try {
            PROXY_SERVER = msgBundle.getString("Proxy_host");
            PROXY_PORT = msgBundle.getString("Proxy_port");
        } catch (Exception e) {
            PROXY_SERVER = null;
            PROXY_PORT = null;
        }
    }

    /**
	* Send a HTTP GET request to the specified url
	*@param urlForSalesData URL for the request
	*@param request HttpServletRequest 
	*@return Object conatining data fetched by the GET request
	*/
    public static List<SalesOrderServiceBean> fireGetRequest(LoggerHelper.RecordType reqType, String urlForSalesData, HttpServletRequest request) throws IOException {
        AtomParser atomParser = new AtomParser();
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
            httpUrlConnection.addRequestProperty("token", (String) request.getSession().getAttribute("acsToken"));
            httpUrlConnection.addRequestProperty("solutionName", (String) request.getSession().getAttribute("solutionName"));
            httpUrlConnection.connect();
            System.out.println(httpUrlConnection.getResponseMessage());
            if (httpUrlConnection.getResponseCode() == HttpServletResponse.SC_UNAUTHORIZED) {
                List<SalesOrderServiceBean> salesOrderServiceBean = new ArrayList<SalesOrderServiceBean>();
                SalesOrderServiceBean response = new SalesOrderServiceBean();
                response.setResponseCode(HttpServletResponse.SC_UNAUTHORIZED);
                response.setResponseMessage(httpUrlConnection.getResponseMessage());
                salesOrderServiceBean.add(response);
                return salesOrderServiceBean;
            }
            inputStream = httpUrlConnection.getInputStream();
            input = new DataInputStream(inputStream);
            bufferedReader = new BufferedReader(new InputStreamReader(input));
            String str;
            while (null != ((str = bufferedReader.readLine()))) {
                sBuf.append(str);
            }
            String responseString = sBuf.toString();
            List<SalesOrderServiceBean> salesOrderServiceBean = new ArrayList<SalesOrderServiceBean>();
            salesOrderServiceBean = atomParser.parseString(responseString);
            return salesOrderServiceBean;
        } catch (MalformedURLException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }
    }
}
