package utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Vector;

public class AppUpdater {

    Vector<String> packages = null;

    String author = null;

    String appName = null;

    String changeLog = null;

    long major = 0;

    long minor = 0;

    long release = 0;

    long build = 0;

    String u;

    public String getAuthor() {
        return author;
    }

    public String getAppName() {
        return appName;
    }

    public String getChangeLog() {
        return changeLog;
    }

    public long getMajor() {
        return major;
    }

    public long getMinor() {
        return minor;
    }

    public long getRelease() {
        return release;
    }

    public long getBuild() {
        return build;
    }

    public void setUrl(String url) {
        if (url == null) {
            throw new NullPointerException();
        }
        u = url;
    }

    public boolean shouldPerform() {
        if (packages.size() > 0 && author != null && author.length() > 0 && appName != null && appName.length() > 0 && (major != 0 || minor != 0)) {
            return false;
        }
        return true;
    }

    public String versionToString() {
        return getMajor() + "." + getMinor() + "." + getRelease() + "." + getBuild();
    }

    public boolean isUpdate(long major, long minor, long release, long build) {
        String localVersion, serverVersion;
        if (major == -1) {
            major = 0;
        }
        if (minor == -1) {
            minor = 0;
        }
        if (release == -1) {
            release = 0;
        }
        if (build == -1) {
            build = 0;
        }
        localVersion = "" + major + minor + release + build;
        serverVersion = "" + getMajor() + getMinor() + getRelease() + getBuild();
        if (Long.parseLong(serverVersion) > Long.parseLong(localVersion)) {
            return true;
        }
        return false;
    }

    public String getPackageUrlForOS(String os) {
        String osPkg;
        int pos;
        if (os == null) {
            throw new NullPointerException("os can\'t be null");
        }
        if (packages == null || packages.size() == 0) {
            throw new NullPointerException("No packages, try perform() first");
        }
        osPkg = "app.os." + os + ".package";
        osPkg = osPkg.toLowerCase();
        for (String pkg : packages) {
            if (pkg.length() > 0 && (pos = pkg.indexOf('=')) != -1 && pkg.indexOf(osPkg) == 0) {
                return pkg.substring(pos + 1, pkg.length()).trim();
            }
        }
        return null;
    }

    void reset() {
        author = null;
        appName = null;
        changeLog = null;
        packages = new Vector<String>();
        major = 0;
        minor = 0;
        release = 0;
        build = 0;
    }

    public AppUpdater(String url) {
        if (url == null) {
            throw new NullPointerException();
        }
        this.u = url;
    }

    public boolean perform() throws IOException {
        URLConnection urlConn;
        StringBuffer sb;
        InputStream is;
        byte[] bytes;
        int len;
        URL url;
        url = new URL(u);
        urlConn = url.openConnection();
        urlConn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 5.0; H010818)");
        urlConn.setConnectTimeout(7000);
        is = urlConn.getInputStream();
        sb = new StringBuffer();
        bytes = new byte[4096];
        while ((len = is.read(bytes)) > 0) {
            sb.append(new String(bytes, 0, len));
            Arrays.fill(bytes, (byte) 0);
        }
        is.close();
        return decode(sb.toString().getBytes());
    }

    public boolean decode(byte[] bytes) throws IOException {
        BufferedReader br;
        String line, key, arg;
        int pos;
        br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes)));
        reset();
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.indexOf("app.start") == 0) {
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.indexOf("app.end") == 0) {
                        break;
                    }
                    if ((pos = line.indexOf(':')) == -1) {
                        continue;
                    }
                    arg = line.substring(pos + 1, line.length()).trim();
                    key = line.substring(0, pos).trim().toLowerCase();
                    if (key.equals("app.name")) {
                        appName = arg;
                    } else if (key.equals("app.author")) {
                        author = arg;
                    } else if (key.equals("app.version.major")) {
                        try {
                            major = Integer.parseInt(arg);
                        } catch (NumberFormatException nfx) {
                            major = 0;
                        }
                    } else if (key.equals("app.version.minor")) {
                        try {
                            minor = Integer.parseInt(arg);
                        } catch (NumberFormatException nfx) {
                            minor = 0;
                        }
                    } else if (key.equals("app.version.release")) {
                        try {
                            release = Integer.parseInt(arg);
                        } catch (NumberFormatException nfx) {
                            release = 0;
                        }
                    } else if (key.equals("app.version.build")) {
                        try {
                            build = Integer.parseInt(arg);
                        } catch (NumberFormatException nfx) {
                            build = 0;
                        }
                    } else if (key.equals("app.changelog.start")) {
                        changeLog = "";
                        while ((line = br.readLine()) != null) {
                            if (line == null) {
                                line = "";
                            }
                            if (line.indexOf("app.changelog.end") == 0) {
                                break;
                            }
                            changeLog += line + "\n";
                        }
                        changeLog = changeLog.trim();
                    } else if (key.indexOf("app.os.") == 0 && key.indexOf(".package") != -1) {
                        packages.add(key + "=" + arg);
                    }
                }
            }
        }
        br.close();
        return true;
    }
}
