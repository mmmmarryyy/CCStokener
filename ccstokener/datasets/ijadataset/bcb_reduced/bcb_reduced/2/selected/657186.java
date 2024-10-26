package edu.upmc.opi.caBIG.caTIES.client.vr.utils.ncitree;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.StringTokenizer;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import org.apache.log4j.Logger;

/**
 */
public class NCIThesTreeLoader {

    /**
     * Field logger.
     */
    private static Logger logger = Logger.getLogger(NCIThesTreeLoader.class);

    /**
	 * Field url.
	 */
    URL url;

    /**
	 * Field filename.
	 */
    String filename = "Concepts2.out";

    /**
	 * @param tree JTree
	 * @throws MalformedURLException
	 */
    public NCIThesTreeLoader(JTree tree) {
        this(tree, null);
    }

    /**
	 * Constructor for NCIThesTreeLoader.
	 * @param tree JTree
	 * @param url URL
	 */
    public NCIThesTreeLoader(JTree tree, URL url) {
        nodeList = new ArrayList();
        nodeMap = new HashMap(0x30d40);
        if (url != null) this.url = url; else try {
            this.url = new URL("gate:/creole/spin/" + filename);
        } catch (MalformedURLException e) {
            logger.debug("Malformed url for location of datafile for NCI tree");
        }
        if (!loadTree(tree)) logger.debug("The NCI Tree couldnot be loaded successfully. The data file may be missing or corrupt");
    }

    /**
	 * Method loadTree.
	 * @param tree JTree
	 * @return boolean
	 */
    public boolean loadTree(JTree tree) {
        this.tree = tree;
        if (readRemoteFile()) populateTree(); else return false;
        return true;
    }

    /**
	 * Method readRemoteFile.
	 * @return boolean
	 */
    private boolean readRemoteFile() {
        InputStream inputstream;
        Concept concept = new Concept();
        try {
            inputstream = url.openStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputstream);
            BufferedReader bufferedreader = new BufferedReader(inputStreamReader);
            String s4;
            while ((s4 = bufferedreader.readLine()) != null && s4.length() > 0) {
                if (!parseLine(s4, concept)) {
                    return false;
                }
            }
        } catch (MalformedURLException e) {
            logger.fatal("malformed URL, trying to read local file");
            return readLocalFile();
        } catch (IOException e1) {
            logger.fatal("Error reading URL file, trying to read local file");
            return readLocalFile();
        } catch (Exception x) {
            logger.fatal("Failed to readRemoteFile " + x.getMessage() + ", trying to read local file");
            return readLocalFile();
        }
        return true;
    }

    /**
	  * Method readLocalFile.
	  * @return boolean
	  */
    private boolean readLocalFile() {
        Concept concept = new Concept();
        try {
            BufferedReader bufferedreader = new BufferedReader(new FileReader(filename));
            String s4;
            int i = 0;
            while ((s4 = bufferedreader.readLine()) != null && s4.length() > 0) {
                if (!parseLine(s4, concept)) return false;
                if (++i > 10000) break;
            }
        } catch (Exception exception) {
            logger.fatal("Error reading local text file.");
            return false;
        }
        return true;
    }

    /**
	 * Method parseLine.
	 * @param s String
	 * @param concept Concept
	 * @return boolean
	 */
    private boolean parseLine(String s, Concept concept) {
        StringTokenizer stringtokenizer = new StringTokenizer(s, "\t");
        try {
            concept.level = Integer.parseInt(stringtokenizer.nextToken());
            String s1 = stringtokenizer.nextToken();
            concept.cui = s1.substring(0, 8);
            concept.pt = s1.substring(9, s1.length());
            DefaultMutableTreeNode defaultmutabletreenode = new DefaultMutableTreeNode(new Concept(concept));
            nodeList.add(defaultmutabletreenode);
            nodeMap.put(concept.cui, defaultmutabletreenode);
        } catch (Exception exception) {
            logger.fatal("Error parsing input lines in file.");
            return false;
        }
        return true;
    }

    /**
	 * @return Returns the nodeList.
	 */
    public ArrayList getNodeList() {
        return nodeList;
    }

    /**
	 * @return Returns the nodeMap.
	 */
    public HashMap getNodeMap() {
        return nodeMap;
    }

    /**
	 * Method populateTree.
	 */
    private void populateTree() {
        Stack stack = new Stack();
        DefaultMutableTreeNode defaultmutabletreenode = (DefaultMutableTreeNode) nodeList.get(0);
        int i = ((Concept) defaultmutabletreenode.getUserObject()).level;
        tree.setModel(new DefaultTreeModel(defaultmutabletreenode));
        stack.push(defaultmutabletreenode);
        DefaultMutableTreeNode defaultmutabletreenode4 = defaultmutabletreenode;
        for (int k = 1; k < nodeList.size(); k++) {
            DefaultMutableTreeNode defaultmutabletreenode1 = (DefaultMutableTreeNode) nodeList.get(k);
            int j = ((Concept) defaultmutabletreenode1.getUserObject()).level;
            if (j == i) {
                DefaultMutableTreeNode defaultmutabletreenode2 = (DefaultMutableTreeNode) stack.peek();
                defaultmutabletreenode2.add(defaultmutabletreenode1);
            } else if (j > i) {
                i++;
                stack.push(defaultmutabletreenode4);
                defaultmutabletreenode4.add(defaultmutabletreenode1);
            } else {
                while (i > j) {
                    i--;
                    stack.pop();
                }
                DefaultMutableTreeNode defaultmutabletreenode3 = (DefaultMutableTreeNode) stack.peek();
                defaultmutabletreenode3.add(defaultmutabletreenode1);
            }
            defaultmutabletreenode4 = defaultmutabletreenode1;
        }
    }

    /**
	 * Field nodeList.
	 */
    private ArrayList nodeList;

    /**
	 * Field nodeMap.
	 */
    private HashMap nodeMap;

    /**
	 * Field tree.
	 */
    private JTree tree;
}
