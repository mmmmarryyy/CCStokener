package net.sourceforge.ondex.args;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import net.sourceforge.ondex.InvalidPluginArgumentException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

/**
 * ArgumentDefinition for an input stream.
 * Can convert String into a valid inputStream
 * Can handle files and URLs, uncompressed as well as Zipped and GZipped.
 *
 * Should only be used for InputStreams that will NOT be wrapped with a Reader!
 * For Inputs that handle only text/xml use ReaderArgumentDefinition.
 *
 * @author hindlem, Christian Brenninkmeijer
 */
public class InputStreamArgumentDefinition extends AbstractArgumentDefinition<InputStream> {

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
    public InputStreamArgumentDefinition(String name, String description, String suffix, boolean required, boolean canHaveMultipleInstances) {
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
    public InputStreamArgumentDefinition(String name, String description, String suffix, boolean required) {
        this(name, description, suffix, required, false);
    }

    /**
     * Returns associated java class.
     *
     * @return Class
     */
    @Override
    public Class<InputStream> getClassType() {
        return InputStream.class;
    }

    /**
     * Returns default value.
     *
     * @return null
     */
    public InputStream getDefaultValue() {
        return null;
    }

    /**
     * Checks for valid argument.
     *
     * @return boolean
     */
    public void isValidArgument(Object obj) throws InvalidPluginArgumentException {
        if (obj instanceof String) {
            try {
                URL url = new URL((String) obj);
                return;
            } catch (MalformedURLException ex) {
            }
            File file = new File((String) obj);
            if (obj.toString().trim().length() == 0) {
                throw new InvalidPluginArgumentException("An empty argument is invalid for " + this.getName());
            }
            if (!file.exists()) {
                throw new InvalidPluginArgumentException("The file " + file + " does not exist and is required to do so for " + this.getName() + " ");
            }
            return;
        }
        if (obj instanceof InputStream) {
            return;
        }
        throw new InvalidPluginArgumentException("A " + getName() + " argument is required to be specified as a InputStream for " + this.getName() + " class " + obj.getClass().getName() + " was found ");
    }

    /**
     * Converts a String into an InputStream.
     *
     * @param argument The String to be converted into an InputStream.
     *                  Legal values for the String are:
     *                  1) Path and Name of a File Stored on the machine running the code
     *                      a) File that end with ".zip" will be assumed to be in ZIP Format.
     *                      b) File that end with ".gz" will be assumed to be in GZ Format.
     *                      c) All other files are assumed to be in unzipped format.
     *                  2) Valid URL pointing to the file.
     *                      (Same format assumptions as for file names)
     * @return          The Reader represented by this String
     */
    public static InputStream StringToInputStream(String argument) throws InvalidPluginArgumentException {
        if (argument == null) {
            throw new InvalidPluginArgumentException("Illegal Attempt to convert null to an InputStream.");
        }
        if (argument.isEmpty()) {
            throw new InvalidPluginArgumentException("Illegal Attempt to convert empty String to an InputStream.");
        }
        if (argument.toLowerCase().endsWith(".gz")) {
            return GZipToInputStream(argument);
        }
        if (argument.toLowerCase().endsWith(".zip")) {
            return ZipToInputStream(argument);
        }
        return StringToPureInputStream(argument);
    }

    /**
     * Converts the String representing some GZip format into an InputStream.
     *
     * @param argument The InputStream as a String
     *                  Legal values for the String are:
     *                  File is assumed to be in GZip Format.
     *                  1) Path and Name of a GZip File Stored on the machine running the code
     *                  2) Valid URL pointing to the GZIP file.
     * @return          The InputStream represented by this String
     * @throws InvalidPluginArgumentException
     */
    public static InputStream GZipToInputStream(String argument) throws InvalidPluginArgumentException {
        InputStream pureStream = StringToPureInputStream(argument);
        try {
            return new GZIPInputStream(pureStream);
        } catch (IOException ex) {
            throw new InvalidPluginArgumentException("Exception attempting to convert InputStream  " + argument + " to a GZIPInputStream." + ex);
        }
    }

    /**
     * @param argument The InputStream as a String
     *                  Legal values for the String are:
     *                  File is assumed to be in Zip Format.
     *                  1) Path and Name of a Zip File Stored on the machine running the code
     *                  2) Valid URL pointing to the GZIP file.
     * @return          The Reader represented by this String
     * @throws InvalidPluginArgumentException
     */
    public static InputStream ZipToInputStream(String argument) throws InvalidPluginArgumentException {
        InputStream pureStream = StringToPureInputStream(argument);
        return new ZipInputStream(pureStream);
    }

    /**
     * @param argument The Reader as an inputStream.
     *                  No Attempt is made to unzip the stream.
     *                  Legal values for the String are:
     *                  1) Path and Name of a Zip File Stored on the machine running the code
     *                  2) Valid URL pointing to the GZIP file.
     * @return          The InputStream represented by this String or null.
     * @throws InvalidPluginArgumentException
     */
    public static InputStream StringToPureInputStream(String argument) throws InvalidPluginArgumentException {
        File file = new File(argument);
        InputStream inputStream;
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new InvalidPluginArgumentException("Illegal attempt to convert a directory " + argument + " to an InputStream.");
            }
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException ex) {
                throw new InvalidPluginArgumentException("Exception attempting to convert existing file " + argument + " to an InputStream." + ex);
            }
        } else {
            try {
                URL url = new URL(argument);
                return url.openStream();
            } catch (Exception ex) {
                throw new InvalidPluginArgumentException("The arguement " + argument + " of type String, could not be converted " + "to either a URL or an existing file.");
            }
        }
    }

    /**
     * Parses argument object from String.
     * Uses the Static method StringToReader and its submethods.
     * @see StringToReader
     *
     * @param argument String
     * @return InputStream
     * @throws InvalidPluginArgumentException
     * @throws IOException
     */
    @Override
    public InputStream parseString(String argument) throws InvalidPluginArgumentException {
        return StringToInputStream(argument);
    }
}
