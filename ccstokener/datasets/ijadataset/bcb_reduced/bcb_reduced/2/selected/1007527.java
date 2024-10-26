package annone.engine.local;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import annone.util.AnnoneException;
import annone.util.Checks;
import annone.util.Text;

public class Resources {

    private final URL[] urls;

    public Resources() {
        try {
            File f1 = new File("../annone.libraries/bin/").getCanonicalFile();
            File f2 = new File("../it.novabyte.bill/bin/").getCanonicalFile();
            urls = new URL[] { f1.toURL(), f2.toURL() };
        } catch (Throwable xp) {
            throw new AnnoneException(Text.get("Resources initialization failed."), xp);
        }
    }

    private String[] findResources(String name) {
        Checks.notEmpty("name", name);
        if (name.startsWith("/")) throw new AnnoneException(Text.get("Name can''t start with \"{0}\".", "/"));
        if (name.endsWith("**")) throw new AnnoneException(Text.get("Name can''t end with \"{0}\".", "**"));
        boolean findDirectories = name.endsWith("/");
        if (findDirectories) name = name.substring(0, name.length() - 1);
        String[] parts = name.split("/");
        List<String> out = new ArrayList<String>(1);
        for (URL url : urls) traverse(out, url, "", parts, 0, findDirectories);
        return out.toArray(new String[out.size()]);
    }

    private void traverse(List<String> out, URL url, String path, String[] parts, int index, boolean findDirectories) {
        try {
            String part = parts[index];
            boolean last = (index == (parts.length - 1));
            if (last) {
                String[] list = list(url, !findDirectories, findDirectories);
                if (part.equals("*")) for (String item : list) out.add(path + item); else for (String item : list) if (match(item, part)) out.add(path + item);
            } else if (part.equals("**")) {
                String[] list = list(url, false, true);
                for (String item : list) {
                    URL url2 = new URL(url, item);
                    traverse(out, url2, path + item, parts, index + 1, findDirectories);
                    traverse(out, url2, path + item, parts, index, findDirectories);
                }
            } else if (part.equals("*")) {
                String[] list = list(url, false, true);
                for (String item : list) {
                    URL url2 = new URL(url, item);
                    traverse(out, url2, path + item, parts, index + 1, findDirectories);
                }
            } else {
                String[] list = list(url, false, true);
                for (String item : list) if (match(item.substring(0, item.length() - 1), part)) {
                    URL url2 = new URL(url, item);
                    traverse(out, url2, path + item, parts, index + 1, findDirectories);
                }
            }
        } catch (Throwable xp) {
            throw new AnnoneException(Text.get("Can''t traverse resources."), xp);
        }
    }

    private String[] list(URL url, final boolean files, final boolean directories) throws URISyntaxException {
        if (!files && !directories) return new String[0];
        String protocol = url.getProtocol();
        if ("file".equals(protocol)) {
            File dir = new File(url.toURI());
            if (files && directories) return dir.list(); else {
                File[] fileList;
                if (files) fileList = dir.listFiles(new FileFilter() {

                    @Override
                    public boolean accept(File pathname) {
                        return pathname.isFile();
                    }
                }); else fileList = dir.listFiles(new FileFilter() {

                    @Override
                    public boolean accept(File pathname) {
                        return pathname.isDirectory();
                    }
                });
                String[] list = new String[fileList.length];
                int i = 0;
                if (files) for (File f : fileList) list[i++] = f.getName(); else for (File f : fileList) list[i++] = f.getName() + "/";
                return list;
            }
        } else throw new AnnoneException(Text.get("Can''t handle protocol \"{0}\".", protocol));
    }

    private boolean match(String item, String part) {
        int i0 = part.indexOf('*');
        int i1 = part.lastIndexOf('*');
        if ((i0 == -1) && (i1 == -1)) return item.equals(part); else if (i0 == i1) return item.startsWith(part.substring(0, i0)) && item.endsWith(part.substring(i0 + 1)); else if ((i0 == 0) && (i1 == (part.length() - 1))) return item.contains(part.substring(i0 + 1, i1)); else return item.startsWith(part.substring(0, i0)) && item.endsWith(part.substring(i0 + 1)) && item.contains(part.substring(i0 + 1, i1));
    }

    public boolean isResource(String name) {
        return findResources(name).length != 0;
    }

    public String[] getResources(String name) {
        return findResources(name);
    }

    public InputStream getInputStream(String name) {
        for (URL url : urls) try {
            URL url2 = new URL(url, name);
            InputStream in = url2.openStream();
            if (in != null) return in;
        } catch (Throwable xp) {
        }
        return null;
    }
}
