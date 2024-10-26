package hermes.browser.jython;

import hermes.Hermes;
import hermes.SystemProperties;
import hermes.browser.HermesBrowser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import org.apache.log4j.Logger;
import org.python.core.PyException;
import org.python.util.PythonInterpreter;
import com.artenum.jyconsole.JyConsole;

/**
 * @author colincrist@hermesjms.com
 * @version $Id: JythonManager.java,v 1.2 2006/09/16 15:49:24 colincrist Exp $
 */
public class JythonManager {

    private static final Logger log = Logger.getLogger(JythonManager.class);

    private JyConsole jyConsole = new JyConsole();

    public JythonManager() {
        init();
    }

    public PythonInterpreter getInterpreter() {
        return jyConsole.getPythonInterpreter();
    }

    public JyConsole getConsole() {
        return jyConsole;
    }

    public void exec(String info, InputStream istream) {
        try {
            getInterpreter().execfile(istream);
        } catch (PyException ex) {
            HermesBrowser.getBrowser().getDefaultMessageSink().add("Error " + info + ": " + ex.getMessage());
            log.error(ex.getMessage(), ex);
        } catch (Exception ex) {
            HermesBrowser.getBrowser().getDefaultMessageSink().add("Error " + info + ": " + ex.getMessage());
            log.error(ex.getMessage(), ex);
        }
    }

    public void init() {
        log.debug("bootstraping jython...");
        exec("Bootstrapping Jython", getClass().getResourceAsStream("bootstrap.py"));
        File bootstrapFile = new File(System.getProperty("user.home") + SystemProperties.FILE_SEPARATOR + ".hermes" + SystemProperties.FILE_SEPARATOR + "hermesrc.py");
        if (bootstrapFile.exists()) {
            try {
                log.debug("reading " + bootstrapFile.getName());
                exec("Reading hermesrc.py", new FileInputStream(bootstrapFile));
                Hermes.ui.getDefaultMessageSink().add("Loaded hermesrc.py");
            } catch (FileNotFoundException e) {
                log.error(e.getMessage(), e);
            }
        } else {
            log.debug("Unable to locate a hermesrc.py in " + System.getProperty("user.home"));
        }
        try {
            if (System.getProperty("hermes.python.url") != null) {
                String url = System.getProperty("hermes.python.url");
                log.debug("reading " + url);
                exec("Reading " + url, new URL(url).openStream());
            } else {
                log.debug("no hermes.python.url set");
            }
        } catch (Exception ex) {
            HermesBrowser.getBrowser().showErrorDialog(ex);
        }
    }
}
