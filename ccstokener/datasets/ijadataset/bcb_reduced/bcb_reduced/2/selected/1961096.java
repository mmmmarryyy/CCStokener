package com.rapidminer.operator.io;

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
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.csv.LineReader;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.nio.model.DataResultSet;
import com.rapidminer.operator.nio.model.ParseException;
import com.rapidminer.operator.nio.model.ParsingError;
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
public class CSVbatchResultSet implements DataResultSet {

    private CSVbatchResultSetConfiguration configuration;

    private LineReader reader;

    private LineParser parser;

    private String[] next;

    private String[] current;

    private int currentRow;

    private int batch;

    private String[] columnNames;

    private int[] valueTypes;

    private int numColumns = 0;

    private Operator operator;

    private final List<ParsingError> errors = new LinkedList<ParsingError>();

    public CSVbatchResultSet(CSVbatchResultSetConfiguration configuration, Operator operator) throws OperatorException {
        this.configuration = configuration;
        this.operator = operator;
        open();
    }

    private void open() throws OperatorException {
        getErrors().clear();
        close();
        InputStream in;
        try {
            URL url = new URL(configuration.getCsvFile());
            try {
                in = url.openStream();
            } catch (IOException e) {
                throw new UserError(operator, 301, e, configuration.getCsvFile());
            }
        } catch (MalformedURLException e) {
            try {
                in = new FileInputStream(configuration.getCsvFile());
            } catch (FileNotFoundException e1) {
                throw new UserError(operator, 301, e1, configuration.getCsvFile());
            }
        }
        reader = new LineReader(in, configuration.getEncoding());
        parser = new LineParser(configuration);
        try {
            readNext();
        } catch (IOException e) {
            throw new UserError(operator, e, 321, configuration.getCsvFile(), e.toString());
        }
        if (next == null) {
            errors.add(new ParsingError(1, -1, ErrorCode.FILE_SYNTAX_ERROR, "No valid line found."));
            columnNames = new String[0];
            valueTypes = new int[0];
        } else {
            columnNames = new String[next.length + 1];
            for (int i = 0; i < next.length + 1; i++) {
                columnNames[i] = "att" + (i + 1);
            }
            valueTypes = new int[next.length + 1];
            Arrays.fill(valueTypes, Ontology.NOMINAL);
            currentRow = -1;
            batch = -1;
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
                if (batch == -1) batch = 0; else {
                    while (line.trim().equals("")) {
                        batch++;
                        line = reader.readLine();
                        if (line == null) {
                            next = null;
                            return;
                        }
                    }
                }
                next = parser.parse(line);
                if (next != null) {
                    String[] tempNext = new String[next.length + 1];
                    for (int i = 0; i < next.length; i++) {
                        tempNext[i] = next[i];
                    }
                    tempNext[tempNext.length - 1] = new Integer(batch).toString();
                    next = tempNext;
                    break;
                }
            } catch (CSVParseException e) {
                getErrors().add(new ParsingError(currentRow, -1, ErrorCode.FILE_SYNTAX_ERROR, line, e));
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
        if (current == null) {
            System.out.println();
        }
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
        return (columnIndex >= current.length) || (current[columnIndex] == null) || current[columnIndex].isEmpty();
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
        batch = -1;
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
