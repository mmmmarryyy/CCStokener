package progranet.components.ajax;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.json.JSONArray;
import net.sf.json.JSONFunction;
import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import progranet.ganesa.metamodel.ClassView;
import progranet.ganesa.metamodel.Menu;
import progranet.ganesa.metamodel.Query;
import progranet.ganesa.metamodel.View;
import progranet.ganesa.model.Ganesa;
import progranet.model.exception.ModelException;
import progranet.omg.cmof.Element;
import progranet.omg.cmof.InstanceSpecification;
import progranet.view.bean.ObjectBeanL;
import progranet.view.bean.SessionBean;
import progranet.view.bean.ViewBean;
import progranet.view.builder.ObjectBuilder;
import progranet.view.util.FacesUtils;

public class AjaxPhaseListener implements PhaseListener {

    private static final long serialVersionUID = 8059764602061241046L;

    protected final Log logger = LogFactory.getLog(this.getClass());

    protected final boolean IS_DEBUG = logger.isDebugEnabled();

    static final int MAX_RESULTS_RETURNED = 10;

    private static final String AJAX_VIEW_ID = "ajax-autocomplete";

    private static final String AJAX_VIEW_GRAPH_ID = "ajax-graph";

    private static final String AJAX_SESSION_ID = "ajax-session";

    private static final String AJAX_SET_VIEW_ID = "ajax-set-view";

    private static final String AJAX_GET_MENU_ID = "ajax-get-menu";

    private static final String AJAX_GET_VIEWS_ID = "ajax-get-views";

    private static final String AJAX_GET_CONTEXT_MENU_ID = "ajax-get-context-menu";

    public static final String SCRIPT_VIEW_ID = "property-reference-field.js";

    public static final String CSS_VIEW_ID = "property-reference-field.css";

    public AjaxPhaseListener() {
    }

    public void afterPhase(PhaseEvent event) {
        if (event.getFacesContext().getViewRoot() == null) return;
        String rootId = event.getFacesContext().getViewRoot().getViewId();
        if (rootId.endsWith(SCRIPT_VIEW_ID)) {
            handleResourceRequest(event, "script.js", "text/javascript");
        } else if (rootId.endsWith(CSS_VIEW_ID)) {
            handleResourceRequest(event, "styles.css", "text/css");
        } else if (rootId.indexOf(AJAX_VIEW_ID) != -1) {
            handleAjaxRequest(event);
        } else if (rootId.indexOf(AJAX_VIEW_GRAPH_ID) != -1) {
            try {
                handleAjaxGraphRequest(event);
            } catch (ModelException e) {
                logger.error("Cannot handle graph request", e);
            }
        } else if (rootId.indexOf(AJAX_SESSION_ID) != -1) {
            handleAjaxSessionRequest(event);
        } else if (rootId.indexOf(AJAX_SET_VIEW_ID) != -1) {
            handleAjaxSetViewRequest(event);
        } else if (rootId.indexOf(AJAX_GET_MENU_ID) != -1) {
            handleAjaxGetMenuRequest(event);
        } else if (rootId.indexOf(AJAX_GET_VIEWS_ID) != -1) {
            handleAjaxGetViewsRequest(event);
        } else if (rootId.indexOf(AJAX_GET_CONTEXT_MENU_ID) != -1) {
            handleAjaxGetContextMenuRequest(event);
        }
    }

    public static String protect(String s) {
        return s == null ? "" : s.replaceAll("<[^>]*>", "").replaceAll("&[^;]*;", "").replaceAll("\"", "'").replaceAll("\\s", " ").replaceAll(",", " ").replaceAll(";", " ");
    }

