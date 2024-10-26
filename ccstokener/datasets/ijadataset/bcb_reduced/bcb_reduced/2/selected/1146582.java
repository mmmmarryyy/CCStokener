package com.taobao.top.analysis.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import javax.script.ScriptException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.taobao.top.analysis.AnalysisConstants;
import com.taobao.top.analysis.data.Alias;
import com.taobao.top.analysis.data.EntryValueOperator;
import com.taobao.top.analysis.data.Report;
import com.taobao.top.analysis.data.ReportAlert;
import com.taobao.top.analysis.data.ReportEntry;
import com.taobao.top.analysis.data.ReportEntryValueType;
import com.taobao.top.analysis.data.Rule;

/**
 * 报表工具类
 * 
 * @author fangweng
 * 
 */
public class ReportUtil {

    private static final Log logger = LogFactory.getLog(ReportUtil.class);

    private static Map<Object, Object> localCache = new ConcurrentHashMap<Object, Object>();

    /**
	 * 根据别名定义来转换报表模型中定义的key，直接转换为实际的列号
	 * 
	 * @param keys
	 * @param aliasPool
	 */
    public static void transformVars(String[] keys, Map<String, Alias> aliasPool) {
        if (aliasPool != null && aliasPool.size() > 0 && keys != null && keys.length > 0) {
            for (int i = 0; i < keys.length; i++) {
                if (aliasPool.get(keys[i]) != null) keys[i] = aliasPool.get(keys[i]).getKey();
            }
        }
    }

    /**
	 * 根据别名定义来转换报表模型中定义的key，直接转换为实际的列号
	 * 
	 * @param key
	 * @param aliasPool
	 * @return
	 */
    public static String transformVar(String key, Map<String, Alias> aliasPool) {
        String result = key;
        if (aliasPool != null && aliasPool.size() > 0 && aliasPool.get(key) != null) {
            result = aliasPool.get(key).getKey();
        }
        return result;
    }

    /**
	 * 根据定义获取对应日志行产生的key
	 * 
	 * @param entry
	 * @param contents
	 * @return
	 */
    public static String generateKey(ReportEntry entry, String[] contents) {
        StringBuilder key = new StringBuilder();
        try {
            boolean checkResult = false;
            if (entry.getConditionKStack() != null && entry.getConditionKStack().size() > 0) {
                for (int i = 0; i < entry.getConditionKStack().size(); i++) {
                    Object conditionKey = entry.getConditionKStack().get(i);
                    String operator = entry.getConditionOpStack().get(i);
                    String conditionValue = entry.getConditionVStack().get(i);
                    int k = -1;
                    if (!conditionKey.equals(AnalysisConstants.RECORD_LENGTH)) {
                        k = (Integer) conditionKey;
                    }
                    checkResult = checkKeyCondition(operator, k, conditionValue, contents);
                    if (entry.isAndCondition() && !checkResult) return AnalysisConstants.IGNORE_PROCESS;
                    if (!entry.isAndCondition() && checkResult) break;
                }
            }
            if (!entry.isAndCondition() && !checkResult) return AnalysisConstants.IGNORE_PROCESS;
            for (String c : entry.getKeys()) {
                if (c.equals(AnalysisConstants.GLOBAL_KEY)) return AnalysisConstants.GLOBAL_KEY;
                key.append(contents[Integer.valueOf(c) - 1]).append("--");
            }
        } catch (Exception ex) {
            return AnalysisConstants.IGNORE_PROCESS;
        }
        return key.toString();
    }

    /**
	 * 返回是否符合条件
	 * @param operator
	 * @param conditionKey
	 * @param conditionValue
	 * @param contents
	 * @return
	 */
    private static boolean checkKeyCondition(String operator, int conditionKey, String conditionValue, String[] contents) {
        boolean result = false;
        if (operator.equals(AnalysisConstants.CONDITION_EQUAL)) {
            if (conditionKey > 0) result = contents[conditionKey - 1].equals(conditionValue); else result = contents.length == Integer.valueOf(conditionValue);
        } else if (operator.equals(AnalysisConstants.CONDITION_NOT_EQUAL)) {
            if (conditionKey > 0) result = !contents[conditionKey - 1].equals(conditionValue); else result = contents.length != Integer.valueOf(conditionValue);
        } else {
            double cmpValue = 0;
            if (conditionKey > 0) cmpValue = Double.valueOf(contents[conditionKey - 1]) - Double.valueOf(conditionValue); else cmpValue = contents.length - Integer.valueOf(conditionValue);
            if (operator.equals(AnalysisConstants.CONDITION_EQUALORGREATER)) return cmpValue >= 0;
            if (operator.equals(AnalysisConstants.CONDITION_EQUALORLESSER)) return cmpValue <= 0;
            if (operator.equals(AnalysisConstants.CONDITION_GREATER)) return cmpValue > 0;
            if (operator.equals(AnalysisConstants.CONDITION_LESSER)) return cmpValue < 0;
        }
        return result;
    }

    /**
	 * 根据接口定义获取实际的接口实现实例
	 * 
	 * @param <I>
	 * @param interfaceDefinition
	 * @param classLoader
	 * @param className
	 * @param needCache
	 * @return
	 */
    @SuppressWarnings("unchecked")
    public static <I> I getInstance(Class<I> interfaceDefinition, ClassLoader classLoader, String className, boolean needCache) {
        if (needCache) {
            Object instance = localCache.get(className);
            if (instance == null) {
                instance = newInstance(interfaceDefinition, className, classLoader);
                localCache.put(className, instance);
            }
            return (I) instance;
        } else {
            return newInstance(interfaceDefinition, className, classLoader);
        }
    }

    /**
	 * 创建实例
	 * 
	 * @param <I>
	 * @param interfaceDefinition
	 * @param className
	 * @param classLoader
	 * @return
	 */
    @SuppressWarnings("unchecked")
    private static <I> I newInstance(Class<I> interfaceDefinition, String className, ClassLoader classLoader) {
        try {
            Class<I> spiClass;
            if (classLoader == null) {
                spiClass = (Class<I>) Class.forName(className);
            } else {
                spiClass = (Class<I>) classLoader.loadClass(className);
            }
            return spiClass.newInstance();
        } catch (ClassNotFoundException x) {
            throw new java.lang.RuntimeException("Provider " + className + " not found", x);
        } catch (Exception ex) {
            throw new java.lang.RuntimeException("Provider " + className + " could not be instantiated: " + ex, ex);
        }
    }

