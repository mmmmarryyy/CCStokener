package org.osidx.junit;

import junit.framework.TestCase;
import java.io.IOException;
import org.osid.*;
import org.osid.shared.*;
import org.osid.agent.*;

/**
 *  <p>
 *  AgentTest is a JUnit test for the Agent OSID. The OSID implementation
 *  loaded is specified through using the TestCase constructor. 
 *  </p><p>
 *  CVS $Id: AgentTest.java,v 1.1 2005/08/25 15:18:56 tom Exp $
 *  </p>
 *
 *  @author  Tom Coppeto
 *  @version $OSID: 2.0$ $Revision: 1.1 $
 */
public class AgentTest extends TestCase {

    private String osid = "org.osid.agent.AgentManager";

    private AgentManager mgr;

    private String impl;

    private boolean supportsAdmin;

    private Id agentId;

    private String agentName;

    private Type agentType;

    private org.osid.shared.Properties agentProperties;

    private Id groupId;

    private String groupName;

    private Type groupType;

    private org.osid.shared.Properties groupProperties;

    protected void setUp() {
        String test = System.getProperty("test.name");
        if (test == null) {
            fail("no test specified");
            return;
        }
        java.util.Properties p = new java.util.Properties();
        java.net.URL url = ClassLoader.getSystemResource(test + ".properties");
        try {
            p.load(url.openStream());
        } catch (IOException ie) {
            fail(ie.getMessage());
            return;
        }
        this.impl = p.getProperty("agent_manager");
        try {
            log("loading AgentManager(" + impl + ")");
            this.mgr = (AgentManager) OsidLoader.getManager(osid, impl, new OsidContext(), new java.util.Properties());
            assertNotNull(mgr);
        } catch (OsidException oe) {
            oe.printStackTrace();
            fail("OsidException" + oe.getMessage());
            return;
        }
        String property = p.getProperty("agent_admin");
        if (property.equals("true")) {
            this.supportsAdmin = true;
        } else {
            this.supportsAdmin = false;
        }
        this.agentName = p.getProperty("agent");
    }

    protected void tearDown() {
    }

    public void testAgentManager_getAgentTypes() {
        log("testing AgentManager.getAgentTypes()");
        try {
            TypeIterator types = mgr.getAgentTypes();
            assertNotNull(types);
            assertTrue(types.hasNextType());
            log("\tAgent Types:");
            while (types.hasNextType()) {
                Type type = types.nextType();
                assertNotNull(type);
                log("\t" + formatType(type));
            }
        } catch (AgentException ae) {
            ae.printStackTrace();
            fail("AgentException" + ae.getMessage());
            return;
        } catch (SharedException se) {
            se.printStackTrace();
            fail("SharedException" + se.getMessage());
            return;
        }
        return;
    }

    public void testAgentManager_getPropertyTypes() {
        log("testing AgentManager.getPropertyTypes()");
        try {
            TypeIterator types = mgr.getPropertyTypes();
            assertNotNull(types);
            assertTrue(types.hasNextType());
            log("\tProperty Types:");
            while (types.hasNextType()) {
                Type type = types.nextType();
                assertNotNull(type);
                log("\t" + formatType(type));
            }
        } catch (AgentException ae) {
            ae.printStackTrace();
            fail("AgentException" + ae.getMessage());
            return;
        } catch (SharedException se) {
            se.printStackTrace();
            fail("SharedException" + se.getMessage());
            return;
        }
        return;
    }

    public void testAgentManager_getGroupTypes() {
        log("testing AgentManager.getGroupTypes()");
        try {
            TypeIterator types = mgr.getGroupTypes();
            assertNotNull(types);
            assertTrue(types.hasNextType());
            log("\tGroup Types:");
            while (types.hasNextType()) {
                Type type = types.nextType();
                assertNotNull(type);
                log("\t" + formatType(type));
            }
        } catch (AgentException ae) {
            ae.printStackTrace();
            fail("AgentException" + ae.getMessage());
            return;
        } catch (SharedException se) {
            se.printStackTrace();
            fail("SharedException" + se.getMessage());
        }
        return;
    }

    public void testAgentManager_getAgentSearchTypes() {
        log("testing AgentManager.getAgentSearchTypes()");
        try {
            TypeIterator types = mgr.getAgentSearchTypes();
            assertNotNull(types);
            assertTrue(types.hasNextType());
            log("\tAgent Search Types:");
            while (types.hasNextType()) {
                Type type = types.nextType();
                assertNotNull(type);
                log("\t" + formatType(type));
            }
        } catch (AgentException ae) {
            ae.printStackTrace();
            fail("AgentException" + ae.getMessage());
            return;
        } catch (SharedException se) {
            se.printStackTrace();
            fail("SharedException" + se.getMessage());
        }
        return;
    }

