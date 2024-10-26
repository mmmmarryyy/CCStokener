package auo.cms.applet.hsvautotune;

import java.applet.*;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import org.math.io.files.*;
import shu.cms.plot.*;
import shu.math.array.*;

/**
 * <p>Title: Colour Management System</p>
 *
 * <p>Description: a Colour Management System by Java</p>
 *
 * <p>Copyright: Copyright (c) 2009</p>
 *
 * <p>Company: skygroup</p>
 *
 * @author skyforce
 * @version 1.0
 */
public class HuePlaneApplet extends Applet {

    protected boolean isStandalone = false;

    protected BorderLayout borderLayout1 = new BorderLayout();

    String dir;

    private Plot3D plot = Plot3D.getInstance();

    public String getParameter(String key, String def) {
        return isStandalone ? System.getProperty(key, def) : (getParameter(key) != null ? getParameter(key) : def);
    }

    public HuePlaneApplet() {
    }

    public void init() {
        try {
            dir = this.getParameter("param0", "1");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        JPanel panel = plot.getPlotPanel();
        int w = this.getWidth();
        int h = this.getHeight();
        w = w == 0 ? 600 : w;
        h = h == 0 ? 550 : h;
        panel.setSize(w, h);
        panel.setPreferredSize(new Dimension(w, h));
        jCheckBox1.setText("Show Delta");
        jCheckBox1.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jCheckBox1_actionPerformed(e);
            }
        });
        this.add(jCheckBox1);
        this.add(jCheckBox1);
        this.add(panel);
    }

    public String getAppletInfo() {
        return "Applet Information";
    }

    public String[][] getParameterInfo() {
        java.lang.String[][] pinfo = { { "param0", "String", "" } };
        return pinfo;
    }

    public static void main(String[] args) {
        HuePlaneApplet applet = new HuePlaneApplet();
        applet.isStandalone = true;
        Frame frame;
        frame = new Frame() {

            protected void processWindowEvent(WindowEvent e) {
                super.processWindowEvent(e);
                if (e.getID() == WindowEvent.WINDOW_CLOSING) {
                    System.exit(0);
                }
            }

            public synchronized void setTitle(String title) {
                super.setTitle(title);
                enableEvents(AWTEvent.WINDOW_EVENT_MASK);
            }
        };
        frame.setTitle("Applet Frame");
        frame.add(applet, BorderLayout.CENTER);
        applet.init();
        applet.start();
        frame.setSize(700, 620);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((d.width - frame.getSize().width) / 2, (d.height - frame.getSize().height) / 2);
        frame.setVisible(true);
    }

    private double[][][] newDataArray;

    private double[][][] targetDataArray;

    protected JCheckBox jCheckBox1 = new JCheckBox();

    private void normalPlot() {
        plot.removeAllPlots();
        plot.addPlanePlot("new", java.awt.Color.black, newDataArray);
        plot.addPlanePlot("target", java.awt.Color.red, targetDataArray);
        plot.setAxisLabels("L*", "C*", "h*");
        plot.setFixedBounds(0, 0, 100);
        plot.setFixedBounds(1, 0, 100);
        plot.setFixedBounds(2, 80, 120);
        plot.addLegend();
        plot.rotateToAxis(3);
    }

    private void deltaHPlot() {
        plot.removeAllPlots();
        double[][] delta = new double[10][10];
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                delta[x][y] = newDataArray[x][y][2] - targetDataArray[x][y][2];
            }
        }
        double[] axis = DoubleArray.buildX(10, 100, 10);
        plot.addGridPlot("delta", axis, axis, delta);
        plot.setAxisLabels("S", "V", "dh");
        plot.setFixedBounds(0, 0, 100);
        plot.setFixedBounds(1, 0, 100);
        plot.setFixedBounds(2, -20, 20);
    }

    public void start() {
        URL base = null;
        try {
            base = this.isActive() ? this.getDocumentBase() : new File(System.getProperty("user.dir")).toURI().toURL();
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }
        try {
            URL url = new URL(base, "hueplane/" + dir + "/new.txt");
            newDataArray = DoubleArray.to3DDoubleArray(ASCIIFile.readDoubleArray(url.openStream()), 10, 10);
            url = new URL(base, "hueplane/" + dir + "/target.txt");
            targetDataArray = DoubleArray.to3DDoubleArray(ASCIIFile.readDoubleArray(url.openStream()), 10, 10);
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        normalPlot();
    }

    public void jCheckBox1_actionPerformed(ActionEvent e) {
        boolean select = this.jCheckBox1.isSelected();
        if (select) {
            deltaHPlot();
        } else {
            normalPlot();
        }
    }
}
