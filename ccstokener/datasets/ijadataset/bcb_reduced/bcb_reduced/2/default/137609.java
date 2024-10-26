import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.*;
import java.net.URL;
import java.text.*;
import javax.media.jai.*;
import javax.swing.JOptionPane;

public class Tools implements Comparable {

    public static final int BASE = 1;

    public static final int OUT = 2;

    public static final int THUMB = 4;

    public static final int EXCLUDE = 8;

    public static final String OUT_PREFIX = "~out";

    public static final String THUMB_PREFIX = "~thumb";

    public static final String EXCLUDE_PREFIX = "~ex";

    private static final int pngTextType = 0x74455874;

    private static final DateFormat exifDf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

    private File src;

    private File parent;

    private String imageName;

    private String extension;

    private int type;

    private PlanarImage image;

    private PlanarImage target;

    private float imageQuality = .9f;

    public Tools(File f) {
        setFile(f);
    }

    private void setFile(File f) {
        src = f;
        parent = f.getParentFile();
        String name = f.getName();
        int index = name.lastIndexOf('.');
        extension = name.substring(index + 1);
        name = name.substring(0, index);
        index = name.indexOf('_');
        type = BASE;
        if (index > 0) {
            String prefix = name.substring(0, index).toLowerCase();
            if (prefix.equals(OUT_PREFIX)) type = OUT; else if (prefix.equals(THUMB_PREFIX)) type = THUMB; else if (prefix.equals(EXCLUDE_PREFIX)) type = EXCLUDE;
            if (type != BASE) imageName = name.substring(index + 1);
        }
        if (imageName == null) imageName = name;
        image = target = null;
    }

    public void setQuality(float q) {
        imageQuality = q;
    }

    public File getSourceFile() {
        return src;
    }

    public File getBaseFile() {
        return getBaseFile(null);
    }

    public File getBaseFile(File dir) {
        return new File(dir != null ? dir : parent, imageName + "." + extension);
    }

    public File getThumbFile() {
        return getThumbFile(null);
    }

    public File getThumbFile(File dir) {
        return new File(dir != null ? dir : parent, THUMB_PREFIX + "_" + imageName + "." + extension);
    }

    public File getOutFile() {
        return getOutFile(null);
    }

    public File getOutFile(File dir) {
        return new File(dir != null ? dir : parent, OUT_PREFIX + "_" + imageName + "." + extension);
    }

    public File getExcludeFile() {
        return getExcludeFile(null);
    }

    public File getExcludeFile(File dir) {
        return new File(dir != null ? dir : parent, EXCLUDE_PREFIX + "_" + imageName + "." + extension);
    }

    public String getName() {
        return imageName;
    }

    public int getType() {
        return type;
    }

    public String getExtension() {
        return extension;
    }

    public int getWidth() {
        if (image == null) return -1;
        return image.getWidth();
    }

    public int getHeight() {
        if (image == null) return -1;
        return image.getHeight();
    }

    public void load(File f) throws IOException {
        InputStream is = new com.sun.media.jai.codec.FileSeekableStream(f);
        dispose();
        try {
            image = JAI.create("fileload", f.toString());
        } finally {
            is.close();
        }
    }

    public void save(File f) throws IOException {
        save(f, false);
    }

    public void save(File f, boolean makeSrc) throws IOException {
        if (target != null) {
            int width = target.getWidth();
            int height = target.getHeight();
            if (width <= 0 || height <= 0) throw new IOException("Image not loaded!");
            BufferedImage bi = target.getAsBufferedImage();
            try {
                if (!extension.equalsIgnoreCase("jpg") && !extension.equalsIgnoreCase("jpeg")) ImageIOWrapper.write(bi, f, extension); else {
                    OutputStream os = new BufferedOutputStream(new FileOutputStream(f), 8192);
                    try {
                        com.sun.image.codec.jpeg.JPEGEncodeParam p = com.sun.image.codec.jpeg.JPEGCodec.getDefaultJPEGEncodeParam(bi);
                        p.setQuality(imageQuality, false);
                        com.sun.image.codec.jpeg.JPEGImageEncoder encoder = com.sun.image.codec.jpeg.JPEGCodec.createJPEGEncoder(os);
                        encoder.encode(bi, p);
                    } finally {
                        os.close();
                    }
                }
            } finally {
                bi.flush();
            }
            if (makeSrc) setFile(f);
        }
    }

