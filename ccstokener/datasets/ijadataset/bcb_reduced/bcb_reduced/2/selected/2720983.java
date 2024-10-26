package net.sf.ttd.core.extensions.impl;

import java.net.URL;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.io.IOException;
import org.osgi.framework.Bundle;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IConfigurationElement;
import net.sf.ttd.common.utils.NotNull;
import net.sf.ttd.core.Activator;
import net.sf.ttd.core.config.TaskTagImage;
import net.sf.ttd.core.extensions.IImagesProvider;

/**
 * @author pkrupets
 */
public final class ImagesProvider implements IImagesProvider {

    public ImagesProvider(@NotNull ExtensionPointUtils extensionPointUtils, @NotNull Bundle coreBundle) {
        final Map<String, TaskTagImage> images = new HashMap<String, TaskTagImage>();
        extensionPointUtils.visit(EP_TASK_TAG_IMAGE, new IExtensionVisitor() {

            /**
			 * @see net.sf.ttd.core.extensions.impl.IExtensionVisitor#visit(org.osgi.framework.Bundle, org.eclipse.core.runtime.IExtension)
			 */
            @Override
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
        });
        for (int i = 0; i < IMAGE_NAMES.length; i++) {
            if (!images.containsKey(IMAGE_NAMES[i])) {
                TaskTagImage image = createTaskTagImage(coreBundle, IMAGE_NAMES[i], IMAGE_RESOURCES[i]);
                if (image != null) {
                    images.put(IMAGE_NAMES[i], image);
                }
            }
        }
        this.images = images.values().toArray(new TaskTagImage[images.size()]);
        Arrays.sort(this.images);
    }

    /**
	 * @see net.sf.ttd.core.extensions.IImagesProvider#getTaskTagImages()
	 */
    @Override
    public TaskTagImage[] getTaskTagImages() {
        return this.images.clone();
    }

    private TaskTagImage createTaskTagImage(Bundle bundle, String name, String resource) {
        if (name == null || name.length() == 0) {
            Activator.getLogger().logError("Task tag image name must not be null or empty!");
            return null;
        }
        if (resource == null || resource.length() == 0) {
            Activator.getLogger().logError("Task tag image resource must not be null or empty!");
            return null;
        }
        ImageDescriptor img = null;
        URL url = bundle.getResource(resource);
        if (url != null) {
            try {
                ImageData imgData = new ImageData(url.openConnection().getInputStream());
                img = ImageDescriptor.createFromImageData(imgData);
            } catch (IOException e) {
            }
        }
        if (img == null) {
            Activator.getLogger().logError("Unable to load '" + name + "' task tag image from '" + resource + "' resource!");
            return null;
        }
        return new TaskTagImage(name, img);
    }

    private final TaskTagImage[] images;
}
