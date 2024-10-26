package ch.fhnw.wi.fit.ruleengine.control.parser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import ch.fhnw.wi.fit.ruleengine.abstraction.RuleFactory;
import ch.fhnw.wi.fit.ruleengine.abstraction.rule.IAtom;
import ch.fhnw.wi.fit.ruleengine.abstraction.rule.IBody;
import ch.fhnw.wi.fit.ruleengine.abstraction.rule.IHead;
import ch.fhnw.wi.fit.ruleengine.abstraction.rule.IRule;
import ch.fhnw.wi.fit.ruleengine.abstraction.rule.IRuleSet;
import ch.fhnw.wi.fit.ruleengine.abstraction.rule.atoms.IBuildinAtom;
import ch.fhnw.wi.fit.ruleengine.abstraction.rule.atoms.IClassAtom;
import ch.fhnw.wi.fit.ruleengine.abstraction.rule.atoms.IDatavaluedPropertyAtom;
import ch.fhnw.wi.fit.ruleengine.abstraction.rule.atoms.IIndividualPropertyAtom;
import ch.fhnw.wi.fit.ruleengine.abstraction.rule.atoms.buildins.IEqualBuildinAtom;
import ch.fhnw.wi.fit.ruleengine.abstraction.rule.atoms.buildins.IGreaterThanBuildinAtom;
import ch.fhnw.wi.fit.ruleengine.abstraction.rule.atoms.buildins.IGreaterThanOrEqualBuildinAtom;
import ch.fhnw.wi.fit.ruleengine.abstraction.rule.atoms.buildins.ILessThanBuildinAtom;
import ch.fhnw.wi.fit.ruleengine.abstraction.rule.atoms.buildins.ILessThanOrEqualBuildinAtom;
import ch.fhnw.wi.fit.ruleengine.abstraction.rule.atoms.buildins.IUnequalBuildin;
import ch.fhnw.wi.fit.ruleengine.control.exceptions.OntologyManagerException;
import ch.fhnw.wi.fit.ruleengine.control.exceptions.SWRLXParseException;

/**
 * Parse a SWRLX-RuleSet and set all data of IRuleSet.
 * 
 * @author daniela.feldkamp
 * 
 */
public class SWRLXParser {

    /**
	 * Parse a SWRLX-RuleSet, sets all data of IRuleSet and return the ruleset.
	 * 
	 * @param swrlFile
	 *            the file, containing the rule set.
	 * @return the IRuleSet, the SWRLX-model.
	 * @throws SWRLParseException
	 *             is thrown while parsing the swrlx-file
	 * @throws SWRLXParseException
	 *             is thrown while parsing the file
	 * @throws IOException
	 */
    public IRuleSet getRuleSetByURL(String url) throws SWRLXParseException, IOException {
        URL url2 = new URL(url);
        InputStream stream = url2.openStream();
        return this.getRuleSetByInputStream(stream);
    }

    /**
	 * Parse a SWRLX-RuleSet, sets all data of IRuleSet and return the ruleset.
	 * 
	 * @param swrlFile
	 *            the file, containing the rule set.
	 * @return the IRuleSet, the SWRLX-model.
	 * @throws SWRLParseException
	 *             is thrown while parsing the swrlx-file
	 * @throws SWRLXParseException
	 *             is thrown while parsing the file
	 * @throws IOException
	 */
    public IRuleSet getRuleSetByInputStream(InputStream stream) throws SWRLXParseException, IOException {
        IRuleSet ruleSet = RuleFactory.createDefaultRuleSet();
        Element root = this.getRootElement(stream);
        this.addRule(root, ruleSet);
        stream.close();
        return ruleSet;
    }

    /**
	 * Parse a SWRLX-RuleSet, sets all data of IRuleSet and return the ruleset.
	 * 
	 * @param swrlFile
	 *            the file, containing the rule set.
	 * @return the IRuleSet, the SWRLX-model.
	 * @throws SWRLParseException
	 *             is thrown while parsing the swrlx-file
	 * @throws SWRLXParseException
	 *             is thrown while parsing the file
	 * @throws IOException
	 * @throws OntologyManagerException 
	 */
    public IRuleSet getRuleSetByActivityID(String activityID, OntModel ontologyModel) throws SWRLXParseException, IOException, OntologyManagerException {
        URL url2 = new URL(this.getRuleSetURL(ontologyModel, activityID));
        InputStream stream = url2.openStream();
        return this.getRuleSetByInputStream(stream);
    }

