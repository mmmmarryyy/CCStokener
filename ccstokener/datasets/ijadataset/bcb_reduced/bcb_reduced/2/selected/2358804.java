package com.beanstalktech.applet.plugin;

import com.beanstalktech.applet.component.BeanstalkApplet;
import com.beanstalktech.common.connection.ServerConnection;
import com.beanstalktech.common.server.*;
import com.beanstalktech.common.context.AppEvent;
import com.beanstalktech.common.context.AppError;
import com.beanstalktech.common.utility.Logger;
import java.lang.Integer;
import java.net.*;
import java.io.*;
import java.util.Vector;

/**
 * Communicates with HTTP Servers through URL Connections. This
 * HTTP server adapter is suitable for applet implementations.
 * 
 */
public class AppletHTTPServerAdapter extends ServerAdapter implements Runnable {

    protected static final int READ_BUFFER_SIZE = 1000;

    protected Thread m_thread;

    protected AppEvent m_event;

    protected HTTPServerRequest m_request;

    /**
     * This method advertises the class as a server adapter for
     * ScriptHTTP requests.
     * 
     * @param event Application Event containing a reference to the Server 
     * Connection for the request
     */
    public void handle_ScriptHTTP(AppEvent event) {
        handleRequest(event);
    }

    /**
     * This method advertises the class as a server adapter for
     * HTTP requests.
     * 
     * @param event Application Event containing a reference to the Server 
     * Connection for the request
     */
    public void handle_HTTP(AppEvent event) {
        handleRequest(event);
    }

    /**
     * Implements
     * 
     * @param event
     */
    public void handleRequest(AppEvent event) {
        m_event = event;
        m_request = (HTTPServerRequest) m_event.getConnection().getRequest();
        m_thread = new Thread(this);
        m_thread.start();
    }

    public void run() {
        try {
            URLConnection urlConnection = (URLConnection) provideResource(m_event);
            m_event.getApplication().getLogger().logMessage(3, "Applet HTTP Server Adapter: Request length is: " + m_request.getRequestBytesLength());
            OutputStream outputStream = urlConnection.getOutputStream();
            outputStream.write(m_request.getRequestBytes());
            urlConnection.connect();
            String contentType = urlConnection.getContentType();
            InputStream inputStream = urlConnection.getInputStream();
            byte[] readBuffer = new byte[READ_BUFFER_SIZE];
            ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
            int i;
            while ((i = inputStream.read(readBuffer)) != -1) {
                responseStream.write(readBuffer, 0, i);
            }
            byte[] response = responseStream.toByteArray();
            int responseLength = response.length;
            m_event.getApplication().getLogger().logMessage(3, "Applet HTTP Server Adapter: Response length is: " + responseLength);
            m_event.getApplication().getLogger().logMessage(8, "Applet HTTP Server Adapter: Response: " + response);
            ((ServerConnection) m_event.getConnection()).setServerResource(this, urlConnection);
            m_event.getConnection().setResponse(new HTTPServerResponse(response, contentType));
        } catch (ServerResourceException e) {
            m_event.getApplication().getLogger().logError(3, "Applet HTTP Server Adapter failed to obtain URL Connection.");
            m_event.getConnection().setError(AppError.ms_serverError);
        } catch (Exception e) {
            m_event.getApplication().getLogger().logError(3, "Applet HTTP Server Adapter failed to write to or read from URL Connection. " + "Exception: " + e);
            m_event.getConnection().setError(AppError.ms_serverError);
        }
    }

    /**
     * Creates a URL connection. This method is called by the parent's
     * provideResource method when a URL Connection is not available in the
     * resource pool.
     */
    protected synchronized Object createResource(AppEvent event) throws ServerResourceException {
        try {
            ServerConnection connection = (ServerConnection) event.getConnection();
            Object applet = m_event.getApplication().getCallBackObject();
            if (!(applet instanceof BeanstalkApplet)) {
                throw new Exception("AppletHTTPServerAdapter: Not executing in applet environment.");
            }
            URLConnection urlConnection = new URL(((BeanstalkApplet) applet).getDocumentBase(), connection.getServerID()).openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setRequestProperty("Method", m_request.getMethod());
            urlConnection.setRequestProperty("Content-type", m_request.getContentType());
            return urlConnection;
        } catch (Exception e) {
            m_event.getApplication().getLogger().logError(3, "Applet HTTP Server Adapter failed create URL Connection. " + "Exception: " + e);
            m_event.getConnection().setError(AppError.ms_serverError);
            throw new ServerResourceException();
        }
    }

    /**
     * Overrides ServerAdapter method to return a resource to a pool. Instead
     * the resource (a URLConnection) is closed and the resources in use counter
     * is decremented.
     */
    public synchronized void releaseResource(AppEvent event, Object resource) {
        try {
            String serverID = ((ServerConnection) event.getConnection()).getServerID();
            ms_resourcesInUse--;
            m_event.getApplication().getLogger().logMessage(3, "Applet HTTP Server Adapter released connection for server: " + serverID + " Connections still in use: " + ms_resourcesInUse);
        } catch (Exception e) {
            m_event.getApplication().getLogger().logError(1, "Applet HTTP Server Adapter could not close a connection. Exception: " + e);
        }
    }
}
