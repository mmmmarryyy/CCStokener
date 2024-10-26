package org.lindenb.tinytools;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StreamTokenizer;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.lindenb.bio.Chromosome;
import org.lindenb.bio.GeneticCode;
import org.lindenb.bio.NucleotideUtils;
import org.lindenb.bio.Strand;
import org.lindenb.io.IOUtils;
import org.lindenb.lang.IllegalInputException;
import org.lindenb.me.Me;
import org.lindenb.util.Cast;
import org.lindenb.util.Compilation;
import org.lindenb.xml.XMLUtilities;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 * Consequences
 * java -cp mysql-connector-java-5.1.6-bin.jar:consequences.jar org.lindenb.tinytools.Consequences
 */
public class Consequences {

    private static final String DEFAULT_HG = "hg18";

    private List<KnownGene> knownGenes = new ArrayList<KnownGene>();

    private List<BaseChange> mutations = new ArrayList<BaseChange>();

    private Chromosome chromosome = null;

    private int minGenomicPos = Integer.MAX_VALUE;

    private int maxGenomicPos = 0;

    private GenomicSequence genomicSequence = null;

    private boolean find_rs_number = false;

    private String genomeVersion = DEFAULT_HG;

    /**
     * GenomicSequence
     */
    static class GenomicSequence {

        private int start;

        private int end;

        private byte[] sequence;

        private GenomicSequence() {
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public int length() {
            return end - start;
        }

        public char charAt(int genomicIndex) {
            if (genomicIndex < start) return '?';
            if (genomicIndex >= end) return '?';
            return (char) sequence[genomicIndex - start];
        }

        @Override
        public String toString() {
            return new String(this.sequence);
        }
    }

    /** DASHandler */
    private static class DASHandler extends DefaultHandler {

        ByteArrayOutputStream bytes = null;

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
            if (name.equals("DNA") && bytes == null) {
                bytes = new ByteArrayOutputStream();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (bytes == null) return;
            for (int i = 0; i < length; ++i) {
                char c = Character.toUpperCase(ch[start + i]);
                if (Character.isWhitespace(c)) continue;
                bytes.write((byte) c);
            }
        }
    }

    /**
     * BaseChange
     *
     */
    static class BaseChange {

        private String name;

        private int position;

        private char base;

        private String rs = null;

        public BaseChange(String name, int position, char base) {
            this.name = name;
            this.position = position;
            this.base = Character.toUpperCase(base);
        }

        public String getName() {
            return name;
        }

        public int getPosition() {
            return position;
        }

        public char getBase() {
            return base;
        }

        @Override
        public String toString() {
            return "" + getBase() + " (" + getPosition() + ")";
        }
    }

    private static class Shuttle {

        KnownGene gene;

        boolean in_utr5 = false;

        boolean in_utr3 = false;

        Exon left_exon_for_in_intron = null;

        Exon right_exon_for_in_intron = null;

        Exon exon = null;

        BaseChange baseChange;

        StringBuilder cDNA = new StringBuilder();

        StringBuilder protein = new StringBuilder();

        StringBuilder codonWild = new StringBuilder();

        StringBuilder codonMut = new StringBuilder();

        String codon_wild = null;

        String codon_mut = null;

        char aaWild = '\0';

        char aaMut = '\0';

        int index_in_cdna = -1;

        int index_in_protein = -1;

