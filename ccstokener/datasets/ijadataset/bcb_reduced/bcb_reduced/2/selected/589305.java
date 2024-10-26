package bop.parser;

import java.io.*;
import java.util.*;
import java.text.*;
import java.net.URL;
import bop.datamodel.*;
import bop.parser.*;
import bop.exception.OutputMalFormatException;

public class GffParser extends ResultParser {

    boolean record_query_residues = true;

    String query_name;

    String subject_db;

    String program;

    String version;

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
        try {
            ResultSpan span = null;
            String line;
            String value;
            BufferedReader data = new BufferedReader(new InputStreamReader(data_stream));
            boolean none = false;
            boolean found_hit = false;
            line = data.readLine();
            int i = 0;
            while (line != null && i < 10) {
                if (!line.startsWith("##gff-")) {
                    line = data.readLine();
                    i++;
                } else {
                    break;
                }
            }
            if (!line.startsWith("##gff-")) {
                program = null;
                return parsed;
            }
            program = line.substring(2, "##gff".length());
            version = line.substring("##gff-".length());
            AnalysisI analysis = curation.getAnalysis(analysis_type);
            boolean new_analysis = (analysis == null);
            if (new_analysis) {
                analysis = new Analysis();
                analysis.setType(analysis_type);
                System.out.println("Created new analysis for " + analysis.getType());
            }
            analysis.setVersion(version);
            analysis.setDate(grabDate(data));
            Sequence seq = grabDNA(data, regexp);
            analysis.setSequence(seq);
            grabGenes(data, curation, analysis);
            data.close();
            parsed = true;
        } catch (OutputMalFormatException ex) {
            throw new OutputMalFormatException(ex.getMessage(), ex);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return parsed;
    }

    public String getProgram() {
        return program;
    }

    private Date grabDate(BufferedReader data) throws OutputMalFormatException {
        Date date = null;
        String value = findKeyValue(data, "##date ");
        if (value != null && !value.equals("")) {
            try {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-d");
                date = df.parse(value);
            } catch (ParseException ex) {
                throw new OutputMalFormatException("Error when getting date " + value, ex.getMessage(), ex);
            } catch (Exception ex) {
                System.out.println("could not parse gff date\n" + ex.getMessage());
                ex.printStackTrace();
            }
        }
        return date;
    }

