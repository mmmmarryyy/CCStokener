package uk.ac.ebi.intact.util.controlledVocab;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import uk.ac.ebi.intact.model.CvTopic;
import uk.ac.ebi.intact.model.CvXrefQualifier;
import uk.ac.ebi.intact.util.controlledVocab.model.*;
import uk.ac.ebi.ook.loader.impl.AbstractLoader;
import uk.ac.ebi.ook.loader.parser.OBOFormatParser;
import uk.ac.ebi.ook.model.implementation.AnnotationBean;
import uk.ac.ebi.ook.model.implementation.DbXrefBean;
import uk.ac.ebi.ook.model.implementation.TermBean;
import uk.ac.ebi.ook.model.implementation.TermSynonymBean;
import uk.ac.ebi.ook.model.interfaces.DbXref;
import uk.ac.ebi.ook.model.interfaces.TermRelationship;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapper class that hides the way OLS handles OBO files.
 *
 * @author Samuel Kerrien
 * @version $Id: PSILoader.java 4749 2006-04-05 08:19:54Z skerrien $
 * @since <pre>30-Sep-2005</pre>
 */
public class PSILoader extends AbstractLoader {

    protected void configure() {
        logger = Logger.getLogger(PSILoader.class);
        parser = new OBOFormatParser();
        ONTOLOGY_DEFINITION = "PSI MI";
        FULL_NAME = "PSI Molecular Interactions";
        SHORT_NAME = "PSI-MI";
        FQCN = PSILoader.class.getName();
    }

    protected void parse(Object params) {
        try {
            Vector v = new Vector();
            v.add((String) params);
            ((OBOFormatParser) parser).configure(v);
            parser.parseFile();
        } catch (Exception e) {
            logger.fatal("Parse failed: " + e.getMessage(), e);
        }
    }

    protected void printUsage() {
    }

    private List getRelatedSynonyms(TermBean term) throws PsiLoaderException {
        if (term == null) {
            throw new IllegalArgumentException("You must give a non null TermBean.");
        }
        List synonyms = new ArrayList(4);
        for (Iterator itSyn = term.getSynonyms().iterator(); itSyn.hasNext(); ) {
            TermSynonymBean syn = (TermSynonymBean) itSyn.next();
            if (syn.getSynonymType() != null) {
                if (syn.getSynonymType().getName().equals("related")) {
                    synonyms.add(syn.getSynonym());
                }
            }
        }
        if (synonyms.isEmpty()) {
            synonyms = Collections.EMPTY_LIST;
        }
        return synonyms;
    }

    /**
     * Returns an exact synonym if any available.
     *
     * @param term the term from which we want to get an exact_synonym.
     *
     * @return an exact synonym or null if none found.
     *
     * @throws PsiLoaderException if more than one exact synonym are available.
     */
    private String getExactSynonym(TermBean term) throws PsiLoaderException {
        if (term == null) {
            throw new IllegalArgumentException("You must give a non null TermBean.");
        }
        if (term.getSynonyms() == null) {
            return null;
        }
        String synonym = null;
        for (Iterator it_syn = term.getSynonyms().iterator(); it_syn.hasNext(); ) {
            TermSynonymBean syn = (TermSynonymBean) it_syn.next();
            if (syn.getSynonymType() != null) {
                if (syn.getSynonymType().getName().equals("exact")) {
                    if (synonym != null) {
                        throw new PsiLoaderException("CV Term '" + term.getIdentifier() + "' has more than one " + "exact_synonym found: '" + synonym + "' and '" + syn.getSynonym() + "'");
                    } else {
                        synonym = syn.getSynonym();
                    }
                }
            }
        }
        return synonym;
    }

    private static final Pattern ID_PATTERN = Pattern.compile("(MI|IA):\\d+");

    private boolean isIdentifierValid(TermBean term) {
        Matcher matcher = ID_PATTERN.matcher(term.getIdentifier());
        return matcher.matches();
    }

    /**
     * XML character escaper.
     *
     * @param input String that can be null.
     *
     * @return the escaped string or null if non was given.
     */
    private String escapeXMLTags(final String input) {
        if (input == null) {
            return null;
        }
        return StringEscapeUtils.unescapeXml(input);
    }

