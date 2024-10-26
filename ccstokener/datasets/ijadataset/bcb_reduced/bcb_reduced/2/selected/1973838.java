package prajna.semantic.accessor;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import prajna.data.*;
import prajna.semantic.DataTemplate;
import prajna.semantic.FieldDesc;
import prajna.semantic.TimeFieldDesc;
import prajna.text.StringUtils;

/**
 * Accessor which uses Dbpedia to retrieve information. This method uses a few
 * pre-constructed SPARQL queries to retrieve information from the Dbpedia RDF
 * store. It parses the information, and assembles the records into DataRecord
 * items, and builds trees, graphs, and grids from the resulting data. It can
 * query groups of objects by RDF type or by the SKOS subject.
 * 
 * @author <a href="http://www.ganae.com/edswing">Edward Swing</a>
 */
public class DbpediaAccessor extends SemanticAccessor {

    private String lang = "en";

    private String topic = null;

    private static String queryBase = "http://dbpedia.org/snorql?default-graph-uri=http%3A%2F%2Fdbpedia.org&stylesheet=xml-to-html.xsl&query=";

    private static String prefix = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" + "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" + "PREFIX : <http://dbpedia.org/resource/>\n" + "PREFIX dbpedia2: <http://dbpedia.org/property/>\n" + "PREFIX dbpedia: <http://dbpedia.org/>\n" + "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n";

    private static DocumentBuilder docBuild;

    static {
        try {
            docBuild = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException exc) {
            System.err.println("Cannot initialize DbpediaAccessor:");
            exc.printStackTrace();
        }
    }

