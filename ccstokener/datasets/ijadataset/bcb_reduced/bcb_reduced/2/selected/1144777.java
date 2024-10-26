package com.technoetic.tornado.config;

import static com.technoetic.tornado.config.ConfigSupport.*;
import com.technoetic.tornado.ColumnMapping;
import com.technoetic.tornado.DefaultInstanceFactory;
import com.technoetic.tornado.JdbcConnectionFactory;
import com.technoetic.tornado.JdbcPersistentClass;
import com.technoetic.tornado.ObjectMapping;
import com.technoetic.tornado.ObjectMappingImpl;
import com.technoetic.tornado.PersistenceException;
import com.technoetic.tornado.PersistentClass;
import com.technoetic.tornado.config.annotations.AnnotationProcessor;
import com.technoetic.tornado.engine.DatabaseEngine;
import com.technoetic.tornado.engine.MysqlDatabaseEngine;
import com.technoetic.tornado.engine.OracleDatabaseEngine;
import com.technoetic.tornado.event.AbstractEventListenerMapping;
import com.technoetic.tornado.event.ClassObserverFactory;
import com.technoetic.tornado.event.ObserverFactory;
import com.technoetic.tornado.event.PersistentEventListenerMapping;
import com.technoetic.tornado.event.StaticFactoryMethodObserverFactory;
import com.technoetic.tornado.event.TransientEventListenerMapping;
import com.technoetic.tornado.plugin.ClassSelector;
import com.technoetic.tornado.plugin.ColumnConverter;
import com.technoetic.tornado.plugin.GenericClassSelector;
import com.technoetic.tornado.plugin.IdentityGenerator;
import com.technoetic.tornado.plugin.InstanceFactory;
import com.technoetic.tornado.relationship.ManyToManyCollectionMapping;
import com.technoetic.tornado.relationship.ManyToManyCollectionQueryImpl;
import com.technoetic.tornado.relationship.OneToManyCollectionMapping;
import com.technoetic.tornado.relationship.OneToManyCollectionQueryImpl;
import com.technoetic.tornado.relationship.RelationshipMapping;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.apache.log4j.Category;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.List;

public class DatabaseConfigurationImpl implements DatabaseConfiguration {

    private static final Category log = Category.getInstance("tornado.DatabaseConfiguration");

    private String databaseEngineName;

    private Map<Class<?>, PersistentClass> classMappings = new HashMap<Class<?>, PersistentClass>();

    private List<String> mappingDirectories = new ArrayList<String>();

    private List<TransactionListenerConfiguration> transactionListeners = new ArrayList<TransactionListenerConfiguration>();

    private JdbcConnectionFactory connectionFactory = null;

    private Map<String, Object> globalUserTypes = new HashMap<String, Object>();

    private Map<String, Object> localUserTypes = new HashMap<String, Object>();

    private static Map<String, DatabaseConfigurationImpl> configurations = new HashMap<String, DatabaseConfigurationImpl>();

    private DatabaseEngine databaseEngine;

    private InstanceFactory defaultInstanceFactory;

    private AnnotationProcessor annotationProcessor = new AnnotationProcessor();

    public Map<Class<?>, PersistentClass> getClassMappings() {
        return classMappings;
    }

    public List<TransactionListenerConfiguration> getTransactionListeners() {
        return transactionListeners;
    }

    public JdbcConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public String getDatabaseEngineName() {
        return databaseEngineName;
    }

    public Map<String, Object> getGlobalUserTypes() {
        return globalUserTypes;
    }

    public Map<String, Object> getLocalUserTypes() {
        return localUserTypes;
    }

    public synchronized void load(String name) {
        DatabaseConfigurationImpl cachedConfig = configurations.get(name);
        if (cachedConfig != null) {
            loadFromCache(cachedConfig);
        } else {
            loadFromFile(name);
        }
    }