    private String findKeyValue(BufferedReader data, String goal) {
        String line;
        String keyword = "";
        String value = "";
        boolean value_found = false;
        try {
            line = data.readLine();
            while (line != null && !value_found) {
                if (line.startsWith(goal)) {
                    value = line.substring(goal.length());
                    value = value.trim();
                    value_found = true;
                } else if (line.startsWith("#")) {
                    line = data.readLine();
                } else {
                    value_found = true;
                }
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return value;
    }

    private Sequence grabDNA(BufferedReader data, String regexp) throws OutputMalFormatException {
        Sequence seq = null;
        String value = findKeyValue(data, "##DNA ");
        if (value != null && !value.equals("")) {
            seq = new Sequence(value);
            try {
                RegexpAdapter ad = new RegexpAdapter(regexp, value);
                String id = ad.group(1);
                seq.setID(id);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            StringBuffer dna = new StringBuffer();
            String line = "";
            try {
                line = data.readLine();
                while (line != null && !(line.startsWith("##end-DNA"))) {
                    dna.append(line.substring(2));
                    line = data.readLine();
                }
                seq.setResidues(dna.toString());
            } catch (RuntimeException ex) {
                throw new OutputMalFormatException("Error when parsing line " + line, ex.getMessage(), ex);
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            }
        }
        if (seq == null) {
            System.out.println("Could not parse out ##DNA");
        }
        return seq;
    }

    private void grabGenes(BufferedReader data, CurationI curation, AnalysisI analysis) throws OutputMalFormatException {
        String line;
        String name = null;
        SequenceI seq = analysis.getSequence();
        ResultSet gene = null;
        AnalysisI splice_analysis = curation.getAnalysis("Splice");
        try {
            while ((line = data.readLine()) != null) {
                line = line.trim();
                StringTokenizer tokens = new StringTokenizer(line);
                boolean okay = true;
                String seq_name = ((tokens.hasMoreTokens()) ? tokens.nextToken() : "");
                if (seq == null) {
                    seq = new Sequence(seq_name);
                    analysis.setSequence(seq);
                }
                okay &= seq_name.indexOf(seq.getName()) >= 0;
                String source = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
                okay &= !source.equals("");
                String type = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
                okay &= !type.equals("");
                String low = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
                okay &= !low.equals("");
                String high = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
                okay &= !high.equals("");
                String score = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
                okay &= !score.equals("");
                String strand = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
                okay &= !strand.equals("");
                okay &= (strand.equals("+") || strand.equals("-") || strand.equals("."));
                String phase = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
                okay &= !phase.equals("");
                okay &= (phase.equals("0") || phase.equals("1") || phase.equals("2") || phase.equals("."));
                String gene_id = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
                if (gene_id == null) gene_id = "";
                if (okay) {
                    if (!source.equals("")) {
                        program = source;
                        if (source.equals("GenieC")) {
                            if (splice_analysis == null) {
                                splice_analysis = new Analysis();
                                splice_analysis.setType("Splice");
                                splice_analysis.setProgram(program);
                                splice_analysis.setVersion(analysis.getVersion());
                                splice_analysis.setDate(analysis.getDate());
                                splice_analysis.setSequence(analysis.getSequence());
                            }
                        } else {
                            analysis.setProgram(program);
                        }
                    }
                    if (gene != null && !gene.getID().equals(gene_id)) {
                        gene.setScore(gene.getScore() / gene.getSpans().size());
                        analysis.addResult(gene);
                        gene = null;
                    }
                    if (type.equals("exon")) {
                        if (gene == null) {
                            gene = new ResultSet();
                            gene.setSequence(seq);
                            gene.setID(gene_id);
                        }
                        ResultSpan exon = new ResultSpan();
                        exon.setScore(score);
                        gene.setScore(gene.getScore() + exon.getScore());
                        int start, end;
                        try {
                            if (strand.equals("+") || strand.equals(".")) {
                                start = Integer.parseInt(low);
                                end = Integer.parseInt(high);
                            } else {
                                start = Integer.parseInt(high);
                                end = Integer.parseInt(low);
                            }
                            exon.setStart(start);
                            exon.setEnd(end);
                            exon.setFrame("" + (Integer.parseInt(phase) + 1));
                            gene.addSpan(exon);
                        } catch (NumberFormatException ex) {
                            throw new OutputMalFormatException("Error parsing low/high as number " + low + "/" + high, ex.getMessage(), ex);
                        } catch (Exception ex) {
                            System.out.println(ex.getMessage() + "can't parse" + line);
                            okay = false;
                        }
                    }
                    if (type.startsWith("splice") && source.equals("GenieC")) {
                        ResultSet splice = new ResultSet();
                        splice.setSequence(seq);
                        ResultSpan site = new ResultSpan();
                        site.setScore(score);
                        splice.setScore(score);
                        int start, end;
                        try {
                            if (strand.equals("+") || strand.equals(".")) {
                                start = Integer.parseInt(low);
                                end = Integer.parseInt(high);
                            } else {
                                start = Integer.parseInt(high);
                                end = Integer.parseInt(low);
                            }
                            site.setStart(start);
                            site.setEnd(end);
                            splice.addSpan(site);
                            if (type.equals("splice5")) {
                                splice.addOutput("splice", "donor");
                            } else {
                                splice.addOutput("splice", "acceptor");
                            }
                            splice_analysis.addResult(splice);
                        } catch (NumberFormatException ex) {
                            throw new OutputMalFormatException("Error parsing low/high as number " + low + "/" + high, ex.getMessage(), ex);
                        } catch (Exception ex) {
                            System.out.println(ex.getMessage() + "can't parse" + line);
                            okay = false;
                        }
                    }
                } else {
                    if (!line.startsWith("#") && !type.equals("sequence")) System.out.println(line + " cannot be parsed correctly");
                }
            }
            if (gene != null) {
                gene.setScore(gene.getScore() / gene.getSpans().size());
                analysis.addResult(gene);
            }
        } catch (OutputMalFormatException ex) {
            throw new OutputMalFormatException(ex.getMessage(), ex);
        } catch (Exception ex) {
            System.out.println(ex.getMessage() + "can't parse gff");
        }
        curation.addAnalysis(analysis);
        if (splice_analysis != null) {
            curation.addAnalysis(splice_analysis);
            System.out.println("Created new analysis for " + splice_analysis.getType());
        }
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
        boolean parseable = false;
        try {
            BufferedReader data = new BufferedReader(new InputStreamReader(in));
            String line = data.readLine();
            int i = 0;
            while (line != null && i < 10) {
                if (!line.startsWith("##gff-")) {
                    line = data.readLine();
                    i++;
                } else {
                    break;
                }
            }
            if (line != null && !line.equals("")) parseable = (line.startsWith("##gff-"));
            data.close();
        } catch (Exception ex) {
            System.out.println("Couldn't verify input stream as GFF data " + "and error: " + ex.getMessage());
        }
        return parseable;
    }
}
