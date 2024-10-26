import java.io.*;
import java.net.*;
import java.util.*;

/**
@author Thomas Cherry jceaser@mac.com
*/
public class Test {

    public Test() {
        try {
            URL listSite = new URL("http://www.spamcop.net/w3m?action=inprogress&type=www");
            System.out.println(getFile(listSite));
        } catch (MalformedURLException mue) {
            System.err.println("bad url: " + mue.toString());
        }
    }

    public static void main(String args[]) {
        Test test = new Test();
    }

    public String getFile(URL url) {
        int letter;
        String data = "";
        try {
            StringBuffer dataBuffer = new StringBuffer();
            InputStream in = url.openStream();
            while ((letter = in.read()) != -1) {
                dataBuffer.append((char) letter);
                System.out.print((char) letter);
            }
            data = dataBuffer.toString();
        } catch (java.io.IOException ioe) {
            System.err.println(ioe.toString());
        }
        return data;
    }
}