    /**
	 * 排序
	 * 
	 * @param list
	 * @param orders
	 */
    public static void doOrder(ArrayList<Object[]> list, String[] orders, Report report) {
        if (orders == null || (orders != null && (orders.length == 0))) return;
        int[] columns = new int[orders.length];
        boolean[] isDesc = new boolean[orders.length];
        for (int i = 0; i < isDesc.length; i++) {
            isDesc[i] = true;
        }
        int index = 0;
        for (String order : orders) {
            if (order.startsWith("+") || order.startsWith("-")) {
                if (order.startsWith("+")) isDesc[index] = false;
                order = order.substring(1);
            }
            for (ReportEntry entry : report.getReportEntrys()) {
                if (order.equals(entry.getName())) {
                    break;
                }
                columns[index] += 1;
            }
            if (columns[index] >= report.getReportEntrys().size()) columns[index] = 0;
            index += 1;
        }
        Collections.sort(list, new ReportOrderComparator<Object[]>(columns, isDesc));
    }

    /**
	 * 根据配置创建比对告警通知
	 * 
	 * @param 当前分析后的报表
	 * @param 告警设置
	 * @return
	 */
    public static List<String> generateAlerts(Map<String, Report> reportPool, List<ReportAlert> alerts, String dir) {
        if (alerts == null || reportPool == null) return null;
        List<String> result = new ArrayList<String>();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        String today = new StringBuilder().append(calendar.get(Calendar.YEAR)).append("-").append(calendar.get(Calendar.MONTH) + 1).append("-").append(calendar.get(Calendar.DAY_OF_MONTH)).toString();
        for (ReportAlert alert : alerts) {
            try {
                Report report = reportPool.get(alert.getReportId());
                if (report == null) continue;
                String entryname = alert.getEntryname();
                Map<String, Integer> keyentrys = new HashMap<String, Integer>();
                String keyentryStr = alert.getKeyentry();
                if (keyentryStr != null && !"".equals(keyentryStr)) {
                    String[] keys = keyentryStr.split(",");
                    if (keys != null && keys.length > 0) {
                        for (String k : keys) {
                            keyentrys.put(k, -1);
                        }
                    }
                }
                String alerttype = alert.getAlerttype();
                String valve = alert.getValve();
                String reportname = report.getFile();
                File newReport;
                File oldReport;
                StringBuilder alertBuffer = new StringBuilder();
                int index = -1;
                double alertValve;
                String operator = null;
                Map<String, String> oldReportValues = new HashMap<String, String>();
                if (report == null || entryname == null || "".equals(entryname) || alerttype == null || "".equals(alerttype) || valve == null || "".equals(valve)) continue;
                if (valve.indexOf(">") < 0 && valve.indexOf("<") < 0) alertValve = Double.valueOf(valve); else {
                    if (valve.indexOf(">") >= 0) operator = ">"; else operator = "<";
                    alertValve = Double.valueOf(valve.substring(1));
                }
                if (report.getReportEntrys() != null) for (int i = 0; i < report.getReportEntrys().size(); i++) {
                    ReportEntry entry = report.getReportEntrys().get(i);
                    if (entry.getName().equals(entryname)) index = i;
                    if (keyentrys != null && keyentrys.get(entry.getName()) != null) keyentrys.put(entry.getName(), i);
                }
                if (index == -1) continue;
                if (keyentrys.size() >= 0) {
                    Iterator<String> keys = keyentrys.keySet().iterator();
                    while (keys.hasNext()) {
                        alertBuffer.append(keys.next()).append(",");
                    }
                }
                if (alerttype.equals(AnalysisConstants.ALERT_TYPE_NOW)) alertBuffer.append(entryname).append(",").append("预设阀值").append(AnalysisConstants.RETURN); else alertBuffer.append(entryname).append("(").append(getCompareDateStr(alerttype)).append(")").append(",").append(entryname).append("(").append(today).append(")").append(",").append("预设阀值").append(AnalysisConstants.RETURN);
                if (alerttype.equals(AnalysisConstants.ALERT_TYPE_NOW)) {
                    newReport = new File(ReportUtil.getReportFileLocation(reportname, dir, System.currentTimeMillis(), false));
                    if (!newReport.exists()) continue;
                    BufferedReader newReader = null;
                    try {
                        newReader = new BufferedReader(new FileReader(newReport));
                        if (keyentrys.size() == 0) {
                            newReader.readLine();
                            String content = newReader.readLine();
                            if (content != null) checkAlartRecord(alertBuffer, AnalysisConstants.ALERT_TYPE_NOW, operator, alertValve, content.split(","), null, index, keyentrys);
                        } else {
                            String content = newReader.readLine();
                            while ((content = newReader.readLine()) != null) {
                                checkAlartRecord(alertBuffer, AnalysisConstants.ALERT_TYPE_NOW, operator, alertValve, content.split(","), null, index, keyentrys);
                            }
                        }
                    } catch (Exception ex) {
                        logger.error(ex, ex);
                    } finally {
                        if (newReader != null) newReader.close();
                    }
                } else {
                    long date = getCompareDate(alerttype);
                    oldReport = new File(ReportUtil.getReportFileLocation(reportname, dir, date, false));
                    newReport = new File(ReportUtil.getReportFileLocation(reportname, dir, System.currentTimeMillis(), false));
                    if (!oldReport.exists() || !newReport.exists()) continue;
                    BufferedReader newReader = new BufferedReader(new FileReader(newReport));
                    BufferedReader oldReader = new BufferedReader(new FileReader(oldReport));
                    try {
                        String content = oldReader.readLine();
                        while ((content = oldReader.readLine()) != null) {
                            if (keyentrys.size() > 0) {
                                oldReportValues.put(generateAlertKey(keyentrys, content), content);
                            } else {
                                oldReportValues.put(AnalysisConstants.GLOBAL_KEY, content);
                                break;
                            }
                        }
                        if (keyentrys.size() == 0) {
                            newReader.readLine();
                            content = newReader.readLine();
                            checkAlartRecord(alertBuffer, alerttype, operator, alertValve, content.split(","), oldReportValues.get(AnalysisConstants.GLOBAL_KEY).split(","), index, keyentrys);
                        } else {
                            content = newReader.readLine();
                            while ((content = newReader.readLine()) != null) {
                                String dest = oldReportValues.get(generateAlertKey(keyentrys, content));
                                if (dest == null) checkAlartRecord(alertBuffer, alerttype, operator, alertValve, content.split(","), null, index, keyentrys); else checkAlartRecord(alertBuffer, alerttype, operator, alertValve, content.split(","), dest.split(","), index, keyentrys);
                            }
                        }
                    } catch (Exception ex) {
                        logger.error(ex, ex);
                    } finally {
                        if (newReader != null) newReader.close();
                        if (oldReader != null) oldReader.close();
                    }
                }
                result.add(alertBuffer.toString());
            } catch (Exception ex) {
                logger.error(ex, ex);
            }
        }
        return result;
    }

