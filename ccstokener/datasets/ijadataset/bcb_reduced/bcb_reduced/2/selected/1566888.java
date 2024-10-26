package pl.kernelpanic.dbmonster.schema;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import pl.kernelpanic.dbmonster.generator.DataGenerator;
import pl.kernelpanic.dbmonster.generator.KeyGenerator;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.digester.AbstractObjectCreationFactory;
import org.apache.commons.digester.Digester;
import org.apache.commons.logging.Log;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Utility to manipulate schema.
 *
 * @author Piotr Maj &lt;pm@jcake.com&gt;
 *
 * @version $Id: SchemaUtil.java,v 1.2 2006/01/05 16:29:37 majek Exp $
 */
public final class SchemaUtil {

    /**
     * DTD identifier.
     */
    public static final String DTD = "-//kernelpanic.pl//DBMonster Database Schema DTD 1.1//EN";

    /**
     * DTD url.
     */
    public static final String DTD_URL = "http://dbmonster.kernelpanic.pl/dtd/dbmonster-schema-1.1.dtd";

    /**
     * Project key.
     */
    public static final String PROJECT = "project";

    /**
     * Schema key.
     */
    public static final String SCHEMA_WRAPPER = "schema";

    /**
     * Table key.
     */
    public static final String TABLE = "table";

    /**
     * Column key.
     */
    public static final String COLUMN = "column";

    /**
     * Primary key key.
     */
    public static final String KEY = "key";

    /**
     * Key generator key.
     */
    public static final String KEY_GENERATOR = "key_generator";

    /**
     * Data generator key.
     */
    public static final String DATA_GENERATOR = "data_generator";

    /**
     * Holds the excluded property names.
     */
    private static Map exclusions = new HashMap();

    /**
     * System dependent line separator.
     */
    public static final String CRLF = System.getProperty("line.separator");

    static {
        exclusions.put(SchemaUtil.PROJECT, new String[] { "fileName", "properties", "jdbcViaProperties" });
        exclusions.put(SchemaUtil.SCHEMA_WRAPPER, new String[] { "fileName", "schema" });
        exclusions.put(SchemaUtil.TABLE, new String[] { "key", "schema" });
        exclusions.put(SchemaUtil.KEY, new String[] { "table", "generator" });
        exclusions.put(SchemaUtil.COLUMN, new String[] { "table", "generator", "value" });
        exclusions.put(SchemaUtil.KEY_GENERATOR, new String[] { "key" });
        exclusions.put(SchemaUtil.DATA_GENERATOR, new String[] { "column" });
    }

    /**
     * We do not need any public constructor.
     */
    private SchemaUtil() {
    }

    /**
     * Loads a schema from a file.
     *
     * @param fileName the name of the file which contains schema definition.
     * @param log logger
     *
     * @return the schema
     *
     * @throws Exception if schema cannot be loaded.
     */
    public static Schema loadSchema(String fileName, Log log) throws Exception {
        return loadSchema(fileName, log, null);
    }

    public static Schema loadSchema(String fileName, Log log, ClassLoader classloader) throws Exception {
        File f = new File(fileName);
        FileInputStream fis = new FileInputStream(f);
        Schema schema = loadSchema(fis, log, classloader);
        String homePath = f.getParent();
        schema.setHome(homePath);
        return schema;
    }

    /**
     * Loads a schema from an url.
     *
     * @param url url
     * @param log logger
     *
     * @return the schema
     *
     * @throws Exception on errors
     */
    public static Schema loadSchema(URL url, Log log) throws Exception {
        return loadSchema(url.openStream(), log);
    }

    /**
     * Loads a schema from an input stream.
     *
     * @param is input stream
     * @param log logger
     *
     * @return the schema
     *
     * @throws Exception if schema cannot be loaded.
     */
    public static Schema loadSchema(InputStream is, Log log) throws Exception {
        return loadSchema(is, log, null);
    }

