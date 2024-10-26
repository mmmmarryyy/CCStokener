package org.sam.jogl;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3f;
import javax.vecmath.TexCoord2f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

/**
 * <p>Clase que proporciona los métodos estáticos para poder cargar la geometría
 * almacenada en un archivo <i>Wavefront .obj</i>.</p>
 * <u>Nota:</u> La implementación de este cargador es muy básica, y almacena todas las
 * partes y objetos contenidos en el archivo en una <b>única geometría</b>, igonorando
 * los distintos materiales y texturas aplicadas a cada parte u objeto almacenado.
 */
public class ObjLoader {

    /**
	 * Constante que indica que no se hará ninguna modificación con los datos cargados.
	 */
    public static final int NONE = 0x00;

    /**
	 * Constante que indica que los datos cargados se almacenarán en una
	 * geometría accesible {@code GeometriaAbs}. Por defecto se almacenan
	 * directamente en memoria de video a través de una {@code OglList}.
	 */
    public static final int TO_GEOMETRY = 0x01;

    /**
	 * Constante que indica que los vertices cargados serán reescalados y
	 * trasladados. De tal forma que todos queden contenidos dentro de un
	 * cubo de lado 1 unidad centrado en el origen de coordenadas.
	 */
    public static final int RESIZE = 0x02;

    /**
	 * Constante que indica que todas las caras de la geometría serán
	 * tratadas como triangulos, si aparece un cudrilatero se dividirá
	 * en dos triangulos.
	 */
    public static final int TRIANGULATE = 0x04;

    /**
	 * Constante que indica que se reflejarán verticalmente las
	 * coordenadas de textura.
	 */
    public static final int MUST_FLIP_VERTICALLY_TEXCOORDS = 0x08;

    /**
	 * Constante que indica que se prepará la geometría para poder
	 * hacer uso del shader <i>SinglePassWireFrame</i>.
	 */
    public static final int WIREFRAME = 0x10;

    /**
	 * Constante que indica que se generarán tanto la tangente como
	 * la bitangente, correspondiente a cada vertice, necesarias para
	 * el poder realizar <i>normal mapping</i>.
	 */
    public static final int GENERATE_TANGENTS = 0x20;

    /**
	 * Constante que indica que en vez de almacenar la geometría,
	 * se generarán las líneas que representan: tanto la normal, 
	 * la tangente, como la bitangente, correspondientes a cada vértice.<br/>
	 * Útil para poder visualizar valores al realizar test.
	 */
    public static final int GENERATE_NTB = 0x40;

    /**
	 * Excepción que será lanzada cuando se produzca un error al interpretar
	 * los datos del fichero.
	 */
    @SuppressWarnings("serial")
    public static class ParsingErrorException extends RuntimeException {

        /** Crea una {@code ParsingErrorException} sin mensaje de detalle.
	     */
        public ParsingErrorException() {
            super();
        }

        /** Crea una {@code ParsingErrorException} con el mensaje de detalle
	     *  especifico.
	     * @param   message  Mensaje que detalla la causa de la excepción.
	     */
        public ParsingErrorException(String message) {
            super(message);
        }
    }

    private static class ObjParser extends StreamTokenizer {

        private static final char BACKSLASH = '\\';

        ObjParser(Reader r) {
            super(r);
            setup();
        }

        void setup() {
            resetSyntax();
            eolIsSignificant(true);
            lowerCaseMode(true);
            wordChars('!', '~');
            commentChar('!');
            commentChar('#');
            whitespaceChars(' ', ' ');
            whitespaceChars('\n', '\n');
            whitespaceChars('\r', '\r');
            whitespaceChars('\t', '\t');
            ordinaryChar('/');
            ordinaryChar(BACKSLASH);
        }

        void getToken() throws ParsingErrorException {
            try {
                while (true) {
                    nextToken();
                    if (ttype != ObjParser.BACKSLASH) break;
                    nextToken();
                    if (ttype != StreamTokenizer.TT_EOL) break;
                }
            } catch (IOException e) {
                throw new ParsingErrorException("IO error on line " + lineno() + ": " + e.getMessage());
            }
        }

        void skipToNextLine() throws ParsingErrorException {
            while (ttype != StreamTokenizer.TT_EOF && ttype != StreamTokenizer.TT_EOL) {
                getToken();
            }
        }

        float getFloat() throws ParsingErrorException {
            do {
                getToken();
            } while (ttype == StreamTokenizer.TT_EOL);
            return getLastValueAsFloat();
        }

        float getLastValueAsFloat() throws ParsingErrorException {
            try {
                if (ttype == StreamTokenizer.TT_WORD) return (Double.valueOf(sval)).floatValue();
                throw new ParsingErrorException("Expected number on line " + lineno());
            } catch (NumberFormatException e) {
                throw new ParsingErrorException(e.getMessage());
            }
        }

        int getInteger() throws ParsingErrorException {
            do {
                getToken();
            } while (ttype == StreamTokenizer.TT_EOL);
            return getLastValueAsInteger();
        }

        int getLastValueAsInteger() throws ParsingErrorException {
            try {
                if (ttype == StreamTokenizer.TT_WORD) return (Integer.valueOf(sval)).intValue();
                throw new ParsingErrorException("Expected number on line " + lineno());
            } catch (NumberFormatException e) {
                throw new ParsingErrorException(e.getMessage());
            }
        }
    }

