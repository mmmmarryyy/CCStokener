package openfuture.bugbase.servlet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import openfuture.bugbase.domain.BugReport;
import openfuture.bugbase.domain.Login;
import openfuture.bugbase.domain.User;
import openfuture.bugbase.model.TransactionResult;
import openfuture.bugbase.model.Version;
import openfuture.bugbase.xobjects.BugBaseQuery;
import openfuture.bugbase.xobjects.BugBaseQueryResult;
import openfuture.bugbase.xobjects.QueryResultMasterData;
import openfuture.bugbase.xobjects.QueryResultVersions;
import openfuture.util.error.I18NException;
import openfuture.util.misc.Message;

/**
 * <p>This class is the proxy to {@link BugBaseServlet}. It communicates with
 * serialized objects via HTTP with the servlet.</p>
 *
 *
 * Created: Sun Feb 06 16:58:12 2000
 *
 * @author <a href="mailto:wolfgang@openfuture.de">Wolfgang Reissenberger</a>
 * @version $Revision: 1.10 $
 */
public class BugBaseServletClient {

    private URL url;

    private Integer sessionID;

    /**
     * Creates a new <code>BugBaseServletClient</code> instance.
     *
     * @param newUrl URL of
     * {@link openfuture.bugbase.servlet.BugBaseServlet BugBaseServlet}
     * @exception MalformedURLException if an error occurs
     */
    public BugBaseServletClient(String newUrl) throws MalformedURLException {
        url = new URL(newUrl);
    }

