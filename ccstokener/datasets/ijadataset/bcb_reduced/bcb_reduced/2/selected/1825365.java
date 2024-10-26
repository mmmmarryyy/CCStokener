package net.firstpartners.drools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.firstpartners.drools.data.RuleSource;
import net.firstpartners.drools.log.ILogger;
import net.firstpartners.drools.log.SpreadSheetLogger;
import net.firstpartners.spreadsheet.Range;
import net.firstpartners.spreadsheet.RangeConvertor;
import net.firstpartners.spreadsheet.RangeHolder;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.drools.compiler.DroolsParserException;

/**
 * Common Entry point to both Servlet and command line,
 * Unit Tests and samples for calling for rules manipulating Spreadsheet data
 */
public class SpreadSheetRuleRunner {

    private static final Logger log = Logger.getLogger(SpreadSheetRuleRunner.class.getName());

    RuleRunner ruleRunner;

    public SpreadSheetRuleRunner(IRuleLoader ruleLoader) {
        ruleRunner = new RuleRunner(ruleLoader);
    }

    /**
	 * 
	 * @param spreadsheetRange - Red Piranha representation of the spreadsheet format
	 * @param args
	 * @param nameOfLogSheet
	 * @return
	 * @throws DroolsParserException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
    public RangeHolder callRules(RangeHolder spreadsheetRange, RuleSource ruleSource, String nameOfLogSheet, ILogger logger) throws DroolsParserException, IOException, ClassNotFoundException {
        log.finer("============ Spreadsheet Cell Contents In =========");
        for (Range r : spreadsheetRange) {
            log.finer(r.toString());
        }
        ruleSource.addFacts(spreadsheetRange.getAllRangesAndCells());
        ruleRunner.runStatelessRules(ruleSource, logger);
        log.finer("============ Spreadsheet Cell Contents Out =========");
        for (Range r : spreadsheetRange) {
            log.finer(r.toString());
        }
        return spreadsheetRange;
    }

    /**
	 * 
	 * @param inputFromExcel
	 *            - the excel data sheet as already opened as a Java Stream
	 * @param args
	 * @param nameOfLogSheet
	 * @return
	 * @throws DroolsParserException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
    public HSSFWorkbook callRules(InputStream inputFromExcel, RuleSource ruleSource, String nameOfLogSheet) throws DroolsParserException, IOException, ClassNotFoundException {
        SpreadSheetLogger spreadsheetLogger = new SpreadSheetLogger();
        HSSFWorkbook wb = new HSSFWorkbook(new POIFSFileSystem(inputFromExcel));
        RangeHolder ranges = RangeConvertor.convertExcelToCells(wb);
        callRules(ranges, ruleSource, nameOfLogSheet, spreadsheetLogger);
        RangeConvertor.convertCellsToExcel(wb, ranges);
        spreadsheetLogger.flush(wb, nameOfLogSheet);
        inputFromExcel.close();
        return wb;
    }

    /**
	 * Read an excel file and spit out what we find.
	 * 
	 * Method is protected (not private) to allow for unit testing
	 * 
	 * @param args
	 *            Expect one argument that is the file to read.
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws DroolsParserException
	 * @throws Exception
	 * @throws Exception
	 */
    public HSSFWorkbook callRules(URL urlOfExcelDataFile, RuleSource ruleSource, String excelLogSheet) throws DroolsParserException, IOException, ClassNotFoundException {
        InputStream inputFromExcel = null;
        try {
            log.info("Looking for url:" + urlOfExcelDataFile);
            inputFromExcel = urlOfExcelDataFile.openStream();
            log.info("found url:" + urlOfExcelDataFile);
        } catch (MalformedURLException e) {
            log.log(Level.SEVERE, "Malformed URL Exception Loading rules", e);
            throw e;
        } catch (IOException e) {
            log.log(Level.SEVERE, "IO Exception Loading rules", e);
            throw e;
        }
        return callRules(inputFromExcel, ruleSource, excelLogSheet);
    }

    /**
	 * Read an excel file and spit out what we find.
	 * 
	 * Method is protected (not private) to allow for unit testing
	 * 
	 * @param args
	 *            Expect one argument that is the file to read.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws DroolsParserException
	 * @throws Exception
	 * @throws Exception
	 */
    public HSSFWorkbook callRules(File locationOfExcelDataFile, RuleSource ruleSource, String excelLogSheet) throws IOException, DroolsParserException, ClassNotFoundException {
        InputStream inputFromExcel = null;
        if (locationOfExcelDataFile == null) {
            throw new IOException("java.io.File cannot be null");
        }
        if (!locationOfExcelDataFile.exists()) {
            throw new IOException("no file at location:" + locationOfExcelDataFile.getAbsolutePath());
        }
        try {
            log.info("Looking for file:" + locationOfExcelDataFile.getAbsolutePath());
            inputFromExcel = new FileInputStream(locationOfExcelDataFile);
            log.info("found file:" + locationOfExcelDataFile);
        } catch (IOException e) {
            log.log(Level.SEVERE, "IO Exception Loading rules", e);
            throw e;
        }
        return callRules(inputFromExcel, ruleSource, excelLogSheet);
    }
}