    private static String generateAlertKey(Map<String, Integer> keyentrys, String content) {
        StringBuilder result = new StringBuilder();
        if (keyentrys == null || (keyentrys != null && keyentrys.size() == 0) || content == null || "".equals(content)) return result.toString();
        String[] c = content.split(",");
        Iterator<Integer> values = keyentrys.values().iterator();
        while (values.hasNext()) {
            int v = values.next();
            if (v < c.length) result.append(c[v]).append("--");
        }
        return result.toString();
    }

    private static long getCompareDate(String alerttype) {
        Calendar calendar = Calendar.getInstance();
        long date = 0;
        if (alerttype.equals(AnalysisConstants.ALERT_TYPE_DAY)) {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            date = calendar.getTimeInMillis();
        } else if (alerttype.equals(AnalysisConstants.ALERT_TYPE_WEEK)) {
            calendar.add(Calendar.WEEK_OF_MONTH, -1);
            date = calendar.getTimeInMillis();
        } else if (alerttype.equals(AnalysisConstants.ALERT_TYPE_MONTH)) {
            calendar.add(Calendar.MONTH, -1);
            date = calendar.getTimeInMillis();
        }
        return date;
    }

    private static String getCompareDateStr(String alerttype) {
        StringBuilder result = new StringBuilder();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        if (alerttype.equals(AnalysisConstants.ALERT_TYPE_DAY)) {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        } else if (alerttype.equals(AnalysisConstants.ALERT_TYPE_WEEK)) {
            calendar.add(Calendar.WEEK_OF_MONTH, -1);
        } else if (alerttype.equals(AnalysisConstants.ALERT_TYPE_MONTH)) {
            calendar.add(Calendar.MONTH, -1);
        }
        result.append(calendar.get(Calendar.YEAR)).append("-").append(calendar.get(Calendar.MONTH) + 1).append("-").append(calendar.get(Calendar.DAY_OF_MONTH));
        return result.toString();
    }

    /**
	 * 校验是否需要告警
	 * 
	 * @param alertBuffer
	 * @param alertType
	 * @param operator
	 * @param valve
	 * @param newValues
	 * @param oldValues
	 * @param compareIndex
	 * @param keyIndex
	 */
    private static void checkAlartRecord(StringBuilder alertBuffer, String alertType, String operator, Double valve, String[] newValues, String[] oldValues, int compareIndex, Map<String, Integer> keyentrys) {
        if (newValues == null || (newValues != null && newValues.length - 1 < compareIndex)) return;
        if (alertType.equals(AnalysisConstants.ALERT_TYPE_NOW)) {
            if (operator == null) return;
            if ((operator.equals(">") && Double.valueOf(newValues[compareIndex].toLowerCase()) > valve) || (operator.equals("<") && Double.valueOf(newValues[compareIndex].toLowerCase()) < valve)) {
                if (keyentrys.size() > 0) {
                    Iterator<String> keys = keyentrys.keySet().iterator();
                    while (keys.hasNext()) {
                        alertBuffer.append(newValues[keyentrys.get(keys.next())]).append(",");
                    }
                    alertBuffer.append(Double.valueOf(newValues[compareIndex].toLowerCase())).append(",").append(valve).append(AnalysisConstants.RETURN);
                } else alertBuffer.append(Double.valueOf(newValues[compareIndex].toLowerCase())).append(",").append(valve).append(AnalysisConstants.RETURN);
            }
            return;
        } else {
            double diff = 0;
            if (oldValues == null || compareIndex > oldValues.length - 1) diff = Double.valueOf(newValues[compareIndex]); else diff = Double.valueOf(newValues[compareIndex]) - Double.valueOf(oldValues[compareIndex]);
            boolean alertflag = false;
            if (operator == null) {
                if (java.lang.Math.abs(diff) > valve) {
                    alertflag = true;
                }
            } else {
                if (operator.equals(">") && diff > valve) alertflag = true;
                if (operator.equals("<") && diff < -valve) alertflag = true;
            }
            if (alertflag) if (keyentrys.size() > 0) {
                Iterator<String> keys = keyentrys.keySet().iterator();
                while (keys.hasNext()) {
                    alertBuffer.append(newValues[keyentrys.get(keys.next())]).append(",");
                }
                if (oldValues == null || compareIndex > oldValues.length - 1) {
                    alertBuffer.append(0).append(",").append(Double.valueOf(newValues[compareIndex])).append(",").append(valve).append(AnalysisConstants.RETURN);
                } else {
                    alertBuffer.append(Double.valueOf(oldValues[compareIndex])).append(",").append(Double.valueOf(newValues[compareIndex])).append(",").append(valve).append(AnalysisConstants.RETURN);
                }
            } else {
                if (oldValues == null || compareIndex > oldValues.length - 1) alertBuffer.append(0).append(",").append(Double.valueOf(newValues[compareIndex])).append(",").append(valve).append(AnalysisConstants.RETURN); else alertBuffer.append(Double.valueOf(oldValues[compareIndex])).append(",").append(Double.valueOf(newValues[compareIndex])).append(",").append(valve).append(AnalysisConstants.RETURN);
            }
        }
    }

    /**
	 * 格式化结果,很消耗...
	 * 
	 * @param formatStack
	 * @param value
	 * @return
	 */
    public static Object formatValue(List<String> formatStack, Object value) {
        Object result = value;
        try {
            for (String filter : formatStack) {
                if (filter.startsWith(AnalysisConstants.CONDITION_ROUND)) {
                    int round = Integer.valueOf(filter.substring(AnalysisConstants.CONDITION_ROUND.length()));
                    double r = Math.pow(10, round);
                    if (value instanceof Double) {
                        result = (Double) (Math.round((Double) value * r) / r);
                    } else result = (Double) (Math.round((Double.valueOf(value.toString()) * r)) / r);
                    continue;
                }
            }
        } catch (Exception ex) {
            logger.error(ex, ex);
        }
        return result;
    }

