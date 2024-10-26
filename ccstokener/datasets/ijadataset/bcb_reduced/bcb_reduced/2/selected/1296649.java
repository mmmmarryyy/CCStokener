package org.jomc.ant;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.jomc.ant.types.KeyValueType;
import org.jomc.ant.types.NameType;
import org.jomc.ant.types.PropertiesFormatType;
import org.jomc.ant.types.PropertiesResourceType;
import org.jomc.ant.types.ResourceType;
import org.jomc.ant.types.TransformerResourceType;
import org.jomc.model.ModelObject;
import org.jomc.modlet.DefaultModelContext;
import org.jomc.modlet.DefaultModletProvider;
import org.jomc.modlet.Model;
import org.jomc.modlet.ModelContext;
import org.jomc.modlet.ModelContextFactory;
import org.jomc.modlet.ModelException;
import org.jomc.modlet.ModelValidationReport;
import org.jomc.modlet.ModletProvider;

/**
 * Base class for executing tasks.
 *
 * @author <a href="mailto:schulte2005@users.sourceforge.net">Christian Schulte</a>
 * @version $JOMC$
 * @see #execute()
 */
public class JomcTask extends Task {

    /** The class path to process. */
    private Path classpath;

    /** The identifier of the model to process. */
    private String model;

    /** {@code ModelContext} attributes to apply. */
    private List<KeyValueType> modelContextAttributes;

    /** The name of the {@code ModelContextFactory} implementation class backing the task. */
    private String modelContextFactoryClassName;

    /** Controls processing of models. */
    private boolean modelProcessingEnabled = true;

    /** The location to search for modlets. */
    private String modletLocation;

    /** The {@code http://jomc.org/modlet} namespace schema system id of the context backing the task. */
    private String modletSchemaSystemId;

    /** The location to search for providers. */
    private String providerLocation;

    /** The location to search for platform providers. */
    private String platformProviderLocation;

    /** The global transformation parameters to apply. */
    private List<KeyValueType> transformationParameters;

    /** The global transformation parameter resources to apply. */
    private List<PropertiesResourceType> transformationParameterResources;

    /** The global transformation output properties to apply. */
    private List<KeyValueType> transformationOutputProperties;

    /** The flag indicating JAXP schema validation of modlet resources is enabled. */
    private boolean modletResourceValidationEnabled = true;

    /** Property controlling the execution of the task. */
    private Object _if;

    /** Property controlling the execution of the task. */
    private Object unless;

    /** Creates a new {@code JomcTask} instance. */
    public JomcTask() {
        super();
    }

    /**
     * Gets an object controlling the execution of the task.
     *
     * @return An object controlling the execution of the task or {@code null}.
     *
     * @see #setIf(java.lang.Object)
     */
    public final Object getIf() {
        return this._if;
    }

    /**
     * Sets an object controlling the execution of the task.
     *
     * @param value The new object controlling the execution of the task or {@code null}.
     *
     * @see #getIf()
     */
    public final void setIf(final Object value) {
        this._if = value;
    }

    /**
     * Gets an object controlling the execution of the task.
     *
     * @return An object controlling the execution of the task or {@code null}.
     *
     * @see #setUnless(java.lang.Object)
     */
    public final Object getUnless() {
        if (this.unless == null) {
            this.unless = Boolean.TRUE;
        }
        return this.unless;
    }

    /**
     * Sets an object controlling the execution of the task.
     *
     * @param value The new object controlling the execution of the task or {@code null}.
     *
     * @see #getUnless()
     */
    public final void setUnless(final Object value) {
        this.unless = value;
    }

    /**
     * Creates a new {@code classpath} element instance.
     *
     * @return A new {@code classpath} element instance.
     */
    public final Path createClasspath() {
        return this.getClasspath().createPath();
    }

    /**
     * Gets the class path to process.
     *
     * @return The class path to process.
     *
     * @see #setClasspath(org.apache.tools.ant.types.Path)
     */
    public final Path getClasspath() {
        if (this.classpath == null) {
            this.classpath = new Path(this.getProject());
        }
        return this.classpath;
    }

    /**
     * Adds to the class path to process.
     *
     * @param value The path to add to the list of class path elements.
     *
     * @see #getClasspath()
     */
    public final void setClasspath(final Path value) {
        this.getClasspath().add(value);
    }

    /**
     * Adds a reference to a class path defined elsewhere.
     *
     * @param value A reference to a class path.
     *
     * @see #getClasspath()
     */
    public final void setClasspathRef(final Reference value) {
        this.getClasspath().setRefid(value);
    }

    /**
     * Gets the identifier of the model to process.
     *
     * @return The identifier of the model to process.
     *
     * @see #setModel(java.lang.String)
     */
    public final String getModel() {
        if (this.model == null) {
            this.model = ModelObject.MODEL_PUBLIC_ID;
        }
        return this.model;
    }

    /**
     * Sets the identifier of the model to process.
     *
     * @param value The new identifier of the model to process or {@code null}.
     *
     * @see #getModel()
     */
    public final void setModel(final String value) {
        this.model = value;
    }

