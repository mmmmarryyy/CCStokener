package org.eclipse.core.internal.registry;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.eclipse.core.internal.runtime.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A listener for bundle events.  When a bundles come and go we look to see 
 * if there are any extensions or extension points and update the registry accordingly.
 * Using a Synchronous listener here is important. If the
 * bundle activator code tries to access the registry to get its extension
 * points, we need to ensure that they are in the registry before the
 * bundle start is called. By listening sync we are able to ensure that
 * happens.
 */
public class EclipseBundleListener implements SynchronousBundleListener {

    private static final String PLUGIN_MANIFEST = "plugin.xml";

    private static final String FRAGMENT_MANIFEST = "fragment.xml";

    private ExtensionRegistry registry;

    private ServiceTracker xmlTracker;

    public EclipseBundleListener(ExtensionRegistry registry) {
        this.registry = registry;
        xmlTracker = new ServiceTracker(InternalPlatform.getDefault().getBundleContext(), SAXParserFactory.class.getName(), null);
        xmlTracker.open();
    }

    public void bundleChanged(BundleEvent event) {
        Bundle bundle = event.getBundle();
        switch(event.getType()) {
            case BundleEvent.RESOLVED:
                addBundle(bundle);
                break;
            case BundleEvent.UNRESOLVED:
                removeBundle(bundle);
                break;
        }
    }

    public void processBundles(Bundle[] bundles) {
        for (int i = 0; i < bundles.length; i++) {
            if (isBundleResolved(bundles[i])) addBundle(bundles[i]); else removeBundle(bundles[i]);
        }
    }

    private boolean isBundleResolved(Bundle bundle) {
        return (bundle.getState() & (Bundle.RESOLVED | Bundle.ACTIVE | Bundle.STARTING | Bundle.STOPPING)) != 0;
    }

    private void removeBundle(Bundle bundle) {
        registry.remove(bundle.getBundleId());
    }

    private void addBundle(Bundle bundle) {
        if (registry.hasNamespace(bundle.getBundleId())) return;
        Contribution bundleModel = getBundleModel(bundle);
        if (bundleModel == null) return;
        if (Platform.PI_RUNTIME.equals(bundleModel.getNamespace())) Messages.reloadMessages();
        registry.add(bundleModel);
    }

    private boolean isSingleton(Bundle bundle) {
        Dictionary allHeaders = bundle.getHeaders("");
        String symbolicNameHeader = (String) allHeaders.get(Constants.BUNDLE_SYMBOLICNAME);
        try {
            if (symbolicNameHeader != null) {
                ManifestElement[] symbolicNameElements = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, symbolicNameHeader);
                if (symbolicNameElements.length > 0) {
                    String singleton = symbolicNameElements[0].getDirective(Constants.SINGLETON_DIRECTIVE);
                    if (singleton == null) singleton = symbolicNameElements[0].getAttribute(Constants.SINGLETON_DIRECTIVE);
                    if (!"true".equalsIgnoreCase(singleton)) {
                        int status = IStatus.INFO;
                        String manifestVersion = (String) allHeaders.get(org.osgi.framework.Constants.BUNDLE_MANIFESTVERSION);
                        if (manifestVersion == null) {
                            if (InternalPlatform.getDefault().getBundle(symbolicNameElements[0].getValue()) == bundle) {
                                return true;
                            }
                            status = IStatus.ERROR;
                        }
                        if (InternalPlatform.DEBUG_REGISTRY || status == IStatus.ERROR) {
                            String message = NLS.bind(Messages.parse_nonSingleton, bundle.getLocation());
                            InternalPlatform.getDefault().log(new Status(status, Platform.PI_RUNTIME, 0, message, null));
                        }
                        return false;
                    }
                }
            }
        } catch (BundleException e1) {
        }
        return true;
    }

    /**
	 * Tries to create a bundle model from a plugin/fragment manifest in the bundle.
	 */
    private Contribution getBundleModel(Bundle bundle) {
        if (bundle.getBundleId() == 0) return null;
        if (bundle.getSymbolicName() == null) return null;
        if (!isSingleton(bundle)) return null;
        boolean isFragment = InternalPlatform.getDefault().isFragment(bundle);
        if (isFragment) {
            Bundle[] hosts = InternalPlatform.getDefault().getHosts(bundle);
            if (hosts != null && isSingleton(hosts[0]) == false) return null;
        }
        InputStream is = null;
        String manifestType = null;
        String manifestName = isFragment ? FRAGMENT_MANIFEST : PLUGIN_MANIFEST;
        try {
            URL url = bundle.getEntry(manifestName);
            if (url != null) {
                is = url.openStream();
                manifestType = isFragment ? ExtensionsParser.FRAGMENT : ExtensionsParser.PLUGIN;
            }
        } catch (IOException ex) {
            is = null;
        }
        if (is == null) return null;
        try {
            String message = NLS.bind(Messages.parse_problems, bundle.getLocation());
            MultiStatus problems = new MultiStatus(Platform.PI_RUNTIME, ExtensionsParser.PARSE_PROBLEM, message, null);
            ResourceBundle b = null;
            try {
                b = ResourceTranslator.getResourceBundle(bundle);
            } catch (MissingResourceException e) {
            }
            ExtensionsParser parser = new ExtensionsParser(problems);
            Contribution bundleModel = new Contribution(bundle);
            parser.parseManifest(xmlTracker, new InputSource(is), manifestType, manifestName, registry.getObjectManager(), bundleModel, b);
            if (problems.getSeverity() != IStatus.OK) InternalPlatform.getDefault().log(problems);
            return bundleModel;
        } catch (ParserConfigurationException e) {
            logParsingError(bundle, e);
            return null;
        } catch (SAXException e) {
            logParsingError(bundle, e);
            return null;
        } catch (IOException e) {
            logParsingError(bundle, e);
            return null;
        } finally {
            try {
                is.close();
            } catch (IOException ioe) {
            }
        }
    }

    private void logParsingError(Bundle bundle, Exception e) {
        String message = NLS.bind(Messages.parse_failedParsingManifest, bundle.getLocation());
        InternalPlatform.getDefault().log(new Status(IStatus.ERROR, Platform.PI_RUNTIME, 0, message, e));
    }
}
