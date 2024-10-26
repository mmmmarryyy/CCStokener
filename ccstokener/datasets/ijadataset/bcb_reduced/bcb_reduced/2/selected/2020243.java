package org.dwgsoftware.raistlin.composition.model.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.avalon.framework.logger.Logger;
import org.dwgsoftware.raistlin.composition.data.BlockCompositionDirective;
import org.dwgsoftware.raistlin.composition.data.BlockIncludeDirective;
import org.dwgsoftware.raistlin.composition.data.ComponentProfile;
import org.dwgsoftware.raistlin.composition.data.ContainmentProfile;
import org.dwgsoftware.raistlin.composition.data.DeploymentProfile;
import org.dwgsoftware.raistlin.composition.data.NamedComponentProfile;
import org.dwgsoftware.raistlin.composition.data.TargetDirective;
import org.dwgsoftware.raistlin.composition.data.builder.ContainmentProfileBuilder;
import org.dwgsoftware.raistlin.composition.data.builder.XMLContainmentProfileCreator;
import org.dwgsoftware.raistlin.composition.data.builder.XMLTargetsCreator;
import org.dwgsoftware.raistlin.composition.event.CompositionEvent;
import org.dwgsoftware.raistlin.composition.event.CompositionListener;
import org.dwgsoftware.raistlin.composition.model.AssemblyException;
import org.dwgsoftware.raistlin.composition.model.ClassLoaderModel;
import org.dwgsoftware.raistlin.composition.model.ComponentModel;
import org.dwgsoftware.raistlin.composition.model.ContainmentModel;
import org.dwgsoftware.raistlin.composition.model.DependencyGraph;
import org.dwgsoftware.raistlin.composition.model.DeploymentModel;
import org.dwgsoftware.raistlin.composition.model.ModelException;
import org.dwgsoftware.raistlin.composition.model.ModelRepository;
import org.dwgsoftware.raistlin.composition.model.ServiceModel;
import org.dwgsoftware.raistlin.composition.model.TypeRepository;
import org.dwgsoftware.raistlin.composition.provider.ComponentContext;
import org.dwgsoftware.raistlin.composition.provider.ContainmentContext;
import org.dwgsoftware.raistlin.composition.provider.ModelFactory;
import org.dwgsoftware.raistlin.composition.provider.SecurityModel;
import org.dwgsoftware.raistlin.composition.provider.SystemContext;
import org.dwgsoftware.raistlin.composition.util.DefaultState;
import org.dwgsoftware.raistlin.logging.data.CategoriesDirective;
import org.dwgsoftware.raistlin.logging.provider.LoggingManager;
import org.dwgsoftware.raistlin.meta.info.DependencyDescriptor;
import org.dwgsoftware.raistlin.meta.info.ReferenceDescriptor;
import org.dwgsoftware.raistlin.meta.info.ServiceDescriptor;
import org.dwgsoftware.raistlin.meta.info.StageDescriptor;
import org.dwgsoftware.raistlin.meta.info.Type;
import org.dwgsoftware.raistlin.repository.Artifact;
import org.dwgsoftware.raistlin.repository.Repository;
import org.dwgsoftware.raistlin.repository.RepositoryException;
import org.dwgsoftware.raistlin.util.i18n.ResourceManager;
import org.dwgsoftware.raistlin.util.i18n.Resources;

/**
 * Containment model implmentation within which composite models are aggregated
 * as a part of a containment deployment model.
 *
 * @author <a href="mailto:dev@avalon.apache.org">Avalon Development Team</a>
 * @version $Revision: 1.1 $ $Date: 2005/09/06 00:58:18 $
 */
public class DefaultContainmentModel extends DefaultDeploymentModel implements ContainmentModel {

    private static final Resources REZ = ResourceManager.getPackageResources(DefaultContainmentModel.class);

    private static final ContainmentProfileBuilder BUILDER = new ContainmentProfileBuilder();

    private static final XMLContainmentProfileCreator CREATOR = new XMLContainmentProfileCreator();

    private static final XMLTargetsCreator TARGETS = new XMLTargetsCreator();

    private static String getPath(ContainmentContext context) {
        if (context.getPartitionName() == null) {
            return SEPARATOR;
        } else {
            return context.getPartitionName();
        }
    }

    private final LinkedList m_compositionListeners = new LinkedList();

    private final DefaultState m_assembly = new DefaultState();

    private final ContainmentContext m_context;

