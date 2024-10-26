package com.mockturtlesolutions.snifflib.datatypes;

import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.io.File;
import java.io.StreamTokenizer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Vector;
import java.io.Reader;
import java.lang.reflect.Constructor;
import com.mockturtlesolutions.snifflib.flatfiletools.database.FlatFileStorage;
import com.mockturtlesolutions.snifflib.flatfiletools.database.FlatFileDOM;
import java.net.URI;
import java.net.URL;
import java.io.InputStreamReader;
import java.io.DataInputStream;

public class DataSetReader {

    private URL url2goto;

    private File file;

    private boolean isURL;

    private Vector types;

    private Vector headers;

    private Vector constructors;

    private boolean determineHeaders;

    private boolean hasHeaderLine;

    private boolean determineTypes;

    private int ignorePreHeaderLines;

    private int ignorePostHeaderLines;

    private String delimiter;

    private String recorddelimiter;

    private Object targetValue;

    private int targetRow;

    private int targetColumn;

    private int rowcount;

    private int lowestRow;

    private int rowSpan;

    private int lowestColumn;

    private int colSpan;

    private boolean dryrun;

    private boolean findingTargetValue;

    private Double scientificNumber;

    private ValueQueue valueQueue;

    public DataSetReader() {
        this.types = null;
        this.headers = new Vector();
        this.types = new Vector();
        this.constructors = new Vector();
        this.determineHeaders = true;
        this.determineTypes = true;
        this.ignorePreHeaderLines = 0;
        this.hasHeaderLine = false;
        this.ignorePostHeaderLines = 0;
        this.delimiter = "\\t";
        this.recorddelimiter = "\\n";
        this.dryrun = false;
        this.scientificNumber = null;
        this.lowestRow = 0;
        this.rowSpan = 10;
        this.lowestColumn = 0;
        this.colSpan = 10;
        this.targetValue = null;
        this.targetRow = -1;
        this.targetColumn = -1;
        this.findingTargetValue = false;
        this.rowcount = 0;
        this.valueQueue = new ValueQueue();
    }

    public DataSetReader(FlatFileStorage fileinfo) {
        this();
        Boolean isurl = new Boolean(fileinfo.getIsURL());
        this.isURL = isurl.booleanValue();
        if (this.isURL) {
            this.file = null;
            try {
                this.url2goto = new URL(fileinfo.getFilename());
            } catch (Exception err) {
                throw new RuntimeException("Unable to create URL for " + fileinfo.getFilename() + ".", err);
            }
        } else {
            this.file = new File(fileinfo.getFilename());
            this.url2goto = null;
        }
        Integer val = new Integer(fileinfo.getPreHeaderLines());
        this.ignorePreHeaderLines = val.intValue();
        val = new Integer(fileinfo.getPostHeaderLines());
        this.ignorePostHeaderLines = val.intValue();
        this.delimiter = fileinfo.getFieldDelimiter();
        this.recorddelimiter = fileinfo.getRecordDelimiter();
        String FMT = fileinfo.getFormat();
        String[] FMTsplit = FMT.split("%");
        String[] hdrs = new String[FMTsplit.length - 1];
        Class[] tps = new Class[FMTsplit.length - 1];
        for (int j = 0; j < tps.length; j++) {
            hdrs[j] = "" + (j + 1);
            if (FMTsplit[j + 1].equals("s")) {
                tps[j] = String.class;
            } else if (FMTsplit[j + 1].equals("f")) {
                tps[j] = Double.class;
            } else {
                tps[j] = String.class;
            }
        }
        this.setTypes(tps);
        this.setHeaders(hdrs);
        this.valueQueue = new ValueQueue();
    }

    private class ValueQueue {

        private Vector vqueue;

        private int bufferSize;

        private int upper_index;

        public ValueQueue() {
            this.bufferSize = 10000;
            this.upper_index = -1;
            this.vqueue = new Vector();
        }

        public int getUpperIndex() {
            return (this.upper_index);
        }

        public int getLowerIndex() {
            return (this.upper_index - this.vqueue.size() + 1);
        }

        public Object getValue(int r, int c) {
            boolean out = true;
            int lower_index = this.upper_index - this.vqueue.size() + 1;
            int index2get = subs2index(r, c, getColumnCount());
            int vind = index2get - lower_index;
            if ((vind >= this.vqueue.size()) || (vind < 0)) {
                throw new RuntimeException("Index " + vind + " is outside currently queued range (" + lower_index + "," + this.upper_index + ").");
            }
            Object OUT = this.vqueue.get(vind);
            if (OUT == null) {
                throw new RuntimeException("Queued item was null.");
            }
            return (OUT);
        }