    public void rotateRight() {
        rotate(Math.PI / 2);
    }

    public void rotateLeft() {
        rotate(-Math.PI / 2);
    }

    public void rotate(double theta) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add((float) 0);
        pb.add((float) 0);
        pb.add((float) theta);
        target = JAI.create("rotate", pb);
    }

    public void makeThumb(int area) {
        int width = image.getWidth();
        int height = image.getHeight();
        int a = width * height;
        double r = (double) area / (double) a;
        float zoom = (float) Math.sqrt(r);
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(zoom);
        pb.add(zoom);
        pb.add(0.0F);
        pb.add(0.0F);
        pb.add(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
        target = JAI.create("scale", pb);
    }

    public AlbumGen.ImageDefinition parseInfo() throws IOException {
        File f = getBaseFile();
        AlbumGen.ImageDefinition retVal = new AlbumGen.ImageDefinition();
        DataInputStream is = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
        try {
            if (extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("jpeg")) {
                short soi = is.readShort();
                if (soi != (short) 0xFFD8) throw new IOException("Expected FFD8, found " + Integer.toString(soi, 16));
                int marker = 0;
                while ((marker = is.readShort() & 0xffff) != 0xFFDA) {
                    int len = is.readShort() & 0xFFFF;
                    System.out.println("Marker: " + Integer.toHexString(marker));
                    System.out.println("Length: " + Integer.toHexString(len));
                    switch(marker) {
                        case 0xFFED:
                            {
                                byte[] buf = new byte[len - 2];
                                is.readFully(buf);
                                DataInputStream bais = new DataInputStream(new ByteArrayInputStream(buf));
                                String str = readNTString(bais);
                                skip(6, bais);
                                String utf = readVLString(bais);
                                skip(2, bais);
                                int bimSegmentSize = bais.readShort() & 0xFFFF;
                                while (bimSegmentSize > 0) {
                                    skip(2, bais);
                                    bimSegmentSize -= 2;
                                    int type = bais.read() & 0xFF;
                                    bimSegmentSize--;
                                    int size = bais.readShort() & 0xFFFF;
                                    bimSegmentSize -= 2;
                                    if (type == 0x78) {
                                        byte[] textBytes = new byte[size];
                                        bais.readFully(textBytes);
                                        String text = new String(textBytes);
                                        retVal.caption = text;
                                    } else {
                                        skip(size, bais);
                                    }
                                    bimSegmentSize -= size;
                                }
                                break;
                            }
                        case 0xFFE1:
                            {
                                skip(6, is);
                                int posFromTiffStart = 0;
                                int byteAlign = is.readShort() & 0xFFFF;
                                boolean networkOrder = byteAlign == 19789;
                                skip(2, is);
                                int offsetToFirstIFD = networkOrder ? is.readInt() : readIntelInt(is);
                                posFromTiffStart += 8;
                                skip(offsetToFirstIFD - 8, is);
                                posFromTiffStart += offsetToFirstIFD - 8;
                                int numEntries = networkOrder ? is.readShort() : readIntelShort(is);
                                posFromTiffStart += 2;
                                System.out.println("posFromTiffStart: " + posFromTiffStart);
                                for (int i = 0; i < numEntries; i++) {
                                    int tag = networkOrder ? is.readShort() : readIntelShort(is);
                                    int format = networkOrder ? is.readShort() : readIntelShort(is);
                                    int numComponents = networkOrder ? is.readInt() : readIntelInt(is);
                                    int data = networkOrder ? is.readInt() : readIntelInt(is);
                                    posFromTiffStart += 12;
                                    switch(tag) {
                                        case 0x0112:
                                            retVal.orientation = data;
                                            break;
                                        case 0x132:
                                            {
                                                int distance = data - posFromTiffStart;
                                                is.mark(distance + 30);
                                                skip(distance, is);
                                                String dateString = readNTString(is);
                                                try {
                                                    retVal.date = exifDf.parse(dateString);
                                                } catch (ParseException e) {
                                                    System.err.println("Unable to parse date: " + dateString);
                                                }
                                                is.reset();
                                            }
                                            break;
                                    }
                                }
                                skip(len - 2 - 6 - posFromTiffStart, is);
                                break;
                            }
                        default:
                            len -= 2;
                            skip(len, is);
                    }
                }
            } else if (extension.equalsIgnoreCase("png")) {
                skip(8, is);
                while (is.available() > 0) {
                    int len = is.readInt();
                    int type = is.readInt();
                    if (type == pngTextType) {
                        String keyword = readNTString(is);
                        len -= keyword.length() + 1;
                        if (keyword.equalsIgnoreCase("description")) {
                            byte[] textBytes = new byte[len];
                            is.readFully(textBytes);
                            String text = new String(textBytes);
                            retVal.caption = text;
                        }
                    }
                    skip(len, is);
                    skip(4, is);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            is.close();
        }
        return retVal;
    }

    private void skip(int n, InputStream is) throws IOException {
        while (n > 0) {
            n -= is.skip(n);
        }
    }

    private String readNTString(DataInputStream is) throws IOException {
        int b;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((b = is.read()) != 0) {
            baos.write(b);
        }
        return new String(baos.toByteArray());
    }

    private String readVLString(DataInputStream is) throws IOException {
        int len = is.read() & 0xff;
        byte[] bytes = new byte[len];
        is.readFully(bytes);
        return new String(bytes);
    }

    private int readIntelInt(DataInputStream is) throws IOException {
        int b;
        int val = (b = is.read() & 0xff);
        val |= (b = is.read() & 0xff) << 8;
        val |= (b = is.read() & 0xff) << 16;
        val |= (b = is.read() & 0xff) << 24;
        return val;
    }

    private int readIntelShort(DataInputStream is) throws IOException {
        int val = is.read() & 0xff;
        val |= (is.read() & 0xff) << 8;
        return val;
    }

    public void dispose() {
        if (image != null) {
            image.dispose();
            image = null;
        }
        if (target != null) {
            target.dispose();
            target = null;
        }
    }

    public int hashCode() {
        return imageName.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof Tools) {
            Tools that = (Tools) o;
            if (imageName.equals(that.imageName)) return parent.equals(that.parent);
        }
        return false;
    }

    public int compareTo(Object o) {
        return getBaseFile().compareTo(((Tools) o).getBaseFile());
    }

    public static void main(String[] args) {
        boolean rotateLeft = false;
        boolean rotateRight = false;
        boolean exclude = false;
        boolean reset = false;
        float quality = 0f;
        int thumbArea = 12000;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-rotl")) rotateLeft = true; else if (args[i].equals("-rotr")) rotateRight = true; else if (args[i].equals("-exclude")) exclude = true; else if (args[i].equals("-reset")) reset = true; else if (args[i].equals("-quality")) quality = Float.parseFloat(args[++i]); else if (args[i].equals("-area")) thumbArea = Integer.parseInt(args[++i]); else {
                File f = new File(args[i]);
                try {
                    Tools t = new Tools(f);
                    if (exclude) {
                        URL url = t.getClass().getResource("exclude.jpg");
                        InputStream is = url.openStream();
                        File dest = t.getExcludeFile();
                        OutputStream os = new FileOutputStream(dest);
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
                        os.close();
                        is.close();
                        t.getOutFile().delete();
                        t.getThumbFile().delete();
                        System.exit(0);
                    }
                    if (reset) {
                        t.getOutFile().delete();
                        t.getThumbFile().delete();
                        t.getExcludeFile().delete();
                        System.exit(0);
                    }
                    if (quality > 0) t.setQuality(quality);
                    if (t.getType() == Tools.THUMB || t.getType() == Tools.EXCLUDE) t.load(t.getBaseFile()); else t.load(t.getSourceFile());
                    File out = t.getOutFile();
                    if (rotateLeft) t.rotateLeft(); else if (rotateRight) t.rotateRight();
                    t.save(out);
                    t.getExcludeFile().delete();
                    t.getThumbFile().delete();
                    System.exit(0);
                } catch (Throwable e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "The operation could not be performed", "JPhotoAlbum", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            }
        }
    }
}