    private final String m_partition;

    private final ServiceModel[] m_services;

    private final DefaultState m_commissioned = new DefaultState();

    private CategoriesDirective m_categories;

    /**
    * Creation of a new containment model.
    *
    * @param context the containment context that establishes
    *   the structural association of this containment model
    *   within a parent scope
    */
    public DefaultContainmentModel(final ContainmentContext context, final SecurityModel security) throws ModelException {
        super(context, security);
        m_context = context;
        if (null == context.getPartitionName()) {
            m_partition = DeploymentModel.SEPARATOR;
        } else {
            m_partition = context.getPartitionName() + context.getName() + DeploymentModel.SEPARATOR;
        }
        DeploymentProfile[] profiles = context.getContainmentProfile().getProfiles();
        for (int i = 0; i < profiles.length; i++) {
            addModel(profiles[i]);
        }
        DefaultContainmentModelExportHelper helper = new DefaultContainmentModelExportHelper(m_context, this);
        m_services = helper.createServiceExport();
    }

    /**
    * Commission the appliance. 
    *
    * @exception Exception if a commissioning error occurs
    */
    public void commission() throws Exception {
        if (!isAssembled()) assemble();
        synchronized (m_commissioned) {
            if (m_commissioned.isEnabled()) return;
            DeploymentModel[] startup = getStartupGraph();
            Commissioner commissioner = new Commissioner(getLogger(), true);
            try {
                for (int i = 0; i < startup.length; i++) {
                    final DeploymentModel child = startup[i];
                    commissioner.commission(child);
                }
            } finally {
                commissioner.dispose();
            }
            super.commission();
            m_commissioned.setEnabled(true);
        }
    }

    /**
    * Decommission the appliance.  Once an appliance is 
    * decommissioned it may be re-commissioned.
    */
    public void decommission() {
        synchronized (m_commissioned) {
            if (!m_commissioned.isEnabled()) return;
            if (getLogger().isDebugEnabled()) {
                String message = "decommissioning";
                getLogger().debug(message);
            }
            super.decommission();
            DeploymentModel[] shutdown = getShutdownGraph();
            long timeout = getDeploymentTimeout();
            Commissioner commissioner = new Commissioner(getLogger(), false);
            try {
                for (int i = 0; i < shutdown.length; i++) {
                    final DeploymentModel child = shutdown[i];
                    child.decommission();
                }
            } finally {
                commissioner.dispose();
            }
            m_commissioned.setEnabled(false);
        }
    }

    /**
    * Return the classloader model.
    *
    * @return the classloader model
    */
    public ClassLoaderModel getClassLoaderModel() {
        return m_context.getClassLoaderModel();
    }

    /** 
    * Returns the maximum allowable time for deployment.
    *
    * @return the maximum time expressed in millisecond of how 
    * long a deployment may take.
    */
    public long getDeploymentTimeout() {
        return 0;
    }

    /**
    * Return the set of services produced by the model.
    * @return the services
    */
    public ServiceDescriptor[] getServices() {
        return m_context.getContainmentProfile().getExportDirectives();
    }

    /**
    * Return TRUE is this model is capable of supporting a supplied 
    * depedendency.
    * @return true if this model can fulfill the dependency
    */
    public boolean isaCandidate(DependencyDescriptor dependency) {
        return isaCandidate(dependency.getReference());
    }

    /**
    * Return TRUE is this model is capable of supporting a supplied 
    * service.
    *
    * @param reference the service reference descriptor
    * @return true if this model can fulfill the service
    */
    public boolean isaCandidate(ReferenceDescriptor reference) {
        ServiceDescriptor[] services = getServices();
        for (int i = 0; i < services.length; i++) {
            ServiceDescriptor service = services[i];
            if (service.getReference().matches(reference)) {
                return true;
            }
        }
        return false;
    }

    /**
    * Return TRUE is this model is capable of supporting a supplied 
    * stage dependency. The containment model implementation will 
    * allways return FALSE.
    *
    * @return FALSE containers don't export stage handling
    */
    public boolean isaCandidate(StageDescriptor stage) {
        return false;
    }

    /**
     * Returns the assembled state of the model.
     * @return true if this model is assembled
     */
    public boolean isAssembled() {
        return m_assembly.isEnabled();
    }

