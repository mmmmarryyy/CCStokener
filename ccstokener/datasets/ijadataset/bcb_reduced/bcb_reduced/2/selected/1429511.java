package com.simconomy.magic.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.CssSelectorNodeFilter;
import org.htmlparser.filters.HasChildFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import com.google.appengine.repackaged.com.google.common.util.Base64;
import com.simconomy.magic.exceptions.CardPriceException;
import com.simconomy.magic.jpa.Serie;
import com.simconomy.magic.model.Price;

public class HtmlParseServiceImpl implements HtmlParseService {

    public List<Price> getPrices(String html, Serie serie) throws CardPriceException {
        List<Price> prices = new ArrayList<Price>();
        List<TableRow> nodes = getDate(html);
        for (TableRow tableRow : nodes) {
            Node object = tableRow.childAt(5).getChildren().elementAt(1).getFirstChild();
            String name = object.getText();
            Node object2 = tableRow.childAt(7).getChildren().elementAt(1);
            if (object2 == null) {
                continue;
            }
            String image = object2.getText().replace("img src='priceGen.php?p=", "").replace("'", "");
            String test = new String(Base64.encode(image.getBytes()));
            String test2 = "";
            Price price = new Price(name, serie, image);
            prices.add(price);
        }
        return prices;
    }

    private List<TableRow> getDate(String html) throws CardPriceException {
        List<TableRow> rows = new ArrayList<TableRow>();
        try {
            URL url = new URL("http://www.mtgotraders.com/store/" + html);
            Parser parser = new Parser(url.openConnection());
            NodeList list = new NodeList();
            NodeFilter filter = new AndFilter(new TagNameFilter("TR"), new HasChildFilter(new CssSelectorNodeFilter(".products")));
            for (NodeIterator e = parser.elements(); e.hasMoreNodes(); ) {
                e.nextNode().collectInto(list, filter);
            }
            for (Node node : list.toNodeArray()) {
                rows.add((TableRow) node);
            }
            return rows;
        } catch (MalformedURLException e) {
            throw new CardPriceException(e);
        } catch (IOException e) {
            throw new CardPriceException(e);
        } catch (ParserException e) {
            throw new CardPriceException(e);
        }
    }
}
