package org.netbeans.modules.flexbean.project.module.core.builders.antprojecthelper;

import org.netbeans.modules.flexbean.project.module.core.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.modules.flexbean.platform.api.Platform;
import org.netbeans.modules.flexbean.platform.api.PlatformManager;
import org.netbeans.modules.flexbean.platform.api.Specification;
import org.netbeans.modules.flexbean.platform.api.SpecificationVersion;
import org.netbeans.modules.flexbean.project.api.FlexProjectAuxiliaryConfiguration;
import org.netbeans.modules.flexbean.project.api.FlexProjectPropertiesConfiguration;
import org.netbeans.spi.project.support.ant.AntProjectHelper;
import org.netbeans.spi.project.support.ant.GeneratedFilesHelper;
import org.netbeans.spi.project.support.ant.ProjectGenerator;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.util.Mutex;
import org.openide.util.MutexException;

/**
 *
 * @author arnaud
 */
public abstract class AntProjectHelperBaseBuilder implements AntProjectHelperBuilder {

    private String projectLocation;

    private String projectName;

    private String sdkDisplayName;

    private FileObject projectFolder;

    private AntProjectHelper helper;

    private final Map<String, String> type2Templates = new HashMap<String, String>(2);

    protected AntProjectHelperBaseBuilder() {
        type2Templates.put("mxml", "Templates/Flex/APPLICATIONMXML.mxml");
        type2Templates.put("as", "Templates/Flex/APPLICATIONAS.as");
    }

    protected String getTemplateValue(String templateName) {
        return type2Templates.get(templateName);
    }

    public String getProjectLocation() {
        return projectLocation;
    }

    public void setProjectLocation(String projectLocation) {
        this.projectLocation = projectLocation;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getSdkDisplayName() {
        return sdkDisplayName;
    }

    public void setSdkDisplayName(String sdkDisplayName) {
        this.sdkDisplayName = sdkDisplayName;
    }

    @Override
    public boolean isValid() {
        boolean result = (projectName != null && !projectName.isEmpty()) && (projectLocation != null && !projectLocation.isEmpty()) && (sdkDisplayName != null && !sdkDisplayName.isEmpty());
        return result;
    }

    protected abstract void initProperties(Project project, FlexProjectPropertiesConfiguration properties);

    protected void createBuildXmlFile(Project project) throws IOException {
        FileObject dest = project.getProjectDirectory().createData(GeneratedFilesHelper.BUILD_XML_PATH);
        final URL url = ProjectType.class.getResource("/org/netbeans/modules/flexbean/project/module/ant/build-impl.xml");
        InputStream in = url.openStream();
        OutputStream out = dest.getOutputStream();
        FileUtil.copy(in, out);
        in.close();
        out.close();
    }

    @Override
    public AntProjectHelper create() throws IOException {
        AntProjectHelper result = null;
        final Mutex mutex = ProjectManager.mutex();
        try {
            result = mutex.writeAccess(new Mutex.ExceptionAction<AntProjectHelper>() {

                @Override
                public AntProjectHelper run() throws IOException {
                    AntProjectHelper result = getAntProjectHelper();
                    final Project project = ProjectManager.getDefault().findProject(projectFolder);
                    result = project.getLookup().lookup(AntProjectHelper.class);
                    FlexProjectPropertiesConfiguration propsConf = project.getLookup().lookup(FlexProjectPropertiesConfiguration.class);
                    Platform platform = PlatformManager.getDefault().getPlatforms(sdkDisplayName, null)[0];
                    propsConf.setPlatformActive(platform);
                    propsConf.setSourceDir("src");
                    propsConf.setBuildDir("build");
                    propsConf.setTestSourceDir("test");
                    Specification spec = platform.getSpecification();
                    SpecificationVersion version = spec.getVersion();
                    propsConf.setSourceCompatibility(version);
                    try {
                        propsConf.save();
                    } catch (Exception ex) {
                        throw new IOException(ex);
                    }
                    initProperties(project, propsConf);
                    createBuildXmlFile(project);
                    try {
                        propsConf.save();
                    } catch (Exception ex) {
                        throw new IOException(ex);
                    }
                    FlexProjectAuxiliaryConfiguration conf = project.getLookup().lookup(FlexProjectAuxiliaryConfiguration.class);
                    conf.setDisplayProjectName(projectName);
                    conf.save();
                    ProjectManager.getDefault().saveProject(project);
                    return result;
                }
            });
        } catch (MutexException ex) {
        }
        return result;
    }

    protected FileObject getProjectFolder() throws IOException {
        if (projectFolder == null) {
            File projectLocationDir = new File(projectLocation, projectName);
            projectFolder = FileUtil.createFolder(projectLocationDir);
            projectFolder.createFolder("src");
            projectFolder.createFolder("test");
            projectFolder.createFolder("build");
        }
        return projectFolder;
    }

    protected AntProjectHelper getAntProjectHelper() throws IOException {
        if (helper == null) {
            final FileObject localProjectFolder = getProjectFolder();
            localProjectFolder.getFileSystem().runAtomicAction(new FileSystem.AtomicAction() {

                @Override
                public void run() throws IOException {
                    helper = ProjectGenerator.createProject(localProjectFolder, ProjectType.TYPE);
                }
            });
        }
        return helper;
    }
}
