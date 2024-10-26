package de.guidoludwig.jtrade.expimp.action;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import org.dom4j.Document;
import de.guidoludwig.jtrade.ErrorMessage;
import de.guidoludwig.jtrade.I18n;
import de.guidoludwig.jtrade.db4o.JTDB;
import de.guidoludwig.jtrade.install.JTradeProperties;
import de.guidoludwig.jtrade.util.SwingUtil;
import de.guidoludwig.jtrade.xml.IO;
import de.guidoludwig.jtrade.xml.XSLTransformer;

/**
 * Action to export the Tradelist to html files
 * 
 * @author <a href="mailto:jtrade@gigabss.de">Guido Ludwig</a>
 * @version $Revision: 1.5 $
 */
public class ExportHTMLAction extends AbstractAction {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private static final String USER_EMAIL = "email";

    private static final String HTML_PAGETITLE = "pageTitle";

    private static final String TOOLTIP = "action.export.html.tooltip";

    private static final String ICON = "action.export.html.icon";

    private static final String ACTION_NAME = "action.export.html.name";

    public ExportHTMLAction() {
        super(I18n.getString(ACTION_NAME), I18n.getIcon(ICON));
        putValue(Action.SHORT_DESCRIPTION, I18n.getString(TOOLTIP));
    }

    public void actionPerformed(ActionEvent event) {
        SwingUtil.finishCurrentEdit();
        try {
            Document doc = IO.createDocument(JTDB.INSTANCE.getAllArtists(), false);
            XSLTransformer overviewTransformer = new XSLTransformer(doc, new StreamSource(getXSLStream(JTradeProperties.HTML_XSL_OVERVIEW)), getOutputStream(JTradeProperties.INSTANCE.getProperty(JTradeProperties.HTML_OVERVIEW_FILE)));
            overviewTransformer.setParameter(HTML_PAGETITLE, JTradeProperties.INSTANCE.getProperty(JTradeProperties.HTML_PAGE_TITLE));
            overviewTransformer.setParameter(USER_EMAIL, JTradeProperties.INSTANCE.getProperty(JTradeProperties.USER_EMAIL));
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).finest("Export overview");
            overviewTransformer.transform();
        } catch (IOException exception) {
            ErrorMessage.handle(exception);
        } catch (TransformerConfigurationException exception) {
            ErrorMessage.handle(exception);
        } catch (TransformerException exception) {
            ErrorMessage.handle(exception);
        }
    }

    private InputStream getXSLStream(String xslID) throws IOException {
        InputStream stream = null;
        if (JTradeProperties.INSTANCE.isUserSpecific(xslID)) {
            File xsl = new File(JTradeProperties.INSTANCE.getHome() + File.separator + JTradeProperties.INSTANCE.getProperty(xslID));
            if (xsl.exists()) {
                stream = new FileInputStream(xsl);
            } else {
                URL url = ClassLoader.getSystemResource(JTradeProperties.INSTANCE.getProperty(xslID));
                stream = url.openStream();
            }
        } else {
            URL url = ClassLoader.getSystemResource(JTradeProperties.INSTANCE.getProperty(xslID));
            stream = url.openStream();
        }
        return stream;
    }

    private PrintStream getOutputStream(String filename) throws FileNotFoundException {
        return new PrintStream(new FileOutputStream(JTradeProperties.INSTANCE.getProperty(JTradeProperties.HTML_OUTPUT_DIRECTORY) + File.separator + filename));
    }
}
