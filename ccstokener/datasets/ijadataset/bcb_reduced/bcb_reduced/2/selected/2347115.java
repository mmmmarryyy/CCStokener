package uk.co.greenbarsoft.twitcher.deployment;

import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.MalformedURLException;
import java.net.URL;
import uk.co.greenbarsoft.twitcher.BuildMonitor;

public class BootFromJar {

    public static void main(String[] args) throws MalformedURLException {
        if (args.length > 0 && args[0].equals("-version")) {
            System.out.println(new BootFromJar().getVersioningStringFromFileReturnedByClassloader("/version.stamp.txt"));
        } else {
            BuildMonitor.main(args);
        }
    }

    public String getVersioningStringFromFileReturnedByClassloader(String pathToFile) {
        StringBuffer result = new StringBuffer();
        URL url = this.getClass().getResource(pathToFile);
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(url.openStream());
            LineNumberReader lineNumberReader = new LineNumberReader(inputStreamReader);
            String line = lineNumberReader.readLine();
            while (line != null) {
                result.append(line + "\n");
                line = lineNumberReader.readLine();
            }
            lineNumberReader.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read from \"" + pathToFile + "\"", e);
        }
        return result.toString();
    }
}
