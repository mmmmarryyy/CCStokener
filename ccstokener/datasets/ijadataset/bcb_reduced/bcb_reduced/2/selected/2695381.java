package oboes;

import java.util.*;
import java.lang.*;
import java.io.*;
import java.util.regex.*;
import java.net.*;
import java.io.*;

/**
 * Class <code>OBOES</code>, which stands for <b>OBO</b> (Open Biomedical Ontology) <b>E</b>nrichment <b>S</b>earch,
 * represents the main user interface to the OBOES framework.
 * <p>
 * Usage of the OBOES framework typically involves instantiating an <code>OBOES</code> object. Using <code>loadOntology</code>,
 * ontologies can be loaded from OBO files. See <a href="http://www.geneontology.org/GO.format.shtml#oboflat">http://www.geneontology.org/GO.format.shtml#oboflat</a>
 * for a description of the OBO file format. Ontologies loaded by <code>loadOntology</code> are made available in the
 * <code>ontologies</code> field of <code>OBOES</code>.
 * <p>
 * After loading one or more ontologies, invoke the <code>loadAnnotation</code> method, which reads in annotation data
 * from a specified file. Using these annotation data, accession-to-term mappings are constructed which are used when
 * the functions <code>getSimpleEnrichment</code>, <code>getUnderRepresentedTerms</code>, and <code>getCompoundEnrichment</code> are called.
 * See {@link Enrichment}, {@link SimpleEnrichment}, {@link CompoundEnrichment} for
 * information on working with the output from those methods.
 * <p>
 * <code>OBOES</code> maintains a "minimum count" and "maximum p-value" as cut-offs to decide when a Term or Terms is
 * significantly enriched or not. In order for a Term or Terms to be significantly enriched, the number of accessions
 * annotated with that Term or the entire set of Terms must be at least the "minimum count." In addition, the
 * statistically computed p-value must be at most the "maximum p-value." The "minimum count" defaults to 2; the
 * "maximum p-value" defaults to 0.01, which is usually a standard choice to balance between Type I and Type II errors.
 * The "minimum count" and "maximum p-value" can be manually set and retrieved using <code>OBOES</code> methods. Any
 * invocation of <code>getSimpleEnrichment</code> and <code>getCompoundEnrichment</code> will only consider an
 * enrichment to be significant if it satisfies the "maximum p-value" and "minimum count" values that are currently
 * set in the <code>OBOES</code> object.
 * <p>
 * <code>OBOES</code> provides additional methods that influence the statistics involved in searching for enrichment.
 * For example, <code>OBOES</code> defaults the total sample space size to the total number of unique accessions
 * loaded through <code>loadAnnotation</code> (or <code>loadBackgroundAnnotation</code>, if invoked; see {@link loadBackgroundAnnotation}).
 * The total sample space is understood to refer to the entire set
 * of possible accessions from which the accessions in a given input sample are selected. For example, the total
 * sample space for a microarray experiment would be the whole genome of the organism, and the sample space size
 * is then equal to the number of genes for the organism. <code>OBOES</code> also provides a variety of
 * <em>multiple hypothesis correction</em> schemes, which are ways to keep the overall likelihood of encountering
 * a false positive low, in the face of testing many hypotheses. Multiple hypothesis correction is automatically
 * employed by <code>getSimpleEnrichment</code>, since it tests all possible enriched Terms in a single invocation.
 * They are not employed by <code>getCompoundEnrichment</code>, since it tests only one hypothesis in an invocation.
 * For this reason, <code>OBOES</code> features an interface for its multiple hypothesis correction schemes (see
 * the <code>applyMultipleHypothesisCorrection</code> method) that allows you to apply various correction schemes
 * to the results you obtain at your own discretion.
 * <p>
 * An example of basic usage of <code>OBOES</code> in MATLAB, with loading of the Gene Ontology (GO), might occur as follows:
 * <p>
 * <code>
 * myOBOES = OBOES(); <br>
 * myOBOES.loadOntology('C:\data\gene_ontology.obo', 'gene ontology'); <br>
 * myOBOES.loadAnnotation('C:\data\organismal_gene_annotation.txt', 'gene ontology'); <br>
 * enrichment = myOBOES.getSimpleEnrichment(exp12_genes, 'gene ontology');
 * </code>
 * <p>
 * To analyze the results, you could then write statements such as:
 * <p>
 * <code>
 * enrichment(1).term.name <br>
 * enrichment(1).p_value
 * </code>
 * <p>
 * and so forth. The code above assumes that the file "organismal_gene_annotation.txt" contains a mapping of gene accessions
 * to GO terms, and that the variable <code>exp12_genes</code> is an array of the accessions of the input
 * sample (presumeably, by the name of the variable, from the output of an experiment). The OBO for GO can be
 * found online at <a href="http://www.geneontology.org">http://www.geneontology.org</a>.
 *
 * @see Enrichment
 * @see SimpleEnrichment
 * @see CompoundEnrichment
 * @see OBO_Object
 */
public class OBOES {

    private Hashtable ontologies;

    private int min_count;

    private double max_pval;

    private String correction_method;

    private int sample_space_size;

    private double[] logFactorial;

    private boolean verbose;

    /**
     * Instantiates an <code>OBOES</code> object. It defaults the "minimum count" to 2, the "maximum p-value" to
     * 0.01, and the multiple hypothesis correction scheme to the Bonferroni method. Additionally, the total
     * sample space is calculated automatically based on the total number of accessions loaded through <code>loadAnnotation</code>.
     * The total sample space size can be user-determined for statistical purposes via <code>setSampleSpaceSize</code>.
     * <p>
     * These defaults can be changed using <code>OBOES</code> methods that set the behavior of the <code>OBOES</code>
     * object.
     */
    public OBOES() {
        this.min_count = 2;
        this.max_pval = 0.01;
        this.correction_method = "bonferroni";
        this.ontologies = new Hashtable();
        this.sample_space_size = -1;
        this.verbose = true;
        this.logFactorial = new double[200000];
        this.logFactorial[0] = 0;
        double sum = 0;
        for (int i = 1; i < logFactorial.length; i++) {
            sum += Math.log(i);
            this.logFactorial[i] = sum;
        }
    }

    /******                         ******
     ****** BASIC INFRASTRUCTURE    ******
     ******                         ******/
    private static boolean isURL(String str) {
        str = str.toLowerCase();
        return str.length() > 4 && ((str.substring(0, 4).equals("http") || str.substring(0, 3).equals("ftp")));
    }

    /**
     * Returns the current multiple hypothesis correction method being used by <code>getSimpleEnrichment</code>.
     *
     * @return  the multiple hypothesis correction method
     */
    public String getCorrectionMethod() {
        return this.correction_method;
    }

    /**
     * Sets the multiple hypothesis correction method being used by <code>getSimpleEnrichment</code>
     *
     * @param method      the multiple hypothesis correction method; valid values are: "bonferroni", "sidak", "holm", "hochberg", "benjamini_hochberg", "none"
     */
    public void setCorrectionMethod(String method) {
        method = method.trim().toLowerCase();
        if (!method.equals("bonferroni") && !method.equals("sidak") && !method.equals("holm") && !method.equals("hochberg") && !method.equals("benjamini_hochberg") && !method.equals("none")) {
            System.out.println("Error: unrecognized correction method '" + method + "'.");
            System.out.println("Acceptable values are: 'bonferroni', 'sidak', 'holm', 'hochberg', 'benjamini_hochberg', 'none'.");
            return;
        }
        this.correction_method = method;
    }

    /**
     * Returns the current "minimum count" threshold being used by <code>getSimpleEnrichment</code> and <code>getCompoundEnrichment</code> as a test of significance.
     *
     * @return  the current value of "minimum count"
     */
    public int getMinimumCount() {
        return this.min_count;
    }

    /**
     * Sets the "minimum count" threshold being used by <code>getSimpleEnrichment</code> and <code>getCompoundEnrichment</code> as a test of significance.
     *
     * @param   count      value to set "minimum count" to
     */
    public void setMinimumCount(int count) {
        if (count < 1) {
            System.out.println("Error: minimum count cannot be set to less than 1.");
            return;
        }
        this.min_count = count;
    }

    /**
     * Enables output of status messages to the screen (default: status messages are displayed).
     */
    public void enableVerboseMode() {
        this.verbose = true;
    }

    /**
     * Disables output of status messages to the screen (default: status messages are displayed).
     */
    public void disableVerboseMode() {
        this.verbose = false;
    }

    /**
     * Returns the current "maximum p-value" threshold being used by <code>getSimpleEnrichment</code> and <code>getCompoundEnrichment</code> as a test of significance.
     *
     * @return  the current value of "maximum p-value"
     */
    public double getMaximumPvalue() {
        return this.max_pval;
    }

    /**
     * Sets the "maximum p-value" threshold being used by <code>getSimpleEnrichment</code> and <code>getCompoundEnrichment</code> as a test of significance.
     *
     * @param   pval      value to set "maximum p-value" to
     */
    public void setMaximumPvalue(double pval) {
        if (pval < 0 || pval > 1) {
            System.out.println("Error: maximum p-value must be between 0 and 1.");
            return;
        }
        this.max_pval = pval;
    }

    /**
     * Returns the current sample space size, if the automatic method of determining sample space size has been manually overridden. If not,
     * a message is displayed indicating that sample space size is being automatically determined, and a value less than 1 is returned.
     *
     * @return  the manually set sample space size, if positive; otherwise, a negative or zero return value signifies that no manual sample space size is currently set
     */
    public int getSampleSpaceSize() {
        if (this.sample_space_size < 1) {
            System.out.println("Manual sample space size is currently disabled. The sample space size is being determined by the total number of unique accessions loaded for one ontology (getSimpleEnrichment), or the total number of unique accessions loaded across all relevant ontologies (getCompoundEnrichment).");
        }
        return this.sample_space_size;
    }

    /**
     * Sets the current sample space size. Entering a value less than 1 results in disabling of the manual sample space size.
     *
     * @param   sample_space_size   if greater than 1: the new manual sample space size; otherwise, disables manual sample space size
     */
    public void setSampleSpaceSize(int sample_space_size) {
        this.sample_space_size = sample_space_size;
        if (sample_space_size < 1) {
            System.out.println("Manual sample space size disabled. Relying on annotation files to determine sample space size (default).");
        } else {
            System.out.println("Sample space size set manually to " + sample_space_size + ".");
        }
    }

    /**
     * Disables the manual sample space size. This can also be accomplished by setting the sample space size to a negative value
     * through <code>setSampleSpaceSize</code>.
     */
    public void disableManualSampleSpaceSize() {
        this.setSampleSpaceSize(-1);
    }

    private OBO_Object castToArrays(OBO_Object obj, ArrayList alt_id, ArrayList subset, ArrayList synonym, ArrayList related_synonym, ArrayList exact_synonym, ArrayList broad_synonym, ArrayList narrow_synonym, ArrayList xref_analog, ArrayList xref_unknown, ArrayList is_a, ArrayList relationship, ArrayList use_term, ArrayList xref) {
        Integer[] Integer_alt_id = new Integer[alt_id.size()];
        obj.alt_id = (Integer[]) alt_id.toArray(Integer_alt_id);
        String[] string_subset = new String[subset.size()];
        obj.subset = (String[]) subset.toArray(string_subset);
        Synonym[] synonym_synonym = new Synonym[synonym.size()];
        obj.synonym = (Synonym[]) synonym.toArray(synonym_synonym);
        Synonym[] synonym_related_synonym = new Synonym[related_synonym.size()];
        obj.related_synonym = (Synonym[]) related_synonym.toArray(synonym_related_synonym);
        Synonym[] synonym_exact_synonym = new Synonym[exact_synonym.size()];
        obj.exact_synonym = (Synonym[]) exact_synonym.toArray(synonym_exact_synonym);
        Synonym[] synonym_broad_synonym = new Synonym[broad_synonym.size()];
        obj.broad_synonym = (Synonym[]) broad_synonym.toArray(synonym_broad_synonym);
        Synonym[] synonym_narrow_synonym = new Synonym[narrow_synonym.size()];
        obj.narrow_synonym = (Synonym[]) narrow_synonym.toArray(synonym_narrow_synonym);
        Dbxref[] dbxref_xref_analog = new Dbxref[xref_analog.size()];
        obj.xref_analog = (Dbxref[]) xref_analog.toArray(dbxref_xref_analog);
        Dbxref[] dbxref_xref_unknown = new Dbxref[xref_unknown.size()];
        obj.xref_unknown = (Dbxref[]) xref_unknown.toArray(dbxref_xref_unknown);
        Integer[] Integer_is_a = new Integer[is_a.size()];
        obj.is_a = (Integer[]) is_a.toArray(Integer_is_a);
        Integer[] Integer_use_term = new Integer[use_term.size()];
        obj.use_term = (Integer[]) use_term.toArray(Integer_use_term);
        Relationship[] Relationship_relationship = new Relationship[relationship.size()];
        obj.relationship = (Relationship[]) relationship.toArray(Relationship_relationship);
        Dbxref[] dbxref_xref = new Dbxref[xref.size()];
        obj.xref = (Dbxref[]) xref.toArray(dbxref_xref);
        return obj;
    }

