package fileHandling;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Random;
import com.jme.scene.state.GLSLShaderObjectsState;
import com.jme.system.DisplaySystem;
import com.jme.util.resource.ResourceLocatorTool;

public class ShaderLoader {

    private static final String SHADERS_PATH = "data/shaders/";

    private static final String SPEC_PATH = SHADERS_PATH + "specular/";

    private static final String SPEC_VERT = SPEC_PATH + "normalmap.vert";

    private static final String SPEC_FRAG = SPEC_PATH + "normalmap.frag";

    private static final String SHIELD_PATH = SHADERS_PATH + "shield/";

    private static final String SHIELD_VERT = SHIELD_PATH + "shieldeffect.vert";

    private static final String SHIELD_FRAG = SHIELD_PATH + "shieldeffect.frag";

    private static final String CLOAK_PATH = SHADERS_PATH + "cloak/";

    private static final String CLOAK_VERT = CLOAK_PATH + "cloakeffect.vert";

    private static final String CLOAK_FRAG = CLOAK_PATH + "cloakeffect.frag";

    private static HashMap<String, String> savedShaders = new HashMap<String, String>();

    public static GLSLShaderObjectsState getSpecularShader() {
        GLSLShaderObjectsState so = getShaderState(SPEC_VERT, SPEC_FRAG);
        so.setUniform("baseMap", 0);
        so.setUniform("normalMap", 1);
        so.setUniform("specularMap", 2);
        so.setUniform("heightMap", 3);
        so.setUniform("heightValue", 0.05f);
        return so;
    }

    public static GLSLShaderObjectsState getShieldShader() {
        GLSLShaderObjectsState so = getShaderState(SHIELD_VERT, SHIELD_FRAG);
        return so;
    }

    public static GLSLShaderObjectsState getCloakShader() {
        GLSLShaderObjectsState so = getShaderState(CLOAK_VERT, CLOAK_FRAG);
        so.setUniform("x", getRandomValue(1, 6));
        so.setUniform("y", getRandomValue(1, 6));
        return so;
    }

    protected static int getRandomValue(int min, int max) {
        Random rand = new Random();
        int v = min + rand.nextInt(max - min);
        return rand.nextBoolean() ? v : -v;
    }

    private static GLSLShaderObjectsState getShaderState(String vertPath, String fragPath) {
        String vert = getShaderString(ModelImporter.getURL(vertPath, ResourceLocatorTool.TYPE_SHADER));
        String frag = getShaderString(ModelImporter.getURL(fragPath, ResourceLocatorTool.TYPE_SHADER));
        GLSLShaderObjectsState so = DisplaySystem.getDisplaySystem().getRenderer().createGLSLShaderObjectsState();
        so.load(vert, frag);
        return so;
    }

    private static String getShaderString(URL url) {
        String fileName = url.getFile();
        String content = savedShaders.get(fileName);
        if (content != null) return content;
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder buf = new StringBuilder();
            while (r.ready()) {
                buf.append(r.readLine()).append('\n');
            }
            content = buf.toString();
            savedShaders.put(fileName, content);
            return content;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
