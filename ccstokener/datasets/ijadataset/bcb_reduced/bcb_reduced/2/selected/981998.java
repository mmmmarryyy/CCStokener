package org.paquitosoft.namtia.session.facade;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Calendar;
import org.paquitosoft.namtia.common.SystemValues;
import org.paquitosoft.namtia.common.exceptions.MusicBrainzException;

/**
 *
 * @author telemaco
 */
public class MusicBrainzFacade {

    private static final String SERVER_ARTIST = "http://musicbrainz.org/ws/1/artist/";

    private static final String SERVER_ALBUM = "http://musicbrainz.org/ws/1/release/";

    private static final String SERVER_TRACK = "http://musicbrainz.org/ws/1/track/";

    private static final String RESULT_TYPE = "?type=xml";

    private static final String SERVER_RESPONSE_FILE = SystemValues.getUserHome() + SystemValues.getFileSeparator() + ((SystemValues.getOsName().toUpperCase().indexOf("WINDOWS") != -1) ? "" : ".") + "namtia/xml/serverResponse.xml";

    /**
     * Creates a new instance of MusicBrainzFacade
     */
    public MusicBrainzFacade() {
    }

    /**
     *  This method is used to find an artist by name
     *  @param String -> artist name
     *  @return File -> xml server response
     */
    public File findArtistLite(String artistName) throws MusicBrainzException {
        String query = SERVER_ARTIST + "?type=xml" + "&name=" + artistName.replace(' ', '+');
        return this.sendQuery(query);
    }

    /**
     *  This method is used to find an album.
     *  @param String albumTitle
     *  @param String artistName (this can be null)
     *  @return File -> xml server response
     */
    public File findAlbumLite(String albumTitle, String artistName) throws MusicBrainzException {
        String query = SERVER_ALBUM + "?type=xml";
        if (artistName != null && artistName.length() > 0) {
            query += "&artist=" + artistName.replace(' ', '+');
        }
        query += "&title=" + albumTitle.replace(' ', '+');
        query += "&releasetypes=Official&limit=5";
        return this.sendQuery(query);
    }

    /**
     *  This method is used to find the artist's album when we know
     *  artist's music brainz identifier.
     *  @param String -> artist's music brainz identifier
     *  @return File -> xml server response
     */
    public File findAlbumLiteByArtist(String MBIdArtist) throws MusicBrainzException {
        String query = SERVER_ALBUM + "?type=xml" + "&artistid=" + MBIdArtist + "&releasetypes=Official" + "&limit=20";
        return this.sendQuery(query);
    }

    public File findTracksByAlbumId(String albumMusicBrainzId) throws MusicBrainzException {
        String query = SERVER_ALBUM + albumMusicBrainzId + "?type=xml&inc=tracks";
        return this.sendQuery(query);
    }

    public File findTrackByTitle(String trackTitle, String artistName) throws MusicBrainzException {
        String query = SERVER_TRACK + "?type=xml";
        query += "&title=" + trackTitle.replace(' ', '+');
        if (artistName != null) {
            query += "&artist=" + artistName.replace(' ', '+');
        }
        query += "&limit=10";
        return this.sendQuery(query);
    }

    private File sendQuery(String query) throws MusicBrainzException {
        File xmlServerResponse = null;
        try {
            xmlServerResponse = new File(SERVER_RESPONSE_FILE);
            long start = Calendar.getInstance().getTimeInMillis();
            System.out.println("\n\n++++++++++++++++++++++++++++++++++++++++++++++++++++");
            System.out.println("    consulta de busqueda -> " + query);
            URL url = new URL(query);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String response = "";
            String line = "";
            System.out.println("    Respuesta del servidor: \n");
            while ((line = in.readLine()) != null) {
                response += line;
            }
            xmlServerResponse = new File(SERVER_RESPONSE_FILE);
            System.out.println("    Ruta del archivo XML -> " + xmlServerResponse.getAbsolutePath());
            BufferedWriter out = new BufferedWriter(new FileWriter(xmlServerResponse));
            out.write(response);
            out.close();
            System.out.println("Tamanho del xmlFile -> " + xmlServerResponse.length());
            long ahora = (Calendar.getInstance().getTimeInMillis() - start);
            System.out.println(" Tiempo transcurrido en la consulta (en milesimas) -> " + ahora);
            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++\n\n");
        } catch (IOException e) {
            e.printStackTrace();
            String msg = e.getMessage();
            if (e instanceof FileNotFoundException) {
                msg = "ERROR: MusicBrainz URL used is not found:\n" + msg;
            } else {
            }
            throw new MusicBrainzException(msg);
        }
        return xmlServerResponse;
    }
}
