package uk.org.ogsadai.client.toolkit.astro;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.OnceRowPipe;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.votable.VOTableBuilder;
import uk.org.ogsadai.common.files.FileUtilities;

/**
 * Simple command line client for executing ADQL queries and printing out the
 * results.
 * 
 * @author The OGSA-DAI Project Team.
 * 
 */
public class TAPClient {

    /** Copyright statement. */
    private static final String COPYRIGHT_NOTICE = "Copyright (c) The University of Edinburgh, 2011.";

    /** Read the query from a file. */
    public static final String FILE = "-f";

    /** Read the query from the command line argument. */
    public static final String QUERY = "-q";

    /** 
     * Print out the raw response. This is useful if an error occurs because
     * the STIL library doesn't handle errors.  
     */
    public static final String PRINT_RAW = "-r";

    /**
     * Run from the command line.
     * 
     * @param args command-line arguments
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            throw new IllegalArgumentException("Usage: TAPClient URL ( <-q query> | <-f file> ) [ -r ]");
        }
        String tapURL = args[0];
        String expression = getQuery(args, 1);
        System.out.println("TAP URL:    " + tapURL);
        System.out.println("ADQL Query: " + expression);
        System.out.println();
        long start = System.currentTimeMillis();
        final InputStream result = getStreamFromURL(tapURL, expression);
        try {
            if (args.length == 4 && PRINT_RAW.equals(args[3])) {
                printRawResult(result);
            } else {
                prettyPrintResult(result);
            }
            System.out.println("Duration: " + (System.currentTimeMillis() - start) + " ms");
        } finally {
            result.close();
        }
    }

    private static String getQuery(String[] args, int position) throws IOException {
        String query;
        if (FILE.equals(args[position])) {
            query = FileUtilities.readFileToString(args[position + 1]);
        } else if (QUERY.equals(args[position])) {
            query = args[position + 1];
        } else {
            throw new IllegalArgumentException("Unknown option: " + args[position]);
        }
        return query;
    }

    private static InputStream getStreamFromURL(String tapURL, String expression) throws IllegalStateException, IOException {
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(tapURL);
        List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
        postParameters.add(new BasicNameValuePair("REQUEST", "doQuery"));
        postParameters.add(new BasicNameValuePair("VERSION", "1.0"));
        postParameters.add(new BasicNameValuePair("LANG", "ADQL"));
        postParameters.add(new BasicNameValuePair("QUERY", expression));
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(postParameters);
        post.setEntity(formEntity);
        HttpResponse response = client.execute(post);
        return response.getEntity().getContent();
    }

    private static void printRawResult(final InputStream result) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(result));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        System.out.println();
    }

    private static void prettyPrintResult(final InputStream result) throws IOException {
        final VOTableBuilder builder = new VOTableBuilder();
        final OnceRowPipe sink = new OnceRowPipe();
        new Thread() {

            public void run() {
                try {
                    builder.streamStarTable(result, sink, null);
                } catch (IOException e) {
                    sink.setError(e);
                }
            }
        }.start();
        StarTable star = sink.waitForStarTable();
        int numberOfColumns = star.getColumnCount();
        System.out.print("| ");
        for (int i = 0; i < numberOfColumns; i++) {
            ColumnInfo columnInfo = star.getColumnInfo(i);
            System.out.print(columnInfo);
            System.out.print(" | ");
        }
        System.out.println();
        long rowCount = 0;
        RowSequence sequence = star.getRowSequence();
        try {
            while (sequence.next()) {
                rowCount++;
                System.out.print("| ");
                Object[] row = sequence.getRow();
                for (int i = 0; i < row.length; i++) {
                    System.out.print(row[i]);
                    System.out.print(" | ");
                }
                System.out.println();
            }
        } finally {
            sequence.close();
        }
        System.out.println();
        if (rowCount == 1) {
            System.out.println("Read 1 row.");
        } else {
            System.out.println("Read " + rowCount + " rows.");
        }
    }
}
