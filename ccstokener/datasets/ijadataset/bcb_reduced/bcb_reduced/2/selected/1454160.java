package net.sf.jsfcomp.facelets.deploy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import javax.faces.FacesException;
import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.Converter;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;
import javax.faces.render.Renderer;
import javax.faces.validator.Validator;
import net.sf.jsfcomp.facelets.deploy.DeploymentFinderFactory.FactoryType;
import net.sf.jsfcomp.facelets.deploy.factory.ParserFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.myfaces.renderkit.RenderKitFactoryImpl;
import org.apache.myfaces.renderkit.html.HtmlRenderKitImpl;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlResponseWriterImpl;
import org.jboss.seam.mock.MockApplication;
import org.jboss.seam.mock.MockExternalContext;
import org.jboss.seam.mock.MockFacesContext;
import org.jboss.seam.mock.MockViewHandler;
import org.testng.Reporter;
import org.testng.TestNG;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sun.facelets.compiler.Compiler;
import com.sun.facelets.compiler.SAXCompiler;
import com.sun.facelets.impl.DefaultFaceletFactory;
import com.sun.facelets.impl.DefaultResourceResolver;

/**
 *
 * @author Andrew Robinson (andrew)
 */
public class DeploymentTest {

    @BeforeClass(groups = "deployment")
    public void setup() throws IOException {
        BasicConfigurator.configure();
        ExternalContext extContext = new MockExternalContext();
        Application app = new CustomApplication();
        MockFacesContext context = new MockFacesContext(extContext, app) {

            private ResponseWriter writer;

            @Override
            public RenderKit getRenderKit() {
                RenderKitFactory factory = (RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
                return factory.getRenderKit(this, RenderKitFactory.HTML_BASIC_RENDER_KIT);
            }

            @Override
            public ResponseWriter getResponseWriter() {
                return writer;
            }

            @Override
            public void setResponseWriter(ResponseWriter writer) {
                this.writer = writer;
            }
        };
        context.setCurrent();
        Compiler compiler = new SAXCompiler();
        DefaultResourceResolver resourceResolver = new DefaultResourceResolver();
        DefaultFaceletFactory faceletFactory = new DefaultFaceletFactory(compiler, resourceResolver);
        DefaultFaceletFactory.setInstance(faceletFactory);
        FaceletViewHandler viewHandler = new FaceletViewHandler(new MockViewHandler()) {

            @Override
            public String getDefaultSuffix(FacesContext context) throws FacesException {
                return ".xhtml";
            }

            @Override
            public String getActionURL(FacesContext context, String viewId) {
                return viewId;
            }

            @Override
            protected ResponseWriter createResponseWriter(FacesContext context) throws IOException, FacesException {
                return new HtmlResponseWriterImpl(new OutputStreamWriter(System.out), null, null);
            }
        };
        app.setViewHandler(viewHandler);
        FactoryFinder.setFactory(FactoryFinder.RENDER_KIT_FACTORY, RenderKitFactoryImpl.class.getName());
        RenderKitFactory factory = (RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
        factory.addRenderKit(RenderKitFactory.HTML_BASIC_RENDER_KIT, new HtmlRenderKitImpl());
    }

    @Test(groups = "deployment")
    public void testFactory() throws Exception {
        DeploymentFinderFactory.registerFactoryType(FactoryType.DEPLOYER, DirOnlyParserFactory.class);
        ParserFactory factory = (ParserFactory) DeploymentFinderFactory.createFactory(FactoryType.DEPLOYER);
        FaceletAnnotationParser parser = (FaceletAnnotationParser) factory.createAnnotationParser();
        assert FaceletAnnotationParserDirOnly.class.equals(parser.getClass()) : "Wrong type";
    }

    @Test(groups = "deployment")
    public void parse() throws Exception {
        FaceletAnnotationParser parser = new FaceletAnnotationParserDirOnly();
        Set<AnnotationTagLibrary> libraries = parser.parse();
        Reporter.log("Parsed " + libraries.size() + " libraries");
        FaceletAnnotationParser.addToFacelets(libraries);
    }

    @Test(groups = "jar-deployment")
    public void parseJar() throws Exception {
        Manifest mf = new Manifest();
        Map<String, Attributes> entries = mf.getEntries();
        Attributes attr = new Attributes();
        attr.putValue("Scan", "true");
        attr.putValue("RegisterPriority", "UNLESS_REGISTERED");
        entries.put(FaceletAnnotationParser.MANIFEST_SECTION_NAME, attr);
        File tmpFile = createJarFile(mf, TestValidator.class);
        try {
            FaceletAnnotationParser parser = new FaceletAnnotationParserJarOnly(tmpFile);
            Set<AnnotationTagLibrary> libraries = parser.parse();
            assert libraries.size() == 1;
        } finally {
            tmpFile.delete();
        }
    }

    @Test(dependsOnGroups = "deployment", groups = "deployment-validation")
    public void validateFunctions() throws IOException {
        TestImplicitFunctions.invocationCount = 0;
        TestExplicitFunctions.invocationCount = 0;
        FacesContext context = FacesContext.getCurrentInstance();
        ViewHandler viewHandler = context.getApplication().getViewHandler();
        UIViewRoot view = viewHandler.createView(context, "/testfunctions.xhtml");
        assert view != null : "Unable to get test facelet";
        context.setViewRoot(view);
        view.setRenderKitId(RenderKitFactory.HTML_BASIC_RENDER_KIT);
        viewHandler.renderView(context, view);
        assert TestImplicitFunctions.invocationCount == 2 : "Implicit functions did not execute twice";
        assert TestExplicitFunctions.invocationCount == 2 : "Explicit functions did not execute twice";
    }

    @Test(dependsOnGroups = "deployment", groups = "deployment-validation")
    public void validateComponents() {
        Application app = FacesContext.getCurrentInstance().getApplication();
        UIComponent comp = app.createComponent(TestComponent.COMPONENT_TYPE);
        assert comp != null : "Test component could not be created";
        assert TestComponent.class.equals(comp.getClass()) : "Component was wrong class";
    }

    @Test(dependsOnGroups = "deployment", dependsOnMethods = "validateComponents", groups = "deployment-validation")
    public void validateRenderers() {
        Application app = FacesContext.getCurrentInstance().getApplication();
        TestComponent comp = (TestComponent) app.createComponent(TestComponent.COMPONENT_TYPE);
        Renderer renderer = comp.getRenderer(FacesContext.getCurrentInstance());
        assert renderer != null : "Renderer was null";
        assert TestRenderer.class.equals(renderer.getClass()) : "Renderer was wrong class";
    }

    @Test(dependsOnGroups = "deployment", groups = "deployment-validation")
    public void validateTagHandlers() throws Exception {
        int invocationCount = TestTagHandler.getInvocationCount();
        FacesContext context = FacesContext.getCurrentInstance();
        ViewHandler viewHandler = context.getApplication().getViewHandler();
        UIViewRoot view = viewHandler.createView(context, "/testtaghandlers.xhtml");
        assert view != null : "Unable to get test facelet";
        context.setViewRoot(view);
        view.setRenderKitId(RenderKitFactory.HTML_BASIC_RENDER_KIT);
        viewHandler.renderView(context, view);
        assert TestTagHandler.getInvocationCount() == invocationCount + 1 : "Tag handler was not called";
    }

    @Test(dependsOnGroups = "deployment", groups = "deployment-validation")
    public void validateValidators() {
        Application app = FacesContext.getCurrentInstance().getApplication();
        Validator val = app.createValidator("testValidator");
        assert val != null : "Test validator could not be created";
        assert TestValidator.class.equals(val.getClass()) : "Validator was wrong class";
    }

    @Test(dependsOnGroups = "deployment", groups = "deployment-validation")
    public void validateIdConverters() {
        Application app = FacesContext.getCurrentInstance().getApplication();
        Converter conv = app.createConverter("testConverter");
        assert conv != null : "Test converter could not be created";
        assert TestConverter.class.equals(conv.getClass()) : "Converter was wrong class";
    }

    @Test(dependsOnGroups = "deployment", groups = "deployment-validation")
    public void validateClassConverters() {
        Application app = FacesContext.getCurrentInstance().getApplication();
        Converter conv = app.createConverter(String[].class);
        assert conv != null : "Test converter could not be created";
        assert TestConverter.class.equals(conv.getClass()) : "Converter was wrong class";
    }

    private static File createJarFile(Manifest mf, Class<?>... classes) throws IOException {
        byte b[] = new byte[512];
        File jarFile = File.createTempFile("tmp_", ".jar");
        Reporter.log("Jar file: " + jarFile);
        jarFile.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(jarFile);
        JarOutputStream jout = new JarOutputStream(fos);
        JarEntry e = new JarEntry("META-INF/MANIFEST.MF");
        jout.putNextEntry(e);
        mf.write(jout);
        jout.closeEntry();
        for (Class<?> cls : classes) {
            e = new JarEntry(cls.getName().replace('.', '/') + ".class");
            jout.putNextEntry(e);
            int len = 0;
            URL url = cls.getResource("/" + e.getName());
            InputStream in = url.openStream();
            while ((len = in.read(b)) != -1) jout.write(b, 0, len);
            jout.closeEntry();
        }
        jout.close();
        return jarFile;
    }

    private static class CustomApplication extends MockApplication {

        private Map<String, String> componentMap = new HashMap<String, String>();

        private Map<String, String> validatorMap = new HashMap<String, String>();

        /**
     * @see org.jboss.seam.mock.MockApplication#addComponent(java.lang.String, java.lang.String)
     */
        @Override
        public void addComponent(String arg0, String arg1) {
            componentMap.put(arg0, arg1);
        }

        /**
     * @see org.jboss.seam.mock.MockApplication#addValidator(java.lang.String, java.lang.String)
     */
        @Override
        public void addValidator(String arg0, String arg1) {
            validatorMap.put(arg0, arg1);
        }

        /**
     * @see org.jboss.seam.mock.MockApplication#createComponent(java.lang.String)
     */
        @Override
        public UIComponent createComponent(String arg0) throws FacesException {
            try {
                String name = componentMap.get(arg0);
                Class<?> cls = Class.forName(name);
                return (UIComponent) cls.newInstance();
            } catch (Throwable error) {
                throw new FacesException(error);
            }
        }

        /**
     * @see org.jboss.seam.mock.MockApplication#createValidator(java.lang.String)
     */
        @Override
        public Validator createValidator(String arg0) throws FacesException {
            try {
                String name = validatorMap.get(arg0);
                Class<?> cls = Class.forName(name);
                return (Validator) cls.newInstance();
            } catch (Throwable error) {
                throw new FacesException(error);
            }
        }

        /**
     * @see org.jboss.seam.mock.MockApplication#getComponentTypes()
     */
        @Override
        public Iterator<?> getComponentTypes() {
            return componentMap.keySet().iterator();
        }

        /**
     * @see org.jboss.seam.mock.MockApplication#getValidatorIds()
     */
        @Override
        public Iterator<?> getValidatorIds() {
            return validatorMap.keySet().iterator();
        }

        /**
     * @see org.jboss.seam.mock.MockApplication#getDefaultRenderKitId()
     */
        @Override
        public String getDefaultRenderKitId() {
            return RenderKitFactory.HTML_BASIC_RENDER_KIT;
        }
    }

    public static class DirOnlyParserFactory implements ParserFactory {

        /**
     * @see net.sf.jsfcomp.facelets.deploy.factory.ParserFactory#createAnnotationParser()
     */
        public FaceletAnnotationParser createAnnotationParser() {
            return new FaceletAnnotationParserDirOnly();
        }
    }

    private static class FaceletAnnotationParserJarOnly extends FaceletAnnotationParser {

        private File jarFile;

        public FaceletAnnotationParserJarOnly(File jarFile) {
            this.jarFile = jarFile;
        }

        /**
     * @see net.sf.jsfcomp.facelets.deploy.FaceletAnnotationParser#getManifestsToScan()
     */
        @Override
        protected Iterator<URL> getManifestsToScan() throws IOException {
            return Collections.singleton(new URL("jar:" + jarFile.toURL().toExternalForm() + "!/META-INF/MANIFEST.MF")).iterator();
        }
    }

    private static class FaceletAnnotationParserDirOnly extends FaceletAnnotationParser {

        /**
     * @see net.sf.jsfcomp.facelets.deploy.FaceletAnnotationParser#scanClassesInJar(javax.faces.context.FacesContext, 
     * java.io.File, net.sf.jsfcomp.facelets.deploy.RegisterPriority)
     */
        @Override
        protected void scanClassesInJar(FacesContext context, File jarFile, RegisterPriority defaultPriority) throws IOException, FactoryException {
            return;
        }
    }

    public static void main(String[] args) {
        try {
            new TestNG().run();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
