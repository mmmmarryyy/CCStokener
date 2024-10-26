package org.jomc.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Unknown;
import org.jomc.model.Dependencies;
import org.jomc.model.Dependency;
import org.jomc.model.Implementation;
import org.jomc.model.Message;
import org.jomc.model.Messages;
import org.jomc.model.ModelException;
import org.jomc.model.ModelObject;
import org.jomc.model.Module;
import org.jomc.model.Properties;
import org.jomc.model.Property;
import org.jomc.model.Specification;
import org.jomc.model.SpecificationReference;
import org.jomc.model.Specifications;
import org.jomc.util.ParseException;
import org.jomc.util.TokenMgrError;
import org.jomc.util.VersionParser;
import org.xml.sax.SAXException;

/**
 * Manages Java classes.
 *
 * <p><b>Use cases</b><br/><ul>
 * <li>{@link #commitClasses(java.io.File) }</li>
 * <li>{@link #commitClasses(org.jomc.model.Module, java.io.File) }</li>
 * <li>{@link #commitClasses(org.jomc.model.Specification, java.io.File) }</li>
 * <li>{@link #commitClasses(org.jomc.model.Implementation, java.io.File) }</li>
 * <li>{@link #validateClasses(java.io.File) }</li>
 * <li>{@link #validateClasses(java.lang.ClassLoader) }</li>
 * <li>{@link #validateClasses(org.jomc.model.Module, java.io.File) }</li>
 * <li>{@link #validateClasses(org.jomc.model.Module, java.lang.ClassLoader) }</li>
 * <li>{@link #validateClasses(org.jomc.model.Specification, org.apache.bcel.classfile.JavaClass) }</li>
 * <li>{@link #validateClasses(org.jomc.model.Implementation, org.apache.bcel.classfile.JavaClass) }</li>
 * <li>{@link #transformClasses(java.io.File, javax.xml.transform.Transformer) }</li>
 * <li>{@link #transformClasses(org.jomc.model.Module, java.io.File, javax.xml.transform.Transformer) }</li>
 * <li>{@link #transformClasses(org.jomc.model.Specification, org.apache.bcel.classfile.JavaClass, javax.xml.transform.Transformer) }</li>
 * <li>{@link #transformClasses(org.jomc.model.Implementation, org.apache.bcel.classfile.JavaClass, javax.xml.transform.Transformer) }</li>
 * </ul></p>
 *
 * @author <a href="mailto:cs@jomc.org">Christian Schulte</a>
 * @version $Id: JavaClasses.java 847 2009-10-12 16:49:24Z schulte2005 $
 *
 * @see #getModules()
 */
public class JavaClasses extends JomcTool {

    /** Creates a new {@code JavaClasses} instance. */
    public JavaClasses() {
        super();
    }

    /**
     * Creates a new {@code JavaClasses} instance taking a {@code JavaClasses} instance to initialize the instance with.
     *
     * @param tool The instance to initialize the new instance with,
     */
    public JavaClasses(final JavaClasses tool) {
        super(tool);
    }

    /**
     * Commits meta-data of the modules of the instance to compiled Java classes.
     *
     * @param classesDirectory The directory holding the compiled class files.
     *
     * @throws NullPointerException if {@code classesDirectory} is {@code null}.
     * @throws IOException if committing meta-data fails.
     *
     * @see #commitClasses(org.jomc.model.Module, java.io.File)
     */
    public void commitClasses(final File classesDirectory) throws IOException {
        if (classesDirectory == null) {
            throw new NullPointerException("classesDirectory");
        }
        for (Module m : this.getModules().getModule()) {
            this.commitClasses(m, classesDirectory);
        }
    }

    /**
     * Commits meta-data of a given module of the modules of the instance to compiled Java classes.
     *
     * @param module The module to process.
     * @param classesDirectory The directory holding the compiled class files.
     *
     * @throws NullPointerException if {@code module} or {@code classesDirectory} is {@code null}.
     * @throws IOException if committing meta-data fails.
     *
     * @see #commitClasses(org.jomc.model.Specification, java.io.File)
     * @see #commitClasses(org.jomc.model.Implementation, java.io.File)
     */
    public void commitClasses(final Module module, final File classesDirectory) throws IOException {
        if (module == null) {
            throw new NullPointerException("module");
        }
        if (classesDirectory == null) {
            throw new NullPointerException("classesDirectory");
        }
        if (module.getSpecifications() != null) {
            for (Specification s : module.getSpecifications().getSpecification()) {
                this.commitClasses(s, classesDirectory);
            }
        }
        if (module.getImplementations() != null) {
            for (Implementation i : module.getImplementations().getImplementation()) {
                this.commitClasses(i, classesDirectory);
            }
        }
    }

    /**
     * Commits meta-data of a given specification of the modules of the instance to compiled Java classes.
     *
     * @param specification The specification to process.
     * @param classesDirectory The directory holding the compiled class files.
     *
     * @throws NullPointerException if {@code specification} or {@code classesDirectory} is {@code null}.
     * @throws IOException if committing meta-data fails.
     */
    public void commitClasses(final Specification specification, final File classesDirectory) throws IOException {
        if (specification == null) {
            throw new NullPointerException("specification");
        }
        if (classesDirectory == null) {
            throw new NullPointerException("classesDirectory");
        }
        if (this.isJavaClassDeclaration(specification)) {
            final String classLocation = specification.getClazz().replace('.', File.separatorChar) + ".class";
            final File classFile = new File(classesDirectory, classLocation);
            if (this.isLoggable(Level.INFO)) {
                this.log(Level.INFO, this.getMessage("committing", new Object[] { classFile.getAbsolutePath() }), null);
            }
            final JavaClass javaClass = this.getJavaClass(classFile);
            this.setClassfileAttribute(javaClass, Specification.class.getName(), this.encodeModelObject(this.getModelManager().getObjectFactory().createSpecification(specification)));
            javaClass.dump(classFile);
        }
    }

