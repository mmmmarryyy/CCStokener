package com.pallas.unicore.client.plugins.povray;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.logging.Level;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import com.pallas.unicore.client.UserDefaults;
import com.pallas.unicore.client.controls.DoubleTextField;
import com.pallas.unicore.client.controls.GenericFileFilter;
import com.pallas.unicore.client.controls.IntTextField;
import com.pallas.unicore.client.controls.LayoutTools;
import com.pallas.unicore.client.editor.RemoteTextEditor;
import com.pallas.unicore.client.explorer.SelectorDialog;
import com.pallas.unicore.client.panels.FileExportPanel;
import com.pallas.unicore.client.panels.FileImportPanel;
import com.pallas.unicore.client.panels.JPAPanel;
import com.pallas.unicore.client.panels.OptionPanel;
import com.pallas.unicore.resourcemanager.ResourceManager;

/**
 * Panel displaying input text field as well as some parameter controls
 * 
 * @author Ralf Ratering
 * @version $Id: PovrayJPAPanel.java,v 1.1 2004/05/25 14:58:51 rmenday Exp $
 */
public class PovrayJPAPanel extends JPAPanel {

    private DoubleTextField aaField;

    private PovrayContainer container;

    private FileExportPanel exportPanel;

    private FileImportPanel importPanel;

    private JTextField nameTextField;

    private OptionPanel optionsPanel;

    private JTextField outputDirField;

    private PovrayDefaults povrayDefaults;

    private RemoteTextEditor textEditor = new RemoteTextEditor();

    private IntTextField widthField, heightField, initialFrameField, finalFrameField, subsetStartFrameField, subsetEndFrameField;

    private DoubleTextField initialClockField, finalClockField;

    private JCheckBox exportCheckBox, animateCheckBox;

    private JLabel directoryLabel, initialFrameLabel, finalFrameLabel, subsetStartFrameLabel, subsetEndFrameLabel, initialClockLabel, finalClockLabel;

    private JButton browseButton;

    /**
	 * Constructor
	 * 
	 * @param parentFrame
	 *            reference to main JFrame for modal sub dialogs
	 * @param container
	 *            container that this panel represents
	 */
    public PovrayJPAPanel(JFrame parentFrame, PovrayContainer container) {
        super(parentFrame, container);
        this.container = container;
        buildComponents();
    }

    /**
	 * Apply values from GUI to container
	 */
    public void applyValues() {
        nameTextField.setText(nameTextField.getText().trim().replace(' ', '_'));
        updateModifiedTime();
        container.setName(nameTextField.getText());
        container.setSceneDescription(textEditor.getText());
        container.setWidth(widthField.getValue());
        container.setHeight(heightField.getValue());
        container.setAntiAliasing(aaField.getValue());
        container.setExported(exportCheckBox.isSelected());
        container.setOutputDirectory(outputDirField.getText());
        container.setAnimated(animateCheckBox.isSelected());
        container.setInitialFrame(initialFrameField.getValue());
        container.setFinalFrame(finalFrameField.getValue());
        container.setSubsetStartFrame(subsetStartFrameField.getValue());
        container.setSubsetEndFrame(subsetEndFrameField.getValue());
        container.setInitialClock(initialClockField.getValue());
        container.setFinalClock(finalClockField.getValue());
        importPanel.applyValues();
        exportPanel.applyValues();
        optionsPanel.applyValues();
    }

