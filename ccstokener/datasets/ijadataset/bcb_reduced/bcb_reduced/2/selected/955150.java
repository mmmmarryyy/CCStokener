package visad.data.text;

import java.io.IOException;
import java.io.*;
import java.util.*;
import visad.Set;
import java.net.URL;
import visad.*;
import visad.VisADException;
import visad.data.in.ArithProg;

/** this is an VisAD file adapter for comma-, tab- and blank-separated
  * ASCII text file data.  It will attempt to create a FlatField from
  * the data and descriptions given in the file and/or the constructor.
  *
  * The text files contained delimited data.  The delimiter is 
  * determined as follows:  if the file has a well-known extension
  * (.csv, .tsv, .bsv) then the delimiter is implied by the extension.
  * In all other cases, the delimiter for the data (and for the
  * "column labels") is determined by reading the first line and
  * looking, in order, for a tab, comma, or blank.  Which ever one
  * is found first is taken as the delimiter.
  *
  * Two extra pieces of information are needed:  the VisAD "MathType"
  * which is specified as a string (e.g., (x,y)->(temperature))
  * and may either be the first line of the file or passed in through
  * one of the constructors.
  *
  * The second item are the "column labels" which contain the names
  * of each field in the data.  The names of all range components
  * specified in the "MathType" must appear.  The names of domain
  * components are optional.  The values in this string are separated
  * by the delimiter, as defined above.
  *
  * See visad.data.text.README.text for more details.
  * 
  * @author Tom Whittaker
  * 
  */
public class TextAdapter {

    private static final String ATTR_COLSPAN = "colspan";

    private static final String ATTR_VALUE = "value";

    private static final String ATTR_OFFSET = "off";

    private static final String ATTR_ERROR = "err";

    private static final String ATTR_SCALE = "sca";

    private static final String ATTR_POSITION = "pos";

    private static final String ATTR_FORMAT = "fmt";

    private static final String ATTR_TIMEZONE = "tz";

    private static final String ATTR_UNIT = "unit";

    private static final String ATTR_MISSING = "mis";

    private static final String ATTR_INTERVAL = "int";

    private static final String COMMA = ",";

    private static final String SEMICOLON = ";";

    private static final String TAB = "\t";

    private static final String BLANK = " ";

    private static final String BLANK_DELIM = "\\s+";

    private FlatField ff = null;

    private Field field = null;

    private boolean debug = false;

    private String DELIM;

    private boolean DOQUOTE = true;

    private boolean GOTTIME = false;

    HeaderInfo[] infos;

    double[] rangeErrorEstimates;

    Unit[] rangeUnits;

    Set[] rangeSets;

    double[] domainErrorEstimates;

    Unit[] domainUnits;

    int[][] hdrColumns;

    int[][] values_to_index;

    private boolean onlyReadOneLine = false;

    /** Create a VisAD FlatField from a local Text (comma-, tab- or 
    * blank-separated values) ASCII file
    * @param filename name of local file.
    * @exception IOException if there was a problem reading the file.
    * @exception VisADException if an unexpected problem occurs.
    */
    public TextAdapter(String filename) throws IOException, VisADException {
        this(filename, null, null);
    }

    /** Create a VisAD FlatField from a local Text (comma-, tab- or 
    * blank-separated values) ASCII file
    * @param filename name of local file.
    * @param map the VisAD "MathType" as a string defining the FlatField
    * @param params the list of parameters used to define what columns
    *  of the text file correspond to what MathType parameters.
    * @exception IOException if there was a problem reading the file.
    * @exception VisADException if an unexpected problem occurs.
    */
    public TextAdapter(String filename, String map, String params) throws IOException, VisADException {
        InputStream is = new FileInputStream(filename);
        DELIM = getDelimiter(filename);
        readit(is, map, params);
    }

    /** Create a VisAD FlatField from a remote Text (comma-, tab- or 
    * blank-separated values) ASCII file
    *
    * @param url File URL.
    * @exception IOException if there was a problem reading the file.
    * @exception VisADException if an unexpected problem occurs.
    */
    public TextAdapter(URL url) throws IOException, VisADException {
        this(url, null, null);
    }

    /** Create a VisAD FlatField from a local Text (comma-, tab- or 
    * blank-separated values) ASCII file
    * @param url File URL.
    * @param map the VisAD "MathType" as a string defining the FlatField
    * @param params the list of parameters used to define what columns
    *  of the text file correspond to what MathType parameters.
    * @exception IOException if there was a problem reading the file.
    * @exception VisADException if an unexpected problem occurs.
    */
    public TextAdapter(URL url, String map, String params) throws IOException, VisADException {
        DELIM = getDelimiter(url.getFile());
        InputStream is = url.openStream();
        readit(is, map, params);
    }

    /** Create a VisAD FlatField from a local Text (comma-, tab- or 
    * blank-separated values) ASCII file
    * @param inputStream The input stream to read from
    * @param delimiter the delimiter
    * @param map the VisAD "MathType" as a string defining the FlatField
    * @param params the list of parameters used to define what columns
    *  of the text file correspond to what MathType parameters.
    * @exception IOException if there was a problem reading the file.
    * @exception VisADException if an unexpected problem occurs.
    */
    public TextAdapter(InputStream inputStream, String delimiter, String map, String params) throws IOException, VisADException {
        this(inputStream, delimiter, map, params, false);
    }