        void print(PrintStream out) {
            if (in_utr5) {
                out.println("<in-utr-5/>");
                return;
            }
            if (in_utr3) {
                out.println("<in-utr-3/>");
                return;
            }
            if (left_exon_for_in_intron != null) {
                out.print("<in-intron ");
                if (gene.isForward()) {
                    out.print(" name=\"Intron " + (1 + left_exon_for_in_intron.arrayIndex) + "\"");
                } else {
                    out.print(" name=\"Intron " + (gene.getExonCount() - right_exon_for_in_intron.arrayIndex) + "\"");
                }
                out.print(" intron-start=\"" + left_exon_for_in_intron.getEnd() + "\"");
                out.print(" intron-end=\"" + right_exon_for_in_intron.getStart() + "\"");
                out.println("/>");
                return;
            }
            out.print("<in-exon");
            if (gene.isForward()) {
                out.print(" name=\"Exon " + (1 + exon.arrayIndex) + "\"");
            } else {
                out.print(" name=\"Exon " + (gene.getExonCount() - exon.arrayIndex) + "\"");
            }
            out.print(" codon-wild=\"" + codon_wild + "\" ");
            out.print(" codon-mut=\"" + codon_mut + "\" ");
            out.print(" aa-wild=\"" + aaWild + "\" ");
            out.print(" aa-mut=\"" + aaMut + "\" ");
            out.print(" base-wild=\"" + this.cDNA.charAt(index_in_cdna) + "\" ");
            out.print(" base-mut=\"" + (gene.isForward() ? baseChange.getBase() : NucleotideUtils.complement(baseChange.getBase())) + "\" ");
            out.print(" index-cdna=\"" + index_in_cdna + "\" ");
            out.print(" index-protein=\"" + index_in_protein + "\" ");
            out.println(">");
            if (!codon_wild.equals(codon_mut)) {
                String s = this.cDNA.toString();
                out.println("<wild-cDNA>" + s.substring(0, index_in_cdna) + " " + s.charAt(index_in_cdna) + " " + s.substring(index_in_cdna + 1) + "</wild-cDNA>");
                out.println("<mut-cDNA >" + s.substring(0, index_in_cdna) + " " + (gene.isForward() ? baseChange.getBase() : NucleotideUtils.complement(baseChange.getBase())) + " " + s.substring(index_in_cdna + 1) + "</mut-cDNA>");
                if (aaWild != aaMut) {
                    s = this.protein.toString();
                    out.println("<wild-protein>" + s.substring(0, index_in_protein) + " " + aaWild + " " + s.substring(index_in_protein + 1) + "</wild-protein>");
                    out.println("<mut-protein >" + s.substring(0, index_in_protein) + " " + aaMut + " " + s.substring(index_in_protein + 1) + "</mut-protein>");
                }
            }
            out.print("</in-exon");
            out.println(">");
        }

        @Override
        public String toString() {
            return "in_utr5=" + in_utr5 + "\n" + "in_utr3=" + in_utr3 + "\n" + "change=" + baseChange + "\n" + "cDNA=" + cDNA + "\n" + "protein=" + protein + "\n" + "codon_wild=" + codon_wild + "\n" + "codon_mut=" + codon_mut + "\n" + "aaWild=" + aaWild + "\n" + "aaMut=" + aaMut + "\n" + "index_in_cdna=" + index_in_cdna + "\n" + "index_in_protein=" + index_in_protein + "\n";
        }
    }

    /**
     * Exon
     */
    private abstract class Exon {

        private int arrayIndex;

        Exon(int arrayIndex) {
            this.arrayIndex = arrayIndex;
        }

        public int getStart() {
            return getKnownGene().exonStart(this.arrayIndex);
        }

        public int getEnd() {
            return getKnownGene().exonEnd(this.arrayIndex);
        }

        @SuppressWarnings("unused")
        public int length() {
            return getEnd() - getStart();
        }

        public int getIndex1() {
            return getKnownGene().getStrand() == Strand.PLUS ? arrayIndex + 1 : getKnownGene().getExonCount() - arrayIndex;
        }

        public String getName() {
            return "Exon " + getIndex1();
        }

        public abstract KnownGene getKnownGene();

        public void challenge(Shuttle shuttle) {
            if (getKnownGene().isForward()) {
                for (int i = getStart(); i < getEnd(); ++i) {
                    if (i < getKnownGene().getCdsStart()) continue;
                    if (getKnownGene().getCdsEnd() <= i) continue;
                    char base = getKnownGene().getGenomicSequence().charAt(i);
                    shuttle.codonWild.append(base);
                    if (i == shuttle.baseChange.getPosition()) {
                        shuttle.exon = this;
                        shuttle.codonMut.append(shuttle.baseChange.getBase());
                        shuttle.index_in_cdna = shuttle.cDNA.length();
                        shuttle.index_in_protein = shuttle.protein.length();
                    } else {
                        shuttle.codonMut.append(base);
                    }
                    shuttle.cDNA.append(base);
                    if (shuttle.codonWild.length() == 3) {
                        char aa = GeneticCode.getStandard().translate(shuttle.codonWild.charAt(0), shuttle.codonWild.charAt(1), shuttle.codonWild.charAt(2));
                        if (shuttle.index_in_protein == shuttle.protein.length()) {
                            shuttle.codon_mut = shuttle.codonMut.toString();
                            shuttle.codon_wild = shuttle.codonWild.toString();
                            shuttle.aaWild = aa;
                            shuttle.aaMut = GeneticCode.getStandard().translate(shuttle.codonMut.charAt(0), shuttle.codonMut.charAt(1), shuttle.codonMut.charAt(2));
                        }
                        shuttle.protein.append(aa);
                        shuttle.codonWild.setLength(0);
                        shuttle.codonMut.setLength(0);
                    }
                }
            } else {
                for (int i = getEnd() - 1; i >= getStart(); --i) {
                    if (i < getKnownGene().getCdsStart()) continue;
                    if (getKnownGene().getCdsEnd() <= i) continue;
                    char base_compl = NucleotideUtils.complement(getKnownGene().getGenomicSequence().charAt(i));
                    shuttle.codonWild.append(base_compl);
                    if (i == shuttle.baseChange.getPosition()) {
                        shuttle.exon = this;
                        shuttle.codonMut.append(NucleotideUtils.complement(shuttle.baseChange.getBase()));
                        shuttle.index_in_cdna = shuttle.cDNA.length();
                        shuttle.index_in_protein = shuttle.protein.length();
                    } else {
                        shuttle.codonMut.append(base_compl);
                    }
                    shuttle.cDNA.append(base_compl);
                    if (shuttle.codonWild.length() == 3) {
                        char aa = GeneticCode.getStandard().translate(shuttle.codonWild.charAt(0), shuttle.codonWild.charAt(1), shuttle.codonWild.charAt(2));
                        if (shuttle.index_in_protein == shuttle.protein.length()) {
                            shuttle.codon_mut = shuttle.codonMut.toString();
                            shuttle.codon_wild = shuttle.codonWild.toString();
                            shuttle.aaWild = aa;
                            shuttle.aaMut = GeneticCode.getStandard().translate(shuttle.codonMut.charAt(0), shuttle.codonMut.charAt(1), shuttle.codonMut.charAt(2));
                        }
                        shuttle.protein.append(aa);
                        shuttle.codonWild.setLength(0);
                        shuttle.codonMut.setLength(0);
                    }
                }
            }
        }

