package edu.ucsd.ncmir.Obj;

import edu.ucsd.ncmir.geometry.GeometryComponent;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Hashtable;

/** 
 * Obj class
 */
public class Obj implements GeometryComponent {

    private GeometryComponent[] _children = null;

    /** Constructs and initializes a <code>Obj</code> object
     */
    public Obj() {
        super();
    }

    /** Constructs and initializes a <code>Obj</code> object from input in 
     * <code>file</code>.
     *
     * @param	filename String object describing fully qualified path to 
     *		<B>Obj</B> trace file.
     */
    public Obj(String filename) throws EOFException, IOException, ParseException {
        this(new File(filename));
    }

    /** Constructs and initializes a <code>Obj</code> object from input in 
     * <code>file</code>.
     *
     * @param	file File object describing fully qualified path to 
     *		<B>Obj</B> trace file.
     */
    public Obj(File file) throws EOFException, IOException, ParseException {
        this();
        this.readMesh(file);
        String fpath[] = file.getPath().split(File.separator);
        String fname[] = fpath[fpath.length - 1].split("\\.");
        switch(fname.length) {
            case 0:
                {
                    throw new ParseException("Filename parse error: " + file.getPath(), 0);
                }
            case 1:
            case 2:
                {
                    this._name = fname[0];
                    break;
                }
            default:
                {
                    this._name = fname[0];
                    for (int i = 1; i < fname.length - 1; i++) this._name += "_" + fname[i];
                    break;
                }
        }
    }

    /** Constructs and initializes a <code>Obj</code> object from input in 
     * <code>url</code>.
     *
     * @param	url	url
     */
    public Obj(URL url) throws EOFException, IOException {
        this();
        this.readMesh(new InputStreamReader(url.openStream()));
    }

    /** Constructs and initializes a <code>Obj</code> object from input in 
     * <code>input_stream_reader</code>.
     *
     * @param	input_stream_reader character input stream
     */
    public Obj(InputStreamReader input_stream_reader) throws EOFException, IOException {
        this();
        this.readMesh(input_stream_reader);
    }

    /** Constructs and initializes a <code>Obj</code> object from input in 
     * <code>reader</code>.
     *
     * @param	reader character input stream
     */
    public Obj(LineNumberReader reader) throws EOFException, IOException {
        this();
        this.readMesh(reader);
    }

    private void readMesh(File file) throws EOFException, IOException {
        this.readMesh(new FileReader(file));
    }

    private void readMesh(InputStreamReader input_stream_reader) throws EOFException, IOException {
        this.readMesh(new LineNumberReader(input_stream_reader));
    }

    private static final String THIS = "//this//";

    private Mesh _mesh = null;

    private void readMesh(LineNumberReader line_reader) throws EOFException, IOException {
        String s;
        ArrayList<Triplet> vertices = new ArrayList<Triplet>();
        ArrayList<Triplet> normals = new ArrayList<Triplet>();
        Hashtable<String, Group> group_table = new Hashtable<String, Group>();
        Group group;
        group_table.put(THIS, group = new Group(THIS));
        TextReader reader = new TextReader(line_reader);
        while ((s = reader.readLine()) != null) {
            String[] tokens = s.trim().split("[ \t]+");
            if (tokens.length > 0) {
                if (tokens[0].equals("v")) vertices.add(new Triplet(1, tokens)); else if (tokens[0].equals("vn")) normals.add(new Triplet(1, tokens)); else if (tokens[0].equals("g") || tokens[0].equals("o")) {
                    String gname = THIS;
                    if (tokens.length > 1) gname = tokens[1];
                    group = group_table.get(gname);
                    if (group == null) group_table.put(gname, group = new Group(gname));
                } else if (tokens[0].equals("f")) group.add(new Face(this, 1, tokens, vertices, normals));
            }
        }
        group = group_table.get(THIS);
        group_table.remove(THIS);
        if (group.size() > 0) this._mesh = new Mesh(null, group);
        ArrayList<Obj> children = new ArrayList<Obj>();
        for (Group g : group_table.values()) if (g.size() > 0) children.add(new Mesh(this, g));
        this._children = children.toArray(new Obj[children.size()]);
    }

    private class Mesh extends Obj {

        private GeometryComponent[] _faces;

        private Obj _parent;

