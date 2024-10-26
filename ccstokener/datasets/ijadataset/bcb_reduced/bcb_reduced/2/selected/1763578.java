package guidatatag;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

public class FetchTextFromWebPage {

    Reader r;

    String path;

    File HTMLFile;

    HtmlTextParser htmltextparser;

    public FetchTextFromWebPage(String path) {
        HTMLEditorKit.Parser parser;
        HTMLFile = new File(path);
        byte[] html = new byte[new Long(HTMLFile.length()).intValue()];
        try {
            FileInputStream fis = new FileInputStream(HTMLFile);
            try {
                fis.read(html);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ByteArrayInputStream ba_read = new ByteArrayInputStream(html);
            r = new InputStreamReader(ba_read);
        } catch (FileNotFoundException e) {
            URL url = null;
            try {
                url = new URL(path);
                URLConnection connection = null;
                try {
                    connection = url.openConnection();
                    connection.setDoInput(true);
                    InputStream inStream = null;
                    try {
                        inStream = connection.getInputStream();
                        BufferedReader input = new BufferedReader(new InputStreamReader(inStream));
                        StringBuffer sbr = new StringBuffer();
                        String line = null;
                        try {
                            while ((line = input.readLine()) != null) {
                                System.out.println(line);
                                sbr.append(line + "\n");
                            }
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        File temp = new File("//home//wiki14//temp.txt");
                        if (!temp.exists()) {
                            if (!temp.createNewFile()) System.out.println("File caanot be created");
                        } else {
                            temp.delete();
                            if (!temp.createNewFile()) System.out.println("File caanot be created");
                        }
                        java.io.FileWriter fw = new java.io.FileWriter(temp);
                        fw.write(sbr.toString());
                        fw.close();
                        byte[] newbyte = new byte[new Long(sbr.length()).intValue()];
                        ByteArrayInputStream ba_read = new ByteArrayInputStream(newbyte);
                        r = new InputStreamReader(ba_read);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
            }
        }
        parser = new ParserDelegator();
        htmltextparser = new HtmlTextParser();
        htmltextparser.initData();
        try {
            parser.parse(r, htmltextparser, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            r.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getData() {
        return htmltextparser.getData();
    }

    public static void main(String[] args) {
        FetchTextFromWebPage fetch = new FetchTextFromWebPage("//home//wiki14//temp.txt");
        try {
            File temp = new File("//home//wiki14//webtext.txt");
            if (!temp.exists()) {
                if (!temp.createNewFile()) System.out.println("File caanot be created");
            } else {
                temp.delete();
                if (!temp.createNewFile()) System.out.println("File caanot be created");
            }
            java.io.FileWriter fw = new java.io.FileWriter(temp);
            fw.write(fetch.getData());
            fw.close();
        } catch (Exception e) {
        }
    }
}
