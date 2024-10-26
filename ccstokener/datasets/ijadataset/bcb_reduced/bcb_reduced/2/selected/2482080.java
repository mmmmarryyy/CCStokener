package cz.razor.dzemuj.datamodels;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Expects data in text file, row = one record, attributes separated by semicolon; 
 * first line contains metadata
 * other lines contain data
 * 
 * @author zdenek.kedaj@gmail.com
 * @version 20.5. 2008
 */
public class CountModel {

    private List<CountModelItem> list;

    private HashMap<String, CountModelItem> map;

    /**
	 * constructor loads data from file
	 * expect data format _R_cetnosti_spr
	 * 
	 * @param url input file
	 * @throws IOException
	 */
    public CountModel(URL url) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String line;
        list = new ArrayList<CountModelItem>();
        map = new HashMap<String, CountModelItem>();
        line = in.readLine();
        int n = 1;
        String[] rowAttrib;
        CountModelItem item;
        while ((line = in.readLine()) != null) {
            rowAttrib = line.split(";");
            item = new CountModelItem(n, Integer.valueOf(rowAttrib[1]).intValue(), Integer.valueOf(rowAttrib[2]).intValue(), Integer.valueOf(rowAttrib[3]).intValue(), rowAttrib[0]);
            list.add(item);
            map.put(item.getHash(), item);
            n++;
        }
        in.close();
    }

    public HashMap<String, CountModelItem> getMap() {
        return map;
    }

    public void setMap(HashMap<String, CountModelItem> map) {
        this.map = map;
    }
}
