package jj2000.j2k.decoder;

import jj2000.j2k.quantization.dequantizer.*;
import jj2000.j2k.image.invcomptransf.*;
import jj2000.j2k.fileformat.reader.*;
import jj2000.j2k.codestream.reader.*;
import jj2000.j2k.wavelet.synthesis.*;
import jj2000.j2k.entropy.decoder.*;
import jj2000.j2k.image.output.*;
import jj2000.j2k.image.*;
import jj2000.j2k.util.*;
import jj2000.j2k.roi.*;
import jj2000.j2k.io.*;
import jj2000.disp.*;
import jj2000.j2k.*;
import colorspace.*;
import icc.*;
import java.awt.image.*;
import java.util.*;
import java.awt.*;
import java.net.*;
import java.io.*;

/**
 * This class is the main class of JJ2000's decoder. It instantiates all objects
 * and performs the decoding operations. It then writes the image to the output
 * file or displays it.
 * 
 * <p>
 * First the decoder should be initialized with a ParameterList object given
 * through the constructor. The when the run() method is invoked and the decoder
 * executes. The exit code of the class can be obtained with the getExitCode()
 * method, after the constructor and after the run method. A non-zero value
 * indicates that an error has ocurred.
 * </p>
 * 
 * <p>
 * The decoding chain corresponds to the following sequence of modules:
 * </p>
 * 
 * <ul>
 * <li>BitstreamReaderAgent</li>
 * <li>EntropyDecoder</li>
 * <li>ROIDeScaler</li>
 * <li>Dequantizer</li>
 * <li>InverseWT</li>
 * <li>ImgDataConverter</li>
 * <li>EnumratedColorSpaceMapper, SyccColorSpaceMapper or ICCProfiler</li>
 * <li>ComponentDemixer (if needed)</li>
 * <li>ImgDataAdapter (if ComponentDemixer is needed)</li>
 * <li>ImgWriter</li>
 * <li>BlkImgDataSrcImageProducer</li>
 * </ul>
 * 
 * <p>
 * The 2 last modules cannot be used at the same time and corresponds
 * respectively to the writing of decoded image into a file or the graphical
 * display of this same image.
 * </p>
 * 
 * <p>
 * The behaviour of each module may be modified according to the current
 * tile-component. All the specifications are kept in modules extending
 * ModuleSpec and accessible through an instance of DecoderSpecs class.
 * </p>
 * 
 * @see BitstreamReaderAgent
 * @see EntropyDecoder
 * @see ROIDeScaler
 * @see Dequantizer
 * @see InverseWT
 * @see ImgDataConverter
 * @see InvCompTransf
 * @see ImgWriter
 * @see BlkImgDataSrcImageProducer
 * @see ModuleSpec
 * @see DecoderSpecs
 * */
public class Decoder extends ImgDecoder implements Runnable {

    /**
	 * Reference to the TitleUpdater instance. Only used when decoded image is
	 * displayed
	 */
    TitleUpdater title = null;

    /**
	 * False if the Decoder instance is self-contained process, false if thrown
	 * by another process (i.e by a GUI)
	 */
    private boolean isChildProcess = false;

    /** The default parameter list (arguments) */
    private ParameterList defpl;

    /** The valid list of options prefixes */
    private static final char vprfxs[] = { BitstreamReaderAgent.OPT_PREFIX, EntropyDecoder.OPT_PREFIX, ROIDeScaler.OPT_PREFIX, Dequantizer.OPT_PREFIX, InvCompTransf.OPT_PREFIX, HeaderDecoder.OPT_PREFIX, ColorSpaceMapper.OPT_PREFIX };

    /** Frame used to display decoded image */
    private Frame win = null;

    /** The component where the image is to be displayed */
    private ImgScrollPane isp;

