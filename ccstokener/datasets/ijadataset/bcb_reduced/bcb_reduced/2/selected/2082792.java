package org.ufba.dp.j3d.stl;

import com.sun.j3d.loaders.Loader;
import com.sun.j3d.loaders.Scene;
import com.sun.j3d.loaders.SceneBase;
import com.sun.j3d.loaders.IncorrectFormatException;
import com.sun.j3d.loaders.ParsingErrorException;
import com.sun.j3d.utils.geometry.GeometryInfo;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.ArrayList;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Shape3D;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;

/**
 * Title:         STL Loader
 * Description:   STL files loader (Supports ASCII and binary files) for Java3D
 *                Needs JDK 1.4 due to endian problems
 * Company:       Universidad del Pais Vasco (UPV/EHU)
 * @author:       Carlos Pedrinaci Godoy
 * @version:      1.0
 *
 * Contact : xenicp@yahoo.es
 *
 *
 * Things TO-DO:
 *    1.-We can't read binary files over the net.
 *    2.-For binary files if size is lower than expected (calculated with the number of faces)
 *    the program will block.
 *    3.-Improve the way for detecting the kind of stl file?
 *    Can give us problems if the comment of the binary file begins by "solid"
 */
public class StlFile implements Loader {

    private static final int DEBUG = 0;

    private static final int MAX_PATH_LENGTH = 1024;

    private int flag;

    private URL baseUrl = null;

    private String basePath = null;

    private boolean fromUrl = false;

    private boolean Ascii = true;

    private String fileName = null;

    private ArrayList coordList;

    private ArrayList normList;

    private Point3f[] coordArray = null;

    private Vector3f[] normArray = null;

    private int[] stripCounts = null;

    private String objectName = new String("Not available");

    /**
  *  Constructor
  */
    public StlFile() {
    }

    /**
   * Method that reads the EOL
   * Needed for verifying that the file has a correct format
   *
   * @param parser The file parser. An instance of StlFileParser.
   */
    private void readEOL(StlFileParser parser) {
        try {
            parser.nextToken();
        } catch (IOException e) {
            System.err.println("IO Error on line " + parser.lineno() + ": " + e.getMessage());
        }
        if (parser.ttype != StlFileParser.TT_EOL) {
            System.err.println("Format Error:expecting End Of Line on line " + parser.lineno());
        }
    }

    /**
   * Method that reads the word "solid" and stores the object name.
   * It also detects what kind of file it is
   * TO-DO:
   *    1.- Better way control of exceptions?
   *    2.- Better way to decide between Ascii and Binary?
   *
   * @param parser The file parser. An instance of StlFileParser.
   */
    private void readSolid(StlFileParser parser) {
        if (!parser.sval.equals("solid")) {
            this.setAscii(false);
        } else {
            try {
                parser.nextToken();
            } catch (IOException e) {
                System.err.println("IO Error on line " + parser.lineno() + ": " + e.getMessage());
            }
            if (parser.ttype != StlFileParser.TT_WORD) {
                System.err.println("Format Error:expecting the object name on line " + parser.lineno());
            } else {
                this.setObjectName(new String(parser.sval));
                if (DEBUG == 1) {
                    System.out.println("Object Name:" + this.getObjectName().toString());
                }
                this.readEOL(parser);
            }
        }
    }

    /**
   * Method that reads a normal
   *
   * @param parser The file parser. An instance of StlFileParser.
   */
    private void readNormal(StlFileParser parser) {
        Vector3f v = new Vector3f();
        if (!(parser.ttype == StlFileParser.TT_WORD && parser.sval.equals("normal"))) {
            System.err.println("Format Error:expecting 'normal' on line " + parser.lineno());
        } else {
            if (parser.getNumber()) {
                v.x = (float) parser.nval;
                if (DEBUG == 1) {
                    System.out.println("Normal:");
                    System.out.print("X=" + v.x + " ");
                }
                if (parser.getNumber()) {
                    v.y = (float) parser.nval;
                    if (DEBUG == 1) System.out.print("Y=" + v.y + " ");
                    if (parser.getNumber()) {
                        v.z = (float) parser.nval;
                        if (DEBUG == 1) System.out.println("Z=" + v.z);
                        this.normList.add(v);
                        this.readEOL(parser);
                    } else System.err.println("Format Error:expecting coordinate on line " + parser.lineno());
                } else System.err.println("Format Error:expecting coordinate on line " + parser.lineno());
            } else System.err.println("Format Error:expecting coordinate on line " + parser.lineno());
        }
    }

