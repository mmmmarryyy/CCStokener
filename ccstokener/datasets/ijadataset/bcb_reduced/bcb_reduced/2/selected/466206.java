package org.jhotdraw.samples.draw;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.LinkedList;
import javax.swing.BoxLayout;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.jhotdraw.draw.DOMStorableInputOutputFormat;
import org.jhotdraw.draw.Drawing;
import org.jhotdraw.draw.ImageFigure;
import org.jhotdraw.draw.ImageInputFormat;
import org.jhotdraw.draw.ImageOutputFormat;
import org.jhotdraw.draw.InputFormat;
import org.jhotdraw.draw.OutputFormat;
import org.jhotdraw.draw.TextFigure;
import org.jhotdraw.draw.TextInputFormat;
import org.jhotdraw.gui.Worker;
import org.jhotdraw.xml.NanoXMLDOMInput;
import org.jhotdraw.xml.NanoXMLDOMOutput;

/**
 * DrawApplet.
 *
 * @author  wrandels
 * @version 2.1 2006-07-15 Added main method.
 * <br>2.0 Changed to support double precision coordinates.
 * <br>1.0 Created on 10. Marz 2004, 13:22.
 */
public class DrawApplet extends JApplet {

    private static String version;

    private static final String NAME = "JHotDraw Draw";

    private DrawingPanel drawingPanel;

    /**
     * We override getParameter() to make it work even if we have no Applet
     * context.
     */
    public String getParameter(String name) {
        try {
            return super.getParameter(name);
        } catch (NullPointerException e) {
            return null;
        }
    }

    protected String getVersion() {
        if (version == null) {
            BufferedReader r = null;
            try {
                r = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("version.txt"), "UTF-8"));
                version = r.readLine();
            } catch (Throwable e) {
                version = "unknown";
            } finally {
                if (r != null) try {
                    r.close();
                } catch (IOException e) {
                }
            }
        }
        return version;
    }

    /** Initializes the applet DrawApplet */
    public void init() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Throwable e) {
        }
        Container c = getContentPane();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        String[] labels = getAppletInfo().split("\n");
        for (int i = 0; i < labels.length; i++) {
            c.add(new JLabel((labels[i].length() == 0) ? " " : labels[i]));
        }
        new Worker() {

            public Object construct() {
                Object result;
                try {
                    if (getParameter("data") != null) {
                        NanoXMLDOMInput domi = new NanoXMLDOMInput(new DrawFigureFactory(), new StringReader(getParameter("data")));
                        result = domi.readObject(0);
                    } else if (getParameter("datafile") != null) {
                        InputStream in = null;
                        try {
                            URL url = new URL(getDocumentBase(), getParameter("datafile"));
                            in = url.openConnection().getInputStream();
                            NanoXMLDOMInput domi = new NanoXMLDOMInput(new DrawFigureFactory(), in);
                            result = domi.readObject(0);
                        } finally {
                            if (in != null) in.close();
                        }
                    } else {
                        result = null;
                    }
                } catch (Throwable t) {
                    result = t;
                }
                return result;
            }

            public void finished(Object value) {
                if (value instanceof Throwable) {
                    ((Throwable) value).printStackTrace();
                }
                Container c = getContentPane();
                c.setLayout(new BorderLayout());
                c.removeAll();
                c.add(drawingPanel = new DrawingPanel());
                Object result = getValue();
                initComponents();
                if (result != null) {
                    if (result instanceof Drawing) {
                        Drawing drawing = (Drawing) result;
                        setDrawing(drawing);
                    } else if (result instanceof Throwable) {
                        getDrawing().add(new TextFigure(result.toString()));
                        ((Throwable) result).printStackTrace();
                    }
                }
                initDrawing(getDrawing());
                c.validate();
            }
        }.start();
    }

    private void setDrawing(Drawing d) {
        drawingPanel.setDrawing(d);
    }

    private Drawing getDrawing() {
        return drawingPanel.getDrawing();
    }

    /**
     * Configure Drawing object to support copy and paste.
     */
    private void initDrawing(Drawing d) {
        LinkedList<InputFormat> inputFormats = new LinkedList<InputFormat>();
        LinkedList<OutputFormat> outputFormats = new LinkedList<OutputFormat>();
        DOMStorableInputOutputFormat ioFormat = new DOMStorableInputOutputFormat(new DrawFigureFactory());
        inputFormats.add(ioFormat);
        outputFormats.add(ioFormat);
        inputFormats.add(new ImageInputFormat(new ImageFigure()));
        inputFormats.add(new TextInputFormat(new TextFigure()));
        outputFormats.add(new ImageOutputFormat());
        d.setInputFormats(inputFormats);
        d.setOutputFormats(outputFormats);
    }

    public void setData(String text) {
        if (text != null && text.length() > 0) {
            StringReader in = new StringReader(text);
            try {
                NanoXMLDOMInput domi = new NanoXMLDOMInput(new DrawFigureFactory(), in);
                setDrawing((Drawing) domi.readObject(0));
            } catch (Throwable e) {
                getDrawing().clear();
                TextFigure tf = new TextFigure();
                tf.setText(e.getMessage());
                tf.setBounds(new Point2D.Double(10, 10), new Point2D.Double(100, 100));
                getDrawing().add(tf);
                e.printStackTrace();
            } finally {
                if (in != null) in.close();
            }
        }
    }

    public String getData() {
        CharArrayWriter out = new CharArrayWriter();
        try {
            NanoXMLDOMOutput domo = new NanoXMLDOMOutput(new DrawFigureFactory());
            domo.writeObject(getDrawing());
            domo.save(out);
        } catch (IOException e) {
            TextFigure tf = new TextFigure();
            tf.setText(e.getMessage());
            tf.setBounds(new Point2D.Double(10, 10), new Point2D.Double(100, 100));
            getDrawing().add(tf);
            e.printStackTrace();
        } finally {
            if (out != null) out.close();
        }
        return out.toString();
    }

    public String[][] getParameterInfo() {
        return new String[][] { { "data", "String", "the data to be displayed by this applet." }, { "datafile", "URL", "an URL to a file containing the data to be displayed by this applet." } };
    }

    public String getAppletInfo() {
        return NAME + "\nVersion " + getVersion() + "\n\nCopyright 2006-2007 (c) by the authors of JHotDraw" + "\nThis software is licensed under LGPL or" + "\nCreative Commons 2.5 BY";
    }

    private void initComponents() {
        toolButtonGroup = new javax.swing.ButtonGroup();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                JFrame f = new JFrame("JHotDraw Draw Applet");
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                DrawApplet a = new DrawApplet();
                f.getContentPane().add(a);
                a.init();
                f.setSize(500, 400);
                f.setVisible(true);
                a.start();
            }
        });
    }

    private javax.swing.ButtonGroup toolButtonGroup;
}
