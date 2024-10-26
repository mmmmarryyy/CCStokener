package fr.fg.server.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import fr.fg.server.data.DataAccess;
import fr.fg.server.data.IllegalOperationException;
import fr.fg.server.data.Message;
import fr.fg.server.util.Config;
import fr.fg.server.util.LoggingSystem;

public class MessageTools {

    public static final int KEEP_QUOTES = 1 << 1;

    /**
	 * Ajoute un message à la messagerie d'un joueur.
	 *
	 * @param message Le message a envoyer.
	 */
    public static void sendMessage(Message message) {
        DataAccess.save(message);
        UpdateTools.queueNewMessageUpdate(message.getIdPlayer());
    }

    public static String sanitizeHTML(String html) throws IllegalOperationException {
        try {
            String data = "content=" + URLEncoder.encode(html, "UTF-8");
            URL url = new URL(Config.getSanitizer());
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(data);
            writer.flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String line;
            StringBuffer buffer = new StringBuffer();
            while ((line = reader.readLine()) != null) buffer.append(line);
            writer.close();
            reader.close();
            return buffer.toString();
        } catch (Exception e) {
            LoggingSystem.getServerLogger().warn("Could not sanitize message.", e);
            throw new IllegalOperationException("Impossible de parser le message.");
        }
    }

    public static String tidyHTML(String html) {
        return tidyHTML(html, 0);
    }

    public static String tidyHTML(String html, int options) {
        html = html.replaceAll("&(?!(#[0-9]+|amp|nbsp|lt|gt|quot);)", "&amp;");
        html = html.replace("<", "&lt;");
        html = html.replace(">", "&gt;");
        if ((options & KEEP_QUOTES) == 0) html = html.replace("\"", "&quot;");
        return html;
    }
}
