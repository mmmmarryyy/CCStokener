package com.apelon.dts.db.admin.migrate.from32to33.config;

import java.io.*;
import java.net.URL;
import java.util.Map;
import java.sql.Connection;
import com.apelon.common.sql.SQL;
import com.apelon.common.util.db.DBSystemConfig;
import com.apelon.common.log4j.*;

public class DBMigrateConfig implements DBSystemConfig {

    private static final String DTD = "dtsconfig.dtd";

    private static final String KB_MIGRATE_LOG_CONFIG = "kbmigratelog.xml";

    public DBMigrateConfig() {
    }

    public void init(Connection source, Connection target) throws Exception {
        Categories.app().info("Performing migration from version 3.2 to version 3.3.");
        boolean apelonVersionExist = SQL.checkTableExists(target, "APELON_VERSION");
        if (apelonVersionExist) {
            throw new Exception("Current DTS schema version is 3.3. No migration is required from 3.2 to 3.3.");
        }
    }

    public String getDaoDTDFileName() {
        return DTD;
    }

    public String getDaoDTDURL() {
        return "http://apelon.com/dtd/dts/" + DTD;
    }

    public Class getDaoDTDClass() {
        return com.apelon.dts.db.admin.migrate.from32to33.config.DBMigrateConfig.class;
    }

    public InputStream getDaoConfig(String connectionType) throws IOException {
        URL url = null;
        if (connectionType.equals(SQL.ORACLE)) {
            url = DBMigrateConfig.class.getResource("oracle.xml");
        } else if (connectionType.equals(SQL.SQL2K)) {
            url = DBMigrateConfig.class.getResource("sql2k.xml");
        }
        return url.openStream();
    }

    public InputStream getTableLists() throws IOException {
        URL url = DBMigrateConfig.class.getResource("dbmigratetable.xml");
        return url.openStream();
    }

    public LogConfigLoader logConfigLoader() {
        return new LogConfigLoader(getKBMigrateDir(), KB_MIGRATE_LOG_CONFIG, DBMigrateConfig.class);
    }

    public void validateInput(Map map) throws Exception {
    }

    public String[] getConnectionProperties() {
        return null;
    }

    private static final String getKBMigrateDir() {
        return "kb" + File.separator + "migrate" + File.separator;
    }
}
