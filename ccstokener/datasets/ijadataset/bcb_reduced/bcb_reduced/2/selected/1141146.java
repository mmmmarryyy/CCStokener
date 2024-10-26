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
import org.jomc.ant.types.ModletResourceType;
import org.jomc.ant.types.NameType;
import org.jomc.ant.types.ResourceType;
import org.jomc.ant.types.TransformerResourceType;
import org.jomc.modlet.DefaultModletProvider;
import org.jomc.modlet.ModelContext;
import org.jomc.modlet.ModelException;
import org.jomc.modlet.ModelValidationReport;
import org.jomc.modlet.Modlet;
import org.jomc.modlet.ModletObject;
import org.jomc.modlet.Modlets;
import org.jomc.modlet.ObjectFactory;

/**
 * Task for merging modlet resources.
 *
 * @author <a href="mailto:schulte2005@users.sourceforge.net">Christian Schulte</a>
 * @version $JOMC$
 */
public final class MergeModletsTask extends JomcTask {

    /** The encoding of the modlet resource. */
    private String modletEncoding;

    /** File to write the merged modlet to. */
    private File modletFile;

    /** The name of the merged modlet. */
    private String modletName;

    /** The version of the merged modlet. */
    private String modletVersion;

    /** The vendor of the merged modlet. */
    private String modletVendor;

    /** Resources to merge. */
    private Set<ModletResourceType> modletResources;

    /** Included modlets. */
    private Set<NameType> modletIncludes;

    /** Excluded modlets. */
    private Set<NameType> modletExcludes;

    /** XSLT documents to use for transforming modlet objects. */
    private List<TransformerResourceType> modletObjectStylesheetResources;

    /** Creates a new {@code MergeModletsTask} instance. */
    public MergeModletsTask() {
        super();
    }

    /**
     * Gets the file to write the merged modlet to.
     *
     * @return The file to write the merged modlet to or {@code null}.
     *
     * @see #setModletFile(java.io.File)
     */
    public File getModletFile() {
        return this.modletFile;
    }

    /**
     * Sets the file to write the merged modlet to.
     *
     * @param value The new file to write the merged modlet to or {@code null}.
     *
     * @see #getModletFile()
     */
    public void setModletFile(final File value) {
        this.modletFile = value;
    }

    /**
     * Gets the encoding of the modlet resource.
     *
     * @return The encoding of the modlet resource.
     *
     * @see #setModletEncoding(java.lang.String)
     */
    public String getModletEncoding() {
        if (this.modletEncoding == null) {
            this.modletEncoding = new OutputStreamWriter(new ByteArrayOutputStream()).getEncoding();
        }
        return this.modletEncoding;
    }

    /**
     * Sets the encoding of the modlet resource.
     *
     * @param value The new encoding of the modlet resource or {@code null}.
     *
     * @see #getModletEncoding()
     */
    public void setModletEncoding(final String value) {
        this.modletEncoding = value;
    }

    /**
     * Gets the name of the merged modlet.
     *
     * @return The name of the merged modlet or {@code null}.
     *
     * @see #setModletName(java.lang.String)
     */
    public String getModletName() {
        return this.modletName;
    }

    /**
     * Sets the name of the merged modlet.
     *
     * @param value The new name of the merged modlet or {@code null}.
     *
     * @see #getModletName()
     */
    public void setModletName(final String value) {
        this.modletName = value;
    }

    /**
     * Gets the version of the merged modlet.
     *
     * @return The version of the merged modlet or {@code null}.
     *
     * @see #setModletVersion(java.lang.String)
     */
    public String getModletVersion() {
        return this.modletVersion;
    }

    /**
     * Sets the version of the merged modlet.
     *
     * @param value The new version of the merged modlet or {@code null}.
     *
     * @see #getModletVersion()
     */
    public void setModletVersion(final String value) {
        this.modletVersion = value;
    }

