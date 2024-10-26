package org.dbe.composer.wfengine.bpel.server.ref;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.jar.JarEntry;
import javax.wsdl.xml.WSDLLocator;
import org.activebpel.rt.AeException;
import org.apache.log4j.Logger;
import org.dbe.composer.wfengine.util.SdlUtil;
import org.dbe.sdl.exceptions.SDLException;
import org.dbe.sdl.util.SdlJarReaderUtil;
import org.dbe.sdl.util.StringUtils;
import org.dbe.sdl.xml.SDLLocator;
import org.xml.sax.InputSource;

/**
 * @author nickell
 *
 */
public class LocatorImpl implements SDLLocator, WSDLLocator {

    private static final Logger logger = Logger.getLogger(LocatorImpl.class.getName());

    /** The last import file which was requested */
    protected String mLastImportURI;

    /** the sdl file location */
    protected String mLocation;

    /** the sdl file namespace */
    protected String mNamespace;

    /** The factory for mapping import locations to local resources */
    protected ServiceResolver mResolver;

    /**
     * Constructor.
     * @param aResolver used to lookup mappings
     * @param aLocationHint used as a key to resolve the top level read
     */
    public LocatorImpl(ServiceResolver aResolver, String aLocationHint, String aNamespace) {
        logger.debug("LocatorImpl() " + aLocationHint + " " + aNamespace);
        mResolver = aResolver;
        mLocation = aLocationHint;
        mNamespace = aNamespace;
    }

    public String getBaseURI() {
        return mLocation;
    }

    public String getNamespace() {
        return mNamespace;
    }

    /**
     * This value is used as a key by org.dbe.sdl.xml.SDLReaderImpl
     * to cache imports.
     */
    public String getLatestImportURI() {
        return mLastImportURI;
    }

    /**
     * Accessor for resolver.
     */
    protected ServiceResolver getResolver() {
        return mResolver;
    }

    public InputSource getBaseInputSource() {
        logger.debug("getBaseInputSource() " + mLocation);
        if (getResolver().hasWsdlMapping(mLocation)) {
            return loadInternalWsdl(mLocation);
        } else if (getResolver().hasSdlMapping(mLocation)) {
            return loadInternalSdl(mLocation);
        } else {
            logger.warn("OK: I have to load SDL or WSDL here");
            return loadExternalWsdl(mLocation);
        }
    }

    /**
     * Load the sdl from the sdl factory.
     * @param aLocationHint
     */
    protected InputSource loadInternalSdl(String aLocationHint) {
        logger.debug("loadInternalSdl(String) " + aLocationHint);
        InputSource inputSource = null;
        try {
            inputSource = getResolver().getSdlInputSource(aLocationHint);
            logger.info("loaded inputSource " + inputSource.toString());
        } catch (IOException e) {
            logger.error("Error:" + e);
            AeException.logError(e, "Error resolving input source for " + aLocationHint);
        }
        return inputSource;
    }

    /**
     * Load the wsdl from the wsdl factory.
     * @param aLocationHint
     */
    protected InputSource loadInternalWsdl(String aLocationHint) {
        logger.debug("loadInternalWsdl() " + aLocationHint);
        InputSource inSrc = null;
        try {
            inSrc = getResolver().getWsdlInputSource(aLocationHint);
        } catch (IOException e) {
            logger.error("Error: Error resolving input source for " + aLocationHint);
            AeException.logError(e, "Error resolving input source for " + aLocationHint);
        }
        return inSrc;
    }

    /**
     * @see javax.wsdl.xml.WSDLLocator#getImportInputSource(java.lang.String, java.lang.String)
     */
    public InputSource getImportInputSource(String aParentLocation, String aImportLocation) {
        logger.debug("getImportInputSource() for parent " + aParentLocation + " and import " + aImportLocation);
        String parentLocation;
        if (SdlUtil.isNullOrEmpty(aParentLocation)) {
            parentLocation = getBaseURI();
        } else {
            parentLocation = aParentLocation;
        }
        String importURI = SdlUtil.resolveImport(parentLocation, aImportLocation);
        mLastImportURI = importURI;
        if (getResolver().hasWsdlMapping(parentLocation)) {
            int isSdl = importURI.indexOf(".sdl");
            if (isSdl > 0) {
                logger.debug("looking for an sdl file");
            }
            if (getResolver().hasSdlMapping(importURI) || isSdl > 0) {
                return searchSdlCatalog(importURI);
            }
            return searchWsdlCatalog(importURI);
        } else {
            return loadExternalWsdl(importURI);
        }
    }

