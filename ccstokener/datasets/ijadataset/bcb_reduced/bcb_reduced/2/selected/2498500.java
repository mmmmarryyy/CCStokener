package net.sourceforge.fluxion.runcible.io;

import net.sourceforge.fluxion.runcible.*;
import net.sourceforge.fluxion.runcible.impl.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Javadocs go here.
 *
 * @author Tony Burdett
 * @version 1.0
 * @date 23-Apr-2007
 */
public class RuncibleRulesParser {

    public static Mapping parseMapping(URL url) throws ParserException, InvalidXMLException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(url.openStream());
            Element mapElement = doc.getDocumentElement();
            Mapping mapping = new MappingImpl();
            mapping.setSourceOntologyURI(URI.create(mapElement.lookupNamespaceURI("source")));
            mapping.setTargetOntologyURI(URI.create(mapElement.lookupNamespaceURI("target")));
            List<Rule> scope = new ArrayList<Rule>();
            List<Element> ruleElements = extractElements(mapElement.getChildNodes(), "Rule");
            if (ruleElements.size() == 0) {
                throw new InvalidXMLException("Zero Rule elements in this document");
            }
            for (Element element : ruleElements) {
                mapping.addRule(parseRule(scope, element));
            }
            return mapping;
        } catch (IOException e) {
            throw new ParserException(e);
        } catch (ParserConfigurationException e) {
            throw new ParserException(e);
        } catch (SAXException e) {
            throw new ParserException(e);
        }
    }

    protected static Rule parseRule(List<Rule> scope, Element ruleElement) throws InvalidXMLException, ParserException {
        Rule rule = new StandardRule();
        List<Element> foralls = extractElements(ruleElement.getChildNodes(), "for_all");
        if (foralls.size() != 1) {
            throw new InvalidXMLException(ruleElement + " does not have exactly 1 <for_all>");
        }
        Element forallElement = foralls.iterator().next();
        Forall forall = parseForall(forallElement);
        rule.setForall(forall);
        List<Element> in_inds = extractElements(ruleElement.getChildNodes(), "in_individuals");
        if (in_inds.size() != 1) {
            throw new InvalidXMLException(ruleElement + " does not have exactly 1 <in_individuals>");
        }
        Element in_ind = in_inds.iterator().next();
        rule.setIndividualSelection(parseIndividualSelection(in_ind, forall.getVariableID()));
        List<Element> using_datas = extractElements(ruleElement.getChildNodes(), "using_data");
        if (using_datas.size() > 1) {
            throw new InvalidXMLException(ruleElement + " has more than 1 <using_data> elements");
        }
        for (Element using_data : using_datas) {
            List<Element> data_clauses = extractElements(using_data.getChildNodes(), "DataClause");
            if (data_clauses.size() < 1) {
                throw new InvalidXMLException(using_data + " should have at least 1 <DataClause>");
            }
            for (Element data_clause : data_clauses) {
                rule.addDataClause(parseDataClause(data_clause));
            }
        }
        List<Element> do_actions = extractElements(ruleElement.getChildNodes(), "do_action");
        if (do_actions.size() > 1) {
            throw new InvalidXMLException(ruleElement + " has more than 1 <do_action>");
        }
        for (Element do_action : do_actions) {
            NodeList facts = do_action.getChildNodes();
            for (int i = 0; i < facts.getLength(); i++) {
                if (facts.item(i) instanceof Element) {
                    Element fact = (Element) facts.item(i);
                    rule.addFact(parseFact(fact));
                }
            }
        }
        List<Element> then_applys = extractElements(ruleElement.getChildNodes(), "then_apply");
        if (then_applys.size() > 1) {
            throw new InvalidXMLException(ruleElement + " has more than 1 <then_apply>");
        }
        List<Rule> nextRules = new ArrayList<Rule>();
        for (Element then_apply : then_applys) {
            List<Element> nextRuleElements = extractElements(then_apply.getChildNodes(), "Rule");
            for (Element nextRuleElement : nextRuleElements) {
                scope.add(rule);
                Rule nextRule = parseRule(scope, nextRuleElement);
                scope.remove(rule);
                nextRules.add(nextRule);
            }
        }
        rule.setNextRules(nextRules);
        return rule;
    }

    protected static Forall parseForall(Element forallElement) {
        String id = forallElement.getTextContent();
        Forall f = new Forall();
        f.setVariableID(id);
        return f;
    }

    protected static IndividualSelection parseIndividualSelection(Element individualElement, String for_all) throws InvalidXMLException, ParserException {
        List<Element> inPlaceElements = extractElements(individualElement.getChildNodes(), "InPlace");
        List<Element> selectIndividualvalueElements = extractElements(individualElement.getChildNodes(), "SelectIndividualvalue");
        List<Element> rootAtElements = extractElements(individualElement.getChildNodes(), "RootAt");
        if ((inPlaceElements.size() + selectIndividualvalueElements.size() + rootAtElements.size()) != 1) {
            throw new InvalidXMLException(individualElement + " must have at least one selection declaration" + ", is actually " + (inPlaceElements.size() + selectIndividualvalueElements.size()));
        }
        for (Element inPlaceElement : inPlaceElements) {
            InPlace inPlace = new InPlace();
            List<Element> filterElements = extractElements(inPlaceElement.getChildNodes(), "filter");
            if (filterElements.size() != 1) {
                throw new InvalidXMLException(inPlace + " must have at one filter declaration");
            }
            Element filterElement = filterElements.iterator().next();
            List<Element> owlClassElements = extractElements(filterElement.getChildNodes(), "OWLClass");
            List<Element> hasValueElements = extractElements(filterElement.getChildNodes(), "DataHasValue");
            if ((owlClassElements.size() + hasValueElements.size()) != 1) {
                throw new InvalidXMLException("The <filter> element must describe exactly 1 element, either an <OWLClass> or a <DataHasValue>. Got " + owlClassElements.size() + " <OWLClass> and " + hasValueElements.size() + " <DataHasValue> elements");
            }
            if (owlClassElements.size() > 1) {
                throw new InvalidXMLException("More than one <OWLClass> element defined at <for_all>" + for_all + "</for_all>");
            }
            for (Element owlClassElement : owlClassElements) {
                String uriValue = owlClassElement.getAttributeNode("URI").getValue();
                inPlace.setOWLClassURI(URI.create(uriValue));
                return inPlace;
            }
            if (hasValueElements.size() > 1) {
                throw new InvalidXMLException("No constant defined at element:\n" + "<for_all>" + for_all + "</for_all>\n" + "<in_individuals>\n" + "\t<InPlace>\n" + "\t\t<filter>\n" + "\t\t\t<owl:DataHasValue> <<<--- NOT DEFINED");
            }
            for (Element dataHasValueElement : hasValueElements) {
                List<Element> dataPropertyElements = extractElements(dataHasValueElement.getChildNodes(), "DataProperty");
                if (dataPropertyElements.size() != 1) {
                    throw new InvalidXMLException("No constant defined at element:\n" + "<for_all>" + for_all + "</for_all>\n" + "<in_individuals>\n" + "\t<InPlace>\n" + "\t\t<filter>\n" + "\t\t\t<owl:DataHasValue>\n" + "\t\t\t\t<owl:DataProperty> <<<--- NOT DEFINED");
                }
                Element onPropElement = dataPropertyElements.iterator().next();
                String propURIValue = onPropElement.getAttribute("URI");
                URI propertyURI = URI.create(propURIValue);
                List<Element> constantElements = extractElements(dataHasValueElement.getChildNodes(), "Constant");
                List<Element> variableElements = extractElements(dataHasValueElement.getChildNodes(), "Variable");
                if (constantElements.size() + variableElements.size() != 1) {
                    throw new InvalidXMLException("No constant defined at element:\n" + "<for_all>" + for_all + "</for_all>\n" + "<in_individuals>\n" + "\t<InPlace>\n" + "\t\t<filter>\n" + "\t\t\t<owl:DataHasValue>\n" + "\t\t\t\t<owl:DataProperty URI=" + propertyURI + ">\n" + "\t\t\t\t<owl:Constant> <<<--- NOT DEFINED");
                }
                if (constantElements.size() == 1) {
                    Element hasValueElement = constantElements.iterator().next();
                    String value = hasValueElement.getTextContent();
                    inPlace.setValueRestriction(propertyURI, value);
                } else {
                    Element hasValueElement = variableElements.iterator().next();
                    String value = hasValueElement.getTextContent();
                    inPlace.setValueRestriction(propertyURI, value);
                }
                return inPlace;
            }
        }
        for (Element selectIndividualvalueElement : selectIndividualvalueElements) {
            SelectIndividualValue selectIndividualValue = new SelectIndividualValue();
            List<Element> follows = extractElements(selectIndividualvalueElement.getChildNodes(), "follow");
            if (follows.size() != 1) {
                throw new InvalidXMLException();
            }
            Element follow = follows.iterator().next();
            List<Element> objectProperties = extractElements(follow.getChildNodes(), "ObjectProperty");
            if (objectProperties.size() != 1) {
                throw new InvalidXMLException();
            }
            Element objectProperty = objectProperties.iterator().next();
            String propertyURIValue = objectProperty.getAttributeNode("URI").getValue();
            URI propertyURI = URI.create(propertyURIValue);
            selectIndividualValue.setObjectPropertyURI(propertyURI);
            return selectIndividualValue;
        }
        for (Element rootAtElement : rootAtElements) {
            RootAt rootAt = new RootAt();
            List<Element> selections = extractElements(rootAtElement.getChildNodes(), "selection");
            if (selections.size() != 1) {
                throw new InvalidXMLException();
            }
            Element selection = selections.iterator().next();
            List<Element> owlClasses = extractElements(selection.getChildNodes(), "OWLClass");
            if (owlClasses.size() != 1) {
                throw new InvalidXMLException();
            }
            Element owlClass = owlClasses.iterator().next();
            String classURIValue = owlClass.getAttributeNode("URI").getValue();
            URI classURI = URI.create(classURIValue);
            rootAt.setOWLClassURI(classURI);
            return rootAt;
        }
        throw new ParserException("Balls! We shouldn't have gotten down to hear, one of either InPlace, " + "SelectIndividualValue or RootAt should have returned");
    }

    protected static DataClause parseDataClause(Element dataElement) throws InvalidXMLException {
        DataClause dataClause = new DataClauseImpl();
        List<Element> variables = extractElements(dataElement.getChildNodes(), "variable");
        if (variables.size() != 1) {
            throw new InvalidXMLException();
        }
        Element variable = variables.iterator().next();
        String variableID = variable.getTextContent();
        List<Element> from_ranges = extractElements(dataElement.getChildNodes(), "from_range");
        if (from_ranges.size() != 1) {
            throw new InvalidXMLException();
        }
        Element from_range = from_ranges.iterator().next();
        List<Element> selectDatavalues = extractElements(from_range.getChildNodes(), "SelectDatavalue");
        if (selectDatavalues.size() != 1) {
            throw new InvalidXMLException("Got a DataClause with no SelectDatavalue, for variable " + variableID);
        }
        Element selectDatavalue = selectDatavalues.iterator().next();
        List<Element> follows = extractElements(selectDatavalue.getChildNodes(), "follow");
        if (follows.size() != 1) {
            throw new InvalidXMLException();
        }
        Element follow = follows.iterator().next();
        List<Element> dataProperties = extractElements(follow.getChildNodes(), "DataProperty");
        if (dataProperties.size() != 1) {
            throw new InvalidXMLException("No DataProperty to follow");
        }
        Element dataProperty = dataProperties.iterator().next();
        String propertyURIValue = dataProperty.getAttributeNode("URI").getValue();
        URI propertyURI = URI.create(propertyURIValue);
        SelectDataValue selectDataValue = new SelectDataValue();
        selectDataValue.setDataPropertyURI(propertyURI);
        dataClause.setVariableID(variableID);
        dataClause.setDataSelection(selectDataValue);
        return dataClause;
    }

    protected static Fact parseFact(Element factElement) throws ParserException, InvalidXMLException {
        if (factElement.getLocalName().matches("ClassAssertion")) {
            ClassAssertion classAssertion = new ClassAssertion();
            List<Element> classElements = extractElements(factElement.getChildNodes(), "OWLClass");
            if (classElements.size() != 1) {
                throw new InvalidXMLException();
            }
            Element classElement = classElements.iterator().next();
            String uriValue = classElement.getAttributeNode("URI").getValue();
            URI classURI = URI.create(uriValue);
            List<Element> variableElements = extractElements(factElement.getChildNodes(), "Variable");
            if (variableElements.size() != 1) {
                throw new InvalidXMLException();
            }
            Element variableElement = variableElements.iterator().next();
            String id = variableElement.getTextContent();
            classAssertion.setIndividualOf(classURI);
            classAssertion.setIdentifier(id);
            return classAssertion;
        } else if (factElement.getLocalName().matches("ObjectPropertyAssertion")) {
            ObjectValueAssertion objectValueAssertion = new ObjectValueAssertion();
            List<Element> objectPropertyElements = extractElements(factElement.getChildNodes(), "ObjectProperty");
            if (objectPropertyElements.size() != 1) {
                throw new InvalidXMLException();
            }
            Element objectPropertyElement = objectPropertyElements.iterator().next();
            String uriValue = objectPropertyElement.getAttributeNode("URI").getValue();
            URI objectPropURI = URI.create(uriValue);
            List<Element> variableElements = extractElements(factElement.getChildNodes(), "Variable");
            if (variableElements.size() != 2) {
                throw new InvalidXMLException();
            }
            Element variableFromElement = variableElements.get(0);
            String fromVariableID = variableFromElement.getTextContent();
            Element variableToElement = variableElements.get(1);
            String toVariableID = variableToElement.getTextContent();
            objectValueAssertion.setIdentifier(toVariableID);
            objectValueAssertion.setValueOf(fromVariableID);
            objectValueAssertion.setValueToSet(toVariableID);
            objectValueAssertion.setObjectPropertyURI(objectPropURI);
            return objectValueAssertion;
        } else if (factElement.getLocalName().matches("DataPropertyAssertion")) {
            List<Element> objectPropertyElements = extractElements(factElement.getChildNodes(), "DataProperty");
            if (objectPropertyElements.size() != 1) {
                throw new InvalidXMLException();
            }
            Element objectPropertyElement = objectPropertyElements.iterator().next();
            String uriValue = objectPropertyElement.getAttributeNode("URI").getValue();
            URI objectPropURI = URI.create(uriValue);
            List<Element> variableElements = extractElements(factElement.getChildNodes(), "Variable");
            if (variableElements.size() != 1) {
                throw new InvalidXMLException("Not exactly 1 Variable element, got " + variableElements.size());
            }
            Element variableElement = variableElements.iterator().next();
            String variable = variableElement.getTextContent();
            Element dataVariableElement = null;
            String dataVariable = null;
            List<Element> dataVariableElements = extractElements(factElement.getChildNodes(), "DataVariable");
            if (dataVariableElements.size() != 1) {
                List<Element> dataConstantElements = extractElements(factElement.getChildNodes(), "Constant");
                if (dataConstantElements.size() != 1) {
                    throw new InvalidXMLException("No DataVariables or Constants found for DataPropertyAssertion " + uriValue);
                }
                dataVariableElement = dataConstantElements.iterator().next();
                dataVariable = dataVariableElement.getTextContent();
                DataConstantAssertion dataConstantAssertion = new DataConstantAssertion();
                dataConstantAssertion.setIdentifier(dataVariable);
                dataConstantAssertion.setValueOf(variable);
                dataConstantAssertion.setValueToSet(dataVariable);
                dataConstantAssertion.setDatatypePropertyURI(objectPropURI);
                return dataConstantAssertion;
            } else {
                dataVariableElement = dataVariableElements.iterator().next();
                dataVariable = dataVariableElement.getTextContent();
                DataValueAssertion dataValueAssertion = new DataValueAssertion();
                dataValueAssertion.setIdentifier(dataVariable);
                dataValueAssertion.setValueOf(variable);
                dataValueAssertion.setValueToSet(dataVariable);
                dataValueAssertion.setDatatypePropertyURI(objectPropURI);
                return dataValueAssertion;
            }
        }
        throw new ParserException("Should never have got to here! " + "Fact never got initialized but no error thrown?");
    }

    private static List<Element> extractElements(NodeList nodes, String elementName) {
        List<Element> elements = new ArrayList<Element>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node next = nodes.item(i);
            if (next instanceof Element) {
                Element element = (Element) nodes.item(i);
                if (element.getLocalName().matches(elementName)) {
                    elements.add(element);
                }
            }
        }
        return elements;
    }
}
