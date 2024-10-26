package gnu.java.awt.image;

import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Vector;

public abstract class ImageDecoder implements ImageProducer {

    Vector consumers = new Vector();

    String filename;

    URL url;

    byte[] data;

    int offset;

    int length;

    InputStream input;

    DataInput datainput;

    static {
    }

    public ImageDecoder(String filename) {
        this.filename = filename;
    }

    public ImageDecoder(URL url) {
        this.url = url;
    }

    public ImageDecoder(InputStream is) {
        this.input = is;
    }

    public ImageDecoder(DataInput datainput) {
        this.datainput = datainput;
    }

    public ImageDecoder(byte[] imagedata, int imageoffset, int imagelength) {
        data = imagedata;
        offset = imageoffset;
        length = imagelength;
    }

    public void addConsumer(ImageConsumer ic) {
        consumers.addElement(ic);
    }

    public boolean isConsumer(ImageConsumer ic) {
        return consumers.contains(ic);
    }

    public void removeConsumer(ImageConsumer ic) {
        consumers.removeElement(ic);
    }

    public void startProduction(ImageConsumer ic) {
        if (!isConsumer(ic)) addConsumer(ic);
        Vector list = (Vector) consumers.clone();
        try {
            if (input == null) {
                try {
                    if (url != null) input = url.openStream(); else if (datainput != null) input = new DataInputStreamWrapper(datainput); else {
                        if (filename != null) input = new FileInputStream(filename); else input = new ByteArrayInputStream(data, offset, length);
                    }
                    produce(list, input);
                } finally {
                    input = null;
                }
            } else {
                produce(list, input);
            }
        } catch (Exception e) {
            for (int i = 0; i < list.size(); i++) {
                ImageConsumer ic2 = (ImageConsumer) list.elementAt(i);
                ic2.imageComplete(ImageConsumer.IMAGEERROR);
            }
        }
    }

    public void requestTopDownLeftRightResend(ImageConsumer ic) {
    }

    public abstract void produce(Vector v, InputStream is) throws IOException;

    private static class DataInputStreamWrapper extends InputStream {

        private final DataInput datainput;

        DataInputStreamWrapper(DataInput datainput) {
            this.datainput = datainput;
        }

        public int read() throws IOException {
            try {
                return datainput.readByte() & 0xFF;
            } catch (EOFException eofe) {
                return -1;
            }
        }
    }
}
