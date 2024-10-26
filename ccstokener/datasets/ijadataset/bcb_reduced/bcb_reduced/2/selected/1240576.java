package net.sourceforge.webcompmath.applets;

import javax.swing.*;
import java.awt.event.*;
import java.awt.BorderLayout;
import java.awt.Font;
import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.Color;
import net.sourceforge.webcompmath.data.*;
import net.sourceforge.webcompmath.draw.*;
import net.sourceforge.webcompmath.awt.*;

/**
 * A ScatterPlotApplet shows a scatter plot of data from a DataTableInput. The
 * user can enter the data in a two-column table that is shown in the applet. It
 * is also possible to configure the applet with a menu of file names. These
 * files, which must be in the same directory as the Web page on which the
 * applet appears, will appear in a menu. A file can contain data for the table,
 * with two numbers per line. When the user loads the file, the data replaces
 * the data in the table.
 */
public class ScatterPlotApplet extends JApplet implements ActionListener {

    /**
	 * Comment for <code>serialVersionUID</code>
	 */
    private static final long serialVersionUID = 3905237944380699449L;

    private JFrame frame;

    private String frameTitle;

    private JButton launchButton;

    private String launchButtonName;

    private DataTableInput table;

    private ScatterPlot scatterPlot;

    private DisplayCanvas canvas;

    private JButton loadFileButton;

    private JComboBox fileMenu;

    private String[] fileNames;

    private Controller mainController;

    private float fontSize = 0.0f;

    private boolean presentation = false;

    /**
	 * The init() method is called by the system to set up the applet. If the
	 * applet does not appear as a button, then init() creates the main panel of
	 * the applet and calls setUpMainPanel to set it up.
	 */
    @Override
    public void init() {
        String fstr = getParameter("FontSize");
        presentation = "yes".equalsIgnoreCase(getParameter("Presentation", "no"));
        if (fstr != null) {
            fontSize = Float.valueOf(fstr).floatValue();
            AppletUtilities.setUIFontSize(fontSize);
        } else if (presentation) {
            fontSize = 20.0f;
            AppletUtilities.setUIFontSize(fontSize);
        }
        frameTitle = getParameter("FrameTitle");
        if (frameTitle == null) {
            frameTitle = "Scatter Plots";
            int pos = frameTitle.lastIndexOf('.');
            if (pos > -1) frameTitle = frameTitle.substring(pos + 1);
        }
        setLayout(new BorderLayout());
        int height = getSize().height;
        launchButtonName = getParameter("LaunchButtonName");
        if ((height > 0 && height <= 50) || launchButtonName != null) {
            if (launchButtonName == null) launchButtonName = "Launch " + frameTitle;
            launchButton = new JButton(launchButtonName);
            add(launchButton, BorderLayout.CENTER);
            launchButton.addActionListener(this);
        } else {
            add(makeMainPanel(), BorderLayout.CENTER);
        }
    }

