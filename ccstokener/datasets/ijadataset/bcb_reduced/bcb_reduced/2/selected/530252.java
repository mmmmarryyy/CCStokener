package examples.wslauncher4;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import javax.xml.namespace.QName;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.transport.http.HTTPSender;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import de.fhg.igd.amoa.MigrationTransition;
import de.fhg.igd.jhsm.AbstractAction;
import de.fhg.igd.jhsm.Context;
import de.fhg.igd.jhsm.FinalState;
import de.fhg.igd.jhsm.HSM;
import de.fhg.igd.jhsm.HSMState;
import de.fhg.igd.jhsm.HSMTransition;
import de.fhg.igd.jhsm.InitialState;
import de.fhg.igd.jhsm.State;
import de.fhg.igd.semoa.hsmagent.actions.LocalAddressAction;
import de.fhg.igd.semoa.hsmagent.actions.PrepareHomeMigrationAction;
import de.fhg.igd.semoa.hsmagent.actions.PrepareMigrationAction;
import de.fhg.igd.semoa.hsmagent.conditions.MoreDestinationsCondition;
import de.fhg.igd.semoa.server.Environment;
import de.fhg.igd.semoa.server.FieldType;
import de.fhg.igd.semoa.server.Mobility;
import de.fhg.igd.util.Resource;
import de.fhg.igd.util.Variables;
import de.fhg.igd.util.VariablesContext;
import de.fhg.igd.util.WhatIs;

/**
 * An HSM Agent. Looks for the list of other servers and jumps to the first one
 * on the list. There it calls the IhkWebservice. Then it returns home.
 * 
 * <pre>
 *  HSM:
 *  ----
 *  initial --> homeaddr --> destaddr --> nextaddr ....> webservice -+
 *                               |                                   |
 *                               |                                   |
 *  finish <.....gohome <--------+-----------------------------------+
 * </pre>
 * 
 * This agent must be granted the following rights:
 * <ul>
 * <li>java.util.PropertyPermission "axis.attachments.implementation" "read"
 * <li>java.util.PropertyPermission "axis.doAutoTypes" "read"
 * <li>java.util.PropertyPermission "enableNamespacePrefixOptimization" "read"
 * </ul>
 * 
 * @author C. Nickel
 * @version $Id: AgentBehaviour.java 1913 2007-08-08 02:41:53Z jpeters $
 */
public class AgentBehaviour extends HSM {

    /**
     * Keys for the context of the state machine. This context is a global data
     * repository for information sharing between states.
     */
    private static final String CTX_HOME = "url.home";

    private static final String CTX_DESTINATIONS = "map.destinations";

    private static final String CTX_ANSW = "String.answ";

    /**
     * Keys for handle the roundtrip webservice action.
     */
    protected static final String CTX_XML = "XMLDocument";

    protected static final String CTX_CONF = "XMLControlFile";

    protected static final String CTX_DOCID = "long.docid";

    private static final String CTX_RESULT = "String.result";

    /**
     * Tags for XML-Document-Config parsing.
     */
    public static final String XML_DELEGATE = "/conf/ws-delegate";

    public static final String XML_HOST = "ws-host";

    public static final String XML_URL = "ws-url";

    public static final String XML_METHOD = "ws-method";

    public static final String XML_PARAM = "ws-param";

    /**
     * XML path to flow entry.
     */
    public static final String XML_FLOW = "/conf/ws-flow";

    /**
     * Keyword for callback function.
     */
    public static final String XML_CALLBACK = "/conf/ws-callback";

    /**
     * Keyword for serial ws-delegation. 1 Agent makes a roundtrip.
     */
    public static String WS_PERFORM_ROUNDTRIP = "roundtrip";

    /**
     * Keyword for parallel ws-delegation. n Agents for n targets.
     */
    public static String WS_PERFORM_CONCURRENT = "concurrent";

    public static final String CTX_ACTUAL = "int.actual";

    /**
     * Prefered migration protocol.
     */
    private static final String PROTOCOL = "raw";

