package org.apache.derbyDemo.vtis.core;

import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import javax.xml.parsers.*;
import org.w3c.dom.*;

/**
 * <p>
 * This is a VTI designed to read XML files which are structured like row sets.
 * </p>
 *
  */
public class XmlVTI extends StringColumnVTI {

    private String _rowTag;

    private String _xmlResourceName;

    private int _rowIdx = -1;

    private int _rowCount = -1;

    private String[] _currentRow;

    private DocumentBuilder _builder;

    private NodeList _rawRows;

    /**
     * <p>
     * Build a XmlVTI given the name of an xml resource, the  tag of the row
     * element, and an array of attribute-names/element-tags underneath the row element
     * </p>
     */
    public XmlVTI(String xmlResourceName, String rowTag, String[] childTags) {
        super(childTags);
        _xmlResourceName = xmlResourceName;
        _rowTag = rowTag;
    }

    /**
     * <p>
     * Register all of the XMLRow table functions in a class. This method is exposed as a
     * database procedure.
     * </p>
     */
    public static void registerXMLRowVTIs(String className) throws Exception {
        Class theClass = Class.forName(className);
        Method[] methods = theClass.getMethods();
        int count = methods.length;
        Method candidate = null;
        XMLRow xmlRowAnnotation = null;
        for (int i = 0; i < count; i++) {
            candidate = methods[i];
            int modifiers = candidate.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && candidate.getReturnType() == ResultSet.class) {
                xmlRowAnnotation = candidate.getAnnotation(XMLRow.class);
                if (xmlRowAnnotation != null) {
                    VTIHelper.unregisterVTI(candidate);
                    registerVTI(candidate);
                }
            }
        }
    }

    /**
     * <p>
     * Get the string value of the column in the current row identified by the 1-based columnNumber.
     * </p>
     */
    protected String getRawColumn(int columnNumber) throws SQLException {
        try {
            return _currentRow[columnNumber - 1];
        } catch (Throwable t) {
            throw new SQLException(t.getMessage());
        }
    }

    public void close() throws SQLException {
        _builder = null;
        _rawRows = null;
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public boolean next() throws SQLException {
        try {
            if (_rowIdx < 0) {
                readRows();
            }
            if (++_rowIdx < _rowCount) {
                parseRow(_rowIdx);
                return true;
            } else {
                return false;
            }
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            throw new SQLException(t.getMessage());
        }
    }

    /**
     * <p>
     * Register a method as a Derby Table Function. We assume the following:
     * </p>
     *
     * <ul>
     * <li>The method is public and static.</li>
     * <li>The method returns a ResultSet.</li>
     * <li>The method is annotated as an XMLRow.</li>
     * </ul>
     *
     */
    public static void registerVTI(Method method) throws Exception {
        XMLRow annotation = method.getAnnotation(XMLRow.class);
        String methodName = method.getName();
        String sqlName = methodName;
        Class methodClass = method.getDeclaringClass();
        Class[] parameterTypes = method.getParameterTypes();
        int parameterCount = parameterTypes.length;
        String[] columnNames = annotation.childTags();
        String[] columnTypes = annotation.childTypes();
        int columnCount = columnNames.length;
        int typeCount = columnTypes.length;
        StringBuilder buffer = new StringBuilder();
        if (columnCount != typeCount) {
            throw new Exception("Bad XMLRow annotation for " + methodName + ". The number of childTags (" + columnCount + ") does not equal the number of childTypes (" + typeCount + ").");
        }
        VTIHelper.registerVTI(method, columnNames, columnTypes, false);
    }

    /**
     * <p>
     * Create a VTI ResultSet. It is assumed that our caller is an
     * XMLRow-annotated method with one String argument. It
     * is assumed that ResultSet is an instance of vtiClass and that
     * vtiClass extends XmlVTI and has a constructor with the same
     * arguments as the constructor of XmlVTI.
     * </p>
     *
     */
    public static ResultSet instantiateVTI(String xmlResourceName) throws SQLException {
        try {
            StackTraceElement[] stack = (new Throwable()).getStackTrace();
            StackTraceElement caller = stack[1];
            Class callerClass = Class.forName(caller.getClassName());
            String methodName = caller.getMethodName();
            Method method = callerClass.getMethod(methodName, new Class[] { String.class });
            XMLRow annotation = method.getAnnotation(XMLRow.class);
            String rowTag = annotation.rowTag();
            String[] childTags = annotation.childTags();
            String vtiClassName = annotation.vtiClassName();
            Class vtiClass = Class.forName(vtiClassName);
            Constructor constructor = vtiClass.getConstructor(new Class[] { String.class, String.class, String[].class });
            return (ResultSet) constructor.newInstance(xmlResourceName, rowTag, childTags);
        } catch (Throwable t) {
            throw new SQLException(t.getMessage());
        }
    }

    /**
     * <p>
     * Fault in the list of rows.
     * </p>
     */
    private void readRows() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        _builder = factory.newDocumentBuilder();
        URL url = new URL(_xmlResourceName);
        InputStream is = url.openStream();
        Document doc = _builder.parse(is);
        Element root = doc.getDocumentElement();
        _rawRows = root.getElementsByTagName(_rowTag);
        _rowCount = _rawRows.getLength();
        is.close();
    }

    /**
     * <p>
     * Parse a row into columns.
     * </p>
     */
    private void parseRow(int rowNumber) throws Exception {
        Element rawRow = (Element) _rawRows.item(rowNumber);
        String[] columnNames = getColumnNames();
        int columnCount = columnNames.length;
        _currentRow = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            String columnName = columnNames[i];
            String contents = rawRow.getAttribute(columnName);
            if ((contents == null) || "".equals(contents)) {
                NodeList children = rawRow.getElementsByTagName(columnName);
                if ((children != null) && (children.getLength() > 0)) {
                    int childCount = children.getLength();
                    StringBuffer buffer = new StringBuffer();
                    for (int j = 0; j < childCount; j++) {
                        Element child = (Element) children.item(j);
                        if (j != 0) buffer.append(" ");
                        buffer.append(squeezeText(child));
                    }
                    contents = buffer.toString();
                }
            }
            _currentRow[i] = contents;
        }
    }

    /**
     * <p>
     * Squeeze the text out of an Element.
     * </p>
     */
    private String squeezeText(Element node) throws Exception {
        String text = null;
        Node textChild = node.getFirstChild();
        if (textChild != null) {
            text = textChild.getNodeValue();
        }
        return text;
    }
}
