package spidr.applets.ptolemy.plot;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

/** * This class provides a labeled box within which to place a data plot. * A title, X and Y axis labels, tick marks, and a legend are all supported. * Zooming in and out is supported.  To zoom in, drag the mouse * downwards to draw a box.  To zoom out, drag the mouse upward. * <p> * The box can be configured either through a file with commands or * through direct invocation of the public methods of the class. * Use the read() method to read a URL or a file. * <p> * When calling the methods, in most cases the changes will not * be visible until paint() has been called.  To request that this * be done, call repaint(). * <p> * The ASCII format for the file allows any number * commands, one per line.  Unrecognized commands and commands with * syntax errors are ignored.  Comments are denoted by a line starting * with a pound sign "#".  The recognized commands include: * <pre> * TitleText: <i>string</i> * XLabel: <i>string</i> * YLabel: <i>string</i> * </pre> * These commands provide a title and labels for the X (horizontal) and Y * (vertical) axes. * A <i>string</i> is simply a sequence of characters, possibly * including spaces.  There is no need here to surround them with * quotation marks, and in fact, if you do, the quotation marks will * be included in the labels. * <p> * The ranges of the X and Y axes can be optionally given by commands like: * <pre> * XRange: <i>min</i>, <i>max</i> * YRange: <i>min</i>, <i>max</i> * </pre> * The arguments <i>min</i> and <i>max</i> are numbers, possibly * including a sign and a decimal point. If they are not specified, * then the ranges are computed automatically from the data and padded * slightly so that datapoints are not plotted on the axes. * <p> * The tick marks for the axes are usually computed automatically from * the ranges.  Every attempt is made to choose reasonable positions * for the tick marks regardless of the data ranges (powers of * ten multiplied by 1, 2, or 5 are used).  However, they can also be * specified explicitly using commands like: * <pre> * XTicks: <i>label position, label position, ...</i> * YTicks: <i>label position, label position, ...</i> * </pre> * A <i>label</i> is a string that must be surrounded by quotation * marks if it contains any spaces.  A <i>position</i> is a number * giving the location of the tick mark along the axis.  For example, * a horizontal axis for a frequency domain plot might have tick marks * as follows: * <pre> * XTicks: -PI -3.14159, -PI/2 -1.570795, 0 0, PI/2 1.570795, PI 3.14159 * </pre> * Tick marks could also denote years, months, days of the week, etc. * <p> * The X and Y axes can use a logarithmic scale with the following commands: * <pre> * XLog: on * YLog: on * </pre> * The grid labels represent powers of 10.  Note that if a logarithmic * scale is used, then the values must be positive.  Non-positive values * will be silently dropped. * <p> * By default, tick marks are connected by a light grey background grid. * This grid can be turned off with the following command: * <pre> * Grid: off * </pre> * It can be turned back on with * <pre> * Grid: on * </pre> * Also, by default, the first ten data sets are shown each in a unique color. * The use of color can be turned off with the command: * <pre> * Color: off * </pre> * It can be turned back on with * <pre> * Color: on * </pre> * All of the above commands can also be invoked directly by calling the * the corresponding public methods from some Java procedure. * <p> * This class uses features of JDK 1.1, and hence if used in an applet, * it can only be viewed by a browser that supports JDK 1.1. * * @author Edward A. Lee, Christopher Hylands * @version $Id: PlotBox.java,v 1.2 2009-10-20 13:11:48 dmedv Exp $ */
public class PlotBox extends Panel implements Observer {

    protected Color[] currentColors = new Color[10];

    private boolean useSync = true;

    public void clearCurrentColors() {
        for (int i = 0; i < currentColors.length; i++) {
            currentColors[i] = null;
        }
    }

    public void setCurrentColor(int index, Color color) {
        currentColors[index] = color;
    }

    public PlotBox(boolean useSync) {
        setLayout(new FlowLayout(FlowLayout.RIGHT));
        addMouseListener(new ZoomListener());
        addMouseMotionListener(new DragListener());
        _measureFonts();
        this.useSync = useSync;
        if (this.useSync) PlotRegistry.instance().addObserver(this);
    }

    public PlotBox() {
        this(true);
    }

    /** Add a legend (displayed at the upper right) for the specified     *  data set with the specified string.  Short strings generally     *  fit better than long strings.     *  @param dataset The dataset index.     *  @param legend The label for the dataset.     */
    public void addLegend(int dataset, String legend) {
        _legendStrings.addElement(legend);
        _legendDatasets.addElement(new Integer(dataset));
    }

    /** Specify a tick mark for the X axis.  The label given is placed     *  on the axis at the position given by <i>position</i>. If this     *  is called once or more, automatic generation of tick marks is     *  disabled.  The tick mark will appear only if it is within the X     *  range.     *  @param label The label for the tick mark.     *  @param position The position on the X axis.     */
    public void addXTick(String label, double position) {
        if (_xticks == null) {
            _xticks = new Vector();
            _xticklabels = new Vector();
        }
        _xticks.addElement(new Double(position));
        _xticklabels.addElement(label);
    }

    /** Specify a tick mark for the Y axis.  The label given is placed     *  on the axis at the position given by <i>position</i>. If this     *  is called once or more, automatic generation of tick marks is     *  disabled.  The tick mark will appear only if it is within the Y     *  range.     *  @param label The label for the tick mark.     *  @param position The position on the Y axis.     */
    public void addYTick(String label, double position) {
        if (_yticks == null) {
            _yticks = new Vector();
            _yticklabels = new Vector();
        }
        _yticks.addElement(new Double(position));
        _yticklabels.addElement(label);
    }

    /** If the argument is true, clear the axes.  I.e., set all parameters     *  controlling the axes to their initial conditions.     *  For the change to take effect, call repaint().  If the argument     *  is false, do nothing.     *  @param axes If true, clear the axes parameters.     */
    public synchronized void clear(boolean axes) {
        _xBottom = Double.MAX_VALUE;
        _xTop = -Double.MAX_VALUE;
        _yBottom = Double.MAX_VALUE;
        _yTop = -Double.MAX_VALUE;
        if (axes) {
            _yMax = 0;
            _yMin = 0;
            _xMax = 0;
            _xMin = 0;
            _xRangeGiven = false;
            _yRangeGiven = false;
            _xlog = false;
            _ylog = false;
            _grid = true;
            _usecolor = true;
            _filespec = null;
            _xlabel = null;
            _ylabel = null;
            _title = null;
            _legendStrings = new Vector();
            _legendDatasets = new Vector();
            _xticks = null;
            _xticklabels = null;
            _yticks = null;
            _yticklabels = null;
        }
    }

    /** Rescale so that the data that is currently plotted just fits.     *  This is done based on the protected variables _xBottom, _xTop,     *  _yBottom, and _yTop.  It is up to derived classes to ensure that     *  variables are valid.     *  This method calls repaint(), which eventually causes the display     *  to be updated.     */
    public synchronized void fillPlot() {
        _setXRange(_xBottom, _xTop);
        _setYRange(_yBottom, _yTop);
        if (this.useSync) PlotRegistry.instance().changeRange(_xBottom, _xTop);
        repaint();
    }

    /** Convert a color name into a Color. Currently, only a very limited     *  set of color names is supported: black, white, red, green, and blue.     *  @param name A color name, or null if not found.     *  @return An instance of Color.     */
    public static Color getColorByName(String name) {
        try {
            Color col = new Color(Integer.parseInt(name, 16));
            return col;
        } catch (NumberFormatException e) {
        }
        String names[][] = { { "black", "00000" }, { "white", "ffffff" }, { "red", "ff0000" }, { "green", "00ff00" }, { "blue", "0000ff" } };
        for (int i = 0; i < names.length; i++) {
            if (name.equals(names[i][0])) {
                try {
                    Color col = new Color(Integer.parseInt(names[i][1], 16));
                    return col;
                } catch (NumberFormatException e) {
                }
            }
        }
        return null;
    }

