package pedro.soa.ontology.sources;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import pedro.util.ConfigurationURLUtility;
import pedro.system.*;
import pedro.util.Parameter;
import pedro.util.SystemLog;
import pedro.soa.ontology.provenance.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class XMLOntologySource extends AbstractTreeOntologySource {

    protected Hashtable webPageFromIdentifier;

    protected Hashtable definitionFromIdentifier;

    protected Hashtable labelFromIdentifier;

    protected Hashtable ontologyTermFromIdentifier;

    protected Hashtable relatedTermsFromIdentifier;

    protected Hashtable imageFromIdentifier;

    protected OntologyServiceMetaData ontologyServiceMetaData;

    public XMLOntologySource() {
        webPageFromIdentifier = new Hashtable();
        definitionFromIdentifier = new Hashtable();
        labelFromIdentifier = new Hashtable();
        ontologyTermFromIdentifier = new Hashtable();
        relatedTermsFromIdentifier = new Hashtable();
        imageFromIdentifier = new Hashtable();
        ontologyServiceMetaData = new OntologyServiceMetaData();
    }

    public void load() {
        try {
            isSourceWorking = true;
            URLConnection urlConnection = url.openConnection();
            ontologyServiceMetaData.setName("Ontology for " + url.getFile());
            parseDocument(urlConnection.getInputStream());
            buildTree();
            isSourceWorking = true;
            String statusOKMessage = PedroResources.getMessage("ontology.statusOK", url.getFile());
            status = new StringBuffer();
            status.append(statusOKMessage);
        } catch (Exception err) {
            err.printStackTrace(System.out);
            String statusErrorMessage = PedroResources.getMessage("ontology.statusError", err.toString());
            status.append(statusErrorMessage);
            isSourceWorking = false;
        }
    }

    private void parseDocument(InputStream inputStream) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(inputStream);
        Node currentChild = document.getFirstChild();
        Element rootElement = null;
        Element ontologyTermsElement = null;
        while (currentChild != null) {
            if (currentChild.getNodeType() == Node.ELEMENT_NODE) {
                Element currentElement = (Element) currentChild;
                String tagName = currentElement.getTagName();
                if (tagName.equals("ontologyTerms") == true) {
                    ontologyTermsElement = currentElement;
                    break;
                }
            }
            currentChild = currentChild.getNextSibling();
        }
        TreeOntologyTerm topLevelTerm = null;
        currentChild = ontologyTermsElement.getFirstChild();
        int i = 0;
        while (currentChild != null) {
            if (currentChild.getNodeType() == Node.ELEMENT_NODE) {
                Element currentTermDeclaration = (Element) currentChild;
                String tagName = currentTermDeclaration.getTagName();
                if (topLevelTerm == null) {
                    topLevelTerm = parseOntologyTerm(currentTermDeclaration);
                } else {
                    parseOntologyTerm(currentTermDeclaration);
                }
            }
            currentChild = currentChild.getNextSibling();
        }
        rootTerm = topLevelTerm;
    }

    protected TreeOntologyTerm parseOntologyTerm(Element ontologyDeclaration) {
        String idValue = ontologyDeclaration.getAttribute("identifier");
        String identifier = TermIdentifierUtility.createIdentifier(url, idValue);
        String relatedTerms = ontologyDeclaration.getAttribute("relatedTerms");
        relatedTermsFromIdentifier.put(identifier, relatedTerms);
        Node currentNode = ontologyDeclaration.getFirstChild();
        while (currentNode != null) {
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                Element currentElement = (Element) currentNode;
                String termPropertyName = currentElement.getTagName();
                String value = getFieldValue(currentElement);
                if (value != null) {
                    if (termPropertyName.equals("wordPhrase") == true) {
                        labelFromIdentifier.put(identifier, value);
                    } else if (termPropertyName.equals("definition") == true) {
                        definitionFromIdentifier.put(identifier, value);
                    } else if (termPropertyName.equals("webPage") == true) {
                        webPageFromIdentifier.put(identifier, value);
                    } else if (termPropertyName.equals("image") == true) {
                        imageFromIdentifier.put(identifier, value);
                    }
                }
            }
            currentNode = currentNode.getNextSibling();
        }
        String wordPhrase = (String) labelFromIdentifier.get(identifier);
        if (wordPhrase == null) {
            wordPhrase = PedroResources.EMPTY_STRING;
        }
        TreeOntologyTerm ontologyTerm = new TreeOntologyTerm(identifier, wordPhrase);
        ontologyTermFromIdentifier.put(identifier, ontologyTerm);
        return ontologyTerm;
    }

    private void buildTree() {
        ArrayList ontologyTerms = new ArrayList();
        ontologyTerms.addAll(ontologyTermFromIdentifier.values());
        int numberOfTerms = ontologyTerms.size();
        for (int i = 0; i < numberOfTerms; i++) {
            TreeOntologyTerm currentTerm = (TreeOntologyTerm) ontologyTerms.get(i);
            String identifier = currentTerm.getIdentifier();
            String relatedTermIDs = (String) relatedTermsFromIdentifier.get(identifier);
            addRelatedTerms(currentTerm, relatedTermIDs);
        }
    }

    private void addRelatedTerms(TreeOntologyTerm parentTerm, String relatedTerms) {
        String parentID = parentTerm.getIdentifier();
        StringTokenizer tokenizer = new StringTokenizer(relatedTerms);
        while (tokenizer.hasMoreTokens() == true) {
            String value = tokenizer.nextToken();
            String childIdentifier = TermIdentifierUtility.createIdentifier(url, value);
            TreeOntologyTerm childTerm = (TreeOntologyTerm) ontologyTermFromIdentifier.get(childIdentifier);
            if (childTerm != null) {
                childTerm.setParentTerm(parentTerm);
                parentTerm.addRelatedTerm(childTerm);
            }
        }
        ArrayList bl = parentTerm.getRelatedTerms();
    }

    public void setParameters(Parameter[] parameters) {
        super.setParameters(parameters);
        try {
            boolean loadDone = false;
            for (int i = 0; i < parameters.length; i++) {
                String parameterName = parameters[i].getName();
                if (parameterName.equals("inputFile") == true) {
                    String fileName = parameters[i].getValue();
                    setDescription(fileName);
                    url = ConfigurationURLUtility.createResourceURL(resourceDirectory, fileName);
                    if (loadDone == false) {
                        load();
                        loadDone = true;
                    }
                } else if (parameterName.equals("anchorTerm") == true) {
                    addAnchorTerm(parameters[i].getValue());
                }
            }
            if (loadDone == false) {
                String errorMessage = PedroResources.getMessage("ontology.fileError", ontologyServiceMetaData.getName());
                SystemLog.addError(errorMessage);
                isSourceWorking = false;
                status = new StringBuffer();
                status.append(errorMessage);
            } else {
                reviseTreeWithAnchorTerms();
            }
            return;
        } catch (Exception err) {
            SystemLog.addError(err);
        }
        return;
    }

    public OntologyTermProvenance getOntologyTermProvenance(PedroFormContext pedroFormContext, OntologyTerm ontologyTerm) {
        OntologyTermProvenance ontologyTermProvenance = new OntologyTermProvenance();
        ontologyTermProvenance.setOntologyServiceMetaData(ontologyServiceMetaData);
        OntologyTermMetaData ontologyTermMetaData = ontologyTermProvenance.getOntologyTermMetaData();
        String identifier = ontologyTerm.getIdentifier();
        ontologyTermMetaData.setIdentifier(identifier);
        ontologyTermMetaData.setLabel(ontologyTerm.getLabel());
        String definition = (String) definitionFromIdentifier.get(identifier);
        ontologyTermMetaData.setDefinition(definition);
        String urlText = (String) webPageFromIdentifier.get(identifier);
        if (urlText != null) {
            if (urlText.equals(PedroResources.EMPTY_STRING) == false) {
                try {
                    URL webPage = ConfigurationURLUtility.createResourceURL(resourceDirectory, urlText);
                    ontologyTermMetaData.setWebPage(webPage);
                } catch (Exception err) {
                    err.printStackTrace(System.out);
                }
            }
        }
        try {
            String imageFileName = (String) imageFromIdentifier.get(identifier);
            if (imageFileName != null) {
                URL imageURL = ConfigurationURLUtility.createResourceURL(resourceDirectory, imageFileName);
                ontologyTermMetaData.setImage(imageURL);
            }
        } catch (Exception err) {
        }
        return ontologyTermProvenance;
    }

    /**
     * convenience routine for extracting the text value for an element
     */
    protected String getFieldValue(Element element) {
        Node fieldChild = element.getFirstChild();
        if (fieldChild == null) {
            return null;
        }
        if (fieldChild.getNodeType() == Node.TEXT_NODE) {
            Text text = (Text) fieldChild;
            String data = text.getData();
            data = data.trim();
            return data;
        }
        return PedroResources.EMPTY_STRING;
    }

    public void printTree(PedroFormContext pedroFormContext, int indentationLevel, TreeOntologyTerm treeOntologyTerm) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < indentationLevel; i++) {
            buffer.append("\t");
        }
        OntologyTermProvenance ontologyTermProvenance = getOntologyTermProvenance(pedroFormContext, treeOntologyTerm);
        OntologyTermMetaData ontologyTermMetaData = ontologyTermProvenance.getOntologyTermMetaData();
        String label = ontologyTermMetaData.getLabel();
        buffer.append(label);
        System.out.println(buffer.toString());
        ArrayList relatedTerms = treeOntologyTerm.getRelatedTerms();
        int numberOfTerms = relatedTerms.size();
        for (int i = 0; i < numberOfTerms; i++) {
            TreeOntologyTerm currentTerm = (TreeOntologyTerm) relatedTerms.get(i);
            printTree(pedroFormContext, indentationLevel + 1, currentTerm);
        }
    }
}
