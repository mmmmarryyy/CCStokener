package com.tenline.pinecone.platform.sdk;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamReader;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;
import com.tenline.pinecone.platform.model.Exchange;
import com.tenline.pinecone.platform.sdk.development.APIResponse;
import com.tenline.pinecone.platform.sdk.development.JaxbAPI;

/**
 * @author Bill
 *
 */
public class ExchangeAPI extends JaxbAPI {

    /**
	 * @param host
	 * @param port
	 * @param context
	 */
    public ExchangeAPI(String host, String port, String context) {
        super(host, port, context);
        try {
            jaxbContext = JAXBContext.newInstance(Exchange.class);
            marshaller = jaxbContext.createMarshaller();
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    /**
	 * 
	 * @param exchange
	 * @return
	 * @throws Exception
	 */
    public APIResponse create(Exchange exchange) throws Exception {
        APIResponse response = new APIResponse();
        connection = (HttpURLConnection) new URL(url + "/api/exchange/create").openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setUseCaches(false);
        connection.setConnectTimeout(TIMEOUT);
        connection.connect();
        marshaller.marshal(exchange, new MappedXMLStreamWriter(new MappedNamespaceConvention(new Configuration()), new OutputStreamWriter(connection.getOutputStream(), "utf-8")));
        connection.getOutputStream().flush();
        connection.getOutputStream().close();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            JSONObject obj = new JSONObject(new String(new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8")).readLine()));
            response.setDone(true);
            response.setMessage(unmarshaller.unmarshal(new MappedXMLStreamReader(obj, new MappedNamespaceConvention(new Configuration()))));
            connection.getInputStream().close();
        } else {
            response.setDone(false);
            response.setMessage("Create Exchange Error Code: Http (" + connection.getResponseCode() + ")");
        }
        connection.disconnect();
        return response;
    }

    /**
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
    public APIResponse delete(String id) throws Exception {
        APIResponse response = new APIResponse();
        connection = (HttpURLConnection) new URL(url + "/api/exchange/delete/" + id).openConnection();
        connection.setRequestMethod("DELETE");
        connection.setConnectTimeout(TIMEOUT);
        connection.connect();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            response.setDone(true);
            response.setMessage("Exchange Deleted!");
        } else {
            response.setDone(false);
            response.setMessage("Delete Exchange Error Code: Http (" + connection.getResponseCode() + ")");
        }
        connection.disconnect();
        return response;
    }

    /**
	 * 
	 * @param exchange
	 * @return
	 * @throws Exception
	 */
    public APIResponse update(Exchange exchange) throws Exception {
        APIResponse response = new APIResponse();
        connection = (HttpURLConnection) new URL(url + "/api/exchange/update").openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setUseCaches(false);
        connection.setConnectTimeout(TIMEOUT);
        connection.connect();
        marshaller.marshal(exchange, new MappedXMLStreamWriter(new MappedNamespaceConvention(new Configuration()), new OutputStreamWriter(connection.getOutputStream(), "utf-8")));
        connection.getOutputStream().flush();
        connection.getOutputStream().close();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            JSONObject obj = new JSONObject(new String(new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8")).readLine()));
            response.setDone(true);
            response.setMessage(unmarshaller.unmarshal(new MappedXMLStreamReader(obj, new MappedNamespaceConvention(new Configuration()))));
            connection.getInputStream().close();
        } else {
            response.setDone(false);
            response.setMessage("Update Exchange Error Code: Http (" + connection.getResponseCode() + ")");
        }
        connection.disconnect();
        return response;
    }

    /**
	 * 
	 * @param filter
	 * @return
	 * @throws Exception
	 */
    public APIResponse show(String filter) throws Exception {
        APIResponse response = new APIResponse();
        String requestUrl = url + "/api/exchange/show/" + filter;
        connection = (HttpURLConnection) new URL(requestUrl).openConnection();
        connection.setConnectTimeout(TIMEOUT);
        connection.connect();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            JSONArray array = new JSONArray(new String(new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8")).readLine()));
            Collection<Exchange> message = new ArrayList<Exchange>();
            for (int i = 0; i < array.length(); i++) {
                message.add((Exchange) unmarshaller.unmarshal(new MappedXMLStreamReader(array.getJSONObject(i), new MappedNamespaceConvention(new Configuration()))));
            }
            response.setDone(true);
            response.setMessage(message);
            connection.getInputStream().close();
        } else {
            response.setDone(false);
            response.setMessage("Show Exchange Error Code: Http (" + connection.getResponseCode() + ")");
        }
        connection.disconnect();
        return response;
    }

    /**
	 * 
	 * @param filter
	 * @return
	 * @throws Exception
	 */
    public APIResponse showByAccount(String filter) throws Exception {
        APIResponse response = new APIResponse();
        String requestUrl = url + "/api/exchange/show/@Account/" + filter;
        connection = (HttpURLConnection) new URL(requestUrl).openConnection();
        connection.setConnectTimeout(TIMEOUT);
        connection.connect();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            JSONArray array = new JSONArray(new String(new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8")).readLine()));
            Collection<Exchange> message = new ArrayList<Exchange>();
            for (int i = 0; i < array.length(); i++) {
                message.add((Exchange) unmarshaller.unmarshal(new MappedXMLStreamReader(array.getJSONObject(i), new MappedNamespaceConvention(new Configuration()))));
            }
            response.setDone(true);
            response.setMessage(message);
            connection.getInputStream().close();
        } else {
            response.setDone(false);
            response.setMessage("Show Exchange By Account Error Code: Http (" + connection.getResponseCode() + ")");
        }
        connection.disconnect();
        return response;
    }
}
