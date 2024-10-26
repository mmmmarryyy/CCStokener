package ch.trackedbean.internal;

import java.io.*;
import java.net.*;
import org.eclipse.osgi.baseadaptor.bundlefile.*;
import org.eclipse.osgi.baseadaptor.loader.*;
import org.osgi.framework.*;
import ch.trackedbean.internal.weaving.*;
import ch.trackedbean.tracking.injection.*;
import ch.trackedbean.tracking.internal.*;
import ch.trackedbean.tracking.internal.injection.*;

/**
 * {@link ITrackedBeanWeavingService} implementation checking if a class implements a specified marker interface.
 * 
 * @author M. Hautle
 */
@SuppressWarnings("restriction")
public class MarkerInterfaceWeaver extends AbstractClassAnalyzer implements ITrackedBeanWeavingService {

    /** The class file adapter. */
    private final TrackedBeanAdapter adapter = new TrackedBeanAdapter(this);

    /** The current class path manager. */
    private ClasspathManager cpm;

    /** The symbolic name of the bundle for which this weaver is responsible. */
    private final String bundleName;

    /**
     * Default constructor.
     * 
     * @param bundleName The symbolic name of the bundle ({@link Bundle#getSymbolicName()}) for which this service is responsible
     * @param marker The marker class name
     */
    public MarkerInterfaceWeaver(String bundleName, String marker) {
        super(marker);
        this.bundleName = bundleName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] processClass(String name, byte[] classbytes, ClasspathManager manager) {
        cpm = manager;
        final String internalName = name.replace('.', '/');
        try {
            return adapt(classbytes, internalName);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            cpm = null;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAssociatedBundle() {
        return bundleName;
    }

    /**
     * Adapts the passed class byte code if necessary.
     * 
     * @param b A byte array holding the class file
     * @param name The internal class name
     * @return The adapted bytecode or null
     * @throws IOException If something went wrong
     */
    private byte[] adapt(byte[] b, String name) throws IOException {
        final ClassInformation ci = getInformation(name, b);
        if (ci.tracked || ci.parentTracked) return adapter.adapt(ci, b);
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected InputStream getResourceStream(String name) throws Exception {
        final BundleEntry entry = cpm.findLocalEntry(name);
        if (entry != null) return entry.getInputStream();
        final URL url = cpm.getBaseData().getBundle().getResource(name);
        if (url != null) return url.openStream();
        return null;
    }
}
