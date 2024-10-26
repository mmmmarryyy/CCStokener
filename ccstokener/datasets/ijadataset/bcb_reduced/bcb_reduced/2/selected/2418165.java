package org.lindenb.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.tools.ant.BuildException;

/**
 * @author pierre
 * &lt;taskdef name="mytask" classname="org.lindenb.ant.CompileInfoTask"/&gt;
 */
public class WGetTask extends org.apache.tools.ant.Task {

    private String urlStr = null;

    private File toFile = null;

    private File toDir = null;

    private String baseStr = null;

    public WGetTask() {
    }

    public void setUrl(String url) {
        this.urlStr = url;
    }

    public void setBase(String baseStr) {
        this.baseStr = baseStr;
    }

    public void setTofile(File toFile) {
        this.toFile = toFile;
    }

    public void setTodir(File toDir) {
        this.toDir = toDir;
    }

    @Override
    public void execute() throws BuildException {
        if (this.toFile == null && this.toDir == null) throw new BuildException("Missing Destination File/Dir");
        if (this.toFile != null && this.toDir != null) throw new BuildException("Both Defined Destination File/Dir");
        if (this.urlStr == null) throw new BuildException("Missing URL");
        URL base = null;
        try {
            if (baseStr != null) base = new URL(this.baseStr + (baseStr.endsWith("/") ? "" : "/"));
        } catch (MalformedURLException e) {
            throw new BuildException(e);
        }
        String tokens[] = this.urlStr.split("[ \t\n]+");
        try {
            for (String nextURL : tokens) {
                nextURL = nextURL.trim();
                if (nextURL.length() == 0) continue;
                URL url = null;
                try {
                    url = new URL(base, nextURL);
                } catch (MalformedURLException e) {
                    throw new BuildException(e);
                }
                File dest = null;
                if (this.toDir != null) {
                    String file = url.getFile();
                    int i = file.lastIndexOf('/');
                    if (i != -1 && i + 1 != file.length()) file = file.substring(i + 1);
                    dest = new File(this.toDir, file);
                } else {
                    dest = this.toFile;
                }
                if (dest.exists()) continue;
                byte buff[] = new byte[2048];
                FileOutputStream out = new FileOutputStream(dest);
                InputStream in = url.openStream();
                int n = 0;
                while ((n = in.read(buff)) != -1) {
                    out.write(buff, 0, n);
                }
                in.close();
                out.flush();
                out.close();
                System.out.println("Downloaded " + url + " to " + dest);
            }
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }
}
