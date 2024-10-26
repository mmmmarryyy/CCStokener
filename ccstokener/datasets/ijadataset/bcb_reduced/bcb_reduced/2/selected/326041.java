package textfilter.parse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.ParagraphTag;
import org.htmlparser.tags.TitleTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

public class TextParser {

    public static void main(String args[]) throws Exception {
        String encoding = "UTF-8";
        String path = "http://mobile.csdn.net/n/20100604/267264.html";
        String content = TextParser.getContent(path, encoding);
        String title = TextParser.getTitle(content);
        List<String> strlist = getContentList(content);
        System.out.println("title is:" + title);
        System.out.println("content is:");
        for (int i = 0; i < strlist.size(); i++) {
            System.out.println(strlist.get(i));
        }
    }

    public static String getContent(String path, String encoding) throws IOException {
        URL url = new URL(path);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        InputStream inputStream = conn.getInputStream();
        InputStreamReader isr = new InputStreamReader(inputStream, encoding);
        StringBuffer sb = new StringBuffer();
        BufferedReader in = new BufferedReader(isr);
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            sb.append(inputLine);
            sb.append("\n");
        }
        return sb.toString();
    }

    public static String getTitle(String content) {
        Pattern titlePattern = Pattern.compile("<title>.*</title>");
        Matcher titleMatcher = titlePattern.matcher(content);
        if (titleMatcher.find()) {
            return titleMatcher.group().replaceAll("<[/]*title>", "");
        } else {
            return "";
        }
    }

    public static ArrayList<String> getContentList(String content) {
        ArrayList<String> paraList = new ArrayList<String>();
        Parser myParser = new Parser();
        NodeList nodeList = null;
        NodeFilter paraFilter = new NodeClassFilter(ParagraphTag.class);
        NodeFilter titleFilter = new NodeClassFilter(TitleTag.class);
        OrFilter lastFilter = new OrFilter();
        lastFilter.setPredicates(new NodeFilter[] { paraFilter, titleFilter });
        try {
            myParser.setInputHTML(content);
            nodeList = myParser.parse(paraFilter);
            Node[] nodes = nodeList.toNodeArray();
            String line = "";
            for (int i = 0; i < nodes.length; i++) {
                Node node = nodes[i];
                if (node instanceof TextNode) {
                    TextNode textnode = (TextNode) node;
                    line = textnode.getText();
                    System.out.println("textnode.");
                } else if (node instanceof LinkTag) {
                } else if (node instanceof TitleTag) {
                    TitleTag titlenode = (TitleTag) node;
                    line = titlenode.getTitle();
                    System.err.println("------------------title-------------");
                } else if (node instanceof ParagraphTag) {
                    ParagraphTag paraTag = (ParagraphTag) node;
                    line = paraTag.getStringText();
                }
                if (line != null && !line.contains("<style") && !line.contains("</style>") && !line.contains("<script>") && !line.contains("</script>")) {
                    line = line.replaceAll("<[Aa].*href=.*>", "");
                    line = line.replaceAll("&nbsp;", "");
                    line = line.replaceAll("&gt;", "");
                    line = line.replaceAll("<strong>", "");
                    line = line.replaceAll("<STRONG>", "");
                    line = line.replaceAll("</STRONG>", "");
                    line = line.replaceAll("</strong>", "");
                    line = line.replaceAll("<[Bb][rR]>", "\n");
                    line = line.replaceAll("<!--.*-->", "");
                    line = line.replaceAll("<img .*/>", "");
                    line = line.replaceAll("<IMG .*>", "");
                    line = line.replaceAll("<span .* </span>", "");
                    line = line.replaceAll("&[lr]dquo;", "\"");
                    line = line.replaceAll("&hellip;", "...");
                    line = line.replaceAll("<FONT.*>", "");
                    line = line.replaceAll("</FONT>", "");
                    line = line.replaceAll("<span .*</span>", "");
                    if (!line.trim().equals("")) {
                        paraList.add(line.trim());
                    }
                }
            }
        } catch (ParserException e) {
            e.printStackTrace();
        }
        return paraList;
    }
}
