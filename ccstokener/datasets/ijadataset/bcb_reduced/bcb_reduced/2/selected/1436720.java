package spidr.applets.ptolemy.plot;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.net.*;

/** * A flexible signal plotter.  The plot can be configured and data can * be provided either through a file with commands or through direct * invocation of the public methods of the class.  To read a file or a * URL, use the read() method. * <p> * When calling the public methods, in most cases the changes will not * be visible until paint() has been called.  To request that this * be done, call repaint().  One exception is addPoint(), which * makes the new point visible immediately if the plot is visible on * the screen. * <p> * For backwards compatibility with an older program, pxgraph, the * readPxgraph() method reads pxgraph-compatible * binary files.  Since those binary * files have no format information, and format information in pxgraph is * provided by command line arguments, a method parsePxgraphargs() is * provided to interpret the pxgraph-style command-line arguments. * <p> * The ASCII format for the file * file contains any number commands, * one per line.  Unrecognized commands and commands with syntax * errors are ignored.  Comments are denoted by a line starting with a * pound sign "#".  The recognized commands include those supported by * the base class, plus a few more.  The commands are case * insensitive, but are usually capitalized.  The number of data sets * to be plotted does not need to be specified.  Data sets are added as needed. * Each dataset can be optionally identified with * color (see the base class) or with unique marks.  The style of * marks used to denote a data point is defined by one of the following * commands: * <pre> * Marks: none * Marks: points * Marks: dots * Marks: various * </pre> * Here, "points" are small dots, while "dots" are larger.  If "various" * is specified, then unique marks are used for the first ten data sets, * and then recycled. * Using no marks is useful when lines connect the points in a plot, * which is done by default. * If the above directive appears before any DataSet directive, then it * specifies the default for all data sets.  If if appears after a DataSet * directive, then it applies only to that data set. * <p> * To disable connecting lines, use: * <pre> * Lines: off * </pre> * To reenable them, use * <pre> * Lines: on * </pre> * You can also specify "impulses", which are lines drawn from a plotted point * down to the x axis.  Plots with impulses are often called "stem plots." * These are off by default, but can be turned on with the * command: * <pre> * Impulses: on * </pre> * or back off with the command * <pre> * Impulses: off * </pre> * If that command appears before any DataSet directive, then the command * applies to all data sets.  Otherwise, it applies only to the current data * set. * To create a bar graph, turn off lines and use any of the following commands: * <pre> * Bars: on * Bars: <i>width</i> * Bars: <i>width, offset</i> * </pre> * The <i>width</i> is a real number specifying the width of the bars * in the units of the x axis.  The <i>offset</i> is a real number * specifying how much the bar of the <i>i</i><sup>th</sup> data set * is offset from the previous one.  This allows bars to "peek out" * from behind the ones in front.  Note that the frontmost data set * will be the last one.  To turn off bars, use * <pre> * Bars: off * </pre> * To specify data to be plotted, start a data set with the following command: * <pre> * DataSet: <i>string</i> * </pre> * Here, <i>string</i> is a label that will appear in the legend. * It is not necessary to enclose the string in quotation marks. * To start a new dataset without giving it a name, use: * <pre> * DataSet: * </pre> * In this case, no item will appear in the legend. * New datasets are plotted <i>behind</i> the previous ones. * If the following directive occurs: * <pre> * ReuseDataSets: on * </pre> * Then datasets with the same name will be merged.  This makes it * easier to combine multiple datafiles that contain the same datasets * into one file.  By default, this capability is turned off, so * datasets with the same name are not merged. * The data itself is given by a sequence of commands with one of the * following forms: * <pre> * <i>x</i>, <i>y</i> * draw: <i>x</i>, <i>y</i> * move: <i>x</i>, <i>y</i> * <i>x</i>, <i>y</i>, <i>yLowErrorBar</i>, <i>yHighErrorBar</i> * draw: <i>x</i>, <i>y</i>, <i>yLowErrorBar</i>, <i>yHighErrorBar</i> * move: <i>x</i>, <i>y</i>, <i>yLowErrorBar</i>, <i>yHighErrorBar</i> * </pre> * The "draw" command is optional, so the first two forms are equivalent. * The "move" command causes a break in connected points, if lines are * being drawn between points. The numbers <i>x</i> and <i>y</i> are * arbitrary numbers as supported by the Double parser in Java. * If there are four numbers, then the last two numbers are assumed to * be the lower and upper values for error bars. * The numbers can be separated by commas, spaces or tabs. * <p> * This plotter has some <A NAME="ptplot limitations">limitations</a>: * <ul> * <li> If you zoom in far enough, the plot becomes unreliable. *      In particular, if the total extent of the plot is more than *      2<sup>32</sup> times extent of the visible area, quantization *      errors can result in displaying points or lines. *      Note that 2<sup>32</sup> is over 4 billion. * <li> The limitations of the log axis facility are listed in *      the <code>_gridInit()</code> method in the PlotBox class. * <li> The compatibility issues of the <code>pxgraph</code> script are *      list in the *<a href="pt.plot.Pxgraph.html#pxgraph script compatibility issues">Pxgraph</a> *      class. * </ul> * * @author Edward A. Lee, Christopher Hylands * @version $Id: Plot.java,v 1.2 2009-10-20 13:11:48 dmedv Exp $ */
public class Plot extends PlotBox {

    public Plot() {
        super();
    }

    public Plot(boolean useSync) {
        super(useSync);
    }

    /** Add a legend (displayed at the upper right) for the specified     *  data set with the specified string.  Short strings generally     *  fit better than long strings.     *  @param dataset The dataset index.     *  @param legend The label for the dataset.     */
    public void addLegend(int dataset, String legend) {
        _checkDatasetIndex(dataset);
        super.addLegend(dataset, legend);
    }

    /** In the specified data set, add the specified x, y point to the     *  plot.  Data set indices begin with zero.  If the data set     *  does not exist, create it.  The fourth argument indicates     *  whether the point should be connected by a line to the previous     *  point.  The new point will be made visible if the plot is visible     *  on the screen.  Otherwise, it will be drawn the next time the plot     *  is drawn on the screen.     *  @param dataset The data set index.     *  @param x The X position of the new point.     *  @param y The Y position of the new point.     *  @param connected If true, a line is drawn to connect to the previous     *   point.     */
    public synchronized void addPoint(int dataset, double x, double y, boolean connected) {
        if (_xlog) {
            if (x <= 0.0) {
                System.err.println("Can't plot non-positive X values " + "when the logarithmic X axis value is specified: " + x);
                return;
            }
            x = Math.log(x) * _LOG10SCALE;
        }
        if (_ylog) {
            if (y <= 0.0) {
                System.err.println("Can't plot non-positive Y values " + "when the logarithmic Y axis value is specified: " + y);
                return;
            }
            y = Math.log(y) * _LOG10SCALE;
        }
        _addPoint(dataset, x, y, 0, 0, connected, false);
    }

