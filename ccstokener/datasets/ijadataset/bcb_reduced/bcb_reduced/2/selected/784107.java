package org.dwgsoftware.raistlin.composition.model.impl;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.avalon.framework.logger.Logger;
import org.dwgsoftware.raistlin.composition.data.ClassLoaderDirective;
import org.dwgsoftware.raistlin.composition.data.ContainmentProfile;
import org.dwgsoftware.raistlin.composition.data.SecurityProfile;
import org.dwgsoftware.raistlin.composition.data.builder.ContainmentProfileBuilder;
import org.dwgsoftware.raistlin.composition.data.builder.XMLContainmentProfileCreator;
import org.dwgsoftware.raistlin.composition.model.ClassLoaderModel;
import org.dwgsoftware.raistlin.composition.model.ComponentModel;
import org.dwgsoftware.raistlin.composition.model.ContainmentModel;
import org.dwgsoftware.raistlin.composition.model.ModelException;
import org.dwgsoftware.raistlin.composition.model.ModelRuntimeException;
import org.dwgsoftware.raistlin.composition.provider.ClassLoaderContext;
import org.dwgsoftware.raistlin.composition.provider.ComponentContext;
import org.dwgsoftware.raistlin.composition.provider.ContainmentContext;
import org.dwgsoftware.raistlin.composition.provider.ModelFactory;
import org.dwgsoftware.raistlin.composition.provider.SecurityModel;
import org.dwgsoftware.raistlin.composition.provider.SystemContext;
import org.dwgsoftware.raistlin.util.i18n.ResourceManager;
import org.dwgsoftware.raistlin.util.i18n.Resources;

/**
 * A factory enabling the establishment of new composition model instances.
 *
 * @author <a href="mailto:dev@avalon.apache.org">Avalon Development Team</a>
 * @version $Revision: 1.1 $ $Date: 2005/09/06 00:58:18 $
 */
public class DefaultModelFactory implements ModelFactory {

    private static final XMLContainmentProfileCreator CREATOR = new XMLContainmentProfileCreator();

    private static final ContainmentProfileBuilder BUILDER = new ContainmentProfileBuilder();

    private static final Resources REZ = ResourceManager.getPackageResources(DefaultModelFactory.class);

    private static final SecurityModel NULL_SECURITY = new DefaultSecurityModel();

    private static final String DEFAULT_PROFILE_NAME = "default";

    private final SystemContext m_system;

    private final Logger m_logger;

    /**
    * A map of security models keyed by profile name.
    */
    private final Map m_security = new Hashtable();

    /**
    * The root containment model against which refresh actions are 
    * resolved.
    */
    private ContainmentModel m_root;

    /**
    * A table of permissions instances keyed by code source.
    */
    private Map m_permissions = new Hashtable();

    /**
    * A table of protection domain instances keyed by code source.
    */
    private Map m_grants;

