package org.bdgp.parser;

import java.net.*;
import java.util.*;
import java.io.*;
import java.applet.*;
import org.xml.sax.*;
import org.bdgp.datamodel.*;

/**
 * WARNING -- AElfred (and other SAX drivers) _may_ break large 
 * stretches of unmarked content into smaller chunks and call 
 * characters() for each smaller chunk
 * CURRENT IMPLEMENTATION DOES NOT DEAL WITH THIS 
 * COULD CAUSE PROBLEM WHEN READING IN SEQUENCE RESIDUES
 * haven't seen a problem yet though -- GAH 6-15-98
 */
public class GameXML_Converter implements DocumentHandler {

    /** 
   *  boolean for trying calls to OutputStream.flush() to avoid 
   *  occasional weird extra return/formfeed/newline characters 
   *  when outputting large XML docs
   */
    boolean USE_FLUSH = true;

    boolean debug = false;

    org.xml.sax.Parser xml_parser = null;

    String default_parser_name = "com.microstar.xml.SAXDriver";

    int elements = 0;

    String quoter = "\"";

    String indenter = "    ";

    /**
   *  the Java object corresponding to the "current" meaningful element
   *  Note that it is possible for an element to be in the document for
   *  structural purposes but have no corresponding Java class
   *
   *  might want to rename parent_obj, a more accurate description
   */
    XML_ParserI current_model = null;

    String current_element;

    Curation curation = null;

    Sequence annot_seq = null;

    /**
   *  Hashtables for writing out XML,
   *     to hold referenced objects so they can be added at end of XML doc
   *     if not already written
   *  Maybe should fold these in with obj_by_id and/or ref_resolver, the
   *     Hashtables used for reading XML...
   */
    Hashtable write_refhash;

    Hashtable write_donehash;

    /**
   *  Needed for flattening out a Java object network to an XML document
   *      via writeXML() call
   *  a mapping of objects to assigned unique document ids for objects that
   *     don't have their own unique ids but are referenced by other
   *     elements/objects
   *  Note that docids are guaranteed to be unique _only_ within the scope
   *     of the XML document being created via writeXML(), and that an
   *     object's docid may change across multiple XML documents
   */
    Hashtable id_to_model = new Hashtable();

    Stack model_chain;

    Stack element_chain;

    static Hashtable modeled_elements;

    static {
        modeled_elements = new Hashtable();
        modeled_elements.put("game", "Curation");
        modeled_elements.put("annotation", "Annotation");
        modeled_elements.put("evidence", "Evidence");
        modeled_elements.put("feature_set", "FeatureSet");
        modeled_elements.put("feature_span", "FeatureSpan");
        modeled_elements.put("map_position", "FragmentSpan");
        modeled_elements.put("computational_analysis", "Analysis");
        modeled_elements.put("result_set", "ResultSet");
        modeled_elements.put("result_span", "ResultSpan");
        modeled_elements.put("seq", "Sequence");
        modeled_elements.put("dbxref", "DB_xref");
        modeled_elements.put("aspect", "Aspect");
        modeled_elements.put("gene", "Gene");
        modeled_elements.put("AnnotatedSequence", "Sequence");
        modeled_elements.put("Residues", "Sequence");
        modeled_elements.put("Sequence", "Sequence");
        modeled_elements.put("Annotation", "FeatureSet");
        modeled_elements.put("StructureSpan", "FeatureSpan");
        modeled_elements.put("SimilaritySpan", "FeatureSpan");
        modeled_elements.put("AnalysisResults", "Analysis");
        modeled_elements.put("Result", "ResultSet");
        modeled_elements.put("PredictSpan", "ResultSpan");
        modeled_elements.put("Alignment", "AlignSet");
        modeled_elements.put("AlignSpan", "AlignSpan");
    }

    public GameXML_Converter() {
        super();
    }

