package apollo.dataadapter;

import org.omg.CORBA.ORB;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import org.apollo.datamodel.*;
import org.apollo.session.*;
import org.apollo.exceptions.*;
import apollo.gui.AnnotationChangeLog;
import apollo.datamodel.*;
import apollo.dataadapter.debug.*;
import org.bdgp.io.*;
import org.bdgp.util.ProgressEvent;
import org.bdgp.util.Properties;

public class CORBAAdapter extends AbstractDataAdapter implements ApolloDataAdapterI {

    private String path;

    private Param[] sessParams;

    private Session mySess;

    private AnnotatedRegion annSeq;

    private String extId;

    private SequenceI genome_seq = null;

    private CurationSet curation_set;

    private double currentProgress;

    private double progressSectionSize;

    IOOperation[] supportedOperations = { ApolloDataAdapterI.OP_READ_DATA, ApolloDataAdapterI.OP_WRITE_DATA, ApolloDataAdapterI.OP_READ_SEQUENCE, ApolloDataAdapterI.OP_READ_RAW_ANALYSIS };

    public CORBAAdapter() {
    }

    public CORBAAdapter(String path, Param[] sessParams) throws DataAdapterException {
        setupAdapter(path, sessParams);
    }

    public CORBAAdapter(String path) throws DataAdapterException {
        this(path, getDefaultParams());
    }

    public CORBAAdapter(String path, Hashtable options) throws DataAdapterException {
        this(path, hashToParams(options));
    }

    public void init() {
    }

    public String getName() {
        return "CORBA Adapter";
    }

    public String getType() {
        return "Apollo CORBA Source";
    }

    public IOOperation[] getSupportedOperations() {
        return supportedOperations;
    }

    public DataAdapterUI getUI(IOOperation op) {
        if (op.equals(ApolloDataAdapterI.OP_READ_DATA) || op.equals(ApolloDataAdapterI.OP_WRITE_DATA)) return new CORBAAdapterGUI(op); else return null;
    }

    public Properties getStateInformation() {
        Properties props = new Properties();
        props.put("path", path);
        boolean useXML = false;
        for (int i = 0; i < sessParams.length; i++) {
            if (sessParams[i].name.equalsIgnoreCase("source") && sessParams[i].value.equalsIgnoreCase("xml")) {
                useXML = true;
                break;
            }
        }
        props.put("usexml", useXML + "");
        return props;
    }

    public void setStateInformation(Properties props) {
        path = props.getProperty("path");
        Param[] params;
        if (props.getProperty("useXML") != null && props.getProperty("useXML").equals("true")) {
            params = new Param[1];
            params[0] = new Param("source", "xml");
        } else params = new Param[0];
        try {
            setupAdapter(path, params);
        } catch (DataAdapterException e) {
            System.err.println("Couldn't initialize adapter " + "because of exception");
            e.printStackTrace();
        }
    }

    public void setupAdapter(String path) throws DataAdapterException {
        setupAdapter(path, getDefaultParams());
    }

    public void setupAdapter(String path, Param[] sessParams) throws DataAdapterException {
        this.path = path;
        this.sessParams = sessParams;
        mySess = getSession();
    }

    private static Param[] getDefaultParams() {
        Param[] out = new Param[1];
        out[0] = new Param("source", "xml");
        return out;
    }

    private static Param[] hashToParams(Hashtable options) {
        String key;
        Enumeration e = options.keys();
        Param[] params = new Param[options.size()];
        int i = 0;
        while (e.hasMoreElements()) {
            key = (String) e.nextElement();
            Param p = new Param(key, (String) options.get(key));
            params[i] = p;
            i++;
            System.out.println(i + " " + key + "=" + (String) options.get(key));
        }
        return params;
    }

    public void setRegion(String extId) throws DataAdapterException {
        this.extId = extId;
        try {
            AnnotatedRegion annSeq = mySess.get_AnnotatedRegion(extId, "");
            setRegion(extId, annSeq);
        } catch (Exception e) {
            throw new DataAdapterException("Load failed. Are you sure " + extId + "is a real sequence?", e);
        }
    }

    public void setRegion(String extId, long start, long end) throws DataAdapterException {
        this.extId = extId;
        try {
            AnnotatedRegion annSeq = mySess.get_AnnotatedSubRegion(extId, (int) start, (int) end, "");
            setRegion(extId, annSeq);
        } catch (Exception e) {
            throw new DataAdapterException("Load failed. Are you sure " + extId + "is a real sequence?", e);
        }
    }

    public void setRegion(String extId, AnnotatedRegion annSeq) {
        this.extId = extId;
        this.annSeq = annSeq;
    }

