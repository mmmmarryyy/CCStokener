package at.ac.tuwien.springmorphia;

import at.ac.tuwien.springmorphia.basic.DBRE;
import at.ac.tuwien.springmorphia.basic.MyRepositoryLayer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.log4j.Logger;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.controller.WebMvcOperations;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.ImportMetadata;
import org.springframework.roo.classpath.details.ImportMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Repository;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.osgi.UrlFindingUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import static org.springframework.roo.model.RooJavaType.ROO_JAVA_BEAN;
import static org.springframework.roo.model.RooJavaType.ROO_MONGO_ENTITY;
import static org.springframework.roo.model.RooJavaType.ROO_TO_STRING;

/**
 * Implementation of {@link SpringmorphiaOperations} interface.
 *
 * @since 1.1.1
 */
@Component
@Service
public class SpringmorphiaOperationsImpl implements SpringmorphiaOperations {

    private static final char SEPARATOR = File.separatorChar;

    private final Logger logger = Logger.getLogger(SpringmorphiaOperations.class.getName());

    private String entityName = "Object";

    private ComponentContext context;

    @Reference
    private TypeManagementService typeManagementService;

    @Reference
    private MetadataService metadataService;

    @Reference
    private WebMvcOperations mvcOperations;

    /**
     * This routine is called when the component is activated.
     * @param context
     */
    protected void activate(ComponentContext context) {
        this.context = context;
    }

    /**
     * Get a reference to the FileManager from the underlying OSGi container. Make sure you
     * are referencing the Roo bundle which contains this service in your add-on pom.xml.
     *
     * Using the Roo file manager instead if java.io.File gives you automatic rollback in case
     * an Exception is thrown.
     */
    @Reference
    private FileManager fileManager;

    /**
     * Get a reference to the ProjectOperations from the underlying OSGi container. Make sure you
     * are referencing the Roo bundle which contains this service in your add-on pom.xml.
     */
    @Reference
    private ProjectOperations projectOperations;

    /** {@inheritDoc} */
    public boolean isInstallTagsCommandAvailable() {
        String moduleName = projectOperations.getFocusedModuleName();
        boolean isProjAvailable = projectOperations.isProjectAvailable(moduleName);
        return isProjAvailable;
    }

    /** {@inheritDoc} */
    public String getProperty(String propertyName) {
        Assert.hasText(propertyName, "Property name required");
        return System.getProperty(propertyName);
    }

    /** {@inheritDoc} */
    public void installTags() {
        PathResolver pathResolver = projectOperations.getPathResolver();
        LogicalPath lp = LogicalPath.getInstance(Path.SRC_MAIN_WEBAPP, "");
        createOrReplaceFile(pathResolver.getIdentifier(lp, "WEB-INF" + SEPARATOR + "tags" + SEPARATOR + "util"), "info.tagx");
        createOrReplaceFile(pathResolver.getIdentifier(lp, "WEB-INF" + SEPARATOR + "tags" + SEPARATOR + "form"), "show.tagx");
    }

