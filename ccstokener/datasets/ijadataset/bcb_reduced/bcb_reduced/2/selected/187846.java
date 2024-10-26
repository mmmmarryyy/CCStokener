package com.mycila.maven.ui;

import com.google.code.xmltool.XMLDoc;
import com.google.code.xmltool.XMLDocument;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.util.Properties;

/**
 * @author Mathieu Carbou - mathieu.carbou(at)gmail.com
 */
public final class MavenDeployer {

    private static File currentDir = new File(".");

    private static Deployer deployer;

    private static File pom;

    public static void main(String[] args) {
        final MavenDeployerGui gui = new MavenDeployerGui();
        final Chooser repositoryChooser = new Chooser(gui.formPanel, JFileChooser.DIRECTORIES_ONLY);
        final Chooser artifactChooser = new Chooser(gui.formPanel, JFileChooser.FILES_ONLY);
        final Chooser pomChooser = new Chooser(gui.formPanel, JFileChooser.FILES_ONLY, new POMFilter());
        gui.cancel.setEnabled(false);
        gui.cbDeployPOM.setVisible(false);
        gui.cbDeployPOM.setEnabled(false);
        gui.mavenBin.setText(findMavenExecutable());
        gui.repositoryBrowser.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                File repo = repositoryChooser.chooseFrom(currentDir);
                if (repo != null) {
                    currentDir = repositoryChooser.currentFolder;
                    gui.repositoryURL.setText(repo.getAbsolutePath());
                }
            }
        });
        gui.artifactBrowser.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                File artifact = artifactChooser.chooseFrom(currentDir);
                if (artifact != null) {
                    currentDir = artifactChooser.currentFolder;
                    gui.artifactFile.setText(artifact.getAbsolutePath());
                }
            }
        });
        gui.deploy.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                deployer = new Deployer(gui, pom);
                deployer.execute();
            }
        });
        gui.clear.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                gui.console.setText("");
            }
        });
        gui.cancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (deployer != null) {
                    deployer.stop();
                    deployer = null;
                }
            }
        });
        gui.cbDeployPOM.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                readPOM(gui);
            }
        });
        gui.loadPOM.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                pom = pomChooser.chooseFrom(currentDir);
                if (pom != null) {
                    currentDir = pomChooser.currentFolder;
                    readPOM(gui);
                    gui.cbDeployPOM.setText("Deploy also " + pom.getAbsolutePath());
                    gui.cbDeployPOM.setEnabled(true);
                    gui.cbDeployPOM.setVisible(true);
                }
            }
        });
        String version = "";
        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource("META-INF/maven/com.mycila.maven/maven-deployer/pom.properties");
            Properties p = new Properties();
            p.load(url.openStream());
            version = " " + p.getProperty("version");
        } catch (Exception ignored) {
            version = " x.y";
        }
        JFrame frame = new JFrame("Maven Deployer" + version + " - By Mathieu Carbou (http://blog.mycila.com)");
        frame.setContentPane(gui.formPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLocationByPlatform(true);
        frame.pack();
        frame.setVisible(true);
    }

    private static String findMavenExecutable() {
        String script = "mvn";
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            script += ".bat";
        }
        for (String path : System.getenv("PATH").split(File.pathSeparator)) {
            File file = new File(path, script);
            if (file.exists() && file.isFile() && file.canRead()) {
                return file.getAbsolutePath();
            }
        }
        return "";
    }

    private static void readPOM(MavenDeployerGui gui) {
        try {
            XMLDocument doc = XMLDoc.from(pom).gotoRoot();
            String ns = doc.getPefix("http://maven.apache.org/POM/4.0.0");
            if (ns.length() > 0) ns += ":";
            if (doc.hasTag("%1$sgroupId", ns)) gui.groupId.setText(doc.getText("%1$sgroupId", ns));
            if (doc.hasTag("%1$sartifactId", ns)) gui.artifactId.setText(doc.getText("%1$sartifactId", ns));
            if (doc.hasTag("%1$sversion", ns)) gui.version.setText(doc.getText("%1$sversion", ns));
            if (doc.hasTag("%1$spackaging", ns)) gui.packaging.setSelectedItem(doc.getText("%1$spackaging", ns));
            if (doc.hasTag("%1$sdescription", ns)) gui.description.setText(doc.getText("%1$sdescription", ns));
            if (doc.hasTag("%1$sdistributionManagement/%1$srepository/%1$surl", ns)) gui.repositoryURL.setText(doc.getText("%1$sdistributionManagement/%1$srepository/%1$surl", ns));
            if (doc.hasTag("%1$sdistributionManagement/%1$srepository/%1$sid", ns)) gui.repositoryID.setText(doc.getText("%1$sdistributionManagement/%1$srepository/%1$sid", ns));
        } catch (Exception ee) {
            gui.console.setText(ExceptionUtils.asText(ee));
        }
    }
}
