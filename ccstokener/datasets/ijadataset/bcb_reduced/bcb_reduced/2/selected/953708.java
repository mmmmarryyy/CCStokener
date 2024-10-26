package mipt.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * AbstractReader reading tab/comma/..-separated tables.
 * Can read separate row or separate column from the table.
 * @author Zhmurov
 */
public class TableReader extends AbstractReader {

    protected String separator = "\t";

    protected BufferedReader reader;

    public TableReader(File file) throws FileNotFoundException {
        super(file);
    }

    public TableReader(URL url) throws IOException {
        super(url);
    }

    public TableReader(String source) throws IOException {
        super(source);
    }

    /**
	 * Read the row from the file. If the file end - return null.
	 * @return the next row from the file
	 * @throws IOException if I/O error occurs. 
	 */
    public String[] readRow() throws IOException {
        if (reader.ready()) {
            return reader.readLine().split(separator);
        } else {
            return null;
        }
    }

    /**
	 * Reads all the file at once. Closes reader!
	 * @return the file data
	 */
    public String[][] readAllData() {
        LinkedList data = new LinkedList();
        try {
            while (reader.ready()) {
                data.add(reader.readLine().split(separator));
            }
            close();
            String[][] result = new String[data.size()][];
            data.toArray(result);
            return result;
        } catch (IOException e) {
            exceptionOccured(e);
            return null;
        }
    }

    /**
	 * Reads the column and represent its data as List.
	 * After reading the reader is closed.
	 * If there is no column with requested index, empty array returned.
	 * @param columnIndex index of the column to read. The first column is 0.
	 * @return the LinkedList with the column data.
	 */
    public List readColumnAsCollection(int columnIndex) {
        LinkedList data = new LinkedList();
        try {
            while (reader.ready()) {
                try {
                    data.add(reader.readLine().split(separator)[columnIndex]);
                } catch (ArrayIndexOutOfBoundsException e) {
                    break;
                }
            }
            close();
            return data;
        } catch (IOException e) {
            exceptionOccured(e);
            return null;
        }
    }

    /**
	 * Reads the column and represent its data as array of Strings. Calls the {@link #readColumnAsCollection(int)}.
	 * @param columnIndex index of the column to read. The first column is 0.
	 * @return the array with the column data.
	 * @throws ArrayIndexOutOfBoundsException when the requested column index is out of bounds (the number of the columns is less then the (columnsIndex + 1)
	 */
    public String[] readColumn(int columnIndex) {
        List data = readColumnAsCollection(columnIndex);
        String[] result = new String[data.size()];
        data.toArray(result);
        return result;
    }

    /**
	 * Get current separator.
	 * @return
	 */
    public final String getSeparator() {
        return separator;
    }

    /**
	 * Set the separator.
	 * @param separator string to be the separator
	 */
    public void setSeparator(String separator) {
        this.separator = separator;
    }

    /**
	 * @see mipt.io.AbstractReader#createReader(java.net.URL)
	 */
    protected void createReader(URL url) throws IOException {
        reader = initReader(url.openStream());
    }

    /**
	 * @see mipt.io.AbstractReader#createReader(java.io.File)
	 */
    protected void createReader(File file) throws FileNotFoundException {
        reader = initReader(new FileInputStream(file));
    }

    /**
	 * Factory method.
	 */
    protected BufferedReader initReader(InputStream stream) {
        return new BufferedReader(new InputStreamReader(stream));
    }

    /**
	 * @return reader
	 */
    public final BufferedReader getReader() {
        return reader;
    }

    /**
	 * 
	 */
    public void close() {
        try {
            reader.close();
        } catch (IOException e) {
        }
    }
}
