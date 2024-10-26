package cytoprophet;

import giny.view.NodeView;
import java.awt.Component;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.LinkedList;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.task.TaskMonitor;
import cytoscape.task.Task;
import cytoscape.util.CyFileFilter;
import cytoscape.view.CyNetworkView;
import cytoscape.task.ui.JTask;

/**
 * 	prophetTask.java <br> 
 *  LCLS at the University of Notre Dame <br><br>
 * 
 * This class implements the Task class. It is the main engine class of the Cytoprophet plugin.
 * It contacts the Tomcat server with an HTTP request, receives the results of the predictions,
 * parses the data files, and plots the predicted networks. 
 * 
 * @author Charles Lamanna
 * @author Faruck Morcos
 */
public class prophetTask implements Task {

    private TaskMonitor taskMonitor;

    public static String tmp_fpath = System.getProperty("java.io.tmpdir") + File.separatorChar;

    protected ButtonGroup alg_buttons, network_scope;

    protected JCheckBox ddi_cbox, go_cbox, self_interactions;

    private boolean interrupted = false;

    private Thread myThread = null;

    public static int runNum;

    int node_count;

    private Boolean empty_ddi = false;

    doItClick ddi;

    private int ppi_edges, ppi_nodes;

    private int ddi_edges, ddi_nodes;

    /**
	 * This constructor accepts the handles for the main features of the User Interface. These
	 * will be used to correctly follow the options specified by the user. 
	 * 
	 * @param alg_group represent which algorithm has been selected. 
	 * @param networkType describes whether or not to analyze all/selected network.
	 * @param ddi_checkbox toggles the domain-domain interaction network.
	 * @param go_checkbox toggles Go distances for the network.
	 * @param self_int toggles self interactions.
	 */
    public prophetTask(ButtonGroup alg_group, ButtonGroup networkType, JCheckBox ddi_checkbox, JCheckBox go_checkbox, JCheckBox self_int) {
        alg_buttons = alg_group;
        ddi_cbox = ddi_checkbox;
        go_cbox = go_checkbox;
        self_interactions = self_int;
        network_scope = networkType;
    }

    /**
	 * Returns a linked list containing the names of all the selected nodes in the network.
	 * 
	 * @return holds the names of all the selected nodes. 
	 */
    public LinkedList<String> getNetwork() {
        LinkedList<String> networkStrings;
        networkStrings = new LinkedList<String>();
        node_count = 0;
        CyNetworkView view = Cytoscape.getCurrentNetworkView();
        CyNetwork network = Cytoscape.getCurrentNetwork();
        if (network_scope.getSelection().getActionCommand().equals("whole")) {
            for (Iterator i = network.nodesIterator(); i.hasNext(); ) {
                CyNode node = (CyNode) i.next();
                networkStrings.add(node.toString().trim());
                ++node_count;
            }
        } else if (network_scope.getSelection().getActionCommand().equals("selected")) {
            for (Iterator i = view.getSelectedNodes().iterator(); i.hasNext(); ) {
                NodeView nView = (NodeView) i.next();
                CyNode node = (CyNode) nView.getNode();
                networkStrings.add(node.toString().trim());
                ++node_count;
            }
        }
        return networkStrings;
    }

    /**
	 * Applies the yLayout algorithm specified by the layout String.
	 * Programatically traverses the
	 * Cytoscape menu GUI and calls the "doClick" function for the yLayout menu item
	 * that has a name matching the algorithm specified in the constructor. 
	 * @param layout string representing which yLayout algorithm to use.
	 */
    public void doLayout(String layout) {
        JMenu layoutmenu = Cytoscape.getDesktop().getCyMenus().getLayoutMenu();
        for (int cv = 0; cv < layoutmenu.getMenuComponentCount(); ++cv) {
            try {
                Component temp = layoutmenu.getMenuComponent(cv);
                JMenu subLayoutMenu = (JMenu) (temp);
                if (subLayoutMenu.getText().equals("yFiles")) {
                    JMenu yfileMenu = subLayoutMenu;
                    for (int lcv = 0; lcv < yfileMenu.getMenuComponentCount(); ++lcv) {
                        try {
                            JMenuItem algItem = (JMenuItem) (yfileMenu.getMenuComponent(lcv));
                            if (algItem.getText().equals(layout)) {
                                ddi = new doItClick(algItem);
                                try {
                                    SwingUtilities.invokeLater(ddi);
                                } catch (Exception ex) {
                                }
                            }
                        } catch (Exception ex) {
                        }
                    }
                }
            } catch (Exception ex) {
            }
        }
    }

