package org.gdi3d.vrmlloader;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Vector;
import javax.media.ding3d.GLSLShaderProgram;
import javax.media.ding3d.Shader;
import javax.media.ding3d.ShaderAppearance;
import javax.media.ding3d.ShaderAttribute;
import javax.media.ding3d.ShaderAttributeBinding;
import javax.media.ding3d.ShaderAttributeSet;
import javax.media.ding3d.ShaderAttributeValue;
import javax.media.ding3d.ShaderProgram;
import javax.media.ding3d.SourceCodeShader;
import javax.media.ding3d.vecmath.Color4f;
import javax.media.ding3d.vecmath.Matrix4f;
import javax.media.ding3d.vecmath.Vector3f;
import javax.media.ding3d.vecmath.Vector4f;

public class AtmosphericAppearance extends ShaderAppearance {

    public static final float DEFAULT_ESUN = 30f;

    public static final float DEFAULT_KM = 0.009358868f;

    public static final float DEFAULT_KR = 0.003f;

    public static final float DEFAULT_G = -0.999f;

    public static final int DEFAULT_NSAMPLES = 3;

    public static final Vector3f DEFAULT_WAVELENGTH = new Vector3f(0.650f, 0.570f, 0.475f);

    public static final float DEFAULT_ATMOSPHERE_HEIGHT = 2f * 159453f;

    public static final float DEFAULT_SCALEDEPTH = 0.125f;

    public static final float earthRadius = 6378137f;

    public static float atmosphereHeight = DEFAULT_ATMOSPHERE_HEIGHT;

    public static float atmosphereVisibilityFromGround = (float) Math.sqrt(atmosphereHeight * (2.0 * earthRadius + atmosphereHeight));

    private static float kR = DEFAULT_KR;

    private static float kM = DEFAULT_KM;

    private static float eSun = DEFAULT_ESUN;

    private static float fESun2Ground = 1f / 30f;

    private static float fExposure = 1.0f;

    private boolean initialized = false;

    private static ShaderProgram atmospheric_sp;

    private static Vector4f v4GlobalCameraPos = new Vector4f();

    private static Vector3f v3GlobalLightPos = new Vector3f();

    private static Vector3f waveLength = new Vector3f(DEFAULT_WAVELENGTH);

    private static Vector3f v3InvWavelength = new Vector3f(1f / (float) Math.pow(waveLength.x, 4.0f), 1f / (float) Math.pow(waveLength.y, 4.0f), 1f / (float) Math.pow(waveLength.z, 4.0f));

    private static float fOuterRadius = earthRadius + atmosphereHeight;

    private static float fOuterRadius2 = fOuterRadius * fOuterRadius;

    private static float fInnerRadius = earthRadius;

    private static float fKr4PI = kR * 4.0f * (float) Math.PI;

    private static float fKm4PI = kM * 4.0f * (float) Math.PI;

    private static float fScale = 1f / (fOuterRadius - fInnerRadius);

    private static float fScaleDepth = DEFAULT_SCALEDEPTH;

    private static float fScaleOverScaleDepth = fScale / fScaleDepth;

    private static int nSamples = DEFAULT_NSAMPLES;

    private static float fSamples = (float) nSamples;

    private static float g = DEFAULT_G;

    private static float g2 = g * g;

    private static Matrix4f mInverseViewMatrix = new Matrix4f();

    private static ShaderAttributeValue v4GlobalCameraPos_sav;

    private static ShaderAttributeValue v3GlobalLightPos_sav;

    private static ShaderAttributeValue v3InvWavelength_sav;

    private static ShaderAttributeValue fOuterRadius_sav;

    private static ShaderAttributeValue fOuterRadius2_sav;

    private static ShaderAttributeValue fInnerRadius_sav;

    private static ShaderAttributeValue fKrESun_sav;

    private static ShaderAttributeValue fKmESun_sav;

    private static ShaderAttributeValue fKr4PI_sav;

