package htmlpage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlPage implements HtmlPageI, Serializable {

    private static final long serialVersionUID = 42L;

    private ArrayList<String> txt;

    private ArrayList<String> links;

    private ArrayList<String> keyWords;

    private String title;

    private String coding;

    private String pagePath;

    private boolean isHtml;

    HtmlPage() {
        txt = new ArrayList<String>();
        links = new ArrayList<String>();
        keyWords = new ArrayList<String>();
        title = "";
        coding = "";
        pagePath = "";
        isHtml = false;
    }

    public static void main(String[] args) {
        String pagePath = "";
        HtmlPage hp = new HtmlPage();
        hp.SetPagePath(pagePath);
        hp.ReadText();
        hp.ReadLinks();
        hp.ReadKeyWords();
        hp.ReadTitle();
        hp.ReadCoding();
        hp.IsHtml();
        hp.Print(hp.links);
        System.out.println("ExpPageTest#: Ok.");
    }

    @Override
    public void ReadText() {
        try {
            URL url = new URL(pagePath);
            InputStreamReader isr = new InputStreamReader(url.openConnection().getInputStream());
            BufferedReader br = new BufferedReader(isr);
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                txt.add(line);
            }
            br.close();
            return;
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    @Override
    public void ReadLinks() {
        Pattern p = Pattern.compile("href\\s*=\\s*\"([^\"]*)\"");
        for (Iterator<String> iter = txt.iterator(); iter.hasNext(); ) {
            Matcher m = p.matcher(iter.next().toLowerCase());
            while (m.find()) {
                links.add(m.group(1));
            }
        }
    }

    @Override
    public void ReadKeyWords() {
        String notKeyWords = "\\/|\\*|\\[|\\]|\\!|\\{|\\}|\\<|\\>|\\#|\\&|\\-|\\_|" + "\\(|\\)|\"|\\'|\\.|\\,|\\;|\\:|\\=|\\+|\\|";
        for (Iterator<String> iter = txt.iterator(); iter.hasNext(); ) {
            String inputLine = iter.next();
            inputLine = inputLine.replaceAll("(<.*?>)", "");
            inputLine = inputLine.replaceAll(notKeyWords, " ");
            String[] arstr = Pattern.compile("(\\s+)|(\\d+)").split(inputLine);
            for (int i = 0; i < arstr.length; i++) {
                if (!arstr[i].isEmpty()) {
                    keyWords.add(arstr[i]);
                }
            }
        }
    }

    @Override
    public void ReadTitle() {
        Pattern p = Pattern.compile("<\\s*title\\s*>([^\"]*)<\\s*/\\s*title\\s*>");
        for (Iterator<String> iter = txt.iterator(); iter.hasNext(); ) {
            Matcher m = p.matcher(iter.next().toString().toLowerCase());
            if (m.find()) {
                title = m.group(1).toString();
            }
        }
    }

    @Override
    public void ReadCoding() {
        Pattern p = Pattern.compile("<\\s*meta\\s*([^>]*)charset\\s*=\\s*([^>]*)\"[^>]*\\s*>");
        for (Iterator<String> iter = txt.iterator(); iter.hasNext(); ) {
            Matcher m = p.matcher(iter.next().toLowerCase());
            if (m.find()) {
                coding = m.group(2).toString();
            }
        }
    }

    @Override
    public boolean IsHtml() {
        Pattern p = Pattern.compile("<\\s*html.*");
        for (Iterator<String> iter = txt.iterator(); iter.hasNext(); ) {
            Matcher m = p.matcher(iter.next().toLowerCase());
            if (m.find()) {
                isHtml = true;
                return true;
            }
        }
        return false;
    }

    public void Print(ArrayList<String> l) {
        for (Iterator<String> iter = l.iterator(); iter.hasNext(); ) {
            System.out.println("#: " + iter.next());
        }
    }

    public void Print(String s) {
        System.out.println("#: " + s);
    }

    public void PrintPage() {
        OutputStream os = System.out;
        OutputStreamWriter osw = new OutputStreamWriter(os);
        try {
            osw.write("Page path: " + this.pagePath + "\n");
            osw.write("Page encoding: " + this.coding + "\n");
            osw.write("Page title: " + this.title + "\n");
            osw.write("Page key words: " + "\n");
            for (Iterator<String> iter = this.keyWords.iterator(); iter.hasNext(); ) {
                osw.write(iter.next() + " ");
            }
            osw.write("\n");
            osw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void WritePage(String filename, boolean isAppend) {
        try {
            OutputStream os = new FileOutputStream(filename, isAppend);
            OutputStreamWriter osw = new OutputStreamWriter(os);
            BufferedWriter bw = new BufferedWriter(osw);
            bw.write("Page path: " + this.pagePath + "\n");
            bw.write("Page encoding: " + this.coding + "\n");
            bw.write("Page title: " + this.title + "\n");
            bw.write("Page key words: " + "\n");
            for (Iterator<String> iter = this.keyWords.iterator(); iter.hasNext(); ) {
                bw.write(iter.next() + " ");
            }
            bw.write("\n");
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void Write(String filename, String s) {
        try {
            OutputStream os = new FileOutputStream(filename);
            OutputStreamWriter osw = new OutputStreamWriter(os);
            BufferedWriter bw = new BufferedWriter(osw);
            bw.append(s + "\n");
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void Write(String filename, ArrayList<String> l) {
        try {
            OutputStream os = new FileOutputStream(filename);
            OutputStreamWriter osw = new OutputStreamWriter(os);
            BufferedWriter bw = new BufferedWriter(osw);
            for (Iterator<String> iter = l.iterator(); iter.hasNext(); ) {
                bw.write(iter.next() + "\n");
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void SetCoding(String coding) {
        this.coding = coding;
    }

    @Override
    public void SetIsHtml(boolean isHtml) {
        this.isHtml = isHtml;
    }

    @Override
    public void SetPagePath(String pagePath) {
        this.pagePath = pagePath;
    }

    @Override
    public void SetTitle(String title) {
        this.title = title;
    }

    @Override
    public String GetCoding() {
        return this.coding;
    }

    @Override
    public boolean GetIsHtml() {
        return this.isHtml;
    }

    @Override
    public String GetPagePath() {
        return this.pagePath;
    }

    @Override
    public String GetTitle() {
        return this.title;
    }

    @Override
    public ArrayList<String> GetLinks() {
        return this.links;
    }

    @Override
    public ArrayList<String> GetKeyWords() {
        return this.keyWords;
    }

    @Override
    public ArrayList<String> GetText() {
        return this.txt;
    }
}
