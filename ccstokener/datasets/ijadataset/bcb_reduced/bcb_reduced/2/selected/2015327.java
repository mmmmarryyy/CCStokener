package ossobookupdater;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Date;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 *
 * @author Jojo
 */
public class Updater extends javax.swing.JFrame {

    private String updatePath = "http://ossobook.svn.sourceforge.net/svnroot/ossobook/trunk/update/";

    private int numberFiles = 0;

    /** Creates new form Updater */
    public Updater() {
        try {
            setTitle("OssoBook Updater");
            System.setProperty("java.net.preferIPv4Stack", "true");
            initComponents();
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            int posX = (screen.width / 2) - (640 / 2);
            int posY = (screen.height / 2) - (480 / 2);
            setLocation(posX, posY);
            jProgressBar1.setVisible(true);
            labelPercuentalProgress.setVisible(true);
            URL url = new URL(updatePath + "currentVersion.txt");
            URLConnection con = url.openConnection();
            con.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line;
            for (; (line = in.readLine()) != null; ) {
                numberFiles++;
            }
            labelFileProgress.setText("0/" + numberFiles);
            labelPercuentalProgress.setText("0%");
            jProgressBar2.setMaximum(numberFiles);
            URL url2 = new URL(updatePath + "Changelog.txt");
            URLConnection con2 = url2.openConnection();
            con2.connect();
            BufferedReader in2 = new BufferedReader(new InputStreamReader(con2.getInputStream()));
            jTextArea1.setMargin(new Insets(10, 10, 10, 10));
            Font f = new Font("Monospaced", Font.PLAIN, 12);
            jTextArea1.setFont(f);
            for (; (line = in2.readLine()) != null; ) {
                jTextArea1.setText(jTextArea1.getText() + line + "\n");
            }
        } catch (IOException ex) {
            Logger.getLogger(Updater.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void update() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    jButton1.setEnabled(false);
                    jButton2.setEnabled(false);
                    URL url = new URL(updatePath + "currentVersion.txt");
                    URLConnection con = url.openConnection();
                    con.connect();
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String line;
                    for (int i = 0; (line = in.readLine()) != null; i++) {
                        URL fileUrl = new URL(updatePath + line);
                        URLConnection filecon = fileUrl.openConnection();
                        InputStream stream = fileUrl.openStream();
                        int oneChar, count = 0;
                        int size = filecon.getContentLength();
                        jProgressBar1.setMaximum(size);
                        jProgressBar1.setValue(0);
                        File testFile = new File(line);
                        String build = "";
                        for (String dirtest : line.split("/")) {
                            build += dirtest;
                            if (!build.contains(".")) {
                                File dirfile = new File(build);
                                if (!dirfile.exists()) {
                                    dirfile.mkdir();
                                }
                            }
                            build += "/";
                        }
                        if (testFile.length() == size) {
                        } else {
                            transferFile(line, fileUrl, size);
                            if (line.endsWith("documents.zip")) {
                                ZipInputStream in2 = new ZipInputStream(new FileInputStream(line));
                                ZipEntry entry;
                                String pathDoc = line.split("documents.zip")[0];
                                File docDir = new File(pathDoc + "documents");
                                if (!docDir.exists()) {
                                    docDir.mkdir();
                                }
                                while ((entry = in2.getNextEntry()) != null) {
                                    String outFilename = pathDoc + "documents/" + entry.getName();
                                    OutputStream out = new BufferedOutputStream(new FileOutputStream(outFilename));
                                    byte[] buf = new byte[1024];
                                    int len;
                                    while ((len = in2.read(buf)) > 0) {
                                        out.write(buf, 0, len);
                                    }
                                    out.close();
                                }
                                in2.close();
                            }
                            if (line.endsWith("mysql.zip")) {
                                ZipFile zipfile = new ZipFile(line);
                                Enumeration entries = zipfile.entries();
                                String pathDoc = line.split("mysql.zip")[0];
                                File docDir = new File(pathDoc + "mysql");
                                if (!docDir.exists()) {
                                    docDir.mkdir();
                                }
                                while (entries.hasMoreElements()) {
                                    ZipEntry entry = (ZipEntry) entries.nextElement();
                                    if (entry.isDirectory()) {
                                        System.err.println("Extracting directory: " + entry.getName());
                                        (new File(pathDoc + "mysql/" + entry.getName())).mkdir();
                                        continue;
                                    }
                                    System.err.println("Extracting file: " + entry.getName());
                                    InputStream in2 = zipfile.getInputStream(entry);
                                    OutputStream out = new BufferedOutputStream(new FileOutputStream(pathDoc + "mysql/" + entry.getName()));
                                    byte[] buf = new byte[1024];
                                    int len;
                                    while ((len = in2.read(buf)) > 0) {
                                        out.write(buf, 0, len);
                                    }
                                    in2.close();
                                    out.close();
                                }
                            }
                        }
                        jProgressBar2.setValue(i + 1);
                        labelFileProgress.setText((i + 1) + "/" + numberFiles);
                    }
                    labelStatus.setText("Update Finished");
                    jButton1.setVisible(false);
                    jButton2.setText("Finished");
                    jButton1.setEnabled(true);
                    jButton2.setEnabled(true);
                } catch (IOException ex) {
                    Logger.getLogger(Updater.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }).start();
    }

    private void transferFile(String line, URL fileUrl, int size) throws FileNotFoundException, IOException {
        FileOutputStream fos = new FileOutputStream(line);
        ReadableByteChannel rbc = Channels.newChannel(fileUrl.openStream());
        labelStatus.setText("Writing File: " + line);
        if (line.contains("/")) {
            int end = line.lastIndexOf("/");
            String directory = line.substring(0, end);
            File file = new File(directory);
            if (!file.exists()) {
                file.mkdirs();
            }
        }
        Date date = new Date();
        byte[] buffer = new byte[1024];
        int bytesRead = 0;
        int count = 0;
        try {
            BufferedInputStream in = new BufferedInputStream(fileUrl.openStream());
            while ((bytesRead = in.read(buffer, 0, buffer.length)) != -1) {
                fos.write(buffer, 0, bytesRead);
                count += bytesRead;
                jProgressBar1.setValue(count);
                labelPercuentalProgress.setText((count * 100 / size) + "%");
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
            transferFile(line, fileUrl, size);
        }
        Date date2 = new Date();
        fos.close();
        rbc.close();
        File check = new File(line);
        if (check.length() != size) {
            System.out.println("error: " + line);
            transferFile(line, fileUrl, size);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jProgressBar1 = new javax.swing.JProgressBar();
        jProgressBar2 = new javax.swing.JProgressBar();
        labelFileProgress = new javax.swing.JLabel();
        labelPercuentalProgress = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        labelStatus = new javax.swing.JLabel();
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(null);
        setResizable(false);
        jButton1.setText("Updaten");
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jButton2.setText("Close");
        jButton2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 620, Short.MAX_VALUE).addGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(jProgressBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(layout.createSequentialGroup().addComponent(jButton1).addGap(18, 18, 18).addComponent(jButton2)).addComponent(jProgressBar2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(labelStatus, javax.swing.GroupLayout.DEFAULT_SIZE, 460, Short.MAX_VALUE).addComponent(labelPercuentalProgress, javax.swing.GroupLayout.DEFAULT_SIZE, 460, Short.MAX_VALUE).addComponent(labelFileProgress, javax.swing.GroupLayout.DEFAULT_SIZE, 460, Short.MAX_VALUE)))).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 388, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(jProgressBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 15, Short.MAX_VALUE).addComponent(labelStatus, javax.swing.GroupLayout.DEFAULT_SIZE, 15, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(jProgressBar2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jButton1).addComponent(jButton2))).addGroup(layout.createSequentialGroup().addComponent(labelFileProgress, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(labelPercuentalProgress, javax.swing.GroupLayout.DEFAULT_SIZE, 18, Short.MAX_VALUE))).addContainerGap()));
        pack();
    }

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
        System.exit(0);
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        update();
    }

    private javax.swing.JButton jButton1;

    private javax.swing.JButton jButton2;

    private javax.swing.JProgressBar jProgressBar1;

    private javax.swing.JProgressBar jProgressBar2;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JTextArea jTextArea1;

    private javax.swing.JLabel labelFileProgress;

    private javax.swing.JLabel labelPercuentalProgress;

    private javax.swing.JLabel labelStatus;
}