    /**
     * Retrieve an InputSource from the real world.
     * @param aActualLocation
     * @throws RuntimeException wraps any type of IOException
     */
    protected InputSource loadExternalSdl(String aActualLocation) throws RuntimeException {
        logger.debug("loadExternalSdl(String) " + aActualLocation);
        InputStream inputStream;
        InputSource inputSource = null;
        try {
            String[] sub = aActualLocation.split("bpr:");
            String lookup = sub[0] + "" + sub[1];
            URL lUrl = StringUtils.getURL(null, aActualLocation);
            SdlJarReaderUtil sjru = new SdlJarReaderUtil(lUrl);
            SdlNameFilter ff = new SdlNameFilter(".sdl");
            if (SdlUtil.fileExists(aActualLocation)) {
                Collection sdlEntries = sjru.getEntries(ff);
                if (!SdlUtil.isNullOrEmpty(sdlEntries)) {
                    for (Iterator it = sdlEntries.iterator(); it.hasNext(); ) {
                        JarEntry jarEntry = (JarEntry) it.next();
                        URL url = StringUtils.getURL(null, jarEntry.getName());
                        inputStream = sjru.getInputStream(jarEntry);
                        inputSource = new InputSource(inputStream);
                        inputSource.setSystemId(url.toString());
                        inputStream.close();
                    }
                } else {
                    throw new SDLException(SDLException.NO_SDL_FILE_IN_BPR_JAR, "the scanned jar file should contain sdl service descriptions");
                }
            } else {
                throw new SDLException(SDLException.NO_BPR_FILE_FOUND, "bpr file cannot be found");
            }
        } catch (SDLException e) {
            logger.error("Error: problems with sdl " + aActualLocation + " " + e);
            throw new RuntimeException("problems with sdl " + aActualLocation + " ", e);
        } catch (MalformedURLException e) {
            logger.error("Error: " + aActualLocation + " is not a valid url " + e);
            throw new RuntimeException(aActualLocation + " is not a valid url", e);
        } catch (IOException e) {
            logger.error("Error: error loading sdl from " + aActualLocation + " " + e);
            throw new RuntimeException("error loading sdl from " + aActualLocation, e);
        }
        return inputSource;
    }

    /**
     * Retrieve an InputSource from the real world.
     * @param aActualLocation
     * @throws RuntimeException wraps any type of IOException
     */
    protected InputSource loadExternalWsdl(String aActualLocation) throws RuntimeException {
        logger.debug("loadExternalWsdl() " + aActualLocation);
        try {
            URL url = new URL(aActualLocation);
            return new InputSource(url.openStream());
        } catch (MalformedURLException e) {
            logger.error("Error: " + aActualLocation + " is not a valid url ");
            throw new RuntimeException(aActualLocation + " is not a valid url ", e);
        } catch (IOException e) {
            logger.error("Error: error loading wsdl from " + aActualLocation);
            throw new RuntimeException("error loading wsdl from " + aActualLocation, e);
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
        logger.debug("searchSdlCatalog() " + aImportLocation);
        if (getResolver().hasSdlMapping(aImportLocation)) {
            return loadInternalSdl(aImportLocation);
        }
        return loadExternalSdl(aImportLocation);
    }

    /**
     * Attempts to resolve the import location to a local
     * location (in the bpr).  If none is found, it expects
     * that the importLocation arg is a fully qualified url
     * and it will return an InputSource to it.
     * @param aImportLocation
     */
    protected InputSource searchWsdlCatalog(String aImportLocation) {
        logger.debug("searchWsdlCatalog() " + aImportLocation);
        if (getResolver().hasWsdlMapping(aImportLocation)) {
            return loadInternalWsdl(aImportLocation);
        }
        return loadExternalWsdl(aImportLocation);
    }

    /**
     * Makes a copy of this sdl locator instance.
     */
    protected LocatorImpl makeCopy() {
        logger.debug("makeCopy() for location " + mLocation + " namespace " + mNamespace);
        LocatorImpl locator = new LocatorImpl(mResolver, mLocation, mNamespace);
        return locator;
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
            if (aFilename == null) {
                return false;
            } else if (aFilename.toLowerCase().endsWith(mExt)) {
                return true;
            }
            return false;
        }
    }
}
