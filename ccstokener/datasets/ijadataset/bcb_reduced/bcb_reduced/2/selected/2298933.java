package com.daffodilwoods.daffodildb.server.datadictionarysystem;

import java.io.*;
import java.util.*;
import com.daffodilwoods.daffodildb.server.datasystem.interfaces.*;
import com.daffodilwoods.database.general.*;
import com.daffodilwoods.database.resource.*;

public class SystemTablesCreator {

    public static final Integer SQLIdentifierSize = new Integer(128);

    public static final Integer characterDataSize = new Integer(1026);

    public static final Integer viewDefinitionSize = new Integer(4096);

    public static final Integer dtdIdentifierSize = new Integer(257);

    public static final Integer serializeObjectDataSize = new Integer(12000);

    public static final String domainCatalog = "system";

    public static final String domainSchema = "information_schema";

    public static final String sqlIdentifier = "sql_Identifier";

    public static final String characterData = "character_data";

    public static final String cardinalNumber = "cardinal_number";

    public static final String timeStamp = "time_stamp";

    public static final String transactionIndexTable = "default_index_transactionId";

    public static final String sessionIndexTable = "default_index_sessionId";

    public static final String databaseInfoIndexTable = "databaseInfo";

    public static final String tableInfoIndexTable = "tableInfo";

    public static final String ColumnInfoIndexTable = "columnInfo";

    public static final String ClusterInfoIndexTable = "clusterInfo";

    static Properties properties;

    static HashMap structureMap;

    public static HashMap primaryKeymap;