    private void loadFromCache(DatabaseConfigurationImpl existingConfig) {
        databaseEngineName = existingConfig.databaseEngineName;
        databaseEngine = existingConfig.databaseEngine;
        classMappings = existingConfig.classMappings;
        mappingDirectories = existingConfig.mappingDirectories;
        transactionListeners = existingConfig.transactionListeners;
        connectionFactory = existingConfig.connectionFactory;
        globalUserTypes = existingConfig.globalUserTypes;
        defaultInstanceFactory = existingConfig.defaultInstanceFactory;
    }

    private void loadFromFile(String name) {
        try {
            InputStream in = getInputStream(name);
            SAXBuilder builder = new SAXBuilder();
            Document document = builder.build(in, name);
            Element database = document.getRootElement();
            if (database.getName().equals("database")) {
                initializeDefaultInstanceFactory(database);
                parseDatabaseEngine(database);
                parseDatabaseDriver(database);
                parseMappingInformation(database);
                parseGlobalTransactionListeners(database);
                parseUserDefinedTypes(globalUserTypes, database);
            }
            configurations.put(name, this);
        } catch (JDOMException ex) {
            log.error(name + " is not well formed.", ex);
        } catch (Exception ex) {
            log.error("error loading configuration: " + name, ex);
        }
    }

    private void parseDatabaseEngine(Element database) throws PersistenceException {
        databaseEngineName = getAttribute(database, "engine");
        if (databaseEngineName != null) {
            if (databaseEngineName.equals("mysql")) {
                databaseEngine = new MysqlDatabaseEngine();
            } else if (databaseEngineName.equals("oracle")) {
                databaseEngine = new OracleDatabaseEngine();
            } else {
                try {
                    databaseEngine = (DatabaseEngine) Class.forName(databaseEngineName).newInstance();
                } catch (Exception e) {
                    throw new PersistenceException(e);
                }
            }
        }
    }