    /** In the specified data set, add the specified x, y point to the     *  plot with error bars.  Data set indices begin with zero.  If     *  the dataset does not exist, create it.  yLowEB and     *  yHighEB are the lower and upper error bars.  The sixth argument     *  indicates whether the point should be connected by a line to     *  the previous point.     *  The new point will be made visible if the plot is visible     *  on the screen.  Otherwise, it will be drawn the next time the plot     *  is drawn on the screen.     *  This method is based on a suggestion by     *  Michael Altmann <michael@email.labmed.umn.edu>.     *     *  @param dataset The data set index.     *  @param x The X position of the new point.     *  @param y The Y position of the new point.     *  @param yLowEB The low point of the error bar.     *  @param yHighEB The high point of the error bar.     *  @param connected If true, a line is drawn to connect to the previous     *   point.     */
    public synchronized void addPointWithErrorBars(int dataset, double x, double y, double yLowEB, double yHighEB, boolean connected) {
        if (_xlog) {
            if (x <= 0.0) {
                System.err.println("Can't plot non-positive X values " + "when the logarithmic X axis value is specified: " + x);
                return;
            }
            x = Math.log(x) * _LOG10SCALE;
        }
        if (_ylog) {
            if (y <= 0.0 || yLowEB <= 0.0 || yHighEB <= 0.0) {
                System.err.println("Can't plot non-positive Y values " + "when the logarithmic Y axis value is specified: " + y);
                return;
            }
            y = Math.log(y) * _LOG10SCALE;
            yLowEB = Math.log(yLowEB) * _LOG10SCALE;
            yHighEB = Math.log(yHighEB) * _LOG10SCALE;
        }
        _addPoint(dataset, x, y, yLowEB, yHighEB, connected, true);
    }

    /** Clear the plot of all data points.  If the argument is true, then     *  reset all parameters to their initial conditions, including     *  the persistence, plotting format, and axes formats.     *  For the change to take effect, you must call repaint().     *  @param format If true, clear the format controls as well.     */
    public synchronized void clear(boolean format) {
        super.clear(format);
        _currentdataset = -1;
        int size = _points.size();
        _points = new Vector();
        _prevx = new Vector();
        _prevy = new Vector();
        _painted = false;
        _maxdataset = -1;
        _firstinset = true;
        _sawfirstdataset = false;
        _pxgraphBlankLineMode = true;
        _endian = _NATIVE_ENDIAN;
        _xyInvalid = false;
        _filename = null;
        _showing = false;
        if (format) {
            _formats = new Vector();
            _marks = 0;
            _pointsPersistence = 0;
            _sweepsPersistence = 0;
            _bars = false;
            _barwidth = 0.5;
            _baroffset = 0.05;
            _connected = true;
            _impulses = false;
            _reusedatasets = false;
        }
    }

    /** Erase the point at the given index in the given dataset.  If     *  lines are being drawn, also erase the line to the next points     *  (note: not to the previous point).  The point is not checked to     *  see whether it is in range, so care must be taken by the caller     *  to ensure that it is.     *  The change will be made visible if the plot is visible     *  on the screen.  Otherwise, it will take effect the next time the plot     *  is drawn on the screen.     *     *  @param dataset The data set index.     *  @param index The index of the point to erase.     */
    public synchronized void erasePoint(int dataset, int index) {
        _checkDatasetIndex(dataset);
        if (isShowing()) {
            _erasePoint(getGraphics(), dataset, index);
        }
        Vector points = (Vector) _points.elementAt(dataset);
        if (points != null) {
            PlotPoint pt = (PlotPoint) points.elementAt(index);
            if (pt != null) {
                if (pt.x == _xBottom || pt.x == _xTop || pt.y == _yBottom || pt.y == _yTop) {
                    _xyInvalid = true;
                }
                points.removeElementAt(index);
            }
        }
    }

    /** Rescale so that the data that is currently plotted just fits.     *  This overrides the base class method to ensure that the protected     *  variables _xBottom, _xTop, _yBottom, and _yTop are valid.     *  This method calls repaint(), which eventually causes the display     *  to be updated.     */
    public synchronized void fillPlot() {
        if (_xyInvalid) {
            _xBottom = Double.MAX_VALUE;
            _xTop = -Double.MIN_VALUE;
            _yBottom = Double.MAX_VALUE;
            _yTop = -Double.MIN_VALUE;
            for (int dataset = 0; dataset < _points.size(); dataset++) {
                Vector points = (Vector) _points.elementAt(dataset);
                for (int index = 0; index < points.size(); index++) {
                    PlotPoint pt = (PlotPoint) points.elementAt(index);
                    if (pt.x < _xBottom) _xBottom = pt.x;
                    if (pt.x > _xTop) _xTop = pt.x;
                    if (pt.y < _yBottom) _yBottom = pt.y;
                    if (pt.y > _yTop) _yTop = pt.y;
                }
            }
        }
        _xyInvalid = false;
        if (_bars) {
            if (_yBottom > 0.0) _yBottom = 0.0;
            if (_yTop < 0.0) _yTop = 0.0;
        }
        super.fillPlot();
    }

    /** Return the last file name seen on the command-line arguments parsed     *  by parseArgs().  If there was none, return null.     *  @return A file name, or null if there is none.     */
    public String getCmdLineFilename() {
        return _filename;
    }

    /** Return the maximum number of data sets.     *  This method is deprecated, since there is no longer an upper bound.     *  @deprecated     */
    public int getMaxDataSets() {
        return Integer.MAX_VALUE;
    }

    /** Start a new thread to paint the component contents.     *  This is done in a new thread so that large data sets can     *  can be displayed without freezing the user interface.     *  When repainting is completed, the protected variable _painted     *  is set to true, and notifyAll() is called.     *  @param graphics The graphics context.     */
    public void paint(Graphics graphics) {
        _drawPlot(graphics, true);
    }