    private static ShaderAttributeValue fKm4PI_sav;

    private static ShaderAttributeValue fScale_sav;

    private static ShaderAttributeValue fScaleDepth_sav;

    private static ShaderAttributeValue fScaleOverScaleDepth_sav;

    private static ShaderAttributeValue nSamples_sav;

    private static ShaderAttributeValue fSamples_sav;

    private static ShaderAttributeValue g_sav;

    private static ShaderAttributeValue g2_sav;

    private static ShaderAttributeValue mInverseViewMatrix_sav;

    private static ShaderAttributeValue fESun_sav;

    private static ShaderAttributeValue fESun2Ground_sav;

    private static ShaderAttributeValue fExposure_sav;

    private int ground = 0;

    private ShaderAttributeValue ground_sav;

    private int textured = 0;

    private ShaderAttributeValue textured_sav;

    public AtmosphericAppearance() {
    }

    static {
        v4GlobalCameraPos_sav = new ShaderAttributeValue("v4GlobalCameraPos", new Vector4f(v4GlobalCameraPos));
        v3GlobalLightPos_sav = new ShaderAttributeValue("v3GlobalLightPos", new Vector3f(v3GlobalLightPos));
        v3InvWavelength_sav = new ShaderAttributeValue("v3InvWavelength", new Vector3f(v3InvWavelength));
        fOuterRadius_sav = new ShaderAttributeValue("fOuterRadius", new Float(fOuterRadius));
        fOuterRadius2_sav = new ShaderAttributeValue("fOuterRadius2", new Float(fOuterRadius2));
        fInnerRadius_sav = new ShaderAttributeValue("fInnerRadius", new Float(fInnerRadius));
        fKrESun_sav = new ShaderAttributeValue("fKrESun", new Float(kR * eSun));
        fKmESun_sav = new ShaderAttributeValue("fKmESun", new Float(kM * eSun));
        fKr4PI_sav = new ShaderAttributeValue("fKr4PI", new Float(fKr4PI));
        fKm4PI_sav = new ShaderAttributeValue("fKm4PI", new Float(fKm4PI));
        fScale_sav = new ShaderAttributeValue("fScale", new Float(fScale));
        fScaleDepth_sav = new ShaderAttributeValue("fScaleDepth", new Float(fScaleDepth));
        fScaleOverScaleDepth_sav = new ShaderAttributeValue("fScaleOverScaleDepth", new Float(fScaleOverScaleDepth));
        nSamples_sav = new ShaderAttributeValue("nSamples", new Integer(nSamples));
        fSamples_sav = new ShaderAttributeValue("fSamples", new Float(fSamples));
        g_sav = new ShaderAttributeValue("g", new Float(g));
        g2_sav = new ShaderAttributeValue("g2", new Float(g2));
        mInverseViewMatrix_sav = new ShaderAttributeValue("mInverseViewMatrix", new Matrix4f(mInverseViewMatrix));
        fESun_sav = new ShaderAttributeValue("fESun", new Float(eSun));
        fESun2Ground_sav = new ShaderAttributeValue("fESun2Ground", new Float(fESun2Ground));
        fExposure_sav = new ShaderAttributeValue("fExposure", new Float(fExposure));
        Vector<ShaderAttributeValue> attribute_values = new Vector<ShaderAttributeValue>();
        attribute_values.add(v4GlobalCameraPos_sav);
        attribute_values.add(v3GlobalLightPos_sav);
        attribute_values.add(v3InvWavelength_sav);
        attribute_values.add(fOuterRadius_sav);
        attribute_values.add(fOuterRadius2_sav);
        attribute_values.add(fInnerRadius_sav);
        attribute_values.add(fKrESun_sav);
        attribute_values.add(fKmESun_sav);
        attribute_values.add(fKr4PI_sav);
        attribute_values.add(fKm4PI_sav);
        attribute_values.add(fScale_sav);
        attribute_values.add(fScaleDepth_sav);
        attribute_values.add(fScaleOverScaleDepth_sav);
        attribute_values.add(nSamples_sav);
        attribute_values.add(fSamples_sav);
        attribute_values.add(g_sav);
        attribute_values.add(g2_sav);
        attribute_values.add(mInverseViewMatrix_sav);
        attribute_values.add(fESun_sav);
        attribute_values.add(fESun2Ground_sav);
        attribute_values.add(fExposure_sav);
        for (int i = 0; i < attribute_values.size(); i++) {
            ShaderAttributeValue value = attribute_values.get(i);
            value.setCapability(ShaderAttributeValue.ALLOW_VALUE_READ);
            value.setCapability(ShaderAttributeValue.ALLOW_VALUE_WRITE);
        }
    }

