package org.jomc.ant;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.jomc.ant.types.KeyValueType;
import org.jomc.ant.types.ModuleResourceType;
import org.jomc.ant.types.ResourceType;
import org.jomc.model.Module;
import org.jomc.model.Modules;
import org.jomc.model.modlet.DefaultModelProcessor;
import org.jomc.model.modlet.DefaultModelProvider;
import org.jomc.model.modlet.ModelHelper;
import org.jomc.modlet.Model;
import org.jomc.modlet.ModelContext;
import org.jomc.modlet.ModelException;
import org.jomc.tools.modlet.ToolsModelProcessor;
import org.jomc.tools.modlet.ToolsModelProvider;

/**
 * Base class for executing model based tasks.
 *
 * @author <a href="mailto:schulte2005@users.sourceforge.net">Christian Schulte</a>
 * @version $JOMC$
 */
public class JomcModelTask extends JomcTask {

    /** Controls model object class path resolution. */
    private boolean modelObjectClasspathResolutionEnabled = true;

    /** The location to search for modules. */
    private String moduleLocation;

    /** The location to search for transformers. */
    private String transformerLocation;

    /** Module resources. */
    private Set<ModuleResourceType> moduleResources;

    /** The flag indicating JAXP schema validation of model resources is enabled. */
    private boolean modelResourceValidationEnabled = true;

    /** Creates a new {@code JomcModelTask} instance. */
    public JomcModelTask() {
        super();
    }

    /**
     * Gets the location searched for modules.
     *
     * @return The location searched for modules or {@code null}.
     *
     * @see #setModuleLocation(java.lang.String)
     */
    public final String getModuleLocation() {
        return this.moduleLocation;
    }

    /**
     * Sets the location to search for modules.
     *
     * @param value The new location to search for modules or {@code null}.
     *
     * @see #getModuleLocation()
     */
    public final void setModuleLocation(final String value) {
        this.moduleLocation = value;
    }

    /**
     * Gets the location searched for transformers.
     *
     * @return The location searched for transformers or {@code null}.
     *
     * @see #setTransformerLocation(java.lang.String)
     */
    public final String getTransformerLocation() {
        return this.transformerLocation;
    }

    /**
     * Sets the location to search for transformers.
     *
     * @param value The new location to search for transformers or {@code null}.
     *
     * @see #getTransformerLocation()
     */
    public final void setTransformerLocation(final String value) {
        this.transformerLocation = value;
    }

    /**
     * Gets a flag indicating model object class path resolution is enabled.
     *
     * @return {@code true}, if model object class path resolution is enabled; {@code false}, else.
     *
     * @see #setModelObjectClasspathResolutionEnabled(boolean)
     */
    public final boolean isModelObjectClasspathResolutionEnabled() {
        return this.modelObjectClasspathResolutionEnabled;
    }

    /**
     * Sets the flag indicating model object class path resolution is enabled.
     *
     * @param value {@code true}, to enable model object class path resolution; {@code false}, to disable model object
     * class path resolution.
     *
     * @see #isModelObjectClasspathResolutionEnabled()
     */
    public final void setModelObjectClasspathResolutionEnabled(final boolean value) {
        this.modelObjectClasspathResolutionEnabled = value;
    }

    /**
     * Gets a set of module resources.
     * <p>This accessor method returns a reference to the live set, not a snapshot. Therefore any modification you make
     * to the returned set will be present inside the object. This is why there is no {@code set} method for the
     * module resources property.</p>
     *
     * @return A set of module resources.
     *
     * @see #createModuleResource()
     */
    public Set<ModuleResourceType> getModuleResources() {
        if (this.moduleResources == null) {
            this.moduleResources = new HashSet<ModuleResourceType>();
        }
        return this.moduleResources;
    }

