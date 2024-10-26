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

public class AdvancedAppearance extends ShaderAppearance {

    private boolean initialized = false;

    private float lightBrightness = 1.0f;

    private float directLight = 0.7f;

    private float diffuseLight = 0.3f;

    private Color4f ambientLightColor = new Color4f(0.98f, 1.0f, 1.02f, 1.0f);

    private boolean textured = false;

    private static ShaderProgram advancedSP;

    public AdvancedAppearance() {
    }

    public void init() {
        try {
            ShaderAttributeSet attributes = new ShaderAttributeSet();
            attributes.setCapability(ShaderAttributeSet.ALLOW_ATTRIBUTES_READ);
            attributes.setCapability(ShaderAttributeSet.ALLOW_ATTRIBUTES_WRITE);
            Vector<ShaderAttributeValue> attribute_values = new Vector<ShaderAttributeValue>();
            attribute_values.add(new ShaderAttributeValue("textured", new Integer(0)));
            attribute_values.add(new ShaderAttributeValue("light_brightness", new Float(lightBrightness)));
            attribute_values.add(new ShaderAttributeValue("direct_light", new Float(directLight)));
            attribute_values.add(new ShaderAttributeValue("diffuse_light", new Float(diffuseLight)));
            attribute_values.add(new ShaderAttributeValue("ambient_light_color", new Color4f(ambientLightColor)));
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

    public float getLightBrightness() {
        return this.lightBrightness;
    }

    public void setLightBrightness(float value) {
        this.lightBrightness = value;
        ShaderAttributeSet attributes = this.getShaderAttributeSet();
        ShaderAttributeValue attr_value = (ShaderAttributeValue) attributes.get("light_brightness");
        attr_value.setValue(new Float(value));
    }

    public float getDirectLight() {
        return this.directLight;
    }

    public void setDirectLight(float value) {
        this.directLight = value;
        ShaderAttributeSet attributes = this.getShaderAttributeSet();
        ShaderAttributeValue attr_value = (ShaderAttributeValue) attributes.get("direct_light");
        attr_value.setValue(new Float(value));
    }

    public float getDiffuseLight() {
        return this.diffuseLight;
    }

    public void setDiffuseLight(float value) {
        this.diffuseLight = value;
        ShaderAttributeSet attributes = this.getShaderAttributeSet();
        ShaderAttributeValue attr_value = (ShaderAttributeValue) attributes.get("diffuse_light");
        attr_value.setValue(new Float(value));
    }

    public Color4f getAmbientLightColor() {
        return this.ambientLightColor;
    }

    public void setAmbientLightColor(Color4f value) {
        this.ambientLightColor = value;
        ShaderAttributeSet attributes = this.getShaderAttributeSet();
        ShaderAttributeValue attr_value = (ShaderAttributeValue) attributes.get("ambient_light_color");
        attr_value.setValue(new Color4f(value));
    }

    public boolean getTextured() {
        return this.textured;
    }

    public void setTextured(boolean value) {
        this.textured = value;
        ShaderAttributeSet attributes = this.getShaderAttributeSet();
        ShaderAttributeValue attr_value = (ShaderAttributeValue) attributes.get("textured");
        if (value == true) attr_value.setValue(new Integer(1)); else attr_value.setValue(new Integer(0));
    }

    private ShaderProgram loadShaderProgram() {
        ShaderProgram sp = null;
        String vertexProgram = null;
        String fragmentProgram = null;
        Shader[] shaders = new Shader[2];
        try {
            ClassLoader cl = this.getClass().getClassLoader();
            URL url = cl.getResource("Shaders/advanced.vert");
            System.out.println("url " + url);
            InputStream inputSteam = cl.getResourceAsStream("Shaders/advanced.vert");
            Reader reader = null;
            if (inputSteam != null) {
                reader = new InputStreamReader(inputSteam);
            } else {
                File file = new File("lib");
                URL url2 = new URL("jar:file:" + file.getAbsolutePath() + "/j3d-vrml97-i3mainz.jar!/Shaders/advanced.vert");
                InputStream inputSteam2 = url2.openStream();
                reader = new InputStreamReader(inputSteam2);
            }
            char[] buffer = new char[10000];
            int len = reader.read(buffer);
            vertexProgram = new String(buffer);
            vertexProgram = vertexProgram.substring(0, len);
        } catch (Exception e) {
            System.err.println("could'nt load advanced.vert");
            e.printStackTrace();
        }
        try {
            ClassLoader cl = this.getClass().getClassLoader();
            URL url = cl.getResource("Shaders/advanced.frag");
            System.out.println("url " + url);
            InputStream inputSteam = cl.getResourceAsStream("Shaders/advanced.frag");
            Reader reader = null;
            if (inputSteam != null) {
                reader = new InputStreamReader(inputSteam);
            } else {
                File file = new File("lib");
                URL url2 = new URL("jar:file:" + file.getAbsolutePath() + "/j3d-vrml97-i3mainz.jar!/Shaders/advanced.frag");
                InputStream inputSteam2 = url2.openStream();
                reader = new InputStreamReader(inputSteam2);
            }
            char[] buffer = new char[10000];
            int len = reader.read(buffer);
            fragmentProgram = new String(buffer);
            fragmentProgram = fragmentProgram.substring(0, len);
        } catch (Exception e) {
            System.err.println("could'nt load advanced.frag");
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

    private ShaderProgram loadShaderProgram(URL vert_file, URL frag_file) {
        ShaderProgram sp = null;
        String vertexProgram = null;
        String fragmentProgram = null;
        Shader[] shaders = new Shader[2];
        try {
            vertexProgram = StringIO.readFully(vert_file);
        } catch (Exception e) {
            System.err.println("could'nt load " + vert_file);
            e.printStackTrace();
        }
        try {
            fragmentProgram = StringIO.readFully(frag_file);
        } catch (Exception e) {
            System.err.println("could'nt load " + frag_file);
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
