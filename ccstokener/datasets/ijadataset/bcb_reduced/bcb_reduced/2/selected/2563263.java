package jsattrak.utilities;

import jsattrak.gui.JProgressDialog;
import jsattrak.gui.LoadTleDirectDialog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import jsattrak.gui.JSatTrak;
import name.gano.file.IOFileFilter;

/**
 *
 * @author sgano
 */
public class SatBrowserTleDataLoader extends SwingWorker<Boolean, ProgressStatus> {

    boolean notDone = true;

    DefaultMutableTreeNode topTreeNode;

    private Hashtable<String, TLE> tleHash;

    JSatTrak parentComponent;

    JTextArea tleOutputTextArea;

    JTree satTree;

    Hashtable<String, DefaultMutableTreeNode> mainNodesHash;

    Hashtable<String, DefaultMutableTreeNode> secondaryNodesHash;

    static String usrTLEpath = "data/tle_user";

    private JProgressDialog dialog = null;

    boolean loadTLEfromWeb = false;

    TLEDownloader tleDownloader;

    boolean userSelectedNoToDownload = false;

    int satCount = 0;

    /** Creates a new instance of ProgressBarWorker
     * @param parentComponent
     * @param topTreeNode
     * @param tleHash
     * @param tleOutputTextArea
     * @param satTree 
     */
    public SatBrowserTleDataLoader(JSatTrak parentComponent, DefaultMutableTreeNode topTreeNode, Hashtable<String, TLE> tleHash, JTextArea tleOutputTextArea, JTree satTree) {
        this.topTreeNode = topTreeNode;
        this.tleHash = tleHash;
        this.parentComponent = parentComponent;
        this.tleOutputTextArea = tleOutputTextArea;
        this.satTree = satTree;
        tleDownloader = new TLEDownloader();
        loadTLEfromWeb = false;
        if (parentComponent == null) {
            System.out.println("2 JProgress Dialog Parent == NULL");
        }
        if (!(new File(tleDownloader.getLocalPath()).exists()) || !(new File(tleDownloader.getTleFilePath(0)).exists())) {
            LoadTleDirectDialog dlg = new LoadTleDirectDialog(parentComponent, true);
            dlg.setVisible(true);
            if (dlg.isWasYesSelected()) {
                loadTLEfromWeb = true;
                dialog = new JProgressDialog(parentComponent, false);
                dialog.setVisible(true);
            } else {
                loadTLEfromWeb = true;
                userSelectedNoToDownload = true;
            }
        }
    }

