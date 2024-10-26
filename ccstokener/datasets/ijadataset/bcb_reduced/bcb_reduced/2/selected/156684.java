package org.netbeans.module.flexbean.modules.project;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.module.flexbean.modules.platform.FlexPlatform;
import org.netbeans.module.flexbean.modules.project.properties.FlexProjectPropertiesSupport;
import org.netbeans.spi.project.support.ant.AntProjectHelper;
import org.netbeans.spi.project.support.ant.EditableProperties;
import org.netbeans.spi.project.support.ant.GeneratedFilesHelper;
import org.netbeans.spi.project.support.ant.ProjectGenerator;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.Repository;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author arnaud
 */
public final class FlexProjectGenerator {

    public static AntProjectHelper createFlexComponentProject(final String name, final File projectFolder, final FlexPlatform flexSdk) throws IOException {
        final FlexProjectGenerator flexProjectGenerator = new FlexProjectGenerator();
        return flexProjectGenerator.createFlexProject(name, projectFolder, flexSdk, null);
    }

    public static AntProjectHelper createFlexApplicationProject(final String name, final File projectFolder, final FlexPlatform flexSdk, final String mainClass) throws IOException {
        final FlexProjectGenerator flexProjectGenerator = new FlexProjectGenerator();
        return flexProjectGenerator.createFlexProject(name, projectFolder, flexSdk, mainClass);
    }

    private AntProjectHelper createFlexProject(final String name, final File projectFolder, final FlexPlatform flexSdk, final String mainClass) throws IOException {
        final AntProjectHelper[] antProjectHelper = new AntProjectHelper[1];
        final FileObject projectFolderFO = FileUtil.createFolder(projectFolder);
        projectFolderFO.getFileSystem().runAtomicAction(new FileSystem.AtomicAction() {

            public void run() throws IOException {
                antProjectHelper[0] = createProject(name, projectFolderFO, "src", "build", flexSdk, mainClass);
                final FlexProject flexProject = (FlexProject) ProjectManager.getDefault().findProject(projectFolderFO);
                ProjectManager.getDefault().saveProject(flexProject);
                final FileObject srcFolderFO = projectFolderFO.createFolder("src");
                FileObject dest = projectFolderFO.createData(GeneratedFilesHelper.BUILD_XML_PATH);
                final URL url = FlexProjectGenerator.class.getResource("resources/build-impl.xml");
                InputStream in = url.openStream();
                OutputStream out = dest.getOutputStream();
                FileUtil.copy(in, out);
                in.close();
                out.close();
                if (mainClass != null) {
                    createMainClass(srcFolderFO, mainClass);
                }
                return;
            }
        });
        return antProjectHelper[0];
    }

    private void createMainClass(FileObject sourcesFolder, String main) throws IOException {
        FileObject mainTemplate = Repository.getDefault().getDefaultFileSystem().findResource("Templates/Flex/Privileged/File.mxml");
        DataObject t = DataObject.find(mainTemplate);
        DataFolder folder = DataFolder.findFolder(sourcesFolder);
        t.createFromTemplate(folder, main);
    }

    private AntProjectHelper createProject(String name, FileObject projectFolderFO, String srcDirName, String buildDirName, FlexPlatform flexSdk, String mainClass) throws IOException {
        final AntProjectHelper antProjectHelper = ProjectGenerator.createProject(projectFolderFO, FlexProjectType.FLEXPROJECTTYPE);
        Element data = antProjectHelper.getPrimaryConfigurationData(true);
        Document doc = data.getOwnerDocument();
        final EditableProperties antProperties = FlexProjectPropertiesSupport.create(antProjectHelper);
        final Element nameEl = doc.createElementNS(FlexProjectType.PROJECT_CONFIGURATION_NAMESPACE, "name");
        nameEl.appendChild(doc.createTextNode(name));
        data.appendChild(nameEl);
        final Element srcEl = doc.createElementNS(FlexProjectType.PROJECT_CONFIGURATION_NAMESPACE, "source");
        srcEl.appendChild(doc.createTextNode(srcDirName));
        antProperties.setProperty(FlexProjectProperties.PROJECT_SRC_PATH, srcDirName);
        data.appendChild(srcEl);
        antProjectHelper.putPrimaryConfigurationData(data, true);
        antProperties.setProperty(FlexProjectProperties.PROJECT_BUILD_PATH, buildDirName);
        antProperties.setProperty(FlexProjectProperties.FLEX_SDK_REF, flexSdk.getName());
        antProperties.setProperty(FlexProjectProperties.PROJECT_ANTBUILD_FILE, "build.xml");
        antProperties.setProperty(FlexProjectProperties.PROJECT_NAME, name);
        if (mainClass != null) {
            antProperties.setProperty(FlexProjectProperties.PROJECT_TYPE, FlexProjectProperties.PROJECT_TYPE_APPLICATION_VALUE);
            antProperties.setProperty(FlexProjectProperties.PROJECT_MAINCLASS, mainClass);
        } else {
            antProperties.setProperty(FlexProjectProperties.PROJECT_TYPE, FlexProjectProperties.PROJECT_TYPE_COMPONENT_VALUE);
        }
        antProjectHelper.putProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH, antProperties);
        return antProjectHelper;
    }
}
