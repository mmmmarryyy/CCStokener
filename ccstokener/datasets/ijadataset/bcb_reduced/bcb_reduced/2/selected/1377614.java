package org.gudy.azureus2.core3.tracker.client.impl.bt;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import org.gudy.azureus2.core3.util.Constants;

/**
 * @author Olivier Chalouhi
 *
 */
public class TrackerLoadTester {

    private static final String trackerUrl = "http://localhost:6969/announce";

    public TrackerLoadTester(int nbTorrents, int nbClientsPerTorrent) {
        for (int i = 0; i < nbTorrents; i++) {
            byte[] hash = generate20BytesHash(i);
            for (int j = 0; j < nbClientsPerTorrent; j++) {
                byte[] peerId = generate20BytesHash(j);
                announce(trackerUrl, hash, peerId, 6881 + j);
            }
        }
    }

    public static void main(String args[]) {
        if (args.length < 2) return;
        int nbTorrents = Integer.parseInt(args[0]);
        int nbClientsPerTorrent = Integer.parseInt(args[1]);
        new TrackerLoadTester(nbTorrents, nbClientsPerTorrent);
    }

    private void announce(String trackerURL, byte[] hash, byte[] peerId, int port) {
        try {
            String strUrl = trackerURL + "?info_hash=" + URLEncoder.encode(new String(hash, Constants.BYTE_ENCODING), Constants.BYTE_ENCODING).replaceAll("\\+", "%20") + "&peer_id=" + URLEncoder.encode(new String(peerId, Constants.BYTE_ENCODING), Constants.BYTE_ENCODING).replaceAll("\\+", "%20") + "&port=" + port + "&uploaded=0&downloaded=0&left=0&numwant=50&no_peer_id=1&compact=1";
            URL url = new URL(strUrl);
            URLConnection con = url.openConnection();
            con.connect();
            con.getContent();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] generate20BytesHash(int iter) {
        byte[] result = new byte[20];
        int pos = 0;
        while (iter > 0) {
            result[pos++] = (byte) (iter % 255);
            iter = iter / 255;
        }
        return result;
    }
}
