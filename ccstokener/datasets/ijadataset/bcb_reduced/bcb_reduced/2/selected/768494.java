package org.xfeep.asura.bootstrap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.xfeep.asura.core.ComponentDefinition;
import org.xfeep.asura.core.ComponentManager;

/**
 * XmlComponentLoader can load all component definitations from a single xml file whose schema
 * is compatible with  asura-composite-1.0.xsd
 * @author zhang yuexiang
 *
 */
public class XmlComponentLoader implements ComponentLoader {

    public static final String COMPOSITE_XML = "asura_composite_xml";

    JAXBContext jaxbContext;

    public JAXBContext getJaxbContext() {
        return jaxbContext;
    }

    public void setJaxbContext(JAXBContext jaxbContext) {
        this.jaxbContext = jaxbContext;
    }

    InputStream xmlStream;

    boolean streamExternal = true;

    String xmlFile;

    public XmlComponentLoader() throws FileNotFoundException {
        this(getCompositeXmlFile());
    }

    public XmlComponentLoader(File xmlFile) throws FileNotFoundException {
        this(new FileInputStream(xmlFile));
        this.xmlFile = xmlFile.getAbsolutePath();
        this.streamExternal = false;
    }

    public XmlComponentLoader(String xmlFile) throws FileNotFoundException {
        this(new File(xmlFile));
    }

    public XmlComponentLoader(InputStream xmlStream) {
        this.xmlStream = xmlStream;
    }

    public XmlComponentLoader(URL url) throws IOException {
        this.xmlStream = url.openStream();
        this.streamExternal = false;
    }

    public static String getCompositeXmlFile() {
        String xmlFile = System.getProperty(COMPOSITE_XML);
        if (xmlFile == null) {
            xmlFile = "composites/asura_core_composite.xml";
        }
        return xmlFile;
    }

    public List<ComponentDefinition> getStaticLoadComponents(ClassLoader classLoader) throws IOException {
        if (jaxbContext == null) {
            try {
                jaxbContext = JAXBContext.newInstance(CompositeDefinitionInfo.class);
            } catch (JAXBException e) {
                throw new RuntimeException("can not initialize JAXBContext", e);
            }
        }
        List<ComponentDefinition> rt = new ArrayList<ComponentDefinition>();
        try {
            CompositeDefinitionInfo compositeDefinitionInfo = (CompositeDefinitionInfo) jaxbContext.createUnmarshaller().unmarshal(xmlStream);
            for (ComponentDefinitionInfo cdi : compositeDefinitionInfo.components) {
                rt.add(cdi.createComponentDefinition(classLoader));
            }
        } catch (Throwable e) {
            throw new IOException("parse xml error: " + e.getMessage(), e);
        }
        return rt;
    }

    public void initialize(ComponentManager componentManager) {
    }

    public void setupComponentDynamicLoadListener(ComponentDynamicLoadListener listener) {
    }

    @Override
    public String toString() {
        if (xmlFile != null) {
            return this.getClass().getSimpleName() + ": " + xmlFile;
        }
        return this.getClass().getName();
    }
}
