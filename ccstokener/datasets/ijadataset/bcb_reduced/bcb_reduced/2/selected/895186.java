package apollo.datamodel;

import java.util.*;
import java.io.*;
import java.lang.String;
import java.net.*;
import apollo.gui.event.*;
import apollo.gui.*;
import apollo.dataadapter.*;
import apollo.util.*;
import apollo.seq.io.*;

public class SRSSequence extends AbstractLazySequence implements LazySequenceI {

    public SRSSequence(String id, Controller c) {
        super(id, c);
        length = -1;
    }

    public SequenceI getSubseq(int start, int end) {
        return new Sequence(getDisplayId(), getResidues(start, end));
    }

    public int getLength() {
        if (length == -1) {
            length = DataUtil.getLengthFromSRS(getDisplayId());
        }
        return length;
    }

    protected String getResiduesFromSourceImpl(int low, int high) {
        String out = "";
        try {
            String host = "srs.sanger.ac.uk";
            int port = 80;
            String baseId = getDisplayId();
            int dotInd;
            if ((dotInd = baseId.lastIndexOf('.')) > 0) {
                baseId = baseId.substring(0, dotInd);
            }
            String urlstr = "http://" + host + ":" + port + "/srs6bin/cgi-bin/wgetz?-f+AccNumber+-f+Sequence+-sf+fasta+[embl-sv:\"" + getDisplayId() + "\"]|[embl-AccNumber:\"" + getDisplayId() + "\"]|[swall-AccNumber:\"" + baseId + "\"]";
            URL url = new URL(urlstr);
            URLConnection urlconn = url.openConnection();
            DataInputStream in = new DataInputStream(urlconn.getInputStream());
            System.out.println("in = " + in);
            String data;
            try {
                boolean hadId = false;
                while ((data = in.readLine()) != null) {
                    data = HTMLUtil.removeHtml(data);
                    if (data.indexOf("AC ") == 0) {
                        if (data.indexOf(baseId) > -1) {
                            data = in.readLine();
                        }
                        out += data + "\n";
                        ;
                        hadId = true;
                    } else if (hadId) {
                        out += data + "\n";
                        ;
                    }
                    System.out.println("Dehtmledline = " + data);
                }
            } catch (IOException ioex) {
                System.out.println("Exception " + ioex);
            }
            in.close();
        } catch (MalformedURLException ex) {
            System.out.println("Exception " + ex);
        } catch (IOException ioex) {
            System.out.println("Exception " + ioex);
        } catch (Exception ex) {
            System.out.println("Exception " + ex);
            ex.printStackTrace();
        }
        System.out.println("FASTA string = " + out);
        FastaFile fa = new FastaFile(out);
        return ((SequenceI) fa.seqs.elementAt(0)).getResidues(low + 1, high + 1);
    }

    public static void main(String[] argv) {
        Controller c = new Controller();
        SRSSequence seq = new SRSSequence("AK001640", c);
        seq.getCacher().setMinChunkSize(1000);
        System.out.println("Sequence first 10 = " + seq.getResidues(1, 10));
        System.out.println("Sequence next 10 = " + seq.getResidues(11, 20));
        System.out.println("Sequence 100-110 = " + seq.getResidues(100, 110));
    }
}
