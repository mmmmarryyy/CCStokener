package org.cilogon.service.config;

import net.oauth.OAuth;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.cilogon.service.servlet.util.DBServiceException;
import org.cilogon.service.servlet.util.DBServiceSerializer;
import org.cilogon.service.storage.IdentityProvider;
import org.cilogon.service.storage.User;
import org.cilogon.service.storage.impl.postgres.IdentityProvidersTable;
import org.cilogon.service.util.CILogonServiceTransaction;
import org.cilogon.service.util.ServiceTestBase;
import org.junit.Test;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.cilogon.service.servlet.DBService.*;
import static org.cilogon.service.storage.impl.postgres.UsersTable.*;

/**
 * Periodically run this test. You must start up the service as indicated in the host string
 * below beforehand.
 * <p>Created by Jeff Gaynor<br>
 * on Nov 15, 2010 at  12:46:49 PM
 */
public class RemoteDBServiceTest extends ServiceTestBase {

    public static String host = "http://localhost:55555/delegation/dbService";

    public static boolean timingsOn = true;

    public static long totalTime = 0L;

    public static long totalCalls = 0L;

    public static long maxTime = 0L;

    public static long minTime = 0L;

    public DBServiceSerializer getSerializer() {
        if (serializer == null) {
            serializer = new DBServiceSerializer();
        }
        return serializer;
    }

    public void setSerializer(DBServiceSerializer serializer) {
        this.serializer = serializer;
    }

    DBServiceSerializer serializer;

    static HttpClient httpClient;

    protected HttpClient getHttpClient() {
        httpClient = new DefaultHttpClient();
        return httpClient;
    }

