package net.benojt;

import java.awt.Component;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Enumeration;
import java.util.TreeMap;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import net.benojt.context.Context;
import net.benojt.dlgs.BenojtDlg;
import net.benojt.dlgs.IteratorManagerDlg;
import net.benojt.dlgs.NewIteratorDlg;
import net.benojt.iterator.Iterator;
import net.benojt.tools.CompileThread;
import net.benojt.tools.IteratorTemplateData;
import net.benojt.tools.TemplateItem;
import net.benojt.tools.UIModule;
import net.benojt.tools.Wrapper;
import net.benojt.xml.XMLNode;
import net.benojt.xml.XMLTag;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * manage and create iterators. this class contains all kinds of functions for iterators.
 * @author frank
 *
 */
public class IteratorManager {

    public static final String ITERATOR_PACKAGE = "net.benojt.iterator";

    public static final String FILE_SEPERATOR = System.getProperty("file.separator");

    public static final String LINEBREAK = System.getProperty("line.separator");

    public static final String TEMPLATE_DIR = "template/";

    public static final String CLASS_DIR = "classes/";

    public static final String SOURCE_DIR = "src/";

    /** the list of available iterators from the distribution */
    static Collection<Class<? extends Iterator>> distIterators;

    /** the list of local (self compiled) iterators*/
    static Collection<Class<? extends Iterator>> localIterators;

    /** the list of available iterator templates */
    static Collection<String> templateNames;

    /** the last successfully parsed template */
    static IteratorTemplateData lastParsedTemplate;

    /** the fractal panel this iterator manager belongs to */
    FractalPanel fp;

    /** a dialog to manage the iterators */
    IteratorManagerDlg dlg;

    /**
	 * the files that need to be downloaded from jar for compiling template iterators
	 */
    static final String[] downloadFiles = new String[] { "net", "net/benojt", "net/benojt/FractalPanel.class", "net/benojt/iterator", "net/benojt/iterator/Iterator.class", "net/benojt/iterator/IteratorReport.class", "net/benojt/iterator/AbstractIterator.class", "net/benojt/iterator/AbstractIterator$ConfigDlg.class", "net/benojt/iterator/AbstractTemplate.class", "net/benojt/iterator/AbstractAttractor.class", "net/benojt/iterator/AbstractParameterIterator.class", "net/benojt/iterator/AbstractParameterIterator$ConfigDlg.class", "net/benojt/iterator/IteratorTemplate.class", "net/benojt/iterator/Lyapunov.class", "net/benojt/iterator/Buddhabrot.class", "net/benojt/display", "net/benojt/display/Display.class", "net/benojt/tools", "net/benojt/tools/Complex.class", "net/benojt/tools/BigDecimalComplex.class", "net/benojt/tools/Cloneable.class", "net/benojt/tools/AbstractUIModule.class", "net/benojt/tools/AbstractUIModule$ConfigDlg.class", "net/benojt/tools/UIModule.class", "net/benojt/tools/BoundingBox.class", "net/benojt/ui", "net/benojt/ui/IntegerSpinner.class", "net/benojt/ui/NumberSpinner.class", "net/benojt/ui/NumberTextField.class", "net/benojt/ui/DoubleTextField.class", "net/benojt/xml", "net/benojt/xml/XMLNode.class", "net/benojt/dlgs", "net/benojt/dlgs/BenojtDlg.class", "net/benojt/dlgs/DlgConstraints.class" };

    public IteratorManager(FractalPanel fw) {
        this.fp = fw;
        this.initialize();
    }

    public void initialize() {
        distIterators = this.loadDistIterators();
        localIterators = this.loadLocalIterators();
        templateNames = this.loadTemplates();
    }