        @Override
        public String toString() {
            return getName() + " " + getStart() + " " + getEnd();
        }
    }

    private class KnownGene {

        private String name;

        private Strand strand;

        private int txStart;

        private int txEnd;

        private int cdsStart;

        private int cdsEnd;

        private int exonsStarts[];

        private int exonsEnds[];

        private KnownGene(ResultSet row) throws SQLException, IOException {
            this.name = row.getString(1);
            this.strand = Strand.newInstance(row.getString(2).charAt(0));
            this.txStart = row.getInt(3);
            this.txEnd = row.getInt(4);
            this.cdsStart = row.getInt(5);
            this.cdsEnd = row.getInt(6);
            int exonCount = row.getInt(7);
            this.exonsStarts = new int[exonCount];
            this.exonsEnds = new int[exonCount];
            for (int side = 0; side < 2; ++side) {
                int index = 0;
                Blob blob = row.getBlob(8 + side);
                StreamTokenizer st = new StreamTokenizer(new InputStreamReader(blob.getBinaryStream()));
                st.parseNumbers();
                while (st.nextToken() != StreamTokenizer.TT_EOF) {
                    switch(st.ttype) {
                        case StreamTokenizer.TT_NUMBER:
                            {
                                if (side == 0) {
                                    exonsStarts[index] = (int) st.nval;
                                } else {
                                    exonsEnds[index] = (int) st.nval;
                                }
                                break;
                            }
                        case ',':
                            ++index;
                            break;
                    }
                }
                blob.free();
            }
        }

        public int getTxStart() {
            return txStart;
        }

        public int getTxEnd() {
            return txEnd;
        }

        public int getCdsStart() {
            return cdsStart;
        }

        public int getCdsEnd() {
            return cdsEnd;
        }

        public String getName() {
            return name;
        }

        public Strand getStrand() {
            return strand;
        }

        int exonStart(int index) {
            return this.exonsStarts[index];
        }

        int exonEnd(int index) {
            return this.exonsEnds[index];
        }

        public int getExonCount() {
            return this.exonsStarts.length;
        }

        public GenomicSequence getGenomicSequence() {
            return Consequences.this.genomicSequence;
        }

        public Exon getExon(int index) {
            return new Exon(index) {

                @Override
                public KnownGene getKnownGene() {
                    return KnownGene.this;
                }
            };
        }

        boolean isForward() {
            return getStrand() == Strand.PLUS;
        }