    /**
     * Assemble the model.  Model assembly is a process of 
     * wiring together candidate service providers with consumers.
     * The assembly implementation will assemble each deployment
     * model contained within this model.
     *
     * @exception Exception if assembly cannot be fulfilled
     */
    public void assemble() throws AssemblyException {
        List list = new ArrayList();
        assemble(list);
    }

    /**
    * Assemble the model.
    * @param subjects the list of deployment targets making up the assembly chain
    * @exception Exception if an error occurs during model assembly
    */
    public void assemble(List subjects) throws AssemblyException {
        synchronized (m_assembly) {
            if (isAssembled()) {
                return;
            }
            getLogger().debug("assembly phase");
            DefaultContainmentModelAssemblyHelper helper = new DefaultContainmentModelAssemblyHelper(m_context, this);
            DeploymentModel[] models = m_context.getModelRepository().getModels();
            for (int i = 0; i < models.length; i++) {
                DeploymentModel model = models[i];
                helper.assembleModel(model, subjects);
            }
            m_assembly.setEnabled(true);
        }
    }

    /**
     * Disassemble the model.
     */
    public void disassemble() {
        synchronized (m_assembly) {
            if (!isAssembled()) {
                return;
            }
            getLogger().debug("dissassembly phase");
            DeploymentModel[] models = m_context.getModelRepository().getModels();
            for (int i = 0; i < models.length; i++) {
                DeploymentModel model = models[i];
                if (model instanceof ContainmentModel) {
                    ContainmentModel containment = (ContainmentModel) model;
                    containment.disassemble();
                } else {
                    ComponentModel component = (ComponentModel) model;
                    dissasemble(component);
                }
            }
            m_assembly.setEnabled(false);
        }
    }

    private void dissasemble(ComponentModel model) {
    }

    /**
     * Return the set of models assigned as providers.
     * @return the providers consumed by the model
     * @exception IllegalStateException if the model is not in an assembled state 
     */
    public DeploymentModel[] getProviders() {
        if (!isAssembled()) {
            final String error = "Model is not assembled " + this;
            throw new IllegalStateException(error);
        }
        ArrayList list = new ArrayList();
        DeploymentModel[] models = m_context.getModelRepository().getModels();
        for (int i = 0; i < models.length; i++) {
            DeploymentModel model = models[i];
            DeploymentModel[] providers = model.getProviders();
            for (int j = 0; j < providers.length; j++) {
                DeploymentModel provider = providers[j];
                final String path = provider.getPath();
                final String root = getPartition();
                if (!path.startsWith(root)) {
                    list.add(providers[j]);
                }
            }
        }
        return (DeploymentModel[]) list.toArray(new DeploymentModel[0]);
    }

    /**
    * Add a composition listener to the model.
    * @param listener the composition listener
    */
    public void addCompositionListener(CompositionListener listener) {
        synchronized (m_compositionListeners) {
            m_compositionListeners.add(listener);
        }
    }

    /**
    * Remove a composition listener from the model.
    * @param listener the composition listener
    */
    public void removeCompositionListener(CompositionListener listener) {
        synchronized (m_compositionListeners) {
            m_compositionListeners.remove(listener);
        }
    }

    /**
    * Return the set of service export mappings
    * @return the set of export directives published by the model
    */
    public ServiceModel[] getServiceModels() {
        return m_services;
    }

    /**
    * Return the set of service export directives for a supplied class.
    * @param clazz a cleaa identifying the directive
    * @return the export directives
    */
    public ServiceModel getServiceModel(Class clazz) {
        ServiceModel[] models = getServiceModels();
        for (int i = 0; i < models.length; i++) {
            ServiceModel model = models[i];
            if (clazz.isAssignableFrom(model.getServiceClass())) {
                return model;
            }
        }
        return null;
    }

    /**
     * Get the startup sequence for the model.
     */
    public DeploymentModel[] getStartupGraph() {
        return m_context.getDependencyGraph().getStartupGraph();
    }

    /**
     * Get the shutdown sequence for the model.
     */
    public DeploymentModel[] getShutdownGraph() {
        return m_context.getDependencyGraph().getShutdownGraph();
    }

    /**
    * Add a model referenced by a url to this model.
    * @param url the url of the model to include
    * @return the model 
    * @exception ModelException if a model related error occurs
    */
    public ContainmentModel addContainmentModel(URL url) throws ModelException {
        return addContainmentModel(url, null);
    }

