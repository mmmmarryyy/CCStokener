package com.diccionarioderimas.updater;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import com.diccionarioderimas.generator.ErrorDialog;
import com.diccionarioderimas.generator.Main;
import com.diccionarioderimas.generator.Utils;

public class UpdateGUI extends JDialog implements ActionListener, Runnable {

    private static final long serialVersionUID = 4379960618417398170L;

    private Update dic, gen, res, help;

    private String basePath;

    private JButton ok, cancel;

    private JProgressBar progressBar;

    private int total = 0;

    private JFrame frame;

    public UpdateGUI(JFrame frame, String basePath, Update dic, Update gen, Update res, Update help) {
        super(frame, "Actualizaciones", false);
        this.frame = frame;
        this.dic = dic;
        this.gen = gen;
        this.res = res;
        this.help = help;
        this.basePath = basePath;
        Container cp = getContentPane();
        cp.setBackground(Color.WHITE);
        cp.setLayout(null);
        JTextArea textArea = new JTextArea();
        if (dic != null) {
            total += dic.getParam1AsNumber();
            textArea.append(dic.getParam2() + "\n");
        }
        if (gen != null) {
            total += gen.getParam1AsNumber();
            textArea.append(gen.getParam2() + "\n");
        }
        if (res != null) {
            total += res.getParam1AsNumber();
            textArea.append(res.getParam2() + "\n");
        }
        if (help != null) {
            total += help.getParam1AsNumber();
            textArea.append(help.getParam2() + "\n");
        }
        progressBar = new JProgressBar(0, total);
        progressBar.setStringPainted(true);
        JLabel label = new JLabel("<html>&nbsp;&nbsp;&nbsp;&nbsp;Hay actualizaciones para el <b>diccionario de rimas</b>:</html>");
        label.setBorder(BorderFactory.createTitledBorder(""));
        textArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(textArea);
        ok = new JButton("Instalar");
        cancel = new JButton("Cancelar");
        ok.addActionListener(this);
        cancel.addActionListener(this);
        cp.add(progressBar);
        cp.add(label);
        cp.add(scroll);
        cp.add(ok);
        cp.add(cancel);
        label.setBounds(20, 20, 450, 50);
        scroll.setBounds(20, 80, 450, 100);
        progressBar.setBounds(20, 190, 450, 25);
        ok.setBounds(260, 250, 100, 25);
        cancel.setBounds(370, 250, 100, 25);
        setSize(new Dimension(500, 340));
        Dimension dim = getToolkit().getScreenSize();
        setLocation((dim.width - 500) / 2, (dim.height - 340) / 2);
        setResizable(false);
        setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == cancel) setVisible(false); else {
            new Thread(this).start();
        }
    }

    public void run() {
        Vector<Update> updates = new Vector<Update>();
        if (dic != null) updates.add(dic);
        if (gen != null) updates.add(gen);
        if (res != null) updates.add(res);
        if (help != null) updates.add(help);
        for (Iterator iterator = updates.iterator(); iterator.hasNext(); ) {
            Update update = (Update) iterator.next();
            try {
                File temp = File.createTempFile("fm_" + update.getType(), ".jar");
                temp.deleteOnExit();
                FileOutputStream out = new FileOutputStream(temp);
                URL url = new URL(update.getAction());
                URLConnection conn = url.openConnection();
                com.diccionarioderimas.Utils.setupProxy(conn);
                InputStream in = conn.getInputStream();
                byte[] buffer = new byte[1024];
                int read = 0;
                int total = 0;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    total += read;
                    if (total > 10000) {
                        progressBar.setValue(progressBar.getValue() + total);
                        total = 0;
                    }
                }
                out.close();
                in.close();
                String fileTo = basePath + "diccionariorimas.jar";
                if (update.getType() == Update.GENERATOR) fileTo = basePath + "generador.jar"; else if (update.getType() == Update.RESBC) fileTo = basePath + "resbc.me"; else if (update.getType() == Update.HELP) fileTo = basePath + "help.html";
                if (update.getType() == Update.RESBC) {
                    Utils.unzip(temp, new File(fileTo));
                } else {
                    Utils.copyFile(new FileInputStream(temp), new File(fileTo));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        setVisible(false);
        if (gen != null || res != null) {
            try {
                new Main(null, basePath, false);
            } catch (Exception e) {
                new ErrorDialog(frame, e);
            }
        }
        String restart = "";
        if (dic != null) restart += "\nAlgunas de ellas s�lo estar�n disponibles despu�s de reiniciar el diccionario.";
        JOptionPane.showMessageDialog(frame, "Se han terminado de realizar las actualizaciones." + restart, "Actualizaciones", JOptionPane.INFORMATION_MESSAGE);
    }
}
