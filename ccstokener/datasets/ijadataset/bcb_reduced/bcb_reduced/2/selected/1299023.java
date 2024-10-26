package mwt.xml.xdbforms.xformlayer.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import mwt.xml.xdbforms.schemalayer.SchemaDocument;
import mwt.xml.xdbforms.schemalayer.exception.SchemaQueryException;
import mwt.xml.xdbforms.xformlayer.XFormDocument;
import mwt.xml.xdbforms.xformlayer.XFormErrorListener;
import mwt.xml.xdbforms.xformlayer.XFormNameSpaceResolver;
import mwt.xml.xdbforms.xformlayer.XFormTransformer;
import mwt.xml.xdbforms.xformlayer.exception.XFormDocumentException;
import mwt.xml.xdbforms.xformlayer.exception.XFormTransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Progetto Master Web Technology
 * @author Gianfranco Murador, Cristian Castiglia, Matteo Ferri
 * Copyright (C) 2009 MCG08 Group
 */
public class XFormDocumentImpl implements XFormDocument {

    private Document xformDocument = null;

    private XFormNameSpaceResolver xfnsr = null;

    private Document dataModelDoc = null;

    private Document valueDoc = null;

    private Map<String, String> parameters;

    /***
     **/
    public XFormDocumentImpl(Document xformDocument, XFormNameSpaceResolver nr) {
        this(nr);
        this.xformDocument = xformDocument;
    }

    public XFormDocumentImpl(XFormNameSpaceResolver nr) {
        xfnsr = nr;
        parameters = new HashMap<String, String>();
    }

    /**
     * 
     * @param parameter
     * @param output
     * @throws mwt.xml.xdbforms.xformlayer.exception.XFormDocumentException
     */
    public void buildPresentation(Map<String, String> parameter, OutputStream output) throws XFormDocumentException {
        parameters.putAll(parameter);
        if (dataModelDoc == null) {
            throw new XFormDocumentException("Error, please create a data model first");
        }
        String xsltPath = "mwt/xml/xdbforms/xformlayer/xslt/xdbforms.xsl";
        @SuppressWarnings("static-access") URL url = Thread.currentThread().getContextClassLoader().getResource(xsltPath);
        XFormErrorListener xfel = new XFormErrorListener();
        XFormTransformer xft = new XFormsTransformerImpl(parameters, xfel, output);
        try {
            InputStream stream = url.openStream();
            xft.transform(new StreamSource(stream), new DOMSource(dataModelDoc));
        } catch (XFormTransformerException ex) {
            throw new XFormDocumentException(ex);
        } catch (IOException ioe) {
            throw new XFormDocumentException(ioe);
        }
    }