    /**
     * Gets the {@code ModelContext} attributes to apply.
     * <p>This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make
     * to the returned list will be present inside the object. This is why there is no {@code set} method for the
     * model context attributes property.</p>
     *
     * @return The  {@code ModelContext} attributes to apply.
     *
     * @see #createModelContextAttribute()
     * @see #newModelContext(java.lang.ClassLoader)
     */
    public final List<KeyValueType> getModelContextAttributes() {
        if (this.modelContextAttributes == null) {
            this.modelContextAttributes = new LinkedList<KeyValueType>();
        }
        return this.modelContextAttributes;
    }

    /**
     * Creates a new {@code modelContextAttribute} element instance.
     *
     * @return A new {@code modelContextAttribute} element instance.
     *
     * @see #getModelContextAttributes()
     */
    public KeyValueType createModelContextAttribute() {
        final KeyValueType modelContextAttribute = new KeyValueType();
        this.getModelContextAttributes().add(modelContextAttribute);
        return modelContextAttribute;
    }

    /**
     * Gets the name of the {@code ModelContextFactory} implementation class backing the task.
     *
     * @return The name of the {@code ModelContextFactory} implementation class backing the task or {@code null}.
     *
     * @see #setModelContextFactoryClassName(java.lang.String)
     */
    public final String getModelContextFactoryClassName() {
        return this.modelContextFactoryClassName;
    }

    /**
     * Sets the name of the {@code ModelContextFactory} implementation class backing the task.
     *
     * @param value The new name of the {@code ModelContextFactory} implementation class backing the task or
     * {@code null}.
     *
     * @see #getModelContextFactoryClassName()
     */
    public final void setModelContextFactoryClassName(final String value) {
        this.modelContextFactoryClassName = value;
    }

    /**
     * Gets a flag indicating the processing of models is enabled.
     *
     * @return {@code true}, if processing of models is enabled; {@code false}, else.
     *
     * @see #setModelProcessingEnabled(boolean)
     */
    public final boolean isModelProcessingEnabled() {
        return this.modelProcessingEnabled;
    }

    /**
     * Sets the flag indicating the processing of models is enabled.
     *
     * @param value {@code true}, to enable processing of models; {@code false}, to disable processing of models.
     *
     * @see #isModelProcessingEnabled()
     */
    public final void setModelProcessingEnabled(final boolean value) {
        this.modelProcessingEnabled = value;
    }

    /**
     * Gets the location searched for modlets.
     *
     * @return The location searched for modlets or {@code null}.
     *
     * @see #setModletLocation(java.lang.String)
     */
    public final String getModletLocation() {
        return this.modletLocation;
    }

    /**
     * Sets the location to search for modlets.
     *
     * @param value The new location to search for modlets or {@code null}.
     *
     * @see #getModletLocation()
     */
    public final void setModletLocation(final String value) {
        this.modletLocation = value;
    }

    /**
     * Gets the {@code http://jomc.org/modlet} namespace schema system id of the context backing the task.
     *
     * @return The {@code http://jomc.org/modlet} namespace schema system id of the context backing the task or
     * {@code null}.
     *
     * @see #setModletSchemaSystemId(java.lang.String)
     */
    public final String getModletSchemaSystemId() {
        return this.modletSchemaSystemId;
    }

    /**
     * Sets the {@code http://jomc.org/modlet} namespace schema system id of the context backing the task.
     *
     * @param value The new {@code http://jomc.org/modlet} namespace schema system id of the context backing the task or
     * {@code null}.
     *
     * @see #getModletSchemaSystemId()
     */
    public final void setModletSchemaSystemId(final String value) {
        this.modletSchemaSystemId = value;
    }

    /**
     * Gets the location searched for providers.
     *
     * @return The location searched for providers or {@code null}.
     *
     * @see #setProviderLocation(java.lang.String)
     */
    public final String getProviderLocation() {
        return this.providerLocation;
    }

    /**
     * Sets the location to search for providers.
     *
     * @param value The new location to search for providers or {@code null}.
     *
     * @see #getProviderLocation()
     */
    public final void setProviderLocation(final String value) {
        this.providerLocation = value;
    }

    /**
     * Gets the location searched for platform provider resources.
     *
     * @return The location searched for platform provider resources or {@code null}.
     *
     * @see #setPlatformProviderLocation(java.lang.String)
     */
    public final String getPlatformProviderLocation() {
        return this.platformProviderLocation;
    }

    /**
     * Sets the location to search for platform provider resources.
     *
     * @param value The new location to search for platform provider resources or {@code null}.
     *
     * @see #getPlatformProviderLocation()
     */
    public final void setPlatformProviderLocation(final String value) {
        this.platformProviderLocation = value;
    }

    /**
     * Gets the global transformation parameters to apply.
     * <p>This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make
     * to the returned list will be present inside the object. This is why there is no {@code set} method for the
     * transformation parameters property.</p>
     *
     * @return The global transformation parameters to apply.
     *
     * @see #createTransformationParameter()
     * @see #getTransformer(org.jomc.ant.types.TransformerResourceType)
     */
    public final List<KeyValueType> getTransformationParameters() {
        if (this.transformationParameters == null) {
            this.transformationParameters = new LinkedList<KeyValueType>();
        }
        return this.transformationParameters;
    }

