package de.ibis.permoto.loganalyzer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.prefs.Preferences;
import javax.sql.DataSource;
import javax.swing.tree.DefaultMutableTreeNode;
import org.apache.log4j.Logger;
import de.ibis.permoto.loganalyzer.db.JdbcDataSourceFactory;
import de.ibis.permoto.loganalyzer.db.JdbcUtils;
import de.ibis.permoto.loganalyzer.exception.JdbcNoConnection;
import de.ibis.permoto.model.basic.applicationmodel.ApplicationModel;
import de.ibis.permoto.model.basic.applicationmodel.ChildItem;
import de.ibis.permoto.model.basic.applicationmodel.Configuration;
import de.ibis.permoto.model.basic.applicationmodel.Item;
import de.ibis.permoto.model.basic.applicationmodel.Workflow;
import de.ibis.permoto.model.basic.applicationmodel.WorkflowInstance;
import de.ibis.permoto.model.util.ModelPersistenceManager;
import de.ibis.permoto.util.PermotoPreferences;

/**
 * holds workflow structure for use in logAnalyzer and GUI
 * @author Andreas Schamberger
 */
public class WorkflowStructure {

    /** serial version uid */
    private static final long serialVersionUID = 1L;

    /** Logger for this class */
    private static final Logger logger = Logger.getLogger(WorkflowStructure.class);

    /** table names table name */
    private static final String TABLE_NAME_TABLE = "TABLE_NAME";

    /** singelton instance */
    private static WorkflowStructure instance;

    /** DB DataSource */
    private DataSource ds;

    /** DB Connection */
    private Connection con;

    /** table names */
    private HashMap<String, String> tableNames;

    /** table names by application */
    private HashMap<String, HashMap<String, String>> tableNamesByApplication;

    /** application name mapping to real name */
    private HashMap<String, String> applicationName;

    /** table name extension for queue data table */
    public static final String ERROR_EXT = "_E";

    /** name of currently active application model */
    private String activeApplicationModel;

    /** tree map for workflow structure */
    private DefaultMutableTreeNode activeWorkflowStructure;

    /** contains the tree structures of the application models */
    private HashMap<String, DefaultMutableTreeNode> treeStructures;

    /** contains the tree structures of the save application models */
    private HashMap<String, DefaultMutableTreeNode> saveTreeStructures;

    /** contains the tree structures of the normal application models */
    private HashMap<String, DefaultMutableTreeNode> normalTreeStructures;

    /** contains the application configurations */
    private HashMap<String, Configuration> configurations;

    /** contains the application prefixes */
    private String[] prefixes;

    /** contains the tree structures of the normal application models */
    private HashMap<String, HashMap<String, Float>> workflowInstances;

    /**
     * get concrete instance of this class
     * @return WorkflowStructure instance
     */
    public static WorkflowStructure getInstance() {
        return getInstance(false);
    }

    /**
     * get concrete instance of this class
     * @param reloadData reload data from db
     * @return WorkflowStructure instance
     */
    public static WorkflowStructure getInstance(boolean reloadData) {
        if (WorkflowStructure.instance == null) {
            WorkflowStructure.instance = new WorkflowStructure();
        } else if (true == reloadData) {
            WorkflowStructure.instance.init();
        }
        return WorkflowStructure.instance;
    }

    /**
     * Constructor
     */
    private WorkflowStructure() {
        init();
    }

