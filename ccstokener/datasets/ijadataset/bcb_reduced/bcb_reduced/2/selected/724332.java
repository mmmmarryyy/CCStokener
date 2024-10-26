package cz.razor.dzemuj.radviz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.datatable.SimpleDataTableRow;

/**
 * Class with only one method to load data
 * @author zdenek.kedaj@gmail.com
 * @version 20.5. 2008
 */
public class DataLoader {

    /**
	 * loads data from urls to table
	 * 
	 * @param urlMetadata
	 * @param urlData
	 * @return SimpleDataTable as rapidminer components expect it
	 * @throws IOException
	 */
    public static SimpleDataTable loadDataFromFile(URL urlMetadata, URL urlData) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(urlMetadata.openStream()));
        List<String> columnNamesList = new ArrayList<String>();
        String[] lineParts = null;
        String line;
        in.readLine();
        while ((line = in.readLine()) != null) {
            lineParts = line.split(",");
            columnNamesList.add(lineParts[0]);
        }
        String[] columnNamesArray = new String[columnNamesList.size()];
        int index = 0;
        for (String s : columnNamesList) {
            columnNamesArray[index] = s;
            index++;
        }
        SimpleDataTable table = new SimpleDataTable("tabulka s daty", columnNamesArray);
        in = new BufferedReader(new InputStreamReader(urlData.openStream()));
        lineParts = null;
        line = null;
        SimpleDataTableRow tableRow;
        double[] rowData;
        while ((line = in.readLine()) != null) {
            lineParts = line.split(",");
            rowData = new double[columnNamesList.size()];
            for (int i = 0; i < columnNamesArray.length; i++) {
                rowData[i] = Double.parseDouble(lineParts[i + 1]);
            }
            tableRow = new SimpleDataTableRow(rowData, lineParts[0]);
            table.add(tableRow);
        }
        return table;
    }
}
