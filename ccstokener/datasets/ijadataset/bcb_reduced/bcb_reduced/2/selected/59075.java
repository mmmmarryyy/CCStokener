package mipt.util.log;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * The {@link Log} implementation using java.util.logging.Logger.
 * If configure() is not called, the file = logging.properties and must be placed in classes root.
 * If the file is not found, default configuration remains (e.g. java_home/lib/logging.properties).
 * Note for web applications: container may use java.util.logging for its own purposes
 *  so there can be a conflict between its configurations and the application configuration.
 * Note for standalone applications: if you have system property "java.util.logging.config.class" or
 *  "java.util.logging.config.file", then configuration via this class does not have much sense.
 * @author Evdokimov
 */
public class Logging extends LogT<Logger> {

    /**
	 * @see ru.ipccenter.study.Log#log(java.util.logging.Level, java.lang.String, java.lang.Throwable)
	 */
    public final void log(Level level, String message, Throwable exception) {
        getLogger().log(level, message, exception);
    }

    /**
	 * @see ru.ipccenter.study.Log#configure(java.net.URL)
	 */
    public void configure(URL url) throws IOException {
        LogManager.getLogManager().readConfiguration(url.openStream());
    }

    /**
	 * @see ru.ipccenter.study.Log#initLogger()
	 */
    protected Logger initLogger() {
        logger = Logger.getLogger(Logging.class.getName());
        if (!configured) configure("logging.properties");
        return logger;
    }
}