    /** The parameter information for this class */
    private static final String[][] pinfo = { { "u", "[on|off]", "Prints usage information. If specified all other arguments (except 'v') are ignored", "off" }, { "v", "[on|off]", "Prints version and copyright information", "off" }, { "verbose", "[on|off]", "Prints information about the decoded codestream", "on" }, { "pfile", "<filename>", "Loads the arguments from the specified file. Arguments that are " + "specified on the command line override the ones from the file.\n" + "The arguments file is a simple text file with one argument per " + "line of the following form:\n  <argument name>=<argument value>\n" + "If the argument is of boolean type (i.e. its presence turns a " + "feature on), then the 'on' value turns it on, while the 'off' " + "value turns it off. The argument name does not include the '-' " + "or '+' character. Long lines can be broken into several lines " + "by terminating them with '\\'. Lines starting with '#' are " + "considered as comments. This option is not recursive: any 'pfile' " + "argument appearing in the file is ignored.", null }, { "res", "<resolution level index>", "The resolution level at which to reconstruct the image " + " (0 means the lowest available resolution whereas the maximum " + "resolution level corresponds to the original image resolution). If the given index" + " is greater than the number of available resolution levels of the " + "compressed image, the image is reconstructed at its highest " + "resolution (among all tile-components). Note that this option" + " affects only the inverse wavelet transform and not the number " + " of bytes read by the codestream parser: this number of bytes " + "depends only on options '-nbytes' or '-rate'.", null }, { "i", "<filename or url>", "The file containing the JPEG 2000 compressed data. This can be " + "either a JPEG 2000 codestream or a JP2 file containing a JPEG 2000 " + "codestream. In the latter case the first codestream in the file " + "will be decoded. If an URL is specified (e.g., http://...) " + "the data will be downloaded and cached in memory before decoding. " + "This is intended for easy use in applets, but it is not a very " + "efficient way of decoding network served data.", null }, { "o", "<filename>", "This is the name of the file to which the decompressed image " + "is written. If no output filename is given, the image is displayed on the screen. " + "Output file format is PGX by default. If the extension" + " is '.pgm' then a PGM file is written as output, however this is " + "only permitted if the component bitdepth does not exceed 8. If " + "the extension is '.ppm' then a PPM file is written, however this " + "is only permitted if there are 3 components and none of them has " + "a bitdepth of more than 8. If there is more than 1 component, " + "suffices '-1', '-2', '-3', ... are added to the file name, just " + "before the extension, except for PPM files where all three " + "components are written to the same file.", null }, { "rate", "<decoding rate in bpp>", "Specifies the decoding rate in bits per pixel (bpp) where the " + "number of pixels is related to the image's original size (Note:" + " this number is not affected by the '-res' option). If it is equal" + "to -1, the whole codestream is decoded. " + "The codestream is either parsed (default) or truncated depending " + "the command line option '-parsing'. To specify the decoding " + "rate in bytes, use '-nbytes' options instead.", "-1" }, { "nbytes", "<decoding rate in bytes>", "Specifies the decoding rate in bytes. " + "The codestream is either parsed (default) or truncated depending " + "the command line option '-parsing'. To specify the decoding " + "rate in bits per pixel, use '-rate' options instead.", "-1" }, { "parsing", null, "Enable or not the parsing mode when decoding rate is specified " + "('-nbytes' or '-rate' options). If it is false, the codestream " + "is decoded as if it were truncated to the given rate. If it is " + "true, the decoder creates, truncates and decodes a virtual layer" + " progressive codestream with the same truncation points in each code-block.", "on" }, { "ncb_quit", "<max number of code blocks>", "Use the ncb and lbody quit conditions. If state information is " + "found for more code blocks than is indicated with this option, the decoder " + "will decode using only information found before that point. " + "Using this otion implies that the 'rate' or 'nbyte' parameter " + "is used to indicate the lbody parameter which is the number of " + "packet body bytes the decoder will decode.", "-1" }, { "l_quit", "<max number of layers>", "Specifies the maximum number of layers to decode for any code-block", "-1" }, { "m_quit", "<max number of bit planes>", "Specifies the maximum number of bit planes to decode for any code-block", "-1" }, { "poc_quit", null, "Specifies the whether the decoder should only decode code-blocks " + "included in the first progression order.", "off" }, { "one_tp", null, "Specifies whether the decoder should only decode the first tile part of each tile.", "off" }, { "comp_transf", null, "Specifies whether the component transform indicated in the codestream should be used.", "on" }, { "debug", null, "Print debugging messages when an error is encountered.", "off" }, { "cdstr_info", null, "Display information about the codestream. This information is: " + "\n- Marker segments value in main and tile-part headers," + "\n- Tile-part length and position within the code-stream.", "off" }, { "nocolorspace", null, "Ignore any colorspace information in the image.", "off" }, { "colorspace_debug", null, "Print debugging messages when an error is encountered in the colorspace module.", "off" } };

