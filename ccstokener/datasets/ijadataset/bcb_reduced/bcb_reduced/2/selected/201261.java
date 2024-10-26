package fi.hip.gb.mobile;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Random;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.annotation.AnnotationElement;
import org.jboss.aop.joinpoint.ConstructorInvocation;
import org.jboss.aop.joinpoint.Invocation;
import fi.hip.gb.core.Config;
import fi.hip.gb.core.WorkDescription;
import fi.hip.gb.net.ClientSession;
import fi.hip.gb.net.SessionFactory;
import fi.hip.gb.net.discovery.DiscoveryPacket;
import fi.hip.gb.net.rpc.DiscoveryService;
import fi.hip.gb.server.scheduler.Scheduler;

/**
 * Manages agent sessions started from the client. Only one instance per VM.
 * <p>
 * Generates new agent session or gives automatically the same singleton instances
 * depending of the existence of <code>Singleton</code> annotation.
 * 
 * @author Juho Karppinen
 */
public class AgentManager implements Interceptor {

    /** known sessions at the moment */
    private static Hashtable<Object, ClientSession> sessions = new Hashtable<Object, ClientSession>();

    /** all singleton instances */
    private static Hashtable<String, Object> singletons = new Hashtable<String, Object>();

    /** list of known servers */
    private static DiscoveryPacket[] availableHosts = null;

    /** random generator for scheduler */
    private static Random random = new Random();

    private static Log log = LogFactory.getLog(AgentManager.class);

    public AgentManager() {
        Config.getInstance();
    }

    public String getName() {
        return "AgentManager";
    }

    /**
     * Refresh the list of available servers.
     * @param discoveryURL server to be asked
     */
    private void performDiscovery(String discoveryURL) {
        try {
            DiscoveryService discovery = new DiscoveryService(discoveryURL);
            availableHosts = discovery.listServices();
            log.info("Found " + availableHosts.length + " GBAgent servers");
        } catch (RemoteException re) {
            log.error("Failed to fetch the list of servers" + re.getMessage(), re);
        }
        if (availableHosts == null || availableHosts.length == 0) {
            availableHosts = new DiscoveryPacket[] { new DiscoveryPacket(discoveryURL) };
        }
    }

    public Object invoke(Invocation invocation) throws Throwable {
        try {
            ConstructorInvocation ci = (ConstructorInvocation) invocation;
            String name = ci.getConstructor().getName();
            MobileAgent mobileAgent = new MobileAgentProperties(ci.getConstructor().getDeclaringClass(), "mobileagent.properties");
            if (mobileAgent.disabled()) {
                return invocation.invokeNext();
            }
            if (availableHosts == null) performDiscovery(mobileAgent.discoveryURL());
            boolean hasUndeployer = false;
            for (Method m : ci.getConstructor().getDeclaringClass().getMethods()) {
                if (AnnotationElement.getAnyAnnotation(m, AgentUndeployer.class) != null) {
                    hasUndeployer = true;
                    break;
                }
            }
            if (mobileAgent.singleton() || mobileAgent.jobID() != -1) {
                if (mobileAgent.jobID() != -1) {
                    name += ":" + Long.toString(mobileAgent.jobID());
                    log.debug("jobid is defined " + name);
                }
                if (singletons.containsKey(name)) {
                    System.out.println("-------- Local agent: " + name + " ----------- ");
                    return singletons.get(name);
                }
                for (DiscoveryPacket dp : availableHosts) {
                    ClientSession s = SessionFactory.createClientConnection(dp.getServiceURL(), Config.getWorkingDir(null));
                    log.info("session opened to " + dp.getServiceURL() + " with name " + name);
                    for (Long id : s.getJoblist(Boolean.TRUE, name)) {
                        WorkDescription wds = s.getDescription(id);
                        s.init(wds);
                        if (mobileAgent.singleton() && Boolean.parseBoolean(wds.flags().getProperty(Scheduler.SINGLETON, "0")) || id.equals(mobileAgent.jobID())) {
                            System.out.println("-------- Remote agent: " + name + " ----------- ");
                            Object agentObject = invocation.invokeNext();
                            sessions.put(agentObject, s);
                            singletons.put(name, agentObject);
                            return agentObject;
                        }
                    }
                }
            }
            String targetService = mobileAgent.serviceURL();
            if (targetService.length() == 0) {
                targetService = getRandomHost();
            }
            WorkDescription wds = new WorkDescription(targetService);
            wds.getExecutable().setClassName(ci.getConstructor().getName());
            wds.attachFile(new URL(mobileAgent.jar()));
            if (mobileAgent.jobID() != -1) {
                wds.setJobID(new Long[] { mobileAgent.jobID() });
            }
            System.out.println("--------New agent: " + name + " ---- ID: " + Arrays.toString(wds.getJobID()) + " into " + targetService);
            if (hasUndeployer) {
                wds.flags().put(Scheduler.PERMANENT, "true");
            }
            if (mobileAgent.singleton()) {
                wds.flags().put(Scheduler.SINGLETON, "true");
            }
            if (mobileAgent.stickySessions() == false) {
                wds.flags().put(Scheduler.AUTO_MIGRATE, "true");
            }
            if (ci.getArguments() != null) {
                wds.getExecutable().putParameters(ci.getArguments());
                log.debug("constructor call with parameters " + wds.getExecutable().printCallDebug());
            }
            ClientSession s = SessionFactory.createClientConnection(Config.getWorkingDir(wds.currentID()), wds);
            s.dispatch(wds);
            try {
                while (s.getResult(wds.currentID(), new String[0], Boolean.FALSE).getChildren().length == 0) {
                    if (s.getStatus(wds.currentID()).getError() != null) {
                        throw new RemoteException(s.getStatus().getError());
                    }
                    Thread.sleep(100);
                }
                log.debug("constructor returned " + Arrays.toString(s.getResult().getChildren()));
            } catch (Exception e) {
                log.error("Failed to initiliase agent", e);
            }
            Object agentObject = invocation.invokeNext();
            sessions.put(agentObject, s);
            if (mobileAgent.singleton() || mobileAgent.jobID() != -1) {
                singletons.put(name, agentObject);
            }
            return agentObject;
        } finally {
            log.debug("agent created");
        }
    }

