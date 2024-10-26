package com.icesoft.faces.webapp.parser;

import com.icesoft.jasper.JasperException;
import com.icesoft.jasper.compiler.TldLocationsCache;
import com.icesoft.jasper.xmlparser.ParserUtils;
import com.icesoft.jasper.xmlparser.TreeNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

public class JspPageToDocument {

    static String JSP_TAGLIB_PATTERN = "<%@\\s*taglib\\s+(.*?)%>";

    static String URI_PATTERN = "uri=\"(.*?)\"";

    static String PREFIX_PATTERN = "prefix=\"(.*?)\"";

    static String JSP_DECL_PATTERN = "<%.*?%>";

    static String STATIC_INCLUDE_PATTERN = "<%@\\s*include\\s+file=\"([^\"]*)\".*?%>";

    static String STATIC_INCLUDE_DIRECTIVE_PATTERN = "<\\s*jsp:directive.include\\s+file=\"([^\"]*)\".*?/>";

    static String OPEN_TAG_PATTERN = "<\\s*(\\w*\\:*\\w+)([^>]*)>";

    static String GENERIC_TAG_PATTERN = "(<\\s*)(/*)(\\w+)([^:])";

    static String ATTRIBUTE_PATTERN = "\\s*([^=]*)\\s*=\\s*\"([^\"]*)\"";

    static String HTML_TAG_PATTERN = "<\\s*html";

    static String P_TAG_PATTERN = "<\\s*/*(p)([^>]*?)/*>";

    static String IMG_TAG_PATTERN = "<\\s*(img)([^>]*?)/*>";

    static String JSP_INCLUDE_PATTERN = "<\\s*jsp:include([^>]*?)/>";

    static String BR_TAG_PATTERN = "<\\s*br\\s*>";

    static String HR_TAG_PATTERN = "<\\s*hr\\s*>";

    static String LINK_TAG_PATTERN = "<\\s*(link)([^>]*?)/*>";

    static String META_TAG_PATTERN = "<\\s*(meta)([^>]*?)/*>";

    static String INPUT_TAG_PATTERN = "<\\s*(input)([^>]*?)/*>";

    static String NBSP_ENTITY_PATTERN = "&nbsp";

    static String COPY_ENTITY_PATTERN = "&copy;";

    static String DOC_DECL_PATTERN = "<!DOCTYPE [^>]*>";

    static String XML_DECL_PATTERN = "<\\?xml [^>]*\\?>";

    static String MYFACES_TAG_CLASS = "org/apache/myfaces/taglib/core/ViewTag.class";

    static String SUN_TAG_CLASS = "com/sun/faces/taglib/jsf_core/ViewTag.class";

    static String HTML_TLD_SUFFIX = "META-INF/html_basic.tld";

    static String CORE_TLD_SUFFIX = "META-INF/jsf_core.tld";

    private static final Log log = LogFactory.getLog(JspPageToDocument.class);