    protected HttpResponse doGet(String action, String[][] args) throws IOException {
        long startTime = System.currentTimeMillis();
        String getString = host + "?" + ACTION_PARAMETER + "=" + action;
        if (args != null && args.length != 0) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].length != 0) {
                    getString = getString + "&" + args[i][0] + "=" + encode(args[i][1]);
                }
            }
        }
        HttpGet httpGet = new HttpGet(getString);
        HttpResponse response = getHttpClient().execute(httpGet);
        if (timingsOn) {
            totalCalls++;
            long elapsedTime = (System.currentTimeMillis() - startTime);
            if (totalCalls != 1) {
                totalTime = totalTime + elapsedTime;
                minTime = Math.min(minTime, elapsedTime);
                maxTime = Math.max(maxTime, elapsedTime);
            } else {
                minTime = elapsedTime;
                maxTime = 0L;
            }
            System.out.println("http get took " + elapsedTime + " ms., (calls, min, max, av) = (" + totalCalls + ", " + minTime + ", " + maxTime + ", " + (totalTime / totalCalls) + ")");
        }
        return response;
    }

    public static String encode(String x) throws UnsupportedEncodingException {
        return URLEncoder.encode(x, UTF8_ENCODING);
    }

    public static String decode(String x) throws UnsupportedEncodingException {
        return URLDecoder.decode(x, UTF8_ENCODING);
    }

    @Test
    public void testGetUserID() throws Exception {
        User user = newUser();
        assert user.getUid().equals(parseUserID(doGet(GET_USER_ID, new String[][] { { remoteUserColumn, user.getRemoteUser() }, { idpColumn, user.getIdP().toString() } })));
    }

    @Test
    public void testCIL11() throws Exception {
        User user = newUser();
        for (int i = 0; i < count; i++) {
            assert getReturnStatus(doGet(GET_USER, new String[][] { { userIDColumn, user.getUid().toString() } })) == STATUS_OK;
        }
        user.setEmail(null);
        user.setFirstName(null);
        user.setLastName(null);
        user.setIDPName(null);
        getArchivedUserStore().archiveUser(user.getUid());
        getUserStore().update(user);
        System.out.println("Updated user = " + user.getUid());
        for (int i = 0; i < count; i++) {
            assert getReturnStatus(doGet(GET_USER, new String[][] { { remoteUserColumn, user.getRemoteUser() }, { idpColumn, user.getIdP() } })) == STATUS_OK;
        }
    }

    @Test
    public void testGetNewUser() throws Exception {
        String x = "remote-user-" + getRandomString();
        User user = parseUser(doGet(GET_USER, new String[][] { { remoteUserColumn, x }, { idpColumn, "test-idp-" + getRandomString() } }));
        assert responseOk(doGet(HAS_USER, new String[][] { { remoteUserColumn, user.getRemoteUser() }, { idpColumn, user.getRemoteUser() } }));
        User user2 = getUserStore().get(user.getUid());
        assert user.equals(user2);
    }

    @Test
    public void testUpdateUser() throws Exception {
        User user = newUser();
        String firstName = "Giovanni";
        String lastName = "Arnolfini";
        String email = "guido@medici.com";
        String[][] argList = new String[][] { { remoteUserColumn, user.getRemoteUser() }, { idpColumn, user.getIdP() }, { idpDisplayNameColumn, user.getIDPName() }, { firstNameColumn, firstName }, { lastNameColumn, lastName }, { emailColumn, email } };
        User user2 = parseUser(doGet(UPDATE_USER, argList));
        assert user2.getFirstName().equals(firstName);
        assert user2.getLastName().equals(lastName);
        assert user2.getEmail().equals(email);
        User user3 = parseUser(doGet(GET_LAST_ARCHIVED_USER, new String[][] { { userIDColumn, user.getUid().toString() } }));
        assert user.equals(user3);
    }

    @Test
    public void testHasUser() throws Exception {
        HttpResponse response = doGet(HAS_USER, new String[][] { { userIDColumn, "foo" } });
        assert getSerializer().readResponseOnly(response.getEntity().getContent()) == STATUS_USER_NOT_FOUND;
        response = doGet(HAS_USER, new String[][] { { remoteUserColumn, "foo" }, { idpColumn, "bar" } });
        assert getSerializer().readResponseOnly(response.getEntity().getContent()) == STATUS_USER_NOT_FOUND;
        User user = newUser();
        response = doGet(HAS_USER, new String[][] { { userIDColumn, user.getUid().toString() } });
        assert getSerializer().readResponseOnly(response.getEntity().getContent()) == STATUS_USER_EXISTS;
        response = doGet(HAS_USER, new String[][] { { remoteUserColumn, user.getRemoteUser() }, { idpColumn, user.getIdP() } });
        int x = getSerializer().readResponseOnly(response.getEntity().getContent());
        assert x == STATUS_USER_EXISTS;
    }

    @Test
    public void testErrorConditions() throws Exception {
        try {
            parseUser(doGet("foo", new String[][] { { remoteUserColumn, "remoteUser" }, { idpColumn, "idp" } }));
            assert false : "A bad action was specified, but the server did not process this correctly";
        } catch (DBServiceException x) {
            assert x.checkMessage(STATUS_ACTION_NOT_FOUND);
        }
        try {
            parseUser(doGet(GET_USER, new String[][] { { ACTION_PARAMETER, HAS_USER } }));
        } catch (DBServiceException x) {
            assert x.checkMessage(STATUS_DUPLICATE_ARGUMENT);
        }
        try {
            parseUser(doGet(GET_USER, new String[][] { {} }));
            assert false : "Missing parameter not caught";
        } catch (DBServiceException x) {
            assert x.checkMessage(STATUS_MISSING_ARGUMENT);
        }
        try {
            parseUser(doGet(GET_USER, new String[][] { { "user_uid", "useruid" }, { "user_uid", "another user uid" } }));
            assert false : "Duplicate parameter not caught";
        } catch (DBServiceException x) {
            assert x.checkMessage(STATUS_DUPLICATE_ARGUMENT);
        }
    }

    @Test
    public void testGetUser() throws Exception {
        User user = newUser();
        assert user.equals(parseUser(doGet(GET_USER, new String[][] { { userIDColumn, user.getUid().toString() } })));
        assert user.equals(parseUser(doGet(GET_USER, new String[][] { { remoteUserColumn, user.getRemoteUser() }, { idpColumn, user.getIdP().toString() }, { idpDisplayNameColumn, user.getIDPName() }, { firstNameColumn, user.getFirstName() }, { lastNameColumn, user.getLastName() }, { emailColumn, user.getEmail() } })));
        User updatedUser = parseUser(doGet(GET_USER, new String[][] { { remoteUserColumn, user.getRemoteUser() }, { idpColumn, user.getIdP().toString() }, { idpDisplayNameColumn, user.getIDPName() + "foo1" }, { firstNameColumn, user.getFirstName() + "foo2" }, { lastNameColumn, user.getLastName() + "foo3" }, { emailColumn, user.getEmail() + "foo4" } }));
        user = getUserStore().get(user.getUid());
        assert user.equals(updatedUser);
    }

    @Test
    public void testCreateUser() throws Exception {
        User user = parseUser(doGet(CREATE_USER, new String[][] { { remoteUserColumn, "test-user-" + getRandomString() }, { idpColumn, "test-idp-" + getRandomString() } }));
        User user2 = getUserStore().get(user.getUid());
        assert user.equals(user2);
    }

    /**
     * This test shows that the API for getting an archived user works.
     *
     * @throws Exception
     */
    @Test
    public void testArchivedUser() throws Exception {
        User user = newUser();
        responseOk(doGet(GET_LAST_ARCHIVED_USER, new String[][] { { userIDColumn, user.getUid().toString() } }));
        User user2 = parseUser(doGet(GET_USER, new String[][] { { userIDColumn, user.getUid().toString() } }));
        user.setFirstName("Aethelred");
        user.setLastName("Cerdicing");
        getArchivedUserStore().archiveUser(user.getUid());
        getUserStore().update(user);
        assert user2.equals(parseUser(doGet(GET_LAST_ARCHIVED_USER, new String[][] { { userIDColumn, user.getUid().toString() } })));
        try {
            responseOk(doGet(GET_LAST_ARCHIVED_USER, new String[][] { { userIDColumn, "fake:user:id:123" } }));
        } catch (DBServiceException x) {
            assert x.checkMessage(STATUS_USER_NOT_FOUND_ERROR);
        }
    }

    @Test
    public void testPortalParameter() throws Exception {
        String r = getRandomString(16);
        CILogonServiceTransaction t = new CILogonServiceTransaction();
        t.setAuthorizationGrant(getServiceEnvironment().getTokenFactory().newAuthorizationGrant());
        t.setPortalName("Test DBService portal name");
        t.setCallback(createToken("callbackURI"));
        t.setSuccess(createToken("successURI"));
        t.setFailure(createToken("failureURI"));
        getTransactionStore().save(t);
        Map<String, String> t2;
        t2 = parseTransaction(doGet(GET_PORTAL_PARAMETER, new String[][] { { OAuth.OAUTH_TOKEN, t.getAuthorizationGrant().getToken() } }));
        assert t2.get(CILOGON_CALLBACK_URI).equals(t.getCallback().toString());
        assert t2.get(CILOGON_FAILURE_URI).equals(t.getFailure().toString());
        assert t2.get(CILOGON_SUCCESS_URI).equals(t.getSuccess().toString());
        assert t2.get(OAuth.OAUTH_TOKEN).equals(t.getAuthorizationGrant().getToken());
        assert t2.get(CILOGON_PORTAL_NAME).equals(t.getPortalName());
        try {
            parseTransaction(doGet(GET_PORTAL_PARAMETER, new String[][] { { OAuth.OAUTH_TOKEN, t.getAuthorizationGrant().getToken() }, { OAuth.OAUTH_TOKEN, t.getAuthorizationGrant().getToken() } }));
            assert false : "duplicate temp cred should fail to work";
        } catch (DBServiceException x) {
            assert x.checkMessage(STATUS_DUPLICATE_ARGUMENT);
        }
        try {
            parseTransaction(doGet(GET_PORTAL_PARAMETER, new String[][] { { OAuth.OAUTH_TOKEN, "foo" } }));
            assert false : "bad identifier should result in no transaction being found";
        } catch (DBServiceException x) {
            assert x.checkMessage(STATUS_TRANSACTION_NOT_FOUND);
        }
        try {
            parseTransaction(doGet(GET_PORTAL_PARAMETER, new String[][] { {} }));
            assert false : "Missing parameter";
        } catch (DBServiceException x) {
            assert x.checkMessage(STATUS_MISSING_ARGUMENT);
        }
    }

    @Test
    public void testRemoveUser() throws Exception {
        User user = newUser();
        responseOk(doGet(REMOVE_USER, new String[][] { { userIDColumn, user.getUid().toString() } }));
        try {
            responseOk(doGet(REMOVE_USER, new String[][] { { userIDColumn, user.getUid().toString() } }));
        } catch (DBServiceException x) {
            x.checkMessage(STATUS_USER_NOT_FOUND_ERROR);
        }
        assert responseOk(doGet(HAS_USER, new String[][] { { userIDColumn, user.getUid().toString() } })) : "Removing a user did not actually remove the user";
        User user2 = parseUser(doGet(GET_LAST_ARCHIVED_USER, new String[][] { { userIDColumn, user.getUid().toString() } }));
        assert user.equals(user2);
        try {
            parseUser(doGet(REMOVE_USER, new String[][] { { userIDColumn, "fake:user:id:123" } }));
        } catch (DBServiceException x) {
            x.checkMessage(STATUS_USER_NOT_FOUND_ERROR);
        }
    }

    protected Map<String, String> parseTransaction(HttpResponse response) throws IOException {
        return getSerializer().deserializeToMap(response.getEntity().getContent());
    }

    @Test
    public void testIDPs() throws Exception {
        List<IdentityProvider> originalIdps = parseIdps(doGet(GET_ALL_IDPS, new String[][] { {} }));
        originalIdps.add(new IdentityProvider(createToken("idp")));
        originalIdps.add(new IdentityProvider(createToken("idp")));
        originalIdps.add(new IdentityProvider(createToken("idp")));
        responseOk(doGet(SET_ALL_IDPS, convertIdpsToArray(originalIdps)));
        List<IdentityProvider> newIdps = parseIdps(doGet(GET_ALL_IDPS, new String[][] { {} }));
        assert originalIdps.size() == newIdps.size();
        for (IdentityProvider idp : originalIdps) {
            assert newIdps.contains(idp);
        }
        newIdps = new ArrayList<IdentityProvider>();
        for (int i = 0; i < count; i++) {
            newIdps.add(new IdentityProvider(createToken("idp")));
        }
        responseOk(doGet(SET_ALL_IDPS, convertIdpsToArray(newIdps)));
        List<IdentityProvider> newIdps2 = parseIdps(doGet(GET_ALL_IDPS, new String[][] { {} }));
        assert newIdps.size() == newIdps2.size();
        for (IdentityProvider idp : newIdps2) {
            assert newIdps.contains(idp);
        }
        try {
            responseOk(doGet(SET_ALL_IDPS, new String[][] { { idpColumn, "" } }));
        } catch (DBServiceException x) {
            assert x.checkMessage(STATUS_IDP_SAVE_FAILED);
        }
    }

    protected String[][] convertIdpsToArray(List<IdentityProvider> idps) throws UnsupportedEncodingException {
        String[][] out = new String[idps.size()][2];
        for (int i = 0; i < idps.size(); i++) {
            out[i][0] = IdentityProvidersTable.idpUIDColumn;
            out[i][1] = encode(idps.get(i).getUid().toString());
        }
        return out;
    }

    protected List<IdentityProvider> parseIdps(HttpResponse response) throws IOException {
        return getSerializer().deserializeIdps(response.getEntity().getContent());
    }

    protected boolean responseOk(HttpResponse response) throws Exception {
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new Exception("ERROR! " + response.getStatusLine().getStatusCode() + " status= " + response.getStatusLine().getStatusCode());
        }
        return getSerializer().reponseOk(response.getEntity().getContent());
    }

    protected User parseUser(HttpResponse response) throws IOException, ParseException {
        return getSerializer().deserializeUser(response.getEntity().getContent(), new User(getUris()));
    }

    protected URI parseUserID(HttpResponse response) throws IOException {
        return getSerializer().deserializeUserID(response.getEntity().getContent());
    }

    protected int getReturnStatus(HttpResponse response) throws IOException {
        return getSerializer().readResponseOnly(response.getEntity().getContent());
    }
}
