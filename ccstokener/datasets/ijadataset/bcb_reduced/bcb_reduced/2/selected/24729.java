package de.grogra.ext.x3d.objects;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.vecmath.Tuple2f;
import javax.vecmath.Vector2f;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import de.grogra.ext.x3d.Util;
import de.grogra.ext.x3d.X3DExport;
import de.grogra.ext.x3d.X3DImport;
import de.grogra.ext.x3d.interfaces.Definable;
import de.grogra.graph.GraphState;
import de.grogra.imp.io.ImageReader;
import de.grogra.imp.objects.FixedImageAdapter;
import de.grogra.imp.objects.ImageAdapter;
import de.grogra.imp3d.objects.ShadedNull;
import de.grogra.imp3d.shading.AffineUVTransformation;
import de.grogra.imp3d.shading.ImageMap;
import de.grogra.imp3d.shading.Phong;
import de.grogra.imp3d.shading.RGBAShader;
import de.grogra.imp3d.shading.Shader;
import de.grogra.imp3d.shading.SurfaceMap;
import de.grogra.math.ChannelMap;
import de.grogra.math.ColorMap;
import de.grogra.math.Graytone;
import de.grogra.math.RGBColor;
import de.grogra.persistence.SCOType;
import de.grogra.persistence.ShareableBase;
import de.grogra.persistence.SharedObjectProvider;
import de.grogra.pf.registry.Item;
import de.grogra.pf.ui.Workbench;
import de.grogra.pf.ui.registry.FileFactory;

/**
 * Appearance class. Saves the attributes of an x3d appearance element.
 * Provides static methods for import and export tasks.
 * 
 * @author Udo Bischof, Uwe Mannl
 *
 */
public class X3DAppearance extends ShareableBase implements Definable {

    protected X3DMaterial material = null;

    protected X3DTexture texture = null;

    protected X3DTextureTransform textureTransform = null;

    protected String def = null;

    protected String use = null;

    public X3DAppearance() {
        super();
    }

    public X3DMaterial getMaterial() {
        return material;
    }

    public void setMaterial(X3DMaterial material) {
        this.material = material;
    }

    public X3DTexture getTexture() {
        return texture;
    }

    public void setTexture(X3DTexture texture) {
        this.texture = texture;
    }

    public X3DTextureTransform getTextureTransform() {
        return textureTransform;
    }

    public void setTextureTransform(X3DTextureTransform textureTransform) {
        this.textureTransform = textureTransform;
    }

    public String getDef() {
        return def;
    }

    public String getUse() {
        return use;
    }

    /**
	 * Creates a new instance of this class. X3D attributes are read and set in
	 * corresponding class attributes.
	 * @param atts
	 * @return
	 */
    public static X3DAppearance createInstance(Attributes atts) {
        X3DAppearance newAppearance = new X3DAppearance();
        String valueString;
        valueString = atts.getValue("DEF");
        newAppearance.def = valueString;
        valueString = atts.getValue("USE");
        newAppearance.use = valueString;
        return newAppearance;
    }

    /**
	 * Applies the material and texture from appearance to a given node.
	 * @param node Node for which the material is to set
	 * @param appearance Material (appearance node in x3d) to set
	 */
    public static void applyMaterial(ShadedNull node, X3DAppearance appearance) {
        if (appearance != null) {
            Phong p = null;
            if ((appearance.getMaterial() == null) && (appearance.getTexture() != null)) {
                p = new Phong();
                applyTexture(node, appearance.getTexture(), appearance, p);
                node.setShader(p);
            } else if (appearance.getMaterial() != null) {
                p = new Phong();
                p.setDiffuse(new RGBColor(appearance.getMaterial().getDiffuseColor()));
                if (appearance.getTexture() != null) {
                    applyTexture(node, appearance.getTexture(), appearance, p);
                }
                p.setAmbient(new Graytone(appearance.getMaterial().getAmbientIntensity()));
                p.setEmissive(new RGBColor(appearance.getMaterial().getEmissiveColor()));
                p.setShininess(new Graytone(appearance.getMaterial().getShininess()));
                p.setSpecular(new RGBColor(appearance.getMaterial().getSpecularColor()));
                p.setTransparency(new Graytone(appearance.getMaterial().getTransparency()));
                node.setShader(p);
            }
        }
    }

