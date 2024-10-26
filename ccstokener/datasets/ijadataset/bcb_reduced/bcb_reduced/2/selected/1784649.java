package misc;

import java.net.*;
import java.io.*;
import csimage.util.EasyInput;
import csimage.util.dbg;

/**
 * @author I-Ling
 */
public class SimpleWebPageReader {

    public static void main(String[] args) {
        SimpleWebPageReader sb = new SimpleWebPageReader();
        String url = EasyInput.input("Please enter Web page address you want retrieve (enter to quit): ");
        while (url.length() > 0) {
            dbg.sayln("you type " + url);
            String pageContent = sb.getWebPage(url);
            dbg.sayln(pageContent);
            url = EasyInput.input("Please enter Web page address you want retrieve (enter to quit): ");
        }
        System.out.println("Bye!");
    }

    public String getWebPage(String url) {
        String content = "";
        URL urlObj = null;
        try {
            urlObj = new URL(url);
        } catch (MalformedURLException urlEx) {
            urlEx.printStackTrace();
            throw new Error("URL creation failed.");
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlObj.openStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                content += line;
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new Error("Page retrieval failed.");
        }
        return content;
    }
}