    /**
     * Retrieve the list of bug reports matching the given criteria.
     *
     * @param project project name
     * @param filterReported state at least reported?
     * @param filterStarted state at least started?
     * @param filterFixed state at least fixed?
     * @param filterRejected state at least rejected?
     * @param searchString string that should occur either in the
     *        title or in the description
     * @param reporterId user ID of the bug reporter
     * @param doctorId user ID of the bug doctor
     * @return list of {@link openfuture.bugbase.domain.BugReport bug reports}.
     * @exception I18NException if an error occurs
     */
    public LinkedList getBugReportList(String project, Boolean filterReported, Boolean filterStarted, Boolean filterFixed, Boolean filterRejected, String searchString, String reporterId, String doctorId) throws I18NException {
        LinkedList parameter = new LinkedList();
        parameter.add(project);
        parameter.add(filterReported);
        parameter.add(filterStarted);
        parameter.add(filterFixed);
        parameter.add(filterRejected);
        parameter.add(searchString);
        parameter.add(reporterId);
        parameter.add(doctorId);
        BugBaseQuery query = createQuery(BugBaseQuery.QUERY_BUG_REPORT_LIST, parameter);
        BugBaseQueryResult result = doQuery(query);
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw (new I18NException(new Message("exception.failed.bugreports.get"), (I18NException) result.getResult()));
        } else {
            return ((LinkedList) result.getResult());
        }
    }

    /**
     * Retrieve a single bug report.
     *
     * @param id ID of the bug report.
     * @return bug report matching the given ID
     * @exception I18NException if an error occurs
     */
    public BugReport getBugReport(Integer id) throws I18NException {
        LinkedList parameter = new LinkedList();
        parameter.add(id);
        BugBaseQuery query = createQuery(BugBaseQuery.QUERY_BUG_REPORT, parameter);
        BugBaseQueryResult result = doQuery(query);
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw (new I18NException(new Message("exception.failed.bugreport.get"), (I18NException) result.getResult()));
        } else {
            return ((BugReport) result.getResult());
        }
    }

    /**
     * Add a new bug report. The number of the report is returned.
     *
     * @param project project name
     * @param report the bug report
     * @return number of the report
     * @exception I18NException if an error occurs
     */
    public Integer addBugReport(String project, BugReport report) throws I18NException {
        LinkedList parameter = new LinkedList();
        parameter.add(project);
        parameter.add(report);
        BugBaseQuery query = createQuery(BugBaseQuery.QUERY_ADD_BUGREPORT, parameter);
        BugBaseQueryResult result = doQuery(query);
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw ((I18NException) result.getResult());
        }
        return ((Integer) result.getResult());
    }

    /**
     * Add a new bug report. The number of the report is returned.
     *
     * @param project project name.
     * @param report new bug report.
     * @param sessionID session ID.
     * @return number of the report
     * @exception I18NException if an error occurs
     */
    public synchronized Integer addBugReport(String project, BugReport report, Integer sessionID) throws I18NException {
        LinkedList parameter = new LinkedList();
        parameter.add(project);
        parameter.add(report);
        BugBaseQuery query = createQuery(BugBaseQuery.QUERY_ADD_BUGREPORT, parameter);
        BugBaseQueryResult result = doQuery(query);
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw (new I18NException(new Message("exception.failed.bugreport.add"), (I18NException) result.getResult()));
        }
        return ((Integer) result.getResult());
    }

    /**
     * Retrieve the list of projects registered at Bug Base.
     *
     * @return list of projects
     * @exception I18NException if an error occurs
     */
    public LinkedList getProjects() throws I18NException {
        BugBaseQuery query = createQuery(BugBaseQuery.QUERY_PROJECTS, null);
        BugBaseQueryResult result = doQuery(query);
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw (new I18NException(new Message("exception.failed.projects.get"), (I18NException) result.getResult()));
        } else {
            return ((LinkedList) result.getResult());
        }
    }

    /**
     * Set the list of projects registered in Bug Base
     *
     * @param projects list of projects
     * @exception I18NException if an error occurs
     */
    public void setProjects(LinkedList projects) throws I18NException {
        LinkedList parameter = new LinkedList();
        parameter.add(projects);
        BugBaseQuery query = createQuery(BugBaseQuery.QUERY_PROJECTS, parameter);
        BugBaseQueryResult result = doQuery(query);
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw (new I18NException(new Message("exception.failed.projects.set"), (I18NException) result.getResult()));
        }
    }

    /**
     * Retrieve the list of packages for a given project.
     *
     * @param project project name
     * @return list of packages
     * @exception I18NException if an error occurs
     */
    public LinkedList getPackages(String project) throws I18NException {
        LinkedList parameter = new LinkedList();
        parameter.add(project);
        BugBaseQuery query = createQuery(BugBaseQuery.QUERY_PACKAGES, parameter);
        BugBaseQueryResult result = doQuery(query);
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw (new I18NException(new Message("exception.failed.packages.get"), (I18NException) result.getResult()));
        } else {
            return ((LinkedList) result.getResult());
        }
    }

    /**
     * Retrieves the list of packages for a certain project.
     *
     * @param project project name
     * @param sessionID session ID
     * @return list of package names
     * @exception I18NException if an error occurs
     */
    public LinkedList getPackages(String project, Integer sessionID) throws I18NException {
        LinkedList parameter = new LinkedList();
        parameter.add(project);
        BugBaseQuery query = createQuery(BugBaseQuery.QUERY_PACKAGES, parameter);
        BugBaseQueryResult result = doQuery(query);
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw (new I18NException(new Message("exception.failed.packages.get"), (I18NException) result.getResult()));
        } else {
            return ((LinkedList) result.getResult());
        }
    }

    /**
     * Set the list of packages for a given project
     *
     * @param project project name
     * @param packages list of package names
     * @exception I18NException if an error occurs
     */
    public void setPackages(String project, LinkedList packages) throws I18NException {
        LinkedList parameter = new LinkedList();
        parameter.add(project);
        parameter.add(packages);
        BugBaseQuery query = createQuery(BugBaseQuery.QUERY_PACKAGES, parameter);
        BugBaseQueryResult result = doQuery(query);
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw (new I18NException(new Message("exception.failed.packages.set"), (I18NException) result.getResult()));
        }
    }

    /**
     * Retrieve the list of users belonging to the 'doctors' group
     *
     * @return list of users belonging to the 'doctors' group
     * @exception I18NException if an error occurs
     */
    public LinkedList getDoctors() throws I18NException {
        BugBaseQuery query = createQuery(BugBaseQuery.QUERY_DOCTORS, null);
        BugBaseQueryResult result = doQuery(query);
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw (new I18NException(new Message("exception.failed.doctors.get"), (I18NException) result.getResult()));
        } else {
            return ((LinkedList) result.getResult());
        }
    }

    /**
     * Update the given bug report. if sendEmail is true, the 
     * bug reporter will be notified via email about the changes.
     *
     * @param user user modifying the bug reports
     * @param reports list of bug reports to be updated
     * @param sendEmail should an email be sent?
     * @exception I18NException if an error occurs
     */
    public void updateBugReports(User user, LinkedList reports, Boolean sendEmail) throws I18NException {
        LinkedList parameter = new LinkedList();
        parameter.add(user);
        parameter.add(reports);
        parameter.add(sendEmail);
        BugBaseQuery query = createQuery(BugBaseQuery.QUERY_UPDATE_BUGREPORTS, parameter);
        BugBaseQueryResult result = doQuery(query);
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw (new I18NException(new Message("exception.failed.bugreport.update"), (I18NException) result.getResult()));
        }
    }

    /**
     * Update the given bug report.
     *
     * @param user user modifying the bug reports
     * @param report bug report to be updated
     * @exception I18NException if an error occurs
     */
    public void updateBugReport(User user, BugReport report) throws I18NException {
        LinkedList parameter = new LinkedList();
        parameter.add(user);
        parameter.add(report);
        BugBaseQuery query = createQuery(BugBaseQuery.QUERY_UPDATE_BUGREPORT, parameter);
        BugBaseQueryResult result = doQuery(query);
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw (new I18NException(new Message("exception.failed.bugreport.update"), (I18NException) result.getResult()));
        }
    }

    /**
     * Execute a database query.
     *
     * @param command SQL command
     * @return result set returned by the SQL query
     * @exception I18NException if an error occurs
     */
    public LinkedList executeQuery(String command) throws I18NException {
        LinkedList parameter = new LinkedList();
        parameter.add(command);
        BugBaseQuery query = createQuery(BugBaseQuery.QUERY_EXECUTE, parameter);
        BugBaseQueryResult result = doQuery(query);
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw (new I18NException(new Message("exception.failed.query.execute"), (I18NException) result.getResult()));
        } else {
            return ((LinkedList) result.getResult());
        }
    }

    /**
     * Retrieve the versions of the 
     * {@link openfuture.bugbase.servlet.BugBaseServlet BugBaseServlet},
     * {@link openfuture.bugbase.domain.Persistency persistency} and the
     * available versions from the
     *  {@link openfuture.bugbase.domain.VersionManager version manager}.
     *
     * @return version informations
     * @exception I18NException if an error occurs
     */
    public QueryResultVersions getVersions() throws I18NException {
        BugBaseQuery query = createQuery(BugBaseQuery.QUERY_VERSIONS, null);
        BugBaseQueryResult result = doQuery(query);
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw (new I18NException(new Message("exception.failed.versions.get"), (I18NException) result.getResult()));
        } else {
            return ((QueryResultVersions) result.getResult());
        }
    }

    /**
     * Retrieve the list of available updates.
     *
     * @return list of available updates
     * @exception I18NException if an error occurs
     */
    public LinkedList getAvailableUpdates() throws I18NException {
        BugBaseQuery query = createQuery(BugBaseQuery.QUERY_AVAILABLE_UPDATES, null);
        BugBaseQueryResult result = doQuery(query);
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw (new I18NException(new Message("exception.failed.updates.get"), (I18NException) result.getResult()));
        } else {
            return ((LinkedList) result.getResult());
        }
    }

    /**
     * Update the persistency to a given version.
     *
     * @param version new persistency version
     * @return result of this transaction
     * @exception I18NException if an error occurs
     */
    public TransactionResult updatePersistency(Version version) throws I18NException {
        LinkedList parameter = new LinkedList();
        parameter.add(version);
        BugBaseQuery query = createQuery(BugBaseQuery.QUERY_UPDATE, parameter);
        BugBaseQueryResult result = doQuery(query);
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw (new I18NException(new Message("exception.failed.persistency.update"), (I18NException) result.getResult()));
        } else {
            return ((TransactionResult) result.getResult());
        }
    }

    /**
     * Retrieve the list of user data. This method may
     * only be used by admin users.
     *
     * @return the list of user data
     * @exception I18NException if an error occurs
     */
    public LinkedList getUserData() throws I18NException {
        BugBaseQuery query = createQuery(BugBaseQuery.QUERY_USERDATA, null);
        BugBaseQueryResult result = doQuery(query);
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw (new I18NException(new Message("exception.failed.user.data"), (I18NException) result.getResult()));
        } else {
            return ((LinkedList) result.getResult());
        }
    }

    /**
     * Retrieve the list of user data except for their passwords.
     * This method may be executed by ordinary users.
     *
     * @return the list of user data
     * @exception I18NException if an error occurs
     */
    public LinkedList getUserList() throws I18NException {
        BugBaseQuery query = createQuery(BugBaseQuery.QUERY_USERLIST, null);
        BugBaseQueryResult result = doQuery(query);
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw (new I18NException(new Message("exception.failed.user.data"), (I18NException) result.getResult()));
        } else {
            return ((LinkedList) result.getResult());
        }
    }

    /**
     * Retrieve the list of all group IDs
     *
     * @return the list of all group IDs
     * @exception I18NException if an error occurs
     */
    public LinkedList getGroups() throws I18NException {
        BugBaseQuery query = createQuery(BugBaseQuery.QUERY_GROUPDATA, null);
        BugBaseQueryResult result = doQuery(query);
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw (new I18NException(new Message("exception.failed.groups.get"), (I18NException) result.getResult()));
        } else {
            return ((LinkedList) result.getResult());
        }
    }

    /**
     * Save user data. Update only dirty users. The users from deleted 
     * are deleted.
     *
     * @param users list of users to be updated
     * @param deleted list of users to be deleted
     * @exception I18NException if an error occurs
     * @see openfuture.bugbase.domain.User
     */
    public void saveUserData(LinkedList users, LinkedList deleted) throws I18NException {
        LinkedList parameter = new LinkedList();
        parameter.add(users);
        parameter.add(deleted);
        BugBaseQueryResult result = (BugBaseQueryResult) doQuery(createQuery(BugBaseQuery.QUERY_SAVE_USERDATA, parameter));
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw (new I18NException(new Message("exception.failed.users.save"), (I18NException) result.getResult()));
        } else {
            if (users != null) {
                Iterator it = users.iterator();
                while (it.hasNext()) {
                    User user = (User) it.next();
                    user.setClean();
                }
            }
        }
    }

    /**
     * Save user data. Update only dirty users. The users from deleted 
     * are deleted.
     *
     * @param users list of users to be updated
     * @param deleted list of users to be deleted
     * @exception I18NException if an error occurs
     * @see openfuture.bugbase.domain.User
     */
    public void registerUser(User user) throws I18NException {
        LinkedList parameter = new LinkedList();
        parameter.add(user);
        BugBaseQueryResult result = (BugBaseQueryResult) doQuery(createQuery(BugBaseQuery.QUERY_REGISTER_USER, parameter));
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            if (result.getResult() instanceof I18NException) {
                throw (new I18NException(new Message("exception.failed.users.save"), (I18NException) result.getResult()));
            } else {
                Message[] reasons = { new Message(((Exception) result.getResult()).getMessage()) };
                throw (new I18NException(new Message("exception.failed.users.save"), reasons));
            }
        } else {
            user.setClean();
        }
    }

    /**
     * Test the connection using a question to the  
     * {@link openfuture.bugbase.servlet.BugBaseServlet BugBaseServlet}.
     *
     * @param question the question to be posed
     * @return the answer
     * @exception I18NException if an error occurs
     */
    public String test(String question) throws I18NException {
        LinkedList parameter = new LinkedList();
        parameter.add(question);
        BugBaseQuery query = createQuery(BugBaseQuery.QUERY_TEST, parameter);
        BugBaseQueryResult result = doQuery(query);
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw (new I18NException(new Message("exception.failed.test"), (I18NException) result.getResult()));
        } else {
            return ((String) result.getResult());
        }
    }

    /**
     * Retrieve the list of doctors and project packages
     *
     * @param project project name
     * @return masterdata for the given project
     * @exception I18NException if an error occurs
     */
    public QueryResultMasterData getMasterData(String project) throws I18NException {
        LinkedList parameter = new LinkedList();
        parameter.add(project);
        BugBaseQuery query = createQuery(BugBaseQuery.QUERY_MASTERDATA, parameter);
        BugBaseQueryResult result = doQuery(query);
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw (new I18NException(new Message("exception.failed.masterdata.get"), (I18NException) result.getResult()));
        } else {
            return ((QueryResultMasterData) result.getResult());
        }
    }

    /**
     * Tries to establish a new connection to the database.
     *
     * @param login user data
     * @return user data, if succeeded, null otherwise.
     * @exception I18NException if an error occurs
     */
    public User login(Login login) throws I18NException {
        LinkedList parameter = new LinkedList();
        parameter.add(login);
        BugBaseQueryResult result = (BugBaseQueryResult) doQuery(createQuery(BugBaseQuery.QUERY_LOGIN, parameter));
        if (result.getQueryType() == BugBaseQueryResult.QR_OK) {
            return ((User) result.getResult());
        } else {
            throw (new I18NException(new Message("exception.failed.login"), (Message[]) null));
        }
    }

    /**
     * Tries to establish a new connection to the database 
     * as administrator.
     *
     * @param login user data
     * @return user data, if succeeded, null otherwise.
     * @exception I18NException if an error occurs
     */
    public User adminLogin(Login login) throws I18NException {
        LinkedList parameter = new LinkedList();
        parameter.add(login);
        BugBaseQueryResult result = (BugBaseQueryResult) doQuery(createQuery(BugBaseQuery.QUERY_ADMIN_LOGIN, parameter));
        if (result.getQueryType() == BugBaseQueryResult.QR_OK) {
            return ((User) result.getResult());
        } else {
            throw (new I18NException(new Message("exception.failed.login")));
        }
    }

    /**
     * Tries to establish a new connection to the database
     * as doctor.
     *
     * @param login user data
     * @return user data, if succeeded, null otherwise.
     * @exception I18NException if an error occurs
     */
    public User doctorLogin(Login login) throws I18NException {
        LinkedList parameter = new LinkedList();
        parameter.add(login);
        BugBaseQueryResult result = (BugBaseQueryResult) doQuery(createQuery(BugBaseQuery.QUERY_DOCTOR_LOGIN, parameter));
        if (result.getQueryType() == BugBaseQueryResult.QR_OK) {
            return ((User) result.getResult());
        } else {
            throw (new I18NException(new Message("exception.failed.login")));
        }
    }

    /**
     * Check, if the user belongs to the group 'doctors'.
     *
     * @param user user data
     * @return true, if the user is an bugbase doctor
     */
    public boolean validDoctorUser(User user) {
        if (user == null) return false;
        LinkedList groups = user.getGroupList();
        if (groups == null) return false; else return (groups.contains("doctors"));
    }

    /**
     * Change the current users password.
     *
     * @param login current login information
     * @param newPassword new password
     * @exception I18NException if an error occurs
     */
    public void changePassword(Login login, String newPassword) throws I18NException {
        LinkedList parameter = new LinkedList();
        parameter.add(login);
        parameter.add(newPassword);
        BugBaseQueryResult result = (BugBaseQueryResult) doQuery(createQuery(BugBaseQuery.QUERY_CHANGE_PASSWORD, parameter));
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw (new I18NException(new Message("exception.failed.password.change"), (I18NException) result.getResult()));
        }
    }

    /**
     * Add an attachment to a given bug report
     *
     * @param reportID the ID of the bug report
     * @param attachmentPath relative path where the attachment file
     * is located.
     * @exception I18NException if an error occurs
     */
    public void addAttachment(Integer reportID, String attachmentPath) throws I18NException {
        LinkedList parameter = new LinkedList();
        parameter.add(reportID);
        parameter.add(attachmentPath);
        BugBaseQueryResult result = (BugBaseQueryResult) doQuery(createQuery(BugBaseQuery.QUERY_ADD_ATTACHMENT, parameter));
        if (result.getQueryType() == BugBaseQueryResult.QR_FAILED) {
            throw (new I18NException(new Message("exception.failed.attachment.add"), (I18NException) result.getResult()));
        }
    }

    /**
     * Retrieve the status information from the
     * {@link openfuture.bugbase.servlet.BugBaseServlet BugBaseServlet}.
     *
     * @return a <code>Hashtable</code> holding the information
     * as key/value pair.
     * @exception I18NException if an error occurs
     */
    public Hashtable getStatus() throws I18NException {
        BugBaseQueryResult result = (BugBaseQueryResult) doQuery(createQuery(BugBaseQuery.QUERY_STATUS, null));
        if (result.getQueryType() == BugBaseQueryResult.QR_OK) {
            return ((Hashtable) result.getResult());
        } else {
            throw (new I18NException(new Message("exception.failed.status.check"), (I18NException) result.getResult()));
        }
    }

    /**
     * Execute a query and set the session ID obtained by
     * the query result.
     *
     * @param query a <code>BugBaseQuery</code> value
     * @return result of the query
     * @exception I18NException if an error occurs
     */
    private BugBaseQueryResult doQuery(BugBaseQuery query) throws I18NException {
        BugBaseQueryResult result = (BugBaseQueryResult) service(query);
        if (result != null) {
            setSessionID(result.getSessionID());
        }
        return (result);
    }

    /**
     * Creates a new query instance and sets its session ID.
     *
     * @param queryType query type
     * @param parameter query parameter
     * @return new instance
     * @see openfuture.bugbase.xobjects.BugBaseQuery
     */
    private BugBaseQuery createQuery(int queryType, LinkedList parameter) {
        BugBaseQuery query = new BugBaseQuery(queryType, parameter);
        query.setSessionID(getSessionID());
        return query;
    }

    /**
     * Execute a query over a HTTP connection. Both the query and
     * the query result are serialized.
     *
     * @param request query object
     * @return query result object
     * @exception I18NException if an error occurs
     */
    private Serializable service(Serializable request) throws I18NException {
        URLConnection urlc = null;
        try {
            urlc = url.openConnection();
            urlc.setDoInput(true);
            urlc.setDoOutput(true);
            ObjectOutputStream out = new ObjectOutputStream(urlc.getOutputStream());
            out.writeObject(request);
            out.close();
            ObjectInputStream in = new ObjectInputStream(urlc.getInputStream());
            return ((Serializable) in.readObject());
        } catch (IOException exp) {
            Message[] reasons = new Message[1];
            reasons[0] = new Message(exp.getMessage());
            throw new I18NException(new Message("exception.request.failed"), reasons);
        } catch (ClassNotFoundException exp) {
            Message[] reasons = new Message[1];
            reasons[0] = new Message(exp.getMessage());
            throw new I18NException(new Message("exception.request.failed"), reasons);
        }
    }

    /**
     * Get the value of sessionID.
     * @return Value of sessionID.
     */
    public Integer getSessionID() {
        return sessionID;
    }

    /**
     * Set the value of sessionID, if either the current session ID
     * is null or the given value is not null.
     * @param v  Value to assign to sessionID.
     */
    public void setSessionID(Integer v) {
        if (this.sessionID == null || v != null) {
            this.sessionID = v;
        }
    }
}
