package gui.applet;

import java.util.*;
import java.io.*;
import java.net.*;
import java.awt.Font;
import java.awt.Color;

/**
 *
 * @author  alexog
 */
public class ChatFieldApplet extends javax.swing.JApplet {

    /** Initializes the applet ChatFieldApplet */
    public void init() {
        try {
            java.awt.EventQueue.invokeAndWait(new Runnable() {

                public void run() {
                    initComponents();
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static boolean doPost(String urlString, Map<String, String> nameValuePairs) throws IOException {
        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        PrintWriter out = new PrintWriter(conn.getOutputStream());
        boolean first = true;
        for (Map.Entry<String, String> pair : nameValuePairs.entrySet()) {
            if (first) first = false; else out.print('&');
            String name = pair.getKey();
            String value = pair.getValue();
            out.print(name);
            out.print('=');
            out.print(URLEncoder.encode(value, "UTF-8"));
        }
        out.close();
        Scanner in;
        StringBuilder response = new StringBuilder();
        try {
            in = new Scanner(conn.getInputStream());
        } catch (IOException ex) {
            if (!(conn instanceof HttpURLConnection)) throw ex;
            InputStream err = ((HttpURLConnection) conn).getErrorStream();
            in = new Scanner(err);
        }
        while (in.hasNextLine()) {
            response.append(in.nextLine());
            response.append("\n");
        }
        in.close();
        return true;
    }

    private String username;

    void sendPost(String textsize, String textcolor, String text) {
        String url = "http://localhost:8080/Game-war/chatservlet";
        Map<String, String> pair = new HashMap<String, String>();
        pair.put("username", username);
        pair.put("textsize", textsize);
        pair.put("textcolor", textcolor);
        pair.put("textfield", text);
        try {
            if (doPost(url, pair)) {
            } else {
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void initComponents() {
        smilePopopMenu = new javax.swing.JPopupMenu();
        smileList = new javax.swing.JMenuItem();
        chatToolBar = new javax.swing.JToolBar();
        textSizeComboBox = new javax.swing.JComboBox();
        colorChoose = new javax.swing.JButton();
        smileButton = new javax.swing.JButton();
        mainPane = new javax.swing.JScrollPane();
        textField = new javax.swing.JTextArea();
        sayButton = new javax.swing.JButton();
        smileList.setText("Item");
        smileList.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                smileListActionPerformed(evt);
            }
        });
        smilePopopMenu.add(smileList);
        chatToolBar.setBackground(new java.awt.Color(255, 255, 255));
        textSizeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "h7", "h6", "h5", "h4", "h3", "h2", "h1", "h0" }));
        chatToolBar.add(textSizeComboBox);
        colorChoose.setText("Color");
        colorChoose.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colorChooseActionPerformed(evt);
            }
        });
        chatToolBar.add(colorChoose);
        smileButton.setComponentPopupMenu(smilePopopMenu);
        smileButton.setText("smile");
        smileButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                smileButtonActionPerformed(evt);
            }
        });
        chatToolBar.add(smileButton);
        getContentPane().add(chatToolBar, java.awt.BorderLayout.PAGE_START);
        textField.setColumns(20);
        textField.setRows(5);
        mainPane.setViewportView(textField);
        getContentPane().add(mainPane, java.awt.BorderLayout.CENTER);
        sayButton.setText("Say!");
        sayButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sayButtonActionPerformed(evt);
            }
        });
        getContentPane().add(sayButton, java.awt.BorderLayout.PAGE_END);
    }

    private void sayButtonActionPerformed(java.awt.event.ActionEvent evt) {
        this.sendPost(this.textSizeComboBox.getSelectedItem().toString(), this.getColortest().toString(), this.textField.getText());
    }

    private void smileListActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void smileButtonActionPerformed(java.awt.event.ActionEvent evt) {
        this.smilePopopMenu.show(this.smileButton, this.smileButton.getX(), this.smileButton.getY());
    }

    private void colorChooseActionPerformed(java.awt.event.ActionEvent evt) {
        this.setColortest(javax.swing.JColorChooser.showDialog(this, "Choose text color", Color.BLACK));
    }

    private Color colortest = Color.BLACK;

    public Color getColortest() {
        return colortest;
    }

    public void setColortest(Color colortest) {
        this.colortest = colortest;
    }

    private javax.swing.JToolBar chatToolBar;

    private javax.swing.JButton colorChoose;

    private javax.swing.JScrollPane mainPane;

    private javax.swing.JButton sayButton;

    private javax.swing.JButton smileButton;

    private javax.swing.JMenuItem smileList;

    private javax.swing.JPopupMenu smilePopopMenu;

    private javax.swing.JTextArea textField;

    private javax.swing.JComboBox textSizeComboBox;
}
