package org.gdi3d.vrmlloader.impl;

import javax.imageio.ImageIO;
import javax.media.ding3d.utils.image.ImageException;
import javax.media.ding3d.utils.image.TextureLoader;
import java.awt.*;
import java.awt.image.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import javax.media.ding3d.ImageComponent;
import javax.media.ding3d.TextureAttributes;
import javax.media.ding3d.ImageComponent2D;
import javax.media.ding3d.Texture;
import javax.media.ding3d.Texture2D;
import javax.swing.ImageIcon;
import javax.media.ding3d.vecmath.Color3f;
import javax.media.ding3d.vecmath.Color4f;
import javax.media.ding3d.vecmath.Vector3d;
import javax.media.ding3d.vecmath.Vector3f;
import org.gdi3d.vrmlloader.AdvancedAppearance;
import org.gdi3d.vrmlloader.VrmlLoader;
import org.gdi3d.vrmlloader.VrmlLoaderSettings;
import java.beans.*;

/**  Description of the Class */
public class ImageTexture extends Node implements TextureSrc {

    MFString url;

    URL urlObj;

    String encoding;

    SFBool repeatS;

    SFBool repeatT;

    Texture impl;

    Vector<Appearance> parentAppearances;

    Canvas observer = new Canvas();

    boolean transparency = false;

    PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public static final String TRANSPARENCY = "transparency";

    /**
	 *Constructor for the ImageTexture object
	 *
	 *@param  loader Description of the Parameter
	 */
    public ImageTexture(Loader loader) {
        super(loader);
        url = new MFString();
        repeatS = new SFBool(true);
        repeatT = new SFBool(true);
        initFields();
    }

    /**
	 *Constructor for the ImageTexture object
	 *
	 *@param  loader Description of the Parameter
	 *@param  url Description of the Parameter
	 *@param  repeatS Description of the Parameter
	 *@param  repeatT Description of the Parameter
	 */
    ImageTexture(Loader loader, MFString url, SFBool repeatS, SFBool repeatT) {
        super(loader);
        this.url = url;
        this.repeatS = repeatS;
        this.repeatT = repeatT;
        initFields();
    }

    public boolean equals(BaseNode other) {
        boolean result = false;
        if (other instanceof ImageTexture) {
            ImageTexture otherCast = (ImageTexture) other;
            result = url.equals(otherCast.url) && repeatS.equals(otherCast.repeatS) && repeatT.equals(otherCast.repeatT);
        }
        return result;
    }

    /**  Description of the Method */
    void initImpl() {
        impl = null;
        doChangeUrl();
        implReady = true;
    }

    /**
	 *  Description of the Method
	 *
	 *@return  Description of the Return Value
	 */
    public Object clone() {
        ImageTexture it = new ImageTexture(loader, (MFString) url.clone(), (SFBool) repeatS.clone(), (SFBool) repeatT.clone());
        return it;
    }

    /**  Sets the repeatS attribute of the ImageTexture object */
    private void setRepeatS() {
        if (repeatS.value == true) {
            impl.setBoundaryModeS(Texture.WRAP);
        } else {
            impl.setBoundaryModeS(Texture.CLAMP);
        }
    }

    /**  Sets the repeatT attribute of the ImageTexture object */
    private void setRepeatT() {
        if (repeatT.value == true) {
            impl.setBoundaryModeT(Texture.WRAP);
        } else {
            impl.setBoundaryModeT(Texture.CLAMP);
        }
    }

    /**
	 *  Description of the Method
	 *
	 *@param  eventInName Description of the Parameter
	 *@param  time Description of the Parameter
	 */
    public void notifyMethod(String eventInName, double time) {
        if (eventInName.equals("url")) {
            doChangeUrl();
        } else if (eventInName.equals("repeatS")) {
            if (impl != null) {
                setRepeatS();
            }
        } else if (eventInName.equals("repeatT")) {
            if (impl != null) {
                setRepeatT();
            }
        }
    }

    /**  Description of the Method */
    void doChangeUrl() {
        if (impl != null) {
            Texture new_texture = loadTexture();
            impl = new_texture;
            if (parentAppearances != null) {
                int numApp = parentAppearances.size();
                for (int i = 0; i < numApp; i++) {
                    Appearance app = parentAppearances.get(i);
                    if (app != null) {
                        app.impl.setTexture(impl);
                    }
                }
            }
        }
    }

