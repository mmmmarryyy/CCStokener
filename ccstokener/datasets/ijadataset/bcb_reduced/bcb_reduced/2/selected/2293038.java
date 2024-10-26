package bop.parser;

import java.io.*;
import java.net.*;
import java.util.*;
import bop.datamodel.*;
import bop.exception.OutputMalFormatException;
import bop.parser.*;

public class FastaParser extends ResultParser {

    public boolean parseResults(URL url, String file_type, CurationI curation, Date file_date, String regexp) throws OutputMalFormatException {
        boolean parsed = false;
        try {
            InputStream data_stream = url.openStream();
            parsed = parseResults(data_stream, file_type, curation, file_date, regexp);
        } catch (OutputMalFormatException ex) {
            throw new OutputMalFormatException(ex.getMessage(), ex);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return parsed;
    }

    public boolean parseResults(InputStream data_stream, String file_type, CurationI curation, Date file_date, String regexp) throws OutputMalFormatException {
        boolean parsed = false;
        try {
            int index = 0;
            BufferedReader seq_input = new BufferedReader(new InputStreamReader(data_stream));
            String line;
            StringBuffer DNA_buffer = new StringBuffer();
            line = seq_input.readLine();
            if (!((line.substring(0, 1)).equals(">"))) {
                System.out.println("ERROR: sequence is not in FASTA format.  First line: " + line);
                return (parsed);
            }
            String seq_name = ((line.indexOf(" ") > 0) ? line.substring(1, line.indexOf(" ")) : line.substring(1));
            SequenceI seq = curation.getCuratedSeq(seq_name);
            try {
                RegexpAdapter ad = new RegexpAdapter(regexp, seq_name);
                String id = ad.group(1);
                seq.setID(id);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            while ((line = seq_input.readLine()) != null) {
                line = line.trim();
                DNA_buffer.append(line);
            }
            seq.setResidues(DNA_buffer.toString());
            System.gc();
            parsed = true;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return parsed;
    }

    public boolean canParse(String file_name) {
        boolean parseable = false;
        try {
            FileInputStream istream = new FileInputStream(file_name);
            parseable = canParse(istream);
        } catch (Exception ex) {
            System.out.println("Couldn't verify " + file_name + " as fasta data");
        }
        return parseable;
    }

    public boolean canParse(InputStream in) {
        boolean parseable = false;
        try {
            BufferedReader seq_input = new BufferedReader(new InputStreamReader(in));
            String line = seq_input.readLine();
            if (line != null && line.length() > 0) parseable = ((line.substring(0, 1)).equals(">"));
            seq_input.close();
        } catch (Exception ex) {
            System.out.println("Couldn't verify input stream as fasta data " + "and error: " + ex.getMessage());
        }
        return parseable;
    }

    public static void parseHeader(SequenceI seq, String fasta_header) {
        if (fasta_header == null || fasta_header.equals("")) {
            return;
        }
        String before = fasta_header;
        int index = fasta_header.indexOf(' ');
        if (index > 0) {
            seq.setDescription((fasta_header.substring(index)).trim());
            fasta_header = fasta_header.substring(0, index);
            if (fasta_header.equals("Drosophila")) {
                seq.setName(seq.getDescription());
                seq.setID(seq.getDescription());
                return;
            }
        }
        int lastPipeIndex = fasta_header.lastIndexOf('|');
        if (lastPipeIndex > 0 && lastPipeIndex != fasta_header.length() - 1) {
            seq.appendToDescription(fasta_header.substring(lastPipeIndex).trim());
        }
        while (fasta_header.lastIndexOf('|') == (fasta_header.length() - 1)) {
            fasta_header = fasta_header.substring(0, fasta_header.length() - 1);
        }
        Vector words = new Vector();
        while (fasta_header.length() > 0) {
            index = fasta_header.indexOf('|');
            if (index < 0) {
                addWord(fasta_header, words);
                fasta_header = "";
            } else {
                if (index > 0) {
                    addWord(fasta_header.substring(0, index), words);
                }
                index++;
                if (fasta_header.length() > index) {
                    fasta_header = fasta_header.substring(index);
                }
            }
        }
        if (words.size() == 1) {
            String word = (String) words.elementAt(0);
            seq.setName(word);
            seq.setID(word);
        } else {
            String word = (String) words.elementAt(0);
            if (word.equalsIgnoreCase("gb") || word.equalsIgnoreCase("emb") || word.equalsIgnoreCase("embl") || word.equalsIgnoreCase("ug") || word.equalsIgnoreCase("sptr") || word.equalsIgnoreCase("fb")) {
                if (word.equalsIgnoreCase("embl")) word = "emb";
                seq.setDatabase(word.toUpperCase());
                word = (String) words.elementAt(1);
                seq.setAccession(word);
                word = (words.size() > 2 ? (String) words.elementAt(2) : word);
                seq.setID(word);
                if (words.size() > 3) {
                    seq.setDescription(seq.getDescription() + " " + (String) words.elementAt(3));
                }
            } else if (word.equalsIgnoreCase("id")) {
                seq.setName((String) words.elementAt(1));
                if (words.size() > 3) {
                    seq.setDatabase((String) words.elementAt(2));
                    seq.setAccession((String) words.elementAt(3));
                    seq.setID(seq.getAccession());
                } else {
                    seq.setID(seq.getName());
                }
            } else if (word.toLowerCase().startsWith("anon") && words.size() > 1) {
                seq.setName(word + "-" + words.elementAt(1));
                seq.setID(seq.getName());
            } else if (word.equalsIgnoreCase("gadfly")) {
                seq.setName((String) words.elementAt(1));
                seq.setID((String) words.elementAt(1));
                seq.setDatabase((String) words.elementAt(2));
                seq.setAccession((String) words.elementAt(3));
                if (words.size() > 4) {
                    seq.setDescription(seq.getDescription() + " " + (String) words.elementAt(4));
                }
            } else {
                if ((((String) words.elementAt(1)).startsWith("FBgn")) || (((String) words.elementAt(1)).startsWith("FBan"))) {
                    seq.setName(word);
                    seq.setDatabase("FB");
                    seq.setAccession((String) words.elementAt(1));
                    seq.setID((String) words.elementAt(2));
                } else {
                    seq.setName(word);
                    seq.setID(word);
                    System.err.println("Unable to parse " + before);
                }
            }
        }
    }

    private static void addWord(String word, Vector words) {
        if (!word.equalsIgnoreCase("gi") && !word.equalsIgnoreCase("gnl")) words.addElement(word);
    }
}
