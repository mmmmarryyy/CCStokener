package bop.parser;

import java.io.*;
import java.util.*;
import java.text.*;
import java.net.URL;
import bop.datamodel.*;
import bop.parser.*;
import bop.exception.OutputMalFormatException;

public class RpMaskerParser extends ResultParser {

    String program;

    String score;

    String perc_sub = "";

    String perc_del = "";

    String perc_ins = "";

    String seq_name = "";

    String query_start = "";

    String query_end = "";

    String query_past = "";

    String complement = "";

    String repeatname = "";

    String acc = "";

    String subj_start = "";

    String subj_end = "";

    String subj_prior = "";

    String subj_unique = "";

    Sequence seq = null;

    public boolean parseResults(URL url, String analysis_type, CurationI curation, Date analysis_date, String regexp) throws OutputMalFormatException {
        boolean parsed = false;
        try {
            InputStream data_stream = url.openStream();
            parsed = parseResults(data_stream, analysis_type, curation, analysis_date, regexp);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return parsed;
    }

    public boolean parseResults(InputStream data_stream, String analysis_type, CurationI curation, Date analysis_date, String regexp) throws OutputMalFormatException {
        boolean parsed = false;
        seq = null;
        program = null;
        String line;
        BufferedReader data = null;
        try {
            data = new BufferedReader(new InputStreamReader(data_stream));
            line = data.readLine();
            int i = 0;
            while (line != null && i < 10 && !parsed && line.toLowerCase().indexOf("genscan") < 0) {
                parsed = parseLine(line);
                if (!parsed) {
                    line = data.readLine();
                    i++;
                }
            }
        } catch (Exception ex) {
            return parsed;
        }
        if (!parsed) {
            return parsed;
        }
        AnalysisI analysis = curation.getAnalysis(analysis_type);
        boolean new_analysis = (analysis == null);
        if (new_analysis) {
            analysis = new Analysis();
            analysis.setType(analysis_type);
            System.out.println("Created new analysis for " + analysis.getType());
        }
        program = "RepeatMasker";
        analysis.setProgram(program);
        analysis.setDate(analysis_date);
        try {
            while ((line = data.readLine()) != null) {
                ResultSet repeat = grabRepeat(line, analysis, curation);
                if (repeat != null) analysis.addResult(repeat);
            }
            parsed = true;
            data.close();
        } catch (Exception ex) {
            throw new OutputMalFormatException("can't parse" + line, ex.getMessage(), ex);
        }
        return parsed;
    }

    public String getProgram() {
        return program;
    }

    public boolean canParse(String file_name) {
        boolean parseable = false;
        try {
            FileInputStream istream = new FileInputStream(file_name);
            parseable = canParse(istream);
        } catch (Exception ex) {
            System.out.println("Couldn't verify " + file_name + " as gff data");
        }
        return parseable;
    }

    public boolean canParse(InputStream in) {
        boolean parsed = false;
        try {
            BufferedReader data = new BufferedReader(new InputStreamReader(in));
            String line = data.readLine();
            int i = 0;
            while (line != null && i < 10 && !parsed && line.toLowerCase().indexOf("genscan") < 0) {
                parsed = parseLine(line);
                if (!parsed) {
                    line = data.readLine();
                    i++;
                } else {
                    System.out.println("Able to parse " + line);
                }
            }
        } catch (Exception ex) {
            System.out.println("Couldn't verify input stream as RepeatMasker " + "and error: " + ex.getMessage());
            return false;
        }
        return parsed;
    }

    private ResultSet grabRepeat(String line, AnalysisI analysis, CurationI curation) throws OutputMalFormatException {
        String name = null;
        ResultSet repeat = null;
        ResultSpan span = null;
        try {
            if (parseLine(line) && subj_unique.equals("")) {
                if (analysis.getSequence() == null) {
                    analysis.setSequence(new Sequence(seq_name));
                    curation.addAnalysis(analysis);
                }
                repeat = new ResultSet();
                repeat.setAlignment(new Span());
                repeat.setSequence(analysis.getSequence());
                repeat.setType(program);
                repeat.setScore(score);
                repeat.setID(repeatname);
                span = new ResultSpan();
                span.setAlignment(new ResultSpan());
                span.setScore(score);
                parseRange(span, query_start, query_end, query_past);
                int length;
                if (complement.equals("+")) {
                    length = parseRange(span.getAlignment(), subj_start, subj_end, subj_prior);
                } else {
                    System.out.println(repeatname + " start=" + subj_end + " end=" + subj_prior + " remains=" + subj_start);
                    length = parseRange(span.getAlignment(), subj_end, subj_prior, subj_start);
                }
                SequenceI aligned_seq = ((Analysis) analysis).getCuration().getSequence(acc);
                if (aligned_seq == null) aligned_seq = ((Analysis) analysis).getCuration().getSequence(repeatname);
                if (aligned_seq == null) {
                    aligned_seq = new Sequence();
                    aligned_seq.setLength(length);
                    if (acc.startsWith("FB") || acc.startsWith("fb")) {
                        aligned_seq.setDatabase("FB");
                        aligned_seq.setAccession(acc);
                        aligned_seq.setID(repeatname);
                    } else {
                        aligned_seq.setDatabase("gb");
                        aligned_seq.setAccession(acc);
                        aligned_seq.setID(acc);
                    }
                }
                aligned_seq.setName(repeatname);
                repeat.getAlignment().setSequence(aligned_seq);
                span.getAlignment().setSequence(aligned_seq);
                span.addOutput("substitutions", perc_sub);
                span.addOutput("deletions", perc_del);
                span.addOutput("insertions", perc_ins);
                repeat.addSpan(span);
            }
        } catch (OutputMalFormatException ex) {
            repeat = null;
            throw new OutputMalFormatException(ex.getMessage(), ex);
        }
        return repeat;
    }

    private boolean parseLine(String line) {
        boolean okay = true;
        try {
            line = line.trim();
            StringTokenizer tokens = new StringTokenizer(line);
            score = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
            okay &= (!score.equals("") && (Double.valueOf(score)).doubleValue() > 0);
            perc_sub = ((tokens.hasMoreTokens()) ? tokens.nextToken() : "");
            okay &= !perc_sub.equals("");
            perc_del = ((tokens.hasMoreTokens()) ? tokens.nextToken() : "");
            okay &= !perc_del.equals("");
            perc_ins = ((tokens.hasMoreTokens()) ? tokens.nextToken() : "");
            okay &= !perc_ins.equals("");
            seq_name = ((tokens.hasMoreTokens()) ? tokens.nextToken() : "");
            okay &= !seq_name.equals("");
            query_start = ((tokens.hasMoreTokens()) ? tokens.nextToken() : "");
            okay &= !query_start.equals("");
            query_end = ((tokens.hasMoreTokens()) ? tokens.nextToken() : "");
            okay &= !query_end.equals("");
            query_past = ((tokens.hasMoreTokens()) ? tokens.nextToken() : "");
            okay &= !query_past.equals("");
            complement = ((tokens.hasMoreTokens()) ? tokens.nextToken() : "");
            okay &= !complement.equals("");
            repeatname = ((tokens.hasMoreTokens()) ? tokens.nextToken() : "");
            okay &= !repeatname.equals("");
            acc = ((tokens.hasMoreTokens()) ? tokens.nextToken() : "");
            okay &= !acc.equals("");
            subj_start = ((tokens.hasMoreTokens()) ? tokens.nextToken() : "");
            okay &= !subj_start.equals("");
            subj_end = ((tokens.hasMoreTokens()) ? tokens.nextToken() : "");
            okay &= !subj_end.equals("");
            subj_prior = ((tokens.hasMoreTokens()) ? tokens.nextToken() : "");
            okay &= !subj_prior.equals("");
            subj_unique = ((tokens.hasMoreTokens()) ? tokens.nextToken() : "");
        } catch (Exception ex) {
            okay = false;
        }
        return okay;
    }

    private int parseRange(SpanI span, String span_start, String span_end, String remainder) throws OutputMalFormatException {
        int start;
        int end;
        int length = 0;
        try {
            if (complement.equals("+")) {
                start = Integer.parseInt(span_start);
                end = Integer.parseInt(span_end);
            } else {
                start = Integer.parseInt(span_end);
                end = Integer.parseInt(span_start);
            }
            span.setStart(start);
            span.setEnd(end);
            if (remainder.indexOf("(") >= 0) remainder = remainder.substring(remainder.indexOf("(") + 1);
            if (remainder.indexOf(")") > 0) remainder = remainder.substring(0, remainder.indexOf(")"));
            length = span.length() + Integer.parseInt(remainder);
        } catch (NumberFormatException ex) {
            throw new OutputMalFormatException("Error parsing number " + " span_start=" + span_start + " span_end=" + span_end, ex.getMessage(), ex);
        }
        return length;
    }
}
