package jinvest;

import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JApplet;

public class NumberOfLines extends JApplet {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    TextArea ta;

    public void init() {
        ta = new TextArea(40, 40);
        ta.setEditable(false);
        setLayout(new BorderLayout());
        add(ta, "Center");
        int n = numberofLines(this, "MSCI_NorthAmerica");
        ta.append(" " + n);
    }

    public static int numberofLines(JApplet ja, String filename) {
        int count = 0;
        URL url = null;
        String FileToRead;
        FileToRead = "data/" + filename + ".csv";
        try {
            url = new URL(ja.getCodeBase(), FileToRead);
        } catch (MalformedURLException e) {
            System.out.println("Malformed URL ");
            ja.stop();
        }
        System.out.println(url.toString());
        try {
            InputStream in = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            while ((reader.readLine()) != null) {
                count++;
            }
            in.close();
        } catch (IOException e) {
        }
        return count;
    }
}