        private Mesh(Obj parent, Group group) {
            super();
            this.setName(group.getName());
            this._faces = this.triangularize(group);
            this._parent = parent;
        }

        private GeometryComponent[] triangularize(Group group) {
            ArrayList<GeometryComponent> list = new ArrayList<GeometryComponent>();
            for (Face f : group) if (f.size() == 3) list.add(f); else {
                Obj parent = (Obj) f.getParent();
                Vertex[] vertices = f.toArray(new Vertex[f.size()]);
                list.addAll(this.emitPolygon(parent, vertices));
            }
            return list.toArray(new GeometryComponent[list.size()]);
        }

        private static final boolean COUNTER_CLOCKWISE = false;

        private static final boolean CLOCKWISE = true;

        private boolean orientation(Vertex[] v) {
            double a = 0.0;
            int i;
            for (i = 0; i < v.length - 1; i++) a += this.area((i - 1) < 0 ? v.length - 1 : i - 1, i, i + 1, v);
            return (a >= 0.0) ? COUNTER_CLOCKWISE : CLOCKWISE;
        }

        private boolean determinant(int p1, int p2, int p3, Vertex[] v) {
            if (area(p1, p2, p3, v) >= 0.0) return COUNTER_CLOCKWISE; else return CLOCKWISE;
        }

        private double area(int p1, int p2, int p3, Vertex[] v) {
            double x1 = v[p1].getX();
            double y1 = v[p1].getY();
            double z1 = v[p1].getZ();
            double x2 = v[p2].getX();
            double y2 = v[p2].getY();
            double z2 = v[p2].getZ();
            double x3 = v[p3].getX();
            double y3 = v[p3].getY();
            double z3 = v[p3].getZ();
            double determ = ((x1 * y2 * z3) + (y1 * z2 * x3) + (z1 * x2 * y3)) - ((x1 * z2 * y3) + (y1 * x2 * z3) + (z1 * y2 * x3));
            return determ;
        }

        private double distance2(double x1, double y1, double x2, double y2) {
            double xd, yd;
            double dist2;
            xd = x1 - x2;
            yd = y1 - y2;
            dist2 = xd * xd + yd * yd;
            return dist2;
        }

        private boolean no_interior(int p1, int p2, int p3, Vertex[] v, int[] vp, int n, boolean poly_or) {
            int i;
            int p;
            for (i = 0; i < n; i++) {
                p = vp[i];
                if ((p == p1) || (p == p2) || (p == p3)) continue;
                if ((determinant(p2, p1, p, v) == poly_or) || (determinant(p1, p3, p, v) == poly_or) || (determinant(p3, p2, p, v) == poly_or)) {
                    continue;
                } else {
                    return false;
                }
            }
            return true;
        }

        private ArrayList<Face> emitPolygon(Obj parent, Vertex[] v) {
            ArrayList<Face> triangles = new ArrayList<Face>();
            int n = v.length;
            int prev, cur, next;
            int[] vp;
            int count;
            int min_vert;
            int i;
            double dist;
            double min_dist;
            boolean poly_orientation;
            boolean beenHere = false;
            vp = new int[n];
            poly_orientation = this.orientation(v);
            for (i = 0; i < n; i++) vp[i] = i;
            count = n;
            while (count > 3) {
                min_dist = Double.MAX_VALUE;
                min_vert = 0;
                for (cur = 0; cur < count; cur++) {
                    prev = cur - 1;
                    next = cur + 1;
                    if (cur == 0) prev = count - 1; else if (next == count) next = 0;
                    if ((determinant(vp[prev], vp[cur], vp[next], v) == poly_orientation) && no_interior(vp[prev], vp[cur], vp[next], v, vp, count, poly_orientation) && ((dist = distance2(v[vp[prev]].getX(), v[vp[prev]].getY(), v[vp[next]].getX(), v[vp[next]].getY())) < min_dist)) {
                        min_dist = dist;
                        min_vert = cur;
                    }
                }
                if (min_dist == Double.MAX_VALUE) {
                    if (beenHere) return null;
                    poly_orientation = !poly_orientation;
                    beenHere = true;
                } else {
                    beenHere = false;
                    prev = min_vert - 1;
                    next = min_vert + 1;
                    if (min_vert == 0) prev = count - 1; else if (next == count) next = 0;
                    triangles.add(new Face(parent, v[vp[prev]], v[vp[min_vert]], v[vp[next]]));
                    count--;
                    for (i = min_vert; i < count; i++) vp[i] = vp[i + 1];
                }
            }
            triangles.add(new Face(parent, v[vp[0]], v[vp[1]], v[vp[2]]));
            return triangles;
        }

