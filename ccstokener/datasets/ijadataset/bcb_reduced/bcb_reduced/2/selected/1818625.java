package org.xactor.test.recover.test;

import java.net.URL;
import java.util.Properties;
import javax.management.MBeanServerConnection;
import javax.naming.Context;
import javax.naming.InitialContext;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.jboss.test.JBossTestCase;
import org.xactor.test.recover.bean.DummyRecoverableProxyService.RecoverableWrapper;
import org.xactor.test.recover.interfaces.DummyXAResource;

/**
 * A JBossCrashRecoveryTestCase.
 * 
 * @author <a href="reverbel@ime.usp.br">Francisco Reverbel</a>
 * @version $Revision: 58484 $
 */
public abstract class JBossCrashRecoveryTestCase extends JBossTestCase {

    protected int N = 10;

    protected Properties resource1ServerJndiProps;

    protected Properties resource2ServerJndiProps;

    protected Properties resource3ServerJndiProps;

    protected MBeanServerConnection resource1Server;

    protected MBeanServerConnection resource2Server;

    protected MBeanServerConnection resource3Server;

    protected MBeanServerConnection server;

    protected JBossCrashRecoveryTestCase(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        server = delegate.getServer();
    }

    protected String getUnqualifiedClassName() {
        String s = getClass().getName();
        int i = s.lastIndexOf('.') + 1;
        return s.substring(i);
    }

    protected DummyXAResource getXAResource(String recoverableName) throws Exception {
        RecoverableWrapper recoverable = (RecoverableWrapper) getInitialContext().lookup(recoverableName);
        DummyXAResource xaRes = (DummyXAResource) recoverable.getUnwrappedResource();
        return xaRes;
    }

    protected Context getResource1ServerInitialContext() throws Exception {
        if (resource1ServerJndiProps == null) {
            URL url = ClassLoader.getSystemResource("jndi.properties");
            resource1ServerJndiProps = new java.util.Properties();
            resource1ServerJndiProps.load(url.openStream());
            String jndiHost = System.getProperty("jbosstest.resource1.server.host", "localhost");
            String jndiUrl = "jnp://" + jndiHost + ":1099";
            resource1ServerJndiProps.setProperty("java.naming.provider.url", jndiUrl);
        }
        return new InitialContext(resource1ServerJndiProps);
    }

    protected MBeanServerConnection getResource1Server() throws Exception {
        if (resource1Server == null) {
            String adaptorName = System.getProperty("jbosstest.server.name", "jmx/invoker/RMIAdaptor");
            Context resCtx = getResource1ServerInitialContext();
            resource1Server = (MBeanServerConnection) resCtx.lookup(adaptorName);
        }
        return resource1Server;
    }

    protected Context getResource2ServerInitialContext() throws Exception {
        if (resource2ServerJndiProps == null) {
            URL url = ClassLoader.getSystemResource("jndi.properties");
            resource2ServerJndiProps = new java.util.Properties();
            resource2ServerJndiProps.load(url.openStream());
            String jndiHost = System.getProperty("jbosstest.resource2.server.host", "localhost");
            String jndiUrl = "jnp://" + jndiHost + ":1099";
            resource2ServerJndiProps.setProperty("java.naming.provider.url", jndiUrl);
        }
        return new InitialContext(resource2ServerJndiProps);
    }

    protected MBeanServerConnection getResource2Server() throws Exception {
        if (resource2Server == null) {
            String adaptorName = System.getProperty("jbosstest.server.name", "jmx/invoker/RMIAdaptor");
            Context resCtx = getResource2ServerInitialContext();
            resource2Server = (MBeanServerConnection) resCtx.lookup(adaptorName);
        }
        return resource2Server;
    }

    protected Context getResource3ServerInitialContext() throws Exception {
        if (resource3ServerJndiProps == null) {
            URL url = ClassLoader.getSystemResource("jndi.properties");
            resource3ServerJndiProps = new java.util.Properties();
            resource3ServerJndiProps.load(url.openStream());
            String jndiHost = System.getProperty("jbosstest.resource3.server.host", "localhost");
            String jndiUrl = "jnp://" + jndiHost + ":1099";
            resource3ServerJndiProps.setProperty("java.naming.provider.url", jndiUrl);
        }
        return new InitialContext(resource3ServerJndiProps);
    }

    protected MBeanServerConnection getResource3Server() throws Exception {
        if (resource3Server == null) {
            String adaptorName = System.getProperty("jbosstest.server.name", "jmx/invoker/RMIAdaptor");
            Context resCtx = getResource3ServerInitialContext();
            resource3Server = (MBeanServerConnection) resCtx.lookup(adaptorName);
        }
        return resource3Server;
    }

    protected boolean isExpectedThrowable(Throwable e) {
        return e instanceof java.rmi.MarshalException || e instanceof org.jboss.remoting.CannotConnectException;
    }

    public static Test suite(Class testCaseClass) {
        TestSuite suite = new TestSuite();
        suite.addTest(new TestSuite(testCaseClass));
        return suite;
    }
}