    /**
     * Gets a session object for an agent.
     * @param agentObject agent instance
     * @return agent session, always found if agent's constructor is annotated
     */
    public static ClientSession getSession(Object agentObject) {
        return sessions.get(agentObject);
    }

    /**
     * Removes an agent session permanently.
     * @param agentObject agent instance
     */
    public static void removeSession(Object agentObject) {
        sessions.remove(agentObject);
        for (String key : singletons.keySet()) {
            if (singletons.get(key) == agentObject) {
                singletons.remove(key);
            }
        }
    }

    /**
     * Gives the next random host from known services.
     * @return URL to the service
     */
    public static String getRandomHost() {
        String targetService = availableHosts[random.nextInt(availableHosts.length)].getServiceURL();
        log.debug("scheduler server " + targetService + " out of " + availableHosts.length + " hosts");
        return targetService;
    }

    /**
     * Gets the properties for current MobileAgent. Property file overrides the defaults 
     * from source code annotations. If environmental variables are found with the
     * parameter name, they get the highest priority.
     */
    class MobileAgentProperties implements MobileAgent {

        public static final String DISCOVERYURL = "discoveryurl";

        public static final String JOBID = "jobid";

        public static final String JAR = "jar";

        public static final String SINGLETON = "singleton";

        public static final String REMOVEONERROR = "removeonerror";

        public static final String RETRIES = "retries";

        public static final String STICKYSESSION = "stickySession";

        public static final String SERVICEURL = "serviceurl";

        public static final String DISABLED = "disabled";

        Properties props = new Properties();

        MobileAgent defaults = null;

        public MobileAgentProperties(Class declaringClass, String propertyFile) throws IOException {
            this.defaults = (MobileAgent) AnnotationElement.getAnyAnnotation(declaringClass, MobileAgent.class);
            URL url = getClass().getClassLoader().getResource(propertyFile);
            if (url != null) {
                props.load(url.openStream());
                log.info("MobileAgent parameters loaded from file " + url);
            }
        }

        public String discoveryURL() {
            if (System.getProperty(DISCOVERYURL, "").length() > 0) return System.getProperty(DISCOVERYURL); else if (props.getProperty(DISCOVERYURL, "").length() > 0) return props.getProperty(DISCOVERYURL); else return defaults.discoveryURL();
        }

        public long jobID() {
            if (System.getProperty(JOBID, "").length() > 0) return Long.parseLong(System.getProperty(JOBID)); else if (props.getProperty(JOBID, "").length() > 0) return Long.parseLong(props.getProperty(JOBID)); else return defaults.jobID();
        }

        public String jar() {
            String jars = "";
            if (System.getProperty(JAR, "").length() > 0) jars = System.getProperty(JAR); else if (props.getProperty(JAR, "").length() > 0) jars = props.getProperty(JAR); else jars = defaults.jar();
            jars = "jar:" + jars + "!/";
            System.out.println("JARSSS" + jars);
            return jars;
        }

        public boolean singleton() {
            if (System.getProperty(SINGLETON, "").length() > 0) return Boolean.parseBoolean(System.getProperty(SINGLETON)); else if (props.getProperty(SINGLETON, "").length() > 0) return Boolean.parseBoolean(props.getProperty(SINGLETON)); else return defaults.singleton();
        }

        public boolean removeOnError() {
            if (System.getProperty(REMOVEONERROR, "").length() > 0) return Boolean.parseBoolean(System.getProperty(REMOVEONERROR)); else if (props.getProperty(REMOVEONERROR, "").length() > 0) return Boolean.parseBoolean(props.getProperty(REMOVEONERROR)); else return defaults.removeOnError();
        }

        public int retries() {
            if (System.getProperty(RETRIES, "").length() > 0) return Integer.parseInt(System.getProperty(RETRIES)); else if (props.getProperty(RETRIES, "").length() > 0) return Integer.parseInt(props.getProperty(RETRIES)); else return defaults.retries();
        }

        public boolean stickySessions() {
            if (System.getProperty(STICKYSESSION, "").length() > 0) return Boolean.parseBoolean(System.getProperty(STICKYSESSION)); else if (props.getProperty(STICKYSESSION, "").length() > 0) return Boolean.parseBoolean(props.getProperty(STICKYSESSION)); else return defaults.stickySessions();
        }

        public String serviceURL() {
            if (System.getProperty(SERVICEURL, "").length() > 0) return System.getProperty(SERVICEURL); else if (props.getProperty(SERVICEURL, "").length() > 0) return props.getProperty(SERVICEURL); else return defaults.serviceURL();
        }

        public boolean disabled() {
            if (System.getProperty(DISABLED, "").length() > 0) return Boolean.parseBoolean(System.getProperty(DISABLED)); else if (props.getProperty(DISABLED, "").length() > 0) return Boolean.parseBoolean(props.getProperty(DISABLED)); else return defaults.disabled();
        }

        public Class<? extends Annotation> annotationType() {
            return null;
        }
    }
}
