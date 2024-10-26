package org.dbe.composer.wfengine.bpel.server.sdl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.activebpel.rt.AeException;
import org.activebpel.rt.bpel.server.AeMessages;
import org.activebpel.rt.util.AeUtil;
import org.apache.log4j.Logger;
import org.dbe.sdl.xml.SDLLocator;
import org.xml.sax.InputSource;

/**
 * Helper class used by SDL reader to resolve sdl imports.
 */
public class SdlLocator implements SDLLocator {

    private static final Logger logger = Logger.getLogger(SdlLocator.class.getName());

    /** The last import file which was requested */
    private String mLastImportURI;

    /** the sdl file location */
    private String mLocation;

    /** The factory for mapping import locations to local resources */
    private SdlResolver mResolver;

    /**
     * Constructor.
     * @param aResolver used to lookup mappings
     * @param aLocationHint used as a key to resolve the top level read
     */
    public SdlLocator(SdlResolver aResolver, String aLocationHint) {
        mResolver = aResolver;
        mLocation = aLocationHint;
    }

    public InputSource getBaseInputSource() {
        if (getResolver().hasMapping(mLocation)) {
            return loadInternalSdl(mLocation);
        } else {
            return loadExternalSdl(mLocation);
        }
    }

    /**
     * Load the sdl from the sdl factory.
     * @param aLocationHint
     */
    protected InputSource loadInternalSdl(String aLocationHint) {
        logger.debug("loadInternalSdl(String) " + aLocationHint);
        InputSource inSrc = null;
        try {
            inSrc = getResolver().getInputSource(aLocationHint);
        } catch (IOException e) {
            logger.error(e);
            AeException.logError(e, AeMessages.getString("AeWsdlLocator.ERROR_0") + aLocationHint);
        }
        return inSrc;
    }

    public String getBaseURI() {
        return mLocation;
    }

    public InputSource getImportInputSource(String aParentLocation, String aImportLocation) {
        String parentLocation;
        if (AeUtil.isNullOrEmpty(aParentLocation)) {
            parentLocation = getBaseURI();
        } else {
            parentLocation = aParentLocation;
        }
        String importURI = AeUtil.resolveImport(parentLocation, aImportLocation);
        mLastImportURI = importURI;
        if (getResolver().hasMapping(parentLocation)) {
            return searchSdlCatalog(importURI);
        } else {
            return loadExternalSdl(importURI);
        }
    }

    /**
     * Retrieve an InputSource from the real world.
     * @param aActualLocation
     * @throws RuntimeException wraps any type of IOException
     */
    protected InputSource loadExternalSdl(String aActualLocation) throws RuntimeException {
        logger.debug("loadExternalSdl(String) " + aActualLocation);
        try {
            URL url = new URL(aActualLocation);
            return new InputSource(url.openStream());
        } catch (MalformedURLException e) {
            logger.error(e);
            throw new RuntimeException(aActualLocation + AeMessages.getString("AeWsdlLocator.ERROR_1"), e);
        } catch (IOException e) {
            logger.error(e);
            throw new RuntimeException(AeMessages.getString("AeWsdlLocator.ERROR_2") + aActualLocation, e);
        }
    }

    /**
     * Attempts to resolve the import location to a local
     * location (in the bpr).  If none is found, it expects
     * that the importLocation arg is a fully qualified url
     * and it will return an InputSource to it.
     * @param aImportLocation
     */
    protected InputSource searchSdlCatalog(String aImportLocation) {
        if (getResolver().hasMapping(aImportLocation)) {
            return loadInternalSdl(aImportLocation);
        }
        return loadExternalSdl(aImportLocation);
    }

    /**
     * This value is used as a key by org.dbe.sdl.xml.SDLReaderImpl
     * to cache imports.
     */
    public String getLatestImportURI() {
        return mLastImportURI;
    }

    /**
     * Makes a copy of this sdl locator instance.
     */
    protected SdlLocator makeCopy() {
        SdlLocator locator = new SdlLocator(mResolver, mLocation);
        return locator;
    }

    /**
     * Accessor for sdl resolver.
     */
    protected SdlResolver getResolver() {
        return mResolver;
    }
}
