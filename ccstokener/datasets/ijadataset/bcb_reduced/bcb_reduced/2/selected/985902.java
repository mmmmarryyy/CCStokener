package sequime.io.read.ena;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import javax.xml.crypto.Data;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import sequime.io.read.ena.ENABrowserNodeDialog.ena_details;

/**
 * This is the model implementation of ENABrowser.
 * Retrieve nucleotide sequences from the european nucleotide archive (ENA)
 *
 * @author Nikolas Fechner
 */
public class ENABrowserNodeModel extends NodeModel {

    public static String CFG_DOMAIN = "domain";

    private SettingsModelString m_domain = new SettingsModelString(CFG_DOMAIN, "Eukaryota");

    public static String CFG_TAXA = "taxa";

    private SettingsModelStringArray m_taxa = new SettingsModelStringArray(CFG_TAXA, new String[] { "" });

    Hashtable<String, ena_details> H = new Hashtable<String, ENABrowserNodeDialog.ena_details>();

    private SettingsModel[] models = new SettingsModel[] { m_domain, m_taxa };

    /**
     * Constructor for the node model.
     */
    protected ENABrowserNodeModel() {
        super(0, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {
        BufferedDataContainer container = exec.createDataContainer(createSpec());
        String[] descs = m_taxa.getStringArrayValue();
        for (int i = 0; i < descs.length; i++) {
            try {
                String id = ENADataHolder.instance().get(descs[i]).key;
                String[] t = ENADataHolder.instance().retrieveFasta(id);
                DataRow row = new DefaultRow(id, new StringCell(id), new StringCell(descs[i]), new StringCell(ENADataHolder.instance().get(descs[i]).taxid), new StringCell(t[1]));
                container.addRowToTable(row);
            } catch (Exception e) {
                e.printStackTrace();
            }
            exec.setProgress((double) i / (double) descs.length);
        }
        container.close();
        return new BufferedDataTable[] { container.getTable() };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    private DataTableSpec createSpec() {
        return new DataTableSpec(DataTableSpec.createColumnSpecs(new String[] { "ID", "Description", "Taxon", "Sequence" }, new DataType[] { StringCell.TYPE, StringCell.TYPE, StringCell.TYPE, StringCell.TYPE }));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[] { createSpec() };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        for (SettingsModel model : models) model.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        for (SettingsModel model : models) model.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        for (SettingsModel model : models) model.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
        List<String> taxa = new Vector<String>();
        String domain = m_domain.getStringValue();
        String id = "";
        if (domain.equalsIgnoreCase("Eukaryota")) id = "eukaryota";
        try {
            URL url = new URL("http://www.ebi.ac.uk/genomes/" + id + ".details.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String link = "";
            String key = "";
            String name = "";
            int counter = 0;
            String line = "";
            while ((line = reader.readLine()) != null) {
                String[] st = line.split("\t");
                ena_details ena = new ena_details(st[0], st[1], st[2], st[3], st[4]);
                ENADataHolder.instance().put(ena.desc, ena);
                taxa.add(ena.desc);
            }
            reader.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
    }
}
