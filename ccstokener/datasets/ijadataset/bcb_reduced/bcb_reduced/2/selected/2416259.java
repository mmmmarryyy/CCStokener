package net.sf.xqz.rcp;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

    private static final String PERSPECTIVE_ID = "net.sf.xqz.rcp.perspective";

    public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        return new ApplicationWorkbenchWindowAdvisor(configurer);
    }

    public String getInitialWindowPerspectiveId() {
        return PERSPECTIVE_ID;
    }

    @Override
    public void initialize(IWorkbenchConfigurer configurer) {
        super.initialize(configurer);
        try {
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            IWorkspaceRoot wsroot = workspace.getRoot();
            IProject p = wsroot.getProject("DefaultXQZProject");
            IProjectDescription desc = workspace.newProjectDescription(p.getName());
            if (!p.exists()) {
                p.create(desc, null);
            }
            if (!p.isOpen()) {
                p.open(null);
            }
            Path dataDirPath = new Path("data");
            if (!p.getFolder(dataDirPath).exists()) {
                p.getFolder(dataDirPath).create(true, true, new NullProgressMonitor());
            }
            Enumeration<URL> genModelFiles = Activator.getDefault().getBundle().findEntries("data", "*.*", true);
            while (genModelFiles.hasMoreElements()) {
                URL urlGenModelFile = genModelFiles.nextElement();
                if (!urlGenModelFile.getFile().contains(".svn")) {
                    Path genmodelFilePath = new Path(urlGenModelFile.getPath());
                    if (!p.getFile(genmodelFilePath).exists()) {
                        p.getFile(genmodelFilePath).create(FileLocator.resolve(urlGenModelFile).openStream(), true, new NullProgressMonitor());
                    }
                }
            }
            p.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
        } catch (CoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
