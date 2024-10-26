package pedro.soa.ontology.sources;

import pedro.util.ConfigurationURLUtility;
import pedro.soa.ontology.provenance.*;
import pedro.system.*;
import pedro.util.Parameter;
import pedro.util.SystemLog;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;

public class SingleColumnTextSource implements OntologySource {

    private URL url;

    private String[] vocabulary;

    private StringBuffer status;

    private String fileName;

    private boolean isSourceWorking;

    private boolean isRemoteSource;

    private Parameter[] parameters;

    private OntologyContext ontologyContext;

    private URL resourceDirectory;

    private OntologyServiceMetaData ontologyServiceMetaData;

    public SingleColumnTextSource() {
        status = new StringBuffer();
        isSourceWorking = false;
        ontologyServiceMetaData = new OntologyServiceMetaData();
    }

    private void load() {
        try {
            URLConnection urlConnection = url.openConnection();
            InputStreamReader isr = new InputStreamReader(urlConnection.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(isr);
            ArrayList results = new ArrayList();
            String currentLine = bufferedReader.readLine();
            while (currentLine != null) {
                results.add(currentLine.trim());
                currentLine = bufferedReader.readLine();
            }
            this.vocabulary = (String[]) results.toArray(new String[0]);
            Arrays.sort(vocabulary);
            String statusOKMessage = PedroResources.getMessage("ontology.statusOK", url.getFile());
            status = new StringBuffer();
            status.append(statusOKMessage);
            isSourceWorking = true;
        } catch (Exception err) {
            isSourceWorking = false;
            status = new StringBuffer();
            String statusErrorMessage = PedroResources.getMessage("ontology.statusError", err.toString());
            status.append(statusErrorMessage);
        }
    }

    public OntologyServiceMetaData getOntologyServiceMetaData() {
        return ontologyServiceMetaData;
    }

    public void setRemoteSource(boolean isRemoteSource) {
        this.isRemoteSource = isRemoteSource;
    }

    public void setResourceDirectory(URL resourceDirectory) {
        this.resourceDirectory = resourceDirectory;
    }

    public Parameter[] getParameters() {
        return parameters;
    }

    public void setParameters(Parameter[] parameters) {
        OntologyMetaDataUtility ontologyMetaDataUtility = new OntologyMetaDataUtility();
        ontologyMetaDataUtility.loadValuesFromParameters(parameters, ontologyServiceMetaData);
        try {
            for (int i = 0; i < parameters.length; i++) {
                String parameterName = parameters[i].getName();
                if (parameterName.equals("inputFile") == true) {
                    String fileName = parameters[i].getValue();
                    url = ConfigurationURLUtility.createResourceURL(resourceDirectory, fileName);
                    String ontologyName = PedroResources.getMessage("ontology.singleColumnTextSource.name", url.getFile());
                    ontologyServiceMetaData.setName(ontologyName);
                    load();
                    return;
                }
            }
        } catch (Exception err) {
            SystemLog.addError(err);
            return;
        }
    }

    public OntologyServiceMetaData getOntologyServiceMetaData(PedroFormContext pedroFormContext) {
        return ontologyServiceMetaData;
    }

    public OntologyTermProvenance getOntologyTermProvenance(PedroFormContext pedroFormContext, OntologyTerm ontologyTerm) {
        OntologyTermProvenance ontologyTermProvenance = new OntologyTermProvenance();
        ontologyTermProvenance.setOntologyServiceMetaData(ontologyServiceMetaData);
        OntologyTermMetaData ontologyTermMetaData = ontologyTermProvenance.getOntologyTermMetaData();
        String identifier = ontologyTerm.getIdentifier();
        try {
            ontologyTermMetaData.setIdentifier(identifier);
            String label = TermIdentifierUtility.extractTerm(identifier);
            ontologyTermMetaData.setLabel(label);
            URL webPage = new URL(identifier);
            ontologyTermMetaData.setWebPage(webPage);
        } catch (Exception err) {
        }
        return ontologyTermProvenance;
    }

    public boolean isWorking() {
        return isSourceWorking;
    }

    public String test() {
        return status.toString();
    }

    public OntologyTerm[] getTerms(PedroFormContext pedroFormContext) {
        ontologyContext = (OntologyContext) pedroFormContext.getProperty(PedroFormContext.ONTOLOGY_CONTEXT);
        OntologyTerm[] ontologyTerms = new OntologyTerm[vocabulary.length];
        for (int i = 0; i < vocabulary.length; i++) {
            String identifier = TermIdentifierUtility.createIdentifier(url, vocabulary[i]);
            ontologyTerms[i] = new OntologyTerm(identifier, vocabulary[i]);
        }
        return ontologyTerms;
    }

    public OntologyRelationshipType[] getSupportedOntologyRelationships(PedroFormContext pedroFormContext) {
        OntologyRelationshipType[] types = new OntologyRelationshipType[0];
        return types;
    }

    public OntologyTerm[] getRelatedTerms(PedroFormContext pedroFormContext, OntologyRelationshipType relationshipType, OntologyTerm ontologyTerm) {
        OntologyTerm[] emptyResultList = new OntologyTerm[0];
        return emptyResultList;
    }

    public boolean containsTerm(PedroFormContext pedroFormContext, OntologyTerm ontologyTerm) {
        String identifier = ontologyTerm.getIdentifier();
        String term = TermIdentifierUtility.extractTerm(identifier);
        for (int i = 0; i < vocabulary.length; i++) {
            if (vocabulary[i].equals(term) == true) {
                return true;
            }
        }
        return false;
    }

    public OntologySource getSubOntologySource(PedroFormContext pedroFormContext, Parameter[] parameterList) {
        return this;
    }
}
