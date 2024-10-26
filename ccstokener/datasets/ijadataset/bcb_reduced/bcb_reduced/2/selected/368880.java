package net.sourceforge.ondex.validator.scientificspeciesname;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.sourceforge.ondex.InvalidPluginArgumentException;
import net.sourceforge.ondex.ONDEXPluginArguments;
import net.sourceforge.ondex.annotations.Custodians;
import net.sourceforge.ondex.args.ArgumentDefinition;
import net.sourceforge.ondex.args.FileArgumentDefinition;
import net.sourceforge.ondex.event.type.DataFileErrorEvent;
import net.sourceforge.ondex.event.type.DataFileMissingEvent;
import net.sourceforge.ondex.event.type.DatabaseErrorEvent;
import net.sourceforge.ondex.event.type.GeneralOutputEvent;
import net.sourceforge.ondex.validator.AbstractONDEXValidator;
import org.apache.log4j.Level;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;

/**
 * Implements a species formal scientific name lookup for the NCBI taxonomy.
 * 
 * @author taubertj, hindlem
 */
@Custodians(custodians = { "Matthew Hindle" }, emails = { "matthew_hindle at users.sourceforge.net" })
public class Validator extends AbstractONDEXValidator {

    private ONDEXPluginArguments va;

    private Environment myEnv = null;

    private EntityStore store = null;

    private PrimaryIndex<Integer, Entry> index = null;

    /**
	 * Returns name of this validator.
	 * 
	 * @return String
	 */
    public String getName() {
        return new String("ScientificSpeciesName");
    }

    /**
	 * Returns version of this validator.
	 * 
	 * @return String
	 */
    public String getVersion() {
        return new String("24.02.2012");
    }

    @Override
    public String getId() {
        return "scientificspeciesname";
    }

    /**
	 * Gets taxonomy file from FTp directly.
	 * 
	 * @param inputDir
	 */
    private void downloadTaxonomy(File inputDir) {
        File file = new File(inputDir.getAbsolutePath() + File.separatorChar + "names.dmp");
        if (!file.exists()) {
            GeneralOutputEvent goe = new GeneralOutputEvent("Trying to download NCBI Taxonomy from FTP to " + inputDir.getAbsolutePath(), "[Validator - downloadTaxonomy]");
            goe.setLog4jLevel(Level.INFO);
            fireEventOccurred(goe);
            try {
                URL url = new URL("ftp://ftp.ncbi.nih.gov/pub/taxonomy/taxdmp.zip");
                ZipInputStream zis = new ZipInputStream(url.openStream());
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    System.out.println("Unzipping: " + entry.getName());
                    int size;
                    byte[] buffer = new byte[2048];
                    FileOutputStream fos = new FileOutputStream(inputDir.getAbsolutePath() + File.separatorChar + entry.getName());
                    BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length);
                    while ((size = zis.read(buffer, 0, buffer.length)) != -1) {
                        bos.write(buffer, 0, size);
                    }
                    bos.flush();
                    bos.close();
                }
                zis.close();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setArguments(ONDEXPluginArguments va) throws InvalidPluginArgumentException {
        this.va = va;
        try {
            EnvironmentConfig myEnvConfig = new EnvironmentConfig();
            StoreConfig storeConfig = new StoreConfig();
            myEnvConfig.setAllowCreate(true);
            storeConfig.setAllowCreate(true);
            File inputDir = new File((String) va.getUniqueValue(FileArgumentDefinition.INPUT_DIR));
            String s = (String) va.getUniqueValue(FileArgumentDefinition.EXPORT_DIR);
            s = s.substring(0, s.indexOf("dbs") + 3) + File.separatorChar + s.substring(s.indexOf("validatorsout"), s.length());
            File dir = new File(s);
            if (!dir.exists()) {
                dir.mkdirs();
                downloadTaxonomy(inputDir);
            }
            myEnv = new Environment(dir, myEnvConfig);
            store = new EntityStore(myEnv, "ScientificSpeciesNameEntityStore", storeConfig);
            index = store.getPrimaryIndex(Integer.class, Entry.class);
            if (index.map().keySet().size() == 0) {
                String filename = inputDir.getAbsolutePath() + File.separator + "names.dmp";
                GeneralOutputEvent goe = new GeneralOutputEvent("Using taxonomy flatfile " + filename, "[Validator - validate]");
                goe.setLog4jLevel(Level.INFO);
                fireEventOccurred(goe);
                BufferedReader reader = new BufferedReader(new FileReader(filename));
                while (reader.ready()) {
                    String line = reader.readLine();
                    String[] split = line.split("\t\\|\t");
                    String type = split[3].toLowerCase().trim().substring(0, split[3].length() - 1).trim();
                    if (type.equals("scientific name")) {
                        String name = split[1].toLowerCase().trim();
                        index.put(new Entry(Integer.valueOf(split[0]), name));
                    }
                }
                reader.close();
            }
        } catch (DatabaseException dbe) {
            fireEventOccurred(new DatabaseErrorEvent(dbe.getMessage(), "[Validator - validate]"));
        } catch (FileNotFoundException fnfe) {
            fireEventOccurred(new DataFileMissingEvent(fnfe.getMessage(), "[Validator - validate]"));
        } catch (IOException ioe) {
            fireEventOccurred(new DataFileErrorEvent(ioe.getMessage(), "[Validator - validate]"));
        }
    }

    @Override
    public ONDEXPluginArguments getArguments() {
        return va;
    }

    @Override
    public Object validate(Object o) {
        if (o instanceof String || o instanceof Number) {
            Integer taxid;
            try {
                taxid = Integer.parseInt(o.toString());
            } catch (NumberFormatException nfe) {
                return o;
            }
            try {
                Entry org = index.get(taxid);
                if (org != null) return org.getName();
            } catch (DatabaseException dbe) {
                fireEventOccurred(new DatabaseErrorEvent(dbe.getMessage(), "[Validator - validate]"));
            }
        }
        return null;
    }

    @Override
    public void cleanup() {
        index = null;
        if (store != null) {
            try {
                store.close();
                store = null;
            } catch (DatabaseException dbe) {
                fireEventOccurred(new DatabaseErrorEvent(dbe.getMessage(), "[Validator - cleanup]"));
            }
        }
        if (myEnv != null) {
            try {
                myEnv.close();
                myEnv = null;
            } catch (DatabaseException dbe) {
                fireEventOccurred(new DatabaseErrorEvent(dbe.getMessage(), "[Validator - cleanup]"));
            }
        }
    }

    /**
	 * Requires no special arguments.
	 */
    public ArgumentDefinition<?>[] getArgumentDefinitions() {
        FileArgumentDefinition inputDir = new FileArgumentDefinition(FileArgumentDefinition.INPUT_DIR, "directory with taxonomy files", true, true, true, false);
        FileArgumentDefinition outputDir = new FileArgumentDefinition(FileArgumentDefinition.EXPORT_DIR, "temporary directory for index structure", true, true, true, false);
        return new ArgumentDefinition<?>[] { inputDir, outputDir };
    }
}
