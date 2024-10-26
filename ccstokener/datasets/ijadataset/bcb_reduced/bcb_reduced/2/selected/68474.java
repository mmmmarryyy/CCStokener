package edu.upmc.opi.caBIG.caTIES.config;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import edu.upmc.opi.caBIG.caTIES.common.CaTIES_Constants;
import edu.upmc.opi.caBIG.common.ProxyURLFactory;

public class ClientConfiguration {

    private static ClientConfiguration singletonConfig;

    public static ClientConfiguration getInstance() throws Exception {
        if (singletonConfig == null) {
            singletonConfig = new ClientConfiguration();
        }
        return singletonConfig;
    }

    private ClientConfiguration() throws Exception {
        initClientConfiguration();
    }

    /**
	 * Reads the client configuration specified as either a filename or a URL. Priority is given to the file
	 * if both exist. 
	 * @throws Exception
	 */
    private void initClientConfiguration() throws Exception {
        String configFile = System.getProperty(CaTIES_Constants.PROPERTY_KEY_CLIENT_CONFIG_FILE);
        String configURLStr = System.getProperty(CaTIES_Constants.PROPERTY_KEY_CLIENT_CONFIG_URL);
        if (configURLStr != null) {
            initClientConfigurationFromURL(configURLStr);
        } else if (configFile != null && edu.upmc.opi.caBIG.caTIES.common.GeneralUtilities.exists(configFile)) {
            initClientConfigurationFromFile(configFile);
        } else throw new Exception("Could not find Client Configuration file or URL");
    }

    private void initClientConfigurationFromURL(String urlStr) throws Exception {
        try {
            URL url = ProxyURLFactory.createHttpUrl(urlStr);
            initClientConfiguration(url.openStream());
        } catch (Exception e) {
            throw new Exception("Could not initialize from Client Configuration URL:" + urlStr, e);
        }
    }

    private void initClientConfigurationFromFile(String filename) throws Exception {
        try {
            if (filename.startsWith("/")) filename = filename.substring(1);
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream inputStream = loader.getResourceAsStream(filename);
            initClientConfiguration(inputStream);
        } catch (Exception e) {
            throw new Exception("Could not initialize from Client Configuration file:" + filename, e);
        }
    }

    private void initClientConfiguration(InputStream inputStream) throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(inputStream);
        Element root = doc.getRootElement();
        initCTRMConfiguration(root.getChild("ctrms"));
        loadSectionsList();
    }

    private void loadSectionsList() {
    }

    private void initCTRMConfiguration(Element ctrms) throws Exception {
        ctrmConfigurations = new ArrayList<CTRMConfiguration>();
        for (Object ctrm : ctrms.getChildren("ctrm")) {
            CTRMConfiguration c = createCTRMConfiguration((Element) ctrm);
            ctrmConfigurations.add(c);
        }
    }

    private CTRMConfiguration createCTRMConfiguration(Element ctrm) {
        CTRMConfiguration c = new CTRMConfiguration();
        c.name = ctrm.getAttributeValue("name");
        c.ip = ctrm.getAttributeValue("ip");
        c.http = ctrm.getAttributeValue("httpPort");
        c.https = ctrm.getAttributeValue("httpsPort");
        c.hibernateFile = ctrm.getAttributeValue("hibernateFile");
        c.anonymousService = ctrm.getAttributeValue("anonymousService");
        return c;
    }

    private List<CTRMConfiguration> ctrmConfigurations;

    private CTRMConfiguration currentCTRM;

    public List<CTRMConfiguration> getCtrmConfigurations() {
        return ctrmConfigurations;
    }

    public static boolean isUsingEmailer() {
        String s = System.getProperty(CaTIES_Constants.USE_EMAILER_KEY);
        if (s != null) return new Boolean(s);
        return false;
    }

    public void setCurrentCTRM(CTRMConfiguration currentCTRM) {
        this.currentCTRM = currentCTRM;
    }

    public CTRMConfiguration getCurrentCTRM() {
        return currentCTRM;
    }

    public static String getApplicationName() {
        return System.getProperty(CaTIES_Constants.PROPERTY_KEY_APPLICATION_NAME);
    }

    public static String getApplicationVersion() {
        return System.getProperty(CaTIES_Constants.PROPERTY_KEY_APPLICATION_VERSION);
    }
}
