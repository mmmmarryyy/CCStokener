package org.netbeans.module.flexbean.modules.project;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import org.netbeans.api.project.Project;
import org.netbeans.module.flexbean.modules.project.lookup.FlexProjectLookupAbstractFactory;
import org.netbeans.spi.project.AuxiliaryConfiguration;
import org.netbeans.spi.project.support.ant.AntProjectHelper;
import org.netbeans.spi.project.support.ant.PropertyEvaluator;
import org.netbeans.spi.project.support.ant.ReferenceHelper;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 * @author arnaud
 */
public final class FlexProjectBuilder {

    private AntProjectHelper _antProjectHelper;

    static class ProjectImpl implements Project {

        private AntProjectHelper _antProjectHelper;

        private Lookup _lookup = null;

        public FileObject getProjectDirectory() {
            return _antProjectHelper.getProjectDirectory();
        }

        public Lookup getLookup() {
            return _lookup;
        }

        void setAntProjectHelper(AntProjectHelper helper) {
            _antProjectHelper = helper;
        }

        void setLookup(Lookup lookup) {
            _lookup = lookup;
        }
    }

    public Project build() {
        if (_antProjectHelper == null) {
            throw new IllegalStateException("AntProjectHelper missing");
        }
        final ProjectImpl project = new ProjectImpl();
        project.setAntProjectHelper(_antProjectHelper);
        project.setLookup(_createLookup(project));
        try {
            _createBuildXml();
        } catch (Exception ex) {
        }
        return project;
    }

    public void setAntProjectHelper(AntProjectHelper helper) {
        if (helper == null) {
            throw new IllegalArgumentException("AntProjectHelper missing");
        }
        _antProjectHelper = helper;
    }

    private void _createBuildXml() throws IOException {
        final FileObject projectFolderFO = _antProjectHelper.getProjectDirectory();
        projectFolderFO.getFileSystem().runAtomicAction(new FileSystem.AtomicAction() {

            public void run() throws IOException {
                FileObject dest = projectFolderFO.getFileObject("build.xml");
                final URL url = AntProjectHelperBuilder.class.getResource("resources/build-impl.xml");
                InputStream in = url.openStream();
                OutputStream out = dest.getOutputStream();
                FileUtil.copy(in, out);
                in.close();
                out.close();
            }
        });
    }

    private Lookup _createLookup(Project project) {
        final AuxiliaryConfiguration aux = _antProjectHelper.createAuxiliaryConfiguration();
        final PropertyEvaluator evaluator = _antProjectHelper.getStandardPropertyEvaluator();
        final ReferenceHelper refHelper = new ReferenceHelper(_antProjectHelper, aux, evaluator);
        final FlexProjectLookupAbstractFactory factory = new FlexProjectLookupAbstractFactory();
        factory.setProject(project);
        final InstanceContent contents = new InstanceContent();
        contents.add(this);
        contents.add(_antProjectHelper);
        contents.add(aux);
        contents.add(refHelper);
        contents.add(factory.createProjectInformation());
        contents.add(factory.createPrivilegedTemplates());
        contents.add(factory.createSources());
        contents.add(factory.createFlexPlatformProvider());
        contents.add(factory.createCreateFromTemplateAttributesProvider());
        contents.add(factory.createDataFilesProviderImplementation());
        contents.add(factory.createFlexProjectDependenciesProvider());
        contents.add(factory.createCustomizerProvider());
        contents.add(factory.createActionProvider());
        contents.add(factory.createLogicalViewProvider());
        final Lookup newLookup = new AbstractLookup(contents);
        return newLookup;
    }
}
