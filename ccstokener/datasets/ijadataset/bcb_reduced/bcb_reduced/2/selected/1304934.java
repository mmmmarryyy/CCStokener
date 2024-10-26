package mn.more.mock.gen;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

/**
 * @author $Author: mikeliucc $
 * @version $Revision: 119 $
 */
public abstract class GeneratorBase {

    protected int max;

    protected boolean repeating;

    protected boolean allowEmptyData;

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public boolean isRepeating() {
        return repeating;
    }

    public void setRepeating(boolean repeating) {
        this.repeating = repeating;
    }

    public boolean isAllowEmptyData() {
        return allowEmptyData;
    }

    public void setAllowEmptyData(boolean allowEmptyData) {
        this.allowEmptyData = allowEmptyData;
    }

    public abstract List<String> generate() throws IOException;

    protected void readInput(String filename, List<String> list) throws IOException {
        URL url = GeneratorBase.class.getResource(filename);
        if (url == null) {
            throw new FileNotFoundException("specified file not available - " + filename);
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                list.add(line.trim());
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }

    protected boolean isDataUsable(List<String> generated, String newData) {
        if (!allowEmptyData && (newData == null || newData.length() < 1)) {
            return false;
        }
        return repeating || !generated.contains(newData);
    }
}
