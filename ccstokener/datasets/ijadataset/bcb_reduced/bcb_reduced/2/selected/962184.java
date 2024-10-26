package prajna.semantic.ontology;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * Loader for loading RDF and OWL ontology files. This class loads the ontology
 * definitions from RDF or OWL files, either locally or on the web, and creates
 * all OwlClasses, properties, and OwlThings defined in the ontology. The
 * OwlLoader is implemented as a Singleton class.
 * 
 * @author <a href="http://www.ganae.com/edswing">Edward Swing</a>
 */
public class OwlLoader {

    private static DocumentBuilder docBuild;

    private static HashMap<URL, OwlOntology> loading = new HashMap<URL, OwlOntology>();

    static {
        try {
            loading.put(new URL("http://www.w3.org/2002/07/owl"), null);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            docBuild = factory.newDocumentBuilder();
        } catch (Exception exc) {
            System.err.println("Cannot initialize OwlLoader:");
            exc.printStackTrace();
        }
    }

    /**
     * Get the set of URLS that have been loaded
     * 
     * @return the set of URLS that have been loaded
     */
    public Set<URL> getLoadedUrls() {
        return loading.keySet();
    }

    /**
     * Get the Ontology definition for an ontology that has already been
     * loaded.
     * 
     * @param url The URL reference for the ontology
     * @return the ontology definition object
     */
    public OwlOntology getOntology(URL url) {
        return loading.get(url);
    }

    /**
     * Return whether the given URL has been loaded
     * 
     * @param url the URL to check
     * @return true if an ontology has been loaded with the given URL, false
     *         otherwise
     */
    public boolean isLoaded(URL url) {
        return loading.containsKey(url);
    }

    /**
     * Load the ontology data from the specified file. The argument may either
     * be a local file path or a URL on the web.
     * 
     * @param fileName the file name or URL path for the ontology
     * @throws IOException If there is a problem reading the file
     * @throws SAXException if there is a problem parsing the file
     * @return the URL used to load the data
     */
    public URL loadData(String fileName) throws IOException, SAXException {
        URL url = null;
        try {
            url = new URL(fileName);
            loadData(url);
        } catch (MalformedURLException exc) {
            url = new File(fileName).toURI().toURL();
            loadData(url);
        }
        return url;
    }