    public ContainmentModel addContainmentModel(URL block, URL config) throws ModelException {
        ContainmentModel model = createContainmentModel(null, block);
        addModel(model.getName(), model);
        applyTargets(config);
        return model;
    }

    /**
    * Addition of a new subsidiary model within
    * the containment context.
    *
    * @param model a containment or component model 
    * @return the supplied model
    */
    public DeploymentModel addModel(DeploymentModel model) {
        final String name = model.getName();
        return addModel(name, model);
    }

    /**
    * Addition of a new subsidiary model within
    * the containment context using a supplied profile.
    *
    * @param profile a containment or deployment profile 
    * @return the model based on the supplied profile
    * @exception ModelException if an error occurs during model establishment
    */
    public DeploymentModel addModel(DeploymentProfile profile) throws ModelException {
        final String name = profile.getName();
        DeploymentModel model = createDeploymentModel(name, profile);
        addModel(name, model);
        return model;
    }

    /**
    * Addition of a new subsidiary model within
    * the containment context using a supplied profile.
    *
    * @param profile a containment or deployment profile 
    * @return the model based on the supplied profile
    * @exception ModelException if an error occurs during model establishment
    */
    DeploymentModel createDeploymentModel(DeploymentProfile profile) throws ModelException {
        final String name = profile.getName();
        return createDeploymentModel(name, profile);
    }

    /**
    * Addition of a new subsidiary model within
    * the containment context using a supplied profile.
    *
    * @param profile a containment or deployment profile 
    * @return the model based on the supplied profile
    * @exception ModelException if an error occurs during model establishment
    */
    DeploymentModel createDeploymentModel(String name, DeploymentProfile profile) throws ModelException {
        if (null == profile) throw new NullPointerException("profile");
        DeploymentModel model = null;
        if (profile instanceof ContainmentProfile) {
            ContainmentProfile containment = (ContainmentProfile) profile;
            model = createContainmentModel(containment);
        } else if (profile instanceof ComponentProfile) {
            ComponentProfile deployment = (ComponentProfile) profile;
            model = createComponentModel(deployment);
        } else if (profile instanceof NamedComponentProfile) {
            ComponentProfile deployment = createComponentProfile((NamedComponentProfile) profile);
            model = createComponentModel(deployment);
        } else if (profile instanceof BlockIncludeDirective) {
            BlockIncludeDirective directive = (BlockIncludeDirective) profile;
            model = createContainmentModel(directive);
        } else if (profile instanceof BlockCompositionDirective) {
            BlockCompositionDirective directive = (BlockCompositionDirective) profile;
            model = createContainmentModel(directive);
        } else {
            final String error = REZ.getString("containment.unknown-profile-class.error", getPath(), profile.getClass().getName());
            throw new ModelException(error);
        }
        return model;
    }

    /**
    * Removal of a named model for the containment model.
    *
    * @param name the name of the subsidiary model to be removed
    * @exception IllegalArgumentException if the supplied name is unknown
    */
    public void removeModel(String name) throws IllegalArgumentException {
        ModelRepository repository = m_context.getModelRepository();
        synchronized (repository) {
            DeploymentModel model = (DeploymentModel) repository.getModel(name);
            if (null == model) {
                final String error = "No model named [" + name + "] is referenced with the model [" + this + "].";
                throw new IllegalArgumentException(error);
            } else {
                m_context.getDependencyGraph().remove(model);
                repository.removeModel(model);
                CompositionEvent event = new CompositionEvent(this, model);
                fireModelRemovedEvent(event);
            }
        }
    }

    /**
    * Return the partition name established by this containment context.
    * @return the partition name
    */
    public String getPartition() {
        return m_partition;
    }

    /**
    * Return the set of immediate child models nested 
    * within this model.
    *
    * @return the nested model
    */
    public DeploymentModel[] getModels() {
        return m_context.getModelRepository().getModels();
    }

    /**
    * Return a child model relative to a supplied name.
    *
    * @param path a relative or absolute path
    * @return the named model or null if the name is unknown
    * @exception IllegalArgumentException if the name if badly formed
    */
    public DeploymentModel getModel(String path) {
        DefaultContainmentModelNavigationHelper helper = new DefaultContainmentModelNavigationHelper(m_context, this);
        return helper.getModel(path);
    }