    /**
     * sets the project with the initial setup up.
     * @param isGWT boolean flag that indicates, if it should be initalized as GWT Project
     * @param ip the Ip of the Mongo Instance
     * @param schema the Database schema of the mongo instance
     */
    public void setProjectUp(boolean isGWT, String ip, String schema) {
        logger.info("Updating project dependencies SpringMorphiaOperationsImpl");
        updatePomDependencies(isGWT);
        if (ip != null && !ip.isEmpty() && schema != null && !schema.isEmpty()) {
            DBRE reverse = new DBRE();
            String[] args = new String[] { ip, schema };
            reverse.main(args);
            Map<String, HashMap<String, List<String>>> result = reverse.getResult();
            for (Map.Entry e : result.entrySet()) {
                HashMap<String, List<String>> entityInfo = (HashMap<String, List<String>>) e.getValue();
                JavaType curEnt = new JavaType(e.getKey().toString());
                createEntity(curEnt, "Entity", true);
                entityName = curEnt.toString();
                for (Map.Entry info : entityInfo.entrySet()) {
                    String propertyName = info.getKey().toString();
                    List bonusString = (List) info.getValue();
                    String typ = (String) bonusString.get(0);
                    String[] split = typ.split("\\.");
                    typ = split[split.length - 1];
                    JavaType type = new JavaType(typ);
                    String annotation = (String) bonusString.get(1);
                    if (!typ.equals("ObjectId")) {
                        if (!annotation.contains("public")) {
                            logger.info(String.format("adding Property with params %s,%s,%s,%s", curEnt, propertyName, typ, annotation));
                            addProperty(curEnt, propertyName, type, annotation);
                        } else {
                            logger.info(String.format("adding Property with params %s,%s,%s,%s", curEnt, propertyName, typ, null));
                            addProperty(curEnt, propertyName, type, null);
                        }
                    }
                }
            }
        }
        if (isGWT) {
            if (!fileManager.exists(projectOperations.getPathResolver().getIdentifier(LogicalPath.getInstance(Path.SRC_MAIN_WEBAPP, ""), "/WEB-INF/web.xml"))) {
                mvcOperations.installAllWebMvcArtifacts();
            }
            logger.info("Coping Template Files - GWT");
            ProjectPath p7 = ProjectPath.MODEL_CLIENT;
            moveFiles(p7);
            ProjectPath p1 = ProjectPath.ROOT;
            copyTemplates(p1);
            ProjectPath p2 = ProjectPath.WEB;
            copyTemplates(p2);
            ProjectPath p3 = ProjectPath.ENTRYPOINT;
            copyTemplates(p3);
            ProjectPath p4 = ProjectPath.SERVER;
            copyTemplates(p4);
            ProjectPath p5 = ProjectPath.SERVICE;
            copyTemplates(p5);
            ProjectPath p6 = ProjectPath.TABLE;
            copyTemplates(p6);
        } else {
            logger.info("Coping Template Files - normal");
            ProjectPath p = ProjectPath.MODEL;
            copyTemplates(p);
        }
    }