    private abstract static class Primitive {

        Primitive() {
        }

        abstract Triangle[] toTriangles();

        abstract void generateTangents(final List<Point3f> coordList, Vector3f[][] tangents);

        abstract void generate(GL2 gl, Generator generator, final List<Point3f> coordList, final List<Vector3f> normList, final List<TexCoord2f> texList);

        abstract void generate(GL2 gl, Generator generator, final List<Point3f> coordList, final List<Vector3f> normList, final Vector3f[][] tangents, final List<TexCoord2f> texList);
    }

    private static class Triangle extends Primitive {

        int indexP1, indexP2, indexP3;

        int indexN1, indexN2, indexN3;

        int indexT1, indexT2, indexT3;

        Triangle(int indexP1, int indexP2, int indexP3, int indexN1, int indexN2, int indexN3, int indexT1, int indexT2, int indexT3) {
            this.indexP1 = indexP1;
            this.indexP2 = indexP2;
            this.indexP3 = indexP3;
            this.indexN1 = indexN1;
            this.indexN2 = indexN2;
            this.indexN3 = indexN3;
            this.indexT1 = indexT1;
            this.indexT2 = indexT2;
            this.indexT3 = indexT3;
        }

        Triangle[] toTriangles() {
            return new Triangle[] { this };
        }

        private static final transient Vector3f tangent = new Vector3f();

        private static final transient Vector3f bitangent = new Vector3f();

        void generateTangents(final List<Point3f> coordList, Vector3f[][] tangents) {
            final Point3f p0 = coordList.get(indexP1);
            final Point3f p1 = coordList.get(indexP2);
            final Point3f p2 = coordList.get(indexP3);
            tangent.set(p1.x - p0.x, p1.y - p0.y, p1.z - p0.z);
            bitangent.set(p2.x - p0.x, p2.y - p0.y, p2.z - p0.z);
            tangents[0][indexN1].add(tangent);
            tangents[0][indexN2].add(tangent);
            tangents[0][indexN3].add(tangent);
            tangents[1][indexN1].add(bitangent);
            tangents[1][indexN2].add(bitangent);
            tangents[1][indexN3].add(bitangent);
        }

        @Override
        void generate(GL2 gl, Generator generator, final List<Point3f> coordList, final List<Vector3f> normList, final List<TexCoord2f> texList) {
            final Point3f p00 = coordList.get(indexP1);
            final Point3f p10 = coordList.get(indexP2);
            final Point3f p11 = coordList.get(indexP3);
            final TexCoord2f c00 = indexT1 < 0 ? null : texList.get(indexT1);
            final TexCoord2f c10 = indexT2 < 0 ? null : texList.get(indexT2);
            final TexCoord2f c11 = indexT3 < 0 ? null : texList.get(indexT3);
            final Vector3f n00 = indexN1 < 0 ? null : normList.get(indexN1);
            final Vector3f n10 = indexN2 < 0 ? null : normList.get(indexN2);
            final Vector3f n11 = indexN3 < 0 ? null : normList.get(indexN3);
            final Vector4f t00 = null;
            final Vector4f t10 = null;
            final Vector4f t11 = null;
            generator.generate(gl, p00, p10, p11, c00, c10, c11, n00, n10, n11, t00, t10, t11);
        }

        @Override
        void generate(GL2 gl, Generator generator, final List<Point3f> coordList, final List<Vector3f> normList, final Vector3f[][] tangents, final List<TexCoord2f> texList) {
            final Point3f p00 = coordList.get(indexP1);
            final Point3f p10 = coordList.get(indexP2);
            final Point3f p11 = coordList.get(indexP3);
            final Vector3f n00 = normList.get(indexN1);
            final Vector3f n10 = normList.get(indexN2);
            final Vector3f n11 = normList.get(indexN3);
            final Vector4f t00 = new Vector4f();
            final Vector4f t10 = new Vector4f();
            final Vector4f t11 = new Vector4f();
            final TexCoord2f c00 = texList.get(indexT1);
            final TexCoord2f c10 = texList.get(indexT2);
            final TexCoord2f c11 = texList.get(indexT3);
            float s1 = c11.x - c00.x;
            float s2 = c10.x - c00.x;
            float t1 = c11.y - c00.y;
            float t2 = c10.y - c00.y;
            float r = 1.0f / (s1 * t2 - s2 * t1);
            final Vector2f s = new Vector2f(s1, s2);
            s.scale(r);
            final Vector2f t = new Vector2f(t1, t2);
            t.scale(r);
            calculateTangent(n00, s, t, t00);
            calculateTangent(n10, s, t, t10);
            calculateTangent(n11, s, t, t11);
            generator.generate(gl, p00, p10, p11, c00, c10, c11, n00, n10, n11, t00, t10, t11);
        }
    }

    private static class Quad extends Primitive {

        int indexP1, indexP2, indexP3, indexP4;

        int indexN1, indexN2, indexN3, indexN4;

        int indexT1, indexT2, indexT3, indexT4;

