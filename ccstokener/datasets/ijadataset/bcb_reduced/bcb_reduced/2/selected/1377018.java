package reports.utility;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *
 * @author  Administrator
 */
public class LogFileHandler extends javax.swing.JDialog {

    /** Creates new form LogFileHandler */
    static String filePath;

    public javax.swing.Timer timer = null;

    public boolean status = false;

    public LogFileHandler(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        filePath = reports.utility.NewGenLibDesktopRoot.getRoot();
        setFilePath(filePath);
        txLocation.setText(getFilePath());
        jProgressBar1.setStringPainted(true);
        jProgressBar1.setIndeterminate(false);
        jProgressBar1.setString("");
        timer = new javax.swing.Timer(1000, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jProgressBar1.setString("Retrieving log file");
                if (!getStaus()) {
                    jProgressBar1.setString("");
                    jProgressBar1.setIndeterminate(false);
                    timer.stop();
                }
            }
        });
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public boolean getStaus() {
        return this.status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public void retrieveLog() {
        setFilePath(txLocation.getText());
        String logPath = "";
        String envVarPath = "";
        try {
            java.util.Properties props = new java.util.Properties();
            java.net.URL url = new java.net.URL(reports.utility.NewGenLibDesktopRoot.getInstance().getURLRoot() + "/SystemFiles/Env_Var.txt");
            props.load(url.openStream());
            String jbossHomePath = props.getProperty("JBOSS_HOME");
            jbossHomePath = jbossHomePath.replaceAll("deploy", "log");
            logPath = jbossHomePath + "/server.log";
            FileInputStream fis = new FileInputStream(new File(jbossHomePath + "/server.log"));
            BufferedInputStream bis = new BufferedInputStream(fis, 2048);
            ZipEntry entry = new ZipEntry(jbossHomePath + "/server.log");
            FileOutputStream fos = new FileOutputStream(new File(getFilePath() + "/server.zip"));
            ZipOutputStream zout = new ZipOutputStream(new BufferedOutputStream(fos));
            zout.putNextEntry(entry);
            byte buffer[] = new byte[2048];
            int count;
            while ((count = bis.read(buffer, 0, 2048)) != -1) {
                zout.write(buffer, 0, count);
            }
            zout.close();
            fos.close();
            bis.close();
            fis.close();
            setStatus(false);
            callSuccess();
        } catch (java.io.FileNotFoundException fileExp) {
            javax.swing.JOptionPane.showMessageDialog(this, "System is unable to find the log file in the path\n   " + logPath + "\nPlease go to " + reports.utility.NewGenLibDesktopRoot.getRoot() + "/SystemFiles/Env_Var.txt," + "\nand check the path of the variable JBOSS_HOME. \nAlso, make sure this operation is being carried on the \nsystem where NewGenLib server is running.", "File not found", javax.swing.JOptionPane.ERROR_MESSAGE);
            setStatus(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void callSuccess() {
        this.dispose();
        this.setVisible(false);
        logUtil.MailFormDialog mailForm = new logUtil.MailFormDialog();
        mailForm.setLocation(100, 100);
        String libName = reports.utility.StaticValues.getInstance().getLoginLibraryName();
        String userName = reports.utility.StaticValues.getInstance().getLoginPatronName();
        mailForm.sendLogThroughMail(libName, userName, getFilePath() + "/server.zip");
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        txLocation = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jProgressBar1 = new javax.swing.JProgressBar();
        jLabel2 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Retrieve log file");
        jPanel1.setLayout(new java.awt.BorderLayout());
        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.Y_AXIS));
        jPanel2.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jLabel1.setText("<html>System will now retrieve the log file, compress it and </li><li>save it in the below mentioned directory.</li></html>");
        jPanel5.add(jLabel1);
        jPanel2.add(jPanel5);
        jPanel4.setLayout(new java.awt.GridBagLayout());
        txLocation.setColumns(20);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        jPanel4.add(txLocation, gridBagConstraints);
        jButton1.setText("Change Location");
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        jPanel4.add(jButton1, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel4.add(jProgressBar1, gridBagConstraints);
        jLabel2.setForeground(new java.awt.Color(170, 0, 0));
        jLabel2.setText("<html>Plese perform this operation, if you are working on the system</li>\n<li> where NewGenLib application server is running.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel4.add(jLabel2, gridBagConstraints);
        jPanel2.add(jPanel4);
        jPanel1.add(jPanel2, java.awt.BorderLayout.CENTER);
        jPanel3.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jButton2.setText("Continue");
        jButton2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton2);
        jButton3.setText("Cancel");
        jButton3.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton3);
        jPanel1.add(jPanel3, java.awt.BorderLayout.SOUTH);
        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);
        pack();
    }

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            setStatus(true);
            jProgressBar1.setIndeterminate(true);
            timer.start();
            final tools.SwingWorker worker = new tools.SwingWorker() {

                public Object construct() {
                    retrieveLog();
                    return new Integer(1);
                }
            };
            worker.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            javax.swing.JFileChooser filechoose = new javax.swing.JFileChooser();
            filechoose.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
            filechoose.showOpenDialog(this);
            txLocation.setText(filechoose.getSelectedFile().getAbsolutePath());
            filePath = txLocation.getText();
            System.out.println("filePath = " + filePath);
            setFilePath(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
        this.setVisible(false);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new LogFileHandler(new javax.swing.JFrame(), true).setVisible(true);
            }
        });
    }

    private javax.swing.JButton jButton1;

    private javax.swing.JButton jButton2;

    private javax.swing.JButton jButton3;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JPanel jPanel3;

    private javax.swing.JPanel jPanel4;

    private javax.swing.JPanel jPanel5;

    private javax.swing.JProgressBar jProgressBar1;

    private javax.swing.JTextField txLocation;
}
