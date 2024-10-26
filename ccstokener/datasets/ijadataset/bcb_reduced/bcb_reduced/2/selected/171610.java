package org.abettor.leaf4e.decorators;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.abettor.leaf4e.Activator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;

/**
 * 工程图标修饰类
 * @author shawn
 *
 */
public class ProjectDecorator implements ILightweightLabelDecorator {

    private final Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);

    private ILog logger = Activator.getDefault().getLog();

    @Override
    public void addListener(ILabelProviderListener listener) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean isLabelProperty(Object element, String propery) {
        return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
    }

    @Override
    public void decorate(Object element, IDecoration decoration) {
        if (element != null && element instanceof IProject) {
            InputStream is = null;
            try {
                IProject project = (IProject) element;
                IFile file = project.getFile(Activator.PLUGIN_CONF);
                if (file.exists()) {
                    URL url = bundle.getEntry("icons/leaf4e_decorator.gif");
                    is = FileLocator.toFileURL(url).openStream();
                    Image img = new Image(Display.getCurrent(), is);
                    ImageDescriptor id = ImageDescriptor.createFromImage(img);
                    decoration.addOverlay(id, IDecoration.TOP_LEFT);
                }
            } catch (Exception e) {
                Status status = new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Decorating error", e);
                logger.log(status);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        Status status = new Status(IStatus.WARNING, Activator.PLUGIN_ID, "", e);
                        logger.log(status);
                    }
                }
            }
        }
    }
}
