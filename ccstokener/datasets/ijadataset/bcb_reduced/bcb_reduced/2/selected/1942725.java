package prajna.semantic.accessor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import prajna.data.DataRecord;
import prajna.data.TimeSpan;
import prajna.semantic.DataTemplate;
import prajna.semantic.FieldDesc;
import prajna.semantic.TimeFieldDesc;

/**
 * <P>
 * Data accessor for reading semi-structured XML files. This accessor creates
 * DataRecords based upon the record identifier for each data template. The
 * child elements within that record are stored as fields within the data
 * record. For instance, if the record identifier were <code>car</code>, the
 * following XML would create a data record with source fields for Make, Model,
 * and Year:
 * </P>
 * 
 * <pre>
 * &lt;car&gt;
 *     &lt;Make&gt;Honda&lt;/Make&gt;
 *     &lt;Model&gt;Odyssey&lt;/Model&gt;
 *     &lt;Year&gt;2012&lt;/Year&gt;
 * &lt;/car&gt;
 * </pre>
 * 
 * @author <a href="http://www.ganae.com/edswing">Edward Swing</a>
 */
public class XmlDataAccessor extends SemanticAccessor {

    private static DocumentBuilder docBuild;

    private HashMap<URL, Element> dataFiles = new HashMap<URL, Element>();

    private String query;

    static {
        try {
            docBuild = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException exc) {
            System.err.println("Cannot initialize XmlDataAccessor:");
            exc.printStackTrace();
        }
    }

    /**
     * Create a data record from a row of string data. Each data value is
     * mapped according to the column and the data template.
     * 
     * @param data the element containing the record data
     * @param template The data record template
     * @return A new data record which matches the data template. or null if
     *         the record cannot be matched to the template.
     */
    private DataRecord createDataRecord(Element data, DataTemplate template) {
        DataRecord rec = new DataRecord();
        String nameKey = template.getNameKey();
        if (nameKey != null) {
            String attName = data.getAttribute(nameKey);
            if (attName != null && attName.length() > 0) {
                rec.setName(attName);
            } else {
                NodeList nameList = data.getElementsByTagName(nameKey);
                if (nameList.getLength() == 1) {
                    Node txtNode = nameList.item(0).getFirstChild();
                    if (txtNode != null && txtNode instanceof Text) {
                        rec.setName(((Text) txtNode).getData().trim());
                    }
                }
            }
        }
        String queryField = null;
        String queryValue = null;
        if (query != null) {
            String[] vals = query.split("=");
            if (vals.length == 2) {
                queryField = vals[0].trim();
                queryValue = vals[1].trim();
            }
        }
        Collection<FieldDesc<?>> fieldDefs = template.getFieldDescriptions().values();
        for (FieldDesc<?> desc : fieldDefs) {
            String field = desc.getFieldName();
            FieldHandler handler = getFieldHandler(field);
            try {
                if (desc instanceof TimeFieldDesc && ((TimeFieldDesc) desc).isSpanField()) {
                    TimeFieldDesc tfDesc = (TimeFieldDesc) desc;
                    String startField = tfDesc.getSourceStartField();
                    String stopField = tfDesc.getSourceStopField();
                    String durField = tfDesc.getDurationField();
                    boolean isDur = (stopField == null);
                    String endField = (isDur) ? durField : stopField;
                    NodeList startList = data.getElementsByTagName(startField);
                    NodeList endList = data.getElementsByTagName(endField);
                    if (startList.getLength() != endList.getLength()) {
                        throw new IllegalStateException("Mismatch of time values for " + tfDesc);
                    }
                    for (int i = 0; i < startList.getLength(); i++) {
                        if (tfDesc.isMultiValue() || rec.getTimeField(field) == null) {
                            Node txtNode = ((Element) startList.item(i)).getFirstChild();
                            Node endNode = ((Element) endList.item(i)).getFirstChild();
                            if (txtNode instanceof Text && endNode instanceof Text) {
                                Text text = (Text) txtNode;
                                String value = text.getData().trim();
                                Text endText = (Text) endNode;
                                String endVal = endText.getData().trim();
                                TimeSpan span = tfDesc.parseValueIntoRecord(rec, value);
                                if (isDur) {
                                    tfDesc.parseDuration(span, endVal);
                                } else {
                                    tfDesc.parseStopTime(span, endVal);
                                }
                                if (handler != null) {
                                    handler.handleField(field, span.toString(), rec);
                                }
                            }
                        }
                    }
                } else {
                    for (String srcField : desc.getSourceFields()) {
                        NodeList nodeList = data.getElementsByTagName(srcField);
                        for (int i = 0; i < nodeList.getLength(); i++) {
                            if (desc.isMultiValue() || rec.getFieldAsString(field) == null) {
                                Element dataElem = (Element) nodeList.item(i);
                                Node txtNode = dataElem.getFirstChild();
                                if (txtNode instanceof Text) {
                                    Text text = (Text) txtNode;
                                    String value = text.getData().trim();
                                    if (value != null && value.length() > 0 && (query == null || !value.equals(queryValue) || !field.equals(queryField))) {
                                        desc.parseValueIntoRecord(rec, value);
                                        if (handler != null) {
                                            handler.handleField(field, value, rec);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (ParseException exc) {
                exc.printStackTrace();
            }
            if (rec.getFieldAsString(field) == null) {
                desc.storeDefaultIntoRecord(rec);
            }
        }
        return rec;
    }

    /**
     * Read the data file, parsing the data into rows.
     * 
     * @throws IOException
     * @throws SAXException
     */
    private Element readUrl(URL url) throws IOException, SAXException {
        InputStream inStream = url.openStream();
        Document doc = docBuild.parse(inStream);
        return doc.getDocumentElement();
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
        HashSet<DataRecord> dataSet = new HashSet<DataRecord>();
        for (Iterator<URL> urlIter = dataFiles.keySet().iterator(); urlIter.hasNext(); ) {
            URL dataFile = urlIter.next();
            Element rootElem = dataFiles.get(dataFile);
            if (rootElem == null) {
                try {
                    rootElem = readUrl(dataFile);
                    dataFiles.put(dataFile, rootElem);
                } catch (Exception exc) {
                    System.err.println("Error reading " + dataFile);
                    urlIter.remove();
                }
            }
            String recId = template.getRecordIdentifier();
            if (recId != null && rootElem != null) {
                NodeList nodeList = rootElem.getElementsByTagName(recId);
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Element elem = (Element) nodeList.item(i);
                    DataRecord dataRec = createDataRecord(elem, template);
                    if (dataRec != null) {
                        dataSet.add(dataRec);
                    }
                }
            }
        }
        return dataSet;
    }

    /**
     * Set any initialization parameters required by this data accessor
     * 
     * @param parameters a map of initialization parameters
     */
    public void setInitParameters(Map<String, String> parameters) {
        if (parameters.containsKey("dataFile")) {
            String path = parameters.get("dataFile");
            try {
                dataFiles.put(new URL(path), null);
            } catch (MalformedURLException exc) {
                File file = new File(path);
                try {
                    dataFiles.put(file.toURI().toURL(), null);
                } catch (MalformedURLException exc1) {
                    exc1.printStackTrace();
                }
            }
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
}
