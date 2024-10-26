package net.sourceforge.fluxion.runcible.swing.component.dialog;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Javadocs go here.
 *
 * @author Tony Burdett
 * @version 1.0
 * @date 15-May-2007
 */
public class ConfirmLocationDialog extends JDialog {

    public static final int DIALOG_ACTIVE = 0;

    public static final int OK = 1;

    public static final int CANCEL = 2;

    public static final int ABORT = -1;

    private Container content;

    private JOptionPane optionPane;

    private File chooserDir;

    private URL url;

    private String message;

    private JTextField textField;

    private static int exit_status = DIALOG_ACTIVE;

    public static ConfirmLocationDialog showDialog(Component parent, String message) {
        final ConfirmLocationDialog dialog;
        String title = "Configure Mapping";
        if (parent instanceof Frame || parent instanceof Dialog) {
            if (parent instanceof Frame) {
                dialog = new ConfirmLocationDialog((Frame) parent, message, title);
            } else {
                dialog = new ConfirmLocationDialog((Dialog) parent, message, title);
            }
        } else {
            Window window = SwingUtilities.windowForComponent(parent);
            if (window instanceof Frame) {
                dialog = new ConfirmLocationDialog((Frame) window, message, title);
            } else {
                dialog = new ConfirmLocationDialog((Dialog) window, message, title);
            }
        }
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
        while (!dialog.isDismissed()) {
        }
        return dialog;
    }

    public static ConfirmLocationDialog showDialog(Component parent, String message, File chooserDirectory) {
        final ConfirmLocationDialog dialog;
        String title = "Configure Mapping";
        if (parent instanceof Frame || parent instanceof Dialog) {
            if (parent instanceof Frame) {
                dialog = new ConfirmLocationDialog((Frame) parent, message, title, chooserDirectory);
            } else {
                dialog = new ConfirmLocationDialog((Dialog) parent, message, title, chooserDirectory);
            }
        } else {
            Window window = SwingUtilities.windowForComponent(parent);
            if (window instanceof Frame) {
                dialog = new ConfirmLocationDialog((Frame) window, message, title, chooserDirectory);
            } else {
                dialog = new ConfirmLocationDialog((Dialog) window, message, title, chooserDirectory);
            }
        }
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
        while (!dialog.isDismissed()) {
        }
        return dialog;
    }

    private ConfirmLocationDialog(Frame frame, String message, String title) {
        super(frame, title, true);
        this.message = message;
        assembleDialogContents();
        Container contentPane = this.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(content, BorderLayout.CENTER);
        pack();
        setResizable(false);
        setLocationRelativeTo(frame);
    }

    private ConfirmLocationDialog(Dialog dialog, String message, String title) {
        super(dialog, title, true);
        this.message = message;
        assembleDialogContents();
        Container contentPane = this.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(content, BorderLayout.CENTER);
        pack();
        setResizable(false);
        setLocationRelativeTo(dialog);
    }

    private ConfirmLocationDialog(Frame frame, String message, String title, File chooserDirectory) {
        this(frame, message, title);
        this.chooserDir = chooserDirectory;
    }

    private ConfirmLocationDialog(Dialog dialog, String message, String title, File chooserDirectory) {
        this(dialog, message, title);
        this.chooserDir = chooserDirectory;
    }

    private void assembleDialogContents() {
        optionPane = new JOptionPane();
        JPanel infoPanel = new JPanel();
        textField = new JTextField();
        textField.getDocument().addDocumentListener(new DocumentListener() {

            public void insertUpdate(DocumentEvent e) {
                onTextBoxSelection(textField.getText().toString());
            }

            public void removeUpdate(DocumentEvent e) {
                onTextBoxSelection(textField.getText().toString());
            }

            public void changedUpdate(DocumentEvent e) {
                onTextBoxSelection(textField.getText().toString());
            }
        });
        JButton ellipsis = new JButton("...");
        ellipsis.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Select ontology location");
                if (chooserDir != null) chooser.setCurrentDirectory(chooserDir);
                chooser.setFileFilter(new FileFilter() {

                    public boolean accept(File f) {
                        return (f.isDirectory() || f.getAbsolutePath().endsWith(".owl"));
                    }

                    public String getDescription() {
                        return "*.owl";
                    }
                });
                if (chooser.showOpenDialog(ConfirmLocationDialog.this) == JFileChooser.APPROVE_OPTION) {
                    onFileSelection(chooser.getSelectedFile());
                }
            }
        });
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.LINE_AXIS));
        infoPanel.add(textField);
        infoPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        infoPanel.add(ellipsis, BorderLayout.EAST);
        Object msg[] = { message, infoPanel };
        optionPane.setMessage(msg);
        optionPane.setMessageType(JOptionPane.QUESTION_MESSAGE);
        optionPane.setOptionType(JOptionPane.OK_CANCEL_OPTION);
        optionPane.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (ConfirmLocationDialog.this.isVisible() && (evt.getSource() == optionPane) && (evt.getPropertyName().equals((JOptionPane.VALUE_PROPERTY)) || evt.getPropertyName().equals((JOptionPane.INPUT_VALUE_PROPERTY)))) {
                    if (optionPane.getValue() instanceof Integer && ((Integer) optionPane.getValue()) == JOptionPane.OK_OPTION) {
                        onOKAction();
                    }
                    if (optionPane.getValue() instanceof Integer && ((Integer) optionPane.getValue()) == JOptionPane.CANCEL_OPTION) {
                        onCancelAction();
                    }
                }
            }
        });
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent evt) {
                exit_status = ABORT;
                setVisible(false);
                dispose();
            }
        });
        content = optionPane;
    }

    private void onTextBoxSelection(String urlText) {
        if (urlText != null && urlText.length() != 0) {
            try {
                url = new URL(urlText);
                textField.setForeground(Color.BLACK);
            } catch (MalformedURLException e) {
                textField.setForeground(Color.RED);
            }
        }
    }

    private void onFileSelection(File selectedFile) {
        try {
            textField.setText(selectedFile.toURL().toString());
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(this, "The selected file won't connvert to a valid URL", "Invalid file", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onOKAction() {
        if (url == null) {
            optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
            JOptionPane.showMessageDialog(this, "URL field cannot be empty", "Empty URL", JOptionPane.ERROR_MESSAGE);
        } else {
            try {
                URLConnection sourceConnection = url.openConnection();
                sourceConnection.setConnectTimeout(10);
                if (sourceConnection.getContentLength() == -1) {
                    throw new IOException("Can't connect to " + url.toString());
                }
                exit_status = OK;
                setVisible(false);
                dispose();
            } catch (IOException e) {
                optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
                JOptionPane.showMessageDialog(this, "Please enter valid, resolvable source and target URLs...\n " + e.getMessage(), "Invalid URL", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onCancelAction() {
        exit_status = CANCEL;
        dispose();
    }

    public boolean isDismissed() {
        if (exit_status != 0) {
            return true;
        }
        return false;
    }

    public int getExitStatus() {
        return exit_status;
    }

    public URL getURL() {
        return url;
    }
}
