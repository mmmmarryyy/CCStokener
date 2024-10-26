package iwork.icrafter.uigen.html;

import java.util.*;
import iwork.icrafter.util.*;
import iwork.icrafter.im.UserSpec;
import iwork.icrafter.im.UISpec;
import iwork.icrafter.remote.*;
import java.io.*;
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;
import iwork.eheap2.*;
import iwork.state.*;
import org.python.util.PythonInterpreter;

/**
 * This class should typically only be needed only for reference. This
 * class is a HTTP front end for the Interface Manager. (That is, users
 * can go to this servlet and find a list of all services running in
 * the local environment. They can also request user interfaces for
 * these services.) This servlet also allows making calls to services
 * from web links.
 **/
public class HTTPFrontEnd extends HttpServlet {

    String servletURL;

    String heapMachine;

    int opn_expire;

    EventHeap eh;

    EHCallObject ehc;

    String libFuncFile;

    final String UISPEC = "uispec";

    final String URL = "url";

    final String REQTYPE = "requestType";

    final String CALL = "call";

    final String MULTIPLESERVICES = "MultipleServices";

    final String SERVICESCOMBO = "ServicesCombo";

    final String REQUESTINTERFACE = "RequestInterface";

    final String SAVEFAVORITE = "SaveFavorite";

    final String SCRIPT = "script";

    public static String appSpecStr = "<applianceSpec><language name='html'>" + "</language></applianceSpec>";

    public static String userSpecStr = "<userSpec><favorites></favorites><property " + "name='checkFavorites' value='false'></property></userSpec>";