    /** Create a VisAD FlatField from a local Text (comma-, tab- or 
    * blank-separated values) ASCII file
    * @param inputStream The input stream to read from
    * @param delimiter the delimiter
    * @param map the VisAD "MathType" as a string defining the FlatField
    * @param params the list of parameters used to define what columns
    *  of the text file correspond to what MathType parameters.
    * @param onlyReadOneLine If true then only read one line of data. This is used so client code can
    * read the meta data.
    * @exception IOException if there was a problem reading the file.
    * @exception VisADException if an unexpected problem occurs.
    */
    public TextAdapter(InputStream inputStream, String delimiter, String map, String params, boolean onlyReadOneLine) throws IOException, VisADException {
        this.onlyReadOneLine = onlyReadOneLine;
        DELIM = delimiter;
        readit(inputStream, map, params);
    }

    public static String getDelimiter(String filename) {
        if (filename == null) return null;
        filename = filename.trim().toLowerCase();
        if (filename.endsWith(".csv")) return COMMA;
        if (filename.endsWith(".tsv")) return TAB;
        if (filename.endsWith(".bsv")) return BLANK;
        return null;
    }

    /**
   * Is the given text line a comment
   *
   * @return is it a comment line
   */
    public static boolean isComment(String line) {
        return (line.startsWith("#") || line.startsWith("!") || line.startsWith("%") || line.length() < 1);
    }

    public static String readLine(BufferedReader bis) throws IOException {
        while (true) {
            String line = bis.readLine();
            if (line == null) return null;
            if (!isText(line)) return null;
            if (isComment(line)) continue;
            return line;
        }
    }

