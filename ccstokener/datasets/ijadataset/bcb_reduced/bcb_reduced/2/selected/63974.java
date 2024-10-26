package ru.ksu.niimm.cll.mocassin.crawl.analyzer.mapping.matchers.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

public class NameMatcherPropertiesLoaderImpl implements NameMatcherPropertiesLoader {

    private static final String OMDOC_ONTOLOGY_URI_PARAMETER_NAME = "ontology.uri";

    private static final String MATCH_CONCEPT_URIS_PARAMETER_NAME = "match.concept.uris";

    private static final String PROPERTIES_FILENAME = "matcher/name_matcher.properties";

    private Properties properties;

    private List<String> matchedConceptUris = new ArrayList<String>();

    public NameMatcherPropertiesLoaderImpl() throws IOException {
        properties = loadProperties();
        String conceptUris = get(MATCH_CONCEPT_URIS_PARAMETER_NAME).trim();
        String omdocOntologyUri = get(OMDOC_ONTOLOGY_URI_PARAMETER_NAME);
        StringTokenizer st = new StringTokenizer(conceptUris, ",");
        while (st.hasMoreTokens()) {
            matchedConceptUris.add(String.format("%s#%s", omdocOntologyUri, st.nextToken()));
        }
    }

    private final Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        ClassLoader loader = NameMatcherPropertiesLoaderImpl.class.getClassLoader();
        URL url = loader.getResource(PROPERTIES_FILENAME);
        InputStream stream = url.openStream();
        try {
            properties.load(stream);
        } finally {
            stream.close();
        }
        return properties;
    }

    @Override
    public String get(String key) {
        return getProperties().getProperty(key);
    }

    @Override
    public List<String> getMatchedURIs() {
        return this.matchedConceptUris;
    }

    protected String getPropertiesFilename() {
        return PROPERTIES_FILENAME;
    }

    private Properties getProperties() {
        return properties;
    }
}
