package bop.parser;

import java.io.*;
import java.util.*;
import java.text.*;
import java.net.URL;
import bop.datamodel.*;
import bop.parser.*;
import bop.exception.OutputMalFormatException;

public class GenscanParser extends ResultParser {

    String program;

    String version;

    String matrix;

    Sequence seq;

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
            ResultSpan span = null;
            String line;
            String value;
            BufferedReader data = new BufferedReader(new InputStreamReader(data_stream));
            boolean none = false;
            boolean found_hit = false;
            line = data.readLine();
            while (line != null) {
                if (!line.startsWith("GENSCAN")) line = data.readLine(); else break;
            }
            if (!line.startsWith("GENSCAN")) {
                program = null;
                return parsed;
            }
            program = line.substring(0, "GENSCAN".length());
            version = line.substring("GENSCAN ".length());
            int index = version.indexOf('\t');
            if (index >= 0) {
                version = version.substring(0, index);
            }
            AnalysisI analysis = curation.getAnalysis(analysis_type);
            boolean new_analysis = (analysis == null);
            if (new_analysis) {
                analysis = new Analysis();
                analysis.setType(analysis_type);
            }
            analysis.setDate(grabDate(line));
            analysis.setProgram(program);
            analysis.setVersion(version);
            seq = grabSequence(data, regexp);
            analysis.setSequence(seq);
            if (new_analysis) {
                curation.addAnalysis(analysis);
                System.out.println("Created new analysis for " + analysis.getType());
            }
            matrix = grabMatrix(data);
            grabGenes(data, analysis);
            parsed = true;
            data.close();
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

    private Date grabDate(String line) throws OutputMalFormatException {
        Date date = null;
        String key = "Date run: ";
        String value = null;
        int index = line.indexOf(key);
        if (index >= 0) {
            value = line.substring(index + key.length());
            index = value.indexOf("\t");
            if (index >= 0) {
                value = value.substring(0, index);
            }
            index = value.lastIndexOf('-');
            if (value.length() - (index + 1) > 2) {
                value = (value.substring(0, index + 1) + value.substring(value.length() - 2));
            }
            try {
                SimpleDateFormat df = new SimpleDateFormat("d-MMM-yy");
                date = df.parse(value);
            } catch (ParseException ex) {
                throw new OutputMalFormatException("Error when parsing line (look for date) " + line, ex.getMessage(), ex);
            } catch (Exception ex) {
                System.out.println("could not parse genscan run date\n" + line + "\n" + ex.getMessage());
                ex.printStackTrace();
            }
        }
        return date;
    }