    void readit(InputStream is, String map, String params) throws IOException, VisADException {
        ff = null;
        field = null;
        if (debug) System.out.println("####   Text Adapter v2.x running");
        BufferedReader bis = new BufferedReader(new InputStreamReader(is));
        String maps = null;
        if (map == null) {
            maps = readLine(bis);
            if (maps != null) {
                maps = maps.trim();
            }
        } else {
            maps = map;
        }
        if (maps != null) {
            maps = makeMT(maps);
        }
        if (maps == null) {
            throw new visad.data.BadFormException("TextAdapter: Invalid or missing MathType");
        }
        if (debug) System.out.println("Specified MathType = " + maps);
        String hdr = null;
        if (params == null) {
            hdr = readLine(bis);
        } else {
            hdr = params;
        }
        String hdrDelim = DELIM;
        if (DELIM == null) {
            if (hdr.indexOf(BLANK) != -1) hdrDelim = BLANK_DELIM;
            if (hdr.indexOf(COMMA) != -1) hdrDelim = COMMA;
            if (hdr.indexOf(SEMICOLON) != -1) hdrDelim = SEMICOLON;
            if (hdr.indexOf(TAB) != -1) hdrDelim = TAB;
            if (debug) System.out.println("Using header delimiter = " + hdrDelim + "(" + (hdrDelim.getBytes())[0] + ")");
        }
        String[] sthdr = hdr.split(hdrDelim);
        int nhdr = sthdr.length;
        infos = new HeaderInfo[nhdr];
        for (int i = 0; i < infos.length; i++) {
            infos[i] = new HeaderInfo();
        }
        Real[] prototypeReals = new Real[nhdr];
        hdrColumns = new int[2][nhdr];
        int numHdrValues = 0;
        for (int i = 0; i < nhdr; i++) {
            String name = sthdr[i].trim();
            String hdrUnitString = null;
            hdrColumns[0][i] = -1;
            int m = name.indexOf("[");
            if (m == -1) {
                infos[i].name = name;
                hdrUnitString = null;
            } else {
                int m2 = name.indexOf("]");
                if (m2 == -1) {
                    throw new VisADException("TextAdapter: Bad [descriptor] named in:" + name);
                }
                if (m2 >= name.length()) {
                    infos[i].name = name.substring(0, m).trim();
                } else {
                    infos[i].name = (name.substring(0, m) + name.substring(m2 + 1)).trim();
                }
                String cl = name.substring(m + 1, m2).trim();
                String[] stcl = cl.split(BLANK_DELIM);
                int ncl = stcl.length;
                if (ncl == 1 && cl.indexOf("=") == -1) {
                    hdrUnitString = cl;
                } else {
                    for (int l = 0; l < ncl; l++) {
                        String s = stcl[l];
                        String[] sts = s.split("=");
                        if (sts.length != 2) {
                            throw new VisADException("TextAdapter: Invalid clause in: " + s);
                        }
                        String tok = sts[0];
                        String val = sts[1];
                        if (val.startsWith("\"")) {
                            if (val.endsWith("\"")) {
                                String v2 = val.substring(1, val.length() - 1);
                                val = v2;
                            } else {
                                try {
                                    String v2 = "";
                                    for (int q = l + 1; q < ncl; q++) {
                                        String vTmp = stcl[q];
                                        int pos = vTmp.indexOf("\"");
                                        l++;
                                        if (pos < 0) {
                                            v2 = v2 + " " + vTmp;
                                        } else {
                                            v2 = v2 + " " + vTmp.substring(0, pos);
                                            break;
                                        }
                                    }
                                    String v3 = val.substring(1) + v2;
                                    val = v3;
                                } catch (ArrayIndexOutOfBoundsException nse2) {
                                    val = "";
                                }
                            }
                        }
                        if (debug) System.out.println("####   tok = " + tok + " val = '" + val + "'");
                        if (tok.toLowerCase().startsWith(ATTR_UNIT)) {
                            hdrUnitString = val;
                        } else if (tok.toLowerCase().startsWith(ATTR_MISSING)) {
                            infos[i].missingString = val.trim();
                            try {
                                infos[i].missingValue = Double.parseDouble(val);
                            } catch (java.lang.NumberFormatException me) {
                                infos[i].missingValue = Double.NaN;
                            }
                        } else if (tok.toLowerCase().startsWith(ATTR_INTERVAL)) {
                            infos[i].isInterval = -1;
                            if (val.toLowerCase().startsWith("t")) infos[i].isInterval = 1;
                            if (val.toLowerCase().startsWith("f")) infos[i].isInterval = 0;
                            if (infos[i].isInterval == -1) {
                                throw new VisADException("TextAdapter: Value of \'interval\' must be \'true\' or \'false\'");
                            }
                        } else if (tok.toLowerCase().startsWith(ATTR_ERROR)) {
                            infos[i].errorEstimate = Double.parseDouble(val);
                        } else if (tok.toLowerCase().startsWith(ATTR_SCALE)) {
                            infos[i].scale = Double.parseDouble(val);
                        } else if (tok.toLowerCase().startsWith(ATTR_OFFSET)) {
                            infos[i].offset = Double.parseDouble(val);
                        } else if (tok.toLowerCase().startsWith(ATTR_VALUE)) {
                            infos[i].fixedValue = val.trim();
                            numHdrValues++;
                        } else if (tok.toLowerCase().startsWith(ATTR_COLSPAN)) {
                            infos[i].colspan = (int) Double.parseDouble(val.trim());
                        } else if (tok.toLowerCase().startsWith(ATTR_POSITION)) {
                            String[] stp = val.split(":");
                            if (stp.length != 2) {
                                throw new VisADException("TextAdapter: invalid Position parameter in:" + s);
                            }
                            hdrColumns[0][i] = Integer.parseInt(stp[0].trim());
                            hdrColumns[1][i] = Integer.parseInt(stp[1].trim());
                        } else if (tok.toLowerCase().startsWith(ATTR_FORMAT)) {
                            infos[i].formatString = val.trim();
                        } else if (tok.toLowerCase().startsWith(ATTR_TIMEZONE)) {
                            infos[i].tzString = val.trim();
                        } else {
                            throw new VisADException("TextAdapter: invalid token name: " + s);
                        }
                    }
                }
            }
            if (debug) System.out.println("hdr name = " + infos[i] + " units=" + hdrUnitString + " miss=" + infos[i].missingValue + " interval=" + infos[i].isInterval + " errorest=" + infos[i].errorEstimate + " scale=" + infos[i].scale + " offset=" + infos[i].offset + " pos=" + hdrColumns[0][i] + ":" + hdrColumns[1][i]);
            Unit u = null;
            if (hdrUnitString != null && !hdrUnitString.trim().equalsIgnoreCase("null")) {
                try {
                    u = visad.data.units.Parser.parse(hdrUnitString.trim());
                } catch (Exception ue) {
                    try {
                        u = visad.data.units.Parser.parse(hdrUnitString.trim().replace(' ', '_'));
                    } catch (Exception ue2) {
                        System.out.println("Unit name problem:" + ue + " with: " + hdrUnitString);
                        u = null;
                    }
                }
            }
            if (debug) System.out.println("####   assigned Unit as u=" + u);
            String rttemp = infos[i].name.trim();
            if (rttemp.indexOf("(Text)") == -1) {
                int parenIndex = rttemp.indexOf("(");
                if (parenIndex < 0) parenIndex = rttemp.indexOf("[");
                if (parenIndex < 0) parenIndex = rttemp.indexOf("{");
                if (parenIndex < 0) parenIndex = rttemp.indexOf(" ");
                String rtname = parenIndex < 0 ? rttemp.trim() : rttemp.substring(0, parenIndex);
                RealType rt = RealType.getRealType(rtname, u, null, infos[i].isInterval);
                if (rt == null) {
                    if (debug) System.out.println("####   rt was returned as null");
                    if (u != null) System.out.println("####  Could not make RealType using specified Unit (" + hdrUnitString + ") for parameter name: " + rtname);
                    rt = RealType.getRealType(rtname);
                }
                if (rt.equals(visad.RealType.Time)) {
                    GOTTIME = true;
                    if (debug) System.out.println("####  found a visad.RealType.Time component");
                } else {
                    GOTTIME = false;
                }
                if (u == null) u = rt.getDefaultUnit();
                if (debug) System.out.println("####  retrieve units from RealType = " + u);
            }
            infos[i].unit = u;
        }
        MathType mt = null;
        try {
            mt = MathType.stringToType(maps);
        } catch (Exception mte) {
            System.out.println("####  Exception: " + mte);
            throw new VisADException("TextAdapter: MathType badly formed or missing: " + maps);
        }
        if (debug) {
            System.out.println(mt);
            new visad.jmet.DumpType().dumpMathType(mt, System.out);
        }
        String[] domainNames = null;
        String[] rangeNames = null;
        int numDom = 0;
        int numRng = 0;
        RealTupleType domType;
        TupleType rngType;
        if (mt instanceof FunctionType) {
            domType = ((FunctionType) mt).getDomain();
            numDom = domType.getDimension();
            domainNames = new String[numDom];
            for (int i = 0; i < numDom; i++) {
                MathType comp = domType.getComponent(i);
                domainNames[i] = ((RealType) comp).toString().trim();
                if (debug) System.out.println("dom " + i + " = " + domainNames[i]);
            }
            rngType = (TupleType) ((FunctionType) mt).getRange();
            numRng = rngType.getDimension();
            rangeNames = new String[numRng];
            rangeSets = new Set[numRng];
            for (int i = 0; i < numRng; i++) {
                MathType comp = rngType.getComponent(i);
                rangeNames[i] = (comp).toString().trim();
                if (debug) System.out.println("range " + i + " = " + rangeNames[i]);
                if (comp instanceof RealType) {
                    rangeSets[i] = ((RealType) comp).getDefaultSet();
                    if (rangeSets[i] == null) {
                        if (comp.equals(RealType.Time)) {
                            rangeSets[i] = new DoubleSet(new SetType(comp));
                        } else {
                            rangeSets[i] = new FloatSet(new SetType(comp));
                        }
                    }
                } else {
                    rangeSets[i] = null;
                }
                if (debug) System.out.println("####  rangeSet = " + rangeSets[i]);
                ;
            }
        } else {
            throw new visad.VisADException("TextAdapter: Math Type is not a simple FunctionType");
        }
        int[] domainPointer = new int[numDom];
        double[][] domainRanges = new double[3][numDom];
        boolean[] gotDomainRanges = new boolean[numDom];
        domainErrorEstimates = new double[numDom];
        domainUnits = new Unit[numDom];
        rangeErrorEstimates = new double[numRng];
        rangeUnits = new Unit[numRng];
        int countDomain = 0;
        for (int i = 0; i < numDom; i++) {
            domainPointer[i] = -1;
            gotDomainRanges[i] = false;
            domainErrorEstimates[i] = Double.NaN;
            domainUnits[i] = null;
        }
        int[] rangePointer = new int[numRng];
        int countRange = 0;
        for (int i = 0; i < numRng; i++) {
            rangePointer[i] = -1;
            rangeErrorEstimates[i] = Double.NaN;
            rangeUnits[i] = null;
        }
        int countValues = -1;
        values_to_index = new int[3][nhdr];
        for (int i = 0; i < nhdr; i++) {
            values_to_index[0][i] = -1;
            values_to_index[1][i] = -1;
            values_to_index[2][i] = -1;
            countValues++;
            String name = infos[i].name;
            boolean gotName = false;
            String test_name = name;
            int n = test_name.indexOf("(");
            if (n != -1) {
                if ((test_name.indexOf("(Text)")) == -1) {
                    test_name = name.substring(0, n).trim();
                    countValues--;
                    countDomain--;
                }
            }
            for (int k = 0; k < numDom; k++) {
                if (test_name.equals(domainNames[k])) {
                    domainPointer[k] = countValues;
                    domainErrorEstimates[k] = infos[i].errorEstimate;
                    domainUnits[k] = infos[i].unit;
                    gotName = true;
                    countDomain++;
                    if (n != -1) {
                        try {
                            String ss = name.substring(n + 1, name.length() - 1);
                            String[] sct = ss.split(":");
                            String first = sct[0].trim();
                            String second = sct[1].trim();
                            String third = "1";
                            if (sct.length == 3) third = sct[2].trim();
                            domainRanges[0][k] = Double.parseDouble(first);
                            domainRanges[1][k] = Double.parseDouble(second);
                            domainRanges[2][k] = Double.parseDouble(third);
                            gotDomainRanges[k] = true;
                        } catch (Exception ef) {
                            throw new VisADException("TextAdapter: Error while interpreting min:max values for domain " + name);
                        }
                    } else if (countValues > -1) {
                        values_to_index[0][countValues] = k;
                        values_to_index[2][countValues] = i;
                    }
                    break;
                }
            }
            if (gotName) continue;
            for (int k = 0; k < numRng; k++) {
                if (name.equals(rangeNames[k])) {
                    rangePointer[k] = countValues;
                    rangeErrorEstimates[k] = infos[i].errorEstimate;
                    rangeUnits[k] = infos[i].unit;
                    countRange++;
                    values_to_index[1][countValues] = k;
                    values_to_index[2][countValues] = i;
                    gotName = true;
                }
            }
        }
        if (debug) {
            System.out.println("countDom/numDom=" + countDomain + " " + numDom);
            System.out.println("countRange/numRng=" + countRange + " " + numRng);
            System.out.println("Domain info:");
            for (int i = 0; i < numDom; i++) {
                System.out.println("Dom name / index = " + domainNames[i] + "  " + domainPointer[i]);
                if (gotDomainRanges[i]) {
                    System.out.println("    ..." + domainRanges[0][i] + "  " + domainRanges[1][i] + "    " + domainRanges[2][i]);
                }
            }
            System.out.println("Range info:");
            for (int i = 0; i < numRng; i++) {
                System.out.println("Rng name / index / error est = " + rangeNames[i] + "  " + rangePointer[i] + "  " + rangeErrorEstimates[i] + " " + rangeUnits[i]);
            }
            System.out.println("values_to_index pointers = ");
            for (int i = 0; i < nhdr; i++) {
                System.out.println(" inx / value = " + i + " " + values_to_index[0][i] + "    " + values_to_index[1][i] + " " + values_to_index[2][i]);
            }
        }
        ArrayList domainValues = new ArrayList();
        ArrayList rangeValues = new ArrayList();
        ArrayList tupleValues = new ArrayList();
        boolean tryToMakeTuple = true;
        Tuple tuple = null;
        String dataDelim = DELIM;
        boolean isRaster = false;
        int numElements = 1;
        if (countRange == 1 && numRng == 1 && numDom == 2 && countDomain < 2) isRaster = true;
        int index;
        while (true) {
            String s = bis.readLine();
            if (debug) System.out.println("read:" + s);
            if (s == null) break;
            if (!isText(s)) return;
            if (isComment(s)) continue;
            if ((index = s.indexOf("=")) >= 0) {
                String name = s.substring(0, index).trim();
                String value = s.substring(index + 1).trim();
                boolean foundIt = false;
                for (int paramIdx = 0; paramIdx < infos.length; paramIdx++) {
                    if (infos[paramIdx].isParam(name)) {
                        if (infos[paramIdx].fixedValue == null) {
                            numHdrValues++;
                        }
                        infos[paramIdx].fixedValue = value;
                        foundIt = true;
                        break;
                    }
                }
                if (!foundIt) {
                    throw new VisADException("TextAdapter: Cannot find field with name:" + name + " from line:" + s);
                }
                continue;
            }
            if (dataDelim == null) {
                if (s.indexOf(BLANK) != -1) dataDelim = BLANK_DELIM;
                if (s.indexOf(COMMA) != -1) dataDelim = COMMA;
                if (s.indexOf(SEMICOLON) != -1) dataDelim = SEMICOLON;
                if (s.indexOf(TAB) != -1) dataDelim = TAB;
                if (debug) System.out.println("Using data delimiter = " + ((dataDelim == null) ? "null" : dataDelim + " (" + (dataDelim.getBytes())[0] + ")"));
            }
            String[] st = s.split(dataDelim);
            int n = st.length;
            if (n < 1) continue;
            double[] dValues = new double[numDom];
            double[] rValues = null;
            Data[] tValues = null;
            if (isRaster) {
                if (debug) System.out.println("probably a raster...");
                boolean gotFirst = false;
                int rvaluePointer = 0;
                int irange = 0;
                for (int i = 0; i < n; i++) {
                    String sa = st[i];
                    if (i >= nhdr) {
                        if (!gotFirst) {
                            throw new VisADException("TextAdapter: Cannot find first raster value");
                        }
                        rvaluePointer++;
                        rValues[rvaluePointer] = getVal(sa, irange);
                    } else {
                        if (values_to_index[0][i] != -1) {
                            dValues[values_to_index[0][i]] = getVal(sa, i);
                        }
                        if (gotFirst) {
                            rvaluePointer++;
                            rValues[rvaluePointer] = getVal(sa, irange);
                        } else {
                            if (values_to_index[1][i] != -1) {
                                rValues = new double[n - i];
                                irange = i;
                                rValues[rvaluePointer] = getVal(sa, irange);
                                gotFirst = true;
                            }
                        }
                    }
                }
            } else {
                tValues = new Data[numRng];
                if (debug) System.out.println("probably not a raster...");
                rValues = new double[numRng];
                double thisDouble;
                MathType thisMT;
                if (n > nhdr) n = nhdr;
                n += numHdrValues;
                int l = 0;
                for (int i = 0; i < nhdr; i++) {
                    String sa;
                    if (infos[i].fixedValue != null) {
                        sa = infos[i].fixedValue;
                    } else if (l >= st.length) {
                        sa = "";
                    } else {
                        sa = st[l++].trim();
                        int moreColumns = infos[i].colspan - 1;
                        while (moreColumns > 0) {
                            sa = sa + " " + st[l++].trim();
                            moreColumns--;
                        }
                    }
                    String sThisText;
                    if (values_to_index[0][i] != -1) {
                        dValues[values_to_index[0][i]] = getVal(sa, i);
                    } else if (values_to_index[1][i] != -1) {
                        thisMT = rngType.getComponent(values_to_index[1][i]);
                        if (thisMT instanceof TextType) {
                            if (sa.startsWith("\"")) {
                                if (sa.endsWith("\"")) {
                                    String sa2 = sa.substring(1, sa.length() - 1);
                                    sThisText = sa2;
                                } else {
                                    try {
                                        String delim = dataDelim.equals(BLANK_DELIM) ? BLANK : dataDelim;
                                        String sa2 = "";
                                        for (int q = l; q < st.length; q++) {
                                            String saTmp = st[q];
                                            int pos = saTmp.indexOf("\"");
                                            l++;
                                            if (pos < 0) {
                                                sa2 = sa2 + delim + saTmp;
                                            } else {
                                                sa2 = sa2 + saTmp.substring(0, pos);
                                                break;
                                            }
                                        }
                                        sThisText = sa.substring(1) + delim + sa2;
                                    } catch (ArrayIndexOutOfBoundsException nse) {
                                        sThisText = "";
                                    }
                                }
                                if (debug) System.out.println("####   Text value='" + sThisText + "'");
                            } else {
                                sThisText = sa;
                            }
                            try {
                                tValues[values_to_index[1][i]] = new Text((TextType) thisMT, sThisText);
                                if (debug) System.out.println("tValues[" + values_to_index[1][i] + "] = " + tValues[values_to_index[1][i]]);
                            } catch (Exception e) {
                                System.out.println(" Exception converting " + thisMT + " to TextType " + e);
                            }
                        } else {
                            double value = getVal(sa, i);
                            rValues[values_to_index[1][i]] = value;
                            try {
                                if (prototypeReals[i] == null) {
                                    prototypeReals[i] = new Real((RealType) thisMT, getVal(sa, i), infos[i].unit);
                                }
                                tValues[values_to_index[1][i]] = prototypeReals[i].cloneButValue(value);
                                if (debug) System.out.println("tValues[" + values_to_index[1][i] + "] = " + tValues[values_to_index[1][i]]);
                            } catch (Exception e) {
                                System.out.println(" Exception converting " + thisMT + " " + e);
                            }
                        }
                    }
                }
            }
            if (tryToMakeTuple) {
                try {
                    if (tValues != null) tuple = new Tuple(tValues);
                } catch (visad.TypeException te) {
                    tuple = null;
                    tryToMakeTuple = false;
                } catch (NullPointerException npe) {
                    for (int i = 0; i < tValues.length; i++) {
                        if (tValues[i] == null) {
                            throw new IllegalArgumentException("An error occurred reading column number:" + (i + 1));
                        }
                    }
                    throw npe;
                }
            }
            domainValues.add(dValues);
            rangeValues.add(rValues);
            if (tuple != null) tupleValues.add(tuple);
            if (isRaster) numElements = rValues.length;
            if (onlyReadOneLine) break;
        }
        int numSamples = rangeValues.size();
        if (debug) {
            try {
                System.out.println("domain size = " + domainValues.size());
                double[] dt = (double[]) domainValues.get(1);
                System.out.println("domain.array[0] = " + dt[0]);
                System.out.println("range size = " + rangeValues.size());
                System.out.println("# samples = " + numSamples);
            } catch (Exception er) {
                System.out.println("out range");
            }
        }
        Linear1DSet[] lset = new Linear1DSet[numDom];
        boolean keepConstant = false;
        int numVal = numRng;
        if (numDom == 1) numVal = numSamples;
        if (numDom == 2 && numRng == 1 && numElements > 1) numVal = numElements;
        if (numDom > 2 && numRng == 1 && numElements == 1) {
            numVal = numSamples / (2 * numDom);
            keepConstant = true;
        }
        for (int i = 0; i < numDom; i++) {
            if (gotDomainRanges[i]) {
                if (numDom == 2 && numRng == 1 && numElements == 1) numVal = (int) domainRanges[2][i];
                lset[i] = new Linear1DSet(domType.getComponent(i), domainRanges[0][i], domainRanges[1][i], numVal);
                if (debug) System.out.println("lset from domain = " + lset[i]);
            } else if (domainPointer[i] == -1) {
                lset[i] = new Linear1DSet(0., (double) (numVal - 1), numVal);
                if (debug) System.out.println("lset from range = " + lset[i]);
            } else {
                lset[i] = null;
            }
            if (!keepConstant) numVal = numSamples;
        }
        Set domain = null;
        if (numDom == 1) {
            if (lset[0] == null) {
                domain = createAppropriate1DDomain(domType, numSamples, domainValues);
            } else {
                domain = lset[0];
            }
        } else if (numDom == 2) {
            if (lset[0] != null && lset[1] != null) {
                domain = new Linear2DSet(domType, lset);
            } else {
                float[][] samples = new float[numDom][numSamples];
                for (int k = 0; k < numDom; k++) {
                    if (lset[k] == null) {
                        samples[k] = (getDomSamples(k, numSamples, domainValues))[0];
                    } else {
                        samples[k] = (lset[k].getSamples())[0];
                    }
                }
                domain = (Set) new Irregular2DSet(domType, samples);
            }
        } else if (numDom == 3) {
            if (lset[0] != null && lset[1] != null && lset[2] != null) {
                domain = new Linear3DSet(domType, lset);
            } else {
                float[][] samples = new float[numDom][numSamples];
                for (int k = 0; k < numDom; k++) {
                    if (lset[k] == null) {
                        samples[k] = (getDomSamples(k, numSamples, domainValues))[0];
                    } else {
                        samples[k] = (lset[k].getSamples())[0];
                    }
                }
                domain = (Set) new Irregular3DSet(domType, samples);
            }
        } else {
            boolean allLinear = true;
            for (int k = 0; k < numDom; k++) {
                if (lset[k] == null) allLinear = false;
            }
            if (allLinear) {
                if (debug) System.out.println("####   Making LinearNDset");
                domain = new LinearNDSet(domType, lset);
            } else {
                if (debug) System.out.println("####   Making IrregularSet");
                float[][] samples = new float[numDom][numSamples];
                for (int k = 0; k < numDom; k++) {
                    if (lset[k] == null) {
                        samples[k] = (getDomSamples(k, numSamples, domainValues))[0];
                    } else {
                        samples[k] = (lset[k].getSamples())[0];
                    }
                }
                domain = new IrregularSet(domType, samples);
            }
        }
        try {
            ff = new FlatField((FunctionType) mt, domain, null, null, rangeSets, rangeUnits);
        } catch (FieldException fe) {
            field = new FieldImpl((FunctionType) mt, domain);
        } catch (UnitException fe) {
            System.out.println("####  Problem with Units; attempting to make Field anyway");
            field = new FieldImpl((FunctionType) mt, domain);
        }
        if (debug) {
            if (ff != null) {
                System.out.println("ff.Length " + ff.getLength());
                System.out.println("ff.getType " + ff.getType());
            }
            if (field != null) {
                System.out.println("field.Length " + field.getLength());
                System.out.println("field.getType " + field.getType());
            }
            System.out.println("domain = " + domain);
            System.out.println("size of a = " + numRng + " x " + (numSamples * numElements));
        }
        double[][] a = new double[numRng][numSamples * numElements];
        Tuple[] at = new Tuple[numSamples];
        if (isRaster) {
            int samPointer = 0;
            for (int i = 0; i < numSamples; i++) {
                double[] rs = (double[]) (rangeValues.get(i));
                for (int j = 0; j < numElements; j++) {
                    a[0][samPointer] = rs[j];
                    samPointer++;
                }
            }
        } else {
            for (int i = 0; i < numSamples; i++) {
                double[] rs = (double[]) (rangeValues.get(i));
                for (int j = 0; j < numRng; j++) {
                    a[j][i] = rs[j];
                }
                if (!tupleValues.isEmpty()) {
                    at[i] = (Tuple) tupleValues.get(i);
                }
            }
        }
        if (debug) System.out.println("about to field.setSamples");
        try {
            if (ff != null) {
                if (debug) System.out.println("####   ff is not null");
                ff.setSamples(a, false);
                field = (Field) ff;
            } else {
                if (debug) System.out.println("####   ff is null..use FieldImpl");
                field.setSamples(at, false);
            }
        } catch (Exception ffe) {
            ffe.printStackTrace();
        }
        ErrorEstimate[] es = new ErrorEstimate[numRng];
        for (int i = 0; i < numRng; i++) {
            es[i] = new ErrorEstimate(a[i], rangeErrorEstimates[i], rangeUnits[i]);
        }
        try {
            ((FlatField) field).setRangeErrors(es);
        } catch (FieldException fe) {
            if (debug) System.out.println("caught " + fe);
        } catch (ClassCastException cce) {
            if (debug) System.out.println("caught " + cce);
        }
        if (debug) {
            new visad.jmet.DumpType().dumpDataType(field, System.out);
            System.out.println("field = " + field);
        }
        bis.close();
    }

