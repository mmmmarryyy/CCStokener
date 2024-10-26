package fi.arcusys.acj.util.table.ods;

import java.io.InputStream;
import java.net.URL;
import org.junit.Assert;
import org.junit.Test;
import fi.arcusys.acj.util.table.LookupTable;

/**
 * FIXME Add documentation to type ODSTableBuilderTest.
 * 
 * @version 1.0 $Rev: 1562 $
 * @author mikko
 * 
 * 
 */
public class ODSTableImporterTest {

    private void assertMessagesOds(ODSTableImporter b) {
        int tableCount = b.getTableCount();
        Assert.assertEquals("Table count", 3, tableCount);
        Assert.assertEquals("Name of table 1", "Messages1", b.getTableName(0));
        Assert.assertEquals("Name of table 2", "Messages2", b.getTableName(1));
        Assert.assertEquals("Name of table 3", "Taulukko3", b.getTableName(2));
        LookupTable table = b.getLookupTableBuilder(0).toLookupTable();
        int rowCount = table.getRowCount();
        Assert.assertEquals("Row count", 7, rowCount);
        Assert.assertEquals("Key / Lang", table.getValue(0, 0));
        Assert.assertEquals("en_US", table.getValue(0, 1));
        Assert.assertEquals("fi", table.getValue(0, 2));
        Assert.assertEquals("Save", table.getValue(1, 0));
        Assert.assertEquals("Save", table.getValue(1, 1));
        Assert.assertEquals("Tallenna", table.getValue(1, 2));
        Assert.assertEquals(null, table.getValue(5, 0));
        Assert.assertEquals("FinalRow", table.getValue(6, 0));
        Assert.assertEquals("Final row after an empty row", table.getValue(6, 1));
        Assert.assertEquals("Viimeinen rivi tyhjän rivin jälkeen", table.getValue(6, 2));
        table = b.getLookupTableBuilder(1).toLookupTable();
        rowCount = table.getRowCount();
        Assert.assertEquals("Row count", 3, rowCount);
        Assert.assertEquals("Help", table.getValue(1, 0));
        Assert.assertEquals("Help", table.getValue(1, 1));
        Assert.assertEquals("Ohje", table.getValue(1, 2));
        table = b.getLookupTableBuilder(2).toLookupTable();
        rowCount = table.getRowCount();
        Assert.assertEquals("Row count", 0, rowCount);
    }

    /**
     * Test importing of content.xml.
     * @throws Exception
     */
    @Test
    public final void testImportODScontentXml() throws Exception {
        URL url = ODSTableImporterTest.class.getResource("/Messages.ods_FILES/content.xml");
        String systemId = url.getPath();
        InputStream in = url.openStream();
        ODSTableImporter b = new ODSTableImporter();
        b.importODSContentXml(systemId, in, null);
        assertMessagesOds(b);
    }

    /**
     * Test importing of entire ODS file.
     * @throws Exception
     */
    @Test
    public final void testImportODS() throws Exception {
        URL url = ODSTableImporterTest.class.getResource("/Messages.ods");
        InputStream in = url.openStream();
        ODSTableImporter b = new ODSTableImporter();
        b.importODS(in, null);
        assertMessagesOds(b);
    }
}
