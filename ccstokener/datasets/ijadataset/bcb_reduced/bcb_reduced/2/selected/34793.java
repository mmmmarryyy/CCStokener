package info.absu.snow.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.text.ParseException;

/**
 * This class is a helper to load up data from a source. The source can be specified as a text string, or a URL of a
 * resource. For URLs a charset can be specified. The type of data source can be specified, but if it is not, the type
 * will be guessed. 
 * @author Denys Rtveliashvili
 *
 */
public class ConfigurableSourceProvider implements SourceDataProvider {

    /**
	 * Type of the source data
	 *
	 */
    public enum DataType {

        /**
		 * Comma-separated values
		 */
        CSV, /**
		 * JavaScript Object Notation (in fact, it is not a real JSON, just a subset of this format)
		 */
        JSON
    }

    private DataType type;

    private String data;

    private String nullName;

    private URL url;

    private String charset;

    /**
	 * Attempts to read the data from the source specified before calling this method.
	 * If the type of the data source was not specified, tries to guess it.
	 * @return the data
	 * @throws IllegalArgumentException if the data can not be read
	 */
    public Object getDataRoot() {
        try {
            if (type == null) {
                try {
                    return parseData(DataType.JSON);
                } catch (ParseException e) {
                    return parseData(DataType.CSV);
                }
            } else return parseData(type);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Unable to parse the provided data", e);
        }
    }

    /**
	 * Tries to parse the data.
	 * @param type type of the data to be read
	 * @return the parsed data
	 * @throws ParseException if there was any problem with parsing the data
	 */
    private Object parseData(DataType type) throws ParseException {
        try {
            final Reader reader = getReader();
            switch(type) {
                case CSV:
                    final CSVParser csvParser = new CSVParser(reader);
                    csvParser.setNullAlias(nullName);
                    return csvParser.getDataRoot();
                case JSON:
                    final PseudoJSONParser jsonParser = new PseudoJSONParser(reader);
                    jsonParser.setNullAlias(nullName);
                    return jsonParser.getDataRoot();
                default:
                    throw new IllegalStateException("Unrecognized data type " + type);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Configuration is inaccessible", e);
        }
    }

    /**
	 * Creates a reader for the data depending on the way the data source was specified.
	 * @return the reader which can read up the source data or null if no data was specified at all
	 * @throws IOException if there was a problem with reading the data
	 */
    private Reader getReader() throws IOException {
        if (data != null) {
            if (url != null) throw new IllegalArgumentException("URL for source data and the data itself must never be specified together.");
            if (charset != null) throw new IllegalArgumentException("Charset has sense only for URL-based data");
            return new StringReader(data);
        } else if (url != null) {
            InputStream stream = url.openStream();
            if (charset == null) return new InputStreamReader(stream); else return new InputStreamReader(stream, charset);
        }
        return null;
    }

    /**
	 * Specifies the source data as a text string
	 * @param data the source data
	 */
    public void setData(String data) {
        this.data = data;
    }

    /**
	 * Specifies the type of the data to be read 
	 * @param type type of data
	 */
    public void setType(String type) {
        this.type = DataType.valueOf(type.toUpperCase());
    }

    /**
	 * Specifies the source data by telling its URL
	 * @param url the URL of the source data
	 */
    public void setUrl(URL url) {
        this.url = url;
    }

    /**
	 * Specifies the charset of the source data. This is not applicable for the source data specified as a
	 * text string.
	 * @param charset charset name 
	 */
    public void setCharset(String charset) {
        this.charset = charset;
    }

    /**
	 * Specifies the name of the null values within the source data
	 * @param nullName the name of null values
	 */
    public void setNullName(String nullName) {
        this.nullName = nullName;
    }
}
