package org.bejug.javacareers.common.ajax;

import com.sun.faces.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.io.IOUtils;
import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.MethodBinding;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.webapp.UIComponentTag;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;

/**
 * <p/>
 * Phase listener which handles two types of requests:
 * <ol>
 * <li> Responds to requests for the JavaScript file referenced by the rendered
 * textfield component markup</li>
 * <li> Responds to autocompletion requests</li>
 * </ol>
 *
 * @author Tor Norbye
 * @author Ed Burns
 * @author Bavo Bruylandt (Last modification by $Author: shally $)
 * $Revision: 1.14 $ - $Date: 2005/12/20 15:36:46 $
 */
public class PhaseListener implements javax.faces.event.PhaseListener {

    /**
     * Max number of results returned in a single completion request.
     */
    static final int MAX_RESULTS_RETURNED = 10;

    /**
     * viewid for ajax.
     */
    private static final String AJAX_VIEW_ID = "autocomplete.ajax";

    /**
     * id of the script.
     */
    public static final String SCRIPT_VIEW_ID = "textfield_js.ajax";

    /**
     * id for the css.
     */
    public static final String CSS_VIEW_ID = "textfield_css.ajax";

    /**
     * the local logger.
     */
    private static final Log LOG = LogFactory.getLog(PhaseListener.class);

    /**
     * the constructor.
     */
    public PhaseListener() {
        LOG.info("Debug: PhaseListener init");
    }

    /**
     * called after a phaseevent.
     *
     * @param event the PhaseEvent.
     */
    public void afterPhase(PhaseEvent event) {
        LOG.info("Debug: afterPhase init " + event);
        String rootId = event.getFacesContext().getViewRoot().getViewId();
        LOG.info("Debug: rootId = " + rootId);
        if (rootId.endsWith(SCRIPT_VIEW_ID)) {
            handleResourceRequest(event, "script.js", "text/javascript");
        } else if (rootId.endsWith(CSS_VIEW_ID)) {
            handleResourceRequest(event, "styles.css", "text/css");
        } else if (rootId.indexOf(AJAX_VIEW_ID) != -1) {
            handleAjaxRequest(event);
        }
    }

    /**
     * The URL is identified as an "ajax" request, e.g. an asynchronous request,
     * so we need to extract the arguments from the request, invoke the completion
     * method, and return the results in the form of an XML response that the
     * browser JavaScript can handle.
     *
     * @param event the PhaseEvent.
     */
    private void handleAjaxRequest(final PhaseEvent event) {
        FacesContext context = event.getFacesContext();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        Object object = context.getExternalContext().getRequest();
        if (!(object instanceof HttpServletRequest)) {
            return;
        }
        HttpServletRequest request = (HttpServletRequest) object;
        String prefix = request.getParameter("prefix");
        String method = request.getParameter("method");
        StringBuffer sb = new StringBuffer();
        try {
            if (prefix.trim().length() > 0) {
                CompletionResult results = getCompletionItems(context, method, prefix);
                List items = results.getItems();
                int n = Math.min(MAX_RESULTS_RETURNED, items.size());
                sb.append("<?xml version=\"1.0\" encoding=\"");
                sb.append(response.getCharacterEncoding());
                sb.append("\"?>");
                if (n > 0) {
                    sb.append("<items>");
                    Iterator it = items.iterator();
                    while (it.hasNext()) {
                        sb.append("<item>");
                        sb.append(it.next().toString());
                        sb.append("</item>");
                    }
                    sb.append("</items>");
                    response.setContentType("text/xml");
                    response.setHeader("Cache-Control", "no-cache");
                    Writer outWriter = null;
                    try {
                        outWriter = new OutputStreamWriter(response.getOutputStream(), response.getCharacterEncoding());
                        LOG.info("Debug: response.getCharacterEncoding()" + response.getCharacterEncoding());
                        outWriter.write(sb.toString());
                    } catch (IOException e) {
                        LOG.error(e);
                    } finally {
                        IOUtils.closeQuietly(outWriter);
                    }
                    LOG.info("Written: " + sb.toString());
                } else {
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    LOG.info("nothing to send");
                }
                event.getFacesContext().responseComplete();
            }
        } catch (EvaluationException ee) {
            LOG.error(ee);
        }
    }

    /**
     * gets the completionresults.
     *
     * @param context    the FacesContext.
     * @param methodExpr the methodExpression.
     * @param prefix     the prefix.
     * @return a List of completionResults.
     */
    private CompletionResult getCompletionItems(FacesContext context, String methodExpr, String prefix) {
        if (UIComponentTag.isValueReference(methodExpr)) {
            Class[] argTypes = { FacesContext.class, String.class, CompletionResult.class };
            MethodBinding vb = context.getApplication().createMethodBinding(methodExpr, argTypes);
            LOG.info("Debug: MethodBinding: " + vb);
            LOG.info("Debug: FacesContext: " + context);
            LOG.info("Debug: methodExpr: " + methodExpr);
            LOG.info("Debug: prefix: " + prefix);
            CompletionResult result = new CompletionResult();
            Object[] args = { context, prefix, result };
            vb.invoke(context, args);
            LOG.info("Debug: Result: " + result.getItems());
            return result;
        }
        Object[] params = { methodExpr };
        throw new javax.faces.FacesException(Util.getExceptionMessageString(Util.INVALID_EXPRESSION_ID, params));
    }

    /**
     * The URL looks like a request for a resource, such as a JavaScript
     * or CSS file. Write the given resource to the response writer.
     *
     * @param event       the PhaseEvent that occurred.
     * @param resource    a String containing the resource to load.
     * @param contentType a String containing the contentType.
     */
    private void handleResourceRequest(PhaseEvent event, String resource, String contentType) {
        LOG.info("Debug: Trying to load: " + resource);
        URL url = null;
        url = PhaseListener.class.getResource(resource);
        LOG.info("Debug: url = " + url);
        URLConnection conn = null;
        InputStream stream = null;
        BufferedReader bufReader = null;
        HttpServletResponse response = (HttpServletResponse) event.getFacesContext().getExternalContext().getResponse();
        OutputStreamWriter outWriter = null;
        String curLine = null;
        try {
            outWriter = new OutputStreamWriter(response.getOutputStream(), response.getCharacterEncoding());
            conn = url.openConnection();
            conn.setUseCaches(false);
            stream = conn.getInputStream();
            bufReader = new BufferedReader(new InputStreamReader(stream));
            response.setContentType(contentType);
            response.setStatus(200);
            while (null != (curLine = bufReader.readLine())) {
                outWriter.write(curLine + "\n");
            }
            outWriter.flush();
            event.getFacesContext().responseComplete();
        } catch (IOException e) {
            LOG.debug("Can't load resource:" + url.toExternalForm());
        } finally {
            IOUtils.closeQuietly(outWriter);
            IOUtils.closeQuietly(bufReader);
            IOUtils.closeQuietly(stream);
        }
    }

    /**
     * called before the phaseevent...
     *
     * @param event the PhaseEvent that took place.
     */
    public void beforePhase(PhaseEvent event) {
    }

    /**
     * the id of the Phase
     *
     * @return the id of the phase.
     */
    public PhaseId getPhaseId() {
        return PhaseId.RESTORE_VIEW;
    }
}
