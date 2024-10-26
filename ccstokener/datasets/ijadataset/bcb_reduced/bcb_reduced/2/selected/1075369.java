package edu.ucla.stat.SOCR.analyses.jri.gui;

import edu.ucla.stat.SOCR.analyses.example.ExampleData;
import edu.ucla.stat.SOCR.analyses.example.ExampleDataRandom;
import edu.ucla.stat.SOCR.analyses.gui.Chart;
import edu.ucla.stat.SOCR.util.*;
import edu.ucla.stat.SOCR.analyses.jri.*;
import java.awt.*;
import java.util.ArrayList;
import java.awt.event.*;
import java.text.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.event.*;
import javax.swing.JButton;
import javax.swing.JToolBar;
import java.net.URLConnection;
import java.net.URL;
import java.net.HttpURLConnection;
import edu.ucla.stat.SOCR.analyses.data.Data;
import edu.ucla.stat.SOCR.analyses.result.Result;
import edu.ucla.stat.SOCR.analyses.model.*;
import edu.ucla.stat.SOCR.core.Modeler;
import edu.ucla.stat.SOCR.core.ModelerGui;
import edu.ucla.stat.SOCR.servlet.util.*;
import javax.crypto.*;
import java.security.*;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.interfaces.*;
import com.sun.crypto.provider.SunJCE;

/**This class defines a basic type of Statistical Analysis procedure that can be
 * subclassed by the specific types of analyses
 * (e.g., ANOVA, Regression, prediction, etc.)*/
public class Analysis extends JApplet implements Runnable, MouseListener, ActionListener, MouseMotionListener, WindowListener {

    /****************** Start Variable Declaration **********************/
    private static String DEFAULT_USER_NAME = "SoCr";

    private static String DEFAULT_PASSWORD = "StatistiCS";

    protected final String EXAMPLE = " EXAMPLE ";

    protected final String CALCULATE = " CALCULATE ";

    protected final String CLEAR = " CLEAR ";

    protected final String LOAD_FILE = " LOAD FILE ";

    protected final String USER_INPUT = " USER INPUT ";

    protected final String USE_SERVER = CALCULATE;

    protected final String DATA = "DATA";

    protected final String RESULT = "RESULT";

    protected final String GRAPH = "GRAPH";

    protected final String MAPPING = "MAPPING";

    protected final String ADD = " ADD  ";

    protected final String REMOVE = "REMOVE";

    protected final String SHOW_ALL = "SHOW ALL";

    protected final String RANDOM_EXAMPLE = "RANDOM EXAMPLE";

    protected final String DEPENDENT = "DEPENDENT";

    protected final String INDEPENDENT = "INDEPENDENT";

    protected final String VARIABLE = "VARIABLE";

    protected final String DEFAULT_HEADER = "C";

    protected final String DATA_MISSING_MESSAGE = "DATA MISSING: Click on EXAMPLE for data first and click on MAPPING to continue.";

    protected final String VARIABLE_MISSING_MESSAGE = "VARIABLE MISSING: Map variables first by clicking on MAPPING.";

    protected final String DATA_COLINEAR_MESSAGE = "DATA CLOSE TO COLINEAR: Please remove colinearity before continue.";

    protected static final String outputFontFace = "Helvetica";

    protected static final int outputFontSize = 12;

    protected String onlineDescription = "http://mathworld.wolfram.com/";

    protected String onlineHelp = "http://en.wikipedia.org/wiki/Statistical_analysis";

    private static final int TEXT_AREA_ROW_SIZE = 100;

    private static final int TEXT_AREA_COLUMN_SIZE = 100;

    protected int independentLength = 0;

    protected int plotWidth = 300;

    protected int plotHeight = 300;

    protected int independentIndex = -1;

    protected int dependentIndex = -1;

    protected int timeIndex = -1;

    protected int censorIndex = -1;

    protected int groupNamesIndex = -1;

    protected String analysisName = "";

    /************** flags that controls the actions **************/
    protected boolean useStaticExample = true;

    protected boolean useRandomExample = true;

    protected boolean useLocalExample = true;

    protected boolean useInputExample = true;

    protected boolean useServerExample = true;

