package leeon.mobile.BBSBrowser.test;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import leeon.kaixin.wap.action.LoginAction;
import leeon.kaixin.wap.util.HttpUtil;
import leeon.mobile.BBSBrowser.NetworkException;
import leeon.mobile.BBSBrowser.actions.BBSDocAction;
import leeon.mobile.BBSBrowser.actions.HttpConfig;
import leeon.mobile.BBSBrowser.models.BoardObject;
import leeon.mobile.BBSBrowser.models.DocObject;
import leeon.mobile.BBSBrowser.utils.HTMLUtil;
import leeon.mobile.BBSBrowser.utils.HTMLUtil.PatternListener;
import leeon.mobile.BBSBrowser.utils.HTTPUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.ccil.cowan.tagsoup.HTMLSchema;
import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class TestHtml {

    private static final Map<String, String> COLOR_MAP = new HashMap<String, String>();

    static {
        COLOR_MAP.put("40", "#ffffff");
        COLOR_MAP.put("41", "#ff0000");
        COLOR_MAP.put("42", "#00ff00");
        COLOR_MAP.put("43", "#ffff00");
        COLOR_MAP.put("44", "#0000ff");
        COLOR_MAP.put("45", "#ff00ff");
        COLOR_MAP.put("46", "#00ffff");
        COLOR_MAP.put("47", "#ffffff");
        COLOR_MAP.put("030", "#000000");
        COLOR_MAP.put("130", "#000000");
        COLOR_MAP.put("031", "#800000");
        COLOR_MAP.put("131", "#b00000");
        COLOR_MAP.put("032", "#008000");
        COLOR_MAP.put("132", "#00b000");
        COLOR_MAP.put("033", "#808000");
        COLOR_MAP.put("133", "#b0b000");
        COLOR_MAP.put("034", "#000080");
        COLOR_MAP.put("134", "#0000b0");
        COLOR_MAP.put("035", "#800080");
        COLOR_MAP.put("135", "#b000b0");
        COLOR_MAP.put("036", "#008080");
        COLOR_MAP.put("136", "#00b0b0");
        COLOR_MAP.put("037", "#000000");
        COLOR_MAP.put("137", "#000000");
    }

    /**
	 * @param args
	 * @throws Exception 
	 */
    public static void main() throws Exception {
        List<DocObject> list = new BBSDocAction().docContent(new DocObject("3085035256", null, null, null, null, true, new BoardObject("65", null, null)), false);
        String content = list.get(0).getContent();
        content = HTMLUtil.replacePattern(content, "http://[\\w\\p{Punct}]+", new PatternListener() {

            public String onPatternMatch(String source) {
                String end = source.substring(source.length() - 4);
                if (end.equalsIgnoreCase(".jpg") || end.equalsIgnoreCase(".png") || end.equalsIgnoreCase(".gif") || end.equalsIgnoreCase(".bmp")) return "<img\fwidth=\"" + 300 + "\"\fsrc=\"" + source + "\"/>"; else return "<a\fhref=\"" + source + "\">" + source + "</a>";
            }
        });
        content = content.replaceAll("\\n", "<br>");
        content = content.replaceAll(" ", "&nbsp;");
        content = content.replaceAll("\\f", " ");
        content = HTMLUtil.replacePattern(content, ">1b\\[[\\d;]*[a-zA-Z]", new PatternListener() {

            boolean inSpan = false;

            boolean dl = false;

            String hl = "0";

            String fc = "";

            String bc = "";

            public String onPatternMatch(String source) {
                if (!source.endsWith("m")) {
                    return "";
                }
                source = source.substring(4, source.length() - 1);
                String ret = "";
                boolean newSpan = false;
                if (inSpan) {
                    inSpan = false;
                    ret += "</span>";
                }
                if (source.length() == 0) {
                    fc = "";
                    bc = "";
                    dl = false;
                    return ret;
                }
                String[] tags = source.split(";");
                for (String tag : tags) {
                    if ("0".equals(tag)) {
                        hl = "0";
                        fc = "";
                        bc = "";
                        dl = false;
                    } else if ("1".equals(tag)) {
                        newSpan = true;
                        hl = "1";
                    } else if ("4".equals(tag)) {
                        newSpan = true;
                        dl = true;
                    } else if ("5".equals(tag)) {
                    } else if ("7".equals(tag)) {
                    } else if ("8".equals(tag)) {
                    } else {
                        if (tag.startsWith("3")) {
                            newSpan = true;
                            fc = tag;
                        } else if (tag.startsWith("4")) {
                            newSpan = true;
                            bc = tag;
                        }
                    }
                }
                if (newSpan) {
                    inSpan = true;
                    ret += "<span style=\"";
                    if (!"".equals(fc)) {
                        ret += "color:" + COLOR_MAP.get(hl + fc) + ";";
                    }
                    if (!"".equals(bc)) {
                        ret += "background-color:" + COLOR_MAP.get(bc) + ";";
                    }
                    if (dl) ret += "text-decoration:underline;";
                    ret += "\">";
                }
                return ret;
            }
        });
        System.out.println(content);
    }

    private static class HtmlParser {

        private static final HTMLSchema schema = new HTMLSchema();
    }

    public static void main(String[] args) throws Exception {
        String verify = "2538938_2538938_1301470122_f4df630273553820bea083ae0f52d4ba_kx";
        String start = "0";
        HttpClient client = HttpConfig.newInstance();
        String url = HttpUtil.KAIXIN_STATUS_URL + HttpUtil.KAIXIN_PARAM_UID + LoginAction.uid(verify);
        url += "&" + HttpUtil.KAIXIN_PARAM_VERIFY + verify;
        url += "&" + HttpUtil.KAIXIN_PRRAM_PSTART + start;
        HttpGet get = new HttpGet(url);
        HttpUtil.setHeader(get);
        try {
            HttpResponse response = client.execute(get);
            String html = HTTPUtil.toString(response.getEntity());
            System.out.println(html);
            fromHtml(html);
        } catch (Exception e) {
            e.printStackTrace();
            throw new NetworkException(e);
        }
    }

    public static void fromHtml(String source) {
        Parser parser = new Parser();
        try {
            parser.setProperty(Parser.schemaProperty, HtmlParser.schema);
        } catch (org.xml.sax.SAXNotRecognizedException e) {
            throw new RuntimeException(e);
        } catch (org.xml.sax.SAXNotSupportedException e) {
            throw new RuntimeException(e);
        }
        parser.setContentHandler(new ContentHandler() {

            public void characters(char[] arg0, int arg1, int arg2) throws SAXException {
                System.out.println(new String(arg0, arg1, arg2));
            }

            public void endElement(String arg0, String arg1, String arg2) throws SAXException {
                System.out.println("</" + arg2 + ">");
            }

            public void startElement(String arg0, String arg1, String arg2, Attributes arg3) throws SAXException {
                System.out.println("<" + arg2 + ">");
            }

            public void endDocument() throws SAXException {
            }

            public void endPrefixMapping(String arg0) throws SAXException {
            }

            public void ignorableWhitespace(char[] arg0, int arg1, int arg2) throws SAXException {
            }

            public void processingInstruction(String arg0, String arg1) throws SAXException {
            }

            public void setDocumentLocator(Locator arg0) {
            }

            public void skippedEntity(String arg0) throws SAXException {
            }

            public void startDocument() throws SAXException {
            }

            public void startPrefixMapping(String arg0, String arg1) throws SAXException {
            }
        });
        try {
            parser.parse(new InputSource(new StringReader(source)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }
}
