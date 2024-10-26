package fr.ana.anaballistics.gui.window;

import javax.swing.JPanel;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import java.awt.Rectangle;
import java.awt.Label;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;
import fr.ana.anaballistics.Config;
import fr.ana.anaballistics.gui.listener.UpdateListener;

public class UpdateWindow extends JDialog {

    private static final long serialVersionUID = 1L;

    private JPanel jContentPane = null;

    private JProgressBar jProgressBar = null;

    private Label label0 = null;

    private JLabel currentVersionLabel = null;

    private JLabel installedVersionLabel = null;

    private JLabel downloadHereLabel = null;

    private JTextField downloadHereTextField = null;

    private JButton j3dots = null;

    private JButton exitButton = null;

    private JButton downloadButton = null;

    private UpdateListener ul;

    private MainWindow mw;

    private String downloadURL;

    /**
	 * @param owner
	 */
    public UpdateWindow(MainWindow mw) {
        super(mw);
        this.mw = mw;
        initialize();
    }

    /**
	 * This method initializes this
	 * 
	 * @return void
	 */
    private void initialize() {
        this.setSize(320, 240);
        ul = new UpdateListener(this);
        this.setContentPane(getJContentPane());
        this.setTitle(mw.getLangPak().get("Update"));
        this.setLocationRelativeTo(this.getParent());
        this.setResizable(false);
        this.setAlwaysOnTop(true);
        downloadURL = null;
        Thread initCurrentVersion = new Thread() {

            public void run() {
                URL url;
                try {
                    url = new URL(Config.UPDATE_SITE_URL);
                    InputStream is = url.openStream();
                    Writer writer = new StringWriter();
                    char[] buffer = new char[1024];
                    Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    int n;
                    while ((n = reader.read(buffer)) != -1) {
                        writer.write(buffer, 0, n);
                    }
                    String updatePage = writer.toString();
                    is.close();
                    writer.close();
                    System.out.println("DOWNLOAD PAGE :\n" + updatePage);
                    int pos1 = updatePage.indexOf("[ANA-CABV]") + 10;
                    int pos2 = updatePage.indexOf("[/ANA-CABV]");
                    int pos3 = updatePage.indexOf("[ANA-CABVURL]") + 13;
                    int pos4 = updatePage.indexOf("[/ANA-CABVURL]");
                    String currentVersion = updatePage.substring(pos1, pos2);
                    currentVersionLabel.setText(currentVersionLabel.getText() + currentVersion);
                    if (Double.valueOf(Config.VERSION) < Double.valueOf(currentVersion)) {
                        downloadButton.setEnabled(true);
                        label0.setText(mw.getLangMap().get("Update_Avalaible"));
                    } else label0.setText(mw.getLangMap().get("Update_NonAvalaible"));
                    downloadURL = updatePage.substring(pos3, pos4);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        initCurrentVersion.start();
    }

    /**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            downloadHereLabel = new JLabel();
            downloadHereLabel.setBounds(new Rectangle(23, 85, 112, 16));
            downloadHereLabel.setText(mw.getLangMap().get("Update_DownloadHere") + " :");
            installedVersionLabel = new JLabel();
            installedVersionLabel.setBounds(new Rectangle(23, 62, 263, 16));
            installedVersionLabel.setText(mw.getLangMap().get("Update_InstalledVersion") + " : " + Config.VERSION);
            currentVersionLabel = new JLabel();
            currentVersionLabel.setBounds(new Rectangle(23, 39, 263, 16));
            currentVersionLabel.setText(mw.getLangMap().get("Update_CurrentVersion") + " : ");
            label0 = new Label();
            label0.setBounds(new Rectangle(23, 16, 263, 16));
            label0.setText("");
            jContentPane = new JPanel();
            jContentPane.setLayout(null);
            jContentPane.add(getJProgressBar(), null);
            jContentPane.add(label0, null);
            jContentPane.add(currentVersionLabel, null);
            jContentPane.add(installedVersionLabel, null);
            jContentPane.add(downloadHereLabel, null);
            jContentPane.add(getDownloadHereTextField(), null);
            jContentPane.add(getJ3dots(), null);
            jContentPane.add(getExitButton(), null);
            jContentPane.add(getDownloadButton(), null);
        }
        return jContentPane;
    }

    /**
	 * This method initializes jProgressBar	
	 * 	
	 * @return javax.swing.JProgressBar	
	 */
    public JProgressBar getJProgressBar() {
        if (jProgressBar == null) {
            jProgressBar = new JProgressBar();
            jProgressBar.setBounds(new Rectangle(23, 108, 263, 39));
            jProgressBar.setMinimum(0);
        }
        return jProgressBar;
    }

    /**
	 * This method initializes downloadHereTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
    public JTextField getDownloadHereTextField() {
        if (downloadHereTextField == null) {
            downloadHereTextField = new JTextField();
            downloadHereTextField.setBounds(new Rectangle(144, 84, 116, 19));
        }
        return downloadHereTextField;
    }

    /**
	 * This method initializes j3dots	
	 * 	
	 * @return javax.swing.JButton	
	 */
    public JButton getJ3dots() {
        if (j3dots == null) {
            j3dots = new JButton();
            j3dots.setBounds(new Rectangle(267, 84, 19, 19));
            j3dots.setText("...");
            j3dots.addMouseListener(ul);
        }
        return j3dots;
    }

    /**
	 * This method initializes exitButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
    public JButton getExitButton() {
        if (exitButton == null) {
            exitButton = new JButton();
            exitButton.setBounds(new Rectangle(158, 154, 128, 30));
            exitButton.setText(mw.getLangMap().get("Update_ExitButton"));
            exitButton.addMouseListener(ul);
        }
        return exitButton;
    }

    /**
	 * This method initializes downloadButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
    public JButton getDownloadButton() {
        if (downloadButton == null) {
            downloadButton = new JButton();
            downloadButton.setBounds(new Rectangle(25, 154, 128, 30));
            downloadButton.setText(mw.getLangMap().get("Update_DownloadButton"));
            downloadButton.addMouseListener(ul);
            downloadButton.setEnabled(false);
        }
        return downloadButton;
    }

    public String getDownloadURL() {
        return downloadURL;
    }

    public MainWindow getMW() {
        return mw;
    }
}