        public void clear() {
            this.vqueue = new Vector();
            this.upper_index = -1;
        }

        /**
		Returns the index (in column major ordering) of the i,j element of a table
		having m rows and n columns. 
		*/
        public int subs2index(int i, int j, int n) {
            int out = i * n + j;
            return (out);
        }

        /**
		Returns the subscripts for the given index.
		*/
        public int[] index2subs(int index, int n) {
            int[] out = new int[2];
            int col = index % n;
            int row = index / n;
            out[0] = row;
            out[1] = col;
            return (out);
        }

        /**
		Determines if this queue currently holds the value for the table at row r and column c.
		*/
        public boolean hasValueAt(int r, int c) {
            boolean out = true;
            int lower_index = this.upper_index - this.vqueue.size() + 1;
            int index2get = subs2index(r, c, getColumnCount());
            if ((index2get < lower_index) || (index2get > this.upper_index)) {
                out = false;
            }
            return (out);
        }

        public int[] readAheadFor(int r, int c) {
            int desired = subs2index(r, c, getColumnCount());
            int readahead2 = desired + this.bufferSize / 2;
            return (index2subs(readahead2, getColumnCount()));
        }

        public void push(Object c) {
            if (c == null) {
                throw new RuntimeException("Null tokens can not be added to queue.");
            }
            this.vqueue.add(c);
            this.upper_index++;
            while (this.vqueue.size() > this.bufferSize) {
                this.vqueue.removeElementAt(0);
            }
        }
    }

    public int getColumnCount() {
        return (this.getHeaders().size());
    }

    public DataSetReader(String datafile) {
        this(new File(datafile));
    }

    public DataSetReader(File f) {
        this();
        this.file = f;
    }

    public void setFile(File f) {
        this.file = f;
    }

    public File getFile() {
        return (this.file);
    }

    /**
	Ignore this many lines before the headers line (if any) before beginning to parse.
	*/
    public void setIgnorePreHeaderLines(int h) {
        this.ignorePreHeaderLines = h;
    }

    /**
	Gets the number of lines before the headers line (if any) to be ignored.
	*/
    public int getIgnorePreHeaderLines() {
        return (this.ignorePreHeaderLines);
    }

    /**
	Ignore this many lines after the headers line (if any) before beginning to parse.
	*/
    public void setIgnorePostHeaderLines(int h) {
        this.ignorePostHeaderLines = h;
    }

    /**
	Gets the number of lines after the headers line (if any) to be ignored.
	*/
    public int getIgnorePostHeaderLines() {
        return (this.ignorePostHeaderLines);
    }

    public int getRowCount() {
        return (this.rowcount);
    }

    /**
	Informs the parser that headers are not to be parsed from the data file.
	If headers are not explicitly provided using the setHeaders(String[] hdrs)
	method, then the automatic headers are generated.
	*/
    public void noHeaders() {
        this.determineHeaders = false;
    }

    /**
	Informs the parser that types are not to be determined from the data file.
	*/
    public void noTypes() {
        this.determineTypes = false;
    }

    /**
	Explicitly provide headers.  Using this method automatically sets noHeaders().
	*/
    public void setHeaders(String[] hdrs) {
        for (int j = 0; j < hdrs.length; j++) {
            this.headers.add(hdrs[j]);
        }
        this.determineHeaders = false;
    }

    /**
	Explicitly provide types.  Using this method automatically sets noTypes().
	*/
    public void setTypes(Class[] t) {
        for (int j = 0; j < t.length; j++) {
            this.types.add(t[j]);
        }
    }

    public Vector getTypes() {
        return (this.types);
    }

    public Vector getHeaders() {
        return (this.headers);
    }

    public FlatFileDOM getFlatFileDOM() {
        FlatFileDOM out = new FlatFileDOM();
        return (out);
    }

