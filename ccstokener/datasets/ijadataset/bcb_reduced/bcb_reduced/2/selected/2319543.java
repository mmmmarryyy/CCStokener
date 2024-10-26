package org.personalsmartspace.impl;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import junit.framework.TestCase;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.personalsmartspace.pss_psm_pssmanager.api.platform.IPssManager;

/**
 * @author mcrotty
 * 
 */
public class TestPssStandardStarted extends TestCase {

    static String PSS = "org.personalsmartspace";

    static String CHECK_FILE = "standard.check";

    private Bundle[] bundles = null;

    private Properties checklist = null;

    private ServiceTracker pssTracker = null;

    protected void setUp() throws Exception {
        super.setUp();
        bundles = Activator.bundleContext.getBundles();
        for (int i = 0; i < bundles.length; ++i) {
            if (bundles[i] != null) {
                if ((bundles[i].getSymbolicName() == null) || (!bundles[i].getSymbolicName().startsWith(PSS))) {
                    bundles[i] = null;
                }
            }
        }
        checklist = new Properties();
        try {
            URL url = Activator.bundleContext.getBundle().getResource(CHECK_FILE);
            InputStream is = new BufferedInputStream(url.openStream());
            checklist.load(is);
            is.close();
        } catch (FileNotFoundException fe) {
            fail("Failed to find service checklist file");
        } catch (IOException e) {
            fail("Failed to load service checklist file");
        }
        if (pssTracker == null) {
            pssTracker = new ServiceTracker(Activator.bundleContext, IPssManager.class.getName(), null);
        }
        pssTracker.open();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        if (pssTracker != null) {
            pssTracker.close();
        }
    }

    public void testServices() {
        System.out.println("Checking started services\n--------------");
        ArrayList<String> fails = new ArrayList<String>();
        for (int i = 0; i < bundles.length; ++i) {
            if (bundles[i] != null) {
                String expect = (String) checklist.remove(bundles[i].getSymbolicName());
                int expected = ((expect != null) ? Integer.decode(expect) : 0);
                String bname = getName(bundles[i]);
                if (check(bundles[i], expected)) {
                    System.out.println("Verified       :" + bname);
                } else {
                    System.out.println("Incomplete     :" + bname + " was expecting " + expected + "services");
                    fails.add(bname);
                }
            }
        }
        int missing = 0;
        Set<String> names = checklist.stringPropertyNames();
        for (Iterator i = names.iterator(); i.hasNext(); ) {
            System.out.println("Missing Bundle:" + i.next());
            missing++;
        }
        String message = "Missing " + missing + " bundles, and " + fails.size() + " incomplete services";
        assertTrue(message, ((missing == 0) && (fails.isEmpty())));
    }

    public void testPeerInitialised() {
        IPssManager psm = (IPssManager) pssTracker.getService();
        if (psm != null) {
            assertTrue("Peer is not started", psm.isPeerStarted());
        }
    }

    private boolean check(Bundle bundle, int expected) {
        ServiceReference refs[] = bundle.getRegisteredServices();
        if ((refs != null) && (refs.length >= expected)) {
            return true;
        }
        return false;
    }

    /**
     * Gets the bundle name
     * @param Bundle
     * @return The bundle name
     */
    private String getName(Bundle b) {
        String bname = (String) b.getHeaders().get(Constants.BUNDLE_NAME);
        if (bname == null) {
            bname = b.getSymbolicName();
        }
        return bname;
    }
}