    public void testAgentManager_getGroupSearchTypes() {
        log("testing AgentManager.getGroupSearchTypes()");
        try {
            TypeIterator types = mgr.getGroupSearchTypes();
            assertNotNull(types);
            assertTrue(types.hasNextType());
            log("\tGroup Search Types:");
            while (types.hasNextType()) {
                Type type = types.nextType();
                assertNotNull(type);
                log("\t" + formatType(type));
            }
        } catch (AgentException ae) {
            ae.printStackTrace();
            fail("AgentException" + ae.getMessage());
            return;
        } catch (SharedException se) {
            se.printStackTrace();
            fail("SharedException" + se.getMessage());
        }
        return;
    }

    public void test_getAgent() {
        log("testing AgentManager.getAgent()");
        try {
            Agent agent = mgr.getAgent(agentId);
            assertNotNull(agent);
            assertEquals(agentId, agent.getId());
            log("\tretrieved agent " + agent.getId());
            agentTest(agent);
        } catch (AgentException ae) {
            String msg = ae.getMessage();
            ae.printStackTrace();
            fail("AgentException" + ae.getMessage());
            return;
        } catch (SharedException se) {
            se.printStackTrace();
            fail("SharedException" + se.getMessage());
            return;
        }
        return;
    }

    public void testAgentManager_agent() {
        Agent agent;
        if (supportsAdmin != true) {
            log("skipping admin functions");
            return;
        }
        log("testing AgentManager.createAgent()");
        try {
            agent = mgr.createAgent(agentName, agentType, agentProperties);
            assertNotNull(agent);
        } catch (AgentException ae) {
            String msg = ae.getMessage();
            if (msg.equals(AgentException.UNIMPLEMENTED)) {
                log("***UNIMPLEMENTED***");
                return;
            } else {
                ae.printStackTrace();
                fail("AgentException" + ae.getMessage());
                return;
            }
        } catch (SharedException se) {
            se.printStackTrace();
            fail("SharedException" + se.getMessage());
            return;
        }
        log("testing AgentManager.getAgent()");
        try {
            Agent agent2 = mgr.getAgent(agent.getId());
            assertNotNull(agent2);
            assertEquals(agent.getId(), agent2.getId());
            log("\tretrieved agent " + agent.getId());
            agentTest(agent2);
        } catch (AgentException ae) {
            String msg = ae.getMessage();
            ae.printStackTrace();
            fail("AgentException" + ae.getMessage());
            return;
        } catch (SharedException se) {
            se.printStackTrace();
            fail("SharedException" + se.getMessage());
            return;
        }
        log("testing AgentManager.deleteAgent()");
        try {
            mgr.deleteAgent(agent.getId());
            log("\tdeleted agent " + (Id) agent.getId());
        } catch (AgentException ae) {
            String msg = ae.getMessage();
            if (msg.equals(AgentException.UNIMPLEMENTED)) {
                log("***UNIMPLEMENTED***");
            } else {
                ae.printStackTrace();
                fail("AgentException" + ae.getMessage());
                return;
            }
        } catch (SharedException se) {
            se.printStackTrace();
            fail("SharedException" + se.getMessage());
            return;
        }
        return;
    }

    private void agentTest(Agent agent) {
        log("\tAgent DisplayName:");
        try {
            String displayName = agent.getDisplayName();
            assertNotNull(displayName);
            log("\t\t" + displayName);
        } catch (AgentException ae) {
            String msg = ae.getMessage();
            ae.printStackTrace();
            fail("AgentException" + ae.getMessage());
            return;
        }
        log("\tAgent Type:");
        try {
            Type agentType = agent.getType();
            assertNotNull(agentType);
            log("\t" + formatType(agentType));
        } catch (AgentException ae) {
            String msg = ae.getMessage();
            ae.printStackTrace();
            fail("AgentException" + ae.getMessage());
            return;
        }
        log("\tAgent Property Types:");
        try {
            TypeIterator ti = agent.getPropertyTypes();
            assertNotNull(ti);
            assertTrue(ti.hasNextType());
            while (ti.hasNextType()) {
                Type type = ti.nextType();
                log("\t" + formatType(type));
                log("\t\ttesting Agent.getPropertiesByType()");
                org.osid.shared.Properties properties = agent.getPropertiesByType(type);
                assertNotNull(properties);
                Type pType = properties.getType();
                assertNotNull(pType);
                assertEquals(pType, type);
                org.osid.shared.ObjectIterator keys = properties.getKeys();
                assertNotNull(keys);
                assertTrue(keys.hasNextObject());
                log("\t\tProperties: ");
                while (keys.hasNextObject()) {
                    java.io.Serializable object = keys.nextObject();
                    assertNotNull(object);
                    java.io.Serializable property = properties.getProperty(object);
                    assertNotNull(property);
                    log("\t\t" + object + " " + property);
                }
            }
        } catch (AgentException ae) {
            String msg = ae.getMessage();
            ae.printStackTrace();
            fail("AgentException" + ae.getMessage());
            return;
        } catch (SharedException se) {
            se.printStackTrace();
            fail("SharedException" + se.getMessage());
            return;
        }
        log("testing Agent.getProperties()");
        try {
            org.osid.shared.PropertiesIterator pi = agent.getProperties();
            assertNotNull(pi);
            assertTrue(pi.hasNextProperties());
            while (pi.hasNextProperties()) {
                org.osid.shared.Properties properties = pi.nextProperties();
                assertNotNull(properties);
                Type pType = properties.getType();
                assertNotNull(pType);
                org.osid.shared.ObjectIterator keys = properties.getKeys();
                assertNotNull(keys);
                assertTrue(keys.hasNextObject());
                log("\t\tProperties: ");
                while (keys.hasNextObject()) {
                    java.io.Serializable object = keys.nextObject();
                    assertNotNull(object);
                    java.io.Serializable property = properties.getProperty(object);
                    assertNotNull(property);
                    log("\t\t" + object + " " + property);
                }
            }
        } catch (AgentException ae) {
            ae.printStackTrace();
            fail("AgentException" + ae.getMessage());
            return;
        } catch (SharedException se) {
            se.printStackTrace();
            fail("SharedException" + se.getMessage());
            return;
        }
        return;
    }

