package org.opensourcephysics.tools;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import org.opensourcephysics.controls.*;
import org.opensourcephysics.display.OSPRuntime;

/**
 * This modal dialog lets the user choose launchable classes from jar files.
 */
public class LaunchClassChooser extends JDialog {

    private static Pattern pattern;

    private static Matcher matcher;

    private static Map classMaps = new TreeMap();

    protected static boolean jarsOnly = true;

    private JTextField searchField;

    private String defaultSearch = "";

    private String currentSearch = defaultSearch;

    private JScrollPane scroller;

    private JList choices;

    private LaunchableClassMap classMap;

    private boolean applyChanges = false;

    private JButton okButton;

    /**
   * Constructs an empty LaunchClassChooser dialog.
   *
   * @param owner the component that owns the dialog (may be null)
   */
    public LaunchClassChooser(Component owner) {
        super(JOptionPane.getFrameForComponent(owner), true);
        setTitle(LaunchRes.getString("ClassChooser.Frame.Title"));
        JLabel textLabel = new JLabel(LaunchRes.getString("ClassChooser.Search.Label") + " ");
        okButton = new JButton(LaunchRes.getString("ClassChooser.Button.Accept"));
        okButton.setEnabled(false);
        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                applyChanges = true;
                setVisible(false);
            }
        });
        JButton cancelButton = new JButton(LaunchRes.getString("ClassChooser.Button.Cancel"));
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        searchField = new JTextField(defaultSearch);
        searchField.addKeyListener(new KeyAdapter() {

            public void keyReleased(KeyEvent e) {
                Object obj = choices.getSelectedValue();
                search();
                choices.setSelectedValue(obj, true);
            }
        });
        getRootPane().setDefaultButton(okButton);
        JPanel headerPane = new JPanel();
        headerPane.setLayout(new BoxLayout(headerPane, BoxLayout.X_AXIS));
        headerPane.add(textLabel);
        headerPane.add(Box.createHorizontalGlue());
        headerPane.add(searchField);
        headerPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        JPanel scrollPane = new JPanel(new BorderLayout());
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(cancelButton);
        Container contentPane = getContentPane();
        contentPane.add(headerPane, BorderLayout.NORTH);
        contentPane.add(scrollPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.SOUTH);
        scroller = new JScrollPane();
        scroller.setPreferredSize(new Dimension(400, 300));
        scrollPane.add(scroller, BorderLayout.CENTER);
        pack();
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (dim.width - this.getBounds().width) / 2;
        int y = (dim.height - this.getBounds().height) / 2;
        setLocation(x, y);
    }

    /**
   * Sets the path to be searched. The path must be a set of jar file names
   * separated by semicolons unless jarsOnly is set to false.
   *
   * @param path the search path
   * @return true if at least one jar file was successfully loaded
   */
    public boolean setPath(String path) {
        String[] jarNames = parsePath(path);
        classMap = null;
        if (jarNames == null || jarNames.length == 0) {
            return false;
        }
        String key = "";
        for (int i = 0; i < jarNames.length; i++) {
            if (!key.equals("")) {
                key += ";";
            }
            key += jarNames[i];
        }
        classMap = (LaunchableClassMap) classMaps.get(key);
        if (classMap == null) {
            classMap = new LaunchableClassMap(jarNames);
            classMaps.put(key, classMap);
        }
        return true;
    }

    /**
   * Determines if the specified path is loaded. This will return true
   * only if the path is one or more jar files all of which are loaded.
   *
   * @param path the path
   * @return true if all jars in the path are loaded
   */
    public boolean isLoaded(String path) {
        if (classMap == null) {
            return false;
        }
        String[] jarNames = parsePath(path);
        for (int i = 0; i < jarNames.length; i++) {
            if (!classMap.includesJar(jarNames[i])) {
                return false;
            }
        }
        return true;
    }

    /**
   * Chooses a launchable class and assigns it to the specified launch node.
   *
   * @param node the node
   * @return true if the class assignment is approved
   */
    public boolean chooseClassFor(LaunchNode node) {
        search();
        choices.setSelectedValue(node.launchClassName, true);
        applyChanges = false;
        setVisible(true);
        if (!applyChanges) {
            return false;
        }
        Object obj = choices.getSelectedValue();
        if (obj == null) {
            return false;
        }
        String className = obj.toString();
        node.launchClass = (Class) classMap.get(className);
        node.launchClassName = className;
        return true;
    }

    /**
   * Gets the class with the given name in the current class map.
   *
   * @param className the class name
   * @return the Class object, or null if not found
   */
    public Class getClass(String className) {
        if (classMap == null) {
            return null;
        }
        return classMap.getClass(className);
    }

    /**
   * Gets the class with the given name in the specified path.
   *
   * @param classPath the path
   * @param className the class name
   * @return the Class object, or null if not found
   */
    public static Class getClass(String classPath, String className) {
        if (classPath == null || className == null) {
            return null;
        }
        String[] jarNames = parsePath(classPath);
        LaunchableClassMap classMap = getClassMap(jarNames);
        return classMap.getClass(className);
    }

    /**
   * Gets the class with the given name in the specified path.
   *
   * @param classPath the path
   * @return the ClassLoader object, or null if not found
   */
    public static ClassLoader getClassLoader(String classPath) {
        if (classPath == null || classPath.equals("")) {
            return null;
        }
        String[] jarNames = parsePath(classPath);
        LaunchableClassMap classMap = getClassMap(jarNames);
        return classMap.classLoader;
    }

    /**
   * Gets the launchable class map for the specified jar name array.
   *
   * @param jarNames the string array of jar names
   * @return the class map
   */
    private static LaunchableClassMap getClassMap(String[] jarNames) {
        String key = "";
        for (int i = 0; i < jarNames.length; i++) {
            if (!key.equals("")) {
                key += ";";
            }
            key += jarNames[i];
        }
        LaunchableClassMap classMap = (LaunchableClassMap) classMaps.get(key);
        if (classMap == null) {
            classMap = new LaunchableClassMap(jarNames);
            classMaps.put(key, classMap);
        }
        return classMap;
    }

    /**
   * Searches using the current search field text.
   */
    private void search() {
        if (classMap == null) {
            return;
        }
        classMap.loadAllLaunchables();
        if (search(searchField.getText())) {
            currentSearch = searchField.getText();
            searchField.setBackground(Color.white);
        } else {
            JOptionPane.showMessageDialog(this, LaunchRes.getString("Dialog.InvalidRegex.Message") + " \"" + searchField.getText() + "\"", LaunchRes.getString("Dialog.InvalidRegex.Title"), JOptionPane.WARNING_MESSAGE);
            searchField.setText(currentSearch);
        }
    }

    /**
   * Searches for class names using a regular expression string
   * and puts matches into the class chooser list of choices.
   *
   * @param regex the regular expression
   * @return true if the search succeeded (even if no matches found)
   */
    private boolean search(String regex) {
        regex = regex.toLowerCase();
        okButton.setEnabled(false);
        try {
            pattern = Pattern.compile(regex);
        } catch (Exception ex) {
            return false;
        }
        ArrayList matches = new ArrayList();
        for (Iterator it = classMap.keySet().iterator(); it.hasNext(); ) {
            String name = (String) it.next();
            matcher = pattern.matcher(name.toLowerCase());
            if (matcher.find()) {
                matches.add(name);
            }
        }
        Object[] results = matches.toArray();
        choices = new JList(results);
        choices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        choices.setFont(searchField.getFont());
        choices.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                JList theList = (JList) e.getSource();
                okButton.setEnabled(!theList.isSelectionEmpty());
            }
        });
        choices.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                JList theList = (JList) e.getSource();
                if (e.getClickCount() == 2 && !theList.isSelectionEmpty()) {
                    okButton.doClick();
                }
            }
        });
        scroller.getViewport().setView(choices);
        return true;
    }

    /**
   * Parses the specified path into path tokens (at semicolons).
   *
   * @param path the path
   * @param jarsOnly true if only ".jar" names are returned
   * @return an array of path names
   */
    static String[] parsePath(String path) {
        return parsePath(path, jarsOnly);
    }

    /**
   * Parses the specified path into path tokens (at semicolons).
   *
   * @param path the path
   * @param jarsOnly true if only ".jar" names are returned
   * @return an array of path names
   */
    static String[] parsePath(String path, boolean jarsOnly) {
        Collection tokens = new ArrayList();
        String next = path;
        int i = path.indexOf(";");
        if (i != -1) {
            next = path.substring(0, i);
            path = path.substring(i + 1);
        } else {
            path = "";
        }
        while (next.length() > 0) {
            if (!jarsOnly || next.endsWith(".jar")) {
                tokens.add(next);
            }
            i = path.indexOf(";");
            if (i == -1) {
                next = path.trim();
                path = "";
            } else {
                next = path.substring(0, i).trim();
                path = path.substring(i + 1).trim();
            }
        }
        return (String[]) tokens.toArray(new String[0]);
    }
}

