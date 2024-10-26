package org.phylowidget.tree;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.jgrapht.WeightedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.phylowidget.PWPlatform;
import org.phylowidget.PhyloTree;
import org.phylowidget.PhyloWidget;
import org.phylowidget.render.images.ImageLoader;

public class TreeIO {

    public static final boolean DEBUG = false;

    public static RootedTree parseFile(RootedTree t, File f) {
        try {
            URI uri = f.toURI();
            URL url = uri.toURL();
            InputStream is = url.openStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            if (t instanceof PhyloTree) {
                PhyloTree pt = (PhyloTree) t;
                String str = f.getParent();
                pt.setBaseURL(str);
            }
            return parseReader(t, br);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static RootedTree parseReader(RootedTree t, BufferedReader br) throws Exception {
        String line;
        StringBuffer buff = new StringBuffer();
        translationMap.clear();
        boolean isNexus = false;
        try {
            while ((line = br.readLine()) != null) {
                if (line.indexOf("#NEXUS") != -1) {
                    isNexus = true;
                }
                buff.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        if (isNexus) {
            String newickFromNexus = getNewickFromNexus(buff.toString());
            return parseNewickString(t, newickFromNexus);
        }
        return parseNewickString(t, buff.toString());
    }

    private static boolean isNeXML(String s) {
        return s.contains("nex:nexml");
    }

    public static RootedTree parseNewickString(RootedTree tree, String s) {
        if (isNeXML(s)) {
            NexmlIO io = new NexmlIO(tree.getClass());
            try {
                return io.parseString(s);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        boolean oldEnforceUniqueLabels = tree.getEnforceUniqueLabels();
        tree.setEnforceUniqueLabels(false);
        if (tree instanceof PhyloTree) {
            PhyloTree pt = (PhyloTree) tree;
        }
        if (DEBUG) System.out.println(System.currentTimeMillis() + "\tStarting parse...");
        DefaultVertex root = null;
        int endInd = Math.min(10, s.length() - 1);
        String test = s.substring(0, endInd).toLowerCase();
        if (test.startsWith("http://") || test.startsWith("ftp://") || test.startsWith("file://")) {
            try {
                URL url = new URL(s);
                BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()));
                return TreeIO.parseReader(tree, r);
            } catch (SecurityException e) {
                e.printStackTrace();
                PWPlatform.getInstance().getThisAppContext().getPW().setMessage("Error: to load a tree from a URL, please use PhyloWidget Full!");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        boolean nhx = (s.indexOf("NHX") != -1);
        boolean poorMans = (s.indexOf(POOR_MANS_NHX) != -1);
        if (s.startsWith("'(")) s = s.substring(1);
        if (s.endsWith("'")) s = s.substring(0, s.length() - 1);
        if (s.indexOf(';') == -1) s = s + ';';
        NHXHandler nhxHandler = new NHXHandler();
        boolean stripAnnotations = false;
        if (stripAnnotations) s = nhxHandler.stripAnnotations(s);
        int[] countForDepth = new int[50];
        HashMap<DefaultVertex, DefaultVertex> firstChildren = new HashMap<DefaultVertex, DefaultVertex>();
        int curDepth = 0;
        Stack<DefaultVertex> vertices = new Stack<DefaultVertex>();
        Stack<Double> lengths = new Stack<Double>();
        boolean parsingNumber = false;
        boolean innerNode = false;
        boolean withinEscapedString = false;
        boolean withinNHX = false;
        String controlChars = "();,";
        StringBuffer temp = new StringBuffer(10000);
        String curLabel = new String();
        if (DEBUG) System.out.println(System.currentTimeMillis() + "\tChar loop...");
        long len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            boolean isControl = (c == '(' || c == ')' || c == ';' || c == ',');
            if (DEBUG) {
                if (i % (len / 50 + 1) == 0) {
                    System.out.print(".");
                }
            }
            if (c == '[' && !withinNHX) withinNHX = true; else if (withinNHX && c == ']') withinNHX = false;
            if (withinNHX) isControl = false;
            if (withinEscapedString) {
                temp.append(c);
                if (c == '\'') withinEscapedString = false;
                continue;
            } else if (c == '\'' && temp.length() == 0) {
                temp.append(c);
                withinEscapedString = true;
                continue;
            }
            if (isControl) {
                if (c == '(') {
                    curDepth++;
                    if (curDepth >= countForDepth.length) {
                        int[] newArr = new int[countForDepth.length << 2];
                        System.arraycopy(countForDepth, 0, newArr, 0, countForDepth.length);
                        countForDepth = newArr;
                    }
                }
                if (c == ')' || c == ',' || c == ';') {
                    curLabel = temp.toString();
                    curLabel = curLabel.trim();
                    if (stripAnnotations) curLabel = nhxHandler.replaceAnnotation(curLabel);
                    PhyloNode curNode = newNode(tree, curLabel, nhx, poorMans);
                    if (c == ';') {
                        root = curNode;
                    }
                    if (innerNode) {
                        DefaultVertex child = null;
                        for (int j = 0; j < countForDepth[curDepth]; j++) {
                            child = vertices.pop();
                            double length = lengths.pop();
                            Object o = null;
                            if (!tree.containsEdge(curNode, child)) o = tree.addEdge(curNode, child);
                            o = tree.getEdge(curNode, child);
                            tree.setEdgeWeight(o, length);
                        }
                        countForDepth[curDepth] = 0;
                        curDepth--;
                        firstChildren.put(curNode, child);
                    }
                    vertices.push(curNode);
                    lengths.push(curNode.getBranchLengthCache());
                    countForDepth[curDepth]++;
                    temp.replace(0, temp.length(), "");
                    parsingNumber = false;
                    innerNode = false;
                }
                if (c == ')') {
                    innerNode = true;
                }
            } else {
                temp.append(c);
            }
        }
        tree.setRoot(root);
        if (DEBUG) System.out.println(System.currentTimeMillis() + "\nSorting nodes...");
        PhyloTree pt = (PhyloTree) tree;
        BreadthFirstIterator dfi = new BreadthFirstIterator(tree, tree.getRoot());
        while (dfi.hasNext()) {
            DefaultVertex p = (DefaultVertex) dfi.next();
            if (!tree.isLeaf(p)) {
                List l = tree.getChildrenOf(p);
                if (l.get(0) != firstChildren.get(p)) {
                    tree.sorting.put(p, RootedTree.REVERSE);
                }
            }
        }
        oldTree = null;
        ((CachedRootedTree) tree).modPlus();
        if (tree.getNumEnclosedLeaves(tree.getRoot()) > 1000) tree.setEnforceUniqueLabels(false); else tree.setEnforceUniqueLabels(oldEnforceUniqueLabels);
        if (DEBUG) System.out.println(System.currentTimeMillis() + "\nDone loading tree!");
        return tree;
    }

    static RootedTree oldTree;

    public static void setOldTree(RootedTree t) {
        oldTree = t;
    }

    public static final String POOR_MANS_NHX = "**";

    public static final String POOR_MANS_DELIM = "*";

    static int newNodeCount = 0;

    static PhyloNode newNode(RootedTree t, String s, boolean useNhx, boolean poorMan) {
        PhyloNode v = new PhyloNode();
        String nameAndLength = s;
        int nhxInd = -1;
        int altNhxInd = -1;
        if (useNhx) {
            nhxInd = s.indexOf("[&&NHX");
            altNhxInd = s.indexOf("[**NHX");
            if (nhxInd != -1 || altNhxInd != -1) {
                String nhx = "";
                if (nhxInd != -1) {
                    nameAndLength = s.substring(0, nhxInd);
                    nhx = s.substring(nhxInd, s.length());
                    nhx = nhx.replaceAll("(\\[&&NHX:|\\])", "");
                } else if (altNhxInd != -1) {
                    nameAndLength = s.substring(0, altNhxInd);
                    nhx = s.substring(altNhxInd, s.length());
                    nhx = nhx.replaceAll("(\\[\\*\\*NHX:|\\])", "");
                }
                String[] attrs = nhx.split(":");
                for (String attr : attrs) {
                    attr = attr.replaceAll(COLON_REPLACE, ":");
                    int ind = attr.indexOf('=');
                    if (ind != -1) {
                        v.setAnnotation(attr.substring(0, ind), attr.substring(ind + 1, attr.length()));
                    }
                }
            }
        }
        int colonInd = nameAndLength.indexOf(":");
        String name = nameAndLength;
        double curLength = 1;
        if (colonInd != -1) {
            String length = nameAndLength.substring(colonInd + 1);
            name = nameAndLength.substring(0, colonInd);
            if (length.contains("[")) {
                int startInd = length.indexOf("[");
                int endInd = length.indexOf("]");
                String bootstrap = length.substring(startInd + 1, endInd);
                length = length.substring(0, startInd);
                if (v instanceof PhyloNode) {
                    PhyloNode pn = (PhyloNode) v;
                    pn.setAnnotation("b", bootstrap);
                }
            }
            try {
                curLength = Double.parseDouble(length);
            } catch (Exception e) {
                e.printStackTrace();
                curLength = 1;
            }
        }
        v.setBranchLengthCache(curLength);
        if (poorMan && nhxInd == -1 && altNhxInd == -1) {
            int poorInd = name.indexOf(POOR_MANS_NHX);
            if (poorInd != -1) {
                String keeperName = name.substring(0, poorInd);
                String nhx = name.substring(poorInd + POOR_MANS_NHX.length(), name.length());
                String[] attrs = nhx.split("\\" + POOR_MANS_DELIM);
                for (int i = 0; i < attrs.length; i++) {
                    String attr = attrs[i];
                    i++;
                    if (i > attrs.length - 1) break;
                    String attr2 = attrs[i];
                    v.setAnnotation(attr, attr2);
                }
                name = keeperName;
            }
        }
        s = name;
        s = translateName(s);
        s = parseNexusLabel(s);
        if (oldTree != null) {
        }
        t.addVertex(v);
        t.setLabel(v, s);
        return v;
    }

    /**
	 * Translates this node label using the translation table.
	 * 
	 * @param s
	 */
    private static String translateName(String s) {
        String mapped = translationMap.get(s);
        if (mapped != null) return mapped; else return s;
    }

    public static String createNewickString(RootedTree tree) {
        TreeOutputConfig config = new TreeOutputConfig();
        config.outputNHX = false;
        return createTreeString(tree, config);
    }

    public static String createNHXString(RootedTree tree) {
        TreeOutputConfig config = new TreeOutputConfig();
        config.outputNHX = true;
        return createTreeString(tree, config);
    }

    public static String createNeXMLString(RootedTree tree) {
        NexmlIO io = new NexmlIO(tree.getClass());
        return io.createNeXMLString(tree);
    }

    private static String createTreeString(RootedTree tree, TreeOutputConfig config) {
        if (config == null) config = new TreeOutputConfig();
        StringBuffer sb = new StringBuffer();
        synchronized (tree) {
            outputVertex(tree, sb, tree.getRoot(), config);
        }
        return sb.toString() + ";";
    }

    private static void outputVertex(RootedTree tree, StringBuffer sb, DefaultVertex v, TreeOutputConfig config) {
        if (!tree.isLeaf(v) || tree.isCollapsed(v)) {
            sb.append('(');
            List<DefaultVertex> l = tree.getChildrenOf(v);
            for (int i = 0; i < l.size(); i++) {
                outputVertex(tree, sb, l.get(i), config);
                if (i != l.size() - 1) sb.append(',');
            }
            sb.append(')');
        }
        String s = getNexusCompliantLabel(tree, v, config.includeStupidLabels, config.scrapeNaughtyChars, config.outputAllInnerNodes);
        if (s.length() != 0) sb.append(s);
        Object p = tree.getParentOf(v);
        if (p != null) {
            double ew = tree.getEdgeWeight(tree.getEdge(p, v));
            sb.append(":" + Double.toString(ew));
        }
        if (v instanceof PhyloNode && config.outputNHX) {
            PhyloNode n = (PhyloNode) v;
            HashMap<String, String> annot = n.getAnnotations();
            if (annot != null) {
                sb.append("[&&NHX");
                Set<String> set = annot.keySet();
                for (String st : set) {
                    if (st.length() == 0) continue;
                    String value = annot.get(st);
                    value = value.replaceAll(":", COLON_REPLACE);
                    sb.append(":" + st + "=" + value);
                }
                sb.append("]");
            }
        }
    }

    static final String COLON_REPLACE = "&colon;";

    static Pattern escaper = Pattern.compile("([^a-zA-Z0-9])");

    public static String escapeRE(String str) {
        return escaper.matcher(str).replaceAll("\\\\$1");
    }

    static String naughtyChars = "()[]{}/\\,;:=*'\"`<>^-+~";

    static String naughtyRegex = "[" + escapeRE(naughtyChars) + "]";

    static Pattern naughtyPattern = Pattern.compile(naughtyRegex);

    static Pattern quotePattern = Pattern.compile("'");

    public static String getNexusCompliantLabel(RootedTree t, DefaultVertex v, boolean includeStupidLabels, boolean scrapeNaughtyChars, boolean outputAllInnerNodes) {
        String s = v.toString();
        Matcher m = naughtyPattern.matcher(s);
        if (m.find()) {
            System.out.println(s);
            if (scrapeNaughtyChars) {
                s = m.replaceAll("");
                s = s.replaceAll(" ", "_");
            } else {
                Matcher quoteM = quotePattern.matcher(s);
                s = quoteM.replaceAll("''");
                s = "'" + s + "'";
            }
        } else {
            s = s.replaceAll(" ", "_");
        }
        if (!includeStupidLabels && !t.isLabelSignificant(s) && !t.isLeaf(v)) {
            boolean pr = outputAllInnerNodes;
            if (!pr) {
                s = "";
            }
        }
        return s;
    }

    static Pattern singleQuotePattern = Pattern.compile("('')");

    private static HashMap<String, String> translationMap = new HashMap<String, String>();

    private static String parseNexusLabel(String label) {
        label = label.replaceAll("`", "'");
        if (label.indexOf("'") == 0) {
            label = label.substring(1, label.length() - 1);
            Matcher m = singleQuotePattern.matcher(label);
            label = m.replaceAll("'");
        }
        if (label.indexOf("_") != -1) label = label.replace('_', ' ');
        return label;
    }

    static Pattern createPattern(String pattern) {
        return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    }

    static String removeComments(String s) {
        Pattern commentFinder = createPattern("(\\[.*?\\])");
        Matcher m = commentFinder.matcher(s);
        String output = m.replaceAll("");
        return output;
    }

    static String matchGroup(String s, String pattern, int groupNumber) {
        Pattern p = createPattern(pattern);
        Matcher m = p.matcher(s);
        m.find();
        try {
            return m.group(groupNumber);
        } catch (Exception e) {
            return "";
        }
    }

    static String getTreesBlock(String s) {
        return matchGroup(s, "begin trees;(.*)end;", 1);
    }

    static String getTranslateBlock(String s) {
        return matchGroup(s, "translate(.*?);", 1);
    }

    static String getTreeFromTreesBlock(String treesBlock) {
        return matchGroup(treesBlock, "^??tree (.*?);", 1);
    }

    static void getTranslationMap(String treesBlock) {
        String trans = getTranslateBlock(treesBlock);
        translationMap = new HashMap<String, String>();
        if (trans.length() > 0) {
            String[] pairs = trans.split(",");
            for (String pair : pairs) {
                pair = pair.trim();
                if (pair.length() < 1) continue;
                String[] twoS = pair.split("[\\s]+");
                String from = twoS[0].trim();
                String to = twoS[1].trim();
                translationMap.put(from, to);
            }
        }
    }

    static String getNewickFromNexus(String s) {
        s = removeComments(s);
        s = getTreesBlock(s);
        getTranslationMap(s);
        s = getTreeFromTreesBlock(s);
        s = s.substring(s.indexOf("=") + 1);
        s = s.trim();
        return s;
    }

    public static BufferedImage createBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }
        BufferedImage bi = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return bi;
    }

    public static void outputTreeImages(RootedTree t, File dir) {
        ImageLoader loader = PWPlatform.getInstance().getThisAppContext().trees().imageLoader;
        ArrayList<PhyloNode> nodes = new ArrayList<PhyloNode>();
        t.getAll(t.getRoot(), null, nodes);
        int img_id = 0;
        for (PhyloNode n : nodes) {
            try {
                if (n.getAnnotation("img") != null) {
                    String imgURL = n.getAnnotation("img");
                    Image img = loader.getImageForNode(n);
                    if (img != null) {
                        img = createBufferedImage(img);
                        File f = new File(dir.getAbsolutePath() + File.separator + img_id + ".jpg");
                        n.setAnnotation("img", f.toURL().toString());
                        System.out.println(n.getAnnotation("img"));
                        try {
                            ImageIO.write((RenderedImage) img, "jpg", f);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        img_id++;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    public static final class NHXHandler {

        HashMap<String, String> annotationMap = new HashMap<String, String>();

        public void clear() {
            annotationMap.clear();
        }

        Pattern annotationRegex = Pattern.compile("\\[.*?\\]");

        public String stripAnnotations(String s) {
            if (DEBUG) System.out.println("Stripping annotations... ");
            StringBuffer sb = new StringBuffer(s);
            int i = 1;
            Matcher m = annotationRegex.matcher(sb);
            int index = 0;
            while (m.find(index)) {
                String annotation = m.group();
                annotation = annotation.replaceAll("http:", "http" + COLON_REPLACE);
                annotation = annotation.replaceAll("ftp:", "ftp" + COLON_REPLACE);
                String key = ANNOT_PREFIX + String.valueOf(i) + "#";
                annotationMap.put(key, annotation);
                sb.replace(m.start(), m.end(), key);
                index = m.start();
                i++;
            }
            return sb.toString();
        }

        private static final String ANNOT_PREFIX = "#ANNOT_";

        public String replaceAnnotation(String s) {
            int ind = s.indexOf(ANNOT_PREFIX);
            if (ind != -1) {
                String annot = s.substring(ind, s.length());
                String repl = annotationMap.get(annot);
                if (repl != null) s = s.replace(annot, repl);
            }
            return s;
        }
    }

    static final class TreeOutputConfig {

        public boolean scrapeNaughtyChars;

        public boolean outputAllInnerNodes;

        public boolean outputNHX;

        public boolean includeStupidLabels;

        public boolean outputTreeImages;

        public TreeOutputConfig() {
            outputNHX = true;
            includeStupidLabels = false;
            outputTreeImages = false;
            scrapeNaughtyChars = true;
            outputAllInnerNodes = false;
        }
    }
}
