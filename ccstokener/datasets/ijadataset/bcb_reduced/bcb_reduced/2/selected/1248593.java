package de.offis.semanticmm4u.user_profiles_connector.uri;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.TreeSet;
import component_interfaces.semanticmm4u.realization.user_profile_connector.provided.IUserModelNode;
import component_interfaces.semanticmm4u.realization.user_profile_connector.provided.IUserProfile;
import de.offis.semanticmm4u.failures.user_profiles_connectors.MM4UCannotCloseUserProfilesConnectionException;
import de.offis.semanticmm4u.failures.user_profiles_connectors.MM4UCannotDeleteUserProfileException;
import de.offis.semanticmm4u.failures.user_profiles_connectors.MM4UCannotExportUserProfileException;
import de.offis.semanticmm4u.failures.user_profiles_connectors.MM4UCannotExtendUserModelException;
import de.offis.semanticmm4u.failures.user_profiles_connectors.MM4UCannotImportUserProfileException;
import de.offis.semanticmm4u.failures.user_profiles_connectors.MM4UCannotOpenUserProfileConnectionException;
import de.offis.semanticmm4u.failures.user_profiles_connectors.MM4UCannotRemoveUserModelExtensionException;
import de.offis.semanticmm4u.failures.user_profiles_connectors.MM4UCannotStoreUserProfileException;
import de.offis.semanticmm4u.failures.user_profiles_connectors.MM4UUserProfileNotFoundException;
import de.offis.semanticmm4u.global.Debug;
import de.offis.semanticmm4u.global.Property;
import de.offis.semanticmm4u.global.PropertyList;
import de.offis.semanticmm4u.user_profiles_connector.SimpleUserProfile;
import de.offis.semanticmm4u.user_profiles_connector.UserProfileAccessorToolkit;

public class URIUserProfileConnectorFactory extends UserProfileAccessorToolkit {

    protected static final String FILE_SUFFIX = ".profile";

    private String profileURI = null;

    public URIUserProfileConnectorFactory(URIUserProfileConnectorLocator myLocatorObject) {
        this.profileURI = myLocatorObject.getStringValue(URIUserProfileConnectorLocator.PROFILE_URI);
    }

    public IUserProfile getUserProfile(String profileID) throws MM4UUserProfileNotFoundException {
        SimpleUserProfile tempProfile = null;
        String tempProfileString = this.profileURI + profileID + FILE_SUFFIX;
        try {
            URL url = new URL(tempProfileString);
            Debug.println("Retrieve profile with ID: " + url);
            tempProfile = new SimpleUserProfile();
            BufferedReader input = new BufferedReader(new InputStreamReader(url.openStream()));
            String tempLine = null;
            tempProfile.add("id", profileID);
            while ((tempLine = input.readLine()) != null) {
                Property tempProperty = PropertyList.splitStringIntoKeyAndValue(tempLine);
                if (tempProperty != null) {
                    tempProfile.addIfNotNull(tempProperty.getKey(), tempProperty.getValue());
                }
            }
            input.close();
        } catch (MalformedURLException exception) {
            throw new MM4UUserProfileNotFoundException(this, "getProfile", "Profile '" + tempProfileString + "' not found.");
        } catch (IOException exception) {
            throw new MM4UUserProfileNotFoundException(this, "getProfile", "Profile '" + tempProfileString + "' not found.");
        }
        return tempProfile;
    }

    public void setUserProfile(IUserProfile profile) throws MM4UCannotStoreUserProfileException {
        this.setProfile("", (SimpleUserProfile) profile);
    }

    private void setProfile(String loginName, SimpleUserProfile profile) throws MM4UCannotStoreUserProfileException {
        try {
            OutputStream outStream = null;
            URL url = new URL(this.profileURI + profile.getID() + FILE_SUFFIX);
            if (url.getProtocol().equals("file")) {
                File file = new File(url.getFile());
                outStream = new FileOutputStream(file);
            } else {
                URLConnection connection = url.openConnection();
                connection.setDoOutput(true);
                outStream = connection.getOutputStream();
            }
            OutputStreamWriter writer = new OutputStreamWriter(outStream);
            Enumeration myEnum = profile.keys();
            while (myEnum.hasMoreElements()) {
                String key = myEnum.nextElement().toString();
                if (key != "id") writer.write(key + "=" + profile.getStringValue(key) + System.getProperty("line.separator"));
            }
            writer.flush();
            writer.close();
        } catch (Exception e) {
            throw new MM4UCannotStoreUserProfileException(this, "setProfile", e.toString());
        }
    }

    public void openConnection() throws MM4UCannotOpenUserProfileConnectionException {
    }

    public void closeConnection() throws MM4UCannotCloseUserProfilesConnectionException {
    }

    public IUserProfile createUserProfile(String ID) {
        throw new RuntimeException("(TODO) not implemented!");
    }

    public void deleteUserProfile(String id) throws MM4UCannotDeleteUserProfileException {
        throw new RuntimeException("(TODO) not implemented!");
    }

    public void exportUserProfile(String format, String id, String outputID) throws MM4UCannotExportUserProfileException {
        throw new RuntimeException("(TODO) not implemented!");
    }

    public IUserProfile importUserProfile(String externalProfile, String format) throws MM4UCannotImportUserProfileException {
        throw new RuntimeException("(TODO) not implemented!");
    }

    public void extendUserModel(IUserModelNode nodeToExtend, String type, String name) throws MM4UCannotExtendUserModelException {
        throw new RuntimeException("(TODO) not implemented!");
    }

    public void removeUserModelExtension(IUserModelNode extensionNode) throws MM4UCannotRemoveUserModelExtensionException {
        throw new RuntimeException("(TODO) not implemented!");
    }

    public TreeSet getRemovableUserModelNodes() {
        throw new RuntimeException("(TODO) not implemented!");
    }

    public IUserModelNode getUserModelRoot() {
        throw new RuntimeException("(TODO) not implemented!");
    }

    public TreeSet getUserModelNodesTypes() {
        throw new RuntimeException("(TODO) not implemented!");
    }

    public boolean existUserProfile(String ID) {
        return true;
    }
}