    protected boolean callServer = true;

    protected boolean useGraph = true;

    protected boolean useDataPanel = true;

    protected boolean useResultPanel = true;

    protected boolean useGraphPanel = true;

    protected boolean useMapPanel = true;

    protected boolean useSelectPanel = true;

    protected boolean useVisualizePanel = true;

    protected boolean usemixPanel = true;

    protected boolean isInitialInput = true;

    protected boolean hasExample = false;

    /************** ************** ************** **************/
    protected Action exampleStaticAction;

    protected Action exampleRandomAction;

    protected Action exampleLocalAction;

    protected Action exampleRemoteAction;

    protected Action exampleInputAction;

    protected Action callServerAction;

    protected Action computeAction;

    protected Action clearAction;

    protected Action userDataAction;

    protected Action fileLoadAction;

    protected short analysisType = -1;

    protected String analysisDescription = "";

    protected String[] independentHeaderArray = null;

    protected int exampleSampleSize = 0;

    private int time = 0, updateCount = 0, stopCount = 0, tabbedPaneCount = 0, toolbarCount = 0;

    public int selectedInd = 0;

    public static JTable dataTable;

    public Object[][] dataObject;

    protected String dataText = "";

    protected int columnNumber = 10;

    protected int rowNumber = 10;

    public String[] columnNames;

    public static javax.swing.table.DefaultTableModel tModel;

    protected static JPanel controlPanel, dataPanel, resultPanel, graphPanel, mappingInnerPanel, mappingPanel, selectPanel, visualizePanel, mixPanel;

    public JTextArea resultPanelTextArea;

    protected TableColumnModel columnModel;

    public JList listAdded, listDepRemoved, listIndepRemoved;

    public JList listTime, listCensor, listGroupNames;

    public JButton addButton1 = new JButton(ADD);

    public JButton addButton2 = new JButton("ADD 2");

    public JButton removeButton1 = new JButton(REMOVE);

    public JButton removeButton2 = new JButton(REMOVE);

    public JButton addButton3 = new JButton(ADD);

    public JButton removeButton3 = new JButton(REMOVE);

    JButton exampleButton = null;

    JButton calculateButton = null;

    JButton clearButton = null;

    JButton inputButton = null;

    JButton randomButton = null;

    JButton serverButton = null;

    DefaultListModel lModel1, lModel2, lModel3, lModel4;

    protected int depMax = 1;

    protected int indMax = 2;

    protected int currentDepIndex = -1;

    protected int currentIndepIndex = -1;

    protected ArrayList<Integer> independentList = null;

    protected int independentListCursor = 0;

    public int[] listIndex;

    JToolBar tools1, tools2, tools3;

    protected JScrollPane mixPanelContainer;

    protected Thread analysis = null;

    protected boolean stopNow = false;

    public JTabbedPane tabbedPanelContainer;

    protected Chart chartFactory;

    public static Font font = new Font("SansSerif", Font.PLAIN, 12);

    private JFrame frame;

    private DecimalFormat decimalFormat = new DecimalFormat();

    public JPanel leftAnalysisChoicePanel;

    protected JLabel depLabel = new JLabel(DEPENDENT);

    protected JLabel indLabel = new JLabel(INDEPENDENT);

    protected JLabel varLabel = new JLabel(VARIABLE);

    protected String inputXMLString = null;

    protected Data data = null;

    protected String xmlInputString = null;

    protected String xmlOutputString = null;

    protected static boolean randomDataStep = false;

    protected String hypothesisType = null;

    protected NormalCurve graphRawData = new NormalCurve(ModelerGui.GUI_LOWER_LIMIT, ModelerGui.GUI_UPPER_LIMIT, 1);

    protected NormalCurve graphSampleMean = new NormalCurve(ModelerGui.GUI_LOWER_LIMIT, ModelerGui.GUI_UPPER_LIMIT, 1);

    protected NormalCurve graphZScore = new NormalCurve(ModelerGui.GUI_LOWER_LIMIT, ModelerGui.GUI_UPPER_LIMIT, 1);

    protected JPanel leftPanel = new JPanel();

    protected JPanel rightPanel = new JPanel();