    /**
   * Method that reads the coordinates of a vector
   *
   * @param parser The file parser. An instance of StlFileParser.
   */
    private void readVertex(StlFileParser parser) {
        Point3f p = new Point3f();
        if (!(parser.ttype == StlFileParser.TT_WORD && parser.sval.equals("vertex"))) {
            System.err.println("Format Error:expecting 'vertex' on line " + parser.lineno());
        } else {
            if (parser.getNumber()) {
                p.x = (float) parser.nval;
                if (DEBUG == 1) {
                    System.out.println("Vertex:");
                    System.out.print("X=" + p.x + " ");
                }
                if (parser.getNumber()) {
                    p.y = (float) parser.nval;
                    if (DEBUG == 1) System.out.print("Y=" + p.y + " ");
                    if (parser.getNumber()) {
                        p.z = (float) parser.nval;
                        if (DEBUG == 1) System.out.println("Z=" + p.z);
                        coordList.add(p);
                        readEOL(parser);
                    } else System.err.println("Format Error: expecting coordinate on line " + parser.lineno());
                } else System.err.println("Format Error: expecting coordinate on line " + parser.lineno());
            } else System.err.println("Format Error: expecting coordinate on line " + parser.lineno());
        }
    }

    /**
   * Method that reads "outer loop" and then EOL
   *
   * @param parser The file parser. An instance of StlFileParser.
   */
    private void readLoop(StlFileParser parser) {
        if (!(parser.ttype == StlFileParser.TT_WORD && parser.sval.equals("outer"))) {
            System.err.println("Format Error:expecting 'outer' on line " + parser.lineno());
        } else {
            try {
                parser.nextToken();
            } catch (IOException e) {
                System.err.println("IO error on line " + parser.lineno() + ": " + e.getMessage());
            }
            if (!(parser.ttype == StlFileParser.TT_WORD && parser.sval.equals("loop"))) {
                System.err.println("Format Error:expecting 'loop' on line " + parser.lineno());
            } else readEOL(parser);
        }
    }

    /**
   * Method that reads "endloop" then EOL
   *
   * @param parser The file parser. An instance of StlFileParser.
   */
    private void readEndLoop(StlFileParser parser) {
        if (!(parser.ttype == StlFileParser.TT_WORD && parser.sval.equals("endloop"))) {
            System.err.println("Format Error:expecting 'endloop' on line " + parser.lineno());
        } else readEOL(parser);
    }

    /**
   * Method that reads "endfacet" then EOL
   *
   * @param parser The file parser. An instance of StlFileParser.
   */
    private void readEndFacet(StlFileParser parser) {
        if (!(parser.ttype == StlFileParser.TT_WORD && parser.sval.equals("endfacet"))) {
            System.err.println("Format Error:expecting 'endfacet' on line " + parser.lineno());
        } else readEOL(parser);
    }

    /**
   * Method that reads a face of the object
   * (Cares about the format)
   *
   * @param parser The file parser. An instance of StlFileParser.
   */
    private void readFacet(StlFileParser parser) {
        if (!(parser.ttype == StlFileParser.TT_WORD && parser.sval.equals("facet"))) {
            System.err.println("Format Error:expecting 'facet' on line " + parser.lineno());
        } else {
            try {
                parser.nextToken();
                readNormal(parser);
                parser.nextToken();
                readLoop(parser);
                parser.nextToken();
                readVertex(parser);
                parser.nextToken();
                readVertex(parser);
                parser.nextToken();
                readVertex(parser);
                parser.nextToken();
                readEndLoop(parser);
                parser.nextToken();
                readEndFacet(parser);
            } catch (IOException e) {
                System.err.println("IO Error on line " + parser.lineno() + ": " + e.getMessage());
            }
        }
    }

