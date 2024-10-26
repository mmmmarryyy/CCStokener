package apollo.analysis;

import java.util.*;
import java.net.URL;
import java.io.*;
import java.awt.event.*;
import org.bdgp.io.*;
import apollo.analysis.SeqAnalysisBase;
import apollo.datamodel.*;

public class SeqAnalysisBDGP extends SeqAnalysisRemote {

    public SeqAnalysisBDGP() {
        super();
        initSeqAnalysisBDGP();
    }

    public SeqAnalysisBDGP(Hashtable inproperties) {
        super(inproperties);
        initSeqAnalysisBDGP();
    }

    public void initSeqAnalysisBDGP() {
        Vector v = new Vector();
        v.addElement("programName");
        v.addElement("programParams");
        setAllowedProperties(v);
        Vector progs = new Vector();
        progs.addElement("sim4wrap");
        progs.addElement("blastp");
        progs.addElement("interpro");
        progs.addElement("full peptide analysis");
        progs.addElement("full genomic analysis");
        setAllowedValues("programName", progs);
    }

    public DataAdapterUI getUI(IOOperation op) {
        return new SeqAnalysisGUI(this, op);
    }

    public boolean launch() {
        try {
            String path = "http://www.fruitfly.org/cgi-bin/annot/launch.pl?seqid=" + getInputSequence().getName() + "&residues=" + getInputSequence().getResidues();
            URL url = new URL(path);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String ticket = in.readLine();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getName() {
        return "SeqAnalysisBDGP";
    }

    public String getType() {
        return "BDGP beowulf executed analysis";
    }
}
