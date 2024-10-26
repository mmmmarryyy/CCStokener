package user.losacorp.html;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.JDomSerializer;
import org.htmlcleaner.TagNode;
import org.jdom.Document;
import org.jdom.Element;

/**
 * @author Javier Lo
 * 
 */
public class Tarantula {

    private static boolean isArchivo;

    private InputStream in = null;

    private List<TagNode> nodes;

    private static CleanerProperties props;

    public Tarantula(String urlSite) {
        try {
            if (!isArchivo()) {
                in = (new URL(urlSite)).openStream();
            } else {
                in = new FileInputStream(new File(urlSite));
            }
            HtmlCleaner cleaner = new HtmlCleaner();
            setProps(cleaner.getProperties());
            TagNode node = cleaner.clean(in);
            nodes = node.getElementListByAttValue("class", "entry", true, true);
        } catch (MalformedURLException e) {
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * @return the isArchivo
	 */
    public static boolean isArchivo() {
        return isArchivo;
    }

    /**
	 * @param isArchivo
	 *                the isArchivo to set
	 */
    public static void setArchivo(boolean isArchivo) {
        Tarantula.isArchivo = isArchivo;
    }

    /**
	 * @return the nodes
	 */
    public List<TagNode> getNodes() {
        return nodes;
    }

    /**
	 * @param nodes
	 *                the nodes to set
	 */
    public void setNodes(List<TagNode> nodes) {
        this.nodes = nodes;
    }

    public static void main(String[] args) {
        setArchivo(false);
        String urlSite = "http://www.techeblog.com/index.php/tech-gadget/dupont-circle-snowball-fight-kicks-off-snowpocalypse";
        Tarantula taran = new Tarantula(urlSite);
        for (TagNode node : taran.getNodes()) {
            System.out.println(node.getName() + " - " + node.getAttributeByName("class") + " " + node.getText());
            System.out.println(node);
            Document myJDom = new JDomSerializer(props, true).createJDom(node);
            System.out.println(myJDom.toString());
            Element rootElement = myJDom.getRootElement();
            System.out.println(rootElement.getChildren().toString());
            System.out.println(rootElement.getChild("p").getChildren().toString());
            System.out.println(node.getAllElementsList(false).toString());
            List<TagNode> titles = node.getElementListByAttValue("class", "title", true, false);
            System.out.println("titles: " + titles.toString());
            List<TagNode> images = node.getElementListByName("img", true);
            System.out.println("images: " + images.toString());
            List<TagNode> paragraphs = node.getElementListByName("p", true);
            System.out.println("paragraphs: " + paragraphs.toString());
            List<TagNode> objects = node.getElementListByName("object", true);
            System.out.println("objects: " + objects.toString());
        }
    }

    public void setProps(CleanerProperties props) {
        this.props = props;
    }

    public CleanerProperties getProps() {
        return props;
    }
}
