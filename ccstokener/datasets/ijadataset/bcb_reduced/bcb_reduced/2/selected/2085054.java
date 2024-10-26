package vodoo.catalog.network;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;
import vodoo.catalog.Catalog;

public class HTTPCatalogReader extends CatalogReader {

    /**
	 * Url de connection
	 */
    private URLConnection connection;

    /**
	 * Surcharge de la m�thode abstraite Construire
	 * Permet de se connecter en HTTP
	 */
    protected void Construire() {
        URL urlCatalog;
        try {
            urlCatalog = new URL("http://" + ip + ":" + port + "/catalogue.txt");
            connection = urlCatalog.openConnection();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Constructeur
	 * @param ip
	 * @param port
	 * @throws IOException
	 */
    public HTTPCatalogReader(String ip, int port) throws IOException {
        super(ip, port);
    }

    /**
	 * Permet de r�cuperer un catalogue en HTTP � partir de l'ip 
	 * et le port pass� lors de la construction
	 * @throws IOException
	 */
    public void GetCatalog() throws IOException {
        Thread t_http = new Thread() {

            public void run() {
                InputStream fluxFichier = null;
                try {
                    fluxFichier = connection.getInputStream();
                } catch (IOException e) {
                    return;
                }
                byte contenuFichier[] = new byte[connection.getContentLength()];
                int octetsLus = 0;
                try {
                    octetsLus = fluxFichier.read(contenuFichier);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String chaineAParser = new String(contenuFichier, 0, octetsLus);
                Catalog newCatalog = null;
                try {
                    newCatalog = Parse(chaineAParser);
                } catch (ParseCatalogException ex) {
                    ex.printStackTrace();
                    Logger.getLogger("Catalogue.network").fine("ParseError : " + ex.getMessage());
                } catch (StringIndexOutOfBoundsException ex) {
                    ex.printStackTrace();
                }
                if (newCatalog != null) {
                    fireNewCatalog(newCatalog);
                }
            }

            ;
        };
        t_http.start();
    }
}
