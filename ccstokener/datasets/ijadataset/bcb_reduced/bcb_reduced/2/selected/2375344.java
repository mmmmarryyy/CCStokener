package gov.sns.apps.ringmeasurement;

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
 * Read and parse a RingBPMViewer app BPM export file, and export data as an 
 * ArrayList of HashMaps
 *
 * @author  cp3
 */
public class ParseBPMFile {

    HashMap xdatamap = new HashMap();

    HashMap ydatamap = new HashMap();

    HashMap ampdatamap = new HashMap();

    ArrayList data = new ArrayList();

    /** Creates new ParseWireFile */
    public ParseBPMFile() {
    }

    public ArrayList parseFile(File newfile) throws IOException {
        String s;
        String firstname;
        String secondname;
        String direction;
        String header;
        String name = null;
        String[] tokens;
        boolean readingHArrays = false;
        boolean readingVArrays = false;
        boolean readingAArrays = false;
        ArrayList xturndat = new ArrayList();
        ArrayList yturndat = new ArrayList();
        ArrayList ampturndat = new ArrayList();
        int nvalues;
        URL url = newfile.toURI().toURL();
        InputStream is = url.openStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        s = br.readLine();
        s = br.readLine();
        s = br.readLine();
        s = br.readLine();
        s = br.readLine();
        s = br.readLine();
        s = br.readLine();
        while ((s = br.readLine()) != null) {
            tokens = s.split("\\s+");
            nvalues = tokens.length;
            if (nvalues < 1) continue;
            firstname = tokens[0];
            secondname = tokens[1];
            if (secondname.startsWith("BPM")) {
                if (readingHArrays) dumpxData(name, xturndat); else if (readingVArrays) dumpyData(name, yturndat); else if (readingAArrays) dumpampData(name, ampturndat);
                direction = tokens[4];
                if (direction.equals("HORIZONTAL")) {
                    readingHArrays = true;
                    readingVArrays = false;
                    readingAArrays = false;
                }
                if (direction.equals("VERTICAL")) {
                    readingVArrays = true;
                    readingHArrays = false;
                    readingAArrays = false;
                }
                if (direction.equals("AMPLITUDE")) {
                    readingVArrays = false;
                    readingHArrays = false;
                    readingAArrays = true;
                }
                name = tokens[3];
                xturndat.clear();
                yturndat.clear();
                ampturndat.clear();
            }
            if (secondname.startsWith("WAVEFORM")) continue;
            if (nvalues == 3) {
                if (readingHArrays) xturndat.add(new Double(Double.parseDouble(tokens[2]))); else if (readingVArrays) yturndat.add(new Double(Double.parseDouble(tokens[2]))); else if (readingAArrays) ampturndat.add(new Double(Double.parseDouble(tokens[2])));
            }
        }
        dumpampData(name, ampturndat);
        data.add(xdatamap);
        data.add(ydatamap);
        data.add(ampdatamap);
        return data;
    }

    public void dumpxData(String name, ArrayList xturndata) {
        if (xturndata.size() > 0) {
            xdatamap.put(new String(name), new ArrayList(xturndata));
        }
    }

    public void dumpyData(String name, ArrayList yturndata) {
        if (yturndata.size() > 0) {
            ydatamap.put(new String(name), new ArrayList(yturndata));
        }
    }

    public void dumpampData(String name, ArrayList ampturndata) {
        if (ampturndata.size() > 0) {
            ampdatamap.put(new String(name), new ArrayList(ampturndata));
        }
    }
}
