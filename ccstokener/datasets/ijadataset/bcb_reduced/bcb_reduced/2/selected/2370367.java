package com.apelon.dts.db.admin.migrate.from33to34.config;

import com.apelon.common.log4j.*;
import com.apelon.common.sql.SQL;
import com.apelon.common.util.db.DBSystemConfig;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

/**
 * Configuration class for migrating DTS schema from 3.3 to 3.4
 */
public class DBMigrateConfig implements DBSystemConfig {

    private static final String DTD = "dtsconfig.dtd";

    private static final String KB_MIGRATE_LOG_CONFIG = "kbmigratelog.xml";

    private String APELON_VERSION_TABLE = "APELON_VERSION";

    public DBMigrateConfig() {
    }

    public void init(Connection source, Connection target) throws Exception {
        boolean apelonVersionExist = SQL.checkTableExists(target, APELON_VERSION_TABLE);
        if (!apelonVersionExist) {
            throw new Exception("Please migrate the DTS Schema to 3.3 and then migrate to 3.4");
        }
        Categories.app().info("Performing migration from version 3.3 to version 3.4.");
        String version = getSchemaVersion(target);
        if (version.equals("3.4")) {
            throw new Exception("Current DTS schema version is 3.4. No migration is required from 3.3 to 3.4.");
        }
    }

    private String getSchemaVersion(Connection conn) throws Exception {
        String version = "";
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            String sql = "SELECT version FROM " + APELON_VERSION_TABLE + " WHERE app = 'dts'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                version = rs.getString(1);
            }
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
        return version;
    }

    public String getDaoDTDFileName() {
        return DTD;
    }

    public String getDaoDTDURL() {
        return "http://apelon.com/dtd/dts/" + DTD;
    }

    public Class getDaoDTDClass() {
        return com.apelon.dts.db.admin.migrate.from33to34.config.DBMigrateConfig.class;
    }

    public InputStream getDaoConfig(String connectionType) throws IOException {
        URL url = null;
        if (connectionType.equals(SQL.ORACLE)) {
            url = getDaoDTDClass().getResource("oracle.xml");
        } else if (connectionType.equals(SQL.SQL2K)) {
            url = getDaoDTDClass().getResource("sql2k.xml");
        }
        return url.openStream();
    }

    public InputStream getTableLists() throws IOException {
        URL url = getDaoDTDClass().getResource("dbmigratetable.xml");
        return url.openStream();
    }

    public LogConfigLoader logConfigLoader() {
        return new LogConfigLoader(getKBMigrateDir(), KB_MIGRATE_LOG_CONFIG, getDaoDTDClass());
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