    /**
	 * 检查参数是否符合过滤器定义
	 * 
	 * @param valuefilterOpStack
	 * @param valuefilterStack
	 * @param value
	 * @return
	 */
    public static boolean checkValue(List<String> valuefilterOpStack, List<String> valuefilterStack, Object value) {
        boolean result = true;
        if (valuefilterStack == null || (valuefilterStack != null && valuefilterStack.size() == 0)) return result;
        try {
            for (int i = 0; i < valuefilterStack.size(); i++) {
                String filterValue = valuefilterStack.get(i);
                String filterOpt = valuefilterOpStack.get(i);
                if (filterOpt.equals(AnalysisConstants.CONDITION_ISNUMBER)) {
                    Double.valueOf(value.toString());
                    continue;
                }
                if (filterOpt.equals(AnalysisConstants.CONDITION_EQUAL)) {
                    Double v = Double.valueOf(value.toString());
                    Double compareValue = Double.valueOf(filterValue);
                    if (v == compareValue) {
                        continue;
                    } else return false;
                }
                if (filterOpt.equals(AnalysisConstants.CONDITION_EQUALORGREATER)) {
                    Double v = Double.valueOf(value.toString());
                    Double compareValue = Double.valueOf(filterValue);
                    if (v >= compareValue) {
                        continue;
                    } else return false;
                }
                if (filterOpt.equals(AnalysisConstants.CONDITION_EQUALORLESSER)) {
                    Double v = Double.valueOf(value.toString());
                    Double compareValue = Double.valueOf(filterValue);
                    if (v <= compareValue) {
                        continue;
                    } else return false;
                }
                if (filterOpt.equals(AnalysisConstants.CONDITION_GREATER)) {
                    Double v = Double.valueOf(value.toString());
                    Double compareValue = Double.valueOf(filterValue);
                    if (v > compareValue) {
                        continue;
                    } else return false;
                }
                if (filterOpt.equals(AnalysisConstants.CONDITION_LESSER)) {
                    Double v = Double.valueOf(value.toString());
                    Double compareValue = Double.valueOf(filterValue);
                    if (v < compareValue) {
                        continue;
                    } else return false;
                }
                if (filterOpt.equals(AnalysisConstants.CONDITION_NOT_EQUAL)) {
                    Double v = Double.valueOf(value.toString());
                    Double compareValue = Double.valueOf(filterValue);
                    if (!v.equals(compareValue)) {
                        continue;
                    } else return false;
                }
            }
        } catch (Exception ex) {
            result = false;
        }
        return result;
    }

    /**
	 * 编译分析规则模型
	 * @param 配置文件
	 * @param entry定义池
	 * @param 父级entry定义池
	 * @param 报表定义池
	 * @param 别名定义池
	 * @param 告警定义池
	 */
    public static void buildReportModule(String configFile, Rule rule) {
        InputStream in = null;
        XMLEventReader r = null;
        ReportEntry currentEntry = null;
        Report report = null;
        StringBuilder globalConditions = new StringBuilder();
        StringBuilder globalValuefilter = new StringBuilder();
        List<String> globalMapClass = new ArrayList<String>();
        String domain = null;
        String localdir = new StringBuilder().append(System.getProperty("user.dir")).append(File.separatorChar).toString();
        if (configFile == null || "".equals(configFile)) {
            String error = "configFile can not be null !";
            logger.error(error);
            throw new java.lang.RuntimeException(error);
        }
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (configFile.startsWith("file:")) {
                try {
                    in = new java.io.FileInputStream(new File(configFile.substring(configFile.indexOf("file:") + "file:".length())));
                } catch (Exception e) {
                    logger.error(e);
                }
                if (in == null) in = new java.io.FileInputStream(new File(localdir + configFile.substring(configFile.indexOf("file:") + "file:".length())));
            } else {
                URL url = loader.getResource(configFile);
                if (url == null) {
                    String error = "configFile: " + configFile + " not exist !";
                    logger.error(error);
                    throw new java.lang.RuntimeException(error);
                }
                in = url.openStream();
            }
            r = factory.createXMLEventReader(in);
            List<String> parents = new ArrayList<String>();
            while (r.hasNext()) {
                XMLEvent event = r.nextEvent();
                if (event.isStartElement()) {
                    StartElement start = event.asStartElement();
                    String tag = start.getName().getLocalPart();
                    if (tag.equalsIgnoreCase("domain")) {
                        if (start.getAttributeByName(new QName("", "value")) != null) {
                            domain = start.getAttributeByName(new QName("", "value")).getValue();
                            rule.setDomain(domain);
                        }
                        continue;
                    }
                    if (tag.equalsIgnoreCase("alias")) {
                        Alias alias = new Alias();
                        alias.setName(start.getAttributeByName(new QName("", "name")).getValue());
                        alias.setKey(start.getAttributeByName(new QName("", "key")).getValue());
                        rule.getAliasPool().put(alias.getName(), alias);
                        continue;
                    }
                    if (tag.equalsIgnoreCase("global-condition")) {
                        if (start.getAttributeByName(new QName("", "value")) != null) {
                            globalConditions.append(start.getAttributeByName(new QName("", "value")).getValue()).append("&");
                        }
                        continue;
                    }
                    if (tag.equalsIgnoreCase("global-mapClass")) {
                        if (start.getAttributeByName(new QName("", "value")) != null) {
                            globalMapClass.add(start.getAttributeByName(new QName("", "value")).getValue());
                        }
                        continue;
                    }
                    if (tag.equalsIgnoreCase("global-valuefilter")) {
                        if (start.getAttributeByName(new QName("", "value")) != null) {
                            globalValuefilter.append(start.getAttributeByName(new QName("", "value")).getValue()).append("&");
                        }
                        continue;
                    }
                    if (tag.equalsIgnoreCase("ReportEntry") || tag.equalsIgnoreCase("entry")) {
                        ReportEntry entry = new ReportEntry();
                        currentEntry = entry;
                        if (tag.equalsIgnoreCase("ReportEntry")) setReportEntry(true, start, entry, report, rule.getEntryPool(), rule.getAliasPool(), globalConditions, globalValuefilter, globalMapClass, parents); else {
                            setReportEntry(false, start, entry, report, rule.getEntryPool(), rule.getAliasPool(), globalConditions, globalValuefilter, globalMapClass, parents);
                        }
                        if (entry.getId() != null) {
                            rule.getEntryPool().put(entry.getId(), entry);
                        }
                        if (tag.equalsIgnoreCase("entry")) {
                            if (entry.getId() != null) rule.getReferEntrys().put(entry.getId(), entry); else if (report.getReportEntrys() != null && report.getReportEntrys().size() > 0) rule.getReferEntrys().put(report.getReportEntrys().get(report.getReportEntrys().size() - 1).getId(), report.getReportEntrys().get(report.getReportEntrys().size() - 1));
                        }
                        ReportEntry _tmpEntry = entry;
                        if (_tmpEntry.getId() == null && report.getReportEntrys() != null && report.getReportEntrys().size() > 0) _tmpEntry = report.getReportEntrys().get(report.getReportEntrys().size() - 1);
                        if (_tmpEntry.getBindingStack() != null) {
                            if (_tmpEntry.getValueExpression() != null && _tmpEntry.getValueExpression().indexOf("entry(") >= 0) for (String k : _tmpEntry.getBindingStack()) {
                                rule.getReferEntrys().put(k, null);
                            }
                        }
                        continue;
                    }
                    if (tag.equalsIgnoreCase("report")) {
                        if (report != null) rule.getReportPool().put(report.getId(), report);
                        report = new Report();
                        setReport(start, report, rule.getReportPool());
                        continue;
                    }
                    if (tag.equalsIgnoreCase("entryList")) {
                        report.setReportEntrys(new ArrayList<ReportEntry>());
                        continue;
                    }
                    if (tag.equalsIgnoreCase("alert")) {
                        ReportAlert alert = new ReportAlert();
                        setAlert(start, alert);
                        rule.getAlerts().add(alert);
                        continue;
                    }
                }
                if (event.isEndElement()) {
                    EndElement end = event.asEndElement();
                    String tag = end.getName().getLocalPart();
                    if (tag.equalsIgnoreCase("reports") && report != null) {
                        rule.getReportPool().put(report.getId(), report);
                        continue;
                    }
                }
            }
            for (Iterator<String> iterator = parents.iterator(); iterator.hasNext(); ) {
                String parent = iterator.next();
                ReportEntry parentEntry = rule.getEntryPool().get(parent);
                rule.getParentEntryPool().put(parent, parentEntry);
            }
            if (rule.getReferEntrys() != null && rule.getReferEntrys().size() > 0) {
                Iterator<Entry<String, ReportEntry>> iter = rule.getEntryPool().entrySet().iterator();
                StringBuilder invalidKeys = new StringBuilder();
                while (iter.hasNext()) {
                    Entry<String, ReportEntry> e = iter.next();
                    if (!rule.getReferEntrys().containsKey(e.getKey())) {
                        iter.remove();
                        invalidKeys.append(e.getKey()).append(",");
                    }
                }
                logger.error("File: " + configFile + " ----- remove invalid entry define : " + invalidKeys.toString());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            if (currentEntry != null && currentEntry.getName() != null) logger.error(new StringBuilder("Entry : ").append(currentEntry.getName()).toString());
            throw new RuntimeException("buildReportModule error!");
        } finally {
            try {
                if (r != null) r.close();
                if (in != null) in.close();
                r = null;
                in = null;
            } catch (Exception ex) {
                throw new RuntimeException("processConfigURL error !", ex);
            }
        }
    }

