package org.argouml.xml.argo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.argouml.application.api.Argo;
import org.argouml.kernel.Project;
import org.argouml.xml.SAXParserBase;
import org.argouml.xml.XMLElement;
import org.xml.sax.SAXException;

public class ArgoParser extends SAXParserBase {

    public static ArgoParser SINGLETON = new ArgoParser();

    protected Project _proj = null;

    private ArgoTokenTable _tokens = new ArgoTokenTable();

    private boolean _addMembers = true;

    private URL _url = null;

    private boolean lastLoadStatus = true;

    private String lastLoadMessage = null;

    protected ArgoParser() {
        super();
    }

    public synchronized void readProject(URL url) throws IOException {
        readProject(url, true);
    }

    public synchronized void readProject(URL url, boolean addMembers) throws IOException {
        _url = url;
        try {
            readProject(url.openStream(), addMembers);
        } catch (IOException e) {
            Argo.log.info("Couldn't open InputStream in ArgoParser.load(" + url + ") " + e);
            e.printStackTrace();
            lastLoadMessage = e.toString();
            throw e;
        }
    }

    public void setURL(URL url) {
        _url = url;
    }

    public synchronized void readProject(InputStream is, boolean addMembers) {
        lastLoadStatus = true;
        lastLoadMessage = "OK";
        _addMembers = addMembers;
        if ((_url == null) && _addMembers) {
            Argo.log.info("URL not set! Won't be able to add members! Aborting...");
            lastLoadMessage = "URL not set!";
            return;
        }
        try {
            Argo.log.info("=======================================");
            Argo.log.info("== READING PROJECT " + _url);
            _proj = new Project(_url);
            parse(is);
        } catch (SAXException saxEx) {
            lastLoadStatus = false;
            Argo.log.info("Exception reading project================");
            Exception ex = saxEx.getException();
            if (ex == null) {
                lastLoadMessage = saxEx.toString();
                saxEx.printStackTrace();
            } else {
                lastLoadMessage = saxEx.toString();
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            Argo.log.info("Exception reading project================");
            ex.printStackTrace();
            lastLoadMessage = ex.toString();
        }
    }

    public Project getProject() {
        return _proj;
    }

    public void handleStartElement(XMLElement e) {
        if (_dbg) System.out.println("NOTE: ArgoParser handleStartTag:" + e.getName());
        try {
            switch(_tokens.toToken(e.getName(), true)) {
                case ArgoTokenTable.TOKEN_argo:
                    handleArgo(e);
                    break;
                case ArgoTokenTable.TOKEN_documentation:
                    handleDocumentation(e);
                    break;
                default:
                    if (_dbg) System.out.println("WARNING: unknown tag:" + e.getName());
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void handleEndElement(XMLElement e) {
        if (_dbg) System.out.println("NOTE: ArgoParser handleEndTag:" + e.getName() + ".");
        try {
            switch(_tokens.toToken(e.getName(), false)) {
                case ArgoTokenTable.TOKEN_authorname:
                    handleAuthorname(e);
                    break;
                case ArgoTokenTable.TOKEN_version:
                    handleVersion(e);
                    break;
                case ArgoTokenTable.TOKEN_description:
                    handleDescription(e);
                    break;
                case ArgoTokenTable.TOKEN_searchpath:
                    handleSearchpath(e);
                    break;
                case ArgoTokenTable.TOKEN_member:
                    handleMember(e);
                    break;
                case ArgoTokenTable.TOKEN_historyfile:
                    handleHistoryfile(e);
                    break;
                default:
                    if (_dbg) System.out.println("WARNING: unknown end tag:" + e.getName());
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void handleArgo(XMLElement e) {
    }

    protected void handleDocumentation(XMLElement e) {
    }

    protected void handleAuthorname(XMLElement e) {
        String authorname = e.getText().trim();
        _proj._authorname = authorname;
    }

    protected void handleVersion(XMLElement e) {
        String version = e.getText().trim();
        _proj._version = version;
    }

    protected void handleDescription(XMLElement e) {
        String description = e.getText().trim();
        _proj._description = description;
    }

    protected void handleSearchpath(XMLElement e) {
        String searchpath = e.getAttribute("href").trim();
        _proj.addSearchPath(searchpath);
    }

    protected void handleMember(XMLElement e) {
        if (_addMembers) {
            String name = e.getAttribute("name").trim();
            String type = e.getAttribute("type").trim();
            _proj.addMember(name, type);
        }
    }

    protected void handleHistoryfile(XMLElement e) {
        if (e.getAttribute("name") == null) return;
        String historyfile = e.getAttribute("name").trim();
        _proj._historyFile = historyfile;
    }

    /** return the status of the last load attempt.
        Used for junit tests.
     */
    public boolean getLastLoadStatus() {
        return lastLoadStatus;
    }

    /** set the status of the last load attempt. 
        Used for junit tests.
     */
    public void setLastLoadStatus(boolean status) {
        lastLoadStatus = status;
    }

    /** get the last message which caused loading to fail. 
        Used for junit tests.
     */
    public String getLastLoadMessage() {
        return lastLoadMessage;
    }

    /** set the last load message.
        Used for junit tests.
    */
    public void setLastLoadMessage(String msg) {
        lastLoadMessage = msg;
    }
}
