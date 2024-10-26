package net.sf.jplist.input;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import net.sf.jplist.core.Document;
import net.sf.jplist.core.JPListException;
import net.sf.jplist.input.ascii.AsciiPListParser;
import net.sf.jplist.input.ascii.ParseException;

/**
 * Builds a JPList Document object from an ASCII representation of a PList.
 * The class provides various methods to build a JPList Document object from
 * a variety of input sources.
 * @author Sujit Pal (spal@users.sourceforge.net)
 * @version $Revision: 1.1 $
 */
public class AsciiBuilder {

    /**
     * Instantiate a new AsciiBuilder object.
     */
    public AsciiBuilder() {
        super();
    }

    /**
     * Builds a JPList Document object from the contents of a File object.
     * @param file the name of the file to read.
     * @return a JPList Document object.
     */
    public Document build(File file) throws JPListException {
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            return AsciiPListParser.parse(reader);
        } catch (FileNotFoundException e) {
            throw new JPListException("File not found:", e);
        } catch (ParseException e) {
            throw new JPListException("Unrecognized format", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Builds a JPList Document object from an InputStream.
     * @param istream the InputStream to read.
     * @return a JPList Document object.
     * @throws JPListException if there was a problem.
     */
    public Document build(InputStream istream) throws JPListException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(istream));
            return AsciiPListParser.parse(reader);
        } catch (ParseException e) {
            throw new JPListException("Unrecognized format", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Builds a JPList Document object from a Reader.
     * @param reader the Reader to read.
     * @return a JPList Document object.
     * @throws JPListException if there was a problem.
     */
    public Document build(Reader reader) throws JPListException {
        try {
            return AsciiPListParser.parse(reader);
        } catch (ParseException e) {
            throw new JPListException("Unrecognized format", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Builds a JPList Document object from a URL.
     * @param url the URL to read.
     * @return a JPList Document object.
     * @throws JPListException if there was a problem.
     */
    public Document build(URL url) throws JPListException {
        URLConnection urlconn = null;
        InputStream istream = null;
        try {
            urlconn = url.openConnection();
            istream = urlconn.getInputStream();
            return build(istream);
        } catch (IOException e) {
            throw new JPListException("IO Exception", e);
        } finally {
            if (istream != null) {
                try {
                    istream.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Builds a JPList Document object from a String.
     * @param input the String containing the ASCII representation of the PList.
     * @return a JPList Document object.
     * @throws JPListException if there was a problem.
     */
    public Document build(String input) throws JPListException {
        StringReader reader = null;
        reader = new StringReader(input);
        return build(reader);
    }
}