    protected static JButton test1 = new JButton();

    protected static JButton test2 = new JButton();

    /**This method initializes the Analysis, by setting up the basic tabbedPanes.*/
    public void init() {
        setFont(font);
        setName("SOCR: Statistical Analysis");
        frame = getFrame(this.getContentPane());
        setMainPanel();
        dataPanel = new JPanel();
        resultPanel = new JPanel();
        resultPanelTextArea = new JTextArea();
        graphPanel = new JPanel();
        mappingPanel = new JPanel(new BorderLayout());
        mappingInnerPanel = new JPanel(new GridLayout(2, 3, 50, 50));
        mixPanelContainer = new JScrollPane(mixPanel);
        mixPanelContainer.setPreferredSize(new Dimension(400, 400));
        mixPanel = new JPanel();
        dataPanel.setPreferredSize(new Dimension(400, 400));
        graphPanel.setPreferredSize(new Dimension(400, 400));
        resultPanel.setPreferredSize(new Dimension(400, 400));
        tools1 = new JToolBar(JToolBar.VERTICAL);
        tools2 = new JToolBar(JToolBar.VERTICAL);
        tools3 = new JToolBar(JToolBar.VERTICAL);
        addTabbedPane(DATA, dataPanel);
        addTabbedPane(GRAPH, graphPanel);
        addTabbedPane(MAPPING, mappingPanel);
        addTabbedPane(RESULT, resultPanel);
        setDataPanel();
        setGraphPanel();
        setMappingPanel();
        setResultPanel();
        this.getContentPane().add(new JScrollPane(tabbedPanelContainer), BorderLayout.CENTER);
        tabbedPanelContainer.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
            }
        });
    }

    protected void setDataPanel() {
        dataObject = new Object[rowNumber][columnNumber];
        columnNames = new String[columnNumber];
        independentList = new ArrayList<Integer>();
        for (int i = 0; i < columnNumber; i++) {
            columnNames[i] = new String(DEFAULT_HEADER + (i + 1));
        }
        tModel = new javax.swing.table.DefaultTableModel(dataObject, columnNames);
        dataTable = new JTable(tModel);
        dataTable.doLayout();
        dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        try {
            dataTable.setDragEnabled(true);
        } catch (Exception e) {
        }
        columnModel = dataTable.getColumnModel();
        dataTable.setTableHeader(new EditableHeader(columnModel));
        dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        dataTable.setCellSelectionEnabled(true);
        dataTable.setColumnSelectionAllowed(true);
        dataTable.setRowSelectionAllowed(true);
        dataPanel.add(new JScrollPane(dataTable));
    }

    protected void setResultPanel() {
        JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        resultPanelTextArea.setLineWrap(true);
        resultPanelTextArea.setRows(60);
        resultPanelTextArea.setColumns(60);
        resultPanel.setBackground(Color.WHITE);
        resultPanelTextArea.setEditable(true);
        resultPanelTextArea.setBackground(Color.WHITE);
        resultPanelTextArea.setForeground(Color.BLACK);
        resultPanel.add(new JScrollPane(resultPanelTextArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS));
        resultPanel.setPreferredSize(new Dimension(400, 400));
    }

    protected void setGraphPanel() {
        graphPanel.setLayout(new BorderLayout());
    }

    protected void setVisualizePanel() {
        visualizePanel.removeAll();
    }

    protected void setSelectPanel() {
    }

    protected void setShowAllPanel() {
        mixPanel.setBackground(Color.PINK);
        leftPanel.add(test1);
        rightPanel.add(test2);
        mixPanel.add(leftPanel, BorderLayout.WEST);
        mixPanel.add(rightPanel, BorderLayout.EAST);
    }

    /** Sets the amin Stat Analysis GUI with left-analysis-choice & right-data-cointrol
     * Jpanel's.*/
    public void setMainPanel() {
        tabbedPanelContainer = new JTabbedPane();
    }

    /**This method returns basic copyright, author, and other metadata information*/
    public String getAppletInfo() {
        return "\nUCLA Department of Statistics: SOCR Resource\n" + "http://www.stat.ucla.edu\n";
    }

    public String format(double x, String precision) {
        setDecimalFormat(new DecimalFormat(precision));
        return decimalFormat.format(x);
    }

    /**This method sets the decimal format, so that the properties of the decimal
     * format can then be changed*/
    public void setDecimalFormat(DecimalFormat d) {
        decimalFormat = d;
    }

    /**This class method returns the frame that contains a given component*/
    static JFrame getFrame(Container component) {
        JFrame frame = null;
        while ((component = component.getParent()) != null) {
            if (component instanceof JFrame) frame = (JFrame) component;
        }
        return frame;
    }

    /**This method add a new component to the tabbed panel.*/
    public void addTabbedPane(String name, JComponent c) {
        tabbedPanelContainer.addTab(name, c);
        tabbedPaneCount++;
    }

    /**This method add a new component to the tabbed panel.*/
    public void addTabbedPane(String title, Icon icon, JComponent component, String tip) {
        tabbedPanelContainer.addTab(title, icon, component, tip);
        tabbedPaneCount++;
    }

    /**This method removes a component from the tabbed panel.*/
    public void removeTabbedPane(int index) {
        tabbedPanelContainer.removeTabAt(index);
        tabbedPaneCount--;
    }

    /**This method sets a component in the tabbed panel to a specified new component.*/
    public void setTabbedPaneComponent(int index, JComponent c) {
        tabbedPanelContainer.setComponentAt(index, c);
    }

    /**This method gets the time parameter of the analysis process.
     * May have to be overwritten .*/
    public int getTime() {
        return time;
    }

    /**This method runs the analysis thread*/
    public void run() {
        Thread thread = Thread.currentThread();
        while (analysis == thread) {
            doAnalysis();
            stopCount++;
            updateCount++;
            if (stopNow) {
                stop();
                if (updateCount != 0) update();
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                stop();
            }
        }
    }

    /**This method stops the analysis thread*/
    public void stop() {
        analysis = null;
        stopCount = 0;
    }

    /**This method defines the boolean variable that stops the analysis,
     * when the process is in run mode*/
    public void setStopNow(boolean b) {
        stopNow = b;
    }

    /**This method is the default update method and defines how the display is updated.
     * This method should be overridden by the specific analysis.*/
    public void update() {
    }

    /**This method is the default reset method, that resets the analysis
     * process to its initial state. It should be overridden by the specific
     * analysis tools.*/
    public void reset() {
        hasExample = false;
        dependentIndex = -1;
        independentIndex = -1;
        independentHeaderArray = null;
        independentList.clear();
        resultPanelTextArea.setText("");
        if (dataTable.isEditing()) dataTable.getCellEditor().stopCellEditing();
        dataPanel.removeAll();
        dataPanel.add(new JScrollPane(dataTable));
        dataPanel.validate();
        for (int i = 0; i < dataTable.getRowCount(); i++) {
            for (int j = 0; j < dataTable.getColumnCount(); j++) {
                dataTable.setValueAt("", i, j);
            }
        }
        for (int i = 0; i < columnNumber; i++) {
            columnNames[i] = new String(DEFAULT_HEADER + (i + 1));
            columnModel.getColumn(i).setHeaderValue(columnNames[i]);
        }
        time = 0;
        resetGraph();
        resetVisualize();
        resetParameterSelect();
    }

    public void resetMappingList() {
        dependentIndex = -1;
        independentIndex = -1;
        independentList.clear();
        removeButtonDependent();
        removeButtonIndependentAll();
    }

    public void resetMappingListGUI() {
        dependentIndex = -1;
        independentIndex = -1;
        independentList.clear();
        removeButtonDependent();
        removeButtonIndependentAll();
    }

    /**This method defines what the analysis really does, and should be overridden
     * by the specific analysis tools.*/
    public void doAnalysis() {
    }

    /**This method is the default step method, that runs the analysis one time unit.
     * This method may be overridden by the specific analysis tools.*/
    public void step() {
        doAnalysis();
        update();
    }

    /**This method handles the action events associated with the action
     * buttons in the Analysis Control JTabbedPane (Panel). It needs to
     * overridden by the specific analysis tools.*/
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == addButton1) {
            addButtonDependent();
        } else if (event.getSource() == removeButton1) {
            removeButtonDependent();
        } else if (event.getSource() == addButton2) {
            addButtonIndependent();
        } else if (event.getSource() == removeButton2) {
            removeButtonIndependent();
        }
    }

    private void addButtonDependent() {
        int ct1 = -1;
        int sIdx = listAdded.getSelectedIndex();
        int idx2 = lModel2.getSize();
        dependentIndex = 0;
        if (sIdx > -1 && idx2 < depMax) {
            for (int i = 0; i < listIndex.length; i++) {
                if (listIndex[i] == 1) ct1++;
                if (ct1 == sIdx) {
                    dependentIndex = i;
                    break;
                }
            }
            listIndex[dependentIndex] = 2;
            paintTable(listIndex);
        }
    }

    protected void removeButtonDependent() {
        dependentIndex = -1;
        int ct1 = -1;
        int idx1 = 0;
        int sIdx = listDepRemoved.getSelectedIndex();
        if (sIdx > -1) {
            for (int i = 0; i < listIndex.length; i++) {
                if (listIndex[i] == 2) ct1++;
                if (ct1 == sIdx) {
                    idx1 = i;
                    break;
                }
            }
            listIndex[idx1] = 1;
            paintTable(listIndex);
        }
    }

    private void addButtonIndependent() {
        independentLength++;
        independentIndex = 0;
        int ct1 = -1;
        int sIdx = listAdded.getSelectedIndex();
        int idx3 = lModel3.getSize();
        if (sIdx > -1 && idx3 < indMax) {
            for (int i = 0; i < listIndex.length; i++) {
                if (listIndex[i] == 1) ct1++;
                if (ct1 == sIdx) {
                    independentIndex = i;
                    independentList.add(independentListCursor, new Integer(independentIndex));
                    break;
                }
            }
            listIndex[independentIndex] = 3;
            paintTable(listIndex);
        }
    }

    private void removeButtonIndependent() {
        if (independentLength > 0) independentLength--;
        int ct1 = -1;
        int idx1 = 0;
        int sIdx = listIndepRemoved.getSelectedIndex();
        if (sIdx > -1) {
            for (int i = 0; i < listIndex.length; i++) {
                if (listIndex[i] == 3) ct1++;
                if (ct1 == sIdx) {
                    idx1 = i;
                    break;
                }
            }
            listIndex[idx1] = 1;
            paintTable(listIndex);
        }
    }

    protected void removeButtonIndependentAll() {
        independentLength = 0;
        independentIndex = -1;
        int ct1 = -1;
        int idx1 = 0;
        int sIdx = 0;
        try {
            listIndepRemoved.getSelectedIndex();
            for (int i = 0; i < indMax; i++) {
                try {
                    removeButtonIndependent();
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
        paintTable(listIndex);
    }

    /**This method returns an online description of this Statistical Analysis.
     * It should be overwritten by each specific analysis method.*/
    public String getOnlineDescription() {
        return new String("http://socr.stat.ucla.edu/");
    }

    public String getOnlineHelp() {
        return new String("http://en.wikipedia.org/wiki/Statistical_analysis");
    }

    /**Mouse events*/
    public void mouseClicked(MouseEvent event) {
    }

    public void mouseEntered(MouseEvent event) {
    }

    public void mouseExited(MouseEvent event) {
    }

    public void mousePressed(MouseEvent event) {
    }

    public void mouseReleased(MouseEvent event) {
    }

    /**Mouse motion events*/
    public void mouseMoved(MouseEvent event) {
    }

    public void mouseDragged(MouseEvent event) {
    }

    /**Window events*/
    public void windowOpened(WindowEvent event) {
    }

    public void windowClosing(WindowEvent event) {
    }

    public void windowClosed(WindowEvent event) {
    }

    public void windowIconified(WindowEvent event) {
    }

    public void windowDeiconified(WindowEvent event) {
    }

    public void windowActivated(WindowEvent event) {
    }

    public void windowDeactivated(WindowEvent event) {
    }

    protected void setMappingPanel() {
        listIndex = new int[dataTable.getColumnCount()];
        for (int j = 0; j < listIndex.length; j++) listIndex[j] = 1;
        mappingPanel.add(mappingInnerPanel, BorderLayout.CENTER);
        removeButton1.addActionListener(this);
        removeButton2.addActionListener(this);
        lModel1 = new DefaultListModel();
        lModel2 = new DefaultListModel();
        lModel3 = new DefaultListModel();
        int cellWidth = 10;
        listAdded = new JList(lModel1);
        listAdded.setSelectedIndex(0);
        listDepRemoved = new JList(lModel2);
        listIndepRemoved = new JList(lModel3);
        paintTable(listIndex);
        listAdded.setFixedCellWidth(cellWidth);
        listDepRemoved.setFixedCellWidth(cellWidth);
        listIndepRemoved.setFixedCellWidth(cellWidth);
        tools1.add(depLabel);
        tools2.add(indLabel);
        tools1.add(addButton1);
        tools1.add(removeButton1);
        tools2.add(addButton2);
        tools2.add(removeButton2);
        JPanel emptyPanel = new JPanel();
        mappingInnerPanel.add(new JScrollPane(listAdded));
        mappingInnerPanel.add(tools1);
        mappingInnerPanel.add(new JScrollPane(listDepRemoved));
        mappingInnerPanel.add(emptyPanel);
        mappingInnerPanel.add(tools2);
        mappingInnerPanel.add(new JScrollPane(listIndepRemoved));
    }

    public void paintTable(int[] lstInd) {
        lModel1.clear();
        lModel2.clear();
        lModel3.clear();
        for (int i = 0; i < lstInd.length; i++) {
            switch(lstInd[i]) {
                case 0:
                    break;
                case 1:
                    lModel1.addElement(columnModel.getColumn(i).getHeaderValue().toString().trim());
                    listAdded.setSelectedIndex(0);
                    break;
                case 2:
                    lModel2.addElement(columnModel.getColumn(i).getHeaderValue().toString().trim());
                    listDepRemoved.setSelectedIndex(0);
                    break;
                case 3:
                    lModel3.addElement(columnModel.getColumn(i).getHeaderValue().toString().trim());
                    listIndepRemoved.setSelectedIndex(0);
                    break;
                default:
                    break;
            }
            String temp = columnModel.getColumn(i).getHeaderValue().toString().trim();
        }
        listAdded.setSelectedIndex(0);
    }

    public void appendTableRows(int n) {
        int cl = dataTable.getSelectedColumn();
        int ct = dataTable.getColumnCount();
        tModel = (javax.swing.table.DefaultTableModel) dataTable.getModel();
        for (int j = 0; j < n; j++) tModel.addRow(new java.util.Vector(ct));
        dataTable.setModel(tModel);
    }

    public int getDistinctElements(Matrix Cl) {
        int rowCt = Cl.rows;
        int count = 1;
        double clData = 0;
        double[] distinctElements = new double[rowCt];
        distinctElements[0] = Cl.element[0][0];
        for (int i = 1; i < rowCt; i++) {
            clData = Cl.element[i][0];
            int flag = 0;
            for (int j = 0; j < count; j++) {
                if (clData == distinctElements[j]) {
                    flag = 1;
                    break;
                }
            }
            if (flag == 0) {
                distinctElements[count] = Cl.element[i][0];
                count++;
            }
        }
        return count;
    }

    public void updateExample(ExampleData example) {
        try {
            removeButtonIndependentAll();
        } catch (Exception e) {
        }
        hasExample = true;
        JTable tempDataTable = example.getExample();
        if (tempDataTable.getRowCount() > dataTable.getRowCount()) appendTableRows(tempDataTable.getRowCount() - dataTable.getRowCount());
        for (int i = 0; i < tempDataTable.getColumnCount(); i++) {
            columnModel.getColumn(i).setHeaderValue(tempDataTable.getColumnName(i));
        }
        for (int i = 0; i < tempDataTable.getRowCount(); i++) for (int j = 0; j < tempDataTable.getColumnCount(); j++) {
            dataTable.setValueAt(tempDataTable.getValueAt(i, j), i, j);
        }
        dataPanel.removeAll();
        dataPanel.add(new JScrollPane(dataTable));
        dataPanel.validate();
        tModel = new javax.swing.table.DefaultTableModel(dataObject, columnNames);
        try {
            dataTable.setDragEnabled(true);
        } catch (Exception e) {
        }
        columnModel = dataTable.getColumnModel();
        dataTable.setTableHeader(new EditableHeader(columnModel));
        paintTable(listIndex);
    }

    public int chkDataIntegrity() {
        int error = 0;
        if (dataTable.isEditing()) dataTable.getCellEditor().stopCellEditing();
        OKDialog OKD;
        String d = dataText;
        int dep = -1, fac1 = -1, fac2 = -1, flg = 0;
        for (int p = 0; p < listIndex.length; p++) {
            if (listIndex[p] == 2) dep = p;
            if (listIndex[p] == 3 && flg == 0) {
                fac1 = p;
                flg = 1;
            }
            if (listIndex[p] + fac1 >= 3 && listIndex[p] == 3) fac2 = p;
        }
        if (dep == -1 || fac1 == -1) {
            OKD = new OKDialog(null, true, "Map Fields First");
            OKD.setVisible(true);
            return error;
        }
        int i, j, k;
        final double zero = 0.00001;
        String newln = System.getProperty("line.separator");
        int dependantCount = 0;
        int factorsCount = 1;
        if (fac2 > -1) factorsCount = 2;
        for (int n = 0; n < dataTable.getRowCount(); n++) {
            if (dataTable.getValueAt(n, dep) == null || dataTable.getValueAt(n, dep).toString().trim().equals("")) {
                break;
            }
            dependantCount++;
        }
        if (dependantCount == 0) {
            OKD = new OKDialog(null, true, "Dependant Column missing values");
            OKD.setVisible(true);
            return error;
        }
        int[] facs = new int[3];
        facs[0] = dep;
        facs[1] = fac1;
        facs[2] = fac2;
        int flag = 0;
        for (int n = 1; n <= factorsCount; n++) {
            for (int m = 0; m < dependantCount; m++) {
                if (dataTable.getValueAt(m, facs[n]) == null || dataTable.getValueAt(m, facs[n]).toString().trim().equals("")) {
                    flag = 1;
                    break;
                }
            }
        }
        if (flag == 1) {
            OKD = new OKDialog(null, true, "Factors missing values");
            OKD.setVisible(true);
            return error;
        }
        if (factorsCount == 0) {
            OKD = new OKDialog(null, true, "Factor column missing values");
            OKD.setVisible(true);
            return error;
        }
        return error;
    }

    public static Analysis getInstance(String classname) throws Exception {
        return (Analysis) Class.forName(classname).newInstance();
    }

    public Container getDisplayPane() {
        Container container1 = new Container();
        Container container2 = new Container();
        container1.setBackground(Color.black);
        JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setBackground(Color.BLUE);
        container1.add(scrollPane);
        JSplitPane container = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        container.setBackground(Color.BLUE);
        container.add(tabbedPanelContainer);
        return container;
    }

    /**
   * used for some sublcass to initialize befrore be used
   */
    public void initialize() {
    }

    protected void createActionComponents(JToolBar toolBar) {
        if (useStaticExample) {
            exampleStaticAction = new AbstractAction() {

                public void actionPerformed(ActionEvent e) {
                    ExampleData exampleStatic = new ExampleData(analysisType, 0);
                    updateExample(exampleStatic);
                    updateExample(exampleStatic);
                }
            };
            exampleButton = toolBar.add(exampleStaticAction);
            exampleButton.setText(EXAMPLE);
            exampleButton.setToolTipText(analysisDescription);
        }
        if (callServer) {
            callServerAction = new AbstractAction() {

                public void actionPerformed(ActionEvent e) {
                    resultPanelTextArea.append("\n************");
                    doAnalysis();
                    try {
                        Data data = new Data();
                        xmlOutputString = getAnalysisOutputFromServer(getXMLInputString());
                    } catch (Exception ex) {
                    }
                }
            };
            serverButton = toolBar.add(callServerAction);
            serverButton.setText(USE_SERVER);
            serverButton.setToolTipText("Call Server");
        }
        clearAction = new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                reset();
                reset();
                resultPanelTextArea.setText("");
                tModel = new javax.swing.table.DefaultTableModel(dataObject, columnNames);
                dataTable = new JTable(tModel);
                removeButtonIndependentAll();
                removeButtonDependent();
                ExampleData exampleNull = new ExampleData(0, 0);
                updateExample(exampleNull);
                updateExample(exampleNull);
                graphPanel.removeAll();
                randomDataStep = false;
                hypothesisType = null;
            }
        };
        clearButton = toolBar.add(clearAction);
        clearButton.setText(CLEAR);
        clearButton.setToolTipText("Clears All Windows");
        if (useRandomExample) {
            exampleRandomAction = new AbstractAction() {

                public void actionPerformed(ActionEvent e) {
                    reset();
                    ExampleData exampleRandom = new ExampleDataRandom(analysisType, 0, exampleSampleSize);
                    updateExample(exampleRandom);
                }
            };
            randomButton = toolBar.add(exampleRandomAction);
            randomButton.setText(RANDOM_EXAMPLE);
            randomButton.setToolTipText("This is a RANDOMLY GENERATED Example");
        }
        if (useInputExample) {
            exampleInputAction = new AbstractAction() {

                public void actionPerformed(ActionEvent e) {
                    if (isInitialInput) {
                        reset();
                    }
                }
            };
            inputButton = toolBar.add(exampleInputAction);
            inputButton.setText(USER_INPUT);
            inputButton.setToolTipText("Simple Regression Example");
        }
    }

    protected void doGraph() {
    }

    protected void resetGraph() {
    }

    protected void resetVisualize() {
    }

    protected void resetParameterSelect() {
    }

    protected ByteArrayOutputStream getAnalysisOutputStreamFromServer(ByteArrayOutputStream input) {
        return null;
    }

    protected String getAnalysisOutputFromServer(String input) {
        java.io.BufferedWriter bWriter = null;
        URLConnection connection = null;
        String inputXML = null;
        String encryptedInputXML = null;
        Object zippedInput = null;
        String resultString = "";
        bWriter = null;
        connection = null;
        String target = ServletConstant.ANALYSIS_SERVLET;
        Key someKey = CryptoUtil.generateKey();
        String encrptedResult = CryptoUtil.encrypt(getXMLInputString(), someKey);
        ClientObject clientObject = new ClientObject(someKey, getXMLInputString(), DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        String decryptedResult = null;
        try {
            URL url = new URL(target);
            connection = (HttpURLConnection) url.openConnection();
            ((HttpURLConnection) connection).setRequestMethod("POST");
            connection.setDoOutput(true);
            ObjectOutputStream outputToHost = new ObjectOutputStream(connection.getOutputStream());
            outputToHost.writeObject(clientObject);
            outputToHost.flush();
            outputToHost.close();
            java.io.BufferedReader bReader = null;
            bReader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream()));
            InputStream inputStreamFromServlet = connection.getInputStream();
            ObjectInputStream dataFromServlet = new ObjectInputStream(connection.getInputStream());
            ServerObject serverObject = (ServerObject) dataFromServlet.readObject();
            resultString = serverObject.getStringAttachment();
            ((HttpURLConnection) connection).disconnect();
        } catch (java.io.IOException ex) {
            resultString += ex.toString();
        } catch (Exception e) {
            resultString += e.toString();
        } finally {
            if (bWriter != null) {
                try {
                    bWriter.close();
                } catch (Exception ex) {
                    resultString += ex.toString();
                }
            }
            if (connection != null) {
                try {
                    ((HttpURLConnection) connection).disconnect();
                } catch (Exception ex) {
                    resultString += ex.toString();
                }
            }
            setXMLOutputString(resultString);
        }
        return resultString;
    }

    protected String getXMLInputString() {
        return this.xmlInputString;
    }

    protected void setXMLInputString(String input) {
        this.xmlInputString = input;
    }

    protected void setXMLOutputString(String input) {
        this.xmlOutputString = input;
    }

    protected String getXMLOutputString() {
        return this.xmlOutputString;
    }
}
