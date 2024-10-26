package uk.ac.ebi.intact.uniprot.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.aristotle.model.sptr.AristotleSPTRException;
import uk.ac.ebi.aristotle.model.sptr.comment.Function;
import uk.ac.ebi.aristotle.model.sptr.feature.PolypeptideChainFeature;
import uk.ac.ebi.aristotle.util.interfaces.AlternativeSplicingAdapter;
import uk.ac.ebi.intact.uniprot.UniprotServiceException;
import uk.ac.ebi.intact.uniprot.model.*;
import uk.ac.ebi.interfaces.Factory;
import uk.ac.ebi.interfaces.feature.Feature;
import uk.ac.ebi.interfaces.feature.FeatureException;
import uk.ac.ebi.interfaces.feature.FeatureLocation;
import uk.ac.ebi.interfaces.sptr.*;
import uk.ac.ebi.sptr.flatfile.yasp.EntryIterator;
import uk.ac.ebi.sptr.flatfile.yasp.YASP;
import uk.ac.ebi.sptr.flatfile.yasp.YASPException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.URL;
import java.util.*;

/**
 * Adapter to read UniProt entries using YASP/Aristotle.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id: YaspService.java 9836 2007-10-04 16:02:34Z baranda $
 * @since <pre>15-Sep-2006</pre>
 */
@Deprecated
public class YaspService extends AbstractUniprotService {

    /**
     * Sets up a logger for that class.
     */
    public static final Log log = LogFactory.getLog(YaspService.class);

    /**
     * Expected content of the beginning of the first line of a UniProt flat file.
     */
    public static final String FIRST_LINE_FIRST_FIVE_CHARS = "ID   ";

    public static final int CHAR_TO_READ = FIRST_LINE_FIRST_FIVE_CHARS.length();

    public Collection<UniprotProtein> retrieve(String ac) {
        if (ac == null) {
            throw new IllegalArgumentException("You must give a non null protein AC.");
        }
        String entryUrl = buildUniprotSearchUrl(ac);
        InputStream is = null;
        Collection<UniprotProtein> proteins = new ArrayList<UniprotProtein>();
        try {
            is = checkUrlDataFormat(new URL(entryUrl));
        } catch (IOException e) {
            addError(ac, new UniprotServiceReport("Error upon reading URL: " + entryUrl, e));
            return proteins;
        } catch (UniprotServiceException e) {
            addError(ac, new UniprotServiceReport("UniProt entry has invalid format: " + entryUrl, e));
            return proteins;
        }
        try {
            Collection<UniprotProtein> p = retreive(is, ac);
            if (p != null) {
                proteins.addAll(p);
                log.debug("[AC: " + ac + "] Retreived " + p.size() + " protein(s).");
            } else {
                log.error("");
            }
        } catch (UniprotServiceException e) {
            addError(ac, new UniprotServiceReport("Error while processing UniProt entry: " + ac, e));
            return proteins;
        }
        try {
            is.close();
        } catch (IOException e) {
            addError(ac, new UniprotServiceReport("Error while closing URL: " + entryUrl, e));
        }
        return proteins;
    }

    public Map<String, Collection<UniprotProtein>> retrieve(Collection<String> acs) {
        if (acs == null) {
            throw new IllegalArgumentException("You must give a non null List of UniProt ACs.");
        }
        if (acs.isEmpty()) {
            throw new IllegalArgumentException("You must give a non empty List of UniProt ACs.");
        }
        Map<String, Collection<UniprotProtein>> results = new HashMap<String, Collection<UniprotProtein>>(acs.size());
        String ac = null;
        for (Iterator<String> iterator = acs.iterator(); iterator.hasNext(); ) {
            ac = iterator.next();
            Collection<UniprotProtein> proteins = retrieve(ac);
            if (proteins != null) {
                results.put(ac, proteins);
            } else {
                addError(ac, new UniprotServiceReport("Could not retreive any proteins for UniProt AC: " + ac));
            }
        }
        return results;
    }

    @Deprecated
    public Collection<UniprotProtein> retreive(String ac) {
        return retrieve(ac);
    }

    @Deprecated
    public Map<String, Collection<UniprotProtein>> retreive(Collection<String> acs) {
        return retrieve(acs);
    }

