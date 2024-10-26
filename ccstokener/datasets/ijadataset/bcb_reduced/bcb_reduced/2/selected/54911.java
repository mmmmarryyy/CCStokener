package com.persistent.appfabric.sample.salesdataclient;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.ResourceBundle;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.persistent.appfabric.common.AppFabricException;
import com.persistent.appfabric.common.LoggerUtil;
import com.persistent.appfabric.acs.Credentials;
import com.persistent.appfabric.acs.Credentials.TOKEN_TYPE;
import com.persistent.appfabric.common.AppFabricEnvironment;
import com.persistent.appfabric.sample.salesdataclient.config.ConfigUtil;
import com.persistent.appfabric.sample.salesdataclient.logger.LoggerHelper;
import com.persistent.appfabric.servicebus.MessageBufferPolicy;
import com.persistent.appfabric.sample.salesdataclient.service.ServiceBusRequest.HttpVerbs;
import com.persistent.appfabric.sample.salesdataclient.service.ServiceBusRequest;

/**
 * Servlet implementation class AddEditsalesData
 */
public class AddSalesOrder extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static ResourceBundle msgBundle = ResourceBundle.getBundle("com.persistent.appfabric.sample.salesdataclient.config.proxySettings");

    private static ResourceBundle acsBundle = ResourceBundle.getBundle("com.persistent.appfabric.sample.salesdataclient.config.ACS");

    private static final String solutionName = acsBundle.getString("SERVICE_NAME");

    private static final String issuerName = acsBundle.getString("issuerName");

    private static final String issuerKey = acsBundle.getString("issuerKey");

    private static String PROXY_SERVER = null;

    private static String PROXY_PORT = null;

    private static final String messageBufferName = acsBundle.getString("MESSAGE_BUFFER_NAME");

    static {
        try {
            PROXY_SERVER = msgBundle.getString("Proxy_host");
            PROXY_PORT = msgBundle.getString("Proxy_port");
        } catch (Exception e) {
            PROXY_SERVER = null;
            PROXY_PORT = "0";
        }
    }

    /**
     * @see HttpServlet#HttpServlet()
     */
    public AddSalesOrder() {
        super();
    }

    /**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    /**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String ProductId = request.getParameter("product");
        String urlForAddSales = "http://" + ConfigUtil.getServiceHost() + "/" + ConfigUtil.getServiceName() + "/SalesOrder/SalesOrderData";
        StringBuilder postData = new StringBuilder();
        postData.append("productId=" + URLEncoder.encode(ProductId, AppFabricEnvironment.ENCODING));
        postData.append("&");
        postData.append("orderQty=" + URLEncoder.encode(request.getParameter("txtProductQty"), AppFabricEnvironment.ENCODING));
        postData.append("&");
        postData.append("salesPersonId=" + URLEncoder.encode((String) request.getSession().getAttribute("salesPersonId"), AppFabricEnvironment.ENCODING));
        if (ConfigUtil.isMessageBuffer) {
            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put("token", request.getSession().getAttribute("acsToken").toString());
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            headers.put("Content-Language", "en-US");
            headers.put("Accept", "*/*");
            MessageBufferPolicy messageBufferPolicy = new MessageBufferPolicy("Required", "None", "PT5M", 10);
            Credentials credentials;
            try {
                credentials = new Credentials(TOKEN_TYPE.SharedSecretToken, issuerName, issuerKey);
                ServiceBusRequest msgBufferClient = new ServiceBusRequest(messageBufferName, messageBufferPolicy, PROXY_SERVER, Integer.parseInt(PROXY_PORT), solutionName, credentials);
                String xmlData = msgBufferClient.xmlForPollingService(urlForAddSales, HttpVerbs.PUT, request.getSession().getId(), headers, "", true);
                if (LoggerUtil.getIsLoggingOn()) LoggerHelper.logMessage(URLEncoder.encode(xmlData, "UTF-8"), LoggerHelper.RecordType.AddEditSalesData_REQUEST);
                String responseData = msgBufferClient.sendPostOrPutRequest(urlForAddSales, HttpVerbs.PUT, request.getSession().getId(), headers, postData.toString());
                if (LoggerUtil.getIsLoggingOn()) LoggerHelper.logMessage(URLEncoder.encode(responseData, "UTF-8"), LoggerHelper.RecordType.AddEditSalesData_RESPONSE);
            } catch (AppFabricException e) {
                response.sendRedirect("/" + ConfigUtil.getClientName() + "/pages/error.jsp?message=" + e.getMessage());
                return;
            }
        } else {
            DataOutputStream outputStream = null;
            InputStream inputStream = null;
            BufferedReader bufferedReader = null;
            DataInputStream input = null;
            StringBuffer sBuf = new StringBuffer();
            URL url = null;
            url = new URL(urlForAddSales);
            try {
                HttpURLConnection httpUrlConnection;
                httpUrlConnection = (HttpURLConnection) url.openConnection();
                httpUrlConnection.setRequestMethod("PUT");
                httpUrlConnection.setUseCaches(false);
                httpUrlConnection.setDoInput(true);
                httpUrlConnection.setDoOutput(true);
                httpUrlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                httpUrlConnection.setRequestProperty("Content-Language", "en-US");
                httpUrlConnection.setRequestProperty("Accept", "*/*");
                httpUrlConnection.setRequestProperty("Authorization", (String) request.getSession().getAttribute("acsToken"));
                httpUrlConnection.addRequestProperty("token", (String) request.getSession().getAttribute("acsToken"));
                outputStream = new DataOutputStream(httpUrlConnection.getOutputStream());
                outputStream.writeBytes(postData.toString());
                outputStream.flush();
                inputStream = httpUrlConnection.getInputStream();
                if (httpUrlConnection.getResponseCode() == HttpServletResponse.SC_UNAUTHORIZED) {
                    response.sendRedirect("/" + ConfigUtil.getClientName() + "/pages/error.jsp?code=" + httpUrlConnection.getResponseCode() + "&message=" + httpUrlConnection.getResponseMessage() + "&method=Add Order");
                    return;
                }
                input = new DataInputStream(inputStream);
                bufferedReader = new BufferedReader(new InputStreamReader(input));
                String str;
                while (null != ((str = bufferedReader.readLine()))) {
                    sBuf.append(str);
                }
            } catch (MalformedURLException e) {
                response.sendRedirect("/" + ConfigUtil.getClientName() + "/pages/error.jsp?message=Malformed URL exception");
            } catch (IOException e) {
                response.sendRedirect("/" + ConfigUtil.getClientName() + "/pages/error.jsp?message=IOException in Add sales order");
            }
        }
        response.sendRedirect("/" + ConfigUtil.getClientName() + "/pages/personalSalesOrder.jsp");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    }
}
