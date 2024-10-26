package org.mcisb.ontology.cath;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import org.mcisb.ontology.*;
import org.mcisb.util.*;

/**
 * 
 * @author Neil Swainston
 */
public class CathUtils extends OntologySource {

    /**
	 * 
	 */
    private static CathUtils utils = null;

    /**
	 * 
	 */
    private final Map<String, String> idToName = new HashMap<String, String>();

    /**
	 * 
	 * @return ChebiUtils
	 * @throws Exception
	 */
    public static synchronized CathUtils getInstance() throws Exception {
        if (utils == null) {
            utils = new CathUtils();
        }
        return utils;
    }

    /**
	 * 
	 * @throws Exception
	 */
    private CathUtils() throws Exception {
        super(Ontology.CATH);
        InputStream is = null;
        BufferedReader reader = null;
        try {
            final String CATH_REGEXP = OntologyFactory.getOntology(Ontology.CATH).getRegularExpression();
            final URL url = new URL("http://release.cathdb.info/v3.4.0/CathNames");
            is = url.openStream();
            reader = new BufferedReader(new InputStreamReader(is, Charset.defaultCharset()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                final String[] tokens = line.split("\\s+");
                if (RegularExpressionUtils.getMatches(tokens[0], CATH_REGEXP).size() > 0) {
                    idToName.put(tokens[0], line.substring(line.indexOf(':') + 1, line.length()));
                }
            }
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }
    }

    /**
	 * 
	 * @param id
	 * @return String
	 */
    private String getName(final String id) {
        return idToName.get(id);
    }

    @Override
    protected OntologyTerm getOntologyTermFromId(final String id) throws Exception {
        return new CathTerm(id, getName(id));
    }
}
