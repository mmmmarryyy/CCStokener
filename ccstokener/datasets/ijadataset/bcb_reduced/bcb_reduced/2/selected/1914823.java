package org.mobicents.ssf.flow.config.spring.annotation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import org.mobicents.ssf.flow.annotation.Action;
import org.mobicents.ssf.flow.annotation.Evaluate;
import org.mobicents.ssf.flow.configuration.Attribute;
import org.mobicents.ssf.flow.configuration.FlowRoot;
import org.mobicents.ssf.flow.configuration.State;
import org.mobicents.ssf.flow.configuration.Transition;
import org.mobicents.ssf.flow.configuration.TransitionSet;
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationTypeDoc;
import com.sun.javadoc.AnnotationValue;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.Doclet;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.AnnotationDesc.ElementValuePair;

/**
 * <p>
 * Docletを使用して、アノテーションからSIPフロー用の設定ファイルを出力する
 * ためのツールです。
 * </p>
 * <p>
 * 以下のファイルが出力されます。
 * </p>
 * 
 * <p>
 * 設定ファイル：
 * <dl>
 * <dt>sipflow-common.xml</dt>
 * <dd>フロー共通の設定ファイル</dd>
 * 
 * <dt>spring-＜フローID＞.xml</dt>
 * <dd>SpringFrameworkで読み込むための各フローの設定ファイル</dd>
 *
 * <dt>＜フローID＞.xml</dt>
 * <dd>各フローの定義ファイル</dd>
 *
 * <dt>＜フローID＞-＜状態名＞.xml
 * <li>各フローに対応する状態の定義ファイル
 * </dl>
 * </p>
 * 
 * 設定ファイルは、デフォルトのディレクトリ（javadoc実行時ディレクトリ）に出力されます。
 * また、これらのファイルはjavadoc実行時に常に上書きされます。
 * 
 * </p>
 * ソース：
 * <dl>
 * <dt>package-info.java</dt>
 * <dd>遷移先として指定された状態のパッケージに存在しなかった場合に生成されるパッケージ情報。</dd>
 * <dt>＜アクションクラス＞Action.java</dt>
 * <dd>遷移先として指定されたActionクラス。</dd>
 * </p>
 * 
 * ソースファイルは"-outputSrc"を指定した場合にのみ出力されます。
 * また、これらのファイルはすでに存在する場合は生成されません。
 * 
 * @author nisihara
 *
 */
public class SipFlowAnnotationDoclet {

    private ResourceBundle bundle = ResourceBundle.getBundle("org.mobicents.ssf.flow.resources.evaluate-classes");

    private ResourceBundle extendedBundle = null;

    private ResourceBundle extendableProperty = ResourceBundle.getBundle("org.mobicents.ssf.flow.resources.extendable-property");

    private static final String listenerBaseName = "sFlowListener";

    private String LINE_SEP = System.getProperty("line.separator");

    private int INDENT_SPACE = 2;

    private static final String WHITE_SPACE = " ";

    public static final String DEFAULT_ACTION_METHOD = "execute";

    public static final String DEFAULT_EVALUATE_METHOD = "evaluate";

    private static final String ACTION_CLASS_TEMPLATE = "org/mobicents/ssf/flow/resources/action-template.txt";

    private static final String PACKAGE_INFO_TEMPLATE = "org/mobicents/ssf/flow/resources/package-info-template.txt";

    private static final String SPRING_FLOW_TEMPLATE = "org/mobicents/ssf/flow/resources/spring-flow-template";

    private static final String SIPFLOW_SERVLET_HEADER = "org/mobicents/ssf/flow/resources/sipflow-servlet-header.txt";

    private static final String SIPFLOW_SERVLET_FOOTER = "org/mobicents/ssf/flow/resources/sipflow-servlet-footer.txt";

    public static LanguageVersion languageVersion() {
        return LanguageVersion.JAVA_1_5;
    }

