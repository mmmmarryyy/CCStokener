package cz.razor.dzemuj.datamodels;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Expects data in text file, row = one record, attributes separated by semicolon; 
 * first line contains metadata
 * other lines contain data
 * 
 * @author zdenek.kedaj@gmail.com
 * @version 20.5. 2008
 */
public class ScoreModel {

    private ArrayList<ScoreModelItem> list;

    private HashMap<String, ScoreModelItem> map;

    /**
	 * constructor loads data from file
	 * 
	 * @param filename
	 * @throws IOException
	 */
    public ScoreModel(URL url) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String line;
        list = new ArrayList<ScoreModelItem>();
        map = new HashMap<String, ScoreModelItem>();
        line = in.readLine();
        int n = 1;
        String[] rowAttrib;
        ScoreModelItem item;
        while ((line = in.readLine()) != null) {
            rowAttrib = line.split(";");
            item = new ScoreModelItem(n, Double.valueOf(rowAttrib[3]), Double.valueOf(rowAttrib[4]), Double.valueOf(rowAttrib[2]), Double.valueOf(rowAttrib[5]), rowAttrib[1]);
            list.add(item);
            map.put(item.getHash(), item);
            n++;
        }
        in.close();
    }

    public HashMap<String, ScoreModelItem> getMap() {
        return map;
    }

    public void setMap(HashMap<String, ScoreModelItem> map) {
        this.map = map;
    }
}