    public void init() throws ServletException {
        try {
            Utils.debug("HTTPFrontEnd", "init  called!");
            servletURL = getInitParameter("servletURL");
            Utils.debug("HTTPFrontEnd", "servletURL : " + servletURL);
            libFuncFile = getInitParameter("libFuncFile");
            heapMachine = getInitParameter("heapMachine");
            opn_expire = (new Integer(getInitParameter("expiry"))).intValue();
            EventHeap eh = new EventHeap(heapMachine);
            EHCall.init(eh);
            ehc = new EHCallObject(eh, opn_expire);
        } catch (Exception e) {
            Utils.debug("HTTPFrontEnd", "init received an exception:");
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        doPost(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            long before = System.currentTimeMillis();
            Utils.debug("HTTPFrontEnd", "doPost called");
            Utils.debug("HTTPFrontEnd", "Request URI: " + request.getRequestURI());
            String queryString = request.getQueryString();
            String reqType = request.getParameter(REQTYPE);
            if (reqType == null) reqType = CALL;
            Utils.debug("HTTPFrontEnd", "reqType: " + reqType);
            if (queryString == null || queryString.equals("")) {
                genIMInterface(response, ehc);
            } else if (reqType.equals(MULTIPLESERVICES)) {
                genMultipleServicesInterface(response, ehc);
            } else if (reqType.equals(SERVICESCOMBO)) {
                genServicesComboInterface(request, response, ehc);
            } else if (reqType.equals(REQUESTINTERFACE)) {
                requestInterface(request, response, ehc);
            } else if (reqType.equals(SAVEFAVORITE)) {
                saveFavorite(request, response, ehc);
            } else if (reqType.equals(SCRIPT)) {
                executeScript(request, response, ehc);
            } else {
                httpCall(request, response, ehc);
            }
            long after = System.currentTimeMillis();
            long etime = after - before;
            Utils.debug("HTTPFrontEnd", "Before: " + before + " After: " + after + " Elapsed time: " + etime);
        } catch (Exception e) {
            Utils.warning("HTTPFrontEnd", "Exception thrown while processing request");
            e.printStackTrace();
            errorReturn(response);
        }
    }

    protected void httpCall(HttpServletRequest request, HttpServletResponse response, EHCallObject ehc) {
        try {
            Utils.debug("HTTPFrontEnd", "httpCall called");
            String referer = request.getHeader("Referer");
            String destURL = request.getParameter("destURL");
            if (destURL == null) destURL = request.getParameter("dest");
            if (destURL == null && referer != null) destURL = referer;
            int num = 0;
            while (true) {
                String svcName = null;
                String ifName = null;
                String opName = null;
                if (num == 0) {
                    svcName = request.getParameter(ICrafterConstants.SERVICE_NAME);
                    ifName = request.getParameter(ICrafterConstants.INTERFACE_NAME);
                    opName = request.getParameter(ICrafterConstants.OPERATION_NAME);
                } else {
                    svcName = request.getParameter(ICrafterConstants.SERVICE_NAME + num);
                    ifName = request.getParameter(ICrafterConstants.INTERFACE_NAME + num);
                    opName = request.getParameter(ICrafterConstants.OPERATION_NAME + num);
                    if ((svcName == null && ifName == null) || opName == null) break;
                }
                Utils.debug("HTTPFrontEnd", "opName = " + opName);
                if (opName == null) {
                    Utils.warning("HTTPFrontEnd", "No operation name in method call");
                    num++;
                    continue;
                }
                int i = 0;
                String param = null;
                if (num == 0) {
                    param = request.getParameter(ICrafterConstants.PARAM + i);
                } else {
                    param = request.getParameter(ICrafterConstants.PARAM + num + "," + i);
                    if (param == null) param = request.getParameter(ICrafterConstants.PARAM + num + "." + i);
                }
                while (param != null && !param.trim().equals("")) {
                    Utils.debug("HTTPFrontEnd", "CALLEVENT: Parameter name " + new String(ICrafterConstants.PARAM + i) + " val: " + param);
                    i++;
                    if (num == 0) {
                        param = request.getParameter(ICrafterConstants.PARAM + i);
                    } else {
                        param = request.getParameter(ICrafterConstants.PARAM + num + "," + i);
                        if (param == null) param = request.getParameter(ICrafterConstants.PARAM + num + "." + i);
                    }
                }
                int numParams = i;
                Utils.debug("HTTPFrontEnd", "numParams: " + numParams);
                Serializable[] params = null;
                if (numParams > 0) {
                    params = new Serializable[numParams];
                    for (i = 0; i < numParams; i++) {
                        if (num == 0) {
                            param = request.getParameter(ICrafterConstants.PARAM + i);
                        } else {
                            param = request.getParameter(ICrafterConstants.PARAM + num + "," + i);
                            if (param == null) param = request.getParameter(ICrafterConstants.PARAM + num + "." + i);
                        }
                        String paramType = null;
                        if (num == 0) {
                            paramType = request.getParameter(ICrafterConstants.PARAM_TYPE + i);
                        } else {
                            paramType = request.getParameter(ICrafterConstants.PARAM_TYPE + num + "," + i);
                            if (paramType == null) paramType = request.getParameter(ICrafterConstants.PARAM_TYPE + num + "." + i);
                        }
                        if (paramType.equals(StateConstants.STRING)) {
                            Utils.debug("HTTPFrontEnd", "string parameter " + i + " Type: " + paramType + " Value: " + param);
                            params[i] = param;
                        } else if (paramType.equals(StateConstants.INT)) {
                            Utils.debug("HTTPFrontEnd", "integer parameter " + i + " Type: " + paramType + " Value: " + param);
                            params[i] = new Integer(param.trim());
                        } else if (paramType.equals(StateConstants.FLOAT)) {
                            Utils.debug("HTTPFrontEnd", "float parameter " + i + " Type: " + paramType + " Value: " + param);
                            params[i] = new Float(param.trim());
                        } else if (paramType.equals(StateConstants.STRINGARRAY)) {
                            Utils.debug("HTTPFrontEnd", "array parameter " + i + " Type: " + paramType + " Value: " + param);
                            String[] arr;
                            if (num == 0) {
                                arr = request.getParameterValues(ICrafterConstants.PARAM + i);
                            } else {
                                arr = request.getParameterValues(ICrafterConstants.PARAM + num + "," + i);
                                if (arr == null) arr = request.getParameterValues(ICrafterConstants.PARAM + num + "." + i);
                            }
                            params[i] = arr;
                        } else {
                            Utils.warning("HTTPFrontEnd", "Unsupported " + "parameter type: " + paramType);
                            errorReturn(response);
                        }
                    }
                } else {
                    params = null;
                }
                if (ifName != null) {
                    Vector mVars = new Vector();
                    String mv = null;
                    i = 0;
                    if (num == 0) {
                        mv = request.getParameter(ICrafterConstants.MATCHING_VARIABLE + i);
                    } else {
                        mv = request.getParameter(ICrafterConstants.MATCHING_VARIABLE + num + "," + i);
                        if (mv == null) mv = request.getParameter(ICrafterConstants.MATCHING_VARIABLE + num + "." + i);
                    }
                    while (mv != null && !mv.trim().equals("")) {
                        Utils.debug("HTTPFrontEnd", "MatchVar " + num + "," + i + ":" + mv);
                        mVars.add(new MatchingVariable(mv));
                        i++;
                        if (num == 0) {
                            mv = request.getParameter(ICrafterConstants.MATCHING_VARIABLE + i);
                        } else {
                            mv = request.getParameter(ICrafterConstants.MATCHING_VARIABLE + num + "," + i);
                            if (mv == null) mv = request.getParameter(ICrafterConstants.MATCHING_VARIABLE + num + "." + i);
                        }
                    }
                    MatchingVariable[] mVarArray = (MatchingVariable[]) mVars.toArray(new MatchingVariable[0]);
                    Utils.debug("HTTPFrontEnd", "Making call to: " + ifName + "matching vars: " + mVars);
                    ehc.ehIFCall(ifName, opName, params, mVarArray);
                    num++;
                    continue;
                }
                String retType = null;
                if (num == 0) retType = request.getParameter(ICrafterConstants.RETURN_TYPE); else retType = request.getParameter(ICrafterConstants.RETURN_TYPE + num);
                Utils.debug("HTTPFrontEnd", "Return Type " + retType);
                if (retType == null || retType.trim().equals("")) {
                    Utils.debug("HTTPFrontEnd", "No return specified for this call");
                    Utils.debug("HTTPFrontEnd", "Making call to: " + svcName);
                    ehc.ehCall(svcName, opName, params);
                    num++;
                    continue;
                }
                if (retType.equals(UISPEC)) {
                    String uiSpecStr = (String) ehc.ehCall(svcName, opName, true, params);
                    UISpec uiSpec = new UISpec(uiSpecStr);
                    response.setContentType("text/html");
                    OutputStream out = response.getOutputStream();
                    out.write(uiSpec.getUICode());
                    Utils.debug("HTTPFrontEnd", "Returned received interface " + "to the HTTP client");
                    return;
                } else if (retType.equals(StateConstants.INT)) {
                    Integer intRet = (Integer) ehc.ehCall(svcName, opName, true, params);
                    resultReturn(intRet.toString(), response, destURL);
                    return;
                } else if (retType.equals(StateConstants.FLOAT)) {
                    Float floatRet = (Float) ehc.ehCall(svcName, opName, true, params);
                    resultReturn(floatRet.toString(), response, destURL);
                    return;
                } else if (retType.equals(URL)) {
                    String urlRet = (String) ehc.ehCall(svcName, opName, true, params);
                    redirect(response, urlRet);
                    return;
                } else {
                    String strRet = (String) ehc.ehCall(svcName, opName, true, params);
                    resultReturn(strRet, response, destURL);
                    return;
                }
            }
            if (destURL != null) {
                redirect(response, destURL);
            } else {
                genIMInterface(response, ehc);
            }
        } catch (Exception e) {
            Utils.warning("HTTPFrontEnd", "Exception thrown in httpCall");
            e.printStackTrace();
            return;
        }
    }

    /**
     * executes the given script and returns the result if any 
     */
    protected void executeScript(HttpServletRequest request, HttpServletResponse response, EHCallObject ehc) {
        try {
            String referer = request.getHeader("Referer");
            String destURL = request.getParameter("destURL");
            if (destURL == null) destURL = request.getParameter("dest");
            if (destURL == null && referer != null) destURL = referer;
            String script = request.getParameter(SCRIPT);
            Utils.debug("HTTPFrontEnd", "python script: " + script);
            int prevIndex = 0;
            int index = 0;
            while ((index = script.indexOf("$", prevIndex)) != -1) {
                prevIndex = index + 1;
                index = script.indexOf("$", prevIndex);
                String name = script.substring(prevIndex, index);
                String val = request.getParameter(name).trim();
                script = script.substring(0, prevIndex - 1) + "'" + val + "'" + script.substring(index + 1);
                prevIndex = index + 1;
            }
            Utils.debug("HTTPFrontEnd", "python script after processing:\n" + script);
            PythonInterpreter interp = new PythonInterpreter();
            interp.execfile(libFuncFile);
            interp.exec(script);
            String retType = request.getParameter(ICrafterConstants.RETURN_TYPE);
            Utils.debug("HTTPFrontEnd", "Script return type " + retType);
            if (retType == null || retType.trim().equals("")) {
                return;
            }
            Object pyret = interp.get("result");
            Object ret = PyUtils.toJava(pyret);
            if (retType.equals(UISPEC)) {
                String uiSpecStr = (String) ret;
                UISpec uiSpec = new UISpec(uiSpecStr);
                response.setContentType("text/html");
                OutputStream out = response.getOutputStream();
                out.write(uiSpec.getUICode());
                Utils.debug("HTTPFrontEnd", "Returned received interface " + "to the HTTP client");
                return;
            } else if (retType.equals(StateConstants.INT)) {
                Integer intRet = (Integer) ret;
                resultReturn(intRet.toString(), response, destURL);
                return;
            } else if (retType.equals(StateConstants.FLOAT)) {
                Float floatRet = (Float) ret;
                resultReturn(floatRet.toString(), response, destURL);
                return;
            } else if (retType.equals(URL)) {
                String urlRet = (String) ret;
                redirect(response, urlRet);
                return;
            } else {
                String strRet = (String) ret;
                resultReturn(strRet, response, destURL);
                return;
            }
        } catch (Exception e) {
            Utils.warning("HTTPFrontEnd", "Exception thrown in requestInterface");
            e.printStackTrace();
            return;
        }
    }

    /**
     * gets the interface of the Interface Manager and presents it to the user 
     */
    protected void genIMInterface(HttpServletResponse response, EHCallObject ehc) {
        try {
            Serializable params[] = new Serializable[3];
            params[0] = "<serviceSpec><instance name='InterfaceManager'>" + "</instance></serviceSpec>";
            params[1] = appSpecStr;
            params[2] = userSpecStr;
            String uiSpecStr = (String) ehc.ehCall("InterfaceManager", "getInterface", true, params);
            UISpec uiSpec = new UISpec(uiSpecStr);
            response.setContentType("text/html");
            OutputStream out = response.getOutputStream();
            out.write(uiSpec.getUICode());
        } catch (Exception e) {
            Utils.warning("HTTPFrontEnd", "Exception thrown while " + "trying to generate main services page!");
            e.printStackTrace();
        }
    }

    /**
     * gets the interface of the given service(s) and presents it to the user 
     */
    protected void requestInterface(HttpServletRequest request, HttpServletResponse response, EHCallObject ehc) {
        try {
            Serializable params[] = new Serializable[3];
            params[0] = request.getParameter("servicespec");
            params[1] = request.getParameter("appliancespec");
            params[2] = request.getParameter("userspec");
            UserSpec userSpec = new UserSpec((String) params[2]);
            String checkFav = request.getParameter("checkFavorites");
            Utils.debug("HTTPFrontEnd", "Check Fav value: " + checkFav);
            if (checkFav == null) checkFav = "off";
            checkFav = checkFav.trim();
            userSpec.setProperty("checkFavorites", checkFav);
            Utils.debug("HTTPFrontEnd", "Check Fav value: " + checkFav);
            if (checkFav.equals("on")) {
                Utils.debug("HTTPFrontEnd", "Check favorites turned on!");
                Cookie[] cookies = request.getCookies();
                for (int i = 0; i < cookies.length; i++) {
                    String name = cookies[i].getName();
                    Utils.debug("HTTPFrontEnd", "Cookie " + i + ": " + name);
                    if (name.equals("FavoriteGenerators")) {
                        String value = cookies[i].getValue();
                        Utils.debug("HTTPFrontEnd", "Adding favorites: " + value);
                        userSpec.setFavorites(value);
                    }
                }
            }
            params[2] = userSpec.toXML();
            String uiSpecStr = (String) ehc.ehCall("InterfaceManager", "getInterface", true, params);
            UISpec uiSpec = new UISpec(uiSpecStr);
            response.setContentType("text/html");
            OutputStream out = response.getOutputStream();
            out.write(uiSpec.getUICode());
        } catch (Exception e) {
            Utils.warning("HTTPFrontEnd", "Exception thrown while " + "trying to generate main services page!");
            e.printStackTrace();
        }
    }

    private void saveFavorite(HttpServletRequest request, HttpServletResponse response, EHCallObject ehc) {
        try {
            URL url = new URL(request.getParameter("url"));
            StringWriter sw = new StringWriter();
            InputStreamReader isr = new InputStreamReader(url.openStream());
            char[] c = new char[100];
            int len = 0;
            while ((len = isr.read(c, 0, 100)) != -1) sw.write(c, 0, len);
            String newFav = (String) (sw.toString());
            newFav = newFav.replace('\n', ' ');
            Utils.debug("HTTPFrontEnd", "Favorite TBA: " + newFav);
            Cookie[] cookies = request.getCookies();
            for (int i = 0; i < cookies.length; i++) {
                String name = cookies[i].getName();
                if (name.equals("FavoriteGenerators")) {
                    String favorites = cookies[i].getValue();
                    Utils.debug("HTTPFrontEnd", "Favorites cookie val: " + favorites);
                    int from = newFav.indexOf("<generator");
                    int at = favorites.indexOf("</generators>");
                    favorites = favorites.substring(0, at - 1) + newFav.substring(from) + favorites.substring(at);
                    Utils.debug("HTTPFrontEnd", "Updated favorites: " + favorites);
                    cookies[i].setValue(favorites);
                    response.addCookie(cookies[i]);
                    return;
                }
            }
            int from = newFav.indexOf("<generator");
            String initVal = "<?xml version=\"1.0\"?>" + "<generators>" + newFav.substring(from) + "</generators>";
            Utils.debug("HTTPFrontEnd", "Initial favorite cookie val: " + initVal);
            Cookie cookie = new Cookie("FavoriteGenerators", initVal);
            response.addCookie(cookie);
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.println("<HTML>");
            out.println("<HEAD><TITLE> Favorite added </TITLE></HEAD>");
            out.println("<BODY>");
            out.println("<p>Successfully added to favorites!</p>");
            out.println("</BODY></HTML>");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * gets the interface MultipleServices checkbox page
     * and presents it to the user 
     */
    protected void genMultipleServicesInterface(HttpServletResponse response, EHCallObject ehc) {
        try {
            Serializable params[] = new Serializable[3];
            params[0] = "<serviceSpec><instance name='MultipleServices'>" + "</instance></serviceSpec>";
            params[1] = appSpecStr;
            params[2] = userSpecStr;
            String uiSpecStr = (String) ehc.ehCall("InterfaceManager", "getInterface", true, params);
            UISpec uiSpec = new UISpec(uiSpecStr);
            response.setContentType("text/html");
            OutputStream out = response.getOutputStream();
            out.write(uiSpec.getUICode());
        } catch (Exception e) {
            Utils.warning("HTTPFrontEnd", "Exception thrown while " + "trying to generate main services page!");
            e.printStackTrace();
        }
    }

    /**
     * gets the interface for combination of Services and presents it
     * to the user 
     */
    protected void genServicesComboInterface(HttpServletRequest request, HttpServletResponse response, EHCallObject ehc) {
        try {
            String num = request.getParameter("numServices");
            int numServices = 0;
            if (num != null) {
                numServices = Integer.parseInt(num);
            }
            String svcString = "";
            for (int i = 0; i < numServices; i++) {
                String selected = request.getParameter("selected" + i);
                if (selected != null && !selected.equals("off")) {
                    svcString = svcString + "<instance name='" + selected + "'></instance>";
                }
            }
            Serializable params[] = new Serializable[3];
            params[0] = "<serviceSpec>" + svcString + "</serviceSpec>";
            params[1] = appSpecStr;
            params[2] = userSpecStr;
            String uiSpecStr = (String) ehc.ehCall("InterfaceManager", "getInterface", true, params);
            UISpec uiSpec = new UISpec(uiSpecStr);
            response.setContentType("text/html");
            OutputStream out = response.getOutputStream();
            out.write(uiSpec.getUICode());
        } catch (Exception e) {
            Utils.warning("HTTPFrontEnd", "Exception thrown while trying " + "to generate main services page!");
            e.printStackTrace();
        }
    }

    /**
     * returns an error
     */
    protected void errorReturn(HttpServletResponse response) {
        PrintWriter out;
        try {
            Utils.debug("HTTPFrontEnd", "Error routine called");
            response.setContentType("text/html");
            out = response.getWriter();
            out.println("<HTML>");
            out.println("<HEAD><TITLE> Error </TITLE></HEAD>");
            out.println("<BODY>");
            out.println("<H1>There was an error while processing " + "the request. </H1>");
            out.println("<H1>Click <A HREF=\"" + servletURL + "\">here</A> to " + "go back to the services page </H1>");
            out.println("</BODY></HTML>");
        } catch (Exception e) {
            Utils.warning("HTTPFrontEnd", "Exception thrown in errorReturn");
            e.printStackTrace();
        }
    }

    /**
     *  returns the result for the operation invoked by the user
     */
    protected void resultReturn(String result, HttpServletResponse response, String destURL) {
        PrintWriter out;
        try {
            Utils.debug("HTTPFrontEnd", "Result routine called");
            if (destURL != null && destURL.trim().equals("return")) {
                Utils.debug("HTTPFrontEnd", "Redirect to result called");
                redirect(response, result);
                return;
            }
            response.setContentType("text/html");
            out = response.getWriter();
            out.println("<HTML>");
            out.println("<HEAD><TITLE> Result Page </TITLE></HEAD>");
            out.println("<BODY>");
            out.println("<H1>Result: " + result + " </H1>");
            if (destURL != null) out.println("<H1>Click <A HREF=\"" + destURL + "\">here</A> to go to the next page </H1>"); else out.println("<H1>Click <A HREF=\"" + servletURL + "\">here</A>" + " to go back to the services page </H1>");
            out.println("</BODY></HTML>");
        } catch (Exception e) {
            Utils.warning("HTTPFrontEnd", "Exception thrown in resultReturn");
            e.printStackTrace();
            errorReturn(response);
        }
    }

    /** 
     * Sets up the response such that the user is redirected to the url
     * given by the parameter "destURL"
     * @param response HTTP response object
     * @param destURL Destination URL
     */
    protected void redirect(HttpServletResponse response, String destURL) {
        response.setStatus(response.SC_MOVED_TEMPORARILY);
        response.setHeader("Location", destURL);
    }
}
