package org.javahispano.dbmt.migrations;

import java.sql.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import org.xml.sax.*;
import javax.xml.parsers.*;
import org.xml.sax.helpers.DefaultHandler;
import static java.util.logging.Level.*;
import java.util.logging.*;
import org.javahispano.dbmt.*;
import org.medfoster.sqljep.*;

/**
 * Source for reading tables from XML files.
 * <code>MigrationSaxXMLSource</code> uses JAXP SAX parser to get source tables.
 *<p>
 * For example
 * <p>
 * &lt;?xml version="1.0" encoding="Windows-1251"?&gt;<br/>
 * &lt;select&gt;<br/>
 * &lt;row&gt;<br/>
 * &lt;COL_NAME1&gt;value1&lt;/COL_NAME1&gt;<br/>
 * &lt;COL_NAME2&gt;value2&lt;/COL_NAME2&gt;<br/>
 * &lt;/row&gt;<br/>
 * &lt;row&gt;<br/>
 * &lt;COL_NAME1&gt;value3&lt;/COL_NAME1&gt;<br/>
 * &lt;COL_NAME2&gt;value4&lt;/COL_NAME2&gt;<br/>
 * &lt;/row&gt;<br/>
 * &lt;/select&gt;<br/>
 *<p>
 * Names of columns are taken only from the first row.
 *<p>
 * Names of tags 'select' and 'row' can be changed in  {@link #setSource(String driver, String url)}.
 *<p>
 * <code>MigrationSaxXMLSource</code> supports compressed xml files. It this case <CODE>url</CODE>
 * parameter in {@link #setSource(String driver, String url)}
 * is JAR archive and step's {@link Step#getSourceTable()} is a name of the XML file in JAR archive.
 * JAR achive will opened only once per migration process.
 *<p>
 * <code>MigrationSaxXMLSource</code> has one parameter.
 * <table border=1 cellSpacing=1 summary="MigrationSaxXMLSource parameters">
 * <tr>
 * <th>Name</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>driver</td>
 * <td>Path in XML file of rows. By default path is 'select.row'. Minimum number of tags are two.</td>
 * </tr>
 * </table>
 *
 * @author <a href="mailto:alexey.gaidukov@gmail.com">Alexey Gaidukov</a>
 * @see Step
 */
public final class MigrationSaxXMLSource extends DefaultHandler implements MigrationSource, CallbackMigrationSource {

    private static final Logger logger = Logger.getLogger("org.javahispano.dbmt");

    private String url;

    private String source;

    private String rowTag = "select.row";

    private String rowPrefixTag = "select";

    private JarFile jar = null;

    private String xmlSource = null;

    private SAXParser sp = null;

    private Logger stepLog = null;

    private String path = "";

    private Locator locator = null;

    private StringBuilder textNode = new StringBuilder();

    private HashMap<String, Integer> columnMapping = new HashMap<String, Integer>();

    private HashMap<String, String> firstRow = null;

    private Comparable[] record = null;

    private int columnIndex = 0;

    private Step step = null;

    /**
	 *
	 * @param driver Path  of rows in XML file.
	 *  By default path is 'select.row'. Minimum number of tags are two.
	 * @param url of the XML file.
	 * Full path is <CODE>url+Step.getSourceTable()</CODE>.
	 * If in <CODE>url</CODE> is JAR protocol (<CODE>jar:http://example.org/archive.jar!/</CODE>) then
	 * <CODE>url</CODE> is the name of jar archive and <CODE>Step.getSourceTable()</CODE>
	 * is path to file in archive.
	 */
    public void setSource(String driver, String url, String username, String password) throws MigrationException {
        this.url = (url != null) ? url : "";
        if (driver != null) {
            int i = driver.lastIndexOf('.');
            if (i > 0) {
                rowTag = driver;
                rowPrefixTag = driver.substring(0, i);
            }
        }
    }

    public void setXML(String xml) {
        xmlSource = xml;
    }

    /**
	 * Implementation of interface ContentHandler
	 */
    public void startDocument() {
        path = "";
    }