    private String makeMT(String s) {
        int k = s.indexOf("->");
        if (k < 0) {
            return null;
        }
        StringBuffer sb = new StringBuffer("");
        for (int i = 0; i < s.length(); i++) {
            String r = s.substring(i, i + 1);
            if (!r.equals(" ") && !r.equals("\t") && !r.equals("\n")) {
                sb.append(r);
            }
        }
        String t = sb.toString();
        k = t.indexOf("->");
        if (t.charAt(k - 1) != ')') {
            if (t.charAt(k + 2) != '(') {
                String t2 = "(" + t.substring(0, k) + ")->(" + t.substring(k + 2) + ")";
                t = t2;
            } else {
                String t2 = "(" + t.substring(0, k) + ")" + t.substring(k);
                t = t2;
            }
        } else if (t.charAt(k + 2) != '(') {
            String t2 = t.substring(0, k + 2) + "(" + t.substring(k + 2) + ")";
            t = t2;
        }
        if (!t.startsWith("((")) {
            String t2 = "(" + t + ")";
            t = t2;
        }
        return t;
    }

    private static final boolean isText(String s) {
        final int len = (s == null ? -1 : s.length());
        if (len <= 0) {
            return true;
        }
        for (int i = 0; i < len; i++) {
            final char ch = s.charAt(i);
            if (Character.isISOControl(ch) && !Character.isWhitespace(ch)) {
                return false;
            }
        }
        return true;
    }