    public void commitChanges(apollo.datamodel.CurationSet curation_set) throws DataAdapterException {
        try {
            this.curation_set = curation_set;
            AnnotationChangeLog log = curation_set.getAnnotationChangeLog();
            Vector addedGenes = new Vector();
            Vector deletedGenes = new Vector();
            Vector changedGenes = new Vector();
            Vector addedAnnot = new Vector();
            Vector deletedAnnot = new Vector();
            Vector changedAnnot = new Vector();
            for (int i = 0; i < log.getAddedFeatures().size(); i++) {
                GenericAnnotationI annot = (GenericAnnotationI) log.getAddedFeatures().elementAt(i);
                if (annot instanceof Gene) {
                    addedGenes.addElement(getCORBAGene((Gene) annot));
                } else {
                    addedAnnot.addElement(getCORBAGenericAnnotation(annot));
                }
            }
            for (int i = 0; i < log.getChangedFeatures().size(); i++) {
                GenericAnnotationI annot = (GenericAnnotationI) log.getChangedFeatures().elementAt(i);
                if (annot instanceof Gene) {
                    changedGenes.addElement(getCORBAGene((Gene) annot));
                } else {
                    changedAnnot.addElement(getCORBAGenericAnnotation(annot));
                }
            }
            for (int i = 0; i < log.getDeletedFeatures().size(); i++) {
                GenericAnnotationI annot = (GenericAnnotationI) log.getDeletedFeatures().elementAt(i);
                if (annot instanceof Gene) {
                    deletedGenes.addElement(getCORBAGene((Gene) annot));
                } else {
                    deletedAnnot.addElement(getCORBAGenericAnnotation(annot));
                }
            }
            org.apollo.datamodel.GenericAnnotation[] addedGeneric = new org.apollo.datamodel.GenericAnnotation[addedAnnot.size()];
            for (int i = 0; i < addedAnnot.size(); i++) addedGeneric[i] = (org.apollo.datamodel.GenericAnnotation) addedAnnot.elementAt(i);
            org.apollo.datamodel.GenericAnnotation[] deletedGeneric = new org.apollo.datamodel.GenericAnnotation[deletedAnnot.size()];
            for (int i = 0; i < deletedAnnot.size(); i++) deletedGeneric[i] = (org.apollo.datamodel.GenericAnnotation) deletedAnnot.elementAt(i);
            org.apollo.datamodel.GenericAnnotation[] changedGeneric = new org.apollo.datamodel.GenericAnnotation[changedAnnot.size()];
            for (int i = 0; i < changedAnnot.size(); i++) changedGeneric[i] = (org.apollo.datamodel.GenericAnnotation) changedAnnot.elementAt(i);
            AnnotatedGene[] addedGene = new AnnotatedGene[addedGenes.size()];
            for (int i = 0; i < addedGenes.size(); i++) addedGene[i] = (org.apollo.datamodel.AnnotatedGene) addedGenes.elementAt(i);
            AnnotatedGene[] deletedGene = new AnnotatedGene[deletedGenes.size()];
            for (int i = 0; i < deletedGenes.size(); i++) deletedGene[i] = (org.apollo.datamodel.AnnotatedGene) deletedGenes.elementAt(i);
            AnnotatedGene[] changedGene = new AnnotatedGene[changedGenes.size()];
            for (int i = 0; i < changedGenes.size(); i++) changedGene[i] = (org.apollo.datamodel.AnnotatedGene) changedGenes.elementAt(i);
            annSeq.save_AnnotatedGenes(addedGene, changedGene, deletedGene);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAdapterException("Error during save: " + getExceptionDescription(e), e);
        }
    }

    public String getExceptionDescription(Exception e) {
        if (e instanceof ProcessError) return e.toString() + ", reason = " + ((ProcessError) e).reason; else if (e instanceof org.omg.CORBA.UNKNOWN) return e.toString() + ", reason code = " + ((org.omg.CORBA.UNKNOWN) e).minor; else return e.toString();
    }

    public org.apollo.datamodel.GenericAnnotation getCORBAGenericAnnotation(GenericAnnotationI in) {
        org.apollo.datamodel.GenericAnnotation out = new org.apollo.datamodel.GenericAnnotation();
        out.ident = getCORBAIdentifier(in.getIdentifier());
        out.type = in.getType();
        out.qualifiers = new org.apollo.datamodel.Param[0];
        out.range = getCORBARange(in);
        org.apollo.datamodel.Evidence[] evidence = new org.apollo.datamodel.Evidence[in.getEvidence().size()];
        for (int i = 0; i < evidence.length; i++) {
            evidence[i] = getCORBAEvidence(new apollo.datamodel.Evidence((String) in.getEvidence().elementAt(i)));
        }
        out.evidence_list = evidence;
        org.apollo.datamodel.Comment[] comments = new org.apollo.datamodel.Comment[in.getComments().size()];
        for (int i = 0; i < in.getComments().size(); i++) {
            apollo.datamodel.Comment c = (apollo.datamodel.Comment) in.getComments().elementAt(i);
            comments[i] = getCORBAComment(c);
        }
        out.comments = comments;
        return out;
    }

