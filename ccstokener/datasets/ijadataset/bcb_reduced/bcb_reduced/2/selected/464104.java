package org.xith3d.loaders.shaders.impl.glsl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import org.xith3d.loaders.shaders.base.ShaderLoader;
import org.xith3d.scenegraph.Shader.ShaderType;
import org.xith3d.scenegraph.GLSLFragmentShader;
import org.xith3d.scenegraph.GLSLShader;
import org.xith3d.scenegraph.GLSLVertexShader;
import org.xith3d.utility.logs.Log;

/**
 * Loads a GLSL shader.
 * 
 * @author Florian Hofmann (ok ... i copied most from Matthias Mann)
 * @author Marvin Froehlich
 *
 * 14.12.2006 - fhofmann shader source is now displayed correctly in error messages
 */
public class GLSLShaderLoader extends ShaderLoader<GLSLShader> {

    private static final GLSLShaderLoader instance = new GLSLShaderLoader();

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public GLSLShader loadShader(Reader reader, ShaderType type) throws IOException {
        BufferedReader buffReader;
        if (reader instanceof BufferedReader) buffReader = (BufferedReader) reader; else buffReader = new BufferedReader(reader);
        GLSLShader shader = null;
        StringBuffer shaderSource = new StringBuffer();
        String line;
        try {
            while ((line = buffReader.readLine()) != null) {
                shaderSource.append(line);
                shaderSource.append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (shaderSource.length() == 0) {
            Log.error("failed to load shader \"" + "..." + "\"");
            return null;
        }
        if (type == ShaderType.GL_FRAGMENT_SHADER) shader = new GLSLFragmentShader(shaderSource.toString()); else shader = new GLSLVertexShader(shaderSource.toString());
        if (shader == null) {
            Log.error("failed to load shader \"" + "..." + "\"");
            return null;
        }
        return shader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GLSLShader loadShader(InputStream in, ShaderType type) throws IOException {
        return loadShader(new InputStreamReader(in), type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GLSLShader loadShader(URL url, ShaderType type) throws IOException {
        GLSLShader shader = loadShader(url.openStream(), type);
        return (shader);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public GLSLShader loadShader(String name, ShaderType type) throws IOException {
        String cacheTag = type + name;
        GLSLShader<GLSLShader> shader = (GLSLShader<GLSLShader>) getFromCache(cacheTag);
        if (shader != null) return (shader);
        File shaderFile = new File(name);
        if (shaderFile.exists()) {
            shader = loadShader(new FileReader(name), type);
        } else {
            if (getBaseURL() != null) shader = loadShader(new URL(getBaseURL(), name).openStream(), type); else if (getBasePath() != null) shader = loadShader(new FileReader(new File(getBasePath(), name)), type);
        }
        if (shader != null) {
            cacheShader(cacheTag, shader);
            return shader;
        } else {
            Log.error("failed to load shader \"" + name + "\"");
            return null;
        }
    }

    /**
     * Retrives the Shader with the given name.
     * 
     * @param name
     *            The name of the Shader.
     *            
     * @return The Shader object
     */
    public GLSLVertexShader loadVertexShader(String name) throws IOException {
        return (GLSLVertexShader) loadShader(name, ShaderType.GL_VERTEX_SHADER);
    }

    /**
     * Retrives the Shader with the given name.
     * 
     * @param name
     *            The name of the Shader.
     *            
     * @return The Shader object
     */
    public GLSLFragmentShader loadFragmentShader(String name) throws IOException {
        return (GLSLFragmentShader) loadShader(name, ShaderType.GL_FRAGMENT_SHADER);
    }

    /**
     * {@inheritDoc}
	 */
    @Override
    public GLSLShader loadShaderFromString(String source, ShaderType typ) {
        if (typ == ShaderType.GL_FRAGMENT_SHADER) return new GLSLFragmentShader(source); else return new GLSLVertexShader(source);
    }

    /**
     * Creates a Shader from the given String. The generated Shader is
     * not cached.
     * 
     * @param source
     *            The String that should get parsed
     *
     * @return Shader A Shader object that is based on the current content of
     *         the given String
     */
    public GLSLVertexShader loadVertexShaderFromString(String source) {
        return (GLSLVertexShader) loadShaderFromString(source, ShaderType.GL_VERTEX_SHADER);
    }

    /**
     * Creates a Shader from the given String. The generated Shader is
     * not cached.
     * 
     * @param source
     *            The String that should get parsed
     *
     * @return Shader A Shader object that is based on the current content of
     *         the given String
     */
    public GLSLFragmentShader loadFragmentShaderFromString(String source) {
        return (GLSLFragmentShader) loadShaderFromString(source, ShaderType.GL_FRAGMENT_SHADER);
    }

    /**
     * Constructs a Loader with the specified baseURL.
     * 
     * @param baseURL the new baseURL to take resources from
     */
    public GLSLShaderLoader(URL baseURL) {
        super(baseURL);
    }

    /**
     * Constructs a Loader with the specified basePath.
     * 
     * @param basePath the new basePath to take resources from
     */
    public GLSLShaderLoader(String basePath) {
        super(basePath);
    }

    /**
     * Constructs a Loader with default values for all variables.
     */
    public GLSLShaderLoader() {
        super();
    }

    /**
     * @return the singleton instance of the TextureLoader
     */
    public static GLSLShaderLoader getInstance() {
        return instance;
    }
}