    /**
     * Open the given URL and check that the data pointed to start with the expected prefix.
     * <p/>
     * The check is on the 5 first char contained in the InputStream, they have to match 'ID   '.
     *
     * @param url the URL pointing to the UniProt protein entry (can be one or more).
     *
     * @return the input stream one can read the entry (1..n) from.
     *
     * @throws UniprotServiceException if the format of the entry is not as expected.
     * @throws IOException
     */
    private InputStream checkUrlDataFormat(URL url) throws UniprotServiceException, IOException {
        if (url == null) {
            throw new IllegalArgumentException("URL must not be null.");
        }
        PushbackInputStream pis = new PushbackInputStream(url.openStream(), 5);
        byte[] b = new byte[CHAR_TO_READ];
        pis.read(b, 0, CHAR_TO_READ);
        if (b.length < CHAR_TO_READ) {
            throw new RuntimeException("Could not read the whole 5 bytes");
        }
        String fiveFirstChars = new String(b);
        if (!FIRST_LINE_FIRST_FIVE_CHARS.equals(fiveFirstChars)) {
            throw new UniprotServiceException("Invalid UniProt entry format. An entry is expected to start with :'" + FIRST_LINE_FIRST_FIVE_CHARS + "' and not '" + fiveFirstChars + "'.");
        }
        pis.unread(b);
        return pis;
    }

    /**
     * Build a URL allowing to access the UniProt flat file given a specific UniProt AC.
     *
     * @param ac (non null) UniProt AC.
     *
     * @return a URL as a String.
     */
    private String buildUniprotSearchUrl(String ac) {
        if (ac == null) {
            throw new IllegalArgumentException("You must give a non null AC.");
        }
        String url = "http://www.ebi.uniprot.org/entry/" + ac + "?format=text&ascii";
        log.debug("Built URL: " + url);
        return url;
    }

    /**
     * Build UniprotProteins based on a given InputStream.
     *
     * @param is InputStream on the data.
     * @param ac protein ac
     *
     * @return a non null collection of UniprotProteins.
     *
     * @throws UniprotServiceException
     */
    private Collection<UniprotProtein> retreive(InputStream is, String ac) throws UniprotServiceException {
        if (is == null) {
            throw new IllegalArgumentException("You must give a non null InputStream.");
        }
        if (ac == null) {
            throw new IllegalArgumentException("You must give a non null AC.");
        }
        Collection<UniprotProtein> uniprotProteins = new ArrayList<UniprotProtein>(4);
        UniprotProtein uniprotProtein = null;
        try {
            EntryIterator entryIterator = YASP.parseAll(is);
            int entryCount = 0;
            while (entryIterator.hasNext()) {
                entryCount++;
                if (entryIterator.hadException()) {
                    YASPException originalException = entryIterator.getException();
                    throw originalException;
                }
                SPTREntry sptrEntry = (SPTREntry) entryIterator.next();
                if (sptrEntry == null) {
                    if (log != null) {
                        log.error("\n\nSPTR entry is NULL ... skip it");
                    }
                    continue;
                }
                log.info("Processing " + sptrEntry.getID() + " ...");
                uniprotProtein = buildUniprotProtein(sptrEntry);
                uniprotProteins.add(uniprotProtein);
            }
        } catch (YASPException e) {
            throw new UniprotServiceException("A YASP error occured while processing", e);
        } catch (SPTRException e) {
            throw new UniprotServiceException("An SPTR error occured while processing", e);
        } catch (IOException e) {
            throw new UniprotServiceException("An IO error occured while processing", e);
        } catch (FeatureException e) {
            throw new UniprotServiceException("An error occured while processing the features of an entry", e);
        }
        return uniprotProteins;
    }

