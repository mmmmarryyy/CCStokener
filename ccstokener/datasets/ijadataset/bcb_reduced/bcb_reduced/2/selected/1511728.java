package com.turnguard.labs.hbase.util.tablecreator.rdf;

import com.turnguard.labs.hbase.util.tablecreator.rdf.vocab.HColumnFamily;
import com.turnguard.labs.hbase.util.tablecreator.rdf.vocab.HTable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.io.hfile.Compression.Algorithm;
import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;

/**
 *
 * @author turnguard
 */
public class HTableManagerSesame {

    private final ValueFactory valueFactory = ValueFactoryImpl.getInstance();

    protected Graph tableGraph;

    private HBaseAdmin hBaseAdmin;

    private Map<String, HTableDescriptor> hTables = new HashMap<String, HTableDescriptor>();

    /**
     * 
     * @throws MasterNotRunningException
     */
    public HTableManagerSesame() throws MasterNotRunningException {
        this(new HBaseConfiguration());
    }

    /**
     * 
     * @param hBaseConfig
     * @throws MasterNotRunningException
     */
    public HTableManagerSesame(HBaseConfiguration hBaseConfig) throws MasterNotRunningException {
        hBaseAdmin = new HBaseAdmin(hBaseConfig);
    }

    /**
     *
     * @param file
     * @throws Exception
     */
    public void readHTableRDF(File file) throws Exception {
        this.readTableRDF(new FileInputStream(file), Rio.getParserFormatForFileName(file.getName()));
    }

    /**
     *
     * @param url
     * @throws Exception
     */
    public void readHTableRDF(URL url) throws Exception {
        this.readTableRDF(url.openStream(), Rio.getParserFormatForFileName(url.getPath()));
    }

    /**
     * 
     * @param inStream
     * @param rdfFormat
     * @throws Exception
     */
    private void readTableRDF(InputStream inStream, RDFFormat rdfFormat) throws Exception {
        tableGraph = new GraphImpl(this.valueFactory);
        RDFParser rdfParser = Rio.createParser(rdfFormat, this.valueFactory);
        rdfParser.setRDFHandler(new StatementCollector(tableGraph));
        rdfParser.parse(inStream, "");
        Set<Resource> hTablesFromRDF = GraphUtil.getSubjects(tableGraph, RDF.TYPE, HTable.HTABLE);
        for (Resource res : hTablesFromRDF) {
            this.loadTable(res, GraphUtil.getUniqueObjectLiteral(tableGraph, res, HTable.NAME));
        }
    }

    /**
     *
     * @param hTableURI
     * @param hTableName
     * @throws GraphUtilException
     */
    private void loadTable(Resource hTableURI, Literal hTableName) throws GraphUtilException {
        HTableDescriptor hTable = new HTableDescriptor(hTableName.stringValue());
        Iterator<Statement> hTableStatements = tableGraph.match(hTableURI, null, null);
        while (hTableStatements.hasNext()) {
            Statement hTableStatement = hTableStatements.next();
            if (hTableStatement.getPredicate().equals(HTable.READONLY)) {
                hTable.setReadOnly(Boolean.parseBoolean(hTableStatement.getObject().stringValue()));
            }
            if (hTableStatement.getPredicate().equals(HTable.COLUMNFAMILY)) {
                this.loadColumnFamily(hTable, (Resource) hTableStatement.getObject(), GraphUtil.getUniqueObjectLiteral(tableGraph, (Resource) hTableStatement.getObject(), HColumnFamily.NAME));
            }
        }
        this.hTables.put(hTableName.stringValue(), hTable);
    }

    /**
     *
     * @param hTable
     * @param hColumnFamilyURI
     * @param hColumnFamilyName
     */
    private void loadColumnFamily(HTableDescriptor hTable, Resource hColumnFamilyURI, Literal hColumnFamilyName) {
        HColumnDescriptor hColumnFamily = new HColumnDescriptor(hColumnFamilyName.stringValue());
        Iterator<Statement> hColumnFamilyStatements = tableGraph.match(hColumnFamilyURI, null, null);
        while (hColumnFamilyStatements.hasNext()) {
            Statement hColumnFamilyStatement = hColumnFamilyStatements.next();
            if (hColumnFamilyStatement.getPredicate().equals(HColumnFamily.INMEMORY)) {
                hColumnFamily.setInMemory(Boolean.parseBoolean(hColumnFamilyStatement.getObject().stringValue()));
            }
            if (hColumnFamilyStatement.getPredicate().equals(HColumnFamily.COMPRESSION)) {
                hColumnFamily.setCompressionType(Algorithm.valueOf(hColumnFamilyStatement.getObject().stringValue()));
            }
            if (hColumnFamilyStatement.getPredicate().equals(HColumnFamily.VERSIONS)) {
                hColumnFamily.setMaxVersions(Integer.parseInt(hColumnFamilyStatement.getObject().stringValue()));
            }
            if (hColumnFamilyStatement.getPredicate().equals(HColumnFamily.TTL)) {
                hColumnFamily.setTimeToLive(Integer.parseInt(hColumnFamilyStatement.getObject().stringValue()));
            }
            if (hColumnFamilyStatement.getPredicate().equals(HColumnFamily.BLOCKSIZE)) {
                hColumnFamily.setBlocksize(Integer.parseInt(hColumnFamilyStatement.getObject().stringValue()));
            }
            if (hColumnFamilyStatement.getPredicate().equals(HColumnFamily.BLOCKCACHE)) {
                hColumnFamily.setBlockCacheEnabled(Boolean.parseBoolean(hColumnFamilyStatement.getObject().stringValue()));
            }
        }
        hTable.addFamily(hColumnFamily);
    }

    /**
     *
     * @return
     */
    public Collection<String> getHTableNames() {
        return this.hTables.keySet();
    }

    /**
     *
     * @param tableName
     * @return
     */
    public HTableDescriptor getHTable(String tableName) {
        return this.hTables.get(tableName);
    }

    /**
     *
     * @return
     */
    public Collection<HTableDescriptor> getHTables() {
        return this.hTables.values();
    }

    /**
     *
     * @param tableName
     * @throws IOException
     */
    public void createHTable(String tableName) throws IOException {
        this.hBaseAdmin.createTable(this.getHTable(tableName));
    }

    /**
     * 
     * @throws IOException
     */
    public void createHTables() throws IOException {
        for (Iterator<String> it = this.hTables.keySet().iterator(); it.hasNext(); ) {
            this.createHTable(it.next());
        }
    }

    /**
     * 
     * @param tableName
     */
    public void updateHTable(String tableName) {
    }

    /**
     * 
     */
    public void updateHTables() {
    }

    /**
     * 
     * @param tableName
     * @throws IOException
     */
    public void deleteHTable(String tableName) throws IOException {
        this.hBaseAdmin.disableTable(tableName);
        this.hBaseAdmin.deleteTable(tableName);
    }

    /**
     * 
     * @throws IOException
     */
    public void deleteHTables() throws IOException {
        for (Iterator<String> it = this.hTables.keySet().iterator(); it.hasNext(); ) {
            this.deleteHTable(it.next());
        }
    }
}
