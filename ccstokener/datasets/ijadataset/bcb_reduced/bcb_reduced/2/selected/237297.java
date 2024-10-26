package net.sourceforge.antex.antprettybuild;

import java.applet.Applet;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import javax.swing.JFileChooser;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class AntPrettyBuildApplet extends Applet implements PrivilegedAction {

    private static final long serialVersionUID = 1L;

    boolean debug = false;

    String methodName = "";

    Object[] objects = null;

    Object object = null;

    String file_separator = System.getProperty("file.separator");

    String path_separator = System.getProperty("path.separator");

    String line_separator = System.getProperty("line.separator");

    String javaVersion = System.getProperty("java.version");

    String envPath = "";

    String javaHome = "";

    String antHome = "";

    String antBuildFile = null;

    String antTargets = null;

    String antLib = null;

    String antLogger = null;

    String antLogFile = null;

    String antMode = null;

    String antMore = null;

    String antPropertyFile = null;

    Properties antProperties = new Properties();

    String antManualURL = "http://ant.apache.org/manual";

    public AntPrettyBuildApplet() {
    }

    /**
	 * Log.
	 */
    public void log(String message) {
        if (debug) System.out.println("> " + message);
    }

    /**
	 * Applet init.
	 */
    public void init() {
        debug = Boolean.valueOf(getParameter("debug")).booleanValue();
        log("Initializing...");
    }

    /**
	 * Applet start.
	 */
    public void start() {
        log("Starting...");
    }

    /**
	 * Applet stop.
	 */
    public void stop() {
        log("Stopping...");
    }

    /**
	 * Applet destroy.
	 */
    public void destroy() {
        log("Destroying...");
    }

    /**
	 * Get.
	 * 
	 * @param fieldName
	 * @return Object
	 */
    public Object get(String fieldName) {
        Object object = null;
        try {
            object = this.getClass().getDeclaredField(fieldName).get(this);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        log("Get " + fieldName + "=" + object);
        return object;
    }

    /**
	 * Set.
	 * 
	 * @param fieldName
	 * @param fieldValue
	 */
    public void set(String fieldName, Object fieldValue) {
        try {
            this.getClass().getDeclaredField(fieldName).set(this, fieldValue);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        log("Set " + fieldName + "=" + fieldValue);
    }

    /**
	 * Invoke void method.
	 * 
	 * @param methodName
	 * @return Object
	 */
    public Object invoke(String methodName) {
        log("Invoking " + methodName + " with no parameters...");
        Object object = null;
        this.methodName = methodName;
        this.objects = null;
        object = AccessController.doPrivileged(this);
        return object;
    }

    /**
	 * Invoke parameterized method.
	 * 
	 * @param methodName
	 * @param objects
	 * @return Object
	 */
    public Object invoke(String methodName, Object objects) {
        log("Invoking " + methodName + " with parameters " + objects + "...");
        Object object = null;
        this.methodName = methodName;
        if (objects != null) this.objects = new Object[] { objects }; else this.objects = null;
        object = AccessController.doPrivileged(this);
        return object;
    }

    /**
	 * Run.
	 * 
	 * @return Object
	 */
    public Object run() {
        log("Running " + methodName + " with parameters " + objects + "...");
        Object object = null;
        Class[] classes = null;
        if (objects != null) {
            classes = new Class[objects.length];
            for (int i = 0; i < objects.length; i++) {
                if (objects[i] != null) {
                    classes[i] = objects[i].getClass();
                    log("classes[" + i + "]=" + classes[i]);
                }
            }
        }
        try {
            object = Class.forName(this.getClass().getName()).getDeclaredMethod(methodName, classes).invoke(this, objects);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return object;
    }

    /**
	 * Get product latest version.
	 * 
	 * @param website
	 * @return String
	 */
    public String getLatestVersion(String website) {
        String latestVersion = "";
        try {
            URL url = new URL(website + "/version");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
            String string;
            while ((string = bufferedReader.readLine()) != null) {
                latestVersion = string;
            }
            bufferedReader.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
        }
        return latestVersion;
    }

    /**
	 * Set environment.
	 */
    public void setEnvironment() {
        log("Setting environment...");
        try {
            antBuildFile = URLDecoder.decode(getDocumentBase().getFile(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String paths[] = null;
        if (envPath != null && envPath.equals("")) {
            envPath = System.getenv("PATH");
        }
        if (envPath != null) {
            paths = envPath.split(path_separator);
        }
        if (!javaHome.equals("")) System.setProperty("JAVA_HOME", javaHome);
        if (!antHome.equals("")) System.setProperty("ANT_HOME", antHome);
        if (paths != null) {
            String jHome = "";
            String aHome = "";
            for (int i = 0; i < paths.length; i++) {
                if (paths[i] != null) {
                    String path = paths[i].replaceAll("\"", "");
                    String parent = new File(path).getParent();
                    String[] jvmPaths = { path + file_separator + "java", parent + file_separator + "lib" + file_separator + "core.jar", parent + file_separator + "lib" + file_separator + "rt.jar" };
                    String[] jdkPaths = { parent + file_separator + "jre" + file_separator + "lib" + file_separator + "core.jar", parent + file_separator + "jre" + file_separator + "lib" + file_separator + "rt.jar", parent + file_separator + "lib" + file_separator + "tools.jar" };
                    String[] antPaths = { parent + file_separator + "lib" + file_separator + "ant.jar" };
                    File file = null;
                    for (int j = 0; j < jdkPaths.length; j++) {
                        file = new File(jdkPaths[j]);
                        if (file != null && file.isFile() && file.exists()) jHome = parent;
                    }
                    for (int j = 0; j < jvmPaths.length; j++) {
                        file = new File(jvmPaths[j]);
                        if (file != null && file.isFile() && file.exists()) jHome = parent;
                    }
                    for (int j = 0; j < antPaths.length; j++) {
                        file = new File(antPaths[j]);
                        if (file != null && file.isFile() && file.exists()) aHome = parent;
                    }
                }
            }
            if (javaHome.equals("") && !jHome.equals("")) javaHome = jHome;
            if (antHome.equals("") && !aHome.equals("")) antHome = aHome;
        }
    }

    /**
	 * Execute.
	 */
    public void execute() {
        log("Executing...");
        Vector vector = new Vector();
        String[] javaCommand = new String[] { !javaHome.equals("") ? javaHome + file_separator + "bin" + file_separator + "java" : "java" };
        String[] javaOptions = new String[] { "-classpath", antHome + file_separator + "lib" + file_separator + "ant-launcher.jar", "-Dant.home=" + antHome };
        String[] javaClass = new String[] { "org.apache.tools.ant.launch.Launcher" };
        String[] javaArgs = new String[] { "" };
        for (int i = 0; i < javaCommand.length; i++) {
            vector.add(javaCommand[i]);
        }
        for (int i = 0; i < javaOptions.length; i++) {
            vector.add(javaOptions[i]);
        }
        for (int i = 0; i < javaClass.length; i++) {
            vector.add(javaClass[i]);
        }
        for (int i = 0; i < javaArgs.length; i++) {
            vector.add(javaArgs[i]);
        }
        if (antProperties != null) {
            Enumeration keys = antProperties.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                String value = (String) antProperties.get(key);
                vector.add("-D" + key + "=" + value);
            }
        }
        if (antLib != null && !antLib.equals("")) {
            String[] antLibArg = { "-lib", antLib };
            for (int i = 0; i < antLibArg.length; i++) {
                vector.add(antLibArg[i]);
            }
        }
        if (antLogger != null && !antLogger.equals("")) {
            String antLoggerClassName = null;
            String antLoggerClassArgs = null;
            if (antLogger.equals("NoBannerLogger")) {
                antLoggerClassName = "org.apache.tools.ant.NoBannerLogger";
                antLoggerClassArgs = "";
            }
            if (antLogger.equals("TimestampedLogger")) {
                antLoggerClassName = "org.apache.tools.ant.listener.TimestampedLogger";
                antLoggerClassArgs = "";
            }
            if (antLogger.equals("XmlLogger")) {
                antLoggerClassName = "org.apache.tools.ant.XmlLogger";
                antLoggerClassArgs = "-Dant.XmlLogger.stylesheet.uri=" + "file://" + antHome.replace('\\', '/') + "/etc/log.xsl";
            }
            String[] antLoggerArg = { "-logger", antLoggerClassName, antLoggerClassArgs };
            for (int i = 0; i < antLoggerArg.length; i++) {
                vector.add(antLoggerArg[i]);
            }
        }
        if (antLogFile != null && !antLogFile.equals("")) {
            String[] antLogFileArg = { "-logfile", antLogFile };
            for (int i = 0; i < antLogFileArg.length; i++) {
                vector.add(antLogFileArg[i]);
            }
        }
        if (antMode != null && !antMode.equals("")) {
            String[] antModeArg = { "-" + antMode };
            for (int i = 0; i < antModeArg.length; i++) {
                vector.add(antModeArg[i]);
            }
        }
        if (antMore != null && !antMore.equals("")) {
            String[] antMoreArg = { antMore };
            for (int i = 0; i < antMoreArg.length; i++) {
                vector.add(antMoreArg[i]);
            }
        }
        if (antPropertyFile != null && !antPropertyFile.equals("")) {
            String[] antPropertyFileArg = { "-propertyfile", antPropertyFile };
            for (int i = 0; i < antPropertyFileArg.length; i++) {
                vector.add(antPropertyFileArg[i]);
            }
        }
        if (antBuildFile != null && !antBuildFile.equals("")) {
            String[] antBuildFileArg = { "-buildfile", antBuildFile };
            for (int i = 0; i < antBuildFileArg.length; i++) {
                vector.add(antBuildFileArg[i]);
            }
        }
        if (antTargets != null && !antTargets.equals("")) {
            String[] antTargetsArg = antTargets.split("\\s");
            for (int i = 0; i < antTargetsArg.length; i++) {
                vector.add(antTargetsArg[i]);
            }
        }
        String[] command = new String[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            if (vector.get(i) != null) {
                command[i] = (String) vector.get(i);
                log(command[i]);
            }
        }
        StringBuffer[] stringBuffer = exec(command);
        StringBuffer outputStringBuffer = stringBuffer[0];
        StringBuffer errorStringBuffer = stringBuffer[1];
        log(outputStringBuffer.toString());
        log(errorStringBuffer.toString());
        String jsCommand = null;
        if (outputStringBuffer.indexOf("BUILD FAILED") >= 0 || errorStringBuffer.indexOf("BUILD FAILED") >= 0) {
            jsCommand = "if (typeof onBuildFailed == 'function') onBuildFailed()";
        } else if (outputStringBuffer.indexOf("BUILD SUCCESSFUL") >= 0 || errorStringBuffer.indexOf("BUILD SUCCESSFUL") >= 0) {
            jsCommand = "if (typeof onBuildSuccessful == 'function') onBuildSuccessful()";
        }
        if (jsCommand != null) {
            Object object = jsExec(jsCommand);
        }
    }

    /**
	 * Exec.
	 * 
	 * @param command
	 * @return StringBuffer[]
	 */
    public StringBuffer[] exec(String[] command) {
        StringBuffer[] stringBuffer = null;
        final StringBuffer outputStringBuffer = new StringBuffer();
        final StringBuffer errorStringBuffer = new StringBuffer();
        try {
            final Process p = Runtime.getRuntime().exec(command, null, new File(antBuildFile).getParentFile());
            Thread outputThread = new Thread() {

                public void run() {
                    final BufferedReader outputBufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String line;
                    try {
                        while ((line = outputBufferedReader.readLine()) != null) {
                            outputStringBuffer.append(line + line_separator);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            outputBufferedReader.close();
                        } catch (IOException e) {
                        }
                    }
                }
            };
            Thread errorThread = new Thread() {

                public void run() {
                    final BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                    String line;
                    try {
                        while ((line = errorBufferedReader.readLine()) != null) {
                            errorStringBuffer.append(line + line_separator);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            errorBufferedReader.close();
                        } catch (IOException e) {
                        }
                    }
                }
            };
            try {
                outputThread.start();
                errorThread.start();
                int result = p.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                try {
                    outputThread.join();
                    errorThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                p.destroy();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        stringBuffer = new StringBuffer[] { outputStringBuffer, errorStringBuffer };
        return stringBuffer;
    }

    /**
	 * Set a property.
	 * 
	 * @param propertyName
	 * @param propertyValue
	 */
    public void setProperty(String propertyName, String propertyValue) {
        log("Setting property " + propertyName + "=" + propertyValue);
        antProperties.put(propertyName, propertyValue);
    }

    /**
	 * Remove a property.
	 * 
	 * @param propertyName
	 */
    public void removeProperty(String propertyName) {
        log("Removing property " + propertyName);
        antProperties.remove(propertyName);
    }

    /**
	 * Reset properties.
	 */
    public void resetProperties() {
        log("Resetting properties...");
        antProperties.clear();
    }

    /**
	 * Expand a property value.
	 * 
	 * @param propertyValue
	 * @return String
	 */
    public String expandPropertyValue(String value) {
        String expandedPropertyValue = "";
        NodeList projectList = getDocument(getDocumentBase().toString()).getElementsByTagName("project");
        NodeList propertyList = getDocument(getDocumentBase().toString()).getElementsByTagName("property");
        NodeList basenameList = getDocument(getDocumentBase().toString()).getElementsByTagName("basename");
        Element element = null;
        Vector fragments = new Vector();
        int prev = 0;
        int pos;
        while ((pos = value.indexOf("$", prev)) >= 0) {
            if (pos > 0) {
                fragments.addElement(value.substring(prev, pos));
            }
            if (pos == (value.length() - 1)) {
                fragments.addElement("$");
                prev = pos + 1;
            } else if (value.charAt(pos + 1) != '{') {
                if (value.charAt(pos + 1) == '$') {
                    fragments.addElement("$");
                    prev = pos + 2;
                } else {
                    fragments.addElement(value.substring(pos, pos + 2));
                    prev = pos + 2;
                }
            } else {
                int endName = value.indexOf('}', pos);
                if (endName < 0) {
                    log("Syntax error in property: " + value);
                }
                String propertyName = value.substring(pos + 2, endName);
                String propertyValue = System.getProperty(propertyName);
                if (propertyValue == null) {
                    propertyValue = antProperties.getProperty(propertyName);
                }
                if (propertyValue == null) {
                    for (int i = 0; i < propertyList.getLength(); i++) {
                        element = (Element) propertyList.item(i);
                        String pName = element.getAttribute("name");
                        String pValue = element.getAttribute("value");
                        if (propertyName.equals(pName)) propertyValue = pValue;
                    }
                    for (int i = 0; i < basenameList.getLength(); i++) {
                        element = (Element) basenameList.item(i);
                        String pName = element.getAttribute("property");
                        String pFile = element.getAttribute("file");
                        String pSuffix = element.getAttribute("suffix");
                        if (propertyName.equals(pName)) {
                            if (pFile.indexOf("${") != -1) {
                                pFile = expandPropertyValue(pFile);
                            }
                            String pFileName = new File(pFile).getName();
                            if (pSuffix == null) {
                                pFile = pFileName;
                            } else {
                                if (!pSuffix.startsWith(".")) pSuffix = "." + pSuffix;
                                pFile = pFileName.substring(0, pFileName.lastIndexOf(pSuffix));
                            }
                            propertyValue = pFile;
                        }
                    }
                    if (propertyName.equals("ant.home")) {
                        propertyValue = antHome;
                    }
                    if (propertyName.equals("ant.file")) {
                        propertyValue = antBuildFile;
                    }
                    if (propertyName.equals("ant.project.name")) {
                        propertyValue = ((Element) projectList.item(0)).getAttribute("name");
                    }
                    if (propertyName.equals("basedir")) {
                        try {
                            propertyValue = new File(((Element) projectList.item(0)).getAttribute("basedir")).getCanonicalPath();
                        } catch (IOException e) {
                        }
                    }
                    if (propertyName.equals("ant.version")) {
                        String osName = System.getProperty("os.name").toLowerCase();
                        String[] command = (osName.indexOf("windows") >= 0 || osName.indexOf("nt") >= 0) ? new String[] { antHome + "/bin/ant.bat", "-version" } : new String[] { antHome + "/bin/ant", "-version" };
                        StringBuffer[] stringBuffer = exec(command);
                        propertyValue = stringBuffer[0].toString();
                    }
                }
                if (propertyValue == null) {
                    propertyValue = "${" + propertyName + "}";
                }
                log(propertyName + "=" + propertyValue);
                fragments.addElement(propertyValue);
                prev = endName + 1;
            }
        }
        if (prev < value.length()) {
            fragments.addElement(value.substring(prev));
        }
        fragments.trimToSize();
        for (int i = 0; i < fragments.size(); i++) {
            expandedPropertyValue += (String) fragments.get(i);
        }
        return expandedPropertyValue;
    }

    /**
	 * Browse file or directory.
	 * 
	 * @return String
	 */
    public String browseFile(String fileSelectionMode) {
        String string = null;
        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setFileSelectionMode(Integer.parseInt(fileSelectionMode));
        int returnVal = jFileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = jFileChooser.getSelectedFile();
            try {
                string = file.getCanonicalPath();
            } catch (IOException e) {
            }
        } else {
        }
        return string;
    }

    /**
	 * Load property files.
	 */
    public void loadPropertyFiles() {
        log("Loading property files...");
        NodeList propertyList = getDocument(getDocumentBase().toString()).getElementsByTagName("property");
        Element element = null;
        String fileAttribute = null;
        for (int i = 0; i < propertyList.getLength(); i++) {
            element = (Element) propertyList.item(i);
            fileAttribute = element.getAttribute("file");
            loadPropertyFile(fileAttribute);
        }
    }

    /**
	 * Load single property file.
	 */
    public void loadPropertyFile(String fileAttribute) {
        Properties properties = new Properties();
        if (fileAttribute != null && !fileAttribute.equals("")) {
            try {
                int i = 0;
                while (fileAttribute.indexOf("${") != -1 && i < 10) {
                    fileAttribute = expandPropertyValue(fileAttribute);
                    i++;
                }
                log("Loading file " + fileAttribute + "...");
                properties.load(new FileInputStream(new File(fileAttribute).getCanonicalFile()));
                log("Loaded successfully.");
            } catch (IOException e) {
                try {
                    properties.load(new FileInputStream(new File(antBuildFile).getParentFile().getCanonicalPath() + file_separator + fileAttribute));
                    log("Loaded successfully.");
                } catch (IOException ioe) {
                }
            }
        }
        Enumeration keys = properties.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            String value = (String) properties.get(key);
            jsExec("addPropertyRow('" + key + "', '" + value + "')");
        }
    }

    /**
	 * Get DOM document.
	 * 
	 * @param uri
	 * @return Document
	 */
    public Document getDocument(String uri) {
        Document document = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            document = factory.newDocumentBuilder().parse(uri);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        return document;
    }

    /**
	 * Get XML fragment file name.
	 * 
	 * @param name
	 * @return String
	 */
    public String getXMLFragmentFileName(String name) {
        String tempFileName = null;
        Document document = null;
        try {
            File tempFile = File.createTempFile("antprettybuild." + name + ".", ".xml");
            tempFile.deleteOnExit();
            tempFileName = tempFile.getCanonicalPath();
            log("Temporary file=" + tempFileName);
            document = getDocument(getDocumentBase().toString());
            Element element = null;
            NamedNodeMap attrs = null;
            Attr attr = null;
            String attrName = null;
            String attrValue = null;
            if (name != null && !name.equals("null")) {
                NodeList targetList = document.getElementsByTagName("target");
                for (int i = 0; i < targetList.getLength(); i++) {
                    attrs = targetList.item(i).getAttributes();
                    int attrsLength = attrs.getLength();
                    for (int j = 0; j < attrsLength; j++) {
                        attr = (Attr) attrs.item(j);
                        attrName = attr.getNodeName();
                        attrValue = attr.getNodeValue();
                        if (attrName.equals("name") && attrValue.equals(name)) {
                            element = (Element) targetList.item(i);
                        }
                    }
                }
            } else {
                NodeList projectList = document.getElementsByTagName("project");
                element = (Element) projectList.item(0);
            }
            Source xmlSource = new DOMSource(element);
            Result outputTarget = new StreamResult(tempFile);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty("omit-xml-declaration", "yes");
            transformer.transform(xmlSource, outputTarget);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tempFileName;
    }

    /**
	 * Get URL of task documentation from online Ant manual.
	 * 
	 * @param taskName
	 * @return String
	 */
    public String getAntTaskManualURL(String taskName) {
        String taskManualURL = null;
        String antManualCoreTasksURL = antManualURL + "/CoreTasks";
        String antManualOptionalTasksURL = antManualURL + "/OptionalTasks";
        boolean isCoreTask = false;
        try {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(antManualCoreTasksURL + "/" + taskName + ".html").openConnection();
            httpURLConnection.setRequestMethod("HEAD");
            isCoreTask = (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (IOException e) {
        }
        taskManualURL = isCoreTask ? antManualCoreTasksURL + "/" + taskName + ".html" : antManualOptionalTasksURL + "/" + taskName + ".html";
        return taskManualURL;
    }

    /**
	 * JS Exec.
	 * 
	 * @return Object
	 */
    public Object jsExec(String command) {
        Object object = null;
        try {
            Class jsObject = Class.forName("netscape.javascript.JSObject");
            Method getWindow = jsObject.getDeclaredMethod("getWindow", new Class[] { Applet.class });
            Method eval = jsObject.getDeclaredMethod("eval", new Class[] { String.class });
            Object window = getWindow.invoke(jsObject, new Object[] { this });
            object = eval.invoke(window, new Object[] { command });
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return object;
    }

    /**
	 * Java Exec.
	 * 
	 * @param string
	 */
    public void javaExec(String string) {
        log("Executing java...");
        Vector vector = new Vector();
        String[] javaCommand = new String[] { !javaHome.equals("") ? javaHome + file_separator + "bin" + file_separator + "java" : "java" };
        String[] javaArgs = toArray(string);
        for (int i = 0; i < javaCommand.length; i++) {
            vector.add(javaCommand[i]);
        }
        for (int i = 0; i < javaArgs.length; i++) {
            vector.add(javaArgs[i]);
        }
        String[] command = new String[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            if (vector.get(i) != null) {
                command[i] = (String) vector.get(i);
                log(command[i]);
            }
        }
        StringBuffer[] stringBuffer = exec(command);
        StringBuffer outputStringBuffer = stringBuffer[0];
        StringBuffer errorStringBuffer = stringBuffer[1];
        log(outputStringBuffer.toString());
        log(errorStringBuffer.toString());
    }

    /**
	 * Convert joined array to array.
	 * 
	 * @param string
	 * @return String[]
	 */
    public String[] toArray(String string) {
        String[] strings = string.split("#");
        return strings;
    }
}
