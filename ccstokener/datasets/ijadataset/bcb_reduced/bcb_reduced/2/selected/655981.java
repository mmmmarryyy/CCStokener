package com.editor.transmissao;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URLConnection;
import javax.swing.JLabel;

/**
 * Classe Envia XML para o servidor
 * @author Jo�o Victor
 * @see Thread
 */
public class EnviaXML extends Thread {

    public static final String URL_EXPORT_ACTION = "http://mobileformeditor.appspot.com/armazena";

    private String xml = "";

    private JLabel label;

    @Override
    public void run() {
        try {
            URL url = new URL(URL_EXPORT_ACTION);
            getLabel().setText("Estabelecendo conex�o a mobileformeditor.appspot.com ...");
            StringBuilder menssage = new StringBuilder("nick=JV&senha=1234&arquivo=");
            menssage = menssage.append(getXml());
            System.out.println("Mensagem a Enviar: " + menssage);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            System.out.println(writer.getEncoding());
            getLabel().setText("Transmitindo ...");
            writer.write(new String(menssage.toString().getBytes("ISO-8859-1")));
            writer.flush();
            StringBuffer answer = new StringBuffer();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                answer.append(line);
            }
            writer.close();
            reader.close();
            System.out.println(answer.toString());
            if (answer.toString().compareTo("OK") == 0) {
                getLabel().setText("Opera��o conclu�da com sucesso!");
            } else {
                getLabel().setText("Erro na transmiss�o: " + answer.toString());
            }
        } catch (MalformedURLException e) {
            getLabel().setText("Erro na transmiss�o: URL");
        } catch (IOException e) {
            getLabel().setText("Erro na transmiss�o: IO");
        }
    }

    /**
     * @return the xml
     */
    public String getXml() {
        return xml;
    }

    /**
     * @param xml the xml to set
     */
    public void setXml(String xml) {
        this.xml = xml;
    }

    /**
     * @return the label
     */
    public JLabel getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(JLabel label) {
        this.label = label;
    }
}
