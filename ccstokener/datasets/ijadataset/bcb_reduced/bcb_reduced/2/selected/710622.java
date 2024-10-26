package auo.cms.applet.hsvautotune;

import java.applet.*;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import org.math.io.files.*;
import org.math.plot.*;
import shu.cms.colorspace.depend.*;
import shu.cms.plot.*;

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
public class TuneSpotApplet extends Applet {

    protected boolean isStandalone = false;

    protected BorderLayout borderLayout1 = new BorderLayout();

    String mode;

    String dir;

    public String getParameter(String key, String def) {
        return isStandalone ? System.getProperty(key, def) : (getParameter(key) != null ? getParameter(key) : def);
    }

    public TuneSpotApplet() {
    }

    public void init() {
        try {
            mode = this.getParameter("param0", "HSV");
            dir = this.getParameter("param1", "fromHSV");
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
        ((PlotPanel) panel).removePlotToolBar();
        int w = this.getWidth();
        int h = this.getHeight();
        w = w == 0 ? 600 : w;
        h = h == 0 ? 600 : h;
        panel.setSize(w, h);
        panel.setPreferredSize(new Dimension(w, h));
        this.add(panel);
    }

    public void start() {
        URL base = null;
        try {
            base = this.isActive() ? this.getDocumentBase() : new File(System.getProperty("user.dir")).toURI().toURL();
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }
        try {
            for (int x = 0; x < 360; x += 15) {
                URL url = new URL(base, "hsvapplet/" + dir + "/" + Integer.toString(x) + mode + ".dat");
                double[][] dataArray = ASCIIFile.readDoubleArray(url.openStream());
                java.awt.Color c = HSV.getLineColor(x);
                int size = dataArray.length;
                for (int y = 0; y < size; y++) {
                    plot.addCacheScatterPlot(Integer.toString(x), c, dataArray[y]);
                }
            }
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        plot.setAxisLabels(mode.substring(0, 1), mode.substring(1, 2), mode.substring(2, 3));
        plot.drawCachePlot();
        if (mode.equals("HSV")) {
            plot.setFixedBounds(0, 0, 360);
            plot.setFixedBounds(1, 0, 100);
            plot.setFixedBounds(2, 0, 100);
        } else {
            plot.setFixedBounds(0, 0, 100);
            plot.setFixedBounds(1, 0, 120);
            plot.setFixedBounds(2, 0, 360);
        }
    }

    private Plot3D plot = Plot3D.getInstance();

    public String getAppletInfo() {
        return "Applet Information";
    }

    public String[][] getParameterInfo() {
        java.lang.String[][] pinfo = { { "param0", "String", "" } };
        return pinfo;
    }

    public static void main(String[] args) {
        TuneSpotApplet applet = new TuneSpotApplet();
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
        ;
        frame.setTitle("Applet Frame");
        frame.add(applet, BorderLayout.CENTER);
        applet.init();
        applet.start();
        frame.setSize(600, 600);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((d.width - frame.getSize().width) / 2, (d.height - frame.getSize().height) / 2);
        frame.setVisible(true);
    }
}
