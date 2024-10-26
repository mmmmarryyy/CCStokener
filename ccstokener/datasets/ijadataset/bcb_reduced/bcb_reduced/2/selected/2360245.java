package Parser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

public class HTMLParser {

    URL url;

    ArrayList<String> nameList, typeList;

    public ArrayList<String> getNameList() {
        return nameList;
    }

    public void setNameList(ArrayList<String> nameList) {
        this.nameList = nameList;
    }

    public ArrayList<String> getTypeList() {
        return typeList;
    }

    public void setTypeList(ArrayList<String> typeList) {
        this.typeList = typeList;
    }

    public HTMLParser() {
    }

    public void initializeIt(String path) {
        nameList = new ArrayList<String>();
        typeList = new ArrayList<String>();
        try {
            url = new URL(path);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HTMLEditorKit kit = new HTMLEditorKit();
        HTMLDocument doc = (HTMLDocument) kit.createDefaultDocument();
        doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
        Reader HTMLReader;
        try {
            HTMLReader = new InputStreamReader(url.openConnection().getInputStream());
            kit.read(HTMLReader, doc, 0);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        ElementIterator it = new ElementIterator(doc);
        Element elem;
        while ((elem = it.next()) != null) {
            String elemanismi = elem.getName();
            String elemanDegeri = (String) elem.getAttributes().getAttribute(HTML.Attribute.TYPE);
            nameList.add(elemanismi);
            typeList.add(elemanDegeri);
        }
    }
}
