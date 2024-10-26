package jvs.vfs.resource;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author qiangli
 * 
 */
public class LinkExtractor {

    private static final byte[] _base = { '<', 'b', 'a', 's', 'e' };

    private static final byte[] _a = { '<', 'a' };

    private static final byte[] _href = { 'h', 'r', 'e', 'f' };

    private static final byte[] _eq = { '=' };

    private static final byte[] _delim = { '\t', '\r', '\n', '"', '>' };

    public LinkExtractor() {
    }

    public static List scan(URL url) throws Exception {
        ArrayList al = null;
        try {
            int maxlen = 1024 * 1024;
            byte[] content = new byte[maxlen];
            int len = get(url, content);
            al = parse(content, len, url);
            resolve(url, al);
        } catch (Exception e) {
            throw e;
        }
        return al;
    }

    public static List scan(URL base, String content) throws Exception {
        ArrayList al = null;
        try {
            al = parse(content.getBytes(), content.length(), base);
            resolve(base, al);
        } catch (Exception e) {
            throw e;
        }
        return al;
    }

    private static void resolve(URL base, ArrayList urls) {
        Iterator it = urls.iterator();
        while (it.hasNext()) {
            URL u = (URL) it.next();
            if (!isSameSite(u, base)) {
                it.remove();
                continue;
            }
        }
    }

    private static boolean isSameSite(URL u1, URL u2) {
        boolean rc = false;
        try {
            rc = u1.getProtocol().equals(u2.getProtocol()) && u1.getHost().equals(u2.getHost()) && (u1.getPort() == u2.getPort());
        } catch (Exception e) {
        }
        return rc;
    }

    private static int indexOf(byte[] c, byte[] tag, int off, int len) {
        for (int i = off, j = 0; i < len && i < c.length; i++) {
            if (c[i] == tag[j]) {
                j++;
                if (j == tag.length) {
                    return (i - tag.length + 1);
                }
            } else {
                j = 0;
            }
        }
        return -1;
    }

    private static int index(byte[] content, byte[] tag, int off, int len) {
        int idx = -1;
        if ((idx = indexOf(content, tag, off, len)) == -1) {
            return idx;
        }
        idx += tag.length;
        if ((idx = indexOf(content, _href, idx, len)) == -1) {
            return idx;
        }
        idx += _href.length;
        if ((idx = indexOf(content, _eq, idx, len)) == -1) {
            return idx;
        }
        idx += _eq.length;
        return idx;
    }

    private static boolean isDelimiter(byte b) {
        for (int i = 0; i < _delim.length; i++) {
            if (b == _delim[i]) {
                return true;
            }
        }
        return false;
    }

    private static URL extract(byte[] content, int off, int len, URL base) {
        URL url = null;
        try {
            byte[] _url = new byte[1024];
            String link = null;
            boolean token = false;
            for (int i = off, j = 0; i < len && i < content.length && j < _url.length; i++) {
                if (isDelimiter(content[i]) && !token) {
                    continue;
                }
                if (isDelimiter(content[i]) && token) {
                    link = new String(_url, 0, j);
                    break;
                }
                token = true;
                _url[j++] = content[i];
            }
            url = new URL(base, link);
            url = new URL(url, url.getFile());
            String p = url.getProtocol();
            if (!p.equals("http")) {
                return null;
            }
        } catch (MalformedURLException e) {
        } catch (Exception e) {
        }
        return url;
    }

    private static void toLowerCase(byte[] c) {
        for (int i = 0; i < c.length; i++) {
            if (c[i] >= 'A' && c[i] <= 'Z') {
                c[i] = (byte) (c[i] + ('a' - 'A'));
            }
        }
    }

    private static ArrayList parse(byte[] c, int len, URL base) {
        ArrayList urls = new ArrayList();
        int idx = index(c, _base, 0, len);
        if (idx != -1) {
            URL url = extract(c, idx, len, base);
            if (url != null) {
                url = base;
            }
        }
        idx = 0;
        while ((idx = index(c, _a, idx, len)) != -1) {
            URL url = extract(c, idx, len, base);
            if (url != null) {
                urls.add(url);
            }
        }
        return urls;
    }

    private static int read(InputStream in, byte[] b) throws IOException {
        for (int total = 0, nread = 0; total < b.length; total += nread) {
            nread = in.read(b, total, b.length - total);
            if (nread == -1) {
                return total;
            }
        }
        return b.length;
    }

    private static int get(URL url, byte[] content) throws IOException {
        int len = -1;
        InputStream in = null;
        try {
            in = new BufferedInputStream(url.openStream());
            String type = URLConnection.guessContentTypeFromStream(in);
            if (type == null || type.compareTo("text/html") != 0) {
                return -1;
            }
            len = read(in, content);
        } catch (IOException e) {
            throw e;
        } finally {
            close(in);
        }
        return len;
    }

    private static void close(InputStream in) {
        try {
            in.close();
        } catch (Exception e) {
        }
    }
}
