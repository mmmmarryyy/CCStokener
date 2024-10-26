package org.eclipse.babel.editor.bundle;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.eclipse.babel.core.message.MessagesBundle;
import org.eclipse.babel.core.message.resource.PropertiesIFileResource;
import org.eclipse.babel.core.message.resource.PropertiesReadOnlyResource;
import org.eclipse.babel.core.message.resource.ser.PropertiesDeserializer;
import org.eclipse.babel.core.message.resource.ser.PropertiesSerializer;
import org.eclipse.babel.editor.plugin.MessagesEditorPlugin;
import org.eclipse.babel.editor.preferences.MsgEditorPreferences;
import org.eclipse.babel.editor.resource.EclipsePropertiesEditorResource;
import org.eclipse.babel.editor.util.UIUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.FileEditorInput;
import org.osgi.framework.Bundle;

/**
 * This strategy is used when a resource bundle that belongs to Plugin-Fragment 
 * project is loaded.
 * <p>
 * This class loads resource bundles following the default strategy.
 * If no root locale resource is found, it tries to locate that resource
 * inside the host-plugin of the fragment. The host plugin is searched inside
 * the workspace
 * first and if not found inside the eclipse-platform being run.
 * </p>
 * <p>
 * This is useful for the developement of i18n packages for eclipse plugin:
 * The best practice is to define the root locale messages inside the plugin
 * itself and to define the other locales in a fragment that host that plugin.
 * <br/>Thanks to this strategy the root locale can be used by the user when
 *  he edits
 * the messages defined in the fragment alone.
 * <p>
 * See Bug #214521.
 * </p>
 * @author Pascal Essiembre
 * @author Hugues Malphettes
 */
public class NLFragmentBundleGroupStrategy extends NLPluginBundleGroupStrategy {

    private final String _fragmentHostID;

    private boolean hostPluginInWorkspaceWasLookedFor = false;

    private IProject hostPluginInWorkspace;

    /**
     * 
     */
    public NLFragmentBundleGroupStrategy(IEditorSite site, IFile file, String fragmentHostID, IFolder nlFolder) {
        super(site, file, nlFolder);
        _fragmentHostID = fragmentHostID;
    }