    public static boolean start(RootDoc root) {
        String outputDir = readOutputDir(root.options());
        String extendedBundleName = readExtendedBundle(root.options());
        ResourceBundle extendedBundle = null;
        if (extendedBundleName != null) {
            extendedBundle = ResourceBundle.getBundle(extendedBundleName);
        }
        root.printNotice("outputDir=" + outputDir);
        SipFlowAnnotationDoclet doclet = new SipFlowAnnotationDoclet(root, outputDir, extendedBundle);
        doclet.writeXml();
        root.printNotice("Done all.");
        return true;
    }

    private static String readOutputDir(String[][] options) {
        String value = null;
        for (int i = 0; i < options.length; i++) {
            String[] opt = options[i];
            if (opt[0].equals("-outputSrc")) {
                value = opt[1];
                break;
            }
            if (opt[0].equals("-extendedBundle")) {
                value = opt[1];
                break;
            }
        }
        return value;
    }

    private static String readExtendedBundle(String[][] options) {
        String value = null;
        for (int i = 0; i < options.length; i++) {
            String[] opt = options[i];
            if (opt[0].equals("-extendedBundle")) {
                value = opt[1];
                break;
            }
        }
        return value;
    }

    public static int optionLength(String option) {
        System.out.println("optionLength:option=" + option);
        if (option.equals("-outputSrc")) {
            return 2;
        }
        if (option.equals("-extendedBundle")) {
            return 2;
        }
        return Doclet.optionLength(option);
    }

    public static boolean validOptions(String options[][], DocErrorReporter reporter) {
        boolean foundOutputOption = false;
        boolean extendedBundleOption = false;
        for (int i = 0; i < options.length; i++) {
            String[] opt = options[i];
            reporter.printNotice("opt=" + opt[0]);
            if (opt[0].equals("-outputSrc")) {
                if (foundOutputOption) {
                    reporter.printError("Only one -outputSrc option allowed.");
                } else {
                    foundOutputOption = true;
                }
            }
            if (opt[0].equals("-extendedBundle")) {
                if (extendedBundleOption) {
                    reporter.printError("Only one -extendedBundle option allowed.");
                } else {
                    extendedBundleOption = true;
                }
            }
        }
        return Doclet.validOptions(options, reporter);
    }

    private String output;

    private RootDoc root;

    private SipFlowAnnotationDoclet(RootDoc root, String output, ResourceBundle bundle) {
        this.root = root;
        this.output = output;
        this.extendedBundle = bundle;
    }

