package org.dbe.composer.wfengine.bpel.server.deploy.bpr;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;
import org.apache.log4j.Logger;
import org.dbe.composer.wfengine.SdlException;
import org.dbe.composer.wfengine.bpel.server.addressing.ISdlPartnerAddressing;
import org.dbe.composer.wfengine.bpel.server.addressing.pdef.ISdlPartnerDefInfo;
import org.dbe.composer.wfengine.bpel.server.addressing.pdef.SdlXmlPartnerDefInfoReader;
import org.dbe.composer.wfengine.bpel.server.deploy.IServiceDeploymentContext;
import org.dbe.composer.wfengine.bpel.server.deploy.IServiceDeploymentSource;
import org.dbe.composer.wfengine.bpel.server.deploy.SdlDeploymentException;
import org.dbe.composer.wfengine.bpel.server.engine.SdlEngineFactory;
import org.dbe.composer.wfengine.util.SdlCloser;
import org.dbe.composer.wfengine.util.SdlJarReaderUtil;
import org.dbe.composer.wfengine.util.SdlUtil;
import org.dbe.composer.wfengine.xml.XMLParserBase;
import org.w3c.dom.Document;

/**
 * Standard IAeBprFile impl for BPR archive file deployments.
 */
public class BprFile implements IBprFile {

    private static final Logger logger = Logger.getLogger(BprFile.class.getName());

    /** resource name for sdl catalog within the context */
    protected static final String SDL_CATALOG = "META-INF/sdlCatalog.xml";

    /** resource name for wsdl catalog within the context */
    protected static final String WSDL_CATALOG = "META-INF/wsdlCatalog.xml";

    /** The deployment context. */
    protected IServiceDeploymentContext mContext;

    /** The wsdd resource for Axis deployments. */
    protected String mWsddResource;

    /** The pdd resource names. */
    protected Collection mPddResources;

    /** The pdef resource names. */
    protected Collection mPdefResources;

    /** The sdl resource names. */
    protected Collection mSdlResources;

    /** The wsdl resource names. */
    protected Collection mWsdlResources;

    /** XML parser. */
    protected XMLParserBase mSdlParser;

    /** Partner addressing layer */
    protected ISdlPartnerAddressing mAddressing;

    /**
     * Create method for BprFile instances.
     * @param aContext
     * @throws SdlException
     */
    public static BprFile create(IServiceDeploymentContext aContext) throws SdlException {
        logger.debug("create() " + aContext.getShortName());
        BprFile file = new BprFile(aContext, SdlEngineFactory.getPartnerAddressing());
        file.init();
        return file;
    }

    /**
     * Constructor.
     * @param aContext The deployment context.
     * @param aAddressing The partner addressing layer.
     */
    public BprFile(IServiceDeploymentContext aContext, ISdlPartnerAddressing aAddressing) {
        mContext = aContext;
        mAddressing = aAddressing;
    }

    /**
     * Reads through the BPR archive and sets up the internal state.
     * @throws SdlDeploymentException
     */
    public void init() throws SdlDeploymentException {
        logger.debug("init()");
        SdlJarReaderUtil jru = null;
        try {
            jru = new SdlJarReaderUtil(getDeploymentContext().getDeploymentLocation());
            mPddResources = jru.getEntryNames(new SdlNameFilter(".pdd"));
            mPdefResources = jru.getEntryNames(new SdlNameFilter(".pdef"));
            mSdlResources = jru.getEntryNames(new SdlNameFilter(".sdl"));
            mWsdlResources = jru.getEntryNames(new SdlNameFilter(".wsdl"));
            Collection wsdds = jru.getEntryNames(new SdlNameFilter(".wsdd"));
            if (!SdlUtil.isNullOrEmpty(wsdds)) {
                if (!mPddResources.isEmpty()) {
                    logger.error("invalid bpr file format - deploy bpel separately");
                    throw new SdlDeploymentException("invalid bpr file format - deploy bpel separately");
                }
                mWsddResource = (String) wsdds.iterator().next();
            }
        } catch (IOException e) {
            logger.error("error loading" + getDeploymentContext().getDeploymentLocation() + ": " + e);
            throw new SdlDeploymentException("error loading" + getDeploymentContext().getDeploymentLocation(), e);
        } finally {
            if (jru != null) {
                jru.close();
            }
        }
    }

    /**
     * Accessor for the deployment context.
     * @return IServiceDeploymentContext
     */
    protected IServiceDeploymentContext getDeploymentContext() {
        return mContext;
    }

    /**
     * Accessor for the XML parser.
     * @return XMLParserBase
     */
    protected XMLParserBase getSdlParser() {
        if (mSdlParser == null) {
            mSdlParser = new XMLParserBase();
            mSdlParser.setValidating(false);
            mSdlParser.setNamespaceAware(true);
        }
        return mSdlParser;
    }

    public String getBprFileName() {
        return getDeploymentContext().getShortName();
    }

    /**
     * Accessor for the Pdd resource
     * @return Collection
     */
    public Collection getPddResources() {
        return mPddResources;
    }

    protected ISdlPartnerAddressing getPartnerAddressing() {
        return mAddressing;
    }

