package org.scidac.sam.eln.serverproxy;

import eln.login.LoginGUIPanel;
import eln.nob.*;
import eln.server.*;
import eln.server.event.AdminActionEvent;
import eln.util.Base64;
import eln.util.InterestDialog;
import eln.util.MimeTypes;
import eln.util.PageDisplayer;
import eln.util.QuotedPrintable;
import eln.util.StatusChangeListener;
import emsl.JavaShare.EmslProperties;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import javax.swing.JDialog;
import javax.swing.event.TreeSelectionEvent;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.URIException;
import org.apache.webdav.lib.Property;
import org.apache.webdav.lib.PropertyName;
import org.apache.webdav.lib.ResponseEntity;
import org.apache.webdav.lib.methods.DepthSupport;
import org.jdom.input.*;
import org.scidac.sam.eln.nob.WebdavNOb;
import org.scidac.sam.eln.serverproxy.SAMCredentials;

/**
 *  Description of the Class
 *
 * @author     Jim Myers
 * @created    March 5, 2003
 */
public class SAMNotebookServerProxy extends NotebookServerProxy {

    /**
   *  Description of the Field
   */
    public static final String ELNNS = "http://purl.oclc.org/NET/SAM/eln";

    /**
   *  Description of the Field
   */
    protected static final int kServerResponseTimeout = 10000;

    private static final String kFrmUserName = "username";

    private static final String kFrmPassword = "password";

    private static final String kFrmeMail = "email";

    private static final String kFrmNUPass1 = "pass1";

    private static final String kFrmNUPass2 = "pass2";

    private static final String kFrmAddAsAdmin = "addAsAdmin";

    private static final String kFrmChangedPassword = "changedpassword";

    private static final int kMinAllowedLevel = NObList.kNoteLevel + 1;

    /**
   *  CLASS DATA
   */
    protected SAMCredentials mCredentials;

    /**
   *  Description of the Field
   */
    protected SAMELNConnection mELNCon;

    /**
   *  Description of the Field
   */
    protected int mRole = NO_ROLE;

    /**
   *  Description of the Field
   */
    protected String mRootNode = null;

    /**
   *  Description of the Field
   */
    protected boolean mNotify = false;

    /**
   *  Description of the Field
   */
    protected URL mWebProxyURL = null;

    /**
   *  Description of the Field
   */
    protected String mBoundary = "";

    /**
   *  Description of the Field
   */
    protected Administration mAdmin;

    protected final String mProtocol = "http";

    /**
   *  Method Description: Standard constructor.
   *
   * @param  gen  Description of the Parameter
   */
    public SAMNotebookServerProxy(GenericConnection gen) {
        super(gen);
        setupProtocol();
        mELNCon = new SAMELNConnection(gen);
        assert (mServerURL.getProtocol().toLowerCase().equals(mProtocol));
        SAMCredentials c = (SAMCredentials) getCredentialStructure();
        String authinfo = mELNCon.getAuthInfo();
        c.updateCredentialsFromCookies(authinfo);
        if (c.getNBUserName() != null) {
            c.setGrpUserName(c.getNBUserName());
            c.setGrpPassword(c.getNBPassword());
            mServerStatus = LOGGED_IN;
            mRole = NORMAL_ROLE;
            try {
                if (isAdmin()) {
                    mRole = ADMIN_ROLE;
                }
            } catch (NotebookServerException nbse) {
                System.err.println("Could not determine Admin status: " + nbse);
            }
        }
        mCredentials = c;
        mRootNode = mELNCon.getRootNode();
        mAdmin = new SAMAdministration(this);
        WebdavNOb.setDefaultDepth(WebdavNOb.ELNPROPS);
        mELNCon.getContext();
    }

    /**
   *  Sets the credentialStructure attribute of the SAMNotebookServerProxy
   *  object
   *
   * @param  theCreds                     The new credentialStructure value
   * @exception  NotebookServerException  Description of the Exception
   */
    public void setCredentialStructure(Credentials theCreds) throws NotebookServerException {
        try {
            mCredentials = (SAMCredentials) theCreds;
        } catch (ClassCastException cce) {
            throw new NotebookLoginException("Incorrect credential type");
        }
    }

    /**
   *  Sets the webProxy attribute of the SAMNotebookServerProxy object
   *
   * @param  webProxy  The new webProxy value
   */
    public void setWebProxy(URL webProxy) {
        mWebProxyURL = webProxy;
    }

    /**
   *  Returns the webProxy attribute of the SAMNotebookServerProxy object
   *
   * @return  webProxy  The new webProxy value
   */
    public URL getWebProxy() {
        return mWebProxyURL;
    }

    /**
   *  Sets interests list on server (See notify.pl for the format of interest
   *  lines)
   *
   * @param  theInterests                 - Vector of vectors matching output of
   *      getInterestList()
   * @exception  NotebookServerException  Description of the Exception
   */
    public void setInterestList(Vector theInterests) throws NotebookServerException {
        String result = null;
        String id;
        boolean bHasEmail;
        URL urlWithCommand = null;
        try {
            urlWithCommand = makeCommandURL("set_interests");
            FormSubmitter theSubmitter = new FormSubmitter(urlWithCommand, mCredentials, mWebProxyURL);
            StringBuffer interestString = new StringBuffer();
            Enumeration elements = theInterests.elements();
            interestString.append("<interest_changes>");
            while (elements.hasMoreElements()) {
                Vector v = (Vector) elements.nextElement();
                id = (String) v.elementAt(0);
                bHasEmail = !((String) v.elementAt(1)).equals("");
                interestString.append("<id ");
                if (!bHasEmail) {
                    interestString.append("function=" + (String) v.elementAt(2));
                } else {
                    interestString.append("function=modified");
                }
                interestString.append(" name=" + id);
                if (!bHasEmail) {
                    interestString.append("/>");
                }
                if (bHasEmail) {
                    interestString.append("><email function=" + (String) v.elementAt(2) + " name=" + (String) v.elementAt(1) + "/>");
                    interestString.append("</id>");
                }
            }
            interestString.append("</interest_changes>");
            theSubmitter.addFormData("interests", interestString.toString());
            theSubmitter.submit();
            Reader resultReader = theSubmitter.getResponseReader();
            ELNHttpResponseParser ndp = new ELNHttpResponseParser(new LineNumberReader(resultReader));
            result = ndp.getELNResponse();
            if (result == null) {
                throw new NotebookServerException("Error setting interests file, no result received.", urlWithCommand, result);
            }
            if (!result.startsWith("ELN_OK")) {
                throw new NotebookServerException(result);
            }
        } catch (IOException io) {
            throw new NotebookServerException("Error setting interests file: " + io, urlWithCommand, result);
        }
    }

    /**
   *  Gets the notebookUID attribute of the SAMNotebookServerProxy object
   *
   * @return    The notebookUID value
   */
    public String getNotebookUID() {
        return mELNCon.getNotebookUID();
    }

    /**
   *  Gets the credentialStructure attribute of the SAMNotebookServerProxy
   *  object
   *
   * @return    The credentialStructure value
   */
    public Credentials getCredentialStructure() {
        if (mCredentials == null) {
            mCredentials = new SAMCredentials();
        }
        return mCredentials;
    }

    /**
   *  Gets the serverURL attribute of the SAMNotebookServerProxy object
   *
   * @return    The serverURL value
   */
    public URL getServerURL() {
        return mServerURL;
    }

    /**
   *  Gets the pageDisplayer attribute of the SAMNotebookServerProxy object
   *
   * @return    The pageDisplayer value
   */
    public PageDisplayer getPageDisplayer() {
        return mELNCon;
    }

    /**
   *  Checks to see if current user is an admin. Local admin flag is set
   *  appropriately. Assumes that user is already logged in (always true in
   *  https)
   *
   * @return                              The userRole value
   * @exception  NotebookServerException  Description of the Exception
   */
    public int getUserRole() throws NotebookServerException {
        return mRole;
    }

    /**
   *  Gets the preferredBackgroundColor attribute of the SAMNotebookServerProxy
   *  object
   *
   * @return    The preferredBackgroundColor value
   */
    public Color getPreferredBackgroundColor() {
        return mELNCon.getBackgroundColor();
    }

    /**
   *  Checks with the server to see if current user is an admin. Local admin
   *  flag is set appropriately. Assumes that user is already logged in (always
   *  true in https)
   *
   * @return                              The notificationEnabled value
   * @exception  NotebookServerException  Description of the Exception
   */
    public boolean isNotificationEnabled() throws NotebookServerException {
        URL urlWithCommand = makeCommandURL("check_notify");
        GetSubmitter theSubmitter = new GetSubmitter(urlWithCommand, mCredentials, mWebProxyURL);
        Reader resultReader = null;
        theSubmitter.submit();
        resultReader = theSubmitter.getResponseReader();
        ELNHttpResponseParser ndp = new ELNHttpResponseParser(new LineNumberReader(resultReader));
        String result = ndp.getELNResponse();
        if (result.equals("1")) {
            mNotify = true;
        }
        return (mNotify);
    }

