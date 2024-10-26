package org.jomc.ant;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBResult;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.jomc.ant.types.NameType;
import org.jomc.ant.types.ResourceType;
import org.jomc.ant.types.TransformerResourceType;
import org.jomc.model.Module;
import org.jomc.model.Modules;
import org.jomc.model.ObjectFactory;
import org.jomc.model.modlet.DefaultModelProvider;
import org.jomc.modlet.ModelContext;
import org.jomc.modlet.ModelException;
import org.jomc.modlet.ModelValidationReport;

/**
 * Task for merging module resources.
 *
 * @author <a href="mailto:schulte2005@users.sourceforge.net">Christian Schulte</a>
 * @version $JOMC$
 */
public final class MergeModulesTask extends JomcModelTask {

    /** The encoding of the module resource. */
    private String moduleEncoding;

    /** File to write the merged module to. */
    private File moduleFile;

    /** The name of the merged module. */
    private String moduleName;

    /** The version of the merged module. */
    private String moduleVersion;

    /** The vendor of the merged module. */
    private String moduleVendor;

    /** Included modules. */
    private Set<NameType> moduleIncludes;

    /** Excluded modules. */
    private Set<NameType> moduleExcludes;

    /** XSLT documents to use for transforming model objects. */
    private List<TransformerResourceType> modelObjectStylesheetResources;

    /** Creates a new {@code MergeModulesTask} instance. */
    public MergeModulesTask() {
        super();
    }

    /**
     * Gets the file to write the merged module to.
     *
     * @return The file to write the merged module to or {@code null}.
     *
     * @see #setModuleFile(java.io.File)
     */
    public File getModuleFile() {
        return this.moduleFile;
    }

    /**
     * Sets the file to write the merged module to.
     *
     * @param value The new file to write the merged module to or {@code null}.
     *
     * @see #getModuleFile()
     */
    public void setModuleFile(final File value) {
        this.moduleFile = value;
    }

    /**
     * Gets the encoding of the module resource.
     *
     * @return The encoding of the module resource.
     *
     * @see #setModuleEncoding(java.lang.String)
     */
    public String getModuleEncoding() {
        if (this.moduleEncoding == null) {
            this.moduleEncoding = new OutputStreamWriter(new ByteArrayOutputStream()).getEncoding();
        }
        return this.moduleEncoding;
    }

    /**
     * Sets the encoding of the module resource.
     *
     * @param value The new encoding of the module resource or {@code null}.
     *
     * @see #getModuleEncoding()
     */
    public void setModuleEncoding(final String value) {
        this.moduleEncoding = value;
    }

    /**
     * Gets the name of the merged module.
     *
     * @return The name of the merged module or {@code null}.
     *
     * @see #setModuleName(java.lang.String)
     */
    public String getModuleName() {
        return this.moduleName;
    }

    /**
     * Sets the name of the merged module.
     *
     * @param value The new name of the merged module or {@code null}.
     *
     * @see #getModuleName()
     */
    public void setModuleName(final String value) {
        this.moduleName = value;
    }

    /**
     * Gets the version of the merged module.
     *
     * @return The version of the merged module or {@code null}.
     *
     * @see #setModuleVersion(java.lang.String)
     */
    public String getModuleVersion() {
        return this.moduleVersion;
    }

    /**
     * Sets the version of the merged module.
     *
     * @param value The new version of the merged module or {@code null}.
     *
     * @see #getModuleVersion()
     */
    public void setModuleVersion(final String value) {
        this.moduleVersion = value;
    }

    /**
     * Gets the vendor of the merged module.
     *
     * @return The vendor of the merge module or {@code null}.
     *
     * @see #setModuleVendor(java.lang.String)
     */
    public String getModuleVendor() {
        return this.moduleVendor;
    }

    /**
     * Sets the vendor of the merged module.
     *
     * @param value The new vendor of the merged module or {@code null}.
     *
     * @see #getModuleVendor()
     */
    public void setModuleVendor(final String value) {
        this.moduleVendor = value;
    }

    /**
     * Gets a set of module names to include.
     * <p>This accessor method returns a reference to the live set, not a snapshot. Therefore any modification you make
     * to the returned set will be present inside the object. This is why there is no {@code set} method for the
     * module includes property.</p>
     *
     * @return A set of module names to include.
     *
     * @see #createModuleInclude()
     */
    public Set<NameType> getModuleIncludes() {
        if (this.moduleIncludes == null) {
            this.moduleIncludes = new HashSet<NameType>();
        }
        return this.moduleIncludes;
    }

    /**
     * Creates a new {@code moduleInclude} element instance.
     *
     * @return A new {@code moduleInclude} element instance.
     *
     * @see #getModuleIncludes()
     */
    public NameType createModuleInclude() {
        final NameType moduleInclude = new NameType();
        this.getModuleIncludes().add(moduleInclude);
        return moduleInclude;
    }

