package com.apelon.dts.db.admin.config;

import java.io.*;
import java.net.URL;
import java.util.Map;
import java.sql.Connection;
import com.apelon.common.sql.SQL;
import com.apelon.common.util.db.DBSystemConfig;
import com.apelon.common.util.db.DbCreate;
import com.apelon.common.log4j.LogConfigLoader;
import com.apelon.dts.db.admin.DTSDbCreateMgr;

public class DBCreateConfig implements DBSystemConfig {

    private static final String DTD = "dtsconfig.dtd";

    private static final String KB_CREATE_LOG_CONFIG = "kbcreatelog.xml";

    public DBCreateConfig() {
    }

    public void init(Connection source, Connection target) {
    }

    public String getDaoDTDFileName() {
        return DTD;
    }

    public String getDaoDTDURL() {
        return "http://apelon.com/dtd/dts/" + DTD;
    }

    public Class getDaoDTDClass() {
        return com.apelon.dts.db.admin.config.DBCreateConfig.class;
    }

    public InputStream getDaoConfig(String connectionType) throws IOException {
        URL url = null;
        if (connectionType.equals(SQL.ORACLE)) {
            url = DBCreateConfig.class.getResource("oracle.xml");
        } else if (connectionType.equals(SQL.SQL2K)) {
            url = DBCreateConfig.class.getResource("sql2k.xml");
        } else if (connectionType.equals(SQL.CACHE)) {
            url = DBCreateConfig.class.getResource("cache.xml");
        }
        return url.openStream();
    }

    public InputStream getTableLists() throws IOException {
        URL url = DBCreateConfig.class.getResource("dbcreatetable.xml");
        return url.openStream();
    }

    public LogConfigLoader logConfigLoader() {
        return new LogConfigLoader(getKBCreateDir(), KB_CREATE_LOG_CONFIG, DBCreateConfig.class);
    }

    public void validateInput(Map map) throws Exception {
        map.put(DbCreate.DB_CREATE_MGR_PROP, new DTSDbCreateMgr());
    }

    public String[] getConnectionProperties() {
        return null;
    }

    private static final String getKBCreateDir() {
        return "kb" + File.separator + "create" + File.separator;
    }
}