    /**
     * Commits meta-data of a given implementation of the modules of the instance to compiled Java classes.
     *
     * @param implementation The implementation to process.
     * @param classesDirectory The directory holding the compiled class files.
     *
     * @throws NullPointerException if {@code implementation} or {@code classesDirectory} is {@code null}.
     * @throws IOException if committing meta-data fails.
     */
    public void commitClasses(final Implementation implementation, final File classesDirectory) throws IOException {
        if (implementation == null) {
            throw new NullPointerException("implementation");
        }
        if (classesDirectory == null) {
            throw new NullPointerException("classesDirectory");
        }
        if (this.isJavaClassDeclaration(implementation)) {
            final Dependencies dependencies = new Dependencies(this.getModules().getDependencies(implementation.getIdentifier()));
            final Properties properties = new Properties(this.getModules().getProperties(implementation.getIdentifier()));
            final Messages messages = new Messages(this.getModules().getMessages(implementation.getIdentifier()));
            final Specifications specifications = new Specifications(this.getModules().getSpecifications(implementation.getIdentifier()));
            for (SpecificationReference r : specifications.getReference()) {
                if (specifications.getSpecification(r.getIdentifier()) == null && this.isLoggable(Level.WARNING)) {
                    this.log(Level.WARNING, this.getMessage("unresolvedSpecification", new Object[] { r.getIdentifier(), implementation.getIdentifier() }), null);
                }
            }
            for (Dependency d : dependencies.getDependency()) {
                final Specification s = this.getModules().getSpecification(d.getIdentifier());
                if (s != null) {
                    if (specifications.getSpecification(s.getIdentifier()) == null) {
                        specifications.getSpecification().add(s);
                    }
                } else if (this.isLoggable(Level.WARNING)) {
                    this.log(Level.WARNING, this.getMessage("unresolvedDependencySpecification", new Object[] { d.getIdentifier(), d.getName(), implementation.getIdentifier() }), null);
                }
            }
            final String classLocation = implementation.getClazz().replace('.', File.separatorChar) + ".class";
            final File classFile = new File(classesDirectory, classLocation);
            if (this.isLoggable(Level.INFO)) {
                this.log(Level.INFO, this.getMessage("committing", new Object[] { classFile.getAbsolutePath() }), null);
            }
            final JavaClass javaClass = this.getJavaClass(classFile);
            this.setClassfileAttribute(javaClass, Dependencies.class.getName(), this.encodeModelObject(this.getModelManager().getObjectFactory().createDependencies(dependencies)));
            this.setClassfileAttribute(javaClass, Properties.class.getName(), this.encodeModelObject(this.getModelManager().getObjectFactory().createProperties(properties)));
            this.setClassfileAttribute(javaClass, Messages.class.getName(), this.encodeModelObject(this.getModelManager().getObjectFactory().createMessages(messages)));
            this.setClassfileAttribute(javaClass, Specifications.class.getName(), this.encodeModelObject(this.getModelManager().getObjectFactory().createSpecifications(specifications)));
            javaClass.dump(classFile);
        }
    }

    /**
     * Validates compiled Java classes against the modules of the instance.
     *
     * @param classesDirectory The directory holding the compiled class files.
     *
     * @throws NullPointerException if {@code classesDirectory} is {@code null}.
     * @throws IOException if reading class files fails.
     * @throws ModelException if invalid classes are found.
     *
     * @see #validateClasses(org.jomc.model.Module, java.io.File)
     */
    public void validateClasses(final File classesDirectory) throws IOException, ModelException {
        if (classesDirectory == null) {
            throw new NullPointerException("classesDirectory");
        }
        final List<ModelException.Detail> details = new LinkedList<ModelException.Detail>();
        ModelException thrown = null;
        for (Module m : this.getModules().getModule()) {
            try {
                this.validateClasses(m, classesDirectory);
            } catch (final ModelException e) {
                thrown = e;
                details.addAll(e.getDetails());
            }
        }
        if (!details.isEmpty()) {
            final ModelException modelException = new ModelException(this.getMessage("validationFailed", null));
            modelException.getDetails().addAll(details);
            throw modelException;
        }
        if (thrown != null) {
            throw thrown;
        }
    }

    /**
     * Validates compiled Java classes against the modules of the instance.
     *
     * @param classLoader The class loader to search for classes.
     *
     * @throws NullPointerException if {@code classLoader} is {@code null}.
     * @throws IOException if reading class files fails.
     * @throws ModelException if invalid classes are found.
     *
     * @see #validateClasses(org.jomc.model.Module, java.lang.ClassLoader)
     */
    public void validateClasses(final ClassLoader classLoader) throws IOException, ModelException {
        if (classLoader == null) {
            throw new NullPointerException("classLoader");
        }
        final List<ModelException.Detail> details = new LinkedList<ModelException.Detail>();
        ModelException thrown = null;
        for (Module m : this.getModules().getModule()) {
            try {
                this.validateClasses(m, classLoader);
            } catch (final ModelException e) {
                thrown = e;
                details.addAll(e.getDetails());
            }
        }
        if (!details.isEmpty()) {
            final ModelException modelException = new ModelException(this.getMessage("validationFailed", null));
            modelException.getDetails().addAll(details);
            throw modelException;
        }
        if (thrown != null) {
            throw thrown;
        }
    }

