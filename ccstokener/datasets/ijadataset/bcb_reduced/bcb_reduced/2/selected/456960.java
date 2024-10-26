package prajna.semantic.accessor;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.*;
import prajna.data.*;
import prajna.semantic.*;
import prajna.util.Graph;
import prajna.util.Tree;

/**
 * Implementation of a SemanticAccessor which accesses its data from a Solr
 * instance. This class builds datasets, graphs, and trees based upon the
 * configuration parameters and Solr schemas.
 * 
 * @author <a href="http://www.ganae.com/edswing">Edward Swing</a>
 */
public class SolrAccessor extends SemanticAccessor {

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private String baseQueryUrl;

    private String nodeQuery = null;

    private String query;

    private static DocumentBuilder docBuild;

    static {
        try {
            docBuild = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException exc) {
            System.err.println("Cannot initialize SolrDataAccessor:");
            exc.printStackTrace();
        }
    }

    /**
     * Derive a data record for inclusion in a graph, tree, or other structure
     * based upon a referring node. This method is used when a data structure
     * contains a reference to a record which is not available. For instance,
     * this method is called when a graph contains an edge which refers to a
     * node which is not in the current set of graph nodes, and cannot be
     * retrieved from the underlying data source.
     * 
     * @param referrer the data record containing the reference to the node
     * @param name The name for the new Data Record. This should be one of the
     *            values of the dimension
     * @param desc The field descriptor for the field containing the reference.
     * @return A data record with the given name and a navigation link.
     */
    @Override
    protected DataRecord createReferenceNode(DataRecord referrer, String name, FieldDesc<?> desc) {
        DataRecord rec = new DataRecord(name);
        Set<String> srcFields = desc.getSourceFields();
        if (srcFields.size() == 1) {
            rec.setLink(srcFields.iterator().next() + ":\"" + name + "\"");
        } else {
            rec.setLink("\"" + name + "\"");
        }
        return rec;
    }

    /**
     * Create a Solr query from the provided template and the query. If the
     * query is null, this method simply returns the default query
     * <code>*:*</code>. Otherwise, this method checks the various field
     * descriptors in the template and derives a Solr query based upon the
     * query string.
     * 
     * @param template The data template specifying field mappings and
     *            descriptions
     * @return a Solr formatted query
     */
    private String createSolrQuery(DataTemplate template) {
        String solrQuery = "*:*";
        if (query != null && nodeQuery == null) {
            Collection<FieldDesc<?>> fieldDefs = template.getFieldDescriptions().values();
            String[] vals = query.split("=");
            if (vals.length == 2) {
                String queryField = vals[0].trim();
                String queryValue = vals[1].trim();
                for (FieldDesc<?> desc : fieldDefs) {
                    if (desc.getFieldName().equals(queryField)) {
                        Set<String> srcFields = desc.getSourceFields();
                        if (srcFields.size() == 1) {
                            solrQuery = srcFields.iterator().next() + ":" + queryValue;
                        } else {
                        }
                    }
                }
            }
        } else if (nodeQuery != null) {
            solrQuery = nodeQuery;
        }
        return solrQuery;
    }

    /**
     * Extend the given graph around the specified node. If the graphName is
     * empty or null, and there is only one graph defined, that graph will be
     * used by default.
     * 
     * @param graphName The name of the graph
     * @param graph The graph to extend
     * @param node The node to use as an extension point.
     * @return true if more data was added to the graph, false otherwise.
     */
    @Override
    public boolean extendGraph(String graphName, Graph<DataRecord, DataRecord> graph, DataRecord node) {
        if (graphName == null || graphName.length() == 0) {
            Set<String> names = getGraphNames();
            if (names.size() == 1) {
                graphName = names.iterator().next();
            }
        }
        nodeQuery = node.getLink();
        if (nodeQuery == null) {
            nodeQuery = "\"" + node.getName() + "\"";
        }
        GraphSpec spec = (GraphSpec) getSpec(graphName);
        int nodeSize = graph.order();
        int edgeSize = graph.size();
        if (spec != null) {
            addToGraph(graph, spec);
        }
        nodeQuery = null;
        return (nodeSize < graph.order() || edgeSize < graph.size());
    }

