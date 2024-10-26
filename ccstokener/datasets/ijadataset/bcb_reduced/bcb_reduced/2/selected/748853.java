package org.cofax.util;

import java.net.*;
import java.io.*;
import java.util.*;
import java.text.*;
import org.apache.oro.text.perl.Perl5Util;
import org.cofax.WysiwygTemplate;
import org.cofax.XMLConfig;

class WebToXML {

    /**
     * September, 2000
     * performs an HTTP get, then stuffs the document into a template
     * takes an XML configuration file as its one argument
     * modified 12/12/2000 to take hard-coded date from template
     * or insert current date
     * modified 4/2001 to deal with tilde chars
     * @author Philip Ravenscroft philip@infosculpture.com
     * @version Version 0.3
     */
    static String urlToGet;

    static String fileToWrite;

    static String templateFilename;

    static String articleFilename;

    static String section;

    static String pubName;

    static String noVersioning;

    static String disableIndex;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: WebToXML configFilename");
        } else {
            String xmlConfigFilename = args[0];
            readConfigFile(xmlConfigFilename);
            String results = "";
            try {
                results = getURL(urlToGet);
                String xmlToWrite = encodeAsXML(results, templateFilename);
                writeToFile(xmlToWrite, fileToWrite);
                System.out.println("ok, wrote to: " + fileToWrite);
            } catch (HTTPNotOKException ex) {
                System.err.println("Foreign server didn't respond correctly");
                System.err.println("exiting...");
            }
        }
    }

    public static String getURL(String URLToGet) throws HTTPNotOKException {
        String pageContents = "";
        URL url = null;
        HttpURLConnection connection = null;
        int responseCode = 0;
        try {
            url = new URL(URLToGet);
            connection = (HttpURLConnection) url.openConnection();
            responseCode = connection.getResponseCode();
        } catch (MalformedURLException ex) {
            System.err.println("Error: Malformed URL");
        } catch (IOException ex) {
            System.err.println("Error: IO Exception");
        }
        if (responseCode != 200) {
            throw new HTTPNotOKException(responseCode);
        } else {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    pageContents += inputLine;
                }
            } catch (IOException ex) {
                System.err.println("Error: IO Exception");
            }
        }
        return pageContents;
    }

    public static String encodeAsXML(String stringToEncode, String xmlTemplateFilename) {
        String escapedStringToEncode = convertSpecialChars(stringToEncode);
        File inputFile = new File(xmlTemplateFilename);
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile)));
        } catch (FileNotFoundException ex) {
            System.out.println("Error: Template file not found");
        }
        String inputLine;
        String fileContents = "";
        try {
            while ((inputLine = in.readLine()) != null) {
                fileContents += inputLine + "\n";
            }
        } catch (IOException ex) {
            System.err.println("Error: IOException found trying to read template file");
        }
        StringBuffer fileContentsStringBuffer = new StringBuffer(fileContents);
        HashMap glossary = new HashMap();
        glossary.put("date", returnDate());
        glossary.put("pubName", pubName);
        glossary.put("section", section);
        glossary.put("filename", articleFilename);
        glossary.put("body", escapedStringToEncode);
        glossary.put("noVersioning", noVersioning);
        glossary.put("disableIndex", disableIndex);
        WysiwygTemplate template = new WysiwygTemplate();
        String completeXML = template.applyTemplate(fileContentsStringBuffer, glossary);
        return completeXML;
    }

    public static void writeToFile(String stringToWrite, String fileName) {
        FileOutputStream out;
        PrintStream p;
        try {
            out = new FileOutputStream(fileName);
            p = new PrintStream(out);
            p.println(stringToWrite);
            p.close();
        } catch (Exception e) {
            System.err.println("Error writing to file");
        }
    }

    public static String convertSpecialChars(String linesToParse) {
        Perl5Util util = new Perl5Util();
        linesToParse = util.substitute("s/\n+/\n/g", linesToParse);
        linesToParse = util.substitute("s/\n/<br><br>\n/g", linesToParse);
        linesToParse = util.substitute("s/\021//g", linesToParse);
        linesToParse = util.substitute("s/\252 *//g", linesToParse);
        linesToParse = util.substitute("s/\317/-/g", linesToParse);
        linesToParse = util.substitute("s/\376//g", linesToParse);
        linesToParse = util.substitute("s/\004/<li>/g", linesToParse);
        linesToParse = util.substitute("s/</&lt;/g", linesToParse);
        linesToParse = util.substitute("s/>/&gt;/g", linesToParse);
        linesToParse = util.substitute("s/\\^C\\^D/ /g", linesToParse);
        linesToParse = util.substitute("s/\\^G//g", linesToParse);
        linesToParse = util.substitute("s/\\\\//g", linesToParse);
        linesToParse = util.substitute("s/\\'\\'/\"/g", linesToParse);
        linesToParse = util.substitute("s/\\`/\\'/g", linesToParse);
        linesToParse = util.substitute("s/\"/&quot;/g", linesToParse);
        linesToParse = util.substitute("s/&/&amp;/g", linesToParse);
        linesToParse = util.substitute("s/&amp;([a-zA-Z0-9]*;)/&$1/g", linesToParse);
        linesToParse = util.substitute("s/&nbsp;/&amp;nbsp;/g", linesToParse);
        linesToParse = util.substitute("s/&ntilde;/&#241;/g", linesToParse);
        return linesToParse;
    }

    public static String returnDate() {
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = dateFormat.format(now);
        return formattedDate;
    }

    public static void readConfigFile(String configFilename) {
        boolean didload = false;
        try {
            XMLConfig configFile = new XMLConfig();
            configFile.setXMLFileName(configFilename);
            didload = configFile.load();
            if (!didload) {
                System.err.println("Configuration file didn't load");
                System.err.println(configFile.getLastError());
            }
            urlToGet = configFile.getString("urlToGet");
            fileToWrite = configFile.getString("fileToWrite");
            templateFilename = configFile.getString("templateFilename");
            articleFilename = configFile.getString("articleFilename");
            section = configFile.getString("section");
            pubName = configFile.getString("pubName");
            noVersioning = configFile.getString("noVersioning");
            disableIndex = configFile.getString("disableIndex");
        } catch (Exception e) {
            System.err.println("Error reading configuration:");
            e.printStackTrace(System.err);
        }
    }
}

class HTTPNotOKException extends Exception {

    HTTPNotOKException(int responseCode) {
        super("HTTP 200 not returned: server responded with " + responseCode);
    }
}
