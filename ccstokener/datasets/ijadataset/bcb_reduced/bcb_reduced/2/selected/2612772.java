package net.sf.dpdesktop.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Level;
import net.sf.dpdesktop.service.exception.ResponseCodeException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import net.sf.dpdesktop.module.util.ssl.HttpsController;
import net.sf.dpdesktop.service.exception.ResponseException;
import net.sf.dpdesktop.module.util.hash.HashFactory;
import org.apache.log4j.BasicConfigurator;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.apache.log4j.Logger;

/**
 *
 * @author Heiner Reinhardt
 */
public class ContextHttp extends Context {

    private boolean isHttpsSupported = true;

    public void setHttpSupported(boolean supported) {
        isHttpsSupported = supported;
    }

    public ContextHttp() {
    }

    @Override
    public void clean(DataService dataService) {
    }

    private HttpURLConnection createConnection(DataService dataService) throws IOException {
        URL url = dataService.getRemotePlace().getURL();
        if (!isHttpsSupported && url.getProtocol().equals("https")) {
            throw new IOException("HTTPS is currently disabled.");
        }
        return (HttpURLConnection) url.openConnection();
    }

    private enum RequestMethod {

        LOAD, STORE
    }

    @Override
    public boolean store(DataService dataService, Document storeDocument) throws MalformedURLException, IOException, ServiceAuthException {
        HttpURLConnection connection = createConnection(dataService);
        Document completeDocument = DocumentHelper.createDocument(DocumentHelper.createElement("root"));
        Element requestElement = this.createRequestElement(dataService, RequestMethod.STORE);
        completeDocument.getRootElement().add(requestElement);
        completeDocument.getRootElement().add(storeDocument.getRootElement());
        connection.setDoInput(true);
        connection.setDoOutput(true);
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        super.store(writer, completeDocument);
        switch(connection.getResponseCode()) {
            case HttpURLConnection.HTTP_OK:
                break;
            case HttpURLConnection.HTTP_UNAUTHORIZED:
                throw new ServiceAuthException();
            default:
                throw new IOException(String.format("Server reply: %d - %s \n\n ", connection.getResponseCode(), connection.getResponseMessage(), connection.getContent()));
        }
        return true;
    }

    @Override
    public Document load(DataService dataService) throws MalformedURLException, DocumentException, IOException, ServiceAuthException {
        HttpURLConnection connection = createConnection(dataService);
        Document complete = DocumentHelper.createDocument(DocumentHelper.createElement("root"));
        Element requestElement = createRequestElement(dataService, RequestMethod.LOAD);
        complete.getRootElement().add(requestElement);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        Logger.getLogger(this.getClass()).debug("Loading data from URL: " + dataService.getRemotePlace().getURLAsString());
        store(new OutputStreamWriter(connection.getOutputStream()), complete);
        switch(connection.getResponseCode()) {
            case HttpURLConnection.HTTP_OK:
                break;
            case HttpURLConnection.HTTP_UNAUTHORIZED:
                throw new ServiceAuthException();
            default:
                throw new IOException(connection.getResponseMessage());
        }
        return super.load(new InputStreamReader(connection.getInputStream(), "UTF-8"));
    }

    private Element createRequestElement(DataService dataService, RequestMethod requestMethod) {
        Element requestElement = DocumentHelper.createElement("request");
        requestElement.addAttribute("username", dataService.getRemotePlace().getUsername());
        requestElement.addAttribute("password", dataService.getRemotePlace().getPassword());
        requestElement.addAttribute("module", dataService.getModuleName());
        requestElement.addAttribute("method", requestMethod.name());
        return requestElement;
    }
}