    /** Get the file specification that was given by setDataurl.     *  This method is deprecated.  Use read() instead.     *  @deprecated     */
    public String getDataurl() {
        return _filespec;
    }

    /** Get the document base that was set by setDocumentBase.     *  This method is deprecated.  Use read() instead.     *  @deprecated     */
    public URL getDocumentBase() {
        return _documentBase;
    }

    /** Get the legend for a dataset, or null if there is none.     *  The legend would have been set by addLegend().     *  @param dataset The dataset index.     *  @return The legend label, or null if there is none.     */
    public String getLegend(int dataset) {
        int idx = _legendDatasets.indexOf(new Integer(dataset), 0);
        if (idx != -1) {
            return (String) _legendStrings.elementAt(idx);
        } else {
            return null;
        }
    }

    /** Get the minimum size of this component.     *  This is simply the dimensions specified by setBounds() or setSize().     *  @return The minimum size.     */
    public Dimension getMinimumSize() {
        return new Dimension(_width, _height);
    }

    /** Get the preferred size of this component.     *  This is simply the dimensions specified by setBounds() or setSize().     *  @return The preferred size.     */
    public Dimension getPreferredSize() {
        return new Dimension(_width, _height);
    }

    /** Initialize the component, creating the fill button and parsing     *  an input file, if one has been specified.  This is deprecated.     *  Call setButtons() and read() instead.     *  @deprecated     */
    public void init() {
        setButtons(true);
        if (_filespec != null) {
            parseFile(_filespec, _documentBase);
        }
    }

    /** Paint the component contents, which in this base class is     *  only the axes.     *  @param graphics The graphics context.     */
    public void paint(Graphics graphics) {
        _drawPlot(graphics, true);
    }

    /** Syntactic sugar for parseFile(filespec, documentBase).     *  This method is deprecated.  Use read().  Note that this method     *  no longer supports pxgraph binary files.  Use readPxgraph() in the     *  derived class Plot instead.     *  @deprecated     */
    public void parseFile(String filespec) {
        parseFile(filespec, (URL) null);
    }

    /** Open up the input file, which could be stdin, a URL, or a file.     *  This method is deprecated.  Use read() instead.     *  @deprecated     */
    public void parseFile(String filespec, URL documentBase) {
        DataInputStream in = null;
        if (filespec == null || filespec.length() == 0) {
            in = new DataInputStream(System.in);
        } else {
            try {
                URL url = null;
                if (documentBase == null && _documentBase != null) {
                    documentBase = _documentBase;
                }
                if (documentBase == null) {
                    url = new URL(filespec);
                } else {
                    try {
                        url = new URL(documentBase, filespec);
                    } catch (NullPointerException e) {
                        url = new URL(filespec);
                    }
                }
                in = new DataInputStream(url.openStream());
            } catch (MalformedURLException e) {
                try {
                    in = new DataInputStream(new FileInputStream(filespec));
                } catch (FileNotFoundException me) {
                    _errorMsg = new String[2];
                    _errorMsg[0] = "File not found: " + filespec;
                    _errorMsg[1] = me.getMessage();
                    return;
                } catch (SecurityException me) {
                    _errorMsg = new String[2];
                    _errorMsg[0] = "Security Exception: " + filespec;
                    _errorMsg[1] = me.getMessage();
                    return;
                }
            } catch (IOException ioe) {
                _errorMsg = new String[3];
                _errorMsg[0] = "Failure opening URL: ";
                _errorMsg[1] = " " + filespec;
                _errorMsg[2] = ioe.getMessage();
                return;
            }
        }
        try {
            BufferedReader din = new BufferedReader(new InputStreamReader(in));
            String line = din.readLine();
            while (line != null) {
                _parseLine(line);
                line = din.readLine();
            }
        } catch (MalformedURLException e) {
            _errorMsg = new String[2];
            _errorMsg[0] = "Malformed URL: " + filespec;
            _errorMsg[1] = e.getMessage();
            return;
        } catch (IOException e) {
            _errorMsg = new String[2];
            _errorMsg[0] = "Failure reading data: " + filespec;
            _errorMsg[1] = e.getMessage();
            _errorMsg[1] = e.getMessage();
        } finally {
            try {
                in.close();
            } catch (IOException me) {
            }
        }
    }

    /** Read commands and/or plot data from an input stream.     *  To update the display, call repaint(), or make the plot visible with     *  setVisible(true).     *  <p>     *  To read from standard input, use:     *  <pre>     *     read(System.in);     *  </pre>     *  To read from a url, use:     *  <pre>     *     read(url.openStream());     *  </pre>     *  To read a URL from within an applet, use:     *  <pre>     *     URL url = new URL(getDocumentBase(), urlSpec);     *     read(url.openStream());     *  </pre>     *  Within an application, if you have an absolute URL, use:     *  <pre>     *     URL url = new URL(urlSpec);     *     read(url.openStream());     *  </pre>     *  To read from a file, use:     *  <pre>     *     read(new FileInputStream(filename));     *  </pre>     *  The input stream currently must be in ASCII format, although a binary     *  format will be supported eventually.     *  @param in The input stream.     */
    public void read(InputStream in) throws IOException {
        try {
            if (false) {
                DataInputStream din = new DataInputStream(new BufferedInputStream(in));
                try {
                } finally {
                    din.close();
                }
            } else {
                BufferedReader din = new BufferedReader(new InputStreamReader(in));
                try {
                    String line = din.readLine();
                    while (line != null) {
                        _parseLine(line);
                        line = din.readLine();
                    }
                } finally {
                    din.close();
                }
            }
        } catch (IOException e) {
            _errorMsg = new String[2];
            _errorMsg[0] = "Failure reading input data.";
            _errorMsg[1] = e.getMessage();
            throw e;
        }
    }

    /** Move and resize this component. The new location of the top-left     *  corner is specified by x and y, and the new size is specified by     *  width and height. This overrides the base class method to make     *  a record of the new size.     *  @param x The new x-coordinate of this component.     *  @param y The new y-coordinate of this component.     *  @param width The new width of this component.     *  @param height The new height of this component.     */
    public void setBounds(int x, int y, int width, int height) {
        _width = width;
        _height = height;
        super.setBounds(x, y, _width, _height);
    }

    /** If the argument is true, make a fill button visible at the upper     *  right.  This button auto-scales the plot.     *  NOTE: The button may infringe on the title space,     *  if the title is long.  In an application, it is preferable to provide     *  a menu with the fill command.  This way, when printing the plot,     *  the printed plot will not have a spurious button.  Thus, this method     *  should be used only by applets, which normally do not have menus.     */
    public void setButtons(boolean visible) {
        if (_fillButton == null) {
            _fillButton = new Button("zoom out");
            _fillButton.addActionListener(new FillButtonListener());
            add(_fillButton);
        }
        _fillButton.setVisible(visible);
    }

    /** Set the size of the plot.     *  @param width The width, in pixels.     *  @param height The height, in pixels.     */
    public void setSize(int width, int height) {
        _width = width;
        _height = height;
        super.setSize(width, height);
    }

    /** Set the background color.     *  @param background The background color.     */
    public void setBackground(Color background) {
        _background = background;
        super.setBackground(_background);
    }

    /** Set the foreground color.     *  @param foreground The foreground color.     */
    public void setForeground(Color foreground) {
        _foreground = foreground;
        super.setForeground(_foreground);
    }