    public static Schema loadSchema(InputStream is, Log log, ClassLoader classloader) throws Exception {
        Schema schema = null;
        ErrorHandler errorHandler = new ErrorHandler() {

            public void warning(SAXParseException exception) throws SAXException {
                throw exception;
            }

            public void error(SAXParseException exception) throws SAXException {
                throw exception;
            }

            public void fatalError(SAXParseException exception) throws SAXException {
                throw exception;
            }
        };
        URL url = SchemaUtil.class.getResource("/pl/kernelpanic/dbmonster/resources/dbmonster-schema-1.1.dtd");
        Digester digester = new Digester();
        if (log != null) {
            digester.setLogger(log);
        }
        digester.register(DTD, url.toString());
        if (classloader != null) {
            digester.setClassLoader(classloader);
        } else {
            digester.setUseContextClassLoader(true);
        }
        digester.setErrorHandler(errorHandler);
        digester.setValidating(true);
        digester.addObjectCreate("dbmonster-schema", Schema.class);
        digester.addCallMethod("dbmonster-schema/name", "setName", 0);
        digester.addObjectCreate("*/table", Table.class);
        digester.addSetProperties("*/table");
        digester.addObjectCreate("*/table/key", Key.class);
        digester.addSetProperties("*/table/key");
        digester.addFactoryCreate("*/table/key/generator", new KeyGeneratorFactory());
        digester.addSetProperty("*/table/key/generator/property", "name", "value");
        digester.addSetNext("*/table/key/generator", "setGenerator", KeyGenerator.class.getName());
        digester.addSetNext("*/table/key", "setKey", Key.class.getName());
        digester.addSetNext("*/table", "addTable", Table.class.getName());
        digester.addObjectCreate("*/table/column", Column.class);
        digester.addSetProperties("*/table/column");
        digester.addSetNext("*/table/column", "addColumn", Column.class.getName());
        digester.addFactoryCreate("*/table/column/generator", new GeneratorFactory());
        digester.addSetProperties("*/table/column/generator");
        digester.addSetNext("*/table/column/generator", "setGenerator", DataGenerator.class.getName());
        digester.addSetProperty("*/table/column/generator/property", "name", "value");
        schema = (Schema) digester.parse(is);
        return schema;
    }

    /**
     * Validate schema.
     *
     * @param schema schema to validate
     *
     * @return list of error messages or <code>null</code> if schema is ok
     */
    public static List validateSchema(Schema schema) {
        List errors = new ArrayList();
        String name = schema.getName();
        if (name == null || "".equals(name)) {
            errors.add("Schema has no name.");
        }
        List tables = schema.getTables();
        if (tables.isEmpty()) {
            errors.add("Schema " + name + " must have at least one table.");
        }
        for (int i = 0; i < tables.size(); i++) {
            Table t = (Table) tables.get(i);
            List tableErrors = validateTable(t);
            if (tableErrors != null) {
                errors.addAll(tableErrors);
            }
        }
        if (errors.isEmpty()) {
            return null;
        }
        return errors;
    }

