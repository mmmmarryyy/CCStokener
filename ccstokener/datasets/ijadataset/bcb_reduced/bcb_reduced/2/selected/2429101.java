package org.lindenb.knime.plugins.ncbi.esearch;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.lindenb.bio.ncbi.QueryKeyHandler;
import org.lindenb.knime.plugins.ncbi.NCBINodeModel;
import org.lindenb.me.Me;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ESearchNodeModel extends NCBINodeModel {

    private static final Logger LOGGER = Logger.getLogger(ESearchNodeModel.class.getName());

    private static String[] DATABASES_ = null;

    static final String KEY_DATABASE = "ncbi.database";

    static final String KEY_TERM = "ncbi.term";

    static final String KEY_START = "ncbi.start";

    static final String KEY_LIMIT = "ncbi.limit";

    /** the column in the input containing the key */
    private final SettingsModelColumnName m_term = new SettingsModelColumnName(KEY_TERM, "");

    /** database */
    private final SettingsModelString m_database = new SettingsModelString(KEY_DATABASE, "pubmed");

    /** start index */
    private final SettingsModelInteger m_start = new SettingsModelInteger(KEY_START, 0);

    /** limit index */
    private final SettingsModelInteger m_limit = new SettingsModelInteger(KEY_LIMIT, 50);

    public ESearchNodeModel() {
        super(1, 1);
        addSettings(m_term);
        addSettings(m_database);
        addSettings(m_start);
        addSettings(m_limit);
        LOGGER.setLevel(Level.ALL);
    }

    private static class EInfo extends DefaultHandler {

        private StringBuilder b = new StringBuilder();

        ArrayList<String> db = new ArrayList<String>();

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
            b.setLength(0);
        }

        @Override
        public void endElement(String uri, String localName, String name) throws SAXException {
            if (name.equals("DbName")) db.add(b.toString());
            b.setLength(0);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            b.append(ch, start, length);
        }
    }

    public static synchronized String[] getDatabases() {
        if (DATABASES_ != null) return DATABASES_;
        try {
            SAXParserFactory f = SAXParserFactory.newInstance();
            f.setNamespaceAware(false);
            f.setValidating(false);
            EInfo h = new EInfo();
            f.newSAXParser().parse("http://www.ncbi.nlm.nih.gov/entrez/eutils/einfo.fcgi", h);
            DATABASES_ = h.db.toArray(new String[h.db.size()]);
        } catch (Exception e) {
            e.printStackTrace();
            DATABASES_ = new String[] { "pubmed", "nucleotide", "protein" };
        }
        return DATABASES_;
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {
        if (this.m_term.getColumnName() == null) {
            setWarningMessage("Columns for \"term\" was not selected.");
        }
        if (inData == null || inData.length != 1 || inData[0] == null) {
            throw new IllegalArgumentException("No input data available.");
        }
        final BufferedDataTable inputTable = inData[0];
        if (inputTable.getRowCount() < 1) {
            setWarningMessage("Empty input table found");
        }
        final int columnIndex = inputTable.getDataTableSpec().findColumnIndex(this.m_term.getColumnName());
        if (columnIndex == -1) throw new IllegalArgumentException("No column data available column selected was :" + m_term.getColumnName());
        LOGGER.info("ColumnIndex=" + columnIndex);
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        SAXParser parser = factory.newSAXParser();
        StringCell dbAsCell = new StringCell(m_database.getStringValue());
        int rowCountInput = inputTable.getRowCount();
        int rowCount = -1;
        int rowIndex = 0;
        DataTableSpec dataTableSpec = createTableSpec();
        BufferedDataContainer container = exec.createDataContainer(dataTableSpec);
        for (DataRow row : inputTable) {
            ++rowCount;
            DataCell keyCell = row.getCell(columnIndex);
            if (keyCell.isMissing()) {
                LOGGER.info("empty cell");
                continue;
            }
            if (!(keyCell instanceof org.knime.core.data.def.StringCell)) {
                LOGGER.info("not a String cell");
                continue;
            }
            String term = StringCell.class.cast(keyCell).getStringValue();
            if (term == null || term.length() == 0) continue;
            StringCell termAsCell = new StringCell(term);
            QueryKeyHandler.FetchSet handler = new QueryKeyHandler.FetchSet();
            URL url = new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=" + URLEncoder.encode(m_database.getStringValue(), "UTF-8") + "&term=" + URLEncoder.encode(term, "UTF-8") + "&retstart=" + m_start.getIntValue() + "&retmax=" + m_limit.getIntValue() + "&retmode=xml" + "&email=" + URLEncoder.encode(Me.MAIL, "UTF-8") + "&tool=knime");
            LOGGER.info("Calling " + url);
            InputStream in = url.openStream();
            parser.parse(in, handler);
            in.close();
            for (Integer id : handler.getPMID()) {
                LOGGER.info("found id=" + id);
                RowKey rowKey = RowKey.createRowKey(++rowIndex);
                DataCell cells[] = new DataCell[] { termAsCell, dbAsCell, new IntCell(id) };
                container.addRowToTable(new DefaultRow(rowKey, cells));
            }
            exec.checkCanceled();
            exec.setProgress(rowCount / (double) rowCountInput, "Adding row " + rowIndex);
        }
        container.close();
        return new BufferedDataTable[] { container.getTable() };
    }

    @Override
    protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[] { createTableSpec() };
    }

    private DataTableSpec createTableSpec() {
        return new DataTableSpec(new DataColumnSpec[] { new DataColumnSpecCreator("query", org.knime.core.data.def.StringCell.TYPE).createSpec(), new DataColumnSpecCreator("database", org.knime.core.data.def.StringCell.TYPE).createSpec(), new DataColumnSpecCreator("id", org.knime.core.data.def.IntCell.TYPE).createSpec() });
    }
}