    private void writeXml() {
        PackageDoc[] packageDocs = root.specifiedPackages();
        root.printNotice("packageDocs.length:" + packageDocs.length);
        List<FlowContainer> flowList = new ArrayList<FlowContainer>();
        List<StateContainer> stateList = new ArrayList<StateContainer>();
        for (PackageDoc pack : packageDocs) {
            root.printNotice("reading package:" + pack.name());
            AnnotationDesc[] annos = pack.annotations();
            StateContainer state = null;
            Map<String, String> attrMap = null;
            for (AnnotationDesc annoDesc : annos) {
                AnnotationTypeDoc typeDoc = annoDesc.annotationType();
                ElementValuePair[] valuePairs = annoDesc.elementValues();
                String name = typeDoc.qualifiedTypeName();
                root.printNotice("reading annotation:" + name);
                if (name.equals(FlowRoot.class.getName())) {
                    FlowContainer flow = createFlowRoot(pack, valuePairs);
                    flowList.add(flow);
                } else if (name.equals(State.class.getName())) {
                    state = createState(pack, valuePairs);
                } else if (name.equals(Attribute.class.getName())) {
                    if (attrMap == null) {
                        attrMap = new HashMap<String, String>();
                    }
                    setupAttribute(attrMap, valuePairs);
                } else {
                    root.printWarning("Annotation not found.[" + pack.name() + "]");
                }
                if (state != null) {
                    if (attrMap != null) {
                        for (Map.Entry<String, String> entry : attrMap.entrySet()) {
                            String key = entry.getKey();
                            String value = entry.getValue();
                            state.setAttribute(key, value);
                        }
                    }
                    stateList.add(state);
                }
            }
        }
        this.root.printNotice("start to construct flow.");
        for (StateContainer state : stateList) {
            String parentPack = state.getParentPackage();
            boolean isAdded = false;
            for (FlowContainer flow : flowList) {
                this.root.printNotice("flow=" + flow.getId());
                this.root.printNotice("flow.pack=" + flow.getPackageName());
                this.root.printNotice("state.pack=" + state.getPackageName());
                if (flow.getPackageName().equals(parentPack)) {
                    flow.addState(state);
                    isAdded = true;
                    break;
                }
            }
            if (!isAdded) {
                this.root.printWarning("Cannot find the Flow for State.[state=" + state.getId() + "]");
            }
        }
        this.root.printNotice("done.");
        for (FlowContainer flow : flowList) {
            StringBuilder sb = new StringBuilder();
            appendLine(0, sb, "<flow id=\"" + flow.getId() + "\" start-state=\"" + flow.getStartStateId() + "\">");
            for (StateContainer state : flow.stateList) {
                String stateFileName = flow.getId() + "-" + state.getId() + ".xml";
                appendLine(1, sb, "<state-location path=\"" + stateFileName + "\"/>");
                writeState(stateFileName, state, flow);
            }
            StateContainer sc = flow.getState(flow.getStartStateId());
            this.root.printNotice("Start state=" + sc);
            if (output != null && sc == null) {
                try {
                    createStatePackage(flow, flow.getStartStateId());
                } catch (IOException e) {
                    this.root.printError(e.getMessage());
                }
            }
            appendLine(0, sb, "</flow>");
            System.out.println("flow:");
            System.out.print(sb.toString());
            File file = null;
            file = new File(flow.getId() + ".xml");
            writeStringToFile(sb.toString(), file, true);
        }
        StringBuilder sb = new StringBuilder();
        StringBuilder importSb = new StringBuilder();
        appendLine(1, sb, "<bean id=\"sipHandlerFactory\" class=\"org.mobicents.ssf.flow.servlet.handler.DynamicSipFlowHandlerFactory\"/>");
        for (FlowContainer flow : flowList) {
            try {
                String filename = "spring-" + flow.getId() + ".xml";
                this.root.printNotice("Setting up flow." + filename);
                StringBuilder sbflow = new StringBuilder();
                String template = this.readTemplate(SPRING_FLOW_TEMPLATE + "1.txt");
                template = template.replaceAll("%FLOW_NAME%", flow.getId());
                template = template.replaceAll("%FLOW_PACKAGE%", flow.getPackageName());
                sbflow.append(template);
                Enumeration<String> e = extendableProperty.getKeys();
                int count = 0;
                while (e.hasMoreElements()) {
                    e.nextElement();
                    template = this.readTemplate(SPRING_FLOW_TEMPLATE + "2.txt");
                    template = template.replaceAll("%LISTENER_REF%", listenerBaseName + count++);
                    sbflow.append(template);
                }
                template = this.readTemplate(SPRING_FLOW_TEMPLATE + "3.txt");
                template = template.replaceAll("%FLOW_NAME%", flow.getId());
                sbflow.append(template);
                template = this.readTemplate(SPRING_FLOW_TEMPLATE + "5.txt");
                sbflow.append(template);
                e = extendableProperty.getKeys();
                count = 0;
                while (e.hasMoreElements()) {
                    String key = e.nextElement();
                    String value = extendableProperty.getString(key);
                    sbflow.append("  <bean name=\"" + listenerBaseName + count++ + "\" class=\"" + value + "\"/>");
                }
                template = this.readTemplate(SPRING_FLOW_TEMPLATE + "6.txt");
                sbflow.append(template);
                File file = new File(filename);
                this.writeStringToFile(sbflow.toString(), file, false);
                appendLine(1, importSb, "<import resource=\"" + filename + "\"/>" + this.LINE_SEP);
            } catch (IOException e) {
                this.root.printError(e.getMessage());
            }
        }
        try {
            String header = this.readTemplate(SIPFLOW_SERVLET_HEADER);
            String footer = this.readTemplate(SIPFLOW_SERVLET_FOOTER);
            File file = new File("sipflow-common.xml");
            this.writeStringToFile(header + this.LINE_SEP + sb.toString() + importSb.toString() + footer, file, true);
        } catch (IOException e) {
            this.root.printError(e.getMessage());
        }
    }

