package util;

import com.jme.system.DisplaySystem;
import com.jme.system.dummy.DummySystemProvider;
import com.jme.util.export.Savable;
import com.jme.util.export.binary.BinaryExporter;
import com.jmex.model.ogrexml.OgreLoader;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**h
 *
 * @author jmadar
 */
public class Sysutil {

    /**
     * Get a URL representation of a resource.
     * @param resource
     * @return the URL
     */
    public static URL getURL(String resource) {
        java.net.URL resourceURL;
        if (resource.startsWith("http")) {
            try {
                resourceURL = new java.net.URL(resource);
            } catch (Exception e) {
                e.printStackTrace();
                resourceURL = null;
            }
        } else {
            java.lang.ClassLoader cldr = Sysutil.class.getClassLoader();
            resourceURL = cldr.getResource(resource);
            if (resourceURL == null) {
                try {
                    resourceURL = (new File(resource)).toURI().toURL();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    resourceURL = null;
                }
            }
        }
        return resourceURL;
    }

    /**
     * Read the entire URL into a string
     * @param url
     * @return
     */
    public static String readUrl(String urlString) {
        try {
            java.net.URL url = new java.net.URL(urlString);
            BufferedReader br = null;
            if (url != null) {
                br = new BufferedReader(new InputStreamReader(url.openStream()));
            }
            StringBuffer fileString = new StringBuffer();
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                fileString.append(line + "\n");
            }
            return fileString.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static void executeCommand(String command) {
        String osName = System.getProperty("os.name");
        String[] cmdArray;
        if (osName.startsWith("Windows")) {
            cmdArray = new String[] { "cmd", "/C", command };
        } else {
            cmdArray = new String[] { "bash", "-c", command };
        }
        Runtime r = Runtime.getRuntime();
        try {
            Process p = r.exec(cmdArray, null, null);
            InputStream in = p.getInputStream();
            InputStream err = p.getErrorStream();
            BufferedInputStream buf = new BufferedInputStream(in);
            InputStreamReader inread = new InputStreamReader(buf);
            BufferedReader bufferedreader = new BufferedReader(inread);
            BufferedReader errReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(err)));
            String line;
            while ((line = bufferedreader.readLine()) != null) {
                System.out.println(line);
            }
            while ((line = errReader.readLine()) != null) {
                System.out.println(line);
            }
            try {
                if (p.waitFor() != 0) {
                    System.out.println("exit value = " + p.exitValue());
                }
            } catch (InterruptedException e) {
                System.out.println(e);
            } finally {
                bufferedreader.close();
                inread.close();
                buf.close();
                in.close();
                errReader.close();
            }
        } catch (Exception e) {
            System.out.println("Error :" + e);
        }
    }

    public static String getMacAddress() throws IOException {
        String command = null;
        String osName = System.getProperty("os.name");
        Pattern p = null;
        if (osName.startsWith("Windows")) {
            command = "ipconfig /all";
            p = Pattern.compile(".*Physical Address.*: (.*)");
        } else {
            command = "ifconfig";
            p = Pattern.compile(".*ether (.*)\\s*");
        }
        Process pid = Runtime.getRuntime().exec(command);
        BufferedReader in = new BufferedReader(new InputStreamReader(pid.getInputStream()));
        if (p != null) {
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    return m.group(1).substring(0, 17);
                }
            }
        }
        return InetAddress.getLocalHost().getHostAddress();
    }

    public static void main(String args[]) {
        DisplaySystem.getDisplaySystem(DummySystemProvider.DUMMY_SYSTEM_IDENTIFIER);
        String file = "/Users/jmadar/Downloads/skeleton/Cube.004.mesh.xml";
        String prefix = file.substring(0, file.lastIndexOf("."));
        String[] files = { file, prefix + ".jme" };
        System.out.println(files[0] + " to " + files[1]);
        try {
            OgreLoader loader = new OgreLoader();
            java.net.URL url = new File(files[0]).toURI().toURL();
            System.out.println(url);
            Savable s = loader.loadModel(url, null);
            BinaryExporter.getInstance().save(s, new File(files[1]));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