    public String writeXML(Curation curation) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        writeXML(curation, bos);
        return bos.toString();
    }

    public void writeXML(Curation curation, OutputStream os) {
        DataOutputStream dos;
        write_donehash = new Hashtable();
        write_refhash = new Hashtable();
        String indent0, indent1, indent2;
        indent0 = "";
        indent1 = indent0 + indenter;
        indent2 = indent1 + indenter;
        if (os instanceof DataOutputStream) {
            dos = (DataOutputStream) os;
        } else {
            dos = new DataOutputStream(os);
        }
        try {
            int ref_count = 0;
            Sequence curated_seq = curation.getCuratedSeq();
            dos.writeBytes(writeGameBegin(indent0, curated_seq.getName()));
            dos.writeBytes(curated_seq.toXML(" focus=\"true\""));
            Vector annotations = curation.getAnnotations();
            for (int i = 0; i < annotations.size(); i++) {
                Annotation annot = (Annotation) annotations.elementAt(i);
                dos.writeBytes(annot.toXML());
            }
            if (USE_FLUSH) {
                dos.flush();
            }
            Vector analyses = curation.getAnalyses();
            for (int i = 0; i < analyses.size(); i++) {
                Analysis analysis = (Analysis) analyses.elementAt(i);
                System.out.println("Dumping " + analysis.getName() + " with " + analysis.getResults().size() + " results");
                dos.writeBytes(analysis.toXML());
            }
            if (USE_FLUSH) {
                dos.flush();
            }
            Vector seqs = curation.getSequences();
            for (int i = 0; i < seqs.size(); i++) {
                Sequence seq = (Sequence) seqs.elementAt(i);
                if (seq != curated_seq) dos.writeBytes(seq.toXML(""));
            }
            if (USE_FLUSH) {
                dos.flush();
            }
            dos.writeBytes(writeGameEnd(indent0));
        } catch (IOException ex) {
            System.err.println("Caught IOException in XML_game.writeXML");
        }
    }

    public String writeGameBegin(String indent, String seq_name) {
        StringBuffer buf = new StringBuffer();
        buf.append(indent + "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
        buf.append("\n");
        buf.append(indent + "<!-- DOCTYPE game SYSTEM \"GAME.dtd\" -->\n");
        buf.append("\n");
        buf.append("<game>\n");
        buf.append("  <!-- pipeline results filtered by bop.java -->\n");
        buf.append("  <!-- Analysis of: " + seq_name + " -->\n");
        buf.append("  <!-- autogenerated on " + (new Date()).toString() + " -->\n");
        return buf.toString();
    }

    public String writeGameEnd(String indent) {
        return (indent + "</game>\n");
    }

    public void readXML(Curation curation, String doc_url_string) {
        URL doc_url = null;
        try {
            doc_url = new URL(doc_url_string);
            readXML(curation, doc_url);
        } catch (Exception ex1) {
            try {
                InputStream xml_stream = new FileInputStream(doc_url_string);
                BufferedInputStream bis = new BufferedInputStream(xml_stream);
                readXML(curation, bis);
            } catch (Exception ex2) {
                System.err.println("caught Exception in readXML(doc_url_string): ");
                System.out.println(ex2.getMessage());
                ex2.printStackTrace();
            }
        }
    }

    /**
   * Parse an XML document -- GAH 5-12-98
   */
    public boolean readXML(Curation curation, URL doc_url) {
        InputStream is = null;
        BufferedInputStream bis = null;
        try {
            is = doc_url.openStream();
        } catch (Exception ex) {
            return false;
        }
        try {
            bis = new BufferedInputStream(is);
        } catch (Exception ex) {
            System.err.println("caught Exception in BufferedInputStream for " + doc_url + ": " + ex);
            return false;
        }
        try {
            readXML(curation, bis);
        } catch (Exception ex) {
            System.err.println("Failed to read XML from " + doc_url + ": " + ex);
            return false;
        }
        return true;
    }

    /** 
   *  reads an Curation XML document from an InputStream and 
   *  returns an Curation derived from the XML document
   */
    public void readXML(Curation curation, InputStream istream) {
        this.curation = curation;
        this.annot_seq = curation.getCuratedSeq();
        model_chain = new Stack();
        element_chain = new Stack();
        try {
            if (xml_parser == null) {
                xml_parser = (org.xml.sax.Parser) Class.forName(default_parser_name).newInstance();
            }
        } catch (Exception e) {
            System.err.println("Fatal Error in xml_parser new: " + e.getMessage());
        }
        try {
            xml_parser.setDocumentHandler(this);
        } catch (Exception e) {
            System.err.println("Fatal Error in xml_parser.setDocumentHandler: " + e.getMessage());
        }
        try {
            xml_parser.parse(new InputSource(istream));
        } catch (Exception e) {
            System.err.println("Fatal Error near element # " + elements + ", " + " : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setParser(org.xml.sax.Parser xml_parser) {
        this.xml_parser = xml_parser;
    }

    public void startElement(String name, AttributeList atts) {
        elements++;
        try {
            String model_name = (String) modeled_elements.get(name);
            if (model_name == null) {
                if (!current_model.startSubElement(name, atts, id_to_model)) {
                    System.out.println(current_model.getClass().getName() + " Couldn't parse element " + name);
                }
            } else {
                if (current_model != null) model_chain.push(current_model);
                current_model = map_ID_to_Model(model_name, atts, "id");
                if (current_model == null) {
                    Class model_class = Class.forName("org.bdgp.datamodel." + model_name);
                    current_model = (XML_ParserI) model_class.newInstance();
                }
                preLink(current_model);
                current_model.setAttributes(atts, id_to_model);
                if (current_model instanceof Sequence) {
                    Sequence s = (Sequence) current_model;
                    if (s.focusOn() && annot_seq == null) {
                        annot_seq = s;
                        curation.addCuratedSeq(s);
                    }
                }
            }
            current_element = name;
            element_chain.push(current_element);
        } catch (Exception ex) {
            System.err.println("Exception parsing " + name + " in current model " + current_model + " " + ex.getMessage());
            ex.printStackTrace();
            System.exit(0);
        }
    }

    public void endElement(String name) {
        if (modeled_elements.get(name) != null && !model_chain.empty()) {
            postLink(current_model);
            current_model = (XML_ParserI) model_chain.pop();
        } else {
            current_model.endSubElement(name);
        }
        current_element = (String) element_chain.pop();
    }

    public void characters(char ch[], int start, int length) {
        String char_data;
        if ((current_element.equals("Residues")) || (current_element.equals("residues"))) {
            char_data = XML_util.filterWhiteSpace(ch, start, length);
            if (char_data != null && !char_data.equals("")) {
                annot_seq.setResidues(char_data);
            }
        } else {
            char_data = XML_util.trimWhiteSpace(ch, start, length);
            if (char_data != null && !char_data.equals("")) current_model.setCharacters(current_element, char_data, id_to_model);
        }
    }

    public void ignorableWhitespace(char ch[], int start, int length) {
    }

    public void startDocument() {
    }

    public void endDocument() {
    }

    public void doctype(String name, String publicID, String systemID) {
    }

    public void processingInstruction(String name, String remainder) {
    }

    public void setDocumentLocator(Locator locator) {
    }

    public void printAttributes(AttributeList atts) {
        int max = atts.getLength();
        String allatts = "     Attributes: ";
        String aname, special, curatt;
        if (max == 0) {
            System.err.println("     [Attributes not available]");
        }
        for (int i = 0; i < max; i++) {
            aname = atts.getName(i);
            special = atts.getType(i);
            if (special == null) {
                special = "";
            }
            curatt = (aname + "=\"" + atts.getValue(aname) + '"' + special);
            allatts = allatts + curatt + ", ";
        }
        System.err.println(allatts);
    }

    private void preLink(Object current_model) {
        if (current_model instanceof Gene) {
            Gene g = (Gene) current_model;
            g.setAnnotation((Annotation) model_chain.peek());
        } else if (current_model instanceof FeatureSet) {
            FeatureSet fs = (FeatureSet) current_model;
            fs.setSequence(annot_seq);
        } else if (current_model instanceof FeatureSpan) {
            FeatureSpan span = (FeatureSpan) current_model;
            span.makePartOf((FeatureSet) model_chain.peek());
        } else if (current_model instanceof Analysis) {
            Analysis analysis = (Analysis) current_model;
            analysis.setSequence(annot_seq);
        } else if (current_model instanceof ResultSet) {
            ResultSet result = (ResultSet) current_model;
            result.setAnalysis((Analysis) model_chain.peek());
            result.setSequence(annot_seq);
        } else if (current_model instanceof ResultSpan) {
            ResultSpan span = (ResultSpan) current_model;
            span.setParentResult((ResultSet) model_chain.peek());
        }
    }

    private void postLink(Object current_model) {
        if (current_model instanceof Sequence) {
            curation.addSequence((Sequence) current_model);
            XML_ParserI parent = (XML_ParserI) model_chain.peek();
            if (parent != null && parent instanceof Evidence) ((Evidence) parent).setHomologousSeq((Sequence) current_model);
        } else if (current_model instanceof Annotation) {
            curation.addAnnotation((Annotation) current_model);
        } else if (current_model instanceof FragmentSpan) {
            curation.addFragment((FragmentSpan) current_model);
        } else if (current_model instanceof FeatureSet) {
            Annotation annot = (Annotation) model_chain.peek();
            annot.addFeature((FeatureSet) current_model);
        } else if (current_model instanceof FeatureSpan) {
            FeatureSet parent = (FeatureSet) model_chain.peek();
            parent.addSpan((FeatureSpan) current_model);
        } else if (current_model instanceof Analysis) {
            curation.addAnalysis((Analysis) current_model);
            annot_seq.addAnalysis((Analysis) current_model);
        } else if (current_model instanceof ResultSet) {
            Analysis analysis = (Analysis) model_chain.peek();
            analysis.addResult((ResultSet) current_model);
        } else if (current_model instanceof ResultSpan) {
            ResultSet parent = (ResultSet) model_chain.peek();
            parent.addSpan((ResultSpan) current_model);
        } else if (current_model instanceof Evidence) {
            XML_ParserI parent = (XML_ParserI) model_chain.peek();
            if (parent instanceof FeatureSpan) ((FeatureSpan) parent).addEvidence((Evidence) current_model); else if (parent instanceof FeatureSet) ((FeatureSet) parent).addEvidence((Evidence) current_model);
        } else if (current_model instanceof Aspect) {
            Annotation parent = (Annotation) model_chain.peek();
            parent.addAspect((Aspect) current_model);
        } else if (current_model instanceof DB_xref) {
            XML_ParserI parent_model = (XML_ParserI) model_chain.peek();
            if (!(parent_model instanceof DB_xrefI)) System.out.println("DB_xref used inappropriately for " + parent_model.toString()); else {
                DB_xrefI parent = (DB_xrefI) parent_model;
                parent.setDBxref((DB_xref) current_model);
            }
        }
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    private XML_ParserI map_ID_to_Model(String model_name, AttributeList atts, String id_attribute) {
        XML_ParserI the_model = null;
        if (model_name.equals("Curation")) {
            the_model = this.curation;
        } else {
            String the_id = atts.getValue(id_attribute);
            if (the_id != null) {
                String model_key = model_name + the_id;
                the_model = (XML_ParserI) id_to_model.get(model_key);
                if (the_model == null) {
                } else {
                    String this_model_name = the_model.getClass().getName();
                    if (!this_model_name.endsWith(model_name)) {
                        the_model = null;
                        System.err.println("Drat the id " + the_id + " has been used for both " + this_model_name + " and " + model_name);
                    }
                }
            }
        }
        return (the_model);
    }
}