    /**
     * Validates compiled Java classes against a given module of the modules of the instance.
     *
     * @param module The module to process.
     * @param classesDirectory The directory holding the compiled class files.
     *
     * @throws NullPointerException if {@code module} or {@code classesDirectory} is {@code null}.
     * @throws IOException if reading class files fails.
     * @throws ModelException if invalid classes are found.
     *
     * @see #validateClasses(org.jomc.model.Specification, org.apache.bcel.classfile.JavaClass)
     * @see #validateClasses(org.jomc.model.Implementation, org.apache.bcel.classfile.JavaClass)
     */
    public void validateClasses(final Module module, final File classesDirectory) throws IOException, ModelException {
        if (module == null) {
            throw new NullPointerException("module");
        }
        if (classesDirectory == null) {
            throw new NullPointerException("classesDirectory");
        }
        final List<ModelException.Detail> details = new LinkedList<ModelException.Detail>();
        ModelException thrown = null;
        if (module.getSpecifications() != null) {
            for (Specification s : module.getSpecifications().getSpecification()) {
                if (this.isJavaClassDeclaration(s)) {
                    final String classLocation = s.getClazz().replace('.', File.separatorChar) + ".class";
                    final File classFile = new File(classesDirectory, classLocation);
                    try {
                        this.validateClasses(s, this.getJavaClass(classFile));
                    } catch (final ModelException e) {
                        thrown = e;
                        details.addAll(e.getDetails());
                    }
                }
            }
        }
        if (module.getImplementations() != null) {
            for (Implementation i : module.getImplementations().getImplementation()) {
                if (this.isJavaClassDeclaration(i)) {
                    final String classLocation = i.getClazz().replace('.', File.separatorChar) + ".class";
                    final File classFile = new File(classesDirectory, classLocation);
                    final JavaClass javaClass = this.getJavaClass(classFile);
                    try {
                        this.validateClasses(i, javaClass);
                    } catch (final ModelException e) {
                        thrown = e;
                        details.addAll(e.getDetails());
                    }
                }
            }
        }
        if (!details.isEmpty()) {
            final ModelException modelException = new ModelException(this.getMessage("validationFailed", null));
            modelException.getDetails().addAll(details);
            throw modelException;
        }
        if (thrown != null) {
            throw thrown;
        }
    }

    /**
     * Validates compiled Java classes against a given module of the modules of the instance.
     *
     * @param module The module to process.
     * @param classLoader The class loader to search for classes.
     *
     * @throws NullPointerException if {@code module} or {@code classLoader} is {@code null}.
     * @throws IOException if reading class files fails.
     * @throws ModelException if invalid classes are found.
     *
     * @see #validateClasses(org.jomc.model.Specification, org.apache.bcel.classfile.JavaClass)
     * @see #validateClasses(org.jomc.model.Implementation, org.apache.bcel.classfile.JavaClass)
     */
    public void validateClasses(final Module module, final ClassLoader classLoader) throws IOException, ModelException {
        if (module == null) {
            throw new NullPointerException("module");
        }
        if (classLoader == null) {
            throw new NullPointerException("classLoader");
        }
        final List<ModelException.Detail> details = new LinkedList<ModelException.Detail>();
        ModelException thrown = null;
        if (module.getSpecifications() != null) {
            for (Specification s : module.getSpecifications().getSpecification()) {
                if (this.isJavaClassDeclaration(s)) {
                    final String classLocation = s.getClazz().replace('.', File.separatorChar) + ".class";
                    final URL classUrl = classLoader.getResource(classLocation);
                    if (classUrl == null) {
                        throw new IOException(this.getMessage("resourceNotFound", new Object[] { classLocation }));
                    }
                    final JavaClass javaClass = this.getJavaClass(classUrl, classLocation);
                    try {
                        this.validateClasses(s, javaClass);
                    } catch (final ModelException e) {
                        thrown = e;
                        details.addAll(e.getDetails());
                    }
                }
            }
        }
        if (module.getImplementations() != null) {
            for (Implementation i : module.getImplementations().getImplementation()) {
                if (this.isJavaClassDeclaration(i)) {
                    final String classLocation = i.getClazz().replace('.', File.separatorChar) + ".class";
                    final URL classUrl = classLoader.getResource(classLocation);
                    if (classUrl == null) {
                        throw new IOException(this.getMessage("resourceNotFound", new Object[] { classLocation }));
                    }
                    final JavaClass javaClass = this.getJavaClass(classUrl, classLocation);
                    try {
                        this.validateClasses(i, javaClass);
                    } catch (final ModelException e) {
                        thrown = e;
                        details.addAll(e.getDetails());
                    }
                }
            }
        }
        if (!details.isEmpty()) {
            final ModelException modelException = new ModelException(this.getMessage("validationFailed", null));
            modelException.getDetails().addAll(details);
            throw modelException;
        }
        if (thrown != null) {
            throw thrown;
        }
    }