    /**
   *  Gets interests list fromserver (See notify.pl for the format of interest
   *  lines)
   *
   * @return                              a Vector of Vectors, each containing
   *      two string elements: the objectID string and a string containing the
   *      list of email addresses
   * @exception  NotebookServerException  Description of the Exception
   */
    public Vector getInterestList() throws NotebookServerException {
        Vector interests = new Vector();
        String result = null;
        URL urlWithCommand = null;
        try {
            urlWithCommand = makeCommandURL("get_interests");
            GetSubmitter theSubmitter = new GetSubmitter(urlWithCommand, mCredentials, mWebProxyURL);
            Reader resultReader = null;
            theSubmitter.submit();
            resultReader = theSubmitter.getResponseReader();
            ELNHttpResponseParser ndp = new ELNHttpResponseParser(new LineNumberReader(resultReader));
            result = ndp.getELNResponse();
            if (result != null) {
                StringTokenizer st = new StringTokenizer(result, ":");
                while (st.hasMoreTokens()) {
                    String aLine = st.nextToken();
                    int firstSpace = aLine.indexOf(' ');
                    Vector v = new Vector();
                    if (firstSpace != -1) {
                        v.addElement(aLine.substring(0, firstSpace));
                        v.addElement(aLine.substring(firstSpace + 1));
                        v.addElement("");
                    } else {
                        v.addElement(aLine);
                        v.addElement("");
                    }
                    interests.addElement(v);
                }
            }
        } catch (IOException e) {
            throw new NotebookServerException("Error getting interests file: " + e, urlWithCommand, result);
        }
        return (interests);
    }

    /**
   *  Retrieves current list of user names from the server
   *
   * @return                              The userList value
   * @exception  NotebookServerException  Description of the Exception
   */
    public String[] getUserList() throws NotebookServerException {
        TreeSet userList = new TreeSet();
        URL urlWithCommand = makeCommandURL("listELNUsers");
        GetSubmitter theSubmitter = new GetSubmitter(urlWithCommand, mCredentials, mWebProxyURL);
        Reader resultReader = null;
        theSubmitter.submit();
        resultReader = theSubmitter.getResponseReader();
        ELNHttpResponseParser ndp = new ELNHttpResponseParser(new LineNumberReader(resultReader));
        String result = ndp.getELNResponse();
        String httpResponse = ndp.getHTTPcode();
        String httpCode = null;
        if (httpResponse != null) {
            StringTokenizer parser = new StringTokenizer(httpResponse);
            String httpVersion = parser.nextToken();
            httpCode = parser.nextToken();
        }
        if (httpCode.equals("401")) {
            throw new NotebookAccessDeniedException("Access Denied - Check Password", urlWithCommand, httpCode);
        }
        if (!(result.equals(""))) {
            StringTokenizer listTokens = new StringTokenizer(result, ":");
            while (listTokens.hasMoreTokens()) {
                userList.add(listTokens.nextToken());
            }
        } else {
            throw new NotebookServerException("No user names returned");
        }
        String[] retval = new String[userList.size()];
        return ((String[]) (userList.toArray(retval)));
    }

    /**
   *  Method Description: Generic function for requesting part of a notebook
   *
   * @param  senddata                     should full NObs (with data) be sent?
   *      If false, headers will be sent
   * @param  depth                        the number of levels of nested
   *      NObLists to return (if they exist). NObLists at the lowest level will
   *      be returned as headers (no vector of NObs inside).
   * @param  requestedNObNode             Description of the Parameter
   * @return                              The nObs value
   * @exception  NotebookServerException  thrown if an error occurs while
   *      getting the requested NObNodes probably caused by network errors or
   *      server misconfiguration
   * @paramrootNObNode                    the top level item to return or a
   *      header for that item
   */
    public NObNode getNObs(boolean senddata, NObNode requestedNObNode, int depth) throws NotebookServerException {
        String uri = null;
        WebdavNOb currentNOb = null;
        try {
            NObNode returnNObNode = null;
            NObNode startNOb = null;
            if (requestedNObNode == null) {
                uri = mRootNode;
            } else {
                uri = requestedNObNode.getOriginalOID();
            }
            boolean isRealRoot = uri.equals(mRootNode);
            String pathPart = mServerURL.getPath();
            int port = mServerURL.getPort();
            if (port == -1) {
                port = mServerURL.getDefaultPort();
            }
            String newurl = (new URL(mServerURL.getProtocol(), mServerURL.getHost(), port, uri)).toExternalForm();
            HttpURL httpURL = createURL(newurl);
            String samToken = mCredentials.getNBAuthToken();
            if (samToken == null || samToken.length() == 0) {
                httpURL.setEscapedUserinfo(mCredentials.getGrpUserName(), mCredentials.getGrpPassword());
            }
            currentNOb = new WebdavNOb(httpURL, mRootNode, isRealRoot, mCredentials);
            currentNOb.setContext(mELNCon.getContext());
            if (requestedNObNode instanceof WebdavNOb) {
                currentNOb.setShowDeleted(((WebdavNOb) requestedNObNode).getShowDeleted());
            }
            currentNOb.setProperties(WebdavNOb.ELNPROPS, depth, senddata);
        } catch (NotebookAccessDeniedException nade) {
            throw nade;
        } catch (Exception e) {
            throw new NotebookServerException(e.getMessage());
        }
        return (NObNode) currentNOb;
    }

    /**
   * @param  aCommand  Description of the Parameter
   * @return           the internal form of an OID (remove the
   *      "http://...note.cgi/command" portion)
   */
    public String getOIDFromCommand(String aCommand) {
        String newOID = null;
        if (aCommand != null) {
            if (aCommand.toLowerCase().startsWith("http") == true) {
                int commandStart = aCommand.indexOf("note.cgi/");
                if (commandStart >= 0) {
                    int commandEnd = aCommand.indexOf("/", commandStart + "note.cgi/".length());
                    if (commandEnd >= commandStart) {
                        newOID = aCommand.substring(commandEnd);
                    }
                }
            }
        }
        return newOID;
    }

    /**
   *  Parses class file names from an HTML directory listing. Filenames are
   *  assumed to be listed as '... ="thename.class"> ...' e.g. '...
   *  NAME="thename.class"> ...' or '... HREF="thename.class"> ...' somewhere
   *  within the text
   *
   * @return                              The editorClassNames value
   * @exception  NotebookServerException  Description of the Exception
   */
    public Vector getEditorClassNames() throws NotebookServerException {
        Vector names = new Vector();
        URL relURL = null;
        try {
            relURL = new URL(mELNCon.getCodebase() + "eln/editors/");
        } catch (MalformedURLException mue) {
            System.err.println("Error getting editor URL" + mue);
        }
        GetSubmitter FormMgr = new GetSubmitter(relURL, mCredentials, mWebProxyURL);
        Reader result = null;
        FormMgr.submit();
        result = FormMgr.getResponseReader();
        LineNumberReader lineResult = new LineNumberReader(result);
        StringBuffer tmpBuf = new StringBuffer();
        String line;
        try {
            while ((line = lineResult.readLine()) != null) {
                tmpBuf.append(line).append("\r\n");
            }
        } catch (IOException io) {
            io.printStackTrace();
            throw new NotebookServerException("Unable to read response:  " + io);
        }
        String response = tmpBuf.toString();
        int start = response.indexOf("=\"");
        while (start >= 0) {
            response = response.substring(start + "=\"".length());
            int end = response.indexOf(">");
            int fileEnd = response.indexOf(".class");
            if (end >= 0) {
                if ((fileEnd >= 0) && (fileEnd < end)) {
                    int endOfPath = (response.substring(0, fileEnd)).lastIndexOf('/');
                    String newEditor = response.substring(endOfPath + 1, fileEnd);
                    boolean isValidEditor = true;
                    if (!(Character.isJavaIdentifierStart(newEditor.charAt(0)))) {
                        isValidEditor = false;
                    }
                    for (int i = 1; i < newEditor.length(); i++) {
                        if (!(Character.isJavaIdentifierPart(newEditor.charAt(i)))) {
                            isValidEditor = false;
                        }
                    }
                    if (isValidEditor == true) {
                        newEditor = "eln.editors." + newEditor;
                        Enumeration editors = names.elements();
                        boolean inListAlready = false;
                        while (editors.hasMoreElements() && !inListAlready) {
                            if (((String) editors.nextElement()).equals(newEditor)) {
                                inListAlready = true;
                            }
                        }
                        if (!inListAlready) {
                            names.addElement(newEditor);
                        }
                    } else {
                        newEditor = newEditor;
                        Enumeration editors = names.elements();
                        boolean inListAlready = false;
                        while (editors.hasMoreElements() && !inListAlready) {
                            if (((String) editors.nextElement()).equals(newEditor)) {
                                inListAlready = true;
                            }
                        }
                        if (!inListAlready) {
                            names.addElement(newEditor);
                        }
                    }
                }
            }
            start = response.indexOf("=\"");
        }
        return names;
    }

    /**
   *  Gets the minimumContentNodeLevel attribute of the SAMNotebookServerProxy
   *  object
   *
   * @return    The minimumContentNodeLevel value
   */
    public int getMinimumContentNodeLevel() {
        return kMinAllowedLevel;
    }

    /**
   *  isAdmin checks with the server to verify that current user is an admin
   *
   * @return                              The admin value
   * @exception  NotebookServerException  Description of the Exception
   */
    public boolean isAdmin() throws NotebookServerException {
        boolean isAnAdmin = false;
        try {
            URL urlWithCommand = makeCommandURL("isELNAdmin");
            GetSubmitter theSubmitter = new GetSubmitter(urlWithCommand, mCredentials, mWebProxyURL);
            Reader resultReader = null;
            theSubmitter.submit();
            resultReader = theSubmitter.getResponseReader();
            ELNHttpResponseParser ndp = new ELNHttpResponseParser(new LineNumberReader(resultReader));
            String result = ndp.getELNResponse();
            StringTokenizer parser = new StringTokenizer(result, ",");
            String status = parser.nextToken();
            if (status.equals("ISADMIN_TRUE")) {
                isAnAdmin = true;
            } else if (status.equals("ISADMIN_FALSE")) {
            } else if (status.equals("ISADMIN_ERROR")) {
                String msg = parser.nextToken();
                throw new NotebookServerException(msg, urlWithCommand, ndp.getResponseAsString(-1));
            }
        } catch (Exception e) {
            System.err.println("Exception in isAdmin: " + e);
            e.printStackTrace();
            if (e instanceof NotebookServerException) {
                throw ((NotebookServerException) e);
            }
            throw (new NotebookServerException("Error getting admin status from server: check log"));
        }
        return isAnAdmin;
    }

