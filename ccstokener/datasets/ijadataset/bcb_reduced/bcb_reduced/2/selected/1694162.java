package org.ji18n.core.container;

import java.beans.XMLDecoder;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.ji18n.core.log.LogLevel;
import org.ji18n.core.log.LogService;
import org.ji18n.core.log.LogServiceFactory;
import org.ji18n.core.util.Classes;

/**
 * @version $Id: BeansContainerService.java 159 2008-07-03 01:28:51Z david_ward2 $
 * @author david at ji18n.org
 */
public class BeansContainerService implements ContainerService {

    private static final String NAME = "beans";

    private static final String CONFIG_NAME = "java";

    private static LogService logService = LogServiceFactory.createLogService(BeansContainerService.class);

    private Map<String, Object> id_obj_map = null;

    public BeansContainerService() {
    }

    public String getName() {
        return NAME;
    }

    public String[] getContainerNames() {
        return new String[] { NAME };
    }

    public String getConfigName() {
        return CONFIG_NAME;
    }

    public void start() {
        if (id_obj_map != null) throw new IllegalStateException("id_obj_map != null");
        id_obj_map = new HashMap<String, Object>();
    }

    public void deploy(URL... urls) {
        for (URL url : urls) {
            InputStream is = null;
            try {
                is = process(url);
                XMLDecoder d = new XMLDecoder(is);
                try {
                    while (true) {
                        Object o = d.readObject();
                        if (o instanceof IdObject) {
                            IdObject id_obj = (IdObject) o;
                            id_obj_map.put(id_obj.getId(), id_obj.getObject());
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                }
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            } finally {
                try {
                    if (is != null) is.close();
                } catch (Throwable t) {
                }
            }
        }
    }

    protected InputStream process(URL url) throws IOException {
        if (logService.isEnabledFor(LogLevel.DEBUG)) logService.log(LogLevel.DEBUG, NAME + "-" + CONFIG_NAME + " -> " + url);
        return transform(url);
    }

    protected InputStream transform(URL url) throws IOException {
        TransformerFactory tf = TransformerFactory.newInstance();
        InputStream xsl_is = null;
        InputStream url_is = null;
        ByteArrayOutputStream os = null;
        byte[] output;
        try {
            xsl_is = Classes.getThreadClassLoader().getResourceAsStream(getStylesheet());
            url_is = new BufferedInputStream(url.openStream());
            os = new ByteArrayOutputStream();
            Transformer tr = tf.newTransformer(new StreamSource(xsl_is));
            tr.transform(new StreamSource(url_is), new StreamResult(os));
            output = os.toByteArray();
        } catch (TransformerConfigurationException tce) {
            throw new IOException(tce.getLocalizedMessage());
        } catch (TransformerException te) {
            throw new IOException(te.getLocalizedMessage());
        } finally {
            try {
                if (os != null) os.close();
            } catch (Throwable t) {
            }
            try {
                if (url_is != null) url_is.close();
            } catch (Throwable t) {
            }
            try {
                if (xsl_is != null) xsl_is.close();
            } catch (Throwable t) {
            }
        }
        if (logService.isEnabledFor(LogLevel.TRACE)) logService.log(LogLevel.TRACE, new String(output));
        return new ByteArrayInputStream(output);
    }

    protected String getStylesheet() {
        return "META-INF/ji18n-core.beans.xsl";
    }

    public String[] getObjectIds() {
        Set<String> set = id_obj_map.keySet();
        return set.toArray(new String[set.size()]);
    }

    public Object getObject(String id) {
        return id_obj_map.get(id);
    }

    public void stop() {
        if (id_obj_map != null) {
            id_obj_map.clear();
            id_obj_map = null;
        }
    }
}
