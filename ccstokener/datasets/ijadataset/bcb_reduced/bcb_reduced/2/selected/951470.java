package com.j2xtreme.xwidget.xwt.skin.standard;

import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.j2xtreme.xwidget.core.RenderContext;
import com.j2xtreme.xwidget.ecs.HTMLElement;
import com.j2xtreme.xwidget.ecs.HTMLNode;
import com.j2xtreme.xwidget.util.IOUtil;
import com.j2xtreme.xwidget.xwt.HTTPResource;
import com.j2xtreme.xwidget.xwt.Image;

/**
 * @author rob
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ImageSkin extends HTTPResourceSkin {

    static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ImageSkin.class);

    protected HTMLNode render(RenderContext ctx) {
        Image img = (Image) getWidget();
        HTMLElement imgElement = ctx.getDocument().createElement("img");
        imgElement.setAttribute("src", img.getURL());
        return imgElement;
    }

    public void serveResource(HTTPResource resource, HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
        Image image = (Image) resource;
        log.debug("Serving: " + image);
        URL url = image.getResourceURL();
        int idx = url.toString().lastIndexOf(".");
        String fn = image.getId() + url.toString().substring(idx);
        String cd = "filename=\"" + fn + "\"";
        response.setContentType(image.getContentType());
        log.debug("LOADING: " + url);
        IOUtil.copyData(response.getOutputStream(), url.openStream());
    }
}
