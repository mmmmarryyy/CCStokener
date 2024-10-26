package net.sourceforge.ondex.webservice2.plugins;

import net.sourceforge.ondex.AbstractArguments;
import net.sourceforge.ondex.AbstractONDEXPlugin;
import net.sourceforge.ondex.InvalidPluginArgumentException;
import net.sourceforge.ondex.args.ArgumentDefinition;
import net.sourceforge.ondex.args.FileArgumentDefinition;
import net.sourceforge.ondex.config.Config;
import net.sourceforge.ondex.event.ONDEXEvent;
import net.sourceforge.ondex.event.ONDEXListener;
import net.sourceforge.ondex.event.type.EventType;
import net.sourceforge.ondex.webservice2.Exceptions.CaughtException;
import net.sourceforge.ondex.webservice2.Exceptions.IllegalArguementsException;
import net.sourceforge.ondex.webservice2.Exceptions.PluginNotFoundException;
import net.sourceforge.ondex.webservice2.WebServiceEngine;
import net.sourceforge.ondex.workflow.engine.Engine;
import org.apache.log4j.Logger;
import java.io.*;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.sourceforge.ondex.args.InputStreamArgumentDefinition;
import net.sourceforge.ondex.args.ReaderArgumentDefinition;
import net.sourceforge.ondex.webservice2.Exceptions.WebserviceException;

/**
 * Webservice wrapper of the Mapping Plugins.
 *
 * @author Christian Brenninkmeijer
 */
public class PluginWS implements ONDEXListener {

    private static final Logger logger = Logger.getLogger(PluginWS.class);

    public static String NEW_LINE = System.getProperty("line.separator");

    protected static WebServiceEngine webServiceEngine = WebServiceEngine.getWebServiceEngine();

    protected static Engine engine = Engine.getEngine();

    private static File tempDir;

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

    protected String getInfo(AbstractONDEXPlugin plugin) throws PluginNotFoundException, CaughtException {
        logger.info("get infomapping name called " + plugin.getId());
        StringBuffer info = new StringBuffer();
        info.append(plugin.getId());
        info.append(": ");
        info.append(plugin.getName());
        info.append(NEW_LINE);
        ArgumentDefinition<AbstractArguments>[] argumentDefinitions = (ArgumentDefinition<AbstractArguments>[]) plugin.getArgumentDefinitions();
        for (ArgumentDefinition argumentDefinition : argumentDefinitions) {
            info.append(argumentDefinition.getName());
            info.append(", ");
            info.append(argumentDefinition.getClassType());
            info.append(", ");
            info.append(argumentDefinition.getDefaultValue());
            if (argumentDefinition.isRequiredArgument()) {
                info.append(", Required");
            } else {
                info.append(", Optional");
            }
            if (argumentDefinition.isAllowedMultipleInstances()) {
                info.append(", Multiple");
            } else {
                info.append(", Single");
            }
            info.append(", ");
            info.append(argumentDefinition.getDescription());
            info.append(NEW_LINE);
        }
        return info.toString();
    }

