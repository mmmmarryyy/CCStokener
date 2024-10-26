package main;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class URLTest {

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        BufferedReader in = null;
        try {
            URL url = new URL("http://api.openstreetmap.org/api/0.6/map?bbox=14.59821,50.11001,14.60419,50.11411");
            URLConnection conn = url.openConnection();
            InputStream inStr = conn.getInputStream();
            conn.connect();
            XMLInputFactory inXML = XMLInputFactory.newInstance();
            XMLStreamReader reader = inXML.createXMLStreamReader(inStr);
            DefaultMutableTreeNode current = new DefaultMutableTreeNode();
            parseRestOfDocument(reader, current);
            Enumeration e = current.preorderEnumeration();
            System.out.println("root " + current.getFirstChild().getChildCount());
            TreeNode osmNode = current.getFirstChild().getChildAt(1);
            for (Enumeration en = current.getFirstChild().children(); en.hasMoreElements(); ) {
                if (osmNode.getChildCount() > 0) {
                    System.out.println(osmNode.toString());
                }
                osmNode = (TreeNode) en.nextElement();
            }
        } catch (MalformedURLException ex) {
            System.err.println(ex);
        } catch (FileNotFoundException ex) {
            System.err.println("Failed to open stream to URL: " + ex);
        } catch (IOException ex) {
            System.err.println("Error reading URL content: " + ex);
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
        if (in != null) try {
            in.close();
        } catch (IOException ex) {
        }
    }

    private static void parseRestOfDocument(XMLStreamReader reader, DefaultMutableTreeNode current) throws XMLStreamException {
        while (reader.hasNext()) {
            int type = reader.next();
            switch(type) {
                case XMLStreamConstants.START_ELEMENT:
                    DefaultMutableTreeNode element = new DefaultMutableTreeNode("Element: " + reader.getLocalName());
                    current.add(element);
                    current = element;
                    if (reader.getNamespaceURI() != null) {
                        String prefix = reader.getPrefix();
                        if (prefix == null) {
                            prefix = "[None]";
                        }
                        DefaultMutableTreeNode namespace = new DefaultMutableTreeNode("Namespace: prefix = '" + prefix + "', URI = '" + reader.getNamespaceURI() + "'");
                        current.add(namespace);
                    }
                    if (reader.getAttributeCount() > 0) {
                        for (int i = 0; i < reader.getAttributeCount(); i++) {
                            DefaultMutableTreeNode attribute = new DefaultMutableTreeNode("Attribute (name = '" + reader.getAttributeLocalName(i) + "', value = '" + reader.getAttributeValue(i) + "')");
                            String attURI = reader.getAttributeNamespace(i);
                            if (attURI != null) {
                                String attPrefix = reader.getAttributePrefix(i);
                                if (attPrefix == null || attPrefix.equals("")) {
                                    attPrefix = "[None]";
                                }
                                DefaultMutableTreeNode attNamespace = new DefaultMutableTreeNode("Namespace: prefix = '" + attPrefix + "', URI = '" + attURI + "'");
                                attribute.add(attNamespace);
                            }
                            current.add(attribute);
                        }
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    current = (DefaultMutableTreeNode) current.getParent();
                    break;
                case XMLStreamConstants.CHARACTERS:
                    if (!reader.isWhiteSpace()) {
                        DefaultMutableTreeNode data = new DefaultMutableTreeNode("Character Data: '" + reader.getText() + "'");
                        current.add(data);
                    }
                    break;
                case XMLStreamConstants.DTD:
                    DefaultMutableTreeNode dtd = new DefaultMutableTreeNode("DTD: '" + reader.getText() + "'");
                    current.add(dtd);
                    break;
                case XMLStreamConstants.SPACE:
                    break;
                case XMLStreamConstants.COMMENT:
                    DefaultMutableTreeNode comment = new DefaultMutableTreeNode("Comment: '" + reader.getText() + "'");
                    current.add(comment);
                    break;
                default:
            }
        }
    }
}
