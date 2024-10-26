package org.apache.ws.jaxme.generator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.ws.jaxme.generator.impl.GeneratorImpl;
import org.apache.ws.jaxme.generator.sg.SGFactoryChain;
import org.apache.ws.jaxme.generator.sg.SchemaSG;
import org.apache.ws.jaxme.generator.sg.impl.JAXBSchemaReader;
import org.apache.ws.jaxme.generator.sg.impl.JaxMeSchemaReader;
import org.apache.ws.jaxme.js.JavaSource;
import org.apache.ws.jaxme.js.JavaSourceFactory;
import org.apache.ws.jaxme.js.TextFile;
import org.apache.ws.jaxme.logging.AntProjectLoggerFactory;
import org.apache.ws.jaxme.logging.LoggerAccess;
import org.apache.ws.jaxme.logging.LoggerFactory;
import org.apache.ws.jaxme.util.ClassLoader;
import org.apache.ws.jaxme.xs.parser.impl.LocSAXException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.xml.sax.SAXParseException;

/** <p>An Ant task for running JaxMe, designed to be JAXB compatible.</p>
 * <p>This task supports the following attributes:</p>
 * <table border="1">
 *   <tr>
 *     <th>Name</th>
 *     <th>Description</th>
 *     <th>Required/Default</th>
 *   </tr>
 *   <tr>
 *     <td>schema</td>
 *     <td>Name of a schema file being compiled</td>
 *     <td>This or nested &lt;schema&gt; elements are required</td>
 *   </tr>
 *   <tr>
 *     <td>binding</td>
 *     <td>An external binding file being applied to the schema file</td>
 *     <td>No</td>
 *   </tr>
 *   <tr>
 *     <td>force</td>
 *     <td>Setting this option to true forces the up-to-date check to fail.
 *       This option is mainly useful while working on the JaxMe generator.
 *       For JaxMe users, which only change schema files, this option isn't of much
 *       use. It is designed for JaxMe developers.</td>
 *     <td>No, false</td>
 *   </tr>
 *   <tr>
 *     <td>package</td>
 *     <td>Specifies the generated Java sources package name. Overrides package specifications in
 *       the schema bindings, if any.</td>
 *     <td>No, a package may be specified in the schema bindings.</td>
 *   </tr>
 *   <tr>
 *     <td>target</td>
 *     <td>Specifies the target directory, where generated sources are being created. A package
 *       structure will be created below that directory. For example, with target="src" and
 *       package="org.acme", you will have files being created in "src/org/acme".</td>
 *     <td>No, defaults to the current directory</td>
 *   </tr>
 *   <tr>
 *     <td>readonly</td>
 *     <td>Generated Java source files are in read-only mode, if true is specified</td>
 *     <td>No, defaults to false</td>
 *   </tr>
 *   <tr>
 *     <td>extension</td>
 *     <td>If set to true, the XJC binding compiler will run in the extension mode.
 *       Otherwise, it will run in the strict conformance mode.</td>
 *     <td>No, defaults to false</td>
 *   </tr>
 *   <tr>
 *     <td>stackSize</td>
 *     <td>Specify the thread stack size for the XJC binding compiler (J2SE SDK v1.4 or higher).
 *       The XJC binding compiler can fail to compile large schemas with StackOverflowError and,
 *       in that case, this option can be used to extend the stack size. If unspecified, the default
 *       VM size is used. The format is equivalent to the -Xss command-line argument for Sun Microsystems JVM.
 *       This value can be specified in bytes (stackSize="2097152"), kilobytes (stackSize="2048kb"),
 *       or megabytes (stackSize="2mb").<br>
 *       This attribute is ignored by the JaxMe ant task and present for compatibility reasons only.</td>
 *     <td>No, defaults to false</td>
 *   </tr>
 *   <tr>
 *     <td>removeOldOutput</td>
 *     <td>If one or more nested &lt;produces&gt; elements are specified and this attribute is
 *       set to true, then the Ant task will ensure that only generated files will remain. In other
 *       words, if you had removed an element named "Foo" from the previous schema version, then the
 *       Ant task will remove "Foo.java".</td>
 *     <td>No, defaults to false</td>
 *   </tr>
 *   <tr>
 *     <td>validating</td>
 *     <td>Sets whether the XML schema parser is validating. By default it isn't.</td>
 *     <td>No, defaults to false</td>
 *   </tr>
 * </table>
 * <p>Besides the attributes, the ant task also supports the following nested elements:</p>
 * <table border="1">
 *   <tr>
 *     <th>Name</th>
 *     <th>Description</th>
 *     <th>Required/Multiplicity</th>
 *   </tr>
 *   <tr>
 *     <td>schema</td>
 *     <td>Multiple schema files may be compiled in one or more nested &lt;schema&gt;
 *       elements. The element syntax is equivalent to a nested &lt;fileset&gt;.
 *       Use of a nested &lt;schema&gt; element is mutually exclusive with the use
 *       of a "schema" attribute.</td>
 *     <td>0 - Unbounded</td>
 *   </tr>
 *   <tr>
 *     <td>binding</td>
 *     <td>Multiple external binding files may be specified. The element syntax is equivalent
 *       to a nested &lt;fileset&gt;. Use of a nested &lt;binding&gt; element is
 *       mutually exclusive with the use of a "binding" attribute.</td>
 *     <td>0 - Unbounded</td>
 *   </tr>
 *   <tr>
 *     <td>classpath</td>
 *     <td>This nested element is ignored by the JaxMe ant task and exists for compatibility
 *       to the JAXB ant task only. In the case of JAXB it specifies a classpath for loading
 *       user defined types (required in the case of a &lt;javaType&gt; customization)
 *       </td>
 *     <td>0 - Unbounded</td>
 *   </tr>
 *   <tr>
 *     <td>arg</td>
 *     <td>This nested element is ignored by the JaxMe ant task and exists for compatibility
 *       to the JAXB ant task only. In the case of JAXB it specifies additional command line
 *       arguments being passed to the XJC. For details about the syntax, see the relevant
 *       section in the Ant manual.<br>
 *       This nested element can be used to specify various options not natively supported in
 *       the xjc Ant task. For example, currently there is no native support for the following
 *       xjc command-line options:
 *       <ul>
 *         <li>-nv</li>
 *         <li>-catalog</li>
 *         <li>-use-runtime</li>
 *         <li>-schema</li>
 *         <li>-dtd</li>
 *         <li>-relaxng</li>
 *       </ul>
 *     </td>
 *     <td>0 - Unbounded</td>
 *   </tr>
 *   <tr>
 *     <td>dtd</td>
 *     <td>If this nested element is used to specify, that the input files
 *       aren't instances of XML Schema, but DTD's. The nested element may
 *       have an attribute "targetNamespace", which specifies an optional
 *       target namespace.
 *     </td>
 *     <td>No</td>
 *   </tr>
 *   <tr>
 *     <td>depends</td>
 *     <td>By default the JaxMe Ant tasks up-to-date check considers the specified schema
 *       and binding files only. This is insufficient, if other schema files are included,
 *       imported or redefined.<br>
 *       The nested &lt;depends&gt; element allows to specify additional files to consider
 *       for the up-to-date check. Typically these are the additional schema files.<br>
 *       Syntactically the &lt;depends&gt; element specifies a nested &lt;fileset&gt;.</td>
 *     <td>0 - Unbounded</td>
 *   </tr>
 *   <tr>
 *     <td>produces</td>
 *     <td>Specifies the set of files being created by the JaxMe ant task. These files are
 *       considered as targets for the up-to-date check. The syntax of the &lt;produces&gt;
 *       element is equivalent to a nested &lt;fileset&gt;. However, you typically do not
 *       need to set the "dir" attribute, because it defaults to the target directory.</td>
 *     <td>0 - Unbounded</td>
 *   </tr>
 *   <tr>
 *     <td>property</td>
 *     <td>Sets a property value. These properties may be used by the various source
 *       generators to configure the behaviour. For example, the JDBC schema reader uses
 *       the options "jdbc.driver", "jdbc.url", "jdbc.user", and "jdbc.password" to
 *       configure the database connection. Each property must have attributes "name" (the
 *       property name) and "value" (the property value).</td>
 *     <td>0 - Unbounded</td>
 *   </tr>
 *   <tr>
 *     <td>schemaReader</td>
 *     <td>Configures the schema reader to use. Defaults to
 *       "org.apache.ws.jaxme.generator.sg.impl.JAXBSchemaReader", which is the JAXB compliant
 *       schema reader. An alternative schema readers is, for example,
 *       "org.apache.ws.jaxme.generator.sg.impl.JaxMeSchemaReader" (a subclass of JAXBSchemaReader
 *       with JaxMe specific extensions).</td>
 *     <td>0 - 1</td>
 *   </tr>
 *   <tr>
 *     <td>sgFactoryChain</td>
 *     <td>If the schema reader is an instance of
 *       {@link org.apache.ws.jaxme.generator.sg.impl.JAXBSchemaReader}, then you may
 *       add instances of {@link org.apache.ws.jaxme.generator.sg.SGFactoryChain} to
 *       the schema generation process. For example, such chains are used to create
 *       the persistency layer. The best example is the
 *       {@link org.apache.ws.jaxme.pm.generator.jdbc.JaxMeJdbcSG}, which is able to
 *       populate the schema with tables and columns read from a database via
 *       JDBC metadata.</td>
 *     <td>0 - Unbounded</td>
 *   </tr>
 * </table>
 * <p>By default, the JaxMe ant task will always run the generator and create new files. This
 * is typically inappropriate for an ant script where your desire is to have as little
 * modifications as possible, because new files also need to be recompiled, which is slow
 * and time consuming.</p>
 * <p>To achieve a better behaviour, use the nested &lt;produces&gt; and &lt;depends&gt; elements.
 * If one or more &lt;produces&gt; element is specified, then an up-to-date check is performed
 * as follows:
 * <ol>
 *   <li>If either of the filesets specified by the &lt;produces&gt; elements is empty,
 *     then the binding compiler will run.</li>
 *   <li>Otherwise the sets of source and target files will be created. The set of source
 *     files is specified by the "schema" and "binding" attributes, and by the nested
 *     &lt;schema&gt;, &lt;binding&gt;, and &lt;depends&gt; elements. If any of the files
 *     in the source set is newer than any of the files in the target set, then the
 *     binding comoiler will run.</li>
 * </ol>
 *
 * @author <a href="mailto:joe@ispsoft.de">Jochen Wiedmann</a>
 */
