package org.xaware.server.security;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

/**
 * A simple properties file based login module that consults two Java Properties formatted text files for username to
 * password("users.properties") and username to roles("roles.properties") mapping. The names of the properties files may
 * be overriden by the usersProperties and rolesProperties options. The properties files are loaded during
 * initialization.
 * 
 * The users.properties file uses a format: username1=password1 username2=password2 ...
 * 
 * to define all valid usernames and their corresponding passwords.
 * 
 * The roles.properties file uses a format: username1=role1,role2,... username2=role1,role3,...
 * 
 * to define the sets of roles for valid usernames.
 */
public class UsersRolesLoginModule extends UsernamePasswordLoginModule {

    /** The name of the properties resource containing user/passwords */
    private String usersRsrcName = "users.properties";

    /** The name of the properties resource containing user/roles */
    private String rolesRsrcName = "roles.properties";

    /** The users.properties values */
    private Properties users;

    /** The roles.properties values */
    private Properties roles;

    /**
     * Initialize this LoginModule.
     * 
     * @param options,
     *            the login module option map. Supported options include: usersProperties: The name of the properties
     *            resource containing user/passwords. The default is "users.properties" rolesProperties: The name of the
     *            properties resource containing user/roles The default is "roles.properties".
     */
    @Override
    public void initialize(final Subject subject, final CallbackHandler callbackHandler, final Map sharedState, final Map options) {
        super.initialize(subject, callbackHandler, sharedState, options);
        try {
            String option = (String) options.get("usersProperties");
            if (option != null) {
                usersRsrcName = option;
            }
            option = (String) options.get("rolesProperties");
            if (option != null) {
                rolesRsrcName = option;
            }
            loadUsers();
            loadRoles();
        } catch (final Exception e) {
        }
    }

    /**
     * Method to authenticate a Subject (phase 1). This validates that the users and roles properties files were loaded
     * and then calls super.login to perform the validation of the password.
     * 
     * @exception LoginException,
     *                thrown if the users or roles properties files were not found or the super.login method fails.
     */
    @Override
    public boolean login() throws LoginException {
        if (users == null) {
            throw new LoginException("Missing users.properties file.");
        }
        if (roles == null) {
            throw new LoginException("Missing roles.properties file.");
        }
        return super.login();
    }

    /**
     * Create the set of roles the user belongs to by parsing the roles.properties data for username=role1,role2,...
     * patterns.
     * 
     * @return Object[] containing the sets of roles
     */
    @Override
    protected Object[] getRoleSets() throws LoginException {
        final String targetUser = getUsername();
        final Enumeration users = roles.propertyNames();
        final ArrayList groups = new ArrayList();
        while (users.hasMoreElements() && targetUser != null) {
            final String user = (String) users.nextElement();
            if (user.equalsIgnoreCase(targetUser) == false) {
                continue;
            }
            final String value = roles.getProperty(user);
            parseGroupMembers(groups, value);
        }
        return groups.toArray();
    }

    @Override
    protected String getUsersPassword() {
        final String username = getUsername();
        String password = null;
        if (username != null) {
            password = users.getProperty(username, null);
        }
        return password;
    }

    private void parseGroupMembers(final ArrayList group, final String value) {
        final StringTokenizer tokenizer = new StringTokenizer(value, ",");
        while (tokenizer.hasMoreTokens()) {
            final String token = tokenizer.nextToken();
            final Principal p = new XAGroupPrincipal(token);
            group.add(p);
        }
    }

    private void loadUsers() throws IOException {
        users = loadProperties(usersRsrcName);
    }

    private void loadRoles() throws IOException {
        roles = loadProperties(rolesRsrcName);
    }

    /**
     * Loads the given properties file and returns a Properties object containing the key,value pairs in that file. The
     * properties files should be in the class path.
     */
    private Properties loadProperties(final String propertiesName) throws IOException {
        Properties bundle = null;
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final URL url = loader.getResource(propertiesName);
        if (url == null) {
            throw new IOException("Properties file " + propertiesName + " not found");
        }
        final InputStream is = url.openStream();
        if (is != null) {
            bundle = new Properties();
            bundle.load(is);
        } else {
            throw new IOException("Properties file " + propertiesName + " not avilable");
        }
        return bundle;
    }
}