    /**
	 * Instantiates a decoder object, with the ParameterList object given as
	 * argument and a component where to display the image if no output file is
	 * specified. It also retrieves the default ParameterList.
	 * 
	 * @param pl
	 *            The ParameterList for this decoder (contains also defaults
	 *            values).
	 * 
	 * @param isp
	 *            The component where the image is to be displayed if not output
	 *            file is specified. If null a new frame will be created to
	 *            display the image.
	 * */
    public Decoder(ParameterList pl, ImgScrollPane isp) {
        super(pl);
        defpl = pl.getDefaultParameterList();
        this.isp = isp;
    }

    /**
	 * Instantiates a decoder object, with the ParameterList object given as
	 * argument. It also retrieves the default ParameterList.
	 * 
	 * @param pl
	 *            The ParameterList for this decoder (contains also defaults
	 *            values).
	 * */
    public Decoder(ParameterList pl) {
        this(pl, null);
    }

    /**
	 * Returns the parameters that are used in this class. It returns a 2D
	 * String array. Each of the 1D arrays is for a different option, and they
	 * have 3 elements. The first element is the option name, the second one is
	 * the synopsis and the third one is a long description of what the
	 * parameter is. The synopsis or description may be 'null', in which case it
	 * is assumed that there is no synopsis or description of the option,
	 * respectively.
	 * 
	 * @return the options name, their synopsis and their explanation.
	 * */
    public static String[][] getParameterInfo() {
        return pinfo;
    }