public class XJCTask extends Task {

    /** This class is used to store the nested element "dtd".
	 */
    public static class Dtd {

        private String targetNamespace;

        /** Sets the target namespace being used.
         */
        public void setTargetNamespace(String pTargetNamespace) {
            targetNamespace = pTargetNamespace;
        }

        /** Returns the target namespace being used.
         */
        public String getTargetNamespace() {
            return targetNamespace;
        }
    }

    public static class Property {

        private String name;

        private String value;

        public void setName(String pName) {
            name = pName;
        }

        public void setValue(String pValue) {
            value = pValue;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public void finish() {
            if (name == null) {
                throw new NullPointerException("Missing attribute: 'name'");
            }
            if (value == null) {
                throw new NullPointerException("Missing attribute: 'value'");
            }
        }
    }

    public static class ClassType {

        private String className;

        public void setClassName(String pClassName) {
            className = pClassName;
        }

        public String getClassName() {
            return className;
        }

        public Object getInstance(Class pInstanceClass) {
            if (className == null) {
                throw new NullPointerException("Missing attribute: 'class'");
            }
            Class cl;
            try {
                cl = ClassLoader.getClass(className, pInstanceClass);
            } catch (ClassNotFoundException e) {
                throw new BuildException("Could not load class " + className, e);
            } catch (IllegalArgumentException e) {
                throw new BuildException(e);
            }
            try {
                return cl.newInstance();
            } catch (Exception e) {
                throw new BuildException("The class " + className + " could not be instantiated: " + e.getMessage(), e);
            }
        }
    }