    /**
     * Validates compiled Java classes against a given specification of the modules of the instance.
     *
     * @param specification The specification to process.
     * @param javaClass The class to validate.
     *
     * @throws NullPointerException if {@code specification} or {@code javaClass} is {@code null}.
     * @throws IOException if reading class files fails.
     * @throws ModelException if invalid classes are found.
     */
    public void validateClasses(final Specification specification, final JavaClass javaClass) throws IOException, ModelException {
        if (specification == null) {
            throw new NullPointerException("specification");
        }
        if (javaClass == null) {
            throw new NullPointerException("javaClass");
        }
        if (this.isLoggable(Level.INFO)) {
            this.log(Level.INFO, this.getMessage("validatingSpecification", new Object[] { specification.getIdentifier() }), null);
        }
        Specification decoded = null;
        final byte[] bytes = this.getClassfileAttribute(javaClass, Specification.class.getName());
        if (bytes != null) {
            decoded = this.decodeModelObject(bytes, Specification.class);
        }
        if (decoded != null) {
            final List<ModelException.Detail> details = new LinkedList<ModelException.Detail>();
            if (decoded.getMultiplicity() != specification.getMultiplicity()) {
                details.add(new ModelException.Detail("CLASS_ILLEGAL_SPECIFICATION_MULTIPLICITY", Level.SEVERE, this.getMessage("illegalMultiplicity", new Object[] { specification.getIdentifier(), specification.getMultiplicity().value(), decoded.getMultiplicity().value() })));
            }
            if (decoded.getScope() == null ? specification.getScope() != null : !decoded.getScope().equals(specification.getScope())) {
                details.add(new ModelException.Detail("CLASS_ILLEGAL_SPECIFICATION_SCOPE", Level.SEVERE, this.getMessage("illegalScope", new Object[] { specification.getIdentifier(), specification.getScope() == null ? "Multiton" : specification.getScope(), decoded.getScope() == null ? "Multiton" : decoded.getScope() })));
            }
            if (!decoded.getClazz().equals(specification.getClazz())) {
                details.add(new ModelException.Detail("CLASS_ILLEGAL_SPECIFICATION_CLASS", Level.SEVERE, this.getMessage("illegalSpecificationClass", new Object[] { decoded.getIdentifier(), specification.getClazz(), decoded.getClazz() })));
            }
            if (!details.isEmpty()) {
                final ModelException modelException = new ModelException(this.getMessage("validationFailed", null));
                modelException.getDetails().addAll(details);
                throw modelException;
            }
        } else if (this.isLoggable(Level.WARNING)) {
            this.log(Level.WARNING, this.getMessage("cannotValidateSpecification", new Object[] { specification.getIdentifier(), Specification.class.getName() }), null);
        }
    }

