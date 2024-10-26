package uk.ac.ebi.intact.util.reactome;

import org.apache.commons.cli.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import uk.ac.ebi.intact.business.IntactException;
import uk.ac.ebi.intact.business.IntactHelper;
import uk.ac.ebi.intact.model.CvDatabase;
import uk.ac.ebi.intact.model.CvTopic;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Produce a file containing all intact Xrefs to Reactome.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id: ReactomeXrefs.java 5119 2006-06-28 11:43:09Z skerrien $
 * @since <pre>26-Jan-2006</pre>
 */
public class ReactomeXrefs {

    private static final String FILE_OPTION = "file";

    private static final String URL_OPTION = "url";

    private static final String NEW_LINE = System.getProperty("line.separator");

    private static final CommandLine setupCommandLine(String[] args) {
        Option helpOpt = new Option("help", "print this message.");
        Option fileOpt = OptionBuilder.withArgName("xrefFilename").hasArg().withDescription("output filename").create("file");
        fileOpt.setRequired(false);
        Option urlOpt = OptionBuilder.withArgName("reactomeURL").hasArg().withDescription("URL or reactome file to check against").create("url");
        urlOpt.setRequired(false);
        Options options = new Options();
        options.addOption(helpOpt);
        options.addOption(fileOpt);
        options.addOption(urlOpt);
        CommandLineParser parser = new BasicParser();
        CommandLine line = null;
        try {
            line = parser.parse(options, args, true);
        } catch (ParseException exp) {
            displayUsage(options);
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            System.exit(1);
        }
        if (line.hasOption("help")) {
            displayUsage(options);
            System.exit(0);
        }
        return line;
    }