    private File binding, schema, target;

    private String packageName;

    private boolean readOnly, extension, removeOldOutput, force, isValidating;

    private boolean isSettingLoggerFactory = true;

    private String stackSize;

    private List bindings = new ArrayList(), schemas = new ArrayList();

    private List depends = new ArrayList(), produces = new ArrayList();

    private List sgFactoryChains = new ArrayList();

    private ClassType schemaReader;

    private List properties = new ArrayList();

    private Dtd dtd;

    /** <p>Sets a property value. These properties may be used by the various source
   * generators to configure the behaviour. For example, the JDBC schema reader uses
   * the options "jdbc.driver", "jdbc.url", "jdbc.user", and "jdbc.password" to
   * configure the database connection. Each property must have attributes "name" (the
   * property name) and "value" (the property value).</p>
   */
    public Property createProperty() {
        Property property = new Property();
        properties.add(property);
        return property;
    }

    /** <p>Returns the configured property values. These properties may be used by the various source
   * generators to configure the behaviour. For example, the JDBC schema reader uses
   * the options "jdbc.driver", "jdbc.url", "jdbc.user", and "jdbc.password" to
   * configure the database connection. Each property must have attributes "name" (the
   * property name) and "value" (the property value).</p>
   */
    public Property[] getProperties() {
        return (Property[]) properties.toArray(new Property[properties.size()]);
    }

    /**  <p>Configures the schema reader to use. Defaults to
   * "org.apache.ws.jaxme.generator.sg.impl.JAXBSchemaReader", which is the JAXB compliant
   * schema reader. An alternative schema readers  is, for example,
   * "org.apache.ws.jaxme.generator.sg.impl.JaxMeSchemaReader" (a subclass of JAXBSchemaReader
   * with JaxMe specific extensions).</p> 
   */
    public ClassType createSchemaReader() {
        if (schemaReader != null) {
            throw new BuildException("Only one SchemaReader may be configured");
        }
        schemaReader = new ClassType();
        return schemaReader;
    }

    /**  <p>Returns the configured schema reader to use. Defaults to
   * "org.apache.ws.jaxme.generator.sg.impl.JAXBSchemaReader", which is the JAXB compliant
   * schema reader. An alternative schema readers  is, for example,
   * "org.apache.ws.jaxme.generator.sg.impl.JaxMeSchemaReader" (a subclass of JAXBSchemaReader
   * with JaxMe specific extensions).</p> 
   */
    public SchemaReader getSchemaReader() {
        if (schemaReader == null) {
            if (isExtension()) {
                return new JaxMeSchemaReader();
            } else {
                return new JAXBSchemaReader();
            }
        } else {
            return (SchemaReader) schemaReader.getInstance(SchemaReader.class);
        }
    }

