package com.rapidminer.operator.nio.model;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.csv.LineReader;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.nio.model.ParsingError.ErrorCode;
import com.rapidminer.tools.CSVParseException;
import com.rapidminer.tools.LineParser;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ProgressListener;

/**
 * 
 * @author Simon Fischer
 * 
 */
public class CSVResultSet implements DataResultSet {

    private CSVResultSetConfiguration configuration;

    private LineReader reader;

    private LineParser parser;

    private String[] next;

    private String[] current;

    private int currentRow;

    private String[] columnNames;

    private int[] valueTypes;

    private int numColumns = 0;

    private Operator operator;

    private final List<ParsingError> errors = new LinkedList<ParsingError>();

    public CSVResultSet(CSVResultSetConfiguration configuration, Operator operator) throws OperatorException {
        this.configuration = configuration;
        this.operator = operator;
        open();
    }

    private void open() throws OperatorException {
        getErrors().clear();
        close();
        InputStream in = openStream();
        if (configuration.getEncoding().name().equals("UTF-8")) {
            try {
                if (in.read() != 239 || in.read() != 187 || in.read() != 191) {
                    in.close();
                    in = openStream();
                }
            } catch (IOException e) {
                try {
                    in.close();
                } catch (IOException e1) {
                }
                throw new UserError(operator, e, 321, configuration.getCsvFile(), e.toString());
            }
        }
        reader = new LineReader(in, configuration.getEncoding());
        parser = new LineParser(configuration);
        try {
            readNext();
        } catch (IOException e) {
            try {
                in.close();
            } catch (IOException e1) {
            }
            throw new UserError(operator, e, 321, configuration.getCsvFile(), e.toString());
        }
        if (next == null) {
            errors.add(new ParsingError(1, -1, ErrorCode.FILE_SYNTAX_ERROR, "No valid line found."));
            columnNames = new String[0];
            valueTypes = new int[0];
        } else {
            columnNames = new String[next.length];
            for (int i = 0; i < next.length; i++) {
                columnNames[i] = "att" + (i + 1);
            }
            valueTypes = new int[next.length];
            Arrays.fill(valueTypes, Ontology.NOMINAL);
            currentRow = -1;
        }
    }

    private InputStream openStream() throws UserError {
        try {
            URL url = new URL(configuration.getCsvFile());
            try {
                return url.openStream();
            } catch (IOException e) {
                throw new UserError(operator, 301, e, configuration.getCsvFile());
            }
        } catch (MalformedURLException e) {
            try {
                String csvFile = configuration.getCsvFile();
                if (csvFile == null) {
                    throw new UserError(this.operator, "file_consumer.no_file_defined");
                }
                return new FileInputStream(csvFile);
            } catch (FileNotFoundException e1) {
                throw new UserError(operator, 301, e1, configuration.getCsvFile());
            }
        }
    }

    private void readNext() throws IOException {
        do {
            String line = reader.readLine();
            if (line == null) {
                next = null;
                return;
            }
            try {
                next = parser.parse(line);
                if (next != null) {
                    break;
                }
            } catch (CSVParseException e) {
                ParsingError parsingError = new ParsingError(currentRow, -1, ErrorCode.FILE_SYNTAX_ERROR, line, e);
                getErrors().add(parsingError);
                String warning = "Could not parse line " + currentRow + " in input: " + e.toString();
                if (operator != null) {
                    operator.logWarning(warning);
                } else {
                    Logger.getLogger(getClass().getName()).warning(warning);
                }
                next = new String[] { line };
            }
        } while (true);
        numColumns = Math.max(numColumns, next.length);
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public void next(ProgressListener listener) throws OperatorException {
        current = next;
        currentRow++;
        try {
            readNext();
        } catch (IOException e) {
            throw new UserError(operator, e, 321, configuration.getCsvFile(), e.toString());
        }
    }

    @Override
    public int getNumberOfColumns() {
        if (current != null) {
            return current.length;
        } else {
            return numColumns;
        }
    }

    @Override
    public String[] getColumnNames() {
        return columnNames;
    }

    @Override
    public boolean isMissing(int columnIndex) {
        return columnIndex >= current.length || current[columnIndex] == null || current[columnIndex].isEmpty();
    }

    @Override
    public Number getNumber(int columnIndex) throws ParseException {
        throw new ParseException(new ParsingError(currentRow, columnIndex, ParsingError.ErrorCode.UNPARSEABLE_REAL, current[columnIndex]));
    }

    @Override
    public String getString(int columnIndex) throws ParseException {
        if (columnIndex < current.length) {
            return current[columnIndex];
        } else {
            return null;
        }
    }

    @Override
    public Date getDate(int columnIndex) throws ParseException {
        throw new ParseException(new ParsingError(currentRow, columnIndex, ParsingError.ErrorCode.UNPARSEABLE_DATE, current[columnIndex]));
    }

    @Override
    public ValueType getNativeValueType(int columnIndex) throws ParseException {
        return ValueType.STRING;
    }

    @Override
    public void close() throws OperatorException {
        if (reader == null) {
            return;
        }
        try {
            reader.close();
        } catch (IOException e) {
            throw new UserError(operator, 321, e, configuration.getCsvFile(), e.toString());
        } finally {
            reader = null;
        }
    }

    @Override
    public void reset(ProgressListener listener) throws OperatorException {
        open();
    }

    @Override
    public int[] getValueTypes() {
        return valueTypes;
    }

    @Override
    public int getCurrentRow() {
        return currentRow;
    }

    public List<ParsingError> getErrors() {
        return errors;
    }
}