    /**
    * Resolve a model capable of supporting the supplied service reference.
    *
    * @param descriptor a service reference descriptor
    * @return the model or null if unresolvable
    */
    public DeploymentModel getModel(ReferenceDescriptor descriptor) throws AssemblyException {
        DefaultContainmentModelAssemblyHelper helper = new DefaultContainmentModelAssemblyHelper(m_context, this);
        return helper.findServiceProvider(descriptor);
    }

    /**
    * Resolve a model capable of supporting the supplied service reference.
    *
    * @param dependency a dependency descriptor
    * @return the model or null if unresolvable
    * @exception AssemblyException if an assembly error occurs
    */
    public DeploymentModel getModel(DependencyDescriptor dependency) throws AssemblyException {
        DefaultContainmentModelAssemblyHelper helper = new DefaultContainmentModelAssemblyHelper(m_context, this);
        return helper.findDependencyProvider(dependency);
    }

    /**
    * Apply a set of override targets resolvable from a supplied url.
    * @param config a url resolvable to a TargetDirective[]
    * @exception ModelException if an error occurs
    */
    public void applyTargets(URL config) throws ModelException {
        if (config != null) {
            TargetDirective[] targets = getTargets(config);
            applyTargets(targets);
        }
    }

    /**
    * Apply a set of override targets.
    * @param targets a set of target directives
    */
    public void applyTargets(TargetDirective[] targets) {
        for (int i = 0; i < targets.length; i++) {
            TargetDirective target = targets[i];
            final String path = target.getPath();
            DeploymentModel model = getModel(path);
            if (model != null) {
                getLogger().debug("customizing target " + model);
                if (target.getCategoriesDirective() != null) {
                    model.setCategories(target.getCategoriesDirective());
                }
                if (model instanceof ComponentModel) {
                    ComponentModel deployment = (ComponentModel) model;
                    if (target.getConfiguration() != null) {
                        deployment.setConfiguration(target.getConfiguration());
                    }
                }
            } else {
                final String warning = REZ.getString("target.ignore", path, toString());
                getLogger().warn(warning);
            }
        }
    }

    private DeploymentModel addModel(String name, DeploymentModel model) {
        if (model.equals(this)) return model;
        ModelRepository repository = m_context.getModelRepository();
        synchronized (repository) {
            repository.addModel(name, model);
            m_context.getDependencyGraph().add(model);
            CompositionEvent event = new CompositionEvent(this, model);
            fireModelAddedEvent(event);
            return model;
        }
    }

    private void fireModelAddedEvent(CompositionEvent event) {
        Iterator iterator = m_compositionListeners.iterator();
        while (iterator.hasNext()) {
            final CompositionListener listener = (CompositionListener) iterator.next();
            try {
                listener.modelAdded(event);
            } catch (Throwable e) {
                final String error = "A composition listener raised an exception";
                getLogger().warn(error, e);
            }
        }
    }

    private void fireModelRemovedEvent(CompositionEvent event) {
        Iterator iterator = m_compositionListeners.iterator();
        while (iterator.hasNext()) {
            final CompositionListener listener = (CompositionListener) iterator.next();
            try {
                listener.modelRemoved(event);
            } catch (Throwable e) {
                final String error = "A composition listener raised an exception";
                getLogger().warn(error, e);
            }
        }
    }

    /**
    * Creation of a new instance of a deployment model within
    * this containment context.
    *
    * @param profile a containment profile 
    * @return the composition model
    */
    private ComponentModel createComponentModel(final ComponentProfile profile) throws ModelException {
        DefaultContainmentModelComponentHelper helper = new DefaultContainmentModelComponentHelper(m_context, this);
        ComponentContext context = helper.createComponentContext(profile);
        ModelFactory factory = m_context.getSystemContext().getModelFactory();
        return factory.createComponentModel(context);
    }

    /**
    * Creation of a new instance of a containment model within
    * this containment context.
    *
    * @param profile a containment profile 
    * @return the composition model
    */
    private ContainmentModel createContainmentModel(final ContainmentProfile profile) throws ModelException {
        final String name = profile.getName();
        return createContainmentModel(name, profile);
    }

    /**
    * Creation of a new instance of a containment model within
    * this containment context.
    *
    * @param name the containment name
    * @param profile a containment profile 
    * @return the composition model
    */
    private ContainmentModel createContainmentModel(final String name, final ContainmentProfile profile) throws ModelException {
        return createContainmentModel(name, profile, new URL[0]);
    }

