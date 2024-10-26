package org.allcolor.services.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.allcolor.services.jndi.JNDIPropertiesConfig;
import org.allcolor.services.servlet.RemoteException;
import org.allcolor.services.servlet.ServiceDescriptor;
import org.allcolor.services.servlet.ServiceRegister;
import org.allcolor.services.xml.BaseXMLSerializer;
import org.allcolor.services.xml.rest.Bind;
import org.allcolor.services.xml.rest.HttpMethod;
import org.allcolor.services.xml.rest.ResourceParameter;
import org.allcolor.services.xml.rest.Service;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.PartSource;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Document;

public final class ServiceLocator {

    private static final class Client implements InvocationHandler, Serializable, Cloneable {

        /**
		 * 
		 */
        private static final long serialVersionUID = -7355434437781137356L;

        private final URL url;

        private Client(final URL url) {
            this.url = url;
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            if ((args != null) && (args.length > 2)) {
                throw new RemoteException("Invalid request !", 400, null);
            }
            Document xmlRequest = null;
            String sXmlRequest = "";
            if ((args != null) && (args.length >= 1) && (args[0] instanceof XmlObject)) {
                final XmlObject xo = (XmlObject) args[0];
                sXmlRequest = xo.xmlText();
                xmlRequest = (Document) xo.getDomNode();
            } else if ((args != null) && (args.length >= 1) && (args[0] != null)) {
                sXmlRequest = BaseXMLSerializer.toXML(args[0]);
                xmlRequest = BaseXMLSerializer.toXMLDOM(args[0]);
            }
            final Bind bind = method.getAnnotation(Bind.class);
            final HttpMethod httpMethod = bind.httpMethod();
            String queryString = "";
            URL toCall = null;
            if ((args != null) && (args.length >= 1) && (args[0] != null)) {
                if (httpMethod == HttpMethod.GET) {
                    queryString = xmlRequest != null ? BaseXMLSerializer.toXMLRequestParameters(xmlRequest) : "";
                }
                final Map<String, String[]> map = BaseXMLSerializer.parseQueryString(!"".equals(queryString) ? queryString : (xmlRequest != null ? BaseXMLSerializer.toXMLRequestParameters(xmlRequest) : ""));
                final ResourceParameter[] rps = bind.resourceParameters();
                final StringBuilder urlAppender = new StringBuilder();
                if (rps != null) {
                    for (final ResourceParameter rp : rps) {
                        urlAppender.append("/");
                        urlAppender.append(rp.urlPrefix());
                        final String[] values = map.get(rp.parameterName());
                        map.remove(rp.parameterName());
                        if ((values != null) && (values.length > 0)) {
                            urlAppender.append(values[0]);
                        } else {
                            if ("".equals(rp.urlPrefix())) {
                                throw new RemoteException("Invalid request !", 400, null);
                            }
                        }
                    }
                } else {
                    urlAppender.append("/");
                }
                queryString = "";
                if (httpMethod == HttpMethod.GET) {
                    for (final Map.Entry<String, String[]> entry : map.entrySet()) {
                        for (final String val : entry.getValue()) {
                            queryString += "&" + URLEncoder.encode(entry.getKey(), "utf-8") + "=" + URLEncoder.encode(val, "utf-8");
                        }
                    }
                }
                String surl = this.url.toExternalForm();
                if (surl.endsWith("/")) {
                    surl = surl.substring(0, surl.length() - 1);
                }
                toCall = new URL(surl + urlAppender.toString() + (!"".equals(queryString) ? "/?" + queryString : "/"));
            } else {
                if ((bind.resourceParameters() != null) && (bind.resourceParameters().length > 0)) {
                    throw new RemoteException("Invalid request !", 400, null);
                }
                toCall = this.url;
            }
            if ((args != null) && (args.length == 2)) {
                try {
                    if ((args[1] instanceof StreamDescriptor) && (httpMethod == HttpMethod.POST)) {
                        final PostMethod post = new PostMethod(toCall.toExternalForm());
                        try {
                            final StreamDescriptor descr = (StreamDescriptor) args[1];
                            queryString = xmlRequest != null ? BaseXMLSerializer.toXMLRequestParameters(xmlRequest) : "";
                            final List<Part> parts = new ArrayList<Part>();
                            final Map<String, String[]> map = BaseXMLSerializer.parseQueryString(queryString);
                            for (final Map.Entry<String, String[]> entry : map.entrySet()) {
                                final String[] values = entry.getValue();
                                if (values != null) {
                                    for (final String value : values) {
                                        parts.add(new StringPart(entry.getKey(), value, "utf-8"));
                                    }
                                }
                            }
                            final InputStream[] files = descr.getStreams();
                            final String[] names = descr.getNames();
                            final long[] lengths = descr.getLengths();
                            for (int i = 0; i < files.length; i++) {
                                final int index = i;
                                parts.add(new FilePart(names[index], new PartSource() {

                                    public InputStream createInputStream() throws IOException {
                                        return files[index];
                                    }

                                    public String getFileName() {
                                        return names[index];
                                    }

                                    public long getLength() {
                                        return lengths[index];
                                    }
                                }));
                            }
                            post.setRequestEntity(new MultipartRequestEntity(parts.toArray(new Part[parts.size()]), post.getParams()));
                            final int status = ServiceLocator.client.executeMethod(post);
                            if (status == 200) {
                                if (InputStream.class.isAssignableFrom(method.getReturnType())) {
                                    return new ByteArrayInputStream(post.getResponseBody());
                                } else {
                                    InputStream in = null;
                                    try {
                                        in = new ByteArrayInputStream(post.getResponseBody());
                                        final Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(method.getReturnType().getName() + "$Factory");
                                        final Method parse = clazz.getDeclaredMethod("parse", InputStream.class);
                                        return parse.invoke(null, in);
                                    } catch (final Exception e) {
                                        throw new RemoteException(e.getMessage(), 500, e);
                                    } finally {
                                        try {
                                            in.close();
                                        } catch (final Exception ignore) {
                                        }
                                    }
                                }
                            } else {
                                throw new RemoteException(post.getStatusLine().getReasonPhrase(), status, null);
                            }
                        } finally {
                            try {
                                post.releaseConnection();
                            } catch (final Exception ignore) {
                            }
                        }
                    } else {
                        throw new RemoteException("Invalid request !", 400, null);
                    }
                } catch (final RemoteException e) {
                    throw e;
                } catch (final Exception e) {
                    throw new RemoteException(e.getMessage(), 500, e);
                }
            } else if ((args == null) || (args.length <= 1)) {
                org.apache.commons.httpclient.HttpMethod hmb = null;
                try {
                    if (httpMethod == HttpMethod.GET) {
                        hmb = new GetMethod(toCall.toExternalForm());
                    } else {
                        byte[] array = null;
                        if ((args != null) && (args.length == 1) && (args[0] != null)) {
                            array = sXmlRequest.getBytes("utf-8");
                        } else {
                            array = new byte[0];
                        }
                        if (httpMethod == HttpMethod.POST) {
                            final PostMethod pm = new PostMethod(toCall.toExternalForm());
                            pm.setRequestEntity(new ByteArrayRequestEntity(array));
                            pm.setRequestHeader("Content-Type", "application/xml");
                            hmb = pm;
                        } else if (httpMethod == HttpMethod.DELETE) {
                            DeleteMethod pm = null;
                            String url = toCall.toExternalForm();
                            if (xmlRequest == null) {
                                pm = new DeleteMethod(toCall.toExternalForm());
                            } else {
                                if (url.indexOf("?") != -1) {
                                    if (url.endsWith("&")) {
                                        url = url.substring(0, url.length() - 1);
                                    }
                                    pm = new DeleteMethod(toCall.toExternalForm() + "&" + BaseXMLSerializer.toXMLRequestParameters(xmlRequest));
                                } else {
                                    pm = new DeleteMethod(toCall.toExternalForm() + "?" + BaseXMLSerializer.toXMLRequestParameters(xmlRequest));
                                }
                            }
                            hmb = pm;
                        } else if (httpMethod == HttpMethod.PUT) {
                            final PutMethod pm = new PutMethod(toCall.toExternalForm());
                            pm.setRequestEntity(new ByteArrayRequestEntity(array));
                            pm.setRequestHeader("Content-Type", "application/xml");
                            hmb = pm;
                        }
                    }
                    final int status = ServiceLocator.client.executeMethod(hmb);
                    if (status == 200) {
                        if (InputStream.class.isAssignableFrom(method.getReturnType())) {
                            return hmb.getResponseBodyAsStream();
                        } else {
                            InputStream in = null;
                            try {
                                in = hmb.getResponseBodyAsStream();
                                if (XmlObject.class.isAssignableFrom(method.getReturnType())) {
                                    final Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(method.getReturnType().getName() + "$Factory");
                                    final Method parse = clazz.getDeclaredMethod("parse", InputStream.class);
                                    return parse.invoke(null, in);
                                } else {
                                    return BaseXMLSerializer.fromXML(in, method.getReturnType());
                                }
                            } catch (final Exception e) {
                                throw new RemoteException(e.getMessage(), 500, e);
                            } finally {
                                try {
                                    in.close();
                                } catch (final Exception ignore) {
                                }
                            }
                        }
                    } else {
                        throw new RemoteException(hmb.getStatusLine().getReasonPhrase(), status, null);
                    }
                } finally {
                    try {
                        hmb.releaseConnection();
                    } catch (final Exception ignore) {
                    }
                }
            } else {
                throw new RemoteException("Invalid request !", 400, null);
            }
        }
    }

