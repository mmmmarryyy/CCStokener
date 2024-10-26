package org.velma.treeviewer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Hashtable;
import java.io.StreamTokenizer;
import java.util.Stack;

/**
 * This class represents my hacked together version of a DND file parser. The
 * functions of this class likely need to be made more robust (I'm not sure
 * every feature of the file format is accounted for), or another parser needs
 * to be found.
 * 
 * @author Andy Walsh
 * @author Jay DePasse
 * 
 */
public class DNDParser {

    public static MyTree parse(URL url, Hashtable<String, Integer> treeToAlignment) {
        try {
            return DNDParser.parse(url.openStream(), treeToAlignment);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static MyTree parse(String filename, Hashtable<String, Integer> treeToAlignment) {
        try {
            return DNDParser.parse(new FileInputStream(filename), treeToAlignment);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static MyTree parseOriginal(InputStream stream, Hashtable<String, Integer> treeToAlignment) {
        String line = null;
        MyTree tree = null;
        MyNode curr = null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            int numNodes = 0;
            int numLeaves = 0;
            while ((line = reader.readLine()) != null) {
                line = line.replace(";", "");
                if (line.equals("(")) {
                    curr = new MyNode(curr, Integer.toString(++numNodes * -1), treeToAlignment);
                    if (tree == null) {
                        tree = new MyTree(curr);
                    } else {
                        tree.addEdge(curr.parent.name, curr.name);
                    }
                } else if (line.endsWith(")")) {
                    while (line != null && line.endsWith(")")) {
                        if (!line.startsWith(":")) {
                            new MyLeaf(curr, Integer.toString(++numLeaves), line.substring(0, line.indexOf(":")), Double.parseDouble(line.substring(line.indexOf(":") + 1).replace(")", "")), treeToAlignment);
                            tree.addEdge(curr.name, Integer.toString(numLeaves));
                        }
                        line = reader.readLine();
                        if (line != null) {
                            curr.setDistanceFromParent(Double.parseDouble(line.replace(":", "").replace(")", "").replace(",", "").replace(";", "")));
                            curr = curr.parent;
                        }
                    }
                } else if (line.endsWith(",")) {
                    if (!line.startsWith(":")) {
                        new MyLeaf(curr, Integer.toString(++numLeaves), line.substring(0, line.indexOf(":")), Double.parseDouble(line.substring(line.indexOf(":") + 1).replace(",", "")), treeToAlignment);
                        tree.addEdge(curr.name, Integer.toString(numLeaves));
                    } else {
                        curr.setDistanceFromParent(Double.parseDouble(line.replace(":", "").replace(",", "")));
                    }
                } else {
                    curr.setDistanceFromParent(Double.parseDouble(line.replace(":", "").replace(");", "")));
                }
            }
            reader.close();
            tree.setNumLeaves(numLeaves);
            tree.setNumNodes(numNodes);
            return tree;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.err.println(line);
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("'" + line + "'");
            System.out.println(curr);
            e.printStackTrace();
        }
        return null;
    }

    public static MyTree parse(InputStream stream, Hashtable<String, Integer> treeToAlignment) {
        int numNodes = 0;
        int numLeaves = 0;
        Stack stack = new Stack();
        MyNode curr = null;
        stack.push(curr);
        MyTree tree = null;
        final char openBracket = '(', closeBracket = ')', childSeparator = ',', treeTerminator = ';', singleQuote = '\'', doubleQuote = '"', infoSeparator = ':';
        int thisToken;
        boolean EOT = false;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            StreamTokenizer tokenizer = newickTokenizer(reader);
            boolean atTip = false;
            char quote = '\'';
            boolean inQuote = false;
            while (EOT == false && (thisToken = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
                switch(thisToken) {
                    case singleQuote:
                        thisToken = StreamTokenizer.TT_WORD;
                    case doubleQuote:
                        thisToken = StreamTokenizer.TT_WORD;
                    case StreamTokenizer.TT_WORD:
                        curr = (MyNode) stack.peek();
                        MyLeaf leaf = new MyLeaf(curr, Integer.toString(++numLeaves), tokenizer.sval, treeToAlignment);
                        tree.addEdge(curr.name, Integer.toString(numLeaves));
                        stack.push(leaf);
                        atTip = true;
                        break;
                    case StreamTokenizer.TT_NUMBER:
                        if (atTip) {
                            ((MyLeaf) stack.peek()).setDistanceFromParent(tokenizer.nval);
                        } else {
                            ((MyNode) stack.peek()).setDistanceFromParent(tokenizer.nval);
                        }
                        atTip = false;
                        break;
                    case infoSeparator:
                        break;
                    case treeTerminator:
                    case StreamTokenizer.TT_EOF:
                        break;
                    case openBracket:
                        curr = (MyNode) stack.peek();
                        curr = new MyNode(curr, Integer.toString(++numNodes * -1), treeToAlignment);
                        if (tree == null) {
                            tree = new MyTree(curr);
                        } else {
                            tree.addEdge(curr.parent.name, curr.name);
                        }
                        stack.push(curr);
                        break;
                    case closeBracket:
                        stack.pop();
                        break;
                    case childSeparator:
                        stack.pop();
                        break;
                    default:
                        debugOutput("default " + (char) thisToken);
                        break;
                }
            }
            reader.close();
            tree.setNumLeaves(numLeaves);
            tree.setNumNodes(numNodes);
            return tree;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.err.println(" ");
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("");
            System.out.println(curr);
            e.printStackTrace();
        }
        return null;
    }

    public static StreamTokenizer newickTokenizer(BufferedReader b) {
        StreamTokenizer tokenizer = new StreamTokenizer(b);
        tokenizer.eolIsSignificant(false);
        tokenizer.wordChars('!', '!');
        tokenizer.wordChars('#', '&');
        tokenizer.wordChars('*', '+');
        tokenizer.wordChars('-', '/');
        tokenizer.wordChars('<', '<');
        tokenizer.wordChars('>', '@');
        tokenizer.wordChars('^', '`');
        tokenizer.wordChars('{', '~');
        return tokenizer;
    }

    public static void debugOutput(String s) {
        System.out.println(s);
    }
}