    /** <p>Configures a new instance of
   * {@link org.apache.ws.jaxme.generator.sg.SGFactoryChain} being included into
   * the schema generation process. This option is valid only, if the schema reader
   * is an instance of {@link JAXBSchemaReader}, because its method
   * {@link JAXBSchemaReader#addSGFactoryChain(Class)} must be invoked.</p>
   * <p>The order of the chain elements may be significant. The schema reader
   * itself will always be the last element in the chain.</p>
   */
    public ClassType createSGFactoryChain() {
        ClassType result = new ClassType();
        sgFactoryChains.add(result);
        return result;
    }

    /** <p>Returns the array of configured instances of
   * {@link org.apache.ws.jaxme.generator.sg.SGFactoryChain}. The order of
   * the array is significant. The schema reader itself will always be the
   * last element in the chain. Therefore, it is not present in the array.</p>
   */
    public ClassType[] getSGFactoryChains() {
        return (ClassType[]) sgFactoryChains.toArray(new ClassType[sgFactoryChains.size()]);
    }

    /**  <p>Returns the ant tasks description.</p> 
   */
    public String getDescription() {
        return "A JaxMe generator task converting XML schemata into Java source files.";
    }

    /** <p>Sets whether the XML schema parser is validating.</p>
   */
    public void setValidating(boolean pValidating) {
        isValidating = pValidating;
    }

    /** <p>Returns whether the XML schema parser is validating.</p>
   */
    public boolean isValidating() {
        return isValidating;
    }

    /** <p>Setting this option to true forces the up-to-date check to fail.
   * This option is mainly useful while working on the JaxMe generator.
   * For JaxMe users, which only change schema files, this option isn't of much
   * use. It is designed for JaxMe developers.</p>
   */
    public boolean isForce() {
        return force;
    }

    /** <p>Setting this option to true forces the up-to-date check to fail.
   * This option is mainly useful while working on the JaxMe generator.
   * For JaxMe users, which only change schema files, this option isn't of much
   * use. It is designed for JaxMe developers.</p>
   */
    public void setForce(boolean pForce) {
        force = pForce;
    }

    /** <p>Returns whether the ant task is setting the {@link LoggerFactory}. This
   * option is only useful, if you are using the Ant task from another Java class
   * and not from within Ant.</p>
   */
    public boolean isSettingLoggerFactory() {
        return isSettingLoggerFactory;
    }

    /** <p>Sets whether the ant task is setting the {@link LoggerFactory}. This
   * option is only useful, if you are using the Ant task from another Java class
   * and not from within Ant.</p>
   */
    public void setSettingLoggerFactory(boolean pIsSettingLoggerFactory) {
        isSettingLoggerFactory = pIsSettingLoggerFactory;
    }

    /** <p>Returns an external binding file being applied to the schema file.</p>
   */
    public File getBinding() {
        return binding;
    }

    /** <p>Sets an external binding file being applied to the schema file.</p>
   */
    public void setBinding(File pBinding) {
        binding = pBinding;
    }

    /** <p>Returns, whether the XJC binding compiler will run in the extension mode.
   * By default, it will run in the strict conformance mode.</p>
   */
    public boolean isExtension() {
        return extension;
    }

    /** <p>Sets, whether the XJC binding compiler will run in the extension mode.
   * By default, it will run in the strict conformance mode.</p>
   */
    public void setExtension(boolean pExtension) {
        extension = pExtension;
    }

    /** <p>Returns the generated Java sources package name. A non-null package specification
   * overrides package specifications in the schema bindings, if any.</p>
   */
    public String getPackage() {
        return packageName;
    }

    /** <p>Sets the generated Java sources package name. A non-null package specification
   * overrides package specifications in the schema bindings, if any.</p>
   */
    public void setPackage(String pPackageName) {
        packageName = pPackageName;
    }

    /** @deprecated Use {@link #setPackage(String)}.
   */
    public void setPackageName(String pPackageName) {
        log("Warning: The 'packageName' attribute is updated to 'package', for compatibility reasons. Please update your build script.", Project.MSG_WARN);
        setPackage(pPackageName);
    }

    /** <p>Returns, whether generated Java source files are in read-only mode.</p>
   */
    public boolean isReadOnly() {
        return readOnly;
    }

    /** <p>Sets, whether generated Java source files are in read-only mode.</p>
   */
    public void setReadOnly(boolean pReadOnly) {
        readOnly = pReadOnly;
    }

    /** <p>If one or more nested &lt;produces&gt; elements are specified and
   * this attribute is set to true, then the Ant task will ensure that only
   * generated files will remain. In other words, if you had removed an element
   * named "Foo" from the previous schema version, then the Ant task will remove
   * "Foo.java".</p>
   */
    public boolean isRemoveOldOutput() {
        return removeOldOutput;
    }

