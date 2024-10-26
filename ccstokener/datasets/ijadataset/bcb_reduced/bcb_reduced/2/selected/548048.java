package bop.parser;

import java.io.*;
import java.util.*;
import java.net.URL;
import bop.datamodel.*;
import bop.parser.*;
import bop.exception.OutputMalFormatException;

public class tRNA_Parser extends ResultParser {

    String program = "";

    public tRNA_Parser() {
    }

    public String getProgram() {
        return program;
    }

    public boolean parseResults(URL url, String analysis_type, CurationI curation, Date analysis_date, String regexp) throws OutputMalFormatException {
        boolean parsed = false;
        try {
            InputStream data_stream = url.openStream();
            parsed = parseResults(data_stream, analysis_type, curation, analysis_date, regexp);
        } catch (OutputMalFormatException ex) {
            throw new OutputMalFormatException(ex.getMessage(), ex);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return parsed;
    }

    public boolean parseResults(InputStream data_stream, String analysis_type, CurationI curation, Date analysis_date, String regexp) throws OutputMalFormatException {
        boolean parsed = false;
        try {
            BufferedReader data = new BufferedReader(new InputStreamReader(data_stream));
            String line = data.readLine();
            if (line != null && line.startsWith("Sequence")) {
                program = "tRNAscan-SE";
                AnalysisI analysis = curation.getAnalysis(analysis_type);
                boolean new_analysis = (analysis == null);
                if (new_analysis) {
                    analysis = new Analysis();
                    analysis.setType(analysis_type);
                }
                analysis.setProgram(program);
                analysis.setDate(analysis_date);
                line = data.readLine();
                line = data.readLine();
                while ((line = data.readLine()) != null) {
                    line = line.trim();
                    ResultSet tRNA = grab_tRNA(curation, analysis, line, regexp, new_analysis);
                    if (tRNA != null) {
                        analysis.addResult(tRNA);
                        parsed = true;
                    }
                }
            }
            data.close();
        } catch (OutputMalFormatException ex) {
            throw new OutputMalFormatException(ex.getMessage(), ex);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return parsed;
    }

    private ResultSet grab_tRNA(CurationI curation, AnalysisI analysis, String line, String regexp, boolean new_analysis) throws OutputMalFormatException {
        String value = "";
        StringTokenizer tokens;
        int i;
        ResultSet tRNA = new ResultSet();
        SequenceI seq = analysis.getSequence();
        ResultSpan span;
        boolean valid = true;
        tokens = new StringTokenizer(line);
        span = new ResultSpan();
        try {
            if (valid &= tokens.hasMoreElements()) {
                String seq_name = tokens.nextToken();
                if (seq == null) {
                    seq = new Sequence();
                    bop.parser.FastaParser.parseHeader(seq, seq_name);
                    try {
                        RegexpAdapter ad = new RegexpAdapter(regexp, seq_name);
                        String id = ad.group(1);
                        seq.setID(id);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                    analysis.setSequence(seq);
                    if (new_analysis) {
                        curation.addAnalysis(analysis);
                        System.out.println("Created new analysis for " + analysis.getType());
                    }
                }
                tRNA.setSequence(seq);
            }
            if (valid &= tokens.hasMoreElements()) tokens.nextToken();
            if (valid &= tokens.hasMoreElements()) {
                span.setSeqStart(tokens.nextToken());
            }
            if (valid &= tokens.hasMoreElements()) {
                span.setSeqEnd(tokens.nextToken());
            }
            if (valid &= tokens.hasMoreElements()) {
                tRNA.setOutput("amino-acid", tokens.nextToken());
            }
            if (valid &= tokens.hasMoreElements()) tRNA.setOutput("anti-codon", tokens.nextToken());
            if (valid &= tokens.hasMoreElements()) {
                tokens.nextToken();
            }
            if (valid &= tokens.hasMoreElements()) {
                tokens.nextToken();
            }
            if (valid &= tokens.hasMoreElements()) {
                span.setScore(tokens.nextToken());
                tRNA.setScore(span.getScore());
            }
        } catch (IllegalArgumentException ex) {
            throw new OutputMalFormatException("Error passing line " + line, ex.getMessage(), ex);
        }
        if (valid) {
            tRNA.addSpan(span);
            return (tRNA);
        } else {
            return null;
        }
    }

    public boolean canParse(String file_name) {
        boolean parseable = false;
        try {
            FileInputStream istream = new FileInputStream(file_name);
            parseable = canParse(istream);
        } catch (Exception ex) {
            System.out.println("Couldn't verify " + file_name + " as tRNA-scanSE data");
        }
        return parseable;
    }

    public boolean canParse(InputStream in) {
        boolean parseable = false;
        try {
            BufferedReader data = new BufferedReader(new InputStreamReader(in));
            String line = data.readLine();
            parseable = (line != null && line.startsWith("Sequence"));
            data.close();
        } catch (Exception ex) {
            System.out.println("Couldn't verify input steam as tRNA-scanSE data " + "and error: " + ex.getMessage());
        }
        return parseable;
    }
}
