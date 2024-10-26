package org.chessworks.common.javatools.io;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.chessworks.common.javatools.ExceptionHelper;
import org.chessworks.common.javatools.LogHelper;
import org.chessworks.common.javatools.io.codec.StreamDecoder;

public class IOHelper {

    public static final String EOL = System.getProperty("line.separator");

    public static final Charset ASCII = Charset.forName("ASCII");

    public static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

    public static final Charset LATIN = ISO_8859_1;

    public static final Charset UTF16 = Charset.forName("UTF-16");

    public static final Charset UTF8 = Charset.forName("UTF-8");

    public void doAppend(PrintWriter out) throws Exception {
    }

    public void doAppend(DataOutputStream out) throws Exception {
    }

    public void doRead(BufferedReader in) throws Exception {
    }

    public void doRead(DataInputStream in) throws Exception {
    }

    public void doWrite(PrintWriter out) throws Exception {
    }

    public void doWrite(DataOutputStream out) throws Exception {
    }

    public static <T> T readURL(URL url, StreamDecoder<T> codec) throws IOException {
        InputStream stream = null;
        try {
            stream = url.openStream();
            T data = codec.decode(stream);
            return data;
        } finally {
            IOHelper.closeQuietly(stream);
        }
    }

    /**
	 * Safely closes the I/O stream, logging any exceptions.
	 *
	 * @param stream
	 *            The stream to close.
	 */
    public static void closeQuietly(Closeable stream) {
        Logger logger = LogHelper.getCallerLogger();
        closeQuietly(stream, logger);
    }

    /**
	 * Safely closes the I/O stream, logging any exceptions.
	 *
	 * @param stream
	 *            The stream to close.
	 */
    public static void closeQuietly(Closeable stream, Logger logger) {
        if (stream == null) return;
        try {
            stream.close();
        } catch (Throwable e) {
            ExceptionHelper.finallyException(e, logger);
        }
    }

    /**
	 * Safely closes the I/O stream, logging any exceptions.
	 *
	 * @param stream
	 *            The stream to close.
	 */
    public static void closeQuietly(Closeable stream, Logger logger, Level logLevel) {
        if (stream == null) return;
        try {
            stream.close();
        } catch (Throwable e) {
            ExceptionHelper.finallyException(e, logger, logLevel);
        }
    }
}
