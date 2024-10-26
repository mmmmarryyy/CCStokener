package fr.ign.cogit.geoxygene.schema.util.persistance;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import fr.ign.cogit.geoxygene.schema.schemaConceptuelISOJeu.AttributeType;
import fr.ign.cogit.geoxygene.schema.schemaConceptuelISOJeu.FeatureType;
import fr.ign.cogit.geoxygene.schema.schemaConceptuelISOJeu.SchemaConceptuelJeu;

/**
 * @author Balley transforme un GMLschema, c'est à dire un fichier x.xsd
 *         conforme à http://schemas.opengis.net/gml/2.1.2/feature.xsd, en un
 *         SchemaISOJeu
 * 
 *         TODO gérer le GML3 et les xlink, s'occuper plus en détail des types
 *         géométriques
 */
public class ChargeurGMLSchema {

    Document docXSD;

    public ChargeurGMLSchema() {
    }

    public ChargeurGMLSchema(Document doc) {
        this.docXSD = doc;
    }

    /**
   * @param args FIXME c'est très moche : il y a des noms de fichiers locaux...
   */
    public static void main(String[] args) {
        try {
            File fichierXSD = new File("D:/Users/Balley/données/gml/commune.xsd");
            URL urlFichierXSD = fichierXSD.toURI().toURL();
            InputStream isXSD = urlFichierXSD.openStream();
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbFactory.newDocumentBuilder();
            Document documentXSD = (builder.parse(isXSD));
            ChargeurGMLSchema chargeur = new ChargeurGMLSchema(documentXSD);
            SchemaConceptuelJeu sc = chargeur.gmlSchema2schemaConceptuel(documentXSD);
            System.out.println(sc.getFeatureTypes().size());
            for (int i = 0; i < sc.getFeatureTypes().size(); i++) {
                System.out.println(sc.getFeatureTypes().get(i).getTypeName());
                for (int j = 0; j < sc.getFeatureTypes().get(i).getFeatureAttributes().size(); j++) {
                    System.out.println("    " + sc.getFeatureTypes().get(i).getFeatureAttributes().get(j).getMemberName() + " : " + sc.getFeatureTypes().get(i).getFeatureAttributes().get(j).getValueType());
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    /**
   * transforme un GMLSchema déjà parsé en un objet SchemaISOJeu contenant une
   * liste de FeatureType.
   * @param newDocXSD
   * @return SchemaConceptuelJeu
   */
    public SchemaConceptuelJeu gmlSchema2schemaConceptuel(Document newDocXSD) {
        SchemaConceptuelJeu schemaConceptuel = new SchemaConceptuelJeu();
        NodeList listeNoeuds = newDocXSD.getElementsByTagName("extension");
        Node noeudAbstractFeatureType = null;
        String nomType = null;
        String nomElementsDeCeType = null;
        String typeSimple = null;
        for (int i = 0; i < listeNoeuds.getLength(); i++) {
            if (listeNoeuds.item(i).getAttributes().getNamedItem("base").getNodeValue().equals("gml:AbstractFeatureType")) {
                System.out.println("type = " + listeNoeuds.item(i).getParentNode().getParentNode().getNodeName());
                noeudAbstractFeatureType = listeNoeuds.item(i).getParentNode().getParentNode();
                nomType = noeudAbstractFeatureType.getAttributes().getNamedItem("name").getNodeValue();
                System.out.println("le type : " + nomType + " etend AbstractFeatureType");
                System.out.println("\nLecture des attributs...");
                NodeList listElements = newDocXSD.getElementsByTagName("element");
                for (int j = 0; j < listElements.getLength(); j++) {
                    if (listElements.item(j).getAttributes().getNamedItem("type") != null) {
                        if (listElements.item(j).getAttributes().getNamedItem("type").getNodeValue().equals("gml2:" + nomType)) {
                            nomElementsDeCeType = listElements.item(j).getAttributes().getNamedItem("name").getNodeValue();
                            FeatureType ft = new FeatureType();
                            ft.setTypeName(nomElementsDeCeType);
                            Node noeudSequence = noeudAbstractFeatureType.getChildNodes().item(1).getChildNodes().item(1).getChildNodes().item(1);
                            NodeList noeudsAttributs = noeudSequence.getChildNodes();
                            Node noeudAttribut = null;
                            AttributeType fa = null;
                            for (int k = 1; k < noeudsAttributs.getLength() - 1; k = k + 2) {
                                noeudAttribut = noeudsAttributs.item(k);
                                if (noeudAttribut.getNodeName().equals("choice")) {
                                } else if (noeudAttribut.getNodeName().equals("element")) {
                                    System.out.println("name = " + noeudAttribut.getAttributes().getNamedItem("name").getNodeValue());
                                    fa = new AttributeType();
                                    fa.setMemberName(noeudAttribut.getAttributes().getNamedItem("name").getNodeValue());
                                    if (noeudAttribut.getAttributes().getNamedItem("type") != null) {
                                        fa.setValueType(noeudAttribut.getAttributes().getNamedItem("type").getNodeValue());
                                    } else if (noeudAttribut.hasChildNodes()) {
                                        for (int l = 0; l < noeudAttribut.getChildNodes().getLength(); l++) {
                                            if (noeudAttribut.getChildNodes().item(l).getNodeName().equals("simpleType")) {
                                                typeSimple = noeudAttribut.getChildNodes().item(l).getChildNodes().item(1).getAttributes().getNamedItem("base").getNodeValue();
                                                fa.setValueType(ChargeurGMLSchema.GMLType2schemaType(typeSimple));
                                            }
                                        }
                                    }
                                    ft.addFeatureAttribute(fa);
                                }
                            }
                            this.trouveTypeGeom(noeudAbstractFeatureType, ft);
                            ft.setIsExplicite(true);
                            schemaConceptuel.getFeatureTypes().add(ft);
                        }
                    }
                }
            }
            System.out.println();
        }
        return schemaConceptuel;
    }

    public void trouveTypeGeom(Node noeudFT, FeatureType ft) {
        AttributeType attribGeom = null;
        System.out.println("\nrecherche spatialité...");
        NodeList list = noeudFT.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
        }
        NodeList list2 = this.docXSD.getElementsByTagName("element");
        for (int i = 0; i < list2.getLength(); i++) {
            if (list2.item(i).getAttributes().getNamedItem("ref") != null) {
                Node noeudParent = list2.item(i);
                while (noeudParent != null) {
                    if (noeudParent.equals(noeudFT)) {
                        break;
                    }
                    noeudParent = noeudParent.getParentNode();
                }
                if (noeudParent == null) {
                }
                if (list2.item(i).getAttributes().getNamedItem("ref").getNodeValue().equals("gml:polygonProperty")) {
                    attribGeom = new AttributeType();
                    attribGeom.setMemberName("geom");
                    attribGeom.setValueType("surfacique");
                } else if (list2.item(i).getAttributes().getNamedItem("ref").getNodeValue().equals("gml:multiPolygonProperty")) {
                    attribGeom = new AttributeType();
                    attribGeom.setMemberName("geom2");
                    attribGeom.setValueType("surfacique multiple");
                }
                ft.addFeatureAttribute(attribGeom);
            }
        }
    }

    protected static String GMLType2schemaType(String GMLType) {
        if (GMLType.compareToIgnoreCase("string") == 0) {
            return "text";
        } else if (GMLType.compareToIgnoreCase("") == 0) {
            return "float";
        } else if (GMLType.compareToIgnoreCase("integer") == 0) {
            return "entier";
        } else if (GMLType.compareToIgnoreCase("") == 0) {
            return "bool";
        } else if (GMLType.compareToIgnoreCase("gml:lineStringProperty") == 0) {
            return "linéaire";
        } else if (GMLType.compareToIgnoreCase("gml:multiLineStringProperty") == 0) {
            return "linéaire";
        } else if (GMLType.compareToIgnoreCase("gml:pointProperty") == 0) {
            return "ponctuel";
        } else if (GMLType.compareToIgnoreCase("gml:multiPolygonProperty") == 0) {
            return "surfacique";
        } else if (GMLType.compareToIgnoreCase("gml:polygonProperty") == 0) {
            return "surfacique";
        } else if (GMLType.compareToIgnoreCase("") == 0) {
            return "arc";
        } else if (GMLType.compareToIgnoreCase("") == 0) {
            return "noeud";
        } else if (GMLType.compareToIgnoreCase("") == 0) {
            return "face";
        } else {
            return "text";
        }
    }
}