    /**
	 * Create the main panel of the applet.
	 * 
	 * @return the newly made panel
	 */
    public JPanel makeMainPanel() {
        WcmPanel panel = new WcmPanel(2);
        mainController = panel.getController();
        Color background = new Color(0, 0, 180);
        panel.setBackground(background);
        panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        setLayout(new BorderLayout());
        table = new DataTableInput(null, 2);
        table.setColumnName(0, getParameter("ColumnName1", "X"));
        table.setColumnName(1, getParameter("ColumnName2", "Y"));
        table.setThrowErrors(true);
        if ("yes".equalsIgnoreCase(getParameter("ShowColumnTitles", "yes"))) table.setShowColumnTitles(true);
        if ("yes".equalsIgnoreCase(getParameter("ShowRowNumbers", "yes"))) table.setShowRowNumbers(true);
        Parser parser = new Parser();
        table.addVariablesToParser(parser);
        ExpressionInput input1 = new ExpressionInput(table.getColumnName(0), parser);
        input1.setOnUserAction(mainController);
        ExpressionInput input2 = new ExpressionInput(table.getColumnName(1), parser);
        input2.setOnUserAction(mainController);
        scatterPlot = new ScatterPlot(table, input1.getExpression(), input2.getExpression());
        if (!"yes".equalsIgnoreCase(getParameter("ShowRegressionLine", "yes"))) scatterPlot.setShowRegressionLine(false);
        if (!"yes".equalsIgnoreCase(getParameter("MissingValueIsError", "yes"))) scatterPlot.setMissingValueIsError(false);
        canvas = new DisplayCanvas();
        WcmAxes axes = new WcmAxes();
        if (presentation) {
            axes.setLineWidth(3.0f);
            axes.setMajorTickWidth(3.0f);
            scatterPlot.setLineWidth(3);
            scatterPlot.setCrossHalfSize(5);
            scatterPlot.setCrossLineWidth(3);
        }
        canvas.add(axes);
        canvas.add(scatterPlot);
        mainController.setErrorReporter(canvas);
        ComputeButton computeButton = new ComputeButton("Update Display");
        computeButton.setOnUserAction(mainController);
        computeButton.setBackground(Color.lightGray);
        JPanel menu = makefileMenu();
        WcmPanel inputPanel = null;
        JPanel bottom = null;
        if ("yes".equalsIgnoreCase(getParameter("UseExpressionInputs", "yes"))) {
            inputPanel = new WcmPanel(1, 2);
            inputPanel.setBackground(Color.lightGray);
            WcmPanel leftInput = new WcmPanel();
            leftInput.setBackground(Color.lightGray);
            leftInput.add(new JLabel("  Plot:  "), BorderLayout.WEST);
            leftInput.add(input1, BorderLayout.CENTER);
            inputPanel.add(leftInput);
            WcmPanel rightInput = new WcmPanel();
            rightInput.setBackground(Color.lightGray);
            rightInput.add(new JLabel(" versus: "), BorderLayout.WEST);
            rightInput.add(input2, BorderLayout.CENTER);
            inputPanel.add(rightInput);
            bottom = new WcmPanel(new BorderLayout(12, 3));
            bottom.setBackground(background);
            bottom.add(inputPanel, BorderLayout.CENTER);
            bottom.add(computeButton, BorderLayout.EAST);
        }
        if (scatterPlot.getShowRegressionLine() && "yes".equalsIgnoreCase(getParameter("ShowStats", "yes"))) {
            DisplayLabel dl = new DisplayLabel("Slope = #;  Intercept = #;  Correlation = #", new Value[] { scatterPlot.getValueObject(ScatterPlot.SLOPE), scatterPlot.getValueObject(ScatterPlot.INTERCEPT), scatterPlot.getValueObject(ScatterPlot.CORRELATION) });
            dl.setHorizontalAlignment(JLabel.CENTER);
            dl.setBackground(Color.lightGray);
            dl.setOpaque(true);
            dl.setForeground(new Color(200, 0, 0));
            if (fontSize == 0.0f) {
                dl.setFont(new Font("Serif", Font.PLAIN, 14));
            }
            if (bottom != null) bottom.add(dl, BorderLayout.SOUTH); else {
                bottom = new WcmPanel(new BorderLayout(12, 3));
                bottom.add(dl, BorderLayout.CENTER);
                bottom.add(computeButton, BorderLayout.EAST);
            }
        }
        if (bottom == null) {
            if (menu != null) menu.add(computeButton, BorderLayout.EAST); else {
                bottom = new JPanel();
                bottom.add(computeButton);
            }
        }
        panel.add(canvas, BorderLayout.CENTER);
        panel.add(table, BorderLayout.WEST);
        if (bottom != null) panel.add(bottom, BorderLayout.SOUTH);
        if (menu != null) panel.add(menu, BorderLayout.NORTH); else {
            String title = getParameter("PanelTitle");
            if (title != null) {
                JLabel pt = new JLabel(title, JLabel.CENTER);
                pt.setBackground(Color.lightGray);
                pt.setForeground(new Color(200, 0, 0));
                if (fontSize == 0.0f) {
                    pt.setFont(new Font("Serif", Font.PLAIN, 14));
                }
                panel.add(pt, BorderLayout.NORTH);
            }
        }
        return panel;
    }

