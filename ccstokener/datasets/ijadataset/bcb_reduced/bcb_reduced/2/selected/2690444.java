package com.cross.cfg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.security.auth.login.Configuration;
import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import com.cross.exception.CrossException;
import com.cross.exception.MappingException;
import com.cross.util.CrossUtil;
import com.cross.util.XMLUtil;
import com.sun.org.apache.bcel.internal.classfile.Attribute;

public class CrossConfiguration {

    Logger log = org.apache.log4j.LogManager.getLogger(CrossConfiguration.class);

    public CrossConfiguration configure() throws CrossException {
        configure("/cross.cfg.xml");
        return this;
    }

    protected InputStream getConfigurationInputStream(String resource) throws CrossException {
        return CrossUtil.getResourceAsStream(resource);
    }

    public Configuration configure(String resource) throws CrossException {
        InputStream stream = getConfigurationInputStream(resource);
        return doConfigure(stream, resource);
    }

    public Configuration configure(URL url) throws CrossException {
        try {
            return doConfigure(url.openStream(), url.toString());
        } catch (IOException ioe) {
            throw new CrossException("could not configure from URL: " + url, ioe);
        }
    }

    public Configuration configure(File configFile) throws CrossException {
        log.info("configuring from file: " + configFile.getName());
        try {
            return doConfigure(new FileInputStream(configFile), configFile.toString());
        } catch (FileNotFoundException fnfe) {
            throw new CrossException("could not find file: " + configFile, fnfe);
        }
    }

    protected Configuration doConfigure(InputStream stream, String resourceName) throws CrossException {
        Document doc;
        try {
            List errors = new ArrayList();
            doc = XMLUtil.createSAXReader(resourceName, errors, entityResolver).read(new InputSource(stream));
            if (errors.size() != 0) throw new MappingException("invalid configuration", (Throwable) errors.get(0));
        } catch (DocumentException e) {
            throw new CrossException("Could not parse configuration: " + resourceName, e);
        } finally {
            try {
                stream.close();
            } catch (IOException ioe) {
                log.warn("could not close input stream for: " + resourceName, ioe);
            }
        }
        return doConfigure(doc);
    }

    public Configuration configure(Document document) throws CrossException {
        log.info("configuring from XML document");
        return doConfigure(XMLUtil.createDOMReader().read(document));
    }

    protected Configuration doConfigure(Document doc) throws CrossException {
        Element sfNode = doc.getRootElement().element("session-factory");
        String name = sfNode.attributeValue("name");
        if (name != null) properties.setProperty("hibernate.session_factory_name", name);
        addProperties(sfNode);
        parseSessionFactory(sfNode, name);
        Element secNode = doc.getRootElement().element("security");
        if (secNode != null) parseSecurity(secNode);
        log.info("Configured SessionFactory: " + name);
        log.debug("properties: " + properties);
        return this;
    }

    private void parseSessionFactory(Element sfNode, String name) {
        Iterator elements = sfNode.elementIterator();
        do {
            if (!elements.hasNext()) break;
            Element subelement = (Element) elements.next();
            String subelementName = subelement.getName();
            if ("mapping".equals(subelementName)) parseMappingElement(subelement, name); else if ("class-cache".equals(subelementName)) {
                String className = subelement.attributeValue("class");
                Attribute regionNode = subelement.attribute("region");
                String region = regionNode != null ? regionNode.getValue() : className;
                boolean includeLazy = !"non-lazy".equals(subelement.attributeValue("include"));
                setCacheConcurrencyStrategy(className, subelement.attributeValue("usage"), region, includeLazy);
            } else if ("collection-cache".equals(subelementName)) {
                String role = subelement.attributeValue("collection");
                Attribute regionNode = subelement.attribute("region");
                String region = regionNode != null ? regionNode.getValue() : role;
                setCollectionCacheConcurrencyStrategy(role, subelement.attributeValue("usage"), region);
            } else if ("listener".equals(subelementName)) parseListener(subelement); else if ("event".equals(subelementName)) parseEvent(subelement);
        } while (true);
    }

    protected void parseMappingElement(Element subelement, String name) {
        Attribute rsrc = subelement.attribute("resource");
        Attribute file = subelement.attribute("file");
        Attribute jar = subelement.attribute("jar");
        Attribute pkg = subelement.attribute("package");
        Attribute clazz = subelement.attribute("class");
        if (rsrc != null) {
            log.debug(name + "<-" + rsrc);
            addResource(rsrc.getValue());
        } else if (jar != null) {
            log.debug(name + "<-" + jar);
            addJar(new File(jar.getValue()));
        } else {
            if (pkg != null) throw new MappingException("An AnnotationConfiguration instance is required to use <mapping package=\"" + pkg.getValue() + "\"/>");
            if (clazz != null) throw new MappingException("An AnnotationConfiguration instance is required to use <mapping class=\"" + clazz.getValue() + "\"/>");
            if (file == null) throw new MappingException("<mapping> element in configuration specifies no attributes");
            log.debug(name + "<-" + file);
            addFile(file.getValue());
        }
    }
}