    /**
     * init object
     */
    private void init() {
        tableNames = new HashMap<String, String>();
        tableNamesByApplication = new HashMap<String, HashMap<String, String>>();
        boolean check = true;
        try {
            ds = JdbcDataSourceFactory.setupDataSource("logs", true, true);
            try {
                con = ds.getConnection();
            } catch (SQLException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Can't establish DB connection!", e);
                } else {
                    logger.info("Can't establish DB connection!");
                }
                check = false;
            }
        } catch (JdbcNoConnection e) {
            logger.info("No valid config for DB connection!");
            check = false;
        }
        if (check) {
            createTableNameTable();
            getTableNamesFromDb();
        }
        applicationName = new HashMap<String, String>();
        treeStructures = new LinkedHashMap<String, DefaultMutableTreeNode>();
        normalTreeStructures = new LinkedHashMap<String, DefaultMutableTreeNode>();
        saveTreeStructures = new LinkedHashMap<String, DefaultMutableTreeNode>();
        configurations = new HashMap<String, Configuration>();
        workflowInstances = new HashMap<String, HashMap<String, Float>>();
        String[] pams = null;
        try {
            pams = getPamFiles();
        } catch (IOException e1) {
        }
        prefixes = new String[pams.length];
        int index = 0;
        for (String pam : pams) {
            InputStream is = getClass().getResourceAsStream(pam);
            ApplicationModel am = ModelPersistenceManager.loadApplicationModel(is);
            createTreeModel(am);
            prefixes[index] = am.getConfiguration().getDbTableNamePrefix();
            index++;
            configurations.put(am.getApplicationName(), am.getConfiguration());
            applicationName.put(am.getApplicationName(), am.getApplicationName());
            if (am.getApplicationNameSave() != null) {
                configurations.put(am.getApplicationNameSave(), am.getConfiguration());
                applicationName.put(am.getApplicationNameSave(), am.getApplicationName());
            }
            if (activeApplicationModel == null) {
                setActiveApplicationModel(am.getApplicationName());
            }
        }
        try {
            if (null != con) {
                con.close();
            }
        } catch (SQLException e) {
            logger.error("SQLException!", e);
        }
    }

    /**
     * create table name table if necessary
     * and insert all tablenames
     */
    public void generateTableNameTable() {
        init();
    }

    /**
     * create table name table if necessary
     * @return new table created
     */
    private boolean createTableNameTable() {
        try {
            if (null != con && !JdbcUtils.tableExists(con, TABLE_NAME_TABLE)) {
                Statement stmt = con.createStatement();
                logger.debug("createTableNameTable() - create new " + "table " + TABLE_NAME_TABLE + " .... ");
                String query;
                if (JdbcDataSourceFactory.getConnectionUrl("logs").startsWith("jdbc:sqlite")) {
                    query = "CREATE TABLE " + TABLE_NAME_TABLE + " (" + " id INTEGER PRIMARY KEY AUTOINCREMENT," + " application VARCHAR(50)," + " object_name VARCHAR(100)," + " table_name VARCHAR(100)" + " )";
                } else if (JdbcDataSourceFactory.getConnectionUrl("logs").startsWith("jdbc:mysql")) {
                    query = "CREATE TABLE " + TABLE_NAME_TABLE + " (" + " id INTEGER PRIMARY KEY AUTO_INCREMENT," + " application VARCHAR(50)," + " object_name VARCHAR(100)," + " table_name VARCHAR(100)" + " )";
                } else {
                    query = "CREATE TABLE " + TABLE_NAME_TABLE + " (" + " id INTEGER PRIMARY KEY," + " application VARCHAR(50)," + " object_name VARCHAR(100)," + " table_name VARCHAR(100)" + " )";
                }
                stmt.executeUpdate(query);
                query = "CREATE SEQUENCE " + TABLE_NAME_TABLE + "_SEQUENCE" + " START WITH 1" + " INCREMENT BY 1";
                stmt.executeUpdate(query);
                query = "CREATE OR REPLACE TRIGGER " + TABLE_NAME_TABLE + "_TRIGGER" + " BEFORE INSERT ON " + TABLE_NAME_TABLE + "\n" + " REFERENCING NEW AS NEW\n" + " FOR EACH ROW\n" + " BEGIN\n" + " SELECT " + TABLE_NAME_TABLE + "_SEQUENCE.nextval" + " INTO :NEW.ID FROM dual;\n" + " END;";
                stmt.executeUpdate(query);
                stmt.close();
                return true;
            } else {
                logger.debug("createTableNameTable() - found table " + TABLE_NAME_TABLE + "!");
                return false;
            }
        } catch (SQLException e) {
            logger.error("createTableNameTable()", e);
        }
        return false;
    }

    private String insertNewObjectInTable(String applicationName, String objectName, String tableName) {
        try {
            if (null != con) {
                Statement stmt = con.createStatement();
                logger.debug("insertNewObjectInTable() - create new " + "object in table " + TABLE_NAME_TABLE + " .... ");
                String query;
                query = "INSERT INTO " + TABLE_NAME_TABLE + " (application, object_name, table_name) VALUES " + "('" + applicationName + "','" + objectName + "', '" + tableName + "_')";
                stmt.executeUpdate(query);
                if (JdbcDataSourceFactory.getConnectionUrl("logs").startsWith("jdbc:sqlite")) {
                    query = "UPDATE " + TABLE_NAME_TABLE + " SET table_name=(table_name||id) " + "WHERE object_name='" + objectName + "'";
                } else {
                    query = "UPDATE " + TABLE_NAME_TABLE + " SET table_name=CONCAT(table_name,id) " + "WHERE object_name='" + objectName + "'";
                }
                stmt.executeUpdate(query);
                query = "SELECT table_name FROM " + TABLE_NAME_TABLE + " WHERE object_name='" + objectName + "'";
                stmt.executeQuery(query);
                ResultSet rs = stmt.executeQuery(query);
                rs.next();
                tableName = rs.getString("table_name");
                rs.close();
                stmt.close();
                return tableName;
            }
        } catch (SQLException e) {
            logger.error("insertNewObjectInTable()", e);
        }
        return null;
    }

    /**
     * get existing table name rows for given application
     * @param applicationName
     */
    private void getTableNamesFromDb() {
        try {
            if (null != con) {
                String query = "SELECT" + " object_name, table_name, application" + " FROM " + TABLE_NAME_TABLE;
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(query);
                while (rs.next()) {
                    tableNames.put(rs.getString("object_name"), rs.getString("table_name"));
                    if (tableNamesByApplication.containsKey(rs.getString("application"))) {
                        tableNamesByApplication.get(rs.getString("application")).put(rs.getString("object_name"), rs.getString("table_name"));
                    } else {
                        HashMap<String, String> create = new HashMap<String, String>(100);
                        create.put(rs.getString("object_name"), rs.getString("table_name"));
                        tableNamesByApplication.put(rs.getString("application"), create);
                    }
                }
                stmt.close();
            }
        } catch (SQLException e) {
            logger.error("SQLException", e);
        }
    }

    /**
     * get all application models' names
     * @return array of application names
     */
    public String[] getApplicationModelNames() {
        return treeStructures.keySet().toArray(new String[0]);
    }

    /**
     * get save application models' names
     * @return array of application names
     */
    public String[] getSaveApplicationModelNames() {
        return saveTreeStructures.keySet().toArray(new String[0]);
    }

    /**
     * get normal application models' names
     * @return array of application names
     */
    public String[] getNormalApplicationModelNames() {
        return normalTreeStructures.keySet().toArray(new String[0]);
    }

    /**
     * get active application model's name
     * @return string with application name
     */
    public String getActiveApplicationModel() {
        return activeApplicationModel;
    }

    /**
     * get active application model's real name
     * @return string with application name
     */
    public String getActiveApplicationModelRealName() {
        return applicationName.get(activeApplicationModel);
    }

    /**
     * set active application model
     * @param name
     */
    public void setActiveApplicationModel(String name) {
        if (treeStructures.containsKey(name)) {
            activeApplicationModel = name;
            activeWorkflowStructure = treeStructures.get(name);
            setApplicationPreferences(configurations.get(name));
        }
    }

    /**
     * get tree with all workflows, items, services
     * @return tree of DefaultMutableTreeNode
     */
    public DefaultMutableTreeNode getWorkflowStructure() {
        return activeWorkflowStructure;
    }

    /**
     * get tree for one workflow, identified by WorkflowObject
     * @param workflow
     * @return tree of DefaultMutableTreeNode
     */
    public DefaultMutableTreeNode getWorkflowStructure(WorkflowObject workflow) {
        int num = activeWorkflowStructure.getChildCount();
        for (int i = 0; i < num; i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) activeWorkflowStructure.getChildAt(i);
            WorkflowObject wo = (WorkflowObject) (node.getUserObject());
            if (wo.equals(workflow)) {
                return node;
            }
        }
        return null;
    }

    /**
     * get Array with names of ErrorTables for the active application
     * @return String array with the ErrorTables for the active Application
     */
    public String[] getErrorTableNames() {
        int num = activeWorkflowStructure.getChildCount();
        String errorTables[] = new String[num];
        for (int i = 0; i < num; i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) activeWorkflowStructure.getChildAt(i);
            WorkflowObject wo = (WorkflowObject) (node.getUserObject());
            errorTables[i] = wo.dbTableError;
        }
        return errorTables;
    }

    /**
     * Sort given HashSet with {@link WorkflowObject}s according to workflow
     * structure.
     * @param workflowObjects
     * @return HashSet containing the sorted WorkflowObjects
     */
    public HashSet<WorkflowObject> sortWorkflowObjectsByTreeStructure(HashSet<WorkflowObject> workflowObjects) {
        HashSet<WorkflowObject> sortedWorkflowObjects = new LinkedHashSet<WorkflowObject>();
        int num = activeWorkflowStructure.getChildCount();
        for (int i = 0; i < num; i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) activeWorkflowStructure.getChildAt(i);
            WorkflowObject wo = (WorkflowObject) (node.getUserObject());
            if (workflowObjects.contains(wo)) {
                sortedWorkflowObjects.add(wo);
            }
        }
        return sortedWorkflowObjects;
    }

    private void createTreeModel(ApplicationModel am) {
        List<Item> ItemList = am.getItemSection().getItem();
        HashMap<String, Item> itemMap = new HashMap<String, Item>(ItemList.size());
        for (Item item : ItemList) {
            itemMap.put(item.getRealName(), item);
        }
        String objectNamePrefix = am.getApplicationName().replace(" ", "-");
        String tableNamePrefix = am.getConfiguration().getDbTableNamePrefix().replace(" ", "-");
        if (tableNamePrefix.length() > 10) {
            tableNamePrefix = tableNamePrefix.substring(0, 10);
        }
        DefaultMutableTreeNode treeRootReal = null;
        DefaultMutableTreeNode treeRootSave = null;
        treeRootReal = new DefaultMutableTreeNode("All Workflows");
        if (am.getApplicationNameSave() != null && !am.getApplicationNameSave().equals("")) {
            treeRootSave = new DefaultMutableTreeNode("All Workflows");
        }
        String applicationName = am.getApplicationName();
        String objectName;
        String tableNamePrefixWorkflow;
        String tableName;
        String tableNameQueuing;
        String tableNameError;
        String dbTableName;
        WorkflowObject wo;
        DefaultMutableTreeNode treeNodeReal = null;
        DefaultMutableTreeNode treeNodeSave = null;
        List<Workflow> WorkflowList = am.getWorkflowSection().getWorkflow();
        for (Workflow workflow : WorkflowList) {
            objectName = objectNamePrefix + "_" + workflow.getRealName().replace(" ", "-");
            tableNamePrefixWorkflow = tableNamePrefix;
            if (workflow.getDbTableName() != null && !workflow.getDbTableName().equals("")) {
                tableNamePrefixWorkflow += "_" + workflow.getDbTableName().replace(" ", "-").toUpperCase();
                if (tableNamePrefixWorkflow.length() > 22) {
                    tableNamePrefixWorkflow = tableNamePrefixWorkflow.substring(0, 22);
                }
            }
            if (tableNames.containsKey(objectName)) {
                tableName = tableNames.get(objectName);
            } else {
                tableName = tableNamePrefixWorkflow;
                tableName = insertNewObjectInTable(applicationName, objectName, tableName);
                tableNames.put(objectName, tableName);
            }
            tableNameQueuing = null;
            tableNameError = tableName + ERROR_EXT;
            dbTableName = workflow.getDbTableName().toUpperCase();
            String dbPrefix = am.getConfiguration().getDbTableNamePrefix().toUpperCase();
            wo = new WorkflowObject(0, "w", objectName, workflow.getRealName(), workflow.getIdentifier(), tableName, tableNameQueuing, tableNameError, dbTableName, dbPrefix, workflow.getStaticDelayTime());
            treeNodeReal = new DefaultMutableTreeNode(wo);
            treeRootReal.add(treeNodeReal);
            if (treeRootSave != null) {
                if (workflow.getSaveName() != null && !workflow.getSaveName().equals("undefined")) {
                    wo = new WorkflowObject(0, "w", objectName, workflow.getSaveName(), workflow.getIdentifier(), tableName, tableNameQueuing, tableNameError, dbTableName, dbPrefix, workflow.getStaticDelayTime());
                }
                treeNodeSave = new DefaultMutableTreeNode(wo);
                treeRootSave.add(treeNodeSave);
            }
            List<WorkflowInstance> instances = workflow.getWorkflowInstance();
            if (instances.size() > 0) {
                for (WorkflowInstance wi : instances) {
                    if (!workflowInstances.containsKey(wo.objectName)) {
                        workflowInstances.put(wo.objectName, new LinkedHashMap<String, Float>(instances.size()));
                    }
                    workflowInstances.get(wo.objectName).put(wi.getRealName(), wi.getFraction());
                }
            }
            List<ChildItem> children = workflow.getChildItem();
            if (children.size() > 0) {
                addChildItems(treeNodeReal, treeNodeSave, children, itemMap, objectName, tableNamePrefixWorkflow, applicationName, dbTableName, dbPrefix, 1);
            }
        }
        treeStructures.put(am.getApplicationName(), treeRootReal);
        normalTreeStructures.put(am.getApplicationName(), treeRootReal);
        if (treeRootSave != null) {
            treeStructures.put(am.getApplicationNameSave(), treeRootSave);
            saveTreeStructures.put(am.getApplicationNameSave(), treeRootSave);
        }
    }

    private void addChildItems(DefaultMutableTreeNode parentTreeNodeReal, DefaultMutableTreeNode parentTreeNodeSave, List<ChildItem> children, HashMap<String, Item> itemMap, String objectNamePrefix, String tableNamePrefix, String applicationName, String dbTableName, String dbPrefix, Integer level) {
        String objectName;
        String tableName;
        String tableNameQueuing;
        String tableNameError;
        WorkflowObject wo;
        DefaultMutableTreeNode treeNodeReal = null;
        DefaultMutableTreeNode treeNodeSave = null;
        WorkflowObject prevItem = null;
        HashMap<String, Integer> multipleCalls = new HashMap<String, Integer>(children.size());
        for (ChildItem child : children) {
            String childName = child.getRealName();
            if (multipleCalls.containsKey(childName)) {
                Integer count = multipleCalls.get(childName);
                multipleCalls.put(childName, ++count);
            } else {
                multipleCalls.put(childName, 1);
            }
        }
        HashMap<String, Integer> multipleCallsCounter = new HashMap<String, Integer>(children.size());
        for (ChildItem child : children) {
            String childName = child.getRealName();
            if (multipleCallsCounter.containsKey(childName)) {
                Integer count = multipleCallsCounter.get(childName);
                multipleCallsCounter.put(childName, ++count);
            } else {
                multipleCallsCounter.put(childName, 1);
            }
            Item item = itemMap.get(child.getRealName());
            objectName = objectNamePrefix + "_" + item.getRealName().replace(" ", "-");
            if (tableNames.containsKey(objectName)) {
                tableName = tableNames.get(objectName);
            } else {
                tableName = tableNamePrefix;
                tableName = insertNewObjectInTable(applicationName, objectName, tableName);
                tableNames.put(objectName, tableName);
            }
            if (prevItem != null && prevItem.isDiffStation) {
                tableNameQueuing = prevItem.dbTable;
            } else {
                tableNameQueuing = null;
            }
            Boolean hasMultipleCalls = false;
            Integer callPosition = 1;
            if (multipleCalls.get(child.getRealName()) > 1) {
                hasMultipleCalls = true;
                callPosition = multipleCallsCounter.get(child.getRealName());
            }
            tableNameError = tableName + ERROR_EXT;
            String workflowInstances = child.getWorkflowInstances();
            if (child.getWorkflowInstances() != null && child.getWorkflowInstances().trim().equals("")) {
                workflowInstances = null;
            }
            wo = new WorkflowObject(level, "i", objectName, item.getRealName(), item.getIdentifier(), item.getDefaultServiceTime(), item.getDefaultQueueLength(), item.getDefaultNrParallelWorkers(), tableName, tableNameQueuing, tableNameError, dbTableName, dbPrefix, item.isIsDiffStation(), item.isIsEndStation(), item.getStaticDelayTime(), hasMultipleCalls, callPosition, workflowInstances);
            treeNodeReal = new DefaultMutableTreeNode(wo);
            parentTreeNodeReal.add(treeNodeReal);
            if (parentTreeNodeSave != null) {
                if (item.getSaveName() != null && !item.getSaveName().equals("undefined")) {
                    wo = new WorkflowObject(level, "i", objectName, item.getSaveName(), item.getIdentifier(), item.getDefaultServiceTime(), item.getDefaultQueueLength(), item.getDefaultNrParallelWorkers(), tableName, tableNameQueuing, tableNameError, dbTableName, dbPrefix, item.isIsDiffStation(), item.isIsEndStation(), item.getStaticDelayTime(), hasMultipleCalls, callPosition, workflowInstances);
                }
                treeNodeSave = new DefaultMutableTreeNode(wo);
                parentTreeNodeSave.add(treeNodeSave);
            }
            prevItem = wo;
            List<ChildItem> nodeChildren = child.getChildItem();
            if (nodeChildren.size() > 0) {
                addChildItems(treeNodeReal, treeNodeSave, nodeChildren, itemMap, objectName, tableNamePrefix, applicationName, dbTableName, dbPrefix, level + 1);
            }
        }
    }

    /**
     * Set preferences for given Configuration
     * @param config
     */
    private void setApplicationPreferences(Configuration config) {
        PermotoPreferences.resetApplicationPreferences();
        Preferences prefs = PermotoPreferences.getApplicationPreferences().node("/de/ibis/permoto/loganalyzer/ParameterCalculator");
        if (config.getDefaultServiceTime() != null) {
            prefs.putFloat("DefaultServiceTime", config.getDefaultServiceTime());
        }
        if (config.getDefaultQueueLength() != null) {
            prefs.putInt("DefaultQueueLength", config.getDefaultQueueLength());
        }
        if (config.getDefaultNrParallelWorkers() != null) {
            prefs.putInt("DefaultNrParallelWorkers", config.getDefaultNrParallelWorkers());
        }
        if (config.getDefaultRoutingAlgorithm() != null) {
            prefs.put("DefaultRoutingAlgorithm", config.getDefaultRoutingAlgorithm().value());
        }
        if (config.getDefaultQueueingDiscipline() != null) {
            prefs.put("DefaultQueueingDiscipline", config.getDefaultQueueingDiscipline().value());
        }
        if (config.getDefaultDropRule() != null) {
            prefs.put("DefaultDropRule", config.getDefaultDropRule().value());
        }
        if (config.getDefaultArrivalRateDistribution() != null) {
            prefs.put("DefaultArrivalRateDistribution", config.getDefaultArrivalRateDistribution().value());
        }
        if (config.getDefaultServiceTimeDistribution() != null) {
            prefs.put("DefaultServiceTimeDistribution", config.getDefaultServiceTimeDistribution().value());
        }
        PermotoPreferences.reloadGlobalPreferences();
    }

    /**
     * get table name prefix
     * @return table name prefix
     */
    public String getTableNamePrefix() {
        Configuration config = configurations.get(activeApplicationModel);
        return config.getDbTableNamePrefix();
    }

    /**
     * get table name prefixes
     * @return table name prefixes
     */
    public String[] getPrefixes() {
        return prefixes;
    }

    /**
     * get name of active importer strategy -- activeApplicationModel from configurations
     * @return String the name of the active importer strategy
     */
    public String getNameOfActiveImporterStrategy() {
        Configuration config = configurations.get(activeApplicationModel);
        return config.getImporter();
    }

    /**
     * get name of active loader strategy -- activeApplicationModel from configurations
     * @return String the name of the active loader strategy
     */
    public String getNameOfActiveLoaderStrategy() {
        Configuration config = configurations.get(activeApplicationModel);
        return config.getLoader();
    }

    /**
     * get table name for object
     * @param objectName
     * @return table name
     */
    public String getTableName(String objectName) {
        return tableNames.get(objectName);
    }

    /**
     * get table names for all objects
     * @return table names
     */
    public String[] getTableNames() {
        return tableNames.values().toArray(new String[0]);
    }

    /**
     * get object names for application
     * @return object names
     */
    public String[] getObjectNames() {
        return tableNames.keySet().toArray(new String[0]);
    }

    /**
     * get table names for application
     * @param application
     * @return table names
     */
    public String[] getTableNames(String application) {
        application = applicationName.get(application);
        return tableNamesByApplication.get(application).values().toArray(new String[0]);
    }

    /**
     * get object names for application
     * @param application
     * @return object names
     */
    public String[] getObjectNames(String application) {
        application = applicationName.get(application);
        logger.debug(application);
        HashMap<String, String> tables = tableNamesByApplication.get(application);
        return tables.keySet().toArray(new String[0]);
    }

    /**
     * Get array of PAM files.
     *
     * @return
     * @throws IOException
     */
    private String[] getPamFiles() throws IOException {
        URL url = WorkflowStructure.class.getResource("/de/ibis/permoto/loganalyzer/pam");
        Set<String> result = new LinkedHashSet<String>(8);
        if (url.getProtocol().equals("jar")) {
            URLConnection con = url.openConnection();
            JarURLConnection jarCon = (JarURLConnection) con;
            JarFile jarFile = jarCon.getJarFile();
            JarEntry jarEntry = jarCon.getJarEntry();
            String rootEntryPath = (jarEntry != null ? jarEntry.getName() : "");
            rootEntryPath = rootEntryPath + "/";
            for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements(); ) {
                JarEntry entry = entries.nextElement();
                String entryPath = entry.getName();
                if (entryPath.startsWith(rootEntryPath)) {
                    if (entryPath.endsWith(".pam")) {
                        result.add("/" + entryPath);
                    }
                }
            }
        } else {
            String rootEntryPath = url.getFile();
            File dir = new File(url.getFile());
            File[] dirContents = dir.listFiles();
            for (int i = 0; i < dirContents.length; i++) {
                File content = dirContents[i];
                if (content.getName().endsWith(".pam")) {
                    String relativePath = content.getAbsolutePath().substring(rootEntryPath.length());
                    result.add("/de/ibis/permoto/loganalyzer/pam/" + relativePath.replace(File.separatorChar, '/'));
                }
            }
        }
        return result.toArray(new String[result.size()]);
    }

    /**
     * Gets HashMap with workflow instances for given workflow.
     *
     * @param wo
     * @return HashMap with workflow instances
     */
    public HashMap<String, Float> getWorkflowInstances(WorkflowObject wo) {
        String objectName = wo.objectName;
        if (workflowInstances.containsKey(objectName)) {
            return workflowInstances.get(objectName);
        }
        HashMap<String, Float> workflowInstances = new HashMap<String, Float>();
        workflowInstances.put(null, 1.0f);
        return workflowInstances;
    }
}
