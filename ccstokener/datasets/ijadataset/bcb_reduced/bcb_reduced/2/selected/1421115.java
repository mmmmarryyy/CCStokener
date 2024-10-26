package com.ondelette.servlet.webforum;

import java.util.*;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.net.*;
import com.ondelette.servlet.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.queryParser.*;

/**
 *  A forum object containing posts and folder of posts
 *
 *@author     Daniel Lemire
 *@created    March 12th 1999
 */
public final class Forum {

    /**
	 *  The title to be displayed in HTML
	 *
	 *@since    0.30
	 */
    protected String mForumTitle;

    /**
	 *  A subtitle to be displayed in HTML
	 *
	 *@since    0.30
	 */
    protected String mForumSubTitle;

    /**
	* If non null, will parse the content of this URL
	* and append the content of body before the actual
	* output of the servlet.
	*/
    protected URL mPrefixURL;

    /**
	* If non null, will parse the content of this URL
	* and append the content of body after the actual
	* output of the servlet.
	*/
    protected URL mSuffixURL;

    /**
	 *  The object which determines how things are displayed (including language, charset encoding...)
	 *
	 *@since    0.30
	 */
    protected ForumLocale mForumLocale;

    /**
	 *  How often messages are written to disk.
	 *
	 *@since    0.30
	 */
    protected long mSaveDelay;

    /**
	 *  Threshold on user level for post privilege (default is 0).
	 *
	 *@since    0.30
	 */
    protected int mPostPrivilege = 0;

    /**
	 *  MessageFolder object, maintains a database of sorts.
	 *
	 *@since    0.30
	 */
    protected MessageFolder mMessageFolder;

    /**
	 *  List of all users registered with this forum.
	 *
	 *@since    0.30
	 */
    protected UserList mUserList;

    /**
	 *  Directory where the database is stored.
	 *
	 *@since    0.30
	 */
    protected File mMessageArchiveDirectory;

    /**
	 *  File containing the configuration parameters of this forum.
	 *
	 *@since    0.30
	 */
    protected File mConfigFile;

    /**
	 *  File containing the parameter of the ForumLocale object.
	 *
	 *@since    0.30
	 */
    protected File mForumLocaleFile;

    /**
	 *  File containing the list of users.
	 *
	 *@since    0.30
	 */
    protected File mUserListFile;

    /**
	 *  Threshold to be able to view the forum.
	 *
	 *@since    0.30
	 */
    protected int mAuthorizationLevel = 0;

    /**
	 *  A string used in the footer of the HTML pages.
	 *
	 *@since    0.30
	 */
    protected String mForumFooter;

    /**
	 *  A URL to the login page.
	 *
	 *@since    0.30
	 */
    protected String mWelcomeURL;

    protected String mFailedLoginURL;

    /**
	 *  URL pointing to the CSS file.
	 *
	 *@since    0.30
	 */
    protected String mCSSURL;

    /**
	 *  pointing to the lucene index search directory.
	 *
	 *@since    0.36.000  - Stan Towianski
	 */
    protected File mSearchIndexDir;

    protected String mSearchIndexDirPath;

    final String EOL = "\n";

    final String ckSearchHdrScript = "<script language=\"javascript\">" + EOL + "function ckSearch()" + EOL + "{" + EOL + "if ( window.document.searchForm.searchField.options[window.document.searchForm.searchField.selectedIndex].value == '' )" + EOL + "   {" + EOL + "   alert( 'Please select a field to search on first.' );" + EOL + "   return false;" + EOL + "   }" + EOL + "}" + EOL + "</script>" + EOL;

