package uk.ac.ebi.rhea.biopax.level2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collection;
import org.apache.log4j.Logger;
import org.biopax.paxtools.impl.level2.Level2FactoryImpl;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.io.jena.JenaIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level2.dataSource;
import org.biopax.paxtools.model.level2.publicationXref;
import org.biopax.paxtools.model.level2.relationshipXref;
import org.biopax.paxtools.model.level2.unificationXref;
import org.biopax.paxtools.model.level2.xref;
import uk.ac.ebi.biobabel.citations.DataSource;
import uk.ac.ebi.cdb.webservice.Author;
import uk.ac.ebi.cdb.webservice.Citation;
import uk.ac.ebi.rhea.domain.Database;
import uk.ac.ebi.rhea.domain.Reaction;
import uk.ac.ebi.rhea.domain.ReactionException;
import uk.ac.ebi.rhea.domain.XRef;
import uk.ac.ebi.rhea.mapper.util.IChebiHelper;

/**
 * Utility class to convert BioPAX OWL data and Rhea {@link Reaction}s objects
 * back and forth.
 * @author rafalcan
 *
 */
public class Biopax {

    public static final String RHEA_PREFIX = "rhea";

    public static final String RHEA_NS = "http://www.ebi.ac.uk/rhea#";

    private static final Logger LOGGER = Logger.getLogger(Biopax.class);

    /**
	 * Creates an empty ontology model with the needed namespaces.
	 * @return an OWL model.
	 */
    protected static Model createModel() {
        Model biopaxModel = new Level2FactoryImpl().createModel();
        biopaxModel.setXmlBase(RHEA_NS);
        biopaxModel.getNameSpacePrefixMap().put("", Biopax.RHEA_NS);
        biopaxModel.getNameSpacePrefixMap().put(RHEA_PREFIX, RHEA_NS);
        return biopaxModel;
    }

    /**
	 * Reads an OWL file from an external resource.
	 * @param url the resource URL.
	 * @param chebiHelper a ChEBI helper to define accurately compounds
	 * 		contained in the BioPAX model.
	 * @return a {@link BiopaxModel} wrapper
	 * @throws ReactionException
	 * @throws IOException 
	 */
    public static BiopaxModel read(URL url, IChebiHelper chebiHelper) throws ReactionException, IOException {
        BiopaxModel model = null;
        InputStream in = null;
        try {
            in = url.openStream();
            model = read(in, chebiHelper);
        } catch (IOException e) {
            LOGGER.error("Unable to read from URL " + url, e);
        } finally {
            if (in != null) in.close();
        }
        return model;
    }

    /**
	 * Reads an OWL file from an external resource.
	 * @param url the resource URL.
	 * @return a {@link BiopaxModel} wrapper
	 * @throws ReactionException
	 * @throws IOException
	 * @deprecated use {@link #read(URL, IChebiHelper)} instead.
	 */
    public static BiopaxModel read(URL url) throws ReactionException, IOException {
        return read(url, null);
    }

    /**
	 * Reads an OWL file from an InputStream.
	 * @param in the input stream.
	 * @param chebiHelper a ChEBI helper to define accurately compounds
	 * 		contained in the BioPAX model.
	 * @return a {@link BiopaxModel} wrapper
	 * @throws ReactionException
	 */
    public static BiopaxModel read(InputStream in, IChebiHelper chebiHelper) throws ReactionException {
        return new BiopaxModel(new JenaIOHandler().convertFromOWL(in), chebiHelper);
    }

    /**
	 * Reads an OWL file from an InputStream.
	 * @param in the input stream.
	 * @return a {@link BiopaxModel} wrapper
	 * @throws ReactionException
	 * @deprecated use {@link #read(InputStream, IChebiHelper)} instead.
	 */
    public static BiopaxModel read(InputStream in) throws ReactionException {
        return read(in, null);
    }

    /**
	 * Writes Rhea {@link Reaction}s in BioPAX format.
	 * @param reactions the reactions to write.
	 * @param out an output stream to write to.
	 * @param rheaRelease Rhea release number.
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws IOException 
	 */
    public static void write(Collection<Reaction> reactions, OutputStream out, String rheaRelease) throws IOException, IllegalAccessException, InvocationTargetException {
        Model model = Biopax.createModel();
        for (Reaction reaction : reactions) {
            try {
                new BiopaxBiochemicalReaction(reaction, model, rheaRelease);
                LOGGER.info("Added to BioPAX model - RHEA:" + reaction.getId().toString());
            } catch (Exception e) {
                LOGGER.error("Unable to convert to BioPAX - RHEA:" + reaction.getId().toString(), e);
            }
        }
        write(model, out);
    }

