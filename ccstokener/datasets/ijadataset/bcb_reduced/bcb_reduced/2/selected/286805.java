package net.sourceforge.ondex.wsapi.plugins;

import com.ice.tar.TarEntry;
import com.ice.tar.TarOutputStream;
import net.sourceforge.ondex.ONDEXPlugin;
import net.sourceforge.ondex.ONDEXPluginArguments;
import net.sourceforge.ondex.InvalidPluginArgumentException;
import net.sourceforge.ondex.args.ArgumentDefinition;
import net.sourceforge.ondex.args.FileArgumentDefinition;
import net.sourceforge.ondex.core.ONDEXGraph;
import net.sourceforge.ondex.export.ONDEXExport;
import net.sourceforge.ondex.parser.ONDEXParser;
import net.sourceforge.ondex.workflow.engine.Engine;
import net.sourceforge.ondex.wsapi.exceptions.CaughtException;
import net.sourceforge.ondex.wsapi.exceptions.IllegalArguementsException;
import net.sourceforge.ondex.wsapi.exceptions.WebserviceException;
import net.sourceforge.ondex.wsapi.WebServiceEngine;
import org.apache.log4j.Logger;
import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Webservice wrapper of the Mapping Plugins.
 *
 * @author Christian Brenninkmeijer
 */
public class PluginWS {

    private static final String ONDEX_LOCATION = ":8080/ondex/";

    private static final Logger logger = Logger.getLogger(PluginWS.class);

    public static String NEW_LINE = System.getProperty("line.separator");

    protected static WebServiceEngine webServiceEngine = WebServiceEngine.getWebServiceEngine();

    protected static Engine engine = Engine.getEngine();

    protected static File tempDir;

    protected PluginWS() throws CaughtException {
        try {
            if (tempDir == null) {
                tempDir = new File(System.getProperty("webapp.root") + File.separator + "temp");
                if (!tempDir.exists()) {
                    tempDir.mkdir();
                }
            }
        } catch (Exception e) {
            throw new CaughtException("Creating main temp directory", e, logger);
        }
    }

    public static void addDefaultArgements(ONDEXPlugin plugin, ONDEXPluginArguments arguments) throws IllegalArguementsException {
        ArgumentDefinition<?>[] argumentDefinitions = plugin.getArgumentDefinitions();
        try {
            for (ArgumentDefinition<?> aDefinition : argumentDefinitions) {
                logger.info(aDefinition.getName());
                if (arguments.getObjectValueList(aDefinition.getName()).size() > 0) {
                    logger.info("exists");
                } else if (aDefinition.getDefaultValue() != null) {
                    logger.info("No value provided for " + aDefinition.getName() + " using default value of: " + aDefinition.getDefaultValue());
                    createArguement(plugin, arguments, aDefinition.getName(), aDefinition.getDefaultValue());
                } else if (aDefinition.isRequiredArgument()) {
                    throw new IllegalArguementsException("No value found and no default available" + " for arguement with the name: " + aDefinition.getName() + " in the plugin " + plugin.getClass(), plugin, logger);
                } else {
                    logger.info("Not required and no default.");
                }
            }
        } catch (InvalidPluginArgumentException e) {
            throw new IllegalArguementsException("Unexpected mismatch between annouced deafults " + "and excepted aurguements", plugin, logger);
        }
    }

    protected static void checkArguementName(ONDEXPlugin plugin, String arguementName) throws IllegalArguementsException {
        ONDEXPluginArguments arguments = plugin.getArguments();
        ArgumentDefinition[] argumentDefinitions = plugin.getArgumentDefinitions();
        for (ArgumentDefinition argumentDefinition : argumentDefinitions) {
            if (argumentDefinition.getName().equals(arguementName)) {
                return;
            }
        }
        throw new IllegalArguementsException("Plugin has been changed!  " + "Arguement with the name: " + arguementName + " no longer in the plugin " + plugin.getClass(), plugin, logger);
    }

