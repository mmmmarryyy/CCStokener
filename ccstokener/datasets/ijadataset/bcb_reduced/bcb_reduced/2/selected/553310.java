package apollo.dataadapter.gff3;

import apollo.datamodel.*;
import apollo.dataadapter.*;
import apollo.gui.synteny.CurationManager;
import apollo.seq.io.FastaFile;
import apollo.util.OBOParser;
import apollo.util.IOUtil;
import apollo.util.Pair;
import apollo.config.Style;
import apollo.config.Config;
import org.bdgp.io.IOOperation;
import org.bdgp.io.DataAdapterUI;
import org.obo.dataadapter.OBOParseException;
import org.obo.datamodel.IdentifiedObject;
import org.apache.log4j.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

public class GFF3Adapter extends AbstractApolloAdapter {

    protected static final Logger logger = LogManager.getLogger(GFF3Adapter.class);

    private String gffFilename;

    private String seqFilename;

    private CurationSet curation;

    private boolean fastaFromGff;

    private Map<String, SequenceI> seqs;

    private OBOParser oboParser;

    /** Constructor
   */
    public GFF3Adapter() {
        setName("GFF3 format");
        seqs = new TreeMap<String, SequenceI>();
        fastaFromGff = false;
    }

    /** Sets whether to parse FASTA from GFF or external FASTA file
   */
    public void setFastaFromGff(boolean fastaFromGff) {
        this.fastaFromGff = fastaFromGff;
    }

    /** Checks whether to parse FASTA from GFF or external FASTA file
   */
    public boolean isFastaFromGff() {
        return fastaFromGff;
    }

    /** Get supported operations (read / write)
   */
    public IOOperation[] getSupportedOperations() {
        return new IOOperation[] { ApolloDataAdapterI.OP_READ_DATA, ApolloDataAdapterI.OP_WRITE_DATA, ApolloDataAdapterI.OP_APPEND_DATA };
    }

    /** Get the UI for the data adapter
   */
    public DataAdapterUI getUI(IOOperation op) {
        return new GFF3AdapterGUI(op);
    }

    /** Initialize the adapter
   */
    public void init() {
    }

    /** Get the GFF filename
   */
    public String getGffFilename() {
        return gffFilename;
    }

    /** Set the GFF filename
   */
    public void setGffFilename(String gffFilename) {
        this.gffFilename = gffFilename;
    }

    /** Get the sequence filename
   */
    public String getSeqFilename() {
        return seqFilename;
    }

    /** Set the sequence filename
   */
    public void setSeqFilename(String seqFilename) {
        this.seqFilename = seqFilename;
    }

    /** Sets the input for this adapter
   */
    public void setDataInput(apollo.dataadapter.DataInput di) {
        if (di.isFile()) {
            setGffFilename(di.getFilename());
        } else if (di.isUrl()) {
            setGffFilename(di.getUrl());
        }
        if (di.hasSequenceFilename()) {
            setSeqFilename(di.getSequenceFilename());
        }
    }

    /** Parse and return the CurationSet
   */
    public CurationSet getCurationSet() throws ApolloAdapterException {
        clearOldData();
        curation = new CurationSet();
        if (!isFastaFromGff() && getSeqFilename() != null) {
            parseSeq(getSeqFilename());
        }
        parseGff(curation, true);
        curation.setName(curation.getRefSequence().getName());
        if (!curation.getName().matches(".*:\\d+-\\d+$")) {
            curation.setName(curation.getName() + ":" + curation.getLow() + "-" + curation.getHigh());
        }
        return curation;
    }

    /** Add GFF3 data to currently existing CurationSet
   * 
   * @param values - ApolloDataI object that contains CurationSet
   * @throws ApolloAdapterException if there is an error in parsing the data
   */
    public Boolean addToCurationSet() throws ApolloAdapterException {
        if (curation == null) {
            curation = CurationManager.getActiveCurationState().getCurationSet();
        }
        parseGff(curation, false);
        return true;
    }

    /** Commit data
   */
    public void commitChanges(CurationSet curation) throws ApolloAdapterException {
        write(curation);
    }

