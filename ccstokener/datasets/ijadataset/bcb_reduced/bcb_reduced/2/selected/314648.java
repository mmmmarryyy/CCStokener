package weka.core.parser.JFlex;

import java.io.*;
import java.net.URL;
import java.util.Vector;

/**
 * This class stores the skeleton of generated scanners.
 *
 * The skeleton consists of several parts that can be emitted to
 * a file. Usually there is a portion of generated code
 * (produced in class Emitter) between every two parts of skeleton code.
 *
 * There is a static part (the skeleton code) and state based iterator
 * part to this class. The iterator part is used to emit consecutive skeleton
 * sections to some <code>PrintWriter</code>. 
 *
 * @see JFlex.Emitter
 *
 * @author Gerwin Klein
 * @version JFlex 1.4.1, $Revision: 1.1 $, $Date: 2008-05-09 09:14:11 $
 */
public class Skeleton {

    /** expected number of sections in the skeleton file */
    private static final int size = 21;

    /** platform specific newline */
    private static final String NL = System.getProperty("line.separator");

    /** The skeleton */
    public static String line[];

    /** initialization */
    static {
        readDefault();
    }

    /**
   * The current part of the skeleton (an index of nextStop[])
   */
    private int pos;

    /**
   * The writer to write the skeleton-parts to
   */
    private PrintWriter out;

    /**
   * Creates a new skeleton (iterator) instance. 
   *
   * @param   out  the writer to write the skeleton-parts to
   */
    public Skeleton(PrintWriter out) {
        this.out = out;
    }

    /**
   * Emits the next part of the skeleton
   */
    public void emitNext() {
        out.print(line[pos++]);
    }

    /**
   * Make the skeleton private.
   *
   * Replaces all occurences of " public " in the skeleton with " private ". 
   */
    public static void makePrivate() {
        for (int i = 0; i < line.length; i++) {
            line[i] = replace(" public ", " private ", line[i]);
        }
    }

    /**
   * Reads an external skeleton file for later use with this class.
   * 
   * @param skeletonFile  the file to read (must be != null and readable)
   */
    public static void readSkelFile(File skeletonFile) {
        if (skeletonFile == null) throw new IllegalArgumentException("Skeleton file must not be null");
        if (!skeletonFile.isFile() || !skeletonFile.canRead()) {
            Out.error(ErrorMessages.CANNOT_READ_SKEL, skeletonFile.toString());
            throw new GeneratorException();
        }
        Out.println(ErrorMessages.READING_SKEL, skeletonFile.toString());
        try {
            BufferedReader reader = new BufferedReader(new FileReader(skeletonFile));
            readSkel(reader);
        } catch (IOException e) {
            Out.error(ErrorMessages.SKEL_IO_ERROR);
            throw new GeneratorException();
        }
    }

    /**
   * Reads an external skeleton file from a BufferedReader.
   * 
   * @param  reader             the reader to read from (must be != null)
   * @throws IOException        if an IO error occurs
   * @throws GeneratorException if the number of skeleton sections does not match 
   */
    public static void readSkel(BufferedReader reader) throws IOException {
        Vector lines = new Vector();
        StringBuffer section = new StringBuffer();
        String ln;
        while ((ln = reader.readLine()) != null) {
            if (ln.startsWith("---")) {
                lines.addElement(section.toString());
                section.setLength(0);
            } else {
                section.append(ln);
                section.append(NL);
            }
        }
        if (section.length() > 0) lines.addElement(section.toString());
        if (lines.size() != size) {
            Out.error(ErrorMessages.WRONG_SKELETON);
            throw new GeneratorException();
        }
        line = new String[size];
        for (int i = 0; i < size; i++) line[i] = (String) lines.elementAt(i);
    }

    /**
   * Replaces a with b in c.
   * 
   * @param a  the String to be replaced
   * @param b  the replacement
   * @param c  the String in which to replace a by b
   * @return a String object with a replaced by b in c 
   */
    public static String replace(String a, String b, String c) {
        StringBuffer result = new StringBuffer(c.length());
        int i = 0;
        int j = c.indexOf(a);
        while (j >= i) {
            result.append(c.substring(i, j));
            result.append(b);
            i = j + a.length();
            j = c.indexOf(a, i);
        }
        result.append(c.substring(i, c.length()));
        return result.toString();
    }

    /**
   * (Re)load the default skeleton. Looks in the current system class path.   
   */
    public static void readDefault() {
        ClassLoader l = Skeleton.class.getClassLoader();
        URL url = l.getResource("weka/core/parser/JFlex/skeleton.default");
        if (url == null) {
            Out.error(ErrorMessages.SKEL_IO_ERROR_DEFAULT);
            throw new GeneratorException();
        }
        try {
            InputStreamReader reader = new InputStreamReader(url.openStream());
            readSkel(new BufferedReader(reader));
        } catch (IOException e) {
            Out.error(ErrorMessages.SKEL_IO_ERROR_DEFAULT);
            throw new GeneratorException();
        }
    }
}