    /**
     * Constructs the state machine for this agent behaviour.
     */
    public AgentBehaviour() {
        setName("WsAgent5");
        State initial = new InitialState("STATE INITIAL");
        State homeaddr = new HSMState("STATE DETERMINE HOME ADDRESS");
        State nextaction = new HSMState("STATE DETERMINE NEXT ACTION");
        State nextaddr = new HSMState("STATE CHOOSE DESTINATION");
        State webservice = new HSMState("STATE CALL WEBSERVICE");
        State gohome = new HSMState("STATE PREPARE HOME MIGRATION");
        State finish = new FinalState("STATE FINISHED");
        homeaddr.setAction(new LocalAddressAction(CTX_HOME));
        nextaction.setAction(new PrepareNextAction());
        nextaddr.setAction(new PrepareMigrationAction(CTX_HOME, CTX_DESTINATIONS, PROTOCOL));
        webservice.setAction(new CallWebserviceAction());
        gohome.setAction(new PrepareHomeMigrationAction(CTX_HOME, PROTOCOL));
        finish.setAction(new ResponseAction());
        new HSMTransition(initial, homeaddr);
        new HSMTransition(homeaddr, nextaction);
        new HSMTransition(nextaction, nextaddr, new MoreDestinationsCondition(CTX_DESTINATIONS, true));
        new MigrationTransition(nextaddr, webservice);
        new HSMTransition(webservice, nextaction);
        new HSMTransition(nextaction, gohome, new MoreDestinationsCondition(CTX_DESTINATIONS, false));
        new MigrationTransition(gohome, finish);
        addState(initial);
        addState(homeaddr);
        addState(nextaction);
        addState(nextaddr);
        addState(webservice);
        addState(gohome);
        addState(finish);
    }

    /**
     * 
     * @author Martin
     *
     */
    private class PrepareNextAction extends AbstractAction {

        public void perform(Context context) {
            VariablesContext var;
            int next;
            Map ctxdest;
            String ws_host, s_next;
            var = Variables.getContext();
            s_next = (String) context.get(CTX_ACTUAL);
            if (s_next == null) {
                next = 0;
            } else {
                next = Integer.parseInt(s_next) + 1;
            }
            context.set(CTX_ACTUAL, String.valueOf(next));
            ws_host = var.get(next + "." + XML_HOST);
            if (ws_host == null || ws_host.length() == 0) {
                context.set(CTX_DESTINATIONS, null);
            } else {
                try {
                    ctxdest = new Hashtable();
                    ctxdest.put("next", new de.fhg.igd.util.URL(ws_host));
                    context.set(CTX_DESTINATIONS, ctxdest);
                } catch (MalformedURLException e1) {
                    System.out.println("AgentBehaviour4.PrepareNextAgent: " + e1);
                }
            }
        }
    }

    /**
     * This action calls the Webservice
     */
    private class CallWebserviceAction extends AbstractAction {

        public void perform(Context context) {
            SimpleProvider config;
            VariablesContext var;
            Service service;
            Object ret;
            Call call;
            Object[] parameter;
            String method;
            String wsurl;
            String xmlDoc;
            int actual;
            URL url;
            try {
                var = Variables.getContext();
                actual = Integer.parseInt((String) context.get(CTX_ACTUAL));
                wsurl = var.get(actual + "." + XML_URL);
                method = var.get(actual + "." + XML_METHOD);
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
                xmlDoc = (String) context.get(CTX_XML);
                if (xmlDoc == null) xmlDoc = var.get(CTX_XML);
                Vector v_param = new Vector();
                v_param.add(xmlDoc);
                v_param.add(new Long(var.get(CTX_DOCID)));
                parameter = v_param.toArray();
                config = new SimpleProvider();
                config.deployTransport("http", new HTTPSender());
                service = new Service(config);
                call = (Call) service.createCall();
                call.setTargetEndpointAddress(url);
                call.setOperationName(new QName("http://schemas.xmlsoap.org/soap/encoding/", method));
                try {
                    ret = call.invoke(parameter);
                    System.out.println("Returned: " + ret);
                    context.set(CTX_XML, ret);
                } catch (RemoteException ex) {
                    System.out.println("----- Could not invoke the method! -----");
                }
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        }
    }

    /**
     * This action invokes the method 'response' of the WebserviceLauncher.
     */
    private class ResponseAction extends AbstractAction {

        /**
         * This method retreives the token from the properties and the answer
         * from the context. Then it invokes the method response from the
         * WebserviceLauncher.
         * 
         * @see examples.wslauncher2.WebserviceLauncher#response(String, String)
         */
        public void perform(Context context) {
            WebserviceLauncher ws;
            VariablesContext var;
            Environment env;
            String token;
            String answ;
            String key;
            var = Variables.getContext();
            answ = (String) context.get(CTX_XML);
            if (answ == null) {
                answ = "Did not invoke the webservice.";
            }
            token = var.get("token");
            if (token == null) {
                System.out.println("Can not find the token.");
                return;
            }
            env = Environment.getEnvironment();
            key = WhatIs.stringValue(WebserviceLauncher.WHATIS);
            ws = (WebserviceLauncher) env.lookup(key);
            if (ws == null) {
                System.out.println("Can not find the webservice launcher.");
                return;
            }
            ws.response(answ, token);
        }
    }
}