    static {
        try {
            properties = new Properties();
            Class cl = Class.forName("com.daffodilwoods.daffodildb.server.datadictionarysystem.SystemTablesCreator");
            java.net.URL urlw = cl.getResource("/com/daffodilwoods/daffodildb/server/datadictionarysystem/systemtablesStructure.obj");
            ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(urlw.openStream()));
            structureMap = (HashMap) ois.readObject();
            ois.close();
            java.net.URL urlprimary = cl.getResource("/com/daffodilwoods/daffodildb/server/datadictionarysystem/systemtablesPrimaryKeys.obj");
            ObjectInputStream oisPrimary = new ObjectInputStream(new BufferedInputStream(urlprimary.openStream()));
            primaryKeymap = (HashMap) oisPrimary.readObject();
            oisPrimary.close();
            java.net.URL url = cl.getResource("/com/daffodilwoods/daffodildb/server/datadictionarysystem/systemtablesdefinition.properties");
            properties.load(url.openStream());
        } catch (IOException ex) {
        } catch (ClassNotFoundException ex) {
        }
    }

    public SystemTablesCreator() throws DException {
    }

    static String[] routine_columnUsage_primaryKeys() {
        return new String[] { "SPECIFIC_CATALOG", "SPECIFIC_SCHEMA", "SPECIFIC_NAME", "TABLE_CATALOG", "TABLE_SCHEMA", "TABLE_NAME", "COLUMN_NAME" };
    }

    static String[] routine_TableUsage_primaryKeys() {
        return new String[] { "SPECIFIC_CATALOG", "SPECIFIC_SCHEMA", "SPECIFIC_NAME", "TABLE_CATALOG", "TABLE_SCHEMA", "TABLE_NAME" };
    }

    public static String[] viewNames = { SystemTables.columns_ViewName.getIdentifier(), SystemTables.tables_ViewName.getIdentifier() };

    public static String[] tableNames = { SystemTables.characterSets_TableName.getIdentifier(), SystemTables.check_constraints_TableName.getIdentifier(), SystemTables.check_column_usage_TableName.getIdentifier(), SystemTables.check_table_usage_TableName.getIdentifier(), SystemTables.collations_TableName.getIdentifier(), SystemTables.column_privileges_TableName.getIdentifier(), SystemTables.columns_TableName.getIdentifier(), SystemTables.dataTypeDescriptor_TableName.getIdentifier(), SystemTables.domains_TableName.getIdentifier(), SystemTables.domain_constraints_TableName.getIdentifier(), SystemTables.key_column_usage_TableName.getIdentifier(), SystemTables.role_authorization_TableName.getIdentifier(), SystemTables.roles_TableName.getIdentifier(), SystemTables.referential_constraints_TableName.getIdentifier(), SystemTables.schema_TableName.getIdentifier(), SystemTables.table_constraints_TableName.getIdentifier(), SystemTables.table_privileges_TableName.getIdentifier(), SystemTables.tables_TableName.getIdentifier(), SystemTables.triggers_TableName.getIdentifier(), SystemTables.trigger_table_usage_TableName.getIdentifier(), SystemTables.trigger_column_usage_TableName.getIdentifier(), SystemTables.triggered_update_columns_TableName.getIdentifier(), SystemTables.users_TableName.getIdentifier(), SystemTables.usage_privileges_TableName.getIdentifier(), SystemTables.views_TableName.getIdentifier(), SystemTables.view_colum_usage_TableName.getIdentifier(), SystemTables.view_table_usage_TableName.getIdentifier(), SystemTables.transaction_TableName.getIdentifier(), SystemTables.parameters_TableName.getIdentifier(), SystemTables.routines_TableName.getIdentifier(), SystemTables.routine_privileges_TableName.getIdentifier(), SystemTables.INDEXINFO.getIdentifier(), SystemTables.INDEXCOLUMNS.getIdentifier(), SystemTables.FULLTEXTINFO.getIdentifier(), SystemTables.FULLTEXTCOLUMNINFO.getIdentifier(), SystemTables.DATABASEFULLTEXTINFO.getIdentifier(), SystemTables.DATABASEFULLTEXTCOLUMNINFO.getIdentifier(), SystemTables.user_defined_type_TableName.getIdentifier(), SystemTables.user_defined_type_privileges_TableName.getIdentifier(), SystemTables.method_specifications_TableName.getIdentifier(), SystemTables.method_specification_parameters_TableName.getIdentifier(), SystemTables.transforms_TableName.getIdentifier(), SystemTables.sequence_number_TableName.getIdentifier(), SystemTables.DATABASEINFO.getIdentifier(), SystemTables.TABLEINFO.getIdentifier(), SystemTables.COLUMNINFO.getIdentifier(), SystemTables.CLUSTERINFO.getIdentifier(), SystemTables.dualSystemTable.getIdentifier(), SystemTables.routine_table_usage_TableName.getIdentifier(), SystemTables.routine_column_usage_TableName.getIdentifier() };

    public static String[] getPrimaryKeys(QualifiedIdentifier tableName) throws DException {
        return (String[]) primaryKeymap.get(tableName);
    }

    public static Object[][] getTableStructure(QualifiedIdentifier tableName) throws DException {
        if (tableName.getName().startsWith("default_index")) {
            return DefaultIndexTableStructure();
        } else if (tableName.getName().equalsIgnoreCase(transactionIndexTable)) {
            return TransactionIndexTableStructure();
        } else if (tableName.getName().startsWith(sessionIndexTable)) {
            return SessionIndexTableStructure();
        } else if (tableName.getName().equalsIgnoreCase("dualSystemTable")) {
            return dualSystemTableStructure();
        } else if (tableName.getName().equalsIgnoreCase("dualSystemTable")) {
            return dualSystemTableStructure();
        } else {
            Object[][] asads = (Object[][]) structureMap.get(tableName);
            return asads;
        }
    }

    public static ArrayList getInformationSchemaStatements() throws DException {
        ArrayList informationSchemaStatement = new ArrayList(10);
        informationSchemaStatement.add("CREATE SCHEMA " + SystemTables.systemCatalog + ".INFORMATION_SCHEMA AUTHORIZATION _SYSTEM");
        informationSchemaStatement.add("CREATE DOMAIN " + SystemTables.systemCatalog + ".INFORMATION_SCHEMA.CARDINAL_NUMBER AS INTEGER " + "CONSTRAINT CARDINAL_NUMBER_DOMAIN_CHECK " + "CHECK ( VALUE >= 0 )");
        informationSchemaStatement.add("CREATE DOMAIN " + SystemTables.systemCatalog + ".INFORMATION_SCHEMA.CHARACTER_DATA AS " + "CHARACTER VARYING (" + characterDataSize + ") ");
        informationSchemaStatement.add("CREATE DOMAIN " + SystemTables.systemCatalog + ".INFORMATION_SCHEMA.SQL_IDENTIFIER AS " + "CHARACTER VARYING (" + SQLIdentifierSize + ") ");
        informationSchemaStatement.add(" CREATE DOMAIN " + SystemTables.systemCatalog + ".INFORMATION_SCHEMA.TIME_STAMP AS TIMESTAMP (2)");
        return informationSchemaStatement;
    }

    public static ArrayList getDefinitionSchemaStatement() throws DException {
        ArrayList tableDefinitionList = new ArrayList(50);
        tableDefinitionList.add("Create Schema " + SystemTables.systemCatalog + ".Definition_Schema authorization _SYSTEM");
        tableDefinitionList.add(properties.getProperty("UsersDefinition"));
        tableDefinitionList.add(properties.getProperty("RolesDefinition"));
        tableDefinitionList.add(properties.getProperty("Role_Authorization_DescriptorsDefinition"));
        tableDefinitionList.add(properties.getProperty("SchemataDefinition"));
        tableDefinitionList.add(properties.getProperty("Character_SetsDefinition"));
        tableDefinitionList.add(properties.getProperty("CollationsDefinition"));
        tableDefinitionList.add(properties.getProperty("DomainDefinition"));
        tableDefinitionList.add(properties.getProperty("TablesDefinition"));
        tableDefinitionList.add(properties.getProperty("ColumnsDefinition"));
        tableDefinitionList.add(properties.getProperty("Data_Type_DescriptorDefinition"));
        tableDefinitionList.add(properties.getProperty("Table_ConstraintsDefinition"));
        tableDefinitionList.add(properties.getProperty("Key_Column_UsageDefinition"));
        tableDefinitionList.add(properties.getProperty("Referential_ConstraintsDefinition"));
        tableDefinitionList.add(properties.getProperty("Check_ConstraintsDefinition"));
        tableDefinitionList.add(properties.getProperty("Check_Column_UsageDefinition"));
        tableDefinitionList.add(properties.getProperty("Check_Table_UsageDefinition"));
        tableDefinitionList.add(properties.getProperty("TriggersDefinition"));
        tableDefinitionList.add(properties.getProperty("Trigger_Table_UsageDefinition"));
        tableDefinitionList.add(properties.getProperty("Trigger_Column_UsageDefinition"));
        tableDefinitionList.add(properties.getProperty("Triggered_Update_ColumnsDefinition"));
        tableDefinitionList.add(properties.getProperty("ViewsDefinition"));
        tableDefinitionList.add(properties.getProperty("View_Column_UsageDefinition"));
        tableDefinitionList.add(properties.getProperty("View_Table_UsageDefinition"));
        tableDefinitionList.add(properties.getProperty("Domain_ConstraintsDefinition"));
        tableDefinitionList.add(properties.getProperty("Column_PrivilegesDefinition"));
        tableDefinitionList.add(properties.getProperty("Table_PrivilegesDefinition"));
        tableDefinitionList.add(properties.getProperty("Usage_PrivilegesDefinition"));
        tableDefinitionList.add(properties.getProperty("RoutinesDefinition"));
        tableDefinitionList.add(properties.getProperty("ParametersDefinition"));
        tableDefinitionList.add(properties.getProperty("Routine_PrivilegesDefinition"));
        tableDefinitionList.add(properties.getProperty("IndexInfoDefinition"));
        tableDefinitionList.add(properties.getProperty("IndexColumnsDefinition"));
        tableDefinitionList.add(properties.getProperty("FulltextindexDefinition"));
        tableDefinitionList.add(properties.getProperty("FulltextIndexColumnsDefinition"));
        tableDefinitionList.add(properties.getProperty("User_Defined_TypesDefinition"));
        tableDefinitionList.add(properties.getProperty("User_Defined_Type_PrivilegesDefinition"));
        tableDefinitionList.add(properties.getProperty("TransformDefinition"));
        tableDefinitionList.add(properties.getProperty("Method_SpecificationDefinition"));
        tableDefinitionList.add(properties.getProperty("Method_Specification_ParametersDefinition"));
        tableDefinitionList.add(properties.getProperty("Sequence_NumberDefinition"));
        return tableDefinitionList;
    }

    public static ArrayList getSystemViews() {
        ArrayList viewDefinitionList = new ArrayList(4);
        viewDefinitionList.add(properties.getProperty("TablesViewDefinition"));
        viewDefinitionList.add(properties.getProperty("ColumnsViewDefinition"));
        viewDefinitionList.add(properties.getProperty("GrantTablesViewQuery"));
        viewDefinitionList.add(properties.getProperty("GrantColumnsViewQuery"));
        return viewDefinitionList;
    }

    public static ArrayList getSystemTableIndexes() throws DException {
        ArrayList indexesList = new ArrayList(7);
        indexesList.add("create index psg_table_constraint_1 on " + SystemTables.table_constraints_TableName + " (table_catalog,table_schema,table_name,constraint_type,is_deferrable) ");
        indexesList.add("create index psg_key_column_usage_1 on " + SystemTables.key_column_usage_TableName + " (table_catalog,table_schema,table_name,column_name) ");
        indexesList.add("create index psg_domain_constraint_1 on " + SystemTables.domain_constraints_TableName + " (domain_catalog,domain_schema,domain_name) ");
        indexesList.add("create index psg_referential_constraint_1 on " + SystemTables.referential_constraints_TableName + " (unique_constraint_catalog,unique_constraint_schema,unique_constraint_name) ");
        indexesList.add("create index psg_triggers_1 on " + SystemTables.triggers_TableName + " (event_object_catalog,event_object_schema,event_object_table,event_manipulation,action_orientation,condition_timing) ");
        indexesList.add("create index psg_triggered_update_columns_1 on " + SystemTables.triggered_update_columns_TableName + " (event_object_catalog,event_object_schema,event_object_table) ");
        indexesList.add("create index psg_schemata_1 on " + SystemTables.schema_TableName + " (SCHEMA_OWNER) ");
        indexesList.add("create index psg_views_1 on " + SystemTables.views_TableName + " (TABLE_CATALOG, TABLE_SCHEMA, MATERIALIZED_TABLE_NAME ) ");
        return indexesList;
    }

    static Object[][] dualSystemTableStructure() {
        return new Object[][] { { "Column_name", new Long(Datatype.INT), new Integer(Datatype.INTSIZE), Boolean.TRUE, null, null, null } };
    }

    static Object[][] DefaultIndexTableStructure() {
        return new Object[][] { { "RecordClusterAddress", new Long(Datatype.LONG), new Integer(Datatype.LONGSIZE), Boolean.TRUE, null, null, null }, { "RecordNumber", new Long(Datatype.INTEGER), new Integer(Datatype.INTSIZE), Boolean.TRUE, null, null, null }, { "ClusterSize", new Long(Datatype.INTEGER), new Integer(Datatype.INTSIZE), Boolean.TRUE, null, null, null }, { "NextNodeAddress", new Long(Datatype.LONG), new Integer(Datatype.LONGSIZE), Boolean.TRUE, null, null, null }, { "NextNodeRecordNumber", new Long(Datatype.INTEGER), new Integer(Datatype.INTSIZE), Boolean.TRUE, null, null, null }, { "NextClusterSize", new Long(Datatype.INTEGER), new Integer(Datatype.INTSIZE), Boolean.TRUE, null, null, null }, { CharacteristicsConstants.systemFields[CharacteristicsConstants.rowId], new Long(Datatype.LONG), new Integer(Datatype.LONGSIZE), Boolean.TRUE, null, null, null }, { CharacteristicsConstants.systemFields[CharacteristicsConstants.transactionId], new Long(Datatype.LONG), new Integer(Datatype.LONGSIZE), Boolean.TRUE, null, null, null }, { CharacteristicsConstants.systemFields[CharacteristicsConstants.sessionId], new Long(Datatype.LONG), new Integer(Datatype.LONGSIZE), Boolean.TRUE, null, null, null } };
    }

    static Object[][] TransactionIndexTableStructure() {
        return new Object[][] { { "RecordClusterAddress", new Long(Datatype.LONG), new Integer(Datatype.LONGSIZE), Boolean.TRUE, null, null, null }, { "RecordNumber", new Long(Datatype.INTEGER), new Integer(Datatype.INTSIZE), Boolean.TRUE, null, null, null }, { "ClusterSize", new Long(Datatype.INTEGER), new Integer(Datatype.INTSIZE), Boolean.TRUE, null, null, null }, { "NextNodeAddress", new Long(Datatype.LONG), new Integer(Datatype.LONGSIZE), Boolean.TRUE, null, null, null }, { "NextNodeRecordNumber", new Long(Datatype.INTEGER), new Integer(Datatype.INTSIZE), Boolean.TRUE, null, null, null }, { "NextClusterSize", new Long(Datatype.INTEGER), new Integer(Datatype.INTSIZE), Boolean.TRUE, null, null, null }, { "SavedTransactionId", new Long(Datatype.LONG), new Integer(Datatype.LONGSIZE), Boolean.TRUE, null, null, null } };
    }

    static Object[][] SessionIndexTableStructure() {
        return new Object[][] { { "RecordClusterAddress", new Long(Datatype.LONG), new Integer(Datatype.LONGSIZE), Boolean.TRUE, null, null, null }, { "RecordNumber", new Long(Datatype.INTEGER), new Integer(Datatype.INTSIZE), Boolean.TRUE, null, null, null }, { "ClusterSize", new Long(Datatype.INTEGER), new Integer(Datatype.INTSIZE), Boolean.TRUE, null, null, null }, { "NextNodeAddress", new Long(Datatype.LONG), new Integer(Datatype.LONGSIZE), Boolean.TRUE, null, null, null }, { "NextNodeRecordNumber", new Long(Datatype.INTEGER), new Integer(Datatype.INTSIZE), Boolean.TRUE, null, null, null }, { "NextClusterSize", new Long(Datatype.INTEGER), new Integer(Datatype.INTSIZE), Boolean.TRUE, null, null, null }, { "SavedSessionId", new Long(Datatype.LONG), new Integer(Datatype.LONGSIZE), Boolean.TRUE, null, null, null } };
    }

    /**
    * New Method written by harvinder related to bug 11484. */
    public static void changeStructure(double version) throws DException {
        try {
            Class cl = Class.forName("com.daffodilwoods.daffodildb.server.datadictionarysystem.SystemTablesCreator");
            java.net.URL urlw = cl.getResource("/com/daffodilwoods/daffodildb/server/datadictionarysystem/systemtablesStructure.obj");
            ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(urlw.openStream()));
            structureMap = (HashMap) ois.readObject();
            if (version >= 3.4) {
                Object[][] columnsTableStructure = (Object[][]) structureMap.get((Object) SystemTables.columns_TableName);
                columnsTableStructure[9][2] = new Integer(1027);
            }
            ois.close();
        } catch (IOException ex) {
        } catch (ClassNotFoundException ex) {
        }
    }
}
