package com.skruk.elvis.pool;

import com.skruk.elvis.beans.ContextKeeper;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.pool.KeyedPoolableObjectFactory;

/**
 * @author     skruk To change the template for this generated type comment go to Window - Preferences - Java - Code Generation - Code and
 * 				 Comments
 * @created    19 lipiec 2004
 */
public class KeyedXsltPoolableFactory implements KeyedPoolableObjectFactory {

    /** */
    public static final String S_POOL_NAME = "xsltTranslet";

    /** */
    private static final Map MAP_TEMPLATES = new HashMap();

    static {
        String key = "javax.xml.transform.TransformerFactory";
        String value = "org.apache.xalan.xsltc.trax.TransformerFactoryImpl";
        Properties props = System.getProperties();
        props.put(key, value);
        System.setProperties(props);
    }

    /**
	 * @param  xsltName  Description of the Parameter
	 * @return           The template value
	 */
    private static final Templates getTemplate(String xsltName) {
        Templates translet = null;
        synchronized (MAP_TEMPLATES) {
            translet = (Templates) MAP_TEMPLATES.get(xsltName);
            if (translet == null) {
                TransformerFactory tFactory = TransformerFactory.newInstance();
                tFactory.setAttribute("translet-name", xsltName.substring(0, 1).toUpperCase() + xsltName.substring(1) + "Xslt");
                tFactory.setAttribute("destination-directory", ContextKeeper.getInstallDir() + "WEB-INF/classes/");
                tFactory.setAttribute("package-name", "com.skruk.elvis.xslt");
                tFactory.setAttribute("generate-translet", Boolean.TRUE);
                tFactory.setAttribute("auto-translet", Boolean.TRUE);
                tFactory.setAttribute("use-classpath", Boolean.TRUE);
                System.out.println("[DEBUG] name: " + tFactory.getAttribute("translet-name"));
                try {
                    Source source = null;
                    if (xsltName.startsWith("http")) {
                        URL url = new URL(xsltName);
                        source = new StreamSource(url.openStream());
                    } else {
                        String sName = ContextKeeper.getInstallDir() + "xsl/" + xsltName + ".xsl";
                        System.out.println("[DEBUG] sName " + sName);
                        source = new StreamSource(new File(sName));
                    }
                    translet = tFactory.newTemplates(source);
                } catch (TransformerConfigurationException tcex) {
                    tcex.printStackTrace();
                } catch (MalformedURLException muex) {
                    muex.printStackTrace();
                } catch (IOException ioex) {
                    ioex.printStackTrace();
                } finally {
                    MAP_TEMPLATES.put(xsltName, translet);
                }
            }
        }
        return translet;
    }

    /**
	 * (non-Javadoc)
	 *
	 * @param  arg0           Description of the Parameter
	 * @return                Description of the Return Value
	 * @exception  Exception  Description of the Exception
	 * @see                   org.apache.commons.pool.KeyedPoolableObjectFactory#makeObject(java.lang.Object)
	 */
    public Object makeObject(Object arg0) throws Exception {
        Transformer result = null;
        if (arg0 instanceof String) {
            Templates translet = KeyedXsltPoolableFactory.getTemplate((String) arg0);
            result = translet.newTransformer();
        }
        return result;
    }

    /**
	 * @param  key  Description of the Parameter
	 * @param  obj  Description of the Parameter
	 */
    public void activateObject(Object key, Object obj) {
        if (((key == null) || key instanceof String) && (obj != null) && (obj instanceof Transformer)) {
            ((Transformer) obj).clearParameters();
        }
    }

    /**
	 * @param  key  Description of the Parameter
	 * @param  obj  Description of the Parameter
	 */
    public void destroyObject(Object key, Object obj) {
    }

    /**
	 * @param  key  Description of the Parameter
	 * @param  obj  Description of the Parameter
	 */
    public void passivateObject(Object key, Object obj) {
    }

    /**
	 * @param  key  Description of the Parameter
	 * @param  obj  Description of the Parameter
	 * @return      Description of the Return Value
	 */
    public boolean validateObject(Object key, Object obj) {
        return true;
    }
}
