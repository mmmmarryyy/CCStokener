package com.apelon.dtswf.db.admin;

import com.apelon.dts.db.admin.config.DBCreateConfig;
import com.apelon.common.sql.SQL;
import com.apelon.common.log4j.LogConfigLoader;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

/**
 * DBCreateConfig for DTSWF
 */
public class DTSWFDBCreateConfig extends DBCreateConfig {

    public InputStream getDaoConfig(String connectionType) throws IOException {
        URL url = null;
        if (connectionType.equals(SQL.ORACLE)) {
            url = DTSWFDBCreateConfig.class.getResource("oracle.xml");
        } else if (connectionType.equals(SQL.SQL2K)) {
            url = DTSWFDBCreateConfig.class.getResource("sql2k.xml");
        } else if (connectionType.equals(SQL.CACHE)) {
            url = DTSWFDBCreateConfig.class.getResource("cache.xml");
        }
        return url.openStream();
    }

    public InputStream getTableLists() throws IOException {
        URL url = DTSWFDBCreateConfig.class.getResource("dtswfdbcreatetable.xml");
        return url.openStream();
    }

    public LogConfigLoader logConfigLoader() {
        return new LogConfigLoader("dtswf/dtswfkbcreatelog.xml", DTSWFDBCreateConfig.class);
    }
}
