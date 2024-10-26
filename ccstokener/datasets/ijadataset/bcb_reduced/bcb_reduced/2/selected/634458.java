package org.p2s.interfaces.web;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.p2s.data.Artist;
import org.p2s.data.Release;
import org.p2s.data.Track;
import org.p2s.lib.Connect;
import org.p2s.lib.Helper;
import org.p2s.lib.MusicBrainz;

public class HTTPServer extends Thread {

    public void sendResource(OutputStream out, String res) throws Exception {
        Connect.connect(this.getClass().getResourceAsStream(res), out, 1024);
    }

    public void notFound(OutputStream out) throws Exception {
        out.write("Page not found!".getBytes());
    }

    public void quit(OutputStream out) throws Exception {
        out.write("\r\n".getBytes());
        out.write("<html><head></head><body>".getBytes());
        out.write("The server has exited<br />".getBytes());
        out.write("Thank you for using p2s".getBytes());
        out.write("</body></html>".getBytes());
        out.close();
        System.exit(0);
    }

    public void listReleases(OutputStream out, ArrayList releases) throws Exception {
        for (Iterator it = releases.iterator(); it.hasNext(); ) {
            Release release = (Release) it.next();
            String o = " <a target=\"player\" href=\"/player.html?id=" + release.id + "\">" + release.name + "</a> <a href=\"m3u.html?id=" + release.id + "\">m3u</a><br/>";
            out.write(o.getBytes());
        }
    }

    public void artisthtml(OutputStream out, String id) throws Exception {
        out.write("\r\n".getBytes());
        Artist artist = MusicBrainz.artist(id);
        header(out);
        out.write("<html><head></head><body>".getBytes());
        out.write(("<h1>" + artist.name + "</h1>").getBytes());
        out.write(("<h2>Albums</h2>").getBytes());
        listReleases(out, artist.album);
        out.write(("<h2>Compilations</h2>").getBytes());
        listReleases(out, artist.compilation);
        out.write(("<h2>Singles</h2>").getBytes());
        listReleases(out, artist.single);
        out.write(("<h2>Eps</h2>").getBytes());
        listReleases(out, artist.ep);
        out.write("</body></html>".getBytes());
    }

    public void searchhtml(OutputStream out, String query) throws Exception {
        out.write("\r\n".getBytes());
        ArrayList artists = MusicBrainz.searchArtist(query);
        header(out);
        out.write("<html><head></head><body>".getBytes());
        for (Iterator it = artists.iterator(); it.hasNext(); ) {
            Artist artist = (Artist) it.next();
            String o = "<a href=\"/artist.html?id=" + artist.id + "\">" + artist.name + "</a><br/>";
            out.write(o.getBytes());
        }
        if (artists.size() == 0) out.write("no artists found".getBytes());
        out.write("</body></html>".getBytes());
        footer(out);
    }

    public void musicbrainzhtml(OutputStream out) throws Exception {
        out.write("\r\n".getBytes());
        header(out);
        footer(out);
    }

    public void header(OutputStream out) throws Exception {
        out.write("<html><head></head><body>".getBytes());
        out.write("<div align=\"right\"><a href=\"quit.html\">quit</a><br /></div>".getBytes());
        out.write("<form action=\"/search.html\">Search artist: <input type=\"text\" name=\"q\"><form><br />".getBytes());
    }

    public void footer(OutputStream out) throws Exception {
        out.write("</body></html>".getBytes());
    }

    public void coverjpg(OutputStream out, String id) throws Exception {
        out.write("\r\n".getBytes());
        Release release = MusicBrainz.getRelease(id);
        if (release.asin != null) {
            String url = "http://images.amazon.com/images/P/" + release.asin + ".01._SCLZZZZZZZ_PU_PU-5_.jpg";
            HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
            try {
                Connect.connect(conn.getInputStream(), out, 1024);
            } catch (Exception e) {
                sendResource(out, "dummy.jpg");
            }
        } else {
            sendResource(out, "dummy.jpg");
        }
    }

