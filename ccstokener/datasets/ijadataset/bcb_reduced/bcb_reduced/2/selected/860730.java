package org.s3b.pool;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.jeromedl.beans.ContextKeeper;
import org.s3b.stringdict.Statics;

/**
 * To change the template for this generated type comment go to Window - Preferences - Java - Code
 * Generation - Code and Comments
 *      
 * @author    Sebastian Kruk, Adam Westerski, Paweł Bugalski   
 */
public class KeyedXsltPoolableFactory implements KeyedPoolableObjectFactory {

    private static Logger logger = Logger.getLogger("org.jeromedl.pool");

    /** */
    public static final String S_POOL_NAME = "xsltTranslet";

    /** */
    private static final Map<String, Templates> MAP_TEMPLATES = new HashMap<String, Templates>();

    /**
    *  (non-Javadoc)
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

    /**
    * @param  xsltName  Description of the Parameter
    * @return           The template value
    */
    private static final Templates getTemplate(String xsltName) {
        Templates translet = null;
        synchronized (MAP_TEMPLATES) {
            translet = MAP_TEMPLATES.get(xsltName);
            if (translet == null) {
                TransformerFactory tFactory = TransformerFactory.newInstance();
                tFactory.setAttribute("translet-name", xsltName.substring(0, 1).toUpperCase() + xsltName.substring(1) + "Xslt");
                tFactory.setAttribute("destination-directory", ContextKeeper.getInstallPath() + "WEB-INF/classes/");
                tFactory.setAttribute("package-name", "org.jeromedl.xslt");
                tFactory.setAttribute("generate-translet", Boolean.TRUE);
                tFactory.setAttribute("auto-translet", Boolean.TRUE);
                tFactory.setAttribute("use-classpath", Boolean.TRUE);
                logger.finer("[DEBUG] name: " + tFactory.getAttribute("translet-name"));
                try {
                    Source source = null;
                    if (xsltName.startsWith("http")) {
                        URL url = new URL(xsltName);
                        source = new StreamSource(url.openStream());
                    } else {
                        String sName = ContextKeeper.getInstallPath() + "xsl/" + xsltName + Statics.XSL;
                        logger.finer("[DEBUG] sName " + sName);
                        source = new StreamSource(new File(sName));
                    }
                    translet = tFactory.newTemplates(source);
                } catch (TransformerConfigurationException tcex) {
                    logger.log(Level.INFO, "Error while getting templates", tcex);
                } catch (MalformedURLException muex) {
                    logger.log(Level.INFO, "Error while getting templates", muex);
                } catch (IOException ioex) {
                    logger.log(Level.INFO, "Error while getting templates", ioex);
                } finally {
                    MAP_TEMPLATES.put(xsltName, translet);
                }
            }
        }
        return translet;
    }

    static {
        String key = "javax.xml.transform.TransformerFactory";
        String value = "org.apache.xalan.xsltc.trax.TransformerFactoryImpl";
        Properties props = System.getProperties();
        props.put(key, value);
        System.setProperties(props);
    }
}
