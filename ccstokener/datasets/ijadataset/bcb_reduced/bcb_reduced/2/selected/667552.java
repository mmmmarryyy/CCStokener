package org.xactor.test.dtm.test;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.transaction.RollbackException;
import javax.transaction.UserTransaction;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.jboss.test.JBossTestCase;
import org.jboss.test.JBossTestSetup;
import org.xactor.test.dtm.interfaces.PassThrough;
import org.xactor.test.dtm.interfaces.PassThroughHome;

public class T06OTSInterpositionUnitTestCase extends JBossTestCase {

    private java.util.Properties jndiProps;

    public T06OTSInterpositionUnitTestCase(String name) throws java.io.IOException {
        super(name);
        java.net.URL url = ClassLoader.getSystemResource("host0.cosnaming.jndi.properties");
        jndiProps = new java.util.Properties();
        jndiProps.load(url.openStream());
    }

    protected InitialContext getInitialContext() throws Exception {
        return new InitialContext(jndiProps);
    }

    public void testCommittedTx() throws Exception {
        getLog().debug("+++ testCommittedTx");
        Context ctx = getInitialContext();
        getLog().debug("Obtain UserTransaction instance");
        UserTransaction userTx = (UserTransaction) ctx.lookup("UserTransaction");
        getLog().debug("Obtain home interface");
        Object objref = ctx.lookup("dtmtest/PassThrough2OtsEJB");
        PassThroughHome home = (PassThroughHome) PortableRemoteObject.narrow(objref, PassThroughHome.class);
        getLog().debug("Create PassThrough bean");
        PassThrough session = home.create("testCommittedTx");
        getLog().debug("Set balances");
        userTx.begin();
        session.setBalances(101, 102);
        userTx.commit();
        int balances[];
        getLog().debug("Get balances");
        userTx.begin();
        balances = session.getBalances();
        userTx.commit();
        assertTrue("first balance == 101", balances[0] == 101);
        assertTrue("second balance == 102", balances[1] == 102);
        getLog().debug("Update balances");
        userTx.begin();
        session.addToBalances(100);
        userTx.commit();
        getLog().debug("Check updated balances");
        userTx.begin();
        balances = session.getBalances();
        userTx.commit();
        assertTrue("first balance == 201", balances[0] == 201);
        assertTrue("second balance == 202", balances[1] == 202);
        getLog().debug("Remove PassThrough bean");
        session.remove();
    }

    public void testRolledbackTx() throws Exception {
        getLog().debug("+++ testRolledbackTx");
        Context ctx = getInitialContext();
        getLog().debug("Obtain UserTransaction instance");
        UserTransaction userTx = (UserTransaction) ctx.lookup("UserTransaction");
        getLog().debug("Obtain home interface");
        Object objref = ctx.lookup("dtmtest/PassThrough2OtsEJB");
        PassThroughHome home = (PassThroughHome) PortableRemoteObject.narrow(objref, PassThroughHome.class);
        getLog().debug("Create PassThrough bean");
        PassThrough session = home.create("testRolledbackTx");
        getLog().debug("Set balances");
        userTx.begin();
        session.setBalances(101, 102);
        userTx.commit();
        int balances[];
        getLog().debug("Get balances");
        userTx.begin();
        balances = session.getBalances();
        userTx.commit();
        assertTrue("first balance == 101", balances[0] == 101);
        assertTrue("second balance == 102", balances[1] == 102);
        getLog().debug("Update balances and rollback");
        userTx.begin();
        session.addToBalances(100);
        userTx.rollback();
        getLog().debug("Check that all balances remain the same");
        userTx.begin();
        balances = session.getBalances();
        userTx.commit();
        assertTrue("first balance == 101", balances[0] == 101);
        assertTrue("second balance == 102", balances[1] == 102);
        session.remove();
    }

    public void testSetRollbackOnly() throws Exception {
        getLog().debug("+++ testSetRollbackOnly");
        Context ctx = getInitialContext();
        getLog().debug("Obtain UserTransaction instance");
        UserTransaction userTx = (UserTransaction) ctx.lookup("UserTransaction");
        getLog().debug("Obtain home interface");
        Object objref = ctx.lookup("dtmtest/PassThrough2OtsEJB");
        PassThroughHome home = (PassThroughHome) PortableRemoteObject.narrow(objref, PassThroughHome.class);
        getLog().debug("Create PassThrough bean");
        PassThrough session = home.create("testSetRollbackOnly");
        getLog().debug("Set balances");
        userTx.begin();
        session.setBalances(101, 102);
        userTx.commit();
        int balances[];
        getLog().debug("Get balances");
        userTx.begin();
        balances = session.getBalances();
        userTx.commit();
        assertTrue("first balance == 101", balances[0] == 101);
        assertTrue("second balance == 102", balances[1] == 102);
        getLog().debug("Update balances, set rollback only at the front end, " + "and try to commit");
        try {
            userTx.begin();
            session.addToBalances(100);
            session.setRollbackOnly();
            session.addToBalances(50);
            userTx.commit();
            fail("RollbackException should have been thrown");
        } catch (RollbackException e) {
            getLog().debug("Expected exception: " + e);
        }
        getLog().debug("Check that all balances remain the same");
        userTx.begin();
        balances = session.getBalances();
        userTx.commit();
        assertTrue("first balance == 101", balances[0] == 101);
        assertTrue("second balance == 102", balances[1] == 102);
        session.remove();
    }

