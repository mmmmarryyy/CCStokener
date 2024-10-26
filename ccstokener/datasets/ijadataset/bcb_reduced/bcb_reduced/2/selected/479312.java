package memodivx;

import memodivx.divx.Film;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class MovieCovers {

    private static ArrayList tousLesFilms;

    private static ArrayList getTousLesFilms() throws ConnectionException {
        if (tousLesFilms == null) recupList();
        return tousLesFilms;
    }

    public static ArrayList getMatchingFilms(String request) throws ConnectionException {
        ArrayList matchingIDMC = new ArrayList();
        request = ".*" + request.toUpperCase() + ".*";
        for (int i = 0; i < getTousLesFilms().size(); i++) {
            if (getIDMC(i).matches(request)) {
                matchingIDMC.add(getIDMC(i));
            }
        }
        return matchingIDMC;
    }

    private static String getIDMC(int i) throws ConnectionException {
        return (String) getTousLesFilms().get(i);
    }

    public static void recupList() throws ConnectionException {
        try {
            tousLesFilms = new ArrayList();
            URL recup = new URL("http://www.moviecovers.com/DATA/movies.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(recup.openStream()));
            String line = br.readLine();
            while ((line = br.readLine()) != null) tousLesFilms.add(toIDMC(line));
        } catch (Exception e1) {
            throw new ConnectionException("Impossible d'�tablir la connection � Moviecovers.com");
        }
    }

    public static String toIDMC(String s) {
        if (s.charAt(s.length() - 1) != ')') {
            return s;
        }
        int index = s.length() - 2;
        while ((index > 0) && (s.charAt(index) != '(')) index--;
        String temp = s.substring(index + 1, s.length() - 1);
        try {
            Integer.parseInt(temp);
            return s;
        } catch (Exception e) {
            if (temp.charAt(temp.length() - 1) != '\'') {
                temp += " ";
            }
            temp += s.substring(0, index);
            return temp.trim();
        }
    }

    public static Film recupFilm(String idmc) throws ConnectionException {
        try {
            URL url = new URL("http://www.moviecovers.com/getfilm.html");
            URLConnection connect = url.openConnection();
            connect.setDoOutput(true);
            connect.connect();
            PrintWriter os = new PrintWriter(connect.getOutputStream());
            os.print("idmc=" + encode(toIDMC(idmc)));
            os.close();
            BufferedReader br = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            Film f = new Film(br);
            try {
                recupImage(idmc, f.getTitle());
            } catch (ConnectionException ce) {
            }
            return f;
        } catch (Exception e) {
            throw new ConnectionException("Impossible d'�tablir la connection � Moviecovers.com");
        }
    }

    private static void recupImage(String idmc, String title) throws ConnectionException {
        try {
            URL url = new URL("http://www.moviecovers.com/getjpg.html/" + encode(toIDMC(idmc)) + ".jpg");
            BufferedInputStream br = new BufferedInputStream(url.openStream());
            File f = new File("image/" + title + ".jpg");
            FileOutputStream out = new FileOutputStream(f);
            byte[] buffer = new byte[100 * 1024];
            int nbLecture;
            while ((nbLecture = br.read(buffer)) != -1) {
                out.write(buffer, 0, nbLecture);
            }
        } catch (Exception e) {
            throw new ConnectionException("Impossible d'�tablir la connection � Moviecovers.com");
        }
    }

    private static String encode(String idmc) {
        String result = "";
        int i = 0;
        while (i < idmc.length()) {
            if (idmc.charAt(i) == ' ') {
                result += "%20";
            } else {
                result += idmc.charAt(i);
            }
            i++;
        }
        return result;
    }

    public static void main(String[] args) {
        System.out.println(MovieCovers.toIDMC("Hello You (l')"));
    }
}
