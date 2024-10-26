package net.sf.escripts.examples.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import net.sf.escripts.EscriptsPlugin;
import net.sf.escripts.utilities.BufferedReaderInputStream;
import net.sf.escripts.utilities.PreferenceUtilities;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

/**
* The class {@link EscriptsExamplesProjectWizard} provides a simple wizard for creating a demo
* project that contains some Escripts example scripts.
*
* @author raner
* @version $Revision: 50 $
**/
public class EscriptsExamplesProjectWizard extends BasicNewResourceWizard implements IRunnableWithProgress {

    private static final String DEFAULT_PROJECT_NAME = "Escripts Examples";

    private static final String NAME_AND_LOCATION_PAGE = "nameAndLocationPage";

    private static final String EXAMPLES = "examples/";

    private static final String MANIFEST = ".manifest";

    private static final String[][] MATCH_AND_REPLACE = { { "^<!-- .*-->", "" }, { DEFAULT_PROJECT_NAME, DEFAULT_PROJECT_NAME } };

    private WizardNewProjectCreationPage page;

    private IProjectDescription description;

    private IProject project;

    /**
    * Creates a new {@link EscriptsExamplesProjectWizard}.
    *
    * @author raner
    **/
    public EscriptsExamplesProjectWizard() {
        super();
    }

    /**
    * Initializes the wizard using the specified workbench and object selection. This method will
    * also enable the progress monitor and set the window title.
    *
    * @param workbench the {@link IWorkbench}
    * @param currentSelection the current selection
    * @see BasicNewResourceWizard#init(IWorkbench, IStructuredSelection)
    *
    * @author raner
    **/
    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        super.init(workbench, currentSelection);
        setNeedsProgressMonitor(true);
        setWindowTitle(EscriptsExamplesProjectWizardMessages.windowTitle);
    }

    /**
    * Adds a single {@link WizardNewProjectCreationPage} that contains the project name and
    * location.
    *
    * @see org.eclipse.jface.wizard.IWizard#addPages()
    *
    * @author raner
    **/
    public void addPages() {
        super.addPages();
        page = new WizardNewProjectCreationPage(NAME_AND_LOCATION_PAGE);
        page.setTitle(EscriptsExamplesProjectWizardMessages.pageTitle);
        page.setDescription(EscriptsExamplesProjectWizardMessages.pageDescription);
        page.setInitialProjectName(getDefaultProjectName());
        this.addPage(page);
    }

    /**
    * Performs any actions appropriate in response to the user having pressed the Finish button, or
    * refuses if finishing is currently not permitted. This method will create the Escripts example
    * project.
    *
    * @return <code>true</code> to indicate the finish request was accepted, and <code>false</code>
    * to indicate that the finish request was refused
    * @see org.eclipse.jface.wizard.IWizard#performFinish()
    *
    * @author raner
    **/
    public boolean performFinish() {
        createExampleProject();
        if (project == null) {
            return false;
        }
        selectAndReveal(project.getFile("CreateNewJavaProject.escript"));
        return true;
    }

    /**
    * Provides the body of the {@link WorkspaceModifyOperation} that creates the example project.
    *
    * @param monitor
    * @throws InvocationTargetException
    * @throws InterruptedException
    * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
    *
    * @author mirko
    **/
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        try {
            createProject(monitor);
        } catch (CoreException exception) {
            throw new InvocationTargetException(exception);
        }
    }

    private IProject createExampleProject() {
        if (project != null) {
            return project;
        }
        project = page.getProjectHandle();
        IPath path = page.useDefaults() ? null : page.getLocationPath();
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        description = workspace.newProjectDescription(project.getName());
        description.setLocation(path);
        WorkspaceModifyOperation workspaceOperation = new WorkspaceModifyDelegatingOperation(this);
        try {
            getContainer().run(true, true, workspaceOperation);
        } catch (InterruptedException interrupt) {
            return project = null;
        } catch (InvocationTargetException exception) {
            final String ERROR = EscriptsExamplesProjectWizardMessages.errorTitle;
            Throwable target = exception.getTargetException();
            if (target instanceof CoreException) {
                ErrorDialog.openError(getShell(), ERROR, null, ((CoreException) target).getStatus());
            } else {
                final String INTERNAL_ERROR = EscriptsExamplesProjectWizardMessages.internalError;
                String detailMessage = NLS.bind(INTERNAL_ERROR, target.getMessage());
                MessageDialog.openError(getShell(), ERROR, detailMessage);
            }
            return project = null;
        }
        return project;
    }

    private void createProject(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
        final int TICKS = 1000;
        try {
            project.create(description, new SubProgressMonitor(monitor, TICKS));
            if (monitor.isCanceled()) {
                throw new OperationCanceledException();
            }
            project.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, TICKS));
            URL manifest = EscriptsPlugin.getURL(EXAMPLES + MANIFEST);
            BufferedReader listOfFilenames = getBufferedReader(manifest);
            String exampleFilename = null;
            while ((exampleFilename = listOfFilenames.readLine()) != null) {
                if (exampleFilename.startsWith("//")) {
                    continue;
                }
                URL exampleTemplate = EscriptsPlugin.getURL(EXAMPLES + exampleFilename);
                IFile exampleFile = project.getFile(exampleFilename);
                BufferedReader content = getBufferedReader(exampleTemplate);
                final String ENCODING = PreferenceUtilities.getFileEncoding(project);
                final String LS = PreferenceUtilities.getLineSeparator(project);
                InputStream data;
                synchronized (MATCH_AND_REPLACE) {
                    MATCH_AND_REPLACE[1][1] = project.getName();
                    data = new BufferedReaderInputStream(content, ENCODING, LS, MATCH_AND_REPLACE);
                }
                exampleFile.create(data, true, null);
                data.close();
            }
            listOfFilenames.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        } finally {
            monitor.done();
        }
    }

    private static String getDefaultProjectName() {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        String projectName = DEFAULT_PROJECT_NAME;
        int suffix = 1;
        while (workspaceRoot.getProject(projectName).exists()) {
            projectName = DEFAULT_PROJECT_NAME + " (" + (++suffix) + ')';
        }
        return projectName;
    }

    private static BufferedReader getBufferedReader(URL url) throws IOException {
        InputStream stream = url.openStream();
        InputStreamReader reader = new InputStreamReader(stream);
        return new BufferedReader(reader);
    }
}
