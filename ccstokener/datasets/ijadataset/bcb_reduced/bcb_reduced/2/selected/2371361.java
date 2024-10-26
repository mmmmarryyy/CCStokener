package com.xdatasystem.contactsimporter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.UrlEncodedFormEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpProtocolParams;
import com.xdatasystem.contactsimporter.msn.MSNImporter;

public abstract class ContactListImporterImpl implements ContactListImporter {

    private String username;

    private String password;

    private static Logger log = Logger.getLogger(com.xdatasystem.contactsimporter.ContactListImporterImpl.class.getName());

    private Pattern emailPattern;

    private DefaultHttpClient client;

    private String currentURL;

    public ContactListImporterImpl(String username, String password) {
        currentURL = null;
        this.username = username;
        this.password = password;
        emailPattern = Pattern.compile("^[0-9a-z]([-_.~]?[0-9a-z])*@[0-9a-z]([-.]?[0-9a-z])*\\.[a-z]{2,4}$");
    }

    protected Logger getLogger() {
        return log;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public abstract String getLoginURL();

    public abstract String getContactListURL();

    public String sendMail() throws Exception {
        return "";
    }

    public boolean isEmailAddress(String email) {
        return emailPattern.matcher(email).matches();
    }

    public List getContactList() throws ContactListImporterException {
        try {
            DefaultHttpClient client = getHttpClient();
            log.info("Performing login");
            login(client);
            log.info("Login succeeded");
            if (this instanceof MSNImporter) {
                return parseContacts(null);
            }
            String host = ((HttpHost) client.getDefaultContext().getAttribute("http.target_host")).getHostName();
            return getAndParseContacts(client, host);
        } catch (Exception e) {
            if (e instanceof ContactListImporterException) throw (ContactListImporterException) e; else throw new ContactListImporterException((new StringBuilder("Exception occured: ")).append(e.getMessage()).toString(), e);
        }
    }

    public List getAndUpdateContactList(List contacts) throws ContactListImporterException {
        try {
            DefaultHttpClient client = getHttpClient();
            log.info("Performing login");
            login(client);
            log.info("Login succeeded");
            String host = ((HttpHost) client.getDefaultContext().getAttribute("http.target_host")).getHostName();
            return getAndParseUpdateContacts(client, host, contacts);
        } catch (Exception e) {
            if (e instanceof ContactListImporterException) throw (ContactListImporterException) e; else throw new ContactListImporterException((new StringBuilder("Exception occured: ")).append(e.getMessage()).toString(), e);
        }
    }

    protected List getAndParseUpdateContacts(DefaultHttpClient client, String host, List contacts) throws Exception {
        return null;
    }

    protected List getAndParseContacts(DefaultHttpClient client, String host) throws Exception {
        String listUrl = getContactListURL();
        if (username.contains("@hotmail.com")) {
            listUrl = String.format(getContactListURL(), new Object[] { host });
        }
        log.info("Retrieving contactlist");
        InputStream input = getContactListContent(client, listUrl, null);
        log.info("Parsing contactlist");
        List contacts = parseContacts(input);
        input.close();
        return contacts;
    }

    protected InputStream getContactListContent(DefaultHttpClient client, String listUrl, String referer) throws ContactListImporterException, URISyntaxException, InterruptedException, HttpException, IOException {
        return doGet(client, listUrl, referer);
    }

    protected abstract void login(DefaultHttpClient defaulthttpclient) throws Exception;

    protected abstract List parseContacts(InputStream inputstream) throws Exception;

    protected DefaultHttpClient getHttpClient() {
        if (client == null) {
            client = new DefaultHttpClient();
            client.setCookieStore(new UpdateableCookieStore());
            client.setRedirectHandler(new MemorizingRedirectHandler());
            client.getParams().setParameter("http.protocol.cookie-policy", "compatibility");
            List headers = new ArrayList();
            headers.add(new BasicHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; nl; rv:1.8.1.13) Gecko/20080311 Firefox/2.0.0.13"));
            client.getParams().setParameter("http.default-headers", headers);
            return client;
        } else {
            return client;
        }
    }

    protected void setHeaders(HttpRequest req, String referer) {
        req.addHeader("Accept", "text/xml,text/javascript,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
        req.addHeader("Accept-Language", "en-us;q=0.7,en;q=0.3");
        req.addHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        if (referer != null) req.addHeader("Referer", referer);
    }

    private void updateCurrentUrl(HttpClient client) {
        HttpRequest req = (HttpRequest) client.getDefaultContext().getAttribute("http.request");
        HttpHost host = (HttpHost) client.getDefaultContext().getAttribute("http.target_host");
        currentURL = (new StringBuilder(String.valueOf(host.toURI()))).append(req.getRequestLine().getUri()).toString();
    }

    protected String getCurrentUrl() {
        return currentURL;
    }

    protected InputStream doGet(HttpClient client, String url, String referer) throws ContactListImporterException, URISyntaxException, InterruptedException, HttpException, IOException {
        client.getConnectionManager().closeIdleConnections(0L, TimeUnit.MILLISECONDS);
        HttpGet get = new HttpGet(url);
        setHeaders(get, referer);
        HttpResponse resp = client.execute(get, client.getDefaultContext());
        updateCurrentUrl(client);
        InputStream content = resp.getEntity().getContent();
        return content;
    }

    protected HttpResponse getResponse(HttpClient client, String url, String referer) throws ContactListImporterException, URISyntaxException, InterruptedException, HttpException, IOException {
        client.getConnectionManager().closeIdleConnections(0L, TimeUnit.MILLISECONDS);
        HttpGet get = new HttpGet(url);
        setHeaders(get, referer);
        HttpResponse resp = client.execute(get, client.getDefaultContext());
        return resp;
    }

    protected InputStream doPost(HttpClient client, String url, NameValuePair data[], String referer) throws ContactListImporterException, HttpException, IOException, InterruptedException, URISyntaxException {
        log.info((new StringBuilder("POST ")).append(url).toString());
        HttpPost post = new HttpPost(url);
        setHeaders(post, referer);
        post.addHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setEntity(new UrlEncodedFormEntity(data, "UTF-8"));
        HttpProtocolParams.setUseExpectContinue(client.getParams(), false);
        HttpProtocolParams.setUseExpectContinue(post.getParams(), false);
        HttpResponse resp = client.execute(post, client.getDefaultContext());
        updateCurrentUrl(client);
        InputStream content = resp.getEntity().getContent();
        return content;
    }

    protected String readInputStream(InputStream is, String encode) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(is, encode));
        StringBuffer buffer = new StringBuffer();
        String line;
        while ((line = in.readLine()) != null) buffer.append(line);
        is.close();
        return buffer.toString();
    }

    protected void consumeInputStream(InputStream is) throws IOException {
        for (BufferedReader in = new BufferedReader(new InputStreamReader(is)); in.readLine() != null; ) ;
        is.close();
    }

    public static boolean isConformingEmail(String email, String domains[]) {
        if (email == null) return false;
        String as[];
        int j = (as = domains).length;
        for (int i = 0; i < j; i++) {
            String d = as[i];
            if (email.indexOf(d) == email.length() - d.length()) return true;
        }
        return false;
    }
}
