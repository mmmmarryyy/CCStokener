package flattree.eclipse.ui.wizards;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import flattree.eclipse.FlatTreePlugin;
import flattree.eclipse.model.FlattreeProject;

/**
 * Wizard for creation of a FlatTree project.
 */
public class SampleProjectWizard extends Wizard implements INewWizard {

    private WizardNewProjectCreationPage page;

    /**
	 * Constructor for SampleProjectWizard.
	 */
    public SampleProjectWizard() {
        setNeedsProgressMonitor(true);
    }

    /**
	 * Adding the page to the wizard.
	 */
    public void addPages() {
        page = new WizardNewProjectCreationPage("FlatTree");
        page.setTitle("Project");
        page.setDescription("Create a FlatTree Sample project.");
        addPage(page);
    }

    public boolean performFinish() {
        IProject project = page.getProjectHandle();
        try {
            getContainer().run(true, true, new ProjectCreator(project));
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private class ProjectCreator extends WorkspaceModifyOperation {

        private IProject project;

        public ProjectCreator(IProject project) {
            this.project = project;
        }

        protected void execute(IProgressMonitor monitor) throws CoreException {
            project.create(monitor);
            project.open(monitor);
            copySamples(monitor);
            initConfigurations();
        }

        private void initConfigurations() throws CoreException {
            project.accept(new IResourceVisitor() {

                public boolean visit(IResource resource) throws CoreException {
                    if (resource.getName().endsWith(".flat")) {
                        String configuration = resource.getName().replace(".flat", ".flattree");
                        FlattreeProject.setConfiguration(resource, configuration);
                    }
                    return true;
                }
            });
        }

        private void copySamples(IProgressMonitor monitor) throws CoreException {
            try {
                Enumeration<URL> samples = FlatTreePlugin.getDefault().find("sample");
                while (samples.hasMoreElements()) {
                    URL sample = samples.nextElement();
                    String file = sample.getPath();
                    int slash = file.lastIndexOf('/');
                    if (slash != -1) {
                        file = file.substring(slash + 1);
                    }
                    if (file.isEmpty()) {
                        continue;
                    }
                    copy(sample, project.getFile(file), monitor);
                }
            } catch (IOException ex) {
                throw new CoreException(Status.CANCEL_STATUS);
            }
        }

        private void copy(URL url, IFile file, IProgressMonitor monitor) throws CoreException, IOException {
            InputStream input = null;
            try {
                input = url.openStream();
                if (file.exists()) {
                    file.setContents(input, IResource.FORCE, monitor);
                } else {
                    file.create(input, IResource.FORCE, monitor);
                }
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }
    }

    public void init(IWorkbench workbench, IStructuredSelection selection) {
    }
}
