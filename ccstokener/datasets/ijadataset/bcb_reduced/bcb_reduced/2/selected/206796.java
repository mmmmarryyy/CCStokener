package com.apelon.dts.db.config;

import java.io.*;
import java.net.URL;
import java.util.Map;
import com.apelon.common.sql.SQL;
import com.apelon.common.util.db.DBSystemConfig;

/**
 * This class will load the DAO configuration files.
 *
 * The better appoach is to update GeneralDAO to support
 * mutiple tables and mutiple schema and entities.
 **/
public class DTSClassifyConfig extends DTSDBConfig {

    private static final String COMMON = "classify-common.xml";

    public String getDaoDTDFileName() {
        return COMMON;
    }

    public String getDaoDTDURL() {
        return "http://apelon.com/dtd/dts/" + COMMON;
    }

    public Class getDaoDTDClass() {
        return com.apelon.dts.db.config.DTSClassifyConfig.class;
    }

    public InputStream getDaoConfig(String connectionType) throws IOException {
        URL url = null;
        if (connectionType.equals(SQL.ORACLE)) {
            url = DTSClassifyConfig.class.getResource("classify-oracle.xml");
        } else if (connectionType.equals(SQL.SQL2K)) {
            url = DTSClassifyConfig.class.getResource("classify-sql2k.xml");
        } else if (connectionType.equals(SQL.CACHE)) {
            url = DTSClassifyConfig.class.getResource("classify-cache.xml");
        }
        return url.openStream();
    }
}
