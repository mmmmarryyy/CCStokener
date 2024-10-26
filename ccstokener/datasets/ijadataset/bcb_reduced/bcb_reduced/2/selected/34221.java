package tw.idv.cut7man.cuttle.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.ServletConfig;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.xmlrules.DigesterLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;
import tw.idv.cut7man.cuttle.constants.Constants;
import tw.idv.cut7man.cuttle.controller.CompositePageServlet;
import tw.idv.cut7man.cuttle.core.ConfigureDigester;
import tw.idv.cut7man.cuttle.core.Handler;
import tw.idv.cut7man.cuttle.core.exception.CoreException;
import tw.idv.cut7man.cuttle.core.plugable.ConfigurePlugable;
import tw.idv.cut7man.cuttle.filter.ResponseWeaverObject;
import tw.idv.cut7man.cuttle.helper.ActionHelper;
import tw.idv.cut7man.cuttle.helper.ParameterObj;
import tw.idv.cut7man.cuttle.util.ProcessUtil;
import tw.idv.cut7man.cuttle.util.PseudoProcessUtil;
import tw.idv.cut7man.cuttle.util.parameter.ParameterDigester;
import tw.idv.cut7man.cuttle.vo.XMLAction;
import tw.idv.cut7man.cuttle.vo.XMLActionConfig;
import tw.idv.cut7man.cuttle.vo.XMLCacheBuilder;
import tw.idv.cut7man.cuttle.vo.XMLCuttleConfiguration;
import tw.idv.cut7man.cuttle.vo.XMLEnv;
import tw.idv.cut7man.cuttle.vo.XMLForm;
import tw.idv.cut7man.cuttle.vo.XMLGlobal;
import tw.idv.cut7man.cuttle.vo.XMLPlugin;
import tw.idv.cut7man.cuttle.vo.XMLProcess;
import tw.idv.cut7man.cuttle.vo.XMLProcessException;
import tw.idv.cut7man.cuttle.vo.XMLProcessUnit;
import tw.idv.cut7man.cuttle.vo.XMLRoot;

public class CompositePageUtil {

    private CompositePageUtil() {
    }

    public static Log logger = LogFactory.getLog(CompositePageUtil.class);

    public static void parseConfigV2(List rootList, InputStream is, javax.servlet.ServletContext context, List configFileList) throws Exception {
        URL configUrl = CompositePageUtil.class.getResource("/cuttleConfigurationV2.xml");
        if (configUrl == null) configUrl = CompositePageUtil.class.getClassLoader().getResource("/cuttleConfigurationV2.xml");
        URL dtdUrl = CompositePageUtil.class.getResource("/dtd/cuttleConfiguration.dtd");
        if (dtdUrl == null) dtdUrl = CompositePageUtil.class.getClassLoader().getResource("/dtd/cuttleConfiguration.dtd");
        Digester digester = DigesterLoader.createDigester(configUrl);
        digester.setValidating(false);
        digester.register("-//Cuttle MVC Framework//DTD Cuttle Configuration 1.0//EN", dtdUrl.toString());
        XMLCuttleConfiguration cuttleConfiguration = (XMLCuttleConfiguration) digester.parse(is);
        ConfigureDigester.setXmlCuttleConfiguration(cuttleConfiguration);
        if (configFileList != null) {
            for (int i = 0; i < configFileList.size(); i++) {
                String file = (String) configFileList.get(i);
                URL url2 = CompositePageUtil.class.getResource(file);
                if (url2 == null) url2 = CompositePageUtil.class.getClassLoader().getResource(file);
                if (url2 == null) {
                    logger.error("file path:" + file + " not found!");
                }
                XMLRoot root = (XMLRoot) ConfigureDigester.parseXMLToObject(url2.openStream());
                rootList.add(root);
            }
        } else {
            for (int i = 0; i < cuttleConfiguration.getActionConfigs().size(); i++) {
                XMLActionConfig config = (XMLActionConfig) cuttleConfiguration.getActionConfigs().get(i);
                URL url2 = context.getResource(config.getResource());
                if (url2 == null) {
                    logger.error("file path:" + config.getResource() + " not found!");
                }
                XMLRoot root = (XMLRoot) ConfigureDigester.parseXMLToObject(url2.openStream());
                rootList.add(root);
            }
        }
        compositeXMLRoot(rootList);
        XMLCuttleConfiguration config = ConfigureDigester.getXmlCuttleConfiguration();
        if (config != null) {
            List processUnits = config.getProcessUnits();
            if (processUnits != null) {
                for (int i = 0; i < processUnits.size(); i++) {
                    XMLProcessUnit processUnit = (XMLProcessUnit) processUnits.get(i);
                    if (processUnit.getSpringMapping() == null || processUnit.getSpringMapping().equals("")) {
                        Class businessClass = Class.forName(processUnit.getClazz());
                        Object business = businessClass.newInstance();
                        ConfigureDigester.addObjectToPool(business);
                    }
                }
            }
        }
    }