    /** 
   * generate a DateTime from a string
   * @param string - Formatted date/time string
   *
   * @return - the equivalent VisAD DateTime for the string
   *
   * (lifted from au.gov.bom.aifs.common.ada.VisADXMLAdapter.java)
   */
    private static visad.DateTime makeDateTimeFromString(String string, String format, String tz) throws java.text.ParseException {
        visad.DateTime dt = null;
        try {
            if (dateParsers != null) {
                for (int i = 0; i < dateParsers.size(); i++) {
                    DateParser dateParser = (DateParser) dateParsers.get(i);
                    dt = dateParser.createDateTime(string, format, TimeZone.getTimeZone(tz));
                    if (dt != null) {
                        return dt;
                    }
                }
            }
            dt = visad.DateTime.createDateTime(string, format, TimeZone.getTimeZone(tz));
        } catch (VisADException e) {
        }
        if (dt == null) {
            throw new java.text.ParseException("Couldn't parse visad.DateTime from \"" + string + "\"", -1);
        } else {
            return dt;
        }
    }

    /** This list of DateFormatter-s will be checked when we are making a DateTime wiht a given format */
    private static List dateParsers;

    /** used to allow applications to define their own date parsing */
    public static interface DateParser {

        /** If this particular DateParser does not know how to handle the give  format then this method should return null */
        public DateTime createDateTime(String value, String format, TimeZone timezone) throws VisADException;
    }