    /** Set the binary flag to true if we are reading pxgraph format binary     *  data. This method is deprecated.  Use read() instead, which recognizes     *  binary files.  To read pxgraph binary files, use readPxgraph()     *  in the derived class Plot.     *  @deprecated     */
    public void setBinary(boolean binary) {
        _binary = binary;
    }

    /** Set the file to read when init() is called.     *  This method is deprecated.  Use read() instead.     *  @deprecated     */
    public void setDataurl(String filespec) {
        _filespec = filespec;
    }

    /** Set the document base to used when init() is called to read a URL.     *  This method is deprecated.  Use read() instead.     *  @deprecated     */
    public void setDocumentBase(URL documentBase) {
        _documentBase = documentBase;
    }

    /** Control whether the grid is drawn.     *  @param grid If true, a grid is drawn.     */
    public void setGrid(boolean grid) {
        _grid = grid;
    }

    /** Set the label font, which is used for axis labels and legend labels.     *  The font names understood are those understood by     *  java.awt.Font.decode().     *  @param name A font name.     */
    public void setLabelFont(String name) {
        _labelfont = Font.decode(name);
    }

    /** Set the title of the graph.     *  @param title The title.     */
    public void setTitle(String title) {
        _title = title;
    }

    /** Set the title font.     *  The font names understood are those understood by     *  java.awt.Font.decode().     *  @param name A font name.     */
    public void setTitleFont(String name) {
        _titlefont = Font.decode(name);
        _titleFontMetrics = getFontMetrics(_titlefont);
    }

    /** Set the label for the X (horizontal) axis.     *  @param label The label.     */
    public void setXLabel(String label) {
        _xlabel = label;
    }

    /** Specify whether the X axis is drawn with a logarithmic scale.     *  @param xlog If true, logarithmic axis is used.     */
    public void setXLog(boolean xlog) {
        _xlog = xlog;
    }

    /** Specify whether the X axis is drawn with a time scale.     *  @param xlog If true, time axis is used.     */
    public void setXTime(boolean xtime) {
        _xtime = xtime;
    }

    /** Set the X (horizontal) range of the plot.  If this is not done     *  explicitly, then the range is computed automatically from data     *  available when the plot is drawn.  If min and max     *  are identical, then the range is arbitrarily spread by 1.     *  @param min The left extent of the range.     *  @param max The right extent of the range.     */
    public void setXRange(double min, double max) {
        _xRangeGiven = true;
        _xlowgiven = min;
        _xhighgiven = max;
        _setXRange(min, max);
        if (this.useSync) PlotRegistry.instance().changeRange(min, max);
    }

    /** Get the X (horizontal) range of the plot.     */
    public double[] getXRange() {
        double[] range = new double[2];
        range[0] = _xMin;
        range[1] = _xMax;
        return range;
    }

    /** Set the label for the Y (vertical) axis.     *  @param label The label.     */
    public void setYLabel(String label) {
        _ylabel = label;
    }

    /** Specify whether the Y axis is drawn with a logarithmic scale.     *  @param ylog If true, logarithmic axis is used.     */
    public void setYLog(boolean ylog) {
        _ylog = ylog;
    }

    /** Set the Y (vertical) range of the plot.  If this is not done     *  explicitly, then the range is computed automatically from data     *  available when the plot is drawn.  If min and max are identical,     *  then the range is arbitrarily spread by 0.1.     *  @param min The bottom extent of the range.     *  @param max The top extent of the range.     */
    public void setYRange(double min, double max) {
        _yRangeGiven = true;
        _ylowgiven = min;
        _yhighgiven = max;
        _setYRange(min, max);
    }

    /** Write the current data and plot configuration to the     *  specified stream.  The output is buffered, and is flushed and     *  closed before exiting.  Derived classes should override _write()     *  rather than this method.     *  @param out An output stream.     */
    public void write(OutputStream out) {
        PrintWriter output = new PrintWriter(new BufferedOutputStream(out), false);
        _write(output);
        output.flush();
        output.close();
    }

    /** Zoom in or out to the specified rectangle.     *  This method calls repaint().     *  @param lowx The low end of the new X range.     *  @param lowy The low end of the new Y range.     *  @param highx The high end of the new X range.     *  @param highy The high end of the new Y range.     */
    public synchronized void zoom(double lowx, double lowy, double highx, double highy) {
        setXRange(lowx, highx);
        setYRange(lowy, highy);
        repaint();
    }