    /**
     * Creates a new {@code transformationParameter} element instance.
     *
     * @return A new {@code transformationParameter} element instance.
     *
     * @see #getTransformationParameters()
     */
    public KeyValueType createTransformationParameter() {
        final KeyValueType transformationParameter = new KeyValueType();
        this.getTransformationParameters().add(transformationParameter);
        return transformationParameter;
    }

    /**
     * Gets the global transformation parameter resources to apply.
     * <p>This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make
     * to the returned list will be present inside the object. This is why there is no {@code set} method for the
     * transformation parameter resources property.</p>
     *
     * @return The global transformation parameter resources to apply.
     *
     * @see #createTransformationParameterResource()
     * @see #getTransformer(org.jomc.ant.types.TransformerResourceType)
     */
    public final List<PropertiesResourceType> getTransformationParameterResources() {
        if (this.transformationParameterResources == null) {
            this.transformationParameterResources = new LinkedList<PropertiesResourceType>();
        }
        return this.transformationParameterResources;
    }

    /**
     * Creates a new {@code transformationParameterResource} element instance.
     *
     * @return A new {@code transformationParameterResource} element instance.
     *
     * @see #getTransformationParameterResources()
     */
    public PropertiesResourceType createTransformationParameterResource() {
        final PropertiesResourceType transformationParameterResource = new PropertiesResourceType();
        this.getTransformationParameterResources().add(transformationParameterResource);
        return transformationParameterResource;
    }

    /**
     * Gets the global transformation output properties to apply.
     * <p>This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make
     * to the returned list will be present inside the object. This is why there is no {@code set} method for the
     * transformation output properties property.</p>
     *
     * @return The global transformation output properties to apply.
     *
     * @see #createTransformationOutputProperty()
     */
    public final List<KeyValueType> getTransformationOutputProperties() {
        if (this.transformationOutputProperties == null) {
            this.transformationOutputProperties = new LinkedList<KeyValueType>();
        }
        return this.transformationOutputProperties;
    }

    /**
     * Creates a new {@code transformationOutputProperty} element instance.
     *
     * @return A new {@code transformationOutputProperty} element instance.
     *
     * @see #getTransformationOutputProperties()
     */
    public KeyValueType createTransformationOutputProperty() {
        final KeyValueType transformationOutputProperty = new KeyValueType();
        this.getTransformationOutputProperties().add(transformationOutputProperty);
        return transformationOutputProperty;
    }

    /**
     * Gets a flag indicating JAXP schema validation of modlet resources is enabled.
     *
     * @return {@code true}, if JAXP schema validation of modlet resources is enabled; {@code false}, else.
     *
     * @see #setModletResourceValidationEnabled(boolean)
     */
    public final boolean isModletResourceValidationEnabled() {
        return this.modletResourceValidationEnabled;
    }

    /**
     * Sets the flag indicating JAXP schema validation of modlet resources is enabled.
     *
     * @param value {@code true}, to enable JAXP schema validation of modlet resources; {@code false}, to disable JAXP
     * schema validation of modlet resources.
     *
     * @see #isModletResourceValidationEnabled()
     */
    public final void setModletResourceValidationEnabled(final boolean value) {
        this.modletResourceValidationEnabled = value;
    }

    /**
     * Called by the project to let the task do its work.
     *
     * @throws BuildException if execution fails.
     *
     * @see #getIf()
     * @see #getUnless()
     * @see #preExecuteTask()
     * @see #executeTask()
     * @see #postExecuteTask()
     */
    @Override
    public final void execute() throws BuildException {
        final PropertyHelper propertyHelper = PropertyHelper.getPropertyHelper(this.getProject());
        if (propertyHelper.testIfCondition(this.getIf()) && !propertyHelper.testUnlessCondition(this.getUnless())) {
            try {
                this.preExecuteTask();
                this.executeTask();
            } finally {
                this.postExecuteTask();
            }
        }
    }

    /**
     * Called by the {@code execute} method prior to the {@code executeTask} method.
     *
     * @throws BuildException if execution fails.
     *
     * @see #execute()
     */
    public void preExecuteTask() throws BuildException {
        this.logSeparator();
        this.log(Messages.getMessage("title"));
        this.logSeparator();
        this.assertNotNull("model", this.getModel());
        this.assertKeysNotNull(this.getModelContextAttributes());
        this.assertKeysNotNull(this.getTransformationParameters());
        this.assertKeysNotNull(this.getTransformationOutputProperties());
        this.assertLocationsNotNull(this.getTransformationParameterResources());
    }

    /**
     * Called by the {@code execute} method prior to the {@code postExecuteTask} method.
     *
     * @throws BuildException if execution fails.
     *
     * @see #execute()
     */
    public void executeTask() throws BuildException {
        this.getProject().log(Messages.getMessage("unimplementedTask", this.getClass().getName(), "executeTask"), Project.MSG_WARN);
    }