    /**
	 * load the  iterators from the distribution either from jar-file or compiled tree.
	 */
    private Vector<Class<? extends Iterator>> loadDistIterators() {
        TreeMap<String, Class<? extends Iterator>> iteratorTM = new TreeMap<String, Class<? extends Iterator>>();
        try {
            ClassLoader cl = this.getClass().getClassLoader();
            URL url = cl.getResource(ITERATOR_PACKAGE.replace(".", "/"));
            File dir = new File(URLDecoder.decode(url.getPath(), "UTF-8"));
            if (dir.isDirectory()) {
                for (File f : dir.listFiles()) {
                    String classLoc = ITERATOR_PACKAGE + "." + f.getName().substring(0, f.getName().indexOf("."));
                    try {
                        Class<?> c = cl.loadClass(classLoc);
                        if ((c.getModifiers() & Modifier.ABSTRACT) == 0 && Iterator.class.isAssignableFrom(c)) iteratorTM.put(c.getSimpleName(), c.asSubclass(Iterator.class));
                    } catch (Exception ex) {
                        System.out.println("could not load " + classLoc);
                        ex.printStackTrace();
                    }
                }
            } else {
                JarURLConnection conn = (JarURLConnection) url.openConnection();
                JarFile jarFile = conn.getJarFile();
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String className = entry.getName();
                    if (className.startsWith(ITERATOR_PACKAGE.replace(".", "/")) && className.endsWith(".class")) {
                        String classLoc = className.substring(0, className.indexOf(".class")).replace("/", ".");
                        Class<?> c = cl.loadClass(classLoc);
                        if ((c.getModifiers() & Modifier.ABSTRACT) == 0 && Iterator.class.isAssignableFrom(c)) iteratorTM.put(c.getSimpleName(), c.asSubclass(Iterator.class));
                    }
                }
            }
            return new Vector<Class<? extends Iterator>>(iteratorTM.values());
        } catch (SecurityException ex) {
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
	 * reads the list of iterators in the users home directory.
	 * if the home directory is not accessible localIterators is set to null
	 * @return the list of iterator classes in the local direcory
	 *
	 */
    public Vector<Class<? extends Iterator>> loadLocalIterators() {
        TreeMap<String, Class<? extends Iterator>> localIterTM = new TreeMap<String, Class<? extends Iterator>>();
        try {
            String clsPath = Context.getBenoitDir() + FILE_SEPERATOR + CLASS_DIR;
            File dir = new File(clsPath + ITERATOR_PACKAGE.replace(".", "/"));
            if (dir.isDirectory()) {
                ClassLoader cl = this.getClass().getClassLoader();
                File classesDir = new File(clsPath);
                URLClassLoader loader1 = new URLClassLoader(new URL[] { classesDir.toURI().toURL() }, cl);
                for (File f : dir.listFiles()) {
                    String classLoc = ITERATOR_PACKAGE + "." + f.getName().substring(0, f.getName().indexOf("."));
                    try {
                        Class<?> c = loader1.loadClass(classLoc);
                        if ((c.getModifiers() & Modifier.ABSTRACT) == 0 && Iterator.class.isAssignableFrom(c)) localIterTM.put(c.getSimpleName(), c.asSubclass(Iterator.class));
                    } catch (UnsupportedClassVersionError ucve) {
                        System.out.println("wrong version in class " + classLoc);
                    } catch (Throwable ex) {
                        System.out.println("could not load " + classLoc);
                        ex.printStackTrace();
                    }
                }
            } else System.out.println("no local iterator dir");
        } catch (SecurityException ex) {
            localIterTM = null;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (localIterTM == null) return null;
        return new Vector<Class<? extends Iterator>>(localIterTM.values());
    }

    /**
	 * reads a list of templates from the archive.
	 * if the archive is not accessible templates is set to null
	 *
	 */
    private Vector<String> loadTemplates() {
        TreeMap<String, String> templateTM = new TreeMap<String, String>();
        try {
            File dir = new File(TEMPLATE_DIR);
            if (dir.isDirectory()) {
                for (File f : dir.listFiles()) if (!f.isDirectory()) {
                    String name = f.getName();
                    templateTM.put(name, name);
                }
            } else {
                ClassLoader cl = this.getClass().getClassLoader();
                URL url = cl.getResource(ITERATOR_PACKAGE.replace(".", "/"));
                JarURLConnection conn = (JarURLConnection) url.openConnection();
                JarFile jarFile = conn.getJarFile();
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String className = entry.getName();
                    if (className.startsWith("template") && className.endsWith(".java")) {
                        URL template = cl.getResource(className);
                        String name = template.toString().substring(template.toString().lastIndexOf("/") + 1);
                        templateTM.put(name, name);
                    }
                }
            }
        } catch (SecurityException ex) {
            templateTM = null;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return templateTM == null ? null : new Vector<String>(templateTM.values());
    }

    /**
	 * returns the list of iterators in the distribution. 
	 * either read from the iterator package or from fixed array.
	 * @return
	 */
    public Collection<Class<? extends Iterator>> getDistIterators() {
        Collection<Class<? extends Iterator>> res = null;
        if (distIterators == null) res = Context.iterators; else res = distIterators;
        return res;
    }

    /**
	 * returns the list of local (user created) iterators.
	 * @return
	 */
    public Collection<Class<? extends Iterator>> getLocalIterators() {
        return localIterators;
    }

    /**
	 * returns the names of the templates in the template directory.
	 * @return
	 */
    public Collection<String> getTemplates() {
        return templateNames;
    }

    public void showIteratorManagerDlg() {
        if (dlg == null) {
            dlg = new IteratorManagerDlg(this);
            dlg.setLocationByPlatform(true);
        }
        int result = dlg.showDlg();
        Object sel = dlg.getSelectedItem();
        switch(result) {
            case IteratorManagerDlg.USE_ITERATOR:
                if (sel == null) break; else if (sel instanceof Wrapper.Iterator) {
                    Class<? extends Iterator> cls = ((Wrapper.Iterator) sel).getContent();
                    if (cls != null) {
                        if (sel instanceof Wrapper.DistIterator) this.fp.setIteratorFromDefault(cls); else if (sel instanceof Wrapper.LocalIterator) this.fp.setIterator(cls, dlg.getInitIter());
                    }
                } else if (sel instanceof Wrapper.Template) {
                    String templName = ((Wrapper.Template) sel).getContent();
                    useTemplate(templName, dlg.getInitIter());
                }
                break;
            case IteratorManagerDlg.USE_LAST:
                this.createIteratorFromTemplate(lastParsedTemplate, dlg.getInitIter());
                return;
            case IteratorManagerDlg.EDIT_ITERATOR:
                if (!(sel instanceof Wrapper.LocalIterator)) break;
                Class<? extends Iterator> editcls = ((Wrapper.LocalIterator) sel).getContent();
                this.editLocalIterator(editcls, dlg.getInitIter());
                break;
        }
    }

    /**
	 * create an iterator from given template.
	 * @param templName
	 * @param initIter
	 */
    public void useTemplate(String templName, boolean initIter) {
        String templateName = templName.substring(0, templName.lastIndexOf(".java"));
        String tName = TEMPLATE_DIR + templName;
        IteratorTemplateData parsedTemplate = this.parseTemplate(tName);
        if (parsedTemplate == null) {
            JOptionPane.showMessageDialog(null, "Could not read template.", "Template Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        parsedTemplate.setTemplateName(templateName);
        this.createIteratorFromTemplate(parsedTemplate, initIter);
        this.initialize();
    }

    /**
	 * deletes an iterator from the benojt directory.
	 * @param delcls the iterator class to delete
	 */
    public void deleteLocalIterator(Class<?> delcls) {
        if (delcls != null) {
            try {
                String clsPath = Context.getBenoitDir() + FILE_SEPERATOR + CLASS_DIR;
                File clsFile = new File(clsPath + delcls.getName().replace(".", "/") + ".class");
                if (clsFile.canWrite()) clsFile.delete();
                String srcPath = Context.getBenoitDir() + FILE_SEPERATOR + SOURCE_DIR;
                File srcFile = new File(srcPath + delcls.getSimpleName() + ".java");
                if (srcFile.canWrite()) srcFile.delete();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            this.initialize();
        }
    }

    /**
	 * use a local iterator to create a new one. 
	 * first tries reading the template items from the class file. 
	 * if this fails it tries to read them from the source file.
	 * @param editcls the iterator class
	 * @param initIter if true initializes the iterator defaults
	 */
    public void editLocalIterator(Class<? extends Iterator> editcls, boolean initIter) {
        IteratorTemplateData parsedTemplate = null;
        if (editcls != null) {
            try {
                parsedTemplate = IteratorManager.getIteratorTemplate(editcls);
                String templateLoc = parsedTemplate.getTemplateLoc();
                BufferedReader br1 = new BufferedReader(Context.getReader(templateLoc));
                String line = null;
                StringBuffer templateText = new StringBuffer();
                while ((line = br1.readLine()) != null) templateText.append(line + "\n");
                parsedTemplate.setTemplateText(templateText.toString());
                br1.close();
                System.out.println("read items from class");
            } catch (Exception classEx) {
                System.out.println("could not create template from class " + this.getClass().getName());
                try {
                    String srcPath = Context.getBenoitDir() + FILE_SEPERATOR + SOURCE_DIR;
                    String srcName = editcls.getSimpleName() + ".java";
                    String srcFilePath = srcPath + srcName;
                    parsedTemplate = this.parseTemplate(srcFilePath);
                    if (parsedTemplate == null) throw new Exception("could not parse iterator source");
                    parsedTemplate.setTemplateName(editcls.getSimpleName());
                    parsedTemplate.setClassName(editcls.getSimpleName());
                } catch (Exception sourceEx) {
                    System.out.println("could not create template " + this.getClass().getName());
                    classEx.printStackTrace();
                    sourceEx.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Could not read data from iterator.", "Template Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            this.createIteratorFromTemplate(parsedTemplate, initIter);
            this.initialize();
            return;
        }
    }

    /**
	 * returns the fractal panel this iterator manager belongs to
	 * @return
	 */
    public FractalPanel getFractalPanel() {
        return this.fp;
    }

    /**
	 * parses a iterator template source for items to be inserted.
	 * since the insertable item marks are in the source files of the 
	 * created iterators it can also parse the source of an created iterator. 
	 * it is however better to read the template items of a created iterator 
	 * from the class file.
	 * @param templateLoc the source of the template or iterator source
	 * @return 
	 */
    private IteratorTemplateData parseTemplate(String templateLoc) {
        IteratorTemplateData res = new IteratorTemplateData();
        res.setTemplateLoc(templateLoc);
        BufferedReader br1 = new BufferedReader(Context.getReader(templateLoc));
        try {
            String line = null;
            String name = null;
            boolean isString = false;
            StringBuffer itemText = new StringBuffer();
            StringBuffer templateText = new StringBuffer();
            while ((line = br1.readLine()) != null) {
                templateText.append(line + "\n");
                String l = line.trim();
                if (l.startsWith("//\"<")) {
                    name = l.substring(4);
                    isString = true;
                } else if (l.startsWith("//<")) {
                    name = l.substring(3);
                    isString = false;
                } else if ((name != null) && (l.startsWith("//>"))) {
                    res.put(name, new TemplateItem(name, itemText.toString()));
                    name = null;
                    itemText = new StringBuffer();
                } else if (name != null) {
                    if (isString) {
                        int pos1 = l.indexOf("\"");
                        int pos2 = l.lastIndexOf("\"");
                        if (pos1 >= 0 && pos2 > 0 && pos2 > pos1) itemText.append(l.substring(pos1 + 1, pos2) + "\n");
                    } else itemText.append(l + "\n");
                }
            }
            res.setTemplateText(templateText.toString());
            br1.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        lastParsedTemplate = res;
        return res;
    }

    /**
	 * create java file for iterator from parsed template items and user input.
	 * @param parsedTemplate
	 * @return the java file
	 */
    private File createSourceFile(IteratorTemplateData parsedTemplate) {
        String localSrcPath = null;
        try {
            localSrcPath = Context.getBenoitDir() + FILE_SEPERATOR + SOURCE_DIR;
            File srcDir = new File(localSrcPath);
            if (!srcDir.exists()) srcDir.mkdirs();
            if (!srcDir.canRead() || !srcDir.canWrite()) throw new SecurityException();
        } catch (SecurityException sex) {
            return null;
        }
        String templateName = parsedTemplate.getTemplateName();
        String className = parsedTemplate.getClassName();
        String srcPath = localSrcPath + FILE_SEPERATOR + className + ".java";
        File res = new File(srcPath);
        BufferedReader templateReader = new BufferedReader(new StringReader(parsedTemplate.getTemplateText()));
        FileWriter writer = null;
        try {
            writer = new FileWriter(res);
            boolean skip = false;
            String line = null;
            String name = null;
            while ((line = templateReader.readLine()) != null) {
                String l = line.trim();
                if (l.startsWith("//<")) {
                    name = l.substring(3);
                    writer.write(line + LINEBREAK);
                    writer.write(parsedTemplate.get(name).getTextComponent().getText().trim() + LINEBREAK);
                    skip = true;
                } else if (l.startsWith("//>")) {
                    writer.write(line + LINEBREAK);
                    skip = false;
                } else if (l.startsWith("//!<") || l.startsWith("//\"<")) {
                    name = l.substring(4);
                    String itemText = parsedTemplate.get(name).getTextComponent().getText().trim();
                    writer.write(line + LINEBREAK);
                    BufferedReader brTempl = new BufferedReader(new StringReader(itemText));
                    String lTempl;
                    while ((lTempl = brTempl.readLine()) != null) {
                        if (l.startsWith("//!<")) writer.write("\"" + lTempl.replace("\"", "\\\"") + "\\n\" + \n"); else writer.write("\"" + lTempl + "\" +\n");
                    }
                    writer.write("\"\"\n");
                    skip = true;
                } else if (!skip) {
                    line = line.replace("class " + templateName, "class " + className);
                    line = line.replace("public " + templateName, "public " + className);
                    line = line.replace("version = \"version\"", "version = \"" + Benojt.VERSION_STR + "\"");
                    writer.write(line + LINEBREAK);
                }
            }
            writer.flush();
        } catch (IOException ex) {
            res = null;
            ex.printStackTrace();
        } finally {
            if (writer != null) try {
                writer.close();
            } catch (IOException e) {
            }
            try {
                templateReader.close();
            } catch (IOException e) {
            }
        }
        return res;
    }

    /**
	 * invoke new iterator dialog, compile iterator, load new iterator.<BR>
	 * if javac cannot be found locate the compiler
	 * @param filledTemplate
	 * @param initIter 
	 */
    private String createIteratorFromTemplate(IteratorTemplateData filledTemplate, boolean initIter) {
        String errors = "";
        NewIteratorDlg niDlg = new NewIteratorDlg(this.fp, filledTemplate, "Create Iterator from " + filledTemplate.getTemplateName(), true);
        niDlg.setLocationRelativeTo(this.fp);
        boolean keepTrying = false;
        do {
            if (niDlg.showDlg() == BenojtDlg.CANCEL) return errors;
            filledTemplate.setClassName(niDlg.getName());
            keepTrying = !this.compileTemplate(filledTemplate, initIter);
        } while (keepTrying);
        return errors;
    }

    /**
	 * compiles an iterator from some filled template and loads it.
	 * @param filledTemplate
	 * @param initIter
	 * @return
	 */
    private boolean compileTemplate(IteratorTemplateData filledTemplate, boolean initIter) {
        String benojtClassPath = getClassPath();
        if (benojtClassPath == null) {
            JOptionPane.showMessageDialog(this.getFractalPanel(), "The class path of benojt could not be determined." + LINEBREAK + "Compilation is not possible.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        File localClassDir = getLocalClassDir();
        if (localClassDir == null) {
            JOptionPane.showMessageDialog(this.getFractalPanel(), "The Benojt class directory can not be accessed." + LINEBREAK + "Compilation is not possible.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        File sourceFile = this.createSourceFile(filledTemplate);
        if (sourceFile == null) {
            JOptionPane.showMessageDialog(this.getFractalPanel(), "The iterator source file could not be created.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        final JDialog actDlg = getMessageDialog(JOptionPane.getFrameForComponent(getFractalPanel()), "Compiling...");
        CompileThread ct = new CompileThread(localClassDir.getAbsolutePath(), benojtClassPath, sourceFile, getFractalPanel(), actDlg);
        ct.start();
        actDlg.setVisible(true);
        if (!ct.getSuccess()) return false;
        this.loadIteratorClass(localClassDir, filledTemplate.getClassName(), initIter);
        this.loadLocalIterators();
        return true;
    }

    /**
	 * detects the class path where the benojt classes are located.
	 * either local classes or in a jar file.
	 * when started with java web start. all relevant class files are 
	 * saved in benojtClasses. 
	 * @return
	 */
    private static String getClassPath() {
        String res = null;
        try {
            URL thisURL = IteratorManager.class.getClassLoader().getResource("");
            if (thisURL != null) {
                res = URLDecoder.decode(thisURL.getPath(), "UTF-8");
            } else {
                thisURL = IteratorManager.class.getClassLoader().getResource("net");
                if (thisURL.getProtocol().equals("jar")) {
                    JarURLConnection conn = (JarURLConnection) thisURL.openConnection();
                    JarFile jarFile = conn.getJarFile();
                    if (!conn.getJarFileURL().toString().startsWith("http")) {
                        res = jarFile.getName();
                    } else {
                        String benojtClasses = Context.getBenoitDir() + FILE_SEPERATOR + "benojtClasses";
                        File classDir = new File(benojtClasses);
                        if (!classDir.exists()) classDir.mkdir();
                        for (String entryName : downloadFiles) {
                            JarEntry jarEntry = jarFile.getJarEntry(entryName);
                            if (jarEntry == null) {
                                System.out.println("not exists " + entryName);
                                continue;
                            }
                            File f = new File(benojtClasses + FILE_SEPERATOR + entryName);
                            if (!entryName.endsWith(".class") && !f.exists()) {
                                f.mkdir();
                            } else if (!jarEntry.isDirectory() && entryName.endsWith(".class")) {
                                if (f.exists()) f.delete();
                                BufferedInputStream bis = new BufferedInputStream(jarFile.getInputStream(jarEntry));
                                FileOutputStream fos = new FileOutputStream(f);
                                byte[] buffer = new byte[(int) jarEntry.getSize()];
                                bis.read(buffer);
                                fos.write(buffer);
                                fos.flush();
                                fos.close();
                                bis.close();
                            }
                        }
                        res = classDir.getAbsolutePath();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("classPath: " + res);
        return res;
    }

    private static File getLocalClassDir() {
        File res = new File(Context.getBenoitDir() + FILE_SEPERATOR + CLASS_DIR);
        if (!res.exists()) {
            try {
                res.mkdirs();
                if (!res.canRead() | !res.canWrite()) res = null;
            } catch (SecurityException sex) {
                res = null;
            }
        }
        return res;
    }

    /**
	 * load some created iterator.
	 * @param classPath the path where the class file is located
	 * @param iteratorName the name of the iterator class
	 * @param initIter if true the loaded iterator is initialized
	 */
    private void loadIteratorClass(File classPath, String iteratorName, boolean initIter) {
        try {
            URLClassLoader loader1 = new URLClassLoader(new URL[] { classPath.toURI().toURL() }, this.getClass().getClassLoader());
            String className = ITERATOR_PACKAGE + "." + iteratorName;
            Class<?> cls1 = loader1.loadClass(className);
            this.getFractalPanel().setIterator(cls1.asSubclass(Iterator.class), initIter);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Sorry, could not be load iterator." + iteratorName, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
	 * create a borderles dialog that displays some message.
	 * @param component
	 * @param message
	 * @return
	 */
    private JDialog getMessageDialog(Component component, String message) {
        JDialog msgDlg = new JDialog(JOptionPane.getFrameForComponent(component), "", true);
        JPanel panel = new JPanel();
        JLabel label = new JLabel(message);
        label.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.add(label);
        msgDlg.getContentPane().add(panel);
        msgDlg.getRootPane().setBorder(BorderFactory.createRaisedBevelBorder());
        msgDlg.setContentPane(panel);
        msgDlg.setUndecorated(true);
        msgDlg.pack();
        msgDlg.setLocationRelativeTo(component);
        return msgDlg;
    }

    /**
	 * returns true if there is a stored last used template.
	 * @return true if somersed template exists last pa
	 */
    public boolean hasLastTemplate() {
        return lastParsedTemplate != null;
    }

    /**
	 * load/create/init iterator from xml file definition.
	 * @param className the name of the class
	 * @param isTemplate true if the iterator is fom template
	 * @param nodes the config is loaded from
	 * @return error string
	 */
    public String loadConfig(String className, boolean isTemplate, NodeList nodes) {
        String errors = "";
        String templateName = null;
        NodeList tItemNodes = null;
        for (int j = 0; j < nodes.getLength(); j++) {
            Node n = nodes.item(j);
            if (n.getNodeName().equals(XMLNode.XMLNodeTemplateItems)) {
                Node propName = n.getAttributes().getNamedItem(XMLNode.XMLNodeTemplateName);
                if (propName != null) templateName = propName.getNodeValue();
                tItemNodes = n.getChildNodes();
            }
        }
        if (!isTemplate) {
            try {
                Class<?> ic = this.getClass().getClassLoader().loadClass("net.benojt.iterator" + "." + className);
                this.fp.setIterator(ic.asSubclass(net.benojt.iterator.Iterator.class), false);
            } catch (Exception ex) {
                ex.printStackTrace();
                errors += "Could not set iterator.\n";
            }
        } else if (isTemplate && templateName != null && tItemNodes != null) {
            String tName = TEMPLATE_DIR + templateName + ".java";
            IteratorTemplateData iTempl = this.parseTemplate(tName);
            if (iTempl == null) return errors + "Could not read iterator template.\n";
            iTempl.setTemplateName(templateName);
            for (int j = 0; j < tItemNodes.getLength(); j++) {
                org.w3c.dom.Node n = tItemNodes.item(j);
                if (n.getNodeName().equals("item")) {
                    Node propName = n.getAttributes().getNamedItem("name");
                    String value = n.getTextContent();
                    StringBuffer res = new StringBuffer();
                    try {
                        BufferedReader br = new BufferedReader(new java.io.StringReader(value));
                        String line;
                        while ((line = br.readLine()) != null) {
                            String l = line;
                            int startPos = 0;
                            for (int i = 0; i < line.length() - 1; i++) if (l.charAt(startPos) == '\t') startPos++;
                            if (startPos > 0) l = l.substring(startPos, l.length());
                            res.append(l + "\n");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        res = new StringBuffer(value);
                    }
                    iTempl.put(propName.getNodeValue(), new TemplateItem(propName.getNodeValue(), res.toString()));
                }
            }
            iTempl.setClassName(className);
            if (this.compileTemplate(iTempl, false)) {
                this.initialize();
            } else {
                return errors + "Could not create iterator.\n";
            }
        }
        Iterator it = this.fp.getIterator();
        if (it instanceof UIModule) errors += ((UIModule) it).loadConfig(nodes);
        this.fp.markForRedraw();
        return errors;
    }

    /**
	 * creates a xml node for an iterator class from the templateItems array. 
	 * the iterator class should be created from a template.
	 * @param cls the iterator class
	 */
    public static XMLNode getNodeFromTemplate(Class<? extends Iterator> cls) {
        XMLNode templCont = null;
        try {
            String tName = getTemplateName(cls);
            String[][] tis = getTemplateItems(cls);
            if (tis.length > 0) {
                templCont = new XMLNode(new XMLTag(XMLNode.XMLNodeTemplateItems, XMLNode.XMLNodeTemplateName, tName));
                for (String[] ti : tis) if (ti.length == 2) {
                    templCont.addNode(new XMLTag("item", "name", ti[0]), ti[1]);
                }
            }
            return templCont;
        } catch (Exception ex) {
            System.out.println("could not write templateItems " + cls.getName());
            ex.printStackTrace();
        }
        return new XMLNode("null");
    }

    public static void showTemplateItems(Class<? extends Iterator> cls) {
        IteratorTemplateData templateItems = null;
        try {
            templateItems = getIteratorTemplate(cls);
            NewIteratorDlg dlg = new NewIteratorDlg(null, templateItems, "Iterator from template " + cls.getSimpleName(), false);
            dlg.setLocationRelativeTo(null);
            dlg.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Could not read template items from iterator.", "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
            return;
        }
    }

    /**
	 * read template name from static field.
	 * @param cls the iterator class to get the template name for
	 * @return the template name 
	 * @throws Exception some exception
	 */
    public static String getTemplateName(Class<? extends Iterator> cls) throws Exception {
        return (String) cls.getDeclaredField(XMLNode.XMLNodeTemplateName).get(null);
    }

    /**
	 * read the template items from static field.
	 * @param cls the iterator class to get the template items for
	 * @return the template items in an array
	 * @throws Exception some exception
	 */
    public static String[][] getTemplateItems(Class<? extends Iterator> cls) throws Exception {
        return (String[][]) cls.getDeclaredField("templateItems").get(null);
    }

    /**
	 * read items from class and create ItaratorTemplate.
	 * @param cls the iterator class
	 * @return template data
	 * @throws Exception some exception
	 */
    public static IteratorTemplateData getIteratorTemplate(Class<? extends Iterator> cls) throws Exception {
        IteratorTemplateData templateItems = new IteratorTemplateData();
        templateItems.setClassName(cls.getSimpleName());
        String tName = getTemplateName(cls);
        templateItems.setTemplateName(tName);
        templateItems.setTemplateLoc(TEMPLATE_DIR + tName + ".java");
        String[][] tis = getTemplateItems(cls);
        if (tis.length > 0) {
            for (String[] ti : tis) if (ti.length == 2) {
                templateItems.put(ti[0], new TemplateItem(ti[0], ti[1]));
            }
        }
        return templateItems;
    }
}