    /**
     * Gets a set of module names to exclude.
     * <p>This accessor method returns a reference to the live set, not a snapshot. Therefore any modification you make
     * to the returned set will be present inside the object. This is why there is no {@code set} method for the
     * module excludes property.</p>
     *
     * @return A set of module names to exclude.
     *
     * @see #createModuleExclude()
     */
    public Set<NameType> getModuleExcludes() {
        if (this.moduleExcludes == null) {
            this.moduleExcludes = new HashSet<NameType>();
        }
        return this.moduleExcludes;
    }

    /**
     * Creates a new {@code moduleExclude} element instance.
     *
     * @return A new {@code moduleExclude} element instance.
     *
     * @see #getModuleExcludes()
     */
    public NameType createModuleExclude() {
        final NameType moduleExclude = new NameType();
        this.getModuleExcludes().add(moduleExclude);
        return moduleExclude;
    }

    /**
     * Gets the XSLT documents to use for transforming model objects.
     * <p>This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make
     * to the returned list will be present inside the object. This is why there is no {@code set} method for the
     * model object stylesheet resources property.</p>
     *
     * @return The XSLT documents to use for transforming model objects.
     *
     * @see #createModelObjectStylesheetResource()
     */
    public List<TransformerResourceType> getModelObjectStylesheetResources() {
        if (this.modelObjectStylesheetResources == null) {
            this.modelObjectStylesheetResources = new LinkedList<TransformerResourceType>();
        }
        return this.modelObjectStylesheetResources;
    }

    /**
     * Creates a new {@code modelObjectStylesheetResource} element instance.
     *
     * @return A new {@code modelObjectStylesheetResource} element instance.
     *
     * @see #getModelObjectStylesheetResources()
     */
    public TransformerResourceType createModelObjectStylesheetResource() {
        final TransformerResourceType modelObjectStylesheetResource = new TransformerResourceType();
        this.getModelObjectStylesheetResources().add(modelObjectStylesheetResource);
        return modelObjectStylesheetResource;
    }

    /** {@inheritDoc} */
    @Override
    public void preExecuteTask() throws BuildException {
        super.preExecuteTask();
        this.assertNotNull("moduleFile", this.getModuleFile());
        this.assertNotNull("moduleName", this.getModuleName());
        this.assertNamesNotNull(this.getModuleExcludes());
        this.assertNamesNotNull(this.getModuleIncludes());
        this.assertLocationsNotNull(this.getModelObjectStylesheetResources());
    }

