package bop.parser;

import java.io.*;
import java.util.*;
import java.net.*;
import bop.datamodel.*;
import bop.exception.OutputMalFormatException;
import bop.filter.Compress;
import bop.util.DNA;

public class Sim4Parser extends ResultParser {

    String program;

    SequenceI query_seq = null;

    boolean complement = false;

    boolean introns_reverse;

    boolean introns_forward;

    int donor_index;

    Stack introns = new Stack();

    int polyA_length = 16;

    public Sim4Parser() {
    }

    public void setOptions(Hashtable options) {
        super.setOptions(options);
        if (options != null) {
            String value = (String) options.get("-polyA_length");
            if (value != null) {
                try {
                    polyA_length = Integer.parseInt(value);
                    System.out.println("Parsing with polyA_length " + polyA_length);
                } catch (NumberFormatException e) {
                    polyA_length = 16;
                }
            }
        }
    }

    public String getProgram() {
        return program;
    }

    public boolean parseResults(URL url, String analysis_type, CurationI curation, Date analysis_date, String regexp) throws OutputMalFormatException {
        boolean parsed = false;
        try {
            InputStream data_stream = url.openStream();
            parsed = parseResults(data_stream, analysis_type, curation, analysis_date, regexp);
            data_stream.close();
        } catch (OutputMalFormatException ex) {
            throw new OutputMalFormatException(ex.getMessage(), ex);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return parsed;
    }

    public boolean parseResults(InputStream data_stream, String analysis_type, CurationI curation, Date analysis_date, String regexp) throws OutputMalFormatException {
        boolean can_parse = false;
        String line = "";
        query_seq = curation.getCuratedSeq();
        try {
            BufferedReader sim4_data = new BufferedReader(new InputStreamReader(data_stream));
            String query_name = null;
            query_name = findQuery(sim4_data, "", regexp, true);
            can_parse = (query_name != null && !query_name.equals(""));
            if (!can_parse) {
                program = null;
                System.out.println("Why can't we parse " + query_name);
                return can_parse;
            }
            program = "sim4";
            String database = "";
            AnalysisI analysis = curation.getAnalysis(analysis_type);
            if (analysis == null) {
                analysis = new Analysis();
                analysis.setType(analysis_type);
                analysis.setSequence(query_seq);
                curation.addAnalysis(analysis);
                System.out.println("Created new sim4 analysis for " + analysis.getType() + " of " + query_seq.getID());
            } else {
                analysis.setSequence(query_seq);
            }
            analysis.setProgram(program);
            analysis.setDate(analysis_date);
            analysis.setDatabase(database);
            while (!query_name.equals("")) {
                ResultSet match = grabMatch(sim4_data, query_name, regexp);
                if (match != null) {
                    match.setSequence(analysis.getSequence());
                    introns.clear();
                    line = grabSpans(sim4_data, analysis, match);
                    if (match.getSpans().size() > 0) {
                        if ((complement && !introns_forward) || introns_reverse) {
                            Vector spans = (Vector) match.getSpans().clone();
                            while (match.getSpans().size() > 0) {
                                ResultSpan span = (ResultSpan) match.getSpans().elementAt(0);
                                match.removeSpan(span);
                            }
                            while (spans.size() > 0) {
                                ResultSpan span = (ResultSpan) spans.elementAt(0);
                                spans.removeElement(span);
                                int temp = span.getStart();
                                span.setStart(span.getEnd());
                                span.setEnd(temp);
                                match.addSpan(span);
                            }
                            spans = (Vector) match.getSpans();
                            SequenceI align_seq = match.getAlignment().getSequence();
                            int seq_length = align_seq.length();
                            for (int i = 0; i < spans.size(); i++) {
                                ResultSpan span = (ResultSpan) spans.elementAt(0);
                                SpanI align = span.getAlignment();
                                int pos1 = complementPosition(seq_length, align.getStart());
                                int pos2 = complementPosition(seq_length, align.getEnd());
                                align.setStart(pos2);
                                align.setEnd(pos1);
                                align.setSubSequence(DNA.reverseComplement(align.getSubSequence()));
                            }
                            if ((complement && introns_forward) || (!complement && introns_reverse)) {
                                String seq_name = align_seq.getName();
                                int index = seq_name.indexOf("_revcomp");
                                if (index > 0) seq_name = seq_name.substring(0, index); else seq_name = seq_name + "_revcomp";
                                align_seq.setName(seq_name);
                                String seq_id = align_seq.getID();
                                int i = seq_id.indexOf("_revcomp");
                                if (i > 0) seq_id = seq_id.substring(0, index); else seq_id = seq_id + "_revcomp";
                                align_seq.setID(seq_id);
                            }
                        }
                        analysis.addResult(match);
                    }
                } else {
                    line = sim4_data.readLine();
                }
                query_name = findQuery(sim4_data, line, regexp, true);
            }
            sim4_data.close();
        } catch (OutputMalFormatException e) {
            throw new OutputMalFormatException(e.getMessage(), e);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return can_parse;
    }

    private String findQuery(BufferedReader sim4_data, String line, String regexp, boolean create_seq) throws OutputMalFormatException {
        String query_name = "";
        boolean value_found = false;
        try {
            while (line != null && !value_found) {
                if (line.startsWith("seq1 = ")) {
                    value_found = true;
                    int index1 = ((line.lastIndexOf("/") > 0) ? line.lastIndexOf("/") + "/".length() : "seq1 = ".length());
                    int index2 = line.lastIndexOf(".fst,");
                    if (index2 < 0) {
                        index2 = line.lastIndexOf(".seq,");
                        if (index2 < 0) index2 = line.lastIndexOf(",");
                    }
                    query_name = line.substring(index1, index2);
                    String length_val = "";
                    int query_length = 0;
                    try {
                        length_val = line.substring(line.lastIndexOf(", ") + ", ".length(), line.lastIndexOf(" bp"));
                        query_length = Integer.parseInt(length_val);
                    } catch (RuntimeException ex) {
                        throw new OutputMalFormatException("Error parsing line, " + "looking for \", bp\":" + "  " + line, ex.getMessage(), ex);
                    }
                    if (query_seq == null && create_seq) {
                        System.out.println("Should not be creating a seq for " + query_name);
                        query_seq = new Sequence();
                        bop.parser.FastaParser.parseHeader(query_seq, query_name);
                        try {
                            RegexpAdapter ad = new RegexpAdapter(regexp, query_name);
                            String id = ad.group(1);
                            query_seq.setID(id);
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                        query_seq.setLength(query_length);
                    }
                } else {
                    line = sim4_data.readLine();
                }
            }
        } catch (OutputMalFormatException ex) {
            throw new OutputMalFormatException(ex.getMessage(), ex);
        } catch (Exception ex) {
            System.out.println("Erroring parsing line " + line);
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return query_name;
    }

    private ResultSet grabMatch(BufferedReader sim4_data, String query_name, String regexp) throws OutputMalFormatException {
        ResultSet match = null;
        String line = "";
        int slash = query_name.lastIndexOf("/");
        if (slash >= 0) {
            query_name = query_name.substring(slash + 1);
        }
        try {
            line = sim4_data.readLine();
            while (line != null && !(line.startsWith("seq2"))) {
                line = sim4_data.readLine();
            }
            if (line != null && (line.startsWith("seq2"))) {
                Sequence seq2 = grabSubject(line, query_name, regexp);
                if (seq2 != null) {
                    match = new ResultSet();
                    match.setScore("-1");
                    match.setType(program);
                    ResultSet align_span = new ResultSet();
                    align_span.setSequence(seq2);
                    match.setAlignment(align_span);
                }
            }
        } catch (OutputMalFormatException e) {
            throw new OutputMalFormatException(e.getMessage(), e);
        } catch (Exception e) {
            throw new OutputMalFormatException("Erroring reading line " + line, e.getMessage(), e);
        }
        return match;
    }

    private boolean validSeq2(String line) {
        int open_paren = line.indexOf("(");
        int close_paren = line.lastIndexOf("),");
        return (open_paren >= 0 && close_paren >= 0 && open_paren + "(".length() < close_paren);
    }

    private Sequence grabSubject(String line, String query_name, String regexp) throws OutputMalFormatException {
        String value;
        Sequence seq = null;
        if (validSeq2(line)) {
            try {
                value = line.substring(line.indexOf("(") + "(".length(), line.lastIndexOf("),"));
                if (value.charAt(0) == '>') value = value.substring(1);
            } catch (RuntimeException ex) {
                throw new OutputMalFormatException("Error parsing line, " + "looking for \"(>\": " + line, ex.getMessage(), ex);
            }
            String match_name;
            String desc = "";
            int index = value.indexOf("_");
            if (index > 0 && value.indexOf("TN_") < 0 && value.indexOf("OL_") < 0) {
                int barindex = value.indexOf("|");
                if (barindex > 0 && barindex < index) {
                    int secondindex = value.indexOf("_", index);
                    char char_after_bar = value.charAt(secondindex + 1);
                    if (secondindex != index && Character.isDigit(char_after_bar)) {
                        index = value.indexOf("_", secondindex + 1);
                    }
                }
                match_name = value.substring(0, index);
                desc = value.substring(index + 1);
                desc = desc.replace('_', ' ');
            } else {
                match_name = value;
            }
            if ((match_name.indexOf(query_name)) < 0) {
                try {
                    value = line.substring(line.lastIndexOf(", ") + ", ".length(), line.lastIndexOf(" bp"));
                } catch (RuntimeException ex) {
                    throw new OutputMalFormatException("Error parsing line, " + "looking for " + "\", bp\" " + line, ex.getMessage(), ex);
                }
                int length = Integer.parseInt(value);
                seq = new Sequence();
                bop.parser.FastaParser.parseHeader(seq, match_name);
                try {
                    RegexpAdapter ad = new RegexpAdapter(regexp, match_name);
                    String id = ad.group(1);
                    seq.setID(id);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                seq.setDescription(desc);
                seq.setLength(length);
            }
        }
        return seq;
    }

    private String grabSpans(BufferedReader sim4_data, AnalysisI analysis, ResultSet match) throws OutputMalFormatException {
        String line = "";
        boolean value_found = false;
        boolean keep_it = true;
        complement = false;
        donor_index = -1;
        ResultSet match_align = (ResultSet) match.getAlignment();
        int seq_length = (match_align.getSequence()).length();
        try {
            line = sim4_data.readLine();
            while (line != null && !value_found) {
                String untrimmed_line = line;
                line = line.trim();
                if (line.startsWith("seq")) {
                    value_found = true;
                } else if (line.startsWith(">")) {
                    grabDescription(line, match.getAlignment().getSequence());
                } else if (line.length() > 0 && (line.startsWith("(complement)"))) {
                    complement = true;
                } else if (line.indexOf("%") >= 0 && line.indexOf("(") >= 0) {
                    int index = line.indexOf("-");
                    keep_it &= index > 0;
                    if (keep_it) {
                        ResultSpan span = grabSpan(line, seq_length, analysis);
                        if (span != null && span.getScore() > 0 && !overlapsExisting(match, span)) {
                            match.addSpan(span);
                            match_align.addSpan(span.getAlignment());
                            SequenceI seq2 = match_align.getSequence();
                            span.getAlignment().setSequence(seq2);
                            if (span.getScore() > match.getScore()) {
                                match.setScore(span.getScore());
                            }
                        }
                    }
                } else if (line.length() > 0 && untrimmed_line.indexOf("0 ") >= 0 && untrimmed_line.charAt(0) == ' ') {
                    grabAlignment(sim4_data, seq_length, match);
                } else if (line.length() > 0) {
                    System.err.println(match.getAlignName() + ": Don't know how to parse \"" + line + "\"");
                    System.exit(1);
                }
                if (!value_found) {
                    line = sim4_data.readLine();
                }
            }
        } catch (RuntimeException ex) {
            throw new OutputMalFormatException("Error parsing " + line, ex.getMessage(), ex);
        } catch (Exception ex) {
            System.err.println("Could not parse " + line + " for " + match.getAlignment().getSequence().getName());
            System.out.println(ex.getMessage());
            keep_it = false;
            ex.printStackTrace();
        }
        if (match.getSpans().size() > 0) {
            setIntrons(match);
        }
        keep_it |= !(introns_forward && introns_reverse);
        if (!keep_it) {
            match.getSpans().removeAllElements();
        }
        return (line);
    }

    private ResultSpan grabSpan(String line, int seq_length, AnalysisI analysis) throws OutputMalFormatException {
        String before = line;
        ResultSpan span = null;
        try {
            int index = line.indexOf("-");
            String seq_start = line.substring(0, index);
            index++;
            line = line.substring(index);
            index = line.indexOf(" ");
            String seq_end = line.substring(0, index);
            index = line.indexOf("(");
            index++;
            line = line.substring(index);
            index = line.indexOf("-");
            String match_start = line.substring(0, index);
            index++;
            line = line.substring(index);
            index = line.indexOf(")");
            String match_end = line.substring(0, index);
            index++;
            line = line.substring(index);
            index = line.indexOf("%");
            String score = line.substring(0, index);
            score = score.trim();
            String current_intron = ((line.indexOf("<-") >= 0) ? "reverse" : ((line.indexOf("->") >= 0) ? "forward" : ""));
            introns.push(current_intron);
            span = new ResultSpan();
            span.setAlignment(new ResultSpan());
            span.setSequence(analysis.getSequence());
            span.setSeqStart(seq_start);
            span.setSeqEnd(seq_end);
            span.setAlignStart(match_start);
            span.setAlignEnd(match_end);
            span.setScore(score);
        } catch (RuntimeException ex) {
            throw new OutputMalFormatException("Error parsing " + before, ex.getMessage(), ex);
        }
        return span;
    }

    private void setIntrons(ResultSet match) {
        introns_reverse = false;
        introns_forward = false;
        introns.pop();
        if (match.getSpans().size() > 1) {
            int index = match.getSpans().size() - 1;
            ResultSpan span;
            span = (ResultSpan) (match.getSpans()).elementAt(index);
            if (stripPolyATail(span)) {
                match.removeSpan(span);
                String stuff = (String) introns.pop();
            }
            span = (ResultSpan) (match.getSpans()).elementAt(0);
            if (stripPolyATail(span)) {
                match.removeSpan(span);
                if (!introns.empty()) {
                    String stuff = (String) introns.elementAt(0);
                    introns.removeElementAt(0);
                }
            }
        }
        while (!introns.empty()) {
            String current_intron = (String) introns.pop();
            introns_forward |= current_intron.equals("forward");
            introns_reverse |= current_intron.equals("reverse");
        }
    }

    private boolean stripPolyATail(ResultSpan span) {
        boolean stripped = false;
        if (span.getSequence() != null) {
            String dna = span.getSequence().getResidues(span.getStart(), span.getEnd());
            if (dna.length() > 0 && dna.length() <= 50) {
                double shrinks_to = Compress.compress(dna, 1);
                if (shrinks_to <= polyA_length) {
                    stripped = true;
                }
            }
        }
        return stripped;
    }

    private int complementPosition(int seq_length, int raw_position) {
        return (seq_length - raw_position + 1);
    }

    private void grabDescription(String line, SequenceI sbjct_seq) {
        int index = line.indexOf(" ");
        if (index > 0) {
            String fasta_id = line.substring(1, index);
            index++;
            if (index < line.length()) {
                String desc = line.substring(index);
                Sequence tmp_seq = new Sequence();
                bop.parser.FastaParser.parseHeader(tmp_seq, fasta_id);
                if (tmp_seq.getName().equals(sbjct_seq.getName())) {
                    sbjct_seq.setDescription(desc);
                } else if (tmp_seq.getName().equals(query_seq.getName())) {
                    query_seq.setDescription(desc);
                } else {
                    System.out.println(tmp_seq.getName() + " doesn't match either " + query_seq.getName() + " or " + sbjct_seq.getName());
                }
            }
        }
    }

    private void grabAlignment(BufferedReader sim4_data, int seq_length, ResultSet match) throws OutputMalFormatException {
        String query_line = "";
        String align_line = "";
        String sbjct_line = "";
        if (donor_index > 0) donor_index = 0;
        try {
            ResultSet match_align = (ResultSet) match.getAlignment();
            ResultSpan query_span;
            ResultSpan sbjct_span;
            query_line = sim4_data.readLine().trim();
            align_line = sim4_data.readLine().trim();
            sbjct_line = sim4_data.readLine().trim();
            int space = query_line.indexOf(" ");
            int query_pos;
            int sbjct_pos;
            if (space >= 0) {
                query_pos = Integer.parseInt(query_line.substring(0, space));
                query_line = query_line.substring(space + 1);
            } else {
                query_pos = Integer.parseInt(query_line);
                query_line = " ";
            }
            space = sbjct_line.indexOf(" ");
            if (space >= 0) {
                sbjct_pos = Integer.parseInt(sbjct_line.substring(0, space));
                sbjct_line = sbjct_line.substring(space + 1);
            } else {
                sbjct_pos = Integer.parseInt(sbjct_line);
                sbjct_line = " ";
            }
            sbjct_span = (ResultSpan) match_align.spanOverlapping(sbjct_pos);
            query_span = (ResultSpan) match.spanOverlapping(query_pos);
            int query_index = (query_span != null ? match.getSpans().indexOf(query_span) : -1);
            int sbjct_index = (sbjct_span != null ? match_align.getSpans().indexOf(sbjct_span) : -1);
            int span_index = sbjct_index;
            if (sbjct_index != query_index || span_index == -1) {
                if ((query_index < sbjct_index && query_index >= 0) || span_index == -1) span_index = query_index;
                if (span_index == -1) {
                    ResultSet a = (ResultSet) match.getAlignment();
                    System.out.println("Have no span at either " + sbjct_pos + " or " + query_pos + " limits are " + a.getStart() + "-" + a.getEnd() + " " + match.getAlignName() + " " + a.getSpans().size() + " spans");
                    for (int i = 0; i < match.getSpans().size(); i++) {
                        ResultSpan s = (ResultSpan) match.getSpans().elementAt(i);
                        System.out.println("\t" + s.getAlignment().getStart() + "-" + s.getAlignment().getEnd());
                    }
                    System.exit(-1);
                }
                sbjct_span = (ResultSpan) match_align.getSpans().elementAt(span_index);
                query_span = (ResultSpan) match.getSpans().elementAt(span_index);
            }
            if (sbjct_span == null || query_span == null) {
                ResultSet a = (ResultSet) match.getAlignment();
                System.out.println("Have no span at " + sbjct_pos + " or " + query_pos + " limits are " + a.getStart() + "-" + a.getEnd() + " " + match.getAlignName() + " " + a.getSpans().size() + " spans");
                for (int i = 0; i < match.getSpans().size(); i++) {
                    ResultSpan s = (ResultSpan) match.getSpans().elementAt(i);
                    System.out.println("\t" + s.getAlignment().getStart() + "-" + s.getAlignment().getEnd());
                }
                System.exit(1);
            }
            int[] exon_indices = exonIndex(align_line);
            int start_index = exon_indices[0];
            int end_index = exon_indices[1];
            while (start_index >= 0) {
                String query_seq = ((end_index < 0 || end_index >= query_line.length()) ? query_line.substring(start_index) : query_line.substring(start_index, end_index));
                String sbjct_seq = ((end_index < 0 || end_index >= sbjct_line.length()) ? sbjct_line.substring(start_index) : sbjct_line.substring(start_index, end_index));
                sbjct_span.setSubSequence(sbjct_span.getSubSequence() + sbjct_seq);
                if (end_index >= 0) {
                    align_line = align_line.substring(end_index);
                    if (end_index < query_line.length()) query_line = query_line.substring(end_index); else query_line = "";
                    if (end_index < sbjct_line.length()) sbjct_line = sbjct_line.substring(end_index); else sbjct_line = "";
                    query_line = padForAlign(query_line, align_line.length());
                    sbjct_line = padForAlign(sbjct_line, align_line.length());
                    exon_indices = exonIndex(align_line);
                    start_index = exon_indices[0];
                    end_index = exon_indices[1];
                    if (start_index >= 0) {
                        query_span = (ResultSpan) query_span.getNextSpan();
                        sbjct_span = (ResultSpan) sbjct_span.getNextSpan();
                        if (query_span == null || sbjct_span == null) {
                            System.out.println(match.getAlignName() + " with " + match.getSpans().size() + " spans " + " has alignment " + align_line + " and query " + query_line + " and sbjct " + sbjct_line + " BUT spans are null " + " span_index is " + span_index);
                            for (int i = 0; i < match.getSpans().size(); i++) {
                                Span s = (Span) match.getSpans().elementAt(i);
                                System.out.println("\t" + i + ". " + s.getStart() + "-" + s.getEnd());
                            }
                        }
                    }
                } else {
                    start_index = -1;
                }
            }
        } catch (Exception ex) {
            System.out.println("Error parsing alignment of " + match.getAlignName() + "\n\t" + query_line + "\n\t" + align_line + "\n\t" + sbjct_line);
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static String[] intron_begin = { "|>", " >", "->", "|<", " <", "-<" };

    private static String[] intron_end = { ">|", "> ", ">-", "<|", "< ", "<-", ">>>", "<<<" };

    private int[] exonIndex(String align_line) {
        int[] exon_indices = { 0, align_line.length() - 1 };
        if (donor_index < 0 && (align_line.startsWith("<<<") || align_line.startsWith(">>>"))) {
            donor_index = 1;
        }
        if (donor_index >= 0) {
            int find_it = -1;
            for (int i = 0; i < intron_end.length && find_it < 0; i++) {
                find_it = (align_line.indexOf(intron_end[i], donor_index));
                if (find_it >= 0) {
                    if (intron_end[i].length() == 2) exon_indices[0] = find_it + 1; else exon_indices[0] = -1;
                    donor_index = -1;
                }
            }
            if (find_it < 0) {
                exon_indices[0] = -1;
            }
        }
        if (donor_index < 0 && exon_indices[0] >= 0) {
            int find_it = -1;
            for (int i = 0; i < intron_begin.length && find_it < 0; i++) {
                find_it = (align_line.indexOf(intron_begin[i], exon_indices[0]));
                if (find_it >= 0) {
                    exon_indices[1] = align_line.indexOf(">", find_it);
                    if (exon_indices[1] < 0) exon_indices[1] = align_line.indexOf("<", find_it);
                    donor_index = 1;
                }
            }
            if (find_it < 0) exon_indices[1] = -1;
        }
        return exon_indices;
    }

    private String padForAlign(String str, int length) {
        if (str.length() < length) {
            int pad = length - str.length();
            StringBuffer buf = new StringBuffer(pad);
            for (int i = 0; i < pad; i++) buf = buf.append(' ');
            return str + buf.toString();
        } else return str;
    }

    private boolean overlapsExisting(ResultSet hit, ResultSpan new_span) {
        boolean overlaps = false;
        for (int i = 0; i < hit.getSpans().size() && !overlaps; i++) {
            ResultSpan check_span = (ResultSpan) (hit.getSpans()).elementAt(i);
            overlaps = check_span.overlaps(new_span);
            if (overlaps) {
                System.out.println(hit.getAlignName() + " has overlapping spans.\n\told at " + check_span.getStart() + "-" + check_span.getEnd() + "\n\tnew at " + new_span.getStart() + "-" + new_span.getEnd());
                check_span.extendWith(new_span);
                hit.adjustSetEdges(check_span);
                check_span.getAlignment().extendWith(new_span.getAlignment());
                ((ResultSet) hit.getAlignment()).adjustSetEdges(check_span.getAlignment());
            }
        }
        return overlaps;
    }

    public boolean canParse(String file_name) {
        boolean parseable = false;
        try {
            FileInputStream istream = new FileInputStream(file_name);
            parseable = canParse(istream);
        } catch (Exception ex) {
            System.out.println("Couldn't verify " + file_name + " as sim4 data");
        }
        return parseable;
    }

    public boolean canParse(InputStream in) {
        boolean parseable = false;
        try {
            BufferedReader sim4_data = new BufferedReader(new InputStreamReader(in));
            String query_name = findQuery(sim4_data, "", "", false);
            parseable = (query_name != null && !query_name.equals(""));
            sim4_data.close();
        } catch (Exception ex) {
            System.out.println("Couldn't verify input stream as sim4 data " + "and error: " + ex.getMessage());
        }
        return parseable;
    }
}