    /**
	 * The main function of Cytoprophet's execution. It sends the network to the server, following
	 * the specified parameters, and receives the raw data. It then calls xmlParser to parse the
	 * raw server data into the xgmml file format. It also extracts and loads the Cytoprophet
	 * Vizmap properties as required. It then generates the network and applies the visual appearances.
	 * 
	 * @see xmlParser
	 */
    public void sendToStream() {
        String go_fname;
        try {
            runNum = ((int) (Math.random() * 25555));
            sendNetworkToServer("", "").close();
            if (interrupted) return;
            taskMonitor.setStatus("Data retrieved from server.");
            if (go_cbox.isSelected()) go_fname = tmp_fpath + runNum + "server.go"; else go_fname = null;
            if (!(new File(tmp_fpath + runNum + "cytoprophet.props").exists())) {
                final CyFileFilter propsFilter = new CyFileFilter();
                propsFilter.addExtension("props");
                propsFilter.setDescription("Property files");
                try {
                    BufferedWriter out;
                    File fname;
                    fname = new File(tmp_fpath + runNum + "cytoprophet.props");
                    InputStream is = null;
                    BufferedReader br = null;
                    out = null;
                    String line;
                    try {
                        is = getClass().getResourceAsStream("/cyto.props");
                        br = new BufferedReader(new InputStreamReader(is));
                        out = new BufferedWriter(new FileWriter(tmp_fpath + runNum + "cytoprophet.props"));
                        while ((line = br.readLine()) != null) {
                            out.write(line + '\n');
                        }
                        out.write('\n');
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (br != null) br.close();
                            if (is != null) is.close();
                            if (out != null) out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception ex) {
                }
            }
            if (interrupted) return;
            if (ddi_cbox.isSelected()) {
                doDDI();
            }
            if (interrupted) return;
            doPPI(go_fname);
            taskMonitor.setStatus("Complete");
        } catch (FileNotFoundException fioex) {
            JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "File Not Found");
        } catch (IOException ioex) {
            JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "File opening error" + ioex.toString());
        } catch (Exception ex) {
            if (ex.getMessage().equals("Error in server")) {
                JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "<html>The Server is currently down." + "If you do not think this is a problem with your local network, please contact the" + " Cytoprophet support team.</html>", "Error: The Server is Down", JOptionPane.ERROR_MESSAGE);
            } else if (ex.getMessage().equals("Empty PPI")) {
                JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "<html>Data insufficient to make a prediction." + "<br><b>Make sure input data is valid and that a network window is selected.</b><br>" + "Not displaying network.</html>", "Warning: No PPI Interactions found", JOptionPane.ERROR_MESSAGE);
            } else JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Error in program execution: " + ex.getMessage());
        }
    }

    private Boolean parseDDI() {
        xmlParser ddiParsed;
        ddiParsed = new xmlParser(tmp_fpath + runNum + "serverddi.sif", tmp_fpath + runNum + "server_ddi.xgmml", null, alg_buttons.getSelection().getActionCommand() + ": DDI", self_interactions.isSelected(), alg_buttons.getSelection().getActionCommand());
        ddi_edges = ddiParsed.interactions.size();
        ddi_nodes = ddiParsed.interactors.size();
        if (ddiParsed.isEmpty()) return false;
        return true;
    }

    private Boolean parsePPI(String go_fname) {
        xmlParser ppiParsed;
        ppiParsed = new xmlParser(tmp_fpath + runNum + "server_raw.sif", tmp_fpath + runNum + "server_input.xgmml", go_fname, alg_buttons.getSelection().getActionCommand() + ": PPI ", self_interactions.isSelected(), alg_buttons.getSelection().getActionCommand());
        ppi_edges = ppiParsed.interactions.size();
        ppi_nodes = ppiParsed.interactors.size();
        if (ppiParsed.isEmpty()) return false;
        return true;
    }

    private void doPPI(String go_fname) throws Exception {
        if (!ddi_cbox.isSelected() || empty_ddi) SwingUtilities.invokeLater(new loadVizmap(new File(tmp_fpath + runNum + "cytoprophet.props")));
        if (parsePPI(go_fname)) {
            if (ddi_cbox.isSelected()) {
                try {
                    taskMonitor.setStatus("Building...");
                    if (ppi_edges + ppi_nodes > 1500) Thread.sleep(10000); else if (ppi_edges + ppi_nodes > 500) Thread.sleep(5000); else Thread.sleep(3000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            SwingUtilities.invokeLater(new makeNetwork(prophetTask.tmp_fpath + prophetTask.runNum + "server_input.xgmml"));
            if (ppi_nodes < 10000) {
                SwingUtilities.invokeLater(new makeNetworkView());
                if (ppi_edges < 25000) {
                    doLayout("Organic");
                }
                SwingUtilities.invokeLater(new performDDINetwork(node_count));
            }
        } else throw new Exception("Empty PPI");
    }

    private void doDDI() {
        if (!parseDDI()) {
            taskMonitor.setStatus("No DDI interactions found... will not generate a DDI network.");
            empty_ddi = true;
        } else {
            empty_ddi = false;
            SwingUtilities.invokeLater(new loadVizmap(new File(tmp_fpath + runNum + "cytoprophet.props")));
            SwingUtilities.invokeLater(new makeNetwork(prophetTask.tmp_fpath + prophetTask.runNum + "server_ddi.xgmml"));
            if (ddi_nodes < 10000) {
                SwingUtilities.invokeLater(new makeNetworkView());
                if (ddi_edges + ddi_nodes < 2500) doLayout("Organic");
                SwingUtilities.invokeLater(new performDDINetwork(node_count));
            }
        }
    }

    /**
	 * Initiates an HTTP connection with the Cytoprophet server, sends the network
	 * and the options for the prediction algorithms, receives the results, and parses 
	 * them. A helper function to sendToStream. 
	 * 
	 * @param raw_fname the filepath of the raw data received from the server.
	 * @param xgmml_fname the filepath where to save the parsed server data.
	 * @throws IOException generated by an incorrectly parsed file.
	 * @throws Exception generated by a a failed server connection
	 * @see xmlParser
	 */
    private BufferedReader sendNetworkToServer(String raw_fname, String xgmml_fname) throws IOException, Exception {
        String data = "proteins=";
        String data_prot = "";
        LinkedList<String> network = getNetwork();
        if (node_count > 1000) taskMonitor.setStatus("Your network is over 1000 nodes. " + "Predicting over such a large data set can take quite a bit of time. ");
        int cv = 0;
        while (cv < network.size()) {
            data_prot += network.get(cv) + '\n';
            ++cv;
        }
        try {
            data += URLEncoder.encode(data_prot, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            throw new Exception("Error with using URLEncoder");
        }
        if (alg_buttons != null) data += "&algorithm=" + alg_buttons.getSelection().getActionCommand(); else data += "&algorithm=mssc";
        try {
            System.out.println("Cytoprophet is now querying the server to predict interactions...");
            URL url = new URL("http://ppi.cse.nd.edu:8080/examples/servlets/servlet/Prediction");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            System.out.println("Connected to the Cytoprophet server...");
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            wr.close();
            if (node_count > 1000) taskMonitor.setStatus("Request sent to server... " + "Your network contains over 1000 nodes. " + "Predicting over such a large data set can take a long time.");
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            BufferedWriter out_raw, out_ppi, out_ddi, out_go, out_alias;
            out_raw = new BufferedWriter(new FileWriter(tmp_fpath + runNum + "server_raw.sif"));
            out_ppi = new BufferedWriter(new FileWriter(tmp_fpath + runNum + "server.ppi"));
            out_ddi = new BufferedWriter(new FileWriter(tmp_fpath + runNum + "serverddi.sif"));
            out_go = new BufferedWriter(new FileWriter(tmp_fpath + runNum + "server.go"));
            out_alias = new BufferedWriter(new FileWriter(tmp_fpath + runNum + "server.alias"));
            if (interrupted) return rd;
            System.out.println("Receiving server data...");
            while ((line = rd.readLine()) != null) {
                if (line.contains("<START PPI>")) break;
                out_raw.write(line + '\n');
            }
            out_raw.close();
            if (interrupted) return rd;
            while ((line = rd.readLine()) != null) {
                if (line.contains("<START DDI>")) break;
                out_ppi.write(line + '\n');
            }
            out_ppi.close();
            if (interrupted) return rd;
            while ((line = rd.readLine()) != null) {
                if (line.contains("<START GO>")) break;
                out_ddi.write(line + '\n');
            }
            out_ddi.close();
            if (interrupted) return rd;
            while ((line = rd.readLine()) != null) {
                if (line.contains("<START ALIAS>")) break;
                out_go.write(line + '\n');
            }
            out_go.close();
            if (interrupted) return rd;
            while ((line = rd.readLine()) != null) {
                out_alias.write(line + '\n');
            }
            out_alias.close();
            return rd;
        } catch (IOException ex) {
            throw new Exception("Error in server");
        }
    }

    /**
	 * Implements the Task run function. Calls the sendToStream function and handles
	 * the display options for the taskMonitor. 
	 */
    public void run() {
        myThread = Thread.currentThread();
        taskMonitor.setStatus("Running prediction...");
        taskMonitor.setPercentCompleted(-1);
        sendToStream();
        taskMonitor.setStatus("Prediction Complete....");
        taskMonitor.setPercentCompleted(100);
    }

    /**
	 * Not yet implemented.
	 */
    public void halt() {
        if (myThread != null) {
            myThread.interrupt();
            this.interrupted = true;
            ((JTask) taskMonitor).setDone();
        }
    }

    /**
	 * Sets this Task's TaskMonitor to taskMonitor. 
	 * 
	 * @param TaskMonitor taskMonitor the TaskMonitor to map this LoadVizmapTask
	 * @exception IllegalThreadStateException thrown if an invalid thread state is given. 
	 */
    public void setTaskMonitor(TaskMonitor monitor) throws IllegalThreadStateException {
        this.taskMonitor = monitor;
    }

    /**
	 * Returns the title of this prophetTask.
	 * 
	 * @return String the title of the LoadVizmapTask. 
	 */
    public String getTitle() {
        return "Cytoprophet: Running Predictions";
    }
}
