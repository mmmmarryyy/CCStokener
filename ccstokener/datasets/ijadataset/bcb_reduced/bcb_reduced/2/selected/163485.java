package org.gdi3d.xnavi.viewer;

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
import javax.media.ding3d.vecmath.Vector3f;
import javax.media.ding3d.vecmath.Vector4f;
import javax.media.ding3d.utils.shader.StringIO;

public class BackgroundAppearance extends ShaderAppearance {

    private boolean initialized = false;

    private static ShaderProgram advancedSP;

    private static ShaderAttributeSet attributes;

    private static ShaderAttributeValue j3d_pos_sav;

    private static Vector4f j3d_pos = new Vector4f();

    public BackgroundAppearance() {
    }

    public void init() {
        try {
            attributes = new ShaderAttributeSet();
            attributes.setCapability(ShaderAttributeSet.ALLOW_ATTRIBUTES_READ);
            attributes.setCapability(ShaderAttributeSet.ALLOW_ATTRIBUTES_WRITE);
            j3d_pos_sav = new ShaderAttributeValue("j3d_pos", new Vector4f(j3d_pos));
            Vector<ShaderAttributeValue> attribute_values = new Vector<ShaderAttributeValue>();
            attribute_values.add(j3d_pos_sav);
            for (int i = 0; i < attribute_values.size(); i++) {
                ShaderAttributeValue value = attribute_values.get(i);
                value.setCapability(ShaderAttributeValue.ALLOW_VALUE_READ);
                value.setCapability(ShaderAttributeValue.ALLOW_VALUE_WRITE);
                attributes.put(value);
            }
            this.setShaderAttributeSet(attributes);
            if (advancedSP == null) {
                advancedSP = loadShaderProgram();
                if (advancedSP != null) {
                    advancedSP.setCapability(ShaderProgram.ALLOW_NAMES_READ);
                    advancedSP.setCapability(ShaderProgram.ALLOW_SHADERS_READ);
                    ShaderAttribute[] attrs = attributes.getAll();
                    String[] attrNames = new String[attrs.length];
                    for (int n = 0, nmax = attrs.length; n < nmax; n++) {
                        attrNames[n] = attrs[n].getAttributeName();
                    }
                    advancedSP.setShaderAttrNames(attrNames);
                }
            }
            this.setShaderProgram(advancedSP);
        } catch (Exception e) {
            e.printStackTrace();
        }
        initialized = true;
    }

    public static void setJ3DPos(Vector4f value) {
        j3d_pos = value;
        j3d_pos_sav.setValue(value);
    }

    private ShaderProgram loadShaderProgram() {
        ShaderProgram sp = null;
        String vertexProgram = null;
        String fragmentProgram = null;
        Shader[] shaders = new Shader[2];
        try {
            ClassLoader cl = this.getClass().getClassLoader();
            URL url = cl.getResource("resources/Shaders/background.vert");
            System.out.println("url " + url);
            InputStream inputSteam = cl.getResourceAsStream("resources/Shaders/background.vert");
            Reader reader = null;
            if (inputSteam != null) {
                reader = new InputStreamReader(inputSteam);
            } else {
                File file = new File("lib");
                URL url2 = new URL("jar:file:" + file.getAbsolutePath() + "/resources.jar!/resources/Shaders/background.vert");
                InputStream inputSteam2 = url2.openStream();
                reader = new InputStreamReader(inputSteam2);
            }
            char[] buffer = new char[10000];
            int len = reader.read(buffer);
            vertexProgram = new String(buffer);
            vertexProgram = vertexProgram.substring(0, len);
        } catch (Exception e) {
            System.err.println("could'nt load background.vert");
            e.printStackTrace();
        }
        try {
            ClassLoader cl = this.getClass().getClassLoader();
            URL url = cl.getResource("resources/Shaders/background.frag");
            System.out.println("url " + url);
            InputStream inputSteam = cl.getResourceAsStream("resources/Shaders/background.frag");
            Reader reader = null;
            if (inputSteam != null) {
                reader = new InputStreamReader(inputSteam);
            } else {
                File file = new File("lib");
                URL url2 = new URL("jar:file:" + file.getAbsolutePath() + "/resources.jar/resources/Shaders/background.frag");
                InputStream inputSteam2 = url2.openStream();
                reader = new InputStreamReader(inputSteam2);
            }
            char[] buffer = new char[10000];
            int len = reader.read(buffer);
            fragmentProgram = new String(buffer);
            fragmentProgram = fragmentProgram.substring(0, len);
        } catch (Exception e) {
            System.err.println("could'nt load background.frag");
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