    /** Draw the axes using the current range, label, and title information.     *  If the second argument is true, clear the display before redrawing.     *  This method is called by paint().  To cause it to be called you     *  would normally call repaint(), which eventually causes paint() to     *  be called.     *  @param graphics The graphics context.     *  @param clearfirst If true, clear the plot before proceeding.     */
    protected synchronized void _drawPlot(Graphics graphics, boolean clearfirst) {
        if (graphics == null) {
            throw new RuntimeException("PlotBox._drawPlot: Attempt to draw axes" + " without a Graphics object.");
        }
        Rectangle drawRect = getBounds();
        graphics.setPaintMode();
        if (clearfirst) {
            graphics.clearRect(0, 0, drawRect.width, drawRect.height);
            graphics.setColor(Color.white);
            graphics.fillRect(0, 0, drawRect.width, drawRect.height);
        }
        if (_errorMsg != null) {
            int fheight = _labelFontMetrics.getHeight() + 2;
            int msgy = fheight;
            graphics.setColor(Color.black);
            for (int i = 0; i < _errorMsg.length; i++) {
                graphics.drawString(_errorMsg[i], 10, msgy);
                msgy += fheight;
                System.err.println(_errorMsg[i]);
            }
            return;
        }
        if (!_xRangeGiven) {
            if (_xBottom > _xTop) {
                _setXRange(0, 0);
            } else {
                _setXRange(_xBottom, _xTop);
            }
        }
        if (!_yRangeGiven) {
            if (_yBottom > _yTop) {
                _setYRange(0, 0);
            } else {
                _setYRange(_yBottom, _yTop);
            }
        }
        int titley = 0;
        int titlefontheight = _titleFontMetrics.getHeight();
        if (_title == null) {
            _title = "";
        }
        if (_title != null || _yExp != 0) {
            titley = titlefontheight + _topPadding;
        }
        graphics.setFont(_labelfont);
        int labelheight = _labelFontMetrics.getHeight();
        int halflabelheight = labelheight / 2;
        int ySPos = drawRect.height - 5;
        int xSPos = drawRect.width - _rightPadding;
        if (_xlog) _xExp = (int) Math.floor(_xtickMin);
        if (_xExp != 0 && _xticks == null) {
            String superscript = Integer.toString(_xExp);
            xSPos -= _superscriptFontMetrics.stringWidth(superscript);
            graphics.setFont(_superscriptfont);
            if (!_xlog && !_xtime) {
                graphics.drawString(superscript, xSPos, ySPos - halflabelheight);
                xSPos -= _labelFontMetrics.stringWidth("x10");
                graphics.setFont(_labelfont);
                graphics.drawString("x10", xSPos, ySPos);
            }
            _bottomPadding = (3 * labelheight) / 2 + 5;
        }
        if (_xlabel != null && _bottomPadding < labelheight + 5) {
            _bottomPadding = titlefontheight + 5;
        }
        _uly = titley + 5;
        _lry = drawRect.height - labelheight - _bottomPadding - 3;
        int height = _lry - _uly;
        _yscale = height / (_yMax - _yMin);
        _ytickscale = height / (_ytickMax - _ytickMin);
        int ny = 2 + height / (labelheight + 10);
        double yStep = _roundUp((_ytickMax - _ytickMin) / (double) ny);
        double yStart = yStep * Math.ceil(_ytickMin / yStep);
        int widesty = 0;
        String ylabels[] = new String[ny];
        int ylabwidth[] = new int[ny];
        int ind = 0;
        if (_yticks == null) {
            Vector ygrid = null;
            if (_ylog) {
                ygrid = _gridInit(yStart, yStep, true, null);
            }
            int numfracdigits = _numFracDigits(yStep);
            double yTmpStart = yStart;
            if (_ylog) yTmpStart = _gridStep(ygrid, yStart, yStep, _ylog);
            for (double ypos = yTmpStart; ypos <= _ytickMax; ypos = _gridStep(ygrid, ypos, yStep, _ylog)) {
                if (ind >= ny) break;
                String yticklabel;
                if (_ylog) {
                    yticklabel = _formatLogNum(ypos, numfracdigits);
                } else {
                    yticklabel = _formatNum(ypos, numfracdigits);
                }
                ylabels[ind] = yticklabel;
                int lw = _labelFontMetrics.stringWidth(yticklabel);
                ylabwidth[ind++] = lw;
                if (lw > widesty) {
                    widesty = lw;
                }
            }
        } else {
            Enumeration nl = _yticklabels.elements();
            while (nl.hasMoreElements()) {
                String label = (String) nl.nextElement();
                int lw = _labelFontMetrics.stringWidth(label);
                if (lw > widesty) {
                    widesty = lw;
                }
            }
        }
        if (_xtime) widesty = 25;
        if (_ylabel != null) {
            _ulx = widesty + _labelFontMetrics.stringWidth("W") + _leftPadding;
        } else {
            _ulx = widesty + _leftPadding;
        }
        int legendwidth = _drawLegend(graphics, drawRect.width - _rightPadding, _uly);
        _lrx = drawRect.width - legendwidth - _rightPadding;
        int width = _lrx - _ulx;
        _xscale = width / (_xMax - _xMin);
        _xtickscale = width / (_xtickMax - _xtickMin);
        graphics.setColor(Color.white);
        graphics.fillRect(_ulx, _uly, width, height);
        graphics.setColor(_foreground);
        graphics.drawRect(_ulx, _uly, width, height);
        int tickLength = 5;
        int xCoord1 = _ulx + tickLength;
        int xCoord2 = _lrx - tickLength;
        if (_yticks == null) {
            Vector ygrid = null;
            double yTmpStart = yStart;
            if (_ylog) {
                ygrid = _gridInit(yStart, yStep, true, null);
                yTmpStart = _gridStep(ygrid, yStart, yStep, _ylog);
                ny = ind;
            }
            ind = 0;
            boolean needExponent = _ylog;
            for (double ypos = yTmpStart; ypos <= _ytickMax; ypos = _gridStep(ygrid, ypos, yStep, _ylog)) {
                if (ind >= ny) break;
                int yCoord1 = _lry - (int) ((ypos - _ytickMin) * _ytickscale);
                int offset = 0;
                if (ind > 0 && !_ylog) offset = halflabelheight;
                graphics.drawLine(_ulx, yCoord1, xCoord1, yCoord1);
                graphics.drawLine(_lrx, yCoord1, xCoord2, yCoord1);
                if (_grid && yCoord1 != _uly && yCoord1 != _lry) {
                    graphics.setColor(Color.lightGray);
                    graphics.drawLine(xCoord1, yCoord1, xCoord2, yCoord1);
                    graphics.setColor(_foreground);
                }
                if (_ylog && ylabels[ind].indexOf('e') != -1) needExponent = false;
                graphics.drawString(ylabels[ind], _ulx - ylabwidth[ind++] - 4, yCoord1 + offset);
            }
            if (_ylog) {
                Vector unlabeledgrid = _gridInit(yStart, yStep, false, ygrid);
                if (unlabeledgrid.size() > 0) {
                    double tmpStep = (yStep > 1.0) ? 1.0 : yStep;
                    for (double ypos = _gridStep(unlabeledgrid, yStart, tmpStep, _ylog); ypos <= _ytickMax; ypos = _gridStep(unlabeledgrid, ypos, tmpStep, _ylog)) {
                        int yCoord1 = _lry - (int) ((ypos - _ytickMin) * _ytickscale);
                        if (_grid && yCoord1 != _uly && yCoord1 != _lry) {
                            graphics.setColor(Color.lightGray);
                            graphics.drawLine(_ulx + 1, yCoord1, _lrx - 1, yCoord1);
                            graphics.setColor(_foreground);
                        }
                    }
                }
                if (needExponent) {
                    _yExp = (int) Math.floor(yTmpStart);
                } else {
                    _yExp = 0;
                }
            }
            if (_yExp != 0) {
                graphics.drawString("x10", 2, titley);
                graphics.setFont(_superscriptfont);
                graphics.drawString(Integer.toString(_yExp), _labelFontMetrics.stringWidth("x10") + 2, titley - halflabelheight);
                graphics.setFont(_labelfont);
            }
        } else {
            Enumeration nt = _yticks.elements();
            Enumeration nl = _yticklabels.elements();
            while (nl.hasMoreElements()) {
                String label = (String) nl.nextElement();
                double ypos = ((Double) (nt.nextElement())).doubleValue();
                if (ypos > _yMax || ypos < _yMin) continue;
                int yCoord1 = _lry - (int) ((ypos - _yMin) * _yscale);
                int offset = 0;
                if (ypos < _lry - labelheight) offset = halflabelheight;
                graphics.drawLine(_ulx, yCoord1, xCoord1, yCoord1);
                graphics.drawLine(_lrx, yCoord1, xCoord2, yCoord1);
                if (_grid && yCoord1 != _uly && yCoord1 != _lry) {
                    graphics.setColor(Color.lightGray);
                    graphics.drawLine(xCoord1, yCoord1, xCoord2, yCoord1);
                    graphics.setColor(_foreground);
                }
                graphics.drawString(label, _ulx - _labelFontMetrics.stringWidth(label) - 3, yCoord1 + offset);
            }
        }
        int yCoord1 = _uly + tickLength;
        int yCoord2 = _lry - tickLength;
        if (_xticks == null) {
            int nx = 10;
            double xStep = 0.0;
            int numfracdigits = 0;
            int charwidth = _labelFontMetrics.stringWidth("8");
            if (_xlog) {
                nx = 2 + width / ((charwidth * 6) + 10);
            } else if (_xtime) {
                nx = 2 + width / ((charwidth * 12) + 10);
            } else {
                int count = 0;
                while (count++ <= 10) {
                    xStep = _roundUp((_xtickMax - _xtickMin) / (double) nx);
                    numfracdigits = _numFracDigits(xStep);
                    int intdigits = _numIntDigits(_xtickMax);
                    int inttemp = _numIntDigits(_xtickMin);
                    if (intdigits < inttemp) {
                        intdigits = inttemp;
                    }
                    int maxlabelwidth = charwidth * (numfracdigits + 2 + intdigits);
                    int savenx = nx;
                    nx = 2 + width / (maxlabelwidth + 10);
                    if (nx - savenx <= 1 || savenx - nx <= 1) break;
                }
            }
            if (_xtime) {
                xStep = _roundUpTime((_xtickMax - _xtickMin) / (double) nx);
                numfracdigits = 0;
            } else {
                xStep = _roundUp((_xtickMax - _xtickMin) / (double) nx);
                numfracdigits = _numFracDigits(xStep);
            }
            double xStart = xStep * Math.ceil(_xtickMin / xStep);
            Vector xgrid = null;
            double xTmpStart = xStart;
            if (_xlog) {
                xgrid = _gridInit(xStart, xStep, true, null);
                xTmpStart = _gridRoundUp(xgrid, xStart);
            }
            boolean needExponent = _xlog;
            for (double xpos = xTmpStart; xpos <= _xtickMax; xpos = _gridStep(xgrid, xpos, xStep, _xlog)) {
                String xticklabel;
                if (_xlog) {
                    xticklabel = _formatLogNum(xpos, numfracdigits);
                    if (xticklabel.indexOf('e') != -1) needExponent = false;
                } else if (_xtime) {
                    xticklabel = _formatTime((long) xpos);
                    needExponent = false;
                } else {
                    xticklabel = _formatNum(xpos, numfracdigits);
                }
                xCoord1 = _ulx + (int) ((xpos - _xtickMin) * _xtickscale);
                graphics.drawLine(xCoord1, _uly, xCoord1, yCoord1);
                graphics.drawLine(xCoord1, _lry, xCoord1, yCoord2);
                if (_grid && xCoord1 != _ulx && xCoord1 != _lrx) {
                    graphics.setColor(Color.lightGray);
                    graphics.drawLine(xCoord1, yCoord1, xCoord1, yCoord2);
                    graphics.setColor(_foreground);
                }
                int labxpos = xCoord1 - _labelFontMetrics.stringWidth(xticklabel) / 2;
                graphics.drawString(xticklabel, labxpos, _lry + 3 + labelheight);
            }
            if (_xlog) {
                double tmpStep = (xStep > 1.0) ? 1.0 : xStep;
                xTmpStart = tmpStep * Math.ceil(_xtickMin / tmpStep);
                Vector unlabeledgrid = _gridInit(xTmpStart, tmpStep, false, xgrid);
                if (unlabeledgrid.size() > 0) {
                    for (double xpos = _gridStep(unlabeledgrid, xTmpStart, tmpStep, _xlog); xpos <= _xtickMax; xpos = _gridStep(unlabeledgrid, xpos, tmpStep, _xlog)) {
                        xCoord1 = _ulx + (int) ((xpos - _xtickMin) * _xtickscale);
                        if (_grid && xCoord1 != _ulx && xCoord1 != _lrx) {
                            graphics.setColor(Color.lightGray);
                            graphics.drawLine(xCoord1, _uly + 1, xCoord1, _lry - 1);
                            graphics.setColor(_foreground);
                        }
                    }
                }
                if (needExponent) {
                    _xExp = (int) Math.floor(xTmpStart);
                    graphics.setFont(_superscriptfont);
                    graphics.drawString(Integer.toString(_xExp), xSPos, ySPos - halflabelheight);
                    xSPos -= _labelFontMetrics.stringWidth("x10");
                    graphics.setFont(_labelfont);
                    graphics.drawString("x10", xSPos, ySPos);
                } else {
                    _xExp = 0;
                }
            }
        } else {
            Enumeration nt = _xticks.elements();
            Enumeration nl = _xticklabels.elements();
            while (nl.hasMoreElements()) {
                String label = (String) nl.nextElement();
                double xpos = ((Double) (nt.nextElement())).doubleValue();
                if (xpos > _xMax || xpos < _xMin) continue;
                xCoord1 = _ulx + (int) ((xpos - _xMin) * _xscale);
                graphics.drawLine(xCoord1, _uly, xCoord1, yCoord1);
                graphics.drawLine(xCoord1, _lry, xCoord1, yCoord2);
                if (_grid && xCoord1 != _ulx && xCoord1 != _lrx) {
                    graphics.setColor(Color.lightGray);
                    graphics.drawLine(xCoord1, yCoord1, xCoord1, yCoord2);
                    graphics.setColor(_foreground);
                }
                int labxpos = xCoord1 - _labelFontMetrics.stringWidth(label) / 2;
                graphics.drawString(label, labxpos, _lry + 3 + labelheight);
            }
        }
        graphics.setColor(_foreground);
        if (_title != null) {
            graphics.setFont(_titlefont);
            int titlex = _ulx + (width - _titleFontMetrics.stringWidth(_title)) / 2;
            graphics.drawString(_title, titlex, titley);
        }
        graphics.setFont(_labelfont);
        if (_xlabel != null) {
            int labelx = _ulx + (width - _labelFontMetrics.stringWidth(_xlabel)) / 2;
            graphics.drawString(_xlabel, labelx, ySPos);
        }
        int charcenter = 2 + _labelFontMetrics.stringWidth("W") / 2;
        int charheight = labelheight;
        if (_ylabel != null) {
            int yl = _ylabel.length();
            int starty = _uly + (_lry - _uly) / 2 - yl * charheight / 2 + charheight;
            for (int i = 0; i < yl; i++) {
                String nchar = _ylabel.substring(i, i + 1);
                int cwidth = _labelFontMetrics.stringWidth(nchar);
                graphics.drawString(nchar, charcenter - cwidth / 2, starty);
                starty += charheight;
            }
        }
    }

