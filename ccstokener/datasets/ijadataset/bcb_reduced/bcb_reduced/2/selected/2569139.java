package ca.usask.bioinfo.traceview;

import java.io.*;
import java.net.*;

/**
 * The <code>TraceFileFactory</code> class provide methods to create a
 * <code>TraceFile</code> object from a file, URL, or input stream. In each
 * case, the data source is examined to determine the format of the file and
 * an appropriate subclass of <code>TraceFile</code> is instantiated.
 *
 * @author  Luke McCarthy
 * @version 0.1 2003-04-04
 */
public abstract class TraceFileFactory {

    /**
	 * Creates a <code>TraceFile</code> object from the specified input
	 * stream.  The stream is examined to determine the format of the file and
	 * an appropriate subclass of <code>TraceFile</code> is instantiated and
	 * returned.
	 *
	 * @param	is	the input stream containing the trace file
	 *
	 * @exception	IOException	if an I/O exception occurs
	 */
    public static TraceFile createTraceFile(InputStream is) throws IOException {
        byte[] buf = new byte[4096];
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        for (int read = 0; (read = is.read(buf)) > 0; ) os.write(buf, 0, read);
        is = new ByteArrayInputStream(os.toByteArray());
        if (ABIFile.isABI(is)) return new ABIFile(is); else if (SCFFile.isSCF(is)) return new SCFFile(is); else throw new IOException("Unsupported file format");
    }

    /**
	 * Creates a <code>TraceFile</code> object from the specified file.
	 *
	 * @param	f	the trace file
	 *
	 * @exception	IOException	if an I/O exception occurs
	 */
    public static TraceFile createTraceFile(File f) throws IOException {
        return createTraceFile(new FileInputStream(f));
    }

    /**
	 * Creates a <code>TraceFile</code> object from the specified URL.
	 *
	 * @param	url	the URL from which the trace file can be retrieved
	 *
	 * @exception	IOException	if an I/O exception occurs
	 */
    public static TraceFile createTraceFile(URL url) throws IOException {
        return createTraceFile(url.openStream());
    }
}
