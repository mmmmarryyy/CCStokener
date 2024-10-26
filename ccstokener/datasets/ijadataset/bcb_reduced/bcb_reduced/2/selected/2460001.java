package de.fzi.kadmos.cmdutils;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVStrategy;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import de.fzi.kadmos.api.Alignment;
import de.fzi.kadmos.api.IncompatibleAlignmentsException;
import de.fzi.kadmos.cmdutils.formatter.CSVResultFormatter;
import de.fzi.kadmos.cmdutils.formatter.ConsoleResultFormatter;
import de.fzi.kadmos.cmdutils.formatter.GNUPlotResultFormatter;
import de.fzi.kadmos.cmdutils.result.EvaluatorResult;
import de.fzi.kadmos.cmdutils.result.PrecRecEvaluatorResult;
import de.fzi.kadmos.cmdutils.result.SymProxEvaluatorResult;
import de.fzi.kadmos.evaluator.impl.PrecRecEvaluatorImpl;
import de.fzi.kadmos.evaluator.impl.SymProxEvaluatorImpl;
import de.fzi.kadmos.parser.AlignmentParser;
import de.fzi.kadmos.parser.AlignmentParserException;
import de.fzi.kadmos.parser.ParsingLevel;
import de.fzi.kadmos.parser.impl.INRIAFormatParser;

/**
 * Command line evaluator utility.
 *
 * This tool can be used as a standalone application for evaluating an alignment.
 *
 * <pre>
 * Usage: Evaluator REFERENCE ALIGNMENT [OPTION]...
 *   -c, --classical-pr   compute classical precision/recall (default)
 *   -s, --symmetric-pr   compute symmetric precision/recall
 *   -d, --deep-scan 	  if given, the directory containing one or more alignments is recursively scanned
 *   -x, --csv 			  output evaluator result as CSV file
 *   -g, --gnu-plot		  output evaluator result as GNUPlot compatible file
 *   -b. --batch		  batch processes a comma-separated csv-file containing pairs of reference alignment (format: ID,REFERENCE,ALIGNMENT)
 *   -l, --laxParsing     allows to parse alignments which have n:m correspondences and not only 1:1
 *   -h, --help           display help
 *   -v, --version        display version and license information
 * </pre>
 * REFERENCE is a pathname or resolvable URL pointing to
 * the reference alignment
 * ALIGNMENT is a pathnames or resolvable URLs pointing to
 * one of the following:
 * a) Direct reference to an alignment to be evaluated
 * b) Directory containing one or more alignments to be evaluated (if -d is provided, the directory is recursively scanned)
 * c) Text file (with mime type plain/text) containing one or more direct references to alignments to be evaluated, directories, or other text files
 *
 * @author Juergen Bock
 * @author Matthias Stumpp
 * @version 1.2.0
 * @since 1.0
 */
public final class Evaluator {

    private static final String VERSION = "1.0.0";

    private static final String HELP_LONGOPT = "help";

    private static final String VERSION_LONGOPT = "version";

    private static final String CLASSICAL_PR_LONGOPT = "classical-pr";

    private static final String SYMMETRIC_PR_LONGOPT = "symmetric-pr";

    private static final String DEEP_SCAN_LONGOPT = "deep-scan";

    private static final String BATCH_PROCESS_LONGOPT = "batch-file";

    private static final String OUTPURT_FORMAT_CSV_LONGOPT = "csv";

    private static final String OUTPURT_FORMAT_GNUPLOT_LONGOPT = "gnu-plot";

    private static final String LAX_PARSING = "laxParsing";

    private String referenceLocation;

    private String alignmentSource;

    private String batchFileLocation;

    private List<AlignmentWrapper> alignmentWrappers = new ArrayList<AlignmentWrapper>();

    private List<EvaluatorResult> evaluatorResults = new ArrayList<EvaluatorResult>();

    private boolean deepScan;

    private boolean computeClassicalPR;

