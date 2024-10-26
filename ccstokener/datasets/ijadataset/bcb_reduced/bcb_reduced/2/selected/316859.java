package org.dronus.gl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.hfbk.vis.Prefs;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

public class GLObj {

    protected int dl;

    public Vector3f epicenter;

    public float radius;

    public class Line extends ArrayList<Vector3f> {
    }

    public class Face extends ArrayList<Vector3f> {
    }

    public class Group {

        public String name;

        public List<Face> faces = new ArrayList<Face>();

        public List<Line> lines = new ArrayList<Line>();
    }

    public List<Vector3f> verticies = new ArrayList<Vector3f>();

    public List<Group> groups = new ArrayList<Group>();

    public List<List<Vector3f>> triangles = new ArrayList<List<Vector3f>>();

    public GLObj(String file) {
        try {
            load(new URL("file:" + file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * load given .obj file into verticies and faces indicies array
	 *  
	 * the format used is:
	 * 
	 * v 10.0 10.0 -4.0     .. denotes a vertex
	 * f 1 2 3 4            .. denotes a polygon spanning the first four defined verticies
	 * f 1//2  2//2 3//2 4//2  .. same, as normal and texture indicies are ignored.
	 * 
	 * 
	 * @param filename obj file to load
	 * @throws IOException 
	 */
    void load(URL url) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()));
        Vector3f scale = new Vector3f(1, 1, 1);
        Group currentGroup = new Group();
        currentGroup.name = "default";
        groups.add(currentGroup);
        String line;
        while ((line = r.readLine()) != null) {
            String[] params = line.split(" +");
            if (params.length == 0) continue;
            String command = params[0];
            if (params[0].equals("v")) {
                Vector3f vertex = new Vector3f(Float.parseFloat(params[1]) * scale.x, Float.parseFloat(params[2]) * scale.y, Float.parseFloat(params[3]) * scale.z);
                verticies.add(vertex);
                radius = Math.max(radius, vertex.length());
            }
            if (command.equals("center")) {
                epicenter = new Vector3f(Float.parseFloat(params[1]), Float.parseFloat(params[2]), Float.parseFloat(params[3]));
            } else if (command.equals("f")) {
                Face f = new Face();
                for (int i = 1; i < params.length; i++) {
                    String parts[] = params[i].split("/");
                    Vector3f v = verticies.get(Integer.parseInt(parts[0]) - 1);
                    f.add(v);
                }
                currentGroup.faces.add(f);
            } else if (command.equals("l")) {
                Line l = new Line();
                for (int i = 1; i < params.length; i++) {
                    Vector3f v = verticies.get(Integer.parseInt(params[i]) - 1);
                    l.add(v);
                }
                currentGroup.lines.add(l);
            } else if (command.equals("g") && params.length > 1) {
                currentGroup = new Group();
                currentGroup.name = params[1];
                groups.add(currentGroup);
            } else if (command.equals("scale")) {
                scale = new Vector3f(Float.parseFloat(params[1]), Float.parseFloat(params[2]), Float.parseFloat(params[3]));
            }
        }
        r.close();
    }

    public void drawNormals() {
        GL11.glLineWidth(2);
        for (Group g : groups) for (Face f : g.faces) {
            if (f.size() < 3) continue;
            Vector3f normal = getNormal(f);
            normal.scale(10);
            Vector3f mid = getCenter(f);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glColor3f(0, 0, 1);
            GL11.glVertex3f(normal.x + mid.x, normal.y + mid.y, normal.z + mid.z);
            GL11.glColor3f(1, 1, 1);
            GL11.glVertex3f(mid.x, mid.y, mid.z);
            GL11.glColor3f(1, 0, 0);
            GL11.glVertex3f(-normal.x + mid.x, -normal.y + mid.y, -normal.z + mid.z);
            GL11.glEnd();
        }
        GL11.glLineWidth(1);
    }

    public Vector3f getNormal(List<Vector3f> tri) {
        Vector3f n0 = tri.get(0), n1 = tri.get(1), n2 = tri.get(2);
        Vector3f d0 = new Vector3f(), d2 = new Vector3f();
        Vector3f.sub(n0, n1, d0);
        Vector3f.sub(n2, n1, d2);
        Vector3f normal = new Vector3f();
        Vector3f.cross(d0, d2, normal);
        normal.normalise();
        return normal;
    }

    public Vector3f getCenter(List<Vector3f> poly) {
        Vector3f c = new Vector3f();
        for (Vector3f v : poly) {
            Vector3f.add(c, v, c);
        }
        c.scale(1f / poly.size());
        return c;
    }

    public void drawFaces() {
        for (Group g : groups) {
            if (!g.name.equals("default")) groupColor(g.name.hashCode() & 0xFF);
            for (Face f : g.faces) {
                List<List<Vector3f>> convexPolys;
                if (f.size() < 4) {
                    convexPolys = new ArrayList<List<Vector3f>>();
                    convexPolys.add(f);
                } else {
                    Convexiser bfec = new Convexiser();
                    convexPolys = bfec.convexise(f);
                }
                for (List<Vector3f> t : convexPolys) {
                    GL11.glBegin(GL11.GL_POLYGON);
                    for (Vector3f v : t) GL11.glVertex3f(v.x, v.y, v.z);
                    GL11.glEnd();
                    triangles.add(t);
                }
            }
        }
    }

    void groupColor(float g) {
        if (g > 0) {
            g = g * 41;
            GL11.glColor3f((g / 3) % 1f, (g / 5) % 1f, (g / 7) % 1f);
        }
    }

    public void drawWireframe() {
        for (Group g : groups) for (Face f : g.faces) {
            GL11.glBegin(GL11.GL_LINE_LOOP);
            for (Vector3f v : f) GL11.glVertex3f(v.x, v.y, v.z);
            GL11.glEnd();
        }
    }

    public void drawLines() {
        for (Group g : groups) for (Line l : g.lines) {
            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (Vector3f v : l) GL11.glVertex3f(v.x, v.y, v.z);
            GL11.glEnd();
        }
    }

    void drawIndicies() {
        GLFont.getDefault().render();
        int index = 0;
        for (Vector3f v : verticies) {
            GL11.glPushMatrix();
            GL11.glTranslatef(v.x, v.y, v.z);
            GLUtil.billboardCylinder();
            GL11.glColor3f(1, 1, 1);
            GLFont.getDefault().print("" + index++);
            GL11.glPopMatrix();
        }
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    /**
	 * render this .obj file as white wireframe and colored transparent faces
	 */
    public void render() {
        if (dl == 0) {
            dl = GL11.glGenLists(1);
            GL11.glNewList(dl, GL11.GL_COMPILE);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glPolygonOffset(1, 1);
            GL11.glColor4f(.5f, .5f, .5f, .3f);
            drawFaces();
            GL11.glPolygonOffset(0, 0);
            GL11.glColor3f(1, 1, 1);
            drawWireframe();
            drawLines();
            GL11.glEndList();
        }
        GL11.glCallList(dl);
        if (Prefs.current.debug) {
            drawNormals();
            drawIndicies();
        }
    }
}
