package org.hibnet.gant;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.util.FileUtils;
import org.codehaus.groovy.control.CompilationFailedException;

public class GantProjectHelper extends ProjectHelper {

    private static final String REFID_CONTEXT = "gant.parsingcontext";

    private static final String REFID_BUILDER = "gant.builder";

    @Override
    public String getDefaultBuildFile() {
        return "build.groovy";
    }

    @Override
    public boolean supportsBuildFile(File buildFile) {
        return buildFile.getName().toLowerCase().endsWith(".groovy");
    }

    @Override
    public void parse(Project project, Object source) throws BuildException {
        @SuppressWarnings("unchecked") Vector<Object> stack = getImportStack();
        stack.addElement(source);
        GantParsingContext context = null;
        context = (GantParsingContext) project.getReference(REFID_CONTEXT);
        if (context == null) {
            context = new GantParsingContext();
            project.addReference(REFID_CONTEXT, context);
        }
        if (getImportStack().size() > 1) {
            Map<String, Target> currentTargets = context.getCurrentTargets();
            String currentProjectName = context.getCurrentProjectName();
            boolean imported = context.isImported();
            try {
                context.setImported(true);
                context.setCurrentTargets(new HashMap<String, Target>());
                parse(project, source, context);
            } finally {
                context.setCurrentTargets(currentTargets);
                context.setCurrentProjectName(currentProjectName);
                context.setImported(imported);
            }
        } else {
            context.setCurrentTargets(new HashMap<String, Target>());
            parse(project, source, context);
        }
    }

    private void parse(Project project, Object source, GantParsingContext context) throws BuildException {
        InputStream in;
        String buildFileName = null;
        try {
            if (source instanceof File) {
                File buildFile = (File) source;
                buildFileName = buildFile.toString();
                buildFile = FileUtils.getFileUtils().normalize(buildFile.getAbsolutePath());
                context.setBuildFile(buildFile);
                in = new FileInputStream(buildFile);
            } else if (source instanceof URL) {
                URL url = (URL) source;
                buildFileName = url.toString();
                in = url.openStream();
            } else {
                throw new BuildException("Source " + source.getClass().getName() + " not supported by this plugin");
            }
        } catch (IOException e) {
            throw new BuildException("Error reading groovy file " + buildFileName + ": " + e.getMessage(), e);
        }
        if (project.getProperty("basedir") != null) {
            project.setBasedir(project.getProperty("basedir"));
        } else {
            project.setBasedir(context.getBuildFileParent().getAbsolutePath());
        }
        GantProject gantProject;
        if (project instanceof GantProject) {
            gantProject = (GantProject) project;
        } else {
            gantProject = new GantProject(project, context, buildFileName);
        }
        defineDefaultTasks(gantProject);
        GantBuilder antBuilder = new GantBuilder(gantProject);
        gantProject.addReference(REFID_BUILDER, antBuilder);
        Binding binding = new GantBinding(gantProject, antBuilder);
        GroovyShell groovyShell = new GroovyShell(getClass().getClassLoader(), binding);
        final Script script;
        try {
            script = groovyShell.parse(in, buildFileName);
        } catch (CompilationFailedException e) {
            throw new BuildException("Error reading groovy file " + buildFileName + ": " + e.getMessage(), e);
        }
        script.setBinding(binding);
        script.setMetaClass(new GantScriptMetaClass(script.getMetaClass(), gantProject, antBuilder, context));
        new GroovyRunner() {

            @Override
            protected void doRun() {
                script.run();
            }
        }.run();
    }

    private void defineDefaultTasks(GantProject gantProject) {
        defineDefaultTask("gant", GantTask.class, gantProject);
        defineDefaultTask("subgant", SubGantTask.class, gantProject);
    }

    private void defineDefaultTask(String name, Class cl, GantProject gantProject) {
        AntTypeDefinition def = new AntTypeDefinition();
        def.setName(name);
        def.setClassName(cl.getCanonicalName());
        def.setClass(cl);
        def.setAdapterClass(null);
        def.setAdaptToClass(null);
        def.setRestrict(false);
        def.setClassLoader(this.getClass().getClassLoader());
        def.checkClass(gantProject);
        ComponentHelper.getComponentHelper(gantProject).addDataTypeDefinition(def);
    }
}
