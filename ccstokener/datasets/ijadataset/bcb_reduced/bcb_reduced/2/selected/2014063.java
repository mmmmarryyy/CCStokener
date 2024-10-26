package net.homeip.yann_lab;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Macallan
 */
public class HSTemplate {

    private static final Log log = LogFactory.getLog(HSTemplate.class);

    private static final String hibernStrator = "HibernStrator";

    private static final String hibernStratorVersion = "0.1";

    private File templateFile = null;

    private FileInputStream templateFis = null;

    private InputStream templateIs = null;

    private InputStreamReader templateIsr = null;

    private BufferedReader templateReader = null;

    private String targetDirectory = "";

    private String targetFilename = "";

    private File targetFile = null;

    private FileOutputStream targetFos = null;

    private OutputStreamWriter targetWriter = null;

    private final int readHeadSize = 4 * 1024 * 1024;

    private HSKeywords hsKeywords = new HSKeywords();

    private HSMetaData hsMeta = HSMetaData.getInstance();

    private HSDataTypeVector hsDataTypeVector = HSDataTypeVector.getInstance();

    private HSProperties hsProperties = HSProperties.getInstance();

    private HSUtil hsUtil = HSUtil.getInstance();

    private HashMap stringIntegerFunction = new HashMap();

    private HashMap stringIntIntFunction = new HashMap();

    private HashMap stringVoidFunction = new HashMap();

    private String tableName = "";

    private String actionPackage = "";

    private String actionFormPackage = "";

    private String springPackage = "";

    private String actionFormClassName = "";

    private String simpleName = "";

    private String springBeanName = "";

    private String springClassName = "";

    private String springImplClassName = "";

    private String springInterfaceName = "";

    private String springImplInterfaceName = "";

    private String springIdClassName = "";

    private String springIdInterfaceName = "";

    private String parmDeleteActionClassName = "";

    private String parmInsertActionClassName = "";

    private String parmListActionClassName = "";

    private String parmSelectActionClassName = "";

    private String parmUpdateActionClassName = "";

    private String formDeleteActionClassName = "";

    private String formInsertActionClassName = "";

    private String formListActionClassName = "";

    private String formSelectActionClassName = "";

    private String formUpdateActionClassName = "";

    private String decoratorClassName = "";

    private String decoratorPackage = "";

    public HSTemplate() {
        log.debug("HSTemplate");
        methodExplorer(this);
        methodExplorer(hsMeta);
        methodExplorer(hsProperties);
        methodExplorer(hsUtil);
    }

    private Object invokeMethod(Object object, Object[] args, String methodName) throws Exception {
        Class[] paramTypes = null;
        if (args != null) {
            paramTypes = new Class[args.length];
            for (int i = 0; i < args.length; ++i) {
                paramTypes[i] = args[i].getClass();
            }
        }
        Method m = object.getClass().getMethod(methodName, paramTypes);
        return m.invoke(object, args);
    }

    private void browseMethod(Object object, Method[] m) {
        Class[] functionParams = null;
        for (int i = 0; i < m.length; ++i) {
            int modifier = m[i].getModifiers();
            String modifierString = Modifier.toString(modifier);
            String returnedType = m[i].getReturnType().getName();
            String functionName = m[i].getName();
            functionParams = m[i].getParameterTypes();
            String paramType0 = "void";
            String paramType1 = "void";
            if (returnedType.equals("java.lang.String") && modifierString.contains("public")) {
                if (functionParams.length == 1) {
                    paramType0 = functionParams[0].getName();
                    if (paramType0.equals("java.lang.Integer")) {
                        stringIntegerFunction.put(functionName, object);
                    }
                } else if (functionParams.length == 2) {
                    paramType0 = functionParams[0].getName();
                    paramType1 = functionParams[1].getName();
                    if (paramType0.equals("java.lang.Integer") && paramType1.equals("java.lang.Integer")) {
                        stringIntIntFunction.put(functionName, object);
                    }
                } else {
                    stringVoidFunction.put(functionName, object);
                }
            }
        }
    }

    private void methodExplorer(Object object) {
        Class theClass = object.getClass();
        Method[] methods = theClass.getMethods();
        browseMethod(object, methods);
    }

    private void writeLog(int level, String text) throws IOException {
        String stars = new String("-");
        for (int i = 0; i < level; i++) {
            stars += "-";
        }
        log.debug(level + ":" + stars + " " + text);
    }

    public static void createDirectory(String directory) {
        File file = new File(directory);
        file.mkdirs();
    }

    public static String createDirectory(String directory, String inter) {
        String newDirectory = directory + File.separatorChar + inter;
        createDirectory(newDirectory);
        return newDirectory;
    }