    private void writeState(String filename, StateContainer state, FlowContainer flow) {
        StringBuilder sb = new StringBuilder();
        try {
            appendLine(sb, "<state>");
            appendLine(1, sb, "<evaluate-state id=\"" + state.getId() + "\">");
            appendLine(2, sb, "<name>" + state.getId() + "</name>");
            for (String key : state.keySet()) {
                String value = state.getAttribute(key);
                appendLine(2, sb, "<property name=\"" + key + "\" " + "value=\"" + value + "\"/>");
            }
            for (ClassDoc evaluateClassDoc : state.evaluateList) {
                appendLine(2, sb, "<evaluate type=\"pojo\" id=\"" + evaluateClassDoc.qualifiedName() + "\">");
                appendLine(3, sb, "<property name=\"objectClass\" value=\"" + evaluateClassDoc.qualifiedName() + "\"/>");
                appendLine(3, sb, "<property name=\"method\" value=\"" + this.getEvaluateMethod(evaluateClassDoc) + "\"/>");
                String tset = this.getTransitionSet(evaluateClassDoc);
                if (tset != null) {
                    appendLine(3, sb, "<property name=\"transitions\" value=\"" + tset + "\"/>");
                }
                appendLine(3, sb, "<property name=\"targetEvent\" value=\"" + getEvaluateTargetEvent(evaluateClassDoc.name()) + "\"/>");
                appendLine(2, sb, "</evaluate>");
            }
            if (state.getEntryAction() != null) {
                ClassDoc actionClass = state.getEntryAction();
                appendAction(sb, actionClass, "entry-action-list");
            }
            if (state.getExitAction() != null) {
                ClassDoc actionClass = state.getExitAction();
                appendAction(sb, actionClass, "exit-action-list", null, true);
            }
            for (String key : state.keySet()) {
                Enumeration<String> e = extendableProperty.getKeys();
                while (e.hasMoreElements()) {
                    String pkey = e.nextElement();
                    this.root.printNotice("key=" + key + ",pkey=" + pkey);
                    if (pkey.equals(key)) {
                        String value = state.getAttribute(key);
                        if (this.output != null) {
                            createStatePackage(flow, value);
                        }
                    }
                }
            }
            for (String on : state.actionMap.keySet()) {
                if (on.equals(state.getId())) {
                }
                if (on.equals(state.getId())) {
                    appendLine(2, sb, "<transition on=\"" + on + "\" to=\"" + on + "\"/>");
                }
                ClassDoc actionClassDoc = state.actionMap.get(on);
                if (actionClassDoc == null) {
                    continue;
                }
                String to = this.getActionTransition(actionClassDoc);
                if (to == null) {
                    continue;
                }
                StateContainer destState = flow.getState(to);
                if (destState == null) {
                    this.root.printNotice("Cannot find the destination State:[" + to + "]");
                    if (this.output != null) {
                        createStatePackage(flow, to);
                    }
                }
                String parentTag = null;
                if (to.equals(state.getId())) {
                    parentTag = "event-action-list";
                } else {
                    parentTag = "exit-action-list";
                }
                appendAction(sb, actionClassDoc, parentTag, on, false);
                if (to != null && !to.equals(state.getId())) {
                    appendLine(2, sb, "<transition on=\"" + on + "\" to=\"" + to + "\" action=\"" + on + "\"/>");
                } else {
                    appendLine(2, sb, "<transition on=\"" + on + "\" action=\"" + on + "\"/>");
                }
            }
            appendLine(1, sb, "</evaluate-state>");
            appendLine(sb, "</state>");
            this.root.printNotice("state=" + sb.toString());
            File file = null;
            file = new File(filename);
            writeStringToFile(sb.toString(), file, true);
        } catch (IOException e) {
            e.printStackTrace();
            this.root.printError(e.getMessage());
        }
    }