    /**
     * @see org.eclipse.babel.core.bundle.IBundleGroupStrategy#loadBundles()
     */
    public MessagesBundle[] loadMessagesBundles() {
        MessagesBundle[] defaultFiles = super.loadMessagesBundles();
        for (int i = 0; i < defaultFiles.length; i++) {
            MessagesBundle mb = defaultFiles[i];
            if (UIUtils.ROOT_LOCALE.equals(mb.getLocale()) || mb.getLocale() == null) {
                return defaultFiles;
            }
        }
        try {
            MessagesBundle locatedBaseProperties = loadDefaultMessagesBundle();
            if (locatedBaseProperties != null) {
                MessagesBundle[] result = new MessagesBundle[defaultFiles.length + 1];
                result[0] = locatedBaseProperties;
                System.arraycopy(defaultFiles, 0, result, 1, defaultFiles.length);
                return result;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @see org.eclipse.babel.core.bundle.IBundleGroupStrategy
     *          #createBundle(java.util.Locale)
     */
    public MessagesBundle createMessagesBundle(Locale locale) {
        return super.createMessagesBundle(locale);
    }

    /**
     * @see org.eclipse.babel.core.message.strategy.IMessagesBundleGroupStrategy
     *          #createMessagesBundleGroupName()
     */
    public String createMessagesBundleGroupName() {
        return super.createMessagesBundleGroupName();
    }

    /**
     * @return The message bundle for the message.properties file associated to
     * the edited resource bundle once this code is executed by eclipse.
     * @throws CoreException
     * @throws IOException 
     */
    private MessagesBundle loadDefaultMessagesBundle() throws IOException, CoreException {
        IEditorInput newEditorInput = null;
        IPath propertiesBasePath = getPropertiesPath();
        String resourceLocationLabel = null;
        IProject developpedProject = getHostPluginProjectDevelopedInWorkspace();
        if (developpedProject != null) {
            IFile file = getPropertiesFile(developpedProject, propertiesBasePath);
            if (!file.exists()) {
                String[] jarredProps = getJarredPropertiesAndResourceLocationLabel(developpedProject, propertiesBasePath);
                if (jarredProps != null) {
                    if (site == null) {
                        MsgEditorPreferences prefs = MsgEditorPreferences.getInstance();
                        return new MessagesBundle(new PropertiesReadOnlyResource(UIUtils.ROOT_LOCALE, new PropertiesSerializer(prefs), new PropertiesDeserializer(prefs), jarredProps[0], jarredProps[1]));
                    }
                    newEditorInput = new DummyEditorInput(jarredProps[0], getPropertiesPath().lastSegment(), jarredProps[1]);
                    resourceLocationLabel = jarredProps[1];
                }
            }
            if (site == null) {
                if (file.exists()) {
                    MsgEditorPreferences prefs = MsgEditorPreferences.getInstance();
                    return new MessagesBundle(new PropertiesIFileResource(UIUtils.ROOT_LOCALE, new PropertiesSerializer(prefs), new PropertiesDeserializer(prefs), file, MessagesEditorPlugin.getDefault()));
                } else {
                    return null;
                }
            }
            if (file.exists()) {
                newEditorInput = new FileEditorInput(file);
            }
        }
        if (newEditorInput == null) {
            InputStream in = null;
            String resourceName = null;
            try {
                Bundle bundle = Platform.getBundle(_fragmentHostID);
                if (bundle != null) {
                    resourceName = propertiesBasePath.toString();
                    URL url = bundle.getEntry(resourceName);
                    if (url != null) {
                        in = url.openStream();
                        resourceLocationLabel = url.toExternalForm();
                    } else {
                        url = bundle.getResource(resourceName);
                        if (url != null) {
                            in = url.openStream();
                            resourceLocationLabel = url.toExternalForm();
                        }
                    }
                }
                if (in != null) {
                    String contents = getContents(in);
                    if (site == null) {
                        MsgEditorPreferences prefs = MsgEditorPreferences.getInstance();
                        return new MessagesBundle(new PropertiesReadOnlyResource(UIUtils.ROOT_LOCALE, new PropertiesSerializer(prefs), new PropertiesDeserializer(prefs), contents, resourceLocationLabel));
                    }
                    newEditorInput = new DummyEditorInput(contents, getPropertiesPath().lastSegment(), getPropertiesPath().toString());
                }
            } finally {
                if (in != null) try {
                    in.close();
                } catch (IOException ioe) {
                }
            }
        }
        if (newEditorInput != null) {
            TextEditor textEditor = null;
            if (site != null) {
                try {
                    textEditor = (TextEditor) Class.forName(PROPERTIES_EDITOR_CLASS_NAME).newInstance();
                } catch (Exception e) {
                    textEditor = new TextEditor();
                }
                textEditor.init(site, newEditorInput);
            } else {
                System.err.println("the site " + site);
            }
            MsgEditorPreferences prefs = MsgEditorPreferences.getInstance();
            EclipsePropertiesEditorResource readOnly = new EclipsePropertiesEditorResource(UIUtils.ROOT_LOCALE, new PropertiesSerializer(prefs), new PropertiesDeserializer(prefs), textEditor);
            if (resourceLocationLabel != null) {
                readOnly.setResourceLocationLabel(resourceLocationLabel);
            }
            return new MessagesBundle(readOnly);
        }
        return null;
    }

    private String getContents(InputStream in) throws IOException {
        Reader reader = new BufferedReader(new InputStreamReader(in));
        int ch;
        StringBuffer buffer = new StringBuffer();
        while ((ch = reader.read()) > -1) {
            buffer.append((char) ch);
        }
        in.close();
        return buffer.toString();
    }

    /**
     * The resource bundle can be either with the rest of the classes or
     * with the bundle resources.
     * @return
     */
    protected boolean resourceBundleIsInsideClasses() {
        IProject thisProj = getOpenedFile().getProject();
        Collection<String> srcs = getSourceFolderPathes(thisProj);
        String thisPath = getOpenedFile().getProjectRelativePath().toString();
        if (srcs != null) {
            for (String srcPath : srcs) {
                if (thisPath.startsWith(srcPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return The path of the base properties file. Relative to a source folder
     * or the root of the project if there is no source folder.
     * <p>
     * For example if the properties file is for the package 
     * org.eclipse.ui.workbench.internal.messages.properties
     * The path return is org/eclipse/ui/workbench/internal/messages/properties
     * </p>
     */
    protected IPath getPropertiesPath() {
        IPath projRelative = super.basePathInsideNL == null ? super.getOpenedFile().getParent().getProjectRelativePath() : new Path(super.basePathInsideNL);
        return removePathToSourceFolder(projRelative).append(getBaseName() + ".properties");
    }

    /**
     * @param hostPluginProject The project in the workspace that is the host
     *        plugin
     * @param propertiesBasePath The result of getPropertiesPath();
     * @return
     */
    protected IFile getPropertiesFile(IProject hostPluginProject, IPath propertiesBasePath) {
        IResource r = hostPluginProject.findMember(propertiesBasePath);
        if (r != null && r.getType() == IResource.FILE) {
            return (IFile) r;
        }
        Collection<String> srcPathes = getSourceFolderPathes(hostPluginProject);
        if (srcPathes != null) {
            for (String srcPath : srcPathes) {
                IFolder srcFolder = hostPluginProject.getFolder(new Path(srcPath));
                if (srcFolder.exists()) {
                    r = srcFolder.findMember(propertiesBasePath);
                    if (r != null && r.getType() == IResource.FILE) {
                        return (IFile) r;
                    }
                }
            }
        }
        return hostPluginProject.getFile(propertiesBasePath);
    }

    /**
     * Returns the content of the properties if they were located inside a jar
     * inside the plugin.
     * 
     * @param hostPluginProject
     * @param propertiesBasePath
     * @return The content and location label of the properties or null if
     * they could not be found.
     */
    private String[] getJarredPropertiesAndResourceLocationLabel(IProject hostPluginProject, IPath propertiesBasePath) {
        Collection<String> libPathes = getLibPathes(hostPluginProject);
        if (libPathes != null) {
            String entryName = propertiesBasePath.toString();
            for (String libPath : libPathes) {
                if (libPath.endsWith(".jar")) {
                    IFile jar = hostPluginProject.getFile(new Path(libPath));
                    if (jar.exists()) {
                        File file = jar.getRawLocation().toFile();
                        if (file.exists()) {
                            JarFile jf = null;
                            try {
                                jf = new JarFile(file);
                                JarEntry je = jf.getJarEntry(entryName);
                                if (je != null) {
                                    String content = getContents(jf.getInputStream(je));
                                    String location = jar.getFullPath().toString() + "!/" + entryName;
                                    return new String[] { content, location };
                                }
                            } catch (IOException e) {
                            } finally {
                                if (jf != null) {
                                    try {
                                        jf.close();
                                    } catch (IOException e) {
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Redo a little parser utility in order to not depend on pde.
     * 
     * @param proj
     * @return The pathes of the source folders extracted from 
     *          the .classpath file
     */
    protected static Collection<String> getSourceFolderPathes(IProject proj) {
        return getClasspathEntryPathes(proj, "src");
    }

    /**
     * Redo a little parser utility in order to not depend on pde.
     * 
     * @param proj
     * @return The pathes of the source folders extracted from the
     *           .classpath file
     */
    protected Collection<String> getLibPathes(IProject proj) {
        return getClasspathEntryPathes(proj, "lib");
    }

    protected static Collection<String> getClasspathEntryPathes(IProject proj, String classpathentryKind) {
        IFile classpathRes = proj.getFile(".classpath");
        if (!classpathRes.exists()) {
            return null;
        }
        Collection<String> res = new ArrayList<String>();
        InputStream in = null;
        try {
            in = ((IFile) classpathRes).getContents();
            Reader r = new InputStreamReader(in, "UTF-8");
            LineNumberReader lnr = new LineNumberReader(r);
            String line = lnr.readLine();
            while (line != null) {
                if (line.indexOf("<classpathentry ") != -1 && line.indexOf(" kind=\"" + classpathentryKind + "\" ") != -1) {
                    int pathIndex = line.indexOf(" path=\"");
                    if (pathIndex != -1) {
                        int secondQuoteIndex = line.indexOf('\"', pathIndex + " path=\"".length());
                        if (secondQuoteIndex != -1) {
                            res.add(line.substring(pathIndex + " path=\"".length(), secondQuoteIndex));
                        }
                    }
                }
                line = lnr.readLine();
            }
            lnr.close();
            r.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException e) {
            }
        }
        return res;
    }

    /**
     * A dummy editor input represents text that may be shown in a 
     * text editor, but cannot be saved as it does not relate to a modifiable
     * file.
     */
    private class DummyEditorInput implements IStorageEditorInput, IStorage {

        /**
         * the error messages as a Java inputStream.
         */
        private String _contents;

        /**
         * the name of the input
         */
        private String _name;

        /**
         * the tooltip text, optional
         */
        private String _toolTipText;

        /**
         * Simplified constructor that does not need 
         * a tooltip, the name is used instead
         * @param contents
         * @param _name
         */
        public DummyEditorInput(String contents, String name, boolean isEditable) {
            this(contents, name, name);
        }

        /**
         * The complete constructor.
         * @param contents the contents to be given to the editor
         * @param _name the name of the input
         * @param tipText the text to be shown when a tooltip
         * is requested on the editor name part.
         */
        public DummyEditorInput(String contents, String name, String tipText) {
            super();
            _contents = contents;
            _name = name;
            _toolTipText = tipText;
        }

        /**
         * we return the instance itself.
         */
        public IStorage getStorage() throws CoreException {
            return this;
        }

        /**
         * the input never exists
         */
        public boolean exists() {
            return false;
        }

        /**
         * nothing in particular for now.
         */
        public ImageDescriptor getImageDescriptor() {
            return null;
        }

        /**
         * the name given in constructor
         */
        public String getName() {
            return _name;
        }

        /**
         * our input is not persistable.
         */
        public IPersistableElement getPersistable() {
            return null;
        }

        /**
         * returns the value passed as an argument in constructor.
         */
        public String getToolTipText() {
            return _toolTipText;
        }

        /**
         * we do not adapt much.
         */
        public Object getAdapter(Class adapter) {
            return null;
        }

        /**
         * the contents
         */
        public InputStream getContents() throws CoreException {
            return new ByteArrayInputStream(_contents.getBytes());
        }

        /**
         * this is not used and should not impact the IDE.
         */
        public IPath getFullPath() {
            return null;
        }

        /**
         * the text is always readonly.
         */
        public boolean isReadOnly() {
            return true;
        }
    }

    /**
     * Called when using an nl structure.
     * We need to find out whether the variant is in fact a folder.
     * If we locate a folder inside the project with this name we assume it is
     * not a variant.
     * <p>
     * This method is overridden inside the NLFragment thing as we need to
     * check 2 projects over there:
     * the host-plugin project and the current project.
     * </p>
     * @param possibleVariant
     * @return
     */
    protected boolean isExistingFirstFolderForDefaultLocale(String folderName) {
        IProject thisProject = getOpenedFile().getProject();
        if (thisProject == null) {
            return false;
        }
        boolean res = thisProject.getFolder(folderName).exists();
        if (res) {
            return true;
        }
        IProject developpedProject = getHostPluginProjectDevelopedInWorkspace();
        if (developpedProject != null) {
            res = developpedProject.getFolder(folderName).exists();
            if (res) {
                return true;
            }
            return false;
        }
        Bundle bundle = getHostPluginBundleInPlatform();
        if (bundle != null) {
            if (bundle.getEntry(folderName) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Look for the host plugin inside the workspace itself.
     * Caches the result.
     * @return
     */
    private IProject getHostPluginProjectDevelopedInWorkspace() {
        if (hostPluginInWorkspaceWasLookedFor) {
            return hostPluginInWorkspace;
        } else {
            hostPluginInWorkspaceWasLookedFor = true;
        }
        IProject thisProject = getOpenedFile().getProject();
        if (thisProject == null) {
            return null;
        }
        try {
            IResource[] members = ((IContainer) thisProject.getParent()).members();
            for (int i = 0; i < members.length; i++) {
                IResource childRes = members[i];
                if (childRes != thisProject && childRes.getType() == IResource.PROJECT) {
                    String bundle = MessagesBundleGroupFactory.getBundleId(childRes);
                    if (_fragmentHostID.equals(bundle)) {
                        hostPluginInWorkspace = (IProject) childRes;
                        return hostPluginInWorkspace;
                    }
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    private Bundle getHostPluginBundleInPlatform() {
        Bundle bundle = Platform.getBundle(_fragmentHostID);
        if (bundle != null) {
            return bundle;
        }
        return null;
    }
}
