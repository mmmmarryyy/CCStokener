package gov.sns.apps.escape;

import gov.sns.tools.*;
import gov.sns.xal.smf.*;
import gov.sns.xal.smf.impl.*;
import gov.sns.ca.*;
import gov.sns.tools.messaging.*;
import gov.sns.tools.statistics.*;
import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * Read and parse a wirescan file, extract data and send out as HashMap.
 *
 * @author  cp3
 */
public class ParseScannerFile {

    public ArrayList data = new ArrayList();

    /** Creates new ParseWireFile */
    public ParseScannerFile() {
    }

    public ArrayList parseFile(File newfile) throws IOException {
        String s;
        String[] tokens;
        URL url = newfile.toURI().toURL();
        InputStream is = url.openStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        while ((s = br.readLine()) != null) {
            tokens = s.split("\\s+");
            int nvalues = tokens.length;
            ArrayList columndata = new ArrayList();
            for (int i = 0; i < nvalues; i++) {
                if (((String) tokens[i]).length() > 0) {
                    columndata.add(new Double(Double.parseDouble(tokens[i])));
                }
            }
            data.add(columndata);
        }
        System.out.println("Matrix size is " + data.size() + " by " + ((ArrayList) data.get(0)).size());
        return data;
    }
}
