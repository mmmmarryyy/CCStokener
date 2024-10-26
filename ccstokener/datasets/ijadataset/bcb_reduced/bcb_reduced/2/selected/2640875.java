package hu.schmidtsoft.map.model.io;

import hu.schmidtsoft.map.model.MMissing;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

public class ImportList {

    public static MMissing load(URL url) throws IOException {
        MMissing ret = new MMissing();
        InputStream is = url.openStream();
        try {
            Reader r = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(r);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() > 0) {
                    ret.add(line);
                }
            }
            return ret;
        } finally {
            is.close();
        }
    }
}