        Quad(int indexP1, int indexP2, int indexP3, int indexP4, int indexN1, int indexN2, int indexN3, int indexN4, int indexT1, int indexT2, int indexT3, int indexT4) {
            this.indexP1 = indexP1;
            this.indexP2 = indexP2;
            this.indexP3 = indexP3;
            this.indexP4 = indexP4;
            this.indexN1 = indexN1;
            this.indexN2 = indexN2;
            this.indexN3 = indexN3;
            this.indexN4 = indexN4;
            this.indexT1 = indexT1;
            this.indexT2 = indexT2;
            this.indexT3 = indexT3;
            this.indexT4 = indexT4;
        }

        Triangle[] toTriangles() {
            return new Triangle[] { new Triangle(indexP1, indexP2, indexP3, indexN1, indexN2, indexN3, indexT1, indexT2, indexT3), new Triangle(indexP3, indexP4, indexP1, indexN3, indexN4, indexN1, indexT3, indexT4, indexT1) };
        }

        private static final transient Vector3f tangent = new Vector3f();

        private static final transient Vector3f bitangent = new Vector3f();

        void generateTangents(final List<Point3f> coordList, Vector3f[][] tangents) {
            final Point3f p00 = coordList.get(indexP1);
            final Point3f p10 = coordList.get(indexP2);
            final Point3f p11 = coordList.get(indexP3);
            final Point3f p01 = coordList.get(indexP4);
            tangent.set(p11.x + p01.x - p10.x - p00.x, p11.y + p01.y - p10.y - p00.y, p11.z + p01.z - p10.z - p00.z);
            bitangent.set(p11.x + p10.x - p01.x - p00.x, p11.y + p10.y - p01.y - p00.y, p11.z + p10.z - p01.z - p00.z);
            tangents[0][indexN1].add(tangent);
            tangents[0][indexN2].add(tangent);
            tangents[0][indexN3].add(tangent);
            tangents[0][indexN4].add(tangent);
            tangents[1][indexN1].add(bitangent);
            tangents[1][indexN2].add(bitangent);
            tangents[1][indexN3].add(bitangent);
            tangents[1][indexN4].add(bitangent);
        }

        @Override
        void generate(GL2 gl, Generator generator, final List<Point3f> coordList, final List<Vector3f> normList, final List<TexCoord2f> texList) {
            final Point3f p00 = coordList.get(indexP1);
            final Point3f p10 = coordList.get(indexP2);
            final Point3f p11 = coordList.get(indexP3);
            final Point3f p01 = coordList.get(indexP4);
            final TexCoord2f c00 = indexT1 < 0 ? null : texList.get(indexT1);
            final TexCoord2f c10 = indexT2 < 0 ? null : texList.get(indexT2);
            final TexCoord2f c11 = indexT3 < 0 ? null : texList.get(indexT3);
            final TexCoord2f c01 = indexT4 < 0 ? null : texList.get(indexT4);
            final Vector3f n00 = indexN1 < 0 ? null : normList.get(indexN1);
            final Vector3f n10 = indexN2 < 0 ? null : normList.get(indexN2);
            final Vector3f n11 = indexN3 < 0 ? null : normList.get(indexN3);
            final Vector3f n01 = indexN4 < 0 ? null : normList.get(indexN4);
            final Vector4f t00 = null;
            final Vector4f t10 = null;
            final Vector4f t11 = null;
            final Vector4f t01 = null;
            generator.generate(gl, p00, p10, p11, p01, c00, c10, c11, c01, n00, n10, n11, n01, t00, t10, t11, t01);
        }

        @Override
        void generate(GL2 gl, Generator generator, final List<Point3f> coordList, final List<Vector3f> normList, final Vector3f[][] tangents, final List<TexCoord2f> texList) {
            final Point3f p00 = coordList.get(indexP1);
            final Point3f p10 = coordList.get(indexP2);
            final Point3f p11 = coordList.get(indexP3);
            final Point3f p01 = coordList.get(indexP4);
            final Vector3f n00 = normList.get(indexN1);
            final Vector3f n10 = normList.get(indexN2);
            final Vector3f n11 = normList.get(indexN3);
            final Vector3f n01 = normList.get(indexN4);
            final Vector4f t00 = new Vector4f();
            final Vector4f t10 = new Vector4f();
            final Vector4f t11 = new Vector4f();
            final Vector4f t01 = new Vector4f();
            final TexCoord2f c00 = texList.get(indexT1);
            final TexCoord2f c10 = texList.get(indexT2);
            final TexCoord2f c11 = texList.get(indexT3);
            final TexCoord2f c01 = texList.get(indexT4);
            float s1 = c11.x + c01.x - c10.x - c00.x;
            float s2 = c11.x + c10.x - c01.x - c00.x;
            float t1 = c11.y + c01.y - c10.y - c00.y;
            float t2 = c11.y + c10.y - c01.y - c00.y;
            float r = 1.0f / (s1 * t2 - s2 * t1);
            final Vector2f s = new Vector2f(s1, s2);
            s.scale(r);
            final Vector2f t = new Vector2f(t1, t2);
            t.scale(r);
            calculateTangent(n00, s, t, t00);
            calculateTangent(n10, s, t, t10);
            calculateTangent(n11, s, t, t11);
            calculateTangent(n01, s, t, t01);
            generator.generate(gl, p00, p10, p11, p01, c00, c10, c11, c01, n00, n10, n11, n01, t00, t10, t11, t01);
        }
    }