    /**
     * Parse the document for a particular resource into a DataRecord according
     * to the provided template. The document is an HTML table, with rows
     * indicating the property and value.
     * 
     * @param resMap the mapping of properties and values from Dbpedia
     * @param template the data template to use for creating the DataRecord
     * @return a new data record for the resource
     */
    private DataRecord parseResource(MultiValueMap<String> resMap, DataTemplate template) {
        DataRecord rec = null;
        if (template != null) {
            String name = resMap.getField(template.getNameKey());
            Collection<FieldDesc<?>> fieldDefs = template.getFieldDescriptions().values();
            if (name != null) {
                rec = new DataRecord(name);
                boolean valid = true;
                for (FieldDesc<?> desc : fieldDefs) {
                    String field = desc.getFieldName();
                    FieldHandler handler = getFieldHandler(field);
                    if (desc instanceof TimeFieldDesc && ((TimeFieldDesc) desc).isSpanField()) {
                        TimeFieldDesc timeDesc = (TimeFieldDesc) desc;
                        Set<String> starts = resMap.get(timeDesc.getSourceStartField());
                        Set<String> stops = resMap.get(timeDesc.getSourceStopField());
                        Set<TimeSpan> spans = parseTimes(starts, stops, timeDesc);
                        for (TimeSpan span : spans) {
                            valid = timeDesc.isValid(span);
                            if (valid && (desc.isMultiValue() || rec.getFieldAsString(field) == null)) {
                                rec.addTimeFieldValue(field, span);
                                if (handler != null) {
                                    handler.handleField(field, span.toString(), rec);
                                }
                            }
                        }
                    } else {
                        for (String srcField : desc.getSourceFields()) {
                            Set<String> vals = resMap.get(srcField);
                            if (vals != null) {
                                for (String value : vals) {
                                    if (desc.isMultiValue() || rec.getFieldAsString(field) == null) {
                                        String val = desc.applyTransform(value);
                                        try {
                                            desc.parseValueIntoRecord(rec, val);
                                            if (handler != null) {
                                                handler.handleField(field, val, rec);
                                            }
                                        } catch (Exception exc) {
                                            valid = false;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (rec.getFieldAsString(field) == null) {
                        desc.storeDefaultIntoRecord(rec);
                    }
                }
            }
        } else {
            rec = new DataRecord();
            for (String key : resMap.keySet()) {
                Set<String> vals = resMap.get(key);
                for (String val : vals) {
                    rec.addTextFieldValue(key, val);
                }
            }
        }
        return rec;
    }

    /**
     * Parse a set of times for a particular field descriptor. This method
     * scans the set of start and stop strings, determines whether the stop
     * values should be considered to be durations or stop date-times, and
     * creates a set of time spans for the data provided
     * 
     * @param starts the set of start times, as date-time strings
     * @param stops the set of stop times, either date-time strings or duration
     *            strings
     * @param timeDesc the set of time spans computed
     * @return the set of time spans
     */
    private Set<TimeSpan> parseTimes(Set<String> startVals, Set<String> stopVals, TimeFieldDesc desc) {
        ArrayList<Date> startDates = new ArrayList<Date>();
        Set<TimeSpan> spans = new HashSet<TimeSpan>();
        for (String start : startVals) {
            String val = desc.applyTransform(start);
            try {
                Date startDate = desc.parseDate(val);
                startDates.add(startDate);
            } catch (ParseException exc) {
                throw new RuntimeException("Cannot parse: " + val, exc);
            }
        }
        if (desc.getDurationField() != null) {
            if (stopVals.size() > 1 || stopVals.size() == 1 && startDates.size() > 1) {
                throw new IllegalStateException("Cannot match durations to start times");
            }
            Date startDate = startDates.get(0);
            String durString = stopVals.iterator().next();
            String val = desc.applyTransform(durString);
            TimeSpan span = desc.parseDuration(startDate, val);
            spans.add(span);
        } else {
            ArrayList<Date> stopDates = new ArrayList<Date>();
            for (String stop : stopVals) {
                String val = desc.applyTransform(stop);
                try {
                    Date stopDate = desc.parseDate(val);
                    stopDates.add(stopDate);
                } catch (ParseException exc) {
                    throw new RuntimeException("Cannot parse: " + val, exc);
                }
            }
            spans = new TimeSequence(startDates, stopDates).getTimeSpans();
        }
        return spans;
    }

    /**
     * Query Dbpedia for a particular resource. The resource name should
     * include the full dbpedia resource specification for the resource.
     * 
     * @param resourceName the resource reference
     * @return a map containing the properties and values of the reference
     */
    public MultiValueMap<String> queryResource(String resourceName) {
        if (resourceName.startsWith("http://dbpedia.org/resource/")) {
            resourceName = resourceName.substring(28);
        }
        try {
            resourceName = resourceName.replace(' ', '_');
            resourceName = URLEncoder.encode(resourceName, "UTF-8");
        } catch (UnsupportedEncodingException exc) {
        }
        String select = prefix + " SELECT ?property ?hasValue WHERE { { " + "<http://dbpedia.org/resource/" + resourceName + "> ?property ?hasValue  } FILTER (lang(?hasValue) = \"" + lang + "\" || !isLiteral(?hasValue))}";
        System.out.println(select);
        MultiValueMap<String> resourceMap = new MultiValueMap<String>();
        try {
            URL url = new URL(queryBase + URLEncoder.encode(select, "UTF-8"));
            InputStream inStream = url.openStream();
            Document doc = docBuild.parse(inStream);
            Element table = doc.getDocumentElement();
            NodeList rows = table.getElementsByTagName("tr");
            for (int i = 0; i < rows.getLength(); i++) {
                Element row = (Element) rows.item(i);
                NodeList cols = row.getElementsByTagName("td");
                if (cols.getLength() > 1) {
                    Element propElem = (Element) cols.item(0);
                    Element valElem = (Element) cols.item(1);
                    String property = ((Text) propElem.getFirstChild()).getData();
                    if (property.startsWith("http://dbpedia.org/property/")) {
                        property = property.substring(28);
                    } else {
                        int inx = property.indexOf('#');
                        if (inx == -1) {
                            inx = property.lastIndexOf('/');
                        }
                        property = property.substring(inx + 1);
                    }
                    String value = ((Text) valElem.getFirstChild()).getData();
                    if (value.startsWith("http://dbpedia.org/resource/")) {
                        value = value.substring(28).replaceAll("_", " ");
                    }
                    resourceMap.addFieldValue(property, value);
                }
            }
        } catch (UnsupportedEncodingException exc) {
            exc.printStackTrace();
        } catch (IOException exc) {
            System.err.println("Cannot retrieve record for " + resourceName);
        } catch (SAXException exc) {
            System.err.println("Cannot parse record for " + resourceName);
        }
        return resourceMap;
    }

    /**
     * Query Dbpedia for a particular resource and property. The resource name
     * and property name should include the full dbpedia resource specification
     * for the resource. The resourceName is the subject, the propertyName is
     * the Predicate. The matching Objects are returned
     * 
     * @param resourceName the resource reference - the Subject
     * @param propertyName the property value - the Predicate
     * @return a set containing the values (Objects) from Dbpedia's RDF store
     */
    public HashSet<String> queryResource(String resourceName, String propertyName) {
        if (resourceName.startsWith("http://dbpedia.org/resource/")) {
            resourceName = resourceName.substring(28);
        }
        try {
            resourceName = resourceName.trim().replace(' ', '_');
            resourceName = URLEncoder.encode(resourceName, "UTF-8");
        } catch (UnsupportedEncodingException exc) {
        }
        String select = prefix + " SELECT ?hasValue WHERE { { " + "<http://dbpedia.org/resource/" + resourceName + "> " + propertyName + " ?hasValue  } FILTER (lang(?hasValue) = \"" + lang + "\" || !isLiteral(?hasValue))}";
        System.out.println(select);
        HashSet<String> values = new HashSet<String>();
        try {
            URL url = new URL(queryBase + URLEncoder.encode(select, "UTF-8"));
            InputStream inStream = url.openStream();
            Document doc = docBuild.parse(inStream);
            Element table = doc.getDocumentElement();
            NodeList rows = table.getElementsByTagName("tr");
            for (int i = 0; i < rows.getLength(); i++) {
                Element row = (Element) rows.item(i);
                NodeList cols = row.getElementsByTagName("td");
                if (cols.getLength() > 0) {
                    Element valElem = (Element) cols.item(0);
                    String value = ((Text) valElem.getFirstChild()).getData();
                    if (value.startsWith("http://dbpedia.org/resource/")) {
                        value = value.substring(28).replaceAll("_", " ");
                    } else if (value.startsWith("http://dbpedia.org/ontology/")) {
                        value = value.substring(28).replaceAll("_", " ");
                    } else if (value.startsWith("http://dbpedia.org/class/yago/")) {
                        value = value.substring(30);
                        value = value.split("[\\d]+")[0];
                    }
                    values.add(value);
                }
            }
        } catch (UnsupportedEncodingException exc) {
            exc.printStackTrace();
        } catch (IOException exc) {
            System.err.println("Cannot retrieve record for " + resourceName);
        } catch (SAXException exc) {
            System.err.println("Cannot parse record for " + resourceName);
        }
        return values;
    }

    /**
     * Retrieve a set of data records for a particular DataTemplate. This
     * method is used by the various get<I>Structure</i> methods.
     * 
     * @param template The data template specifying field mappings and
     *            descriptions
     * @return the set of DataRecords for the template
     */
    @Override
    protected Set<DataRecord> retrieveRecords(DataTemplate template) {
        String query = template.getQuery();
        if (query == null) {
            query = topic;
        }
        String select = prefix + " SELECT ?resource WHERE { { ?resource rdf:type " + "<http://dbpedia.org/class/yago/" + StringUtils.toCamelCase(query) + "> } UNION { ?resource skos:subject <http://dbpedia.org/resource/Category:" + query.replaceAll(" ", "_") + "> } }";
        Document doc = null;
        HashSet<DataRecord> recs = new HashSet<DataRecord>();
        try {
            URL url = new URL(queryBase + URLEncoder.encode(select, "UTF-8"));
            InputStream inStream = url.openStream();
            doc = docBuild.parse(inStream);
            HashSet<String> resourceNames = new HashSet<String>();
            Element table = doc.getDocumentElement();
            NodeList rows = table.getElementsByTagName("tr");
            for (int i = 0; i < rows.getLength(); i++) {
                Element row = (Element) rows.item(i);
                NodeList cols = row.getElementsByTagName("td");
                if (cols.getLength() > 0) {
                    Element elem = (Element) cols.item(0);
                    String resource = ((Text) elem.getFirstChild()).getData();
                    resourceNames.add(resource);
                }
            }
            inStream.close();
            for (String resource : resourceNames) {
                MultiValueMap<String> resRecord = queryResource(resource);
                if (resource != null) {
                    DataRecord rec = parseResource(resRecord, template);
                    if (rec != null) {
                        recs.add(rec);
                    }
                }
            }
        } catch (IOException exc) {
            exc.printStackTrace();
        } catch (SAXException exc) {
            exc.printStackTrace();
        }
        return recs;
    }

    /**
     * Set any initialization parameters required by this data accessor. The
     * key for the only parameters are <code>query</code>, which should contain
     * the type query for Dbpedia, and the optional <code>language</code>
     * parameter, which indicates the language used by the accessor. The
     * default query should be in Solr format. If the language is not set, the
     * accessor uses English.
     * 
     * @param parameters a map of initialization parameters
     */
    public void setInitParameters(Map<String, String> parameters) {
        String language = parameters.get("language");
        lang = (language != null && language.length() > 0) ? language : "en";
        topic = parameters.get("query");
    }

    /**
     * Set the language used by the accessor. The language string should be one
     * of the standardized 2-character language strings.
     * 
     * @param language the specifier for the language
     */
    public void setLanguage(String language) {
        lang = language;
    }

    /**
     * Set the query used to identify records from Dbpedia. If the query string
     * is of the form <b>field=value</b>, the Otherwise, the internal topic is
     * set to the query. The topic should be something matching either the skos
     * subject field, or the rdf type. The query will be converted to camel
     * notation for RDF, and any spaces will be converted to underscores for
     * skos subjects.
     * 
     * @param query the query string
     */
    @Override
    public void setQuery(String query) {
        if (query.indexOf('=') != -1) {
        } else {
            topic = query;
        }
    }
}