    /**
     * Creates a new {@code moduleResource} element instance.
     *
     * @return A new {@code moduleResource} element instance.
     *
     * @see #getModuleResources()
     */
    public ModuleResourceType createModuleResource() {
        final ModuleResourceType moduleResource = new ModuleResourceType();
        this.getModuleResources().add(moduleResource);
        return moduleResource;
    }

    /**
     * Gets a flag indicating JAXP schema validation of model resources is enabled.
     *
     * @return {@code true}, if JAXP schema validation of model resources is enabled; {@code false}, else.
     *
     * @see #setModelResourceValidationEnabled(boolean)
     */
    public final boolean isModelResourceValidationEnabled() {
        return this.modelResourceValidationEnabled;
    }

    /**
     * Sets the flag indicating JAXP schema validation of model resources is enabled.
     *
     * @param value {@code true}, to enable JAXP schema validation of model resources; {@code false}, to disable JAXP
     * schema validation of model resources.
     *
     * @see #isModelResourceValidationEnabled()
     */
    public final void setModelResourceValidationEnabled(final boolean value) {
        this.modelResourceValidationEnabled = value;
    }

    /**
     * Gets a {@code Model} from a given {@code ModelContext}.
     *
     * @param context The context to get a {@code Model} from.
     *
     * @return The {@code Model} from {@code context}.
     *
     * @throws NullPointerException if {@code contexŧ} is {@code null}.
     * @throws BuildException if no model is found.
     * @throws ModelException if getting the model fails.
     *
     * @see #getModel()
     * @see #isModelObjectClasspathResolutionEnabled()
     * @see #isModelProcessingEnabled()
     */
    @Override
    public Model getModel(final ModelContext context) throws BuildException, ModelException {
        if (context == null) {
            throw new NullPointerException("context");
        }
        Model model = new Model();
        model.setIdentifier(this.getModel());
        Modules modules = new Modules();
        ModelHelper.setModules(model, modules);
        Unmarshaller unmarshaller = null;
        for (ResourceType resource : this.getModuleResources()) {
            final URL[] urls = this.getResources(context, resource.getLocation());
            if (urls.length == 0) {
                if (resource.isOptional()) {
                    this.logMessage(Level.WARNING, Messages.getMessage("moduleResourceNotFound", resource.getLocation()));
                } else {
                    throw new BuildException(Messages.getMessage("moduleResourceNotFound", resource.getLocation()), this.getLocation());
                }
            }
            for (int i = urls.length - 1; i >= 0; i--) {
                InputStream in = null;
                boolean suppressExceptionOnClose = true;
                try {
                    this.logMessage(Level.FINEST, Messages.getMessage("reading", urls[i].toExternalForm()));
                    final URLConnection con = urls[i].openConnection();
                    con.setConnectTimeout(resource.getConnectTimeout());
                    con.setReadTimeout(resource.getReadTimeout());
                    con.connect();
                    in = con.getInputStream();
                    final Source source = new StreamSource(in, urls[i].toURI().toASCIIString());
                    if (unmarshaller == null) {
                        unmarshaller = context.createUnmarshaller(this.getModel());
                        if (this.isModelResourceValidationEnabled()) {
                            unmarshaller.setSchema(context.createSchema(this.getModel()));
                        }
                    }
                    Object o = unmarshaller.unmarshal(source);
                    if (o instanceof JAXBElement<?>) {
                        o = ((JAXBElement<?>) o).getValue();
                    }
                    if (o instanceof Module) {
                        modules.getModule().add((Module) o);
                    } else {
                        this.log(Messages.getMessage("unsupportedModuleResource", urls[i].toExternalForm()), Project.MSG_WARN);
                    }
                    suppressExceptionOnClose = false;
                } catch (final SocketTimeoutException e) {
                    String message = Messages.getMessage(e);
                    message = Messages.getMessage("resourceTimeout", message != null ? " " + message : "");
                    if (resource.isOptional()) {
                        this.getProject().log(message, e, Project.MSG_WARN);
                    } else {
                        throw new BuildException(message, e, this.getLocation());
                    }
                } catch (final IOException e) {
                    String message = Messages.getMessage(e);
                    message = Messages.getMessage("resourceFailure", message != null ? " " + message : "");
                    if (resource.isOptional()) {
                        this.getProject().log(message, e, Project.MSG_WARN);
                    } else {
                        throw new BuildException(message, e, this.getLocation());
                    }
                } catch (final URISyntaxException e) {
                    throw new BuildException(Messages.getMessage(e), e, this.getLocation());
                } catch (final JAXBException e) {
                    String message = Messages.getMessage(e);
                    if (message == null) {
                        message = Messages.getMessage(e.getLinkedException());
                    }
                    throw new BuildException(message, e, this.getLocation());
                } finally {
                    try {
                        if (in != null) {
                            in.close();
                        }
                    } catch (final IOException e) {
                        if (suppressExceptionOnClose) {
                            this.logMessage(Level.SEVERE, Messages.getMessage(e), e);
                        } else {
                            throw new BuildException(Messages.getMessage(e), e, this.getLocation());
                        }
                    }
                }
            }
        }
        model = context.findModel(model);
        modules = ModelHelper.getModules(model);
        if (modules != null && this.isModelObjectClasspathResolutionEnabled()) {
            final Module classpathModule = modules.getClasspathModule(Modules.getDefaultClasspathModuleName(), context.getClassLoader());
            if (classpathModule != null && modules.getModule(Modules.getDefaultClasspathModuleName()) == null) {
                modules.getModule().add(classpathModule);
            }
        }
        if (this.isModelProcessingEnabled()) {
            model = context.processModel(model);
        }
        return model;
    }