    public void testAgentManager_getAgents() {
        log("testing AgentManager.getAgents()");
        try {
            AgentIterator agents = mgr.getAgents();
            assertNotNull(agents);
            assertTrue(agents.hasNextAgent());
            log("\tAgents:");
            while (agents.hasNextAgent()) {
                Agent agent = agents.nextAgent();
                assertNotNull(agent);
                log("\t" + agent.getDisplayName() + " " + formatType(agent.getType()));
            }
        } catch (AgentException ae) {
            String msg = ae.getMessage();
            if (msg.equals(AgentException.UNIMPLEMENTED)) {
                log("***UNIMPLEMENTED***");
            } else {
                ae.printStackTrace();
                fail("AgentException" + ae.getMessage());
                return;
            }
        } catch (SharedException se) {
            se.printStackTrace();
            fail("SharedException" + se.getMessage());
            return;
        }
        return;
    }

    public void testAgentManager_getGroup() {
        log("testing AgentManager.getGroup()");
        try {
            Group group = mgr.getGroup(groupId);
            assertNotNull(group);
            assertEquals(groupId, group.getId());
            log("\tretrieved group " + group.getId());
            groupTest(group);
        } catch (AgentException ae) {
            ae.printStackTrace();
            fail("AgentException" + ae.getMessage());
            return;
        } catch (SharedException se) {
            se.printStackTrace();
            fail("SharedException" + se.getMessage());
            return;
        }
        return;
    }

    public void testAgentManager_group() {
        Group group;
        if (supportsAdmin != true) {
            log("skipping admin functions");
            return;
        }
        log("testing AgentManager.createGroup()");
        try {
            group = mgr.createGroup(groupName, groupType, "testing 1", groupProperties);
            assertNotNull(group);
        } catch (AgentException ae) {
            String msg = ae.getMessage();
            if (msg.equals(AgentException.UNIMPLEMENTED)) {
                log("***UNIMPLEMENTED***");
                return;
            } else {
                ae.printStackTrace();
                fail("AgentException" + ae.getMessage());
                return;
            }
        } catch (SharedException se) {
            se.printStackTrace();
            fail("SharedException" + se.getMessage());
            return;
        }
        log("testing AgentManager.getGroup()");
        try {
            Group group2 = mgr.getGroup(group.getId());
            assertNotNull(group2);
            assertEquals(group.getId(), group2.getId());
            log("\tretrieved agent " + group.getId());
            groupTest(group2);
        } catch (AgentException ae) {
            String msg = ae.getMessage();
            ae.printStackTrace();
            fail("AgentException" + ae.getMessage());
            return;
        } catch (SharedException se) {
            se.printStackTrace();
            fail("SharedException" + se.getMessage());
            return;
        }
        log("testing Group.updateDescription()");
        try {
            group.updateDescription("testing 2");
            String desc = group.getDescription();
            assertNotNull(desc);
            assertEquals(desc, "testing 2");
        } catch (AgentException ae) {
            String msg = ae.getMessage();
            ae.printStackTrace();
            fail("AgentException" + ae.getMessage());
            return;
        } catch (SharedException se) {
            se.printStackTrace();
            fail("SharedException" + se.getMessage());
            return;
        }
        log("testing AgentManager.deleteGroup()");
        try {
            mgr.deleteAgent(group.getId());
            log("\tdeleted group " + (Id) group.getId());
        } catch (AgentException ae) {
            String msg = ae.getMessage();
            if (msg.equals(AgentException.UNIMPLEMENTED)) {
                log("***UNIMPLEMENTED***");
            } else {
                ae.printStackTrace();
                fail("AgentException" + ae.getMessage());
                return;
            }
        } catch (SharedException se) {
            se.printStackTrace();
            fail("SharedException" + se.getMessage());
            return;
        }
        return;
    }

