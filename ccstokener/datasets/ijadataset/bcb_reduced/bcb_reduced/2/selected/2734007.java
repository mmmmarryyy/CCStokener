package org.abettor.leaf4e.wizards;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.abettor.leaf4e.Activator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.osgi.framework.Bundle;

/**
 * 创建LEAF工程向导
 * @author shawn
 *
 */
public class LeafProjectNewWizard extends Wizard implements INewWizard {

    private static final String WIZARD_NAME = "LEAF Project Wizard";

    private final Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);

    private ILog logger = Activator.getDefault().getLog();

    private ISelection selection;

    private LeafProjectNewWizardPage page;

    private String projectName;

    private String runtimeName;

    private String dbDriver;

    private String dbUrl;

    private String dbUser;

    private String dbPass;

    private String dbDialect;

    private String logPath;

    private List<String> skipList = new ArrayList<String>();

    public LeafProjectNewWizard() {
        super();
        setWindowTitle(WIZARD_NAME);
        setNeedsProgressMonitor(true);
        skipList.add(".svn");
        skipList.add("vssver.scc");
    }

    public void addPages() {
        page = new LeafProjectNewWizardPage(selection);
        super.addPage(page);
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.selection = selection;
    }

    @Override
    public boolean performFinish() {
        projectName = page.getProject();
        runtimeName = page.getRuntime();
        dbDriver = page.getDbDriver();
        dbUrl = page.getDbUrl();
        dbUser = page.getDbUser();
        dbPass = page.getDbPassword();
        dbDialect = page.getDbDialect();
        logPath = page.getLogPath();
        IRunnableWithProgress op = new IRunnableWithProgress() {

            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                try {
                    doFinish(monitor);
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            }
        };
        try {
            getContainer().run(true, false, op);
        } catch (InvocationTargetException e) {
            Throwable realException = e.getTargetException();
            Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Generate LEAF project error", realException);
            logger.log(status);
            MessageDialog.openError(getShell(), "Error", realException.getMessage());
            return false;
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }

    private void doFinish(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException, CoreException, IOException, URISyntaxException {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = root.getProject(projectName);
        if (project.exists()) {
            String msg = "Project \"" + projectName + "\" has already existed.";
            Exception e = new Exception(msg);
            throw new InvocationTargetException(e, msg);
        }
        project.create(monitor);
        if (!project.isOpen()) {
            project.open(monitor);
        }
        resourceCopy("resources/" + Activator.PLUGIN_CONF, project, Activator.PLUGIN_CONF, monitor);
        Properties prop = new Properties();
        InputStream is = null;
        try {
            IFile conf = project.getFile(Activator.PLUGIN_CONF);
            is = conf.getContents();
            prop.load(is);
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        } finally {
            if (is != null) {
                is.close();
            }
        }
        IFolder folder = project.getFolder(prop.getProperty("SRC"));
        if (!folder.exists()) {
            folder.create(true, true, monitor);
        }
        folder = project.getFolder(prop.getProperty("WEB_ROOT"));
        if (!folder.exists()) {
            folder.create(true, true, monitor);
        }
        folder = project.getFolder(prop.getProperty("WEB_ROOT") + "/META-INF");
        if (!folder.exists()) {
            folder.create(true, true, monitor);
        }
        folder = project.getFolder(prop.getProperty("WEB_ROOT") + "/WEB-INF");
        if (!folder.exists()) {
            folder.create(true, true, monitor);
        }
        folder = project.getFolder(prop.getProperty("WEB_ROOT") + "/" + prop.getProperty("CONTEXT_FOLDER"));
        if (!folder.exists()) {
            folder.create(true, true, monitor);
        }
        folder = project.getFolder(prop.getProperty("WEB_ROOT") + "/WEB-INF/classes");
        if (!folder.exists()) {
            folder.create(true, true, monitor);
        }
        folder = project.getFolder(prop.getProperty("WEB_ROOT") + "/WEB-INF/lib");
        if (!folder.exists()) {
            folder.create(true, true, monitor);
        }
        folder = project.getFolder(".settings");
        if (!folder.exists()) {
            folder.create(true, true, monitor);
        }
        resourceDirectoryCopy("resources/src", project, prop.getProperty("SRC"), monitor);
        resourceCopy("resources/src/log4j.properties", project, prop.getProperty("SRC") + "/log4j.properties", monitor, "\\{LogFile\\}", logPath, "UTF-8");
        resourceDirectoryCopy("resources/WebContent/META-INF", project, prop.getProperty("WEB_ROOT") + "/META-INF", monitor);
        resourceDirectoryCopy("resources/WebContent/WEB-INF/lib", project, prop.getProperty("WEB_ROOT") + "/WEB-INF/lib", monitor);
        resourceDirectoryCopy("resources/WebContent/WEB-INF/config", project, prop.getProperty("WEB_ROOT") + "/" + prop.getProperty("CONTEXT_FOLDER"), monitor);
        Map<String, String> repl = new HashMap<String, String>();
        repl.put("\\{DataSourceDriver\\}", dbDriver);
        repl.put("\\{DataSourceUrl\\}", dbUrl);
        repl.put("\\{DataSourceUser\\}", dbUser);
        repl.put("\\{DataSourcePassword\\}", dbPass);
        repl.put("\\{DataSourceDialect\\}", dbDialect);
        resourceCopy("resources/WebContent/WEB-INF/config/dataSourceContext.xml", project, prop.getProperty("WEB_ROOT") + "/" + prop.getProperty("CONTEXT_FOLDER") + "/" + prop.getProperty("DATA_SOURCE_CONTEXT"), monitor, repl, "UTF-8");
        resourceCopy("resources/WebContent/WEB-INF/urlrewrite.xml", project, prop.getProperty("WEB_ROOT") + "/WEB-INF/urlrewrite.xml", monitor);
        resourceCopy("resources/WebContent/WEB-INF/web.xml", project, prop.getProperty("WEB_ROOT") + "/WEB-INF/web.xml", monitor, "\\{ProjectName\\}", projectName, "UTF-8");
        resourceCopy("resources/.project", project, ".project", monitor, "\\{ProjectName\\}", projectName, "UTF-8");
        resourceCopy("resources/.classpath", project, ".classpath", monitor, "\\{RuntimeName\\}", runtimeName, "UTF-8");
        repl = new HashMap<String, String>();
        repl.put("\\{ProjectName\\}", projectName);
        repl.put("\\{RuntimeName\\}", runtimeName);
        Enumeration<URL> it = bundle.findEntries("resources/.settings", "*", false);
        while (it.hasMoreElements()) {
            URL url = it.nextElement();
            File f = new File(FileLocator.toFileURL(url).toURI());
            if (!f.isFile()) {
                continue;
            }
            String fName = f.getName();
            boolean skip = false;
            for (String skiper : skipList) {
                if (fName.equals(skiper)) {
                    skip = true;
                    break;
                }
            }
            if (!skip) {
                resourceCopy("resources/.settings/" + fName, project, ".settings/" + fName, monitor, repl, "UTF-8");
            }
        }
    }

    private void resourceCopy(String resource, IProject project, String target, IProgressMonitor monitor) throws URISyntaxException, IOException {
        IFile targetFile = project.getFile(target);
        URL url = bundle.getEntry(resource);
        InputStream is = null;
        try {
            is = FileLocator.toFileURL(url).openStream();
            if (targetFile.exists()) {
                targetFile.setContents(is, true, false, monitor);
            } else {
                targetFile.create(is, true, monitor);
            }
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private void resourceCopy(String resource, IProject project, String target, IProgressMonitor monitor, String toReplace, String replacement, String charset) throws URISyntaxException, IOException {
        IFile targetFile = project.getFile(target);
        URL url = bundle.getEntry(resource);
        InputStream is = null;
        ByteArrayInputStream bais = null;
        try {
            is = FileLocator.toFileURL(url).openStream();
            int len = is.available();
            byte[] buf = new byte[len];
            is.read(buf);
            if (toReplace == null || toReplace.isEmpty()) {
                bais = new ByteArrayInputStream(buf);
            } else {
                String str = new String(buf, charset);
                str = str.replaceAll(toReplace, replacement);
                bais = new ByteArrayInputStream(str.getBytes(charset));
            }
            if (targetFile.exists()) {
                targetFile.setContents(bais, true, false, monitor);
            } else {
                targetFile.create(bais, true, monitor);
            }
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            if (bais != null) {
                bais.close();
            }
            if (is != null) {
                is.close();
            }
        }
    }

    private void resourceCopy(String resource, IProject project, String target, IProgressMonitor monitor, Map<String, String> replacement, String charset) throws URISyntaxException, IOException {
        IFile targetFile = project.getFile(target);
        URL url = bundle.getEntry(resource);
        InputStream is = null;
        ByteArrayInputStream bais = null;
        try {
            is = FileLocator.toFileURL(url).openStream();
            int len = is.available();
            byte[] buf = new byte[len];
            is.read(buf);
            String str = new String(buf, charset);
            for (String toRepl : replacement.keySet()) {
                str = str.replaceAll(toRepl, replacement.get(toRepl));
            }
            bais = new ByteArrayInputStream(str.getBytes("UTF-8"));
            if (targetFile.exists()) {
                targetFile.setContents(bais, true, false, monitor);
            } else {
                targetFile.create(bais, true, monitor);
            }
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            if (bais != null) {
                bais.close();
            }
            if (is != null) {
                is.close();
            }
        }
    }

    private void resourceDirectoryCopy(String resource, IProject project, String target, IProgressMonitor monitor) throws URISyntaxException, IOException, CoreException {
        if (!target.endsWith("/")) {
            target += "/";
        }
        String res = resource;
        if (!res.endsWith("/")) ;
        {
            res += "/";
        }
        Enumeration<URL> it = bundle.findEntries(resource, "*", false);
        while (it.hasMoreElements()) {
            URL url = it.nextElement();
            File f = new File(FileLocator.toFileURL(url).toURI());
            String fName = f.getName();
            boolean skip = false;
            for (String skiper : skipList) {
                if (fName.equals(skiper)) {
                    skip = true;
                    break;
                }
            }
            if (skip) {
                continue;
            }
            String targetName = target + fName;
            if (f.isDirectory()) {
                IFolder folder = project.getFolder(targetName);
                if (!folder.exists()) {
                    folder.create(true, true, monitor);
                }
                resourceDirectoryCopy(res + f.getName(), project, targetName, monitor);
            } else if (f.isFile()) {
                IFile targetFile = project.getFile(targetName);
                InputStream is = null;
                try {
                    is = url.openStream();
                    if (targetFile.exists()) {
                        targetFile.setContents(is, true, false, monitor);
                    } else {
                        targetFile.create(is, true, monitor);
                    }
                } catch (Exception e) {
                    throw new IOException(e);
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }
        }
    }
}
