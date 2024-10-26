package net.sf.japi.archstat;

import net.sf.japi.io.args.ArgParser;
import net.sf.japi.io.args.BasicCommand;
import net.sf.japi.io.args.Option;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A Command for performing recursive source code file statistics.
 * @author <a href="mailto:cher@riedquat.de">Christian Hujer</a>
 * @since 0.1
 */
public class ArchStat extends BasicCommand {

    /** Default buffer size. */
    private static final int BUF_SIZE = 8192;

    /** Main program.
     * @param args Command line arguments (try --help).
     */
    public static void main(final String... args) {
        ArgParser.simpleParseAndRun(new ArchStat(), args);
    }

    /** The DocumentBuilder for parsing XML documents. */
    @NotNull
    private DocumentBuilder documentBuilder;

    /** Whether or not to output memory statistics. */
    private boolean memoryStatistics;

    /** Whether or not to print summaries. */
    private boolean printSummaries;

    /** {@inheritDoc} */
    @SuppressWarnings({ "InstanceMethodNamingConvention", "ProhibitedExceptionDeclared" })
    public int run(@NotNull final List<String> args) throws Exception {
        if (args.isEmpty()) {
            System.err.println("Error: No arguments given.");
            return 1;
        }
        if (memoryStatistics) {
            System.err.println("Memory free (start): " + Runtime.getRuntime().freeMemory());
        }
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        documentBuilder = dbf.newDocumentBuilder();
        readDefaultCheckers();
        final FileStat fileStat = new FileStat(checkers);
        for (final String arg : args) {
            fileStat.addChild(arg, printSummaries);
        }
        fileStat.printStatistic(System.out, depth);
        if (memoryStatistics) {
            System.err.println("Memory free (end, before GC): " + Runtime.getRuntime().freeMemory());
            System.gc();
            System.err.println("Memory free (end, after GC): " + Runtime.getRuntime().freeMemory());
        }
        return fileStat.getWarnings() > 0 ? 1 : 0;
    }

    /** The default depth. */
    private static final int DEFAULT_DEPTH = 0;

    /** The depth up to which statistics are printed.
     */
    private int depth = DEFAULT_DEPTH;

    /** Sets whether or not to output memory statistics.
     * @param memoryStatistics Whether or not to output memory statistics.
     */
    @Option({ "memory" })
    public void setMemoryStatistics(@NotNull final Boolean memoryStatistics) {
        this.memoryStatistics = memoryStatistics;
    }

    /** Sets the depth up to which statistics are printed.
     * @param depth Depth up to which statistics are printed.
     */
    @Option({ "d", "depth" })
    public void setDepth(@NotNull final Integer depth) {
        this.depth = depth;
    }

    /** Sets whether or not to print summaries.
     * @param printSummaries <code>true</code> to print summaries, otherwise <code>false</code>.
     */
    @Option({ "s", "summary" })
    public void setPrintSummary(@NotNull final Boolean printSummaries) {
        this.printSummaries = printSummaries;
    }

    /** Returns the depth up to which statistics are printed.
     * @return The depth up to which statistics are printed.
     */
    public int getDepth() {
        return depth;
    }

    /** The checks that should be performed. */
    private final List<RegexLineCheck> checkers = new ArrayList<RegexLineCheck>();

    /** Reads a file.
     * @param file File to read.
     * @return Contents of that file as String.
     * @throws IOException In case of I/O problems.
     */
    public static CharSequence readFile(final File file) throws IOException {
        assert file.isFile();
        final long fileLength = file.length();
        if (fileLength > Integer.MAX_VALUE) {
            throw new IOException("File too large" + file);
        }
        @SuppressWarnings({ "NumericCastThatLosesPrecision" }) final StringBuilder sb = new StringBuilder((int) fileLength);
        final BufferedReader in = new BufferedReader(new FileReader(file));
        try {
            final char[] buf = new char[BUF_SIZE];
            for (int charsRead; (charsRead = in.read(buf)) != -1; ) {
                sb.append(buf, 0, charsRead);
            }
        } finally {
            in.close();
        }
        return sb.toString();
    }