    /** <p>If one or more nested &lt;produces&gt; elements are specified and
   * this attribute is set to true, then the Ant task will ensure that only
   * generated files will remain. In other words, if you had removed an element
   * named "Foo" from the previous schema version, then the Ant task will remove
   * "Foo.java".</p>
   */
    public void setRemoveOldOutput(boolean pRemoveOldOutput) {
        removeOldOutput = pRemoveOldOutput;
    }

    /** <p>Returns the name of the schema file being compiled.</p>
   */
    public File getSchema() {
        return schema;
    }

    /** <p>Sets the name of the schema file being compiled.</p>
   */
    public void setSchema(File pSchema) {
        schema = pSchema;
    }

    /** <p>Returns the thread stack size for the XJC binding compiler (J2SE SDK v1.4 or higher).
   * The XJC binding compiler can fail to compile large schemas with StackOverflowError and,
   * in that case, this option can be used to extend the stack size. If unspecified, the default
   * VM size is used. The format is equivalent to the -Xss command-line argument for Sun Microsystems JVM.
   * This value can be specified in bytes (stackSize="2097152"), kilobytes (stackSize="2048kb"),
   * or megabytes (stackSize="2mb").</p>
   * <p>This attribute is ignored by the JaxMe ant task and present for compatibility reasons only.</p>
   */
    public String getStackSize() {
        return stackSize;
    }

    /** <p>Sets the thread stack size for the XJC binding compiler (J2SE SDK v1.4 or higher).
   * The XJC binding compiler can fail to compile large schemas with StackOverflowError and,
   * in that case, this option can be used to extend the stack size. If unspecified, the default
   * VM size is used. The format is equivalent to the -Xss command-line argument for Sun Microsystems JVM.
   * This value can be specified in bytes (stackSize="2097152"), kilobytes (stackSize="2048kb"),
   * or megabytes (stackSize="2mb").</p>
   * <p>This attribute is ignored by the JaxMe ant task and present for compatibility reasons only.</p>
   */
    public void setStackSize(String pStackSize) {
        stackSize = pStackSize;
        log("The 'stackSize' attribute is ignored by the JaxMe ant task.", Project.MSG_WARN);
    }

    /** <p>Returns the target directory, where generated sources are being created. A package
   * structure will be created below that directory. For example, with target="src" and
   * package="org.acme", you will have files being created in "src/org/acme".</p>
   */
    public File getTarget() {
        return target;
    }

    /** <p>Sets the target directory, where generated sources are being created. A package
   * structure will be created below that directory. For example, with target="src" and
   * package="org.acme", you will have files being created in "src/org/acme".</p>
   */
    public void setTarget(File pTarget) {
        target = pTarget;
    }

    /** <p>Multiple schema files may be compiled in one or more nested &lt;schema&gt;
   * elements. The element syntax is equivalent to a nested &lt;fileset&gt;.
   * Use of a nested &lt;schema&gt; element is mutually exclusive with the use
   * of a "schema" attribute.</p>
   */
    public void addSchema(FileSet pSchemas) {
        if (getSchema() != null) {
            throw new BuildException("The 'schema' attribute and the nested 'schema' element are mutually exclusive.");
        }
        schemas.add(pSchemas);
    }

    /** <p>Multiple schema files may be compiled in one or more nested &lt;schema&gt;
   * elements. The element syntax is equivalent to a nested &lt;fileset&gt;.
   * Use of a nested &lt;schema&gt; element is mutually exclusive with the use
   * of a "schema" attribute.</p>
   */
    public FileSet[] getSchemas() {
        return (FileSet[]) schemas.toArray(new FileSet[schemas.size()]);
    }

    /** <p>Multiple external binding files may be specified. The element syntax is equivalent
   * to a nested &lt;fileset&gt;. Use of a nested &lt;binding&gt; element is
   * mutually exclusive with the use of a "binding" attribute.</p>
   */
    public void addBinding(FileSet pBindings) {
        if (getBinding() != null) {
            throw new BuildException("The 'binding' attribute and the nested 'binding' element are mutually exclusive.");
        }
        bindings.add(pBindings);
    }

    /** <p>Multiple external binding files may be specified. The element syntax is equivalent
   * to a nested &lt;fileset&gt;. Use of a nested &lt;binding&gt; element is
   * mutually exclusive with the use of a "binding" attribute.</p>
   */
    public FileSet[] getBindings() {
        return (FileSet[]) bindings.toArray(new FileSet[bindings.size()]);
    }

    /** <p>This nested element is ignored by the JaxMe ant task and exists for compatibility
   * to the JAXB ant task only. In the case of JAXB it specifies a classpath for loading
   * user defined types (required in the case of a &lt;javaType&gt; customization)</p>
   */
    public void addClasspath(Path pClasspath) {
        log("The 'classpath' attribute is ignored by the JaxMe ant task.", Project.MSG_WARN);
    }

