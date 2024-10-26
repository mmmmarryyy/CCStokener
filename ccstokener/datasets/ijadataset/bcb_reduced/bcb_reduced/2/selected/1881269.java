package uk.ac.bath.ai.io;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

/**
 *
 * @author pjl
 */
public class SequentialTrainingFileReader {

    long pos;

    DataInputStream fis;

    private DATA key;

    private int ndim;

    private int dim[];

    private long size;

    public int[] getSizes() {
        return dim;
    }

    enum DATA {

        U8(0x08, 1), S89(0x09, 1), S16(0x0B, 2), U16(0x0C, 2), FLOAT(0x0D, 4), DOUBLE(0x0E, 8);

        private byte TYPE;

        private int size;

        DATA(int t, int size) {
            TYPE = (byte) t;
            this.size = size;
        }
    }

    ;

    public SequentialTrainingFileReader(URL url) throws IOException {
        this(new DataInputStream(url.openStream()));
    }

    public SequentialTrainingFileReader(File file) throws FileNotFoundException, IOException {
        this(new DataInputStream(new FileInputStream(file)));
    }

    public SequentialTrainingFileReader(DataInputStream fis) throws FileNotFoundException, IOException {
        byte buff[] = new byte[4];
        this.fis = fis;
        pos = 0;
        int cnt = fis.read(buff, 0, 4);
        assert (cnt == 4);
        byte akey = buff[2];
        for (DATA d : DATA.values()) {
            if (d.TYPE == akey) {
                key = d;
                break;
            }
        }
        System.out.println(" Type = " + key);
        ndim = buff[3];
        System.out.println(" Ndim = " + ndim);
        dim = new int[ndim];
        System.out.print(" Dims = [");
        for (int i = 0; i < ndim; i++) {
            dim[i] = fis.readInt();
            if (i > 0) {
                System.out.print(",");
            }
            System.out.print(dim[i]);
        }
        System.out.println("]");
        size = key.size;
        for (int i = 1; i < ndim; i++) {
            size = dim[i] * size;
        }
    }

    public Object getData(int index) throws Exception {
        assert (pos == index);
        pos++;
        Object ret = null;
        if (key.size == 1) {
            byte[][] o = new byte[dim[1]][dim[2]];
            for (int i = 0; i < dim[1]; i++) {
                fis.readFully(o[i], 0, dim[2]);
            }
            ret = o;
        }
        return ret;
    }

    public int getLabel(int index) throws Exception {
        if (key.size == 1) {
            return fis.readByte();
        }
        throw new Exception(" Data is " + key);
    }
}
