package gmusic.ma.batch;

import gmusic.ma.dao.MetalArchivesDAO;
import gmusic.ma.dao.impl.MetalArchivesDAOImpl;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import org.apache.log4j.Logger;

public class RecuperationGroupe {

    private static final Logger log = Logger.getLogger(RecuperationGroupe.class);

    /**
	 * @param args
	 * @throws URISyntaxException
	 * @throws IOException
	 * @throws SQLException
	 * @throws NumberFormatException
	 */
    public static void main(String[] args) throws URISyntaxException, IOException, NumberFormatException, SQLException {
        String[] lettres = new String[] { "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y" };
        for (int i = 0; i < lettres.length; i++) {
            URL fileURL = new URL("http://www.metal-archives.com/browseL.php?l=" + lettres[i]);
            URLConnection urlConnection = fileURL.openConnection();
            InputStream httpStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(httpStream));
            String ligne = null;
            boolean b = true;
            int li;
            int li2;
            String href;
            int nbGroupes = 0;
            MetalArchivesDAO mag = MetalArchivesDAOImpl.getInstance();
            while ((ligne = br.readLine()) != null && b) {
                if (ligne.indexOf("<a href='band.php?id=") != -1) {
                    StringBuffer sb = new StringBuffer(ligne.substring(ligne.indexOf("<a href='band.php?id=")));
                    li = 0;
                    li2 = sb.indexOf("</a><br>", li);
                    while (li2 != -1) {
                        href = sb.substring(li, li2);
                        nbGroupes++;
                        li = li2 + 8;
                        li2 = sb.indexOf("</a><br>", li);
                    }
                }
            }
            br.close();
            httpStream.close();
            log.info("Nombre de groupe inseres [" + lettres[i] + "] = " + nbGroupes);
        }
    }
}