    /**
     * Loads an ontology from a specified OBO file. See <a href="http://www.geneontology.org/GO.format.shtml#oboflat">http://www.geneontology.org/GO.format.shtml#oboflat</a>
     * for a description of the OBO file format. The OBO for GO can be found online at <a href="http://www.geneontology.org">http://www.geneontology.org</a>.
     * Ontologies loaded by <code>loadOntology</code> can be retrieved through the <code>getOntology</code> function of <code>OBOES</code>.
     * <p>
     * When loading an ontology, you must specify the path to the OBO file representing the ontology, as well as a user-specified
     * name or handle to the ontology. Ontology names are arbitrary, but it is suggested that they bear
     * some semblance to the actual name of the ontology. You cannot specify the same ontology name to be loaded
     * twice for the same <code>OBOES</code> object. An ontology cannot be unloaded from an existing <code>OBOES</code> object after it is
     * loaded; instead, simply create a new <code>OBOES</code> object.
     *
     * The OBO file path can be a local file, or it can be an online file. If the file is online, the full URL must be used, including the
     * "http://" or "ftp://" prefix.
     *
     * @param OBOFile       the path to the OBO file to read in
     * @param ontology_name the user-specified name/handle to give to this ontology
     * @return              <code>true</code> if the specified OBO file was read in without problems. <code>false</code>
     *                      if a problem was encountered; an error message is also displayed.
     * @see Ontology
     */
    public boolean loadOntology(String OBOFile, String ontology_name) {
        if (this.ontologies.get(ontology_name) != null) {
            if (this.verbose) System.out.println("Error: an ontology by the name of '" + ontology_name + "' has already been loaded.");
            return false;
        }
        BufferedReader in;
        try {
            if (isURL(OBOFile)) {
                URL url = new URL(OBOFile);
                URLConnection uc = url.openConnection();
                in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            } else {
                in = new BufferedReader(new FileReader(OBOFile));
            }
        } catch (Exception e) {
            if (this.verbose) System.out.println("Error: the file at " + OBOFile + " could not be opened for reading.");
            return false;
        }
        Ontology ontology = new Ontology();
        ontology.name = ontology_name;
        ArrayList subsetdef = new ArrayList();
        OBO_Object obj = null;
        String currentObject = "";
        ArrayList alt_id = new ArrayList();
        ArrayList subset = new ArrayList();
        ArrayList synonym = new ArrayList();
        ArrayList related_synonym = new ArrayList();
        ArrayList exact_synonym = new ArrayList();
        ArrayList broad_synonym = new ArrayList();
        ArrayList narrow_synonym = new ArrayList();
        ArrayList xref_analog = new ArrayList();
        ArrayList xref_unknown = new ArrayList();
        ArrayList is_a = new ArrayList();
        ArrayList relationship = new ArrayList();
        ArrayList use_term = new ArrayList();
        ArrayList xref = new ArrayList();
        int lineNum = 0;
        String line = "";
        try {
            while (true) {
                try {
                    line = removeComment(in.readLine());
                } catch (Exception e) {
                    break;
                }
                lineNum++;
                if (line.length() == 0 || line.charAt(0) == '!') {
                    continue;
                } else if (line.matches("^\\[.*?\\]$")) {
                    if (line.equals("[Term]")) {
                        currentObject = "Term";
                    } else if (line.equals("[Typedef]")) {
                        currentObject = "Typedef";
                    } else {
                        throw new Exception("Error parsing OBO file: Unknown stanza header");
                    }
                    if (obj != null) {
                        obj = castToArrays(obj, alt_id, subset, synonym, related_synonym, exact_synonym, broad_synonym, narrow_synonym, xref_analog, xref_unknown, is_a, relationship, use_term, xref);
                        obj.information_content = -1;
                    }
                    obj = new OBO_Object();
                    obj.ontology_name = ontology_name;
                    alt_id = new ArrayList();
                    subset = new ArrayList();
                    synonym = new ArrayList();
                    related_synonym = new ArrayList();
                    exact_synonym = new ArrayList();
                    broad_synonym = new ArrayList();
                    narrow_synonym = new ArrayList();
                    xref_analog = new ArrayList();
                    xref_unknown = new ArrayList();
                    is_a = new ArrayList();
                    relationship = new ArrayList();
                    use_term = new ArrayList();
                    xref = new ArrayList();
                    continue;
                }
                while (line.charAt(line.length() - 1) == '\\') {
                    line = line.substring(0, line.length() - 1);
                    String nextLine = removeComment(in.readLine());
                    lineNum++;
                    if (nextLine.matches("[^\\\\]:")) {
                        throw new Exception("Error parsing OBO file: Unexpected end of line");
                    }
                    line += nextLine;
                }
                String tag, value;
                try {
                    tag = line.substring(0, line.indexOf(':')).trim();
                    value = line.substring(line.indexOf(':') + 1).trim();
                } catch (IndexOutOfBoundsException e) {
                    throw new Exception("Error parsing OBO file: cannot find key-terminating colon");
                }
                if (value == null || value.equals("")) {
                    throw new Exception("Error parsing OBO file: no value following tag");
                }
                if (tag.equals("format-version")) {
                    ontology.format_version = removeEscapedChars(value);
                } else if (tag.equals("typeref")) {
                    ontology.typeref = removeEscapedChars(value);
                } else if (tag.equals("version")) {
                    ontology.version = removeEscapedChars(value);
                } else if (tag.equals("date")) {
                    ontology.date = removeEscapedChars(value);
                } else if (tag.equals("saved-by")) {
                    ontology.saved_by = removeEscapedChars(value);
                } else if (tag.equals("auto-generated-by")) {
                    ontology.auto_generated_by = removeEscapedChars(value);
                } else if (tag.equals("default-namespace")) {
                    ontology.default_namespace = removeEscapedChars(value);
                } else if (tag.equals("remark")) {
                    ontology.remark = removeEscapedChars(value);
                } else if (tag.equals("subsetdef")) {
                    inspectQuotedString(value);
                    subsetdef.add(parse_subsetdef(value));
                } else if (tag.equals("id")) {
                    if (obj.id != null) {
                        if (currentObject.equals("Term")) {
                            obj = ontology.getTermByID(obj.integer_id.intValue());
                        } else {
                            obj = ontology.getTypedefByID(obj.id);
                        }
                        continue;
                    }
                    obj.id = removeEscapedChars(value);
                    if (currentObject.equals("Term")) {
                        obj.integer_id = parse_id(value);
                        ontology.terms.put(obj.integer_id, obj);
                    } else {
                        ontology.typedefs.put(obj.id, obj);
                    }
                } else if (tag.equals("alt_id")) {
                    Object alternate_id;
                    if (currentObject.equals("Term")) {
                        alternate_id = parse_id(value);
                        ontology.terms.put(alternate_id, obj);
                    } else {
                        alternate_id = removeEscapedChars(value);
                        ontology.typedefs.put(alternate_id, obj);
                    }
                    alt_id.add(alternate_id);
                } else if (tag.equals("name")) {
                    obj.name = removeEscapedChars(value);
                    if (currentObject.equals("Term")) {
                        ontology.TermNameToTerm.put(obj.name.toLowerCase(), obj);
                    }
                } else if (tag.equals("namespace")) {
                    obj.namespace = removeEscapedChars(value);
                } else if (tag.equals("def")) {
                    inspectQuotedString(value);
                    if (obj.def != null) {
                        throw new Exception("Error parsing OBO file: Multiple def tags detected");
                    }
                    obj.def = parse_def(value);
                } else if (tag.equals("comment")) {
                    if (obj.comment != null) {
                        throw new Exception("Error parsing OBO file: Multiple comment tags detected");
                    }
                    obj.comment = removeEscapedChars(value);
                } else if (tag.equals("subset")) {
                    subset.add(removeEscapedChars(value));
                } else if (tag.equals("synonym")) {
                    synonym.add(parse_synonym(value));
                } else if (tag.equals("related_synonym")) {
                    related_synonym.add(parse_synonym(value));
                } else if (tag.equals("exact_synonym")) {
                    exact_synonym.add(parse_synonym(value));
                } else if (tag.equals("broad_synonym")) {
                    broad_synonym.add(parse_synonym(value));
                } else if (tag.equals("narrow_synonym")) {
                    narrow_synonym.add(parse_synonym(value));
                } else if (tag.equals("xref")) {
                    xref.addAll(parse_dbxref(value));
                } else if (tag.equals("xref_analog")) {
                    xref_analog.addAll(parse_dbxref(value));
                } else if (tag.equals("xref_unknown")) {
                    xref_unknown.addAll(parse_dbxref(value));
                } else if (tag.equals("is_a")) {
                    is_a.add(parse_id(value));
                } else if (tag.equals("relationship")) {
                    relationship.add(parse_relationship(value));
                } else if (tag.equals("is_obsolete")) {
                    obj.is_obsolete = parse_boolean(value);
                } else if (tag.equals("use_term")) {
                    use_term.add(parse_id(value));
                } else if (tag.equals("domain")) {
                    obj.domain = parse_id(value);
                } else if (tag.equals("range")) {
                    obj.range = parse_id(value);
                } else if (tag.equals("is_transitive")) {
                    obj.is_transitive = parse_boolean(value);
                } else if (tag.equals("is_cyclic")) {
                    obj.is_cyclic = parse_boolean(value);
                } else if (tag.equals("is_symmetric")) {
                    obj.is_symmetric = parse_boolean(value);
                } else {
                    if (this.verbose) System.out.println("Line " + Integer.toString(lineNum) + ": Warning parsing OBO file: Unrecognized tag\n  --> contents of line: " + line);
                }
                if (currentObject.equals("Term") && obj.namespace == null && ontology.default_namespace != null) {
                    obj.namespace = ontology.default_namespace;
                }
            }
            obj = castToArrays(obj, alt_id, subset, synonym, related_synonym, exact_synonym, broad_synonym, narrow_synonym, xref_analog, xref_unknown, is_a, relationship, use_term, xref);
            Subsetdef[] subset_subsetdef = new Subsetdef[subsetdef.size()];
            ontology.subsetdef = (Subsetdef[]) subsetdef.toArray(subset_subsetdef);
            in.close();
        } catch (IOException e) {
            if (this.verbose) System.out.println("Error reading file");
            try {
                in.close();
            } catch (Exception e1) {
            }
            return false;
        } catch (Exception e) {
            if (this.verbose) System.out.println("Line " + Integer.toString(lineNum) + ": " + e.getMessage() + "\n  --> contents of line: " + line);
            try {
                in.close();
            } catch (Exception e1) {
            }
            return false;
        }
        HashSet allTerms = new HashSet();
        Iterator it = ontology.terms.values().iterator();
        while (it.hasNext()) {
            obj = (OBO_Object) it.next();
            allTerms.add(obj);
        }
        it = allTerms.iterator();
        while (it.hasNext()) {
            OBO_Object obj_orig = (OBO_Object) it.next();
            Integer[] parents = obj_orig.is_a;
            Integer child_id = obj_orig.integer_id;
            for (int i = 0; i < parents.length; i++) {
                try {
                    obj = (OBO_Object) ontology.terms.get(parents[i]);
                    if (obj.children == null) {
                        obj.children = new Integer[1];
                        obj.children[0] = child_id;
                    } else {
                        Integer[] updated = new Integer[obj.children.length + 1];
                        for (int j = 0; j < obj.children.length; j++) {
                            updated[j] = obj.children[j];
                        }
                        updated[updated.length - 1] = child_id;
                        obj.children = updated;
                    }
                } catch (Exception e) {
                    continue;
                }
            }
            Relationship[] relationships = obj_orig.relationship;
            if (relationships == null) {
                continue;
            }
            ArrayList part_of_parents = new ArrayList();
            for (int i = 0; i < relationships.length; i++) {
                if (relationships[i].relationship.equals("part_of")) {
                    part_of_parents.add(relationships[i].integer_id);
                }
            }
            for (int i = 0; i < part_of_parents.size(); i++) {
                try {
                    obj = (OBO_Object) ontology.terms.get(part_of_parents.get(i));
                    if (obj.children == null) {
                        obj.children = new Integer[1];
                        obj.children[0] = child_id;
                    } else {
                        Integer[] updated = new Integer[obj.children.length + 1];
                        for (int j = 0; j < obj.children.length; j++) {
                            updated[j] = obj.children[j];
                        }
                        updated[updated.length - 1] = child_id;
                        obj.children = updated;
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        }
        this.ontologies.put(ontology_name, ontology);
        return true;
    }

    private void inspectQuotedString(String value) throws Exception {
        if (!value.matches(".*?[^\\\\]?\".*")) {
            throw new Exception("Error parsing OBO file: Expected quoted string");
        } else if (!value.matches(".*?[^\\\\]?\".*?[^\\\\]\".*")) {
            throw new Exception("Error parsing OBO file: Unclosed quoted string");
        }
    }

    private String removeComment(String line) {
        return line.split("[^\\\\]!", 2)[0].trim();
    }

    private String removeEscapedChars(String value) {
        value = value.trim();
        value = value.replaceAll("\\\\n", "\n");
        value = value.replaceAll("\\\\t", "\t");
        value = value.replaceAll("\\\\W", " ");
        int index = value.indexOf('\\');
        while (index > -1 && index + 1 < value.length() && !Character.isWhitespace(value.charAt(index + 1))) {
            value = value.substring(0, index) + value.substring(index + 1);
            index = value.indexOf('\\');
        }
        return value;
    }

    private Subsetdef parse_subsetdef(String value) throws Exception {
        Pattern pattern = Pattern.compile("^(\\S+)\\s+\"(.*)\"$");
        Matcher matcher = pattern.matcher(value);
        if (matcher.matches()) {
            Subsetdef ret = new Subsetdef();
            ret.name = removeEscapedChars(matcher.group(1));
            ret.def = removeEscapedChars(matcher.group(2));
            return ret;
        } else {
            throw new Exception("Error parsing OBO file: Malformed subsetdef value");
        }
    }

    private Integer parse_id(String value) {
        String[] components = value.split("[^\\\\]:");
        return Integer.valueOf(components[components.length - 1]);
    }

    private Object[] parse_quotedtext_dbxreflist(String value) throws Exception {
        Object[] components = new Object[2];
        Pattern pattern = Pattern.compile("(.*)[^\\\\]\\[(.*)\\]$");
        Matcher matcher = pattern.matcher(value);
        String text;
        if (matcher.matches()) {
            text = matcher.group(1);
            components[1] = parse_dbxref(matcher.group(2).trim());
        } else if (Pattern.compile("(.*)[^([^\\\\]\\[)([^\\\\]\\])]$").matcher(value).matches()) {
            text = value;
        } else {
            throw new Exception("Error parsing OBO file: Malformed or unclosed dbxref list");
        }
        pattern = Pattern.compile("^\"?(.*?)\"?$");
        matcher = pattern.matcher(text.trim());
        if (matcher.matches()) {
            components[0] = removeEscapedChars(matcher.group(1));
        }
        return components;
    }

    private Dbxref[] castDbxrefListToArray(ArrayList dbxrefs) {
        if (dbxrefs == null) {
            return null;
        } else {
            Dbxref[] dbxref_dbxrefs = new Dbxref[dbxrefs.size()];
            return (Dbxref[]) dbxrefs.toArray(dbxref_dbxrefs);
        }
    }

    private Def parse_def(String value) throws Exception {
        Def def = new Def();
        Object[] components = parse_quotedtext_dbxreflist(value);
        def.def = (String) components[0];
        def.dbxref = castDbxrefListToArray((ArrayList) components[1]);
        return def;
    }

    private Synonym parse_synonym(String value) throws Exception {
        Synonym synonym = new Synonym();
        Object[] components = parse_quotedtext_dbxreflist(value);
        synonym.synonym = (String) components[0];
        synonym.dbxref = castDbxrefListToArray((ArrayList) components[1]);
        return synonym;
    }

    private ArrayList split_dbxref_list(String value) {
        ArrayList dbxrefs = new ArrayList();
        int start = value.indexOf('[') + 1;
        for (int i = start + 1; i < value.length(); i++) {
            if (value.charAt(i) == ',' && value.charAt(i - 1) != '\\') {
                dbxrefs.add(value.substring(start, i).trim());
                start = i + 1;
            }
        }
        if (value.charAt(value.length() - 1) == ']') {
            dbxrefs.add(value.substring(start, value.length() - 1).trim());
        } else {
            dbxrefs.add(value.substring(start, value.length()).trim());
        }
        return dbxrefs;
    }

    private ArrayList parse_dbxref(String value) throws Exception {
        if (value == null || value.length() == 0 || value.equals("[]")) {
            return null;
        }
        ArrayList dbxrefs = split_dbxref_list(value);
        if (dbxrefs == null) {
            return null;
        }
        for (int i = 0; i < dbxrefs.size(); i++) {
            Pattern pattern = Pattern.compile("^(\\S+)\\s*\"?(.*?)\"?$");
            Matcher matcher = pattern.matcher((String) dbxrefs.get(i));
            if (matcher.matches()) {
                Dbxref dbxref = new Dbxref();
                dbxref.name = removeEscapedChars(matcher.group(1).trim());
                if (matcher.group(2).length() > 0) {
                    dbxref.description = removeEscapedChars(matcher.group(2).trim());
                }
                dbxrefs.set(i, dbxref);
            }
        }
        return dbxrefs;
    }

    private Relationship parse_relationship(String value) {
        String[] components = value.split("\\s+");
        Relationship rel = new Relationship();
        rel.relationship = components[0];
        rel.integer_id = parse_id(components[1].trim());
        return rel;
    }

    private boolean parse_boolean(String value) {
        if (value.equals("true")) return true; else return false;
    }

    /**
     * Returns the specified <code>Ontology</code> object given the <code>String</code> handle used as input when the ontology was loaded using <code>loadOntology</code>.
     *
     * @param ontology_name     the name of the ontology
     * @return                  <code>Ontology</code> object corresponding to the specified ontology
     * @see Ontology
     */
    public Ontology getOntology(String ontology_name) throws Exception {
        Ontology ontology = (Ontology) this.ontologies.get(ontology_name);
        if (ontology == null) {
            throw new Exception("Error: the ontology '" + ontology_name + "' is invalid; most likely reason: it has not been loaded");
        }
        return ontology;
    }

    /**
     * Unloads a loaded ontology.
     *
     * @param ontology_name     the name of the ontology to unload
     * @see Ontology
     */
    public void unloadOntology(String ontology_name) {
        this.ontologies.remove(ontology_name);
    }

    /**
     * Loads background annotation for the specified ontology. Without annotation, an ontology is relatively useless.
     * <code>loadBackgroundAnnotation</code> reads in a file that contains accessions and the ontology Terms associated
     * with each "background" accession. It then creates background-accession-to-term and term-to-background-accession mappings for the
     * specified ontology.
     * <p>
     * An "accession" is a generic identifier; for example, LocusLink IDs, Gene Identifiers (GIs), and Uniprot IDs are all types
     * of accessions for genes. A hypothetical ontology for cars could be used with car accessions derived from a car's make
     * and year. The OBOES framework cares about neither the format of the accession, since all accessions are stored as
     * <code>String</code>s, nor the underlying meaning of the accession. Ontology Terms are said to describe, or "annotate,"
     * certain accessions. An accession may thus be annotated by zero, one, or multiple Terms from an ontology.
     * <p>
     * "Background" annotation refers to the assumed composition of the set of accessions from which the set of accessions loaded through <code>loadAnnotation</code> are derived.
     * If background annotation is not loaded, then the annotation loaded through <code>loadAnnotation</code> is assumed to represent the background distribution of ontology Terms.
     * For instance: "background" accessions may refer to all the genes in an organism. <code>loadBackgroundAnnotation</code> allows the unique specification and customization
     * of the background distribution. If background annotation is loaded, then the total number of accessions in the background annotation
     * determines the total sample space size, unless the user manually overrides the sample space size.
     *
     * The annotation format that <code>loadBackgroundAnnotation</code> and <code>loadAnnotation</code> expect is as follows:
     * <p>
     * <code>
     * accession1   term_integer_id1 term_integer_id2 term_integer_id3 ... <br>
     * accession2   term_integer_id1 term_integer_id2 term_integer_id3 ... <br>
     * ...
     * </code>
     * <p>
     * In words: each line annotates a single accession, and the accession is the first element of the line. The accession
     * is followed by any whitespace; a tab is suggested. Next comes a whitespace-delimited list of integer IDs for Terms
     * that are associated with the accession. Note: the integer IDs for the annotation of an accession can be spread out over
     * multiple lines if the accession is specified again at the beginning of each line containing annotation for that accession.
     * <p>
     * When loading annotation, it is necessary to specify the name/handle of the ontology to which to map the accessions
     * and terms in the specified file. The name/handle of the ontology was specified when the ontology was loaded using
     * <code>loadOntology</code>. If annotation for a single ontology is spread out over multiple files, <code>loadBackgroundAnnotation</code>
     * can be called repeatedly, specifying a different file each time but the same ontology name.
     *
     * @param AnnotationFile    the path to the file containing annotations
     * @param ontology_name     the name of the ontology for which to create accession-to-term and term-to-accession mappings
     * @return                  <code>true</code> if the annotation was read in successfully; <code>false</code> otherwise.
     * @see                     loadAnnotation
     * @see                     Ontology
     */
    public boolean loadBackgroundAnnotation(String AnnotationFile, String ontology_name) throws Exception {
        Ontology ontology = getOntology(ontology_name);
        return loadAnnotationHelper(AnnotationFile, ontology, ontology.TermToBackgroundAccessions, ontology.BackgroundAccessionToTerms);
    }

    /**
     * Loads annotation for the specified ontology. Without annotation, an ontology is relatively useless.
     * <code>loadAnnotation</code> reads in a file that contains accessions and the ontology Terms associated
     * with each accession. It then creates accession-to-term and term-to-background-accession mappings for the
     * specified ontology. These mappings can be retrieved using <code>Ontology</code> functions such as
     * <code>getAccessionsForTerm</code> and <code>getTermsForAccession</code>.
     * <p>
     * An "accession" is a generic identifier; for example, LocusLink IDs, Gene Identifiers (GIs), and Uniprot IDs are all types
     * of accessions for genes. A hypothetical ontology for cars could be used with car accessions derived from a car's make
     * and year. The OBOES framework cares about neither the format of the accession, since all accessions are stored as
     * <code>String</code>s, nor the underlying meaning of the accession. Ontology Terms are said to describe, or "annotate,"
     * certain accessions. An accession may thus be annotated by zero, one, or multiple Terms from an ontology.
     * <p>
     * The annotations loaded through <code>loadAnnotation</code> are, by default, considered to be as complete as possible. That is, the annotation will
     * be assumed to represent the background distribution of ontology Terms. In addition, the annotation will be used to identify Terms associated with
     * input accessions for use with the <code>getSimpleEnrichment</code> and <code>getCompoundEnrichment</code> functions. The background distribution
     * can be separately loaded and specified through the <code>loadBackgroundAnnotation</code> function.
     *
     * The annotation format that <code>loadAnnotation</code> (and <code>loadBackgroundAnnotation</code>) expect is as follows:
     * <p>
     * <code>
     * accession1   term_integer_id1 term_integer_id2 term_integer_id3 ... <br>
     * accession2   term_integer_id1 term_integer_id2 term_integer_id3 ... <br>
     * ...
     * </code>
     * <p>
     * In words: each line annotates a single accession, and the accession is the first element of the line. The accession
     * is followed by any whitespace; a tab is suggested. Next comes a whitespace-delimited list of integer IDs for Terms
     * that are associated with the accession. Note: the integer IDs for the annotation of an accession can be spread out over
     * multiple lines if the accession is specified again at the beginning of each line containing annotation for that accession.
     * <p>
     * When loading annotation, it is necessary to specify the name/handle of the ontology to which to map the accessions
     * and terms in the specified file. The name/handle of the ontology was specified when the ontology was loaded using
     * <code>loadOntology</code>. If annotation for a single ontology is spread out over multiple files, <code>loadAnnotation</code>
     * can be called repeatedly, specifying a different file each time but the same ontology name.
     *
     * The annotation file path can be a local file, or it can be an online file. If the file is online, the full URL must be used, including the
     * "http://" or "ftp://" prefix.
     *
     * @param AnnotationFile    the path to the file containing annotations
     * @param ontology_name     the name of the ontology for which to create accession-to-term and term-to-accession mappings
     * @return                  <code>true</code> if the annotation was read in successfully; <code>false</code> otherwise.
     * @see                     loadBackgroundAnnotation
     * @see                     Ontology
     */
    public boolean loadAnnotation(String AnnotationFile, String ontology_name) throws Exception {
        Ontology ontology = getOntology(ontology_name);
        return loadAnnotationHelper(AnnotationFile, ontology, ontology.TermToAccessions, ontology.AccessionToTerms);
    }

    /**
     * For an ontology with annotation loaded in by <code>loadAnnotation</code>, adds in annotation of ancestor nodes as appropriate to complete the annotation.
     * For example, if an accession is annotated by Term B but not Term A, and Term A is a parent (or higher-order ancestor) of Term B, then this function adds
     * Term A to the accession's associated annotation.
     *
     * @param ontology_name     the name/handle of the ontology for which to create accession-to-term and term-to-accession mappings
     * @see                     loadAnnotation
     */
    public void addAncestorAnnotation(String ontology_name) throws Exception {
        Ontology ontology = getOntology(ontology_name);
        String[] accessions = ontology.getAllAccessions();
        for (int i = 0; i < accessions.length; i++) {
            HashSet hs = (HashSet) ontology.AccessionToTerms.get(accessions[i]);
            if (hs == null || hs.size() == 0) continue;
            HashSet newhs = new HashSet(hs);
            for (Iterator it = hs.iterator(); it.hasNext(); ) {
                OBO_Object term = (OBO_Object) it.next();
                newhs.addAll(ontology.getHashSetAllAncestors(term.integer_id.intValue()));
            }
            ontology.AccessionToTerms.put(accessions[i], newhs);
            for (Iterator it = newhs.iterator(); it.hasNext(); ) {
                OBO_Object term = (OBO_Object) it.next();
                HashSet hs2 = (HashSet) ontology.TermToAccessions.get(term.integer_id);
                if (hs2 == null) {
                    hs2 = new HashSet();
                    ontology.TermToAccessions.put(term.integer_id, hs2);
                }
                hs2.add(accessions[i]);
            }
        }
    }

    private boolean loadAnnotationHelper(String AnnotationFile, Ontology ontology, Hashtable TermToAccessions_HT, Hashtable AccessionToTerms_HT) {
        BufferedReader in;
        try {
            if (isURL(AnnotationFile)) {
                URL url = new URL(AnnotationFile);
                URLConnection uc = url.openConnection();
                in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            } else {
                in = new BufferedReader(new FileReader(AnnotationFile));
            }
        } catch (Exception e) {
            if (this.verbose) System.out.println("Error: the file at " + AnnotationFile + " could not be opened for reading.");
            return false;
        }
        int lineNum = 1;
        try {
            while (true) {
                String line;
                try {
                    line = in.readLine().trim();
                } catch (Exception e) {
                    break;
                }
                lineNum++;
                String[] components = line.split("\\s");
                String accession = components[0].trim();
                int startI = 1;
                if (line.charAt(0) == '\"') {
                    int terminatingIndex = line.indexOf("\"", 1);
                    accession = line.substring(1, terminatingIndex);
                    components = line.substring(terminatingIndex + 1).trim().split("\\s");
                    startI = 0;
                }
                HashSet terms = new HashSet(components.length - 1);
                for (int i = startI; i < components.length; i++) {
                    OBO_Object obj = (OBO_Object) ontology.terms.get(Integer.valueOf(components[i]));
                    if (obj == null) {
                        if (this.verbose) System.out.println("Line " + Integer.toString(lineNum) + ": Warning: Unrecognized ontology ID " + components[i] + " found in annotation");
                        continue;
                    }
                    while (obj.is_obsolete) {
                        if (obj.use_term != null && obj.use_term.length > 0) {
                            obj = (OBO_Object) ontology.terms.get(obj.use_term[0]);
                        } else {
                            break;
                        }
                    }
                    terms.add(obj);
                    HashSet AccessionsForTerm = (HashSet) TermToAccessions_HT.get(obj.integer_id);
                    if (AccessionsForTerm == null) {
                        AccessionsForTerm = new HashSet();
                        TermToAccessions_HT.put(obj.integer_id, AccessionsForTerm);
                    }
                    AccessionsForTerm.add(accession);
                }
                HashSet existingTerms = (HashSet) AccessionToTerms_HT.get(accession);
                if (existingTerms != null) {
                    existingTerms.addAll(terms);
                } else {
                    AccessionToTerms_HT.put(accession, terms);
                }
            }
            in.close();
        } catch (IOException e) {
            if (this.verbose) System.out.println("Error reading file");
            return false;
        }
        return true;
    }

    /**
     * Finds enriched ontology Terms in a given input sample of accessions. Prior to using <code>getSimpleEnrichment</code>,
     * the ontology must first be loaded (<code>loadOntology</code>), then its annotation data loaded (<code>loadAnnotation</code>).
     * <p>
     * <code>getSimpleEnrichment</code> takes in a list of accessions (as <code>String</code>s) and the name of the
     * ontology in which to work. The name of the ontology was specified by the user when it was loaded using
     * <code>loadOntology</code>. <code>getSimpleEnrichment</code> evaluates all ontology Terms that annotate the
     * given input sample, and identifies ontology Terms that are independently enriched. A Term is said to be enriched
     * if the number of accessions in the input sample associated with the Term is significantly higher than predicted
     * by random chance. A <code>SimpleEnrichment</code> object is created for each Term found to be enriched (see
     * {@link SimpleEnrichment}, {@link Enrichment} to view the data recorded in these objects), then they are
     * compiled together into an array and returned.
     * <p>
     * In order to specify what is meant by "significantly higher than random chance," <code>OBOES</code> maintains a "minimum count" and "maximum p-value" as cut-offs to decide when a Term is
     * significantly enriched or not. In order for a Term to be significantly enriched, the number of accessions
     * annotated with that Term must be at least the "minimum count." In addition, the
     * statistically computed p-value must be at most the "maximum p-value." The "minimum count" defaults to 2; the
     * "maximum p-value" defaults to 0.01, which is usually a standard choice to balance between Type I and Type II errors.
     * The "minimum count" and "maximum p-value" can be manually set and retrieved using <code>OBOES</code> methods. Any
     * invocation of <code>getSimpleEnrichment</code> and <code>getCompoundEnrichment</code> will only consider an
     * enrichment to be significant if it satisfies the "maximum p-value" and "minimum count" values that are currently
     * set in the <code>OBOES</code> object.
     * <p>
     * To compute p-values, <code>OBOES</code> uses Fisher's exact test to compute the exact p-value. In other words,
     * the reported p-value (prior to correction, described next) is the probability of observing a Term at its level
     * of representation in the input sample or higher. The probabilities follow a hypergeometric distribution.
     * <p>
     * <code>OBOES</code> provides additional methods that influence the statistics involved in searching for enrichment.
     * For example, <code>getSimpleEnrichment</code> defaults the total sample space size to the total number of unique accessions
     * loaded through <code>loadAnnotation</code> for a specified ontology, unless <code>loadBackgroundAnnotation</code> is
     * invoked. The total sample space is understood to refer to the entire set
     * of possible accessions from which the accessions in a given input sample are selected. For example, the total
     * sample space for a microarray experiment would be the whole genome of the organism, and the sample space size
     * is then equal to the number of genes for the organism. <code>OBOES</code> also provides a variety of
     * <em>multiple hypothesis correction</em> schemes, which are ways to keep the overall likelihood of encountering
     * a false positive low, in the face of testing many hypotheses. Multiple hypothesis correction is automatically
     * employed by <code>getSimpleEnrichment</code>, since it tests all possible enriched Terms in a single invocation.
     * The default correction scheme is the Bonferroni method, which multiplies the p-value of each p-value obtained
     * by the number of tests.
     *
     * @param accessions    the list of accessions comprising the input sample
     * @param ontology_name the ontology to use for finding enrichment in the input sample (specified from <code>loadOntology</code>)
     * @return              an array of <code>SimpleEnrichment</code> objects, each one denoting a Term that was found to be
     *                      significantly enriched
     * @see Enrichment
     * @see SimpleEnrichment
     * @see OBO_Object
     */
    public SimpleEnrichment[] getSimpleEnrichment(String[] accessions, String ontology_name) throws Exception {
        Ontology ontology = getOntology(ontology_name);
        ArrayList enrichment = new ArrayList();
        HashSet myAccessions = new HashSet();
        for (int i = 0; i < accessions.length; i++) {
            myAccessions.add(accessions[i]);
        }
        HashSet processedTerms = new HashSet();
        int numTests = 0;
        Hashtable TermToBackgroundAccessions_HT = ontology.TermToAccessions;
        int totalCount;
        if (this.sample_space_size > 0) {
            totalCount = this.sample_space_size;
        } else if (ontology.BackgroundAccessionToTerms.size() == 0) {
            totalCount = ontology.getHashSetAllAccessions().size();
        } else {
            TermToBackgroundAccessions_HT = ontology.TermToBackgroundAccessions;
            totalCount = ontology.BackgroundAccessionToTerms.size();
        }
        for (Iterator it_acc = myAccessions.iterator(); it_acc.hasNext(); ) {
            String accession = ((String) it_acc.next());
            HashSet myTerms = ontology.getHashSetTermsForAccession(accession);
            if (myTerms == null) {
                continue;
            }
            for (Iterator it_terms = myTerms.iterator(); it_terms.hasNext(); ) {
                OBO_Object nextTerm = (OBO_Object) it_terms.next();
                if (processedTerms.contains(nextTerm)) {
                    continue;
                }
                processedTerms.add(nextTerm);
                HashSet backgroundAccessionsWithThisTerm = (HashSet) TermToBackgroundAccessions_HT.get(nextTerm.integer_id);
                if (backgroundAccessionsWithThisTerm == null) {
                    backgroundAccessionsWithThisTerm = new HashSet(0);
                }
                HashSet myAccessionsWithThisTerm = new HashSet(myAccessions);
                HashSet intermed = new HashSet(backgroundAccessionsWithThisTerm);
                intermed.addAll(ontology.getHashSetAccessionsForTerm(nextTerm));
                myAccessionsWithThisTerm.retainAll(intermed);
                int count = myAccessionsWithThisTerm.size();
                if (count < this.min_count) {
                    continue;
                }
                numTests++;
                double pval = computePval(myAccessions.size(), count, totalCount, backgroundAccessionsWithThisTerm.size());
                if (pval > this.max_pval) {
                    continue;
                }
                String[] s = new String[myAccessionsWithThisTerm.size()];
                enrichment.add(new SimpleEnrichment(nextTerm, (String[]) myAccessionsWithThisTerm.toArray(s), pval, count, (double) count / (double) myAccessions.size() * 100.0));
            }
        }
        if (enrichment.size() < 1) {
            return null;
        }
        Enrichment[] enr = applyMultipleHypothesisCorrection(arrayListToEnrichment(enrichment), numTests, this.getCorrectionMethod());
        SimpleEnrichment[] simple_enr = new SimpleEnrichment[enr.length];
        System.arraycopy(enr, 0, simple_enr, 0, enr.length);
        return simple_enr;
    }

    /**
     * Finds under-enriched (i.e., underrepresented) ontology Terms in a given input sample of accessions. Prior to use,
     * the ontology must first be loaded (<code>loadOntology</code>), then its annotation data loaded (<code>loadAnnotation</code>).
     * <p>
     * <code>getUnderRepresentedTerms</code> takes in a list of accessions (as <code>String</code>s) and the name of the
     * ontology in which to work. It functions similarly to <code>getSimpleEnrichment</code>, except that it considers all possible ontology Terms to
     * determine those Terms that are <b>underrepresented</b> in the sample. When using <code>getUnderRepresentedTerms</code>, the "minimum count"
     * (as set through <code>getMinimumCount</code>) does not apply; however, the "maximum p-value" (as set through <code>getMaximumPvalue</code>)
     * still applies. Because all ontology Terms are tested, this function may take some time to run.
     *
     * Because all possible ontology Terms are tested, it is recommended that a less conservative multiple hypothesis correction scheme is used
     * when using <code>getUnderRepresentedTerms</code>, or that no multiple hypothesis correction be used at all (at least for preliminary purposes).
     *
     * @param accessions    the list of accessions comprising the input sample
     * @param ontology_name the ontology to use for finding underrepsented Terms in the input sample (specified from <code>loadOntology</code>)
     * @return              an array of <code>SimpleEnrichment</code> objects, each one denoting a Term that was found to be
     *                      significantly under-enriched (underrepresented)
     * @see Enrichment
     * @see SimpleEnrichment
     * @see OBO_Object
     */
    public SimpleEnrichment[] getUnderRepresentedTerms(String[] accessions, String ontology_name) throws Exception {
        Ontology ontology = getOntology(ontology_name);
        ArrayList enrichment = new ArrayList();
        HashSet myAccessions = new HashSet();
        for (int i = 0; i < accessions.length; i++) {
            myAccessions.add(accessions[i]);
        }
        int numTests = 0;
        Hashtable TermToBackgroundAccessions_HT = ontology.TermToAccessions;
        int totalCount;
        if (this.sample_space_size > 0) {
            totalCount = this.sample_space_size;
        } else if (ontology.BackgroundAccessionToTerms.size() == 0) {
            totalCount = ontology.getHashSetAllAccessions().size();
        } else {
            TermToBackgroundAccessions_HT = ontology.TermToBackgroundAccessions;
            totalCount = ontology.BackgroundAccessionToTerms.size();
        }
        HashSet allTerms = ontology.getHashSetAllTerms();
        Iterator it = allTerms.iterator();
        while (it.hasNext()) {
            OBO_Object obj = (OBO_Object) it.next();
            HashSet backgroundAccessionsWithThisTerm = (HashSet) TermToBackgroundAccessions_HT.get(obj.integer_id);
            if (backgroundAccessionsWithThisTerm == null || backgroundAccessionsWithThisTerm.size() == 0) {
                continue;
            }
            numTests++;
            HashSet myAccessionsWithThisTerm = new HashSet(myAccessions);
            HashSet intermed = new HashSet(backgroundAccessionsWithThisTerm);
            intermed.addAll(ontology.getHashSetAccessionsForTerm(obj));
            myAccessionsWithThisTerm.retainAll(intermed);
            int count = myAccessionsWithThisTerm.size();
            double pval = computeUnderRepresentedPval(myAccessions.size(), count, totalCount, backgroundAccessionsWithThisTerm.size());
            if (pval > this.max_pval) {
                continue;
            }
            String[] s = new String[myAccessionsWithThisTerm.size()];
            enrichment.add(new SimpleEnrichment(obj, (String[]) myAccessionsWithThisTerm.toArray(s), pval, count, (double) count / (double) myAccessions.size() * 100.0));
        }
        if (enrichment.size() < 1) {
            return null;
        }
        Enrichment[] enr = applyMultipleHypothesisCorrection(arrayListToEnrichment(enrichment), numTests, this.getCorrectionMethod());
        SimpleEnrichment[] simple_enr = new SimpleEnrichment[enr.length];
        System.arraycopy(enr, 0, simple_enr, 0, enr.length);
        return simple_enr;
    }

    private ArrayList sortEnrichmentByPval(ArrayList enrichment) {
        for (int start = 0; start < enrichment.size(); start++) {
            int indexOfSmallest = start;
            for (int i = start + 1; i < enrichment.size(); i++) {
                if (((Enrichment) enrichment.get(i)).p_value < ((Enrichment) enrichment.get(indexOfSmallest)).p_value) {
                    indexOfSmallest = i;
                }
            }
            if (indexOfSmallest == start) {
                continue;
            }
            Enrichment smallest = (Enrichment) enrichment.get(indexOfSmallest);
            Enrichment old = (Enrichment) enrichment.get(start);
            enrichment.remove(start);
            enrichment.remove(indexOfSmallest - 1);
            enrichment.add(start, smallest);
            enrichment.add(old);
        }
        return enrichment;
    }

    private ArrayList sortEnrichmentByPercentCoverage(ArrayList enrichment) {
        for (int start = 0; start < enrichment.size(); start++) {
            int indexOfSmallest = start;
            for (int i = start + 1; i < enrichment.size(); i++) {
                if (((Enrichment) enrichment.get(i)).percentage > ((Enrichment) enrichment.get(indexOfSmallest)).percentage) {
                    indexOfSmallest = i;
                }
            }
            if (indexOfSmallest == start) {
                continue;
            }
            Enrichment smallest = (Enrichment) enrichment.get(indexOfSmallest);
            Enrichment old = (Enrichment) enrichment.get(start);
            enrichment.remove(start);
            enrichment.remove(indexOfSmallest - 1);
            enrichment.add(start, smallest);
            enrichment.add(old);
        }
        return enrichment;
    }

    /**
     * Applies multiple hypothesis correction to an array of enrichments. Multiple hypothesis correction keeps the overall likelihood of encountering
     * a false positive low, in the face of testing many hypotheses. Multiple hypothesis correction is automatically
     * employed by <code>getSimpleEnrichment</code>, since it tests all possible enriched Terms in a single invocation.
     * They are not employed by <code>getCompoundEnrichment</code>, since it tests only one hypothesis in an invocation.
     * For this reason, <code>applyMultipleHypothesisCorrection</code> serves as an interface and useful method to allow
     * you to apply multiple hypothesis correction to your results, particularly from <code>getCompoundEnrichment</code>, at your own discretion.
     * For example, you may choose to run 20 invocations of <code>getCompoundEnrichment</code> to test for compound
     * enrichment; then, your number of tests would be 20, and you can easily apply multiple hypothesis correction
     * to your results by collecting your 20 <code>CompoundEnrichment</code> objects into a 20-element array and
     * passing it to <code>applyMultipleHypothesisCorrection</code> along with 20 as the number of tests as well as
     * specifying your desired correction scheme. <code>applyMultipleHypothesisCorrection</code> removes enrichment
     * objects from the input array whose adjusted p-values are greater than the "maximum p-value" set in the <code>OBOES</code>
     * object; all other enrichment objects are returned with their <code>p_value</code> fields adjusted according
     * to the specified correction scheme.
     * <p>
     * <code>OBOES</code> offers 5 multiple hypothesis correction schemes: Bonferroni, Sidak, Holm, Hochberg,
     * and Benjamini-Hochberg. For more information about these correction schemes, consult the OBOES publication.
     *
     * @param enrichment_array               array of <code>SimpleEnrichment</code> or <code>CompoundEnrichment</code> objects, whose p-values are to be corrected
     * @param numTests          the total number of tests assumed when performing the correction
     * @param correction_method the correction method to perform; valid values are: "bonferroni", "sidak", "holm", "hochberg", "benjamini_hochberg", "none".
     * @return                  new enrichment objects with their p-values adjusted, and with objects whose p-values were greater than
     *                          the "maximum p-value" removed; null is returned if an invalid correction method is specified
     * @see SimpleEnrichment
     * @see CompoundEnrichment
     * @see Enrichment
     */
    public Enrichment[] applyMultipleHypothesisCorrection(Enrichment[] enrichment_array, int numTests, String correction_method) {
        Enrichment[] enr = new Enrichment[enrichment_array.length];
        for (int i = 0; i < enrichment_array.length; i++) {
            try {
                CompoundEnrichment ce = (CompoundEnrichment) enrichment_array[i];
                enr[i] = new CompoundEnrichment(ce.terms, ce.accessions, ce.p_value, ce.count, ce.percentage);
            } catch (Exception e) {
                SimpleEnrichment se = (SimpleEnrichment) enrichment_array[i];
                enr[i] = new SimpleEnrichment(se.term, se.accessions, se.p_value, se.count, se.percentage);
            }
        }
        ArrayList enrichment = enrichmentToArrayList(enr);
        enrichment = sortEnrichmentByPercentCoverage(enrichment);
        if (correction_method.equals("bonferroni")) {
            return arrayListToEnrichment(bonferroni_correction(enrichment, numTests));
        } else if (correction_method.equals("sidak")) {
            return arrayListToEnrichment(sidak_correction(enrichment, numTests));
        } else if (correction_method.equals("holm")) {
            return arrayListToEnrichment(holm_correction(enrichment, numTests));
        } else if (correction_method.equals("hochberg")) {
            return arrayListToEnrichment(hochberg_correction(enrichment, numTests));
        } else if (correction_method.equals("benjamini_hochberg")) {
            return arrayListToEnrichment(benjamini_hochberg_correction(enrichment, numTests));
        } else if (correction_method.equals("none")) {
            return arrayListToEnrichment(enrichment);
        }
        System.out.println("Error: Unrecognized correction method. Returning null.");
        return null;
    }

    private ArrayList enrichmentToArrayList(Enrichment[] enr) {
        ArrayList enrichment = new ArrayList();
        for (int i = 0; i < enr.length; i++) {
            enrichment.add(enr[i]);
        }
        return enrichment;
    }

    private Enrichment[] arrayListToEnrichment(ArrayList enrichment) {
        Enrichment[] enr = new Enrichment[enrichment.size()];
        return (Enrichment[]) enrichment.toArray(enr);
    }

    private ArrayList bonferroni_correction(ArrayList enrichment, int numTests) {
        for (int i = enrichment.size() - 1; i >= 0; i--) {
            Enrichment enr = (Enrichment) enrichment.get(i);
            enr.p_value = enr.p_value * numTests;
            if (enr.p_value > this.max_pval) {
                enrichment.remove(i);
            }
        }
        return enrichment;
    }

    private ArrayList sidak_correction(ArrayList enrichment, int numTests) {
        for (int i = enrichment.size() - 1; i >= 0; i--) {
            Enrichment enr = (Enrichment) enrichment.get(i);
            enr.p_value = (1 - Math.pow(1 - enr.p_value, numTests));
            if (enr.p_value > this.max_pval) {
                enrichment.remove(i);
            }
        }
        return enrichment;
    }

    private ArrayList holm_correction(ArrayList enrichment, int numTests) {
        Enrichment enr = (Enrichment) enrichment.get(0);
        enr.p_value = enr.p_value * numTests;
        for (int i = 1; i < enrichment.size(); i++) {
            enr = (Enrichment) enrichment.get(i);
            Enrichment enrPrev = (Enrichment) enrichment.get(i - 1);
            enr.p_value = Math.max(enrPrev.p_value, (numTests - i) * enr.p_value);
        }
        for (int i = enrichment.size() - 1; i >= 0; i--) {
            enr = (Enrichment) enrichment.get(i);
            if (enr.p_value > this.max_pval) {
                enrichment.remove(i);
            }
        }
        return enrichment;
    }

    private ArrayList hochberg_correction(ArrayList enrichment, int numTests) {
        for (int i = enrichment.size() - 2; i >= 0; i--) {
            Enrichment enr = (Enrichment) enrichment.get(i);
            Enrichment enrPrev = (Enrichment) enrichment.get(i + 1);
            enr.p_value = Math.min(enrPrev.p_value, (numTests - i) * enr.p_value);
        }
        for (int i = enrichment.size() - 1; i >= 0; i--) {
            Enrichment enr = (Enrichment) enrichment.get(i);
            if (enr.p_value > this.max_pval) {
                enrichment.remove(i);
            }
        }
        return enrichment;
    }

    private ArrayList benjamini_hochberg_correction(ArrayList enrichment, int numTests) {
        for (int i = enrichment.size() - 2; i >= 0; i--) {
            Enrichment enr = (Enrichment) enrichment.get(i);
            Enrichment enrPrev = (Enrichment) enrichment.get(i + 1);
            enr.p_value = Math.min(enrPrev.p_value, ((double) numTests / (i + 1)) * enr.p_value);
        }
        for (int i = enrichment.size() - 1; i >= 0; i--) {
            Enrichment enr = (Enrichment) enrichment.get(i);
            if (enr.p_value > this.max_pval) {
                enrichment.remove(i);
            }
        }
        return enrichment;
    }

    public double computePval(int totalChosen, int numChosenWithTerm, int total, int numTotalWithTerm) {
        double sum = 0;
        double logNumer = logFactorial[numTotalWithTerm] + logFactorial[total - numTotalWithTerm] + logFactorial[totalChosen] + logFactorial[total - totalChosen];
        double logTotal = logFactorial[total];
        int upperLimit = totalChosen;
        if (numTotalWithTerm < totalChosen) {
            upperLimit = numTotalWithTerm;
        }
        for (int i = numChosenWithTerm; i <= upperLimit; i++) {
            double logPval = logNumer - (logTotal + logFactorial[i] + logFactorial[totalChosen - i] + logFactorial[numTotalWithTerm - i] + logFactorial[total - numTotalWithTerm - totalChosen + i]);
            double pVal = Math.pow(Math.E, logPval);
            sum += pVal;
        }
        return sum;
    }

    private double computeUnderRepresentedPval(int totalChosen, int numChosenWithTerm, int total, int numTotalWithTerm) {
        double sum = 0;
        double logNumer = logFactorial[numTotalWithTerm] + logFactorial[total - numTotalWithTerm] + logFactorial[totalChosen] + logFactorial[total - totalChosen];
        double logTotal = logFactorial[total];
        for (int i = numChosenWithTerm; i >= 0; i--) {
            double logPval = logNumer - (logTotal + logFactorial[i] + logFactorial[totalChosen - i] + logFactorial[numTotalWithTerm - i] + logFactorial[total - numTotalWithTerm - totalChosen + i]);
            double pVal = Math.pow(Math.E, logPval);
            sum += pVal;
        }
        return sum;
    }

    /**
     * Tests for compound enrichment, given an input sample, Terms, and their associated ontologies. This method is
     * equivalent to <code>getCompoundEnrichment(String, int[], String[])</code>, except that instead of referring to Terms
     * by their integer IDs, you can refer to terms by their names.
     *
     * @param accessions        the list of accessions comprising the input sample
     * @param term_names        the names of Terms for which to test for compound enrichment
     * @param ontology_names    the ontologies that correspond to the terms in the <code>term_names</code> parameter, where
     *                          the nth element of <code>ontology_names</code> specifies the ontology for the nth element
     *                          of <code>term_names</code>
     * @return                  if the number of accessions in the input sample annotated with the specified Terms is
     *                          greater than "minimum count" and the calculated p-value is smaller than "maximum p-value",
     *                          the <code>CompoundEnrichment</code> object describing this enrichment is returned. Otherwise,
     *                          a value of null is returned.
     * @see CompoundEnrichment
     * @see SimpleEnrichment
     * @see Enrichment
     */
    public CompoundEnrichment getCompoundEnrichment(String[] accessions, String[] term_names, String[] ontology_names) throws Exception {
        int[] term_ids = new int[term_names.length];
        for (int i = 0; i < term_names.length; i++) {
            term_ids[i] = getOntology(ontology_names[i]).getTermByName(term_names[i]).integer_id.intValue();
        }
        return getCompoundEnrichment(accessions, term_ids, ontology_names);
    }

    /**
     * Tests for compound enrichment of a set of specified Terms from a set of specified ontologies in a given input sample of accessions. Prior to using <code>getCompoundEnrichment</code>,
     * the specified ontologies must first be loaded (<code>loadOntology</code>) along with annotation data (<code>loadAnnotation</code>).
     * <p>
     * <code>getCompoundEnrichment</code> takes in a list of accessions (as <code>String</code>s) and a list of integer IDs
     * of the Terms for which to test for compound enrichment. Compound enrichment is defined as the occurrence of a set of terms
     * together at a frequency that is significantly higher than random chance would predict. In other words, if compound
     * enrichment is found, then the number of accessions in the input sample that are annotated with all of the specified Terms
     * is significantly higher than random chance would predict. Compound enrichment enables asking separate and more complex
     * questions than can be answered by simple enrichment alone. For example, consider an input sample of genes from organism X.
     * The genome of organism X could be abundant in kinases and abundant in proteins that regulate transport phenomena, but
     * proteins that both are kinases and regulate transport phenomena are relatively rare. In the input sample of genes,
     * there may not be simple enrichment for kinases or regulation of transport phenomena as reported by <code>getSimpleEnrichment</code>,
     * since both appear commonly on their own, and the probability of picking genes out of the genome of organism X that is
     * a kinase or that regulates transport phenomena is high. However, since proteins with both traits are rare, the probability
     * of seeing proteins with both traits in the input sample is much lower; thus, there could very well be
     * significance in observing a certain number of proteins with both traits in the input sample. <code>getCompoundEnrichment</code>
     * serves to test for the significance of such hypotheses.
     * <p>
     * Unlike <code>getSimpleEnrichment</code>, which evaluates the p-values of all Terms that could be enriched,
     * <code>getCompoundEnrichment</code> only calculates the p-value of the set of specified Terms being enriched as a unit, in tandem.
     * This is because, if we only limited the number of compound enrichment Terms to two, we would still have a vastly greater
     * number of hypotheses to test than in the simple enrichment case, since we would have to obtain a p-value for every
     * pairwise combination of Terms. The situation grows even worse for compound enrichment of three, four, and more Terms.
     * Thus, it is suggested that user intelligently selects a number of sets of Terms to test for compound enrichment,
     * based upon experimental data and inference and, perhaps, results (or lack thereof) from <code>getSimpleEnrichment</code>.
     * These are all guiding factors with which to determine which Terms to test for compound enrichment. After invoking
     * <code>getCompoundEnrichment</code> for the number of desired tests, you can then opt to apply multiple
     * hypothesis correction at your own discretion by using the <code>applyMultipleHypothesisCorrection</code> method.
     * <p>
     * In addition to specifying the Terms to test for compound enrichment, you must also specify the ontologies from
     * which they come from. The nth element of the list of ontology names is assumed to refer to the ontology for the nth
     * element of the list of Terms. The ontology names do not all have to be different; you can test for compound
     * enrichment on multiple Terms of the same ontology. For example, we might imagine that "kinase" and "regulator of
     * transport phenomena" are two terms from the Gene Ontology (GO). If compound enrichment is found to be significant,
     * a <code>CompoundEnrichment</code> object is created and returned; otherwise, a value of null is returned.
     * <p>
     * In order to specify what is meant by "significantly higher than random chance," <code>OBOES</code> maintains a "minimum count" and "maximum p-value" as cut-offs to decide when a set of Terms is
     * significantly enriched or not. In order for the Terms to be significantly enriched, the number of accessions
     * annotated with all of those Terms must be at least the "minimum count." In addition, the
     * statistically computed p-value must be at most the "maximum p-value." The "minimum count" defaults to 2; the
     * "maximum p-value" defaults to 0.01, which is usually a standard choice to balance between Type I and Type II errors.
     * The "minimum count" and "maximum p-value" can be manually set and retrieved using <code>OBOES</code> methods. Any
     * invocation of <code>getSimpleEnrichment</code> and <code>getCompoundEnrichment</code> will only consider an
     * enrichment to be significant if it satisfies the "maximum p-value" and "minimum count" values that are currently
     * set in the <code>OBOES</code> object.
     * <p>
     * To compute p-values, <code>OBOES</code> uses Fisher's exact test to compute the exact p-value. In other words,
     * the reported p-value is the probability of observing a set of Terms at its level
     * of representation in the input sample or higher. The probabilities follow a hypergeometric distribution.
     * <p>
     * <code>OBOES</code> provides additional methods that influence the statistics involved in searching for enrichment.
     * For example, <code>getCompoundEnrichment</code> defaults the total sample space size to the total number of unique accessions
     * across all specified ontologies for the Terms of compound enrichment. The total sample space is understood to refer to the entire set
     * of possible accessions from which the accessions in a given input sample are selected. For example, the total
     * sample space for a microarray experiment would be the whole genome of the organism, and the sample space size
     * is then equal to the number of genes for the organism.
     *
     * @param accessions        the list of accessions comprising the input sample
     * @param term_ids          the integer IDs of Terms for which to test for compound enrichment
     * @param ontology_names    the ontologies that correspond to the terms in the <code>term_names</code> parameter, where
     *                          the nth element of <code>ontology_names</code> specifies the ontology for the nth element
     *                          of <code>term_names</code>
     * @return                  if the number of accessions in the input sample annotated with the specified Terms is
     *                          greater than "minimum count" and the calculated p-value is smaller than "maximum p-value",
     *                          the <code>CompoundEnrichment</code> object describing this enrichment is returned. Otherwise,
     *                          a value of null is returned.
     * @see CompoundEnrichment
     * @see SimpleEnrichment
     * @see Enrichment
     */
    public CompoundEnrichment getCompoundEnrichment(String[] accessions, int[] term_ids, String[] ontology_names) throws Exception {
        if (term_ids == null || term_ids.length < 1 || ontology_names == null | ontology_names.length < 1) {
            if (this.verbose) System.out.println("Error: invalid input for terms or ontologies");
            return null;
        }
        if (term_ids.length != ontology_names.length) {
            if (this.verbose) System.out.println("Error: Mismatch between list of terms submitted for compound enrichment and list of corresponding ontologies");
            return null;
        }
        HashSet backgroundAccessions = new HashSet();
        HashSet myAccessions = new HashSet();
        HashSet terms = new HashSet(term_ids.length);
        for (int i = 0; i < accessions.length; i++) {
            myAccessions.add(accessions[i]);
        }
        int totalAccessions = myAccessions.size();
        Ontology ontology = getOntology(ontology_names[0]);
        HashSet backgroundAccessionsWithTheseTerms = ontology.getHashSetAccessionsForTerm(term_ids[0]);
        if (ontology.TermToBackgroundAccessions.size() > 0) {
            backgroundAccessionsWithTheseTerms = (HashSet) ontology.TermToBackgroundAccessions.get(new Integer(term_ids[0]));
        }
        for (int i = 0; i < term_ids.length; i++) {
            ontology = getOntology(ontology_names[i]);
            HashSet backgroundacc = ontology.getHashSetAccessionsForTerm(term_ids[i]);
            if (ontology.TermToBackgroundAccessions.size() > 0) {
                backgroundacc = (HashSet) ontology.TermToBackgroundAccessions.get(new Integer(term_ids[i]));
            }
            if (backgroundacc != null) {
                backgroundAccessionsWithTheseTerms.retainAll(backgroundacc);
            }
            backgroundacc = ontology.getHashSetAllAccessions();
            if (ontology.BackgroundAccessionToTerms.size() > 0) {
                backgroundacc = (HashSet) ontology.BackgroundAccessionToTerms.keySet();
            }
            backgroundAccessions.addAll(backgroundacc);
            HashSet intermed = new HashSet(backgroundAccessionsWithTheseTerms);
            intermed.addAll(ontology.getHashSetAccessionsForTerm(term_ids[i]));
            myAccessions.retainAll(intermed);
            terms.add(ontology.getTermByID(term_ids[i]));
        }
        int numTotalWithTerms = backgroundAccessionsWithTheseTerms.size();
        int totalCount;
        if (this.sample_space_size > 0) {
            totalCount = this.sample_space_size;
        } else {
            totalCount = backgroundAccessions.size();
        }
        int count = myAccessions.size();
        if (count < this.getMinimumCount()) {
            return null;
        }
        double pval = computePval(totalAccessions, count, totalCount, numTotalWithTerms);
        if (pval > this.getMaximumPvalue()) {
            return null;
        }
        String[] s = new String[count];
        OBO_Object[] obj_terms = new OBO_Object[term_ids.length];
        return new CompoundEnrichment((OBO_Object[]) terms.toArray(obj_terms), (String[]) myAccessions.toArray(s), pval, count, (double) count / (double) totalAccessions * 100.0);
    }

    /**
     * Returns an ontology term partition, represented as an array of <code>OBO_Object</code>s, given specific parameters.
     * An ontology term partition is a method of obtaining a set of ontology Terms for use with purposes such as
     * visual graph enrichment or first-pass annotation enrichment. The Terms comprising a partition are selected
     * based on the total number of Terms desired and the information content (Shannon information) of the Terms of the
     * ontology. Terms are chosen according to the theoretical optimal information content, which is the information content
     * of a term assuming, on average, an accession is annotated by one Term in the partition. For more on ontology term
     * partitions, please refer to the publication (Alterovitz, Xiang, Ramoni 2007, Nucleic Acids Research).
     *
     * @param ontology_name    the name of the ontology (specified in <code>loadOntology</code>)
     * @param numNodes    the number of nodes (Terms) to form the partition
     * @param root  indicates the numerical ID of a specified node, under which selection of partition nodes should only occur; if set to 0, then all terms in the ontology are available
     * @param addIfNonempty <code>boolean</code> value. If true, nodes will only be considered as partitions if they contain at least 1 accession.
     * @return  an array of the ontology Terms comprising the partition
     */
    public OBO_Object[] getOntologyPartitionTerms(String ontology_name, int numNodes, int root, boolean addIfNonempty) throws Exception {
        Ontology ontology = getOntology(ontology_name);
        HashSet allTerms = ontology.getHashSetAllTerms();
        int totalGenes = ontology.getHashSetAllAccessions().size();
        double p = 1.00 / (double) (numNodes);
        double optimal_value = -1 * Math.log(p) / Math.log(2);
        ArrayList AllOBO_Objects = new ArrayList();
        OBO_Object[] AllOBO_Objects_array;
        if (root == 0) {
            Iterator it = allTerms.iterator();
            AllOBO_Objects_array = new OBO_Object[1];
            AllOBO_Objects_array = (OBO_Object[]) allTerms.toArray(AllOBO_Objects_array);
            while (it.hasNext()) {
                OBO_Object obj = (OBO_Object) it.next();
                obj.information_content = Math.abs(obj.information_content - optimal_value);
            }
        } else {
            int[] descendants = ontology.getAllDescendants(root);
            AllOBO_Objects_array = new OBO_Object[descendants.length];
            for (int i = 0; i < descendants.length; i++) {
                AllOBO_Objects_array[i] = ontology.getTermByID(descendants[i]);
                AllOBO_Objects_array[i].information_content = Math.abs(AllOBO_Objects_array[i].information_content - optimal_value);
            }
        }
        Arrays.sort(AllOBO_Objects_array);
        HashSet optimalNodes = new HashSet();
        HashSet disallowedNodes = new HashSet();
        HashSet disallowedTermNames = new HashSet();
        optimalNodes.add(AllOBO_Objects_array[0]);
        for (int index = 1; index < AllOBO_Objects_array.length && optimalNodes.size() < numNodes; index++) {
            OBO_Object nextNode = AllOBO_Objects_array[index];
            if (nextNode.is_obsolete || disallowedNodes.contains(nextNode) || disallowedTermNames.contains(nextNode.name)) continue;
            if (addIfNonempty) {
                HashSet hs = ontology.getHashSetAccessionsForTerm(nextNode);
                if (hs == null || hs.size() == 0) continue;
            }
            HashSet ancestors = ontology.getHashSetAllAncestors(nextNode.integer_id.intValue());
            HashSet descendants = ontology.getHashSetAllDescendants(nextNode.integer_id.intValue());
            disallowedNodes.addAll(ancestors);
            disallowedNodes.addAll(descendants);
            optimalNodes.add(nextNode);
            disallowedTermNames.add(nextNode.name);
        }
        if (root == 0) ontology.calculateInformationContentOfNodes(); else ontology.calculateInformationContentOfNodes(root);
        return (OBO_Object[]) optimalNodes.toArray(new OBO_Object[1]);
    }

    /**
     * Compares the annotation composition of one set of accessions with another set of accessions. The lower
     * the relative entropy, the closer the distributions of annotation between the two sets of accessions. Relative entropy
     * can be thought of as the "distance" between two distributions, although it is not a true distance metric. For
     * more information regarding relative entropy, consult an information theory text.
     *
     * @param accessions1   the first set of accessions
     * @param accessions2   the second set of accessions
     * @param ontology_name    the name of the ontology (specified in <code>loadOntology</code>)
     * @return  the relative entropy of the annotation distributions of the two sets of accessions, in bits
     */
    public double calculateRelativeEntropy(String[] accessions1, String[] accessions2, String ontology_name) throws Exception {
        Ontology ontology = getOntology(ontology_name);
        HashSet accessions1Set = new HashSet(accessions1.length);
        HashSet accessions2Set = new HashSet(accessions2.length);
        for (int i = 0; i < accessions1.length; i++) {
            accessions1Set.add(accessions1[i]);
        }
        for (int i = 0; i < accessions2.length; i++) {
            accessions2Set.add(accessions2[i]);
        }
        double denominator1 = 0;
        double denominator2 = 0;
        HashSet allTerms = ontology.getHashSetAllTerms();
        Iterator it = allTerms.iterator();
        while (it.hasNext()) {
            OBO_Object obj = (OBO_Object) it.next();
            HashSet accessionsWithThisTerm = accessionsWithThisTerm = ontology.getHashSetAccessionsForTerm(obj);
            if (accessionsWithThisTerm == null || accessionsWithThisTerm.size() == 0) continue;
            HashSet accessions1WithThisTerm = new HashSet(accessions1Set);
            accessions1WithThisTerm.retainAll(accessionsWithThisTerm);
            HashSet accessions2WithThisTerm = new HashSet(accessions2Set);
            accessions2WithThisTerm.retainAll(accessionsWithThisTerm);
            denominator1 += accessions1WithThisTerm.size();
            if (accessions2WithThisTerm.size() == 0 && accessions1WithThisTerm.size() > 0) {
                denominator2 += 0.5;
            } else {
                denominator2 += accessions2WithThisTerm.size();
            }
        }
        it = allTerms.iterator();
        Hashtable normalizedProbabilities = new Hashtable();
        while (it.hasNext()) {
            OBO_Object obj = (OBO_Object) it.next();
            HashSet accessionsWithThisTerm = accessionsWithThisTerm = ontology.getHashSetAccessionsForTerm(obj);
            if (accessionsWithThisTerm == null || accessionsWithThisTerm.size() == 0) continue;
            HashSet accessions1WithThisTerm = new HashSet(accessions1Set);
            accessions1WithThisTerm.retainAll(accessionsWithThisTerm);
            HashSet accessions2WithThisTerm = new HashSet(accessions2Set);
            accessions2WithThisTerm.retainAll(accessionsWithThisTerm);
            double[] normProb = new double[2];
            normProb[0] = (double) accessions1WithThisTerm.size() / (double) denominator1;
            normProb[1] = (double) accessions2WithThisTerm.size() / (double) denominator2;
            normalizedProbabilities.put(obj.integer_id, normProb);
        }
        it = normalizedProbabilities.values().iterator();
        double relative_entropy = 0;
        double log2 = Math.log(2);
        while (it.hasNext()) {
            double[] normProb = (double[]) it.next();
            if (normProb[0] == 0) {
                continue;
            }
            if (normProb[1] == 0) {
                normProb[1] = 0.5 / (double) denominator2;
            }
            relative_entropy += (normProb[0] * (Math.log(normProb[0] / normProb[1]) / log2));
        }
        return relative_entropy;
    }

    /**
     * Compares the annotation composition of a set of accessions with another. Mutual information is calculated from
     * the annotation distributions of the two sets of accessions. The higher the mutual information, the more properties
     * can be inferred from one set of accessions regarding another. Uses discretized bins of probability of annotation.
     * For more information about mutual information, consult an information theory text.
     *
     * @param accessions1   the first set of accessions
     * @param accessions2   the second set of accessions
     * @param divisions indicates cut-off values for annotation frequencies used to discretize annotation distributions for mutual information calculation
     * @param ontology_name    the name of the ontology (specified in <code>loadOntology</code>)
     * @return  the mutual information of the annotation distributions of the sets of accessions
     */
    public double calculateMutualInformation(String[] accessions1, String[] accessions2, double[] divisions, String ontology_name) throws Exception {
        Ontology ontology = getOntology(ontology_name);
        HashSet accessions1Set = new HashSet(accessions1.length);
        HashSet accessions2Set = new HashSet(accessions2.length);
        for (int i = 0; i < accessions1.length; i++) {
            accessions1Set.add(accessions1[i]);
        }
        for (int i = 0; i < accessions2.length; i++) {
            accessions2Set.add(accessions2[i]);
        }
        double[] bounds = new double[divisions.length + 1];
        bounds[bounds.length - 1] = 1;
        for (int i = 0; i < divisions.length; i++) {
            bounds[i] = divisions[i];
        }
        Arrays.sort(bounds);
        HashSet allTerms = ontology.getHashSetAllTerms();
        Iterator it = allTerms.iterator();
        Hashtable representedPercentages = new Hashtable();
        while (it.hasNext()) {
            OBO_Object obj = (OBO_Object) it.next();
            HashSet accessionsWithThisTerm = accessionsWithThisTerm = ontology.getHashSetAccessionsForTerm(obj);
            if (accessionsWithThisTerm == null || accessionsWithThisTerm.size() == 0) continue;
            HashSet accessions1WithThisTerm = new HashSet(accessions1Set);
            accessions1WithThisTerm.retainAll(accessionsWithThisTerm);
            HashSet accessions2WithThisTerm = new HashSet(accessions2Set);
            accessions2WithThisTerm.retainAll(accessionsWithThisTerm);
            double[] percentages = new double[2];
            percentages[0] = (double) accessions1WithThisTerm.size() / (double) accessions1Set.size();
            percentages[1] = (double) accessions2WithThisTerm.size() / (double) accessions2Set.size();
            representedPercentages.put(obj.integer_id, percentages);
        }
        double mutualInformation = 0;
        double lowerBoundX = 0;
        double log2 = Math.log(2);
        for (int x = 0; x < bounds.length; x++) {
            double lowerBoundY = 0;
            double upperBoundX = bounds[x];
            for (int y = 0; y < bounds.length; y++) {
                double upperBoundY = bounds[y];
                int numInX = 0;
                int numInY = 0;
                int numInXAndY = 0;
                it = representedPercentages.values().iterator();
                while (it.hasNext()) {
                    double[] percentages = (double[]) it.next();
                    boolean inX = false;
                    boolean inY = false;
                    if (percentages[0] >= lowerBoundX && percentages[0] < upperBoundX) {
                        numInX++;
                        inX = true;
                    }
                    if (percentages[1] >= lowerBoundY && percentages[1] < upperBoundY) {
                        numInY++;
                        inY = true;
                    }
                    if (inX && inY) {
                        numInXAndY++;
                    }
                }
                lowerBoundY = upperBoundY;
                double pXandY = (double) numInXAndY / (double) allTerms.size();
                if (pXandY == 0) {
                    continue;
                }
                double pX = (double) numInX / (double) allTerms.size();
                double pY = (double) numInY / (double) allTerms.size();
                mutualInformation += ((pXandY) * (Math.log(pXandY / (pX * pY)) / log2));
            }
            lowerBoundX = upperBoundX;
        }
        return mutualInformation;
    }

    /**
     * Calculates the total entropy contained within the annotation distribution of a set of accessions, also
     * represented by H(X). Uses discretized bins of probability of annotation. For more information regarding total entropy, consult an information theory text.
     *
     * @param accessions   the set of accessions
     * @param divisions indicates cut-off values for annotation frequencies used to discretize annotation distributions for total entropy calculation
     * @param ontology_name    the name of the ontology (specified in <code>loadOntology</code>)
     * @return  the total entropy of the annotation distribution of the set of accessions
     */
    public double calculateTotalEntropy(String[] accessions, double[] divisions, String ontology_name) throws Exception {
        Ontology ontology = getOntology(ontology_name);
        HashSet accessionsSet = new HashSet(accessions.length);
        for (int i = 0; i < accessions.length; i++) {
            accessionsSet.add(accessions[i]);
        }
        double[] bounds = new double[divisions.length + 1];
        bounds[bounds.length - 1] = 1;
        for (int i = 0; i < divisions.length; i++) {
            bounds[i] = divisions[i];
        }
        Arrays.sort(bounds);
        HashSet allTerms = ontology.getHashSetAllTerms();
        Iterator it = allTerms.iterator();
        Hashtable representedPercentages = new Hashtable();
        while (it.hasNext()) {
            OBO_Object obj = (OBO_Object) it.next();
            HashSet accessionsWithThisTerm = accessionsWithThisTerm = ontology.getHashSetAccessionsForTerm(obj);
            if (accessionsWithThisTerm == null || accessionsWithThisTerm.size() == 0) continue;
            accessionsWithThisTerm.retainAll(accessionsSet);
            double[] percentage = new double[1];
            percentage[0] = (double) accessionsWithThisTerm.size() / (double) accessionsSet.size();
            representedPercentages.put(obj.integer_id, percentage);
        }
        double entropy = 0;
        double lowerBoundX = 0;
        double log2 = Math.log(2);
        for (int x = 0; x < bounds.length; x++) {
            double upperBoundX = bounds[x];
            int numInX = 0;
            it = representedPercentages.values().iterator();
            while (it.hasNext()) {
                double[] percentage = (double[]) it.next();
                if (percentage[0] >= lowerBoundX && percentage[0] < upperBoundX) {
                    numInX++;
                }
            }
            if (numInX > 0) {
                double pX = (double) numInX / (double) allTerms.size();
                entropy += -1 * ((pX) * (Math.log(pX) / log2));
            }
            lowerBoundX = upperBoundX;
        }
        return entropy;
    }

    /**
     * Calculates the conditional entropy contained within the annotation distribution of a set of accessions given the
     * annotation distribution of another set of accessions. Uses discretized bins of probability of annotation.
     * For more information regarding conditional entropy, consult an information theory text.
     *
     * @param accessions1   the first set of accessions; goal is to calculate the conditional entropy in this set, given the second set of accessions
     * @param accessions2   the second set of accessions
     * @param divisions indicates cut-off values for annotation frequencies used to discretize annotation distributions for entropy calculation
     * @param ontology_name    the name of the ontology (specified in <code>loadOntology</code>)
     * @return  the conditional entropy of the annotation distribution of the first set of accessions, given the second set of accessions
     */
    public double calculateConditionalEntropy(String[] accessions1, String[] accessions2, double[] divisions, String ontology_name) throws Exception {
        double mutualInformation = calculateMutualInformation(accessions1, accessions2, divisions, ontology_name);
        double totalEntropy = calculateTotalEntropy(accessions1, divisions, ontology_name);
        return totalEntropy - mutualInformation;
    }

    /******************************** PROPRIETARY FOR HST **********************************/
    public double[][] NARWebserverFunction_NoLoad(String[] UniProts, String ontology_name, int GOID1, int GOID2, int GOID3) throws Exception {
        Ontology ontology = getOntology(ontology_name);
        ontology.calculateInformationContentOfNodes();
        System.out.println("Computing spatial coordinates...");
        double log2 = Math.log(2);
        int totalCount = ontology.getHashSetAllAccessions().size();
        double[][] spatial_coordinates = new double[UniProts.length][3];
        for (int x = 0; x < UniProts.length; x++) {
            double GOID1_max = 0;
            double GOID2_max = 0;
            double GOID3_max = 0;
            HashSet terms = ontology.getHashSetTermsForAccession(UniProts[x]);
            if (terms != null) {
                Iterator it = terms.iterator();
                while (it.hasNext()) {
                    OBO_Object term = (OBO_Object) it.next();
                    HashSet accessions = ontology.getHashSetAccessionsForTerm(term);
                    double p = (double) accessions.size() / (double) totalCount;
                    double information_content = -1 * Math.log(p) / log2;
                    HashSet allAncestors = ontology.getHashSetAllAncestors(term.integer_id.intValue());
                    if (allAncestors.contains(ontology.getTermByID(GOID1)) && information_content > GOID1_max) {
                        GOID1_max = information_content;
                    }
                    if (allAncestors.contains(ontology.getTermByID(GOID2)) && information_content > GOID2_max) {
                        GOID2_max = information_content;
                    }
                    if (allAncestors.contains(ontology.getTermByID(GOID3)) && information_content > GOID3_max) {
                        GOID3_max = information_content;
                    }
                }
            }
            spatial_coordinates[x][0] = GOID1_max;
            spatial_coordinates[x][1] = GOID2_max;
            spatial_coordinates[x][2] = GOID3_max;
        }
        return spatial_coordinates;
    }

    public double[][] NARWebserverFunction_NoLoad_EntropyCalc(String[] UniProts, String ontology_name, int GOID1, int GOID2, int GOID3) throws Exception {
        Ontology ontology = getOntology(ontology_name);
        ontology.calculateInformationContentOfNodes();
        System.out.println("Computing spatial coordinates...");
        double log2 = Math.log(2);
        int totalCount = ontology.getHashSetAllAccessions().size();
        double[][] spatial_coordinates = new double[UniProts.length][3];
        for (int x = 0; x < UniProts.length; x++) {
            double GOID1_max = 0;
            double GOID2_max = 0;
            double GOID3_max = 0;
            HashSet terms = ontology.getHashSetTermsForAccession(UniProts[x]);
            if (terms != null) {
                Iterator it = terms.iterator();
                while (it.hasNext()) {
                    OBO_Object term = (OBO_Object) it.next();
                    HashSet accessions = ontology.getHashSetAccessionsForTerm(term);
                    double p = (double) accessions.size() / (double) totalCount;
                    double information_content = -1 * p * Math.log(p) / log2;
                    HashSet allAncestors = ontology.getHashSetAllAncestors(term.integer_id.intValue());
                    if (allAncestors.contains(ontology.getTermByID(GOID1)) && information_content > GOID1_max) {
                        GOID1_max = information_content;
                    }
                    if (allAncestors.contains(ontology.getTermByID(GOID2)) && information_content > GOID2_max) {
                        GOID2_max = information_content;
                    }
                    if (allAncestors.contains(ontology.getTermByID(GOID3)) && information_content > GOID3_max) {
                        GOID3_max = information_content;
                    }
                }
            }
            spatial_coordinates[x][0] = GOID1_max;
            spatial_coordinates[x][1] = GOID2_max;
            spatial_coordinates[x][2] = GOID3_max;
        }
        return spatial_coordinates;
    }

    public String[] getGenesForMicroarrayChip(String ontology_name, int minGenes, int maxGenes, int numPartitions, int restrictToBranch, int GO_constraint1, int GO_constraint2) throws Exception {
        Ontology ontology = getOntology(ontology_name);
        HashSet genesBeneathRoot = ontology.getHashSetAccessionsForTerm(restrictToBranch);
        if (genesBeneathRoot.size() <= minGenes) {
            System.out.println("Fewer genes than the minimum specified (" + minGenes + ") are annotated to the specified root. All genes beneath root are returned; filtering by GO terms (if specified) was NOT done.");
            return (String[]) genesBeneathRoot.toArray(new String[1]);
        }
        System.out.println("Getting GO partitions...");
        OBO_Object[] partitions;
        HashSet allPartitions;
        HashSet remainingGenes;
        int numPartitionsToGet = numPartitions;
        while (true) {
            partitions = getOntologyPartitionTerms(ontology_name, numPartitionsToGet, restrictToBranch, true);
            allPartitions = new HashSet();
            remainingGenes = new HashSet();
            for (int i = 0; i < partitions.length; i++) {
                HashSet genes = ontology.getHashSetAccessionsForTerm(partitions[i]);
                allPartitions.add(partitions[i]);
                remainingGenes.addAll(genes);
            }
            if (remainingGenes.size() < minGenes && numPartitionsToGet >= 2) {
                numPartitionsToGet = (int) Math.ceil((double) numPartitionsToGet / 2.0);
                continue;
            } else {
                break;
            }
        }
        HashSet microarrayGenes = new HashSet();
        System.out.println("Number of partitions requested: " + numPartitions + "; actual number successfully returned: " + allPartitions.size());
        System.out.println("Total genes covered by partitions: " + remainingGenes.size());
        System.out.println("Filtering by GO constraints (if specified)...");
        if (GO_constraint1 != 0) {
            HashSet genes = ontology.getHashSetAccessionsForTerm(GO_constraint1);
            remainingGenes.retainAll(genes);
        }
        if (GO_constraint2 != 0) {
            HashSet genes = ontology.getHashSetAccessionsForTerm(GO_constraint2);
            remainingGenes.retainAll(genes);
        }
        System.out.println("Total genes covered by partitions AND GO constraints (if specified): " + remainingGenes.size());
        System.out.println("Getting microarray genes...");
        while (remainingGenes.size() > 0 && microarrayGenes.size() < maxGenes) {
            HashSet partitionsToConsider = new HashSet(allPartitions);
            for (int i = 1; i <= partitions.length; i++) {
                Iterator it2 = allPartitions.iterator();
                while (it2.hasNext()) {
                    OBO_Object nextPart = (OBO_Object) it2.next();
                    HashSet genesInPartition = ontology.getHashSetAccessionsForTerm(nextPart);
                    genesInPartition.retainAll(remainingGenes);
                    if (genesInPartition.size() == 0) {
                        partitionsToConsider.remove(nextPart);
                        continue;
                    }
                    int highestCoverage = 0;
                    String geneToAdd = "";
                    Iterator it = genesInPartition.iterator();
                    while (it.hasNext()) {
                        String gene = (String) it.next();
                        HashSet terms = ontology.getHashSetTermsForAccession(gene);
                        int coverage = terms.size();
                        terms.retainAll(allPartitions);
                        int specificity = terms.size();
                        if (specificity == i) {
                            if (coverage > highestCoverage) {
                                highestCoverage = coverage;
                                geneToAdd = gene;
                            }
                        }
                    }
                    if (geneToAdd.length() > 0) {
                        partitionsToConsider.remove(nextPart);
                        microarrayGenes.add(geneToAdd);
                        remainingGenes.remove(geneToAdd);
                    }
                    if (partitionsToConsider.size() == 0) {
                        break;
                    }
                    if (remainingGenes.size() == 0 || microarrayGenes.size() == maxGenes) {
                        System.out.println("Number of microarray genes returned: " + microarrayGenes.size());
                        return (String[]) microarrayGenes.toArray(new String[1]);
                    }
                }
            }
        }
        System.out.println("Number of microarray genes returned: " + microarrayGenes.size());
        return (String[]) microarrayGenes.toArray(new String[1]);
    }

    public double calculateEntropyRateForOntology(String ontology_name) throws Exception {
        Ontology ontology = getOntology(ontology_name);
        HashSet allTerms = ontology.getHashSetAllTerms();
        int[] nodeWeights = new int[allTerms.size()];
        Iterator it = allTerms.iterator();
        int x = 0;
        while (it.hasNext()) {
            OBO_Object obj = (OBO_Object) it.next();
            nodeWeights[x] = (ontology.getAllChildren(obj.integer_id.intValue())).length;
            x = x + 1;
        }
        int totalWeights = 0;
        for (x = 0; x < nodeWeights.length; x++) {
            totalWeights += nodeWeights[x];
        }
        double entropyRate = 0;
        double log2 = Math.log(2);
        for (x = 0; x < nodeWeights.length; x++) {
            if (nodeWeights[x] == 0) {
                continue;
            }
            entropyRate += ((double) nodeWeights[x] / (double) totalWeights) * (Math.log(nodeWeights[x]) / log2);
        }
        return entropyRate;
    }
}