    /**
     * Extend the given tree around the specified node. This method adds any
     * children which are not already part of the tree. The default
     * implementation simply returns false. SemanticAccessors which can extend
     * the graph should override this method.
     * 
     * @param treeName The name of the graph
     * @param tree The graph to extend
     * @param node The node to use as an extension point.
     * @return true if more data was added to the tree, false otherwise.
     */
    @Override
    public boolean extendTree(String treeName, Tree<DataRecord> tree, DataRecord node) {
        if (treeName == null || treeName.length() == 0) {
            Set<String> names = getTreeNames();
            if (names.size() == 1) {
                treeName = names.iterator().next();
            }
        }
        int size = tree.size();
        TreeSpec spec = (TreeSpec) getSpec(treeName);
        if (spec != null) {
            nodeQuery = (spec.getParentField() == null) ? node.getName() : spec.getParentField() + ":" + node.getName();
            tree = addToTree(tree, spec);
        }
        nodeQuery = null;
        return (tree.size() != size);
    }

    /**
     * Get the set of String values from a Solr XML element.
     * 
     * @param elem the element containing the values
     * @return the set of values
     */
    private List<String> getFieldValues(Element elem) {
        ArrayList<String> values = new ArrayList<String>();
        if (elem != null) {
            if (elem.getTagName().equals("arr")) {
                NodeList children = elem.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Element child = (Element) children.item(i);
                    String value = child.getTextContent();
                    values.add(value);
                }
            } else {
                String value = elem.getTextContent();
                values.add(value);
            }
        }
        return values;
    }

    /**
     * Parse the time field from the Solr response elements. This method
     * retrieves the values for the start element (corresponding to the
     * sourceStartField) and the end element (corresponding to either the
     * sourceStopField or the durationField). It attempts to construct time
     * spans from the data and store them in the data record provided.
     * 
     * @param desc the field descriptor for the time field
     * @param rec the data record to be loaded
     * @param startElem the start DOM element containing the start times
     * @param endElem the end DOM element containing either the stop times or
     *            durations)
     */
    private void parseTimeField(TimeFieldDesc desc, DataRecord rec, Element startElem, Element endElem) {
        List<String> startVals = getFieldValues(startElem);
        List<Date> startDates = new ArrayList<Date>();
        for (String start : startVals) {
            try {
                Date startDate = desc.parseDate(start);
                startDates.add(startDate);
            } catch (ParseException exc) {
                try {
                    Date startDate = dateFormat.parse(start);
                    startDates.add(startDate);
                } catch (ParseException exc1) {
                    throw new RuntimeException("Cannot parse: " + start, exc);
                }
            }
        }
        List<String> endVals = getFieldValues(endElem);
        if (desc.getDurationField() != null) {
            if (endVals.size() != startDates.size()) {
                if (endVals.size() > 0) {
                    throw new IllegalStateException("Durations do not equal start dates");
                }
                for (Date startDate : startDates) {
                    storeTimeSpan(new TimeSpan(startDate), rec, desc);
                }
            } else {
                for (int i = 0; i < endVals.size(); i++) {
                    String durString = endVals.get(i);
                    Date startDate = startDates.get(i);
                    TimeSpan span = desc.parseDuration(startDate, durString);
                    storeTimeSpan(span, rec, desc);
                }
            }
        } else {
            ArrayList<Date> stopDates = new ArrayList<Date>();
            for (String stop : endVals) {
                try {
                    Date stopDate = desc.parseDate(stop);
                    stopDates.add(stopDate);
                } catch (ParseException exc) {
                    try {
                        Date startDate = dateFormat.parse(stop);
                        stopDates.add(startDate);
                    } catch (ParseException exc1) {
                        throw new RuntimeException("Cannot parse: " + stop, exc);
                    }
                }
            }
            TimeSequence spans = new TimeSequence(startDates, stopDates);
            for (TimeSpan span : spans.getTimeSpans()) {
                storeTimeSpan(span, rec, desc);
            }
        }
    }

    /**
     * Send a query to the Solr engine, and return the set of <code>doc</code>
     * elements. This method will attach the query to the baseQueryUrl, and
     * UrlEncode the entire query string.
     * 
     * @param solrQuery the Solr query.
     * @return a set of DOM elements containing the records returned from the
     *         Solr query.
     */
    private Set<Element> querySolrEngine(String solrQuery) {
        HashSet<Element> elements = new HashSet<Element>();
        try {
            String encoded = baseQueryUrl + "/select?rows=" + getMaxRecords() + "&q=" + URLEncoder.encode(solrQuery, "UTF-8");
            URL url = new URL(encoded);
            InputStream inStream = url.openStream();
            Document doc = docBuild.parse(inStream);
            Element root = doc.getDocumentElement();
            NodeList recList = root.getElementsByTagName("doc");
            for (int i = 0; i < recList.getLength(); i++) {
                Element elem = (Element) recList.item(i);
                elements.add(elem);
            }
            inStream.close();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        return elements;
    }

    /**
     * Read the fields from the schema to create a default template. This
     * method will create a data template based upon the Solr schema. It will
     * also create a DatasetSpec with the template created.
     */
    private void readFieldsFromSchema() {
        try {
            URL schemaUrl = new URL(baseQueryUrl + "/admin/file/?file=schema.xml");
            Document doc = docBuild.parse(schemaUrl.openStream());
            NodeList fieldElems = doc.getElementsByTagName("field");
            Set<FieldDesc<?>> fieldDescs = new HashSet<FieldDesc<?>>();
            for (int i = 0; i < fieldElems.getLength(); i++) {
                Element elem = (Element) fieldElems.item(i);
                String name = elem.getAttribute("name");
                String type = elem.getAttribute("type");
                String multi = elem.getAttribute("multiValued");
                boolean multiFlag = (multi != null && Boolean.parseBoolean(multi));
                FieldDesc<?> desc = null;
                if (type.equals("integer") || type.equals("long") || type.equals("sint") || type.equals("slong")) {
                    desc = new IntFieldDesc(name);
                } else if (type.equals("float") || type.equals("double") || type.equals("sfloat") || type.equals("sdouble")) {
                    desc = new MeasureFieldDesc(name, Unitless.RATIO);
                } else if (type.equals("date")) {
                    desc = new TimeFieldDesc(name, dateFormat.toPattern());
                } else if (type.equals("boolean")) {
                    HashSet<String> boolSet = new HashSet<String>();
                    boolSet.add("true");
                    boolSet.add("false");
                    desc = new EnumFieldDesc(name, boolSet);
                } else {
                    desc = new TextFieldDesc(name);
                }
                desc.setMultiValue(multiFlag);
                desc.addSourceField(name);
                fieldDescs.add(desc);
            }
            DataTemplate template = new DataTemplate(fieldDescs);
            template.setName("schema");
            NodeList keys = doc.getElementsByTagName("uniqueKey");
            if (keys.getLength() > 0) {
                Element elem = (Element) keys.item(0);
                Text txt = (Text) elem.getFirstChild();
                template.addNameKey(txt.getData());
            }
            DatasetSpec spec = new DatasetSpec("rawData");
            spec.addTemplate("schema");
            addTemplate(template);
            addStructure(spec);
        } catch (Exception exc) {
            throw new RuntimeException("Cannot parse schema to determine data format at " + baseQueryUrl);
        }
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
        HashSet<DataRecord> records = new HashSet<DataRecord>();
        String solrQuery = createSolrQuery(template);
        Set<Element> elements = querySolrEngine(solrQuery);
        Collection<FieldDesc<?>> fieldDefs = template.getFieldDescriptions().values();
        HashMap<String, Element> fieldElems = new HashMap<String, Element>();
        for (Element elem : elements) {
            fieldElems.clear();
            NodeList children = elem.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i) instanceof Element) {
                    Element fieldElem = (Element) children.item(i);
                    String name = fieldElem.getAttribute("name");
                    if (name != null && name.length() > 0) {
                        fieldElems.put(name, fieldElem);
                    }
                }
            }
            Element nameElem = fieldElems.get(template.getNameKey());
            List<String> values = getFieldValues(nameElem);
            if (values.size() != 1) {
                throw new IllegalStateException("Multiple names for name field " + template.getNameKey() + ": " + values);
            }
            String name = values.iterator().next();
            DataRecord rec = new DataRecord(name);
            boolean valid = true;
            String recIdField = template.getRecordIdentifier();
            if (recIdField != null) {
                Element idElem = fieldElems.get(recIdField);
                values = getFieldValues(idElem);
                if (values.size() != 1) {
                    throw new IllegalStateException("Specified record ID field (" + recIdField + ") does not match schema");
                }
                String recId = values.iterator().next();
                rec.setLink(baseQueryUrl + "/select?q=" + recIdField + "%3A" + recId);
            }
            for (FieldDesc<?> desc : fieldDefs) {
                String field = desc.getFieldName();
                FieldHandler handler = getFieldHandler(field);
                if (desc instanceof TimeFieldDesc && ((TimeFieldDesc) desc).isSpanField()) {
                    TimeFieldDesc timeDesc = (TimeFieldDesc) desc;
                    Element startElem = fieldElems.get(timeDesc.getSourceStartField());
                    Element endElem = (timeDesc.getSourceStopField() != null) ? fieldElems.get(timeDesc.getSourceStopField()) : fieldElems.get(timeDesc.getDurationField());
                    parseTimeField(timeDesc, rec, startElem, endElem);
                } else {
                    Set<String> srcFields = desc.getSourceFields();
                    for (String srcField : srcFields) {
                        Element fieldElem = fieldElems.get(srcField);
                        List<String> vals = getFieldValues(fieldElem);
                        for (String value : vals) {
                            if (desc.isMultiValue() || rec.getFieldAsString(field) == null) {
                                try {
                                    desc.parseValueIntoRecord(rec, value);
                                    if (handler != null) {
                                        handler.handleField(field, value, rec);
                                    }
                                } catch (ParseException exc) {
                                    valid = false;
                                }
                            }
                        }
                    }
                }
                if (rec.getFieldAsString(field) == null) {
                    desc.storeDefaultIntoRecord(rec);
                }
            }
            if (valid) {
                records.add(rec);
            }
        }
        return records;
    }

    /**
     * <P>
     * Set any initialization parameters required by this data accessor. The
     * key for the only parameters are <code>solrQueryUrl</code>, which should
     * contain the base URL for Solr queries, and the optional
     * <code>defaultQuery</code>, which indicates the default query for the
     * accessor. If it is not set, the query matching all records is used.
     * </P>
     * <P>
     * If this accessor has not been initialized with data templates and data
     * structure specifications, this method will try to read the schema from
     * the Solr instance, and create a default data set representation from the
     * schema.
     * </P>
     * 
     * @param parameters a map of initialization parameters
     */
    public void setInitParameters(Map<String, String> parameters) {
        baseQueryUrl = parameters.get("solrQueryUrl").trim();
        String defQuery = parameters.get("defaultQuery");
        if (query != null) {
            setQuery(defQuery);
        }
        if (getTemplateNames().isEmpty()) {
            readFieldsFromSchema();
        }
    }

    /**
     * Set the query string used to retrieve records. This method should
     * transform the query string into a format which the accessor can parse.
     * The query string supports a query of the form <b>field=value</b>.
     * 
     * @param queryString the query string
     */
    @Override
    public void setQuery(String queryString) {
        query = queryString;
    }

    /**
     * Validate the time span in the data record, and store it in the
     * appropriate field. Trigger any field handlers on the time span.
     * 
     * @param span The time span
     * @param rec The data record to store the field in
     * @param desc the TimeFieldDescriptor which indicates the field
     * @return true if the span was valid, false otherwise
     */
    private boolean storeTimeSpan(TimeSpan span, DataRecord rec, TimeFieldDesc desc) {
        String field = desc.getFieldName();
        boolean valid = desc.isValid(span);
        if (valid && (desc.isMultiValue() || rec.getTimeField(field) == null)) {
            rec.addTimeFieldValue(desc.getFieldName(), span);
            FieldHandler handler = getFieldHandler(desc.getFieldName());
            if (handler != null) {
                handler.handleField(desc.getFieldName(), span.toString(), rec);
            }
        }
        return valid;
    }
}
