package myown.ontology;

import java.net.URL;
import java.net.URLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import pedro.ontology.OntologySource;
import pedro.ontology.OntologyTerm;

public class RemoteWordListScraper implements OntologySource {

    private String name;

    private String description;

    private OntologyTerm[] vocabulary;

    private boolean isSourceWorking;

    private String fileName;

    private StringBuffer status;

    private boolean isRemoteSource;

    public RemoteWordListScraper() {
        name = "Remote Word List for Common Names";
        StringBuffer buffer = new StringBuffer();
        buffer.append("Reads a word list from a web page ");
        buffer.append("being served on a remote server.");
        description = buffer.toString();
    }

    private void load() {
        URL url = null;
        BufferedReader reader = null;
        try {
            url = new URL(fileName);
            URLConnection urlConnection = url.openConnection();
            InputStreamReader isr = new InputStreamReader(urlConnection.getInputStream());
            reader = new BufferedReader(isr);
        } catch (Exception err) {
            isSourceWorking = false;
            status = new StringBuffer();
            status.append("ERROR: Can't connect to URL ");
            status.append(fileName);
            return;
        }
        try {
            parseFile(reader);
            status = new StringBuffer();
            status.append("URL ");
            status.append(url.toString());
            status.append(" is accessible and ");
            status.append(" the page contents can be ");
            status.append("interpretted correctly.");
            isSourceWorking = true;
        } catch (Exception err) {
            status = new StringBuffer();
            status.append("URL is valid but data could not be interpretted properly");
            isSourceWorking = false;
        }
    }

    private void parseFile(BufferedReader reader) throws IOException {
        ArrayList results = new ArrayList();
        String currentLine = reader.readLine();
        boolean acceptWords = false;
        while (currentLine != null) {
            String word = currentLine.trim();
            if (word.startsWith("<ul>") == true) {
                acceptWords = true;
            } else if (acceptWords == true) {
                if (word.startsWith("</ul>") == true) {
                    acceptWords = false;
                } else {
                    OntologyTerm term = new OntologyTerm(word);
                    results.add(term);
                }
            }
            currentLine = reader.readLine();
        }
        vocabulary = (OntologyTerm[]) results.toArray(new OntologyTerm[0]);
        Arrays.sort(vocabulary);
    }

    public void setRemoteSource(boolean isRemoteSource) {
        this.isRemoteSource = isRemoteSource;
    }

    public boolean isRemoteSource() {
        return isRemoteSource;
    }

    public boolean isWorking() {
        load();
        return isSourceWorking;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String test() {
        return status.toString();
    }

    public OntologyTerm[] getTerms() {
        return vocabulary;
    }

    public OntologyTerm[] getRelatedTerms(OntologyTerm ontologyTerm) {
        if (containsTerm(ontologyTerm) == true) {
            OntologyTerm[] relatedTerms = new OntologyTerm[1];
            relatedTerms[0] = ontologyTerm;
            return relatedTerms;
        } else {
            return new OntologyTerm[0];
        }
    }

    public boolean containsTerm(OntologyTerm ontologyTerm) {
        String term = ontologyTerm.getTerm();
        for (int i = 0; i < vocabulary.length; i++) {
            if (vocabulary[i].getTerm().equals(term) == true) {
                return true;
            }
        }
        return false;
    }

    public boolean containsTerm(String term) {
        for (int i = 0; i < vocabulary.length; i++) {
            if (vocabulary[i].equals(term) == true) {
                return true;
            }
        }
        return false;
    }

    public void setFileName(String _fileName) {
        this.fileName = _fileName;
        load();
    }

    public OntologySource getView(String parameters) {
        return this;
    }
}
