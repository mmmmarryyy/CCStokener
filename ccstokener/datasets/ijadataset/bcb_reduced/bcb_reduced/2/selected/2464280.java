package net.sf.osadm.docbook.annotation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import org.jdom.Document;

public class AmayaTeaAnnotation {

    public static void main(String[] args) {
        File homePath = new File(System.getProperty("user.home"));
        File annotationDataPath = new File(new File(homePath, ".amaya"), "annotations");
        if (annotationDataPath.isDirectory()) {
            System.out.println("Found location: " + annotationDataPath);
        }
        File annotationIndexFile = new File(annotationDataPath, "annot.index");
        Map<String, String> annotationIndexMap = null;
        try {
            annotationIndexMap = loadAnnotationIndexMap(annotationIndexFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        if (annotationIndexMap.size() > 0) {
            for (String documentLocation : annotationIndexMap.keySet()) {
                System.out.println("\n\ndocument location: " + documentLocation);
                String rdfLocation = annotationIndexMap.get(documentLocation);
                if (rdfLocation != null && (rdfLocation.startsWith("file") || rdfLocation.startsWith("http"))) {
                    try {
                        URL url = new URL(rdfLocation);
                        InputStreamReader isr = new InputStreamReader(url.openStream());
                        showFile(isr);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        URL url = null;
        try {
            url = new URL("http://develvm03/svn/prototypes/ac_documentation/tags/rev_20071011/src/docbkx/interface/interface_engine/interface_engine_book.xml");
            System.out.println("Created url '" + url + "'");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void showFile(Reader reader) {
        LineNumberReader lnr = null;
        if (reader instanceof BufferedReader) {
            lnr = (LineNumberReader) reader;
        } else {
            lnr = new LineNumberReader(reader);
        }
        String line = null;
        try {
            while ((line = lnr.readLine()) != null) {
                System.out.println(lnr.getLineNumber() + "  " + line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, String> loadAnnotationIndexMap(File annotationIndexFile) throws IOException {
        return loadAnnotationIndexMap(new FileReader(annotationIndexFile));
    }

    private static Map<String, String> loadAnnotationIndexMap(Reader readerx) throws IOException {
        final Map<String, String> annotationIndexMap = new HashMap<String, String>();
        LineNumberReader reader = new LineNumberReader(readerx);
        String line = null;
        while ((line = reader.readLine()) != null) {
            StringTokenizer tokenizer = new StringTokenizer(line);
            String documentLocation = null;
            String rdfLocation = null;
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                if (documentLocation == null) {
                    documentLocation = token;
                    continue;
                }
                if (rdfLocation == null) {
                    rdfLocation = token;
                    continue;
                }
                System.out.println("Unexpected token found '" + token + "'.");
            }
            System.out.println("doc:  '" + documentLocation + "'  rdf:  '" + rdfLocation + "'.");
            annotationIndexMap.put(documentLocation, rdfLocation);
        }
        return annotationIndexMap;
    }

    public Document loadDocument(URL url) {
        return null;
    }
}
