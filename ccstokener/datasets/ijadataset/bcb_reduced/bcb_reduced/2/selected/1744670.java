package org.xactor.test.recover.test;

import java.net.URL;
import java.util.Properties;
import javax.management.MBeanServerConnection;
import javax.naming.Context;
import javax.naming.InitialContext;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.jboss.test.JBossIIOPTestCase;
import org.xactor.test.recover.bean.DummyRecoverableProxyService.RecoverableWrapper;
import org.xactor.test.recover.interfaces.DummyXAResource;

/**
 * A JBossCrashRecoveryIIOPTestCase.
 * 
 * @author <a href="reverbel@ime.usp.br">Francisco Reverbel</a>
 * @version $Revision: 58484 $
 */
public class JBossCrashRecoveryIIOPTestCase extends JBossIIOPTestCase {

    protected int N = 10;

    protected Properties resource1ServerJndiProps;

    protected Properties resource2ServerJndiProps;

    protected MBeanServerConnection resource1Server;

    protected MBeanServerConnection resource2Server;

    protected MBeanServerConnection server;

    protected JBossCrashRecoveryIIOPTestCase(String name) {
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
        RecoverableWrapper recoverable = (RecoverableWrapper) getInitialJnpContext().lookup(recoverableName);
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

    public static Test suite(Class testCaseClass) {
        TestSuite suite = new TestSuite();
        suite.addTest(new TestSuite(testCaseClass));
        return suite;
    }
}