    /**
	 * 构建告警信息
	 * @param 节点
	 * @param 告警池
	 */
    public static void setAlert(StartElement start, ReportAlert alert) {
        alert.setReportId(start.getAttributeByName(new QName("", "reportId")).getValue());
        alert.setEntryname(start.getAttributeByName(new QName("", "entryname")).getValue());
        alert.setAlerttype(start.getAttributeByName(new QName("", "alerttype")).getValue());
        alert.setValve(start.getAttributeByName(new QName("", "valve")).getValue());
        if (start.getAttributeByName(new QName("", "keyentry")) != null) {
            alert.setKeyentry(start.getAttributeByName(new QName("", "keyentry")).getValue());
        }
    }

    /**
	 * 构建报表对象
	 * @param 数据节点
	 * @param 上一个节点
	 * @param 报表池
	 */
    public static void setReport(StartElement start, Report report, Map<String, Report> reportPool) {
        if (start.getAttributeByName(new QName("", "id")) != null) {
            report.setId(start.getAttributeByName(new QName("", "id")).getValue());
        }
        if (start.getAttributeByName(new QName("", "file")) != null) {
            report.setFile(start.getAttributeByName(new QName("", "file")).getValue());
        }
        if (start.getAttributeByName(new QName("", "period")) != null) {
            report.setPeriod(Boolean.valueOf(start.getAttributeByName(new QName("", "period")).getValue()));
        }
        if (start.getAttributeByName(new QName("", "mailto")) != null) {
            report.setMailto(start.getAttributeByName(new QName("", "mailto")).getValue());
        }
        if (start.getAttributeByName(new QName("", "orderby")) != null) {
            report.setOrderby(start.getAttributeByName(new QName("", "orderby")).getValue());
        }
        if (start.getAttributeByName(new QName("", "chartTitle")) != null) {
            report.setChartTitle(start.getAttributeByName(new QName("", "chartTitle")).getValue());
        }
        if (start.getAttributeByName(new QName("", "export")) != null) {
            report.setExport(start.getAttributeByName(new QName("", "export")).getValue());
        }
        if (start.getAttributeByName(new QName("", "chartType")) != null) {
            report.setChartType(start.getAttributeByName(new QName("", "chartType")).getValue());
        }
        if (start.getAttributeByName(new QName("", "rowCount")) != null) {
            report.setRowCount(Integer.valueOf(start.getAttributeByName(new QName("", "rowCount")).getValue()));
        }
        if (start.getAttributeByName(new QName("", "exportCount")) != null) {
            report.setExportCount(Integer.valueOf(start.getAttributeByName(new QName("", "exportCount")).getValue()));
        }
        Attribute attr = start.getAttributeByName(new QName("", "condition"));
        if (attr != null) {
            report.setConditions(attr.getValue());
        }
    }