    public Texture loadTexture() {
        Object key = Thread.currentThread();
        VrmlLoaderSettings settings = VrmlLoader.loaderSettings.get(key);
        if (settings == null) {
            settings = new VrmlLoaderSettings();
            VrmlLoader.loaderSettings.put(key, settings);
        }
        if (settings.loadedTextures == null) {
            settings.loadedTextures = new HashMap<String, Texture2D>();
        }
        Texture2D texture = null;
        if (url.strings != null && url.strings.length > 0) {
            for (int i = 0; i < url.strings.length; i++) {
                String urlString = url.strings[i];
                boolean hasAlpha = false;
                texture = settings.loadedTextures.get(url.strings[i]);
                if (texture == null) {
                    try {
                        TextureLoader loader = null;
                        try {
                            if (settings.loadTextures) {
                                urlObj = null;
                                try {
                                    urlObj = this.loader.stringToURL(urlString);
                                } catch (MalformedURLException url_e) {
                                    System.err.println("wrong URL format: " + urlString);
                                }
                                if (settings.verbose) System.out.println("loading texture " + urlObj + " .. ");
                                String suffix = url.strings[i].substring(url.strings[i].lastIndexOf('.') + 1).toLowerCase();
                                String ldr_format = "RGBA";
                                if (suffix.equalsIgnoreCase("jpg") || suffix.equalsIgnoreCase("jpeg") || suffix.equalsIgnoreCase("jp2") || suffix.equalsIgnoreCase("j2c")) {
                                    ldr_format = "RGB";
                                }
                                if (settings.generateMipMaps) {
                                    if (false) {
                                        loader = new TextureLoader(urlObj, ldr_format, TextureLoader.BY_REFERENCE | TextureLoader.GENERATE_MIPMAP, this.observer);
                                        if (loader != null) {
                                            texture = (Texture2D) loader.getTexture();
                                        }
                                    } else {
                                        loader = new TextureLoader(urlObj, ldr_format, TextureLoader.BY_REFERENCE, this.observer);
                                        if (loader != null) {
                                            texture = (Texture2D) loader.getTexture();
                                            texture = createMipMapTexture(texture);
                                        }
                                    }
                                    texture.setMagFilter(Texture.BASE_LEVEL_LINEAR);
                                    texture.setMinFilter(Texture.MULTI_LEVEL_LINEAR);
                                } else {
                                    encoding = settings.encoding;
                                    BufferedImage bufferedImage = (BufferedImage) java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {

                                        public Object run() {
                                            try {
                                                URLConnection connection = urlObj.openConnection();
                                                if (encoding != null) {
                                                    connection.setRequestProperty("Authorization", "Basic " + encoding);
                                                }
                                                InputStream inputStream = connection.getInputStream();
                                                BufferedImage image = ImageIO.read(inputStream);
                                                inputStream.close();
                                                return image;
                                            } catch (IOException e) {
                                                throw new ImageException(e);
                                            }
                                        }
                                    });
                                    loader = new TextureLoader(bufferedImage, ldr_format, TextureLoader.BY_REFERENCE, this.observer);
                                    if (loader != null) {
                                        texture = (Texture2D) loader.getTexture();
                                    }
                                }
                            } else {
                            }
                        } catch (javax.media.ding3d.utils.image.ImageException img_e) {
                            img_e.printStackTrace();
                        }
                        if (texture != null) {
                            texture.setUserData(urlString);
                            texture.setCapability(Texture2D.ALLOW_ENABLE_WRITE);
                            ImageComponent[] images = texture.getImages();
                            for (ImageComponent image : images) {
                                image.setCapability(ImageComponent.ALLOW_IMAGE_WRITE);
                            }
                            if (repeatS.value == true) {
                                texture.setBoundaryModeS(Texture.WRAP);
                            } else {
                                texture.setBoundaryModeS(Texture.CLAMP_TO_EDGE);
                            }
                            if (repeatT.value == true) {
                                texture.setBoundaryModeT(Texture.WRAP);
                            } else {
                                texture.setBoundaryModeT(Texture.CLAMP_TO_EDGE);
                            }
                            texture.setMagFilter(Texture2D.NICEST);
                            if (settings.loadTextures) {
                                ImageComponent2D imageComponent = (ImageComponent2D) texture.getImage(0);
                                ColorModel cm = imageComponent.getImage().getColorModel();
                                hasAlpha = cm.hasAlpha();
                                this.setTransparency(hasAlpha);
                                if (settings.anisotropicFiltering) {
                                    texture.setAnisotropicFilterMode(Texture2D.ANISOTROPIC_SINGLE_VALUE);
                                } else {
                                    texture.setAnisotropicFilterMode(Texture2D.ANISOTROPIC_NONE);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        texture = null;
                    }
                    if (settings.verbose && settings.loadTextures) {
                        if (texture != null) System.out.println("ok."); else System.out.println("failed.");
                    }
                    settings.loadedTextures.put(urlString, (Texture2D) texture);
                    break;
                } else {
                    ImageComponent2D imageComponent = (ImageComponent2D) texture.getImage(0);
                    ColorModel cm = imageComponent.getImage().getColorModel();
                    hasAlpha = cm.hasAlpha();
                    this.setTransparency(hasAlpha);
                }
            }
        }
        if (texture != null) {
            texture.setEnable(true);
        }
        return texture;
    }

    private Texture2D createMipMapTexture(Texture2D texture) {
        Texture2D new_texture = null;
        int tex_format = texture.getFormat();
        int imageWidth = texture.getWidth();
        int imageHeight = texture.getHeight();
        int ic_format = ImageComponent2D.FORMAT_RGB;
        int bi_format = BufferedImage.TYPE_3BYTE_BGR;
        if (tex_format == Texture2D.RGBA) {
            ic_format = ImageComponent2D.FORMAT_RGBA;
            bi_format = BufferedImage.TYPE_4BYTE_ABGR;
        }
        ImageComponent2D image_component = (ImageComponent2D) texture.getImage(0);
        new_texture = new Texture2D(Texture.MULTI_LEVEL_MIPMAP, tex_format, imageWidth, imageHeight);
        new_texture.setCapability(Texture.ALLOW_ENABLE_WRITE);
        int imageLevel = 0;
        BufferedImage image = null;
        BufferedImage org_image = image_component.getImage();
        if (org_image.getWidth() != imageWidth || org_image.getHeight() != imageHeight) {
            VrmlLoaderSettings settings = VrmlLoader.loaderSettings.get(Thread.currentThread());
            if (settings.verbose) System.out.print(" reducing.. ");
            BufferedImage image_temp = new BufferedImage(imageWidth, imageHeight, bi_format);
            Graphics2D graphics2D = image_temp.createGraphics();
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics2D.drawImage(org_image, 0, 0, imageWidth, imageHeight, null);
            image = image_temp;
        } else image = org_image;
        image_component = null;
        ImageComponent2D base_image_component = new ImageComponent2D(ic_format, image);
        base_image_component.setCapability(ImageComponent2D.ALLOW_IMAGE_READ);
        new_texture.setImage(imageLevel, base_image_component);
        while (imageWidth > 1 || imageHeight > 1) {
            imageLevel++;
            if (imageWidth > 1) imageWidth /= 2;
            if (imageHeight > 1) imageHeight /= 2;
            BufferedImage image_temp = new BufferedImage(imageWidth, imageHeight, bi_format);
            Graphics2D graphics2D = image_temp.createGraphics();
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics2D.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            graphics2D.drawImage(image, 0, 0, imageWidth, imageHeight, null);
            ImageComponent2D mipmap_image_component = new ImageComponent2D(ic_format, image_temp);
            mipmap_image_component.setCapability(ImageComponent2D.ALLOW_IMAGE_READ);
            new_texture.setImage(imageLevel, mipmap_image_component);
            image = image_temp;
        }
        new_texture.setMagFilter(Texture.BASE_LEVEL_LINEAR);
        new_texture.setMinFilter(Texture.MULTI_LEVEL_LINEAR);
        return new_texture;
    }

    public boolean getTransparency() {
        return transparency;
    }

    public void setTransparency(boolean value) {
        transparency = value;
    }

    public void addPropertyChangeListener(String n, PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(n, l);
    }

    public void removePropertyChangeListener(String n, PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(n, l);
    }

    /**
	 *  Gets the implTexture attribute of the ImageTexture object
	 *
	 *@return  The implTexture value
	 */
    public Texture getImplTexture() {
        return impl;
    }

    /**
	 *  Gets the type attribute of the ImageTexture object
	 *
	 *@return  The type value
	 */
    public String getType() {
        return "ImageTexture";
    }

    /**  Description of the Method */
    void initFields() {
        url.init(this, FieldSpec, Field.EXPOSED_FIELD, "url");
        repeatS.init(this, FieldSpec, Field.EXPOSED_FIELD, "repeatS");
        repeatT.init(this, FieldSpec, Field.EXPOSED_FIELD, "repeatT");
    }

    public MFString getUrl() {
        return url;
    }

    public Canvas getObserver() {
        return observer;
    }
}
