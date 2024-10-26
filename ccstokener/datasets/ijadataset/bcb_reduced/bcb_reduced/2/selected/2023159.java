package bop.parser;

import java.applet.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.net.*;
import bop.datamodel.*;
import bop.exception.OutputMalFormatException;

public class BlastParser extends ResultParser {

    boolean record_query_residues = true;

    String program;

    String version;

    boolean ncbi_version;

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
            int index = 0;
            BufferedReader blast_data = new BufferedReader(new InputStreamReader(data_stream));
            program = null;
            String line = blast_data.readLine();
            int tries = 0;
            while (line != null && !parsed && tries < 10) {
                index = line.indexOf(' ');
                parsed = (index >= 0 && (line.indexOf("BLAST") >= 0));
                if (!parsed) {
                    tries++;
                    line = blast_data.readLine();
                }
                if (!parsed) {
                    return parsed;
                }
            }
            program = line.substring(0, index).toLowerCase();
            version = line.substring(++index);
            ncbi_version = (version.toLowerCase().indexOf("wash") < 0 && version.toLowerCase().indexOf("build") < 0);
            Sequence query_seq = getQuerySequence(blast_data, regexp);
            String database = grabDatabase(blast_data);
            AnalysisI analysis = curation.getAnalysis(analysis_type);
            if (analysis == null) {
                analysis = new Analysis();
                analysis.setType(analysis_type);
                analysis.setSequence(query_seq);
                curation.addAnalysis(analysis);
                System.out.println("Created new analysis for " + analysis.getType());
            } else {
                analysis.setSequence(query_seq);
            }
            analysis.setProgram(program);
            analysis.setDatabase(database);
            analysis.setVersion(version);
            analysis.setDate(analysis_date);
            if (ncbi_version) {
                parseNCBI(blast_data, analysis, regexp);
            } else {
                parseWU(blast_data, analysis, regexp);
            }
            blast_data.close();
        } catch (Exception ex) {
            parsed = false;
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            throw new OutputMalFormatException(ex.getMessage(), ex);
        }
        return parsed;
    }

    private void parseWU(BufferedReader blast_data, AnalysisI analysis, String regexp) throws OutputMalFormatException {
        try {
            String line = blast_data.readLine();
            boolean none = false;
            Hashtable hit_hash = new Hashtable();
            boolean good_hit = false;
            boolean found_hit = false;
            ResultSet query_hit_minus = null;
            ResultSet query_hit_plus = null;
            ResultSpan span = null;
            while (line != null && !none) {
                line = line.trim();
                if (line.indexOf("*** NONE **") != -1) {
                    none = true;
                    good_hit = false;
                } else if (line.length() > 0 && line.charAt(0) == '>') {
                    recordHit(good_hit, query_hit_plus, query_hit_minus, span, analysis);
                    grabNameAndDesc(line, hit_hash);
                    span = null;
                    found_hit = false;
                    good_hit = (!sameSeqName(analysis.getSequence().getName(), hit_hash));
                    if (good_hit) {
                        grabLength(blast_data, hit_hash);
                        if (program.equals("BLASTP")) {
                            query_hit_plus = initHit(hit_hash, analysis, regexp);
                            query_hit_minus = initHit(hit_hash, analysis, regexp);
                            found_hit = true;
                        }
                    }
                } else if (good_hit && (line.startsWith("Plus Strand") || line.startsWith("Minus Strand"))) {
                    recordHit(good_hit, query_hit_plus, query_hit_minus, span, analysis);
                    query_hit_plus = initHit(hit_hash, analysis, regexp);
                    query_hit_minus = initHit(hit_hash, analysis, regexp);
                    span = null;
                    found_hit = true;
                } else if (good_hit && found_hit && line.trim().startsWith("Score =")) {
                    if (span != null) {
                        insertSpan(span, query_hit_plus, query_hit_minus);
                    }
                    span = new ResultSpan();
                    span.setAlignment(new ResultSpan());
                    grabScores(span, line.trim());
                    line = blast_data.readLine();
                    grabMatches(line, span);
                    grabFrame(line, span);
                } else if (span != null && good_hit && found_hit && line.startsWith("Query:")) {
                    grabQueryMatch(line, span, false);
                } else if (span != null && good_hit && found_hit && line.startsWith("Sbjct:")) {
                    grabSubjMatch(line, span, false);
                }
                line = blast_data.readLine();
            }
            recordHit(good_hit, query_hit_plus, query_hit_minus, span, analysis);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            throw new OutputMalFormatException(ex.getMessage(), ex);
        }
    }

    private void parseNCBI(BufferedReader blast_data, AnalysisI analysis, String regexp) throws OutputMalFormatException {
        try {
            boolean none = false;
            boolean good_hit = false;
            boolean found_hit = false;
            boolean flip = false;
            Hashtable hit_hash = new Hashtable();
            ResultSet query_hit_minus = null;
            ResultSet query_hit_plus = null;
            ResultSpan span = null;
            String line = blast_data.readLine();
            while (line != null && !none) {
                line = line.trim();
                if (line.indexOf("*** NONE **") != -1) {
                    none = true;
                    good_hit = false;
                } else if (line.length() > 0 && line.charAt(0) == '>') {
                    recordHit(good_hit, query_hit_plus, query_hit_minus, span, analysis);
                    grabNameAndDesc(line, hit_hash);
                    span = null;
                    found_hit = false;
                    good_hit = (!sameSeqName(analysis.getSequence().getName(), hit_hash));
                    if (good_hit) {
                        grabLength(blast_data, hit_hash);
                        query_hit_plus = initHit(hit_hash, analysis, regexp);
                        query_hit_minus = initHit(hit_hash, analysis, regexp);
                        found_hit = true;
                    }
                } else if (good_hit && found_hit && line.trim().startsWith("Score =")) {
                    if (span != null) {
                        insertSpan(span, query_hit_plus, query_hit_minus);
                    }
                    span = new ResultSpan();
                    span.setAlignment(new ResultSpan());
                    grabScores(span, line.trim());
                    line = blast_data.readLine();
                    grabMatches(line, span);
                    line = blast_data.readLine();
                    String subj_strand = grabFrame(line, span);
                    flip = subj_strand.equals("Minus");
                } else if (span != null && good_hit && found_hit && line.startsWith("Query:")) {
                    grabQueryMatch(line, span, flip);
                } else if (span != null && good_hit && found_hit && line.startsWith("Sbjct:")) {
                    grabSubjMatch(line, span, flip);
                }
                line = blast_data.readLine();
            }
            recordHit(good_hit, query_hit_plus, query_hit_minus, span, analysis);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            throw new OutputMalFormatException(ex.getMessage(), ex);
        }
    }

    public String getProgram() {
        return program;
    }

    private Sequence getQuerySequence(BufferedReader blast_data, String regexp) {
        StringBuffer query_name = new StringBuffer();
        query_name.append(findKeyValue(blast_data, "Query="));
        int length = 0;
        try {
            String line = blast_data.readLine();
            while (line != null && length == 0) {
                line = line.trim();
                int index;
                if ((index = line.indexOf(" letters")) > 0) {
                    String value = line.substring(0, index);
                    value = value.replace(',', ' ');
                    value = value.replace('(', ' ');
                    value = value.trim();
                    StringTokenizer tokens = new StringTokenizer(value);
                    while (tokens.hasMoreElements()) {
                        String numb = tokens.nextToken();
                        length = (length * 1000) + Integer.parseInt(numb);
                    }
                } else {
                    query_name.append(" " + line);
                    line = blast_data.readLine();
                }
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        Sequence query_seq = new Sequence();
        bop.parser.FastaParser.parseHeader(query_seq, query_name.toString());
        try {
            RegexpAdapter ad = new RegexpAdapter(regexp, query_name.toString());
            String id = ad.group(1);
            query_seq.setID(id);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        query_seq.setLength(length);
        return query_seq;
    }

    private String findKeyValue(BufferedReader blast_data, String goal) {
        String line;
        String keyword = "";
        String value = "";
        boolean value_found = false;
        try {
            line = blast_data.readLine();
            while (line != null && !value_found) {
                if (line.startsWith(goal)) {
                    value = line.substring(goal.length());
                    value = value.trim();
                    value_found = true;
                } else {
                    line = blast_data.readLine();
                }
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return value;
    }

    private String grabDatabase(BufferedReader blast_data) {
        String db = findKeyValue(blast_data, "Database:");
        int index = db.lastIndexOf('/') + 1;
        if (index > 0 && index < db.length()) db = db.substring(index);
        return db;
    }

    private void grabLength(BufferedReader blast_data, Hashtable hit_hash) {
        String line;
        String value;
        boolean value_found = false;
        StringTokenizer tokens;
        int MAX_DESCRIP_LENGTH = 1000;
        try {
            line = blast_data.readLine();
            while (!value_found && line != null) {
                line = line.trim();
                if (line.startsWith("Length =")) {
                    value = line.substring("Length =".length());
                    value = value.replace(',', ' ');
                    tokens = new StringTokenizer(value);
                    int length = 0;
                    while (tokens.hasMoreElements()) {
                        String numb = tokens.nextToken();
                        length = (length * 1000) + Integer.parseInt(numb);
                    }
                    value = String.valueOf(length);
                    hit_hash.put("length", value);
                    value_found = true;
                } else {
                    value = (String) hit_hash.remove("description");
                    if (value.length() < MAX_DESCRIP_LENGTH) value = value.concat(" " + line);
                    hit_hash.put("description", value);
                    line = blast_data.readLine();
                }
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void grabScores(ResultSpan span, String line) throws OutputMalFormatException {
        String score;
        String expect;
        String prob;
        try {
            if (line.indexOf("bits (") >= 0) {
                score = line.substring(line.indexOf("bits (") + "bits (".length(), line.indexOf(')'));
                expect = line.substring(line.indexOf("Expect = ") + "Expect = ".length());
                expect = expect.trim();
            } else {
                score = line.substring("Score =".length(), line.indexOf('('));
                score = score.trim();
                expect = line.substring(line.indexOf("Expect = ") + "Expect = ".length(), line.lastIndexOf(", "));
                expect = expect.trim();
                prob = line.substring(line.indexOf(" = ") + " = ".length());
                prob = prob.trim();
                span.setProb(prob);
            }
            span.setScore(score);
            span.setExpect(expect);
        } catch (RuntimeException ex) {
            throw new OutputMalFormatException("Error parsing score from " + line, ex.getMessage(), ex);
        }
    }

    private void grabMatches(String line, ResultSpan span) throws OutputMalFormatException {
        int index;
        String match_string;
        try {
            if (line != null) {
                line = line.trim();
                index = line.indexOf("/");
                match_string = (line.substring("Identities = ".length(), index));
                if (program.endsWith("BLASTX")) {
                    int matches = Integer.parseInt(match_string) * 3;
                    span.setMatches(String.valueOf(matches));
                } else {
                    span.setMatches(match_string);
                }
            }
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            throw new OutputMalFormatException("Error parsing identities from " + line, ex.getMessage(), ex);
        }
    }

    private String grabFrame(String line, ResultSpan span) throws OutputMalFormatException {
        int index;
        String keyword = "";
        String subj = "";
        try {
            if (line != null) {
                line = line.trim();
                if ((index = line.lastIndexOf("Frame = ")) != -1) {
                    keyword = line.substring(index);
                    index = keyword.indexOf(" / ");
                    if (index != -1) {
                        span.setFrame(keyword.substring("Frame = ".length(), index));
                        subj = keyword.substring(index + " / ".length());
                        ((ResultSpan) span.getAlignment()).setFrame(subj);
                    } else {
                        span.setFrame(keyword.substring("Frame = ".length()));
                        subj = "Plus";
                    }
                } else if ((index = line.lastIndexOf("Strand = ")) >= 0) {
                    index = line.indexOf(" / ");
                    subj = line.substring(index + " / ".length());
                } else if (!program.equals("BLASTP")) {
                    throw new OutputMalFormatException("Error parsing strand from " + line);
                }
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            throw new OutputMalFormatException("Error parsing strand/frame in " + line, ex.getMessage(), ex);
        }
        return subj;
    }

    private void insertSpan(ResultSpan span, ResultSet query_hit_plus, ResultSet query_hit_minus) {
        int section;
        ResultSpan check_span;
        boolean located = false;
        boolean subject_plus;
        ResultSet hit;
        if (span != null && query_hit_plus != null && query_hit_minus != null) {
            hit = (span.isForward() ? query_hit_plus : query_hit_minus);
            hit.addSpan(span);
        }
    }

    public int getMin() {
        return 0;
    }

    public int getMax() {
        if (program.equals("BLASTN")) return 12000; else return 2000;
    }

    private void grabQueryMatch(String line, ResultSpan span, boolean flip) throws OutputMalFormatException {
        StringTokenizer tokens = new StringTokenizer(line.substring("Query:".length()));
        String value;
        int start, end;
        value = tokens.nextToken();
        if (span.getSeqStart() == 0) {
            if (!flip) span.setSeqStart(value); else span.setSeqEnd(value);
        }
        String more_query_residues = tokens.nextToken();
        span.setQueryResidues(span.getQueryResidues() + more_query_residues);
        if (!tokens.hasMoreElements()) {
            throw new OutputMalFormatException("Error parsing sbjct from " + line + " for span " + span);
        }
        value = tokens.nextToken();
        if (!flip) span.setSeqEnd(value); else span.setSeqStart(value);
    }

    private void grabSubjMatch(String line, ResultSpan span, boolean flip) throws OutputMalFormatException {
        StringTokenizer tokens = new StringTokenizer(line.substring("Sbjct:".length()));
        String value;
        int start, end;
        value = tokens.nextToken();
        if (span.getAlignStart() == 0) {
            if (!flip) span.setAlignStart(value); else span.setAlignEnd(value);
        }
        span.setSubjectResidues(span.getSubjectResidues() + (tokens.nextToken()));
        if (!(tokens.hasMoreElements())) {
            throw new OutputMalFormatException("Error parsing sbjct from " + line + " for span " + span);
        }
        value = tokens.nextToken();
        if (!flip) span.setAlignEnd(value); else span.setAlignStart(value);
    }

    private void setHitScore(ResultSet hit) {
        ResultSpan span;
        int score;
        for (int i = 0; i < (hit.getSpans()).size(); i++) {
            span = (ResultSpan) (hit.getSpans()).elementAt(i);
            score = (int) span.getScore();
            if ((double) score > hit.getScore() || hit.getScore() == -1 || ((double) score == hit.getScore() && span.getExpectValue() < hit.getExpect())) {
                hit.setExpect(span.getExpect());
                hit.setScore(String.valueOf(score));
                hit.setProb(span.getProb());
            }
        }
    }

    private void recordHit(boolean good_hit, ResultSet query_hit_plus, ResultSet query_hit_minus, ResultSpan span, AnalysisI analysis) {
        if (good_hit && query_hit_plus != null && query_hit_minus != null) {
            if (span != null) {
                insertSpan(span, query_hit_plus, query_hit_minus);
            }
            if ((query_hit_plus.getSpans()).size() > 0) {
                setHitScore(query_hit_plus);
                analysis.addResult(query_hit_plus);
            }
            if ((query_hit_minus.getSpans()).size() > 0) {
                setHitScore(query_hit_minus);
                analysis.addResult(query_hit_minus);
            }
            System.gc();
        }
    }

    private ResultSet initHit(Hashtable hit_hash, AnalysisI analysis, String regexp) {
        String value;
        value = (String) hit_hash.get("name");
        ResultSet hit = new ResultSet();
        hit.setSequence(analysis.getSequence());
        hit.setType(program);
        Sequence aligned_seq = new Sequence();
        bop.parser.FastaParser.parseHeader(aligned_seq, value);
        try {
            RegexpAdapter ad = new RegexpAdapter(regexp, value);
            String id = ad.group(1);
            aligned_seq.setID(id);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        Span align_span = new Span();
        align_span.setSequence(aligned_seq);
        hit.setAlignment(align_span);
        hit.setExpect("-1");
        hit.setScore("-1");
        value = (String) hit_hash.get("description");
        if (value == null) value = "";
        aligned_seq.setDescription(value);
        value = (String) hit_hash.get("length");
        aligned_seq.setLength(Integer.parseInt(value));
        return hit;
    }

    private boolean sameSeqName(String seq_name, Hashtable hit_hash) {
        if (seq_name.equals((String) hit_hash.get("name"))) return true;
        String descrip = (String) hit_hash.get("description");
        if ((descrip != null) && descrip.indexOf(seq_name) >= 0) return true;
        return false;
    }

    private void grabNameAndDesc(String line, Hashtable hit_hash) {
        int end_of_name = line.indexOf(' ');
        if (end_of_name != -1) {
            hit_hash.put("name", line.substring(1, end_of_name));
            String value = line.substring(end_of_name);
            value = value.trim();
            hit_hash.put("description", value);
        } else {
            hit_hash.put("name", line.substring(1));
        }
    }

    public boolean canParse(String file_name) {
        boolean parseable = false;
        try {
            FileInputStream istream = new FileInputStream(file_name);
            parseable = canParse(istream);
        } catch (Exception ex) {
            System.out.println("Couldn't verify " + file_name + " as BLAST data");
        }
        return parseable;
    }

    public boolean canParse(InputStream in) {
        boolean parseable = false;
        try {
            BufferedReader blast_data = new BufferedReader(new InputStreamReader(in));
            String line = blast_data.readLine();
            int i = 0;
            while (line != null && i < 10) {
                if (line.indexOf("BLAST") < 0) {
                    line = blast_data.readLine();
                    i++;
                } else {
                    break;
                }
            }
            if (line != null) {
                int space_index = line.indexOf(' ');
                int blast_index = line.indexOf("BLAST");
                parseable = (space_index >= 0 && (blast_index == 0 || blast_index == 1));
            }
            blast_data.close();
        } catch (Exception ex) {
            System.out.println("Couldn't verify input stream as BLAST data " + "and error: " + ex.getMessage());
        }
        return parseable;
    }
}
