package embedding;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.sax.SAXResult;
import org.apache.avalon.framework.ExceptionUtil;
import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.fop.apps.Driver;
import org.apache.fop.apps.FOPException;
import org.apache.fop.render.awt.AWTRenderer;
import org.apache.fop.viewer.PreviewDialog;
import org.apache.fop.viewer.SecureResourceBundle;
import org.apache.fop.viewer.Translator;
import org.apache.fop.viewer.UserMessage;

/**
 * This class demonstrates the use of the AWT Viewer.
 */
public class ExampleAWTViewer {

    public static final String TRANSLATION_PATH = "/org/apache/fop/viewer/resources/";

    protected PreviewDialog createPreviewDialog(AWTRenderer renderer, Translator res) {
        PreviewDialog frame = new PreviewDialog(renderer, res);
        frame.validate();
        frame.addWindowListener(new WindowAdapter() {

            public void windowClosed(WindowEvent we) {
                System.exit(0);
            }
        });
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();
        if (frameSize.height > screenSize.height) frameSize.height = screenSize.height;
        if (frameSize.width > screenSize.width) frameSize.width = screenSize.width;
        frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
        frame.setVisible(true);
        return frame;
    }

    private SecureResourceBundle getResourceBundle(String path) throws IOException {
        URL url = getClass().getResource(path);
        if (url == null) {
            path = path.substring(0, path.lastIndexOf(".")) + ".en";
            url = getClass().getResource(path);
        }
        return new SecureResourceBundle(url.openStream());
    }

    public void viewFO(File fo) throws IOException, FOPException, TransformerException {
        String language = System.getProperty("user.language");
        Translator translator = getResourceBundle(TRANSLATION_PATH + "resources." + language);
        translator.setMissingEmphasized(false);
        UserMessage.setTranslator(getResourceBundle(TRANSLATION_PATH + "messages." + language));
        AWTRenderer renderer = new AWTRenderer(translator);
        PreviewDialog frame = createPreviewDialog(renderer, translator);
        renderer.setProgressListener(frame);
        renderer.setComponent(frame);
        Driver driver = new Driver();
        driver.setLogger(new ConsoleLogger(ConsoleLogger.LEVEL_INFO));
        driver.setRenderer(renderer);
        try {
            frame.progress(translator.getString("Build FO tree") + " ...");
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            Source src = new StreamSource(fo);
            Result res = new SAXResult(driver.getContentHandler());
            transformer.transform(src, res);
            frame.progress(translator.getString("Show"));
            frame.showPage();
        } catch (Exception e) {
            frame.reportException(e);
            if (e instanceof FOPException) {
                throw (FOPException) e;
            }
            throw new FOPException(e);
        }
    }

    public static void main(String[] args) {
        try {
            System.out.println("FOP ExampleAWTViewer\n");
            System.out.println("Preparing...");
            File baseDir = new File(".");
            File outDir = new File(baseDir, "out");
            outDir.mkdirs();
            File fofile = new File(baseDir, "xml/fo/helloworld.fo");
            System.out.println("Input: XSL-FO (" + fofile + ")");
            System.out.println("Output: AWT Viewer");
            System.out.println();
            System.out.println("Starting AWT Viewer...");
            ExampleAWTViewer app = new ExampleAWTViewer();
            app.viewFO(fofile);
            System.out.println("Success!");
        } catch (Exception e) {
            System.err.println(ExceptionUtil.printStackTrace(e));
            System.exit(-1);
        }
    }
}