    @SuppressWarnings("unchecked")
    private JPanel makefileMenu() {
        Vector names = new Vector();
        fileMenu = new JComboBox();
        String file = getParameter("File");
        int ct = 1;
        if (file == null) {
            file = getParameter("File1");
            ct = 2;
        }
        while (file != null) {
            file = file.trim();
            int pos = file.indexOf(";");
            String menuEntry;
            if (pos == -1) menuEntry = file; else {
                menuEntry = file.substring(0, pos).trim();
                file = file.substring(pos + 1).trim();
            }
            names.addElement(file);
            fileMenu.addItem(menuEntry);
            file = getParameter("File" + ct);
            ct++;
        }
        if (names.size() == 0) {
            fileMenu = null;
            return null;
        } else {
            fileNames = new String[names.size()];
            for (int i = 0; i < names.size(); i++) fileNames[i] = (String) names.elementAt(i);
            JPanel p = new JPanel();
            p.setBackground(Color.lightGray);
            p.setLayout(new BorderLayout(5, 5));
            p.add(fileMenu, BorderLayout.CENTER);
            loadFileButton = new JButton("Load Data File: ");
            loadFileButton.addActionListener(this);
            p.add(loadFileButton, BorderLayout.WEST);
            fileMenu.setBackground(Color.white);
            return p;
        }
    }

    private void doLoadFile(String name) {
        InputStream in;
        try {
            URL url = new URL(getDocumentBase(), name);
            in = url.openStream();
        } catch (Exception e) {
            canvas.setErrorMessage(null, "Unable to open file named \"" + name + "\": " + e);
            return;
        }
        Reader inputReader = new InputStreamReader(in);
        try {
            table.readFromStream(inputReader);
            inputReader.close();
        } catch (Exception e) {
            canvas.setErrorMessage(null, "Unable to get data from file \"" + name + "\": " + e.getMessage());
            return;
        }
        mainController.compute();
    }

    /**
	 * Respond when user clicks a button; not meant to be called directly. This
	 * opens and closes the separate window.
	 * 
	 * @param evt
	 *            the event created when the user clicked the button
	 */
    public synchronized void actionPerformed(ActionEvent evt) {
        Object source = evt.getSource();
        if (loadFileButton != null && source == loadFileButton) {
            doLoadFile(fileNames[fileMenu.getSelectedIndex()]);
        } else if (source == launchButton && launchButton != null) {
            launchButton.setEnabled(false);
            if (frame == null) {
                frame = new JFrame(frameTitle);
                frame.add(makeMainPanel());
                frame.addWindowListener(new WindowAdapter() {

                    @Override
                    public void windowClosing(WindowEvent evt) {
                        frame.dispose();
                    }

                    @Override
                    public void windowClosed(WindowEvent evt) {
                        frameClosed();
                    }
                });
                frame.pack();
                frame.setLocation(50, 50);
                frame.setVisible(true);
                launchButton.setText("Close Window");
                launchButton.setEnabled(true);
            } else {
                frame.dispose();
            }
        }
    }

    private synchronized void frameClosed() {
        frame = null;
        launchButton.setText(launchButtonName);
        launchButton.setEnabled(true);
    }

    /**
	 * Return the applet parameter with a given param name, but if no such
	 * applet param exists, return a default value instead.
	 * 
	 * @param paramName
	 *            the name of the parameter
	 * @param defaultValue
	 *            the value to return if not set
	 * @return the parameter's value
	 */
    protected String getParameter(String paramName, String defaultValue) {
        String val = getParameter(paramName);
        return (val == null) ? defaultValue : val;
    }
}
