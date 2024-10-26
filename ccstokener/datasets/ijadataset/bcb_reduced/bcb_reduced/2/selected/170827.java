package org.rascalli.nebulatest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rascalli.framework.config.ConfigService;
import org.rascalli.framework.core.Agent;
import org.rascalli.framework.core.AgentConfiguration;
import org.rascalli.framework.core.AgentFactory;
import org.rascalli.framework.core.User;
import org.rascalli.framework.eca.NebulaCommunicationProtocol;
import org.rascalli.framework.eca.UserConnected;
import org.rascalli.framework.eca.UserDisconnected;
import org.rascalli.framework.eca.UserPraise;
import org.rascalli.framework.eca.UserScolding;
import org.rascalli.framework.eca.UserUtterance;
import org.rascalli.framework.event.Event;
import org.rascalli.framework.net.tcp.ClosedConnectionException;
import org.rascalli.framework.rss.FeedEntry;

/**
 * <p>
 * 
 * </p>
 * 
 * <p>
 * <b>Company:&nbsp;</b> SAT, Research Studios Austria
 * </p>
 * 
 * <p>
 * <b>Copyright:&nbsp;</b> (c) 2007
 * </p>
 * 
 * <p>
 * <b>last modified:</b><br/> $Author: christian $<br/> $Date: 2007-11-28
 * 10:21:23 +0100 (Mi, 28 Nov 2007) $<br/> $Revision: 2447 $
 * </p>
 * 
 * @author Christian Schollum
 */
public class EcaTestAgent implements Agent {

    private final Log log = LogFactory.getLog(getClass());

    private final int id;

    private final User user;

    private final ConfigService configService;

    private NebulaCommunicationProtocol nebula;

    private String defaultResponseHeader;

    private String defaultResponseContent;

    private String defaultResponseFooter;

    private String praiseResponse;

    private String scoldingResponse;

    private final Map<String, String> textResponses = new HashMap<String, String>();

    private Map<String, Object> properties;

    private final AgentFactory factory;

    /**
	 * @param user2
	 * @param id2
	 * @param name2
	 * @param properties
	 */
    public EcaTestAgent(User user, AgentConfiguration spec, ConfigService configService, AgentFactory factory) {
        this.user = user;
        this.factory = factory;
        this.id = spec.getAgentId();
        this.properties = new HashMap<String, Object>(spec.getAgentProperties());
        this.configService = configService;
    }

    public String getName() {
        return (String) getProperty(P_AGENT_NAME);
    }

    public void handleEvent(Event event) {
        if (event instanceof UserConnected) {
            nebula = ((UserConnected) event).getNebulaCommunicationProtocol();
        } else if (event instanceof UserDisconnected) {
            nebula = null;
        } else if (event instanceof UserUtterance) {
            String text = ((UserUtterance) event).getText();
            String response = textResponses.get(normalize(text));
            if (response == null) {
                sendDefaultResponse(text);
            } else {
                sendResponse(response);
            }
        } else if (event instanceof UserPraise) {
            sendResponse(praiseResponse);
        } else if (event instanceof UserScolding) {
            sendResponse(scoldingResponse);
        }
    }

    private void sendResponse(String response) {
        if (response == null) {
            sendDefaultResponse("Undefined test case.");
        } else {
            if (nebula == null) {
                log.warn("cannot send response: client not connected");
            } else {
                log.info("sending response:\n" + response);
                try {
                    nebula.sendMultimodalOutput(response);
                } catch (ClosedConnectionException e) {
                    log.warn("cannot send response", e);
                }
            }
        }
    }

    private void sendDefaultResponse(String text) {
        StringBuffer response = new StringBuffer();
        response.append(defaultResponseHeader);
        response.append(defaultResponseContent == null ? "You said " + text : defaultResponseContent);
        response.append(defaultResponseFooter);
        sendResponse(response.toString());
    }

    private String stripWhiteSpaceFromString(String str) {
        return (str != null ? str.replaceAll(">\\s+", ">").replaceAll("\\s+<", "<") : null);
    }

    /**
	 * 
	 */
    private void loadTestCaseConfig() {
        Properties testCases = new Properties();
        try {
            final URL url = configService.getConfigFileUrl(getTestCaseConfigFile());
            final InputStream inputStream = url.openStream();
            log.info("loading test case config from" + url.toExternalForm());
            testCases.loadFromXML(inputStream);
        } catch (IOException e) {
            log.error("cannot load test case config from file: " + getTestCaseConfigFile(), e);
        }
        praiseResponse = extractTestCase("praise", testCases);
        scoldingResponse = extractTestCase("scolding", testCases);
        defaultResponseHeader = extractTestCase("defaultResponseHeader", testCases);
        defaultResponseContent = extractTestCase("defaultResponseContent", testCases);
        defaultResponseFooter = extractTestCase("defaultResponseFooter", testCases);
        textResponses.clear();
        for (Map.Entry<Object, Object> entry : testCases.entrySet()) {
            textResponses.put(normalize((String) entry.getKey()), stripWhiteSpaceFromString((String) entry.getValue()));
        }
    }

    /**
	 * @param key
	 * @return
	 */
    private String normalize(String str) {
        return str.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private String extractTestCase(String key, Properties testCases) {
        final String testCase = (String) testCases.remove(key);
        if (testCase == null) {
            if (log.isWarnEnabled()) {
                log.warn("required test case '" + key + "' not defined");
            }
        }
        return stripWhiteSpaceFromString(testCase);
    }

    public void start() {
        loadTestCaseConfig();
    }

    public void stop() {
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public int getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void feedEntryReceived(FeedEntry entry) {
    }

    public void update(AgentConfiguration spec) {
        properties = new HashMap<String, Object>(spec.getAgentProperties());
        loadTestCaseConfig();
    }

    /**
	 * @return the testCaseConfigFile
	 */
    private String getTestCaseConfigFile() {
        return (String) getProperty(EcaTestAgentFactory.P_TEST_CASE_CONFIG);
    }

    public String getAgentFactoryId() {
        return factory.getId();
    }
}