    private void handleAjaxGetContextMenuRequest(PhaseEvent event) {
        FacesContext context = event.getFacesContext();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        Object object = context.getExternalContext().getRequest();
        if (!(object instanceof HttpServletRequest)) return;
        String elementId = (String) context.getExternalContext().getRequestParameterMap().get("objectId");
        String revisionId = (String) context.getExternalContext().getRequestParameterMap().get("revisionId");
        if ("".equals(revisionId)) revisionId = null;
        if (IS_DEBUG) logger.debug("objectId=" + elementId + "&revisionId=" + revisionId);
        Element element = FacesUtils.getSessionBean().getElement(elementId);
        ViewBean viewBean = FacesUtils.getViewBean();
        View view = viewBean.getView();
        ObjectBeanL objectBean = ObjectBuilder.createObjectBeanL(element, view, null);
        JSONArray json = new JSONArray();
        JSONObject m;
        if (objectBean.getRead()) {
            m = new JSONObject();
            m.put("text", viewBean.getLabel().get(Ganesa.VIEW_CONTEXT_ACTION_VIEW.getName()));
            m.put("icon", "template/silk/page.png");
            m.put("handler", new JSONFunction("viewObject(\"" + elementId + "\"" + (revisionId == null ? "" : ", \"" + revisionId + "\"") + ")"));
            json.add(m);
            if (revisionId == null) {
                m = new JSONObject();
                m.put("text", viewBean.getLabel().get(Ganesa.VIEW_CONTEXT_ACTION_GRAPH.getName()));
                m.put("icon", "template/silk/sitemap.png");
                m.put("handler", new JSONFunction("graphObject(\"" + elementId + "\", \"" + protect(viewBean.getLabelShort(element)) + "\", \"" + protect(viewBean.getLabelShort(element.getMetaClass())) + "\")"));
                json.add(m);
            }
        }
        if (objectBean.getWrite()) {
            if (revisionId == null) {
                m = new JSONObject();
                m.put("text", viewBean.getLabel().get(Ganesa.VIEW_CONTEXT_ACTION_EDIT.getName()));
                m.put("icon", "template/silk/page_edit.png");
                m.put("handler", new JSONFunction("editObject(\"" + elementId + "\")"));
                json.add(m);
                m = new JSONObject();
                m.put("text", viewBean.getLabel().get(Ganesa.VIEW_CONTEXT_ACTION_CLONE.getName()));
                m.put("icon", "template/silk/page_white_copy.png");
                m.put("handler", new JSONFunction("cloneObject(\"" + elementId + "\")"));
                json.add(m);
            } else {
                m = new JSONObject();
                m.put("text", viewBean.getLabel().get(Ganesa.VIEW_CONTEXT_ACTION_REVERT.getName()));
                m.put("icon", "template/silk/page_refresh.png");
                m.put("handler", new JSONFunction("editObject(\"" + elementId + "\", \"" + revisionId + "\"" + ")"));
                json.add(m);
            }
        }
        if (objectBean.getDelete() && revisionId == null) {
            m = new JSONObject();
            m.put("text", viewBean.getLabel().get(Ganesa.VIEW_CONTEXT_ACTION_DELETE.getName()));
            m.put("icon", "template/silk/bin.png");
            m.put("handler", new JSONFunction("deleteObject(\"" + elementId + "\")"));
            json.add(m);
        }
        if (json.size() > 0) json.add("-");
        if (revisionId == null) {
            m = new JSONObject();
            if (FacesUtils.getAccountBean().getDesktop().contains(element)) {
                m.put("text", viewBean.getLabel().get(Ganesa.VIEW_CONTEXT_ACTION_REMOVE_FROM_DESKTOP.getName()));
                m.put("icon", "template/silk/monitor_delete.png");
                m.put("handler", new JSONFunction("removeFromDesktop(\"" + elementId + "\")"));
            } else {
                m.put("text", viewBean.getLabel().get(Ganesa.VIEW_CONTEXT_ACTION_ADD_TO_DESKTOP.getName()));
                m.put("icon", "template/silk/monitor_add.png");
                m.put("handler", new JSONFunction("addToDesktop(\"" + elementId + "\")"));
            }
            json.add(m);
            json.add("-");
        }
        if (element instanceof Query) {
            m = new JSONObject();
            m.put("text", viewBean.getLabel().get(Ganesa.VIEW_CONTEXT_ACTION_EXECUTE.getName()));
            m.put("icon", "template/silk/find.png");
            m.put("handler", new JSONFunction("invokeQuery(\"" + elementId + "\")"));
            json.add(m);
        }
        if (IS_DEBUG) logger.debug("JSON context menu content: " + json);
        try {
            response.setContentType("text/xml;charset=utf-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expires", 0);
            response.setHeader("Pragma", "no-cache");
            response.getWriter().write("/*-secure-\n" + json + "*/");
            response.getWriter().flush();
            response.getWriter().close();
            event.getFacesContext().responseComplete();
        } catch (IOException ioe) {
            logger.error("", ioe);
        }
    }

    private String getMenuRow(ClassView classView) {
        return "<tr>" + "<td><a href=\"objectList.jsf?classId=" + classView.getElement().getId() + "\">" + classView.getLabel() + "</a></td>" + "<td>" + (classView.isAbstract() ? "&nbsp;" : "<a href=\"createObject.jsf?classId=" + classView.getElement().getId() + "\"><img src=\"template/silk/add.png\"></img>") + "</td>" + "</tr>";
    }