    public void playlistxml(OutputStream out, String id) throws Exception {
        out.write("\r\n".getBytes());
        Release release = MusicBrainz.getRelease(id);
        out.write("<playlist version=\"1\" xmlns=\"http://xspf.org/ns/0/\"><trackList>\n".getBytes());
        for (Iterator it = release.tracks.iterator(); it.hasNext(); ) {
            Track track = (Track) it.next();
            out.write(("<track><title>" + Helper.escapeXml(track.name) + "</title><creator></creator><location>http://" + coreHost + ":" + corePort + "/" + track.id + ".mp3</location></track>\n").getBytes());
        }
        out.write("</trackList></playlist>".getBytes());
    }

    public void playerhtml(OutputStream out, String releaseId) throws Exception {
        out.write("\r\n".getBytes());
        Release release = MusicBrainz.getRelease(releaseId);
        out.write("<html><head><title></title>".getBytes());
        out.write("<script type=\"text/javascript\" src=\"swfobject.js\"></script>".getBytes());
        out.write("</head>".getBytes());
        out.write("<body>".getBytes());
        out.write(("<center><h2>" + release.name + "</h2></center>").getBytes());
        out.write(("<img src=\"cover.jpg?id=" + release.id + "\" alt=\"Cover\" width=\"240\" height=\"200\" /> ").getBytes());
        out.write("<p id=\"player2\"><a href=\"http://www.macromedia.com/go/getflashplayer\">Get the Flash Player</a> and restart your browser to listen this album.</p>".getBytes());
        out.write("<script type=\"text/javascript\">\n".getBytes());
        out.write("	var s = new SWFObject(\"mp3player.swf\", \"playlist\", \"240\", \"250\", \"7\");\n".getBytes());
        out.write(("	s.addVariable(\"file\",\"playlist.xml?id=" + release.id + "\");\n").getBytes());
        out.write("	s.addVariable(\"backcolor\",\"0x00000\");\n".getBytes());
        out.write("	s.addVariable(\"frontcolor\",\"0xEECCDD\");\n".getBytes());
        out.write("	s.addVariable(\"lightcolor\",\"0xCC0066\");\n".getBytes());
        out.write("	s.addVariable(\"displayheight\",\"0\");\n".getBytes());
        out.write("	s.addVariable(\"repeat\",\"list\");\n".getBytes());
        out.write("	s.addVariable(\"shuffle\",\"false\");\n".getBytes());
        out.write("	s.addVariable(\"autostart\",\"true\");\n".getBytes());
        out.write("	//s.addVariable(\"image\",\"cover.jpg\");\n".getBytes());
        out.write("	s.write(\"player2\");\n".getBytes());
        out.write("</script>\n".getBytes());
        out.write(("<a href=\"#\" onclick=\"javascript:window.open('http://www.amazon.com/gp/product/" + release.asin + "?ie=UTF8&tag=p2s-20&linkCode=as2&camp=1789&creative=9325&creativeASIN=" + release.asin + "');\">buy this album at amazon</a>\n").getBytes());
        out.write("</body></html>".getBytes());
    }

    public void m3u(OutputStream out, String releaseId) throws Exception {
        out.write("Content-type: audio/x-mpegurl\r\n\r\n".getBytes());
        Release release = MusicBrainz.getRelease(releaseId);
        out.write("#EXTM3U\n\n".getBytes());
        for (Iterator it = release.tracks.iterator(); it.hasNext(); ) {
            Track track = (Track) it.next();
            out.write(("#EXTINF:" + track.duration + "," + track.name + "\n").getBytes());
            out.write(("http://" + coreHost + ":" + corePort + "/" + track.id + ".mp3?fileName=" + track.name + "\n\n").getBytes());
        }
    }