    static float clearDist;

    @SuppressWarnings("serial")
    private static Point3f readVertex(ObjParser st) throws ParsingErrorException {
        Point3f v = new Point3f() {

            public boolean equals(Object t) {
                try {
                    return VectorUtils.distance(this, (Tuple3f) t) <= clearDist;
                } catch (NullPointerException e2) {
                    return false;
                } catch (ClassCastException e1) {
                    return false;
                }
            }
        };
        v.x = st.getFloat();
        v.y = st.getFloat();
        v.z = st.getFloat();
        return v;
    }

    private static TexCoord2f readTexture(ObjParser st) throws ParsingErrorException {
        TexCoord2f t = new TexCoord2f();
        t.x = st.getFloat();
        t.y = st.getFloat();
        return t;
    }

    @SuppressWarnings("serial")
    private static Vector3f readNormal(ObjParser st) throws ParsingErrorException {
        Vector3f n = new Vector3f() {

            public boolean equals(Object t) {
                try {
                    return dot((Vector3f) t) > 0.9f;
                } catch (NullPointerException e2) {
                    return false;
                } catch (ClassCastException e1) {
                    return false;
                }
            }
        };
        n.x = st.getFloat();
        n.y = st.getFloat();
        n.z = st.getFloat();
        n.normalize();
        return n;
    }

    private static Primitive readPrimitive(ObjParser st, int coordListSize, int texListSize, int normListSize) throws ParsingErrorException {
        int[] vertIndex = new int[] { -1, -1, -1, -1 };
        int[] texIndex = new int[] { -1, -1, -1, -1 };
        int[] normIndex = new int[] { -1, -1, -1, -1 };
        int count = 0;
        st.getToken();
        do {
            if (st.ttype == StreamTokenizer.TT_WORD) {
                vertIndex[count] = st.getLastValueAsInteger() - 1;
                if (vertIndex[count] < 0) vertIndex[count] += coordListSize + 1;
            }
            st.getToken();
            if (st.ttype == '/') {
                st.getToken();
                if (st.ttype == StreamTokenizer.TT_WORD) {
                    texIndex[count] = st.getLastValueAsInteger() - 1;
                    if (texIndex[count] < 0) texIndex[count] += texListSize + 1;
                    st.getToken();
                }
                if (st.ttype == '/') {
                    normIndex[count] = st.getInteger() - 1;
                    if (normIndex[count] < 0) normIndex[count] += normListSize + 1;
                    st.getToken();
                }
            }
            count++;
        } while (st.ttype != StreamTokenizer.TT_EOF && st.ttype != StreamTokenizer.TT_EOL);
        if (count == 3) {
            return new Triangle(vertIndex[0], vertIndex[1], vertIndex[2], normIndex[0], normIndex[1], normIndex[2], texIndex[0], texIndex[1], texIndex[2]);
        }
        return new Quad(vertIndex[0], vertIndex[1], vertIndex[2], vertIndex[3], normIndex[0], normIndex[1], normIndex[2], normIndex[3], texIndex[0], texIndex[1], texIndex[2], texIndex[3]);
    }

    private static void purgeVertices(final Queue<Primitive> primitives, final List<Point3f> coordList) {
        int[] newIndices = new int[coordList.size()];
        ArrayList<Point3f> newCoordList = new ArrayList<Point3f>(coordList.size());
        int i = 0, j = 0;
        for (Point3f p : coordList) {
            int index = newCoordList.indexOf(p);
            if (index < 0) {
                newCoordList.add(p);
                newIndices[i] = j;
                j++;
            } else {
                newIndices[i] = index;
            }
            i++;
        }
        if (coordList.size() != newCoordList.size()) {
            System.out.println("Vertices: " + coordList.size() + " --> " + newCoordList.size());
            for (Primitive p : primitives) {
                if (p instanceof Triangle) {
                    Triangle t = (Triangle) p;
                    t.indexP1 = newIndices[t.indexP1];
                    t.indexP2 = newIndices[t.indexP2];
                    t.indexP3 = newIndices[t.indexP3];
                } else {
                    Quad q = (Quad) p;
                    q.indexP1 = newIndices[q.indexP1];
                    q.indexP2 = newIndices[q.indexP2];
                    q.indexP3 = newIndices[q.indexP3];
                    q.indexP4 = newIndices[q.indexP4];
                }
            }
            coordList.clear();
            coordList.addAll(newCoordList);
        } else {
            System.out.println("Vertices: " + coordList.size());
        }
        LinkedList<Primitive> newPrimitives = new LinkedList<Primitive>();
        while (!primitives.isEmpty()) {
            Primitive p = primitives.poll();
            if (p instanceof Triangle) {
                Triangle t = (Triangle) p;
                if (t.indexP1 == t.indexP2 || t.indexP2 == t.indexP3 || t.indexP3 == t.indexP1) continue;
                newPrimitives.add(p);
            } else {
                Quad q = (Quad) p;
                if (q.indexP1 == q.indexP3 || q.indexP2 == q.indexP4) continue;
                if (q.indexP1 == q.indexP2 || q.indexP1 == q.indexP4) {
                    newPrimitives.add(new Triangle(q.indexP2, q.indexP3, q.indexP4, q.indexN2, q.indexN3, q.indexN4, q.indexT2, q.indexT3, q.indexT4));
                } else if (q.indexP3 == q.indexP2 || q.indexP3 == q.indexP4) {
                    newPrimitives.add(new Triangle(q.indexP1, q.indexP2, q.indexP4, q.indexN1, q.indexN2, q.indexN4, q.indexT1, q.indexT2, q.indexT4));
                } else newPrimitives.add(q);
            }
        }
        primitives.addAll(newPrimitives);
    }