    /** Put a mark corresponding to the specified dataset at the     *  specified x and y position.   The mark is drawn in the     *  current color.  In this base class, a point is a     *  filled rectangle 6 pixels across.  Note that marks greater than     *  about 6 pixels in size will not look very good since they will     *  overlap axis labels and may not fit well in the legend.   The     *  <i>clip</i> argument, if <code>true</code>, states     *  that the point should not be drawn if     *  it is out of range.     *  @param graphics The graphics context.     *  @param dataset The index of the data set.     *  @param xpos The X position.     *  @param ypos The Y position.     *  @param clip If true, do not draw if out of range.     */
    protected void _drawPoint(Graphics graphics, int dataset, long xpos, long ypos, boolean clip) {
        boolean pointinside = ypos <= _lry && ypos >= _uly && xpos <= _lrx && xpos >= _ulx;
        if (!pointinside && clip) {
            return;
        }
        graphics.fillRect((int) xpos - 6, (int) ypos - 6, 6, 6);
    }

    /** Parse a line that gives plotting information.  In this base     *  class, only lines pertaining to the title and labels are processed.     *  Everything else is ignored. Return true if the line is recognized.     *  @param line A line of text.     */
    protected boolean _parseLine(String line) {
        String lcLine = new String(line.toLowerCase());
        if (lcLine.startsWith("#")) {
            return true;
        } else if (lcLine.startsWith("titletext:")) {
            setTitle((line.substring(10)).trim());
            return true;
        } else if (lcLine.startsWith("xlabel:")) {
            setXLabel((line.substring(7)).trim());
            return true;
        } else if (lcLine.startsWith("ylabel:")) {
            setYLabel((line.substring(7)).trim());
            return true;
        } else if (lcLine.startsWith("xrange:")) {
            int comma = line.indexOf(",", 7);
            if (comma > 0) {
                String min = (line.substring(7, comma)).trim();
                String max = (line.substring(comma + 1)).trim();
                try {
                    Double dmin = new Double(min);
                    Double dmax = new Double(max);
                    setXRange(dmin.doubleValue(), dmax.doubleValue());
                } catch (NumberFormatException e) {
                }
            }
            return true;
        } else if (lcLine.startsWith("yrange:")) {
            int comma = line.indexOf(",", 7);
            if (comma > 0) {
                String min = (line.substring(7, comma)).trim();
                String max = (line.substring(comma + 1)).trim();
                try {
                    Double dmin = new Double(min);
                    Double dmax = new Double(max);
                    setYRange(dmin.doubleValue(), dmax.doubleValue());
                } catch (NumberFormatException e) {
                }
            }
            return true;
        } else if (lcLine.startsWith("xticks:")) {
            _parsePairs(line.substring(7), true);
            return true;
        } else if (lcLine.startsWith("yticks:")) {
            _parsePairs(line.substring(7), false);
            return true;
        } else if (lcLine.startsWith("xlog:")) {
            if (lcLine.indexOf("off", 5) >= 0) {
                _xlog = false;
            } else {
                _xlog = true;
            }
            return true;
        } else if (lcLine.startsWith("ylog:")) {
            if (lcLine.indexOf("off", 5) >= 0) {
                _ylog = false;
            } else {
                _ylog = true;
            }
            return true;
        } else if (lcLine.startsWith("grid:")) {
            if (lcLine.indexOf("off", 5) >= 0) {
                _grid = false;
            } else {
                _grid = true;
            }
            return true;
        } else if (lcLine.startsWith("color:")) {
            if (lcLine.indexOf("off", 6) >= 0) {
                _usecolor = false;
            } else {
                _usecolor = true;
            }
            return true;
        }
        return false;
    }