    /**
     * Gets the vendor of the merged modlet.
     *
     * @return The vendor of the merge modlet or {@code null}.
     *
     * @see #setModletVendor(java.lang.String)
     */
    public String getModletVendor() {
        return this.modletVendor;
    }

    /**
     * Sets the vendor of the merged modlet.
     *
     * @param value The new vendor of the merged modlet or {@code null}.
     *
     * @see #getModletVendor()
     */
    public void setModletVendor(final String value) {
        this.modletVendor = value;
    }

    /**
     * Gets a set of resource names to merge.
     * <p>This accessor method returns a reference to the live set, not a snapshot. Therefore any modification you make
     * to the returned set will be present inside the object. This is why there is no {@code set} method for the
     * modlet resources property.</p>
     *
     * @return A set of names of resources to merge.
     *
     * @see #createModletResource()
     */
    public Set<ModletResourceType> getModletResources() {
        if (this.modletResources == null) {
            this.modletResources = new HashSet<ModletResourceType>();
        }
        return this.modletResources;
    }

    /**
     * Creates a new {@code modletResource} element instance.
     *
     * @return A new {@code modletResource} element instance.
     *
     * @see #getModletResources()
     */
    public ModletResourceType createModletResource() {
        final ModletResourceType modletResource = new ModletResourceType();
        this.getModletResources().add(modletResource);
        return modletResource;
    }

    /**
     * Gets a set of modlet names to include.
     * <p>This accessor method returns a reference to the live set, not a snapshot. Therefore any modification you make
     * to the returned set will be present inside the object. This is why there is no {@code set} method for the
     * modlet includes property.</p>
     *
     * @return A set of modlet names to include.
     *
     * @see #createModletInclude()
     */
    public Set<NameType> getModletIncludes() {
        if (this.modletIncludes == null) {
            this.modletIncludes = new HashSet<NameType>();
        }
        return this.modletIncludes;
    }

    /**
     * Creates a new {@code modletInclude} element instance.
     *
     * @return A new {@code modletInclude} element instance.
     *
     * @see #getModletIncludes()
     */
    public NameType createModletInclude() {
        final NameType modletInclude = new NameType();
        this.getModletIncludes().add(modletInclude);
        return modletInclude;
    }

    /**
     * Gets a set of modlet names to exclude.
     * <p>This accessor method returns a reference to the live set, not a snapshot. Therefore any modification you make
     * to the returned set will be present inside the object. This is why there is no {@code set} method for the
     * modlet excludes property.</p>
     *
     * @return A set of modlet names to exclude.
     *
     * @see #createModletExclude()
     */
    public Set<NameType> getModletExcludes() {
        if (this.modletExcludes == null) {
            this.modletExcludes = new HashSet<NameType>();
        }
        return this.modletExcludes;
    }

    /**
     * Creates a new {@code modletExclude} element instance.
     *
     * @return A new {@code modletExclude} element instance.
     *
     * @see #getModletExcludes()
     */
    public NameType createModletExclude() {
        final NameType modletExclude = new NameType();
        this.getModletExcludes().add(modletExclude);
        return modletExclude;
    }

    /**
     * Gets the XSLT documents to use for transforming modlet objects.
     * <p>This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make
     * to the returned list will be present inside the object. This is why there is no {@code set} method for the
     * modlet object stylesheet resources property.</p>
     *
     * @return The XSLT documents to use for transforming modlet objects.
     *
     * @see #createModletObjectStylesheetResource()
     */
    public List<TransformerResourceType> getModletObjectStylesheetResources() {
        if (this.modletObjectStylesheetResources == null) {
            this.modletObjectStylesheetResources = new LinkedList<TransformerResourceType>();
        }
        return this.modletObjectStylesheetResources;
    }

    /**
     * Creates a new {@code modletObjectStylesheetResource} element instance.
     *
     * @return A new {@code modletObjectStylesheetResource} element instance.
     *
     * @see #getModletObjectStylesheetResources()
     */
    public TransformerResourceType createModletObjectStylesheetResource() {
        final TransformerResourceType modletObjectStylesheetResource = new TransformerResourceType();
        this.getModletObjectStylesheetResources().add(modletObjectStylesheetResource);
        return modletObjectStylesheetResource;
    }

