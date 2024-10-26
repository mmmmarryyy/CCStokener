package uk.org.ogsadai.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;

/**
 * Utility methods reading and writing input and output data.
 *  
 * @author The OGSA-DAI Project Team.
 */
public class IOUtilities {

    /** Copyright statement. */
    private static final String COPYRIGHT_NOTICE = "Copyright (c) The University of Edinburgh, 2007-2010.";

    /**
     * Streams data from an input stream into an output stream. Buffers are 
     * used to increase performance and the output is flushed before the method
     * returns.
     * 
     * @param input
     *     Input stream to consume from.
     * @param output
     *     Output stream to produce into.
     * @throws IOException
     *     If an error occurs reading or writing data.
     */
    public static void streamData(InputStream input, OutputStream output) throws IOException {
        BufferedInputStream bufinput = new BufferedInputStream(input);
        BufferedOutputStream bufoutput = new BufferedOutputStream(output);
        final byte[] buffer = new byte[1024];
        int length;
        while ((length = bufinput.read(buffer)) != -1) {
            bufoutput.write(buffer, 0, length);
        }
        bufoutput.flush();
    }

    /**
     * Streams data from a reader into a writer. Buffers are used to increase
     * performance and the writer is flushed before the method returns.
     * 
     * @param reader
     *     Reader to consume from.
     * @param writer
     *     Writer to produce into.
     * @throws IOException
     *     If an error occurs reading or writing data.
     */
    public static void streamData(Reader reader, Writer writer) throws IOException {
        BufferedReader bufreader = new BufferedReader(reader);
        BufferedWriter bufwriter = new BufferedWriter(writer);
        final char[] buffer = new char[512];
        int length;
        while ((length = bufreader.read(buffer)) != -1) {
            bufwriter.write(buffer, 0, length);
        }
        bufwriter.flush();
    }

    /**
     * Streams data from an input stream into an output stream. Buffers are 
     * used to increase performance and the output is flushed before the method
     * returns.
     * 
     * @param input
     *     Reader to consume from.
     * @param output
     *     Output stream to produce into.
     * @throws IOException
     *     If an error occurs reading or writing data.
     */
    public static void streamData(Reader input, DataOutputStream output) throws IOException {
        BufferedReader bufinput = new BufferedReader(input);
        final char[] chars = new char[512];
        int length;
        while ((length = bufinput.read(chars)) != -1) {
            output.writeBytes(new String(chars, 0, length));
        }
        output.flush();
    }

    /**
     * Opens an output stream to write to the specified URL.
     * 
     * @param url
     *     URL to write to.
     * @return open output stream
     * @throws IOException
     *     If an error occurs accessing the URL.
     */
    public static OutputStream openOutputStream(final URL url) throws IOException {
        final URLConnection connection = url.openConnection();
        connection.setDoInput(false);
        connection.setDoOutput(true);
        return connection.getOutputStream();
    }

    /**
     * Reads all the data from a {@link java.io.Reader} into a character
     * array.
     * 
     * @param input
     *     Input to read data from.
     * @return an array of the data that has been read.
     * @throws IOException
     *     If an error occurs streaming the data.
     */
    public static char[] readData(final Reader input) throws IOException {
        final CharArrayWriter output = new CharArrayWriter();
        streamData(input, output);
        output.close();
        return output.toCharArray();
    }

    /**
     * Reads all the data from a {@link java.io.InputStream} into a byte
     * array.
     * 
     * @param input
     *     Input to read data from.
     * @return an array of the data that has been read.
     * @throws IOException
     *     If an error occurs streaming the data
     */
    public static byte[] readData(final InputStream input) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        streamData(input, output);
        output.close();
        return output.toByteArray();
    }
}