    private void initializeDefaultInstanceFactory(Element database) throws PersistenceException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        String instanceFactoryName = getAttribute(database, "instance-factory");
        if (instanceFactoryName != null) {
            Class<?> instanceFactoryClass = Class.forName(instanceFactoryName);
            defaultInstanceFactory = (InstanceFactory) instanceFactoryClass.newInstance();
        } else {
            defaultInstanceFactory = new DefaultInstanceFactory();
        }
    }

    private void parseGlobalTransactionListeners(Element database) throws PersistenceException {
        Iterator<?> listenerItr = database.getChildren("transaction-listener").iterator();
        while (listenerItr.hasNext()) {
            transactionListeners.add(parseTransactionListener((Element) listenerItr.next()));
        }
    }

    private void parseMappingInformation(Element database) throws PersistenceException {
        mappingDirectories.add("");
        Iterator<?> mappingDirectoryItr = database.getChildren("mapping-directory").iterator();
        while (mappingDirectoryItr.hasNext()) {
            Element mappingDirectory = (Element) mappingDirectoryItr.next();
            mappingDirectories.add(getAttribute(mappingDirectory, "path", true) + "/");
        }
        Iterator<?> mappingItr = database.getChildren("mapping").iterator();
        while (mappingItr.hasNext()) {
            Element mapping = (Element) mappingItr.next();
            ObjectMapping objectMapping = loadObjectMapping(getAttribute(mapping, "href", true));
            classMappings.put(objectMapping.getObjectClass(), new JdbcPersistentClass(databaseEngine, objectMapping));
        }
    }

    private void parseDatabaseDriver(Element database) throws PersistenceException {
        Element driver = database.getChild("driver");
        if (driver != null) {
            String driverClass = getAttribute(driver, "class-name", true);
            String url = getAttribute(driver, "url", true);
            String user = null;
            String password = null;
            Iterator<?> itr = driver.getChildren("param").iterator();
            while (itr.hasNext()) {
                Element param = (Element) itr.next();
                String name = getAttribute(param, "name", true);
                String value = getAttribute(param, "value", true);
                if (name.equals("user")) {
                    user = value;
                } else if (name.equals("password")) {
                    password = value;
                }
            }
            try {
                Class.forName(driverClass).newInstance();
            } catch (Exception ex) {
                throw new PersistenceException("couldn't load db driver", ex);
            }
            connectionFactory = new JdbcConnectionFactory(url, user, password);
        }
    }

    private ObjectMapping loadObjectMapping(String filename) throws PersistenceException {
        return loadObjectMapping(filename, getInputStream(filename));
    }

    private ObjectMapping loadObjectMapping(String filename, InputStream in) throws PersistenceException {
        log.debug("load mapping: " + filename);
        try {
            SAXBuilder builder = new SAXBuilder();
            Document document = builder.build(in, filename);
            Element mapping = document.getRootElement();
            if (mapping.getName().equals("mapping")) {
                Element classDescription = mapping.getChild("class");
                if (classDescription != null) {
                    parseUserDefinedTypes(localUserTypes, classDescription);
                    String name = getAttribute(classDescription, "name", true);
                    String extendsName = getAttribute(classDescription, "extends");
                    String[] idFields = parseIds(getAttribute(classDescription, "identity", true));
                    Element deletedColumn = classDescription.getChild("deleted-column");
                    String deletedColumnName = deletedColumn != null ? getAttribute(deletedColumn, "name") : null;
                    Element lastUpdateColumn = classDescription.getChild("last-update-column");
                    String lastUpdateColumnName = lastUpdateColumn != null ? getAttribute(lastUpdateColumn, "name") : null;
                    Element mapTo = classDescription.getChild("map-to");
                    String tableName = mapTo != null ? getAttribute(mapTo, "table") : null;
                    ArrayList<RelationshipMapping> relationships = new ArrayList<RelationshipMapping>();
                    ArrayList<ColumnMapping> columnMappings = new ArrayList<ColumnMapping>();
                    Iterator<?> fieldElements = classDescription.getChildren("field").iterator();
                    while (fieldElements.hasNext()) {
                        Element field = (Element) fieldElements.next();
                        String fieldName = getAttribute(field, "name", true);
                        String fieldTypeName = getAttribute(field, "type");
                        Element sql = field.getChild("sql");
                        if (sql != null) {
                            parseSqlDescription(name, fieldName, fieldTypeName, sql, columnMappings, relationships);
                        } else {
                            String columnName = fieldName;
                            ColumnMapping cmap = new ColumnMapping();
                            cmap.fieldName = fieldName;
                            cmap.columnName = columnName;
                            setFieldType(cmap, name, fieldName, fieldTypeName);
                            if (cmap.converter != null) {
                                cmap.columnType = cmap.converter.getSqlColumnType();
                            } else {
                                cmap.columnType = convertJavaClassToSqlType(cmap.fieldType);
                            }
                            columnMappings.add(cmap);
                        }
                    }
                    ArrayList<AbstractEventListenerMapping> listeners = parseEventListeners(classDescription, columnMappings);
                    ObjectMappingImpl objectMapping;
                    Class<?> objectClass = Class.forName(name);
                    objectMapping = new ObjectMappingImpl(objectClass, columnMappings, relationships, listeners, tableName, idFields);
                    if (deletedColumnName != null) {
                        objectMapping.setDeletedColumn(deletedColumnName);
                    }
                    if (lastUpdateColumnName != null) {
                        objectMapping.setLastUpdateColumn(lastUpdateColumnName);
                    }
                    String identityGeneratorName = getAttribute(classDescription, "identity-generator");
                    if (identityGeneratorName != null) {
                        Class<?> generatorClass = Class.forName(identityGeneratorName);
                        if (IdentityGenerator.class.isAssignableFrom(generatorClass)) {
                            objectMapping.setIdentityGenerator((IdentityGenerator) generatorClass.newInstance());
                        } else {
                            throw new Exception("invalid class for identity generator: " + identityGeneratorName);
                        }
                    }
                    String instanceFactoryName = getAttribute(classDescription, "instance-factory");
                    if (instanceFactoryName != null) {
                        Class<?> instanceFactoryClass = Class.forName(instanceFactoryName);
                        objectMapping.setInstanceFactory((InstanceFactory) instanceFactoryClass.newInstance());
                    } else {
                        objectMapping.setInstanceFactory(defaultInstanceFactory);
                    }
                    ClassSelector classSelectorInstance = parseClassSelector(classDescription);
                    if (classSelectorInstance != null) {
                        objectMapping.setClassSelector(classSelectorInstance);
                    }
                    if (extendsName != null) {
                        objectMapping.setParentClass(Class.forName(extendsName));
                    }
                    parseClassTransactionListeners(classDescription);
                    return objectMapping;
                }
            }
        } catch (JDOMException ex) {
            throw new PersistenceException(filename + " is not well formed", ex);
        } catch (PersistenceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PersistenceException("error while loading object mapping: " + filename, ex);
        }
        localUserTypes.clear();
        return null;
    }

    private void parseSqlDescription(String name, String fieldName, String fieldType, Element sql, ArrayList<ColumnMapping> columnMappings, ArrayList<RelationshipMapping> relationships) throws Exception {
        if (sql.getAttribute("many-table") != null) {
            String manyTable = getAttribute(sql, "many-table", true);
            String fromColumn = getAttribute(sql, "from-column", true);
            String toColumn = getAttribute(sql, "to-column", true);
            String where = getAttribute(sql, "where");
            String order = getAttribute(sql, "order");
            String delete = getAttribute(sql, "delete");
            boolean doDelete = delete != null && delete.equals("cascade");
            relationships.add(new ManyToManyCollectionMapping(Class.forName(name), fieldName, doDelete, new ManyToManyCollectionQueryImpl(Class.forName(name), Class.forName(fieldType), manyTable, fromColumn, toColumn, where, order)));
        } else if (sql.getAttribute("related-by") != null) {
            String relatedBy = getAttribute(sql, "related-by", true);
            String where = getAttribute(sql, "where");
            String order = getAttribute(sql, "order");
            String delete = getAttribute(sql, "delete");
            boolean doDelete = delete != null && delete.equals("cascade");
            relationships.add(new OneToManyCollectionMapping(Class.forName(name), fieldName, doDelete, new OneToManyCollectionQueryImpl(Class.forName(name), Class.forName(fieldType), relatedBy, where, order)));
        } else {
            String columnName = getAttribute(sql, "name", true);
            String columnType = getAttribute(sql, "type", true);
            ColumnMapping cmap = new ColumnMapping();
            cmap.fieldName = fieldName;
            cmap.columnName = columnName;
            setFieldType(cmap, name, fieldName, fieldType);
            setColumnType(cmap, columnType);
            columnMappings.add(cmap);
        }
    }

    private ArrayList<AbstractEventListenerMapping> parseEventListeners(Element classDescription, ArrayList<ColumnMapping> columnMappings) throws Exception {
        String name = getAttribute(classDescription, "name", true);
        ArrayList<AbstractEventListenerMapping> listeners = new ArrayList<AbstractEventListenerMapping>();
        Iterator<?> listenerElements = classDescription.getChildren("listener").iterator();
        while (listenerElements.hasNext()) {
            Element listenerElement = (Element) listenerElements.next();
            String event = getAttribute(listenerElement, "event", true);
            String id = getAttribute(listenerElement, "id");
            String isShared = getAttribute(listenerElement, "shared");
            if (id == null) {
                ObserverFactory observerFactory = getObserverFactory(listenerElement);
                listeners.add(new TransientEventListenerMapping(event, Class.forName(name), observerFactory, (isShared == null || isShared.equals("yes") || isShared.equals("true")) ? true : false));
            } else {
                String observerType = getAttribute(listenerElement, "type", true);
                Class<?> selectorClass = null;
                String selectorMethodName = null;
                Element selector = listenerElement.getChild("selector");
                if (selector != null) {
                    String selectorTypeName = getAttribute(selector, "type", true);
                    selectorClass = Class.forName(selectorTypeName);
                    selectorMethodName = getAttribute(selector, "method");
                    if (selectorMethodName == null) {
                        selectorMethodName = "isSelected";
                    }
                }
                listeners.add(new PersistentEventListenerMapping(event, columnMappings, Class.forName(name), Class.forName(observerType), id, selectorClass, selectorMethodName));
            }
        }
        return listeners;
    }

    private ObserverFactory getObserverFactory(Element listenerElement) throws PersistenceException, ClassNotFoundException, NoSuchMethodException {
        String observerTypeString = getAttribute(listenerElement, "type", false);
        if (observerTypeString != null) {
            return new ClassObserverFactory(observerTypeString);
        }
        String staticFactoryMethodString = getAttribute(listenerElement, "staticFactoryMethod", false);
        if (staticFactoryMethodString != null) {
            return new StaticFactoryMethodObserverFactory(staticFactoryMethodString);
        }
        throw new PersistenceException("<listener> element must have one of the following properties set" + ": 'type', 'staticFactoryMethod'");
    }

    private TransactionListenerConfiguration parseTransactionListener(Element element) throws PersistenceException {
        String event = getAttribute(element, "event", true);
        String observedClass = getAttribute(element, "observed-class", true);
        String listener = getAttribute(element, "listener", true);
        return new TransactionListenerConfigurationImpl(event, observedClass, listener);
    }

    private void parseClassTransactionListeners(Element classDescription) throws Exception {
        String name = getAttribute(classDescription, "name", true);
        Iterator<?> listenerItr = classDescription.getChildren("transaction-listener").iterator();
        while (listenerItr.hasNext()) {
            Element listenerDescription = (Element) listenerItr.next();
            TransactionListenerConfiguration config = parseTransactionListener(listenerDescription);
            config.setObservedClass(Class.forName(name));
            transactionListeners.add(config);
        }
    }

    protected InputStream getInputStream(String filename) throws PersistenceException {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(filename);
        if (in == null) {
            try {
                in = new FileInputStream(filename);
            } catch (IOException ex) {
                log.debug("Did not load InputStream as resource: filename=" + filename);
            }
        }
        if (in == null) {
            try {
                URL url = new URL(filename);
                in = url.openStream();
            } catch (Exception ex) {
                log.debug("Did not load InputStream as URL: filename=" + filename);
            }
        }
        return in;
    }

    private String getAttribute(Element element, String name) throws PersistenceException {
        return getAttribute(element, name, false);
    }

    private String getAttribute(Element element, String name, boolean required) throws PersistenceException {
        Attribute value = element.getAttribute(name);
        if (value == null && required) {
            throw new PersistenceException("Required mapping attribute missing: " + name);
        }
        return value == null ? null : value.getValue();
    }

    private String[] parseIds(String idString) {
        ArrayList<String> ids = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(idString, ",");
        while (tokenizer.hasMoreTokens()) {
            ids.add(tokenizer.nextToken());
        }
        return ids.toArray(new String[ids.size()]);
    }

    private void setFieldType(ColumnMapping mapping, String className, String fieldName, String fieldTypeName) throws Exception {
        if (fieldTypeName == null) {
            mapping.fieldType = findFieldClass(className, fieldName);
        } else {
            Object udt = getUserDefinedColumnType(fieldTypeName);
            if (udt instanceof ColumnConverter) {
                mapping.fieldType = ColumnMapping.SINGLE_COLUMN_SUBOBJECT;
                mapping.converter = (ColumnConverter) udt;
            } else if (udt instanceof List) {
                mapping.fieldType = ColumnMapping.SUBOBJECT;
                mapping.submapping = (List<?>) udt;
            } else {
                mapping.fieldType = convertJavaFieldTypeToClass(fieldTypeName);
            }
        }
    }

    private Class<?> findFieldClass(String className, String fieldName) throws Exception {
        Class<?> objectClass = Class.forName(className);
        Field field = null;
        try {
            field = objectClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException ex) {
        }
        if (field != null) {
            return field.getType();
        } else {
            BeanInfo beanInfo = Introspector.getBeanInfo(objectClass);
            PropertyDescriptor[] properties = beanInfo.getPropertyDescriptors();
            for (int i = 0; i < properties.length; i++) {
                if (properties[i].getName().equals(fieldName)) {
                    return properties[i].getPropertyType();
                }
            }
        }
        throw new IntrospectionException("Couldn't introspect property for " + className + "." + fieldName);
    }

    private Class<?> convertJavaFieldTypeToClass(String fieldTypeName) {
        if (fieldTypeName.equals("string")) {
            return String.class;
        } else if (fieldTypeName.equals("boolean")) {
            return Boolean.class;
        } else if (fieldTypeName.equals("char")) {
            return Character.class;
        } else if (fieldTypeName.equals("short")) {
            return Short.class;
        } else if (fieldTypeName.equals("long")) {
            return Long.class;
        } else if (fieldTypeName.equals("int")) {
            return Integer.class;
        } else if (fieldTypeName.equals("integer")) {
            return Integer.class;
        } else if (fieldTypeName.equals("float")) {
            return Float.class;
        } else if (fieldTypeName.equals("double")) {
            return Double.class;
        } else if (fieldTypeName.equals("boolean")) {
            return Boolean.class;
        } else if (fieldTypeName.equals("date")) {
            return Date.class;
        } else if (fieldTypeName.equals("object")) {
            return Object.class;
        } else {
            return Object.class;
        }
    }

    private void setColumnType(ColumnMapping mapping, String columnTypeName) {
        if (columnTypeName.equals("bit")) {
            mapping.columnType = java.sql.Types.BIT;
        } else if (columnTypeName.equals("boolean")) {
            mapping.columnType = java.sql.Types.BIT;
        } else if (columnTypeName.equals("tinyint")) {
            mapping.columnType = java.sql.Types.TINYINT;
        } else if (columnTypeName.equals("smallint")) {
            mapping.columnType = java.sql.Types.SMALLINT;
        } else if (columnTypeName.equals("int")) {
            mapping.columnType = java.sql.Types.INTEGER;
        } else if (columnTypeName.equals("integer")) {
            mapping.columnType = java.sql.Types.INTEGER;
        } else if (columnTypeName.equals("biginteger")) {
            mapping.columnType = java.sql.Types.BIGINT;
        } else if (columnTypeName.equals("float")) {
            mapping.columnType = java.sql.Types.FLOAT;
        } else if (columnTypeName.equals("real")) {
            mapping.columnType = java.sql.Types.REAL;
        } else if (columnTypeName.equals("double")) {
            mapping.columnType = java.sql.Types.DOUBLE;
        } else if (columnTypeName.equals("numeric")) {
            mapping.columnType = java.sql.Types.NUMERIC;
        } else if (columnTypeName.equals("decimal")) {
            mapping.columnType = java.sql.Types.DECIMAL;
        } else if (columnTypeName.equals("char")) {
            mapping.columnType = java.sql.Types.CHAR;
        } else if (columnTypeName.equals("varchar")) {
            mapping.columnType = java.sql.Types.VARCHAR;
        } else if (columnTypeName.equals("longvarchar")) {
            mapping.columnType = java.sql.Types.LONGVARCHAR;
        } else if (columnTypeName.equals("date")) {
            mapping.columnType = java.sql.Types.DATE;
        } else if (columnTypeName.equals("time")) {
            mapping.columnType = java.sql.Types.TIME;
        } else if (columnTypeName.equals("timestamp")) {
            mapping.columnType = java.sql.Types.TIMESTAMP;
        } else if (columnTypeName.equals("binary")) {
            mapping.columnType = java.sql.Types.BINARY;
        } else if (columnTypeName.equals("varbinary")) {
            mapping.columnType = java.sql.Types.VARBINARY;
        } else if (columnTypeName.equals("longvarbinary")) {
            mapping.columnType = java.sql.Types.LONGVARBINARY;
        } else if (columnTypeName.equals("blob")) {
            mapping.columnType = java.sql.Types.BLOB;
        } else if (columnTypeName.equals("clob")) {
            mapping.columnType = java.sql.Types.CLOB;
        } else {
            mapping.columnType = -1;
        }
    }

    public void addMapping(ObjectMapping mapping) {
        classMappings.put(mapping.getObjectClass(), new JdbcPersistentClass(databaseEngine, mapping));
    }

    public PersistentClass getPersistentClass(Class<?> objectClass) throws PersistenceException {
        PersistentClass pclass = searchForPersistentClass(objectClass);
        if (pclass == null) {
            throw new PersistenceException("no mapping for: " + objectClass.getName());
        }
        return pclass;
    }

    private PersistentClass searchForPersistentClass(Class<?> objectClass) throws PersistenceException {
        PersistentClass pclass = classMappings.get(objectClass);
        if (annotationProcessor.isEntity(objectClass)) {
            ObjectMapping objectMapping = annotationProcessor.getObjectMapping(objectClass);
            pclass = new JdbcPersistentClass(databaseEngine, objectMapping);
        }
        if (pclass == null) {
            pclass = loadPersistentClass(objectClass);
            if (pclass == null) {
                pclass = getAncestorPersistentClass(objectClass);
            }
            classMappings.put(objectClass, pclass);
        }
        return pclass;
    }

    private PersistentClass getAncestorPersistentClass(Class<?> objectClass) throws PersistenceException {
        objectClass = objectClass.getSuperclass();
        if (objectClass != null) {
            return searchForPersistentClass(objectClass);
        } else {
            return null;
        }
    }

    private PersistentClass loadPersistentClass(Class<?> objectClass) throws PersistenceException {
        String suffix = objectClass.getName().replace('.', '/') + ".xml";
        PersistentClass pclass = null;
        for (int i = 0; i < mappingDirectories.size() && pclass == null; i++) {
            String mappingDirectory = (String) mappingDirectories.get(i);
            String path = mappingDirectory + suffix;
            InputStream in = getInputStream(path);
            if (in != null) {
                ObjectMapping mapping = loadObjectMapping(path, in);
                if (mapping != null) {
                    pclass = new JdbcPersistentClass(databaseEngine, mapping);
                }
            }
        }
        return pclass;
    }

    private Object getUserDefinedColumnType(String fieldType) {
        Object udt = localUserTypes.get(fieldType);
        if (udt == null) {
            udt = globalUserTypes.get(fieldType);
        }
        return udt;
    }

    private void parseUserDefinedTypes(Map<String, Object> types, Element element) throws Exception {
        Iterator<?> mappingItr = element.getChildren("user-defined-type").iterator();
        while (mappingItr.hasNext()) {
            Element type = (Element) mappingItr.next();
            String name = getAttribute(type, "name", true);
            Element classDescription = type.getChild("class");
            String converterClassName = getAttribute(type, "converter", false);
            if (converterClassName != null) {
                HashMap<String, String> parameters = new HashMap<String, String>();
                Iterator<?> parameterElements = type.getChildren("parameter").iterator();
                while (parameterElements.hasNext()) {
                    Element parameterElement = (Element) parameterElements.next();
                    String parameterName = getAttribute(parameterElement, "name", true);
                    String value = getAttribute(parameterElement, "value", true);
                    if (name != null && value != null) {
                        parameters.put(parameterName, value);
                    } else {
                        throw new PersistenceException("invalid parameter for user-defined type: " + name + "=" + value);
                    }
                }
                try {
                    Class<?> converterClass = Class.forName(converterClassName);
                    Constructor<?> constructor = converterClass.getConstructor(new Class[] { String.class, Map.class });
                    if (constructor != null) {
                        types.put(name, constructor.newInstance(new Object[] { name, parameters }));
                    } else {
                        throw new PersistenceException("no map-based constructor for converter with parameters");
                    }
                } catch (NoSuchMethodException ex) {
                    throw new PersistenceException("missing (String,Map) constructor for user-defined type", ex);
                } catch (Exception ex) {
                    throw new PersistenceException("user type converter error", ex);
                }
            } else if (classDescription != null) {
                ArrayList<ColumnMapping> columnMappings = new ArrayList<ColumnMapping>();
                Iterator<?> fieldElements = classDescription.getChildren("field").iterator();
                while (fieldElements.hasNext()) {
                    Element field = (Element) fieldElements.next();
                    String fieldName = getAttribute(field, "name", true);
                    String fieldTypeName = getAttribute(field, "type");
                    Element sql = field.getChild("sql");
                    if (sql != null) {
                        parseSqlDescription(name, fieldName, fieldTypeName, sql, columnMappings, new ArrayList<RelationshipMapping>());
                    } else {
                        String columnName = fieldName;
                        ColumnMapping cmap = new ColumnMapping();
                        cmap.fieldName = fieldName;
                        cmap.columnName = columnName;
                        setFieldType(cmap, name, fieldName, fieldTypeName);
                        if (cmap.converter != null) {
                            cmap.columnType = cmap.converter.getSqlColumnType();
                        } else {
                            cmap.columnType = convertJavaClassToSqlType(cmap.fieldType);
                        }
                        columnMappings.add(cmap);
                    }
                }
                types.put(name, columnMappings);
            } else {
                throw new PersistenceException("no converter for user-defined type");
            }
        }
    }

    private ClassSelector parseClassSelector(Element classDescription) throws Exception {
        Element classSelectorElement = classDescription.getChild("class-selector");
        ClassSelector classSelector = null;
        if (classSelectorElement != null) {
            String classSelectorType = getAttribute(classSelectorElement, "class");
            if (classSelectorType != null) {
                Class<?> selectorClass = Class.forName(classSelectorType);
                if (ClassSelector.class.isAssignableFrom(selectorClass)) {
                    classSelector = (ClassSelector) selectorClass.newInstance();
                }
            } else {
                String classSelectorColumn = getAttribute(classSelectorElement, "column", true);
                if (classSelectorColumn != null) {
                    GenericClassSelector genericClassSelector = new GenericClassSelector();
                    genericClassSelector.setColumnName(classSelectorColumn);
                    String defaultClassName = getAttribute(classSelectorElement, "default-class");
                    if (defaultClassName != null) {
                        genericClassSelector.setDefaultClass(Class.forName(defaultClassName));
                    }
                    Iterator<?> candidateClasses = classSelectorElement.getChildren("use-class").iterator();
                    while (candidateClasses.hasNext()) {
                        Element useClass = (Element) candidateClasses.next();
                        String value = getAttribute(useClass, "value", true);
                        String className = getAttribute(useClass, "class", true);
                        if (value != null && className != null) {
                            genericClassSelector.addClass(value, Class.forName(className));
                        }
                    }
                    classSelector = genericClassSelector;
                }
            }
        }
        return classSelector;
    }

    public DatabaseEngine getDatabaseEngine() {
        return databaseEngine;
    }

    public void setDatabaseEngine(DatabaseEngine databaseEngine) {
        this.databaseEngine = databaseEngine;
    }

    public void dumpXML(Element e) throws IOException {
        XMLOutputter outputter = new XMLOutputter();
        outputter.setIndent(" ");
        outputter.setNewlines(true);
        outputter.setTextNormalize(true);
        outputter.output(e, System.out);
    }
}
