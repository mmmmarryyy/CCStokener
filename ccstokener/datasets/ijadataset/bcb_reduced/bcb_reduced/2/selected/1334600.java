package org.dbe.composer.wfengine.bpel.webserver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import org.activebpel.rt.AeException;
import org.activebpel.rt.util.AeCloser;
import org.apache.axis.AxisEngine;
import org.apache.axis.ConfigurationException;
import org.apache.axis.Handler;
import org.apache.axis.WSDDEngineConfiguration;
import org.apache.axis.deployment.wsdd.WSDDDeployment;
import org.apache.axis.deployment.wsdd.WSDDDocument;
import org.apache.axis.deployment.wsdd.WSDDException;
import org.apache.axis.deployment.wsdd.WSDDGlobalConfiguration;
import org.apache.axis.encoding.TypeMappingRegistry;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.utils.Messages;
import org.apache.axis.utils.XMLUtils;
import org.apache.log4j.Logger;
import org.dbe.composer.wfengine.util.SdlUtil;
import org.xml.sax.SAXException;

/**
 * This is a reusable implementation of the WSDDEngineConfiguration object.  This object
 * can be instantiated and then used by multiple SdlService objects without reloading the
 * deployment from disk.
 */
public class SdlAxisEngineConfiguration implements WSDDEngineConfiguration {

    /** for deployment logging purposes */
    private static final Logger logger = Logger.getLogger(SdlAxisEngineConfiguration.class.getName());

    /** A byte array containing the contents of the client-config.wsdd file (performance and loading tweak). */
    private static byte[] sConfig = null;

    private static final String DEFAULT_AXIS_CLIENT_CONFIG = "client-config.wsdd";

    /** The cached WSDD deployment. */
    private WSDDDeployment mDeployment = null;

    /**
     * Creates an axis engine configuration object from the given resource name.
     *
     * @param aResourceName The name of the resource to load.
     */
    public SdlAxisEngineConfiguration() {
        init();
    }

    /**
     * Initializes the configuration object.  This method
     * @param aResourceName
     */
    public void init() {
        logger.debug("init()");
        InputStream is = null;
        try {
            is = new ByteArrayInputStream(getConfig());
            WSDDDocument doc = new WSDDDocument(XMLUtils.newDocument(is));
            mDeployment = doc.getDeployment();
        } catch (WSDDException e) {
            logger.error("WSDDException " + e);
            mDeployment = null;
        } catch (ParserConfigurationException pce) {
            logger.error("ParserConfigurationException " + pce);
            mDeployment = null;
        } catch (SAXException se) {
            logger.error("SAXException in " + se);
            mDeployment = null;
        } catch (IOException se) {
            logger.error("SAXException " + se);
            mDeployment = null;
        } finally {
            AeCloser.close(is);
        }
    }

    /**
     * @see org.apache.axis.WSDDEngineConfiguration#getDeployment()
     */
    public WSDDDeployment getDeployment() {
        return mDeployment;
    }

    /**
     * @see org.apache.axis.EngineConfiguration#configureEngine(org.apache.axis.AxisEngine)
     */
    public void configureEngine(AxisEngine engine) throws ConfigurationException {
        try {
            mDeployment.configureEngine(engine);
            engine.refreshGlobalOptions();
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * @see org.apache.axis.EngineConfiguration#writeEngineConfig(org.apache.axis.AxisEngine)
     */
    public void writeEngineConfig(AxisEngine engine) throws ConfigurationException {
    }

    /**
     * @see org.apache.axis.EngineConfiguration#getHandler(javax.xml.namespace.QName)
     */
    public Handler getHandler(QName qname) throws ConfigurationException {
        return mDeployment.getHandler(qname);
    }

    /**
     * @see org.apache.axis.EngineConfiguration#getService(javax.xml.namespace.QName)
     */
    public SOAPService getService(QName qname) throws ConfigurationException {
        SOAPService service = mDeployment.getService(qname);
        if (service == null) {
            throw new ConfigurationException(Messages.getMessage("noService10", qname.toString()));
        }
        return service;
    }

    /**
     * @see org.apache.axis.EngineConfiguration#getServiceByNamespaceURI(java.lang.String)
     */
    public SOAPService getServiceByNamespaceURI(String namespace) throws ConfigurationException {
        return mDeployment.getServiceByNamespaceURI(namespace);
    }

    /**
     * @see org.apache.axis.EngineConfiguration#getTransport(javax.xml.namespace.QName)
     */
    public Handler getTransport(QName qname) throws ConfigurationException {
        return mDeployment.getTransport(qname);
    }

    /**
     * @see org.apache.axis.EngineConfiguration#getTypeMappingRegistry()
     */
    public TypeMappingRegistry getTypeMappingRegistry() throws ConfigurationException {
        return mDeployment.getTypeMappingRegistry();
    }

    /**
     * @see org.apache.axis.EngineConfiguration#getGlobalRequest()
     */
    public Handler getGlobalRequest() throws ConfigurationException {
        return mDeployment.getGlobalRequest();
    }

    /**
     * @see org.apache.axis.EngineConfiguration#getGlobalResponse()
     */
    public Handler getGlobalResponse() throws ConfigurationException {
        return mDeployment.getGlobalResponse();
    }

    /**
     * @see org.apache.axis.EngineConfiguration#getGlobalOptions()
     */
    public Hashtable getGlobalOptions() throws ConfigurationException {
        WSDDGlobalConfiguration globalConfig = mDeployment.getGlobalConfiguration();
        if (globalConfig != null) return globalConfig.getParametersTable();
        return null;
    }

    /**
     * @see org.apache.axis.EngineConfiguration#getDeployedServices()
     */
    public Iterator getDeployedServices() throws ConfigurationException {
        return mDeployment.getDeployedServices();
    }

    /**
     * Implements method by returning the associated deployments roles.
     * @see org.apache.axis.EngineConfiguration#getRoles()
     */
    public List getRoles() {
        return mDeployment.getRoles();
    }

    /**
     * @return Returns the engineConfig.
     */
    protected static byte[] getConfig() {
        if (sConfig == null) loadConfig(DEFAULT_AXIS_CLIENT_CONFIG);
        return sConfig;
    }

    /**
     * User to force the loading of the engine config while we have the
     * right thread context.
     * @param aConfigLoc The path of the configguration file to load.
     */
    public static void loadConfig(String aConfigLoc) {
        logger.debug("loadConfig() " + aConfigLoc);
        InputStream is = null;
        try {
            URL url = SdlUtil.findOnClasspath(aConfigLoc, SdlAxisEngineConfiguration.class);
            is = url.openStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buff = new byte[4096];
            int count = 0;
            while ((count = is.read(buff)) != -1) {
                baos.write(buff, 0, count);
            }
            sConfig = baos.toByteArray();
        } catch (Exception e) {
            logger.error("Error loading Axis client engine configuration (client-config.wsdd).");
            AeException.logError(e, "Error loading Axis client engine configuration (client-config.wsdd).");
        } finally {
            AeCloser.close(is);
        }
    }
}