    public void init() {
        try {
            ground_sav = new ShaderAttributeValue("ground", new Integer(ground));
            ground_sav.setCapability(ShaderAttributeValue.ALLOW_VALUE_READ);
            ground_sav.setCapability(ShaderAttributeValue.ALLOW_VALUE_WRITE);
            textured_sav = new ShaderAttributeValue("textured", new Integer(textured));
            textured_sav.setCapability(ShaderAttributeValue.ALLOW_VALUE_READ);
            textured_sav.setCapability(ShaderAttributeValue.ALLOW_VALUE_WRITE);
            ShaderAttributeSet attributes;
            attributes = new ShaderAttributeSet();
            attributes.setCapability(ShaderAttributeSet.ALLOW_ATTRIBUTES_READ);
            attributes.setCapability(ShaderAttributeSet.ALLOW_ATTRIBUTES_WRITE);
            Vector<ShaderAttributeValue> attribute_values = new Vector<ShaderAttributeValue>();
            attribute_values.add(v4GlobalCameraPos_sav);
            attribute_values.add(v3GlobalLightPos_sav);
            attribute_values.add(v3InvWavelength_sav);
            attribute_values.add(fOuterRadius_sav);
            attribute_values.add(fOuterRadius2_sav);
            attribute_values.add(fInnerRadius_sav);
            attribute_values.add(fKrESun_sav);
            attribute_values.add(fKmESun_sav);
            attribute_values.add(fKr4PI_sav);
            attribute_values.add(fKm4PI_sav);
            attribute_values.add(fScale_sav);
            attribute_values.add(fScaleDepth_sav);
            attribute_values.add(fScaleOverScaleDepth_sav);
            attribute_values.add(nSamples_sav);
            attribute_values.add(fSamples_sav);
            attribute_values.add(g_sav);
            attribute_values.add(g2_sav);
            attribute_values.add(mInverseViewMatrix_sav);
            attribute_values.add(ground_sav);
            attribute_values.add(textured_sav);
            attribute_values.add(fESun_sav);
            attribute_values.add(fESun2Ground_sav);
            attribute_values.add(fExposure_sav);
            for (int i = 0; i < attribute_values.size(); i++) {
                ShaderAttributeValue value = attribute_values.get(i);
                attributes.put(value);
            }
            this.setShaderAttributeSet(attributes);
            if (atmospheric_sp == null) {
                atmospheric_sp = loadShaderProgram("atmospheric");
                if (atmospheric_sp != null) {
                    atmospheric_sp.setCapability(ShaderProgram.ALLOW_NAMES_READ);
                    atmospheric_sp.setCapability(ShaderProgram.ALLOW_SHADERS_READ);
                    ShaderAttribute[] attrs = attributes.getAll();
                    String[] attrNames = new String[attrs.length];
                    for (int n = 0, nmax = attrs.length; n < nmax; n++) {
                        attrNames[n] = attrs[n].getAttributeName();
                    }
                    atmospheric_sp.setShaderAttrNames(attrNames);
                }
            }
            this.setShaderProgram(atmospheric_sp);
        } catch (Exception e) {
            e.printStackTrace();
        }
        initialized = true;
    }