    /** used to allow applications to define their own date parsing */
    public static void addDateParser(DateParser dateParser) {
        if (dateParsers == null) {
            dateParsers = new ArrayList();
        }
        dateParsers.add(dateParser);
    }

    double getVal(String s, int k) {
        int i = values_to_index[2][k];
        if (i < 0 || s == null || s.length() < 1 || s.equals(infos[i].missingString)) {
            return Double.NaN;
        }
        if (infos[i].formatString == null) {
            try {
                double v;
                try {
                    v = Double.parseDouble(s);
                } catch (java.lang.NumberFormatException nfe1) {
                    if (infos[i].unit != null && Unit.canConvert(infos[i].unit, visad.CommonUnit.degree)) {
                        v = decodeLatLon(s);
                    } else {
                        throw nfe1;
                    }
                    if (v != v) throw new java.lang.NumberFormatException(s);
                }
                if (v == infos[i].missingValue) {
                    return Double.NaN;
                }
                v = v * infos[i].scale + infos[i].offset;
                return v;
            } catch (java.lang.NumberFormatException ne) {
                System.out.println("Invalid number format for " + s);
            }
        } else {
            try {
                visad.DateTime dt = makeDateTimeFromString(s, infos[i].formatString, infos[i].tzString);
                return dt.getReal().getValue();
            } catch (java.text.ParseException pe) {
                System.out.println("Invalid number/time format for " + s);
            }
        }
        return Double.NaN;
    }

