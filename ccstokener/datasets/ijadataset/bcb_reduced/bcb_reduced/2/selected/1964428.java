package org.mcisb.thermodynamics;

import java.io.*;
import java.net.*;
import java.util.*;
import org.mcisb.ontology.*;
import org.mcisb.ontology.chebi.*;
import org.mcisb.util.*;
import org.mcisb.util.io.*;

/**
 * @author neilswainston
 * 
 */
public class RmgUtils {

    /**
	 * 
	 */
    private static final float KJ_CONVERSION_FACTOR = 4.184f;

    /**
	 * 
	 * @param chebiTerm
	 * @return float
	 * @throws Exception
	 */
    public static float getEnthalpyOfFormation(final ChebiTerm chebiTerm) throws Exception {
        BufferedWriter writer = null;
        OutputStream os = null;
        BufferedReader reader = null;
        try {
            final String adjacencyList = getAdjacencyList(chebiTerm);
            if (adjacencyList != null) {
                final File inputFile = File.createTempFile("thermodynamics", ".txt");
                writer = new BufferedWriter(new FileWriter(inputFile));
                final File outputFile = File.createTempFile("thermodynamics", ".txt");
                os = new FileOutputStream(outputFile);
                writer.write("Database: RMG_database");
                writer.newLine();
                writer.newLine();
                writer.write("PrimaryThermoLibrary:");
                writer.newLine();
                writer.write("END");
                writer.newLine();
                writer.newLine();
                writer.write(adjacencyList);
                writer.close();
                final Map<String, String> additionalEnvs = new HashMap<String, String>();
                additionalEnvs.put("RMG", "/Applications/rmg3");
                final Executor executor2 = new Executor(new String[] { "java", "-classpath", "RMG.jar", "ThermoDataEstimator", inputFile.getAbsolutePath() }, os, System.err, new File("/Applications/rmg3/bin"), additionalEnvs);
                executor2.doTask();
                final Exception e2 = executor2.getException();
                if (e2 != null) {
                    throw e2;
                }
                os.close();
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(outputFile)));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    if (line.equals("ThermoData is ")) {
                        line = reader.readLine();
                        if (line != null) {
                            final String[] tokens = line.split("\\s+");
                            return Float.parseFloat(tokens[0]) * KJ_CONVERSION_FACTOR;
                        }
                        break;
                    }
                }
            }
            throw new Exception();
        } finally {
            if (writer != null) {
                writer.close();
            }
            if (os != null) {
                os.close();
            }
            if (reader != null) {
                reader.close();
            }
        }
    }

    /**
	 * @param chebiTerm
	 * @return
	 * @throws IOException
	 */
    private static String getAdjacencyList(final ChebiTerm chebiTerm) throws IOException {
        final String smiles = chebiTerm.getSmiles();
        if (smiles == null) {
            return null;
        }
        final URL url = new URL("http://rmg.mit.edu/adjacencylist/" + smiles);
        return new String(StreamReader.read(url.openStream()));
    }

    /**
	 * @param args
	 * @throws Exception
	 */
    public static void main(String[] args) throws Exception {
        System.out.println(getEnthalpyOfFormation((ChebiTerm) OntologyUtils.getInstance().getOntologyTerm(Ontology.CHEBI, "CHEBI:15903")));
    }
}
