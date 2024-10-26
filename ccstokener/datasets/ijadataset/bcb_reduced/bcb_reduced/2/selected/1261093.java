package org.tagbox.engine.action;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.InputStream;
import org.tagbox.engine.action.EvaluateAction;
import org.tagbox.engine.Component;
import org.tagbox.engine.TagEnvironment;
import org.tagbox.engine.TagBoxException;
import org.tagbox.engine.TagBoxProcessingException;
import org.tagbox.xml.InputHandler;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.tagbox.util.Log;

public class HitURLAction extends EvaluateAction {

    private static final String TYPE_TEXT = "text";

    private static final String TYPE_XML = "xml";

    private static final String TYPE_NONE = "none";

    private static final String DEFAULT_TYPE = TYPE_NONE;

    private DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    private String baseurl;

    public void init(Component ctxt, Element config) throws TagBoxException {
        baseurl = ctxt.getContext().getParameter("base-url");
    }

    public void process(Element e, TagEnvironment env) throws TagBoxException {
        String href = e.getAttribute("href");
        if (href.equals("")) throw new TagBoxProcessingException(e, "missing 'href' attribute");
        href = evaluate(href, env, e);
        if (href.startsWith("/") || href.startsWith("..")) href = baseurl + href;
        Log.info("HitURL: " + href);
        String type = e.getAttribute("type");
        if (type.equals("")) type = evaluate(type, env, e);
        InputHandler handler;
        if (type.equals("xml")) handler = new InputHandler(InputHandler.TYPE_XML); else if (type.equals("text")) handler = new InputHandler(InputHandler.TYPE_TEXT); else handler = new InputHandler(InputHandler.TYPE_NONE);
        try {
            URL url = new URL(href);
            InputStream response = url.openStream();
            Node result = handler.handle(response);
            if (result == null) e.getParentNode().removeChild(e); else {
                result = e.getOwnerDocument().importNode(result, true);
                e.getParentNode().replaceChild(result, e);
            }
            response.close();
        } catch (MalformedURLException exc) {
            throw new TagBoxProcessingException(e, exc);
        } catch (IOException exc) {
            throw new TagBoxProcessingException(e, exc);
        } catch (ParserConfigurationException exc) {
            throw new TagBoxProcessingException(e, exc);
        } catch (SAXException exc) {
            throw new TagBoxProcessingException(e, exc);
        }
    }
}
