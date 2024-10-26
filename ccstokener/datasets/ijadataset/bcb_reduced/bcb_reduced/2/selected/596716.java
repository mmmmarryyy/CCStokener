package org.actioncenters.servlets.initialization;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.StringTokenizer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.actioncenters.cometd.cache.user.UserCacheController;
import org.actioncenters.core.activitylog.ActivityInfo;
import org.actioncenters.core.contribution.data.ISystemRole;
import org.actioncenters.core.contribution.data.IUser;
import org.actioncenters.core.contribution.data.impl.SystemRole;
import org.actioncenters.core.contribution.svc.exception.ContributionServiceException;
import org.actioncenters.core.spring.ApplicationContextHelper;
import org.actioncenters.core.system.settings.ISystemSettingsService;
import org.actioncenters.core.usersecurity.ActionCentersPasswordEncoder;
import org.actioncenters.core.usersecurity.IUserManagementService;
import org.actioncenters.servlets.GetSystemScriptServlet;
import org.springframework.context.ApplicationContext;

/**
 * Initializes ActionCenters.
 *
 * @author dougk
 */
public class ActionCentersInitialization extends HttpServlet {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request, response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request, response);
    }

    /**
     * Handles requests.
     *
     * @param request
     *          the request object
     * @param response
     *          the response object
     */
    private void handleRequest(HttpServletRequest request, HttpServletResponse response) {
        uploadPlugins();
    }

    /** Auto-generated serial version ID. */
    private static final long serialVersionUID = 7602605908926079200L;

    /** Application Context. */
    private static final ApplicationContext AC = ApplicationContextHelper.getApplicationContext("actioncenters.xml");

    /** User management service. */
    private static final IUserManagementService USER_MGMT_SVC = (IUserManagementService) AC.getBean("userManagementService");

    /** The system settings service. */
    private static final ISystemSettingsService SYSTEM_SETTINGS_SVC = (ISystemSettingsService) AC.getBean("systemSettingsService");

    /**
     * Initialization of the servlet. <br>
     *
     * @throws ServletException
     *             if an error occurs
     */
    @Override
    public void init() throws ServletException {
        ActivityInfo.setActivityInfo("System", "StartupInit", null);
        if (USER_MGMT_SVC.getUserCount() == 0) {
            initializeUserAccounts();
        }
        String data = SYSTEM_SETTINGS_SVC.getElementMetadata();
        if (data == null) {
            System.out.println("System element configuration does not exist. " + "Uploading the configurations. Please wait...");
            uploadPlugins();
        }
    }

    /**
     * Upload plugins.
     */
    private void uploadPlugins() {
        try {
            File pluginDir = new File(getClass().getResource("/plugins").toURI());
            for (File plugin : pluginDir.listFiles()) {
                URL pluginUrl = plugin.toURI().toURL();
                System.out.println(pluginUrl.toString());
                uploadConfiguration(pluginUrl, UserCacheController.getUser("Admin"));
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ContributionServiceException e) {
            e.printStackTrace();
        }
    }

    /**
     * Upload configuration.
     *
     * @param url the URL path to plugin configuration file
     * @param iUser the i user
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ContributionServiceException the contribution service exception
     */
    private void uploadConfiguration(URL url, IUser iUser) throws IOException, ContributionServiceException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append(System.getProperty("line.separator"));
        }
        SYSTEM_SETTINGS_SVC.setElementMetadata(sb.toString(), iUser);
        GetSystemScriptServlet.resetScript();
    }

    /**
     * Create the default user roles and the default administrator user.
     */
    public void initializeUserAccounts() {
        createDefaultRoles();
        createDefaultAdminUser();
    }

    /**
     * Creates default system roles.
     */
    private void createDefaultRoles() {
        String roleNames = getInitParameter("Roles");
        StringTokenizer roleNameST = new StringTokenizer(roleNames, ",");
        while (roleNameST.hasMoreElements()) {
            createRole(roleNameST.nextToken());
        }
    }

    /**
     * Creates a system role.
     *
     * @param roleName
     *            the new role name.
     */
    private void createRole(String roleName) {
        ISystemRole systemRole = new SystemRole();
        systemRole.setName(roleName);
        USER_MGMT_SVC.addSystemRole(systemRole);
    }

    /**
     * Create the default admin user.
     */
    private void createDefaultAdminUser() {
        String users = getInitParameter("Users");
        StringTokenizer userST = new StringTokenizer(users, "/");
        while (userST.hasMoreElements()) {
            createUser(userST.nextToken());
        }
    }

    /**
     * Creates a user based on the specified initialization string.
     *
     * @param userString
     *            The name, username, password, system roles.
     */
    private void createUser(String userString) {
        ActionCentersPasswordEncoder passwordEncoder = (ActionCentersPasswordEncoder) AC.getBean("passwordEncoder");
        IUser user = new org.actioncenters.core.contribution.data.impl.User();
        StringTokenizer userAttributes = new StringTokenizer(userString, ",");
        user.setFirstName(userAttributes.nextToken());
        user.setLastName(userAttributes.nextToken());
        user.setUsername(userAttributes.nextToken());
        user.setPassword(passwordEncoder.encodePassword(userAttributes.nextToken()));
        user.setEmail(userAttributes.nextToken());
        user.setFromDate(new Timestamp(System.currentTimeMillis()));
        user.setSystemRoles(new ArrayList<ISystemRole>());
        String roles = userAttributes.nextToken();
        StringTokenizer roleST = new StringTokenizer(roles, "|");
        while (roleST.hasMoreElements()) {
            user.getSystemRoles().add(USER_MGMT_SVC.getSystemRole(roleST.nextToken()));
        }
        USER_MGMT_SVC.addUser(user);
    }
}
