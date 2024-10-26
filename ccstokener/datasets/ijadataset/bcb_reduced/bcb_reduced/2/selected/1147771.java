package sequime.io.read.ena;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Hashtable;
import sequime.io.read.ena.ENABrowserNodeDialog.ena_details;

public class ENADataHolder extends Hashtable<String, ena_details> {

    private static ENADataHolder H;

    private ENADataHolder() {
        super();
    }

    public static ENADataHolder instance() {
        if (H == null) H = new ENADataHolder();
        return H;
    }

    @Override
    public synchronized ena_details put(String key, ena_details value) {
        if (H.containsKey(key)) return super.get(key);
        return super.put(key, value);
    }

    public String[] retrieveFasta(String id) throws Exception {
        URL url = new URL("http://www.ebi.ac.uk/ena/data/view/" + id + "&display=fasta");
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        String header = reader.readLine();
        StringBuffer seq = new StringBuffer();
        String line = "";
        while ((line = reader.readLine()) != null) {
            seq.append(line);
        }
        reader.close();
        return new String[] { header, seq.toString() };
    }
}