    private void groupTest(Group group) {
        try {
            log("testing Group.getId()");
            Id id = group.getId();
            assertNotNull(id);
            assertEquals(id, group.getId());
            log("\t" + id.getIdString());
            log("testing Group.getDisplayName()");
            String name = group.getDisplayName();
            assertNotNull(name);
            log("\t" + name);
            log("testing Group.getDescription()");
            assertNotNull(group);
            String desc = group.getDescription();
            assertNotNull(desc);
            log("\tdescription=" + desc);
            log("testing Group.getType()");
            Type type = group.getType();
            assertNotNull(type);
            log("\t" + formatType(type));
        } catch (AgentException ae) {
            ae.printStackTrace();
            fail("AgentException" + ae.getMessage());
            return;
        } catch (SharedException se) {
            se.printStackTrace();
            fail("SharedException" + se.getMessage());
            return;
        }
        return;
    }

    public void testAgentManager_getGroups() {
        log("testing AgentManager.getGroups()");
        try {
            AgentIterator groups = mgr.getGroups();
            assertNotNull(groups);
            assertTrue(groups.hasNextAgent());
            log("\tGroups:");
            while (groups.hasNextAgent()) {
                Group group = (Group) groups.nextAgent();
                assertNotNull(group);
                log("\t" + group.getDisplayName() + " " + formatType(group.getType()));
            }
        } catch (AgentException ae) {
            String msg = ae.getMessage();
            if (msg.equals(AgentException.UNIMPLEMENTED)) {
                log("***UNIMPLEMENTED***");
            } else {
                ae.printStackTrace();
                fail("AgentException" + ae.getMessage());
                return;
            }
        } catch (SharedException se) {
            se.printStackTrace();
            fail("SharedException" + se.getMessage());
            return;
        }
        return;
    }

    public void testAgentManager_getAgentsByType() {
        log("testing AgentManager.getAgentsByType()");
        try {
            TypeIterator types = mgr.getAgentTypes();
            assertNotNull(types);
            assertTrue(types.hasNextType());
            log("\tAgent Types:");
            while (types.hasNextType()) {
                Type type = types.nextType();
                assertNotNull(type);
                log("\t" + formatType(type));
                AgentIterator agents = mgr.getAgentsByType(type);
                assertNotNull(agents);
                assertTrue(agents.hasNextAgent());
                while (agents.hasNextAgent()) {
                    Agent agent = agents.nextAgent();
                    assertNotNull(agent);
                    log("\t\t" + agent.getDisplayName() + " " + formatType(agent.getType()));
                }
            }
        } catch (AgentException ae) {
            String msg = ae.getMessage();
            if (msg.equals(AgentException.UNIMPLEMENTED)) {
                log("***UNIMPLEMENTED***");
            } else {
                ae.printStackTrace();
                fail("AgentException" + ae.getMessage());
                return;
            }
        } catch (SharedException se) {
            se.printStackTrace();
            fail("SharedException" + se.getMessage());
            return;
        }
        return;
    }

    public void testAgentManager_getGroupsByType() {
        log("testing AgentManager.getGroupsByType()");
        try {
            TypeIterator types = mgr.getGroupTypes();
            assertNotNull(types);
            assertTrue(types.hasNextType());
            log("\tGroup Types:");
            while (types.hasNextType()) {
                Type type = types.nextType();
                assertNotNull(type);
                log("\t" + formatType(type));
                AgentIterator groups = mgr.getGroupsByType(type);
                assertNotNull(groups);
                assertTrue(groups.hasNextAgent());
                while (groups.hasNextAgent()) {
                    Group group = (Group) groups.nextAgent();
                    assertNotNull(group);
                    log("\t\t" + group.getDisplayName() + " " + formatType(group.getType()));
                }
            }
        } catch (AgentException ae) {
            if (ae.getMessage().equals(AgentException.UNIMPLEMENTED)) {
                log("***UNIMPLEMENTED***");
            } else {
                ae.printStackTrace();
                fail("AgentException" + ae.getMessage());
                return;
            }
        } catch (SharedException se) {
            se.printStackTrace();
            fail("SharedException" + se.getMessage());
            return;
        }
        return;
    }

    private String formatType(Type type) {
        return (type.getDomain() + "/" + type.getKeyword() + "@" + type.getAuthority() + ": " + type.getDescription());
    }

    private void log(String msg) {
        System.out.println(msg);
    }
}
