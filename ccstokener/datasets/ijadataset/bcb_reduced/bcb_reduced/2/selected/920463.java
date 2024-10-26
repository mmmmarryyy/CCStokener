package com.apelon.dtswf.db.assignment.config;

import com.apelon.dts.db.config.DTSDBConfig;
import com.apelon.common.sql.SQL;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

/**
 * DBSystemConfig for DTW Workflow.
 *
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: Apelon, Inc.</p>
 * @author Apelon Inc.
 * @since 3.2
 */
public class DTSWFDBConfig extends DTSDBConfig {

    /**
   * Returns DTS Workflow DAO Config for a connection type.
   *
   * @param connectionType SQL.ORACLE or SQL.SQL2K
   * @return DAO Config XML for DTS Workflow
   * @throws IOException
   */
    public InputStream getDaoConfig(String connectionType) throws IOException {
        URL url = null;
        if (connectionType.equals(SQL.ORACLE)) {
            url = DTSWFDBConfig.class.getResource("oracle.xml");
        } else if (connectionType.equals(SQL.SQL2K)) {
            url = DTSWFDBConfig.class.getResource("sql2k.xml");
        } else if (connectionType.equals(SQL.CACHE)) {
            url = DTSWFDBConfig.class.getResource("cache.xml");
        }
        return url.openStream();
    }
}
