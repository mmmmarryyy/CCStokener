package bop.parser;

import java.applet.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.net.*;
import bop.datamodel.*;
import bop.adapter.GameAdapter;
import bop.exception.OutputMalFormatException;

public class GameParser extends ResultParser {

    public GameParser() {
    }

    public boolean parseResults(URL url, String data_type, CurationI curation, Date analysis_date, String regexp) throws OutputMalFormatException {
        boolean parsed = false;
        try {
            InputStream data_stream = url.openStream();
            parsed = parseResults(data_stream, data_type, curation, analysis_date, regexp);
            data_stream.close();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return parsed;
    }

    public boolean parseResults(InputStream data_stream, String data_type, CurationI curation, Date analysis_date, String regexp) throws OutputMalFormatException {
        boolean parsed = false;
        GameAdapter parser = new GameAdapter();
        try {
            System.out.println("Parsing XML");
            parsed = parser.readXML(curation, data_stream);
            System.gc();
            data_stream.close();
        } catch (Exception ex) {
            System.out.println("Couldn't parse XML");
            ex.printStackTrace();
        }
        return parsed;
    }

    public boolean canParse(String file_name) {
        boolean parseable = false;
        try {
            FileInputStream istream = new FileInputStream(file_name);
            parseable = canParse(istream);
        } catch (Exception ex) {
            System.out.println("Couldn't verify " + file_name + " as XML data");
        }
        return parseable;
    }

    public boolean canParse(InputStream in) {
        boolean parseable = false;
        try {
            BufferedReader xml_data = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = xml_data.readLine()) != null && parseable == false) {
                parseable = ((line.indexOf("<game") >= 0) || (line.indexOf("<GAME") >= 0));
            }
            xml_data.close();
        } catch (Exception ex) {
            System.out.println("Couldn't verify input stream as XML data " + "and error: " + ex.getMessage());
        }
        return parseable;
    }
}