    private void createStatePackage(FlowContainer flow, String stateName) throws IOException {
        String parentPackage = flow.getPackageName();
        String packageName = parentPackage + "." + stateName;
        File file = new File(output, translateSeparator(packageName));
        if (!file.exists()) {
            this.root.printNotice("Creating directory.[" + file + "]");
            boolean isCreated = file.mkdirs();
            if (!isCreated) {
                this.root.printNotice("Already created.[" + file + "]");
            } else {
                this.root.printNotice("Directories created.[" + file + "]");
            }
        }
        File packageInfo = new File(file, "package-info.java");
        if (!packageInfo.exists()) {
            String template = this.readTemplate(PACKAGE_INFO_TEMPLATE);
            template = template.replaceAll("%PACKAGE_NAME%", packageName);
            template = template.replaceAll("%STATE_NAME%", stateName);
            this.root.printNotice("Creating package-info.java.[" + packageInfo + "]");
            writeStringToFile(template, packageInfo, false);
        }
    }

    private void appendAction(StringBuilder sb, ClassDoc actionClassDoc, String parentTag) {
        appendAction(sb, actionClassDoc, parentTag, null, false);
    }

    private void appendAction(StringBuilder sb, ClassDoc actionClassDoc, String parentTag, String id, boolean defaultExecute) {
        if (id == null) {
            appendLine(2, sb, "<" + parentTag + ">");
        } else {
            appendLine(2, sb, "<" + parentTag + " id=\"" + id + "\">");
        }
        if (defaultExecute) {
            appendLine(3, sb, "<property name=\"defaultExecute\" value=\"true\"/>");
        }
        appendLine(3, sb, "<action type=\"pojo\">");
        appendLine(4, sb, "<property name=\"objectClass\" value=\"" + actionClassDoc.qualifiedName() + "\"/>");
        appendLine(4, sb, "<property name=\"method\" value=\"" + this.getActionMethod(actionClassDoc) + "\"/>");
        appendLine(3, sb, "</action>");
        appendLine(2, sb, "</" + parentTag + ">");
    }

    private void appendLine(int indent, StringBuilder sb, String line) {
        for (int i = 0; i < indent * INDENT_SPACE; i++) {
            sb.append(WHITE_SPACE);
        }
        sb.append(line + LINE_SEP);
    }

    private void appendLine(StringBuilder sb, String line) {
        appendLine(0, sb, line);
    }

    private FlowContainer createFlowRoot(PackageDoc pack, ElementValuePair[] valuePairs) {
        String flowId = null;
        String startState = null;
        String[] types = null;
        for (ElementValuePair pair : valuePairs) {
            String name = pair.element().name();
            if (name.equals("startState")) {
                startState = (String) pair.value().value();
            }
            if (name.equals("name")) {
                flowId = (String) pair.value().value();
            }
            if (name.equals("flowDecisionType")) {
                this.root.printNotice("flowDecisionType:" + pair.value().value());
                AnnotationValue[] values = (AnnotationValue[]) pair.value().value();
                List<String> list = new ArrayList<String>();
                for (AnnotationValue value : values) {
                    FieldDoc fd = (FieldDoc) value.value();
                    this.root.printNotice("fd=" + fd.name());
                    list.add(fd.name());
                }
                types = list.toArray(new String[0]);
            }
        }
        FlowContainer container = new FlowContainer(flowId);
        container.setStartState(startState);
        container.setPackageName(pack.name());
        container.setFlowDecisionTypes(types);
        return container;
    }

    private void setupAttribute(Map<String, String> map, ElementValuePair[] valuePairs) {
        String key = null;
        String value = null;
        for (ElementValuePair pair : valuePairs) {
            String name = pair.element().name();
            if (name.equals("key")) {
                key = (String) pair.value().value();
            }
            if (name.equals("value")) {
                value = (String) pair.value().value();
            }
        }
        map.put(key, value);
    }

    private StateContainer createState(PackageDoc pack, ElementValuePair[] valuePairs) {
        String stateId = null;
        StateContainer container = null;
        for (ElementValuePair pair : valuePairs) {
            String name = pair.element().name();
            if (name.equals("name")) {
                stateId = (String) pair.value().value();
                container = new StateContainer(stateId);
            }
        }
        if (container == null) {
            container = new StateContainer(null);
        }
        assert container != null;
        ClassDoc[] classes = pack.allClasses();
        container.setPackageName(pack.name());
        addEvaluateClass(container, classes);
        addActionClass(container);
        setActionClass(container, classes, "EntryAction");
        setActionClass(container, classes, "ExitAction");
        this.root.printNotice("pack.name=" + pack.name());
        this.root.printNotice("container.packageName=" + container.getPackageName());
        return container;
    }