    /**
    * Creation of a new model factory.
    *
    * @param system the system context
    * @param profiles the set of initial security profiles
    * @param grants the set of initial address to permission 
    *    profile assignments 
    */
    DefaultModelFactory(final SystemContext system, SecurityProfile[] profiles, Map grants) {
        if (system == null) {
            throw new NullPointerException("system");
        }
        if (profiles == null) {
            throw new NullPointerException("profiles");
        }
        if (grants == null) {
            throw new NullPointerException("grants");
        }
        m_system = system;
        m_grants = grants;
        m_logger = system.getLogger();
        if (system.isCodeSecurityEnabled()) {
            for (int i = 0; i < profiles.length; i++) {
                SecurityProfile profile = profiles[i];
                final String name = profile.getName();
                if (m_logger.isDebugEnabled()) {
                    m_logger.debug("adding security profile [" + name + "].");
                }
                try {
                    SecurityModel model = new DefaultSecurityModel(profile);
                    m_security.put(profile.getName(), model);
                } catch (Throwable e) {
                    final String error = "Cannot include initial security model [" + name + "].";
                    throw new ModelRuntimeException(error, e);
                }
            }
            if (null == m_security.get(DEFAULT_PROFILE_NAME)) {
                final String error = "Security enabled without a \"" + DEFAULT_PROFILE_NAME + "\" profile.";
                throw new IllegalStateException(error);
            }
            Set entries = grants.entrySet();
            Iterator iterator = entries.iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                final String profile = (String) entry.getValue();
                getNamedSecurityModel(profile);
            }
        }
    }

    /**
    * Creation of a new root containment model using 
    * a URL referring to a containment profile.
    *
    * @param url a composition profile source
    * @return the containment model
    */
    public ContainmentModel createRootContainmentModel(URL url) throws ModelException {
        if (url.toString().endsWith(".xml")) {
            try {
                DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
                Configuration config = builder.build(url.toString());
                final ContainmentProfile profile = CREATOR.createContainmentProfile(config);
                return createRootContainmentModel(profile);
            } catch (ModelException e) {
                throw e;
            } catch (Throwable e) {
                final String error = "Could not create model due to a build related error.";
                throw new ModelException(error, e);
            }
        }
        try {
            final URLConnection connection = url.openConnection();
            final InputStream stream = connection.getInputStream();
            final ContainmentProfile profile = BUILDER.createContainmentProfile(stream);
            return createRootContainmentModel(profile);
        } catch (Throwable e) {
            final String error = REZ.getString("factory.containment.create-url.error", url.toString());
            throw new ModelException(error, e);
        }
    }

    /**
    * Creation of a new root containment model using 
    * a supplied profile.
    *
    * @param profile a containment profile 
    * @return the containment model
    */
    public ContainmentModel createRootContainmentModel(ContainmentProfile profile) throws ModelException {
        try {
            ContainmentContext context = createRootContainmentContext(profile);
            m_root = createContainmentModel(context);
            return m_root;
        } catch (Throwable e) {
            final String error = REZ.getString("factory.containment.create.error", profile.getName());
            throw new ModelException(error, e);
        }
    }

    /**
    * Creation of a new nested deployment model.  This method is called
    * by a container implementation when constructing model instances.  The 
    * factory is identified by its implementation classname.
    *
    * @param context a potentially foreign deployment context
    * @return the deployment model
    */
    public ComponentModel createComponentModel(ComponentContext context) throws ModelException {
        final String path = context.getQualifiedName();
        SecurityModel security = getAssignedSecurityModel(path);
        return new DefaultComponentModel(context, security);
    }

    /**
    * Creation of a new nested containment model.  This method is called
    * by a container implementation when constructing model instances.
    *
    * @param context a potentially foreign containment context
    * @return the containment model
    */
    public ContainmentModel createContainmentModel(ContainmentContext context) throws ModelException {
        final String path = context.getQualifiedName();
        SecurityModel security = getAssignedSecurityModel(path);
        return new DefaultContainmentModel(context, security);
    }

    /**
    * Creation of a new root containment context.
    *
    * @param profile a containment profile 
    * @return the containment context
    */
    private ContainmentContext createRootContainmentContext(ContainmentProfile profile) throws ModelException {
        if (profile == null) {
            throw new NullPointerException("profile");
        }
        m_system.getLoggingManager().addCategories(profile.getCategories());
        final Logger logger = m_system.getLoggingManager().getLoggerForCategory("");
        try {
            ClassLoader root = m_system.getAPIClassLoader();
            ClassLoaderDirective classLoaderDirective = profile.getClassLoaderDirective();
            ClassLoaderContext classLoaderContext = new DefaultClassLoaderContext(logger, m_system, root, classLoaderDirective);
            ClassLoaderModel classLoaderModel = new DefaultClassLoaderModel(classLoaderContext);
            return new DefaultContainmentContext(logger, m_system, classLoaderModel, null, null, profile);
        } catch (Throwable e) {
            final String error = REZ.getString("factory.containment.create.error", profile.getName());
            throw new ModelException(error, e);
        }
    }

    private Logger getLogger() {
        return m_logger;
    }

    /**
    * Return the security profile matching the supplied deployment
    * path.  The implementation will return the default security profile
    * unless an explicit grant has been declared for the the supplied
    * path, and the grant referes to a known security profile.
    * 
    * @param path the container or component path
    * @return the assigned security model
    */
    private SecurityModel getAssignedSecurityModel(final String path) {
        if (m_system.isCodeSecurityEnabled()) {
            final String profile = getAssignedProfileName(path);
            return getNamedSecurityModel(profile);
        } else {
            return NULL_SECURITY;
        }
    }

    /**
    * Return a named security profile. 
    * @param name an existing security profile name
    * @return the security model
    * @exception IllegalArgumentException if the name is unknown
    */
    private SecurityModel getNamedSecurityModel(String name) {
        SecurityModel model = (SecurityModel) m_security.get(name);
        if (null != model) {
            return model;
        } else {
            final String error = "Unknown security profile [" + name + "].";
            throw new IllegalArgumentException(error);
        }
    }

    /**
    * Return the security profile name assigned to the supplied path.
    * If no assignment has been declared the default security profile
    * name will be returned.
    *
    * @return the security profile name for the path
    */
    private String getAssignedProfileName(String path) {
        final String profile = (String) m_grants.get(path);
        if (null != profile) return profile;
        return DEFAULT_PROFILE_NAME;
    }

    /**
    * Return the default security model.
    * 
    * @return the default security model
    */
    private SecurityModel getDefaultSecurityModel() {
        return (SecurityModel) m_security.get(DEFAULT_PROFILE_NAME);
    }
}
