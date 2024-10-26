package examples.wslauncher4;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.rmi.RemoteException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Vector;
import javax.xml.namespace.QName;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.transport.http.HTTPSender;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import de.fhg.igd.semoa.security.AgentStructure;
import de.fhg.igd.semoa.server.AgentLauncher;
import de.fhg.igd.semoa.server.Environment;
import de.fhg.igd.semoa.server.IllegalAgentException;
import de.fhg.igd.semoa.service.AbstractService;
import de.fhg.igd.semoa.webservice.Webservice;
import de.fhg.igd.semoa.webservice.WebserviceService;
import de.fhg.igd.semoa.webservice.uddi.UddiService;
import de.fhg.igd.util.MemoryResource;
import de.fhg.igd.util.Resource;
import de.fhg.igd.util.WhatIs;

/** 
 * This service launches agent(s) via webservice.
 * Dependent of the given XML-based control structure one agent
 * migrates to the given list of web service delegates in a sequencial
 * manner resp. agents migrate to the given web service delegates in
 * parallel (see methode <code>launch()</code>).
 * 
 * @author C. Nickel, M. Sommer, J. Peters
 * @version $Id: WebserviceLauncherImpl.java 1913 2007-08-08 02:41:53Z jpeters $
 */
public class WebserviceLauncherImpl extends AbstractService implements WebserviceLauncher {

    /**
     * Length of the token which is used to synchronize agent and webservice. 
     * The token is a random string.
     */
    private static int TOKEN_LENGTH = 10;

    /**
     * The class of the agent which shall be launched
     */
    private static String AGENT_ = "examples.wslauncher4.Agent";

    /**
     * Additional package prefixes which shall not be imported into the agent
     */
    private static String ADDITIONAL_EXCLUDES = "org.apache.*";

    /**
     * Timeout for each thread to wait for the agent's return.
     */
    private long TIMEOUT = 100000;

    /**
     * The syncmap_ is needed to coordinate the webservices and the agents. 
     * When several webservices are running it ensures that the agent gives 
     * his response to the right one. 
     * The keys in the syncmap_ are the different tokens which are also stored
     * in the properties of the agent. The values are the answers reported by
     * the agents.
     */
    private Map syncmap_;

    /**
     * Lock object for synchronization.
     */
    private Object lock_;

    /** 
     * Constructor
     */
    public WebserviceLauncherImpl() {
        syncmap_ = new HashMap();
        lock_ = new Object();
    }

