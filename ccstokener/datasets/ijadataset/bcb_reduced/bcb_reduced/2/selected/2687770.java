package com.memoire.bu;

import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Properties;
import javax.swing.JFrame;
import com.memoire.fu.FuFactoryInteger;
import com.memoire.fu.FuLib;
import com.memoire.fu.FuResource;

/**
 * Utility class to manage resources.
 * As images, icons, localized strings, ...
 */
public class BuResource extends FuResource {

    public static final BuResource BU = new BuResource();

    public int getDefaultSize() {
        return BuPreferences.BU.getIntegerProperty("icons.size", 16);
    }

    public void setDefaultSize(int _v) {
        if (_v <= 0) BuPreferences.BU.removeProperty("icons.size"); else BuPreferences.BU.putIntegerProperty("icons.size", _v);
    }

    /**
   * Preference icons.menuSize, icon size for menus, lists, tables, trees.
   */
    public int getDefaultMenuSize() {
        return BuPreferences.BU.getIntegerProperty("icons.menusize", getDefaultSize());
    }

    public void setDefaultMenuSize(int _v) {
        if (_v <= 0) BuPreferences.BU.removeProperty("icons.menusize"); else BuPreferences.BU.putIntegerProperty("icons.menusize", _v);
    }

    /**
   * Preference icons.buttonSize, icon size for normal buttons.
   */
    public int getDefaultButtonSize() {
        return BuPreferences.BU.getIntegerProperty("icons.buttonsize", getDefaultSize());
    }

    public void setDefaultButtonSize(int _v) {
        if (_v <= 0) BuPreferences.BU.removeProperty("icons.buttonsize"); else BuPreferences.BU.putIntegerProperty("icons.buttonsize", _v);
    }

    /**
   * Preference icons.toolSize, icon size for tools.
   * A tool is a button in a toolbar with no or small text.
   */
    public int getDefaultToolSize() {
        return BuPreferences.BU.getIntegerProperty("icons.toolsize", getDefaultSize());
    }

    public void setDefaultToolSize(int _v) {
        if (_v <= 0) BuPreferences.BU.removeProperty("icons.toolsize"); else BuPreferences.BU.putIntegerProperty("icons.toolsize", _v);
    }

    /**
   * Preference icons.frameSize, icon size for the small icon in an
   * internal frame.
   */
    public int getDefaultFrameSize() {
        return BuPreferences.BU.getIntegerProperty("icons.framesize", getDefaultSize());
    }

    public void setDefaultFrameSize(int _v) {
        if (_v <= 0) BuPreferences.BU.removeProperty("icons.framesize"); else BuPreferences.BU.putIntegerProperty("icons.framesize", _v);
    }

    /**
   * Preference icons.tabSize, icon size for the icon in a tab.
   */
    public int getDefaultTabSize() {
        return BuPreferences.BU.getIntegerProperty("icons.tabsize", getDefaultSize());
    }

    public void setDefaultTabSize(int _v) {
        if (_v <= 0) BuPreferences.BU.removeProperty("icons.tabsize"); else BuPreferences.BU.putIntegerProperty("icons.tabsize", _v);
    }

    public int getDefaultCursorSize() {
        return BuPreferences.BU.getIntegerProperty("cursors.size", getDefaultSize());
    }

    public void setDefaultCursorSize(int _v) {
        if (_v <= 0) BuPreferences.BU.removeProperty("cursors.size"); else BuPreferences.BU.putIntegerProperty("cursors.size", _v);
    }

    private URL defaultBase_;

    public URL getDefaultBase() {
        return defaultBase_;
    }

    public void setDefaultBase(URL _v) {
        defaultBase_ = _v;
    }

    private BuResource parent_;

    public BuResource getParent() {
        return parent_;
    }

    public void setParent(BuResource _parent) {
        parent_ = _parent;
    }

    String family_;

    public String getIconFamily() {
        return family_;
    }

    public void setIconFamily(String _v) {
        family_ = _v;
    }

    public boolean isIconFamilyAvailable(String _name, String _ext) {
        return true;
    }

    private static Properties iconmap_;

    private static Hashtable status_;

    {
        if (iconmap_ == null) iconmap_ = new Properties();
        if (status_ == null) status_ = new Hashtable();
    }

    public URL getURL(String _path) {
        URL url = null;
        String p = _path;
        String family = getIconFamily();
        if (family != null) {
            try {
                String k = FuLib.replace(p, ".png", "");
                while (k != null) {
                    String v = iconmap_.getProperty(k);
                    if (v == null) v = k;
                    if (isIconFamilyAvailable(family, "png")) {
                        String s = family + "_" + v + ".png";
                        url = getClass().getResource(s);
                        if (url != null) return url;
                    }
                    int i = k.lastIndexOf('_');
                    k = (i >= 0) ? k.substring(0, i) : null;
                }
            } catch (SecurityException ex) {
            }
        }
        return getURL0(p);
    }

    private static final String[] EXTS = new String[] { ".gif", ".png", ".jpg" };

