package org.cybergarage.xml;

import java.net.*;
import java.io.*;

public abstract class Parser {

    public Parser() {
    }

    public abstract Node parse(InputStream inStream) throws ParserException;

    public Node parse(URL locationURL) throws ParserException {
        try {
            HttpURLConnection urlCon = (HttpURLConnection) locationURL.openConnection();
            urlCon.setRequestMethod("GET");
            InputStream urlIn = urlCon.getInputStream();
            Node rootElem = parse(urlIn);
            urlIn.close();
            urlCon.disconnect();
            return rootElem;
        } catch (Exception e) {
            throw new ParserException(e);
        }
    }

    public Node parse(String locationURL) throws ParserException {
        try {
            URL url = new URL(locationURL);
            return parse(url);
        } catch (Exception e) {
            throw new ParserException(e);
        }
    }

    public Node parse(File descriptionFile) throws ParserException {
        System.out.println(descriptionFile.toString());
        try {
            InputStream fileIn = new FileInputStream(descriptionFile);
            Node root = parse(fileIn);
            fileIn.close();
            return root;
        } catch (Exception e) {
            throw new ParserException(e);
        }
    }
}