    public void mainhtml(OutputStream out) throws Exception {
        out.write("\r\n".getBytes());
        out.write("<html><head><title></title>".getBytes());
        out.write("</head>".getBytes());
        out.write("<h1>P2S, Peer to Speaker</h1>".getBytes());
        out.write("<body>".getBytes());
        out.write("</body></html>".getBytes());
    }

    public void indexhtml(OutputStream out) throws Exception {
        out.write("\r\n".getBytes());
        out.write("<html><head><title>P2S, Peer to Speaker</title>".getBytes());
        out.write("<frameset cols=\"25%,75%\">".getBytes());
        out.write("  <frame src=\"main.html\" name=\"player\">".getBytes());
        out.write("  <frame src=\"musicbrainz.html\" name=\"musicbrainz\">".getBytes());
        out.write("</frameset>".getBytes());
        out.write("</html>".getBytes());
    }

    public void processParams(String path, OutputStream out, HashMap params) throws Exception {
        if (path.equals("/")) indexhtml(out); else if (path.equals("/search.html")) searchhtml(out, (String) params.get("q")); else if (path.equals("/artist.html")) artisthtml(out, (String) params.get("id")); else if (path.equals("/playlist.xml")) playlistxml(out, (String) params.get("id")); else if (path.equals("/musicbrainz.html")) musicbrainzhtml(out); else if (path.equals("/main.html")) mainhtml(out); else if (path.equals("/player.html")) playerhtml(out, (String) params.get("id")); else if (path.equals("/m3u.html")) m3u(out, (String) params.get("id")); else if (path.equals("/cover.jpg")) coverjpg(out, (String) params.get("id")); else if (path.equals("/mp3player.swf")) sendResource(out, "mp3player.swf"); else if (path.equals("/swfobject.js")) sendResource(out, "swfobject.js"); else if (path.equals("/quit.html")) quit(out); else notFound(out);
    }

    HashMap getParams(String params) {
        HashMap parMap = new HashMap();
        String pars[] = params.split("&");
        for (int i = 0; i < pars.length; i++) {
            String keyval[] = pars[i].split("=");
            if (keyval.length > 1) parMap.put(keyval[0], keyval[1]);
        }
        return parMap;
    }

    public void process(String path, OutputStream out) throws Exception {
        System.out.println(path);
        HashMap params = new HashMap();
        int idx = path.indexOf('?');
        if (idx != -1) {
            params = getParams(path.substring(idx + 1, path.length()));
            path = path.substring(0, idx);
        }
        processParams(path, out, params);
    }

    public void run() {
        try {
            InputStream streamFromClient = client.getInputStream();
            OutputStream streamToClient = client.getOutputStream();
            StringBuffer path = new StringBuffer();
            streamFromClient.skip(4);
            int b = streamFromClient.read();
            while (b != ' ') {
                path.append((char) b);
                b = streamFromClient.read();
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(streamFromClient));
            String l = in.readLine();
            while (l.length() > 0) {
                l = in.readLine();
            }
            streamToClient.write("HTTP/1.1 200 OK\r\n".getBytes());
            process(path.toString(), streamToClient);
            streamToClient.close();
        } catch (Exception e) {
        }
    }

    String coreHost;

    int corePort;

    Socket client;

    public HTTPServer(String coreHost, int corePort, Socket client) {
        super();
        this.coreHost = coreHost;
        this.corePort = corePort;
        this.client = client;
    }

    public static void listen(int localport, String coreHost, int corePort) throws Exception {
        ServerSocket ss = new ServerSocket(localport);
        while (true) {
            Socket client = ss.accept();
            HTTPServer server = new HTTPServer(coreHost, corePort, client);
            server.start();
        }
    }

    public static void main(String args[]) throws Exception {
        listen(HttpConfig.getInstance().getWebPort(), HttpConfig.getInstance().getCoreHost(), HttpConfig.getInstance().getCorePort());
    }
}