    public static void initConfigurationV2(String cuttleConfiguration, javax.servlet.ServletContext context, List configFileList) throws Exception {
        ConfigureDigester.clearMap();
        List rootList = new ArrayList();
        InputStream is = null;
        if (cuttleConfiguration == null) {
            URL url = CompositePageUtil.class.getResource("/cuttle.xml");
            if (url == null) url = CompositePageUtil.class.getClassLoader().getResource("/cuttle.xml");
            is = url.openStream();
        } else {
            is = context.getResourceAsStream(cuttleConfiguration);
        }
        parseConfigV2(rootList, is, context, configFileList);
        if (ConfigureDigester.getXmlCuttleConfiguration() != null && ConfigureDigester.getXmlCuttleConfiguration().getPlugins() != null) {
            for (int i = 0; i < ConfigureDigester.getXmlCuttleConfiguration().getPlugins().size(); i++) {
                XMLPlugin plugin = (XMLPlugin) ConfigureDigester.getXmlCuttleConfiguration().getPlugins().get(i);
                if (plugin.getConfigurePlugable() != null && !plugin.getConfigurePlugable().equals("")) {
                    Class pluginable = Class.forName(plugin.getConfigurePlugable());
                    ConfigurePlugable pluginableObj = (ConfigurePlugable) pluginable.newInstance();
                    pluginableObj.initConfiguration(plugin.getConfigurationPath(), context);
                }
            }
        }
    }

    private static void compositeXMLRoot(List rootList) throws Exception {
        XMLRoot root = new XMLRoot();
        XMLRoot rootPre = null;
        for (int i = 0; i < rootList.size(); i++) {
            rootPre = (XMLRoot) rootList.get(i);
            for (int j = 0; j < rootPre.getXMLActions().size(); j++) {
                XMLAction action = (XMLAction) rootPre.getXMLActions().get(j);
                root.addAction(action);
            }
            for (int j = 0; j < rootPre.getXMLProcessUnits().size(); j++) {
                root.addProcessUnit((XMLProcessUnit) rootPre.getXMLProcessUnits().get(j));
            }
            if (rootPre.getEnv() != null) {
                if (root.getEnv() != null) {
                    logger.error("XMLEnv has already exist!");
                    throw new CoreException();
                }
                root.setEnv(rootPre.getEnv());
            }
            for (int j = 0; j < rootPre.getXMLForms().size(); j++) {
                root.addForm((XMLForm) rootPre.getXMLForms().get(j));
            }
            if (rootPre.getGlobal() != null) {
                root.setGlobal(rootPre.getGlobal());
            }
            if (rootPre.getCacheBuilder() != null) {
                root.setCacheBuilder(rootPre.getCacheBuilder());
            }
        }
        ConfigureDigester.setXmlRoot(root);
        for (int i = 0; i < root.getXMLActions().size(); i++) {
            XMLAction action = (XMLAction) root.getXMLActions().get(i);
            ConfigureDigester.addActionToMap(action);
        }
        for (int i = 0; i < root.getXMLActions().size(); i++) {
            XMLAction action = (XMLAction) root.getXMLActions().get(i);
            ConfigureUtil.addProcessListToMap(action);
        }
        ConfigureUtil.addObjectToPool();
    }