    private static boolean valorNoValido(float f) {
        return Float.compare(f, 0.0f) == 0 || Float.compare(f, -0.0f) == 0 || Float.compare(f, Float.NaN) == 0 || Float.compare(f, Float.POSITIVE_INFINITY) == 0 || Float.compare(f, Float.NEGATIVE_INFINITY) == 0;
    }

    @SuppressWarnings("unchecked")
    private static void regenerateNormals(final Queue<Primitive> primitives, final List<Point3f> coordList, final List<Vector3f> normList) {
        purgeVertices(primitives, coordList);
        Map<Integer, Integer>[] newIndices = new Map[coordList.size()];
        for (int i = 0; i < newIndices.length; i++) newIndices[i] = new HashMap<Integer, Integer>();
        for (Primitive p : primitives) {
            if (p instanceof Triangle) {
                Triangle t = (Triangle) p;
                newIndices[t.indexP1].put(t.indexN1, t.indexN1);
                newIndices[t.indexP2].put(t.indexN2, t.indexN2);
                newIndices[t.indexP3].put(t.indexN3, t.indexN3);
            } else {
                Quad q = (Quad) p;
                newIndices[q.indexP1].put(q.indexN1, q.indexN1);
                newIndices[q.indexP2].put(q.indexN2, q.indexN2);
                newIndices[q.indexP3].put(q.indexN3, q.indexN3);
                newIndices[q.indexP4].put(q.indexN4, q.indexN4);
            }
        }
        ArrayList<Vector3f> newNormList = new ArrayList<Vector3f>(normList.size());
        for (Map<Integer, Integer> m : newIndices) {
            int indexI = newNormList.size();
            for (int oldIndex : m.keySet()) {
                Vector3f v = normList.get(oldIndex);
                int index = newNormList.lastIndexOf(v);
                if (index < 0) {
                    m.put(oldIndex, newNormList.size());
                    newNormList.add(v);
                } else if (index < indexI) {
                    m.put(oldIndex, newNormList.size());
                    newNormList.add(v);
                } else {
                    m.put(oldIndex, index);
                }
            }
        }
        System.out.println("Normals: " + normList.size() + " --> " + newNormList.size());
        for (Primitive p : primitives) {
            if (p instanceof Triangle) {
                Triangle t = (Triangle) p;
                t.indexN1 = newIndices[t.indexP1].get(t.indexN1);
                t.indexN2 = newIndices[t.indexP2].get(t.indexN2);
                t.indexN3 = newIndices[t.indexP3].get(t.indexN3);
            } else {
                Quad q = (Quad) p;
                q.indexN1 = newIndices[q.indexP1].get(q.indexN1);
                q.indexN2 = newIndices[q.indexP2].get(q.indexN2);
                q.indexN3 = newIndices[q.indexP3].get(q.indexN3);
                q.indexN4 = newIndices[q.indexP4].get(q.indexN4);
            }
        }
        normList.clear();
        for (int i = 0; i < newNormList.size(); i++) normList.add(new Vector3f());
        final Point3f p1 = new Point3f();
        final Point3f p2 = new Point3f();
        final Point3f p3 = new Point3f();
        final Point3f p4 = new Point3f();
        final Vector3f normal = new Vector3f();
        for (Primitive p : primitives) {
            if (p instanceof Triangle) {
                Triangle t = (Triangle) p;
                p1.set(coordList.get(t.indexP1));
                p2.set(coordList.get(t.indexP2));
                p3.set(coordList.get(t.indexP3));
                normal.set(VectorUtils.normal(p1, p3, p2));
                normal.add(VectorUtils.normal(p2, p1, p3));
                normal.add(VectorUtils.normal(p3, p2, p1));
                normal.scale(1.0f / 3);
                normList.get(t.indexN1).add(normal);
                normList.get(t.indexN2).add(normal);
                normList.get(t.indexN3).add(normal);
            } else {
                Quad q = (Quad) p;
                p1.set(coordList.get(q.indexP1));
                p2.set(coordList.get(q.indexP2));
                p3.set(coordList.get(q.indexP3));
                p4.set(coordList.get(q.indexP4));
                normal.set(VectorUtils.normal(p1, p4, p2));
                normal.add(VectorUtils.normal(p2, p1, p3));
                normal.add(VectorUtils.normal(p3, p2, p4));
                normal.add(VectorUtils.normal(p4, p3, p1));
                normal.scale(1.0f / 4);
                normList.get(q.indexN1).add(normal);
                normList.get(q.indexN2).add(normal);
                normList.get(q.indexN3).add(normal);
                normList.get(q.indexN4).add(normal);
            }
        }
        for (Vector3f n : normList) n.normalize();
        for (Primitive p : primitives) if (p instanceof Quad) {
            Quad q = (Quad) p;
            float l1 = normList.get(q.indexN1).length();
            float l2 = normList.get(q.indexN2).length();
            float l3 = normList.get(q.indexN3).length();
            float l4 = normList.get(q.indexN4).length();
            if (valorNoValido(l1) || valorNoValido(l2) || valorNoValido(l3) || valorNoValido(l4)) {
                System.out.println(l1 + " " + l2 + " " + l3 + " " + l4);
            }
        }
    }

