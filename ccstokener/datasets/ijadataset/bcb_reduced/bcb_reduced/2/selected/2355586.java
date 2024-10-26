package de.objectcode.time4u.client.feeds.job;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.eclipse.jface.preference.IPreferenceStore;
import de.objectcode.time4u.client.Activator;
import de.objectcode.time4u.client.feeds.util.EasySSLProtocolSocketFactory;
import de.objectcode.time4u.client.preferences.PreferenceConstants;
import de.objectcode.time4u.util.PasswordCipher;

public abstract class BaseHttpFeed implements IFeed {

    private static final String CRYPT_ADD = "D9A9DC9E5D163D82357B78AE3BF5EB0D";

    protected String m_url;

    protected String m_credentials;

    private HttpClient m_client;

    private HttpMethod m_method;

    protected BaseHttpFeed() {
        m_url = "";
        m_credentials = null;
    }

    protected BaseHttpFeed(IPreferenceStore store, String base) {
        m_url = store.getString(base + "url");
        if (store.getBoolean(base + "needsAuthentication")) m_credentials = store.getString(base + "credentials");
    }

    public String getDescription() {
        return m_url;
    }

    public String getUrl() {
        return m_url;
    }

    public void setUrl(String url) {
        m_url = url;
    }

    public boolean isNeedAuthentication() {
        return m_credentials != null;
    }

    public String getUserId() {
        if (m_credentials != null) {
            IPreferenceStore store = Activator.getDefault().getPreferenceStore();
            String userId = store.getString(PreferenceConstants.USERCONTEXT_USERID);
            PasswordCipher.Credentials credentials = PasswordCipher.decode(CRYPT_ADD + userId + CRYPT_ADD, m_credentials);
            return credentials.getUserId();
        }
        return null;
    }

    public void setCredentials(boolean needAuthentication, String userId, String password) {
        if (needAuthentication) {
            IPreferenceStore store = Activator.getDefault().getPreferenceStore();
            String userName = store.getString(PreferenceConstants.USERCONTEXT_USERID);
            PasswordCipher.Credentials credentials = new PasswordCipher.Credentials(userId, password, m_url);
            m_credentials = PasswordCipher.encode(CRYPT_ADD + userName + CRYPT_ADD, credentials);
        } else {
            m_credentials = null;
        }
    }

    public void store(IPreferenceStore store, String base) {
        store.setValue(base + "url", m_url);
        store.setValue(base + "needsAuthentication", m_credentials != null);
        if (m_credentials != null) store.setValue(base + "credentials", m_credentials);
    }

    protected InputStream openConnection(String url) throws HttpException, IOException {
        Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 443));
        m_client = new HttpClient();
        if (m_credentials != null) {
            IPreferenceStore store = Activator.getDefault().getPreferenceStore();
            String userId = store.getString(PreferenceConstants.USERCONTEXT_USERID);
            PasswordCipher.Credentials credentials = PasswordCipher.decode(CRYPT_ADD + userId + CRYPT_ADD, m_credentials);
            m_client.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(credentials.getUserId(), credentials.getPassword()));
        }
        m_method = new GetMethod(url);
        m_method.setDoAuthentication(m_credentials != null);
        if (m_client.executeMethod(m_method) == HttpStatus.SC_OK) return m_method.getResponseBodyAsStream();
        return null;
    }

    protected void closeConnection() {
        if (m_method != null) m_method.releaseConnection();
        m_method = null;
        m_client = null;
    }
}
