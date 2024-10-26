package itjava.tests;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;

public class Html2Text extends HTMLEditorKit.ParserCallback {

    StringBuffer s;

    public Html2Text() {
    }

    public void parse(Reader in) throws IOException {
        s = new StringBuffer();
        ParserDelegator delegator = new ParserDelegator();
        delegator.parse(in, this, Boolean.TRUE);
    }

    public void handleText(char[] text, int pos) {
        s.append(text);
    }

    public String getText() {
        return s.toString();
    }

    public static void main(String[] args) {
        try {
            URL url = new URL("http://www.java2s.com/Tutorial/Java/0180__File/UsingScannertoreceiveuserinput.htm");
            URLConnection conn = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine;
            String finalContents = "";
            while ((inputLine = reader.readLine()) != null) {
                finalContents += "\n" + inputLine.replace("<br", "\n<br").replaceAll("<code", "<pre").replaceAll("code>", "pre>");
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter("samples/testHtml.html"));
            writer.write(finalContents);
            writer.close();
            FileReader in = new FileReader("samples/testHtml.html");
            Html2Text parser = new Html2Text();
            parser.parse(in);
            in.close();
            System.out.println(parser.getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