    private static void generateTangents(final Queue<Primitive> primitives, final List<Point3f> coordList, Vector3f[][] tangents) {
        for (int i = 0; i < tangents[0].length; i++) {
            tangents[0][i] = new Vector3f();
            tangents[1][i] = new Vector3f();
        }
        for (Primitive p : primitives) p.generateTangents(coordList, tangents);
    }

    /**
	 * @return dot( cross(v1, v2), v3 )
	 */
    private static float dotcross(Vector3f v1, Vector3f v2, Vector3f v3) {
        return ((v1.y * v2.z - v1.z * v2.y) * v3.x + (v2.x * v1.z - v2.z * v1.x) * v3.y + (v1.x * v2.y - v1.y * v2.x) * v3.z);
    }

    @SuppressWarnings("unused")
    private static void calculateTangent(Vector3f normal, Vector2f s, Vector2f t, Vector3f uVec, Vector3f vVec, Vector4f t4f) {
        Vector3f sDir = new Vector3f((t.y * uVec.x - t.x * vVec.x), (t.y * uVec.y - t.x * vVec.y), (t.y * uVec.z - t.x * vVec.z));
        sDir.normalize();
        Vector3f tDir = new Vector3f((s.y * uVec.x + s.x * vVec.x), (s.y * uVec.y + s.x * vVec.y), (s.y * uVec.z + s.x * vVec.z));
        t4f.set(sDir.x, sDir.y, sDir.z, dotcross(normal, sDir, tDir) < 0.0f ? -1.0f : 1.0f);
    }

    @SuppressWarnings("unused")
    static void calculateTangent(Vector3f normal, Vector2f s, Vector2f t, Vector4f t4f) {
        Vector3f sDir = new Vector3f(Math.abs(normal.z), 0.0f, -normal.x * Math.signum(normal.z));
        VectorUtils.orthogonalizeGramSchmidt(normal, sDir);
        Vector3f tDir = new Vector3f(0.0f, 1.0f, 0.0f);
        VectorUtils.orthogonalizeGramSchmidt(normal, tDir);
        t4f.set(sDir.x, sDir.y, sDir.z, dotcross(normal, sDir, tDir) < 0.0f ? -1.0f : 1.0f);
    }

    private final int flags;

    private final List<Point3f> coordList;

    private final List<Vector3f> normList;

    private final List<TexCoord2f> texList;

    private final Queue<Primitive> primitives;

    private float minX, maxX;

    private float minY, maxY;

    private float minZ, maxZ;

    private ObjLoader(int flags) {
        this.flags = flags;
        coordList = new ArrayList<Point3f>(512);
        texList = new ArrayList<TexCoord2f>(512);
        normList = new ArrayList<Vector3f>(512);
        primitives = new LinkedList<Primitive>();
        minX = Float.MAX_VALUE;
        maxX = -Float.MAX_VALUE;
        minY = Float.MAX_VALUE;
        maxY = -Float.MAX_VALUE;
        minZ = Float.MAX_VALUE;
        maxZ = -Float.MAX_VALUE;
    }

    private void read(ObjParser st) throws ParsingErrorException {
        do {
            st.getToken();
            if (st.ttype == StreamTokenizer.TT_WORD) {
                if (st.sval.equals("v")) {
                    Point3f v = readVertex(st);
                    coordList.add(v);
                    if (v.x < minX) minX = v.x;
                    if (v.x > maxX) maxX = v.x;
                    if (v.y < minY) minY = v.y;
                    if (v.y > maxY) maxY = v.y;
                    if (v.z < minZ) minZ = v.z;
                    if (v.z > maxZ) maxZ = v.z;
                } else if (st.sval.equals("vn")) normList.add(readNormal(st)); else if (st.sval.equals("vt")) {
                    TexCoord2f t = readTexture(st);
                    if ((flags & MUST_FLIP_VERTICALLY_TEXCOORDS) != 0) t.y = 1.0f - t.y;
                    texList.add(t);
                } else if (st.sval.equals("f") || st.sval.equals("fo")) primitives.offer(readPrimitive(st, coordList.size(), texList.size(), normList.size())); else if (st.sval.equals("g")) st.skipToNextLine(); else if (st.sval.equals("s")) st.skipToNextLine(); else if (st.sval.equals("p")) st.skipToNextLine(); else if (st.sval.equals("l")) st.skipToNextLine(); else if (st.sval.equals("mtllib")) st.skipToNextLine(); else if (st.sval.equals("usemtl")) st.skipToNextLine(); else if (st.sval.equals("maplib")) st.skipToNextLine(); else if (st.sval.equals("usemap")) st.skipToNextLine(); else throw new ParsingErrorException("Unrecognized token, line " + st.lineno());
            }
            st.skipToNextLine();
        } while (st.ttype != StreamTokenizer.TT_EOF);
    }

