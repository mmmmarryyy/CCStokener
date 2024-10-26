package matrix.view;

import java.net.URISyntaxException;
import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MatrixVisualization extends JPanel implements ActionListener {

    public static String filelocation = "http://www.cs.nmsu.edu/~bchisham/cgi-bin/phylows/matrix/M450?format=csv";

    private static String cutoffString;

    private static JFrame frame;

    private JvTable table;

    private JvUndoableTableModel tableModel;

    private JScrollPane scrollPane;

    private JPanel buttonPanel;

    private JButton hide;

    private JButton extract;

    private JCheckBox rowCheck;

    private JCheckBox columnCheck;

    private JLabel label;

    private ButtonGroup buttonGroup;

    private JTextArea output;

    private void initComponents() {
        table = new JvTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        table.setPreferredScrollableViewportSize(new Dimension(dimension.height, dimension.width - 100));
        table.calcColumnWidths();
        table.getTableHeader().setReorderingAllowed(false);
        table.getSelectionModel().addListSelectionListener(new RowListener());
        table.getColumnModel().getSelectionModel().addListSelectionListener(new ColumnListener());
        scrollPane = new JScrollPane(table);
        scrollPane.setAlignmentX(LEFT_ALIGNMENT);
        add(scrollPane);
        label = new JLabel("Selection Options");
        label.setAlignmentX(LEFT_ALIGNMENT);
        add(label);
        rowCheck = addCheckBox("Row Selection");
        rowCheck.setSelected(true);
        columnCheck = addCheckBox("Column Selection");
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setAlignmentX(LEFT_ALIGNMENT);
        hide = addButton("Hide");
        extract = addButton("Extract");
        add(buttonPanel);
    }

    /**
     * Open the specified matrix file initially.
     * @param path
     */
    public MatrixVisualization(String path) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        try {
            tableModel = CSVReader(path);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading table. Exiting...");
            System.exit(1);
        }
        initComponents();
    }

    /**
     * Display the matrix at the specified url.
     * @param url
     */
    public MatrixVisualization(URL url) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        try {
            tableModel = CSVReader(url.openStream());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading table. Exiting...");
            System.exit(1);
        }
        initComponents();
    }

    /**
     * Display the default matrix.
     */
    public MatrixVisualization() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        try {
            tableModel = CSVReader(filelocation);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading table. Exiting...");
            System.exit(1);
        }
        initComponents();
    }

    /**
     * Display the matrix with selected row or column
     */
    public MatrixVisualization(JvUndoableTableModel tm) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        tableModel = tm;
        initComponents();
    }

    /**
     * Read a matrix from the specified input stream
     * @param is
     * @return
     * @throws IOException
     */
    private static JvUndoableTableModel CSVReader(InputStream is) throws IOException {
        JvUndoableTableModel tableModel = null;
        Vector<Vector<String>> data = new Vector();
        boolean flag = false;
        Scanner bufRdr = new Scanner(is);
        String line = null;
        int colCount = 0;
        while (bufRdr.hasNextLine()) {
            line = bufRdr.nextLine();
            Vector<String> vline = new Vector<String>();
            StringTokenizer st = new StringTokenizer(line, ",");
            String s;
            int index;
            Pattern separator = Pattern.compile(",");
            String[] cells = separator.split(line);
            if (colCount <= 1) {
                colCount = cells.length;
            }
            for (int i = 0; i < cells.length; ++i) {
                vline.add(cells[i]);
            }
            data.add(vline);
        }
        bufRdr.close();
        Vector<String> columnNames = new Vector<String>();
        columnNames.add("");
        for (long i = 1; i < colCount; i++) {
            columnNames.add("trait" + i);
        }
        tableModel = new JvUndoableTableModel(data, columnNames);
        return tableModel;
    }

    /**
     * Read a matrix from a file
     * @param file
     * @return
     * @throws IOException
     */
    private static JvUndoableTableModel CSVReader(File file) throws IOException {
        return CSVReader(new FileInputStream(file));
    }

    /**
     * Read a matrix from a file or url.
     * @param filepath
     * @return
     * @throws IOException
     */
    private static JvUndoableTableModel CSVReader(String filepath) throws IOException {
        try {
            URI url = new URI(filepath);
            return CSVReader(url.toURL().openStream());
        } catch (URISyntaxException ex) {
            File file = new File(filepath);
            return CSVReader(file);
        }
    }

    /**
     * Write the displayed matrix to the specified stream.
     * @param tableModel
     * @param file
     * @throws IOException
     */
    static void CSVWriter(JvUndoableTableModel tableModel, File file) throws IOException {
        Vector data = tableModel.getDataVector();
        BufferedWriter bufWrt = new BufferedWriter(new FileWriter(file));
        for (Iterator i = data.iterator(); i.hasNext(); ) {
            Vector<String> line = (Vector<String>) i.next();
            for (Iterator j = line.iterator(); j.hasNext(); ) {
                bufWrt.write(cutoffString + j.next() + ",");
            }
            bufWrt.write("\n");
        }
        bufWrt.close();
    }

    private JButton addButton(String text) {
        JButton button = new JButton(text);
        button.addActionListener(this);
        button.setActionCommand(text);
        buttonPanel.add(button);
        return button;
    }

    private JCheckBox addCheckBox(String text) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.addActionListener(this);
        checkBox.setAlignmentX(LEFT_ALIGNMENT);
        add(checkBox);
        return checkBox;
    }

    private JRadioButton addRadio(String text) {
        JRadioButton b = new JRadioButton(text);
        b.addActionListener(this);
        buttonGroup.add(b);
        add(b);
        return b;
    }

    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (command.equals("Row Selection") || command.equals("Column Selection")) {
            if (command.equals("Row Selection")) {
                table.setRowSelectionAllowed(rowCheck.isSelected());
                table.setColumnSelectionAllowed(!rowCheck.isSelected());
            } else if (command.equals("Column Selection")) {
                table.setColumnSelectionAllowed(columnCheck.isSelected());
                table.setRowSelectionAllowed(!columnCheck.isSelected());
            }
            table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            rowCheck.setSelected(table.getRowSelectionAllowed());
            columnCheck.setSelected(table.getColumnSelectionAllowed());
        } else {
            if (command.equals("Hide")) {
                int[] indices;
                if (rowCheck.isSelected()) {
                    indices = table.getSelectedRows();
                    for (int i = 0; i < indices.length; i++) tableModel.removeRow(table.getSelectedRow());
                } else if (columnCheck.isSelected()) {
                    indices = table.getSelectedColumns();
                    for (int i = 0; i < indices.length; i++) {
                        removeColumnAndData(indices[i] - i);
                    }
                    table.calcColumnWidths();
                }
            } else if (command.equals("Extract")) {
                Vector newdata = new Vector();
                Vector olddata = tableModel.getDataVector();
                int[] indices;
                if (rowCheck.isSelected()) {
                    indices = table.getSelectedRows();
                    for (int i = 0; i < indices.length; i++) {
                        newdata.add(olddata.elementAt(indices[i]));
                    }
                } else if (columnCheck.isSelected()) {
                    indices = table.getSelectedColumns();
                    for (int j = 0; j < olddata.size(); j++) {
                        Vector<String> vline = new Vector<String>();
                        vline.add(((Vector<String>) olddata.get(j)).get(0));
                        for (int i = 0; i < indices.length; i++) {
                            vline.add(((Vector<String>) olddata.get(j)).get(indices[i]));
                        }
                        newdata.add(vline);
                    }
                }
                Vector<String> columnNames = new Vector<String>();
                columnNames.add("");
                for (long i = 1; i < ((Vector) newdata.firstElement()).size(); i++) columnNames.add("" + i);
                JvUndoableTableModel tm = new JvUndoableTableModel(newdata, columnNames);
                createAndShowGUI(tm);
            }
        }
    }

    public void removeColumnAndData(int vColIndex) {
        TableColumn col = table.getColumnModel().getColumn(vColIndex);
        int columnModelIndex = col.getModelIndex();
        Vector data = tableModel.getDataVector();
        Vector colIds = tableModel.getColumnIdentifiers();
        table.removeColumn(col);
        colIds.removeElementAt(columnModelIndex);
        for (int r = 0; r < data.size(); r++) {
            Vector row = (Vector) data.get(r);
            row.removeElementAt(columnModelIndex);
        }
        tableModel.setDataVector(data, colIds);
        Enumeration<TableColumn> en = table.getColumnModel().getColumns();
        for (; en.hasMoreElements(); ) {
            TableColumn c = (TableColumn) en.nextElement();
            if (c.getModelIndex() >= columnModelIndex) {
                c.setModelIndex(c.getModelIndex() - 1);
            }
        }
        tableModel.fireTableStructureChanged();
    }

    private void outputSelection() {
        output.append(String.format("Lead: %d, %d. ", table.getSelectionModel().getLeadSelectionIndex(), table.getColumnModel().getSelectionModel().getLeadSelectionIndex()));
        output.append("Rows:");
        for (int c : table.getSelectedRows()) {
            output.append(String.format(" %d", c));
        }
        output.append(". Columns:");
        for (int c : table.getSelectedColumns()) {
            output.append(String.format(" %d", c));
        }
        output.append(".\n");
    }

    private class RowListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                return;
            }
        }
    }

    private class ColumnListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                return;
            }
        }
    }

    class Menu extends JMenuBar {

        Menu(JvTable table, JvUndoableTableModel tableModel) {
            JvUndoManager undoManager = new JvUndoManager();
            tableModel.addUndoableEditListener(undoManager);
            JFileChooser fc = new JFileChooser(new File("."));
            final String[] EXTENSIONS = { ".csv" };
            fc.addChoosableFileFilter(new MyFileFilter(EXTENSIONS));
            JMenu fileMenu = new JMenu("File");
            JMenuItem save = new JMenuItem("Save");
            save.addActionListener(new JvSaveAction(tableModel, fc));
            JMenuItem saveAs = new JMenuItem("Save As");
            saveAs.addActionListener(new JvSaveAsAction(tableModel, fc));
            JMenuItem exit = new JMenuItem("Exit");
            exit.addActionListener(new JvExitAction());
            fileMenu.add(save);
            fileMenu.add(saveAs);
            fileMenu.addSeparator();
            fileMenu.add(exit);
            JMenu editMenu = new JMenu("Edit");
            editMenu.add(undoManager.getUndoAction());
            editMenu.add(undoManager.getRedoAction());
            JMenu viewMenu = new JMenu("View");
            viewMenu.addSeparator();
            viewMenu.add(new JvZoomAction(table, undoManager, 200));
            viewMenu.add(new JvZoomAction(table, undoManager, 100));
            viewMenu.add(new JvZoomAction(table, undoManager, 75));
            viewMenu.add(new JvZoomAction(table, undoManager, 50));
            viewMenu.add(new JvZoomAction(table, undoManager, 25));
            viewMenu.add(new JvZoomAction(table, undoManager, 20));
            add(fileMenu);
            add(editMenu);
            add(viewMenu);
        }
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        frame = new JFrame("Matrix");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        MatrixVisualization newContentPane = new MatrixVisualization();
        newContentPane.setOpaque(true);
        Menu menuBar = newContentPane.new Menu(newContentPane.table, newContentPane.tableModel);
        frame.setContentPane(newContentPane);
        frame.setJMenuBar(menuBar);
        frame.setSize(1200, 700);
        frame.pack();
        frame.setVisible(true);
    }

    private static void createAndShowGUI(String path) {
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        frame = new JFrame("Matrix");
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        MatrixVisualization newContentPane = new MatrixVisualization(path);
        newContentPane.setOpaque(true);
        Menu menuBar = newContentPane.new Menu(newContentPane.table, newContentPane.tableModel);
        frame.setContentPane(newContentPane);
        frame.setJMenuBar(menuBar);
        frame.setSize(1200, 700);
        frame.pack();
        frame.setVisible(true);
    }

    private static void createAndShowGUI(JvUndoableTableModel tm) {
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        frame = new JFrame("Matrix");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        MatrixVisualization newContentPane = new MatrixVisualization(tm);
        newContentPane.setOpaque(true);
        Menu menuBar = newContentPane.new Menu(newContentPane.table, newContentPane.tableModel);
        frame.setContentPane(newContentPane);
        frame.setJMenuBar(menuBar);
        frame.setSize(1200, 700);
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    createAndShowGUI();
                }
            });
        } else {
            final String path = args[1];
            javax.swing.SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    createAndShowGUI(path);
                }
            });
        }
    }
}