    /**
     * Add all of the TLE data files to the satellite browser
     * @return
     */
    @Override
    public Boolean doInBackground() {
        boolean result = true;
        mainNodesHash = new Hashtable<String, DefaultMutableTreeNode>();
        secondaryNodesHash = new Hashtable<String, DefaultMutableTreeNode>();
        DefaultMutableTreeNode currentMainNode = topTreeNode;
        DefaultMutableTreeNode currentSecondaryNode;
        TLE currentTLE = null;
        satCount = 0;
        if (loadTLEfromWeb && userSelectedNoToDownload) {
            return new Boolean(result);
        }
        for (int i = 0; i < tleDownloader.fileNames.length; i++) {
            if (mainNodesHash.containsKey(tleDownloader.primCat[i])) {
                currentMainNode = mainNodesHash.get(tleDownloader.primCat[i]);
            } else {
                currentMainNode = new DefaultMutableTreeNode(tleDownloader.primCat[i]);
                mainNodesHash.put(tleDownloader.primCat[i], currentMainNode);
                topTreeNode.add(currentMainNode);
            }
            currentSecondaryNode = new DefaultMutableTreeNode(tleDownloader.secondCat[i]);
            currentMainNode.add(currentSecondaryNode);
            secondaryNodesHash.put(tleDownloader.secondCat[i], currentSecondaryNode);
            try {
                BufferedReader tleReader = null;
                if (!loadTLEfromWeb) {
                    File tleFile = new File(tleDownloader.getTleFilePath(i));
                    FileReader tleFileReader = new FileReader(tleFile);
                    tleReader = new BufferedReader(tleFileReader);
                } else {
                    if (!dialog.isVisible()) {
                        result = false;
                        return new Boolean(result);
                    }
                    URL url = new URL(tleDownloader.getTleWebPath(i));
                    URLConnection c = url.openConnection();
                    InputStreamReader isr = new InputStreamReader(c.getInputStream());
                    tleReader = new BufferedReader(isr);
                    publish(new ProgressStatus((int) Math.round((i * 100.0) / tleDownloader.fileNames.length), tleDownloader.fileNames[i]));
                }
                String nextLine = null;
                while ((nextLine = tleReader.readLine()) != null) {
                    currentTLE = new TLE(nextLine, tleReader.readLine(), tleReader.readLine());
                    tleHash.put(currentTLE.getSatName(), currentTLE);
                    currentSecondaryNode.add(new DefaultMutableTreeNode(currentTLE.getSatName()));
                    satCount++;
                }
                tleReader.close();
            } catch (Exception e) {
                System.out.println("ERROR IN TLE READING POSSIBLE FILE FORMAT OR MISSING TLE FILES:" + e.toString());
                result = false;
                return new Boolean(result);
            }
        }
        try {
            File userTLdir = new File(usrTLEpath);
            if (userTLdir.isDirectory()) {
                IOFileFilter tleFilter = new IOFileFilter("txt", "tle", "dat");
                File[] tleFiles = userTLdir.listFiles(tleFilter);
                for (File f : tleFiles) {
                    String fn = f.getName();
                    Boolean r = loadTLEDataFile(f, "Custom", fn.substring(0, fn.length() - 4), false);
                    if (!r) {
                        System.out.println("Error loading TLE file: " + f.getCanonicalPath());
                    }
                }
            } else {
                System.out.println("ERROR: User TLE folder path is not a directory.");
            }
        } catch (Exception e) {
            System.out.println("Error loading user supplied TLE data files:" + e.toString());
        }
        return new Boolean(result);
    }

    @Override
    protected void process(List<ProgressStatus> chunks) {
        if (dialog == null) {
            dialog = new JProgressDialog(parentComponent, false);
        }
        ProgressStatus ps = chunks.get(chunks.size() - 1);
        dialog.setProgress(ps.getPercentComplete());
        dialog.repaint();
        dialog.setStatusText("Downloading File: " + ps.getStatusText());
    }

    @Override
    protected void done() {
        if (loadTLEfromWeb) {
            dialog.setVisible(false);
        }
        tleOutputTextArea.setText("Number of Satellites in list: " + satCount);
        satTree.expandRow(0);
    }

    /**
     * loads a given file and adds the TLE's to the sat Browser and auto expands the new addition
     * @param tleFile file containing TLE's (3line format, (1) name, (2) TLE line 1 (3) line 2
     * @param primaryCategory Primary list category to display the satellites under
     * @param secondaryCategory secondary category to display the satellites under (can be null)
     * @return if loading of TLE file was successful
     */
    public Boolean loadTLEDataFile(File tleFile, String primaryCategory, String secondaryCategory) {
        return loadTLEDataFile(tleFile, primaryCategory, secondaryCategory, true);
    }