    /**
	 * 构建报表entry对象
	 * @param 是否是公用的entry，非公用的定义在report对象中
	 * @param 数据节点
	 * @param 当前处理的entry
	 * @param 当前隶属的report，这个值只有在非公用的entry解析的时候用到
	 * @param entry池
	 * @param 别名池
	 * @param 全局条件池
	 * @param 全局的valueFilter定义
	 * @param 全局的mapClass定义
	 * @param 父entry定义池
	 */
    public static void setReportEntry(boolean isPublic, StartElement start, ReportEntry entry, Report report, Map<String, ReportEntry> entryPool, Map<String, Alias> aliasPool, StringBuilder globalConditions, StringBuilder globalValuefilter, List<String> globalMapClass, List<String> parents) {
        if (!isPublic && start.getAttributeByName(new QName("", "id")) != null && start.getAttributeByName(new QName("", "name")) == null) {
            ReportEntry node = entryPool.get(start.getAttributeByName(new QName("", "id")).getValue());
            if (node != null) {
                if (report.getConditions() != null && report.getConditions().length() > 0) {
                    ReportEntry cloneReportEntry = node.clone();
                    cloneReportEntry.setId(report.getId() + "_" + cloneReportEntry.getId());
                    cloneReportEntry.appendConditions(report.getConditions(), aliasPool);
                    report.getReportEntrys().add(cloneReportEntry);
                    entryPool.put(cloneReportEntry.getId(), cloneReportEntry);
                } else {
                    report.getReportEntrys().add(node);
                }
            } else {
                String errorMsg = new StringBuilder().append("reportEntry not exist :").append(start.getAttributeByName(new QName("", "id")).getValue()).toString();
                logger.error(errorMsg);
                throw new java.lang.RuntimeException(errorMsg);
            }
            return;
        }
        if (start.getAttributeByName(new QName("", "name")) != null) {
            entry.setName(start.getAttributeByName(new QName("", "name")).getValue());
        }
        if (start.getAttributeByName(new QName("", "id")) != null) {
            entry.setId(start.getAttributeByName(new QName("", "id")).getValue());
        } else {
            if (!isPublic && report != null) {
                entry.setId(new StringBuilder().append("report:").append(report.getId()).append(entry.getName()).toString());
            }
        }
        if (entry.getId() == null) throw new java.lang.RuntimeException("entry id can't be null...");
        if (start.getAttributeByName(new QName("", "parent")) != null) {
            String parent = start.getAttributeByName(new QName("", "parent")).getValue();
            entry.setParent(parent);
            parents.add(parent);
        }
        if (start.getAttributeByName(new QName("", "key")) != null) {
            entry.setKeys(start.getAttributeByName(new QName("", "key")).getValue().split(","));
            ReportUtil.transformVars(entry.getKeys(), aliasPool);
        }
        if (start.getAttributeByName(new QName("", "value")) != null) {
            String content = start.getAttributeByName(new QName("", "value")).getValue();
            String type = content.substring(0, content.indexOf("("));
            String expression = content.substring(content.indexOf("(") + 1, content.lastIndexOf(")"));
            entry.setValueType(ReportEntryValueType.getType(type));
            entry.setValueExpression(expression, aliasPool);
        }
        if (start.getAttributeByName(new QName("", "mapClass")) != null) {
            entry.setMapClass(start.getAttributeByName(new QName("", "mapClass")).getValue());
        }
        if (start.getAttributeByName(new QName("", "reduceClass")) != null) {
            entry.setReduceClass(start.getAttributeByName(new QName("", "reduceClass")).getValue());
        }
        if (start.getAttributeByName(new QName("", "mapParams")) != null) {
            entry.setMapParams(start.getAttributeByName(new QName("", "mapParams")).getValue());
        }
        if (start.getAttributeByName(new QName("", "reduceParams")) != null) {
            entry.setReduceParams(start.getAttributeByName(new QName("", "reduceParams")).getValue());
        }
        if (start.getAttributeByName(new QName("", "engine")) != null) {
            entry.setEngine(start.getAttributeByName(new QName("", "engine")).getValue());
        }
        if (start.getAttributeByName(new QName("", "lazy")) != null) {
            entry.setLazy(Boolean.valueOf(start.getAttributeByName(new QName("", "lazy")).getValue()));
        }
        StringBuilder conditions = new StringBuilder();
        if (globalConditions != null && globalConditions.length() > 0) {
            conditions.append(new StringBuilder(globalConditions));
        }
        if (report != null && report.getConditions() != null && report.getConditions().length() > 0 && !isPublic) {
            conditions.append(report.getConditions());
        }
        Attribute attr = start.getAttributeByName(new QName("", "condition"));
        if (attr != null) {
            conditions.append(attr.getValue());
        }
        if (conditions.length() > 0) {
            entry.setConditions(conditions.toString(), aliasPool);
        }
        if (start.getAttributeByName(new QName("", "valuefilter")) != null) {
            if (globalValuefilter != null && globalValuefilter.length() > 0) entry.setValuefilter(new StringBuilder(globalValuefilter).append(start.getAttributeByName(new QName("", "valuefilter")).getValue()).toString()); else entry.setValuefilter(start.getAttributeByName(new QName("", "valuefilter")).getValue());
        } else {
            if (globalValuefilter != null && globalValuefilter.length() > 0) entry.setValuefilter(globalValuefilter.toString());
        }
        if (globalMapClass != null && globalMapClass.size() > 0) entry.setGlobalMapClass(globalMapClass);
        if (report != null) report.getReportEntrys().add(entry);
    }

    /**
	 * 合并结果集
	 * 
	 * @param resultPools
	 * @param entryPool
	 * @return
	 */
    public static Map<String, Map<String, Object>> mergeResultPools(Map<String, Map<String, Object>>[] resultPools, Map<String, ReportEntry> entryPool, boolean needMergeLazy) {
        if (resultPools == null || (resultPools != null && resultPools.length == 0)) return null;
        Map<String, Map<String, Object>> result = null;
        result = merge(resultPools, entryPool);
        if (result == null || (result != null && result.size() <= 0)) return result;
        if (needMergeLazy) {
            lazyMerge(result, entryPool);
        }
        return result;
    }

