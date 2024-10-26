package zipperSwing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.regex.Pattern;
import javax.swing.JList;

public class DownLoad {

    public void getFile(String s, String dir) {
        try {
            URI uri = new URI(s);
            URL url = uri.toURL();
            URLConnection urlcon = url.openConnection();
            InputStream fileIS = urlcon.getInputStream();
            String filenamesplit[] = s.split("/");
            File saveFile = new File(dir, filenamesplit[filenamesplit.length - 1]);
            FileOutputStream fileOS = new FileOutputStream(saveFile);
            int c;
            while ((c = fileIS.read()) != -1) {
                fileOS.write((byte) c);
            }
            System.out.println("Get " + filenamesplit[filenamesplit.length - 1]);
            fileOS.close();
            fileIS.close();
        } catch (MalformedURLException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
        } catch (URISyntaxException e) {
            System.err.println(e);
        }
    }

    public LinkedBlockingDeque<String> makeDownloadDeque(JList list, LinkedBlockingDeque<String> sum, String candidateRegex) {
        int listMax = list.getModel().getSize();
        for (int i = 0; i < listMax; i++) {
            if (Pattern.compile(candidateRegex).matcher((String) list.getModel().getElementAt(i)).matches()) sum.add((String) list.getModel().getElementAt(i));
        }
        return sum;
    }

    public void makedir(String dir) {
        File folder = new File(dir);
        folder.mkdir();
    }
}