    /**
	 * Writes the model.
	 * @param model
	 * @param out
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws IOException 
	 */
    public static void write(Model model, OutputStream out) throws IOException, IllegalAccessException, InvocationTargetException {
        new SimpleIOHandler(BioPAXLevel.L2).convertToOWL(model, out);
    }

    private static String encode(String s) {
        String encoded = s;
        encoded = encoded.replace(' ', '_');
        return encoded;
    }

    /**
     * Fixes an RDF ID by encoding characters and adding a namespace prefix.
     * @param nsPrefix a namespace prefix to add. May be <code>null</code>.
     * @param id
     * @param encode encode ID characters?
     * @return
     */
    public static String fixId(String nsPrefix, String id, boolean encode) {
        String fixedId = encode ? encode(id) : id;
        return (nsPrefix == null || nsPrefix.length() == 0) ? "#" + fixedId : nsPrefix + ":" + fixedId;
    }

    /**
	 * Gets a {@link dataSource} from the BioPAX model,
	 * or creates it if it does not exist yet.
	 * @param src a Rhea source database.
	 * @param model a BioPAX model.
	 * @return a BioPAX {@link dataSource}.
	 */
    public static dataSource getBpDataSource(Database src, Model model) {
        String srcName = (src == null) ? Database.UNDEF.getName() : src.getName();
        String dsBpId = "#dataSource:" + encode(srcName);
        dataSource ds = (dataSource) model.getByID(dsBpId);
        if (ds == null) {
            ds = model.addNew(dataSource.class, dsBpId);
            ds.addNAME(src.getName());
        }
        return ds;
    }

    public static xref getBpXref(XRef rheaXref, String relationship, Model model) {
        return getBpXref(rheaXref, relationship, model, null);
    }

    /**
	 * Gets a {@link xref} from the BioPAX model,
	 * or creates it if it does not exist yet. <b>NOTE</b>: for citationXref,
	 * use method {@link #getBpPublicationXref(Citation, Model)}.
	 * @param rheaXref a Rhea {@link XRef}.
	 * @param relationship the relationship (<code>null</code> for
	 * 		{@link unificationXref}s).
	 * @param model a BioPAX model.
     * @param nsPrefix a namespace prefix for the RDF ID.
	 * @return a BioPAX {@link xref}.
	 */
    public static xref getBpXref(XRef rheaXref, String relationship, Model model, String nsPrefix) {
        xref x;
        String relPrefix = relationship == null ? "" : "rel/" + relationship + "/";
        String dbPrefix = Database.CHEBI.equals(rheaXref.getDatabase()) ? "" : rheaXref.getDatabaseName().toUpperCase() + ":";
        String xrefBpId = fixId(nsPrefix, relPrefix + dbPrefix + rheaXref.getAccessionNumber(), true);
        x = (xref) model.getByID(xrefBpId);
        if (x == null) {
            x = model.addNew(relationship == null ? unificationXref.class : relationshipXref.class, xrefBpId);
            x.setDB(rheaXref.getDatabaseName());
            if (rheaXref.getDbVersion() != null) {
                x.setDB_VERSION(rheaXref.getDbVersion());
            }
            x.setID(rheaXref.getAccessionNumber());
            if (relationship != null) {
                ((relationshipXref) x).setRELATIONSHIP_TYPE(relationship);
            }
        }
        return x;
    }

    public static publicationXref getBpPublicationXref(Citation cit, Model model) {
        return getBpPublicationXref(cit, model, null);
    }

    /**
	 * Gets a {@link publicationXref} from the BioPAX model,
	 * or creates it if it does not exist yet.
	 * @param cit a Rhea {@link Citation}.
	 * @param model a BioPAX model.
     * @param nsPrefix a namespace prefix for the RDF ID.
	 * @return a BioPAX {@link publicationXref}.
	 */
    public static publicationXref getBpPublicationXref(Citation cit, Model model, String nsPrefix) {
        publicationXref cx;
        String citBpId = fixId(nsPrefix, cit.getDataSource() + cit.getExternalId(), false);
        if (model.getByID(citBpId) != null) {
            cx = (publicationXref) model.getByID(citBpId);
        } else {
            cx = model.addNew(publicationXref.class, citBpId);
            cx.setDB(DataSource.valueOf(cit.getDataSource()).getName());
            cx.setID(cit.getExternalId());
            cx.setYEAR(cit.getJournalIssue().getYearOfPublication().intValue());
            cx.setTITLE(cit.getTitle());
            StringBuilder citSrc = new StringBuilder().append(cit.getJournalIssue().getJournal().getTitle()).append(' ').append(cit.getJournalIssue().getVolume()).append(", ").append(cit.getPageInfo());
            cx.addSOURCE(citSrc.toString());
            for (Author author : cit.getAuthorCollection()) {
                cx.addAUTHORS(author.getFullName());
            }
        }
        return cx;
    }
}
