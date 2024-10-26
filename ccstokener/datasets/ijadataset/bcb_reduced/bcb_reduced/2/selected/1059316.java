package net.jankenpoi.sudokuki.ui.swing;

import static net.jankenpoi.i18n.I18n._;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import net.jankenpoi.sudokuki.Version;

/**
 * CheckUpdateDialog.java
 * 
 * @author svedrenne
 */
@SuppressWarnings("serial")
public class CheckUpdateDialog extends JDialog {

    private static final String VERSION_FILE_DOWNLOAD_WEB_SITE = "http://sourceforge.net/projects/sudokuki/files/sudokuki/1.1/LATEST/download";

    private JFrame parent;

    final CheckUpdateAction checkUpdateAction;

    private int result = -1;

    private final SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {

        @Override
        protected String doInBackground() throws Exception {
            final String str = getHttpLatestVersionString();
            return str;
        }

        @Override
        protected void done() {
            result = -1;
            try {
                String httpVersionString = get();
                if (httpVersionString.equals(Version.versionString)) {
                    result = 0;
                } else if (httpVersionString.startsWith("Sudokuki")) {
                    checkUpdateAction.notifyNewVersionFound();
                    result = 1;
                }
            } catch (InterruptedException e) {
                System.out.println("CheckUpdateDialog.worker.new SwingWorker() {...}.done() Interrupted !!!!");
            } catch (ExecutionException e) {
                System.out.println("CheckUpdateDialog.worker.new SwingWorker() {...}.done() ExecutionException !!!!");
            } catch (CancellationException e) {
                System.out.println("CheckUpdateDialog.worker.new SwingWorker() {...}.done() CancellationException !!!!");
                result = -2;
            } finally {
                CheckUpdateDialog.this.dispose();
            }
        }

        private String getHttpLatestVersionString() {
            String line = null;
            BufferedReader dis = null;
            try {
                URL url;
                URLConnection urlConn;
                url = new URL(VERSION_FILE_DOWNLOAD_WEB_SITE);
                urlConn = url.openConnection();
                urlConn.setDoInput(true);
                urlConn.setUseCaches(false);
                dis = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
                line = dis.readLine();
            } catch (MalformedURLException mue) {
                mue.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                try {
                    dis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            String versionString = "";
            if (line != null) {
                String[] strs = line.split(" is the latest version.");
                if (strs.length >= 1) {
                    versionString = strs[0];
                }
            }
            return versionString;
        }
    };

    public CheckUpdateDialog(JFrame parent, final CheckUpdateAction checkUpdateAction) {
        super(parent, true);
        this.checkUpdateAction = checkUpdateAction;
        this.parent = parent;
        initComponents();
        setResizable(false);
        worker.execute();
    }

    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        Container pane = getContentPane();
        GridLayout layout = new GridLayout(3, 1);
        pane.setLayout(layout);
        JLabel messageLbl1 = new JLabel("<html>" + "<table border=\"0\">" + "<tr>" + "<td align=\"center\">" + _("Checking for available updates") + ".</td>" + "</tr><html>");
        JLabel messageLbl2 = new JLabel("<html>" + "<table border=\"0\">" + "<tr>" + "<td align=\"center\">" + _("Please wait...") + "</td>" + "</tr><html>");
        JButton cancelBtn = new JButton(_("Cancel"));
        cancelBtn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        cancelBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonClicked();
            }
        });
        pane.add(messageLbl1);
        pane.add(messageLbl2);
        FlowLayout btnLayout = new FlowLayout(1);
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(btnLayout);
        btnPanel.add(cancelBtn);
        pane.add(btnPanel);
        pack();
        setLocationRelativeTo(parent);
    }

    private void cancelButtonClicked() {
        worker.cancel(true);
    }

    /**
	 * @return <ul>
	 *         <li>0 if the version is up-to-date</li>
	 *         <li>1 if a new version is available</li>
	 *         <li>-1 if an error occurred</li>
	 *         <li>-2 if canceled by the user</li>
	 *         </ul>
	 * 
	 *         Must be executed in the EDT only (because it accesses
	 *         <i>result</i>).
	 */
    int getResult() {
        return result;
    }
}
