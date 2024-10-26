package atv;

import java.io.*;
import java.net.*;
import java.util.*;

/**

@author Christian M. Zmasek

@version 1.200 -- last modified: 10/08/01

*/
public class TreeHelper {

    /** 

    Rounds d to an int.

    */
    public static int roundToInt(double d) {
        return (int) (d + 0.5);
    }

    /**

    Reads a Tree in NH or NHX format from a textfile f.

    */
    public static Tree readNHtree(File f) throws Exception {
        String incoming = "";
        StringBuffer sb = null;
        BufferedReader in = null;
        if (!f.exists()) {
            throw new Exception(f.getAbsolutePath() + " does not exist.");
        } else if (!f.isFile()) {
            throw new Exception(f.getAbsolutePath() + " is not a file.");
        }
        sb = new StringBuffer((int) f.length());
        in = new BufferedReader(new FileReader(f));
        while ((incoming = in.readLine()) != null) {
            sb.append(incoming);
        }
        in.close();
        return new Tree(toNHX.insertBt(sb.toString()));
    }

    /**

    Reads a Tree in NH or NHX format from a URL url.

    */
    public static Tree readNHtree(URL url) throws Exception {
        String incoming = "";
        StringBuffer sb = null;
        BufferedReader in = null;
        sb = new StringBuffer(10000);
        in = new BufferedReader(new InputStreamReader(url.openStream()));
        while ((incoming = in.readLine()) != null) {
            sb.append(incoming);
        }
        in.close();
        return new Tree(sb.toString());
    }

    /**

    Writes a Tree t to a textfile f.
    Set boolean nhx to true to write Tree in New Hampshire X (NHX) format.
    Set boolean nhx to false to write Tree in New Hampshire (NH) format.
    Both overwrite1 and overwrite2 need to be true to allow for overwriting.

    */
    public static void writeNHtree(Tree t, File f, boolean nhx, boolean overwrite1, boolean overwrite2) throws Exception {
        if (t.isEmpty()) {
            String message = "writeNHtree( Tree, File, boolean, boolean,";
            message += " boolean ): Tree must not be empty.";
            throw new Exception(message);
        }
        if (f.exists() && !(overwrite1 && overwrite2)) {
            throw new Exception(f.getAbsolutePath() + " does already exist and is not allowed to be overwritten.");
        }
        if (f.exists() && !f.isFile()) {
            throw new Exception(f.getAbsolutePath() + " is not a file. Cannot be overwritten.");
        }
        String s;
        s = t.toNewHampshireX();
        s = toNHX.toNewick(s);
        try {
            PrintWriter out = new PrintWriter(new FileWriter(f), true);
            out.println(s);
            out.close();
        } catch (Exception e) {
            throw new Exception("writeNHtree( Tree, File, boolean, boolean, boolean ): " + e);
        }
    }

    /**
    
    Reads in multiple Trees from a File multipletreefile,
    containing Tree descriptions in New Hampshire (NH) or
    New Hampshire X (NHX) format separated by semicolons followed
    by a newline. Returns a array of Trees.
    
    @param multipletreefile Textfile containg Tree descriptions 
                            in NH or NHX format separated by
                            semicolons followd by a newline
                            
    @return Tree[]                        
    
    */
    public static Tree[] readMultipleNHTrees(File multipletreefile) throws Exception {
        String incoming = "";
        StringBuffer sb = null;
        BufferedReader in1 = null, in2 = null;
        int number_of_trees = 0, j = 0, size = 0;
        Tree[] t = null;
        if (!multipletreefile.exists()) {
            throw new Exception(multipletreefile.getAbsolutePath() + " does not exist.");
        } else if (!multipletreefile.isFile()) {
            throw new Exception(multipletreefile.getAbsolutePath() + " is not a file.");
        }
        in1 = new BufferedReader(new FileReader(multipletreefile));
        while ((incoming = in1.readLine()) != null) {
            if (incoming.indexOf(";") != -1) {
                number_of_trees++;
            }
        }
        in1.close();
        t = new Tree[number_of_trees];
        size = (int) (multipletreefile.length() / number_of_trees) + 1;
        sb = new StringBuffer(size);
        incoming = "";
        in2 = new BufferedReader(new FileReader(multipletreefile));
        while ((incoming = in2.readLine()) != null) {
            sb.append(incoming);
            if (incoming.indexOf(";") != -1) {
                t[j++] = new Tree(sb.toString());
                sb = new StringBuffer(size);
            }
        }
        in2.close();
        return t;
    }