    /** {@inheritDoc} */
    @Override
    public void preExecuteTask() throws BuildException {
        super.preExecuteTask();
        this.assertNotNull("modletFile", this.getModletFile());
        this.assertNotNull("modletName", this.getModletName());
        this.assertNamesNotNull(this.getModletExcludes());
        this.assertNamesNotNull(this.getModletIncludes());
        this.assertLocationsNotNull(this.getModletResources());
        this.assertLocationsNotNull(this.getModletObjectStylesheetResources());
    }

    /**
     * Merges modlet resources.
     *
     * @throws BuildException if merging modlet resources fails.
     */
    @Override
    public void executeTask() throws BuildException {
        ProjectClassLoader classLoader = null;
        boolean suppressExceptionOnClose = true;
        try {
            this.log(Messages.getMessage("mergingModlets", this.getModel()));
            classLoader = this.newProjectClassLoader();
            final Modlets modlets = new Modlets();
            final Set<ResourceType> resources = new HashSet<ResourceType>(this.getModletResources());
            final ModelContext context = this.newModelContext(classLoader);
            final Marshaller marshaller = context.createMarshaller(ModletObject.MODEL_PUBLIC_ID);
            final Unmarshaller unmarshaller = context.createUnmarshaller(ModletObject.MODEL_PUBLIC_ID);
            if (this.isModletResourceValidationEnabled()) {
                unmarshaller.setSchema(context.createSchema(ModletObject.MODEL_PUBLIC_ID));
            }
            if (resources.isEmpty()) {
                final ResourceType defaultResource = new ResourceType();
                defaultResource.setLocation(DefaultModletProvider.getDefaultModletLocation());
                defaultResource.setOptional(true);
                resources.add(defaultResource);
            }
            for (ResourceType resource : resources) {
                final URL[] urls = this.getResources(context, resource.getLocation());
                if (urls.length == 0) {
                    if (resource.isOptional()) {
                        this.logMessage(Level.WARNING, Messages.getMessage("modletResourceNotFound", resource.getLocation()));
                    } else {
                        throw new BuildException(Messages.getMessage("modletResourceNotFound", resource.getLocation()));
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
                        if (o instanceof Modlet) {
                            modlets.getModlet().add((Modlet) o);
                        } else if (o instanceof Modlets) {
                            modlets.getModlet().addAll(((Modlets) o).getModlet());
                        } else {
                            this.logMessage(Level.WARNING, Messages.getMessage("unsupportedModletResource", urls[i].toExternalForm()));
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
            for (String defaultExclude : classLoader.getModletExcludes()) {
                final Modlet m = modlets.getModlet(defaultExclude);
                if (m != null) {
                    modlets.getModlet().remove(m);
                }
            }
            modlets.getModlet().addAll(classLoader.getExcludedModlets().getModlet());
            for (final Iterator<Modlet> it = modlets.getModlet().iterator(); it.hasNext(); ) {
                final Modlet modlet = it.next();
                if (!this.isModletIncluded(modlet) || this.isModletExcluded(modlet)) {
                    it.remove();
                    this.log(Messages.getMessage("excludingModlet", modlet.getName()));
                } else {
                    this.log(Messages.getMessage("includingModlet", modlet.getName()));
                }
            }
            final ModelValidationReport validationReport = context.validateModel(ModletObject.MODEL_PUBLIC_ID, new JAXBSource(marshaller, new ObjectFactory().createModlets(modlets)));
            this.logValidationReport(context, validationReport);
            if (!validationReport.isModelValid()) {
                throw new ModelException(Messages.getMessage("invalidModel", ModletObject.MODEL_PUBLIC_ID));
            }
            Modlet mergedModlet = modlets.getMergedModlet(this.getModletName(), this.getModel());
            mergedModlet.setVendor(this.getModletVendor());
            mergedModlet.setVersion(this.getModletVersion());
            for (int i = 0, s0 = this.getModletObjectStylesheetResources().size(); i < s0; i++) {
                final Transformer transformer = this.getTransformer(this.getModletObjectStylesheetResources().get(i));
                if (transformer != null) {
                    final JAXBSource source = new JAXBSource(marshaller, new ObjectFactory().createModlet(mergedModlet));
                    final JAXBResult result = new JAXBResult(unmarshaller);
                    transformer.transform(source, result);
                    if (result.getResult() instanceof JAXBElement<?> && ((JAXBElement<?>) result.getResult()).getValue() instanceof Modlet) {
                        mergedModlet = (Modlet) ((JAXBElement<?>) result.getResult()).getValue();
                    } else {
                        throw new BuildException(Messages.getMessage("illegalTransformationResult", this.getModletObjectStylesheetResources().get(i).getLocation()), this.getLocation());
                    }
                }
            }
            this.log(Messages.getMessage("writingEncoded", this.getModletFile().getAbsolutePath(), this.getModletEncoding()));
            marshaller.setProperty(Marshaller.JAXB_ENCODING, this.getModletEncoding());
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setSchema(context.createSchema(ModletObject.MODEL_PUBLIC_ID));
            marshaller.marshal(new ObjectFactory().createModlet(mergedModlet), this.getModletFile());
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
     * Tests inclusion of a given modlet based on property {@code modletIncludes}.
     *
     * @param modlet The modlet to test.
     *
     * @return {@code true}, if {@code modlet} is included based on property {@code modletIncludes}.
     *
     * @throws NullPointerException if {@code modlet} is {@code null}.
     *
     * @see #getModletIncludes()
     */
    public boolean isModletIncluded(final Modlet modlet) {
        if (modlet == null) {
            throw new NullPointerException("modlet");
        }
        for (NameType include : this.getModletIncludes()) {
            if (include.getName().equals(modlet.getName())) {
                return true;
            }
        }
        return this.getModletIncludes().isEmpty() ? true : false;
    }

    /**
     * Tests exclusion of a given modlet based on property {@code modletExcludes}.
     *
     * @param modlet The modlet to test.
     *
     * @return {@code true}, if {@code modlet} is excluded based on property {@code modletExcludes}.
     *
     * @throws NullPointerException if {@code modlet} is {@code null}.
     *
     * @see #getModletExcludes()
     */
    public boolean isModletExcluded(final Modlet modlet) {
        if (modlet == null) {
            throw new NullPointerException("modlet");
        }
        for (NameType exclude : this.getModletExcludes()) {
            if (exclude.getName().equals(modlet.getName())) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public MergeModletsTask clone() {
        final MergeModletsTask clone = (MergeModletsTask) super.clone();
        clone.modletFile = this.modletFile != null ? new File(this.modletFile.getAbsolutePath()) : null;
        if (this.modletResources != null) {
            clone.modletResources = new HashSet<ModletResourceType>(this.modletResources.size());
            for (ModletResourceType e : this.modletResources) {
                clone.modletResources.add(e.clone());
            }
        }
        if (this.modletExcludes != null) {
            clone.modletExcludes = new HashSet<NameType>(this.modletExcludes.size());
            for (NameType e : this.modletExcludes) {
                clone.modletExcludes.add(e.clone());
            }
        }
        if (this.modletIncludes != null) {
            clone.modletIncludes = new HashSet<NameType>(this.modletIncludes.size());
            for (NameType e : this.modletIncludes) {
                clone.modletIncludes.add(e.clone());
            }
        }
        if (this.modletObjectStylesheetResources != null) {
            clone.modletObjectStylesheetResources = new ArrayList<TransformerResourceType>(this.modletObjectStylesheetResources.size());
            for (TransformerResourceType e : this.modletObjectStylesheetResources) {
                clone.modletObjectStylesheetResources.add(e.clone());
            }
        }
        return clone;
    }
}
