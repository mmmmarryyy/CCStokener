package org.ensembl.draw.data;

import java.io.*;
import java.util.*;
import java.net.*;
import javax.swing.*;

public class VirtualContig {

    String fileName = "temp.ens";

    String karyotypeFileName = "org/ensembl/draw/data/cytoBand.txt.gz";

    boolean doCaching = false, cacheDirVerified = false;

    public int _global_start, stop;

    public String chrom;

    public VirtualContig() {
        System.out.println("WE ARE USING THIS!");
    }

    public VirtualContig(String chr, int start, int stop, boolean allFeatures) {
        fileName = "temp" + chr + "$" + start + "$" + stop + ".ens";
        chrom = chr;
        _global_start = start;
        this.stop = stop;
        if (doCaching && new File("Cache\\" + fileName).exists()) {
        } else {
            if (!cacheDirVerified) {
                File cacheDir = new File("Cache");
                if (!cacheDir.exists()) {
                    cacheDir.mkdir();
                }
                cacheDirVerified = true;
            }
            int tries = 0;
            for (int i = 0; i < 2; i++) {
                try {
                    tries++;
                    downloadFile(chr, start, stop, allFeatures);
                    break;
                } catch (Exception e) {
                }
            }
            if (tries > 1) {
            }
        }
    }

    public void downloadFile(String chr, int start, int stop, boolean allFeatures) throws IOException {
        chrom = chr;
        _global_start = start;
        this.stop = stop;
        URL url = null;
        try {
            if (allFeatures) url = new URL("http://www.ensembl.org/Homo_sapiens/exportview?tab=embl&out=text&" + "chr=" + chr + "&bp_start=" + start + "&type=basepairs&embl_format=embl&" + "embl_repeat=on&embl_contig=on&" + "ftype=gene&btnsubmit=Export&bp_end=" + stop); else url = new URL("http://www.ensembl.org/Homo_sapiens/exportview?tab=embl&out=text&" + "chr=" + chr + "&bp_start=" + start + "&type=basepairs&embl_format=embl&" + "embl_contig=on&" + "ftype=gene&btnsubmit=Export&bp_end=" + stop);
            if (!cacheDirVerified) {
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            DataOutputStream outstr = new DataOutputStream(new FileOutputStream(fileName));
            String inputLine;
            inputLine = in.readLine();
            if (!inputLine.startsWith("ID")) {
                throw (new IOException("Bad Ensembl Download"));
            }
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.startsWith("SQ")) break;
                outstr.writeBytes(inputLine + "\n");
            }
            in.close();
            outstr.close();
        } catch (Exception exc) {
        }
    }

    public Vector getAllContigs() {
        ContigTable ct = new ContigTable(fileName);
        return ct.contigVector;
    }

    public Vector fetch_karyotype_band_start_end() {
        BandTable bandTable = new BandTable(chrom, karyotypeFileName);
        return bandTable.bandVector;
    }

    public Vector getAllExternalFeatures() {
        getAllContigs();
        Vector v = new Vector();
        int count = 0;
        SNPTable snpTable = new SNPTable(fileName);
        for (int i = 0; i < snpTable.snpVector.size(); i++) {
            SNP aSNP = (SNP) snpTable.snpVector.elementAt(i);
            Variation var = new Variation();
            var.start = aSNP.posContig;
            var.id = aSNP.dbSNP;
            v.add(var);
            count++;
        }
        return v;
    }

    public Vector getAllPredictionFeatures() {
        GeneScanTable genScanTable = new GeneScanTable(fileName);
        return genScanTable.genscanVector;
    }

    public Vector getRepeatFeatures() {
        RepeatTable repeatTable = new RepeatTable(fileName);
        return repeatTable.repeatVector;
    }
}
