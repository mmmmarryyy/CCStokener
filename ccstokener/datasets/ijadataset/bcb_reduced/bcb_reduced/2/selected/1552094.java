package com.apelon.dts.db.admin.config;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import com.apelon.common.log4j.*;
import com.apelon.common.sql.*;
import com.apelon.common.util.db.*;

public class DBSubscriptionConfig implements DBSystemConfig {

    private static final String DTD = "dtsconfig.dtd";

    public DBSubscriptionConfig() {
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
        return com.apelon.dts.db.admin.config.DBSubscriptionConfig.class;
    }

    public InputStream getDaoConfig(String connectionType) throws IOException {
        URL url = null;
        if (connectionType.equals(SQL.ORACLE)) {
            url = DBSubscriptionConfig.class.getResource("subscription-oracle.xml");
        } else if (connectionType.equals(SQL.SQL2K)) {
            url = DBSubscriptionConfig.class.getResource("subscription-sql2k.xml");
        }
        return url.openStream();
    }

    public InputStream getTableLists() throws IOException {
        URL url = DBSubscriptionConfig.class.getResource("subscription-table.xml");
        return url.openStream();
    }

    public LogConfigLoader logConfigLoader() {
        return new LogConfigLoader("dbcreatelog.xml", DBSubscriptionConfig.class);
    }

    public void validateInput(Map map) throws Exception {
    }

    public String[] getConnectionProperties() {
        return null;
    }
}