    protected URL getURL0(String _path) {
        URL url = null;
        String p = _path;
        try {
            p = adjust(p);
            if (getDefaultBase() != null) {
                try {
                    url = new URL(p);
                } catch (MalformedURLException ex) {
                }
            } else {
                url = getClass().getResource(p);
                if (url == null) {
                    String q = p;
                    if (q.endsWith(".png")) q = q.substring(0, p.length() - 4);
                    for (int k = 0; k < EXTS.length; k++) {
                        p = q + EXTS[k];
                        url = getClass().getResource(p);
                        if (url == null) {
                            int i, j;
                            i = p.lastIndexOf('_');
                            j = p.lastIndexOf('.');
                            p = (i >= 0 ? p.substring(0, i) : p.substring(0, j)) + "_" + getDefaultSize() + p.substring(j);
                            url = getClass().getResource(p);
                        }
                        if ((url == null) && (getDefaultSize() != 16)) {
                            int i, j;
                            i = p.lastIndexOf('_');
                            j = p.lastIndexOf('.');
                            p = (i >= 0 ? p.substring(0, i) : p.substring(0, j)) + "_16" + p.substring(j);
                            url = getClass().getResource(p);
                        }
                        if (url != null) break;
                    }
                }
            }
        } catch (Exception ex) {
            url = null;
        }
        if ((url == null) && (getParent() != null)) url = getParent().getURL0(_path);
        return url;
    }

    public String adjust(String _path) {
        String r = _path;
        if (getDefaultBase() != null) {
            String c = getClass().getName();
            int i = c.lastIndexOf('.');
            if (i >= 0) c = c.substring(0, i + 1);
            c = c.replace('.', '/');
            r = "" + getDefaultBase() + c + _path;
        }
        return r;
    }

    public InputStream getStream(String _path) {
        InputStream r = null;
        URL url = getURL(_path);
        if (url != null) {
            try {
                r = url.openStream();
            } catch (IOException ex) {
            }
        }
        return r;
    }

    public Image getImage(String _path, int _size) {
        return getIcon(_path, _size).getImage();
    }

    public Image getImage(String _path) {
        return getIcon(_path).getImage();
    }

    private static Hashtable global_ = new Hashtable(101);

    private Hashtable local_ = new Hashtable(11);

    private static Hashtable resized_ = new Hashtable(11);

    public BuIcon getIcon(String _path, int _size) {
        String p = _path.toLowerCase();
        if (p.endsWith(".gif")) p = p.substring(0, p.length() - 4) + "_" + _size + ".gif"; else if (p.endsWith(".jpg")) p = p.substring(0, p.length() - 4) + "_" + _size + ".jpg"; else if (p.endsWith(".png")) p = p.substring(0, p.length() - 4) + "_" + _size + ".png"; else p += "_" + _size + ".gif";
        return getIcon0(p, false);
    }

    public BuIcon getIcon(String _path) {
        String p = _path.toLowerCase();
        if (family_ == null && !p.endsWith(".gif") && !p.endsWith(".jpg") && !p.endsWith(".png")) p += ".png";
        return getIcon0(p, true);
    }

    private BuIcon getIcon0(String _path, boolean _family) {
        BuIcon r = (BuIcon) local_.get(_path);
        if (r == null) {
            URL u = (_family ? getURL(_path) : getURL0(_path));
            if (u != null) r = (BuIcon) global_.get(u);
            if (r == null) r = getIcon(u);
            local_.put(_path, r);
        }
        return r;
    }

    public BuIcon getMenuIcon(String _path) {
        return reduceMenuIcon(getIcon(_path));
    }

    public BuIcon getButtonIcon(String _path) {
        return reduceButtonIcon(getIcon(_path));
    }

    public BuIcon getToolIcon(String _path) {
        return reduceToolIcon(getIcon(_path));
    }

    /**
   * @deprecated use getFrameIcon() instead
   */
    public BuIcon getBarIcon(String _path) {
        return getFrameIcon(_path);
    }

    public BuIcon getFrameIcon(String _path) {
        return reduceFrameIcon(getIcon(_path));
    }

    public BuIcon getTabIcon(String _path) {
        return reduceTabIcon(getIcon(_path));
    }

    public BuIcon reduceMenuIcon(BuIcon _icon) {
        return resizeIcon(_icon, getDefaultMenuSize());
    }

    public BuIcon reduceButtonIcon(BuIcon _icon) {
        return resizeIcon(_icon, getDefaultButtonSize());
    }

    public BuIcon reduceToolIcon(BuIcon _icon) {
        return resizeIcon(_icon, getDefaultToolSize());
    }

    public BuIcon reduceFrameIcon(BuIcon _icon) {
        return resizeIcon(_icon, getDefaultFrameSize());
    }

    public BuIcon reduceTabIcon(BuIcon _icon) {
        return resizeIcon(_icon, getDefaultTabSize());
    }