    /**
     * Called by the {@code execute} method after the {@code preExecuteTask}/{@code executeTask} methods even if those
     * methods threw an exception.
     *
     * @throws BuildException if execution fails.
     *
     * @see #execute()
     */
    public void postExecuteTask() throws BuildException {
        this.logSeparator();
    }

    /**
     * Gets a {@code Model} from a given {@code ModelContext}.
     *
     * @param context The context to get a {@code Model} from.
     *
     * @return The {@code Model} from {@code context}.
     *
     * @throws NullPointerException if {@code contexŧ} is {@code null}.
     * @throws ModelException if getting the model fails.
     *
     * @see #getModel()
     * @see #isModelProcessingEnabled()
     */
    public Model getModel(final ModelContext context) throws ModelException {
        if (context == null) {
            throw new NullPointerException("context");
        }
        Model foundModel = context.findModel(this.getModel());
        if (foundModel != null && this.isModelProcessingEnabled()) {
            foundModel = context.processModel(foundModel);
        }
        return foundModel;
    }

    /**
     * Creates an {@code URL} for a given resource location.
     * <p>This method first searches the class path of the task for a single resource matching {@code location}. If
     * such a resource is found, the URL of that resource is returned. If no such resource is found, an attempt is made
     * to parse the given location to an URL. On successful parsing, that URL is returned. Failing that, the given
     * location is interpreted as a file name relative to the project's base directory. If that file is found, the URL
     * of that file is returned. Otherwise {@code null} is returned.</p>
     *
     * @param location The resource location to create an {@code URL} from.
     *
     * @return An {@code URL} for {@code location} or {@code null}, if parsing {@code location} to an URL fails and
     * {@code location} points to a non-existent resource.
     *
     * @throws NullPointerException if {@code location} is {@code null}.
     * @throws BuildException if creating an URL fails.
     */
    public URL getResource(final String location) throws BuildException {
        if (location == null) {
            throw new NullPointerException("location");
        }
        try {
            String absolute = location;
            if (!absolute.startsWith("/")) {
                absolute = "/" + absolute;
            }
            URL resource = this.getClass().getResource(absolute);
            if (resource == null) {
                try {
                    resource = new URL(location);
                } catch (final MalformedURLException e) {
                    this.log(e, Project.MSG_DEBUG);
                    resource = null;
                }
            }
            if (resource == null) {
                final File f = this.getProject().resolveFile(location);
                if (f.isFile()) {
                    resource = f.toURI().toURL();
                }
            }
            return resource;
        } catch (final MalformedURLException e) {
            String m = Messages.getMessage(e);
            m = m == null ? "" : " " + m;
            throw new BuildException(Messages.getMessage("malformedLocation", location, m), e, this.getLocation());
        }
    }

    /**
     * Creates an array of {@code URL}s for a given resource location.
     * <p>This method first searches the given context for resources matching {@code location}. If such resources are
     * found, an array of URLs of those resources is returned. If no such resources are found, an attempt is made
     * to parse the given location to an URL. On successful parsing, that URL is returned. Failing that, the given
     * location is interpreted as a file name relative to the project's base directory. If that file is found, the URL
     * of that file is returned. Otherwise an empty array is returned.</p>
     *
     * @param context The context to search for resources.
     * @param location The resource location to create an array of {@code URL}s from.
     *
     * @return An array of {@code URL}s for {@code location} or an empty array if parsing {@code location} to an URL
     * fails and {@code location} points to non-existent resources.
     *
     * @throws NullPointerException if {@code context} or {@code location} is {@code null}.
     * @throws BuildException if creating an URL array fails.
     */
    public URL[] getResources(final ModelContext context, final String location) throws BuildException {
        if (context == null) {
            throw new NullPointerException("context");
        }
        if (location == null) {
            throw new NullPointerException("location");
        }
        final Set<URI> uris = new HashSet<URI>();
        try {
            for (final Enumeration<URL> e = context.findResources(location); e.hasMoreElements(); ) {
                uris.add(e.nextElement().toURI());
            }
        } catch (final URISyntaxException e) {
            this.log(e, Project.MSG_DEBUG);
        } catch (final ModelException e) {
            this.log(e, Project.MSG_DEBUG);
        }
        if (uris.isEmpty()) {
            try {
                uris.add(new URL(location).toURI());
            } catch (final MalformedURLException e) {
                this.log(e, Project.MSG_DEBUG);
            } catch (final URISyntaxException e) {
                this.log(e, Project.MSG_DEBUG);
            }
        }
        if (uris.isEmpty()) {
            final File f = this.getProject().resolveFile(location);
            if (f.isFile()) {
                uris.add(f.toURI());
            }
        }
        int i = 0;
        final URL[] urls = new URL[uris.size()];
        for (URI uri : uris) {
            try {
                urls[i++] = uri.toURL();
            } catch (final MalformedURLException e) {
                String m = Messages.getMessage(e);
                m = m == null ? "" : " " + m;
                throw new BuildException(Messages.getMessage("malformedLocation", uri.toASCIIString(), m), e, this.getLocation());
            }
        }
        return urls;
    }