    /** Parse pxgraph style command line arguments.     *  This method exists only for backward compatibility with the X11 pxgraph     *  program.     *     *  @param args A set of command-line arguments.     *  @return The number of arguments read.     *  @exception CmdLineArgException If there is a problem parsing     *   the command line arguments.     *  @exception FileNotFoundException If a file is specified that is not     *   found.     *  @exception IOException If an error occurs reading an input file.     */
    public int parseArgs(String args[]) throws CmdLineArgException, FileNotFoundException, IOException {
        int i = 0, j, argsread = 0;
        boolean sawbararg = false;
        boolean sawnlarg = false;
        int savedmarks = 0;
        boolean binary = false;
        String arg;
        String unsupportedOptions[] = { "-bd", "-brb", "-bw", "-gw", "-lw", "-zg", "-zw" };
        while (i < args.length && (args[i].startsWith("-") || args[i].startsWith("="))) {
            arg = args[i++];
            if (arg.startsWith("-")) {
                boolean badarg = false;
                for (j = 0; j < unsupportedOptions.length; j++) {
                    if (arg.equals(unsupportedOptions[j])) {
                        System.err.println("pxgraph: " + arg + " is not yet supported");
                        i++;
                        badarg = true;
                    }
                }
                if (badarg) continue;
                if (arg.equals("-bb")) {
                    continue;
                } else if (arg.equals("-bg")) {
                    setBackground(getColorByName(args[i++]));
                    continue;
                } else if (arg.equals("-brw")) {
                    if (arg.indexOf(",") == -1) {
                        if (!_parseLine("Bars: " + args[i++] + ",0")) {
                            throw new CmdLineArgException("Failed to parse `" + arg + "'");
                        }
                    } else {
                        if (!_parseLine("Bars: " + args[i++])) {
                            throw new CmdLineArgException("Failed to parse `" + arg + "'");
                        }
                    }
                    continue;
                } else if (arg.equals("-lf")) {
                    setLabelFont(args[i++]);
                    continue;
                } else if (arg.equals("-lx")) {
                    if (!_parseLine("XRange: " + args[i++])) {
                        throw new CmdLineArgException("Failed to parse `" + arg + "'");
                    }
                    continue;
                } else if (arg.equals("-ly")) {
                    if (!_parseLine("YRange: " + args[i++])) {
                        throw new CmdLineArgException("Failed to parse `" + arg + "'");
                    }
                    continue;
                } else if (arg.equals("-t")) {
                    String title = args[i++];
                    setTitle(title);
                    continue;
                } else if (arg.equals("-tf")) {
                    setTitleFont(args[i++]);
                    continue;
                } else if (arg.equals("-x")) {
                    setXLabel(args[i++]);
                    continue;
                } else if (arg.equals("-y")) {
                    setYLabel(args[i++]);
                    continue;
                } else if (arg.equals("-bar")) {
                    sawbararg = true;
                    if (sawnlarg) {
                        setImpulses(true);
                    } else {
                        setBars(true);
                        savedmarks = _marks;
                        setMarksStyle("none");
                    }
                    setConnected(false);
                    continue;
                } else if (arg.equals("-binary")) {
                    binary = true;
                    _endian = _NATIVE_ENDIAN;
                    continue;
                } else if (arg.equals("-bigendian")) {
                    binary = true;
                    _endian = _BIG_ENDIAN;
                    continue;
                } else if (arg.equals("-littleendian")) {
                    binary = true;
                    _endian = _LITTLE_ENDIAN;
                    continue;
                } else if (arg.equals("-db")) {
                    continue;
                } else if (arg.equals("-debug")) {
                    continue;
                } else if (arg.equals("-fg")) {
                    setForeground(getColorByName(args[i++]));
                    continue;
                } else if (arg.equals("-help")) {
                    continue;
                } else if (arg.equals("-impulses")) {
                    setImpulses(true);
                    setConnected(false);
                    continue;
                } else if (arg.equals("-lnx")) {
                    setXLog(true);
                    continue;
                } else if (arg.equals("-lny")) {
                    setYLog(true);
                    continue;
                } else if (arg.equals("-m")) {
                    setMarksStyle("various");
                    continue;
                } else if (arg.equals("-M")) {
                    setMarksStyle("various");
                    continue;
                } else if (arg.equals("-nl")) {
                    sawnlarg = true;
                    if (sawbararg) {
                        _marks = savedmarks;
                        setBars(false);
                        setImpulses(true);
                    }
                    setConnected(false);
                    continue;
                } else if (arg.equals("-o")) {
                    i++;
                    continue;
                } else if (arg.equals("-p")) {
                    setMarksStyle("points");
                    continue;
                } else if (arg.equals("-P")) {
                    setMarksStyle("dots");
                    continue;
                } else if (arg.equals("-print")) {
                    continue;
                } else if (arg.equals("-rv")) {
                    setBackground(getColorByName("black"));
                    setForeground(getColorByName("white"));
                    continue;
                } else if (arg.equals("-test")) {
                    continue;
                } else if (arg.equals("-tk")) {
                    setGrid(false);
                    continue;
                } else if (arg.equals("-v") || arg.equals("-version")) {
                    continue;
                } else if (arg.length() > 1 && arg.charAt(0) == '-') {
                    try {
                        Integer datasetnumberint = new Integer(arg.substring(1));
                        int datasetnumber = datasetnumberint.intValue();
                        if (datasetnumber >= 0) {
                            addLegend(datasetnumber, args[i++]);
                            continue;
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            } else {
                if (arg.startsWith("=")) {
                    _width = (int) Integer.valueOf(arg.substring(1, arg.indexOf('x'))).intValue();
                    int plusIndex = arg.indexOf('+');
                    int minusIndex = arg.indexOf('-');
                    if (plusIndex != -1 || minusIndex != -1) {
                        if (plusIndex != -1 && minusIndex != -1) {
                            int index = minusIndex;
                            if (plusIndex < minusIndex) {
                                index = plusIndex;
                            }
                            _height = Integer.valueOf(arg.substring(arg.indexOf('x') + 1, index)).intValue();
                        } else {
                            if (plusIndex != -1) {
                                _height = Integer.valueOf(arg.substring(arg.indexOf('x') + 1, plusIndex)).intValue();
                            } else {
                                _height = Integer.valueOf(arg.substring(arg.indexOf('x') + 1, minusIndex)).intValue();
                            }
                        }
                    } else {
                        if (arg.length() > arg.indexOf('x')) {
                            _height = Integer.valueOf(arg.substring(arg.indexOf('x') + 1, arg.length())).intValue();
                        }
                    }
                    continue;
                }
            }
            throw new CmdLineArgException("Failed to parse `" + arg + "'");
        }
        argsread = i++;
        setSize(_width, _height);
        for (i = argsread; i < args.length; i++) {
            InputStream instream;
            try {
                URL inurl = new URL(_documentBase, args[i]);
                instream = inurl.openStream();
            } catch (MalformedURLException ex) {
                instream = new FileInputStream(args[i]);
                _filename = args[i];
            }
            if (binary) {
                readPxgraph(instream);
            } else {
                read(instream);
            }
        }
        return argsread;
    }

    /** Override the base class to indicate that a new data set is being read.     *  This method is deprecated.  Use read() or readPxgraph() instead.     *  @deprecated     */
    public void parseFile(String filespec, URL documentBase) {
        _firstinset = true;
        _sawfirstdataset = false;
        super.parseFile(filespec, documentBase);
    }

    /** Split a string containing pxgraph-compatible command-line arguments     *  into an array and call parseArgs() on the array.  This is used     *  in the rare circumstance that you want to control the format     *  of a plot from an applet HTML file rather than in the plot data     *  file.     *  @return The number of arguments read.     *  @exception CmdLineArgException If there is a problem parsing     *   the command line arguments.     *  @exception FileNotFoundException If a file is specified that is not     *   found.     *  @exception IOException If an error occurs reading an input file.     */
    public int parsePxgraphargs(String pxgraphargs) throws CmdLineArgException, FileNotFoundException, IOException {
        Vector argvector = new Vector();
        boolean prependdash = false;
        StringReader pin = new StringReader(pxgraphargs);
        try {
            StreamTokenizer stoken = new StreamTokenizer(pin);
            stoken.resetSyntax();
            stoken.whitespaceChars(0, ' ');
            stoken.wordChars('(', '~');
            stoken.quoteChar('"');
            stoken.quoteChar('\'');
            int c;
            String partialarg = null;
            out: while (true) {
                c = stoken.nextToken();
                switch(stoken.ttype) {
                    case StreamTokenizer.TT_EOF:
                        break out;
                    case StreamTokenizer.TT_WORD:
                        if (prependdash) {
                            prependdash = false;
                            if (partialarg == null) argvector.addElement(new String("-" + stoken.sval)); else argvector.addElement(new String("-" + partialarg + stoken.sval));
                        } else {
                            if (partialarg == null) argvector.addElement(new String(stoken.sval)); else argvector.addElement(new String(partialarg + stoken.sval));
                        }
                        partialarg = null;
                        break;
                    case '-':
                        prependdash = true;
                        break;
                    case '#':
                    case '$':
                    case '%':
                    case '&':
                        partialarg = ((String) argvector.lastElement()) + (char) c;
                        argvector.removeElementAt(argvector.size() - 1);
                        break;
                    case '"':
                    case '\'':
                        argvector.addElement(new String(stoken.sval));
                        break;
                    default:
                        throw new IOException("Failed to parse: '" + (char) c + "' in `" + pxgraphargs + "'");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String args[] = new String[argvector.size()];
        for (int i = 0; i < argvector.size(); i++) {
            args[i] = (String) argvector.elementAt(i);
        }
        return parseArgs(args);
    }

    /** Override the base class to register that we are reading a new     *  data set.     *  @param inputstream The input stream.     */
    public void read(InputStream in) throws IOException {
        super.read(in);
        _firstinset = true;
        _sawfirstdataset = false;
    }

    /** Read a pxgraph-compatible binary encoded file.     *  @param in The input stream.     *  @exception java.io.IOException If an I/O error occurs.     */
    public void readPxgraph(InputStream inputstream) throws IOException {
        Cursor oldCursor = getCursor();
        setCursor(new Cursor(Cursor.WAIT_CURSOR));
        try {
            _firstinset = true;
            _sawfirstdataset = false;
            DataInputStream in = new DataInputStream(new BufferedInputStream(inputstream));
            int c;
            float x = 0, y = 0, pointCount = 0;
            boolean byteSwapped = false;
            boolean connected = false;
            byte input[] = new byte[4];
            if (_connected) connected = true;
            switch(_endian) {
                case _NATIVE_ENDIAN:
                    try {
                        if (System.getProperty("os.arch").equals("x86")) {
                            byteSwapped = true;
                        }
                    } catch (SecurityException e) {
                    }
                    break;
                case _BIG_ENDIAN:
                    break;
                case _LITTLE_ENDIAN:
                    byteSwapped = true;
                    break;
                default:
                    throw new IOException("Internal Error: Don't know about '" + _endian + "' style of endian");
            }
            try {
                c = in.readByte();
                if (c != 'd') {
                    int bits = c;
                    bits = bits << 8;
                    bits += in.readByte();
                    bits = bits << 8;
                    bits += in.readByte();
                    bits = bits << 8;
                    bits += in.readByte();
                    x = Float.intBitsToFloat(bits);
                    y = in.readFloat();
                    connected = _addLegendIfNecessary(connected);
                    addPoint(_currentdataset, x, y, connected);
                    if (_connected) connected = true;
                    while (true) {
                        x = in.readFloat();
                        y = in.readFloat();
                        connected = _addLegendIfNecessary(connected);
                        addPoint(_currentdataset, x, y, connected);
                        if (_connected) connected = true;
                    }
                } else {
                    while (true) {
                        switch(c) {
                            case 'd':
                                if (byteSwapped) {
                                    in.readFully(input);
                                    x = Float.intBitsToFloat(((input[3] & 0xFF) << 24) | ((input[2] & 0xFF) << 16) | ((input[1] & 0xFF) << 8) | (input[0] & 0xFF));
                                    in.readFully(input);
                                    y = Float.intBitsToFloat(((input[3] & 0xFF) << 24) | ((input[2] & 0xFF) << 16) | ((input[1] & 0xFF) << 8) | (input[0] & 0xFF));
                                } else {
                                    x = in.readFloat();
                                    y = in.readFloat();
                                }
                                pointCount++;
                                connected = _addLegendIfNecessary(connected);
                                addPoint(_currentdataset, x, y, connected);
                                if (_connected) connected = true;
                                break;
                            case 'e':
                                connected = false;
                                break;
                            case 'n':
                                StringBuffer datasetname = new StringBuffer();
                                _firstinset = true;
                                _sawfirstdataset = true;
                                _currentdataset++;
                                if (_currentdataset >= _MAX_MARKS) _currentdataset = 0;
                                while (c != '\n') datasetname.append(in.readChar());
                                addLegend(_currentdataset, datasetname.toString());
                                setConnected(true);
                                break;
                            case 'm':
                                connected = false;
                                break;
                            default:
                                throw new IOException("Don't understand `" + (char) c + "' character " + "(decimal value = " + c + ") in binary file.  Last point was (" + x + "," + y + ").\nProcessed " + pointCount + " points successfully");
                        }
                        c = in.readByte();
                    }
                }
            } catch (EOFException e) {
            }
        } finally {
            setCursor(oldCursor);
        }
    }

    /** Turn bars on or off (for bar charts).     *  @param on If true, turn bars on.     */
    public void setBars(boolean on) {
        _bars = on;
    }

    /** Turn bars on and set the width and offset.  Both are specified     *  in units of the x axis.  The offset is the amount by which the     *  i<sup>th</sup> data set is shifted to the right, so that it     *  peeks out from behind the earlier data sets.     *  @param width The width of the bars.     *  @param offset The offset per data set.     */
    public void setBars(double width, double offset) {
        _barwidth = width;
        _baroffset = offset;
        _bars = true;
    }

    /** If the argument is true, then the default is to connect     *  subsequent points with a line.  If the argument is false, then     *  points are not connected.  When points are by default     *  connected, individual points can be not connected by giving the     *  appropriate argument to addPoint().     */
    public void setConnected(boolean on) {
        _connected = on;
    }

    /** If the argument is true, then a line will be drawn from any     *  plotted point down to the x axis.  Otherwise, this feature is     *  disabled.  A plot with such lines is also known as a stem plot.     *  @param on If true, draw a stem plot.     */
    public void setImpulses(boolean on) {
        _impulses = on;
    }

    /** If the first argument is true, then a line will be drawn from any     *  plotted point in the specified dataset down to the x axis.     *  Otherwise, this feature is     *  disabled.  A plot with such lines is also known as a stem plot.     *  @param on If true, draw a stem plot.     *  @param dataset The dataset to which this should apply.     */
    public void setImpulses(boolean on, int dataset) {
        _checkDatasetIndex(dataset);
        Format fmt = (Format) _formats.elementAt(dataset);
        fmt.impulses = on;
        fmt.impulsesUseDefault = false;
    }

    /** Set the marks style to "none", "points", "dots", or "various".     *  In the last case, unique marks are used for the first ten data     *  sets, then recycled.     *  @param style A string specifying the style for points.     */
    public void setMarksStyle(String style) {
        if (style.equalsIgnoreCase("none")) {
            _marks = 0;
        } else if (style.equalsIgnoreCase("points")) {
            _marks = 1;
        } else if (style.equalsIgnoreCase("dots")) {
            _marks = 2;
        } else if (style.equalsIgnoreCase("various")) {
            _marks = 3;
        }
    }

    /** Set the marks style to "none", "points", "dots", or "various"     *  for the specified dataset.     *  In the last case, unique marks are used for the first ten data     *  sets, then recycled.     *  @param style A string specifying the style for points.     *  @param dataset The dataset to which this should apply.     */
    public void setMarksStyle(String style, int dataset) {
        _checkDatasetIndex(dataset);
        Format fmt = (Format) _formats.elementAt(dataset);
        if (style.equalsIgnoreCase("none")) {
            fmt.marks = 0;
        } else if (style.equalsIgnoreCase("points")) {
            fmt.marks = 1;
        } else if (style.equalsIgnoreCase("dots")) {
            fmt.marks = 2;
        } else if (style.equalsIgnoreCase("various")) {
            fmt.marks = 3;
        }
        fmt.marksUseDefault = false;
    }

    /** Specify the number of data sets to be plotted together.     *  This method is deprecated, since it is no longer necessary to     *  specify the number of data sets ahead of time.     *  It has the effect of clearing all previously plotted points.     *  This method should be called before setPointsPersistence().     *  This method throws IllegalArgumentException if the number is less     *  than 1.  This is a runtime exception, so it need not be declared.     *  @param numsets The number of data sets.     *  @deprecated     */
    public void setNumSets(int numsets) {
        if (numsets < 1) {
            throw new IllegalArgumentException("Number of data sets (" + numsets + ") must be greater than 0.");
        }
        _currentdataset = -1;
        _points.removeAllElements();
        _formats.removeAllElements();
        _prevx.removeAllElements();
        _prevy.removeAllElements();
        for (int i = 0; i < numsets; i++) {
            _points.addElement(new Vector());
            _formats.addElement(new Format());
            _prevx.addElement(new Long(0));
            _prevy.addElement(new Long(0));
        }
    }

    /** Calling this method with a positive argument sets the     *  persistence of the plot to the given number of points.  Calling     *  with a zero argument turns off this feature, reverting to     *  infinite memory (unless sweeps persistence is set).  If both     *  sweeps and points persistence are set then sweeps take     *  precedence.     */
    public void setPointsPersistence(int persistence) {
        _pointsPersistence = persistence;
    }

    /** A sweep is a sequence of points where the value of X is     *  increasing.  A point that is added with a smaller x than the     *  previous point increments the sweep count.  Calling this method     *  with a non-zero argument sets the persistence of the plot to     *  the given number of sweeps.  Calling with a zero argument turns     *  off this feature.  If both sweeps and points persistence are     *  set then sweeps take precedence.     *  <b> This feature is not implemented yet, so this method has no     *  effect</b>.     */
    public void setSweepsPersistence(int persistence) {
        _sweepsPersistence = persistence;
    }

    /** Override the base class to not clear the component first.     *  @param graphics The graphics context.     */
    public void update(Graphics g) {
        paint(g);
    }

    /** Check the argument to ensure that it is a valid data set index.     *  If it is less than zero, throw an IllegalArgumentException (which     *  is a runtime exception).  If it does not refer to an existing     *  data set, then fill out the _points Vector so that it does refer     *  to an existing data set. All other dataset-related vectors are     *  similarly filled out.     *  @param dataset The data set index.     */
    protected void _checkDatasetIndex(int dataset) {
        if (dataset < 0) {
            throw new IllegalArgumentException("Plot._checkDatasetIndex: Cannot" + " give a negative number for the data set index.");
        }
        while (dataset >= _points.size()) {
            _points.addElement(new Vector());
            _formats.addElement(new Format());
            _prevx.addElement(new Long(0));
            _prevy.addElement(new Long(0));
        }
    }

    /** Draw bar from the specified point to the y axis.     *  If the specified point is below the y axis or outside the     *  x range, do nothing.  If the <i>clip</i> argument is true,     *  then do not draw above the y range.     *  Note that paint() should be called before     *  calling this method so that _xscale and _yscale are properly set.     *  @param graphics The graphics context.     *  @param dataset The index of the dataset.     *  @param xpos The x position.     *  @param ypos The y position.     *  @param clip If true, then do not draw outside the range.     */
    protected void _drawBar(Graphics graphics, int dataset, long xpos, long ypos, boolean clip) {
        if (clip) {
            if (ypos < _uly) {
                ypos = _uly;
            }
            if (ypos > _lry) {
                ypos = _lry;
            }
        }
        if (ypos <= _lry && xpos <= _lrx && xpos >= _ulx) {
            int barlx = (int) (xpos - _barwidth * _xscale / 2 + _baroffset * _xscale);
            int barrx = (int) (barlx + _barwidth * _xscale) + 1;
            if (barlx < _ulx) barlx = _ulx;
            if (barrx > _lrx) barrx = _lrx;
            if (barlx >= barrx) barrx = barlx + 1;
            long zeroypos = _lry - (long) ((0 - _yMin) * _yscale);
            if (_lry < zeroypos) zeroypos = _lry;
            if (_uly > zeroypos) zeroypos = _uly;
            if (_yMin >= 0 || ypos <= zeroypos) {
                graphics.fillRect(barlx, (int) ypos, barrx - barlx, (int) (zeroypos - ypos));
            } else {
                graphics.fillRect(barlx, (int) zeroypos, barrx - barlx, (int) (ypos - zeroypos));
            }
        }
    }

    /** Draw an error bar for the specified yLowEB and yHighEB values.     *  If the specified point is below the y axis or outside the     *  x range, do nothing.  If the <i>clip</i> argument is true,     *  then do not draw above the y range.     *  @param graphics The graphics context.     *  @param dataset The index of the dataset.     *  @param xpos The x position.     *  @param yLowEBPos The lower y position of the error bar.     *  @param yHighEBPos The upper y position of the error bar.     *  @param clip If true, then do not draw above the range.     */
    protected void _drawErrorBar(Graphics graphics, int dataset, long xpos, long yLowEBPos, long yHighEBPos, boolean clip) {
        _drawLine(graphics, dataset, xpos - _ERRORBAR_LEG_LENGTH, yHighEBPos, xpos + _ERRORBAR_LEG_LENGTH, yHighEBPos, clip);
        _drawLine(graphics, dataset, xpos, yLowEBPos, xpos, yHighEBPos, clip);
        _drawLine(graphics, dataset, xpos - _ERRORBAR_LEG_LENGTH, yLowEBPos, xpos + _ERRORBAR_LEG_LENGTH, yLowEBPos, clip);
    }

    /** Draw a line from the specified point to the y axis.     *  If the specified point is below the y axis or outside the     *  x range, do nothing.  If the <i>clip</i> argument is true,     *  then do not draw above the y range.     *  @param graphics The graphics context.     *  @param xpos The x position.     *  @param ypos The y position.     *  @param clip If true, then do not draw outside the range.     */
    protected void _drawImpulse(Graphics graphics, long xpos, long ypos, boolean clip) {
        if (clip) {
            if (ypos < _uly) {
                ypos = _uly;
            }
            if (ypos > _lry) {
                ypos = _lry;
            }
        }
        if (ypos <= _lry && xpos <= _lrx && xpos >= _ulx) {
            double zeroypos = _lry - (long) ((0 - _yMin) * _yscale);
            if (_lry < zeroypos) zeroypos = _lry;
            if (_uly > zeroypos) zeroypos = _uly;
            graphics.drawLine((int) xpos, (int) ypos, (int) xpos, (int) zeroypos);
        }
    }

    /** Draw a line from the specified starting point to the specified     *  ending point.  The current color is used.  If the <i>clip</i> argument     *  is true, then draw only that portion of the line that lies within the     *  plotting rectangle.     *  @param graphics The graphics context.     *  @param dataset The index of the dataset.     *  @param startx The starting x position.     *  @param starty The starting y position.     *  @param endx The ending x position.     *  @param endy The ending y position.     *  @param clip If true, then do not draw outside the range.     */
    protected void _drawLine(Graphics graphics, int dataset, long startx, long starty, long endx, long endy, boolean clip) {
        if (clip) {
            if (!((endx <= _ulx && startx <= _ulx) || (endx >= _lrx && startx >= _lrx) || (endy <= _uly && starty <= _uly) || (endy >= _lry && starty >= _lry))) {
                if (startx != endx) {
                    if (endx < _ulx) {
                        endy = (int) (endy + ((long) (starty - endy) * (_ulx - endx)) / (startx - endx));
                        endx = _ulx;
                    } else if (endx > _lrx) {
                        endy = (int) (endy + ((long) (starty - endy) * (_lrx - endx)) / (startx - endx));
                        endx = _lrx;
                    }
                }
                if (starty != endy) {
                    if (endy < _uly) {
                        endx = (int) (endx + ((long) (startx - endx) * (_uly - endy)) / (starty - endy));
                        endy = _uly;
                    } else if (endy > _lry) {
                        endx = (int) (endx + ((long) (startx - endx) * (_lry - endy)) / (starty - endy));
                        endy = _lry;
                    }
                }
                if (startx != endx) {
                    if (startx < _ulx) {
                        starty = (int) (starty + ((long) (endy - starty) * (_ulx - startx)) / (endx - startx));
                        startx = _ulx;
                    } else if (startx > _lrx) {
                        starty = (int) (starty + ((long) (endy - starty) * (_lrx - startx)) / (endx - startx));
                        startx = _lrx;
                    }
                }
                if (starty != endy) {
                    if (starty < _uly) {
                        startx = (int) (startx + ((long) (endx - startx) * (_uly - starty)) / (endy - starty));
                        starty = _uly;
                    } else if (starty > _lry) {
                        startx = (int) (startx + ((long) (endx - startx) * (_lry - starty)) / (endy - starty));
                        starty = _lry;
                    }
                }
            }
            if (endx >= _ulx && endx <= _lrx && endy >= _uly && endy <= _lry && startx >= _ulx && startx <= _lrx && starty >= _uly && starty <= _lry) {
                graphics.drawLine((int) startx, (int) starty, (int) endx, (int) endy);
            }
        } else {
            graphics.drawLine((int) startx, (int) starty, (int) endx, (int) endy);
        }
    }

    /** Draw the axes and then plot all points.  This is synchronized     *  to prevent multiple threads from drawing the plot at the same     *  time.  It sets _painted true and calls notifyAll() at the end so that a     *  thread can use <code>wait()</code> to prevent it plotting     *  points before the axes have been first drawn.  If the second     *  argument is true, clear the display first.     *  This method is called by paint().  To cause it to be called you     *  would normally call repaint(), which eventually causes paint() to     *  be called.     *  @param graphics The graphics context.     *  @param clearfirst If true, clear the plot before proceeding.     */
    protected synchronized void _drawPlot(Graphics graphics, boolean clearfirst) {
        super._drawPlot(graphics, clearfirst);
        _showing = true;
        for (int dataset = _points.size() - 1; dataset >= 0; dataset--) {
            Vector data = (Vector) _points.elementAt(dataset);
            for (int pointnum = 0; pointnum < data.size(); pointnum++) {
                _drawPlotPoint(graphics, dataset, pointnum);
            }
        }
        _painted = true;
        notifyAll();
    }

    /** Put a mark corresponding to the specified dataset at the     *  specified x and y position. The mark is drawn in the current     *  color. What kind of mark is drawn depends on the _marks     *  variable and the dataset argument. If the fourth argument is     *  true, then check the range and plot only points that     *  are in range.     *  @param graphics The graphics context.     *  @param dataset The index of the dataset.     *  @param xpos The x position.     *  @param ypos The y position.     *  @param clip If true, then do not draw outside the range.     */
    protected void _drawPoint(Graphics graphics, int dataset, long xpos, long ypos, boolean clip) {
        if (!clip || (ypos <= _lry && ypos >= _uly && xpos <= _lrx && xpos >= _ulx)) {
            int xposi = (int) xpos;
            int yposi = (int) ypos;
            Format fmt = (Format) _formats.elementAt(dataset);
            int marks = _marks;
            if (!fmt.marksUseDefault) marks = fmt.marks;
            switch(marks) {
                case 0:
                    graphics.fillRect(xposi - 6, yposi - 6, 6, 6);
                    break;
                case 1:
                    graphics.fillOval(xposi - 1, yposi - 1, 3, 3);
                    break;
                case 2:
                    graphics.fillOval(xposi - _radius, yposi - _radius, _diameter, _diameter);
                    break;
                case 3:
                    int xpoints[], ypoints[];
                    int mark = dataset % _MAX_MARKS;
                    switch(mark) {
                        case 0:
                            graphics.fillOval(xposi - _radius, yposi - _radius, _diameter, _diameter);
                            break;
                        case 1:
                            graphics.drawLine(xposi - _radius, yposi - _radius, xposi + _radius, yposi + _radius);
                            graphics.drawLine(xposi + _radius, yposi - _radius, xposi - _radius, yposi + _radius);
                            break;
                        case 2:
                            graphics.drawRect(xposi - _radius, yposi - _radius, _diameter, _diameter);
                            break;
                        case 3:
                            xpoints = new int[4];
                            ypoints = new int[4];
                            xpoints[0] = xposi;
                            ypoints[0] = yposi - _radius;
                            xpoints[1] = xposi + _radius;
                            ypoints[1] = yposi + _radius;
                            xpoints[2] = xposi - _radius;
                            ypoints[2] = yposi + _radius;
                            xpoints[3] = xposi;
                            ypoints[3] = yposi - _radius;
                            graphics.fillPolygon(xpoints, ypoints, 4);
                            break;
                        case 4:
                            xpoints = new int[5];
                            ypoints = new int[5];
                            xpoints[0] = xposi;
                            ypoints[0] = yposi - _radius;
                            xpoints[1] = xposi + _radius;
                            ypoints[1] = yposi;
                            xpoints[2] = xposi;
                            ypoints[2] = yposi + _radius;
                            xpoints[3] = xposi - _radius;
                            ypoints[3] = yposi;
                            xpoints[4] = xposi;
                            ypoints[4] = yposi - _radius;
                            graphics.drawPolygon(xpoints, ypoints, 5);
                            break;
                        case 5:
                            graphics.drawOval(xposi - _radius, yposi - _radius, _diameter, _diameter);
                            break;
                        case 6:
                            graphics.drawLine(xposi, yposi - _radius, xposi, yposi + _radius);
                            graphics.drawLine(xposi - _radius, yposi, xposi + _radius, yposi);
                            break;
                        case 7:
                            graphics.fillRect(xposi - _radius, yposi - _radius, _diameter, _diameter);
                            break;
                        case 8:
                            xpoints = new int[4];
                            ypoints = new int[4];
                            xpoints[0] = xposi;
                            ypoints[0] = yposi - _radius;
                            xpoints[1] = xposi + _radius;
                            ypoints[1] = yposi + _radius;
                            xpoints[2] = xposi - _radius;
                            ypoints[2] = yposi + _radius;
                            xpoints[3] = xposi;
                            ypoints[3] = yposi - _radius;
                            graphics.drawPolygon(xpoints, ypoints, 4);
                            break;
                        case 9:
                            xpoints = new int[5];
                            ypoints = new int[5];
                            xpoints[0] = xposi;
                            ypoints[0] = yposi - _radius;
                            xpoints[1] = xposi + _radius;
                            ypoints[1] = yposi;
                            xpoints[2] = xposi;
                            ypoints[2] = yposi + _radius;
                            xpoints[3] = xposi - _radius;
                            ypoints[3] = yposi;
                            xpoints[4] = xposi;
                            ypoints[4] = yposi - _radius;
                            graphics.fillPolygon(xpoints, ypoints, 5);
                            break;
                    }
                    break;
                default:
            }
        }
    }

    /** Parse a line that gives plotting information. Return true if     *  the line is recognized.  Lines with syntax errors are ignored.     *  @param line A command line.     *  @return True if the line is recognized.     */
    protected boolean _parseLine(String line) {
        boolean connected = false;
        if (_connected) connected = true;
        if (super._parseLine(line)) {
            _pxgraphBlankLineMode = false;
            return true;
        } else {
            String lcLine = new String(line.toLowerCase());
            if (lcLine.startsWith("marks:")) {
                String style = (line.substring(6)).trim();
                if (_sawfirstdataset) {
                    setMarksStyle(style, _currentdataset);
                } else {
                    setMarksStyle(style);
                }
                _pxgraphBlankLineMode = false;
                return true;
            } else if (lcLine.startsWith("numsets:")) {
                _pxgraphBlankLineMode = false;
                return true;
            } else if (lcLine.startsWith("reusedatasets:")) {
                if (lcLine.indexOf("off", 16) >= 0) {
                    _reusedatasets = false;
                } else {
                    _reusedatasets = true;
                }
                return true;
            } else if (lcLine.startsWith("dataset:") || (_pxgraphBlankLineMode && lcLine.length() == 0)) {
                if (_reusedatasets && lcLine.length() > 0) {
                    String tlegend = (line.substring(8)).trim();
                    _currentdataset = -1;
                    int i;
                    for (i = 0; i <= _maxdataset; i++) {
                        if (getLegend(i).compareTo(tlegend) == 0) {
                            _currentdataset = i;
                        }
                    }
                    if (_currentdataset != -1) {
                        return true;
                    } else {
                        _currentdataset = _maxdataset;
                    }
                }
                _firstinset = true;
                _sawfirstdataset = true;
                _currentdataset++;
                if (lcLine.length() > 0) {
                    String legend = (line.substring(8)).trim();
                    if (legend != null && legend.length() > 0) {
                        addLegend(_currentdataset, legend);
                    }
                    _pxgraphBlankLineMode = false;
                }
                _maxdataset = _currentdataset;
                return true;
            } else if (lcLine.startsWith("lines:")) {
                if (lcLine.indexOf("off", 6) >= 0) {
                    setConnected(false);
                } else {
                    setConnected(true);
                }
                _pxgraphBlankLineMode = false;
                return true;
            } else if (lcLine.startsWith("impulses:")) {
                if (_sawfirstdataset) {
                    if (lcLine.indexOf("off", 9) >= 0) {
                        setImpulses(false, _currentdataset);
                    } else {
                        setImpulses(true, _currentdataset);
                    }
                } else {
                    if (lcLine.indexOf("off", 9) >= 0) {
                        setImpulses(false);
                    } else {
                        setImpulses(true);
                    }
                }
                _pxgraphBlankLineMode = false;
                return true;
            } else if (lcLine.startsWith("bars:")) {
                if (lcLine.indexOf("off", 5) >= 0) {
                    setBars(false);
                } else {
                    setBars(true);
                    int comma = line.indexOf(",", 5);
                    String barwidth;
                    String baroffset = null;
                    if (comma > 0) {
                        barwidth = (line.substring(5, comma)).trim();
                        baroffset = (line.substring(comma + 1)).trim();
                    } else {
                        barwidth = (line.substring(5)).trim();
                    }
                    try {
                        Double bwidth = new Double(barwidth);
                        double boffset = _baroffset;
                        if (baroffset != null) {
                            boffset = (new Double(baroffset)).doubleValue();
                        }
                        setBars(bwidth.doubleValue(), boffset);
                    } catch (NumberFormatException e) {
                    }
                }
                _pxgraphBlankLineMode = false;
                return true;
            } else if (line.startsWith("move:")) {
                connected = false;
                line = line.substring(5, line.length()).trim();
            } else if (line.startsWith("move")) {
                connected = false;
                line = line.substring(4, line.length()).trim();
            } else if (line.startsWith("draw:")) {
                line = line.substring(5, line.length()).trim();
            } else if (line.startsWith("draw")) {
                line = line.substring(4, line.length()).trim();
            }
            line = line.trim();
            int fieldsplit = line.indexOf(",");
            if (fieldsplit == -1) {
                fieldsplit = line.indexOf(" ");
            }
            if (fieldsplit == -1) {
                fieldsplit = line.indexOf(" ");
            }
            if (fieldsplit > 0) {
                String x = (line.substring(0, fieldsplit)).trim();
                String y = (line.substring(fieldsplit + 1)).trim();
                int fieldsplit2 = y.indexOf(",");
                if (fieldsplit2 == -1) {
                    fieldsplit2 = y.indexOf(" ");
                }
                if (fieldsplit2 == -1) {
                    fieldsplit2 = y.indexOf(" ");
                }
                if (fieldsplit2 > 0) {
                    line = (y.substring(fieldsplit2 + 1)).trim();
                    y = (y.substring(0, fieldsplit2)).trim();
                }
                try {
                    Double xpt = new Double(x);
                    Double ypt = new Double(y);
                    if (fieldsplit2 > 0) {
                        int fieldsplit3 = line.indexOf(",");
                        if (fieldsplit3 == -1) {
                            fieldsplit3 = line.indexOf(" ");
                        }
                        if (fieldsplit3 == -1) {
                            fieldsplit2 = line.indexOf(" ");
                        }
                        if (fieldsplit3 > 0) {
                            String yl = (line.substring(0, fieldsplit3)).trim();
                            String yh = (line.substring(fieldsplit3 + 1)).trim();
                            Double yLowEB = new Double(yl);
                            Double yHighEB = new Double(yh);
                            connected = _addLegendIfNecessary(connected);
                            addPointWithErrorBars(_currentdataset, xpt.doubleValue(), ypt.doubleValue(), yLowEB.doubleValue(), yHighEB.doubleValue(), connected);
                            return true;
                        } else {
                            connected = _addLegendIfNecessary(connected);
                            addPoint(_currentdataset, xpt.doubleValue(), ypt.doubleValue(), connected);
                            return true;
                        }
                    } else {
                        connected = _addLegendIfNecessary(connected);
                        addPoint(_currentdataset, xpt.doubleValue(), ypt.doubleValue(), connected);
                        return true;
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        return false;
    }

    /** Write plot information to the specified output stream.     *  Derived classes should override this method to first call     *  the parent class method, then add whatever additional information     *  they wish to add to the stream.     *  @param output A buffered print writer.     */
    protected void _write(PrintWriter output) {
        super._write(output);
        if (_reusedatasets) output.println("ReuseDatasets: on");
        if (!_connected) output.println("Lines: off");
        if (_bars) output.println("Bars: " + _barwidth + ", " + _baroffset);
        if (_impulses) output.println("Impulses: on");
        switch(_marks) {
            case 1:
                output.println("Marks: points");
            case 2:
                output.println("Marks: dots");
            case 3:
                output.println("Marks: various");
        }
        for (int dataset = 0; dataset < _points.size(); dataset++) {
            String legend = getLegend(dataset);
            if (legend != null) {
                output.println("DataSet: " + getLegend(dataset));
            } else {
                output.println("DataSet:");
            }
            Format fmt = (Format) _formats.elementAt(dataset);
            if (!fmt.impulsesUseDefault) {
                if (fmt.impulses) output.println("Impulses: on"); else output.println("Impulses: off");
            }
            if (!fmt.marksUseDefault) {
                switch(fmt.marks) {
                    case 0:
                        output.println("Marks: none");
                    case 1:
                        output.println("Marks: points");
                    case 2:
                        output.println("Marks: dots");
                    case 3:
                        output.println("Marks: various");
                }
            }
            Vector pts = (Vector) _points.elementAt(dataset);
            for (int pointnum = 0; pointnum < pts.size(); pointnum++) {
                PlotPoint pt = (PlotPoint) pts.elementAt(pointnum);
                if (!pt.connected) output.print("move: ");
                if (pt.errorBar) {
                    output.println(pt.x + ", " + pt.y + ", " + pt.yLowEB + ", " + pt.yHighEB);
                } else {
                    output.println(pt.x + ", " + pt.y);
                }
            }
        }
    }

    protected int _currentdataset = -1;

    protected Vector _points = new Vector();

    protected int _marks;

    protected boolean _painted = false;

    private boolean _addLegendIfNecessary(boolean connected) {
        if (!_sawfirstdataset || _currentdataset < 0) {
            _sawfirstdataset = true;
            _currentdataset++;
        }
        if (getLegend(_currentdataset) == null) {
            _firstinset = true;
            _sawfirstdataset = true;
            addLegend(_currentdataset, new String("Set " + _currentdataset));
        }
        if (_firstinset) {
            connected = false;
            _firstinset = false;
        }
        return connected;
    }

    private synchronized void _addPoint(int dataset, double x, double y, double yLowEB, double yHighEB, boolean connected, boolean errorBar) {
        _checkDatasetIndex(dataset);
        if (x < _xBottom) _xBottom = x;
        if (x > _xTop) _xTop = x;
        if (y < _yBottom) _yBottom = y;
        if (y > _yTop) _yTop = y;
        PlotPoint pt = new PlotPoint();
        pt.x = x;
        pt.y = y;
        pt.connected = connected && _connected;
        if (errorBar) {
            if (yLowEB < _yBottom) _yBottom = yLowEB;
            if (yLowEB > _yTop) _yTop = yLowEB;
            if (yHighEB < _yBottom) _yBottom = yHighEB;
            if (yHighEB > _yTop) _yTop = yHighEB;
            pt.yLowEB = yLowEB;
            pt.yHighEB = yHighEB;
            pt.errorBar = true;
        }
        Vector pts = (Vector) _points.elementAt(dataset);
        pts.addElement(pt);
        if (_pointsPersistence > 0) {
            if (pts.size() > _pointsPersistence) erasePoint(dataset, 0);
        }
        if (_showing) {
            _drawPlotPoint(getGraphics(), dataset, pts.size() - 1);
        }
    }

    private synchronized void _drawPlotPoint(Graphics graphics, int dataset, int index) {
        if (_pointsPersistence > 0) {
            graphics.setXORMode(_background);
        }
        if (_usecolor) {
            int color = dataset % _colors.length;
            graphics.setColor(_colors[color]);
            if (currentColors[color] != null) {
                graphics.setColor(currentColors[color]);
            }
        } else {
            graphics.setColor(_foreground);
        }
        Vector pts = (Vector) _points.elementAt(dataset);
        PlotPoint pt = (PlotPoint) pts.elementAt(index);
        long ypos = _lry - (long) ((pt.y - _yMin) * _yscale);
        long xpos = _ulx + (long) ((pt.x - _xMin) * _xscale);
        long prevx = ((Long) _prevx.elementAt(dataset)).longValue();
        long prevy = ((Long) _prevy.elementAt(dataset)).longValue();
        if (pt.connected) _drawLine(graphics, dataset, xpos, ypos, prevx, prevy, true);
        _prevx.setElementAt(new Long(xpos), dataset);
        _prevy.setElementAt(new Long(ypos), dataset);
        Format fmt = (Format) _formats.elementAt(dataset);
        if (fmt.impulsesUseDefault) {
            if (_impulses) _drawImpulse(graphics, xpos, ypos, true);
        } else {
            if (fmt.impulses) _drawImpulse(graphics, xpos, ypos, true);
        }
        int marks = _marks;
        if (!fmt.marksUseDefault) marks = fmt.marks;
        if (marks != 0) _drawPoint(graphics, dataset, xpos, ypos, true);
        if (_bars) _drawBar(graphics, dataset, xpos, ypos, true);
        if (pt.errorBar) _drawErrorBar(graphics, dataset, xpos, _lry - (long) ((pt.yLowEB - _yMin) * _yscale), _lry - (long) ((pt.yHighEB - _yMin) * _yscale), true);
        graphics.setColor(_foreground);
        if (_pointsPersistence > 0) {
            graphics.setPaintMode();
        }
    }

    private synchronized void _erasePoint(Graphics graphics, int dataset, int index) {
        if (_pointsPersistence > 0) {
            graphics.setXORMode(_background);
        }
        if (_usecolor) {
            int color = dataset % _colors.length;
            graphics.setColor(_colors[color]);
            if (currentColors[color] != null) {
                graphics.setColor(currentColors[color]);
            }
        } else {
            graphics.setColor(_foreground);
        }
        Vector pts = (Vector) _points.elementAt(dataset);
        PlotPoint pt = (PlotPoint) pts.elementAt(index);
        long ypos = _lry - (long) ((pt.y - _yMin) * _yscale);
        long xpos = _ulx + (long) ((pt.x - _xMin) * _xscale);
        if (index < pts.size() - 1) {
            PlotPoint nextp = (PlotPoint) pts.elementAt(index + 1);
            int nextx = _ulx + (int) ((nextp.x - _xMin) * _xscale);
            int nexty = _lry - (int) ((nextp.y - _yMin) * _yscale);
            if (nextp.connected) _drawLine(graphics, dataset, nextx, nexty, xpos, ypos, true);
            nextp.connected = false;
        }
        Format fmt = (Format) _formats.elementAt(dataset);
        if (fmt.impulsesUseDefault) {
            if (_impulses) _drawImpulse(graphics, xpos, ypos, true);
        } else {
            if (fmt.impulses) _drawImpulse(graphics, xpos, ypos, true);
        }
        int marks = _marks;
        if (!fmt.marksUseDefault) marks = fmt.marks;
        if (marks != 0) _drawPoint(graphics, dataset, xpos, ypos, true);
        if (_bars) _drawBar(graphics, dataset, xpos, ypos, true);
        if (pt.errorBar) _drawErrorBar(graphics, dataset, xpos, _lry - (long) ((pt.yLowEB - _yMin) * _yscale), _lry - (long) ((pt.yHighEB - _yMin) * _yscale), true);
        graphics.setColor(_foreground);
        if (_pointsPersistence > 0) {
            graphics.setPaintMode();
        }
    }

    private int _pointsPersistence = 0;

    private int _sweepsPersistence = 0;

    private boolean _bars = false;

    private double _barwidth = 0.5;

    private double _baroffset = 0.05;

    private boolean _connected = true;

    private boolean _impulses = false;

    private int _maxdataset = -1;

    private boolean _reusedatasets = false;

    private boolean _firstinset = true;

    private boolean _sawfirstdataset = false;

    private int _radius = 3;

    private int _diameter = 6;

    private boolean _pxgraphBlankLineMode = true;

    private static final int _NATIVE_ENDIAN = 0;

    private static final int _BIG_ENDIAN = 1;

    private static final int _LITTLE_ENDIAN = 2;

    private int _endian = _NATIVE_ENDIAN;

    private Vector _prevx = new Vector(), _prevy = new Vector();

    private static final int _ERRORBAR_LEG_LENGTH = 5;

    private static final int _MAX_MARKS = 10;

    private boolean _xyInvalid = false;

    private String _filename = null;

    private boolean _showing = false;

    private Vector _formats = new Vector();

    private class Format {

        public boolean impulses;

        public boolean impulsesUseDefault = true;

        public int marks;

        public boolean marksUseDefault = true;
    }
}
