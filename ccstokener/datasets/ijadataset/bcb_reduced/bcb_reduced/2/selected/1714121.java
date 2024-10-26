package com.apelon.dts.db.admin.config;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import com.apelon.common.log4j.*;
import com.apelon.common.sql.*;
import com.apelon.common.util.*;
import com.apelon.common.util.db.*;

public class MigrateConfig implements DBSystemConfig {

    private static final String DTD = "dtsconfig.dtd";

    private String ASSOCIATION_FROM_ARCHIVE_TABLE = "ASSOCIATIONS_FROM_ARCHIVE";

    private String CLASS_PREFIX = "com.apelon.dts.db.admin.table.Table";

    public MigrateConfig() {
    }

    public String getDaoDTDFileName() {
        return DTD;
    }

    public String getDaoDTDURL() {
        return "http://apelon.com/dtd/dts/" + DTD;
    }

    public Class getDaoDTDClass() {
        return com.apelon.dts.db.admin.config.MigrateConfig.class;
    }

    public InputStream getDaoConfig(String connectionType) throws IOException {
        URL url = null;
        if (connectionType.equals(SQL.ORACLE)) {
            url = com.apelon.dts.db.admin.config.MigrateConfig.class.getResource("oracle.xml");
        } else if (connectionType.equals(SQL.SQL2K)) {
            url = com.apelon.dts.db.admin.config.MigrateConfig.class.getResource("sql2k.xml");
        }
        return url.openStream();
    }

    public void init(Connection sourceConn, Connection targetConn) {
        this.sourceConn = sourceConn;
        this.targetConn = targetConn;
    }

    /**
   * This method returns an inputstream of table lists to be loaded
   * into the target schema.
   *
   * @return inputstream
   * @throws IOException
   */
    public InputStream getTableLists() throws IOException {
        String sb = new String("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<!DOCTYPE DTSDbConfig SYSTEM \"http://apelon.com/dtd/dts/dtsconfig.dtd\">" + "<DTSDbConfig>" + "<property name=\"prefix\" value=\"com.apelon.dts.db.admin.table.Table\" />" + "<table name=\"" + this.ASSOCIATION_FROM_ARCHIVE_TABLE + "\" />" + "</DTSDbConfig>");
        Categories.dataDb().debug("Table List generated for the Migration Utility : \n" + sb);
        byte[] bytes = sb.getBytes("UTF-8");
        InputStream input = new ByteArrayInputStream(bytes);
        return input;
    }

    public LogConfigLoader logConfigLoader() {
        return new LogConfigLoader("dbloadlog.xml", MigrateConfig.class);
    }

    public void validateInput(Map propertyMap) throws Exception {
        String namespaceType = (String) propertyMap.get("namespaceType");
        if (namespaceType != null) {
            if (namespaceType.equals("local")) {
                GidGenerator.localNamespaceFlag = true;
            } else if (namespaceType.equals("nonlocal")) {
                GidGenerator.localNamespaceFlag = false;
            } else throw new IllegalArgumentException("Incorrect value [" + namespaceType + "] provided for 'namespaceType'. Expecting 'local' or 'nonlocal'");
        }
    }

    public String[] getConnectionProperties() {
        return null;
    }

    private Connection sourceConn = null;

    private Connection targetConn = null;
}
