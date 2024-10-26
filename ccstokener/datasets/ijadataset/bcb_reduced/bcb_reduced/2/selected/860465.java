package uk.ac.ebi.intact.kickstart.psicquic;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * This script download MITAB data from the IntAct PSICQUIC REST service.
 * It expects a input file containing one MIQL [1] query per line.
 * The second parameter is the output file in which the data is going to be stored.
 *
 * [1] http://code.google.com/p/psicquic/wiki/MiqlReference
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 */
public class DownloadBatchMITAB {

    public static final String NEW_LINE = System.getProperty("line.separator");

    public static final Integer PAGE_SIZE = 200;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("usage: DownloadBatchMITAB <input.file> <output.file>");
            System.exit(1);
        }
        File input = new File(args[0]);
        File output = new File(args[1]);
        BufferedReader in = new BufferedReader(new FileReader(input));
        String str;
        int totalInteractionCount = 0;
        int matches = 0;
        while ((str = in.readLine()) != null) {
            int count = processMIQL(str, output);
            if (count > 0) {
                matches++;
            }
            totalInteractionCount += count;
        }
        in.close();
        System.out.println("\n\nTotal interaction(s): " + totalInteractionCount + " from " + matches + " identifier(s)");
    }

    private static int processMIQL(String miql, File output) throws IOException {
        int interactionCount = 0;
        System.out.print("\n" + miql + ": ");
        final FileWriter writer = new FileWriter(output, true);
        int from = 0;
        List<String> lines = null;
        do {
            URL url = new URL("http://www.ebi.ac.uk/Tools/webservices/psicquic/intact/webservices/current/search/query/" + miql + "?firstResult=" + from + "&maxResults=" + PAGE_SIZE);
            final URLConnection con = url.openConnection();
            con.setConnectTimeout(5000);
            final InputStream is = con.getInputStream();
            lines = readLines(is);
            interactionCount += lines.size();
            is.close();
            System.out.print(lines.size() + "  ");
            from += PAGE_SIZE;
            writeLines(lines, writer);
        } while (lines.size() == PAGE_SIZE);
        return interactionCount;
    }

    private static void writeLines(List<String> lines, Writer writer) throws IOException {
        BufferedWriter out = new BufferedWriter(writer);
        for (String line : lines) {
            out.write(line + NEW_LINE);
        }
        out.flush();
    }

    private static List<String> readLines(InputStream inputStream) throws IOException {
        List<String> lines = new ArrayList<String>();
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String str;
        while ((str = in.readLine()) != null) {
            lines.add(str);
        }
        in.close();
        return lines;
    }
}