    private double getMaxDistance() {
        double absX = maxX - minX;
        double absY = maxY - minY;
        double absZ = maxZ - minZ;
        return Math.max(absX, Math.max(absY, absZ));
    }

    private Matrix4d escalarYCentrar() {
        double centerX = (minX + maxX) / 2;
        double centerY = (minY + maxY) / 2;
        double centerZ = (minZ + maxZ) / 2;
        double scale = 1.0 / Math.max(1.0, getMaxDistance());
        Matrix4d mt = new Matrix4d();
        mt.set(scale, new Vector3d(-centerX * scale, -centerY * scale, -centerZ * scale));
        return mt;
    }

    /**
	 * @param mt  
	 */
    private GeometriaAbs generarGeometria(Matrix4d mt) {
        throw new UnsupportedOperationException();
    }

    private static int getGLPrimitive(Primitive primitive, int flags) {
        if ((flags & ObjLoader.GENERATE_NTB) != 0) return GL.GL_LINES;
        if ((flags & ObjLoader.WIREFRAME) != 0) return GL2.GL_QUADS;
        if ((flags & ObjLoader.TRIANGULATE) != 0 || primitive instanceof Triangle) return GL.GL_TRIANGLES;
        return GL2.GL_QUADS;
    }

    private OglList almacenarGeometria(GL2 gl, Matrix4d mt) {
        boolean textCoord = texList.size() > 0;
        boolean normal = normList.size() > 0;
        clearDist = (float) (getMaxDistance() * 0.0001);
        regenerateNormals(primitives, coordList, normList);
        if ((flags & RESIZE) != 0) {
            if (mt != null) mt.mul(escalarYCentrar()); else mt = escalarYCentrar();
        }
        if (mt != null) {
            for (Point3f v : coordList) mt.transform(v);
            for (Vector3f n : normList) {
                mt.transform(n);
                n.normalize();
            }
        }
        OglList oglList = new OglList(gl);
        if ((flags & ObjLoader.GENERATE_TANGENTS) != 0) {
            Vector3f[][] tb = new Vector3f[2][normList.size()];
            generateTangents(primitives, coordList, tb);
            for (int i = 0; i < normList.size(); i++) {
                VectorUtils.orthogonalizeGramSchmidt(normList.get(i), tb[0][i]);
                VectorUtils.orthogonalizeGramSchmidt(normList.get(i), tb[1][i]);
            }
            Generator generator = (flags & ObjLoader.GENERATE_NTB) != 0 ? new Generator.NTBGenerator(0.25f) : Generator.VerticesTangents;
            if ((flags & ObjLoader.TRIANGULATE) != 0) {
                gl.glBegin(getGLPrimitive(primitives.element(), flags));
                for (Primitive p : primitives) for (Triangle t : p.toTriangles()) t.generate(gl, generator, coordList, normList, tb, texList);
            } else {
                int glPrimitivePre = getGLPrimitive(primitives.element(), flags);
                gl.glBegin(glPrimitivePre);
                for (Primitive p : primitives) {
                    int glPrimitiveCur = getGLPrimitive(p, flags);
                    if (glPrimitiveCur != glPrimitivePre) {
                        gl.glEnd();
                        gl.glBegin(glPrimitiveCur);
                    }
                    p.generate(gl, generator, coordList, normList, tb, texList);
                    glPrimitivePre = glPrimitiveCur;
                }
            }
        } else {
            Generator generator;
            if ((flags & WIREFRAME) != 0) generator = Generator.VerticesWireFrame; else if (normal && textCoord) generator = Generator.VerticesNormalsTexCoords; else if (textCoord) generator = Generator.VerticesTexCoords; else if (normal) generator = Generator.VerticesNormals; else generator = Generator.Vertices;
            if ((flags & ObjLoader.TRIANGULATE) != 0) {
                gl.glBegin(getGLPrimitive(primitives.element(), flags));
                for (Primitive p : primitives) for (Triangle t : p.toTriangles()) t.generate(gl, generator, coordList, normList, texList);
            } else {
                int glPrimitivePre = getGLPrimitive(primitives.element(), flags);
                gl.glBegin(glPrimitivePre);
                for (Primitive p : primitives) {
                    int glPrimitiveCur = getGLPrimitive(p, flags);
                    if (glPrimitiveCur != glPrimitivePre) {
                        gl.glEnd();
                        gl.glBegin(glPrimitiveCur);
                    }
                    p.generate(gl, generator, coordList, normList, texList);
                    glPrimitivePre = glPrimitiveCur;
                }
            }
        }
        gl.glEnd();
        OglList.endList(gl);
        if (textCoord) texList.clear();
        if (normal) normList.clear();
        coordList.clear();
        primitives.clear();
        return oglList;
    }

    private static Objeto3D load(Reader reader, int flags, Matrix4d mt) throws ParsingErrorException {
        ObjLoader loader = new ObjLoader(flags);
        loader.read(new ObjParser(new BufferedReader(reader)));
        Apariencia ap = new Apariencia();
        if ((flags & ObjLoader.GENERATE_NTB) == 0) ap.setMaterial(Material.DEFAULT);
        if ((flags & TO_GEOMETRY) != 0) return new Objeto3D(loader.generarGeometria(mt), ap);
        return new Objeto3D(loader.almacenarGeometria(GLU.getCurrentGL().getGL2(), mt), ap);
    }

