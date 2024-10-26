package pedro.soa.ontology.sources;

import pedro.util.ConfigurationURLUtility;
import pedro.system.*;
import pedro.soa.ontology.provenance.*;
import pedro.util.Parameter;
import pedro.util.SystemLog;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Hashtable;

public class TabIndentedTextSource extends AbstractTreeOntologySource {

    private Hashtable rootsForIndentationLevels;

    private TreeOntologyTerm overAllRootTerm;

    private int numberOfIndents;

    private OntologyServiceMetaData ontologyServiceMetaData;

    public TabIndentedTextSource() {
        isSourceWorking = false;
        ontologyServiceMetaData = new OntologyServiceMetaData();
    }

    /**
	* loads terms read from a file.  in general this method will be 
	* what changes most in implementations of <code>OntologySource</code>
	*/
    public void load() {
        try {
            rootsForIndentationLevels = new Hashtable();
            String rootIdentifier = TermIdentifierUtility.createIdentifier(url, "root");
            overAllRootTerm = new TreeOntologyTerm(rootIdentifier, "root");
            URLConnection urlConnection = url.openConnection();
            InputStreamReader isr = new InputStreamReader(urlConnection.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(isr);
            String currentLine = bufferedReader.readLine();
            while (currentLine != null) {
                numberOfIndents = countIndentationLevels(currentLine);
                String phrase = currentLine.trim();
                String identifier = TermIdentifierUtility.createIdentifier(url, phrase);
                TreeOntologyTerm currentTerm = new TreeOntologyTerm(identifier, phrase);
                if (numberOfIndents == 0) {
                    overAllRootTerm.addRelatedTerm(currentTerm);
                } else {
                    TreeOntologyTerm levelTerm = (TreeOntologyTerm) rootsForIndentationLevels.get(new Integer(numberOfIndents - 1));
                    levelTerm.addRelatedTerm(currentTerm);
                    currentTerm.setParentTerm(levelTerm);
                }
                rootsForIndentationLevels.remove(new Integer(numberOfIndents));
                rootsForIndentationLevels.put(new Integer(numberOfIndents), currentTerm);
                currentLine = bufferedReader.readLine();
            }
            ArrayList children = overAllRootTerm.getRelatedTerms();
            int numberOfChildren = children.size();
            if (numberOfChildren == 1) {
                TreeOntologyTerm firstChild = (TreeOntologyTerm) children.get(0);
                rootTerm = firstChild;
            } else {
                rootTerm = overAllRootTerm;
            }
            isSourceWorking = true;
            String statusOKMessage = PedroResources.getMessage("ontology.statusOK", url.getFile());
            status = new StringBuffer();
            status.append(statusOKMessage);
        } catch (Exception err) {
            status = new StringBuffer();
            String statusErrorMessage = PedroResources.getMessage("ontology.statusError", err.toString());
            status.append(statusErrorMessage);
            isSourceWorking = false;
            err.printStackTrace(System.out);
        }
    }

    private int countIndentationLevels(String value) {
        int numberOfCharacters = value.length();
        int numberOfTabs = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '\t') {
                numberOfTabs++;
            } else {
                break;
            }
        }
        return numberOfTabs;
    }

    public OntologyTermProvenance getOntologyTermProvenance(PedroFormContext pedroFormContext, OntologyTerm ontologyTerm) {
        OntologyTermProvenance ontologyTermProvenance = new OntologyTermProvenance();
        ontologyTermProvenance.setOntologyServiceMetaData(ontologyServiceMetaData);
        OntologyTermMetaData ontologyTermMetaData = ontologyTermProvenance.getOntologyTermMetaData();
        String identifier = ontologyTerm.getIdentifier();
        ontologyTermMetaData.setIdentifier(identifier);
        String label = TermIdentifierUtility.extractTerm(identifier);
        ontologyTermMetaData.setLabel(label);
        try {
            URL webPage = new URL(identifier);
            ontologyTermMetaData.setWebPage(webPage);
        } catch (Exception err) {
            err.printStackTrace(System.out);
        }
        return ontologyTermProvenance;
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
                    ontologyServiceMetaData.setName(url.getFile());
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
}
