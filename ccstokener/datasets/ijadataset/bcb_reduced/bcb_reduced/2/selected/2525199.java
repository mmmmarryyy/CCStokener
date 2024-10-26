package com.apelon.dts.db.admin.config;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import com.apelon.common.log4j.*;
import com.apelon.common.sql.*;
import com.apelon.common.util.db.*;

public class DBAdminConfig implements DBSystemConfig {

    private static final String DTD = "dtsconfig.dtd";

    private static final String KB_ADMIN_LOG_CONFIG = "kbadminlog.xml";

    private static final String KB_ADMIN_TABLE_CONFIG = "kbadmintable.xml";

    public void init(Connection source, Connection target) {
    }

    public DBAdminConfig() {
    }

    public String getDaoDTDFileName() {
        return DTD;
    }

    public String getDaoDTDURL() {
        return "http://apelon.com/dtd/dts/" + DTD;
    }

    public Class getDaoDTDClass() {
        return com.apelon.dts.db.admin.config.DBAdminConfig.class;
    }

    public InputStream getDaoConfig(String connectionType) throws IOException {
        URL url = null;
        if (connectionType.equals(SQL.ORACLE)) {
            url = DBCreateConfig.class.getResource("oracle.xml");
        } else if (connectionType.equals(SQL.SQL2K)) {
            url = DBCreateConfig.class.getResource("sql2k.xml");
        }
        return url.openStream();
    }

    public InputStream getTableLists() throws IOException {
        FileInputStream fis = null;
        fis = new FileInputStream(new File(getKBAdminDir() + KB_ADMIN_TABLE_CONFIG));
        return fis;
    }

    public LogConfigLoader logConfigLoader() {
        return new LogConfigLoader(getKBAdminDir(), KB_ADMIN_LOG_CONFIG, DBCreateConfig.class);
    }

    public void validateInput(Map propertyMap) throws Exception {
    }

    public String[] getConnectionProperties() {
        return null;
    }

    private static final String getKBAdminDir() {
        return "kb" + File.separator + "admin" + File.separator;
    }
}