    public void testSetRollbackOnlyAtTheFirstAccount() throws Exception {
        getLog().debug("+++ testSetRollbackOnlyAtTheFirstAccount");
        Context ctx = getInitialContext();
        getLog().debug("Obtain UserTransaction instance");
        UserTransaction userTx = (UserTransaction) ctx.lookup("UserTransaction");
        getLog().debug("Obtain home interface");
        Object objref = ctx.lookup("dtmtest/PassThrough2OtsEJB");
        PassThroughHome home = (PassThroughHome) PortableRemoteObject.narrow(objref, PassThroughHome.class);
        getLog().debug("Create PassThrough bean");
        PassThrough session = home.create("testSetRollbackOnlyAtTheFirstAccount");
        getLog().debug("Set balances");
        userTx.begin();
        session.setBalances(101, 102);
        userTx.commit();
        int balances[];
        getLog().debug("Get balances");
        userTx.begin();
        balances = session.getBalances();
        userTx.commit();
        assertTrue("first balance == 101", balances[0] == 101);
        assertTrue("second balance == 102", balances[1] == 102);
        getLog().debug("Update balances, set rollback only at " + "the first account, and try to commit");
        try {
            userTx.begin();
            session.addToBalances(100);
            session.tellFirstAccountToSetRollbackOnly();
            session.addToBalances(50);
            userTx.commit();
            fail("RollbackException should have been thrown");
        } catch (RollbackException e) {
            getLog().debug("Expected exception: " + e);
        }
        getLog().debug("Check that all balances remain the same");
        userTx.begin();
        balances = session.getBalances();
        userTx.commit();
        assertTrue("first balance == 101", balances[0] == 101);
        assertTrue("second balance == 102", balances[1] == 102);
        session.remove();
    }

    public void testSetRollbackOnlyAtTheSecondAccount() throws Exception {
        getLog().debug("+++ testSetRollbackOnlyAtTheSecondAccount");
        Context ctx = getInitialContext();
        getLog().debug("Obtain UserTransaction instance");
        UserTransaction userTx = (UserTransaction) ctx.lookup("UserTransaction");
        getLog().debug("Obtain home interface");
        Object objref = ctx.lookup("dtmtest/PassThrough2OtsEJB");
        PassThroughHome home = (PassThroughHome) PortableRemoteObject.narrow(objref, PassThroughHome.class);
        getLog().debug("Create PassThrough bean");
        PassThrough session = home.create("testSetRollbackOnlyAtTheSecondAccount");
        getLog().debug("Set balances");
        userTx.begin();
        session.setBalances(101, 102);
        userTx.commit();
        int balances[];
        getLog().debug("Get balances");
        userTx.begin();
        balances = session.getBalances();
        userTx.commit();
        assertTrue("first balance == 101", balances[0] == 101);
        assertTrue("second balance == 102", balances[1] == 102);
        getLog().debug("Update balances, set rollback only at " + "the first account, and try to commit");
        try {
            userTx.begin();
            session.addToBalances(100);
            session.tellSecondAccountToSetRollbackOnly();
            session.addToBalances(50);
            userTx.commit();
            fail("RollbackException should have been thrown");
        } catch (RollbackException e) {
            getLog().debug("Expected exception: " + e);
        }
        getLog().debug("Check that all balances remain the same");
        userTx.begin();
        balances = session.getBalances();
        userTx.commit();
        assertTrue("first balance == 101", balances[0] == 101);
        assertTrue("second balance == 102", balances[1] == 102);
        session.remove();
    }

    public static Test suite() throws Exception {
        java.net.URL url = ClassLoader.getSystemResource("host0.jndi.properties");
        java.util.Properties host0JndiProps = new java.util.Properties();
        host0JndiProps.load(url.openStream());
        java.util.Properties systemProps = System.getProperties();
        systemProps.putAll(host0JndiProps);
        System.setProperties(systemProps);
        TestSuite suite = new TestSuite();
        suite.addTest(new TestSuite(T06OTSInterpositionUnitTestCase.class));
        TestSetup wrapper = new JBossTestSetup(suite) {

            protected void setUp() throws Exception {
                super.setUp();
                deploy("dtmpassthrough2ots.jar");
            }

            protected void tearDown() throws Exception {
                undeploy("dtmpassthrough2ots.jar");
                super.tearDown();
            }
        };
        return wrapper;
    }
}