    public static void set_v4CameraPos(Vector4f value) {
        v4GlobalCameraPos = value;
        v4GlobalCameraPos_sav.setValue(value);
    }

    public static void set_v3LightPos(Vector3f value) {
        v3GlobalLightPos = value;
        v3GlobalLightPos_sav.setValue(value);
    }

    public static void set_fOuterRadius(float value) {
        fOuterRadius = value;
        fOuterRadius_sav.setValue(new Float(value));
    }

    public static void set_fOuterRadius2(float value) {
        fOuterRadius2 = value;
        fOuterRadius2_sav.setValue(new Float(value));
    }

    public static void set_fInnerRadius(float value) {
        fInnerRadius = value;
        fInnerRadius_sav.setValue(new Float(value));
    }

    public static void set_fKr4PI(float value) {
        fKr4PI = value;
        fKr4PI_sav.setValue(new Float(value));
    }

    public static void set_fKm4PI(float value) {
        fKm4PI = value;
        fKm4PI_sav.setValue(new Float(value));
    }

    public static void set_fScale(float value) {
        fScale = value;
        fScale_sav.setValue(new Float(value));
    }

    public static void set_fScaleDepth(float value) {
        fScaleDepth = value;
        fScaleDepth_sav.setValue(new Float(value));
    }

    public static void set_fScaleOverScaleDepth(float value) {
        fScaleOverScaleDepth = value;
        fScaleOverScaleDepth_sav.setValue(new Float(value));
    }

    public static void set_nSamples(int value) {
        nSamples = value;
        nSamples_sav.setValue(new Integer(value));
    }

    public static void set_fSamples(float value) {
        fSamples = value;
        fSamples_sav.setValue(new Float(value));
    }

    public static float get_fExposure() {
        return fExposure;
    }

    public static void set_fExposure(float value) {
        fExposure = value;
        fExposure_sav.setValue(new Float(value));
    }

    public static float get_g() {
        return g;
    }

    public static void set_g(float value) {
        g = value;
        g_sav.setValue(new Float(g));
        g2 = g * g;
        g2_sav.setValue(new Float(g2));
    }

    public void set_textured(boolean value) {
        if (value == true) {
            textured = 1;
            textured_sav.setValue(new Integer(1));
        } else {
            textured = 0;
            textured_sav.setValue(new Integer(0));
        }
    }

    public void set_ground(boolean value) {
        if (value == true) {
            ground = 1;
            ground_sav.setValue(new Integer(1));
        } else {
            ground = 0;
            ground_sav.setValue(new Integer(0));
        }
    }

    public static void set_mInverseViewMatrix(Matrix4f value) {
        mInverseViewMatrix = value;
        mInverseViewMatrix_sav.setValue(value);
    }

