package org.iosgi.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.service.command.CommandProcessor;
import org.iosgi.Constants;
import org.iosgi.IsolatedFramework;
import org.iosgi.IsolationAdmin;
import org.iosgi.IsolationConstraint;
import org.iosgi.IsolationConstraintRegistry;
import org.iosgi.IsolationDirective;
import org.iosgi.IsolationEnvironment;
import org.iosgi.IsolationEnvironmentFactory;
import org.iosgi.engine.IsolationEngine;
import org.iosgi.engine.Operation;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sven Schulz
 */
@Component(immediate = true)
@Provides(specifications = { IsolationAdmin.class })
public class IsolationAdminImpl implements IsolationAdmin {

    private static final Logger LOGGER = LoggerFactory.getLogger(IsolationAdminImpl.class);

    private static final Pattern DIRECTIVE = Pattern.compile("level:=([^;]*);([^,]*)");

    private static Dictionary<String, String> toDictionary(Attributes attrs) {
        Dictionary<String, String> d = new Hashtable<String, String>();
        for (Object k : attrs.keySet()) {
            Attributes.Name name = (Attributes.Name) k;
            d.put(name.toString(), attrs.getValue(name).toString());
        }
        return d;
    }

    @ServiceProperty(name = CommandProcessor.COMMAND_SCOPE)
    public String commandScope = "iosgi";

    @ServiceProperty(name = CommandProcessor.COMMAND_FUNCTION)
    public String[] commandFunction = new String[] { "environments", "environment", "spawn", "install", "uninstall", "bundle" };

    @Requires(optional = true)
    private IsolationEnvironment[] environments;

    @Requires(optional = true)
    private IsolatedFramework[] frameworks;

    @Requires(optional = true)
    private IsolationEnvironmentFactory[] factories;

    @Requires
    private IsolationConstraintRegistry registry;

    @Requires
    private IsolationEngine engine;

    @Requires(optional = true)
    private Bundle[] bundles;

    private Map<String, List<IsolationDirective>> directives;

    private Map<String, Dictionary<String, String>> manifestEntries;

    private final BundleContext context;

    IsolationAdminImpl(BundleContext context) {
        this.context = context;
        directives = new HashMap<String, List<IsolationDirective>>();
        manifestEntries = new HashMap<String, Dictionary<String, String>>();
    }

    public void environments() {
        for (IsolationEnvironment ie : environments) {
            System.out.println(ie.getId());
        }
    }

    public IsolationEnvironment environment(String id) {
        return getEnvironment(URI.create(id));
    }

    @Override
    public IsolationEnvironment getEnvironment(URI id) {
        for (IsolationEnvironment ie : environments) {
            if (ie.getId().equals(id)) {
                return ie;
            }
        }
        return null;
    }

    @Override
    public IsolatedFramework getIsolatedFramework(URI environmentId) {
        for (IsolatedFramework f : frameworks) {
            if (f.getId().equals(environmentId)) {
                return f;
            }
        }
        return null;
    }

    @Override
    public URI spawn(URI parent) throws Exception {
        String pssp = parent.getSchemeSpecificPart();
        Map<String, Object> props = new HashMap<String, Object>();
        for (IsolationEnvironmentFactory f : factories) {
            if (f.getId().getSchemeSpecificPart().equals(pssp)) {
                return f.newIsolationEnvironment(props);
            }
        }
        throw new IOException("no matching factory found (" + parent + ")");
    }

    @Override
    public void destroy(final URI id) throws Exception {
        IsolationEnvironment env = getEnvironment(id);
        final Semaphore s = new Semaphore(0);
        ServiceListener l = new ServiceListener() {

            @Override
            public void serviceChanged(ServiceEvent event) {
                if (event.getType() != ServiceEvent.UNREGISTERING) {
                    return;
                }
                ServiceReference ref = event.getServiceReference();
                String[] objClasses = (String[]) ref.getProperty("objectClass");
                boolean contains = Arrays.binarySearch(objClasses, IsolationEnvironment.class.getName()) >= 0;
                if (contains && ref.getProperty("environment.id").equals(id)) {
                    s.release();
                }
            }
        };
        context.addServiceListener(l);
        try {
            env.destroy();
            s.acquire();
        } finally {
            context.removeServiceListener(l);
        }
    }

