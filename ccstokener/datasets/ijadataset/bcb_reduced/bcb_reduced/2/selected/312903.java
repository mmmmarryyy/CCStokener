package org.jcvi.glk.elvira.flu;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jcvi.Range;
import org.jcvi.assembly.ace.AceContig;
import org.jcvi.assembly.ace.AceParser;
import org.jcvi.assembly.ace.AcePlacedRead;
import org.jcvi.fasta.FastaRecord;
import org.jcvi.glyph.EncodedGlyphs;
import org.jcvi.glyph.encoder.RunLengthEncodedGlyphCodec;
import org.jcvi.glyph.nuc.NucleotideGlyph;
import org.jcvi.glyph.nuc.NucleotideGlyphFactory;
import org.jcvi.glyph.phredQuality.PhredQuality;
import org.jcvi.trace.MultiTrace;
import org.jcvi.trace.TraceDecoderException;
import org.jcvi.trace.fourFiveFour.flowgram.Flowgram;
import org.jcvi.trace.fourFiveFour.flowgram.sff.DefaultSFFCommonHeaderCodec;
import org.jcvi.trace.fourFiveFour.flowgram.sff.DefaultSFFReadDataCodec;
import org.jcvi.trace.fourFiveFour.flowgram.sff.DefaultSFFReadHeaderCodec;
import org.jcvi.trace.fourFiveFour.flowgram.sff.SFFCodec;

public class FluNCBIUtil {

    private static final String NCBI_URL = "http://www.ncbi.nlm.nih.gov/genomes/FLU/Database/annotation.cgi";

    private static final String UTF_8 = "UTF-8";

    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";

    public static InputStream annotateFluSegment(FastaRecord<EncodedGlyphs<NucleotideGlyph>> fasta) throws IOException {
        URLConnection connection = connectToNCBIValidator();
        Writer writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(generateNCBIPostData(fasta));
        writer.flush();
        writer.close();
        return connection.getInputStream();
    }

    private static URLConnection connectToNCBIValidator() throws IOException {
        final URL url = new URL(NCBI_URL);
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestProperty("Content-Type", CONTENT_TYPE);
        return connection;
    }

    private static String generateNCBIPostData(FastaRecord<EncodedGlyphs<NucleotideGlyph>> fasta) throws IOException {
        StringBuilder result = new StringBuilder();
        postPair(result, "upfile", "");
        result.append("&");
        postPair(result, "SUBMIT", "Annotate FASTA");
        result.append("&");
        postPair(result, "sequence", fasta.toString());
        return result.toString();
    }

    private static void postPair(StringBuilder result, String key, String value) throws IOException {
        result.append(key);
        result.append("=");
        result.append(encode(value));
    }

    private static String encode(String string) throws IOException {
        return URLEncoder.encode(string, UTF_8);
    }

    public static void main(String args[]) throws IOException, TraceDecoderException {
        File rootDir = new File("/usr/local/projects/454POSTSEQ/VI_454_Workorder/bz252_20080627_Flu_Barcoding/newbler_mappings");
        NucleotideGlyphFactory glyphFactory = NucleotideGlyphFactory.getInstance();
        Pattern barcodePattern = Pattern.compile("\\w+_(\\w+)");
        for (File subDir : rootDir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return dir.isDirectory();
            }
        })) {
            Matcher matcher = barcodePattern.matcher(subDir.getName());
            if (matcher.find()) {
                String acePath = subDir.getAbsolutePath() + "/mapping/454Contigs.ace";
                String sffPath = subDir.getAbsolutePath() + "/sff/" + matcher.group(1) + ".sff";
                System.out.println(subDir.getAbsolutePath());
                Pattern segmentPattern = Pattern.compile("(\\w+)-(\\w+)");
                AceParser aceParser = new AceParser(new FileInputStream(acePath));
                SFFCodec codec = new SFFCodec(new DefaultSFFCommonHeaderCodec(), new DefaultSFFReadHeaderCodec(), new DefaultSFFReadDataCodec(), new RunLengthEncodedGlyphCodec(PhredQuality.MAX_VALUE));
                MultiTrace<Flowgram> flowgramMap = codec.decode(new DataInputStream(new FileInputStream(sffPath)));
                for (AceContig contig : aceParser.parseContigsFrom()) {
                    System.out.println(contig.getId() + " size = " + contig.getConsensus().getLength());
                    for (AcePlacedRead read : contig.getPlacedReads()) {
                        if (!flowgramMap.contains(read.getId())) {
                            System.out.println("\tskipping... " + read.getId());
                            System.out.println("\t" + Range.buildRangeOfLength(read.getStart(), read.getLength()));
                        }
                    }
                }
                System.exit(0);
            }
        }
    }
}
