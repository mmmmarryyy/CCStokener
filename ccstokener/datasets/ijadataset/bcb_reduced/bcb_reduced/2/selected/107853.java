package server.chat;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import javax.swing.JToolBar;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import javax.swing.JButton;
import javax.swing.JTextField;
import java.awt.Dimension;
import javax.swing.DebugGraphics;
import javax.swing.BorderFactory;
import java.awt.Color;
import javax.swing.*;
import java.util.*;
import java.io.*;
import java.net.*;

/**
 * <p>Title: ChatField</p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: </p>
 *
 * @author alexog
 * @version 1.0
 */
public class ChatFieldApplet extends Applet {

    boolean isStandalone = false;

    BorderLayout borderLayout1 = new BorderLayout();

    String username;

    JToolBar jChatbar = new JToolBar();

    JTextArea ChatArea = new JTextArea();

    JButton Smile = new JButton();

    JButton TextFont = new JButton();

    JTextField NumberofChars = new JTextField();

    JButton SayButton = new JButton();

    public String getParameter(String key, String def) {
        return isStandalone ? System.getProperty(key, def) : (getParameter(key) != null ? getParameter(key) : def);
    }

    public ChatFieldApplet() {
    }

    public void init() {
        try {
            username = this.getParameter("username", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        this.setLayout(null);
        ChatArea.setBounds(new Rectangle(2, 28, 597, 75));
        ChatArea.addKeyListener(new ChatFieldApplet_ChatArea_keyAdapter(this));
        ChatArea.setText(this.getCodeBase().toString());
        jChatbar.setBorder(BorderFactory.createLineBorder(Color.black));
        jChatbar.setDebugGraphicsOptions(0);
        jChatbar.setToolTipText("");
        jChatbar.setBounds(new Rectangle(2, 0, 598, 26));
        Smile.setActionCommand("smile");
        Smile.setText("Simylik");
        TextFont.setActionCommand("textfont");
        TextFont.setText("jButton2");
        NumberofChars.setDebugGraphicsOptions(DebugGraphics.FLASH_OPTION);
        NumberofChars.setPreferredSize(new Dimension(12, 20));
        NumberofChars.setEditable(false);
        NumberofChars.setText("0");
        NumberofChars.setHorizontalAlignment(SwingConstants.CENTER);
        this.setBackground(SystemColor.textHighlightText);
        this.setForeground(SystemColor.control);
        SayButton.setBounds(new Rectangle(2, 106, 597, 24));
        SayButton.setText("SAY");
        SayButton.addActionListener(new ChatFieldApplet_SayButton_actionAdapter(this));
        this.add(ChatArea);
        this.add(SayButton);
        this.add(jChatbar, null);
        jChatbar.add(NumberofChars);
        jChatbar.add(Smile);
        jChatbar.add(TextFont);
    }

    public void start() {
    }

    public void stop() {
    }

    public void destroy() {
    }

    public String getAppletInfo() {
        return "Applet Information";
    }

    public String[][] getParameterInfo() {
        java.lang.String[][] pinfo = { { "username", "String", "" } };
        return pinfo;
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

    void sendPost() {
        String url = "http://localhost:8080/Game-war/chatservlet";
        Map<String, String> pair = new HashMap<String, String>();
        pair.put("username", username);
        pair.put("textfield", ChatArea.getText());
        try {
            if (doPost(url, pair)) {
                ChatArea.setText("OK");
            } else {
                ChatArea.setText("False");
            }
        } catch (IOException ex) {
            ChatArea.setText("Error");
        }
    }

    public void SayButton_actionPerformed(ActionEvent e) {
        sendPost();
    }

    public void ChatArea_keyPressed(KeyEvent e) {
        if (e.getKeyCode() == e.VK_ENTER) sendPost();
    }
}

class ChatFieldApplet_ChatArea_keyAdapter extends KeyAdapter {

    private ChatFieldApplet adaptee;

    ChatFieldApplet_ChatArea_keyAdapter(ChatFieldApplet adaptee) {
        this.adaptee = adaptee;
    }

    public void keyPressed(KeyEvent e) {
        adaptee.ChatArea_keyPressed(e);
    }
}

class ChatFieldApplet_SayButton_actionAdapter implements ActionListener {

    private ChatFieldApplet adaptee;

    ChatFieldApplet_SayButton_actionAdapter(ChatFieldApplet adaptee) {
        this.adaptee = adaptee;
    }

    public void actionPerformed(ActionEvent e) {
        adaptee.SayButton_actionPerformed(e);
    }
}