    @Override
    public Bundle getBundle(String location) {
        for (Bundle b : bundles) {
            String loc = b.getLocation();
            if (loc.equals(location)) {
                return b;
            }
        }
        return null;
    }

    public Bundle bundle(String location) {
        return this.getBundle(location);
    }

    @Override
    public void install(final String location) throws IOException {
        URL lurl = new URL(location);
        InputStream is = lurl.openStream();
        JarInputStream jis = new JarInputStream(is);
        Manifest mf = jis.getManifest();
        if (mf == null) {
            throw new IOException("missing manifest");
        }
        Dictionary<String, String> headers = toDictionary(mf.getMainAttributes());
        manifestEntries.put(location, headers);
        String s = headers.get(Constants.BUNDLE_ISOLATION);
        if (s == null) {
            LOGGER.debug("no isolation constraints specified in manifest");
        } else {
            List<IsolationDirective> directives = this.getDirectives(location);
            Matcher m = DIRECTIVE.matcher(s);
            while (m.find()) {
                int level = Integer.parseInt(m.group(1));
                Filter filter = null;
                try {
                    filter = FrameworkUtil.createFilter(m.group(2));
                } catch (InvalidSyntaxException ise) {
                    throw new IOException("invalid filter in isolation directive: " + m.group(), ise);
                }
                IsolationDirective d = new IsolationDirective(level, filter);
                directives.add(d);
                LOGGER.debug("directive {} added", d);
            }
            for (IsolationDirective d : directives) {
                Filter f = d.getFilter();
                int level = d.getLevel();
                for (Map.Entry<String, Dictionary<String, String>> e : manifestEntries.entrySet()) {
                    String otherLocation = e.getKey();
                    if (!otherLocation.equals(location) && f.match(e.getValue())) {
                        IsolationConstraint c = new IsolationConstraint(location, otherLocation, level);
                        registry.add(c);
                    }
                }
            }
        }
        Dictionary<String, String> props = manifestEntries.get(location);
        for (Map.Entry<String, List<IsolationDirective>> e : directives.entrySet()) {
            String otherLocation = e.getKey();
            if (otherLocation.equals(location)) continue;
            for (IsolationDirective d : e.getValue()) {
                Filter f = d.getFilter();
                if (f.match(props)) {
                    int level = d.getLevel();
                    IsolationConstraint c = new IsolationConstraint(otherLocation, location, level);
                    registry.add(c);
                }
            }
        }
        List<Operation> ops = null;
        try {
            ops = engine.install(location);
        } catch (Exception e) {
            throw new IOException(e);
        }
        execute(ops);
    }

    @Override
    public void uninstall(String location) throws IOException {
        for (IsolationConstraint c : registry.getConstraints(location)) {
            registry.remove(c);
        }
        List<Operation> ops = null;
        try {
            ops = engine.uninstall(location);
        } catch (Exception e) {
            throw new IOException(e);
        }
        execute(ops);
    }

    private void execute(List<Operation> ops) throws IOException {
        LOGGER.debug("performing {} operations ({})", ops.size(), ops);
        for (Operation op : ops) {
            try {
                LOGGER.debug("performing {}", op);
                op.perform(this);
            } catch (Exception e) {
                throw new IOException("operation (" + op + ") failed", e);
            }
        }
    }

    private List<IsolationDirective> getDirectives(final String location) {
        List<IsolationDirective> d = directives.get(location);
        if (d == null) {
            directives.put(location, d = new ArrayList<IsolationDirective>());
        }
        return d;
    }
}
