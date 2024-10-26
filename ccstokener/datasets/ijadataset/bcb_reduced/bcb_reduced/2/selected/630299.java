package owltools.gaf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.apache.log4j.Logger;

/**
 * 
 * @author Shahid Manzoor
 * 
 */
public class GAFParser {

    private static final String GAF_COMMENT = "!";

    private static final String GAF_VERSION = GAF_COMMENT + "gaf-version:";

    protected static Logger LOG = Logger.getLogger(GAFParser.class);

    private static boolean DEBUG = LOG.isDebugEnabled();

    public static final int DB = 0;

    public static final int DB_OBJECT_ID = 1;

    public static final int DB_OBJECT_SYMBOL = 2;

    public static final int QUALIFIER = 3;

    public static final int GOID = 4;

    public static final int REFERENCE = 5;

    public static final int EVIDENCE = 6;

    public static final int WITH = 7;

    public static final int ASPECT = 8;

    public static final int DB_OBJECT_NAME = 9;

    public static final int DB_OBJECT_SYNONYM = 10;

    public static final int DB_OBJECT_TYPE = 11;

    public static final int TAXON = 12;

    public static final int DATE = 13;

    public static final int ASSIGNED_BY = 14;

    public static final int ANNOTATION_XP = 15;

    public static final int GENE_PRODUCT_ISOFORM = 16;

    private double gafVersion;

    private BufferedReader reader;

    private String currentRow;

    private String currentCols[];

    private int expectedNumCols;

    private int lineNumber;

    private List<Object> voilations;

    private List<GafParserListener> parserListeners;

    public List<Object> getAnnotationRuleViolations() {
        return this.voilations;
    }

    /**
	 * Method declaration
	 * 
	 * @return true, if next was successful
	 * @throws IOException
	 */
    public boolean next() throws IOException {
        if (reader != null) {
            currentRow = reader.readLine();
            if (currentRow == null) {
                return false;
            }
            lineNumber++;
            if (DEBUG) LOG.debug("Parsing Row: " + lineNumber + " -- " + currentRow);
            if (this.currentRow.trim().length() == 0) {
                LOG.warn("Blank Line");
                return next();
            } else if (currentRow.startsWith(GAF_COMMENT)) {
                if (gafVersion < 1) {
                    if (isFormatDeclaration(currentRow)) {
                        gafVersion = parseGafVersion(currentRow);
                        if (gafVersion == 2.0) {
                            expectedNumCols = 17;
                        }
                    }
                }
                return next();
            } else {
                fireParsing();
                this.currentCols = this.currentRow.split("\\t", -1);
                if (currentCols.length != expectedNumCols) {
                    String error = "Got invalid number of columns for row (expected " + expectedNumCols + ", got " + currentCols.length + "). The '" + lineNumber + "' row is ignored.";
                    if (currentCols.length < expectedNumCols) {
                        String v = error;
                        voilations.add(v);
                        fireParsingError(error);
                        LOG.error(error + " : " + this.currentRow);
                        return next();
                    } else {
                        LOG.warn(error + " : " + this.currentRow);
                    }
                }
                return true;
            }
        }
        return false;
    }

    private void fireParsing() {
        for (GafParserListener listner : parserListeners) {
            listner.parsing(this.currentRow, lineNumber);
        }
    }

    private void fireParsingError(String message) {
        for (GafParserListener listner : parserListeners) {
            listner.parserError(message, this.currentRow, lineNumber);
        }
    }

    public String getCurrentRow() {
        return this.currentRow;
    }

    public GAFParser() {
        init();
    }

    public void init() {
        this.gafVersion = 0;
        this.reader = null;
        this.expectedNumCols = 15;
        voilations = new Vector<Object>();
        lineNumber = 0;
        if (parserListeners == null) {
            parserListeners = new Vector<GafParserListener>();
        }
    }

