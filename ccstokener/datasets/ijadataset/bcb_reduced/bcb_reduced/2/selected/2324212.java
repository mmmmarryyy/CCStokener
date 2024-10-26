package at.langegger.xlwrap.spreadsheet;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import at.langegger.xlwrap.common.XLWrapException;
import at.langegger.xlwrap.spreadsheet.csv.CSVWorkbook;
import at.langegger.xlwrap.spreadsheet.excel.ExcelWorkbook;
import at.langegger.xlwrap.spreadsheet.opendoc.OpenDocumentWorkbook;
import com.hp.hpl.jena.util.FileUtils;

/**
 * @author dorgon
 *
 */
public class WorkbookFactory {

    public static enum Type {

        MSEXCEL, OPENDOCUMENT, OOFICE, CSV
    }

    public static final Map<String, Type> extToType = new Hashtable<String, Type>();

    static {
        extToType.put("xls", Type.MSEXCEL);
        extToType.put("ods", Type.OPENDOCUMENT);
        extToType.put("sxc", Type.OOFICE);
        extToType.put("csv", Type.CSV);
        extToType.put("txt", Type.CSV);
    }

    /**
	 * @param fileName
	 * @return
	 * @throws XLWrapException
	 */
    public static Workbook getWorkbook(String fileName) throws XLWrapException {
        return getWorkbook(fileName, null);
    }

    /**
	 * @param fileName
	 * @param type, if null type will be detected based on the file extension
	 * @return
	 * @throws XLWrapException
	 */
    private static Workbook getWorkbook(String fileName, Type type) throws XLWrapException {
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
        Type t = extToType.get(ext);
        try {
            switch(t) {
                case MSEXCEL:
                    return new ExcelWorkbook(open(fileName), fileName);
                case OPENDOCUMENT:
                    File f = null;
                    if (FileUtils.isURI(fileName)) f = downloadToTemp(fileName); else f = new File(fileName);
                    return new OpenDocumentWorkbook(f, fileName);
                case CSV:
                    return new CSVWorkbook(open(fileName), fileName);
                default:
                    throw new XLWrapException("Cannot open document '" + fileName + "', extension '." + ext + "' is not recognized.");
            }
        } catch (MalformedURLException e) {
            throw new XLWrapException("Failed to open spreadsheet from <" + fileName + ">.", e);
        } catch (Throwable e) {
            throw new XLWrapException("Failed to open spreadsheet file '" + fileName + "'.", e);
        }
    }

    /**
	 * @param fileName
	 * @return
	 * @throws XLWrapException 
	 */
    private static File downloadToTemp(String fileName) throws IOException, XLWrapException {
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (!tmpDir.endsWith(File.pathSeparator)) tmpDir = "/" + tmpDir;
        URL url = new URL(fileName);
        InputStream in = url.openStream();
        String file = null;
        Matcher m = Pattern.compile("^.*\\/(.*)$").matcher(fileName);
        if (m.find()) file = m.group(1);
        File tmp = new File(tmpDir + file);
        BufferedOutputStream out;
        try {
            out = new BufferedOutputStream(new FileOutputStream(tmp));
        } catch (IOException e) {
            throw new XLWrapException("Failed to download " + fileName + ", cannot write into temp directory (" + tmpDir + file + ".");
        }
        if (!tmp.canWrite()) throw new XLWrapException("Failed to download " + fileName + ", cannot write into temp directory " + tmpDir + ".");
        int b;
        while ((b = in.read()) >= 0) out.write(b);
        in.close();
        out.close();
        return tmp;
    }

    private static InputStream open(String url) throws MalformedURLException, IOException {
        if (FileUtils.isURI(url)) return new URL(url).openStream(); else return new FileInputStream(url);
    }
}