    /**
   * Method that reads a face in binary files
   * All binary versions of the methods end by 'B'
   * As in binary files we can read the number of faces, we don't need
   * to use coordArray and normArray (reading binary files should be faster)
   *
   * @param in The ByteBuffer with the data of the object.
   * @param index The facet index
   *
   * @throws IOException
   */
    private void readFacetB(ByteBuffer in, int index) throws IOException {
        Vector3f normal = new Vector3f();
        Point3f vertex = new Point3f();
        if (DEBUG == 1) System.out.println("Reading face number " + index);
        normArray[index] = new Vector3f();
        normArray[index].x = in.getFloat();
        normArray[index].y = in.getFloat();
        normArray[index].z = in.getFloat();
        if (DEBUG == 1) System.out.println("Normal: X=" + normArray[index].x + " Y=" + normArray[index].y + " Z=" + normArray[index].z);
        coordArray[index * 3] = new Point3f();
        coordArray[index * 3].x = in.getFloat();
        coordArray[index * 3].y = in.getFloat();
        coordArray[index * 3].z = in.getFloat();
        if (DEBUG == 1) System.out.println("Vertex 1: X=" + coordArray[index * 3].x + " Y=" + coordArray[index * 3].y + " Z=" + coordArray[index * 3].z);
        coordArray[index * 3 + 1] = new Point3f();
        coordArray[index * 3 + 1].x = in.getFloat();
        coordArray[index * 3 + 1].y = in.getFloat();
        coordArray[index * 3 + 1].z = in.getFloat();
        if (DEBUG == 1) System.out.println("Vertex 2: X=" + coordArray[index * 3 + 1].x + " Y=" + coordArray[index * 3 + 1].y + " Z=" + coordArray[index * 3 + 1].z);
        coordArray[index * 3 + 2] = new Point3f();
        coordArray[index * 3 + 2].x = in.getFloat();
        coordArray[index * 3 + 2].y = in.getFloat();
        coordArray[index * 3 + 2].z = in.getFloat();
        if (DEBUG == 1) System.out.println("Vertex 3: X=" + coordArray[index * 3 + 2].x + " Y=" + coordArray[index * 3 + 2].y + " Z=" + coordArray[index * 3 + 2].z);
    }

    /**
   * Method for reading binary files
   * Execution is completly different
   * It uses ByteBuffer for reading data and ByteOrder for retrieving the machine's endian
   * (Needs JDK 1.4)
   *
   * TO-DO:
   *  1.-Be able to read files over Internet
   *  2.-If the amount of data expected is bigger than what is on the file then
   *  the program will block forever
   *
   * @param file The name of the file
   *
   * @throws IOException
   */
    private void readBinaryFile(String file) throws IOException {
        FileInputStream data;
        ByteBuffer dataBuffer;
        byte[] Info = new byte[80];
        byte[] Array_number = new byte[4];
        byte[] Temp_Info;
        int Number_faces;
        if (DEBUG == 1) System.out.println("Machine's endian: " + ByteOrder.nativeOrder());
        if (fromUrl) {
            System.out.println("This version doesn't support reading binary files from internet");
        } else {
            data = new FileInputStream(file);
            if (80 != data.read(Info)) {
                throw new IncorrectFormatException();
            } else {
                data.read(Array_number);
                dataBuffer = ByteBuffer.wrap(Array_number);
                dataBuffer.order(ByteOrder.nativeOrder());
                Number_faces = dataBuffer.getInt();
                Temp_Info = new byte[50 * Number_faces];
                data.read(Temp_Info);
                dataBuffer = ByteBuffer.wrap(Temp_Info);
                dataBuffer.order(ByteOrder.nativeOrder());
                if (DEBUG == 1) System.out.println("Number of faces= " + Number_faces);
                coordArray = new Point3f[Number_faces * 3];
                normArray = new Vector3f[Number_faces];
                stripCounts = new int[Number_faces];
                for (int i = 0; i < Number_faces; i++) {
                    stripCounts[i] = 3;
                    try {
                        readFacetB(dataBuffer, i);
                        if (i != Number_faces - 1) {
                            dataBuffer.get();
                            dataBuffer.get();
                        }
                    } catch (IOException e) {
                        System.out.println("Format Error: iteration number " + i);
                        throw new IncorrectFormatException();
                    }
                }
            }
        }
    }