    /** <p>This nested element is ignored by the JaxMe ant task and exists for compatibility
   * to the JAXB ant task only. In the case of JAXB it specifies additional command line
   * arguments being passed to the XJC. For details about the syntax, see the relevant
   * section in the Ant manual.<br>
   * This nested element can be used to specify various options not natively supported in
   * the xjc Ant task. For example, currently there is no native support for the following
   * xjc command-line options:
   * <ul>
   *   <li>-nv</li>
   *   <li>-catalog</li>
   *   <li>-use-runtime</li>
   *   <li>-schema</li>
   *   <li>-dtd</li>
   *   <li>-relaxng</li>
   * </ul></p>
   */
    public void addArg(Commandline.Argument pArg) {
        log("The 'arg' attribute is ignored by the JaxMe ant task.", Project.MSG_WARN);
    }

    /** <p>By default the JaxMe Ant tasks up-to-date check considers the specified schema
   * and binding files only. This is insufficient, if other schema files are included,
   * imported or redefined.<br>
   * The nested &lt;depends&gt; element allows to specify additional files to consider
   * for the up-to-date check. Typically these are the additional schema files.<br>
   * Syntactically the &lt;depends&gt; element specifies a nested &lt;fileset&gt;.</p>
   */
    public void addDepends(FileSet pDepends) {
        depends.add(pDepends);
    }

    /** <p>By default the JaxMe Ant tasks up-to-date check considers the specified schema
   * and binding files only. This is insufficient, if other schema files are included,
   * imported or redefined.<br>
   * The nested &lt;depends&gt; element allows to specify additional files to consider
   * for the up-to-date check. Typically these are the additional schema files.<br>
   * Syntactically the &lt;depends&gt; element specifies a nested &lt;fileset&gt;.</p>
   */
    public FileSet[] getDepends() {
        return (FileSet[]) depends.toArray(new FileSet[depends.size()]);
    }

    /** <p>Specifies the set of files being created by the JaxMe ant task. These files are
   * considered as targets for the up-to-date check. The syntax of the &lt;produces&gt;
   * element is equivalent to a nested &lt;fileset&gt;.</p>
   */
    public FileSet createProduces() {
        FileSet result = new FileSet();
        produces.add(result);
        return result;
    }

    /** <p>Returns the set of files being created by the JaxMe ant task. These files are
   * considered as targets for the up-to-date check. The syntax of the &lt;produces&gt;
   * element is equivalent to a nested &lt;fileset&gt;.</p>
   */
    public FileSet[] getProduces() {
        return (FileSet[]) produces.toArray(new FileSet[produces.size()]);
    }

    /** Creates a nested element "dtd".
     */
    public Dtd createDtd() {
        if (dtd == null) {
            dtd = new Dtd();
            return dtd;
        } else {
            throw new BuildException("Multiple nested 'dtd' elements are forbidden.", getLocation());
        }
    }

    /** Returns the nested element "dtd".
     */
    public Dtd getDtd() {
        return dtd;
    }

    public void finish() {
        if (getSchema() == null && getSchemas().length == 0) {
            throw new BuildException("Either of the 'schema' attribute or the nested 'schema' elements must be given.", getLocation());
        }
    }

    private File[] getFiles(FileSet[] pFileSets) {
        List list = new ArrayList();
        for (int i = 0; i < pFileSets.length; i++) {
            FileSet fileSet = pFileSets[i];
            DirectoryScanner scanner = fileSet.getDirectoryScanner(getProject());
            scanner.scan();
            String[] files = scanner.getIncludedFiles();
            for (int j = 0; j < files.length; j++) {
                list.add(new File(fileSet.getDir(getProject()), files[j]));
            }
        }
        return (File[]) list.toArray(new File[list.size()]);
    }

    private File[] getSchemaFiles() {
        if (getSchema() != null) {
            return new File[] { getSchema() };
        } else {
            return getFiles(getSchemas());
        }
    }

    private File[] getBindingFiles() {
        if (getBinding() != null) {
            return new File[] { getBinding() };
        } else {
            return getFiles(getBindings());
        }
    }

    private File[] getDependsFiles() {
        return getFiles(getDepends());
    }