    /**
     * Creates an {@code URL} for a given directory location.
     * <p>This method first attempts to parse the given location to an URL. On successful parsing, that URL is returned.
     * Failing that, the given location is interpreted as a directory name relative to the project's base directory. If
     * that directory is found, the URL of that directory is returned. Otherwise {@code null} is returned.</p>
     *
     * @param location The directory location to create an {@code URL} from.
     *
     * @return An {@code URL} for {@code location} or {@code null}, if parsing {@code location} to an URL fails and
     * {@code location} points to a non-existent directory.
     *
     * @throws NullPointerException if {@code location} is {@code null}.
     * @throws BuildException if creating an URL fails.
     */
    public URL getDirectory(final String location) throws BuildException {
        if (location == null) {
            throw new NullPointerException("location");
        }
        try {
            URL resource = null;
            try {
                resource = new URL(location);
            } catch (final MalformedURLException e) {
                this.log(e, Project.MSG_DEBUG);
                resource = null;
            }
            if (resource == null) {
                final File f = this.getProject().resolveFile(location);
                if (f.isDirectory()) {
                    resource = f.toURI().toURL();
                }
            }
            return resource;
        } catch (final MalformedURLException e) {
            String m = Messages.getMessage(e);
            m = m == null ? "" : " " + m;
            throw new BuildException(Messages.getMessage("malformedLocation", location, m), e, this.getLocation());
        }
    }