    private void handleAjaxGetMenuRequest(PhaseEvent event) {
        FacesContext context = event.getFacesContext();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        Object object = context.getExternalContext().getRequest();
        if (!(object instanceof HttpServletRequest)) return;
        JSONArray json = new JSONArray();
        JSONObject m = new JSONObject();
        boolean dcv = false;
        String html = "<table style=\"width: 100%\"><tr><th style=\"width: 100%\"></th><th></th></tr>";
        for (Element element : FacesUtils.getAccountBean().getDesktop()) {
            if (element instanceof ClassView) {
                html = html + this.getMenuRow((ClassView) element);
                dcv = true;
            }
        }
        if (dcv) {
            html = html + "</table>";
            m.put("title", "<img src=\"template/silk/folder_link.png\"></img>");
            m.put("html", html);
            json.add(m);
        }
        List<Menu> menus = new ArrayList<Menu>();
        Map<Menu, List<ClassView>> classes = new HashMap<Menu, List<ClassView>>();
        for (ClassView classView : FacesUtils.getViewBean().getView().getClassViews()) {
            Menu menu = classView.getMenu();
            if (menu == null) continue;
            if (!menus.contains(menu)) menus.add(menu);
            if (classes.get(menu) == null) {
                classes.put(menu, new ArrayList<ClassView>());
            }
            classes.get(menu).add(classView);
        }
        for (Menu menu : menus) {
            m = new JSONObject();
            html = "<table style=\"width: 100%\"><tr><th style=\"width: 100%\"></th><th></th></tr>";
            for (ClassView classView : classes.get(menu)) html = html + this.getMenuRow(classView);
            html = html + "</table>";
            m.put("title", menu.getLabel());
            m.put("html", html);
            json.add(m);
        }
        if (IS_DEBUG) logger.debug("JSON menu content: " + json);
        try {
            response.setContentType("text/xml;charset=utf-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expires", 0);
            response.setHeader("Pragma", "no-cache");
            response.getWriter().write("/*-secure-\n" + json + "*/");
            response.getWriter().flush();
            response.getWriter().close();
            event.getFacesContext().responseComplete();
        } catch (IOException ioe) {
            logger.error("", ioe);
        }
    }

    private void handleAjaxGetViewsRequest(PhaseEvent event) {
        FacesContext context = event.getFacesContext();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        Object object = context.getExternalContext().getRequest();
        if (!(object instanceof HttpServletRequest)) return;
        JSONArray json = new JSONArray();
        JSONObject m = new JSONObject();
        m.put("title", "<img src=\"template/silk/eye.png\"></img>");
        String html = "";
        try {
            for (View view : FacesUtils.getViewBean().getViews()) {
                if (!view.equals(FacesUtils.getViewBean().getView())) {
                    html = html + "<div><a href=\"#\" onclick=\"setView('" + ((InstanceSpecification) view).getId() + "')\">" + view.getName() + "</a></div>";
                } else {
                    html = html + "<div class=\"selector\">" + view.getName() + "</div>";
                }
            }
        } catch (ModelException e) {
            logger.error("Cannot get available views");
        }
        m.put("html", html);
        json.add(m);
        if (IS_DEBUG) logger.debug("JSON views content: " + json);
        try {
            response.setContentType("text/xml;charset=utf-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expires", 0);
            response.setHeader("Pragma", "no-cache");
            response.getWriter().write("/*-secure-\n" + json + "*/");
            response.getWriter().flush();
            response.getWriter().close();
            event.getFacesContext().responseComplete();
        } catch (IOException ioe) {
            logger.error("", ioe);
        }
    }

    private void handleAjaxSetViewRequest(PhaseEvent event) {
        FacesContext context = event.getFacesContext();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        Object object = context.getExternalContext().getRequest();
        if (!(object instanceof HttpServletRequest)) return;
        String viewId = (String) context.getExternalContext().getRequestParameterMap().get("viewId");
        if (IS_DEBUG) logger.debug("viewId=" + viewId);
        FacesUtils.getSessionBean().setViewId(viewId);
        try {
            response.setContentType("text/xml;charset=utf-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expires", 0);
            response.setHeader("Pragma", "no-cache");
            response.getWriter().flush();
            response.getWriter().close();
            event.getFacesContext().responseComplete();
        } catch (IOException ioe) {
            logger.error("", ioe);
        }
    }