    /**
     * Load the ontology data from the specified URL.
     * 
     * @param url the URL path for the ontology
     * @throws IOException If there is a problem reading the file
     * @throws SAXException if there is a problem parsing the file
     * @return the URL used to load the data. This should be the argument
     *         passed in.
     */
    public URL loadData(URL url) throws IOException, SAXException {
        if (!loading.containsKey(url)) {
            loading.put(url, null);
            OwlOntology ont = null;
            InputStream inStream = url.openStream();
            Document doc = docBuild.parse(inStream);
            Element root = doc.getDocumentElement();
            NodeList nodes = root.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node instanceof Element) {
                    Element elem = (Element) node;
                    String tagName = elem.getTagName();
                    if (tagName.equals("owl:Ontology")) {
                        ont = new OwlOntology(elem);
                        String name = url.getFile();
                        int slashInx = name.lastIndexOf('/');
                        if (slashInx > -1) {
                            name = name.substring(slashInx + 1);
                        }
                        ont.setName(name);
                        ont.setUrl(url);
                        loading.put(url, ont);
                    } else if (tagName.equals("owlx:Annotation") || tagName.equals("owlx:VersionInfo")) {
                    } else if (tagName.equals("owl:Class") || tagName.equals("rdfs:Class") || tagName.equals("Class")) {
                        OwlClass.createFromElement(elem);
                    } else if (tagName.equals("owlx:SubClassOf")) {
                        parseSubClassElement(elem);
                    } else if (tagName.endsWith("ObjectProperty")) {
                        OwlObjectProperty.createFromElement(elem);
                    } else if (tagName.equals("owl:TransitiveProperty")) {
                        OwlObjectProperty prop = OwlObjectProperty.createFromElement(elem);
                        prop.setTransitive(true);
                    } else if (tagName.equals("owl:SymmetricProperty")) {
                        OwlObjectProperty prop = OwlObjectProperty.createFromElement(elem);
                        prop.setSymmetric(true);
                    } else if (tagName.equals("owl:FunctionalProperty")) {
                        OwlObjectProperty prop = OwlObjectProperty.createFromElement(elem);
                        prop.setFunctional(true);
                    } else if (tagName.equals("owl:InverseFunctionalProperty")) {
                        OwlObjectProperty prop = OwlObjectProperty.createFromElement(elem);
                        prop.setInverseFunctional(true);
                    } else if (tagName.endsWith("DatatypeProperty") || tagName.equals("rdf:Property")) {
                        OwlDatatypeProperty.createFromElement(elem);
                    } else if (tagName.endsWith("AnnotationProperty")) {
                        parseAnnotationProp(elem);
                    } else if (tagName.contains(":Description")) {
                    } else if (OwlClass.getClassNames().contains(tagName) || tagName.equals("owl:Thing") || tagName.endsWith(":Individual")) {
                        OwlThing.createFromElement(elem);
                    } else if (tagName.equals("owlx:SameIndividual")) {
                        parseSameThingElement(elem);
                    } else if (tagName.equals("owl:AllDifferent")) {
                    } else if (tagName.endsWith("DisjointClasses")) {
                        parseDisjointClasses(elem);
                    } else {
                        parseUnknownElement(elem);
                    }
                }
            }
            for (OwlThing thing : OwlThing.getAllThings()) {
                OwlClass cls = thing.getOwlClass();
                if (cls != null) {
                    cls.setDefinedValues(thing);
                    thing.validate();
                }
            }
        }
        return url;
    }

    /**
     * TODO: DOCUMENT ME
     * 
     * @param elem
     */
    private void parseAnnotationProp(Element elem) {
        NodeList typeList = elem.getElementsByTagName("rdf:type");
        if (typeList != null && typeList.getLength() > 0) {
            Element typeElem = (Element) typeList.item(0);
            String type = typeElem.getAttribute("rdf:resource");
            if (type != null) {
                if (type.contains("Datatype")) {
                    OwlDatatypeProperty.createFromElement(elem);
                } else if (type.contains("Object")) {
                    OwlObjectProperty.createFromElement(elem);
                }
            }
        }
    }

    /**
     * TODO: DOCUMENT ME
     * 
     * @param elem
     */
    private void parseDisjointClasses(Element elem) {
        HashSet<OwlClass> classSet = new HashSet<OwlClass>();
        NodeList members = elem.getElementsByTagName("owl:members");
        if (members.getLength() == 1) {
            Element memberElem = (Element) members.item(0);
            NodeList classElems = memberElem.getElementsByTagName("owl:Class");
            for (int i = 0; i < classElems.getLength(); i++) {
                Element clsElem = (Element) classElems.item(i);
                String clsName = clsElem.getAttribute("rdf:about");
                if (clsName != null) {
                    classSet.add(OwlClass.getOwlClass(clsName));
                }
            }
        } else {
            NodeList classElems = elem.getElementsByTagName("Class");
            for (int i = 0; i < classElems.getLength(); i++) {
                Element clsElem = (Element) classElems.item(i);
                String clsName = clsElem.getAttribute("IRI");
                if (clsName != null) {
                    classSet.add(OwlClass.getOwlClass(clsName));
                }
            }
        }
        for (OwlClass cls : classSet) {
            for (OwlClass disjoint : classSet) {
                if (!cls.equals(disjoint)) {
                    cls.addToDisjoint(disjoint);
                }
            }
        }
    }

    /**
     * Parse a SubClass element, which contains information about the class and
     * superclass.
     * 
     * @param elem the element containing class and superclass information
     */
    private void parseSubClassElement(Element elem) {
        Element subElem = (Element) elem.getElementsByTagName("owlx:sub").item(0);
        Element clsElem = (Element) subElem.getElementsByTagName("owlx:Class").item(0);
        String subName = clsElem.getAttribute("owlx:name");
        System.out.println("SubName " + subName);
        System.out.flush();
        OwlClass cls = OwlClass.getOwlClass(subName);
        Element supElem = (Element) elem.getElementsByTagName("owlx:super").item(0);
        cls.parseSubClassElement(supElem);
    }

    /**
     * Parse an element identifying things as being the same thing
     * 
     * @param elem the SameAs element
     */
    private void parseSameThingElement(Element elem) {
        HashMap<String, Integer> things = new HashMap<String, Integer>();
        int mostRelations = 0;
        OwlThing master = null;
        NodeList list = elem.getElementsByTagName("owlx:Individual");
        for (int i = 0; i < list.getLength(); i++) {
            Element thingElem = (Element) list.item(i);
            String name = thingElem.getAttribute("owlx:name").substring(1);
            OwlThing thing = OwlThing.findOwlThing(name);
            if (thing != null && thing.getName().equals(name)) {
                int relSize = thing.getRelationships().size();
                things.put(name, relSize);
                if (relSize > mostRelations) {
                    master = thing;
                    mostRelations = relSize;
                }
            } else {
                things.put(name, 0);
            }
        }
        for (String name : things.keySet()) {
            int cnt = things.get(name);
            if (!master.getName().equals(name)) {
                if (cnt > 0) {
                    OwlThing thing = OwlThing.findOwlThing(name);
                    master.mergeThing(thing);
                } else {
                    master.addSameAs(name);
                }
            }
        }
    }

    /**
     * Parse an element with an unknown tag. This method allows the OwlLoader
     * to be extended to incorporate extensions to OWL. The default
     * implementation does nothing.
     * 
     * @param elem the element to parse
     */
    protected void parseUnknownElement(Element elem) {
    }
}