    public ISdlPartnerDefInfo getPartnerDefInfo(String aPdefResource) throws SdlException {
        logger.debug("getPartnerDefInfo()");
        Document pdefXml = getWsdlDomResource(aPdefResource);
        return SdlXmlPartnerDefInfoReader.read(pdefXml, getPartnerAddressing());
    }

    /**
     * Accessor for the resource as stream
     * @param aResourceName
     * @return InputStream
     */
    public InputStream getResourceAsStream(String aResourceName) {
        return getDeploymentContext().getResourceAsStream(aResourceName);
    }

    /**
     * returns the deployment source for SDL
     * @param aPddName
     * @return IServiceDeploymentSource
     */
    public IServiceDeploymentSource getSdlDeploymentSource(String aPddName) throws SdlException {
        InputStream in = getResourceAsStream(aPddName);
        if (in == null) {
        }
        Document pddDom = getSdlParser().loadSdlDocument(in, null);
        BprDeploymentSource source = new BprDeploymentSource(getDeploymentContext());
        logger.debug("have got pdd=" + aPddName);
        source.setPddDom(pddDom);
        source.setPddName(aPddName);
        source.setPartnerAddressing(getPartnerAddressing());
        source.init();
        return source;
    }

    /**
     * returns the deployment source for WSDL
     * @param aPddName
     * @return IServiceDeploymentSource
     */
    public IServiceDeploymentSource getWsdlDeploymentSource(String aPddName) throws SdlException {
        InputStream in = getResourceAsStream(aPddName);
        Document pddDom = getSdlParser().loadWsdlDocument(in, null);
        BprDeploymentSource source = new BprDeploymentSource(getDeploymentContext());
        logger.debug("have got pdd=" + aPddName);
        source.setPddDom(pddDom);
        source.setPddName(aPddName);
        source.setPartnerAddressing(getPartnerAddressing());
        source.init();
        return source;
    }

    /**
     * returns the SDL catalog as a org.w3c.dom.Document
     * @return Document
     */
    public Document getSdlCatalogDom() throws SdlException {
        return getSdlDomResource(SDL_CATALOG);
    }

    /**
     * returns the WSDL catalog as a org.w3c.dom.Document
     * @return Document
     */
    public Document getWsdlCatalogDom() throws SdlException {
        return getWsdlDomResource(WSDL_CATALOG);
    }

    public String getWsddResource() {
        return mWsddResource;
    }

    public boolean isWsddDeployment() {
        return mWsddResource != null;
    }

    public boolean exists(String aResourceName) {
        return getDeploymentContext().getResourceURL(aResourceName) != null;
    }

    /**
     * returns the resource as an org.w3c.dom.Documnt for name
     * @param aResourceName
     * @return Document either retrieved or if none exists a newly created one
     */
    public Document getSdlDomResource(String aResourceName) throws SdlException {
        InputStream in = null;
        try {
            URL url = getDeploymentContext().getResourceURL(aResourceName);
            if (url == null) {
                return null;
            } else {
                in = url.openStream();
                return getSdlParser().loadSdlDocument(in, null);
            }
        } catch (Throwable t) {
            logger.error("Error: unable to load: " + aResourceName + " from " + getDeploymentContext().getDeploymentLocation());
            throw new SdlDeploymentException(MessageFormat.format("unable to load: {0} from {1}", new Object[] { aResourceName, getDeploymentContext().getDeploymentLocation() }), t);
        } finally {
            SdlCloser.close(in);
        }
    }

    /**
     * returns the resource as an org.w3c.dom.Documnt for name
     * @param aResourceName
     * @return Document either retrieved or if none exists a newly created one
     */
    public Document getWsdlDomResource(String aResourceName) throws SdlException {
        logger.debug("getWsdlDomResource() " + aResourceName);
        InputStream in = null;
        try {
            URL url = getDeploymentContext().getResourceURL(aResourceName);
            if (url == null) {
                logger.error("url is null");
                return null;
            } else {
                logger.debug("loading wsdl document " + aResourceName);
                in = url.openStream();
                return getSdlParser().loadWsdlDocument(in, null);
            }
        } catch (Throwable t) {
            logger.error("Error: " + t + " for " + aResourceName);
            throw new SdlDeploymentException(MessageFormat.format("unable to load: {0} from {1}", new Object[] { aResourceName, getDeploymentContext().getDeploymentLocation() }), t);
        } finally {
            SdlCloser.close(in);
        }
    }

    /**
     * returns Collections of SDL resources
     * @return Collection
     */
    public Collection getSdlResources() throws SdlException {
        return mSdlResources;
    }

    public Collection getPdefResources() {
        return mPdefResources;
    }

    public Collection getWsdlResources() throws SdlException {
        return mWsdlResources;
    }

    /**
     * Convience class - impl of FilenameFilter for building
     * deployment descriptor object.
     */
    static class SdlNameFilter implements FilenameFilter {

        String mExt;

        /**
         * Constructor
         * @param aExt extension to filter on.
         */
        public SdlNameFilter(String aExt) {
            mExt = aExt;
        }

        /**
         * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
         */
        public boolean accept(File aFile, String aFilename) {
            return !SdlUtil.isNullOrEmpty(aFilename) && aFilename.toLowerCase().endsWith(mExt);
        }
    }
}
