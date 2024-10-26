import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
	Sample code to Parse and render GCS Category Response.
	Usage:
	java -cp . -Dfile.encoding=UTF-8 ParseGCSCategories api_key cx categories.include taxonomy
	
	where 
		categories.include  
		    roots(children:5)     --> get taxonomy 5 levels down from root   
			node:3(children:5)    --> get taxonomy 5 levels down from node id=3
		    node:3(parents)       --> get taxonomy levels up from node id=3
		taxonomy
		    private:common		  --> taxonomy to parse in the form 
		    							private:<group_name>
 */
public class ParseGCSCategories {

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage:  ParseGCSCategories apikey cx categories.include taxonomy");
            System.exit(0);
        }
        ParseGCSCategories pc = new ParseGCSCategories(args[0], args[1], args[2], args[3]);
    }

    public ParseGCSCategories(String api_key, String cx, String category, String taxonomy) {
        String base_shopping_query = "https://www.googleapis.com/shopping/search/v1/cx:" + cx + "/products?alt=atom&country=us&categories.enabled=true" + "&key=" + api_key + "&categories.include=" + category + "&taxonomy=" + taxonomy;
        try {
            URL url = new URL(base_shopping_query);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            InputStream inputStream = httpConnection.getInputStream();
            DocumentBuilderFactory docBF = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = (DocumentBuilder) docBF.newDocumentBuilder();
            Document doc = docBuilder.parse(inputStream);
            doc.getDocumentElement().normalize();
            NamespaceContext ctx = new NamespaceContext() {

                public String getNamespaceURI(String prefix) {
                    String uri;
                    if (prefix.equals("openSearch")) uri = "http://a9.com/-/spec/opensearchrss/1.0/"; else if (prefix.equals("s")) uri = "http://www.google.com/shopping/api/schemas/2010"; else if (prefix.equals("gd")) uri = "http://schemas.google.com/g/2005"; else if (prefix.equals("atom")) uri = "http://www.w3.org/2005/Atom"; else uri = null;
                    return uri;
                }

                public Iterator getPrefixes(String val) {
                    return null;
                }

                public String getPrefix(String uri) {
                    return null;
                }
            };
            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();
            xpath.setNamespaceContext(ctx);
            ArrayList<Category> al_cat = new ArrayList<Category>();
            NodeList nl_category = (NodeList) xpath.evaluate("/feed/categories/category", doc, XPathConstants.NODESET);
            for (int i = 0; i < nl_category.getLength(); i++) {
                Node n_category = nl_category.item(i);
                String id = n_category.getAttributes().getNamedItem("id").getFirstChild().getNodeValue();
                String name = n_category.getAttributes().getNamedItem("shortName").getFirstChild().getNodeValue();
                String surl = n_category.getAttributes().getNamedItem("url").getFirstChild().getNodeValue();
                NodeList nl_parents = (NodeList) xpath.evaluate("parent", n_category, XPathConstants.NODESET);
                Category c = null;
                String[] parents = new String[nl_parents.getLength()];
                if (nl_parents.getLength() > 0) {
                    for (int j = 0; j < nl_parents.getLength(); j++) {
                        String nparent = nl_parents.item(j).getAttributes().getNamedItem("id").getFirstChild().getNodeValue();
                        parents[j] = nparent;
                    }
                }
                c = new Category(name, id, surl, parents);
                al_cat.add(c);
            }
            System.out.println("Categories found: " + al_cat.size());
            ArrayList<Category> roots = getRelativeRootList(al_cat);
            String html_category = "";
            for (Category c : roots) {
                System.out.println("Found Root Node: [" + c.id + "]");
                html_category = html_category + c.name + " [" + c.id + "]\n";
                html_category = html_category + renderChildren(c, al_cat, " ");
            }
            System.out.println(html_category);
        } catch (Exception ex) {
            System.out.println("Error " + ex);
        }
    }

    private String renderChildren(Category inc, ArrayList<Category> al_cat, String sindents) {
        String ret = "";
        for (Category c : al_cat) {
            if (Arrays.asList(c.parents).contains(inc.id)) {
                ret = ret + (sindents + c.name + " [" + c.id + "]\n");
                ret = ret + renderChildren(c, al_cat, sindents + " ");
            }
        }
        return ret;
    }

    private ArrayList<Category> getRelativeRootList(ArrayList<Category> al_cat) {
        ArrayList<Category> rc = new ArrayList<Category>();
        for (Category c : al_cat) {
            if (!parentExists(c, al_cat)) rc.add(c);
        }
        return rc;
    }

    private boolean parentExists(Category c, ArrayList<Category> al_cat) {
        for (Category cc : al_cat) {
            if (Arrays.asList(c.parents).contains(cc.id)) return true;
        }
        return false;
    }
}

class Category {

    public String name;

    public String id;

    public String url;

    String[] parents;

    public Category(String name, String id, String url, String[] parents) {
        this.name = name;
        this.id = id;
        this.url = url;
        this.parents = parents;
    }

    public String toString() {
        return id + " " + name;
    }
}