    public static void main(String[] args) {
        try {
            getInputAsString(transform(new FileReader(args[0])));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param input
     * @return String
     */
    public static String transform(String input) {
        String result = input;
        Pattern jspDeclarationPattern = Pattern.compile(JSP_DECL_PATTERN, Pattern.DOTALL);
        Matcher jspDeclarationMatcher = jspDeclarationPattern.matcher(result);
        if (!jspDeclarationMatcher.find()) {
            return (preprocessJspDocument(result));
        }
        result = performStaticInclude(STATIC_INCLUDE_PATTERN, input);
        result = toLowerHTML(result);
        Pattern jspTaglibPattern = Pattern.compile(JSP_TAGLIB_PATTERN, Pattern.DOTALL);
        Pattern openTagPattern = Pattern.compile(OPEN_TAG_PATTERN, Pattern.DOTALL);
        Pattern attributePattern = Pattern.compile(ATTRIBUTE_PATTERN, Pattern.DOTALL);
        Pattern prefixPattern = Pattern.compile(PREFIX_PATTERN, Pattern.DOTALL);
        Pattern uriPattern = Pattern.compile(URI_PATTERN, Pattern.DOTALL);
        Hashtable declarationsTable = new Hashtable();
        Matcher jspTaglibMatcher = jspTaglibPattern.matcher(result);
        declarationsTable.put("xmlns:jsp", "jsp");
        declarationsTable.put("xmlns:icefaces", "http://www.icesoft.com/icefaces");
        while (jspTaglibMatcher.find()) {
            String attributes = jspTaglibMatcher.group(1);
            Matcher prefixMatcher = prefixPattern.matcher(attributes);
            Matcher uriMatcher = uriPattern.matcher(attributes);
            prefixMatcher.find();
            uriMatcher.find();
            String prefix = prefixMatcher.group(1);
            String url = uriMatcher.group(1);
            declarationsTable.put("xmlns:" + prefix, url);
        }
        Matcher openTagMatcher = openTagPattern.matcher(result);
        openTagMatcher.find();
        String tag = openTagMatcher.group(1);
        String attributes = openTagMatcher.group(2);
        Matcher attributeMatcher = attributePattern.matcher(attributes);
        while (attributeMatcher.find()) {
            String name = attributeMatcher.group(1);
            String value = attributeMatcher.group(2);
            declarationsTable.put(name, value);
        }
        Enumeration declarations = declarationsTable.keys();
        String namespaceDeclarations = "";
        while (declarations.hasMoreElements()) {
            String prefix = (String) declarations.nextElement();
            String url = (String) declarationsTable.get(prefix);
            namespaceDeclarations += prefix + "=\"" + url + "\" ";
        }
        jspDeclarationMatcher = jspDeclarationPattern.matcher(result);
        result = jspDeclarationMatcher.replaceAll("");
        result = "<icefaces:root " + namespaceDeclarations + ">" + result + "</icefaces:root>";
        Pattern jspIncludePattern = Pattern.compile(JSP_INCLUDE_PATTERN);
        Matcher jspIncludeMatcher = jspIncludePattern.matcher(result);
        StringBuffer jspIncludeBuf = new StringBuffer();
        while (jspIncludeMatcher.find()) {
            String args = jspIncludeMatcher.group(1);
            jspIncludeMatcher.appendReplacement(jspIncludeBuf, "<icefaces:include" + args + " isDynamic=\"#{true}\" />");
        }
        jspIncludeMatcher.appendTail(jspIncludeBuf);
        result = jspIncludeBuf.toString();
        result = toSingletonTag(P_TAG_PATTERN, result);
        result = toSingletonTag(LINK_TAG_PATTERN, result);
        result = toSingletonTag(META_TAG_PATTERN, result);
        result = toSingletonTag(IMG_TAG_PATTERN, result);
        result = toSingletonTag(INPUT_TAG_PATTERN, result);
        Pattern brTagPattern = Pattern.compile(BR_TAG_PATTERN);
        Matcher brTagMatcher = brTagPattern.matcher(result);
        result = brTagMatcher.replaceAll("<br/>");
        Pattern hrTagPattern = Pattern.compile(HR_TAG_PATTERN);
        Matcher hrTagMatcher = hrTagPattern.matcher(result);
        result = hrTagMatcher.replaceAll("<hr/>");
        Pattern nbspEntityPattern = Pattern.compile(NBSP_ENTITY_PATTERN);
        Matcher nbspEntityMatcher = nbspEntityPattern.matcher(result);
        result = nbspEntityMatcher.replaceAll("&amp;nbsp");
        Pattern copyEntityPattern = Pattern.compile(COPY_ENTITY_PATTERN);
        Matcher copyEntityMatcher = copyEntityPattern.matcher(result);
        result = copyEntityMatcher.replaceAll("&#169;");
        Pattern docDeclPattern = Pattern.compile(DOC_DECL_PATTERN);
        Matcher docDeclMatcher = docDeclPattern.matcher(result);
        result = docDeclMatcher.replaceAll("");
        result = unescapeBackslash(result);
        return result;
    }

    static String preprocessJspDocument(String input) {
        String result = input;
        result = performStaticInclude(STATIC_INCLUDE_DIRECTIVE_PATTERN, input);
        return result;
    }

    /**
     * @param input
     * @return Reader
     */
    public static Reader preprocessJspDocument(Reader input) {
        String inputString = getInputAsString(input);
        String outputString = preprocessJspDocument(inputString);
        return new StringReader(outputString);
    }

    static String performStaticInclude(String includePatternString, String input) {
        String result = input;
        boolean matchFound = true;
        Pattern staticIncludePattern = Pattern.compile(includePatternString);
        StringBuffer staticIncludeBuf;
        while (matchFound) {
            matchFound = false;
            staticIncludeBuf = new StringBuffer();
            Matcher staticIncludeMatcher = staticIncludePattern.matcher(result);
            while (staticIncludeMatcher.find()) {
                matchFound = true;
                String args = staticIncludeMatcher.group(1);
                try {
                    if (!args.startsWith("/")) {
                        String servletPath = FacesContext.getCurrentInstance().getExternalContext().getRequestServletPath();
                        String workingDir = servletPath.substring(0, servletPath.lastIndexOf("/"));
                        args = workingDir + "/" + args;
                    }
                    String includeString = getInputAsString(new InputStreamReader(FacesContext.getCurrentInstance().getExternalContext().getResource(args).openConnection().getInputStream()));
                    Pattern xmlDeclPattern = Pattern.compile(XML_DECL_PATTERN);
                    Matcher xmlDeclMatcher = xmlDeclPattern.matcher(includeString);
                    includeString = xmlDeclMatcher.replaceAll("");
                    staticIncludeMatcher.appendReplacement(staticIncludeBuf, escapeBackreference(includeString));
                } catch (Exception e) {
                    staticIncludeMatcher.appendReplacement(staticIncludeBuf, "");
                    if (log.isErrorEnabled()) {
                        log.error("static include failed to include " + args, e);
                    }
                }
            }
            staticIncludeMatcher.appendTail(staticIncludeBuf);
            result = staticIncludeBuf.toString();
        }
        return result;
    }

    static String toSingletonTag(String pattern, String input) {
        Pattern tagPattern = Pattern.compile(pattern, Pattern.DOTALL);
        Matcher tagMatcher = tagPattern.matcher(input);
        StringBuffer tagBuf = new StringBuffer();
        while (tagMatcher.find()) {
            String tagName = tagMatcher.group(1);
            String attributes = tagMatcher.group(2);
            tagMatcher.appendReplacement(tagBuf, escapeBackreference("<" + tagName + attributes + "/>"));
        }
        tagMatcher.appendTail(tagBuf);
        return tagBuf.toString();
    }

    /**
     * @param input
     * @return String
     */
    public static String toLowerHTML(String input) {
        Pattern genericPattern = Pattern.compile(GENERIC_TAG_PATTERN);
        Matcher genericMatcher = genericPattern.matcher(input);
        StringBuffer inputBuffer = new StringBuffer();
        while (genericMatcher.find()) {
            String openBracket = genericMatcher.group(1);
            String slash = genericMatcher.group(2);
            String tagName = genericMatcher.group(3);
            String trailing = genericMatcher.group(4);
            if (!"HTML".equals(tagName)) {
                tagName = tagName.toLowerCase();
            }
            genericMatcher.appendReplacement(inputBuffer, escapeBackreference(openBracket + slash + tagName + trailing));
        }
        genericMatcher.appendTail(inputBuffer);
        return inputBuffer.toString();
    }

    /**
     * @param input
     * @return String
     */
    public static String escapeBackreference(String input) {
        String result = input;
        Pattern slashPattern = Pattern.compile("\\\\");
        Matcher slashMatcher = slashPattern.matcher(result);
        result = slashMatcher.replaceAll("\\\\\\\\");
        Pattern dollarPattern = Pattern.compile("\\$");
        Matcher dollarMatcher = dollarPattern.matcher(result);
        result = dollarMatcher.replaceAll("\\\\\\$");
        return result;
    }

    /**
     * @param input
     * @return String
     */
    public static String unescapeBackslash(String input) {
        String result = input;
        Pattern slashPattern = Pattern.compile("\\\\\\\\");
        Matcher slashMatcher = slashPattern.matcher(result);
        result = slashMatcher.replaceAll("\\\\");
        return result;
    }

    /**
     * @param input
     * @return String
     */
    public static Reader transform(Reader input) {
        String inputString = getInputAsString(input);
        String outputString = transform(inputString);
        if (log.isTraceEnabled()) {
            log.trace(outputString);
        }
        return new StringReader(outputString);
    }

    /**
     * @param context
     * @param namespaceURL
     * @return InputStream
     * @throws IOException
     */
    public static InputStream getTldInputStream(ExternalContext context, String namespaceURL) throws IOException {
        JarFile jarFile = null;
        String[] location = null;
        namespaceURL = String.valueOf(namespaceURL);
        if ("jsp".equals(namespaceURL)) {
            return null;
        }
        if ("http://java.sun.com/JSP/Page".equals(namespaceURL)) {
            return null;
        }
        TldLocationsCache tldCache = new TldLocationsCache(context);
        try {
            location = tldCache.getLocation(namespaceURL);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug(e.getMessage(), e);
            }
        }
        if (null == location) {
            if (namespaceURL.startsWith("/") && namespaceURL.endsWith(".tld")) {
                location = new String[] { namespaceURL };
            }
        }
        if (null == location) {
            location = scanJars(context, namespaceURL);
        }
        if (null == location) {
            URL tagURL = JspPageToDocument.class.getClassLoader().getResource(SUN_TAG_CLASS);
            if (null != tagURL) {
                URLConnection conn = tagURL.openConnection();
                if (tagURL.getProtocol().equals("vfszip")) {
                    String tagPath = tagURL.toExternalForm();
                    String jarPath = tagPath.substring(0, tagPath.indexOf(SUN_TAG_CLASS));
                    String tldPath = jarPath;
                    if (namespaceURL.endsWith("html")) {
                        tldPath += HTML_TLD_SUFFIX;
                    } else if (namespaceURL.endsWith("core")) {
                        tldPath += CORE_TLD_SUFFIX;
                    }
                    URL tldURL = new URL(tldPath);
                    return tldURL.openConnection().getInputStream();
                }
                if (conn instanceof JarURLConnection) {
                    location = scanJar((JarURLConnection) conn, namespaceURL);
                } else {
                    if (namespaceURL.endsWith("html")) {
                        location = getBundleLocation(tagURL, HTML_TLD_SUFFIX);
                    } else if (namespaceURL.endsWith("core")) {
                        location = getBundleLocation(tagURL, CORE_TLD_SUFFIX);
                    }
                }
            }
        }
        if (null == location) {
            try {
                String separator = System.getProperty("path.separator");
                String wsDirs = System.getProperty("ws.ext.dirs");
                String[] dirs = null;
                if (null != wsDirs) {
                    dirs = wsDirs.split(separator);
                } else {
                    dirs = new String[] {};
                }
                Iterator theDirs = Arrays.asList(dirs).iterator();
                while (theDirs.hasNext()) {
                    String dir = (String) theDirs.next();
                    try {
                        location = scanJars(dir, namespaceURL);
                    } catch (Exception e) {
                    }
                    if (null != location) {
                        break;
                    }
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug(e.getMessage(), e);
                }
            }
        }
        if (null == location) {
            URL tagURL = JspPageToDocument.class.getClassLoader().getResource(MYFACES_TAG_CLASS);
            if (null != tagURL) {
                URLConnection conn = tagURL.openConnection();
                if (conn instanceof JarURLConnection) {
                    location = scanJar((JarURLConnection) conn, namespaceURL);
                } else {
                    if (namespaceURL.endsWith("html")) {
                        location = getBundleLocation(tagURL, HTML_TLD_SUFFIX);
                    } else if (namespaceURL.endsWith("core")) {
                        location = getBundleLocation(tagURL, CORE_TLD_SUFFIX);
                    }
                }
            }
        }
        if (null == location) {
            String msg = "Can't find TLD for location [" + namespaceURL + "]. JAR containing the TLD may not be in the classpath";
            log.error(msg);
            return null;
        } else {
            if (log.isTraceEnabled()) {
                for (int i = 0; i < location.length; i++) {
                    log.trace("Found TLD location for " + namespaceURL + " = " + location[i]);
                }
            }
        }
        if (!location[0].endsWith("jar")) {
            InputStream tldStream = context.getResourceAsStream(location[0]);
            if (null == tldStream) {
                tldStream = (new URL(location[0])).openConnection().getInputStream();
            }
            return tldStream;
        } else {
            URL jarFileUrl = new URL("jar:" + location[0] + "!/");
            JarURLConnection conn = (JarURLConnection) jarFileUrl.openConnection();
            conn.setUseCaches(false);
            conn.connect();
            jarFile = conn.getJarFile();
            ZipEntry jarEntry = jarFile.getEntry(location[1]);
            return jarFile.getInputStream(jarEntry);
        }
    }

    /**
     * Construct a full URL to the specified path given the URL
     * to some other resource in the bundle
     *
     * @param url
     * @param path
     * @return locaion
     */
    private static String[] getBundleLocation(URL url, String path) {
        String protocol = url.getProtocol();
        String host = url.getHost();
        String port = String.valueOf(url.getPort());
        String urlString = protocol + "://" + host + ":" + port + "/" + path;
        return new String[] { urlString, null };
    }

    /**
     * @param source
     * @return InputStream
     */
    public static InputStream stripDoctype(InputStream source) {
        String result = getInputAsString(new InputStreamReader(source));
        Pattern docDeclPattern = Pattern.compile(DOC_DECL_PATTERN);
        Matcher docDeclMatcher = docDeclPattern.matcher(result);
        result = docDeclMatcher.replaceAll("");
        return new StringBufferInputStream(result);
    }

    static String getInputAsString(Reader in) {
        char[] buf = new char[1024];
        StringWriter out = new StringWriter();
        try {
            int l = 1;
            while (l > 0) {
                l = in.read(buf);
                if (l > 0) {
                    out.write(buf, 0, l);
                }
            }
        } catch (IOException e) {
            if (log.isWarnEnabled()) {
                log.warn(e.getMessage(), e);
            }
        }
        return out.toString();
    }

    static String[] scanJars(ExternalContext context, String namespaceURL) {
        try {
            String[] location = null;
            Iterator paths = context.getResourcePaths("/WEB-INF/lib/").iterator();
            while (paths.hasNext()) {
                String path = (String) paths.next();
                if (!path.endsWith(".jar")) {
                    continue;
                }
                path = context.getResource(path).toString();
                JarURLConnection connection = (JarURLConnection) new URL("jar:" + path + "!/").openConnection();
                location = scanJar(connection, namespaceURL);
                if (null != location) {
                    return location;
                }
            }
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Jar scanning under /WEB_INF/lib failed " + e.getMessage(), e);
            }
        }
        return null;
    }

