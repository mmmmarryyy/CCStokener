package net.hanjava.roas;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

/**
 * a Book contains multiple sheets.
 * a Book(aka. Workbook) is represented as a single file.
 */
public class XlsBook {

    private HSSFWorkbook workbook;

    /**
     * @param path http URL or local path.
     * @throws IOException
     */
    public XlsBook(String path) throws IOException {
        boolean isHttp = path.startsWith("http://");
        InputStream is = null;
        if (isHttp) {
            URL url = new URL(path);
            is = url.openStream();
        } else {
            File file = new File(path);
            is = new FileInputStream(file);
        }
        workbook = XlsBook.createWorkbook(is);
        is.close();
    }

    public XlsBook(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        workbook = XlsBook.createWorkbook(fis);
        fis.close();
    }

    public XlsBook(URL url) throws IOException {
        InputStream is = url.openStream();
        workbook = XlsBook.createWorkbook(is);
        is.close();
    }

    public XlsBook(InputStream source) throws IOException {
        workbook = XlsBook.createWorkbook(source);
    }

    /**
     * it will not close inputstream. you should close <code>source</code> manually.
     */
    private static HSSFWorkbook createWorkbook(InputStream source) throws IOException {
        POIFSFileSystem fs = new POIFSFileSystem(source);
        HSSFWorkbook book = new HSSFWorkbook(fs);
        return book;
    }

    public int getSheetCount() {
        return workbook.getNumberOfSheets();
    }

    HSSFWorkbook getWorkbook() {
        return workbook;
    }
}
