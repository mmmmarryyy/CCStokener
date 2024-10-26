package updaters;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.net.URL;
import updaters.utils.IOUtil;

public class Repository {

    public Repository(String urlBase) {
        urlBase_ = fixURL(urlBase) + "/";
    }

    public Version getLatestVersion() throws IOException {
        System.err.println("Getting latest version from LatestVersion.ver");
        BufferedReader verFile = getCharFile("LatestVersion.ver");
        return new Version(this, verFile.readLine());
    }

    public FileDiff getDiff(FileInfo fileDiffInfo, Version version) {
        return new ReplaceFileDiff(fileDiffInfo, version);
    }

    public BufferedReader getCharFile(String filename) throws IOException {
        return new BufferedReader(new InputStreamReader(getBinaryFile(filename)));
    }

    public InputStream getBinaryFile(String filename) throws IOException {
        filename = fixURL(filename);
        String url = IOUtil.delimitURL(IOUtil.fixJarURL(urlBase_ + filename));
        System.err.println("Retrieving url " + url + " from server");
        InputStream retval = new BufferedInputStream(new URL(url).openStream());
        return retval;
    }

    public static String fixURL(String url) {
        return IOUtil.trimFileSeparator(url);
    }

    String urlBase_;
}
