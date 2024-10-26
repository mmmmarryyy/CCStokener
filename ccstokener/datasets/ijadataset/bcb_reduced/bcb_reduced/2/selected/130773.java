package jp.dodododo.reloadable.servlet.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarInputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jp.dodododo.reloadable.net.URLClassLoaderEx;
import jp.dodododo.reloadable.util.ContextClassLoaderUtils;
import jp.dodododo.reloadable.util.StringUtils;

public class CLServlet extends HttpServlet {

    private static final long serialVersionUID = -8494183828672461746L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        process(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        process(req, res);
    }

    private void process(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String exceptionString = req.getParameter("ex");
        String resourcePathString = req.getParameter("rp");
        if (StringUtils.isEmpty(exceptionString) == true) {
            exceptionString = "";
        }
        if (StringUtils.isEmpty(resourcePathString) == true) {
            resourcePathString = "";
        }
        res.setContentType("text/html; charset=UTF-8");
        PrintWriter w = res.getWriter();
        w.write("<html>");
        w.write("<head>");
        w.write("<title>CLServlet</title>");
        w.write("<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">");
        w.write("</head>");
        w.write("<body>");
        w.write("<form method=\"POST\" action=\"CLServlet#result\">");
        w.write("Exception:");
        w.write("<br />");
        w.write("<textarea name=\"ex\" cols=\"100\" rows=\"10\">");
        w.write(exceptionString);
        w.write("</textarea>");
        w.write("<br />");
        w.write("ResourcePath:");
        w.write("<br />");
        w.write("<input name=\"rp\" type=\"text\" size=\"100\" value=\"" + resourcePathString + "\" />");
        w.write("<br />");
        w.write("<input type=\"submit\" value=\"submit\">");
        w.write("<br />");
        Set<String> resourceNames = new TreeSet<String>();
        if (StringUtils.isEmpty(exceptionString) == false) {
            resourceNames = getResourceNames(exceptionString);
        }
        if (StringUtils.isEmpty(resourcePathString) == false) {
            resourceNames.add(resourcePathString);
        }
        if (resourceNames.isEmpty() == false) {
            writeResult(w, resourceNames);
        }
        w.write("</form>");
        w.write("</body>");
        w.write("</html>");
    }

    private void writeResult(PrintWriter w, Set<String> resourceNames) {
        w.write("<a name=\"result\" ></a>");
        ClassLoader loader = ContextClassLoaderUtils.getContextClassLoader();
        if (loader instanceof URLClassLoaderEx) {
            URLClassLoaderEx ex = URLClassLoaderEx.class.cast(loader);
            URL[] ls = ex.getURLs();
            for (int i = 0; i < ls.length; i++) {
                URL url = ls[i];
                try {
                    validateJarFile(url);
                } catch (IOException normal) {
                    writeInvalidJarInfo(w, url);
                }
            }
        }
        w.write("<br />");
        w.write("<font color=\"red\">red:Not Found.<font><br />");
        w.write("<font color=\"black\">black:Can not Reload.<font><br />");
        w.write("<font color=\"blue\">blue:Can Reload.<font><br />");
        writeTable(w, resourceNames);
    }

    private void writeInvalidJarInfo(PrintWriter w, URL url) {
        w.write("<hr />");
        w.write("<font color=\"red\" size=\"+2\">");
        w.write("Invalid jar :" + url);
        w.write("</font>");
        w.write("<hr />");
    }

    private void writeTable(PrintWriter w, Set<String> resourceNames) {
        ClassLoader loader = ContextClassLoaderUtils.getContextClassLoader();
        ClassLoader webLoader = getWebAppClassLoader(loader);
        w.write("<table border=\"1\" cellspacing=\"0\" cellpadding=\"2\">");
        w.write("<thead><tr><th>Resource</th><th>URL</th></tr></thead>");
        w.write("<tbody>");
        for (Iterator<String> iter = resourceNames.iterator(); iter.hasNext(); ) {
            String path = iter.next();
            URL resource = loader.getResource(path);
            URL webResource = webLoader.getResource(path);
            String prefix = "<font color=\"blue\">";
            String sufix = "</font>";
            if (resource == null) {
                prefix = "<font color=\"red\">";
            } else if (isReloadableResource(resource, webResource) == false) {
                prefix = "<font color=\"black\">";
            }
            w.write("<tr>");
            w.write("<td>");
            w.write(prefix + path + sufix);
            w.write("</td>");
            w.write("<td>");
            if (resource != null) {
                w.write(prefix + resource + sufix);
            } else {
                w.write(prefix + "Not Found" + sufix);
            }
            w.write("</td>");
            w.write("</tr>");
        }
        w.write("</tbody>");
        w.write("</table>");
    }

    private boolean isReloadableResource(URL resource, URL webResource) {
        if (resource == null) {
            return false;
        }
        if (webResource == null) {
            return true;
        }
        if (resource.equals(webResource) == false) {
            return true;
        } else {
            return false;
        }
    }

    private ClassLoader getWebAppClassLoader(ClassLoader loader) {
        if (loader == null) {
            return null;
        }
        if (loader instanceof URLClassLoaderEx) {
            ClassLoader parent = loader.getParent();
            if (parent instanceof URLClassLoaderEx == false) {
                return parent;
            }
        }
        ClassLoader ret = getWebAppClassLoader(loader.getParent());
        if (ret != null) {
            return ret;
        } else {
            return loader;
        }
    }

    private static void validateJarFile(URL url) throws IOException {
        InputStream stream = url.openStream();
        JarInputStream jarStream = new JarInputStream(stream, true);
        try {
            while (null != jarStream.getNextEntry()) {
            }
        } finally {
            try {
                jarStream.close();
            } catch (Exception ignore) {
            }
        }
    }

    private static Set<String> getResourceNames(String exceptionString) {
        Set<String> ret = new TreeSet<String>();
        if (StringUtils.isEmpty(exceptionString) == true) {
            return ret;
        }
        String[] strings = exceptionString.split(" |\\:|\\(|\\)|\\n|\\r|\\t|\\<|\\>|\\[|\\]");
        for (int i = 0; i < strings.length; i++) {
            String string = strings[i];
            if (StringUtils.isEmpty(string) == true || ignoreStringSet.contains(string) || string.matches("\\d*") || string.endsWith(".java")) {
                continue;
            }
            String path = toResourceFilePath(string);
            if (StringUtils.isEmpty(path) == true) {
                continue;
            }
            ret.add(path);
        }
        return ret;
    }

    private static Set<String> ignoreStringSet = new HashSet<String>();

    static {
        ignoreStringSet.add("more");
        ignoreStringSet.add("...");
        ignoreStringSet.add("Caused");
        ignoreStringSet.add("by");
        ignoreStringSet.add("Native");
        ignoreStringSet.add("Method");
        ignoreStringSet.add("at");
        ignoreStringSet.add("clinit");
        ignoreStringSet.add("init");
        ignoreStringSet.add("Source");
        ignoreStringSet.add("Unknown");
    }

    private static String toResourceFilePath(String string) {
        string = removeMethod(string);
        String ret = string.replaceAll("\\.", "/") + ".class";
        ret = ret.replaceAll("\\/\\.", ".");
        return ret;
    }

    private static String removeMethod(String string) {
        if (string.contains(".") == false) {
            return string;
        }
        int lastIndexOf = string.lastIndexOf('.');
        if (lastIndexOf + 1 < string.length() && Character.isLowerCase(string.charAt(lastIndexOf + 1)) == false) {
            return string;
        }
        return string.substring(0, lastIndexOf);
    }
}
