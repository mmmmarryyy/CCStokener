import java.net.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;

/**
 * Class GifDecoder - Decodes a GIF file into one or more frames.
 * <br><pre>
 * Example:
 *    GifDecoder d = new GifDecoder();
 *    d.read("sample.gif");
 *    int n = d.getFrameCount();
 *    for (int i = 0; i < n; i++) {
 *	 BufferedImage frame = d.getFrame(i);  // frame i
 *	 int t = d.getDelay(i);	 // display duration of frame in milliseconds
 *	 // do something with frame
 *    }
 * </pre>
 * No copyright asserted on the source code of this class.  May be used for
 * any purpose, however, refer to the Unisys LZW patent for any additional
 * restrictions.  Please forward any corrections to kweiner@fmsware.com.
 *
 * @author Kevin Weiner, FM Software; LZW decoder adapted from John Cristy's ImageMagick.
 * @version 1.03 November 2003
 *
 */
public class GifDecoder {

    /**
	 * File read status: No errors.
	 */
    public static final int STATUS_OK = 0;

    /**
	 * File read status: Error decoding file (may be partially decoded)
	 */
    public static final int STATUS_FORMAT_ERROR = 1;

    /**
	 * File read status: Unable to open source.
	 */
    public static final int STATUS_OPEN_ERROR = 2;

    protected BufferedInputStream in;

    protected int status;

    protected int width;

    protected int height;

    protected boolean gctFlag;

    protected int gctSize;

    protected int loopCount = 1;

    protected int[] gct;

    protected int[] lct;

    protected int[] act;

    protected int bgIndex;

    protected int bgColor;

    protected int lastBgColor;

    protected int pixelAspect;

    protected boolean lctFlag;

    protected boolean interlace;

    protected int lctSize;

    protected int ix, iy, iw, ih;

    protected Rectangle lastRect;

    protected BufferedImage image;

    protected BufferedImage lastImage;

    protected byte[] block = new byte[256];

    protected int blockSize = 0;

    protected int dispose = 0;

    protected int lastDispose = 0;

    protected boolean transparency = false;

    protected int delay = 0;

    protected int transIndex;

    protected static final int MaxStackSize = 4096;

    protected short[] prefix;

    protected byte[] suffix;

    protected byte[] pixelStack;

    protected byte[] pixels;

    protected ArrayList frames;

    protected int frameCount;

    static class GifFrame {

        public GifFrame(BufferedImage im, int del) {
            _prof.prof.cnt[652]++;
            {
                _prof.prof.cnt[653]++;
                image = im;
            }
            {
                _prof.prof.cnt[654]++;
                delay = del;
            }
        }

        public BufferedImage image;

        public int delay;
    }

    /**
	 * Gets display duration for specified frame.
	 *
	 * @param n int index of frame
	 * @return delay in milliseconds
	 */
    public int getDelay(int n) {
        _prof.prof.cnt[655]++;
        {
            _prof.prof.cnt[656]++;
            delay = -1;
        }
        {
            _prof.prof.cnt[657]++;
            if ((n >= 0) && (n < frameCount)) {
                {
                    _prof.prof.cnt[658]++;
                    delay = ((GifFrame) frames.get(n)).delay;
                }
            }
        }
        {
            _prof.prof.cnt[659]++;
            return delay;
        }
    }

    /**
	 * Gets the number of frames read from file.
	 * @return frame count
	 */
    public int getFrameCount() {
        _prof.prof.cnt[660]++;
        {
            _prof.prof.cnt[661]++;
            return frameCount;
        }
    }

    /**
	 * Gets the first (or only) image read.
	 *
	 * @return BufferedImage containing first frame, or null if none.
	 */
    public BufferedImage getImage() {
        _prof.prof.cnt[662]++;
        {
            _prof.prof.cnt[663]++;
            return getFrame(0);
        }
    }

    /**
	 * Gets the "Netscape" iteration count, if any.
	 * A count of 0 means repeat indefinitiely.
	 *
	 * @return iteration count if one was specified, else 1.
	 */
    public int getLoopCount() {
        _prof.prof.cnt[664]++;
        {
            _prof.prof.cnt[665]++;
            return loopCount;
        }
    }