    /**
   * Method that reads ASCII files
   * Uses StlFileParser for correct reading and format checking
   * The beggining of that method is common to binary and ASCII files
   * We try to detect what king of file it is
   *
   * TO-DO:
   *  1.- Find a best way to decide what kind of file it is
   *  2.- Is that return (first catch) the best thing to do?
   *
   * @param parser The file parser. An instance of StlFileParser.
   */
    private void readFile(StlFileParser parser) {
        int t;
        try {
            parser.nextToken();
        } catch (IOException e) {
            System.err.println("IO Error on line " + parser.lineno() + ": " + e.getMessage());
            System.err.println("File seems to be empty");
            return;
        }
        readSolid(parser);
        if (getAscii()) {
            try {
                parser.nextToken();
            } catch (IOException e) {
                System.err.println("IO Error on line " + parser.lineno() + ": " + e.getMessage());
            }
            while (parser.ttype != StlFileParser.TT_EOF && !parser.sval.equals("endsolid")) {
                readFacet(parser);
                try {
                    parser.nextToken();
                } catch (IOException e) {
                    System.err.println("IO Error on line " + parser.lineno() + ": " + e.getMessage());
                }
            }
            if (parser.ttype == StlFileParser.TT_EOF) System.err.println("Format Error:expecting 'endsolid', line " + parser.lineno()); else {
                if (DEBUG == 1) System.out.println("File readed");
            }
        } else {
            try {
                readBinaryFile(getFileName());
            } catch (IOException e) {
                System.err.println("Format Error: reading the binary file");
            }
        }
    }

    /**
   * The Stl File is loaded from the .stl file specified by
   * the filename.
   * To attach the model to your scene, call getSceneGroup() on
   * the Scene object passed back, and attach the returned
   * BranchGroup to your scene graph.  For an example, see
   * $J3D/programs/examples/ObjLoad/ObjLoad.java.
   *
   * @param filename The name of the file with the object to load
   *
   * @return Scene The scene with the object loaded.
   *
   * @throws FileNotFoundException
   * @throws IncorrectFormatException
   * @throws ParsingErrorException
   */
    public Scene load(String filename) throws FileNotFoundException, IncorrectFormatException, ParsingErrorException {
        setBasePathFromFilename(filename);
        setFileName(filename);
        Reader reader = new BufferedReader(new FileReader(filename));
        return load(reader);
    }

    /**
   * The Stl file is loaded off of the web.
   * To attach the model to your scene, call getSceneGroup() on
   * the Scene object passed back, and attach the returned
   * BranchGroup to your scene graph.  For an example, see
   * $J3D/programs/examples/ObjLoad/ObjLoad.java.
   *
   * @param url The url to load the onject from
   *
   * @return Scene The scene with the object loaded.
   *
   * @throws FileNotFoundException
   * @throws IncorrectFormatException
   * @throws ParsingErrorException
   */
    public Scene load(URL url) throws FileNotFoundException, IncorrectFormatException, ParsingErrorException {
        BufferedReader reader;
        setBaseUrlFromUrl(url);
        try {
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
        } catch (IOException e) {
            throw new FileNotFoundException();
        }
        fromUrl = true;
        return load(reader);
    }

    /**
   * The Stl File is loaded from the already opened file.
   * To attach the model to your scene, call getSceneGroup() on
   * the Scene object passed back, and attach the returned
   * BranchGroup to your scene graph.  For an example, see
   * $J3D/programs/examples/ObjLoad/ObjLoad.java.
   *
   * @param reader The reader to read the object from
   *
   * @return Scene The scene with the object loaded.
   *
   * @throws FileNotFoundException
   * @throws IncorrectFormatException
   * @throws ParsingErrorException
   */
    public Scene load(Reader reader) throws FileNotFoundException, IncorrectFormatException, ParsingErrorException {
        StlFileParser st = new StlFileParser(reader);
        coordList = new ArrayList();
        normList = new ArrayList();
        setAscii(true);
        readFile(st);
        return makeScene();
    }

    /**
   * Method that takes the info from an ArrayList of Point3f
   * and returns a Point3f[].
   * Needed for ASCII files as we don't know the number of facets until the end
   *
   * @param inList The list to transform into Point3f[]
   *
   * @return Point3f[] The result.
   */
    private Point3f[] objectToPoint3Array(ArrayList inList) {
        Point3f outList[] = new Point3f[inList.size()];
        for (int i = 0; i < inList.size(); i++) {
            outList[i] = (Point3f) inList.get(i);
        }
        return outList;
    }