    public Object getValueAt(int r, int c) {
        Object out = null;
        if (this.valueQueue.hasValueAt(r, c)) {
            out = this.valueQueue.getValue(r, c);
        } else {
            this.valueQueue.clear();
            out = null;
            boolean prevdry = this.dryrun;
            this.dryrun = true;
            this.scientificNumber = null;
            int[] targets = valueQueue.readAheadFor(r, c);
            this.targetRow = targets[0];
            this.targetColumn = targets[1];
            this.targetValue = null;
            this.findingTargetValue = true;
            this.dryrun = true;
            try {
                this.newparse();
            } catch (Exception err) {
                throw new RuntimeException("Problem parsing data.", err);
            }
            this.dryrun = prevdry;
            out = this.valueQueue.getValue(r, c);
            this.targetValue = null;
        }
        return (out);
    }

    public int determineRowCount() {
        boolean prevdry = this.dryrun;
        this.dryrun = true;
        try {
            this.newparse();
        } catch (Exception err) {
            throw new RuntimeException("Problem determining row count during parse of data.", err);
        }
        this.dryrun = prevdry;
        return (this.getRowCount());
    }

    public DataSet newparse() throws SnifflibDatatypeException {
        NumberFormat numformat = NumberFormat.getInstance();
        if (this.headers.size() != this.types.size()) {
            throw new SnifflibDatatypeException("Different number of headers (" + this.headers.size() + ") and types(" + this.types.size() + ").");
        }
        DataSet out = null;
        if (!this.dryrun) {
            out = new DataSet();
        }
        BufferedReader r = null;
        StreamTokenizer tokenizer = null;
        try {
            if (this.isURL) {
                if (this.url2goto == null) {
                    return (null);
                }
                DataInputStream in = null;
                try {
                    in = new DataInputStream(this.url2goto.openStream());
                    System.out.println("READY TO READ FROM URL:" + url2goto);
                    r = new BufferedReader(new InputStreamReader(in));
                } catch (Exception err) {
                    throw new RuntimeException("Problem reading from URL " + this.url2goto + ".", err);
                }
            } else {
                if (this.file == null) {
                    throw new RuntimeException("Data file to be parsed can not be null.");
                }
                if (!this.file.exists()) {
                    throw new RuntimeException("The file " + this.file + " does not exist.");
                }
                r = new BufferedReader(new FileReader(this.file));
            }
            if (this.ignorePreHeaderLines > 0) {
                String strLine;
                int k = 0;
                while ((k < this.ignorePreHeaderLines) && ((strLine = r.readLine()) != null)) {
                    k++;
                }
            }
            tokenizer = new StreamTokenizer(r);
            tokenizer.resetSyntax();
            tokenizer.eolIsSignificant(true);
            boolean parseNumbers = false;
            for (int k = 0; k < this.types.size(); k++) {
                Class type = (Class) this.types.get(k);
                if (Number.class.isAssignableFrom(type)) {
                    parseNumbers = true;
                    break;
                }
            }
            if (parseNumbers) {
                tokenizer.parseNumbers();
            }
            tokenizer.eolIsSignificant(true);
            if (this.delimiter.equals("\\t")) {
                tokenizer.whitespaceChars('\t', '\t');
                tokenizer.quoteChar('"');
                tokenizer.whitespaceChars(' ', ' ');
            } else if (this.delimiter.equals(",")) {
                tokenizer.quoteChar('"');
                tokenizer.whitespaceChars(',', ',');
                tokenizer.whitespaceChars(' ', ' ');
            } else {
                if (this.delimiter.length() > 1) {
                    throw new RuntimeException("Delimiter must be a single character.  Multiple character delimiters are not allowed.");
                }
                if (this.delimiter.length() > 0) {
                    tokenizer.whitespaceChars(this.delimiter.charAt(0), this.delimiter.charAt(0));
                } else {
                    tokenizer.wordChars(Character.MIN_VALUE, Character.MAX_VALUE);
                    tokenizer.eolIsSignificant(true);
                    tokenizer.ordinaryChar('\n');
                }
            }
            boolean readingHeaders = true;
            boolean readingInitialValues = false;
            boolean readingData = false;
            boolean readingScientificNotation = false;
            if (this.headers.size() > 0) {
                readingHeaders = false;
                readingInitialValues = true;
            }
            if (this.types.size() > 0) {
                readingInitialValues = false;
                Class targetclass;
                for (int j = 0; j < this.types.size(); j++) {
                    targetclass = (Class) this.types.get(j);
                    try {
                        this.constructors.add(targetclass.getConstructor(String.class));
                    } catch (java.lang.NoSuchMethodException err) {
                        throw new SnifflibDatatypeException("Could not find appropriate constructor for " + targetclass + ". " + err.getMessage());
                    }
                }
                readingData = true;
            }
            int currentColumn = 0;
            int currentRow = 0;
            this.rowcount = 0;
            boolean advanceField = true;
            while (true) {
                tokenizer.nextToken();
                switch(tokenizer.ttype) {
                    case StreamTokenizer.TT_WORD:
                        {
                            advanceField = true;
                            if (readingScientificNotation) {
                                throw new RuntimeException("Problem reading scientific notation at row " + currentRow + " column " + currentColumn + ".");
                            }
                            if (readingHeaders) {
                                this.headers.add(tokenizer.sval);
                            } else {
                                if (readingInitialValues) {
                                    this.types.add(String.class);
                                }
                                if (!this.dryrun) {
                                    if (out.getColumnCount() <= currentColumn) {
                                        out.addColumn((String) this.headers.get(currentColumn), (Class) this.types.get(currentColumn));
                                    }
                                }
                                try {
                                    Constructor construct;
                                    if (currentColumn < this.constructors.size()) {
                                        construct = (Constructor) this.constructors.get(currentColumn);
                                    } else {
                                        Class targetclass = (Class) this.types.get(currentColumn);
                                        construct = targetclass.getConstructor(String.class);
                                        this.constructors.add(construct);
                                    }
                                    try {
                                        try {
                                            try {
                                                if (!this.dryrun) {
                                                    out.setValueAt(construct.newInstance((String) tokenizer.sval), currentRow, currentColumn);
                                                } else if (this.findingTargetValue) {
                                                    Object vvv = construct.newInstance((String) tokenizer.sval);
                                                    this.valueQueue.push(vvv);
                                                    if ((this.targetRow == currentRow) && (this.targetColumn == currentColumn)) {
                                                        this.targetValue = vvv;
                                                        r.close();
                                                        return (null);
                                                    }
                                                }
                                            } catch (java.lang.reflect.InvocationTargetException err) {
                                                throw new SnifflibDatatypeException("Problem constructing 1" + err.getMessage());
                                            }
                                        } catch (java.lang.IllegalAccessException err) {
                                            throw new SnifflibDatatypeException("Problem constructing 2" + err.getMessage());
                                        }
                                    } catch (java.lang.InstantiationException err) {
                                        throw new SnifflibDatatypeException("Problem constructing 3" + err.getMessage());
                                    }
                                } catch (java.lang.NoSuchMethodException err) {
                                    throw new SnifflibDatatypeException("Problem constructing 4" + err.getMessage());
                                }
                            }
                            break;
                        }
                    case StreamTokenizer.TT_NUMBER:
                        {
                            advanceField = true;
                            if (readingHeaders) {
                                throw new SnifflibDatatypeException("Expecting string header at row=" + currentRow + ", column=" + currentColumn + ".");
                            } else {
                                if (readingInitialValues) {
                                    this.types.add(Double.class);
                                }
                                if (!this.dryrun) {
                                    if (out.getColumnCount() <= currentColumn) {
                                        out.addColumn((String) this.headers.get(currentColumn), (Class) this.types.get(currentColumn));
                                    }
                                }
                                try {
                                    Constructor construct;
                                    if (currentColumn < this.constructors.size()) {
                                        construct = (Constructor) this.constructors.get(currentColumn);
                                    } else {
                                        Class targetclass = (Class) this.types.get(currentColumn);
                                        construct = targetclass.getConstructor(double.class);
                                        this.constructors.add(construct);
                                    }
                                    if (readingScientificNotation) {
                                        Double val = this.scientificNumber;
                                        if (!this.dryrun) {
                                            try {
                                                out.setValueAt(new Double(val.doubleValue() * tokenizer.nval), currentRow, currentColumn);
                                            } catch (Exception err) {
                                                throw new SnifflibDatatypeException("Problem constructing " + construct.getDeclaringClass() + "at row " + currentRow + " column " + currentColumn + ".", err);
                                            }
                                        } else if (this.findingTargetValue) {
                                            Double NVAL = new Double(tokenizer.nval);
                                            Object vvv = null;
                                            try {
                                                vvv = Double.parseDouble(val + "E" + NVAL.intValue());
                                            } catch (Exception err) {
                                                throw new RuntimeException("Problem parsing scientific notation at row=" + currentRow + " col=" + currentColumn + ".", err);
                                            }
                                            tokenizer.nextToken();
                                            if (tokenizer.ttype != 'e') {
                                                this.valueQueue.push(vvv);
                                                if ((this.targetRow == currentRow) && (this.targetColumn == currentColumn)) {
                                                    this.targetValue = vvv;
                                                    r.close();
                                                    return (null);
                                                }
                                                currentColumn++;
                                            } else {
                                                tokenizer.pushBack();
                                            }
                                        }
                                        readingScientificNotation = false;
                                    } else {
                                        try {
                                            this.scientificNumber = new Double(tokenizer.nval);
                                            if (!this.dryrun) {
                                                out.setValueAt(this.scientificNumber, currentRow, currentColumn);
                                            } else if (this.findingTargetValue) {
                                                this.valueQueue.push(this.scientificNumber);
                                                if ((this.targetRow == currentRow) && (this.targetColumn == currentColumn)) {
                                                    this.targetValue = this.scientificNumber;
                                                    r.close();
                                                    return (null);
                                                }
                                            }
                                        } catch (Exception err) {
                                            throw new SnifflibDatatypeException("Problem constructing " + construct.getDeclaringClass() + "at row " + currentRow + " column " + currentColumn + ".", err);
                                        }
                                    }
                                } catch (java.lang.NoSuchMethodException err) {
                                    throw new SnifflibDatatypeException("Problem constructing" + err.getMessage());
                                }
                            }
                            break;
                        }
                    case StreamTokenizer.TT_EOL:
                        {
                            if (readingHeaders) {
                                readingHeaders = false;
                                readingInitialValues = true;
                            } else {
                                if (readingInitialValues) {
                                    readingInitialValues = false;
                                    readingData = true;
                                }
                            }
                            if (readingData) {
                                if (valueQueue.getUpperIndex() < currentRow) {
                                    valueQueue.push("");
                                }
                                currentRow++;
                            }
                            break;
                        }
                    case StreamTokenizer.TT_EOF:
                        {
                            if (readingHeaders) {
                                throw new SnifflibDatatypeException("End of file reached while reading headers.");
                            }
                            if (readingInitialValues) {
                                throw new SnifflibDatatypeException("End of file reached while reading initial values.");
                            }
                            if (readingData) {
                                readingData = false;
                            }
                            break;
                        }
                    default:
                        {
                            if (tokenizer.ttype == '"') {
                                advanceField = true;
                                if (readingHeaders) {
                                    this.headers.add(tokenizer.sval);
                                } else {
                                    if (readingInitialValues) {
                                        this.types.add(String.class);
                                    }
                                    if (!this.dryrun) {
                                        if (out.getColumnCount() <= currentColumn) {
                                            out.addColumn((String) this.headers.get(currentColumn), (Class) this.types.get(currentColumn));
                                        }
                                    }
                                    try {
                                        Constructor construct;
                                        if (currentColumn < this.constructors.size()) {
                                            construct = (Constructor) this.constructors.get(currentColumn);
                                        } else {
                                            Class targetclass = (Class) this.types.get(currentColumn);
                                            construct = targetclass.getConstructor(String.class);
                                            this.constructors.add(construct);
                                        }
                                        try {
                                            try {
                                                try {
                                                    if (!this.dryrun) {
                                                        out.setValueAt(construct.newInstance((String) tokenizer.sval), currentRow, currentColumn);
                                                    } else if (this.findingTargetValue) {
                                                        Object vvv = construct.newInstance((String) tokenizer.sval);
                                                        this.valueQueue.push(vvv);
                                                        if ((this.targetRow == currentRow) && (this.targetColumn == currentColumn)) {
                                                            this.targetValue = vvv;
                                                            r.close();
                                                            return (null);
                                                        }
                                                    }
                                                } catch (java.lang.reflect.InvocationTargetException err) {
                                                    throw new SnifflibDatatypeException("Problem constructing a " + construct, err);
                                                }
                                            } catch (java.lang.IllegalAccessException err) {
                                                throw new SnifflibDatatypeException("Problem constructing 2 ", err);
                                            }
                                        } catch (java.lang.InstantiationException err) {
                                            throw new SnifflibDatatypeException("Problem constructing 3 ", err);
                                        }
                                    } catch (java.lang.NoSuchMethodException err) {
                                        throw new SnifflibDatatypeException("Problem constructing 4", err);
                                    }
                                }
                            } else if (tokenizer.ttype == 'e') {
                                Class targetclass = (Class) this.types.get(currentColumn);
                                if (Number.class.isAssignableFrom(targetclass)) {
                                    currentColumn--;
                                    readingScientificNotation = true;
                                    advanceField = false;
                                }
                            } else {
                                advanceField = false;
                            }
                            break;
                        }
                }
                if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
                    advanceField = false;
                    break;
                }
                if (advanceField) {
                    currentColumn++;
                    if (!readingHeaders) {
                        if (currentColumn >= this.headers.size()) {
                            currentColumn = 0;
                        }
                    }
                }
            }
            if (!readingHeaders) {
                this.rowcount = currentRow;
            } else {
                this.rowcount = 0;
                readingHeaders = false;
                if (this.ignorePostHeaderLines > 0) {
                    String strLine;
                    int k = 0;
                    while ((k < this.ignorePostHeaderLines) && ((strLine = r.readLine()) != null)) {
                        k++;
                    }
                }
            }
            r.close();
        } catch (java.io.IOException err) {
            throw new SnifflibDatatypeException(err.getMessage());
        }
        if (!this.dryrun) {
            for (int j = 0; j < this.headers.size(); j++) {
                out.setColumnName(j, (String) this.headers.get(j));
            }
        }
        return (out);
    }

    public DataSet parse() throws SnifflibDatatypeException {
        NumberFormat numformat = NumberFormat.getInstance();
        if (this.headers.size() != this.types.size()) {
            throw new SnifflibDatatypeException("Different number of headers (" + this.headers.size() + ") and types(" + this.types.size() + ").");
        }
        DataSet out = null;
        if (!this.dryrun) {
            out = new DataSet();
        }
        BufferedReader r = null;
        StreamTokenizer tokenizer = null;
        try {
            if (this.isURL) {
                if (this.url2goto == null) {
                    return (null);
                }
                DataInputStream in = null;
                try {
                    in = new DataInputStream(this.url2goto.openStream());
                    System.out.println("READY TO READ FROM URL:" + url2goto);
                    r = new BufferedReader(new InputStreamReader(in));
                } catch (Exception err) {
                    throw new RuntimeException("Problem reading from URL " + this.url2goto + ".", err);
                }
            } else {
                if (this.file == null) {
                    throw new RuntimeException("Data file to be parsed can not be null.");
                }
                if (!this.file.exists()) {
                    throw new RuntimeException("The file " + this.file + " does not exist.");
                }
                r = new BufferedReader(new FileReader(this.file));
            }
            if (this.ignorePreHeaderLines > 0) {
                String strLine;
                int k = 0;
                while ((k < this.ignorePreHeaderLines) && ((strLine = r.readLine()) != null)) {
                    k++;
                }
            }
            tokenizer = new StreamTokenizer(r);
            tokenizer.resetSyntax();
            tokenizer.eolIsSignificant(true);
            tokenizer.parseNumbers();
            if (this.delimiter.equals("\\t")) {
                tokenizer.whitespaceChars('\t', '\t');
            }
            if (this.delimiter.equals(",")) {
                tokenizer.whitespaceChars(',', ',');
            }
            tokenizer.quoteChar('"');
            tokenizer.whitespaceChars(' ', ' ');
            boolean readingHeaders = true;
            boolean readingInitialValues = false;
            boolean readingData = false;
            boolean readingScientificNotation = false;
            if (this.headers.size() > 0) {
                readingHeaders = false;
                readingInitialValues = true;
            }
            if (this.types.size() > 0) {
                readingInitialValues = false;
                Class targetclass;
                for (int j = 0; j < this.types.size(); j++) {
                    targetclass = (Class) this.types.get(j);
                    try {
                        this.constructors.add(targetclass.getConstructor(String.class));
                    } catch (java.lang.NoSuchMethodException err) {
                        throw new SnifflibDatatypeException("Could not find appropriate constructor for " + targetclass + ". " + err.getMessage());
                    }
                }
                readingData = true;
            }
            int currentColumn = 0;
            int currentRow = 0;
            this.rowcount = 0;
            boolean advanceField = true;
            while (true) {
                tokenizer.nextToken();
                switch(tokenizer.ttype) {
                    case StreamTokenizer.TT_WORD:
                        {
                            if (readingScientificNotation) {
                                throw new RuntimeException("Problem reading scientific notation at row " + currentRow + " column " + currentColumn + ".");
                            }
                            advanceField = true;
                            if (readingHeaders) {
                                this.headers.add(tokenizer.sval);
                            } else {
                                if (readingInitialValues) {
                                    this.types.add(String.class);
                                }
                                if (!this.dryrun) {
                                    if (out.getColumnCount() <= currentColumn) {
                                        out.addColumn((String) this.headers.get(currentColumn), (Class) this.types.get(currentColumn));
                                    }
                                }
                                try {
                                    Constructor construct;
                                    if (currentColumn < this.constructors.size()) {
                                        construct = (Constructor) this.constructors.get(currentColumn);
                                    } else {
                                        Class targetclass = (Class) this.types.get(currentColumn);
                                        construct = targetclass.getConstructor(String.class);
                                        this.constructors.add(construct);
                                    }
                                    try {
                                        try {
                                            try {
                                                if (!this.dryrun) {
                                                    out.setValueAt(construct.newInstance((String) tokenizer.sval), currentRow, currentColumn);
                                                } else if (this.findingTargetValue) {
                                                    if ((this.targetRow == currentRow) && (this.targetColumn == currentColumn)) {
                                                        this.targetValue = construct.newInstance((String) tokenizer.sval);
                                                        r.close();
                                                        return (null);
                                                    }
                                                }
                                            } catch (java.lang.reflect.InvocationTargetException err) {
                                                throw new SnifflibDatatypeException("Problem constructing 1" + err.getMessage());
                                            }
                                        } catch (java.lang.IllegalAccessException err) {
                                            throw new SnifflibDatatypeException("Problem constructing 2" + err.getMessage());
                                        }
                                    } catch (java.lang.InstantiationException err) {
                                        throw new SnifflibDatatypeException("Problem constructing 3" + err.getMessage());
                                    }
                                } catch (java.lang.NoSuchMethodException err) {
                                    throw new SnifflibDatatypeException("Problem constructing 4" + err.getMessage());
                                }
                            }
                            break;
                        }
                    case StreamTokenizer.TT_NUMBER:
                        {
                            advanceField = true;
                            if (readingHeaders) {
                                throw new SnifflibDatatypeException("Expecting string header at row=" + currentRow + ", column=" + currentColumn + ".");
                            } else {
                                if (readingInitialValues) {
                                    this.types.add(Double.class);
                                }
                                if (!this.dryrun) {
                                    if (out.getColumnCount() <= currentColumn) {
                                        out.addColumn((String) this.headers.get(currentColumn), (Class) this.types.get(currentColumn));
                                    }
                                }
                                try {
                                    Constructor construct;
                                    if (currentColumn < this.constructors.size()) {
                                        construct = (Constructor) this.constructors.get(currentColumn);
                                    } else {
                                        Class targetclass = (Class) this.types.get(currentColumn);
                                        construct = targetclass.getConstructor(double.class);
                                        this.constructors.add(construct);
                                    }
                                    if (readingScientificNotation) {
                                        Double val = this.scientificNumber;
                                        if (!this.dryrun) {
                                            try {
                                                out.setValueAt(new Double(val.doubleValue() * tokenizer.nval), currentRow, currentColumn);
                                            } catch (Exception err) {
                                                throw new SnifflibDatatypeException("Problem constructing " + construct.getDeclaringClass() + "at row " + currentRow + " column " + currentColumn + ".", err);
                                            }
                                        } else if (this.findingTargetValue) {
                                            if ((this.targetRow == currentRow) && (this.targetColumn == currentColumn)) {
                                                Double NVAL = new Double(tokenizer.nval);
                                                try {
                                                    this.targetValue = numformat.parse(val + "E" + NVAL);
                                                } catch (Exception err) {
                                                    throw new RuntimeException("Problem parsing scientific notation at row=" + currentRow + " col=" + currentColumn + ".");
                                                }
                                                tokenizer.nextToken();
                                                if (tokenizer.ttype != 'e') {
                                                    r.close();
                                                    return (null);
                                                } else {
                                                    tokenizer.pushBack();
                                                }
                                            }
                                        }
                                        readingScientificNotation = false;
                                    } else {
                                        try {
                                            this.scientificNumber = new Double(tokenizer.nval);
                                            if (!this.dryrun) {
                                                out.setValueAt(this.scientificNumber, currentRow, currentColumn);
                                            } else if (this.findingTargetValue) {
                                                if ((this.targetRow == currentRow) && (this.targetColumn == currentColumn)) {
                                                    this.targetValue = this.scientificNumber;
                                                    r.close();
                                                    return (null);
                                                }
                                            }
                                        } catch (Exception err) {
                                            throw new SnifflibDatatypeException("Problem constructing " + construct.getDeclaringClass() + "at row " + currentRow + " column " + currentColumn + ".", err);
                                        }
                                    }
                                } catch (java.lang.NoSuchMethodException err) {
                                    throw new SnifflibDatatypeException("Problem constructing" + err.getMessage());
                                }
                            }
                            break;
                        }
                    case StreamTokenizer.TT_EOL:
                        {
                            if (readingHeaders) {
                                readingHeaders = false;
                                readingInitialValues = true;
                            } else {
                                if (readingInitialValues) {
                                    readingInitialValues = false;
                                    readingData = true;
                                }
                            }
                            if (readingData) {
                                currentRow++;
                            }
                            break;
                        }
                    case StreamTokenizer.TT_EOF:
                        {
                            if (readingHeaders) {
                                throw new SnifflibDatatypeException("End of file reached while reading headers.");
                            }
                            if (readingInitialValues) {
                                throw new SnifflibDatatypeException("End of file reached while reading initial values.");
                            }
                            if (readingData) {
                                readingData = false;
                            }
                            break;
                        }
                    default:
                        {
                            if (tokenizer.ttype == '"') {
                                advanceField = true;
                                if (readingHeaders) {
                                    this.headers.add(tokenizer.sval);
                                } else {
                                    if (readingInitialValues) {
                                        this.types.add(String.class);
                                    }
                                    if (!this.dryrun) {
                                        if (out.getColumnCount() <= currentColumn) {
                                            out.addColumn((String) this.headers.get(currentColumn), (Class) this.types.get(currentColumn));
                                        }
                                    }
                                    try {
                                        Constructor construct;
                                        if (currentColumn < this.constructors.size()) {
                                            construct = (Constructor) this.constructors.get(currentColumn);
                                        } else {
                                            Class targetclass = (Class) this.types.get(currentColumn);
                                            construct = targetclass.getConstructor(String.class);
                                            this.constructors.add(construct);
                                        }
                                        try {
                                            try {
                                                try {
                                                    if (!this.dryrun) {
                                                        out.setValueAt(construct.newInstance((String) tokenizer.sval), currentRow, currentColumn);
                                                    } else if (this.findingTargetValue) {
                                                        if ((this.targetRow == currentRow) && (this.targetColumn == currentColumn)) {
                                                            this.targetValue = construct.newInstance((String) tokenizer.sval);
                                                            r.close();
                                                            return (null);
                                                        }
                                                    }
                                                } catch (java.lang.reflect.InvocationTargetException err) {
                                                    throw new SnifflibDatatypeException("Problem constructing 1 " + err.getMessage());
                                                }
                                            } catch (java.lang.IllegalAccessException err) {
                                                throw new SnifflibDatatypeException("Problem constructing 2 " + err.getMessage());
                                            }
                                        } catch (java.lang.InstantiationException err) {
                                            throw new SnifflibDatatypeException("Problem constructing 3 " + err.getMessage());
                                        }
                                    } catch (java.lang.NoSuchMethodException err) {
                                        throw new SnifflibDatatypeException("Problem constructing 4" + err.getMessage());
                                    }
                                }
                            } else if (tokenizer.ttype == 'e') {
                                Class targetclass = (Class) this.types.get(currentColumn);
                                if (Number.class.isAssignableFrom(targetclass)) {
                                    currentColumn--;
                                    readingScientificNotation = true;
                                    advanceField = false;
                                }
                            } else {
                                advanceField = false;
                            }
                            break;
                        }
                }
                if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
                    break;
                }
                if (advanceField) {
                    currentColumn++;
                    if (!readingHeaders) {
                        if (currentColumn >= this.headers.size()) {
                            currentColumn = 0;
                        }
                    }
                }
            }
            if (!readingHeaders) {
                this.rowcount = currentRow;
            } else {
                this.rowcount = 0;
                readingHeaders = false;
                if (this.ignorePostHeaderLines > 0) {
                    String strLine;
                    int k = 0;
                    while ((k < this.ignorePostHeaderLines) && ((strLine = r.readLine()) != null)) {
                        k++;
                    }
                }
            }
            r.close();
        } catch (java.io.IOException err) {
            throw new SnifflibDatatypeException(err.getMessage());
        }
        if (!this.dryrun) {
            for (int j = 0; j < this.headers.size(); j++) {
                out.setColumnName(j, (String) this.headers.get(j));
            }
        }
        return (out);
    }
}
