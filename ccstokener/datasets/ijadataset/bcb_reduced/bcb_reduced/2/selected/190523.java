package org.teiid.cdk.core.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.osgi.framework.Bundle;

/**
 * Helper class to deal with miscellaneous resource related operations
 * 
 * @author Sanjay Chaudhuri <email2sanjayc@gmail.com>
 */
public class ResourceUtils {

    public static IProject createProject(String projectName, URI creationURI, IProgressMonitor monitor) throws CoreException {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        if (!project.exists()) {
            IProjectDescription desc = project.getWorkspace().newProjectDescription(project.getName());
            URI uri = null;
            if (!ResourcesPlugin.getWorkspace().getRoot().getLocationURI().equals(creationURI)) {
                IPath path = URIUtil.toPath(creationURI).append(projectName);
                uri = URIUtil.toURI(path);
            }
            desc.setLocationURI(uri);
            project.create(desc, monitor);
        }
        if (!project.isOpen()) {
            project.open(monitor);
        }
        return project;
    }

    public static void createFile(IPath relativeDstFilePath, File srcFile, String license, String connectorName, String packageName) throws CoreException {
        FileReader fileReader = null;
        PrintWriter fileWriter = null;
        try {
            byte[] bytes = readBytesFromFile(srcFile);
            if (bytes != null) {
                String fileContent = new String(bytes, "UTF-8");
                fileContent = fileContent.replaceAll("\\$\\{license\\}", license);
                fileContent = fileContent.replaceAll("\\$\\{package-name\\}", packageName);
                fileContent = fileContent.replaceAll("\\$\\{connector-name\\}", connectorName);
                File dstFile = ResourcesPlugin.getWorkspace().getRoot().getFile(relativeDstFilePath).getLocation().toFile();
                fileWriter = new PrintWriter(dstFile);
                BufferedReader bufferedReader = new BufferedReader(new StringReader(fileContent));
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    fileWriter.println(line);
                }
                bufferedReader.close();
                fileWriter.close();
            }
        } catch (FileNotFoundException e) {
            CdkLog.logError("File not found " + srcFile.getName());
        } catch (IOException e) {
            CdkLog.logError("Could not create file " + relativeDstFilePath);
        } finally {
            try {
                if (fileReader != null) fileReader.close();
                if (fileWriter != null) fileWriter.close();
            } catch (IOException e) {
                CdkLog.logError("Exception: ", e);
            }
            fileReader = null;
            fileWriter = null;
        }
    }

    public static void createFolders(IProject project, String[] folderStructures, IProgressMonitor monitor) throws CoreException {
        for (String folderStructure : folderStructures) {
            IPath relativePath = new Path(folderStructure);
            IContainer container = project;
            for (String pathSegment : relativePath.segments()) {
                container = container.getFolder(new Path(pathSegment));
                createFolders((IFolder) container, monitor);
            }
        }
    }

    public static void createFolders(IFolder folder, IProgressMonitor monitor) throws CoreException {
        if (!folder.exists()) folder.create(true, true, monitor);
    }

    public static byte[] readBytesFromFile(File file) {
        int fileLength = (int) file.length();
        if (fileLength > Integer.MAX_VALUE) {
            CdkLog.logError(new StringBuilder("File '").append(file.getName()).append("' is too large to handle.").toString());
            return null;
        }
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            byte[] bytes = new byte[fileLength];
            int byteOffset = 0;
            while (byteOffset < fileLength) {
                int bytesReadCount = inputStream.read(bytes, byteOffset, fileLength - byteOffset);
                if (bytesReadCount == -1) {
                    CdkLog.logError(new StringBuilder("Error reading reading file '").append(file.getName()).append("'.").toString());
                    return null;
                }
                byteOffset += bytesReadCount;
            }
            return bytes;
        } catch (IOException e) {
            CdkLog.logError(new StringBuilder("IOException while reading file '").append(file.getName()).append("'.").toString());
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    CdkLog.logError(new StringBuilder("IOException while closing file '").append(file.getName()).append("'.").toString());
                }
            }
            inputStream = null;
        }
    }

    public static void addNatures(IProject project, List<String> newNatureIds, IProgressMonitor monitor) throws CoreException {
        IProjectDescription description = project.getDescription();
        List<String> existingNatureIds = new ArrayList<String>(Arrays.asList(description.getNatureIds()));
        boolean isNatureAdded = false;
        for (String natureId : newNatureIds) {
            if (!project.hasNature(natureId)) {
                isNatureAdded = true;
                existingNatureIds.add(natureId);
            }
        }
        if (isNatureAdded) {
            description.setNatureIds((String[]) existingNatureIds.toArray(new String[0]));
            project.setDescription(description, monitor);
        }
    }

    public static void setClasspath(IProject project, String[][] srcAndOutputFolders, String defaultOutputFolder, IPath[] containerPaths, IPath[] libraryPaths, IProgressMonitor monitor) throws JavaModelException {
        List<IClasspathEntry> classpathEntries = new ArrayList<IClasspathEntry>();
        for (String[] srcAndOutputFolder : srcAndOutputFolders) {
            classpathEntries.add(JavaCore.newSourceEntry(project.getFullPath().append(srcAndOutputFolder[0]), new IPath[] {}, project.getFullPath().append(srcAndOutputFolder[1])));
        }
        for (IPath libraryPath : libraryPaths) {
            classpathEntries.add(JavaCore.newLibraryEntry(project.getFullPath().append(libraryPath), null, null, true));
        }
        classpathEntries.addAll(Arrays.asList(PreferenceConstants.getDefaultJRELibrary()));
        for (IPath containerPath : containerPaths) {
            if (containerPath != null) classpathEntries.add(JavaCore.newContainerEntry(containerPath));
        }
        JavaCore.create(project).setRawClasspath((IClasspathEntry[]) classpathEntries.toArray(new IClasspathEntry[0]), project.getFullPath().append(defaultOutputFolder), monitor);
    }

    public static IPath[] getResourcePathFromBundle(Bundle bundle, IPath resourceLocation) {
        URL directoryUrl = null;
        List<IPath> paths = new ArrayList<IPath>();
        if (resourceLocation.lastSegment().equals("*")) {
            try {
                directoryUrl = FileLocator.toFileURL(FileLocator.find(bundle, resourceLocation.removeLastSegments(1), null));
                URI uri = org.eclipse.core.runtime.URIUtil.toURI(directoryUrl);
                File dir = org.eclipse.core.runtime.URIUtil.toFile(uri);
                File[] files = dir.listFiles(new FilenameFilter() {

                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".jar");
                    }
                });
                for (File file : files) {
                    paths.add(new Path(file.getPath()));
                }
                return paths.toArray(new IPath[paths.size()]);
            } catch (IOException e) {
                CdkLog.logError("Exception: ", e);
            } catch (URISyntaxException e) {
                CdkLog.logError("Exception: ", e);
            }
            return null;
        } else {
            directoryUrl = FileLocator.find(bundle, resourceLocation, null);
            if (directoryUrl == null) {
                CdkLog.logError(new StringBuilder("Could not find '").append(resourceLocation).append("' in the plugin ").append(bundle.getSymbolicName()).append(" located at ").append(bundle.getLocation()).toString());
                return null;
            } else {
                try {
                    return new Path[] { new Path(FileLocator.toFileURL(directoryUrl).getPath()) };
                } catch (IOException e) {
                    CdkLog.logError("Exception: ", e);
                    return null;
                }
            }
        }
    }

    public static void createDirectory(File directory) throws CoreException {
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new CoreException(CdkLog.getError(String.format("Could not create directory: %1$s ", directory)));
            }
        }
    }

    public static String getFile(String path) {
        int separatorIndex = path.lastIndexOf(File.separator);
        return separatorIndex == -1 ? path : path.substring(separatorIndex + 1);
    }

    public static File getDirectory(File file) {
        return new File(getDirectory(file.getAbsolutePath()));
    }

    public static String getDirectory(String path) {
        int separatorIndex = path.lastIndexOf(File.separator);
        return separatorIndex == -1 ? "" : path.substring(0, separatorIndex);
    }

    public static void deleteFile(File file) throws CoreException {
        if (!file.delete()) {
            throw new CoreException(CdkLog.getError(String.format("Could not delete file: %1$s ", file)));
        }
    }

    public static boolean hasCompileErrors(IResource resource) throws CoreException {
        IMarker[] problemMarkers = resource.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
        for (int i = 0; i < problemMarkers.length; i++) {
            if (problemMarkers[i].getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR) return true;
        }
        return false;
    }

    public static IJavaProject getJavaProject(Object element) {
        if (element instanceof IJavaElement) {
            return ((IJavaElement) element).getJavaProject();
        } else if (element instanceof IResource) {
            IProject project = ((IResource) element).getProject();
            try {
                if (project.hasNature(JavaCore.NATURE_ID)) return JavaCore.create(project);
            } catch (CoreException ex) {
                CdkLog.logError("Exception: ", ex);
            }
        }
        return null;
    }

    public static void downloadJars(IProject project, String repositoryUrl, String jarDirectory, String[] jars) {
        try {
            File tmpFile = null;
            for (String jar : jars) {
                try {
                    tmpFile = File.createTempFile("tmpPlugin_", ".zip");
                    URL url = new URL(repositoryUrl + jarDirectory + jar);
                    String destFilename = new File(url.getFile()).getName();
                    File destFile = new File(project.getLocation().append("lib").append(jarDirectory).toFile(), destFilename);
                    InputStream inputStream = null;
                    FileOutputStream outputStream = null;
                    try {
                        URLConnection urlConnection = url.openConnection();
                        inputStream = urlConnection.getInputStream();
                        outputStream = new FileOutputStream(tmpFile);
                        IOUtils.copy(inputStream, outputStream);
                    } finally {
                        if (outputStream != null) {
                            outputStream.close();
                        }
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    }
                    FileUtils.copyFile(tmpFile, destFile);
                } finally {
                    if (tmpFile != null) {
                        tmpFile.delete();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
