package org.robcash.commons.plugin.impl;

import static java.util.jar.Attributes.Name.SPECIFICATION_TITLE;
import static java.util.jar.Attributes.Name.SPECIFICATION_VERSION;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.robcash.commons.plugin.BasePlugInManager;
import org.robcash.commons.plugin.InvalidPlugInException;
import org.robcash.commons.plugin.PlugIn;
import org.robcash.commons.plugin.PlugInInstantiationException;
import org.robcash.commons.plugin.PlugInManager;
import org.robcash.commons.plugin.PlugInManagerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JarPlugInManager
 *
 * @author Rob Cash
 * @param <P> Type of plug-in supported by this manager
 */
public class JarPlugInManager<P extends PlugIn> extends BasePlugInManager<P> implements PlugInManager<P> {

    private static final Logger LOG = LoggerFactory.getLogger(JarPlugInManager.class);

    private static final String MANIFEST_RESOURCE = "META-INF/MANIFEST.MF";

    private String plugInSpecification;

    private String plugInSpecificationVersion;

    private String plugInClassAttribute;

    public void setPlugInSpecification(final String plugInSpecification) {
        this.plugInSpecification = plugInSpecification;
    }

    public void setPlugInSpecificationVersion(final String plugInSpecificationVersion) {
        this.plugInSpecificationVersion = plugInSpecificationVersion;
    }

    public void setPlugInClassAttribute(final String plugInClassAttribute) {
        this.plugInClassAttribute = plugInClassAttribute;
    }

    /**
	 * Discovers plug-ins by looking
	 *
	 * @throws PlugInManagerException
	 */
    @Override
    public void discoverPlugIns() throws PlugInManagerException {
        LOG.info("Discovering plug-ins defined in JAR manifests...");
        ClassLoader classLoader = this.getClass().getClassLoader();
        Enumeration<URL> manifests = null;
        try {
            manifests = classLoader.getResources(MANIFEST_RESOURCE);
            if (manifests == null || !manifests.hasMoreElements()) {
                LOG.info("No provider manifests found");
                return;
            }
        } catch (IOException ex) {
            LOG.error("Discovery failed", ex);
            return;
        }
        while (manifests.hasMoreElements()) {
            URL url = manifests.nextElement();
            try {
                Manifest manifest = new Manifest(url.openStream());
                LOG.debug("Validating manifest with URL of " + url);
                if (validatePlugInInfo(manifest)) {
                    P plugIn = instantiatePlugIn(manifest);
                    registerPlugIn(plugIn);
                }
            } catch (IOException e) {
                LOG.error("Failed to load manifest with url " + url, e);
            } catch (InvalidPlugInException e) {
                LOG.error("Provider with url " + url + " is not valid", e);
            } catch (PlugInInstantiationException e) {
                LOG.error("Provider with url " + url + " could not be instantiated", e);
            } catch (Exception e) {
                LOG.error("Provider with url " + url + " could not be initialized", e);
            }
        }
        LOG.info("Found and successfully validated " + getPlugIns().size() + " plug-ins");
    }

    /**
	 * Validate plug-in info. If the Specification-Title and
	 * Specification-Version match {@code plugInSpecification} and
	 * {@code plugInSpecificationVersion} respectfully, then the attribute named
	 * by {@code plugInClassAttribute} must contain the fully qualified class
	 * name of a class that implements the plug-in.
	 *
	 * @param plugInInfo Information about the plug-in to be validated
	 * @return If the manifest notates a valid plugIn {@code true} is returned.
	 *         If the manifest does not denote a plugIn at all, {@code false} is
	 *         returned. If the manifest denotes a plugIn but other critical
	 *         information, such as plugIn class name, is missing or incorrect,
	 *         an exception is thrown
	 * @throws InvalidPlugInException Thrown if plugIn is not valid
	 */
    protected boolean validatePlugInInfo(final Manifest plugInInfo) throws InvalidPlugInException {
        boolean validProvider = false;
        Attributes mainAttributes = plugInInfo.getMainAttributes();
        if (mainAttributes.containsKey(SPECIFICATION_TITLE) && mainAttributes.containsKey(SPECIFICATION_VERSION)) {
            String spec = mainAttributes.getValue(SPECIFICATION_TITLE);
            String version = mainAttributes.getValue(SPECIFICATION_VERSION);
            if (plugInSpecification.equals(spec) && plugInSpecificationVersion.equals(version)) {
                String className = mainAttributes.getValue(plugInClassAttribute);
                if (className == null) {
                    throw new InvalidPlugInException("Manifest does not contain " + plugInClassAttribute + " attribute");
                }
                try {
                    Class<?> clazz = Class.forName(className);
                    if (!PlugIn.class.isAssignableFrom(clazz)) {
                        throw new InvalidPlugInException("Provider class " + className + " does not implement the PlugIn interface");
                    }
                    validProvider = true;
                } catch (ClassNotFoundException e) {
                    throw new InvalidPlugInException("Provider class " + className + " cannot be found on the class path");
                }
            }
        }
        return validProvider;
    }

    /**
	 * Instantiate a plug-in using the information in the Jar manifest
	 *
	 * @param plugInInfo Information about a plug-in
	 * @return Instantiated plug-in
	 * @throws PlugInInstantiationException Thrown if the plug-in cannot be
	 *         instantiated
	 */
    @SuppressWarnings("unchecked")
    protected P instantiatePlugIn(final Manifest plugInInfo) throws PlugInInstantiationException {
        P plugIn = null;
        Attributes mainAttributes = plugInInfo.getMainAttributes();
        try {
            Class<?> clazz = Class.forName(mainAttributes.getValue(plugInClassAttribute));
            plugIn = (P) clazz.newInstance();
            plugIn.onInit();
        } catch (ClassNotFoundException e) {
            throw new PlugInInstantiationException(e);
        } catch (InstantiationException e) {
            throw new PlugInInstantiationException(e);
        } catch (IllegalAccessException e) {
            throw new PlugInInstantiationException(e);
        } catch (Exception e) {
            throw new PlugInInstantiationException("Plug-in initialization failed", e);
        }
        return plugIn;
    }
}
