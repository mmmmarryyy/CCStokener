package org.apache.fop.apps;

import org.apache.fop.viewer.*;
import org.apache.fop.render.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.UIManager;
import java.awt.*;
import org.xml.sax.XMLReader;
import org.apache.avalon.framework.logger.ConsoleLogger;
import java.io.InputStream;
import java.net.URL;

/**
 * initialize AWT previewer
 */
public class AWTStarter extends CommandLineStarter {

    PreviewDialog frame;

    AWTRenderer renderer;

    protected Driver driver;

    protected XMLReader parser;

    public static final String TRANSLATION_PATH = "/org/apache/fop/viewer/resources/";

    private Translator resource;

    public AWTStarter(CommandLineOptions commandLineOptions) throws FOPException {
        super(commandLineOptions);
        init();
    }

    private void init() throws FOPException {
        try {
            UIManager.setLookAndFeel(new javax.swing.plaf.metal.MetalLookAndFeel());
        } catch (Exception e) {
            e.printStackTrace();
        }
        String language = commandLineOptions.getLanguage();
        if (language == null) {
            try {
                language = System.getProperty("user.language");
            } catch (SecurityException se) {
            }
        }
        resource = getResourceBundle(TRANSLATION_PATH + "resources." + language);
        UserMessage.setTranslator(getResourceBundle(TRANSLATION_PATH + "messages." + language));
        resource.setMissingEmphasized(false);
        renderer = new AWTRenderer(resource);
        frame = createPreviewDialog(renderer, resource);
        renderer.setProgressListener(frame);
        renderer.setComponent(frame);
        driver = new Driver();
        driver.setLogger(new ConsoleLogger(ConsoleLogger.LEVEL_INFO));
        if (errorDump) {
            driver.setErrorDump(true);
        }
        driver.setRenderer(renderer);
        frame.progress(resource.getString("Init parser") + " ...");
        parser = inputHandler.getParser();
        if (parser == null) {
            throw new FOPException("Unable to create SAX parser");
        }
    }

    public void run() throws FOPException {
        driver.reset();
        try {
            frame.progress(resource.getString("Build FO tree") + " ...");
            driver.render(parser, inputHandler.getInputSource());
            frame.progress(resource.getString("Show"));
            frame.showPage();
        } catch (Exception e) {
            frame.reportException(e);
            if (e instanceof FOPException) {
                throw (FOPException) e;
            }
            throw new FOPException(e);
        }
    }

    protected PreviewDialog createPreviewDialog(AWTRenderer renderer, Translator res) {
        PreviewDialog frame = new PreviewDialog(this, renderer, res);
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

    private SecureResourceBundle getResourceBundle(String path) {
        InputStream in = null;
        try {
            URL url = getClass().getResource(path);
            if (url == null) {
                path = path.substring(0, path.lastIndexOf(".")) + ".en";
                url = getClass().getResource(path);
            }
            in = url.openStream();
        } catch (Exception ex) {
            log.error("Can't find URL to: <" + path + "> " + ex.getMessage(), ex);
        }
        return new SecureResourceBundle(in);
    }
}
