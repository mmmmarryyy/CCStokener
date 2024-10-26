package org.microemu;

import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

/**
 * @author vlads
 * 
 */
public class WebStart extends JFrame {

    private static final long serialVersionUID = 1L;

    JTextArea tx;

    String lastURL = "http://localhost:8080/bytecode-webstart/more/bytecode-test-app-0.0.1.jar";

    File recentDirectory;

    File recentJar;

    public WebStart() throws HeadlessException {
        super("ME2 bytecode Preporcessor");
        Dimension size = new Dimension(400, 300);
        setSize(size);
        setLocation(300, 300);
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        JMenu menuFile = new JMenu("File");
        menuBar.add(menuFile);
        JMenuItem menuStart = new JMenuItem("Start Internal app");
        menuStart.addActionListener(new MenuStartInternalListener());
        menuFile.add(menuStart);
        JMenuItem menuOpenJARFile = new JMenuItem("Open JAR File...");
        menuOpenJARFile.addActionListener(new MenuOpenJARFileListener());
        menuFile.add(menuOpenJARFile);
        JMenuItem menuOpenJARFileFaset = new JMenuItem("Re-open JAR");
        menuOpenJARFileFaset.addActionListener(new MenuOpenJARFileFastListener());
        menuFile.add(menuOpenJARFileFaset);
        JMenuItem menuOpenJARURL = new JMenuItem("Open JAR URL...");
        menuOpenJARURL.addActionListener(new MenuOpenJARURLListener());
        menuFile.add(menuOpenJARURL);
        getContentPane().add(tx = new JTextArea(), "Center");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private class MenuStartInternalListener extends AbstractAction {

        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent event) {
            tx.setText("Start Internal app...\n");
            runApp(null);
        }
    }

    private class MenuOpenJARFileListener extends AbstractAction {

        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent ev) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Open JAR File...");
            if (recentDirectory == null) {
                recentDirectory = new File(new File(new File("..").getAbsoluteFile(), "bytecode-test-app"), "target");
            }
            fileChooser.setCurrentDirectory(recentDirectory);
            int returnVal = fileChooser.showOpenDialog(WebStart.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                try {
                    recentDirectory = fileChooser.getCurrentDirectory().getAbsoluteFile();
                    tx.setText("");
                    recentJar = fileChooser.getSelectedFile();
                    openJar(recentJar.toURI().toURL());
                } catch (Throwable e) {
                    System.err.println("Cannot load " + fileChooser.getSelectedFile().getName());
                }
            }
        }
    }

    ;

    private class MenuOpenJARFileFastListener extends AbstractAction {

        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent ev) {
            try {
                tx.setText("");
                if (recentJar == null) {
                    File dir = new File(new File(new File("..").getAbsoluteFile(), "bytecode-test-app"), "target").getCanonicalFile();
                    recentJar = new File(dir.getAbsoluteFile(), "bytecode-test-app-0.0.1.jar");
                }
                openJar(recentJar.toURI().toURL());
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    ;

    private class MenuOpenJARURLListener extends AbstractAction {

        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent ev) {
            try {
                String entered = JOptionPane.showInputDialog(WebStart.this, "Enter JAR URL:", lastURL);
                if (lastURL != null) {
                    tx.setText("");
                    lastURL = entered;
                    openJar(new URL(lastURL));
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    ;

    private void openJar(URL path) {
        println("Open [" + path + "]");
        runApp(path);
    }

    private void println(String text) {
        System.out.println(text);
        tx.append(text);
        tx.append("\n");
    }

    public void runApp(URL path) {
        println("Start Class loader test...");
        println("ClassLoader " + this.getClass().getClassLoader().hashCode() + " WebStart");
        try {
            SystemProperties.setProperty("microedition.platform", "MicroEmulator-Test");
            ClassLoader parent = this.getClass().getClassLoader();
            if (path == null) {
                path = PreporcessorClassLoader.getClassURL(parent, PreporcessorTest.TEST_CLASS);
            } else {
                InputStream is = path.openStream();
                try {
                    is.read();
                } finally {
                    is.close();
                }
                println("Read OK " + path);
            }
            URL[] urls = new URL[] { path };
            PreporcessorClassLoader cl = new PreporcessorClassLoader(urls, parent);
            cl.disableClassLoad(SystemProperties.class);
            cl.disableClassLoad(ResourceLoader.class);
            ResourceLoader.classLoader = cl;
            println("ClassLoader " + cl.hashCode() + " <-- PreporcessorClassLoader");
            println("ClassLoader created...");
            acessResource(cl, PreporcessorClassLoader.getClassResourceName(PreporcessorTest.TEST_CLASS));
            acessResource(cl, "org/TestResourceLoad.class");
            acessResource(cl, PreporcessorClassLoader.getClassResourceName(this.getClass().getName()));
            acessResource(cl, "app-data.txt");
            Class instrumentedClass = cl.loadClass(PreporcessorTest.TEST_CLASS);
            Runnable instrumentedInstance = (Runnable) instrumentedClass.newInstance();
            instrumentedInstance.run();
            println("Looks good!\n");
        } catch (Throwable e) {
            println("Error " + e.toString());
            e.printStackTrace();
        }
    }

    void acessResource(ClassLoader cl, String resource) throws IOException {
        URL url = cl.getResource(resource);
        if (url == null) {
            println("Ups can't find resource " + resource);
        } else {
            println("URL OK " + resource + " ->" + url);
            InputStream is = url.openStream();
            try {
                is.read();
            } finally {
                is.close();
            }
            println("Read OK " + resource + " ->" + url);
        }
    }

    public static void main(String[] args) {
        WebStart app = new WebStart();
        app.validate();
        app.setVisible(true);
    }
}