    public boolean isUpToDate(File[] pSchemaFiles, File[] pBindingFiles, File[] pDependsFiles, List pProducesList) {
        FileSet[] myProduces = getProduces();
        if (myProduces.length == 0) {
            log("No nested 'produces' elements, up-to-date check returns false", Project.MSG_VERBOSE);
            return false;
        }
        boolean result = true;
        long firstTarget = 0;
        File firstTargetFile = null;
        for (int i = 0; i < myProduces.length; i++) {
            File dir = myProduces[i].getDir(getProject());
            if (dir == null) {
                dir = getTarget();
                if (dir == null) {
                    dir = getProject().getBaseDir();
                }
                myProduces[i].setDir(dir);
            }
            if (!dir.exists()) {
                log("The directory specified by the nested 'produces' element #" + i + " does not exist, up-to-date check returns false", Project.MSG_VERBOSE);
                result = false;
                continue;
            }
            DirectoryScanner scanner = myProduces[i].getDirectoryScanner(getProject());
            scanner.scan();
            String[] files = scanner.getIncludedFiles();
            if (files.length == 0) {
                log("The fileset specified by the nested 'produces' element #" + i + " is empty, up-to-date check returns false", Project.MSG_VERBOSE);
                result = false;
            }
            for (int j = 0; j < files.length; j++) {
                File f = new File(dir, files[j]).getAbsoluteFile();
                if (pProducesList != null) {
                    pProducesList.add(f);
                }
                long l = f.lastModified();
                if (l == -1) {
                    log("Unable to determine timestamp of target file " + f + ", up-to-date check returns false.", Project.MSG_VERBOSE);
                    result = false;
                }
                if (firstTargetFile == null || firstTarget > l) {
                    firstTargetFile = f;
                    firstTarget = l;
                }
            }
        }
        if (isForce()) {
            log("Force option is set, up-to-date check returns false", Project.MSG_VERBOSE);
            result = false;
        }
        if (!result) {
            return false;
        }
        List sourceFiles = new ArrayList();
        for (int i = 0; i < pSchemaFiles.length; i++) {
            sourceFiles.add(pSchemaFiles[i]);
        }
        for (int i = 0; i < pBindingFiles.length; i++) {
            sourceFiles.add(pBindingFiles[i]);
        }
        for (int i = 0; i < pDependsFiles.length; i++) {
            sourceFiles.add(pDependsFiles[i]);
        }
        long lastSource = 0;
        File lastSourceFile = null;
        for (Iterator iter = sourceFiles.iterator(); iter.hasNext(); ) {
            File f = (File) iter.next();
            long l = f.lastModified();
            if (l == -1) {
                log("Unable to determine timestamp of source file " + f + ", up-to-date check returns false.", Project.MSG_VERBOSE);
                result = false;
            }
            if (lastSourceFile == null || lastSource < l) {
                lastSource = l;
                lastSourceFile = f;
            }
        }
        if (lastSourceFile == null) {
            log("No source files found, up-to-date check returns false.", Project.MSG_VERBOSE);
            return false;
        }
        if (!result) {
            return false;
        }
        try {
            URL url = Generator.class.getClassLoader().getResource(Generator.class.getName().replace('.', '/') + ".class");
            if (url != null) {
                long l = url.openConnection().getLastModified();
                if (l != 0 && lastSource < l) {
                    log("Generator class is newer than any schema files, using Generator classes timestamp as schema timestamp.", Project.MSG_DEBUG);
                    lastSource = l;
                }
            }
        } catch (IOException e) {
        }
        if (lastSource >= firstTarget) {
            log("Source file " + lastSourceFile + " is more recent than target file " + firstTargetFile + ", up-to-date check returns false", Project.MSG_VERBOSE);
            return false;
        }
        log("All target files are up-to-date.", Project.MSG_VERBOSE);
        return true;
    }

    public class MyClassLoader extends java.lang.ClassLoader {

        private java.lang.ClassLoader parent;

        public java.lang.ClassLoader getMyParent() {
            return parent;
        }

        public MyClassLoader(java.lang.ClassLoader pParent) {
            super(XJCTask.this.getClass().getClassLoader());
            parent = pParent;
        }

        public Class findClass(String name) throws ClassNotFoundException {
            return parent.loadClass(name);
        }

        public URL findResource(String resource) {
            return parent.getResource(resource);
        }

        public Enumeration findResources(String resource) throws IOException {
            return parent.getResources(resource);
        }
    }

    public void stopLogging(LoggerFactory pFactory) {
        if (pFactory != null) {
            LoggerAccess.setLoggerFactory(pFactory);
        }
    }

    public LoggerFactory initLogging() {
        if (!isSettingLoggerFactory()) {
            return null;
        }
        LoggerFactory loggerFactory = LoggerAccess.getLoggerFactory();
        if (!(loggerFactory instanceof AntProjectLoggerFactory)) {
            loggerFactory = new AntProjectLoggerFactory(this);
            LoggerAccess.setLoggerFactory(loggerFactory);
            return loggerFactory;
        }
        return null;
    }

