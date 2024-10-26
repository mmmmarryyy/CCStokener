package edu.ucsd.ncmir.ontology.browser.bioportal;

import edu.ucsd.ncmir.spl.minixml.Document;
import edu.ucsd.ncmir.spl.minixml.Element;
import edu.ucsd.ncmir.spl.minixml.SAXBuilder;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

public final class BioportalLoader {

    static ArrayList<BioportalRow> _rows = null;

    static ArrayList<BioportalRow> getRowList() {
        if (_rows == null) try {
            _rows = new ArrayList<BioportalRow>();
            URL url = new URL(BioportalStrings.BASE);
            Document document = new SAXBuilder().build(url.openStream());
            if (document != null) parse(document);
        } catch (Exception ex) {
        }
        return _rows;
    }

    static void parse(Document document) {
        Element root = document.getRootElement();
        Element list = root.getChild("data").getChild("list");
        for (Element b : list.getChildren("ontologyBean")) try {
            String id = b.getChild("id").getValue();
            String label = b.getChild("displayLabel").getValue();
            String format = b.getChild("format").getValue();
            _rows.add(new BioportalRow(id, label, format));
        } catch (Exception ex) {
        }
    }

    /**
     * @return the rows
     */
    static BioportalRow[] getRows() {
        return getRowList().toArray(new BioportalRow[0]);
    }

    static BioportalRow[] getSelectedFormat(String type) {
        ArrayList<BioportalRow> list = new ArrayList<BioportalRow>();
        for (BioportalRow row : getRowList()) if (row.getFormat().equals(type)) list.add(row);
        BioportalRow[] rows = list.toArray(new BioportalRow[0]);
        Arrays.sort(rows);
        return rows;
    }

    static BioportalRow[] getListByFormat(String format) {
        return getSelectedFormat(format);
    }

    public static String getReferenceByName(String name) {
        String reference = null;
        for (BioportalRow row : getRowList()) if (row.toString().equalsIgnoreCase(name)) {
            reference = row.getDownloadReference();
            break;
        }
        return reference;
    }
}
