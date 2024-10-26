package jaxlib.xmlrpc;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.ConnectIOException;
import java.rmi.MarshalException;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.security.AccessController;
import javax.xml.ws.WebServiceException;
import org.xml.sax.SAXException;
import jaxlib.io.IO;
import jaxlib.security.action.GetClassLoaderAction;
import jaxlib.util.CheckArg;
import jaxlib.util.Strings;

/**
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: XmlRpcClient.java 2730 2009-04-21 01:12:29Z joerg_wassmer $
 */
public class XmlRpcClient extends Object {

    private final XmlRpcClient.Invoker invoker = new Invoker();

    private int readTimeoutMillis = 60000;

    private final URL url;

    public XmlRpcClient(final URL url) {
        super();
        CheckArg.notNull(url, "url");
        this.url = url;
    }

    private String createErrorMsg(final String msg, final String methodName) {
        return Strings.concat("XML-RPC - ", msg, ":" + "\n  service url : ", this.url.toString(), "\n  method name : ", methodName);
    }

    private void tryCloseConnection(final URLConnection connection) {
        if (connection == null) return;
        try {
            closeConnection(connection);
        } catch (final IOException ex) {
        }
    }

    private void closeConnection(final URLConnection connection) throws RemoteException {
        if (connection instanceof HttpURLConnection) {
            final HttpURLConnection http = (HttpURLConnection) connection;
            http.disconnect();
        }
    }

    private URLConnection openConnection(final String methodName) throws RemoteException {
        boolean ok = false;
        URLConnection connection = null;
        try {
            connection = this.url.openConnection();
            connection.setAllowUserInteraction(false);
            connection.setDoOutput(true);
            connection.setReadTimeout(this.readTimeoutMillis);
            connection.setRequestProperty("Content-Type", "text/xml");
            connection.setUseCaches(false);
            if (connection instanceof HttpURLConnection) {
                final HttpURLConnection http = (HttpURLConnection) connection;
                http.setRequestMethod("POST");
            }
            connection.connect();
            ok = true;
            return connection;
        } catch (java.net.UnknownHostException ex) {
            throw new java.rmi.UnknownHostException(createErrorMsg("Unknown host", methodName), ex);
        } catch (final IOException ex) {
            throw new ConnectIOException(createErrorMsg("Can not connect to remote", methodName), ex);
        } finally {
            if (!ok) tryCloseConnection(connection);
        }
    }

    private void sendRequest(final URLConnection connection, XmlRpcRequest request) throws RemoteException {
        OutputStream out = null;
        try {
            out = connection.getOutputStream();
            request.writeXml(out);
            out.flush();
            out.close();
            out = null;
        } catch (final SecurityException ex) {
            throw ex;
        } catch (final RuntimeException ex) {
            throw new MarshalException(createErrorMsg("Unable to marshal request", request.getMethodName()), ex);
        } catch (java.net.UnknownHostException ex) {
            throw new java.rmi.UnknownHostException(createErrorMsg("Unknown host", request.getMethodName()), ex);
        } catch (final IOException ex) {
            throw new ConnectIOException(createErrorMsg("Can not connect to remote", request.getMethodName()), ex);
        } finally {
            IO.tryClose(out);
        }
    }

    public <T> T createProxy(final Class<T> type) {
        return createProxy(null, type);
    }

    @SuppressWarnings("unchecked")
    public <T> T createProxy(ClassLoader classLoader, final Class<T> type) {
        if (classLoader == null) classLoader = AccessController.doPrivileged(new GetClassLoaderAction(type));
        return (T) Proxy.newProxyInstance(classLoader, new Class[] { type }, this.invoker);
    }

    public final int getReadTimeoutMillis() {
        return this.readTimeoutMillis;
    }

    public final URL getURL() {
        return this.url;
    }

    public XmlRpcResponse invoke(XmlRpcRequest request) throws RemoteException {
        CheckArg.notNull(request, "request");
        final String methodName = request.getMethodName();
        URLConnection connection = openConnection(methodName);
        sendRequest(connection, request);
        request = null;
        XmlRpcResponse response;
        InputStream in = null;
        try {
            in = connection.getInputStream();
            response = new XmlRpcResponse(in);
            in.close();
            in = null;
            closeConnection(connection);
            connection = null;
        } catch (final SAXException ex) {
            throw new UnmarshalException(createErrorMsg("Unable to unmarshal response", methodName), ex);
        } catch (final IOException ex) {
            throw new ConnectIOException(createErrorMsg("Receiving response failed", methodName), ex);
        } finally {
            IO.tryClose(in);
            tryCloseConnection(connection);
        }
        return response;
    }

    public final Object invoke(final String methodName) throws RemoteException, XmlRpcServerException {
        return invoke(methodName, (Object[]) null);
    }

    public Object invoke(final String methodName, Object... parameters) throws RemoteException, XmlRpcServerException {
        XmlRpcRequest request = new XmlRpcRequest(methodName, parameters);
        parameters = null;
        XmlRpcResponse response = invoke(request);
        request = null;
        if (response.isFault()) throw new XmlRpcServerException(this.url, methodName, response.getFaultString(), response.getFaultCode());
        final Object[] result = response.parameters;
        response = null;
        return (result == null) ? null : (result.length == 1) ? result[0] : result;
    }

    public void setReadTimeoutMillis(final int timeout) {
        CheckArg.notNegative(timeout, "timeout");
        this.readTimeoutMillis = timeout;
    }

    private final class Invoker extends Object implements InvocationHandler {

        Invoker() {
            super();
        }

        @Override
        public final Object invoke(final Object proxy, final Method m, final Object[] args) throws Throwable {
            try {
                final Class<?> c = m.getDeclaringClass();
                if (c == Object.class) return m.invoke(this, args); else return XmlRpcClient.this.invoke(c.getName() + "." + m.getName(), args);
            } catch (final RemoteException ex) {
                final Class<?>[] exceptionTypes = m.getExceptionTypes();
                if (exceptionTypes.length == 0) {
                    throw new WebServiceException(ex);
                } else {
                    final Class<?> t = ex.getClass();
                    for (final Class<?> c : exceptionTypes) {
                        if (c.isAssignableFrom(t)) throw ex;
                    }
                    throw new WebServiceException(ex);
                }
            }
        }
    }
}
