package org.ontospread.gui.demo;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.log4j.Logger;
import org.ontospread.dao.JenaOntologyDAOImpl;
import org.ontospread.exceptions.ConceptNotFoundException;
import org.ontospread.gui.utils.DAOTransformerFactory;
import org.ontospread.gui.utils.FileDAOTransformerFactory;
import org.ontospread.model.loader.OntoSpreadModelWrapper;
import org.ontospread.to.ConceptTO;
import org.ontospread.xmlbind.Concept;
import org.ontospread.xmlbind.utils.ConceptXMLBind;

public class WebOntologyDAOImpl extends JenaOntologyDAOImpl {

    private static final Logger logger = Logger.getLogger(WebOntologyDAOImpl.class);

    private static final String keyTemplates = "resources/dbpedia.xsl";

    private static final int MAX_CACHE = 2048;

    private DAOTransformerFactory templates = new FileDAOTransformerFactory();

    public HashMap<String, Concept> cache = new HashMap<String, Concept>();

    public WebOntologyDAOImpl(OntoSpreadModelWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Concept getConcept(String conceptUri, String contextUri) throws ConceptNotFoundException {
        return fetchDataNeeded(conceptUri);
    }

    private Concept fetchDataNeeded(String conceptUri) {
        if (cache.size() > MAX_CACHE) cache.clear();
        if (cache.containsKey(conceptUri)) return this.cache.get(conceptUri);
        try {
            URL url = new URL(conceptUri);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Accept", "application/rdf+xml");
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK && connection.getContentType().contains("application/rdf+xml")) {
                InputStream is = connection.getInputStream();
                HashMap<String, String> parameters = new HashMap<String, String>();
                parameters.put("uri", conceptUri);
                Transformer tf = this.templates.getDAOTransformer(keyTemplates, parameters);
                DOMResult outputTarget = new DOMResult();
                tf.transform(new StreamSource(is), outputTarget);
                Concept concept = ConceptXMLBind.getInstance().restoreConcept(outputTarget.getNode());
                this.cache.put(conceptUri, concept);
                return concept;
            } else {
                logger.error("Unable to get a representation of the resource: " + connection.getResponseCode() + " => " + connection.getContentType());
                throw new RuntimeException("Unable to get a representation of the resource");
            }
        } catch (Exception e) {
            logger.error("Unable to fetch data for concept " + conceptUri, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public ConceptTO getConceptTO(String conceptUri) throws ConceptNotFoundException {
        Concept c = getConcept(conceptUri, null);
        ConceptTO conceptTO = new ConceptTO();
        conceptTO.setUri(c.getConceptDescription().getUri());
        conceptTO.setDescription(c.getConceptDescription().getDescription());
        conceptTO.setName(c.getConceptDescription().getName());
        return conceptTO;
    }
}