    private static final class LocalClient implements InvocationHandler, Serializable, Cloneable {

        /**
		 * 
		 */
        private static final long serialVersionUID = -7355434437781137356L;

        private final ServiceDescriptor sd;

        private LocalClient(final ServiceDescriptor sd) {
            this.sd = sd;
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            if ((args != null) && (args.length > 1)) {
                throw new RemoteException("Invalid local request ! Too much arguments. Max == 1.", 400, null);
            }
            final Bind bind = method.getAnnotation(Bind.class);
            final HttpMethod httpMethod = bind.httpMethod();
            final ResourceParameter[] rps = bind.resourceParameters();
            final String[] urlParameters = new String[rps.length];
            int index = 0;
            for (final ResourceParameter rp : rps) {
                urlParameters[index] = rp.urlPrefix();
                index++;
            }
            final Method toCall = this.sd.getMethod(httpMethod, urlParameters);
            if (toCall == null) {
                throw new RemoteException("Invalid local request ! Method not found.", 400, null);
            }
            try {
                final Object response = ServiceLocator.newInstance(toCall.getParameterTypes()[1]);
                toCall.invoke(this.sd.getService(), new Object[] { args[0], response });
                return response;
            } catch (final InvocationTargetException e) {
                final Throwable t = e.getCause();
                if (t instanceof RemoteException) {
                    throw (RemoteException) t;
                }
                throw new RemoteException("Error while executing local call to " + toCall.toGenericString(), 500, t);
            } catch (final RemoteException e) {
                throw e;
            } catch (final Throwable e) {
                throw new RemoteException("Error while executing local call to " + toCall.toGenericString(), 500, e);
            }
        }
    }