    /** Set the visibility of the Fill button.     *  This is deprecated.  Use setButtons().     *  @deprecated     */
    protected void _setButtonsVisibility(boolean vis) {
        _fillButton.setVisible(vis);
    }

    /** Write plot information to the specified output stream.     *  Derived classes should override this method to first call     *  the parent class method, then add whatever additional information     *  they wish to add to the stream.     *  @param output A buffered print writer.     */
    protected void _write(PrintWriter output) {
        output.println("# Ptolemy plot, version 2.0");
        if (_title != null) output.println("TitleText: " + _title);
        if (_xlabel != null) output.println("XLabel: " + _xlabel);
        if (_ylabel != null) output.println("YLabel: " + _ylabel);
        if (_xRangeGiven) output.println("XRange: " + _xlowgiven + ", " + _xhighgiven);
        if (_yRangeGiven) output.println("YRange: " + _ylowgiven + ", " + _yhighgiven);
        if (_xticks != null && _xticks.size() > 0) {
            output.print("XTicks: ");
            int last = _xticks.size() - 1;
            for (int i = 0; i < last; i++) {
                output.print("\"" + (String) _xticklabels.elementAt(i) + "\" " + (Double) _xticks.elementAt(i) + ", ");
            }
            output.println("\"" + (String) _xticklabels.elementAt(last) + "\" " + (Double) _xticks.elementAt(last));
        }
        if (_yticks != null && _yticks.size() > 0) {
            output.print("YTicks: ");
            int last = _yticks.size() - 1;
            for (int i = 0; i < last; i++) {
                output.print("\"" + (String) _yticklabels.elementAt(i) + "\" " + (Double) _yticks.elementAt(i) + ", ");
            }
            output.println("\"" + (String) _yticklabels.elementAt(last) + "\" " + (Double) _yticks.elementAt(last));
        }
        if (_xlog) output.println("XLog: on");
        if (_ylog) output.println("YLog: on");
        if (!_grid) output.println("Grid: off");
        if (!_usecolor) output.println("Color: off");
    }

    protected transient double _yMax = 0, _yMin = 0, _xMax = 0, _xMin = 0;

    protected static final transient double _PADDING = 0.05;

    protected transient boolean _xRangeGiven = false;

    protected transient boolean _yRangeGiven = false;

    protected double _xBottom = Double.MAX_VALUE;

    protected double _xTop = -Double.MAX_VALUE;

    protected double _yBottom = Double.MAX_VALUE;

    protected double _yTop = -Double.MAX_VALUE;

    protected boolean _xlog = false, _ylog = false;

    protected boolean _xtime = false;

    protected static final double _LOG10SCALE = 1 / Math.log(10);

    protected boolean _grid = true;

    /** @serial Color of the background, settable from HTML. */
    protected Color _background = Color.white;

    /** @serial Color of the foreground, settable from HTML. */
    protected Color _foreground = Color.black;

    protected int _topPadding = 10;

    protected int _bottomPadding = 5;

    protected int _rightPadding = 10;

    protected int _leftPadding = 10;

    /** @serial The plot rectangle in pixels.     * The naming convention is: "_ulx" = "upper left x", where "x" is     * the horizontal dimension.     */
    protected int _ulx = 1, _uly = 1, _lrx = 100, _lry = 100;

    /** @serial Scaling used in plotting points. */
    protected double _yscale = 1.0, _xscale = 1.0;

    protected boolean _usecolor = true;

    protected static Color[] _colors = { new Color(0xff0000), new Color(0x0000ff), new Color(0x00aaaa), new Color(0x000000), new Color(0xffa500), new Color(0x53868b), new Color(0xff7f50), new Color(0x45ab1f), new Color(0x90422d), new Color(0xa0a0a0), new Color(0x14ff14) };

    protected int _width = 400, _height = 400;

    protected URL _documentBase = null;

    private int _drawLegend(Graphics graphics, int urx, int ury) {
        graphics.setFont(_labelfont);
        int spacing = _labelFontMetrics.getHeight();
        Enumeration v = _legendStrings.elements();
        Enumeration i = _legendDatasets.elements();
        int ypos = ury + spacing;
        int maxwidth = 0;
        while (v.hasMoreElements()) {
            String legend = (String) v.nextElement();
            int dataset = ((Integer) i.nextElement()).intValue();
            if (dataset >= 0) {
                if (_usecolor) {
                    int color = dataset % _colors.length;
                    graphics.setColor(_colors[color]);
                    if (currentColors[color] != null) {
                        graphics.setColor(currentColors[color]);
                    }
                }
                _drawPoint(graphics, dataset, urx - 3, ypos - 3, false);
                graphics.setColor(_foreground);
                int width = _labelFontMetrics.stringWidth(legend);
                if (width > maxwidth) maxwidth = width;
                graphics.drawString(legend, urx - 15 - width, ypos);
                ypos += spacing;
            }
        }
        return 22 + maxwidth;
    }

    private String _formatLogNum(double num, int numfracdigits) {
        String results;
        int exponent = (int) num;
        if (exponent >= 0 && exponent < 10) {
            results = "0" + exponent;
        } else {
            if (exponent < 0 && exponent > -10) {
                results = "-0" + (-exponent);
            } else {
                results = Integer.toString(exponent);
            }
        }
        if (num >= 0.0) {
            if (num - (int) (num) < 0.001) {
                results = "1e" + results;
            } else {
                results = _formatNum(Math.pow(10.0, (num - (int) num)), numfracdigits);
            }
        } else {
            if (-num - (int) (-num) < 0.001) {
                results = "1e" + results;
            } else {
                results = _formatNum(Math.pow(10.0, (num - (int) num)) * 10, numfracdigits);
            }
        }
        return results;
    }

    private String _formatTime(long time) {
        if (_timeFormatDay == null) {
            _timeZone = new SimpleTimeZone(0, "GMT");
            _Calendar = new GregorianCalendar(_timeZone);
            _timeFormatDay = new SimpleDateFormat("yyyy-MM-dd");
            _timeFormatHour = new SimpleDateFormat("HH:mm");
            _timeFormatDay.setTimeZone(_timeZone);
            _timeFormatDay.setCalendar(_Calendar);
            _timeFormatHour.setTimeZone(_timeZone);
            _timeFormatHour.setCalendar(_Calendar);
        }
        Date fd = new Date(time);
        String hours = _timeFormatHour.format(fd);
        if (hours.equals("00:00")) return _timeFormatDay.format(fd); else return hours;
    }

    private String _formatNum(double num, int numfracdigits) {
        if (_numberFormat == null) {
            _numberFormat = NumberFormat.getInstance();
        }
        _numberFormat.setMinimumFractionDigits(numfracdigits);
        _numberFormat.setMaximumFractionDigits(numfracdigits);
        return _numberFormat.format(num);
    }