    public static String createDirectory(String directory, String inter, String packageName) {
        String packageDirectory = packageName.replace('.', File.separatorChar);
        String newDirectory = directory + File.separatorChar + inter + File.separatorChar + packageDirectory;
        createDirectory(newDirectory);
        return newDirectory;
    }

    public void setTargetSpringClassName(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory(), springPackage);
        targetFilename = targetDirectory + File.separatorChar + springClassName + hsProperties.getPropJavaType();
    }

    public void setTargetSpringInterfaceName(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory(), springPackage);
        targetFilename = targetDirectory + File.separatorChar + springInterfaceName + hsProperties.getPropJavaType();
    }

    public void setTargetSpringImplClassName(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory(), springPackage);
        targetFilename = targetDirectory + File.separatorChar + springImplClassName + hsProperties.getPropJavaType();
    }

    public void setTargetSpringImplInterfaceName(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory(), springPackage);
        targetFilename = targetDirectory + File.separatorChar + springImplInterfaceName + hsProperties.getPropJavaType();
    }

    public void setTargetSpringIdClassName(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory(), springPackage);
        targetFilename = targetDirectory + File.separatorChar + springIdClassName + hsProperties.getPropJavaType();
    }

    public void setTargetSpringIdInterfaceName(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory(), springPackage);
        targetFilename = targetDirectory + File.separatorChar + springIdInterfaceName + hsProperties.getPropJavaType();
    }

    public void setTargetHibernateMappingFile(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory(), springPackage);
        targetFilename = targetDirectory + File.separatorChar + simpleName + ".hbm.xml";
    }

    public void setTargetActionFormClassName(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory(), actionFormPackage);
        targetFilename = targetDirectory + File.separatorChar + actionFormClassName + hsProperties.getPropJavaType();
    }

    public void setTargetParmDeleteActionClassName(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory(), actionPackage);
        targetFilename = targetDirectory + File.separatorChar + parmDeleteActionClassName + hsProperties.getPropJavaType();
    }

    public void setTargetParmInsertActionClassName(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory(), actionPackage);
        targetFilename = targetDirectory + File.separatorChar + parmInsertActionClassName + hsProperties.getPropJavaType();
    }

    public void setTargetParmUpdateActionClassName(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory(), actionPackage);
        targetFilename = targetDirectory + File.separatorChar + parmUpdateActionClassName + hsProperties.getPropJavaType();
    }

    public void setTargetParmListActionClassName(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory(), actionPackage);
        targetFilename = targetDirectory + File.separatorChar + parmListActionClassName + hsProperties.getPropJavaType();
    }

    public void setTargetParmSelectActionClassName(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory(), actionPackage);
        targetFilename = targetDirectory + File.separatorChar + parmSelectActionClassName + hsProperties.getPropJavaType();
    }

    public void setTargetFormDeleteActionClassName(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory(), actionPackage);
        targetFilename = targetDirectory + File.separatorChar + formDeleteActionClassName + hsProperties.getPropJavaType();
    }

    public void setTargetFormInsertActionClassName(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory(), actionPackage);
        targetFilename = targetDirectory + File.separatorChar + formInsertActionClassName + hsProperties.getPropJavaType();
    }

    public void setTargetFormUpdateActionClassName(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory(), actionPackage);
        targetFilename = targetDirectory + File.separatorChar + formUpdateActionClassName + hsProperties.getPropJavaType();
    }

    public void setTargetFormListActionClassName(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory(), actionPackage);
        targetFilename = targetDirectory + File.separatorChar + formListActionClassName + hsProperties.getPropJavaType();
    }

    public void setTargetFormSelectActionClassName(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory(), actionPackage);
        targetFilename = targetDirectory + File.separatorChar + formSelectActionClassName + hsProperties.getPropJavaType();
    }

    public void setTargetDecoratorClassName(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory(), decoratorPackage);
        targetFilename = targetDirectory + File.separatorChar + decoratorClassName + hsProperties.getPropJavaType();
    }

    public void setTargetComboPage(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropWebDirectory(), simpleName);
        targetFilename = targetDirectory + File.separatorChar + hsProperties.getPropComboPage() + hsProperties.getPropJspType();
    }

    public void setTargetCreatePage(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropWebDirectory(), simpleName);
        targetFilename = targetDirectory + File.separatorChar + hsProperties.getPropCreatePage() + hsProperties.getPropJspType();
    }

    public void setTargetDeletePage(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropWebDirectory(), simpleName);
        targetFilename = targetDirectory + File.separatorChar + hsProperties.getPropDeletePage() + hsProperties.getPropJspType();
    }

    public void setTargetEditPage(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropWebDirectory(), simpleName);
        targetFilename = targetDirectory + File.separatorChar + hsProperties.getPropEditPage() + hsProperties.getPropJspType();
    }

    public void setTargetListPage(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropWebDirectory(), simpleName);
        targetFilename = targetDirectory + File.separatorChar + hsProperties.getPropListPage() + hsProperties.getPropJspType();
    }

    public void setTargetShowPage(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropWebDirectory(), simpleName);
        targetFilename = targetDirectory + File.separatorChar + hsProperties.getPropShowPage() + hsProperties.getPropJspType();
    }

    public void setTargetTablePage(String tableName) {
        setObjectNames(tableName);
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropWebDirectory(), simpleName);
        targetFilename = targetDirectory + File.separatorChar + hsProperties.getPropTablePage() + hsProperties.getPropJspType();
    }

    public void setTargetReadMe() {
        setObjectNames("");
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory(), springPackage);
        targetFilename = targetDirectory + File.separatorChar + "readme.txt";
    }

    public void setTargetApplicationContext() {
        setObjectNames("");
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory());
        targetFilename = targetDirectory + File.separatorChar + "applicationContext.xml";
    }

    public void setTargetApplicationContextProperties() {
        setObjectNames("");
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory());
        targetFilename = targetDirectory + File.separatorChar + "applicationContext.properties";
    }

    public void setTargetStrutsConfig() {
        setObjectNames("");
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropWebDirectory(), hsProperties.getPropWebInf());
        targetFilename = targetDirectory + File.separatorChar + "struts-config.xml";
    }

    public void setTargetTilesDefs() {
        setObjectNames("");
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropWebDirectory(), hsProperties.getPropWebInf());
        targetFilename = targetDirectory + File.separatorChar + "tiles-defs.xml";
    }

    public void setTargetHibernateUtil() {
        setObjectNames("");
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory(), springPackage);
        targetFilename = targetDirectory + File.separatorChar + "HibernateUtil.java";
    }

    public void setTargetApplicationResource() {
        setObjectNames("");
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropSrcDirectory());
        targetFilename = targetDirectory + File.separatorChar + "ApplicationResource.properties";
    }

    public void setTargetHeaderJsp() {
        setObjectNames("");
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropWebDirectory());
        targetFilename = targetDirectory + File.separatorChar + hsProperties.getPropJspHeader();
    }

    public void setTargetMenuJsp() {
        setObjectNames("");
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropWebDirectory());
        targetFilename = targetDirectory + File.separatorChar + hsProperties.getPropJspMenu();
    }

    public void setTargetBodyJsp() {
        setObjectNames("");
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropWebDirectory());
        targetFilename = targetDirectory + File.separatorChar + hsProperties.getPropJspBody();
    }

    public void setTargetFooterJsp() {
        setObjectNames("");
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropWebDirectory());
        targetFilename = targetDirectory + File.separatorChar + hsProperties.getPropJspFooter();
    }

    public void setTargetDefaultJsp() {
        setObjectNames("");
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropWebDirectory());
        targetFilename = targetDirectory + File.separatorChar + hsProperties.getPropJspDefault();
    }

    public void setTargetIndexJsp() {
        setObjectNames("");
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropWebDirectory());
        targetFilename = targetDirectory + File.separatorChar + hsProperties.getPropJspIndex();
    }

    public void setTargetErrorJsp() {
        setObjectNames("");
        targetDirectory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropWebDirectory());
        targetFilename = targetDirectory + File.separatorChar + hsProperties.getPropJspError();
    }

    public void setObjectNames(String tableName) {
        setActionPackage(hsProperties.getPropPackageName() + hsProperties.getPropActionPackage());
        setActionFormPackage(hsProperties.getPropPackageName() + hsProperties.getPropActionFormPackage());
        setSpringPackage(hsProperties.getPropPackageName() + hsProperties.getPropSpringPackage());
        setSimpleName(hsUtil.forgeJavaSimpleNameByString(tableName));
        setSpringBeanName(hsUtil.forgeJavaSimpleNameByString(tableName) + hsProperties.getPropSuffixBean());
        setActionFormClassName(hsUtil.forgeJavaClassNameByString(tableName) + hsProperties.getPropSuffixActionForm());
        setParmDeleteActionClassName(hsUtil.forgeJavaClassNameByString(tableName) + hsProperties.getPropParmDeleteAction());
        setParmInsertActionClassName(hsUtil.forgeJavaClassNameByString(tableName) + hsProperties.getPropParmInsertAction());
        setParmListActionClassName(hsUtil.forgeJavaClassNameByString(tableName) + hsProperties.getPropParmListAction());
        setParmSelectActionClassName(hsUtil.forgeJavaClassNameByString(tableName) + hsProperties.getPropParmSelectAction());
        setParmUpdateActionClassName(hsUtil.forgeJavaClassNameByString(tableName) + hsProperties.getPropParmUpdateAction());
        setFormDeleteActionClassName(hsUtil.forgeJavaClassNameByString(tableName) + hsProperties.getPropFormDeleteAction());
        setFormInsertActionClassName(hsUtil.forgeJavaClassNameByString(tableName) + hsProperties.getPropFormInsertAction());
        setFormListActionClassName(hsUtil.forgeJavaClassNameByString(tableName) + hsProperties.getPropFormListAction());
        setFormSelectActionClassName(hsUtil.forgeJavaClassNameByString(tableName) + hsProperties.getPropFormSelectAction());
        setFormUpdateActionClassName(hsUtil.forgeJavaClassNameByString(tableName) + hsProperties.getPropFormUpdateAction());
        setDecoratorClassName(hsUtil.forgeJavaClassNameByString(tableName) + hsProperties.getPropSuffixDecorator());
        setDecoratorPackage(hsProperties.getPropPackageName() + hsProperties.getPropDecoratorPackage());
        setSpringClassName(hsUtil.forgeJavaClassNameByString(tableName));
        setSpringImplClassName(hsUtil.forgeJavaClassNameByString(tableName) + hsProperties.getPropSuffixImpl());
        setSpringInterfaceName(hsUtil.forgeJavaInterfaceNameByString(tableName));
        setSpringImplInterfaceName(hsUtil.forgeJavaInterfaceNameByString(tableName) + hsProperties.getPropSuffixImpl());
        setSpringIdClassName(hsUtil.forgeJavaClassNameByString(tableName) + hsProperties.getPropSuffixId());
        setSpringIdInterfaceName(hsUtil.forgeJavaInterfaceNameByString(tableName) + hsProperties.getPropSuffixId());
    }

    public void openTarget(String targetFilename) throws FileNotFoundException, UnsupportedEncodingException {
        targetFile = new File(targetFilename);
        targetFos = new FileOutputStream(targetFile);
        targetWriter = new OutputStreamWriter(targetFos, "UTF-8");
    }

    public static String getDateCreation() {
        Date currentDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("yyyy'/'MM'/'dd HH':'mm':'ss");
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.FRANCE);
        return sdf.format(currentDate);
    }

    private String makeEnd(String keyword) {
        keyword = "end" + hsUtil.upperFirstLetter(keyword);
        return keyword;
    }

    private String makeElse(String keyword) {
        keyword = "else" + hsUtil.upperFirstLetter(keyword);
        return keyword;
    }

    private String treatTemplateReplace(String line, String keyword, String replacement) {
        if (line != null && keyword != null) {
            if (replacement == null) {
                replacement = "$<" + keyword + ">";
            }
            line = line.replace("$[" + keyword + "]", replacement);
        }
        return line;
    }

    private String treatTemplateReplace(String line, String keyword) {
        if (line != null && keyword != null) {
            line = treatTemplateReplace(line, keyword, null);
        }
        return line;
    }

    private String treatTemplateKeyword(String line, String keyword, int iTable, int iColumn, String endCondition, boolean elseTrue, String elseCondition, boolean insideElse, boolean bValid, int level) throws IOException {
        writeLog(level, "treatTemplateKeyword : '" + keyword + "'");
        String replacement = null;
        if (stringIntIntFunction.containsKey(keyword) && iTable != -1 && iColumn != -1) {
            Object object = stringIntIntFunction.get(keyword);
            try {
                replacement = (String) invokeMethod(object, new Object[] { iTable, iColumn }, keyword);
            } catch (Exception ex) {
                String helpText = "Exception (Integer,Integer) - Table : " + iTable + " - Column :" + iColumn + "- Keyword :" + keyword + " - Class :'" + object.getClass().getSimpleName() + "'";
                log.error(helpText, ex);
            }
        } else if (stringIntegerFunction.containsKey(keyword) && iColumn != -1) {
            Object object = stringIntegerFunction.get(keyword);
            try {
                replacement = (String) invokeMethod(object, new Object[] { iColumn }, keyword);
            } catch (Exception ex) {
                String helpText = "Exception (Integer) - Column : " + iColumn + " - Keyword : " + keyword + " - Class : '" + object.getClass().getSimpleName() + "'";
                log.error(helpText, ex);
            }
        } else if (stringIntegerFunction.containsKey(keyword) && iTable != -1) {
            Object object = stringIntegerFunction.get(keyword);
            try {
                replacement = (String) invokeMethod(object, new Object[] { iTable }, keyword);
            } catch (Exception ex) {
                String helpText = "Exception (Integer) - Table : " + iTable + " - Keyword : " + keyword + " - Class : '" + object.getClass().getSimpleName() + "'";
                log.error(helpText, ex);
            }
        } else if (stringVoidFunction.containsKey(keyword)) {
            Object object = stringVoidFunction.get(keyword);
            try {
                replacement = (String) invokeMethod(object, null, keyword);
            } catch (Exception ex) {
                String helpText = "Exception (void) - Keyword : " + keyword + " - Class : '" + object.getClass().getSimpleName() + "'";
                log.error(helpText, ex);
            }
        }
        line = treatTemplateReplace(line, keyword, replacement);
        return line;
    }

    private String treatTemplateIf(String line, String keyword, int iTable, int iColumn, String endCondition, boolean elseTrue, String elseCondition, boolean insideElse, boolean bValid, int level) throws IOException {
        writeLog(level, "treatTemplateIf : '" + keyword + "'");
        if (keyword.equals(hsKeywords.ifNotId)) {
            if (!hsMeta.getIsInsidePkArray(iColumn) || hsMeta.getPkColumnNameArrayLength() <= 1) {
                writeLog(level, "ifNotId is True");
                line = treatTemplate(line, keyword, iTable, iColumn, endCondition, false, elseCondition, false, true, level + 1);
            } else {
                writeLog(level, "ifNotId is False");
                line = treatTemplate(line, keyword, iTable, iColumn, endCondition, true, elseCondition, false, false, level + 1);
            }
        } else if (keyword.equals(hsKeywords.ifNotKey)) {
            if (!hsMeta.getIsInsidePkArray(iColumn)) {
                writeLog(level, "ifNotKey is True");
                line = treatTemplate(line, keyword, iTable, iColumn, endCondition, false, elseCondition, false, true, level + 1);
            } else {
                writeLog(level, "ifNotKey is False:");
                line = treatTemplate(line, keyword, iTable, iColumn, endCondition, true, elseCondition, false, false, level + 1);
            }
        } else if (keyword.equals(hsKeywords.ifIsKey)) {
            if (hsMeta.getIsInsidePkArray(iColumn)) {
                writeLog(level, "ifIsId is True");
                line = treatTemplate(line, keyword, iTable, iColumn, endCondition, false, elseCondition, false, true, level + 1);
            } else {
                writeLog(level, "ifIsId is False");
                line = treatTemplate(line, keyword, iTable, iColumn, endCondition, true, elseCondition, false, false, level + 1);
            }
        } else if (keyword.equals(hsKeywords.ifIdExists)) {
            if (hsMeta.getPkColumnNameArrayLength() > 1) {
                writeLog(level, "ifIdExists is True - Index : " + hsMeta.getPkColumnNameArrayLength());
                line = treatTemplate(line, keyword, iTable, iColumn, endCondition, false, elseCondition, false, true, level + 1);
            } else {
                writeLog(level, "ifIdExists is False - Index : " + hsMeta.getPkColumnNameArrayLength());
                line = treatTemplate(line, keyword, iTable, iColumn, endCondition, true, elseCondition, false, false, level + 1);
            }
        } else if (keyword.equals(hsKeywords.ifTextArea)) {
            if (hsMeta.getColumnSizeArray(iColumn) >= hsProperties.getPropAreaStartSize()) {
                writeLog(level, "ifTextArea is True - Index : " + iColumn);
                line = treatTemplate(line, keyword, iTable, iColumn, endCondition, false, elseCondition, false, true, level + 1);
            } else {
                writeLog(level, "ifTextArea is False - Index : " + iColumn);
                line = treatTemplate(line, keyword, iTable, iColumn, endCondition, true, elseCondition, false, false, level + 1);
            }
        }
        line = treatTemplateReplace(line, keyword);
        return line;
    }

    private String treatTemplateLoop(String line, String keyword, int iTable, int iColumn, String endCondition, boolean elseTrue, String elseCondition, boolean insideElse, boolean bValid, int level) throws IOException {
        writeLog(level, "treatTemplateLoop : '" + keyword + "'");
        if (!templateReader.markSupported()) {
            log.error("markSupported Fails");
            throw new RuntimeException("markSupported Fails");
        }
        templateReader.mark(readHeadSize);
        if (keyword.equals(hsKeywords.loopColumns)) {
            for (int ic = 0; ic < hsMeta.getColumnNameArrayLength(); ic++) {
                templateReader.reset();
                line = treatTemplate(line, keyword, iTable, ic, endCondition, false, elseCondition, false, true, level + 1);
            }
        } else if (keyword.equals(hsKeywords.loopNoIdColumns)) {
            for (int in = 0; in < hsMeta.getColumnNameArrayLength(); in++) {
                templateReader.reset();
                line = treatTemplate(line, keyword, iTable, in, endCondition, false, elseCondition, false, true, level + 1);
            }
        } else if (keyword.equals(hsKeywords.loopPrimaryColumns)) {
            for (int ip = 0; ip < hsMeta.getPkColumnNameArrayLength(); ip++) {
                templateReader.reset();
                line = treatTemplate(line, keyword, iTable, ip, endCondition, false, elseCondition, false, true, level + 1);
            }
        } else if (keyword.equals(hsKeywords.loopTables)) {
            for (int it = 0; it < hsMeta.numberOfTables(); it++) {
                templateReader.reset();
                hsMeta.setCurrentTable(it);
                setObjectNames(hsMeta.getTableName(it));
                line = treatTemplate(line, keyword, it, -1, endCondition, false, elseCondition, false, true, level + 1);
            }
        } else {
            line = treatTemplateKeyword(line, keyword, -1, -1, endCondition, false, elseCondition, false, true, level + 1);
        }
        return line;
    }

    private String treatTemplate(String line, String mainKeyword, int iTable, int iColumn, String endCondition, boolean elseTrue, String elseCondition, boolean insideElse, boolean bValid, int level) throws IOException {
        writeLog(level, "treatTemplate start : '" + mainKeyword + "' - Else : '" + elseCondition + "' - End : '" + endCondition + "'");
        while (templateReader.ready()) {
            writeLog(level, "States - Inside Else : '" + insideElse + "' - Else Is True : '" + elseTrue + "' - Valid : '" + bValid + "'");
            line = templateReader.readLine();
            int iResult = 0;
            do {
                iResult = -1;
                String keyword = hsKeywords.compareToKeywords(line);
                if (keyword.length() > 0) {
                    iResult = 1;
                    if (!elseCondition.equals("") && keyword.equals(elseCondition)) {
                        writeLog(level, "treatTemplate else : " + elseCondition);
                        line = treatTemplateReplace(line, keyword, "");
                        insideElse = true;
                    }
                    if (!endCondition.equals("") && keyword.equals(endCondition)) {
                        writeLog(level, "treatTemplate end : " + endCondition);
                        line = treatTemplateReplace(line, keyword, "");
                        return line;
                    }
                    if (insideElse) {
                        if (elseTrue) {
                            bValid = true;
                        } else {
                            bValid = false;
                        }
                    } else {
                        if (elseTrue) {
                            bValid = false;
                        } else {
                            bValid = true;
                        }
                    }
                    if (keyword.startsWith("loop")) {
                        if (bValid) {
                            writeLog(level, "treatTemplate loop - Start of Treatment : " + keyword);
                            line = treatTemplateLoop(line, keyword, iTable, iColumn, makeEnd(keyword), false, makeElse(keyword), false, true, level + 1);
                            line = treatTemplateReplace(line, keyword, "");
                            writeLog(level, "treatTemplate loop - End of Treatment : " + keyword);
                        } else {
                            line = treatTemplateKeyword(line, keyword, iTable, iColumn, makeEnd(keyword), elseTrue, makeElse(keyword), insideElse, true, level + 1);
                        }
                    } else if (keyword.startsWith("if")) {
                        if (bValid) {
                            writeLog(level, "treatTemplate if - Start of Treatment : " + keyword);
                            line = treatTemplateIf(line, keyword, iTable, iColumn, makeEnd(keyword), false, makeElse(keyword), false, true, level + 1);
                            line = treatTemplateReplace(line, keyword, "");
                            writeLog(level, "treatTemplate if - End of Treatment : " + keyword);
                            insideElse = false;
                            elseTrue = false;
                        } else {
                            line = treatTemplateKeyword(line, keyword, iTable, iColumn, makeEnd(keyword), elseTrue, makeElse(keyword), insideElse, true, level + 1);
                        }
                    } else {
                        line = treatTemplateKeyword(line, keyword, iTable, iColumn, makeEnd(keyword), elseTrue, makeElse(keyword), insideElse, true, level + 1);
                    }
                }
            } while (iResult > 0);
            if (bValid) {
                targetWriter.write(line + "\n");
            } else {
                log.debug("Skip Line : '" + line + "'");
            }
        }
        return line;
    }

    public void treatTemplate(String tableName) throws IOException {
        writeLog(0, "Table Name : " + tableName);
        this.setTableName(tableName);
        setObjectNames(tableName);
        treatTemplate("", "", -1, -1, "", false, "", false, true, 0);
    }

    public void openTemplate(String templateFilename) throws UnsupportedEncodingException, IOException {
        writeLog(0, "Template Filename : " + templateFilename);
        try {
            templateFile = new File(templateFilename);
            templateFis = new FileInputStream(templateFile);
            templateIsr = new InputStreamReader(templateFis, "UTF-8");
        } catch (FileNotFoundException ex) {
            URL url = ClassLoader.getSystemResource(templateFilename);
            templateIs = url.openStream();
            templateIsr = new InputStreamReader(templateIs, "UTF-8");
        }
        templateReader = new BufferedReader(templateIsr, readHeadSize);
        log.info("Generation of: '" + targetFilename + "'");
        targetFile = new File(targetFilename);
        targetFos = new FileOutputStream(targetFile);
        targetWriter = new OutputStreamWriter(targetFos, "UTF-8");
    }

    private void closeTarget() throws IOException {
        if (targetWriter != null) {
            targetWriter.close();
            targetWriter = null;
        }
        if (targetFos != null) {
            targetFos.close();
            targetFos = null;
        }
    }

    private void closeTemplate() throws IOException {
        if (templateReader != null) {
            templateReader.close();
            templateReader = null;
        }
        if (templateIsr != null) {
            templateIsr.close();
            templateIsr = null;
        }
        if (templateIs != null) {
            templateIs.close();
            templateIs = null;
        }
        if (templateFis != null) {
            templateFis.close();
            templateFis = null;
        }
    }

    public void close() throws IOException {
        closeTarget();
        closeTemplate();
    }

    public void copyFile(String inputFilename, String outputFilename) throws UnsupportedEncodingException, IOException {
        log.info("Copying '" + inputFilename + "' to '" + outputFilename + "'");
        File inputFile = null;
        InputStream inputStream = null;
        File outputFile = null;
        OutputStream outputStream = null;
        try {
            inputFile = new File(inputFilename);
            inputStream = new FileInputStream(inputFile);
        } catch (FileNotFoundException ex) {
            URL url = ClassLoader.getSystemResource(inputFilename);
            inputStream = url.openStream();
        }
        outputFile = new File(outputFilename);
        outputStream = new FileOutputStream(outputFile);
        int iRead = 0;
        do {
            byte[] bytesRead = new byte[256];
            iRead = inputStream.read(bytesRead);
            if (iRead > 0) {
                outputStream.write(bytesRead, 0, iRead);
            }
        } while (iRead > 0);
        outputStream.close();
        inputStream.close();
    }

    public void copyFileToWeb(String filename) {
        String directory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropWebDirectory());
        try {
            copyFile(filename, directory + File.separatorChar + filename);
        } catch (Exception ex) {
            log.warn("A problem occurs with '" + filename + "'", ex);
        }
    }

    public void copyFileToCss(String filename) {
        String directory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropWebDirectory(), "css");
        try {
            copyFile("css/" + filename, directory + File.separatorChar + filename);
        } catch (Exception ex) {
            log.warn("A problem occurs with '" + filename + "'", ex);
        }
    }

    public void copyFileToImg(String filename) {
        String directory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropWebDirectory(), "img");
        try {
            copyFile("img/" + filename, directory + File.separatorChar + filename);
        } catch (Exception ex) {
            log.warn("A problem occurs with '" + filename + "'", ex);
        }
    }

    public void copyFileToImages(String filename) {
        String directory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropWebDirectory(), "images");
        try {
            copyFile("images/" + filename, directory + File.separatorChar + filename);
        } catch (Exception ex) {
            log.warn("A problem occurs with '" + filename + "'", ex);
        }
    }

    public void copyFileToWebInf(String filename) {
        String directory = createDirectory(hsProperties.getPropRootDirectory(), hsProperties.getPropWebDirectory(), hsProperties.getPropWebInf());
        try {
            copyFile(filename, directory + File.separatorChar + filename);
        } catch (Exception ex) {
            log.warn("A problem occurs with '" + filename + "'", ex);
        }
    }

    public String getActionPackage() {
        return actionPackage;
    }

    public void setActionPackage(String actionPackage) {
        this.actionPackage = actionPackage;
    }

    public String getActionFormPackage() {
        return actionFormPackage;
    }

    public void setActionFormPackage(String actionFormPackage) {
        this.actionFormPackage = actionFormPackage;
    }

    public String getSpringPackage() {
        return springPackage;
    }

    public String getSpringPackagePath() {
        String springPackagePath = springPackage;
        while (springPackagePath.contains(".")) {
            springPackagePath = springPackagePath.replace('.', '/');
        }
        return springPackagePath;
    }

    public void setSpringPackage(String springPackage) {
        this.springPackage = springPackage;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public void setSimpleName(String simpleName) {
        this.simpleName = simpleName;
    }

    public String getSpringBeanName() {
        return springBeanName;
    }

    public void setSpringBeanName(String springBeanName) {
        this.springBeanName = springBeanName;
    }

    public String getSpringClassName() {
        return springClassName;
    }

    public void setSpringClassName(String SpringClassName) {
        this.springClassName = SpringClassName;
    }

    public String getSpringImplClassName() {
        return springImplClassName;
    }

    public void setSpringImplClassName(String springImplClassName) {
        this.springImplClassName = springImplClassName;
    }

    public String getSpringInterfaceName() {
        return springInterfaceName;
    }

    public void setSpringInterfaceName(String springInterfaceName) {
        this.springInterfaceName = springInterfaceName;
    }

    public String getSpringImplInterfaceName() {
        return springImplInterfaceName;
    }

    public void setSpringImplInterfaceName(String springImplInterfaceName) {
        this.springImplInterfaceName = springImplInterfaceName;
    }

    public String getSpringIdClassName() {
        return springIdClassName;
    }

    public void setSpringIdClassName(String springIdClassName) {
        this.springIdClassName = springIdClassName;
    }

    public String getSpringIdInterfaceName() {
        return springIdInterfaceName;
    }

    public void setSpringIdInterfaceName(String springIdInterfaceName) {
        this.springIdInterfaceName = springIdInterfaceName;
    }

    public static String getHibernStrator() {
        return hibernStrator;
    }

    public String getActionFormClassName() {
        return actionFormClassName;
    }

    public void setActionFormClassName(String actionFormClassName) {
        this.actionFormClassName = actionFormClassName;
    }

    public String getParmDeleteActionClassName() {
        return parmDeleteActionClassName;
    }

    public void setParmDeleteActionClassName(String parmDeleteActionClassName) {
        this.parmDeleteActionClassName = parmDeleteActionClassName;
    }

    public String getParmInsertActionClassName() {
        return parmInsertActionClassName;
    }

    public void setParmInsertActionClassName(String parmInsertActionClassName) {
        this.parmInsertActionClassName = parmInsertActionClassName;
    }

    public String getParmListActionClassName() {
        return parmListActionClassName;
    }

    public void setParmListActionClassName(String parmListActionClassName) {
        this.parmListActionClassName = parmListActionClassName;
    }

    public String getParmSelectActionClassName() {
        return parmSelectActionClassName;
    }

    public void setParmSelectActionClassName(String parmSelectActionClassName) {
        this.parmSelectActionClassName = parmSelectActionClassName;
    }

    public String getParmUpdateActionClassName() {
        return parmUpdateActionClassName;
    }

    public void setParmUpdateActionClassName(String parmUpdateActionClassName) {
        this.parmUpdateActionClassName = parmUpdateActionClassName;
    }

    public String getFormDeleteActionClassName() {
        return formDeleteActionClassName;
    }

    public void setFormDeleteActionClassName(String formDeleteActionClassName) {
        this.formDeleteActionClassName = formDeleteActionClassName;
    }

    public String getFormInsertActionClassName() {
        return formInsertActionClassName;
    }

    public void setFormInsertActionClassName(String formInsertActionClassName) {
        this.formInsertActionClassName = formInsertActionClassName;
    }

    public String getFormListActionClassName() {
        return formListActionClassName;
    }

    public void setFormListActionClassName(String formListActionClassName) {
        this.formListActionClassName = formListActionClassName;
    }

    public String getFormSelectActionClassName() {
        return formSelectActionClassName;
    }

    public String getFormUpdateActionClassName() {
        return formUpdateActionClassName;
    }

    public void setFormSelectActionClassName(String formSelectActionClassName) {
        this.formSelectActionClassName = formSelectActionClassName;
    }

    public void setFormUpdateActionClassName(String formUpdateActionClassName) {
        this.formUpdateActionClassName = formUpdateActionClassName;
    }

    public String getDecoratorClassName() {
        return decoratorClassName;
    }

    public void setDecoratorClassName(String decoratorClassName) {
        this.decoratorClassName = decoratorClassName;
    }

    public String getDecoratorPackage() {
        return decoratorPackage;
    }

    public void setDecoratorPackage(String decoratorPackage) {
        this.decoratorPackage = decoratorPackage;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public static String getHibernStratorVersion() {
        return hibernStratorVersion;
    }
}