    private static final MultiThreadedHttpConnectionManager _connectionManager = new MultiThreadedHttpConnectionManager();

    private static final HttpClient client = new HttpClient(ServiceLocator._connectionManager);

    private static ServiceLocatorConfig config;

    public static final String XML_MIME_TYPE = "application/xml";

    private static final Map<String, Method> xmlObjectNewInstanceMap = new Hashtable<String, Method>();

    static {
        ServiceLocator.init();
    }

    public static <T> T getLocator(final Class<T> interf) {
        if ((ServiceLocator.config == null) || (ServiceLocator.config.getMapServiceUrl() == null)) {
            ServiceLocator.config = null;
            ServiceLocator.init();
        }
        return ServiceLocator.getLocator(interf, ServiceLocator.config != null ? ServiceLocator.config.getMapServiceUrl().get(interf.getSimpleName()) : null);
    }

    public static <T> T getLocator(final Class<T> interf, final URL url) {
        return ServiceLocator.getLocator(interf, url, false);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getLocator(final Class<T> interf, final URL url, final boolean forceURL) {
        if ((ServiceLocator.config == null) || (ServiceLocator.config.getMapServiceUrl() == null)) {
            ServiceLocator.config = null;
            ServiceLocator.init();
        }
        final String serviceName = interf.getAnnotation(Service.class).name();
        final T ret = (forceURL && url != null) ? null : ServiceLocator.getLocator(interf, serviceName);
        if (ret != null) {
            return ret;
        } else if (url != null) {
            return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] { interf }, new Client(url));
        }
        throw new RemoteException("Service not found. Unknown " + serviceName, 500, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getLocator(final Class<T> interf, final String serviceName) {
        if ((ServiceLocator.config == null) || (ServiceLocator.config.getMapServiceUrl() == null)) {
            ServiceLocator.config = null;
            ServiceLocator.init();
        }
        final ServiceDescriptor sd = ServiceRegister.getHandle().getService("", "/" + serviceName, "");
        if (sd != null) {
            return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] { interf }, new LocalClient(sd));
        }
        return null;
    }

    private static void init() {
        ServiceLocator.client.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());
        try {
            final Enumeration<URL> URLs = Thread.currentThread().getContextClassLoader().getResources("config/service-locator.xml");
            for (; URLs.hasMoreElements(); ) {
                InputStream in = null;
                try {
                    final URL url = URLs.nextElement();
                    in = url.openStream();
                    if (ServiceLocator.config == null) {
                        ServiceLocator.config = BaseXMLSerializer.fromXML(in, ServiceLocatorConfig.class);
                    } else {
                        final ServiceLocatorConfig nconfig = BaseXMLSerializer.fromXML(in, ServiceLocatorConfig.class);
                        for (final Map.Entry<String, URL> entry : nconfig.getMapServiceUrl().entrySet()) {
                            ServiceLocator.config.getMapServiceUrl().put(entry.getKey(), entry.getValue());
                        }
                    }
                } finally {
                    try {
                        in.close();
                    } catch (final Exception ignore) {
                    }
                }
            }
        } catch (final Exception ignore) {
        }
        try {
            final Context initCtx = new InitialContext();
            final Context envCtx = (Context) initCtx.lookup("java:comp/env");
            final JNDIPropertiesConfig jndiProps = (JNDIPropertiesConfig) envCtx.lookup("alcrest/serviceslocator");
            final ServiceLocatorConfig slb = new ServiceLocatorConfig();
            slb.setServicesProperties(jndiProps.getContentAsProperties());
            if (ServiceLocator.config == null) {
                ServiceLocator.config = slb;
            } else {
                for (final Map.Entry<String, URL> entry : slb.getMapServiceUrl().entrySet()) {
                    ServiceLocator.config.getMapServiceUrl().put(entry.getKey(), entry.getValue());
                }
            }
        } catch (final Exception ignore) {
        }
    }

    private static Object newInstance(final Class<?> clazz) throws Exception {
        if (XmlObject.class.isAssignableFrom(clazz)) {
            Method newInstance = ServiceLocator.xmlObjectNewInstanceMap.get(clazz.getName());
            if (newInstance == null) {
                final Class<?> factory = clazz.getClassLoader().loadClass(clazz.getName() + "$Factory");
                newInstance = factory.getMethod("newInstance", new Class<?>[0]);
                ServiceLocator.xmlObjectNewInstanceMap.put(clazz.getName(), newInstance);
            }
            return newInstance.invoke(null, new Object[0]);
        } else {
            return clazz.newInstance();
        }
    }

    private ServiceLocator() {
    }
}