    static String[] scanJars(String dir, String namespaceURL) throws IOException {
        String[] location = null;
        Iterator paths = Arrays.asList(new File(dir).listFiles()).iterator();
        while (paths.hasNext()) {
            String path = ((File) paths.next()).getCanonicalPath();
            if (!path.endsWith(".jar")) {
                continue;
            }
            URL url = new URL("jar:file:" + path + "!/");
            JarURLConnection connection = (JarURLConnection) url.openConnection();
            location = scanJar(connection, namespaceURL);
            if (null != location) {
                return location;
            }
        }
        return null;
    }

    static String[] scanJar(JarURLConnection conn, String namespaceURL) throws IOException {
        JarFile jarFile = null;
        String resourcePath = conn.getJarFileURL().toString();
        if (log.isTraceEnabled()) {
            log.trace("Fallback Scanning Jar " + resourcePath);
        }
        jarFile = conn.getJarFile();
        Enumeration entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            try {
                JarEntry entry = (JarEntry) entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith("META-INF/")) {
                    continue;
                }
                if (!name.endsWith(".tld")) {
                    continue;
                }
                InputStream stream = jarFile.getInputStream(entry);
                try {
                    String uri = getUriFromTld(resourcePath, stream);
                    if ((uri != null) && (uri.equals(namespaceURL))) {
                        return (new String[] { resourcePath, name });
                    }
                } catch (JasperException jpe) {
                    if (log.isDebugEnabled()) {
                        log.debug(jpe.getMessage(), jpe);
                    }
                } finally {
                    if (stream != null) {
                        stream.close();
                    }
                }
            } catch (Throwable t) {
                if (log.isDebugEnabled()) {
                    log.debug(t.getMessage(), t);
                }
            }
        }
        return null;
    }

    static String getUriFromTld(String resourcePath, InputStream in) throws JasperException {
        TreeNode tld = new ParserUtils().parseXMLDocument(resourcePath, in);
        TreeNode uri = tld.findChild("uri");
        if (uri != null) {
            String body = uri.getBody();
            if (body != null) {
                return body;
            }
        }
        return null;
    }
}
