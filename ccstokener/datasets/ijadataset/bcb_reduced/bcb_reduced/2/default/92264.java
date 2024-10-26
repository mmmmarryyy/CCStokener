import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.accessibility.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import java.io.*;
import java.applet.*;
import java.net.*;

/**
 * JTree Demo
 *
 * @version 1.10 07/26/04
 * @author Jeff Dinkins
 */
public class TreeDemo extends DemoModule {

    JTree tree;

    /**
     * main method allows us to run as a standalone demo.
     */
    public static void main(String[] args) {
        TreeDemo demo = new TreeDemo(null);
        demo.mainImpl();
    }

    /**
     * TreeDemo Constructor
     */
    public TreeDemo(SwingSet2 swingset) {
        super(swingset, "TreeDemo", "toolbar/JTree.gif");
        getDemoPanel().add(createTree(), BorderLayout.CENTER);
    }

    public JScrollPane createTree() {
        DefaultMutableTreeNode top = new DefaultMutableTreeNode(getString("TreeDemo.music"));
        DefaultMutableTreeNode catagory = null;
        DefaultMutableTreeNode artist = null;
        DefaultMutableTreeNode record = null;
        URL url = getClass().getResource("/resources/tree.txt");
        try {
            InputStream is = url.openStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader reader = new BufferedReader(isr);
            String line = reader.readLine();
            while (line != null) {
                char linetype = line.charAt(0);
                switch(linetype) {
                    case 'C':
                        catagory = new DefaultMutableTreeNode(line.substring(2));
                        top.add(catagory);
                        break;
                    case 'A':
                        if (catagory != null) {
                            catagory.add(artist = new DefaultMutableTreeNode(line.substring(2)));
                        }
                        break;
                    case 'R':
                        if (artist != null) {
                            artist.add(record = new DefaultMutableTreeNode(line.substring(2)));
                        }
                        break;
                    case 'S':
                        if (record != null) {
                            record.add(new DefaultMutableTreeNode(line.substring(2)));
                        }
                        break;
                    default:
                        break;
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
        }
        tree = new JTree(top) {

            public Insets getInsets() {
                return new Insets(5, 5, 5, 5);
            }
        };
        tree.setName("TreeDemo.tree");
        tree.setEditable(true);
        return new JScrollPane(tree);
    }

    void updateDragEnabled(boolean dragEnabled) {
        tree.setDragEnabled(dragEnabled);
    }
}
