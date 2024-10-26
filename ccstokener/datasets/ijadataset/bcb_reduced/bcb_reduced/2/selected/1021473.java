package ishima.sandbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTML.Attribute;
import javax.swing.text.MutableAttributeSet;

public class HTMLUtils {

    private HTMLUtils() {
    }

    private static String currentLink = new String();

    public static List<String> extractLinks(Reader reader) throws IOException {
        final ArrayList<String> list = new ArrayList<String>();
        ParserDelegator parserDelegator = new ParserDelegator();
        ParserCallback parserCallback = new ParserCallback() {

            @Override
            public void handleText(final char[] data, final int pos) {
            }

            @Override
            public void handleStartTag(Tag tag, MutableAttributeSet attribute, int pos) {
                if (tag == Tag.A) {
                    String address = (String) attribute.getAttribute(Attribute.HREF);
                    currentLink = address;
                    list.add(address);
                }
            }

            @Override
            public void handleEndTag(Tag tag, final int pos) {
                if (tag == Tag.A) {
                    currentLink = null;
                }
            }

            @Override
            public void handleSimpleTag(Tag t, MutableAttributeSet a, final int pos) {
            }

            @Override
            public void handleComment(final char[] data, final int pos) {
            }

            @Override
            public void handleError(final java.lang.String errMsg, final int pos) {
            }
        };
        parserDelegator.parse(reader, parserCallback, false);
        return list;
    }

    public static final void main(String[] args) throws Exception {
        newRead();
    }

    public static final void newRead() {
        HTMLDocument html = new HTMLDocument();
        html.putProperty("IgnoreCharsetDirective", new Boolean(true));
        try {
            HTMLEditorKit kit = new HTMLEditorKit();
            URL url = new URL("http://omega.rtu.lv/en/index.html");
            kit.read(new BufferedReader(new InputStreamReader(url.openStream())), html, 0);
            Reader reader = new FileReader(html.getText(0, html.getLength()));
            List<String> links = HTMLUtils.extractLinks(reader);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
