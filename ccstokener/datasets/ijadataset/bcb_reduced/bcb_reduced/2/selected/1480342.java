package org.maven.ide.eclipse.checkstyle;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringInputStream;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;
import com.atlassw.tools.eclipse.checkstyle.config.CheckConfigurationWorkingCopy;
import com.atlassw.tools.eclipse.checkstyle.config.ICheckConfiguration;
import com.atlassw.tools.eclipse.checkstyle.config.ICheckConfigurationWorkingSet;
import com.atlassw.tools.eclipse.checkstyle.config.ResolvableProperty;
import com.atlassw.tools.eclipse.checkstyle.config.configtypes.ConfigurationTypes;
import com.atlassw.tools.eclipse.checkstyle.config.configtypes.IConfigurationType;
import com.atlassw.tools.eclipse.checkstyle.nature.CheckstyleNature;
import com.atlassw.tools.eclipse.checkstyle.projectconfig.FileMatchPattern;
import com.atlassw.tools.eclipse.checkstyle.projectconfig.FileSet;
import com.atlassw.tools.eclipse.checkstyle.projectconfig.IProjectConfiguration;
import com.atlassw.tools.eclipse.checkstyle.projectconfig.ProjectConfigurationFactory;
import com.atlassw.tools.eclipse.checkstyle.projectconfig.ProjectConfigurationWorkingCopy;
import com.atlassw.tools.eclipse.checkstyle.util.CheckstylePluginException;

/**
 * @author <a href="nicolas@apache.org">Nicolas De loof</a>
 * @author <a href="Peter.Hayes@fmr.com">Peter Hayes</a>
 */
public class CheckstyleProjectConfigurator extends AbstractProjectConfigurator {

    /**
     *
     */
    private static final String CONFIGURATION_NAME = "maven-chekstyle-plugin";

    private static final String CHECKSTYLE_PLUGIN_GROUPID = "org.apache.maven.plugins";

    private static final String CHECKSTYLE_PLUGIN_ARTIFACTID = "maven-checkstyle-plugin";

    private IConfigurationType remoteConfigurationType = ConfigurationTypes.getByInternalName("remote");

    /**
     * {@inheritDoc}
     * 
     * @see org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator#configure(org.apache.maven.embedder.MavenEmbedder,
     *      org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest,
     *      org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void configure(MavenEmbedder embedder, ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
        Plugin plugin = getCheckstylePlugin(request.getMavenProject());
        if (plugin == null) {
            return;
        }
        createCheckstyleConfiguration(embedder, request.getMavenProject(), plugin, request.getProject(), monitor);
        addNature(request.getProject(), CheckstyleNature.NATURE_ID, monitor);
    }

    /**
     * Setup the eclipse Checkstyle plugin based on maven plugin configuration and resources
     * 
     * @throws CheckstylePluginException
     */
    private void createCheckstyleConfiguration(MavenEmbedder embedder, MavenProject mavenProject, Plugin mavenPlugin, IProject project, IProgressMonitor monitor) throws CoreException {
        try {
            IProjectConfiguration projectConfig = ProjectConfigurationFactory.getConfiguration(project);
            ProjectConfigurationWorkingCopy copy = new ProjectConfigurationWorkingCopy(projectConfig);
            copy.setUseSimpleConfig(false);
            URL ruleSet = createOrUpdateMavenCheckstyle(embedder, mavenProject, mavenPlugin, project, monitor);
            if (ruleSet == null) {
                return;
            }
            console.logMessage("Configure checkstyle from ruleSet " + ruleSet);
            ICheckConfiguration checkConfig = createOrUpdateLocalCheckConfiguration(project, copy, ruleSet);
            if (checkConfig == null) {
                return;
            }
            createOrUpdateFileSet(project, mavenProject, copy, checkConfig);
            if (copy.isDirty()) {
                copy.store();
            }
        } catch (CheckstylePluginException cpe) {
            embedder.getLogger().error("Failed to configure Checkstyle plugin", cpe);
        }
    }

    private URL createOrUpdateMavenCheckstyle(MavenEmbedder embedder, MavenProject mavenProject, Plugin mavenPlugin, IProject project, IProgressMonitor monitor) throws CoreException {
        ClassLoader classLoader = configureClassLoader(embedder, mavenProject, mavenPlugin);
        String configLocation = extractConfiguredCSLocation(mavenPlugin);
        if (configLocation == null) {
            configLocation = "config/sun_checks.xml";
        }
        URL url = locateRuleSet(classLoader, configLocation);
        return url;
    }