    /**

    Copies the content of the sequence name field to the
    species name field (if empty - does not overwrite!)
    for each node of Tree tree.
    <p>
    If the sequence name appears to be (contain) a SWISS-PROT
    name, the species name is extracted in the following manner:
    It extracts all characters after the <b>last<\b> "_" and before
    any potential "/", "-", "\", ";", ".".
    <p>
    If the sequence name appears not to be (contain) a SWISS-PROT,
    name it is just copied unchanged to the species name field.
    <p>
    (last modified: 10/03/01)

    @param tree the Tree for which species names are to be extracted/copied

    */
    public static void extractSpeciesNameFromSeqName(Tree tree) {
        PreorderTreeIterator it = null;
        int i = 0;
        try {
            it = new PreorderTreeIterator(tree);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Unexpected Error: Could not create iterator. Terminating.");
            System.exit(-1);
        }
        String seqname = "";
        while (!it.isDone()) {
            if (it.currentNode().getSpecies().length() < 1) {
                seqname = it.currentNode().getSeqName();
                i = seqname.lastIndexOf("_");
                if (i >= 0) {
                    seqname.trim();
                    seqname = seqname.substring(i + 1);
                    i = seqname.indexOf("/");
                    if (i >= 0) {
                        seqname = seqname.substring(0, i);
                    }
                    i = seqname.indexOf("-");
                    if (i >= 0) {
                        seqname = seqname.substring(0, i);
                    }
                    i = seqname.indexOf("\\");
                    if (i >= 0) {
                        seqname = seqname.substring(0, i);
                    }
                    i = seqname.indexOf(";");
                    if (i >= 0) {
                        seqname = seqname.substring(0, i);
                    }
                    i = seqname.indexOf(".");
                    if (i >= 0) {
                        seqname = seqname.substring(0, i);
                    }
                    it.currentNode().setSpecies(seqname);
                } else {
                    it.currentNode().setSpecies(seqname);
                }
            }
            it.next();
        }
    }

    /**

    For each external node of Tree tree: Cleans up SWISS-PROT 
    species names: It removes everything (including ) after a potential 
    "/", "_", "-", "\", ";", ".". It removes everything which 
    comes after the fifth letter.
    <p>
    (last modified: 10/03/01)

    */
    public static void cleanSpeciesNamesInExtNodes(Tree tree) {
        Node node = tree.getExtNode0();
        String species = "";
        int i = 0;
        while (node != null) {
            species = node.getSpecies().trim();
            if (species.length() > 0) {
                i = species.indexOf("/");
                if (i >= 0) {
                    species = species.substring(0, i);
                }
                i = species.indexOf("_");
                if (i >= 0) {
                    species = species.substring(0, i);
                }
                i = species.indexOf("-");
                if (i >= 0) {
                    species = species.substring(0, i);
                }
                i = species.indexOf("\\");
                if (i >= 0) {
                    species = species.substring(0, i);
                }
                i = species.indexOf(";");
                if (i >= 0) {
                    species = species.substring(0, i);
                }
                i = species.indexOf(".");
                if (i >= 0) {
                    species = species.substring(0, i);
                }
                node.setSpecies(species);
            }
            node = node.getNextExtNode();
        }
    }

