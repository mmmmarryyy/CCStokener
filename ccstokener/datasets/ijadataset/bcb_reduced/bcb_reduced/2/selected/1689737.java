package test;

import fileManagerPackage.ChangeReader;
import fileManagerPackage.TagWriter;
import fileManagerPackage.TagReader;
import java.io.*;
import java.util.ArrayList;
import java.util.Set;
import java.net.*;
import changeServerPackage.ChangeCapsule;
import org.semanticweb.owl.model.*;
import org.semanticweb.owl.vocab.OWLRDFVocabulary;

/**
 * Created by IntelliJ IDEA.
 * User: candidasa
 * Date: Feb 4, 2008
 * Time: 1:30:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestDownloaderClient {

    public TestDownloaderClient() {
    }

    /** returns whether or not the given ontology contains all the changes that are on the server. That is,
     * if there have been any changes published by other clients.
     * @param ontology which should be tested to test
     * @return true=is current, false=is not current and needs to be updated
     */
    public boolean bringOntologyUpToDate(OWLOntology ontology) throws IOException {
        Long clientVersion = getOntologySequenceNumber(ontology);
        Long serverVersion = queryServer(ontology);
        ArrayList<ChangeCapsule> listOfChanges = new ArrayList<ChangeCapsule>();
        while (clientVersion < serverVersion) {
            clientVersion++;
            fetchServer(ontology, clientVersion);
        }
        return false;
    }

    /** queries the server for the latest change sequence number for this given ontology */
    private Long queryServer(OWLOntology ontologyURI) throws IOException {
        String requestString = "http://" + InetAddress.getLocalHost().getHostName() + ":8080/ChangeServer";
        requestString += "?query=" + URLEncoder.encode(ontologyURI.getURI().toString(), "UTF-8");
        URL url = new URL(requestString);
        BufferedReader input = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuffer returned = new StringBuffer();
        String str;
        while (null != ((str = input.readLine()))) {
            returned.append(str);
        }
        input.close();
        return new Long(returned.toString());
    }

    /** read the change sequence number of an ontology */
    protected Long getOntologySequenceNumber(OWLOntology ontology) {
        Long number = null;
        Set<OWLOntologyAnnotationAxiom> allAnnotations = ontology.getOntologyAnnotationAxioms();
        for (OWLOntologyAnnotationAxiom annotation : allAnnotations) {
            if (annotation.getAnnotation().getAnnotationURI().compareTo(OWLRDFVocabulary.OWL_VERSION_INFO.getURI()) == 0) {
                if (annotation.getAnnotation().getAnnotationValue() instanceof OWLConstant) {
                    String literal = ((OWLConstant) annotation.getAnnotation().getAnnotationValue()).getLiteral();
                    if (literal.startsWith(TagReader.CHANGEAXIOMPREFIX)) {
                        number = new Long(literal.substring(TagReader.CHANGEAXIOMPREFIX.length()));
                    }
                }
            }
        }
        return number;
    }

    /** fetches on specific changeCapsule object from the server */
    private ChangeCapsule fetchServer(OWLOntology ontologyURI, Long sequenceNumber) throws IOException {
        String requestString = "http://" + InetAddress.getLocalHost().getHostName() + ":8080/ChangeServer";
        requestString += "?fetch=" + URLEncoder.encode(ontologyURI.getURI().toString(), "UTF-8");
        requestString += "&number" + sequenceNumber;
        URL url = new URL(requestString);
        BufferedReader input = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuffer returned = new StringBuffer();
        String str;
        while (null != ((str = input.readLine()))) {
            returned.append(str);
        }
        input.close();
        ChangeCapsule cp = new ChangeCapsule(returned.toString());
        return cp;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java TestDownloaderClient <ontology-base>");
            System.exit(1);
        }
        TagWriter writer = null;
        ArrayList<ChangeCapsule> changeCaps = null;
        try {
            writer = new TagWriter(new File(args[0]));
            ChangeReader cr = new ChangeReader(new File(args[1]));
            changeCaps = new ArrayList<ChangeCapsule>();
            for (int i = 1; i < args.length; i++) {
                ChangeCapsule cap = cr.getChange(new File(args[i]));
                changeCaps.add(cap);
            }
            writer.applyChanges(changeCaps);
            writer.saveNewTag();
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        } catch (OWLOntologyChangeException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