    /**
     * This method generates a token and stores it in the agent's properties
     * as well as in the syncmap_. 
     * It launches the agent named in AGENT_.
     * Webservice and agent are synchronized via the token.
     * To combine the request with the answer, the answer is assigned to the 
     * token in the syncmap_.
     * The xmlDocument must be valid. It must contain the delegation properties.
     * 
     * Example:
     * 
     * <xml>
     * <conf>
     *    <ws-flow>concurrent</ws-flow>
     *    <ws-delegate>
     *        <ws-host>raw://146.140.8.144:47471</ws-host>
     *        <ws-url>http://PC1019:8080/webservice/services/Hello</ws-url>
     *        <ws-method>sayHello</ws-method>
     *        <ws-param><id>12345</id></ws-param>
     *    </ws-delegate>
     * </conf>
     * <tag>content</tag>
     * </xml>
     * 
     * The whole document except the conf-tag content will be the first 
     * parameter of the webservice.
     * 
     * Within the conf-tag the delegation properties are defined.
     *  ws-flow:     either "concurrent" (n Agent for n delegation - parallel)
     *               or "roundtrip" (1 Agent for n delegation - serial)
     *  ws-delegate: defines one webservice delegation as follow
     *  ws-host:     SeMoA-conform address of the target host the agent should 
     *               jump to
     *  ws-url:      the webservice the agent should call from defined ws-host
     *  ws-method:   method of the webservice to call
     *  ws-param:    further parameters for the method; all parameter values 
     *               must be inside a named tag even the name has no further 
     *               relevance; parameters will be handed to the method in the 
     *               given order
     * 
     * @param xmlDocument 
     *   the xmlDocument that contains all informations - the calling
     *   parameters and the ws-delegation
     * 
     * @see examples.wslauncher4.WebserviceLauncher#launch(String)
     */
    public void launch(String xmlControl, String xmlDoc, long docId) {
        AgentLauncher l;
        Environment env;
        Properties prop;
        Resource res;
        String token;
        String deflt;
        String answ;
        String key;
        String entry;
        NodeList flow;
        InputSource xmlcontrolstream;
        TreeMap results;
        int nrOfAgents = 0;
        synchronized (lock_) {
            if (xmlControl == null || xmlControl.length() == 0 || xmlDoc == null || xmlDoc.length() == 0) {
                System.out.println("---- Need control AND XML document! ----");
                return;
            }
            Vector v_delegations_host = new Vector();
            Vector v_delegations_url = new Vector();
            Vector v_delegations_method = new Vector();
            xmlcontrolstream = new InputSource(new StringReader(xmlControl));
            NodeList destinations = SimpleXMLParser.parseDocument(xmlcontrolstream, AgentBehaviour.XML_DELEGATE);
            for (int i = 0; i < destinations.getLength(); i++) {
                if (destinations.item(i).getTextContent() != null && destinations.item(i).getTextContent().length() > 0) {
                    System.out.println(destinations.item(i).getTextContent());
                    entry = SimpleXMLParser.findChildEntry(destinations.item(i), AgentBehaviour.XML_HOST);
                    v_delegations_host.add(entry);
                    entry = SimpleXMLParser.findChildEntry(destinations.item(i), AgentBehaviour.XML_URL);
                    v_delegations_url.add(entry);
                    entry = SimpleXMLParser.findChildEntry(destinations.item(i), AgentBehaviour.XML_METHOD);
                    v_delegations_method.add(entry);
                }
            }
            xmlcontrolstream = new InputSource(new StringReader(xmlControl));
            flow = SimpleXMLParser.parseDocument(xmlcontrolstream, AgentBehaviour.XML_FLOW);
            if (flow.item(0).getTextContent() != null && flow.item(0).getTextContent().equals(AgentBehaviour.WS_PERFORM_ROUNDTRIP)) {
                nrOfAgents = 1;
            } else if (flow.item(0).getTextContent() != null && flow.item(0).getTextContent().equals(AgentBehaviour.WS_PERFORM_CONCURRENT)) {
                nrOfAgents = v_delegations_host.size();
            }
            token = "";
            results = new TreeMap();
            for (int agentCounter = 0; agentCounter < nrOfAgents; agentCounter++) {
                for (int i = 0; i < TOKEN_LENGTH; i++) {
                    token = token + (char) (Math.random() * 26 + 65);
                }
                results.put(token, null);
                prop = AgentStructure.defaults();
                prop.setProperty(AgentStructure.PROP_AGENT_CLASS, AGENT_);
                prop.setProperty(AgentBehaviour.CTX_DOCID, String.valueOf(docId));
                prop.setProperty(AgentBehaviour.CTX_XML, xmlDoc);
                prop.setProperty("token", token);
                deflt = prop.getProperty(AgentStructure.PROP_AGENT_EXCLUDE);
                prop.setProperty(AgentStructure.PROP_AGENT_EXCLUDE, deflt + ":" + ADDITIONAL_EXCLUDES);
                if (nrOfAgents == 1) {
                    for (int i = 0; i < v_delegations_host.size(); i++) {
                        prop.setProperty(i + "." + AgentBehaviour.XML_HOST, (String) v_delegations_host.elementAt(i));
                        prop.setProperty(i + "." + AgentBehaviour.XML_URL, (String) v_delegations_url.elementAt(i));
                        prop.setProperty(i + "." + AgentBehaviour.XML_METHOD, (String) v_delegations_method.elementAt(i));
                    }
                } else {
                    prop.setProperty(0 + "." + AgentBehaviour.XML_HOST, (String) v_delegations_host.elementAt(agentCounter));
                    prop.setProperty(0 + "." + AgentBehaviour.XML_URL, (String) v_delegations_url.elementAt(agentCounter));
                    prop.setProperty(0 + "." + AgentBehaviour.XML_METHOD, (String) v_delegations_method.elementAt(agentCounter));
                }
                res = new MemoryResource();
                env = Environment.getEnvironment();
                key = WhatIs.stringValue(AgentLauncher.WHATIS);
                l = (AgentLauncher) env.lookup(key);
                if (l == null) {
                    System.out.println("Can't find the agent launcher");
                    return;
                }
                try {
                    l.launchAgent(res, prop);
                } catch (IllegalAgentException ex) {
                    System.out.println(ex);
                } catch (GeneralSecurityException ex) {
                    System.out.println(ex);
                } catch (IOException ex) {
                    System.out.println(ex);
                }
            }
            syncmap_.put(token, results);
            System.out.println("----- TOKEN = " + token + "------");
        }
        try {
            synchronized (token) {
                token.wait(TIMEOUT);
                Map m_results = (Map) syncmap_.get(token);
                Collection c_results = m_results.values();
                String[] sa_results = (String[]) c_results.toArray(new String[0]);
                answ = "";
                for (int j = 0; j < sa_results.length; j++) {
                    answ = answ + sa_results[j];
                }
                syncmap_.remove(token);
                System.out.println("----- " + answ + " -----");
                callbackWS(xmlControl, answ, docId);
            }
        } catch (InterruptedException ex) {
            System.out.println(ex);
        }
    }

