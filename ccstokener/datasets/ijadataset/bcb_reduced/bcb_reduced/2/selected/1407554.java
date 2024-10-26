package org.allcolor.ywt.view;

import org.allcolor.services.xml.BaseXMLSerializer;
import org.allcolor.xml.parser.CShaniDomParser;
import org.allcolor.ywt.YwtVersion;
import org.allcolor.ywt.adapter.web.CEnvironment;
import org.allcolor.ywt.controller.ABean;
import org.allcolor.ywt.filter.CBeanHandlerFilter;
import org.allcolor.ywt.filter.CContext;
import org.allcolor.ywt.filter.CFilterChain;
import org.allcolor.ywt.html.CPadawan;
import org.allcolor.ywt.i18n.CBundlesList;
import org.allcolor.ywt.i18n.CResourceBundle;
import org.allcolor.ywt.utils.LOGGERHelper;
import org.allcolor.alc.thread.Mutex;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
DOCUMENT ME!
 *
 * @author Quentin Anciaux
 * @version 0.1.0
 */
@SuppressWarnings("unchecked")
public class CViewServlet extends HttpServlet implements Serializable {

    public static class Serializer {

        public static final String toBeanURLParams(final Object obj, final String prefix) {
            return BaseXMLSerializer.toBeanXMLRequestParameters(obj, prefix);
        }

        public static final String toBeanFormParams(final Object obj, final String prefix) {
            return BaseXMLSerializer.toBeanXMLFormParameter(obj, prefix);
        }

        public static final String toObjectURLParams(final Object obj, final String prefix) {
            return BaseXMLSerializer.toXMLRequestParameters(obj, prefix);
        }

        public static final String toObjectFormParams(final Object obj, final String prefix) {
            return BaseXMLSerializer.toXMLFormParameter(obj, prefix);
        }
    }

    /** DOCUMENT ME! */
    private static final Logger log = LOGGERHelper.getLogger(CViewServlet.class);

    /** DOCUMENT ME! */
    static final long serialVersionUID = -3080751230157825144L;

    /** DOCUMENT ME! */
    private static final List<CVTools> toolsList = new ArrayList<CVTools>();

    /** DOCUMENT ME! */
    private final Mutex mutex = new Mutex();

    /** DOCUMENT ME! */
    private final VelocityEngine ve = new VelocityEngine();