    /**
    * Creation of a new instance of a containment model within
    * this containment context.
    *
    * @param name the containment name
    * @param profile a containment profile 
    * @param implicit any implicit urls to include in the container classloader
    * @return the composition model
    */
    private ContainmentModel createContainmentModel(final String name, final ContainmentProfile profile, URL[] implicit) throws ModelException {
        final String partition = getPartition();
        if (getLogger().isDebugEnabled()) {
            SystemContext system = m_context.getSystemContext();
            final String message = REZ.getString("containment.add", system.toString(name));
            getLogger().debug(message);
        }
        LoggingManager logging = m_context.getSystemContext().getLoggingManager();
        final String base = partition + name;
        logging.addCategories(base, profile.getCategories());
        Logger log = logging.getLoggerForCategory(base);
        try {
            final ClassLoaderModel classLoaderModel = m_context.getClassLoaderModel().createClassLoaderModel(log, profile, implicit);
            final File home = new File(m_context.getHomeDirectory(), name);
            final File temp = new File(m_context.getTempDirectory(), name);
            final Logger logger = getLogger().getChildLogger(name);
            ModelRepository modelRepository = m_context.getModelRepository();
            DependencyGraph graph = m_context.getDependencyGraph();
            DefaultContainmentContext context = new DefaultContainmentContext(logger, m_context.getSystemContext(), classLoaderModel, modelRepository, graph, home, temp, this, profile, partition, name);
            ModelFactory factory = m_context.getSystemContext().getModelFactory();
            return factory.createContainmentModel(context);
        } catch (ModelException e) {
            throw e;
        } catch (Throwable e) {
            final String error = REZ.getString("containment.container.create.error", getPath(), profile.getName());
            throw new ModelException(error, e);
        }
    }

    /**
    * Add a containment profile that is derived from an external resource.
    * @param directive the block composition directive
    * @return the containment model established by the include
    */
    private ContainmentModel createContainmentModel(BlockCompositionDirective directive) throws ModelException {
        final String name = directive.getName();
        ContainmentModel model = null;
        try {
            Repository repository = m_context.getSystemContext().getRepository();
            Artifact artifact = directive.getArtifact();
            final URL url = repository.getResource(artifact);
            model = createContainmentModel(name, url);
        } catch (RepositoryException e) {
            final String error = "Unable to include block [" + name + "] into the containmment model [" + getQualifiedName() + "] because of a repository related error.";
            throw new ModelException(error, e);
        }
        TargetDirective[] targets = directive.getTargetDirectives();
        model.applyTargets(targets);
        return model;
    }

    /**
    * Create a containment model that is derived from an external 
    * source profile defintion.
    *
    * @param directive the block include directive
    * @return the containment model established by the include
    */
    private ContainmentModel createContainmentModel(BlockIncludeDirective directive) throws ModelException {
        final String name = directive.getName();
        final String path = directive.getPath();
        try {
            if (path.indexOf(":") < 0) {
                URL anchor = m_context.getSystemContext().getBaseDirectory().toURL();
                URL url = new URL(anchor, path);
                return createContainmentModel(name, url);
            } else {
                URL url = new URL(path);
                return createContainmentModel(name, url);
            }
        } catch (MalformedURLException e) {
            final String error = "Unable to include block [" + name + "] into the containmment model [" + getQualifiedName() + "] because of a url related error.";
            throw new ModelException(error, e);
        }
    }

