package br.ufpb.di.knowledgetv.classificador;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

public class ImportadorArff {

    private static final String ARFF_URL = "http://knowledgetvform.appspot.com/arff";

    public static Reader getArffReader() {
        URL url = null;
        BufferedReader reader = null;
        try {
            url = new URL(ARFF_URL);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return reader;
    }
}