        public Shuttle challenge(BaseChange bc) {
            Shuttle shuttle = new Shuttle();
            shuttle.baseChange = bc;
            shuttle.gene = this;
            if (isForward()) {
                if (bc.getPosition() < getCdsStart()) {
                    shuttle.in_utr5 = true;
                }
                if (getCdsEnd() <= bc.getPosition()) {
                    shuttle.in_utr3 = true;
                }
                for (int i = 0; i < getExonCount(); ++i) {
                    Exon ex = getExon(i);
                    if (i + 1 < getExonCount()) {
                        Exon nextExon = getExon(i + 1);
                        if (ex.getEnd() <= bc.getPosition() && bc.getPosition() < nextExon.getStart()) {
                            shuttle.left_exon_for_in_intron = ex;
                            shuttle.right_exon_for_in_intron = nextExon;
                        }
                    }
                    ex.challenge(shuttle);
                }
            } else {
                if (bc.getPosition() < getCdsStart()) {
                    shuttle.in_utr3 = true;
                }
                if (getCdsEnd() <= bc.getPosition()) {
                    shuttle.in_utr5 = true;
                }
                for (int i = getExonCount() - 1; i >= 0; --i) {
                    Exon ex = getExon(i);
                    if (i > 0) {
                        Exon nextExon = getExon(i - 1);
                        if (nextExon.getEnd() <= bc.getPosition() && bc.getPosition() < ex.getStart()) {
                            shuttle.left_exon_for_in_intron = nextExon;
                            shuttle.right_exon_for_in_intron = ex;
                        }
                    }
                    ex.challenge(shuttle);
                }
            }
            return shuttle;
        }
    }

    private Consequences() {
    }

    private void challenge(PrintStream out) {
        out.println("<consequences chrom='" + this.chromosome + "\'>");
        for (BaseChange bc : this.mutations) {
            out.print("<observed-mutation position='" + bc.getPosition() + "\' " + " name='" + XMLUtilities.escape(bc.getName()) + "' " + " base='" + bc.getBase() + "'");
            if (find_rs_number && bc.rs != null) {
                out.print(" rs=\"" + bc.rs + "\"");
            }
            out.println(">");
            for (KnownGene kg : this.knownGenes) {
                if (bc.getPosition() < kg.getTxStart()) continue;
                if (kg.getTxEnd() <= bc.getPosition()) continue;
                out.println("<gene name='" + XMLUtilities.escape(kg.getName()) + "'" + " exon-count='" + kg.getExonCount() + "' " + " strand='" + kg.getStrand() + "' " + " txStart='" + kg.getTxStart() + "' " + " txEnd='" + kg.getTxEnd() + "'" + " cdsStart='" + kg.getCdsStart() + "' " + " cdsEnd='" + kg.getCdsEnd() + "'" + " >");
                Shuttle shuttle = kg.challenge(bc);
                shuttle.print(out);
                out.println("</gene>");
            }
            out.println("</observed-mutation>");
        }
        out.println("</consequences>");
    }

    private void fetchFasta() throws IOException {
        this.genomicSequence = fetch(this.chromosome, this.minGenomicPos, this.maxGenomicPos);
    }

