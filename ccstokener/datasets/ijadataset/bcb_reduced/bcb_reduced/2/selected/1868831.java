package com.apelon.dts.db.config;

import java.io.*;
import java.net.URL;
import java.util.Map;
import java.sql.Connection;
import com.apelon.common.sql.SQL;
import com.apelon.common.util.db.DBSystemConfig;
import com.apelon.common.log4j.LogConfigLoader;

/**
 * DTSDBConfig for ClassifyDetails. Used by DAO framework.
 *
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: Apelon, Inc.</p>
 * @author Apelon Inc.
 * @version DTS 3.3.0
 * @since 3.3.0
 */
public class DTSClassifyDetailsConfig extends DTSDBConfig {

    /**
   * Overrides this method to return classify details config
   * @param connectionType ORACLE or SQL2k
   * @return
   * @throws IOException
   */
    public InputStream getDaoConfig(String connectionType) throws IOException {
        URL url = null;
        if (connectionType.equals(SQL.ORACLE)) {
            url = DTSClassifyDetailsConfig.class.getResource("classifydetails-oracle.xml");
        } else if (connectionType.equals(SQL.SQL2K)) {
            url = DTSClassifyDetailsConfig.class.getResource("classifydetails-sql2k.xml");
        } else if (connectionType.equals(SQL.CACHE)) {
            url = DTSClassifyDetailsConfig.class.getResource("classifydetails-cache.xml");
        }
        return url.openStream();
    }
}
