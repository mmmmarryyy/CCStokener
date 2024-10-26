package org.dcm4chee.web.war;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Principal;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.security.auth.Subject;
import javax.servlet.http.Cookie;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebRequest;
import org.dcm4chee.web.common.base.BaseWicketApplication;
import org.dcm4chee.web.war.common.model.AbstractDicomModel;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;
import org.dcm4chee.web.war.folder.model.StudyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision: 16180 $ $Date: 2011-11-02 12:06:39 -0400 (Wed, 02 Nov 2011) $
 * @since Dec 20, 2010
 */
public class StudyPermissionHelper implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static Logger log = LoggerFactory.getLogger(StudyPermissionHelper.class);

    private String newline = System.getProperty("line.separator");

    private List<String> dicomRoles = new ArrayList<String>();

    private StudyPermissionRight studyPermissionRight = StudyPermissionRight.NONE;

    private boolean manageStudyPermissions;

    private boolean useStudyPermissions;

    private boolean allowPropagation;

    private boolean ignoreEditTimeLimit;

    private boolean SSO = false;

    public static StudyPermissionHelper get() {
        return ((AuthenticatedWebSession) AuthenticatedWebSession.get()).getStudyPermissionHelper();
    }

    public StudyPermissionHelper(org.apache.wicket.security.hive.authentication.Subject webSubject) {
        setStudyPermissionParameters();
        setStudyPermissionRight(webSubject);
        setIgnoreEditTimeLimit(webSubject);
        SSO = true;
    }

    public StudyPermissionHelper(String username, String password, org.apache.wicket.security.hive.authentication.Subject webSubject) throws AttributeNotFoundException, InstanceNotFoundException, MalformedObjectNameException, MBeanException, ReflectionException, NullPointerException, IOException {
        setStudyPermissionParameters();
        setStudyPermissionRight(webSubject);
        try {
            javax.security.auth.Subject dicomSubject = new javax.security.auth.Subject();
            WebCfgDelegate.getInstance().getMBeanServer().invoke(new ObjectName(((BaseWicketApplication) RequestCycle.get().getApplication()).getInitParameter("DicomSecurityService")), "isValid", new Object[] { username, password, dicomSubject }, new String[] { String.class.getName(), String.class.getName(), javax.security.auth.Subject.class.getName() });
            setDicomRoles(dicomSubject);
        } catch (Exception e) {
            log.error("Error creating StudyPermissionHelper: revoked rights: ", e);
        }
    }

    public void doDicomAuthentication() {
        if (!manageStudyPermissions && !useStudyPermissions) return;
        HttpURLConnection urlConnection = null;
        InputStream in = null;
        try {
            urlConnection = (HttpURLConnection) new URL(WebCfgDelegate.getInstance().getDicomSecurityServletUrl()).openConnection();
            WebRequest webRequest = ((WebRequest) RequestCycle.get().getRequest());
            Cookie[] cookies = webRequest.getCookies();
            if (cookies == null) return;
            StringBuffer cookieValue = new StringBuffer();
            for (Cookie cookie : Arrays.asList(cookies)) {
                cookieValue.append(cookie.getName()).append("=").append(cookie.getValue()).append("; ");
            }
            if (cookieValue.length() > 0) cookieValue.delete(cookieValue.length() - 2, cookieValue.length() - 1);
            urlConnection.setRequestProperty("Cookie", cookieValue.toString());
            urlConnection.connect();
            in = urlConnection.getInputStream();
            Reader reader = new InputStreamReader(in);
            char[] buffer = new char[1];
            StringBuffer returnValue = new StringBuffer();
            while ((reader.read(buffer)) >= 0) returnValue.append(buffer);
            reader.close();
            if (returnValue.length() > 0) setDicomRoles(returnValue.toString());
        } catch (IOException e) {
            log.error(getClass().getName() + ": ", e);
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException e) {
                log.error(getClass().getName() + ": Could not close input stream of url connection: ", e);
            }
            if (urlConnection != null) urlConnection.disconnect();
        }
    }

    private void setStudyPermissionParameters() {
        manageStudyPermissions = WebCfgDelegate.getInstance().getManageStudyPermissions();
        useStudyPermissions = WebCfgDelegate.getInstance().getUseStudyPermissions();
    }

    public boolean applyStudyPermissions() {
        return isUseStudyPermissions() && !getStudyPermissionRight().equals(StudyPermissionHelper.StudyPermissionRight.ALL);
    }

    private void setDicomRoles(Subject dicomSubject) {
        if (manageStudyPermissions || useStudyPermissions) {
            Principal rolesPrincipal;
            for (Iterator<Principal> i = dicomSubject.getPrincipals().iterator(); i.hasNext(); ) {
                rolesPrincipal = i.next();
                if (rolesPrincipal instanceof Group) {
                    Enumeration<? extends Principal> e = ((Group) rolesPrincipal).members();
                    while (e.hasMoreElements()) dicomRoles.add(e.nextElement().getName());
                }
            }
        }
    }

    private void setDicomRoles(String dicomRoleString) {
        if (manageStudyPermissions || useStudyPermissions) dicomRoles = Arrays.asList(dicomRoleString.split(newline));
    }

    public List<String> getDicomRoles() {
        return dicomRoles;
    }

    public boolean isManageStudyPermissions() {
        return manageStudyPermissions;
    }

    public void setUseStudyPermissions(boolean useStudyPermissions) {
        this.useStudyPermissions = useStudyPermissions;
    }

    public boolean isUseStudyPermissions() {
        return useStudyPermissions;
    }

    public void setStudyPermissionRight(StudyPermissionRight studyPermissionRight) {
        this.studyPermissionRight = studyPermissionRight;
    }

    public StudyPermissionRight getStudyPermissionRight() {
        return studyPermissionRight;
    }

    public boolean isPropagationAllowed() {
        return allowPropagation;
    }

    public boolean ignoreEditTimeLimit() {
        return ignoreEditTimeLimit;
    }

    public void setSSO(boolean sSO) {
        SSO = sSO;
    }

    public boolean isSSO() {
        return SSO;
    }

    public boolean checkPermission(Collection<? extends AbstractDicomModel> c, String action, boolean all) {
        if (!isUseStudyPermissions() || (dicomRoles == null)) return true;
        if (dicomRoles.size() == 0) return false;
        if (c == null || c.isEmpty()) return true;
        for (AbstractDicomModel m : c) {
            if (!checkStudyPermission(m, action)) {
                if (all) return false;
            } else {
                if (!all) return true;
            }
        }
        return all;
    }

    public boolean checkPermission(AbstractDicomModel m, String action) {
        if ((!isUseStudyPermissions()) || (dicomRoles == null)) return true;
        if (dicomRoles.size() == 0) return false;
        return checkStudyPermission(m, action);
    }

    private boolean checkStudyPermission(AbstractDicomModel m, String action) {
        if (m.levelOfModel() == AbstractDicomModel.PATIENT_LEVEL) return true;
        while (m.levelOfModel() > AbstractDicomModel.STUDY_LEVEL) m = m.getParent();
        return ((StudyModel) m).getStudyPermissionActions().contains(action);
    }

    private void setStudyPermissionRight(org.apache.wicket.security.hive.authentication.Subject webSubject) {
        studyPermissionRight = StudyPermissionRight.NONE;
        String studyPermissionsAll = BaseWicketApplication.get().getInitParameter("StudyPermissionsAllRolename");
        String studyPermissionsOwn = BaseWicketApplication.get().getInitParameter("StudyPermissionsOwnRolename");
        String studyPermissionsPropagation = BaseWicketApplication.get().getInitParameter("StudyPermissionsPropagationRolename");
        if (studyPermissionsAll != null || studyPermissionsOwn != null) {
            Iterator<org.apache.wicket.security.hive.authorization.Principal> i = webSubject.getPrincipals().iterator();
            while (i.hasNext()) {
                String rolename = i.next().getName();
                if (rolename.equals(studyPermissionsAll)) {
                    studyPermissionRight = StudyPermissionRight.ALL;
                    break;
                } else if (rolename.equals(studyPermissionsOwn)) {
                    studyPermissionRight = StudyPermissionRight.OWN;
                } else if (rolename.equals(studyPermissionsPropagation)) allowPropagation = true;
            }
        }
    }

    private void setIgnoreEditTimeLimit(org.apache.wicket.security.hive.authentication.Subject webSubject) {
        ignoreEditTimeLimit = false;
        String ignoreEditTimeLimitRolename = WebCfgDelegate.getInstance().getIgnoreEditTimeLimitRolename();
        Iterator<org.apache.wicket.security.hive.authorization.Principal> i = webSubject.getPrincipals().iterator();
        while (i.hasNext()) {
            String rolename = i.next().getName();
            if (rolename.equals(ignoreEditTimeLimitRolename)) {
                ignoreEditTimeLimit = true;
                break;
            }
        }
    }

    public enum StudyPermissionRight {

        NONE, OWN, ALL
    }
}