    /**
	 * initiate the configuration file
	 * 
	 * @param config
	 * @param context
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 */
    public static void initConfiguration(ServletConfig config, javax.servlet.ServletContext context) throws Exception {
        ConfigureDigester.clearMap();
        List rootList = new ArrayList();
        String files[] = config.getInitParameter("compositepage").trim().split(",");
        try {
            for (int i = 0; i < files.length; i++) {
                File file = new File(context.getRealPath(files[i]));
                parseConfig(file, rootList, context);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        compositeXMLRoot(rootList);
    }

    private static void parseConfig(File file, List rootList, javax.servlet.ServletContext context) {
        if (file.isDirectory()) {
            File[] fileList = file.listFiles();
            for (int j = 0; j < fileList.length; j++) {
                try {
                    if (fileList[j].isDirectory()) parseConfig(fileList[j], rootList, context);
                    if (fileList[j].getName().endsWith(".xml")) {
                        FileInputStream fis = new FileInputStream(fileList[j]);
                        XMLRoot root = ConfigureDigester.parseXMLToObject(fis);
                        rootList.add(root);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.debug(e);
                }
            }
        } else {
            try {
                if (file.getName().endsWith(".xml")) {
                    FileInputStream fis = new FileInputStream(new File(context.getRealPath(file.getName())));
                    XMLRoot root = ConfigureDigester.parseXMLToObject(fis);
                    rootList.add(root);
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.debug(e);
            }
        }
    }

    /**
	 * ��oaction�U���Ҧ�process
	 * 
	 * @param actionStr
	 * @param root
	 * @return
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
    public static List getProcesses(ActionHelper helper) throws Exception {
        ParameterObj parameter = (ParameterObj) helper.getRequest().getAttribute(Constants.CUTTLE_PARAMETER_OBJ);
        String ajaxRequest = parameter.getAjaxRequest();
        List processList = new ArrayList();
        if (ajaxRequest != null && ajaxRequest.equals("true")) {
            processList = ConfigureDigester.getActionProcessesFromMap(helper.getActionStr() + "_ajax");
        } else {
            processList = ConfigureDigester.getActionProcessesFromMap(helper.getActionStr());
        }
        if (PseudoProcessUtil.getPseudoProcessList(helper) != null) {
            processList.addAll(PseudoProcessUtil.getPseudoProcessList(helper));
        }
        return processList;
    }

    public static XMLProcess getProcess(String actionStr, String processStr) {
        XMLAction action = ConfigureDigester.getActionFromMap(actionStr);
        XMLProcess process = null;
        boolean isFound = false;
        if (action != null) for (int i = 0; i < action.getXMLProcesses().size(); i++) {
            process = (XMLProcess) action.getXMLProcesses().get(i);
            if (process.getName().equals(processStr)) {
                isFound = true;
                break;
            }
        }
        if (!isFound) process = null;
        return process;
    }

    public static List getProcessUnits() {
        List processUnits = new ArrayList();
        if (ConfigureDigester.getXmlRoot().getXMLProcessUnits() != null) processUnits.addAll(ConfigureDigester.getXmlRoot().getXMLProcessUnits());
        if (ConfigureDigester.getXmlCuttleConfiguration() != null && ConfigureDigester.getXmlCuttleConfiguration().getProcessUnits() != null) processUnits.addAll(ConfigureDigester.getXmlCuttleConfiguration().getProcessUnits());
        return processUnits;
    }

    /**
	 * ��oaction
	 * 
	 * @param actionStr
	 * @return
	 */
    public static XMLAction getAction(String actionStr) {
        XMLRoot root = ConfigureDigester.getXmlRoot();
        XMLAction action = null;
        boolean isExist = false;
        if (root != null && root.getXMLActions() != null) for (int i = 0; i < root.getXMLActions().size(); i++) {
            action = (XMLAction) root.getXMLActions().get(i);
            if (action.getName().equals(actionStr)) {
                isExist = true;
                break;
            }
        }
        if (!isExist) {
            action = null;
        }
        return action;
    }

    /**
	 * �]�wResponseWeaver����, �ھ�action.responsewrap�򪾬O�_�i��response wrap
	 * 
	 * @param helper
	 */
    public static void setResponseWeaverObject(ActionHelper helper) throws Exception {
        XMLAction action = null;
        action = ConfigureDigester.getActionFromMap(helper.getActionStr());
        if (action != null) if (helper.getProperties(Constants.CUTTLE_DO_DELAY_PROCESS) == null || !helper.getProperties(Constants.CUTTLE_DO_DELAY_PROCESS).equals("true")) {
            if (action.getResponsewrap() != null && action.getResponsewrap().equals("true")) {
                String postfix = CompositePageServlet.getPostfix();
                Enumeration parameterNames = helper.getRequest().getParameterNames();
                StringBuffer parameterStr = new StringBuffer();
                while (parameterNames.hasMoreElements()) {
                    if (parameterStr.length() == 0) {
                        parameterStr.append("?");
                    } else {
                        parameterStr.append("&");
                    }
                    String parameterKey = (String) parameterNames.nextElement();
                    String parameterValue = helper.getRequest().getParameter(parameterKey);
                    parameterValue = java.net.URLEncoder.encode(parameterValue, "UTF-8");
                    parameterStr.append(parameterKey + "=" + parameterValue);
                }
                ResponseWeaverObject weaverObject = new ResponseWeaverObject();
                weaverObject.setAction(helper.getActionStr());
                weaverObject.setWrap(true);
                weaverObject.setPostfix(postfix);
                weaverObject.setParameterStr(parameterStr.toString());
                helper.getRequest().setAttribute("responseWeaverObject", weaverObject);
            }
        }
    }

    public static StringBuffer checkSign(String source, String reg) {
        StringBuffer result = new StringBuffer();
        if (source.indexOf(reg) >= 0) {
            result.append(source.substring(0, source.indexOf(reg)));
            if (source.indexOf(reg) + 1 <= source.length()) {
                result.append(source.substring(source.indexOf(reg) + 1));
            }
        }
        if (result.indexOf(reg) >= 0) {
            return checkSign(result.toString(), reg);
        }
        return result;
    }

    public static void parseRequest(ActionHelper helper, XMLAction action) throws Exception {
        XMLForm form = getForm(action.getForm());
        if (form.getType() == null || form.getType().trim().equals("") || form.getType().equalsIgnoreCase(XMLForm.TYPE_DYNA)) {
            helper.setForm(ParameterDigester.parse(helper.getRequest()));
        } else if (form.getType().equalsIgnoreCase(XMLForm.TYPE_OBJECT)) {
            Object aForm = helper.getForm();
            if (aForm != null) {
                helper.setForm(ParameterDigester.parse(helper.getRequest(), aForm));
            } else {
                helper.setForm(ParameterDigester.parseRequest(helper.getRequest(), form.getClazz()));
            }
        }
        if (form.getScope() == null || form.getScope().trim().equals("") || form.getScope().equalsIgnoreCase(XMLForm.SCOPE_REQUEST)) {
            helper.getRequest().setAttribute(form.getName(), helper.getForm());
        } else if (form.getScope().equalsIgnoreCase(XMLForm.SCOPE_SESSION)) {
            helper.getRequest().getSession().setAttribute(form.getName(), helper.getForm());
        } else if (form.getScope().equalsIgnoreCase(XMLForm.SCOPE_APPLICATION)) {
            helper.getContext().setAttribute(form.getName(), helper.getForm());
        }
    }

    public static void clearProcessFromViewMap(ActionHelper helper) throws Exception {
        helper.addProperties("PROCESSLIST_FOR_VIEW", null);
        helper.addProperties("PROCESSLIST_FOR_VIEW_MAP", null);
    }

    /**
	 * Warnning:this method only for Cuttle internal use
	 */
    public static List getProcessesFromViewMap(ActionHelper helper) throws Exception {
        return (List) helper.getProperties("PROCESSLIST_FOR_VIEW");
    }

    public static XMLProcess getProcessFromProcessesView(ActionHelper helper, String process) {
        Map processMap = (Map) helper.getProperties("PROCESSLIST_FOR_VIEW_MAP");
        if (processMap != null) {
            XMLProcess xmlprocess = (XMLProcess) processMap.get(helper.getActionStr() + "_" + process);
            return xmlprocess;
        }
        return null;
    }

    public static XMLProcess getProcessFromProcessesViewByPageId(ActionHelper helper, String pageId) {
        Map processMap = (Map) helper.getProperties("PROCESSLIST_FOR_VIEW_MAP");
        boolean isExist = false;
        XMLProcess xmlprocess = null;
        if (processMap != null) {
            Iterator iter = processMap.entrySet().iterator();
            while (iter.hasNext()) {
                xmlprocess = (XMLProcess) ((Entry) iter.next()).getValue();
                if (xmlprocess.getPageId() != null && xmlprocess.getPageId().equals(pageId)) {
                    isExist = true;
                    break;
                }
            }
            if (!isExist) return null;
            return xmlprocess;
        }
        return null;
    }

    /**
	 * Warnning:this method only for Cuttle internal use
	 */
    public static void addProcessToViewMap(ActionHelper helper, XMLProcess process) throws Exception {
        List processList = getProcessesFromViewMap(helper);
        Map processMap = (Map) helper.getProperties("PROCESSLIST_FOR_VIEW_MAP");
        if (processList == null) {
            processList = new ArrayList();
            helper.addProperties("PROCESSLIST_FOR_VIEW", processList);
            processMap = new HashMap();
            helper.addProperties("PROCESSLIST_FOR_VIEW_MAP", processMap);
        }
        processList.add(process);
        processMap.put(helper.getActionStr() + "_" + process.getName(), process);
    }

    public static XMLProcess getAlternativeProcess(ActionHelper helper, String actionStr, String process) throws Exception {
        XMLAction action = ConfigureDigester.getActionFromMap(actionStr);
        if (action.getAlternative() != null) {
            if (action.getAlternative().getXMLProcesses() != null) {
                for (int i = 0; i < action.getAlternative().getXMLProcesses().size(); i++) {
                    XMLProcess xmlprocess = (XMLProcess) action.getAlternative().getXMLProcesses().get(i);
                    if (xmlprocess.getName().equals(process)) return xmlprocess;
                }
            }
        }
        return null;
    }

    public static XMLForm getForm(String formName) {
        XMLRoot root = ConfigureDigester.getXmlRoot();
        List formList = root.getXMLForms();
        for (int i = 0; i < formList.size(); i++) {
            XMLForm form = (XMLForm) formList.get(i);
            if (form.getName() != null && form.getName().equals(formName)) {
                return form;
            }
        }
        XMLCuttleConfiguration conf = ConfigureDigester.getXmlCuttleConfiguration();
        if (conf != null) {
            formList = conf.getForms();
            for (int i = 0; i < formList.size(); i++) {
                XMLForm form = (XMLForm) formList.get(i);
                if (form.getName() != null && form.getName().equals(formName)) {
                    return form;
                }
            }
        }
        return null;
    }

    public static XMLForm getFormByClazz(String clazz) {
        XMLRoot root = ConfigureDigester.getXmlRoot();
        List formList = root.getXMLForms();
        for (int i = 0; i < formList.size(); i++) {
            XMLForm form = (XMLForm) formList.get(i);
            if (form.getClazz() != null && form.getClazz().equals(clazz)) {
                return form;
            }
        }
        XMLCuttleConfiguration conf = ConfigureDigester.getXmlCuttleConfiguration();
        if (conf != null) {
            formList = conf.getForms();
            for (int i = 0; i < formList.size(); i++) {
                XMLForm form = (XMLForm) formList.get(i);
                if (form.getClazz() != null && form.getClazz().equals(clazz)) {
                    return form;
                }
            }
        }
        return null;
    }

    public static List getActionExceptions(String actionName) {
        XMLAction action = ConfigureDigester.getActionFromMap(actionName);
        List exceptionList = action.getXMLActionExceptions();
        return exceptionList;
    }

    public static List getProcessExceptions(String actionName, String processName) {
        XMLProcess process = ConfigureDigester.getProcessFromMap(actionName, processName);
        if (process == null) return new ArrayList();
        List exceptionList = process.getProcessExceptions();
        return exceptionList;
    }

    public static XMLGlobal getGlobal() {
        XMLGlobal global = null;
        if (ConfigureDigester.getXmlRoot().getGlobal() != null) {
            global = ConfigureDigester.getXmlRoot().getGlobal();
        } else if (ConfigureDigester.getXmlCuttleConfiguration() != null && ConfigureDigester.getXmlCuttleConfiguration().getGlobal() != null) {
            global = ConfigureDigester.getXmlCuttleConfiguration().getGlobal();
        }
        return global;
    }

    public static List getGlobalActionExceptions(String actionName) {
        XMLGlobal global = CompositePageUtil.getGlobal();
        if (global != null) {
            List exceptionList = global.getActionExceptions();
            return exceptionList;
        }
        return new ArrayList();
    }

    public static List getGlobalProcessExceptions(String actionName, String processName) {
        XMLGlobal global = CompositePageUtil.getGlobal();
        if (global != null) {
            List exceptionList = global.getProcessExceptions();
            return exceptionList;
        }
        return new ArrayList();
    }

    public static void processExceptionForward(ActionHelper helper, String actionName, XMLProcessException processException) throws Exception {
        XMLProcess process = new XMLProcess();
        process.setCache(processException.getCache());
        process.setWindow(processException.getWindow());
        process.setDecorator(processException.getDecorator());
        process.setName(processException.getProcessName());
        process.setNonwrap(processException.getNowrap());
        process.setOutput(processException.getOutput());
        process.setRefreshZone(processException.getRefreshZone());
        process.setScript(processException.getScript());
        Handler.alternativeProcess(helper, process);
        ProcessUtil.replaceCurrentProcess(helper, process);
    }

    public static XMLEnv getEnv() {
        XMLEnv env = null;
        if (ConfigureDigester.getXmlRoot().getEnv() != null) {
            env = ConfigureDigester.getXmlRoot().getEnv();
        } else {
            if (ConfigureDigester.getXmlCuttleConfiguration() != null && ConfigureDigester.getXmlCuttleConfiguration().getEnv() != null) {
                env = ConfigureDigester.getXmlCuttleConfiguration().getEnv();
            }
        }
        return env;
    }

    public static XMLCacheBuilder getCacheBuilder() {
        XMLCacheBuilder cacheBuilder = null;
        if (ConfigureDigester.getXmlRoot().getCacheBuilder() != null) {
            cacheBuilder = ConfigureDigester.getXmlRoot().getCacheBuilder();
        } else if (ConfigureDigester.getXmlCuttleConfiguration() != null && ConfigureDigester.getXmlCuttleConfiguration().getCacheBuilder() != null) {
            cacheBuilder = ConfigureDigester.getXmlCuttleConfiguration().getCacheBuilder();
        }
        return cacheBuilder;
    }

    public static List getXMLActions() {
        List xmlActions = new ArrayList();
        if (ConfigureDigester.getXmlRoot().getXMLActions() != null) {
            xmlActions.addAll(ConfigureDigester.getXmlRoot().getXMLActions());
        }
        return xmlActions;
    }
}
