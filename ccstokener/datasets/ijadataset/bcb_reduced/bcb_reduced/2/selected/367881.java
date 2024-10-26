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
import javax.media.ding3d.utils.shader.StringIO;

public class GoochAppearance extends ShaderAppearance {

    private boolean initialized = false;

    private boolean textured = false;

    private static ShaderProgram shaderProgram;

    public GoochAppearance() {
    }

    public void init() {
        try {
            ShaderAttributeSet attributes = new ShaderAttributeSet();
            attributes.setCapability(ShaderAttributeSet.ALLOW_ATTRIBUTES_READ);
            attributes.setCapability(ShaderAttributeSet.ALLOW_ATTRIBUTES_WRITE);
            Vector<ShaderAttributeValue> attribute_values = new Vector<ShaderAttributeValue>();
            for (int i = 0; i < attribute_values.size(); i++) {
                ShaderAttributeValue value = attribute_values.get(i);
                value.setCapability(ShaderAttributeValue.ALLOW_VALUE_READ);
                value.setCapability(ShaderAttributeValue.ALLOW_VALUE_WRITE);
                attributes.put(value);
            }
            this.setShaderAttributeSet(attributes);
            if (shaderProgram == null) {
                ResourceLoader rl = new ResourceLoader();
                shaderProgram = loadShaderProgram();
                if (shaderProgram != null) {
                    shaderProgram.setCapability(ShaderProgram.ALLOW_NAMES_READ);
                    shaderProgram.setCapability(ShaderProgram.ALLOW_SHADERS_READ);
                    ShaderAttribute[] attrs = attributes.getAll();
                    String[] attrNames = new String[attrs.length];
                    for (int n = 0, nmax = attrs.length; n < nmax; n++) {
                        attrNames[n] = attrs[n].getAttributeName();
                    }
                    shaderProgram.setShaderAttrNames(attrNames);
                }
            }
            this.setShaderProgram(shaderProgram);
        } catch (Exception e) {
            e.printStackTrace();
        }
        initialized = true;
    }

    public boolean getTextured() {
        return this.textured;
    }

    public void setTextured(boolean value) {
        this.textured = value;
    }

    private ShaderProgram loadShaderProgram() {
        ShaderProgram sp = null;
        String vertexProgram = null;
        String fragmentProgram = null;
        Shader[] shaders = new Shader[2];
        try {
            ClassLoader cl = this.getClass().getClassLoader();
            URL url = cl.getResource("Shaders/gooch.vert");
            System.out.println("url " + url);
            InputStream inputSteam = cl.getResourceAsStream("Shaders/gooch.vert");
            Reader reader = null;
            if (inputSteam != null) {
                reader = new InputStreamReader(inputSteam);
            } else {
                File file = new File("lib");
                URL url2 = new URL("jar:file:" + file.getAbsolutePath() + "/j3d-vrml97-i3mainz.jar!/Shaders/gooch.vert");
                InputStream inputSteam2 = url2.openStream();
                reader = new InputStreamReader(inputSteam2);
            }
            char[] buffer = new char[10000];
            int len = reader.read(buffer);
            vertexProgram = new String(buffer);
            vertexProgram = vertexProgram.substring(0, len);
        } catch (Exception e) {
            System.err.println("could'nt load gooch.vert");
            e.printStackTrace();
        }
        try {
            ClassLoader cl = this.getClass().getClassLoader();
            URL url = cl.getResource("Shaders/gooch.frag");
            System.out.println("url " + url);
            InputStream inputSteam = cl.getResourceAsStream("Shaders/gooch.frag");
            Reader reader = null;
            if (inputSteam != null) {
                reader = new InputStreamReader(inputSteam);
            } else {
                File file = new File("lib");
                URL url2 = new URL("jar:file:" + file.getAbsolutePath() + "/j3d-vrml97-i3mainz.jar!/Shaders/gooch.frag");
                InputStream inputSteam2 = url2.openStream();
                reader = new InputStreamReader(inputSteam2);
            }
            char[] buffer = new char[10000];
            int len = reader.read(buffer);
            fragmentProgram = new String(buffer);
            fragmentProgram = fragmentProgram.substring(0, len);
        } catch (Exception e) {
            System.err.println("could'nt load gooch.frag");
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
