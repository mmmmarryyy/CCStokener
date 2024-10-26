package com.j2xtreme.xwidget.xwt.skin.standard;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.j2xtreme.xwidget.core.RenderContext;
import com.j2xtreme.xwidget.ecs.HTMLElement;
import com.j2xtreme.xwidget.ecs.HTMLNode;
import com.j2xtreme.xwidget.util.ClassLoaderUtil;
import com.j2xtreme.xwidget.util.IOUtil;
import com.j2xtreme.xwidget.xwt.HTTPResource;
import com.j2xtreme.xwidget.xwt.JavaScriptResource;

/**
 * @author Rob Schoening
 *
 */
public class JavaScriptResourceSkin extends HTTPResourceSkin {

    public void serveResource(HTTPResource resource, HttpServletRequest request, HttpServletResponse response) throws IOException {
        JavaScriptResource jsr = (JavaScriptResource) resource;
        response.setContentType("text/javascript");
        if (jsr.getScriptText() != null) {
            PrintWriter pw = response.getWriter();
            pw.println(jsr.getScriptText());
        } else if (jsr.getResourceName() != null) {
            URL url = ClassLoaderUtil.getResource(jsr.getResourceName());
            IOUtil.copyData(response.getOutputStream(), url.openStream());
        } else {
            throw new IOException("No Javascript to Serve");
        }
    }

    protected HTMLNode render(RenderContext ctx) {
        HTMLElement script = ctx.getDocument().createElement("script");
        JavaScriptResource jsr = (JavaScriptResource) getWidget();
        script.setAttribute("src", jsr.getURL());
        script.setAttribute("type", "text/javascript");
        script.setCloseTagRequired(true);
        script.addText("");
        return script;
    }
}
