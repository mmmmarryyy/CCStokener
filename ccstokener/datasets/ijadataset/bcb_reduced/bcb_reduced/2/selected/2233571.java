package org.enml.utils.poster.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;

/**
 * @author pag
 *
 */
public class MainFrame extends JFrame implements WindowListener, ActionListener {

    /**
	 * Serial UID 
	 */
    private static final long serialVersionUID = -6556846174131727395L;

    public static final int DEFAULT_WIDTH = 800;

    public static final int DEFAULT_HEIGHT = 600;

    private BorderLayout layout;

    public JMenuBar menuBar;

    public JToolBar toolBar;

    public MainPanel mainPanel;

    public String selectedRequest;

    public String requestText;

    public MainFrame() {
        makeComponents();
        makeMenu();
    }

    public void init() {
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        addWindowListener(this);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("XML Poster");
        layout = new BorderLayout(5, 5);
        setLayout(layout);
        add(toolBar, BorderLayout.PAGE_START);
        setJMenuBar(menuBar);
        add(mainPanel);
        mainPanel.init();
    }

    @Override
    public void windowActivated(WindowEvent arg0) {
    }

    @Override
    public void windowClosed(WindowEvent arg0) {
    }

    @Override
    public void windowClosing(WindowEvent arg0) {
        if (JOptionPane.showConfirmDialog(this, "Sure to go?", "Exit", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            dispose();
        }
    }

    @Override
    public void windowDeactivated(WindowEvent arg0) {
    }

    @Override
    public void windowDeiconified(WindowEvent arg0) {
    }

    @Override
    public void windowIconified(WindowEvent arg0) {
    }

    @Override
    public void windowOpened(WindowEvent arg0) {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == menuBar.getMenu(0).getItem(0)) {
            if (JOptionPane.showConfirmDialog(this, "Sure to go?", "Exit", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        } else if (e.getSource() == menuBar.getMenu(1).getItem(0)) {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("./requests"));
            chooser.setMultiSelectionEnabled(false);
            chooser.setFileFilter(new FileFilter() {

                @Override
                public String getDescription() {
                    return "Xml files";
                }

                @Override
                public boolean accept(File f) {
                    return f.getName().toLowerCase().endsWith(".xml") || f.isDirectory();
                }
            });
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedRequest = chooser.getSelectedFile().getAbsoluteFile().getAbsolutePath();
                loadFileContents(selectedRequest);
            }
        } else if (e.getSource() == menuBar.getMenu(1).getItem(1)) {
            if (!mainPanel.hostPanel.txtHost.getText().equals("Please insert host address") && !mainPanel.xmlRequest.getText().isEmpty()) {
                postXml();
            } else {
                JOptionPane.showMessageDialog(this, "Please control your input data");
            }
        }
    }

    private void makeMenu() {
        JMenu menu;
        JMenuItem menuItem;
        menu = new JMenu("File");
        menuBar.add(menu);
        menuItem = new JMenuItem("Exit");
        menu.add(menuItem);
        menuItem.addActionListener(this);
        menu = new JMenu("Operations");
        menuBar.add(menu);
        menuItem = new JMenuItem("Load request");
        menu.add(menuItem);
        menuItem.addActionListener(this);
        menuItem = new JMenuItem("Post");
        menu.add(menuItem);
        menuItem.addActionListener(this);
    }

    private void makeToolbar() {
        toolBar = new JToolBar();
    }

    private void makeComponents() {
        menuBar = new JMenuBar();
        makeToolbar();
        mainPanel = new MainPanel();
        selectedRequest = new String();
        requestText = new String();
    }

    private void loadFileContents(String path) {
        String line;
        requestText = "";
        mainPanel.xmlRequest.setText("");
        mainPanel.xmlResponse.setText("");
        try {
            BufferedReader r = new BufferedReader(new FileReader(path));
            try {
                while ((line = r.readLine()) != null) {
                    requestText += line + "\n";
                }
                r.close();
                mainPanel.xmlRequest.setText(requestText);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void postXml() {
        try {
            URL url = new URL(mainPanel.hostPanel.txtHost.getText());
            URLConnection con = url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setDefaultUseCaches(false);
            con.setRequestProperty("Content-Type", "text/xml");
            OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
            writer.write(URLEncoder.encode(mainPanel.xmlRequest.getText(), "UTF-8"));
            writer.flush();
            writer.close();
            InputStreamReader reader = new InputStreamReader(con.getInputStream());
            StringBuilder buf = new StringBuilder();
            char[] cbuf = new char[2048];
            int num;
            while (-1 != (num = reader.read(cbuf))) {
                buf.append(cbuf, 0, num);
            }
            String result = buf.toString();
            mainPanel.xmlResponse.setText(result);
        } catch (Throwable t) {
            t.printStackTrace(System.out);
        }
    }
}
