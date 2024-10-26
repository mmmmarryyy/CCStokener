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
import com.persistent.appfabric.acs.Credentials;
import com.persistent.appfabric.acs.Credentials.TOKEN_TYPE;
import com.persistent.appfabric.common.AppFabricEnvironment;
import com.persistent.appfabric.common.AppFabricException;
import com.persistent.appfabric.common.LoggerUtil;
import com.persistent.appfabric.sample.salesdataclient.config.ConfigUtil;
import com.persistent.appfabric.sample.salesdataclient.logger.LoggerHelper;
import com.persistent.appfabric.servicebus.MessageBufferPolicy;
import com.persistent.appfabric.sample.salesdataclient.service.ServiceBusRequest.HttpVerbs;
import com.persistent.appfabric.sample.salesdataclient.service.ServiceBusRequest;

/**
 * Servlet implementation class UpdateSalesOrder
 */
public class UpdateSalesOrder extends HttpServlet {

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
    public UpdateSalesOrder() {
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
        String salesOrderId = request.getParameter("txtSalesOrderId");
        String productId = request.getParameter("product");
        String orderQty = request.getParameter("txtProductQty");
        String urlForAddSales = "http://" + ConfigUtil.getServiceHost() + "/" + ConfigUtil.getServiceName() + "/SalesOrder/SalesOrderData";
        String responseString = null;
        DataOutputStream outputStream = null;
        InputStream inputStream = null;
        BufferedReader bufferedReader = null;
        DataInputStream input = null;
        StringBuffer sBuf = new StringBuffer();
        URL url = null;
        StringBuilder postData = new StringBuilder();
        postData.append("productId=" + URLEncoder.encode(productId, AppFabricEnvironment.ENCODING));
        postData.append("&");
        postData.append("orderQty=" + URLEncoder.encode(orderQty, AppFabricEnvironment.ENCODING));
        postData.append("&");
        postData.append("salesOrderId=" + URLEncoder.encode(salesOrderId, AppFabricEnvironment.ENCODING));
        if (ConfigUtil.isMessageBuffer) {
            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put("token", request.getSession().getAttribute("acsToken").toString());
            MessageBufferPolicy messageBufferPolicy = new MessageBufferPolicy("Required", "None", "PT5M", 10);
            Credentials credentials;
            try {
                credentials = new Credentials(TOKEN_TYPE.SharedSecretToken, issuerName, issuerKey);
                ServiceBusRequest msgBufferClient = new ServiceBusRequest(messageBufferName, messageBufferPolicy, PROXY_SERVER, Integer.parseInt(PROXY_PORT), solutionName, credentials);
                String xmlData = msgBufferClient.xmlForPollingService(urlForAddSales, HttpVerbs.POST, request.getSession().getId(), headers, "", true);
                if (LoggerUtil.getIsLoggingOn()) LoggerHelper.logMessage(URLEncoder.encode(xmlData, "UTF-8"), LoggerHelper.RecordType.UpdateSalesOrder_REQUEST);
                String responseData = msgBufferClient.sendPostOrPutRequest(urlForAddSales, HttpVerbs.POST, request.getSession().getId(), headers, postData.toString());
                if (LoggerUtil.getIsLoggingOn()) LoggerHelper.logMessage(URLEncoder.encode(responseData, "UTF-8"), LoggerHelper.RecordType.UpdateSalesOrder_RESPONSE);
            } catch (AppFabricException e) {
                response.sendRedirect("/" + ConfigUtil.getClientName() + "/pages/error.jsp?message=" + e.getMessage());
                return;
            }
        } else {
            try {
                url = new URL(urlForAddSales);
                HttpURLConnection httpUrlConnection;
                httpUrlConnection = (HttpURLConnection) url.openConnection();
                httpUrlConnection.setRequestMethod("POST");
                httpUrlConnection.setUseCaches(false);
                httpUrlConnection.setDoInput(true);
                httpUrlConnection.setDoOutput(true);
                httpUrlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                httpUrlConnection.setRequestProperty("Content-Language", "en-US");
                httpUrlConnection.setRequestProperty("Accept", "*/*");
                httpUrlConnection.addRequestProperty("token", (String) request.getSession().getAttribute("acsToken"));
                httpUrlConnection.addRequestProperty("solutionName", (String) request.getSession().getAttribute("solutionName"));
                outputStream = new DataOutputStream(httpUrlConnection.getOutputStream());
                outputStream.writeBytes(postData.toString());
                outputStream.flush();
                if (httpUrlConnection.getResponseCode() == HttpServletResponse.SC_UNAUTHORIZED) {
                    response.sendRedirect("/" + ConfigUtil.getClientName() + "/pages/error.jsp?code=" + httpUrlConnection.getResponseCode() + "&message=" + httpUrlConnection.getResponseMessage() + "&method=Update Order");
                    return;
                }
                inputStream = httpUrlConnection.getInputStream();
                input = new DataInputStream(inputStream);
                bufferedReader = new BufferedReader(new InputStreamReader(input));
                String str;
                while (null != ((str = bufferedReader.readLine()))) {
                    sBuf.append(str);
                }
                responseString = sBuf.toString();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        response.sendRedirect("/" + ConfigUtil.getClientName() + "/pages/personalSalesOrder.jsp");
    }
}