    protected static void lazyMerge(Map<String, Map<String, Object>> result, Map<String, ReportEntry> entryPool) {
        ArrayList<String> entryKeys = new ArrayList<String>();
        entryKeys.addAll(entryPool.keySet());
        Collections.sort(entryKeys);
        for (String entryId : entryKeys) {
            ReportEntry entry = entryPool.get(entryId);
            if (entry.isLazy()) {
                if (result.get(entryId) == null) result.put(entryId, new HashMap<String, Object>());
                if (entry.getBindingStack() != null && entry.getBindingStack().size() > 0) {
                    List<String> bindingStack = entry.getBindingStack();
                    int size = bindingStack.size();
                    String leftEntryId = bindingStack.get(0);
                    Map<String, Object> leftMap = result.get(leftEntryId);
                    if (leftMap == null || (leftMap != null && leftMap.size() <= 0)) continue;
                    Iterator<String> iter = leftMap.keySet().iterator();
                    while (iter.hasNext()) {
                        try {
                            String nodekey = iter.next();
                            Object nodevalue = result.get(leftEntryId).get(nodekey);
                            Object rightvalue;
                            for (int i = 0; i < size - 1; i++) {
                                String rightkey = bindingStack.get(i + 1);
                                if (rightkey.startsWith("sum:")) {
                                    rightkey = rightkey.substring(rightkey.indexOf("sum:") + "sum:".length());
                                    Iterator<Object> rValues = result.get(rightkey).values().iterator();
                                    double sumValue = 0;
                                    while (rValues.hasNext()) {
                                        Object rv = rValues.next();
                                        if (rv != null) {
                                            if (rv instanceof String) sumValue += Double.valueOf((String) rv); else {
                                                sumValue += (Double) rv;
                                            }
                                        }
                                    }
                                    rightvalue = sumValue;
                                } else {
                                    if (rightkey.indexOf(EntryValueOperator.PLUS.toString()) > 0 || rightkey.indexOf(EntryValueOperator.MINUS.toString()) > 0) {
                                        String l;
                                        String r;
                                        if (rightkey.indexOf(EntryValueOperator.PLUS.toString()) > 0) {
                                            l = rightkey.substring(0, rightkey.indexOf(EntryValueOperator.PLUS.toString())).trim();
                                            r = rightkey.substring(rightkey.indexOf(EntryValueOperator.PLUS.toString()) + 1).trim();
                                            if (result.get(l) == null || result.get(r) == null || (result.get(l) != null && result.get(l).get(nodekey) == null) || (result.get(r) != null && result.get(r).get(nodekey) == null)) continue;
                                            rightvalue = Double.valueOf(result.get(l).get(nodekey).toString()) + Double.valueOf(result.get(r).get(nodekey).toString());
                                        } else {
                                            l = rightkey.substring(0, rightkey.indexOf(EntryValueOperator.MINUS.toString())).trim();
                                            r = rightkey.substring(rightkey.indexOf(EntryValueOperator.MINUS.toString()) + 1).trim();
                                            if (result.get(l) == null || result.get(r) == null || (result.get(l) != null && result.get(l).get(nodekey) == null) || (result.get(r) != null && result.get(r).get(nodekey) == null)) continue;
                                            rightvalue = Double.valueOf(result.get(l).get(nodekey).toString()) - Double.valueOf(result.get(r).get(nodekey).toString());
                                        }
                                    } else rightvalue = result.get(rightkey).get(nodekey);
                                }
                                if (rightvalue != null) {
                                    if (entry.getOperatorStack().get(i).equals(EntryValueOperator.PLUS.toString())) {
                                        if (nodevalue instanceof Double || rightvalue instanceof Double) nodevalue = Double.valueOf(nodevalue.toString()) + Double.valueOf(rightvalue.toString()); else nodevalue = (Long) nodevalue + (Long) rightvalue;
                                        continue;
                                    }
                                    if (entry.getOperatorStack().get(i).equals(EntryValueOperator.MINUS.toString())) {
                                        if (nodevalue instanceof Double || rightvalue instanceof Double) nodevalue = Double.valueOf(nodevalue.toString()) - Double.valueOf(rightvalue.toString()); else nodevalue = (Long) nodevalue - (Long) rightvalue;
                                        continue;
                                    }
                                    if (entry.getOperatorStack().get(i).equals(EntryValueOperator.RIDE.toString())) {
                                        if (nodevalue instanceof Double || rightvalue instanceof Double) nodevalue = Double.valueOf(nodevalue.toString()) * Double.valueOf(rightvalue.toString()); else nodevalue = (Long) nodevalue * (Long) rightvalue;
                                        continue;
                                    }
                                    if (entry.getOperatorStack().get(i).equals(EntryValueOperator.DIVIDE.toString())) {
                                        nodevalue = Double.valueOf(nodevalue.toString()) / Double.valueOf(rightvalue.toString());
                                        continue;
                                    }
                                }
                            }
                            result.get(entryId).put(nodekey, nodevalue);
                        } catch (Exception ex) {
                            logger.error(new StringBuilder("entry : ").append(entry.getName()).append(" lazy process error!"), ex);
                            continue;
                        }
                    }
                }
            }
        }
    }

    protected static Map<String, Map<String, Object>> merge(Map<String, Map<String, Object>>[] resultPools, Map<String, ReportEntry> entryPool) {
        Map<String, Map<String, Object>> result = null;
        int _index = 0;
        for (int i = 0; i < resultPools.length; i++) {
            if (resultPools[_index] != null) {
                result = resultPools[_index];
                _index += 1;
                break;
            }
        }
        if (_index == resultPools.length) return result;
        for (int i = _index - 1; i < resultPools.length; i++) {
            Map<String, Map<String, Object>> node = resultPools[i];
            if (node == null || (node != null && node.size() == 0)) continue;
            Iterator<String> iter = node.keySet().iterator();
            while (iter.hasNext()) {
                String entryId = iter.next();
                ReportEntry entry = entryPool.get(entryId);
                if (entry == null || (entry != null && entry.isLazy())) continue;
                if (result.get(entryId) == null) result.put(entryId, new HashMap<String, Object>());
                Map<String, Object> content = resultPools[i].get(entryId);
                Iterator<String> keyIter = content.keySet().iterator();
                while (keyIter.hasNext()) {
                    try {
                        String key = keyIter.next();
                        Object value = content.get(key);
                        if (value == null) continue;
                        switch(entry.getValueType()) {
                            case AVERAGE:
                                if (key.startsWith("a:sum") || key.startsWith("a:count")) continue;
                                String sumkey = new StringBuilder().append("a:sum").append(key).toString();
                                String countkey = new StringBuilder().append("a:count").append(key).toString();
                                if (content.get(sumkey) == null || content.get(countkey) == null) continue;
                                double sum = (Double) content.get(sumkey);
                                double count = (Double) content.get(countkey);
                                for (int j = i + 1; j < resultPools.length; j++) {
                                    if (resultPools[j].get(entryId) != null && resultPools[j].get(entryId).get(key) != null) {
                                        count += (Double) resultPools[j].get(entryId).get(countkey);
                                        sum += (Double) resultPools[j].get(entryId).get(sumkey);
                                        resultPools[j].get(entryId).remove(key);
                                        resultPools[j].get(entryId).remove(countkey);
                                        resultPools[j].get(entryId).remove(sumkey);
                                    }
                                }
                                Double newvalue = sum / count;
                                result.get(entryId).put(key, newvalue);
                                result.get(entryId).put(sumkey, sum);
                                result.get(entryId).put(countkey, count);
                                break;
                            case MIN:
                                double min = (Double) value;
                                for (int j = i + 1; j < resultPools.length; j++) {
                                    if (resultPools[j].get(entryId) != null && resultPools[j].get(entryId).get(key) != null) {
                                        if ((Double) resultPools[j].get(entryId).get(key) < min) min = (Double) resultPools[j].get(entryId).get(key);
                                        resultPools[j].get(entryId).remove(key);
                                    }
                                }
                                result.get(entryId).put(key, min);
                                break;
                            case MAX:
                                double max = (Double) value;
                                for (int j = i + 1; j < resultPools.length; j++) {
                                    if (resultPools[j].get(entryId) != null && resultPools[j].get(entryId).get(key) != null) {
                                        if ((Double) resultPools[j].get(entryId).get(key) > max) max = (Double) resultPools[j].get(entryId).get(key);
                                        resultPools[j].get(entryId).remove(key);
                                    }
                                }
                                result.get(entryId).put(key, max);
                                break;
                            case SUM:
                                sum = (Double) value;
                                for (int j = i + 1; j < resultPools.length; j++) {
                                    if (resultPools[j].get(entryId) != null && resultPools[j].get(entryId).get(key) != null) {
                                        sum += (Double) resultPools[j].get(entryId).get(key);
                                        resultPools[j].get(entryId).remove(key);
                                    }
                                }
                                result.get(entryId).put(key, sum);
                                break;
                            case COUNT:
                                count = (Double) value;
                                for (int j = i + 1; j < resultPools.length; j++) {
                                    if (resultPools[j].get(entryId) != null && resultPools[j].get(entryId).get(key) != null) {
                                        count += (Double) resultPools[j].get(entryId).get(key);
                                        resultPools[j].get(entryId).remove(key);
                                    }
                                }
                                result.get(entryId).put(key, count);
                                break;
                            case PLAIN:
                                Object v = value;
                                for (int j = i + 1; j < resultPools.length; j++) {
                                    if (resultPools[j].get(entryId) != null && resultPools[j].get(entryId).get(key) != null) {
                                        if (v == null) v = resultPools[j].get(entryId).get(key);
                                        resultPools[j].get(entryId).remove(key);
                                    }
                                }
                                result.get(entryId).put(key, v);
                                break;
                        }
                    } catch (Exception ex) {
                        logger.error(ex, ex);
                    }
                }
            }
        }
        return result;
    }

