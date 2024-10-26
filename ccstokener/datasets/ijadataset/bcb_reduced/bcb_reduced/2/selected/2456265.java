package zing.utils;

import java.io.*;
import java.net.URL;
import java.sql.DriverManager;
import java.util.Enumeration;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.QueryDataSet;
import org.dbunit.database.DatabaseSequenceFilter;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.filter.ITableFilter;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.FilteredDataSet;
import zing.utils.xmlobjects.Datasets;
import zing.utils.xmlobjects.Dataset;
import zing.utils.xmlobjects.Connection;

/**
 * Utility for exporting datasets from existing database.
 * It takes the config file from classpath if not specified.
 * It can take referential integrity into account and export tables according
 * to the table dependancy so that while updating the dataset into database,
 * referential constraint is not violated. This feature is enabled by passing
 * "-filter" as argument.
 */
public class DataExporter {

    /**
     * Denotes a database connection.
     */
    private IDatabaseConnection conn;

    private static final String DEFAULT_CONFIG_XML = "DataExportConfig.xml";

    /**
     * Main method for the class.
     * @param argv Denotes the list of arguments.
     */
    public static void main(String[] argv) {
        try {
            DataExporter dataExporter = new DataExporter();
            if (argv.length == 0) {
                dataExporter.exportDataSets(DEFAULT_CONFIG_XML);
            } else if (argv.length == 1) {
                dataExporter.exportDataSets(argv[0]);
            } else {
                System.out.println("Usage: DataExporter [XML Path]");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the dataset configuration from the XML.
     * @param strConfigXmlPath Dentoes the path of the config XML.
     * @return Array of DataSets.
     * @throws Exception In case of error.
     */
    private Datasets loadDataSets(String strConfigXmlPath) throws Exception {
        InputStreamReader isr;
        try {
            isr = new FileReader(strConfigXmlPath);
        } catch (FileNotFoundException e) {
            URL url = this.getClass().getClassLoader().getResource(strConfigXmlPath);
            if (null != url) {
                isr = new InputStreamReader(url.openStream());
            } else {
                throw new Exception("Could not load data export configuration.");
            }
        }
        Datasets datasets = Datasets.unmarshal(isr);
        isr.close();
        return datasets;
    }

    /**
     * Exports the datasets configured in XML.
     * @param strConfigXmlPath Denotes the path to export to.
     * @throws Exception In case of error.
     */
    private void exportDataSets(String strConfigXmlPath) throws Exception {
        Datasets datasets = loadDataSets(strConfigXmlPath);
        setConnection(datasets.getConnection());
        Enumeration enumDataset = datasets.enumerateDataset();
        Dataset dataset;
        while (enumDataset.hasMoreElements()) {
            dataset = (Dataset) enumDataset.nextElement();
            export(dataset);
        }
        System.out.println("Done exportDataSets");
    }

    /**
     * Creates the connection.
     * @param connexion The configured connection
     * @throws Exception In case of error.
     */
    private void setConnection(Connection connexion) throws Exception {
        Class.forName(connexion.getDriver());
        java.sql.Connection connection = DriverManager.getConnection(connexion.getUrl(), connexion.getUsername(), connexion.getPassword());
        conn = new DatabaseConnection(connection, connexion.getUsername().toUpperCase());
    }

    /**
     * Exports the specified dataset.
     * @param dataset The dataset to be exported.
     * @throws Exception In case of error.
     */
    private void export(Dataset dataset) throws Exception {
        QueryDataSet querydataset = new QueryDataSet(conn);
        String tables[] = new String[dataset.getSqlCount()];
        for (int i = 0; i < dataset.getSqlCount(); i++) {
            querydataset.addTable(dataset.getSql(i).getTable(), dataset.getSql(i).getQuery());
            tables[i] = dataset.getSql(i).getTable();
        }
        ITableFilter filter = new DatabaseSequenceFilter(conn, tables);
        IDataSet idataset = new FilteredDataSet(filter, querydataset);
        FlatXmlDataSet.write(idataset, new FileOutputStream(dataset.getPath()));
    }
}