    private String getRuleSetURL(OntModel ontologyModel, String activityID) throws OntologyManagerException {
        OntClass atomiProcessClass = ontologyModel.getOntClass("http://www.daml.org/services/owl-s/1.2/generic/ObjectList.owl#AtomicProcess");
        OntClass activiyClass = null;
        Iterator it = atomiProcessClass.listSubClasses();
        while (it.hasNext()) {
            OntClass currentOntologyClass = (OntClass) it.next();
            if (currentOntologyClass.getLabel("en") != null && currentOntologyClass.getLabel("en").equals(activityID)) {
                activiyClass = currentOntologyClass;
            } else if (currentOntologyClass.getURI().indexOf(activityID) > 0) {
                activiyClass = currentOntologyClass;
            }
        }
        if (activiyClass == null) {
            atomiProcessClass = ontologyModel.getOntClass("http://www.daml.org/services/owl-s/1.2/generic/ObjectList.owl#CompositeProcess");
            it = atomiProcessClass.listSubClasses();
            while (it.hasNext()) {
                OntClass currentOntologyClass = (OntClass) it.next();
                System.out.println(currentOntologyClass + ": " + activityID);
                if (currentOntologyClass.getLabel("en") != null && currentOntologyClass.getLabel("en").equals(activityID)) {
                    activiyClass = currentOntologyClass;
                } else if (currentOntologyClass.getURI().indexOf(activityID) > 0) {
                    activiyClass = currentOntologyClass;
                }
            }
        }
        if (activiyClass == null) {
        } else {
            Iterator instances = activiyClass.listInstances();
            Individual ind = null;
            boolean foundInd = false;
            while (instances.hasNext() && !foundInd) {
                ind = (Individual) instances.next();
                foundInd = true;
            }
            if (ind == null) {
                throw new OntologyManagerException("Cannot find " + activityID);
            } else {
                OntProperty prop = ontologyModel.getOntProperty("http://ch.fhnw.ch/fit/FITOntology#isRelatedToRuleSet");
                RDFNode node = ind.getPropertyValue(prop);
                Resource res = (Resource) node;
                OntProperty prop2 = ontologyModel.getOntProperty("http://ch.fhnw.ch/fit/FITOntology#storedIn");
                return res.getProperty(prop2).getString();
            }
        }
        return null;
    }

    /**
	 * Add rules to the rule set. Iterates through every rule and add their
	 * atoms to the head and body.
	 * 
	 * @param root
	 *            the root element, of the swrlx-file.
	 * @param ruleSet
	 *            the current rule set, to which the rules should be added.
	 * @throws SWRLXParseException
	 *             is thrown while parsing the file
	 */
    private void addRule(Element root, IRuleSet ruleSet) throws SWRLXParseException {
        NodeList ruleElements = root.getElementsByTagName("ruleml:imp");
        for (int index = 0; index < ruleElements.getLength(); index++) {
            Element ruleElement = (Element) ruleElements.item(index);
            IRule newRule = RuleFactory.createDefaultRule();
            newRule.setBody(this.getBody(ruleElement));
            newRule.setHeader(this.getHeader(ruleElement));
            ruleSet.addRule(newRule);
        }
    }

    /**
	 * Create a default body and adds all atoms of the body-part of the
	 * swrlx-file.
	 * 
	 * @param rule
	 *            the element, which contains the whole rule.
	 * @return the body containing all atoms.
	 * @throws SWRLXParseException
	 *             is thrown while parsing the file
	 */
    private IBody getBody(Element rule) throws SWRLXParseException {
        NodeList bodyElements = rule.getElementsByTagName("ruleml:_body");
        IBody body = RuleFactory.createDefaultBody();
        if (bodyElements.getLength() > 0) {
            Element bodyElement = (Element) bodyElements.item(0);
            NodeList atomElements = bodyElement.getChildNodes();
            body.setListOfAtoms(this.getListOfAtoms(atomElements));
        }
        return body;
    }

    /**
	 * Create a default header and adds all atoms of the head-part of the
	 * swrlx-file.
	 * 
	 * @param rule
	 *            the element, which contains the whole rule.
	 * @return the head containing all atoms.
	 * @throws SWRLXParseException
	 *             is thrown while parsing the file
	 */
    private IHead getHeader(Element rule) throws SWRLXParseException {
        NodeList headerElements = rule.getElementsByTagName("ruleml:_head");
        IHead header = RuleFactory.createDefaultHeader();
        if (headerElements.getLength() > 0) {
            Element headerElement = (Element) headerElements.item(0);
            NodeList atomElements = headerElement.getChildNodes();
            header.setListOfAtoms(this.getListOfAtoms(atomElements));
        }
        return header;
    }