    protected static void createArguement(ONDEXPlugin plugin, ONDEXPluginArguments arguments, String arguementName, Object arguementValue) throws IllegalArguementsException {
        checkArguementName(plugin, arguementName);
        if (arguementValue == null) {
            logger.info("Ignoring null value for arguement " + arguementName);
            return;
        }
        if (arguementValue instanceof String) {
            String asString = (String) arguementValue;
            if (asString.length() == 0) {
                logger.info("Ignoring empty string for arguement " + arguementName);
                return;
            }
        }
        try {
            arguments.addOption(arguementName, arguementValue);
        } catch (Exception e) {
            throw new IllegalArguementsException("Unable to add value " + arguementValue + " for arguement with the name: " + arguementName + " in the plugin " + plugin.getClass() + " exception thrown was ", e, plugin, logger);
        }
    }

    protected void createArguement(ONDEXPlugin plugin, ONDEXPluginArguments arguments, String arguementName, Object[] arguementValues) throws IllegalArguementsException {
        checkArguementName(plugin, arguementName);
        if (arguementValues == null) {
            logger.info("Ignoring null value for arguement " + arguementName);
            return;
        }
        if (arguementValues.length == 0) {
            logger.info("Ignoring empty array for arguement " + arguementName);
            return;
        }
        try {
            arguments.addOptions(arguementName, arguementValues);
        } catch (Exception e) {
            throw new IllegalArguementsException("Unable to add values " + arguementValues + " for arguement with the name: " + arguementName + " in the plugin " + plugin.getClass() + " exception thrown was ", e, plugin, logger);
        }
    }

    protected void addExportFile(ONDEXPlugin plugin, ONDEXPluginArguments arguments, File file) throws IllegalArguementsException {
        checkArguementName(plugin, FileArgumentDefinition.EXPORT_FILE);
        try {
            arguments.addOption(FileArgumentDefinition.EXPORT_FILE, file.getAbsolutePath());
            logger.info("added export file " + file.getAbsolutePath());
        } catch (InvalidPluginArgumentException e) {
            new IllegalArguementsException("Error adding the export file ", e, plugin, logger);
        }
    }

    protected void addExportFile(ONDEXPlugin plugin, String aurguementName, ONDEXPluginArguments arguments, File file) throws IllegalArguementsException {
        checkArguementName(plugin, aurguementName);
        try {
            arguments.addOption(aurguementName, file.getAbsolutePath());
            logger.info("added export file " + file.getAbsolutePath());
        } catch (InvalidPluginArgumentException e) {
            new IllegalArguementsException("Error adding the export file to aurgument " + aurguementName, e, plugin, logger);
        }
    }

    private static final int BUFFER = 2048;

    private static void checkExists(File dir) {
        logger.info("checking: " + dir);
        if (dir.exists()) {
            logger.info("exists");
            return;
        }
        checkExists(dir.getParentFile());
        dir.mkdir();
        logger.info("made directory " + dir);
    }

    private static void copyFile(File output, InputStream input) throws CaughtException {
        try {
            logger.info("Coping file " + input);
            BufferedInputStream is = new BufferedInputStream(input);
            logger.info("Using " + is);
            logger.info("going To " + output);
            checkExists(output.getParentFile());
            FileOutputStream fos = new FileOutputStream(output);
            logger.info("To " + fos);
            BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
            logger.info("reading to copy to " + dest);
            int count;
            byte data[] = new byte[BUFFER];
            while ((count = is.read(data, 0, BUFFER)) != -1) {
                dest.write(data, 0, count);
            }
            is.close();
            dest.flush();
            dest.close();
        } catch (IOException e) {
            throw new CaughtException(e, logger);
        }
    }

    private static File GzipToFile(byte[] gzippedXml, String suffix) throws CaughtException {
        return GzipToFile(tempDir, gzippedXml, suffix);
    }

    private static File GzipToFile(File dir, byte[] gzippedXml, String suffix) throws CaughtException {
        try {
            File tempFile = File.createTempFile("input", suffix, dir);
            InputStream zipped = new ByteArrayInputStream(gzippedXml);
            InputStream unzipped = new GZIPInputStream(zipped);
            copyFile(tempFile, unzipped);
            return tempFile;
        } catch (IOException e) {
            throw new CaughtException(e, logger);
        }
    }

    protected File createOutputFile(String prefix, String suffix) throws CaughtException {
        try {
            String webapp = System.getProperty("webapp.root");
            File dir = new File(webapp + File.separator + "output");
            checkExists(dir);
            return File.createTempFile(prefix, suffix, dir);
        } catch (IOException e) {
            throw new CaughtException("Trying to create a temporary file", e, logger);
        }
    }