    public org.apollo.datamodel.Identifier getCORBAIdentifier(apollo.datamodel.Identifier in) {
        org.apollo.datamodel.Identifier ident = new org.apollo.datamodel.Identifier();
        ident.name = in.getName();
        ident.description = in.getDescription();
        if (ident.description == null) {
            ident.description = "";
        }
        String[] synonyms = new String[in.getSynonyms().size()];
        for (int i = 0; i < synonyms.length; i++) synonyms[i] = (String) in.getSynonyms().elementAt(i);
        ident.synonyms = synonyms;
        org.apollo.datamodel.DbXref[] refs = new org.apollo.datamodel.DbXref[in.getdbXrefs().size()];
        for (int i = 0; i < refs.length; i++) {
            apollo.datamodel.DbXref ref = (apollo.datamodel.DbXref) in.getdbXrefs().elementAt(i);
            refs[i].idtype = ref.getIdType();
            refs[i].idvalue = ref.getIdValue();
            refs[i].dbname = ref.getDbName();
        }
        ident.dbxrefs = refs;
        return ident;
    }

    public org.apollo.datamodel.Comment getCORBAComment(apollo.datamodel.Comment in) {
        org.apollo.datamodel.Comment out = new org.apollo.datamodel.Comment();
        out.comment_id = in.getId();
        out.text = in.getText();
        if (in.isInternal()) out.text = "INTERNAL\n" + in.getText(); else out.text = "EXTERNAL\n" + in.getText();
        Person person = new Person();
        person.readable_name = in.getPerson();
        person.person_id = in.getPerson();
        out.person = person;
        out.time = (int) in.getTimeStamp();
        return out;
    }

    public org.apollo.datamodel.Evidence getCORBAEvidence(apollo.datamodel.Evidence in) {
        org.apollo.datamodel.Evidence out = new org.apollo.datamodel.Evidence();
        out.result_span_id = in.getFeatureId();
        out.type = in.getTypeAsString();
        out.result_set_id = in.getSetId();
        return out;
    }

    public org.apollo.datamodel.Range getCORBARange(SeqFeatureI in) {
        Range range = new Range();
        range.range_min = (int) in.getLow();
        range.range_max = (int) in.getHigh();
        range.sequence_id = extId;
        if (in.getStrand() < 0) {
            range.strand = StrandType.minus;
            System.out.println("Minus");
        } else if (in.getStrand() > 0) {
            range.strand = StrandType.plus;
            System.out.println("Plus");
        } else {
            range.strand = StrandType.nulltype;
            System.out.println("Null");
        }
        return range;
    }

    public org.apollo.datamodel.Exon getCORBAExon(apollo.datamodel.ExonI in) {
        org.apollo.datamodel.Exon out = new org.apollo.datamodel.Exon();
        out.ident = getCORBAIdentifier(in.getIdentifier());
        out.type = in.getType();
        out.range = getCORBARange(in);
        org.apollo.datamodel.Evidence[] evidence = new org.apollo.datamodel.Evidence[in.getEvidence().size()];
        for (int i = 0; i < evidence.length; i++) {
            evidence[i] = getCORBAEvidence((apollo.datamodel.Evidence) in.getEvidence().elementAt(i));
        }
        out.evidence_list = evidence;
        return out;
    }

    public org.apollo.datamodel.Transcript getCORBATranscript(apollo.datamodel.Transcript in) {
        org.apollo.datamodel.Transcript out = new org.apollo.datamodel.Transcript();
        out.ident = getCORBAIdentifier(in.getIdentifier());
        org.apollo.datamodel.Exon[] exons = new org.apollo.datamodel.Exon[in.getExons().size()];
        for (int i = 0; i < in.getExons().size(); i++) {
            apollo.datamodel.ExonI e = (apollo.datamodel.ExonI) in.getExons().elementAt(i);
            exons[i] = getCORBAExon(e);
            System.err.println("attaching exon :" + exons[i]);
        }
        out.exons = exons;
        out.cds_range = getCORBARange(in);
        org.apollo.datamodel.Evidence[] evidence = new org.apollo.datamodel.Evidence[in.getEvidence().size()];
        for (int i = 0; i < evidence.length; i++) {
            evidence[i] = getCORBAEvidence(new apollo.datamodel.Evidence((String) in.getEvidence().elementAt(i)));
        }
        out.evidence_list = evidence;
        org.apollo.datamodel.Comment[] comments = new org.apollo.datamodel.Comment[in.getComments().size()];
        for (int i = 0; i < in.getComments().size(); i++) {
            apollo.datamodel.Comment c = (apollo.datamodel.Comment) in.getComments().elementAt(i);
            comments[i] = getCORBAComment(c);
        }
        out.comments = comments;
        return out;
    }