    private void setActionClass(StateContainer container, ClassDoc[] classes, String actionName) {
        for (ClassDoc classDoc : classes) {
            if (classDoc.name().equals(actionName)) {
                if ("ExitAction".equals(actionName)) {
                    container.setExitAction(classDoc);
                } else {
                    container.setEntryAction(classDoc);
                }
                return;
            }
        }
        this.root.printNotice(actionName + " is NOT defined.[state id:" + container.getId() + "]");
    }

    private void addEvaluateClass(StateContainer state, ClassDoc[] classes) {
        List<String> classList = getEvaluateClasses();
        for (ClassDoc classDoc : classes) {
            System.out.println("addEvaluate:classDoc.name=" + classDoc.name());
            String className = classDoc.name();
            if (classList.contains(className)) {
                state.addEvaluateClass(classDoc);
            }
        }
    }

    private String getEvaluateTargetEvent(String className) {
        String rv = null;
        try {
            rv = bundle.getString(className);
        } catch (MissingResourceException e) {
            if (this.extendedBundle == null) throw e;
        }
        if (rv == null && this.extendedBundle != null) {
            rv = this.extendedBundle.getString(className);
        }
        return rv;
    }

    private List<String> getEvaluateClasses() {
        String value = null;
        try {
            value = bundle.getString("classes");
        } catch (MissingResourceException e) {
            if (this.extendedBundle == null) {
                throw e;
            }
        }
        if (value == null && this.extendedBundle != null) {
            value = this.extendedBundle.getString("classes");
        }
        StringTokenizer token = new StringTokenizer(value, ", ");
        List<String> rv = new ArrayList<String>();
        while (token.hasMoreTokens()) {
            rv.add(token.nextToken());
        }
        return rv;
    }

    private String getTransitionSet(ClassDoc classDoc) {
        AnnotationDesc[] annos = classDoc.annotations();
        for (AnnotationDesc anno : annos) {
            AnnotationTypeDoc typeDoc = anno.annotationType();
            ElementValuePair[] pairs = anno.elementValues();
            if (TransitionSet.class.getName().equals((typeDoc.qualifiedName()))) {
                for (ElementValuePair pair : pairs) {
                    if ("values".equals(pair.element().name())) {
                        AnnotationValue[] values = (AnnotationValue[]) pair.value().value();
                        StringBuilder sb = new StringBuilder();
                        for (AnnotationValue value : values) {
                            String v = (String) value.value();
                            if (v.trim().length() == 0) {
                                continue;
                            }
                            sb.append(value.value() + ",");
                        }
                        String rv = sb.toString();
                        return rv.substring(0, rv.length() - 1);
                    }
                }
            }
        }
        return null;
    }

    private void addActionClass(StateContainer state) {
        for (ClassDoc classDoc : state.evaluateList) {
            AnnotationDesc[] annos = classDoc.annotations();
            for (AnnotationDesc anno : annos) {
                AnnotationTypeDoc typeDoc = anno.annotationType();
                ElementValuePair[] pairs = anno.elementValues();
                if (TransitionSet.class.getName().equals((typeDoc.qualifiedName()))) {
                    for (ElementValuePair pair : pairs) {
                        if ("values".equals(pair.element().name())) {
                            AnnotationValue[] values = (AnnotationValue[]) pair.value().value();
                            for (AnnotationValue value : values) {
                                String transition = (String) value.value();
                                String actionClass = getActionClassName(state, transition);
                                ClassDoc actionClassDoc = this.root.classNamed(actionClass);
                                if (actionClassDoc == null && !transition.equals(state.getId())) {
                                    if (output != null) {
                                        createActionClass(actionClass, state.getId());
                                        continue;
                                    } else {
                                        this.root.printWarning("Cannot find actionClass:" + actionClass);
                                    }
                                }
                                state.addActionClasss(transition, actionClassDoc);
                            }
                        }
                    }
                }
            }
        }
    }

