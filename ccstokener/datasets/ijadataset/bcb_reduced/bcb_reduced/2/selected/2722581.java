package net.sf.dpdesktop;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;
import net.sf.dpdesktop.gui.About;
import net.sf.dpdesktop.gui.MainWindow;
import net.sf.dpdesktop.gui.util.MessageHandler;
import net.sf.dpdesktop.service.ConfigurationService;
import net.sf.dpdesktop.util.Helper;
import org.dom4j.DocumentException;

/**
 *
 * @author Heiner Reinhardt
 */
class VersionController {

    private static final String urlString = "http://dpdesktop.sourceforge.net/dotproject/service/version/version.php";

    private static final ResourceBundle i18n = ResourceBundle.getBundle("net/sf/dpdesktop/i18n/i18n");

    private MainWindow m_mainView;

    private MessageHandler m_messageHandler;

    private ConfigurationService versionService = new ConfigurationService();

    private About m_about;

    public VersionController(MainWindow mainView, About about, MessageHandler messageHandler, ConfigurationService settingsService) {
        this.m_mainView = mainView;
        this.m_about = about;
        this.m_messageHandler = messageHandler;
        try {
            if (settingsService.getFlag("notifyOnNewVersionAvailable", false)) {
                echoVersion();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        m_about.setVersion(Helper.getVersion());
    }

    private void echoVersion() throws Exception, MalformedURLException, DocumentException, IOException {
        String localVersion = Helper.getVersion();
        URL url = new URL(urlString);
        versionService.load(new InputStreamReader(url.openStream()));
        String newVersion = versionService.getValue("version");
        if (Float.valueOf(newVersion) > Float.valueOf(localVersion)) {
            m_messageHandler.info(m_mainView, i18n.getString("headline_new_version_available"), i18n.getString("message_new_version_available"));
        }
    }
}
