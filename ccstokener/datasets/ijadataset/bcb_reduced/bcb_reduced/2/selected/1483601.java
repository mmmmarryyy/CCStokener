package au.edu.diasb.annotation.danno.admin;

import static au.edu.diasb.annotation.danno.common.DannoProperties.ADMIN_LOGIN_URL_PROP;
import static au.edu.diasb.annotation.danno.common.DannoProperties.ADMIN_PASSWORD_PROP;
import static au.edu.diasb.annotation.danno.common.DannoProperties.ADMIN_USER_PROP;
import static au.edu.diasb.annotation.danno.common.DannoProperties.ANNOTEA_URL_PROP;
import static au.edu.diasb.annotation.danno.common.DannoProperties.OAI_URL_PROP;
import static au.edu.diasb.chico.config.DurationUtils.MILLIS_PER_SECOND;
import static au.edu.diasb.danno.constants.AnnoteaProtocolConstants.DANNO_CHECK_TRIPLE_STORE;
import static au.edu.diasb.danno.constants.AnnoteaProtocolConstants.DANNO_DUMP_TRIPLE_STORE;
import static au.edu.diasb.danno.constants.AnnoteaProtocolConstants.DANNO_GET_INFO;
import static au.edu.diasb.danno.constants.AnnoteaProtocolConstants.DANNO_LOAD_TRIPLE_STORE;
import static au.edu.diasb.danno.constants.AnnoteaProtocolConstants.DANNO_RESET_TRIPLE_STORE;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import au.edu.diasb.annotation.danno.protocol.DannoClient;
import au.edu.diasb.annotation.danno.protocol.DannoClientFactory;
import au.edu.diasb.annotation.danno.protocol.LoginDannoClientFactory;

/**
 * Base class for the Danno client apps including stand-alone test apps. 
 * 
 * @author scrawley
 */
public abstract class DannoAppBase implements Reporter {

    private class DefaultReporter implements Reporter {

        private PrintStream report;

        DefaultReporter(PrintStream report) {
            this.report = report;
        }

        public void report(String message) {
            report.println(message);
        }

        public void report(String propName, String value) {
            report.println("    " + propName + "=" + value);
        }

        public void report(long start, long count, String desc) {
            long end = System.currentTimeMillis();
            report.println(count + " " + desc + " requests took " + (end - start) + " milliseconds: " + ((end - start + 0.0D) / count / MILLIS_PER_SECOND) + " seconds avg");
        }
    }

    protected final DatatypeFactory xmlDatatypeFactory;

    protected final String loginUrl;

    protected final String annoteaUrl;

    protected final String oaiUrl;

    protected final String username;

    protected final String password;

    private boolean debug;

    private boolean noAuth;

    private DannoClient client;

    private DannoClientFactory acf;

    private Reporter reporter = new DefaultReporter(System.out);

    public DannoAppBase(Properties props) throws DatatypeConfigurationException, ClientProtocolException, IOException {
        xmlDatatypeFactory = DatatypeFactory.newInstance();
        annoteaUrl = props.getProperty(ANNOTEA_URL_PROP);
        oaiUrl = props.getProperty(OAI_URL_PROP);
        loginUrl = props.getProperty(ADMIN_LOGIN_URL_PROP);
        username = props.getProperty(ADMIN_USER_PROP);
        password = props.getProperty(ADMIN_PASSWORD_PROP);
    }

    public abstract void run(long count) throws IOException, InterruptedException, DatatypeConfigurationException, ProtocolException;

    public abstract void run(String[] args) throws DannoControlAppException, IOException, InterruptedException, DatatypeConfigurationException, ProtocolException;

    protected DannoClient getClient() throws IOException {
        if (client == null) {
            acf = noAuth ? new LoginDannoClientFactory(null, null, null) : new LoginDannoClientFactory(loginUrl, username, password);
            client = acf.createClient((HttpServletRequest) null, null);
        }
        return client;
    }

