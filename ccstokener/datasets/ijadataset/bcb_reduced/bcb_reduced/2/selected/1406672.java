package net.face2face.ui;

import java.util.*;
import javax.swing.*;
import net.face2face.core.net.UserInfo;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.rtf.RTFEditorKit;
import net.face2face.core.net.MessageListener;
import net.face2face.core.net.NetworkManager;
import rath.msnm.util.StringUtil;

/**
 *
 * @author  Patrice
 */
public class ChatPanel extends JPanel implements MessageListener {

    private static final SimpleDateFormat ts = new SimpleDateFormat("[HH:mm]");

    static {
        loadEmoticons();
    }

    private SimpleAttributeSet style;

    private UserInfo userInfo;

    private int order = 0;

    /** Creates new form ChatPanel */
    public ChatPanel(UserInfo userInfo) {
        this.userInfo = userInfo;
        initComponents();
        style = new SimpleAttributeSet();
        StyleConstants.setForeground(style, Color.black);
        StyleConstants.setFontSize(style, 12);
        StyleConstants.setBold(style, false);
        StyleConstants.setItalic(style, false);
        chatlogTextPane.setContentType("text/rtf");
        chatlogTextPane.setEditorKit(new RTFEditorKit());
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        chatPanel = new javax.swing.JPanel();
        chatlogScrollPanel = new javax.swing.JScrollPane();
        chatlogTextPane = new javax.swing.JTextPane();
        chatinputPanel = new javax.swing.JPanel();
        messageTextField = new javax.swing.JTextField();
        sendButton = new javax.swing.JButton();
        setLayout(new java.awt.BorderLayout());
        chatPanel.setLayout(new java.awt.GridBagLayout());
        chatPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Chat"));
        chatlogScrollPanel.setAutoscrolls(true);
        chatlogScrollPanel.setPreferredSize(new java.awt.Dimension(50, 50));
        chatlogTextPane.setEditable(false);
        chatlogScrollPanel.setViewportView(chatlogTextPane);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        chatPanel.add(chatlogScrollPanel, gridBagConstraints);
        chatinputPanel.setLayout(new java.awt.GridBagLayout());
        messageTextField.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                messageTextFieldActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        chatinputPanel.add(messageTextField, gridBagConstraints);
        sendButton.setText("send");
        sendButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendButtonActionPerformed(evt);
            }
        });
        chatinputPanel.add(sendButton, new java.awt.GridBagConstraints());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        chatPanel.add(chatinputPanel, gridBagConstraints);
        add(chatPanel, java.awt.BorderLayout.CENTER);
    }

    private void messageTextFieldActionPerformed(java.awt.event.ActionEvent evt) {
        sendMessage();
    }

    private void sendButtonActionPerformed(java.awt.event.ActionEvent evt) {
        sendMessage();
    }

    private void sendMessage() {
        String messageString = messageTextField.getText();
        append(ts.format(new Date()) + " Me: " + messageString, Color.BLUE, "B", "Dialog");
        NetworkManager.sendSend(userInfo.getUUID(), messageString, order++);
        messageTextField.setText("");
    }

    public void messageReceived(UUID fromServent, int order, String messageString) {
        append(ts.format(new Date()) + " " + userInfo.getName() + ": " + messageString, Color.MAGENTA, "I", "Dialog");
    }

    public void append(String msg) {
        append(msg, new Color(0, 0, 0), "", "Dialog");
    }

    /**
     * append message to chat window.
     *
     * @param msg the message
     * @param co  the color
     * @param ef  the font type
     * @param fn  the font name.
     */
    public void append(String msg, Color co, String ef, String fn) {
        Color a3 = StyleConstants.getForeground(style);
        StyleConstants.setForeground(style, co);
        String a2 = StyleConstants.getFontFamily(style);
        StyleConstants.setFontFamily(style, fn);
        boolean fi = false;
        boolean fu = false;
        boolean fb = false;
        boolean fs = false;
        if (ef.indexOf('B') != -1) {
            fb = StyleConstants.isBold(style);
            StyleConstants.setBold(style, true);
        }
        if (ef.indexOf('I') != -1) {
            fi = StyleConstants.isItalic(style);
            StyleConstants.setItalic(style, true);
        }
        if (ef.indexOf('S') != -1) {
            fs = StyleConstants.isStrikeThrough(style);
            StyleConstants.setStrikeThrough(style, true);
        }
        if (ef.indexOf('U') != -1) {
            fu = StyleConstants.isUnderline(style);
            StyleConstants.setUnderline(style, true);
        }
        try {
            Document doc = chatlogTextPane.getDocument();
            String s = doc.getText(0, doc.getLength());
            int d1 = s.length();
            doc.insertString(doc.getLength(), msg, style);
            chatlogTextPane.setEditable(true);
            replaceEmoticon(msg, d1);
            chatlogTextPane.setEditable(false);
            doc.insertString(doc.getLength(), "\n", style);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        StyleConstants.setFontFamily(style, a2);
        StyleConstants.setForeground(style, a3);
        StyleConstants.setBold(style, fb);
        StyleConstants.setItalic(style, fi);
        StyleConstants.setStrikeThrough(style, fs);
        StyleConstants.setUnderline(style, fu);
        chatlogTextPane.validate();
        chatlogScrollPanel.validate();
        JScrollBar sb = chatlogScrollPanel.getVerticalScrollBar();
        sb.setValue(sb.getMaximum());
    }

    /**
     * Message���� �̸�Ƽ���� ġȯ�Ѵ�.
     *
     */
    protected void replaceEmoticon(String msg, int d1) {
        Document doc = chatlogTextPane.getDocument();
        try {
            boolean hasMoreEmoticons = true;
            int lid = -1;
            while (hasMoreEmoticons) {
                Enumeration<String> e = emoticons.keys();
                boolean a = false;
                while (e.hasMoreElements()) {
                    String key = e.nextElement();
                    int index = -1;
                    if ((index = msg.indexOf(key.toUpperCase(), lid)) != -1 || (index = msg.indexOf(key.toLowerCase(), lid)) != -1) {
                        lid = index;
                        doc.remove((d1 + index), key.length());
                        chatlogTextPane.setCaretPosition(d1 + index);
                        chatlogTextPane.insertIcon(emoticons.get(key));
                        msg = doc.getText(d1, doc.getLength() - d1);
                        a = true;
                        break;
                    }
                }
                hasMoreEmoticons = a;
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static Hashtable<String, ImageIcon> emoticons;

    private static void loadEmoticons() {
        emoticons = new Hashtable();
        URL url = ChatPanel.class.getResource("/resources/text/emoticon.properties");
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() == 0 || line.charAt(0) == '#') continue;
                int i0 = line.indexOf('=');
                if (i0 != -1) {
                    String key = line.substring(0, i0).trim();
                    String value = line.substring(i0 + 1).trim();
                    value = StringUtil.replaceString(value, "\\n", "\n");
                    URL eUrl = ChatPanel.class.getResource("/resources/emoticon/" + value);
                    if (eUrl != null) emoticons.put(key, new ImageIcon(eUrl));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                }
            }
        }
    }

    javax.swing.JPanel chatPanel;

    javax.swing.JPanel chatinputPanel;

    javax.swing.JScrollPane chatlogScrollPanel;

    javax.swing.JTextPane chatlogTextPane;

    javax.swing.JTextField messageTextField;

    javax.swing.JButton sendButton;
}