    /**
     * loads a given file and adds the TLE's to the sat Browser
     * @param tleFile file containing TLE's (3line format, (1) name, (2) TLE line 1 (3) line 2
     * @param primaryCategory Primary list category to display the satellites under
     * @param secondaryCategory secondary category to display the satellites under (can be null)
     * @param autoExpandSelectNotify auto expand and select the added TLE datafile, also notify user with a dialog box if error
     * @return if loading of TLE file was successful
     */
    public Boolean loadTLEDataFile(File tleFile, String primaryCategory, String secondaryCategory, boolean autoExpandSelectNotify) {
        boolean result = true;
        int newSatCount = 0;
        boolean customCategoriesInFile = false;
        TLE currentTLE = null;
        try {
            BufferedReader tleReader = null;
            FileReader tleFileReader = new FileReader(tleFile);
            tleReader = new BufferedReader(tleFileReader);
            String nextLine = tleReader.readLine();
            if (nextLine.startsWith("##main=")) {
                String[] data1 = nextLine.split("=");
                String pri = data1[1].split(",")[0].trim();
                String sec = data1[2].trim();
                if (pri != null && sec != null) {
                    primaryCategory = pri;
                    secondaryCategory = sec;
                    if (secondaryCategory.equalsIgnoreCase("NULL")) {
                        secondaryCategory = null;
                    }
                    customCategoriesInFile = true;
                }
            }
            tleReader.close();
        } catch (Exception e) {
        }
        DefaultMutableTreeNode currentMainNode = topTreeNode;
        DefaultMutableTreeNode currentSecondaryNode;
        if (mainNodesHash.containsKey(primaryCategory)) {
            currentMainNode = mainNodesHash.get(primaryCategory);
        } else {
            currentMainNode = new DefaultMutableTreeNode(primaryCategory);
            mainNodesHash.put(primaryCategory, currentMainNode);
            topTreeNode.add(currentMainNode);
        }
        if (secondaryCategory != null) {
            if (secondaryNodesHash.containsKey(secondaryCategory)) {
                currentSecondaryNode = secondaryNodesHash.get(secondaryCategory);
            } else {
                currentSecondaryNode = new DefaultMutableTreeNode(secondaryCategory);
                secondaryNodesHash.put(secondaryCategory, currentSecondaryNode);
                currentMainNode.add(currentSecondaryNode);
            }
        } else {
            currentSecondaryNode = currentMainNode;
        }
        try {
            BufferedReader tleReader = null;
            FileReader tleFileReader = new FileReader(tleFile);
            tleReader = new BufferedReader(tleFileReader);
            String nextLine = null;
            if (customCategoriesInFile) {
                nextLine = tleReader.readLine();
            }
            while ((nextLine = tleReader.readLine()) != null) {
                currentTLE = new TLE(nextLine, tleReader.readLine(), tleReader.readLine());
                tleHash.put(currentTLE.getSatName(), currentTLE);
                currentSecondaryNode.add(new DefaultMutableTreeNode(currentTLE.getSatName()));
                newSatCount++;
            }
            tleReader.close();
        } catch (Exception e) {
            System.out.println("ERROR IN TLE READING- bad format/missing file/permissions:" + e.toString());
            e.printStackTrace();
            if (autoExpandSelectNotify) {
                try {
                    JOptionPane.showMessageDialog(parentComponent, "Error Loading TLE Data File, bad permissions or file format: \n" + tleFile.getCanonicalPath().toString() + "\n" + e.toString(), "TLE LOADING ERROR", JOptionPane.ERROR_MESSAGE);
                } catch (Exception ee) {
                    JOptionPane.showMessageDialog(parentComponent, "Error Loading TLE Data File, bad permissions or file format: \n" + e.toString(), "TLE LOADING ERROR", JOptionPane.ERROR_MESSAGE);
                }
            }
            result = false;
            return new Boolean(result);
        }
        ((DefaultTreeModel) satTree.getModel()).reload();
        if (autoExpandSelectNotify) {
            satTree.expandPath(getTreePath(currentSecondaryNode));
            satTree.getSelectionModel().setSelectionPath(getTreePath(currentSecondaryNode));
        }
        tleOutputTextArea.setText("Number of New Satellites add to list: " + newSatCount);
        try {
            System.out.println("Custom TLE data loaded from file: (" + primaryCategory + "," + secondaryCategory + ")" + tleFile.getCanonicalPath().toString());
        } catch (Exception e) {
        }
        return new Boolean(result);
    }

    public TreePath getTreePath(TreeNode node) {
        List<TreeNode> list = new ArrayList<TreeNode>();
        while (node != null) {
            list.add(node);
            node = node.getParent();
        }
        Collections.reverse(list);
        return new TreePath(list.toArray());
    }
}
