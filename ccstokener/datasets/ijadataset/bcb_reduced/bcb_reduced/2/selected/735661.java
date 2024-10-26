package net.sf.csutils.poi;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

/**
 * Test case for the {@link DocumentParser}.
 */
public class DocumentParserTest {

    @Test
    public void testNewXls() throws Exception {
        final Workbook workbook = new HSSFWorkbook();
        final String outputFile = "target/DocumentParserTestNewXls.xls";
        run(workbook, outputFile, "documentParserTest.xml");
    }

    @Test
    public void testNewXlsx() throws Exception {
        final Workbook workbook = new XSSFWorkbook();
        final String outputFile = "target/DocumentParserTestNewXls.xlsx";
        run(workbook, outputFile, "documentParserTest.xml");
    }

    private void run(final Workbook pWorkbook, final String pOutputFile, String pTemplate) throws IOException, XMLStreamException {
        InputStream istream = null;
        try {
            final URL url = getClass().getResource(pTemplate);
            istream = url.openStream();
            final DocumentParser parser = new DocumentParser();
            parser.process(pWorkbook, new StreamSource(url.openStream()));
            istream.close();
        } finally {
            if (istream != null) {
                try {
                    istream.close();
                } catch (Throwable t) {
                }
            }
        }
        OutputStream ostream = null;
        try {
            istream = null;
            ostream = new FileOutputStream(pOutputFile);
            pWorkbook.write(ostream);
            ostream.close();
            ostream = null;
        } finally {
            if (ostream != null) {
                try {
                    ostream.close();
                } catch (Throwable t) {
                }
            }
        }
    }

    private void run(final String pTemplateSheet, String pTemplate) throws IOException, XMLStreamException {
        final URL url = getClass().getResource(pTemplateSheet);
        InputStream istream = null;
        final Workbook workbook;
        try {
            istream = url.openStream();
            workbook = new HSSFWorkbook(istream, true);
            istream = null;
        } finally {
            if (istream != null) {
                try {
                    istream.close();
                } catch (Throwable t) {
                }
            }
        }
        final String outputFile = "target/" + pTemplateSheet;
        run(workbook, outputFile, pTemplate);
    }

    @Test
    public void testOldXls() throws Exception {
        run("DocumentParserTestOld.xls", "documentParserTest.xml");
    }

    @Test
    public void testRows() throws Exception {
        run("RowsTest.xls", "rowsTest.xml");
    }

    @Test
    public void testFormulas() throws Exception {
        run("FormulaTest.xls", "formulaTest.xml");
    }

    @Test
    public void testConditionalFormulas() throws Exception {
        run("ConditionalFormulaTest.xls", "conditionalFormulas.xml");
    }

    @Test
    public void testGroups() throws Exception {
        run("GroupTest.xls", "GroupTest.xml");
    }
}
