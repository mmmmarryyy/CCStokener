package org.weblayouttag.skin.impl;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.SAXReader;
import org.weblayouttag.WebLayoutRuntimeException;
import org.weblayouttag.mode.StandardFormModes;
import org.weblayouttag.skin.CannotRetrieveResource;
import org.weblayouttag.skin.DocumentRuntimeException;
import org.weblayouttag.skin.Skin;
import org.weblayouttag.skin.SkinManager;
import javax.servlet.ServletContext;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Andy Marek
 * @version May 30, 2005
 */
public class SkinManagerImpl implements SkinManager {

    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

    protected Skin defaultSkin;

    protected ServletContext servletContext;

    protected Map skins = new HashMap();

    protected Map skinsXslCache = new HashMap();

    /**
	 * Instantiates a new <code>SkinManagerImpl</code> object.
	 */
    public SkinManagerImpl(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void add(Skin skin) {
        if (skin == null) {
            throw new NullPointerException();
        }
        if (skin.isCache()) {
            Document stylesheet = this.getDocument(skin.getLocation());
            skinsXslCache.put(skin, stylesheet);
        }
        skins.put(skin.getName(), skin);
    }

    public void freeze() {
    }

    public Skin get(String skin) {
        return (Skin) skins.get(skin);
    }

    public Skin getDefault() {
        return defaultSkin;
    }

    protected Document getDocument(String location) {
        SAXReader saxReader = new SAXReader();
        InputStream is = this.getResource(location);
        Document stylesheet = null;
        try {
            stylesheet = saxReader.read(is);
        } catch (DocumentException e) {
            throw new DocumentRuntimeException(e);
        }
        return stylesheet;
    }

    protected InputStream getResource(String location) {
        if (location == null) {
            return null;
        }
        if (location.startsWith("web:")) {
            if (this.servletContext == null) {
                return null;
            }
            location = location.substring(4);
            return this.servletContext.getResourceAsStream(location);
        }
        if (location.startsWith("classpath:")) {
            location = location.substring(10);
            return this.getClass().getResourceAsStream(location);
        } else if (location.startsWith("http:") || location.startsWith("https:")) {
            try {
                URL url = new URL(location);
                return url.openConnection().getInputStream();
            } catch (IOException e) {
                throw new CannotRetrieveResource(e);
            }
        }
        throw new CannotRetrieveResource();
    }

    public void setDefault(Skin skin) {
        if (skin == null) {
            throw new NullPointerException();
        }
        defaultSkin = skin;
    }

    public Document transform(Skin skin, Document document, String formMode) {
        skin = (Skin) ObjectUtils.defaultIfNull(skin, defaultSkin);
        Document stylesheet = null;
        if (skin.isCache()) {
            stylesheet = (Document) skinsXslCache.get(skin);
        } else {
            stylesheet = this.getDocument(skin.getLocation());
        }
        DocumentSource documentSource = new DocumentSource(document);
        DocumentSource stylesheetSource = new DocumentSource(stylesheet);
        DocumentResult result = new DocumentResult();
        try {
            Transformer transformer = TRANSFORMER_FACTORY.newTransformer(stylesheetSource);
            transformer.setParameter("pageDisplay", StringUtils.defaultString(formMode, StandardFormModes.edit));
            for (Iterator iterator = skin.getProperties().keySet().iterator(); iterator.hasNext(); ) {
                String key = (String) iterator.next();
                if (!"pageDisplay".equals(key)) {
                    transformer.setParameter(key, skin.getProperties().get(key));
                }
            }
            transformer.transform(documentSource, result);
        } catch (TransformerException e) {
            throw new WebLayoutRuntimeException(e);
        }
        return result.getDocument();
    }

    public Document transform(Skin skin, Element element, String formMode) {
        Document document = (Document) ObjectUtils.defaultIfNull(element.getDocument(), DocumentHelper.createDocument(element));
        return this.transform(skin, document, formMode);
    }
}