    public static final BuIcon resizeIcon(final BuIcon _icon, final int _size) {
        BuIcon r = _icon;
        if ((r instanceof BuLazyIcon) && !((BuLazyIcon) _icon).isAvailable()) {
            if ((r.getIconWidth() != _size) || (r.getIconHeight() != _size)) r = new BuLazyIcon(((BuLazyIcon) _icon).getURL(), _size, _size);
            return r;
        }
        if ((r != null) && ((r.getIconWidth() != _size) || (r.getIconHeight() != _size))) {
            Hashtable t = (Hashtable) resized_.get(r);
            if (t == null) {
                t = new Hashtable(11);
                resized_.put(r, t);
            }
            Integer i = FuFactoryInteger.get(_size);
            BuIcon z = (BuIcon) t.get(i);
            if (z != null) {
                r = z;
            } else {
                try {
                    Image m = r.getImage();
                    m = m.getScaledInstance(_size, _size, Image.SCALE_SMOOTH);
                    r = new BuIcon(m);
                } catch (NullPointerException ex) {
                }
                r = BuLib.filter(r);
                String d = r.getDescription();
                r.setDescription(d + " resized to " + _size + "x" + _size);
                t.put(i, r);
            }
        }
        return r;
    }

    public BuIcon getIcon(URL _url) {
        if (_url == null) return new BuIcon();
        BuIcon r = (BuIcon) global_.get(_url);
        if (r != null) return r;
        r = new BuIcon(_url);
        if (!r.isDefault()) {
            r = BuLib.filter(r);
        }
        global_.put(_url, r);
        return r;
    }

    /**
   * Load an icon from a file which the name is given
   * by an actionCommand.
   */
    public BuIcon loadCommandIcon(String _cmd) {
        BuIcon r = null;
        String c = _cmd.toLowerCase();
        int i = c.indexOf('_');
        if (i >= 0) c = c.substring(0, i);
        r = getIcon(c);
        if ((r == null) || r.isDefault()) r = getIcon("aucun");
        return r;
    }

    /**
   * Load an icon from a file which the name is given
   * by an actionCommand and reduce it according to the
   * menu icon size.
   */
    public BuIcon loadMenuCommandIcon(String _cmd) {
        return reduceMenuIcon(loadCommandIcon(_cmd));
    }

    /**
   * Load an icon from a file which the name is given
   * by an actionCommand and reduce it according to the
   * button icon size.
   */
    public BuIcon loadButtonCommandIcon(String _cmd) {
        return reduceButtonIcon(loadCommandIcon(_cmd));
    }

    /**
   * Load an icon from a file which the name is given
   * by an actionCommand and reduce it according to the
   * tool icon size.
   */
    public BuIcon loadToolCommandIcon(String _cmd) {
        return reduceToolIcon(loadCommandIcon(_cmd));
    }

    /**
   * @deprecated
   */
    public BuIcon loadBarCommandIcon(String _cmd) {
        return loadFrameCommandIcon(_cmd);
    }

    /**
   * Load an icon from a file which the name is given
   * by an actionCommand and reduce it according to the
   * frame icon size.
   */
    public BuIcon loadFrameCommandIcon(String _cmd) {
        return reduceFrameIcon(loadCommandIcon(_cmd));
    }

    /**
   * Load an icon from a file which the name is given
   * by an actionCommand and reduce it according to the
   * tab icon size.
   */
    public BuIcon loadTabCommandIcon(String _cmd) {
        return reduceTabIcon(loadCommandIcon(_cmd));
    }

    public Cursor getCursor(String _path, int _dx, int _dy) {
        return getCursor(_path, getDefaultCursorSize(), new Point(_dx, _dy));
    }

    public Cursor getCursor(String _path, int _size, Point _spot) {
        return getCursor(_path, _size, _spot, Cursor.DEFAULT_CURSOR);
    }

    public Cursor getCursor(String _path, int _size, Point _spot, int _default) {
        Cursor r = null;
        try {
            Image i = null;
            if (_size == -1) i = getImage(_path); else i = getImage(_path, _size);
            int size = 32;
            Toolkit tk = BuLib.HELPER.getToolkit();
            i = tk.createImage(new FilteredImageSource(i.getSource(), new CropImageFilter(0, 0, size, size)));
            if (BuPreferences.BU.getBooleanProperty("cursors.monochrome", FuLib.isUnix())) i = tk.createImage(new FilteredImageSource(i.getSource(), BuFilters.BW));
            r = tk.createCustomCursor(i, _spot, _path);
        } catch (Exception ex) {
            r = Cursor.getPredefinedCursor(_default);
        }
        return r;
    }

    public final String getString(String _s) {
        String r = super.getString(_s);
        if ((_s != null) && _s.equals(r)) {
            if (getParent() != null) r = getParent().getString(_s); else r = FuResource.FU.getString(_s);
        }
        return r;
    }

    public static void main(String[] argv) {
        JFrame frame = new JFrame("test curseur");
        frame.setBounds(0, 0, 200, 200);
        frame.setVisible(true);
        frame.setCursor(BU.getCursor("voir", 10, 10));
    }
}
