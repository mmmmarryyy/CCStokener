package javamath.hasse.lattice;

import javamath.hasse.config.*;
import java.util.*;
import java.io.*;
import java.net.*;

/**
 * A class for inputting a lattice (or ordered set) from a file, string
 * or network connection.
 * <p>
 * @author Ralph Freese and Andrew Solomon
 * @version $Id: InputLattice.java,v 1.8 2001/09/11 02:40:45 ralphfreese Exp $
 */
public class InputLattice implements Serializable {

    public static final int FILE = 0, STRING = 1, URL = 2;

    public Vector labels;

    public Vector upperCoversVector;

    public Hashtable edgeColors;

    public static final String joinStr = "join";

    public static final String meetStr = "meet";

    public static final String joinSign = "∨";

    public static final String meetSign = "∧";

    boolean convertJoinMeet = true;

    public String name;

    protected InputLattice() {
        super();
    }

    public InputLattice(String file) throws FileNotFoundException, IOException {
        this(file, true);
    }

    public InputLattice(String file, int type) throws FileNotFoundException, IOException {
        this(file, type, true);
    }

    public InputLattice(String file, int type, boolean convertJoinMeet) throws FileNotFoundException, IOException {
        InputStream latStream = null;
        labels = new Vector();
        upperCoversVector = new Vector();
        Vector upperCovers = new Vector();
        edgeColors = new Hashtable();
        Vector upperCovering = new Vector();
        int level = 0;
        String str;
        String currentElt = null;
        String cover = null;
        boolean first = true;
        StreamTokenizer in;
        switch(type) {
            case FILE:
                latStream = new FileInputStream(file);
                break;
            case STRING:
                latStream = new StringBufferInputStream(file);
                break;
            case URL:
                URL url = new URL("http", Config.HOST, Config.PORT, "/" + Config.HASSE + "/" + Config.URL_STREAMER + file);
                System.out.println("url is " + url);
                URLConnection connection = url.openConnection();
                latStream = new DataInputStream(connection.getInputStream());
                break;
        }
        in = new StreamTokenizer(latStream);
        in.wordChars('^', '_');
        in.wordChars('*', '.');
        while (in.nextToken() != StreamTokenizer.TT_EOF) {
            if (in.ttype == StreamTokenizer.TT_WORD || in.ttype == StreamTokenizer.TT_NUMBER || in.ttype == '"') {
                if (in.ttype == StreamTokenizer.TT_NUMBER) {
                    str = "" + Math.round(in.nval);
                } else {
                    str = in.sval;
                }
                if (convertJoinMeet && level > 1) {
                    str = stringSubstitute(str, joinStr, joinSign);
                    str = stringSubstitute(str, meetStr, meetSign);
                }
                if (level == 1) {
                    name = new String(str);
                }
                if (level == 2) {
                    labels.addElement(str);
                    currentElt = str;
                }
                if (level == 3) {
                    upperCovers.addElement(str);
                }
                if (level == 4) {
                    if (first) {
                        upperCovers.addElement(str);
                        cover = str;
                        first = false;
                    } else {
                        edgeColors.put(new Edge(currentElt, cover), str);
                    }
                }
            }
            if (in.ttype == '(') {
                level++;
                if (level == 3) upperCovers = new Vector();
            }
            if (in.ttype == ')') {
                level--;
                if (level == 3) first = true;
                if (level == 2) upperCoversVector.addElement(upperCovers);
                if (level == 0) {
                    if (latStream != null) latStream.close();
                    return;
                }
            }
        }
    }

    public InputLattice(String file, boolean isFile) throws FileNotFoundException, IOException {
        this(file, isFile ? FILE : STRING);
    }

    /** InputLattice from
  * String (name) 
  * Vector of Strings  (labels)
  * Vector of vectors of strings (upper covers)
  */
    public InputLattice(String nm, Vector labelvec, Vector ucovers) {
        upperCoversVector = new Vector();
        for (int i = 0; i < ucovers.size(); i++) {
            upperCoversVector.addElement(((Vector) ucovers.elementAt(i)).clone());
        }
        name = nm;
        labels = new Vector();
        for (int i = 0; i < labelvec.size(); i++) {
            labels.addElement(labelvec.elementAt(i));
        }
        return;
    }

    /**
   * Global subsitution. Could be more efficient.
   */
    public String stringSubstitute(String str, String old, String rep) {
        int index = str.indexOf(old);
        if (index == -1) return str;
        StringBuffer sb = new StringBuffer(str.substring(0, index));
        sb.append(rep);
        sb.append(stringSubstitute(str.substring(index + old.length()), old, rep));
        return sb.toString();
    }
}