    /**
    * Create a containment model that is derived from an external 
    * source containment profile defintion.
    *
    * @param directive the block include directive
    * @return the containment model established by the include
    */
    private ContainmentModel createContainmentModel(String name, URL url) throws ModelException {
        if (url.getProtocol().equals("artifact") || url.getProtocol().equals("block")) {
            try {
                Artifact artifact = (Artifact) url.getContent();
                URL target = m_context.getSystemContext().getRepository().getResource(artifact);
                return createContainmentModel(name, target);
            } catch (Throwable e) {
                final String error = "Unresolvable artifact reference [" + url + "].";
                throw new ModelException(error, e);
            }
        }
        final String path = url.toString();
        try {
            if (path.endsWith(".jar")) {
                final URL jarURL = convertToJarURL(url);
                final URL blockURL = new URL(jarURL, "/BLOCK-INF/block.xml");
                final InputStream stream = blockURL.openStream();
                try {
                    final ContainmentProfile profile = BUILDER.createContainmentProfile(stream);
                    final String message = "including composite block: " + blockURL.toString();
                    getLogger().debug(message);
                    return createContainmentModel(getName(name, profile), profile, new URL[] { url });
                } catch (Throwable e) {
                    final String error = "Unable to create block from embedded descriptor [" + blockURL.toString() + "] in the containmment model [" + getQualifiedName() + "] due to a build related error.";
                    throw new ModelException(error, e);
                }
            } else if (path.endsWith(".xml") || path.endsWith(".block")) {
                DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
                Configuration config = builder.build(path);
                final ContainmentProfile profile = CREATOR.createContainmentProfile(config);
                final String message = "including composite block: " + path;
                getLogger().debug(message);
                return createContainmentModel(getName(name, profile), profile);
            } else if (path.endsWith("/")) {
                verifyPath(path);
                final URL blockURL = new URL(url.toString() + "BLOCK-INF/block.xml");
                DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
                Configuration config = builder.build(blockURL.toString());
                final ContainmentProfile profile = CREATOR.createContainmentProfile(config);
                final String message = "including composite block: " + blockURL.toString();
                getLogger().debug(message);
                return createContainmentModel(getName(name, profile), profile, new URL[] { url });
            } else if (path.endsWith(".bar")) {
                final String error = "Cannot execute a block archive: " + path;
                throw new ModelException(error);
            } else {
                verifyPath(path);
                return createContainmentModel(name, new URL(path + "/"));
            }
        } catch (ModelException e) {
            throw e;
        } catch (MalformedURLException e) {
            final String error = "Unable to include block [" + path + "] into the containmment model [" + getQualifiedName() + "] because of a url related error.";
            throw new ModelException(error, e);
        } catch (IOException e) {
            final String error = "Unable to include block [" + path + "] into the containmment model [" + getQualifiedName() + "] because of a io related error.";
            throw new ModelException(error, e);
        } catch (Throwable e) {
            final String error = "Unable to include block [" + path + "] into the containmment model [" + getQualifiedName() + "] because of an unexpected error.";
            throw new ModelException(error, e);
        }
    }

    /**
    * Verify the a path is valid.  The implementation will 
    * throw an exception if a connection to a url established 
    * using the path agument cann be resolved.
    *
    * @exception ModelException if the path is not resolvable 
    *    to a url connection
    */
    private void verifyPath(String path) throws ModelException {
        try {
            URL url = new URL(path);
            URLConnection connection = url.openConnection();
            connection.connect();
        } catch (java.io.FileNotFoundException e) {
            final String error = "File not found: " + path;
            throw new ModelException(error);
        } catch (Throwable e) {
            final String error = "Invalid path: " + path;
            throw new ModelException(error, e);
        }
    }

    private String getName(String name, DeploymentProfile profile) {
        if (name != null) return name;
        return profile.getName();
    }

    /**
    * Conver a classic url to a jar url.  If the supplied url protocol is not 
    * the "jar" protocol, a ne url is created by prepending jar: and adding the 
    * trailing "!/".
    *
    * @param url the url to convert
    * @return the converted url
    * @exception MalformedURLException if something goes wrong
    */
    private URL convertToJarURL(URL url) throws MalformedURLException {
        if (url.getProtocol().equals("jar")) return url;
        return new URL("jar:" + url.toString() + "!/");
    }

    /**
    * Create a full deployment profile using a supplied named 
    * profile reference.
    *
    * @param profile the named profile reference directive
    * @return the deployment profile
    * @exception ModelException if an error occurs during 
    *    profile creation
    */
    private ComponentProfile createComponentProfile(NamedComponentProfile profile) throws ModelException {
        try {
            NamedComponentProfile holder = (NamedComponentProfile) profile;
            final String classname = holder.getClassname();
            final String key = holder.getKey();
            TypeRepository repository = m_context.getClassLoaderModel().getTypeRepository();
            Type type = repository.getType(classname);
            ComponentProfile template = repository.getProfile(type, key);
            return new ComponentProfile(profile.getName(), template);
        } catch (Throwable e) {
            final String error = REZ.getString("containment.model.create.deployment.error", profile.getKey(), getPath(), profile.getClassname());
            throw new ModelException(error, e);
        }
    }

    private TargetDirective[] getTargets(final URL url) throws ModelException {
        try {
            DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
            Configuration config = builder.build(url.toString());
            return TARGETS.createTargets(config).getTargets();
        } catch (Throwable e) {
            final String error = "Could not load the targets directive: " + url;
            throw new ModelException(error, e);
        }
    }
}