    /**
	 *  Constructor for the Forum object
	 *
	 *@param  forumconfigfile
	 *@exception  IOException
	 *@since                   0.30
	 */
    public Forum(String dataDirectory, String filename) throws IOException {
        File forumconfigfile = new File(dataDirectory + filename);
        System.out.println("forumconfigfile =" + forumconfigfile.getAbsolutePath() + "=");
        if (!forumconfigfile.exists()) throw new IllegalArgumentException("Config file " + forumconfigfile.getAbsolutePath() + " could not be found!");
        Properties p = new Properties();
        BufferedInputStream fis = new BufferedInputStream(new FileInputStream(forumconfigfile));
        p.load(fis);
        fis.close();
        mConfigFile = new File(filename);
        mMessageArchiveDirectory = new File(ForumsList.dataDirectory + p.getProperty("MessageArchiveDirectory"));
        mForumLocaleFile = new File(dataDirectory + p.getProperty("ForumLocaleFile"));
        System.out.println("Locale config file =" + mForumLocaleFile.getAbsolutePath() + "=");
        if (!mForumLocaleFile.exists()) {
            throw new IllegalArgumentException("Locale config file " + mForumLocaleFile.getAbsolutePath() + " could not be found!");
        }
        mUserListFile = new File(ForumsList.dataDirectory + p.getProperty("UserListFile"));
        File tempuserfile = new File(mUserListFile.getAbsolutePath() + ".tmp");
        System.err.println("Temp user list file: " + mUserListFile.getAbsolutePath() + ".tmp");
        if (!mUserListFile.exists() && tempuserfile.exists()) {
            System.err.println("\nForum(): user list file does not exist but temp user list file does!");
            System.err.println("Temp user list file is : " + tempuserfile.getAbsolutePath());
            System.err.println("User list file is : " + mUserListFile.getAbsolutePath());
            System.err.println("Use temp user list file as real user list file since it's missing.");
            if (!tempuserfile.renameTo(mUserListFile)) {
                System.err.println("Forum(): Couldn't copy temp user file to real user file, aborting!");
                System.err.println("Temp user list file is : " + tempuserfile.getAbsolutePath());
                System.err.println("User list file is : " + mUserListFile.getAbsolutePath());
                throw new IllegalArgumentException("User List config file " + mUserListFile.getAbsolutePath() + " could not be found!");
            }
        }
        if (!mUserListFile.exists()) {
            System.err.println("I will create missing User list file =" + mUserListFile.getAbsolutePath() + "=");
            mUserListFile.createNewFile();
        }
        try {
            mSaveDelay = Long.parseLong(p.getProperty("SaveDelay"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        mCSSURL = p.getProperty("CSSURL");
        if (mCSSURL == null) {
            mCSSURL = "";
        }
        mForumTitle = p.getProperty("ForumTitle");
        if (mForumTitle == null) {
            mForumTitle = "";
        }
        mForumSubTitle = p.getProperty("ForumSubTitle");
        if (mForumSubTitle == null) {
            mForumSubTitle = "";
        }
        try {
            mAuthorizationLevel = Integer.parseInt(p.getProperty("AuthorizationLevel"));
        } catch (Exception e) {
        }
        mForumFooter = p.getProperty("ForumFooter");
        if (mForumFooter == null) {
            mForumFooter = "";
        }
        mWelcomeURL = p.getProperty("WelcomeURL");
        if (mWelcomeURL == null) {
            mWelcomeURL = "";
        }
        mFailedLoginURL = p.getProperty("FailedLoginURL");
        if (mFailedLoginURL == null) {
            mFailedLoginURL = mWelcomeURL;
        }
        String prefix = p.getProperty("PrefixURL");
        try {
            if (prefix != null) mPrefixURL = new URL(prefix.trim());
        } catch (MalformedURLException murle) {
            System.err.println("Is this a URL: '" + prefix + "'");
            murle.printStackTrace();
        }
        String suffix = p.getProperty("SuffixURL");
        try {
            if (suffix != null) mSuffixURL = new URL(suffix.trim());
        } catch (MalformedURLException murle) {
            System.err.println("Is this a URL: '" + suffix + "'");
            murle.printStackTrace();
        }
        mSearchIndexDirPath = p.getProperty("SearchIndexDirPath");
        if (mSearchIndexDirPath == null) {
            mSearchIndexDirPath = "";
            mSearchIndexDir = null;
        } else {
            mSearchIndexDirPath = ForumsList.dataDirectory + mSearchIndexDirPath;
            mSearchIndexDir = new File(mSearchIndexDirPath);
        }
        try {
            mPostPrivilege = Integer.parseInt(p.getProperty("PostPrivilege"));
        } catch (Exception e) {
        }
        mForumLocale = ForumLocaleFactory.getForumLocale(mForumLocaleFile);
        mUserList = UserListFactory.getUserList(mUserListFile);
        mMessageFolder = new MessageFolder(this);
        if (mSearchIndexDir != null) {
            try {
                Date start = new Date();
                IndexWriter writer = null;
                System.out.println("Index path =" + mSearchIndexDirPath + "segments");
                File tmpfile = new File(mSearchIndexDirPath + "segments");
                if (!tmpfile.exists()) {
                    System.out.println("Index path defined but no segment file for index exists so will create new index.");
                    writer = new IndexWriter(mSearchIndexDirPath, new StandardAnalyzer(), true);
                    indexDocs(writer, mMessageArchiveDirectory);
                    writer.optimize();
                }
                if (writer != null) writer.close();
                Date end = new Date();
                System.out.print(end.getTime() - start.getTime());
                System.out.println(" total milliseconds to create new index on forum.");
                tmpfile = null;
                start = null;
                end = null;
            } catch (Exception e) {
                System.out.println(" f: caught a " + e.getClass() + "\n with message: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    protected ForumLocale getForumLocale() {
        return mForumLocale;
    }

    public Message getMessageFolderMessage(int id) throws IOException {
        return mMessageFolder.getMessage(id);
    }

    /**
	 *  Index a Directory of Documents
	 *  Stan Towianski
	 *@param  writer
	 *@param  file
	 * see similar code in MessageFolder.addMessage()
	 *@since           0.36.0
 *   Notes:  Dec/2003  Stan Towianski
 *                Added code to add new msg/doc to lucene search index for forum.
	 */
    public void indexDocs(IndexWriter writer, File file) throws Exception {
        if (file.isDirectory()) {
            String[] files = file.list();
            for (int i = 0; i < files.length; i++) {
                int mri;
                try {
                    mri = Integer.parseInt(files[i]);
                } catch (Exception e) {
                    System.out.println("\nSkipping file [" + i + "] because non-number name =" + files[i] + "=\n");
                    continue;
                }
                Message message = mMessageFolder.getMessage(mri);
                writer.addDocument(FileDocument.Document(message.getSubject(), message.getMessage(), mMessageArchiveDirectory.getAbsolutePath() + "/" + files[i], message.getAuthor()));
            }
        }
    }

    /**
	 *  Gets the page attribute of the Forum object
	 *
	 *@param  k
	 *@param  out
	 *@param  user
	 *@param  servletPathp.getProperty( "SearchIndexDir" )
	 *@exception  IOException
	 *@since                   0.30
	 */
    public void getPage(int k, PrintWriter out, User user, String servletPath, HttpServletRequest request) throws IOException {
        mForumLocale.printForumHeader(this, "", "", ckSearchHdrScript, out);
        printBodyFrom(mPrefixURL, out);
        mForumLocale.printForumTitle(request, this, out);
        mForumLocale.printMenu(this, out, k, user, servletPath);
        mMessageFolder.writePageSummaryHTML(k, user, out, servletPath, request);
        mForumLocale.printSearchForm(this, out, user, servletPath);
        if (mPostPrivilege > 0) {
            if (user != null) {
                if (user.getAutorizationLevel() >= mPostPrivilege) {
                    mForumLocale.printPostForm(this, out, user, servletPath);
                }
            }
        } else {
            mForumLocale.printPostForm(this, out, user, servletPath);
        }
        mForumLocale.printMenu(this, out, k, user, servletPath);
        mForumLocale.printButtons(this, out, user, servletPath);
        out.print(mForumFooter);
        printBodyFrom(mSuffixURL, out);
        ServletCopyright.printFooter(out);
    }

    /**
	 *  Gets a search text page attribute of the Forum object
	 *
	 *@param  k
	 *@param  out
	 *@param  user
	 *@param  servletPath
	 *@exception  IOException
	 *@since                   0.36.000  Stan Towianski
	 *  Notes:  Dec/2003 would like to put in paging later. Used method like 'last messages' for now
	 *               but would rather have it do paging like regular forum when/if I had time.
	 */
    public void getSearchPage(String queryString, String queryField, int k, PrintWriter out, User user, String servletPath, HttpServletRequest request) throws IOException {
        boolean error = false;
        Searcher searcher = new IndexSearcher(mSearchIndexDirPath);
        Analyzer analyzer = new StopAnalyzer();
        String indexLocation = mSearchIndexDirPath;
        Query query = null;
        Hits hits = null;
        int startindex = 0;
        int maxpage = 100;
        String startVal = "0";
        String maxresults = "100";
        int thispage = 0;
        Vector mSrchVec = new Vector();
        mForumLocale.printForumHeader(this, "", "", ckSearchHdrScript, out);
        printBodyFrom(mPrefixURL, out);
        mForumLocale.printForumTitle(request, this, out);
        mForumLocale.printMenu(this, out, k, user, servletPath);
        mForumLocale.printBackToForum(out, this, servletPath);
        try {
            searcher = new IndexSearcher(IndexReader.open(mSearchIndexDirPath));
        } catch (Exception e) {
            out.println("<p>ERROR opening the Index - contact sysadmin!</p>");
            out.println("<p>While parsing query: " + e.getMessage() + "</p>");
            error = true;
        }
        if (error == false) {
            try {
                maxpage = Integer.parseInt(maxresults);
                startindex = Integer.parseInt(startVal);
            } catch (Exception e) {
            }
            try {
                query = QueryParser.parse(queryString, queryField, analyzer);
            } catch (ParseException e) {
                out.println("<p>While parsing query: " + e.getMessage() + "</p>");
                error = true;
            }
        }
        if (error == false && searcher != null) {
            thispage = maxpage;
            hits = searcher.search(query);
            if (hits.length() == 0) {
                out.println("<p> I'm sorry I couldn't find what you were looking for. </p>");
                error = true;
            }
        }
        if (error == false && searcher != null) {
            if ((startindex + maxpage) > hits.length()) {
                thispage = hits.length() - startindex;
            }
            for (int i = startindex; i < (thispage + startindex); i++) {
                Document doc = hits.doc(i);
                String path = doc.get("path");
                int at = path.lastIndexOf('/');
                int iint = Integer.parseInt(path.substring(at + 1));
                MessageReference mr = new MessageReference(iint);
                mSrchVec.addElement(mr);
            }
        }
        out.println("Search for text >" + queryString + "<  in field: " + queryField);
        mMessageFolder.writeSearchMessagesSummaryHTML(mSrchVec, hits.length(), user, out, servletPath, request);
        mForumLocale.printMenu(this, out, k, user, servletPath);
        mForumLocale.printSearchForm(this, out, user, servletPath);
        if (mPostPrivilege > 0) {
            if (user != null) {
                if (user.getAutorizationLevel() >= mPostPrivilege) {
                    mForumLocale.printPostForm(this, out, user, servletPath);
                }
            }
        } else {
            mForumLocale.printPostForm(this, out, user, servletPath);
        }
        mForumLocale.printButtons(this, out, user, servletPath);
        out.print(mForumFooter);
        printBodyFrom(mSuffixURL, out);
        ServletCopyright.printFooter(out);
    }

    public UserList getUserList() {
        return mUserList;
    }

    /**
	 *  Gets the lastMessages attribute of the Forum object
	 *
	 *@param  k
	 *@param  out
	 *@param  user
	 *@param  servletPath
	 *@exception  IOException
	 *@since                   0.30
	 */
    public void getLastMessages(int k, PrintWriter out, User user, String servletPath, HttpServletRequest request) throws IOException {
        mForumLocale.printForumHeader(this, out);
        printBodyFrom(mPrefixURL, out);
        mForumLocale.printForumTitle(request, this, out);
        mForumLocale.printMenu(this, out, -1, user, servletPath);
        mForumLocale.printBackToForum(out, this, servletPath);
        mMessageFolder.writeLastMessagesSummaryHTML(k, user, out, servletPath, request);
        if (mPostPrivilege > 0) {
            if (user != null) {
                if (user.getAutorizationLevel() >= mPostPrivilege) {
                    mForumLocale.printPostForm(this, out, user, servletPath);
                }
            }
        } else {
            mForumLocale.printPostForm(this, out, user, servletPath);
        }
        mForumLocale.printMenu(this, out, -1, user, servletPath);
        mForumLocale.printButtons(this, out, user, servletPath);
        out.print(mForumFooter);
        printBodyFrom(mSuffixURL, out);
        ServletCopyright.printFooter(out);
    }

    /**
	 *  Adds a feature to the Message attribute of the Forum object
	 *
	 *@param  subjectString    The subject of this message
	 *@param  messageString    The actual message itself
	 *@param  suthorString     The author of this message
	 *@param  rmailString      The email address of the author of this message
     *@param  optionalURLString A URL that was given along with the message
     *@param  linkTitleString   The title of the link
	 *@param  user             The feature to be added to the Message attribute
	 *@return an index to the message that has just been added
	 *@exception  IOException
	 *@since                   0.30
	 */
    public MessageReference addMessage(String subjectString, String messageString, String authorString, String emailString, String optionalURLString, String linkTitleString, User user) throws IOException {
        Message message = new Message(authorString, emailString, subjectString, messageString, optionalURLString, linkTitleString);
        if (user != null) {
            message.setAutorizationLevel(user.getAutorizationLevel());
        }
        return (addMessage(message));
    }

    /**
	 *  Adds a feature to the Message attribute of the Forum object
	 *
	 *@param  message          The feature to be added to the Message attribute
	 *@return
	 *@exception  IOException
	 *@since                   0.30
	 */
    public MessageReference addMessage(Message message) throws IOException {
        return (mMessageFolder.addMessage(message));
    }

    /**
	 *  Adds a feature to the Reply attribute of the Forum object
	 *
	 *@param k                 The ID of this message
	 *@param subjectString     The subject of this message
	 *@param messageString     The body of this message
	 *@param authorString      The name of the author of this message
	 *@param emailString       The email address of the author of this message
	 *@param optionalURLString The URL of a link that goes at the bottom of the message
	 *@param linkTitleString   The title of the link to be added
	 *@param user              The user posting the message
	 *@return                  A reference to the message just added
	 *@exception  IOException
	 *@since                   0.30
	 */
    public MessageReference addReply(int k, String subjectString, String messageString, String authorString, String emailString, String optionalURLString, String linkTitleString, User user) throws IOException {
        Message message = new Message(authorString, emailString, subjectString, messageString, optionalURLString, linkTitleString);
        message.setInReplyTo(k);
        if (user != null) {
            message.setAutorizationLevel(user.getAutorizationLevel());
        }
        return (addMessage(message));
    }

    /**
	 *  
	 *
	 *@param  k
	 *@param  out
	 *@param  user
	 *@param  servletPath
	 *@exception  IOException
	 *@since                   0.30
	 */
    public void showMessage(int k, PrintWriter out, User user, String servletPath, HttpServletRequest request) throws IOException {
        Message message = mMessageFolder.getMessage(k);
        if (message != null) if (message.getMessage() != null) mForumLocale.printForumHeader(this, message.getSubject(), HTMLUtil.makeStringHTMLSafe(message.getMessage()), "", out); else mForumLocale.printForumHeader(this, message.getSubject(), null, "", out); else mForumLocale.printForumHeader(this, out);
        printBodyFrom(mPrefixURL, out);
        mForumLocale.printForumTitle(request, this, out);
        mForumLocale.printMenu(this, out, -1, user, servletPath);
        mForumLocale.printInFull(k, this, out, user, servletPath, request);
        if (mPostPrivilege > 0) {
            if (user != null) {
                if (user.getAutorizationLevel() >= mPostPrivilege) {
                    mForumLocale.printReplyForm(k, this, out, user, servletPath);
                }
            }
        } else {
            mForumLocale.printReplyForm(k, this, out, user, servletPath);
        }
        mForumLocale.printButtons(this, out, user, servletPath);
        out.print(mForumFooter);
        printBodyFrom(mSuffixURL, out);
        ServletCopyright.printFooter(out);
    }

    /**
	 *  
	 *
	 *@param  k
	 *@param  out
	 *@param  user
	 *@param  servletPath
	 *@exception  IOException
	 *@since                   0.30
	 */
    public void deleteMessage(int k, PrintWriter out, User user, String servletPath, HttpServletRequest request) throws IOException {
        mMessageFolder.deleteMessage(k);
        getPage(0, out, user, servletPath, request);
    }

    /**
	 *  
	 *
	 *@param  k
	 *@param  out
	 *@param  user
	 *@param  servletPath
	 *@exception  IOException
	 *@since                   0.30
	 */
    public void deleteMessageContent(int k, PrintWriter out, User user, String servletPath, HttpServletRequest request) throws IOException {
        mMessageFolder.deleteMessageContent(k);
        getPage(0, out, user, servletPath, request);
    }

    /**
	 *  
	 *
	 *@param  k
	 *@param  out
	 *@param  user
	 *@param  servletPath
	 *@exception  IOException
	 *@since                   0.30
	 */
    public void askForConfirmationOnDeletingMessage(int k, PrintWriter out, User user, String servletPath, HttpServletRequest request) throws IOException {
        mForumLocale.printForumHeader(this, out);
        printBodyFrom(mPrefixURL, out);
        mForumLocale.printForumTitle(request, this, out);
        mForumLocale.printDeleteConfirmation(k, this, out, user, servletPath, request);
        out.print(mForumFooter);
        printBodyFrom(mSuffixURL, out);
        ServletCopyright.printFooter(out);
    }

    /**
	 *  
	 *
	 *@param  k
	 *@param  out
	 *@param  user
	 *@param  servletPath
	 *@exception  IOException
	 *@since                   0.30
	 */
    public void askForConfirmationOnErasingMessageContent(int k, PrintWriter out, User user, String servletPath, HttpServletRequest request) throws IOException {
        mForumLocale.printForumHeader(this, out);
        printBodyFrom(mPrefixURL, out);
        mForumLocale.printForumTitle(request, this, out);
        mForumLocale.printEraseContentConfirmation(k, this, out, user, servletPath, request);
        out.print(mForumFooter);
        printBodyFrom(mSuffixURL, out);
        ServletCopyright.printFooter(out);
    }

    /**
	 *  
	 *
	 *@since    0.30
	 */
    public void destroy() {
        mMessageFolder.destroy();
    }

    /**
	 *  
	 *
	 *@param  name
	 *@param  out
	 *@exception  IOException
	 *@since                   0.30
	 */
    public void showWarningNameInUse(String name, PrintWriter out) throws IOException {
        mForumLocale.printForumHeader(this, out);
        printBodyFrom(mPrefixURL, out);
        mForumLocale.printForumTitle(null, this, out);
        mForumLocale.showWarningNameInUse(name, out, this);
        out.print(mForumFooter);
        printBodyFrom(mSuffixURL, out);
        ServletCopyright.printFooter(out);
    }

    /**
	 *  
	 *
	 *@param  name
	 *@param  out
	 *@exception  IOException
	 *@since                   0.30
	 */
    public void showWarningPasswordsDontMatch(String name, PrintWriter out) throws IOException {
        mForumLocale.printForumHeader(this, out);
        printBodyFrom(mPrefixURL, out);
        mForumLocale.printForumTitle(null, this, out);
        mForumLocale.showWarningPasswordsDontMatch(name, out, this);
        out.print(mForumFooter);
        printBodyFrom(mSuffixURL, out);
        ServletCopyright.printFooter(out);
    }

    /**
	 *  
	 *
	 *@param  out
	 *@exception  IOException
	 *@since                   0.30
	 */
    public void showWarningAutorizationRequired(PrintWriter out) throws IOException {
        mForumLocale.printForumHeader(this, out);
        printBodyFrom(mPrefixURL, out);
        mForumLocale.printForumTitle(null, this, out);
        mForumLocale.showWarningAutorizationRequired(out, this);
        out.print(mForumFooter);
        printBodyFrom(mSuffixURL, out);
        ServletCopyright.printFooter(out);
    }

    /**
	 *  
	 *
	 *@param  out
	 *@exception  IOException
	 *@since                   0.30
	 */
    public void showWarningCannotChangeAdmin(PrintWriter out) throws IOException {
        mForumLocale.printForumHeader(this, out);
        printBodyFrom(mPrefixURL, out);
        mForumLocale.printForumTitle(null, this, out);
        mForumLocale.showWarningCannotChangeAdmin(out, this);
        out.print(mForumFooter);
        printBodyFrom(mSuffixURL, out);
        ServletCopyright.printFooter(out);
    }

    /**
	 *  
	 *
	 *@param  out
	 *@param  servletPath
	 *@exception  IOException
	 *@since                   0.30
	 */
    public void displayUserList(User user, PrintWriter out, String servletPath) throws IOException {
        mForumLocale.printForumHeader(this, out);
        printBodyFrom(mPrefixURL, out);
        mForumLocale.printForumTitle(null, this, out);
        mForumLocale.printUserList(this, user, out, servletPath);
        out.print(mForumFooter);
        printBodyFrom(mSuffixURL, out);
        ServletCopyright.printFooter(out);
    }

    /**
	 *  
	 *
	 *@param  name
	 *@param  out
	 *@param  servletPath
	 *@exception  IOException
	 *@since                   0.30
	 */
    public void displayUser(User user, String name, PrintWriter out, String servletPath) throws IOException {
        mForumLocale.printForumHeader(this, out);
        printBodyFrom(mPrefixURL, out);
        mForumLocale.printForumTitle(null, this, out);
        mForumLocale.printUser(this, user, name, out, servletPath);
        out.print(mForumFooter);
        printBodyFrom(mSuffixURL, out);
        ServletCopyright.printFooter(out);
    }

    /**
	* This code makes the assumption that lines are not too long
	* and that a string put to lowercase has the same length. Should work fine for
	* most HTML out there.
	*/
    public static void printBodyFrom(URL url, PrintWriter out) {
        if (url == null) return;
        try {
            URLConnection con = url.openConnection();
            con.setDoInput(true);
            InputStream is = con.getInputStream();
            String ContentType = con.getContentType();
            String ContentEncoding = con.getContentEncoding();
            boolean grabbody = true;
            if (ContentType != null) {
                if (!ContentType.equals("text/html")) {
                    grabbody = false;
                }
            } else grabbody = false;
            InputStreamReader isr = null;
            if (ContentEncoding != null) isr = new InputStreamReader(is, ContentEncoding); else isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            boolean MustWrite = !grabbody;
            String line;
            boolean LookForClosure = false;
            while ((line = br.readLine()) != null) {
                if (MustWrite) {
                    if (grabbody) {
                        String lowercaseline = line.toLowerCase();
                        if (LookForClosure) {
                            int beginindex = line.indexOf(">");
                            if (beginindex == -1) continue;
                            LookForClosure = false;
                            if (beginindex + 1 < line.length()) {
                                int endindex = lowercaseline.indexOf("</body>");
                                if (endindex == -1) out.println(line.substring(beginindex + 1, line.length())); else if (beginindex + 1 < endindex) out.println(line.substring(beginindex + 1, endindex));
                            }
                            continue;
                        }
                        int index = lowercaseline.indexOf("</body>");
                        if (index == -1) {
                            out.println(line);
                            continue;
                        }
                        out.println(line.substring(0, index));
                        break;
                    }
                    out.println(line);
                    continue;
                }
                String lowercaseline = line.toLowerCase();
                int index = lowercaseline.indexOf("<body");
                if (index == -1) continue;
                MustWrite = true;
                int beginindex = lowercaseline.indexOf(">", index);
                if (beginindex == -1) {
                    LookForClosure = true;
                    continue;
                }
                if (beginindex + 1 < line.length()) {
                    int endindex = lowercaseline.indexOf("</body>");
                    if (endindex == -1) out.println(line.substring(beginindex + 1, line.length())); else if (beginindex + 1 < endindex) out.println(line.substring(beginindex + 1, endindex));
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static void main(String[] arg) throws Exception {
        PrintWriter pw = new PrintWriter(System.out);
        printBodyFrom(new URL(arg[0]), pw);
        pw.flush();
    }
}