    private IntactOntology buildIntactOntology() throws PsiLoaderException {
        IntactOntology ontology = new IntactOntology();
        for (Iterator it_term = ontBean.getTerms().iterator(); it_term.hasNext(); ) {
            TermBean term = (TermBean) it_term.next();
            String id = escapeXMLTags(term.getIdentifier());
            if (!isIdentifierValid(term)) {
                continue;
            }
            String shortlabel = escapeXMLTags(getExactSynonym(term));
            if (shortlabel == null) {
                shortlabel = escapeXMLTags(term.getName());
                if (shortlabel != null && shortlabel.length() > 20) {
                    System.out.println("NOTE: term " + id + " has its name longer than 20 chars. it should have an exact_synonym.");
                }
            }
            CvTerm cvTerm = new CvTerm(id, shortlabel);
            if (term.getName() != null && !"".equals(term.getName().trim())) {
                cvTerm.setFullName(escapeXMLTags(term.getName()));
            } else {
                cvTerm.setFullName(shortlabel);
            }
            String definition = escapeXMLTags(term.getDefinition());
            if (definition != null && !"".equals(definition.trim())) {
                cvTerm.setDefinition(definition);
            }
            if (term.getXrefs() != null) {
                Set identity = new HashSet();
                Set primaryReference = new HashSet();
                for (Iterator itXref = term.getXrefs().iterator(); itXref.hasNext(); ) {
                    DbXrefBean dbXref = (DbXrefBean) itXref.next();
                    String type = escapeXMLTags(dbXref.getDbName());
                    String accession = escapeXMLTags(dbXref.getAccession());
                    String desc = escapeXMLTags(dbXref.getDescription());
                    switch(dbXref.getXrefType()) {
                        case DbXref.OBO_DBXREF_ANALOG:
                            if ("ANNOTATION".equalsIgnoreCase(desc)) {
                                if (type.equals("id-validation-regexp")) {
                                    if (accession.startsWith("\"")) {
                                        accession = accession.substring(1, accession.length());
                                    }
                                    if (accession.endsWith("\"")) {
                                        accession = accession.substring(0, accession.length() - 1);
                                    }
                                    accession = accession.trim();
                                }
                                CvTermAnnotation annotation = new CvTermAnnotation(type, accession);
                                cvTerm.addAnnotation(annotation);
                            } else if ("ALIAS".equalsIgnoreCase(desc)) {
                                CvTermSynonym synonym = new CvTermSynonym(type, accession);
                                cvTerm.addSynonym(synonym);
                            } else {
                                throw new IllegalStateException("Unsupported Xref Type: " + type);
                            }
                            break;
                        case DbXref.OBO_DBXREF_DEFINITION:
                            if (CvXrefQualifier.IDENTITY.equals(desc)) {
                                if (identity.contains(type)) {
                                    throw new PsiLoaderException("CV Term '" + id + "' has more than one " + "Xref(identity, " + type + ")");
                                } else {
                                    identity.add(type);
                                }
                            } else if (CvXrefQualifier.IDENTITY.equals(desc)) {
                                if (primaryReference.contains(type)) {
                                    throw new PsiLoaderException("CV Term '" + id + "' has more than one " + "Xref(primary-reference, " + type + "): " + accession);
                                } else {
                                    primaryReference.add(type);
                                }
                            }
                            CvTermXref xref = new CvTermXref(accession, type, desc);
                            cvTerm.addXref(xref);
                            break;
                        default:
                    }
                }
            }
            if (term.getSynonyms() != null) {
                for (Iterator itSyn = getRelatedSynonyms(term).iterator(); itSyn.hasNext(); ) {
                    String syn = (String) itSyn.next();
                    CvTermSynonym synonym = new CvTermSynonym(escapeXMLTags(syn));
                    cvTerm.addSynonym(synonym);
                }
            }
            if (term.getAnnotations() != null) {
                for (Iterator itAnnot = term.getAnnotations().iterator(); itAnnot.hasNext(); ) {
                    AnnotationBean annot = (AnnotationBean) itAnnot.next();
                    CvTermAnnotation annotation = new CvTermAnnotation(escapeXMLTags(annot.getAnnotationName()), escapeXMLTags(annot.getAnnotationValue()));
                    cvTerm.addAnnotation(annotation);
                }
            }
            cvTerm.setObsolete(term.isObsolete());
            String message = null;
            for (Iterator iterator = cvTerm.getAnnotations().iterator(); iterator.hasNext() && message == null; ) {
                CvTermAnnotation annotation = (CvTermAnnotation) iterator.next();
                if (CvTopic.OBSOLETE.equals(annotation.getTopic())) {
                    message = annotation.getAnnotation();
                }
            }
            if (message != null) {
                cvTerm.setObsoleteMessage(message);
            }
            ontology.addTerm(cvTerm);
        }
        for (Iterator iterator = ontBean.getTerms().iterator(); iterator.hasNext(); ) {
            TermBean term = (TermBean) iterator.next();
            if (term.getRelationships() != null) {
                for (Iterator itRelationship = term.getRelationships().iterator(); itRelationship.hasNext(); ) {
                    TermRelationship relation = (TermRelationship) itRelationship.next();
                    ontology.addLink(relation.getObjectTerm().getIdentifier(), relation.getSubjectTerm().getIdentifier());
                }
            }
        }
        ontology.updateMapping();
        return ontology;
    }

