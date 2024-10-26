package com.trivergia.intouch3.installer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.UIManager;

public class Installer {

    static boolean proceedFrom1 = false;

    public static final String UPDATE_URL = "http://trivergia.com:8080/convergiaupdates.properties";

    /**
	 * @param args
	 */
    public static void main(String[] args) throws Throwable {
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            JFrame frame = new JFrame();
            frame.setSize(650, 500);
            frame.setLocationRelativeTo(null);
            frame.add(new JLabel("<html>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Your computer is not running Windows. To use the installer,<br/>" + "your computer needs to be running windows. However, you can still use<br/>" + "Convergia, but you will have to manually install it. for instructions<br/>" + "on how to manually install Convergia, visit http://convergia.sf.net/manualinstall.html"));
            frame.show();
            frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
            return;
        }
        Frame0 frame0 = new Frame0();
        frame0.setDefaultCloseOperation(frame0.DO_NOTHING_ON_CLOSE);
        frame0.setLocationRelativeTo(null);
        frame0.show();
        new JFileChooser();
        frame0.dispose();
        final Frame1 frame1 = new Frame1();
        frame1.setLocationRelativeTo(null);
        frame1.setDefaultCloseOperation(frame1.EXIT_ON_CLOSE);
        frame1.show();
        frame1.getInstallButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (frame1.getInstallFile() == null) JOptionPane.showMessageDialog(frame1, "You must select a folder to install to first."); else if (!frame1.getInstallFile().exists() && !frame1.getInstallFile().mkdirs()) JOptionPane.showMessageDialog(frame1, "The folder you selected didn't exist, but it could not be created."); else if (!frame1.getInstallFile().canRead()) JOptionPane.showMessageDialog(frame1, "That folder can't be read. Please choose another folder."); else if (!frame1.getInstallFile().canWrite()) JOptionPane.showMessageDialog(frame1, "That folder can't be written to. Please choose another folder."); else {
                    frame1.dispose();
                    proceedFrom1 = true;
                }
            }
        });
        while (!proceedFrom1) {
            Thread.sleep(500);
        }
        Frame2 frame2 = new Frame2();
        frame2.setLocationRelativeTo(null);
        frame2.setDefaultCloseOperation(frame2.DO_NOTHING_ON_CLOSE);
        frame2.show();
        System.out.println("user chose " + frame1.getInstallFile());
        File installTo = frame1.getInstallFile();
        URL updateUrl = new URL(UPDATE_URL);
        InputStream in;
        try {
            in = updateUrl.openStream();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame2, "You are not connected to the internet. Connect " + "to the internet, and then run the installer again.");
            System.exit(0);
            return;
        }
        Properties p = new Properties();
        p.load(in);
        int versionindex = Integer.parseInt(p.getProperty("versionindex"));
        URL url = new URL(p.getProperty("url"));
        frame2.getStatusLabel().setText("Convergia Installer is downloading Convergia, please wait...");
        File jarfile = File.createTempFile("cvginstall", ".jar");
        System.out.println("jarfile is " + jarfile.getAbsolutePath());
        jarfile.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(jarfile);
        byte[] buffer = new byte[4096];
        int amount;
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.connect();
        int filesize = con.getContentLength();
        int amountRead = 0;
        if (filesize != -1) {
            frame2.getProgressBar().setIndeterminate(false);
            frame2.getProgressBar().setMinimum(0);
            frame2.getProgressBar().setMaximum(filesize / 1024);
            frame2.getProgressBar().setValue(0);
        }
        in = con.getInputStream();
        while ((amount = in.read(buffer)) != -1) {
            fos.write(buffer, 0, amount);
            amountRead += amount;
            frame2.getProgressBar().setValue(amountRead / 1024);
        }
        fos.flush();
        fos.close();
        in.close();
        System.out.println("downloaded");
        frame2.getStatusLabel().setText("Convergia Installer is installing Convergia, please wait...");
        installTo.mkdirs();
        System.out.println("about to extract");
        extractUpdates(jarfile, installTo, frame2.getProgressBar());
        System.out.println("finished extracting");
        String installPath = installTo.getCanonicalPath();
        if (installPath.endsWith("/") || installPath.endsWith("\\")) installPath = installPath.substring(0, installPath.length() - 1);
        System.out.println("install path is " + installPath);
        String runCommand = readFile(new File(installTo, "windowsrunscript"));
        runCommand = runCommand.replace("%INSTALLDIR%", installPath);
        writeFile(runCommand, new File(System.getProperty("user.home"), "Desktop/Convergia.bat"));
        frame2.dispose();
        Frame3 frame3 = new Frame3();
        frame3.setLocationRelativeTo(null);
        frame3.show();
        frame3.setDefaultCloseOperation(frame3.EXIT_ON_CLOSE);
    }

    static String readFile(File file) {
        try {
            if (file.length() > (5 * 1000 * 1000)) throw new RuntimeException("the file is " + file.length() + " bytes. that is too large. it can't be larger than 5000000 bytes.");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FileInputStream fis = new FileInputStream(file);
            copy(fis, baos);
            fis.close();
            baos.flush();
            baos.close();
            return new String(baos.toByteArray(), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void writeFile(String string, File file) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(string.getBytes("UTF-8"));
            FileOutputStream fos = new FileOutputStream(file);
            copy(bais, fos);
            bais.close();
            fos.flush();
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int amount;
        while ((amount = in.read(buffer)) != -1) {
            out.write(buffer, 0, amount);
        }
    }

    public static void extractUpdates(File updatejar, File dest, JProgressBar bar) {
        try {
            bar.setIndeterminate(false);
            bar.setStringPainted(true);
            System.out.println("loading jar file");
            JarFile file = new JarFile(updatejar);
            System.out.println("about to extract contents");
            Collection<JarEntry> entrylist = Collections.list(file.entries());
            bar.setMinimum(0);
            bar.setValue(0);
            bar.setMaximum(entrylist.size());
            byte[] buffer = new byte[4096];
            int amount;
            int i = 0;
            for (JarEntry entry : entrylist) {
                bar.setValue(i++);
                bar.setString(entry.getName());
                System.out.println("extracting entry " + entry.getName());
                File targetFile = new File(dest, entry.getName());
                if (!entry.getName().startsWith("appdata")) targetFile.getAbsoluteFile().getParentFile().mkdirs(); else System.out.println("entry was appdata, so don't extract");
                if (!entry.isDirectory() && !entry.getName().startsWith("appdata")) {
                    System.out.println("entry is a file.");
                    InputStream stream = file.getInputStream(entry);
                    FileOutputStream fos = new FileOutputStream(targetFile);
                    while ((amount = stream.read(buffer)) != -1) {
                        fos.write(buffer, 0, amount);
                    }
                    fos.flush();
                    fos.close();
                    stream.close();
                }
                System.out.println("extracted entry successfully.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("successfully extracted jar file.");
    }
}
