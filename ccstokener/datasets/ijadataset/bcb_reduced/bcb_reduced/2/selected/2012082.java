package org.bintrotter.tracker;

import org.bintrotter.ClientInfo;
import org.bintrotter.Main;
import org.bintrotter.afiles.Files;
import org.bintrotter.metafile.Metafile;
import org.bintrotter.tracker.Response.Peer;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class UpdateRequest {

    public List<Response> responses;

    public List<Peer> peers;

    protected String event() {
        return null;
    }

    private String args(Metafile mf, long uploaded, long downloaded, long left, int target_ready_sockets) {
        String e = event();
        if (e != null) return "?info_hash=" + mf.infoHash.SHA1String + "&peer_id=" + ClientInfo.instance().peer_id + "&port=" + Integer.toString(ClientInfo.instance().port) + "&uploaded=" + Long.toString(uploaded) + "&left=" + Long.toString(left) + "&downloaded=" + Long.toString(downloaded) + "&numwant=" + Integer.toString(target_ready_sockets) + "&event=" + e; else return "?info_hash=" + mf.infoHash.SHA1String + "&peer_id=" + ClientInfo.instance().peer_id + "&port=" + Integer.toString(ClientInfo.instance().port) + "&uploaded=" + Long.toString(uploaded) + "&left=" + Long.toString(left) + "&downloaded=" + Long.toString(downloaded) + "&numwant=" + Integer.toString(target_ready_sockets);
    }

    private void ctor(Metafile mf, long uploaded, long downloaded, long left, int target_ready_sockets) throws Throwable {
        responses = new LinkedList<Response>();
        peers = new LinkedList<Peer>();
        String reqArgs = args(mf, uploaded, downloaded, left, target_ready_sockets);
        if (mf.announce != null) {
            try {
                Try(new URL(mf.announce + reqArgs), mf);
            } catch (Throwable e) {
                Main.log.info("exception trying tracker " + mf.announce + reqArgs);
            }
        }
        if (mf.announce_list != null) {
            for (Iterator<List<String>> it = mf.announce_list.iterator(); it.hasNext(); ) {
                for (Iterator<String> ann = it.next().iterator(); ann.hasNext(); ) {
                    String s = null;
                    try {
                        Try(new URL((s = (ann.next() + reqArgs))), mf);
                    } catch (Throwable e) {
                        Main.log.info("exception trying tracker " + s);
                    }
                }
            }
        }
    }

    public UpdateRequest(Metafile mf, long uploaded, long downloaded, long left, int target_ready_sockets) throws Throwable {
        ctor(mf, uploaded, downloaded, left, target_ready_sockets);
    }

    public UpdateRequest(Files files, int target_ready_sockets) throws Throwable {
        ctor(files.metafile, 0, 0, 0, target_ready_sockets);
    }

    private boolean Try(URL url, Metafile mf) throws Throwable {
        InputStream is = null;
        HttpURLConnection con = null;
        boolean success = false;
        try {
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.connect();
            is = con.getInputStream();
            Response r = new Response(is);
            responses.add(r);
            peers.addAll(r.peers);
            Main.log.info("got " + r.peers.size() + " peers from " + url);
            success = true;
        } finally {
            if (is != null) is.close();
            if (con != null) con.disconnect();
        }
        return success;
    }
}
