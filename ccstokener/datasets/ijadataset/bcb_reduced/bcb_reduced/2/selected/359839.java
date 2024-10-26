package org.htmltransfer.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmltransfer.ds.MyNode;
import org.htmltransfer.ds.TRNode;

/**
 * @author jude
 *
 * @version 0.1 2011-4-8
 *
 * @version 0.2 2011-4-11
 * Use TRNode to replace MyNode.
 *
 * @version 0.3 2011-4-18
 * Add table property in the output file.
 */
public class parserTable {

    private int tableNum = 0;

    private int rowNum[];

    /**
     * Get HTML string according to the given URL address
     * @param URL path
     * @return HTML String
     * @throws Exception
     */
    public String getHtml(String path) throws Exception {
        URL url = new URL(path);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        InputStream inputStream = conn.getInputStream();
        InputStreamReader isr = new InputStreamReader(inputStream, "UTF-8");
        StringBuilder sb = new StringBuilder();
        BufferedReader in = new BufferedReader(isr);
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            sb.append(inputLine);
        }
        String result = sb.toString();
        return result;
    }

    /**
     * Get text that I want to show. The data is stored in table, and each
     * table must has a id attribute.The value of attribute must be "GridView1 or
     * GridView2. Maybe the attribute should be a parameter which
     * decided by user. I will do it later:)
     * @param result
     * @return text string that I needed
     * @throws Exception
     */
    public String readTable(String result) throws Exception {
        Parser parser;
        NodeList nodelist;
        parser = Parser.createParser(result, "UTF-8");
        NodeFilter gridView1 = new HasAttributeFilter("id", "GridView1");
        NodeFilter gridView2 = new HasAttributeFilter("id", "GridView2");
        OrFilter orfilter = new OrFilter();
        orfilter.setPredicates(new NodeFilter[] { gridView1, gridView2 });
        nodelist = parser.parse(orfilter);
        Node[] nodes = nodelist.toNodeArray();
        String pageinfo = "{tableNum:";
        String text = "";
        tableNum = nodes.length;
        rowNum = new int[tableNum];
        pageinfo += tableNum + ";";
        for (int i = 0; i < nodes.length; i++) {
            Node node = nodes[i];
            if (node instanceof TableTag) {
                TableTag table = (TableTag) node;
                TableRow[] rows = table.getRows();
                rowNum[i] = rows.length;
                pageinfo += rowNum[i] + ",";
                for (int j = 0; j < rows.length; j++) {
                    String content = "";
                    TableRow tr = (TableRow) rows[j];
                    TableColumn[] td = tr.getColumns();
                    TRNode tn = new TRNode();
                    for (int k = 0; k < td.length; k++) {
                        content += td[k].toPlainTextString().trim() + ",";
                        Node itd = td[k].childAt(0);
                        if (itd instanceof LinkTag) {
                            String url = "";
                            LinkTag lt = (LinkTag) itd;
                            url = lt.getLink();
                            tn.setHasLink(true);
                            tn.setLinkUrl(url);
                        }
                    }
                    tn.setContent(content);
                    text += tn.toString() + "\n";
                }
            }
        }
        pageinfo += "}\n";
        return pageinfo + text;
    }
}