    float[][] getDomSamples(int comp, int numDomValues, ArrayList domValues) {
        float[][] a = new float[1][numDomValues];
        for (int i = 0; i < numDomValues; i++) {
            double[] d = (double[]) (domValues.get(i));
            a[0][i] = (float) d[comp];
        }
        return a;
    }

    /** get the data
  * @return a Field of the data read from the file
  *
  */
    public Field getData() {
        return field;
    }

    /**
   * Returns an appropriate 1D domain.
   *
   * @param type the math-type of the domain
   * @param numSamples the number of samples in the domain
   * @param domValues domain values are extracted from this array list.
   *
   * @return a Linear1DSet if the domain samples form an arithmetic
   *   progression, a Gridded1DDoubleSet if the domain samples are ordered
   *   but do not form an arithmetic progression, otherwise an Irregular1DSet.
   *
   * @throws VisADException there was a problem creating the domain set.
   */
    private Set createAppropriate1DDomain(MathType type, int numSamples, ArrayList domValues) throws VisADException {
        if (0 == numSamples) {
            return null;
        }
        double[][] values = new double[1][numSamples];
        for (int i = 0; i < numSamples; ++i) {
            double[] d = (double[]) domValues.get(i);
            values[0][i] = d[0];
        }
        boolean ordered = true;
        boolean ascending = values[0][numSamples - 1] > values[0][0];
        if (ascending) {
            for (int i = 1; i < numSamples; ++i) {
                if (values[0][i] < values[0][i - 1]) {
                    ordered = false;
                    break;
                }
            }
        } else {
            for (int i = 1; i < numSamples; ++i) {
                if (values[0][i] > values[0][i - 1]) {
                    ordered = false;
                    break;
                }
            }
        }
        Set set = null;
        if (ordered) {
            ArithProg arithProg = new ArithProg();
            if (arithProg.accumulate(values[0])) {
                set = new Linear1DSet(type, values[0][0], values[0][numSamples - 1], numSamples);
            } else {
                set = new Gridded1DDoubleSet(type, values, numSamples);
            }
        } else {
            set = new Irregular1DSet(type, Set.doubleToFloat(values));
        }
        return set;
    }