    /**
     * Validates compiled Java classes against a given implementation of the modules of the instance.
     *
     * @param implementation The implementation to process.
     * @param javaClass The class to validate.
     *
     * @throws NullPointerException if {@code implementation} or {@code javaClass} is {@code null}.
     * @throws IOException if reading class files fails.
     * @throws ModelException if invalid classes are found.
     */
    public void validateClasses(final Implementation implementation, final JavaClass javaClass) throws IOException, ModelException {
        if (implementation == null) {
            throw new NullPointerException("implementation");
        }
        if (javaClass == null) {
            throw new NullPointerException("javaClass");
        }
        if (this.isLoggable(Level.INFO)) {
            this.log(Level.INFO, this.getMessage("validatingImplementation", new Object[] { implementation.getIdentifier() }), null);
        }
        try {
            final Dependencies dependencies = new Dependencies(this.getModules().getDependencies(implementation.getIdentifier()));
            final Properties properties = new Properties(this.getModules().getProperties(implementation.getIdentifier()));
            final Messages messages = new Messages(this.getModules().getMessages(implementation.getIdentifier()));
            final Specifications specifications = new Specifications(this.getModules().getSpecifications(implementation.getIdentifier()));
            final List<ModelException.Detail> details = new LinkedList<ModelException.Detail>();
            Dependencies decodedDependencies = null;
            byte[] bytes = this.getClassfileAttribute(javaClass, Dependencies.class.getName());
            if (bytes != null) {
                decodedDependencies = this.decodeModelObject(bytes, Dependencies.class);
            }
            Properties decodedProperties = null;
            bytes = this.getClassfileAttribute(javaClass, Properties.class.getName());
            if (bytes != null) {
                decodedProperties = this.decodeModelObject(bytes, Properties.class);
            }
            Messages decodedMessages = null;
            bytes = this.getClassfileAttribute(javaClass, Messages.class.getName());
            if (bytes != null) {
                decodedMessages = this.decodeModelObject(bytes, Messages.class);
            }
            Specifications decodedSpecifications = null;
            bytes = this.getClassfileAttribute(javaClass, Specifications.class.getName());
            if (bytes != null) {
                decodedSpecifications = this.decodeModelObject(bytes, Specifications.class);
            }
            if (decodedDependencies != null) {
                for (Dependency decodedDependency : decodedDependencies.getDependency()) {
                    final Dependency dependency = dependencies.getDependency(decodedDependency.getName());
                    if (dependency == null) {
                        details.add(new ModelException.Detail("CLASS_MISSING_IMPLEMENTATION_DEPENDENCY", Level.SEVERE, this.getMessage("missingDependency", new Object[] { implementation.getIdentifier(), decodedDependency.getName() })));
                    }
                    final Specification s = this.getModules().getSpecification(decodedDependency.getIdentifier());
                    if (s != null && s.getVersion() != null && decodedDependency.getVersion() != null && VersionParser.compare(decodedDependency.getVersion(), s.getVersion()) > 0) {
                        final Module moduleOfSpecification = this.getModules().getModuleOfSpecification(s.getIdentifier());
                        final Module moduleOfImplementation = this.getModules().getModuleOfImplementation(implementation.getIdentifier());
                        details.add(new ModelException.Detail("CLASS_INCOMPATIBLE_IMPLEMENTATION_DEPENDENCY", Level.SEVERE, this.getMessage("incompatibleDependency", new Object[] { javaClass.getClassName(), moduleOfImplementation == null ? "<>" : moduleOfImplementation.getName(), s.getIdentifier(), moduleOfSpecification == null ? "<>" : moduleOfSpecification.getName(), decodedDependency.getVersion(), s.getVersion() })));
                    }
                }
            } else if (this.isLoggable(Level.WARNING)) {
                this.log(Level.WARNING, this.getMessage("cannotValidateImplementation", new Object[] { implementation.getIdentifier(), Dependencies.class.getName() }), null);
            }
            if (decodedProperties != null) {
                for (Property decodedProperty : decodedProperties.getProperty()) {
                    final Property property = properties.getProperty(decodedProperty.getName());
                    if (property == null) {
                        details.add(new ModelException.Detail("CLASS_MISSING_IMPLEMENTATION_PROPERTY", Level.SEVERE, this.getMessage("missingProperty", new Object[] { implementation.getIdentifier(), decodedProperty.getName() })));
                    } else {
                        if (decodedProperty.getType() == null ? property.getType() != null : !decodedProperty.getType().equals(property.getType())) {
                            details.add(new ModelException.Detail("CLASS_ILLEGAL_IMPLEMENTATION_PROPERTY", Level.SEVERE, this.getMessage("illegalPropertyType", new Object[] { implementation.getIdentifier(), decodedProperty.getName(), property.getType() == null ? "default" : property.getType(), decodedProperty.getType() == null ? "default" : decodedProperty.getType() })));
                        }
                    }
                }
            } else if (this.isLoggable(Level.WARNING)) {
                this.log(Level.WARNING, this.getMessage("cannotValidateImplementation", new Object[] { implementation.getIdentifier(), Properties.class.getName() }), null);
            }
            if (decodedMessages != null) {
                for (Message decodedMessage : decodedMessages.getMessage()) {
                    final Message message = messages.getMessage(decodedMessage.getName());
                    if (message == null) {
                        details.add(new ModelException.Detail("CLASS_MISSING_IMPLEMENTATION_MESSAGE", Level.SEVERE, this.getMessage("missingMessage", new Object[] { implementation.getIdentifier(), decodedMessage.getName() })));
                    }
                }
            } else if (this.isLoggable(Level.WARNING)) {
                this.log(Level.WARNING, this.getMessage("cannotValidateImplementation", new Object[] { implementation.getIdentifier(), Messages.class.getName() }), null);
            }
            if (decodedSpecifications != null) {
                for (Specification decodedSpecification : decodedSpecifications.getSpecification()) {
                    final Specification specification = this.getModules().getSpecification(decodedSpecification.getIdentifier());
                    if (specification == null) {
                        details.add(new ModelException.Detail("CLASS_MISSING_SPECIFICATION", Level.SEVERE, this.getMessage("missingSpecification", new Object[] { implementation.getIdentifier(), decodedSpecification.getIdentifier() })));
                    } else {
                        if (decodedSpecification.getMultiplicity() != specification.getMultiplicity()) {
                            details.add(new ModelException.Detail("CLASS_ILLEGAL_SPECIFICATION_MULTIPLICITY", Level.SEVERE, this.getMessage("illegalMultiplicity", new Object[] { specification.getIdentifier(), specification.getMultiplicity().value(), decodedSpecification.getMultiplicity().value() })));
                        }
                        if (decodedSpecification.getScope() == null ? specification.getScope() != null : !decodedSpecification.getScope().equals(specification.getScope())) {
                            details.add(new ModelException.Detail("CLASS_ILLEGAL_SPECIFICATION_SCOPE", Level.SEVERE, this.getMessage("illegalScope", new Object[] { decodedSpecification.getIdentifier(), specification.getScope() == null ? "Multiton" : specification.getScope(), decodedSpecification.getScope() == null ? "Multiton" : decodedSpecification.getScope() })));
                        }
                        if (!decodedSpecification.getClazz().equals(specification.getClazz())) {
                            details.add(new ModelException.Detail("CLASS_ILLEGAL_SPECIFICATION_CLASS", Level.SEVERE, this.getMessage("illegalSpecificationClass", new Object[] { decodedSpecification.getIdentifier(), specification.getClazz(), decodedSpecification.getClazz() })));
                        }
                    }
                }
                for (SpecificationReference decodedReference : decodedSpecifications.getReference()) {
                    final Specification specification = specifications.getSpecification(decodedReference.getIdentifier());
                    if (specification == null) {
                        details.add(new ModelException.Detail("CLASS_MISSING_SPECIFICATION", Level.SEVERE, this.getMessage("missingSpecification", new Object[] { implementation.getIdentifier(), decodedReference.getIdentifier() })));
                    } else if (decodedReference.getVersion() != null && specification.getVersion() != null && VersionParser.compare(decodedReference.getVersion(), specification.getVersion()) != 0) {
                        final Module moduleOfSpecification = this.getModules().getModuleOfSpecification(decodedReference.getIdentifier());
                        final Module moduleOfImplementation = this.getModules().getModuleOfImplementation(implementation.getIdentifier());
                        details.add(new ModelException.Detail("CLASS_INCOMPATIBLE_IMPLEMENTATION", Level.SEVERE, this.getMessage("incompatibleImplementation", new Object[] { javaClass.getClassName(), moduleOfImplementation == null ? "<>" : moduleOfImplementation.getName(), specification.getIdentifier(), moduleOfSpecification == null ? "<>" : moduleOfSpecification.getName(), decodedReference.getVersion(), specification.getVersion() })));
                    }
                }
            } else if (this.isLoggable(Level.WARNING)) {
                this.log(Level.WARNING, this.getMessage("cannotValidateImplementation", new Object[] { implementation.getIdentifier(), Specifications.class.getName() }), null);
            }
            if (!details.isEmpty()) {
                final ModelException modelException = new ModelException(this.getMessage("validationFailed", null));
                modelException.getDetails().addAll(details);
                throw modelException;
            }
        } catch (final ParseException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        } catch (final TokenMgrError e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
    }

    /**
     * Transforms committed meta-data of compiled Java classes of the modules of the instance.
     *
     * @param classesDirectory The directory holding the compiled class files.
     * @param transformer The transformer to use for transforming the classes.
     *
     * @throws NullPointerException if {@code classesDirectory} or {@code transformer} is {@code null}.
     * @throws IOException if accessing class files fails.
     * @throws TransformerException if transforming class files fails.
     *
     * @see #transformClasses(org.jomc.model.Module, java.io.File, javax.xml.transform.Transformer)
     */
    public void transformClasses(final File classesDirectory, final Transformer transformer) throws IOException, TransformerException {
        if (transformer == null) {
            throw new NullPointerException("transformer");
        }
        if (classesDirectory == null) {
            throw new NullPointerException("classesDirectory");
        }
        for (Module m : this.getModules().getModule()) {
            this.transformClasses(m, classesDirectory, transformer);
        }
    }

    /**
     * Transforms committed meta-data of compiled Java classes of a given module of the modules of the instance.
     *
     * @param module The module to process.
     * @param classesDirectory The directory holding the compiled class files.
     * @param transformer The transformer to use for transforming the classes.
     *
     * @throws NullPointerException if {@code module}, {@code classesDirectory} or {@code transformer} is {@code null}.
     * @throws IOException if accessing class files fails.
     * @throws TransformerException if transforming class files fails.
     *
     * @see #transformClasses(org.jomc.model.Specification, org.apache.bcel.classfile.JavaClass, javax.xml.transform.Transformer)
     * @see #transformClasses(org.jomc.model.Implementation, org.apache.bcel.classfile.JavaClass, javax.xml.transform.Transformer)
     */
    public void transformClasses(final Module module, final File classesDirectory, final Transformer transformer) throws IOException, TransformerException {
        if (module == null) {
            throw new NullPointerException("module");
        }
        if (transformer == null) {
            throw new NullPointerException("transformer");
        }
        if (classesDirectory == null) {
            throw new NullPointerException("classesDirectory");
        }
        if (module.getSpecifications() != null) {
            for (Specification s : module.getSpecifications().getSpecification()) {
                if (this.isJavaClassDeclaration(s)) {
                    final String classLocation = s.getIdentifier().replace('.', File.separatorChar) + ".class";
                    final File classFile = new File(classesDirectory, classLocation);
                    if (this.isLoggable(Level.INFO)) {
                        this.log(Level.INFO, this.getMessage("transforming", new Object[] { classFile.getAbsolutePath() }), null);
                    }
                    final JavaClass javaClass = this.getJavaClass(classFile);
                    this.transformClasses(s, javaClass, transformer);
                    javaClass.dump(classFile);
                }
            }
        }
        if (module.getImplementations() != null) {
            for (Implementation i : module.getImplementations().getImplementation()) {
                if (this.isJavaClassDeclaration(i)) {
                    final String classLocation = i.getClazz().replace('.', File.separatorChar) + ".class";
                    final File classFile = new File(classesDirectory, classLocation);
                    if (this.isLoggable(Level.INFO)) {
                        this.log(Level.INFO, this.getMessage("transforming", new Object[] { classFile.getAbsolutePath() }), null);
                    }
                    final JavaClass javaClass = this.getJavaClass(classFile);
                    this.transformClasses(i, javaClass, transformer);
                    javaClass.dump(classFile);
                }
            }
        }
    }

    /**
     * Transforms committed meta-data of compiled Java classes of a given specification of the modules of the instance.
     *
     * @param specification The specification to process.
     * @param javaClass The java class to process.
     * @param transformer The transformer to use for transforming the classes.
     *
     * @throws NullPointerException if {@code specification}, {@code javaClass} or {@code transformer} is {@code null}.
     * @throws IOException if accessing class files fails.
     * @throws TransformerException if transforming class files fails.
     */
    public void transformClasses(final Specification specification, final JavaClass javaClass, final Transformer transformer) throws IOException, TransformerException {
        if (specification == null) {
            throw new NullPointerException("specification");
        }
        if (javaClass == null) {
            throw new NullPointerException("javaClass");
        }
        if (transformer == null) {
            throw new NullPointerException("transformer");
        }
        try {
            Specification decodedSpecification = null;
            final byte[] bytes = this.getClassfileAttribute(javaClass, Specification.class.getName());
            if (bytes != null) {
                decodedSpecification = this.decodeModelObject(bytes, Specification.class);
            }
            if (decodedSpecification != null) {
                this.setClassfileAttribute(javaClass, Specification.class.getName(), this.encodeModelObject(this.getModelManager().getObjectFactory().createSpecification(this.getModelManager().transformModelObject(this.getModelManager().getObjectFactory().createSpecification(decodedSpecification), transformer))));
            }
        } catch (final SAXException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        } catch (final JAXBException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
    }

    /**
     * Transforms committed meta-data of compiled Java classes of a given implementation of the modules of the instance.
     *
     * @param implementation The implementation to process.
     * @param javaClass The java class to process.
     * @param transformer The transformer to use for transforming the classes.
     *
     * @throws NullPointerException if {@code implementation}, {@code javaClass} or {@code transformer} is {@code null}.
     * @throws IOException if accessing class files fails.
     * @throws TransformerException if transforming class files fails.
     */
    public void transformClasses(final Implementation implementation, final JavaClass javaClass, final Transformer transformer) throws TransformerException, IOException {
        if (implementation == null) {
            throw new NullPointerException("implementation");
        }
        if (javaClass == null) {
            throw new NullPointerException("javaClass");
        }
        if (transformer == null) {
            throw new NullPointerException("transformer");
        }
        try {
            Dependencies decodedDependencies = null;
            byte[] bytes = this.getClassfileAttribute(javaClass, Dependencies.class.getName());
            if (bytes != null) {
                decodedDependencies = this.decodeModelObject(bytes, Dependencies.class);
            }
            Messages decodedMessages = null;
            bytes = this.getClassfileAttribute(javaClass, Messages.class.getName());
            if (bytes != null) {
                decodedMessages = this.decodeModelObject(bytes, Messages.class);
            }
            Properties decodedProperties = null;
            bytes = this.getClassfileAttribute(javaClass, Properties.class.getName());
            if (bytes != null) {
                decodedProperties = this.decodeModelObject(bytes, Properties.class);
            }
            Specifications decodedSpecifications = null;
            bytes = this.getClassfileAttribute(javaClass, Specifications.class.getName());
            if (bytes != null) {
                decodedSpecifications = this.decodeModelObject(bytes, Specifications.class);
            }
            if (decodedDependencies != null) {
                this.setClassfileAttribute(javaClass, Dependencies.class.getName(), this.encodeModelObject(this.getModelManager().getObjectFactory().createDependencies(this.getModelManager().transformModelObject(this.getModelManager().getObjectFactory().createDependencies(decodedDependencies), transformer))));
            }
            if (decodedMessages != null) {
                this.setClassfileAttribute(javaClass, Messages.class.getName(), this.encodeModelObject(this.getModelManager().getObjectFactory().createMessages(this.getModelManager().transformModelObject(this.getModelManager().getObjectFactory().createMessages(decodedMessages), transformer))));
            }
            if (decodedProperties != null) {
                this.setClassfileAttribute(javaClass, Properties.class.getName(), this.encodeModelObject(this.getModelManager().getObjectFactory().createProperties(this.getModelManager().transformModelObject(this.getModelManager().getObjectFactory().createProperties(decodedProperties), transformer))));
            }
            if (decodedSpecifications != null) {
                this.setClassfileAttribute(javaClass, Specifications.class.getName(), this.encodeModelObject(this.getModelManager().getObjectFactory().createSpecifications(this.getModelManager().transformModelObject(this.getModelManager().getObjectFactory().createSpecifications(decodedSpecifications), transformer))));
            }
        } catch (final SAXException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        } catch (final JAXBException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
    }

    /**
     * Parses a class file.
     *
     * @param classFile The class file to parse.
     *
     * @return The parsed class file.
     *
     * @throws NullPointerException if {@code classFile} is {@code null}.
     * @throws IOException if parsing {@code classFile} fails.
     */
    public JavaClass getJavaClass(final File classFile) throws IOException {
        if (classFile == null) {
            throw new NullPointerException("classFile");
        }
        return this.getJavaClass(classFile.toURI().toURL(), classFile.getName());
    }

    /**
     * Parses a class file.
     *
     * @param url The URL of the class file to parse.
     * @param className The name of the class at {@code url}.
     *
     * @return The parsed class file.
     *
     * @throws NullPointerException if {@code url} or {@code className} is {@code null}.
     * @throws IOException if parsing fails.
     */
    public JavaClass getJavaClass(final URL url, final String className) throws IOException {
        if (url == null) {
            throw new NullPointerException("url");
        }
        if (className == null) {
            throw new NullPointerException("className");
        }
        return this.getJavaClass(url.openStream(), className);
    }

    /**
     * Parses a class file.
     *
     * @param stream The stream to read the class file from.
     * @param className The name of the class to read from {@code stream}.
     *
     * @return The parsed class file.
     *
     * @throws NullPointerException if {@code stream} or {@code className} is {@code null}.
     * @throws IOException if parsing fails.
     */
    public JavaClass getJavaClass(final InputStream stream, final String className) throws IOException {
        if (stream == null) {
            throw new NullPointerException("stream");
        }
        if (className == null) {
            throw new NullPointerException("className");
        }
        final ClassParser parser = new ClassParser(stream, className);
        final JavaClass clazz = parser.parse();
        stream.close();
        return clazz;
    }

    /**
     * Gets an attribute from a java class.
     *
     * @param clazz The java class to get an attribute from.
     * @param attributeName The name of the attribute to get.
     *
     * @return The value of attribute {@code attributeName} of {@code clazz} or {@code null} if no such attribute
     * exists.
     *
     * @throws NullPointerException if {@code clazz} or {@code attributeName} is {@code null}.
     * @throws IOException if getting the attribute fails.
     */
    public byte[] getClassfileAttribute(final JavaClass clazz, final String attributeName) throws IOException {
        if (clazz == null) {
            throw new NullPointerException("clazz");
        }
        if (attributeName == null) {
            throw new NullPointerException("attributeName");
        }
        final Attribute[] attributes = clazz.getAttributes();
        for (int i = attributes.length - 1; i >= 0; i--) {
            final Constant constant = clazz.getConstantPool().getConstant(attributes[i].getNameIndex());
            if (constant instanceof ConstantUtf8 && attributeName.equals(((ConstantUtf8) constant).getBytes())) {
                final Unknown unknown = (Unknown) attributes[i];
                return unknown.getBytes();
            }
        }
        return null;
    }

    /**
     * Adds or updates an attribute in a java class.
     *
     * @param clazz The class to update.
     * @param attributeName The name of the attribute to update.
     * @param data The new data of the attribute to update the {@code classFile} with.
     *
     * @throws NullPointerException if {@code clazz} or {@code attributeName} is {@code null}.
     * @throws IOException if updating the class file fails.
     */
    public void setClassfileAttribute(final JavaClass clazz, final String attributeName, final byte[] data) throws IOException {
        if (clazz == null) {
            throw new NullPointerException("clazz");
        }
        if (attributeName == null) {
            throw new NullPointerException("attributeName");
        }
        Attribute[] attributes = clazz.getAttributes();
        int attributeIndex = -1;
        int nameIndex = -1;
        for (int i = attributes.length - 1; i >= 0; i--) {
            final Constant constant = clazz.getConstantPool().getConstant(attributes[i].getNameIndex());
            if (constant instanceof ConstantUtf8 && attributeName.equals(((ConstantUtf8) constant).getBytes())) {
                attributeIndex = i;
                nameIndex = attributes[i].getNameIndex();
            }
        }
        if (nameIndex == -1) {
            final Constant[] pool = clazz.getConstantPool().getConstantPool();
            final Constant[] tmp = new Constant[pool.length + 1];
            System.arraycopy(pool, 0, tmp, 0, pool.length);
            tmp[pool.length] = new ConstantUtf8(attributeName);
            nameIndex = pool.length;
            clazz.setConstantPool(new ConstantPool(tmp));
        }
        final Unknown unknown = new Unknown(nameIndex, data.length, data, clazz.getConstantPool());
        if (attributeIndex == -1) {
            final Attribute[] tmp = new Attribute[attributes.length + 1];
            System.arraycopy(attributes, 0, tmp, 0, attributes.length);
            tmp[attributes.length] = unknown;
            attributes = tmp;
        } else {
            attributes[attributeIndex] = unknown;
        }
        clazz.setAttributes(attributes);
    }

    /**
     * Encodes a model object to a byte array.
     *
     * @param modelObject The model object to encode.
     *
     * @return GZIP compressed XML document for {@code modelObject}.
     *
     * @throws NullPointerException if {@code modelObject} is {@code null}.
     * @throws IOException if encoding {@code modelObject} fails.
     */
    public byte[] encodeModelObject(final JAXBElement<? extends ModelObject> modelObject) throws IOException {
        if (modelObject == null) {
            throw new NullPointerException("modelObject");
        }
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final GZIPOutputStream out = new GZIPOutputStream(baos);
            this.getModelManager().getMarshaller(false, false).marshal(modelObject, out);
            out.close();
            return baos.toByteArray();
        } catch (final SAXException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        } catch (final JAXBException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
    }

    /**
     * Decodes a model object from a byte array.
     *
     * @param bytes The encoded model object to decode.
     * @param type The type of the encoded model object.
     * @param <T> The type of the decoded model object.
     *
     * @return Model object decoded from {@code bytes}.
     *
     * @throws NullPointerException if {@code bytes} or {@code type} is {@code null}.
     * @throws IOException if decoding {@code bytes} fails.
     */
    public <T extends ModelObject> T decodeModelObject(final byte[] bytes, final Class<T> type) throws IOException {
        if (bytes == null) {
            throw new NullPointerException("bytes");
        }
        if (type == null) {
            throw new NullPointerException("type");
        }
        try {
            final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            final GZIPInputStream in = new GZIPInputStream(bais);
            final JAXBElement<T> element = (JAXBElement<T>) this.getModelManager().getUnmarshaller(false).unmarshal(in);
            in.close();
            return element.getValue();
        } catch (final SAXException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        } catch (final JAXBException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
    }

    private String getMessage(final String key, final Object args) {
        final ResourceBundle b = ResourceBundle.getBundle(JavaClasses.class.getName().replace('.', '/'));
        final MessageFormat f = new MessageFormat(b.getString(key));
        return f.format(args);
    }
}
