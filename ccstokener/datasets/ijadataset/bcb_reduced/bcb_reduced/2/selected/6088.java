package org.apache.ws.jaxme.webapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.ws.jaxme.generator.Generator;
import org.apache.ws.jaxme.generator.impl.GeneratorImpl;
import org.apache.ws.jaxme.generator.sg.impl.JAXBSchemaReader;
import org.apache.ws.jaxme.xs.XSParser;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** <p>This class invokes the JaxMe compiler.</p>
 */
public class JaxMeServlet extends BaseServlet {

    private class StoringEntityResolver implements EntityResolver {

        private final File schemaDir;

        private final Map urlMap = new HashMap();

        public StoringEntityResolver(File pSchemaDir) {
            schemaDir = pSchemaDir;
        }

        public InputSource resolveEntity(String pPublicId, String pSystemId) throws SAXException, IOException {
            try {
                URL url = new URL(pSystemId);
                String fileName = (String) urlMap.get(url);
                if (fileName != null) {
                    FileInputStream istream = new FileInputStream(new File(schemaDir, fileName));
                    InputSource isource = new InputSource(istream);
                    isource.setSystemId(url.toString());
                    return isource;
                }
                String file = url.getFile();
                if (file == null) {
                    file = "";
                } else {
                    int offset = file.lastIndexOf('/');
                    if (offset >= 0) {
                        file = file.substring(offset + 1);
                    }
                }
                if ("".equals(file)) {
                    file = "schema.xsd";
                }
                int offset = file.lastIndexOf('.');
                String prefix;
                String suffix;
                String numAsStr = "";
                if (offset > 0 && offset < file.length()) {
                    prefix = file.substring(0, offset);
                    suffix = file.substring(offset);
                } else {
                    prefix = file;
                    suffix = ".xsd";
                }
                File f;
                for (int num = 1; ; ++num) {
                    f = new File(schemaDir, prefix + numAsStr + suffix);
                    if (f.exists()) {
                        numAsStr = "_" + num;
                    } else {
                        break;
                    }
                }
                InputStream istream = url.openStream();
                schemaDir.mkdirs();
                FileOutputStream fos = new FileOutputStream(f);
                try {
                    byte[] buffer = new byte[1024];
                    for (; ; ) {
                        int res = istream.read(buffer);
                        if (res == -1) {
                            break;
                        } else if (res > 0) {
                            fos.write(buffer, 0, res);
                        }
                    }
                    istream.close();
                    fos.close();
                    fos = null;
                } finally {
                    if (fos != null) {
                        try {
                            f.delete();
                        } catch (Throwable ignore) {
                        }
                    }
                }
                urlMap.put(url, f.getName());
                InputSource isource = new InputSource(new FileInputStream(f));
                isource.setSystemId(url.toString());
                return isource;
            } catch (Exception e) {
                JaxMeServlet.this.log("Failed to resolve URL " + pSystemId, e);
            }
            return null;
        }
    }

    public File createTempDir() throws IOException, ServletException {
        File f = File.createTempFile("jaxme", ".tmp", getWorkDir());
        f.delete();
        if (!f.mkdir()) {
            throw new ServletException("Unable to create temporary directory " + f.getAbsolutePath());
        }
        return f;
    }

    public void addContents(ZipOutputStream pZipFile, File pDirectory, String pDirName) throws IOException {
        File[] files = pDirectory.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            String name = pDirName.length() == 0 ? f.getName() : pDirName + "/" + f.getName();
            if (f.isDirectory()) {
                addContents(pZipFile, f, name);
            } else if (f.isFile()) {
                FileInputStream istream = new FileInputStream(f);
                try {
                    ZipEntry zipEntry = new ZipEntry(name);
                    pZipFile.putNextEntry(zipEntry);
                    byte[] buffer = new byte[1024];
                    for (; ; ) {
                        int res = istream.read(buffer);
                        if (res == -1) {
                            break;
                        } else if (res > 0) {
                            pZipFile.write(buffer, 0, res);
                        }
                    }
                    pZipFile.closeEntry();
                    istream.close();
                    istream = null;
                } finally {
                    if (istream != null) {
                        try {
                            istream.close();
                        } catch (Throwable ignore) {
                        }
                    }
                }
            }
        }
    }

    public void removeDirectory(File pDirectory) throws ServletException {
        cleanDirectory(pDirectory);
        pDirectory.delete();
    }

    protected void doCompile(boolean pValidating, File pTempDir, URL pURL, HttpServletResponse pResponse) throws ServletException, IOException {
        Generator gen = new GeneratorImpl();
        gen.setTargetDirectory(new File(pTempDir, "src"));
        gen.setValidating(pValidating);
        gen.setSchemaReader(new JAXBSchemaReader());
        gen.setEntityResolver(new StoringEntityResolver(new File(pTempDir, "schema")));
        try {
            gen.generate(pURL);
        } catch (Exception e) {
            throw new ServletException(e);
        }
        pResponse.setContentType("application/zip");
        pResponse.setHeader("Content-Disposition", "attachment; filename=\"jaxmeGeneratedSrc.zip\"");
        ZipOutputStream zipOutputStream = new ZipOutputStream(pResponse.getOutputStream());
        addContents(zipOutputStream, pTempDir, "");
        zipOutputStream.close();
        removeDirectory(pTempDir);
        pTempDir = null;
    }

    protected void doValidate(boolean pValidating, File pTempDir, URL pURL, HttpServletResponse pResponse) throws ServletException, IOException {
        XSParser parser = new XSParser();
        parser.setValidating(pValidating);
        InputSource isource = new InputSource(pURL.toString());
        try {
            parser.parse(isource);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    public void doGet(HttpServletRequest pRequest, HttpServletResponse pResponse) throws ServletException, IOException {
        String s = pRequest.getParameter("url");
        if (s == null || s.length() == 0) {
            throw new ServletException("Missing or empty request parameter: " + s);
        }
        URL url;
        try {
            url = new URL(s);
        } catch (MalformedURLException e) {
            throw new ServletException("Malformed URL: " + s);
        }
        boolean isValidating = Boolean.valueOf(pRequest.getParameter("isValidating")).booleanValue();
        File f = createTempDir();
        String what = pRequest.getParameter("what");
        boolean forward = false;
        try {
            if ("compile".equals(what)) {
                try {
                    doCompile(isValidating, f, url, pResponse);
                } catch (ServletException e) {
                    pRequest.setAttribute("error", e);
                    forward = true;
                }
            } else if ("validate".equals(what)) {
                doValidate(isValidating, f, url, pResponse);
                pRequest.setAttribute("success", Boolean.TRUE);
                forward = true;
            } else {
                throw new ServletException("You must choose a proper action: Either 'compile' or 'validate'.");
            }
            f = null;
        } finally {
            if (f != null) {
                try {
                    removeDirectory(f);
                } catch (Throwable ignore) {
                }
            }
        }
        if (forward) {
            pRequest.getRequestDispatcher("index.jsp").forward(pRequest, pResponse);
        }
    }

    public void doPost(HttpServletRequest pRequest, HttpServletResponse pResponse) throws ServletException, IOException {
        doGet(pRequest, pResponse);
    }
}