    /**
     * Creates a new {@code Transformer} for a given {@code TransformerResourceType}.
     *
     * @param resource The resource to create a {@code Transformer} of.
     *
     * @return A new {@code Transformer} for {@code resource} or {@code null}, if {@code resource} is not found and
     * flagged optional.
     *
     * @throws TransformerConfigurationException if creating a new {@code Transformer} fails.
     *
     * @see #getTransformationParameterResources()
     * @see #getTransformationParameters()
     * @see #getResource(java.lang.String)
     */
    public Transformer getTransformer(final TransformerResourceType resource) throws TransformerConfigurationException {
        if (resource == null) {
            throw new NullPointerException("resource");
        }
        InputStream in = null;
        boolean suppressExceptionOnClose = true;
        final URL url = this.getResource(resource.getLocation());
        try {
            if (url != null) {
                final ErrorListener errorListener = new ErrorListener() {

                    public void warning(final TransformerException exception) throws TransformerException {
                        if (getProject() != null) {
                            getProject().log(Messages.getMessage(exception), exception, Project.MSG_WARN);
                        }
                    }

                    public void error(final TransformerException exception) throws TransformerException {
                        throw exception;
                    }

                    public void fatalError(final TransformerException exception) throws TransformerException {
                        throw exception;
                    }
                };
                final URLConnection con = url.openConnection();
                con.setConnectTimeout(resource.getConnectTimeout());
                con.setReadTimeout(resource.getReadTimeout());
                con.connect();
                in = con.getInputStream();
                final TransformerFactory f = TransformerFactory.newInstance();
                f.setErrorListener(errorListener);
                final Transformer transformer = f.newTransformer(new StreamSource(in, url.toURI().toASCIIString()));
                transformer.setErrorListener(errorListener);
                for (Map.Entry<Object, Object> e : System.getProperties().entrySet()) {
                    transformer.setParameter(e.getKey().toString(), e.getValue());
                }
                for (final Iterator<Map.Entry<?, ?>> it = this.getProject().getProperties().entrySet().iterator(); it.hasNext(); ) {
                    final Map.Entry<?, ?> e = it.next();
                    transformer.setParameter(e.getKey().toString(), e.getValue());
                }
                for (int i = 0, s0 = this.getTransformationParameterResources().size(); i < s0; i++) {
                    for (Map.Entry<Object, Object> e : this.getProperties(this.getTransformationParameterResources().get(i)).entrySet()) {
                        transformer.setParameter(e.getKey().toString(), e.getValue());
                    }
                }
                for (int i = 0, s0 = this.getTransformationParameters().size(); i < s0; i++) {
                    final KeyValueType p = this.getTransformationParameters().get(i);
                    transformer.setParameter(p.getKey(), p.getObject(this.getLocation()));
                }
                for (int i = 0, s0 = this.getTransformationOutputProperties().size(); i < s0; i++) {
                    final KeyValueType p = this.getTransformationOutputProperties().get(i);
                    transformer.setOutputProperty(p.getKey(), p.getValue());
                }
                for (int i = 0, s0 = resource.getTransformationParameterResources().size(); i < s0; i++) {
                    for (Map.Entry<Object, Object> e : this.getProperties(resource.getTransformationParameterResources().get(i)).entrySet()) {
                        transformer.setParameter(e.getKey().toString(), e.getValue());
                    }
                }
                for (int i = 0, s0 = resource.getTransformationParameters().size(); i < s0; i++) {
                    final KeyValueType p = resource.getTransformationParameters().get(i);
                    transformer.setParameter(p.getKey(), p.getObject(this.getLocation()));
                }
                for (int i = 0, s0 = resource.getTransformationOutputProperties().size(); i < s0; i++) {
                    final KeyValueType p = resource.getTransformationOutputProperties().get(i);
                    transformer.setOutputProperty(p.getKey(), p.getValue());
                }
                suppressExceptionOnClose = false;
                return transformer;
            } else if (resource.isOptional()) {
                this.log(Messages.getMessage("transformerNotFound", resource.getLocation()), Project.MSG_WARN);
            } else {
                throw new BuildException(Messages.getMessage("transformerNotFound", resource.getLocation()), this.getLocation());
            }
        } catch (final URISyntaxException e) {
            throw new BuildException(Messages.getMessage(e), e, this.getLocation());
        } catch (final SocketTimeoutException e) {
            final String message = Messages.getMessage(e);
            if (resource.isOptional()) {
                this.getProject().log(Messages.getMessage("resourceTimeout", message != null ? " " + message : ""), e, Project.MSG_WARN);
            } else {
                throw new BuildException(Messages.getMessage("resourceTimeout", message != null ? " " + message : ""), e, this.getLocation());
            }
        } catch (final IOException e) {
            final String message = Messages.getMessage(e);
            if (resource.isOptional()) {
                this.getProject().log(Messages.getMessage("resourceFailure", message != null ? " " + message : ""), e, Project.MSG_WARN);
            } else {
                throw new BuildException(Messages.getMessage("resourceFailure", message != null ? " " + message : ""), e, this.getLocation());
            }
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
        return null;
    }

    /**
     * Creates a new {@code Properties} instance from a {@code PropertiesResourceType}.
     *
     * @param propertiesResourceType The {@code PropertiesResourceType} specifying the properties to create.
     *
     * @return The properties for {@code propertiesResourceType}.
     *
     * @throws NullPointerException if {@code propertiesResourceType} is {@code null}.
     * @throws BuildException if loading properties fails.
     */
    public Properties getProperties(final PropertiesResourceType propertiesResourceType) throws BuildException {
        if (propertiesResourceType == null) {
            throw new NullPointerException("propertiesResourceType");
        }
        InputStream in = null;
        boolean suppressExceptionOnClose = true;
        final Properties properties = new Properties();
        final URL url = this.getResource(propertiesResourceType.getLocation());
        try {
            if (url != null) {
                final URLConnection con = url.openConnection();
                con.setConnectTimeout(propertiesResourceType.getConnectTimeout());
                con.setReadTimeout(propertiesResourceType.getReadTimeout());
                con.connect();
                in = con.getInputStream();
                if (propertiesResourceType.getFormat() == PropertiesFormatType.PLAIN) {
                    properties.load(in);
                } else if (propertiesResourceType.getFormat() == PropertiesFormatType.XML) {
                    properties.loadFromXML(in);
                }
            } else if (propertiesResourceType.isOptional()) {
                this.log(Messages.getMessage("propertiesNotFound", propertiesResourceType.getLocation()), Project.MSG_WARN);
            } else {
                throw new BuildException(Messages.getMessage("propertiesNotFound", propertiesResourceType.getLocation()), this.getLocation());
            }
            suppressExceptionOnClose = false;
        } catch (final SocketTimeoutException e) {
            final String message = Messages.getMessage(e);
            if (propertiesResourceType.isOptional()) {
                this.getProject().log(Messages.getMessage("resourceTimeout", message != null ? " " + message : ""), e, Project.MSG_WARN);
            } else {
                throw new BuildException(Messages.getMessage("resourceTimeout", message != null ? " " + message : ""), e, this.getLocation());
            }
        } catch (final IOException e) {
            final String message = Messages.getMessage(e);
            if (propertiesResourceType.isOptional()) {
                this.getProject().log(Messages.getMessage("resourceFailure", message != null ? " " + message : ""), e, Project.MSG_WARN);
            } else {
                throw new BuildException(Messages.getMessage("resourceFailure", message != null ? " " + message : ""), e, this.getLocation());
            }
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
        return properties;
    }

    /**
     * Creates a new {@code ProjectClassLoader} instance.
     *
     * @return A new {@code ProjectClassLoader} instance.
     *
     * @throws BuildException if creating a new class loader instance fails.
     */
    public ProjectClassLoader newProjectClassLoader() throws BuildException {
        try {
            final ProjectClassLoader classLoader = new ProjectClassLoader(this.getProject(), this.getClasspath());
            classLoader.getModletExcludes().addAll(ProjectClassLoader.getDefaultModletExcludes());
            classLoader.getProviderExcludes().addAll(ProjectClassLoader.getDefaultProviderExcludes());
            classLoader.getSchemaExcludes().addAll(ProjectClassLoader.getDefaultSchemaExcludes());
            classLoader.getServiceExcludes().addAll(ProjectClassLoader.getDefaultServiceExcludes());
            if (this.getModletLocation() != null) {
                classLoader.getModletResourceLocations().add(this.getModletLocation());
            } else {
                classLoader.getModletResourceLocations().add(DefaultModletProvider.getDefaultModletLocation());
            }
            if (this.getProviderLocation() != null) {
                classLoader.getProviderResourceLocations().add(this.getProviderLocation() + "/" + ModletProvider.class.getName());
            } else {
                classLoader.getProviderResourceLocations().add(DefaultModelContext.getDefaultProviderLocation() + "/" + ModletProvider.class.getName());
            }
            return classLoader;
        } catch (final IOException e) {
            throw new BuildException(Messages.getMessage(e), e, this.getLocation());
        }
    }

    /**
     * Creates a new {@code ModelContext} instance using a given class loader.
     *
     * @param classLoader The class loader to create a new {@code ModelContext} instance with.
     *
     * @return A new {@code ModelContext} instance backed by {@code classLoader}.
     *
     * @throws ModelException if creating a new {@code ModelContext} instance fails.
     */
    public ModelContext newModelContext(final ClassLoader classLoader) throws ModelException {
        final ModelContextFactory modelContextFactory;
        if (this.modelContextFactoryClassName != null) {
            modelContextFactory = ModelContextFactory.newInstance(this.getModelContextFactoryClassName());
        } else {
            modelContextFactory = ModelContextFactory.newInstance();
        }
        final ModelContext modelContext = modelContextFactory.newModelContext(classLoader);
        modelContext.setLogLevel(Level.ALL);
        modelContext.setModletSchemaSystemId(this.getModletSchemaSystemId());
        modelContext.getListeners().add(new ModelContext.Listener() {

            @Override
            public void onLog(final Level level, final String message, final Throwable t) {
                super.onLog(level, message, t);
                logMessage(level, message, t);
            }
        });
        if (this.getProviderLocation() != null) {
            modelContext.setAttribute(DefaultModelContext.PROVIDER_LOCATION_ATTRIBUTE_NAME, this.getProviderLocation());
        }
        if (this.getPlatformProviderLocation() != null) {
            modelContext.setAttribute(DefaultModelContext.PLATFORM_PROVIDER_LOCATION_ATTRIBUTE_NAME, this.getPlatformProviderLocation());
        }
        if (this.getModletLocation() != null) {
            modelContext.setAttribute(DefaultModletProvider.MODLET_LOCATION_ATTRIBUTE_NAME, this.getModletLocation());
        }
        modelContext.setAttribute(DefaultModletProvider.VALIDATING_ATTRIBUTE_NAME, this.isModletResourceValidationEnabled());
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

    /**
     * Throws a {@code BuildException} on a given {@code null} value.
     *
     * @param attributeName The name of a mandatory attribute.
     * @param value The value of that attribute.
     *
     * @throws NullPointerException if {@code attributeName} is {@code null}.
     * @throws BuildException if {@code value} is {@code null}.
     */
    public final void assertNotNull(final String attributeName, final Object value) throws BuildException {
        if (attributeName == null) {
            throw new NullPointerException("attributeName");
        }
        if (value == null) {
            throw new BuildException(Messages.getMessage("mandatoryAttribute", attributeName), this.getLocation());
        }
    }

    /**
     * Throws a {@code BuildException} on a {@code null} value of a {@code name} property of a given {@code NameType}
     * collection.
     *
     * @param names The collection holding the  {@code NameType} instances to test.
     *
     * @throws NullPointerException if {@code names} is {@code null}.
     * @throws BuildException if a {@code name} property of a given {@code NameType} from the {@code names} collection
     * holds a {@code null} value.
     */
    public final void assertNamesNotNull(final Collection<? extends NameType> names) throws BuildException {
        if (names == null) {
            throw new NullPointerException("names");
        }
        for (NameType n : names) {
            this.assertNotNull("name", n.getName());
        }
    }

    /**
     * Throws a {@code BuildException} on a {@code null} value of a {@code key} property of a given {@code KeyValueType}
     * collection.
     *
     * @param keys The collection holding the  {@code KeyValueType} instances to test.
     *
     * @throws NullPointerException if {@code keys} is {@code null}.
     * @throws BuildException if a {@code key} property of a given {@code KeyValueType} from the {@code keys} collection
     * holds a {@code null} value.
     */
    public final void assertKeysNotNull(final Collection<? extends KeyValueType> keys) throws BuildException {
        if (keys == null) {
            throw new NullPointerException("keys");
        }
        for (KeyValueType k : keys) {
            this.assertNotNull("key", k.getKey());
        }
    }

    /**
     * Throws a {@code BuildException} on a {@code null} value of a {@code location} property of a given
     * {@code ResourceType} collection.
     *
     * @param locations The collection holding the {@code ResourceType} instances to test.
     *
     * @throws NullPointerException if {@code locations} is {@code null}.
     * @throws BuildException if a {@code location} property of a given {@code ResourceType} from the {@code locations}
     * collection holds a {@code null} value.
     */
    public final void assertLocationsNotNull(final Collection<? extends ResourceType> locations) throws BuildException {
        if (locations == null) {
            throw new NullPointerException("locations");
        }
        for (ResourceType r : locations) {
            assertNotNull("location", r.getLocation());
            if (r instanceof TransformerResourceType) {
                assertKeysNotNull(((TransformerResourceType) r).getTransformationParameters());
                assertLocationsNotNull(((TransformerResourceType) r).getTransformationParameterResources());
                assertKeysNotNull(((TransformerResourceType) r).getTransformationOutputProperties());
            }
        }
    }

    /** Logs a separator string. */
    public final void logSeparator() {
        this.log(Messages.getMessage("separator"));
    }

    /**
     * Logs a message at a given level.
     *
     * @param level The level to log at.
     * @param message The message to log.
     *
     * @throws BuildException if logging fails.
     */
    public final void logMessage(final Level level, final String message) throws BuildException {
        BufferedReader reader = null;
        boolean suppressExceptionOnClose = true;
        try {
            String line = null;
            reader = new BufferedReader(new StringReader(message));
            while ((line = reader.readLine()) != null) {
                if (level.intValue() >= Level.SEVERE.intValue()) {
                    log(line, Project.MSG_ERR);
                } else if (level.intValue() >= Level.WARNING.intValue()) {
                    log(line, Project.MSG_WARN);
                } else if (level.intValue() >= Level.INFO.intValue()) {
                    log(line, Project.MSG_INFO);
                } else {
                    log(line, Project.MSG_DEBUG);
                }
            }
            suppressExceptionOnClose = false;
        } catch (final IOException e) {
            throw new BuildException(Messages.getMessage(e), e, this.getLocation());
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (final IOException e) {
                if (suppressExceptionOnClose) {
                    this.log(e, Project.MSG_ERR);
                } else {
                    throw new BuildException(Messages.getMessage(e), e, this.getLocation());
                }
            }
        }
    }

    /**
     * Logs a message at a given level.
     *
     * @param level The level to log at.
     * @param message The message to log.
     * @param throwable The throwable to log.
     *
     * @throws BuildException if logging fails.
     */
    public final void logMessage(final Level level, final String message, final Throwable throwable) throws BuildException {
        this.logMessage(level, message);
        if (level.intValue() >= Level.SEVERE.intValue()) {
            log(throwable, Project.MSG_ERR);
        } else if (level.intValue() >= Level.WARNING.intValue()) {
            log(throwable, Project.MSG_WARN);
        } else if (level.intValue() >= Level.INFO.intValue()) {
            log(throwable, Project.MSG_INFO);
        } else {
            log(throwable, Project.MSG_DEBUG);
        }
    }

    /**
     * Logs a validation report.
     *
     * @param context The context to use for logging the report.
     * @param report The report to log.
     *
     * @throws NullPointerException if {@code context} or {@code report} is {@code null}.
     * @throws BuildException if logging fails.
     */
    public final void logValidationReport(final ModelContext context, final ModelValidationReport report) {
        try {
            if (!report.getDetails().isEmpty()) {
                this.logSeparator();
                Marshaller marshaller = null;
                for (ModelValidationReport.Detail detail : report.getDetails()) {
                    this.logMessage(detail.getLevel(), "o " + detail.getMessage());
                    if (detail.getElement() != null) {
                        if (marshaller == null) {
                            marshaller = context.createMarshaller(this.getModel());
                            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                        }
                        final StringWriter stringWriter = new StringWriter();
                        marshaller.marshal(detail.getElement(), stringWriter);
                        this.logMessage(Level.FINEST, stringWriter.toString());
                    }
                }
            }
        } catch (final ModelException e) {
            throw new BuildException(Messages.getMessage(e), e, this.getLocation());
        } catch (final JAXBException e) {
            String message = Messages.getMessage(e);
            if (message == null && e.getLinkedException() != null) {
                message = Messages.getMessage(e.getLinkedException());
            }
            throw new BuildException(message, e, this.getLocation());
        }
    }

    /**
     * Creates and returns a copy of this object.
     *
     * @return A copy of this object.
     */
    @Override
    public JomcTask clone() {
        try {
            final JomcTask clone = (JomcTask) super.clone();
            clone.classpath = (Path) (this.classpath != null ? this.classpath.clone() : null);
            if (this.modelContextAttributes != null) {
                clone.modelContextAttributes = new ArrayList<KeyValueType>(this.modelContextAttributes.size());
                for (KeyValueType e : this.modelContextAttributes) {
                    clone.modelContextAttributes.add(e.clone());
                }
            }
            if (this.transformationParameters != null) {
                clone.transformationParameters = new ArrayList<KeyValueType>(this.transformationParameters.size());
                for (KeyValueType e : this.transformationParameters) {
                    clone.transformationParameters.add(e.clone());
                }
            }
            if (this.transformationParameterResources != null) {
                clone.transformationParameterResources = new ArrayList<PropertiesResourceType>(this.transformationParameterResources.size());
                for (PropertiesResourceType e : this.transformationParameterResources) {
                    clone.transformationParameterResources.add(e.clone());
                }
            }
            if (this.transformationOutputProperties != null) {
                clone.transformationOutputProperties = new ArrayList<KeyValueType>(this.transformationOutputProperties.size());
                for (KeyValueType e : this.transformationOutputProperties) {
                    clone.transformationOutputProperties.add(e.clone());
                }
            }
            return clone;
        } catch (final CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