    /**
	 * Creates and returns a list of atoms.
	 * 
	 * @param atomElements
	 *            a list of all atom-elements
	 * @return a list of all atoms.
	 * @throws SWRLXParseException
	 *             is thrown while parsing the SWRLX-File
	 */
    private ArrayList<IAtom> getListOfAtoms(NodeList atomElements) throws SWRLXParseException {
        ArrayList<IAtom> allAtoms = new ArrayList<IAtom>();
        for (int index = 0; index < atomElements.getLength(); index++) {
            Node currentNode = atomElements.item(index);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                Element atomElement = (Element) currentNode;
                allAtoms.add(this.getAtom(atomElement));
            }
        }
        return allAtoms;
    }

    /**
	 * Creates a default class or property atom and add the data of the atoms,
	 * like URI of the class or property, first and second argument.
	 * 
	 * @param atomElement
	 *            the element, which presents the current atom.
	 * @return IAtom presenting a class or property atom
	 * @throws SWRLXParseException
	 *             is thrown while parsing the file
	 */
    private IAtom getAtom(Element atomElement) throws SWRLXParseException {
        try {
            if (atomElement.getNodeName().equals("swrlx:classAtom")) {
                return this.getClassAtom(atomElement);
            } else if (atomElement.getNodeName().equals("swrlx:datavaluedPropertyAtom")) {
                return this.getDatavaluedPropertyAtom(atomElement);
            } else if (atomElement.getNodeName().equals("swrlx:individualPropertyAtom")) {
                return this.getIndividualPropertyAtom(atomElement);
            } else if (atomElement.getNodeName().equals("swrlx:builtinAtom")) {
                return this.getBuildin(atomElement);
            }
        } catch (SWRLXParseException e) {
            throw new SWRLXParseException(e + "\n " + "at element :" + atomElement.getLocalName());
        }
        return null;
    }

    /**
	 * Returns a specific build in atom.
	 * 
	 * @param atomElement
	 *            the current element.
	 * @return a specific build in atom.
	 */
    private IBuildinAtom getBuildin(Element atomElement) {
        String attributeValue = atomElement.getAttribute("swrlx:builtin");
        String nameSpace = "http://www.w3.org/2003/11/swrlb#";
        if (attributeValue.equals(nameSpace + "equal")) {
            IEqualBuildinAtom buildin = RuleFactory.createDefaultEqualBuildin();
            this.setDataOfBuildinAtom(buildin, atomElement);
            return buildin;
        } else if (attributeValue.equals(nameSpace + "notEqual")) {
            IUnequalBuildin buildin = RuleFactory.createDefaultUnequalBuildin();
            this.setDataOfBuildinAtom(buildin, atomElement);
            return buildin;
        } else if (attributeValue.equals(nameSpace + "lessThan")) {
            ILessThanBuildinAtom buildin = RuleFactory.createDefaultLessThanBuildinAtom();
            this.setDataOfBuildinAtom(buildin, atomElement);
            return buildin;
        } else if (attributeValue.equals(nameSpace + "greaterThan")) {
            IGreaterThanBuildinAtom buildin = RuleFactory.createDefaultGreaterThanBuildinAtom();
            this.setDataOfBuildinAtom(buildin, atomElement);
            return buildin;
        } else if (attributeValue.equals(nameSpace + "greaterThanOrEqual")) {
            IGreaterThanOrEqualBuildinAtom buildin = RuleFactory.createDefaultGreaterThanOrEqualBuildinAtom();
            this.setDataOfBuildinAtom(buildin, atomElement);
            return buildin;
        } else if (attributeValue.equals(nameSpace + "lessThanOrEqual")) {
            ILessThanOrEqualBuildinAtom buildin = RuleFactory.createDefaultLessThanOrEqualBuildinAtom();
            this.setDataOfBuildinAtom(buildin, atomElement);
            return buildin;
        } else {
            return null;
        }
    }

    /**
	 * Creates a default buildin atom and sets all data.
	 * 
	 * @param atomElement
	 *            the current elment, which contains all buildin atom data
	 * @return the buildin atom.
	 */
    private void setDataOfBuildinAtom(IBuildinAtom buildinAtom, Element atomElement) {
        NodeList elements = atomElement.getElementsByTagName("ruleml:var");
        buildinAtom.setArgument1(elements.item(0).getChildNodes().item(0).getNodeValue());
        if (elements.getLength() > 1) {
            buildinAtom.setArgument2(elements.item(1).getChildNodes().item(0).getNodeValue());
        } else {
            NodeList datavalueList = atomElement.getElementsByTagName("owlx:datavalue");
            Element dataValue = (Element) datavalueList.item(0);
            buildinAtom.setType(dataValue.getAttribute("owlx:datatype"));
            buildinAtom.setValue(dataValue.getChildNodes().item(0).getNodeValue());
        }
    }

    /**
	 * Creates and returns a class atom
	 * 
	 * @param atomElement
	 *            the atom element, containing all values of a class atom.
	 * @return the class atom
	 * @throws SWRLXParseException
	 *             is thrown while parsing the element
	 */
    private IClassAtom getClassAtom(Element atomElement) throws SWRLXParseException {
        IClassAtom classAtom = RuleFactory.createDefaultClassAtom();
        Element classElement;
        classElement = this.getElementByTagName(atomElement, "owlx:Class");
        classAtom.setURI(classElement.getAttribute("owl:name"));
        String argument1 = this.getArgument(0, atomElement);
        if (argument1 != null) {
            classAtom.setArgument1(argument1);
        } else {
            Element indElement = this.getElementByTagName(atomElement, "owlx:Individual");
            classAtom.setIndividualName(indElement.getAttribute("owlx:name"));
        }
        return classAtom;
    }

    /**
	 * Creates and returns an individual property atom.
	 * 
	 * @param atomElement
	 *            a element containing all data of an individual property atom
	 * @return the individual property atom
	 */
    private IIndividualPropertyAtom getIndividualPropertyAtom(Element atomElement) {
        IIndividualPropertyAtom propertyAtom = RuleFactory.createDefaultIndividualPropertyAtom();
        propertyAtom.setURI(atomElement.getAttribute("swrlx:property"));
        propertyAtom.setArgument1(this.getArgument(0, atomElement));
        propertyAtom.setArgument2(this.getArgument(1, atomElement));
        return propertyAtom;
    }

    /**
	 * Creates and returns a datavalue property atom
	 * 
	 * @param atomElement
	 *            the current element, containing all values of a property atom
	 * @return a datavalued property atom.
	 */
    private IDatavaluedPropertyAtom getDatavaluedPropertyAtom(Element atomElement) {
        IDatavaluedPropertyAtom propertyAtom = RuleFactory.createDefaultDatavaluedPropertyAtom();
        propertyAtom.setURI(atomElement.getAttribute("swrlx:property"));
        propertyAtom.setArgument1(this.getArgument(0, atomElement));
        NodeList nodes = atomElement.getElementsByTagName("ruleml:var");
        if (nodes.getLength() > 1) {
            propertyAtom.setArgument2(this.getArgument(1, atomElement));
        } else {
            nodes = atomElement.getElementsByTagName("owlx:datavalue");
            Element dataValueNode = (Element) nodes.item(0);
            String value = dataValueNode.getChildNodes().item(0).getNodeValue();
            propertyAtom.setValue(value);
            propertyAtom.setType(dataValueNode.getAttribute("owlx:datatype"));
        }
        return propertyAtom;
    }

    /**
	 * Iterates through the variable declaration of the tag ruleml:var and
	 * returns the first argument, if part == 0 and otherwise the second
	 * argument.
	 * 
	 * @param part
	 *            if part == 0, then the first argument is returned otherwise
	 *            the second
	 * @param atomElement
	 *            the element, which contains the variables.
	 * @return the argument
	 */
    private String getArgument(int part, Element atomElement) {
        if (atomElement.getElementsByTagName("ruleml:var").getLength() == 0) {
            return null;
        } else {
            if (part == 0) {
                NodeList nodes = atomElement.getElementsByTagName("ruleml:var");
                return nodes.item(0).getChildNodes().item(0).getNodeValue();
            } else {
                NodeList nodes = atomElement.getElementsByTagName("ruleml:var");
                return nodes.item(1).getChildNodes().item(0).getNodeValue();
            }
        }
    }

    /**
	 * Get a nodelist of descentant elements with a given tagName and returns
	 * the first element.
	 * 
	 * @param currentElement
	 *            the root element
	 * @param tagName
	 *            the name of the tag, of which the first element should be
	 *            returned.
	 * @return the first element
	 * @throws SWRLXParseException
	 */
    private Element getElementByTagName(Element currentElement, String tagName) throws SWRLXParseException {
        NodeList nodeList = currentElement.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return (Element) nodeList.item(0);
        } else {
            throw new SWRLXParseException("The element " + currentElement.getNodeName() + " contains child elements");
        }
    }

    /**
	 * Returns the root element of the swrl-file <rdf:RDF>
	 * 
	 * @param swrlFile
	 *            the whole swrl file
	 * @return the root element
	 * @throws IOException
	 * @throws SWRLParseException
	 *             is thrown while parsing a swrl file
	 */
    private Element getRootElement(InputStream swrlFile) throws SWRLXParseException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document swrlDocument;
        DocumentBuilder builder;
        Element root = null;
        try {
            builder = factory.newDocumentBuilder();
            swrlDocument = builder.parse(swrlFile);
            root = swrlDocument.getDocumentElement();
        } catch (ParserConfigurationException e) {
            throw new SWRLXParseException(e.getMessage(), e.getCause());
        } catch (SAXException e) {
            throw new SWRLXParseException(e.getMessage(), e.getCause());
        } catch (IOException e) {
            throw new SWRLXParseException(e.getMessage(), e.getCause());
        }
        swrlFile.close();
        return root;
    }
}
