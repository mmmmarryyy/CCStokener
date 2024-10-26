package br.org.direto.util;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.io.*;
import javax.servlet.ServletOutputStream;
import javax.swing.*;

public class AppletTest extends JApplet implements ActionListener {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    JButton jbutton = null;

    String documento = "";

    public void init() {
        documento = getParameter("documento");
        jbutton = new JButton("Send file " + documento);
        jbutton.addActionListener(this);
        this.getContentPane().add(jbutton);
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == jbutton) {
            try {
                String toservlet = "http://localhost:8080/direto-project/arquivos/teste.odt";
                URL servleturl = new URL(toservlet);
                URLConnection servletconnection = servleturl.openConnection();
                servletconnection.setDoInput(true);
                servletconnection.setDoOutput(true);
                servletconnection.setUseCaches(false);
                servletconnection.setDefaultUseCaches(false);
                DataInputStream inputFromClient = new DataInputStream(servletconnection.getInputStream());
                inputFromClient.readByte();
                OutputStream fos = new FileOutputStream("/home/danillo/arquivo_carregado.odt");
                byte[] buf = new byte[1024];
                int bytesread;
                while ((bytesread = inputFromClient.read(buf)) > -1) {
                    fos.write(buf, 0, bytesread);
                }
                inputFromClient.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