    public AnnotatedGene getCORBAGene(Gene in) {
        AnnotatedGene out = new AnnotatedGene();
        out.type = GeneType.PROTEIN_CODING_GENE;
        out.ident = getCORBAIdentifier(in.getIdentifier());
        org.apollo.datamodel.Transcript[] transcripts = new org.apollo.datamodel.Transcript[in.getTranscripts().size()];
        for (int i = 0; i < in.getTranscripts().size(); i++) {
            apollo.datamodel.Transcript t = (apollo.datamodel.Transcript) in.getTranscripts().elementAt(i);
            transcripts[i] = getCORBATranscript(t);
        }
        out.transcripts = transcripts;
        org.apollo.datamodel.Comment[] comments = new org.apollo.datamodel.Comment[in.getComments().size()];
        for (int i = 0; i < in.getComments().size(); i++) {
            apollo.datamodel.Comment c = (apollo.datamodel.Comment) in.getComments().elementAt(i);
            comments[i] = getCORBAComment(c);
        }
        out.comments = comments;
        return out;
    }

    /**
   * Returns a sequence for the accession number specified in the DbXref
   * for the GadFly database; the database name and type are ignored for
   * now; they will probably be used once more sequence request methods
   * are available from the CORBA server
   */
    public SequenceI getSequence(apollo.datamodel.DbXref dbxref) throws DataAdapterException {
        try {
            return getSequence(dbxref.getIdValue(), annSeq);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAdapterException("Error fetching sequence id: " + dbxref.getIdValue() + " of type " + e.toString(), e);
        }
    }

    /**
   * Returns a piece of a sequence for the accession numberspecified
   * in the DbXref for the GadFly database; the database name and type
   * are ignored for now; they will probably be used once more
   * sequence request methods are available from the CORBA server
   * 
   * note: the start and end parameters are converted to ints before the
   * request is made for compatibility with the idl
   *
   */
    public SequenceI getSequence(apollo.datamodel.DbXref dbxref, long start, long end) throws DataAdapterException {
        try {
            return getSequence(dbxref.getIdValue(), annSeq, start, end);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAdapterException("Error fetching sequence id: " + dbxref.getIdValue() + " of type " + e.toString(), e);
        }
    }

    public SequenceI getSequence(String id) throws DataAdapterException {
        try {
            return getSequence(id, annSeq);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAdapterException("Error fetching sequence id: " + id + " of type " + e.toString(), e);
        }
    }

    public SequenceI getSequence(String id, AnnotatedRegion annSeq, long start, long end) throws DataAdapterException {
        try {
            Seq corba_seq = annSeq.get_Seq(id);
            SequenceI seq = makeNewSequence(id, corba_seq, (int) start, (int) end);
            return seq;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAdapterException("Error fetching sequence id: " + id + " of type " + e.toString(), e);
        }
    }