    /**
     * Converts a String into an InputStream.
     *
     * @param argument The String to be converted into an InputStream.
     *                  Legal values for the String are:
     *                  1) Path and Name of a File Stored on the machine running the code
     *                      a) File that end with ".zip" will be assumed to be in ZIP Format.
     *                      b) File that end with ".gz" or ".oxl" will be assumed to be in GZ Format.
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
        if (argument.toLowerCase().endsWith(".gz") || argument.toLowerCase().endsWith(".oxl")) {
            return GZipToInputStream(argument);
        }
        if (argument.toLowerCase().endsWith(".zip")) {
            return ZipToInputStream(argument);
        }
        return StringToPureInputStream(argument);
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

    protected static void createInputFileArguement(ONDEXPlugin plugin, ONDEXPluginArguments arguments, String arguementName, String asString, byte[] asArray) throws CaughtException {
        try {
            File inputFile;
            if (asString == null || asString.isEmpty()) {
                inputFile = GzipToFile(asArray, ".xml");
            } else {
                inputFile = new File(asString);
                if (!inputFile.exists()) {
                    inputFile = File.createTempFile("input", ".xml", tempDir);
                    InputStream inputStream = StringToInputStream(asString);
                    copyFile(inputFile, inputStream);
                }
            }
            createArguement(plugin, arguments, arguementName, inputFile.getAbsolutePath());
        } catch (Exception e) {
            throw new CaughtException("Trying to create a InputFile for " + arguementName, e, logger);
        }
    }

    protected void createOptionalInputFileArguement(ONDEXPlugin plugin, ONDEXPluginArguments arguments, String arguementName, String asString, byte[] asArray) throws CaughtException {
        if (asString == null || asString.isEmpty()) {
            if (asArray == null || asArray.length == 0) {
                logger.info("No Value found for " + arguementName);
                return;
            }
        }
        createInputFileArguement(plugin, arguments, arguementName, asString, asArray);
    }

    private static String findCompressed(File file) throws UnknownHostException {
        File check = new File(file.getAbsolutePath() + ".gz");
        if (check.exists()) {
            return ("GZIP File can be found at the url: " + fileToUrl(check));
        }
        check = new File(file.getAbsolutePath() + ".oxl");
        if (check.exists()) {
            return ("OXL File (probably GZIP format) can be found at the url: " + fileToUrl(check));
        }
        check = new File(file.getAbsolutePath() + ".zip");
        if (check.exists()) {
            return ("ZIP File can be found at the url: " + fileToUrl(check));
        }
        check = new File(file.getAbsolutePath() + ".tar.gz");
        if (check.exists()) {
            return ("Tar.gz File can be found at the url: " + fileToUrl(check));
        }
        check = new File(file.getAbsolutePath() + ".tar");
        if (check.exists()) {
            return ("TAR File can be found at the url: " + fileToUrl(check));
        }
        if (file.exists()) {
            return ("Sorry result file is empty and no crompressed version found.");
        } else {
            return ("Sorry neither result file nor crompressed version found.");
        }
    }

    public static String fileToUrl(File file) throws UnknownHostException {
        String webapp = System.getProperty("webapp.root");
        String fullPath = file.getAbsolutePath();
        if (fullPath.toLowerCase().startsWith(webapp.toLowerCase())) {
            String path = fullPath.substring(webapp.length());
            path = path.replaceAll("\\\\", "/");
            return InetAddress.getLocalHost().getCanonicalHostName() + ONDEX_LOCATION + path;
        }
        throw new UnknownHostException(" Unable to convert file " + file.getName() + "to URL");
    }

    public static String fileToString(File file) throws FileNotFoundException, IOException, UnknownHostException {
        return fileToString(file, 25000);
    }

    public static String fileToString(File file, int maxSize) throws FileNotFoundException, IOException, UnknownHostException {
        logger.info("converting " + file.getAbsolutePath());
        if (!file.exists() || file.length() == 0) {
            return findCompressed(file);
        }
        if (file.isDirectory()) {
            File zipped = zip(file);
            return ("ZIPPED directory can be found at the url: " + fileToUrl(zipped));
        }
        if (file.length() > maxSize) {
            return ("As file is very large use the url: " + fileToUrl(file));
        }
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuffer results = new StringBuffer();
        String line = null;
        while ((line = reader.readLine()) != null) {
            results.append(line);
            results.append(NEW_LINE);
        }
        reader.close();
        return results.toString();
    }

    private static final void toZip(ZipOutputStream out, File file, String path) throws IOException {
        if (file.isFile()) {
            ZipEntry entry = new ZipEntry(path + file.getName());
            out.putNextEntry(entry);
            BufferedInputStream origin = null;
            byte data[] = new byte[BUFFER];
            FileInputStream fi = new FileInputStream(file);
            origin = new BufferedInputStream(fi, BUFFER);
            int count;
            while ((count = origin.read(data, 0, BUFFER)) != -1) {
                out.write(data, 0, count);
            }
            origin.close();
        } else {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                toZip(out, files[i], path + file.getName() + "/");
            }
        }
    }

    private final void toTar(TarOutputStream out, File file, String path) throws IOException {
        if (file.isFile()) {
            TarEntry entry = new TarEntry(path + file.getName());
            if (file.length() > 0) {
                entry.setSize(file.length());
            }
            out.putNextEntry(entry);
            BufferedInputStream origin = null;
            byte data[] = new byte[BUFFER];
            FileInputStream fi = new FileInputStream(file);
            origin = new BufferedInputStream(fi, BUFFER);
            int count;
            while ((count = origin.read(data, 0, BUFFER)) != -1) {
                out.write(data, 0, count);
            }
            origin.close();
        } else {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                toTar(out, files[i], path + file.getName() + "/");
            }
        }
    }

    private static File zip(File file) throws FileNotFoundException, IOException {
        File destFile = new File(file.getAbsolutePath() + ".zip");
        FileOutputStream dest = new FileOutputStream(destFile);
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
        toZip(out, file, "");
        out.close();
        return destFile;
    }

    private File tar(File file) throws FileNotFoundException, IOException {
        File destFile = new File(file.getAbsolutePath() + ".tar");
        FileOutputStream dest = new FileOutputStream(destFile);
        TarOutputStream out = new TarOutputStream(new BufferedOutputStream(dest));
        toTar(out, file, "");
        out.close();
        return destFile;
    }

    private File tarGzip(File file) throws FileNotFoundException, IOException {
        File destFile = new File(file.getAbsolutePath() + ".tar.gz");
        FileOutputStream dest = new FileOutputStream(destFile);
        GZIPOutputStream gzipStream = new GZIPOutputStream(dest);
        TarOutputStream out = new TarOutputStream(new BufferedOutputStream(gzipStream));
        toTar(out, file, "");
        out.close();
        return destFile;
    }

    private File gzip(File file) throws FileNotFoundException, IOException {
        if (file.isDirectory()) {
            throw new IOException("Unable to gzip a directory");
        }
        File destFile = new File(file.getAbsolutePath() + ".gz");
        FileOutputStream dest = new FileOutputStream(destFile);
        GZIPOutputStream out = new GZIPOutputStream(dest);
        byte data[] = new byte[BUFFER];
        FileInputStream fi = new FileInputStream(destFile);
        BufferedInputStream origin = new BufferedInputStream(fi, BUFFER);
        int count;
        while ((count = origin.read(data, 0, BUFFER)) != -1) {
            out.write(data, 0, count);
        }
        origin.close();
        out.close();
        return destFile;
    }

    protected File zipFile(File file, ZipFormat zipFormat) throws IOException {
        switch(zipFormat) {
            case ZIP:
                return zip(file);
            case TAR:
                return tar(file);
            case TAR_GZIP:
                return tarGzip(file);
            case GZIP:
                return gzip(file);
            case URL:
            case RAW:
                if (file.isFile()) {
                    return file;
                } else {
                    return zip(file);
                }
        }
        throw new IOException("Unexpected zip format " + zipFormat);
    }

    public static String initMetaData(ONDEXGraph graph) throws CaughtException {
        try {
            logger.info("initMetaData called");
            net.sourceforge.ondex.parser.oxl.Parser parser = new net.sourceforge.ondex.parser.oxl.Parser();
            String md = System.getProperty("webapp.root") + File.separator + "WEB-INF" + File.separator + "classes" + File.separator + "data" + File.separator + "xml" + File.separator + "ondex_metadata.xml";
            ONDEXPluginArguments arguments = new ONDEXPluginArguments(parser.getArgumentDefinitions());
            createArguement(parser, arguments, "InputFile", md);
            return runOXLParser(graph, parser, arguments);
        } catch (Exception e) {
            throw new CaughtException(e, logger);
        }
    }

    protected String runExport(ONDEXGraph graph, File file, ONDEXExport export, ONDEXPluginArguments args, ZipFormat zipFormat) throws WebserviceException {
        BufferedOndexListener bufferedOndexListener = new BufferedOndexListener(logger);
        try {
            export.addONDEXListener(bufferedOndexListener);
            logger.info("added listener");
            export.setONDEXGraph(graph);
            logger.info("set graph");
            addDefaultArgements(export, args);
            logger.info("added default arguements");
            engine.runExport(export, args, graph);
            logger.info("ran export");
            logger.info("done");
            if (zipFormat.equals(zipFormat.RAW)) {
                return fileToString(file);
            } else {
                File compressed = zipFile(file, zipFormat);
                return fileToUrl(compressed);
            }
        } catch (WebserviceException e) {
            throw e;
        } catch (Exception e) {
            throw new CaughtException(e, bufferedOndexListener, logger);
        }
    }

    protected static String runOXLParser(ONDEXGraph graph, ONDEXParser parser, ONDEXPluginArguments args) throws WebserviceException {
        BufferedOndexListener bufferedOndexListener = new BufferedOndexListener(logger);
        try {
            parser.addONDEXListener(bufferedOndexListener);
            logger.info("added listener");
            parser.setONDEXGraph(graph);
            logger.info("set graph");
            addDefaultArgements(parser, args);
            logger.info("added default arguements");
            logger.info("running parser");
            engine.runParser(parser, args, graph);
            webServiceEngine.commit(graph.getSID());
            logger.info("ran oxl parser");
            return bufferedOndexListener.getCompleteEventHistory();
        } catch (WebserviceException e) {
            throw e;
        } catch (Exception e) {
            throw new CaughtException(e, bufferedOndexListener, logger);
        }
    }

    protected String runParser(ONDEXGraph graph, ONDEXParser parser, ONDEXPluginArguments args) throws WebserviceException {
        BufferedOndexListener bufferedOndexListener = new BufferedOndexListener(logger);
        try {
            parser.addONDEXListener(bufferedOndexListener);
            logger.info("added listener");
            parser.setONDEXGraph(graph);
            logger.info("set graph");
            addDefaultArgements(parser, args);
            logger.info("added default arguements");
            logger.info("running parser");
            initMetaData(graph);
            engine.runParser(parser, args, graph);
            webServiceEngine.commit(graph.getSID());
            logger.info("ran parser");
            logger.info("done");
            return bufferedOndexListener.getCompleteEventHistory();
        } catch (WebserviceException e) {
            throw e;
        } catch (Exception e) {
            throw new CaughtException(e, bufferedOndexListener, logger);
        }
    }

    protected String runParser(ONDEXGraph graph, File file, ONDEXParser parser, ONDEXPluginArguments args, ZipFormat zipFormat) throws WebserviceException {
        BufferedOndexListener bufferedOndexListener = new BufferedOndexListener(logger);
        try {
            parser.addONDEXListener(bufferedOndexListener);
            logger.info("added listener");
            parser.setONDEXGraph(graph);
            logger.info("set graph");
            addDefaultArgements(parser, args);
            logger.info("added default arguements");
            logger.info("running parser");
            engine.runParser(parser, args, graph);
            webServiceEngine.commit(graph.getSID());
            logger.info("ran parser");
            logger.info("done");
            if (zipFormat.equals(zipFormat.RAW)) {
                return fileToString(file);
            } else {
                File compressed = zipFile(file, zipFormat);
                return fileToUrl(compressed);
            }
        } catch (WebserviceException e) {
            throw e;
        } catch (Exception e) {
            throw new CaughtException(e, bufferedOndexListener, logger);
        }
    }
}
