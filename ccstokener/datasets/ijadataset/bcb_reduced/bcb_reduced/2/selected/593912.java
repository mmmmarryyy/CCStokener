package org.in4ama.editor.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Some usefull methods for reading URLs or streams and returning the data as
 * Direct Buffers
 * 
 * @author Ivan Z. Ganza
 * @author Robert Schuster
 * @author Bart LEBOEUF
 * @version $Id: BufferUtil.java,v 1.5 2005/02/19 02:00:36 ivan_ganza Exp $
 */
public class BufferUtil {

    private static final int BUFFER_SIZE = 1024;

    /**
	 * Tries to open the given URL, get its input stream, returns the data in a
	 * <I><B>direct</I></B> ByteBuffer
	 * 
	 * @param url
	 *            an <code>URL</code> value
	 * @return a <code>ByteBuffer</code> value with the contacts of the data
	 *         present at URL
	 * @exception IOException
	 *                if an error occurs
	 * @exception MalformedURLException
	 *                if an error occurs
	 */
    public static ByteBuffer readURL(URL url) throws IOException, MalformedURLException {
        URLConnection connection = null;
        try {
            connection = url.openConnection();
            return readInputStream(new BufferedInputStream(connection.getInputStream()));
        } catch (IOException e) {
            throw e;
        }
    }

    /**
	 * Fully reads the given InputStream, returning its contents as a ByteBuffer
	 * 
	 * @param in
	 *            an <code>InputStream</code> value
	 * @return a <code>ByteBuffer</code> value
	 * @exception IOException
	 *                if an error occurs
	 */
    public static ByteBuffer readInputStream(InputStream in) throws IOException {
        ReadableByteChannel ch = Channels.newChannel(in);
        List list = new LinkedList();
        int sum = 0, read = 0;
        do {
            ByteBuffer b = createByteBuffer(BUFFER_SIZE);
            read = ch.read(b);
            if (read > 0) {
                b.flip();
                list.add(b);
                sum += read;
            }
        } while (read != -1);
        if (list.size() == 1) {
            return (ByteBuffer) list.get(0);
        }
        ByteBuffer bb = createByteBuffer(sum);
        Iterator ite = list.iterator();
        while (ite.hasNext()) {
            bb.put((ByteBuffer) ite.next());
        }
        list.clear();
        return bb;
    }

    public static ByteBuffer createByteBuffer(int capacity) {
        return ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
    }

    public static ByteBuffer createByteBuffer(byte[] values) {
        return createByteBuffer(values.length).put(values);
    }

    public static FloatBuffer createFloatBuffer(int capacity) {
        return createByteBuffer(capacity * 4).asFloatBuffer();
    }

    public static FloatBuffer createFloatBuffer(float[] values) {
        return createFloatBuffer(values.length).put(values);
    }

    public static IntBuffer createIntBuffer(int capacity) {
        return createByteBuffer(capacity * 4).asIntBuffer();
    }

    public static IntBuffer createIntBuffer(int[] values) {
        return createIntBuffer(values.length).put(values);
    }

    public static DoubleBuffer createDoubleBuffer(int capacity) {
        return createByteBuffer(capacity * 8).asDoubleBuffer();
    }

    public static DoubleBuffer createDoubleBuffer(double[] values) {
        return createDoubleBuffer(values.length).put(values);
    }

    public static LongBuffer createLongBuffer(int capacity) {
        return createByteBuffer(capacity * 8).asLongBuffer();
    }

    public static LongBuffer createLongBuffer(long[] values) {
        return createLongBuffer(values.length).put(values);
    }

    public static ShortBuffer createShortBuffer(int capacity) {
        return createByteBuffer(capacity * 2).asShortBuffer();
    }

    public static ShortBuffer createShortBuffer(short[] values) {
        return createShortBuffer(values.length).put(values);
    }

    public static CharBuffer createCharBuffer(int capacity) {
        return createByteBuffer(capacity * 2).asCharBuffer();
    }

    public static CharBuffer createCharBuffer(char[] values) {
        return createCharBuffer(values.length).put(values);
    }
}
