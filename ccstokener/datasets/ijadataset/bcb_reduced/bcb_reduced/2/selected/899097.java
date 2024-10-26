package de.mpiwg.vspace.oaw.validation;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import de.mpiwg.vspace.common.project.ProjectManager;
import de.mpiwg.vspace.metamodel.LocalImage;
import de.mpiwg.vspace.metamodel.WebImage;

public class ImageValidationHelper {

    public static boolean doesImageFileExist(LocalImage image) {
        IProject project = ProjectManager.getInstance().getCurrentProject();
        String root = project.getLocation().toOSString() + File.separator;
        String path = root + image.getImagePath();
        File imageFile = new File(path);
        if (imageFile.exists()) return true;
        return false;
    }

    public static boolean isImageLinkReachable(WebImage image) {
        if (image.getUrl() == null) return false;
        try {
            URL url = new URL(image.getUrl());
            url.openStream().close();
        } catch (MalformedURLException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static boolean isImageFormatUsable(LocalImage image) {
        List<String> allowedExtensions = new ArrayList<String>();
        allowedExtensions.add("png");
        allowedExtensions.add("jpg");
        allowedExtensions.add("gif");
        allowedExtensions.add("GIF");
        allowedExtensions.add("JPG");
        allowedExtensions.add("PNG");
        String path = image.getImagePath();
        Path imagePath = new Path(path);
        String extension = imagePath.getFileExtension();
        if (allowedExtensions.contains(extension)) return true;
        return false;
    }
}