    private void handleAjaxRequest(PhaseEvent event) {
        FacesContext context = event.getFacesContext();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        Object object = context.getExternalContext().getRequest();
        if (!(object instanceof HttpServletRequest)) return;
        HttpServletRequest request = (HttpServletRequest) object;
        if (IS_DEBUG) logger.debug(context.getExternalContext().getRequestParameterMap());
        try {
            request.setCharacterEncoding("UTF-8");
            String prefix = (String) context.getExternalContext().getRequestParameterMap().get("prefix");
            String filter = (String) context.getExternalContext().getRequestParameterMap().get("filter");
            Map<String, String> items = FacesUtils.getSessionBean().complete(prefix, filter);
            int n = Math.min(MAX_RESULTS_RETURNED, items.size());
            if (n > 0) {
                JSONObject json = new JSONObject();
                json.putAll(items);
                request.setCharacterEncoding("UTF-8");
                response.setContentType("text/xml;charset=utf-8");
                response.setHeader("Cache-Control", "no-cache");
                response.setDateHeader("Expires", 0);
                response.setHeader("Pragma", "no-cache");
                response.getWriter().write("/*-secure-\n" + json + "*/");
                response.getWriter().flush();
                response.getWriter().close();
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
            event.getFacesContext().responseComplete();
            return;
        } catch (IOException ioe) {
            logger.error("", ioe);
        }
    }

    private void handleAjaxGraphRequest(PhaseEvent event) throws ModelException {
        FacesContext context = event.getFacesContext();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        Object object = context.getExternalContext().getRequest();
        if (!(object instanceof HttpServletRequest)) return;
        HttpServletRequest request = (HttpServletRequest) object;
        try {
            request.setCharacterEncoding("UTF-8");
            String node = (String) context.getExternalContext().getRequestParameterMap().get("id");
            StringBuffer sb = new StringBuffer();
            sb.append(node + ";");
            List<List<String>> results = getGraphPowiazania(node);
            for (int it = 0; it < results.size(); it++) {
                List<String> p = results.get(it);
                sb.append(p.get(0) + ",");
                sb.append(p.get(1) + ",");
                sb.append(p.get(2) + ",");
                sb.append(p.get(3) + ",");
                sb.append(p.get(4) + ",");
                sb.append(p.get(5) + ";");
            }
            request.setCharacterEncoding("UTF-8");
            response.setContentType("text/xml;charset=utf-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expires", 0);
            response.setHeader("Pragma", "no-cache");
            response.getWriter().write(sb.toString());
            response.getWriter().flush();
            response.getWriter().close();
            event.getFacesContext().responseComplete();
            return;
        } catch (IOException ioe) {
            logger.error("", ioe);
        }
    }

    private List<List<String>> getGraphPowiazania(String node) throws ModelException {
        List<List<String>> result = new ArrayList<List<String>>();
        FacesUtils.getSessionBean().getReferences(node, result);
        return result;
    }

    private void handleAjaxSessionRequest(PhaseEvent event) {
        FacesContext context = event.getFacesContext();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        Object object = context.getExternalContext().getRequest();
        if (!(object instanceof HttpServletRequest)) return;
        HttpServletRequest request = (HttpServletRequest) object;
        try {
            request.setCharacterEncoding("UTF-8");
            String aktywna = (String) context.getExternalContext().getRequestParameterMap().get("aktywna");
            FacesUtils.getSessionBean().registerRequest(context, new Boolean(aktywna).booleanValue());
            StringBuffer sb = new StringBuffer();
            sb.append("<table>");
            Iterator k = FacesUtils.getApplicationBean().getSessions().iterator();
            while (k.hasNext()) {
                SessionBean sesja = (SessionBean) k.next();
                long czas = (sesja.getLastRequest() == null ? 2001 : (new Date()).getTime() - sesja.getLastRequest().getTime());
                boolean rozlaczony = czas > 2000;
                sb.append("<tr>");
                String color = "color: " + (rozlaczony || !sesja.isRequestActive() ? "silver" : "green");
                String decoration = (rozlaczony ? "; text-decoration: line-through" : "");
                sb.append("<td style=\"" + color + decoration + "\">" + sesja.getCurrentAccount().getName() + "</td>");
                sb.append("<td>" + (rozlaczony ? czas : "&nbsp;") + "</td>");
                sb.append("</tr>");
            }
            sb.append("</table>");
            request.setCharacterEncoding("UTF-8");
            response.setContentType("text/xml;charset=utf-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expires", 0);
            response.setHeader("Pragma", "no-cache");
            response.getWriter().write(sb.toString());
            response.getWriter().flush();
            response.getWriter().close();
            event.getFacesContext().responseComplete();
            return;
        } catch (IOException ioe) {
            logger.error("", ioe);
        }
    }

    private void handleResourceRequest(PhaseEvent event, String resource, String contentType) {
        URL url = AjaxPhaseListener.class.getResource(resource);
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
            outWriter.close();
            event.getFacesContext().responseComplete();
        } catch (UnsupportedEncodingException e) {
            String message = "Can't load resource: " + url.toExternalForm();
            logger.error(message, e);
        } catch (IOException e) {
            String message = "Can't load resource: " + url.toExternalForm();
            logger.error(message, e);
        }
    }

    public void beforePhase(PhaseEvent event) {
    }

    public PhaseId getPhaseId() {
        return PhaseId.RESTORE_VIEW;
    }
}