    public void addParserListener(GafParserListener listener) {
        if (listener == null) return;
        if (!parserListeners.contains(listener)) parserListeners.add(listener);
    }

    public void remoteParserListener(GafParserListener listener) {
        if (listener == null) return;
        parserListeners.remove(listener);
    }

    public void parse(Reader reader) {
        init();
        if (DEBUG) LOG.debug("Parsing Start");
        this.reader = new BufferedReader(reader);
    }

    /**
	 * 
	 * @param file is the location of the gaf file. The location
	 *  could be http url, absolute path and uri. The could refer to a gaf file or compressed gaf (gzip fle).
	 * @throws IOException
	 * @throws URISyntaxException
	 */
    public void parse(String file) throws IOException, URISyntaxException {
        if (file == null) {
            throw new IOException("File '" + file + "' file not found");
        }
        InputStream is = null;
        if (file.startsWith("http://")) {
            URL url = new URL(file);
            is = url.openStream();
        } else if (file.startsWith("file:/")) {
            is = new FileInputStream(new File(new URI(file)));
        } else {
            is = new FileInputStream(file);
        }
        if (file.endsWith(".gz")) {
            is = new GZIPInputStream(is);
        }
        parse(new InputStreamReader(is));
    }

    public void parse(File gaf_file) throws IOException {
        try {
            parse(gaf_file.getAbsoluteFile().toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void checkNext() {
        if (this.currentCols == null) {
            throw new IllegalStateException("Error occured becuase either there is no further " + "record in the file or parse method is not called yet");
        }
    }

    public String getDb() {
        checkNext();
        return this.currentCols[DB];
    }

    public String getDbObjectId() {
        checkNext();
        return this.currentCols[DB_OBJECT_ID];
    }

    public String getDbObjectSymbol() {
        checkNext();
        return this.currentCols[DB_OBJECT_SYMBOL];
    }

    public String getQualifier() {
        checkNext();
        return this.currentCols[QUALIFIER];
    }

    public String getGOId() {
        checkNext();
        return this.currentCols[GOID];
    }

    public String getReference() {
        checkNext();
        return this.currentCols[REFERENCE];
    }

    public String getEvidence() {
        checkNext();
        return this.currentCols[EVIDENCE];
    }

    public String getWith() {
        checkNext();
        return this.currentCols[WITH];
    }

    public String getAspect() {
        checkNext();
        return this.currentCols[ASPECT];
    }

    public String getDbObjectName() {
        checkNext();
        String s = this.currentCols[DB_OBJECT_NAME];
        s = s.replace("\\", "\\\\");
        return s;
    }

    public String getDbObjectSynonym() {
        checkNext();
        return this.currentCols[DB_OBJECT_SYNONYM];
    }

    public String getDBObjectType() {
        checkNext();
        return this.currentCols[DB_OBJECT_TYPE];
    }

    public String getTaxon() {
        checkNext();
        return this.currentCols[TAXON];
    }

    public String getDate() {
        checkNext();
        return this.currentCols[DATE];
    }

    public String getAssignedBy() {
        checkNext();
        return this.currentCols[ASSIGNED_BY];
    }

    public String getAnnotationExtension() {
        checkNext();
        if (this.currentCols.length > 15) {
            return this.currentCols[ANNOTATION_XP];
        }
        return null;
    }

    public String getGeneProjectFormId() {
        checkNext();
        if (this.currentCols.length > 16) {
            return this.currentCols[GENE_PRODUCT_ISOFORM];
        }
        return null;
    }

    private boolean isFormatDeclaration(String line) {
        return line.startsWith(GAF_VERSION);
    }

    private double parseGafVersion(String line) {
        Pattern p = Pattern.compile(GAF_VERSION + "\\s*(\\d+\\.*\\d+)");
        Matcher m = p.matcher(line);
        if (m.matches()) {
            return Double.parseDouble(m.group(1));
        }
        return 0;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
