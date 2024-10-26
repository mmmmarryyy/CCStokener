package uk.ac.lkl.common.util.datafile;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

/**
 * A file at a URL inside the own codebase.
 * 
 * @author sergut
 */
public class BuiltInDataFile extends DataFile {

    private URL url;

    public BuiltInDataFile(String name, URL url) {
        super(name);
        this.url = url;
    }

    @Override
    public String readContents() {
        try {
            String contents = "";
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                contents += line + "\n";
            }
            reader.close();
            return contents;
        } catch (FileNotFoundException e) {
            return "";
        } catch (IOException e) {
            return "";
        }
    }

    @Override
    public boolean writeContents(String contents) {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    public URL getURL() {
        return url;
    }
}
