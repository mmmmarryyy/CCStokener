import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Class CfgUpdater 
 * 
 * The update method will retrieve a config file from the supplied url and if a 
 * config file allready exists on disk it will compare the versions.
 * If no config file is found on disk it will use the one just downloaded.
 */
public class CfgUpdater {

    private String result = null;

    private String fileName;

    /**
     * update(String target, String cfgVersion)
     * the update methode will retrieve a config file from the supplied url and 
     * if needed overwrite the current file
     * 
     * @param target the url from where to retrieve the new config file
     * @param cfgVersion the version of the current config file
     */
    public void update(String target, String cfgVersion) throws MalformedURLException, FileNotFoundException, IOException {
        URL url = new URL(target);
        String[] urlSplit = target.split("/");
        this.fileName = urlSplit[urlSplit.length - 1];
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(Main.getHomeDir() + "tmp-" + this.fileName));
        URLConnection urlConnection = url.openConnection();
        InputStream in = urlConnection.getInputStream();
        byte[] buffer = new byte[1024];
        int numRead;
        while ((numRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, numRead);
        }
        in.close();
        out.close();
        XMLController xmlC = new XMLController();
        String newFileVersion = xmlC.readCfgVersion(Main.getHomeDir() + "tmp-" + this.fileName);
        if (new File(Main.getHomeDir() + this.fileName).exists()) {
            if (Double.parseDouble(newFileVersion) > Double.parseDouble(cfgVersion)) {
                new File(Main.getHomeDir() + this.fileName).delete();
                new File(Main.getHomeDir() + "tmp-" + this.fileName).renameTo(new File(Main.getHomeDir() + this.fileName));
                this.result = "ConfigFile Updated Succesfully";
            } else {
                new File(Main.getHomeDir() + "tmp-" + this.fileName).delete();
            }
        } else {
            new File(Main.getHomeDir() + "tmp-" + this.fileName).renameTo(new File(Main.getHomeDir() + this.fileName));
            this.result = "ConfigFile Loaded";
        }
    }

    /**
     * @return a string containing the result of the last update attempt.
     */
    public String getResult() {
        return this.result;
    }
}
