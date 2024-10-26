package org.mindswap.swoop.automation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Vector;

/**
 * @author Dave Wang
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class WWSpeciator {

    public static final String WONDERWEB = "http://phoebus.cs.man.ac.uk:9999/OWL/Validator";

    public static final String OWLLITE = "Lite";

    public static final String ERROR = "Error";

    public static void speciate(String input, String output, int num_skipped) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(input));
            Vector uris = new Vector();
            String rline = "";
            int counter = 1;
            while ((rline = reader.readLine()) != null) {
                if (counter < num_skipped) {
                    counter++;
                    continue;
                }
                rline = rline.trim();
                uris.addElement(rline);
            }
            BufferedWriter logger = new BufferedWriter(new FileWriter("WWSpeciatorLog.txt"));
            BufferedWriter writer = new BufferedWriter(new FileWriter(output));
            for (int i = 0; i < uris.size(); i++) {
                long startTime = System.currentTimeMillis();
                String ontoURI = (String) uris.elementAt(i);
                String originalURI = ontoURI.toString();
                logger.write("Wonder Web Checking: [" + (i + counter) + "] " + originalURI);
                logger.newLine();
                System.out.println("Wonder Web Checking: [" + (i + counter) + "] " + originalURI);
                try {
                    URLConnection urlConn;
                    DataOutputStream wWriter;
                    URL url = new URL(WONDERWEB);
                    urlConn = url.openConnection();
                    urlConn.setDoInput(true);
                    urlConn.setDoOutput(true);
                    urlConn.setUseCaches(false);
                    urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    wWriter = new DataOutputStream(urlConn.getOutputStream());
                    String content = "url=" + URLEncoder.encode(ontoURI, "UTF-8") + "&level=Lite&abstract=no";
                    wWriter.writeBytes(content);
                    wWriter.flush();
                    wWriter.close();
                    BufferedReader myReader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
                    String line = "";
                    String species = OWLLITE;
                    while ((line = myReader.readLine()) != null) {
                        if (line.startsWith("<h3>OWL")) {
                            int eindex = line.indexOf("</h3>");
                            species = line.substring(8, eindex);
                        }
                    }
                    System.out.println("!! " + species + " !!");
                    writer.write(originalURI + "\t" + species);
                    writer.newLine();
                    writer.flush();
                    logger.write(" - " + originalURI + " is " + species);
                    logger.newLine();
                    logger.flush();
                } catch (Exception e) {
                    writer.write(originalURI + "\t" + ERROR);
                    writer.newLine();
                    writer.flush();
                    logger.write(" - " + originalURI + " is " + ERROR);
                    logger.newLine();
                    logger.flush();
                    logger.write(e.toString());
                    logger.newLine();
                } finally {
                    long stopTime = System.currentTimeMillis();
                    double duration = (stopTime - startTime) / 10000d;
                    logger.write(" - Took " + duration + " seconds ");
                    logger.newLine();
                    logger.flush();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("Starting...");
        WWSpeciator.speciate("supp.txt", "speciation(WW).txt", 1);
        System.out.println("done");
    }
}
