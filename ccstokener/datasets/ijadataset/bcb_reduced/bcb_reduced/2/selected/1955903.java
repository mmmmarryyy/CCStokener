package net.sourceforge.juploader.upload.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sourceforge.juploader.upload.ClientHttpRequest;

/**
 * Klasa umożliwiająca wysyłanie obrazków na hosting Plikojad.
 *
 * @author Adam Pawelec
 * @since 0.10.0
 */
public class Plikojad extends BasicServer {

    public Plikojad() {
        final ResourceBundle bundle = java.util.ResourceBundle.getBundle("net/sourceforge/juploader/upload/server/Bundle");
        fieldNames = new String[] { bundle.getString("PLink01"), bundle.getString("PLink02") };
    }

    public String[] uploadFile() {
        try {
            String sessionID = getSessionID();
            if (sessionID == null) {
                return null;
            }
            ClientHttpRequest request = new ClientHttpRequest("http://bb.plikojad.pl/cgi-bin/upload.cgi");
            request.setParameter("uploaded_file", new File(fileName));
            request.setParameter("sessionid", sessionID);
            InputStream stream = request.post();
            links = parseLinks(stream);
        } catch (IOException ex) {
            Logger.getLogger(Plikojad.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            return links;
        }
    }

    private String getSessionID() {
        String id = null;
        try {
            URL url = new URL("http://plikojad.pl/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String headerField = conn.getHeaderField("Set-Cookie");
            id = headerField.substring(headerField.indexOf('=') + 1, headerField.indexOf(';'));
        } catch (IOException ex) {
            Logger.getLogger(Plikojad.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            return id;
        }
    }

    private String[] parseLinks(InputStream stream) {
        String[] parsedLinks = new String[2];
        Scanner in = new Scanner(stream);
        while (in.findInLine("<a href=\"") == null) {
            in.nextLine();
        }
        in.useDelimiter("\">");
        parsedLinks[0] = in.next();
        in.findInLine("<a href=\"");
        parsedLinks[1] = in.next();
        if (parsedLinks[0].startsWith("http://")) {
            return parsedLinks;
        } else {
            return null;
        }
    }

    public Servers getServer() {
        return Servers.PLIKOJAD;
    }

    public boolean isAcceptable(String fileName) {
        return (new File(fileName).length() < 104857600);
    }
}