    /**
     * Creates an Entity of Morphia
     * This is directly called by the create Morphia command
     * @param name
     * @param annotation
     * @param init indicates if the Entity should be initalized as entity
     */
    public void createEntity(JavaType name, String annotation, boolean init) {
        logger.info("Creating Entity " + name.getSimpleTypeName());
        if (annotation == null || annotation.isEmpty()) {
            annotation = "Entity";
            entityName = name.getSimpleTypeName();
        }
        LogicalPath lp = LogicalPath.getInstance(Path.SRC_MAIN_JAVA, "");
        String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, lp);
        ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, name, PhysicalTypeCategory.CLASS);
        ImportMetadataBuilder met = new ImportMetadataBuilder(declaredByMetadataId);
        StringBuilder annotationString = new StringBuilder("com.google.code.morphia.annotations.");
        annotationString.append(annotation);
        met.setImportType(new JavaType(annotationString.toString()));
        ImportMetadataBuilder idTag = new ImportMetadataBuilder(declaredByMetadataId);
        idTag.setImportType(new JavaType("com.google.code.morphia.annotations.Id"));
        Set<ImportMetadata> imps = new HashSet<ImportMetadata>();
        ImportMetadataBuilder serial = new ImportMetadataBuilder(declaredByMetadataId);
        serial.setImportType(new JavaType("java.io.Serializable"));
        ImportMetadataBuilder rooJavaBean = new ImportMetadataBuilder(declaredByMetadataId);
        rooJavaBean.setImportType(new JavaType("org.springframework.roo.addon.javabean.RooJavaBean"));
        ImportMetadataBuilder mongoEntity = new ImportMetadataBuilder(declaredByMetadataId);
        mongoEntity.setImportType(new JavaType("org.springframework.roo.addon.layers.repository.mongo.RooMongoEntity"));
        imps.add(rooJavaBean.build());
        imps.add(mongoEntity.build());
        imps.add(met.build());
        imps.add(idTag.build());
        imps.add(serial.build());
        typeDetailsBuilder.setRegisteredImports(imps);
        typeDetailsBuilder.addAnnotation(new AnnotationMetadataBuilder(new JavaType(annotation)));
        typeDetailsBuilder.addAnnotation(new AnnotationMetadataBuilder(ROO_JAVA_BEAN));
        typeDetailsBuilder.addAnnotation(new AnnotationMetadataBuilder(ROO_MONGO_ENTITY));
        typeDetailsBuilder.addAnnotation(new AnnotationMetadataBuilder(ROO_TO_STRING));
        if (annotation.equals("Entity")) {
            typeDetailsBuilder.addImplementsType(new JavaType("Serializable"));
        }
        if (annotation.equals("Entity")) {
            FieldMetadataBuilder fieldBuilder = null;
            logger.info("Initalize as GWT ? --> " + init);
            if (init) {
                JavaSymbolName fieldName = new JavaSymbolName("id");
                fieldBuilder = new FieldMetadataBuilder(declaredByMetadataId, Modifier.PRIVATE, fieldName, JavaType.STRING_OBJECT, "");
                fieldBuilder.addAnnotation(new AnnotationMetadataBuilder(new JavaType("Id")));
                InvocableMemberBodyBuilder getter = new InvocableMemberBodyBuilder();
                getter.newLine();
                getter.append("return id;");
                getter.appendIndent();
                MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("getId"), JavaType.STRING_OBJECT, getter);
                MethodMetadata mm = methodBuilder.build();
                InvocableMemberBodyBuilder setter = new InvocableMemberBodyBuilder();
                setter.newLine();
                setter.append("this.id = id;");
                setter.appendIndent();
                MethodMetadataBuilder methodBuilder2 = new MethodMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("setId"), JavaType.VOID_PRIMITIVE, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), setter);
                methodBuilder2.addParameter("id", JavaType.STRING_OBJECT);
                MethodMetadata mm2 = methodBuilder2.build();
                typeDetailsBuilder.addMethod(mm);
                typeDetailsBuilder.addMethod(mm2);
                typeDetailsBuilder.addField(fieldBuilder.build());
            }
            JavaSymbolName serialUID = new JavaSymbolName("serialVersionUID");
            FieldMetadataBuilder serialBuild = new FieldMetadataBuilder(declaredByMetadataId, Modifier.STATIC, serialUID, JavaType.LONG_OBJECT, "1L");
            logger.info("FieldBuilder: " + fieldBuilder);
            typeManagementService.createOrUpdateTypeOnDisk(typeDetailsBuilder.build());
            typeManagementService.addField(serialBuild.build());
        }
    }

    /**
     * This method adds a property to a given class with a given property..
     */
    public void addProperty(JavaType entityName, String propertyName, JavaType type, String annotation) {
        logger.info("Altering Entity Test");
        LogicalPath lp = LogicalPath.getInstance(Path.SRC_MAIN_JAVA, "");
        String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(entityName, lp);
        if (type == null) {
            type = JavaType.STRING_OBJECT;
        }
        JavaType s_type = new JavaType(type.getSimpleTypeName());
        JavaSymbolName fieldName = new JavaSymbolName(propertyName);
        FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(declaredByMetadataId, Modifier.PRIVATE, fieldName, s_type, null);
        if (annotation != null) {
            fieldBuilder.addAnnotation(new AnnotationMetadataBuilder(new JavaType(annotation)));
        }
        InvocableMemberBodyBuilder getter = new InvocableMemberBodyBuilder();
        getter.newLine();
        getter.append("return ").append(propertyName).append(";");
        getter.appendIndent();
        StringBuilder getterName = new StringBuilder().append("get").append(propertyName);
        MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName(getterName.toString()), s_type, getter);
        MethodMetadata mm = methodBuilder.build();
        InvocableMemberBodyBuilder setter = new InvocableMemberBodyBuilder();
        setter.newLine();
        setter.append("this.").append(propertyName).append(" = ").append(propertyName).append(";");
        setter.appendIndent();
        List<JavaSymbolName> parameters = new ArrayList<JavaSymbolName>();
        JavaSymbolName idPara = new JavaSymbolName(propertyName);
        parameters.add(idPara);
        logger.info("return type is: " + s_type);
        List<AnnotatedJavaType> parametersType = new ArrayList<AnnotatedJavaType>();
        StringBuilder setterName = new StringBuilder().append("set").append(propertyName);
        MethodMetadataBuilder methodBuilder2 = new MethodMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName(setterName.toString()), JavaType.VOID_PRIMITIVE, parametersType, parameters, setter);
        MethodMetadata mm2 = methodBuilder2.build();
        FieldMetadata data = fieldBuilder.build();
        PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(data.getDeclaredByMetadataId());
        ClassOrInterfaceTypeDetails details = ptm.getMemberHoldingTypeDetails();
        ClassOrInterfaceTypeDetailsBuilder mutableTypeDetails = new ClassOrInterfaceTypeDetailsBuilder(details);
        mutableTypeDetails.addMethod(mm);
        mutableTypeDetails.addMethod(mm2);
        typeManagementService.addField(fieldBuilder.build());
        typeManagementService.createOrUpdateTypeOnDisk(mutableTypeDetails.build());
    }

    /**
     * Moves a File from a directory into the target directory.
     * @param pPath 
     */
    private void moveFiles(ProjectPath pPath) {
        logger.info(String.format("Handling Path %s", pPath.segmentPackage()));
        final String moduleName = projectOperations.getFocusedModuleName();
        StringBuilder strb = new StringBuilder();
        strb.append(projectOperations.getProjectMetadata(moduleName).toString()).append(".").append("model");
        String topLevelPackage = projectOperations.getTopLevelPackage(moduleName).toString();
        StringBuilder targetPackage = new StringBuilder();
        targetPackage.append(topLevelPackage).append(".client").append(".").append("model");
        StringBuilder sourcePackage = new StringBuilder();
        sourcePackage.append(topLevelPackage).append(".").append("model");
        String sourcePath = ProjectPath.ROOT.canonicalFileSystemPath(projectOperations);
        sourcePath = sourcePath.concat("/");
        String targetDirectory = pPath.canonicalFileSystemPath(projectOperations);
        if (!targetDirectory.endsWith("/")) {
            targetDirectory += "/";
        }
        logger.info(String.format("moving Files into %s", targetDirectory));
        if (!fileManager.exists(targetDirectory)) {
            fileManager.createDirectory(targetDirectory);
        }
        logger.info(String.format("From Source Path %s", sourcePath));
        File parent = new File(sourcePath);
        File[] children = parent.listFiles();
        logger.info("Children: " + Arrays.toString(children));
        if (children != null && children.length > 0) {
            for (File f : children) {
                if (!f.isDirectory()) {
                    logger.info("File: " + f.getName());
                    String targetFilename = targetDirectory.concat(f.getName());
                    try {
                        logger.info("Copied file");
                        InputStream i = new FileInputStream(f);
                        String input = FileCopyUtils.copyToString(new InputStreamReader(i));
                        logger.info(String.format("replace %s with %s", topLevelPackage, targetPackage.toString()));
                        input = input.replace(topLevelPackage, targetPackage.toString());
                        MutableFile mutableFile = fileManager.createFile(targetFilename);
                        FileCopyUtils.copy(input.getBytes(), mutableFile.getOutputStream());
                        f.delete();
                    } catch (IOException ioe) {
                        throw new IllegalStateException("Unable to create '" + targetFilename + "'", ioe);
                    }
                }
            }
            parent.delete();
        }
    }

    /**
     * At least Morphia is a kind of a
     * Database extension, so we put all files into a model directory
     */
    private void copyTemplates(ProjectPath pPath) {
        String sourceAntPath = pPath.sourceAntPath();
        final String moduleName = projectOperations.getFocusedTopLevelPackage().toString();
        logger.info("Module Name: " + moduleName);
        String targetDirectory = pPath.canonicalFileSystemPath(projectOperations);
        logger.info("Moving into target Directory: " + targetDirectory);
        if (!targetDirectory.endsWith("/")) {
            targetDirectory += "/";
        }
        if (!fileManager.exists(targetDirectory)) {
            fileManager.createDirectory(targetDirectory);
        }
        System.out.println("Target Directory: " + pPath.sourceAntPath());
        String path = TemplateUtils.getTemplatePath(getClass(), sourceAntPath);
        Set<URL> urls = UrlFindingUtils.findMatchingClasspathResources(context.getBundleContext(), path);
        Assert.notNull(urls, "Could not search bundles for resources for Ant Path '" + path + "'");
        if (urls.isEmpty()) {
            logger.info("URLS are empty stopping...");
        }
        for (URL url : urls) {
            logger.info("Stepping into " + url.toExternalForm());
            String fileName = url.getPath().substring(url.getPath().lastIndexOf("/") + 1);
            fileName = fileName.replace("-template", "");
            String targetFilename = targetDirectory + fileName;
            logger.info("Handling " + targetFilename);
            if (!fileManager.exists(targetFilename)) {
                try {
                    logger.info("Copied file");
                    String input = FileCopyUtils.copyToString(new InputStreamReader(url.openStream()));
                    logger.info("TopLevelPackage: " + projectOperations.getFocusedTopLevelPackage());
                    logger.info("SegmentPackage: " + pPath.canonicalFileSystemPath(projectOperations));
                    String topLevelPackage = projectOperations.getFocusedTopLevelPackage().toString();
                    input = input.replace("__TOP_LEVEL_PACKAGE__", topLevelPackage);
                    input = input.replace("__SEGMENT_PACKAGE__", pPath.segmentPackage());
                    input = input.replace("__PROJECT_NAME__", projectOperations.getFocusedProjectName());
                    input = input.replace("__ENTITY_NAME__", entityName);
                    MutableFile mutableFile = fileManager.createFile(targetFilename);
                    FileCopyUtils.copy(input.getBytes(), mutableFile.getOutputStream());
                } catch (IOException ioe) {
                    throw new IllegalStateException("Unable to create '" + targetFilename + "'", ioe);
                }
            }
        }
    }

    /**
     * A private method which illustrates how to reference and manipulate resources
     * in the target project as well as the bundle classpath.
     *
     * @param path
     * @param fileName
     */
    private void createOrReplaceFile(String path, String fileName) {
        String targetFile = path + SEPARATOR + fileName;
        MutableFile mutableFile = fileManager.exists(targetFile) ? fileManager.updateFile(targetFile) : fileManager.createFile(targetFile);
        try {
            FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), fileName), mutableFile.getOutputStream());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * This updates the POM; specified under src/main/resources/
     * at.ac.tuwien.springmorphia.configuration.xml
     * it returns a valid updated pom
     * or an error notfication (in the case if the pom isnt valid)
     *
     * It has also 2 valid states
     * 1) The pom is valid and readable => update it
     * 2) display error (in this case the roo project will alway fail)
     * 
     */
    private void updatePomDependencies(boolean isGWT) {
        final String moduleName = projectOperations.getFocusedModuleName();
        String templateName = "configuration.xml";
        if (isGWT) {
            templateName = "configuration-gwt.xml";
        }
        try {
            Element configuration = getConfiguration(templateName);
            if (configuration == null) {
                logger.info("Configuration not found");
            }
            List<Element> dependencies = XmlUtils.findElements("/configuration/dependencies/dependency", configuration);
            if (dependencies.isEmpty()) {
                logger.info("Dependencies not found");
            }
            for (Element dependency : dependencies) {
                logger.info(String.format("Updating %s", dependency.getAttributes()));
                projectOperations.addDependency(moduleName, new Dependency(dependency));
            }
            List<Element> vegaRepositories = XmlUtils.findElements("/configuration/repositories/repository", configuration);
            for (Element repositoryElement : vegaRepositories) {
                projectOperations.addRepository(moduleName, new Repository(repositoryElement));
            }
            List<Element> buildOptions = XmlUtils.findElements("/configuration/build/plugins/plugin", configuration);
            for (Element buildPlugin : buildOptions) {
                projectOperations.addBuildPlugin(moduleName, new Plugin(buildPlugin));
            }
            logger.info("Updating DONE");
        } catch (Exception ex) {
            logger.error("POM NOT FOUND OR POM ISN'T VALID");
        }
    }

    private Element getConfiguration(String templateName) {
        InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), templateName);
        logger.info("Using Configuration Template: " + TemplateUtils.getTemplatePath(getClass(), templateName).toString());
        Assert.notNull(templateInputStream, "Could not acquire " + templateName + " file");
        Document dependencyDoc;
        try {
            dependencyDoc = XmlUtils.getDocumentBuilder().parse(templateInputStream);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return (Element) dependencyDoc.getFirstChild();
    }
}
