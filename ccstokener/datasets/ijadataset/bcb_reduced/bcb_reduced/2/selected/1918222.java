package net.sf.pacx.ui;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Enumeration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.osgi.framework.Bundle;

public class NewCorpusProjectOperation implements IRunnableWithProgress {

    private final IProject project;

    public NewCorpusProjectOperation(IProject project) {
        this.project = project;
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor.beginTask("Creating project structure and scripts...", 3);
            Bundle bundle = PacxUiPlugin.getDefault().getBundle();
            for (Enumeration entries = bundle.findEntries("template", "*", true); entries.hasMoreElements(); ) {
                URL url = (URL) entries.nextElement();
                String path = url.getPath();
                if (!path.startsWith("/template/")) throw new InvocationTargetException(new Throwable("Unknown template file: " + path));
                String targetPath = path.substring("/template".length());
                if (path.endsWith("/")) {
                    IFolder folder = project.getFolder(targetPath);
                    if (!folder.exists()) {
                        folder.create(false, true, null);
                    }
                } else {
                    InputStream in = url.openStream();
                    IFile file = project.getFile(targetPath);
                    if (file.exists()) {
                        file.delete(false, null);
                    }
                    file.create(in, true, null);
                }
            }
            monitor.worked(1);
            createLauncherFiles();
            createFile("bin/corpus.properties", "corpus.version=2\n" + "corpus.name=" + project.getName());
            monitor.worked(1);
            IProjectDescription description = project.getDescription();
            String[] natures = new String[] { "net.sf.vex.editor.pluginNature" };
            description.setNatureIds(natures);
            project.setDescription(description, null);
            monitor.worked(1);
        } catch (IOException e) {
            throw new InvocationTargetException(e);
        } catch (CoreException e) {
            throw new InvocationTargetException(e);
        } finally {
            monitor.done();
        }
    }

    private void createLauncherFiles() throws CoreException {
        String name = project.getName();
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\r\n" + "<launchConfiguration type=\"org.eclipse.ui.externaltools.ProgramLaunchConfigurationType\">\r\n" + "<stringAttribute key=\"org.eclipse.debug.core.ATTR_REFRESH_SCOPE\" value=\"${workspace}\"/>\r\n" + "<booleanAttribute key=\"org.eclipse.debug.core.appendEnvironmentVariables\" value=\"true\"/>\r\n" + "<listAttribute key=\"org.eclipse.debug.ui.favoriteGroups\">\r\n" + "<listEntry value=\"org.eclipse.ui.externaltools.launchGroup\"/>\r\n" + "</listAttribute>\r\n" + "<stringAttribute key=\"org.eclipse.ui.externaltools.ATTR_LOCATION\" value=\"${eclipse_home}\\plugins\\org.apache.ant_1.7.0.v200706080842\\bin\\ant.bat\"/>\r\n" + "<stringAttribute key=\"org.eclipse.ui.externaltools.ATTR_WORKING_DIRECTORY\" value=\"${workspace_loc:/" + name + "/bin}\"/>\r\n" + "</launchConfiguration>";
        createFile("bin/Build " + name + " Corpus.launch", content);
    }

    private void createFile(String filePathAndName, String content) throws CoreException {
        ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes());
        InputStream bufferedIn = new BufferedInputStream(in);
        IFile file = project.getFile(filePathAndName);
        if (file.exists()) {
            file.delete(true, null);
        }
        file.create(bufferedIn, false, null);
    }
}
