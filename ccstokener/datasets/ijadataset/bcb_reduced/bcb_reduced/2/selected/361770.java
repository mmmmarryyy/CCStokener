package net.sourceforge.ondex.dialog.inputs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.MaskFormatter;
import net.sourceforge.ondex.dialog.ErrorDialog;
import net.sourceforge.ondex.taverna.TavernaException;
import net.sourceforge.ondex.taverna.wrapper.TavernaInput;

/**
 * Row gui for imports of depth 1 (simple lists)
 * 
 * @author Christian
 */
class ListRow extends AbstractRow implements DocumentListener, ListSelectionListener, PropertyChangeListener {

    private String ADD = "+";

    private String REMOVE = "Remove Item";

    private String UP = "Move Up";

    private String DOWN = "Move Down";

    private String FILE_PATH = "File Path";

    private String FIND_FILE = "Find File";

    private String ENTER_URI = "Enter URI";

    private String USE_FILE = "useFileAction";

    private String USE_LIST = "useListAction";

    private String NO_URI_OR_FILE = "Please set a URI or file";

    public static String NEW_LINE = System.getProperty("line.separator");

    private DefaultListModel listModel;

    private JList list;

    private JButton removeButton;

    private JButton addButton;

    private JButton moveDownButton;

    private JButton moveUpButton;

    private JButton filePathButton;

    private JButton useFileButton;

    private JTextField textField;

    private JScrollPane listScroller;

    private JTextArea previewArea;

    private JScrollPane previewScroller;

    private JLabel uriLabel;

    private JLabel delimiterLabel;

    private JFormattedTextField delimiterField;

    private JButton findFileButton;

    private JButton uriButton;

    private JLabel countLabel;

    private JButton useListButton;

    private InputGui parentGui;

    private boolean ready;

    private boolean isFile;

    private String name;

    private File listFile;

    private String uri;

    ListRow(InputGui inputGui, String name) throws TavernaException {
        super();
        this.parentGui = inputGui;
        this.name = name;
        addList(0, 0, 1, 4);
        textField = addtextField(20, 1, 0, 1, 1, true);
        addButton = addButton("+", 2, 0, 1, 1, false);
        filePathButton = addButton(FILE_PATH, 3, 0, 1, 1, true);
        removeButton = addButton(REMOVE, 1, 1, 2, 1, false);
        moveUpButton = addButton(UP, 1, 2, 2, 1, false);
        moveDownButton = addButton(DOWN, 1, 3, 2, 1, false);
        addUseFileButton(3, 1, 1, 3);
        uriLabel = addLabel(NO_URI_OR_FILE, 0, 0, 3, 1);
        findFileButton = addButton(FIND_FILE, 0, 1, 1, 1, true);
        uriButton = addButton(ENTER_URI, 1, 1, 2, 1, true);
        delimiterLabel = addLabel("Delimiter (\"char\")", 0, 2, 2, 1);
        delimiterField = addDelimiterField(2, 2, 1, 1);
        countLabel = addLabel(NO_URI_OR_FILE, 0, 3, 3, 1);
        addPreview(3, 0, 1, 4);
        addUseListButton(4, 0, 1, 3);
        switchToFile(false);
    }