    /**
	 * Applies the texture from the texture node to a given node. Textures in GroIMP can
	 * only by applied to shaders like phong.
	 * @param node Node for which the material is to set
	 * @param texture Texture (texture node in x3d) to set
	 * @param phong Shader for setting texture
	 */
    public static void applyTexture(ShadedNull node, X3DTexture texture, X3DAppearance appearance, Phong phong) {
        if (texture instanceof X3DImageTexture) {
            X3DImageTexture imgTex = (X3DImageTexture) texture;
            ImageAdapter ia = null;
            boolean loadedFromWeb = false;
            File f = null;
            if (imgTex.getImg() == null) {
                URL url = null;
                Workbench wb = Workbench.current();
                int imageCount = imgTex.getUrl().length;
                for (int imageIndex = 0; imageIndex < imageCount; imageIndex++) {
                    try {
                        String imgpath = imgTex.getUrl()[imageIndex];
                        if (imgpath.toLowerCase().startsWith("http://")) {
                            String filename = imgpath.substring(imgpath.lastIndexOf("/") + 1, imgpath.lastIndexOf("."));
                            String fileext = imgpath.substring(imgpath.lastIndexOf("."), imgpath.length());
                            f = File.createTempFile(filename, fileext);
                            url = new URL(imgpath);
                            InputStream is = url.openStream();
                            FileOutputStream os = new FileOutputStream(f);
                            byte[] buffer = new byte[0xFFFF];
                            for (int len; (len = is.read(buffer)) != -1; ) os.write(buffer, 0, len);
                            is.close();
                            os.close();
                            url = f.toURI().toURL();
                            loadedFromWeb = true;
                        } else {
                            if (imgpath.startsWith("/") || (imgpath.charAt(1) == ':')) {
                            } else {
                                URL x3durl = X3DImport.getTheImport().getUrl();
                                imgpath = Util.getRealPath(x3durl) + imgpath;
                            }
                            f = new File(imgpath);
                            url = f.toURI().toURL();
                            Object testContent = url.getContent();
                            if (testContent == null) continue;
                            loadedFromWeb = false;
                        }
                        FileFactory ff = ImageReader.getFactory(wb.getRegistry());
                        ia = (FixedImageAdapter) ff.addFromURL(wb.getRegistry(), url, null, wb);
                        if (ia != null) break;
                    } catch (MalformedURLException e) {
                    } catch (IOException e) {
                    } finally {
                        if (loadedFromWeb && f != null) {
                            f.delete();
                        }
                    }
                }
                if (ia == null) return;
                imgTex.setImg(ia);
            } else {
                ia = imgTex.getImg();
            }
            ImageMap i = new ImageMap();
            i.setImageAdapter(ia);
            phong.setDiffuse(i);
            if (appearance.getTextureTransform() != null) {
                X3DTextureTransform texTrans = appearance.getTextureTransform();
                AffineUVTransformation uvTrans = new AffineUVTransformation();
                uvTrans.setAngle(-texTrans.getRotation());
                uvTrans.setOffsetU(-texTrans.getTranslation().x);
                uvTrans.setOffsetV(-texTrans.getTranslation().y);
                uvTrans.setScaleU(texTrans.getScale().x);
                uvTrans.setScaleV(texTrans.getScale().y);
                i.setInput(uvTrans);
            }
        }
    }

    /**
	 * Handles the appearance of a groimp node and returns a x3d appearance
	 * element with corresponding attributes.
	 * @param node
	 * @param export
	 * @return
	 */
    public static Element handleAppearance(Object node, Shader shader, X3DExport export, GraphState gs) {
        X3DMaterial defaultMaterial = new X3DMaterial();
        Element appElement = export.getDoc().createElement("Appearance");
        if (shader == null) {
            Element mat = export.getDoc().createElement("Material");
            appElement.appendChild(mat);
        } else if (shader instanceof RGBAShader) {
            RGBAShader rgba = (RGBAShader) shader;
            Element mat = export.getDoc().createElement("Material");
            mat.setAttribute("diffuseColor", rgba.x + " " + rgba.y + " " + rgba.z);
            if (1.0f - rgba.w != defaultMaterial.getTransparency()) mat.setAttribute("transparency", String.valueOf(1.0f - rgba.w));
            appElement.appendChild(mat);
        } else if (shader instanceof Phong) {
            Phong p = (Phong) shader;
            Element mat = export.getDoc().createElement("Material");
            ChannelMap c = p.getAmbient();
            if (c == null) {
                c = Phong.DEFAULT_AMBIENT;
            }
            if (c instanceof ColorMap) {
                int color = ((ColorMap) c).getAverageColor();
                float gray = intToGray(color);
                if (gray != defaultMaterial.getAmbientIntensity()) mat.setAttribute("ambientIntensity", String.valueOf(gray));
            }
            c = p.getDiffuse();
            if (c == null) {
                c = Phong.DEFAULT_DIFFUSE;
            }
            if (c instanceof ColorMap) {
                int color = ((ColorMap) c).getAverageColor();
                RGBColor rgb = intToRGB(color);
                if (!rgb.equals(defaultMaterial.getDiffuseColor())) mat.setAttribute("diffuseColor", rgb.x + " " + rgb.y + " " + rgb.z);
            } else if (c instanceof SurfaceMap) {
                try {
                    String s = exportChannelMap(c, export, appElement);
                    Element imgTexElement = export.getDoc().createElement("ImageTexture");
                    imgTexElement.setAttribute("url", s);
                    appElement.appendChild(imgTexElement);
                } catch (Exception e) {
                    System.err.println("Couldn't write image file for x3d export.");
                }
            }
            c = p.getEmissive();
            if (c == null) {
                c = Phong.DEFAULT_EMISSIVE;
            }
            if (c instanceof ColorMap) {
                int color = ((ColorMap) c).getAverageColor();
                RGBColor rgb = intToRGB(color);
                if (!rgb.equals(defaultMaterial.getEmissiveColor())) mat.setAttribute("emissiveColor", rgb.x + " " + rgb.y + " " + rgb.z);
            }
            c = p.getShininess();
            if (c == null) {
                c = new Graytone(0.5f);
            }
            if (c instanceof ColorMap) {
                int color = ((ColorMap) c).getAverageColor();
                float gray = intToGray(color);
                if (gray != defaultMaterial.getShininess()) mat.setAttribute("shininess", String.valueOf(gray));
            }
            c = p.getSpecular();
            if (c == null) {
                c = Phong.DEFAULT_SPECULAR;
            }
            if (c instanceof ColorMap) {
                int color = ((ColorMap) c).getAverageColor();
                RGBColor rgb = intToRGB(color);
                if (!rgb.equals(defaultMaterial.getSpecularColor())) mat.setAttribute("specularColor", rgb.x + " " + rgb.y + " " + rgb.z);
            }
            c = p.getTransparency();
            if (c == null) {
                c = Phong.DEFAULT_TRANSPARENCY;
            }
            if (c instanceof ColorMap) {
                int color = ((ColorMap) c).getAverageColor();
                float gray = intToGray(color);
                if (gray != defaultMaterial.getTransparency()) mat.setAttribute("transparency", String.valueOf(gray));
            }
            appElement.appendChild(mat);
        }
        return appElement;
    }

