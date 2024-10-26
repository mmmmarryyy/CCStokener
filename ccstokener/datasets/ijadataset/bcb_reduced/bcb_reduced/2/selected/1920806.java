package org.gdi3d.vrmlloader;

import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.CharBuffer;
import java.util.Vector;
import javax.media.ding3d.*;
import javax.media.ding3d.vecmath.Color4f;
import javax.media.ding3d.vecmath.Matrix4d;
import javax.media.ding3d.vecmath.Matrix4f;
import javax.media.ding3d.vecmath.Vector3f;
import javax.media.ding3d.vecmath.Vector4f;
import javax.media.ding3d.utils.shader.StringIO;

public class SimpleAppearance extends ShaderAppearance {

    private boolean initialized = false;

    private static ShaderProgram simpleSP;

    private static ShaderAttributeSet attributes;

    private static ShaderAttributeValue SAV_inverseViewTrans;

    private static ShaderAttributeValue SAV_new2OldT;

    private static ShaderAttributeValue SAV_vTrans;

    private static float lightBrightness = 1.367f;

    private static float directLight = 0.4f;

    private static float diffuseLight = 0.6f;

    private static float ambientMin = 0.5f;

    private static Color4f ambientLightColor = new Color4f(1.0f, 1.0f, 1.0f, 1.0f);

    public SimpleAppearance() {
    }

    static {
        System.out.println("SimpleAppearance static inititlizer");
        SAV_inverseViewTrans = new ShaderAttributeValue("inverseViewTrans", new Matrix4f());
        SAV_new2OldT = new ShaderAttributeValue("new2OldT", new Matrix4f());
        SAV_vTrans = new ShaderAttributeValue("vTrans", new Matrix4f());
        attributes = new ShaderAttributeSet();
        attributes.setCapability(ShaderAttributeSet.ALLOW_ATTRIBUTES_READ);
        attributes.setCapability(ShaderAttributeSet.ALLOW_ATTRIBUTES_WRITE);
        Vector<ShaderAttributeValue> attribute_values = new Vector<ShaderAttributeValue>();
        attribute_values.add(SAV_inverseViewTrans);
        attribute_values.add(SAV_new2OldT);
        attribute_values.add(SAV_vTrans);
        attribute_values.add(new ShaderAttributeValue("light_brightness", new Float(lightBrightness)));
        attribute_values.add(new ShaderAttributeValue("direct_light", new Float(directLight)));
        attribute_values.add(new ShaderAttributeValue("diffuse_light", new Float(diffuseLight)));
        attribute_values.add(new ShaderAttributeValue("ambient_min", new Float(ambientMin)));
        attribute_values.add(new ShaderAttributeValue("ambient_light_color", new Color4f(ambientLightColor)));
        for (int i = 0; i < attribute_values.size(); i++) {
            ShaderAttributeValue value = attribute_values.get(i);
            value.setCapability(ShaderAttributeValue.ALLOW_VALUE_READ);
            value.setCapability(ShaderAttributeValue.ALLOW_VALUE_WRITE);
            attributes.put(value);
        }
    }

    public void init() {
        try {
            this.setShaderAttributeSet(attributes);
            if (simpleSP == null) {
                ResourceLoader rl = new ResourceLoader();
                simpleSP = loadShaderProgram();
                if (simpleSP != null) {
                    simpleSP.setCapability(ShaderProgram.ALLOW_NAMES_READ);
                    simpleSP.setCapability(ShaderProgram.ALLOW_SHADERS_READ);
                    ShaderAttribute[] attrs = attributes.getAll();
                    String[] attrNames = new String[attrs.length];
                    for (int n = 0, nmax = attrs.length; n < nmax; n++) {
                        attrNames[n] = attrs[n].getAttributeName();
                    }
                    simpleSP.setShaderAttrNames(attrNames);
                }
            }
            this.setShaderProgram(simpleSP);
        } catch (Exception e) {
            e.printStackTrace();
        }
        initialized = true;
    }

    public static void setInverseViewTrans(Matrix4d value) {
        SAV_inverseViewTrans.setValue(new Matrix4f(value));
    }

    public static void setnew2OldT(Matrix4d value) {
        SAV_new2OldT.setValue(new Matrix4f(value));
    }

    public static void setVTrans(Matrix4d value) {
        SAV_vTrans.setValue(new Matrix4f(value));
    }

    private ShaderProgram loadShaderProgram() {
        ShaderProgram sp = null;
        String vertexProgram = null;
        String fragmentProgram = null;
        Shader[] shaders = new Shader[2];
        try {
            ClassLoader cl = this.getClass().getClassLoader();
            URL url = cl.getResource("Shaders/simple.vert");
            System.out.println("url " + url);
            InputStream inputSteam = cl.getResourceAsStream("Shaders/simple.vert");
            Reader reader = null;
            if (inputSteam != null) {
                reader = new InputStreamReader(inputSteam);
            } else {
                File file = new File("lib");
                URL url2 = new URL("jar:file:" + file.getAbsolutePath() + "/j3d-vrml97-i3mainz.jar!/Shaders/simple.vert");
                InputStream inputSteam2 = url2.openStream();
                reader = new InputStreamReader(inputSteam2);
            }
            char[] buffer = new char[10000];
            int len = reader.read(buffer);
            vertexProgram = new String(buffer);
            vertexProgram = vertexProgram.substring(0, len);
        } catch (Exception e) {
            System.err.println("could'nt load simple.vert");
            e.printStackTrace();
        }
        try {
            ClassLoader cl = this.getClass().getClassLoader();
            URL url = cl.getResource("Shaders/simple.frag");
            System.out.println("url " + url);
            InputStream inputSteam = cl.getResourceAsStream("Shaders/simple.frag");
            Reader reader = null;
            if (inputSteam != null) {
                reader = new InputStreamReader(inputSteam);
            } else {
                File file = new File("lib");
                URL url2 = new URL("jar:file:" + file.getAbsolutePath() + "/j3d-vrml97-i3mainz.jar!/Shaders/simple.frag");
                InputStream inputSteam2 = url2.openStream();
                reader = new InputStreamReader(inputSteam2);
            }
            char[] buffer = new char[10000];
            int len = reader.read(buffer);
            fragmentProgram = new String(buffer);
            fragmentProgram = fragmentProgram.substring(0, len);
        } catch (Exception e) {
            System.err.println("could'nt load simple.frag");
            e.printStackTrace();
        }
        if (vertexProgram != null && fragmentProgram != null) {
            shaders[0] = new SourceCodeShader(Shader.SHADING_LANGUAGE_GLSL, Shader.SHADER_TYPE_VERTEX, vertexProgram);
            shaders[1] = new SourceCodeShader(Shader.SHADING_LANGUAGE_GLSL, Shader.SHADER_TYPE_FRAGMENT, fragmentProgram);
            sp = new GLSLShaderProgram();
            sp.setShaders(shaders);
        }
        return sp;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