    /**
     * Builds a UniprotProtein from a SPTREntry.
     *
     * @param sptrEntry
     *
     * @return
     *
     * @throws SPTRException
     * @throws FeatureException
     */
    private UniprotProtein buildUniprotProtein(SPTREntry sptrEntry) throws SPTRException, FeatureException {
        int organismCount = sptrEntry.getOrganismNames().length;
        if (organismCount > 1) {
            throw new IllegalStateException("Entry: " + sptrEntry.getID() + ": expected to find a single organism. Instead found " + organismCount);
        }
        String organismName = sptrEntry.getOrganismNames()[0];
        String entryTaxid = sptrEntry.getNCBITaxonomyID(organismName);
        Organism o = new Organism(Integer.parseInt(entryTaxid), organismName);
        String[] taxons = sptrEntry.getTaxonomy(organismName);
        for (int i = 0; i < taxons.length; i++) {
            String taxon = taxons[i];
            o.getParents().add(taxon);
        }
        UniprotProtein uniprotProtein = new UniprotProtein(sptrEntry.getID(), sptrEntry.getAccessionNumbers()[0], o, sptrEntry.getProteinName());
        String proteinAC[] = sptrEntry.getAccessionNumbers();
        if (proteinAC.length > 1) {
            for (int i = 1; i < proteinAC.length; i++) {
                String ac = proteinAC[i];
                uniprotProtein.getSecondaryAcs().add(ac);
            }
        }
        uniprotProtein.setReleaseVersion(getSPTREntryReleaseVersion(sptrEntry));
        uniprotProtein.setLastAnnotationUpdate(new Date(sptrEntry.getLastAnnotationUpdateDate().getTime()));
        uniprotProtein.setLastSequenceUpdate(new Date(sptrEntry.getLastSequenceUpdateDate().getTime()));
        if (SPTREntry.SWISSPROT == sptrEntry.getEntryType()) {
            uniprotProtein.setSource(UniprotProteinType.SWISSPROT);
        } else if (SPTREntry.TREMBL == sptrEntry.getEntryType()) {
            uniprotProtein.setSource(UniprotProteinType.TREMBL);
        } else if (SPTREntry.UNKNOWN == sptrEntry.getEntryType()) {
            uniprotProtein.setSource(UniprotProteinType.UNKNOWN);
        } else {
            throw new IllegalStateException("Only SWISSPROT, TREMBL and UNKNOWN source are supported: " + sptrEntry.getEntryType());
        }
        processGeneNames(sptrEntry, uniprotProtein);
        SPTRComment[] comments = sptrEntry.getAllComments();
        for (int i = 0; i < comments.length; i++) {
            SPTRComment comment = comments[i];
            log.debug("comment.getClass().getSimpleName() = " + comment.getClass().getSimpleName());
            if (comment instanceof Function) {
                log.debug("Found a Comment( Function ).");
                Function function = (Function) comment;
                uniprotProtein.getFunctions().add(function.getDescription());
            }
        }
        SPTRComment[] diseases = sptrEntry.getComments("DISEASE");
        for (int i = 0; i < diseases.length; i++) {
            String disease = comments[i].getPropertyValue(0);
            uniprotProtein.getDiseases().add(disease);
        }
        String[] keywords = sptrEntry.getKeywords();
        for (int i = 0; i < keywords.length; i++) {
            String keyword = keywords[i];
            uniprotProtein.getKeywords().add(keyword);
        }
        processCrossReference(sptrEntry, uniprotProtein);
        uniprotProtein.setSequence(sptrEntry.getSequence());
        uniprotProtein.setSequenceLength(sptrEntry.getSQSequenceLength());
        uniprotProtein.setCrc64(sptrEntry.getCRC64());
        processSpliceVariants(sptrEntry, uniprotProtein);
        processFeatureChain(sptrEntry, uniprotProtein);
        return uniprotProtein;
    }

    /**
     * Extract from the SPTREntry the annotation release and the entry type, then combine them to get a version we will
     * use in the Xref. uniprot, identity )
     *
     * @param sptrEntry the entry from which we extract the information.
     *
     * @return a version as a String.
     *
     * @throws SPTRException
     */
    private String getSPTREntryReleaseVersion(SPTREntry sptrEntry) throws SPTRException {
        String version = null;
        String uniprotRelease = sptrEntry.getLastAnnotationUpdateRelease();
        if (sptrEntry.getEntryType() == SPTREntry.SWISSPROT) {
            version = SWISS_PROT_PREFIX + uniprotRelease;
        } else if (sptrEntry.getEntryType() == SPTREntry.TREMBL) {
            version = TREMBL_PREFIX + uniprotRelease;
        } else {
            log.warn("Unexpected SPTREntry type: " + sptrEntry.getEntryType());
            version = uniprotRelease;
        }
        return version;
    }

    private void processFeatureChain(SPTREntry sptrEntry, UniprotProtein protein) throws SPTRException {
        Feature[] features = sptrEntry.getFeatures();
        for (Feature feature : features) {
            if (feature instanceof PolypeptideChainFeature) {
                log.debug("Found a PolypeptideChainFeature");
                PolypeptideChainFeature pcf = (PolypeptideChainFeature) feature;
                String id = pcf.getID();
                String chainSequence = null;
                String[] sequences = pcf.getSequenceVariations();
                FeatureLocation location = pcf.getLocation();
                Integer begin = null;
                Integer end = null;
                try {
                    begin = location.getLocationBegin();
                    end = location.getLocationEnd();
                } catch (FeatureException e) {
                }
                if (sequences.length > 0) {
                    for (int i = 0; i < sequences.length; i++) {
                        log.debug("sequence[i] = " + sequences[i]);
                    }
                    if (sequences.length > 1) {
                        log.debug("Pick the first sequence out of " + sequences.length + ";");
                        chainSequence = sequences[0];
                    }
                } else {
                    log.warn("Yasp doesn't seem to provide alternative sequence for that feature. We go DIY-style...");
                    if (begin != null && end != null) {
                        chainSequence = protein.getSequence().substring(begin - 1, end);
                    } else {
                        if (log.isWarnEnabled()) {
                            if (begin == null) {
                                log.warn("Begin of feature range was missing.");
                            }
                            if (end == null) {
                                log.warn("End of feature range was missing.");
                            }
                            log.warn("Could not build the chain sequence => Skipping feature chain " + id);
                        }
                        continue;
                    }
                }
                UniprotFeatureChain chain = new UniprotFeatureChain(id, protein.getOrganism(), chainSequence);
                chain.setStart(begin);
                chain.setEnd(end);
                protein.getFeatureChains().add(chain);
            }
        }
    }

