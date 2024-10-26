package com.atech.update.client.dev;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import com.atech.update.client.action.ActionThread;
import com.atech.update.client.action.BinaryDownloadThread;
import com.atech.update.client.data.UpdateComponentEntry;
import com.atech.update.client.data.UpdateSettings;
import com.atech.update.client.panel.UpdateProgressPanel;
import com.atech.update.client.panel.UpdateProgressPanelAbstract;
import com.atech.utils.file.CheckSumUtility;

/**
 * The Class DownloadFileClient.
 */
public class DownloadFileClient extends JFrame implements ActionListener {

    private static final long serialVersionUID = 5524546247397085042L;

    /** The panel. */
    JPanel panel;

    /** The list. */
    JList list;

    /** The threads. */
    ArrayList<ActionThread> threads = new ArrayList<ActionThread>();

    /**
     * Instantiates a new download file client.
     */
    public DownloadFileClient() {
        super();
        initGUI();
        doAction();
        this.setVisible(true);
    }

    /**
     * Inits the gui.
     */
    private void initGUI() {
        getContentPane().setLayout(null);
        panel = new JPanel();
        panel.setBounds(0, 0, 600, 500);
        panel.setLayout(null);
        getContentPane().add(panel, null);
        JLabel label = new JLabel("New label");
        label.setBounds(83, 44, 70, 15);
        panel.add(label);
        list = new JList();
        JScrollPane pane = new JScrollPane(list);
        pane.setBounds(45, 102, 500, 350);
        panel.add(pane);
        JButton button = new JButton("New button");
        button.setBounds(319, 26, 117, 25);
        button.addActionListener(this);
        panel.add(button);
        this.setSize(600, 500);
    }

    /**
     * Do action.
     */
    public void doAction() {
        UpdateComponentEntry uce = new UpdateComponentEntry();
        uce.action = 1;
        uce.estimated_crc = 1889989348;
        uce.file_id = 1;
        uce.requested_version_id = 1;
        uce.output_file = "img_2345.jpg";
        UpdateProgressPanelAbstract p = new UpdateProgressPanel();
        this.list.add((JPanel) p);
        this.threads.add(new BinaryDownloadThread(new UpdateSettings(), uce, p));
    }

    /**
     * Do actionxxx.
     */
    public void doActionxxx() {
        try {
            System.out.println("app: ggc");
            String server_name = "http://192.168.4.3:8080/";
            server_name = server_name.trim();
            if (server_name.length() == 0) {
                server_name = "http://www.atech-software.com/";
            } else {
                if (!server_name.startsWith("http://")) server_name = "http://" + server_name;
                if (!server_name.endsWith("/")) server_name = server_name + "/";
            }
            URL url = new URL(server_name + "ATechUpdateGetFile?" + "" + "file_id=1" + "&" + "version_requested=1");
            InputStream is = url.openStream();
            RandomAccessFile raf = new RandomAccessFile("/home/andy/test.jpg", "rw");
            ArrayList<Integer> list = new ArrayList<Integer>();
            float size = 671200;
            long current_size = 0;
            System.out.println("File size: " + is.available());
            byte[] array = new byte[1024];
            while (is.available() > 0) {
                if (is.available() < 1024) {
                    array = new byte[is.available()];
                }
                is.read(array);
                raf.write(array);
                current_size += array.length;
                System.out.println("Progress: " + ((current_size / size) * 100));
            }
            System.out.println("Size Arr: " + list.size());
            CheckSumUtility csu = new CheckSumUtility();
            System.out.println("Checksum: " + csu.getChecksumValue("/home/andy/test.jpg"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * The main method.
     * 
     * @param args
     *            the arguments
     */
    public static void main(String[] args) {
        new DownloadFileClient();
    }

    public void actionPerformed(ActionEvent arg0) {
        this.threads.get(0).start();
    }
}