    /**
	 * Implementation of interface ContentHandler
	 */
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    /**
	 * Implementation of interface ContentHandler
	 */
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.indexOf('.') == -1) {
            if (path.length() > 0) {
                path += '.';
            }
            path += qName;
        } else {
            throw new SAXException(toString() + " Tag name can't contain symbol '.'");
        }
    }

    /**
	 * Implementation of interface ContentHandler
	 */
    public void characters(char ch[], int start, int length) throws SAXException {
        String temp = new String(ch, start, length);
        textNode.append(temp.trim());
    }

    /**
	 * Implementation of interface ContentHandler
	 */
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        int idx = path.lastIndexOf(".");
        if (idx > 0) {
            path = path.substring(0, idx);
        } else {
            return;
        }
        if (rowPrefixTag.equals(path)) {
            try {
                if (firstRow != null) {
                    record = new Comparable[columnMapping.size()];
                    for (Field field : step.getFields()) {
                        Integer col = columnMapping.get(field.getFrom());
                        if (col != null) {
                            field.setColumnIndex(col);
                        } else {
                            DbmtJEP jep = new DbmtJEP(field.getFrom(), step);
                            try {
                                jep.parseExpression(columnMapping);
                            } catch (ParseException e) {
                                throw new MigrationException(e);
                            }
                            jep.setRow(record);
                            field.setJEP(jep);
                        }
                    }
                    for (Map.Entry<String, Integer> entry : columnMapping.entrySet()) {
                        String value = firstRow.get(entry.getKey());
                        record[entry.getValue()] = value;
                    }
                    String whereCondition = step.getWhereCondition();
                    if (whereCondition != null) {
                        DbmtJEP where = new DbmtJEP(whereCondition, step);
                        try {
                            where.parseExpression(columnMapping);
                        } catch (ParseException e) {
                            throw new MigrationException(e);
                        }
                        where.setRow(record);
                        step.setWhere(where);
                    } else {
                        step.setWhere(null);
                    }
                    firstRow = null;
                }
                step.step();
            } catch (MigrationException se) {
                throw new SAXException(se);
            }
            columnIndex = 0;
        } else if (rowTag.equals(path)) {
            String value = textNode.length() > 0 ? textNode.toString() : null;
            if (firstRow != null) {
                String column = qName.toUpperCase();
                firstRow.put(column, value);
                columnMapping.put(column, columnIndex++);
            } else {
                record[columnIndex++] = value;
            }
            textNode.setLength(0);
        }
    }

    public void initSource(String source, Step step) throws MigrationException {
        this.source = source;
        this.step = step;
        columnMapping.clear();
        try {
            if (sp == null) {
                SAXParserFactory spf = SAXParserFactory.newInstance();
                sp = spf.newSAXParser();
                XMLReader reader = sp.getXMLReader();
                reader.setContentHandler(this);
            } else {
                sp.reset();
            }
            if (jar == null) {
                URL url = new URL(this.url);
                URLConnection con = url.openConnection();
                if (con instanceof JarURLConnection) {
                    jar = ((JarURLConnection) con).getJarFile();
                }
            }
            firstRow = new HashMap<String, String>();
            record = null;
            path = "";
        } catch (MalformedURLException me) {
        } catch (Exception e) {
            throw new MigrationException(e);
        }
    }

    public void startParse() throws MigrationException {
        if (source != null && source.length() > 0) {
            try {
                if (logger.isLoggable(FINE)) {
                    logger.fine("Open XML:" + url + source);
                }
                if (jar != null) {
                    InputStream jarSource = null;
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.getName().equals(source)) {
                            jarSource = jar.getInputStream(entry);
                            break;
                        }
                    }
                    if (jarSource != null) {
                        sp.getXMLReader().parse(new InputSource(jarSource));
                        jarSource.close();
                    } else {
                        throw new MigrationException("Can't find file '" + source + "' in JAR");
                    }
                } else if (xmlSource != null) {
                    StringReader reader = new StringReader(xmlSource);
                    InputSource is = new InputSource(reader);
                    sp.getXMLReader().parse(is);
                    reader.close();
                } else {
                    sp.getXMLReader().parse(url + source);
                }
            } catch (SAXException se) {
                if (se.getException() instanceof MigrationException) {
                    throw (MigrationException) se.getException();
                } else {
                    throw new MigrationException(toString(), se);
                }
            } catch (Exception e) {
                throw new MigrationException(toString(), e);
            }
        } else {
            for (Field field : step.getFields()) {
                DbmtJEP jep = new DbmtJEP(field.getFrom(), step);
                try {
                    jep.parseExpression(columnMapping);
                } catch (ParseException e) {
                    throw new MigrationException(e);
                }
                field.setJEP(jep);
            }
            step.step();
        }
    }

    /**
	 *	Where condition is created in endElement method and
	 *	passing into Step object by Step.setWhere(BaseJEP jep) method
	 */
    public DbmtJEP compileWhere(String whereCondition) throws MigrationException {
        return null;
    }

    public boolean next() throws MigrationException {
        for (Field field : step.getFields()) {
            if (field.getJEP() == null) {
                int i = field.getColumnIndex();
                record[i] = field.parseValue((String) record[i]);
            }
        }
        return true;
    }

    public Comparable getColumnObject(Field field) throws MigrationException {
        try {
            int column = field.getColumnIndex();
            return record[column];
        } catch (IndexOutOfBoundsException e) {
            throw new MigrationException(e.getMessage());
        }
    }

    public void close() throws MigrationException {
    }

    public void shutdown() throws MigrationException {
        sp = null;
        if (jar != null) {
            try {
                jar.close();
            } catch (IOException e) {
                throw new MigrationException(e);
            }
            jar = null;
        }
    }

    public String toString() {
        String loc = "";
        if (locator != null) {
            loc += "Line " + locator.getLineNumber();
            int col = locator.getColumnNumber();
            if (col >= 0) {
                loc += ", Column " + col;
            }
        } else {
            loc = "";
        }
        return (step != null) ? loc + " (STEP:" + step.toString() + ")" : loc;
    }
}