    /** Write data
   */
    public void write(Object values) throws ApolloAdapterException {
        CurationSet curation = getCurationSetFromApolloData(values);
        PrintWriter out;
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(getGffFilename())));
        } catch (IOException e) {
            throw new ApolloAdapterException("Error writing to " + getGffFilename() + ": " + e.getMessage());
        }
        out.println("##gff-version 3");
        out.printf("##sequence-region %s %d %d\n", curation.getRefSequence().getName(), curation.getLow(), curation.getHigh());
        StrandedFeatureSetI annots = curation.getAnnots();
        StrandedFeatureSetI results = curation.getResults();
        Vector feats = annots.getFeatures();
        Collections.sort(feats, new PositionComparator());
        Map<String, Integer> writtenIds = new TreeMap<String, Integer>();
        for (Object o : feats) {
            SeqFeatureI feat = (SeqFeatureI) o;
            writeAnnot(out, feat, null, writtenIds);
        }
        feats = results.getFeatures();
        Collections.sort(feats, new PositionComparator());
        Map<String, SequenceI> seqs = new HashMap<String, SequenceI>();
        Map<String, Integer> seqIds = new HashMap<String, Integer>();
        for (Object o : feats) {
            SeqFeatureI feat = (SeqFeatureI) o;
            writeResult(out, feat, null, seqs, seqIds);
        }
        out.println("###");
        List<SequenceI> seqsToWrite = new LinkedList<SequenceI>(seqs.values());
        seqsToWrite.add(0, curation.getRefSequence());
        if (isFastaFromGff() || getSeqFilename() == null) {
            writeSeqs(out, seqsToWrite);
        } else {
            try {
                PrintWriter fastaOut = new PrintWriter(new BufferedWriter(new FileWriter(getSeqFilename())));
                writeSeqs(fastaOut, seqsToWrite);
                fastaOut.close();
            } catch (IOException e) {
                throw new ApolloAdapterException("Error writing to " + getSeqFilename() + ": " + e.getMessage());
            }
        }
        out.close();
    }

    private CurationSet getCurationSetFromApolloData(Object values) {
        ApolloDataI apolloData = (ApolloDataI) values;
        return apolloData.isCurationSet() ? apolloData.getCurationSet() : apolloData.getCompositeDataHolder().getCurationSet(0);
    }

    /** Write seqs as FASTA format
   *
   */
    private void writeSeqs(PrintWriter out, List<SequenceI> seqs) {
        out.println("##FASTA");
        for (SequenceI seq : seqs) {
            if (seq.getLength() > 0) {
                out.print(FastaFile.print(seq));
            }
        }
    }

    /** Write annotation SeqFeatureI object into GFF3 tab delimited format
   *
   *   @param feat - SeqFeatureI object containing data to be written
   */
    private void writeAnnot(PrintWriter out, SeqFeatureI feat, SeqFeatureI parent, Map<String, Integer> writtenIds) {
        GFF3Entry entry = new GFF3Entry(feat);
        Integer writtenCount = writtenIds.get(entry.getId());
        if (writtenCount != null) {
            int c = writtenCount.intValue();
            writtenIds.put(entry.getId(), ++c);
            entry.setId(entry.getId() + "-" + c);
        } else {
            writtenIds.put(entry.getId(), 1);
        }
        if (entry.isOneLevelAnnotation()) {
            out.println(entry);
            return;
        }
        if (parent != null) {
            entry.addAttributeValue("Parent", parent.getId());
        }
        Vector children = feat.getFeatures();
        for (Object o : children) {
            SeqFeatureI child = (SeqFeatureI) o;
            if (child instanceof Transcript) {
                if (!child.isProteinCodingGene()) {
                    entry.setType("gene");
                    break;
                }
            }
        }
        if (feat instanceof Transcript) {
            if (feat.isProteinCodingGene()) {
                children = new Vector(children);
                Transcript trans = (Transcript) feat;
                entry.setType("mRNA");
                int tStart = trans.getTranslationStart();
                int tEnd = trans.getLastBaseOfStopCodon();
                if (trans.getStrand() == -1) {
                    int tmp = tStart;
                    tStart = tEnd;
                    tEnd = tmp;
                }
                int currentLength = 0;
                for (Object o : feat.getFeatures()) {
                    SeqFeatureI child = (SeqFeatureI) o;
                    SeqFeatureI cds = null;
                    if (child.getLow() < tStart && child.getHigh() > tStart) {
                        cds = child.cloneFeature();
                        cds.setLow(tStart);
                    }
                    if (child.getHigh() > tEnd && child.getLow() < tEnd) {
                        if (cds == null) {
                            cds = child.cloneFeature();
                        }
                        cds.setHigh(tEnd);
                    }
                    if (child.getLow() >= tStart && child.getHigh() <= tEnd) {
                        cds = child.cloneFeature();
                    }
                    if (cds != null) {
                        cds.setFeatureType("CDS");
                        cds.setId(cds.getId() + "-cds");
                        cds.setName(cds.getName() + "-cds");
                        cds.setPhase(currentLength % 3);
                        currentLength += cds.length();
                        children.add(cds);
                    }
                }
            } else if (parent != null) {
                entry.setType(parent.getFeatureType());
            }
        }
        out.println(entry);
        for (Object o : children) {
            writeAnnot(out, (SeqFeatureI) o, feat, writtenIds);
        }
    }

    /** Write result SeqFeatureI object into GFF3 tab delimited format
   *
   *   @param feat - SeqFeatureI object containing data to be written
   */
    private void writeResult(PrintWriter out, SeqFeatureI feat, String parent, Map<String, SequenceI> seqs, Map<String, Integer> seqIds) {
        GFF3Entry entry = new GFF3Entry(feat);
        entry.setSource(feat.getFeatureType());
        if (parent != null) {
            entry.addAttributeValue("Parent", parent);
            entry.setType("match_part");
        } else {
            entry.setType("match");
        }
        if (feat instanceof FeaturePairI) {
            FeaturePairI fp = (FeaturePairI) feat;
            SequenceI hitSeq = fp.getHitSequence();
            if (hitSeq != null) {
                if (seqs.containsKey(hitSeq.getName()) && hitSeq.getResidues() != null && !seqs.get(hitSeq.getName()).getResidues().equals(hitSeq.getResidues())) {
                    String oldHitName = hitSeq.getName();
                    seqIds.put(hitSeq.getName(), seqIds.get(hitSeq.getName()) + 1);
                    hitSeq.setName(hitSeq.getName() + "-" + seqIds.get(hitSeq.getName()));
                    seqs.put(hitSeq.getName(), hitSeq);
                    List<String> targetAttrs = entry.getAttributeValues("Target");
                    targetAttrs.set(0, targetAttrs.get(0).replace(oldHitName, hitSeq.getName()));
                } else {
                    seqs.put(hitSeq.getName(), hitSeq);
                    seqIds.put(hitSeq.getName(), 1);
                }
            }
        }
        boolean print = isPrintableResult(feat);
        Vector children = feat.getFeatures();
        for (Object o : children) {
            if (isPrintableResult((SeqFeatureI) o)) {
                print = true;
                break;
            }
        }
        String pid = null;
        if (print) {
            pid = entry.getId();
            out.println(entry);
        }
        for (Object o : children) {
            writeResult(out, (SeqFeatureI) o, pid, seqs, seqIds);
        }
    }

    /** Checks whether or not this result should be printed
   */
    private boolean isPrintableResult(SeqFeatureI feat) {
        return feat.getClass().getName().matches(".+FeaturePair$") || feat.getClass().getName().matches(".+SeqFeature$");
    }

    /** Parse FASTA input from Reader
   */
    private void parseSeq(Reader reader) throws ApolloAdapterException {
        FastaFile fasta = new FastaFile(reader, false);
        seqs.clear();
        for (Object o : fasta.getSeqs()) {
            SequenceI seq = (SequenceI) o;
            seqs.put(seq.getName(), seq);
        }
    }

    /** Parse FASTA input from filename
   */
    private void parseSeq(String fname) throws ApolloAdapterException {
        try {
            parseSeq(getReader(fname));
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new ApolloAdapterException("Error parsing FASTA", e);
        }
    }

    /** Parse the GFF data
   */
    private void parseGff(CurationSet curation, boolean setRefSeq) throws ApolloAdapterException {
        if (this.oboParser == null) {
            try {
                Collection<String> fnames = getOBOFilenames();
                logger.info("Loading OBO files: " + fnames);
                oboParser = new OBOParser(fnames, true);
            } catch (IOException e) {
                throw new ApolloAdapterException("Error reading OBO file: " + e.getMessage());
            } catch (OBOParseException e) {
                throw new ApolloAdapterException("Error parsing OBO file: " + e.getMessage());
            }
        }
        try {
            Set<String> invalidSOTerms = new HashSet<String>();
            Set<String> invalidSeqIds = new HashSet<String>();
            String refId = null;
            BufferedReader br = new BufferedReader(getReader(getGffFilename()));
            String line;
            StrandedFeatureSetI annots = curation.getAnnots() == null ? new StrandedFeatureSet(new AnnotatedFeature(), new AnnotatedFeature()) : curation.getAnnots();
            StrandedFeatureSetI results = curation.getResults() == null ? new StrandedFeatureSet(new FeatureSet(), new FeatureSet()) : curation.getResults();
            br.mark(1);
            int c;
            Map<String, GFF3Data> gffMap = new TreeMap<String, GFF3Data>();
            Map<String, int[]> cdsCoords = new TreeMap<String, int[]>();
            while ((c = br.read()) != -1) {
                if (c == '>') {
                    br.reset();
                    if (isFastaFromGff() || seqs.isEmpty()) {
                        parseSeq(br);
                    }
                    break;
                } else {
                    line = (char) c + br.readLine();
                    if (line.charAt(0) == '#') {
                        if (line.charAt(1) == '#') {
                            String[] tokens = line.split("\\s+");
                            if (tokens[0].equals("##sequence-region")) {
                                refId = tokens[1];
                                if (!setRefSeq && !refId.equals(curation.getRefSequence().getName())) {
                                    throw new ApolloAdapterException("Trying to append data for sequence " + refId + " when current data loaded is for " + curation.getRefSequence().getName());
                                }
                                curation.setStrand(1);
                                curation.setLow(Integer.parseInt(tokens[2]));
                                curation.setHigh(Integer.parseInt(tokens[3]));
                            }
                        }
                    } else {
                        if (line.length() == 0) {
                            continue;
                        }
                        GFF3Entry entry = new GFF3Entry(line);
                        if (!setRefSeq && !entry.getReferenceId().matches(curation.getRefSequence().getName())) {
                            if (!invalidSeqIds.contains(entry.getReferenceId())) {
                                apollo.util.IOUtil.errorDialog("Skipping entry for sequence " + entry.getReferenceId() + " when current data loaded is for " + curation.getRefSequence().getName());
                                invalidSeqIds.add(entry.getReferenceId());
                            }
                            continue;
                        }
                        if (refId == null) {
                            refId = entry.getReferenceId();
                        }
                        Pair<IdentifiedObject, String> term = oboParser.getTermByName(entry.getType());
                        if (term == null) {
                            if (!invalidSOTerms.contains(entry.getType())) {
                                apollo.util.IOUtil.errorDialog("Invalid GFF3 file: " + entry.getType() + " is not a valid SO term");
                                invalidSOTerms.add(entry.getType());
                            }
                            continue;
                        }
                        if (!entry.getType().equals(term.getSecond())) {
                            logger.info("GFF3 type " + entry.getType() + " does not match case for SO term " + term.getSecond() + "(" + term.getFirst().getID() + ")");
                        }
                        if (entry.getType().equals("CDS")) {
                            processCds(entry, cdsCoords);
                        } else if (entry.getType().equals("five_prime_UTR") || entry.getType().equals("three_prime_UTR")) {
                        } else if (entry.getType().equals("protein")) {
                        } else {
                            if (!gffMap.containsKey(entry.getId())) {
                                try {
                                    gffMap.put(entry.getId(), new GFF3Data(entry, entry.createFeature(oboParser)));
                                } catch (ClassNotFoundException e) {
                                    throw new ApolloAdapterException("Cannot locate Apollo datamodel class: " + e.getMessage());
                                } catch (IllegalAccessException e) {
                                    throw new ApolloAdapterException(e);
                                } catch (InstantiationException e) {
                                    throw new ApolloAdapterException("Cannot instantiate class: " + e.getMessage());
                                }
                            } else {
                                logger.warn("Multiple features with id " + entry.getId() + ".  Only processing the first one.");
                            }
                        }
                    }
                }
                br.mark(1);
            }
            if (setRefSeq) {
                SequenceI refSeq = refId != null ? seqs.get(refId) : null;
                if (refSeq == null) {
                    if (seqs.size() == 1) {
                        Map.Entry<String, SequenceI> entry = seqs.entrySet().iterator().next();
                        refSeq = entry.getValue();
                        logger.warn("No sequence data found for " + refId + ".  Using sequence " + entry.getKey() + " instead.");
                    } else {
                        throw new ApolloAdapterException("No sequence data found for " + refId);
                    }
                }
                curation.setRefSequence(refSeq);
                if (curation.getLow() == -1) {
                    curation.setLow(1);
                }
                if (curation.getHigh() == -1) {
                    curation.setHigh(refSeq.getLength());
                }
            }
            processFeatures(annots, results, gffMap, cdsCoords, curation);
            curation.setAnnots(annots);
            curation.setResults(results);
        } catch (IOException e) {
            throw new ApolloAdapterException("Error parsing GFF", e);
        }
    }

    private void processFeatures(StrandedFeatureSetI annots, StrandedFeatureSetI results, Map<String, GFF3Data> gffMap, Map<String, int[]> cdsCoords, CurationSet curation) throws ApolloAdapterException {
        Map<String, FeatureSetI[]> resultSets = new TreeMap<String, FeatureSetI[]>();
        Set<String> errors = new HashSet<String>();
        Set<GFF3Data> matches = new HashSet<GFF3Data>();
        for (Map.Entry<String, GFF3Data> mapEntry : gffMap.entrySet()) {
            GFF3Data data = mapEntry.getValue();
            GFF3Entry gff = data.getEntry();
            SeqFeatureI feat = data.getFeat();
            feat.setRefSequence(curation.getRefSequence());
            if (!gff.isAnnotation()) {
                processEvidence(feat);
            }
            if (gff.getType().equals("match")) {
                matches.add(data);
            }
            boolean added = false;
            List<String> pids = gff.getAttributeValues("Parent");
            if (pids != null) {
                int pnum = 0;
                for (String pid : pids) {
                    GFF3Data pdata = gffMap.get(pid);
                    if (gff.isAnnotation()) {
                        SeqFeatureI parent = pdata.getFeat();
                        if (!feat.isTranscript() && !feat.isExon()) {
                            if (!errors.contains(feat.getName())) {
                                IOUtil.errorDialog("Only transcripts and exons can be children of annotations: " + feat.getName() + "[" + feat.getFeatureType() + "]");
                                errors.add(feat.getName());
                            }
                            added = true;
                            continue;
                        }
                        if (feat.isExon() && !parent.isTranscript()) {
                            if (!errors.contains(feat.getName())) {
                                IOUtil.errorDialog("Exons can only be children of transcripts: " + feat.getName() + " -> " + parent.getName() + "[" + parent.getFeatureType() + "]");
                                errors.add(feat.getName());
                            }
                            added = true;
                            continue;
                        }
                    }
                    ++pnum;
                    if (pdata == null) {
                        logger.warn("Missing parent " + pid + " for " + gff.getName());
                        continue;
                    }
                    if (pnum > 1) {
                        if (pnum == 2) {
                            feat.setId(feat.getId() + "-1");
                        }
                        feat = feat.cloneFeature();
                        feat.setId(feat.getId().replaceFirst("-\\d+$", "-" + pnum));
                    }
                    if (gff.isAnnotation()) {
                        processAnnotation(data, pdata.getFeat(), cdsCoords);
                    }
                    try {
                        pdata.getFeat().addFeature(feat);
                        added = true;
                    } catch (ClassCastException e) {
                        logger.warn("Cannot add " + feat + "(" + data.getEntry().getType() + ")" + " to " + pdata.getFeat() + "(" + pdata.getEntry().getType() + ")");
                    }
                }
            } else {
                if (!feat.isAnnotTop()) {
                    if (gff.isAnnotation()) {
                        logger.warn("No parent feature found for " + feat + "(" + gff.getType() + ").  Generating dummy parent feature.");
                        generateParent(feat, annots);
                        added = true;
                    }
                }
            }
            if (!added) {
                if (gff.isAnnotation()) {
                    if (feat.getStrand() == 0) {
                        annots.getForwardSet().addFeature(feat);
                    } else {
                        annots.addFeature(feat);
                    }
                } else {
                    processEvidence(feat);
                    FeatureSetI[] fsArray = resultSets.get(gff.getSource());
                    if (fsArray == null) {
                        fsArray = new FeatureSet[2];
                        resultSets.put(gff.getSource(), fsArray);
                    }
                    int arrayIdx = gff.getStrand() == 1 ? 1 : 0;
                    FeatureSetI fs = fsArray[arrayIdx];
                    if (fs == null) {
                        fs = new FeatureSet(gff.getSource(), gff.getStrand());
                        fsArray[arrayIdx] = fs;
                    }
                    fs.addFeature(feat);
                }
            }
        }
        for (GFF3Data data : matches) {
            SeqFeatureI match = data.getFeat();
            if (match.getFeatures().size() == 0) {
                GFF3Entry matchPart = new GFF3Entry(data.getEntry().toString());
                matchPart.setType("match_part");
                try {
                    SeqFeatureI matchPartFeat = matchPart.createFeature(oboParser);
                    processEvidence(matchPartFeat);
                    match.addFeature(matchPartFeat);
                } catch (ClassNotFoundException e) {
                    throw new ApolloAdapterException("Cannot locate Apollo datamodel class: " + e.getMessage());
                } catch (IllegalAccessException e) {
                    throw new ApolloAdapterException(e);
                } catch (InstantiationException e) {
                    throw new ApolloAdapterException("Cannot instantiate class: " + e.getMessage());
                }
            }
        }
        for (Map.Entry<String, FeatureSetI[]> e : resultSets.entrySet()) {
            if (e.getValue()[0] != null) {
                results.addFeature(e.getValue()[0]);
            }
            if (e.getValue()[1] != null) {
                results.addFeature(e.getValue()[1]);
            }
        }
        setTranslationEnds(gffMap, cdsCoords);
    }

    private void generateParent(SeqFeatureI feat, StrandedFeatureSetI annots) {
        AnnotatedFeatureI parent = null;
        if (feat instanceof Transcript) {
            parent = new AnnotatedFeature();
        } else if (feat instanceof Exon) {
            parent = new Transcript();
        }
        parent.setId(feat.getId() + "-parent");
        parent.setLow(feat.getLow());
        parent.setHigh(feat.getHigh());
        parent.setStrand(feat.getStrand());
        parent.addFeature(feat);
        if (parent.isAnnotTop()) {
            annots.addFeature(parent);
        } else {
            generateParent(parent, annots);
        }
    }

    private void setTranslationEnds(Map<String, GFF3Data> gffMap, Map<String, int[]> cdsCoords) {
        for (GFF3Data data : gffMap.values()) {
            GFF3Entry gff = data.getEntry();
            SeqFeatureI feat = data.getFeat();
            if (gff.getType().equals("mRNA")) {
                int[] coords = cdsCoords.get(gff.getId());
                if (coords != null) {
                    Transcript trans = (Transcript) feat;
                    int stran = trans.getStart();
                    int scds;
                    int ecds;
                    int fix;
                    if (trans.getStrand() == -1) {
                        scds = coords[1];
                        ecds = coords[0];
                        fix = 2;
                    } else {
                        scds = coords[0];
                        ecds = coords[1];
                        fix = -2;
                    }
                    if (stran == scds) {
                        String cod = trans.na2aa(trans.getRefSequence().getResidues(stran, stran + (fix * -1)));
                        if (cod.equals("M")) {
                            trans.setTranslationStart(scds);
                        }
                    } else {
                        trans.setTranslationStart(scds);
                    }
                    trans.setTranslationEndFromStart();
                    int last = trans.getLastBaseOfStopCodon();
                    if (last != ecds) {
                        trans.setTranslationEnd(ecds + fix);
                    }
                }
            }
        }
    }

    private void processAnnotation(GFF3Data data, SeqFeatureI parent, Map<String, int[]> cdsCoords) {
        GFF3Entry gff = data.getEntry();
        SeqFeatureI feat = data.getFeat();
        if (feat instanceof Transcript) {
            if (!gff.getType().equals("mRNA")) {
                parent.setFeatureType(gff.getType());
            }
        }
    }

    private void processEvidence(SeqFeatureI feat) {
        if (feat instanceof FeaturePair) {
            FeaturePair fp = (FeaturePair) feat;
            fp.getQueryFeature().setRefSequence(curation.getRefSequence());
            String hitId = fp.getHitFeature().getProperty("refId");
            SequenceI hitSeq = seqs.get(hitId);
            if (hitSeq == null) {
                for (String id : hitId.split("\\|")) {
                    hitSeq = seqs.get(id);
                    if (hitSeq != null) {
                        hitId = id;
                        break;
                    }
                }
            }
            if (hitSeq != null) {
                fp.getHitFeature().setRefSequence(hitSeq);
            } else {
                logger.warn("FASTA missing hit sequence " + fp.getHitFeature().getProperty("refId") + " for " + feat.getName());
            }
        }
    }

    private void processCds(GFF3Entry entry, Map<String, int[]> cdsCoords) {
        List<String> parents = entry.getAttributeValues("Parent");
        if (parents == null) {
            logger.warn("No Parent attribute for CDS " + entry.getId());
            return;
        }
        for (String pid : parents) {
            int[] coords = cdsCoords.get(pid);
            if (coords == null) {
                coords = new int[] { entry.getStart(), entry.getEnd() };
                cdsCoords.put(pid, coords);
            } else {
                if (coords[0] > entry.getStart()) {
                    coords[0] = entry.getStart();
                }
                if (coords[1] < entry.getEnd()) {
                    coords[1] = entry.getEnd();
                }
            }
        }
    }

    private Collection<String> getOBOFilenames() throws ApolloAdapterException {
        Style style = getStyle();
        String so = style.getParameter("SequenceOntologyOBO");
        if (so == null || so.length() == 0) {
            throw new ApolloAdapterException("No valid SO OBO file found");
        }
        String apollo = style.getParameter("ApolloOBO");
        if (apollo == null || apollo.length() == 0) {
            throw new ApolloAdapterException("No valid Apollo OBO extension found");
        }
        LinkedList<String> fnames = new LinkedList<String>();
        String soFile = IOUtil.findFile(so);
        String apolloFile = IOUtil.findFile(apollo);
        if (soFile == null || Config.isJavaWebStartApplication()) {
            soFile = Config.getRootDir() + "/obo-files/" + so;
            File f = new File(soFile);
            Config.ensureExists(f, so, true);
        }
        if (apolloFile == null || Config.isJavaWebStartApplication()) {
            apolloFile = Config.getRootDir() + "/obo-files/" + apollo;
            File f = new File(apolloFile);
            Config.ensureExists(f, apollo, true);
        }
        fnames.add(soFile);
        fnames.add(apolloFile);
        return fnames;
    }

    private class GFF3Data {

        private GFF3Entry entry;

        private SeqFeatureI feat;

        public GFF3Data(GFF3Entry entry, SeqFeatureI feat) {
            this.entry = entry;
            this.feat = feat;
        }

        public GFF3Entry getEntry() {
            return entry;
        }

        public SeqFeatureI getFeat() {
            return feat;
        }
    }

    private class PositionComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            SeqFeatureI feat1 = (SeqFeatureI) o1;
            SeqFeatureI feat2 = (SeqFeatureI) o2;
            if (feat1.getLow() < feat2.getLow()) {
                return -1;
            } else if (feat1.getLow() > feat2.getLow()) {
                return 1;
            } else {
                if (feat1.getHigh() < feat2.getHigh()) {
                    return -1;
                } else if (feat1.getHigh() > feat2.getHigh()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }
    }

    private static boolean isUrl(String fname) {
        return fname.contains("://");
    }

    private static Reader getReader(String fname) throws IOException {
        InputStream is;
        if (isUrl(fname)) {
            URL url = new URL(fname);
            is = url.openStream();
        } else {
            is = new FileInputStream(fname);
        }
        if (fname.endsWith(".zip")) {
            is = new ZipInputStream(is);
        } else if (fname.endsWith(".gz") || fname.endsWith(".gzip")) {
            is = new GZIPInputStream(is);
        }
        return new InputStreamReader(is);
    }
}