    public static void addDefaultArgements(AbstractONDEXPlugin plugin, AbstractArguments arguments) throws IllegalArguementsException {
        ArgumentDefinition<AbstractArguments>[] argumentDefinitions = (ArgumentDefinition<AbstractArguments>[]) plugin.getArgumentDefinitions();
        try {
            for (int j = 0; j < argumentDefinitions.length; j++) {
                ArgumentDefinition<AbstractArguments> aDefinition = argumentDefinitions[j];
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

    /**
     * Check if there are arguements to parse.
     * <p/>
     * Also validates the arguements names and values are the same size.
     *
     * @param arguementNames
     * @param arguementValues
     * @throws IllegalArguementsException
     */
    protected boolean emptyArguements(AbstractONDEXPlugin plugin, List<String> arguementNames, List<String> arguementValues) throws IllegalArguementsException {
        if (arguementNames == null) {
            if (arguementValues == null) {
                logger.info("Using defaults as both names and values are null.");
                return false;
            } else if (arguementValues.size() == 0) {
                logger.info("Using defaults as both names is null and values empty.");
                return false;
            } else {
                throw new IllegalArguementsException("Found no arguementNames but " + arguementValues.size() + " arguementValues", plugin, logger);
            }
        }
        if (arguementValues == null) {
            if (arguementNames.size() == 0) {
                logger.info("Using defaults as both names is empty and values null");
                return false;
            } else {
                throw new IllegalArguementsException("Found no arguementValues but " + arguementNames.size() + " arguementNames", plugin, logger);
            }
        }
        if (arguementNames.size() != arguementValues.size()) {
            throw new IllegalArguementsException("Found " + arguementNames.size() + "arguementNames but only " + arguementValues.size() + " arguementValues", plugin, logger);
        }
        return true;
    }

    public static void createArguement(AbstractONDEXPlugin plugin, AbstractArguments arguments, String arguementName, Object arguementValue) throws IllegalArguementsException {
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

    protected void createArguement(AbstractONDEXPlugin plugin, AbstractArguments arguments, String arguementName, Object[] arguementValues) throws IllegalArguementsException {
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

    protected void addInputFile(AbstractONDEXPlugin plugin, AbstractArguments arguments, File file) throws IllegalArguementsException {
        try {
            arguments.addOption(FileArgumentDefinition.INPUT_FILE, file.getAbsolutePath());
        } catch (InvalidPluginArgumentException e) {
            new IllegalArguementsException("Error adding the input file " + e, plugin, logger);
        }
    }

    protected void addInputDir(AbstractONDEXPlugin plugin, AbstractArguments arguments, File dir) throws IllegalArguementsException {
        try {
            arguments.addOption(FileArgumentDefinition.INPUT_DIR, dir.getAbsolutePath());
        } catch (InvalidPluginArgumentException e) {
            new IllegalArguementsException("Error adding the input file " + e, plugin, logger);
        }
    }

    protected void addInputDir(AbstractONDEXPlugin plugin, AbstractArguments arguments, File... files) throws IllegalArguementsException, CaughtException {
        try {
            File dir = createDir(files);
            arguments.addOption(FileArgumentDefinition.INPUT_DIR, dir.getAbsolutePath());
        } catch (InvalidPluginArgumentException e) {
            new IllegalArguementsException("Error adding the input file ", e, plugin, logger);
        }
    }

    protected void addExportFile(AbstractONDEXPlugin plugin, AbstractArguments arguments, File file) throws IllegalArguementsException {
        try {
            arguments.addOption(FileArgumentDefinition.EXPORT_FILE, file.getAbsolutePath());
            logger.info("added export file");
        } catch (InvalidPluginArgumentException e) {
            new IllegalArguementsException("Error adding the export file ", e, plugin, logger);
        }
    }

    protected void addExportDir(AbstractONDEXPlugin plugin, AbstractArguments arguments, File file) throws IllegalArguementsException {
        try {
            arguments.addOption(FileArgumentDefinition.EXPORT_DIR, file.getAbsolutePath());
        } catch (InvalidPluginArgumentException e) {
            new IllegalArguementsException("Error adding the export dir ", e, plugin, logger);
        }
    }

    @Override
    public void eventOccurred(ONDEXEvent e) {
        EventType eventType = e.getEventType();
        logger.info("event: ");
        logger.info(eventType.getCompleteMessage());
    }

    static final int BUFFER = 2048;

    public static File createTempFile(String prefix, String suffix) throws IOException {
        return File.createTempFile(prefix, suffix, tempDir);
    }

    protected File StringToFile(String input, String suffix) throws CaughtException {
        return StringToFile(tempDir, input, suffix);
    }

    protected File StringToFile(File dir, String input, String suffix) throws CaughtException {
        try {
            File tempFile = File.createTempFile("input", suffix, dir);
            Writer writer = new FileWriter(tempFile);
            writer.write(input);
            writer.flush();
            writer.close();
            return tempFile;
        } catch (IOException e) {
            throw new CaughtException(e, logger);
        }
    }

    protected File StringToAFile(File dir, String input, String name) throws CaughtException {
        try {
            File tempFile = new File(dir, name);
            Writer writer = new FileWriter(tempFile);
            writer.write(input);
            writer.flush();
            writer.close();
            return tempFile;
        } catch (IOException e) {
            throw new CaughtException(e, logger);
        }
    }

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

    private static void copyFile(File output, File input) throws CaughtException {
        try {
            if (input.isDirectory()) {
                logger.info("Copying dir: " + input);
                File newDir = new File(output, input.getName());
                if (!newDir.exists()) {
                    newDir.mkdir();
                }
                logger.info("To dir: " + newDir);
                for (File file : input.listFiles()) {
                    copyFile(newDir, file);
                }
            } else {
                InputStream stream = new FileInputStream(input);
                copyFile(output, stream);
            }
        } catch (IOException e) {
            throw new CaughtException(e, logger);
        }
    }

    protected File GzipToFile(byte[] gzippedXml, String suffix) throws CaughtException {
        return GzipToFile(tempDir, gzippedXml, suffix);
    }

    protected File GzipToFile(File dir, byte[] gzippedXml, String suffix) throws CaughtException {
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

    protected File GzipToAFile(File dir, byte[] gzippedXml, String fileName) throws CaughtException {
        try {
            File tempFile = new File(dir, fileName);
            InputStream zipped = new ByteArrayInputStream(gzippedXml);
            InputStream unzipped = new GZIPInputStream(zipped);
            copyFile(tempFile, unzipped);
            return tempFile;
        } catch (IOException e) {
            throw new CaughtException(e, logger);
        }
    }

    private static String fileName(URL url) {
        File temp = new File(url.getFile());
        return temp.getName();
    }

    protected static File UrlToFile(String urlSt) throws CaughtException {
        try {
            logger.info("copy from url: " + urlSt);
            URL url = new URL(urlSt);
            InputStream input = url.openStream();
            File dir = tempDir;
            File tempFile = new File(tempDir + File.separator + fileName(url));
            logger.info("created: " + tempFile.getAbsolutePath());
            copyFile(tempFile, input);
            return tempFile;
        } catch (IOException e) {
            throw new CaughtException(e, logger);
        }
    }

    protected static File UrlToFile(File target, String urlSt) throws CaughtException {
        try {
            return UrlToAFile(target, urlSt, fileName(new URL(urlSt)));
        } catch (MalformedURLException e) {
            throw new CaughtException(e, logger);
        }
    }

    protected static File UrlToAFile(File target, String urlSt, String fileName) throws CaughtException {
        try {
            logger.info("copy from url: " + urlSt);
            URL url = new URL(urlSt);
            InputStream input = url.openStream();
            File dir = tempDir;
            File tempFile = new File(target, fileName);
            logger.info("created: " + tempFile.getAbsolutePath());
            copyFile(tempFile, input);
            return tempFile;
        } catch (IOException e) {
            throw new CaughtException(e, logger);
        }
    }

    protected static File copyFile(File target, String fileName) throws CaughtException {
        try {
            logger.info("copy from file: " + fileName);
            File file = new File(fileName);
            InputStream input = new FileInputStream(file);
            logger.info("Stream open");
            File tempFile = new File(target, file.getName());
            logger.info("created: " + tempFile.getAbsolutePath());
            copyFile(tempFile, input);
            return tempFile;
        } catch (IOException e) {
            throw new CaughtException(e, logger);
        }
    }

    protected File UriToFile(String uriSt) throws CaughtException {
        try {
            logger.info("copyiong uri " + uriSt);
            URI uri = new URI(uriSt);
            File uriFile = new File(uri);
            File target = createDir();
            copyFile(target, uriFile);
            return target;
        } catch (CaughtException e) {
            throw e;
        } catch (Exception e) {
            throw new CaughtException(e, logger);
        }
    }

    protected static File UrlGzipToFile(String urlSt, String suffix) throws CaughtException {
        return UrlGzipToFile(tempDir, urlSt, suffix);
    }

    protected static File UrlGzipToFile(File dir, String urlSt, String suffix) throws CaughtException {
        try {
            URL url = new URL(urlSt);
            InputStream zipped = url.openStream();
            InputStream unzipped = new GZIPInputStream(zipped);
            File tempFile = File.createTempFile("input", suffix, dir);
            copyFile(tempFile, unzipped);
            return tempFile;
        } catch (IOException e) {
            throw new CaughtException(e, logger);
        }
    }

    protected static File UrlGzipToAFile(File dir, String urlSt, String fileName) throws CaughtException {
        try {
            URL url = new URL(urlSt);
            InputStream zipped = url.openStream();
            InputStream unzipped = new GZIPInputStream(zipped);
            File tempFile = new File(dir, fileName);
            copyFile(tempFile, unzipped);
            return tempFile;
        } catch (IOException e) {
            throw new CaughtException(e, logger);
        }
    }

    private static void checkDir(File dir) throws IOException {
        System.out.println("checking " + dir);
        if (dir == null) {
            throw new IOException("Unable to create null dir");
        }
        if (dir.exists()) {
            System.out.println(dir + " exists");
            return;
        }
        try {
            checkDir(dir.getParentFile());
        } catch (IOException e) {
            throw new IOException("Unable to create dir " + dir);
        }
        dir.mkdir();
        System.out.println("made " + dir);
    }

    public static void unzip(String directoryName, ZipFile zipfile) throws CaughtException {
        try {
            Enumeration e = zipfile.entries();
            while (e.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                if (entry.isDirectory()) {
                    logger.info("Ignoring " + entry);
                } else {
                    logger.info("Extracting: " + entry);
                    InputStream is = zipfile.getInputStream(entry);
                    File temp = new File(directoryName + File.separator + entry.getName());
                    copyFile(temp, is);
                }
            }
        } catch (IOException e) {
            throw new CaughtException(e, logger);
        }
    }

    public static void unzip(String directoryName, String zipName) throws CaughtException {
        try {
            ZipFile zipfile = new ZipFile(zipName);
            unzip(directoryName, zipfile);
        } catch (IOException e) {
            throw new CaughtException(e, logger);
        }
    }

    protected File UriZipToFile(String zipURI) throws CaughtException {
        logger.info("unzipping " + zipURI);
        try {
            URI uri = new URI(zipURI);
            File uriFile = new File(uri);
            ZipFile zipfile = new ZipFile(uriFile);
            File outputDir = createDir();
            unzip(outputDir.getAbsolutePath(), zipfile);
            return outputDir;
        } catch (CaughtException e) {
            throw e;
        } catch (Exception e) {
            throw new CaughtException(e, logger);
        }
    }

    protected File createDir() throws CaughtException {
        try {
            Date now = new Date();
            File newDir = new File(tempDir + File.separator + "tempDir" + now.getTime());
            newDir.mkdir();
            return newDir;
        } catch (Exception e) {
            throw new CaughtException("Trying to create a temporary directory", e, logger);
        }
    }

    public static boolean deleteFile(File file) {
        boolean ok = true;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File inner : files) {
                ok = ok && deleteFile(inner);
            }
        }
        ok = ok && file.delete();
        return ok;
    }

    public static boolean deleteAllTemporaryFiles() {
        boolean success = deleteFile(tempDir);
        if (!tempDir.exists()) {
            tempDir.mkdir();
        }
        return success;
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

    protected File createDir(File... files) throws CaughtException {
        try {
            Date now = new Date();
            File newDir = new File(tempDir + File.separator + "tempDir" + now.getTime());
            newDir.mkdir();
            for (File file : files) {
                File temp = new File(newDir.getAbsolutePath() + File.separator + file.getName());
                if (!file.renameTo(temp)) {
                    copyFile(temp, file);
                    file.delete();
                }
            }
            return newDir;
        } catch (Exception e) {
            throw new CaughtException("Trying to create a temporary directory", e, logger);
        }
    }

    protected void getProrties() {
        Properties properties = System.getProperties();
        Enumeration<?> keys = properties.propertyNames();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            logger.info(key + "=" + System.getProperty(key.toString()));
        }
    }

    protected void createInputFileArguement(AbstractONDEXPlugin plugin, AbstractArguments arguments, String arguementName, String asString, byte[] asArray) throws CaughtException {
        try {
            File inputFile;
            if (asString == null || asString.isEmpty()) {
                inputFile = GzipToFile(asArray, ".xml");
            } else {
                if (asString.toLowerCase().endsWith(".gz")) {
                    inputFile = File.createTempFile("input", ".xml", this.tempDir);
                    InputStream inputStream = InputStreamArgumentDefinition.GZipToInputStream(asString);
                    copyFile(inputFile, inputStream);
                } else if (asString.toLowerCase().endsWith(".zip")) {
                    inputFile = File.createTempFile("input", ".xml", this.tempDir);
                    InputStream inputStream = InputStreamArgumentDefinition.ZipToInputStream(asString);
                    copyFile(inputFile, inputStream);
                } else {
                    File test = new File(asString);
                    if (test.exists()) {
                        inputFile = test;
                    } else {
                        inputFile = StringToFile(asString, ".xml");
                    }
                }
            }
            createArguement(plugin, arguments, arguementName, inputFile.getAbsolutePath());
        } catch (Exception e) {
            throw new CaughtException("Trying to create a InputFile", e, logger);
        }
    }

    protected void createInputDirectoryArguement(AbstractONDEXPlugin plugin, AbstractArguments arguments, String arguementName, String asString, byte[] asArray) throws CaughtException {
        try {
            if (asString == null || asString.isEmpty()) {
                throw new WebserviceException("Converting array of Bytes representing a Directory not yet supported", logger);
            } else {
                File inputDirectory = new File(asString);
                if (!inputDirectory.exists()) {
                    throw new WebserviceException("Unable to find local (to server) directory " + inputDirectory, logger);
                }
                if (!inputDirectory.isDirectory()) {
                    throw new WebserviceException("Local (to server) file " + inputDirectory + " is not a directory.", logger);
                }
                createArguement(plugin, arguments, arguementName, inputDirectory.getAbsolutePath());
            }
        } catch (Exception e) {
            throw new CaughtException("Trying to create a InputFile", e, logger);
        }
    }

    protected void createReaderArguement(AbstractONDEXPlugin plugin, AbstractArguments arguments, String arguementName, String asString, byte[] asArray) throws CaughtException {
        try {
            Reader reader;
            if (asString == null || asString.isEmpty()) {
                if (asArray.length == 0) {
                    return;
                }
                logger.info("Reader is a GZIP reader");
                InputStream zipped = new ByteArrayInputStream(asArray);
                InputStream unzipped = new GZIPInputStream(zipped);
                reader = new InputStreamReader(unzipped);
            } else {
                if (asString.startsWith("ondex/output/")) {
                    String webapp = System.getProperty("webapp.root");
                    asString = asString.replace("ondex/", webapp);
                }
                logger.info(asString);
                File file = new File(asString);
                logger.info(file);
                logger.info(file.exists());
                reader = ReaderArgumentDefinition.StringToReader(asString);
                logger.info("reader is " + reader + " " + reader.getClass().getSimpleName());
            }
            createArguement(plugin, arguments, arguementName, reader);
        } catch (Exception e) {
            throw new CaughtException("Trying to create a reader", e, logger);
        }
    }

    protected File createFileWriterArguement(AbstractONDEXPlugin plugin, AbstractArguments arguments, String arguementName, String suffix, boolean gzip) throws CaughtException {
        try {
            Writer writer;
            File file;
            if (gzip) {
                file = createOutputFile("output", suffix + ".gz");
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                GZIPOutputStream gZIPOutputStream = new GZIPOutputStream(fileOutputStream);
                writer = new OutputStreamWriter(gZIPOutputStream);
            } else {
                file = createOutputFile("output", suffix);
                writer = new FileWriter(file);
            }
            createArguement(plugin, arguments, arguementName, writer);
            return file;
        } catch (Exception e) {
            throw new CaughtException("Trying to create a FileWriter", e, logger);
        }
    }

    protected StringWriter createStringWriterArguement(AbstractONDEXPlugin plugin, AbstractArguments arguments, String arguementName) throws CaughtException, IllegalArguementsException {
        try {
            StringWriter writer = new StringWriter();
            createArguement(plugin, arguments, arguementName, writer);
            return writer;
        } catch (Exception e) {
            throw new CaughtException("Trying to create a StringWriter", e, logger);
        }
    }
}