    /**
	 * DOCUMENT ME!
	 */
    @Override
    public void destroy() {
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param cx DOCUMENT ME!
	 * @param val DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    public final String evaluate(final VelocityContext cx, final String val) throws IOException {
        final StringWriter w = new StringWriter();
        try {
            if (!this.ve.evaluate(cx, w, "evaluate", val)) {
                System.out.println("Error evaluating " + val);
            }
        } catch (final Exception e) {
            w.write(("Error evaluating: '" + val + "'").toCharArray());
        }
        w.flush();
        return w.toString();
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param cx DOCUMENT ME!
	 * @param in DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    public final Map<String, Object> getMap(final VelocityContext cx, final String in) throws IOException {
        final char array[] = in.toCharArray();
        final Map<String, Object> resultMap = new HashMap<String, Object>();
        final String mapName = "ViewResultMap" + new Random().nextInt();
        cx.put(mapName, resultMap);
        StringBuffer fieldName = new StringBuffer();
        StringBuffer fieldValue = new StringBuffer();
        boolean parseValue = false;
        boolean hasStartQuote = false;
        for (int i = 0; i < array.length; i++) {
            if (!parseValue) {
                if (array[i] == '=') {
                    parseValue = true;
                } else {
                    if (array[i] == ',') {
                        continue;
                    }
                    fieldName.append(array[i]);
                }
            } else {
                if (!hasStartQuote) {
                    if (array[i] == '"') {
                        hasStartQuote = true;
                    } else {
                        continue;
                    }
                } else {
                    if ((array[i] == '"') && (i > 0) && (array[i - 1] != '\\')) {
                        parseValue = false;
                        hasStartQuote = false;
                        String value = fieldValue.toString();
                        if (!value.startsWith("$")) {
                            value = "\"" + value + "\"";
                        }
                        final String toEval = "$" + mapName + ".put(\"" + fieldName.toString() + "\"," + value + ")";
                        fieldName = new StringBuffer();
                        fieldValue = new StringBuffer();
                        this.evaluate(cx, toEval);
                    } else {
                        fieldValue.append(array[i]);
                    }
                }
            }
        }
        return resultMap;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public final VelocityEngine getVe() {
        return this.ve;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param config DOCUMENT ME!
	 *
	 * @throws ServletException DOCUMENT ME!
	 */
    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        try {
            this.ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "webapp");
            this.ve.setProperty("webapp.resource.loader.class", ServletContextLoader.class.getName());
            this.ve.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.NullLogSystem");
            this.ve.setProperty(RuntimeConstants.VM_LIBRARY, "/WEB-INF/config/VM_global_library.vm");
            this.ve.init();
            CViewServlet.loadToolbox(CContext.getInstance().getContext());
        } catch (final Exception e) {
            CViewServlet.log.error(CContext.getInstance().getName() + " error while initializing CViewServlet.", e);
            throw new ServletException(e);
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param request DOCUMENT ME!
	 * @param response DOCUMENT ME!
	 *
	 * @throws ServletException DOCUMENT ME!
	 * @throws IOException DOCUMENT ME!
	 */
    @Override
    public void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        final String contexName = request.getContextPath();
        String uri = request.getRequestURI();
        final int indexOfContext = uri.indexOf(contexName);
        if (indexOfContext != -1) {
            uri = uri.substring(indexOfContext + contexName.length());
        }
        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }
        if (CViewServlet.log.isDebugEnabled()) {
            CViewServlet.log.debug(CContext.getInstance().getName() + "CViewServlet servicing : " + uri);
        }
        String doc = null;
        Throwable save = null;
        try {
            this.mutex.acquire();
            final Template templ = CViewServlet.getTemplate(this.ve, uri);
            if (CViewServlet.log.isDebugEnabled()) {
                CViewServlet.log.debug(CContext.getInstance().getName() + "CViewServlet merging document for : " + uri);
            }
            doc = CViewServlet.merge(templ, request, this.ve, this);
            if (CViewServlet.log.isDebugEnabled()) {
                CViewServlet.log.debug(CContext.getInstance().getName() + "CViewServlet merging document for : " + uri + " done.");
            }
        } catch (final Throwable e) {
            final Throwable t = e;
            if (t.getClass() == ThreadDeath.class) {
                throw (ThreadDeath) t;
            }
            Throwable cause = e.getCause();
            while (cause != null) {
                if (cause.getClass() == ThreadDeath.class) {
                    throw (ThreadDeath) cause;
                }
                cause = cause.getCause();
            }
            save = e;
            CViewServlet.log.error(CContext.getInstance().getName() + "CViewServlet error while merging document for : " + uri, e);
            doc = null;
        } finally {
            try {
                this.mutex.release();
            } catch (final Exception ignore) {
                ;
            }
        }
        if (save == null) {
            String encoding = (String) CEnvironment.getInstance().get("org.allcolor.ywt.view.CViewServlet.RESPONSE_CHARSET");
            if ((encoding == null) || "".equals(encoding.trim())) {
                encoding = "utf-8";
            }
            response.setCharacterEncoding(encoding);
            final OutputStream out = response.getOutputStream();
            final byte array[] = doc.getBytes(encoding);
            out.write(array);
            out.flush();
            response.flushBuffer();
        } else {
            CViewServlet.showError(save, response);
            return;
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param ve DOCUMENT ME!
	 * @param templateName DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws Exception DOCUMENT ME!
	 */
    private static final Template getTemplate(final VelocityEngine ve, final String templateName) throws Exception {
        return ve.getTemplate(templateName);
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param context DOCUMENT ME!
	 */
    private static void loadToolbox(final ServletContext context) {
        try {
            final CShaniDomParser parser = new CShaniDomParser();
            final Document doc = parser.parse(context.getResource("/WEB-INF/config/velocity-toolbox.xml"));
            final NodeList toolList = doc.getElementsByTagName("tool");
            for (int i = 0; i < toolList.getLength(); i++) {
                final Element etool = (Element) toolList.item(i);
                final String key = etool.getElementsByTagName("key").item(0).getTextContent();
                final String sscope = etool.getElementsByTagName("scope").item(0).getTextContent();
                final String sclazz = etool.getElementsByTagName("class").item(0).getTextContent();
                try {
                    final Class clazz = Class.forName(sclazz);
                    int scope = CEnvironment.APPLICATION;
                    if ("session".equalsIgnoreCase(sscope)) {
                        scope = CEnvironment.SESSION;
                    }
                    if ("request".equalsIgnoreCase(sscope)) {
                        scope = CEnvironment.REQUEST;
                    }
                    final CVTools tool = new CVTools(key, clazz, scope);
                    CViewServlet.toolsList.add(tool);
                } catch (final Exception ignore) {
                    ;
                }
            }
        } catch (final Exception ignore) {
            ;
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param templ DOCUMENT ME!
	 * @param request DOCUMENT ME!
	 * @param ve DOCUMENT ME!
	 * @param servlet DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws Exception DOCUMENT ME!
	 */
    private static final String merge(final Template templ, final HttpServletRequest request, final VelocityEngine ve, final CViewServlet servlet) throws Exception {
        final ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        final PrintWriter writer = new PrintWriter(new OutputStreamWriter(bOut, "utf-8"));
        final VelocityContext cx = new VelocityContext();
        final CEnvironment env = CEnvironment.getInstance();
        final String urlForBase = request.getScheme() + "://" + request.getHeader("Host") + request.getContextPath() + "/";
        env.set("base.url", urlForBase);
        final List<ABean> list = CBeanHandlerFilter.getFilter().getBeanHandler().getBeans();
        for (final ABean bean : list) {
            cx.put(bean.getName(), bean.getBean());
        }
        for (final Enumeration it = env.getRequest().getAttributeNames(); it.hasMoreElements(); ) {
            final String name = (String) it.nextElement();
            if (name.startsWith("BEANS_")) {
                final String beanName = name.substring(6);
                final ABean bean = CBeanHandlerFilter.getFilter().getBeanHandler().getBeanByName(beanName);
                if (bean != null) {
                    final Field fields[] = bean.getBean().getClass().getDeclaredFields();
                    for (final Field field : fields) {
                        try {
                            field.setAccessible(true);
                            cx.put(field.getName(), field.get(bean.getBean()));
                        } catch (final Exception ignore) {
                            ignore.printStackTrace();
                        }
                    }
                }
            }
        }
        for (final CVTools tool : CViewServlet.toolsList) {
            cx.put(tool.getKey(), tool.getTool());
        }
        cx.put("vcontext", cx);
        cx.put("vservlet", servlet);
        cx.put("res", CBundlesList.getBundles(request));
        cx.put("bundle", new CResourceBundle());
        cx.put("beanscollection", CBeanHandlerFilter.getFilter().getBeanHandler());
        cx.put("env", env);
        cx.put("pad", CPadawan.getInstance());
        cx.put("request", env.getRequest());
        cx.put("session", env.getRequest().getSession(true));
        cx.put("application", env.getContext());
        cx.put("YwtVersion", YwtVersion.getInstance());
        cx.put("System", org.allcolor.alc.filesystem.classloader.System.getSystem());
        cx.put("serializer", new Serializer());
        templ.setEncoding("utf-8");
        templ.process();
        templ.merge(cx, writer);
        writer.flush();
        final String value = new String(bOut.toByteArray(), "utf-8").trim();
        if (CViewServlet.log.isDebugEnabled()) {
            CViewServlet.log.debug(CContext.getInstance().getName() + "merge result:\n" + value);
        }
        return value;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param t DOCUMENT ME!
	 * @param response DOCUMENT ME!
	 */
    private static void showError(final Throwable t, final HttpServletResponse response) {
        try {
            if ((response != null) && !response.isCommitted()) {
                if (t instanceof ResourceNotFoundException) {
                    CFilterChain.sendError(response, t.getMessage(), HttpServletResponse.SC_NOT_FOUND, null);
                } else {
                    CFilterChain.sendError(response, t.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t);
                }
            }
        } catch (final Exception e) {
            ;
        }
    }

    /**
	 * 
	DOCUMENT ME!
	 *
	 * @author Quentin Anciaux
	 * @version 0.1.0
	 */
    public static final class ServletContextLoader extends ResourceLoader {

        /**
		 * DOCUMENT ME!
		 *
		 * @param arg0 DOCUMENT ME!
		 *
		 * @return DOCUMENT ME!
		 */
        @Override
        public long getLastModified(final Resource arg0) {
            try {
                final ServletContext context = CContext.getInstance().getContext();
                final URL url = context.getResource(arg0.getName());
                final URLConnection conn = url.openConnection();
                final long lm = conn.getLastModified();
                try {
                    conn.getInputStream().close();
                } catch (final Exception ignore) {
                    ;
                }
                return lm;
            } catch (final Exception e) {
                return 0;
            }
        }

        /**
		 * DOCUMENT ME!
		 *
		 * @param arg0 DOCUMENT ME!
		 *
		 * @return DOCUMENT ME!
		 *
		 * @throws ResourceNotFoundException DOCUMENT ME!
		 */
        @Override
        public InputStream getResourceStream(final String arg0) throws ResourceNotFoundException {
            try {
                final ServletContext context = CContext.getInstance().getContext();
                final URL url = context.getResource(arg0);
                return url.openStream();
            } catch (final Exception e) {
                return null;
            }
        }

        /**
		 * DOCUMENT ME!
		 *
		 * @param arg0 DOCUMENT ME!
		 */
        @Override
        public void init(final ExtendedProperties arg0) {
        }

        /**
		 * DOCUMENT ME!
		 *
		 * @param arg0 DOCUMENT ME!
		 *
		 * @return DOCUMENT ME!
		 */
        @Override
        public boolean isSourceModified(final Resource arg0) {
            final long lm = this.getLastModified(arg0);
            if (arg0.getLastModified() != lm) {
                arg0.setLastModified(lm);
                return true;
            }
            return false;
        }
    }

    /**
	 * 
	DOCUMENT ME!
	 *
	 * @author Quentin Anciaux
	 * @version 0.1.0
	 */
    private static class CVTools {

        /** DOCUMENT ME! */
        final Class clazz;

        /** DOCUMENT ME! */
        final String key;

        /** DOCUMENT ME! */
        final int scope;

        /**
     * Creates a new CVTools object.
     * 
     * @param key
     *            DOCUMENT ME!
     * @param clazz
     *            DOCUMENT ME!
     * @param scope
     *            DOCUMENT ME!
     */
        public CVTools(final String key, final Class clazz, final int scope) {
            super();
            this.key = key;
            this.clazz = clazz;
            this.scope = scope;
        }

        /**
		 * DOCUMENT ME!
		 *
		 * @return DOCUMENT ME!
		 */
        public final String getKey() {
            return this.key;
        }

        /**
		 * DOCUMENT ME!
		 *
		 * @return DOCUMENT ME!
		 */
        public final Object getTool() {
            final CEnvironment env = CEnvironment.getInstance();
            Object obj = null;
            switch(this.scope) {
                case CEnvironment.APPLICATION:
                    obj = env.getContext().getAttribute(this.clazz.getName());
                    if (obj == null) {
                        try {
                            obj = this.clazz.newInstance();
                            env.getContext().setAttribute(this.clazz.getName(), obj);
                        } catch (final Exception ignore) {
                            ;
                        }
                    }
                    break;
                case CEnvironment.SESSION:
                    obj = env.getRequest().getSession(true).getAttribute(this.clazz.getName());
                    if (obj == null) {
                        try {
                            obj = this.clazz.newInstance();
                            env.getRequest().getSession(true).setAttribute(this.clazz.getName(), obj);
                        } catch (final Exception ignore) {
                            ;
                        }
                    }
                    break;
                case CEnvironment.REQUEST:
                    obj = env.getRequest().getAttribute(this.clazz.getName());
                    if (obj == null) {
                        try {
                            obj = this.clazz.newInstance();
                            env.getRequest().setAttribute(this.clazz.getName(), obj);
                        } catch (final Exception ignore) {
                            ;
                        }
                    }
                    break;
            }
            return obj;
        }
    }
}