    private static final void displayUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("ReactomeXrefs -file [filename] -url [url] ", options);
    }

    private static final String getDefaultFilename() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd@HH.mm");
        String time = formatter.format(new Date());
        return "reactomeXrefs." + time + ".txt";
    }

    /**
     * Retreives web content from a URL.
     *
     * @param url the URL we want to download data from.
     *
     * @return the content as a String.
     */
    public static List getUrlData(URL url) throws IOException {
        List beans = new ArrayList(256);
        System.out.println("Retreiving content for: " + url);
        StringBuffer content = new StringBuffer(4096);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String str;
        while ((str = in.readLine()) != null) {
            if (str.startsWith("#")) {
                continue;
            }
            StringTokenizer stringTokenizer = new StringTokenizer(str, "\t");
            String InteractionAc = stringTokenizer.nextToken();
            String reactomeId = stringTokenizer.nextToken();
            ReactomeBean reactomeBean = new ReactomeBean();
            reactomeBean.setReactomeID(reactomeId);
            reactomeBean.setInteractionAC(InteractionAc);
            beans.add(reactomeBean);
        }
        in.close();
        return beans;
    }

    /**
     * Extract IntAct IDs from from the given list.
     *
     * @param xrefsFromIntact the given list of Xrefs
     *
     * @return a new instance of List containing all IntAct IDs.
     */
    private static final List getIntactIDs(List xrefsFromIntact) {
        List ids = new ArrayList(xrefsFromIntact.size());
        for (Iterator iterator = xrefsFromIntact.iterator(); iterator.hasNext(); ) {
            ReactomeBean reactomeBean = (ReactomeBean) iterator.next();
            ids.add(reactomeBean.getInteractionAC());
        }
        return ids;
    }

    /**
     * Extract reactome IDs from from the given list.
     *
     * @param reactomeBeans the given list of Xrefs
     *
     * @return a new instance of List containing all reactome IDs.
     */
    private static final List getReactomeIDs(List reactomeBeans) {
        List ids = new ArrayList(reactomeBeans.size());
        for (Iterator iterator = reactomeBeans.iterator(); iterator.hasNext(); ) {
            ReactomeBean reactomeBean = (ReactomeBean) iterator.next();
            ids.add(reactomeBean.getReactomeID());
        }
        return ids;
    }

    public static void main(String[] args) throws IntactException, SQLException {
        CommandLine commandLine = setupCommandLine(args);
        String filename = commandLine.getOptionValue(FILE_OPTION);
        if (filename == null) {
            String defaultFilename = getDefaultFilename();
            System.out.println("No filename given, set it to default value: " + defaultFilename);
            filename = defaultFilename;
        }
        System.out.println("Opening file: " + filename);
        File outputFile = new File(filename);
        if (outputFile.exists() && !outputFile.canWrite()) {
            String defaultFilename = getDefaultFilename();
            System.out.println("Cannot write on " + outputFile.getAbsolutePath() + ", set it to default value.");
            filename = defaultFilename;
            System.out.println("Opening file: " + filename);
            outputFile = new File(filename);
        }
        String urlParam = commandLine.getOptionValue(URL_OPTION);
        if (urlParam != null) {
            System.out.println("URL: " + urlParam);
        }
        IntactHelper helper = null;
        try {
            helper = new IntactHelper();
            System.out.println("Database: " + helper.getDbName());
            CvTopic curatedComplex = helper.getObjectByLabel(CvTopic.class, CvTopic.CURATED_COMPLEX);
            if (curatedComplex == null) {
                throw new IllegalStateException("Could not find CvTopic by shortlabel: ");
            }
            Collection<CvDatabase> databases = helper.getObjectsByXref(CvDatabase.class, CvDatabase.REACTOME_COMPLEX_PSI_REF);
            if (databases == null || databases.isEmpty()) {
                throw new IllegalStateException("Could not find CvDatabase( reactome complex ) by Xref: " + CvDatabase.REACTOME_COMPLEX_PSI_REF);
            }
            CvDatabase reactome = databases.iterator().next();
            Connection connection = helper.getJDBCConnection();
            final String sql = "SELECT i.ac as interactionAC, x.primaryId as reactomeID\n" + "FROM ia_interactor i, ia_xref x, ia_annotation a, ia_int2annot i2a\n" + "WHERE i.objclass LIKE '%Interaction%' AND\n" + "      i.ac = x.parent_ac AND\n" + "      x.database_ac = '" + reactome.getAc() + "' AND\n" + "      i.ac = i2a.interactor_ac AND\n" + "      i2a.annotation_ac = a.ac AND\n" + "      a.topic_ac = '" + curatedComplex.getAc() + "'";
            System.out.println("sql = " + sql);
            QueryRunner queryRunner = new QueryRunner();
            ResultSetHandler handler = new BeanListHandler(ReactomeBean.class);
            List xrefsFromIntact = (List) queryRunner.query(connection, sql, handler);
            if (xrefsFromIntact == null) {
                System.err.println("Error: could not retreive any data about Reactome Xrefs in IntAct.");
                System.exit(1);
            }
            System.out.println("IntAct maintain " + xrefsFromIntact.size() + " Xref" + (xrefsFromIntact.size() > 1 ? "s" : "") + " to Reactome.");
            try {
                BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
                System.out.println(xrefsFromIntact.size() + " Reactome xref" + (xrefsFromIntact.size() > 1 ? "s" : "") + " found.");
                for (Iterator iterator = xrefsFromIntact.iterator(); iterator.hasNext(); ) {
                    ReactomeBean reactomeBean = (ReactomeBean) iterator.next();
                    out.write(reactomeBean.toSingleLine());
                    out.write(NEW_LINE);
                }
                out.close();
                System.out.println("File closed.");
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            if (urlParam != null) {
                try {
                    URL url = new URL(urlParam);
                    List xrefsFromReactome = getUrlData(url);
                    System.out.println("Reactome maintain " + xrefsFromReactome.size() + " Xref" + (xrefsFromReactome.size() > 1 ? "s" : "") + " to IntAct.");
                    Collection reactomeIDs = CollectionUtils.subtract(getReactomeIDs(xrefsFromIntact), getReactomeIDs(xrefsFromReactome));
                    if (!reactomeIDs.isEmpty()) {
                        System.err.println("We have found " + reactomeIDs.size() + " Reactome ID that are used in IntAct but not existing in Reactome anymore.");
                        for (Iterator iterator = reactomeIDs.iterator(); iterator.hasNext(); ) {
                            String reactomeId = (String) iterator.next();
                            System.out.println(reactomeId);
                        }
                    } else {
                        System.out.println("All Reactome Xref maintained in IntAct are Live in Reactome.");
                    }
                    Collection intactIDs = CollectionUtils.subtract(getIntactIDs(xrefsFromReactome), getIntactIDs(xrefsFromIntact));
                    if (!intactIDs.isEmpty()) {
                        System.err.println("We have found " + intactIDs.size() + " Intact Interaction AC that are used in Reactome but not existing in IntAct anymore.");
                        for (Iterator iterator = intactIDs.iterator(); iterator.hasNext(); ) {
                            String reactomeId = (String) iterator.next();
                            System.out.println(reactomeId);
                        }
                    } else {
                        System.out.println("All Reactome Xref maintained in Reactome are Live in IntAct.");
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            if (helper != null) {
                System.out.println("Closing database connection.");
                helper.closeStore();
            }
        }
    }
}