    public void closeClient() {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    protected String doQuery(String target) throws IOException, ProtocolException {
        DannoClient ac = getClient();
        HttpGet get = new HttpGet(target);
        HttpResponse response = ac.execute(get);
        if (!ac.isOK()) {
            throw new DannoRequestFailureException("GET", response);
        }
        return new BasicResponseHandler().handleResponse(response);
    }

    protected void doDelete(String target) throws IOException, ProtocolException {
        DannoClient ac = getClient();
        HttpDelete delete = new HttpDelete(target);
        if (!ac.executeIgnore(delete)) {
            throw new DannoRequestFailureException("DELETE", ac.getLastResponse());
        }
    }

    protected String doPutRDF(String target, String content) throws IOException, ProtocolException {
        DannoClient ac = getClient();
        HttpPut put = new HttpPut(target);
        put.setEntity(new StringEntity(content, "UTF-8"));
        put.addHeader("Content-Type", "application/xml");
        put.addHeader("Accept", "application/xml");
        HttpResponse response = ac.execute(put);
        if (!ac.isOK()) {
            throw new DannoRequestFailureException("PUT", response);
        }
        return new BasicResponseHandler().handleResponse(response);
    }

    protected String doGetRDF(String target) throws IOException, ProtocolException {
        DannoClient ac = getClient();
        HttpGet get = new HttpGet(target);
        get.addHeader("Accept", "application/xml");
        HttpResponse response = ac.execute(get);
        if (!ac.isOK()) {
            throw new DannoRequestFailureException("GET", response);
        }
        return new BasicResponseHandler().handleResponse(response);
    }

    protected String doPostRDF(String target, String content) throws IOException, ProtocolException {
        DannoClient ac = getClient();
        HttpPost post = new HttpPost(target);
        post.setEntity(new StringEntity(content, "UTF-8"));
        post.addHeader("Content-Type", "application/xml");
        post.addHeader("Accept", "application/xml");
        HttpResponse response = ac.execute(post);
        if (!ac.isOK()) {
            throw new DannoRequestFailureException("POST", response);
        }
        return new BasicResponseHandler().handleResponse(response);
    }

    protected void doReset() throws IOException, ProtocolException {
        doAdminCommand(DANNO_RESET_TRIPLE_STORE);
    }

    protected void doDump() throws IOException, ProtocolException {
        doAdminCommand(DANNO_DUMP_TRIPLE_STORE);
    }

    protected void doLoad() throws IOException, ProtocolException {
        doAdminCommand(DANNO_LOAD_TRIPLE_STORE);
    }

    protected void doCheck() throws IOException, ProtocolException {
        doAdminCommand(DANNO_CHECK_TRIPLE_STORE);
    }

    private void doAdminCommand(String command) throws IOException, ProtocolException {
        if (annoteaUrl.endsWith("/") && command.startsWith("/")) {
            command = command.substring(1);
        }
        DannoClient ac = getClient();
        HttpPost post = new HttpPost(command);
        if (!ac.executeIgnore(post)) {
            throw new DannoRequestFailureException("GET", ac.getLastResponse());
        }
    }

    protected Properties doInfo() throws IOException, ProtocolException {
        String message = doQuery(DANNO_GET_INFO);
        if (message == null) {
            return null;
        }
        Properties props = new Properties();
        props.loadFromXML(new ByteArrayInputStream(message.getBytes()));
        return props;
    }

    public void report(String message) {
        reporter.report(message);
    }

    public void report(String propName, String value) {
        reporter.report(propName, value);
    }

    public void report(long start, long count, String desc) {
        reporter.report(start, count, desc);
    }

    protected void setReporter(Reporter reporter) {
        this.reporter = reporter;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isNoAuth() {
        return noAuth;
    }

    public void setNoAuth(boolean noAuth) {
        this.noAuth = noAuth;
    }
}
