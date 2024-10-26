package com.servengine.util;

import java.io.*;
import java.util.Vector;
import java.net.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;

public class HTMLPageParser extends HTMLEditorKit.ParserCallback {

    String texto;

    String title;

    Vector<String> enlaces = new Vector<String>();

    static DTD dtd;

    private boolean istitle = false;

    public HTMLPageParser() throws IOException {
        if (dtd == null) {
            InputStream dtdStream = System.out.getClass().getResourceAsStream("/javax/swing/text/html/parser/html32.bdtd");
            if (dtdStream == null) {
                throw new IOException("No se puede cargar el html32.bdtd");
            } else {
                DataInputStream dis = new DataInputStream(dtdStream);
                dtd = DTD.getDTD("HTML");
                dtd.read(dis);
                dis.close();
            }
        }
    }

    public void parse(String c) throws IOException {
        texto = null;
        title = null;
        enlaces = new Vector<String>();
        byte[] bites = c.getBytes();
        InputStream input = new ByteArrayInputStream(bites);
        new DocumentParser(dtd).parse(new InputStreamReader(input), this, true);
    }

    public void parse(URL url) throws IOException {
        URLConnection con = url.openConnection();
        parse(con.getInputStream());
    }

    public void parse(InputStream input) throws IOException {
        String content = null;
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(input));
        String str;
        while ((str = reader.readLine()) != null) content += str;
        parse(content);
    }

    /** Pasa a Texto quitando los tags HTML */
    public String getText() {
        return texto.substring(4);
    }

    public String getTitle() {
        return title;
    }

    /** Manejador que recoge la parte de texto */
    public void handleText(char[] data, int pos) {
        if (istitle) {
            title = new String(data);
            istitle = false;
        } else {
            if (data == null) return;
            String dato = new String(data);
            if (dato != null && dato.indexOf('{') == -1 & dato.indexOf('}') == -1 & dato.indexOf('<') == -1 & dato.indexOf('>') == -1 & dato.indexOf("error") == -1 & dato.indexOf("HTTP/1") == -1 & dato.indexOf("null") == -1 & !dato.equals("\n")) texto += dato + "\n";
        }
    }

    /** Manejador que recoge todos los href que hay en la página */
    public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        String urlRetorno = null;
        if (t.toString().equals("a")) {
            try {
                urlRetorno = (String) a.getAttribute(HTML.Attribute.HREF);
                if (!enlaces.contains(urlRetorno)) enlaces.addElement(urlRetorno);
            } catch (Exception ex) {
            }
        }
        if (t.toString().equals("title")) {
            istitle = true;
        }
    }

    public String[] getHrefs() {
        String[] urls = new String[enlaces.size()];
        for (int a = 0; a < enlaces.size(); a++) urls[a] = (String) enlaces.elementAt(a);
        return urls;
    }
}