    private JButton addButton(String text, int column, int row, int gridWidth, int gridHeight, boolean enabled) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setActionCommand(text);
        button.addActionListener(this);
        button.setEnabled(enabled);
        GridBagConstraints gridBagConstraints = getConstraints(column, row);
        gridBagConstraints.gridheight = gridHeight;
        gridBagConstraints.gridwidth = gridWidth;
        add(button, gridBagConstraints);
        return button;
    }

    private JRadioButton addRadioButton(String text, int column, ButtonGroup group) {
        JRadioButton button = new JRadioButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setActionCommand(text);
        button.addActionListener(this);
        GridBagConstraints gridBagConstraints = getConstraints(column, 0);
        add(button, gridBagConstraints);
        group.add(button);
        return button;
    }

    private JLabel addLabel(String text, int column, int row, int gridWidth, int gridHeight) {
        JLabel label = new JLabel(text);
        GridBagConstraints gridBagConstraints = getConstraints(column, row);
        gridBagConstraints.gridheight = gridHeight;
        gridBagConstraints.gridwidth = gridWidth;
        add(label, gridBagConstraints);
        return label;
    }

    private void addList(int column, int row, int gridWidth, int gridHeight) {
        listModel = new DefaultListModel();
        list = new JList(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(-1);
        list.setLayoutOrientation(JList.VERTICAL);
        listScroller = new JScrollPane(list);
        list.addListSelectionListener(this);
        GridBagConstraints gridBagConstraints = getConstraints(column, row);
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.gridwidth = gridWidth;
        gridBagConstraints.gridheight = gridHeight;
        add(listScroller, gridBagConstraints);
    }

    private JTextField addtextField(int size, int column, int row, int gridWidth, int gridHeight, boolean stretch) {
        JTextField tf = new JTextField(size);
        tf.getDocument().addDocumentListener(this);
        GridBagConstraints gridBagConstraints = getConstraints(column, row);
        if (stretch) {
            gridBagConstraints.weightx = 1;
        }
        gridBagConstraints.gridwidth = gridWidth;
        gridBagConstraints.gridheight = gridHeight;
        add(tf, gridBagConstraints);
        return tf;
    }

    private void addUseFileButton(int column, int row, int gridWidth, int gridHeight) {
        useFileButton = new JButton();
        useFileButton.setLayout(new BorderLayout());
        JLabel label1 = new JLabel("Load List");
        JLabel label2 = new JLabel("From File");
        JLabel label3 = new JLabel("or URI");
        useFileButton.add(BorderLayout.NORTH, label1);
        useFileButton.add(BorderLayout.CENTER, label2);
        useFileButton.add(BorderLayout.SOUTH, label3);
        useFileButton.setActionCommand(USE_FILE);
        useFileButton.addActionListener(this);
        GridBagConstraints gridBagConstraints = getConstraints(column, row);
        gridBagConstraints.gridwidth = gridWidth;
        gridBagConstraints.gridheight = gridHeight;
        add(useFileButton, gridBagConstraints);
    }

    private void addUseListButton(int column, int row, int gridWidth, int gridHeight) {
        useListButton = new JButton();
        useListButton.setLayout(new BorderLayout());
        JLabel label1 = new JLabel("Manually");
        JLabel label2 = new JLabel("set the");
        JLabel label3 = new JLabel("Values");
        useListButton.add(BorderLayout.NORTH, label1);
        useListButton.add(BorderLayout.CENTER, label2);
        useListButton.add(BorderLayout.SOUTH, label3);
        useListButton.setActionCommand(USE_LIST);
        useListButton.addActionListener(this);
        GridBagConstraints gridBagConstraints = getConstraints(column, row);
        gridBagConstraints.gridwidth = gridWidth;
        gridBagConstraints.gridheight = gridHeight;
        add(useListButton, gridBagConstraints);
    }

    private JFormattedTextField addDelimiterField(int column, int row, int gridWidth, int gridHeight) throws TavernaException {
        MaskFormatter formatter;
        try {
            formatter = new MaskFormatter("\"*\"");
            JFormattedTextField temp = new JFormattedTextField(formatter);
            temp.addPropertyChangeListener(this);
            GridBagConstraints gridBagConstraints = getConstraints(column, row);
            gridBagConstraints.gridheight = gridHeight;
            gridBagConstraints.gridwidth = gridWidth;
            temp.setText(",");
            temp.commitEdit();
            add(temp, gridBagConstraints);
            return temp;
        } catch (ParseException ex) {
            throw new TavernaException("Unexpected exception creating JFormattedTextField", ex);
        }
    }

    private void addPreview(int column, int row, int gridWidth, int gridHeight) {
        previewArea = new JTextArea("No file or uri choosen");
        previewArea.setEditable(false);
        previewArea.setVisible(true);
        previewScroller = new JScrollPane(previewArea);
        GridBagConstraints gridBagConstraints = getConstraints(column, row);
        gridBagConstraints.gridheight = gridHeight;
        gridBagConstraints.gridwidth = gridWidth;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 1;
        add(previewScroller, gridBagConstraints);
    }

    private void switchToFile(boolean useFile) {
        isFile = useFile;
        listScroller.setVisible(!useFile);
        removeButton.setVisible(!useFile);
        addButton.setVisible(!useFile);
        moveDownButton.setVisible(!useFile);
        moveUpButton.setVisible(!useFile);
        filePathButton.setVisible(!useFile);
        useFileButton.setVisible(!useFile);
        textField.setVisible(!useFile);
        previewScroller.setVisible(useFile);
        delimiterLabel.setVisible(useFile);
        uriLabel.setVisible(useFile);
        delimiterField.setVisible(useFile);
        findFileButton.setVisible(useFile);
        uriButton.setVisible(useFile);
        countLabel.setVisible(useFile);
        useListButton.setVisible(useFile);
        list.setSelectedIndex(-1);
        checkTextField();
    }

    @Override
    boolean ready() {
        return ready;
    }

    @Override
    TavernaInput getInput() throws TavernaException, IOException {
        TavernaInput input = new TavernaInput(name, 1);
        if (isFile) {
            if (listFile != null) {
                input.setListFileInput(listFile, delimiterField.getText().charAt(1));
                System.out.println("%" + delimiterField.getText().charAt(1) + "%");
            } else if (uri != null) {
                input.setListURIInput(uri, delimiterField.getText().charAt(1));
            } else {
                throw new TavernaException("Illegal call to getInputs when neither listFile nor URI has been set.");
            }
        } else {
            if (listModel.size() <= 0) {
                throw new TavernaException("Illegal call to getInputs when listModel not set.");
            }
            Object[] fromModel = listModel.toArray();
            String[] values = new String[fromModel.length];
            for (int i = 0; i < fromModel.length; i++) {
                values[i] = fromModel[i].toString();
            }
            input.setStringsInput(values);
        }
        return input;
    }

    private void checkTextField() {
        if (!isFile) {
            if (textField.getText().isEmpty()) {
                addButton.setEnabled(false);
            } else {
                addButton.setEnabled(true);
            }
        }
    }

    private void addText(String text) {
        listModel.addElement(text);
        textField.setText("");
        if (!ready) {
            ready = true;
            parentGui.checkReady();
        }
    }

    private void findFilePath() {
        JFileChooser chooser = new JFileChooser();
        int returnVal = chooser.showOpenDialog(parentGui.getParent());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            addText(file.getAbsolutePath());
        }
    }

    private void checkInputStream(InputStream stream) throws IOException {
        InputStreamReader reader = new InputStreamReader(stream);
        BufferedReader buffer = new BufferedReader(reader);
        previewArea.setText("");
        int maxPreview = 10000;
        char[] cbuf = new char[maxPreview];
        int count = buffer.read(cbuf, 0, maxPreview);
        if (count == -1) {
            previewArea.append("File or Uri appears empty!");
            previewArea.append(NEW_LINE);
            countLabel.setText("File or Uri appears empty!");
        } else {
            char delimiter = this.delimiterField.getText().charAt(1);
            int tokenCount = 1;
            for (int i = 0; i < count - 1; i++) {
                if (cbuf[i] == delimiter) {
                    tokenCount++;
                }
            }
            previewArea.append(new String(cbuf));
            if (count >= maxPreview) {
                previewArea.append(NEW_LINE);
                previewArea.append("Preview cut after " + maxPreview + " characters");
                previewArea.append(NEW_LINE);
                countLabel.setText("At least " + tokenCount + " items");
            } else {
                countLabel.setText("Found " + tokenCount + " items");
            }
        }
        stream.close();
        ready = true;
    }

    private void checkInput() throws MalformedURLException, IOException {
        if (listFile != null) {
            uriLabel.setText("file" + listFile.getAbsolutePath());
            InputStream stream = new FileInputStream(listFile);
            checkInputStream(stream);
        } else if (uri != null) {
            uriLabel.setText(uri);
            URL url = new URL(uri);
            InputStream stream = url.openStream();
            checkInputStream(stream);
        } else {
            uriLabel.setText(NO_URI_OR_FILE);
            countLabel.setText(NO_URI_OR_FILE);
            ready = false;
        }
        parentGui.checkReady();
    }

    private void clearPervious() {
        ready = false;
        listFile = null;
        uri = null;
        previewArea.setText(NO_URI_OR_FILE);
    }

    private void findFile() throws MalformedURLException, IOException {
        clearPervious();
        JFileChooser chooser = new JFileChooser();
        int returnVal = chooser.showOpenDialog(parentGui.getParent());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            listFile = chooser.getSelectedFile();
            uri = "file:" + listFile.getAbsolutePath();
        }
        checkInput();
    }

    private void enterURI() throws MalformedURLException, IOException {
        clearPervious();
        uri = JOptionPane.showInputDialog(parentGui.getParent(), "Please enter a URI including Schema(http:)", "URI to List", JOptionPane.INFORMATION_MESSAGE);
        checkInput();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println(e);
        String command = e.getActionCommand();
        try {
            if (command.equals(ADD)) {
                addText(textField.getText());
            } else if (command.equals(UP)) {
                int position = list.getSelectedIndex();
                Object mover = listModel.get(position);
                Object replaced = listModel.set(position - 1, mover);
                listModel.set(position, replaced);
                list.setSelectedIndex(position - 1);
            } else if (command.equals(DOWN)) {
                int position = list.getSelectedIndex();
                Object mover = listModel.get(position);
                Object replaced = listModel.set(position + 1, mover);
                listModel.set(position, replaced);
                list.setSelectedIndex(position + 1);
            } else if (command.equals(REMOVE)) {
                listModel.remove(list.getSelectedIndex());
                checkSelection();
                if (listModel.size() == 0) {
                    ready = false;
                    parentGui.checkReady();
                }
            } else if (command.equals(FILE_PATH)) {
                findFilePath();
            } else if (command.equals(USE_FILE)) {
                switchToFile(true);
            } else if (command.equals(USE_LIST)) {
                switchToFile(false);
            } else if (command.equals(FIND_FILE)) {
                findFile();
            } else if (command.equals(ENTER_URI)) {
                enterURI();
            }
        } catch (Exception ex) {
            ErrorDialog.show(parentGui.getParent(), ex);
        }
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        checkTextField();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        checkTextField();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        checkTextField();
    }

    private void checkSelection() {
        if (list.getSelectedIndex() == -1) {
            removeButton.setEnabled(false);
            moveUpButton.setEnabled(false);
            moveDownButton.setEnabled(false);
        } else {
            removeButton.setEnabled(true);
            if (list.getSelectedIndex() > 0) {
                moveUpButton.setEnabled(true);
            } else {
                moveUpButton.setEnabled(false);
            }
            if (list.getSelectedIndex() < listModel.getSize() - 1) {
                moveDownButton.setEnabled(true);
            } else {
                moveDownButton.setEnabled(false);
            }
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting() == false) {
            checkSelection();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        Object source = evt.getSource();
        if (source == delimiterField) {
            try {
                checkInput();
            } catch (Exception ex) {
                ErrorDialog.show(parentGui.getParent(), ex);
            }
        }
    }
}
