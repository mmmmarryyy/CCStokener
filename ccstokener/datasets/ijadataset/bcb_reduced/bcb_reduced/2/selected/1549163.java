package entagged.freedb;

import java.util.List;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Freedb {

    public class SimpleTrack implements FreedbTrack {

        private float length;

        public SimpleTrack(float sec) {
            this.length = sec;
        }

        public SimpleTrack(int min, int sec) {
            this(min, sec, 0);
        }

        public SimpleTrack(int min, int sec, float frac) {
            this.length = min * 60 + sec + frac;
        }

        public int getLength() {
            return (int) this.length;
        }

        public float getPreciseLength() {
            return length;
        }
    }

    public static void main(String[] args) throws Exception {
        Freedb freedb = new Freedb();
        String[] serv = freedb.getAvailableServers();
        for (int i = 0; i < serv.length; i++) System.out.println(serv[i]);
    }

    private FreedbSettings settings;

    public Freedb() {
        this.settings = new FreedbSettings();
    }

    public Freedb(FreedbSettings conn) {
        this.settings = conn;
    }

    private String searchFreedb(String search) throws FreedbException {
        setupConnection();
        String terms = search.replaceAll(" ", "+");
        URL url = null;
        try {
            url = new URL("http://www.freedb.org/freedb_search.php?words=" + terms + "&allfields=NO&fields=artist&fields=title&allcats=YES&grouping=none");
        } catch (MalformedURLException e) {
            throw new FreedbException("The URL: " + url + " is invalid, remove any accents from the search terms and try again");
        }
        assert url != null;
        URLConnection connection = null;
        try {
            connection = url.openConnection();
            setupProxy(connection);
        } catch (IOException e) {
            throw new FreedbException("Error while trying to connect to freedb server, " + e.getMessage() + ". Check your internet connection settings.");
        }
        assert connection != null;
        String output = null;
        try {
            InputStreamReader isr = new InputStreamReader(connection.getInputStream());
            BufferedReader in = new BufferedReader(isr);
            String inputLine;
            output = "";
            while ((inputLine = in.readLine()) != null) output += inputLine + "\n";
            in.close();
        } catch (IOException e) {
            throw new FreedbException("Error while trying read data from freedb server, " + e.getMessage() + ". Check your internet connection settings.");
        }
        assert output != null;
        return output;
    }

    protected String askFreedb(String command) throws FreedbException {
        setupConnection();
        URL url = null;
        try {
            url = new URL("http://" + this.settings.getServer() + ":80/~cddb/cddb.cgi");
        } catch (MalformedURLException e) {
            throw new FreedbException("The URL: " + url + " is invalid, correct the server setting");
        }
        assert url != null;
        URLConnection connection = null;
        try {
            connection = url.openConnection();
            setupProxy(connection);
            connection.setDoOutput(true);
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            out.println("cmd=" + command + "&hello=" + this.settings.getUserLogin() + "+" + this.settings.getUserDomain() + "+" + this.settings.getClientName() + "+" + this.settings.getClientVersion() + "&proto=" + this.settings.getProtocol());
            out.close();
        } catch (IOException e) {
            throw new FreedbException("Error while trying to connect to freedb server, " + e.getMessage() + ". Check your internet connection settings.");
        }
        assert connection != null;
        String output = null;
        try {
            InputStreamReader isr;
            try {
                isr = new InputStreamReader(connection.getInputStream(), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                isr = new InputStreamReader(connection.getInputStream());
            }
            BufferedReader in = new BufferedReader(isr);
            String inputLine;
            output = "";
            while ((inputLine = in.readLine()) != null) output += inputLine + "\n";
            in.close();
        } catch (IOException e) {
            throw new FreedbException("Error while trying read data from freedb server, " + e.getMessage() + ". Check your internet connection settings.");
        }
        assert output != null;
        if (output.startsWith("4") || output.startsWith("5")) throw new FreedbException("Freedb server returned an error: \"" + output + "\"");
        return output;
    }

    private void setupProxy(URLConnection connection) {
        if ((this.settings.getInetConn() == FreedbSettings.INETCONN_PROXY_WITH_AUTHENTICATION)) {
            String proxyUPB64 = base64Encode(this.settings.getProxyUser() + ":" + this.settings.getProxyPass());
            connection.setRequestProperty("Proxy-Authorization", "Basic " + proxyUPB64);
        }
    }

    private void setupConnection() {
        if (this.settings.getInetConn() != FreedbSettings.INETCONN_DIRECT) {
            System.setProperty("http.proxyHost", this.settings.getProxyHost());
            System.setProperty("http.proxyPort", this.settings.getProxyPort());
        } else {
            System.getProperties().remove("http.proxyHost");
            System.getProperties().remove("http.proxyPort");
        }
    }

    private String getQueryCommand(FreedbAlbum album) {
        StringBuffer command = new StringBuffer("cddb+query+");
        command.append(album.getDiscId()).append("+");
        int tracks = album.getTracksNumber();
        command.append(tracks).append("+");
        int[] trackOffsets = album.getTrackOffsets();
        for (int j = 0; j < trackOffsets.length - 1; j++) command.append(trackOffsets[j]).append("+");
        command.append((trackOffsets[tracks] - trackOffsets[0]) / 75);
        return command.toString();
    }

    protected String getReadCommand(FreedbQueryResult query) {
        return "cddb+read+" + query.getCategory() + "+" + query.getDiscId();
    }

    private String getReadCommand(String genre, String id) {
        return "cddb+read+" + genre + "+" + id;
    }

    public String[] getAvailableServers() throws FreedbException {
        String answer = askFreedb("sites");
        StringTokenizer st = new StringTokenizer(answer, "\n");
        List l = new LinkedList();
        while (st.hasMoreTokens()) {
            String line = st.nextToken();
            if (!line.startsWith("2") && !line.startsWith(".")) {
                String[] sline = line.split(" ", 7);
                if (sline[1].equals("http")) l.add(sline[0]);
            }
        }
        String[] servers = new String[l.size()];
        Iterator it = l.iterator();
        int i = 0;
        while (it.hasNext()) {
            servers[i] = (String) it.next();
            i++;
        }
        return servers;
    }

    public FreedbQueryResult[] query(FreedbAlbum album) throws FreedbException {
        String command = getQueryCommand(album);
        String queryAnswer = askFreedb(command);
        System.out.println("CDDB ANSWER: " + queryAnswer);
        StringTokenizer st = new StringTokenizer(queryAnswer, "\n");
        String[] answers = new String[st.countTokens()];
        for (int i = 0; i < answers.length; i++) answers[i] = st.nextToken();
        if (queryAnswer.startsWith("200")) return new FreedbQueryResult[] { new FreedbQueryResult(answers[0].substring(4), true) }; else if (!queryAnswer.startsWith("202")) {
            FreedbQueryResult[] queryResults = new FreedbQueryResult[answers.length - 2];
            boolean exact = queryAnswer.startsWith("210");
            for (int i = 0; i < queryResults.length; i++) {
                queryResults[i] = new FreedbQueryResult(answers[i + 1], exact);
            }
            return queryResults;
        } else return null;
    }

    public FreedbQueryResult[] query(FreedbTrack[] tracks) throws FreedbException {
        return query(new FreedbAlbum(tracks));
    }

    public FreedbQueryResult[] query(float[] times) throws FreedbException {
        SimpleTrack[] tracks = new SimpleTrack[times.length];
        for (int i = 0; i < tracks.length; i++) tracks[i] = new SimpleTrack(times[i]);
        return query(tracks);
    }

    public FreedbQueryResult[] query(int[] times) throws FreedbException {
        float[] translated = new float[times.length];
        for (int i = 0; i < translated.length; i++) {
            translated[i] = times[i];
        }
        return query(translated);
    }

    private boolean bigMatch(String line, String[] infos) {
        Matcher matcher = Pattern.compile(".+?php\\?cat=(.+?)&id=(.+?)\">(.+?) \\/ (.+?)<\\/(.*)").matcher(line);
        if (matcher.matches()) {
            infos[0] = matcher.group(1);
            infos[1] = matcher.group(2);
            infos[2] = matcher.group(3);
            infos[2] += " / " + matcher.group(4);
            infos[3] = matcher.group(5);
        }
        return matcher.matches();
    }

    private boolean smallMatch(String line, String[] infos) {
        Matcher matcher = Pattern.compile(".+?php\\?cat=(.+?)&id=(.+?)\">.*<\\/.*").matcher(line);
        if (matcher.matches()) {
            infos[0] = matcher.group(1);
            infos[1] = matcher.group(2);
        }
        return matcher.matches();
    }

    public FreedbQueryResult[] query(String search) throws FreedbException {
        String answer = searchFreedb(search);
        StringTokenizer st = new StringTokenizer(answer, "\n");
        LinkedList list = new LinkedList();
        String[] infos = new String[4];
        while (st.hasMoreTokens()) {
            String line = st.nextToken();
            if (bigMatch(line, infos)) {
                list.add(new FreedbQueryResult(infos[0] + " " + infos[1] + " " + infos[2]));
                if (smallMatch(infos[3], infos)) list.add(new FreedbQueryResult(infos[0] + " " + infos[1] + " " + infos[2]));
            } else if (smallMatch(line, infos)) list.add(new FreedbQueryResult(infos[0] + " " + infos[1] + " " + infos[2]));
        }
        FreedbQueryResult[] results = new FreedbQueryResult[list.size()];
        Iterator it = list.iterator();
        int i = 0;
        while (it.hasNext()) {
            results[i] = (FreedbQueryResult) it.next();
            i++;
        }
        return results;
    }

    public FreedbReadResult read(FreedbQueryResult query) throws FreedbException {
        String command = getReadCommand(query);
        String queryAnswer = askFreedb(command);
        return new FreedbReadResult(queryAnswer, query.isExactMatch());
    }

    public FreedbReadResult read(String genre, String id) throws FreedbException {
        String command = getReadCommand(genre, id);
        String queryAnswer = askFreedb(command);
        return new FreedbReadResult(queryAnswer, genre);
    }

    /**
 * Encode a String in Base64
 * @param whatToEncode
 * @return the encoded string
 */
    public String base64Encode(String whatToEncode) {
        StringBuffer salida = new StringBuffer();
        for (int i = 0; i < whatToEncode.length(); i += 3) {
            salida.append(encode3(whatToEncode, i));
        }
        return salida.toString();
    }

    /**
 * Returns a 4 character string that corresponds to the codification of 3
 * characters of whatToEncode, starting at whereToStart.  Does padding when needed
 * @param whatToEncode
 * @param whereToStart
 * @return the char encoded
 */
    private String encode3(String whatToEncode, int whereToStart) {
        String map = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
        int finish = whereToStart + 2;
        int pads = finish - whatToEncode.length() + 1;
        if (pads > 2) return null;
        char i1 = whatToEncode.charAt(whereToStart);
        char i2 = 0;
        char i3 = 0;
        if (pads < 2) {
            i2 = whatToEncode.charAt(whereToStart + 1);
            if (pads < 1) i3 = whatToEncode.charAt(whereToStart + 2);
        }
        int o1, o2, o3, o4;
        o1 = i1 >> 2;
        o2 = ((i1 & 3) << 4) | (i2 >> 4);
        o3 = (pads == 2) ? 64 : ((i2 & 0x0F) << 2) | (i3 >> 6);
        o4 = (pads > 0) ? 64 : (i3 & 0x3F);
        return map.substring(o1, o1 + 1) + map.substring(o2, o2 + 1) + map.substring(o3, o3 + 1) + map.substring(o4, o4 + 1);
    }
}
