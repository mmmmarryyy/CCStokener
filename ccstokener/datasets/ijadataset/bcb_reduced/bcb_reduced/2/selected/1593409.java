package uk.ac.ebi.rhea.biopax;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import org.apache.log4j.Logger;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.io.jena.JenaIOHandler;
import org.biopax.paxtools.model.Model;
import uk.ac.ebi.rhea.domain.ReactionException;
import uk.ac.ebi.rhea.mapper.util.IChebiHelper;

/**
 * 
 * @author rafalcan
 */
public abstract class Biopax {

    public static final String RHEA_PREFIX = "rhea";

    public static final String RHEA_NS = "http://www.ebi.ac.uk/rhea#";

    protected static final Logger LOGGER = Logger.getLogger(Biopax.class);

    protected static String encode(String s) {
        String encoded = s.replace(' ', '_');
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

    protected static void addNamespaces(Model biopaxModel) {
        biopaxModel.getNameSpacePrefixMap().put("", Biopax.RHEA_NS);
        biopaxModel.getNameSpacePrefixMap().put(Biopax.RHEA_PREFIX, Biopax.RHEA_NS);
    }

    @Deprecated
    public static IBiopaxModel read(InputStream in, IChebiHelper chebiHelper) throws ReactionException {
        return read(in);
    }

    /**
	 * Reads an OWL file from an InputStream, either level 2 or 3.
	 * @param in the input stream.
	 * @return a {@link IBiopaxModel} wrapper
	 * @throws ReactionException
	 */
    public static IBiopaxModel read(InputStream in) throws ReactionException {
        IBiopaxModel o = null;
        Model model = new JenaIOHandler().convertFromOWL(in);
        switch(model.getLevel()) {
            case L2:
                o = new uk.ac.ebi.rhea.biopax.level2.BiopaxModel(model);
                break;
            case L3:
                o = new uk.ac.ebi.rhea.biopax.level3.BiopaxModel(model);
        }
        return o;
    }

    @Deprecated
    public static IBiopaxModel read(URL url, IChebiHelper chebiHelper) throws ReactionException, IOException {
        return read(url);
    }

    /**
	 * Reads an OWL file from an external resource, either level 2 or 3.
	 * @param url the resource URL.
	 * @return a {@link IBiopaxModel} wrapper
	 * @throws ReactionException
	 * @throws IOException 
	 */
    public static IBiopaxModel read(URL url) throws ReactionException, IOException {
        IBiopaxModel model = null;
        InputStream in = null;
        try {
            in = url.openStream();
            model = read(in);
        } catch (IOException e) {
            LOGGER.error("Unable to read from URL " + url, e);
        } finally {
            if (in != null) in.close();
        }
        return model;
    }
}