    /**
	 * Runs the decoder. After completion the exit code is set, a non-zero value
	 * indicates that an error ocurred.
	 * 
	 * @see #getExitCode
	 * */
    @Override
    public void run() {
        boolean verbose;
        int i;
        String infile;
        RandomAccessIO in;
        String outfile = "", outbase = "", outext = "";
        String out[] = null;
        ImgWriter imwriter[] = null;
        boolean disp = false;
        Image img = null;
        Dimension winDim, scrnDim;
        Insets ins = null;
        String btitle = "";
        try {
            try {
                if (pl.getBooleanParameter("v")) {
                    printVersionAndCopyright();
                }
                if (pl.getParameter("u").equals("on")) {
                    printUsage();
                    return;
                }
                verbose = pl.getBooleanParameter("verbose");
            } catch (StringFormatException e) {
                error("An error occured while parsing the arguments:\n" + e.getMessage(), 1, e);
                return;
            } catch (NumberFormatException e) {
                error("An error occured while parsing the arguments:\n" + e.getMessage(), 1, e);
                return;
            }
            try {
                pl.checkList(vprfxs, ParameterList.toNameArray(pinfo));
            } catch (IllegalArgumentException e) {
                error(e.getMessage(), 2, e);
                return;
            }
            infile = pl.getParameter("i");
            if (infile == null) {
                error("Input file ('-i' option) has not been specified", 1);
                return;
            }
            outfile = pl.getParameter("o");
            if (outfile == null) {
                disp = true;
            } else if (outfile.lastIndexOf('.') != -1) {
                outext = outfile.substring(outfile.lastIndexOf('.'), outfile.length());
                outbase = outfile.substring(0, outfile.lastIndexOf('.'));
            } else {
                outbase = outfile;
                outext = ".pgx";
            }
            if (infile.indexOf("/") >= 1 && infile.charAt(infile.indexOf("/") - 1) == ':') {
                URL inurl;
                URLConnection conn;
                int datalen;
                InputStream is;
                try {
                    inurl = new URL(infile);
                } catch (MalformedURLException e) {
                    error("Malformed URL for input file " + infile, 4, e);
                    return;
                }
                try {
                    conn = inurl.openConnection();
                    conn.connect();
                } catch (IOException e) {
                    error("Cannot open connection to " + infile + ((e.getMessage() != null) ? (":\n" + e.getMessage()) : ""), 4, e);
                    return;
                }
                datalen = conn.getContentLength();
                try {
                    is = conn.getInputStream();
                } catch (IOException e) {
                    error("Cannot get data from connection to " + infile + ((e.getMessage() != null) ? (":\n" + e.getMessage()) : ""), 4, e);
                    return;
                }
                if (datalen != -1) {
                    in = new ISRandomAccessIO(is, datalen, 1, datalen);
                } else {
                    in = new ISRandomAccessIO(is);
                }
                try {
                    in.read();
                    in.seek(0);
                } catch (IOException e) {
                    error("Cannot get input data from " + infile + " Invalid URL?", 4, e);
                    return;
                }
            } else {
                try {
                    in = new BEBufferedRandomAccessFile(infile, "r");
                } catch (IOException e) {
                    error("Cannot open input file " + ((e.getMessage() != null) ? (":\n" + e.getMessage()) : ""), 4, e);
                    return;
                }
            }
            FileFormatReader ff = new FileFormatReader(in);
            ff.readFileFormat();
            if (ff.JP2FFUsed) {
                in.seek(ff.getFirstCodeStreamPos());
            }
            BlkImgDataSrc decodedImage = decode(in, ff, verbose);
            int nCompImg = decodedImage.getNumComps();
            if (disp) {
                btitle = "JJ2000: " + (new File(infile)).getName() + " " + decodedImage.getImgWidth() + "x" + decodedImage.getImgHeight();
                if (isp == null) {
                    win = new Frame(btitle + " @ (0,0) : 1");
                    win.setBackground(Color.white);
                    win.addWindowListener(new ExitHandler(this));
                    isp = new ImgScrollPane(ImgScrollPane.SCROLLBARS_AS_NEEDED);
                    win.add(isp, BorderLayout.CENTER);
                    isp.addKeyListener(new ImgKeyListener(isp, this));
                    win.addKeyListener(new ImgKeyListener(isp, this));
                } else {
                    win = null;
                }
                if (win != null) {
                    win.addNotify();
                    ins = win.getInsets();
                    int subX = decodedImage.getCompSubsX(0);
                    int subY = decodedImage.getCompSubsY(0);
                    int w = (decodedImage.getImgWidth() + subX - 1) / subX;
                    int h = (decodedImage.getImgHeight() + subY - 1) / subY;
                    winDim = new Dimension(w + ins.left + ins.right, h + ins.top + ins.bottom);
                    scrnDim = win.getToolkit().getScreenSize();
                    if (winDim.width > scrnDim.width * 8 / 10f) {
                        winDim.width = (int) (scrnDim.width * 8 / 10f);
                    }
                    if (winDim.height > scrnDim.height * 8 / 10f) {
                        winDim.height = (int) (scrnDim.height * 8 / 10f);
                    }
                    win.setSize(winDim);
                    win.validate();
                    win.setVisible(true);
                    Thread tu;
                    title = new TitleUpdater(isp, win, btitle);
                    tu = new Thread(title);
                    tu.start();
                } else {
                    title = null;
                }
            } else {
                if (csMap != null) {
                    if (outext.equalsIgnoreCase(".PPM") && (nCompImg != 3 || decodedImage.getNomRangeBits(0) > 8 || decodedImage.getNomRangeBits(1) > 8 || decodedImage.getNomRangeBits(2) > 8 || csMap.isOutputSigned(0) || csMap.isOutputSigned(1) || csMap.isOutputSigned(2))) {
                        error("Specified PPM output file but compressed image is not of the correct format " + "for PPM or limited decoded components to less than 3.", 1);
                        return;
                    }
                } else {
                    if (outext.equalsIgnoreCase(".PPM") && (nCompImg != 3 || decodedImage.getNomRangeBits(0) > 8 || decodedImage.getNomRangeBits(1) > 8 || decodedImage.getNomRangeBits(2) > 8 || hd.isOriginalSigned(0) || hd.isOriginalSigned(1) || hd.isOriginalSigned(2))) {
                        error("Specified PPM output file but compressed image is not of the correct format " + "for PPM or limited decoded components to less than 3.", 1);
                        return;
                    }
                }
                out = new String[nCompImg];
                for (i = 0; i < nCompImg; i++) {
                    out[i] = "";
                }
                if (nCompImg > 1 && !outext.equalsIgnoreCase(".PPM")) {
                    if (outext.equalsIgnoreCase(".PGM")) {
                        for (i = 0; i < nCompImg; i++) {
                            if (csMap != null) {
                                if (csMap.isOutputSigned(i)) {
                                    error("Specified PGM output file but compressed image is not of the " + "correct format for PGM.", 1);
                                    return;
                                }
                            } else {
                                if (hd.isOriginalSigned(i)) {
                                    error("Specified PGM output file but compressed image is not of the " + "correct format for PGM.", 1);
                                    return;
                                }
                            }
                        }
                    }
                    for (i = 0; i < nCompImg; i++) {
                        out[i] = outbase + "-" + (i + 1) + outext;
                    }
                } else {
                    out[0] = outbase + outext;
                }
                if (outext.equalsIgnoreCase(".PPM")) {
                    imwriter = new ImgWriter[1];
                    try {
                        imwriter[0] = new ImgWriterPPM(out[0], decodedImage, 0, 1, 2);
                    } catch (IOException e) {
                        error("Cannot write PPM header or open output file" + i + ((e.getMessage() != null) ? (":\n" + e.getMessage()) : ""), 2, e);
                        return;
                    }
                } else {
                    imwriter = new ImgWriter[nCompImg];
                }
                if (csMap != null) {
                    if (imwriter.length == 3 && decodedImage.getNomRangeBits(0) <= 8 && decodedImage.getNomRangeBits(1) <= 8 && decodedImage.getNomRangeBits(2) <= 8 && !csMap.isOutputSigned(0) && !csMap.isOutputSigned(1) && !csMap.isOutputSigned(2) && decSpec.cts.isCompTransfUsed()) {
                        warning("JJ2000 is quicker with one PPM output " + "file than with 3 PGM/PGX output files when a" + " component transformation is applied.");
                    }
                } else {
                    if (imwriter.length == 3 && decodedImage.getNomRangeBits(0) <= 8 && decodedImage.getNomRangeBits(1) <= 8 && decodedImage.getNomRangeBits(2) <= 8 && !hd.isOriginalSigned(0) && !hd.isOriginalSigned(1) && !hd.isOriginalSigned(2) && decSpec.cts.isCompTransfUsed()) {
                        warning("JJ2000 is quicker with one PPM output " + "file than with 3 PGM/PGX output files when a" + " component transformation is applied.");
                    }
                }
            }
            int mrl = decSpec.dls.getMin();
            if (verbose) {
                int res = breader.getImgRes();
                if (mrl != res) {
                    FacilityManager.getMsgLogger().println("Reconstructing resolution " + res + " on " + mrl + " (" + breader.getImgWidth(res) + "x" + breader.getImgHeight(res) + ")", 8, 8);
                }
                if (pl.getFloatParameter("rate") != -1) {
                    FacilityManager.getMsgLogger().println("Target rate = " + breader.getTargetRate() + " bpp (" + breader.getTargetNbytes() + " bytes)", 8, 8);
                }
            }
            if (disp) {
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY + 1);
                img = BlkImgDataSrcImageProducer.createImage(decodedImage, isp);
                isp.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                if (win != null) {
                    win.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                }
                isp.setImage(img);
                isp.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                if (win != null) {
                    win.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
                if (win != null) {
                    int status;
                    do {
                        status = isp.checkImage(img, null);
                        if ((status & ImageObserver.ERROR) != 0) {
                            FacilityManager.getMsgLogger().printmsg(MsgLogger.ERROR, "An unknown error occurred while producing the image");
                            return;
                        } else if ((status & ImageObserver.ABORT) != 0) {
                            FacilityManager.getMsgLogger().printmsg(MsgLogger.ERROR, "Image production was aborted for some unknown reason");
                        } else if ((status & ImageObserver.ALLBITS) != 0) {
                            ImgMouseListener iml = new ImgMouseListener(isp);
                            isp.addMouseListener(iml);
                            isp.addMouseMotionListener(iml);
                        } else {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                            }
                        }
                    } while ((status & (ImageObserver.ALLBITS | ImageObserver.ABORT | ImageObserver.ERROR)) == 0);
                }
            } else {
                for (i = 0; i < imwriter.length; i++) {
                    if (outext.equalsIgnoreCase(".PGM")) {
                        try {
                            imwriter[i] = new ImgWriterPGM(out[i], decodedImage, i);
                        } catch (IOException e) {
                            error("Cannot write PGM header or open output file for component " + i + ((e.getMessage() != null) ? (":\n" + e.getMessage()) : ""), 2, e);
                            return;
                        }
                    } else if (outext.equalsIgnoreCase(".PGX")) {
                        try {
                            if (csMap != null) {
                                imwriter[i] = new ImgWriterPGX(out[i], decodedImage, i, csMap.isOutputSigned(i));
                            } else {
                                imwriter[i] = new ImgWriterPGX(out[i], decodedImage, i, hd.isOriginalSigned(i));
                            }
                        } catch (IOException e) {
                            error("Cannot write PGX header or open output file for component " + i + ((e.getMessage() != null) ? (":\n" + e.getMessage()) : ""), 2, e);
                            return;
                        }
                    }
                    try {
                        imwriter[i].writeAll();
                    } catch (IOException e) {
                        error("I/O error while writing output file" + ((e.getMessage() != null) ? (":\n" + e.getMessage()) : ""), 2, e);
                        return;
                    }
                    try {
                        imwriter[i].close();
                    } catch (IOException e) {
                        error("I/O error while closing output file (data may be corrupted" + ((e.getMessage() != null) ? (":\n" + e.getMessage()) : ""), 2, e);
                        return;
                    }
                }
            }
            if (verbose) {
                float bitrate = breader.getActualRate();
                int numBytes = breader.getActualNbytes();
                if (ff.JP2FFUsed) {
                    int imageSize = (int) ((8.0f * numBytes) / bitrate);
                    numBytes += ff.getFirstCodeStreamPos();
                    bitrate = (numBytes * 8.0f) / imageSize;
                }
                if (pl.getIntParameter("ncb_quit") == -1) {
                    FacilityManager.getMsgLogger().println("Actual bitrate = " + bitrate + " bpp (i.e. " + numBytes + " bytes)", 8, 8);
                } else {
                    FacilityManager.getMsgLogger().println("Number of packet body bytes read = " + numBytes, 8, 8);
                }
                FacilityManager.getMsgLogger().flush();
            }
        } catch (IllegalArgumentException e) {
            error(e.getMessage(), 2);
            if (pl.getParameter("debug").equals("on")) e.printStackTrace();
            return;
        } catch (Error e) {
            if (e.getMessage() != null) {
                error(e.getMessage(), 2);
            } else {
                error("An error has occured during decoding.", 2);
            }
            if (pl.getParameter("debug").equals("on")) {
                e.printStackTrace();
            } else {
                error("Use '-debug' option for more details", 2);
            }
            return;
        } catch (RuntimeException e) {
            if (e.getMessage() != null) {
                error("An uncaught runtime exception has occurred:\n" + e.getMessage(), 2);
            } else {
                error("An uncaught runtime exception has occurred.", 2);
            }
            if (pl.getParameter("debug").equals("on")) {
                e.printStackTrace();
            } else {
                error("Use '-debug' option for more details", 2);
            }
            return;
        } catch (Throwable e) {
            error("An uncaught exception has occurred.", 2);
            if (pl.getParameter("debug").equals("on")) {
                e.printStackTrace();
            } else {
                error("Use '-debug' option for more details", 2);
            }
            return;
        }
    }

    /**
	 * Returns all the parameters used in the decoding chain. It calls parameter
	 * from each module and store them in one array (one row per parameter and 4
	 * columns).
	 * 
	 * @return All decoding parameters
	 * 
	 * @see #getParameterInfo
	 * */
    public static String[][] getAllParameters() {
        Vector<String[]> vec = new Vector<String[]>();
        int i;
        String[][] str = BitstreamReaderAgent.getParameterInfo();
        if (str != null) for (i = str.length - 1; i >= 0; i--) vec.addElement(str[i]);
        str = EntropyDecoder.getParameterInfo();
        if (str != null) for (i = str.length - 1; i >= 0; i--) vec.addElement(str[i]);
        str = ROIDeScaler.getParameterInfo();
        if (str != null) for (i = str.length - 1; i >= 0; i--) vec.addElement(str[i]);
        str = Dequantizer.getParameterInfo();
        if (str != null) for (i = str.length - 1; i >= 0; i--) vec.addElement(str[i]);
        str = InvCompTransf.getParameterInfo();
        if (str != null) for (i = str.length - 1; i >= 0; i--) vec.addElement(str[i]);
        str = HeaderDecoder.getParameterInfo();
        if (str != null) for (i = str.length - 1; i >= 0; i--) vec.addElement(str[i]);
        str = ICCProfiler.getParameterInfo();
        if (str != null) for (i = str.length - 1; i >= 0; i--) vec.addElement(str[i]);
        str = getParameterInfo();
        if (str != null) for (i = str.length - 1; i >= 0; i--) vec.addElement(str[i]);
        str = new String[vec.size()][4];
        for (i = str.length - 1; i >= 0; i--) str[i] = vec.elementAt(i);
        return str;
    }

    /**
	 * Prints the warning message 'msg' to standard err, prepending "WARNING" to
	 * it.
	 * 
	 * @param msg
	 *            The error message
	 * */
    private void warning(String msg) {
        FacilityManager.getMsgLogger().printmsg(MsgLogger.WARNING, msg);
    }

    /**
	 * Prints version and copyright information to the logging facility returned
	 * by FacilityManager.getMsgLogger()
	 * */
    private void printVersionAndCopyright() {
        FacilityManager.getMsgLogger().println("JJ2000's JPEG 2000 Decoder\n", 2, 4);
        FacilityManager.getMsgLogger().println("Version: " + JJ2KInfo.version + "\n", 2, 4);
        FacilityManager.getMsgLogger().println("Copyright:\n\n" + JJ2KInfo.copyright + "\n", 2, 4);
        FacilityManager.getMsgLogger().println("Send bug reports to: " + JJ2KInfo.bugaddr + "\n", 2, 4);
    }

    /**
	 * Prints the usage information to stdout. The usage information is written
	 * for all modules in the decoder.
	 * */
    private void printUsage() {
        MsgLogger ml = FacilityManager.getMsgLogger();
        ml.println("Usage:", 0, 0);
        ml.println("JJ2KDecoder args...\n", 10, 12);
        ml.println("The exit code of the decoder is non-zero if an error occurs.", 2, 4);
        ml.println("The following arguments are recongnized:\n", 2, 4);
        printParamInfo(ml, getAllParameters());
        FacilityManager.getMsgLogger().println("\n\n", 0, 0);
        FacilityManager.getMsgLogger().println("Send bug reports to: " + JJ2KInfo.bugaddr + "\n", 2, 4);
    }

    /**
	 * Prints the parameters in 'pinfo' to the provided output, 'out', showing
	 * the existing defaults. The message is printed to the logging facility
	 * returned by FacilityManager.getMsgLogger(). The 'pinfo' argument is a 2D
	 * String array. The first dimension contains String arrays, 1 for each
	 * parameter. Each of these arrays has 3 elements, the first element is the
	 * parameter name, the second element is the synopsis for the parameter and
	 * the third one is a long description of the parameter. If the synopsis or
	 * description is 'null' then no synopsis or description is printed,
	 * respectively. If there is a default value for a parameter it is also
	 * printed.
	 * 
	 * @param out
	 *            Where to print.
	 * 
	 * @param pinfo
	 *            The parameter information to write.
	 * */
    private void printParamInfo(MsgLogger out, String pinfo[][]) {
        String defval;
        for (int i = 0; i < pinfo.length; i++) {
            defval = defpl.getParameter(pinfo[i][0]);
            if (defval != null) {
                out.println("-" + pinfo[i][0] + ((pinfo[i][1] != null) ? " " + pinfo[i][1] + " " : " ") + "(default = " + defval + ")", 4, 8);
            } else {
                out.println("-" + pinfo[i][0] + ((pinfo[i][1] != null) ? " " + pinfo[i][1] : ""), 4, 8);
            }
            if (pinfo[i][2] != null) {
                out.println(pinfo[i][2], 6, 6);
            }
        }
    }

    /**
	 * Exit the decoding process according to the isChildProcess variable
	 **/
    public void exit() {
        if (isChildProcess) {
            if (win != null) win.dispose();
            if (title != null) title.done = true;
            return;
        }
        System.exit(0);
    }

    /**
	 * Set isChildProcess variable.
	 * 
	 * @param b
	 *            The boolean value
	 * */
    public void setChildProcess(boolean b) {
        isChildProcess = b;
    }
}
