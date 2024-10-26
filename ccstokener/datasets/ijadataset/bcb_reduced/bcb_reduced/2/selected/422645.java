package net.sourceforge.yagsbook.pagexml;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.Cvs;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.*;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.*;
import org.apache.fop.apps.*;

/**
 * XSLT plugin for handling PageXML files. Supports generation of rule
 * sets and encyclopedias as referenced by the source XML.
 *
 * Updated 2009/05/10 for Fop 0.9X
 *
 * @author Samuel Penn.
 */
public class Yagsbook extends Cvs {

    private String localRepository;

    private String destination;

    private String relativePath;

    private String targetDir = "html";

    private String tmpDir = "tmp";

    private static final String XSLT_BASE = "/usr/share/xml/yagsbook/article/xslt";

    private static final String XSLT_HTML = XSLT_BASE + "/html/yagsbook.xsl";

    private static final String XSLT_PDF = XSLT_BASE + "/pdf/yagsbook.xsl";

    class XMLException extends Exception {

        XMLException(String msg) {
            super(msg);
        }
    }

    /**
     * Resolve links to external documents so that relative files are found.
     */
    public class LocalResolver implements URIResolver {

        private String baseDir;

        LocalResolver(String baseDir) {
            this.baseDir = baseDir;
        }

        public Source resolve(String href, String base) {
            File file = new File(href);
            URL url = null;
            String fullPath = null;
            Source src = null;
            try {
                if (href.startsWith("http:")) {
                    url = new URL(href);
                    src = new StreamSource(url.openStream());
                } else if (file.isAbsolute()) {
                    src = new StreamSource(new File(href));
                } else {
                    src = new StreamSource(new File(baseDir + "/" + href));
                }
            } catch (Exception e) {
                System.out.println("resolve: " + e.getMessage());
            }
            return src;
        }
    }

    public Yagsbook() {
        setProject(new Project());
        getProject().init();
        setTaskType("cvs");
        setTaskName("cvs");
        target = new Target();
    }

    public void setTargetDir(String targetDir) {
        this.targetDir = targetDir;
    }

    public void setTmpDir(String tmpDir) {
        this.tmpDir = tmpDir;
    }