    private ShaderProgram loadShaderProgram(String name) {
        ShaderProgram sp = null;
        String vertexProgram = null;
        String fragmentProgram = null;
        Shader[] shaders = new Shader[2];
        try {
            ClassLoader cl = this.getClass().getClassLoader();
            URL url = cl.getResource("Shaders/" + name + ".vert");
            System.out.println("url " + url);
            InputStream inputSteam = cl.getResourceAsStream("Shaders/" + name + ".vert");
            Reader reader = null;
            if (inputSteam != null) {
                reader = new InputStreamReader(inputSteam);
            } else {
                File file = new File("lib");
                URL url2 = new URL("jar:file:" + file.getAbsolutePath() + "/resources.jar!/resources/Shaders/" + name + ".vert");
                InputStream inputSteam2 = url2.openStream();
                reader = new InputStreamReader(inputSteam2);
            }
            char[] buffer = new char[10000];
            int len = reader.read(buffer);
            vertexProgram = new String(buffer);
            vertexProgram = vertexProgram.substring(0, len);
        } catch (Exception e) {
            System.err.println("could'nt load " + name + ".vert");
            e.printStackTrace();
        }
        try {
            ClassLoader cl = this.getClass().getClassLoader();
            URL url = cl.getResource("Shaders/" + name + ".frag");
            System.out.println("url " + url);
            InputStream inputSteam = cl.getResourceAsStream("Shaders/" + name + ".frag");
            Reader reader = null;
            if (inputSteam != null) {
                reader = new InputStreamReader(inputSteam);
            } else {
                File file = new File("lib");
                URL url2 = new URL("jar:file:" + file.getAbsolutePath() + "/resources.jar/resources/Shaders/" + name + ".frag");
                InputStream inputSteam2 = url2.openStream();
                reader = new InputStreamReader(inputSteam2);
            }
            char[] buffer = new char[10000];
            int len = reader.read(buffer);
            fragmentProgram = new String(buffer);
            fragmentProgram = fragmentProgram.substring(0, len);
        } catch (Exception e) {
            System.err.println("could'nt load " + name + ".frag");
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

    public static float getESun() {
        return eSun;
    }

    public static void setESun(float sun) {
        eSun = sun;
        fESun_sav.setValue(new Float(eSun));
        fKrESun_sav.setValue(new Float(kR * eSun));
        fKmESun_sav.setValue(new Float(kM * eSun));
    }

    public static Vector3f getWaveLength() {
        return waveLength;
    }

    public static void setWaveLength(Vector3f value) {
        waveLength = value;
        v3InvWavelength.x = 1f / (float) Math.pow(waveLength.x, 4.0f);
        v3InvWavelength.y = 1f / (float) Math.pow(waveLength.y, 4.0f);
        v3InvWavelength.z = 1f / (float) Math.pow(waveLength.z, 4.0f);
        v3InvWavelength_sav.setValue(v3InvWavelength);
    }

    public static float getKM() {
        return kM;
    }

    public static void setKM(float km) {
        kM = km;
        fKm4PI_sav.setValue(new Float(kM * 4.0f * (float) Math.PI));
        fKmESun_sav.setValue(new Float(kM * eSun));
    }

    public static float getAtmosphereHeight() {
        return atmosphereHeight;
    }

    public static void setAtmosphereHeight(float value) {
        atmosphereHeight = value;
        atmosphereVisibilityFromGround = (float) Math.sqrt(atmosphereHeight * (2.0 * earthRadius + atmosphereHeight));
        fOuterRadius = earthRadius + atmosphereHeight;
        fOuterRadius2 = fOuterRadius * fOuterRadius;
        fScale = 1f / (fOuterRadius - fInnerRadius);
        fScaleOverScaleDepth = fScale / fScaleDepth;
        fOuterRadius_sav.setValue(new Float(fOuterRadius));
        fOuterRadius2_sav.setValue(new Float(fOuterRadius2));
        fScale_sav.setValue(new Float(fScale));
        fScaleOverScaleDepth_sav.setValue(new Float(fScaleOverScaleDepth));
    }

    public static void setScaleDepth(float value) {
        fScaleDepth = value;
        fScaleOverScaleDepth = fScale / fScaleDepth;
        fScaleDepth_sav.setValue(new Float(fScaleDepth));
        fScaleOverScaleDepth_sav.setValue(new Float(fScaleOverScaleDepth));
    }

    public static float getScaleDepth() {
        return fScaleDepth;
    }

    public static float getKR() {
        return kR;
    }

    public static void setKR(float kr) {
        kR = kr;
        fKr4PI_sav.setValue(new Float(kR * 4.0f * (float) Math.PI));
        fKrESun_sav.setValue(new Float(kR * eSun));
    }

    public static int getNSamples() {
        return nSamples;
    }

    public static void setNSamples(int ns) {
        nSamples = ns;
        if (nSamples < 1) nSamples = 1;
        nSamples_sav.setValue(new Integer(ns));
        fSamples_sav.setValue(new Float(ns));
    }
}
