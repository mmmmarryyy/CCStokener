package org.iisc.ilts.dialog;

import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.*;
import java.beans.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.net.*;

public class AboutDialog extends JDialog {

    private static final String credits = "Brahmi - RTF WordProcessor with Indic Input Methods and\n" + "Indic Opentype fonts\n\n" + "This software consists of a WordProcessor, Indic Input Methods \n" + "Opentype fonts and other useful resources\n\n" + "Harsha Ravnikar <harsha_pr@users.sourceforge.net>\n" + "Anitha N <nanitha@users.sourceforge.net>\n" + "Ramchandrula Sastry <rsastry@users.sourceforge.net>\n\n" + "http://brahmi.sourceforge.net\n\n";

    private static final String button1String = "OK";

    private static final String button2String = "View License";

    private static final String licenseBoxTitle = "Brahmi is released under the following license";

    private static final String licenseName = "GNU GENERAL PUBLIC LICENSE";

    private static final String urlToLicense = "org/iisc/ilts/brahmi/resources/gpl.txt";

    private String licenseContents;

    private JOptionPane optionPane;

    private static final String classInfo = "Class name: org.iisc.ilts.dialog.CLASSNAME\nhttp://brahmi.sourceforge.net";

    public static String getClassInfo() {
        return classInfo;
    }

    public AboutDialog(Frame parentFrame, String title, boolean modal) {
        super(parentFrame, title, modal);
        Object[] array = { credits };
        ImageIcon brahmiImageIcon;
        brahmiImageIcon = new ImageIcon(getClass().getClassLoader().getResource("org/iisc/ilts/brahmi/resources/images/Brahmi16.gif"));
        Object[] options = { button1String, button2String };
        optionPane = new JOptionPane(array, JOptionPane.INFORMATION_MESSAGE, JOptionPane.YES_NO_OPTION, brahmiImageIcon, options, options[0]);
        setContentPane(optionPane);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent we) {
                optionPane.setValue(new Integer(JOptionPane.CLOSED_OPTION));
                dispose();
            }
        });
        optionPane.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent e) {
                String prop = e.getPropertyName();
                if (isVisible() && (e.getSource() == optionPane) && (prop.equals(JOptionPane.VALUE_PROPERTY) || prop.equals(JOptionPane.INPUT_VALUE_PROPERTY))) {
                    Object value = optionPane.getValue();
                    if (value == JOptionPane.UNINITIALIZED_VALUE) {
                        return;
                    }
                    optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
                    if (value.equals(button1String)) {
                        setVisible(false);
                        return;
                    } else if (value.equals(button2String)) {
                        licenseContents = readFile(getClass().getClassLoader().getResource(urlToLicense));
                        ErrorDialog err = new ErrorDialog(licenseBoxTitle, licenseName, licenseContents);
                    }
                }
            }
        });
        pack();
        setVisible(false);
    }

    public String readFile(URL url) {
        StringBuffer buffer = new StringBuffer();
        try {
            InputStreamReader isr = new InputStreamReader(url.openStream());
            Reader in = new BufferedReader(isr);
            int ch;
            while ((ch = in.read()) > -1) {
                buffer.append((char) ch);
            }
            in.close();
            return buffer.toString();
        } catch (IOException e) {
            ExceptionDialog.displayExceptionDialog(new org.iisc.ilts.utils.BrahmiIconFrame(), e);
            return null;
        }
    }
}
