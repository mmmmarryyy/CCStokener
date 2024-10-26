import java.applet.Applet;
import java.awt.print.PrinterJob;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.pdfbox.pdmodel.PDDocument;

public class pdfApplet extends Applet {

    public URL getURL() throws Exception {
        return new URL(this.getDocumentBase(), this.getParameter("path"));
    }

    public InputStream unZip(URL url) throws Exception {
        ZipInputStream zipped = new ZipInputStream(url.openStream());
        System.out.println("unzipping: " + url.getFile());
        ZipEntry zip = zipped.getNextEntry();
        byte[] b = new byte[4096];
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        for (int iRead = zipped.read(b); iRead != -1; iRead = zipped.read(b)) {
            bOut.write(b, 0, iRead);
        }
        zipped.close();
        ByteArrayInputStream bIn = new ByteArrayInputStream(bOut.toByteArray());
        return (InputStream) bIn;
    }

    public InputStream getInputStream() throws Exception {
        URL url = getURL();
        System.out.println("downloading: " + url.getFile());
        if ("zip".equalsIgnoreCase(getParameter("type")) || getParameter("type") == null) {
            return unZip(getURL());
        } else {
            return getURL().openStream();
        }
    }

    public PDDocument getDocument() throws Exception {
        InputStream is = getInputStream();
        System.out.println("creating pdf");
        return PDDocument.load(is);
    }

    public void print(PDDocument doc) throws Exception {
        PrinterJob pj = PrinterJob.getPrinterJob();
        pj.setJobName("claim print job");
        pj.setPageable(doc);
        if ("yes".equalsIgnoreCase(this.getParameter("print_dialog"))) {
            if (!pj.printDialog()) {
                System.out.println("printing cancelled");
                return;
            } else {
                System.out.println("printer set to: " + pj.getPrintService().getName());
            }
        } else {
            if (pj.getPrintService() == null) {
                System.out.println("no default printer found. asking");
                if (pj.printDialog()) {
                    System.out.println("printer set to: " + pj.getPrintService().getName());
                } else {
                    return;
                }
            } else {
                System.out.println("found printer: " + pj.getPrintService().getName());
            }
        }
        System.out.println("printing");
        pj.print();
    }

    public void init() {
        super.init();
        try {
            PDDocument doc = getDocument();
            print(doc);
            doc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