    private Vector _gridInit(double low, double step, boolean labeled, Vector oldgrid) {
        Vector grid = new Vector(10);
        double ratio = Math.pow(10.0, step);
        int ngrid = 1;
        if (labeled) {
            if (ratio <= 3.5) {
                if (ratio > 2.0) ngrid = 2; else if (ratio > 1.26) ngrid = 5; else if (ratio > 1.125) ngrid = 10; else ngrid = (int) Math.rint(1.0 / step);
            }
        } else {
            if (ratio > 10.0) ngrid = 1; else if (ratio > 3.0) ngrid = 2; else if (ratio > 2.0) ngrid = 5; else if (ratio > 1.125) ngrid = 10; else ngrid = 100;
        }
        int oldgridi = 0;
        for (int i = 0; i < ngrid; i++) {
            double gridval = i * 1.0 / ngrid * 10;
            double logval = _LOG10SCALE * Math.log(gridval);
            if (logval == Double.NEGATIVE_INFINITY) logval = 0.0;
            if (oldgrid != null && oldgridi < oldgrid.size()) {
                while (oldgridi < oldgrid.size() && ((Double) oldgrid.elementAt(oldgridi)).doubleValue() < logval) {
                    oldgridi++;
                }
                if (oldgridi < oldgrid.size()) {
                    if (Math.abs(((Double) oldgrid.elementAt(oldgridi)).doubleValue() - logval) > 0.00001) {
                        grid.addElement(new Double(logval));
                    }
                } else {
                    grid.addElement(new Double(logval));
                }
            } else {
                grid.addElement(new Double(logval));
            }
        }
        _gridCurJuke = 0;
        if (low == -0.0) low = 0.0;
        _gridBase = Math.floor(low);
        double x = low - _gridBase;
        for (_gridCurJuke = -1; (_gridCurJuke + 1) < grid.size() && x >= ((Double) grid.elementAt(_gridCurJuke + 1)).doubleValue(); _gridCurJuke++) {
        }
        return grid;
    }

    private double _gridRoundUp(Vector grid, double pos) {
        double x = pos - Math.floor(pos);
        int i;
        for (i = 0; i < grid.size() && x >= ((Double) grid.elementAt(i)).doubleValue(); i++) {
        }
        if (i >= grid.size()) return pos; else return Math.floor(pos) + ((Double) grid.elementAt(i)).doubleValue();
    }

    private double _gridStep(Vector grid, double pos, double step, boolean logflag) {
        if (logflag) {
            if (++_gridCurJuke >= grid.size()) {
                _gridCurJuke = 0;
                _gridBase += Math.ceil(step);
            }
            if (_gridCurJuke >= grid.size()) return pos + step;
            return _gridBase + ((Double) grid.elementAt(_gridCurJuke)).doubleValue();
        } else {
            return pos + step;
        }
    }

    private void _measureFonts() {
        if (_labelfont == null) _labelfont = new Font("Helvetica", Font.PLAIN, 12);
        if (_superscriptfont == null) _superscriptfont = new Font("Helvetica", Font.PLAIN, 9);
        if (_titlefont == null) _titlefont = new Font("Helvetica", Font.BOLD, 14);
        _labelFontMetrics = getFontMetrics(_labelfont);
        _superscriptFontMetrics = getFontMetrics(_superscriptfont);
        _titleFontMetrics = getFontMetrics(_titlefont);
    }

    private int _numFracDigits(double num) {
        int numdigits = 0;
        while (numdigits <= 15 && num != Math.floor(num)) {
            num *= 10.0;
            numdigits += 1;
        }
        return numdigits;
    }

    private int _numIntDigits(double num) {
        int numdigits = 0;
        while (numdigits <= 15 && (int) num != 0.0) {
            num /= 10.0;
            numdigits += 1;
        }
        return numdigits;
    }

    private void _parsePairs(String line, boolean xtick) {
        int start = 0;
        boolean cont = true;
        while (cont) {
            int comma = line.indexOf(",", start);
            String pair = null;
            if (comma > start) {
                pair = (line.substring(start, comma)).trim();
            } else {
                pair = (line.substring(start)).trim();
                cont = false;
            }
            int close = -1;
            int open = 0;
            if (pair.startsWith("\"")) {
                close = pair.indexOf("\"", 1);
                open = 1;
            } else {
                close = pair.indexOf(" ");
            }
            if (close > 0) {
                String label = pair.substring(open, close);
                String index = (pair.substring(close + 1)).trim();
                try {
                    double idx = (Double.valueOf(index)).doubleValue();
                    if (xtick) addXTick(label, idx); else addYTick(label, idx);
                } catch (NumberFormatException e) {
                }
            }
            start = comma + 1;
            comma = line.indexOf(",", start);
        }
    }

    private double _roundUp(double val) {
        int exponent = (int) Math.floor(Math.log(val) * _LOG10SCALE);
        val *= Math.pow(10, -exponent);
        if (val > 5.0) val = 10.0; else if (val > 2.0) val = 5.0; else if (val > 1.0) val = 2.0;
        val *= Math.pow(10, exponent);
        return val;
    }

    private double _roundUpTime(double val) {
        long MSECS_IN_DAY = 86400000;
        int ndays = (int) Math.ceil(val / MSECS_IN_DAY);
        int nhours = (int) Math.ceil(val / 3600000);
        if (nhours > 12) {
            return ndays * MSECS_IN_DAY;
        } else if (nhours > 6) {
            return 12 * 3600000;
        } else if (nhours > 3) {
            return 6 * 3600000;
        } else if (nhours > 1) {
            return 3 * 3600000;
        }
        return 1 * 3600000;
    }

    private void _setXRange(double min, double max) {
        if (min > max) {
            min = -1.0;
            max = 1.0;
        } else if (min == max) {
            min -= 1.0;
            max += 1.0;
        }
        _xMin = min - ((max - min) * _PADDING);
        _xMax = max + ((max - min) * _PADDING);
        double largest = Math.max(Math.abs(_xMin), Math.abs(_xMax));
        _xExp = (int) Math.floor(Math.log(largest) * _LOG10SCALE);
        if (_xtime) _xExp = 1;
        if (_xExp > 1 || _xExp < -1) {
            double xs = 1.0 / Math.pow(10.0, (double) _xExp);
            _xtickMin = _xMin * xs;
            _xtickMax = _xMax * xs;
        } else {
            _xtickMin = _xMin;
            _xtickMax = _xMax;
            _xExp = 0;
        }
    }

    private void _setYRange(double min, double max) {
        if (min > max) {
            min = -1.0;
            max = 1.0;
        } else if (min == max) {
            min -= 0.1;
            max += 0.1;
        }
        _yMin = min;
        _yMax = max;
        double largest = Math.max(Math.abs(_yMin), Math.abs(_yMax));
        _yExp = (int) Math.floor(Math.log(largest) * _LOG10SCALE);
        if (_yExp > 1 || _yExp < -1) {
            double ys = 1.0 / Math.pow(10.0, (double) _yExp);
            _ytickMin = _yMin * ys;
            _ytickMax = _yMax * ys;
        } else {
            _ytickMin = _yMin;
            _ytickMax = _yMax;
            _yExp = 0;
        }
    }