    /**
   *  Description of the Method
   *
   * @exception  NotebookServerException  Description of the Exception
   */
    public void loginViaGUI() throws NotebookServerException {
        JDialog loginDialog = new JDialog();
        loginDialog.setModal(true);
        loginDialog.setTitle("Notebook Login");
        LoginGUIPanel loginGUI = new LoginGUIPanel(this, mELNCon.allowGuests(), mELNCon.allowNewUsers(), mELNCon.getUserNames(), loginDialog);
        loginDialog.getContentPane().add(loginGUI);
        loginDialog.pack();
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        loginDialog.setLocation((d.width - loginDialog.getSize().width) / 2, (d.height - loginDialog.getSize().height) / 2);
        loginDialog.toFront();
        loginDialog.show();
        loginDialog.dispose();
        loginDialog = null;
    }

    /**
   *  Performs the login to the server, gets a result back from the server and
   *  parses it. The result also contains the password now encrypted.
   *
   * @exception  NotebookServerException  Description of the Exception
   */
    public void login() throws NotebookServerException {
        mServerStatus = LOGGED_IN;
        mRole = ADMIN_ROLE;
    }

    /**
   *  Description of the Method
   *
   * @exception  NotebookServerException  Description of the Exception
   */
    public void logout() throws NotebookServerException {
        mCredentials.setNBUserName("");
        mCredentials.setNBPassword("");
        mServerStatus = OFFLINE;
        mRole = NO_ROLE;
        mELNCon.logout();
    }

    /**
   *  Removes user from the list allowed access to this notebook Must be an
   *  admin for call to succeed.
   *
   * @param  theUserName                  Description of the Parameter
   * @exception  NotebookServerException  Description of the Exception
   */
    public void removeUser(String theUserName) throws NotebookServerException {
        if (mRole == ADMIN_ROLE) {
            URL urlWithCommand = makeCommandURL("removeELNUser");
            FormSubmitter theSubmitter = new FormSubmitter(urlWithCommand, mCredentials, mWebProxyURL);
            theSubmitter.addFormData("username", theUserName);
            Reader resultReader = null;
            theSubmitter.submit();
            resultReader = theSubmitter.getResponseReader();
            ELNHttpResponseParser ndp = new ELNHttpResponseParser(new LineNumberReader(resultReader));
            String httpResponse = ndp.getHTTPcode();
            String httpCode = null;
            if (httpResponse != null) {
                StringTokenizer parser = new StringTokenizer(httpResponse);
                String httpVersion = parser.nextToken();
                httpCode = parser.nextToken();
            }
            if (httpCode.equals("401")) {
                throw new NotebookAccessDeniedException("Access Denied - Check Password", urlWithCommand, httpCode);
            }
            String result = ndp.getELNResponse();
            if (!(result.equals(theUserName + " Removed"))) {
                throw new NotebookServerException("User " + theUserName + " not removed: " + result);
            }
        } else {
            throw new NotebookServerException("Must be admin to remove users: server not contacted");
        }
    }