    private void createActionClass(String actionClassName, String stateId) {
        try {
            int index = actionClassName.lastIndexOf(".");
            String className = actionClassName.substring(index + 1);
            String packageName = actionClassName.substring(0, index);
            this.root.printNotice("Creating class.[package=" + packageName + "][className=" + className + "]");
            String template = readTemplate(ACTION_CLASS_TEMPLATE);
            template = template.replaceAll("%PACKAGE%", packageName);
            template = template.replaceAll("%CLASS_NAME%", className);
            template = template.replaceAll("%TRANSITION%", stateId);
            String path = output + File.separator + translateSeparator(packageName);
            File pathFile = new File(path);
            if (!pathFile.exists()) {
                boolean isCreated = pathFile.mkdirs();
                if (!isCreated) {
                    this.root.printNotice("Already created.[" + path + "]");
                } else {
                    this.root.printNotice("Directories created.[" + path + "]");
                }
            }
            File file = new File(path, className + ".java");
            this.root.printNotice("Creating source file...[" + file + "]");
            this.root.printNotice(template);
            writeStringToFile(template, file, false);
        } catch (IOException e) {
            this.root.printError(e.getMessage());
        }
    }

    private String translateSeparator(String packageName) {
        StringBuilder pathName = new StringBuilder();
        StringTokenizer token = new StringTokenizer(packageName, ".");
        if (token.hasMoreTokens()) {
            pathName.append(token.nextToken());
        }
        while (token.hasMoreTokens()) {
            pathName.append(File.separator);
            pathName.append(token.nextToken());
        }
        return pathName.toString();
    }

