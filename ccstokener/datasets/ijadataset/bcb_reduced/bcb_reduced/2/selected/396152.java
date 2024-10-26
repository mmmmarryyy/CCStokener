package org.paquitosoft.namtia.session.facade;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Calendar;
import org.paquitosoft.namtia.session.actions.lyrics.LyricsHtmlResponseParser;

/**
 *
 * @author paquitosoft
 */
public class LyricsFacade {

    private static final String LYRC_COM_AR = "http://lyrc.com.ar/en/";

    /** Creates a new instance of LyricsFacade */
    public LyricsFacade() {
    }

    public String findSongLyrics(String songTitle, String artistName) {
        songTitle = songTitle.replace(' ', '+');
        String query = LYRC_COM_AR + "tema1en.php?songname=" + songTitle;
        if (artistName != null) {
            artistName = artistName.replace(' ', '+');
            query += "&artist=" + artistName;
        }
        return this.sendQuery(query);
    }

    public String getSongLyrics(String lyricsLink) {
        String query = LYRC_COM_AR + lyricsLink;
        return this.sendQuery(query);
    }

    private String sendQuery(String query) {
        File xmlServerResponse = null;
        String serverResponse = "";
        try {
            long start = Calendar.getInstance().getTimeInMillis();
            System.out.println("\n\n++++++++++++++++++++++++++++++++++++++++++++++++++++");
            System.out.println("    consulta de busqueda -> " + query);
            URL url = new URL(query);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = "";
            while ((line = in.readLine()) != null) {
                serverResponse += line;
            }
            long ahora = (Calendar.getInstance().getTimeInMillis() - start);
            System.out.println(" Tiempo transcurrido en la consulta (en milesimas) -> " + ahora);
            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++\n\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return serverResponse;
    }
}
