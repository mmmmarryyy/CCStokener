package net.sf.csutils.poi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import javax.xml.stream.XMLStreamException;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class DocumentProcessor {

    public static enum Type {

        /** Classic excel spreadsheet type (default)
		 */
        xls, /** Office Open spreadsheet type
		 */
        xlsx
    }

    private static final Logger log = Logger.getLogger(DocumentProcessor.class);

    private File spreadsheetFile;

    private URL spreadsheetUrl;

    private DocumentProcessor.Type spreadsheetType;

    public File getSpreadsheetFile() {
        return spreadsheetFile;
    }

    public void setSpreadsheetFile(File pSpreadsheetFile) {
        spreadsheetFile = pSpreadsheetFile;
    }

    public URL getSpreadsheetUrl() {
        return spreadsheetUrl;
    }

    public void setSpreadsheetUrl(URL pSpreadsheetUrl) {
        spreadsheetUrl = pSpreadsheetUrl;
    }

    public DocumentProcessor.Type getSpreadsheetType() {
        return spreadsheetType;
    }

    public void setSpreadsheetType(DocumentProcessor.Type pSpreadsheetType) {
        spreadsheetType = pSpreadsheetType;
    }

    protected Workbook getWorkbook() throws IOException {
        log.debug("getWorkbook: ->");
        final DocumentProcessor.Type type = getSpreadsheetType() == null ? DocumentProcessor.Type.xls : getSpreadsheetType();
        final File file = getSpreadsheetFile();
        final URL url = getSpreadsheetUrl();
        InputStream stream = null;
        final Workbook result;
        try {
            if (file == null) {
                if (url != null) {
                    log.debug("Opening URL " + url);
                    stream = url.openStream();
                }
            } else {
                log.debug("Opening file " + file);
                stream = new FileInputStream(file);
            }
            switch(type) {
                case xls:
                    result = stream == null ? new HSSFWorkbook() : new HSSFWorkbook(stream, true);
                    break;
                case xlsx:
                    result = stream == null ? new XSSFWorkbook() : new XSSFWorkbook(stream);
                    break;
                default:
                    throw new IllegalStateException("Invalid spreadsheet type: " + type);
            }
            if (stream != null) {
                stream.close();
                stream = null;
            }
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Throwable t) {
                }
            }
        }
        log.debug("getWorkbook: <- " + result);
        return result;
    }

    public void process(Workbook pWorkbook, Reader pReader) throws IOException, XMLStreamException {
        new DocumentParser().process(pWorkbook, pReader);
    }

    public void process(Reader pSource, OutputStream pTarget) throws IOException, XMLStreamException {
        final Workbook workbook = getWorkbook();
        process(workbook, pSource);
        workbook.write(pTarget);
    }
}
