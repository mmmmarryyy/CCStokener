package de.cenit.eb.sm.tools.eclipse.projectfromtemplate.wizards;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.regex.Pattern;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import de.cenit.eb.sm.tools.eclipse.projectfromtemplate.data.Template;
import de.cenit.eb.sm.tools.eclipse.projectfromtemplate.data.Variable;

/**
 * This example shows how to implement an own project wizard that uses the
 * JavaCapabilityConfigurationPage to allow the user to configure the Java build path.
 */
public class ProjectFromTemplateWizard extends Wizard implements IExecutableExtension, INewWizard {

    /** wizard standard main page */
    private WizardNewProjectCreationPage projectPage;

    /**
    * wizard page for project template data
    * @clientCardinality 1
    * @supplierCardinality 1
    */
    private WizardPage1 mainPage;

    /** standard Java build path configuration page  */
    private JavaCapabilityConfigurationPage javaPage;

    /** plugin configuration */
    private IConfigurationElement configElement;

    /** current workbench */
    private IWorkbench workbench;

    /** current object selection */
    private IStructuredSelection selection;

    /**
    * Creates a new ProjectFromTemplateWizard object.
    */
    public ProjectFromTemplateWizard() {
        setWindowTitle("Project From Template");
    }

    /**
    * Set configuration during start of this plugin.
    *
    * @param cfig [in] configuration element
    * @param propertyName [in] attribute name
    * @param data [in] adapter data
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
    */
    public void setInitializationData(final IConfigurationElement cfig, final String propertyName, final Object data) {
        setConfigElement(cfig);
    }

    /**
    * Initialize workbench.
    *
    * @param workbench [in] current workbench
    * @param selection [in] current object selection
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
    */
    public void init(final IWorkbench workbench, final IStructuredSelection selection) {
        setWorkbench(workbench);
        setSelection(selection);
    }

    /**
    * Add required pages to wizard.
    *
    * <p>The wizard has three pages:
    * <ul>
    * <li> the standard "new project" page where the project location can be selected </li>
    * <li> the page where the template specific fields are shown </li>
    * <li> the standard "Java build path configuration" page </li>
    * </ul></p>
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * @see Wizard#addPages
    */
    @Override
    public void addPages() {
        super.addPages();
        setProjectPage(new WizardNewProjectCreationPage("ProjectFromTemplateWizard"));
        getProjectPage().setTitle("Project from CENIT Template");
        getProjectPage().setDescription("Create a new project from a template.");
        addPage(getProjectPage());
        setMainPage(new WizardPage1() {

            @Override
            public void setVisible(final boolean visible) {
                updatePage();
                super.setVisible(visible);
            }
        });
        addPage(getMainPage());
        setJavaPage(new JavaCapabilityConfigurationPage() {

            @Override
            public void setVisible(final boolean visible) {
                updatePage();
                super.setVisible(visible);
            }
        });
        getMainPage().setNextPage(getJavaPage());
        addPage(getJavaPage());
    }

    /**
    * Add buildpath entries from selected template to classpath.
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * <!-- add optional tags @version, @see, @since, @deprecated here -->
    */
    protected void updatePage() {
        final IJavaProject jproject = JavaCore.create(getProjectPage().getProjectHandle());
        IClasspathEntry[] buildPath = { JavaCore.newSourceEntry(jproject.getPath().append("src")), JavaRuntime.getDefaultJREContainerEntry() };
        final Template templ = getMainPage().getSelectedTempl();
        if (templ != null) {
            final List<Variable> varList = templ.getVariables();
            if (0 != templ.getCopyOfVarBuildPaths().length) {
                final int cpLength = buildPath.length + templ.getCopyOfVarBuildPaths().length + templ.getCopyOfConBuildPaths().length;
                final IClasspathEntry[] newClasspaths = new IClasspathEntry[cpLength];
                int offset = 0;
                for (int i = 0; i < buildPath.length; i++) {
                    newClasspaths[i] = buildPath[i];
                }
                offset += buildPath.length;
                for (int i = 0; i < templ.getCopyOfVarBuildPaths().length; i++) {
                    String newEntry = templ.getCopyOfVarBuildPaths()[i];
                    for (final Variable var : varList) {
                        newEntry = newEntry.replaceAll(Pattern.quote("$" + var.getKey() + "$"), var.getValue());
                    }
                    final IClasspathEntry newClassPathEntry = JavaCore.newVariableEntry(new Path(newEntry), null, null);
                    newClasspaths[i + offset] = newClassPathEntry;
                }
                offset += templ.getCopyOfVarBuildPaths().length;
                for (int i = 0; i < templ.getCopyOfConBuildPaths().length; i++) {
                    String newEntry = templ.getCopyOfConBuildPaths()[i];
                    for (final Variable var : varList) {
                        newEntry = newEntry.replaceAll(Pattern.quote("$" + var.getKey() + "$"), var.getValue());
                    }
                    newClasspaths[i + offset] = JavaCore.newContainerEntry(new Path(newEntry));
                }
                buildPath = newClasspaths;
            }
        }
        final IPath outputLocation = jproject.getPath().append("classes");
        getJavaPage().init(jproject, outputLocation, buildPath, false);
    }

