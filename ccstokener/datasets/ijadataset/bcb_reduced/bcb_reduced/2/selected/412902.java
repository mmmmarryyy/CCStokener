package hu.sztaki.lpds.pgportal.portlets.admin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import javax.portlet.GenericPortlet;
import javax.portlet.ActionRequest;
import javax.portlet.RenderRequest;
import javax.portlet.ActionResponse;
import javax.portlet.RenderResponse;
import javax.portlet.PortletException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import javax.portlet.PortletSession;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

/**
 * spequlosPortlet Portlet Class
 * @author krisztian karoczkai
 */
public class SpequlosPortlet extends GenericPortlet {

    public static final String SPEQULOS_SESSION_KEY = "spequlos_url";

    public static final String SPEQULOS_MODULE_CREDIT = "spequlos_credit";

    public static final String SPEQULOS_MODULE_ORACLE = "spequlos_oracle";

    public static final String SPEQULOS_CONFIG_FILE_PATH = "/WEB-INF/spequlos.url";

    private static List<SpequlosData> spequlosCommands = new ArrayList<SpequlosData>();

    @Override
    public void init() throws PortletException {
        System.out.println("INIT");
        super.init();
        spequlosCommands.add(new SpequlosData("credit", "get_credits.py", new String[] { "batch_id", "dg_id", "institution_id", "qosuser_id" }, new String[] { "credit_number" }));
        spequlosCommands.add(new SpequlosData("credit", "order.py", new String[] { "batch_id", "dg_id", "qosuser_id", "credit" }, new String[] { "order_true", "order_false" }));
        spequlosCommands.add(new SpequlosData("oracle", "get_completion_info.py", new String[] { "batch_id", "dg_id" }, new String[] { "completion_elapsed_time", "completion_rate" }));
        spequlosCommands.add(new SpequlosData("oracle", "predict.py", new String[] { "batch_id", "dg_id" }, new String[] { "predict_credit_to_spend", "predict_batch_completion", "predict_confidence" }));
        spequlosCommands.add(new SpequlosData("credit", "get_qosorders.py", new String[] { "qosuser_id" }, new String[] { "qos_batch_id", "qos_dg_id", "qos_user_id", "qos_number" }));
    }

