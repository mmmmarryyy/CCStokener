package de.ibis.permoto.gui.result.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import org.apache.log4j.Logger;
import de.ibis.permoto.gui.img.ImageLoader;
import de.ibis.permoto.gui.result.ResultWizard;
import de.ibis.permoto.gui.result.panes.TabbedPane;
import de.ibis.permoto.gui.result.util.GradientLabel;
import de.ibis.permoto.model.definitions.IBusinessCase;
import de.ibis.permoto.result.ResultCollection;
import de.ibis.permoto.result.ResultLoader;
import de.ibis.permoto.result.ResultNode;
import de.ibis.permoto.util.db.DBManager;

/**
 * ResultPanel
 * @author Christian Markl
 * @author Robert Ibisch
 */
public class LoaderPanel extends BasePanel {

    /** jlist for diagrams */
    private JList jlist;

    /** A panel for a list of diagrams */
    private JPanel listOfDiagramsPanel;

    /** A panel for description and example of diagrams */
    private JPanel descriptionAndExamplePanel;

    /**
	 * this splitPane includes list(on the left) and description-image (on the
	 * right)
	 */
    private JSplitPane splitPane;

    /** Logger for this class */
    private static final Logger logger = Logger.getLogger(LoaderPanel.class);

    /** serial version uid */
    private static final long serialVersionUID = 1L;

    private static final String packageName = "de.ibis.permoto.gui.result.panes.availablediagrams";

    /** a example image-label of the diagram */
    private JLabel diagramImage;

    /** a description textarea of the diagram */
    private JTextArea diagramDescription;

    private JLabel titleLabel;

    /** the image and description of this diagram will be shown at the beginning */
    private static String standardDiagram = "XYLineChart";

    private String selectedDiagram = "Table";

    Class<?> objectOfSelectedDiagram;

    private static LoaderPanel instance = null;

    /**
	 * Constructor
	 * @param fromSolver
	 */
    public LoaderPanel(JFrame parent, boolean fromSolver) {
        super(parent, "DB Loader");
        logger.debug("fromSolver = " + fromSolver);
    }

    public static LoaderPanel getInstance(JFrame parent, boolean fromSolver) {
        if (LoaderPanel.instance == null) {
            LoaderPanel.instance = new LoaderPanel(parent, fromSolver);
        }
        return LoaderPanel.instance;
    }