    public SequenceI getSequence(String id, AnnotatedRegion annSeq) throws DataAdapterException {
        try {
            Seq corba_seq = annSeq.get_Seq(id);
            try {
                SequenceI seq = makeNewSequence(id, corba_seq);
                return seq;
            } catch (Exception e) {
                e.printStackTrace();
                throw new DataAdapterException("Error creating apollo sequence id: " + id + " length=" + corba_seq.residues.length() + " of type " + e.toString(), e);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAdapterException("Error fetching sequence id: " + id + " of type " + e.toString(), e);
        }
    }

    public SequenceI makeNewSequence(String id, org.apollo.datamodel.Seq corba_seq, int start, int end) {
        Sequence seq;
        if (corba_seq.residues.length() > end) seq = new Sequence(id, (corba_seq.residues).substring(start, end)); else if (start < corba_seq.residues.length()) seq = new Sequence(id, corba_seq.residues.substring(start)); else seq = new Sequence(id, corba_seq.residues);
        initNewSequence(corba_seq, seq);
        return seq;
    }

    public SequenceI makeNewSequence(String id, org.apollo.datamodel.Seq corba_seq) {
        Sequence seq = new Sequence(id, corba_seq.residues);
        initNewSequence(corba_seq, seq);
        return seq;
    }

    private void initNewSequence(org.apollo.datamodel.Seq corba_seq, Sequence seq) {
        seq.setAccessionNo(corba_seq.id);
        seq.setDescription(corba_seq.description);
        seq.setLength(corba_seq.seq_len);
        seq.setDatabase(corba_seq.dbname);
        curation_set.addSequence(seq);
    }

    public Vector getSequences(apollo.datamodel.DbXref[] dbxref) throws DataAdapterException {
        try {
            Vector out = new Vector();
            for (int i = 0; i < dbxref.length; i++) {
                SequenceI seq = getSequence(dbxref[i].getIdValue(), annSeq);
                out.addElement(seq);
            }
            return out;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAdapterException("Error fetching sequences: " + e.toString(), e);
        }
    }

    public Vector getSequences(apollo.datamodel.DbXref[] dbxref, long[] start, long[] end) throws DataAdapterException {
        try {
            Vector out = new Vector();
            for (int i = 0; i < dbxref.length; i++) {
                SequenceI seq = getSequence(dbxref[i].getIdValue(), annSeq, start[i], end[i]);
                out.addElement(seq);
            }
            return out;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAdapterException("Error fetching sequence" + " of type " + e.toString(), e);
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            String id = "AE003440";
            if (args.length == 1) {
                id = args[0];
            }
            CORBAAdapter databoy = null;
            System.out.println("Didn't get databoy");
            apollo.datamodel.Gene junk = new Gene();
            apollo.datamodel.Transcript junkt = new apollo.datamodel.Transcript();
            apollo.datamodel.Exon junke = new apollo.datamodel.Exon(5, 10, "fake", -1);
            junkt.addExon(junke);
            junk.addTranscript(junkt);
            CurationSet cs = new CurationSet();
            cs.setRegion(id);
            AnnotationChangeLog fcl = new AnnotationChangeLog();
            fcl.getAddedFeatures().addElement(junk);
            cs.setAnnotationChangeLog(fcl);
            databoy.commitChanges(cs);
            FeatureSetI genes = databoy.getAnnotatedRegion();
            System.err.println("After add");
            System.err.println("--------------");
            for (int i = 0; i < genes.size(); i++) {
                DisplayTool.showGene((Gene) genes.getFeatureAt(i));
            }
            junk.setStart(7);
            fcl = new AnnotationChangeLog();
            fcl.getChangedFeatures().addElement(junk);
            cs.setAnnotationChangeLog(fcl);
            databoy.commitChanges(cs);
            genes = databoy.getAnnotatedRegion();
            System.err.println("After change");
            System.err.println("--------------");
            for (int i = 0; i < genes.size(); i++) {
                DisplayTool.showGene((Gene) genes.getFeatureAt(i));
            }
        } catch (DataAdapterException e) {
            e.getParentException().printStackTrace();
        }
    }

    public void getHitSequences(FeatureSetI top, Hashtable seqHash, AnnotatedRegion annSeq, boolean outer) {
        double progressInc = progressSectionSize / top.size();
        for (int i = 0; i < top.size(); i++) {
            if (outer) {
                fireProgressEvent(new ProgressEvent(this, new Double(currentProgress), "Loading hit sequences..."));
                currentProgress += progressInc;
            }
            if (top.getFeatureAt(i) instanceof FeatureSetI) {
                getHitSequences((FeatureSetI) (top.getFeatureAt(i)), seqHash, annSeq, false);
            } else if (top.getFeatureAt(i) instanceof FeaturePair) {
                FeaturePair fp = (FeaturePair) top.getFeatureAt(i);
                SeqFeatureI hit = fp.getHitFeature();
                if (hit.getName() != null && !hit.getName().equals("")) {
                    fp.getRefFeature().setName(hit.getName());
                    if (hit.getRefSequence() == null) {
                        if (!seqHash.containsKey(hit.getName())) {
                            try {
                                seqHash.put(hit.getName(), getSequence(hit.getName(), annSeq));
                            } catch (Exception e) {
                                System.out.println("Can't fetch sequence " + e);
                            }
                        }
                        SequenceI seq = (SequenceI) seqHash.get(hit.getName());
                        hit.setRefSequence(seq);
                    }
                }
            } else {
                System.out.println("Non FP in getHitSequences");
            }
        }
    }

    public CurationSet getCurationSet() throws DataAdapterException {
        try {
            curation_set = new CurationSet();
            curation_set.setAnnotationChangeLog(new AnnotationChangeLog());
            try {
                genome_seq = getSequence(extId, annSeq);
                System.out.println("Annotated Sequence: " + genome_seq.getDisplayId() + " " + genome_seq.getSeqString().length() + " bases");
            } catch (Exception e) {
                throw new DataAdapterException("Load failed. Are you sure " + extId + "is a real sequence?", e);
            }
            System.out.println("GETTING PREFERENCES");
            currentProgress = 0.0;
            fireProgressEvent(new ProgressEvent(this, new Double(currentProgress), "Loading preferences..."));
            curation_set.setPropertyScheme(apollo.gui.Config.getPropertyScheme());
            curation_set.setRegion((Sequence) genome_seq);
            progressSectionSize = 1.0;
            currentProgress += progressSectionSize;
            progressSectionSize = 50.0;
            System.out.println("GETTING ANALYSIS RESULTS");
            fireProgressEvent(new ProgressEvent(this, new Double(currentProgress), "Loading analysis results..."));
            curation_set.setResults(getAnalysisRegion(annSeq));
            currentProgress = progressSectionSize;
            System.out.println("GETTING ANNOTATIONS");
            fireProgressEvent(new ProgressEvent(this, new Double(currentProgress), "Loading annotations..."));
            curation_set.setAnnots(getAnnotatedRegion(annSeq));
            currentProgress = 2 * progressSectionSize;
            fireProgressEvent(new ProgressEvent(this, new Double(currentProgress), "Done loading..."));
            return curation_set;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAdapterException("Error getting AnnotatedRegion: " + e.toString(), e);
        }
    }

    private StrandedFeatureSetI getAnalysisRegion() throws DataAdapterException {
        try {
            return getAnalysisRegion(annSeq);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAdapterException("Error getting FeatureSets: " + e.toString(), e);
        }
    }

    private StrandedFeatureSetI getAnalysisRegion(AnnotatedRegion annSeq) throws DataAdapterException {
        try {
            String[] analysis_ids = annSeq.get_analysis_id_list();
            StrandedFeatureSetI fs = new StrandedFeatureSet();
            fs.setHolder(true);
            fs.setRefSequence(genome_seq);
            double progressInc = progressSectionSize / analysis_ids.length;
            for (int i = 0; i < analysis_ids.length; i++) {
                fireProgressEvent(new ProgressEvent(this, new Double(currentProgress), "Loading analysis results..."));
                currentProgress += progressInc;
                FeatureSetI analysis_forward = new FeatureSet();
                FeatureSetI analysis_reverse = new FeatureSet();
                Analysis analysis = annSeq.get_analysis_by_id(analysis_ids[i]);
                String type = analysis.program + ":" + analysis.dbname;
                analysis_forward.setHolder(true);
                analysis_reverse.setHolder(true);
                analysis_forward.setId(analysis_ids[i]);
                analysis_reverse.setId(analysis_ids[i]);
                analysis_forward.setRefFeature(fs);
                analysis_reverse.setRefFeature(fs);
                analysis_forward.setProgramName(analysis.program);
                analysis_reverse.setProgramName(analysis.program);
                analysis_forward.setDatabase(analysis.dbname);
                analysis_reverse.setDatabase(analysis.dbname);
                analysis_forward.setType(type);
                analysis_reverse.setType(type);
                analysis_forward.setRefSequence(genome_seq);
                analysis_reverse.setRefSequence(genome_seq);
                ResultSet[] results = analysis.results;
                for (int j = 0; j < results.length; j++) {
                    ResultSet resultSet = results[j];
                    String setId = resultSet.result_id;
                    FeatureSet splitSpansSet = null;
                    FeatureSet resFeatureSet = new FeatureSet();
                    resFeatureSet.setId(setId);
                    resFeatureSet.setType(type);
                    resFeatureSet.setProgramName(analysis_forward.getProgramName());
                    resFeatureSet.setDatabase(analysis_forward.getDatabase());
                    resFeatureSet.setRefSequence(genome_seq);
                    int first_span_strand = 0;
                    for (int k = 0; k < resultSet.result_spans.length; k++) {
                        FeaturePair fp = readResultSpan(resultSet.result_spans[k], type);
                        if (k == 0) {
                            first_span_strand = fp.getStrand();
                        }
                        if (first_span_strand != fp.getStrand()) {
                            System.out.println("Inconsistant strands in " + setId);
                            if (splitSpansSet == null) {
                                splitSpansSet = new FeatureSet();
                                splitSpansSet.setType(type);
                                splitSpansSet.setId(setId + "split");
                                splitSpansSet.setProgramName(analysis_forward.getProgramName());
                                splitSpansSet.setDatabase(analysis_forward.getDatabase());
                                splitSpansSet.setRefSequence(genome_seq);
                            }
                            splitSpansSet.addFeature(fp);
                            fp.setRefFeature(splitSpansSet);
                            fp.setRefId(splitSpansSet.getId());
                        } else {
                            resFeatureSet.addFeature(fp);
                            fp.setRefFeature(resFeatureSet);
                            fp.setRefId(resFeatureSet.getId());
                        }
                    }
                    if (resFeatureSet.getStrand() == 1) {
                        analysis_forward.addFeature(resFeatureSet);
                        resFeatureSet.setRefFeature(analysis_forward);
                    } else {
                        analysis_reverse.addFeature(resFeatureSet);
                        resFeatureSet.setRefFeature(analysis_reverse);
                    }
                    if (splitSpansSet != null) {
                        if (splitSpansSet.getStrand() == 1) {
                            analysis_forward.addFeature(splitSpansSet);
                            splitSpansSet.setRefFeature(analysis_forward);
                        } else {
                            analysis_reverse.addFeature(splitSpansSet);
                            splitSpansSet.setRefFeature(analysis_reverse);
                        }
                    }
                }
                if (analysis_forward.size() > 0) {
                    fs.addFeature(analysis_forward);
                }
                if (analysis_reverse.size() > 0) {
                    fs.addFeature(analysis_reverse);
                }
            }
            return fs;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAdapterException("Error getting FeatureSets: " + e.toString(), e);
        }
    }

    private StrandedFeatureSetI getAnnotatedRegion() throws DataAdapterException {
        return getAnnotatedRegion(annSeq);
    }

    private StrandedFeatureSetI getAnnotatedRegion(AnnotatedRegion annSeq) throws DataAdapterException {
        try {
            AnnotatedGene[] genes;
            Analysis[] analyses;
            org.apollo.datamodel.GenericAnnotation[] genannot;
            genes = annSeq.get_gene_list();
            StrandedFeatureSetI output = new StrandedFeatureSet();
            output.setHolder(true);
            output.setRefSequence(genome_seq);
            double progressInc = progressSectionSize / genes.length;
            for (int i = 0; i < genes.length; i++) {
                fireProgressEvent(new ProgressEvent(this, new Double(currentProgress), "Loading annotations..."));
                currentProgress += progressInc;
                Gene gene = new Gene();
                gene.setHolder(true);
                gene.setRefFeature(output);
                gene.setRefSequence(genome_seq);
                apollo.datamodel.Identifier geneid = readIdentifier(genes[i].ident);
                apollo.datamodel.Comment[] genecomms = readComments(genes[i].comments);
                gene.setIdentifier(geneid);
                gene.setId(geneid.getName());
                gene.setName(geneid.getName());
                for (int c = 0; c < genecomms.length; c++) {
                    gene.addComment(genecomms[c]);
                }
                for (int j = 0; j < genes[i].transcripts.length; j++) {
                    apollo.datamodel.Transcript trans = new apollo.datamodel.Transcript();
                    apollo.datamodel.Identifier tranid = readIdentifier(genes[i].transcripts[j].ident);
                    apollo.datamodel.Comment[] trancomms = readComments(genes[i].transcripts[j].comments);
                    trans.setIdentifier(tranid);
                    trans.setId(tranid.getName());
                    trans.setName(tranid.getName());
                    for (int c = 0; c < trancomms.length; c++) {
                        trans.addComment(trancomms[c]);
                    }
                    trans.setRefSequence(genome_seq);
                    for (int k = 0; k < genes[i].transcripts[j].exons.length; k++) {
                        apollo.datamodel.Identifier exonid = readIdentifier(genes[i].transcripts[j].exons[k].ident);
                        apollo.datamodel.Exon exon;
                        int strand = 0;
                        if (genes[i].transcripts[j].exons[k].range.strand.value() == StrandType.plus.value()) {
                            strand = 1;
                        } else if (genes[i].transcripts[j].exons[k].range.strand.value() == StrandType.minus.value()) {
                            strand = -1;
                        }
                        exon = new apollo.datamodel.Exon(genes[i].transcripts[j].exons[k].range.range_min, genes[i].transcripts[j].exons[k].range.range_max, genes[i].transcripts[j].exons[k].type, strand);
                        exon.setName(genes[i].transcripts[j].exons[k].ident.name);
                        exon.setRefId(trans.getId());
                        exon.setRefSequence(genome_seq);
                        exon.setIdentifier(exonid);
                        exon.setId(exonid.getName());
                        for (int n = 0; n < genes[i].transcripts[j].exons[k].evidence_list.length; n++) {
                            exon.addEvidence(genes[i].transcripts[j].exons[k].evidence_list[n].result_span_id);
                        }
                        trans.addExon(exon);
                    }
                    trans.setTranslationStartAtFirstCodon();
                    gene.addTranscript(trans);
                }
                output.addFeature(gene);
            }
            return output;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAdapterException("Error fetching genes: " + e.toString(), e);
        }
    }

    public Session getSession() throws DataAdapterException {
        try {
            System.out.println("path = " + path);
            URL url = new URL(path);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String refIOR = in.readLine();
            System.out.println("Got IOR = " + refIOR);
            org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init(new String[0], new java.util.Properties());
            System.out.println("Got orb = " + orb);
            org.omg.CORBA.Object manObj = orb.string_to_object(refIOR);
            System.out.println("Got manObj = " + manObj);
            SessionManager mySessMan = SessionManagerHelper.narrow(manObj);
            System.out.println("Got sessman = " + mySessMan);
            Param[] nullParams = new Param[0];
            mySess = mySessMan.initiate_Session(nullParams);
            System.out.println("Got sess = " + mySess);
            mySess.connect(sessParams);
            System.out.println("connected = " + mySess);
            return mySess;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAdapterException("Error creating CORBA session: " + e.toString(), e);
        }
    }

    public FeaturePair readResultSpan(ResultSpan span, String type) {
        SeqFeatureI sf1 = new SeqFeature(span.range1.range_min, span.range1.range_max, type);
        SeqFeatureI sf2 = new SeqFeature(span.range2.range_min, span.range2.range_max, type);
        sf1.setId(span.result_id);
        sf2.setId(span.result_id);
        sf1.setName(span.range1.sequence_id);
        sf2.setName(span.range2.sequence_id);
        sf1.setRefSequence(genome_seq);
        if (sf2.getName() != null && !sf2.getName().equals("")) {
            SequenceI hit_seq = curation_set.getSequence(sf2.getName());
            if (hit_seq == null) {
                try {
                    hit_seq = getSequence(sf2.getName(), annSeq);
                } catch (DataAdapterException e) {
                    System.err.println("Unable to create reference sequence " + sf2.getName());
                }
            }
            sf2.setRefSequence(hit_seq);
        }
        Alignment alignment = null;
        if (span.scores != null && span.scores.length > 0) {
            boolean scoreSet = false;
            String query_seq = null;
            String subject_seq = null;
            for (int i = 0; i < span.scores.length; i++) {
                String name = span.scores[i].type;
                String value = span.scores[i].value;
                try {
                    double score = Double.valueOf(value).doubleValue();
                    sf1.addScore(name, score);
                    sf2.addScore(name, score);
                } catch (Exception e) {
                    sf1.addProperty(name, value);
                    sf2.addProperty(name, value);
                    if (name.equals("query_alignment")) {
                        query_seq = value;
                    } else if (name.equals("subject_alignment")) {
                        subject_seq = value;
                    }
                }
                scoreSet = true;
            }
            if (!scoreSet) {
                sf1.setScore(100.0);
                sf1.setScore(100.0);
            }
            if (query_seq != null && subject_seq != null) {
                alignment = new Alignment(new Sequence("query_align", query_seq), new Sequence("subj_align", subject_seq));
            }
        } else {
            sf1.setScore(100.0);
            sf2.setScore(100.0);
        }
        if (span.range1.strand.value() == StrandType.plus.value()) {
            sf1.setStrand(1);
        } else if (span.range1.strand.value() == StrandType.minus.value()) {
            sf1.setStrand(-1);
        } else {
            sf1.setStrand(0);
        }
        if (span.range2.strand.value() == StrandType.plus.value()) {
            sf2.setStrand(1);
        } else if (span.range2.strand.value() == org.apollo.datamodel.StrandType.minus.value()) {
            sf2.setStrand(-1);
        } else {
            sf2.setStrand(0);
        }
        FeaturePair fp = new FeaturePair(sf1, sf2);
        fp.setAlignment(alignment);
        return fp;
    }

    public apollo.datamodel.Comment[] readComments(org.apollo.datamodel.Comment[] commset) {
        apollo.datamodel.Comment[] comments = new apollo.datamodel.Comment[commset.length];
        for (int i = 0; i < commset.length; i++) {
            comments[i] = readComment(commset[i]);
        }
        return comments;
    }

    public apollo.datamodel.Comment readComment(org.apollo.datamodel.Comment comm) {
        String id = comm.comment_id;
        String text = comm.text;
        boolean isInternal = false;
        if (text.startsWith("INTERNAL\n")) {
            isInternal = true;
            text = text.substring(9, text.length());
        } else if (text.startsWith("EXTERNAL\n")) {
            isInternal = false;
            text = text.substring(9, text.length());
        }
        String person = comm.person.readable_name;
        String person_id = comm.person.person_id;
        long timestamp = comm.time;
        apollo.datamodel.Comment comment = new apollo.datamodel.Comment(id, text, person, person_id, timestamp);
        comment.setIsInternal(isInternal);
        return comment;
    }

    public apollo.datamodel.Identifier readIdentifier(org.apollo.datamodel.Identifier id) {
        String name = id.name;
        String desc = id.description;
        String[] syn = id.synonyms;
        org.apollo.datamodel.DbXref[] dbx = id.dbxrefs;
        apollo.datamodel.Identifier ident = new apollo.datamodel.Identifier(name, desc);
        for (int i = 0; i < syn.length; i++) {
            ident.addSynonym(syn[i]);
        }
        for (int i = 0; i < dbx.length; i++) {
            apollo.datamodel.DbXref dbxref = new apollo.datamodel.DbXref(dbx[i].idtype, dbx[i].idvalue, dbx[i].dbname);
            ident.addDbXref(dbxref);
        }
        return ident;
    }

    public String getRawAnalysisResults(String id) throws DataAdapterException {
        try {
            System.err.println("Trying to get raw results for " + id);
            String[] raws = annSeq.get_raw_results_segment(id);
            if (raws == null || raws.length < 1) throw new DataAdapterException("Raw results not " + "available for " + id);
            return raws[0];
        } catch (Exception e) {
            throw new DataAdapterException(e);
        }
    }
}
