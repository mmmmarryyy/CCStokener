package pl.umk.webclient.gridbeans;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * @author Leszek Wanski (leswan@mat.umk.pl)
 */
public class OutputJspParser {

    private InputStream stream = null;

    private String[] tags = { "gb:output_file", "%" };

    private String[] properties = { "name", "display" };

    private ArrayList<String> gb_output_file_list = new ArrayList<String>();

    private URL url = null;

    public OutputJspParser(String paramStr) throws MalformedURLException {
        url = new URL(paramStr);
    }

    private void startTag() throws IOException {
        String tmpStr = "";
        while (stream.available() != 0) {
            int ch = stream.read();
            if (ch == '>') break;
            tmpStr += (char) ch;
        }
        String[] strArr = tmpStr.split("\\s+");
        int counter = 0;
        boolean gb_output_file = false;
        for (String s : strArr) {
            if (counter == 0) {
                if (s.equalsIgnoreCase(tags[0])) gb_output_file = true;
            } else {
                String[] subArr = s.split("=");
                if (gb_output_file && properties[0].equalsIgnoreCase(subArr[0])) gb_output_file_list.add(subArr[1].replace("\"", ""));
            }
            counter++;
        }
    }

    public void openStream() throws IOException {
        stream = url.openStream();
    }

    public void parse() throws IOException {
        while (stream.available() != 0) {
            int ch = stream.read();
            if (ch == '<') startTag();
        }
        stream.close();
    }

    public ArrayList<String> getNames() {
        return gb_output_file_list;
    }
}