    /**
   *  Adds a new user to the notebook - uses much of the same design as login
   *  After this is successful you still have to call login
   *
   * @param  newUser                      The feature to be added to the NewUser
   *      attribute
   * @param  newUserCredential            The feature to be added to the NewUser
   *      attribute
   * @param  addAsAdmin                   The feature to be added to the NewUser
   *      attribute
   * @exception  NotebookServerException  Description of the Exception
   */
    public void addNewUser(Principal newUser, Object newUserCredential, boolean addAsAdmin) throws NotebookServerException {
        if (!(newUser instanceof ELNPrincipal)) {
            throw new NotebookServerException("Input to addNewUser must implement ELNPrincipal");
        }
        URL urlWithCommand = makeCommandURL("addELNUser");
        FormSubmitter FormMgr = new FormSubmitter(urlWithCommand, mCredentials, mWebProxyURL);
        try {
            String admin = "false";
            if (addAsAdmin == true) {
                admin = "true";
            }
            FormMgr.addFormData(kFrmUserName, newUser.getName());
            FormMgr.addFormData(kFrmAddAsAdmin, admin);
            FormMgr.submit();
        } catch (ClassCastException cce) {
            throw new NotebookServerException("addNewUser: Credential must be a String");
        } catch (NotebookServerException nse) {
            throw new NotebookServerException("Error contacting server");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Reader addResponse = FormMgr.getResponseReader();
        ELNHttpResponseParser ndp = new ELNHttpResponseParser(new LineNumberReader(addResponse));
        String httpResponse = ndp.getHTTPcode();
        String httpCode = null;
        if (httpResponse != null) {
            StringTokenizer parser = new StringTokenizer(httpResponse);
            String httpVersion = parser.nextToken();
            httpCode = parser.nextToken();
        }
        if (httpCode.equals("401")) {
            throw new NotebookAccessDeniedException("Access Denied - Check Password", urlWithCommand, httpCode);
        }
        String addUserResult = ndp.getELNResponse();
        StringTokenizer parser = new StringTokenizer(addUserResult, ",");
        String status = parser.nextToken();
        if (status.equals("ADDUSER_ERROR")) {
            String msg = parser.nextToken();
            throw new NotebookServerException(msg, urlWithCommand, ndp.getResponseAsString(-1));
        }
        if (!status.equals("ADDUSER_OK")) {
            throw new NotebookServerException("Unknown Error Adding New User", urlWithCommand, ndp.getResponseAsString(-1));
        }
    }

    /**
	 *  setUserKeys - set the user's keypair on the server
	 * 
	 * @param  user                    User to find keys for
	 *      attribute
	 * @return  KeyPair                   
	 *      attribute
	 * @exception  NotebookServerException  Description of the Exception
	 */
    public void setNewUserKeys() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("DSA");
            KeyPair kp = gen.generateKeyPair();
            setUserKeys(kp, mCredentials.getNBUserName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 *  setUserKeys - set the user's keypair on the server
	 * 
	 * @param  user                    User to find keys for
	 *      attribute
	 * @return  KeyPair                   
	 *      attribute
	 * @exception  NotebookServerException  Description of the Exception
	 */
    public void setUserKeys(KeyPair keys, String user) throws NotebookServerException {
        URL urlWithCommand = makeCommandURL("getUserKeys");
        byte[] b = keys.getPublic().getEncoded();
        byte[] b2 = keys.getPrivate().getEncoded();
        String type = "text/xml";
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) urlWithCommand.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", type);
            conn.setRequestProperty("Content-Length", new Integer(b.length + b2.length).toString());
            conn.setRequestProperty("Cookie", "$Version=0; nb_pass='");
            conn.setRequestProperty("Cookie", "$Version=0; nb_user=root");
            conn.setRequestProperty("user", user);
            conn.setRequestProperty("publickeylength", new Integer(b.length).toString());
            conn.setRequestProperty("privatekeylength", new Integer(b2.length).toString());
            OutputStream os = conn.getOutputStream();
            os.write(b);
            os.write(b2);
            os.close();
            int rc = conn.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 *  getUserKeys - retrievs the user's keypair from the server
	 * 
	 * @param  user                    User to find keys for
	 *      attribute
	 * @return  KeyPair                   
	 *      attribute
	 * @exception  NotebookServerException  Description of the Exception
	*/
    public KeyPair getUserKeys(String user) throws NotebookServerException {
        KeyPair keys = null;
        byte[] publicResult = null;
        byte[] privateResult = null;
        URL urlWithCommand = makeCommandURL("getUserKeys");
        String type = "text/xml";
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) urlWithCommand.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", type);
            conn.setRequestProperty("Content-Length", new Integer(0).toString());
            conn.setRequestProperty("Cookie", "$Version=0; nb_pass='");
            conn.setRequestProperty("Cookie", "$Version=0; nb_user=root");
            conn.setRequestProperty("user", user);
            conn.setRequestProperty("keytype", "pubkey");
            InputStream is = conn.getInputStream();
            int av = is.available();
            publicResult = new byte[av];
            is.read(publicResult, 0, av);
            int rc = conn.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            conn = (HttpURLConnection) urlWithCommand.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", type);
            conn.setRequestProperty("Content-Length", new Integer(0).toString());
            conn.setRequestProperty("Cookie", "$Version=0; nb_pass='");
            conn.setRequestProperty("Cookie", "$Version=0; nb_user=root");
            conn.setRequestProperty("user", user);
            conn.setRequestProperty("keytype", "privkey");
            InputStream is = conn.getInputStream();
            int av = is.available();
            privateResult = new byte[av];
            is.read(privateResult, 0, av);
            int rc = conn.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            PrivateKey privateKey = null;
            PublicKey publicKey = null;
            KeyFactory keyFactory = KeyFactory.getInstance("DSA");
            try {
                if (privateResult != null && privateResult.length > 0) {
                    EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateResult);
                    privateKey = keyFactory.generatePrivate(privateKeySpec);
                }
            } catch (Exception e) {
                System.err.println("Error creating private key " + e);
            }
            try {
                if (publicResult != null && publicResult.length > 0) {
                    EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicResult);
                    publicKey = keyFactory.generatePublic(publicKeySpec);
                }
            } catch (Exception e) {
                System.err.println("Error creating public key " + e);
            }
            keys = new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return keys;
    }

    /**
   *  Allow a user to change their credentials (e.g. password, certificate) and
   *  admins to reset any user's password
   *
   * @param  submitterCreds               Description of the Parameter
   * @param  theCredsToChange             Description of the Parameter
   * @exception  NotebookServerException  Description of the Exception
   */
    public void resetUserCredentials(Credentials submitterCreds, Object theCredsToChange) throws NotebookServerException {
        URL urlWithCommand = null;
        urlWithCommand = makeCommandURL("changeELNUserCreds");
        FormSubmitter FormMgr = new FormSubmitter(urlWithCommand, submitterCreds, mWebProxyURL);
        String changingUser = ((SAMCredentials) theCredsToChange).getNBUserName();
        FormMgr.addFormData(kFrmUserName, changingUser);
        FormMgr.addFormData(kFrmPassword, ((SAMCredentials) submitterCreds).getNBPassword());
        FormMgr.addFormData(kFrmChangedPassword, ((SAMCredentials) theCredsToChange).getNBPassword());
        try {
            FormMgr.submit();
        } catch (NotebookServerException nse) {
            throw new NotebookServerException("Error contacting server");
        }
        Reader resetResponse = FormMgr.getResponseReader();
        ELNHttpResponseParser elnParser = new ELNHttpResponseParser(new LineNumberReader(resetResponse));
        String httpResponse = elnParser.getHTTPcode();
        String httpCode = null;
        if (httpResponse != null) {
            StringTokenizer parser = new StringTokenizer(httpResponse);
            String httpVersion = parser.nextToken();
            httpCode = parser.nextToken();
        }
        if (httpCode.equals("401")) {
            throw new NotebookAccessDeniedException("Access Denied - Check Password", urlWithCommand, elnParser.getResponseAsString(-1));
        }
        if (httpCode.equals("500")) {
            throw new NotebookServerException("Server Error - Contact Admin", urlWithCommand, elnParser.getResponseAsString(-1));
        }
        String resetResult = elnParser.getELNResponse();
        StringTokenizer parser = new StringTokenizer(resetResult, ",");
        String status = parser.nextToken();
        if (status.equals("CHANGEPASSWORD_ERROR")) {
            String msg = parser.nextToken();
            throw new NotebookServerException(msg, urlWithCommand, elnParser.getResponseAsString(-1));
        }
        if (!status.equals("CHANGEPASSWORD_OK")) {
            throw new NotebookServerException("Unknown Error ChangePassword", urlWithCommand, elnParser.getResponseAsString(-1));
        }
        try {
            if (URLEncoder.encode(changingUser, "UTF-8").equals(mCredentials.getNBUserName())) {
                String newpass = parser.nextToken();
                mCredentials.setNBPassword(newpass);
                mELNCon.setAuthInfo(mCredentials.makeNotebookCookies());
            }
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
        }
    }

    /**
   *  Description of the Method
   *
   * @return    Description of the Return Value
   */
    public int connectionStatus() {
        return mServerStatus;
    }

    /**
   *  A unit test for JUnit
   *
   * @return                              Description of the Return Value
   * @exception  NotebookServerException  Description of the Exception
   */
    public int testConnection() throws NotebookServerException {
        mServerStatus = UNKNOWN;
        URL urlWithCommand = makeCommandURL("status");
        FormSubmitter FormMgr = new FormSubmitter(urlWithCommand, mCredentials, mWebProxyURL);
        Reader result = null;
        FormMgr.submit();
        result = FormMgr.getResponseReader();
        ELNHttpResponseParser ndp = new ELNHttpResponseParser(new LineNumberReader(result));
        String httpResponse = ndp.getHTTPcode();
        String httpCode = null;
        if (httpResponse != null) {
            StringTokenizer parser = new StringTokenizer(httpResponse);
            String httpVersion = parser.nextToken();
            httpCode = parser.nextToken();
        }
        try {
            switch(Integer.parseInt(httpCode)) {
                case 200:
                    String elnResponse = ndp.getELNResponse();
                    if (elnResponse.startsWith("ELN_LOGGED_IN")) {
                        mServerStatus = LOGGED_IN;
                    } else if (elnResponse.startsWith("ELN_CONNECTED")) {
                        mServerStatus = CONNECTED;
                    } else {
                        mServerStatus = INTERNAL_ERROR;
                    }
                    break;
                case 401:
                    mServerStatus = AUTHINFO_REQUIRED;
                    break;
                case 403:
                    mServerStatus = ACCESS_DENIED;
                    break;
                case 404:
                    mServerStatus = NOT_FOUND;
                case 500:
                    mServerStatus = INTERNAL_ERROR;
                    break;
                default:
                    mServerStatus = HTTP_ERROR;
            }
        } catch (NumberFormatException nfe) {
            mServerStatus = INTERNAL_ERROR;
        }
        if ((mServerStatus != LOGGED_IN) || (mServerStatus != CONNECTED)) {
            result = new LineNumberReader(FormMgr.getResponseReader());
            String line;
            System.out.println("Connection Status(): Web Server Response:  ");
            try {
                while ((line = ((LineNumberReader) result).readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException io) {
                System.out.println("Unable to read server response.");
            }
        }
        return mServerStatus;
    }

    /**
   *  Method Description: add an annotation
   *
   * @param  newNObList                   the NObList to send
   * @param  anchorNObList                the NObList to attach the newNObList
   *      to
   * @return                              Description of the Return Value
   * @exception  FileNotFoundException    a NOb dataRef file was not found
   * @exception  UnknownHostException     mHost not found
   * @exception  NotebookServerException  notebook server contacted, but error
   *      occurred
   */
    public String addAnnotation(NObListNode newNObList, NObListNode anchorNObList) throws FileNotFoundException, UnknownHostException, NotebookServerException {
        return addAnnotation(newNObList, anchorNObList, false);
    }

    public String addAnnotation(NObListNode newNObList, NObListNode anchorNObList, boolean replaceEntry) throws FileNotFoundException, UnknownHostException, NotebookServerException {
        return addAnnotation(newNObList, anchorNObList, replaceEntry, false);
    }

    /**
   *  Adds a feature to the Annotation attribute of the SAMNotebookServerProxy
   *  object
   *
   * @param  newNObList                   The feature to be added to the
   *      Annotation attribute
   * @param  anchorNObList                The feature to be added to the
   *      Annotation attribute
   * @param  replaceEntry                 The feature to be added to the
   *      Annotation attribute
   * @param sendAuthorAndDate             Push the authorname and datetime values to the server - allowing migration to new servers without losing original info
   * @return                              Description of the Return Value
   * @exception  FileNotFoundException    Description of the Exception
   * @exception  UnknownHostException     Description of the Exception
   * @exception  NotebookServerException  Description of the Exception
   */
    public String addAnnotation(NObListNode newNObList, NObListNode anchorNObList, boolean replaceEntry, boolean sendAuthorAndDate) throws FileNotFoundException, UnknownHostException, NotebookServerException {
        if ((sendAuthorAndDate) && !(getUserRole() == ADMIN_ROLE)) {
            throw new NotebookServerException("Must be admin to perform migration");
        }
        NObListNode anchorClone = anchorNObList;
        if (!(anchorNObList instanceof WebdavNOb)) {
            anchorClone = convertToWebdavNOb(anchorNObList);
        } else {
            ((WebdavNOb) anchorClone).setCookies(mCredentials);
        }
        String newNObListPath = getNextAvailableChildURI(anchorNObList, (WebdavNOb) anchorClone);
        boolean success = false;
        try {
            success = ((WebdavNOb) anchorClone).mkcolMethod(newNObListPath);
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
        if (!success) {
            URL curURL = null;
            try {
                curURL = new URL(((WebdavNOb) anchorClone).getHttpURL().toString());
            } catch (MalformedURLException mfe) {
            }
            if (((WebdavNOb) anchorClone).getStatusCode() == 401) {
                throw new NotebookAccessDeniedException("Error during MKCOL", curURL, "status is " + ((WebdavNOb) anchorClone).getStatusCode());
            } else {
                throw new NotebookServerException("Error during MKCOL", curURL, "status is " + ((WebdavNOb) anchorClone).getStatusCode());
            }
        }
        Hashtable newProps = new Hashtable();
        if (sendAuthorAndDate) {
            String author = (String) newNObList.get("authorname");
            if (author.length() == 0) {
                author = "Undefined";
            } else {
                int pos = author.indexOf("<");
                if (pos != -1) {
                    author = author.substring(0, pos).trim();
                }
            }
            newProps.put(new PropertyName(WebdavNOb.DCNS, WebdavNOb.CREATOR), author);
            newProps.put(new PropertyName(WebdavNOb.SAMNS, WebdavNOb.ORIGINALCREATIONDATE), newNObList.get("datetime"));
            newProps.put(new PropertyName(WebdavNOb.SAMNS, WebdavNOb.IMPORTER), mCredentials.getNBUserName());
        } else {
            newProps.put(new PropertyName(WebdavNOb.DCNS, WebdavNOb.CREATOR), mCredentials.getNBUserName());
        }
        newProps.put(new PropertyName(WebdavNOb.DAVNS, WebdavNOb.DISPLAYNAME), newNObList.get("label"));
        newProps.put(new PropertyName(WebdavNOb.SAMNS, WebdavNOb.ELNTYPE), NObNode.NOBLISTMIMETYPE);
        newProps.put(new PropertyName(WebdavNOb.DCNS, WebdavNOb.DESCRIPTION), newNObList.get("description"));
        addNonStandardProperties(newNObList, newProps);
        org.jdom.Namespace xlinkNS = org.jdom.Namespace.getNamespace("xlink", WebdavNOb.XLINKNS);
        org.jdom.Namespace samNS = org.jdom.Namespace.getNamespace("sam", WebdavNOb.SAMNS);
        org.jdom.Element childPropValue = new org.jdom.Element("notebookroot", samNS);
        childPropValue.setAttribute("href", ((WebdavNOb) anchorClone).getNBRootNode(), xlinkNS);
        Enumeration children = newNObList.children();
        int count = 1;
        Hashtable childOIDs = new Hashtable();
        while (children.hasMoreElements()) {
            NObNode curChild = (NObNode) children.nextElement();
            String uri = (String) curChild.get("existinguri");
            if (uri == null) {
                String gcEntryName = "Entry_" + count;
                count++;
                uri = newNObListPath + gcEntryName;
                childOIDs.put(curChild, uri);
            } else {
                org.jdom.Element isReferencedElement = new org.jdom.Element("isReferencedByNote", samNS);
                isReferencedElement.setAttribute("href", newNObListPath, xlinkNS);
                isReferencedElement.setAttribute("title", (String) newNObList.get("label"), xlinkNS);
                String isReferencedIn = new org.jdom.output.XMLOutputter().outputString(isReferencedElement);
                Hashtable externalURLProps = new Hashtable();
                externalURLProps.put(new PropertyName(WebdavNOb.SAMNS, WebdavNOb.ELNTYPE), curChild.get("datatype"));
                externalURLProps.put(new PropertyName(WebdavNOb.SAMNS, WebdavNOb.DISPLAY), curChild.get("display"));
                externalURLProps.put(new PropertyName(WebdavNOb.SAMNS, "elnreferences"), isReferencedIn);
                boolean patchedOK = false;
                try {
                    patchedOK = ((WebdavNOb) anchorClone).proppatchMethod(uri, externalURLProps, true);
                } catch (Exception e) {
                    System.err.println(e);
                    e.printStackTrace();
                }
                if (!patchedOK) {
                    if (((WebdavNOb) anchorClone).getStatusCode() != 501) {
                        System.out.println("Error placing link on existing data at: " + uri + ", return status is " + ((WebdavNOb) anchorClone).getStatusCode());
                    }
                }
            }
            org.jdom.Element gChildElement = new org.jdom.Element("child", samNS);
            gChildElement.setAttribute("href", uri, xlinkNS);
            gChildElement.setAttribute("title", (String) curChild.get("label"), xlinkNS);
            childPropValue.addContent(gChildElement);
        }
        String value = new org.jdom.output.XMLOutputter().outputString(childPropValue);
        newProps.put(new PropertyName(WebdavNOb.SAMNS, WebdavNOb.ELNCHILDREN), value);
        success = false;
        try {
            success = ((WebdavNOb) anchorClone).proppatchMethod(newNObListPath, newProps, true);
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
        if (!success) {
            URL curURL = null;
            try {
                curURL = new URL(((WebdavNOb) anchorClone).getHttpURL().toString());
            } catch (MalformedURLException mfe) {
            }
            if (((WebdavNOb) anchorClone).getStatusCode() == 401) throw new NotebookAccessDeniedException("Error during PROPPATCH of " + newNObListPath, curURL, "status is " + ((WebdavNOb) anchorClone).getStatusCode()); else throw new NotebookServerException("Error during PROPPATCH of " + newNObListPath, curURL, "status is " + ((WebdavNOb) anchorClone).getStatusCode());
        }
        children = newNObList.children();
        while (children.hasMoreElements()) {
            NObNode node = (NObNode) children.nextElement();
            String oid = (String) childOIDs.get(node);
            if (oid != null) {
                success = false;
                try {
                    success = ((WebdavNOb) anchorClone).putMethod(oid, ((NOb) node).openNObNodeDataInputStream(), (String) node.get("datatype"));
                } catch (Exception e) {
                    System.err.println(e);
                    e.printStackTrace();
                }
                if (!success) {
                    URL curURL = null;
                    try {
                        curURL = new URL(((WebdavNOb) anchorClone).getHttpURL().toString());
                    } catch (MalformedURLException mfe) {
                    }
                    if (((WebdavNOb) anchorClone).getStatusCode() == 401) throw new NotebookAccessDeniedException("Error during PUT", curURL, "status is " + ((WebdavNOb) anchorClone).getStatusCode()); else throw new NotebookServerException("Error during PUT", curURL, "status is " + ((WebdavNOb) anchorClone).getStatusCode());
                }
                newProps.clear();
                newProps.put(new PropertyName(WebdavNOb.DCNS, WebdavNOb.CREATOR), mCredentials.getNBUserName());
                newProps.put(new PropertyName(WebdavNOb.DAVNS, WebdavNOb.DISPLAYNAME), node.get("label"));
                Object elnType = node.get("datatype");
                Enumeration responses = null;
                Property childProp = null;
                try {
                    Vector propToGet = new Vector(1);
                    propToGet.add(new PropertyName(WebdavNOb.DAVNS, WebdavNOb.CONTENTTYPE));
                    responses = ((WebdavNOb) anchorClone).propfindMethod(oid, DepthSupport.DEPTH_0, propToGet);
                    if (responses != null) {
                        ResponseEntity response = (ResponseEntity) responses.nextElement();
                        Enumeration properties = response.getProperties();
                        childProp = (Property) properties.nextElement();
                        String result = childProp.getPropertyAsString();
                        if (result != null && result.length() > 0) {
                            elnType = result;
                        }
                    }
                } catch (Exception e) {
                    System.err.println(e);
                }
                newProps.put(new PropertyName(WebdavNOb.SAMNS, WebdavNOb.ELNTYPE), elnType);
                newProps.put(new PropertyName(WebdavNOb.DCNS, WebdavNOb.DESCRIPTION), node.get("description"));
                String display = (String) node.get("display");
                if (display == null) {
                    display = "on";
                }
                newProps.put(new PropertyName(WebdavNOb.SAMNS, WebdavNOb.DISPLAY), display);
                addNonStandardProperties(node, newProps);
                success = false;
                try {
                    success = ((WebdavNOb) anchorClone).proppatchMethod(oid, newProps, true);
                } catch (Exception e) {
                    System.err.println(e);
                    e.printStackTrace();
                }
                if (!success) {
                    URL curURL = null;
                    try {
                        curURL = new URL(((WebdavNOb) anchorClone).getHttpURL().toString());
                    } catch (MalformedURLException mfe) {
                    }
                    if (((WebdavNOb) anchorClone).getStatusCode() == 401) throw new NotebookAccessDeniedException("Error during PROPPATCH of " + oid, curURL, "status is " + ((WebdavNOb) anchorClone).getStatusCode()); else throw new NotebookServerException("Error during PROPPATCH of " + oid, curURL, "status is " + ((WebdavNOb) anchorClone).getStatusCode());
                }
            }
        }
        Enumeration responses = null;
        Property childProp = null;
        try {
            Vector propToGet = new Vector(1);
            propToGet.add(new PropertyName(WebdavNOb.SAMNS, WebdavNOb.ELNCHILDREN));
            responses = ((WebdavNOb) anchorClone).propfindMethod(DepthSupport.DEPTH_0, propToGet);
            if (responses == null) {
                throw new Exception("null response");
            }
            ResponseEntity response = (ResponseEntity) responses.nextElement();
            Enumeration properties = response.getProperties();
            childProp = (Property) properties.nextElement();
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
            URL curURL = null;
            try {
                curURL = new URL(((WebdavNOb) anchorClone).getHttpURL().toString());
            } catch (MalformedURLException mfe) {
            }
            if (((WebdavNOb) anchorClone).getStatusCode() == 401) throw new NotebookAccessDeniedException("Error during PROPFIND", curURL, "status is " + ((WebdavNOb) anchorClone).getStatusCode()); else throw new NotebookServerException("Error during PROPFIND", curURL, "status is " + ((WebdavNOb) anchorClone).getStatusCode());
        }
        try {
            boolean found = false;
            org.jdom.Element childElement = null;
            org.jdom.Element root = null;
            if (childProp.getStatusCode() != 404) {
                SAXBuilder sb = new SAXBuilder();
                ListIterator iter = null;
                ByteArrayInputStream bin = new ByteArrayInputStream(("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?><dummy>" + childProp.getPropertyAsXMLString(false) + "</dummy>").getBytes());
                org.jdom.Document transformedDoc = sb.build(bin);
                root = transformedDoc.getRootElement();
                List childrenNodes = root.getChildren();
                iter = childrenNodes.listIterator();
                while (iter.hasNext() && !found) {
                    childElement = (org.jdom.Element) iter.next();
                    if (childElement.getAttributeValue("href", xlinkNS).equals(((WebdavNOb) anchorClone).getNBRootNode())) {
                        found = true;
                    }
                }
            }
            org.jdom.Element gChildElement = new org.jdom.Element("child", samNS);
            gChildElement.setAttribute("href", newNObListPath, xlinkNS);
            gChildElement.setAttribute("title", (String) newNObList.get("label"), xlinkNS);
            if (found) {
                childElement.addContent(gChildElement);
                value = new org.jdom.output.XMLOutputter().outputString(root);
                value = value.substring(7, value.length() - 8);
            } else {
                childElement = new org.jdom.Element("notebookroot", samNS);
                childElement.setAttribute("href", ((WebdavNOb) anchorClone).getNBRootNode(), xlinkNS);
                childElement.addContent(gChildElement);
                if (root == null) {
                    value = new org.jdom.output.XMLOutputter().outputString(childElement);
                } else {
                    root.addContent(childElement);
                    value = new org.jdom.output.XMLOutputter().outputString(root);
                    value = value.substring(7, value.length() - 8);
                }
            }
        } catch (Exception ne) {
            throw new NotebookServerException(ne.getMessage());
        }
        success = false;
        try {
            newProps.clear();
            newProps.put(new PropertyName(WebdavNOb.SAMNS, WebdavNOb.ELNCHILDREN), value);
            success = ((WebdavNOb) anchorClone).proppatchMethod(newProps, true);
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
        if (!success) {
            URL curURL = null;
            try {
                curURL = new URL(((WebdavNOb) anchorClone).getHttpURL().toString());
            } catch (MalformedURLException mfe) {
            }
            if (((WebdavNOb) anchorClone).getStatusCode() == 401) {
                throw new NotebookAccessDeniedException("Error during PROPPATCH on parent", curURL, "status is " + ((WebdavNOb) anchorClone).getStatusCode());
            } else {
                throw new NotebookServerException("Error during PROPPATCH on parent", curURL, "status is " + ((WebdavNOb) anchorClone).getStatusCode());
            }
        }
        int myLevelIndex = ((NObListNode) anchorNObList).getLevelIndex();
        if (myLevelIndex < (NObListNode.kLevels.length - 1)) {
            myLevelIndex++;
        }
        newNObList.put("level", NObListNode.kLevels[myLevelIndex]);
        return newNObListPath;
    }

    /**
     *  Revises an annotation with new children (that are content nodes)
     *
     * @param  newNObList Some new children to be added.  This will be a
     *         NObList, not a WebdavNOb, and it will only have content nodes,
     *         not subdirectories.
     * @param  anchorNObList The node we want to add them to
     * @return The url of the anchorNOb
     * @exception  FileNotFoundException
     * @exception  UnknownHostException
     * @exception  NotebookServerException
     */
    public String reviseAnnotation(NObListNode newNObList, NObListNode anchorNObList) throws FileNotFoundException, UnknownHostException, NotebookServerException {
        NObListNode anchorClone = anchorNObList;
        if (!(anchorNObList instanceof WebdavNOb)) {
            anchorClone = convertToWebdavNOb(anchorNObList);
        } else {
            ((WebdavNOb) anchorClone).setCookies(mCredentials);
        }
        String newNObListPath = anchorClone.getOriginalOID();
        boolean success;
        org.jdom.Namespace xlinkNS = org.jdom.Namespace.getNamespace("xlink", WebdavNOb.XLINKNS);
        org.jdom.Namespace samNS = org.jdom.Namespace.getNamespace("sam", WebdavNOb.SAMNS);
        Hashtable newProps = new Hashtable();
        Enumeration children = newNObList.children();
        Hashtable newChildren = new Hashtable();
        int count = 1;
        while (children.hasMoreElements()) {
            NObNode node = (NObNode) children.nextElement();
            String oid = (String) node.get("existinguri");
            if (oid == null) {
                System.out.println("oid is null - making one");
                String gcEntryName = "Entry_" + count;
                count++;
                oid = newNObListPath + gcEntryName;
            }
            System.out.println("saving child: " + oid);
            newChildren.put(oid, node);
            success = false;
            try {
                success = ((WebdavNOb) anchorClone).putMethod(oid, ((NOb) node).openNObNodeDataInputStream(), (String) node.get("datatype"));
            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace();
            }
            if (!success) {
                URL curURL = null;
                try {
                    curURL = new URL(((WebdavNOb) anchorClone).getHttpURL().toString());
                } catch (MalformedURLException mfe) {
                }
                if (((WebdavNOb) anchorClone).getStatusCode() == 401) throw new NotebookAccessDeniedException("Error during PUT", curURL, "status is " + ((WebdavNOb) anchorClone).getStatusCode()); else throw new NotebookServerException("Error during PUT", curURL, "status is " + ((WebdavNOb) anchorClone).getStatusCode());
            }
            newProps.clear();
            newProps.put(new PropertyName(WebdavNOb.DCNS, WebdavNOb.CREATOR), mCredentials.getNBUserName());
            newProps.put(new PropertyName(WebdavNOb.DAVNS, WebdavNOb.DISPLAYNAME), node.get("label"));
            newProps.put(new PropertyName(WebdavNOb.SAMNS, WebdavNOb.ELNTYPE), node.get("datatype"));
            newProps.put(new PropertyName(WebdavNOb.DCNS, WebdavNOb.DESCRIPTION), node.get("description"));
            String display = (String) node.get("display");
            if (display == null) {
                display = "on";
            }
            newProps.put(new PropertyName(WebdavNOb.SAMNS, WebdavNOb.DISPLAY), display);
            addNonStandardProperties(node, newProps);
            success = false;
            try {
                success = ((WebdavNOb) anchorClone).proppatchMethod(oid, newProps, true);
            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace();
            }
            if (!success) {
                URL curURL = null;
                try {
                    curURL = new URL(((WebdavNOb) anchorClone).getHttpURL().toString());
                } catch (MalformedURLException mfe) {
                }
                if (((WebdavNOb) anchorClone).getStatusCode() == 401) throw new NotebookAccessDeniedException("Error during PROPPATCH of " + oid, curURL, "status is " + ((WebdavNOb) anchorClone).getStatusCode()); else throw new NotebookServerException("Error during PROPPATCH of " + oid, curURL, "status is " + ((WebdavNOb) anchorClone).getStatusCode());
            }
        }
        Enumeration responses = null;
        Property childProp = null;
        try {
            Vector propToGet = new Vector(1);
            propToGet.add(new PropertyName(WebdavNOb.SAMNS, WebdavNOb.ELNCHILDREN));
            responses = ((WebdavNOb) anchorClone).propfindMethod(DepthSupport.DEPTH_0, propToGet);
            if (responses == null) {
                throw new Exception("null response");
            }
            ResponseEntity response = (ResponseEntity) responses.nextElement();
            Enumeration properties = response.getProperties();
            childProp = (Property) properties.nextElement();
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
            URL curURL = null;
            try {
                curURL = new URL(((WebdavNOb) anchorClone).getHttpURL().toString());
            } catch (MalformedURLException mfe) {
            }
            if (((WebdavNOb) anchorClone).getStatusCode() == 401) throw new NotebookAccessDeniedException("Error during PROPFIND", curURL, "status is " + ((WebdavNOb) anchorClone).getStatusCode()); else throw new NotebookServerException("Error during PROPFIND", curURL, "status is " + ((WebdavNOb) anchorClone).getStatusCode());
        }
        org.jdom.Element childElement = null;
        org.jdom.Element root = null;
        if (childProp.getStatusCode() != 404) {
            try {
                SAXBuilder sb = new SAXBuilder();
                ByteArrayInputStream bin = new ByteArrayInputStream(("<dummy>" + childProp.getPropertyAsXMLString(false) + "</dummy>").getBytes());
                org.jdom.Document transformedDoc = sb.build(bin);
                root = transformedDoc.getRootElement();
                List childrenNodes = root.getChildren();
                List grandChildren;
                ListIterator gcIter;
                org.jdom.Element gChild;
                ListIterator iter = childrenNodes.listIterator();
                String uri;
                String title;
                Hashtable childrenToRemove = new Hashtable();
                while (iter.hasNext()) {
                    childElement = (org.jdom.Element) iter.next();
                    if (childElement.getAttributeValue("href", xlinkNS).equals(((WebdavNOb) anchorClone).getNBRootNode())) {
                        grandChildren = childElement.getChildren();
                        gcIter = grandChildren.listIterator();
                        while (gcIter.hasNext()) {
                            gChild = (org.jdom.Element) gcIter.next();
                            uri = gChild.getAttributeValue("href", xlinkNS);
                            title = gChild.getAttributeValue("title", xlinkNS);
                            if (!title.equals("")) {
                                childrenToRemove.put(uri, gChild);
                            }
                        }
                        break;
                    }
                }
                Enumeration keys = childrenToRemove.keys();
                String curUri;
                org.jdom.Element gChildElement;
                while (keys.hasMoreElements()) {
                    curUri = (String) keys.nextElement();
                    gChildElement = (org.jdom.Element) childrenToRemove.get(curUri);
                    System.out.println("removing child: " + curUri);
                    childElement.removeContent(gChildElement);
                }
                keys = newChildren.keys();
                NObNode curNOb;
                while (keys.hasMoreElements()) {
                    curUri = (String) keys.nextElement();
                    curNOb = (NObNode) newChildren.get(curUri);
                    gChildElement = new org.jdom.Element("child", samNS);
                    gChildElement.setAttribute("href", curUri, xlinkNS);
                    gChildElement.setAttribute("title", (String) curNOb.get("label"), xlinkNS);
                    childElement.addContent(gChildElement);
                }
                String value = new org.jdom.output.XMLOutputter().outputString(root);
                value = value.substring(7, value.length() - 8);
                newProps.put(new PropertyName(WebdavNOb.SAMNS, WebdavNOb.ELNCHILDREN), value);
            } catch (Exception ne) {
                ne.printStackTrace();
                throw new NotebookServerException(ne.getMessage());
            }
        }
        newProps.put(new PropertyName(WebdavNOb.DCNS, WebdavNOb.CREATOR), mCredentials.getNBUserName());
        newProps.put(new PropertyName(WebdavNOb.DAVNS, WebdavNOb.DISPLAYNAME), newNObList.get("label"));
        newProps.put(new PropertyName(WebdavNOb.SAMNS, WebdavNOb.ELNTYPE), NObNode.NOBLISTMIMETYPE);
        newProps.put(new PropertyName(WebdavNOb.DCNS, WebdavNOb.DESCRIPTION), newNObList.get("description"));
        addNonStandardProperties(newNObList, newProps);
        success = false;
        try {
            success = ((WebdavNOb) anchorClone).proppatchMethod(newNObListPath, newProps, true);
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
        if (!success) {
            URL curURL = null;
            try {
                curURL = new URL(((WebdavNOb) anchorClone).getHttpURL().toString());
            } catch (MalformedURLException mfe) {
            }
            if (((WebdavNOb) anchorClone).getStatusCode() == 401) throw new NotebookAccessDeniedException("Error during PROPPATCH of Annotation" + newNObListPath, curURL, "status is " + ((WebdavNOb) anchorClone).getStatusCode()); else throw new NotebookServerException("Error during PROPPATCH of Annotation" + newNObListPath, curURL, "status is " + ((WebdavNOb) anchorClone).getStatusCode());
        }
        return newNObListPath;
    }

    protected WebdavNOb convertToWebdavNOb(NObListNode anchorNObList) throws NotebookServerException {
        String uri = "";
        WebdavNOb ret = null;
        try {
            uri = anchorNObList.getOriginalOID();
            int port = mServerURL.getPort();
            if (port == -1) {
                port = mServerURL.getDefaultPort();
            }
            String newurl = (new URL(mServerURL.getProtocol(), mServerURL.getHost(), port, uri)).toExternalForm();
            HttpURL httpURL = createURL(newurl);
            if (mCredentials.getGrpUserName() != null && mCredentials.getGrpUserName().length() > 0) {
                httpURL.setEscapedUserinfo(mCredentials.getGrpUserName(), mCredentials.getGrpPassword());
            }
            ret = new WebdavNOb(httpURL, mRootNode, (uri.equals("/")), mCredentials);
            ret.setContext(mELNCon.getContext());
        } catch (Exception e) {
            System.err.println("Error converting anchor to WebdavNOb: " + uri + " : " + e);
            throw new NotebookServerException(e.getMessage());
        }
        return ret;
    }

    /**
   *  Description of the Method
   *
   * @param  nobToDelete                  Description of the Parameter
   * @exception  NotebookServerException  Description of the Exception
   */
    public void delete(NObNode nobToDelete) throws NotebookServerException {
        URL urlWithCommand = makeCommandURL("trash" + nobToDelete.getOriginalOID());
        FormSubmitter FormMgr = new FormSubmitter(urlWithCommand, mCredentials, mWebProxyURL);
        FormMgr.submit();
    }

    /**
   *  Description of the Method
   *
   * @param  nobToExport                  Description of the Parameter
   * @param  exportFile                   Description of the Parameter
   * @exception  NotebookServerException  Description of Exception
   */
    public void export(NObNode nobToExport, File exportFile, String exportType, boolean encodeData) throws NotebookServerException {
        OutputStream exportFileStream = null;
        try {
            System.out.println("ENCODING DATA " + encodeData);
            NObNode theRealNOb = (NObNode) getNObs(true, nobToExport, -1);
            exportFileStream = new FileOutputStream(exportFile);
            if (exportType.equals("XML")) {
                theRealNOb.writeToXML(exportFileStream, encodeData);
            } else {
                theRealNOb.writeToMIME(exportFileStream);
            }
        } catch (NotebookAccessDeniedException nade) {
            throw nade;
        } catch (IOException iox) {
            System.err.println("IO exception occured during export.  " + iox);
            throw new NotebookServerException("IO exception occured during export.  " + iox);
        } finally {
            try {
                exportFileStream.close();
            } catch (Exception e) {
            }
        }
    }

    public void exportNotebookInfo(NObNode nobToExport, File exportFile) throws NotebookServerException {
        String oldFileName = exportFile.getAbsolutePath();
        String newFileName = "";
        int dotIndex = oldFileName.lastIndexOf(".");
        int slashIndex = oldFileName.lastIndexOf("\\");
        if (dotIndex >= 0 && dotIndex > slashIndex) {
            newFileName = oldFileName.substring(0, dotIndex) + "-nbinfo.xml";
        } else {
            newFileName = oldFileName + "-archive.xml";
        }
        if (nobToExport.getLevelIndex() > 0) {
            String nobToExportS = ((WebdavNOb) nobToExport).getNBRootNode();
        }
        URL urlWithCommand = makeCommandURL("getNotebookInfo");
        GetSubmitter theSubmitter = new GetSubmitter(urlWithCommand, mCredentials, mWebProxyURL);
        Reader resultReader = null;
        theSubmitter.submit();
        resultReader = theSubmitter.getResponseReader();
        try {
            FileOutputStream fos = new FileOutputStream(newFileName);
            LineNumberReader lnreader = new LineNumberReader(resultReader);
            String line = "";
            line = lnreader.readLine();
            while (!line.trim().equals("")) {
                line = lnreader.readLine();
            }
            while (lnreader.ready()) {
                line = lnreader.readLine() + "\n";
                fos.write(line.getBytes());
            }
            fos.close();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    /**
   *  Description of the Method
   *
   * @param  theCommand                   Description of the Parameter
   * @return                              Description of the Return Value
   * @exception  NotebookServerException  Description of the Exception
   */
    public URL makeCommandURL(String theCommand) throws NotebookServerException {
        String commandURLString = mServerURL.toExternalForm();
        commandURLString += "/" + theCommand + "/" + mELNCon.getNBNumber();
        URL newURL = null;
        try {
            newURL = new URL(commandURLString);
        } catch (MalformedURLException mue) {
            throw new NotebookServerException("Error creating commandURL: " + commandURLString);
        }
        return newURL;
    }

    /**
   *  Adds a feature to the StatusChangeListener attribute of the
   *  SAMNotebookServerProxy object
   *
   * @param  scl  The feature to be added to the StatusChangeListener attribute
   */
    public void addStatusChangeListener(StatusChangeListener scl) {
        mELNCon.addStatusChangeListener(scl);
    }

    /**
   *  Description of the Method
   *
   * @param  scl  Description of the Parameter
   */
    public void removeStatusChangeListener(StatusChangeListener scl) {
        mELNCon.removeStatusChangeListener(scl);
    }

    /**
   *  Description of the Method
   *
   * @param  evt  Description of the Parameter
   */
    public void performAdminAction(AdminActionEvent evt) {
        mAdmin.performAdminAction(evt);
    }

    /**
   *  Description of the Method
   *
   * @param  evt  Description of the Parameter
   */
    public void valueChanged(TreeSelectionEvent evt) {
        InterestDialog i = InterestDialog.getExistingDialog();
        if (i != null) {
            i.valueChanged(evt);
        }
    }

    /**
   *  Gets the nextAvailableChildURI attribute of the SAMNotebookServerProxy
   *  object
   *
   * @param  theNode                      Description of the Parameter
   * @param  clone                        Description of Parameter
   * @return                              The nextAvailableChildURI value
   * @exception  NotebookServerException  Description of Exception
   */
    protected String getNextAvailableChildURI(NObListNode theNode, WebdavNOb clone) throws NotebookServerException {
        int entrynum = theNode.getChildCount() + 1;
        String entryName = null;
        int level = theNode.getLevelIndex();
        if (level < (NObListNode.kLevels.length - 1)) {
            level++;
        }
        boolean openName = false;
        try {
            clone.setCookies(mCredentials);
            while (!openName) {
                entryName = NObListNode.kLevels[level] + "_" + entrynum;
                entryName = theNode.getOriginalOID() + entryName + "/";
                clone.headMethod(entryName);
                int status = clone.getStatusCode();
                if (status >= 200 && status <= 299) {
                    entrynum++;
                } else {
                    openName = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (clone.getStatusCode() == 401) throw new NotebookAccessDeniedException(e.getMessage()); else throw new NotebookServerException(e.getMessage());
        }
        return entryName;
    }

    /**
   *  Gets the tempResponseFile attribute of the SAMNotebookServerProxy object
   *
   * @return                  The tempResponseFile value
   * @exception  IOException  Description of the Exception
   */
    protected File getTempResponseFile() throws IOException {
        String elnTempDir = "elntemp";
        try {
            EmslProperties elnProps = EmslProperties.getApplicationProperties();
            String tmpDir = (String) elnProps.get("tmpdir");
            if (tmpDir != null) {
                elnTempDir = tmpDir;
            }
        } catch (IOException io) {
            System.err.println("Couldn't open Application Properties file in SAMNotebookServerProxy.java: " + io.getMessage());
        }
        File tempFile = new File(elnTempDir, "response.tmp");
        File tempDir = new File(tempFile.getParent());
        if (!tempDir.exists()) {
            if (!tempDir.mkdirs()) {
                throw new IOException("Unable to create dir for temporary files" + " in SAMNotebookServerProxy.getTempResponseFile()");
            }
        }
        return tempFile;
    }

    /**
   *  This method defines a name for the file to be used on the server side. If
   *  a filename exists on the client side, use it. If not, create one from the
   *  given name stem. Note: the server may still append an "_n" to the filename
   *  if a file of the requested name already exists.
   *
   * @param  theNOb    Description of the Parameter
   * @param  nameStem  Description of the Parameter
   * @return           The nObFileNameOnServer value
   */
    protected String getNObFileNameOnServer(NObNode theNOb, String nameStem) {
        String serverDataRef = null;
        String dataRef = (String) theNOb.get("dataref");
        if (dataRef.startsWith(NOb.FILEPROTOCOL)) {
            serverDataRef = dataRef.substring(NOb.FILEPROTOCOL.length());
        } else if (dataRef.startsWith(NOb.TEMPFILEPROTOCOL)) {
            if (!(dataRef.startsWith(NOb.TEMPFILEPROTOCOL + "newentry"))) {
                serverDataRef = dataRef.substring(NOb.TEMPFILEPROTOCOL.length());
                int pos = 0;
                if ((pos = serverDataRef.lastIndexOf(File.separatorChar)) != -1) {
                    serverDataRef = serverDataRef.substring(pos + 1);
                }
            }
        }
        if (serverDataRef == null) {
            String type = (String) theNOb.get("datatype");
            serverDataRef = nameStem + MimeTypes.getExtension(type);
        }
        return serverDataRef;
    }

    /**
   *  Description of the Method
   *
   * @param  theResponse      Description of the Parameter
   * @exception  IOException  Description of the Exception
   */
    protected void removeHTTPHeader(LineNumberReader theResponse) throws IOException {
        assert (theResponse != null);
        String line;
        theResponse.mark(200);
        line = theResponse.readLine();
        try {
            while (!line.toLowerCase().startsWith("content-type: multipart")) {
                theResponse.mark(200);
                line = theResponse.readLine();
            }
            theResponse.reset();
        } catch (NullPointerException npe) {
            throw new IOException("Unexpected end of response in removeHTTPHeader");
        }
    }

    /**
   *  Description of the Method
   *
   * @param  theResponse      Description of the Parameter
   * @return                  Description of the Return Value
   * @exception  IOException  Description of the Exception
   */
    protected String readMIMEHeader(LineNumberReader theResponse) throws IOException {
        Hashtable firstBlock = NObArchive.readMIMEBlock(theResponse);
        if (!firstBlock.containsKey("Content-NObList-Version".toLowerCase())) {
            throw new IOException("Error reading archive. It is not Content-ENArcMIME-Version compliant.");
        }
        if (!((String) firstBlock.get("Content-NObList-Version".toLowerCase())).equals("1.1")) {
            throw new IOException("Error reading archive. It is not Content-ENArcMIME-Version: 1.1 compliant.");
        }
        if (!firstBlock.containsKey("Content-type".toLowerCase())) {
            throw new IOException("Error reading archive. No content-type key.");
        }
        StringTokenizer boundaryTokenizer = new StringTokenizer((String) firstBlock.get("Content-type".toLowerCase()), "\"");
        boundaryTokenizer.nextToken();
        return boundaryTokenizer.nextToken();
    }

    /**
   *  Description of the Method
   *
   * @param  theResponseStream  Description of the Parameter
   * @return                    Description of the Return Value
   */
    protected OutputStream openTempFileResponseOutputStream(ByteArrayOutputStream theResponseStream) {
        FileOutputStream newResponseStream = null;
        try {
            File responseFile = getTempResponseFile();
            newResponseStream = new FileOutputStream(responseFile);
            newResponseStream.write(theResponseStream.toByteArray());
        } catch (IOException io) {
            System.out.println("Unable to open response temp output file in SAMNotebookServerProxy");
        }
        return newResponseStream;
    }

    /**
   *  Description of the Method
   *
   * @return    Description of the Return Value
   */
    protected InputStream openTempFileResponseInputStream() {
        FileInputStream newResponseStream = null;
        try {
            File responseFile = getTempResponseFile();
            newResponseStream = new FileInputStream(responseFile);
        } catch (IOException io) {
            System.out.println("Unable to open response temp input file in SAMNotebookServerProxy");
        }
        return newResponseStream;
    }

    /**
   *  Method Description: print a form data name/value pair to theBuf
   *
   * @param  theBuf    PrintWriter to append to
   * @param  theName   name of the form element
   * @param  theValue  value of the form element
   */
    protected void addFormElement(PrintWriter theBuf, String theName, Object theValue) {
        assert ((theValue instanceof String) || (theValue instanceof byte[]));
        boolean isString = (theValue instanceof String);
        String encoding = isString ? "quoted-printable" : "base64";
        String mimeType = isString ? "text/plain" : "application/octet-stream";
        String encodedValue = null;
        if (isString == true) {
            encodedValue = QuotedPrintable.encodeString((String) theValue);
        } else {
            encodedValue = Base64.encodeByteArray((byte[]) theValue);
        }
        theBuf.print("--" + mBoundary + "\r\n" + "Content-Disposition: form-data; name=\"" + theName + "\"");
        theBuf.print("\r\nContent-Type: " + mimeType);
        theBuf.print("\r\nContent-Transfer-Encoding: " + encoding);
        theBuf.print("\r\n\r\n" + encodedValue + "\r\n");
    }

    /**
   *  Adds a feature to the NonStandardProperties attribute of the
   *  SAMNotebookServerProxy object
   *
   * @param  theNObNode     The feature to be added to the NonStandardProperties
   *      attribute
   * @param  theProperties  The feature to be added to the NonStandardProperties
   *      attribute
   */
    protected void addNonStandardProperties(NObNode theNObNode, Hashtable theProperties) {
        Enumeration keys = theNObNode.keys();
        Vector keyVec = new Vector();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            keyVec.add(key);
        }
        keyVec.remove("label");
        keyVec.remove("authorname");
        keyVec.remove("datetime");
        keyVec.remove("objectid");
        keyVec.remove("datatype");
        keyVec.remove("dataref");
        keyVec.remove("description");
        keyVec.remove("level");
        keyVec.remove("display");
        keyVec.remove("data");
        keys = keyVec.elements();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            Object value = theNObNode.get(key);
            try {
                theProperties.put(new PropertyName(ELNNS, key), (String) value);
            } catch (ClassCastException cce) {
                System.err.println("Cannot store property " + key + " on " + theNObNode.getOriginalOID() + ": value is not a String");
            }
        }
    }

    protected HttpURL createURL(String theURLString) throws URIException {
        return new HttpURL(theURLString);
    }

    protected void setupProtocol() {
    }

    /**
   *  Takes the result from the web server and parses it using
   *  ELNHttpResponseParser The result will have a "LOGIN-OK" or an "ERROR" in
   *  it.
   *
   * @param  result  - the result of the login form submission to the browser
   * @return         Description of the Return Value
   */
    private boolean parseLogin(Reader result) {
        ELNHttpResponseParser ndp = new ELNHttpResponseParser(new LineNumberReader(result));
        String loginResult = ndp.getELNResponse();
        if (loginResult != null) {
            StringTokenizer parser = new StringTokenizer(loginResult, ",");
            String status = parser.nextToken();
            if (status.equals("LOGIN-OK")) {
                mRole = NORMAL_ROLE;
                String newCookies = ndp.getCookies();
                while (parser.hasMoreTokens()) {
                    String admin = parser.nextToken();
                    if (admin.equals("1")) {
                        mRole = ADMIN_ROLE;
                    }
                }
                mCredentials.updateCredentialsFromCookies(newCookies);
                mELNCon.setAuthInfo(newCookies);
                if (mCredentials.getNBUserName().equalsIgnoreCase("guest")) {
                    mRole = GUEST_ROLE;
                }
                mServerStatus = LOGGED_IN;
            } else {
                mRole = NO_ROLE;
                mServerStatus = CONNECTED;
            }
        }
        return (mServerStatus == LOGGED_IN);
    }

    /**
   *  Performs a proppatch on given Node, using passed in properties.
   *
   * @param  newNObList  - the NObNode to patch
   * @param  newProps    -  the Hashtable of properties to use
   */
    public void updateNOb(NObNode newNObList, Hashtable newProps) throws NotebookServerException {
        Enumeration keys = newProps.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
        }
        String newNObListPath = newNObList.getOriginalOID();
        boolean success = false;
        try {
            System.out.println("Patching to " + newNObListPath);
            boolean successfulLock = ((WebdavNOb) newNObList).lockMethod(newNObListPath);
            if (successfulLock) {
                success = ((WebdavNOb) newNObList).proppatchMethod(newNObListPath, newProps, true);
                ((WebdavNOb) newNObList).unlockMethod(newNObListPath);
            }
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
        if (!success) {
            URL curURL = null;
            try {
                curURL = new URL(((WebdavNOb) newNObList).getHttpURL().toString());
            } catch (MalformedURLException mfe) {
                mfe.printStackTrace();
            }
            if (((WebdavNOb) newNObList).getStatusCode() == 401) throw new NotebookAccessDeniedException("Error during PROPPATCH of " + newNObListPath, curURL, "status is " + ((WebdavNOb) newNObList).getStatusCode()); else throw new NotebookServerException("Error during PROPPATCH of " + newNObListPath, curURL, "status is " + ((WebdavNOb) newNObList).getStatusCode());
        }
    }

    public void setLanguage(String lc) {
        mELNCon.setLanguage(lc);
    }
}
