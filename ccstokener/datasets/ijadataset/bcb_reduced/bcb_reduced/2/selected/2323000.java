package org.gbif.ipt.model.factory;

import org.gbif.ipt.model.Vocabulary;
import org.gbif.ipt.model.VocabularyConcept;
import org.gbif.ipt.model.VocabularyTerm;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import com.google.inject.Inject;
import org.apache.commons.digester.Digester;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

/**
 * Building from XML definitions.
 */
public class VocabularyFactory {

    public static final String VOCABULARY_NAMESPACE = "http://rs.gbif.org/thesaurus/";

    private static final Logger LOG = Logger.getLogger(VocabularyFactory.class);

    private final HttpClient client;

    private final SAXParserFactory saxf;

    @Inject
    public VocabularyFactory(DefaultHttpClient httpClient, SAXParserFactory saxf) {
        this.client = httpClient;
        this.saxf = saxf;
    }

    /**
   * Builds a Vocabulary from the supplied input stream
   *
   * @param is For the XML
   *
   * @return The extension
   */
    public Vocabulary build(InputStream is) throws IOException, SAXException, ParserConfigurationException {
        Digester digester = new Digester(saxf.newSAXParser());
        digester.setNamespaceAware(true);
        digester.setXIncludeAware(false);
        digester.setRuleNamespaceURI(VOCABULARY_NAMESPACE);
        Vocabulary tv = new Vocabulary();
        digester.push(tv);
        digester.addCallMethod("*/thesaurus", "setTitle", 1);
        digester.addRule("*/thesaurus", new CallParamNoNSRule(0, "title"));
        digester.addCallMethod("*/thesaurus", "setDescription", 1);
        digester.addRule("*/thesaurus", new CallParamNoNSRule(0, "description"));
        digester.addCallMethod("*/thesaurus", "setLink", 1);
        digester.addRule("*/thesaurus", new CallParamNoNSRule(0, "relation"));
        digester.addCallMethod("*/thesaurus", "setUri", 1);
        digester.addRule("*/thesaurus", new CallParamNoNSRule(0, "URI"));
        digester.addObjectCreate("*/concept", VocabularyConcept.class);
        digester.addCallMethod("*/concept", "setLink", 1);
        digester.addRule("*/concept", new CallParamNoNSRule(0, "relation"));
        digester.addCallMethod("*/concept", "setDescription", 1);
        digester.addRule("*/concept", new CallParamNoNSRule(0, "description"));
        digester.addCallMethod("*/concept", "setUri", 1);
        digester.addRule("*/concept", new CallParamNoNSRule(0, "URI"));
        digester.addCallMethod("*/concept", "setIdentifier", 1);
        digester.addRule("*/concept", new CallParamNoNSRule(0, "identifier"));
        digester.addObjectCreate("*/preferred/term", VocabularyTerm.class);
        digester.addCallMethod("*/preferred/term", "setLang", 1);
        digester.addRule("*/preferred/term", new CallParamNoNSRule(0, "lang"));
        digester.addCallMethod("*/preferred/term", "setTitle", 1);
        digester.addRule("*/preferred/term", new CallParamNoNSRule(0, "title"));
        digester.addSetNext("*/preferred/term", "addPreferredTerm");
        digester.addObjectCreate("*/alternative/term", VocabularyTerm.class);
        digester.addCallMethod("*/alternative/term", "setLang", 1);
        digester.addRule("*/alternative/term", new CallParamNoNSRule(0, "lang"));
        digester.addCallMethod("*/alternative/term", "setTitle", 1);
        digester.addRule("*/alternative/term", new CallParamNoNSRule(0, "title"));
        digester.addSetNext("*/alternative/term", "addAlternativeTerm");
        digester.addSetNext("*/concept", "addConcept");
        digester.parse(is);
        return tv;
    }

    /**
   * @param url To build from
   *
   * @return The thesaurus or null on error
   */
    public Vocabulary build(String url) {
        HttpGet get = new HttpGet(url);
        try {
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream is = entity.getContent();
                try {
                    Vocabulary tv = build(is);
                    LOG.info("Successfully parsed Thesaurus: " + tv.getTitle());
                    return tv;
                } catch (SAXException e) {
                    LOG.error("Unable to parse XML for extension: " + e.getMessage(), e);
                } finally {
                    is.close();
                }
                entity.consumeContent();
            }
        } catch (Exception e) {
            LOG.error(e);
        }
        return null;
    }
}