    private ICheckConfiguration createOrUpdateLocalCheckConfiguration(IProject project, ProjectConfigurationWorkingCopy projectConfig, URL ruleSet) throws CheckstylePluginException {
        ICheckConfigurationWorkingSet checkConfig = projectConfig.getLocalCheckConfigWorkingSet();
        CheckConfigurationWorkingCopy workingCopy;
        ICheckConfiguration existing = projectConfig.getLocalCheckConfigByName(CONFIGURATION_NAME);
        if (existing != null) {
            if (remoteConfigurationType.equals(existing.getType())) {
                console.logMessage("A local Checkstyle configuration allready exists with name " + CONFIGURATION_NAME + ". It will be updated to maven plugin configuration");
                workingCopy = new CheckConfigurationWorkingCopy(existing, checkConfig);
            } else {
                console.logError("A local Checkstyle configuration allready exists with name " + CONFIGURATION_NAME + " with incompatible type");
                return null;
            }
        } else {
            workingCopy = new CheckConfigurationWorkingCopy(remoteConfigurationType, checkConfig, false);
        }
        workingCopy.setName(CONFIGURATION_NAME);
        workingCopy.setDescription("Maven checkstyle configuration");
        workingCopy.setLocation(ruleSet.toExternalForm());
        workingCopy.getResolvableProperties().add(new ResolvableProperty("checkstyle.cache.file", "${project_loc}/checkstyle-cachefile"));
        if (existing == null) {
            checkConfig.addCheckConfiguration(workingCopy);
        }
        return workingCopy;
    }

    private void createOrUpdateFileSet(IProject project, MavenProject mavenProject, ProjectConfigurationWorkingCopy copy, ICheckConfiguration checkConfig) throws CheckstylePluginException {
        copy.getFileSets().clear();
        FileSet fileSet = new FileSet("java-sources", checkConfig);
        fileSet.setEnabled(true);
        List<FileMatchPattern> patterns = new ArrayList<FileMatchPattern>();
        URI projectURI = project.getLocationURI();
        List compileSourceRoots = mavenProject.getCompileSourceRoots();
        for (Iterator iter = compileSourceRoots.iterator(); iter.hasNext(); ) {
            String compileSourceRoot = (String) iter.next();
            File compileSourceRootFile = new File(compileSourceRoot);
            URI compileSourceRootURI = compileSourceRootFile.toURI();
            String relativePath = projectURI.relativize(compileSourceRootURI).getPath();
            patterns.add(new FileMatchPattern(relativePath));
        }
        fileSet.setFileMatchPatterns(patterns);
        copy.getFileSets().add(fileSet);
    }

    private URL locateRuleSet(ClassLoader classloader, String location) {
        File file = new File(location);
        if (file.exists()) {
            try {
                return file.toURL();
            } catch (MalformedURLException e) {
            }
        }
        try {
            URL url = new URL(location);
            url.openStream();
        } catch (MalformedURLException e) {
        } catch (Exception e) {
        }
        URL url = classloader.getResource(location);
        if (url == null) {
            console.logError("Failed to locate Checkstyle configuration " + location);
        } else {
            String str = url.toString();
            if (str.startsWith("jar:file:/") && str.charAt(10) != '/') {
                try {
                    url = new URL(str.substring(0, 9) + "//localhost" + str.substring(9));
                } catch (MalformedURLException e) {
                }
            }
        }
        return url;
    }

