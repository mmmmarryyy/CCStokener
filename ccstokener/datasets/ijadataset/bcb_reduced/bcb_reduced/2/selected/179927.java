package com.dcivision.dms.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
  ASPWordTextExtractor.java

  This class is the text extractor for Word files.

    @author          Rollo Chan
    @company         DCIVision Limited
    @creation date   26/08/2003
    @version         $Revision: 1.6.32.4 $
*/
public class ASPWordTextExtractor extends AbstractTextExtractor {

    public static final String REVISION = "$Revision: 1.6.32.4 $";

    public static String CONTEXT_PATH = "http://localhost:9000/asp";

    private static final Log log = LogFactory.getLog(ASPWordTextExtractor.class);

    public ASPWordTextExtractor(InputStream fis, String encoding) {
        super(fis, encoding);
        if (fis == null) {
            return;
        }
        java.io.InputStreamReader isr = null;
        java.net.HttpURLConnection urlConn = null;
        try {
            java.io.File file = java.io.File.createTempFile("DCIVISION", ".doc");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            int tmpInt = 0;
            while ((tmpInt = fis.read()) != -1) {
                fos.write(tmpInt);
            }
            fos.close();
            String strURL = CONTEXT_PATH + "/WordExtractor.asp?fileLocation=" + java.net.URLEncoder.encode(file.getAbsolutePath(), encoding);
            log.debug("strURL:" + strURL);
            java.net.URL thisURL = new java.net.URL(strURL);
            urlConn = (java.net.HttpURLConnection) thisURL.openConnection();
            urlConn.connect();
            isr = new java.io.InputStreamReader(urlConn.getInputStream(), encoding);
            Html2Text parser = new Html2Text();
            parser.parse(isr);
            text = parser.getText();
            file.delete();
        } catch (Exception e) {
            log.error(e, e);
        } finally {
            try {
                isr.close();
            } catch (Exception ignore) {
            } finally {
                isr = null;
            }
            try {
                urlConn.disconnect();
            } catch (Exception ignore) {
            } finally {
                urlConn = null;
            }
        }
    }
}

class Html2Text extends HTMLEditorKit.ParserCallback {

    StringBuffer s;

    public Html2Text() {
    }

    public void parse(Reader in) throws IOException {
        s = new StringBuffer();
        ParserDelegator delegator = new ParserDelegator();
        delegator.parse(in, this, true);
    }

    public void handleText(char[] text, int pos) {
        s.append(text);
    }

    public String getText() {
        return s.toString();
    }
}