    /**
   * Method that takes the info from an ArrayList of Vector3f
   * and returns a Vector3f[].
   * Needed for ASCII files as we don't know the number of facets until the end
   *
   * TO-DO:
   *  1.- Here we fill stripCounts...
   *      Find a better place to do it?
   *
   * @param inList The list to transform into Point3f[]
   *
   * @return Vector3f[] The result.
   */
    private Vector3f[] objectToVectorArray(ArrayList inList) {
        Vector3f outList[] = new Vector3f[inList.size()];
        if (DEBUG == 1) System.out.println("Number of facets of the object=" + inList.size());
        stripCounts = new int[inList.size()];
        for (int i = 0; i < inList.size(); i++) {
            outList[i] = (Vector3f) inList.get(i);
            stripCounts[i] = 3;
        }
        return outList;
    }

    /**
   * Method that creates the SceneBase with the stl file info
   *
   * @return SceneBase The scene
   */
    private SceneBase makeScene() {
        SceneBase scene = new SceneBase();
        BranchGroup group = new BranchGroup();
        scene.setSceneGroup(group);
        GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_STRIP_ARRAY);
        if (this.Ascii) {
            coordArray = objectToPoint3Array(coordList);
            normArray = objectToVectorArray(normList);
        }
        gi.setCoordinates(coordArray);
        gi.setNormals(normArray);
        gi.setStripCounts(stripCounts);
        Shape3D shape = new Shape3D();
        shape.setGeometry(gi.getGeometryArray());
        group.addChild(shape);
        scene.addNamedObject(objectName, shape);
        return scene;
    }

    public URL getBaseUrl() {
        return baseUrl;
    }

    /**
   * Modifier for baseUrl, if accessing internet.
   *
   * @param url The new url
   */
    public void setBaseUrl(URL url) {
        baseUrl = url;
    }

    private void setBaseUrlFromUrl(URL url) {
        StringTokenizer stok = new StringTokenizer(url.toString(), "/\\", true);
        int tocount = stok.countTokens() - 1;
        StringBuffer sb = new StringBuffer(MAX_PATH_LENGTH);
        for (int i = 0; i < tocount; i++) {
            String a = stok.nextToken();
            sb.append(a);
        }
        try {
            baseUrl = new URL(sb.toString());
        } catch (MalformedURLException e) {
            System.err.println("Error setting base URL: " + e.getMessage());
        }
    }

    public String getBasePath() {
        return basePath;
    }

    /**
   * Set the path where files associated with this .stl file are
   * located.
   * Only needs to be called to set it to a different directory
   * from that containing the .stl file.
   *
   * @param pathName The new Path to the file
   */
    public void setBasePath(String pathName) {
        basePath = pathName;
        if (basePath == null || basePath == "") basePath = "." + java.io.File.separator;
        basePath = basePath.replace('/', java.io.File.separatorChar);
        basePath = basePath.replace('\\', java.io.File.separatorChar);
        if (!basePath.endsWith(java.io.File.separator)) basePath = basePath + java.io.File.separator;
    }

    private void setBasePathFromFilename(String fileName) {
        StringTokenizer stok = new StringTokenizer(fileName, java.io.File.separator);
        StringBuffer sb = new StringBuffer(MAX_PATH_LENGTH);
        if (fileName != null && fileName.startsWith(java.io.File.separator)) sb.append(java.io.File.separator);
        for (int i = stok.countTokens() - 1; i > 0; i--) {
            String a = stok.nextToken();
            sb.append(a);
            sb.append(java.io.File.separator);
        }
        setBasePath(sb.toString());
    }

    public int getFlags() {
        return flag;
    }

    public void setFlags(int parm) {
        this.flag = parm;
    }

    public boolean getAscii() {
        return this.Ascii;
    }

    public void setAscii(boolean tipo) {
        this.Ascii = tipo;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileName(String filename) {
        this.fileName = new String(filename);
    }

    public String getObjectName() {
        return this.objectName;
    }

    public void setObjectName(String name) {
        this.objectName = name;
    }
}