    private String findKeyValue(BufferedReader data, String goal) {
        String line = "";
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
                } else {
                    line = data.readLine();
                }
            }
        } catch (Exception ex) {
            System.out.println("Could not find " + goal + " in " + line);
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return value;
    }

    private Sequence grabSequence(BufferedReader data, String regexp) {
        Sequence seq = null;
        String value = findKeyValue(data, "Sequence ");
        if (value != null) {
            seq = new Sequence();
            bop.parser.FastaParser.parseHeader(seq, value);
            try {
                RegexpAdapter ad = new RegexpAdapter(regexp, value);
                String id = ad.group(1);
                seq.setID(id);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            int index = value.indexOf(" : ");
            String seq_name = (index >= 0) ? value.substring(0, index) : value;
            seq.setName(seq_name);
            value = value.substring(index + " : ".length());
            index = value.indexOf(" bp : ");
            String seq_length = ((index >= 0) ? value.substring(0, index) : value);
            seq.setLength(seq_length);
            value = value.substring(index + " bp : ".length());
            seq.setDescription(value);
        }
        if (seq == null) {
            System.out.println("Could not parse out Genscane Sequence");
        }
        return seq;
    }

    private String grabMatrix(BufferedReader data) {
        return (findKeyValue(data, "Parameter matrix: "));
    }

    private void grabGenes(BufferedReader data, AnalysisI analysis) {
        String line;
        String value = findKeyValue(data, "Gn.Ex");
        if (!value.equals("")) {
            boolean found = false;
            try {
                ResultSet gene = null;
                while ((line = data.readLine()) != null && !found) {
                    if (line.startsWith("Predicted peptide ")) {
                        found = true;
                    } else if (!(line.startsWith("----- "))) {
                        line = line.trim();
                        if (line.length() == 0) {
                            if (gene != null && gene.getSpans().size() > 0) {
                                analysis.addResult(gene);
                            }
                            gene = new ResultSet();
                            gene.setSequence(seq);
                        } else {
                            grabExon(gene, line);
                        }
                    }
                }
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private void grabExon(ResultSet gene, String line) throws OutputMalFormatException {
        String type = "";
        StringTokenizer tokens;
        boolean valid = true;
        tokens = new StringTokenizer(line);
        ResultSpan span = new ResultSpan();
        if (valid &= tokens.hasMoreElements()) {
            String gene_id = tokens.nextToken();
            gene.setID(gene_id);
        }
        if (valid &= tokens.hasMoreElements()) {
            type = tokens.nextToken();
            if (!type.equals("Intr")) span.setType(type); else span.setType("Exon");
        }
        if (valid &= tokens.hasMoreElements()) {
            tokens.nextToken();
        }
        if (valid &= tokens.hasMoreElements()) {
            span.setSeqStart(tokens.nextToken());
        }
        if (valid &= tokens.hasMoreElements()) {
            span.setSeqEnd(tokens.nextToken());
        }
        if (valid &= tokens.hasMoreElements()) {
            tokens.nextToken();
        }
        if (!type.equals("PlyA") && !type.equals("Prom")) {
            if (valid &= tokens.hasMoreElements()) {
                tokens.nextToken();
            }
            if (valid &= tokens.hasMoreElements()) {
                tokens.nextToken();
            }
            if (valid &= tokens.hasMoreElements()) {
                span.setOutput("acceptor_score", tokens.nextToken());
            }
            if (valid &= tokens.hasMoreElements()) {
                span.setOutput("donor_score", tokens.nextToken());
            }
            if (valid &= tokens.hasMoreElements()) {
                span.setOutput("coding_potential", tokens.nextToken());
            }
            if (valid &= tokens.hasMoreElements()) {
                String p = tokens.nextToken();
                if (p.toLowerCase().equals("nan")) {
                    p = "0.0001";
                }
                if (p.toLowerCase().equals("nan")) {
                    valid = false;
                } else {
                    double prob = 0;
                    try {
                        prob = (Double.valueOf(p).doubleValue()) * 100;
                    } catch (RuntimeException ex) {
                        throw new OutputMalFormatException("Error when parsing line (look for double value) " + line, ex.getMessage(), ex);
                    } catch (Exception ex) {
                        System.out.println("Unable to parse " + p + " from " + line);
                        System.out.println(ex.getMessage());
                    }
                    if (prob != 0) {
                        span.setOutput("prob", p);
                        if (prob > gene.getScore()) gene.setScore(prob);
                    } else {
                        valid = false;
                    }
                }
            }
        }
        if (valid &= tokens.hasMoreElements()) {
            span.setOutput("total_score", tokens.nextToken());
        }
        if (valid && !type.equals("PlyA") && !type.equals("Prom")) {
            gene.addSpan(span);
        }
    }

    public boolean canParse(String file_name) {
        boolean parseable = false;
        try {
            FileInputStream istream = new FileInputStream(file_name);
            parseable = canParse(istream);
        } catch (Exception ex) {
            System.out.println("Couldn't verify " + file_name + " as genscan data");
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
                if (!line.startsWith("GENSCAN")) {
                    line = data.readLine();
                    i++;
                } else break;
            }
            if (line != null) parseable = line.startsWith("GENSCAN");
            data.close();
        } catch (Exception ex) {
            System.out.println("Couldn't verify input stream as GENSCAN data " + "and error: " + ex.getMessage());
        }
        return parseable;
    }
}
