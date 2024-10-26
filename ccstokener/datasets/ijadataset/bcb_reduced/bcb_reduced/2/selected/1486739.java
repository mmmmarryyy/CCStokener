package net.sf.colorer.eclipse;

import java.net.URL;
import java.util.Hashtable;
import net.sf.colorer.impl.Logger;
import org.eclipse.jface.resource.ImageDescriptor;

public class ImageStore {

    static final URL BASE_URL = ColorerPlugin.getDefault().getBundle().getEntry("/");

    static Hashtable hash = new Hashtable();

    public static final ImageDescriptor EDITOR_UPDATEHRC;

    public static final ImageDescriptor EDITOR_UPDATEHRC_A;

    public static final ImageDescriptor EDITOR_FILETYPE;

    public static final ImageDescriptor EDITOR_FILETYPE_A;

    public static final ImageDescriptor EDITOR_CUR_FILETYPE;

    public static final ImageDescriptor EDITOR_CUR_GROUP;

    public static final ImageDescriptor EDITOR_GROUP;

    public static final ImageDescriptor EDITOR_PAIR_MATCH;

    public static final ImageDescriptor EDITOR_PAIR_SELECT;

    public static final ImageDescriptor EDITOR_PAIR_SELECTCONTENT;

    static String iconPath = "icons/";

    static String prefix = iconPath;

    static {
        EDITOR_UPDATEHRC = createImageDescriptor(prefix + "updatehrc.gif");
        EDITOR_UPDATEHRC_A = createImageDescriptor(prefix + "updatehrc_a.gif");
        EDITOR_FILETYPE = createImageDescriptor(prefix + "filetype.gif");
        EDITOR_FILETYPE_A = createImageDescriptor(prefix + "filetype_a.gif");
        EDITOR_CUR_FILETYPE = createImageDescriptor(prefix + "filetype/filetype.current.gif");
        EDITOR_CUR_GROUP = createImageDescriptor(prefix + "filetype/group.current.gif");
        EDITOR_GROUP = createImageDescriptor(prefix + "filetype/group.gif");
        EDITOR_PAIR_MATCH = createImageDescriptor(prefix + "pair-match.gif");
        EDITOR_PAIR_SELECT = createImageDescriptor(prefix + "pair-select.gif");
        EDITOR_PAIR_SELECTCONTENT = createImageDescriptor(prefix + "pair-select-content.gif");
    }

    private static ImageDescriptor createImageDescriptor(String path) {
        URL url = null;
        try {
            url = new URL(BASE_URL, path);
            url.openStream().close();
            return ImageDescriptor.createFromURL(url);
        } catch (Exception e) {
            Logger.trace("ImageStore", "Can't open URL: " + url);
        }
        return null;
    }

    public static ImageDescriptor getID(String name) {
        ImageDescriptor id = (ImageDescriptor) hash.get(name);
        if (id == null) {
            id = createImageDescriptor(prefix + name + ".gif");
            if (id == null) return null;
            hash.put(name, id);
        }
        ;
        return id;
    }

    ;
}
