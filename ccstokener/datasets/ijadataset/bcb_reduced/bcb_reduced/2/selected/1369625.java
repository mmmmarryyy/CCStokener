package net.sf.osadm.docbook;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class DocumentAnalyzer {

    private static SAXBuilder builder = new SAXBuilder();

    public static void main(String[] argArray) {
        if (argArray.length == 0) {
            System.out.println("Usage: java  -jar doc-analyzer.jar  url | file");
        }
        List<URL> urlList = new LinkedList<URL>();
        for (String urlStr : argArray) {
            if (!(urlStr.startsWith("http") || urlStr.startsWith("file"))) {
                if (urlStr.indexOf("*") > -1) {
                    if (urlStr.indexOf("**") > -1) {
                    }
                    continue;
                } else {
                    if (!urlStr.startsWith("/")) {
                        File workDir = new File(System.getProperty("user.dir"));
                        urlStr = workDir.getPath() + "/" + urlStr;
                    }
                    urlStr = "file:" + urlStr;
                }
            }
            try {
                URL url = new URL(urlStr);
                urlList.add(url);
            } catch (MalformedURLException murle) {
                System.err.println(murle);
            }
        }
        for (URL url : urlList) {
            try {
                Document doc = builder.build(url.openStream());
                Element element = doc.getRootElement();
                Map<String, Long> numberOfElementMap = countElement(element);
                System.out.println("Overview of tags in '" + url + "':");
                for (String elementName : new TreeSet<String>(numberOfElementMap.keySet())) {
                    System.out.println("  " + elementName + ": " + numberOfElementMap.get(elementName));
                }
            } catch (JDOMException jdome) {
                System.err.println(jdome.getMessage());
            } catch (IOException ioe) {
                System.err.println(ioe.getMessage());
            }
        }
    }

    private static Map<String, Long> countElement(Element element) {
        return countElement(new HashMap<String, Long>(), element);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Long> countElement(Map<String, Long> numberOfElementMap, Element element) {
        if (numberOfElementMap.get(element.getName()) == null) {
            numberOfElementMap.put(element.getName(), new Long(1));
        } else {
            numberOfElementMap.put(element.getName(), numberOfElementMap.get(element.getName()) + 1);
        }
        if (element.getChildren().size() > 0) {
            Iterator<Element> iter = element.getChildren().iterator();
            while (iter.hasNext()) {
                Element childElement = iter.next();
                countElement(numberOfElementMap, childElement);
            }
        }
        return numberOfElementMap;
    }
}
