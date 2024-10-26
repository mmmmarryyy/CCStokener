package shu.cms.applet.measure.auto;

import java.io.*;
import java.net.*;
import java.awt.*;
import javax.swing.*;

/**
 * <p>Title: Colour Management System</p>
 *
 * <p>Description: </p>
 * �Ψ����readme��Frame
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: </p>
 *
 * @author cms.shu.edu.tw
 * @version 1.0
 */
public class ReadmeFrame extends JFrame {

    BorderLayout borderLayout1 = new BorderLayout();

    JScrollPane jScrollPane1 = new JScrollPane();

    JTextPane jTextPane1 = new JTextPane();

    public ReadmeFrame() {
        try {
            jbInit();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        getContentPane().setLayout(borderLayout1);
        this.setTitle("�ϥλ���");
        jTextPane1.setEditable(false);
        this.getContentPane().add(jScrollPane1, java.awt.BorderLayout.CENTER);
        jScrollPane1.getViewport().add(jTextPane1);
        this.setSize(400, 600);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        URL url = ReadmeFrame.class.getResource("readme.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuilder strBuilder = new StringBuilder();
        while (reader.ready()) {
            strBuilder.append(reader.readLine());
            strBuilder.append('\n');
        }
        reader.close();
        jTextPane1.setText(strBuilder.toString());
    }

    public static void main(String[] args) {
        ReadmeFrame readmeframe = new ReadmeFrame();
        readmeframe.setVisible(true);
    }
}