    private boolean computeSymmetricPR;

    private String outputDirCSV;

    private String outputDirGNUPlot;

    private boolean prepareCSV;

    private boolean prepareGNUPlot;

    private boolean laxParsing = false;

    private static Logger logger = Logger.getLogger(Evaluator.class);

    private static final String log4jConfigFile = "log4j.properties";

    /**
     * Creates a new instance.
     * (Constructor is package private for testing purposes.)
     */
    Evaluator() {
        if (new File(log4jConfigFile).exists()) {
            PropertyConfigurator.configure(log4jConfigFile);
        } else if (new File("config" + File.separator + log4jConfigFile).exists()) PropertyConfigurator.configure("config" + File.separator + log4jConfigFile); else {
            BasicConfigurator.configure();
            Logger.getRootLogger().setLevel(Level.OFF);
        }
    }

    /**
     * Entry point for the command line evaluator.
     *
     * @param args
     */
    public static void main(String[] args) {
        Evaluator instance = new Evaluator();
        printVersion();
        try {
            instance.parseArguments(args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return;
        }
        Alignment reference = null;
        String alignmentIdentifier = "";
        Alignment alignment = null;
        EvaluatorResult result = null;
        for (int i = 0; i < instance.alignmentWrappers.size(); i++) {
            if (instance.computeClassicalPR) {
                try {
                    reference = instance.alignmentWrappers.get(i).getReference();
                    alignmentIdentifier = instance.alignmentWrappers.get(i).getAlignmentIdentifier();
                    alignment = instance.alignmentWrappers.get(i).getAlignment();
                    result = new PrecRecEvaluatorResult(alignmentIdentifier, alignment);
                    result.applyEvalutor(PrecRecEvaluatorImpl.getInstance(reference));
                    instance.addEvaluatorResult(result);
                    ConsoleResultFormatter out = ConsoleResultFormatter.getInstance();
                    out.format(result);
                } catch (IncompatibleAlignmentsException e) {
                    System.err.println(e.getMessage());
                } catch (KADMOSCMDException e) {
                    System.err.println(e.getMessage());
                }
            }
            if (instance.computeSymmetricPR) {
                try {
                    reference = instance.alignmentWrappers.get(i).getReference();
                    alignmentIdentifier = instance.alignmentWrappers.get(i).getAlignmentIdentifier();
                    alignment = instance.alignmentWrappers.get(i).getAlignment();
                    result = new SymProxEvaluatorResult(alignmentIdentifier, alignment);
                    result.applyEvalutor(SymProxEvaluatorImpl.getInstance(reference));
                    instance.addEvaluatorResult(result);
                    ConsoleResultFormatter out = ConsoleResultFormatter.getInstance();
                    out.format(result);
                } catch (IncompatibleAlignmentsException e) {
                    System.err.println(e.getMessage());
                } catch (KADMOSCMDException e) {
                    System.err.println(e.getMessage());
                }
            }
        }
        if (instance.prepareCSV) {
            CSVResultFormatter out = CSVResultFormatter.getInstance();
            if (instance.outputDirCSV != null) {
                out.setOutputDir(instance.outputDirCSV);
            }
            try {
                out.format(instance.getEvaluatorResults());
            } catch (KADMOSCMDException e) {
                System.err.println(e.getMessage());
            }
        }
        if (instance.prepareGNUPlot) {
            GNUPlotResultFormatter out = GNUPlotResultFormatter.getInstance();
            if (instance.outputDirGNUPlot != null) {
                out.setOutputDir(instance.outputDirGNUPlot);
            }
            try {
                out.format(instance.getEvaluatorResults());
            } catch (KADMOSCMDException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    /**
     * Parses the command line arguments and sets private fields accordingly.
     * (Method is package private for testing purposes.)
     * @param args Command line arguments.
     * @throws KADMOSCMDException if unexpected or incomplete arguments were provided.
     * @throws IllegalArgumentException 
     * @throws AlignmentParserException 
     * @throws IOException 
     */
    void parseArguments(String[] args) throws KADMOSCMDException, AlignmentParserException, IllegalArgumentException, IOException {
        LongOpt[] longopts = { new LongOpt(CLASSICAL_PR_LONGOPT, LongOpt.NO_ARGUMENT, null, 'c'), new LongOpt(SYMMETRIC_PR_LONGOPT, LongOpt.NO_ARGUMENT, null, 's'), new LongOpt(HELP_LONGOPT, LongOpt.NO_ARGUMENT, null, 'h'), new LongOpt(VERSION_LONGOPT, LongOpt.NO_ARGUMENT, null, 'v'), new LongOpt(DEEP_SCAN_LONGOPT, LongOpt.NO_ARGUMENT, null, 'd'), new LongOpt(LAX_PARSING, LongOpt.NO_ARGUMENT, null, 'l'), new LongOpt(BATCH_PROCESS_LONGOPT, LongOpt.REQUIRED_ARGUMENT, null, 'b'), new LongOpt(OUTPURT_FORMAT_CSV_LONGOPT, LongOpt.OPTIONAL_ARGUMENT, null, 'x'), new LongOpt(OUTPURT_FORMAT_GNUPLOT_LONGOPT, LongOpt.OPTIONAL_ARGUMENT, null, 'g') };
        Getopt getopt = new Getopt("Evaluator", args, "cshvdlb:x::g::", longopts);
        int c;
        while ((c = getopt.getopt()) != -1) {
            switch(c) {
                case 'c':
                    computeClassicalPR = true;
                    break;
                case 's':
                    computeSymmetricPR = true;
                    break;
                case 'h':
                    printUsage();
                    return;
                case 'v':
                    printVersion();
                    return;
                case 'd':
                    deepScan = true;
                    break;
                case 'b':
                    batchFileLocation = getopt.getOptarg().replaceAll(" +", "");
                    break;
                case 'l':
                    laxParsing = true;
                    break;
                case 'x':
                    prepareCSV = true;
                    if (getopt.getOptarg() != null) {
                        outputDirCSV = getopt.getOptarg().replaceAll(" +", "");
                    }
                    break;
                case 'g':
                    prepareGNUPlot = true;
                    if (getopt.getOptarg() != null) {
                        outputDirGNUPlot = getopt.getOptarg().replaceAll(" +", "");
                    }
                    break;
                case '?':
                    throw new IllegalArgumentException("Getopt error code: " + c);
                default:
                    throw new IllegalArgumentException("getopt() returned " + c);
            }
        }
        if (batchFileLocation == null) {
            if ((getopt.getOptind() + 1) >= args.length) {
                System.err.println("Evaluator: missing operand");
                printBadUsage();
                throw new IllegalArgumentException("missing operand");
            }
            referenceLocation = args[getopt.getOptind()];
            Alignment reference = parseReference(referenceLocation);
            alignmentSource = args[getopt.getOptind() + 1];
            processAlignmentsFromAlignmentSource(null, reference, alignmentSource);
        } else {
            processAlignmentsFromBatchFile(batchFileLocation);
        }
        if (!computeClassicalPR && !computeSymmetricPR) {
            computeClassicalPR = true;
        }
    }

    /**
     * Parses a reference alignment.
     * @param location Location of the reference alignment. 
     * @return Reference to the reference alignment object.
     * @throws AlignmentParserException if a parsing error occurs.
     * @throws IllegalArgumentException if the reference alignment location is null.
     * @throws FileNotFoundException if the reference alignment location cannot be resolved.
     */
    Alignment parseReference(String referencelocation) throws AlignmentParserException, IllegalArgumentException, FileNotFoundException {
        return parseItem(referencelocation);
    }

    /**
     * Parses an alignment for evaluation.
     * @param location Location of the alignment. 
     * @return Reference to the alignment object.
     * @throws AlignmentParserException if a parsing error occurs.
     * @throws IllegalArgumentException if the alignment location is null.
     * @throws FileNotFoundException if the alignment location cannot be resolved.
     */
    Alignment parseAlignment(String alignmentLocation) throws AlignmentParserException, IllegalArgumentException, FileNotFoundException {
        return parseItem(alignmentLocation);
    }

    /**
     * Parses an item, i.e. either a reference alignment, or an alignment to be evaluated.
     * @param itemLocation Location of the item.
     * @param itemName Name of the item (for error reporting).
     * @return Item reference.
     * @throws AlignmentParserException if there is a parsing problem.
     * @throws FileNotFoundException if item location cannot be resolved for reading.
     * @throws IllegalArgumentException if item location is null.
     */
    private Alignment parseItem(String itemLocation) throws AlignmentParserException, IllegalArgumentException, FileNotFoundException {
        AlignmentParser parser = INRIAFormatParser.getInstance();
        ParsingLevel parsinglevel;
        if (laxParsing) parsinglevel = ParsingLevel.ONE; else parsinglevel = ParsingLevel.TWO;
        parser.setParsingLevel(parsinglevel);
        Alignment item = parser.parse(itemLocation);
        return item;
    }

    /**
     * Retrieves the location of the reference alignment object used for evaluation.
     * (Method is package private for testing purposes.)
     * @return Location of the reference alignment.
     */
    String getReferenceLocation() {
        return referenceLocation;
    }

    /**
     * Retrieves the alignment source to be evaluated.
     * @return Source of the alignment to be evaluated.
     */
    String getAlignmentSource() {
        return alignmentSource;
    }

    /**
     * Retrieves the batch file location to be evaluated.
     * @return Source of the alignment to be evaluated.
     */
    String getBatchFileLocation() {
        return batchFileLocation;
    }

    /**
     * Adds an alignment object to the set of alignment objects.
     * @param Alignment to be added.
     */
    private void addAlignmentWrapper(AlignmentWrapper alignmentWrapper) {
        if (!alignmentWrappers.contains(alignmentWrapper)) {
            alignmentWrappers.add(alignmentWrapper);
        }
    }

    /**
     * Retrieves the alignments to be evaluated.
     * @return Alignments to be evaluated.
     */
    List<AlignmentWrapper> getAlignmentWrappers() {
        return alignmentWrappers;
    }

    /**
     * Adds an evaluator result object to the set of evaluator result objects.
     * @param Evaluator result object to be added.
     */
    private void addEvaluatorResult(EvaluatorResult result) {
        if (!evaluatorResults.contains(result)) {
            evaluatorResults.add(result);
        }
    }

    /**
     * Retrieves the evaluator result objects for further processing.
     * @return List of evaluator result objects.
     */
    List<EvaluatorResult> getEvaluatorResults() {
        return evaluatorResults;
    }

    /**
     * Checks, whether this evaluator is configured to compute classical
     * precision and recall metrics.
     * (Method is package private for testing purposes.)
     * @return <code>true</code> if this evaluator is configured to compute
     *         classical precision and recall metrics, <code>false</code> otherwise.
     */
    boolean computeClassicalPR() {
        return computeClassicalPR;
    }

    /**
     * Checks, whether this evaluator is configured to compute symmetric
     * precision and recall metrics.
     * (Method is package private for testing purposes.)
     * @return <code>true</code> if this evaluator is configured to compute
     *         symmetric precision and recall metrics, <code>false</code> otherwise.
     */
    boolean computeSymmetricPR() {
        return computeSymmetricPR;
    }

    /**
     * Checks, whether this evaluator is configured to do a deeply (recursively) scan of a directory 
     * containing alignments
     * @return <code>true</code> if this evaluator is configured to deeply (recursively) scan a directory, 
     * <code>false</code> otherwise.
     */
    boolean deepScan() {
        return deepScan;
    }

    /**
     * Processes an alignment source possibly containing concrete alignment locations.
     * @param AlignmentSource to be scanned for concrete alignment locations. Such as source may either a 
     * 		  direct reference to an alignment, a directory containing one or more alignments or a text file containing
     * 		  one or more references to alignments (direct, director, text file). As alignment source, both pathnames
     * 		  and resolvable URLs are processed.
     * @throws KADMOSCMDException if alignmentSource cannot be processed.
     * @throws IOException 
     */
    private void processAlignmentsFromAlignmentSource(String name, Alignment reference, String alignmentSource) throws AlignmentParserException, IllegalArgumentException, KADMOSCMDException, IOException {
        if (alignmentSource == null) throw new IllegalArgumentException("alignmentSource is null");
        URL url;
        String st;
        BufferedReader reader;
        Alignment alignment;
        try {
            try {
                alignment = parseAlignment(alignmentSource);
                addAlignmentWrapper(new AlignmentWrapper(name, reference, alignmentSource, alignment));
            } catch (AlignmentParserException e1) {
                url = new URL(alignmentSource);
                reader = new BufferedReader(new InputStreamReader(url.openStream()));
                st = "";
                while (((st = reader.readLine()) != null)) {
                    alignment = parseAlignment(st);
                    addAlignmentWrapper(new AlignmentWrapper(name, reference, alignmentSource, alignment));
                }
            }
        } catch (Exception e1) {
            File itemFile = new File(alignmentSource);
            if (itemFile.exists()) {
                if (itemFile.isDirectory() && !itemFile.isHidden()) {
                    File[] files = itemFile.listFiles();
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].isFile() && !files[i].isHidden()) {
                            processAlignmentsFromAlignmentSource(name, reference, files[i].getPath());
                        } else if (files[i].isDirectory() && !files[i].isHidden() && deepScan) {
                            processAlignmentsFromAlignmentSource(name, reference, files[i].getPath());
                        }
                    }
                } else if (itemFile.isFile() && !itemFile.isHidden()) {
                    try {
                        alignment = parseAlignment(alignmentSource);
                        addAlignmentWrapper(new AlignmentWrapper(name, reference, alignmentSource, alignment));
                    } catch (Exception e2) {
                        reader = new BufferedReader(new FileReader(alignmentSource));
                        st = "";
                        while (((st = reader.readLine()) != null)) {
                            alignment = parseAlignment(st);
                            addAlignmentWrapper(new AlignmentWrapper(name, reference, st, alignment));
                        }
                    }
                } else {
                    throw new FileNotFoundException("File " + alignmentSource + " is neither directory nor file, or it is hidden.");
                }
            } else {
                throw new FileNotFoundException("File " + alignmentSource + " does not exists.");
            }
        }
    }

    /**
     * Processes a batch file containing one or more reference/alignment pairs.
     * @param Location of batch file.
     * @throws KADMOSCMDException if alignmentSource cannot be processed.
     * @throws IOException 
     * @throws IllegalArgumentException 
     * @throws AlignmentParserException 
     * @throws FileNotFoundException
     */
    private void processAlignmentsFromBatchFile(String batchFileLocation) throws IOException, FileNotFoundException, KADMOSCMDException, AlignmentParserException, IllegalArgumentException {
        URL url;
        BufferedReader reader;
        try {
            url = new URL(batchFileLocation);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
        } catch (Exception e) {
            reader = new BufferedReader(new FileReader(batchFileLocation));
        }
        CSVParser csvParser = new CSVParser(reader, new CSVStrategy(',', '"', '#', (char) -2, true, true, false, true));
        String[][] data = csvParser.getAllValues();
        for (int i = 0; i < data.length; i++) {
            if (data[i].length == 3) {
                processAlignmentsFromAlignmentSource(data[i][0], parseReference(data[i][1]), data[i][2]);
            } else {
                throw new KADMOSCMDException("Bad line in csv file.");
            }
        }
    }

    /**
     * Displays version and licensing information on the standard output.
     */
    private static void printVersion() {
        System.out.println("KADMOS command line utilities " + VERSION);
        System.out.println("Copyright (C) 2010-2011");
        System.out.println("FZI Research Center for Information Technology, Karlsruhe, Germany");
        System.out.println("This program comes with ABSOLUTELY NO WARRANTY; for details see file COPYING and COPYING.LESSER");
        System.out.println("This is free software, and you are welcome to redistribute it");
        System.out.println("under certain conditions; see file COPYING and COPYING.LESSER for details.\n");
    }

    /**
     * Displays usage instructions on the standard output.
     */
    private static void printUsage() {
        System.out.println("Usage: Evaluator REFERENCE ALIGNMENT [OPTION]...");
        System.out.println("  -c, --" + CLASSICAL_PR_LONGOPT + "\t compute classical precision/recall (default)");
        System.out.println("  -s, --" + SYMMETRIC_PR_LONGOPT + "\t compute symmetric precision/recall");
        System.out.println("  -d, --" + DEEP_SCAN_LONGOPT + "\t If given, the directory containing one or more alignments is recursively scanned");
        System.out.println("  -x, --" + OUTPURT_FORMAT_CSV_LONGOPT + "\t output evaluator result as CSV file");
        System.out.println("  -g, --" + OUTPURT_FORMAT_GNUPLOT_LONGOPT + "\t output evaluator result as GNUPlot compatible file ");
        System.out.println("  -b, --" + BATCH_PROCESS_LONGOPT + "\t batch processes a comma-separated csv-file containing pairs of reference alignment (format: ID,REFERENCE,ALIGNMENT)");
        System.out.println("  -l, --" + LAX_PARSING + "\t allows to parse alignments which have n:m correspondences and not only 1:1");
        System.out.println("  -h, --" + HELP_LONGOPT + "\t display this help");
        System.out.println("  -v, --" + VERSION_LONGOPT + "\t display version and license information\n");
        System.out.println("REFERENCE is a pathname or a resolvable URL pointing to");
        System.out.println("the reference alignment");
        System.out.println("ALIGNMENT is a pathnames or a resolvable URLs pointing to");
        System.out.println("one of the following:");
        System.out.println("Direct reference to an alignment to be evaluated");
        System.out.println("Directory containing one or more alignments to be evaluated (if -d is provided, the directory is scanned recursively)");
        System.out.println("Text file (with mime type plain/text) containing one or more direct references to alignments to be evaluated, directories, or other text files");
    }

    /**
     * Displays a message when some wrong arguments are passed or arguments are missing.
     */
    private static void printBadUsage() {
        System.err.println("Try: `Evaluator --help' for more information.");
    }

    public class AlignmentWrapper {

        private String alignmentName;

        private Alignment reference;

        private String alignmentLocation;

        private Alignment alignment;

        private AlignmentWrapper(String alignmentName, Alignment reference, String alignmentLocation, Alignment alignment) {
            this.alignmentName = alignmentName;
            this.reference = reference;
            this.alignmentLocation = alignmentLocation;
            this.alignment = alignment;
        }

        public String getAlignmentName() {
            return alignmentName;
        }

        public Alignment getReference() {
            return reference;
        }

        public String getAlignmentLocation() {
            return alignmentLocation;
        }

        public Alignment getAlignment() {
            return alignment;
        }

        public String getAlignmentIdentifier() {
            String id = "";
            if (alignmentName != null && !alignmentName.isEmpty()) {
                id = alignmentName;
            } else if (alignmentLocation != null && !alignmentLocation.isEmpty()) {
                URL url;
                try {
                    url = new URL(alignmentLocation);
                    id = url.getFile();
                } catch (MalformedURLException e) {
                    File file = new File(alignmentLocation);
                    if (file.isFile()) {
                        id = file.getName();
                    }
                }
            }
            return id;
        }
    }
}