    /** 
     * Checks if the token is a key in the syncmap. The method 
     * then stores the answer in the resultmap under this key.
     * It notifies the waiting webservice if and only if all results
     * in the map are not null e.g. if all agents of the webservice-call
     * did already receive.
     * 
     * @param answ
     *             answer from the agent
     * @param token
     *             token for the synchronization of the agent and 
     *             the    webservice launcher
     */
    public void response(String answ, String token) {
        synchronized (lock_) {
            Iterator i;
            String key;
            if (token == null) {
                System.out.println("Token is NULL but shouldnt!");
                return;
            }
            if (answ == null) {
                System.out.println("I do not have an answer");
                return;
            }
            i = syncmap_.keySet().iterator();
            while (i.hasNext()) {
                key = (String) i.next();
                if (key.contains(token)) {
                    Map results = (Map) syncmap_.get(key);
                    results.put(token, answ);
                    syncmap_.put(key, results);
                    if (!results.containsValue(null)) {
                        synchronized (key) {
                            key.notify();
                            System.out.println("----- I have notified " + key + "! -----");
                        }
                    }
                    return;
                }
            }
            System.out.println("Sorry, could not find a matching token.");
        }
    }

    private void callbackWS(String xmlControl, String ws_results, long docId) {
        SimpleProvider config;
        Service service;
        Object ret;
        Call call;
        Object[] parameter;
        String method;
        String wsurl;
        URL url;
        NodeList delegateNodes;
        Node actualNode;
        InputSource xmlcontrolstream;
        try {
            xmlcontrolstream = new InputSource(new StringReader(xmlControl));
            delegateNodes = SimpleXMLParser.parseDocument(xmlcontrolstream, AgentBehaviour.XML_CALLBACK);
            actualNode = delegateNodes.item(0);
            wsurl = SimpleXMLParser.findChildEntry(actualNode, AgentBehaviour.XML_URL);
            method = SimpleXMLParser.findChildEntry(actualNode, AgentBehaviour.XML_METHOD);
            if (wsurl == null || method == null) {
                System.out.println("----- Did not get method or wsurl from the properties! -----");
                return;
            }
            url = new java.net.URL(wsurl);
            try {
                url.openConnection().connect();
            } catch (IOException ex) {
                System.out.println("----- Could not connect to the webservice! -----");
            }
            Vector v_param = new Vector();
            v_param.add(ws_results);
            v_param.add(new Long(docId));
            parameter = v_param.toArray();
            config = new SimpleProvider();
            config.deployTransport("http", new HTTPSender());
            service = new Service(config);
            call = (Call) service.createCall();
            call.setTargetEndpointAddress(url);
            call.setOperationName(new QName("http://schemas.xmlsoap.org/soap/encoding/", method));
            try {
                ret = call.invoke(parameter);
                if (ret == null) {
                    ret = new String("No response from callback function!");
                }
                System.out.println("Callback function returned: " + ret);
            } catch (RemoteException ex) {
                System.out.println("----- Could not invoke the method! -----");
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    public String info() {
        return "This service launches an agent via webservice.";
    }

    public String author() {
        return "Martin Sommer";
    }

    public String revision() {
        return "$id$";
    }

    public static void main(String argv[]) throws Exception {
        WebserviceLauncher wsl;
        WebserviceService wss;
        UddiService uddi;
        Environment env;
        Webservice ws;
        env = Environment.getEnvironment();
        wss = (WebserviceService) env.lookup(WhatIs.stringValue(WebserviceService.WHATIS));
        wsl = new WebserviceLauncherImpl();
        env.publish(WhatIs.stringValue(WHATIS), wsl);
        ws = wss.deployWebservice(WS_NAME, wsl, new Class[] { WebserviceLauncher.class });
        if (argv != null && argv.length == 1 && argv[1].compareToIgnoreCase("uddi") == 0) {
            try {
                uddi = (UddiService) env.lookup(UddiService.WHATIS);
                uddi.registerWebserviceDescription(ws.getDescription());
            } catch (Exception e) {
                System.out.println("[" + WHATIS + "] Could not register web service via UDDI");
            }
        }
    }
}
