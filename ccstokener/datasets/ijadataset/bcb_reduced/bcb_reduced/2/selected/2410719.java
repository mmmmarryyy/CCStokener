package org.mcisb.ontology.uniprot;

import java.net.*;
import java.util.*;
import org.mcisb.ontology.*;
import org.mcisb.util.*;
import org.sbml.jsbml.*;
import uk.ac.ebi.kraken.interfaces.uniprot.*;
import uk.ac.ebi.kraken.interfaces.uniprot.comments.*;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.go.*;
import uk.ac.ebi.kraken.interfaces.uniprot.description.*;
import uk.ac.ebi.kraken.model.uniprot.dbx.geneid.*;
import uk.ac.ebi.kraken.uuw.services.remoting.*;

/**
 *
 * @author Neil Swainston
 */
public class UniProtTerm extends OntologyTerm {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    /**
	 * 
	 */
    private static final OntologyType CELLULAR_COMPARTMENT = OntologyType.C;

    /**
	 * 
	 */
    private Collection<String> organisms = null;

    /**
	 * 
	 */
    private Collection<String> ncbiTaxonomyIds = null;

    /**
	 * 
	 */
    private Collection<OntologyTerm> compartmentGoTerms = null;

    /**
	 * 
	 */
    private String sequence;

    /**
	 * 
	 */
    private String geneId;

    /**
	 * 
	 */
    private String uniProtId;

    /**
	 * 
	 */
    private UniProtEntryType type;

    /**
	 *
	 * @param id
	 * @throws Exception
	 */
    public UniProtTerm(final String id) throws Exception {
        super(OntologyFactory.getOntology(Ontology.UNIPROT), id);
    }

    /**
	 * 
	 * @return String
	 * @throws Exception 
	 */
    public synchronized String getUniProtId() throws Exception {
        if (uniProtId == null) {
            init();
        }
        return uniProtId;
    }

    /**
	 * 
	 * @return UniProtEntryType
	 * @throws Exception 
	 */
    public synchronized UniProtEntryType getType() throws Exception {
        if (type == null) {
            init();
        }
        return type;
    }

    /**
	 * 
	 * @return Collection
	 * @throws Exception
	 */
    public synchronized Collection<String> getOrganisms() throws Exception {
        if (organisms == null) {
            init();
        }
        return organisms;
    }

    /**
	 * 
	 * @return Collection
	 * @throws Exception
	 */
    public synchronized Collection<String> getNcbiTaxonomyIds() throws Exception {
        if (ncbiTaxonomyIds == null) {
            init();
        }
        return ncbiTaxonomyIds;
    }

    /**
	 * 
	 * @return Collection
	 * @throws Exception
	 */
    public synchronized Collection<OntologyTerm> getCompartmentGoTerms() throws Exception {
        if (compartmentGoTerms == null) {
            init();
        }
        return compartmentGoTerms;
    }

    /**
	 * 
	 * @return String
	 * @throws Exception 
	 */
    public synchronized String getSequence() throws Exception {
        if (sequence == null) {
            init();
        }
        return sequence;
    }

    /**
	 * 
	 * @return String
	 * @throws Exception 
	 */
    public synchronized String getGeneId() throws Exception {
        if (geneId == null) {
            init();
        }
        return geneId;
    }

    @Override
    protected synchronized void doInitialise() throws Exception {
        final EntryRetrievalService entryRetrievalService = UniProtJAPI.factory.getEntryRetrievalService();
        final UniProtEntry entry = entryRetrievalService.getUniProtEntry(id);
        if (entry != null) {
            uniProtId = entry.getUniProtId().getValue();
            type = entry.getType();
            final ProteinDescription proteinDescription = entry.getProteinDescription();
            if (proteinDescription.getRecommendedName().getFields().size() > 0) {
                setName(CollectionUtils.getFirst(proteinDescription.getRecommendedName().getFields()).getValue());
            }
            for (Iterator<Name> iterator = proteinDescription.getAlternativeNames().iterator(); iterator.hasNext(); ) {
                for (Iterator<Field> iterator2 = iterator.next().getFields().iterator(); iterator2.hasNext(); ) {
                    addSynonym(iterator2.next().getValue());
                }
            }
            for (DatabaseCrossReference crossReference : entry.getDatabaseCrossReferences()) {
                if (crossReference instanceof GeneIdImpl) {
                    geneId = ((GeneIdImpl) crossReference).getDbAccession();
                }
            }
            organisms = new TreeSet<String>();
            organisms.add(entry.getOrganism().getCommonName().getValue());
            ncbiTaxonomyIds = new TreeSet<String>();
            for (Iterator<NcbiTaxonomyId> iterator = entry.getNcbiTaxonomyIds().iterator(); iterator.hasNext(); ) {
                final NcbiTaxonomyId ncbiTaxonomyId = iterator.next();
                ncbiTaxonomyIds.add(ncbiTaxonomyId.getValue());
            }
            compartmentGoTerms = new LinkedHashSet<OntologyTerm>();
            for (final Comment comment : entry.getComments()) {
                if (comment instanceof SubcellularLocationComment) {
                    for (final SubcellularLocation subcellularLocation : ((SubcellularLocationComment) comment).getSubcellularLocations()) {
                        final SubcellularLocationValue locationValue = subcellularLocation.getLocation();
                        final String location = locationValue.getValue();
                        final OntologyTerm compartmentGoTerm = OntologyUtils.getInstance().getOntologyTerm(Ontology.GO, location);
                        if (compartmentGoTerm != null) {
                            compartmentGoTerms.add(compartmentGoTerm);
                        }
                    }
                }
            }
            for (final DatabaseCrossReference databaseCrossReference : entry.getDatabaseCrossReferences(DatabaseType.GO)) {
                final Go goCrossReference = (Go) databaseCrossReference;
                if (goCrossReference.getOntologyType().equals(CELLULAR_COMPARTMENT)) {
                    final OntologyTerm goTerm = OntologyUtils.getInstance().getOntologyTerm(Ontology.GO, goCrossReference.getGoId().getValue());
                    compartmentGoTerms.add(goTerm);
                }
            }
            sequence = entry.getSequence().getValue();
            addSynonym(id);
            addSynonym(name);
            addSynonym(geneId);
            addSynonym(uniProtId);
            final URL url = new URL("http://www.cathdb.info/cgi-bin/search.pl?search_text=" + id + "#tab-results");
            final Collection<String> cathNodes = RegularExpressionUtils.getMatches(url.openStream(), "(?<=http://www\\.cathdb\\.info/cathnode/)\\d+\\.\\d+\\.\\d+\\.\\d+(?=.*)");
            for (String cathNode : cathNodes) {
                final OntologyTerm cathTerm = OntologyUtils.getInstance().getOntologyTerm(Ontology.CATH, cathNode);
                if (cathTerm != null) {
                    addXref(cathTerm, CVTerm.Type.BIOLOGICAL_QUALIFIER, CVTerm.Qualifier.BQB_HAS_PROPERTY);
                }
            }
        }
    }
}