    /**
	 * Método que carga un fichero <i>Wavefront .obj</i> y devuelve el {@code Objeto3D}
	 * ahí almacenado.
	 * @param filename Ruta y nombre del fichero a cargar.
	 * @return El {@code Objeto3D} cargado.
	 * @throws FileNotFoundException Si no se encuentra el fichero indicado.
	 * @throws ParsingErrorException Si el fichero está malformado.
	 */
    public static Objeto3D load(String filename) throws FileNotFoundException, ParsingErrorException {
        return load(filename, NONE, null);
    }

    /**
	 * Método que carga un fichero <i>Wavefront .obj</i> y devuelve el {@code Objeto3D}
	 * ahí almacenado.
	 * @param filename Ruta y nombre del fichero a cargar.
	 * @param flags Entero que enmascara, una combinacion de las constantes, que indican,
	 * las posibles modificaciones que pueden aplicarse, a los datos cargados.
	 * @return El {@code Objeto3D} cargado.
	 * @throws FileNotFoundException Si no se encuentra el fichero indicado.
	 * @throws ParsingErrorException Si el fichero está malformado.
	 */
    public static Objeto3D load(String filename, int flags) throws FileNotFoundException, ParsingErrorException {
        return load(filename, flags, null);
    }

    /**
	 * Método que carga un fichero <i>Wavefront .obj</i> y devuelve el {@code Objeto3D}
	 * ahí almacenado.
	 * @param filename Ruta y nombre del fichero a cargar.
	 * @param mt {@code Matrix4d} que modifica la geometría al ser cargada.
	 * @return El {@code Objeto3D} cargado.
	 * @throws FileNotFoundException Si no se encuentra el fichero indicado.
	 * @throws ParsingErrorException Si el fichero está malformado.
	 */
    public static Objeto3D load(String filename, Matrix4d mt) throws FileNotFoundException, ParsingErrorException {
        return load(filename, NONE, mt);
    }

    /**
	 * Método que carga un fichero <i>Wavefront .obj</i> y devuelve el {@code Objeto3D}
	 * ahí almacenado.
	 * @param filename Ruta y nombre del fichero a cargar.
	 * @param flags Entero que enmascara, una combinacion de las constantes, que indican,
	 * las posibles modificaciones que pueden aplicarse, a los datos cargados.
	 * @param mt {@code Matrix4d} que modifica la geometría al ser cargada.
	 * @return El {@code Objeto3D} cargado.
	 * @throws FileNotFoundException Si no se encuentra el fichero indicado.
	 * @throws ParsingErrorException Si el fichero está malformado.
	 */
    public static Objeto3D load(String filename, int flags, Matrix4d mt) throws FileNotFoundException, ParsingErrorException {
        return load(new FileReader(filename), flags, mt);
    }

    /**
	 * Método que carga un archivo <i>Wavefront .obj</i> y devuelve el {@code Objeto3D}
	 * ahí almacenado.
	 * @param url {@code URL} del archivo a cargar.
	 * @return El {@code Objeto3D} cargado.
	 * @throws IOException Si se produce un error durante la lectura.
	 * @throws ParsingErrorException Si el fichero está malformado.
	 */
    public static Objeto3D load(URL url) throws IOException, ParsingErrorException {
        return load(url, NONE, null);
    }

    /**
	 * Método que carga un archivo <i>Wavefront .obj</i> y devuelve el {@code Objeto3D}
	 * ahí almacenado.
	 * @param url {@code URL} del archivo a cargar.
	 * @param flags Entero que enmascara, una combinacion de las constantes, que indican,
	 * las posibles modificaciones que pueden aplicarse, a los datos cargados.
	 * @return El {@code Objeto3D} cargado.
	 * @throws IOException Si se produce un error durante la lectura.
	 * @throws ParsingErrorException Si el fichero está malformado.
	 */
    public static Objeto3D load(URL url, int flags) throws IOException, ParsingErrorException {
        return load(url, flags, null);
    }

    /**
	 * Método que carga un archivo <i>Wavefront .obj</i> y devuelve el {@code Objeto3D}
	 * ahí almacenado.
	 * @param url {@code URL} del archivo a cargar.
	 * @param mt {@code Matrix4d} que modifica la geometría al ser cargada.
	 * @return El {@code Objeto3D} cargado.
	 * @throws IOException Si se produce un error durante la lectura.
	 * @throws ParsingErrorException Si el fichero está malformado.
	 */
    public static Objeto3D load(URL url, Matrix4d mt) throws IOException, ParsingErrorException {
        return load(url, NONE, mt);
    }

    /**
	 * Método que carga un archivo <i>Wavefront .obj</i> y devuelve el {@code Objeto3D}
	 * ahí almacenado.
	 * @param url {@code URL} del archivo a cargar.
	 * @param flags Entero que contiene una combinación de los 
	 * @param mt {@code Matrix4d} que modifica la geometría al ser cargada.
	 * @return El {@code Objeto3D} cargado.
	 * @throws IOException Si se produce un error durante la lectura.
	 * @throws ParsingErrorException Si el fichero está malformado.
	 */
    public static Objeto3D load(URL url, int flags, Matrix4d mt) throws IOException, ParsingErrorException {
        return load(new InputStreamReader(url.openStream()), flags, mt);
    }
}
