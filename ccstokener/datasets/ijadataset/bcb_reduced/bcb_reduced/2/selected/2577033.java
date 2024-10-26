package org.apache.solr.handler;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.solr.TestDistributedSearch;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.util.AbstractSolrTestCase;
import java.io.*;
import java.net.URL;

/**
 * Test for ReplicationHandler
 *
 * @version $Id: TestReplicationHandler.java 823711 2009-10-09 21:52:42Z yonik $
 * @since 1.4
 */
public class TestReplicationHandler extends AbstractSolrTestCase {

    public String getSchemaFile() {
        return null;
    }

    public String getSolrConfigFile() {
        return null;
    }

    private static final String CONF_DIR = "." + File.separator + "solr" + File.separator + "conf" + File.separator;

    private static final String SLAVE_CONFIG = CONF_DIR + "solrconfig-slave.xml";

    JettySolrRunner masterJetty, slaveJetty;

    SolrServer masterClient, slaveClient;

    SolrInstance master = null, slave = null;

    String context = "/solr";

    public void setUp() throws Exception {
        super.setUp();
        master = new SolrInstance("master", null);
        master.setUp();
        masterJetty = createJetty(master);
        masterClient = createNewSolrServer(masterJetty.getLocalPort());
        slave = new SolrInstance("slave", masterJetty.getLocalPort());
        slave.setUp();
        slaveJetty = createJetty(slave);
        slaveClient = createNewSolrServer(slaveJetty.getLocalPort());
    }

    @Override
    public void tearDown() throws Exception {
        super.preTearDown();
        masterJetty.stop();
        slaveJetty.stop();
        master.tearDown();
        slave.tearDown();
        super.tearDown();
    }

    private JettySolrRunner createJetty(SolrInstance instance) throws Exception {
        System.setProperty("solr.solr.home", instance.getHomeDir());
        System.setProperty("solr.data.dir", instance.getDataDir());
        JettySolrRunner jetty = new JettySolrRunner("/solr", 0);
        jetty.start();
        return jetty;
    }

