package br.ufrgs.inf.prav.interop.metadata;

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**
 *
 * @author Fernando
 */
public class JDOMReader {

    public JDOMReader() {
    }

    public Config readXML(URL url) {
        Element root = null;
        SAXBuilder parser = new SAXBuilder();
        try {
            Document doc = parser.build(url.openStream());
            root = doc.getRootElement();
        } catch (Exception e) {
            System.out.println("Erro on JDOMReader.readXML: " + e.getMessage());
        }
        return performRead(root);
    }

    public Config readXML(String fileName) {
        Element root = null;
        if (fileName == null) {
            System.out.println("A XML passada como parâmetro não pode ser nula.");
        }
        SAXBuilder parser = new SAXBuilder();
        try {
            Document doc = parser.build(new File(fileName));
            root = doc.getRootElement();
        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
        }
        return performRead(root);
    }

    private Config performRead(Element root) {
        Config config = new Config();
        try {
            List<Element> items = root.getChildren("MediaItem");
            Iterator<Element> i = items.iterator();
            while (i.hasNext()) {
                Element item = i.next();
                MediaItem mediaItem = new MediaItem();
                mediaItem.setId(item.getChildText("id"));
                mediaItem.setFile(item.getChildText("file"));
                mediaItem.setTargetDeviceClass(item.getChildText("targetDeviceClass"));
                config.getItems().add(mediaItem);
            }
        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
        }
        return config;
    }
}
