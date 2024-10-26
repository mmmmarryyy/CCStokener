package com.the_eventhorizon.todo.commons.taskTagDecorator.impl;

import java.net.URL;
import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import org.osgi.framework.Bundle;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IConfigurationElement;
import com.the_eventhorizon.todo.commons.CommonsActivator;
import com.the_eventhorizon.todo.utils.ExtensionUtils;
import com.the_eventhorizon.todo.utils.IExtensionVisitor;
import com.the_eventhorizon.todo.commons.taskTagDecorator.model.TaskTagImage;

/**
 * Loads images (default and from extension points).
 * 
 * @author pkrupets
 */
public final class ImagesProvider {

    public static final String NO_IMAGE = "NO_IMAGE";

    public static final String[] IMAGE_NAMES = { "Question", "Exclamation", "Exclamation Blue", "Exclamation Cyan", "Exclamation Green", "Exclamation Orange", "Exclamation Yellow", "Exclamation Magenta" };

    public static final String[] IMAGE_RESOURCES = { "/com/the_eventhorizon/todo/commons/taskTagDecorator/resources/question.gif", "/com/the_eventhorizon/todo/commons/taskTagDecorator/resources/exclamation-red.gif", "/com/the_eventhorizon/todo/commons/taskTagDecorator/resources/exclamation-blue.gif", "/com/the_eventhorizon/todo/commons/taskTagDecorator/resources/exclamation-cyan.gif", "/com/the_eventhorizon/todo/commons/taskTagDecorator/resources/exclamation-green.gif", "/com/the_eventhorizon/todo/commons/taskTagDecorator/resources/exclamation-orange.gif", "/com/the_eventhorizon/todo/commons/taskTagDecorator/resources/exclamation-yellow.gif", "/com/the_eventhorizon/todo/commons/taskTagDecorator/resources/exclamation-magenta.gif" };

    /**
     * @return cannot be <code>null</code>.
     */
    public static ImagesProvider getInstance() {
        return theInstance;
    }

    /**
     * @return cannot be <code>null</code> or empty.
     */
    public TaskTagImage[] getTaskTagImages() {
        if (this.images == null) {
            synchronized (this) {
                if (this.images == null) {
                    load();
                }
            }
        }
        return this.images;
    }

    private void load() {
        final Map<String, TaskTagImage> images = new HashMap<String, TaskTagImage>();
        ExtensionUtils.visit(EP_TASK_TAG_IMAGE, new IExtensionVisitor() {

            /**
             * @see com.the_eventhorizon.todo.utils.IExtensionVisitor#visit(org.osgi.framework.Bundle, org.eclipse.core.runtime.IExtension)
             */
            public boolean visit(Bundle bundle, IExtension extension) {
                IConfigurationElement[] confItems = extension.getConfigurationElements();
                if (confItems == null || confItems.length == 0) {
                    return true;
                }
                for (IConfigurationElement conf : confItems) {
                    String name = conf.getAttribute(PARAM_TTI_NAME);
                    if (images.containsKey(name)) {
                        continue;
                    }
                    TaskTagImage image = createTaskTagImage(bundle, name, conf.getAttribute(PARAM_TTI_RESOURCE));
                    if (image != null) {
                        images.put(image.getName(), image);
                    }
                }
                return true;
            }

            /**
             * @see com.the_eventhorizon.todo.utils.IExtensionVisitor#failedToStartBundle(org.osgi.framework.Bundle, java.lang.Throwable)
             */
            public boolean failedToStartBundle(Bundle bundle, Throwable cause) {
                CommonsActivator.error("Unable to activate '" + bundle.getBundleId() + "' bundle!", cause);
                return true;
            }
        });
        for (int i = 0; i < IMAGE_NAMES.length; i++) {
            if (!images.containsKey(IMAGE_NAMES[i])) {
                ImageData imgData = new ImageData(getClass().getClassLoader().getResourceAsStream(IMAGE_RESOURCES[i]));
                ImageDescriptor img = ImageDescriptor.createFromImageData(imgData);
                images.put(IMAGE_NAMES[i], new TaskTagImage(IMAGE_NAMES[i], img));
            }
        }
        List<TaskTagImage> values = new ArrayList<TaskTagImage>(images.values());
        Collections.sort(values, new Comparator<TaskTagImage>() {

            public int compare(TaskTagImage o1, TaskTagImage o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        this.images = values.toArray(new TaskTagImage[values.size()]);
    }

    private TaskTagImage createTaskTagImage(Bundle bundle, String name, String resource) {
        if (name == null || name.length() == 0) {
            CommonsActivator.error("Task tag image name must not be null or empty!");
            return null;
        }
        if (resource == null || resource.length() == 0) {
            CommonsActivator.error("Task tag image resource must not be null or empty!");
            return null;
        }
        ImageDescriptor img = null;
        URL url = bundle.getResource(resource);
        if (url != null) {
            try {
                ImageData imgData = new ImageData(url.openConnection().getInputStream());
                img = ImageDescriptor.createFromImageData(imgData);
            } catch (IOException e) {
                CommonsActivator.error("unable to load '" + resource + "' image!", e);
                return null;
            }
        }
        return new TaskTagImage(name, img);
    }

    private ImagesProvider() {
    }

    private TaskTagImage[] images = null;

    private static final ImagesProvider theInstance = new ImagesProvider();

    private static final String PARAM_TTI_NAME = "name";

    private static final String PARAM_TTI_RESOURCE = "resource";

    private static final String EP_TASK_TAG_IMAGE = "taskTagImage";
}