    private JPanel buildAboutPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        ClassLoader loader = getClass().getClassLoader();
        String aboutFilename = "com/pallas/unicore/client/plugins/povray/about.rtf";
        try {
            URL url = loader.getResource(aboutFilename);
            JEditorPane rtfPane = new JEditorPane();
            rtfPane.setContentType("text/rtf");
            rtfPane.setText(readURL(url));
            rtfPane.setEditable(false);
            panel.add(new JScrollPane(rtfPane), BorderLayout.NORTH);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not read " + aboutFilename, e);
        }
        String legalFilename = "com/pallas/unicore/client/plugins/povray/povlegal.rtf";
        try {
            URL url = loader.getResource(legalFilename);
            JEditorPane rtfPane = new JEditorPane();
            rtfPane.setContentType("text/rtf");
            rtfPane.setText(readURL(url));
            rtfPane.setEditable(false);
            panel.add(new JScrollPane(rtfPane), BorderLayout.CENTER);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not read " + legalFilename, e);
        }
        return panel;
    }

    /**
	 * Build GUI components
	 */
    private void buildComponents() {
        nameTextField = new JTextField();
        nameTextField.setToolTipText("Enter Task Name");
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.add(new JLabel("Name:"), LayoutTools.makegbc(0, 0, 1, 0, false));
        GridBagConstraints c = LayoutTools.makegbc(1, 0, 1, 0, true);
        c.gridwidth = GridBagConstraints.REMAINDER;
        topPanel.add(nameTextField, c);
        widthField = new IntTextField(0, 9999);
        topPanel.add(new JLabel("Width:"), LayoutTools.makegbc(1, 1, 1, 0, false));
        topPanel.add(widthField, LayoutTools.makegbc(2, 1, 1, 0, true));
        heightField = new IntTextField(0, 9999);
        topPanel.add(new JLabel("Height:"), LayoutTools.makegbc(3, 1, 1, 0, false));
        topPanel.add(heightField, LayoutTools.makegbc(4, 1, 1, 0, true));
        aaField = new DoubleTextField(0.0, 1.0);
        topPanel.add(new JLabel("Anti Aliasing:"), LayoutTools.makegbc(5, 1, 1, 0, false));
        topPanel.add(aaField, LayoutTools.makegbc(6, 1, 1, 0, true));
        CheckBoxListener checkBoxListener = new CheckBoxListener();
        exportCheckBox = new JCheckBox("Export Results");
        exportCheckBox.addItemListener(checkBoxListener);
        topPanel.add(exportCheckBox, LayoutTools.makegbc(0, 2, 1, 0, false));
        directoryLabel = new JLabel("Directory:");
        topPanel.add(directoryLabel, LayoutTools.makegbc(1, 2, 1, 0, false));
        outputDirField = new JTextField();
        topPanel.add(outputDirField, LayoutTools.makegbc(2, 2, 5, 0, true));
        browseButton = new JButton(new ChooseDirectoryAction());
        topPanel.add(browseButton, LayoutTools.makegbc(7, 2, 1, 0, false));
        animateCheckBox = new JCheckBox("Animate");
        animateCheckBox.addItemListener(checkBoxListener);
        topPanel.add(animateCheckBox, LayoutTools.makegbc(0, 3, 1, 0, false));
        initialFrameLabel = new JLabel("Initial Frame:");
        initialFrameField = new IntTextField(0, 9999);
        initialFrameField.setValue(0);
        topPanel.add(initialFrameLabel, LayoutTools.makegbc(1, 3, 1, 0, false));
        topPanel.add(initialFrameField, LayoutTools.makegbc(2, 3, 1, 0, true));
        finalFrameLabel = new JLabel("Final Frame:");
        finalFrameField = new IntTextField(0, 9999);
        finalFrameField.setValue(0);
        topPanel.add(finalFrameLabel, LayoutTools.makegbc(3, 3, 1, 0, false));
        topPanel.add(finalFrameField, LayoutTools.makegbc(4, 3, 1, 0, true));
        subsetStartFrameLabel = new JLabel("Subset Start Frame:");
        subsetStartFrameField = new IntTextField(0, 9999);
        subsetStartFrameField.setValue(0);
        topPanel.add(subsetStartFrameLabel, LayoutTools.makegbc(1, 4, 1, 0, false));
        topPanel.add(subsetStartFrameField, LayoutTools.makegbc(2, 4, 1, 0, true));
        subsetEndFrameLabel = new JLabel("Subset End Frame:");
        subsetEndFrameField = new IntTextField(0, 9999);
        subsetEndFrameField.setValue(0);
        topPanel.add(subsetEndFrameLabel, LayoutTools.makegbc(3, 4, 1, 0, false));
        topPanel.add(subsetEndFrameField, LayoutTools.makegbc(4, 4, 1, 0, true));
        initialClockLabel = new JLabel("Initial Clock:");
        initialClockField = new DoubleTextField(0, 9999);
        initialClockField.setValue(0);
        topPanel.add(initialClockLabel, LayoutTools.makegbc(1, 5, 1, 0, false));
        topPanel.add(initialClockField, LayoutTools.makegbc(2, 5, 1, 0, true));
        finalClockLabel = new JLabel("Final Clock:");
        finalClockField = new DoubleTextField(0, 9999);
        finalClockField.setValue(0);
        topPanel.add(finalClockLabel, LayoutTools.makegbc(3, 5, 1, 0, false));
        topPanel.add(finalClockField, LayoutTools.makegbc(4, 5, 1, 0, true));
        UserDefaults userDefaults = ResourceManager.getUserDefaults();
        importPanel = new FileImportPanel(container, userDefaults.getImportDirectory());
        exportPanel = new FileExportPanel(container, userDefaults.getExportDirectory());
        optionsPanel = new OptionPanel(container);
        GenericFileFilter filter1 = new GenericFileFilter(".pov", "POV-Ray Scene Files");
        GenericFileFilter filter2 = new GenericFileFilter(".ini", "POV-Ray Ini Files");
        textEditor.addFileFilter(filter1);
        textEditor.addFileFilter(filter2);
        textEditor.setFileFilter(filter1);
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add(textEditor, "POV-Ray Input File");
        tabbedPane.add(optionsPanel, "Options");
        tabbedPane.add(importPanel, "Imports");
        tabbedPane.add(exportPanel, "Exports");
        tabbedPane.add(buildAboutPanel(), "About POV-Ray");
        add(topPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        setBorder(BorderFactory.createEtchedBorder());
    }

    /**
	 * Reads the content from an URL and returns it as aString
	 * 
	 * @param url
	 *            URL to read from
	 * @return Content as String
	 * @exception IOException
	 *                Oops
	 */
    private String readURL(URL url) throws IOException {
        URLConnection uc = url.openConnection();
        InputStream content = (InputStream) uc.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(content));
        String line;
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        while ((line = in.readLine()) != null) {
            pw.println(line);
        }
        return sw.toString();
    }

    /**
	 * Fill values from container into gui
	 */
    public void resetValues() {
        if (container.getIdentifier() == null) {
            return;
        }
        importPanel.resetValues();
        exportPanel.resetValues();
        optionsPanel.resetValues();
        nameTextField.setText(container.getName());
        textEditor.setText(container.getSceneDescription());
        widthField.setValue(container.getWidth());
        heightField.setValue(container.getHeight());
        aaField.setValue(container.getAntiAliasing());
        exportCheckBox.setSelected(container.isExported());
        outputDirField.setText(container.getOutputDirectory());
        enableOutput(container.isExported());
        animateCheckBox.setSelected(container.isAnimated());
        initialFrameField.setValue(container.getInitialFrame());
        finalFrameField.setValue(container.getFinalFrame());
        subsetStartFrameField.setValue(container.getSubsetStartFrame());
        subsetEndFrameField.setValue(container.getSubsetEndFrame());
        initialClockField.setValue(container.getInitialClock());
        finalClockField.setValue(container.getFinalClock());
        enableAnimation(container.isAnimated());
    }

    /**
	 * @param povrayDefaults
	 *            The new defaults value
	 */
    public void setDefaults(PovrayDefaults povrayDefaults) {
        this.povrayDefaults = povrayDefaults;
        textEditor.setDefaultDir(new File(povrayDefaults.getInputDirectory()));
        widthField.setValue(povrayDefaults.getWidth());
        heightField.setValue(povrayDefaults.getHeight());
        aaField.setValue(povrayDefaults.getAntiAliasing());
        outputDirField.setText(povrayDefaults.getOutputDirectory());
    }

    /**
	 * compare gui entries to container values
	 */
    private void updateModifiedTime() {
        if (container.getName() == null || container.getSceneDescription() == null) {
            container.setModifiedTime(new Date());
            return;
        }
        if (!container.getName().equals(nameTextField.getText()) || !container.getSceneDescription().equals(textEditor.getText())) {
            container.setModifiedTime(new Date());
            return;
        }
    }

    /**
	 * Method will be called whenever this panel is activated
	 * 
	 * @param vsiteChanged
	 *            true, if vsite has changed since last update or reset
	 */
    public void updateValues(boolean vsiteChanged) {
        if (container.getVsite() != null) {
            if (vsiteChanged) {
                textEditor.setVsite(container.getVsite());
                importPanel.updateValues();
                exportPanel.updateValues();
            }
        }
        if (!textEditor.getFont().equals(povrayDefaults.getEditorFont()) || textEditor.getLineWrap() != povrayDefaults.getLineWrap()) {
            textEditor.setEditorFont(povrayDefaults.getEditorFont());
            textEditor.setLineWrap(povrayDefaults.getLineWrap());
            textEditor.repaint();
        }
    }

    private void enableAnimation(boolean enabled) {
        initialFrameLabel.setEnabled(enabled);
        initialFrameField.setEnabled(enabled);
        finalFrameLabel.setEnabled(enabled);
        finalFrameField.setEnabled(enabled);
        subsetStartFrameLabel.setEnabled(enabled);
        subsetStartFrameField.setEnabled(enabled);
        subsetEndFrameLabel.setEnabled(enabled);
        subsetEndFrameField.setEnabled(enabled);
        initialClockLabel.setEnabled(enabled);
        initialClockField.setEnabled(enabled);
        finalClockLabel.setEnabled(enabled);
        finalClockField.setEnabled(enabled);
    }

    private void enableOutput(boolean enabled) {
        directoryLabel.setEnabled(enabled);
        outputDirField.setEnabled(enabled);
        browseButton.setEnabled(enabled);
    }

    private class ChooseDirectoryAction extends AbstractAction {

        public ChooseDirectoryAction() {
            super("", ResourceManager.getIcon(ResourceManager.OPEN));
        }

        public void actionPerformed(ActionEvent ev) {
            SelectorDialog dialog = ResourceManager.getSelectorDialog(new File(outputDirField.getText()), null);
            dialog.setFileSelectionMode(SelectorDialog.DIRECTORIES_ONLY);
            int result = dialog.showDialog();
            if (result == SelectorDialog.APPROVE_OPTION) {
                outputDirField.setText(dialog.getSelectedFile().getAbsolutePath());
            }
        }
    }

    private class CheckBoxListener implements ItemListener {

        public void itemStateChanged(ItemEvent e) {
            if (e.getSource().equals(exportCheckBox)) {
                enableOutput(exportCheckBox.isSelected());
            } else if (e.getSource().equals(animateCheckBox)) {
                enableAnimation(animateCheckBox.isSelected());
            }
        }
    }
}
