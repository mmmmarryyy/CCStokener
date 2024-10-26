import java.io.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import java.net.*;

/**
 * The base class for test drivers. To create a test for a new
 * kind of parsing interface, extend this class and implement
 * the abstract methods and the parse() methods.
 *
 * Subclasses should contain a main() method like this:
 *
 * public static void main(String[] args) { new TestClass(args); }
 *
 */
public abstract class AbstractTest {

    private String file;

    private int iterations;

    private String[] options;

    private int inputProcessing = 0;

    public static final int NONE = 0;

    public static final int PREDECODE = 1;

    public static final int PRELOAD = 2;

    /**
     * Instantiates this test with the given command-line options.
     * Note that the filename and any other global options have 
     * already been parsed and stripped off.
     *
     * Subclasses should override this constructor.
     */
    public AbstractTest(String[] args) throws Exception {
        file = args[0];
        iterations = Integer.parseInt(args[1]);
        int numArgs = 2;
        if (args.length >= 3) {
            if ("predecode".equalsIgnoreCase(args[2])) {
                inputProcessing = PREDECODE;
                numArgs++;
            } else if ("preload".equalsIgnoreCase(args[2])) {
                inputProcessing = PRELOAD;
                numArgs++;
            }
        }
        options = new String[args.length - numArgs];
        if (options.length > 0) System.arraycopy(args, numArgs, options, 0, options.length);
    }

    public AbstractTest() {
        file = null;
        iterations = 0;
        options = null;
    }

    /**
     * Returns a string describing the command-line options of this class.
     * Subclasses should override this method, appending information on
     * options to super.usage().
     */
    public String usage() {
        return "<file> <iterations> [predecode|preload]";
    }

    /**
     * Parses an XML document given an InputStream
     * The default implementation creates an InputStreamReader
     * and calls parse(Reader).
     */
    protected void parse(InputStream in) throws Exception {
        parse(new InputStreamReader(in));
    }

    /**
     * Parses an XML document given a Reader.
     */
    protected abstract void parse(Reader reader) throws Exception;

    /**
     * Initializes the test driver. Called by run().
     */
    public abstract void init(String[] options) throws Exception;

    /**
     * Prints a message to System.out.
     * Use this to return test results in 
     * XML form. See DTD for details.
     */
    public static void msg(String msg) {
        System.out.println(msg);
    }

    /**
     * Prints a message to System.err.
     * These messages will be recorded as
     * comments in the test results XML file.
     */
    public static void info(String msg) {
        System.err.println(msg);
    }

    /**
     * Returns an exception for bad arguments, along with 
     * an optional error message.
     */
    protected IllegalArgumentException badUsage(String msg) {
        String s;
        if (msg != null) s = "Error: " + msg + "\nUsage: java " + getClass().getName() + " " + usage(); else s = "Usage: java " + getClass().getName() + " " + usage();
        throw new IllegalArgumentException(s);
    }

    /**
     * Executes the tests. This method should be called at the
     * end of the subclass constructor.
     */
    public void run() {
        long startTime, endTime;
        try {
            init(options);
            URL url = new URL(new URL("file:."), file);
            String chars = null;
            byte[] bytes = null;
            switch(inputProcessing) {
                case PREDECODE:
                    chars = slurpFileIntoString(file);
                    break;
                case PRELOAD:
                    bytes = slurpFileIntoBytes(file);
                    break;
            }
            if (iterations == 0) {
                switch(inputProcessing) {
                    case PREDECODE:
                        parse(new StringReader(chars));
                        break;
                    case PRELOAD:
                        parse(new ByteArrayInputStream(bytes));
                        break;
                    default:
                        parse(url.openStream());
                }
                info("Parsed once for a dry run");
                return;
            }
            info("Warming up the parser....");
            startTime = System.currentTimeMillis();
            int count = 0;
            while (System.currentTimeMillis() - startTime < 5000) {
                switch(inputProcessing) {
                    case PREDECODE:
                        parse(new StringReader(chars));
                        break;
                    case PRELOAD:
                        parse(new ByteArrayInputStream(bytes));
                        break;
                    default:
                        parse(url.openStream());
                }
                ++count;
            }
            info("warm-up count=" + count);
            info("Parsing " + file + " " + iterations + " times by " + getClass().getName());
            startTime = System.currentTimeMillis();
            switch(inputProcessing) {
                case PREDECODE:
                    startTime = System.currentTimeMillis();
                    for (int i = 0; i < iterations; i++) parse(new StringReader(chars));
                    endTime = System.currentTimeMillis();
                    break;
                case PRELOAD:
                    startTime = System.currentTimeMillis();
                    for (int i = 0; i < iterations; i++) parse(new ByteArrayInputStream(bytes));
                    endTime = System.currentTimeMillis();
                    break;
                default:
                    startTime = System.currentTimeMillis();
                    for (int i = 0; i < iterations; i++) parse(url.openStream());
                    endTime = System.currentTimeMillis();
            }
            info("Elapsed time: " + (endTime - startTime) + "ms");
            info("Average parse time: " + ((float) (endTime - startTime) / iterations) + "ms");
            msg("<benchmark elapsed=\"" + (endTime - startTime) + "\" iterations=\"" + iterations + "\"/>");
        } catch (Exception e) {
            msg("<error>" + e.toString() + "</error>");
            e.printStackTrace();
        }
    }

    private static byte[] slurpFileIntoBytes(String file) throws IOException {
        FileInputStream fin = new FileInputStream(file);
        int pos = 0;
        int flen = (int) new File(file).length();
        byte[] content = new byte[flen];
        while (pos < flen) pos += fin.read(content, (int) pos, flen - pos);
        return content;
    }

    private static String slurpFileIntoString(String file) throws IOException, RuntimeException {
        StringBuffer buf = new StringBuffer();
        char[] cbuf = new char[8 * 1024];
        FileReader fr = new FileReader(file);
        long pos = 0;
        long flen = new File(file).length();
        while (true) {
            int ret = fr.read(cbuf);
            if (ret == -1) break;
            buf.append(cbuf, 0, ret);
            pos += ret;
        }
        if (flen != -1 && flen != pos) throw new RuntimeException("could not get whole file");
        String content = buf.toString();
        return content;
    }
}
