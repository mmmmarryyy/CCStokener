package net.sourceforge.ondex.args;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import net.sourceforge.ondex.InvalidPluginArgumentException;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;

/**
 * ArgumentDefinition for an input stream.
 * Can convert String into a valid inouStream
 * Can handle files
 *
 * @author hindlem, Christian Brenninkmeijer
 */
public class ReaderArgumentDefinition extends AbstractArgumentDefinition<Reader> {

    private String suffix;

    /**
     * Constructor which fills all internal fields.
     *
     * @param name                     String
     * @param description              String
     * @param suffix                   String (Optional Filename suffix to suggest the format)
     *                                 Example ".oxl","xml","txt","csv".
     *                                 Use bit before ".gz" or ".zip".
     * @param required                 boolean
     * @param canHaveMultipleInstances boolean
     */
    public ReaderArgumentDefinition(String name, String description, String suffix, boolean required, boolean canHaveMultipleInstances) {
        super(name, description, required, canHaveMultipleInstances);
        this.suffix = suffix;
    }

    /**
     * Constructor which fills most internal fields and sets multiple instances
     * to false.
     *
     * @param name        String
     * @param description String
     * @param suffix                   String (Optional Filename suffix to suggest the format)
     *                                 Example ".oxl","xml","txt","csv".
     *                                 Use bit before ".gz" or ".zip".
     * @param required    boolean
     */
    public ReaderArgumentDefinition(String name, String description, String suffix, boolean required) {
        this(name, description, suffix, required, false);
    }

    /**
     * Returns associated java class.
     *
     * @return Class
     */
    @Override
    public Class<Reader> getClassType() {
        return Reader.class;
    }

    /**
     * Returns default value.
     *
     * @return null
     */
    public Reader getDefaultValue() {
        return null;
    }

    /**
     * Checks for valid argument.
     *
     * @return boolean
     */
    public void isValidArgument(Object obj) throws InvalidPluginArgumentException {
        if (obj instanceof String) {
            if (obj.toString().trim().length() == 0) {
                throw new InvalidPluginArgumentException("An empty argument is invalid for " + this.getName());
            }
            File file = new File((String) obj);
            if (file.exists()) {
                return;
            }
            try {
                URL url = new URL((String) obj);
                return;
            } catch (MalformedURLException ex) {
            }
            throw new InvalidPluginArgumentException("The arguement " + obj + " of type String, could not be converted " + "to either a URL or an existing file for " + this.getName() + " ");
        }
        if (obj instanceof Reader) {
            return;
        }
        throw new InvalidPluginArgumentException("A " + getName() + " argument is required to be specified as a Reader for " + this.getName() + " class " + obj.getClass().getName() + " was found ");
    }

    /**
     * Parser arguement String into a Reader.
     * 
     * @param arguement The Reader as a String
     *                  Legal values for the String are:
     *                  1) Path and Name of a File Stored on the machine running the code
     *                      a) File that end with ".zip" will be assumed to be in ZIP Format.
     *                      b) File that end with ".gz" will be assumed to be in GZ Format.
     *                      c) All other files are assumed to be in String format.
     *                  2) Valid URL pointing to the file. 
     *                      (Same format assumptions as for file names)
     *                  3) Actaul String value. Avoid using this option if data is in a URL;
     * @return          The Reader represented by this String
     */
    public static Reader StringToReader(String argument) throws InvalidPluginArgumentException {
        if (argument == null) {
            throw new InvalidPluginArgumentException("Illegal Attempt to convert null to a Reader.");
        }
        if (argument.isEmpty()) {
            throw new InvalidPluginArgumentException("Illegal Attempt to convert empty String to a Reader.");
        }
        if (argument.toLowerCase().endsWith(".gz")) {
            return GZipToReader(argument);
        }
        if (argument.toLowerCase().endsWith(".zip")) {
            return GZipToReader(argument);
        }
        return notZipToReader(argument);
    }

    /**
     * @param arguement The Reader as a String
     *                  Legal values for the String are:
     *                  File is assumed to be in GZip Format.
     *                  1) Path and Name of a GZip File Stored on the machine running the code
     *                  2) Valid URL pointing to the GZIP file.
     * @return          The Reader represented by this String
     * @throws InvalidPluginArgumentException
     */
    public static Reader GZipToReader(String argument) throws InvalidPluginArgumentException {
        InputStream gzipStream = InputStreamArgumentDefinition.GZipToInputStream(argument);
        return new InputStreamReader(gzipStream);
    }

    /**
     * @param arguement The Reader as a String
     *                  Legal values for the String are:
     *                  File is assumed to be in Zip Format.
     *                  1) Path and Name of a Zip File Stored on the machine running the code
     *                  2) Valid URL pointing to the GZIP file.
     * @return          The Reader represented by this String
     * @throws InvalidPluginArgumentException
     */
    public static Reader ZipToReader(String argument) throws InvalidPluginArgumentException {
        InputStream zipStream = InputStreamArgumentDefinition.ZipToInputStream(argument);
        return new InputStreamReader(zipStream);
    }

    /**
     * @param arguement The Reader as a String
     *                  Legal values for the String are:
     *                  File is assumed not to be in GZip or Zip Format.
     *                  1) Path and Name of a Zip File Stored on the machine running the code
     *                  2) Valid URL pointing to the GZIP file.
     *                  3) Actaul String value. Avoid using this option if data is in a URL;
     * @return          The Reader represented by this String
     * @throws InvalidPluginArgumentException
     */
    public static Reader notZipToReader(String argument) throws InvalidPluginArgumentException {
        File file = new File(argument);
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new InvalidPluginArgumentException("Illegal attempt to convert a directory " + argument + " to Reader.");
            }
            try {
                return new FileReader(file);
            } catch (FileNotFoundException ex) {
                throw new InvalidPluginArgumentException("Exception attempting to convert existing file " + argument + " to a Reader." + ex);
            }
        } else {
            try {
                URL url = new URL(argument);
                InputStream urlStream = url.openStream();
                return new InputStreamReader(urlStream);
            } catch (Exception ex) {
                return new StringReader(argument);
            }
        }
    }

    /**
     * Parses argument object from String.
     * Uses the Static method StringToReader and its submethods.
     * @see StringToReader
     *
     * @param argument String
     * @return Reader
     * @throws InvalidPluginArgumentException
     * @throws IOException
     */
    @Override
    public Reader parseString(String argument) throws InvalidPluginArgumentException {
        return StringToReader(argument);
    }
}