    /**
     * Import all available Yagsbook documents into the website.
     * The specified CVS repository is exported, and all the '.yags' files
     * in the top level directory are converted to HTML and PDF format.
     *
     * @param module    Name of the CVS module to process.
     * @param subdir    Sub directory beneath the CVS module to process.
     * @param cvsroot   Path to the CVS repository. This can be a pserver URL.
     * @param basedir   Base directory.
     */
    public String importBooks(String module, String subdir, String cvsroot, String basedir) {
        export(module, tmpDir + "/", cvsroot);
        localRepository = tmpDir + "/" + module + "/" + subdir;
        destination = targetDir + "/" + basedir + "/" + subdir;
        relativePath = subdir;
        try {
            new File(destination).mkdirs();
            new File(tmpDir).mkdirs();
        } catch (Exception e) {
        }
        String output = null;
        try {
            File directory = new File(localRepository);
            if (directory.isDirectory()) {
                output = processFiles(directory.list());
            } else {
                System.out.println("Not a directory");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        deleteDir(new File(tmpDir));
        return output;
    }

    public String importEncyclopedia(String module, String destdir, String cvsroot, String basedir) {
        export(module, tmpDir + "/", cvsroot);
        localRepository = tmpDir + "/" + module;
        destination = targetDir + "/" + basedir + "/" + destdir;
        relativePath = destdir;
        try {
            new File(destination).mkdirs();
            new File(tmpDir).mkdirs();
        } catch (Exception e) {
        }
        String output = null;
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }

    private void deleteDir(File dir) {
        try {
            File[] list = dir.listFiles();
            for (int i = 0; i < list.length; i++) {
                if (list[i].isDirectory()) {
                    deleteDir(list[i]);
                } else {
                    list[i].delete();
                }
            }
            dir.delete();
        } catch (Exception e) {
        }
    }

    /**
     * Process the list of files, in alphabetical order. Each file (if it
     * ends with a '.yags' suffix) is transformed via XSL, and an entry
     * added for it into the HTML string which is returned. This string
     * consists of links to each HTML document, plus a summary paragraph
     * describing the document (taken from the header/summary element of
     * the yagsbook document.
     *
     * @param files     Array of filenames to be processed.
     * @return          HTML string describing all the files processed.
     */
    String processFiles(String[] files) {
        StringBuffer buffer = new StringBuffer();
        Arrays.sort(files);
        for (int i = 0; i < files.length; i++) {
            String name = files[i];
            if (name.endsWith(".yags")) {
                System.out.println("Transforming " + name);
                buffer.append(transform(name));
            }
        }
        return buffer.toString();
    }

    private Document load(String filename) throws XMLException {
        return load(filename, false);
    }

    private Document load(String filename, boolean useNameSpace) throws XMLException {
        try {
            InputSource in;
            DocumentBuilderFactory dbf;
            in = new InputSource(new FileInputStream(new File(filename)));
            dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(useNameSpace);
            return dbf.newDocumentBuilder().parse(in);
        } catch (ParserConfigurationException pce) {
            throw new XMLException("Cannot configure parser for [" + filename + "]: " + pce.getMessage());
        } catch (SAXException se) {
            throw new XMLException("Cannot parse document [" + filename + "]: " + se.getMessage());
        } catch (IOException ioe) {
            throw new XMLException("Cannot find file [" + filename + "]: " + ioe.getMessage());
        }
    }

    private String getSummary(Document doc) {
        String summary = "";
        String xpath = "/article/header/summary";
        try {
            Node node = XPathAPI.selectSingleNode(doc, xpath);
            if (node != null) {
                if (node.hasChildNodes()) {
                    node = node.getFirstChild();
                    summary = node.getNodeValue();
                }
            }
        } catch (TransformerException te) {
            System.out.println(te.getMessage());
        }
        return summary;
    }

    private String getTitle(Document doc) {
        String summary = "";
        String xpath = "/article/header/title";
        try {
            Node node = XPathAPI.selectSingleNode(doc, xpath);
            if (node != null) {
                if (node.hasChildNodes()) {
                    node = node.getFirstChild();
                    summary = node.getNodeValue();
                }
            }
        } catch (TransformerException te) {
            System.out.println(te.getMessage());
        }
        return summary;
    }

    public String applyStylesheet(String stylesheet, Document doc) throws XMLException {
        StringWriter writer = new StringWriter();
        try {
            File xsltFile = new File(stylesheet);
            Source xslt = new StreamSource(xsltFile);
            Source xml = new DOMSource(doc);
            Result html = new StreamResult(writer);
            TransformerFactory fact = TransformerFactory.newInstance();
            Transformer trans = fact.newTransformer(xslt);
            LocalResolver local = new LocalResolver(localRepository);
            trans.setURIResolver(local);
            trans.transform(xml, html);
        } catch (TransformerConfigurationException tce) {
            tce.printStackTrace();
            throw new XMLException("Cannot configure transform: " + tce.getMessage());
        } catch (TransformerException te) {
            te.printStackTrace();
            throw new XMLException("Cannot apply stylesheet: " + te.getMessage());
        }
        return writer.toString();
    }

    /**
     * Given an XSL-FO file, convert it to PDF and write out to the
     * provided destination file. Uses Apache FOP to do all the hard
     * work of generating the PDF.
     *
     * @param fo        Input file containing XSL-FO.
     * @param pdf       Output file to write PDF to.
     *
     * @return          True if a PDF could be generated.
     */
    private boolean writePdf(File fo, File pdf) {
        boolean success = false;
        FopFactory factory = FopFactory.newInstance();
        FOUserAgent userAgent = factory.newFOUserAgent();
        Fop fop = null;
        userAgent.setBaseURL(localRepository);
        OutputStream stream = null;
        try {
            stream = new BufferedOutputStream(new FileOutputStream(pdf));
            fop = factory.newFop(MimeConstants.MIME_PDF, userAgent, stream);
        } catch (FileNotFoundException e) {
            System.out.println("Failed to find file [" + pdf.getAbsolutePath() + "]");
            return success;
        } catch (FOPException e) {
            e.printStackTrace();
            return success;
        }
        try {
            TransformerFactory tfactory = TransformerFactory.newInstance();
            Transformer transformer = tfactory.newTransformer();
            Source src = new StreamSource(fo);
            Result result = new SAXResult(fop.getDefaultHandler());
            transformer.transform(src, result);
            stream.close();
            success = true;
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FOPException e) {
            e.printStackTrace();
        }
        return success;
    }

    /**
     * Transform the given Yagsbook XML file into both HTML and PDF
     * renditions. The resulting files will have the same base name,
     * with either a '.html' or '.pdf' suffix. Any external images
     * referenced by the document are copied to the destination
     * location. They are currently assumed to be in the directory
     * local to the document.
     */
    String transform(String name) {
        StringBuffer output = new StringBuffer();
        String baseName = name.replaceAll("\\.yags", "");
        FileWriter writer = null;
        try {
            Document doc = load(localRepository + "/" + name, true);
            String html = applyStylesheet(XSLT_HTML, doc);
            String htmlName = destination + "/" + baseName + ".html";
            String pdfName = destination + "/" + baseName + ".pdf";
            writer = new FileWriter(htmlName);
            writer.write(html);
            writer.close();
            boolean havePdf = false;
            try {
                String fo = applyStylesheet(XSLT_PDF, doc);
                File tmpFo = File.createTempFile(baseName, ".fo");
                writer = new FileWriter(tmpFo);
                writer.write(fo);
                writer.close();
                havePdf = writePdf(tmpFo, new File(pdfName));
            } catch (Exception e) {
                e.printStackTrace();
            }
            doc = load(localRepository + "/" + name, false);
            copyImages(doc);
            String htmlHref = relativePath + "/" + baseName + ".html";
            String pdfHref = relativePath + "/" + baseName + ".pdf";
            output.append("<p><a href=\"" + htmlHref + "\">");
            output.append(getTitle(doc) + "</a>");
            if (havePdf) {
                output.append(" (<a href=\"" + pdfHref + "\">PDF</a>)");
            }
            output.append(": ");
            output.append(getSummary(doc));
            output.append("</p>\n");
        } catch (XMLException xmle) {
            xmle.printStackTrace();
            System.out.println(xmle.getMessage());
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
        return output.toString();
    }

    /**
     * Copy the given file into the specified directory. The name of
     * the file is always preserved.
     *
     * @param file      The file to be copied.
     * @param path      The path to the destination directory.
     */
    private void copyFile(File file, File destination) throws IOException {
        String outpath = destination.getPath() + "/" + file.getName();
        byte[] data = new byte[65536];
        int len = 0;
        File dest = new File(outpath);
        if (dest.exists()) {
            return;
        }
        FileInputStream fis = new FileInputStream(file);
        FileOutputStream fos = new FileOutputStream(outpath);
        while ((len = fis.read(data)) >= 0) {
            fos.write(data, 0, len);
        }
        fis.close();
        fos.close();
    }

    /**
     * Finds all the external images referenced in the document and copies
     * them into the destination directory.
     */
    void copyImages(Document doc) {
        NodeList list = null;
        Node node = null;
        String xpath = "//svg/@src";
        try {
            list = XPathAPI.selectNodeList(doc, xpath);
            for (int i = 0; i < list.getLength(); i++) {
                node = list.item(i);
                String path = node.getNodeValue();
                System.out.println(i + ": SVG [" + path + "]");
                copyFile(new File(localRepository + "/" + path), new File(destination));
            }
        } catch (TransformerException e) {
            System.out.println("copyImages: XML Exception (" + e.getMessage() + ")");
        } catch (IOException e) {
            System.out.println("copyImages: IO Exception (" + e.getMessage() + ")");
        }
    }

    void export(String srcdir, String destdir, String cvsroot) {
        try {
            setCvsRoot(cvsroot);
            setCommand("export");
            setDest(new File(destdir));
            setDate("TODAY");
            setPackage(srcdir);
            execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    public void test() {
        StringBuffer output = new StringBuffer();
        StringWriter writer = new StringWriter();
        try {
            InputSource in;
            DocumentBuilderFactory dbf;
            in = new InputSource(new FileInputStream(new File("/home/sam/foo.xml")));
            dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder().parse(in);
            File xsltFile = new File("/home/sam/foo.xslt");
            Source xslt = new StreamSource(xsltFile);
            Source xml = new DOMSource(doc);
            Result html = new StreamResult(writer);
            TransformerFactory fact = TransformerFactory.newInstance();
            Transformer trans = fact.newTransformer(xslt);
            LocalResolver local = new LocalResolver("/tmp");
            trans.setURIResolver(local);
            trans.transform(xml, html);
            System.out.println(writer.toString());
        } catch (Exception xmle) {
            xmle.printStackTrace();
            System.out.println(xmle.getMessage());
        }
    }

    private String archiveDirectory(ZipOutputStream stream, File directory) {
        StringBuffer buffer = new StringBuffer();
        String[] list = directory.list();
        for (int i = 0; i < list.length; i++) {
            File file = new File(directory.getAbsolutePath() + "/" + list[i]);
            if (file.isDirectory()) {
                buffer.append(archiveDirectory(stream, file));
            } else {
                ZipEntry zip = new ZipEntry(list[i]);
            }
        }
        return buffer.toString();
    }

    /**
     * Create a ZIP archive of all the source files.
     */
    private String archiveSource(File rootDir) throws FileNotFoundException {
        String output = null;
        if (!rootDir.isDirectory()) {
            System.out.println("Can only archive files in a directory");
            return "";
        }
        File zip = new File("/tmp/test.zip");
        ZipOutputStream stream = new ZipOutputStream(new FileOutputStream(zip));
        archiveDirectory(stream, rootDir);
        try {
            File directory = new File(localRepository);
            if (directory.isDirectory()) {
                output = processFiles(directory.list());
            } else {
                System.out.println("Not a directory");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        deleteDir(new File(tmpDir));
        return output;
    }

    public static void main(String[] args) {
        Yagsbook yb = new Yagsbook();
        String result = null;
        result = yb.importBooks("yags", "habisfern", ":pserver:sam@cvshost:2401/var/cvs/rpg", "output");
        System.out.println(result);
    }
}