    /**
	 * Creates new frame image from current data (and previous
	 * frames as specified by their disposition codes).
	 */
    protected void setPixels() {
        _prof.prof.cnt[666]++;
        _prof.prof.cnt[667]++;
        int[] dest = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        {
            _prof.prof.cnt[668]++;
            if (lastDispose > 0) {
                {
                    _prof.prof.cnt[669]++;
                    if (lastDispose == 3) {
                        _prof.prof.cnt[670]++;
                        int n = frameCount - 2;
                        {
                            _prof.prof.cnt[671]++;
                            if (n > 0) {
                                {
                                    _prof.prof.cnt[672]++;
                                    lastImage = getFrame(n - 1);
                                }
                            } else {
                                {
                                    _prof.prof.cnt[673]++;
                                    lastImage = null;
                                }
                            }
                        }
                    }
                }
                {
                    _prof.prof.cnt[674]++;
                    if (lastImage != null) {
                        _prof.prof.cnt[675]++;
                        int[] prev = ((DataBufferInt) lastImage.getRaster().getDataBuffer()).getData();
                        {
                            _prof.prof.cnt[676]++;
                            System.arraycopy(prev, 0, dest, 0, width * height);
                        }
                        {
                            _prof.prof.cnt[677]++;
                            if (lastDispose == 2) {
                                _prof.prof.cnt[678]++;
                                Graphics2D g = image.createGraphics();
                                _prof.prof.cnt[679]++;
                                Color c = null;
                                {
                                    _prof.prof.cnt[680]++;
                                    if (transparency) {
                                        {
                                            _prof.prof.cnt[681]++;
                                            c = new Color(0, 0, 0, 0);
                                        }
                                    } else {
                                        {
                                            _prof.prof.cnt[682]++;
                                            c = new Color(lastBgColor);
                                        }
                                    }
                                }
                                {
                                    _prof.prof.cnt[683]++;
                                    g.setColor(c);
                                }
                                {
                                    _prof.prof.cnt[684]++;
                                    g.setComposite(AlphaComposite.Src);
                                }
                                {
                                    _prof.prof.cnt[685]++;
                                    g.fill(lastRect);
                                }
                                {
                                    _prof.prof.cnt[686]++;
                                    g.dispose();
                                }
                            }
                        }
                    }
                }
            }
        }
        _prof.prof.cnt[687]++;
        int pass = 1;
        _prof.prof.cnt[688]++;
        int inc = 8;
        _prof.prof.cnt[689]++;
        int iline = 0;
        {
            _prof.prof.cnt[690]++;
            for (int i = 0; i < ih; i++) {
                _prof.prof.cnt[691]++;
                int line = i;
                {
                    _prof.prof.cnt[692]++;
                    if (interlace) {
                        {
                            _prof.prof.cnt[693]++;
                            if (iline >= ih) {
                                {
                                    _prof.prof.cnt[694]++;
                                    pass++;
                                }
                                {
                                    _prof.prof.cnt[695]++;
                                    switch(pass) {
                                        case 2:
                                            {
                                                _prof.prof.cnt[696]++;
                                                iline = 4;
                                            }
                                            {
                                                _prof.prof.cnt[697]++;
                                                break;
                                            }
                                        case 3:
                                            {
                                                _prof.prof.cnt[698]++;
                                                iline = 2;
                                            }
                                            {
                                                _prof.prof.cnt[699]++;
                                                inc = 4;
                                            }
                                            {
                                                _prof.prof.cnt[700]++;
                                                break;
                                            }
                                        case 4:
                                            {
                                                _prof.prof.cnt[701]++;
                                                iline = 1;
                                            }
                                            {
                                                _prof.prof.cnt[702]++;
                                                inc = 2;
                                            }
                                    }
                                }
                            }
                        }
                        {
                            _prof.prof.cnt[703]++;
                            line = iline;
                        }
                        {
                            _prof.prof.cnt[704]++;
                            iline += inc;
                        }
                    }
                }
                {
                    _prof.prof.cnt[705]++;
                    line += iy;
                }
                {
                    _prof.prof.cnt[706]++;
                    if (line < height) {
                        _prof.prof.cnt[707]++;
                        int k = line * width;
                        _prof.prof.cnt[708]++;
                        int dx = k + ix;
                        _prof.prof.cnt[709]++;
                        int dlim = dx + iw;
                        {
                            _prof.prof.cnt[710]++;
                            if ((k + width) < dlim) {
                                {
                                    _prof.prof.cnt[711]++;
                                    dlim = k + width;
                                }
                            }
                        }
                        _prof.prof.cnt[712]++;
                        int sx = i * iw;
                        {
                            _prof.prof.cnt[713]++;
                            while (dx < dlim) {
                                _prof.prof.cnt[714]++;
                                int index = ((int) pixels[sx++]) & 0xff;
                                _prof.prof.cnt[715]++;
                                int c = act[index];
                                {
                                    _prof.prof.cnt[716]++;
                                    if (c != 0) {
                                        {
                                            _prof.prof.cnt[717]++;
                                            dest[dx] = c;
                                        }
                                    }
                                }
                                {
                                    _prof.prof.cnt[718]++;
                                    dx++;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
	 * Gets the image contents of frame n.
	 *
	 * @return BufferedImage representation of frame, or null if n is invalid.
	 */
    public BufferedImage getFrame(int n) {
        _prof.prof.cnt[719]++;
        _prof.prof.cnt[720]++;
        BufferedImage im = null;
        {
            _prof.prof.cnt[721]++;
            if ((n >= 0) && (n < frameCount)) {
                {
                    _prof.prof.cnt[722]++;
                    im = ((GifFrame) frames.get(n)).image;
                }
            }
        }
        {
            _prof.prof.cnt[723]++;
            return im;
        }
    }

    /**
	 * Gets image size.
	 *
	 * @return GIF image dimensions
	 */
    public Dimension getFrameSize() {
        _prof.prof.cnt[724]++;
        {
            _prof.prof.cnt[725]++;
            return new Dimension(width, height);
        }
    }

    /**
	 * Reads GIF image from stream
	 *
	 * @param BufferedInputStream containing GIF file.
	 * @return read status code (0 = no errors)
	 */
    public int read(BufferedInputStream is) {
        _prof.prof.cnt[726]++;
        {
            _prof.prof.cnt[727]++;
            init();
        }
        {
            _prof.prof.cnt[728]++;
            if (is != null) {
                {
                    _prof.prof.cnt[729]++;
                    in = is;
                }
                {
                    _prof.prof.cnt[730]++;
                    readHeader();
                }
                {
                    _prof.prof.cnt[731]++;
                    if (!err()) {
                        {
                            _prof.prof.cnt[732]++;
                            readContents();
                        }
                        {
                            _prof.prof.cnt[733]++;
                            if (frameCount < 0) {
                                {
                                    _prof.prof.cnt[734]++;
                                    status = STATUS_FORMAT_ERROR;
                                }
                            }
                        }
                    }
                }
            } else {
                {
                    _prof.prof.cnt[735]++;
                    status = STATUS_OPEN_ERROR;
                }
            }
        }
        {
            _prof.prof.cnt[736]++;
            try {
                {
                    _prof.prof.cnt[737]++;
                    is.close();
                }
            } catch (IOException e) {
            }
        }
        {
            _prof.prof.cnt[738]++;
            return status;
        }
    }

    /**
	 * Reads GIF image from stream
	 *
	 * @param InputStream containing GIF file.
	 * @return read status code (0 = no errors)
	 */
    public int read(InputStream is) {
        _prof.prof.cnt[739]++;
        {
            _prof.prof.cnt[740]++;
            init();
        }
        {
            _prof.prof.cnt[741]++;
            if (is != null) {
                {
                    _prof.prof.cnt[742]++;
                    if (!(is instanceof BufferedInputStream)) {
                        _prof.prof.cnt[743]++;
                        is = new BufferedInputStream(is);
                    }
                }
                {
                    _prof.prof.cnt[744]++;
                    in = (BufferedInputStream) is;
                }
                {
                    _prof.prof.cnt[745]++;
                    readHeader();
                }
                {
                    _prof.prof.cnt[746]++;
                    if (!err()) {
                        {
                            _prof.prof.cnt[747]++;
                            readContents();
                        }
                        {
                            _prof.prof.cnt[748]++;
                            if (frameCount < 0) {
                                {
                                    _prof.prof.cnt[749]++;
                                    status = STATUS_FORMAT_ERROR;
                                }
                            }
                        }
                    }
                }
            } else {
                {
                    _prof.prof.cnt[750]++;
                    status = STATUS_OPEN_ERROR;
                }
            }
        }
        {
            _prof.prof.cnt[751]++;
            try {
                {
                    _prof.prof.cnt[752]++;
                    is.close();
                }
            } catch (IOException e) {
            }
        }
        {
            _prof.prof.cnt[753]++;
            return status;
        }
    }

    /**
	 * Reads GIF file from specified file/URL source
	 * (URL assumed if name contains ":/" or "file:")
	 *
	 * @param name String containing source
	 * @return read status code (0 = no errors)
	 */
    public int read(String name) {
        _prof.prof.cnt[754]++;
        {
            _prof.prof.cnt[755]++;
            status = STATUS_OK;
        }
        {
            _prof.prof.cnt[756]++;
            try {
                {
                    _prof.prof.cnt[757]++;
                    name = name.trim().toLowerCase();
                }
                {
                    _prof.prof.cnt[758]++;
                    if ((name.indexOf("file:") >= 0) || (name.indexOf(":/") > 0)) {
                        _prof.prof.cnt[759]++;
                        URL url = new URL(name);
                        {
                            _prof.prof.cnt[760]++;
                            in = new BufferedInputStream(url.openStream());
                        }
                    } else {
                        {
                            _prof.prof.cnt[761]++;
                            in = new BufferedInputStream(new FileInputStream(name));
                        }
                    }
                }
                {
                    _prof.prof.cnt[762]++;
                    status = read(in);
                }
            } catch (IOException e) {
                {
                    _prof.prof.cnt[763]++;
                    status = STATUS_OPEN_ERROR;
                }
            }
        }
        {
            _prof.prof.cnt[764]++;
            return status;
        }
    }

    /**
	 * Decodes LZW image data into pixel array.
	 * Adapted from John Cristy's ImageMagick.
	 */
    protected void decodeImageData() {
        _prof.prof.cnt[765]++;
        _prof.prof.cnt[766]++;
        int NullCode = -1;
        _prof.prof.cnt[767]++;
        int npix = iw * ih;
        _prof.prof.cnt[768]++;
        int available, clear, code_mask, code_size, end_of_information, in_code, old_code, bits, code, count, i, datum, data_size, first, top, bi, pi;
        {
            _prof.prof.cnt[769]++;
            if ((pixels == null) || (pixels.length < npix)) {
                {
                    _prof.prof.cnt[770]++;
                    pixels = new byte[npix];
                }
            }
        }
        {
            _prof.prof.cnt[771]++;
            if (prefix == null) {
                _prof.prof.cnt[772]++;
                prefix = new short[MaxStackSize];
            }
        }
        {
            _prof.prof.cnt[773]++;
            if (suffix == null) {
                _prof.prof.cnt[774]++;
                suffix = new byte[MaxStackSize];
            }
        }
        {
            _prof.prof.cnt[775]++;
            if (pixelStack == null) {
                _prof.prof.cnt[776]++;
                pixelStack = new byte[MaxStackSize + 1];
            }
        }
        {
            _prof.prof.cnt[777]++;
            data_size = read();
        }
        {
            _prof.prof.cnt[778]++;
            clear = 1 << data_size;
        }
        {
            _prof.prof.cnt[779]++;
            end_of_information = clear + 1;
        }
        {
            _prof.prof.cnt[780]++;
            available = clear + 2;
        }
        {
            _prof.prof.cnt[781]++;
            old_code = NullCode;
        }
        {
            _prof.prof.cnt[782]++;
            code_size = data_size + 1;
        }
        {
            _prof.prof.cnt[783]++;
            code_mask = (1 << code_size) - 1;
        }
        {
            _prof.prof.cnt[784]++;
            for (code = 0; code < clear; code++) {
                {
                    _prof.prof.cnt[785]++;
                    prefix[code] = 0;
                }
                {
                    _prof.prof.cnt[786]++;
                    suffix[code] = (byte) code;
                }
            }
        }
        {
            _prof.prof.cnt[787]++;
            datum = bits = count = first = top = pi = bi = 0;
        }
        {
            _prof.prof.cnt[788]++;
            for (i = 0; i < npix; ) {
                {
                    _prof.prof.cnt[789]++;
                    if (top == 0) {
                        {
                            _prof.prof.cnt[790]++;
                            if (bits < code_size) {
                                {
                                    _prof.prof.cnt[791]++;
                                    if (count == 0) {
                                        {
                                            _prof.prof.cnt[792]++;
                                            count = readBlock();
                                        }
                                        {
                                            _prof.prof.cnt[793]++;
                                            if (count <= 0) {
                                                _prof.prof.cnt[794]++;
                                                break;
                                            }
                                        }
                                        {
                                            _prof.prof.cnt[795]++;
                                            bi = 0;
                                        }
                                    }
                                }
                                {
                                    _prof.prof.cnt[796]++;
                                    datum += (((int) block[bi]) & 0xff) << bits;
                                }
                                {
                                    _prof.prof.cnt[797]++;
                                    bits += 8;
                                }
                                {
                                    _prof.prof.cnt[798]++;
                                    bi++;
                                }
                                {
                                    _prof.prof.cnt[799]++;
                                    count--;
                                }
                                {
                                    _prof.prof.cnt[800]++;
                                    continue;
                                }
                            }
                        }
                        {
                            _prof.prof.cnt[801]++;
                            code = datum & code_mask;
                        }
                        {
                            _prof.prof.cnt[802]++;
                            datum >>= code_size;
                        }
                        {
                            _prof.prof.cnt[803]++;
                            bits -= code_size;
                        }
                        {
                            _prof.prof.cnt[804]++;
                            if ((code > available) || (code == end_of_information)) {
                                _prof.prof.cnt[805]++;
                                break;
                            }
                        }
                        {
                            _prof.prof.cnt[806]++;
                            if (code == clear) {
                                {
                                    _prof.prof.cnt[807]++;
                                    code_size = data_size + 1;
                                }
                                {
                                    _prof.prof.cnt[808]++;
                                    code_mask = (1 << code_size) - 1;
                                }
                                {
                                    _prof.prof.cnt[809]++;
                                    available = clear + 2;
                                }
                                {
                                    _prof.prof.cnt[810]++;
                                    old_code = NullCode;
                                }
                                {
                                    _prof.prof.cnt[811]++;
                                    continue;
                                }
                            }
                        }
                        {
                            _prof.prof.cnt[812]++;
                            if (old_code == NullCode) {
                                {
                                    _prof.prof.cnt[813]++;
                                    pixelStack[top++] = suffix[code];
                                }
                                {
                                    _prof.prof.cnt[814]++;
                                    old_code = code;
                                }
                                {
                                    _prof.prof.cnt[815]++;
                                    first = code;
                                }
                                {
                                    _prof.prof.cnt[816]++;
                                    continue;
                                }
                            }
                        }
                        {
                            _prof.prof.cnt[817]++;
                            in_code = code;
                        }
                        {
                            _prof.prof.cnt[818]++;
                            if (code == available) {
                                {
                                    _prof.prof.cnt[819]++;
                                    pixelStack[top++] = (byte) first;
                                }
                                {
                                    _prof.prof.cnt[820]++;
                                    code = old_code;
                                }
                            }
                        }
                        {
                            _prof.prof.cnt[821]++;
                            while (code > clear) {
                                {
                                    _prof.prof.cnt[822]++;
                                    pixelStack[top++] = suffix[code];
                                }
                                {
                                    _prof.prof.cnt[823]++;
                                    code = prefix[code];
                                }
                            }
                        }
                        {
                            _prof.prof.cnt[824]++;
                            first = ((int) suffix[code]) & 0xff;
                        }
                        {
                            _prof.prof.cnt[825]++;
                            if (available >= MaxStackSize) {
                                _prof.prof.cnt[826]++;
                                break;
                            }
                        }
                        {
                            _prof.prof.cnt[827]++;
                            pixelStack[top++] = (byte) first;
                        }
                        {
                            _prof.prof.cnt[828]++;
                            prefix[available] = (short) old_code;
                        }
                        {
                            _prof.prof.cnt[829]++;
                            suffix[available] = (byte) first;
                        }
                        {
                            _prof.prof.cnt[830]++;
                            available++;
                        }
                        {
                            _prof.prof.cnt[831]++;
                            if (((available & code_mask) == 0) && (available < MaxStackSize)) {
                                {
                                    _prof.prof.cnt[832]++;
                                    code_size++;
                                }
                                {
                                    _prof.prof.cnt[833]++;
                                    code_mask += available;
                                }
                            }
                        }
                        {
                            _prof.prof.cnt[834]++;
                            old_code = in_code;
                        }
                    }
                }
                {
                    _prof.prof.cnt[835]++;
                    top--;
                }
                {
                    _prof.prof.cnt[836]++;
                    pixels[pi++] = pixelStack[top];
                }
                {
                    _prof.prof.cnt[837]++;
                    i++;
                }
            }
        }
        {
            _prof.prof.cnt[838]++;
            for (i = pi; i < npix; i++) {
                {
                    _prof.prof.cnt[839]++;
                    pixels[i] = 0;
                }
            }
        }
    }

    /**
	 * Returns true if an error was encountered during reading/decoding
	 */
    protected boolean err() {
        _prof.prof.cnt[840]++;
        {
            _prof.prof.cnt[841]++;
            return status != STATUS_OK;
        }
    }

    /**
	 * Initializes or re-initializes reader
	 */
    protected void init() {
        _prof.prof.cnt[842]++;
        {
            _prof.prof.cnt[843]++;
            status = STATUS_OK;
        }
        {
            _prof.prof.cnt[844]++;
            frameCount = 0;
        }
        {
            _prof.prof.cnt[845]++;
            frames = new ArrayList();
        }
        {
            _prof.prof.cnt[846]++;
            gct = null;
        }
        {
            _prof.prof.cnt[847]++;
            lct = null;
        }
    }

    /**
	 * Reads a single byte from the input stream.
	 */
    protected int read() {
        _prof.prof.cnt[848]++;
        _prof.prof.cnt[849]++;
        int curByte = 0;
        {
            _prof.prof.cnt[850]++;
            try {
                {
                    _prof.prof.cnt[851]++;
                    curByte = in.read();
                }
            } catch (IOException e) {
                {
                    _prof.prof.cnt[852]++;
                    status = STATUS_FORMAT_ERROR;
                }
            }
        }
        {
            _prof.prof.cnt[853]++;
            return curByte;
        }
    }

    /**
	 * Reads next variable length block from input.
	 *
	 * @return number of bytes stored in "buffer"
	 */
    protected int readBlock() {
        _prof.prof.cnt[854]++;
        {
            _prof.prof.cnt[855]++;
            blockSize = read();
        }
        _prof.prof.cnt[856]++;
        int n = 0;
        {
            _prof.prof.cnt[857]++;
            if (blockSize > 0) {
                {
                    _prof.prof.cnt[858]++;
                    try {
                        _prof.prof.cnt[859]++;
                        int count = 0;
                        {
                            _prof.prof.cnt[860]++;
                            while (n < blockSize) {
                                {
                                    _prof.prof.cnt[861]++;
                                    count = in.read(block, n, blockSize - n);
                                }
                                {
                                    _prof.prof.cnt[862]++;
                                    if (count == -1) {
                                        _prof.prof.cnt[863]++;
                                        break;
                                    }
                                }
                                {
                                    _prof.prof.cnt[864]++;
                                    n += count;
                                }
                            }
                        }
                    } catch (IOException e) {
                    }
                }
                {
                    _prof.prof.cnt[865]++;
                    if (n < blockSize) {
                        {
                            _prof.prof.cnt[866]++;
                            status = STATUS_FORMAT_ERROR;
                        }
                    }
                }
            }
        }
        {
            _prof.prof.cnt[867]++;
            return n;
        }
    }

    /**
	 * Reads color table as 256 RGB integer values
	 *
	 * @param ncolors int number of colors to read
	 * @return int array containing 256 colors (packed ARGB with full alpha)
	 */
    protected int[] readColorTable(int ncolors) {
        _prof.prof.cnt[868]++;
        _prof.prof.cnt[869]++;
        int nbytes = 3 * ncolors;
        _prof.prof.cnt[870]++;
        int[] tab = null;
        _prof.prof.cnt[871]++;
        byte[] c = new byte[nbytes];
        _prof.prof.cnt[872]++;
        int n = 0;
        {
            _prof.prof.cnt[873]++;
            try {
                {
                    _prof.prof.cnt[874]++;
                    n = in.read(c);
                }
            } catch (IOException e) {
            }
        }
        {
            _prof.prof.cnt[875]++;
            if (n < nbytes) {
                {
                    _prof.prof.cnt[876]++;
                    status = STATUS_FORMAT_ERROR;
                }
            } else {
                {
                    _prof.prof.cnt[877]++;
                    tab = new int[256];
                }
                _prof.prof.cnt[878]++;
                int i = 0;
                _prof.prof.cnt[879]++;
                int j = 0;
                {
                    _prof.prof.cnt[880]++;
                    while (i < ncolors) {
                        _prof.prof.cnt[881]++;
                        int r = ((int) c[j++]) & 0xff;
                        _prof.prof.cnt[882]++;
                        int g = ((int) c[j++]) & 0xff;
                        _prof.prof.cnt[883]++;
                        int b = ((int) c[j++]) & 0xff;
                        {
                            _prof.prof.cnt[884]++;
                            tab[i++] = 0xff000000 | (r << 16) | (g << 8) | b;
                        }
                    }
                }
            }
        }
        {
            _prof.prof.cnt[885]++;
            return tab;
        }
    }

    /**
	 * Main file parser.  Reads GIF content blocks.
	 */
    protected void readContents() {
        _prof.prof.cnt[886]++;
        _prof.prof.cnt[887]++;
        boolean done = false;
        {
            _prof.prof.cnt[888]++;
            while (!(done || err())) {
                _prof.prof.cnt[889]++;
                int code = read();
                {
                    _prof.prof.cnt[890]++;
                    switch(code) {
                        case 0x2C:
                            {
                                _prof.prof.cnt[891]++;
                                readImage();
                            }
                            {
                                _prof.prof.cnt[892]++;
                                break;
                            }
                        case 0x21:
                            {
                                _prof.prof.cnt[893]++;
                                code = read();
                            }
                            {
                                _prof.prof.cnt[894]++;
                                switch(code) {
                                    case 0xf9:
                                        {
                                            _prof.prof.cnt[895]++;
                                            readGraphicControlExt();
                                        }
                                        {
                                            _prof.prof.cnt[896]++;
                                            break;
                                        }
                                    case 0xff:
                                        {
                                            _prof.prof.cnt[897]++;
                                            readBlock();
                                        }
                                        _prof.prof.cnt[898]++;
                                        String app = "";
                                        {
                                            _prof.prof.cnt[899]++;
                                            for (int i = 0; i < 11; i++) {
                                                {
                                                    _prof.prof.cnt[900]++;
                                                    app += (char) block[i];
                                                }
                                            }
                                        }
                                        {
                                            _prof.prof.cnt[901]++;
                                            if (app.equals("NETSCAPE2.0")) {
                                                {
                                                    _prof.prof.cnt[902]++;
                                                    readNetscapeExt();
                                                }
                                            } else {
                                                _prof.prof.cnt[903]++;
                                                skip();
                                            }
                                        }
                                        {
                                            _prof.prof.cnt[904]++;
                                            break;
                                        }
                                    default:
                                        {
                                            _prof.prof.cnt[905]++;
                                            skip();
                                        }
                                }
                            }
                            {
                                _prof.prof.cnt[906]++;
                                break;
                            }
                        case 0x3b:
                            {
                                _prof.prof.cnt[907]++;
                                done = true;
                            }
                            {
                                _prof.prof.cnt[908]++;
                                break;
                            }
                        case 0x00:
                            {
                                _prof.prof.cnt[909]++;
                                break;
                            }
                        default:
                            {
                                _prof.prof.cnt[910]++;
                                status = STATUS_FORMAT_ERROR;
                            }
                    }
                }
            }
        }
    }

    /**
	 * Reads Graphics Control Extension values
	 */
    protected void readGraphicControlExt() {
        _prof.prof.cnt[911]++;
        {
            _prof.prof.cnt[912]++;
            read();
        }
        _prof.prof.cnt[913]++;
        int packed = read();
        {
            _prof.prof.cnt[914]++;
            dispose = (packed & 0x1c) >> 2;
        }
        {
            _prof.prof.cnt[915]++;
            if (dispose == 0) {
                {
                    _prof.prof.cnt[916]++;
                    dispose = 1;
                }
            }
        }
        {
            _prof.prof.cnt[917]++;
            transparency = (packed & 1) != 0;
        }
        {
            _prof.prof.cnt[918]++;
            delay = readShort() * 10;
        }
        {
            _prof.prof.cnt[919]++;
            transIndex = read();
        }
        {
            _prof.prof.cnt[920]++;
            read();
        }
    }

    /**
	 * Reads GIF file header information.
	 */
    protected void readHeader() {
        _prof.prof.cnt[921]++;
        _prof.prof.cnt[922]++;
        String id = "";
        {
            _prof.prof.cnt[923]++;
            for (int i = 0; i < 6; i++) {
                {
                    _prof.prof.cnt[924]++;
                    id += (char) read();
                }
            }
        }
        {
            _prof.prof.cnt[925]++;
            if (!id.startsWith("GIF")) {
                {
                    _prof.prof.cnt[926]++;
                    status = STATUS_FORMAT_ERROR;
                }
                {
                    _prof.prof.cnt[927]++;
                    return;
                }
            }
        }
        {
            _prof.prof.cnt[928]++;
            readLSD();
        }
        {
            _prof.prof.cnt[929]++;
            if (gctFlag && !err()) {
                {
                    _prof.prof.cnt[930]++;
                    gct = readColorTable(gctSize);
                }
                {
                    _prof.prof.cnt[931]++;
                    bgColor = gct[bgIndex];
                }
            }
        }
    }

    /**
	 * Reads next frame image
	 */
    protected void readImage() {
        _prof.prof.cnt[932]++;
        {
            _prof.prof.cnt[933]++;
            ix = readShort();
        }
        {
            _prof.prof.cnt[934]++;
            iy = readShort();
        }
        {
            _prof.prof.cnt[935]++;
            iw = readShort();
        }
        {
            _prof.prof.cnt[936]++;
            ih = readShort();
        }
        _prof.prof.cnt[937]++;
        int packed = read();
        {
            _prof.prof.cnt[938]++;
            lctFlag = (packed & 0x80) != 0;
        }
        {
            _prof.prof.cnt[939]++;
            interlace = (packed & 0x40) != 0;
        }
        {
            _prof.prof.cnt[940]++;
            lctSize = 2 << (packed & 7);
        }
        {
            _prof.prof.cnt[941]++;
            if (lctFlag) {
                {
                    _prof.prof.cnt[942]++;
                    lct = readColorTable(lctSize);
                }
                {
                    _prof.prof.cnt[943]++;
                    act = lct;
                }
            } else {
                {
                    _prof.prof.cnt[944]++;
                    act = gct;
                }
                {
                    _prof.prof.cnt[945]++;
                    if (bgIndex == transIndex) {
                        _prof.prof.cnt[946]++;
                        bgColor = 0;
                    }
                }
            }
        }
        _prof.prof.cnt[947]++;
        int save = 0;
        {
            _prof.prof.cnt[948]++;
            if (transparency) {
                {
                    _prof.prof.cnt[949]++;
                    save = act[transIndex];
                }
                {
                    _prof.prof.cnt[950]++;
                    act[transIndex] = 0;
                }
            }
        }
        {
            _prof.prof.cnt[951]++;
            if (act == null) {
                {
                    _prof.prof.cnt[952]++;
                    status = STATUS_FORMAT_ERROR;
                }
            }
        }
        {
            _prof.prof.cnt[953]++;
            if (err()) {
                _prof.prof.cnt[954]++;
                return;
            }
        }
        {
            _prof.prof.cnt[955]++;
            decodeImageData();
        }
        {
            _prof.prof.cnt[956]++;
            skip();
        }
        {
            _prof.prof.cnt[957]++;
            if (err()) {
                _prof.prof.cnt[958]++;
                return;
            }
        }
        {
            _prof.prof.cnt[959]++;
            frameCount++;
        }
        {
            _prof.prof.cnt[960]++;
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        }
        {
            _prof.prof.cnt[961]++;
            setPixels();
        }
        {
            _prof.prof.cnt[962]++;
            frames.add(new GifFrame(image, delay));
        }
        {
            _prof.prof.cnt[963]++;
            if (transparency) {
                {
                    _prof.prof.cnt[964]++;
                    act[transIndex] = save;
                }
            }
        }
        {
            _prof.prof.cnt[965]++;
            resetFrame();
        }
    }

    /**
	 * Reads Logical Screen Descriptor
	 */
    protected void readLSD() {
        _prof.prof.cnt[966]++;
        {
            _prof.prof.cnt[967]++;
            width = readShort();
        }
        {
            _prof.prof.cnt[968]++;
            height = readShort();
        }
        _prof.prof.cnt[969]++;
        int packed = read();
        {
            _prof.prof.cnt[970]++;
            gctFlag = (packed & 0x80) != 0;
        }
        {
            _prof.prof.cnt[971]++;
            gctSize = 2 << (packed & 7);
        }
        {
            _prof.prof.cnt[972]++;
            bgIndex = read();
        }
        {
            _prof.prof.cnt[973]++;
            pixelAspect = read();
        }
    }

    /**
	 * Reads Netscape extenstion to obtain iteration count
	 */
    protected void readNetscapeExt() {
        _prof.prof.cnt[974]++;
        {
            _prof.prof.cnt[975]++;
            do {
                {
                    _prof.prof.cnt[976]++;
                    readBlock();
                }
                {
                    _prof.prof.cnt[977]++;
                    if (block[0] == 1) {
                        _prof.prof.cnt[978]++;
                        int b1 = ((int) block[1]) & 0xff;
                        _prof.prof.cnt[979]++;
                        int b2 = ((int) block[2]) & 0xff;
                        {
                            _prof.prof.cnt[980]++;
                            loopCount = (b2 << 8) | b1;
                        }
                    }
                }
            } while ((blockSize > 0) && !err());
        }
    }

    /**
	 * Reads next 16-bit value, LSB first
	 */
    protected int readShort() {
        _prof.prof.cnt[981]++;
        {
            _prof.prof.cnt[982]++;
            return read() | (read() << 8);
        }
    }

    /**
	 * Resets frame state for reading next image.
	 */
    protected void resetFrame() {
        _prof.prof.cnt[983]++;
        {
            _prof.prof.cnt[984]++;
            lastDispose = dispose;
        }
        {
            _prof.prof.cnt[985]++;
            lastRect = new Rectangle(ix, iy, iw, ih);
        }
        {
            _prof.prof.cnt[986]++;
            lastImage = image;
        }
        {
            _prof.prof.cnt[987]++;
            lastBgColor = bgColor;
        }
        _prof.prof.cnt[988]++;
        int dispose = 0;
        _prof.prof.cnt[989]++;
        boolean transparency = false;
        _prof.prof.cnt[990]++;
        int delay = 0;
        {
            _prof.prof.cnt[991]++;
            lct = null;
        }
    }

    /**
	 * Skips variable length blocks up to and including
	 * next zero length block.
	 */
    protected void skip() {
        _prof.prof.cnt[992]++;
        {
            _prof.prof.cnt[993]++;
            do {
                {
                    _prof.prof.cnt[994]++;
                    readBlock();
                }
            } while ((blockSize > 0) && !err());
        }
    }
}
