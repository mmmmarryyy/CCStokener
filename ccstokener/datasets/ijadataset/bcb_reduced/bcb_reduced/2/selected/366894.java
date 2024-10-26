package clump.message;

import clump.kernel.engine.ModelLoader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;

public final class MessageProvider {

    private static MessageProvider DEFAULT;

    private static MessageProvider CURRENT;

    private static synchronized MessageProvider getProvider() {
        if (CURRENT == null) {
            try {
                DEFAULT = new MessageProvider(Locale.ENGLISH);
                try {
                    CURRENT = new MessageProvider(Locale.getDefault());
                } catch (IOException e) {
                    CURRENT = DEFAULT;
                }
            } catch (IOException e) {
                DEFAULT = new MessageProvider();
                CURRENT = DEFAULT;
            }
        }
        return CURRENT;
    }

    public static String getMessage(String key) {
        return getMessage(key, new Object[0]);
    }

    public static String getMessage(String key, Object[] parameters) {
        return getProvider().message(key, parameters);
    }

    private final Properties properties;

    private MessageProvider() {
        this.properties = new Properties();
    }

    private MessageProvider(Locale locale) throws IOException {
        final URL url = ModelLoader.solveResource("messages/messages_" + locale);
        if (url == null) {
            throw new IOException("invalid URL");
        } else {
            final InputStream inputStream = url.openStream();
            try {
                properties = new Properties();
                properties.load(inputStream);
            } finally {
                inputStream.close();
            }
        }
    }

    private String message(String key, Object[] parameters) {
        final String rmessage = properties.getProperty(key);
        if (rmessage != null) {
            try {
                return new MessageFormat(rmessage).format(parameters);
            } catch (Throwable thr) {
            }
        }
        if (this == DEFAULT) {
            return "message not found for <" + key + "> in locale <" + Locale.getDefault().toString() + ">";
        } else {
            return DEFAULT.message(key, parameters);
        }
    }
}