    public static final String NEW_LINE = System.getProperty("line.separator");

    public static final String SOURCEFORGE_URL = "http://psidev.sourceforge.net/mi/rel25/data/psi-mi25.obo";

    /**
     * Load the latest PSI MI definition from sourceforge.
     *
     * @return
     *
     * @throws PsiLoaderException
     */
    public IntactOntology loadLatestPsiMiFromSourceforge() throws PsiLoaderException {
        try {
            return parseOboFile(new URL(SOURCEFORGE_URL));
        } catch (MalformedURLException e) {
            throw new PsiLoaderException("Error loading the latest definition of PSI-MI 2.5 from: " + SOURCEFORGE_URL);
        }
    }

    /**
     * Load an OBO file from an URL.
     *
     * @param url               the URL to load (must not be null)
     * @param keepTemporaryFile if set to false, the temp file containing the dwonloaded data is deleted.
     *
     * @return an ontology
     *
     * @throws PsiLoaderException
     * @see #parseOboFile( File file )
     */
    public IntactOntology parseOboFile(URL url, boolean keepTemporaryFile) throws PsiLoaderException {
        if (url == null) {
            throw new IllegalArgumentException("Please give a non null URL.");
        }
        StringBuffer buffer = new StringBuffer(1024 * 8);
        try {
            System.out.println("Loading URL: " + url);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()), 1024);
            String line;
            int lineCount = 0;
            while ((line = in.readLine()) != null) {
                lineCount++;
                buffer.append(line).append(NEW_LINE);
                if ((lineCount % 20) == 0) {
                    System.out.print(".");
                    System.out.flush();
                    if ((lineCount % 500) == 0) {
                        System.out.println("   " + lineCount);
                    }
                }
            }
            in.close();
            File tempDirectory = new File(System.getProperty("java.io.tmpdir", "tmp"));
            if (!tempDirectory.exists()) {
                if (!tempDirectory.mkdirs()) {
                    throw new IOException("Cannot create temp directory: " + tempDirectory.getAbsolutePath());
                }
            }
            System.out.println("Using temp directory: " + tempDirectory.getAbsolutePath());
            File tempFile = File.createTempFile("psimi.v25.", ".obo", tempDirectory);
            tempFile.deleteOnExit();
            tempFile.deleteOnExit();
            System.out.println("The OBO file is temporary store as: " + tempFile.getAbsolutePath());
            BufferedWriter out = new BufferedWriter(new FileWriter(tempFile), 1024);
            out.write(buffer.toString());
            out.flush();
            out.close();
            return parseOboFile(tempFile);
        } catch (IOException e) {
            throw new PsiLoaderException("Error while loading URL (" + url + ")", e);
        }
    }

    /**
     * Load an OBO file from an URL.
     *
     * @param url the URL to load (must not be null)
     *
     * @return an ontology
     *
     * @throws PsiLoaderException
     * @see #parseOboFile( File file )
     */
    public IntactOntology parseOboFile(URL url) throws PsiLoaderException {
        return parseOboFile(url, false);
    }

    /**
     * Parse the given OBO file and build a representation of the DAG into an IntactOntology.
     *
     * @param file the input file. It has to exist and to be readable, otherwise it will break.
     *
     * @return a non null IntactOntology.
     */
    public IntactOntology parseOboFile(File file) throws PsiLoaderException {
        if (!file.exists()) {
            throw new IllegalArgumentException(file.getAbsolutePath() + " doesn't exist.");
        }
        if (!file.canRead()) {
            throw new IllegalArgumentException(file.getAbsolutePath() + " could not be read.");
        }
        configure();
        System.out.println("Reading " + file.getAbsolutePath());
        parse(file.getAbsolutePath());
        process();
        return buildIntactOntology();
    }
}