    public void execute() {
        java.lang.ClassLoader parent = Thread.currentThread().getContextClassLoader();
        MyClassLoader cl = new MyClassLoader(parent == null ? getClass().getClassLoader() : parent);
        LoggerFactory loggerFactory = initLogging();
        try {
            Thread.currentThread().setContextClassLoader(cl);
            File[] schemaFiles = getSchemaFiles();
            if (schemaFiles.length == 0) {
                log("No schema files specified", Project.MSG_WARN);
                return;
            }
            File[] bindingFiles = getBindingFiles();
            if (bindingFiles.length > 0) {
                throw new BuildException("External schema bindings are still unsupported by JaxMe.", getLocation());
            }
            File[] dependFiles = getDependsFiles();
            List producesFiles = isRemoveOldOutput() ? new ArrayList() : null;
            if (isUpToDate(schemaFiles, bindingFiles, dependFiles, producesFiles)) {
                return;
            }
            Set producesFilesSet = null;
            if (producesFiles != null) {
                producesFilesSet = new HashSet();
                for (Iterator iter = producesFiles.iterator(); iter.hasNext(); ) {
                    File f = ((File) iter.next());
                    producesFilesSet.add(f);
                }
            }
            Generator generator = new GeneratorImpl();
            generator.setForcingOverwrite(isForce());
            generator.setSettingReadOnly(isReadOnly());
            generator.setValidating(isValidating());
            if (getPackage() != null) {
                generator.setProperty("jaxme.package.name", getPackage());
            }
            Dtd myDtd = getDtd();
            if (myDtd != null) {
                generator.setProperty("jaxme.dtd.input", "true");
                if (myDtd.getTargetNamespace() != null) {
                    generator.setProperty("jaxme.dtd.targetNamespace", myDtd.getTargetNamespace());
                }
            }
            Property[] myProperties = getProperties();
            for (int i = 0; i < myProperties.length; i++) {
                Property ot = myProperties[i];
                log("Option " + ot.getName() + "=" + ot.getValue(), Project.MSG_VERBOSE);
                generator.setProperty(ot.getName(), ot.getValue());
            }
            SchemaReader reader = getSchemaReader();
            if (reader instanceof JAXBSchemaReader) {
                ((JAXBSchemaReader) reader).setSupportingExtensions(isExtension());
            }
            generator.setSchemaReader(reader);
            reader.setGenerator(generator);
            generator.setTargetDirectory(getTarget());
            ClassType[] mySgFactoryChains = getSGFactoryChains();
            if (mySgFactoryChains.length > 0) {
                if (!(reader instanceof JAXBSchemaReader)) {
                    throw new BuildException("The nested child element 'sgFactoryChain' is valid only, if the schema reader is an instance of " + JAXBSchemaReader.class.getName(), getLocation());
                }
                for (int i = 0; i < mySgFactoryChains.length; i++) {
                    ClassType ct = mySgFactoryChains[i];
                    Class c;
                    try {
                        c = cl.loadClass(ct.getClassName());
                    } catch (ClassNotFoundException e) {
                        throw new BuildException("Failed to load SGFactoryChain implementation class " + ct.getClassName(), getLocation());
                    }
                    if (!SGFactoryChain.class.isAssignableFrom(c)) {
                        throw new BuildException("The SGFactoryChain class " + c.getName() + " is not implementing " + SGFactoryChain.class.getName(), getLocation());
                    }
                    reader.addSGFactoryChain(c);
                }
            }
            for (int i = 0; i < schemaFiles.length; i++) {
                log("Reading schema file " + schemaFiles[i], Project.MSG_VERBOSE);
                try {
                    SchemaSG schemaSG = generator.generate(schemaFiles[i]);
                    if (producesFilesSet != null) {
                        JavaSourceFactory jsf = schemaSG.getJavaSourceFactory();
                        File targetDirectory = getTarget();
                        for (Iterator iter = jsf.getJavaSources(); iter.hasNext(); ) {
                            JavaSource js = (JavaSource) iter.next();
                            File f = jsf.getLocation(targetDirectory, js).getAbsoluteFile();
                            producesFilesSet.remove(f);
                        }
                        for (Iterator iter = jsf.getTextFiles(); iter.hasNext(); ) {
                            TextFile tf = (TextFile) iter.next();
                            File f = jsf.getLocation(targetDirectory, tf).getAbsoluteFile();
                            producesFilesSet.remove(f);
                        }
                    }
                } catch (SAXParseException e) {
                    e.printStackTrace();
                    String msg = LocSAXException.formatMsg(e.getMessage() == null ? e.getClass().getName() : e.getMessage(), e.getPublicId(), e.getSystemId(), e.getLineNumber(), e.getColumnNumber());
                    System.out.println(msg);
                    throw new BuildException(msg, e, getLocation());
                } catch (Exception e) {
                    String msg = e.getMessage();
                    if (msg == null) {
                        msg = e.getClass().getName();
                    }
                    e.printStackTrace();
                    System.out.println(msg);
                    throw new BuildException(schemaFiles[i] + ": " + msg, e, getLocation());
                }
            }
            if (producesFilesSet != null) {
                for (Iterator iter = producesFilesSet.iterator(); iter.hasNext(); ) {
                    File f = (File) iter.next();
                    log("Removing orphan file " + f, Project.MSG_VERBOSE);
                    if (!f.delete()) {
                        throw new BuildException("Unable to delete file " + f);
                    }
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(parent);
            stopLogging(loggerFactory);
        }
    }
}