    /**
     * Validates table.
     *
     * @param table table to validate
     *
     * @return list of error messages or null if table is OK.
     */
    public static List validateTable(Table table) {
        List errors = new ArrayList();
        String name = table.getName();
        if (name == null || "".equals(name)) {
            errors.add("Table has no name!");
        }
        if (table.getKey() == null && table.getColumns().isEmpty()) {
            errors.add("Table " + name + " must have a key or at least one column.");
        }
        Key key = table.getKey();
        if (key != null) {
            List keyErrors = validateKey(key);
            if (keyErrors != null) {
                errors.addAll(keyErrors);
            }
        }
        List columns = table.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            Column c = (Column) columns.get(i);
            List columnErrors = validateColumn(c);
            if (columnErrors != null) {
                errors.addAll(columnErrors);
            }
        }
        if (errors.isEmpty()) {
            return null;
        }
        return errors;
    }

    /**
     * Validates key.
     *
     * @param key key to validate
     *
     * @return list of error messages or <code>null</code> is key is ok
     */
    public static List validateKey(Key key) {
        List errors = new ArrayList();
        if (key.getGenerator() == null) {
            errors.add("Primary key for table " + key.getTable().getName() + " has no generator");
        }
        if (errors.isEmpty()) {
            return null;
        }
        return errors;
    }

    /**
     * Validates the column.
     *
     * @param column column to validate
     *
     * @return list of error messages or <code>null</code> if column is ok
     */
    public static List validateColumn(Column column) {
        List errors = new ArrayList();
        String name = column.getName();
        if (name == null || "".equals(name)) {
            errors.add("One column in table " + column.getTable().getName() + " has no name.");
        }
        if (column.getGenerator() == null) {
            errors.add("Column " + name + " in table " + column.getTable().getName() + " has no generator.");
        }
        if (errors.isEmpty()) {
            return null;
        }
        return errors;
    }

    /**
     * Returns object's properties. A property is the one which has
     * a public getter and setter and is not reserved DBMonster's property.
     *
     * @param object an schema element
     * @return list of properties
     */
    public static List getProperties(Object object) {
        List retList = new ArrayList();
        try {
            Class clazz = object.getClass();
            Method[] methods = clazz.getMethods();
            Map map = new HashMap();
            for (int i = 0; i < methods.length; i++) {
                Method m = methods[i];
                String mName = m.getName();
                if (mName.startsWith("get") || mName.startsWith("set")) {
                    if (Modifier.isPublic(m.getModifiers())) {
                        map.put(mName, mName);
                    }
                }
            }
            Iterator it = map.keySet().iterator();
            while (it.hasNext()) {
                String name = (String) it.next();
                if (name.startsWith("get")) {
                    String getter = name;
                    String setter = "s" + getter.substring(1);
                    String method = getter.substring(3);
                    char ch = method.charAt(0);
                    method = Character.toLowerCase(ch) + method.substring(1);
                    if (map.containsKey(setter)) {
                        if (!SchemaUtil.isHidden(object, method)) {
                            retList.add(method);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        Collections.sort(retList);
        return retList;
    }

    /**
     * Checks if property is public and not excluded.
     *
     * @param object an object to check
     * @param name property name
     * @return <code>true</code> if property is hidden
     */
    public static boolean isHidden(Object object, String name) {
        String[] excl = null;
        if (object instanceof Table) {
            excl = (String[]) exclusions.get(SchemaUtil.TABLE);
        } else if (object instanceof Key) {
            excl = (String[]) exclusions.get(SchemaUtil.KEY);
        } else if (object instanceof KeyGenerator) {
            excl = (String[]) exclusions.get(SchemaUtil.KEY_GENERATOR);
        } else if (object instanceof Column) {
            excl = (String[]) exclusions.get(SchemaUtil.COLUMN);
        } else {
            excl = (String[]) exclusions.get(SchemaUtil.DATA_GENERATOR);
        }
        for (int i = 0; i < excl.length; i++) {
            if (name.equals(excl[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Dumps the schema to XML file.
     *
     * @param writer writer we are appending to
     * @param schema schema to dump
     *
     * @throws Exception on errors
     */
    public static void serializeSchema(Writer writer, Schema schema) throws Exception {
        writer.write("<?xml version=\"1.0\"?>");
        writer.write(CRLF);
        writer.write("<!DOCTYPE dbmonster-schema PUBLIC \"");
        writer.write(DTD);
        writer.write("\" \"");
        writer.write(DTD_URL);
        writer.write("\">");
        writer.write(CRLF);
        writer.write("<dbmonster-schema>");
        writer.write(CRLF);
        writer.write("  <name>");
        writer.write(schema.getName());
        writer.write("</name>");
        writer.write(CRLF);
        Iterator it = schema.getTables().iterator();
        while (it.hasNext()) {
            Table table = (Table) it.next();
            serializeTable(writer, table);
        }
        writer.write("</dbmonster-schema>");
        writer.write(CRLF);
        writer.flush();
    }

    /**
     * Dumps a table to XML representation.
     *
     * @param writer writer we are appengind to
     * @param table table to serialize
     *
     * @throws Exception on errors
     */
    public static void serializeTable(Writer writer, Table table) throws Exception {
        writer.write("  <table name=\"");
        writer.write(table.getName());
        writer.write("\" rows=\"");
        writer.write(String.valueOf(table.getRows()));
        writer.write("\">");
        writer.write(CRLF);
        if (table.getKey() != null) {
            serializeKey(writer, table.getKey());
        }
        Iterator it = table.getColumns().iterator();
        while (it.hasNext()) {
            Column column = (Column) it.next();
            serializeColumn(writer, column);
        }
        writer.write("  </table>");
        writer.write(CRLF);
    }

    /**
     * Dumps a key to XML representation.
     *
     * @param writer writer we are appending to
     * @param key key to dump
     *
     * @throws Exception on errors
     */
    public static void serializeKey(Writer writer, Key key) throws Exception {
        writer.write("    <key databaseDefault=\"");
        writer.write(String.valueOf(key.getDatabaseDefault()));
        writer.write("\">");
        writer.write(CRLF);
        serializeGenerator(writer, key.getGenerator());
        writer.write("    </key>");
        writer.write(CRLF);
    }

    /**
     * Dumps a column to XML.
     *
     * @param writer writer we are appending to
     * @param column column to dump
     *
     * @throws Exception on errors
     */
    public static void serializeColumn(Writer writer, Column column) throws Exception {
        writer.write("    <column name=\"");
        writer.write(column.getName());
        writer.write("\" databaseDefault=\"");
        writer.write(String.valueOf(column.getDatabaseDefault()));
        writer.write("\">");
        writer.write(CRLF);
        serializeGenerator(writer, column.getGenerator());
        writer.write("    </column>");
        writer.write(CRLF);
    }

    /**
     * Dumps a generator to XML.
     *
     * @param writer writter we are appending to
     * @param generator generator
     *
     * @throws Exception on errors
     */
    public static void serializeGenerator(Writer writer, Object generator) throws Exception {
        writer.write("      <generator type=\"");
        writer.write(generator.getClass().getName());
        writer.write("\">");
        writer.write(CRLF);
        List properties = getProperties(generator);
        Iterator it = properties.iterator();
        while (it.hasNext()) {
            String property = (String) it.next();
            writer.write("        <property name=\"");
            writer.write(property);
            writer.write("\" value=\"");
            String value = BeanUtils.getProperty(generator, property);
            if (value != null) {
                writer.write(value);
            }
            writer.write("\"/>");
            writer.write(CRLF);
        }
        writer.write("      </generator>");
        writer.write(CRLF);
    }
}

/**
 * Data generators factory.
 */
final class GeneratorFactory extends AbstractObjectCreationFactory {

    /**
     * This method creates a data generator object.
     *
     * @param attributes SAX attributes
     *
     * @return created data generator
     *
     * @throws Exception if generator cannot be created
     */
    public Object createObject(Attributes attributes) throws Exception {
        String className = attributes.getValue("type");
        Class clazz = Class.forName(className);
        Object o = clazz.newInstance();
        return o;
    }
}

/**
 * Key generators factory.
 */
final class KeyGeneratorFactory extends AbstractObjectCreationFactory {

    /**
     * This method creates a key generator object.
     *
     * @param attributes SAX attributes
     *
     * @return created key generator
     *
     * @throws Exception if generator cannot be created
     */
    public Object createObject(Attributes attributes) throws Exception {
        String className = attributes.getValue("type");
        Class clazz = Class.forName(className);
        Object o = clazz.newInstance();
        return o;
    }
}