    /**
    * Create the new project according to the settings.
    *
    * <p>All files referenced in the project template will be created.</p>
    *
    * @param monitor [in] progress monitor provided by workbench
    * @throws InterruptedException if processing is interrupted
    * @throws CoreException if an error occurs during project creation
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * <!-- add optional tags @version, @see, @since, @deprecated here -->
    */
    protected void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor.beginTask("Creating project...", 3);
            final IProject project = getProjectPage().getProjectHandle();
            final IPath locationPath = getProjectPage().getLocationPath();
            final IProjectDescription desc = project.getWorkspace().newProjectDescription(project.getName());
            if (!getProjectPage().useDefaults()) {
                desc.setLocation(locationPath);
            }
            project.create(desc, new SubProgressMonitor(monitor, 1));
            project.open(new SubProgressMonitor(monitor, 1));
            updatePage();
            getJavaPage().configureJavaProject(new SubProgressMonitor(monitor, 1));
            final Template templ = getMainPage().getSelectedTempl();
            try {
                final IProjectDescription description = project.getDescription();
                final String[] natures = description.getNatureIds();
                final String[] naturesAdd = templ.getCopyOfNatures();
                final String[] newNatures = new String[natures.length + naturesAdd.length];
                System.arraycopy(natures, 0, newNatures, 0, natures.length);
                System.arraycopy(naturesAdd, 0, newNatures, natures.length, naturesAdd.length);
                final IStatus status = project.getWorkspace().validateNatureSet(natures);
                if (status.getCode() == IStatus.OK) {
                    description.setNatureIds(newNatures);
                    project.setDescription(description, null);
                } else {
                    System.out.println("error validating nature set");
                }
            } catch (final CoreException e) {
            }
            for (final String key : templ.getTextFiles().keySet()) {
                createFile(project, monitor, templ, templ.getTextFiles().get(key), key, false);
            }
            for (final String key : templ.getBinFiles().keySet()) {
                createFile(project, monitor, templ, templ.getBinFiles().get(key), key, true);
            }
            BasicNewProjectResourceWizard.updatePerspective(getConfigElement());
            BasicNewResourceWizard.selectAndReveal(project, getWorkbench().getActiveWorkbenchWindow());
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            monitor.done();
        }
    }

    /**
    * Called if the "Finish" button has been pressed.
    *
    * <p>The new project will be created.</p>
    *
    * @return [boolean] true if project creation was successful
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * @see Wizard#performFinish
    */
    @Override
    public boolean performFinish() {
        getMainPage().setMapFromTable();
        final WorkspaceModifyOperation op = new WorkspaceModifyOperation() {

            @Override
            protected void execute(final IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
                finishPage(monitor);
            }
        };
        try {
            getContainer().run(false, true, op);
        } catch (final InvocationTargetException e) {
            final MessageBox errDialog = new MessageBox(this.workbench.getDisplay().getActiveShell(), SWT.OK | SWT.ERROR);
            errDialog.setText("Error: Failed to create project from template.");
            errDialog.setMessage("Failed to create the project.\n" + "InvocationException message:\n" + e.getLocalizedMessage());
            return false;
        } catch (final InterruptedException e) {
            return false;
        }
        return true;
    }

    /**
    * Create the given file.
    *
    * <p>This method is called for all files referenced in the project template.</p>
    *
    * @param project [in] project where file must be created
    * @param monitor [in] progress monitor provided by workbench
    * @param templ [in] template that was selected for project
    * @param sourceUrl [in] source URL where the template files reside
    * @param destFile [in] destination file name in project
    * @param isBinary [in] if true, the file will be created from the template file without changes;
    *                      if false, variables in the template file will be replaced
    * @return [boolean] true if file could be created
    * @throws IOException if file could not be created
    * @throws CoreException if an error occurs during file creation
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * <!-- add optional tags @version, @see, @since, @deprecated here -->
    */
    protected boolean createFile(final IProject project, final IProgressMonitor monitor, final Template templ, final String sourceUrl, final String destFile, final boolean isBinary) throws IOException, CoreException {
        URL url;
        url = new URL(sourceUrl);
        final URLConnection con = url.openConnection();
        final IFile f = project.getFile(replaceVariables(templ.getVariables(), destFile));
        createParents(f, monitor);
        if (isBinary) {
            f.create(con.getInputStream(), true, monitor);
        } else {
            final StringWriter sw = new StringWriter();
            final InputStream in = con.getInputStream();
            for (; ; ) {
                final int c = in.read();
                if (-1 == c) {
                    break;
                }
                sw.write(c);
            }
            sw.close();
            final String fileText = replaceVariables(templ.getVariables(), sw.toString());
            f.create(new ByteArrayInputStream(fileText.getBytes()), true, monitor);
        }
        return true;
    }

    /** Creates the parent directories required for the given file.
    *
    * @param f [in] file to create
    * @param monitor [in] progress monitor provided by workbench
    * @throws CoreException if creation of parent folders fails
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * <!-- add optional tags @version, @see, @since, @deprecated here -->
    */
    protected void createParents(final IFile f, final IProgressMonitor monitor) throws CoreException {
        final IContainer dir = f.getParent();
        if (!dir.exists()) {
            if (dir instanceof IFolder) {
                final IFolder folder = (IFolder) dir;
                createParents(folder, monitor);
            }
        }
    }

    /** Creates the parent directories required for the given folder.
   *
   * @param f [in] folder to create
   * @param monitor [in] progress monitor provided by workbench
   * @throws CoreException if creation of parent folders fails
   *
   * @.author matysiak
   *
   * @.threadsafe no
   *
   * <!-- add optional tags @version, @see, @since, @deprecated here -->
   */
    protected void createParents(final IFolder f, final IProgressMonitor monitor) throws CoreException {
        if (!f.exists()) {
            final IContainer parent = f.getParent();
            if (parent instanceof IFolder) {
                createParents((IFolder) parent, monitor);
            }
            f.create(true, true, monitor);
        }
    }

    /**
    * Replace variables in the template file names during project creation.
    *
    * The variables must be embedded in "$".
    *
    * @param varList [in] list of variables used for replacement
    * @param input [in] filename where variables must be replaced
    * @return [String] filename where variables have been replaced.
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * <!-- add optional tags @version, @see, @since, @deprecated here -->
    */
    protected String replaceVariables(final List<Variable> varList, final String input) {
        String res = input;
        for (final Variable var : varList) {
            res = res.replaceAll(Pattern.quote("$" + var.getKey() + "$"), var.getValue());
        }
        return res;
    }

    /**
    * Sets javaPage.
    *
    * @param javaPage [in] The javaPage to set.
    */
    protected void setJavaPage(final JavaCapabilityConfigurationPage javaPage) {
        this.javaPage = javaPage;
    }

    /**
    * Getter for javaPage.
    *
    * @return [JavaCapabilityConfigurationPage] Returns the javaPage.
    */
    protected JavaCapabilityConfigurationPage getJavaPage() {
        return this.javaPage;
    }

    /**
    * Sets projectPage.
    *
    * @param projectPage [in] The projectPage to set.
    */
    protected void setProjectPage(final WizardNewProjectCreationPage projectPage) {
        this.projectPage = projectPage;
    }

    /**
    * Getter for projectPage.
    *
    * @return [WizardNewProjectCreationPage] Returns the projectPage.
    */
    protected WizardNewProjectCreationPage getProjectPage() {
        return this.projectPage;
    }

    /**
    * Sets mainPage.
    *
    * @param mainPage [in] The mainPage to set.
    */
    protected void setMainPage(final WizardPage1 mainPage) {
        this.mainPage = mainPage;
    }

    /**
    * Getter for mainPage.
    *
    * @return [WizardPage1] Returns the mainPage.
    */
    protected WizardPage1 getMainPage() {
        return this.mainPage;
    }

    /**
    * Sets workbench.
    *
    * @param workbench [in] The workbench to set.
    */
    protected void setWorkbench(final IWorkbench workbench) {
        this.workbench = workbench;
    }

    /**
    * Getter for workbench.
    *
    * @return [IWorkbench] Returns the workbench.
    */
    protected IWorkbench getWorkbench() {
        return this.workbench;
    }

    /**
    * Sets selection.
    *
    * @param selection [in] The selection to set.
    */
    protected void setSelection(final IStructuredSelection selection) {
        this.selection = selection;
    }

    /**
    * Getter for selection.
    *
    * @return [IStructuredSelection] Returns the selection.
    */
    protected IStructuredSelection getSelection() {
        return this.selection;
    }

    /**
    * Sets configElement.
    *
    * @param configElement [in] The configElement to set.
    */
    protected void setConfigElement(final IConfigurationElement configElement) {
        this.configElement = configElement;
    }

    /**
    * Getter for configElement.
    *
    * @return [IConfigurationElement] Returns the configElement.
    */
    protected IConfigurationElement getConfigElement() {
        return this.configElement;
    }
}