    /**
     * Costruisce il modello dati
     * @param sd
     * @throws mwt.xml.xdbforms.xformlayer.exception.XFormDocumentException
     */
    public void buildDataModel(SchemaDocument sd, String schemaURL) throws XFormDocumentException {
        String content = null;
        try {
            String[] columns = sd.getAllColumnsName();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            dataModelDoc = db.newDocument();
            valueDoc = db.newDocument();
            StringBuilder elemParamString = new StringBuilder();
            Element data = dataModelDoc.createElement("data");
            data.setAttribute("xmlns:xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
            data.setAttribute("xsi:noNamespaceSchemaLocation", schemaURL);
            dataModelDoc.appendChild(data);
            for (int i = 0; i < columns.length; i++) {
                String colname = columns[i];
                Element e = dataModelDoc.createElement(colname);
                data.appendChild(e);
                if ((content = sd.getDafaultValue(colname)) != null) {
                    e.setTextContent(content);
                }
                if (sd.getAutoincrementTypeName(colname) != null) {
                    System.out.println("COLNAME ---> " + colname);
                    e.setAttribute("isAutoincrement", "true");
                    e.setTextContent("0");
                }
                elemParamString.append(colname + " ");
            }
            String elems = elemParamString.toString();
            parameters.put("data-elements", elems.trim());
            elems = null;
            elemParamString = null;
            String[] typeNames = sd.getAllColumnsType();
            StringBuilder typeParamString = new StringBuilder();
            String foreingTable;
            Element value;
            Element item, label;
            Element code_table = null;
            Element code_tables = valueDoc.createElement("values-fields");
            valueDoc.appendChild(code_tables);
            String foreingColName;
            for (int i = 0; i < typeNames.length; i++) {
                String typeName = typeNames[i];
                String colName = columns[i];
                typeParamString.append(typeName + " ");
                if (typeName.equals("SET")) {
                    String setVal[] = sd.getSetValues(colName);
                    code_table = valueDoc.createElement("code-table");
                    Element code_table_id = valueDoc.createElement("code-table-id");
                    code_table_id.setTextContent(colName);
                    code_table.appendChild(code_table_id);
                    for (int j = 0; j < setVal.length; j++) {
                        String val = setVal[j];
                        item = valueDoc.createElement("item");
                        value = valueDoc.createElement("value");
                        label = valueDoc.createElement("label");
                        label.setTextContent(val);
                        value.setTextContent(val);
                        item.appendChild(value);
                        item.appendChild(label);
                        code_table.appendChild(item);
                    }
                    code_tables.appendChild(code_table);
                }
                if (typeName.equals("ENUM")) {
                    String enumVal[] = sd.getEnumValues(colName);
                    code_table = valueDoc.createElement("code-table");
                    Element code_table_id = valueDoc.createElement("code-table-id");
                    code_table_id.setTextContent(colName);
                    code_table.appendChild(code_table_id);
                    for (int j = 0; j < enumVal.length; j++) {
                        String val = enumVal[j];
                        item = valueDoc.createElement("item");
                        value = valueDoc.createElement("value");
                        label = valueDoc.createElement("label");
                        label.setTextContent(val);
                        value.setTextContent(val);
                        item.appendChild(value);
                        item.appendChild(label);
                        code_table.appendChild(item);
                    }
                    code_tables.appendChild(code_table);
                }
                if (typeName.contains("_FK_")) {
                    String tnames[] = typeName.split("_");
                    foreingTable = tnames[2];
                    foreingColName = tnames[3];
                    System.out.println("RIFERIMENTO CHIAVE ESTERNA " + foreingColName);
                    Collection c = sd.getForeingKeyValue(foreingColName, foreingTable);
                    if (c.size() == 0) {
                        throw new XFormDocumentException("Foreing table " + foreingTable + " is empty");
                    }
                    code_table = valueDoc.createElement("code-table");
                    Element code_table_id = valueDoc.createElement("code-table-id");
                    code_table_id.setTextContent(colName);
                    code_table.appendChild(code_table_id);
                    for (Iterator it = c.iterator(); it.hasNext(); ) {
                        List<String> list = (List) it.next();
                        if (list.isEmpty()) {
                            throw new XFormDocumentException("Foreing table " + foreingTable + " is empty");
                        }
                        for (int j = 0; j < list.size(); j++) {
                            String val = list.get(j);
                            item = valueDoc.createElement("item");
                            value = valueDoc.createElement("value");
                            label = valueDoc.createElement("label");
                            label.setTextContent(val);
                            value.setTextContent(val);
                            item.appendChild(value);
                            item.appendChild(label);
                            code_table.appendChild(item);
                        }
                    }
                    code_tables.appendChild(code_table);
                }
            }
            String types = typeParamString.toString();
            parameters.put("data-types", types.trim());
            typeParamString = null;
            types = null;
        } catch (ParserConfigurationException ex) {
            throw new XFormDocumentException(ex.getMessage(), ex);
        } catch (SchemaQueryException ex) {
            throw new XFormDocumentException(ex.getMessage(), ex);
        }
    }

    /**
     * Costruisce il modello dati con dei valori passati
     * in input. Questo rappresenta il modello dati per una
     * richiesta di operazione di update
     * @param sd
     * @param values
     * @throws mwt.xml.xdbforms.xformlayer.exception.XFormDocumentException
     */
    public void buildDataModel(SchemaDocument sd, String schemaURL, Collection values) throws XFormDocumentException {
        try {
            String[] columns = sd.getAllColumnsName();
            if (dataModelDoc != null) {
                throw new XFormDocumentException("Data Model is already created");
            }
            if (values.size() != columns.length) {
                throw new XFormDocumentException("Value data model lack");
            }
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            dataModelDoc = db.newDocument();
            valueDoc = db.newDocument();
            Element data = dataModelDoc.createElement("data");
            data.setAttribute("xmlns:xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
            data.setAttribute("xsi:noNamespaceSchemaLocation", schemaURL);
            dataModelDoc.appendChild(data);
            StringBuilder elemParamString = new StringBuilder();
            int i = 0;
            for (Iterator it = values.iterator(); it.hasNext(); i++) {
                String val = (String) it.next();
                String colname = columns[i];
                Element e = dataModelDoc.createElement(colname);
                e.setTextContent(val);
                if (sd.getAutoincrementTypeName(colname) != null) {
                    System.out.println("COLNAME ---> " + colname);
                    e.setAttribute("isAutoincrement", "true");
                }
                data.appendChild(e);
                elemParamString.append(colname + " ");
            }
            String elems = elemParamString.toString();
            System.out.println("Parametri per la trasformazione " + elems.trim());
            elemParamString = null;
            parameters.put("data-elements", elems.trim());
            elems = null;
            String[] typeNames = sd.getAllColumnsType();
            String foreingTable;
            Element value;
            Element item, label;
            Element code_table;
            String foreingColName;
            Element code_tables = valueDoc.createElement("values-fields");
            valueDoc.appendChild(code_tables);
            StringBuilder typeParamString = new StringBuilder();
            for (int j = 0; j < typeNames.length; j++) {
                String typeName = typeNames[j];
                String colName = columns[j];
                typeParamString.append(typeName + " ");
                if (typeName.equals("SET")) {
                    String setVal[] = sd.getSetValues(colName);
                    code_table = valueDoc.createElement("code-table");
                    Element code_table_id = valueDoc.createElement("code-table-id");
                    code_table_id.setTextContent(colName);
                    code_table.appendChild(code_table_id);
                    for (int k = 0; k < setVal.length; k++) {
                        String val = setVal[k];
                        item = valueDoc.createElement("item");
                        value = valueDoc.createElement("value");
                        label = valueDoc.createElement("label");
                        label.setTextContent(val);
                        value.setTextContent(val);
                        item.appendChild(value);
                        item.appendChild(label);
                        code_table.appendChild(item);
                    }
                    code_tables.appendChild(code_table);
                }
                if (typeName.equals("ENUM")) {
                    String enumVal[] = sd.getEnumValues(colName);
                    code_table = valueDoc.createElement("code-table");
                    Element code_table_id = valueDoc.createElement("code-table-id");
                    code_table_id.setTextContent(colName);
                    code_table.appendChild(code_table_id);
                    for (int k = 0; k < enumVal.length; k++) {
                        String val = enumVal[k];
                        item = valueDoc.createElement("item");
                        value = valueDoc.createElement("value");
                        label = valueDoc.createElement("label");
                        label.setTextContent(val);
                        value.setTextContent(val);
                        item.appendChild(value);
                        item.appendChild(label);
                        code_table.appendChild(item);
                    }
                    code_tables.appendChild(code_table);
                }
                if (typeName.contains("_FK_")) {
                    String tnames[] = typeName.split("_");
                    foreingTable = tnames[2];
                    foreingColName = tnames[3];
                    System.out.println(colName + " HA UN RIFERIMENTO CHIAVE ESTERNA " + foreingColName);
                    Collection c = sd.getForeingKeyValue(foreingColName, foreingTable);
                    if (c.size() == 0) {
                        throw new XFormDocumentException("Foreing table " + foreingTable + " is empty");
                    }
                    code_table = valueDoc.createElement("code-table");
                    Element code_table_id = valueDoc.createElement("code-table-id");
                    code_table_id.setTextContent(colName);
                    code_table.appendChild(code_table_id);
                    for (Iterator it = c.iterator(); it.hasNext(); ) {
                        List<String> l = (List) it.next();
                        for (int k = 0; k < l.size(); k++) {
                            String val = l.get(k);
                            item = valueDoc.createElement("item");
                            value = valueDoc.createElement("value");
                            label = valueDoc.createElement("label");
                            label.setTextContent(val);
                            value.setTextContent(val);
                            item.appendChild(value);
                            item.appendChild(label);
                            code_table.appendChild(item);
                        }
                    }
                    code_tables.appendChild(code_table);
                }
            }
            String types = typeParamString.toString();
            System.out.println("Parametri trasformazione : " + types);
            parameters.put("data-types", types.trim());
            typeParamString = null;
            types = null;
        } catch (ParserConfigurationException ex) {
            throw new XFormDocumentException(ex.getMessage(), ex);
        } catch (SchemaQueryException ex) {
            throw new XFormDocumentException(ex.getMessage(), ex);
        }
    }

    public Document getDataModel() throws XFormDocumentException {
        return dataModelDoc;
    }

    public Document getValueModel() throws XFormDocumentException {
        NodeList nl = valueDoc.getDocumentElement().getChildNodes();
        if (nl.getLength() == 1) {
            return null;
        }
        return valueDoc;
    }
}