/**
 * A map of jar/class name to launchable Class object.
 * The name of the jar is prepended to the class name.
 */
class LaunchableClassMap extends TreeMap {

    ClassLoader classLoader;

    String[] jarNames;

    boolean allLoaded = false;

    LaunchableClassMap(String[] jarNames) {
        this.jarNames = jarNames;
        Collection urls = new ArrayList();
        String jarBase = OSPRuntime.getLaunchJarDirectory();
        for (int i = 0; i < jarNames.length; i++) {
            String jarPath = XML.getResolvedPath(jarNames[i], jarBase);
            try {
                urls.add(new URL("file:" + jarPath));
            } catch (MalformedURLException ex) {
                OSPLog.info(ex + " " + jarPath);
            }
        }
        classLoader = URLClassLoader.newInstance((URL[]) urls.toArray(new URL[0]));
    }

    /**
   * Loads a class from the URLClassLoader or, if this fails, from the
   * current class loader.
   *
   * @param name the class name
   * @return the Class
   * @throws ClassNotFoundException
   */
    Class smartLoadClass(String name) throws ClassNotFoundException {
        try {
            return classLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            return this.getClass().getClassLoader().loadClass(name);
        }
    }

    void loadAllLaunchables() {
        if (allLoaded) {
            return;
        }
        JApplet applet = org.opensourcephysics.display.OSPRuntime.applet;
        for (int i = 0; i < jarNames.length; i++) {
            JarFile jar = null;
            try {
                if (applet == null) {
                    jar = new JarFile(jarNames[i]);
                } else {
                    String path = XML.getResolvedPath(jarNames[i], applet.getCodeBase().toExternalForm());
                    URL url = new URL("jar:" + path + "!/");
                    JarURLConnection conn = (JarURLConnection) url.openConnection();
                    jar = conn.getJarFile();
                }
            } catch (IOException ex) {
                OSPLog.info(ex.getClass().getName() + ": " + ex.getMessage());
            } catch (SecurityException ex) {
                OSPLog.info(ex.getClass().getName() + ": " + ex.getMessage());
            }
            if (jar == null) {
                continue;
            }
            for (Enumeration e = jar.entries(); e.hasMoreElements(); ) {
                JarEntry entry = (JarEntry) e.nextElement();
                String name = entry.getName();
                if (name.endsWith(".class") && name.indexOf("$") == -1) {
                    name = name.substring(0, name.indexOf(".class"));
                    int j = name.indexOf("/");
                    while (j != -1) {
                        name = name.substring(0, j) + "." + name.substring(j + 1);
                        j = name.indexOf("/");
                    }
                    if (get(name) != null) {
                        continue;
                    }
                    try {
                        Class next = smartLoadClass(name);
                        if (Launcher.isLaunchable(next)) {
                            put(name, next);
                        }
                    } catch (ClassNotFoundException ex) {
                    } catch (NoClassDefFoundError err) {
                        OSPLog.info(err.toString());
                    }
                }
            }
        }
        allLoaded = true;
    }

    boolean includesJar(String jarName) {
        for (int i = 0; i < jarNames.length; i++) {
            if (jarNames[i].equals(jarName)) {
                return true;
            }
        }
        return false;
    }

    Class getClass(String className) {
        Class type = (Class) get(className);
        if (type != null || allLoaded) {
            return type;
        }
        try {
            type = smartLoadClass(className);
            if (Launcher.isLaunchable(type)) {
                return type;
            }
        } catch (ClassNotFoundException ex) {
        } catch (NoClassDefFoundError err) {
            OSPLog.info(err.toString());
        }
        return null;
    }
}