    /**

    Calculates the mean and standard deviation of all nodes of 
    Tree t which have a boostrap values zero or more.
    Returns null in case of failure (e.g t has no bootstrap values,
    or just one).
    <p>
    (Last modified: 05/28/01)


    @param  t reference to a tree with bootstrap values

    @return Array of doubles, [0] is the mean, [1] the standard
            deviation
    
    */
    public static double[] calculateMeanBoostrapValue(Tree t) {
        int b = 0, n = 0;
        long sum = 0;
        double x = 0.0, mean = 0.0;
        double[] da = new double[2];
        Vector bv = new Vector();
        Node node = null;
        PreorderTreeIterator i = null;
        try {
            i = new PreorderTreeIterator(t);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("TreeHelper: Unexpected failure.");
            System.exit(-1);
        }
        while (!i.isDone()) {
            node = i.currentNode();
            if (!(node.getParent() != null && node.getParent().isRoot() && node.getParent().getChild1().getBootstrap() > 0 && node.getParent().getChild2().getBootstrap() > 0 && node.getParent().getChild2() == node)) {
                b = node.getBootstrap();
                if (b > 0) {
                    sum += b;
                    bv.addElement(new Integer(b));
                    n++;
                }
            }
            i.next();
        }
        if (n < 2) {
            return null;
        }
        mean = (double) sum / n;
        sum = 0;
        for (int j = 0; j < n; ++j) {
            b = ((Integer) bv.elementAt(j)).intValue();
            x = (double) b - mean;
            sum += (x * x);
        }
        da[0] = mean;
        da[1] = java.lang.Math.sqrt((double) sum / (n - 1.0));
        return da;
    }

    /**

    Checks whether String s is empty.

    @return true if empty, false otherwise

    */
    public static boolean isEmpty(String s) {
        return s.length() < 1;
    }

    /**

    Removes all white space from String s.

    @return String s with white space removed

    */
    public static String removeWhiteSpace(String s) {
        int i;
        for (i = 0; i <= s.length() - 1; i++) {
            if (s.charAt(i) == ' ' || s.charAt(i) == '\t' || s.charAt(i) == '\n' || s.charAt(i) == '\r') {
                s = s.substring(0, i) + s.substring(i + 1);
                i--;
            }
        }
        return s;
    }

    /**

    Removes everythin between '[' and ']' -- except between '[&&NHX' and ']'.

    */
    public static String removeComments(String s) {
        int i = 0, j = 0, x = 0;
        boolean done = false;
        for (i = 0; i < s.length() - 1; i++) {
            if (s.charAt(i) == '[' && (i > s.length() - 3 || !(s.charAt(i + 1) == '&' && s.charAt(i + 2) == '&' && s.charAt(i + 3) == 'N' && s.charAt(i + 4) == 'H' && s.charAt(i + 5) == 'X'))) {
                j = i;
                i++;
                done = false;
                while (i < s.length() && !done) {
                    if (s.charAt(i) == '[') {
                        x++;
                    } else if (s.charAt(i) == ']') {
                        if (x == 0) {
                            s = s.substring(0, j) + s.substring(i + 1);
                            i = j - 2;
                            done = true;
                        } else {
                            x--;
                        }
                    }
                    i++;
                }
            }
        }
        return s;
    }

    /**

    Checks whether number of "(" equals number of ")" in String
    nh_string potentially representing a Tree in NH or NHX format.

    @return total number of  open parantheses if no error detected,
    -1 for faulty string

    */
    public static int countAndCheckParantheses(String nh_string) {
        int openparantheses = 0, closeparantheses = 0, i;
        for (i = 0; i <= nh_string.length() - 1; i++) {
            if (nh_string.charAt(i) == '(') openparantheses++;
            if (nh_string.charAt(i) == ')') closeparantheses++;
        }
        if (closeparantheses != openparantheses) {
            return -1;
        } else {
            return openparantheses;
        }
    }