    private void processGeneNames(SPTREntry sptrEntry, UniprotProtein protein) throws SPTRException {
        log.debug("Processing gene names...");
        Gene[] genes = sptrEntry.getGenes();
        for (Gene gene : genes) {
            String geneName = gene.getName();
            if (geneName != null && (false == "".equals(geneName.trim()))) {
                protein.getGenes().add(geneName);
            }
            String[] synonyms = gene.getSynonyms();
            if (synonyms.length > 0) {
                for (String syn : synonyms) {
                    protein.getSynomyms().add(syn);
                }
            }
            String[] locus = gene.getLocusNames();
            if (locus.length > 0) {
                for (String l : locus) {
                    protein.getLocuses().add(l);
                }
            }
            String[] orfs = gene.getORFNames();
            if (orfs.length > 0) {
                for (String orf : orfs) {
                    protein.getOrfs().add(orf);
                }
            }
        }
    }

    private void processCrossReference(SPTREntry sptrEntry, UniprotProtein protein) throws SPTRException {
        log.debug("Processing cross references...");
        SPTRCrossReference cr[] = sptrEntry.getCrossReferences();
        for (SPTRCrossReference sptrXref : cr) {
            String ac = sptrXref.getAccessionNumber();
            String db = sptrXref.getDatabaseName();
            if (getCrossReferenceSelector() != null && !getCrossReferenceSelector().isSelected(db)) {
                log.debug(getCrossReferenceSelector().getClass().getSimpleName() + " filtered out database: '" + db + "'.");
                continue;
            }
            String desc = null;
            try {
                desc = sptrXref.getPropertyValue(SPTRCrossReference.SECONDARY_PROPERTY);
            } catch (AristotleSPTRException e) {
            }
            protein.getCrossReferences().add(new UniprotXref(ac, db, desc));
        }
        if (getCrossReferenceSelector() != null && (getCrossReferenceSelector().isSelected("HUGE") || getCrossReferenceSelector().isSelected("KIAA"))) {
            Gene[] genes = sptrEntry.getGenes();
            for (Gene gene : genes) {
                String geneName = gene.getName();
                if (geneName.startsWith("KIAA")) {
                    protein.getCrossReferences().add(new UniprotXref(geneName, "HUGE"));
                }
                String[] synonyms = gene.getSynonyms();
                for (String syn : synonyms) {
                    if (syn.startsWith("KIAA")) {
                        protein.getCrossReferences().add(new UniprotXref(syn, "HUGE"));
                    }
                }
            }
        }
    }

    private void processSpliceVariants(SPTREntry sptrEntry, UniprotProtein protein) throws SPTRException, FeatureException {
        log.debug("Processing splice variants...");
        SPTRComment[] comments = sptrEntry.getComments(Factory.COMMENT_ALTERNATIVE_SPLICING);
        for (int j = 0; j < comments.length; j++) {
            SPTRComment comment = comments[j];
            if (!(comment instanceof AlternativeSplicingAdapter)) {
                log.error("Looking for Comment type: " + AlternativeSplicingAdapter.class.getName());
                log.error("Could not handle comment type: " + comment.getClass().getName());
                log.error("SKIP IT.");
                continue;
            }
            AlternativeSplicingAdapter asa = (AlternativeSplicingAdapter) comment;
            Isoform[] isoForms = asa.getIsoforms();
            for (int ii = 0; ii < isoForms.length; ii++) {
                Isoform isoForm = isoForms[ii];
                String[] ids = isoForm.getIDs();
                if (ids.length > 0) {
                    String spliceVariantID = ids[0];
                    log.debug("Splice variant ID: " + spliceVariantID);
                    String sequence = sptrEntry.getAlternativeSequence(isoForm);
                    UniprotSpliceVariant sv = new UniprotSpliceVariant(ids[0], protein.getOrganism(), sequence);
                    protein.getSpliceVariants().add(sv);
                    if (ids.length > 1) {
                        for (int i = 1; i < ids.length; i++) {
                            String id = ids[i];
                            sv.getSecondaryAcs().add(id);
                        }
                    }
                    if (isoForm.getNote() != null) {
                        sv.setNote(isoForm.getNote());
                    }
                    for (int i = 0; i < isoForm.getSynonyms().length; i++) {
                        sv.getSynomyms().add(isoForm.getSynonyms()[i]);
                    }
                }
            }
        }
    }
}