    private static String exportChannelMap(ChannelMap c, X3DExport export, Element appElement) throws IOException {
        String fileName = null;
        String fileType = "png";
        String imgName = "surfacemap." + fileType;
        SurfaceMap sm = (SurfaceMap) c;
        ChannelMap in = sm.getInput();
        AffineUVTransformation t;
        boolean generateImage;
        if (in instanceof AffineUVTransformation) {
            t = (AffineUVTransformation) in;
            if (t.getInput() != null) {
                t = null;
                generateImage = true;
            } else {
                generateImage = false;
            }
        } else if (in != null) {
            generateImage = true;
            t = null;
        } else {
            generateImage = false;
            t = null;
        }
        BufferedImage img;
        if (generateImage) {
            img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            sm.drawImage(img, 1, true);
        } else if (sm instanceof ImageMap) {
            ImageAdapter a = ((ImageMap) sm).getImageAdapter();
            img = a.getBufferedImage();
            SharedObjectProvider sop = a.getProvider();
            String itemName = ((Item) sop).getAbsoluteName();
            itemName = itemName.substring(itemName.lastIndexOf("/") + 1, itemName.length());
            imgName = itemName + "." + fileType;
        } else {
            img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            sm.drawImage(img, 1, false);
        }
        if (img != null) {
            File file = (File) export.getFile(imgName);
            ImageIO.write(img, fileType, file);
            String s = file.getCanonicalPath();
            int pathEnd = s.lastIndexOf(File.separator) + 1;
            fileName = s.substring(pathEnd, s.length());
        }
        if (t != null) {
            X3DTextureTransform defaultTextureTransform = new X3DTextureTransform();
            Element texTrans = export.getDoc().createElement("TextureTransform");
            float angle = t.getAngle();
            if (angle != defaultTextureTransform.getRotation()) texTrans.setAttribute("rotation", String.valueOf(-angle));
            Tuple2f translation = new Vector2f(t.getOffsetU(), t.getOffsetV());
            if (!translation.equals(defaultTextureTransform.getTranslation())) texTrans.setAttribute("translation", -translation.x + " " + -translation.y);
            Tuple2f scale = new Vector2f(t.getScaleU(), t.getScaleV());
            if (!scale.equals(defaultTextureTransform.getScale())) texTrans.setAttribute("scale", scale.x + " " + scale.y);
            appElement.appendChild(texTrans);
        }
        return fileName;
    }

    static RGBColor intToRGB(int color) {
        float r = ((color >> 16) & 255) * (1f / 255);
        float g = ((color >> 8) & 255) * (1f / 255);
        float b = (color & 255) * (1f / 255);
        return new RGBColor(r, g, b);
    }

    static float intToGray(int color) {
        float r = ((color >> 16) & 255) * (1f / 255);
        float g = ((color >> 8) & 255) * (1f / 255);
        float b = (color & 255) * (1f / 255);
        return (r + g + b) / 3f;
    }

    public static final Type $TYPE;

    public static class Type extends SCOType {

        public Type(Class c, de.grogra.persistence.SCOType supertype) {
            super(c, supertype);
        }

        public Type(X3DAppearance representative, de.grogra.persistence.SCOType supertype) {
            super(representative, supertype);
        }

        Type(Class c) {
            super(c, SCOType.$TYPE);
        }

        static Field _addManagedField(Type t, String name, int modifiers, de.grogra.reflect.Type type, de.grogra.reflect.Type componentType, int id) {
            return t.addManagedField(name, modifiers, type, componentType, id);
        }

        @Override
        public Object newInstance() {
            return new X3DAppearance();
        }
    }

    public de.grogra.persistence.ManageableType getManageableType() {
        return $TYPE;
    }

    static {
        $TYPE = new Type(X3DAppearance.class);
        $TYPE.validate();
    }
}