    /** {@inheritDoc} */
    @Override
    public void preExecuteTask() throws BuildException {
        super.preExecuteTask();
        this.assertLocationsNotNull(this.getModuleResources());
    }

    /** {@inheritDoc} */
    @Override
    public ModelContext newModelContext(final ClassLoader classLoader) throws ModelException {
        final ModelContext modelContext = super.newModelContext(classLoader);
        if (this.getTransformerLocation() != null) {
            modelContext.setAttribute(DefaultModelProcessor.TRANSFORMER_LOCATION_ATTRIBUTE_NAME, this.getTransformerLocation());
        }
        if (this.getModuleLocation() != null) {
            modelContext.setAttribute(DefaultModelProvider.MODULE_LOCATION_ATTRIBUTE_NAME, this.getModuleLocation());
        }
        modelContext.setAttribute(ToolsModelProvider.MODEL_OBJECT_CLASSPATH_RESOLUTION_ENABLED_ATTRIBUTE_NAME, this.isModelObjectClasspathResolutionEnabled());
        modelContext.setAttribute(ToolsModelProcessor.MODEL_OBJECT_CLASSPATH_RESOLUTION_ENABLED_ATTRIBUTE_NAME, this.isModelObjectClasspathResolutionEnabled());
        modelContext.setAttribute(DefaultModelProvider.VALIDATING_ATTRIBUTE_NAME, this.isModelResourceValidationEnabled());
        for (int i = 0, s0 = this.getModelContextAttributes().size(); i < s0; i++) {
            final KeyValueType kv = this.getModelContextAttributes().get(i);
            final Object object = kv.getObject(this.getLocation());
            if (object != null) {
                modelContext.setAttribute(kv.getKey(), object);
            } else {
                modelContext.clearAttribute(kv.getKey());
            }
        }
        return modelContext;
    }

    /** {@inheritDoc} */
    @Override
    public JomcModelTask clone() {
        final JomcModelTask clone = (JomcModelTask) super.clone();
        if (this.moduleResources != null) {
            clone.moduleResources = new HashSet<ModuleResourceType>(this.moduleResources.size());
            for (ModuleResourceType e : this.moduleResources) {
                clone.moduleResources.add(e.clone());
            }
        }
        return clone;
    }
}