    protected SolrServer createNewSolrServer(int port) {
        try {
            String url = "http://localhost:" + port + context;
            CommonsHttpSolrServer s = new CommonsHttpSolrServer(url);
            s.setDefaultMaxConnectionsPerHost(100);
            s.setMaxTotalConnections(100);
            return s;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    int index(SolrServer s, Object... fields) throws Exception {
        SolrInputDocument doc = new SolrInputDocument();
        for (int i = 0; i < fields.length; i += 2) {
            doc.addField((String) (fields[i]), fields[i + 1]);
        }
        return s.add(doc).getStatus();
    }

    NamedList query(String query, SolrServer s) throws SolrServerException {
        NamedList res = new SimpleOrderedMap();
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("q", query);
        QueryResponse qres = s.query(params);
        res = qres.getResponse();
        return res;
    }

    public void testIndexAndConfigReplication() throws Exception {
        for (int i = 0; i < 500; i++) index(masterClient, "id", i, "name", "name = " + i);
        masterClient.commit();
        NamedList masterQueryRsp = query("*:*", masterClient);
        SolrDocumentList masterQueryResult = (SolrDocumentList) masterQueryRsp.get("response");
        assertEquals(500, masterQueryResult.getNumFound());
        Thread.sleep(3000);
        NamedList slaveQueryRsp = query("*:*", slaveClient);
        SolrDocumentList slaveQueryResult = (SolrDocumentList) slaveQueryRsp.get("response");
        if (slaveQueryResult.getNumFound() == 0) {
            Thread.sleep(5000);
            slaveQueryRsp = query("*:*", slaveClient);
            slaveQueryResult = (SolrDocumentList) slaveQueryRsp.get("response");
        }
        assertEquals(500, slaveQueryResult.getNumFound());
        String cmp = TestDistributedSearch.compare(masterQueryResult, slaveQueryResult, 0, null);
        assertEquals(null, cmp);
        masterClient.deleteByQuery("*:*");
        masterClient.commit();
        copyFile(new File(CONF_DIR + "schema-replication2.xml"), new File(master.getConfDir(), "schema.xml"));
        masterJetty.stop();
        masterJetty = createJetty(master);
        masterClient = createNewSolrServer(masterJetty.getLocalPort());
        copyFile(new File(SLAVE_CONFIG), new File(slave.getConfDir(), "solrconfig.xml"), masterJetty.getLocalPort());
        slaveJetty.stop();
        slaveJetty = createJetty(slave);
        slaveClient = createNewSolrServer(slaveJetty.getLocalPort());
        index(masterClient, "id", "2000", "name", "name = " + 2000, "newname", "newname = " + 2000);
        masterClient.commit();
        Thread.sleep(3000);
        slaveQueryRsp = query("*:*", slaveClient);
        SolrDocument d = ((SolrDocumentList) slaveQueryRsp.get("response")).get(0);
        assertEquals("newname = 2000", (String) d.getFieldValue("newname"));
    }

    public void testIndexAndConfigAliasReplication() throws Exception {
        for (int i = 0; i < 500; i++) index(masterClient, "id", i, "name", "name = " + i);
        masterClient.commit();
        NamedList masterQueryRsp = query("*:*", masterClient);
        SolrDocumentList masterQueryResult = (SolrDocumentList) masterQueryRsp.get("response");
        assertEquals(500, masterQueryResult.getNumFound());
        Thread.sleep(3000);
        NamedList slaveQueryRsp = query("*:*", slaveClient);
        SolrDocumentList slaveQueryResult = (SolrDocumentList) slaveQueryRsp.get("response");
        if (slaveQueryResult.getNumFound() == 0) {
            Thread.sleep(5000);
            slaveQueryRsp = query("*:*", slaveClient);
            slaveQueryResult = (SolrDocumentList) slaveQueryRsp.get("response");
        }
        assertEquals(500, slaveQueryResult.getNumFound());
        String cmp = TestDistributedSearch.compare(masterQueryResult, slaveQueryResult, 0, null);
        assertEquals(null, cmp);
        masterClient.deleteByQuery("*:*");
        masterClient.commit();
        copyFile(new File(CONF_DIR + "solrconfig-master1.xml"), new File(master.getConfDir(), "solrconfig.xml"));
        copyFile(new File(CONF_DIR + "schema-replication2.xml"), new File(master.getConfDir(), "schema.xml"));
        copyFile(new File(CONF_DIR + "schema-replication2.xml"), new File(master.getConfDir(), "schema-replication2.xml"));
        masterJetty.stop();
        masterJetty = createJetty(master);
        masterClient = createNewSolrServer(masterJetty.getLocalPort());
        copyFile(new File(SLAVE_CONFIG), new File(slave.getConfDir(), "solrconfig.xml"), masterJetty.getLocalPort());
        slaveJetty.stop();
        slaveJetty = createJetty(slave);
        slaveClient = createNewSolrServer(slaveJetty.getLocalPort());
        index(masterClient, "id", "2000", "name", "name = " + 2000, "newname", "newname = " + 2000);
        masterClient.commit();
        Thread.sleep(3000);
        index(slaveClient, "id", "2000", "name", "name = " + 2001, "newname", "newname = " + 2001);
        slaveClient.commit();
        slaveQueryRsp = query("*:*", slaveClient);
        SolrDocument d = ((SolrDocumentList) slaveQueryRsp.get("response")).get(0);
        assertEquals("newname = 2001", (String) d.getFieldValue("newname"));
    }

    public void testStopPoll() throws Exception {
        for (int i = 0; i < 500; i++) index(masterClient, "id", i, "name", "name = " + i);
        masterClient.commit();
        NamedList masterQueryRsp = query("*:*", masterClient);
        SolrDocumentList masterQueryResult = (SolrDocumentList) masterQueryRsp.get("response");
        assertEquals(500, masterQueryResult.getNumFound());
        Thread.sleep(3000);
        NamedList slaveQueryRsp = query("*:*", slaveClient);
        SolrDocumentList slaveQueryResult = (SolrDocumentList) slaveQueryRsp.get("response");
        assertEquals(500, slaveQueryResult.getNumFound());
        String cmp = TestDistributedSearch.compare(masterQueryResult, slaveQueryResult, 0, null);
        assertEquals(null, cmp);
        String masterUrl = "http://localhost:" + slaveJetty.getLocalPort() + "/solr/replication?command=disablepoll";
        URL url = new URL(masterUrl);
        InputStream stream = url.openStream();
        try {
            stream.close();
        } catch (IOException e) {
        }
        index(masterClient, "id", 501, "name", "name = " + 501);
        masterClient.commit();
        Thread.sleep(3000);
        slaveQueryRsp = query("*:*", slaveClient);
        slaveQueryResult = (SolrDocumentList) slaveQueryRsp.get("response");
        assertEquals(500, slaveQueryResult.getNumFound());
        slaveQueryRsp = query("*:*", masterClient);
        slaveQueryResult = (SolrDocumentList) slaveQueryRsp.get("response");
        assertEquals(501, slaveQueryResult.getNumFound());
    }

    public void testSnapPullWithMasterUrl() throws Exception {
        copyFile(new File(CONF_DIR + "solrconfig-slave1.xml"), new File(slave.getConfDir(), "solrconfig.xml"));
        slaveJetty.stop();
        slaveJetty = createJetty(slave);
        slaveClient = createNewSolrServer(slaveJetty.getLocalPort());
        for (int i = 0; i < 500; i++) index(masterClient, "id", i, "name", "name = " + i);
        masterClient.commit();
        NamedList masterQueryRsp = query("*:*", masterClient);
        SolrDocumentList masterQueryResult = (SolrDocumentList) masterQueryRsp.get("response");
        assertEquals(500, masterQueryResult.getNumFound());
        String masterUrl = "http://localhost:" + slaveJetty.getLocalPort() + "/solr/replication?command=fetchindex&masterUrl=";
        masterUrl += "http://localhost:" + masterJetty.getLocalPort() + "/solr/replication";
        URL url = new URL(masterUrl);
        InputStream stream = url.openStream();
        try {
            stream.close();
        } catch (IOException e) {
        }
        Thread.sleep(3000);
        NamedList slaveQueryRsp = query("*:*", slaveClient);
        SolrDocumentList slaveQueryResult = (SolrDocumentList) slaveQueryRsp.get("response");
        assertEquals(500, slaveQueryResult.getNumFound());
        String cmp = TestDistributedSearch.compare(masterQueryResult, slaveQueryResult, 0, null);
        assertEquals(null, cmp);
    }

    public void testReplicateAfterStartup() throws Exception {
        slaveJetty.stop();
        for (int i = 0; i < 500; i++) index(masterClient, "id", i, "name", "name = " + i);
        masterClient.commit();
        NamedList masterQueryRsp = query("*:*", masterClient);
        SolrDocumentList masterQueryResult = (SolrDocumentList) masterQueryRsp.get("response");
        assertEquals(500, masterQueryResult.getNumFound());
        copyFile(new File(CONF_DIR + "solrconfig-master2.xml"), new File(master.getConfDir(), "solrconfig.xml"));
        masterJetty.stop();
        masterJetty = createJetty(master);
        masterClient = createNewSolrServer(masterJetty.getLocalPort());
        copyFile(new File(SLAVE_CONFIG), new File(slave.getConfDir(), "solrconfig.xml"), masterJetty.getLocalPort());
        slaveJetty = createJetty(slave);
        slaveClient = createNewSolrServer(slaveJetty.getLocalPort());
        Thread.sleep(3000);
        NamedList slaveQueryRsp = query("*:*", slaveClient);
        SolrDocumentList slaveQueryResult = (SolrDocumentList) slaveQueryRsp.get("response");
        assertEquals(500, slaveQueryResult.getNumFound());
        String cmp = TestDistributedSearch.compare(masterQueryResult, slaveQueryResult, 0, null);
        assertEquals(null, cmp);
    }

    public void testReplicateAfterWrite2Slave() throws Exception {
        int nDocs = 50;
        for (int i = 0; i < nDocs; i++) {
            index(masterClient, "id", i, "name", "name = " + i);
        }
        String masterUrl = "http://localhost:" + masterJetty.getLocalPort() + "/solr/replication?command=disableReplication";
        URL url = new URL(masterUrl);
        InputStream stream = url.openStream();
        try {
            stream.close();
        } catch (IOException e) {
        }
        masterClient.commit();
        NamedList masterQueryRsp = query("*:*", masterClient);
        SolrDocumentList masterQueryResult = (SolrDocumentList) masterQueryRsp.get("response");
        assertEquals(nDocs, masterQueryResult.getNumFound());
        Thread.sleep(100);
        index(slaveClient, "id", 551, "name", "name = " + 551);
        slaveClient.commit(true, true);
        index(slaveClient, "id", 552, "name", "name = " + 552);
        slaveClient.commit(true, true);
        index(slaveClient, "id", 553, "name", "name = " + 553);
        slaveClient.commit(true, true);
        index(slaveClient, "id", 554, "name", "name = " + 554);
        slaveClient.commit(true, true);
        index(slaveClient, "id", 555, "name", "name = " + 555);
        slaveClient.commit(true, true);
        NamedList slaveQueryRsp = query("id:555", slaveClient);
        SolrDocumentList slaveQueryResult = (SolrDocumentList) slaveQueryRsp.get("response");
        assertEquals(1, slaveQueryResult.getNumFound());
        masterUrl = "http://localhost:" + masterJetty.getLocalPort() + "/solr/replication?command=enableReplication";
        url = new URL(masterUrl);
        stream = url.openStream();
        try {
            stream.close();
        } catch (IOException e) {
        }
        Thread.sleep(3000);
        slaveQueryRsp = query("id:555", slaveClient);
        slaveQueryResult = (SolrDocumentList) slaveQueryRsp.get("response");
        assertEquals(0, slaveQueryResult.getNumFound());
    }

    public void testBackup() throws Exception {
        masterJetty.stop();
        copyFile(new File(CONF_DIR + "solrconfig-master1.xml"), new File(master.getConfDir(), "solrconfig.xml"));
        masterJetty = createJetty(master);
        masterClient = createNewSolrServer(masterJetty.getLocalPort());
        for (int i = 0; i < 500; i++) index(masterClient, "id", i, "name", "name = " + i);
        masterClient.commit();
        class BackupThread extends Thread {

            volatile String fail = null;

            public void run() {
                String masterUrl = "http://localhost:" + masterJetty.getLocalPort() + "/solr/replication?command=" + ReplicationHandler.CMD_BACKUP;
                URL url;
                InputStream stream = null;
                try {
                    url = new URL(masterUrl);
                    stream = url.openStream();
                    stream.close();
                } catch (Exception e) {
                    fail = e.getMessage();
                } finally {
                    IOUtils.closeQuietly(stream);
                }
            }

            ;
        }
        ;
        BackupThread backupThread = new BackupThread();
        backupThread.start();
        File dataDir = new File(master.getDataDir());
        class CheckStatus extends Thread {

            volatile String fail = null;

            volatile String response = null;

            volatile boolean success = false;

            public void run() {
                String masterUrl = "http://localhost:" + masterJetty.getLocalPort() + "/solr/replication?command=" + ReplicationHandler.CMD_DETAILS;
                URL url;
                InputStream stream = null;
                try {
                    url = new URL(masterUrl);
                    stream = url.openStream();
                    response = IOUtils.toString(stream);
                    if (response.contains("<str name=\"status\">success</str>")) {
                        success = true;
                    }
                    stream.close();
                } catch (Exception e) {
                    fail = e.getMessage();
                } finally {
                    IOUtils.closeQuietly(stream);
                }
            }

            ;
        }
        ;
        int waitCnt = 0;
        CheckStatus checkStatus = new CheckStatus();
        while (true) {
            checkStatus.run();
            if (checkStatus.fail != null) {
                fail(checkStatus.fail);
            }
            if (checkStatus.success) {
                break;
            }
            Thread.sleep(200);
            if (waitCnt == 10) {
                fail("Backup success not detected:" + checkStatus.response);
            }
            waitCnt++;
        }
        if (backupThread.fail != null) {
            fail(backupThread.fail);
        }
        File[] files = dataDir.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                if (name.startsWith("snapshot")) {
                    return true;
                }
                return false;
            }
        });
        assertEquals(1, files.length);
        File snapDir = files[0];
        IndexSearcher searcher = new IndexSearcher(new SimpleFSDirectory(snapDir.getAbsoluteFile(), null), true);
        TopDocs hits = searcher.search(new MatchAllDocsQuery(), 1);
        assertEquals(500, hits.totalHits);
    }

    void copyFile(File src, File dst) throws IOException {
        copyFile(src, dst, null);
    }

    /**
   * character copy of file using UTF-8. If port is non-null, will be substituted any time "TEST_PORT" is found.
   */
    private void copyFile(File src, File dst, Integer port) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(src));
        Writer out = new FileWriter(dst);
        for (String line = in.readLine(); null != line; line = in.readLine()) {
            if (null != port) line = line.replace("TEST_PORT", port.toString());
            out.write(line);
        }
        in.close();
        out.close();
    }

    private class SolrInstance extends AbstractSolrTestCase {

        String name;

        Integer masterPort;

        File homeDir;

        File confDir;

        /**
     * if masterPort is null, this instance is a master -- otherwise this instance is a slave, and assumes the master is
     * on localhost at the specified port.
     */
        public SolrInstance(String name, Integer port) {
            this.name = name;
            this.masterPort = port;
        }

        public String getHomeDir() {
            return homeDir.toString();
        }

        @Override
        public String getSchemaFile() {
            return CONF_DIR + "schema-replication1.xml";
        }

        public String getConfDir() {
            return confDir.toString();
        }

        public String getDataDir() {
            return dataDir.toString();
        }

        @Override
        public String getSolrConfigFile() {
            String fname = "";
            if (null == masterPort) fname = CONF_DIR + "solrconfig-master.xml"; else fname = SLAVE_CONFIG;
            return fname;
        }

        public void setUp() throws Exception {
            System.setProperty("solr.test.sys.prop1", "propone");
            System.setProperty("solr.test.sys.prop2", "proptwo");
            String home = System.getProperty("java.io.tmpdir") + File.separator + getClass().getName() + "-" + System.currentTimeMillis();
            if (null == masterPort) {
                homeDir = new File(home + "master");
                dataDir = new File(home + "master", "data");
                confDir = new File(home + "master", "conf");
            } else {
                homeDir = new File(home + "slave");
                dataDir = new File(home + "slave", "data");
                confDir = new File(home + "slave", "conf");
            }
            homeDir.mkdirs();
            dataDir.mkdirs();
            confDir.mkdirs();
            File f = new File(confDir, "solrconfig.xml");
            copyFile(new File(getSolrConfigFile()), f, masterPort);
            f = new File(confDir, "schema.xml");
            copyFile(new File(getSchemaFile()), f);
        }

        public void tearDown() throws Exception {
            super.tearDown();
            AbstractSolrTestCase.recurseDelete(homeDir);
        }
    }
}
