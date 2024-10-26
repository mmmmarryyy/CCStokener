package de.denkselbst.sentrick.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class SbdProviders {

    public static List<String> list() throws IOException {
        List<String> providers = new ArrayList<String>();
        Enumeration<URL> urls = ClassLoader.getSystemResources("sentrick.classifiers");
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            String provider = null;
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            while ((provider = in.readLine()) != null) {
                provider = provider.trim();
                if (provider.length() > 0) providers.add(provider);
            }
            in.close();
        }
        return providers;
    }

    public static void main(String[] args) throws IOException {
        System.err.println("Sentence Boundary Detection Providers:");
        for (String provider : list()) System.err.println("  " + provider);
    }
}
