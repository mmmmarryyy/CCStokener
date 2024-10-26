package org.p2s.core;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opendht.OpenDht;
import org.p2s.data.Mp3File;
import org.p2s.data.Track;
import org.p2s.lib.Connect;
import org.p2s.lib.MusicBrainz;

public class CoreHTTPServer extends Thread {

    public static InputStream getHttpStream(String url, String filename) throws Exception {
        System.out.println("trying url " + url);
        Matcher matcher = Pattern.compile("^http://", Pattern.DOTALL).matcher(url);
        if (matcher.find()) {
            HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
            if ("audio/mpeg".equals(conn.getHeaderField("Content-Type"))) return new InputStreamProxy(conn.getInputStream(), filename);
            conn.getInputStream().close();
            return null;
        }
        return null;
    }

    String filename = null;

    String url = null;

    public InputStream getStream(String mbid) throws Exception {
        Track track = MusicBrainz.track(mbid);
        InputStream in = null;
        System.out.println("searching at opendht");
        try {
            ArrayList ls = OpenDht.get(mbid.getBytes());
            System.out.println("found " + ls.size() + " results");
            for (Iterator it = ls.iterator(); it.hasNext(); ) {
                String url = new String((byte[]) it.next());
                System.out.println("url found " + url);
                filename = Config.getOpenDht() + "/" + track.artist.name + " - " + track.name + ".mp3";
                in = getHttpStream(url, filename);
                if (in != null) return in;
            }
        } catch (Exception e) {
            System.out.println("dht failed " + e.getMessage());
        }
        for (Iterator it = track.puids.iterator(); it.hasNext(); ) {
            String puid = (String) it.next();
            filename = Config.getBlogMusikDir() + "/" + puid + ".mp3";
            url = "http://blogmusik.net/encapsulation.php?ID=" + puid;
            in = getHttpStream(url, filename);
            if (in != null) return in;
        }
        filename = Config.getHypemDir() + "/" + track.artist.name + " - " + track.name + ".mp3";
        ArrayList ls = HypemMp3Stream.search(track.artist.name, track.name, filename);
        if (ls.size() > 0) {
            url = (String) ls.get(0);
            in = getHttpStream(url, filename);
            if (in != null) return in;
        }
        filename = null;
        url = null;
        return this.getClass().getResourceAsStream("dummy.mp3");
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

    Socket client;

    public void run() {
        try {
            InputStream ins = client.getInputStream();
            OutputStream outs = client.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(ins));
            String header = in.readLine();
            if (header == null) return;
            String l = in.readLine();
            if (l == null) return;
            while (l.length() > 0) {
                l = in.readLine();
            }
            System.out.println(header);
            Matcher matcher = Pattern.compile("GET /([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}).mp3(.*) HTTP/1.[01]", Pattern.DOTALL).matcher(header);
            Matcher matcher2 = Pattern.compile("GET /list HTTP/1.[01]", Pattern.DOTALL).matcher(header);
            if (matcher2.find()) {
                outs.write(("HTTP/1.1 200 OK\r\n\r\n").getBytes());
                MediaLibrary ml = MediaLibrary.getInstance();
                for (Iterator it = ml.files.iterator(); it.hasNext(); ) {
                    Mp3File file = (Mp3File) it.next();
                    if (file.getMbid() != null && !file.getMbid().equals("00000000-0000-0000-0000-000000000000")) {
                        outs.write((file.getMbid() + "\r\n").getBytes());
                    }
                }
                outs.close();
            } else if (matcher.find()) {
                String mbid = matcher.group(1);
                String params = matcher.group(2);
                String filenameHeader = "";
                if (params.length() > 0) {
                    params = params.substring(1, params.length());
                    System.out.println(params);
                    HashMap parsMap = getParams(params);
                    String filename = (String) parsMap.get("fileName");
                    if (filename != null) filenameHeader = "Content-Disposition: attachment; filename=\"" + filename + "\"\r\n";
                }
                outs.write(("HTTP/1.1 200 OK\r\nContent-Type: audio/mpeg\r\n" + filenameHeader + "\r\n").getBytes());
                FileInputStream filein = null;
                try {
                    MediaLibrary ml = MediaLibrary.getInstance();
                    String file = ml.getFile(mbid);
                    boolean cached = false;
                    if (file != null) {
                        System.out.println("caching " + file);
                        try {
                            filein = new FileInputStream(file);
                        } catch (Exception e) {
                        }
                        if (filein != null) {
                            Connect.connect(filein, outs, 1024, 40);
                            cached = true;
                        }
                    }
                    if (!cached) {
                        Connect.connect(getStream(mbid), outs, 1024, 40);
                        if (filename == null) {
                            System.out.println("mbid not found");
                        } else {
                            System.out.println("download complete");
                            Mp3File mp3 = new Mp3File();
                            mp3.setFileName(filename);
                            mp3.setUntrustedMbid(mbid);
                            mp3.setSource(url);
                            synchronized (MediaLibrary.getInstance()) {
                                ml.files.add(mp3);
                            }
                            ml.untrustedmbidmap.put(mbid, mp3.getFileName());
                            ml.serialize();
                            System.out.println("added untrustedmbid " + mbid);
                        }
                    }
                } catch (SocketException e) {
                    System.out.println("conexion lost");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (filein != null) filein.close();
            } else {
                outs.write("HTTP/1.1 404\r\n\r\n".getBytes());
            }
            outs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void listen(int localport) throws Exception {
        System.out.println("Running p2s http daemon at port " + localport);
        ServerSocket ss = new ServerSocket(localport);
        while (true) {
            Socket client = ss.accept();
            CoreHTTPServer server = new CoreHTTPServer();
            server.client = client;
            server.start();
        }
    }

    public static void main(String args[]) throws Exception {
        CoreHTTPServer.listen(Config.getInstance().getCorePort());
    }
}
