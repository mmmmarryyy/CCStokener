package client;

import changeServerPackage.ApplyChangesServlet;
import changeServerPackage.ChangeCapsule;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyChange;
import fileManagerPackage.TagReader;
import fileManagerPackage.TagWriter;
import fileManagerPackage.OntologyFileManager;

/**
 * Created by IntelliJ IDEA.
 * User: candidasa
 * Date: Feb 11, 2008
 * Time: 10:34:19 AM
 * Client that uses the connector client to perform a series of operations
 */
public class OperationsClient {

    public static final String TEMPDIR = "temp/";

    private ConnectorClient server;

    protected String serverHostname;

    protected String username;

    /** creates a new client for communicating with a change server. If the username is null the IP-address/hostname is used */
    public OperationsClient(String serverHostname, String username) throws MalformedURLException, UnknownHostException {
        this.serverHostname = serverHostname;
        server = new ConnectorClient(serverHostname);
        if (username == null) this.username = InetAddress.getLocalHost().getHostName(); else this.username = username;
    }

    /** returns the server host this operationsClient connects when trying to issue a command */
    public String getServerHostname() {
        return serverHostname;
    }

    /** returns the username this operationsClient is using */
    public String getUsername() {
        return username;
    }

    /** queries the server for the latest change sequence number for this given ontology */
    public Long getLatestVersionNumber(OWLOntology ontology) throws IOException {
        ChangeCapsule commandCapsule = new ChangeCapsule();
        commandCapsule.setOntologyURI(ontology.getURI().toString());
        String response = server.issueCommandToServer(ApplyChangesServlet.QUERY, commandCapsule);
        Long responseLong = null;
        if (response != null) {
            if (!response.startsWith("Error")) {
                responseLong = new Long(response);
            } else {
                System.err.println(response);
            }
        } else {
            System.err.println("Error: null response");
        }
        return responseLong;
    }

    /** fetches on specific changeCapsule object from the server based upon its sequence number (sequence numbers start from one, not zero)*/
    public ChangeCapsule getSpecificChange(OWLOntology ontology, Long sequenceNumber) throws IOException {
        ChangeCapsule commandCapsule = new ChangeCapsule();
        commandCapsule.setOntologyURI(ontology.getURI().toString());
        commandCapsule.setSequence(sequenceNumber);
        String jsonResponse = server.issueCommandToServer(ApplyChangesServlet.UPDATE, commandCapsule);
        ChangeCapsule responseCapsule = null;
        if (jsonResponse != null) {
            if (!jsonResponse.startsWith("Error")) {
                responseCapsule = new ChangeCapsule(jsonResponse);
            } else {
                System.err.println(jsonResponse);
            }
        } else {
            System.err.println("Error: null response");
        }
        return responseCapsule;
    }

    /** downloads all changes that have occured between the current version of the ontology
     * and the newest version on the server. This method then proceeds to
     * integrate those changes into the current ontology model */
    public void updateOntology(OWLOntology ontology, Long currentSequenceNumber) {
    }

    /** sends a series of change to the server and returns the response */
    public String commitChangestoServer(OWLOntology ontology, Long localSequenceNumber, String summary, List<OWLOntologyChange> changes) throws IOException {
        ChangeCapsule changeCapsule = new ChangeCapsule(changes);
        changeCapsule.setUsername(username);
        changeCapsule.setOntologyURI(ontology.getURI().toString());
        changeCapsule.setSequence(localSequenceNumber);
        changeCapsule.setSummary(summary);
        String response = server.issueCommandToServer(ApplyChangesServlet.COMMIT, changeCapsule);
        if (response == null) {
            System.err.println("Error: null response");
        }
        return response;
    }

    /** download a the latest tag (gzipped) from the server and store it into a temporary local file */
    public File downloadLatestTag(OWLOntology ontology) throws UnsupportedEncodingException, IOException {
        Long latestVersion = getLatestVersionNumber(ontology);
        return downloadSpecificTag(ontology, latestVersion);
    }

    /** downloads a tag with a specific version number from the server (gzipped),
     * the ontology is stored in as a tempoary file (deleted when the virtual machine shuts down) */
    public File downloadSpecificTag(OWLOntology ontology, Long versionNumber) throws IOException {
        File localFile = File.createTempFile(TagWriter.TAGPREFIX + versionNumber, TagWriter.TAGEXTENSION, new File(TEMPDIR + OntologyFileManager.shortenURI(ontology.getURI().toASCIIString())));
        OutputStream out = null;
        URLConnection conn;
        GZIPInputStream zipin = null;
        try {
            URL url = new URL(server.getServerBase() + "/" + OntologyFileManager.shortenURI(ontology.getURI().toASCIIString()) + "/" + OntologyFileManager.TAGSFOLDER + "/" + TagWriter.TAGPREFIX + versionNumber + TagWriter.TAGEXTENSION);
            out = new BufferedOutputStream(new FileOutputStream(localFile));
            conn = url.openConnection();
            zipin = new GZIPInputStream(conn.getInputStream());
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = zipin.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
            }
            System.out.println(localFile.getName() + "\t" + numWritten);
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            try {
                if (zipin != null) {
                    zipin.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
                System.out.println(ioe.toString());
            }
        }
        return localFile;
    }
}