    public synchronized void _zoom(int x, int y) {
        Graphics graphics = getGraphics();
        boolean handled = false;
        if ((_zoomin == true) && (_drawn == true)) {
            if (_zoomxn != -1 || _zoomyn != -1) {
                int minx = Math.min(_zoomx, _zoomxn);
                int maxx = Math.max(_zoomx, _zoomxn);
                int miny = Math.min(_zoomy, _zoomyn);
                int maxy = Math.max(_zoomy, _zoomyn);
                graphics.setXORMode(_background);
                graphics.drawRect(minx, miny, maxx - minx, maxy - miny);
                graphics.setPaintMode();
                if (y > _lry) y = _lry;
                if (y < _uly) y = _uly;
                if (x > _lrx) x = _lrx;
                if (x < _ulx) x = _ulx;
                if ((Math.abs(_zoomx - x) > 5) && (Math.abs(_zoomy - y) > 5)) {
                    double a = _xMin + (_zoomx - _ulx) / _xscale;
                    double b = _xMin + (x - _ulx) / _xscale;
                    if (a < b) setXRange(a, b); else setXRange(b, a);
                    a = _yMax - (_zoomy - _uly) / _yscale;
                    b = _yMax - (y - _uly) / _yscale;
                    if (a < b) setYRange(a, b); else setYRange(b, a);
                }
                repaint();
                handled = true;
            }
        } else if ((_zoomout == true) && (_drawn == true)) {
            graphics.setXORMode(_background);
            int x_diff = Math.abs(_zoomx - _zoomxn);
            int y_diff = Math.abs(_zoomy - _zoomyn);
            graphics.drawRect(_zoomx - 15 - x_diff, _zoomy - 15 - y_diff, 30 + x_diff * 2, 30 + y_diff * 2);
            graphics.setPaintMode();
            double a = (double) (Math.abs(_zoomx - x)) / 30.0;
            double b = (double) (Math.abs(_zoomy - y)) / 30.0;
            double newx1 = _xMax + (_xMax - _xMin) * a;
            double newx2 = _xMin - (_xMax - _xMin) * a;
            double newy1 = _yMax + (_yMax - _yMin) * b;
            double newy2 = _yMin - (_yMax - _yMin) * b;
            zoom(newx2, newy2, newx1, newy1);
            handled = true;
        } else if (_drawn == false) {
            repaint();
            handled = true;
        }
        _drawn = false;
        _zoomin = _zoomout = false;
        _zoomxn = _zoomyn = _zoomx = _zoomy = -1;
    }

    public synchronized void _zoomBox(int x, int y) {
        Graphics graphics = getGraphics();
        if (y > _lry) y = _lry;
        if (y < _uly) y = _uly;
        if (x > _lrx) x = _lrx;
        if (x < _ulx) x = _ulx;
        if ((_zoomx != -1 || _zoomy != -1)) {
            if (_zoomin == false && _zoomout == false) {
                if (y < _zoomy) {
                    _zoomout = true;
                    graphics.drawRect(_zoomx - 15, _zoomy - 15, 30, 30);
                } else if (y > _zoomy) {
                    _zoomin = true;
                }
            }
            if (_zoomin == true) {
                graphics.setXORMode(_background);
                if ((_zoomxn != -1 || _zoomyn != -1) && (_drawn == true)) {
                    int minx = Math.min(_zoomx, _zoomxn);
                    int maxx = Math.max(_zoomx, _zoomxn);
                    int miny = Math.min(_zoomy, _zoomyn);
                    int maxy = Math.max(_zoomy, _zoomyn);
                    graphics.drawRect(minx, miny, maxx - minx, maxy - miny);
                }
                if (y > _zoomy) {
                    _zoomxn = x;
                    _zoomyn = y;
                    int minx = Math.min(_zoomx, _zoomxn);
                    int maxx = Math.max(_zoomx, _zoomxn);
                    int miny = Math.min(_zoomy, _zoomyn);
                    int maxy = Math.max(_zoomy, _zoomyn);
                    graphics.drawRect(minx, miny, maxx - minx, maxy - miny);
                    graphics.setPaintMode();
                    _drawn = true;
                    return;
                } else _drawn = false;
            } else if (_zoomout == true) {
                graphics.setXORMode(_background);
                if ((_zoomxn != -1 || _zoomyn != -1) && (_drawn == true)) {
                    int x_diff = Math.abs(_zoomx - _zoomxn);
                    int y_diff = Math.abs(_zoomy - _zoomyn);
                    graphics.drawRect(_zoomx - 15 - x_diff, _zoomy - 15 - y_diff, 30 + x_diff * 2, 30 + y_diff * 2);
                }
                if (y < _zoomy) {
                    _zoomxn = x;
                    _zoomyn = y;
                    int x_diff = Math.abs(_zoomx - _zoomxn);
                    int y_diff = Math.abs(_zoomy - _zoomyn);
                    graphics.drawRect(_zoomx - 15 - x_diff, _zoomy - 15 - y_diff, 30 + x_diff * 2, 30 + y_diff * 2);
                    graphics.setPaintMode();
                    _drawn = true;
                    return;
                } else _drawn = false;
            }
        }
        graphics.setPaintMode();
    }

    public synchronized void _zoomStart(int x, int y) {
        if (y > _lry) y = _lry;
        if (y < _uly) y = _uly;
        if (x > _lrx) x = _lrx;
        if (x < _ulx) x = _ulx;
        _zoomx = x;
        _zoomy = y;
    }

    /** @serial The file to be opened. */
    private String _filespec = null;

    /** @serial The given X and Y ranges. */
    private double _xlowgiven, _xhighgiven, _ylowgiven, _yhighgiven;

    /** @serial Set to true if we are reading in pxgraph format binary data.     * @deprecated     */
    private boolean _binary = false;

    /** @serial The range of the plot as labeled     * (multiply by 10^exp for actual range.     */
    private double _ytickMax = 0.0, _ytickMin = 0.0, _xtickMax = 0.0, _xtickMin = 0.0;

    /** @serial The power of ten by which the range numbers should     *  be multiplied.     */
    private int _yExp = 0, _xExp = 0;

    /** @serial Scaling used in making tick marks. */
    private double _ytickscale = 0.0, _xtickscale = 0.0;

    /** @serial Font information. */
    private Font _labelfont = null, _superscriptfont = null, _titlefont = null;

    /** @serial FontMetric information. */
    private FontMetrics _labelFontMetrics = null, _superscriptFontMetrics = null, _titleFontMetrics = null;

    private transient NumberFormat _numberFormat = null;

    private transient SimpleTimeZone _timeZone = null;

    private transient GregorianCalendar _Calendar = null;

    private transient SimpleDateFormat _timeFormatDay = null;

    private transient SimpleDateFormat _timeFormatHour = null;

    private transient int _gridCurJuke = 0;

    private transient double _gridBase = 0.0;

    private transient String _errorMsg[];

    /** @serial The title and label strings. */
    private String _xlabel, _ylabel, _title;

    /** @serial Legend information. */
    private Vector _legendStrings = new Vector(), _legendDatasets = new Vector();

    /** @serial If XTicks or YTicks are given/ */
    private Vector _xticks = null, _xticklabels = null, _yticks = null, _yticklabels = null;

    private transient Button _fillButton = null;

    private transient int _zoomx = -1;

    private transient int _zoomy = -1;

    private transient int _zoomxn = -1;

    private transient int _zoomyn = -1;

    private transient boolean _zoomin = false;

    private transient boolean _zoomout = false;

    private transient boolean _drawn = false;

    class FillButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent event) {
            fillPlot();
        }
    }

    public class ZoomListener implements MouseListener {

        public void mouseClicked(MouseEvent event) {
        }

        public void mouseEntered(MouseEvent event) {
        }

        public void mouseExited(MouseEvent event) {
        }

        public void mousePressed(MouseEvent event) {
            PlotBox.this._zoomStart(event.getX(), event.getY());
        }

        public void mouseReleased(MouseEvent event) {
            PlotBox.this._zoom(event.getX(), event.getY());
        }
    }

    public class DragListener implements MouseMotionListener {

        public void mouseDragged(MouseEvent event) {
            PlotBox.this._zoomBox(event.getX(), event.getY());
        }

        public void mouseMoved(MouseEvent event) {
        }
    }

    public synchronized void update(Observable obs, Object ob) {
        if (!(ob instanceof PlotRegistryEvent)) return;
        PlotRegistryEvent evt = (PlotRegistryEvent) ob;
        _xRangeGiven = true;
        _xlowgiven = evt.minX;
        _xhighgiven = evt.maxX;
        _setXRange(evt.minX, evt.maxX);
        repaint();
    }
}