    /**
     * Merges module resources.
     *
     * @throws BuildException if merging module resources fails.
     */
    @Override
    public void executeTask() throws BuildException {
        ProjectClassLoader classLoader = null;
        boolean suppressExceptionOnClose = true;
        try {
            this.log(Messages.getMessage("mergingModules", this.getModel()));
            classLoader = this.newProjectClassLoader();
            final Modules modules = new Modules();
            final Set<ResourceType> resources = new HashSet<ResourceType>(this.getModuleResources());
            final ModelContext context = this.newModelContext(classLoader);
            final Marshaller marshaller = context.createMarshaller(this.getModel());
            final Unmarshaller unmarshaller = context.createUnmarshaller(this.getModel());
            if (this.isModelResourceValidationEnabled()) {
                unmarshaller.setSchema(context.createSchema(this.getModel()));
            }
            if (resources.isEmpty()) {
                final ResourceType defaultResource = new ResourceType();
                defaultResource.setLocation(DefaultModelProvider.getDefaultModuleLocation());
                defaultResource.setOptional(true);
                resources.add(defaultResource);
            }
            for (ResourceType resource : resources) {
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
                    suppressExceptionOnClose = true;
                    try {
                        this.logMessage(Level.FINEST, Messages.getMessage("reading", urls[i].toExternalForm()));
                        final URLConnection con = urls[i].openConnection();
                        con.setConnectTimeout(resource.getConnectTimeout());
                        con.setReadTimeout(resource.getReadTimeout());
                        con.connect();
                        in = con.getInputStream();
                        final Source source = new StreamSource(in, urls[i].toURI().toASCIIString());
                        Object o = unmarshaller.unmarshal(source);
                        if (o instanceof JAXBElement<?>) {
                            o = ((JAXBElement<?>) o).getValue();
                        }
                        if (o instanceof Module) {
                            modules.getModule().add((Module) o);
                        } else if (o instanceof Modules) {
                            modules.getModule().addAll(((Modules) o).getModule());
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
                suppressExceptionOnClose = true;
            }
            for (final Iterator<Module> it = modules.getModule().iterator(); it.hasNext(); ) {
                final Module module = it.next();
                if (!this.isModuleIncluded(module) || this.isModuleExcluded(module)) {
                    it.remove();
                    this.log(Messages.getMessage("excludingModule", module.getName()));
                } else {
                    this.log(Messages.getMessage("includingModule", module.getName()));
                }
            }
            Module classpathModule = null;
            if (this.isModelObjectClasspathResolutionEnabled()) {
                classpathModule = modules.getClasspathModule(Modules.getDefaultClasspathModuleName(), classLoader);
                if (classpathModule != null && modules.getModule(Modules.getDefaultClasspathModuleName()) == null) {
                    modules.getModule().add(classpathModule);
                } else {
                    classpathModule = null;
                }
            }
            final ModelValidationReport validationReport = context.validateModel(this.getModel(), new JAXBSource(marshaller, new ObjectFactory().createModules(modules)));
            this.logValidationReport(context, validationReport);
            if (!validationReport.isModelValid()) {
                throw new ModelException(Messages.getMessage("invalidModel", this.getModel()));
            }
            if (classpathModule != null) {
                modules.getModule().remove(classpathModule);
            }
            Module mergedModule = modules.getMergedModule(this.getModuleName());
            mergedModule.setVendor(this.getModuleVendor());
            mergedModule.setVersion(this.getModuleVersion());
            for (int i = 0, s0 = this.getModelObjectStylesheetResources().size(); i < s0; i++) {
                final Transformer transformer = this.getTransformer(this.getModelObjectStylesheetResources().get(i));
                if (transformer != null) {
                    final JAXBSource source = new JAXBSource(marshaller, new ObjectFactory().createModule(mergedModule));
                    final JAXBResult result = new JAXBResult(unmarshaller);
                    transformer.transform(source, result);
                    if (result.getResult() instanceof JAXBElement<?> && ((JAXBElement<?>) result.getResult()).getValue() instanceof Module) {
                        mergedModule = (Module) ((JAXBElement<?>) result.getResult()).getValue();
                    } else {
                        throw new BuildException(Messages.getMessage("illegalTransformationResult", this.getModelObjectStylesheetResources().get(i).getLocation()), this.getLocation());
                    }
                }
            }
            this.log(Messages.getMessage("writingEncoded", this.getModuleFile().getAbsolutePath(), this.getModuleEncoding()));
            marshaller.setProperty(Marshaller.JAXB_ENCODING, this.getModuleEncoding());
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setSchema(context.createSchema(this.getModel()));
            marshaller.marshal(new ObjectFactory().createModule(mergedModule), this.getModuleFile());
            suppressExceptionOnClose = false;
        } catch (final URISyntaxException e) {
            throw new BuildException(Messages.getMessage(e), e, this.getLocation());
        } catch (final JAXBException e) {
            String message = Messages.getMessage(e);
            if (message == null) {
                message = Messages.getMessage(e.getLinkedException());
            }
            throw new BuildException(message, e, this.getLocation());
        } catch (final TransformerConfigurationException e) {
            throw new BuildException(Messages.getMessage(e), e, this.getLocation());
        } catch (final TransformerException e) {
            throw new BuildException(Messages.getMessage(e), e, this.getLocation());
        } catch (final ModelException e) {
            throw new BuildException(Messages.getMessage(e), e, this.getLocation());
        } finally {
            try {
                if (classLoader != null) {
                    classLoader.close();
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

    /**
     * Tests inclusion of a given module based on property {@code moduleIncludes}.
     *
     * @param module The module to test.
     *
     * @return {@code true}, if {@code module} is included based on property {@code moduleIncludes}.
     *
     * @throws NullPointerException if {@code module} is {@code null}.
     *
     * @see #getModuleIncludes()
     */
    public boolean isModuleIncluded(final Module module) {
        if (module == null) {
            throw new NullPointerException("module");
        }
        for (NameType include : this.getModuleIncludes()) {
            if (include.getName().equals(module.getName())) {
                return true;
            }
        }
        return this.getModuleIncludes().isEmpty() ? true : false;
    }

    /**
     * Tests exclusion of a given module based on property {@code moduleExcludes}.
     *
     * @param module The module to test.
     *
     * @return {@code true}, if {@code module} is excluded based on property {@code moduleExcludes}.
     *
     * @throws NullPointerException if {@code module} is {@code null}.
     *
     * @see #getModuleExcludes()
     */
    public boolean isModuleExcluded(final Module module) {
        if (module == null) {
            throw new NullPointerException("module");
        }
        for (NameType exclude : this.getModuleExcludes()) {
            if (exclude.getName().equals(module.getName())) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public MergeModulesTask clone() {
        final MergeModulesTask clone = (MergeModulesTask) super.clone();
        clone.moduleFile = this.moduleFile != null ? new File(this.moduleFile.getAbsolutePath()) : null;
        if (this.moduleExcludes != null) {
            clone.moduleExcludes = new HashSet<NameType>(this.moduleExcludes.size());
            for (NameType e : this.moduleExcludes) {
                clone.moduleExcludes.add(e.clone());
            }
        }
        if (this.moduleIncludes != null) {
            clone.moduleIncludes = new HashSet<NameType>(this.moduleIncludes.size());
            for (NameType e : this.moduleIncludes) {
                clone.moduleIncludes.add(e.clone());
            }
        }
        if (this.modelObjectStylesheetResources != null) {
            clone.modelObjectStylesheetResources = new ArrayList<TransformerResourceType>(this.modelObjectStylesheetResources.size());
            for (TransformerResourceType e : this.modelObjectStylesheetResources) {
                clone.modelObjectStylesheetResources.add(e.clone());
            }
        }
        return clone;
    }
}