    /**

    Checks the commas of a String nh_string potentially representing a Tree in
    NH or NHX format. Checks for "()", "(" not preceded by a "("
    or ",", ",,", "(,", and ",)".

    @return true if no error detected, false for faulty string

    */
    public static boolean checkCommas(String nh_string) {
        int i;
        for (i = 0; i <= nh_string.length() - 2; i++) {
            if ((nh_string.charAt(i) == '(' && nh_string.charAt(i + 1) == ')') || (nh_string.charAt(i) != ',' && nh_string.charAt(i) != '(' && nh_string.charAt(i + 1) == '(') || (nh_string.charAt(i) == ',' && nh_string.charAt(i + 1) == ',') || (nh_string.charAt(i) == '(' && nh_string.charAt(i + 1) == ',') || (nh_string.charAt(i) == ',' && nh_string.charAt(i + 1) == ')')) {
                return false;
            }
        }
        return true;
    }

    /**

    Sets the species names of the external Nodes of Tree t to a random
    positive integer number between (and including) min and max. 

    @param t whose external species names are to be randomized
    @param min minimal value for random numbers
    @param max maximum value for random numbers

    */
    public static void randomizeSpecies(int min, int max, Tree t) {
        if (t == null || t.isEmpty()) {
            return;
        }
        int mi = Math.abs(min);
        int ma = Math.abs(max);
        Random r = new Random();
        Node n = t.getExtNode0();
        while (n != null) {
            n.setSpecies(((Math.abs(r.nextInt()) % (ma - mi + 1)) + mi) + "");
            n = n.getNextExtNode();
        }
    }

    /**
    
    Sets the species namea of the external Nodes of Tree t to 
    ascending integers, starting with 1.

    */
    public static void numberSpeciesInOrder(Tree t) {
        if (t == null || t.isEmpty()) {
            return;
        }
        Node n = t.getExtNode0();
        int j = 1;
        while (n != null) {
            n.setSpecies(j + "");
            j++;
            n = n.getNextExtNode();
        }
    }

    /**
    
    Sets the species namea of the external Nodes of Tree t to 
    descending integers, ending with 1.

    */
    public static void numberSpeciesInDescOrder(Tree t) {
        if (t == null || t.isEmpty()) {
            return;
        }
        Node n = t.getExtNode0();
        int j = t.getRoot().getSumExtNodes();
        while (n != null) {
            n.setSpecies(j + "");
            j--;
            n = n.getNextExtNode();
        }
    }

    /**

    Sets the species name of the external Nodes of Tree t to
    1, 1+i, 2, 2+i, 3, 3+i, .... 
    Examples: i=2: 1, 3, 2, 4
              i=4: 1, 5, 2, 6, 3, 7, 4, 8
              i=8: 1, 9, 2, 10, 3, 11, 4, 12, ...

    */
    public static void intervalNumberSpecies(Tree t, int i) {
        if (t == null || t.isEmpty()) {
            return;
        }
        Node n = t.getExtNode0();
        int j = 1;
        boolean odd = true;
        while (n != null) {
            if (odd) {
                n.setSpecies(j + "");
            } else {
                n.setSpecies((j + i) + "");
                j++;
            }
            odd = !odd;
            n = n.getNextExtNode();
        }
    }

    /**
    
    Creates a completely unbalanced Tree with i external nodes.

    @return a newly created unbalanced Tree

    */
    public static Tree createUnbalancedTree(int i) {
        Tree t1 = null;
        try {
            t1 = new Tree(":S=");
            t1.setRooted(true);
            for (int j = 1; j < i; ++j) {
                t1.addNodeAndConnect("", "");
            }
            t1.setRoot(t1.getExtNode0().getRoot());
            t1.calculateRealHeight();
        } catch (Exception e) {
            System.err.println("Unexpected exception during \"createUnbalancedTree\":");
            System.err.println(e.toString());
            System.exit(-1);
        }
        return t1;
    }
}
