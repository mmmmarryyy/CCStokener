package net.jwde.util;

import junit.framework.TestCase;
import java.net.*;
import java.io.*;
import net.jwde.util.*;
import jxl.*;
import jxl.read.biff.*;
import org.jdom.*;

public class ExcelHelperTest extends TestCase {

    private Workbook workbook;

    public void setUp() {
        try {
            String excelFile = "result" + java.io.File.separator + "input" + java.io.File.separator + "conextech.xls";
            java.io.File f1 = new java.io.File(excelFile);
            URL url = new URL("file:test/result/input/checksun.xls");
            InputStream is = url.openStream();
            workbook = Workbook.getWorkbook(is);
        } catch (MalformedURLException urlEx) {
            urlEx.printStackTrace();
            fail();
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
            fail();
        } catch (BiffException biffEx) {
            biffEx.printStackTrace();
            fail();
        }
    }

    public void testWriteXML() {
        String result = ExcelHelper.writeXMLString(workbook);
        assertNotNull(result);
        String excelFile = "result" + java.io.File.separator + "output" + java.io.File.separator + "excelTest.xml";
        try {
            java.io.File f1 = new java.io.File(excelFile);
            FileWriter fout = new FileWriter(f1);
            fout.append(result);
            fout.close();
            assertNotNull(XMLHelper.parseXMLFromString(result));
            char c = 100;
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
            fail();
        } catch (XMLHelperException xmlEx) {
            xmlEx.printStackTrace();
            fail();
        }
    }
}
