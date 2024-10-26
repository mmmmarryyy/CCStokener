package net.sourceforge.wildlife.ui.wizards;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

/**
 * This is a sample new wizard. Its role is to create a new file 
 * resource in the provided container. If the container resource
 * (a folder or a project) is selected in the workspace 
 * when the wizard is opened, it will accept it as the target
 * container. The wizard creates one file with the extension
 * "env". If a sample multi-page editor (also available
 * as a template) is registered for the same extension, it will
 * be able to open it.
 */
public class NewWorldWizard extends BasicNewResourceWizard {

    /**
	 *
	 */
    private static final String DATA_WILDLIFE_ADMIN = "/data/wildlife.admin";

    private static final String DATA_WILDLIFE_CONF = "/data/wildlife.conf";

    private static final String DATA_WILDLIFE_POP = "/data/wildlife.pop";

    /**
	 *
	 */
    private NewWorldWizardPage _page;

    private ISelection _selection;

    /**
	 * Constructor for HabitatCreationWizard.
	 */
    public NewWorldWizard() {
    }

    /**
	 * Adding the page to the wizard.
	 */
    @Override
    public void addPages() {
        _page = new NewWorldWizardPage(_selection);
        addPage(_page);
    }

    /**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
    @Override
    public boolean performFinish() {
        final String containerName = _page.getContainerName();
        final String adminFileName = _page.getAdminFileName();
        final String confFileName = _page.getConfigurationFileName();
        final String popFileName = _page.getPopulationFileName();
        final String simulationName = _page.getSimulationName();
        IRunnableWithProgress op = new IRunnableWithProgress() {

            public void run(IProgressMonitor monitor) throws InvocationTargetException {
                try {
                    doFinish(containerName, DATA_WILDLIFE_ADMIN, adminFileName, simulationName, monitor);
                    doFinish(containerName, DATA_WILDLIFE_CONF, confFileName, simulationName, monitor);
                    doFinish(containerName, DATA_WILDLIFE_POP, popFileName, simulationName, monitor);
                } catch (CoreException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            }
        };
        try {
            getContainer().run(true, false, op);
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            Throwable realException = e.getTargetException();
            MessageDialog.openError(getShell(), "Error", realException.getMessage());
            return false;
        }
        return true;
    }

    /**
	 * The worker method. It will find the container, create the
	 * file if missing or just replace its contents, and open
	 * the editor on the newly created file.
	 */
    private void doFinish(String containerName, String sourceFileName, String destFileName, String simulationName_p, IProgressMonitor monitor) throws CoreException {
        monitor.beginTask("Creating " + destFileName, 2);
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IResource resource = root.findMember(new Path(containerName));
        if (!resource.exists() || !(resource instanceof IContainer)) {
            throwCoreException("Container \"" + containerName + "\" does not exist.");
        }
        IContainer container = (IContainer) resource;
        final IFile file = container.getFile(new Path(destFileName));
        try {
            URL url = WildLifeUIWizardsPlugin.class.getResource(sourceFileName);
            InputStream stream = url.openStream();
            if (file.exists()) {
                file.setContents(stream, true, true, monitor);
            } else {
                file.create(stream, true, monitor);
            }
            stream.close();
        } catch (IOException e) {
        }
        monitor.worked(1);
        monitor.setTaskName("Opening file for editing...");
        getShell().getDisplay().asyncExec(new Runnable() {

            public void run() {
                IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                try {
                    IDE.openEditor(page, file, true);
                } catch (PartInitException e) {
                }
            }
        });
        monitor.worked(1);
    }

    /**
	 *
	 */
    private void throwCoreException(String message) throws CoreException {
        IStatus status = new Status(IStatus.ERROR, WildLifeUIWizardsPlugin.PLUGIN_ID, IStatus.OK, message, null);
        throw new CoreException(status);
    }

    /**
	 * We will accept the selection in the workbench to see if we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection_p) {
        super.init(workbench, selection_p);
        _selection = selection_p;
        setNeedsProgressMonitor(true);
    }
}
