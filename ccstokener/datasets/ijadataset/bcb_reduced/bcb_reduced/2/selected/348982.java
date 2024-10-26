package jvs.vfs.resource;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import jvs.vfs.IFileBaseImpl;

/**
 * @author qiangli
 * 
 */
public class UrlFileImpl extends IFileBaseImpl {

    private String path = null;

    public UrlFileImpl(URI uri) {
        super(uri);
        path = uri.toString();
        path = path.substring(4).trim();
    }

    public boolean create() {
        return false;
    }

    public boolean delete() {
        return false;
    }

    private String getPath() {
        return path;
    }

    public boolean exists() {
        return true;
    }

    public InputStream getInputStream() {
        try {
            String p = getPath();
            URL url = new URL(p);
            return url.openStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public OutputStream getOutputStream() {
        return null;
    }

    public boolean isDirectory() {
        return getPath().endsWith("/");
    }

    private String[] list = null;

    private String[] read(String path) throws Exception {
        final String[] names = { "index.txt", "", "index.html", "index.htm" };
        String[] list = null;
        for (int i = 0; i < names.length; i++) {
            URL url = new URL(path + names[i]);
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                StringBuffer sb = new StringBuffer();
                String s = null;
                while ((s = in.readLine()) != null) {
                    s = s.trim();
                    if (s.length() > 0) {
                        sb.append(s + "\n");
                    }
                }
                in.close();
                if (sb.indexOf("<") != -1 && sb.indexOf(">") != -1) {
                    List links = LinkExtractor.scan(url, sb.toString());
                    HashSet set = new HashSet();
                    int prefixLen = path.length();
                    for (Iterator it = links.iterator(); it.hasNext(); ) {
                        String link = it.next().toString();
                        if (!link.startsWith(path)) {
                            continue;
                        }
                        link = link.substring(prefixLen);
                        int idx = link.indexOf("/");
                        int idxq = link.indexOf("?");
                        if (idx > 0 && (idxq == -1 || idx < idxq)) {
                            set.add(link.substring(0, idx + 1));
                        } else {
                            set.add(link);
                        }
                    }
                    list = (String[]) set.toArray(new String[0]);
                } else {
                    list = sb.toString().split("\n");
                }
                return list;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                continue;
            }
        }
        return new String[0];
    }

    public synchronized String[] list() {
        if (list == null) {
            try {
                String p = getPath();
                list = read(p);
            } catch (Exception e) {
                e.printStackTrace();
                list = new String[0];
            }
        }
        return list;
    }

    public boolean mkdir() {
        return false;
    }

    public boolean move(URI uri) {
        return false;
    }
}