    private ClassLoader configureClassLoader(MavenEmbedder embedder, MavenProject mavenProject, Plugin mavenPlugin) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        List<URL> jars = new LinkedList<URL>();
        Artifact pluginArtifact = null;
        try {
            String version = mavenPlugin.getVersion();
            if (version == null) {
                version = Artifact.LATEST_VERSION;
            }
            pluginArtifact = embedder.createArtifact(mavenPlugin.getGroupId(), mavenPlugin.getArtifactId(), version, "compile", "maven-plugin");
        } catch (Exception e) {
            embedder.getLogger().error("Could not create classpath", e);
        }
        try {
            embedder.resolve(pluginArtifact, mavenProject.getRemoteArtifactRepositories(), embedder.getLocalRepository());
        } catch (ArtifactResolutionException e) {
            embedder.getLogger().error("Could not resolve artifact: " + pluginArtifact);
        } catch (ArtifactNotFoundException e) {
            embedder.getLogger().error("Could not find artifact: " + pluginArtifact);
        }
        if (pluginArtifact.isResolved()) {
            try {
                jars.add(pluginArtifact.getFile().toURI().toURL());
            } catch (MalformedURLException e) {
                embedder.getLogger().error("Could not create URL for artifact: " + pluginArtifact.getFile());
            }
        }
        List dependencies = mavenPlugin.getDependencies();
        if (dependencies != null && dependencies.size() > 0) {
            for (int i = 0; i < dependencies.size(); i++) {
                Dependency dependency = (Dependency) dependencies.get(i);
                Artifact artifact = embedder.createArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getScope(), dependency.getType());
                try {
                    embedder.resolve(artifact, mavenProject.getRemoteArtifactRepositories(), embedder.getLocalRepository());
                } catch (ArtifactResolutionException e) {
                    embedder.getLogger().error("Could not resolve artifact: " + artifact);
                } catch (ArtifactNotFoundException e) {
                    embedder.getLogger().error("Could not find artifact: " + artifact);
                }
                if (artifact.isResolved()) {
                    try {
                        jars.add(0, artifact.getFile().toURI().toURL());
                    } catch (MalformedURLException e) {
                        embedder.getLogger().error("Could not create URL for artifact: " + artifact.getFile());
                    }
                }
            }
        }
        classLoader = new URLClassLoader(jars.toArray(new URL[0]), classLoader);
        return classLoader;
    }

    private String extractConfiguredCSLocation(Plugin csPlugin) {
        Xpp3Dom[] rulesetDoms = null;
        Object configuration = csPlugin.getConfiguration();
        if (configuration instanceof Xpp3Dom) {
            Xpp3Dom configDom = (Xpp3Dom) configuration;
            Xpp3Dom configLocationDom = configDom.getChild("configLocation");
            if (configLocationDom != null) {
                return configLocationDom.getValue();
            }
        }
        return null;
    }

    /**
     * @see http://maven.apache.org/plugins/maven-checkstyle-plugin/examples/custom-property-expansion.html
     */
    private Properties extractCustomProperties(Plugin csPlugin) {
        Xpp3Dom[] rulesetDoms = null;
        Properties properties = new Properties();
        Object configuration = csPlugin.getConfiguration();
        if (configuration instanceof Xpp3Dom) {
            Xpp3Dom configDom = (Xpp3Dom) configuration;
            Xpp3Dom propertiesLocationDom = configDom.getChild("propertiesLocation");
            if (propertiesLocationDom != null) {
                propertiesLocationDom.getValue();
            }
            Xpp3Dom propertyExpansion = configDom.getChild("propertyExpansion");
            if (propertyExpansion != null) {
                String keyValuePair = propertyExpansion.getValue();
                try {
                    properties.load(new StringInputStream(keyValuePair));
                } catch (IOException e) {
                    console.logError("Failed to parse checkstyle propertyExpansion as properties.");
                }
            }
        }
        return properties;
    }

    /**
     * @see http://maven.apache.org/plugins/maven-checkstyle-plugin/examples/suppressions-filter.html
     */
    private URL extractSupressionFilter(Plugin csPlugin) {
        Object configuration = csPlugin.getConfiguration();
        if (configuration instanceof Xpp3Dom) {
            Xpp3Dom configDom = (Xpp3Dom) configuration;
            Xpp3Dom suppressionsLocation = configDom.getChild("suppressionsLocation");
            if (suppressionsLocation != null) {
                suppressionsLocation.getValue();
            }
        }
        return null;
    }

    /**
     * Find (if exist) the maven-checkstyle-plugin configuration in the mavenProject
     */
    private Plugin getCheckstylePlugin(MavenProject mavenProject) {
        List<Plugin> plugins = mavenProject.getBuildPlugins();
        for (Plugin plugin : plugins) {
            if (CHECKSTYLE_PLUGIN_GROUPID.equals(plugin.getGroupId()) && CHECKSTYLE_PLUGIN_ARTIFACTID.equals(plugin.getArtifactId())) {
                return plugin;
            }
        }
        List<Plugin> reports = mavenProject.getReportPlugins();
        for (Plugin plugin : reports) {
            if (CHECKSTYLE_PLUGIN_GROUPID.equals(plugin.getGroupId()) && CHECKSTYLE_PLUGIN_ARTIFACTID.equals(plugin.getArtifactId())) {
                return plugin;
            }
        }
        return null;
    }
}