    private static class HeaderInfo {

        String name;

        Unit unit;

        double missingValue = Double.NaN;

        String missingString;

        String formatString;

        String tzString = "GMT";

        int isInterval = 0;

        double errorEstimate = 0;

        double scale = 1.0;

        double offset = 0.0;

        String fixedValue;

        int colspan = 1;

        public boolean isParam(String param) {
            return name.equals(param) || name.equals(param + "(Text)");
        }

        public String toString() {
            return name;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Must supply a filename");
            System.exit(1);
        }
        TextAdapter ta = new TextAdapter(args[0]);
        System.out.println(ta.getData().getType());
        new visad.jmet.DumpType().dumpMathType(ta.getData().getType(), System.out);
        new visad.jmet.DumpType().dumpDataType(ta.getData(), System.out);
        System.out.println("####  Data = " + ta.getData());
        System.out.println("EOF... ");
    }

    /**
     * A cut-and-paste from the IDV Misc method
     * Decodes a string representation of a latitude or longitude and
     * returns a double version (in degrees).  Acceptible formats are:
     * <pre>
     * +/-  ddd:mm, ddd:mm:, ddd:mm:ss, ddd::ss, ddd.fffff ===>   [+/-] ddd.fffff
     * +/-  ddd, ddd:, ddd::                               ===>   [+/-] ddd
     * +/-  :mm, :mm:, :mm:ss, ::ss, .fffff                ===>   [+/-] .fffff
     * +/-  :, ::                                          ===>       0.0
     * Any of the above with N,S,E,W appended
     * </pre>
     *
     * @param latlon  string representation of lat or lon
     * @return the decoded value in degrees
     */
    public static double decodeLatLon(String latlon) {
        latlon = latlon.trim();
        int dirIndex = -1;
        int southOrWest = 1;
        double value = Double.NaN;
        if (latlon.indexOf("S") > 0) {
            southOrWest = -1;
            dirIndex = latlon.indexOf("S");
        } else if (latlon.indexOf("W") > 0) {
            southOrWest = -1;
            dirIndex = latlon.indexOf("W");
        } else if (latlon.indexOf("N") > 0) {
            dirIndex = latlon.indexOf("N");
        } else if (latlon.indexOf("E") > 0) {
            dirIndex = latlon.indexOf("E");
        }
        if (dirIndex > 0) {
            latlon = latlon.substring(0, dirIndex).trim();
        }
        if (latlon.indexOf("-") == 0) {
            southOrWest *= -1;
            latlon = latlon.substring(latlon.indexOf("-") + 1).trim();
        }
        if (latlon.indexOf(":") >= 0) {
            int firstIdx = latlon.indexOf(":");
            String hours = latlon.substring(0, firstIdx);
            String minutes = latlon.substring(firstIdx + 1);
            String seconds = "";
            if (minutes.indexOf(":") >= 0) {
                firstIdx = minutes.indexOf(":");
                String temp = minutes.substring(0, firstIdx);
                seconds = minutes.substring(firstIdx + 1);
                minutes = temp;
            }
            try {
                value = (hours.equals("") == true) ? 0 : Double.parseDouble(hours);
                if (!minutes.equals("")) {
                    value += Double.parseDouble(minutes) / 60.;
                }
                if (!seconds.equals("")) {
                    value += Double.parseDouble(seconds) / 3600.;
                }
            } catch (NumberFormatException nfe) {
                value = Double.NaN;
            }
        } else {
            try {
                value = Double.parseDouble(latlon);
            } catch (NumberFormatException nfe) {
                value = Double.NaN;
            }
        }
        return value * southOrWest;
    }
}
