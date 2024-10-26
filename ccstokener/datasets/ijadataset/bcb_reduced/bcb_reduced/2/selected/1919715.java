package uk.ac.ebi.intact.bridges.taxonomy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Access to the Newt data (that is, the NCBI taxonomy).
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id: NewtTaxonomyService.java 12907 2009-03-25 17:20:55Z skerrien $
 * @since 1.0
 */
public class NewtTaxonomyService implements TaxonomyService {

    /**
     * Parameter that will be replaced by a taxid in a generic URL.
     */
    public static final String TAXID_FLAG = "${taxid}";

    /**
     * Newt base URL
     */
    public static final String NEWT_URL = "http://www.ebi.ac.uk/newt/display";

    /**
     * URL allowing to retrieve a single term.
     */
    public static final String NEWT_URL_SPECIFIC_TERM = NEWT_URL + "?mode=IntAct&search=" + TAXID_FLAG + "&scope=term";

    /**
     * URL allowing to retrieve a term and its children.
     */
    public static final String NEWT_URL_TERMS_CHILDREN = NEWT_URL + "?mode=IntAct&search=" + TAXID_FLAG + "&scope=children";

    /**
     * URL allowing to retrieve a term and its parents.
     */
    public static final String NEWT_URL_TERMS_PARENT = NEWT_URL + "?mode=IntAct&search=" + TAXID_FLAG + "&scope=parent";

    public static final Boolean DEFAULT_CACHE_ENABLED = false;

    /**
     * Define is the caching is enabled.
     */
    private boolean cacheEnabled = DEFAULT_CACHE_ENABLED;

    /**
     * Cache NewtTerm by their taxid. When the cache is enabled, there should be only once instance of a NewtTerm with
     * a specific taxid.
     */
    private Map<Integer, TaxonomyTerm> cache;

    public NewtTaxonomyService() {
        this(DEFAULT_CACHE_ENABLED);
    }

    public NewtTaxonomyService(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
        if (cacheEnabled) {
            cache = new HashMap<Integer, TaxonomyTerm>();
        }
        throw new UnsupportedOperationException("The Newt service has been replaced by the UniProt Taxonomy " + "in March 2009, please use the OLS service instead: " + "uk.ac.ebi.intact.bridges.taxonomy.OLSTaxonomyService");
    }

    /**
     * Read a URL content and store each line in a List.
     *
     * @param url the URL to read.
     *
     * @return a non null Collection of String.
     *
     * @throws IOException
     */
    private List<String> readUrl(URL url) throws IOException {
        List<String> lines = new ArrayList<String>();
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String str;
        while ((str = in.readLine()) != null) {
            lines.add(str);
        }
        in.close();
        return lines;
    }

    public TaxonomyTerm getTaxonomyTerm(int taxid) throws TaxonomyServiceException {
        TaxonomyUtils.isSupportedTaxid(taxid);
        TaxonomyTerm term = null;
        if (cacheEnabled) {
            term = cache.get(taxid);
        }
        if (taxid == -1) {
            term = new TaxonomyTerm(-1);
            term.setScientificName("In vitro");
            term.setCommonName("In vitro");
        } else if (taxid == -2) {
            term = new TaxonomyTerm(-2);
            term.setScientificName("Chemical synthesis");
            term.setCommonName("Chemical synthesis");
        } else if (taxid == -3) {
            term = new TaxonomyTerm(-3);
            term.setScientificName("Unknown");
            term.setCommonName("Uunknown");
        } else if (taxid == -4) {
            term = new TaxonomyTerm(-4);
            term.setScientificName("In vivo");
            term.setCommonName("In vivo");
        } else if (taxid == -5) {
            term = new TaxonomyTerm(-5);
            term.setScientificName("In Silico");
            term.setCommonName("In Silico");
        }
        if (term == null) {
            List<String> terms = null;
            try {
                terms = readUrl(new URL(NEWT_URL_SPECIFIC_TERM.replace(TAXID_FLAG, String.valueOf(taxid))));
            } catch (IOException e) {
                throw new TaxonomyServiceException(e);
            }
            switch(terms.size()) {
                case 0:
                    break;
                case 1:
                    term = new NewtTerm(terms.iterator().next());
                    break;
                default:
                    throw new IllegalArgumentException("More than one line was returned by Newt");
            }
        }
        if (cacheEnabled && term != null) {
            cache.put(taxid, term);
        }
        return term;
    }

    public void retrieveChildren(TaxonomyTerm term, boolean recursively) throws TaxonomyServiceException {
        if (term.getTaxid() == -1) {
            return;
        } else if (term.getTaxid() == -2) {
            return;
        }
        List<TaxonomyTerm> children = getTermChildren(term.getTaxid());
        for (TaxonomyTerm child : children) {
            term.addChild(child);
        }
        if (recursively) {
            for (TaxonomyTerm child : term.getChildren()) {
                retrieveChildren(child, recursively);
            }
        }
    }

    public void retrieveParents(TaxonomyTerm term, boolean recursively) throws TaxonomyServiceException {
        if (term.getTaxid() == -1) {
            return;
        } else if (term.getTaxid() == -2) {
            return;
        }
        List<TaxonomyTerm> parents = getTermParent(term.getTaxid());
        for (TaxonomyTerm parent : parents) {
            term.addParent(parent);
        }
        if (recursively) {
            for (TaxonomyTerm parent : term.getParents()) {
                retrieveParents(parent, recursively);
            }
        }
    }

    public List<TaxonomyTerm> getTermChildren(int taxid) throws TaxonomyServiceException {
        TaxonomyUtils.isSupportedTaxid(taxid);
        List<TaxonomyTerm> terms = new ArrayList<TaxonomyTerm>();
        List<String> lines = null;
        try {
            lines = readUrl(new URL(NEWT_URL_TERMS_CHILDREN.replace(TAXID_FLAG, String.valueOf(taxid))));
        } catch (IOException e) {
            throw new TaxonomyServiceException(e);
        }
        switch(lines.size()) {
            case 0:
                break;
            default:
                for (String line : lines) {
                    TaxonomyTerm term = new NewtTerm(line);
                    if (cacheEnabled) {
                        TaxonomyTerm t = cache.get(term.getTaxid());
                        if (t != null) {
                            term = t;
                        } else {
                            cache.put(term.getTaxid(), term);
                        }
                    }
                    terms.add(term);
                }
        }
        terms.remove(terms.get(0));
        return terms;
    }

    public List<TaxonomyTerm> getTermParent(int taxid) throws TaxonomyServiceException {
        TaxonomyUtils.isSupportedTaxid(taxid);
        List<TaxonomyTerm> terms = new ArrayList<TaxonomyTerm>();
        if (taxid != 1) {
            List<String> lines = null;
            try {
                lines = readUrl(new URL(NEWT_URL_TERMS_PARENT.replace(TAXID_FLAG, String.valueOf(taxid))));
            } catch (IOException e) {
                throw new TaxonomyServiceException(e);
            }
            switch(lines.size()) {
                case 0:
                    break;
                default:
                    for (String line : lines) {
                        TaxonomyTerm term = new NewtTerm(line);
                        if (cacheEnabled) {
                            TaxonomyTerm t = cache.get(term.getTaxid());
                            if (t != null) {
                                term = t;
                            } else {
                                cache.put(term.getTaxid(), term);
                            }
                        }
                        terms.add(term);
                    }
            }
            terms.remove(terms.get(0));
        }
        return terms;
    }

    public String getSourceDatabaseMiRef() {
        return "MI:0247";
    }
}
