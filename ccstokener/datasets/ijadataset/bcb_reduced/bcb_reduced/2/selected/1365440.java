package org.xfel.eclipse.ief.internal.ui.editor;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.xfel.eclipse.ief.core.IFileFormat;
import org.xfel.eclipse.ief.core.ImageCore;
import org.xfel.eclipse.ief.core.document.IDocument;
import org.xfel.eclipse.ief.internal.ui.viewers.AWTImageViewer;
import org.xfel.eclipse.ief.ui.AbstractImageViewer;
import org.xfel.eclipse.ief.ui.ImageUI;

public class ImageEditor extends EditorPart implements IReusableEditor {

    private AbstractImageViewer viewer;

    private IDocument document;

    public ImageEditor() {
    }

    /**
	 * Create contents of the editor part.
	 * 
	 * @param parent
	 */
    @Override
    public void createPartControl(Composite parent) {
        viewer = new AWTImageViewer(parent);
        getSite().setSelectionProvider(viewer);
    }

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    @Override
    public void doSaveAs() {
    }

    public void setDocument(IDocument document) {
        this.document = document;
    }

    public IDocument getDocument() {
        return document;
    }

    @Override
    protected final void setInputWithNotify(IEditorInput input) {
        setInput(input);
    }

    protected void doSetInput(IEditorInput input, IProgressMonitor monitor) throws CoreException {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IFileFormat format = null;
        Object source = null;
        InputStream in = null;
        try {
            IPath path;
            if (input instanceof IStorageEditorInput) {
                IStorage s = ((IStorageEditorInput) input).getStorage();
                in = s.getContents();
                if (s instanceof IFile) {
                    IFile file = (IFile) s;
                    path = file.getRawLocation();
                    if (root.exists(path)) {
                        path = root.getLocation().append(path);
                    }
                    source = path.toFile();
                }
            } else if (input instanceof IPathEditorInput) {
                path = ((IPathEditorInput) input).getPath();
                source = path.toFile();
            } else if (input instanceof IURIEditorInput) {
                URI uri = ((IURIEditorInput) input).getURI();
                if (URIUtil.isFileURI(uri)) {
                    source = URIUtil.toFile(uri);
                } else {
                    URL url = URIUtil.toURL(uri);
                    in = url.openStream();
                }
            }
            if (source == null) {
                if (!in.markSupported()) {
                    in = new BufferedInputStream(in);
                }
                in.mark(10);
                source = in;
            }
            IContentDescription cd = Platform.getContentTypeManager().getDescriptionFor(in, input.getName(), new QualifiedName[] { ImageCore.VALID_FORMATS });
            if (in != null) {
                in.reset();
            }
            Collection<?> valid = (Collection<?>) cd.getProperty(ImageCore.VALID_FORMATS);
            if (valid.isEmpty()) throw new CoreException(new Status(Status.ERROR, ImageUI.PLUGIN_ID, "Unsupported file format."));
            ImageInputStream stream = ImageIO.createImageInputStream(source);
            format = (IFileFormat) valid.iterator().next();
            IDocument document = format.decode(stream, monitor);
            setDocument(document);
        } catch (IOException e) {
            Status status = new Status(Status.ERROR, ImageUI.PLUGIN_ID, "IO Error", e);
            throw new CoreException(status);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        super.setInput(input);
    }

    @Override
    public final void setInput(final IEditorInput input) {
        try {
            doSetInput(input, new NullProgressMonitor());
            firePropertyChange(PROP_INPUT);
        } catch (CoreException e) {
            ErrorDialog.openError(getSite().getShell(), "Error", "An error occured while trying to set the EditorInput.", e.getStatus());
        }
    }

    @Override
    public void init(IEditorSite site, final IEditorInput input) throws PartInitException {
        setSite(site);
        IRunnableWithProgress r = new IRunnableWithProgress() {

            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                try {
                    doSetInput(input, monitor);
                } catch (CoreException e) {
                    throw new InvocationTargetException(e);
                }
            }
        };
        try {
            getSite().getWorkbenchWindow().run(true, true, r);
        } catch (InterruptedException e) {
        } catch (InvocationTargetException x) {
            Throwable t = x.getTargetException();
            if (t instanceof CoreException) {
                CoreException e = (CoreException) t;
                IStatus status = e.getStatus();
                if (status.getException() != null) throw new PartInitException(status);
                throw new PartInitException(new Status(status.getSeverity(), status.getPlugin(), status.getCode(), status.getMessage(), t));
            }
            throw new PartInitException(new Status(IStatus.ERROR, ImageUI.PLUGIN_ID, IStatus.OK, "Editor could not be initialized", t));
        }
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }
}
