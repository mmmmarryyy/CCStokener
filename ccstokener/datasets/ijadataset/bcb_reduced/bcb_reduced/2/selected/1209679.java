package com.apelon.dts.db.config;

import com.apelon.dts.db.config.DTSDBConfig;
import com.apelon.common.sql.SQL;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

/**
 * DBSystemConfig for DTS Subset.
 *
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: Apelon, Inc.</p>
 * @author Apelon Inc.
 * @since 3.4
 */
public class SubsetDBConfig extends DTSDBConfig {

    /**
   * Returns DTS Subset DAO Config for a connection type.
   *
   * @param connectionType SQL.ORACLE or SQL.SQL2K
   * @return DAO Config XML for DTS Workflow
   * @throws java.io.IOException
   */
    public InputStream getDaoConfig(String connectionType) throws IOException {
        URL url = null;
        if (connectionType.equals(SQL.ORACLE)) {
            url = SubsetDBConfig.class.getResource("subset-oracle.xml");
        } else if (connectionType.equals(SQL.SQL2K)) {
            url = SubsetDBConfig.class.getResource("subset-sql2k.xml");
        } else if (connectionType.equals(SQL.CACHE)) {
            url = SubsetDBConfig.class.getResource("subset-cache.xml");
        }
        return url.openStream();
    }
}