    private void fetchGenes() throws IOException, SQLException {
        Connection con = null;
        try {
            con = DriverManager.getConnection("jdbc:mysql://genome-mysql.cse.ucsc.edu/" + genomeVersion + "?user=genome&password=");
            PreparedStatement pstmt = con.prepareStatement("select distinct name,strand,txStart,txEnd,cdsStart,cdsEnd,exonCount,exonStarts,exonEnds " + " from knownGene " + " where chrom=? and not(txEnd<? or ?<txStart)");
            pstmt.setString(1, this.chromosome.toString());
            pstmt.setLong(2, minGenomicPos);
            pstmt.setLong(3, maxGenomicPos);
            ResultSet row = pstmt.executeQuery();
            while (row.next()) {
                KnownGene gene = new KnownGene(row) {

                    @Override
                    public GenomicSequence getGenomicSequence() {
                        return Consequences.this.genomicSequence;
                    }
                };
                knownGenes.add(gene);
            }
            pstmt.close();
            for (KnownGene kg : this.knownGenes) {
                this.minGenomicPos = Math.min(this.minGenomicPos, kg.getTxStart());
                this.maxGenomicPos = Math.max(this.maxGenomicPos, kg.getTxEnd() + 1);
            }
            if (find_rs_number) {
                try {
                    pstmt = con.prepareStatement("select distinct name " + " from snp129 " + " where chrom=? and chromStart=? limit 1");
                    pstmt.setString(1, this.chromosome.toString());
                    for (BaseChange bc : this.mutations) {
                        pstmt.setInt(2, bc.getPosition());
                        row = pstmt.executeQuery();
                        while (row.next()) {
                            bc.rs = row.getString(1);
                        }
                        row.close();
                    }
                    pstmt.close();
                } catch (SQLException sqlerr) {
                    System.err.println("Couldn't get the name of the snp :" + sqlerr.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (con != null) con.close();
        }
    }

    private void readMutations(BufferedReader in) throws IOException {
        Pattern TAB = Pattern.compile("[\t]");
        String line;
        while ((line = in.readLine()) != null) {
            if (line.trim().length() == 0 || line.startsWith("#")) continue;
            String tokens[] = TAB.split(line);
            if (tokens.length < 4) throw new org.lindenb.lang.IllegalTokenCount(4, tokens);
            if (tokens[0].trim().length() == 0) throw new IllegalInputException("empty name " + line + " : " + tokens[0]);
            if (!Cast.UInteger.isA(tokens[2])) throw new IllegalInputException("not a base position in " + line + " : " + tokens[2]);
            if (!tokens[3].matches("[ATGCatgc]")) throw new IllegalInputException("not a base in " + line);
            Chromosome k = Chromosome.newInstance(tokens[1]);
            if (chromosome == null) {
                this.chromosome = k;
            } else if (!chromosome.equals(k)) {
                throw new IllegalInputException("Expected only one chromosome but found " + this.chromosome + " and " + k);
            }
            BaseChange bc = new BaseChange(tokens[0], Cast.UInteger.cast(tokens[2]), tokens[3].charAt(0));
            this.mutations.add(bc);
            this.minGenomicPos = Math.min(this.minGenomicPos, bc.getPosition());
            this.maxGenomicPos = Math.max(this.maxGenomicPos, bc.getPosition() + 1);
        }
    }

    private GenomicSequence fetch(Chromosome k, int start, int end) throws IOException {
        try {
            String chr = k.toString();
            if (chr.toLowerCase().startsWith("chr")) chr = chr.substring(3);
            SAXParserFactory f = SAXParserFactory.newInstance();
            f.setNamespaceAware(false);
            f.setValidating(false);
            SAXParser parser = f.newSAXParser();
            URL url = new URL("http://genome.ucsc.edu/cgi-bin/das/" + genomeVersion + "/dna?segment=" + URLEncoder.encode(chr, "UTF-8") + ":" + (start + 1) + "," + (end));
            DASHandler handler = new DASHandler();
            InputStream in = url.openStream();
            parser.parse(in, handler);
            in.close();
            GenomicSequence seq = new GenomicSequence();
            seq.sequence = handler.bytes.toByteArray();
            seq.start = start;
            seq.end = end;
            if (seq.sequence.length != seq.length()) throw new IOException("bad bound " + seq + " " + seq.sequence.length + " " + seq.length());
            return seq;
        } catch (IOException err) {
            throw err;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static void main(String[] args) {
        Consequences app = new Consequences();
        try {
            int optind = 0;
            while (optind < args.length) {
                if (args[optind].equals("-h")) {
                    System.err.println(Compilation.getLabel());
                    System.err.println("Pierre Lindenbaum PhD " + Me.MAIL + " " + Me.WWW);
                    System.err.println("Find the consequences (mutation of proteins) of a mutation on the genome: download the genomic sequence of the " + " genome using the UCSC DAS Server, and download the structure of the genes from the UCSC mysql server ");
                    System.err.println("-h this screen");
                    System.err.println("-rs find the rs### of your snp in snp129 (if any)");
                    System.err.println("-hg <string> set the genome version : default is " + DEFAULT_HG);
                    System.err.println("<stdin>| files");
                    System.err.println("\n\nInput is a tab delimited file containing the following fields:" + "\n 1) snp name." + "\n 2) chrom e.g. \'chr2\' Note: at this time only one chromosome per input is supported." + "\n 3) position in the genome. First base is 0." + "\n 4) base observed *ON THE PLUS STRAND OF THE GENOME* ");
                    System.err.println();
                    return;
                } else if (args[optind].equals("-rs")) {
                    app.find_rs_number = true;
                } else if (args[optind].equals("-hg")) {
                    app.genomeVersion = args[++optind];
                } else if (args[optind].equals("--")) {
                    ++optind;
                    break;
                } else if (args[optind].startsWith("-")) {
                    System.err.println("bad argument " + args[optind]);
                    System.exit(-1);
                } else {
                    break;
                }
                ++optind;
            }
            Class.forName("com.mysql.jdbc.Driver");
            if (optind == args.length) {
                app.readMutations(new BufferedReader(new InputStreamReader(System.in)));
            } else {
                while (optind < args.length) {
                    BufferedReader in = IOUtils.openFile(new File(args[optind++]));
                    app.readMutations(in);
                    in.close();
                }
            }
            app.chromosome = Chromosome.newInstance(1);
            if (app.chromosome == null || app.mutations.isEmpty()) {
                System.err.println("No Input");
                return;
            }
            app.fetchGenes();
            app.fetchFasta();
            app.challenge(System.out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
