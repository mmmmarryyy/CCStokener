package org.omnidoc.uml;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.middleheaven.io.ManagedIOException;
import org.omnidoc.BinaryDocument;
import org.omnidoc.Document;
import org.omnidoc.DocumentTransformer;
import org.omnidoc.io.IOUtils;
import org.omnidoc.process.DocumentSpace;
import org.omnidoc.xml.XMLDOMDocument;
import org.omnidoc.xml.XMLUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class YmlUmlTransformer implements DocumentTransformer {

    private int diagramIndex = 0;

    @Override
    public boolean canTransform(Document doc) {
        return doc instanceof XMLDOMDocument;
    }

    @Override
    public void transform(Document doc, DocumentSpace target) {
        try {
            XMLDOMDocument xmlDoc = (XMLDOMDocument) doc.duplicate();
            target.addDocument(xmlDoc);
            org.w3c.dom.Document xdoc = xmlDoc.asXMLDocument();
            NodeList list = xdoc.getElementsByTagName("uml");
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                String type = XMLUtils.getStringAttribute("type", node, "class");
                String legend = XMLUtils.getStringAttribute("legend", node, "");
                diagramIndex++;
                String code = node.getTextContent();
                BinaryDocument pic = documentFor(code, type, diagramIndex);
                target.addDocument(pic);
                StringBuilder b = new StringBuilder("<uml");
                if (!legend.isEmpty()) {
                    b.append(" legend=\"" + legend + "\" ");
                }
                b.append("><img src=\"" + pic.getName() + "\" />");
                b.append("</uml>");
                Node imgNode = XMLUtils.parseXml(xdoc, b.toString());
                Node replaceNodeParent = node.getParentNode();
                replaceNodeParent.replaceChild(imgNode, node);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private BinaryDocument documentFor(String code, String type, int diagramIndex) {
        code = code.replaceAll("\n", "").replaceAll("\t", "").trim().replaceAll(" ", "%20");
        StringBuilder builder = new StringBuilder("http://yuml.me/diagram/");
        builder.append(type).append("/");
        builder.append(code);
        URL url;
        try {
            url = new URL(builder.toString());
            String name = "uml" + diagramIndex + ".png";
            diagramIndex++;
            BinaryDocument pic = new BinaryDocument(name, "image/png");
            IOUtils.copy(url.openStream(), pic.getContent().getOutputStream());
            return pic;
        } catch (MalformedURLException e) {
            throw ManagedIOException.manage(e);
        } catch (IOException e) {
            throw ManagedIOException.manage(e);
        }
    }
}