    /**
	 * 获得报表文件存储路径
	 * 
	 * @param 报表名称
	 * @param 输出目录
	 * @param 报表日期
	 * @param 是否需要有后缀
	 * @return
	 */
    public static String getReportFileLocation(String reportname, String targetDir, long date, boolean needsuffix) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        String currentTime = new StringBuilder().append(calendar.get(Calendar.YEAR)).append("-").append(calendar.get(Calendar.MONTH) + 1).append("-").append(calendar.get(Calendar.DAY_OF_MONTH)).toString();
        StringBuilder result = new StringBuilder();
        if (!targetDir.endsWith(File.separator)) result.append(targetDir).append(File.separator).append(currentTime).append(File.separator).toString(); else result.append(targetDir).append(currentTime).append(File.separator).toString();
        if (needsuffix) {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            String stattime = new StringBuilder().append(calendar.get(Calendar.YEAR)).append("-").append(calendar.get(Calendar.MONTH) + 1).append("-").append(calendar.get(Calendar.DAY_OF_MONTH)).toString();
            result.append(reportname).append("_").append(stattime).append(".csv").toString();
        } else {
            result.append(reportname).append(".csv").toString();
        }
        return result.toString();
    }

    /**
	 * @param inputFile
	 * @return
	 */
    public static String createReportHtml(String inputFile, String title, int countNum) {
        StringBuilder result = new StringBuilder();
        java.io.BufferedReader br = null;
        try {
            File file = new File(inputFile);
            if (!file.exists()) throw new java.lang.RuntimeException("chart file not exist : " + inputFile);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "gb2312"));
            String line = null;
            int index = 0;
            result.append("<br/>").append(title).append("<br/>");
            while ((line = br.readLine()) != null) {
                String[] contents = line.split(",");
                if (index == 0) {
                    result.append("<table border=\"1\">");
                }
                result.append("<tr>");
                for (String c : contents) {
                    if (index == 0) result.append("<th>").append(c).append("</th>"); else result.append("<td>").append(c).append("</td>");
                }
                result.append("</tr>");
                index += 1;
                if (countNum > 0 && index > countNum) break;
            }
            result.append("</table>");
        } catch (Exception ex) {
            logger.error(ex, ex);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    logger.error(e, e);
                }
            }
        }
        return result.toString();
    }

    /**
	 * 将对象写入文件
	 * 
	 * @param o
	 * @param file
	 */
    public static void writeObjectToFile(Object o, String file) {
        java.io.ObjectOutputStream out = null;
        try {
            new File(file).createNewFile();
            File f = new File(file);
            out = new ObjectOutputStream(new FileOutputStream(f));
            out.writeObject(o);
        } catch (Exception ex) {
            logger.error(ex, ex);
        } finally {
            if (out != null) try {
                out.close();
            } catch (IOException e) {
                logger.error(e, e);
            }
        }
    }

    /**
	 * 读取对象从文件
	 * 
	 * @param file
	 * @return
	 */
    public static Object readObjectFromFile(String file) {
        Object result = null;
        ObjectInputStream bin = null;
        try {
            File f = new File(file);
            bin = new ObjectInputStream(new FileInputStream(f));
            result = bin.readObject();
        } catch (Exception ex) {
            logger.error(ex, ex);
        } finally {
            if (bin != null) try {
                bin.close();
            } catch (IOException e) {
                logger.error(e, e);
            }
        }
        return result;
    }

    public static Object generateValue(ReportEntry entry, String[] contents) throws ScriptException {
        Object result = null;
        double left = 0;
        if (entry.getBindingStack() != null && entry.getBindingStack().size() > 0) {
            List<String> bindingStack = entry.getBindingStack();
            if (bindingStack.size() > 1) {
                if (bindingStack.get(0).startsWith("#")) left = Double.valueOf(bindingStack.get(0).substring(1)); else {
                    if (Integer.valueOf(bindingStack.get(0)) - 1 >= contents.length) return result;
                    left = Double.valueOf(contents[Integer.valueOf(bindingStack.get(0)) - 1]);
                }
                double right = 0;
                int size = bindingStack.size();
                for (int i = 0; i < size - 1; i++) {
                    if (bindingStack.get(i + 1).startsWith("#")) right = Double.valueOf(bindingStack.get(i + 1).substring(1)); else {
                        if (Integer.valueOf(bindingStack.get(i + 1)) - 1 >= contents.length) return result;
                        right = Double.valueOf(contents[Integer.valueOf(bindingStack.get(i + 1)) - 1]);
                    }
                    if (entry.getOperatorStack().get(i).equals(EntryValueOperator.PLUS.toString())) left += right;
                    if (entry.getOperatorStack().get(i).equals(EntryValueOperator.MINUS.toString())) left -= right;
                    if (entry.getOperatorStack().get(i).equals(EntryValueOperator.RIDE.toString())) left = left * right;
                    if (entry.getOperatorStack().get(i).equals(EntryValueOperator.DIVIDE.toString())) left = left / right;
                }
                result = left;
            } else {
                if (bindingStack.get(0).startsWith("#")) result = Double.valueOf(bindingStack.get(0).substring(1)); else {
                    if (Integer.valueOf(bindingStack.get(0)) - 1 >= contents.length) return result;
                    result = contents[Integer.valueOf(bindingStack.get(0)) - 1];
                }
            }
        }
        return result;
    }
}