    /**
	 * listener for draw diagrams button
	 */
    private class DrawDiagramsListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            ResultWizard.fromSolver = false;
            drawDiagrams();
        }
    }

    /**
	 * listener for selected draw diagrams
	 */
    private class DiagramsSelectionListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) {
                selectedDiagram = (String) jlist.getSelectedValue();
                logger.debug("selected ..:" + selectedDiagram);
                updateSplitPane(selectedDiagram);
            }
        }
    }

    public void drawDiagrams() {
        ResultWizard resultWizard = this.parent;
        TreePath[] paths = wfTree.getSelectionPaths();
        boolean folderChecked = false;
        if (paths != null && thereIsATree) {
            int allSolutionID[] = new int[paths.length];
            int i = 0;
            for (TreePath path : paths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) (path.getLastPathComponent());
                ResultNode rn = (ResultNode) (node.getUserObject());
                if (rn.getSolutionID() == -1) {
                    folderChecked = true;
                } else {
                    DBManager db = new DBManager();
                    int solutionID = rn.getSolutionID();
                    String screnarioId = db.getScenarioIDOfSolutionID(solutionID);
                    IBusinessCase businessCase = db.getBusinessCaseOutOfDB(screnarioId);
                    resultWizard.solutionIDBusinessCase.put(solutionID, businessCase);
                    allSolutionID[i] = solutionID;
                    i++;
                }
            }
            resultWizard.setSolutionID(allSolutionID);
        }
        if (!ResultWizard.fromSolver) {
            if (null == paths) {
                JOptionPane.showMessageDialog(this, "You didn't select a Result", "No Result selected", JOptionPane.INFORMATION_MESSAGE);
                return;
            } else if (folderChecked) {
                JOptionPane.showMessageDialog(this, "You selected not a result but a folder!", "No Result selected", JOptionPane.INFORMATION_MESSAGE);
                return;
            } else if (jlist.getSelectedIndex() == -1) {
                JOptionPane.showMessageDialog(this, "You didn't select a Diagram", "No Diagram selected", JOptionPane.INFORMATION_MESSAGE);
                return;
            } else if (!thereIsATree) {
                JOptionPane.showMessageDialog(this, "You can't draw a Diagram", "No Tree available", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        }
        logger.debug("drawDiagrams() - open new result panel ...");
        try {
            String simpleName = "";
            DBManager db = DBManager.getInstance();
            Vector<Integer> allEcecutionIDs = new Vector<Integer>();
            int[] solutionID = resultWizard.getSolutionID();
            if (ResultWizard.fromSolver) {
                allEcecutionIDs = db.getExecutionIDOfSolutionID(solutionID[0]);
                allEcecutionIDs.trimToSize();
                Integer[] arrayOfAllExecutionIDs = new Integer[allEcecutionIDs.size()];
                allEcecutionIDs.toArray(arrayOfAllExecutionIDs);
                resultWizard.setScenarioIDs(arrayOfAllExecutionIDs);
                resultWizard.initResults();
                int lengthOfExecutionIDs = arrayOfAllExecutionIDs.length;
                logger.debug("Length of arrayOfAllExecutionIDs = " + lengthOfExecutionIDs);
                if (lengthOfExecutionIDs > 1) {
                    String[] possibilities = { "Table", "PieChart", "BarChart", "BarChart3D", "XYLineChart", "StackedBarChart" };
                    selectedDiagram = makeInputDialog(possibilities);
                } else if (lengthOfExecutionIDs == 1) {
                    String[] possibilities = { "Table", "PieChart" };
                    selectedDiagram = makeInputDialog(possibilities);
                }
                simpleName = selectedDiagram;
                updateSplitPane(selectedDiagram);
                logger.debug("simpleName: " + simpleName);
            } else {
                simpleName = objectOfSelectedDiagram.getSimpleName();
                Vector<Integer> temporaryVector = new Vector<Integer>();
                resultWizard.initViewMode();
                ResultWizard.results = new ResultCollection[solutionID.length];
                for (int k = 0; k < solutionID.length; k++) {
                    temporaryVector = db.getExecutionIDOfSolutionID(solutionID[k]);
                    Integer[] arrayExecutionID = new Integer[temporaryVector.size()];
                    temporaryVector.toArray(arrayExecutionID);
                    resultWizard.setBc(resultWizard.solutionIDBusinessCase.get(solutionID[k]));
                    ResultLoader resultLoader = new ResultLoader(resultWizard.getBc());
                    logger.debug("Get the results from DB of solutionID " + solutionID[k]);
                    ResultWizard.results[k] = resultLoader.getResultCollection(solutionID[k], arrayExecutionID);
                    for (int n = 0; n < temporaryVector.size(); n++) {
                        allEcecutionIDs.add(temporaryVector.get(n));
                    }
                    if (simpleName.equalsIgnoreCase("PieChart")) ;
                    k = solutionID.length;
                }
                allEcecutionIDs.trimToSize();
                Integer[] arrayOfAllExecutionIDs = new Integer[allEcecutionIDs.size()];
                allEcecutionIDs.toArray(arrayOfAllExecutionIDs);
                resultWizard.setScenarioIDs(arrayOfAllExecutionIDs);
            }
            TabbedPane pane = null;
            if (resultWizard.getViewMode() == 0) {
                pane = new de.ibis.permoto.gui.result.panes.EmptyPane(parent);
                resultWizard.addResultPanel(pane);
            } else {
                for (Constructor<?> ctor : objectOfSelectedDiagram.getDeclaredConstructors()) {
                    Class<?>[] parameters = ctor.getParameterTypes();
                    if (parameters.length == 2) if ((String.class == parameters[0]) && (ResultWizard.class == parameters[1])) {
                        try {
                            pane = (TabbedPane) ctor.newInstance(new Object[] { selectedDiagram, resultWizard });
                            resultWizard.addResultPanel(pane);
                        } catch (InstantiationException e) {
                            logger.warn("The constructor of selected diagram could not be invoked!");
                        } catch (IllegalAccessException e) {
                            logger.warn("The constructor of selected diagram could not be invoked!");
                        } catch (InvocationTargetException e) {
                            logger.warn("The constructor of selected diagram could not be invoked!");
                        }
                    }
                }
            }
            if (ResultWizard.fromSolver) {
                resultWizard.setSelectedResultPanel(pane);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    /**
	 * The user can choose the diagram, if the result-GUI is started from
	 * solver-GUI.
	 * @return String
	 */
    private String makeInputDialog(String[] possibilities) {
        String s = (String) JOptionPane.showInputDialog(this, "You can choose the diagram in which the results are drawn!\n", "Choose a diagram", JOptionPane.PLAIN_MESSAGE, null, possibilities, possibilities[0]);
        logger.debug("You've chosen " + s);
        if ((s != null) && (s.length() > 0)) {
            return s;
        }
        return "Table";
    }

    /**
	 * Make Action panel
	 */
    protected void makeActionPanel() {
        actionPanel = new JPanel();
        actionPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));
        actionPanel.setLayout(new BorderLayout());
        makeTitleLabelLocal();
        actionPanel.add(titleLabel, BorderLayout.NORTH);
        makeSplitPane("", "");
        actionPanel.add(splitPane, BorderLayout.CENTER);
        JButton drawDiagrams = new JButton("Draw result");
        this.listOfDiagramsPanel.add(drawDiagrams, BorderLayout.SOUTH);
        drawDiagrams.addActionListener(new DrawDiagramsListener());
    }

    /**
	 * Title of the result panel
	 */
    private void makeTitleLabelLocal() {
        String title = "You can choose a diagram from the list and see more information about it";
        titleLabel = new GradientLabel("<html><font size=4><b>" + title + "</b></color></html>");
        titleLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY), BorderFactory.createEmptyBorder(12, 20, 10, 20)));
        titleLabel.setIconTextGap(10);
    }

    /**
	 * this split pane is located on the right side so that it shows the
	 * description and the image of the selected diagram
	 * @param text
	 * @param image
	 */
    private void makeSplitPane(String text, String image) {
        splitPane = new JSplitPane();
        splitPane.setPreferredSize(new Dimension(725, 380));
        splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(makeListPanel());
        splitPane.setRightComponent(makeDescriptionAndExamplePanel(text, image));
        splitPane.setOneTouchExpandable(false);
        splitPane.setDividerLocation(150);
        updateSplitPane(standardDiagram);
    }

    /**
	 * make a list of available diagrams. this list panel will be added in the
	 * split pane.
	 */
    private JPanel makeListPanel() {
        JLabel diagramListLabel = new JLabel("List of diagrams:");
        diagramListLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        try {
            jlist = new JList(getDiagrams());
        } catch (ClassNotFoundException e) {
            jlist = new JList();
        }
        jlist.addListSelectionListener(new DiagramsSelectionListener());
        jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jlist.setLayoutOrientation(JList.VERTICAL);
        jlist.setVisibleRowCount(-1);
        jlist.setToolTipText("choose a diagram");
        jlist.setFixedCellWidth(1);
        JScrollPane jListScrollPane = new JScrollPane(jlist);
        jListScrollPane.setPreferredSize(new Dimension(120, 220));
        listOfDiagramsPanel = new JPanel();
        listOfDiagramsPanel.setLayout(new BorderLayout());
        listOfDiagramsPanel.add(diagramListLabel, BorderLayout.NORTH);
        listOfDiagramsPanel.add(jListScrollPane, BorderLayout.CENTER);
        listOfDiagramsPanel.setMinimumSize(new Dimension(150, 50));
        return listOfDiagramsPanel;
    }

    /**
	 * Get the available diagrams
	 * @return an array for JList
	 * @throws ClassNotFoundException
	 */
    private String[] getDiagrams() throws ClassNotFoundException {
        logger.debug("Available diagrams are found and listed!");
        ArrayList<String> classesAsString = new ArrayList<String>();
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                logger.debug("loader is null");
                throw new ClassNotFoundException();
            }
            String path = packageName.replace('.', '/');
            URL url = loader.getResource(path);
            if (url == null) {
                logger.debug("url is null");
                throw new ClassNotFoundException();
            }
            if (url.getProtocol().equals("jar")) {
                URLConnection con = url.openConnection();
                JarURLConnection jarCon = (JarURLConnection) con;
                JarFile jarFile = jarCon.getJarFile();
                JarEntry jarEntry = jarCon.getJarEntry();
                String rootEntryPath = (jarEntry != null ? jarEntry.getName() : "");
                rootEntryPath = rootEntryPath + "/";
                for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements(); ) {
                    JarEntry entry = entries.nextElement();
                    String entryPath = entry.getName();
                    if (entryPath.startsWith(rootEntryPath)) {
                        if (entryPath.endsWith(".class")) {
                            String temp = entryPath.substring(rootEntryPath.length());
                            temp = temp.replace(".class", "");
                            if (!temp.contains("$")) {
                                classesAsString.add(temp);
                            }
                        }
                    }
                }
            } else {
                String rootEntryPath = url.getFile();
                rootEntryPath = rootEntryPath.replace("%20", " ");
                File dir = new File(rootEntryPath);
                File[] dirContents = dir.listFiles();
                for (int i = 0; i < dirContents.length; i++) {
                    File content = dirContents[i];
                    if (content.getName().endsWith(".class")) {
                        if (!content.getAbsolutePath().contains("$")) {
                            String relativePath = content.getAbsolutePath().substring(rootEntryPath.length());
                            String tmpString = "";
                            for (int k = 0; k < relativePath.length(); k++) {
                                if (relativePath.charAt(k) != '/') {
                                    tmpString += relativePath.charAt(k);
                                }
                            }
                            relativePath = tmpString;
                            classesAsString.add(relativePath.substring(0, relativePath.length() - 6));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new ClassNotFoundException();
        }
        String[] diagrams = new String[classesAsString.size()];
        classesAsString.toArray(diagrams);
        return diagrams;
    }

    /**
	 * A label that shows the description when the diagram in the list is chosen
	 * @return a JPanel object that contains a JTextArea (description of
	 *         diagram) and a JLabel(image of diagram)
	 */
    private JPanel makeDescriptionAndExamplePanel(String text, String image) {
        descriptionAndExamplePanel = new JPanel();
        diagramDescription = new JTextArea();
        diagramDescription.setText(text);
        diagramDescription.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10), diagramDescription.getBorder()));
        diagramDescription.setEditable(false);
        diagramDescription.setLineWrap(true);
        diagramDescription.setWrapStyleWord(true);
        diagramImage = new JLabel();
        diagramImage.setIcon(ImageLoader.loadImage(image));
        diagramImage.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 50, 20, 10), diagramImage.getBorder()));
        descriptionAndExamplePanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.ipadx = 10;
        descriptionAndExamplePanel.add(diagramDescription, c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridheight = GridBagConstraints.REMAINDER;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        descriptionAndExamplePanel.add(diagramImage, c);
        descriptionAndExamplePanel.setSize(new Dimension(splitPane.getRightComponent().getSize()));
        descriptionAndExamplePanel.setMinimumSize(new Dimension(200, 300));
        return descriptionAndExamplePanel;
    }

    /**
	 * you can change the description and image of a diagram in the list. the
	 * field names in the diagram class must be: image and description.
	 * @param objectOfSelectedDiagram
	 */
    private void updateSplitPane(String diagramName) {
        if (diagramName == null) return;
        try {
            objectOfSelectedDiagram = Class.forName(packageName + "." + diagramName);
        } catch (ClassNotFoundException e1) {
            logger.info(e1);
            e1.printStackTrace();
            return;
        }
        Field fieldImage, fieldDescription;
        Object newImage = "", newDesciption = "";
        try {
            fieldImage = objectOfSelectedDiagram.getDeclaredField("image");
            logger.debug(fieldImage.getName() + " of " + diagramName + " is loaded");
            fieldDescription = objectOfSelectedDiagram.getDeclaredField("description");
            logger.debug(fieldDescription.getName() + " of " + diagramName + " is loaded");
            newImage = fieldImage.get(objectOfSelectedDiagram.getClass());
            newDesciption = fieldDescription.get(objectOfSelectedDiagram.getClass());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        diagramImage.setIcon(ImageLoader.loadImage(newImage.toString()));
        diagramDescription.setText(newDesciption.toString());
    }
}
