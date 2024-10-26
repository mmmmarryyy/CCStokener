package com.gorillalogic.faces.renderers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import org.apache.log4j.Logger;
import com.gorillalogic.config.Preferences;
import com.gorillalogic.faces.FacesRuntimeException;
import com.gorillalogic.faces.beans.EditMode;
import com.gorillalogic.faces.beans.GlSession;
import com.gorillalogic.faces.components.Page;
import com.gorillalogic.faces.util.FacesUtils;
import com.gorillalogic.faces.util.ResourceMgr;

public class PageRenderer extends PanelRenderer {

    private static Logger logger = Logger.getLogger(PageRenderer.class);

    public static final String EXPLORER_FORM_ID = "glExpForm";

    public static final String ERRORS_FORM_ID = "glErrForm";

    public void encodeBegin(FacesContext fc, UIComponent c) throws IOException {
        Page page = (Page) c;
        ResponseWriter out = fc.getResponseWriter();
        String styleSheet = ((Page) c).getStyleSheet();
        out.write("<html><head></head><body style='visibility:hidden'>");
        out.write("<link href=\"" + styleSheet + "\" rel=\"stylesheet\" type=\"text/css\">");
        out.write("<script src=\"DHTMLapi.js\"></script><script src=\"gl.js\"></script>");
        if (FacesUtils.isTxnBlocked()) {
            out.write("<script>if (confirm(\"Your request is being blocked by another session.\\nDo you want to abort the other session's open transaction? (Not recommended)\")) {window.location = \"abortAcrossGxe.dex\"}else{window.location.href = \"logout.dex\"}</script>");
        }
        if (page.isMenu()) {
            renderMenu(out);
        }
        String headerAttr = page.getHeader();
        if (!headerAttr.equals("false")) {
            if (!headerAttr.equals("true")) {
                renderHeader(out, page);
            } else {
                renderDefaultHeader(out, page);
            }
        }
        super.encodeBegin(fc, c);
    }

    public void renderHeader(ResponseWriter out, Page page) {
        renderFile(out, ResourceMgr.WEB_RESOURCES_LOCATION + "/" + page.getHeader());
    }

    public void renderFooter(ResponseWriter out, Page page) {
        renderFile(out, ResourceMgr.WEB_RESOURCES_LOCATION + "/" + page.getFooter());
    }

    public void renderMenu(ResponseWriter out) {
        renderFile(out, "faces/dexTemplateHeader.jsp");
    }

    private void renderFile(ResponseWriter out, String file) {
        String host = (String) FacesUtils.getFc().getExternalContext().getRequestHeaderMap().get("Host");
        String context = FacesUtils.getFc().getExternalContext().getRequestContextPath();
        try {
            URL url = new URL("http://" + host + context + "/pages/" + file);
            InputStream in = url.openStream();
            int b = in.read();
            while (b > -1) {
                out.write(b);
                b = in.read();
            }
        } catch (Exception e) {
            throw new FacesRuntimeException(logger, "Error importing " + file + ": " + e.getMessage(), e);
        }
    }

    public void renderDefaultHeader(ResponseWriter out, Page page) {
        String logoImage = page.getImage();
        if (logoImage == null) {
            logoImage = Preferences.getWCILogoImage(null);
        }
        if (logoImage == null) {
            return;
        } else {
            if (!logoImage.startsWith(ResourceMgr.WEB_RESOURCES_LOCATION)) {
                logoImage = ResourceMgr.WEB_RESOURCES_LOCATION + '/' + logoImage;
            }
            logoImage = "../" + logoImage;
        }
        try {
            out.write("<b><img src=\"" + logoImage + "\"></b>");
        } catch (IOException e) {
            throw new FacesRuntimeException(logger, "Error writing default header: " + e.getMessage(), e);
        }
    }

    public void encodeEnd(FacesContext fc, UIComponent c) throws IOException {
        ResponseWriter out = fc.getResponseWriter();
        if (GlSession.getCurrentInstance().isTxnBlocked()) {
            GlSession.getCurrentInstance().setTxnBlocked(false);
            out.write("<script>" + "if (confirm(\"You are blocked by another open transaction. Do you want to cancel it?\")) {" + "window.location = \"abortAcrossGxe.dex\"}" + "else {window.location = \"logout.dex\"}" + "</script>");
        }
        if (GlSession.getCurrentInstance().getResultMsg() != null) {
            out.write("<script>" + "alert(\"" + FacesUtils.escapeQuotes(GlSession.getCurrentInstance().getResultMsg()) + "\")" + "</script>");
        }
        Page page = (Page) c;
        super.encodeEnd(fc, c);
        if (page.getFooter() != null) {
            renderFooter(out, page);
        }
        if (EditMode.getCurrentInstance().isEditMode()) {
            out.write("<script>glEditPage()</script>");
        }
    }
}
