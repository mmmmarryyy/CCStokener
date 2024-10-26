package com.onehao.network;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

class Win extends JFrame implements ActionListener, Runnable {

    JButton button;

    URL url;

    JTextField text;

    JTextArea area;

    byte b[] = new byte[118];

    Thread thread;

    public Win() {
        text = new JTextField(20);
        area = new JTextArea(12, 12);
        button = new JButton("ȷ��");
        button.addActionListener(this);
        thread = new Thread(this);
        JPanel p = new JPanel();
        p.add(new JLabel("������ַ:"));
        p.add(text);
        p.add(button);
        Container con = this.getContentPane();
        con.add(new JScrollPane(area), BorderLayout.CENTER);
        con.add(p, BorderLayout.NORTH);
        setBounds(60, 60, 400, 300);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void actionPerformed(ActionEvent e) {
        if (!(thread.isAlive())) {
            thread = new Thread(this);
        }
        try {
            thread.start();
        } catch (Exception ee) {
            JOptionPane.showMessageDialog(this, "�����ڶ�ȡ�����Ժ�", "��ʾ��Ϣ", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void run() {
        try {
            int n = -1;
            area.setText(null);
            url = new URL(text.getText().trim());
            InputStream in = url.openStream();
            while ((n = in.read(b)) != -1) {
                String s = new String(b, 0, n);
                area.append(s);
            }
        } catch (MalformedURLException e1) {
            text.setText("" + e1);
            return;
        } catch (IOException e1) {
            text.setText("" + e1);
            return;
        }
    }
}

public class UrlConnection4 {

    public static void main(String args[]) {
        new Win();
    }
}