    @Override
    public void processAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        String paramURL = request.getParameter("purl");
        String creditmodule = request.getParameter("credit");
        String oraclemodule = request.getParameter("oracle");
        System.out.println("process action:" + paramURL);
        if (paramURL != null) {
            String configFilePath = getPortletContext().getRealPath(SPEQULOS_CONFIG_FILE_PATH);
            try {
                File f = new File(configFilePath);
                if (!f.exists()) f.createNewFile();
                FileWriter fw = new FileWriter(f);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(paramURL);
                bw.newLine();
                if (creditmodule != null) {
                    bw.write(creditmodule);
                }
                bw.newLine();
                if (oraclemodule != null) {
                    bw.write(oraclemodule);
                }
                bw.newLine();
                bw.flush();
                bw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            PortletSession ps = request.getPortletSession();
            ps.setAttribute(SPEQULOS_SESSION_KEY, paramURL, ps.APPLICATION_SCOPE);
            ps.setAttribute(SPEQULOS_MODULE_CREDIT, creditmodule, ps.APPLICATION_SCOPE);
            ps.setAttribute(SPEQULOS_MODULE_ORACLE, oraclemodule, ps.APPLICATION_SCOPE);
        }
    }

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        System.out.print("doView");
        try {
            initSession(request.getPortletSession());
            getPortletContext().getRequestDispatcher("/WEB-INF/jsp/spequlos/view.jsp").include(request, response);
        } catch (Exception e) {
            getPortletContext().getRequestDispatcher("/WEB-INF/jsp/spequlos/error.jsp").include(request, response);
        }
    }

    @Override
    public void doEdit(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        getPortletContext().getRequestDispatcher("/WEB-INF/jsp/spequlos/edit.jsp").include(request, response);
    }

    @Override
    public void doHelp(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        getPortletContext().getRequestDispatcher("/WEB-INF/jsp/spequlos/help.jsp").include(request, response);
    }

    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {
        String resourceID = request.getResourceID();
        if (resourceID.startsWith("spequlos_form")) {
            int formID = renderConfigForms(resourceID);
            PortletSession ps = request.getPortletSession();
            ps.setAttribute("formid", formID, ps.APPLICATION_SCOPE);
            request.setAttribute("formparameters", spequlosCommands.get(formID).getParameters());
            getPortletContext().getRequestDispatcher("/WEB-INF/jsp/spequlos/form.jsp").include(request, response);
        } else if (resourceID.startsWith("spequlos_data")) try {
            int formID = renderDataForms(resourceID);
            SpequlosResponse spequlosResponse = callspequlosForm(request);
            System.out.println("SPEQULOS RESPONSE: " + spequlosResponse);
            request.setAttribute("spq_log", spequlosResponse.log);
            ArrayList<String> res = spequlosResponse.response;
            request.setAttribute("spq_resp", res);
            request.setAttribute("response_texts", spequlosCommands.get(formID).getResponse());
            request.setAttribute("spq_command_succ", spequlosResponse.succ);
            getPortletContext().getRequestDispatcher("/WEB-INF/jsp/spequlos/response.jsp").include(request, response);
        } catch (Exception e) {
            System.out.println("Exception");
            e.printStackTrace();
            response.getWriter().println(e.getLocalizedMessage());
        }
    }

    /**
 * Determination spequlos config form
 * @param resourceID ResourceID
 * @return number of form
 */
    private int renderConfigForms(String resourceID) {
        return Integer.parseInt(resourceID.substring("spequlos_form".length()));
    }

    /**
 * Determination spequlos data form
 * @param resourceID ResourceID
 * @return number of form
 */
    private int renderDataForms(String resourceID) {
        String[] splittedResourceID = resourceID.split("_");
        return Integer.parseInt(splittedResourceID[splittedResourceID.length - 1]);
    }

    /**
 * Read config data and store in session
 * @param ps User Session
 * @throws java.lang.Exception problem of the file handing
 */
    private void initSession(PortletSession ps) throws Exception {
        if (ps.getAttribute(SPEQULOS_SESSION_KEY, ps.APPLICATION_SCOPE) == null) {
            String configFilePath = getPortletContext().getRealPath(SPEQULOS_CONFIG_FILE_PATH);
            BufferedReader fr = new BufferedReader(new FileReader(configFilePath));
            ps.setAttribute(SPEQULOS_SESSION_KEY, fr.readLine(), ps.APPLICATION_SCOPE);
            ps.setAttribute(SPEQULOS_MODULE_CREDIT, fr.readLine(), ps.APPLICATION_SCOPE);
            ps.setAttribute(SPEQULOS_MODULE_ORACLE, fr.readLine(), ps.APPLICATION_SCOPE);
            fr.close();
        }
    }

    /**
 * spequlos adatform feldolgozas
 * @param resourceID form azonosito
 * @param request keres leiro
 */
    private SpequlosResponse callspequlosForm(ResourceRequest request) throws Exception {
        PortletSession ps = request.getPortletSession();
        int formid = ((Integer) ps.getAttribute("formid", ps.APPLICATION_SCOPE)).intValue();
        String targetURL = (String) request.getPortletSession().getAttribute(SPEQULOS_SESSION_KEY, ps.APPLICATION_SCOPE);
        String targetModuleName = spequlosCommands.get(formid).getModule();
        String targetModule = "";
        if (targetModuleName.equals("credit")) {
            targetModule = (String) ps.getAttribute(SPEQULOS_MODULE_CREDIT, ps.APPLICATION_SCOPE);
        }
        if (targetModuleName.equals("oracle")) {
            targetModule = (String) ps.getAttribute(SPEQULOS_MODULE_ORACLE, ps.APPLICATION_SCOPE);
        }
        String parameters = "";
        for (String t : spequlosCommands.get(formid).getParameters()) {
            parameters = parameters + "&" + t + "=" + URLEncoder.encode(request.getParameter(t), "UTF-8");
        }
        return executeGet(targetURL + targetModule + spequlosCommands.get(formid).getCommand(), parameters);
    }

    /**
 * Form post
 * @param targetURL url
 * @param urlParameters parameters
 * @return response string
 */
    private SpequlosResponse executeGet(String targetURL, String urlParameters) {
        URL url;
        HttpURLConnection connection = null;
        boolean succ = false;
        try {
            url = new URL(targetURL + "?" + urlParameters);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer log = new StringBuffer();
            ArrayList<String> response = new ArrayList<String>();
            while ((line = rd.readLine()) != null) {
                if (line.startsWith("<div class=\"qos\">")) {
                    System.out.println("here is the line : " + line);
                    String resp = line.split(">")[1].split("<")[0];
                    System.out.println("here is the splitted line : " + resp);
                    if (!resp.startsWith("None")) {
                        succ = true;
                        String[] values = resp.split(" ");
                        ArrayList<String> realvalues = new ArrayList<String>();
                        for (String s : values) {
                            realvalues.add(s);
                        }
                        if (realvalues.size() == 5) {
                            realvalues.add(2, realvalues.get(2) + " " + realvalues.get(3));
                            realvalues.remove(3);
                            realvalues.remove(3);
                        }
                        for (String n : realvalues) {
                            response.add(n);
                        }
                    }
                } else {
                    log.append(line);
                    log.append('\r');
                }
            }
            rd.close();
            SpequlosResponse speqresp = new SpequlosResponse(response, log.toString(), succ);
            return speqresp;
        } catch (Exception e) {
            e.printStackTrace();
            String log = "Please check the availability of Spequlos server!<br />" + "URL:" + targetURL + "<br />" + "PARAMETERS:" + urlParameters + "<br />";
            return new SpequlosResponse(null, log, succ);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    /**
 * Class to store spequlos response and log separately
 *
 */
    class SpequlosResponse {

        private String log;

        private ArrayList<String> response = new ArrayList<String>();

        private boolean succ;

        public SpequlosResponse(ArrayList<String> response, String log, boolean succ) {
            this.log = log;
            this.response = response;
            this.succ = succ;
        }

        public ArrayList<String> getResponse() {
            return response;
        }

        public void setResponse(ArrayList<String> response) {
            this.response = response;
        }

        public String getLog() {
            return log;
        }

        public void setLog(String log) {
            this.log = log;
        }

        public boolean isSucc() {
            return succ;
        }

        public void setSucc(boolean succ) {
            this.succ = succ;
        }
    }

    /**
 * spequlos command description
 */
    class SpequlosData {

        private String command;

        private String[] parameters;

        private String module;

        private String[] response;

        public SpequlosData() {
        }

        public SpequlosData(String module, String command, String[] parameters, String[] response) {
            this.module = module;
            this.command = command;
            this.parameters = parameters;
            this.response = response;
        }

        public String[] getResponse() {
            return response;
        }

        public void setResponse(String[] response) {
            this.response = response;
        }

        public String getModule() {
            return module;
        }

        public void setModule(String module) {
            this.module = module;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public String[] getParameters() {
            return parameters;
        }

        public void setParameters(String[] parameters) {
            this.parameters = parameters;
        }
    }
}
