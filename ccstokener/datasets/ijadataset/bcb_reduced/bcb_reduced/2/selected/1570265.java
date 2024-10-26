package examples.wslauncher2;

import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import javax.xml.namespace.QName;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.transport.http.HTTPSender;
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
import de.fhg.igd.semoa.hsmagent.actions.VicinityAddressesAction;
import de.fhg.igd.semoa.hsmagent.conditions.MoreDestinationsCondition;
import de.fhg.igd.semoa.server.Environment;
import de.fhg.igd.util.Variables;
import de.fhg.igd.util.VariablesContext;
import de.fhg.igd.util.WhatIs;

/**
 * An HSM Agent. Looks for the list of other servers and jumps to 
 * the first one on the list. There it calls the IhkWebservice.
 * Then it returns home.
 * <pre>
 * HSM:
 * ----
 * initial --> --> homeaddr --> destaddr --> nextaddr ....> webservice -+
 *                                  |                                   |
 *                                 	|                                   |
 * finish <.........gohome <--------+-----------------------------------+
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
	 * Prefered migration protocol.
	 */
    private static final String PROTOCOL = "raw";

    /**
     * Constructs the state machine for this agent behaviour. 
     */
    public AgentBehaviour() {
        setName("WsAgent2");
        State initial = new InitialState("STATE INITIAL");
        State homeaddr = new HSMState("STATE DETERMINE HOME ADDRESS");
        State destaddr = new HSMState("STATE DETERMINE DESTINATIONS");
        State nextaddr = new HSMState("STATE CHOOSE DESTINATION");
        State webservice = new HSMState("STATE CALL IHK WEBSERVICE");
        State gohome = new HSMState("STATE PREPARE HOME MIGRATION");
        State finish = new FinalState("STATE FINISHED");
        homeaddr.setAction(new LocalAddressAction(CTX_HOME));
        destaddr.setAction(new VicinityAddressesAction(CTX_DESTINATIONS));
        nextaddr.setAction(new PrepareMigrationAction(CTX_HOME, CTX_DESTINATIONS, PROTOCOL));
        webservice.setAction(new CallWebserviceAction());
        gohome.setAction(new PrepareHomeMigrationAction(CTX_HOME, PROTOCOL));
        finish.setAction(new ResponseAction());
        new HSMTransition(initial, homeaddr);
        new HSMTransition(homeaddr, destaddr);
        new HSMTransition(destaddr, nextaddr, new MoreDestinationsCondition(CTX_DESTINATIONS, true));
        new MigrationTransition(nextaddr, webservice);
        new HSMTransition(webservice, gohome);
        new HSMTransition(destaddr, gohome, new MoreDestinationsCondition(CTX_DESTINATIONS, false));
        new MigrationTransition(gohome, finish);
        addState(initial);
        addState(homeaddr);
        addState(destaddr);
        addState(nextaddr);
        addState(webservice);
        addState(gohome);
        addState(finish);
    }

    /**
	 * This action calls the IhkWebservice
	 */
    private class CallWebserviceAction extends AbstractAction {

        public void perform(Context context) {
            Long workflowIdParentProcess;
            SimpleProvider config;
            VariablesContext var;
            String xmldocument;
            Service service;
            Object[] param;
            String method;
            String wsurl;
            Object ret;
            Call call;
            URL url;
            try {
                var = Variables.getContext();
                if (var.get("wsurl") == null || var.get("method") == null) {
                    System.out.println("----- Did not get method or wsurl from the properties! -----");
                    return;
                }
                method = var.get("method");
                wsurl = var.get("wsurl");
                url = new java.net.URL(wsurl);
                try {
                    url.openConnection().connect();
                } catch (IOException ex) {
                    System.out.println("----- Could not connect to the webservice! -----");
                }
                if (var.get("param0") == null || var.get("param1") == null) {
                    System.out.println("----- Need parameters! -----");
                    return;
                }
                xmldocument = var.get("param0");
                workflowIdParentProcess = new Long(var.get("param1"));
                param = new Object[] { xmldocument, workflowIdParentProcess };
                config = new SimpleProvider();
                config.deployTransport("http", new HTTPSender());
                service = new Service(config);
                call = (Call) service.createCall();
                call.setTargetEndpointAddress(url);
                call.setOperationName(new QName("http://schemas.xmlsoap.org/soap/encoding/", method));
                try {
                    ret = call.invoke(param);
                    context.set(CTX_ANSW, "=> notifyIhk invoked - Result: " + ret);
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
		 * This method retreives the token from the properties and the 
		 * answer from the context. Then it invokes the method response 
		 * from the WebserviceLauncher.
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
            if (context.get(CTX_ANSW) == null) {
                answ = "Did not invoke the webservice.";
            } else {
                answ = context.get(CTX_ANSW).toString();
            }
            var = Variables.getContext();
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
