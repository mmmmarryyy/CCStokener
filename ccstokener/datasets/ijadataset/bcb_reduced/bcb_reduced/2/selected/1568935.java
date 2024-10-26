package net.sf.mogbox.renderer.engine.scene.state;

import static org.lwjgl.opengl.GL11.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBFragmentProgram;
import org.lwjgl.opengl.ARBProgram;
import org.lwjgl.opengl.ARBVertexProgram;

public class ShaderState extends RenderState {

    private static Logger log = Logger.getLogger(ShaderState.class.getName());

    private URL vertexProgram;

    private URL fragmentProgram;

    private static boolean checkedVertexProgram = false;

    private static boolean checkedFragmentProgram = false;

    private static boolean vertexProgramSupported;

    private static boolean fragmentProgramSupported;

    private boolean vertexProgramNative;

    private boolean fragmentProgramNative;

    private IntBuffer progs = BufferUtils.createIntBuffer(2);

    public ShaderState(URL vertexProgram, URL fragmentProgram) throws IOException {
        this.vertexProgram = vertexProgram;
        this.fragmentProgram = fragmentProgram;
    }

    public boolean isVertexProgramNative() {
        return vertexProgramNative;
    }

    public boolean isFragmentProgramNative() {
        return fragmentProgramNative;
    }

    @Override
    public int getType() {
        return SHADER;
    }

    @Override
    protected void initializeState() {
        if (vertexProgram != null && !vertexShaderSupported()) return;
        if (fragmentProgram != null && !fragmentShaderSupported()) return;
        IntBuffer temp = BufferUtils.createIntBuffer(16);
        if (vertexProgram != null) {
            try {
                ByteBuffer program = readProgram(vertexProgram);
                glEnable(ARBVertexProgram.GL_VERTEX_PROGRAM_ARB);
                progs.position(0).limit(1);
                ARBProgram.glGenProgramsARB(progs);
                ARBProgram.glBindProgramARB(ARBVertexProgram.GL_VERTEX_PROGRAM_ARB, progs.get(0));
                ARBProgram.glProgramStringARB(ARBVertexProgram.GL_VERTEX_PROGRAM_ARB, ARBProgram.GL_PROGRAM_FORMAT_ASCII_ARB, program);
                temp.position(0);
                glGetInteger(ARBProgram.GL_PROGRAM_ERROR_POSITION_ARB, temp);
                temp.position(1);
                ARBProgram.glGetProgramARB(ARBVertexProgram.GL_VERTEX_PROGRAM_ARB, ARBProgram.GL_PROGRAM_UNDER_NATIVE_LIMITS_ARB, temp);
                if (temp.get(0) != -1) {
                    log.log(Level.WARNING, "Error in Vertex Program\n" + glGetString(ARBProgram.GL_PROGRAM_ERROR_STRING_ARB).trim());
                    progs.position(0).limit(1);
                    ARBProgram.glDeleteProgramsARB(progs);
                    progs.put(0, 0);
                } else {
                    vertexProgramNative = temp.get(1) == 1;
                }
                ARBProgram.glBindProgramARB(ARBVertexProgram.GL_VERTEX_PROGRAM_ARB, 0);
                glDisable(ARBVertexProgram.GL_VERTEX_PROGRAM_ARB);
            } catch (IOException e) {
            }
        }
        if (fragmentProgram != null) {
            try {
                ByteBuffer program = readProgram(fragmentProgram);
                glEnable(ARBFragmentProgram.GL_FRAGMENT_PROGRAM_ARB);
                progs.position(1).limit(2);
                ARBProgram.glGenProgramsARB(progs);
                progs.clear();
                ARBProgram.glBindProgramARB(ARBFragmentProgram.GL_FRAGMENT_PROGRAM_ARB, progs.get(1));
                ARBProgram.glProgramStringARB(ARBFragmentProgram.GL_FRAGMENT_PROGRAM_ARB, ARBProgram.GL_PROGRAM_FORMAT_ASCII_ARB, program);
                temp.position(0);
                glGetInteger(ARBProgram.GL_PROGRAM_ERROR_POSITION_ARB, temp);
                temp.position(1);
                ARBProgram.glGetProgramARB(ARBFragmentProgram.GL_FRAGMENT_PROGRAM_ARB, ARBProgram.GL_PROGRAM_UNDER_NATIVE_LIMITS_ARB, temp);
                if (temp.get(0) != -1) {
                    log.log(Level.WARNING, "Error in Fragment Program\n" + glGetString(ARBProgram.GL_PROGRAM_ERROR_STRING_ARB).trim());
                    progs.position(1).limit(2);
                    ARBProgram.glDeleteProgramsARB(progs);
                    progs.put(1, 0);
                } else {
                    vertexProgramNative = temp.get(1) == 1;
                }
                ARBProgram.glBindProgramARB(ARBFragmentProgram.GL_FRAGMENT_PROGRAM_ARB, 0);
                glDisable(ARBFragmentProgram.GL_FRAGMENT_PROGRAM_ARB);
            } catch (IOException e) {
            }
        }
        progs.clear();
    }

    private ByteBuffer readProgram(URL url) throws IOException {
        StringBuilder program = new StringBuilder();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = in.readLine()) != null) {
                program.append(line).append("\n");
            }
        } finally {
            if (in != null) in.close();
        }
        ByteBuffer buffer = BufferUtils.createByteBuffer(program.length());
        for (int i = 0; i < program.length(); i++) {
            buffer.put((byte) (program.charAt(i) & 0xFF));
        }
        buffer.flip();
        return buffer;
    }

    public static boolean vertexShaderSupported() {
        if (!checkedVertexProgram) {
            checkedVertexProgram = true;
            String extensions = glGetString(GL_EXTENSIONS);
            vertexProgramSupported = extensions.indexOf("GL_ARB_vertex_program") != -1;
        }
        return vertexProgramSupported;
    }

    public static boolean fragmentShaderSupported() {
        if (!checkedFragmentProgram) {
            checkedVertexProgram = true;
            String extensions = glGetString(GL_EXTENSIONS);
            fragmentProgramSupported = extensions.indexOf("GL_ARB_fragment_program") != -1;
        }
        return fragmentProgramSupported;
    }

    @Override
    public void applyState() {
        if (vertexShaderSupported() && progs.get(0) != 0) {
            glEnable(ARBVertexProgram.GL_VERTEX_PROGRAM_ARB);
            ARBProgram.glBindProgramARB(ARBVertexProgram.GL_VERTEX_PROGRAM_ARB, progs.get(0));
        }
        if (fragmentShaderSupported() && progs.get(1) != 0) {
            glEnable(ARBFragmentProgram.GL_FRAGMENT_PROGRAM_ARB);
            ARBProgram.glBindProgramARB(ARBFragmentProgram.GL_FRAGMENT_PROGRAM_ARB, progs.get(1));
        }
    }

    @Override
    protected void clearState() {
        if (vertexShaderSupported()) {
            glDisable(ARBVertexProgram.GL_VERTEX_PROGRAM_ARB);
        }
        if (fragmentShaderSupported()) {
            glDisable(ARBFragmentProgram.GL_FRAGMENT_PROGRAM_ARB);
        }
    }

    @Override
    protected void disposeState() {
        ARBProgram.glDeleteProgramsARB(progs);
        progs.put(0).put(0).clear();
    }
}