        @Override
        public GeometryComponent[] getGeometryComponents() {
            return this._faces;
        }

        @Override
        public double[][] getComponentValues() {
            double[][] values = new double[this._faces.length][];
            for (int i = 0; i < this._faces.length; i++) {
                GeometryComponent gc = this._faces[i];
                double[][] face = gc.getComponentValues();
                ArrayList<Double> val = new ArrayList<Double>();
                for (int f = 0; f < face.length; f++) if (face[f] != null) for (int c = 0; c < face[f].length; c++) val.add(face[f][c]);
                values[i] = new double[val.size()];
                for (int v = 0; v < val.size(); v++) values[i][v] = val.get(v).doubleValue();
            }
            return values;
        }

        @Override
        public GeometryComponent getParent() {
            return this._parent;
        }
    }

    private class Group extends ArrayList<Face> {

        private static final long serialVersionUID = 42l;

        private String _name;

        Group(String name) {
            this._name = name;
        }

        String getName() {
            return this._name;
        }
    }

    private class Triplet {

        private double[] _uvw = new double[3];

        Triplet() {
            this._uvw[0] = this._uvw[1] = this._uvw[2] = 0;
        }

        Triplet(int offset, String[] tokens) {
            for (int i = 0; i < this._uvw.length; i++) this._uvw[i] = Double.parseDouble(tokens[i + offset]);
        }

        double[] getUVW() {
            return this._uvw;
        }
    }

    private class Face extends ArrayList<Vertex> implements GeometryComponent {

        private static final long serialVersionUID = 42l;

        private Obj _parent;

        Face(Obj parent, int offset, String[] tokens, ArrayList<Triplet> vertices, ArrayList<Triplet> normals) {
            this._parent = parent;
            for (int i = offset; i < tokens.length; i++) {
                String[] v = tokens[i].split("/");
                int vno = Integer.parseInt(v[0]);
                Vertex vertex = new Vertex(vertices.get(vno - 1));
                if (v.length == 3) {
                    int vnno = Integer.parseInt(v[2]);
                    vertex.setNormal(normals.get(vnno - 1));
                }
                this.add(vertex);
            }
        }

        Face(Obj parent, Vertex a, Vertex b, Vertex c) {
            this._parent = parent;
            this.add(a);
            this.add(b);
            this.add(c);
        }

        public double[][] getComponentValues() {
            double[][] data = new double[(this.size() * 3) + 1][];
            Vertex[] vlist = this.toArray(new Vertex[this.size()]);
            for (int i = 0; i < this.size(); i++) {
                data[(i * 3) + 0] = vlist[i].getVertex();
                data[(i * 3) + 1] = new double[] { 0, 0, 0 };
                data[(i * 3) + 2] = vlist[i].getNormal();
            }
            return data;
        }

        public GeometryComponent[] getGeometryComponents() {
            return null;
        }

        public GeometryComponent getParent() {
            return this._parent;
        }
    }

    private class Vertex {

        private Triplet _vertex;

        private Triplet _normal = new Triplet();

        Vertex(Triplet vertex) {
            this._vertex = vertex;
        }

        void setNormal(Triplet normal) {
            this._normal = normal;
        }

        double[] getVertex() {
            return this._vertex.getUVW();
        }

        double getX() {
            return this._vertex.getUVW()[0];
        }

        double getY() {
            return this._vertex.getUVW()[1];
        }

        double getZ() {
            return this._vertex.getUVW()[2];
        }

        double[] getNormal() {
            return this._normal.getUVW();
        }
    }

    private String _name = "default";

    public void setName(String name) {
        this._name = name;
    }

    public String getName() {
        return this._name;
    }

    @Override
    public double[][] getComponentValues() {
        return (this._mesh != null) ? this._mesh.getComponentValues() : null;
    }

    @Override
    public GeometryComponent getParent() {
        return null;
    }

    public GeometryComponent[] getGeometryComponents() {
        return this._children;
    }
}
