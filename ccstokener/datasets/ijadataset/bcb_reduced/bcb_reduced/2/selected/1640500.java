package org.dlib.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;

public class TextFileLoader {

    private Vector vector = new Vector();

    private boolean loaded = false;

    private String errorMsg = null;

    private TextFileListener listener;

    public TextFileLoader() {
    }

    public TextFileLoader(String fileName) {
        load(fileName);
    }

    public void load(String fileName) {
        BufferedReader bufReader;
        loaded = false;
        vector.removeAllElements();
        try {
            if (fileName.startsWith("http:")) {
                URL url = new URL(fileName);
                bufReader = new BufferedReader(new InputStreamReader(url.openStream()));
            } else bufReader = new BufferedReader(new FileReader(fileName));
            String inputLine;
            while ((inputLine = bufReader.readLine()) != null) {
                if (listener != null) listener.handleLine(inputLine); else vector.add(inputLine);
            }
            bufReader.close();
            loaded = true;
        } catch (IOException e) {
            errorMsg = e.getMessage();
        }
    }

    public String getString() {
        if (vector.isEmpty()) return null;
        String aux = "";
        for (int i = 0; i < vector.size(); i++) aux = aux + vector.elementAt(i) + "\n";
        return aux;
    }

    public String getRowAt(int rowNum) {
        return (String) vector.get(rowNum);
    }

    public int getRows() {
        return vector.size();
    }

    public boolean isLoaded() {
        return loaded;
    }

    public String getErrorMessage() {
        return errorMsg;
    }

    public Enumeration elements() {
        return vector.elements();
    }

    public void setListener(TextFileListener l) {
        listener = l;
    }
}
