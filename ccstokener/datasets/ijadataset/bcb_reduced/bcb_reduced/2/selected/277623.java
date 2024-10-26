package net.sf.quickui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import net.sf.quickui.widgets.ButtonWidget;
import net.sf.quickui.widgets.CheckboxWidget;
import net.sf.quickui.widgets.FileWidget;
import net.sf.quickui.widgets.FolderWidget;
import net.sf.quickui.widgets.HiddenWidget;
import net.sf.quickui.widgets.LabelWidget;
import net.sf.quickui.widgets.ProgressWidget;
import net.sf.quickui.widgets.RadioWidget;
import net.sf.quickui.widgets.SpinboxWidget;
import net.sf.quickui.widgets.TextAreaWidget;
import net.sf.quickui.widgets.TextWidget;
import net.sf.quickui.widgets.Widget;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * QuickUI - a quick way to create a user interface.
 * @author Linus Ericson
 */
public class QuickUI implements ActionListener {

    private JFrame frame = null;

    private Box mainPanel = null;

    private EventHandler handler = null;

    private ButtonGroup radioGroup;

    private Box radioPanel;

    private Map widgets = new HashMap();

    /**
     * Creates a new QuickUI.
     * @param url the <code>URL</code> to the user interface configuration file (*.qui)
     * @param eventHandler an <code>EventHandler</code>
     * @throws QuickUIException
     * @throws XMLStreamException
     * @throws IOException
     */
    public QuickUI(URL url, EventHandler eventHandler) throws QuickUIException {
        handler = eventHandler;
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainPanel = new Box(BoxLayout.Y_AXIS);
        mainPanel.setBorder(new EmptyBorder(3, 3, 10, 3));
        MyHandler xmlHandler = new MyHandler();
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            Schema schema = schemaFactory.newSchema(this.getClass().getResource("quickui.xsd"));
            parserFactory.setSchema(schema);
            SAXParser parser = parserFactory.newSAXParser();
            parser.parse(url.openStream(), xmlHandler);
            if (xmlHandler.getErrorsExists()) {
                throw new QuickUIException("Validation error:\n" + xmlHandler.getMessages());
            }
        } catch (ParserConfigurationException e) {
            throw new QuickUIException(e);
        } catch (SAXException e) {
            throw new QuickUIException(e);
        } catch (IOException e) {
            throw new QuickUIException(e);
        }
        frame.add(mainPanel);
        frame.pack();
        frame.setSize(Math.max(500, frame.getSize().width), frame.getSize().height);
        int x = (frame.getGraphicsConfiguration().getBounds().width - frame.getWidth()) / 2;
        int y = (frame.getGraphicsConfiguration().getBounds().height - frame.getHeight()) / 2;
        frame.setLocation(x, y);
        frame.setVisible(true);
    }

    private class MyHandler extends DefaultHandler {

        private JPanel buttonGroup = null;

        private boolean errorsExists = false;

        private String messages = "";

        private String get(Attributes attrs, String name, String defaultValue) {
            String value = attrs.getValue("", name);
            if (value == null) {
                value = defaultValue;
            }
            return value;
        }

        private void closeButtonGroup() {
            mainPanel.add(buttonGroup);
            buttonGroup = null;
        }

        public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
            String id = get(attrs, "id", null);
            String text = get(attrs, "text", null);
            if (buttonGroup != null && !"button".equals(qName)) {
                closeButtonGroup();
            }
            if ("button".equals(qName)) {
                if (buttonGroup == null) {
                    buttonGroup = new JPanel();
                }
                createButton(id, text, buttonGroup);
            } else if ("radio".equals(qName)) {
                createRadio(id, text);
            } else if ("item".equals(qName)) {
                String value = get(attrs, "value", "off");
                createRadioItem(id, text, value);
            } else if ("file".equals(qName)) {
                String filter = get(attrs, "filter", null);
                String mode = get(attrs, "mode", "open");
                String button = get(attrs, "button", "Browse...");
                String dir = get(attrs, "dir", null);
                String value = get(attrs, "value", null);
                createFile(id, text, filter, mode, button, dir, value);
            } else if ("folder".equals(qName)) {
                String mode = get(attrs, "mode", "open");
                String button = get(attrs, "button", "Browse...");
                String dir = get(attrs, "dir", null);
                String value = get(attrs, "value", null);
                createFolder(id, text, mode, button, dir, value);
            } else if ("checkbox".equals(qName)) {
                String value = get(attrs, "value", "off");
                createCheckbox(id, text, value);
            } else if ("spinbox".equals(qName)) {
                String value = get(attrs, "value", "0");
                String min = get(attrs, "min", "0");
                String max = get(attrs, "max", "100");
                createSpinbox(id, text, value, min, max);
            } else if ("text".equals(qName)) {
                String value = get(attrs, "value", "");
                createText(id, text, value);
            } else if ("textarea".equals(qName)) {
                String rows = get(attrs, "rows", "5");
                createTextArea(id, text, rows);
            } else if ("hidden".equals(qName)) {
                String value = get(attrs, "value", "");
                createHidden(id, text, value);
            } else if ("progress".equals(qName)) {
                String value = get(attrs, "value", "");
                String max = get(attrs, "max", "100");
                createProgress(id, text, value, max);
            } else if ("label".equals(qName)) {
                String align = get(attrs, "align", "left");
                String size = get(attrs, "size", null);
                createLabel(id, text, align, size);
            } else if ("quickui".equals(qName)) {
                frame.setTitle(get(attrs, "text", "QuickUI"));
            } else {
                System.err.println("Unknown element: " + qName);
            }
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (buttonGroup != null && !"button".equals(qName)) {
                closeButtonGroup();
            }
        }

        public void warning(SAXParseException e) throws SAXException {
            messages = messages + "[" + e.getLineNumber() + "," + e.getColumnNumber() + "] " + e.getLocalizedMessage() + "\n";
            errorsExists = true;
        }

        public void error(SAXParseException e) throws SAXException {
            messages = messages + "[" + e.getLineNumber() + "," + e.getColumnNumber() + "] " + e.getLocalizedMessage() + "\n";
            errorsExists = true;
        }

        public void fatalError(SAXParseException e) throws SAXException {
            throw e;
        }

        public boolean getErrorsExists() {
            return errorsExists;
        }

        public String getMessages() {
            return messages;
        }
    }

    private void createSpinbox(String sID, String sText, String sValue, String sMin, String sMax) {
        JPanel bigPanel = new JPanel();
        bigPanel.setLayout(new GridLayout());
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(0, 5, 5, 5));
        panel.setLayout(new BorderLayout(1, 1));
        int val = Integer.parseInt(sValue);
        int min = Integer.parseInt(sMin);
        int max = Integer.parseInt(sMax);
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(val, min, max, 1));
        panel.add(spinner, BorderLayout.WEST);
        if (sText != null) {
            JPanel labelPanel = new JPanel();
            labelPanel.setLayout(new GridLayout(1, 1));
            labelPanel.setBorder(new EmptyBorder(10, 5, 0, 0));
            JLabel label = new JLabel(sText);
            label.setLabelFor(spinner);
            labelPanel.add(label);
            mainPanel.add(labelPanel);
        }
        bigPanel.add(panel);
        mainPanel.add(bigPanel);
        widgets.put(sID, new SpinboxWidget(sID, sText, spinner));
    }

    private void createHidden(String tID, String text, String value) {
        widgets.put(tID, new HiddenWidget(tID, text, value));
    }

    private void createProgress(String tID, String text, String value, String max) {
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(0, 5, 5, 5));
        panel.setLayout(new GridLayout());
        JProgressBar bar = new JProgressBar(0, Integer.parseInt(max));
        bar.setStringPainted(true);
        panel.add(bar);
        if (text != null) {
            JPanel labelPanel = new JPanel();
            labelPanel.setLayout(new GridLayout(1, 1));
            labelPanel.setBorder(new EmptyBorder(10, 5, 0, 0));
            JLabel label = new JLabel(text);
            label.setLabelFor(bar);
            labelPanel.add(label);
            mainPanel.add(labelPanel);
        }
        mainPanel.add(panel);
        widgets.put(tID, new ProgressWidget(tID, text, bar));
    }

    private void createLabel(String tID, String text, String align, String size) {
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(0, 5, 5, 5));
        panel.setLayout(new GridLayout());
        JLabel lab = new JLabel(text);
        if (size != null) {
            lab.setFont(lab.getFont().deriveFont((float) Integer.parseInt(size)));
        }
        if ("center".equals(align)) {
            lab.setHorizontalAlignment(JLabel.CENTER);
        } else if ("right".equals(align)) {
            lab.setHorizontalAlignment(JLabel.RIGHT);
        }
        panel.add(lab);
        mainPanel.add(panel);
        widgets.put(tID, new LabelWidget(tID, text, lab));
    }

    private void createTextArea(String tID, String tText, String tRows) {
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(0, 5, 5, 5));
        panel.setLayout(new BorderLayout());
        JTextArea textArea = new JTextArea();
        textArea.setRows(Integer.parseInt(tRows));
        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        if (tText != null) {
            JPanel labelPanel = new JPanel();
            labelPanel.setLayout(new GridLayout(1, 1));
            labelPanel.setBorder(new EmptyBorder(10, 5, 0, 0));
            JLabel label = new JLabel(tText);
            label.setLabelFor(textArea);
            labelPanel.add(label);
            mainPanel.add(labelPanel);
        }
        mainPanel.add(panel);
        widgets.put(tID, new TextAreaWidget(tID, tText, textArea));
    }

    private void createText(String tID, String tText, String tValue) {
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(0, 5, 5, 5));
        panel.setLayout(new GridLayout());
        JTextField textField = new JTextField(tValue);
        panel.add(textField);
        if (tText != null) {
            JPanel labelPanel = new JPanel();
            labelPanel.setLayout(new GridLayout(1, 1));
            labelPanel.setBorder(new EmptyBorder(10, 5, 0, 0));
            JLabel label = new JLabel(tText);
            label.setLabelFor(textField);
            labelPanel.add(label);
            mainPanel.add(labelPanel);
        }
        mainPanel.add(panel);
        widgets.put(tID, new TextWidget(tID, tText, textField));
    }

    private void createRadioItem(String iID, String iText, String iValue) {
        JRadioButton radioButton = new JRadioButton(iText);
        if ("on".equals(iValue)) {
            radioButton.setSelected(true);
        }
        radioButton.setActionCommand(iID);
        radioGroup.add(radioButton);
        radioPanel.add(radioButton);
    }

    private void createRadio(String rID, String rText) {
        JPanel bigPanel = new JPanel();
        bigPanel.setLayout(new GridLayout(1, 1));
        radioPanel = Box.createVerticalBox();
        radioPanel.setBorder(new EmptyBorder(0, 15, 0, 0));
        if (rText != null) {
            JPanel labelPanel = new JPanel();
            labelPanel.setLayout(new GridLayout(1, 1));
            labelPanel.setBorder(new EmptyBorder(10, 5, 0, 0));
            JLabel label = new JLabel(rText);
            label.setLabelFor(radioPanel);
            labelPanel.add(label);
            mainPanel.add(labelPanel);
        }
        radioGroup = new ButtonGroup();
        bigPanel.add(radioPanel);
        mainPanel.add(bigPanel);
        widgets.put(rID, new RadioWidget(rID, rText, radioGroup));
    }

    private void createCheckbox(String chkID, String chkText, String chkValue) {
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(0, 15, 0, 0));
        panel.setLayout(new GridLayout(1, 1));
        JCheckBox checkbox = new JCheckBox(chkText);
        checkbox.setSelected("on".equals(chkValue));
        checkbox.setLayout(new GridLayout(1, 1));
        panel.add(checkbox);
        mainPanel.add(panel);
        widgets.put(chkID, new CheckboxWidget(chkID, chkText, checkbox));
    }

    private void createButton(String btnID, String btnText, JPanel buttonPanel) {
        JButton button = new JButton(btnText);
        button.setActionCommand(btnID);
        button.addActionListener(this);
        buttonPanel.add(button);
        widgets.put(btnID, new ButtonWidget(btnID, btnText, button));
    }

    private void createFile(String fID, String fText, String fFilter, String fMode, String fButton, String fDir, String fValue) {
        JPanel bigPanel = new JPanel();
        bigPanel.setLayout(new BorderLayout());
        bigPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        JLabel head = new JLabel(fText);
        bigPanel.add(head, BorderLayout.NORTH);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JTextField field = new JTextField(fValue);
        field.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
        head.setLabelFor(field);
        panel.add(field, BorderLayout.CENTER);
        JButton button = new JButton(fButton);
        button.setActionCommand(fID);
        button.addActionListener(this);
        panel.add(button, BorderLayout.EAST);
        bigPanel.add(panel, BorderLayout.CENTER);
        JPanel xPanel = new JPanel();
        xPanel.setLayout(new GridLayout());
        xPanel.add(bigPanel);
        mainPanel.add(xPanel);
        widgets.put(fID, new FileWidget(fID, fText, fFilter, fMode, fDir, field, button));
    }

    private void createFolder(String fID, String fText, String fMode, String fButton, String fDir, String fValue) {
        JPanel bigPanel = new JPanel();
        bigPanel.setLayout(new BorderLayout());
        bigPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        JLabel head = new JLabel(fText);
        bigPanel.add(head, BorderLayout.NORTH);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JTextField field = new JTextField(fValue);
        field.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
        head.setLabelFor(field);
        panel.add(field, BorderLayout.CENTER);
        JButton button = new JButton(fButton);
        button.setActionCommand(fID);
        button.addActionListener(this);
        panel.add(button, BorderLayout.EAST);
        bigPanel.add(panel, BorderLayout.CENTER);
        JPanel xPanel = new JPanel();
        xPanel.setLayout(new GridLayout());
        xPanel.add(bigPanel);
        mainPanel.add(xPanel);
        widgets.put(fID, new FolderWidget(fID, fText, fMode, fDir, field, button));
    }

    public void actionPerformed(ActionEvent event) {
        Widget widget = (Widget) widgets.get(event.getActionCommand());
        if (widget.isButton()) {
            handler.buttonClick(new Event(event.getActionCommand()));
        } else if (widget.isFile()) {
            FileWidget fw = (FileWidget) widget;
            String startDir = new File(fw.getValue()).getParent();
            if ("open".equals(fw.getMode())) {
                JFileChooser jfc = new JFileChooser(startDir != null ? startDir : fw.getDir());
                if (fw.getFilter() != null) {
                    jfc.setFileFilter(new SimpleFilter(fw.getFilter()));
                }
                jfc.showOpenDialog(frame);
                if (jfc.getSelectedFile() != null) {
                    fw.setValue(jfc.getSelectedFile().toString());
                }
            } else if ("save".equals(fw.getMode())) {
                JFileChooser jfc = new JFileChooser(startDir != null ? startDir : fw.getDir());
                if (fw.getFilter() != null) {
                    jfc.setFileFilter(new SimpleFilter(fw.getFilter()));
                }
                jfc.showSaveDialog(frame);
                if (jfc.getSelectedFile() != null) {
                    fw.setValue(jfc.getSelectedFile().toString());
                }
            } else {
                throw new RuntimeException("Mode must be 'open' or 'save'!");
            }
        } else if (widget.isFolder()) {
            FolderWidget fw = (FolderWidget) widget;
            String startDir = new File(fw.getValue()).getParent();
            if ("open".equals(fw.getMode())) {
                JFileChooser jfc = new JFileChooser(startDir != null ? startDir : fw.getDir());
                jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                jfc.showOpenDialog(frame);
                if (jfc.getSelectedFile() != null) {
                    fw.setValue(jfc.getSelectedFile().toString());
                }
            } else if ("save".equals(fw.getMode())) {
                JFileChooser jfc = new JFileChooser(startDir != null ? startDir : fw.getDir());
                jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                jfc.showSaveDialog(frame);
                if (jfc.getSelectedFile() != null) {
                    fw.setValue(jfc.getSelectedFile().toString());
                }
            } else {
                throw new RuntimeException("Mode must be 'open' or 'save'!");
            }
        }
    }

    /**
     * Creates a message popup window.
     * @param message the message to show
     */
    public void popupMessage(String message) {
        JOptionPane.showMessageDialog(frame, message, frame.getTitle(), JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Creates a popup window with an OK and a Cancel button.
     * @param message the message or the question
     * @return true if the user clicked OK, false otherwise
     */
    public boolean popupOkCancel(String message) {
        return JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(frame, message, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
    }

    /**
     * Gets the value of a specified QuickUI widget.
     * @param id the ID of the widget
     * @return the value of the specified widget
     */
    public String getValue(String id) {
        Widget widget = (Widget) widgets.get(id);
        return widget.getValue();
    }

    /**
     * Gets the Widget with the specified id.
     * @param id the ID of the widget
     * @return the specified widget
     */
    public Widget getWidget(String id) {
        Widget widget = (Widget) widgets.get(id);
        return widget;
    }

    /**
     * Gets the entire set of widget IDs.
     * @return the set of widget IDs
     */
    public Set getIDs() {
        return widgets.keySet();
    }
}