    private static final Pattern LINE_SPLIT_PATTERN = Pattern.compile("(?<=\\r\\n|\\r|\\n)");

    /** The Pattern for counting lines. */
    private static final Pattern LINE_COUNT_PATTERN = Pattern.compile("$", Pattern.MULTILINE);

    /** Pattern that matches comments. */
    private static final Pattern COMMENT_PATTERN = Pattern.compile("/(/.*?$|\\*.*?\\*/(\\s*?\\n)?)", Pattern.MULTILINE | Pattern.DOTALL);

    /** Pattern that matches source lines. */
    private static final Pattern SOURCE_LINE_PATTERN = Pattern.compile("^.*\\S.*\\S.*\\S.*$", Pattern.MULTILINE);

    /** Returns the number of lines in the specified string.
     * @param text String of which to count lines.
     * @return The number of lines in <var>string</var>
     */
    static int countLines(final CharSequence text) {
        return count(LINE_COUNT_PATTERN, text) - 1;
    }

    /** Returns a split array of lines for the specified string.
     * @param text String to split.
     * @return Array with lines.
     */
    static String[] getLines(final CharSequence text) {
        return LINE_SPLIT_PATTERN.split(text);
    }

    /** Returns a String that is the input String with all comments removed.
     * @param text String from which to remove the comments.
     * @return Copy of <var>string</var> with all comments removed.
     */
    static CharSequence removeCComments(final CharSequence text) {
        return COMMENT_PATTERN.matcher(text).replaceAll("");
    }

    /** Returns the number of source lines of <var>string</var>.
     * @param text String of which to count the number of source lines.
     * @return The number of source lines of <var>string</var>.
     */
    static int countSourceLines(final CharSequence text) {
        return count(SOURCE_LINE_PATTERN, text);
    }

    /** Returns the number of matches of <var>pattern</var> on <var>string</var>.
     * @param pattern Pattern of which to count the matches.
     * @param text String in which to count the matches.
     * @return The number of matches of <var>pattern</var> on <var>string</var>.
     */
    private static int count(final Pattern pattern, final CharSequence text) {
        int count = 0;
        final Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /** Reads the default checkers.
     * @throws SAXException In case of XML issues when reading the default checkers.
     * @throws IOException In case of I/O problems when reading the default checkers.
     */
    @SuppressWarnings({ "HardcodedFileSeparator" })
    private void readDefaultCheckers() throws SAXException, IOException {
        final Enumeration<URL> checkerURLs = ArchStat.class.getClassLoader().getResources("net/sf/japi/archstat/Checker.xml");
        while (checkerURLs.hasMoreElements()) {
            final URL url = checkerURLs.nextElement();
            readCheckers(url);
        }
    }

    /** Parses the checkers.
     * @param doc Document from which to parse the checkers.
     */
    private void readCheckers(final Document doc) {
        final NodeList nl = doc.getElementsByTagName("pattern");
        for (int i = 0; i < nl.getLength(); i++) {
            final RegexLineCheck regexLineCheck = new RegexLineCheck((Element) nl.item(i));
            if (checkers.contains(regexLineCheck)) {
                throw new RuntimeException("Duplicate Checker " + regexLineCheck.getName());
            }
            checkers.add(regexLineCheck);
        }
    }

    /** Reads additional configuration from the specified file.
     * @param file File from which to read the config.
     * @throws IOException in case of I/O problems.
     * @throws SAXException in case of XML parsing errors.
     */
    @Option({ "c", "config" })
    public void readCheckers(@NotNull final File file) throws SAXException, IOException {
        readCheckers(documentBuilder.parse(file));
    }

    /** Reads additional configuration from the specified resource.
     * @param url Resource to read.
     * @throws IOException in case of I/O problems.
     * @throws SAXException in case of XML parsing errors.
     */
    private void readCheckers(final URL url) throws SAXException, IOException {
        readCheckers(documentBuilder.parse(url.openStream()));
    }
}
