package com.memoire.bu;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import com.memoire.fu.FuLib;

/**
 * An ImageIcon but with always some image.
 */
public class BuIcon extends BuRobustIcon {

    public BuIcon(String _url) {
        this(FuLib.createURL(_url));
    }

    public BuIcon(final URL _url) {
        if (_url == null) {
            setDescription("null-url");
            setImage(BuLib.DEFAULT_IMAGE);
            return;
        }
        initImage(_url);
    }

    protected void initImage(URL _url) {
        String u = "" + _url;
        setDescription(u);
        Image img = null;
        if ((u.endsWith(".bmp") || u.endsWith(".BMP"))) {
            try {
                BuBmpLoader l = new BuBmpLoader();
                l.read(_url.openStream());
                img = BuLib.HELPER.getToolkit().createImage(l.getImageSource());
            } catch (Exception ex) {
            }
        }
        if (img == null) {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                InputStream in = _url.openStream();
                FuLib.copyFully(in, out);
                in.close();
                img = BuLib.HELPER.getToolkit().createImage(out.toByteArray());
            } catch (Exception ex) {
            }
        }
        setImage(img);
    }

    public BuIcon(byte[] _bytes) {
        super();
        if (_bytes != null) {
            setDescription(_bytes.length + " bytes");
            setImage(BuLib.HELPER.getToolkit().createImage(_bytes));
        } else {
            setDescription("null-" + FuLib.codeLocation());
            setImage(BuLib.DEFAULT_IMAGE);
        }
    }

    public BuIcon(Image _image) {
        super();
        setDescription("image-" + FuLib.codeLocation());
        setImage(_image);
    }

    public BuIcon() {
        super();
        setDescription("default");
        setImage(BuLib.DEFAULT_IMAGE);
    }

    private Point hotspot_ = null;

    public Point getHotSpot() {
        return hotspot_;
    }

    public void setHotSpot(Point _hotspot) {
        hotspot_ = _hotspot;
    }

    public void paintIcon(Component _c, Graphics _g, int _x, int _y) {
        if (!isDefault()) {
            BuLib.setAntialiasing(null, _g);
            if (hotspot_ != null) _g.translate(-hotspot_.x, -hotspot_.y);
            super.paintIcon(_c, _g, _x, _y);
            if (hotspot_ != null) _g.translate(hotspot_.x, hotspot_.y);
        }
    }

    public String toString() {
        String r = getDescription();
        if (r == null) r = "";
        if (isDefault()) r = "default-" + r;
        String n = getClass().getName();
        n = n.substring(n.lastIndexOf('.') + 1);
        return n + "(" + r + ")";
    }
}