    private String readTemplate(String template) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL url = cl.getResource(template);
        InputStream ins = null;
        try {
            ins = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + LINE_SEP);
            }
            reader.close();
            return sb.toString();
        } finally {
            if (ins != null) {
                ins.close();
            }
        }
    }

    private String getActionClassName(StateContainer state, String name) {
        String packageName = state.getPackageName();
        this.root.printNotice("getActionClassName:name=" + name);
        this.root.printNotice("getActionClassName:packageName=" + packageName);
        int index = name.indexOf(".");
        String basePackage = null;
        String baseName = null;
        if (index > 0) {
            baseName = name.substring(index + 1);
            basePackage = name.substring(0, index);
        } else {
            baseName = name;
        }
        baseName = baseName.substring(0, 1).toUpperCase() + baseName.substring(1, baseName.length());
        this.root.printNotice("getActionClassName:basePackage=" + basePackage);
        this.root.printNotice("getActionClassName:baseName=" + baseName);
        if (basePackage != null) {
            return packageName + "." + basePackage + "." + baseName + "Action";
        } else {
            return packageName + "." + baseName + "Action";
        }
    }

    private String getActionTransition(ClassDoc actionClassDoc) {
        MethodDoc[] methods = actionClassDoc.methods();
        for (MethodDoc method : methods) {
            AnnotationDesc[] annos = method.annotations();
            for (AnnotationDesc anno : annos) {
                String annoName = anno.annotationType().qualifiedName();
                if (annoName.equals(Transition.class.getName())) {
                    ElementValuePair[] pairs = anno.elementValues();
                    for (ElementValuePair pair : pairs) {
                        String value = (String) pair.value().value();
                        return value;
                    }
                }
            }
        }
        this.root.printWarning("Cannot find transition.[actionClass=" + actionClassDoc + "]");
        return null;
    }

    private String getActionMethod(ClassDoc actionClassDoc) {
        MethodDoc[] methods = actionClassDoc.methods();
        for (MethodDoc method : methods) {
            AnnotationDesc[] annos = method.annotations();
            for (AnnotationDesc anno : annos) {
                String annoName = anno.annotationType().qualifiedName();
                if (annoName.equals(Transition.class.getName())) {
                    return method.name();
                }
                if (annoName.equals(Action.class.getName())) {
                    return method.name();
                }
            }
        }
        this.root.printNotice("Cannot find Action annotation.[Class=" + actionClassDoc + "]");
        return DEFAULT_ACTION_METHOD;
    }

    private String getEvaluateMethod(ClassDoc actionClassDoc) {
        MethodDoc[] methods = actionClassDoc.methods();
        for (MethodDoc method : methods) {
            AnnotationDesc[] annos = method.annotations();
            for (AnnotationDesc anno : annos) {
                String annoName = anno.annotationType().qualifiedName();
                if (annoName.equals(Evaluate.class.getName())) {
                    return method.name();
                }
            }
        }
        this.root.printNotice("Cannot find Evaluate annotation.[Class=" + actionClassDoc + "]");
        return DEFAULT_EVALUATE_METHOD;
    }

    private void writeStringToFile(String s, File file, boolean isForce) {
        if (!isForce && file.exists()) {
            this.root.printNotice("Writing to the file is skipped." + file.getAbsolutePath());
        }
        this.root.printNotice("Now writing to file." + file.getAbsolutePath());
        BufferedWriter bwriter = null;
        try {
            FileWriter writer = new FileWriter(file);
            bwriter = new BufferedWriter(writer);
            bwriter.write(s);
            bwriter.flush();
            writer.close();
        } catch (IOException e) {
            this.root.printNotice(e.getMessage());
        } finally {
            if (bwriter != null) {
                try {
                    bwriter.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private static class FlowContainer {

        private String id;

        private String startState;

        private String pack;

        private List<StateContainer> stateList = new ArrayList<StateContainer>();

        private String[] types;

        private FlowContainer(String id) {
            this.id = id;
        }

        public void setFlowDecisionTypes(String[] types) {
            this.types = types;
        }

        @SuppressWarnings("unused")
        public String[] getFlowDecisionTypes() {
            return this.types;
        }

        public String getPackageName() {
            return this.pack;
        }

        public void setPackageName(String pack) {
            this.pack = pack;
        }

        public String getId() {
            if (this.id == null || this.id.length() == 0) {
                int index = this.pack.lastIndexOf(".");
                this.id = this.pack.substring(index + 1);
            }
            return this.id;
        }

        public String getStartStateId() {
            return this.startState;
        }

        public void setStartState(String state) {
            this.startState = state;
        }

        public void addState(StateContainer state) {
            this.stateList.add(state);
        }

        public StateContainer getState(String id) {
            for (StateContainer state : this.stateList) {
                if (state.getId().equals(id)) {
                    return state;
                }
            }
            return null;
        }
    }

    private static class StateContainer {

        private String parentPackage;

        private String packageName;

        private Map<String, String> attrMap = new HashMap<String, String>();

        private String id;

        private ClassDoc entryAction;

        private ClassDoc exitAction;

        private List<ClassDoc> evaluateList = new ArrayList<ClassDoc>();

        private Map<String, ClassDoc> actionMap = new HashMap<String, ClassDoc>();

        private StateContainer(String id) {
            this.id = id;
        }

        public String getId() {
            if (this.id == null || this.id.trim().length() == 0) {
                int index = this.packageName.lastIndexOf(".");
                this.id = this.packageName.substring(index + 1);
            }
            return this.id;
        }

        public Set<String> keySet() {
            return this.attrMap.keySet();
        }

        public void setAttribute(String key, String value) {
            this.attrMap.put(key, value);
        }

        public String getAttribute(String key) {
            return this.attrMap.get(key);
        }

        public void addEvaluateClass(ClassDoc classDoc) {
            this.evaluateList.add(classDoc);
        }

        public void addActionClasss(String transition, ClassDoc classDoc) {
            this.actionMap.put(transition, classDoc);
        }

        public void setEntryAction(ClassDoc classDoc) {
            this.entryAction = classDoc;
        }

        public ClassDoc getEntryAction() {
            return this.entryAction;
        }

        public void setExitAction(ClassDoc classDoc) {
            this.exitAction = classDoc;
        }

        public ClassDoc getExitAction() {
            return this.exitAction;
        }

        public String getParentPackage() {
            if (this.parentPackage == null) {
                int index = this.packageName.lastIndexOf(".");
                this.parentPackage = this.packageName.substring(0, index);
            }
            return this.parentPackage;
        }

        public void setPackageName(String name) {
            this.packageName = name;
        }

        public String getPackageName() {
            return this.packageName;
        }
    }
}
