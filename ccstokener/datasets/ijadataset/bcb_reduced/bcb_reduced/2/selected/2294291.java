package name.vaites.ticketwatcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author john
 */
public class Utils {

    /**
     * Move a file from one folder to another.  The file is indexed
     * so files with the same base name can be moved without overwriting.<br>
     * Example: file.0.txt, file.1.txt, ...
     * 
     * @param fileToMove The filename including extension but without path.
     * @param sourceFolder Absolute or relative path to the source folder. 
     * @param targetFolder Absolute or relative path to the folder to move the file into
     * @return An Xml document object 
     */
    public static boolean moveFile(String fileToMove, String sourceFolder, String targetFolder) {
        String pattern = fileToMove.substring(0, fileToMove.lastIndexOf("."));
        String ext = fileToMove.substring(fileToMove.lastIndexOf("."), fileToMove.length());
        int count = Utils.getFileCount(targetFolder, pattern);
        File file = new File(sourceFolder + fileToMove);
        File dir = new File(targetFolder);
        return file.renameTo(new File(dir, pattern + "." + count + ext));
    }

    public static void createFile(String targetFolder, String fileToCreate, String data) {
        String pattern = fileToCreate.substring(0, fileToCreate.lastIndexOf("."));
        String ext = fileToCreate.substring(fileToCreate.lastIndexOf("."), fileToCreate.length());
        int count = Utils.getFileCount(targetFolder, pattern);
        try {
            FileWriter fstream = new FileWriter(targetFolder + pattern + "." + count + ext);
            BufferedWriter fout = new BufferedWriter(fstream);
            fout.write(data);
            fout.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    /**
     * Get the number of files in a folder with matching baseneame.
     * Used for indexing files when they are moved. 
     * @param folder
     * @param pattern
     * @return Number of files in folder with basename = pattern
     */
    public static int getFileCount(String folder, String pattern) {
        File f = new File(folder);
        File[] ff = f.listFiles();
        int count = 0;
        for (int j = 0; j < ff.length; j++) {
            if (ff[j].getName().startsWith(pattern)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Loads an xml file from disk 
     * @param filename full absolute or relative path of the xml file to be loaded
     * @param validating 
     * @return Loaded xml document 
     * @throws ParserConfigurationException 
     */
    public static Document parseXmlFile(String filename, boolean validating) throws ParserConfigurationException {
        try {
            System.out.println("## " + filename);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(validating);
            return factory.newDocumentBuilder().parse(new File(filename));
        } catch (SAXException e) {
            System.out.println(e);
        } catch (ParserConfigurationException e) {
            System.out.println(e);
        } catch (IOException e) {
            System.out.println(e);
        }
        return null;
    }

    /**
     * XmML document to string
     * @param doc
     * @return String representation of the XML document
     */
    public static String serialize(Document doc) {
        try {
            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
            LSSerializer writer = impl.createLSSerializer();
            writer.getDomConfig().setParameter("xml-declaration", false);
            writer.getDomConfig().setParameter("format-pretty-print", false);
            String str = writer.writeToString(doc);
            return str;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    /**
     * Get the DOM representation of an xml string
     * @param XmlString The xml string being loaded into the returned document.
     * @return An Xml document object 
     */
    public static Document stringToDom(String XmlString) {
        System.out.println("stringToDom()");
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(XmlString));
            Document d = builder.parse(is);
            return d;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return null;
    }

    /**
     * Read file data into a string
     * @param fileName The name of the file to read
     * @return The contents of the file as a string
     */
    public static String readFile(String fileName) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            String line;
            StringBuilder str = new StringBuilder();
            while ((line = in.readLine()) != null) {
                str.append(line);
            }
            in.close();
            return str.toString();
        } catch (IOException e) {
            System.out.print(e);
            return null;
        }
    }

    public static boolean fileOrFolderExists(String fileOrFolderName) {
        File file = new File(fileOrFolderName);
        return file.exists();
    }

    public static String inputStreamReader_readline(String host, int port, String str) {
        System.out.println("inputStreamReader_readline");
        try {
            Socket socket = new Socket(host, port);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.writeChars(str);
            StringBuilder message = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                message.append(line);
            }
            out.flush();
            out.close();
            in.close();
            socket.close();
            return message.toString();
        } catch (IOException e) {
            System.out.println(e);
            return null;
        }
    }

    public static String inputStreamReader_readUntilEndsWith(String host, int port, String str, String strEndsWith) {
        System.out.println("inputStreamReader_readUntilEndsWith");
        try {
            Socket socket = new Socket(host, port);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            byte[] b = str.getBytes();
            out.write(b);
            StringBuilder message = new StringBuilder();
            while (!message.toString().endsWith(strEndsWith)) {
                message.append((char) in.read());
            }
            out.flush();
            out.close();
            in.close();
            socket.close();
            return message.toString();
        } catch (IOException e) {
            System.out.println("ERROR: host=" + host + " port=" + port + " error=" + e);
            return null;
        }
    }

    public static String inputStreamReader_timeout(String host, int port, String str, int timeOut) {
        System.out.println("inputStreamReader_timeout");
        try {
            Socket socket = new Socket(host, port);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.writeChars(str);
            StringBuilder message = new StringBuilder();
            int i;
            while ((i = in.read()) != -1) {
                message.append((char) i);
            }
            out.flush();
            out.close();
            in.close();
            socket.close();
            return message.toString();
        } catch (IOException e) {
            System.out.println(e);
            return null;
        }
    }

    /**
     * Post data to a web service using HttpUrlConnection
     * @param targetURL
     * @param urlParameters In the form keyword1=value1&keyword2=value2
     * @return Web service response
     */
    public static String httpUrlConnection_post(String targetURL, String urlParameters) {
        System.out.println("httpUrlConnection_post");
        URL url;
        HttpURLConnection connection = null;
        try {
            url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");
            connection.setUseCaches(false);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            System.out.print(e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
